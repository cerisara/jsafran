package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Wait {
	public static void waitUser() {
		BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
		try {
			f.readLine();
		} catch (IOException e) {
		}
	}
}
