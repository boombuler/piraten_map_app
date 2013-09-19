package com.boombuler.piraten.map;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class AccuracyPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	private SeekBar mSeekBar;
	private TextView mValueText;
	private Context mContext;

	private String mDialogMessage, mSuffix;
	private int mDefault, mMax, mValue = 0;

	public AccuracyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		mDialogMessage = context.getString(R.string.settings_accuracy);
		mSuffix = context.getString(R.string.settings_accuracy_meters);
		mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
		mMax = attrs.getAttributeIntValue(androidns, "max", 100);
	}

	@Override
	protected View onCreateDialogView() {
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		//mValueText.setTextSize(32);
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist())
			mValue = getPersistedInt(mDefault);

		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
		onProgressChanged(mSeekBar, mValue, false);
		return layout;
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		mSeekBar.setMax(mMax);
		mSeekBar.setProgress(mValue);
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		if (restore)
			mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
		else
			mValue = (Integer) defaultValue;
	}

	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
	 	double val = ((double)value) / 10;
		mValueText.setText(mDialogMessage + String.format(mSuffix, val));
		if (shouldPersist())
			persistInt(value);
		callChangeListener(Integer.valueOf(value));
	}

	public void onStartTrackingTouch(SeekBar seek) {
	}

	public void onStopTrackingTouch(SeekBar seek) {
	}

	public void setMax(int max) {
		mMax = max;
	}

	public int getMax() {
		return mMax;
	}

	public void setProgress(int progress) {
		mValue = progress;
		if (mSeekBar != null)
			mSeekBar.setProgress(progress);
	}

	public int getProgress() {
		return mValue;
	}
}
