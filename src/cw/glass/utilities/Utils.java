package cw.glass.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import cw.glass.location.LocationService;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

@SuppressLint("SimpleDateFormat")
public class Utils {
	// convert from bitmap to byte array

	public static File getAlbumStorageDir(String albumName) {
		// Get the directory for the user's public pictures directory.
		File file = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				albumName);
		if (!file.mkdirs()) {
			Log.e("UtilityClass", "Directory not created in getAlbumStorageDir");
		}
		return file;
	}

	public static void LogIt(Context context, String activityName,
			String method, String Comments) {

		DatabaseHandler db = new DatabaseHandler(context);
		Calendar c;
		SimpleDateFormat df;
		String formattedDate;

		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());

		Comments = SolveSqlInjection(Comments);
		method = SolveSqlInjection(method);

		db.addLog(new Logfile(activityName, method, Comments, formattedDate));
		db.close();
		db = null;

	}

	private static String SolveSqlInjection(String comments) {
		comments = comments.replace("'", "");
		comments = comments.replace(";", "");
		comments = comments.replace("--", "");
		comments = comments.replace("/*", "");
		comments = comments.replace("*/", "");
		comments = comments.replace("<", "");
		comments = comments.replace(">", "");
		comments = comments.replace("&", "");
		comments = comments.replace("|", "");
		return comments;
	}

	public static String fileNameCreator(String filePrefix) {

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = filePrefix + timeStamp + ".jpg";
		return imageFileName;
	}

	public static byte[] getBytes(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, 0, stream);
		return stream.toByteArray();
	}

	// convert from byte array to bitmap
	public static Bitmap getPhoto(byte[] image) {
		return BitmapFactory.decodeByteArray(image, 0, image.length);
	}

	public static boolean isLocationServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (LocationService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}