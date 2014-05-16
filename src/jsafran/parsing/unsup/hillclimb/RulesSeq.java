package jsafran.parsing.unsup.hillclimb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import utils.Wait;

import jsafran.Dep;
import jsafran.DetGraph;

public class RulesSeq {
	ArrayList<OneRule> ruleseq = new ArrayList<OneRule>();
	ArrayList<Integer> hashseq = new ArrayList<Integer>();
	
	static Random rand = new Random();
	

	// ------------------------------------------
	// TODO: fill it from the rules designer
	public static ArrayList<OneRule> allrules = new ArrayList<OneRule>();

	public static List<OneRule> getAllRules() {
		return allrules;
	}
	// ------------------------------------------
	
	public int getNrulesInSeq() {return ruleseq.size();}
	
	public void apply(DetGraph g) {
		clear(g);
		for (int i=0;i<ruleseq.size();i++)
			ruleseq.get(i).apply(g, hashseq.get(i));
	}
	
	public void clear(DetGraph g) {
		g.deps.clear();
		if (g.groupnoms!=null) {
			g.groupnoms.clear();
			g.groups.clear();
		}
	}
	
	int sample_Mult(float[] th) {
		double s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextDouble();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}

	public RulesSeq sampleRandomInit(DetGraph g) {
		clear(g);
		RulesSeq res = new RulesSeq();
		List<OneRule> allrules = RulesSeq.getAllRules();
		Collections.shuffle(allrules);
		
		for (;;) { // add each rules one after the other in the list
			ArrayList<OneRule> curRcands = new ArrayList<OneRule>();
			ArrayList<Integer> curHcands = new ArrayList<Integer>();
			for (OneRule r : allrules) {
				int[] hashcodes = r.getApplicable(g);
				int nHcands=hashcodes.length;
				for (int i=0;i<nHcands;i++) {
					curRcands.add(r);
					curHcands.add(hashcodes[i]);
				}
			}
			if (curRcands.size()>0) {
				// compute the length of arcs created
				ArrayList<Dep> savedeps = new ArrayList<Dep>();
				savedeps.addAll(g.deps);
				float[] invlen = new float[curRcands.size()];
				for (int i=0;i<curRcands.size();i++) {
					// apply rule
					res.ruleseq.add(curRcands.get(i));
					res.hashseq.add(curHcands.get(i));
					res.apply(g);
					// average length of arcs
					int ltot=0;
					for (Dep d : g.deps) ltot+=Math.abs(d.gov.getIndexInUtt()-d.head.getIndexInUtt());
					float avglen = (float)ltot/(float)(g.deps.size());
					invlen[i]=1f/avglen;
					// come back
					res.ruleseq.remove(res.ruleseq.size()-1);
					res.hashseq.remove(res.hashseq.size()-1);
					g.deps.clear();
					g.deps.addAll(savedeps);
				}
				// weight invlen to prefer more or less short deps
				// TODO: this does not seem to have any impact ?
				for (int i=0;i<invlen.length;i++) invlen[i] *= 1000f;
				// sample according to invlen
				int r = sample_Mult(invlen);

				
				// debug on choisit le + petit
//				int rmax=0;
//				for (int z=1;z<invlen.length;z++) if (invlen[rmax]<invlen[z]) rmax=z;
//				r=rmax;
//				System.out.println("debug invlen "+r+" "+Arrays.toString(invlen));
//Wait.waitUser();

				res.ruleseq.add(curRcands.get(r));
				res.hashseq.add(curHcands.get(r));
				res.apply(g);
			} else {
				// there is no more rules to add to the current list
				break;
			}
		}
//		Wait.waitUser();
		if (res.hashseq.size()==0) {
			// not any rule can be sampled on this sentence
			return null;
		}
		return res;
	}
	
	public RulesSeq[] sampleNseqs(DetGraph g, int nrulesseqs) {
		ArrayList<RulesSeq> res = new ArrayList<RulesSeq>();
		for (int i=0;i<nrulesseqs;i++) {
			RulesSeq r = sampleRandomUniform(g);
			if (r==null) {
				// a very small number of sequences can be applied on this sentence
				break;
			}
			res.add(r);
		}
		RulesSeq[] r = res.toArray(new RulesSeq[res.size()]);
		return r;
	}
	public RulesSeq sampleGreedy(DetGraph g, Scorer sc) {
		clear(g);
		RulesSeq res = new RulesSeq();
		HashMap<OneRule, Set<Integer>> rulesUsed2hashcode = new HashMap<OneRule, Set<Integer>>();
		List<OneRule> allrules = RulesSeq.getAllRules();
		for (;;) { // add each rules one after the other in the list
			ArrayList<OneRule> curRcands = new ArrayList<OneRule>();
			ArrayList<Integer> curHcands = new ArrayList<Integer>();
			for (OneRule r : allrules) {
				int[] hashcodes = r.getApplicable(g);
				int nHcands=hashcodes.length;
				// remove already used hashcodes
				Set<Integer> alreadyused = rulesUsed2hashcode.get(r);
				if (alreadyused!=null) {
					for (int i=0;i<nHcands;i++) {
						if (alreadyused.contains(hashcodes[i])) {
							hashcodes[i--]=hashcodes[--nHcands];
						}
					}
				}
				for (int i=0;i<nHcands;i++) {
					curRcands.add(r);
					curHcands.add(hashcodes[i]);
				}
			}
			
			if (curRcands.size()>0) {
				// choose the best next rule
				double bestSc = -Double.MAX_VALUE;
				int bestr=-1;
				for (int r=0;r<curRcands.size();r++) {
					DetGraph gg = g.clone();
					curRcands.get(r).apply(gg, curHcands.get(r));
					double s = sc.getScore(gg);
					if (bestr<0||s>bestSc) {
						bestr=r; bestSc=s;
					}
				}
				res.ruleseq.add(curRcands.get(bestr));
				res.hashseq.add(curHcands.get(bestr));
				Set<Integer> alreadyused = rulesUsed2hashcode.get(curRcands.get(bestr));
				if (alreadyused==null) {
					alreadyused = new HashSet<Integer>();
					rulesUsed2hashcode.put(curRcands.get(bestr), alreadyused);
				}
				alreadyused.add(curHcands.get(bestr));
			} else {
				// there is no more rules to add to the current list
				break;
			}
		}
		if (res.hashseq.size()==0) {
			// not any rule can be sampled on this sentence
			return null;
		}
		return res;

	}
	
	RulesSeq sampleRandomUniform(DetGraph g) {
		// TODO: sampling not only creates deps and groups, but also may set variables that should be reset as well here !
		clear(g);
		RulesSeq res = new RulesSeq();
//		HashMap<OneRule, Set<Integer>> rulesUsed2hashcode = new HashMap<OneRule, Set<Integer>>();
		List<OneRule> allrules = RulesSeq.getAllRules();
		Collections.shuffle(allrules);
		
		for (;;) { // add each rules one after the other in the list
			ArrayList<OneRule> curRcands = new ArrayList<OneRule>();
			ArrayList<Integer> curHcands = new ArrayList<Integer>();
			for (OneRule r : allrules) {
				int[] hashcodes = r.getApplicable(g);
				int nHcands=hashcodes.length;

				/*
				 * I don't use any more the hashcode to detect rules that have already been applied, because
				 * this requires the rules to compute some unique hashcode, which is not easy.
				 * Also, given the vars hashmap used in PatternRules, it's not that difficult any more for the
				 * rules themsleves to detect when they have already been applied.
				 * 
				 * well.. it's not so simple, because (i) the VARS are still not reset when they should
				 * and (ii) the search reapplies previously applied rules, so the "old" indexes should be kept 
				 */
				
				// remove already used hashcodes
//				Set<Integer> alreadyused = rulesUsed2hashcode.get(r);
//				if (alreadyused!=null) {
//					for (int i=0;i<nHcands;i++) {
//						if (alreadyused.contains(hashcodes[i])) {
//							hashcodes[i--]=hashcodes[--nHcands];
//						}
//					}
//				}
				for (int i=0;i<nHcands;i++) {
					curRcands.add(r);
					curHcands.add(hashcodes[i]);
				}
			}
			if (curRcands.size()>0) {
				int r = rand.nextInt(curRcands.size());
				res.ruleseq.add(curRcands.get(r));
				res.hashseq.add(curHcands.get(r));
//				Set<Integer> alreadyused = rulesUsed2hashcode.get(curRcands.get(r));
//				if (alreadyused==null) {
//					alreadyused = new HashSet<Integer>();
//					rulesUsed2hashcode.put(curRcands.get(r), alreadyused);
//				}
//				alreadyused.add(curHcands.get(r));
				// il faut appliquer la regle pour calculer les nouvelles regles applicables ensuite et completer la seq
				res.apply(g);

			} else {
				// there is no more rules to add to the current list
				break;
			}
		}
		if (res.hashseq.size()==0) {
			// not any rule can be sampled on this sentence
			return null;
		}
		return res;
	}
}
