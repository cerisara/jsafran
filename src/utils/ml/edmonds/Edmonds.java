package utils.ml.edmonds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import utils.Wait;

/**
 * Performs a minimum spanning tree on a directed graph.
 * Input = double array float[nwords(gov)][nwords(head)] that contains the probabilities of an arc
 * 0 means that no arc is possible.
 * It is actually the highest prob, because when several arc labels can be assigned btw two words, the Edmonds algorithm only considers the best possible one.
 * In case of equal weights, this is up to the caller to choose one of the label randomly.
 * 
 * Note that this may create a problem with rules that generate several arcs, because the Edmonds algorithm does not take into account correlations between arcs.
 * In other words, every arc is atomic.
 * 
 * Internally, all probs are converted into log so that the global score can be summed up.
 * Output = the list of arcs to keep, in the form [root,total logprob,gov,head,gov,head,...]
 * 
 * PROBLEM: we only get one of the MST !
 * We may get around this issue by adding a small amount of noise to the weights; then, getting the MST is equivalent to randomly sampling one of the MST ?
 * 
 * WARNING: the graph passed to this class shall only contain nodes that are linked to the proposed ROOT (?)
 * 
 * 
 * @author xtof
 *
 */
public class Edmonds {

	public final static boolean debug=true;
	
	private static void getSubgraph(TreeSet<Integer> subgraph, float arcs[][], int root) {
		for (int i=0;i<arcs[root].length;i++)
			if (!subgraph.contains(i) && arcs[root][i]>0) {
				subgraph.add(i);
				getSubgraph(subgraph, arcs, i);
			}
	}
	
	public static float[] getMST(float arcs[][]) {
		if (debug) System.out.println("GET MST "+arcs.length);
		final int nnodes=arcs.length;

		if (false&&debug)
			for (int i=0;i<nnodes;i++) {
				for (int j=0;j<nnodes;j++)
					System.out.print(arcs[i][j]+" ");
				System.out.println();
			}

		// Edmonds m = new Edmonds();
		ChuLiu m = new ChuLiu();
		
		// we have several independent trees, so we have to add them all in the final sentence tree
		// but for each tree, we have several possible best root, so we also have to compare all root options
		ArrayList<TreeSet<Integer>> bestTreesNodes = new ArrayList<TreeSet<Integer>>();
		ArrayList<List<Integer>> bestTrees = new ArrayList<List<Integer>>();
//		ArrayList<AdjacencyList> bestTrees = new ArrayList<Edmonds.AdjacencyList>();
		ArrayList<Float> bestTreesScore = new ArrayList<Float>();
		ArrayList<Integer> bestTreesRoot = new ArrayList<Integer>();
		
		for (int root=0;root<nnodes;root++) {
			// check that this node can be root
			// No: this is not a valid condition, because we can remove arcs ! 
//			boolean hasHead=false;
//			for (int j=0;j<nnodes;j++)
//				if (arcs[j][root]>0) {hasHead=true; break;}
//			if (hasHead) continue;
			
			boolean hasChildren=false;
			for (int j=0;j<nnodes;j++)
				if (arcs[root][j]>0) {hasChildren=true; break;}
			if (!hasChildren) continue;

			if (debug) System.out.println("test root "+root+" "+nnodes);

			// OK it can be root !

			// is it part of an already existing tree ?
			int alreadySeenTree = -1;
			for (int i=0;i<bestTreesNodes.size();i++)
				if (bestTreesNodes.get(i).contains(root)) {
					alreadySeenTree=i; break;
				}
			TreeSet<Integer> subgraph;
			if (alreadySeenTree<0) {
				// we still have to extract all of its descendants
				subgraph = new TreeSet<Integer>();
				subgraph.add(root);
				getSubgraph(subgraph,arcs,root);
				if (debug) System.out.println("CREATE NEW TREE "+subgraph);
				// si le subgraph created subsume un previous subgraph, il faut detruire le previous subgraph
				for (int i=bestTreesNodes.size()-1;i>=0;i--) {
					if (subgraph.contains(bestTreesNodes.get(i).first())) {
						bestTrees.remove(i);
						bestTreesNodes.remove(i);
						bestTreesRoot.remove(i);
						bestTreesScore.remove(i);
					}
				}
				bestTreesNodes.add(subgraph);
				bestTreesRoot.add(root);
				alreadySeenTree=bestTreesScore.size();
			} else subgraph=bestTreesNodes.get(alreadySeenTree);
			
			// reduce the transition matrix to the subgraph
			Integer[] wordsInThisSubtree = subgraph.toArray(new Integer[subgraph.size()]);
//			Node[] subgraphnodes = new Node[subgraph.size()];
//			for (int i=0;i<subgraphnodes.length;i++) subgraphnodes[i]=new Node(i);
//			AdjacencyList g = new AdjacencyList();
//			for (int i=0;i<wordsInThisSubtree.length;i++)
//				for (int j=0;j<wordsInThisSubtree.length;j++)
//					if (i!=j && arcs[wordsInThisSubtree[i]][wordsInThisSubtree[j]]>0) {
//						g.addEdge(subgraphnodes[i], subgraphnodes[j], (float)Math.log(arcs[wordsInThisSubtree[i]][wordsInThisSubtree[j]]));
//					}
			
			int subgraphroot = Arrays.binarySearch(wordsInThisSubtree, root);
			
			// transforms the Edmonds structure to the ChuLiu structures
			float[][] subarcs = new float[wordsInThisSubtree.length][wordsInThisSubtree.length];
			for (int i=0;i<subarcs.length;i++) Arrays.fill(subarcs[i], Float.NEGATIVE_INFINITY);
			for (int i=0;i<wordsInThisSubtree.length;i++)
				for (int j=0;j<wordsInThisSubtree.length;j++)
					if (i!=j && arcs[wordsInThisSubtree[i]][wordsInThisSubtree[j]]>0) {
						subarcs[i][j]=arcs[wordsInThisSubtree[i]][wordsInThisSubtree[j]];
					}
			
			if (false&&debug)
				for (int i=0;i<subarcs.length;i++) {
					for (int j=0;j<subarcs.length;j++)
						System.out.print(subarcs[i][j]+" ");
					System.out.println();
				}
			
			List<Integer> resg = ChuLiu.chuLiuEdmonds(subarcs,subgraphroot);
			
			//AdjacencyList resg = m.getMinBranching(subgraphnodes[subgraphroot], g);
			
			float sc = 0;
//			for (Edge e : resg.getAllEdges()) {
//				sc+=e.weight;
//			}
			for (int i=0;i<resg.size();i++) {
				if (resg.get(i)!=null&&resg.get(i)>=0) sc+=subarcs[i][resg.get(i)];
			}
			
			if (alreadySeenTree>=bestTreesScore.size()) {
				// c'est un nouvel arbre
				bestTrees.add(resg);
				bestTreesScore.add(sc);
			} else {
				if (sc>bestTreesScore.get(alreadySeenTree)) {
					bestTreesRoot.set(alreadySeenTree, root);
					bestTreesScore.set(alreadySeenTree, sc);
					bestTrees.set(alreadySeenTree, resg);
				}
			}
			if (debug) System.out.println("ROOT "+root+" "+bestTreesScore);
		}
		
		if (debug) {
			// check that the trees are indeed independent
			for (int i=0;i<bestTrees.size();i++) {
				System.out.println("CHECKTREE "+bestTreesRoot.get(i)+" "+bestTreesNodes.get(i));
				
			}
		}
		
		// we have computed all best independent trees for the sentence
		// we now have to apply all of them
		if (bestTrees.size()==0) return null;
		int ndeps = 0;
		for (int i=0;i<bestTrees.size();i++) {
			List<Integer> btree = bestTrees.get(i);
			if (btree!=null) {
//				ndeps+=bestTrees.get(i).getAllEdges().size();
				int n=0;
				for (Integer d : btree) if (d!=null&&d>=0) n++;
				ndeps+=n;
			}
		}
		float[] r = new float[ndeps*2+2];
		r[0]=bestTreesRoot.get(0); // TODO: this one is useless
		r[1]=0f;
		for (int i=0,j=2;i<bestTrees.size();i++) {
			r[1]+=bestTreesScore.get(i);
			Integer[] wordsInThisSubtree = bestTreesNodes.get(i).toArray(new Integer[bestTreesNodes.get(i).size()]);
//			Collection<Edge> deps = bestTrees.get(i).getAllEdges();
			List<Integer> heads = bestTrees.get(i);
//			for (Edge e : deps) {
			for (int govReduced=0;govReduced<heads.size();govReduced++) {
				Integer headReduced = heads.get(govReduced);
				if (headReduced!=null&&headReduced>=0) {
					int headInit = wordsInThisSubtree[headReduced];
					r[j++]=headInit;
					int govInit = wordsInThisSubtree[govReduced];
					r[j++]=govInit;
				}
			}
		}
		return r;
	}

	public static class Node implements Comparable<Node> {

		final int name;
		boolean visited = false;   // used for Kosaraju's algorithm and Edmonds's algorithm
		int lowlink = -1;          // used for Tarjan's algorithm
		int index = -1;            // used for Tarjan's algorithm

		public Node(final int argName) {
			name = argName;
		}

		public String toString() {return ""+name;}
		public int compareTo(final Node argNode) {
			return argNode == this ? 0 : -1;
		}
	}

	public static class Edge implements Comparable<Edge> {

		final Node from, to; 
		final float weight;

		public Edge(final Node argFrom, final Node argTo, final float argWeight){
			from = argFrom;
			to = argTo;
			weight = argWeight;
		}

		public int compareTo(final Edge argEdge){
			if (weight>argEdge.weight) return 1;
			else if (weight<argEdge.weight) return -1;
			else return 0;
		}

		public String toString() {
			return from.name+"->"+to.name+":"+weight;
		}
	}

	public static class AdjacencyList {

		private Map<Node, List<Edge>> adjacencies = new HashMap<Node, List<Edge>>();

		public void addEdge(Node source, Node target, float weight) {
			List<Edge> list;
			if(!adjacencies.containsKey(source)) {
				list = new ArrayList<Edge>();
				adjacencies.put(source, list);
			} else {
				list = adjacencies.get(source);
			}
			list.add(new Edge(source, target, weight));
		}

		public List<Edge> getAdjacent(Node source) {
			return adjacencies.get(source);
		}

		public void reverseEdge(Edge e) {
			adjacencies.get(e.from).remove(e);
			addEdge(e.to, e.from, e.weight);
		}

		public void reverseGraph() {
			adjacencies = getReversedList().adjacencies;
		}

		public AdjacencyList getReversedList() {
			AdjacencyList newlist = new AdjacencyList();
			for(List<Edge> edges : adjacencies.values()) {
				for(Edge e : edges) {
					newlist.addEdge(e.to, e.from, e.weight);
				}
			}
			return newlist;
		}

		public Set<Node> getSourceNodeSet() {
			return adjacencies.keySet();
		}

		public Collection<Edge> getAllEdges() {
			List<Edge> edges = new ArrayList<Edge>();
			for(List<Edge> e : adjacencies.values()) {
				edges.addAll(e);
			}
			return edges;
		}
		public String toString() {
			return ""+getAllEdges();
		}
	}

	private ArrayList<Node> cycle;

	public AdjacencyList getMinBranching(Node root, AdjacencyList list){
		AdjacencyList reverse = list.getReversedList();
		// remove all edges entering the root
		if(reverse.getAdjacent(root) != null){
			if (debug) System.out.println("remove edge entering root: "+root+": "+reverse.getAdjacent(root));
			reverse.getAdjacent(root).clear();
		}
		if (debug) System.out.println("edges remaining: "+reverse);
		
		AdjacencyList outEdges = new AdjacencyList();
		// for each node, select the edge entering it with smallest weight
		for(Node n : reverse.getSourceNodeSet()){
			List<Edge> inEdges = reverse.getAdjacent(n);
			if(inEdges.isEmpty()) continue;
			Edge min = inEdges.get(0);
			for(Edge e : inEdges){
				if(e.weight < min.weight){
					min = e;
				}
			}
			outEdges.addEdge(min.to, min.from, min.weight);
		}

		if (debug) System.out.println("after edge selection "+outEdges);
		
		// detect cycles
		ArrayList<ArrayList<Node>> cycles = new ArrayList<ArrayList<Node>>();
		cycle = new ArrayList<Node>();
		getCycle(root, outEdges);
		cycles.add(cycle);
		for(Node n : outEdges.getSourceNodeSet()){
			if(!n.visited){
				cycle = new ArrayList<Node>();
				getCycle(n, outEdges);
				cycles.add(cycle);
			}
		}

		// for each cycle formed, modify the path to merge it into another part of the graph
		AdjacencyList outEdgesReverse = outEdges.getReversedList();

		for(ArrayList<Node> x : cycles){
			if(x.contains(root)) continue;
			mergeCycles(x, list, reverse, outEdges, outEdgesReverse);
		}
		return outEdges;
	}

	private void mergeCycles(ArrayList<Node> cycle, AdjacencyList list, AdjacencyList reverse, AdjacencyList outEdges, AdjacencyList outEdgesReverse){
		ArrayList<Edge> cycleAllInEdges = new ArrayList<Edge>();
		Edge minInternalEdge = null;
		// find the minimum internal edge weight
		if (debug) System.out.println("debugcycle "+cycle);
		for(Node n : cycle){
			if (debug) {
				System.out.println("debugforward "+list.getAllEdges());
				System.out.println("debug "+reverse.getAllEdges());
				System.out.println("db2 "+n+" "+reverse.getAdjacent(n));
			}
			for(Edge e : reverse.getAdjacent(n)){
				if(cycle.contains(e.to)){
					if(minInternalEdge == null || minInternalEdge.weight > e.weight){
						minInternalEdge = e;
						continue;
					}
				}else{
					cycleAllInEdges.add(e);
				}
			}
		}
		// find the incoming edge with minimum modified cost
		Edge minExternalEdge = null;
		float minModifiedWeight = 0;
		for(Edge e : cycleAllInEdges){
			float w = e.weight - (outEdgesReverse.getAdjacent(e.from).get(0).weight - minInternalEdge.weight);
			if(minExternalEdge == null || minModifiedWeight > w){
				minExternalEdge = e;
				minModifiedWeight = w;
			}
		}
		// add the incoming edge and remove the inner-circuit incoming edge
		Edge removing = outEdgesReverse.getAdjacent(minExternalEdge.from).get(0);
		outEdgesReverse.getAdjacent(minExternalEdge.from).clear();
		outEdgesReverse.addEdge(minExternalEdge.to, minExternalEdge.from, minExternalEdge.weight);
		List<Edge> adj = outEdges.getAdjacent(removing.to);
		for(Iterator<Edge> i = adj.iterator(); i.hasNext(); ){
			if(i.next().to == removing.from){
				i.remove();
				break;
			}
		}
		outEdges.addEdge(minExternalEdge.to, minExternalEdge.from, minExternalEdge.weight);
	}

	// actually visit all the nodes that can be visited from n and put all of them in "cycle"
	private void getCycle(Node n, AdjacencyList outEdges){
		n.visited = true;
		cycle.add(n);
		if(outEdges.getAdjacent(n) == null) return;
		for(Edge e : outEdges.getAdjacent(n)){
			if(!e.to.visited){
				getCycle(e.to, outEdges);
			}
		}
	}

	public static void main(String args[]) {
		Node[] nodes = new Node[4];
		for (int i=0;i<nodes.length;i++) nodes[i]=new Node(i);
		AdjacencyList g = new AdjacencyList();
		g.addEdge(nodes[1], nodes[0], 10);
		g.addEdge(nodes[1], nodes[2], 10);
		g.addEdge(nodes[1], nodes[3], 10);
		g.addEdge(nodes[3], nodes[2], 10);
		Edmonds m = new Edmonds();
		AdjacencyList gg = m.getMinBranching(nodes[1], g);
		System.out.println(gg.getAllEdges());

		float[][] arcs = {
				{0,0,0,0},
				{1,0,1,1},
				{0,0,0,0},
				{0,0,1,0}};
		float[] res = m.getMST(arcs);
		System.out.println(Arrays.toString(res));
	}
}
