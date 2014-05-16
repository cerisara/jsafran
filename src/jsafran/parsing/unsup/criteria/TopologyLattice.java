package jsafran.parsing.unsup.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.parsing.unsup.rules.Rules;

/**
 * Builds a forward lattice of all the rules that can be applied to every word.
 * Scores this lattice based on topological scores in order to retain (or prefer) only the resulting projective dependency trees.
 * Then provides methods to allow for another class to backtrack on this tree using another type of score.
 * 
 * NO: this approach does not work because of very low mixing.
 * Must do a brute dpeth-first search !
 * 
 * @author xtof
 *
 */
public class TopologyLattice {
	ArrayList<Integer> rulesid = new ArrayList<Integer>();
	ArrayList<Integer> rulepos = new ArrayList<Integer>();

	// temporary stores the number of dependencies before applying some rume; because when applying, all new deps are added _after_ this number
	// so that it's easy to remove "unapply" afterwards
	private int tmpNdepsBeforeRule = -1, tmprApplied=-1;
	private Rules rules = null;
	private DetGraph g = null;
	private List<Integer>[] lattice;
	// bestPrevious[word] = curRule X (bestPrevRule1, bestPrevRule2, ...)
	// this variable stores the backward lattice
	HashMap<Integer, List<Integer>>[] bestPrevious = new HashMap[lattice.length];
	// these two variables are updated after the forward pass; they point to the "entry" nodes of the backward lattice
	ArrayList<Integer> bestFinalNodes = new ArrayList<Integer>();
	int bestFinalScore = Integer.MAX_VALUE;

	public TopologyLattice(DetGraph g, Rules rules) {
		this.rules=rules;
		this.g=g;
		buildLattice(g,rules);
	}

	public int getRuleID(int r) {
		return rulesid.get(r);
	}
	public int getRulePos(int r) {
		return rulepos.get(r);
	}
	// this one applies a rule only on the current word (it splits the rules to get a single arc per rule)
	public void applyRule(DetGraph g, int r, int word) {
		applyRule(g,r);
		Dep d = null;
		for (int i=tmpNdepsBeforeRule;i<g.deps.size();i++) {
			if (g.deps.get(i).gov.getIndexInUtt()-1==word) {
				d=g.deps.get(i);
				break;
			}
		}
		unapplyRule(g, r);
		g.deps.add(d);
	}
	// this one applies a rule on all the words it affects
	private void applyRule(DetGraph g, int r) {
		tmpNdepsBeforeRule = g.deps.size();
		tmprApplied = r;
		rules.applyRule(rulesid.get(r), g, rulepos.get(r));
	}
	// this one unapplies a rule only on the current word (it splits the rules to get a single arc per rule)
	public void unapplyRule(DetGraph g) {
		g.deps.remove(g.deps.size()-1);
	}
	// this one unapplies a rule on all the words it affects
	private void unapplyRule(DetGraph g, int r) {
		assert tmprApplied==r;
		int n=g.deps.size();
		for (int i=tmpNdepsBeforeRule;i<n;i++)
			g.deps.remove(tmpNdepsBeforeRule);
	}

	private void buildLattice(DetGraph g, Rules rules) {
		// first build all applicable rules
		for (int r=0;r<rules.getNrules();r++) {
			int[] pos=rules.getApplicable(r, g);
			for (int i=0;i<pos.length;i++) {
				rulesid.add(r);
				rulepos.add(pos[i]);
			}
		}
		// second, for each word, build the list of rules that modify it
		lattice = new List[g.getNbMots()];
		for (int i=0;i<lattice.length;i++) {
			lattice[i] = new ArrayList<Integer>();
		}
		for (int r=0;r<rulesid.size();r++) {
			applyRule(g,r);
			// look at the new (just-created) dependencies only
			for (int i=tmpNdepsBeforeRule;i<g.deps.size();i++) {
				lattice[g.deps.get(i).gov.getIndexInUtt()-1].add(r);
			}
			unapplyRule(g, r);
		}
		// When a rule affects several words, it's duplicated in the word2rules lists
		// We'll have to be careful, when applying one rule from the lattice, to only apply _the_ dependency that is created by this rule at this word
		// This is so equivalent to "splitting" multi-words rules into several single-word rules
	}

	/**
	 * goes forward into the lattice and scores it with topology only
	 */
	private void forward() {
		int[][] score = new int[2][rulesid.size()];
		int cur=0;
		Arrays.fill(score[0],Integer.MAX_VALUE);
		Arrays.fill(score[1],Integer.MAX_VALUE);
		bestPrevious = new HashMap[lattice.length];
		{
			int i=0;
			bestPrevious[i]=new HashMap<Integer, List<Integer>>();
			for (int r : lattice[0]) {
				score[cur][r]=calcTopologyScore(r,0);
				bestPrevious[i].put(r, null);
			}
			cur=1-cur;
		}
		for (int i=1;i<lattice.length-1;i++) {
			bestPrevious[i]=new HashMap<Integer, List<Integer>>();
			for (int r : lattice[i]) {
				int sc = calcTopologyScore(r,i);
				ArrayList<Integer> bestp = new ArrayList<Integer>();
				int bests=Integer.MAX_VALUE;
				// compute the best paths arriving at (i,r)
				for (int p=0;p<score[1-cur].length;p++) {
					// finds all previous (i-1,p) that can transit to (i,r)
					if (score[1-cur][p]<Integer.MAX_VALUE) {
						// compares the cumulated score to the best one so far
						int cumuls = score[1-cur][p] + sc;
						if (bestp.size()==0||cumuls<bests) {
							bestp.clear();
							bestp.add(p); bests=cumuls;
						} else if (cumuls==bests) {
							// we have to save _all_ best previous paths !
							bestp.add(p);
						}
					}
				}
				score[cur][r]=bests;
				bestPrevious[i].put(r,bestp);
			}
			cur=1-cur;
		}
		{
			// for the last word, we further track the best final nodes
			int i=lattice.length-1;
			bestPrevious[i]=new HashMap<Integer, List<Integer>>();
			for (int r : lattice[i]) {
				int sc = calcTopologyScore(r,i);
				ArrayList<Integer> bestp = new ArrayList<Integer>();
				int bests=Integer.MAX_VALUE;
				// compute the best paths arriving at (i,r)
				for (int p=0;p<score[1-cur].length;p++) {
					// finds all previous (i-1,p) that can transit to (i,r)
					if (score[1-cur][p]<Integer.MAX_VALUE) {
						// compares the cumulated score to the best one so far
						int cumuls = score[1-cur][p] + sc;
						if (bestp.size()==0||cumuls<bests) {
							bestp.clear();
							bestp.add(p); bests=cumuls;
						} else if (cumuls==bests) {
							// we have to save _all_ best previous paths !
							bestp.add(p);
						}
					}
				}
				score[cur][r]=bests;
				bestPrevious[i].put(r,bestp);
				if (bests<bestFinalScore) {
					bestFinalNodes.clear();
					bestFinalNodes.add(r);
					bestFinalScore=bests;
				} else if (bests==bestFinalScore) {
					bestFinalNodes.add(r);
				}
			}
		}
	}

	private int calcTopologyScore(int r, int word) {
		applyRule(g, r, word);
		// count the number of crossing arcs before the word

		unapplyRule(g);
		return 0;
	}


	/**
	 * method used to backtrack in the lattice.
	 * Takes 2 arguments: the current word, and the rule chosen just before in the backward lattice (=chosen rule on the _next_ word)
	 */
	public List<Integer> getBest(int word, int succrule) {
		if (succrule<0) {
			// last word in the sentence = entry in the backward lattice
			return bestFinalNodes;
		} else {
			return bestPrevious[word].get(succrule);
		}
	}

	// **********************************************
        
//        static int penaltySRL(DetGraph g) {
//            int penaltyRepArgs=0;
//		if (g.relatedGraphs!=null && g.relatedGraphs.size()>0) {
//			DetGraph gg = g.relatedGraphs.get(0);
//			boolean[] hasA0 = new boolean[gg.getNbMots()];
//                        boolean[] hasA1 = new boolean[gg.getNbMots()];
//			Arrays.fill(hasA0, false);
//                        Arrays.fill(hasA1, false);
//                        Arrays.fill(predicates,-1);
//			final int A0dep = Dep.getType("A0");
//                        final int A1dep = Dep.getType("A1");
//			for (int i=0;i<gg.deps.size();i++) {
//				Dep d = gg.deps.get(i);
//                                predicates[d.gov.getIndexInUtt()-1]=d.head.getIndexInUtt()-1;
//				if (d.type==A0dep) {
//					if (hasA0[d.gov.getIndexInUtt()-1]) {
//                                            penaltyRepArgs+=100;
//					} else hasA0[d.gov.getIndexInUtt()-1]=true;
//				}
//				if (d.type==A1dep) {
//					if (hasA1[d.gov.getIndexInUtt()-1]) {penaltyRepArgs+=100;
//					} else hasA1[d.gov.getIndexInUtt()-1]=true;
//				}
//			}
//		}		
//                return penaltyRepArgs;
//        }

        static int penaltySRL(DetGraph g) {
            int penaltyRepArgs=0;
		if (g.relatedGraphs!=null && g.relatedGraphs.size()>0) {
                    DetGraph gg = g.relatedGraphs.get(0);
                    for (int i=0;i<gg.getNbMots();i++){
                        if (g.getMot(i).getPOS().startsWith("VB")){
                            boolean hasA0=false;
                            boolean hasA1=false;
                            boolean hasA2=false;
                            boolean hasA3=false;
                            boolean hasA4=false;
                            List<Integer> childre= gg.getFils(i);
                            for (int c:childre){
                                int[]deps=gg.getDeps(c);
                                for (int d:deps){
                                    if (gg.getDepLabel(d).equals("A0")) {
                                        if (hasA0) penaltyRepArgs+=100;
                                        else hasA0=true;
                                    }
                                    else{
                                        if (gg.getDepLabel(d).equals("A1")) {
                                            if (hasA1) penaltyRepArgs+=100;
                                            else hasA1=true;
                                        }else{
                                            if (gg.getDepLabel(d).equals("A2")) {
                                                if (hasA2) penaltyRepArgs+=100;
                                                else hasA2=true;
                                            }else{
                                                if (gg.getDepLabel(d).equals("A3")) {
                                                    if (hasA3) penaltyRepArgs+=100;
                                                    else hasA3=true;
                                                }else{
                                                    if (gg.getDepLabel(d).equals("A4")) {
                                                        if (hasA4) penaltyRepArgs+=100;
                                                        else hasA4=true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return penaltyRepArgs;
        }

	static int penaltyCycles(DetGraph g) {
		HashSet<Integer> dejavus = new HashSet<Integer>();
		int ncycles = 0;
		for (Dep de : g.deps) {
			dejavus.clear();
			int gov = de.gov.getIndexInUtt()-1;
			int head = de.head.getIndexInUtt()-1;
			int w = head;
			dejavus.add(gov);
			dejavus.add(head);
			for (;;) {
				int d = g.getDep(w);
				if (d<0) break;
				w=g.getHead(d);
				if (dejavus.contains(w)) {
					ncycles++;
					break;
				}
				dejavus.add(w);
			}
		}
		return ncycles;
	}
	public static int getPruningScore(DetGraph g, int w) {
		int nc=0;
		for (int i=0;i<g.deps.size();i++) {
			int ga=g.deps.get(i).gov.getIndexInUtt()-1;
			int ha=g.deps.get(i).head.getIndexInUtt()-1;
			for (int j=i+1;j<g.deps.size();j++) {
				int gb=g.deps.get(j).gov.getIndexInUtt()-1;
				int hb=g.deps.get(j).head.getIndexInUtt()-1;
				// detect cycles simples
				if (ga==hb&&gb==ha) return Integer.MAX_VALUE;
				if (ga>ha) {int z=ga;ga=ha;ha=z;}
				if (gb>hb) {int z=gb;gb=hb;hb=z;}
				if (ga<gb) {
					if (ha>gb&&ha<hb) nc++;
				} else if (ga>gb) {
					if (hb>ga&&hb<ha) nc++;
				}
			}
		}
		// normal pruning:
		return nc;
		// no pruning:
		//		return 0;
		// prune all but first one:
		//		if (w==g.getNbMots()) return 0;
		//		else return 1;
	}
	public static int getTopoScore(DetGraph g) {
		int ncr=0, nrts=0;
		for (int i=0;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d>=0) {
				// crossing other arc ?
				int h=g.getHead(d);
				int a=i,b=h;
				if (a>b) {a=h;b=i;}
				for (int j=a+1;j<b;j++) {
					int dd = g.getDep(j);
					if (dd<0) {
						// ncr++;
					} else {
						int hh=g.getHead(dd);
						if (hh>b||hh<a) ncr++;
					}
				}
				// crossing above ROOT ?
				for (int j=a+1;j<b;j++) {
					d = g.getDep(i);
					if (d<0) ncr++;
				}
			} else nrts++;
		}
		ncr+=penaltyCycles(g)*10;
		if (nrts==0) ncr+=50;
		else if (nrts>1) ncr+=nrts*2;
                ncr+=penaltySRL(g);
		return ncr;
	}
}
