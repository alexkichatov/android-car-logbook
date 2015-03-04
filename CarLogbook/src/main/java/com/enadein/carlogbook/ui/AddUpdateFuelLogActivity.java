/*
    CarLogbook.
    Copyright (C) 2014  Eugene Nadein

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.enadein.carlogbook.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.enadein.carlogbook.R;
import com.enadein.carlogbook.core.CarChangedListener;
import com.enadein.carlogbook.core.UnitFacade;
import com.enadein.carlogbook.db.CommonUtils;
import com.enadein.carlogbook.db.DBUtils;
import com.enadein.carlogbook.db.ProviderDescriptor;

import java.util.Calendar;
import java.util.Date;

public class AddUpdateFuelLogActivity extends BaseLogAcivity implements
		LoaderManager.LoaderCallbacks<Cursor> {
	public static final int LOADER_TYPE = 102;
	public static final int LOADER_STATION = 103;

	private SimpleCursorAdapter fuelAdapter;
	private SimpleCursorAdapter stationAdapter;

	private EditText comments;
	private EditText odomenterView;
	private TextView dateView;
	private EditText fuelValueView;
	private EditText priceView;
	private EditText priceTotalView;
	private Spinner fuelTypeSpinner;
	private Spinner stationSpinner;

	private PriceValueState priceValueState = new PriceValueState();
    private boolean halt = false;

    private UnitFacade unitFacade;

	private int fuelTypeId;
	private int fuelStationId;

	private abstract class TextWatcherWrapper implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

		}

		@Override
		public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
			update();
		}

		@Override
		public void afterTextChanged(Editable editable) {

		}

		abstract public void update();
	}


	@Override
	public String getSubTitle() {
		return (mode == PARAM_EDIT) ? getString(R.string.log_fuel_title_edit) : getString(R.string.log_fuel_title);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] queryCols = new String[]{ProviderDescriptor.DataValue.Cols._ID,
				ProviderDescriptor.DataValue.Cols.NAME};

		CursorLoader cursorLoader = null;

		switch (id) {
			case LOADER_STATION: {
				cursorLoader = new CursorLoader(this,
						ProviderDescriptor.DataValue.CONTENT_URI, queryCols,
						"TYPE = ?", new String[]{String.valueOf(ProviderDescriptor.DataValue.Type.STATION)}, null);
				break;
			}
			case LOADER_TYPE: {
				cursorLoader = new CursorLoader(this,
						ProviderDescriptor.DataValue.CONTENT_URI, queryCols,
						"TYPE = ?", new String[]{String.valueOf(ProviderDescriptor.DataValue.Type.FUEL)}, null);

				break;
			}
		}

		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (loader.getId() == LOADER_TYPE) {
			fuelAdapter.swapCursor(data);
			long defaultId = DBUtils.getDefaultId(getContentResolver(),
					ProviderDescriptor.DataValue.Type.FUEL);

			Spinner fuelTypeSpinner = (Spinner) findViewById(R.id.typeSpinner);

			fuelTypeSpinner.setSelection(getPositionFromAdapterById(fuelAdapter,
					(fuelTypeId > 0) ? fuelTypeId : defaultId));

		} else {
			stationAdapter.swapCursor(data);
			long defaultId = DBUtils.getDefaultId(getContentResolver(),
					ProviderDescriptor.DataValue.Type.STATION);

			Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner);
			stationSpinner.setSelection(getPositionFromAdapterById(stationAdapter,
					(fuelStationId > 0) ? fuelStationId : defaultId));
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == LOADER_TYPE) {
			fuelAdapter.swapCursor(null);
		} else {
			stationAdapter.swapCursor(null);
		}
	}

	@Override
	void setDateText(String text) {
		dateView.setText(text);
	}

	private ContentValues getContentValues() {
		ContentResolver cr = getContentResolver();
		ContentValues cv = new ContentValues();

		cv.put(ProviderDescriptor.Log.Cols.ODOMETER,
				Integer.valueOf(odomenterView.getText().toString()));

		long carId = getCarId();
		cv.put(ProviderDescriptor.Log.Cols.CAR_ID, carId);

		cv.put(ProviderDescriptor.Log.Cols.FUEL_VOLUME, CommonUtils.getPriceValue(fuelValueView));
		cv.put(ProviderDescriptor.Log.Cols.PRICE, CommonUtils.getPriceValue(priceView));

		cv.put(ProviderDescriptor.Log.Cols.TYPE_LOG, ProviderDescriptor.Log.Type.FUEL);

		cv.put(ProviderDescriptor.Log.Cols.DATE, date.getTime());

		EditText commentEditText = (EditText) findViewById(R.id.comment);
		String comment = commentEditText.getText().toString().trim();
		setComments(cv, comment);

		long fuelTypeId = fuelAdapter.getItemId(fuelTypeSpinner.getSelectedItemPosition());
		DBUtils.setDafaultId(cr, ProviderDescriptor.DataValue.Type.FUEL, fuelTypeId);
		cv.put(ProviderDescriptor.Log.Cols.FUEL_TYPE_ID, fuelTypeId);

		long fuelStationId = stationAdapter.getItemId(stationSpinner.getSelectedItemPosition());
		DBUtils.setDafaultId(cr, ProviderDescriptor.DataValue.Type.STATION, fuelStationId);
		cv.put(ProviderDescriptor.Log.Cols.FUEL_STATION_ID, fuelStationId);


		return cv;
	}


	private class PriceValueState {
		public void updatePrice() {	};

		public void updateTotal() {	};

		public void updateFuel() {}	;

		protected void calculateTotal() {
			double fuelValue = CommonUtils.getRawDouble(fuelValueView.getText().toString());
			double priceValue = CommonUtils.getRawDouble(priceView.getText().toString());
			double priceTotalValue = fuelValue * priceValue;

			priceTotalView.setText(CommonUtils.formatPriceNew(priceTotalValue, unitFacade));
		}
	}

	private class PriceState extends PriceValueState {

		@Override
		public void updatePrice() {
			calculateTotal();
		}
	}

	private class FuelState extends PriceValueState {

		@Override
		public void updateFuel() {
			calculateTotal();
		}
	}

	private class TotalState extends PriceValueState {

		@Override
		public void updateTotal() {
			double priceValue = CommonUtils.getRawDouble(priceView.getText().toString());
            double priceTotalValue = CommonUtils.getRawDouble(priceTotalView.getText().toString());

            if (priceValue > 0 && !halt) {
                double fuelValue = CommonUtils.div(priceTotalValue, priceValue);
                fuelValueView.setText(CommonUtils.formatPriceNew(fuelValue, unitFacade));
            } else {
                halt = true;
                double fuelValue = CommonUtils.getRawDouble(fuelValueView.getText().toString());
                double price = CommonUtils.div(priceTotalValue, fuelValue);
                priceView.setText(CommonUtils.formatPriceNew(price, unitFacade));
            }
		}
	}

	private class StateOnFocusChangeListener implements View.OnFocusChangeListener {
		private PriceValueState state;

		private StateOnFocusChangeListener(PriceValueState state) {
			this.state = state;
		}

		@Override
		public void onFocusChange(View view, boolean focused) {
			if (focused) {
				priceValueState = state;
                halt = false;
			}
		}
	}

	@Override
	protected boolean validateEntity() {
		boolean result = true;

		if (!validateFuelVavlView(R.id.errorFuel, fuelValueView)) {
			result = false;
		}

		if (!validateView(R.id.errorPrice, priceView)) {
			result = false;
		}

		if (!validateOdometer(R.id.errorOdometer, odomenterView)) {
			result = false;
		}

		return result;
	}

	@Override
	protected void createEntity() {
		Calendar todayCalendar = Calendar.getInstance();
		CommonUtils.trunkDay(todayCalendar);

//		long currentDate = date.getTime();


//        //TODO Refactor It
//		if (currentDate >= todayCalendar.getTimeInMillis()) {
//			DBUtils.updateFuelRate(getContentResolver(), Integer.valueOf(odomenterView.getText().toString()),
//					CommonUtils.getPriceValue(fuelValueView), getMediator().getUnitFacade());
//		}



		getContentResolver().insert(ProviderDescriptor.Log.CONTENT_URI, getContentValues());

		CommonUtils.validateOdometerNotifications(AddUpdateFuelLogActivity.this,
				Integer.valueOf(odomenterView.getText().toString()));

//        updateActiveCar();
	}

	@Override
	protected void hookUpToParrent() {
		showAddNotify("");
	}

	@Override
	protected void updateEntity() {
		CommonUtils.validateOdometerNotifications(AddUpdateFuelLogActivity.this,
				Integer.valueOf(odomenterView.getText().toString()));
		getContentResolver().update(ProviderDescriptor.Log.CONTENT_URI, getContentValues(), ID_PARAM, new String[] {String.valueOf(id)});
	}

	@Override
	protected void populateEditEntity() {
		Cursor logCursor = getContentResolver().query(ProviderDescriptor.Log.CONTENT_URI, null, ID_PARAM, new String[] {String.valueOf(id)}, null);
		if (logCursor != null && logCursor.moveToFirst()) {
			int odometerIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.ODOMETER);
			int priceIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.PRICE);
			int dateIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.DATE);
			int fueldValueIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.FUEL_VOLUME);
			int fuelTypePosIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.FUEL_TYPE_ID);
			int fuelStationPosIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.FUEL_STATION_ID);

			int commentIdx = logCursor.getColumnIndex(ProviderDescriptor.Log.Cols.CMMMENT);
			comments.setText(logCursor.getString(commentIdx));

			odomenterView.setText(String.valueOf(logCursor.getLong(odometerIdx)));
			priceView.setText(CommonUtils.formatPriceNew(logCursor.getDouble(priceIdx), unitFacade));
			date = new Date(logCursor.getLong(dateIdx));
			fuelValueView.setText(CommonUtils.formatFuel(logCursor.getDouble(fueldValueIdx), unitFacade));

			new PriceState().updatePrice();

//			fuelTypeSpinner.setSelection(getPositionFromAdapterById(fuelAdapter,
//					logCursor.getInt(fuelTypePosIdx)));

			fuelTypeId = logCursor.getInt(fuelTypePosIdx);
//
//			stationSpinner.setSelection(getPositionFromAdapterById(stationAdapter,
//					logCursor.getInt(fuelStationPosIdx)));
			fuelStationId = logCursor.getInt(fuelStationPosIdx);

			loadFuelTypeAndStation();

			String carName = DBUtils.getActiveCarName(getContentResolver(), getCarId());
			new AQuery(this).id(R.id.carView).visible().text(carName);

		}
	}


	@Override
	public int getCarSelectorViewId() {
		return R.id.carsAdd ;
	}

	@Override
	protected void populateCreateEntity() {
		findViewById(R.id.notify_group).setVisibility(View.VISIBLE);
		date = new Date(System.currentTimeMillis());
        populateValuesByCar();
		loadFuelTypeAndStation();

		getMediator().showCarSelection(new CarChangedListener() {
			@Override
			public void onCarChanged(long id) {
				populateValuesByCar();
			}
		});
	}

	@Override
	protected void postCreate() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	    unitFacade = getMediator().getUnitFacade();
//		unitFacade.appendFuelUnit((TextView) findViewById(R.id.label_fuel_volume), true);
//		unitFacade.appendCurrency((TextView) findViewById(R.id.label_price), false);
//		unitFacade.appendCurrency((TextView) findViewById(R.id.label_cost), false);
//		unitFacade.appendDistUnit((TextView) findViewById(R.id.label_odometer), true);
        updateLabels();

		odomenterView = (EditText) findViewById(R.id.odometer);
		comments = (EditText) findViewById(R.id.comment);

		priceView = (EditText) findViewById(R.id.price);
		priceTotalView = (EditText) findViewById(R.id.priceTotal);

		fuelValueView = (EditText) findViewById(R.id.fuel_volume);

		fuelTypeSpinner = (Spinner) findViewById(R.id.typeSpinner);
		stationSpinner = (Spinner) findViewById(R.id.stationSpinner);
		String[] adapterCols = new String[]{ProviderDescriptor.DataValue.Cols.NAME};
		int[] adapterRowViews = new int[]{android.R.id.text1};
		fuelAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
				null, adapterCols, adapterRowViews, 0);
		fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fuelTypeSpinner.setAdapter(fuelAdapter);

		stationAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
				null, adapterCols, adapterRowViews, 0);
		stationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stationSpinner.setAdapter(stationAdapter);


//        updateLabels();

//		getSupportLoaderManager().initLoader(LOADER_STATION, null, this);
//		getSupportLoaderManager().initLoader(LOADER_TYPE, null, this);
	}

	private void loadFuelTypeAndStation() {
		getSupportLoaderManager().initLoader(LOADER_STATION, null, this);
		getSupportLoaderManager().initLoader(LOADER_TYPE, null, this);
	}

	@Override
	protected void postPopulate() {

		dateView = (TextView) findViewById(R.id.date);
		dateView.setText(CommonUtils.formatDate(date));

		fuelValueView.setOnFocusChangeListener(new StateOnFocusChangeListener(new FuelState()));
		priceView.setOnFocusChangeListener(new StateOnFocusChangeListener(new PriceState()));
		priceTotalView.setOnFocusChangeListener(new StateOnFocusChangeListener(new TotalState()));

		fuelValueView.addTextChangedListener(new TextWatcherWrapper() {
			@Override
			public void update() {
				priceValueState.updateFuel();
			}
		});

		priceView.addTextChangedListener(new TextWatcherWrapper() {
			@Override
			public void update() {
				priceValueState.updatePrice();
			}
		});
		priceTotalView.addTextChangedListener(new TextWatcherWrapper() {
			@Override
			public void update() {
				priceValueState.updateTotal();
			}
		});

		odomenterView.setSelection(odomenterView.getText().length());
		odomenterView.requestFocus();
	}

	@Override
	protected int getContentLayout() {
		return R.layout.add_fuel_log;
	}

    public void populateValuesByCar() {
        long odometerValue = DBUtils.getMaxOdometerValue(getContentResolver(), getCarId());
        odomenterView.setText(String.valueOf(odometerValue));
        priceView.setText(CommonUtils.formatPriceNew(DBUtils.getLastPriceValue(getContentResolver()),unitFacade));

        updateLabels();
    }

    public void updateLabels() {
        UnitFacade labelFacade = new UnitFacade(this);
        labelFacade.reload(getCarId(), true);

        TextView priceLabel = (TextView) findViewById(R.id.label_price);
        priceLabel.setText(getString(R.string.log_fuel_price));

        TextView fuelVolumeLabel = (TextView) findViewById(R.id.label_fuel_volume);
        fuelVolumeLabel.setText(getString(R.string.log_fuel_volume));

        TextView costLabel = (TextView) findViewById(R.id.label_cost);
        costLabel.setText(getString(R.string.log_fuel_price_total));

        TextView odometerLabel = (TextView) findViewById(R.id.label_odometer);
        odometerLabel.setText(getString(R.string.log_fuel_odometer));

        labelFacade.appendFuelUnit(fuelVolumeLabel, true);
        labelFacade.appendCurrency(priceLabel, false);
        labelFacade.appendCurrency(costLabel, false);
        labelFacade.appendDistUnit(odometerLabel, true);
    }

}
