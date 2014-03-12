package com.boombuler.piraten.map;

import java.util.List;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.boombuler.piraten.map.data.PlakatOverlay;
import com.boombuler.piraten.map.data.PlakatOverlayItem;
import com.boombuler.piraten.map.data.PlakatOverlayItemFilter;
import com.boombuler.piraten.map.fragments.FilterFragment;

public class PirateMap extends FragmentActivity {
	private MapView mMapView;
	private CurrentPositionOverlay mMyPosOverlay;
	protected PlakatOverlay plakatOverlay;
	private PlakatOverlayItemFilter mFilter = new PlakatOverlayItemFilter();
	static int INITIAL_ZOOM = 16;
	private boolean initialMoveToLocationPerformed = false;
	
	
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
    	if (requestCode == Constants.REQ_DETAILS && resultCode == RESULT_OK)
    		buildMap(mFilter); // Something changed so reload!
    	else if (requestCode == Constants.REQ_FILTER && resultCode == Constants.RES_FILTER) {
    		mFilter = data.getExtras().getParcelable(Constants.EXTRA_ITEMFILTER);
    		buildMap(mFilter); // Something changed so reload!
    	}
    		
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:
            	StartSync();
                return true;
            case R.id.menu_add:
            	addMarker();
				return true;
            case R.id.menu_my_location:
            	moveToMyLocation();
            	return true;
            case R.id.menu_filter:
            	openfilterDialog();
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
            case R.id.menu_about:
				startActivity(new Intent(PirateMap.this, AboutActivity.class));
				return true;
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
    
    public void buildMap(PlakatOverlayItemFilter mFilter) {
    	final List<Overlay> overlays = mMapView.getOverlays();
		overlays.clear();
    	
    	new PlakatLoadingTask(this).execute(mFilter);
    	
    	mMyPosOverlay = new CurrentPositionOverlay(PirateMap.this, mMapView);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean returnToMyLocation = initialMoveToLocationPerformed ? prefs.getBoolean(SettingsActivity.KEY_RETURN_TO_MY_LOCATION, true) : true;
		mMyPosOverlay.moveToMyPosition(this, returnToMyLocation );
		initialMoveToLocationPerformed = true; // couldn't think of a better way :/
		mMyPosOverlay.enable();
		mMapView.invalidate();
    }
    
    private void moveToMyLocation() {
    	GeoPoint location = mMyPosOverlay.getMyLocation();
    	if(mMyPosOverlay != null && location != null) {
    		mMapView.getController().animateTo(location);
    	}
    	
    }
    
    @Override
    protected void onResume() {
    	if (mMyPosOverlay == null)
    		buildMap(mFilter);
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
    
    private void openfilterDialog(){
    	// Create the fragment and show it as a dialog.
    	FragmentManager fragmentManager = getSupportFragmentManager();
	    FilterFragment newFragment = new FilterFragment();
	    newFragment.setFilter(mFilter);
	    newFragment.show(fragmentManager, "filterDialog");
    }

    private void addMarker() {
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
                    Constants.REQ_DETAILS);
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
                buildMap(mFilter);
            }
        });
        sc.synchronize();
    }
}