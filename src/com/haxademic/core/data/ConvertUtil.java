package com.haxademic.core.data;

public class ConvertUtil {
	
	// from string

	public static int stringToInt( String str ) {
		return Integer.parseInt( str );
	}
	
	public static float stringToFloat( String str ) {
		return Float.valueOf( str );
	}
	
	public static boolean stringToBoolean( String bool ) {
		return (bool.equals("true")) ? true : false;
	}
	
	// to string

	public static String intToString( int number ) {
		return Integer.toString( number );
	}
	
	public static String floatToString( float number ) {
		return Float.toString( number );
	}
}



