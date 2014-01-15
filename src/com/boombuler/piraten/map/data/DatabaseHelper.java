package com.boombuler.piraten.map.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "plakate";
	private static final int DATABASE_VERSION = 2;
	
	
	private static final String PLAKATE_CREATE = "create table "+DBAdapter.TABLE_PLAKATE+" (" +
			DBAdapter.PLAKATE_ID + " integer primary key, " +
			DBAdapter.PLAKATE_LAT + " integer not null, " +
			DBAdapter.PLAKATE_LON + " integer not null, " +
			DBAdapter.PLAKATE_TYPE + " integer not null, "  +
			DBAdapter.PLAKATE_LAST_MODIFIED + " text, " +
			DBAdapter.PLAKATE_COMMENT + " text" +
			");";
	private static final String CHANGES_CREATE = "create table "+DBAdapter.TABLE_CHANGES + " (" +
			DBAdapter.CHANGES_ID + " integer primary key, " +
			DBAdapter.CHANGES_TYPE + " integer);";
    private static final String SERVERS_CREATE = "create table "+DBAdapter.TABLE_SERVERS + " (" +
            DBAdapter.SERVERS_ID + " text primary key," +
            DBAdapter.SERVERS_NAME + " text, " +
            DBAdapter.SERVERS_INFO + " text, " +
            DBAdapter.SERVERS_URL + " text, " +
            DBAdapter.SERVERS_DEV + " integer not null" +
            ");";

	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);	
	}

	
	// Method is called during creation of the database
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(CHANGES_CREATE);
		database.execSQL(PLAKATE_CREATE);
        database.execSQL(SERVERS_CREATE);
	}
	
	// Method is called during an upgrade of the database, e.g. if you increase
	// the database version
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
        if (oldVersion < 1) {
		    database.execSQL("DROP TABLE IF EXISTS "+DBAdapter.TABLE_PLAKATE);
		    database.execSQL("DROP TABLE IF EXISTS "+DBAdapter.TABLE_CHANGES);
            database.execSQL(CHANGES_CREATE);
            database.execSQL(PLAKATE_CREATE);
        }
        if (oldVersion < 2) {
            database.execSQL(SERVERS_CREATE);
        }
	}

}
