package cw.glass.facerecognization;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.glass.media.CameraManager;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import cw.glass.glasslife.MainActivity;
import cw.glass.glasslife.R;
import cw.glass.utilities.FacePoints;
import cw.glass.utilities.ImageUtility;
import cw.glass.utilities.RestClient;
import cw.glass.utilities.Utils;

public class CameraActivity extends Activity {

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static String FILE_NAME = null;
	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
	private static final String DETECT_URI = "https://animetrics.p.mashape.com/detect?api_key=392a287e500662d0add83eaacbf9f263";
	private static final String RECOGNIZE_URI = "https://animetrics.p.mashape.com/recognize?api_key=392a287e500662d0add83eaacbf9f263&gallery_id=FRIENDS_MEMBERS&image_id=";
	static int check = 0;
	private RestClient restClient = null;
	private TextView resultText;
	private static String result;
	public byte[] stream = null;
	ImageView imageView;
	Calendar c;
	SimpleDateFormat df;
	String formattedDate;
	static String imageLocation;
	String imageID;
	String width, height;
	String lipLineX, lipLineY, rightNostrilY, leftNostrilY;
	String topLeftX, topLeftY;
	boolean done = true, isProcessStart = false;

	ArrayList<FacePoints> facePoints = new ArrayList<FacePoints>();

	// for monocle on right eye
	String rightEyeBrowLeftX, rightEyeBrowLeftY, rightEyeBrowRightX,
			rightEyeBrowRightY;

	String hat, mustaches;
	String identifier;
	static String picturePath;
	boolean isAllowed = false;
	private static final int TAKE_PICTURE_REQUEST = 1;
	String predictedPerson;
	private GestureDetector mGlassDetector;
	PowerManager mPowerManager;
	WakeLock mWakeLock;
	LinearLayout mProgressBarLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		resultText = (TextView) findViewById(R.id.resultText);
		imageView = (ImageView) findViewById(R.id.camera_image);
		mProgressBarLayout = (LinearLayout) findViewById(R.id.progress_container);
//
//		imageView.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View view) {
//				if (isAllowed) {
//					detectFaceResults();
//				}
//			}
//		});

		try {
			getPowerManger();
		} catch (Exception e) {
			Log.e("Camera.onCreate", "getPowerManger.Exception " + e.toString());
			Utils.LogIt(CameraActivity.this, "Camera Activity", "onCreate",
					"getPowerManger.Exception " + e.getMessage());
		}
		try {
			mGlassDetector = createGestureDetector(this);
			Utils.LogIt(CameraActivity.this, "Camera Activity", "onCreate",
					"gesture detected");
		} catch (Exception ex) {
			getExceptionDialog(ex, "gesture detector on glass failed");
		}

		resultText.setMovementMethod(new ScrollingMovementMethod());
		FILE_NAME = Utils.fileNameCreator("Img");
		startCamera();
	}

	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TWO_TAP) {
					if (isAllowed) {
						Utils.LogIt(CameraActivity.this, "Camera Activity",
								"onGesture", "Gesture.TWO TAP");
						detectFaceResults();
					}
					if (gesture == Gesture.SWIPE_DOWN) {
						CameraActivity.this.finish();
						return true;
					}
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
		Utils.LogIt(CameraActivity.this, "Camera Activity", "onBackPressed",
				"inside onBackPressed");
		CameraActivity.this.finish();
		super.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		isProcessStart = false;
		try {
			if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
				try {
					// glass way of getting camera image
					getPhotoPath(data);
				} catch (Exception e) {
					// mobile way of getting camera image
					if (data != null) {
						getPhotoPath(e, data);
					}
				}
				startBitmapProcess();
			} else {
				Utils.LogIt(this, "Camera Activity", "onActivityResult",
						"Re starting camera");
				startCamera();
			}
		} catch (Exception e) {
			getExceptionDialog(e, "data not found");
			resultText.setText("data not found");
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		try {
			if (!isProcessStart)
				releaseWakeLock();
		} catch (Exception e) {
			Log.e("Camera.onPause", "releaseWakeLock.Exception " + e.toString());
			Utils.LogIt(CameraActivity.this, "Camera Activity", "onPause",
					"releaseWakeLock.Exception " + e.getMessage());
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		try {
			if (!isProcessStart)
				releaseWakeLock();
		} catch (Exception e) {
			Log.e("Camera.onDestroy",
					"releaseWakeLock.Exception " + e.toString());
			Utils.LogIt(CameraActivity.this, "Camera Activity", "onDestroy",
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
			Log.e("Camera.onResume", "getPowerManger.Exception " + e.toString());
			Utils.LogIt(CameraActivity.this, "Camera Activity", "onResume",
					"getPowerManger.Exception " + e.getMessage());
		}
	}

	// Helper methods
	private void getExceptionDialog(Exception e, String message) {

		Utils.LogIt(CameraActivity.this, "Camera Activity",
				"onActivityResult getExceptionDialog ", " Exception due to "
						+ e.toString());

		AlertDialog.Builder dialog = new AlertDialog.Builder(
				CameraActivity.this);
		dialog.setTitle(message);
		dialog.setNeutralButton("Cancel", null);
		dialog.create().show();
	}

	public void startCamera() {
		try {
			Utils.LogIt(CameraActivity.this, "Camera Activity", "startCamera",
					"startCamera request");
			isProcessStart = true;
			startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
					TAKE_PICTURE_REQUEST);

		} catch (Exception ex) {
			getExceptionDialog(ex, "Failed to start camera ");
		}
	}

	private void getPhotoPath(Intent data) {

		picturePath = data
				.getStringExtra(CameraManager.EXTRA_PICTURE_FILE_PATH);
		processPictureWhenReady(picturePath);
		imageLocation = picturePath;

		Log.e("Camera", "getPhotoPath path " + imageLocation);
		Utils.LogIt(CameraActivity.this, "Camera Activity", "getPhotoPath",
				"getPhotoPath imageLocation = " + imageLocation);
	}

	private void getPhotoPath(Exception e, Intent data) {
		Uri selectedImageUri = data.getData();
		if (selectedImageUri != null) {
			imageLocation = getPath(selectedImageUri);
			Utils.LogIt(CameraActivity.this, "Camera Activity", "getPhotoPath",
					"getPath imageLocation = " + imageLocation);
			setBitmap();
		} else {
			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"onActivityResult",
					"data.getData() returns imageLocation is null");
		}
	}

	private void startBitmapProcess() {
		try {
			if (!mProgressBarLayout.isShown()) {
				mProgressBarLayout.setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
			Log.e("Camera", "startBitmapProcess exception " + e.toString());
			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"startBitmapProcess", "Exception " + e.getMessage());
		}
		Utils.LogIt(this, "Camera Activity", "startBitmapProcess",
				"ProgressDialog is not null");
		BackGroundTask task = new BackGroundTask();
		task.execute();
	}

	private String otherwork() {
		try {
			Utils.LogIt(CameraActivity.this, "Camera Activity", "otherwork",
					"inside otherwork()");

			if (imageLocation == null) {
				Log.e("Camera", "otherwork imageLocation is null ");
				getMediaPhotoPath();
			} else {
				Log.e("Camera", "otherwork imageLocation is " + imageLocation);
				Utils.LogIt(CameraActivity.this, "Camera Activity",
						"otherwork", "got file location" + imageLocation);
			}

			File imageFile = new File(imageLocation);
			int counter = 1;
			do {
				Utils.LogIt(CameraActivity.this, "Camera Activity",
						"otherwork", "file not yet written " + counter
								+ " seconds");

				Log.e("otherwork", "file not yet written " + counter
						+ " seconds");

				Thread.sleep(2000);
				counter += 2;
			} while (!imageFile.exists());

			imageFile = null;
			photoProcessing();

		} catch (Exception e) {
			Log.e("Exception ", "image processing failed " + e.toString());
			Utils.LogIt(CameraActivity.this, "Camera Activity", "otherwork",
					"image processing failed " + e.getMessage());
		}
		done = false;
		return result;
	}

	private void photoProcessing() throws Exception {

		Bitmap cameraPic = BitmapFactory.decodeFile(imageLocation);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		cameraPic.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
		cameraPic.recycle();

		stream = outputStream.toByteArray();

		if (stream != null) {
			if (restClient == null) {
				restClient = new RestClient();
			}
			// image detection
			detectionResult();
		}
	}

	private void detectionResult() throws Exception {

		String fileNameOriginal = imageLocation.substring(imageLocation
				.lastIndexOf("/") + 1);

		Log.e("detectionResult", "detectionResult imageName "
				+ fileNameOriginal);

		Utils.LogIt(CameraActivity.this, "Camera Activity", "detectionResult",
				" fileNameOriginal" + fileNameOriginal);

		result = restClient.executeDetect(DETECT_URI, stream, fileNameOriginal);
		stream = null;

		Log.e("detectionResult()", "detection result " + result);

		if (result.contains("topLeftX") && result.contains("topLeftY")
				&& result.contains("Complete")) {

			JSONObject jsnString = new JSONObject(result);
			JSONArray jsonImages = jsnString.getJSONArray("images");

			JSONObject jsnObjWidthHeight = jsonImages.getJSONObject(0);

			imageID = jsnObjWidthHeight.getString("image_id");

			JSONArray jsonFaces = jsnObjWidthHeight.getJSONArray("faces");

			int length = jsonFaces.length();

			Log.e("detectionResult", "faces found " + length);

			for (int i = 0; i < length; i++) {

				Log.e("detectionResult", "detection result face no " + i);

				JSONObject jsnObjFaces = jsonFaces.getJSONObject(i);
				width = jsnObjFaces.getString("width");
				height = jsnObjFaces.getString("height");

				topLeftX = jsnObjFaces.getString("topLeftX");
				topLeftY = jsnObjFaces.getString("topLeftY");

				lipLineX = jsnObjFaces.getString("lipLineMiddleX");
				lipLineY = jsnObjFaces.getString("lipLineMiddleY");

				rightNostrilY = jsnObjFaces
						.getString("nostrilRightHoleBottomY");
				leftNostrilY = jsnObjFaces.getString("nostrilLeftHoleBottomY");

				String nostrilLineY;
				if (Float.parseFloat(rightNostrilY) > Float
						.parseFloat(leftNostrilY))
					nostrilLineY = rightNostrilY;
				else
					nostrilLineY = leftNostrilY;

				// @param for monocle
				rightEyeBrowLeftX = jsnObjFaces.getString("rightEyeBrowLeftX");
				rightEyeBrowLeftY = jsnObjFaces.getString("rightEyeBrowLeftY");
				rightEyeBrowRightX = jsnObjFaces
						.getString("rightEyeBrowRightX");
				rightEyeBrowRightY = jsnObjFaces
						.getString("rightEyeBrowRightY");

				String rPath = RECOGNIZE_URI + imageID + "&topLeftX="
						+ topLeftX + "&topLeftY=" + topLeftY + "&width="
						+ width + "&height=" + height;

				FacePoints point = new FacePoints();
				point.setRightEyeBrowLeftX(rightEyeBrowLeftX);
				point.setRightEyeBrowLeftY(rightEyeBrowLeftY);
				point.setRightEyeBrowRightX(rightEyeBrowRightX);
				point.setRightEyeBrowRightY(rightEyeBrowRightY);
				point.setImageID(imageID);
				point.setWidth(width);
				point.setHeight(height);
				point.setLipLineX(lipLineX);
				point.setLipLineY(lipLineY);
				point.setRightNostrilY(nostrilLineY);
				point.setLeftNostrilY(nostrilLineY);
				point.setTopLeftX(topLeftX);
				point.setTopLeftY(topLeftY);

				// image recognization
				// result = restClient.executeRecognize(rPath);

				isAllowed = true;
				point.setPredictedPerson("testing");

				// find predected person for each face
				// findPrediction(point, result);

				Log.d("executeRecognize result of face no " + i, "face no " + i
						+ " resut " + result);

				Utils.LogIt(CameraActivity.this, "Camera Activity",
						"detectionResult()", "finish detectionResult for face "
								+ i + "/" + length);

				// add details in list
				facePoints.add(point);
			}
		} else {
			stream = null;
			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"detectionResult()", "face in the images did not detected.");
			result = "Face(s) in the image(s) did not detected.";
		}
	}

	private void findPrediction(FacePoints point, String recognizeResut) {

		if (recognizeResut.contains("Complete")
				&& recognizeResut.contains("face_id")
				&& recognizeResut.contains("image_id")
				&& recognizeResut.contains("candidates")
				&& recognizeResut.contains("transaction")) {

			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"findPrediction", "inside findPrediction");
			try {
				Log.d("findPrediction INSIDE TRY ", "findPrediction INSIDE TRY");

				JSONObject jsnString = new JSONObject(recognizeResut);
				JSONArray jsonImages = jsnString.getJSONArray("images");
				JSONObject jsnObject = jsonImages.getJSONObject(0);
				JSONObject jsnObjCandidate = jsnObject
						.getJSONObject("candidates");

				Iterator iter = jsnObjCandidate.keys();
				double max2 = 0.0;
				double max = 0.0;
				predictedPerson = "";
				String predictedPerson2 = "";

				Log.d("findPrediction() before while ",
						"findPrediction before while");

				while (iter.hasNext()) {

					String key = (String) iter.next();
					double value = Double.parseDouble(jsnObjCandidate
							.getString(key));

					if (max <= value) {
						max2 = max;
						predictedPerson2 = predictedPerson;
						max = value;
						predictedPerson = key;
					}
					if (max2 < value) {
						if (max == value)
							continue;
						max2 = value;
						predictedPerson2 = key;
					}
				}
				Log.d("findPrediction()", "predictedPerson " + predictedPerson
						+ ", predictedPerson2 " + predictedPerson2);

				File imgFile = new File(imageLocation);
				if (imgFile.exists()) {
					identifier = "both";
					isAllowed = true;
					Utils.LogIt(CameraActivity.this, "Camera Activity",
							"findPrediction", "starting full image activity");
					// set identifier and predicted person
					point.setPredictedPerson(predictedPerson);
				}
			} catch (Exception e) {
				Utils.LogIt(CameraActivity.this, "Camera Activity",
						"findPrediction()",
						"Exception findPrediction " + e.getMessage());

				Log.e("findPrediction() EXCEPTION",
						"findPrediction " + e.toString());
			}
		}
	}

	private void getMediaPhotoPath() {

		String[] projection = new String[] {
				MediaStore.Images.ImageColumns._ID,
				MediaStore.Images.ImageColumns.DATA,
				MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
				MediaStore.Images.ImageColumns.DATE_TAKEN,
				MediaStore.Images.ImageColumns.MIME_TYPE };

		final Cursor cursor = this.getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
				null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

		if (cursor.moveToFirst()) {

			Utils.LogIt(CameraActivity.this, "Camera Activity", "otherwork",
					"cursor.moveToFirst()");

			imageLocation = cursor.getString(1);
			Log.d("imageLocation", imageLocation.toString());

			Utils.LogIt(CameraActivity.this, "Camera Activity", "otherwork",
					"imageLocation = " + imageLocation.toString());

			String imageDate = cursor.getString(3);
			if (imageDate.contains(new Date().toString())) {
				String format2 = imageDate;
				Log.d("getMediaPhotoPath", "getMediaPhotoPath format "
						+ format2);
			}
		}
		cursor.close();
	}

	private void setBitmap() {
		Bitmap result = BitmapFactory.decodeFile(imageLocation);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		result.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
		result = ImageUtility
				.ScaleDownBitmap(result, result.getWidth() * 0.30f);
		imageView.setImageBitmap(result);
	}

	private void processPictureWhenReady(final String picturePath) {
		final File pictureFile = new File(picturePath);
		Utils.LogIt(CameraActivity.this, "Camera Activity",
				"processPictureWhenReady", "starting beforepicture.exists");
		if (pictureFile.exists()) {
			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"processPictureWhenReady", "if picture.exists()");

			// we don't need this??
			CameraActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						Utils.LogIt(CameraActivity.this, "setting UI",
								"processPictureWhenReady",
								"runOnUiThread before setBitmap");
						setBitmap();
					} catch (Exception e) {
						Utils.LogIt(CameraActivity.this, "setting UI",
								"processPictureWhenReady.runOnUiThread",
								"can not update UI from this thread");
					}
				}
			});
			// The picture is ready process it.
		} else {
			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"processPictureWhenReady", "else picture.exists()");
			final File parentDirectory = pictureFile.getParentFile();
			FileObserver observer = new FileObserver(parentDirectory.getPath()) {
				private boolean isFileWritten;

				@Override
				public void onEvent(int event, String path) {
					if (!isFileWritten) {
						Utils.LogIt(CameraActivity.this, "Camera Activity",
								"processPictureWhenReady", "!isFileWritten");

						File affectedFile = new File(parentDirectory, path);
						isFileWritten = (event == FileObserver.CLOSE_WRITE && affectedFile
								.equals(pictureFile));
						if (isFileWritten) {
							Utils.LogIt(CameraActivity.this, "Camera Activity",
									"processPictureWhenReady", "isFileWritten");
							stopWatching();

							Thread thread = new Thread(new Runnable() {

								@Override
								public void run() {
									Utils.LogIt(CameraActivity.this,
											"Camera Activity",
											"processPictureWhenReady",
											"runOnUiThread");
									processPictureWhenReady(picturePath);
								}
							});
							thread.setPriority(Thread.MAX_PRIORITY);
							thread.start();
							// run on main UI thread
							CameraActivity.this.runOnUiThread(thread);
						}
					}
				}
			};
			observer.startWatching();
		}
	}

	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = this.getContentResolver().query(uri, projection, null,
				null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	private void detectFaceResults() {
		try {
			File imgFile = new File(imageLocation);
			if (imgFile.exists()) {

				Intent in = new Intent(this, FullImageActivity.class);

				in.putParcelableArrayListExtra("faces_results", facePoints);
				in.putExtra("imageLocation", imageLocation);
				in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TOP);
				finish();
				startActivity(in);
			}
		} catch (Exception e) {
			Utils.LogIt(CameraActivity.this, "Camera Activity",
					"detectFaceResults()", "Exception " + e.toString());
		}
	}

	// wake lock
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

	// background tasks
	class BackGroundTask extends AsyncTask<Void, Void, String> {

		public BackGroundTask() {
			super();
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				Thread.sleep(3000);
				result = otherwork();
				Utils.LogIt(CameraActivity.this, "Camera Activity",
						"doInBackground", "inside doInBackground");

			} catch (Exception e) {
				Utils.LogIt(CameraActivity.this, "Camera Activity",
						"doInBackground",
						"doInBackground exception " + e.toString());
				// ex.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(String response) {
			Log.e("onPostExecute ", "response " + response);
			if (mProgressBarLayout.isShown()) {
				mProgressBarLayout.setVisibility(View.GONE);
			}
			if (!isAllowed) {
				resultText.setTextColor(Color.RED);
				resultText.setText("Face is not recognized");
			} else {

				String predictedPersons = "";
				for (FacePoints item : facePoints) {
					predictedPersons += item.getPredictedPerson() + ", ";
				}

				boolean isMoreThanTwoPersons = facePoints.size() > 1 ? true
						: false;

				String content = "Predected person"
						+ (isMoreThanTwoPersons ? "s are " : " is ");

				predictedPersons = predictedPersons.substring(0,
						predictedPersons.lastIndexOf(","));

				resultText.setText(content + predictedPersons);
			}
		}
	}
}
