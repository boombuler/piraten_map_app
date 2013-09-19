package com.boombuler.piraten.map;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.boombuler.piraten.map.data.PlakatOverlayItemFilter;

public class FilterActivity extends Activity{
	
	private PlakatOverlayItemFilter mFilter = new PlakatOverlayItemFilter();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter);
        
        loadIntent();
        Spinner election = (Spinner)findViewById(R.id.filterElection);
        if (election!=null) {
        	election.setSelection(mFilter.getPreferenceSelected());
        	election.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int pos, long id) {
					FilterActivity.this.setPlakatOverlayItemFilter(pos-1);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					FilterActivity.this.setPlakatOverlayItemFilter(PlakatOverlayItemFilter.ANY_ITEMS);
				}
			});
        }
    	

        ImageButton btnFilter = (ImageButton)findViewById(R.id.buttonFilter);
        if (btnFilter!=null) {
        	btnFilter.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					FilterActivity.this.executeSearch();
				}
			});
        }
    }
    
    private void loadIntent() {
    	Intent intent = getIntent();
    	if (intent.hasExtra(Constants.EXTRA_ITEMFILTER)) {
    		mFilter = intent.getParcelableExtra(Constants.EXTRA_ITEMFILTER);
    	}
	}

	protected void executeSearch() {
    	if (mFilter==null) mFilter = new PlakatOverlayItemFilter();
    	
		Intent result = new Intent();
		result.putExtra(Constants.EXTRA_ITEMFILTER, (Parcelable)mFilter);
		setResult(Constants.RES_FILTER, result);
		finish();

	}

	private void setPlakatOverlayItemFilter(int filter) {
    	if (this.mFilter==null) mFilter = new PlakatOverlayItemFilter();
    	mFilter.setPreferenceSelected(filter);
    }
}
