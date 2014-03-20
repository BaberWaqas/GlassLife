package cw.glass.glasslife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import cw.glass.facerecognization.CameraActivity;
import cw.glass.location.LocationService;
import cw.glass.utilities.DatabaseHandler;
import cw.glass.utilities.Logfile;
import cw.glass.utilities.Utils;
import cw.glass.wifi.WifiActivity;

public class MainActivity extends Activity {

	private static int REQUEST_CODE = 1234;
	TextView resultText;
	DatabaseHandler db;
	String totaldata;
	String reverseString;
	int counter;
	ArrayList<String> lowerCaseStrings;
	private GestureDetector mGestureDetector;
	TextView cammandResult;
	TextToSpeech mTextToSpeech;
	PowerManager mPowerManager;
	boolean isDatingMode;
	WakeLock mWakeLock;
	boolean isProcessStart = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// startActivity(new Intent(this, CameraActivity.class));
		// finish();

		// cammandResult = (TextView) (findViewById(R.id.resultvoice));
		// cammandResult.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View view) {
		// isProcessStart = true;
		// startProcess();
		// }
		// });

		mTextToSpeech = new TextToSpeech(MainActivity.this,
				new OnInitListener() {

					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS) {
							mTextToSpeech.setPitch(0.5f);
							mTextToSpeech.setSpeechRate(0.9f);
						}
					}
				});

		// wake screen all the time
		try {
			getPowerManger();
		} catch (Exception e) {
			Log.e("MainActivity.onCreate",
					"getPowerManger.Exception " + e.toString());
			Utils.LogIt(MainActivity.this, "Main Activity", "onCreate",
					"getPowerManger.Exception " + e.getMessage());
		}

		// check is location service is running or not, if not than start it
		isServiceRunning();

		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

		if (activities.size() == 0) {
			Utils.LogIt(MainActivity.this, "Main Activity", "Create",
					"Recognizer not found");
		} else {
			Utils.LogIt(MainActivity.this, "Main Activity", "Create",
					"Recognizer found");
		}
		try {
			mGestureDetector = GlassGestureDetector(this);
		} catch (Exception ex) {
			Utils.LogIt(MainActivity.this, "Main Activity", "Create",
					"Glass detection not working " + ex.getMessage());
		}
	}

	private void isServiceRunning() {
		boolean isRunning = Utils.isLocationServiceRunning(MainActivity.this);
		if (!isRunning) {
			startService(new Intent(MainActivity.this, LocationService.class));
		}
	}

	protected void startProcess() {
		mTextToSpeech.speak("Speak command please", TextToSpeech.QUEUE_FLUSH,
				null);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		startVoiceRecognitionActivity();
	}

	private GestureDetector GlassGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					isProcessStart = true;
					startProcess();
				}
				return false;
			}
		});
		return gestureDetector;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			Intent intent = new Intent(MainActivity.this, CameraActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	private void startVoiceRecognitionActivity() {

		Utils.LogIt(MainActivity.this, "Main Activity",
				"startVoiceRecognitionActivity", "before Intent");

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
		intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
		intent.putExtra(
				RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
				4000);
		intent.putExtra(
				RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
				4000);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak command please");
		startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected void onPause() {
		if (mTextToSpeech != null) {
			mTextToSpeech.stop();
			mTextToSpeech.shutdown();
		}
		try {
			if (!isProcessStart)
				releaseWakeLock();
		} catch (Exception e) {
			Log.e("Main.onPause", "releaseWakeLock.Exception " + e.toString());
			Utils.LogIt(MainActivity.this, "Main Activity", "onPause",
					"releaseWakeLock.Exception " + e.getMessage());
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mTextToSpeech != null) {
			mTextToSpeech.stop();
			mTextToSpeech.shutdown();
			mTextToSpeech = null;
		}
		try {
			if (!isProcessStart)
				releaseWakeLock();
		} catch (Exception e) {
			Log.e("Main.onDestroy", "releaseWakeLock.Exception " + e.toString());
			Utils.LogIt(MainActivity.this, "Main Activity", "onDestroy",
					"releaseWakeLock.Exception " + e.getMessage());
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			if (!isProcessStart)
				getPowerManger();
		} catch (Exception e) {
			Log.e("Main.onResume", "getPowerManger.Exception " + e.toString());
			Utils.LogIt(MainActivity.this, "Main Activity", "onResume",
					"getPowerManger.Exception " + e.getMessage());
		}
	}

	private void restartActivity() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Utils.LogIt(MainActivity.this, "Main Activity",
						"restartActivity", "restartActivity request");
				isProcessStart = true;
				startProcess();
			}
		};
		Timer t = new Timer();
		t.schedule(task, 5000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		Utils.LogIt(MainActivity.this, "Main Activity", "onBackPressed",
				"back pressed");
		this.finish();
		super.onBackPressed();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		isProcessStart = false;
		try {
			Log.d("onActivityResult TRY", "inside onActivityResult TRY");

			if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "inside requestcode==REQUESTCODE");
				processData(data);
			} else {
				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "No pharases match");
				resultText.setText("No voice cammand(s)");
				restartActivity();
			}
		} catch (Exception e) {
			Utils.LogIt(MainActivity.this, "Main Activity", "onActivityResult",
					"EXCEPTION " + e.getMessage());
			e.printStackTrace();
		}
	}

	@SuppressLint("DefaultLocale")
	private void processData(Intent data) {

		ArrayList<String> matches = data
				.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		Intent intent = null;
		lowerCaseStrings = new ArrayList<String>(matches.size());

		for (String str : matches) {
			lowerCaseStrings.add(str.toLowerCase());
		}

		for (counter = 0; counter < lowerCaseStrings.size(); counter++) {

			if (lowerCaseStrings.get(counter).contains("what is your name")
					|| lowerCaseStrings.contains(" is your name")) {

				ShowMessage("You have said! " + lowerCaseStrings.get(counter));
				this.startVoiceRecognitionActivity();
				// intent = new Intent(MainActivity.this, CameraActivity.class);

				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "Hello name Recognizer found");
				break;
			}
			if (lowerCaseStrings.get(counter).contains("I am ")
					|| lowerCaseStrings.contains("my name is")) {
				String name = lowerCaseStrings.get(counter).replace("I am", "");
				name = lowerCaseStrings.get(counter).replace("my name is", "");

				ShowMessage("The name" + name + " means wisdom ");
				// intent = new Intent(MainActivity.this, CameraActivity.class);

				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "Hello name Recognizer found");
				break;
			} else if (lowerCaseStrings.get(counter).contains("cloud picture")
					|| lowerCaseStrings.get(counter).contains("picture")
					|| lowerCaseStrings.get(counter).contains("cloud")
					|| lowerCaseStrings.get(counter).contains("loud")) {

				ShowMessage("You have said! cloud picture");
				intent = new Intent(MainActivity.this, CameraActivity.class);

				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "cloud picture Recognizer found");
				break;
			} else if (lowerCaseStrings.get(counter)
					.contains("show me network")) {

				ShowMessage("You have said! " + lowerCaseStrings.get(counter));
				intent = new Intent(this, WifiActivity.class);

				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "network found");
				break;
			} else if (lowerCaseStrings.get(counter).contains("send")
					|| lowerCaseStrings.get(counter).contains("log")) {
				Utils.LogIt(MainActivity.this, "Main Activity",
						"onActivityResult", "send log found");
				DatabaseHandler db = new DatabaseHandler(MainActivity.this);
				ShowMessage(lowerCaseStrings.get(counter) + " , Sending log");
				Log.d("Reading: ", "Reading all contacts..");

				List<Logfile> contacts = db.getAllRecords();
				for (Logfile cn : contacts) {
					totaldata += "Id: " + cn.getId() + " ,tag" + cn.gettag()
							+ " ,Message " + cn.getMesage() + " ,StackTrace "
							+ cn.getStackTrace() + " ,Time " + cn.getTime()
							+ "\n";
					Log.d("Name: ", totaldata);
				}
				ABackGroundTask task = new ABackGroundTask();
				task.execute();
				break;
			} else {
				if (counter == (lowerCaseStrings.size() - 1)) {
					ShowMessage("No command found");
					break;
				}
			}
		}
		if (intent != null)
			startActivity(intent);
	}

	private void ShowMessage(String message) {
		cammandResult.setText(message);
	}

	private String postData(String totaldata) {

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(
				"http://glass-dev2.azurewebsites.net/LogforGlass.aspx");
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("Dump", totaldata));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String response = httpclient.execute(httppost, responseHandler);

			reverseString = response;
		} catch (ClientProtocolException e) {

			Utils.LogIt(MainActivity.this, "MainActivity", "postData",
					"ClientProtocolException");
		} catch (IOException e) {
			Utils.LogIt(MainActivity.this, "MainActivity", "postData",
					"IOException");
		}
		return reverseString;
	}

	private void getPowerManger() {
		if (mPowerManager == null) {
			mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		}
		if (mWakeLock == null) {
			mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
					"Glass Life Lock");
		}
		acquireWakeLock();
	}

	private void releaseWakeLock() {
		mPowerManager = null;
		if (mWakeLock != null) {
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
				mWakeLock = null;
			}
		}
	}

	private void acquireWakeLock() {
		if (mWakeLock != null) {
			if (!mWakeLock.isHeld())
				mWakeLock.acquire();
		}
	}

	class ABackGroundTask extends AsyncTask<Void, Void, String> {
		String aresult;

		public ABackGroundTask() {
			super();
		}

		@Override
		protected void onPreExecute() {
			Utils.LogIt(MainActivity.this, "VoiceRecognitionActivity",
					"onPreExecute", "called");
		}

		@Override
		protected String doInBackground(Void... params) {

			try {
				Utils.LogIt(MainActivity.this, "VoiceRecognitionActivity",
						"doInBackground", "IOException");
				// Thread.sleep(10000);
				aresult = postData(totaldata);

			} catch (Exception ex) {
				Utils.LogIt(MainActivity.this, "VoiceRecognitionActivity",
						"doInBackground", ex.getMessage());
				ShowMessage("Log sending failed");
			}
			return aresult;
		}

		@Override
		protected void onPostExecute(String response) {
			Utils.LogIt(MainActivity.this, "VoiceRecognitionActivity",
					"OnPostExecute", "called");
			Log.d("Successfully uploaded", "Uploaded log");
			ShowMessage("Log successfully uploaded");
		}

	}

}