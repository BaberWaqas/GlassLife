/**
 * 
 */
package cw.glass.utilities;


import android.graphics.Bitmap;

/**
 * @author Eaidikos
 *
 */
public class ImageUtility {

	public static Bitmap ScaleDownBitmap(Bitmap originalImage, float maxImageSize)
    {
        float ratio = Math.min((float)maxImageSize / originalImage.getWidth(), (float)maxImageSize / originalImage.getHeight());
        int width = (int)Math.round(ratio * (float)originalImage.getWidth());
        int height =(int) Math.round(ratio * (float)originalImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(originalImage, width, height, false);
        return newBitmap;
    }
	
	
}
