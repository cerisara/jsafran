package jsafran.parsing.unsup.criteria;

import jsafran.DetGraph;

public interface DetCriterion {
	public double getPenalty(DetGraph g);
}
