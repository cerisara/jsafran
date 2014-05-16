package utils;

public class Debug {
	public static boolean debug=true;

	public static void print(String s) {
		if (debug) System.out.println(s);
	}
	
	// shall be used in conjunction with debug=false
	// but only temporarilly: mustn't use many of them at the same time !
	public static void printOnly(String s) {
		System.out.println(s);
	}
}
