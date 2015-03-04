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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.enadein.carlogbook.R;

public class ConfirmDialog extends DialogFragment {
	public static final int REQUEST_CODE_CONFIRM = 100;
	public static final int RETURN_VALUE_YES = 101;

	public static ConfirmDialog newInstance() {
		return new ConfirmDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.ConfirmDelete);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getViewNew(inflater, container);
		Button yes = (Button) view.findViewById(R.id.yes);
		yes.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				((DialogListener)getActivity()).onDialogEvent(REQUEST_CODE_CONFIRM, getReturnValuesYes(), null);
				dismiss();
			}
		});
		Button no  = (Button) view.findViewById(R.id.no);
		no.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
		return view;
	}

	protected int getReturnValuesYes() {
		return RETURN_VALUE_YES;
	}

	protected View getViewNew(LayoutInflater inflater, ViewGroup container) {
		return inflater.inflate(R.layout.confirm_delete, container, false);
	}
}
