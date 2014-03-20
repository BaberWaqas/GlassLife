package cw.glass.utilities;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 3;

	// Database Name
	private static final String DATABASE_NAME = "logcat";

	// Contacts table name
	private static final String TABLE_CONTACTS = "log";

	// Contacts Table Columns names
	private static final String KEY_ID = "_id";
	private static final String KEY_TAG = "tag";
	private static final String KEY_MESSAGE = "message";
	private static final String KEY_STACKTRACE = "stacktrace";
	private static final String KEY_TIME = "time";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "(" + KEY_ID
				+ " INTEGER PRIMARY KEY," + KEY_TAG + " TEXT," + KEY_MESSAGE
				+ " TEXT," + KEY_STACKTRACE + " TEXT," + KEY_TIME + " TEXT"
				+ ")";
		db.execSQL(CREATE_TABLE);
		Log.d("dbcreated", "dbcreated");
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	// Adding new contact

	// Getting single contact
	/*
	 * Contact getContact(int id) { SQLiteDatabase db =
	 * this.getReadableDatabase();
	 * 
	 * Cursor cursor = db.query(TABLE_CONTACTS, new String[] { KEY_ID, KEY_NAME,
	 * KEY_PH_NO }, KEY_ID + "=?", new String[] { String.valueOf(id) }, null,
	 * null, null, null); if (cursor != null) cursor.moveToFirst();
	 * 
	 * Contact contact = new Contact(Integer.parseInt(cursor.getString(0)),
	 * cursor.getString(1), cursor.getString(2)); // return contact return
	 * contact; }
	 */
	// Getting All Contacts
	public List<Logfile> getAllRecords() {
		List<Logfile> contactList = new ArrayList<Logfile>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Logfile contact = new Logfile();
				contact.settag(cursor.getString(1));
				contact.setMessage(cursor.getString(2));
				contact.setStackTrace(cursor.getString(3));
				contact.setTime(cursor.getString(4));

				// Adding contact to list
				contactList.add(contact);
			} while (cursor.moveToNext());
		}

		// return contact list
		return contactList;
	}

	public void addLog(Logfile contact) {

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_TAG, contact.gettag()); // tag
		values.put(KEY_MESSAGE, contact.getMesage()); // Messsage
		values.put(KEY_STACKTRACE, contact.getStackTrace()); // stack
		values.put(KEY_TIME, contact.getTime()); // time

		db.insert(TABLE_CONTACTS, null, values);
		db.close(); // Closing database connection

	}

	// Updating single contact
	/*
	 * public int updateContact(Contact contact) { SQLiteDatabase db =
	 * this.getWritableDatabase();
	 * 
	 * ContentValues values = new ContentValues(); values.put(KEY_NAME,
	 * contact.getName()); values.put(KEY_PH_NO, contact.getPhoneNumber());
	 * 
	 * // updating row return db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
	 * new String[] { String.valueOf(contact.getID()) }); }
	 */
	// Deleting single contact
	/*
	 * public void deleteContact(Contact contact) { SQLiteDatabase db =
	 * this.getWritableDatabase(); db.delete(TABLE_CONTACTS, KEY_ID + " = ?",
	 * new String[] { String.valueOf(contact.getID()) }); db.close(); }
	 */

	// Getting contacts Count
	/*
	 * public int getContactsCount() { String countQuery = "SELECT  * FROM " +
	 * TABLE_CONTACTS; SQLiteDatabase db = this.getReadableDatabase(); Cursor
	 * cursor = db.rawQuery(countQuery, null); cursor.close();
	 * 
	 * // return count return cursor.getCount(); }
	 */
}
