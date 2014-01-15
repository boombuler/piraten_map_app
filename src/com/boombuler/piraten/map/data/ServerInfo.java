package com.boombuler.piraten.map.data;

import android.database.Cursor;

/**
 * Created with IntelliJ IDEA.
 * User: boombuler
 * Date: 03.08.13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
public class ServerInfo {

    private String mName;
    private String mInfo;
    private String mURL;
    private String mID;
    private boolean mDevServer;

    public ServerInfo(Cursor crs) {
        mID = crs.getString(crs.getColumnIndex(DBAdapter.SERVERS_ID));
        mURL = crs.getString(crs.getColumnIndex(DBAdapter.SERVERS_URL));
        mName = crs.getString(crs.getColumnIndex(DBAdapter.SERVERS_NAME));
        mInfo = crs.getString(crs.getColumnIndex(DBAdapter.SERVERS_INFO));
        mDevServer = crs.getInt(crs.getColumnIndex(DBAdapter.SERVERS_DEV)) == 1;
    }

    public boolean isDevServer() {
        return mDevServer;
    }

    public String getName() {
        return mName;
    }

    public String getURL() {
        return mURL;
    }

    public String getInfo() {
        return mInfo;
    }
}
