package com.oneguy.recognize;

import engine.GoogleOneshotEngine;
import recognize.GoogleVoiceRecognizer;
import recognize.RecognizeListener;
import recognize.Recognizer;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageButton;

public class MainActivity extends Activity implements OnTouchListener,
		RecognizeListener {
	String TAG = "MainActivity";
	ImageButton speekButton;
	EditText edtInput;
	Recognizer mRecognizer;
	boolean isRecognizing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		isRecognizing = false;
		speekButton = (ImageButton) findViewById(R.id.speekButton);
		edtInput = (EditText) findViewById(R.id.editInput);

		speekButton.setOnTouchListener(this);
		mRecognizer = new GoogleVoiceRecognizer();
		mRecognizer.setRecognizeListener(this);
	}

	@Override
	public boolean onTouch(View view, MotionEvent arg1) {
		if (isRecognizing) {
			// ignore
			return false;
		}
		final ImageButton button = (ImageButton) view;
		int action = arg1.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			button.setImageResource(R.drawable.icon_microphone_2);
			mRecognizer.start();
			break;
		case MotionEvent.ACTION_UP:
			Util.timerInit();
			button.setImageResource(R.drawable.icon_microphone_1);
			isRecognizing = true;
			mRecognizer.stop();
			break;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mRecognizer.shutdown();
	}

	@Override
	public void onError(int errorCode) {
		Util.logTime("response");
		Log.e(TAG, "ERROR:" + errorCode);
		isRecognizing = false;
	}

	@Override
	public void onResult(final String result) {
		Util.logTime("response");
		edtInput.post(new Runnable() {

			@Override
			public void run() {
				edtInput.setText(result);
			}
		});

		isRecognizing = false;
	}

}
