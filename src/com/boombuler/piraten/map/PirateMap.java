package com.boombuler.piraten.map;

import java.util.List;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class PirateMap extends MapActivity {
	private MapView mMapView;
	private MyLocationOverlay mMyPosOverlay;
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
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	menu.add(R.string.menu_sync).setIcon(R.drawable.ic_menu_refresh)
    	    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					StartSync();
					return true;
				}
			});
    	menu.add(R.string.menu_add).setIcon(android.R.drawable.ic_menu_add)
	    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				startActivityForResult(
		    			new Intent(PirateMap.this, PlakatDetailsActivity.class)
		    				.putExtra(PlakatDetailsActivity.EXTRA_NEW_PLAKAT, true),
		    				PirateMap.REQUEST_EDIT_PLAKAT);
				return true;
			}
		});
    	menu.add(R.string.settings).setIcon(android.R.drawable.ic_menu_preferences)
    	.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(PirateMap.this, SettingsActivity.class));
				return true;
			}
		});
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
		
		
		
		mMyPosOverlay = new MyLocationOverlay(PirateMap.this, mMapView);
	    overlays.add(mMyPosOverlay);
	    mMyPosOverlay.enableCompass();
	    mMyPosOverlay.enableMyLocation();
	    mMyPosOverlay.runOnFirstFix(new Runnable() {
            public void run() {
            	if (mMapView.getZoomLevel() < INITIAL_ZOOM)
            		mMapView.getController().setZoom(INITIAL_ZOOM);
            	mMapView.getController().animateTo(mMyPosOverlay.getMyLocation());
            }
        });
		mMapView.invalidate();
    }
    
    @Override
    protected void onResume() {
    	if (mMyPosOverlay == null)
    		BuildMap();
    	if (mMyPosOverlay != null && !mMyPosOverlay.isMyLocationEnabled())
    		mMyPosOverlay.enableMyLocation();
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	if (mMyPosOverlay != null)
    		mMyPosOverlay.disableMyLocation();
    	super.onPause();
    }
    
    private void StartSync() {
        SyncController sc = new SyncController(this);
        
        ProgressDialog pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setCancelable(false);
        pd.setTitle(R.string.menu_sync);
        pd.setOwnerActivity(this);
        sc.setProgressDialog(pd);
        sc.setOnCompleteListener(new Runnable() {			
			public void run() {
				BuildMap();
			}
		});
        new Thread(sc).start();
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}