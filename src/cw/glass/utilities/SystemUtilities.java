/**
 * 
 */
package cw.glass.utilities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

/**
 * @author Aidikos
 * 
 */
public class SystemUtilities {

	public static final String FbUrl = "http://glasslife.azurewebsites.net/cw_core/facebook_photo?apikey=201312abc&resultType=json";
	public static final String FBLocationSearchURL ="https://graph.facebook.com/search?access_token=CAAUxbW84eYsBAKVdnjuVZCFhXVt5LZAvCG8grB3zvdeJy87hOPooG3fZA3hrYGbaN9LcZCUdJDYsQmsQjdsItnrKB3FhEpCceqvdghijFEZCaNS2SvqzZBE0ZBRBMvxrmvRik230coGqbTE4mSMOgXc7Ga1Ttw3YOZA4szwmBeZA6AZCq0T1U9gOFn&type=place&distance=1000&center=";
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

	@SuppressLint("SimpleDateFormat")
	public static String fileNameCreator(String filePrefix, String filePostfix) {

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = filePrefix + timeStamp + filePostfix;
		return imageFileName;
	}
}
