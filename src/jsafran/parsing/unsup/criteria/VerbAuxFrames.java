package jsafran.parsing.unsup.criteria;

import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;
import jsafran.parsing.unsup.Criterion;

public class VerbAuxFrames extends Criterion {

	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		ArrayList<Integer> verbs = new ArrayList<Integer>();
		for (int i=0;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d<0) continue;
			if (g.getDepLabel(d).equals("AUX")) verbs.add(i);
		}
		long[] res = new long[verbs.size()];
		for (int i=0;i<res.length;i++) {
			res[i]=getIndex(g, verbs.get(i));
		}
		return res;
	}

	private int getIndex(DetGraph g, int v) {
		List<Integer> fils = g.getFils(v);
		int nsuj=0; // 0 - 1 - 2+
		int nobj=0; // 0 - 1 - 2+
		int npobj=0; // 0 - 1 - 2+
		for (int w : fils) {
			int d = g.getDep(w);
			String l = g.getDepLabel(d);
			if (l.equals("SUJ")) nsuj++;
			else if (l.equals("OBJ")) nobj++;
			else if (l.equals("POBJ")) npobj++;
		}
		if (nsuj>2) nsuj=2;
		if (nobj>2) nobj=2;
		if (npobj>2) npobj=2;
		return nsuj*9+nobj*3+npobj;
	}
	
	@Override
	public long getSpaceSize() {
		final int r = 3*3*3;
		return r;
	}

	@Override
	public int getMetaIndex(int t) {
		// TODO Auto-generated method stub
		return 0;
	}

}
