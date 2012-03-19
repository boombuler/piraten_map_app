package com.boombuler.piraten.map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_ACCURACY = "accuracy";
	public static final String KEY_SYNC_RANGE = "sync_range";
	public static final String KEY_SERVER = "sync_server";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		findPreference(KEY_SERVER).setOnPreferenceChangeListener(this);
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
        
        if (prefs.getString(KEY_SERVER, getString(R.string.default_server)).equals(nv))
        	return false;
        
		new AlertDialog.Builder(this)
		.setMessage(getString(R.string.ask_server_change))
		.setPositiveButton(android.R.string.yes, new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				SharedPreferences.Editor edit = prefs.edit();
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
}
