package jsafran.parsing.unsup.hillclimb;

import jsafran.DetGraph;

public interface UttSearch {
	public float search(DetGraph g, Scorer sc);
	public void init(DetGraph g, Scorer sc);
}
