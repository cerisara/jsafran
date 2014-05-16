package utils;

import java.io.File;
import java.net.URL;

import javax.swing.JOptionPane;

import jsafran.JSafran;


public class Installer {
	
	public static void main(String args[]) {
		launchJSafran(args);
	}
	
	public static void launchJSafran(String args[]) {
		if (!isAlreadyInstalled()) install();
		try {
			JSafran.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void install() {
		int rep = JOptionPane.showConfirmDialog(null, "First time run detected. OK for downloading the models ?");
		if (rep==JOptionPane.OK_OPTION) {
			try {
				URL resurl = new URL("http://talc1.loria.fr/users/cerisara/jsafran/jsafranres.zip");
				WGETJava.DownloadFile(resurl);
				FileUtils.unzip("jsafranres.zip");
				{
					// change permissions to execute
					File f = new File("tagger/bin/");
					File[] fs = f.listFiles();
					for (File ff : fs) {
						ff.setExecutable(true);
					}
				}
				
				
				System.out.println("installation successful");
				File f = new File("jsafranres.zip");
				f.delete();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "error installing: "+e.toString());
			}
		}
	}
	public static boolean isAlreadyInstalled() {
		File f = new File("./tagger");
		if (!f.exists()) return false;
		return true;
	}
}
