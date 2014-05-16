package jsafran.parsing.unsup.hillclimb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import utils.Wait;

import jsafran.Dep;
import jsafran.DetGraph;

public class HillClimbingSearch implements UttSearch {
	final int nruleseq = 10;
	RulesSeq seq0 = new RulesSeq();
	
	// keeps the best structure
	ArrayList<Dep> bestdeps = new ArrayList<Dep>();
	
	public void init(DetGraph g, Scorer sc) {
		// TODO: sample according to inverse dep length
		if (false) {
			// random init
			RulesSeq[] sx = seq0.sampleNseqs(g, 1);
			if (sx!=null&&sx.length>0) sx[0].apply(g);
		} else {
			RulesSeq sx = seq0.sampleRandomInit(g);
			if (sx!=null) sx.apply(g);
		}
		double scor = sc.getScore(g);
		g.conf=(float)scor;
	}
	
	@Override
	public float search(DetGraph g, Scorer sc) {
		// WARNING: this only works as long as the rules only create deps (no vars !)
		bestdeps.clear(); bestdeps.addAll(g.deps);
		float bestScoreSoFar = g.conf;
		RulesSeq[] seqs = seq0.sampleNseqs(g,nruleseq);
		if (seqs.length==0) {
			g.conf=bestScoreSoFar;
			g.deps.clear(); g.deps.addAll(bestdeps);
			return bestScoreSoFar;
		}
		int best=-1;
		for (int j=0;j<seqs.length;j++) {
			seqs[j].apply(g);
			double scor = sc.getScore(g);
			if (scor>bestScoreSoFar) {
				bestScoreSoFar=(float)scor; best=j;
				bestdeps.clear(); bestdeps.addAll(g.deps);
			}
		}
		g.conf=bestScoreSoFar;
		g.deps.clear(); g.deps.addAll(bestdeps);
		if (best<0) {
			// no best solutions; comes back to the previous solution, if any
			return bestScoreSoFar;
		}
		return bestScoreSoFar;
	}
}
