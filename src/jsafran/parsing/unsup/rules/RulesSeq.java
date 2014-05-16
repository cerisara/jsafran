package jsafran.parsing.unsup.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jsafran.Dep;
import jsafran.DetGraph;

/*
 * lorsqu'on veut sampler une sequence de regles, et que le scoring final depend a la fois des regles dans la sequence
 * (car chaque regle a son prior, mais l'ordre des priors ne compte pas) et de l'arbre obtenu (car il y a des contraintes topologiques
 * finales, mais qui ne dependent pas des regles appliquees).
 * 
 * Cette classe construit un treillis, en appliquant les regles successivement et recursivement,
 * et en rassemblant les branches du treillis lorsque les arbres obtenus sur differents chemins sont les memes.
 * Une fois le treillis construit, on peut appliquer Viterbi pour trouver le meilleur chemin global.
 * 
 * - chaque "regle" = hand-crafted rule + position in sentence
 * - 2 chemins sont merges lorsque (i) meme heads; (ii) meme arcs; (iii) meme set of rules ==> meme score
 * 
 * codage rapide de ces contraintes:
 * int[] = heads, int[] = dep types, Set<Integer> = appliedRules
 * 
 */
public class RulesSeq {
	// liste des regles applicables 
	ArrayList<Integer> ruleid = new ArrayList<Integer>();
	ArrayList<Integer> rulepos = new ArrayList<Integer>();
	DetGraph curg;
	
	ArrayList<int[]> node2heads = new ArrayList<int[]>();
	ArrayList<int[]> node2types = new ArrayList<int[]>();
	ArrayList<Set<Integer>> node2rules = new ArrayList<Set<Integer>>();
	
	final int debug=1;
	
	public void sample(DetGraph g, Rules ruleset) {
		List<Integer>[] word2rulepos = ruleGraph(g, ruleset);
		obligatoires1(g, ruleset, word2rulepos);
		ambigues(g,ruleset,word2rulepos);
	}
	
	public void ambigues(DetGraph g, Rules ruleset, List<Integer>[] word2rulepos) {
		HashSet<Integer> amb = new HashSet<Integer>();
		for (int i=0;i<word2rulepos.length;i++) {
			if (word2rulepos[i].size()>1) {
				for (int r : word2rulepos[i]) amb.add(r);
			}
		}
//		int nNonProj0 = countNonProj(g);
		// TODO: evaluer si chaque rule ajoute des non-proj, mais quid de leur combinaison, qui peut ajouter de la non-proj ?
//		int nNonProj1 = countNonProj(g);
	}
	public void obligatoires1(DetGraph g, Rules ruleset, List<Integer>[] word2rulepos) {
		// les regles dont tous les mots impactes ne le sont QUE par cette regle
		// TODO: non, il faut appliquer une regle des qu'elle est non-ambigue pour un mot; MAIS il faut ajouter une regle ROOT qui lie les roots possibles de la phrase
		HashMap<Integer,Integer> obl = new HashMap<Integer, Integer>();
		for (int i=0;i<word2rulepos.length;i++) {
			if (word2rulepos[i].size()==1) {
				int r = word2rulepos[i].get(0);
				Integer co = obl.get(r);
				if (co==null) co=1; else co++;
				obl.put(r, co);
			}
		}
		for (int i=0;i<word2rulepos.length;i++) {
			if (word2rulepos[i].size()>1 && obl.containsKey(word2rulepos[i].get(0))) {
				obl.remove(word2rulepos[i].get(0));
			}
		}
		
		// d'abord les obligatoires qui n'impactent qu'un seul mot
		// peu importe l'ordre
		for (int r : obl.keySet()) {
			if (obl.get(r)==1) {
				ruleset.applyRule(ruleid.get(r), g, rulepos.get(r));
				System.out.println("onewordimpact "+r+" "+ruleid.get(r)+" "+rulepos.get(r)+" "+ruleset.toString(ruleid.get(r)));
			}
		}
		// puis les obligatoires qui impactent 2 mots ou plus; tant pis pour l'ordre
		for (int r : obl.keySet()) {
			if (obl.get(r)>1) {
				ruleset.applyRule(ruleid.get(r), g, rulepos.get(r));
				System.out.println("twowordimpact "+r+" "+ruleid.get(r)+" "+rulepos.get(r)+" "+ruleset.toString(ruleid.get(r)));
			}
		}
	}
	
	public List<Integer>[] ruleGraph(DetGraph g, Rules ruleset) {
		ArrayList<Dep> depsav = new ArrayList<Dep>();
		depsav.addAll(g.deps);
		
		List<Integer>[] word2rulepos = new List[g.getNbMots()];
		for (int i=0;i<word2rulepos.length;i++) word2rulepos[i]=new ArrayList<Integer>();
		
		ruleid = new ArrayList<Integer>();
		rulepos = new ArrayList<Integer>();
		for (int i=0;i<ruleset.getNrules();i++) {
			int[] pos = ruleset.getApplicable(i, g);
			for (int p : pos) {
				ruleid.add(i);
				rulepos.add(p);
				
				int[] changedWords = ruleset.applyRule(i, g, p);
				for (int w : changedWords) word2rulepos[w].add(ruleid.size()-1);
				
				g.deps.clear();
				g.deps.addAll(depsav);
			}
		}
		if (debug>=1)
			for (int i=0;i<word2rulepos.length;i++) {
				System.out.println(i+" "+g.getMot(i)+" "+word2rulepos[i]);
			}
		return word2rulepos;
	}
	
	
	
	
	void recurs(int node) {
		
	}
	
	public void buildLattice(DetGraph g, Rules ruleset) {
		curg=g;
		int[] heads = new int[g.getNbMots()];
		int[] types = new int[g.getNbMots()];
		Arrays.fill(heads, -1);
		Arrays.fill(types, -1);
		HashSet<Integer> appliedRules = new HashSet<Integer>();
		
		ArrayList<Integer> ruleid = new ArrayList<Integer>();
		ArrayList<Integer> rulepos = new ArrayList<Integer>();
		for (int i=0;i<ruleset.getNrules();i++) {
			int[] pos = ruleset.getApplicable(i, g);
			for (int p : pos) {
				ruleid.add(i);
				rulepos.add(p);
			}
		}
		
		recurs(0);
	}
}
