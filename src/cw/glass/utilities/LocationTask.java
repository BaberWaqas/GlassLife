package cw.glass.utilities;

import java.net.URLEncoder;

import android.os.AsyncTask;

// @param String -> doInBackground
// @param Void -> onProgressUpdate
// @param String -> onPostExecute

public class LocationTask extends AsyncTask<String, Void, String> {

	IFacebookLocation mLocationListner;

	public LocationTask(IFacebookLocation locationListner) {
		mLocationListner = locationListner;
	}

	@Override
	protected String doInBackground(String... location) {
		String result = null;

		String Url;
		try {
			Url = SystemUtilities.FBLocationSearchURL
					+ URLEncoder.encode(location[0], "UTF-8");
			
			RestClient mClient = new RestClient();
			result = mClient.DetectLocation(Url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		if (mLocationListner != null) {
			mLocationListner.setLocationId(result);
		}
	}

}
