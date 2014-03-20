package cw.glass.wifi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import cw.glass.glasslife.R;
import cw.glass.utilities.DatabaseHandler;
import cw.glass.utilities.Logfile;

public class WifiActivity extends Activity {
	TextView mainText;
	WifiManager mainWifi;
	WifiReceiver receiverWifi;
	List<ScanResult> wifiList;
	StringBuilder sb = new StringBuilder();
	DatabaseHandler db;
	Calendar c;
	SimpleDateFormat df;
	String formattedDate;
	String totaldata;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi);

		mainText = (TextView) findViewById(R.id.mainText);

		// Initiate wifi service manager
		mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		db = new DatabaseHandler(this);

		/**** Log file **/
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Wifi Activity", "Start", "Oncreate",
				formattedDate));
		db.close();
		// Check for wifi is disabled
		if (mainWifi.isWifiEnabled() == false) {
			c = Calendar.getInstance();
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formattedDate = df.format(c.getTime());
			Log.d("before log", "log created");
			db.addLog(new Logfile("Wifi Activity", "mainWifi",
					"mainWifi.isWifiEnabled() == false", formattedDate));
			db.close();
			// If wifi disabled then enable it
			Toast.makeText(getApplicationContext(),
					"wifi is disabled..making it enabled", Toast.LENGTH_LONG)
					.show();

			mainWifi.setWifiEnabled(true);
		}

		// wifi scaned value broadcast receiver
		receiverWifi = new WifiReceiver();

		// Register broadcast receiver
		// Broacast receiver will automatically call when number of wifi
		// connections changed
		registerReceiver(receiverWifi, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		mainWifi.startScan();
		mainText.setText("Starting Scan...");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Refresh");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Wifi Activity", "onMenuItemSelected", "called",
				formattedDate));
		db.close();
		mainWifi.startScan();
		mainText.setText("Starting Scan");
		return super.onMenuItemSelected(featureId, item);
	}

	protected void onPause() {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Wifi Activity", "onPause", "called",
				formattedDate));
		db.close();
		unregisterReceiver(receiverWifi);
		super.onPause();
	}

	protected void onResume() {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Wifi Activity", "onresume", "called",
				formattedDate));
		db.close();
		registerReceiver(receiverWifi, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		super.onResume();
	}

	@Override
	public void onBackPressed() {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Wifi Activity", "OnBAckPressed", "called",
				formattedDate));
		db.close();
		WifiActivity.this.finish();

		super.onBackPressed();
	}

	class WifiReceiver extends BroadcastReceiver {

		// This method call when number of wifi connections changed
		public void onReceive(Context c, Intent intent) {
			// c = Calendar.getInstance();
			// df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// formattedDate = df.format(c.getTime());
			Log.d("before log", "log created");
			db.addLog(new Logfile("Wifi Activity", "OnReceive", "called",
					formattedDate));
			db.close();
			sb = new StringBuilder();
			wifiList = mainWifi.getScanResults();
			sb.append("\n        Number Of Wifi connections :"
					+ wifiList.size() + "\n\n");

			for (int i = 0; i < wifiList.size(); i++) {

				sb.append(new Integer(i + 1).toString() + ". ");
				sb.append(wifiList.get(i).SSID);
				sb.append("\n\n");
			}

			mainText.setText(sb);
		}

	}

}
