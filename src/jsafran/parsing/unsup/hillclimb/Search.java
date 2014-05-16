package jsafran.parsing.unsup.hillclimb;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import utils.Debug;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.parsing.ClassificationResult;
import jsafran.parsing.unsup.rules.RulesEditor;

public class Search {
	public static int niters=10;
	public static int nstarts=1000000;

	List<DetGraph> corpus;
	public Scorer scorer;
	UttSearch uttsearch = new HillClimbingSearch();
//	UttSearch uttsearch = new GreedySearch();

	List<DetGraph> golds=null;
	public static boolean stopitBeforeNextIter=false, stopitNow=false;

	public interface listenr {
		public void endOfIter();
	}
	public ArrayList<listenr> listeners = new ArrayList<Search.listenr>();

	public Search(List<DetGraph> goldxml,List<DetGraph> corpus) {
		this.corpus=corpus;
		this.golds=goldxml;
	}
	public Search(String goldxml,List<DetGraph> corpus) {
		GraphIO gio = new GraphIO(null);
		golds = gio.loadAllGraphs(goldxml);
		this.corpus=corpus;
	}

	public Search(List<DetGraph> corpus) {
		this.corpus=corpus;
	}

	public static int uttidx=0;

	public void search() {
		// compute gold score
		{
			for(DetGraph g: corpus){
				scorer.incCounts(g);
			}
			double sctot=0;
			for(DetGraph g: corpus){
				sctot+=scorer.getScore(g);
			}
			System.out.println("gold score "+sctot);
			for(DetGraph g: corpus){
				scorer.decCounts(g);
			}
		}

		// just to get the counts right
		for(DetGraph g: corpus){
			scorer.incCounts(g);
		}
		//initialization is done in the restart loop
		stopit:
		for (int start=0;start<nstarts;start++) {
			// restart
			for(DetGraph g: corpus) {
				scorer.decCounts(g);
				uttsearch.init(g,scorer);
				scorer.incCounts(g);
			}
			
			for (int iter=0;iter<niters;iter++) {
				double sctot=0;
				for (int i=0;i<corpus.size();i++) {
					if (stopitNow) break stopit;
					uttidx=i;
					DetGraph g = corpus.get(i);
					scorer.decCounts(g);
					uttsearch.search(g,scorer);
					sctot+=g.conf;
					scorer.incCounts(g);
				}
				if (golds!=null) {
					float[] las = ClassificationResult.calcErrors(corpus.subList(corpus.size()-golds.size(), corpus.size()), golds);
					System.out.println("start "+start+"/"+nstarts+" iter "+iter+"/"+niters+" score "+sctot+" LAS "+las[0]+" UAS "+las[1]);
				} else
					System.out.println("start "+start+" iter "+iter+" score "+sctot);
				for (listenr l : listeners) l.endOfIter();
				if (stopitBeforeNextIter) break stopit;
				if (RulesEditor.pauseIter) {
					int r = JOptionPane.showConfirmDialog(null, "Yes: continue w/ pause; No: continue no pause; Cancel: stop");
					if (r==JOptionPane.NO_OPTION) RulesEditor.pauseIter=false;
					else if (r==JOptionPane.CANCEL_OPTION) {
						stopitNow=true;
					}
				}
			}
		}
		System.out.println("seach fini ");
	}
}
