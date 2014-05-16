package jsafran.parsing.unsup.hillclimb;

import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.parsing.ClassificationResult;

public class OracleScorer implements Scorer {

	List<DetGraph> golds;
	ArrayList<DetGraph> goldOne = new ArrayList<DetGraph>();
	ArrayList<DetGraph> recOne= new ArrayList<DetGraph>();
	
	public OracleScorer(String goldxml) {
		GraphIO gio =new GraphIO(null);
		golds = gio.loadAllGraphs(goldxml);
	}
	
	@Override
	public double getScore(DetGraph g) {
		DetGraph ref = golds.get(Search.uttidx);
		goldOne.clear(); recOne.clear();
		goldOne.add(ref);
		recOne.add(g);
		float[] las = ClassificationResult.calcErrors(recOne, goldOne);
		return las[0];
	}

	@Override
	public void incCounts(DetGraph g) {
	}

	@Override
	public void decCounts(DetGraph g) {
	}
}
