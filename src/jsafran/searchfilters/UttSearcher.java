package jsafran.searchfilters;

import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

import utils.JDiff;
import utils.JDiff.change;

/**
 * looks for an utterance in a given list of utterances
 * 
 * @author cerisara
 *
 */
public class UttSearcher {

	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(args[0]);
		List<DetGraph> gs2 = gio.loadAllGraphs(args[1]);
		ArrayList<DetGraph> remains = new ArrayList<DetGraph>();
		for (DetGraph g : gs) {
			int i=findClosestUtt(g, gs2);
			if (i>=0) {
				if (bestnerr>0) {
					g.comment += "CLOSEST_"+i+"_"+bestnerr;
					remains.add(g);
				} else if (bestnerr==0) {
					// recopie le target dans le set
					gs2.set(i,g);
				}
			}
		}
		gio.save(remains, "output.xml");
		gio.save(gs2, "newset.xml");
	}

	public static int findClosestUtt(DetGraph g, List<DetGraph> gs) {
		ArrayList<String> sgs = new ArrayList<String>();
		for (int i=0;i<gs.size();i++) {
			sgs.add(gs.get(i).sent);
		}
		return findClosestUtt(g.sent, sgs);
	}

	static int bestnerr;

	public static int findClosestUtt(String target, List<String> searchset) {
		String[] tars = target.split(" ");
		int besti=-1;
		bestnerr=Integer.MAX_VALUE;
		for (int i=0;i<searchset.size();i++) {
			String s = searchset.get(i);
			String[] ss = s.split(" ");
			JDiff jdiff = new JDiff(tars,ss);
			change c = jdiff.diff_2(false);
			int nerr = JDiff.areWrong(c).size();
			nerr+=JDiff.getNbDel(c);
			if (nerr<bestnerr) {
				bestnerr=nerr; besti=i;
			}
		}
		return besti;
	}
}
