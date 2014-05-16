package jsafran.parsing.unsup.rl;

import java.util.Arrays;

import jsafran.DetGraph;

/**
 * detects whether a new arc is part of a subtree that is a good prototype
 * 
 * @author cerisara
 *
 */
public class Reuse {

	public int getReward(DetGraph g0, DetGraph g1) {
		boolean[] avant = isInGoodPrototype(g0);
		boolean[] apres = isInGoodPrototype(g1);
		int n=0;
		for (int i=0;i<avant.length;i++)
			if (avant[i]&&!apres[i]) n--;
			else if (!avant[i]&&apres[i]) n++;
		return (int)Math.signum(n);
	}
	
	public boolean[] isInGoodPrototype(DetGraph g) {
		// on tag tous les mots qui sont dans des prototypes
		// sauf le HEAD de la structure
		boolean[] res = new boolean[g.getNbMots()];
		Arrays.fill(res, false);
		checkGN0(g,res);
		return res;
	}

	void checkGN0(DetGraph g, boolean[] res) {
		for (int i=0;i<res.length;i++) {
			if (g.getMot(i).getPOS().startsWith("DET")) {
				int d=g.getDep(i);
				if (d>=0) {
					int h = g.getHead(d);
					if (h>i&&g.getMot(i).getPOS().startsWith("N")) {
						if (g.getDepLabel(d).equals("DET")) {
							res[i]=true; res[h]=true;
						}
					}
				}
			}
		}
	}
}
