package utils.installer;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;

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
			if (jars!=null)
				for (File jar : jars)
					Utils4J.addToClasspath(jar.toURI().toURL());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final String[] args0={};
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
			// fix executable for tagger on linux
			final HashSet<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(PosixFilePermission.GROUP_EXECUTE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
			perms.add(PosixFilePermission.OTHERS_READ);
			java.nio.file.Files.setPosixFilePermissions(FileSystems.getDefault().getPath("tagger/bin", "tree-tagger"), perms);
		} catch (Exception e) {
			System.err.println("can't download resources "+e.getMessage());
		}
	}
}
