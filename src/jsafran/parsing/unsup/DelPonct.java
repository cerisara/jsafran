package jsafran.parsing.unsup;

import java.util.List;

import javax.swing.JOptionPane;

import jsafran.DetGraph;

public class DelPonct {
	public static enum from {pos,form,dep};
	private static from fromt=null;
	private static String[] stpunct = null;
	
	public static void delPonct(List<DetGraph> gs) {
		final String[] opts = {"From POS","From forms","From deps"};
		int rep = JOptionPane.showOptionDialog(null, "How do you define punct ?", "define punct", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, null);
		if (rep<0||rep>2) {
			System.out.println("ponct: doing nothing... "+rep);
			return;
		}
		switch(rep) {
		case 0: delPonctPOS(gs); break;
		case 1: delPonctForms(gs); break;
		case 2: delPonctDeps(gs); break;
		}
		delPonct(gs,fromt,stpunct);
	}
	
	private static int getNonPunctAncestor(DetGraph g, int w) {
		int d=g.getDep(w);
		while (d>=0) {
			int h=g.getHead(d);
			if (!isPonct(g, h)) return h;
			d=g.getDep(h);
		}
		return -1;
	}
	
	public static void delPonct(List<DetGraph> gs, from fromt, String[] stpunct) {
		for (int i=gs.size()-1;i>=0;i--) {
			DetGraph g = gs.get(i);
			for (int j=g.getNbMots()-1;j>=0;j--) {
				if (isPonct(g, j)) {
					List<Integer> fils = g.getFils(j);
					if (fils.size()>0) {
						int h = getNonPunctAncestor(g,j);
						if (h>=0)
							for (int f : fils) {
								int d=g.getDep(f);
								String l = g.getDepLabel(d);
								g.deps.remove(d);
								g.ajoutDep(l, f, h);
							}
					}
					g.delNodes(j, j);
				}
			}
		}
	}
	public static boolean isIn(String x, String[] y) {
		for (String s : y) if (s.equals(x)) return true;
		return false;
	}
	public static boolean isPonct(DetGraph g, int mot) {
		switch (fromt) {
		case pos: return isIn(g.getMot(mot).getPOS(),stpunct);
		case form: return isIn(g.getMot(mot).getForme(),stpunct);
		case dep:
			int d = g.getDep(mot);
			if (d<0) return isIn("ROOT",stpunct);
			return isIn(g.getDepLabel(d),stpunct);
		default: return false;
		}
	}
	public static void delPonctPOS(List<DetGraph> gs) {
		fromt = from.pos;
		String postlist = JOptionPane.showInputDialog("Enter POStags separated by a single whitespace");
		stpunct = postlist.trim().split(" ");
	}		

	public static void delPonctForms(List<DetGraph> gs) {
		fromt = from.form;
		String postlist = JOptionPane.showInputDialog("Enter forms separated by a single whitespace");
		stpunct = postlist.trim().split(" ");
	}
	public static void delPonctDeps(List<DetGraph> gs) {
		fromt = from.dep;
		String postlist = JOptionPane.showInputDialog("Enter deps separated by a single whitespace");
		stpunct = postlist.trim().split(" ");
	}
}
