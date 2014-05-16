package jsafran.parsing.unsup.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsafran.DetGraph;
import jsafran.Mot;
import jsafran.parsing.unsup.SemanticProcesing;
import opennlp.tools.coref.sim.SemanticCompatibility;

/**
 * rules complementaires des rules definies dans synats.cfg
 * 
 * @author cerisara
 *
 */
public class CompRulesFr implements Rules {
	public interface SingleRule {
		public int[] getApplicable(DetGraph g);
		public int[] applyRule(DetGraph g, int pos);
		public String getName();
	}
	ArrayList<CompRulesFr.SingleRule> localrules = new ArrayList<CompRulesFr.SingleRule>();

	public CompRulesFr() {
		localrules.add(new CompRulesFr.GP());
//		localrules.add(new CompRulesFr.twoNAM());
		localrules.add(new CompRulesFr.sujA0());
                localrules.add(new CompRulesFr.sujA1());
		localrules.add(new CompRulesFr.gpcomp());
		localrules.add(new CompRulesFr.objA0());
		localrules.add(new CompRulesFr.objA1());
//		localrules.add(new CompRulesFr.atts());
		localrules.add(new CompRulesFr.rel());
//		localrules.add(new CompRulesFr.aux());
		//		localrules.add(new modA());
		//		localrules.add(new modB());
//		localrules.add(new CompRulesFr.heure());
//		localrules.add(new CompRulesFr.det());

		//		localrules.add(new root());

		// rules for ETAPE corpus
//		localrules.add(new CompRulesFr.euh());
		//		localrules.add(new repet1mot());
//		localrules.add(new CompRulesFr.repet());
//		localrules.add(new CompRulesFr.firstDM());
	}

	class firstDM implements CompRulesFr.SingleRule {
		// warning: must be SORTED !
		final String[] dms = {"dont","et","mais"};
		@Override
		public int[] getApplicable(DetGraph g) {
			if (Arrays.binarySearch(dms, g.getMot(0).getForme())>=0) {
				for (int i=g.getNbMots()-1;i>=0;i--) {
					int d=g.getDep(i);
					if (d<0) {
						int[] r = {i};
						return r;
					}
				}
			}
			int[] r = {};
			return r;
		}
		@Override
		public int[] applyRule(DetGraph g, int i) {
			int d=g.getDep(0);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("Ddm", 0, i);
			int[] r = {0};
			return r;
		}
		@Override
		public String getName() {
			return "firstDM";
		}
	}
	class repet implements CompRulesFr.SingleRule {
		final int nmaxrepet=5;
		@Override
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> res = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++)
				for (int j=nmaxrepet;j>=1;j--) {
					if (i+j+j-1<g.getNbMots()) {
						boolean repet=true;
						for (int k=0;k<j;k++) {
							if (!g.getMot(i+k).getForme().equals(g.getMot(i+j+k))) {
								repet=false; break;
							}
						}
						if (repet) {
							res.add(i);
							break;
						}
					}
				}
			int[] r = new int[res.size()];
			for (int i=0;i<r.length;i++) r[i]=res.get(i);
			return r;
		}

		@Override
		public int[] applyRule(DetGraph g, int i) {
			for (int j=nmaxrepet;j>=1;j--) {
				if (i+j+j-1<g.getNbMots()) {
					boolean repet=true;
					for (int k=0;k<j;k++) {
						if (!g.getMot(i+k).getForme().equals(g.getMot(i+j+k))) {
							repet=false; break;
						}
					}
					if (repet) {
						// chercher HEAD le + a droite
						int h2=i+j+j-1;
						for (int k=i+j+j-1;k>=i+j;k--) {
							int d=g.getDep(k);
							if (d<0) {h2=k;break;}
							int htmp=g.getHead(d);
							if (htmp>i+j+j-1||htmp<i+j) {h2=k;break;}
						}
						// chercher HEAD premier terme
						int h1=i+j-1;
						for (int k=i+j-1;k>=i;k--) {
							int d=g.getDep(k);
							if (d<0) {h1=k;break;}
							int htmp=g.getHead(d);
							if (htmp>i+j-1||htmp<i) {h1=k;break;}
						}
						int d=g.getDep(h1);
						if (d>=0) g.removeDep(d);
						g.ajoutDep("Drep", h1, h2);
						int[] ds = {h1};
						return ds;
					}
				}
			}
			return null;
		}

		@Override
		public String getName() {
			return "repet";
		}
	}
	class euh implements CompRulesFr.SingleRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> res = new ArrayList<Integer>();
			if (g.getNbMots()>1)
				for (int i=0;i<g.getNbMots();i++)
					if (g.getMot(i).getForme().equals("euh")) {
						res.add(i);
					}
			int[] r = new int[res.size()];
			for (int i=0;i<r.length;i++) r[i]=res.get(i);
			return r;
		}

		@Override
		public int[] applyRule(DetGraph g, int pos) {
			if (pos==0) {
				g.ajoutDep("Ddm", 0, 1);
				int[] r = {0};
				return r;
			} else {
				g.ajoutDep("Ddm", pos,pos-1);
				int[] r = {pos};
				return r;
			}
		}

		@Override
		public String getName() {
			return "euh";
		}
	}
	class repet1mot implements CompRulesFr.SingleRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> res = new ArrayList<Integer>();
			if (g.getNbMots()>1)
				for (int i=0;i<g.getNbMots()-1;i++)
					if (g.getMot(i).getForme().equals(g.getMot(i+1).getForme())) {
						res.add(i);
					}
			int[] r = new int[res.size()];
			for (int i=0;i<r.length;i++) r[i]=res.get(i);
			return r;
		}

		@Override
		public int[] applyRule(DetGraph g, int pos) {
			if (pos==0) {
				g.ajoutDep("Drep", 0, 1);
				int[] r = {0};
				return r;
			} else {
				g.ajoutDep("Drep", pos,pos-1);
				int[] r = {pos};
				return r;
			}
		}

		@Override
		public String getName() {
			return "repet1mot";
		}
	}


	@Override
	public int getNrules() {
		return localrules.size();
	}

	@Override
	public int[] getApplicable(int ridx, DetGraph g) {
		return localrules.get(ridx).getApplicable(g);
	}

	@Override
	public double getRulePrior(int ridx) {
		return 1;
	}

	@Override
	public int[] applyRule(int ridx, DetGraph g, int pos) {
		return localrules.get(ridx).applyRule(g, pos);

	}

	@Override
	public void applyDetRules(DetGraph g) {
		for (int i=g.deps.size()-1;i>=0;i--) {
			if (g.deps.get(i).getGov()==g.deps.get(i).getHead()) g.deps.remove(i);
		}
	}

	@Override
	public String toString(int ridx) {
		if (ridx==0) return "linkGP";
		if (ridx==1) return "twoNAM";
		if (ridx==2) return "suj";
		if (ridx==3) return "gpcomp";
		if (ridx==4) return "obj";
		if (ridx==5) return "rel";
		if (ridx==6) return "aux";
		if (ridx==7) return "modA";
		return null;
	}

	@Override
	public void init(List<DetGraph> gs) {
	}
	@Override
	public void setSRL(boolean srl){

	};

	// =================================================================

	// verifie si on n'a pas deja la meme dep
	private boolean checkDepExists(DetGraph g, int gov) {
		return !isNewDep(g, gov);
	}
	private boolean isDepAlreadyThere(DetGraph g, int gov, int head, String lab) {
		int d = g.getDep(gov);
		if (d<0) return false;
		String ll = g.getDepLabel(d);
		// les deps qui commencent par '_' ne doivent jamais etre supprimees !
		if (ll.charAt(0)=='_') return true;
		int h = g.getHead(d);
		if (h!=head) return false;
		else {
			if (lab==null||ll.equals(lab)) return true;
			else return false;
		}
	}
	// verifie si on n'ecrase pas une autre dep plus ancienne
	private boolean isNewDep(DetGraph g, int gov) {
		int d = g.getDep(gov);
		if (d<0) return true;
		return false;
	}

	private boolean isNumber(DetGraph g, int i) {
		char c=g.getMot(i).getForme().charAt(0);
		return (Character.isDigit(c));
	}
	private boolean isGNHead(DetGraph g, int i) {
		return g.getGroupNoms(i).contains("GNHEAD");
	}
	private boolean isGN(DetGraph g, int i) {
		List<String> gn = g.getGroupNoms(i);
		for (String gnn : gn) if (gnn.startsWith("GN")) return true;
		return false;
	}

	// =================================================================
	class GP implements CompRulesFr.SingleRule {
		public String getName() {return "GPrule";}

		// cree le lien EXTERNE du GP
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++) {
				String pos = g.getMot(i).getPOS();
				if (pos.startsWith("P")) {
					if (i==0) {
						// TODO debut de phrase: "de la linguistique..."
					} else {
						int j;
						for (j=i-1;j>=0;j--) {
							if (g.getMot(j).getPOS().startsWith("V")) {
								if (!checkDepExists(g, i)) {
									pp.add(i);  // POBJ
									pp.add(-i); // MOD
									break;
								}
							} else if (g.getGroupNoms(j).contains("GNHEAD")) {
								if (!checkDepExists(g, i)) {
									pp.add(i);
									break;
								}
							}
						}
						if (j<0) {
							// TODO: aucun lien trouve
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			int i=pos;
			String dlab="POBJ";
			if (i<0) {dlab="MOD"; i=-i;}
			if (i==0) {
				// TODO: "de la linguistique..."
			} else {
				int j;
				for (j=i-1;j>=0;j--) {
					if (g.getMot(j).getPOS().startsWith("V")) {
						int d = g.getDep(i);
						if (d>=0) g.removeDep(d);
						g.ajoutDep(dlab, i, j);
						int[] res = {i};
						return res;
					} else if (g.getGroupNoms(j).contains("GNHEAD")) {
						int d = g.getDep(i);
						if (d>=0) g.removeDep(d);
						g.ajoutDep("MOD", i, j);
						int[] res = {i};
						return res;
					}
				}
			}
			System.out.println("ERROR linkGP_applyRule "+pos+" "+g);
			return null;
		}
	}

	// =================================================================
	class twoNAM implements CompRulesFr.SingleRule {
		public String getName() {return "twoNAM";}

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=1;i<g.getNbMots();i++) {
				if ((g.getMot(i).getPOS().equals("NPP") && g.getMot(i-1).getPOS().equals("NPP"))||
						(isNumber(g,i)&&isGNHead(g,i-1))) {
					if (!checkDepExists(g,i)) {
						pp.add(i);
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("MOD", pos, pos-1);
			// TODO: rassembler aussi les GNs ? Pb=on ne peut plus revenir sur cette decision ! ==> il faut supprimer les GNs !
			int[] changed = {pos};
			return changed;
		}
	}

	// =================================================================
	class sujA0 implements CompRulesFr.SingleRule {
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> head = new ArrayList<Integer>();
		public String getName() {return "suj";}
		public int[] getApplicable(DetGraph g) {
			govs.clear(); head.clear();
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gov = m.getIndexInUtt()-1;
					for (int j=gov+1;j<g.getNbMots();j++) {
						String pos = g.getMot(j).getPOS();
						if (pos.startsWith("V") && !pos.endsWith("INF")) {
							if (!checkDepExists(g, gov)) {
								govs.add(gov);
								head.add(j);
							}
						}
					}
				}
			}
			int[] res = new int[govs.size()];
			for (int i=0;i<res.length;i++) res[i] = i;
			return res;
		}
		public int[] applyRule(DetGraph g, int p) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        SemanticProcesing.isFrench=true;
			int gov=govs.get(p);
			int d=g.getDep(gov);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("SUJ", gov, head.get(p));
                        if (SemanticProcesing.isPredicate(g, head.get(p))){
                            int[] depsWithPos=gsrl.getDeps(gov);
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==head.get(p)) gsrl.removeDep(dd); break;
                            }
                            gsrl.ajoutDep("A0", gov, head.get(p));
                        }
			int[] changed = {gov};
			return changed;
		}
	}

	class sujA1 implements CompRulesFr.SingleRule {
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> head = new ArrayList<Integer>();
		public String getName() {return "suj";}
		public int[] getApplicable(DetGraph g) {
			govs.clear(); head.clear();
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gov = m.getIndexInUtt()-1;
					for (int j=gov+1;j<g.getNbMots();j++) {
						String pos = g.getMot(j).getPOS();
						if (pos.startsWith("V") && !pos.endsWith("INF")) {
							if (!checkDepExists(g, gov)) {
								govs.add(gov);
								head.add(j);
							}
						}
					}
				}
			}
			int[] res = new int[govs.size()];
			for (int i=0;i<res.length;i++) res[i] = i;
			return res;
		}
		public int[] applyRule(DetGraph g, int p) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        SemanticProcesing.isFrench=true;
			int gov=govs.get(p);
			int d=g.getDep(gov);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("SUJ", gov, head.get(p));
                        if (SemanticProcesing.isPredicate(g, head.get(p))){
                            int[] depsWithPos=gsrl.getDeps(gov);
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==head.get(p)) gsrl.removeDep(dd); break;
                            }
                            gsrl.ajoutDep("A1", gov, head.get(p));
                        }
			int[] changed = {gov};
			return changed;
		}
	}
        
	// =================================================================
	class gpcomp implements CompRulesFr.SingleRule {
		public String getName() {return "gpcomp";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots()-1;i++) {
				if (g.getMot(i).getPOS().startsWith("P")) {
					if (!g.getGroupNoms(i+1).contains("NP")) {
						if (!checkDepExists(g, i+1)) {
							pp.add(i+1);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int pos) {
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("COMP", pos, pos-1);
			int[] res = {pos};
			return res;
		}
	}

	// =================================================================
	class objA0 implements CompRulesFr.SingleRule {
		public String getName() {return "obj";}
		ArrayList<Integer> objgovs = new ArrayList<Integer>();
		ArrayList<Integer> objhead = new ArrayList<Integer>();
		public int[] getApplicable(DetGraph g) {
			objgovs.clear(); objhead.clear();
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gov = m.getIndexInUtt()-1;
					for (int j=gov-1;j>=0;j--) {
						String pos = g.getMot(j).getPOS();
						if (pos.startsWith("V")) {
							if (!checkDepExists(g, gov)) {
								objgovs.add(gov);
								objhead.add(j);
							}
						}
					}
				}
			}
			int[] res = new int[objgovs.size()];
			for (int i=0;i<res.length;i++) res[i] = i;
			return res;
		}
		public int[] applyRule(DetGraph g, int p) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        SemanticProcesing.isFrench=true;
                        int gov=objgovs.get(p);
			int d=g.getDep(gov);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("OBJ", gov, objhead.get(p));
                        if (SemanticProcesing.isPredicate(g, objhead.get(p))){
                            int[] depsWithPos=gsrl.getDeps(gov);
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==objhead.get(p)) gsrl.removeDep(dd); break;
                            }
                            gsrl.ajoutDep("A0", gov, objhead.get(p));
                        }
			int[] changed = {gov};
			return changed;
		}
	}
	class objA1 implements CompRulesFr.SingleRule {
		public String getName() {return "obj";}
		ArrayList<Integer> objgovs = new ArrayList<Integer>();
		ArrayList<Integer> objhead = new ArrayList<Integer>();
		public int[] getApplicable(DetGraph g) {
			objgovs.clear(); objhead.clear();
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gov = m.getIndexInUtt()-1;
					for (int j=gov-1;j>=0;j--) {
						String pos = g.getMot(j).getPOS();
						if (pos.startsWith("V")) {
							if (!checkDepExists(g, gov)) {
								objgovs.add(gov);
								objhead.add(j);
							}
						}
					}
				}
			}
			int[] res = new int[objgovs.size()];
			for (int i=0;i<res.length;i++) res[i] = i;
			return res;
		}
		public int[] applyRule(DetGraph g, int p) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        SemanticProcesing.isFrench=true;
			int gov=objgovs.get(p);
			int d=g.getDep(gov);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("OBJ", gov, objhead.get(p));
                        if (SemanticProcesing.isPredicate(g, objhead.get(p))){
                            int[] depsWithPos=gsrl.getDeps(gov);
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==objhead.get(p)) gsrl.removeDep(dd); break;
                            }
                            gsrl.ajoutDep("A1", gov, objhead.get(p));
                        }
			int[] changed = {gov};
			return changed;
		}
	}
	// =================================================================
	class atts implements CompRulesFr.SingleRule {
		// same as obj, but put ATTS with some verbs
		public String getName() {return "atts";}
		ArrayList<Integer> objgovs = new ArrayList<Integer>();
		ArrayList<Integer> objhead = new ArrayList<Integer>();
		public int[] getApplicable(DetGraph g) {
			objgovs.clear(); objhead.clear();
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gov = m.getIndexInUtt()-1;
					for (int j=gov-1;j>=0;j--) {
						String lem = g.getMot(j).getLemme().toLowerCase();
						if (lem.equals("être")||lem.equals("paraître")||lem.equals("devenir")) {
							if (!checkDepExists(g, gov)) {
								objgovs.add(gov);
								objhead.add(j);
							}
						}
					}
				}
			}
			int[] res = new int[objgovs.size()];
			for (int i=0;i<res.length;i++) res[i] = i;
			return res;
		}
		public int[] applyRule(DetGraph g, int p) {
			int gov=objgovs.get(p);
			int d=g.getDep(gov);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ATTS", gov, objhead.get(p));
			int[] changed = {gov};
			return changed;
		}
	}

	// =================================================================
	class rel implements CompRulesFr.SingleRule {
		public String getName() {return "rel";}

		public int[] getApplicable(DetGraph g) {
			final String[] rels = {"dont","où","qu'","que","qui","quoi"};
			ArrayList<Integer> pp = new ArrayList<Integer>();
			// TODO: traiter "quoi" (quoi qu'il fasse, ...)
			for (int pronompos=1;pronompos<g.getNbMots();pronompos++) {
				if (checkDepExists(g, pronompos)) continue;
				String pronom = g.getMot(pronompos).getForme().toLowerCase();
				if (Arrays.binarySearch(rels, pronom)>=0) {
					if (isGN(g, pronompos-1)||g.getMot(pronompos-1).getPOS().startsWith("PRO")) {
						for (int j=pronompos+1;j<g.getNbMots();j++) {
							if (g.getMot(j).getPOS().startsWith("V")) {
								if (g.getMot(j).getLemme().equals("avoir")||g.getMot(j).getLemme().equals("être")) {
									// on peut lier au 1er ou au 2eme verbe
									for (int k=j+1;k<g.getNbMots();k++) {
										if (g.getMot(k).getPOS().startsWith("V")) {
											pp.add(-pronompos);break;
										}
									}
								}
								pp.add(pronompos);
								break;
							}
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int pronompos) {
			boolean secondVerb = false;
			if (pronompos<0) {secondVerb=true;pronompos=-pronompos;}
			String w = g.getMot(pronompos).getForme().toLowerCase();
			String lab = "OBJ";
			String wbef = g.getMot(pronompos-1).getForme().toLowerCase();
			if (wbef.equals("à")) lab="POBJ";
			else if (w.equals("qui")) lab="SUJ";
			int verb;
			for (verb=pronompos+1;verb<g.getNbMots();verb++) {
				if (g.getMot(verb).getPOS().startsWith("V")) {
					if (secondVerb) {secondVerb=false; continue;}
					g.ajoutDep(lab, pronompos, verb);
					break;
				}
			}
			if (isGN(g,pronompos-1))
				for (int nom = pronompos-1;nom>=0;nom--) {
					if (isGNHead(g, nom)) {
						g.ajoutDep("MOD", verb, nom);
						break;
					}
				}
			else {
				g.ajoutDep("MOD", verb, pronompos-1);
			}
			int[] changed = {pronompos,verb};
			return changed;
		}
	}

	// =================================================================
	class aux implements CompRulesFr.SingleRule {
		public String getName() {return "aux";}

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++) {
				String l = g.getMot(i).getLemme();
				if (l.equals("être")||l.equals("avoir")) {
					int j=i+1;
					// on autorise les PRO:PER entre l'aux et le verbe a cause des inversions sujet/verbe
					while (j<g.getNbMots()&&(g.getMot(j).getPOS().startsWith("ADV")||g.getMot(j).getPOS().startsWith("CLS"))) j++;
//					if (j<g.getNbMots()&&(g.getMot(j).getPOS().equals("VER:pper"))) {
					if (j<g.getNbMots()&&(g.getMot(j).getPOS().equals("V"))) {
						// a la difference des autres "rules", j'autorise AUX sur des verbes qui ont deja des liens !
						if (!isDepAlreadyThere(g, i, j, "AUX")) {
							if (!isNewDep(g, i)&&!isNewDep(g, j)) {
								// mais il ne faut pas qu'il y ait des arcs partants de l'aux et du verbe principal ! 
							} else pp.add(i);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int auxpos) {
			int v=auxpos+1;
			while (g.getMot(v).getPOS().startsWith("ADV")||g.getMot(v).getPOS().startsWith("CLS")) v++;
			if (g.getMot(v).getLemme().equals("être")&&v+1<g.getNbMots()&&g.getMot(v+1).getPOS().equals("V")) {  //VER:pper
				// on a un AUX + un PASSIF (avec etre)
				g.ajoutDep("AUX", auxpos, v+1);
				g.ajoutDep("AUX", v, v+1);
				int[] changed = {auxpos,v};
				return changed;
			}
			int d=g.getDep(auxpos);
			if (d>=0) {
				// deplace l'arc partant de AUX
				g.deps.get(d).gov=g.getMot(v);
				g.ajoutDep("AUX", auxpos, v);
				int[] changed = {auxpos,v};
				return changed;
			} else {
				g.ajoutDep("AUX", auxpos, v);
				int[] changed = {auxpos};
				return changed;
			}
		}
	}

	// =================================================================
	class modA implements CompRulesFr.SingleRule {
		public String getName() {return "modA";}
		// link any GNHEAD to the next verb as MOD
		// TODO: do the same with the previous verb, with other nouns...
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			if (g.groupnoms==null) {
				int[] r = {}; return r;
			}
			for (int z=0;z<g.groupnoms.size();z++) {
				if (g.groupnoms.get(z).equals("GNHEAD")) {
					{
						// next
						int npos = g.groups.get(z).get(0).getIndexInUtt()-1;
						int d = g.getDep(npos);
						if (d>=0) continue;
						for (int i=npos+1;i<g.getNbMots();i++) {
							if (g.getMot(i).getPOS().startsWith("V")) {
								pp.add(npos);
								break;
							}
						}
					}
					{
						// prev
						int npos = g.groups.get(z).get(0).getIndexInUtt()-1;
						int d = g.getDep(npos);
						if (d>=0) continue;
						for (int i=npos-1;i>=0;i--) {
							if (g.getMot(i).getPOS().startsWith("V")) {
								pp.add(-npos);
								break;
							}
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int npos) {
			if (npos>=0) {
				for (int i=npos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("V")) {
						g.ajoutDep("MOD", npos, i);
						int[] r = {npos};
						return r;
					}
				}
			} else {
				npos=-npos;
				for (int i=npos-1;i>=0;i--) {
					if (g.getMot(i).getPOS().startsWith("V")) {
						g.ajoutDep("MOD", npos, i);
						int[] r = {npos};
						return r;
					}
				}
			}
			int[] r = {}; return r;
		}
	}

	// =================================================================
	class heure implements CompRulesFr.SingleRule {
		// regle specifique importante pour ESTER
		@Override
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=1;i<g.getNbMots();i++) {
				if (g.getMot(i).getForme().toLowerCase().startsWith("heure")) {
					if (Character.isDigit(g.getMot(i-1).getForme().charAt(0)) || g.getMot(i-1).getForme().toLowerCase().equals("une")) {
						pp.add(i);
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		@Override
		public int[] applyRule(DetGraph g, int pos) {
			ArrayList<Integer> modified = new ArrayList<Integer>();

			// forme "5 heures" 
			int d=g.getDep(pos-1);
			if (d<0) {
				g.ajoutDep("DET", pos-1, pos);
				modified.add(pos-1);
			}
			// ? = forme "5 heures 20"
			if (pos<g.getNbMots()-1 && Character.isDigit(g.getMot(pos+1).getForme().charAt(0))) {
				d=g.getDep(pos+1);
				if (d<0) {
					g.ajoutDep("MOD", pos+1, pos);
					modified.add(pos+1);
				}
			} // TODO: "5 heures et demi"

			// ? = forme "à 7 heures"
			if (pos-2>=0&&g.getMot(pos-2).getForme().toLowerCase().equals("à")) {
				d=g.getDep(pos);
				if (d<0) {
					g.ajoutDep("COMP", pos, pos-2);
					modified.add(pos);
				}
				{
					// lie ce groupe au 1er verbe de la phrase
					boolean verb=false;
					for (int i=0;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("V")) {
							g.ajoutDep("MOD", pos-2, i);
							modified.add(pos-2);
							verb=true; break;
						}
					}
					if (!verb) {
						// sinon lie au premier nom avant ou 1er nom apres
						boolean nombef = false;
						for (int i=pos-1;i>=0;i--) {
							if (g.getGroupNoms(i).contains("GNHEAD")) {
								g.ajoutDep("JUXT", pos-2, i);
								modified.add(pos-2);
								nombef=true; break;
							}
						}
						if (!nombef) {
							for (int i=pos+1;i<g.getNbMots();i++) {
								if (g.getGroupNoms(i).contains("GNHEAD")) {
									g.ajoutDep("MOD", pos-2, i);
									modified.add(pos-2);
									break;
								}
							}
						}
					}
				}
			}

			int[] modi = new int[modified.size()];
			for (int i=0;i<modi.length;i++) modi[i] = modified.get(i);
			return modi;
		}

		@Override
		public String getName() {return "heure";}
	}

	// =================================================================
	class det implements CompRulesFr.SingleRule {
		public String getName() {return "det";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				if (!g.groupnoms.get(i).startsWith("GNW")) continue;
				List<Mot> ms = g.groups.get(i);
				int ghead = -1;
				for (int j=0;j<ms.size();j++) {
					Mot m=ms.get(j);
					if (g.getGroupNoms(m.getIndexInUtt()-1).contains("GNHEAD")) {
						ghead=j; break;
					}
				}
				if (ghead>=0) {
					int j;
					for (j=ghead-1;j>=0;j--) {
						String pos = ms.get(j).getPOS();
//						if (pos.startsWith("DET")||pos.startsWith("PRP:det")||pos.startsWith("PRO:IND")||pos.startsWith("PRO:DEM")) {
						if (pos.startsWith("DET")||pos.startsWith("P+D")||pos.startsWith("ADJ")||pos.startsWith("PRO")) {
							pp.add(ms.get(ghead).getIndexInUtt()-1);
							break;
						}
					}
					if (j<0) {
						// pas trouve: peut-etre un NUM ?
						for (j=ghead-1;j>=0;j--) {
							String pos = ms.get(j).getPOS();
//							if (pos.startsWith("NUM")) {
							if (pos.startsWith("NC")) {
								pp.add(ms.get(ghead).getIndexInUtt()-1);
								break;
							}
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int npos) {
			int[] grps = g.getGroups(npos);
			int gr = -1;
			for (int gg : grps)
				if (g.groupnoms.get(gg).startsWith("GNW")) {gr=gg; break;}
			int deb=g.getGroupFirstMot(gr).getIndexInUtt()-1;
			int j;
			for (j=npos-1;j>=deb;j--) {
				String pos = g.getMot(j).getPOS();
				//if (pos.startsWith("DET")||pos.startsWith("PRP:det")||pos.startsWith("PRO:IND")||pos.startsWith("PRO:DEM")) {
                                if (pos.startsWith("DET")||pos.startsWith("P+D")||pos.startsWith("ADJ")||pos.startsWith("PRO")) {
					g.ajoutDep("DET", j, npos);
					int[] r = {j};
					return r;
				}
			}
			if (j<deb) {
				// pas trouve: peut-etre un NUM ?
				for (j=npos-1;j>=0;j--) {
					String pos = g.getMot(j).getPOS();
					//if (pos.startsWith("NUM")) {
                                        if (pos.startsWith("NC")) {
						g.ajoutDep("DET", j, npos);
						int[] r = {j};
						return r;
					}
				}
			}
			return null;
		}
	}

	// =================================================================
	class modB implements CompRulesFr.SingleRule {
		public String getName() {return "modB";}
		// link any GNHEAD to the next or previous noun as MOD
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			if (g.groupnoms==null) {
				int[] r = {}; return r;
			}
			for (int z=0;z<g.groupnoms.size();z++) {
				if (g.groupnoms.get(z).equals("GNHEAD")) {
					{
						// next
						int npos = g.groups.get(z).get(0).getIndexInUtt()-1;
						int d = g.getDep(npos);
						if (d>=0) continue;
						for (int i=npos+1;i<g.getNbMots();i++) {
							//if (g.getMot(i).getPOS().startsWith("NOM")||g.getMot(i).getPOS().startsWith("NAM")) {
                                                        if (g.getMot(i).getPOS().startsWith("NC")||g.getMot(i).getPOS().startsWith("NPP")) {
								pp.add(npos);
								break;
							}
						}
					}
					{
						// prev
						int npos = g.groups.get(z).get(0).getIndexInUtt()-1;
						int d = g.getDep(npos);
						if (d>=0) continue;
						for (int i=npos-1;i>=0;i--) {
							//if (g.getMot(i).getPOS().startsWith("NOM")||g.getMot(i).getPOS().startsWith("NAM")) {
                                                        if (g.getMot(i).getPOS().startsWith("NC")||g.getMot(i).getPOS().startsWith("NPP")) {
								pp.add(-npos);
								break;
							}
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int npos) {
			if (npos>=0) {
				for (int i=npos+1;i<g.getNbMots();i++) {
					//if (g.getMot(i).getPOS().startsWith("NOM")||g.getMot(i).getPOS().startsWith("NAM")) {
                                        if (g.getMot(i).getPOS().startsWith("NC")||g.getMot(i).getPOS().startsWith("NPP")) {
						g.ajoutDep("MOD", npos, i);
						int[] r = {npos};
						return r;
					}
				}
			} else {
				npos=-npos;
				for (int i=npos-1;i>=0;i--) {
					//if (g.getMot(i).getPOS().startsWith("NOM")||g.getMot(i).getPOS().startsWith("NAM")) {
                                        if (g.getMot(i).getPOS().startsWith("NC")||g.getMot(i).getPOS().startsWith("NPP")) {
						g.ajoutDep("MOD", npos, i);
						int[] r = {npos};
						return r;
					}
				}
			}
			int[] r = {}; return r;
		}
	}

	// =================================================================
	class root implements CompRulesFr.SingleRule {
		public String getName() {return "ROOT";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			// is there any verb ?
			for (int i=0;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().startsWith("V")) {
					pp.add(i);
				}
			}
			if (pp.size()==0) {
				for (int i=0;i<g.getNbGroups();i++) {
					if (!g.groupnoms.get(i).startsWith("GNHEAD")) continue;
					pp.add(g.groups.get(i).get(0).getIndexInUtt()-1);
				}				
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int npos) {
			int d=g.getDep(npos);
			if (d>0) g.removeDep(d);
			g.ajoutDep("ROOT", npos, npos);
			int[] r = {npos};
			return r;
		}
	}

}
