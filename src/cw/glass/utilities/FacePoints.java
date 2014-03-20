package cw.glass.utilities;

import android.os.Parcel;
import android.os.Parcelable;

public class FacePoints implements Parcelable {

	String rightEyeBrowLeftX, rightEyeBrowLeftY, rightEyeBrowRightX,
			rightEyeBrowRightY, imageID, width, height, lipLineX, lipLineY,
			rightNostrilY, leftNostrilY, topLeftX, topLeftY, predictedPerson;

	public FacePoints(Parcel source) {
		rightEyeBrowLeftX = source.readString();
		rightEyeBrowLeftY = source.readString();
		rightEyeBrowRightX = source.readString();
		rightEyeBrowRightY = source.readString();
		imageID = source.readString();
		width = source.readString();
		height = source.readString();
		lipLineX = source.readString();
		lipLineY = source.readString();
		rightNostrilY = source.readString();
		leftNostrilY = source.readString();
		topLeftX = source.readString();
		topLeftY = source.readString();
		predictedPerson = source.readString();
	}

	public FacePoints() {
	}

	public String getPredictedPerson() {
		return predictedPerson;
	}

	public void setPredictedPerson(String predictedPerson) {
		this.predictedPerson = predictedPerson;
	}

	public String getRightEyeBrowLeftX() {
		return rightEyeBrowLeftX;
	}

	public void setRightEyeBrowLeftX(String rightEyeBrowLeftX) {
		this.rightEyeBrowLeftX = rightEyeBrowLeftX;
	}

	public String getRightEyeBrowLeftY() {
		return rightEyeBrowLeftY;
	}

	public void setRightEyeBrowLeftY(String rightEyeBrowLeftY) {
		this.rightEyeBrowLeftY = rightEyeBrowLeftY;
	}

	public String getRightEyeBrowRightX() {
		return rightEyeBrowRightX;
	}

	public void setRightEyeBrowRightX(String rightEyeBrowRightX) {
		this.rightEyeBrowRightX = rightEyeBrowRightX;
	}

	public String getRightEyeBrowRightY() {
		return rightEyeBrowRightY;
	}

	public void setRightEyeBrowRightY(String rightEyeBrowRightY) {
		this.rightEyeBrowRightY = rightEyeBrowRightY;
	}

	public String getImageID() {
		return imageID;
	}

	public void setImageID(String imageID) {
		this.imageID = imageID;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getLipLineX() {
		return lipLineX;
	}

	public void setLipLineX(String lipLineX) {
		this.lipLineX = lipLineX;
	}

	public String getLipLineY() {
		return lipLineY;
	}

	public void setLipLineY(String lipLineY) {
		this.lipLineY = lipLineY;
	}

	public String getRightNostrilY() {
		return rightNostrilY;
	}

	public void setRightNostrilY(String rightNostrilY) {
		this.rightNostrilY = rightNostrilY;
	}

	public String getLeftNostrilY() {
		return leftNostrilY;
	}

	public void setLeftNostrilY(String leftNostrilY) {
		this.leftNostrilY = leftNostrilY;
	}

	public String getTopLeftX() {
		return topLeftX;
	}

	public void setTopLeftX(String topLeftX) {
		this.topLeftX = topLeftX;
	}

	public String getTopLeftY() {
		return topLeftY;
	}

	public void setTopLeftY(String topLeftY) {
		this.topLeftY = topLeftY;
	}

	public int describeContents() {
		return this.hashCode();
	}

	public void writeToParcel(Parcel dest, int flags) {

		dest.writeString(rightEyeBrowLeftX);
		dest.writeString(rightEyeBrowLeftY);
		dest.writeString(rightEyeBrowRightX);
		dest.writeString(rightEyeBrowRightY);
		dest.writeString(imageID);
		dest.writeString(width);
		dest.writeString(height);
		dest.writeString(lipLineX);
		dest.writeString(lipLineY);
		dest.writeString(rightNostrilY);
		dest.writeString(leftNostrilY);
		dest.writeString(topLeftX);
		dest.writeString(topLeftY);
		dest.writeString(predictedPerson);
	}

	public static final Parcelable.Creator<FacePoints> CREATOR = new Parcelable.Creator<FacePoints>() {

		@Override
		public FacePoints[] newArray(int size) {
			return new FacePoints[size];
		}

		@Override
		public FacePoints createFromParcel(Parcel source) {
			return new FacePoints(source);
		}
	};

}
