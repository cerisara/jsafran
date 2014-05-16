package jsafran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jsafran.alltaggers.OpenNLPTagger;

import org.annolab.tt4j.ModelResolver;
import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerWrapper;

import corpus.BDLex;
import corpus.DicoEntry;
import corpus.DicoEntry.POStag;

public class POStagger {

	static public boolean useOpenNLP = false;
	
	static private POStagger taggerInstance = null;
	
	DetGraph g;
	static TreeTaggerWrapper<String> treeTagger = null;
	int tokpos;

	BDLex lexik = null;
	private String homett = null;
	
	private POStagger() {}
	private POStagger(String hometreetagger) {
		homett = hometreetagger;
	}
	
	jsafran.alltaggers.OpenNLPTagger opennlptagger = null;
	private void computePOStagsOpenNLP(final DetGraph g) {
		if (opennlptagger==null) opennlptagger = new OpenNLPTagger();
		ArrayList<String> mots = new ArrayList<String>();
		for (int i=0;i<g.getNbMots();i++)
			mots.add(g.getMot(i).getForme());
		List<String> pos = opennlptagger.getPostags(mots);
		for (int i=0;i<g.getNbMots();i++)
			g.getMot(i).setPOS(pos.get(i));
		// trouver les lemmes avec BDLex
		if (lexik==null) lexik = BDLex.getBDLex();
		for (int i=0;i<g.getNbMots();i++) {
			Mot m = g.getMot(i);
			List<DicoEntry> dicos = lexik.getDicoEntries(m.getForme());
			HashMap<String, String> lemme2pos = new HashMap<String, String>();
			for (DicoEntry e : dicos) {
				char posDuDico = 'U';
				if (e.getPOStag()==POStag.adj) posDuDico='A';
				else if (e.getPOStag()==POStag.nom) posDuDico='N';
				else if (e.getPOStag()==POStag.det) posDuDico='D';
				else if (e.getPOStag()==POStag.verb) posDuDico='V';
				String lem = e.getLemme();
				String post = lemme2pos.get(lem);
				if (post==null) {
					post = ""+posDuDico;
					lemme2pos.put(lem, post);
				} else {
					if (post.indexOf(posDuDico)<0)
						lemme2pos.put(lem,post+posDuDico);
				}
			}
			// y a-t-il plusieurs lemmes possibles ?
			if (lemme2pos.size()==0) {
				// mot inconnu
				m.setlemme(m.getForme());
			} else if (lemme2pos.size()==1) {
				// pas de confusion possible
				m.setlemme(lemme2pos.keySet().iterator().next());
				// TODO: check le postag, eventuellement corrige le postag !
			} else {
				// confusion possible !
				// desambiguise sur le postag:
				char posDuTagger = 'U';
				if (m.getPOS().equals("NPP")) {
					// nom propre:
					m.setlemme(m.getForme());
					continue;
				} else if (m.getPOS().equals("V")) {posDuTagger='V';
				} else if (m.getPOS().equals("DET")) {posDuTagger='D';
				} else if (m.getPOS().equals("NC")) {posDuTagger='N';
				} else if (m.getPOS().equals("ADJ")) {posDuTagger='A';
				}
				System.out.println("find lemma: "+posDuTagger+" "+m.getForme());
				ArrayList<String> lemmesPossibles = new ArrayList<String>();
				for (String lem : lemme2pos.keySet()) {
					String postagsDuLemme = lemme2pos.get(lem);
					if (postagsDuLemme.indexOf(posDuTagger)>=0) lemmesPossibles.add(lem);
				}
				if (lemmesPossibles.size()==0) {
					System.out.println("WARNING: aucun lemme avec le postag "+posDuTagger+" "+m.getForme());
					// TODO: pour le moment, on prend le 1er pos qui vient !
					m.setlemme(lemme2pos.keySet().iterator().next());
				} else if (lemmesPossibles.size()>1) {
					System.out.println("WARNING: plusieurs lemmes possibles avec ce pos: "+posDuTagger+" "+Arrays.toString(lemmesPossibles.toArray()));
					m.setlemme(lemmesPossibles.get(0));
				} else
					m.setlemme(lemmesPossibles.get(0));
			}
		}
	}
	
	private final static String frenchModels = "/lib/french-par-linux-3.2-utf8.bin";
	private final static String englishModels = "/lib/english-par-linux-3.2.bin";
	private static String models = frenchModels;
	private static boolean reloadModels = true;
	
	public static void setEnglishModels() {
		if (models==frenchModels) {
			reloadModels=true;
			models = englishModels;
		}
	}

	public static void setFrenchModels() {
		if (models==englishModels) {
			reloadModels=true;
			models = frenchModels;
		}
	}

	private void computePOStagsTreeTagger(final DetGraph g) {
		if (treeTagger==null) {
			treeTagger = new TreeTaggerWrapper<String>();		
			setupTreeTagger();
		}
		if (reloadModels) {
			try {
				treeTagger.setModel(System.getProperty("treetagger.home")+models);
				reloadModels=false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		treeTagger.setHandler(new TokenHandler<String>() {
			public void token(String token, String pos, String lemma) {
				if (!g.getMot(tokpos).getForme().equals(token))
					System.err.println("WARNING TREETAGGER diff tt="+token+" g="+g.getMot(tokpos));
				g.getMot(tokpos).setlemme(lemma);
				g.getMot(tokpos).setPOS(pos);
				tokpos++;
			}
		});
		tokpos=0;
		try {
			String[] phr=g.getMots();
			treeTagger.process(Arrays.asList(phr));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean setupTreeTagger() {
		if (homett!=null) {
			System.setProperty("treetagger.home", homett);
			return true;
		}
		boolean alreadyTested=false;
		boolean treetaggerset=false;
		if (alreadyTested) return treetaggerset;
		alreadyTested=true;
		File f = new File("jsynats.cfg");
		System.out.println("found jsynats.cfg in local dir");
		if (!f.exists()) {
			// teste dans la HOME
			System.out.println("found jsynats.cfg in HOME ");
			f = new File(System.getenv("HOME")+"/jsynats.cfg");
			if (!f.exists()) {
				System.err.println("WARNING treetagger: impossible de trouver jsynats.cfg !");
				treetaggerset=false; return false;
			}
		}
		try {
			BufferedReader ff = new BufferedReader(new FileReader(f));
			for (;;) {
				String s = ff.readLine();
				if (s==null) break;
				if (s.startsWith("TREETAGGERDIR")) {
					int i = s.indexOf(' ');
					System.setProperty("treetagger.home", s.substring(i+1));
					treetaggerset=true;
				}
			}
			ff.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return treetaggerset;
	}

//	final static POSTagging postagging = new POSTagging();
	
	public static void tag(DetGraph g) {
		if (taggerInstance==null) taggerInstance=new POStagger();
		if (useOpenNLP)
			taggerInstance.computePOStagsOpenNLP(g);
		else
			taggerInstance.computePOStagsTreeTagger(g);
	}
	public static void tag(String hometreetagger, DetGraph g) {
		if (taggerInstance==null) taggerInstance=new POStagger(hometreetagger);
		if (useOpenNLP)
			taggerInstance.computePOStagsOpenNLP(g);
		else
			taggerInstance.computePOStagsTreeTagger(g);
	}
	
	// ********************************** tests sur les POStags
	public static boolean areEquivalent(String a, String b) {
		return isVerb(a)&&isVerb(b) || isNoun(a)&&isNoun(b) || isAdj(a)&&isAdj(b) || isPrep(a)&&isPrep(b)
				|| isDet(a)&&isDet(b);
	}
	public static boolean isVerb(String pos) {
		if (pos.toLowerCase().startsWith("ver") || pos.equals("V")) return true;
		else return false;
	}
	public static boolean isVerb(DetGraph g, int mot) {
		return isVerb(g.getMot(mot).getPOS());
	}
	public static boolean isNoun(String pos) {
		if (pos.toLowerCase().startsWith("nom") || pos.equals("N")) return true;
		else return false;
	}
	public static boolean isNoun(DetGraph g, int mot) {
		return isNoun(g.getMot(mot).getPOS());
	}
	public static boolean isAdj(String pos) {
		if (pos.toLowerCase().startsWith("adj")) return true;
		else return false;
	}
	public static boolean isAdv(String pos) {
		if (pos.toLowerCase().startsWith("adv")) return true;
		else return false;
	}
	public static boolean isAdj(DetGraph g, int mot) {
		return isAdj(g.getMot(mot).getPOS());
	}
	public static boolean isPrep(String pos) {
		String p = pos.toLowerCase();
		if (p.startsWith("prp")||p.startsWith("prep")||p.equals("p")||p.equals("p+d")) return true;
		else return false;
	}
	public static boolean isPrep(DetGraph g, int mot) {
		return isPrep(g.getMot(mot).getPOS());
	}
	public static boolean isDet(String pos) {
		String p = pos.toLowerCase();
		if (p.startsWith("det")||p.endsWith("det")) return true;
		else return false;
	}
	public static boolean isDet(DetGraph g, int mot) {
		return isDet(g.getMot(mot).getPOS());
	}

	public static boolean isPonct(String pos) {
		if (pos.toLowerCase().startsWith("sent") ||
				pos.toLowerCase().startsWith("pun") ||
				pos.toLowerCase().startsWith("ponct") ||
				// pour le Czech Tree Bank
				pos.equals("Z")
		) return true;
		else return false;
	}

	public static void main(String args[]) {
		
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(args[0]);
		for (DetGraph g : gs) {
			tag(g);
		}
		gio.save(gs, "output.xml");
		
//		if (args.length==0||args[0].endsWith("-help")||args[0].startsWith("-h")) {
//			System.out.println("usage: java jsynats.POStagger fichier_a_tagger");
//			return;
//		}
//		SyntaxGraphs m = new SyntaxGraphs(new GraphProcessor() {
//			PrintWriter fout = GraphIO.saveEntete("output.xml");
//			public void terminate() {fout.close();}
//			public void processGraph(DetGraph g) {
//				postagger.computePOStags(g);
//				g.save(fout);
//			}
//		});
//		m.parse(args[0]);
	}
}
