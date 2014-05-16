package corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import plugins.utils.FileUtils;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.MateParser;
import jsafran.Mot;
import jsafran.POStagger;

/**
 * Acces aux fichiers TRS du corpus ESTER2 (1.3 millions de mots)
 * 
 * @author xtof
 *
 */
public class Ester2 {
	final String corpdir = "/home/xtof/corpus/ESTER2ftp/trs_train_EN_v1.1";
	File[] fichs;
	int curfich=-1;
	
	public Ester2() {
		File d = new File(corpdir);
		fichs = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".trs")) return true;
				return false;
			}
		});
		curfich=0;
	}
	
	private void addgraph(List<DetGraph> gs, StringBuilder sb) {
		String s=sb.toString().trim();
		if (s.length()==0) return;
		s=s.replaceAll("'", "' ");
		s=s.replaceAll("jourd' +hui", "jourd'hui");
		String[] st = s.split(" ");
		DetGraph g = new DetGraph();
		for (int i=0,j=0;i<st.length;i++) {
			String w=st[i].trim();
			w=w.replace('^', ' ');
			w=w.replace('*', ' ');
			w=w.replace('(', ' ');
			w=w.replace(')', ' ');
			w=w.trim();
			if (w.length()==0) continue;
			Mot m = new Mot(w, w, "_");
			g.addMot(j++, m);
		}
		gs.add(g);
	}
	
	private File[] parsedxml = null;
	private int parsedcur=0;
	/**
	 * this function can only be called AFTER the create() method
	 * @return
	 */
	public List<DetGraph> getNextFileParsed() {
		if (parsedxml==null) {
			parsedcur=0;
			File d = new File("ester2");
			parsedxml = d.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					if (arg1.endsWith(".xml")) return true;
					return false;
				}
			});
		}
		GraphIO gio = new GraphIO(null);
		if (parsedcur>=parsedxml.length) return null;
		List<DetGraph> gs = gio.loadAllGraphs(parsedxml[parsedcur++].getAbsolutePath());
		return gs;
	}
	
	public List<DetGraph> getNextFile() {
		if (curfich>=fichs.length) return null;
		System.out.println("loading "+fichs[curfich].getAbsolutePath());
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		try {
			BufferedReader f = FileUtils.openFileISO(fichs[curfich++].getAbsolutePath());
			StringBuilder utt = new StringBuilder();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) continue;
				if (s.charAt(0)=='<') {
					if (s.startsWith("</Turn>")) {
						addgraph(res,utt);
						utt=new StringBuilder();
					}
					// TODO: parse entities, turns...
					continue;
				}
				// look for strong punctuations
				int i=0,endPrevUtt=0;
				for (;;) {
					int j=s.indexOf(' ',i);
					if (j<0||j>=s.length()-1) break;
					char p=s.charAt(++j);
					if (p=='.'||p=='!'||p=='?') {
						utt.append(s.substring(endPrevUtt, ++j));
						addgraph(res,utt);
						i=endPrevUtt=j;
						utt = new StringBuilder();
					} else {
						i=j;
					}
				}
				utt.append(s.substring(endPrevUtt));
				utt.append(' ');
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	public static void create() {
		Ester2 m = new Ester2();
		GraphIO gio = new GraphIO(null);
		for (;;) {
			List<DetGraph> gs = m.getNextFile();
			if (gs==null) break;

			//			JSafran.viewGraph(gs);
			for (DetGraph g : gs)
				POStagger.tag(g);
			
			if (true) {
				// to parse the graphs
				
				MateParser mate = MateParser.getMateParser(null);
				ArrayList<DetGraph> gsav = new ArrayList<DetGraph>();
				for (DetGraph g : gs) {
					DetGraph gx = g.clone();
					gsav.add(gx);
					JSafran.delponct(g);
					DetGraph gparsed = mate.parseMateOneUtt(g);
					JSafran.projectOnto(gparsed, gx, true, false, false);
				}
				
				gs=gsav;
			}
			
			String nom="ester2/"+m.fichs[m.curfich].getName()+".xml";
			System.out.println("save "+nom);
			gio.save(gs, nom);
		}
	}

	public static void main(String args[]) {
		create();
	}
}
