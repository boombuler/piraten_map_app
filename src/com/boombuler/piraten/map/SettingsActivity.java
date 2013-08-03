package com.boombuler.piraten.map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {

	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_ACCURACY = "accuracy";
	public static final String KEY_SYNC_RANGE = "sync_range";
	public static final String KEY_SERVER = "sync_server";
    public static final  String KEY_HAS_SYNCED = "has_synced_before";
	private static final String KEY_ABOUT_SERVER = "about_server";
	
	private ListPreference mServerPref;
    private List<ServerInfo> mServerList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        mServerPref = (ListPreference)findPreference(KEY_SERVER);

        FetchServerList.Load(this, new Runnable() {
            @Override
            public void run() {
                DBAdapter dba = new DBAdapter(SettingsActivity.this);
                dba.open();
                try {
                    mServerList = dba.GetServers(false);
                }
                finally {
                    dba.close();
                }

                String[] serverNames = new String[mServerList.size()];
                String[] serverURLs = new String[mServerList.size()];

                for (int i = 0; i < mServerList.size(); i++) {
                    serverNames[i] = mServerList.get(i).getName();
                    serverURLs[i] = mServerList.get(i).getURL();
                }
                mServerPref.setEntries(serverNames);
                mServerPref.setEntryValues(serverURLs);
                mServerPref.setOnPreferenceChangeListener(SettingsActivity.this);
            }
        });

		findPreference(KEY_ABOUT_SERVER).setOnPreferenceClickListener(this);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			if (item.getItemId() == android.R.id.home) {
				this.finish();
				return true;
			}
	    }
		return super.onOptionsItemSelected(item);
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		final String nv = (String)newValue;
        final Preference pref = preference;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        if (prefs.getString(KEY_SERVER, "").equals(nv))
        	return false;
        
		new AlertDialog.Builder(this)
		.setMessage(getString(R.string.ask_server_change))
		.setTitle(android.R.string.dialog_alert_title)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setPositiveButton(android.R.string.yes, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(KEY_HAS_SYNCED, false);
				edit.putString(KEY_SERVER, nv);
				DBAdapter dba = new DBAdapter(SettingsActivity.this);
				try {
					dba.open();
					dba.ClearAllData();
				} finally {
                    dba.close();
				}
				((ListPreference)pref).setValue(nv);
				edit.commit();
			}
		}).setNegativeButton(android.R.string.no, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).setCancelable(true)
		.show();
		return false;
	}

	private String getServerInfo() {
		String serv = mServerPref.getValue();
        for (ServerInfo si : mServerList) {
            if (si.getURL() == serv) {
                return si.getInfo();
            }
        }
		return null;
	}
	
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(KEY_ABOUT_SERVER)) {
			Spanned htmltext = Html.fromHtml(getServerInfo());
			AlertDialog dlg = new AlertDialog.Builder(this)
				.setMessage(htmltext)
				.setTitle(R.string.about_server)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(android.R.string.ok, null)
				.show();
			((TextView)dlg.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			
		}
		return false;
	}
}
