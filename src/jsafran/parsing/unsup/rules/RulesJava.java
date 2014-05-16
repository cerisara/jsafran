package jsafran.parsing.unsup.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * 
 * Generic rules in Java, because generic rules in cfgfile are way too slow
 * 
 * TODO: possibilité d'ajouter des contraintes comme "SUJ est dans 80% des cas avant OBJ"
 * en activant ou désactivant, avant chaque itération,  la règle en fonction de cette proba
 * 
 * @author xtof
 *
 */
public class RulesJava implements Rules {

	@Override
	public int getNrules() {
		return 1;
	}

        @Override
        public void setSRL(boolean srl){

        };


	int[] posidx;

	public void init(List<DetGraph> gs) {
		int nmaxwrd = 0;
		for (DetGraph g : gs)
			if (g.getNbMots()>nmaxwrd) nmaxwrd=g.getNbMots();
		posidx = new int[nmaxwrd*(nmaxwrd-1)*Dep.depnoms.length];

		if (false) {
			Random r = new Random();
			for (DetGraph g : gs) {
				int[] ru = getApplicable(0, g);
				int n2apply = (int)((float)ru.length*0.75f);
				for (int i=0;i<n2apply;i++) {
					applyRule(0, g, ru[r.nextInt(ru.length)]);
				}
			}
		}
	}

	@Override
	// return the pattern idx
	public int[] getApplicable(int ridx, DetGraph g) {
		// Any word can be linked to any other word = Nd*(Nw - 1) possible links
		// total = Nw * Nd * (Nw-1)
		int n=0;
		int idx=0;
		for (int gov=0;gov<g.getNbMots();gov++) {
			for (int head=0;head<g.getNbMots();head++) {
				if (head==gov) continue;
				for (int d=0;d<Dep.depnoms.length;d++) {
					if (checkRule(g,gov,head,d)) {
						posidx[n++]=idx;
					}
					// TODO: add a proba to deactivate the rule to fit "fuzzy" user-defined constraints
					idx++;
				}
			}
		}
		int[] res = Arrays.copyOf(posidx, n);
		
		float amb = ambiguityLevel(g);
		g.conf=amb;
		
		return res;
	}

	@Override
	public int[] applyRule(int ridx, DetGraph g, int idx) {
		int gov = idx/((g.getNbMots()-1)*Dep.depnoms.length);
		int x = idx%((g.getNbMots()-1)*Dep.depnoms.length);
		int head = x/Dep.depnoms.length;
		if (head>=gov) head++;
		int lab = x%Dep.depnoms.length;

		int d=g.getDep(gov);
		if (d>=0) g.removeDep(d);
		g.ajoutDep(lab, gov, head);
		int[] res = {gov};
		return res;
	}

	@Override
	public void applyDetRules(DetGraph g) {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString(int ridx) {
		// TODO Auto-generated method stub
		return null;
	}

	final int sujidx = Dep.getType("SUJ");
	final int objidx = Dep.getType("OBJ");
	final int auxidx = Dep.getType("AUX");
	final int pobjidx = Dep.getType("POBJ");
	final int detidx = Dep.getType("DET");
	final int mmidx = Dep.getType("MultiMots");
	final int attsidx = Dep.getType("ATTS");
	final int attoidx = Dep.getType("ATTO");
	final int refidx = Dep.getType("REF");
	final int juxtidx = Dep.getType("JUXT");
	final int apposidx = Dep.getType("APPOS");
	final int dummyidx = Dep.getType("DUMMY");
	final int ccidx = Dep.getType("CC");
	final int compidx = Dep.getType("COMP");

	private boolean checkRule(DetGraph g, int gov, int head, int lab) {
		if (lab==sujidx) return checkSUJ(g,gov,head);
		if (lab==auxidx) return checkAUX(g,gov,head);
		if (lab==objidx) return checkOBJ(g,gov,head);
		if (lab==pobjidx) return checkPOBJ(g,gov,head);
		if (lab==detidx) return checkDET(g,gov,head);
		if (lab==attsidx) return checkATTS(g,gov,head);
		if (lab==refidx) return checkREF(g,gov,head);
		if (lab==ccidx) return checkCC(g,gov,head);
		if (lab==compidx) return checkCOMP(g,gov,head);

		if (lab==dummyidx) return checkDUMMY(g,gov,head);
		if (lab==apposidx) return false;
		if (lab==juxtidx) return false;
		if (lab==attoidx) return false;
		if (lab==mmidx) return false;
		return true;
	}

	// ==================================================
	private boolean checkDUMMY(DetGraph g, int gov, int head) {
		if (head<gov) return false;
		String f = g.getMot(gov).getForme().toLowerCase();
		if (!(f.equals("y"))) return false;
		if (!g.getMot(head).getPOS().startsWith("VER")) return false;
		return true;
	}
	private boolean checkREF(DetGraph g, int gov, int head) {
		if (head<gov) return false;
		String f = g.getMot(gov).getForme().toLowerCase();
		if (!(f.equals("se")||f.equals("s'"))) return false;
		if (!g.getMot(head).getPOS().startsWith("VER")) return false;
		return true;
	}
	private boolean checkCC(DetGraph g, int gov, int head) {
		String p = g.getMot(head).getPOS();
		// TODO
		return false;
	}
	private boolean checkCOMP(DetGraph g, int gov, int head) {
		if (head>gov) return false;
		if (g.getMot(head).getPOS().startsWith("PRP")) return true;
		if (g.getMot(head).getPOS().startsWith("KON")) return true;
		return false;
	}
	private boolean checkSUJ(DetGraph g, int gov, int head) {
		String p = g.getMot(head).getPOS();
		if (p.equals("VER:infi")) return false;
		if (p.startsWith("VER")) return true;
		return false;
	}
	private boolean checkAUX(DetGraph g, int gov, int head) {
		String l = g.getMot(gov).getLemme();
		if (head<gov) return false;
		if (!(l.equals("être")||l.equals("avoir"))) return false;
		if (g.getMot(head).getPOS().equals("VER:pper")) return true;
		return false;
	}
	private boolean checkOBJ(DetGraph g, int gov, int head) {
		String p = g.getMot(head).getPOS();
		if (p.startsWith("VER")) return true;
		return false;
	}
	private boolean checkPOBJ(DetGraph g, int gov, int head) {
		String p = g.getMot(head).getPOS();
		if (p.startsWith("VER")) return true;
		return false;
	}
	private boolean checkDET(DetGraph g, int gov, int head) {
		if (head<gov) return false;
		String p = g.getMot(head).getPOS();
		if (!(p.equals("NOM")||p.equals("NUM")||p.equals("NAM")||p.equals("ABR"))) return false;
		p = g.getMot(gov).getPOS();
		if (!(p.startsWith("DET")||p.equals("PRO:IND"))) return false;
		return true;
	}
	private boolean checkATTS(DetGraph g, int gov, int head) {
		if (gov<head) return false;
		if (g.getMot(head).getLemme().equals("être")) return true;
		return false;
	}
	
	
	// ====================================================================
	
	/**
	 * for debug: compute the ambiguity of a sentence given a set of rules
	 * TODO: cette ambiguite est a debugger
	 */
	public float ambiguityLevel(DetGraph g) {
		int namb=0;
		for (int gov=0;gov<g.getNbMots();gov++) {
			int nheads=0;
			for (int head=0;head<g.getNbMots();head++) {
				if (head==gov) continue;
				int ndeps=0;
				for (int d=0;d<Dep.depnoms.length;d++) {
					// teste tous les liens possibles, ne retient que ceux autorises
					if (checkRule(g,gov,head,d)) {
						ndeps++;
						if (nheads==0) nheads++;
					}
				}
				if (ndeps>1) namb+=ndeps-1;
			}
			if (nheads>1) namb+=nheads-1;
		}
		return (float)namb;
	}

	/**
	 * test en creant tous les liens possibles pour initialiser les counts
	 * puis en "supprimant" les liens un par un
	 */
	public void createAllFirst(List<DetGraph> gs) {
		for (DetGraph g : gs) {
			int[] w = getApplicable(0, g);
			for (int idx : w) {
				int gov = idx/((g.getNbMots()-1)*Dep.depnoms.length);
				int x = idx%((g.getNbMots()-1)*Dep.depnoms.length);
				int head = x/Dep.depnoms.length;
				if (head>=gov) head++;
				int lab = x%Dep.depnoms.length;
				g.ajoutDep(lab, gov, head);
			}
		}
	}
	
	/**
	 * plutot que de sampler les rules, on utilise les rules en
	 * pre-procesing pour reduire la taille du treillis des dépendances
	 * créables sur chaque mot.
	 * Idem, on réduit le treillis en imposant un cout "final" très grand
	 * sur les arbres projectifs et avec des cycles.
	 * Viterbi peut être utilisé pour réduire ce treillis ainsi.
	 * 
	 * Monte Carlo n'est appliqué qu'ensuite, pour sampler des _rules_
	 * avec seulement un critère lexical et sur les frames.
	 * 
	 * 
	 */
	public static void reduceTrellis(final DetGraph g) {
		// le trellis est dans les dépendances de g
		// il a été construit par applyAllRules(g)
		// on ne peut pas utiliser Viterbi; on enumere donc toutes les deps possibles
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				
				int[] chosendeps = new int[g.getNbMots()];
				Arrays.fill(chosendeps,-1);
				recurs(g,chosendeps,0);
				System.out.println("all trees: "+trees.size());
			}
		});
		t.start();
	}
	static ArrayList<int[]> trees = new ArrayList<int[]>();
	static void recurs(DetGraph g, int[] prevdeps, int w) {
//		System.out.println("debug "+w+" "+g.deps);
		if (w>=g.getNbMots()) {
			// terminaison
			// debug
			DetGraph gg = g.getSubGraph(0);
			gg.clearDeps();
			for (int i=0;i<prevdeps.length;i++) {
				if (prevdeps[i]>=0) {
					Dep d = g.deps.get(prevdeps[i]);
					gg.deps.add(d);
				}
			}
//			JSafran.viewGraph(gg);
			trees.add(Arrays.copyOf(prevdeps, prevdeps.length));
			return;
		}
		
		Mot gov = g.getMot(w);
		for (int depidx=0;depidx<g.deps.size();depidx++) {
			Dep d =g.deps.get(depidx);
			if (d.gov==gov) {
				// check proj
				int govidx = w;
				int headidx = d.head.getIndexInUtt()-1;
				boolean isok=true;
				if (govidx<headidx) {
					for (int i=0;i<w;i++) {
						if (prevdeps[i]<0) continue;
						int hi = g.deps.get(prevdeps[i]).head.getIndexInUtt()-1;
						// on a forcement gidx<govidx
						if (hi>govidx&&hi<headidx) {
							isok=false; break;
						}
					}
				} else {
					for (int i=0;i<headidx;i++) {
						if (prevdeps[i]<0) continue;
						int hi = g.deps.get(prevdeps[i]).head.getIndexInUtt()-1;
						// on a forcement gidx<govidx
						if (hi<govidx&&hi>headidx) {
							isok=false; break;
						}
					}
					if (isok) {
						for (int i=headidx+1;i<govidx;i++) {
							if (prevdeps[i]<0) continue;
							int hi = g.deps.get(prevdeps[i]).head.getIndexInUtt()-1;
							// on a forcement gidx<govidx
							if (hi<headidx||hi>govidx) {
								isok=false; break;
							}
						}
						if (isok) {
							// check cycles
							// TODO: conserver la liste de HEADS pour ne pas avoir a la recalculer
							for (int j=0;j<govidx;j++) {
								if (prevdeps[j]>=0) {
									Dep dd = g.deps.get(prevdeps[j]);
									int ddhead = dd.head.getIndexInUtt()-1;
//									System.out.println("isOK "+govidx+" "+j+" "+ddhead+" "+prevdeps[j]);
									if (ddhead==govidx) {
										if (headidx>=j) {isok=false; break;}
									}
								}
							}
						}
					}
				}
				
				// now, if the dep is OK, we recurse
				if (isok) {
					prevdeps[w]=depidx;
					recurs(g, prevdeps, w+1);
				} else {
					// on n'ajoute pas cette dep; passe a la suivante
				}
				prevdeps[w]=-1;
			}
		}
	}

	@Override
	public double getRulePrior(int ridx) {
		// TODO Auto-generated method stub
		return 0;
	}
}
