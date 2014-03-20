package cw.glass.location;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
import cw.glass.glasslife.R;
import cw.glass.utilities.DatabaseHandler;
import cw.glass.utilities.Logfile;

public class LocationActivity extends Activity {// implements
												// UncaughtExceptionHandler {
	// private static final int REQUEST_CODE = 100;
	public String response;
	public String result;
	LocationSender loc;
	public static TextView latitude, longitude;
	static double lat, lng;
	private ProgressDialog pd;
	DatabaseHandler db;
	Calendar c;
	SimpleDateFormat df;

	String formattedDate;
	String totaldata, reverseString;

	private void finishscreen() {
		this.finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);
		latitude = (TextView) findViewById(R.id.latitude);
		longitude = (TextView) findViewById(R.id.longitude);
		db = new DatabaseHandler(this);

		/*
		 * pd = new ProgressDialog(this); db = new DatabaseHandler(this);
		 */
		/**** Log file **/
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Location Activity", "Start",
				"Oncreate before LocationSender instance", formattedDate));
		db.close();
		loc = new LocationSender(LocationActivity.this);

	}

	@Override
	public void onBackPressed() {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Location Activity", "onBackPressed",
				"before finish", formattedDate));
		db.close();
		LocationSender.test = "uncheck";
		LocationActivity.this.finish();
		super.onBackPressed();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Location Activity", "OnCreateOPtionsMenyu",
				"OPtion menu created", formattedDate));
		db.close();
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.location, menu);
		return true;
	}

	class ABackGroundTask extends AsyncTask<Void, Void, String> {
		String aresult;

		public ABackGroundTask() {
			super();
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected String doInBackground(Void... params) {

			try {
				c = Calendar.getInstance();
				df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String formattedDate = df.format(c.getTime());
				Log.d("before log", "log created");
				db.addLog(new Logfile("Location Activity", "doInBackground",
						"doIn background", formattedDate));
				db.close();
				Thread.sleep(5000);
				c = Calendar.getInstance();
				df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				formattedDate = df.format(c.getTime());
				Log.d("before log", "log created");
				db.addLog(new Logfile("Location Activity",
						"after Thread.Sleep", "Thread.sleep(5000)",
						formattedDate));
				db.close();
				aresult = postData(totaldata);

			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return aresult;

		}

		@Override
		protected void onPostExecute(String response) {
			c = Calendar.getInstance();
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formattedDate = df.format(c.getTime());
			Log.d("before log", "log created");
			db.addLog(new Logfile("Location Activity", "onPostExecute",
					"OnPostExecute", formattedDate));
			db.close();
			Log.d("Successfully uploaded", "Uploaded log");

		}

	}

	private String postData(String totaldata) {
		c = Calendar.getInstance();
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formattedDate = df.format(c.getTime());
		Log.d("before log", "log created");
		db.addLog(new Logfile("Location Activity", "postData", "posting data",
				formattedDate));
		db.close();
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(
				"http://glass-dev2.azurewebsites.net/LogforGlass.aspx");

		// This is the data to send
		// String MyName = 'adil'; //any data to send

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("Dump", totaldata));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request

			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String response = httpclient.execute(httppost, responseHandler);

			// This is the response from a php application
			reverseString = response;
			Toast.makeText(this, "response" + reverseString, Toast.LENGTH_LONG)
					.show();

		} catch (ClientProtocolException e) {
			c = Calendar.getInstance();
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formattedDate = df.format(c.getTime());
			Log.d("before log", "log created");
			db.addLog(new Logfile("Location Activity",
					"ClientProtocolException", e.getStackTrace().toString(),
					formattedDate));
			db.close();
			Toast.makeText(this, "CPE response " + e.toString(),
					Toast.LENGTH_LONG).show();
			// TODO Auto-generated catch block
		} catch (IOException e) {
			c = Calendar.getInstance();
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formattedDate = df.format(c.getTime());
			Log.d("before log", "log created");
			db.addLog(new Logfile("Location Activity", "IOException", e
					.getStackTrace().toString(), formattedDate));
			db.close();
			Toast.makeText(this, "IOE response " + e.toString(),
					Toast.LENGTH_LONG).show();
			// TODO Auto-generated catch block
		}
		return reverseString;
	}// end postData()

}
