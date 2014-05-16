package jsafran.parsing;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import utils.FileUtils;

//import plugins.utils.FileUtils;

public class FSM {
	final String[] labs = {"NA","LA1","RA1"};
	// certains etats ne peuvent apparaitre qu'en un seul exemplaire.
	final int[] forcemerge = {1,2};
	
	class Node {
		int act;
		int[] next=null;
	}
	List<Node> nodes = new ArrayList();
	
	public FSM() {
		Node n = new Node(); n.act=-1;
		nodes.add(n);
	}
	
	// profondeur max pour determiner si 2 sous-arbres sont identiques
	final int maxdepth=2;
	
	boolean checksame(Node n1, Node n2, int depth) {
		if (depth>=maxdepth) return true;
		if (n1.act!=n2.act) return false;
		if (Arrays.binarySearch(forcemerge, n1.act)>=0) return true;
		if (n1.next==null&&n2.next==null) return true;
		if (n1.next==null||n2.next==null) return false;
		if (n1.next.length!=n2.next.length) return false;
		for (int i=0;i<n1.next.length;i++) {
			// on n'a pas besoin de tester toutes les combinaisons, car l'ordre dans lequel les etats suivants sont ajoutes est tjs le meme !
			if (n1.next[i]<0&&n2.next[i]<0) continue;
			if (n1.next[i]<0||n2.next[i]<0) return false;
			if (!checksame(nodes.get(n1.next[i]), nodes.get(n2.next[i]),depth+1)) return false;
		}
		return true;
	}
	
	int findSameSubtree(int n0) {
		Node nn0 = nodes.get(n0);
		for (int i=0;i<n0;i++) {
			Node x = nodes.get(i);
			if (checksame(nn0,x,0)) {
				return i;
			}
		}
		return -1;
	}
	
	void recurs(int nodeidx, int[] deps, int t) {
		if (t>=deps.length) {
			System.out.println(Arrays.toString(deps));
			return;
		}
		
		Node n = nodes.get(nodeidx);
		switch(n.act) {
		case -1: break;
		case 0: deps[t]=t; break;
		case 1: deps[t]=t+1; break;
		case 2: deps[t]=t-1; break;
		}
		if (n.next==null) {
			// il faut etendre !
			n.next = new int[labs.length];
			Arrays.fill(n.next,-1);
			// try NA
			boolean naok = true;
			for (int i=0;i<t;i++)
				if (deps[i]>t) {naok=false; break;}
			if (naok) {
				Node nn = new Node(); nn.act=0;
				n.next[0]=nodes.size();
				nodes.add(nn);
				int[] newdeps = Arrays.copyOf(deps, deps.length);
				recurs(n.next[0],newdeps,t+1);
				// quand on arrive ici, on peut regarder si il existe un autre sous-graphe semblable. Si oui, on merge
				int autrenode = findSameSubtree(n.next[0]);
				if (autrenode>=0) {
					nodes = nodes.subList(0, n.next[0]);
					n.next[0]=autrenode;
				}
			}
			
			// on essaye ici les actions possibles a t+1
			// try LA1
			if (t+1<deps.length-1) {
				Node nn = new Node(); nn.act=1;
				n.next[1]=nodes.size();
				nodes.add(nn);
				int[] newdeps = Arrays.copyOf(deps, deps.length);
				recurs(n.next[1],newdeps,t+1);
				// quand on arrive ici, on peut regarder si il existe un autre sous-graphe semblable. Si oui, on merge
				int autrenode = findSameSubtree(n.next[1]);
				if (autrenode>=0) {
					nodes = nodes.subList(0, n.next[1]);
					n.next[1]=autrenode;
				}
			}
			// try RA1
			if (t+1>0&&deps[t]!=t+1) {
				Node nn = new Node(); nn.act=2;
				n.next[2]=nodes.size();
				nodes.add(nn);
				int[] newdeps = Arrays.copyOf(deps, deps.length);
				recurs(n.next[2],newdeps,t+1);
				// quand on arrive ici, on peut regarder si il existe un autre sous-graphe semblable. Si oui, on merge
				int autrenode = findSameSubtree(n.next[2]);
				if (autrenode>=0) {
					nodes = nodes.subList(0, n.next[2]);
					n.next[2]=autrenode;
				}
			}
		} else System.out.println("zarbi...");
	}
	
	void printGraphDot() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph XX {\n");
		for (int i=0;i<nodes.size();i++) {
			Node n = nodes.get(i);
			if (n.next==null) continue;
			for (int dest : n.next) {
				if (dest<0) continue;
				Node n2 = nodes.get(dest);
				String s1 = "S0", s2="S0";
				if (n.act>=0) s1=labs[n.act]+"_"+i;
				if (n2.act>=0) s2=labs[n2.act]+"_"+dest;
				sb.append(s1+" -> "+s2+" ;\n");
			}
		}
		sb.append("}");
		try {
			PrintWriter f = FileUtils.writeFileUTF("gg.dot");
			f.println(sb.toString());
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		int[] deps = new int[10];
		Arrays.fill(deps,-1);
		FSM m =new FSM();
		m.recurs(0,deps,-1);
		System.out.println("nnodes "+m.nodes.size());
		m.printGraphDot();
	}
}
