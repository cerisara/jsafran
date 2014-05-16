package jsafran.parsing.unsup.hillclimb;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import utils.Debug;
import utils.Wait;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.unsup.criteria.TopologyLattice;
import jsafran.parsing.unsup.rules.PatternRules;
import jsafran.parsing.unsup.rules.RulesEditor.Combiner;
import jsafran.parsing.unsup.rules.RulesEditor.TopoScorer;

/**
 * TODO: in the following, I assume that all rules are independent one from the other
 * (because its simpler to program, debug, and faster as I don't recompute applicable rules every time)
 * but it is not required; it can work also with dependent rules.
 * 
 * no, it does not work, because competing rules should not be applied when one of them has already been, otherwise = multiple heads
 * so we have to recompute applicable rules in all cases
 * 
 * @author xtof
 *
 */
public class QSearch implements UttSearch {

	PatternRules rules = new PatternRules();

	// defines one state = sequence of rules
	int[] ruleseq;
	// temporary variable that stores the new action
	int[] action = {-1,-1};

	int[] bestRuleSeqSoFar;
	float bestScoreSoFar;

	final float alpha0 = 0.1f, gamma = 0.1f;
	Random rand = new Random();
	public static JSafran treesframe = null;

	float computeStateReward(DetGraph g, Scorer sc) {
		return (float)sc.getScore(g);
	}

	void recomputeGraph(DetGraph g) {
		g.clearDeps();
		for (int i=0;i<ruleseq.length;i++) {
			rules.apply(g, ruleseq[i]);
		}
	}

	void changeState(int[] action) {
		int r=ruleseq[action[0]];
		ruleseq[action[0]]=ruleseq[action[1]];
		ruleseq[action[1]]=r;
	}
	void undoAction(int[] action) {
		changeState(action);
	}

	// ------------------------------ features & state stuff

	float computeThetaPhi(float[] theta, float[] features) {
		float q=0;
		for (int i=0;i<features.length;i++) {
			q+=theta[i]*features[i];
		}
		return q;
	}

	void updateThetas(float[] theta, float[] features, float thetaPhiMax, float r) {
		float delta = r+gamma*thetaPhiMax-computeThetaPhi(theta,features);
		// to avoid divergence, look for a small enough alpha
		float alpha=alpha0;
		for (;;) {
			float maxupdate = 0;
			for (int i=0;i<features.length;i++) {
				float update = Math.abs(alpha*delta*features[i]);
				if (update>maxupdate) maxupdate=update;
			}
			if (maxupdate<0.1) break;
			alpha/=10f;
		}
		for (int i=0;i<features.length;i++) {
			theta[i]+=alpha*delta*features[i];
		}
	}

	// features = state = (nb crossing deps, avg dep len)
	private float[] features = {0,0};

	// TODO: il faut que les features soient fortement correlees avec le scoring !
	// avec le type de deps, on ne code pas la projectivite, donc il ne pourra pas apprendre...
	float[] computeFeatures(DetGraph g) {
		int ncr=0, nrts=0, totlen=0;
		for (int i=0;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d>=0) {
				// crossing other arc ?
				int h=g.getHead(d);
				int a=i,b=h;
				if (a>b) {a=h;b=i;}
				totlen+=b-a;
				for (int j=a+1;j<b;j++) {
					int dd = g.getDep(j);
					if (dd<0) {
						// ncr++;
					} else {
						int hh=g.getHead(dd);
						if (hh>b||hh<a) ncr++;
					}
				}
				// crossing above ROOT ?
				for (int j=a+1;j<b;j++) {
					d = g.getDep(i);
					if (d<0) ncr++;
				}
			} else nrts++;
		}
		features[0]=-ncr;
		if (g.deps.size()>0)
			features[1]=-(float)totlen/(float)g.deps.size();
		else
			features[1]=0;
		return features;
	}

	float[] initThetas(float[] features) {
		float[] theta = new float[features.length];
		// rather initialize at 0 ?
		final float w = 1f/(float)theta.length;
		Arrays.fill(theta, w);
		return theta;
	}
	// ----------------------------------------------------------

	int[] computePossibleActions(DetGraph g) {
		int[] idx = rules.getApplicable(g,true);
		return idx;
	}
	void changeState(DetGraph g, int ruleidx) {
		rules.apply(g, ruleidx);
	}
	int exploreBoltzmann(float[] q, int t, float bestq) {
		// TODO: reduire eps with t
		final float eps = 0.1f;
		// je ne fais pas Boltzmann pour le moment, mais plus simple:
		float r = rand.nextFloat();
		if (r>eps) {
			ArrayList<Integer> bestactions = new ArrayList<Integer>();
			for (int i=0;i<q.length;i++)
				if (q[i]==bestq) bestactions.add(i);
			return bestactions.get(rand.nextInt(bestactions.size()));
		}
		else return rand.nextInt(q.length);
	}

	/**
	 * must choose one rule sequence, apply it onto g, and put its score in g.conf
	 * and return this score.
	 * The score must be computed with the Scorer sc !!
	 * Computing the score could be done outside this method, but we may need it to build oracle searchs for instance
	 * 
	 * algo:
	 * 1- initialize by emptying the rule seq
	 *    compute the features, init thetas
	 * 2- for N iters:
	 * 	  - for all actions:
	 *      - compute the max for the Q-update
	 * 	  - update the thetas, and so the Q-function
	 *    - Boltzmann exploration: sample next action from distribution exp(Q(s,a)/Time)
	 *    
	 * With linear function approximation, Q = sum theta * Phi
	 * Action definition: we cannot simply switch two rules, because the rules may be applied or not depending on the previous rules
	 *    
	 * A state = current dependency tree on the sentence
	 * At each state, the set of possible actions = applicable actions without considering any existing competing dep (!!) (but still considering all previous deps that may be required by the rule to be applicable)
	 * So the action consists to simply apply one rule, AND remove competing old deps
	 * 
	 * This requires to clarify in the OneRule interface the difference between getApplicable(erase old concurrent deps or not)... done
	 */
	@Override
	public float search(DetGraph g, Scorer sc) {
		// features that describe a given state
		// parameters of the Q-fct approximation
		float[] theta;

		// init = empty graph

		// state definition = feature vector
		float[] features = computeFeatures(g);
		theta=initThetas(features);

		float bestScore = -Float.MAX_VALUE;
		ArrayList<Dep> besttree = new ArrayList<Dep>();
		
		// main Q iter
		for (int it=0;it<100;it++) {
			float r = computeStateReward(g,sc);
			if (r>bestScore) {
				bestScore=r;
				besttree.clear();
				besttree.addAll(g.deps);
			}
			int[] rulesApplicable = computePossibleActions(g);
			if (rulesApplicable.length==0) break;
			ArrayList<Dep> bestdeps = new ArrayList<Dep>();
			bestdeps.addAll(g.deps);
			float[] qvalues = new float[rulesApplicable.length	];
			float thetaPhiMax=-Float.MAX_VALUE;
			for (int aidx=0;aidx<rulesApplicable.length;aidx++) {
				changeState(g,rulesApplicable[aidx]);
				features = computeFeatures(g);
				qvalues[aidx]=computeThetaPhi(theta,features);

				//				Debug.print("stop "+qvalues[aidx]);
				//				JSafran.viewGraph(g);

				if (qvalues[aidx]>thetaPhiMax) {thetaPhiMax=qvalues[aidx];}
				g.deps.clear();
				g.deps.addAll(bestdeps);
			}
			updateThetas(theta,features,thetaPhiMax,r);
			int chosenAction = exploreBoltzmann(qvalues,it,thetaPhiMax);
			Debug.print("debug qs "+chosenAction+" "+r+" "+Arrays.toString(qvalues));
			// g.conf=qvalues[chosenAction];
			changeState(g,rulesApplicable[chosenAction]);
			features = computeFeatures(g);
		}
		
		if (g.conf<bestScore) {
			g.conf=bestScore;
			g.deps.clear();
			g.deps.addAll(besttree);
		}
		return g.conf;
	}

	int sample_Mult(double[] th) {
		double s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextDouble();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}
	int sample_Mult(float[] th) {
		float s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextFloat();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}

	void chooseFirstAction() {
		action[0]=0; action[1]=1;
	}
	boolean chooseNextAction() {
		int i=action[1]+1;
		if (i<ruleseq.length) {
			action[1]=i; return true;
		}
		i=action[0]+1;
		if (i<ruleseq.length-1) {
			action[0]=i;
			action[1]=i+1;
			return true;
		}
		return false;
	}

	@Override
	public void init(DetGraph g, Scorer sc) {
		// TODO Auto-generated method stub

	}

	public void parse(final List<DetGraph> gs, final List<DetGraph> golds) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Search search = new Search(golds,gs);
				search.uttsearch = new QSearch();
				search.scorer=new Combiner();
				search.listeners.add(new Search.listenr() {
					@Override
					public void endOfIter() {
						if (treesframe!=null) {
							treesframe.repaint();
						}
					}
				});
				Search.stopitBeforeNextIter=false;
				Search.stopitNow=false;
				search.search();
			}
		}, "rulesSampler");
		t.start();
	}

	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		// java.util.List<DetGraph> gs = gio.loadAllGraphs("xx.xml");
		// java.util.List<DetGraph> gs = gio.loadAllGraphs("test2009.xml");
				java.util.List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
		ArrayList<DetGraph> golds = new ArrayList<DetGraph>();
		for (DetGraph g : gs) {
			g.conf=-Float.MAX_VALUE;
			golds.add(g.clone());
			g.clearDeps();
		}
		golds=null;
		Search.niters=100;
		Search.nstarts=1;
		QSearch m = new QSearch();
		JSafran j = JSafran.viewGraph(gs,false);
		j.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				Search.stopitBeforeNextIter=true;
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		m.treesframe=j;
		m.parse(gs, golds);
	}
}
