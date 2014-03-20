package cw.glass.facerecognization;

import android.graphics.Bitmap;

public class Image {
	private Bitmap bmp;

	public Image(Bitmap b) {
		bmp = b;
	}

	public Bitmap getBitmap() {
		return bmp;
	}
}
