package com.boombuler.piraten.map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class PlakatDetailsActivity extends Activity 
		implements OnClickListener {
	
	class MarkerTypeAdapter implements SpinnerAdapter {
		class ViewHolder {
			public TextView textView;
			public ImageView imageView;
		}
		
		private String[] mTitles;
		private Drawable[] mIcons;
		private LayoutInflater mInflater;
		
		public MarkerTypeAdapter() {
			Resources res = PlakatDetailsActivity.this.getResources();
			mInflater = PlakatDetailsActivity.this.getLayoutInflater();
			mTitles = res.getStringArray(R.array.markertypes);
			mIcons = new Drawable[] {
					res.getDrawable(R.drawable.plakat_default), 
	        		res.getDrawable(R.drawable.plakat_ok), 
	        		res.getDrawable(R.drawable.plakat_dieb), 
	        		res.getDrawable(R.drawable.plakat_niceplace), 
	        		res.getDrawable(R.drawable.wand),
	        		res.getDrawable(R.drawable.wand_ok),
	        		res.getDrawable(R.drawable.plakat_wrecked)};
		}
		
		public int getCount() {
			return mTitles.length;
		}

		public Object getItem(int position) {
			return mTitles[position];
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.textandicon, parent, false);
				ViewHolder vh = new ViewHolder();
				vh.textView = (TextView)convertView.findViewById(R.id.textView);
				vh.imageView = (ImageView)convertView.findViewById(R.id.imageView);
				convertView.setTag(vh);
			}
			ViewHolder holder = (ViewHolder)convertView.getTag();
			holder.textView.setText(mTitles[position]);
			holder.imageView.setImageDrawable(mIcons[position]);
			return convertView;
		}

		public int getViewTypeCount() {
			return 1;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {			
			return false;
		}

		public void registerDataSetObserver(DataSetObserver observer) {			
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {			
		}

		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			return getView(position, convertView, parent);
		}
		
	}
	
	
	public static final String EXTRA_PLAKAT_ID = "com.boombuler.piraten.map.EXTRA_PLAKAT_ID";
	public static final String EXTRA_NEW_PLAKAT = "com.boombuler.piraten.map.EXTRA_NEW_PLAKAT";
	
	private Button mSaveButton;
	private Spinner mMarkerTypeSpinner;
	private EditText mComment;
	
	private boolean mIsNew;
	private int mId;
	
	private float mMinAccuracy;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		Intent intent = getIntent();
		mId = intent.getIntExtra(EXTRA_PLAKAT_ID, -1);
		mIsNew = intent.getBooleanExtra(EXTRA_NEW_PLAKAT, false);
		if (mId < 0 && !mIsNew)
			finish();
		
		setContentView(R.layout.details);
		
		mSaveButton = (Button)findViewById(R.id.btSave);
		mSaveButton.setOnClickListener(this);
		
		mMarkerTypeSpinner = (Spinner)findViewById(R.id.spMarkerType);
		mMarkerTypeSpinner.setAdapter(new MarkerTypeAdapter());
		
		mComment = (EditText)findViewById(R.id.tvComment);
		
		if (!mIsNew) {
			PlakatOverlayItem item = null;
			DBAdapter adapter = new DBAdapter(this);
			try {
				adapter.open();
				item = adapter.getOverlayItem(mId);
			} finally {
				adapter.close();
			}
			
			if (item != null) {
				mMarkerTypeSpinner.setSelection(item.getType());
				mComment.setText(item.getComment());
			}
		}else {
			mMinAccuracy = ((float)PreferenceManager.getDefaultSharedPreferences(this).getInt(SettingsActivity.KEY_ACCURACY, 70)) / 10f;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mIsNew) {
			MenuItem mi = menu.add(R.string.menu_delete).setIcon(android.R.drawable.ic_menu_delete);
			mi.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					DBAdapter adapter = new DBAdapter(PlakatDetailsActivity.this);
					try {
						adapter.open();
						adapter.delete(mId);
					} finally {
						adapter.close();
					}
					
					PlakatDetailsActivity.this.setResult(RESULT_OK);
					PlakatDetailsActivity.this.finish();
					return true;
				}
			});
			return true;
		}
		return false;
	}

	public void onClick(View v) {
		if (v == mSaveButton) {
			mSaveButton.setEnabled(false);
			if (mIsNew)
				Insert();
			else
				Update();
		}
	}

	private void Update() {
		DBAdapter adapter = new DBAdapter(this);
		try {			
			adapter.open();
			adapter.Update(mId, mMarkerTypeSpinner.getSelectedItemPosition());
		} finally {
			adapter.close();
		}
		setResult(RESULT_OK);
		finish();
	}

	private ProgressDialog mProgressDlg = null;
	
	private void Insert() {
		final LocationListener ll = new LocationListener() {
			
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			public void onProviderEnabled(String provider) {
			}
			
			public void onProviderDisabled(String provider) {
			}
			
			public void onLocationChanged(Location location) {
				CompleteInsert(location);
			}
		}; 
		final LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
		
		mProgressDlg = new ProgressDialog(this);
		mProgressDlg.setOwnerActivity(this);
		mProgressDlg.setCancelable(true);
		mProgressDlg.setCanceledOnTouchOutside(false);
		mProgressDlg.setIndeterminate(true);
		mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDlg.setMessage(getString(R.string.get_position));
		mProgressDlg.setButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mProgressDlg.cancel();
			}
		});
		mProgressDlg.setOnCancelListener(new OnCancelListener() {
			
			public void onCancel(DialogInterface dialog) {
				lm.removeUpdates(ll);
				mSaveButton.setEnabled(true);				
			}
		});
		mProgressDlg.setOnDismissListener(new OnDismissListener() {			
			public void onDismiss(DialogInterface dialog) {
				lm.removeUpdates(ll);
			}
		});
		mProgressDlg.show();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);
	}
	
	private void CompleteInsert(Location loc) {
		if (loc.getAccuracy() <= mMinAccuracy) {
			mProgressDlg.dismiss();
			DBAdapter adapter = new DBAdapter(this);
			try {
				adapter.open();
				adapter.InsertNew( 
						(int)(loc.getLatitude() * 1E6), 
						(int)(loc.getLongitude() * 1E6), 
						mMarkerTypeSpinner.getSelectedItemPosition());
			} finally {
				adapter.close();
			}
			
			setResult(RESULT_OK);
			finish();
		} else {
			String msg = getString(R.string.get_position) + "\n";
			msg += getString(R.string.current_accuracy, loc.getAccuracy());
			
			mProgressDlg.setMessage(msg);
		}

	}
}
