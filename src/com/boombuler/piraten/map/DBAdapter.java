package com.boombuler.piraten.map;

import java.util.LinkedList;
import java.util.List;

import com.boombuler.piraten.map.data.PlakatOverlay;
import com.boombuler.piraten.map.data.PlakatOverlayItem;
import com.boombuler.piraten.map.data.ServerInfo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class DBAdapter {

	public static final String TABLE_PLAKATE = "plakate";

	public static final String PLAKATE_ID = "_id";
	public static final String PLAKATE_LAT = "lat";
	public static final String PLAKATE_LON = "lon";
	public static final String PLAKATE_TYPE = "type";
	public static final String PLAKATE_LAST_MODIFIED = "lastmod";
	public static final String PLAKATE_COMMENT = "comment";

	public static final String TABLE_CHANGES = "changes";
	public static final String CHANGES_ID = "_id";
	public static final String CHANGES_TYPE = "type";

	public static final int CHANGE_TYPE_NEW = 1;
	public static final int CHANGE_TYPE_CHANGED = 2;
	public static final int CHANGE_TYPE_DELETED = 3;

    public static final String TABLE_SERVERS = "servers";
    public static final String SERVERS_ID = "guid";
    public static final String SERVERS_NAME = "name";
    public static final String SERVERS_INFO = "info";
    public static final String SERVERS_URL = "url";
    public static final String SERVERS_DEV = "dev";

	private SQLiteDatabase mDatabase;
	private final Context mContext;
	private final DatabaseHelper mDBHelper;

	public DBAdapter(Context context) {
		mContext = context;
		mDBHelper = new DatabaseHelper(context);
	}

	public void open() {
		mDatabase = mDBHelper.getWritableDatabase();
	}

	public void close() {
		mDBHelper.close();
	}

	public void Insert(int id, int lat, int lon, int type, String lastMod,
			String comment) {
		ContentValues cv = new ContentValues();
		cv.put(PLAKATE_ID, id);
		cv.put(PLAKATE_LAT, lat);
		cv.put(PLAKATE_LON, lon);
		cv.put(PLAKATE_TYPE, type);
		cv.put(PLAKATE_LAST_MODIFIED, lastMod);
		cv.put(PLAKATE_COMMENT, comment);
		try {
			mDatabase.insert(TABLE_PLAKATE, null, cv);
		} catch (SQLiteException ex) {
			ex.printStackTrace();
		}
	}

    public void ClearServers() {
        mDatabase.delete(TABLE_SERVERS, null, null);
    }

    public void InsertServer(String id, String name, String info, String url) {
        ContentValues cv = new ContentValues();
        cv.put(SERVERS_ID, id);
        cv.put(SERVERS_URL, url);
        cv.put(SERVERS_INFO, info);
        cv.put(SERVERS_NAME, name);
        cv.put(SERVERS_DEV, 0);
        mDatabase.insert(TABLE_SERVERS, null, cv);
    }

    public void SetDevServer(String id) {
        ContentValues cv = new ContentValues();
        cv.put(SERVERS_DEV, 1);
        mDatabase.update(TABLE_SERVERS, cv, SERVERS_ID + "=?", new String[]{id});
    }

    public  List<ServerInfo> GetServers(boolean withDevServers) {
        LinkedList<ServerInfo> items = new LinkedList<ServerInfo>();

        Cursor crs = mDatabase.query(TABLE_SERVERS, null, null, null, null, null, null);
        if (crs != null) {
            try {
                if (crs.moveToFirst()) {
                    while (!crs.isAfterLast()) {
                        ServerInfo server = new ServerInfo(crs);
                        if (withDevServers || !server.isDevServer())
                            items.add(server);
                        crs.moveToNext();
                    }
                }
            } finally {
                crs.close();
            }
        }
        return items;
    }

	public void InsertNew(int lat, int lon, int type, String comment) {
		int newId = getNextId();
		Insert(newId, lat, lon, type, null, comment);
		try {
			ContentValues values = new ContentValues();
			values.put(CHANGES_ID, newId);
			values.put(CHANGES_TYPE, CHANGE_TYPE_NEW);
			mDatabase.insert(TABLE_CHANGES, null, values);
		} catch (SQLException ex) {
			ex.printStackTrace();
			// May fail cause we already have a row with "INSERT" as changetype
		}

	}

	public void Update(int id, int newType, String comment) {
		ContentValues cv = new ContentValues();
		cv.put(PLAKATE_TYPE, newType);
        if (comment != null)
            cv.put(PLAKATE_COMMENT, comment);
		mDatabase.update(TABLE_PLAKATE, cv, PLAKATE_ID + "=?",
				new String[] { String.valueOf(id) });
		try {
			if (getChangeType(id, CHANGE_TYPE_CHANGED) != CHANGE_TYPE_NEW) {
				mDatabase.delete(TABLE_CHANGES, CHANGES_ID + "=?", new String[] { String.valueOf(id) });
				ContentValues values = new ContentValues();
				values.put(CHANGES_ID, id);
				values.put(CHANGES_TYPE, CHANGE_TYPE_CHANGED);
				mDatabase.insert(TABLE_CHANGES, null, values);
			}
		} catch (SQLException ex) {
			// May fail cause we already have a row with "INSERT" as changetype
		}
	}

	public void getChangedItems(List<PlakatOverlayItem> inserted,
			List<PlakatOverlayItem> changed, List<Integer> deleted) {
		Cursor changes = mDatabase.query(TABLE_CHANGES, null, null, null, null,
				null, null);
		if (changes != null) {
			try {
				changes.moveToFirst();
				int idx_id = changes.getColumnIndex(CHANGES_ID);
				int idx_typ = changes.getColumnIndex(CHANGES_TYPE);

				while (!changes.isAfterLast()) {
					int id = changes.getInt(idx_id);
					int typ = changes.getInt(idx_typ);
					if (typ == CHANGE_TYPE_NEW)
						inserted.add(getOverlayItem(id));
					else if (typ == CHANGE_TYPE_CHANGED)
						changed.add(getOverlayItem(id));
					else if (typ == CHANGE_TYPE_DELETED)
						deleted.add(new Integer(id));

					changes.moveToNext();
				}
			} finally {
				changes.close();
			}
		}
	}

	public void delete(int id) {
		String[] sid = new String[] { String.valueOf(id) };
		mDatabase.delete(TABLE_PLAKATE, PLAKATE_ID + "=?", sid);
		if (getChangeType(id, CHANGE_TYPE_CHANGED) != CHANGE_TYPE_DELETED) {
			mDatabase.delete(TABLE_CHANGES, CHANGES_ID + "=?", sid);
			ContentValues values = new ContentValues();
			values.put(CHANGES_ID, id);
			values.put(CHANGES_TYPE, CHANGE_TYPE_DELETED);
			mDatabase.insert(TABLE_CHANGES, null, values);
		}
	}

	public void ClearData(int id) {
		String[] sid = new String[] { String.valueOf(id) };
		mDatabase.delete(TABLE_PLAKATE, PLAKATE_ID + "=?", sid);
		mDatabase.delete(TABLE_CHANGES, CHANGES_ID + "=?", sid);
	}
	
	public void ClearAllData() {
		mDatabase.delete(TABLE_PLAKATE, null, null);
		mDatabase.delete(TABLE_CHANGES, null, null);
	}

	public PlakatOverlayItem getOverlayItem(int id) {
		Cursor crs = mDatabase.query(TABLE_PLAKATE, null, PLAKATE_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null);
		try {
			if (crs != null && crs.moveToFirst()) {
				return loadFromCursor(crs);
			}
		} finally {
			crs.close();
		}
		return null;
	}

	private int getNextId() {
		Cursor dataCount = mDatabase.rawQuery("select max(" + PLAKATE_ID
				+ ") from " + TABLE_PLAKATE, null);
		dataCount.moveToFirst();
		try {
			return dataCount.getInt(0) + 1;
		} finally {
			dataCount.close();
		}
	}

	private int getChangeType(int id, int defaultValue) {
		Cursor dataCount = mDatabase.rawQuery("select " + CHANGES_TYPE
				+ " from " + TABLE_CHANGES + " WHERE " + CHANGES_ID + " = "
				+ id, null);

		dataCount.moveToFirst();
		try {
			if (!dataCount.isAfterLast())
				return dataCount.getInt(0);
			return defaultValue;
		} finally {
			dataCount.close();
		}
	}



	public PlakatOverlay getMapOverlay() {
		if (mContext instanceof PirateMap) {
			LinkedList<PlakatOverlayItem> items = new LinkedList<PlakatOverlayItem>();

			Cursor crs = mDatabase.query(TABLE_PLAKATE, null, null, null, null,
					null, null);
			if (crs != null) {
				try {
					if (crs.moveToFirst()) {
						while (!crs.isAfterLast()) {
							PlakatOverlayItem poi = loadFromCursor(crs);
							items.add(poi);
							crs.moveToNext();
						}
					}
				} finally {
					crs.close();
				}
			}
			return new PlakatOverlay((PirateMap) mContext, items);
		} else
			return null;
	}

	private PlakatOverlayItem loadFromCursor(Cursor crs) {
		int idx_id = crs.getColumnIndex(PLAKATE_ID);
		int idx_lat = crs.getColumnIndex(PLAKATE_LAT);
		int idx_lon = crs.getColumnIndex(PLAKATE_LON);
		int idx_type = crs.getColumnIndex(PLAKATE_TYPE);
		int idx_lastmod = crs.getColumnIndex(PLAKATE_LAST_MODIFIED);
		int idx_comment = crs.getColumnIndex(PLAKATE_COMMENT);

		return new PlakatOverlayItem(crs.getInt(idx_id), crs.getInt(idx_lat),
				crs.getInt(idx_lon), crs.getInt(idx_type),
				crs.getString(idx_lastmod), crs.getString(idx_comment));
	}

	
	public void beginTransaction() {
		mDatabase.beginTransaction();
	}
	public void setTransactionSuccessful() {
		mDatabase.setTransactionSuccessful();
	}
	public void endTransaction() {
		mDatabase.endTransaction();
	}
}
