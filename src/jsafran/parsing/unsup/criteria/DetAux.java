package jsafran.parsing.unsup.criteria;

import java.util.List;

import jsafran.DetGraph;

/**
 * penalise les AUX sans fils
 * 
 * @author xtof
 *
 */
public class DetAux implements DetCriterion {

	@Override
	public double getPenalty(DetGraph g) {
		int n=0;
		for (int i=0;i<g.getNbMots();i++) {
			int d=g.getDep(i);
			if (d>=0&&g.getDepLabel(d).equals("AUX")) {
				List<Integer> fils = g.getFils(i);
				n+=fils.size();
			}
		}
		return -(double)n*10.0;
	}

}
