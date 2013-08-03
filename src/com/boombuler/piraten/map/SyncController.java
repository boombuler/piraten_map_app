package com.boombuler.piraten.map;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.boombuler.piraten.map.proto.Api.AddRequest;
import com.boombuler.piraten.map.proto.Api.BoundingBox;
import com.boombuler.piraten.map.proto.Api.ChangeRequest;
import com.boombuler.piraten.map.proto.Api.DeleteRequest;
import com.boombuler.piraten.map.proto.Api.Plakat;
import com.boombuler.piraten.map.proto.Api.Request;
import com.boombuler.piraten.map.proto.Api.Response;
import com.boombuler.piraten.map.proto.Api.ViewRequest;
import com.boombuler.piraten.utils.MyHttpClient;
import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class SyncController implements Runnable {
	private static final String TAG = "boombuler.synccontroller";
	
		
	private final String mAPIUrl;
	private static final int BATCH_SIZE = 100;
	
	private final DefaultHttpClient mClient;
	private final String mUsername;
	private final String mPassword;
	private final PirateMap mContext;
	private ProgressDialog mProgress;
	private Runnable mOnCompleteListener;
	private Location mLocation = null;
	private double mSyncRange = 0;
	
	public SyncController(PirateMap context) {
		mContext = context;
		mClient = new MyHttpClient(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mUsername = prefs.getString(SettingsActivity.KEY_USERNAME, "");
		mPassword = prefs.getString(SettingsActivity.KEY_PASSWORD, "");
		mSyncRange = Double.parseDouble(prefs.getString(SettingsActivity.KEY_SYNC_RANGE, "0"));
		mAPIUrl = prefs.getString(SettingsActivity.KEY_SERVER, "") + "api.php";
		Log.d(TAG, "using server: "+mAPIUrl);
	}
	
	
	
	private double checkBoundsLatitude(double value) {
		value = Math.max(value, -90);
		return Math.min(value, +90);
	}
	
	private double checkBoundsLongitude(double value) {
		while (value < -180 || value > 180) {
			if (value < -180)
				value += 360;
			if (value > 180)
				value -= 360;
		}
		return value;
	}
	
	private BoundingBox.Builder getBoundingBox() {
		BoundingBox.Builder bbox = BoundingBox.newBuilder();
		
		double yDiff = mSyncRange / 111120f;
		bbox.setNorth(checkBoundsLatitude(mLocation.getLatitude() + yDiff));
		bbox.setSouth(checkBoundsLatitude(mLocation.getLatitude() - yDiff));
		
		double xDiff = yDiff / Math.cos(Math.toRadians(mLocation.getLatitude()));
		
		bbox.setWest(checkBoundsLongitude(mLocation.getLongitude() - xDiff));
		bbox.setEast(checkBoundsLongitude(mLocation.getLongitude() + xDiff));
		
		return bbox;
	}
	
	private boolean RunRequest() {
		Request.Builder builder = Request.newBuilder()
				.setUsername(mUsername).setPassword(mPassword);
		if (mLocation != null && mSyncRange > 0) { // Check for ViewBox
			ViewRequest.Builder view = ViewRequest.newBuilder();
			view.setViewBox(getBoundingBox());
			builder.setViewRequest(view);
		}
		
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
			builder.addAdd(SendNewItem(itm));
		}
		for(PlakatOverlayItem itm : changed) {
			builder.addChange(SendChangedItem(itm));
		}
		for(Integer id : deleted) {
			builder.addDelete(DeleteRequest.newBuilder().setId(id).build());
		}
		
		Request request = builder.build();
		
		HttpPost post = new HttpPost(mAPIUrl);
		
		post.setEntity(new ByteArrayEntity(request.toByteArray()));
		
		try {
			HttpResponse rp = mClient.execute(post);
			byte[] res = EntityUtils.toByteArray(rp.getEntity());

			Response response = Response.parseFrom(res);
			
			int addedCnt = response.hasAddedCount() ? response.getAddedCount() : 0;
			int changedCnt = response.hasChangedCount() ? response.getChangedCount() : 0;
			int deletedCnt = response.hasDeletedCount() ? response.getDeletedCount() : 0;
			
			if (addedCnt == inserted.size() &&
				changedCnt == changed.size() &&
				deletedCnt == deleted.size()) {
				SetProgressText(R.string.sync_cleardata);
				ClearAllData();
				LoadItems(response);
				return true;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
            e.printStackTrace();
        }
		return false;
	}

	private void LoadItems(Response resp) {
		SetProgressText(R.string.sync_loading);
		DBAdapter dba = new DBAdapter(mContext);
		try {
			dba.open();
			dba.beginTransaction();
			
			int insertCount = 0;
			
			for(Plakat plakat : resp.getPlakateList()) {
				int lon = (int)(plakat.getLon() * 1E6);
				int lat = (int)(plakat.getLat() * 1E6);
				int type = PlakatOverlayItem.TypeToTypeId(plakat.getType());
				
				dba.Insert(plakat.getId(),
						lat, lon,type,
						plakat.getLastModifiedUser(),
						plakat.getComment());
				insertCount++;
				if (insertCount >= BATCH_SIZE) {
					dba.setTransactionSuccessful();
					dba.endTransaction();
					dba.beginTransaction();
					insertCount = 0;
				}
			}
			dba.setTransactionSuccessful();
		} finally {
			dba.endTransaction();
			dba.close();
		}
	}

	private void ClearAllData() {
		DBAdapter dba = new DBAdapter(mContext);
		try {
			dba.open();
			dba.ClearAllData();
		} finally {
			dba.close();
		}
	}
		
	private AddRequest SendNewItem(PlakatOverlayItem item) {
		AddRequest.Builder result = AddRequest.newBuilder();
		result.setType(item.getTypeStr());
		        
        GeoPoint pt = item.getPoint();
        double lat = ((double)pt.getLatitudeE6()) / 1E6;
        double lon = ((double)pt.getLongitudeE6()) / 1E6;
        
        result.setLat(lat);
        result.setLon(lon);
        result.setComment(item.getComment());
        return result.build();
	}
	
	private ChangeRequest SendChangedItem(PlakatOverlayItem item) {
		ChangeRequest.Builder result = ChangeRequest.newBuilder();
		
		result.setId(item.getId());
		result.setType(item.getTypeStr());
        result.setComment(item.getComment());
        
		return result.build();
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
		SetProgressText(R.string.sync_sending);
		ShowProgressDialog();
		boolean failed = false;
		try {
			if (RunRequest()) {
				SetProgressText(R.string.sync_processing);
				if (mOnCompleteListener != null)
					mContext.runOnUiThread(mOnCompleteListener);
			}	
			else
				failed = true;
		}
		finally {
			CloseProgressDialog();
		}
		if (failed) {
			final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
			ab.setIcon(android.R.drawable.ic_dialog_alert);
			ab.setTitle(R.string.alert_error);
			ab.setMessage(R.string.error_sync_failed);	
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

	private void StartSync() {
		
		mProgress = new ProgressDialog(mContext);
		mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgress.setCancelable(false);
		mProgress.setTitle(R.string.menu_sync);
		mProgress.setOwnerActivity(mContext);

        new Thread(this).start();
	}
	
	public void synchronize() {
		
		if (mSyncRange <= 0) {
			StartSync();
			return;
		}
		
		final ProgressDialog progressDlg = new ProgressDialog(mContext);

		final LocationListener ll = new LocationListener() {
			
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			public void onProviderEnabled(String provider) {
			}
			
			public void onProviderDisabled(String provider) {
			}
			
			public void onLocationChanged(Location location) {
				if (location.getAccuracy() <= 100) {
					progressDlg.dismiss();
					mLocation = location;
					StartSync();
				}
			}
		}; 
		final LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		
		
		progressDlg.setOwnerActivity(mContext);
		progressDlg.setCancelable(true);
		progressDlg.setCanceledOnTouchOutside(false);
		progressDlg.setIndeterminate(true);
		progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDlg.setTitle(R.string.menu_sync);
		progressDlg.setMessage(mContext.getString(R.string.get_position));
		progressDlg.setButton(mContext.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				progressDlg.cancel();
			}
		});
		progressDlg.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				lm.removeUpdates(ll);				
			}
		});
		progressDlg.setOnDismissListener(new OnDismissListener() {			
			public void onDismiss(DialogInterface dialog) {
				lm.removeUpdates(ll);
			}
		});
		progressDlg.show();
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
		    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);
	}


}
