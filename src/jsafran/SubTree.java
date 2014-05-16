package jsafran;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import utils.Permutation;

/**
 * Class used to extract all subtrees and their statistics from a corpus.
 * 
 * @author xtof
 *
 */
public class SubTree {
	static boolean verbose=true;
	public static DetGraph getSubTree(DetGraph g, int root, int maxdepth) {
		List<Integer> subnodes = getSubNodes(g, root, maxdepth);
		DetGraph gg = g.getSubGraph(0);
		for (int i=g.getNbMots()-1;i>=0;i--)
			if (!subnodes.contains(i))
				gg.delNodes(i,i);
		return gg;
	}

	private static List<Integer> getSubNodes(DetGraph g, int root, int maxdepth) {
		ArrayList<Integer> res=new ArrayList<Integer>();
		if (maxdepth<=0) return res;
		res.add(root);
		List<Integer> fils = g.getFils(root);
		for (int n : fils) {
			res.addAll(getSubNodes(g, n, maxdepth-1));
		}
		return res;
	}
	
	private static boolean isSubtreeIncluded(DetGraph target, int root, DetGraph g0, int r, int maxdepth) {
		// changer ici si on veut un test sur la forme, le POStag...
//		if (!g0.getMot(r).getPOS().equals(target.getMot(root).getPOS())) return false;
		// pour le moment, je ne teste que les dependances
		int d1 = target.getDep(root);
		if (d1<=0) {
			// on est a la racine du target: pas de test sur les dependances sortantes !
		} else {
			int d0 = g0.getDep(r); assert d0>=0;
			String lab0 = g0.getDepLabel(d0);
			String lab1 = target.getDepLabel(d1);
			if (!lab0.equals(lab1)) return false;
		}
		
		List<Integer> targetfils = target.getFils(root);
		List<Integer> g0fils = g0.getFils(r);
		if (g0fils.size()<targetfils.size()) return false;
		// utiliser plutot Combination si on ne veut pas autoriser la permutation des fils
		Permutation p = new Permutation(g0fils.size(), targetfils.size());
		while (p.hasNext()) {
			int[] idx = p.next();
			int i=0;
			for (i=0;i<idx.length;i++) {
				// associe le fils i de target avec le fils idx[i] de g0
				if (!isSubtreeIncluded(target, targetfils.get(i), g0, g0fils.get(idx[i]), maxdepth-1)) break;
			}
			if (i>=targetfils.size()) return true;
		}
		return false;
	}
	
	public static void saveIncludingSubtrees(DetGraph target, List<DetGraph> gs, String outfile) {
		List<DetGraph> res = getIncludingSubtrees(gs, target);
		GraphIO gio = new GraphIO(null);
		gio.save(res, outfile);
		System.out.println("all trees saved in "+outfile);
	}
	public static List<DetGraph> getIncludingSubtrees(List<DetGraph> gs, DetGraph target) {
		// I assume there is a single root in target !
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		int root = target.getRoots()[0];
		int maxdepth = target.getDepth(root);
		for (int z=0;z<gs.size();z++) {
			if (verbose) System.out.println("looking in "+z+"/"+gs.size());
			DetGraph g = gs.get(z);
			for (int i=0;i<g.getNbMots();i++) {
				if (isSubtreeIncluded(target,root,g,i,maxdepth)) {
					// meme si target peut etre inclus de plusieurs manieres dans g, je ne garde g qu'une seule fois
					res.add(getSubTree(g, i, maxdepth));
				}
			}
		}
		return res;
	}
	
	/** 
	 * extrait tous les sous-arbres en prenant, pour chaque mot, son sous-arbre jusqu'a la profondeur K
	 * Puis cherche dans tout le corpus les sous-arbres de meme racine qui incluent cet arbre
	 * 
	 * Retourne tous les sous-arbres trouves, avec dans le champs "conf" leur nombre d'occurrence
	 */
	public static List<DetGraph> getAllSubtrees(List<DetGraph> gs, int maxdepth) {
		verbose=false;
		ArrayList<DetGraphComparable> res = new ArrayList<DetGraphComparable>();
		for (int i=0;i<gs.size();i++) {
			System.out.println("allsubtrees "+i+"/"+gs.size());
			DetGraph g = gs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				DetGraph target = getSubTree(g, j, maxdepth);
				DetGraphComparable targetc = new DetGraphComparable(target);
				if (!res.contains(targetc)) {
					List<DetGraph> foundgs = getIncludingSubtrees(gs, target);
					target.conf=foundgs.size();
					res.add(targetc);
				}
			}
		}
		// j'ordonne cette liste par ordre decroissant
		Collections.sort(res);
		ArrayList<DetGraph> x = new ArrayList<DetGraph>();
		for (DetGraphComparable y : res) x.add(y.g);
		verbose=true;
		return x;
	}
	
	public static void saveAllSubtrees(List<DetGraph> gs, int maxdepth, String outfile) {
		List<DetGraph> x = getAllSubtrees(gs, maxdepth);
		GraphIO gio = new GraphIO(null);
		gio.save(x, outfile);
		System.out.println("all subtrees saved "+outfile);
	}
	
	/**
	 * class used to compare 2 trees: they are equal iff they have the same dependency structures and the same
	 * POStags of the leaves
	 * 
	 * @author xtof
	 *
	 */
	static class DetGraphComparable implements Comparable<DetGraphComparable> {
		public DetGraphComparable(DetGraph x) {
			g=x;
			root=g.getRoots()[0];
			depth = x.getDepth(root);
		}
		DetGraph g;
		int root, depth;
		public boolean equals(Object o) {
			DetGraphComparable gc = (DetGraphComparable)o;
			if (gc.g.getNbMots()!=g.getNbMots()) return false;
			if (gc.depth!=depth) return false;
			if (isSubtreeIncluded(gc.g, gc.root, g, root, depth)) return true;
			return false;
		}
		public int hashCode() {return g.getNbMots();}
		@Override
		public int compareTo(DetGraphComparable o) {
			return -Float.compare(g.conf, o.g.conf);
		}
	}

	// liste de tous les POSseqs de la base de train et leur ens de subtrees
	HashMap<POSseq,List<DetGraphComparable>> allseqs = new HashMap<POSseq,List<DetGraphComparable>>();
	
	/**
	 * retourne le nb d'occ correspondant à une liste de subtrees
	 * @param subtrees
	 * @return
	 */
	int getNocc(List<DetGraphComparable> subtrees) {
		int n=0;
		for (DetGraphComparable g:subtrees) n+=g.g.conf;
		return n;
	}
	
	float probmini = 0.00001f;
	
	/**
	 * calcule la proba d'un sous-arbre en le comparant aux sous-arbres observes dans le train avec la meme seq de POStags
	 * @param subtree
	 * @return
	 */
	float compProba(DetGraph subtree) {
		POSseq ps = new POSseq(subtree);
		List<DetGraphComparable> gs = allseqs.get(ps);
		if (gs==null) return 0;
		// cette POS seq a deja ete observee
		// est-ce que cet arbre l'a deje ete ?
		DetGraphComparable g = new DetGraphComparable(subtree);
		for (int i=0;i<gs.size();i++) {
			if (g.equals(gs.get(i))) {
				int nocc = (int)gs.get(i).g.conf;
				int ntot = getNocc(gs);
				float p = (float)nocc/(float)ntot;
				if (p<probmini) p=probmini;
				return p;
			}
		}
		// non cet arbre n'a jamais ete observe, on retourne une proba mini
		return probmini;
	}
	
	/**
	 * recupere le plus grand subtree pour lequel on peut calculer une proba
	 * algo= construit l'ens. de tous les subtrees possibles
	 * 		 fait l'intersection avec l'ensemble des subtrees du train
	 * 		 recupere les subtrees de taille max
	 * @param g
	 * @param root
	 */
	void getLargestSubtreeFromNode(DetGraph g, int root) {
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		gs.add(g);
		List<DetGraph> subtrees = getAllSubtrees(gs, 10);
		ArrayList<DetGraph> toremove = new ArrayList<DetGraph>();
		// intersection
		for (DetGraph gg : subtrees) {
			DetGraphComparable ggg = new DetGraphComparable(gg);
			boolean isin=false;
			for (List<DetGraphComparable> z : allseqs.values()) {
				if (z.contains(ggg)) {
					isin=true;
					break;
				}
			}
			if (!isin) toremove.add(gg);
		}
		for (DetGraph x : toremove) subtrees.remove(x);
		// ordonne par taille croissante
		Collections.sort(subtrees, new Comparator<DetGraph>() {
			@Override
			public int compare(DetGraph o1, DetGraph o2) {
				int n1 = o1.getNbMots(), n2=o2.getNbMots();
				if (n1>n2) return -1;
				else if (n1<n2) return 1;
				else return 0;
			}
		});
		System.out.println("taille max "+subtrees.get(0).getNbMots());
	}
	
	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("allsubtrees.xml");
		SubTree m = new SubTree();
		m.extractAllPOSseqs(gs, 5);
		
		for (DetGraph g : gs) {
			m.getLargestSubtreeFromNode(g, g.getRoots()[0]);

//			g.conf=m.compProba(g);
		}
//		gio.save(gs, "conf.xml");
	}
	
	/**
	 * extrait toutes les sequences de POStags de la base de subtrees.
	 * Celle-ci doit avoir dans les champs "conf" le nombre d'occurrences du subtree
	 * @param subtrees
	 */
	void extractAllPOSseqs(List<DetGraph> subtrees, int cutoff) {
		for (int i=0;i<subtrees.size();i++) {
			DetGraph subtree = subtrees.get(i);
			POSseq ps = new POSseq(subtree);
			List<DetGraphComparable> treelist = allseqs.get(ps);
			if (treelist==null) {
				treelist = new ArrayList<DetGraphComparable>();
				allseqs.put(ps, treelist);
			}
			treelist.add(new DetGraphComparable(subtree));
		}
		System.out.println("allpos seqs "+allseqs.size());
		if (cutoff>1) {
			ArrayList<POSseq> toremove = new ArrayList<SubTree.POSseq>();
			for (POSseq ps : allseqs.keySet()) {
				if (getNocc(allseqs.get(ps))<cutoff) toremove.add(ps);
			}
			for (POSseq ps : toremove) allseqs.remove(ps);
		}
		System.out.println("... after cutoff "+allseqs.size());
	}
	/**
	 * represente la sequence de POStags associee a un subtree
	 */
	static class POSseq {
		// pour chaque POS = nb de ses occurrences
		HashMap<String,Integer> posleaf2nb = new HashMap<String, Integer>();
		public POSseq(DetGraph subtree) {
			for (int i=0;i<subtree.getNbMots();i++) {
				String pos = subtree.getMot(i).getPOS();
				Integer n=posleaf2nb.get(pos);
				if (n==null) {n=1; posleaf2nb.put(pos, n);} else n++;
			}
		}
		public boolean equals(Object o) {
			POSseq p = (POSseq)o;
			if (posleaf2nb.size()!=p.posleaf2nb.size()) return false;
			for (String s : posleaf2nb.keySet()) {
				Integer pn = p.posleaf2nb.get(s);
				if (pn==null) return false;
				if (pn!=posleaf2nb.get(s)) return false;
			}
			return true;
		}
		public int hashCode() {
			return posleaf2nb.keySet().hashCode();
		}
	}
	
}
