package jsafran.parsing.unsup.hillclimb;

import jsafran.DetGraph;

public interface OneRule {
	public int[] getApplicable(DetGraph g);
        //apply, applies the rule only in one and only only one pos???
	public void apply(DetGraph g, int hashCode); 
}
