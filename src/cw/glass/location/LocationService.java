package cw.glass.location;

import java.util.List;
import cw.glass.utilities.Utils;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class LocationService extends Service implements LocationListener {

	private final String TAG = "LocationService";
	private static final int MIN_TIME = 1000 * 5;
	private final IBinder iBinder = new LocationServiceBinder();
	private LocationManager mLocationManager;
	private Criteria mCriteria;
	private Location lastLocation = null;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		initializeLocationManager();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		if (mLocationManager != null) {
			mLocationManager.getBestProvider(mCriteria, true);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		onStopService();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return iBinder;
	}

	// location updates
	private void initializeLocationManager() {
		mLocationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		mCriteria = new Criteria();
		mCriteria.setAccuracy(Criteria.ACCURACY_FINE);
		mCriteria.setBearingRequired(false);
		mCriteria.setSpeedRequired(false);
		getLocation();
	}

	// Helper methods
	public void onStopService() {
		Log.d(TAG, "onStopService called.");
		stopSelf();
	}

	public String getCurrentLocation() {
		getLocation();
		if (lastLocation != null) {
			Log.e(TAG, "getCurrentLocation is " + lastLocation);
			Utils.LogIt(this, "Location Service", "getCurrentLocation",
					"getCurrentLocation lastLocation is " + lastLocation);
			return lastLocation.getLatitude() + ","
					+ lastLocation.getLongitude();
		}
		Log.e(TAG, "getCurrentLocation is null");
		Utils.LogIt(this, "Location Service", "getCurrentLocation",
				"getCurrentLocation lastLocation is NULL");
		return null;
	}

	private void getLocation() {
		List<String> providers = mLocationManager.getProviders(mCriteria, true);

		Utils.LogIt(this, "Location Service", "getLocation",
				"getLocation for providers = " + providers);

		for (String provider : providers) {
			try {
				mLocationManager.requestLocationUpdates(provider, MIN_TIME,
						5.0f, this);
				Utils.LogIt(this, "Location Service", "getLocation",
						"getLocation.requestLocationUpdates for provider "
								+ provider);
			} catch (Exception e) {
				Log.e(TAG, "getLocation failed for provider = " + provider);
				Utils.LogIt(this, "Location Service", "getLocation",
						"getLocation failed for provider = " + provider);
			}
		}

		try {
			String mLocationProvider = null;
			if (providers.size() == 0) {

				mLocationProvider = mLocationManager.getBestProvider(mCriteria,
						true);

				Location lastLoc = mLocationManager
						.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

				if (lastLoc != null) {
					lastLocation = lastLoc;
					Log.e(TAG, lastLocation.toString());
					Utils.LogIt(this, "Location Service", "getLocation",
							"getLocation using getLastKnownLocation " + lastLoc);
				}

				if (mLocationProvider != null) {
					mLocationManager.requestLocationUpdates(mLocationProvider,
							MIN_TIME, 0.0f, this);

					Utils.LogIt(this, "Location Service", "getLocation",
							"getLocation using requestLocationUpdates using sinlge provider");
				}
			}
		} catch (Exception e) {
			Log.e(TAG,
					"requestLocationUpdates using single provider not working "
							+ e.toString());
		}
	}

	private void processLocationData(Location location) {
		if (location == null) {
			Log.e(TAG, "processLocationData called with null location");
			Utils.LogIt(this, "Location Service", "processLocationData",
					"null location");
		} else if (lastLocation == null) {
			lastLocation = location;
		} else {
			Log.e(TAG, "processLocationData location " + location.getLatitude()
					+ "," + location.getLongitude());

			float lastAcuuracy = lastLocation.getAccuracy();
			float currentAcuuracy = location.getAccuracy();

			// check whether the current data is better than the last one
			if (currentAcuuracy < lastAcuuracy) {
				lastLocation = location;
			} else {
				// if new location is different from last location
				// (within the accuracy of the new data),
				// then update the location. Otherwise, consider the new data as
				// "noise".

				double difLatSquare = Math.pow(
						(location.getLatitude() - lastLocation.getLatitude()),
						2);

				double difLonSquare = Math
						.pow((location.getLongitude() - lastLocation
								.getLongitude()), 2);
				double difAltSquare = Math.pow(
						(location.getAltitude() - lastLocation.getAltitude()),
						2);

				double distance = Math.sqrt(difLatSquare + difLonSquare
						+ difAltSquare);

				if (distance >= currentAcuuracy * 0.001) {
					lastLocation = location;
				} else {
					Log.e(TAG,
							"New data is within the error bar from the last location: distance = "
									+ distance + "; new location accuracy = "
									+ currentAcuuracy);
				}
			}
		}
	}

	// binder class
	public class LocationServiceBinder extends Binder {
		public LocationService getService() {
			return LocationService.this;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		processLocationData(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.e(TAG, "onProviderDisabled provider " + provider);
		Utils.LogIt(this, "Location Service", "onProviderDisabled", "provider "
				+ provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.e(TAG, "onProviderEnabled provider " + provider);
		Utils.LogIt(this, "Location Service", "onProviderEnabled", "provider "
				+ provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.e(TAG, "onStatusChanged provider " + provider + " status " + status);
		Utils.LogIt(this, "Location Service", "onStatusChanged", "provider "
				+ provider + " status " + status);
	}

}
