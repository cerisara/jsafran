package jsafran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;


/**
 * 
 * Charge des graphes syntaxiques au format: TXT, CONLL, Syntex-XML
 * 
 * @author cerisara
 *
 */
public class SyntaxGraphs {
	
	public final static int CONLL = 0;
	public static int poscolumn = 3;
	static public SyntaxGraphs sg;
	public static String forceEncoding = null;
	
	// graphe courant
	DetGraph detgraph;
	GraphProcessor phraseProcessor = null;

	HashMap<String, Integer> group2head;

	ArrayList<Integer> debdep = new ArrayList<Integer>();
	ArrayList<Integer> findep = new ArrayList<Integer>();
	ArrayList<String> labdep = new ArrayList<String>();

	int cursent=-1, firstsent=0;

	public SyntaxGraphs(GraphProcessor proc) {
		phraseProcessor=proc;
		sg=this;
	}

	private void finish1phrase() {
		// cree les liens
		for (int i=0;i<debdep.size();i++) {
			int mGoverned = debdep.get(i);
			int head = findep.get(i);
			String lab = labdep.get(i);
			detgraph.ajoutDep(lab, mGoverned-1, head-1);
		}
		// appelle le phrase Processor
		//		System.err.println("fin de phrase "+detgraph.getNbMots());
		if (++cursent>=firstsent)
			processGraph(detgraph);

		debdep.clear(); findep.clear(); labdep.clear();
	}

	public void parseList(String fichlist) {
		try {
			BufferedReader f = new BufferedReader(new FileReader(fichlist));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()>0) parseGuessType(s,-1);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		phraseProcessor.terminate();
	}
	public void parse(BufferedReader fich, int fichType) {
		switch(fichType) {
		case CONLL: parseCONLL(fich);
			break;
		default:
			System.err.println("ERROR PARSE NOT SUPPORTED YET !");
		}
		phraseProcessor.terminate();
	}
	public void parse(String fich) {
		parseGuessType(fich,-1);
//                phraseProcessor.processGraph(detgraph);
		phraseProcessor.terminate();
	}
	public void parse(String fich, int n2skip) {
		firstsent=n2skip;
		parseGuessType(fich,-1);
		phraseProcessor.terminate();
	}
	public void parse(String fich, int n2skip, int nmax) {
		firstsent=n2skip;
		parseGuessType(fich,nmax);
		phraseProcessor.terminate();
	}

	private void parseGuessType(String fich, int nmax) {
		if (fich.endsWith(".conll")) parseCONLL(fich);
		else if (fich.endsWith(".xml")) parseSyntexXML(fich,nmax);
		else if (fich.endsWith(".xml.gz")) parseSyntexXML(fich,nmax);
		else if (fich.endsWith(".txt")) parseTxt(fich);
	}

	public static DetGraph tokenizeSentence(String sent) {
		DetGraph res = new DetGraph();
		StringTokenizer st = new StringTokenizer(sent);
		int i=0;
		while (st.hasMoreTokens()) {
			String forme = st.nextToken();
			
			// separe la ponctuation
			StringTokenizer st2 = new StringTokenizer(forme,",;:.?!\"",true);
			while (st2.hasMoreTokens()) {
				String forme2 = st2.nextToken();

				StringTokenizer st3 = new StringTokenizer(forme2,"’'",true);
				while (st3.hasMoreTokens()) {
					String forme3 = st3.nextToken();
					if (st3.hasMoreTokens()) {
						// supprime l'apostrophe
						st3.nextToken();
						forme3+="'";
					}
					Mot mot = new Mot(forme3,forme3,"_");
					res.addMot(i, mot);
					i++;
				}
			}
		}
		for (i=res.getNbMots()-2;i>=0;i--) {
			if (res.getMot(i).getForme().toLowerCase().equals("aujourd'") &&
				res.getMot(i+1).getForme().toLowerCase().equals("hui")) {
				res.mergeNWords(i, 2, false, ' ');
				res.getMot(i).setForme("aujourd'hui");
			}
		}
		return res;
	}
	
	/**
	 * @deprecated
	 * @param txtfile
	 */
	private void parseTxt(String txtfile) {
		System.err.println("warning: loading texte with the old text parser !");
		try {
			String enc = "UTF-8";
			if (forceEncoding!=null) enc=forceEncoding;
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(txtfile),enc));
			int cursent=0;
			File ff = new File(txtfile);
			URL srcurl = ff.toURI().toURL();
			long pos=0;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.trim().length()==0) continue;
				detgraph = tokenizeSentence(s);
				detgraph.setSource(srcurl);
				detgraph.cursent = ++cursent;
				detgraph.sent=s;
				processGraph(detgraph);
				pos+=s.length();
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * il y a un bug dans cette fonction: des graphes sont "zappes"
	 * @param conllfile
	 */
	public void parseCONLL(BufferedReader f) {
		try {
			int cursent=0;
			detgraph = new DetGraph();
			detgraph.cursent = 0;
			detgraph.sent="";
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] ss = s.split("\t");
				if (ss.length==10) {
					int index=Integer.parseInt(ss[0]);
					if (index==1) {
						// debut de phrase
						if (cursent>0) {
							// il faut creer les liens de la phrase precedente puis appeler le GraphProcessor
							finish1phrase();
						}
						detgraph = new DetGraph();
						detgraph.cursent = ++cursent;
						detgraph.sent="";
					}
					detgraph.sent+=ss[1]+" ";
					Mot m= new Mot(ss[1],ss[2],ss[poscolumn]);
					detgraph.addMot(index-1, m);

					if (ss[6].charAt(0)!='_') {
						int head = Integer.parseInt(ss[6]);
						if (head>0) {
							debdep.add(m.getIndexInUtt());
							findep.add(head);
							labdep.add(ss[7]);
						}
					}
				}
			}			
			if (detgraph.getNbMots()>0) {
				// il faut creer les liens de la phrase precedente puis appeler le GraphProcessor
				finish1phrase();
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void parseCONLL(String conllfile) {
		try {
			BufferedReader f;
			if (forceEncoding==null)
				f = new BufferedReader(new InputStreamReader(new FileInputStream(conllfile),"UTF-8"));
			else
				f = new BufferedReader(new InputStreamReader(new FileInputStream(conllfile),forceEncoding));
			parseCONLL(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void parseXML(BufferedReader f, int ngraphs) throws IOException {
		boolean listemots = false, listedeps=false;
		int curgraph = 0;
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			int i = s.indexOf("<SEQ id");
			if (i>=0) {
				// nouvelle phrase
				curgraph++;
				i=s.indexOf('_');
				int j = s.indexOf('"',i);
				detgraph = new DetGraph();
				detgraph.cursent = Integer.parseInt(s.substring(i+1,j));
			} else if (s.indexOf("<CONF>")>=0) {
				// confidence measure
				String s2 = s.replaceAll("<CONF>", "").replaceAll("</CONF>", "");
				detgraph.conf=Float.parseFloat(s2);
			} else if (s.indexOf("<CMT>")>=0 && !s.equals("<CMT>null</CMT>")) {
				// commentaire
				detgraph.comment = ""+s.replaceAll("<CMT>", "").replaceAll("</CMT>", "");
			} else if (s.indexOf("<SOURCE>")>=0 && !s.equals("<SOURCE>null</SOURCE>")) {
				// source URL
				File fx = new File(new URL(s.replaceAll("<SOURCE>", "").replaceAll("</SOURCE>", "")).getPath());
				detgraph.setSource(fx.toURI().toURL());
			} else if (s.indexOf("<TXT>")>=0) {
				// phrase de reference
				detgraph.sent = ""+s.replaceAll("<TXT>", "").replaceAll("</TXT>", "");
			} else if (s.indexOf("<tokens>")>=0) {
				// liste des mots
				listemots = true;
			} else if (s.indexOf("</tokens>")>=0) {
				// fin liste des mots
				listemots = true;
			} else if (listemots && s.indexOf("<t ")>=0) {
				// un mot
				i=s.indexOf("i=\"");
				int j = s.indexOf('"',i+3);
				int index=Integer.parseInt(s.substring(i+3,j));
				i=s.indexOf("l=\"");
				// attention, il peut y avoir des guillemets dans le mot lui-meme !
				j = s.indexOf("\" f=\"",i+3);
				String lemme=s.substring(i+3,j).trim();
				i=s.indexOf("f=\"",j);
				j = s.indexOf("\" c=\"",i+3);
				String mot=s.substring(i+3,j).trim();
				// il n'y a pas de guillemets dans les postags !
				i=s.indexOf("c=\"",j);
				j = s.indexOf('"',i+3);
				String postag=s.substring(i+3,j).trim();
				Mot m = new Mot(mot,lemme,postag);
				detgraph.addMot(index-1,m);
				// reste la position dans la source
				i=s.indexOf("srcpos=\"",j);
				if (i>=0) {
					j=s.indexOf(';',i+8);
					if (j>=0) {
						long deb = Long.parseLong(s.substring(i+8, j));
						i=s.indexOf('"',j+1);
						if (i>=0) {
							long fin = Long.parseLong(s.substring(j+1, i));
							m.setPosInTxt(deb, fin);
						}
					}
				}
			} else if (s.indexOf("<group>")>=0) {
                String sz=s.trim();
                int z=sz.lastIndexOf(' ');
                int zz=sz.lastIndexOf(' ',z-1);
                int zzz = sz.indexOf(' ')+1;
				detgraph.addgroup(Integer.parseInt(sz.substring(zz+1,z)), Integer.parseInt(sz.substring(z+1)), sz.substring(zzz,zz));
			} else if (s.indexOf("<dependances>")>=0) {
				listedeps=true;
			} else if (s.indexOf("</dependances>")>=0) {
				listedeps=false;
			} else if (listedeps && s.indexOf("<d ")>=0) {
				// une dependance gauche
				i=s.indexOf("r=\"");
				int j = s.indexOf('"',i+3);
				String depnom = s.substring(i+3,j);
				if (depnom.trim().length()==0) continue;
				i=s.indexOf("s=\"");
				j = s.indexOf('"',i+3);
				int mot1 = Integer.parseInt(s.substring(i+3,j));
				if (!detgraph.mots.containsKey(mot1-1)) continue;
				i=s.indexOf("c=\"");
				j = s.indexOf('"',i+3);
				int mot2 = Integer.parseInt(s.substring(i+3,j));
				if (mot2!=0&&!detgraph.mots.containsKey(mot2-1)) continue;
				detgraph.ajoutDep(depnom,mot1-1,mot2-1);
				/*
			} else if (listedeps && s.indexOf("<g ")>=0) {
				// est-ce que les dependances droite et gauche sont vraiment redondantes ??
				// une dependance droite
				i=s.indexOf("r=\"");
				int j = s.indexOf('"',i+3);
				String depnom = s.substring(i+3,j);
				if (depnom.trim().length()==0) continue;
				i=s.indexOf("s=\"");
				j = s.indexOf('"',i+3);
				int mot1 = Integer.parseInt(s.substring(i+3,j));
				i=s.indexOf("c=\"");
				j = s.indexOf('"',i+3);
				int mot2 = Integer.parseInt(s.substring(i+3,j));
				detgraph.ajoutDep(depnom,mot1-1,mot2-1);
				 */
			} else if (s.indexOf("</SEQ>")>=0) {
				// fin de phrase
				// System.err.println("fin de phrase "+detgraph.getNbMots());
				if (++cursent>=firstsent)
					processGraph(detgraph);
				if (ngraphs>=0&&curgraph>=ngraphs) break;
			}
		}
	}

	private void parseSyntexXML(String fichxml, int nmax) {
		boolean openInJAR = false;
		if (fichxml.startsWith("res:")) {
			openInJAR=true;
			fichxml=fichxml.substring(4);
			System.err.println("loading in jar: "+fichxml);
		}
		try {
			BufferedReader f;
			InputStream is;
			String enc = "ISO-8859-1";
			if (forceEncoding==null) {
				// check encoding
				if (openInJAR) is = getClass().getResourceAsStream(fichxml);
				else is = new FileInputStream(fichxml);
				if (fichxml.endsWith(".gz")) is = new GZIPInputStream(is);
				f = new BufferedReader(new InputStreamReader(is,enc));
				String sz = f.readLine();
//				System.err.println("checkenc: "+sz);
				if (sz.startsWith("<checkenc")) {
					// on peut verifier l'encoding ! Sinon, tant pis...
					char c = sz.charAt(10);
					if (c!='é') {
//						System.err.println("UTF8 detected !");
						enc = "UTF-8";
					}
					is.close();
//					System.err.println("retry checkenc");
					// re-check encoding
					if (openInJAR) is = getClass().getResourceAsStream(fichxml);
					else is = new FileInputStream(fichxml);
					if (fichxml.endsWith(".gz")) is = new GZIPInputStream(is);
					f = new BufferedReader(new InputStreamReader(is,enc));
					sz = f.readLine();
//					System.err.println("retry checkenc "+sz);
					c = sz.charAt(10);
					if (c!='é') {
						JOptionPane.showMessageDialog(null, "ERREUR FATALE encoding: ni UTF ni ISO ? Please check XML file encoding manually first");
						return;
					}
					is.close();
				} else {
					JOptionPane.showMessageDialog(null, "Erreur de lecture 1ere ligne du XML: assume UTF-8");
					enc = "UTF-8";
					is.close();
				}
			} else enc=forceEncoding;

			if (openInJAR) is = getClass().getResourceAsStream(fichxml);
			else is = new FileInputStream(fichxml);
			if (fichxml.endsWith(".gz")) is = new GZIPInputStream(is);
			f = new BufferedReader(new InputStreamReader(is,enc));
			parseXML(f,nmax);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processGraph(DetGraph detgraph) {
		// supprime mots vides
		for (int i=0;i<detgraph.getNbMots();i++) {
			Mot m = detgraph.getMot(i);
			if (m.getForme().trim().length()==0)
				if (detgraph.delNodes(i, i)) {
					// il ne reste aucun mot: on supprime la phrase
					return;
				}
		}
		phraseProcessor.processGraph(detgraph);
	}

	public String getTokens(HashMap<String, String> tokens, String s) {
		int i=s.indexOf("tokens=");
		int j=s.indexOf('"',i+8);
		String toks = s.substring(i+8,j);
		String[] tt = toks.split(" ");
		String r = "";
		for (i=0;i<tt.length;i++) {
			r+=tokens.get(tt[i]).trim()+" ";
		}
		return r;
	}
}
