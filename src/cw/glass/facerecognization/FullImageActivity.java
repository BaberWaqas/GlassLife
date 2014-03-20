package cw.glass.facerecognization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import cw.glass.glasslife.MainActivity;
import cw.glass.glasslife.R;
import cw.glass.location.LocationService;
import cw.glass.location.LocationService.LocationServiceBinder;
import cw.glass.utilities.FacePoints;
import cw.glass.utilities.IFacebookLocation;
import cw.glass.utilities.ImageProcessor;
import cw.glass.utilities.ImageUtility;
import cw.glass.utilities.LocationTask;
import cw.glass.utilities.RestClient;
import cw.glass.utilities.SystemUtilities;
import cw.glass.utilities.Utils;

public class FullImageActivity extends Activity implements IFacebookLocation {

	ImageView imageView;
	Bitmap bitmap = null;
	TextView wordcount;
	int newX, newY;
	int value = 10;
	private static final int REQUEST_CODE = 1234;
	private String requestVoice = "photo";
	int hatX = 0, hatY = 0;
	String identifier, imageLocation;
	private String fileLocation = "";
	private GestureDetector mGlassDetector;
	String width, height, topLeftX, topLeftY, lipLineX, lipLineY, nostrilLineY;
	String rightEyeBrowLeftX, rightEyeBrowLeftY, rightEyeBrowRightX,
			rightEyeBrowRightY;
	Bitmap src;
	ArrayList<String> lowerCaseStrings;
	TextView textView;
	TextView cmdResult;
	String pridiction;
	TextToSpeech mTextToSpeech;
	boolean isAllowed = false, captionFlag = false;
	// @param for location service connection
	boolean isBound = false;
	private LocationService mLocationService = null;
	private String mLocationId = null;
	PowerManager mPowerManager;
	WakeLock mWakeLock;
	LinearLayout mProgresslayout;
	boolean isProcessStart = false;
	String effectName = "", photoCaption = "glass life";
	ArrayList<FacePoints> facePoints;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// check is location service is running or not, if not than start it
		isServiceRunning();
		doBindService();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.endimage);

		textView = (TextView) findViewById(R.id.resultText);
		cmdResult = (TextView) findViewById(R.id.cmd_result);
		mProgresslayout = (LinearLayout) findViewById(R.id.progress_container);
		imageView = (ImageView) findViewById(R.id.image_view);
		imageView.setOnClickListener(imgListner);

		// receive intents from camera
		imageLocation = getIntent().getStringExtra("imageLocation");
		facePoints = getIntent().getParcelableArrayListExtra("faces_results");

		getPowerManger();
		imageViewSetting();
		textToSpeech();
		startProcess();
		// getMadridEffect();

		try {
			mGlassDetector = createGestureDetector(this);
		} catch (Exception ex) {
			Log.e("onCreate",
					"createGestureDetectorinside .Exception " + ex.getMessage());
			Utils.LogIt(FullImageActivity.this, "Full Image Activity",
					"onCreate()",
					"createGestureDetector.Exception " + ex.getMessage());
		}
	}

	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TWO_TAP && isAllowed == true) {
					faceBookPosting();
					return true;
				}
				if (gesture == Gesture.TAP && captionFlag) {
					cmdResult
							.setText("Two tap to confirm and post the image to facebook");
				}
				if (gesture == Gesture.SWIPE_DOWN) {
					startActivity(new Intent(FullImageActivity.this,
							MainActivity.class));
					FullImageActivity.this.finish();
					return true;
				}
				return false;
			}
		});
		return gestureDetector;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGlassDetector != null) {
			return mGlassDetector.onMotionEvent(event);
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		Utils.LogIt(FullImageActivity.this, "Full Image Activity",
				"onBackPressed()", "onBackPressed()");
		finish();
		super.onBackPressed();
	}

	private void startVoiceRecognitionActivity() {
		Utils.LogIt(FullImageActivity.this, "Full Image Activity",
				"startVoiceRecognitionActivity()", "clled before intent");
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
		Log.d("startVoiceRecognitionActivity", "voice request " + requestVoice);
		if (requestVoice.equals("photo")) {
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
					"Voice Recognising...");
			startActivityForResult(intent, REQUEST_CODE);
		} else {
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
					"Speak caption please");
			startActivityForResult(intent, 4321);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Utils.LogIt(FullImageActivity.this, "Full Image Activity",
				"startVoiceRecognitionActivity()", "clled before intent");
		try {
			if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
				Log.d("onActivityResult", "mame request");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"onActivityResult", "mame request");
				processImage(data);

			} else if (requestCode == 4321 && resultCode == RESULT_OK) {
				Log.d("onActivityResult", "caption request");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"onActivityResult", "caption request");
				processVoiceForCaption(data);
			} else {
				Log.d("onActivityResult", "inside onActivityResult ELSE");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"onActivityResult", "no data found");
				textToSpeech();
				startProcess();
			}
		} catch (Exception e) {
			Log.d("onActivityResult",
					"Exception onActivityResult " + e.toString());
			Utils.LogIt(FullImageActivity.this, "Full Image Activity",
					"onActivityResult", "EXCEPTION " + e.toString());
		}
	}

	@Override
	protected void onPause() {
		if (mTextToSpeech != null) {
			mTextToSpeech.stop();
			mTextToSpeech.shutdown();
		}
		try {
			releaseWakeLock();
		} catch (Exception e) {
			Log.e("FullImageActivity.onPause",
					"releaseWakeLock.Exception " + e.toString());
			Utils.LogIt(FullImageActivity.this, "FullImageActivity", "onPause",
					"releaseWakeLock.Exception " + e.getMessage());
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			getPowerManger();
		} catch (Exception e) {
			Log.e("FullImageActivity.onResume",
					"getPowerManger.Exception " + e.toString());
			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"onResume", "getPowerManger.Exception " + e.getMessage());
		}
	}

	@Override
	protected void onDestroy() {
		if (mTextToSpeech != null) {
			mTextToSpeech.stop();
			mTextToSpeech.shutdown();
		}
		doUnBindService();
		try {
			releaseWakeLock();
		} catch (Exception e) {
			Log.e("FullImageActivity.onDestroy", "releaseWakeLock.Exception "
					+ e.toString());
			Utils.LogIt(FullImageActivity.this, "FullImageActivity",
					"onDestroy", "releaseWakeLock.Exception " + e.getMessage());
		}
		super.onDestroy();
	}

	@Override
	protected void onUserLeaveHint() {
		try {
			releaseWakeLock();
		} catch (Exception ex) {
			Log.e("Exception in onUserLeaveHint", ex.getMessage());
		}
		super.onUserLeaveHint();
	}

	private void processImage(Intent data) {
		boolean isEffectApplied = true;
		Log.d("onActivityResult TRY", "inside processImage()");
		ArrayList<String> matches = data
				.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		lowerCaseStrings = new ArrayList<String>(matches.size());
		for (String str : matches) {

			lowerCaseStrings.add(str.toLowerCase());
			isEffectApplied = true;

			if (str.toLowerCase().toString().contains("mustaches")
					|| str.toLowerCase().toString().contains("must")) {
				Log.d("processImage", "mustaches found");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"processImage", "mustaches found");
				getmustachesEffect();
				effectName = "Mustaches";
				break;
			} else if (str.toLowerCase().toString().contains("sombrero")
					|| str.toLowerCase().toString().contains("som")
					|| str.toLowerCase().toString().contains("som")) {
				Log.d("processImage", "hat found");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"processImage", "hat found");

				gethatEffect();
				effectName = "Sombrero";
				break;
			} else if (str.toLowerCase().toString().contains("monocle")
					|| str.toLowerCase().toString().contains("monacle")
					|| str.toLowerCase().toString().contains("mono")
					|| str.toLowerCase().toString().contains("mona")
					|| str.toLowerCase().toString().contains("michael")) {

				Log.d("processImage", "monocle found");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"processImage", "monocle found");

				getMonocleEfect();
				effectName = "Monocle";
				break;
			} else if (str.toLowerCase().toString().contains("madrid")
					|| str.toLowerCase().toString().contains("mad")
					|| str.toLowerCase().toString().contains("rid")) {

				Log.d("processImage", "found madrid");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"processImage", "found madrid");

				getMadridEffect();
				effectName = "Madrid face paint";
				break;
			} else if (str.toLowerCase().toString().contains("every thing")
					|| str.toLowerCase().toString().contains("every")) {

				Log.d("processImage", "both found");
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"processImage", "both found");

				getbothEffect(bitmap, width, height, topLeftX, topLeftY,
						lipLineX, lipLineY, nostrilLineY);
				effectName = "All effects";
				break;
			} else {
				isEffectApplied = false;
			}
			Log.d("value", str.toLowerCase().toString());
		}
		if (isEffectApplied) {
			requestVoice = "caption";
			textToSpeech();
			startProcess();
			requestVoice = "photo";
		} else {
			Log.d("processImage", "no effect");
			Utils.LogIt(FullImageActivity.this, "Full Image Activity",
					"processImage", "no effect applied");
			effectName = "none";
			requestVoice = "photo";
		}
	}

	private void processVoiceForCaption(Intent data) {
		ArrayList<String> caption = data
				.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		if (caption.size() > 0) {
			photoCaption = caption.get(0);
			cmdResult.setText("Tap to confirm caption:\n" + photoCaption);
			captionFlag = true;
			Log.d("processVoiceForCaption",
					"processVoiceForCaption.photoCaption " + photoCaption);
			Utils.LogIt(FullImageActivity.this, "Full Image Activity",
					"processVoiceForCaption",
					"processVoiceForCaption.photoCaption " + photoCaption);
		}
	}

	protected void startProcess() {
		if (requestVoice.equals("caption")) {
			mTextToSpeech.speak("Speak caption please",
					TextToSpeech.QUEUE_FLUSH, null);
		} else {
			mTextToSpeech.speak("Apply which effect", TextToSpeech.QUEUE_FLUSH,
					null);
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		startVoiceRecognitionActivity();
	}

	private void getMonocleEfect() {

		Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas c = new Canvas(result);
		Paint p = new Paint();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float scaleWidth = metrics.heightPixels;
		float scaleHeight = metrics.widthPixels;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		c.drawBitmap(bitmap, 0, 0, p);

		for (FacePoints item : facePoints) {

			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"getMonocleEfect", "inside ");

			Log.d("getMonocleEfect", "applying monocle on face ");

			// rightEyeLX = item.getRightEyeBrowLeftX();
			// rightEyeLY = item.getRightEyeBrowLeftY();
			// rightEyeRX = item.getRightEyeBrowRightX();
			// rightEyeRY = item.getRightEyeBrowRightY();

			Log.d("getMonocleEfect", item.toString());

			int eyeLY = (int) Float.parseFloat(item.getRightEyeBrowLeftY());
			int eyeLX = (int) Float.parseFloat(item.getRightEyeBrowLeftX());
			int eyeRX = (int) Float.parseFloat(item.getRightEyeBrowRightX());
			// int eyeRY = (int) Float.parseFloat(rightEyeRY);

			src = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.monocle_man)).replaceColor(
					Color.WHITE, Color.TRANSPARENT);

			if (src.getWidth() < 98) {
				src = Bitmap.createScaledBitmap(src, src.getWidth() * 2,
						src.getHeight() * 2, false);
			}
			// 98 is width of monocle that will cover eye
			int eyeWidth = eyeRX - eyeLX;
			// ration = eye_width / moncle_width
			float difRatio = (float) (eyeWidth / 98.0);
			// scalling down the monocle w.r.t eye
			src = Bitmap.createScaledBitmap(src,
					(int) ((int) src.getWidth() * difRatio),
					(int) ((int) src.getHeight() * difRatio), false);

			c.drawBitmap(src, eyeLX, eyeLY, p);
		}
		try {
			String path = SaveEditedPicture(result);
			fileLocation = path;
		} catch (Exception e) {
			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"getMonocleEfect", "exception " + e.toString());
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					FullImageActivity.this);
			dialog.setMessage(e.getMessage());
			dialog.setNeutralButton("Cool", null);
			dialog.create().show();
		}
	}

	private void getmustachesEffect() {

		Log.d("getmustachesEffect", "inside getmustachesEffect");
		Utils.LogIt(FullImageActivity.this, "Full Image Activity",
				"getmustachesEffect()", "inside getmustachesEffect");

		Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);

		Canvas c = new Canvas(result);
		Paint p = new Paint();
		p.setAntiAlias(false);
		p.setDither(false);
		p.setColor(Color.GREEN);
		p.setStyle(Style.STROKE);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float scaleWidth = metrics.heightPixels;
		float scaleHeight = metrics.widthPixels;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		c.drawBitmap(bitmap, 0, 0, p);

		for (FacePoints item : facePoints) {
			Log.d("getmustachesEffect", "applying mustaches on face ");

			width = item.getWidth();
			height = item.getHeight();
			topLeftX = item.getTopLeftX();
			topLeftY = item.getTopLeftY();
			lipLineX = item.getLipLineX();
			lipLineY = item.getLipLineY();
			nostrilLineY = item.getRightNostrilY();

			Log.d("hatEffect", item.toString());

			Bitmap mustBit = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.must350)).replaceColor(
					Color.WHITE, Color.TRANSPARENT);

			src = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.hat4)).replaceColor(Color.WHITE,
					Color.TRANSPARENT);

			if (src.getWidth() < 500) {
				src = Bitmap.createScaledBitmap(src, src.getWidth() * 2,
						src.getHeight() * 2, false);
				mustBit = Bitmap.createScaledBitmap(mustBit,
						mustBit.getWidth() * 2, mustBit.getHeight() * 2, false);
			}

			float difRatio = (float) (Float.parseFloat(width) / 183.0);
			float wRation = Integer.parseInt(width) / 2;
			float difRatioM = (float) (wRation / 170.0);

			mustBit = Bitmap.createScaledBitmap(mustBit,
					(int) ((int) mustBit.getWidth() * difRatioM),
					(int) ((int) mustBit.getHeight() * difRatioM), false);

			src = Bitmap.createScaledBitmap(src,
					(int) ((int) src.getWidth() * difRatio),
					(int) ((int) src.getHeight() * difRatio), false);

			int hatline = (int) (165 * difRatio);
			if (hatline > Integer.parseInt(topLeftX)) {
				newX = hatline - Integer.parseInt(topLeftX);
			} else {
				newX = 0;
				hatX = Integer.parseInt(topLeftX) - hatline;
			}

			if (src.getHeight() > Integer.parseInt(topLeftY)) {
				newY = src.getHeight() - Integer.parseInt(topLeftY);
			} else {
				newY = 0;
				hatY = Integer.parseInt(topLeftY) - src.getHeight();
			}

			int mustX = (int) (Float.parseFloat(lipLineX) - (170.00 * difRatioM));
			int mustY = (int) (Float.parseFloat(nostrilLineY) + 5.00);

			c.drawBitmap(mustBit, mustX, mustY, p);
		}
		try {
			String pathED = this.SaveEditedPicture(result);
			fileLocation = pathED;
		} catch (Exception ex) {
			Log.d("getmustachesEffect", "inside getmustachesEffect");
			Utils.LogIt(FullImageActivity.this, "Full Image Activity",
					"getmustachesEffect()", "Exception " + ex.getMessage()
							+ " and byte count is" + result.getByteCount());

			AlertDialog.Builder dialog = new AlertDialog.Builder(
					cw.glass.facerecognization.FullImageActivity.this);
			dialog.setMessage(ex.getMessage());
			dialog.setNeutralButton("Cance", null);
			dialog.create().show();
		}
	}

	public void getbothEffect(Bitmap cameraimage, String width, String height,
			String topLeftX, String topLeftY, String lipLineX, String lipLineY,
			String nostrilLineY) {
		String pathED = "";
		try {

			Utils.LogIt(cw.glass.facerecognization.FullImageActivity.this,
					"FullImageActivity", "getbothEffect", "Starting");
			Bitmap mustBit = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.must350)).replaceColor(
					Color.WHITE, Color.TRANSPARENT);

			src = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.hat4)).replaceColor(Color.WHITE,
					Color.TRANSPARENT);
			if (src.getWidth() < 500) {
				src = Bitmap.createScaledBitmap(src, src.getWidth() * 2,
						src.getHeight() * 2, false);
				mustBit = Bitmap.createScaledBitmap(mustBit,
						mustBit.getWidth() * 2, mustBit.getHeight() * 2, false);
			}
			// 347-164 = 183
			// 183 is width of hat area that should cover head
			// calculating face and hat width difference ratio
			float difRatio = (float) (Float.parseFloat(width) / 183.0);
			// getting half face width
			float wRation = Integer.parseInt(width) / 2;
			// calculating face and mustache width difference ration
			float difRatioM = (float) (wRation / 170.0);
			// scale downing of mustache and hat png's w.r.t. face width
			mustBit = Bitmap.createScaledBitmap(mustBit,
					(int) ((int) mustBit.getWidth() * difRatioM),
					(int) ((int) mustBit.getHeight() * difRatioM), false);
			src = Bitmap.createScaledBitmap(src,
					(int) ((int) src.getWidth() * difRatio),
					(int) ((int) src.getHeight() * difRatio), false);
			// topLeftX > 165)
			// 165 is the starting hat area in x axis
			int hatline = (int) (165 * difRatio);

			// x and y location
			if (hatline > Integer.parseInt(topLeftX)) {
				newX = hatline - Integer.parseInt(topLeftX);
			} else {
				newX = 0;
				hatX = Integer.parseInt(topLeftX) - hatline;
			}

			if (src.getHeight() > Integer.parseInt(topLeftY)) {
				newY = src.getHeight() - Integer.parseInt(topLeftY);
			} else {
				newY = 0;
				hatY = Integer.parseInt(topLeftY) - src.getHeight();
			}

			Bitmap croppedBmp = Bitmap.createBitmap(src, newX, newY,
					src.getWidth() - newX, src.getHeight() - newY);
			Log.d("Width+Height", String.valueOf(bitmap.getWidth()) + "+"
					+ String.valueOf(bitmap.getHeight()));
			Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
					bitmap.getHeight(), Config.ARGB_8888);

			int mustX = (int) (Float.parseFloat(lipLineX) - (170.00 * difRatioM));
			int mustY = (int) (Float.parseFloat(nostrilLineY) + 5.00);
			Canvas c = new Canvas(result);
			Paint p = new Paint();
			p.setAntiAlias(false);
			p.setDither(false);
			p.setColor(Color.GREEN);
			p.setStyle(Style.STROKE);
			c.drawBitmap(bitmap, 0, 0, p);
			c.drawBitmap(croppedBmp, hatX, hatY, p);
			c.drawBitmap(mustBit, mustX, mustY, p);
			pathED = this.SaveEditedPicture(result);
			fileLocation = pathED;
		} catch (Exception ex) {
			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"getbothsEffect", "exception " + ex.getMessage());

			AlertDialog.Builder dialog = new AlertDialog.Builder(
					FullImageActivity.this);
			dialog.setTitle(pathED);
			dialog.setMessage(ex.getMessage());
			dialog.setNeutralButton("Cool", null);
			dialog.create().show();
		}
		Utils.LogIt(cw.glass.facerecognization.FullImageActivity.this,
				"FullImage Activity", "getbothsEffect",
				"finishing and pathed is" + pathED);
	}

	private void gethatEffect() {

		Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);

		Canvas c = new Canvas(result);
		Paint p = new Paint();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float scaleWidth = metrics.heightPixels;
		float scaleHeight = metrics.widthPixels;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		c.drawColor(Color.BLACK);
		c.drawBitmap(bitmap, 0, 0, p);

		Log.d("hatEffect", "facePoints.size() " + facePoints);

		for (FacePoints item : facePoints) {

			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"gethatEffect", "inside ");

			Log.d("hatEffect", "applying hat on face ");

			width = item.getWidth();
			height = item.getHeight();
			topLeftX = item.getTopLeftX();
			topLeftY = item.getTopLeftY();
			lipLineX = item.getLipLineX();
			lipLineY = item.getLipLineY();
			nostrilLineY = item.getRightNostrilY();

			Log.d("hatEffect", item.toString());

			src = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.hat4)).replaceColor(Color.WHITE,
					Color.TRANSPARENT);

			if (src.getWidth() < 500) {
				src = Bitmap.createScaledBitmap(src, src.getWidth() * 2,
						src.getHeight() * 2, false);
			}
			float difRatio = (float) (Float.parseFloat(width) / 183.0);
			src = Bitmap.createScaledBitmap(src,
					(int) ((int) src.getWidth() * difRatio),
					(int) ((int) src.getHeight() * difRatio), false);
			int hatline = (int) (165 * difRatio);

			if (hatline > Integer.parseInt(topLeftX)) {
				newX = hatline - Integer.parseInt(topLeftX);
			} else {
				newX = 0;
				hatX = Integer.parseInt(topLeftX) - hatline;
			}

			if (src.getHeight() > Integer.parseInt(topLeftY)) {
				newY = src.getHeight() - Integer.parseInt(topLeftY);
			} else {
				newY = 0;
				hatY = Integer.parseInt(topLeftY) - src.getHeight();
			}

			Bitmap croppedBmp = Bitmap.createBitmap(src, newX, newY,
					src.getWidth() - newX, src.getHeight() - newY);
			Log.d("Width+Height", String.valueOf(bitmap.getWidth()) + "+"
					+ String.valueOf(bitmap.getHeight()));

			c.drawBitmap(croppedBmp, hatX, hatY, p);
		}
		try {
			String pathED = SaveEditedPicture(result);
			fileLocation = pathED;
		} catch (Exception ex) {
			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"gethatEffect", "exception " + ex.getMessage());
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					cw.glass.facerecognization.FullImageActivity.this);
			dialog.setMessage(ex.getMessage());
			dialog.setNeutralButton("Cancel", null);
			dialog.create().show();
		}
	}

	private void getMadridEffect() {

		Utils.LogIt(FullImageActivity.this, "FullImage Activity",
				"getMadridEffect", "inside ");
		Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);

		Canvas c = new Canvas(result);
		Paint p = new Paint();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		float scaleWidth = metrics.heightPixels;
		float scaleHeight = metrics.widthPixels;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		c.drawColor(Color.BLACK);
		c.drawBitmap(bitmap, 0, 0, p);

		for (FacePoints item : facePoints) {

			src = new ImageProcessor(BitmapFactory.decodeResource(
					getResources(), R.drawable.madrid)).replaceColor(
					Color.WHITE, Color.TRANSPARENT);

			if (src.getWidth() < 500) {
				src = Bitmap.createScaledBitmap(src, src.getWidth() * 2,
						src.getHeight() * 2, false);
			}

			int w = (int) Float.parseFloat(item.getWidth());
			int h = (int) Float.parseFloat(item.getHeight());
			int x = (int) Float.parseFloat(item.getTopLeftX());
			int y = (int) Float.parseFloat(item.getTopLeftY());

			Log.d("orignal width/height", "Width " + w + ", Height" + h);

			w = w + (int) Math.round(w * (double) 35 / 100);
			h = h + (int) Math.round(h * (double) 45 / 100);
			x = x - (int) Math.round(x * (double) 12 / 100);
			y = y - (int) Math.round(y * (double) 45 / 100);

			src = Bitmap.createScaledBitmap(src, w, h, false);

			Log.d("updated madrid", "Height " + src.getHeight() + ",Width "
					+ src.getWidth());

			c.drawBitmap(src, x, y, p);
		}

		try {
			String pathED = SaveEditedPicture(result);
			fileLocation = pathED;
		} catch (Exception ex) {
			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"gethatEffect", "exception " + ex.getMessage());
			AlertDialog.Builder dialog = new AlertDialog.Builder(
					cw.glass.facerecognization.FullImageActivity.this);
			dialog.setMessage(ex.getMessage());
			dialog.setNeutralButton("Cool", null);
			dialog.create().show();
		}
	}

	public void faceBookPosting() {
		mProgresslayout.setVisibility(View.VISIBLE);
		Thread thread = new Thread(new Runnable() {
			public void run() {
				PostDataToFaceBook();
				FullImageActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						mProgresslayout.setVisibility(View.GONE);
						cmdResult.setText(effectName
								+ " applied and posted to facebook");
					}
				});
			}
		});
		thread.start();
	}

	public void PostDataToFaceBook() {
		try {
			String location = mLocationId != null ? mLocationId
					: "185804821479728";

			Utils.LogIt(cw.glass.facerecognization.FullImageActivity.this,
					"FullImage Activity", "PostDataToFaceBook",
					"Uploading image to facebok and image location is"
							+ fileLocation);

			RestClient restClient = new RestClient();
			String response = restClient.uploadPhoto(SystemUtilities.FbUrl,
					fileLocation, pridiction != null ? (pridiction
							+ " with effect applied " + effectName + " ")
							: "No prediction " + " "
									+ (captionFlag ? photoCaption : ""),
					location);

			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"PostDataToFaceBook", response);

		} catch (Exception e) {
			Utils.LogIt(cw.glass.facerecognization.FullImageActivity.this,
					"FullImage Activity", "PostDataToFaceBook",
					"Exception is s" + e.getMessage());
		}
	}

	public String SaveEditedPicture(Bitmap result) throws Exception {
		String title = Utils.fileNameCreator("IMGEdited");

		String DCIM = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DCIM).toString();
		String DIRECTORY = DCIM + "/Camera";
		String path = DIRECTORY + '/' + title;

		FileOutputStream out = null;

		out = new FileOutputStream(path);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		result = ImageUtility
				.ScaleDownBitmap(result, result.getWidth() * 0.58f);
		result.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		out.write(outputStream.toByteArray());
		out.close();

		ContentValues values = new ContentValues(5);
		values.put(ImageColumns.TITLE, title.replace(".jpg", ""));
		values.put(ImageColumns.DISPLAY_NAME, title);
		values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
		values.put(ImageColumns.DATA, path);
		values.put(
				ImageColumns.ORIENTATION,
				cw.glass.facerecognization.FullImageActivity.this
						.getWindowManager().getDefaultDisplay().getRotation() + 90);

		Uri uri = null;

		uri = cw.glass.facerecognization.FullImageActivity.this
				.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI,
						values);

		imageView.setImageBitmap(result);
		isAllowed = true;
		TextView resultText = (TextView) findViewById(R.id.resultText);
		resultText.setText("Two finger tap to post on Facebook");
		return path;
	}

	public Uri getImageUri(Bitmap inImage) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = Images.Media.insertImage(this.getContentResolver(),
				inImage, "Title", null);
		return Uri.parse(path);
	}

	public static Bitmap mark(Bitmap src, String watermark, Point location,
			int color, int size, boolean underline) {
		int w = src.getWidth();
		int h = src.getHeight();
		Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());

		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(src, 0, 0, null);

		Paint paint = new Paint();
		paint.setARGB(200, 126, 53, 23);
		paint.setTextSize(size);
		paint.setAntiAlias(true);
		paint.setUnderlineText(underline);
		canvas.drawText(watermark, location.x, location.y, paint);

		return result;
	}

	public static Bitmap boost(Bitmap src, int type, float percent) {
		int width = src.getWidth();
		int height = src.getHeight();
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());

		int A, R, G, B;
		int pixel;

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				if (type == 1) {
					R = (int) (R * (1 + percent));
					if (R > 255)
						R = 255;
				} else if (type == 2) {
					G = (int) (G * (1 + percent));
					if (G > 255)
						G = 255;
				} else if (type == 3) {
					B = (int) (B * (1 + percent));
					if (B > 255)
						B = 255;
				}
				bmOut.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}
		return bmOut;
	}

	public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {

		int width = bm.getWidth();

		int height = bm.getHeight();

		float scaleWidth = ((float) newWidth) / width;

		float scaleHeight = ((float) newHeight) / height;

		Matrix matrix = new Matrix();

		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);

		return resizedBitmap;

	}

	public static Bitmap createTransparentBitmapFromBitmap(Bitmap bmp) {
		bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
		int[] intArray = new int[bmp.getWidth() * bmp.getHeight()];

		// copy pixel data from the Bitmap into the 'intArray' array
		bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(),
				bmp.getHeight());

		// replace the red pixels with yellow ones
		for (int i = 0; i < intArray.length; i++) {
			if (intArray[i] == 0xffffffff) {
				intArray[i] = Color.TRANSPARENT;
			}
		}

		// Initialize the bitmap, with the replaced color
		bmp = Bitmap.createBitmap(intArray, bmp.getWidth(), bmp.getHeight(),
				Bitmap.Config.ARGB_8888);

		return bmp;
	}

	public static int convertDpToPixel(float dp, Context context) {
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		int px = (int) (dp * (metrics.densityDpi / 160f));
		return px;
	}

	public Bitmap rotateImage(Bitmap b, int angle) {

		// create a matrix object
		Matrix matrix = new Matrix();
		matrix.postRotate(angle); // anti-clockwise by 90 degrees

		// create a new bitmap from the original using the matrix to transform
		// the result
		Bitmap rotatedBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
				b.getHeight(), matrix, true);
		b = rotatedBitmap;
		return b;
		// display the rotated bitmap

	}

	public static Bitmap decreaseColorDepth(Bitmap src, int bitOffset) {
		// get image size
		int width = src.getWidth();
		int height = src.getHeight();
		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;

		// scan through all pixels
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);

				// round-off color offset
				R = ((R + (bitOffset / 2))
						- ((R + (bitOffset / 2)) % bitOffset) - 1);
				if (R < 0) {
					R = 0;
				}
				G = ((G + (bitOffset / 2))
						- ((G + (bitOffset / 2)) % bitOffset) - 1);
				if (G < 0) {
					G = 0;
				}
				B = ((B + (bitOffset / 2))
						- ((B + (bitOffset / 2)) % bitOffset) - 1);
				if (B < 0) {
					B = 0;
				}

				// set pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}

		// return final image
		return bmOut;
	}

	public Bitmap getBlurredBitmap(Bitmap original, int radius) {
		if (radius < 1)
			return null;

		int width = original.getWidth();
		int height = original.getHeight();
		int wm = width - 1;
		int hm = height - 1;
		int wh = width * height;
		int div = radius + radius + 1;
		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, p1, p2, yp, yi, yw;
		int vmin[] = new int[Math.max(width, height)];
		int vmax[] = new int[Math.max(width, height)];
		int dv[] = new int[256 * div];
		for (i = 0; i < 256 * div; i++)
			dv[i] = i / div;

		int[] blurredBitmap = new int[wh];
		original.getPixels(blurredBitmap, 0, width, 0, 0, width, height);

		yw = 0;
		yi = 0;

		for (y = 0; y < height; y++) {
			rsum = 0;
			gsum = 0;
			bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = blurredBitmap[yi + Math.min(wm, Math.max(i, 0))];
				rsum += (p & 0xff0000) >> 16;
				gsum += (p & 0x00ff00) >> 8;
				bsum += p & 0x0000ff;
			}
			for (x = 0; x < width; x++) {
				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
					vmax[x] = Math.max(x - radius, 0);
				}
				p1 = blurredBitmap[yw + vmin[x]];
				p2 = blurredBitmap[yw + vmax[x]];

				rsum += ((p1 & 0xff0000) - (p2 & 0xff0000)) >> 16;
				gsum += ((p1 & 0x00ff00) - (p2 & 0x00ff00)) >> 8;
				bsum += (p1 & 0x0000ff) - (p2 & 0x0000ff);
				yi++;
			}
			yw += width;
		}

		for (x = 0; x < width; x++) {
			rsum = gsum = bsum = 0;
			yp = -radius * width;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;
				rsum += r[yi];
				gsum += g[yi];
				bsum += b[yi];
				yp += width;
			}
			yi = x;
			for (y = 0; y < height; y++) {
				blurredBitmap[yi] = 0xff000000 | (dv[rsum] << 16)
						| (dv[gsum] << 8) | dv[bsum];
				if (x == 0) {
					vmin[y] = Math.min(y + radius + 1, hm) * width;
					vmax[y] = Math.max(y - radius, 0) * width;
				}
				p1 = x + vmin[y];
				p2 = x + vmax[y];

				rsum += r[p1] - r[p2];
				gsum += g[p1] - g[p2];
				bsum += b[p1] - b[p2];

				yi += width;
			}
		}

		return Bitmap.createBitmap(blurredBitmap, width, height,
				Bitmap.Config.RGB_565);
	}

	public Bitmap roundCorner(Bitmap src, float round) {
		// image size
		int width = src.getWidth();
		int height = src.getHeight();
		// create bitmap output
		Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		// set canvas for painting
		Canvas canvas = new Canvas(result);
		canvas.drawARGB(0, 0, 0, 0);
		// config paint
		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);
		paint.setAlpha(180);
		paint.setStrokeWidth(30);
		final Paint paint1 = new Paint();
		paint1.setAntiAlias(true);
		paint1.setStrokeWidth(15);

		paint.setStyle(Paint.Style.STROKE);

		paint1.setStyle(Paint.Style.STROKE);
		paint1.setColor(Color.WHITE);

		// config rectangle for embedding
		final Rect rect = new Rect(0, 0, width, height);
		// final RectF rectF = new RectF(rect);

		final Rect rect1 = new Rect(20, 20, width - 20, height - 20);
		final RectF rectF1 = new RectF(rect1);

		// draw source image to canvas
		canvas.drawBitmap(src, rect1, rect1, paint1);
		// draw rect to canvas
		canvas.drawRect(rect, paint);
		// create Xfer mode
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		// canvas.drawRoundRect(rectF, 0, 0, paint);
		canvas.drawRoundRect(rectF1, round, round, paint1);

		// return final image
		return result;
	}

	public static Bitmap doGreyscale(Bitmap src) {
		// constant factors
		final double GS_RED = 0.299;
		final double GS_GREEN = 0.587;
		final double GS_BLUE = 0.114;

		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
				src.getConfig());
		// pixel information
		int A, R, G, B;
		int pixel;

		// get image size
		int width = src.getWidth();
		int height = src.getHeight();

		// scan through every single pixel
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get one pixel color
				pixel = src.getPixel(x, y);
				// retrieve color of all channels
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				// take conversion up to one single value
				R = G = B = (int) (GS_RED * R + GS_GREEN * G + GS_BLUE * B);
				// set new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}

		// return final image
		return bmOut;
	}

	public Bitmap frames(Bitmap src, float round, int frame, int size) {
		// image size
		int width = src.getWidth();
		int height = src.getHeight();
		// create bitmap output
		Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Bitmap resizedBitmap = BitmapFactory.decodeResource(getResources(),
				frame);
		Bitmap result1 = Bitmap.createScaledBitmap(resizedBitmap, width,
				height, false);

		// set canvas for painting
		Canvas canvas = new Canvas(result);
		canvas.drawARGB(0, 0, 0, 0);

		// config paint
		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);
		paint.setAlpha(128);
		final Paint paint1 = new Paint();
		paint1.setAntiAlias(false);
		paint1.setColor(Color.BLACK);

		// config rectangle for embedding
		// final Rect rect = new Rect(0, 0, width, height);
		// final RectF rectF = new RectF(rect);

		final Rect rect1 = new Rect(size, size, width - size, height - size);
		final RectF rectF1 = new RectF(rect1);
		// draw rect to canvas
		canvas.drawBitmap(result1, 0, 0, paint1);
		// canvas.drawRoundRect(rectF, 0, 0, paint);
		canvas.drawRoundRect(rectF1, round, round, paint1);
		// create Xfer mode
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		// draw source image to canvas
		canvas.drawBitmap(src, rect1, rect1, paint);

		// return final image
		return result;
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	// wake lock
	private void getPowerManger() {
		try {
			if (mPowerManager == null) {
				mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			}
			if (mWakeLock == null) {
				mWakeLock = mPowerManager.newWakeLock(
						PowerManager.FULL_WAKE_LOCK, "Glass Life Lock");
			}
			acquireWakeLock();
		} catch (Exception e) {
			Log.e("FullImage.onCreate",
					"getPowerManger.Exception " + e.toString());
			Utils.LogIt(FullImageActivity.this, "FullImage Activity",
					"onCreate", "getPowerManger.Exception " + e.getMessage());
		}
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

	OnClickListener imgListner = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (isAllowed == true) {
				faceBookPosting();
			}
		}
	};

	private void imageViewSetting() {

		File imgFile = new File(imageLocation);

		Bitmap bitmapCompr = BitmapFactory
				.decodeFile(imgFile.getAbsolutePath());
		try {
			Bitmap result = BitmapFactory.decodeFile(imageLocation);
			result = ImageUtility.ScaleDownBitmap(result,
					result.getWidth() * 0.58f);
			imageView.setImageBitmap(result);
		} catch (Exception e) {
			Log.e("Exception file not found", e.toString());
		}
		imgFile = null;

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmapCompr.compress(Bitmap.CompressFormat.JPEG, 38, outputStream);
		bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(
				outputStream.toByteArray()));

		bitmapCompr.recycle();
		bitmapCompr = null;
	}

	private void textToSpeech() {
		mTextToSpeech = new TextToSpeech(FullImageActivity.this,
				new OnInitListener() {

					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS) {
							mTextToSpeech.setPitch(0.8f);
							mTextToSpeech.setSpeechRate(1.1f);
						}
					}
				});
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// location service connection
	private void isServiceRunning() {
		boolean isRunning = Utils
				.isLocationServiceRunning(FullImageActivity.this);
		Log.d("FullImageActivity", "service is "
				+ (isRunning ? "running" : "not running"));
		if (!isRunning) {
			startService(new Intent(FullImageActivity.this,
					LocationService.class));
		}
	}

	private void doBindService() {
		bindService(new Intent(this, LocationService.class), serviceConnection,
				Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	private void doUnBindService() {
		if (isBound) {
			try {
				mLocationService.onStopService();
				unbindService(serviceConnection);
				mLocationService = null;
				serviceConnection = null;
				isBound = false;
			} catch (Exception e) {
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"doUnBindService", "stop service exception");
				Log.e("Full Image Activity",
						"stop service exception " + e.toString());
				e.printStackTrace();
			}
		}
	}

	// result from LocationTask
	@Override
	public void setLocationId(String jsonData) {
		try {
			if (jsonData.contains("data")) {
				JSONObject json = new JSONObject(jsonData);
				JSONArray jsonArray = json.getJSONArray("data");
				JSONObject jsonObject = jsonArray.getJSONObject(0);
				mLocationId = jsonObject.getString("id");
				Log.d("LocationId", "inside setLocationId() locationId = "
						+ mLocationId);
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"setLocationId", "mLocationId " + mLocationId);
			} else {
				Utils.LogIt(FullImageActivity.this, "Full Image Activity",
						"setLocationId", "no data found");
			}
		} catch (Exception e) {
			Log.e("setLocationId data is null", "Exception " + e.toString());
			Utils.LogIt(FullImageActivity.this, "Full Image Activity",
					"setLocationId", "Exception " + e.getMessage());
		}
	}

	// service bounding
	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d("FullImageActivity", "onServiceDisconnected");
			mLocationService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d("FullImageActivity", "onServiceConnected");
			mLocationService = ((LocationServiceBinder) service).getService();
			if (mLocationService == null) {
				Log.d("onServiceConnected", "mLocationService is null");
			} else {
				Log.d("onServiceConnected", "mLocationService is not null");
				String mLocation = mLocationService.getCurrentLocation();
				LocationTask task = new LocationTask(FullImageActivity.this);
				if (mLocation != null) {
					Log.d("onServiceConnected", "location is " + mLocation);
					task.execute(mLocation);
				} else {
					task.execute("33.7167,73.0667");
					Log.d("onServiceConnected",
							"current location is null, location = set 33.7167,73.0667");
				}

			}
		}
	};
}
