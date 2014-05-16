package utils.ml.edmonds;

import gnu.trove.TIntIntHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChuLiu {


	/**
	 * This method is almost a exact copy of the chuLiuEdmonds algortihm from the MSTParser-project
	 *   http://sourceforge.net/projects/mstparser/
	 */
	public static List<Integer> chuLiuEdmonds(float[][] scores, int root) {
		int numWords = scores.length;

		int[][] origIn = new int[numWords][numWords];
		int[][] origOut = new int[numWords][numWords];
		boolean[] currNodes = new boolean[numWords];
		TIntIntHashMap[] reps = new TIntIntHashMap[numWords];
		for (int i = 0; i < numWords; i++) {
			currNodes[i] = true;
			reps[i] = new TIntIntHashMap();
			reps[i].put(i, 0);
			for (int j = 0; j < numWords; j++) {
				origIn[i][j] = i;
				origOut[i][j] = j;
			}
		}

		TIntIntHashMap final_edges = chuLiuEdmondsPrivate(scores, currNodes, origIn, origOut, false, new TIntIntHashMap(), reps, root);


		Integer[] heads = new Integer[numWords];
		for (int from : final_edges.keys()) {
			heads[from] = final_edges.get(from);
		}


		return Arrays.asList(heads);
	}

	private static TIntIntHashMap chuLiuEdmondsPrivate(float[][] scoreMatrix, boolean[] curr_nodes,
			int[][] oldI, int[][] oldO, boolean print,
			TIntIntHashMap final_edges, TIntIntHashMap[] reps, int root) {

		// need to construct for each node list of nodes they represent (here only!)

		int[] par = new int[curr_nodes.length];
		int numWords = curr_nodes.length;

		// create best graph
		par[root] = -1;
		for (int i = 0; i < par.length; i++) {
			if (i==root) continue;
			// only interested in current nodes
			if (!curr_nodes[i]) {
				continue;
			}
			//            float maxScore = scoreMatrix[0][i];
			//            par[i] = 0;
			float maxScore = Float.NEGATIVE_INFINITY;
			par[i]=-1;
			for (int j = 0; j < par.length; j++) {
				if (j == i) {
					continue;
				}
				if (!curr_nodes[j]) {
					continue;
				}
				float newScore = scoreMatrix[j][i];
				if (newScore > maxScore) {
					maxScore = newScore;
					par[i] = j;
				}
			}
		}

		if (Edmonds.debug) {
			System.out.println("After init");
			for (int i = 0; i < par.length; i++) {
				if (curr_nodes[i]) {
					System.out.print(par[i] + "|" + i + " ");
				}
			}
			System.out.println();
		}

		//Find a cycle
		ArrayList cycles = new ArrayList();
		boolean[] added = new boolean[numWords];
		for (int i = 0; i < numWords && cycles.size() == 0; i++) {
			// if I have already considered this or
			// This is not a valid node (i.e. has been contracted)
			if (added[i] || !curr_nodes[i]) {
				continue;
			}
			added[i] = true;
			TIntIntHashMap cycle = new TIntIntHashMap();
			cycle.put(i, 0);
			int l = i;
			while (true) {
				if (par[l] == -1) {
					added[l] = true;
					break;
				}
				if (cycle.contains(par[l])) {
					cycle = new TIntIntHashMap();
					int lorg = par[l];
					cycle.put(lorg, par[lorg]);
					added[lorg] = true;
					int l1 = par[lorg];
					while (l1 != lorg) {
						cycle.put(l1, par[l1]);
						added[l1] = true;
						l1 = par[l1];

					}
					cycles.add(cycle);
					break;
				}
				cycle.put(l, 0);
				l = par[l];
				if (added[l] && !cycle.contains(l)) {
					break;
				}
				added[l] = true;
			}
		}

		if (Edmonds.debug)
			System.out.println("debug cycles "+cycles);

		// get all edges and return them
		if (cycles.size() == 0) {
			if (Edmonds.debug) System.out.println("TREE:");
			for (int i = 0; i < par.length; i++) {
				if (!curr_nodes[i]) {
					continue;
				}
				if (par[i] != -1) {
					int pr = oldI[par[i]][i];
					int ch = oldO[par[i]][i];
					final_edges.put(ch, pr);
					if (Edmonds.debug) System.out.print(pr+"|"+ch + " ");
				} else {
					// this does not work if we use a root that is not the first word !
					//					final_edges.put(0, -1);
				}
			}
			if (Edmonds.debug) System.out.println();
			return final_edges;
		}

		int max_cyc = 0;
		int wh_cyc = 0;
		for (int i = 0; i < cycles.size(); i++) {
			TIntIntHashMap cycle = (TIntIntHashMap) cycles.get(i);
			if (cycle.size() > max_cyc) {
				max_cyc = cycle.size();
				wh_cyc = i;
			}
		}

		TIntIntHashMap cycle = (TIntIntHashMap) cycles.get(wh_cyc);
		int[] cyc_nodes = cycle.keys();
		int rep = cyc_nodes[0];

		if (Edmonds.debug) {
			System.out.println("Found Cycle");
			for (int i = 0; i < cyc_nodes.length; i++) {
				System.out.print(cyc_nodes[i] + " ");
			}
			System.out.println();
		}

		float cyc_weight = 0f;
		for (int j = 0; j < cyc_nodes.length; j++) {
			cyc_weight += scoreMatrix[par[cyc_nodes[j]]][cyc_nodes[j]];
		}


		for (int i = 0; i < numWords; i++) {

			if (!curr_nodes[i] || cycle.contains(i)) {
				continue;
			}

			// cherche les arcs entre le cycle et le node i
			float max1 = Float.NEGATIVE_INFINITY;
			int wh1 = -1;
			float max2 = Float.NEGATIVE_INFINITY;
			int wh2 = -1;

			for (int j = 0; j < cyc_nodes.length; j++) {
				int j1 = cyc_nodes[j];

				if (scoreMatrix[j1][i] > max1) {
					max1 = scoreMatrix[j1][i];
					wh1 = j1;//oldI[j1][i];
				}

				// cycle weight + new edge - removal of old
				float scr = cyc_weight + scoreMatrix[i][j1] - scoreMatrix[par[j1]][j1];
				if (scr > max2) {
					max2 = scr;
					wh2 = j1;//oldO[i][j1];
				}
			}

			// un noeud peut ne pas etre lie au cycle !
			if (wh1>=0) {
				scoreMatrix[rep][i] = max1;
				oldI[rep][i] = oldI[wh1][i];//wh1;
				oldO[rep][i] = oldO[wh1][i];//oldO[wh1][i];
			}
			if (wh2>=0) {
				scoreMatrix[i][rep] = max2;
				oldO[i][rep] = oldO[i][wh2];//wh2;
				oldI[i][rep] = oldI[i][wh2];//oldI[i][wh2];
			}
		}

		TIntIntHashMap[] rep_cons = new TIntIntHashMap[cyc_nodes.length];
		for (int i = 0; i < cyc_nodes.length; i++) {
			rep_cons[i] = new TIntIntHashMap();
			int[] keys = reps[cyc_nodes[i]].keys();
			Arrays.sort(keys);
			if (Edmonds.debug) {
				System.out.print(cyc_nodes[i] + ": ");
			}
			for (int j = 0; j < keys.length; j++) {
				rep_cons[i].put(keys[j], 0);
				if (Edmonds.debug) {
					System.out.print(keys[j] + " ");
				}
			}
			if (Edmonds.debug) {
				System.out.println();
			}
		}

		// don't consider not representative nodes
		// these nodes have been folded
		for (int i = 1; i < cyc_nodes.length; i++) {
			curr_nodes[cyc_nodes[i]] = false;
			int[] keys = reps[cyc_nodes[i]].keys();
			for (int j = 0; j < keys.length; j++) {
				reps[rep].put(keys[j], 0);
			}
		}

		chuLiuEdmondsPrivate(scoreMatrix, curr_nodes, oldI, oldO, print, final_edges, reps, root);

		// check each node in cycle, if one of its representatives
		// is a key in the final_edges, it is the one.
		int wh = -1;
		boolean found = false;
		for (int i = 0; i < rep_cons.length && !found; i++) {
			int[] keys = rep_cons[i].keys();
			for (int j = 0; j < keys.length && !found; j++) {
				if (final_edges.contains(keys[j])) {
					wh = cyc_nodes[i];
					found = true;
				}
			}
		}

		if (!found) System.out.println("ERROR "+Arrays.toString(final_edges.keys()));
		else {
			int l = par[wh];
			while (l != wh) {
				int ch = oldO[par[l]][l];
				int pr = oldI[par[l]][l];
				final_edges.put(ch, pr);
				l = par[l];
			}
		}

		if (Edmonds.debug) {
			int[] keys = final_edges.keys();
			Arrays.sort(keys);
			for (int i = 0; i < keys.length; i++) {
				System.out.print(final_edges.get(keys[i]) + "|" + keys[i] + " ");
			}
			System.out.println();
		}

		return final_edges;

	}

	public static void main(String[] args) {

		{
			float ifn = Float.NEGATIVE_INFINITY;

			float[][] arcs = {
					{ifn,ifn,ifn,ifn,ifn,ifn,ifn},
					{ifn,ifn,ifn,ifn,ifn,ifn,ifn},
					{1,1,ifn,1,1,ifn,1},
					{ifn,ifn,ifn,ifn,ifn,ifn,ifn},
					{ifn,ifn,ifn,1,ifn,ifn,ifn},
					{ifn,ifn,ifn,ifn,ifn,ifn,ifn},
					{ifn,ifn,ifn,ifn,ifn,1,ifn}
			};
			//			float[][] arcs = {
			//					{0,0,1,0,0,0,0},
			//					{0,0,1,0,0,0,0},
			//					{0,0,0,0,0,0,0},
			//					{0,0,1,0,1,0,0},
			//					{0,0,1,0,0,0,0},
			//					{0,0,1,0,0,0,0},
			//					{0,0,0,0,0,0,1}
			//			};

			List<Integer> res = ChuLiu.chuLiuEdmonds(arcs,2);
			System.out.println(res);
		}
	}
}
