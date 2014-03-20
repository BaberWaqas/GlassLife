package cw.glass.utilities;

public class Logfile {
	
	//private variables
	int _id;
	String _tag;
	String _message;
	String _stack;
	String _time;
	// Empty constructor
	public Logfile(){
		
	}
	// constructor
	public Logfile(int id,String tag, String message, String stack,String time){
		this._id = id;
		this._tag = tag;
		this._message = message;
		this._stack = stack;
		this._time = time;
		}
	
	// constructor
	public Logfile(String tag, String message,String stack,String time){
		this._tag = tag;
		this._message = message;
		this._stack = stack;
		this._time = time;
		
	}
	// get ID
		public int getId(){
			return this._id;
		}
		
		// setting phone number
		public void setId(int id){
			this._id = id;
		}
	// getting ID
	public String gettag(){
		return this._tag;
	}
	
	// setting id
	public void settag(String tag){
		this._tag = tag;
	}
	
	// getting name
	public String getMesage(){
		return this._message;
	}
	
	// setting name
	public void setMessage(String message){
		this._message = message;
	}
	
	// getting phone number
	public String getStackTrace(){
		return this._stack;
	}
	
	// setting phone number
	public void setStackTrace(String stack){
		this._stack = stack;
	}
	public String getTime(){
		return this._time;
	}
	
	// setting phone number
	public void setTime(String time){
		this._time = time;
	}
	
	
}
