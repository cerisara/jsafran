package jsafran.parsing.unsup.joint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.ClassificationResult;
import jsafran.parsing.unsup.UnsupParsing;
import jsafran.parsing.unsup.joint.RulesDepthFirst.UserRule;

/**
 * We reuse for now the old implementation of rules, and use the class WSJRules as a "facade" to access all individual rules behind it.
 * From the point of view of the search, there is thus a single rule that is active = WSJRules
 * 
 * impossible de refaire comme avant, car avant, j'avais:
 * 1- appliquer toutes les regles lors de l'init
 * 2- sampler 1 seule regle applicable, soit l'appliquer, soit la desapliquer, soit ne rien modifier
 * 
 * TODO: But we should definitively extract each individual rule from this interface, maybe one by one...
 * 
 * @author xtof
 *
 */
public class WSJRules implements UserRule {

	int[] chunks = null;
	int sentHead;
	// save, pour chaque graphe du corpus, sa version "collins only"
	HashMap<DetGraph, DetGraph> graphsBaseline = new HashMap<DetGraph, DetGraph>();
	
	int[] getNewDeps(DetGraph gsrc, DetGraph gdst) {
		ArrayList<Integer> diffs = new ArrayList<Integer>();
		for (int i=0;i<gsrc.getNbMots();i++) {
			int d1=gsrc.getDep(i);
			int h1=d1>=0?gsrc.getHead(d1):-1;
			String l1=d1>=0?gsrc.getDepLabel(d1):null;
			int d2=gdst.getDep(i);
			int h2=d2>=0?gdst.getHead(d2):-1;
			String l2=d2>=0?gdst.getDepLabel(d2):null;
			if (h1!=h2||l1!=l2) diffs.add(i);
		}
		int[] res = new int[diffs.size()];
		for (int i=0;i<res.length;i++) res[i]=diffs.get(i);
		return res;
	}
	
	private interface OldRuleInterface {
		public boolean applicable(DetGraph g, int pos);
		// return list of deps destroyed
		// do not need to check that it is applicable: when it is called, we assume it is
		public void apply(DetGraph g, int pos);
		public String toString();
	}
	
	void unApply(OldRuleInterface r, int w, DetGraph g) {
		DetGraph g0 = graphsBaseline.get(g).getSubGraph(0);
		r.apply(g0, w);
		int[] changedWords = getNewDeps(graphsBaseline.get(g),g0);
		unApplyDeps(g,graphsBaseline.get(g),changedWords);
	}
	
	void unApplyDeps(DetGraph gdst, DetGraph gsrc, int[] changedWords) {
		for (int w: changedWords) {
			int d=gdst.getDep(w);
			if (d>=0) gdst.removeDep(d);
			d=gsrc.getDep(w);
			if (d>=0) {
				gdst.ajoutDep(gsrc.getDepLabel(d), w, gsrc.getHead(d));
			}
		}
	}

	public void initGraph(DetGraph g) {
		g.clearDeps();
		// identify non-recursive NP
		chunks = new int[g.getNbMots()];
		Arrays.fill(chunks,-1);
		for (int i=0;i<g.getNbMots();i++) {
			String pos = g.getMot(i).getPOS();
			// all nouns, pronouns and possessive markers are part of an NP
			if (pos.startsWith("NN")||pos.startsWith("VBG")||pos.startsWith("PRP")||pos.startsWith("WP")||pos.startsWith("POS")||pos.startsWith("CD")) {
				chunks[i]=0;
			}
		}
		for (int i=g.getNbMots()-2;i>=0;i--) {
			String pos = g.getMot(i).getPOS();
			// all adjectives, conjunctions and determiners immediately preceding an NP are part of the NP
			if (pos.startsWith("JJ")||pos.startsWith("CC")||pos.startsWith("DT")||pos.startsWith("CD")) {
				if (chunks[i+1]>=0) chunks[i]=chunks[i+1];
			}
		}
		// xtof: NPs boundaries ??
		int npidx=0;
		for (int i=0;i<g.getNbMots();i++) {
			if (chunks[i]>=0) {
				chunks[i]=npidx;
			} else if (i>0 && chunks[i-1]>=0) npidx++;
		}

		// mark NP as groups
		for (int i=0;i<g.getNbMots();i++) {
			if (chunks[i]>=0) {
				int j=i+1;
				for (;j<g.getNbMots();j++) {
					if (chunks[j]!=chunks[i]) break;
				}
				g.addgroup(i, j-1, "GN");
				i=j-1;
			}
		}

		// the first verb or modal in the sentence is the headword
		sentHead=-1;
		for (int i=0;i<g.getNbMots();i++) {
			String pos = g.getMot(i).getPOS();
			if (pos.startsWith("VB")||pos.startsWith("MD")) {sentHead=i; break;}
		}
		if (sentHead<0) {
			// sinon, premier head d'un NP sans PREP devant
			// TODO
		}
		// all words in an NP are headed by the last word in the NP
		for (int i=0;i<g.getNbMots();i++) {
			if (chunks[i]>=0) {
				int lastNPword = i;
				for (int j=i+1;j<g.getNbMots();j++) {
					if (chunks[j]==chunks[i]) lastNPword=j;
				}
				if (lastNPword>i) g.ajoutDep("NMOD", i, lastNPword);
				int d = g.getDep(lastNPword);
				if (d<0) {
					// the last word in an NP is headed by word immediately before the NP if it is a preposition
					int precedingWord = -1;
					for (int j=lastNPword-1;j>=0;j--)
						if (chunks[j]!=chunks[lastNPword]) {precedingWord= j; break;}
					if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN"))
						g.ajoutDep("PMOD", lastNPword, precedingWord);
					else {
						// otherwise, it is headed by the headword of the sentence if the NP is before the headword
						if (sentHead>=0&&sentHead>lastNPword) {
							g.ajoutDep("SBJ", lastNPword, sentHead);
						} else {
							// else it is headed by the word preceding the NP
							if (precedingWord>=0) {
								if (g.getMot(precedingWord).getPOS().startsWith("VB"))
									g.ajoutDep("OBJ", lastNPword, precedingWord);
								else g.ajoutDep("DEP", lastNPword, precedingWord);
							}
						}
					}
				}
			}
		}
		// for the first word, set its head to the headword of the sentence
		int d=g.getDep(0);
		if (d<0) {
			// TODO: affiner
			if (sentHead>0) g.ajoutDep("DEP", 0, sentHead);
		}
		// for each other word, set its head to be the previous word
		for (int i=1;i<g.getNbMots();i++) {
			if (i==sentHead) continue;
			d=g.getDep(i);
			if (d<0) {
				// TODO: affiner
				g.ajoutDep("DEP", i, i-1);
			}
		}
		
		DetGraph gg = graphsBaseline.get(g);
		if (gg==null) {
			gg = g.getSubGraph(0);
			graphsBaseline.put(g, gg);
		}
	}

	public List<OldRuleInterface> initRules() {
		ArrayList<OldRuleInterface> rules = new ArrayList<OldRuleInterface>();
		OldRuleInterface rPOS = new OldRuleInterface() {
			public String toString() {return "SUFFIX";}
			@Override
			public void apply(DetGraph g, int pos) {
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("SUFFIX", pos, pos-1);
			}

			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos>0&&g.getMot(pos).getPOS().startsWith("POS")) {
					return true;
				}
				return false;
			}
		};

		OldRuleInterface rCOORD1 = new OldRuleInterface() {
			public String toString() {return "coord1";}
			@Override
			public void apply(DetGraph g, int pos) {
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("COORD", pos, pos-1);
				String posl = g.getMot(pos-1).getPOS();
				if (posl.length()>2) posl=posl.substring(0,2);
				for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
					String posr = g.getMot(i).getPOS();
					if (posr.length()>2) posr=posr.substring(0,2);
					if (posr.equals(posl)) {
						d = g.getDep(i);
						if (d>=0) g.removeDep(d);
						g.ajoutDep("CONJ", i, pos);
						return;
					}
				}
			}
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos==0) return false;
				if (!g.getMot(pos).getPOS().startsWith("CC")) return false;
				// cherche si il y a le meme POS a droite
				String posl = g.getMot(pos-1).getPOS();
				if (posl.length()>2) posl=posl.substring(0,2);
				for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
					String posr = g.getMot(i).getPOS();
					if (posr.length()>2) posr=posr.substring(0,2);
					if (posr.equals(posl)) return true;
				}
				return false;
			}
		};
		OldRuleInterface rCOORD2 = new OldRuleInterface() {
			public String toString() {return "coord2";}
			@Override
			public void apply(DetGraph g, int pos) {
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("COORD", pos, pos-2);
				String posl = g.getMot(pos-2).getPOS();
				if (posl.length()>2) posl=posl.substring(0,2);
				for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
					String posr = g.getMot(i).getPOS();
					if (posr.length()>2) posr=posr.substring(0,2);
					if (posr.equals(posl)) {
						d = g.getDep(i);
						if (d>=0) g.removeDep(d);
						g.ajoutDep("CONJ", i, pos);
						return;
					}
				}
			}
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos<2) return false;
				if (!g.getMot(pos).getPOS().startsWith("CC")) return false;
				// cherche si il y a le meme POS a droite
				String posl = g.getMot(pos-2).getPOS();
				if (posl.length()>2) posl=posl.substring(0,2);
				for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
					String posr = g.getMot(i).getPOS();
					if (posr.length()>2) posr=posr.substring(0,2);
					if (posr.equals(posl)) return true;
				}
				return false;
			}
		};
		OldRuleInterface rCOORD3 = new OldRuleInterface() {
			public String toString() {return "coord3";}
			@Override
			public void apply(DetGraph g, int pos) {
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("COORD", pos, pos-3);
				String posl = g.getMot(pos-3).getPOS();
				if (posl.length()>2) posl=posl.substring(0,2);
				for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
					String posr = g.getMot(i).getPOS();
					if (posr.length()>2) posr=posr.substring(0,2);
					if (posr.equals(posl)) {
						d = g.getDep(i);
						if (d>=0) g.removeDep(d);
						g.ajoutDep("CONJ", i, pos);
						return;
					}
				}
			}
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos<3) return false;
				if (!g.getMot(pos).getPOS().startsWith("CC")) return false;
				// cherche si il y a le meme POS a droite
				String posl = g.getMot(pos-3).getPOS();
				if (posl.length()>2) posl=posl.substring(0,2);
				for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
					String posr = g.getMot(i).getPOS();
					if (posr.length()>2) posr=posr.substring(0,2);
					if (posr.equals(posl)) return true;
				}
				return false;
			}
		};

		OldRuleInterface rSUJRIGHT = new  OldRuleInterface() {
			// lie un NN au premier verbe à droite
			public String toString() {return "sujet";}
			@Override
			public void apply(DetGraph g, int pos) {
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				for (int i=pos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("MD") || (g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
						g.ajoutDep("SBJ", pos, i);
                        return;
					}
				}
			}

			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos>=g.getNbMots()-1) return false;
				String postag = g.getMot(pos).getPOS();
				if (postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD")) {
					int d = g.getDep(pos);
					if (d<0) return true;
					if (g.getMot(g.getHead(d)).getPOS().startsWith("IN")) return false;
					
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("MD") || (g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
							return true;
						}
					}
				}
				return false;
			}
		};

		OldRuleInterface rVC = new OldRuleInterface() {
			public String toString() {return "VC";}
			@Override
			public void apply(DetGraph g, int pos) {

				if (false) {
					// debug
					int dd = g.getDep(5);
					if (dd>=0&&g.deps.get(g.getDep(5)).type==Dep.getType("SBJ")) {
						System.out.println("tentative apply VC");
						JSafran.viewGraph(g);
					}
				}
				
				for (int i=pos-1;i>=0;i--)
					if (ismod(g.getMot(i).getForme())) {
						int d = g.getDep(pos);
						if (d>=0) g.removeDep(d);
						g.ajoutDep("VC", pos, i);
					}
			}

			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (!g.getMot(pos).getPOS().startsWith("VB")) return false;
				int prevMD = -1;
				for (int i=pos-1;i>=0&&i>=pos-5;i--)
					if (ismod(g.getMot(i).getForme())) {
						prevMD=i; break;
					} else if (g.getMot(i).getPOS().startsWith("VB")) break;
				if (prevMD>=0) return true;
				else return false;
			}
			boolean ismod(String mot) {
				final String[] mods = {"have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
				for (String m : mods)
					if (mot.equals(m)) return true;
				return false;
			}
		};

		// si une conj. rel. est gouvernee par un verbe et precedee par un nom, lie le verbe au nom
		OldRuleInterface rRel = new OldRuleInterface() {
			@Override
			public void apply(DetGraph g, int pos) {
				int d = g.getDep(pos);
				int h = g.getHead(d);
				d = g.getDep(h);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("NMOD", h, pos-1);
			}
			
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (!g.getMot(pos).getPOS().startsWith("WDT")) return false;
				if (pos<1) return false;
				int d = g.getDep(pos);
				if (d<0) return false;
				int h = g.getHead(d);
				if (g.getMot(h).getPOS().startsWith("MD") || (g.getMot(h).getPOS().startsWith("VB") && !g.getMot(h).getPOS().startsWith("VBG"))) {
					String postag = g.getMot(pos-1).getPOS();
					if (postag.startsWith("NN")) {
						return true;
					} else return false;
				} else return false;
			}
		};
		
		OldRuleInterface rRBl = new OldRuleInterface() {
			@Override
			public void apply(DetGraph g, int pos) {
				int closest = -1;
				for (int i=pos-1;i>=0;i--) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						if (closest<0||pos-i<closest-i)
							closest=i;
						break;
					}
				}
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("ADV", pos, closest);
			}
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (!g.getMot(pos).getPOS().startsWith("RB")) return false;
				int closest = -1;
				for (int i=pos-1;i>=0;i--) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						if (closest<0||pos-i<closest-i)
							closest=i;
						break;
					}
				}
				if (closest>=0) return true;
				else return false;
			}
		};
		OldRuleInterface rRBr = new OldRuleInterface() {
			@Override
			public void apply(DetGraph g, int pos) {
				int closest = -1;
				for (int i=pos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						closest=i;
						break;
					}
				}
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("ADV", pos, closest);
			}
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (!g.getMot(pos).getPOS().startsWith("RB")) return false;
				int closest = -1;
				for (int i=pos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						closest=i;
						break;
					}
				}
				if (closest>=0) return true;
				else return false;
			}
		};
		
		
		OldRuleInterface rIM = new OldRuleInterface() {
			@Override
			public void apply(DetGraph g, int pos) {
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("IM", pos, pos-1);
			}
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos<=0) return false;
				if (!g.getMot(pos).equals("VB")) return false;
				if (g.getMot(pos-1).getForme().equals("to")) return true;
				else return false;
			}
		};
		
		OldRuleInterface rAint = new OldRuleInterface() {
			
			@Override
			public void apply(DetGraph g, int pos) {
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("ADV", pos, pos-1);
			}
			
			@Override
			public boolean applicable(DetGraph g, int pos) {
				if (pos>0&&g.getMot(pos).getForme().equals("n't")) return true;
				return false;
			}
		};
		
		OldRuleInterface rdeldep = new OldRuleInterface() {
			
			@Override
			public void apply(DetGraph g, int pos) {
				int d = g.getDep(pos);
				if (d<0) return;
				if (!g.getDepLabel(d).equals("DEP")) return;
				if (g.getHead(d)!=pos-1) return;
				int dd=g.getDep(pos-1);
				if (dd<0) return;
				if (g.getHead(dd)!=pos) return;
				g.removeDep(d);
			}
		
			/**
			 * cette regle s'applique typiquement APRES une autre regle pour supprimer un cycle cree par un DEP
			 * il ne faut donc pas tester l'existence de ce cycle en precondition !
			 */
			@Override
			public boolean applicable(DetGraph g, int pos) {
				int d = g.getDep(pos);
				// avec cette contrainte, il n'elimine QUE les DEP créés lors de la phase init
				if (d<0) return false;
				if (!g.getDepLabel(d).equals("DEP")) return false;
				if (g.getHead(d)!=pos-1) return false;
/*
				d=g.getDep(pos-1);
				if (d<0) return false;
				if (g.getHead(d)!=pos) return false;
				*/
				return true;
			}
		};
		
		rules.add(rPOS);
		rules.add(rCOORD1);
		rules.add(rCOORD2);
		rules.add(rCOORD3);
		rules.add(rSUJRIGHT);
		rules.add(rVC);
		rules.add(rRel);
		rules.add(rRBl);
		rules.add(rRBr);
		rules.add(rIM);
		rules.add(rAint);
		rules.add(rdeldep);
		
		// TODO: modifier les rules pour que les rules identifient les NPs,
		// et que les PDFs choisissent parmi plusieurs attachements des NPs et
		// plusieurs segmentations des NPs possibles
		
		return rules;
	}

	// ===================================
	
	List<OldRuleInterface> allrules = new ArrayList<WSJRules.OldRuleInterface>();
	HashSet<DetGraph> isGraphInit = new HashSet<DetGraph>();
	
	public WSJRules() {
		// DODO: check if some individual rule uses static Dep.xxx methods, which requires the corpus to be already loaded !
		allrules = initRules();
	}
	
	@Override
	public boolean applyRule(DetGraph g, int position) {
		int ridx = position/g.getNbMots();
		int pos = position%g.getNbMots();
		allrules.get(ridx).apply(g, pos);
		return true;
	}

	@Override
	public void unapplyRule(DetGraph g, int position) {
		int ridx = position/g.getNbMots();
		int pos = position%g.getNbMots();
		unApply(allrules.get(ridx), pos, g);
	}

	@Override
	public int[] isApplicable(DetGraph g) {
		if (!isGraphInit.contains(g)) {
			initGraph(g);
			isGraphInit.add(g);
		}
		ArrayList<Integer> combinedidx = new ArrayList<Integer>();
		for (int ridx=0;ridx<allrules.size();ridx++) {
			// old loop: it should not exist any more now...
			for (int i=0;i<g.getNbMots();i++) {
				if (allrules.get(ridx).applicable(g, i)) {
					combinedidx.add(ridx*g.getNbMots()+i);
				}
			}
		}
		int[] res = new int[combinedidx.size()];
		for (int i=0;i<res.length;i++) res[i]=combinedidx.get(i);
		return res;
	}

	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
//		List<DetGraph> glist = gio.loadConll06("/home/xtof/corpus/PENN_TREEBANK/wsj10.conll", false);
// debug
		List<DetGraph> glist = gio.loadAllGraphs("wsj10.xml");
		List<DetGraph> gold = gio.loadAllGraphs("wsj10.xml");
		for (DetGraph g : glist) g.clearDeps();
		System.out.println("corp size "+glist.size());

		ArrayList<UserRule> rules = new ArrayList<UserRule>();
		rules.add(new WSJRules());

		RulesDepthFirst.Model mod = new SyntModel(glist);
		RulesDepthFirst search = new RulesDepthFirst(rules, mod);
		search.pruningLevel=1;

		HashMap<DetGraph, List<Integer>> graph2rules = new HashMap<DetGraph, List<Integer>>();
		for (DetGraph gt : glist) {
			gt.clearDeps();
			// l'init ici differe de l'init d'avant, qui n'appliquait aucune rule (?)
			List<Integer> l = search.init(gt);
			graph2rules.put(gt, l);
			mod.updateCounts(true, gt);
		}
		
		// just after init
		{
			float joint=0;
			for (DetGraph gt : glist) {
				joint += mod.getJoint(gt);
			}
			System.out.println("init joint "+joint+" tot "+mod.getTot());
		}
		
		final int niters=1000;
		for (int iter=0;iter<niters;iter++) {
			float joint=0;
			int smin=Integer.MAX_VALUE, smax=0, savg=0;
			for (int gi=0;gi<glist.size();gi++) {
				System.out.print("graph "+gi+"/"+glist.size()+"\r");
				DetGraph g = glist.get(gi);
				
				// decrease counts
				List<Integer> rapp = graph2rules.get(g);
				mod.updateCounts(false, g);
				for (int i=0;i<rapp.size();) {
					int r=rapp.get(i++);
					int p=rapp.get(i++);
					rules.get(r).unapplyRule(g, p);
				}
				rapp.clear();

				// compute post distrib
				double[] post = search.search(g);

				if (post.length<smin) smin=post.length;
				if (post.length>smax) smax=post.length;
				savg+=post.length;
				
				// debugWithOld(g,gi,search.resdepsdbug,search);
				
				// sample
				UnsupParsing.normalizeLog(post);
				int sampledR = search.sample_Mult(post);
				
				// apply chosen rule
				Stack<Integer> rulesapp = search.applyChosenRule(sampledR,g);
				for (int i : rulesapp) rapp.add(i);
				
				// increase counts
				mod.updateCounts(true, g);
				
				joint+=mod.getJoint(g);
			}
			float lavg = (float)savg/(float)(glist.size());
			
			float[] acc = ClassificationResult.calcErrors(glist, gold);
			System.out.println("iter "+iter+" joint "+joint+" tot "+mod.getTot()+" ambiguite "+smin+"-"+smax+":"+lavg+" LAS: "+acc[0]);
JSafran.viewGraph(glist);
			if (iter%100==0) {
				gio.save(glist, "rec"+iter+".xml");
				mod.save("mods.iter"+iter);
			}
		}
	}
	
	String getRuleName(DetGraph g, int position) {
		int ridx = position/g.getNbMots();
		return allrules.get(ridx).toString();
	}
	
	// DEBUG
	static List<DetGraph> oldgraphs = null;
	static void debugWithOld(DetGraph g, int gi, List<int[]> depsgenerated, RulesDepthFirst search) {
		if (oldgraphs==null) {
			GraphIO gio = new GraphIO(null);
			oldgraphs = gio.loadAllGraphs("../newmemm/rec.xml");
		}
		DetGraph gg = oldgraphs.get(gi);
		int[] olddeps = new int[gg.getNbMots()*2];
		for (int i=0;i<g.getNbMots();i++) {
			int dd=gg.getDep(i);
			if (dd>=0) {
				olddeps[i*2]=gg.getHead(dd);
				olddeps[i*2+1]=gg.deps.get(dd).type;
			} else {
				olddeps[i*2]=-1;
				olddeps[i*2+1]=-1;
			}
		}
		boolean found=false;
		for (int[] newdeps : depsgenerated) {
			boolean ok = true;
			for (int i=0;i<newdeps.length;i++) {
				if (newdeps[i]!=olddeps[i]) {ok=false; break;}
			}
			if (ok) {
				found=true; break;
			}
		}
		if (!found) {
			System.out.println("PROBLEM "+gi);
			ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
			gs.add(g);
			gs.add(gg);
			for (int i=0;i<depsgenerated.size();i++) {
				DetGraph g3 = g.clone();
				g3.comment = search.rulesSeqs.get(i).length+" ";
				for (int j=0;j<search.rulesSeqs.get(i).length;j++) {
					int x = search.rulesSeqs.get(i)[j];
					int xpos = search.rpos.get(x);
					String rname = ((WSJRules)(search.handRules.get(search.rules.get(x)))).getRuleName(g3, xpos);
					g3.comment += rname+" ";
				}
				g3.clearDeps();
				int[] ds = depsgenerated.get(i);
				for (int j=0;j<g3.getNbMots();j++) {
					if (ds[j*2]>=0)
						g3.ajoutDep(ds[j*2+1], j, ds[j*2]);
				}
				gs.add(g3);
			}
			
			JSafran.viewGraph(gs);
		}
	}
}
