package com.oneguy.recognize.recognize;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.oneguy.recognize.R;
import com.oneguy.recognize.recognize.AutoDetector.SpeechStatus;

public class SpeechActionPrompt implements SpeechActionListener {

	private static final String TAG = "SpeechActionPrompt";

	private Activity mActivty;
	private View mView;
	private TextView mTitle;
	private ImageView mPromptImg;
	private static final double MIN_RMS = 0.02f;
	private static final double MAX_RMS = 0.3f;
	private Bitmap[] bitmapCache;
	private static final int BITMAP_CACHE_SIZE = 20;

	public SpeechActionPrompt(Activity act) {
		mActivty = act;
		mView = View.inflate(mActivty, R.layout.prompt, null);
		mView.setBackgroundColor(Color.TRANSPARENT);
		mTitle = (TextView) mView.findViewById(R.id.title);
		mPromptImg = (ImageView) mView.findViewById(R.id.promptImg);
		bitmapCache = new Bitmap[BITMAP_CACHE_SIZE + 1];
		prepareCache();
		addPromptView();
	}

	private void prepareCache() {
		Bitmap full = BitmapFactory.decodeResource(mActivty.getResources(),
				R.drawable.vs_popup_mic_full);
		Bitmap base = BitmapFactory.decodeResource(mActivty.getResources(),
				R.drawable.vs_popup_mic_base);
		for (int i = 0; i < BITMAP_CACHE_SIZE; i++) {
			Bitmap bitmap = genMicBackground((double) i
					/ (double) BITMAP_CACHE_SIZE, full, base);
			bitmapCache[i] = bitmap;
		}
	}

	private void addPromptView() {
		LayoutParams lp = new LayoutParams();
		lp.width = LayoutParams.FILL_PARENT;
		lp.height = LayoutParams.FILL_PARENT;
		lp.gravity = Gravity.CENTER;
		mView.setVisibility(View.GONE);
		mActivty.addContentView(mView, lp);
	}

	@Override
	public void onInit() {
		mView.setVisibility(View.VISIBLE);
		setTitle(R.string.title_init);
		setMicWithRms(0);
	}

	@Override
	public void onSilence() {
		setTitle(R.string.title_recording);
	}

	@Override
	public void onStartSpeech(SpeechStatus status) {
		setTitle(R.string.title_recording);
	}

	@Override
	public void onSpeech(SpeechStatus status) {
		setTitle(R.string.title_recording);
		setMicWithRms(status.maxRms);
	}

	private void setMicWithRms(double rms) {
		double percent = calVolPercent(rms);
		Bitmap bitmap = getMicBackgroundFromCache(percent);
		setPromptImage(bitmap);
	}

	@Override
	public void onWaitSpeechResult() {
		setTitle(R.string.title_processing);
	}

	@Override
	public void onEnd() {
		mView.post(new Runnable() {
			@Override
			public void run() {
				mView.setVisibility(View.GONE);
			}
		});
		Log.d(TAG, "onEnd");
	}

	public void setTitle(final int resId) {
		mView.post(new Runnable() {
			@Override
			public void run() {
				mTitle.setText(resId);
			}
		});
	}

	public void setPromptImage(final Bitmap bitmap) {
		mView.post(new Runnable() {
			@Override
			public void run() {
				mPromptImg.setImageBitmap(bitmap);
			}
		});
	}

	private double calVolPercent(double rms) {
		if (rms < MIN_RMS) {
			rms = MIN_RMS;
		}
		if (rms > MAX_RMS) {
			rms = MAX_RMS;
		}
		return (rms - MIN_RMS) / (MAX_RMS - MIN_RMS);
	}

	private Bitmap getMicBackgroundFromCache(double percent) {
		int index = (int) Math.floor(percent * BITMAP_CACHE_SIZE);
		if (index < 0) {
			index = 0;
		}
		if (index > BITMAP_CACHE_SIZE) {
			index = BITMAP_CACHE_SIZE;
		}
		return bitmapCache[index];
	}

	private Bitmap genMicBackground(double percent, Bitmap full, Bitmap base) {
		int width = full.getWidth();
		int height = full.getHeight();
		int imgHeight = (int) (height * percent);
		Bitmap target = Bitmap.createBitmap(base.getWidth(), base.getHeight(),
				Config.ARGB_8888);
		Canvas c = new Canvas(target);
		c.drawBitmap(base, 0, 0, null);
		Rect src = new Rect(0, height - imgHeight, width, height);
		c.drawBitmap(full, src, src, null);
		return target;
	}

}
