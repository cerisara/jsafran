package jsafran.searchfilters;

import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import jsafran.DetGraph;
import jsafran.JSafran;

public class QuickSearch {
	List<DetGraph> graphs;
	String dep2find = null;
	int curg=0, curw=-1;

	static String lastsearch = "";
	static QuickSearch lastquery=null;

	public static void searchWord(JSafran main) {
		String w = main.allgraphs.get(main.curgraph).getMot(main.editword).getForme();
		int n=0;
		for (int i=0;i<main.allgraphs.size();i++) {
			DetGraph g = main.allgraphs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				if (g.getMot(j).getForme().equals(w)) {
					main.addMark(i,j);
					n++;
				}
			}
		}
		System.out.println("words found: "+n);
	}
	
	public QuickSearch(List<DetGraph> gs) {
		graphs=gs;
	}
	private int[] lookForNextOccurence() {
		if (lastsearch.startsWith("d=")) return lookForNextDep(lastsearch.substring(2));
		else if (lastsearch.startsWith("p=")) return lookForNextPos(lastsearch.substring(2));
		else if (lastsearch.startsWith("w=")) return lookForNextWord(lastsearch.substring(2));
		else return lookForNextWord(lastsearch);
	}
	private int[] lookForNextDep(String dep2find) {
		for (;curg<graphs.size();curg++) {
			for (++curw;curw<graphs.get(curg).getNbMots();++curw) {
				int[] deps = graphs.get(curg).getDeps(curw);
				for (int d : deps) {
					if (graphs.get(curg).getDepLabel(d).equals(dep2find)) {
						int[] r = {curg,curw}; return r;
					}
				}
			}
			curw=-1;
		}
		return null;
	}
	private int[] lookForNextPos(String pos2find) {
		for (;curg<graphs.size();curg++) {
			for (++curw;curw<graphs.get(curg).getNbMots();++curw) {
				if (graphs.get(curg).getMot(curw).getPOS().equals(pos2find)) {
					int[] r = {curg,curw}; return r;
				}
			}
			curw=-1;
		}
		return null;
	}
	private int[] lookForNextWord(String word2find) {
		for (;curg<graphs.size();curg++) {
			for (++curw;curw<graphs.get(curg).getNbMots();++curw) {
				if (graphs.get(curg).getMot(curw).getForme().equals(word2find)) {
					int[] r = {curg,curw}; return r;
				}
			}
			curw=-1;
		}
		return null;
	}

	public static void nextsearch(JSafran m) {
		int[] r = lastquery.lookForNextOccurence();
		System.out.println("quick search res "+Arrays.toString(r));
		if (r!=null) {
			m.curgraph=r[0];
			m.editword=r[1]-1;
			m.gotoword(1);
		}
	}

	public static QuickSearch qsearch(JSafran m) {
		QuickSearch qs = new QuickSearch(m.allgraphs);
		lastquery=qs;
		String se = JOptionPane.showInputDialog("enter [d=] [p=] label to find",lastsearch);
		lastsearch=se;
		nextsearch(m);
		return qs;
	}
}
