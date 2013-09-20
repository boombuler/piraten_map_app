package com.boombuler.piraten.map;

import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;

import android.content.ClipData.Item;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;


public class PlakatOverlay extends ItemizedOverlay<PlakatOverlayItem> {

	private List<PlakatOverlayItem> mItems;
	private final PirateMap mContext;
	private Point mCurScreenCoords = new Point();
	
	public PlakatOverlay(PirateMap context, List<PlakatOverlayItem> items) {
		super(PlakatOverlayItem.getDefaultDrawable(), new DefaultResourceProxyImpl(context));
		mContext = context;
		if (items != null) {
			mItems = items;
			populate();
		}
	}
	
	@Override
	protected void drawSafe(ISafeCanvas canvas, MapView mapView, boolean shadow) {

		if (shadow) {
			return;
		}

		final Projection pj = mapView.getProjection();
		final int size = this.size() - 1;

		/* Draw in backward cycle, so the items with the least index are on the front. */
		for (int i = size; i >= 0; i--) {
			final PlakatOverlayItem item = getItem(i);
			pj.toMapPixels(item.getPoint(), mCurScreenCoords);

			if (mapView.getProjection().getBoundingBox().increaseByScale(1.2f).contains(item.getPoint())) {				
				onDrawItem((Canvas) canvas, item, mCurScreenCoords);
			}
			
		}
	}

	@Override
	protected PlakatOverlayItem createItem(int i) {
		return mItems.get(i);
	}

	@Override
	public int size() {
		return mItems.size();
	}

    @Override
    protected boolean onTap(int index) {
    	PlakatOverlayItem item = mItems.get(index);
    	mContext.startActivityForResult(
    			new Intent(mContext, PlakatDetailsActivity.class)
    				.putExtra(PlakatDetailsActivity.EXTRA_PLAKAT_ID, item.getId()),
    				PirateMap.REQUEST_EDIT_PLAKAT);
    	
    	return true;
    }
/*
	public static void Prepare(Drawable[] mIcons) {
		for(int i = 0; i < mIcons.length; i++)
			mIcons[i] = PlakatOverlay. boundCenter(mIcons[i]);
	}
  */

    @Override
    public boolean onSnapToItem(int i, int i2, Point point, IMapView iMapView) {
        return false;
    }
}
