package jsafran.searchfilters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jsafran.DetGraph;

public class SubGraphList {
	// liste de tous les graphes d'origine
	private List<DetGraph> graphs;
	
	// ci-dessous: liste des sous-graphes
	// ces 2 listes sont synchronisees
	// index du graphe dans la liste d'origine
	private List<Integer> graphpos = new ArrayList<Integer>();
	// index de tous les mots du sous-graphe
	private List<List<Integer>> subtreeNodes = new ArrayList<List<Integer>>();
	
	public SubGraphList(List<DetGraph> gs) {graphs=gs;}
	
	public void addSubGraph(int gidx, List<Integer> nodes) {
		graphpos.add(gidx);
		subtreeNodes.add(nodes);
	}
	public void addSubGraph(int gidx, Set<Integer> nodes) {
		ArrayList<Integer> s = new ArrayList<Integer>();
		for (int n : nodes) s.add(n);
		addSubGraph(gidx, s);
	}
	
	int idx=0;
	public void rewind() {idx=0;}
	public int[] next() {
		if (idx>=graphpos.size()) return null;
		int[] r = {graphpos.get(idx),subtreeNodes.get(idx).get(0)};
		idx++;
		return r;
	}
}
