package jsafran.searchfilters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jsafran.DetGraph;

/**
 * - définit la notion de "sous-arbre autour d'un mot"
 * - calcule la distance entre un tel sous-arbre et tout le corpus de train
 * - trie le corpus de train par distance croissante
 * 
 * @author xtof
 *
 */
public class SortedTreeDist implements Comparator<int[]> {
	DetGraph g;
	int mot;
	
	public SortedTreeDist(DetGraph g, int mot) {
		this.g=g; this.mot=mot;
	}
	
	private int simParents(SortedTreeDist t) {
		// sim=profondeur de la branche
		int w1=mot, w2=t.mot;
		int de=0;
		for (de=0;;de++) {
			int d=g.getDep(w1);
			if (d<0) break;
			String lab1 = g.getDepLabel(d);
			w1=g.getHead(d);
			d=t.g.getDep(w2);
			if (d<0) break;
			String lab2 = t.g.getDepLabel(d);
			w2=t.g.getHead(d);
			if (!lab1.equals(lab2)) break;
		}
		return de;
	}
	
	private int simEnfants(SortedTreeDist t) {
		ArrayList<String> depfils = new ArrayList<String>();
		List<Integer> fils = g.getFils(mot);
		for (int f : fils) {
			depfils.add(g.getDepLabel(g.getDep(f)));
		}
		
		ArrayList<String> tdepfils = new ArrayList<String>();
		List<Integer> tfils = t.g.getFils(t.mot);
		for (int f : tfils) {
			tdepfils.add(t.g.getDepLabel(t.g.getDep(f)));
		}
		Collections.sort(tdepfils);
		
		int sim=0;
		for (String s : depfils) {
			int i=Collections.binarySearch(tdepfils, s);
			if (i>=0) {
				sim++;
				tdepfils.remove(i);
			}
		}
		
		return sim;
	}
	
	private int dist(SortedTreeDist t) {
		int d=0;
		d-=simParents(t);
		d-=simEnfants(t);
		return d;
	}
	
	/**
	 * 
	 * @param gs
	 * @return [graphID, wordID, dist]
	 */
	public List<int[]> sortByDist(List<DetGraph> gs) {
		ArrayList<int[]> res = new ArrayList<int[]>();
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				SortedTreeDist t = new SortedTreeDist(g, j);
				int dist = dist(t);
				int[] v={i,j,dist};
				int k=Collections.binarySearch(res, v, this);
				if (k<0) k=-k-1;
				res.add(k, v);
			}
		}
		System.out.println("closest "+Arrays.toString(res.get(0))+" largest "+Arrays.toString(res.get(res.size()-1)));
		for (int i=0;i<10&&i<res.size();i++) {
			System.out.println(Arrays.toString(res.get(i)));
		}
		return res;
	}

	public static List<int[]> sortClosestBranches(List<DetGraph> gs, int gi, int wi) {
		if (gi<0||gi>=gs.size()||wi<0||wi>=gs.get(gi).getNbMots()) return new ArrayList<int[]>();
		SortedTreeDist t = new SortedTreeDist(gs.get(gi), wi);
		return t.sortByDist(gs);
	}
	
	@Override
	public int compare(int[] o1, int[] o2) {
		if (o1[2]>o2[2]) return 1;
		else if (o1[2]<o2[2]) return -1;
		else return 0;
	}
}
