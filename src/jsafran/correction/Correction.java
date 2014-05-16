package jsafran.correction;

import java.util.HashMap;

import jsafran.DetGraph;
import jsafran.JSafran;

public class Correction {
	JSafran js;
	int prevhead=-1;
	String prevlab=null;
	
	public static Correction corr = null;
	public static void mode(JSafran main) {
		if (corr==null) {
			System.out.println("get in correction mode");
			corr = new Correction();
			corr.js=main;
		} else {
			System.out.println("get out correction mode");
			corr=null;
		}
	}
	
	/**
	 * methode appelee juste apres l'edition d'une dep
	 */
	public void getNewGraph() {
		DetGraph g = js.allgraphs.get(js.curgraph);
		
		int gov = js.editword;
		
		if (prevhead<0) {
			// TODO
			return;
		}
		
		// cherche les graphes avec le meme mot+head+lab+posrel
		// TODO: faire cette recherche dans getPrevGraph ?
		String govforme = g.getMot(gov).getForme();
		String headforme = g.getMot(prevhead).getForme();
		int relpos = prevhead-gov;
		js.clearMarks();
		for (int i=0;i<js.allgraphs.size();i++) {
			DetGraph gg = js.allgraphs.get(i);
			for (int j=0;j<gg.getNbMots();j++) {
				int dd = gg.getDep(j);
				if (dd>=0) {
					String xgovforme = gg.getMot(j).getForme();
					if (!xgovforme.equals(govforme)) continue;
					int h = gg.getHead(dd);
					String xheadforme = gg.getMot(h).getForme();
					if (!xheadforme.equals(headforme)) continue;
					int xrelpos = h-j;
					if (xrelpos==relpos) {
						String l = gg.getDepLabel(dd);
						if (!l.equals(prevlab)) continue;
						
						// on a trouve un match !
						js.addMark(i, j);
					}
				}
			}
		}
	}
	
	/**
	 * methode appelee juste avant l'edition d'une dep
	 */
	public void getPrevGraph() {
		// les level, graph, gov word ne changent pas
		// seuls le head et label changent
		DetGraph g = js.allgraphs.get(js.curgraph);
		int gov = js.editword;
		int d = g.getDep(gov);
		if (d<0) {
			prevhead=-1;
		} else {
			prevhead=g.getHead(d);
			prevlab =g.getDepLabel(d);
		}
	}
}
