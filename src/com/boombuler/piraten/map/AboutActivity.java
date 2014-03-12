package com.boombuler.piraten.map;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

public class AboutActivity extends Activity {
	WebView mWebView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        mWebView = (WebView)findViewById(R.id.webview);
        loadWebView();
    }
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about, menu);
        return true;
    }
	
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {	
    	case android.R.id.home:
    	case R.id.menu_ok:
	    	finish();
	    	return true;
	    }
	    return super.onOptionsItemSelected(item);
    }

	private void loadWebView() {
		mWebView.loadUrl("file:///android_asset/about/index.html");
	}
}
