package jsafran.parsing.unsup.hillclimb;

import java.util.HashMap;

import jsafran.DetGraph;

public class GreedySearch implements UttSearch {
	RulesSeq seq0 = new RulesSeq();
	public final static double MINSCORE = -Double.MAX_VALUE;

	private HashMap<DetGraph, RulesSeq> bestg2rules = new HashMap<DetGraph, RulesSeq>();
	private HashMap<DetGraph, Double>   bestg2confs = new HashMap<DetGraph, Double>();

	public void init(DetGraph g, Scorer sc) {
		// random init
		RulesSeq[] sx = seq0.sampleNseqs(g, 1);
		if (sx!=null&&sx.length>0) sx[0].apply(g);
		double scor = sc.getScore(g);
		g.conf=(float)scor;
	}
	
	@Override
	public float search(DetGraph g, Scorer sc) {
		RulesSeq seq = seq0.sampleGreedy(g,sc);
		if (seq==null) return g.conf;
		if (bestg2rules.get(g)!=null) g.conf=bestg2confs.get(g).floatValue();

		seq.apply(g);
		double scor = sc.getScore(g);
		if (scor>g.conf) {
			g.conf=(float)scor;
		} else {
			bestg2rules.get(g).apply(g);
			scor = sc.getScore(g);
			g.conf=(float)scor;
			return g.conf;
		}
		bestg2rules.put(g, seq);
		bestg2confs.put(g, new Double(g.conf));
		return g.conf;
	}
}
