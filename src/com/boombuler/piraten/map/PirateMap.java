package com.boombuler.piraten.map;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PirateMap extends MapActivity {
	private MapView mMapView;
	private CurrentPositionOverlay mMyPosOverlay;
	static int REQUEST_EDIT_PLAKAT = 1;
	static int INITIAL_ZOOM = 16;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Resources res = getResources();
        PlakatOverlayItem.InitResources(res.getDrawable(R.drawable.plakat_default), 
        		res.getDrawable(R.drawable.plakat_ok), 
        		res.getDrawable(R.drawable.plakat_dieb), 
        		res.getDrawable(R.drawable.plakat_niceplace), 
        		res.getDrawable(R.drawable.wand),
        		res.getDrawable(R.drawable.wand_ok),
        		res.getDrawable(R.drawable.plakat_wrecked),
        		res.getDrawable(R.drawable.plakat_a0));
        
        mMapView = (MapView)findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQUEST_EDIT_PLAKAT && resultCode == RESULT_OK)
    		BuildMap(); // Something changed so reload!
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:
            	StartSync();
                return true;
            case R.id.menu_add:
            	startActivityForResult(
		    			new Intent(PirateMap.this, PlakatDetailsActivity.class)
		    				.putExtra(PlakatDetailsActivity.EXTRA_NEW_PLAKAT, true),
		    				PirateMap.REQUEST_EDIT_PLAKAT);
				return true;
            case R.id.menu_settings:
            	startActivity(new Intent(PirateMap.this, SettingsActivity.class));
				return true;
            case android.R.id.home:
            	if (mMapView != null && mMyPosOverlay != null) {
            		GeoPoint location = mMyPosOverlay.getMyLocation();
            		if (location != null) {
            			mMapView.getController().animateTo(location);
            		}
	            	return true;
            	}
            	return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.plakate_map, menu);
    	
        return true;
    }
    
    private void BuildMap() {
    	final List<Overlay> overlays = mMapView.getOverlays();
		overlays.clear();
		
		new Thread(new Runnable() {
			
			public void run() {
				DBAdapter dba = new DBAdapter(PirateMap.this);
				try {
					dba.open();
					overlays.add(dba.getMapOverlay());
				} finally {
					dba.close();
				}
				
			}
		}).start();
		
		if (mMyPosOverlay == null) {
			mMyPosOverlay = new CurrentPositionOverlay(this, mMapView);
		    
			mMyPosOverlay.runOnFirstFix(new Runnable() {
	            public void run() {
	            	if (mMapView.getZoomLevel() < INITIAL_ZOOM)
	            		mMapView.getController().setZoom(INITIAL_ZOOM);
	            	mMapView.getController().animateTo(mMyPosOverlay.getMyLocation());
	            }
	        });
			mMyPosOverlay.enable();
    	}
	    overlays.add(mMyPosOverlay);
		mMapView.invalidate();
    }
    
    @Override
    protected void onResume() {
    	if (mMyPosOverlay == null)
    		BuildMap();
    	if (mMyPosOverlay != null)
    		mMyPosOverlay.enable();
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	if (mMyPosOverlay != null)
    		mMyPosOverlay.disable();
    	super.onPause();
    }
    
    private void StartSync() {
        SyncController sc = new SyncController(this);
        
        sc.setOnCompleteListener(new Runnable() {			
			public void run() {
				BuildMap();
			}
		});
        sc.synchronize();
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}