package com.boombuler.piraten.map;

import android.content.Context;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class CurrentPositionOverlay extends MyLocationOverlay  {

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
}
