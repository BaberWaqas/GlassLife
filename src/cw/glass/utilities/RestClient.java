package cw.glass.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Enumeration;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.Formatter;
import android.util.Log;
//import java.io.ByteArrayInputStream;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.PrintStream;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.entity.FileEntity;
//import org.apache.http.entity.mime.content.FileBody;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.app.AlertDialog.Builder;
//import android.graphics.Bitmap.CompressFormat;

public class RestClient {
	private static final int PHOTO_WIDTH = 1280;
	private static final int PHOTO_HEIGHT = 720;
	public int responseCode = 0;
	public String message;
	public String response;
	byte[] bytes;
	byte[] resultbytes;
	byte[] filebytes;
	File logfile = null;

	public String executeRecognize(String url) throws Exception {
		try {
			HttpGet request = new HttpGet(url);

			request.addHeader("X-Mashape-Authorization",
					"LwJ2KM9eCzl2Yo3biH6E6pIk6D6HWAvX");
			otherwork(request);
			// Log.d("response executeRecognize()", "executeRecognize() "
			// + response);
			return response;
		} catch (Exception ex) {

			ex.printStackTrace();
			throw ex;
		}
	}

	public String DetectLocation(String url) throws Exception {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response;
		String responseString = null;
		try {
			response = httpClient.execute(new HttpGet(url));
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();
				response.getEntity().writeTo(mOutputStream);
				mOutputStream.close();
				responseString = mOutputStream.toString();
			} else {
				response.getEntity().getContent().close();
				throw new Exception(statusLine.getReasonPhrase());
			}
		} catch (Exception e) {
			Log.e("DetectLocation", "Exception " + e.toString());
		}
		return responseString;
	}

	public String executeDetect(String url, byte[] params, String filename)
			throws Exception {
		try {
			HttpPost request = new HttpPost(url);
			request.addHeader("X-Mashape-Authorization",
					"LwJ2KM9eCzl2Yo3biH6E6pIk6D6HWAvX");
			if (params != null) {
				resultbytes = params;
				ByteArrayBody fileinbyte = new ByteArrayBody(resultbytes,
						filename);
				StringBody strSelector = new StringBody("FULL");
				MultipartEntity entity = new MultipartEntity(
						HttpMultipartMode.BROWSER_COMPATIBLE);

				if (resultbytes.length > 0) {
					entity.addPart("selector", strSelector);
					entity.addPart("image", fileinbyte);
				}
				request.setEntity(entity);
			}
			otherwork(request);
			// Log.d("response1", response);
			return response;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						@SuppressWarnings("deprecation")
						String ip = Formatter.formatIpAddress(inetAddress
								.hashCode());
						Log.i("LocalIP", "***** IP=" + ip);
						return ip;
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("LocalIP", ex.toString());
		}
		return null;
	}

	public String uploadPhoto(String url, String filePath,
			String predictedPerson, String location) throws Exception {
		try {
			HttpPost request = new HttpPost(url);

			File file = new File(filePath);
			if (!file.exists())
				Thread.sleep(2000);
			FileBody bin1 = new FileBody(file);

			MultipartEntity entity = new MultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE);
			String localIPAdd = getLocalIpAddress();
			localIPAdd = URLEncoder.encode(localIPAdd, "UTF-8");
			entity.addPart("ID", new StringBody("rhc@cloudwelder.com"));
			entity.addPart("image", bin1);

			entity.addPart("message",
					new StringBody(URLEncoder.encode("@", "UTF-8")
							+ predictedPerson + "at location " + location
							+ " from IP Address " + localIPAdd + " at "
							+ Calendar.getInstance().getTime().toString()));

			entity.addPart("location", new StringBody(location));

			request.setEntity(entity);
			otherwork(request);
			Log.d("response1", response);
			return response;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}

	}

	boolean done = true;

	public void otherwork(HttpUriRequest request) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			response = "";
			httpResponse = client.execute(request);
			responseCode = httpResponse.getStatusLine().getStatusCode();
			message = httpResponse.getStatusLine().getReasonPhrase();
			HttpEntity entity = httpResponse.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				response = convertStreamToString(instream, logfile);
				instream.close();
			}
			done = false;
		} catch (Exception e) {
			throw e;
		}
	}

	byte[] resizeImage(byte[] input) throws Exception {
		ByteArrayOutputStream blob = null;
		try {
			Bitmap original = BitmapFactory.decodeByteArray(input, 0,
					input.length);
			Bitmap resized = Bitmap.createScaledBitmap(original, PHOTO_WIDTH,
					PHOTO_HEIGHT, true);

			blob = new ByteArrayOutputStream();
			resized.compress(Bitmap.CompressFormat.JPEG, 75, blob);
		} catch (Exception e) {
			throw e;
		}
		return blob.toByteArray();
	}

	private static String convertStreamToString(InputStream is, File errorFile) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}
