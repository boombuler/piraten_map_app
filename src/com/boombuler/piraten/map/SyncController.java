package com.boombuler.piraten.map;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SyncController implements Runnable {

	
	private static final String URL_SERVER = "http://piraten.boombuler.de/";
	private static final String URL_LOGIN = URL_SERVER + "login.php";
	private static final String URL_DATA = URL_SERVER + "json.php";
	
	private final DefaultHttpClient mClient;
	private final String mUsername;
	private final String mPassword;
	private final PirateMap mContext;
	private ProgressDialog mProgress;
	private Runnable mOnCompleteListener;
	
	public SyncController(PirateMap context) {
		mContext = context;
		mClient = new DefaultHttpClient();
		// Prevent redirection:
		mClient.setRedirectHandler(new RedirectHandler() {
			public boolean isRedirectRequested(HttpResponse response,
					HttpContext context) {
				return false;
			}
			
			public URI getLocationURI(HttpResponse response, HttpContext context)
					throws ProtocolException {
				return null;
			}
		});
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mUsername = prefs.getString(SettingsActivity.KEY_USERNAME, "");
		mPassword = prefs.getString(SettingsActivity.KEY_PASSWORD, "");
	}
	
	private boolean Login() {
		HttpPost post = new HttpPost(URL_LOGIN);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("username", mUsername));
        nameValuePairs.add(new BasicNameValuePair("password", mPassword));
        
		try {
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse rp = mClient.execute(post);
			Header head = rp.getFirstHeader("Location");
			if (head != null) {
				String location = head.getValue();
				if (location != null)
					return location.endsWith("Login OK") || location.endsWith("Success");
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		return false;
	}
	
	private void Logout() {
		HttpPost post = new HttpPost(URL_LOGIN);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("action", "logout"));
        
		try {
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			mClient.execute(post);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
	}

	private void LoadCurrentItems() {
		HttpPost post = new HttpPost(URL_DATA);
		DBAdapter dba = new DBAdapter(mContext);
		try {
			HttpResponse rp = mClient.execute(post);
			String response = EntityUtils.toString(rp.getEntity());

			dba.open();
			
			JSONParser parser = new JSONParser();
			PlakatJSONTransformer transformer = new PlakatJSONTransformer(dba);
		    parser.parse(response, transformer);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			dba.close();
		}
	}
	
	private void ClearData() {
		DBAdapter dba = new DBAdapter(mContext);
		try {
			dba.open();
			dba.ClearData();
		} finally {
			dba.close();
		}
	}
	
	private void SendChanges() {
		ArrayList<PlakatOverlayItem> inserted = new ArrayList<PlakatOverlayItem>();
		ArrayList<PlakatOverlayItem> changed = new ArrayList<PlakatOverlayItem>();
		ArrayList<Integer> deleted = new ArrayList<Integer>();
		DBAdapter dba = new DBAdapter(mContext);
		try {
			dba.open();			
			dba.GetChangedItems(inserted, changed, deleted);
		} finally {
			dba.close();
		}
		
		for(PlakatOverlayItem itm : inserted) {
			SendNewItem(itm);
		}
		for(PlakatOverlayItem itm : changed) {
			SendChangedItem(itm);
		}
		for(Integer id : deleted) {
			SendDeletedItem(id);
		}
	}
	
	private void SendNewItem(PlakatOverlayItem item) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("action", "add"));
        nameValuePairs.add(new BasicNameValuePair("typ", item.getTypeStr()));
        
        GeoPoint pt = item.getPoint();
        double lat = ((double)pt.getLatitudeE6()) / 1E6;
        double lon = ((double)pt.getLongitudeE6()) / 1E6;
        
        nameValuePairs.add(new BasicNameValuePair("lon", String.valueOf(lon)));
        nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(lat)));
        
        HttpGet get = new HttpGet(URL_DATA + "?" + URLEncodedUtils.format(nameValuePairs, "utf-8"));
		try {
			mClient.execute(get);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
	}
	
	private void SendDeletedItem(Integer id) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("action", "del"));
        nameValuePairs.add(new BasicNameValuePair("id", String.valueOf(id)));
        
        HttpGet get = new HttpGet(URL_DATA + "?" + URLEncodedUtils.format(nameValuePairs, "utf-8"));
		try {
			mClient.execute(get);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
	}
	
	private void SendChangedItem(PlakatOverlayItem item) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("action", "change"));
        nameValuePairs.add(new BasicNameValuePair("type", item.getTypeStr()));
        nameValuePairs.add(new BasicNameValuePair("id", String.valueOf(item.getId())));
        
        HttpGet get = new HttpGet(URL_DATA + "?" + URLEncodedUtils.format(nameValuePairs, "utf-8"));
		try {
			mClient.execute(get);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
	}
	
	public void setProgressDialog(ProgressDialog dialog) {
		mProgress = dialog;
	}

	private void SetProgressText(int textid) {
		final String txt = mContext.getString(textid);
		if (mProgress != null) {
			mContext.runOnUiThread(new Runnable() {
				public void run() {
					mProgress.setMessage(txt);
				}
			});
		}
	}
	
	private void ShowProgressDialog() {
		if (mProgress != null) {
			mContext.runOnUiThread(new Runnable() {
				public void run() {
					mProgress.show();
				}
			});
		}
	}
	
	private void CloseProgressDialog() {
		if (mProgress != null) {
			mContext.runOnUiThread(new Runnable() {
				public void run() {
					mProgress.dismiss();
				}
			});
		}
	}
	
	public void setOnCompleteListener(Runnable listener) {
		mOnCompleteListener = listener;
	}
	

	
	public void run() {
		SetProgressText(R.string.sync_login);
		ShowProgressDialog();
		boolean failed = false;
		try {
			if (Login()) {
				try {
					SetProgressText(R.string.sync_sending);
					SendChanges();
					
					SetProgressText(R.string.sync_cleardata);
					ClearData();
					
					SetProgressText(R.string.sync_loading);
					LoadCurrentItems();
				}
				finally {
					SetProgressText(R.string.sync_logout);
					Logout();
				}
				SetProgressText(R.string.sync_processing);
				if (mOnCompleteListener != null)
					mContext.runOnUiThread(mOnCompleteListener);
			} else {
				failed = true;
			}
		}
		finally {
			CloseProgressDialog();
		}
		if (failed) {
			final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setTitle(R.string.alert_error);
			ab.setMessage(R.string.error_login_failed);	
			mContext.runOnUiThread(new Runnable() {
				public void run() {
					final AlertDialog ad = ab.create();
					ad.setButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {						
						public void onClick(DialogInterface dialog, int which) {
							ad.dismiss();
						}
					});
					ad.show();
				}
			});
			
		}
	}
}
