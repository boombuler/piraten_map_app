package com.boombuler.piraten.map.fragments;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import com.boombuler.piraten.map.PirateMap;
import com.boombuler.piraten.map.R;
import com.boombuler.piraten.map.data.PlakatOverlayItemFilter;


public class FilterFragment extends DialogFragment {
	
	private PlakatOverlayItemFilter mFilter = new PlakatOverlayItemFilter();
	
	public void setFilter(PlakatOverlayItemFilter filter) {
		this.mFilter = filter;
	}
	
    public View createView(LayoutInflater inflater, ViewGroup container) {
    	View v = inflater.inflate(R.layout.filter, container, false);
        
        Spinner election = (Spinner)v.findViewById(R.id.filterElection);
        if (election!=null) {
        	election.setSelection(mFilter.getPreferenceSelected());
        	election.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int pos, long id) {
					FilterFragment.this.setPlakatOverlayItemFilter(pos-1);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					FilterFragment.this.setPlakatOverlayItemFilter(PlakatOverlayItemFilter.ANY_ITEMS);
				}
			});
        }
    	return v;
    }
    
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	LayoutInflater inflater = getActivity().getLayoutInflater();
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.filter_title)
                .setView(createView(inflater, null))
                .setPositiveButton(R.string.button_filter,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	FilterFragment.this.executeSearch();
                        }
                    }
                )
                .create();
    }
    
	public void executeSearch() {
		if (mFilter==null) mFilter = new PlakatOverlayItemFilter();
    	((PirateMap)getActivity()).buildMap(mFilter);
	}
	
	private void setPlakatOverlayItemFilter(int filter) {
    	if (this.mFilter==null) mFilter = new PlakatOverlayItemFilter();
    	mFilter.setPreferenceSelected(filter);
    }

}
