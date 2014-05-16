package corpus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.Mot;

public class MultiMots {
	ArrayList<String> lemme = new ArrayList<String>();
	ArrayList<String> pos = new ArrayList<String>();
	HashMap<String, Integer> form2idx = new HashMap<String, Integer>();
	
	HashMap<String, String> posConversion = new HashMap<String, String>();

	// ET = mot etranger
	// I  = interjection : peut se traduire par n'importe quoi, mais ce sont alors des erreurs d'analyse !
	// TODO: il y a 2 solutions pour DET: DET:ART et DET:POS ?
	// il y a plusieurs pronoms possibles !
	// il y a de nombreux verbes possibles !
	// il y a plusieurs subjonctifs
	final String[] P7pos = {"ADJ","ADV","CC", "CLS",    "CS", "DET",    "ET", "I",  "NC", "NPP","P",  "PRO","PROREL", "V",       "VINF",    "VPP",     "VPR",     "VS"};
	final String[] TTPOS = {"ADJ","ADV","KON","PRO:PER","KON","DET:ART","NAM","INT","NOM","NAM","PRP","PRO","PRO:REL","VER:pres","VER:infi","VER:pper","VER:ppre","VER:subi"};
	
	public MultiMots() {
		for (int i=0;i<P7pos.length;i++) {
			posConversion.put(P7pos[i], TTPOS[i]);
		}
	}
	
	public void loadCfg(String cfgfile) {
		try {
//			BufferedReader f = new BufferedReader(new FileReader(cfgfile));
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile),Charset.forName("UTF-8")));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("MULTIMOTS ")) {
					String[] ss = s.split(" ");
					form2idx.put(ss[1], lemme.size());
					lemme.add(ss[2]);
					pos.add(ss[3]);
				}
			}
			f.close();
			System.out.println("loaded "+lemme.size()+" multimots");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void extractAppend(List<DetGraph> graphs) {
		for (int i=0;i<graphs.size();i++) {
			DetGraph g = graphs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				String form = g.getMot(j).getForme();
				if (form.indexOf('_')>=0 && !form2idx.keySet().contains(form)) {
					// muti-mots
					// remarque: je ne verifie pas si la meme forme peut avoir plusieurs pos !!
					form2idx.put(form, lemme.size());
					pos.add(g.getMot(j).getPOS());
					lemme.add(g.getMot(j).getLemme());
				}
			}
		}
	}
	
	public void printMultiMots() {
		for (String form: form2idx.keySet()) {
			int i=form2idx.get(form);
			System.out.println("MULTIMOT "+form+" "+lemme.get(i)+" "+pos.get(i));
		}
	}
	
	// il faudrait une conversion plus intelligente qui soit fonction du contexte...
	private void convertPOS() {
		for (String form: form2idx.keySet()) {
			int i=form2idx.get(form);
			String posp7 = pos.get(i);
			String postt = posConversion.get(posp7);
			if (postt==null) {
				System.out.println("ERREUR conversion pos "+posp7);
			} else {
				pos.set(i, postt);
			}
		}
	}
	
	public List<Integer> reformeMultiMots(DetGraph g) {
		ArrayList<Integer> mergingsDone = new ArrayList<Integer>();
		for (int j=0;j<g.getNbMots();j++) {
			for (String form: form2idx.keySet()) {
				String[] ss = form.split("_");
				if (g.getNbMots()-j<ss.length) continue;
				int k=0;
				for (;k<ss.length;k++) {
					if (!ss[k].equals(g.getMot(j+k).getForme())) break;
				}
				if (k>=ss.length) {
					System.out.println("trouve multimots "+form);
					// on a trouve un multi-mots !
					mergingsDone.add(j);
					mergingsDone.add(ss.length);
					g.mergeNWords(j, ss.length, false,'_');
					int idx = form2idx.get(form);
					g.getMot(j).setlemme(lemme.get(idx));
					g.getMot(j).setPOS(pos.get(idx));
				}
			}
		}
		return mergingsDone;
	}
	
	public void splitMultimots(DetGraph g) {
		for (int i=g.getNbMots()-1;i>=0;i--) {
			String[] ss = g.getMot(i).getForme().split("_");
			if (ss.length>1) {
				g.getMot(i).setForme(ss[0]);
				for (int j=1;j<ss.length;j++) {
					Mot m = new Mot(ss[j],ss[j],"unk");
					g.insertMot(i+j, m);
				}
			}
		}
	}
	
	public static void main(String args[]) {
		MultiMots m = new MultiMots();
		
		if (args[0].equals("-extract")) {
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(args[1]);
			m.extractAppend(gs);
			m.convertPOS();
			m.printMultiMots();
		} else if (args[0].equals("-parse")) {
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(args[1]);
			m.loadCfg("jsynats.cfg");
			for (int i=0;i<gs.size();i++)
				m.reformeMultiMots(gs.get(i));
			gio.save(gs, "output.xml");
		}
	}
}
