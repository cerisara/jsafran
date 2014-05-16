package jsafran.searchfilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import jsafran.DetGraph;

/**
 * 
 * 
 * @author cerisara
 *
 */
public class SubTreeSelection {
	private DetGraph graph=null;
	private TreeSet<Integer> selectedWords = new TreeSet<Integer>();
	
	public static SubTreeSelection getTreeSelecter() {
		if (treeselect==null) treeselect=new SubTreeSelection();
		return treeselect;
	}
	private static SubTreeSelection treeselect = null;
	private SubTreeSelection() {}
	public static void cancelSelection() {
		treeselect=null;
	}
	
	public void addWordAndDep(DetGraph g, int w) {
		if (graph==null) graph=g;
		selectedWords.add(w);
	}
	
	public static boolean isDepSelected(DetGraph g, int gov, int head) {
		if (treeselect==null) return false;
		if (treeselect.graph!=g) return false;
		if (treeselect.selectedWords.contains(gov) && treeselect.selectedWords.contains(head)) return true;
		return false;
	}
	public static boolean isWordSelected(DetGraph g, int w) {
		if (treeselect==null) return false;
		if (treeselect.graph!=g) return false;
		if (treeselect.selectedWords.contains(w)) return true;
		return false;
	}
	
	private int getRoot() { return getRoot(selectedWords.first());}
	private int getRoot(int r) {
		// je suppose que c'est un arbre avec un seul root !
		int[] deps = graph.getDeps(r);
		if (deps==null) return r;
		for (int d : deps) {
			int head = graph.getHead(d);
			if (selectedWords.contains(head))
				return getRoot(head);
		}
		return r;
	}
	
	List<Integer> getSelectedFils(int root) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		List<Integer> fs = graph.getFils(root);
		for (int f : fs)
			if (selectedWords.contains(f)) res.add(f);
		return res;
	}
	
	boolean filsmatch(DetGraph g, HashMap<Integer, Integer> gnodes2graphnodes, int root) {
		// check mapping of root node here, if you want mapping of POStags for instance
		List<Integer> filsgraph = getSelectedFils(gnodes2graphnodes.get(root));
		if (filsgraph.size()==0) return true;
		List<Integer> fils = g.getFils(root);
		for (int fg : filsgraph) {
			// check dep label
			int[] deps = graph.getDeps(fg);
			String lab2=null;
			for (int d : deps) {
				int h = graph.getHead(d);
				if (h==gnodes2graphnodes.get(root)) {
					lab2=graph.getDepLabel(d);
					break;
				}
			}
			if (lab2==null) return false;
			for (int f : fils) {
				if (gnodes2graphnodes.keySet().contains(f)) continue;
				String lab=null;
				deps = g.getDeps(f);
				for (int d : deps) {
					int h = g.getHead(d);
					if (h==root) {
						lab=g.getDepLabel(d);
						break;
					}
				}
				if (!lab2.equals(lab)) continue;

				// TODO: check order
				gnodes2graphnodes.put(f, fg);
				if (filsmatch(g, gnodes2graphnodes, f)) {
					// on s'arrete quand on a trouve un fils
					break;
				} else {
					gnodes2graphnodes.remove(f);
				}
			}
			if (!gnodes2graphnodes.values().contains(fg)) {
				// aucun mapping de trouve
				return false;
			}
		}
		return true;
	}
	
	public SubGraphList findClosest(List<DetGraph> gs) {
		int selectedRoot = getRoot();
		System.out.println("root = "+graph.getMot(selectedRoot).getForme());
		SubGraphList res = new SubGraphList(gs);
		for (int gidx=0;gidx<gs.size();gidx++) {
			DetGraph g = gs.get(gidx);
			// ceci doit etre la racine
			for (int i=0;i<g.getNbMots();i++) {
				HashMap<Integer, Integer> gnodes2graphnodes = new HashMap<Integer, Integer>();
				gnodes2graphnodes.put(i,selectedRoot);
				if (filsmatch(g,gnodes2graphnodes,i)) {
					res.addSubGraph(gidx, gnodes2graphnodes.keySet());
				}
			}
		}
		return res;
	}
}
