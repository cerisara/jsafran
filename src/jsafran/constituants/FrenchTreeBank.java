package jsafran.constituants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.swing.JFileChooser;

import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.Mot;

public class FrenchTreeBank {
	JSafran m;

	public FrenchTreeBank(JSafran main) {
		m=main;
	}
	public void loadOverride() {
		File xmlfich = null;
		File wdir = new File(".");
		JFileChooser chooser = new JFileChooser(wdir);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			xmlfich = chooser.getSelectedFile();
		}
		if (xmlfich!=null) {
			m.allgraphs = loadFTB(xmlfich.getAbsolutePath());
			m.repaint();
		}
	}

	public static List<DetGraph> loadFTB(String xmlfich) {
		System.out.println("load FTB...");
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		try {
			Stack<String> balisesOpen = new Stack<String>();
			Stack<Integer> balisesFirstWord = new Stack<Integer>();
			BufferedReader f = new BufferedReader(new FileReader(xmlfich));
			DetGraph curg = null;
			String cat=null;
			boolean encodingseen=false;
			int i;
			for (long l=0;;l++) {
				String s0 = f.readLine();
				if (s0==null) break;
				s0=s0.trim();
				//				System.out.println("s0 "+l+" "+s0);
				StringTokenizer st = new StringTokenizer(s0, "<", true);
				while (st.hasMoreTokens()) {
					String s = st.nextToken().trim();
					if (s.equals("<")) s+=st.nextToken().trim();
					if (!encodingseen) {
						i=s.indexOf("encoding=\"");
						if (i>=0) {
							encodingseen=true;
							int j=s.indexOf('"',i+10);
							String enc = s.substring(i+10,j);
							f.close();
							f=new BufferedReader(new InputStreamReader(new FileInputStream(xmlfich), Charset.forName(enc)));
							for (;;) {
								s = f.readLine();
								i=s.indexOf("encoding=\"");
								if (i>=0) break;
							}
						}
					}
					{
						if (s.startsWith("</SENT>")) {
							balisesOpen.clear(); // juste par precaution...
							balisesFirstWord.clear();
							if (curg==null)
								System.out.println("WARNING: graph null loaded !");
							else
								res.add(curg);
							curg = null;
						} else if (s.startsWith("<SENT ")) {
							curg = new DetGraph();
						} else if (curg!=null) {
							if (s.startsWith("<w ")) {
								if (s.indexOf("compound=\"yes\"")>=0) {
									// c'est un compound, on le supprime
									i=s.indexOf(" cat=\"");
									if (i>=0) {
										int j=s.indexOf('"',i+6);
										cat=s.substring(i+6,j);
									}
								} else {
									// terminal
									int j;
									i=s.indexOf('>');
									String mot = s.substring(i+1).trim();
									if (mot.length()<1) {
										// mot virtuel "des" => "de le"
									} else {
										mot=mot.replace(' ', '_');
										Mot m = new Mot(mot, curg.getNbMots()+1);
										i=s.indexOf(" cat=\"");
										if (i>=0) {
											j=s.indexOf('"',i+6);
											cat=s.substring(i+6,j);
											m.setPOS(cat);
										} else {
											i=s.indexOf(" catint=\"");
											if (i>=0) {
												j=s.indexOf('"',i+9);
												String catint = s.substring(i+9,j).trim();
												if (catint.length()==0) m.setPOS(cat);
												else m.setPOS(catint);
											} else
												m.setPOS(cat);
										}
										curg.addMot(curg.getNbMots(), m);
									}
								}
							} else if (s.startsWith("</w>")) {
							} else if (s.startsWith("</")) {
								// fin d'un segment
								int j=s.indexOf('>');
								String x = s.substring(2,j);
								if (!balisesOpen.empty()&&x.equals(balisesOpen.peek())) {
									String seglab=balisesOpen.pop();
									int firstw = balisesFirstWord.pop();
									if (curg.getNbMots()-firstw<1)
										System.out.println("WARNING group empty");
									else
										curg.addgroup(firstw, curg.getNbMots()-1, seglab);
								}
							} else if (s.startsWith("<?")) {
							} else if (s.startsWith("<!--")) {
							} else if (s.startsWith("<")) {
								// non-terminal
								i=s.indexOf('>');
								if (i<0) {
									System.out.println("WARNING substring "+i+" "+s);
									System.exit(1);
									continue;
								}
								balisesOpen.add(s.substring(1,i));
								balisesFirstWord.add(curg.getNbMots());
							}
						}
					}
				}
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public static void splitCompoundWords(List<DetGraph> gs) {
		for (DetGraph g : gs) {
			for (int i=g.getNbMots()-1;i>=0;i--) {
				String w = g.getMot(i).getForme();
				String[] ss = w.split("_");
				if (ss.length>1) {
					Mot old = g.getMot(i);
					String p = old.getPOS();
					ArrayList<Mot> smallgroup = new ArrayList<Mot>();
					for (int j=ss.length-1;j>=0;j--) {
						String ww = ss[j].trim();
						if (ww.length()>0) {
							Mot m = new Mot(ww,ww,p);
							g.insertMot(i, m);
							smallgroup.add(0, m);
						}
					}
					if (g.groups!=null) {
						for (int l=0;l<g.groups.size();l++) {
							List<Mot> onegroup =g.groups.get(l);
							ArrayList<Mot> newgroup = new ArrayList<Mot>();
							for (int k=0;k<onegroup.size();k++) {
								if (onegroup.get(k)==old) {
									newgroup.addAll(smallgroup);
								} else newgroup.add(onegroup.get(k));
							}
							g.groups.set(l, newgroup);
						}
					}
				}
			}
		}
	}

	public static void main(String args[]) {
		List<DetGraph> gs = loadFTB("/home/xtof/git/jsafran/ftball.xml");
		PennTreeBank.save(gs, "/home/xtof/git/jsafran/ftball.mrg");
	}
}
