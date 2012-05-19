package com.boombuler.piraten.utils;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.protocol.HttpContext;

import com.boombuler.piraten.map.R;

import android.content.Context;
import android.util.Log;

public class MyHttpClient extends DefaultHttpClient {
	private static final String TAG = "map.boombuler.HttpClient";
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	
	private final Context mContext;
	
	public MyHttpClient(Context context) {
		super();
		mContext = context;
		PreventRedirection();
		ActivateGZipSupport();
	}
	
	private void PreventRedirection() {
		setRedirectHandler(new RedirectHandler() {
			public boolean isRedirectRequested(HttpResponse response,
					HttpContext context) {
				return false;
			}
			
			public URI getLocationURI(HttpResponse response, HttpContext context)
					throws ProtocolException {
				return null;
			}
		});		
	}
	
	private void ActivateGZipSupport() {
		addRequestInterceptor(new HttpRequestInterceptor() {
			  public void process(HttpRequest request, HttpContext context) {
			    // Add header to accept gzip content
			    if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
			      request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
			    }
			  }
		});

		addResponseInterceptor(new HttpResponseInterceptor() {
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


	 @Override
	 protected ClientConnectionManager createClientConnectionManager() {
		 Log.d(TAG, "creating connection manager");
		 SchemeRegistry registry = new SchemeRegistry();
	     registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	     Log.d(TAG, "http scheme registered");
	     try {
	    	SSLSocketFactory factory = new AdditionalKeyStoresSSLSocketFactory(getKeyStore());	    	 
			registry.register(new Scheme("https", factory, 443));
		} catch (Exception e) {
			e.printStackTrace();
		}
	    return new SingleClientConnManager(getParams(), registry);
	 }
	 
	private KeyStore getKeyStore() {
        try {
			KeyStore trusted = KeyStore.getInstance("BKS");
	        InputStream in = mContext.getResources().openRawResource(R.raw.piraten_server);
            try {
               trusted.load(in, "piraten".toCharArray());
            } finally {
               in.close();
            }
            
            return trusted;
	    } catch (Exception e) {
	        throw new AssertionError(e);
	    }
	 }

}
