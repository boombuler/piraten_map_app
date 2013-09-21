package com.boombuler.piraten.map;

import java.util.List;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.boombuler.piraten.map.data.PlakatOverlay;
import com.boombuler.piraten.map.data.PlakatOverlayItem;

public class PirateMap extends Activity {
	private MapView mMapView;
	private CurrentPositionOverlay mMyPosOverlay;
	protected PlakatOverlay plakatOverlay;
	public static int REQUEST_EDIT_PLAKAT = 1;
	static int INITIAL_ZOOM = 16;
	
	public MapView getMapView() {
		return mMapView;
	}
	
    public CurrentPositionOverlay getMyPosOverlay() {
		return mMyPosOverlay;
	}

	public void setMyPosOverlay(CurrentPositionOverlay mMyPosOverlay) {
		this.mMyPosOverlay = mMyPosOverlay;
	}

	public PlakatOverlay getPlakatOverlay() {
		return plakatOverlay;
	}

	public void setPlakatOverlay(PlakatOverlay plakatOverlay) {
		this.plakatOverlay = plakatOverlay;
	}

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
        mMapView.setMultiTouchControls(true);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQUEST_EDIT_PLAKAT && resultCode == RESULT_OK)
    		buildMap(); // Something changed so reload!
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:
            	StartSync();
                return true;
            case R.id.menu_add:
            	AddMarker();
				return true;
            case R.id.menu_my_location:
            	moveToMyLocation();
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
    
    private void buildMap() {
    	final List<Overlay> overlays = mMapView.getOverlays();
		overlays.clear();
    	
    	new PlakatLoadingTask(this).execute();
    	
    	mMyPosOverlay = new CurrentPositionOverlay(PirateMap.this, mMapView);
		mMyPosOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				PirateMap.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mMapView.getZoomLevel() < INITIAL_ZOOM)
							mMapView.getController().setZoom(INITIAL_ZOOM);
						mMapView.getController().animateTo(mMyPosOverlay.getMyLocation());
					}
				});
			}
		});
		mMyPosOverlay.enable();
		mMapView.invalidate();
    }
    
    private void moveToMyLocation() {
    	mMapView.getController().animateTo(mMyPosOverlay.getMyLocation());
    }
    
    @Override
    protected void onResume() {
    	if (mMyPosOverlay == null)
    		buildMap();
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

    private void AddMarker() {
        boolean hasSyncedBefore = PreferenceManager.getDefaultSharedPreferences(this)
                                                   .getBoolean(SettingsActivity.KEY_HAS_SYNCED, false);
        if (!hasSyncedBefore) {
            new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.warn_sync_first)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        } else {
            startActivityForResult(
                    new Intent(PirateMap.this, PlakatDetailsActivity.class)
                            .putExtra(PlakatDetailsActivity.EXTRA_NEW_PLAKAT, true),
                    PirateMap.REQUEST_EDIT_PLAKAT);
        }
    }
    
    private void StartSync() {
        SyncController sc = new SyncController(this);
        
        sc.setOnCompleteListener(new Runnable() {
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PirateMap.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(SettingsActivity.KEY_HAS_SYNCED, true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    editor.apply();
                } else {
                    editor.commit();
                }
                buildMap();
            }
        });
        sc.synchronize();
    }
}