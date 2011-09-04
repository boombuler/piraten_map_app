package com.boombuler.piraten.map;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.boombuler.piraten.map.proto.Api.AddRequest;
import com.boombuler.piraten.map.proto.Api.ChangeRequest;
import com.boombuler.piraten.map.proto.Api.DeleteRequest;
import com.boombuler.piraten.map.proto.Api.Plakat;
import com.boombuler.piraten.map.proto.Api.Request;
import com.boombuler.piraten.map.proto.Api.Response;
import com.boombuler.piraten.utils.InflatingEntity;
import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SyncController implements Runnable {
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	
	private static final String URL_SERVER = "http://piraten.boombuler.de/";	
	private static final String URL_API = URL_SERVER + "api.php";
	
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
		ActivateGZipSupport();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mUsername = prefs.getString(SettingsActivity.KEY_USERNAME, "");
		mPassword = prefs.getString(SettingsActivity.KEY_PASSWORD, "");
	}
	
	private void ActivateGZipSupport() {
		mClient.addRequestInterceptor(new HttpRequestInterceptor() {
			  public void process(HttpRequest request, HttpContext context) {
			    // Add header to accept gzip content
			    if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
			      request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
			    }
			  }
			});

			mClient.addResponseInterceptor(new HttpResponseInterceptor() {
			  public void process(HttpResponse response, HttpContext context) {
			    // Inflate any responses compressed with gzip
			    final HttpEntity entity = response.getEntity();
			    final Header encoding = entity.getContentEncoding();
			    if (encoding != null) {
			      for (HeaderElement element : encoding.getElements()) {
			        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
			          response.setEntity(new InflatingEntity(response.getEntity()));
			          break;
			        }
			      }
			    }
			  }
			});
		
	}
	
	private boolean RunRequest() {
		Request.Builder builder = Request.newBuilder()
				.setUsername(mUsername).setPassword(mPassword);
		
		
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
		
		HttpPost post = new HttpPost(URL_API);
		
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
		}	
		return false;
	}

	private void LoadItems(Response resp) {
		SetProgressText(R.string.sync_loading);
		DBAdapter dba = new DBAdapter(mContext);
		try {
			dba.open();
			for(Plakat plakat : resp.getPlakateList()) {
				int lon = (int)(plakat.getLon() * 1E6);
				int lat = (int)(plakat.getLat() * 1E6);
				int type = PlakatOverlayItem.TypeToTypeId(plakat.getType());
				
				dba.Insert(plakat.getId(),
						lat, lon,type,
						plakat.getLastModifiedUser(),
						plakat.getComment());
			}
		} finally {
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
        return result.build();
	}
	
	private ChangeRequest SendChangedItem(PlakatOverlayItem item) {
		ChangeRequest.Builder result = ChangeRequest.newBuilder();
		
		result.setId(item.getId());
		result.setType(item.getTypeStr());
        
		return result.build();
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
}
