package jsafran.parsing.unsup.rules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;

/**
 * Complete les mots lies par aucune dep
 * 
 * @author xtof
 *
 */
public class EndRules implements Rules, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public int getNrules() {
//		return 1;
		return 0;
	}

	@Override
	public int[] getApplicable(int ridx, DetGraph g) {
//		if (ridx==0) return adj_getApplicable(g);
		return null;
	}

	@Override
	public double getRulePrior(int ridx) {
		return 0.1;
	}

	@Override
	public int[] applyRule(int ridx, DetGraph g, int pos) {
		if (ridx==0) return adj_applyRule(g,pos);
		return null;
	}

	@Override
	public void applyDetRules(DetGraph g) {
	}

	@Override
	public String toString(int ridx) {
		return "endrules";
	}

	@Override
	public void init(List<DetGraph> gs) {
	}

        @Override
        public void setSRL(boolean srl){

        };


	// ========================================================
	int[] adj_getApplicable(DetGraph g) {
		ArrayList<Integer> pp = new ArrayList<Integer>();
		for (int i=1;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d>=0) continue;
			if (g.getMot(i).getPOS().equals("ADJ")) {
				pp.add(i);
			}
		}
		int[] res = new int[pp.size()];
		for (int i=0;i<res.length;i++) res[i] = pp.get(i);
		return res;
	}
	
	int[] adj_applyRule(DetGraph g, int pos) {
		g.ajoutDep("MOD", pos, pos-1);
		int[] res = {pos};
		return res;
	}
}
