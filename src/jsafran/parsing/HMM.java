package jsafran.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.parsing.TokenPassing.StateNameTransformer;

public class HMM {
	
	// =================================================

	String modname;
	MaxEntOpenNLP maxent;
	int hmmId;
	public float finalProb=0f;

	public int debugNode=-1;

	// TEMP accumulators used during training
	ArrayList<String[]> allfeats = new ArrayList<String[]>();
	ArrayList<String> allstates = new ArrayList<String>();

	private float NANAprob=1f;

	public HMM(int hmmIdx) {
		hmmId=hmmIdx;
	}

	public void load(String prefix) {
		modname = prefix+"."+hmmId+"Model.txt";
		maxent = new MaxEntOpenNLP();
		maxent.loadModel(modname);
	}

	public static HMM load(String prefix, int stage) {
		HMM hmm = new HMM(stage);
		hmm.modname = prefix+"."+stage+"Model.txt";
		hmm.maxent = new MaxEntOpenNLP();
		hmm.maxent.loadModel(hmm.modname);
		return hmm;
	}

	public void accumulate(String[] feats, String act) {
		allfeats.add(feats);
		int k = act.indexOf("2w");
		if (k>=0) act = act.substring(0,k);
		allstates.add(act);
	}

	public void trainHMM(String prefix, int niters) {
		assert allfeats.size()==allstates.size();
		String featfile = prefix+"."+hmmId+".tab";
		String[][] feats = allfeats.toArray(new String[allfeats.size()][]);
		String[] labels = allstates.toArray(new String[allstates.size()]);
		System.out.println("training HMM id="+hmmId+" nfeats="+feats.length);
		modname = MaxEntOpenNLP.train(feats,labels,featfile,niters);
		feats=null; labels=null; allfeats.clear(); allstates.clear(); System.gc();
		maxent = new MaxEntOpenNLP();
		maxent.loadModel(modname);
		System.out.println("training done ! "+modname);
	}

	/**
	 * TODO: essayer HMM d'ordre 2 pour prendre en compte plus de contexte...
	 * 
	 * @param g
	 * @param seq
	 * @param stage
	 * @return
	 */
	public String[] decode(DetGraph g, NodesSeq seq, int stage) {
		if (seq.getNnodes()==1) {
			String[] x = {"NA"};
			return x;
		}

		StateNameTransformer transfo = new StateNameTransformer() {
			@Override
			public String transform(String s) {
				//				return s;
				return s.substring(0,2);
			}
		};

		boolean onlyNA=true;
		String bestact = null;
		int bestpath = -1;
		List<ArrayList<String>> paths = null;

		while (true) {
			// boucle au cas ou on n'aurait que des NA

			// init states and probs
			String[] feats = seq.calcFeats(seq.getNodeRoot(0), -1, stage, 0, -1, "NONE");
			HashMap<String, Float> act2prob;
			if (0==debugNode)
				act2prob = maxent.analyseGUI(feats);
			else
				act2prob = maxent.parse(feats);

			if (NodesSeq.forcedActions!=null) {
				HashMap<String, Float> tt = new HashMap<String, Float>();
				tt.put(NodesSeq.forcedActions[0], act2prob.get(NodesSeq.forcedActions[0]));
				act2prob=tt;
			}
			ArrayList<String> initStatesl = new ArrayList<String>();
			ArrayList<Float> initProbsl = new ArrayList<Float>();
			for (String act : act2prob.keySet()) {
				if (act.charAt(0)!='R') {
					if (seq.getNnodes()>=3||!act.startsWith("LB")) {
						// pas de LR non plus, car il faut alors un RB ensuite !
						// TODO: le MaxEnt ne connait par LR ni RR ni ... !!!
						if (!act.startsWith("LR")) {
							initStatesl.add(act);
							initProbsl.add(act2prob.get(act));
						}
					}
				}
			}
			String[] initStates = initStatesl.toArray(new String[initStatesl.size()]);
			float[] initProbs = new float[initStates.length];
			for (int i=0;i<initProbs.length;i++) initProbs[i]=initProbsl.get(i);

			if (false) {
				// backward compatibility: A SUPPRIMER pour un vrai Viterbi !!!!
				int imax=0;
				float pna=0;
				for (int i=1;i<initProbs.length;i++) {
					if (initStates[i].charAt(0)=='N') pna=initProbs[i];
					else if (initProbs[i]>initProbs[imax]) imax=i;
				}
				String[] is = {null,"NA"};
				float[] ip = {0,pna};
				is[0]=initStates[imax]; ip[0]=initProbs[imax];
				initStates=is; initProbs=ip;
			}

			TokenPassing viterbi = new TokenPassing(initStates, initProbs, transfo);

			// iterations suivantes
			for (int i=1;i<seq.getNnodes();i++) {
				viterbi.createExtension();
				// TODO: comme j'ai supprime les features liees aux transitions, on doit pouvoir accelerer voire supprimer cette boucle ?
				for (String oldact : viterbi.getLastStates(null)) {
					feats = seq.calcFeats(seq.getNodeRoot(i), -1, stage, i, -1, oldact);

					if (i==debugNode)
						act2prob = maxent.analyseGUI(feats);
					else
						act2prob = maxent.parse(feats);

					if (NodesSeq.forcedActions!=null) {
						HashMap<String, Float> tt = new HashMap<String, Float>();
						tt.put(NodesSeq.forcedActions[i], act2prob.get(NodesSeq.forcedActions[i]));
						act2prob=tt;
					}
					// TODO: put out of the loop to go faster

					/*
					 * Nouvelles transitions pour 2 etages:
					 * LB = LA2
					 * RB = RA2
					 * LR = LA1 avant RA2
					 * RR = RA1 avant RA2
					 * LL = LA1 apres LA2
					 * RL = RA1 apres LA2
					 */

					HashMap<String, Map<String, Float>> possibleTransitions = new HashMap<String, Map<String,Float>>();
					HashMap<String, Float> tr = new HashMap<String, Float>();
					if (oldact.startsWith("NA")) {
						for (String s : act2prob.keySet()) {
							if (s.startsWith("NA")) tr.put(s,NANAprob);
							else if (i+2<seq.getNnodes()&&s.startsWith("LB")) tr.put(s,1f);
							else if (i+1<seq.getNnodes()&&s.startsWith("LA")) {tr.put(s,1f);tr.put("LR"+s.substring(2),1f);}
							else if (i>0&&s.startsWith("RA")) {tr.put(s,1f);tr.put("RR"+s.substring(2),1f);}
							else if (i+1<seq.getNnodes()&&s.startsWith("LR")) tr.put(s,1f);
							else if (i>0&&i+1<seq.getNnodes()&&s.startsWith("RR")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("LA")) {
						for (String s : act2prob.keySet()) {
							if (s.startsWith("NA")) tr.put(s,NANAprob);
							else if (i+1<seq.getNnodes()&&s.startsWith("LA")) tr.put(s,1f);
							else if (i+2<seq.getNnodes()&&s.startsWith("LB")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("RA")) {
						for (String s : act2prob.keySet()) {
							if (s.startsWith("NA")) tr.put(s,NANAprob);
							else if (i+1<seq.getNnodes()&&s.startsWith("LA")) {tr.put(s,1f);tr.put("LR"+s.substring(2),1f);}
							else if (i+2<seq.getNnodes()&&s.startsWith("LB")) tr.put(s,1f);
							else if (i>0&&s.startsWith("RA")) {tr.put(s,1f);tr.put("RR"+s.substring(2),1f);}
							else if (i+1<seq.getNnodes()&&s.startsWith("LR")) tr.put(s,1f);
							else if (i>0&&i+1<seq.getNnodes()&&s.startsWith("RR")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("LB")) {
						for (String s : act2prob.keySet()) {
							if (i+1<seq.getNnodes()&&s.startsWith("LL")) tr.put(s,1f);
							else if (i>0&&s.startsWith("RL")) tr.put(s,1f);
							if (i+1<seq.getNnodes()&&s.startsWith("LA")) tr.put("LL"+s.substring(2),1f);
							else if (i>0&&s.startsWith("RA")) tr.put("RL"+s.substring(2),1f);
						}
					} else if (oldact.startsWith("RB")) {
						for (String s : act2prob.keySet()) {
							if (i+2<seq.getNnodes()&&s.startsWith("LB")) tr.put(s,1f);
							else if (s.startsWith("NA")) tr.put(s,NANAprob);
							else if (i+1<seq.getNnodes()&&s.startsWith("LA")) {tr.put(s,1f);tr.put("LR"+s.substring(2),1f);}
							else if (i>0&&s.startsWith("RA")) {tr.put(s,1f);tr.put("RR"+s.substring(2)	,1f);}
							else if (i+1<seq.getNnodes()&&s.startsWith("LR")) tr.put(s,1f);
							else if (i>0&&i+1<seq.getNnodes()&&s.startsWith("RR")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("LL")) {
						for (String s : act2prob.keySet()) {
							if (i+2<seq.getNnodes()&&s.startsWith("LB")) tr.put(s,1f);
							else if (s.startsWith("NA")) tr.put(s,NANAprob);
							else if (i+1<seq.getNnodes()&&s.startsWith("LA")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("RL")) {
						for (String s : act2prob.keySet()) {
							if (i+2<seq.getNnodes()&&s.startsWith("LB")) tr.put(s,1f);
							else if (s.startsWith("NA")) tr.put(s,NANAprob);
							else if (i+1<seq.getNnodes()&&s.startsWith("LA")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("LR")) {
						for (String s : act2prob.keySet()) {
							if (i>1&&s.startsWith("RB")) tr.put(s,1f);
						}
					} else if (oldact.startsWith("RR")) {
						for (String s : act2prob.keySet()) {
							if (i>1&&s.startsWith("RB")) tr.put(s,1f);
						}
					}
					//				possibleTransitions.put(oldact, tr);

					possibleTransitions.put(oldact.substring(0, 2), tr);

					HashMap<String, Float> newAct2prob = new HashMap<String, Float>();
					for (String a : act2prob.keySet()) {
						float p = act2prob.get(a);
						newAct2prob.put(a, p);
						if (a.startsWith("LA")) {
							newAct2prob.put("LR"+a.substring(2), p);
							newAct2prob.put("LL"+a.substring(2), p);
						} else if (a.startsWith("RA")) {
							newAct2prob.put("RR"+a.substring(2), p);
							newAct2prob.put("RL"+a.substring(2), p);
						}
					}
					act2prob=newAct2prob;

					viterbi.extend(possibleTransitions, act2prob);
				}
				viterbi.finalizeExtension();
			}

			// backtrack
			bestact = null;
			bestpath = -1;
			finalProb=0;
			paths = viterbi.getAllPaths();
			List<Float> probs = viterbi.getAllPathProbs();
			for (int i=0;i<paths.size();i++) {
				List<String> path = paths.get(i);
				String act = path.get(path.size()-1);
				// actes LA interdits a la fin !
				if (act.charAt(0)!='L') {
					if (bestact==null||probs.get(i)>finalProb) {
						bestact=act; bestpath=i;
						finalProb=probs.get(i);
					}
				}
			}

			onlyNA=true;
			if (bestpath>=0) {
				List<String> path = paths.get(bestpath);
				for (int j=0;j<path.size();j++) if (path.get(j).charAt(0)!='N') {onlyNA=false; break;}
			} else {
				NANAprob=1f;
				return null;
			}

			if (onlyNA) {
				if (NANAprob<0.1) {
					NANAprob=1f;
					return null;
				}
				System.out.println("reduces NA trans prob "+NANAprob);
				NANAprob/=2f;
			} else break;
		}
		NANAprob=1f;

		String[] acts = paths.get(bestpath).toArray(new String[paths.get(bestpath).size()]);
		if (onlyNA)
			System.out.println("backtrack*: "+finalProb+" "+Arrays.toString(acts));
		else
			System.out.println("backtrack: "+finalProb+" "+Arrays.toString(acts));

		//		System.out.println("seq: "+Arrays.toString(seq.word2node));

		// il faut ensuite decider pour chaque action liante, vers quel head !
		for (int curnode=0;curnode<acts.length;curnode++) {
			int headnode=-1;
			if (acts[curnode].startsWith("LA")) headnode=curnode+1;
			else if (acts[curnode].startsWith("LR")) headnode=curnode+1;
			else if (acts[curnode].startsWith("LL")) headnode=curnode+1;
			else if (acts[curnode].startsWith("LB")) headnode=curnode+2;
			else if (acts[curnode].startsWith("RA")) headnode=curnode-1;
			else if (acts[curnode].startsWith("RR")) headnode=curnode-1;
			else if (acts[curnode].startsWith("RL")) headnode=curnode-1;
			else if (acts[curnode].startsWith("RB")) headnode=curnode-2;
			if (headnode>=0) {
				String prevact="NONE";
				if (curnode>0) prevact = acts[curnode-1];



				int bestw = chooseBestHead(g, seq, stage, curnode, headnode, acts[curnode], prevact);
				acts[curnode]=acts[curnode]+"2w"+bestw;
			}
		}

		return acts;
	}

	private int chooseBestHead(DetGraph g,NodesSeq seq,int stage,int nodegov,int nodehead,String act,String prevact) {
		float bestwordp=-Float.MAX_VALUE;
		int bestw=-1;
		int w = seq.getNodeRoot(nodegov);
		ArrayList<Integer> candidates = new ArrayList<Integer>();
		for (int x : seq.getWordsInNode(nodehead)) {
			if (!seq.checkProjectivity(g, w, x)) continue;
			candidates.add(x);
		}
		if (candidates.size()<=1) {
			bestw=candidates.get(0);
		} else {
			if (true) {
				float pmax=-Float.MAX_VALUE;
				int xmax=-1;
				for (int x: candidates) {
					String dep = act.substring(2);
					int i=dep.indexOf("2w");
					if (i>=0) dep=dep.substring(0,i);
					float p = NodesSeq.selectionModel.getProb(g, w, x, dep, seq);
					if (p>pmax) {
						pmax=p; xmax=x;
					}
				}
				// il faut ensuite choisir parmi les "winners" !
				ArrayList<Integer> candidates2 = new ArrayList<Integer>();
				for (int x: candidates) {
					String dep = act.substring(2);
					int i=dep.indexOf("2w");
					if (i>=0) dep=dep.substring(0,i);
					float p = NodesSeq.selectionModel.getProb(g, w, x, dep, seq);
					if (p==pmax) candidates2.add(x);
				}

				if (candidates2.size()==1)
					bestw=xmax;
				else {
					for (int x: candidates2) {
						// on ne garde qu'un seul mot dans le sous-arbre head:
						String[] feats = seq.calcFeats(w, x, stage, nodegov, nodehead, prevact);
						HashMap<String, Float> act2prob = maxent.parse(feats);
						Float p = act2prob.get(act);
						if (p==null) System.out.println("ERRURRRR "+act+" "+act2prob);
						if (p>bestwordp) {
							bestwordp=p; bestw=x;
						}
					}
				}
			} else {
				// criteres a prendre en compte pour lier (X,Y,dep)
				// X a besoin de dep et n'en a pas d'autre
				// dist(X,Y)
				// (X,Y) aurait pu etre liees a un stage precedent
				for (int x: candidates) {
					// on ne garde qu'un seul mot dans le sous-arbre head:
					String[] feats = seq.calcFeats(w, x, stage, nodegov, nodehead, prevact);
					HashMap<String, Float> act2prob = maxent.parse(feats);
					float p = act2prob.get(act);
					if (p>bestwordp) {
						bestwordp=p; bestw=x;
					}
				}
			}
		}
		return bestw;
	}

	public static List<Integer> applyActions(DetGraph g, NodesSeq seq, String [] acts) {
		assert acts.length==seq.getNnodes();
		ArrayList<Integer> newdeps = new ArrayList<Integer>();
		for (int i=0;i<acts.length;i++) {
			if (acts[i].startsWith("L")) {
				int w = seq.getNodeRoot(i);
				newdeps.add(w);
				int k=acts[i].indexOf("2w");
				int h = Integer.parseInt(acts[i].substring(k+2));
				g.ajoutDep(acts[i].substring(2,k), w, h);
			} else if (acts[i].startsWith("R")) {
				int w = seq.getNodeRoot(i);
				newdeps.add(w);
				int k=acts[i].indexOf("2w");
				int h = Integer.parseInt(acts[i].substring(k+2));
				g.ajoutDep(acts[i].substring(2,k), w, h);
			}
		}
		return newdeps;
	}

	public static String[] calcOracle(NodesSeq seq) {
		String[] acts = new String[seq.getNnodes()];
		boolean nolink=true;
		for (int i=0;i<acts.length;i++) {
			acts[i] = seq.getLink(i);
			if (acts[i].charAt(0)!='N') {
				acts[i]=acts[i]+"2w"+seq.tmphead;
				nolink=false;
			}
		}
		if (nolink||acts.length==0) return null;

		if (false) {
			// transfo les LA et RA en contexte
			for (int i=0;i<acts.length;i++) {
				if (i>0&&acts[i-1].startsWith("LB")) {
					if (acts[i].startsWith("LA"))
						acts[i]="LL"+acts[i].substring(2);
					else if (acts[i].startsWith("RA"))
						acts[i]="RL"+acts[i].substring(2);
				}
				if (i<acts.length-1&&acts[i+1].startsWith("RB")) {
					if (acts[i].startsWith("LA"))
						acts[i]="LR"+acts[i].substring(2);
					else if (acts[i].startsWith("RA"))
						acts[i]="RR"+acts[i].substring(2);
				}
			}
		}

		return acts;
	}

	// ===============================================
	// unsupervised training, ONLY FOR STAGE 0 !!

	// TODO : fix

	public void trainUnsup(List<DetGraph> gs) {
		NodesSeq.selectionModel = new SelectionModel("maxentdatModel.txt");
		load("detmod",0);

		for (DetGraph g : gs) {
			g.clearDeps(); // to be sure we're unsupervised !
			NodesSeq seq = new NodesSeq(g);
			String[] acts = decode(g, seq, 0);
			List<Integer> newdeps = applyActions(g, seq, acts);
			for (int node=0;node<seq.getNnodes();node++) {
				String prevact="NONE";
				if (node>0) prevact = acts[node-1];
				String[] feats = seq.calcFeats(seq.getNodeRoot(node),-1,0,node,-1,prevact);
				//				if (actionProb[node]>0.8)
				//					accumulate(feats,acts[node]);
			}
		}
		System.out.println("all unlabeled corpus seen "+allfeats.size());

		System.out.println("now putting in labeled corpus");
		GraphIO gio = new GraphIO(null);
		gs = gio.loadAllGraphs("../jsafran/train2011.xml");
		int nUttWithSeveralTrees=0;
		ArrayList<HMM> hmmPerStage = new ArrayList<HMM>();
		hmmPerStage.add(this);
		for (DetGraph g : gs) {
			DetGraph gg = g.getSubGraph(0);
			gg.clearDeps();
			NodesSeq seq = new NodesSeq(g);
			NodesSeq gseq = new NodesSeq(gg);
			if (seq.getNnodes()==0) gseq=new NodesSeq();
			for (int stage=0, hmmidx=0;;stage++) { // pour tous les stages
				while (hmmPerStage.size()<=hmmidx) hmmPerStage.add(new HMM(hmmPerStage.size()));
				HMM hmm = hmmPerStage.get(hmmidx);

				String[] acts = HMM.calcOracle(seq);
				if (acts==null) {
					// plus d'action liante ! on doit donc avoir des multiple roots ?!
					nUttWithSeveralTrees++;
					break;
				}

				for (int node=0;node<seq.getNnodes();node++) {
					String prevact="NONE";
					if (node>0) prevact = acts[node-1];
					String[] feats = gseq.calcFeats(gseq.getNodeRoot(node),-1,stage,node,-1,prevact);
					hmm.accumulate(feats,acts[node]);
				}

				seq.mergeNodes(acts);
				HMM.applyActions(gg, gseq, acts);
				gseq.word2node=Arrays.copyOf(seq.word2node, seq.word2node.length);

				if (seq.getNnodes()<=1) break;

				if (hmmidx<NodesSeq.nHMMs-1) hmmidx++; // je cree un HMM pour les N premiers stages, puis un seul pour les suivants
			}
		}
		trainHMM("unsupmod",100);
		System.out.println("end of training:");
	}


	public static void main(String args[]) {
		/*
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("../jsafran/unsup.xml");
		HMM hmm = HMM.load("detmod", 0);
		hmm.trainUnsup(gs);
		 */
	}
}
