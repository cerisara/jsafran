package jsafran.parsing.unsup.hillclimb;

import jsafran.DetGraph;

public interface Scorer {
	public double getScore(DetGraph g);
	public void incCounts(DetGraph g);
	public void decCounts(DetGraph g);
}
