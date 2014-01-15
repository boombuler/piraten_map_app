package com.boombuler.piraten.map.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class PlakatOverlayItemFilter implements Serializable, Parcelable {
	private static final long serialVersionUID = -2586431607660575426L;

	private String sourceId = null;
	public static final int ANY_ITEMS = -1;
	
	private boolean sorter = false;
	
	private String[] projectionColumns = null;

	private int typeSelected = ANY_ITEMS;
	
	public PlakatOverlayItemFilter() {
	}
	
	public boolean doesFilter(){
		return typeSelected!=ANY_ITEMS;
	}

	
	
	public String where() {
		StringBuilder where = new StringBuilder();


		if (doesFilter()) {
		
			if (typeSelected != ANY_ITEMS) {
				if (where.length()>0) where.append(" AND ");
				where.append(DBAdapter.PLAKATE_TYPE).append("=?");
			}
		
		}
		
		return where.toString();
	}
	public String[] whereVales() {
		List<String> values = new ArrayList<String>();
		if (sourceId!=null) values.add(sourceId);

		if (doesFilter()) {
					
			if (typeSelected!=ANY_ITEMS) {
				if (typeSelected==PlakatOverlayItem.INT_PLAKAT_A0) values.add(Integer.toString(PlakatOverlayItem.INT_PLAKAT_A0));
				else if ((typeSelected==PlakatOverlayItem.INT_PLAKAT_DIEB)) values.add(Integer.toString(PlakatOverlayItem.INT_PLAKAT_DIEB));
				else if ((typeSelected==PlakatOverlayItem.INT_PLAKAT_NICE_PLACE)) values.add(Integer.toString(PlakatOverlayItem.INT_PLAKAT_NICE_PLACE));
				else if ((typeSelected==PlakatOverlayItem.INT_PLAKAT_OK)) values.add(Integer.toString(PlakatOverlayItem.INT_PLAKAT_OK));
				else if ((typeSelected==PlakatOverlayItem.INT_PLAKAT_WRECKED)) values.add(Integer.toString(PlakatOverlayItem.INT_PLAKAT_WRECKED));
				else if ((typeSelected==PlakatOverlayItem.INT_WAND)) values.add(Integer.toString(PlakatOverlayItem.INT_WAND));
				else if ((typeSelected==PlakatOverlayItem.INT_WAND_OK)) values.add(Integer.toString(PlakatOverlayItem.INT_WAND_OK));
				else if ((typeSelected==PlakatOverlayItem.INT_DEFAULT)) values.add(Integer.toString(PlakatOverlayItem.INT_DEFAULT));
			}
		}
		
		return values.toArray(new String[0]);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(this);
	}
	
	public void setPreferenceSelected(int preferenceSelected) {
		this.typeSelected = preferenceSelected;
	}

	public int getPreferenceSelected() {
		return typeSelected;
	}
	public void setProjectionColumns(String[] projectionColumns) {
		this.projectionColumns = projectionColumns;
	}

	public String[] getProjectionColumns() {
		return projectionColumns;
	}

	public boolean isSorter() {
		return sorter;
	}
	
	
	public static final Parcelable.Creator<PlakatOverlayItemFilter> CREATOR = new Parcelable.Creator<PlakatOverlayItemFilter>() {
		public PlakatOverlayItemFilter createFromParcel(Parcel in) {
			return (PlakatOverlayItemFilter)in.readSerializable();
		}
		public PlakatOverlayItemFilter[] newArray(int size) {
			return new PlakatOverlayItemFilter[size];
		}
	};

}
