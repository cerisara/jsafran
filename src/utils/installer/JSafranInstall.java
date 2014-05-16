package utils.installer;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.fuin.utils4j.Utils4J;

import utils.Installer;


public class JSafranInstall {
	
	final static String baseurl = "http://talc1.loria.fr/users/cerisara/jsafran/";
	
	public static void main(String args[]) {
		System.out.println("starting installer...");
		File flibs = new File("lib");
		if (!flibs.exists()) downloadApp();
		
		runApp(args);
	}
	
	private static void runApp(String args[]) {
		File d = new File("lib");
		File[] jars = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".jar");
			}
		});
		try {
			for (File jar : jars)
				Utils4J.addToClasspath(jar.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		final String[] args0={"test2009.xml"};
		if (args==null||args.length==0) args=args0;
		Installer.launchJSafran(args);
	}
	
	private static void downloadApp() {
		File dir = new File(".");
		System.out.println("Downloading jars in dir "+dir);
		try {
			WGETJava.DownloadFile(new URL(baseurl+"counts.php"));
			WGETJava.DownloadFile(new URL(baseurl+"res.jar"));
			// res.jar doit contenir tous les jars dans libs/*.jar et toutes les autres resources
			File zipfile = new File("res.jar");
			Utils4J.unzip(zipfile, dir);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
