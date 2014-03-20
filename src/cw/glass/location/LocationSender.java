package cw.glass.location;

import java.io.InputStream;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("SimpleDateFormat")
public class LocationSender {

	Context context;
	static LocationManager locationManager;
	Location location;
	InputStream is;
	static String test = "check";

	public LocationSender(Context ctx) {
		context = ctx;
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);
		locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

	}

	// Acquire a reference to the system Location Manager
	public void getNewLocation(Location location) throws JSONException {
		String latLongString = "";
		if (location != null) {
			this.location = location;
			LocationActivity.lat = location.getLatitude();
			LocationActivity.lng = location.getLongitude();
			latLongString = "Lat:" + String.valueOf(LocationActivity.lat)
					+ "\nLong:" + String.valueOf(LocationActivity.lng);
			Log.d("Location Found", latLongString);
			LocationActivity.latitude.setText("" + LocationActivity.lat);
			LocationActivity.longitude.setText("" + LocationActivity.lng);
		} else {
			location = null;
			latLongString = "No location found";
		}
		Toast.makeText(context, latLongString, Toast.LENGTH_LONG).show();
	}

	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			try {
				if (test.contains("check")) {
					getNewLocation(location);
				} else {
					locationManager.removeUpdates(locationListener);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		public void onProviderEnabled(String provider) {

			Toast.makeText(context, "Provider: " + provider + " : Enabled",
					Toast.LENGTH_LONG).show();
		}

		public void onProviderDisabled(String provider) {

			Toast.makeText(context, "Provider: " + provider + " : disabled",
					Toast.LENGTH_LONG).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Toast.makeText(context,
					"Provider: " + provider + " : status: " + status,
					Toast.LENGTH_LONG).show();

		}
	};

}