package jsafran.parsing.unsup.criteria;

import java.util.List;

import jsafran.DetGraph;

/**
 * penalise les prepositions sans fils
 * 
 * @author xtof
 *
 */
public class DetPrep implements DetCriterion {

	@Override
	public double getPenalty(DetGraph g) {
		int n=0;
		for (int i=0;i<g.getNbMots();i++) {
			if (g.getMot(i).getPOS().equals("PRP")) {
				List<Integer> fils = g.getFils(i);
				if (fils.size()==0) n++;
			}
		}
		return -(double)n*10.0;
	}

}
