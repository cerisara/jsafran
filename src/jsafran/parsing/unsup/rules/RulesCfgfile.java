package jsafran.parsing.unsup.rules;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.Mot;
import jsafran.parsing.unsup.UnsupParsing;
import jsafran.searchfilters.TreeSearch;
import jsafran.searchfilters.TreeSearchCallback;

/**
 * stores rules that follows the syntax of the search/replace JSafran language
 * 
 * @author xtof
 *
 */
public class RulesCfgfile implements Rules, Serializable {
	private static final long serialVersionUID = 1L;
	//hardcoded variable to set english or french !!!!!!!!!!!!!!!!!!!!! (shouldn't it be in the main call?)
	public static boolean ENGLISH=true;
        boolean srl=false;

	ArrayList<String> rules = new ArrayList<String>();
	ArrayList<Character> ruleRank = new ArrayList<Character>();
	CompRules compRules = new CompRules();
	EndRules endRules = new EndRules();
        CompRulesEng compRulesEng= new CompRulesEng();
        CompRulesFr compRulesFr= new CompRulesFr();
	/**
	 * cette fonction ne sert pas a appliquer les regles avec une init. random, ou vide,
	 * mais seulement a configurer le classe !
	 * (ou comme ici a faire une 1ere etape de pre-traitement des corpus en GN...)
	 * Pour l'initialisation de Gibbs, voir sampleAll()
	 */
	public void init(List<DetGraph> gs) {
            //english
            if (ENGLISH){
                compRulesEng.init(gs);
                compRulesEng.setSRL(srl);
            }
            else {//french
                if (srl){
                    compRulesFr.init(gs);
                    endRules.init(gs);
                    GNstepFrSRL(gs);
                    
                }else{
                    compRules.init(gs);
                    endRules.init(gs);
                    GNstep(gs);
                }
            }
	}

        @Override
        public void setSRL(boolean s){
            srl=s;
            compRulesEng.setSRL(srl);
        };

	public double getRulePrior(int r) {
                //english
                if (ENGLISH) return compRulesEng.getRulePrior(r);
                //french
		if (UnsupParsing.giter>40 && r>=rules.size()+compRules.getNrules()) return endRules.getRulePrior(r-rules.size()-compRules.getNrules());
		if (r>=rules.size()) return compRules.getRulePrior(r-rules.size());

		switch(ruleRank.get(r)) {
		case 'A': return 1;
		case 'B': return 0.8;
		case 'C': return 0.5;
		default: return 0.1;
		}
	}
	
	public RulesCfgfile(boolean srll) {
                srl=srll;
                if (!ENGLISH) //only load rules from the config file when it's for french
                {
                    final String configFile = "jsynats.cfg";
                    try {
                            BufferedReader f= new BufferedReader(new InputStreamReader(new FileInputStream(configFile),Charset.forName("UTF-8")));
                            for (;;) {
                                    String s = f.readLine();
                                    if (s==null) break;
                                    if (s.startsWith("RULE")) {
                                            char rk = s.charAt(4);
                                            if (rk==' ') {
                                                    rules.add(s.substring(5));
                                                    ruleRank.add('A');
                                            } else {
                                                    rules.add(s.substring(6));
                                                    ruleRank.add(rk);
                                            }
                                    }
                            }
                            f.close();
                    } catch (IOException e) {
                            e.printStackTrace();
                    }
                    createRules();
                }
	}
	
	ArrayList<TreeSearch> tss = new ArrayList<TreeSearch>();
	// contient l'indice du MATCH qu'il faut appliquer (ou -1)
	int doApply = -1;
	// contient l'indice du prochain MATCH
	int matchIdx = 0;
	// contient la position absolue de dep0 pour chaque match
	ArrayList<Integer> matchPos = new ArrayList<Integer>();
	
	public String getName(int i) {return rules.get(i);}
	
	private void createRules() {
		tss.clear();
		for (int i=0;i<rules.size();i++) {
			String rule = rules.get(i);
			final int j=i;
			TreeSearch ts = TreeSearch.create(rule, new TreeSearchCallback() {
				@Override
				public boolean callbackOneMatch(DetGraph g, HashMap<String, Object> vars) {
					if (doApply<0) {
						// ceci verifie si on change le graphe...
						if (tss.get(j).checkNewDeps(g, vars)) {
							// ... mais si on detruit une dep precedente (imposee par GN), il ne faut pas !
							String[] ruleelts = rules.get(j).split(" ");
							boolean destroyPrevDep = false;
							for (String re: ruleelts) {
								if (re.charAt(0)=='-') {
									Object ow = vars.get(re.substring(1));
									if (ow==null) break;
									Integer iw = (Integer)ow;
									int d = g.getDep(iw);
									if (d>=0) {destroyPrevDep=true; break;}
								}
							}
							if (!destroyPrevDep) {
								matchIdx++;
								matchPos.add((Integer)vars.get("dep0"));
							}
						}
						// return false signifie: ne pas appliquer la partie transfo "=> ..."
						return false;
					} else {
						if (tss.get(j).checkNewDeps(g, vars)) {
							String[] ruleelts = rules.get(j).split(" ");
							boolean destroyPrevDep = false;
							for (String re: ruleelts) {
								if (re.charAt(0)=='-') {
									Object ow = vars.get(re.substring(1));
									if (ow==null) break;
									Integer iw = (Integer)ow;
									int d = g.getDep(iw);
									if (d>=0) {destroyPrevDep=true; break;}
								}
							}
							if (!destroyPrevDep) {
								int dep0 = (Integer)vars.get("dep0");
								matchPos.add(dep0);
								matchIdx++;
								if (dep0==doApply) {
									return true;
								}
							}
						}
						return false;
					}
				}
			});
			tss.add(ts);
		}
	}
	
	public int getNrules() {
            if (ENGLISH) return compRulesEng.getNrules();
            else {
                if (srl) return rules.size() + compRulesFr.getNrules() + endRules.getNrules();
                else return rules.size() + compRules.getNrules() + endRules.getNrules();
            }
	}
	private int[] getApplicableFrench(int ridx, DetGraph g) {
            if (srl){
                if (ridx>=rules.size()+compRulesFr.getNrules()) {
			if (UnsupParsing.giter>40) return endRules.getApplicable(ridx-rules.size()-compRulesFr.getNrules(),g);
			final int[] res = {};
			return res;
		}
		if (ridx>=rules.size()) return compRulesFr.getApplicable(ridx-rules.size(), g);
		doApply=-1; matchIdx=0; matchPos.clear();
		tss.get(ridx).onlyfirstmatch=false;
		tss.get(ridx).findAll(g, -1);

		// TODO: il ne faut pas que l'application de ces regles detruise une dep deja presente !!

		int[] res = new int[matchIdx];
		// il faut tjrs retourner position absolue de dep0 !!
		for (int i=0;i<res.length;i++) res[i]=matchPos.get(i);
		return res;
                
            }
            else{
                if (ridx>=rules.size()+compRules.getNrules()) {
			if (UnsupParsing.giter>40) return endRules.getApplicable(ridx-rules.size()-compRules.getNrules(),g);
			final int[] res = {};
			return res;
		}
		if (ridx>=rules.size()) return compRules.getApplicable(ridx-rules.size(), g);
		doApply=-1; matchIdx=0; matchPos.clear();
		tss.get(ridx).onlyfirstmatch=false;
		tss.get(ridx).findAll(g, -1);

		// TODO: il ne faut pas que l'application de ces regles detruise une dep deja presente !!

		int[] res = new int[matchIdx];
		// il faut tjrs retourner position absolue de dep0 !!
		for (int i=0;i<res.length;i++) res[i]=matchPos.get(i);
		return res;
            }
	}
	private int[] getApplicableEngl(int ridx, DetGraph g) {
		return compRulesEng.getApplicable(ridx, g);
	}

        public int[] getApplicable(int ridx, DetGraph g) {
            int [] res=ENGLISH? getApplicableEngl(ridx, g): getApplicableFrench(ridx, g);
            return res;
	}

	private int[] applyRuleFrench(int ridx, DetGraph g, final int pos) {
            if (srl){
		if (UnsupParsing.giter>40 && ridx>=rules.size()+compRulesFr.getNrules()) return endRules.applyRule(ridx-rules.size()-compRulesFr.getNrules(),g,pos);
		if (ridx>=rules.size()) return compRulesFr.applyRule(ridx-rules.size(), g, pos);

		doApply=pos; matchIdx=0; matchPos.clear();
		ArrayList<Dep> prevdeps = new ArrayList<Dep>();
		prevdeps.addAll(g.deps);
		tss.get(ridx).findAll(g, -1);
		TreeSet<Integer> newdeps = new TreeSet<Integer>();

		// TODO: on suppose W = _dépendants_ des deps créées mais il vaudrait mieux calculer des _features_ par rule
		// ... oui mais on comparerait alors des choses pas comparables !
		for (Dep d : g.deps)
			if (!prevdeps.contains(d)) {
				newdeps.add(d.gov.getIndexInUtt()-1);
			}
		int[] res = new int[newdeps.size()];
		int i=0;
		for (int w : newdeps) res[i++]=w;

		return res;
                
            }
            else{	
                if (UnsupParsing.giter>40 && ridx>=rules.size()+compRules.getNrules()) return endRules.applyRule(ridx-rules.size()-compRules.getNrules(),g,pos);
		if (ridx>=rules.size()) return compRules.applyRule(ridx-rules.size(), g, pos);

		doApply=pos; matchIdx=0; matchPos.clear();
		ArrayList<Dep> prevdeps = new ArrayList<Dep>();
		prevdeps.addAll(g.deps);
		tss.get(ridx).findAll(g, -1);
		TreeSet<Integer> newdeps = new TreeSet<Integer>();

		// TODO: on suppose W = _dépendants_ des deps créées mais il vaudrait mieux calculer des _features_ par rule
		// ... oui mais on comparerait alors des choses pas comparables !
		for (Dep d : g.deps)
			if (!prevdeps.contains(d)) {
				newdeps.add(d.gov.getIndexInUtt()-1);
			}
		int[] res = new int[newdeps.size()];
		int i=0;
		for (int w : newdeps) res[i++]=w;

		return res;
            }
	}

        //TODO: is it OK????????????????????????
	private int[] applyRuleEngl(int ridx, DetGraph g, final int pos) {
		return compRulesEng.applyRule(ridx, g, pos);
	}


        public int[] applyRule(int ridx, DetGraph g, final int pos) {
		int[] res = ENGLISH? applyRuleEngl(ridx, g, pos): applyRuleFrench(ridx, g, pos);
		return res;
	}
	
	public void applyDetRules(DetGraph g) {
            if (ENGLISH) {
                compRulesEng.applyDetRules(g);
            }
            else{
                if (srl){
                    if (UnsupParsing.giter>40) endRules.applyDetRules(g);
                    compRules.applyDetRules(g);
    //		detRule_AUX(g);
    //		detRule_ATTS(g);
                }else{
                    if (UnsupParsing.giter>40) endRules.applyDetRules(g);
                    compRulesFr.applyDetRules(g);
                }
            }
	}
	
	public void editRules() {
		// TODO
	}
	
	public String toString(int ridx) {
            if (ENGLISH){
                return compRulesEng.toString(ridx);
            }else{
		if (ridx<0) return "NORULE";
                if (srl){
                    if (UnsupParsing.giter>40 && ridx>=rules.size()+compRulesFr.getNrules()) return endRules.toString(ridx-rules.size()-compRulesFr.getNrules());
                    if (ridx>=rules.size()) return compRulesFr.toString(ridx-rules.size());
                }else{
                    if (UnsupParsing.giter>40 && ridx>=rules.size()+compRules.getNrules()) return endRules.toString(ridx-rules.size()-compRules.getNrules());
                    if (ridx>=rules.size()) return compRules.toString(ridx-rules.size());
                }
		return rules.get(ridx);
            }
	}
	
	// ===============================================
	// ============================    DETRULES
	
	void detRule_ATTS(DetGraph g) {
		for (Dep d : g.deps) {
			if (d.depnoms[d.type].equals("OBJ")) {
				String v = d.head.getLemme();
				if (v.equals("être")||v.equals("paraître")||v.equals("devenir")) d.type=Dep.getType("ATTS");
			}
		}
	}
	void detRule_AUX(DetGraph g) {
		HashMap<Mot, Mot> newheads = new HashMap<Mot, Mot>();
		for (Dep d : g.deps) {
			if (d.depnoms[d.type].equals("AUX")) {
				newheads.put(d.gov, d.head);
			}
		}
		if (newheads.size()==0) return;
		for (Mot head : newheads.keySet()) {
			Mot nh = newheads.get(head);
			for (Dep d : g.deps) {
				if (d.head==head) d.head=nh;
			}
		}
	}

	// des DETRULES qui completent les deps manquantes apres l'iteration 40
	void detRule_Remaining(DetGraph g) {
		if (UnsupParsing.giter<40) return;
		
		// look for the best ROOT candidate
		int root=-1;
		ArrayList<Integer> uttheadCandidates = new ArrayList<Integer>();
		for (int i=0;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d<0) uttheadCandidates.add(i);
		}
		if (uttheadCandidates.size()<=1) return;
		int goodVerbCandidate = -1, goodNounCandidate = -1;
		for (int w : uttheadCandidates) {
			if (goodVerbCandidate<0 && g.getMot(w).getPOS().startsWith("VER")) {
				// y a-t-il un PRO:REL devant ?
				boolean isInREL=false;
				for (int i=w-1;i>=0;i--) {
					if (isPronomRelatif(g.getMot(i))) {
						isInREL=true; break;
					}
					if (g.getMot(i).getPOS().startsWith("VER")) {
						break;
					}
				}
				if (!isInREL) goodVerbCandidate=w;
			}
			if (goodNounCandidate<0 && isNoun(g.getMot(w))) {
				goodNounCandidate=w;
			}
		}
		if (goodVerbCandidate>=0) root = goodVerbCandidate;
		else if (goodNounCandidate>=0) root=goodNounCandidate;
		else {
			// pas de root valable !
			root=uttheadCandidates.get(0);
		}
		
		// on lie les remaining...
		for (int w : uttheadCandidates) {
			if (w!=root) {
				g.ajoutDep("MOD", w, root);
			}
		}
	}
	
	public static boolean isNoun(Mot m) {
		final String[] noms = {"ABR","NAM","NOM"};
		if (Arrays.binarySearch(noms, m.getPOS())>=0) return true;
		return false;
	}
	
	public static boolean isPronomRelatif(Mot m) {
		final String[] prorel = {"dont","où","que","qui","quoi"};
		if (Arrays.binarySearch(prorel, m.getForme().toLowerCase())>=0) return true;
		return false;
	}
	
	// ======================================================
	// PRE-PROCESSING: identifie le GNs, et les HEADS de ces GNs
	// (les GNs ne sont pas recursifs, et ils n'ont aucun recouvrement)
	// le HEAD du GN est identifie par un groupe HEADGN
	
	void GNstep(List<DetGraph> gs) {
		
		{
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				if (g.getNbMots()>9) {
					for (int j=0;j<g.getNbGroups();j++)
						if (g.getGroupName(j).equals("GNHEAD")) return;
					break;
				}
			}
		}
		
		// TODO: traitement specifique des NUM qui posent probleme
		
		final String[] heads1 = {"ABR","NAM","NOM"};
		final String[] extendAfter = {"ADJ"};
		final String[] extendBeforePos = {"ADJ","ADV","NUM"};
		final String[] extendBeforeForms = {"aucun","aucune","ce","ces","cet","cette","des","euh","l'","la","le","les","leur","leurs","ma","mon","nos","notre","sa","son","ta","ton","tout","toute","un","une","vos","votre"};

		int ng=0;
		for (DetGraph g : gs) {
			// 1st step: detect "standard" GN
			for (int i=0;i<g.getNbMots();i++) {
				String pos = g.getMot(i).getPOS();
				if (Arrays.binarySearch(heads1, pos)>=0) {
					g.addgroup(i, i, "GNHEAD");
				}
			}
			
			// then extend the GNs up to its limits
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gnhead = m.getIndexInUtt()-1;
					
					// extend on the RIGHT
					int j;
					for (j=gnhead+1;j<g.getNbMots();j++) {
						String pos = g.getMot(j).getPOS();
						if (Arrays.binarySearch(extendAfter, pos)<0) break;
						int d=g.getDep(j);
						if (d<0) {
							g.ajoutDep("_MOD", j, gnhead);
						}
					}
					// extend on the LEFT
					int lastw = j-1;
					for (j=gnhead-1;j>=0;j--) {
						String pos = g.getMot(j).getPOS();
						if (Arrays.binarySearch(extendBeforePos, pos)<0) {
							String form = g.getMot(j).getForme().toLowerCase();
							if (Arrays.binarySearch(extendBeforeForms, form)<0) {
								break;
							} else {
								int d=g.getDep(j);
								if (d<0) {
									if (form.equals("euh")) {
										g.ajoutDep("_DISFL", j, j+1);
									} else {
										// si il y a d'autres DET qui vont vers des NUM, je les change en MOD
										List<Mot> fils = g.getFils(m);
										for (Mot mot : fils) {
											if (mot.getPOS().equals("NUM")) {
												int dd=g.getDep(mot.getIndexInUtt()-1);
												if (dd>=0) {
													if (g.getDepLabel(dd).equals("DET")) {
														g.removeDep(dd);
														g.ajoutDep("_MOD", mot.getIndexInUtt()-1, gnhead);
													}
												}
											}
										}
//										g.ajoutDep("_DET", j, gnhead);
									}
								}								
							}
						} else {
							int d=g.getDep(j);
							if (d<0) {
								if (pos.equals("ADJ"))
									g.ajoutDep("_MOD", j, gnhead);
								else if (pos.equals("NUM")) {
									// par defaut j'ajoute en DET, mais je reviendrai plus tard sur cette decision si c'est un NUM avec article devant
//									g.ajoutDep("_DET", j, gnhead);
								}
							}
						}
					}
					int firstw = j+1;
					
					g.addgroup(firstw, lastw, "GNW"+gnhead);
					ng++;
				}
			}
			
			// 2d step: les nombres peuvent etre GNHEAD s'ils ne sont pas deja inclus dans un GN
			
			// 3rd step: detection + link des GPs
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).startsWith("GNW")) {
					Mot m = g.getGroupFirstMot(i);
					int mi = m.getIndexInUtt()-1;
					if (mi>0) {
						--mi;
						if (g.getMot(mi).getPOS().startsWith("PRP")) {
							// il y a une prep devant un GN: on cherche le GNHEAD
							int gnhead = Integer.parseInt(g.getGroupName(i).substring(3));
							int d = g.getDep(gnhead);
							if (d<0) {
								g.ajoutDep("_COMP", gnhead, mi);
							}
						}
					}
				}
			}
		}
		System.out.println("created "+ng+" groups");
		
	}

	// ======================================================
	// PRE-PROCESSING: identifie le GNs, et les HEADS de ces GNs
	// (les GNs ne sont pas recursifs, et ils n'ont aucun recouvrement)
	// le HEAD du GN est identifie par un groupe HEADGN
	
	void GNstepFrSRL(List<DetGraph> gs) {
		
		{
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				if (g.getNbMots()>9) {
					for (int j=0;j<g.getNbGroups();j++)
						if (g.getGroupName(j).equals("GNHEAD")) return;
					break;
				}
			}
		}
		
		// TODO: traitement specifique des NUM qui posent probleme
		
//		final String[] heads1 = {"ABR","NAM","NOM"};
//		final String[] extendAfter = {"ADJ"};
//		final String[] extendBeforePos = {"ADJ","ADV","NUM"};
//		final String[] extendBeforeForms = {"aucun","aucune","ce","ces","cet","cette","des","euh","l'","la","le","les","leur","leurs","ma","mon","nos","notre","sa","son","ta","ton","tout","toute","un","une","vos","votre"};
		final String[] heads1 = {"NC","NPP"}; //todo see what is ABR
		final String[] extendAfter = {"ADJ"};
		final String[] extendBeforePos = {"ADJ","ADV","NC"};
		final String[] extendBeforeForms = {"aucun","aucune","ce","ces","cet","cette","des","euh","l'","la","le","les","leur","leurs","ma","mon","nos","notre","sa","son","ta","ton","tout","toute","un","une","vos","votre"};

		int ng=0;
		for (DetGraph g : gs) {
			// 1st step: detect "standard" GN
			for (int i=0;i<g.getNbMots();i++) {
				String pos = g.getMot(i).getPOS();
				if (Arrays.binarySearch(heads1, pos)>=0) {
					g.addgroup(i, i, "GNHEAD");
				}
			}
			
			// then extend the GNs up to its limits
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).equals("GNHEAD")) {
					Mot m = g.getGroupFirstMot(i);
					int gnhead = m.getIndexInUtt()-1;
					
					// extend on the RIGHT
					int j;
					for (j=gnhead+1;j<g.getNbMots();j++) {
						String pos = g.getMot(j).getPOS();
						if (Arrays.binarySearch(extendAfter, pos)<0) break;
						int d=g.getDep(j);
						if (d<0) {
							g.ajoutDep("_MOD", j, gnhead);
						}
					}
					// extend on the LEFT
					int lastw = j-1;
					for (j=gnhead-1;j>=0;j--) {
						String pos = g.getMot(j).getPOS();
						if (Arrays.binarySearch(extendBeforePos, pos)<0) {
							String form = g.getMot(j).getForme().toLowerCase();
							if (Arrays.binarySearch(extendBeforeForms, form)<0) {
								break;
							} else {
								int d=g.getDep(j);
								if (d<0) {
									if (form.equals("euh")) {
										g.ajoutDep("_DISFL", j, j+1);
									} else {
										// si il y a d'autres DET qui vont vers des NUM, je les change en MOD
										List<Mot> fils = g.getFils(m);
										for (Mot mot : fils) {
											if (mot.getPOS().equals("NC")) {
												int dd=g.getDep(mot.getIndexInUtt()-1);
												if (dd>=0) {
													if (g.getDepLabel(dd).equals("DET")) {
														g.removeDep(dd);
														g.ajoutDep("_MOD", mot.getIndexInUtt()-1, gnhead);
													}
												}
											}
										}
//										g.ajoutDep("_DET", j, gnhead);
									}
								}								
							}
						} else {
							int d=g.getDep(j);
							if (d<0) {
								if (pos.equals("ADJ"))
									g.ajoutDep("_MOD", j, gnhead);
								else if (pos.equals("NC")) {
									// par defaut j'ajoute en DET, mais je reviendrai plus tard sur cette decision si c'est un NUM avec article devant
//									g.ajoutDep("_DET", j, gnhead);
								}
							}
						}
					}
					int firstw = j+1;
					
					g.addgroup(firstw, lastw, "GNW"+gnhead);
					ng++;
				}
			}
			
			// 2d step: les nombres peuvent etre GNHEAD s'ils ne sont pas deja inclus dans un GN
			
			// 3rd step: detection + link des GPs
			for (int i=0;i<g.getNbGroups();i++) {
				if (g.getGroupName(i).startsWith("GNW")) {
					Mot m = g.getGroupFirstMot(i);
					int mi = m.getIndexInUtt()-1;
					if (mi>0) {
						--mi;
						if (g.getMot(mi).getPOS().startsWith("P")) {
							// il y a une prep devant un GN: on cherche le GNHEAD
							int gnhead = Integer.parseInt(g.getGroupName(i).substring(3));
							int d = g.getDep(gnhead);
							if (d<0) {
								g.ajoutDep("_COMP", gnhead, mi);
							}
						}
					}
				}
			}
		}
		System.out.println("created "+ng+" groups");
		
	}
	
}
        
        

