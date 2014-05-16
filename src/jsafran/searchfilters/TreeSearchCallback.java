package jsafran.searchfilters;

import java.util.HashMap;

import jsafran.DetGraph;

public interface TreeSearchCallback {
	/**
	 * 
	 * @param g
	 * @param vars
	 * @return OK or not OK to apply the (eventual) replacement of this matching instance
	 */
	public boolean callbackOneMatch(DetGraph g, HashMap<String, Object> vars);
}
