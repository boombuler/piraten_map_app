package com.boombuler.piraten.map;

import java.util.List;

import org.osmdroid.views.MapView;

import android.os.AsyncTask;

import com.boombuler.piraten.map.data.DBAdapter;
import com.boombuler.piraten.map.data.PlakatOverlay;
import com.boombuler.piraten.map.data.PlakatOverlayItem;
import com.boombuler.piraten.map.data.PlakatOverlayItemFilter;

public class PlakatLoadingTask extends AsyncTask<PlakatOverlayItemFilter, Void, Void> {

	private PirateMap context;
	private MapView mMapView;
	private PlakatOverlay plakatOverlay;
	
	public PlakatLoadingTask(final PirateMap context) {
		this.mMapView = context.getMapView();
		this.context = context;
	}
	
	@Override
	protected Void doInBackground(PlakatOverlayItemFilter... filters) {
		PlakatOverlayItemFilter filter = filters[0];
		
		DBAdapter dba = new DBAdapter(context);
		try {
			dba.open();
			List<PlakatOverlayItem> items = dba.getMapOverlayItems(filter);
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
		mMapView.getOverlays().add(context.getMyPosOverlay());
		mMapView.invalidate();

		super.onPostExecute(result);
	}
	
}
