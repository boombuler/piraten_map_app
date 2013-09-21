package com.boombuler.piraten.map;

import java.util.List;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.os.AsyncTask;

import com.boombuler.piraten.map.data.PlakatOverlay;
import com.boombuler.piraten.map.data.PlakatOverlayItem;

public class PlakatLoadingTask extends AsyncTask<Void, Void, Void> {

	private PirateMap context;
	private MapView mMapView;
	private PlakatOverlay plakatOverlay;
	
	public PlakatLoadingTask(final PirateMap context) {
		this.mMapView = context.getMapView();
		this.context = context;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		
		DBAdapter dba = new DBAdapter(context);
		try {
			dba.open();
			List<PlakatOverlayItem> items = dba.getMapOverlayItems();
			plakatOverlay = new PlakatOverlay(context, items);
			mMapView.getOverlays().add(plakatOverlay);
		} finally {
			dba.close();
		}
		
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		
		context.setPlakatOverlay(plakatOverlay);
		mMapView.invalidate();

		super.onPostExecute(result);
	}
	
}
