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
package com.enadein.carlogbook.core;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.widget.SimpleCursorAdapter;

import com.enadein.carlogbook.R;
import com.enadein.carlogbook.bean.BarInfo;
import com.enadein.carlogbook.bean.Dashboard;
import com.enadein.carlogbook.bean.DataInfo;
import com.enadein.carlogbook.bean.ReportItem;
import com.enadein.carlogbook.bean.XReport;
import com.enadein.carlogbook.db.CommonUtils;
import com.enadein.carlogbook.db.DBUtils;
import com.enadein.carlogbook.db.ProviderDescriptor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;


public class DataLoader extends AsyncTaskLoader<DataInfo> {
	public static final int DASHBOARD = 0xFF000;
	public static final int TYPE = 0x00FF00;
	public static final int LAST_EVENTS = 0x0000FF;
	public static final int CALC_RATE = 0x0000AA;
	public static final int DETAILED = 0x0000AF;

	public static final int CARS = 0x0000AB;

	public static final String FROM = "from";
	public static final String TO = "to";

	private DataInfo data;
	private int type;
	private Bundle params = null;
	private UnitFacade unitFacade;

	public DataLoader(Context context, int type, Bundle params, UnitFacade unitFacade) {
		this(context, type, unitFacade);
		this.params = params;

	}

	public DataLoader(Context context, int type, UnitFacade unitFacade) {
		super(context);
		this.type = type;
		this.unitFacade = unitFacade;
	}

	@Override
	public DataInfo loadInBackground() {
		data = new DataInfo();
		ContentResolver cr = getContext().getContentResolver();

		switch (type) {
			case DASHBOARD: {
                long carId = DBUtils.getActiveCarId(cr);
				Dashboard dashboard = data.getDashboard();
				dashboard.setTotalOdometerCount(DBUtils.getOdometerCount(carId, cr, 0, 0, -1));
				dashboard.setTotalPrice(DBUtils.getTotalPrice(carId, cr));
				dashboard.setTotalFuelCount(DBUtils.getTotalFuel(carId, cr, 0, 0, false));
				dashboard.setPricePer1(DBUtils.getPricePer1km(carId, cr, 0, 0));
				dashboard.setPriceFuelPer1(DBUtils.getPriceFuelPer1km(carId, cr, 0, 0));
                UnitFacade customUnit = new UnitFacade(getContext());
                customUnit.setConsumptionValue(2);
				dashboard.setFuelRateAvg(DBUtils.getAvgFuel(carId, cr, 0, 0, customUnit));


                customUnit.setConsumptionValue(0);
                dashboard.setFuelRateAvg100(DBUtils.getAvgFuel(carId, cr, 0, 0, customUnit));

                customUnit.setConsumptionValue(1);
                dashboard.setFuelRateAvg2(DBUtils.getAvgFuel(carId, cr, 0, 0, customUnit));


                dashboard.setTotalFuelPrice(DBUtils.getTotalPrice(carId, cr, 0, 0,
						ProviderDescriptor.Log.Type.FUEL));

				dashboard.setTotalServicePrice(DBUtils.getTotalPrice(carId, cr, 0, 0,
								ProviderDescriptor.Log.Type.OTHER, DataInfo.service));

				dashboard.setTotalOtherPrice(DBUtils.getTotalPrice(carId, cr, 0, 0,
						ProviderDescriptor.Log.Type.OTHER, DataInfo.other));

				dashboard.setTotalParkingPrice(DBUtils.getTotalPrice(carId, cr, 0, 0,
						ProviderDescriptor.Log.Type.OTHER, DataInfo.parking));

				dashboard.setTotalPartsPrice(DBUtils.getTotalPrice(carId, cr, 0, 0,
						ProviderDescriptor.Log.Type.OTHER, DataInfo.parts));

				Calendar c = Calendar.getInstance();
				CommonUtils.trunkMonth(c);
				c.add(Calendar.MONTH, -3);
				ArrayList<BarInfo> bars = new ArrayList<BarInfo>();
				addMonthTotalPrice(c, cr, bars);
				addMonthTotalPrice(c, cr, bars);
				addMonthTotalPrice(c, cr, bars);
				addMonthTotalPrice(c, cr, bars);
				dashboard.setCostLast4Months(bars);
				///

				///---------
				 c = Calendar.getInstance();
				CommonUtils.trunkMonth(c);
				c.add(Calendar.MONTH, -3);
				bars = new ArrayList<BarInfo>();
				addMonthTotalIncome(c, cr, bars);
				addMonthTotalIncome(c, cr, bars);
				addMonthTotalIncome(c, cr, bars);
				addMonthTotalIncome(c, cr, bars);
				dashboard.setIncomeLast4Months(bars);

				///---------
				c = Calendar.getInstance();
				CommonUtils.trunkMonth(c);
				c.add(Calendar.MONTH, -3);
				bars = new ArrayList<BarInfo>();
				addMonthCost1(c, cr, bars);
				addMonthCost1(c, cr, bars);
				addMonthCost1(c, cr, bars);
				addMonthCost1(c, cr, bars);
				dashboard.setCostPer1(bars);

				///---------
				c = Calendar.getInstance();
				CommonUtils.trunkMonth(c);
				c.add(Calendar.MONTH, -3);
				bars = new ArrayList<BarInfo>();
				addMonthFuelCost1(c, cr, bars);
				addMonthFuelCost1(c, cr, bars);
				addMonthFuelCost1(c, cr, bars);
				addMonthFuelCost1(c, cr, bars);
				dashboard.setFuelCostPer1(bars);

				 c = Calendar.getInstance();
				CommonUtils.trunkMonth(c);

				c.add(Calendar.MONTH, -3);
				bars = new ArrayList<BarInfo>();
				addMonthTotalRun(c, cr, bars);
				addMonthTotalRun(c, cr, bars);
				addMonthTotalRun(c, cr, bars);
				addMonthTotalRun(c, cr, bars);

				dashboard.setRunLast4Months(bars);
				break;
			}
			case TYPE: {
				long from = 0;
				long to = 0;

				if (params != null) {
					from = params.getLong(FROM);
					to = params.getLong(TO);
				}



				String[] logTypes = getContext().getResources().getStringArray(R.array.log_type);
				ArrayList<ReportItem> reportItems = new ArrayList<ReportItem>();
                long carId = DBUtils.getActiveCarId(cr);
				double allCost = DBUtils.getTotalPrice(carId,cr, from, to, -1, null);

				double fuelTotal = DBUtils.getTotalPrice(carId, cr, from, to, ProviderDescriptor.Log.Type.FUEL, null);

				if (fuelTotal > 0.) {
					ReportItem reportItem = new ReportItem();
					String p = CommonUtils.wrapPt(unitFacade,fuelTotal, allCost);

					reportItem.setName(getContext().getString(R.string.total_fuel_cost) + p);
					reportItem.setResId(R.drawable.fuel);
					reportItem.setValue(fuelTotal);

					reportItems.add(reportItem);
				}


				for (int i = 1; i < logTypes.length; i++) {
					double total = DBUtils.getTotalPrice(carId, cr, from, to, ProviderDescriptor.Log.Type.OTHER, new int[] {i});
					String p = CommonUtils.wrapPt(unitFacade,total, allCost);


					if (total > 0) {
						ReportItem reportItem = new ReportItem();

						reportItem.setName(logTypes[i] + p);
						reportItem.setResId(DataInfo.images.get(i));
						reportItem.setValue(total);

						reportItems.add(reportItem);
					}
				}

                Cursor othersCursor = cr.query(ProviderDescriptor.DataValue.CONTENT_URI, null, ProviderDescriptor.DataValue.Cols.TYPE + " = " + ProviderDescriptor.DataValue.Type.OTHERS, null, null);
                while (othersCursor.moveToNext()) {
                    long otherId = othersCursor.getLong(othersCursor.getColumnIndex(ProviderDescriptor.DataValue.Cols._ID));
                    String name = othersCursor.getString(othersCursor.getColumnIndex(ProviderDescriptor.DataValue.Cols.NAME));
                    double total = DBUtils.getTotalPrice(carId, cr, from, to, ProviderDescriptor.Log.Type.OTHER,  new int[] {0}, false, otherId);


                    if (total > 0) {
                        ReportItem reportItem = new ReportItem();
						String p = CommonUtils.wrapPt(unitFacade,total, allCost);
                        reportItem.setName(name + p);
                        reportItem.setResId(DataInfo.images.get(0));
                        reportItem.setValue(total);

                        reportItems.add(reportItem);
                    }
                }
                othersCursor.close();

				if (reportItems.size() > 0) {
					Collections.sort(reportItems, new Comparator<ReportItem>() {
						@Override
						public int compare(ReportItem reportItem, ReportItem reportItem2) {
							return reportItem.getValue() > reportItem2.getValue() ? -1: 0;
						}
					});


                    ReportItem reportItem = new ReportItem();
                    reportItem.setName(getContext().getString(R.string.total_cost));
                    reportItem.setResId(R.drawable.coint);
                    reportItem.setValue(allCost);
                    reportItems.add(reportItem);
				} else {
					addNotFoundItem(reportItems);
				}

				data.setReportData(reportItems);
				break;
			}
			case LAST_EVENTS: {

				long currentTime = System.currentTimeMillis();
				String daysLb = getContext().getString(R.string.days);
				ArrayList<ReportItem> reportItems = new ArrayList<ReportItem>();

				String[] logTypes = getContext().getResources().getStringArray(R.array.log_type);

				long date = DBUtils.getLastEventDate(cr, ProviderDescriptor.Log.Type.FUEL, -1);


				if (date > 0) {
					double dayPassed = DBUtils.calcDayPassedTrunk(date, currentTime);
					String dS = DBUtils.formatDays(dayPassed, daysLb);
					ReportItem reportItem = new ReportItem();
					reportItem.setName(getContext().getString(R.string.total_fuel_cost) + dS);
					reportItem.setResId(R.drawable.fuel);

					reportItem.setValue2(date);


					reportItems.add(reportItem);
				}

				for (int i = 1; i < logTypes.length; i++) {
					date = DBUtils.getLastEventDate(cr, ProviderDescriptor.Log.Type.OTHER, i);

					if (date > 0) {
						double dayPassed = DBUtils.calcDayPassedTrunk(date, currentTime);
						String dS = DBUtils.formatDays(dayPassed, daysLb);

						ReportItem reportItem = new ReportItem();
						reportItem.setName(logTypes[i] + dS);
						reportItem.setResId(DataInfo.images.get(i));
						reportItem.setValue2(date);

						reportItems.add(reportItem);
					}
				}

                Cursor othersCursor = cr.query(ProviderDescriptor.DataValue.CONTENT_URI, null, ProviderDescriptor.DataValue.Cols.TYPE + " = " + ProviderDescriptor.DataValue.Type.OTHERS, null, null);
                while (othersCursor.moveToNext()) {
                    long otherId = othersCursor.getLong(othersCursor.getColumnIndex(ProviderDescriptor.DataValue.Cols._ID));
                    String name = othersCursor.getString(othersCursor.getColumnIndex(ProviderDescriptor.DataValue.Cols.NAME));

                    date = DBUtils.getLastEventDate(cr, ProviderDescriptor.Log.Type.OTHER, 0, otherId);

                    if (date > 0) {
						double dayPassed = DBUtils.calcDayPassedTrunk(date, currentTime);
						String dS = DBUtils.formatDays(dayPassed, daysLb);
                        ReportItem reportItem = new ReportItem();
                        reportItem.setName(name + dS);
                        reportItem.setResId(DataInfo.images.get(0));
                        reportItem.setValue2(date);

                        reportItems.add(reportItem);
                    }
                }
                othersCursor.close();

				if (reportItems.size() > 0) {
					Collections.sort(reportItems, new Comparator<ReportItem>() {
						@Override
						public int compare(ReportItem reportItem, ReportItem reportItem2) {
							return reportItem.getValue2() > reportItem2.getValue2() ? -1: 0;
						}
					});
				} else {
					addNotFoundItem(reportItems);
				}

				data.setReportData(reportItems);
				break;
			}

            case CALC_RATE: {
                ReportFacade reportFacade = new ReportFacade(getContext());
                reportFacade.calculateFuelRate(unitFacade);
                break;
            }

            case DETAILED: {
                ReportFacade reportFacade = new ReportFacade(getContext());
                long carId = DBUtils.getActiveCarId(cr);
                XReport xReport = new XReport();
                xReport.fuelCountTotal = reportFacade.getFuelCountTotal(cr, carId);
                xReport.fillupCount = reportFacade.getFillupCount(cr, carId);
                xReport.minFillupVolume = reportFacade.getMinFillupVolume(cr, carId);
                xReport.maxFillupVolume = reportFacade.getMaxFillupVolume(cr, carId);
                xReport.avgFillupVolume = reportFacade.getAvgFillupVolume(cr, carId);
                xReport.fuelVolumeCurrentMonth = reportFacade.getFuelCountCurrentMonth(cr, carId);
                xReport.fuelVolumeLastMonth = reportFacade.getFuelCountLastMonth(cr, carId);
                xReport.fuelVolumeCurrentYear = reportFacade.getFuelCountCurrentYear(cr, carId);
                xReport.fuelVolumeLastYear = reportFacade.getFuelCountLastYear(cr, carId);
                xReport.totalDist = reportFacade.getTotalDistance(cr, carId);
                xReport.odometer_count = reportFacade.getOdometer(cr, carId);
                xReport.month_dist = reportFacade.getCurrentMonthDistance(cr, carId);
                xReport.last_month_dist = reportFacade.getLastMonthDistance(cr, carId);
                xReport.year_dist = reportFacade.getCurrentYearDistance(cr, carId);
                xReport.last_year_dist = reportFacade.getLastYearDistance(cr, carId);
                xReport.per_day_dist = reportFacade.getAVGDistancePerDay(cr, carId);
                xReport.per_month_dist = reportFacade.getAVGDistancePerMonth(cr, carId);
                xReport.per_year_dist = reportFacade.getAVGDistancePerYear(cr, carId);
                xReport.cost_total = reportFacade.getTotalCost(cr, carId);
                xReport.cost_per1 = reportFacade.getCostPer1Dist(cr, carId);
                xReport.cost_fuel_per1 = DBUtils.getPriceFuelPer1km(carId, cr, 0, 0);
                xReport.cost_total_month = reportFacade.getTotalCostThisMonth(cr, carId);
                xReport.cost_total_last_month = reportFacade.getTotalCostLastMonth(cr, carId);
                xReport.cost_total_year = reportFacade.getTotalCostThisYear(cr, carId);
                xReport.cost_total_last_year = reportFacade.getTotalCostLastYear(cr, carId);
                xReport.cost_price_min = reportFacade.getMinFuelPrice1Unit(cr, carId);
                xReport.cost_price_max = reportFacade.getMaxFuelPrice1Unit(cr, carId);
                xReport.cost_price_avg = reportFacade.getAvgFuelPrice1Unit(cr, carId);
                xReport.cost_fillup_min = reportFacade.getMinCostFillup(cr, carId);
                xReport.cost_fillup_max = reportFacade.getMaxCostFillup(cr, carId);
                xReport.cost_fillup_avg = reportFacade.getAvgCostFillup(cr, carId);
                xReport.cost_total_per_day = reportFacade.getAvgTotalCostPerDay(cr, carId);
                xReport.cost_total_per_month = reportFacade.getAvgTotalCostPerMonth(cr, carId);
                xReport.cost_total_per_year = reportFacade.getAvgTotalCostPerYear(cr, carId);
                xReport.cost_total_per_day_fuel = reportFacade.getAvgFuelCostPerDay(cr, carId);
                xReport.cost_total_per_month_fuel = reportFacade.getAvgFuelCostPerMonth(cr, carId);
                xReport.cost_total_per_year_fuel = reportFacade.getAvgFuelCostPerYear(cr, carId);
                xReport.cost_total_per_day_other = reportFacade.getAvgOtherExpensesCostPerDay(cr, carId);
                xReport.cost_total_per_month_other = reportFacade.getAvgOhterExpensesCostPerMonth(cr, carId);
                xReport.cost_total_per_year_other = reportFacade.getAvgOtherExpensesCostPerYear(cr, carId);


				xReport.total_income = reportFacade.getTotalIncome(cr, carId);
				xReport.total_month_income = reportFacade.getTotalIncomeThisMonth(cr, carId);
				xReport.total_month_last_income = reportFacade.getTotalIncomeLastMonth(cr, carId);
				xReport.total_year_income = reportFacade.getTotalIncomeYear(cr, carId);
				xReport.total_year_last_income = reportFacade.getTotalIncomeLastYear(cr, carId);

				xReport.avg100 = reportFacade.getAvgLPer100(cr, carId);
                xReport.avglperkm = reportFacade.getAvgLPer1Km(cr, carId);
                xReport.avgkmperl = reportFacade.getAvgKmPerL(cr, carId);

                reportFacade.calculateXReport(xReport, cr, carId);

                data.setxReport(xReport);
                break;
            }
			case CARS: {
				CarsDataInfo cdi = new CarsDataInfo();
				Cursor cursor = cr.query(ProviderDescriptor.Car.CONTENT_URI, null, null, null, null);

//				if (cursor != null) {
//					String[] adapterCols = new String[]{ProviderDescriptor.DataValue.Cols.NAME};
//					int[] adapterRowViews = new int[]{android.R.id.text1};
//					SimpleCursorAdapter carsAdapter = new SimpleCursorAdapter(getContext(), R.layout.nav_item,
//							cursor, adapterCols, adapterRowViews, 0);
//					carsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
//					int position = getPositionFromAdapterById(carsAdapter, DBUtils.getActiveCarId(cr));
//
//					cdi.setAdapter(carsAdapter);
//					cdi.setPosition(position);
//				}
				cdi.setCursor(cursor);
				cdi.setPosition(getPosition(cursor, DBUtils.getActiveCarId(cr)));
				data.setCarsDataInfo(cdi);
				break;
			}
		}

		return data;
	}

	protected int getPosition(Cursor c, long id) {
		if (c == null || c.getCount() == 0) {
			return 0;
		}

		int position = 0;
		while (c.moveToNext()) {
			long currentId = c.getLong(0);
			if (currentId == id) {
				position = c.getPosition();
				break;
			}
		}
		return position;
	}


	private void addNotFoundItem(ArrayList<ReportItem> reportItems) {
		ReportItem item = new ReportItem();
		item.setName(getContext().getString(R.string.not_found));
		item.setValue(0);
		item.setResId(R.drawable.clean);
		reportItems.add(item);
	}

	private void addMonthTotalPrice(Calendar c, ContentResolver cr, ArrayList<BarInfo> bars) {
		long from = c.getTimeInMillis();
		String name = CommonUtils.formatMonth(new Date(from));
		c.add(Calendar.MONTH, 1);
		long to = c.getTimeInMillis();
		double price = DBUtils.getTotalPrice(DBUtils.getActiveCarId(cr), cr, from, to, -1);

		BarInfo barInfo = new BarInfo();
		barInfo.setValue((float) price);
		barInfo.setName(name);
		bars.add(barInfo);
	}

	private void addMonthTotalIncome(Calendar c, ContentResolver cr, ArrayList<BarInfo> bars) {
		long from = c.getTimeInMillis();
		String name = CommonUtils.formatMonth(new Date(from));
		c.add(Calendar.MONTH, 1);
		long to = c.getTimeInMillis();
		double price = DBUtils.getTotalIncome(DBUtils.getActiveCarId(cr), cr, from, to);

		BarInfo barInfo = new BarInfo();
		barInfo.setValue((float) price);
		barInfo.setName(name);
		bars.add(barInfo);
	}

	private void addMonthFuelCost1(Calendar c, ContentResolver cr, ArrayList<BarInfo> bars) {
		long from = c.getTimeInMillis();
		String name = CommonUtils.formatMonth(new Date(from));
		c.add(Calendar.MONTH, 1);
		long to = c.getTimeInMillis();
		double price = DBUtils.getPriceFuelPer1km(DBUtils.getActiveCarId(cr), cr, from, to);

		BarInfo barInfo = new BarInfo();
		barInfo.setValue((float) price);
		barInfo.setName(name);
		bars.add(barInfo);
	}

	private void addMonthCost1(Calendar c, ContentResolver cr, ArrayList<BarInfo> bars) {
		long from = c.getTimeInMillis();
		String name = CommonUtils.formatMonth(new Date(from));
		c.add(Calendar.MONTH, 1);
		long to = c.getTimeInMillis();
		double price = DBUtils.getPricePer1km(DBUtils.getActiveCarId(cr), cr, from, to);

		BarInfo barInfo = new BarInfo();
		barInfo.setValue((float) price);
		barInfo.setName(name);
		bars.add(barInfo);
	}

	private void addMonthTotalRun(Calendar c, ContentResolver cr, ArrayList<BarInfo> bars) {
		long from = c.getTimeInMillis();
		String name = CommonUtils.formatMonth(new Date(from));
		c.add(Calendar.MONTH, 1);
		long to = c.getTimeInMillis();
		double run = DBUtils.getOdometerCount(DBUtils.getActiveCarId(cr),cr, from, to, -1);

		BarInfo barInfo = new BarInfo();
		barInfo.setValue((float) run);
		barInfo.setName(name);
		bars.add(barInfo);
	}

	@Override
	protected void onStartLoading() {
	   if (data != null) {
		   deliverResult(data);
	   } else {
		   forceLoad();
	   }
	}
}