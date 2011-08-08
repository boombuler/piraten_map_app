package com.boombuler.piraten.map;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;


public class PlakatJSONTransformer implements ContentHandler {
	Map<String, Object> mCurrentItem = null;
	String mCurrentKey;
	Object mCurrentValue;
	
	final DBAdapter mAdapter;
	
	public PlakatJSONTransformer(DBAdapter adapter) {
		mAdapter = adapter;
	}
	

	public boolean endArray() throws ParseException, IOException {
		return true;
	}

	public void endJSON() throws ParseException, IOException {
	}

	public boolean endObject() throws ParseException, IOException {
		// Translate the object:
		// {"id":"1509","lon":"8.06251265","lat":"52.2801436","type":"plakat_ok","user":"KP","timestamp":"2011-07-30 19:52:59","comment":"","image":null}
		int id = Integer.parseInt((String)mCurrentItem.get("id"));
		int lon = (int)(Double.parseDouble((String)mCurrentItem.get("lon")) * 1E6);
		int lat = (int)(Double.parseDouble((String)mCurrentItem.get("lat")) * 1E6);
		int type = PlakatOverlayItem.TypeToTypeId((String)mCurrentItem.get("type"));
		String lastMod = (String)mCurrentItem.get("user");
		String comment = (String)mCurrentItem.get("comment");
		
		mAdapter.Insert(id, lat, lon, type, lastMod, comment);
		
		return true;
	}

	public boolean endObjectEntry() throws ParseException, IOException {
		mCurrentItem.put(mCurrentKey, mCurrentValue);
		return true;
	}

	public boolean primitive(Object arg0) throws ParseException, IOException {
		mCurrentValue = arg0;
		return true;
	}

	public boolean startArray() throws ParseException, IOException {
		return true;
	}

	public void startJSON() throws ParseException, IOException {
		mCurrentItem = new HashMap<String, Object>();
	}

	public boolean startObject() throws ParseException, IOException {
		mCurrentItem.clear();
		return true;
	}

	public boolean startObjectEntry(String arg0) throws ParseException,
			IOException {
		mCurrentKey = arg0;
		return true;
	}
}
