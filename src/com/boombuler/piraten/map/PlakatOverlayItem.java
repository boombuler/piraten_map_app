package com.boombuler.piraten.map;

import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class PlakatOverlayItem extends OverlayItem {

	private static final String STR_PLAKAT_OK = "plakat_ok";
	private static final String STR_PLAKAT_DIEB = "plakat_dieb";
	private static final String STR_PLAKAT_NICE_PLACE = "plakat_niceplace";
	private static final String STR_WAND = "wand";
	private static final String STR_WAND_OK = "wand_ok";
	private static final String STR_PLAKAT_WRECKED = "plakat_wrecked";

	private static final int INT_DEFAULT = 0;
	private static final int INT_PLAKAT_OK = 1;
	private static final int INT_PLAKAT_DIEB = 2;
	private static final int INT_PLAKAT_NICE_PLACE = 3;
	private static final int INT_WAND = 4;
	private static final int INT_WAND_OK = 5;
	private static final int INT_PLAKAT_WRECKED = 6;
	
	final int mId;
	final String mLastModified;
	String mComment;
	int mType;
	
	private static Drawable[] MIcons;
	
	public int getId() {
		return mId;
	}
	
	public int getType() {
		return mType;
	}
	
	public String getTypeStr() {
		switch(mType) {
		case INT_DEFAULT: return "";
		case INT_PLAKAT_OK: return STR_PLAKAT_OK;
		case INT_PLAKAT_DIEB: return STR_PLAKAT_DIEB;
		case INT_PLAKAT_NICE_PLACE: return STR_PLAKAT_NICE_PLACE;
		case INT_WAND: return STR_WAND;
		case INT_WAND_OK: return STR_WAND_OK;
		case INT_PLAKAT_WRECKED: return STR_PLAKAT_WRECKED;
		}
		return "";
	}
	
	public String getComment() {
		return mComment;
	}
	
	public static void InitResources(Drawable p_default, Drawable p_ok, Drawable p_dieb, Drawable p_nice, Drawable w_default, Drawable w_ok, Drawable p_wracked)
	{
		MIcons = new Drawable[] { p_default, p_ok, p_dieb, p_nice, w_default, w_ok, p_wracked };
		PlakatOverlay.Prepare(MIcons);
	}
	
	public static Drawable getDefaultDrawable() {
		return MIcons[INT_DEFAULT];
	}
	
	public PlakatOverlayItem(int id, int lat, int lon, int type, String lastMod, String comment) {
		super(new GeoPoint(lat, lon), null, null);
		mLastModified = lastMod;
		mId = id;
		
		mComment = comment;
		mType = type;
		setMarker(MIcons[mType]);
	}
	
	public static int TypeToTypeId(String type) {
		if (STR_PLAKAT_OK.equals(type))
			return INT_PLAKAT_OK;
		else if (STR_PLAKAT_DIEB.equals(type))
			return INT_PLAKAT_DIEB;
		else if (STR_PLAKAT_NICE_PLACE.equals(type))
			return INT_PLAKAT_NICE_PLACE;
		else if (STR_WAND.equals(type))
			return INT_WAND;
		else if (STR_WAND_OK.equals(type))
			return INT_WAND_OK;
		else if (STR_PLAKAT_WRECKED.equals(type))
			return INT_PLAKAT_WRECKED;
		else
			return INT_DEFAULT;
	}
	
}
