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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.enadein.carlogbook.CarLogbook;
import com.enadein.carlogbook.R;
import com.enadein.carlogbook.adapter.NotificationAdapter;
import com.enadein.carlogbook.core.BaseFragment;
import com.enadein.carlogbook.core.CarChangedListener;
import com.enadein.carlogbook.db.DBUtils;
import com.enadein.carlogbook.db.ProviderDescriptor;

public class NotificationFragment extends BaseFragment implements
		LoaderManager.LoaderCallbacks<Cursor>,CarChangedListener {
	private NotificationAdapter notificationAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		return inflater.inflate(R.layout.notification_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		notificationAdapter = new NotificationAdapter(getActivity(), null, getMediator().getUnitFacade());
		ListView notifyListView = (ListView) view.findViewById(R.id.list);
		notifyListView.setAdapter(notificationAdapter);


		notifyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				getMediator().showEditNotifcation(id);
//				CommonUtils.createNotify(getActivity(),id);
			}
		});

		view.findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getMediator().showAddNotification();
			}
		});


	}

	@Override
	public void onResume() {
		super.onResume();

		getLoaderManager().restartLoader(CarLogbook.LoaderDesc.NOTIFY_ID, null, this);
		getMediator().showCarSelection(this);
	}

	@Override
	public String getSubTitle() {
		return getString(R.string.menu_item_notifications);
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Long carId = DBUtils.getActiveCarId(getActivity().getContentResolver());
		CursorLoader cursorLoader = new CursorLoader(getActivity(),
				ProviderDescriptor.Notify.CONTENT_URI, null, DBUtils.CAR_SELECTION_NOTIFY, new String[] {String.valueOf(carId)}, null);

		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		notificationAdapter.swapCursor(data);
        showNoItems(data.getCount() == 0);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		notificationAdapter.swapCursor(null);
	}

	@Override
	public void onCarChanged(long id) {
		getLoaderManager().restartLoader(CarLogbook.LoaderDesc.NOTIFY_ID, null, this);
	}
}
