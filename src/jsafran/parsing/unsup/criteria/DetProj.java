package jsafran.parsing.unsup.criteria;

import jsafran.DetGraph;

/** 
 * 
 * penalise les arcs non-projectifs + les utt sans UTTROOT + les utt avec plusieurs UTTROOT
 * 
 * @author xtof
 *
 */
public class DetProj implements DetCriterion {

	@Override
	public double getPenalty(DetGraph g) {
		int ncrosscut=0, nroots=0;
		for (int i=0;i<g.getNbMots();i++) {
			int d=g.getDep(i);
			if (d>=0) {
				int h=g.getHead(d);
				if (h>i) {
					for (int j=i+1;j<h;j++) {
						d=g.getDep(j);
						if (d>=0) {
							int hh=g.getHead(d);
							if (hh>h||hh<i) ncrosscut++;
						} else ncrosscut++;
					}
				} else {
					for (int j=i-1;j>h;j--) {
						d=g.getDep(j);
						if (d>=0) {
							int hh=g.getHead(d);
							if (hh>i||hh<h) ncrosscut++;
						} else ncrosscut++;
					}
				}
			} else {
				// on penalise en meme temps les multiple roots !!
				nroots++;
			}
		}
		
		// I'm not sure the value of these constants is really important
		if (nroots==0) nroots = 5;
		else nroots--;
		double p = -(ncrosscut*10+nroots*100);
//		System.out.println("detproj penalty "+p);
		return p;
	}

}
