package jsafran.parsing.unsup.criteria;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.parsing.unsup.UnsupParsing;
import jsafran.parsing.unsup.rules.Rules;

public class DepthFirst {
	Rules rules;
	DetGraph g;
	//	DetGraph gsrl;
	ArrayList<Integer> rulesid = new ArrayList<Integer>();
	ArrayList<Integer> rulepos = new ArrayList<Integer>();
	private List<Integer>[] lattice;
	//	private List<Integer>[] latticeSRL;
	// temporary stores the number of dependencies before applying some rume; because when applying, all new deps are added _after_ this number
	// so that it's easy to remove "unapply" afterwards
	private int tmpNdepsBeforeRule = -1, tmprApplied=-1, tmpNdepsBeforeRuleSRL=-1;
	private boolean srl=false;
	private PathScorer scorer=null;

	// J'ai un bug avec le pruning !!
	boolean doPruning=false;


	public interface PathScorer {
		public int getScore(DetGraph g, List<Integer> rulesApplied);
		public int getPruningScore(DetGraph g, int w);
	}
	public void addPathScorer(PathScorer p) {
		scorer = p;
	}

	public int getRuleID(int r) {
		if (r<0) return -1;
		return rulesid.get(r);}
	public int getRulePos(int r) {return rulepos.get(r);}
	public void clear() {
		rulesid.clear();
		rulepos.clear();
	}

	// this one applies a rule only on the current word (it splits the rules to get a single arc per rule)
	public boolean applyRule(DetGraph g, int r, int word) {
		applyRule(g,r);
		DetGraph gsrl=null;
		if (g.relatedGraphs!=null){
			gsrl=g.relatedGraphs.get(0);
		}
		//TODO: see the case when it doesn't generate any dependency for the syntax but does for the semantics
		if (tmpNdepsBeforeRule==g.deps.size()) {
			// la rule peut ne generer aucune dep si deps deja la dans un autre mot ?
			// dans ce cas, la rule n'est pas applicable.
			// l'autre option aurait ete d'appliquer seulement LA dep sur word tout de suite
			return false;
		}
		Dep d = null;
		Dep dsrl = null;
		for (int i=tmpNdepsBeforeRule;i<g.deps.size();i++) {
			if (g.deps.get(i).gov.getIndexInUtt()-1==word) {
				d=g.deps.get(i);
				break;
			}
		}
		if (d==null) {
			System.out.println(tmpNdepsBeforeRule+" --- "+ g.deps.size());
			System.out.println("WORDDD="+word+" RUL="+r);
			for (int i=0;i<g.deps.size();i++) {
				System.out.println("  DDGOV "+(g.deps.get(i).gov.getIndexInUtt()-1));
			}
		}
		assert d.gov.getIndexInUtt()-1==word;
		if (srl && (tmpNdepsBeforeRuleSRL!=gsrl.deps.size())){
			for (int i=tmpNdepsBeforeRuleSRL;i<gsrl.deps.size();i++) {
				if (gsrl.deps.get(i).gov.getIndexInUtt()-1==word) {
					dsrl=gsrl.deps.get(i);
					break;
				}
			}
			//                    if (dsrl==null) {
				//                            System.out.println("WORDDD="+word+" RUL="+r);
				//                            for (int i=0;i<gsrl.deps.size();i++) {
			//                                    System.out.println("  DDGOV "+(gsrl.deps.get(i).gov.getIndexInUtt()-1));
			//                            }
			//                    }
		}
		unapplyRule(g, r);
		g.deps.add(d);

		if (dsrl!=null && !gsrl.deps.contains(dsrl)) {
			gsrl=g.relatedGraphs.get(0);
			gsrl.deps.add(dsrl);
		}

		return true;
	}
	// this one applies a rule on all the words it affects
	private void applyRule(DetGraph g, int r) {
		if (g.relatedGraphs!= null){
			DetGraph gsrl=g.relatedGraphs.get(0);
			tmpNdepsBeforeRuleSRL=gsrl.deps.size();
		}
		tmpNdepsBeforeRule = g.deps.size();
		tmprApplied = r;
		rules.applyRule(rulesid.get(r), g, rulepos.get(r));
	}
	// this one unapplies a rule only on the current word (it splits the rules to get a single arc per rule)
	public void unapplyRule(DetGraph g) {
		int i=g.deps.size()-1;
		g.deps.remove(i);
		if (g.relatedGraphs!=null){
			DetGraph gsrl=g.relatedGraphs.get(0);
			if ((tmpNdepsBeforeRuleSRL!=gsrl.deps.size())) {
				int isrl=gsrl.deps.size()-1;
				if (isrl>=0) {
					gsrl.deps.remove(isrl);
				}
			}
		}
	}
	// this one unapplies a rule on all the words it affects
	private void unapplyRule(DetGraph g, int r) {
		if (g.relatedGraphs!= null) {
			DetGraph gsrl=g.relatedGraphs.get(0);
			if (tmpNdepsBeforeRuleSRL!=gsrl.deps.size()){
				int nsrl=gsrl.deps.size();
				for (int i=nsrl-1;i>=tmpNdepsBeforeRuleSRL;i--)
					gsrl.deps.remove(i);
			}
		}
		assert tmprApplied==r;
		int n=g.deps.size();
		for (int i=n-1;i>=tmpNdepsBeforeRule;i--)
			g.deps.remove(i);
	}
	public void setGraph(Rules rules, DetGraph g) {
		this.rules=rules;
		this.g=g;
		if (g.relatedGraphs!=null) {
			//                    this.gsrl=g.relatedGraphs.get(0);
			this.srl=true;
		}
		clear();

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

	private int depthrec(int w, List<Integer> rulesApplied) {
		//            System.out.println("Enter depthrec: "+ w+"  " +rulesApplied);
		//            for (int r:rulesApplied) System.out.print(r+"-"+rules.toString(r)+"  ");
		//            System.out.println("");
		//            System.out.println("num words:"+g.getNbMots()+"   scorer:"+scorer);
		if (w>=g.getNbMots()) {
			if (scorer!=null) {
				int branchScore = scorer.getPruningScore(g,w);
				if (branchScore<bestScoreSoFar) {
					bestScoreSoFar=branchScore;
					bestScoresSoFar.put(g, branchScore);
				}
				return scorer.getScore(g, rulesApplied);
			} else return 0;
		}
		if (scorer!=null) {
			int branchScore=scorer.getPruningScore(g,w);
			if (doPruning&&branchScore>bestScoreSoFar) {
				// on prune cette branche !
				return Integer.MAX_VALUE;
			}
		}
		if (lattice[w].size()==0 ){
			rulesApplied.add(-1);
			int s=depthrec(w+1,rulesApplied);
			rulesApplied.remove(rulesApplied.size()-1);
			return s;
		}else{
			int bests=Integer.MAX_VALUE;

			// allow not to apply any rule ...
			//			rulesApplied.add(-1);
			//			int ss=depthrec(w+1,rulesApplied);
			//			rulesApplied.remove(rulesApplied.size()-1);
			//			if (ss<bests) bests=ss;

			// ... or apply one of the ambiguous rules
			for (int j=0;j<lattice[w].size();j++) {
				int r = lattice[w].get(j);
				if (!applyRule(g, r, w)) {
					continue;
				}
				rulesApplied.add(r);
				int s=depthrec(w+1,rulesApplied);
				if (s<bests) bests=s;
				rulesApplied.remove(rulesApplied.size()-1);
				unapplyRule(g);
			}
			return bests;
		}
	}
	private HashMap<DetGraph,Integer> bestScoresSoFar = new HashMap<DetGraph, Integer>();
	// -1 to allow for breaking cycles as soon as we get one (their score = MAX)
	private int bestScoreSoFar = Integer.MAX_VALUE-1, gidx;
	public int depthFirst() {
		bestScoreSoFar = Integer.MAX_VALUE-1;
		if (bestScoresSoFar.containsKey(g)) bestScoreSoFar = bestScoresSoFar.get(g);
		ArrayList<Integer> rulesApplied = new ArrayList<Integer>();
		return depthrec(0,rulesApplied);
	}
}
