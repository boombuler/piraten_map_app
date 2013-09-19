package com.boombuler.piraten.map.data;

import android.graphics.drawable.Drawable;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

public class PlakatOverlayItem extends OverlayItem {

	private static final String STR_PLAKAT_OK = "plakat_ok";
	private static final String STR_PLAKAT_DIEB = "plakat_dieb";
	private static final String STR_PLAKAT_NICE_PLACE = "plakat_niceplace";
	private static final String STR_WAND = "wand";
	private static final String STR_WAND_OK = "wand_ok";
	private static final String STR_PLAKAT_WRECKED = "plakat_wrecked";
	private static final String STR_PLAKAT_A0 = "plakat_a0";

	protected static final int INT_DEFAULT = 0;
	protected static final int INT_PLAKAT_OK = 1;
	protected static final int INT_PLAKAT_DIEB = 2;
	protected static final int INT_PLAKAT_NICE_PLACE = 3;
	protected static final int INT_WAND = 4;
	protected static final int INT_WAND_OK = 5;
	protected static final int INT_PLAKAT_WRECKED = 6;
	protected static final int INT_PLAKAT_A0 = 7;
	
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
		case INT_PLAKAT_A0: return STR_PLAKAT_A0;
		}
		return "";
	}
	
	public String getComment() {
		return mComment;
	}
	
	public static void InitResources(Drawable p_default, Drawable p_ok, Drawable p_dieb, Drawable p_nice, Drawable w_default, Drawable w_ok, Drawable p_wracked, Drawable p_a0)
	{
		MIcons = new Drawable[] { p_default, p_ok, p_dieb, p_nice, w_default, w_ok, p_wracked, p_a0 };
		//PlakatOverlay.Prepare(MIcons);
	}
	
	public static Drawable getDefaultDrawable() {
		return MIcons[INT_DEFAULT];
	}
	
	public PlakatOverlayItem(int id, int lat, int lon, int type, String lastMod, String comment) {
		super(null, null, new GeoPoint(lat, lon));
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
		else if (STR_PLAKAT_A0.equals(type))
			return INT_PLAKAT_A0;
		else
			return INT_DEFAULT;
	}
	
}
