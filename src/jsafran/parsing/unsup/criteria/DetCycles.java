package jsafran.parsing.unsup.criteria;

import java.util.HashSet;

import jsafran.Dep;
import jsafran.DetGraph;

/** 
 * 
 * penalise les cycles
 * 
 * @author xtof
 *
 */
public class DetCycles implements DetCriterion {

	@Override
	public double getPenalty(DetGraph g) {
		HashSet<Integer> dejavus = new HashSet<Integer>();
		int ncycles = 0;
		for (Dep de : g.deps) {
			dejavus.clear();
			int gov = de.gov.getIndexInUtt()-1;
			int head = de.head.getIndexInUtt()-1;
			int w = head;
			dejavus.add(gov);
			dejavus.add(head);
			for (;;) {
				int d = g.getDep(w);
				if (d<0) break;
				w=g.getHead(d);
				if (dejavus.contains(w)) {
					ncycles++;
					break;
				}
				dejavus.add(w);
			}
		}
		double p = -(ncycles*1000);
//		System.out.println("ncycle pen "+p);
		return p;
	}

}
