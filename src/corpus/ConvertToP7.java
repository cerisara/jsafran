package corpus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.POStagger;

/**
 * convertit le format ETB 2010 au format FTB
 * 
 * @author xtof
 *
 */
public class ConvertToP7 {
	
	ArrayList<String> listeVerbsPrenantEtreAuPasseCompose = new ArrayList<String>();
	ArrayList<String> listeVerbsPrenantObj = new ArrayList<String>();
	ArrayList<String> listeVerbsPrenantAObj = new ArrayList<String>();
	ArrayList<String> listeVerbsPrenantDeObj = new ArrayList<String>();
	
	MultiMots multimots = new MultiMots();
	
	public ConvertToP7() {
		multimots.loadCfg("jsynats.cfg");
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream("res/be-verbs.txt"),Charset.forName("UTF-8")));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim().toLowerCase();
				if (s.length()>0) {
					listeVerbsPrenantEtreAuPasseCompose.add(s);
				}
			}
			f.close();
			f = new BufferedReader(new InputStreamReader(new FileInputStream("res/obj-verbs.txt"),Charset.forName("UTF-8")));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim().toLowerCase();
				if (s.length()>0) listeVerbsPrenantObj.add(s);
			}
			f.close();
			f = new BufferedReader(new InputStreamReader(new FileInputStream("res/deobj-verbs.txt"),Charset.forName("UTF-8")));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim().toLowerCase();
				if (s.length()>0) listeVerbsPrenantDeObj.add(s);
			}
			f.close();
			f = new BufferedReader(new InputStreamReader(new FileInputStream("res/aobj-verbs.txt"),Charset.forName("UTF-8")));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim().toLowerCase();
				if (s.length()>0) listeVerbsPrenantAObj.add(s);
			}
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DetGraph getP7Graph(DetGraph g0) {
		DetGraph g = g0.getSubGraph(0);
		DetGraph gg = g.getSubGraph(0);
		gg.clearDeps();
		
		pretraite(g);
		
		// retag
		POStagger.tag(gg);
		
		// convert les dependances
		for (int i=0;i<g.getNbMots();i++) {
			int[] deps = g.getDeps(i);
			for (int j=0;j<deps.length;j++) {
				int head = g.getHead(deps[j]);
				String lab = g.getDepLabel(deps[j]);
				
				if (lab.equals("OBJ")) convOBJ(g,gg,i,head);
				else if (lab.equals("POBJ")) convPOBJ(g,gg,i,head);
				else if (lab.equals("SUJ")) convSUJ(g,gg,i,head);
				else if (lab.equals("ATTS")) convATTS(g,gg,i,head);
				else if (lab.equals("ATTO")) convATTO(g,gg,i,head);
				else if (lab.startsWith("MOD")) convMOD(g,gg,i,head);
				else if (lab.equals("COMP")) convCOMP(g,gg,i,head);
				else if (lab.equals("DET")) convDET(g,gg,i,head);
				// il faut traiter les AUX apres tous les autres
				else if (lab.equals("AUX")) {}
				else if (lab.equals("coord")) convCoord(g,gg,i,head);
				else if (lab.equals("dep_coord")) convDepCoord(g,gg,i,head);
				else if (lab.equals("REF")) convREF(g,gg,i,head);
				else if (lab.equals("JUXT")) convJUXT(g,gg,i,head);
				else if (lab.equals("APPOS")) convAPPOS(g,gg,i,head);
				else if (lab.equals("MultiMots")) convMultiMots(g,gg,i,head);
				else if (lab.equals("DUMMY")) convDUMMY(g,gg,i,head);
				else if (lab.equals("DISFL")) convDISFL(g,gg,i,head);
				else {
					System.out.println("WARNING: supprime dependance "+lab);
					
//					JOptionPane.showMessageDialog(null, "PROBLEM conversion: dep unknown: "+lab);
//					JSynATS.viewGraph(g0);
				}
			}
			for (int j=0;j<deps.length;j++) {
				int head = g.getHead(deps[j]);
				String lab = g.getDepLabel(deps[j]);
				if (lab.equals("AUX")) convAUX(g,gg,i,head);
			}
		}

		// identifie les multimots (+ leurs lemmes + leurs postags tree-tagger)
		multimots.reformeMultiMots(gg);

		return gg;
	}
	
	private void convJUXT(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("mod", gov, head);
	}
	private void convSUJ(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("suj", gov, head);
	}
	private void convATTS(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("ats", gov, head);
	}
	private void convATTO(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("ato", gov, head);
	}
	private void convCOMP(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("obj", gov, head);
	}
	private void convDET(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("det", gov, head);
	}
	private void convAPPOS(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("mod", gov, head);
	}
	private void convDUMMY(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("aff", gov, head);
	}
	private void convOBJ(DetGraph g, DetGraph gg, int gov, int head) {
		if (POStagger.isVerb(gg,gov) && gg.getMot(head).getLemme().toLowerCase().equals("faire")) {
			gg.ajoutDep("aux_caus", head, gov);
		} else {
			gg.ajoutDep("obj", gov, head);
		}
	}
	private void convPOBJ(DetGraph g, DetGraph gg, int gov, int head) {
		String lemmeM = gg.getMot(gov).getLemme();
		if (lemmeM.equals("de") || lemmeM.equals("en") || lemmeM.equals("dont")) {
			gg.ajoutDep("de_obj", gov, head);
		} else if (lemmeM.equals("à") || lemmeM.equals("y") || lemmeM.equals("auquel") ||
				(lemmeM.equals("lui") && true)) { // TODO: comment verifier les clitic ?
			gg.ajoutDep("a_obj", gov, head);
		} else if (POStagger.isVerb(gg, head)) {
			gg.ajoutDep("p_obj", gov, head);
		} else {
			int[] deps = g.getDeps(gov);
			String deplab="";
			for (int i=0;i<deps.length;i++)
				if (g.getHead(deps[i])==head) deplab = g.getDepLabel(deps[i]);
			if (deplab.equals("COMP"))
				gg.ajoutDep("dep", gov, head);
			else
				gg.ajoutDep("mod", gov, head);
		}
	}
	
	
	private void convMOD(DetGraph g, DetGraph gg, int gov, int head) {
		// test si relative: il faut detecter si il y a un pronom relatif que gov gouverne
		boolean isrel=false;
		if (POStagger.isVerb(gg,gov)) {
			b:
			for (int i=0;i<gg.getNbMots();i++) {
				if (gg.getMot(i).getPOS().equals("PRO:REL")) {
					int[] deps = gg.getDeps(i);
					for (int j=0;j<deps.length;j++) {
						int hh = gg.getHead(deps[j]);
						if (hh==gov) {
							// on a une relative !
							isrel=true;
							gg.ajoutDep("mod_rel", gov, head);
							break b;
						}
					}
				}
			}
		}
		if (!isrel) {
			if (POStagger.isPrep(gg,gov)) {
				// c'est un groupe prepositionnel
				if (POStagger.isVerb(gg, head))
					gg.ajoutDep("mod", gov, head);
				else if (POStagger.isNoun(gg, head) || POStagger.isAdj(gg,head))
					gg.ajoutDep("dep", gov, head);
				else
					gg.ajoutDep("mod", gov, head);
			} else {
				gg.ajoutDep("mod", gov, head);
			}
		}
	}

	private void convAUX(DetGraph g, DetGraph gg, int gov, int head) {
		if (gg.getMot(gov).getLemme().equals("être") &&
				gg.getMot(head).getPOS().equals("VER:pper")) {

			if (listeVerbsPrenantEtreAuPasseCompose.contains(gg.getMot(head).getLemme())) {
				gg.ajoutDep("aux_tps", gov, head);
			} else {

				// est-ce un verbe pronominial ?

				boolean se=false;
				for (int i=0;i<g.getNbMots();i++) {
					if (gg.getMot(i).getLemme().equals("se")) {
						int[] deps = g.getDeps(i);
						for (int j=0;j<deps.length;j++) {
							if (g.getHead(deps[j])==gov) {
								String dd = g.getDepLabel(deps[j]);
								if (dd.equals("obj") || dd.equals("a_obj") || dd.equals("de_obj") || dd.equals("p_obj") || dd.equals("aff")) {
									se=true; break;
								}
							}
						}
						if (se) break;
					}
				}
				if (se) {
					gg.ajoutDep("aux_tps", gov, head);
				} else {
					gg.ajoutDep("aux_pass", gov, head);
				}
			}
		} else if (gg.getMot(gov).getLemme().equals("avoir") &&
				gg.getMot(head).getPOS().equals("VER:pper")) {
			gg.ajoutDep("aux_tps", gov, head);
		} else if (gg.getMot(gov).getLemme().equals("faire")) {
			gg.ajoutDep("aux_caus", gov, head);
		} else {
			// TODO: parfois, les AUX sont inverses !
			System.out.println("ERREUR CONV AUX "+g);
		}
	}

	/*
	 * transforme les relations CC: deplace la tete du groupe de la conjonction vers le 1er elt a gauche
	 * s'il y en a un
	 */
	private void pretraite(DetGraph g) {
		for (int i=0;i<g.getNbMots();i++) {
			int[] deps = g.getDeps(i);
			for (int j=0;j<deps.length;j++) {
				if (g.getDepLabel(deps[j]).equals("CC")) {
					// on a trouve le 1er elt d'un groupe conjonctif
					int posconj = g.getHead(deps[j]);
					if (posconj<i) {
						// la conjonction est la plus a gauche
						// on transforme toutes les relations en dep_coord
						for (int k=i;k<g.getNbMots();k++) {
							int[] dd = g.getDeps(k);
							for (int l=0;l<dd.length;l++) {
								if (g.getDepLabel(dd[l]).equals("CC") && g.getHead(dd[l])==posconj) {
									// on a trouve un autre elt du groupe
									g.deps.get(dd[l]).type=Dep.getType("dep_coord");
								}
							}
						}
					} else {
						// l'elt trouve est le plus a gauche
						
						// on transforme toutes les *autres* relations en dep_coord
						for (int k=i+1;k<g.getNbMots();k++) {
							int[] dd = g.getDeps(k);
							for (int l=0;l<dd.length;l++) {
								if (g.getDepLabel(dd[l]).equals("CC") && g.getHead(dd[l])==posconj) {
									// on a trouve un autre elt du groupe
									g.deps.get(dd[l]).type=Dep.getType("dep_coord");
								}
							}
						}
						// on decale les deps sortantes de la conj vers le 1er elt qui devient la tete du groupe
						int[] dd = g.getDeps(posconj);
						for (int k=0;k<dd.length;k++) {
							g.deps.get(dd[k]).gov=g.getMot(i);
						}
						// on inverse la relation vers le coordonant
						g.deps.get(deps[j]).type=Dep.getType("coord");
						g.deps.get(deps[j]).gov=g.getMot(posconj);
						g.deps.get(deps[j]).head=g.getMot(i);
						// on repart sur l'elt le + a gauche, car on vient de decaler des deps sur lui
						i--;
					}
				}
			}
		}
	}
	private void convCoord(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("coord", gov, head);
	}
	private void convDepCoord(DetGraph g, DetGraph gg, int gov, int head) {
		gg.ajoutDep("dep_coord", gov, head);
	}

	private void convREF(DetGraph g, DetGraph gg, int gov, int head) {
		if (listeVerbsPrenantObj.contains(gg.getMot(head).getLemme())) {
			gg.ajoutDep("obj", gov, head);
		} else if (listeVerbsPrenantAObj.contains(gg.getMot(head).getLemme())) {
			gg.ajoutDep("a_obj", gov, head);
		} else if (listeVerbsPrenantDeObj.contains(gg.getMot(head).getLemme())) {
			gg.ajoutDep("de_obj", gov, head);
		} else {
			// TODO: est-ce bien un AFF sinon ?
			gg.ajoutDep("aff", gov, head);
		}
	}
	
	private void convMultiMots(DetGraph g, DetGraph gg, int gov, int head) {
		// je ne fais rien (supprime la relation), car les multi-mots sont traites apres les dependances 
	}

	private void convDISFL(DetGraph g, DetGraph gg, int gov, int head) {
		// TODO: 
		gg.ajoutDep("dep", gov, head);
	}

	
	// ********************************** 
	
	public static void main(String args[]) {
		ConvertToP7 m = new ConvertToP7();
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(args[0]);
		PrintWriter fout = GraphIO.saveEntete("output.xml");
		for (int i=0;i<gs.size();i++) {
			DetGraph gg = m.getP7Graph(gs.get(i));
			gg.save(fout);
		}
		fout.close();
	}
}
