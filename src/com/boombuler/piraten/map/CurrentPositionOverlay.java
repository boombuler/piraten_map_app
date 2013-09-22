package com.boombuler.piraten.map;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;

import android.content.Context;


public class CurrentPositionOverlay extends MyLocationOverlay {

	public CurrentPositionOverlay(Context context, MapView map) {
		super(context, map);
	}

	public void enable() {
		if (!isCompassEnabled())
			enableCompass();
		if (!isMyLocationEnabled())
			enableMyLocation();
	}

	public void disable() {
		if (isCompassEnabled())
			disableCompass();
		if (isMyLocationEnabled())
			disableMyLocation();
	}

	public void moveToMyPosition(final PirateMap context, final boolean returnToMyLocation) {
		runOnFirstFix(new Runnable() {
			public void run() {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mMapView.getZoomLevel() < PirateMap.INITIAL_ZOOM)
							mMapView.getController().setZoom(PirateMap.INITIAL_ZOOM);
						if(returnToMyLocation)
							mMapView.getController().animateTo(getMyLocation());
					}
				});
			}
		});
	}
}
