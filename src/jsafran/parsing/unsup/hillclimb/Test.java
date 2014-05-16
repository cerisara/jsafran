package jsafran.parsing.unsup.hillclimb;

import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.criteria.TopologyLattice;

public class Test {

	public static void main(String args[]) {
		Test t = new Test();
		t.test1();
	}

	void test1() {
		//RulesSeq.allrules.add(new R1());
		FrRules frrules=new FrRules();
		RulesSeq.allrules.addAll(frrules.getAllRules());
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("test2009.xml");
		for (DetGraph g : gs) {
			g.relatedGraphs= new ArrayList<DetGraph>();
			g.relatedGraphs.add(g.clone());

		}
		final JSafran gui = new JSafran();
		gui.allgraphs=gs;
		gui.initGUI();
		gui.repaint();
		Search search = new Search("test2009.xml",gs);
		Voc v = new Voc();
		v.init(gs);
		//		search.scorer = new DummyScorer();
		search.scorer = new JointModelScorer(v);
//		search.scorer = new OracleScorer("test2009.xml");
		search.listeners.add(new Search.listenr() {
			@Override
			public void endOfIter() {
				gui.repaint();
			}
		});
		search.search();
	}

	class DummyScorer implements Scorer {
		@Override
		public double getScore(DetGraph g) {
			int s = TopologyLattice.getTopoScore(g);
			return -s;
		}
		@Override
		public void incCounts(DetGraph g) {
		}
		@Override
		public void decCounts(DetGraph g) {
		}
	}

	class R1 implements OneRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> nouns = new ArrayList<Integer>();
			ArrayList<Integer> verbs = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++)
				if (g.getMot(i).getPOS().charAt(0)=='N') nouns.add(i);
				else if (g.getMot(i).getPOS().charAt(0)=='V') verbs.add(i);
			int[] res = new int[nouns.size()*verbs.size()];
			for (int i=0,k=0;i<nouns.size();i++)
				for (int j=0;j<verbs.size();j++)
					res[k++]=nouns.get(i)*1000+verbs.get(j);
			return res;
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
			int n = hashCode/1000;
			int v = hashCode%1000;
			int d=g.getDep(n);
			if (d>=0) g.removeDep(d);
			if (n==v)
				System.out.println("add link "+hashCode+" "+n+" "+v);
			g.ajoutDep("SUJ", n, v);
		}
	}
}
