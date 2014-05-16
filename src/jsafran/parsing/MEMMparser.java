package jsafran.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.maltparser.ml.lib.LibLinear;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import utils.ErrorsReporting;
import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.MEMM.ObsTransProbComputer;

/**
 * Plutot que d'avoir plusieurs stages, il vaut mieux n'en avoir qu'un, car les "rich features" sont difficiles à utiliser
 * Avantages d'un seul stage:
 * - pas de pb du choix du head
 * - features mieux définies, car sur un seul mot; mais limitées à des features type "graph-based" parser
 * Je ne m'intéresse pas à calculer toutes les deps, mais je complète pour terminer l'arbre. (avec MATE parser par exemple)
 * Je génère automatiquement les automates, itérativement.
 * 
 * Ceci est une alternative à l'algo de maximum spanning tree !
 * Avantage = il est probabiliste ??
 * Je peux utiliser les memes features que le MATE parser ?
 * 
 * 
 * 
 * @author xtof
 *
 */

public class MEMMparser implements ObsTransProbComputer {
	// le rang est la longueur max des dependances crees a un stage: G1, G2 ou G3
	public static int rank=1;

	DetGraph graph;
	static String modname;
	static MaxEntOpenNLP maxent=null;
	static MEMM hmm;

	// doit toujours etre monotone, et avec des nodes numerotes en +1
	int[] word2node;

	// TEMP accumulators used during training
	static ArrayList<String[]> allfeats = new ArrayList<String[]>();
	static ArrayList<String> allstates = new ArrayList<String>();

	// ===============================================================

	public int getNnodes() {
		if (word2node==null) return 0;
		return word2node[word2node.length-1]+1;
	}

	/**
	 * Autorise les multiple roots qui forment des graphes séparés
	 * Supprime les liens non-projectifs
	 * 
	 * @param g
	 * @return
	 */
	private static List<Integer> countNonProjectivity(DetGraph g, boolean corrige) {
		ArrayList<Integer> badwords = new ArrayList<Integer>();
		for (int i=0;i<g.getNbMots();i++) {
			int d=g.getDep(i);
			if (d>=0) {
				int h=g.getHead(d);
				int w1 = i, w2=h;
				if (w1>w2) {w1=h;w2=i;}
				for (int j=w1+1;j<w2;j++) {
					int d2=g.getDep(j);
					if (d2<0) {
						// il y a un ROOT sous une dépendance
						badwords.add(i);
						if (corrige) {
							// est-ce qu'on peut supporter ce type de non-projectivité ?
							g.removeDep(d);
						}
						break;
					}
					int h2=g.getHead(d2);
					if (h2<w1||h2>w2) {
						// il y a 2 arcs qui se croisent
						badwords.add(i);
						if (corrige) {
							// je supprime le premier arc (supprimer plutot l'arc le plus long ?)
							g.removeDep(d);
						}
						break;
					}
				}
			}
		}
		return badwords;
	}
	public MEMMparser() {
	}
	public MEMMparser(DetGraph gr) {
		graph=gr;
		// utilise seulement le train
//		countNonProjectivity(graph,true);
		word2node = new int[graph.getNbMots()];
		for (int i=0;i<word2node.length;i++) word2node[i]=i;
	}

	/**
	 * 
	 * @param node
	 * @return 2-dim tab, with first and last (exclusive) nodes
	 */
	public int[] getNodeFirstAndLastNodes(int node) {
		int w1;
		for (w1=0;;w1++) {
			if (word2node[w1]==node) break;
		}
		int w2=w1;
		for (;w2<word2node.length;w2++) {
			if (word2node[w2]>node) break;
		}
		int[] res = {w1,w2};
		return res;
	}
	public int getNodeRoot(int node) {
		int[] ws = getNodeFirstAndLastNodes(node);
		int w = ws[0];
		HashSet<Integer> dejavu = new HashSet<Integer>();
		dejavu.add(w);
		for (;;) {
			int d = graph.getDep(w);
			if (d<0) return w;
			int h = graph.getHead(d);
			if (h<ws[0]) return w;
			if (h>=ws[1]) return w;
			w=h;
			if (dejavu.contains(w)) {
				System.out.println("ERROR CYCLE ! "+w+" "+graph.getMot(w)+Arrays.toString(ws));
				JSafran.viewGraph(graph,w);
			}
			dejavu.add(w);
		}
	}

	enum acttype {NA,L1,R1,L2,R2,L3,R3,L4,R4,error};
	static acttype getActType(String act) {
		try {
			int i=act.indexOf('_');
			acttype r;
			if (i>=0) r = acttype.valueOf(act.substring(0,i));
			else r = acttype.valueOf(act);
			if (r!=null) return r;
		} catch (Exception e) {
		} finally {
			if (act.startsWith("NA")) return acttype.NA;
			else if (act.startsWith("LA1")) return acttype.L1;
			else if (act.startsWith("RA1")) return acttype.R1;
			else if (act.startsWith("LA2")) return acttype.L2;
			else if (act.startsWith("RA2")) return acttype.R2;
			else if (act.startsWith("LA3")) return acttype.L3;
			else if (act.startsWith("RA3")) return acttype.R3;
			else if (act.startsWith("LA4")) return acttype.L4;
			else if (act.startsWith("RA4")) return acttype.R4;
		}
		return acttype.error;
	}
	static String getActDep(String act) {
		int i=act.indexOf('_');
		if (i<0) return null;
		int j=act.indexOf("2w",i+1);
		if (j<0) j=act.length();
		return act.substring(i+1,j);
	}
	static int getActHeadword(String act) {
		int j=act.indexOf("2w");
		if (j<0) return -1;
		return Integer.parseInt(act.substring(j+2));
	}

	// j'essaye ici une version allegee de NodesSeq:
	// - un seul MEMM pour tous les stages
	// - extensible a longueur 2, 3, ...
	public static void train(List<DetGraph> golds, List<DetGraph> fromMalt, int niters) {
		assert fromMalt==null||golds.size()==fromMalt.size();
		allfeats.clear();
		allstates.clear();
		for (int gridx=0;gridx<golds.size();gridx++) {
			DetGraph g = golds.get(gridx);
			graphMalt=null;
			if (fromMalt!=null) graphMalt = fromMalt.get(gridx);
			DetGraph gg = g.getSubGraph(0);
			gg.clearDeps();
			MEMMparser goldSeq = new MEMMparser(g);
			MEMMparser inferedSeq = new MEMMparser(gg);

			// TODO: comme je supprime les stages suivants, on peut simplifier le MEMM car il n'y a plus besoin de la notion de nodes

			String[] acts = goldSeq.calcOracle();
			System.out.println("oracle acts: "+Arrays.toString(acts));
			if (acts==null) {
				// plus d'action liante ! on doit donc avoir des multiple roots ?!
				break;
			}

			for (int node=0;node<inferedSeq.getNnodes();node++) {
				String prevact="NONE";
				if (node>0) prevact = getActType(acts[node-1]).toString();
				String[] feats = inferedSeq.calcFeats(inferedSeq.getNodeRoot(node),-1,0,node,-1,prevact);
				acttype actt = getActType(acts[node]);
				String lab4maxent = actt.toString();
				if (actt!=acttype.NA) lab4maxent+="_"+getActDep(acts[node]);
				accumulate(feats,lab4maxent);
			}
		}
		finalizeTraining("memm",niters);
		maxent=null;
	}

	public static void accumulate(String[] feats, String act) {
		allfeats.add(feats);
		allstates.add(act);
	}

	public String[] calcOracle() {
		String[] acts = new String[getNnodes()];
		boolean nolink=true;
		for (int i=0;i<acts.length;i++) {
			acts[i] = getLink(i);
			if (acts[i].charAt(0)!='N') nolink=false;
		}
		if (nolink||acts.length==0) return null;
		return acts;
	}
	public String getLink(int node) {
		int nodehead = getNodeRoot(node);
		int d=graph.getDep(nodehead);
		if (d>=0) {
			int h=graph.getHead(d);
			int headnode = word2node[h];
			if (rank>=1&&headnode==node-1) {
				return "RA1_"+graph.getDepLabel(d)+"2w"+h;
			} else if (rank>=1&&headnode==node+1) {
				return "LA1_"+graph.getDepLabel(d)+"2w"+h;
			} else if (rank>=2&&headnode==node+2) {
				return "LA2_"+graph.getDepLabel(d)+"2w"+h;
			} else if (rank>=2&&headnode==node-2) {
				return "RA2_"+graph.getDepLabel(d)+"2w"+h;
			} else if (rank>=3&&headnode==node+3) {
				return "LA3_"+graph.getDepLabel(d)+"2w"+h;
			} else if (rank>=3&&headnode==node-3) {
				return "RA3_"+graph.getDepLabel(d)+"2w"+h;
			} else if (headnode==node) {
				System.out.println("WARNING: link to same node ?? cycle ??");
			}
		}
		return "NA";
	}

	public int[] getWordsInNode(int node) {
		int[] ws = getNodeFirstAndLastNodes(node);
		int[] res = new int[ws[1]-ws[0]];
		for (int i=0;i<res.length;i++) res[i]=ws[0]+i;
		return res;
	}

	public static String getBroadPOS(String pos) {
		int i=pos.indexOf(':');
		if (i>=0) return pos.substring(0,i);
		return pos;
	}

	public void mergeNodes(String[] acts) {
		if (word2node==null) return;
		assert acts.length==getNnodes();

		int[] actidx2word = new int[acts.length];
		for (int i=0;i<acts.length;i++) actidx2word[i]=getNodeRoot(i);

		// left-right
		for (int i=acts.length-1;i>=0;i--) {
			if (getActType(acts[i])==acttype.L2) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i+2]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i+1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			} else if (getActType(acts[i])==acttype.L3) {
				int targetnode = word2node[actidx2word[i+3]];
				int node2move = word2node[actidx2word[i]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i+2]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i+1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			} else if (getActType(acts[i])==acttype.L1) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i+1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			}
		}
		// right-left
		for (int i=0;i<acts.length;i++) {
			if (getActType(acts[i])==acttype.R2) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i-2]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i-1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			} else if (getActType(acts[i])==acttype.R3) {
				int targetnode = word2node[actidx2word[i-3]];
				int node2move = word2node[actidx2word[i]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i-2]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i-1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			} else if (getActType(acts[i])==acttype.R1) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i-1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			}
		}

		// translate tous les index pour qu'ils soient continus
		HashSet<Integer> vals = new HashSet<Integer>();
		for (int j=0;j<word2node.length;j++)
			vals.add(word2node[j]);
		Integer[] xvals = vals.toArray(new Integer[vals.size()]);
		Arrays.sort(xvals);

		int[] replace = new int[xvals[xvals.length-1]+1];
		Arrays.fill(replace, -1);
		for (int j=0;j<xvals.length;j++) replace[xvals[j]]=j;
		for (int j=0;j<word2node.length;j++)
			word2node[j]=replace[word2node[j]];
	}

	public List<Integer> applyActionsOld(String [] acts) {
		assert acts.length==getNnodes();
		ArrayList<Integer> newdeps = new ArrayList<Integer>();
		for (int i=0;i<acts.length;i++) {
			if (getActType(acts[i])!=acttype.NA) {
				int w = getNodeRoot(i);
				newdeps.add(w);
				int h=getActHeadword(acts[i]);
				graph.ajoutDep(getActDep(acts[i]), w, h);
			}
		}
		return newdeps;
	}
	public List<Integer> applyActions(String [] acts) {
		assert acts.length==getNnodes();
		ArrayList<Integer> newdeps = new ArrayList<Integer>();
		for (int i=0;i<acts.length;i++) {
			acttype at = getActType(acts[i]);
			HashMap<acttype, String> deps = node2dep.get(i);
			String s = deps.get(at);
			// dans l'ordre: deplab_head_prob
			int j=s.lastIndexOf('_');
			int k=s.lastIndexOf('_', j-1);
			int head = Integer.parseInt(s.substring(k+1,j));
			if (head>=0) {
				String deplab = s.substring(0,k);
				graph.ajoutDep(deplab, i, head);
			}
		}
		// TODO: detecter le best NA pour faire le root !
		return newdeps;
	}

	public static void finalizeTraining(String prefix, int niters) {
		assert allfeats.size()==allstates.size();
		String featfile = prefix+"train.tab";
		String[][] feats = allfeats.toArray(new String[allfeats.size()][]);
		String[] labels = allstates.toArray(new String[allstates.size()]);
		System.out.println("training HMM nfeats="+feats.length);
		modname = MaxEntOpenNLP.train(feats,labels,featfile,niters);
		feats=null; labels=null; allfeats.clear(); allstates.clear(); System.gc();
		maxent = new MaxEntOpenNLP();
		System.out.println("training done ! "+modname);
	}

	public static DetGraph graphMalt=null;
	static int curstage=0;
	static String hmmdef="hmm.def";
	public static void parseOld(List<DetGraph> goldGraphs, List<DetGraph> graphsFromMalt) {
		if (maxent==null) {
			maxent = new MaxEntOpenNLP();
			maxent.loadModel(modname);
		}
		hmm = MEMM.loadFromUrl(hmmdef);
		for (int gidx=0;gidx<goldGraphs.size();gidx++) {
			DetGraph g = goldGraphs.get(gidx);
			System.out.println("UTT "+g);
			if (graphsFromMalt!=null)
				graphMalt = graphsFromMalt.get(gidx);
			else graphMalt=null;
			g.clearDeps();
			if (g.getNbMots()<=1) continue;
			MEMMparser m = new MEMMparser(g);

			m.node2dep.clear();
			hmm.saveLattice(true);
			String[] acts = hmm.viterbi(m.getNnodes(),m,null);
			if (acts==null) break;
			System.out.println("acts0: "+Arrays.toString(acts));
			boolean onlyNA = m.rebuildActionsWithHeads(acts);
			System.out.println("acts: "+onlyNA+" "+Arrays.toString(acts));
			m.applyActions(acts);
		}
	}
	
	static acttype[] getForcedActs(DetGraph g) {
		acttype[] res = new acttype[g.getNbMots()];
		for (int i=0;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d<0) res[i]=acttype.NA;
			else {
				int h=g.getHead(d);
				if (Math.abs(h-i)<=rank) {
					switch(h-i) {
					case 1: res[i]=acttype.L1; break;
					case -1: res[i]=acttype.R1; break;
					case 2: res[i]=acttype.L2; break;
					case -2: res[i]=acttype.R2; break;
					case 3: res[i]=acttype.L3; break;
					case -3: res[i]=acttype.R3; break;
					}
				} else res[i]=acttype.NA;
			}
		}
		return res;
	}
	
	// parsing avec liblinear et un seul stage en forcant les actions
	public static void parseGold(List<DetGraph> goldGraphs, List<DetGraph> graphsFromMalt) {
		hmm = MEMM.loadFromUrl(hmmdef);
		try {
			liblinearmodel = Linear.loadModel(new File("liblinear.mod"));
			probs = new double[liblinearmodel.getNrClass()];
			loadvoc("feats.out.voc");
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int gidx=0;gidx<goldGraphs.size();gidx++) {
			DetGraph g = goldGraphs.get(gidx);
			System.out.println("UTT "+g);
			if (g.getNbMots()<=1) continue;
			MEMMparser m = new MEMMparser(g);
			m.forced = getForcedActs(g);
			System.out.println("forced "+Arrays.toString(m.forced));
			g.clearDeps();
			m.node2dep.clear();
			hmm.saveLattice(true);
			
			// le "forcealign" ne se fait pas dans Viterbi, mais dans la fonction de calcul des scores getObs...() !
			String[] acts = hmm.viterbi(m.getNnodes(),m,null);
			if (acts==null) break;
			System.out.println("acts0: "+Arrays.toString(acts));
			m.applyActions(acts);
		}
	}
	public static void parse(List<DetGraph> goldGraphs, List<DetGraph> graphsFromMalt) {
		hmm = MEMM.loadFromUrl(hmmdef);
		try {
			liblinearmodel = Linear.loadModel(new File("liblinear.mod"));
			probs = new double[liblinearmodel.getNrClass()];
			loadvoc("feats.out.voc");
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int gidx=0;gidx<goldGraphs.size();gidx++) {
			DetGraph g = goldGraphs.get(gidx);
			System.out.println("UTT "+g);
			g.clearDeps();
			if (g.getNbMots()<=1) continue;
			MEMMparser m = new MEMMparser(g);
			m.node2dep.clear();
			hmm.saveLattice(true);
			String[] acts = hmm.viterbi(m.getNnodes(),m,null);
			if (acts==null) break;
			System.out.println("acts0: "+Arrays.toString(acts));
			m.applyActions(acts);
		}
	}

	int getBestHead(int node, String act) {
		acttype acttyp = getActType(act);
		int hdnode = node;
		switch (acttyp) {
		case L1: hdnode++; break;
		case L2: hdnode+=2; break;
		case L3: hdnode+=3; break;
		case R1: hdnode--; break;
		case R2: hdnode-=2; break;
		case R3: hdnode-=3; break;
		}
		return hdnode;
	}

	boolean rebuildActionsWithHeads(String[] acts) {
		boolean onlyNA = true;
		for (int i=0;i<acts.length;i++) {
			acttype at = getActType(acts[i]);
			if (at!=acttype.NA) {
				onlyNA = false;
				int head = getBestHead(i,acts[i]);
				acts[i]=at.toString()+"_"+node2dep.get(i).get(at)+"2w"+head;
			}
		}
		return onlyNA;
	}

	// Je conserve ici, pour chaque noeud = obs, le meilleur deplabel choisit pour chaque etat du HMM
	HashMap<Integer,HashMap<acttype, String>> node2dep = new HashMap();

	static float naprior = 1f;
	static Model liblinearmodel = null;
	static double[] probs = null;
	static HashMap<String, Integer> voc2idx = new HashMap<String, Integer>();
	static HashMap<Integer, String> vocinv = new HashMap<Integer, String>();

	/*
	 * comme on doit normaliser les scores par node pour avoir une proba, il faut les pre-calculer pour tous les etats
	 */
	private HashMap<acttype, String> precompNode(int node, String prevState) {
		// state NA
		int besthead = -1;
		float probNA = -Float.MAX_VALUE;
		int bestdeplab = -1;
		// teste tous les head possibles pour NA, i.e. tous les heads qui sont en-dehors du HMM !
		for (int h=0;h<graph.getNbMots();h++) {
			if (h>=node-rank && h<=node+rank) continue;
			List<FeatureNode> fs = getFeats(graph, node, h, voc2idx);
			// TODO: adapt to new liblinear
			bestdeplab = 0; //Linear.predictProbability(liblinearmodel, fs.toArray(new FeatureNode[fs.size()]),probs);
//			int[] labs = liblinearmodel.getLabels();
			// le best suivant correspondre au bestdeplab; attention aux labs[] !!
			for (double p : probs)
				if (p>probNA) {
					probNA=(float)p; besthead=h;
				}
		}
		{
			// pour NA, test aussi si node est le ROOT !
			List<FeatureNode> fs = getFeats(graph, node, -1, voc2idx);
			Linear.predictProbability(liblinearmodel, fs.toArray(new FeatureNode[fs.size()]),probs);
			int[] labs = liblinearmodel.getLabels();
			int rootidx = voc2idx.get("LABROOT");
			for (int i=0;i<probs.length;i++) {
				if (labs[i]==rootidx) {
					double p = probs[i];
					if (p>probNA) {
						probNA=(float)p; besthead=-1; bestdeplab=-1;
					}
				}
			}
		}
		
		if (probNA==-Float.MAX_VALUE) {
			// dans une phrase tres courte, ou avec un rank tres grand, on ne "sort" pas du HMM contraint.
			// ca peut poser pb a Viterbi, car le dernier etat peut se voir interdire les Rx, a cause des
			// contraintes, et NA, donc aucun état n'est alors possible !
			// Il faut donc toujours autoriser NA = head de la phrase
			probNA = 0.001f;
		}

		HashMap<acttype, String> deps = new HashMap<MEMMparser.acttype, String>();
		node2dep.put(node, deps);
		ArrayList<Float> linkingprobs = new ArrayList<Float>();
		switch (rank) {
		case 4:
			// state L4
			if (node+4<graph.getNbMots())
				linkingprobs.add(precompLink(node, prevState, node+4, acttype.L4));
			// state R4
			if (node-4>=0) linkingprobs.add(precompLink(node, prevState, node-4, acttype.R4));
		case 3:
			// state L3
			if (node+3<graph.getNbMots())
				linkingprobs.add(precompLink(node, prevState, node+3, acttype.L3));
			// state R3
			if (node-3>=0) linkingprobs.add(precompLink(node, prevState, node-3, acttype.R3));
		case 2:
			// state L2
			if (node+2<graph.getNbMots()) linkingprobs.add(precompLink(node, prevState, node+2, acttype.L2));
			// state R2
			if (node-2>=0) linkingprobs.add(precompLink(node, prevState, node-2, acttype.R2));
		case 1:
			// state L1
			if (node+1<graph.getNbMots()) linkingprobs.add(precompLink(node, prevState, node+1, acttype.L1));
			// state R1
			if (node-1>=0) linkingprobs.add(precompLink(node, prevState, node-1, acttype.R1));
		}

		// normalize pour avoir une proba
		probNA*=naprior;
		float sum=probNA;
		for (float p : linkingprobs) sum+=p;
		probNA /= sum;
		String bestlab="ROOT";
		if (bestdeplab>=0)
			bestlab = vocinv.get(bestdeplab).substring(3); // car commence par LAB
		deps.put(acttype.NA, bestlab+"_"+besthead+"_"+probNA);
		switch (rank) {
		case 4:
			// state L4
			if (node+4<graph.getNbMots()) {
				String s = deps.get(acttype.L4);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.L4, s+"_"+(node+4)+"_"+p);
			}
			// state R4
			if (node-4>=0) {
				String s = deps.get(acttype.R4);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.R4, s+"_"+(node-4)+"_"+p);
			}
		case 3:
			// state L3
			if (node+3<graph.getNbMots()) {
				String s = deps.get(acttype.L3);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.L3, s+"_"+(node+3)+"_"+p);
			}
			// state R3
			if (node-3>=0) {
				String s = deps.get(acttype.R3);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.R3, s+"_"+(node-3)+"_"+p);
			}
		case 2:
			// state L2
			if (node+2<graph.getNbMots()) {
				String s = deps.get(acttype.L2);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.L2, s+"_"+(node+2)+"_"+p);
			}
			// state R2
			if (node-2>=0) {
				String s = deps.get(acttype.R2);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.R2, s+"_"+(node-2)+"_"+p);
			}
		case 1:
			// state L1
			if (node+1<graph.getNbMots()) {
				String s = deps.get(acttype.L1);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.L1, s+"_"+(node+1)+"_"+p);
			}
			// state R1
			if (node-1>=0) {
				String s = deps.get(acttype.R1);
				float p = linkingprobs.remove(0);
				p/=sum;
				deps.put(acttype.R1, s+"_"+(node-1)+"_"+p);
			}
		}
		assert linkingprobs.size()==0;
		return deps;
	}

	// calcule la meilleure DEP entre 2 mots
	private float precompLink(int node, String prevState, int head, acttype tgttype) {
		List<FeatureNode> fs = getFeats(graph, node, head, voc2idx);
		// TODO: adapt to new liblinear
		int res = 0; //Linear.predictProbability(liblinearmodel, fs.toArray(new FeatureNode[fs.size()]),probs);
		int[] labs = liblinearmodel.getLabels();
		float bestprob = -Float.MAX_VALUE;
		int bestdeplab = -1;
		for (int i=0;i<probs.length;i++) {
			double p = probs[i];
			
			// penalise les distances plus longues
			{
				float dist = Math.abs(head-node);
				if (dist>10) p*=0.1;
				else p*=1f-(dist-1f)/20f;
			}
			
			if (p>bestprob) {
				bestprob=(float)p; bestdeplab=i;
			}
		}
		bestdeplab=labs[bestdeplab];
		if (bestdeplab!=res) System.out.println("WARNING liblinear "+bestdeplab+" "+res);
		HashMap<acttype, String> deps = node2dep.get(node);
		// je garde le head meme pour les linking state, par coherence avec l'etat NA, meme si c'est inutile
		String bestlab = vocinv.get(bestdeplab).substring(3); // car commence par LAB
		deps.put(tgttype, bestlab);
		return bestprob;
	}

	acttype[] forced=null;
	
	@Override
	public double getObsTransProba(int node, String prevState, String curState) {
		HashMap<acttype, String> deps = node2dep.get(node);
		if (deps==null) {
			deps=precompNode(node,prevState);
		}
		acttype tgttype = getActType(curState);
		if (forced!=null&&forced[node]!=null&&forced[node]!=tgttype) return 0;
		String s=deps.get(tgttype);
		if (s==null) {
			// seulement avec NA, lorsque la phrase est trop courte
			return 0;
		}
		int j=s.lastIndexOf('_');
		float p = Float.parseFloat(s.substring(j+1));
//		System.out.println("give prob to viterbi: "+node+" "+curState+" "+p);
		return p;
	}
	public double getObsTransProbaOld(int node, String prevState, String curState) {
		// TODO: accelerer les calculs en n'appelant maxent qu'une seule fois par node avec tous les mots possibles

		int w = getNodeRoot(node);
		String[] feats = calcFeats(w, -1, curstage, node, -1, getActType(prevState).toString());
		HashMap<String,Float> act2prob = maxent.parse(feats);

		if (getActType(curState)==acttype.NA) {
			float probna = act2prob.get("NA");
			probna*=naprior;
			return probna;
		} else {
			// un lien est créé
			acttype tgttype = getActType(curState);
			float prob = 0;
			for (String act : act2prob.keySet()) {
				acttype maxenttype = getActType(act);
				if (maxenttype==acttype.NA) continue;
				if (maxenttype!=tgttype) continue;
				float p = act2prob.get(act);
				if (p>prob) {
					prob=p;
					HashMap<acttype, String> deps = node2dep.get(node);
					if (deps==null) {
						deps = new HashMap<MEMMparser.acttype, String>();
						node2dep.put(node, deps);
					}
					deps.put(tgttype, getActDep(act));
					// normalement, c'est Viterbi qui ne doit pas calculer ces etats, mais bon, c'est + simple de le faire ici...
					int hdnode = node;
					switch (tgttype) {
					case L1: hdnode++; break;
					case L2: hdnode+=2; break;
					case L3: hdnode+=3; break;
					case R1: hdnode--; break;
					case R2: hdnode-=2; break;
					case R3: hdnode-=3; break;
					}
					if (hdnode<0||hdnode>=getNnodes()) return 0;
				}
			}
			return prob;
		}
	}

	// ==============================================

	String[] calcFeatsNull(int w, int h, int stage0, int nodew, int y, String prevAct) {
		ArrayList<String> feats = new ArrayList<String>();

		if (graphMalt!=null) {
			int d = graphMalt.getDep(w);
			if (d<0) feats.add("MALTNA");
			else {
				feats.add("MALTDEP"+graphMalt.getDepLabel(d));
				int malthd = graphMalt.getHead(d);
				int dist = malthd-w;
				if (dist>4) dist=4;
				else if (dist<-4) dist=-4;
				feats.add("MALTDIST"+dist);
			}
		}

		feats.add("PREVACT"+prevAct);

		if (w<graph.getNbMots()-1) {
			int x=w+1;
			String pref = "JointL1";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOL1");
		if (w<graph.getNbMots()-2) {
			int x=w+2;
			String pref = "JointL2";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOL2");
		if (w<graph.getNbMots()-3) {
			int x=w+3;
			String pref = "JointL3";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOL3");
		if (w<graph.getNbMots()-4) {
			int x=w+4;
			String pref = "JointL4";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOL4");
		if (w>0) {
			int x=w-1;
			String pref = "JointR1";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOR1");
		if (w>1) {
			int x=w-2;
			String pref = "JointR2";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOR2");
		if (w>2) {
			int x=w-3;
			String pref = "JointR3";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOR3");
		if (w>3) {
			int x=w-4;
			String pref = "JointR4";
			feats.add(pref+"P"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
			feats.add(pref+"B"+getBroadPOS(graph.getMot(w).getPOS())+"-"+getBroadPOS(graph.getMot(x).getPOS()));
			feats.add(pref+"F"+graph.getMot(w).getForme()+"-"+graph.getMot(x).getForme());
			feats.add(pref+"L"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getLemme());
		} else feats.add("NOR4");

		return feats.toArray(new String[feats.size()]);
	}

	public boolean checkProjectivity(DetGraph g, int w, int h) {
		int nodehead = word2node[h];
		int[] ws = getWordsInNode(nodehead);
		Arrays.sort(ws);
		if (w<h) {
			for (int j=ws[0];j<h;j++) {
				int d=g.getDep(j);
				if (d<0) return false;
				int hh=g.getHead(d);
				if (hh<w||hh>h) return false;
			}
		} else {
			for (int j=h+1;j<=ws[ws.length-1];j++) {
				int d=g.getDep(j);
				if (d<0) return false;
				int hh=g.getHead(d);
				if (hh<h||hh>w) return false;
			}
		}
		return true;
	}
	String[] calcFeats(int w, int h, int stage0, int nodew, int nodeh, String prevAct) {
		return calcFeatsBest(w, h, stage0, nodew, nodeh, prevAct);
	}
	String[] calcFeatsBest(int w, int h, int stage0, int nodew, int nodeh, String prevAct) {
		ArrayList<String> feats = new ArrayList<String>();

		if (graphMalt!=null) {
			int d = graphMalt.getDep(w);
			if (d<0) feats.add("MALTNA");
			else {
				feats.add("MALTDEP"+graphMalt.getDepLabel(d));
				int malthd = graphMalt.getHead(d);
				if (word2node[malthd]==nodew) feats.add("MALTSELF");
				else if (word2node[malthd]==nodew-1) feats.add("MALTR1");
				else if (word2node[malthd]==nodew-2) feats.add("MALTR2");
				else if (word2node[malthd]==nodew-3) feats.add("MALTR3");
				else if (word2node[malthd]==nodew+1) feats.add("MALTL1");
				else if (word2node[malthd]==nodew+2) feats.add("MALTL2");
				else if (word2node[malthd]==nodew+3) feats.add("MALTL3");
				else feats.add("MALTFAR");
			}
		}

		feats.add("PREVACT"+prevAct);
		{
			final int maxn = 10;
			int nn = nodew-1;
			if (nn>maxn) nn=maxn;
			feats.add("NNODESL"+nn);
			nn = getNnodes()-nodew-1;
			if (nn>maxn) nn=maxn;
			feats.add("NNODESR"+nn);
		}

		// features du gouverné
		feats.add("FW"+graph.getMot(w).getForme());
		feats.add("LW"+graph.getMot(w).getLemme());
		feats.add("PW"+graph.getMot(w).getPOS());

		//		feats.add("FPW"+g.getMot(w).getLemme()+"-"+g.getMot(w).getPOS());
		// tous les mots du sous-arbre courant: peut etre utile pour choisir lorsque le root est une prep par exemple !
		for (int x : getWordsInNode(nodew)) {
			if (x!=w) {
				feats.add("CURF"+graph.getMot(x).getForme());
				feats.add("CURP"+graph.getMot(x).getPOS());
			}
		}

		// features du gouverneur gauche
		if (nodew>0) {
			for (int x : getWordsInNode(nodew-1)) {
				if (!checkProjectivity(graph, w, x)) continue;
				if (h<0||x==h||nodeh==nodew+1) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("LEFTW"+graph.getMot(x).getForme());
					feats.add("LEFTL"+graph.getMot(x).getLemme());
					feats.add("LEFTP"+graph.getMot(x).getPOS());
					feats.add("LEFTPD"+graph.getMot(x).getPOS()+"-"+dist);
					feats.add("JOINTLEFTP"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
					feats.add("JOINTLEFTW"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew-1);
			feats.add("LEFTROOTP"+graph.getMot(root).getPOS());
			//			feats.add("LEFTROOTF"+g.getMot(root).getForme());
			feats.add("LEFTROOTL"+graph.getMot(root).getLemme());
			feats.add("RMOSTP"+graph.getMot(graph.getRightmostNode(root)).getPOS());
		} else feats.add("NOLEFT");

		if (nodew>1) {
			for (int x : getWordsInNode(nodew-2)) {
				if (!checkProjectivity(graph, w, x)) continue;
				if (h<0||x==h||nodeh!=nodew-2) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("LLEFTW"+graph.getMot(x).getForme());
					feats.add("LLEFTL"+graph.getMot(x).getLemme());
					feats.add("LLEFTP"+graph.getMot(x).getPOS());
					feats.add("LLEFTPD"+graph.getMot(x).getPOS()+"-"+dist);
					feats.add("LJOINTLEFTP"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
					feats.add("LJOINTLEFTW"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew-2);
			feats.add("LLEFTROOTP"+graph.getMot(root).getPOS());
			//			feats.add("LEFTROOTF"+g.getMot(root).getForme());
			feats.add("LLEFTROOTL"+graph.getMot(root).getLemme());
			feats.add("LRMOSTP"+graph.getMot(graph.getRightmostNode(root)).getPOS());
		} else feats.add("NOLLEFT");

		// features du gouverneur droit
		if (nodew<getNnodes()-1) {
			for (int x : getWordsInNode(nodew+1)) {
				if (!checkProjectivity(graph, w, x)) continue;
				if (h<0||x==h||nodeh==nodew-1) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("RIGHTW"+graph.getMot(x).getForme());
					feats.add("RIGHTL"+graph.getMot(x).getLemme());
					feats.add("RIGHTP"+graph.getMot(x).getPOS());
					feats.add("RIGHTPD"+graph.getMot(x).getPOS()+"-"+dist);
					feats.add("JOINTRIGHTP"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
					feats.add("JOINTRIGHTW"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew+1);
			feats.add("RIGHTROOTP"+graph.getMot(root).getPOS());
			//			feats.add("RIGHTROOTF"+g.getMot(root).getForme());
			feats.add("RIGHTROOTL"+graph.getMot(root).getLemme());
			feats.add("LMOSTP"+graph.getMot(graph.getLeftmostNode(root)).getPOS());
		} else feats.add("NORIGHT");

		if (nodew<getNnodes()-2) {
			for (int x : getWordsInNode(nodew+2)) {
				if (!checkProjectivity(graph, w, x)) continue;
				if (h<0||x==h||nodeh!=nodew+2) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("RRIGHTW"+graph.getMot(x).getForme());
					feats.add("RRIGHTL"+graph.getMot(x).getLemme());
					feats.add("RRIGHTP"+graph.getMot(x).getPOS());
					feats.add("RRIGHTPD"+graph.getMot(x).getPOS()+"-"+dist);
					feats.add("RJOINTRIGHTP"+graph.getMot(w).getPOS()+"-"+graph.getMot(x).getPOS());
					feats.add("RJOINTRIGHTW"+graph.getMot(w).getLemme()+"-"+graph.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew+2);
			feats.add("RRIGHTROOTP"+graph.getMot(root).getPOS());
			//			feats.add("RIGHTROOTF"+g.getMot(root).getForme());
			feats.add("RRIGHTROOTL"+graph.getMot(root).getLemme());
			feats.add("RLMOSTP"+graph.getMot(graph.getLeftmostNode(root)).getPOS());
		} else feats.add("NORRIGHT");

		// features indep des noeuds
		int uttlen = graph.getNbMots()%5;
		if (uttlen>30) uttlen=30;
		feats.add("UTTLEN"+uttlen);

		int stage = stage0;
		if (stage>=6) stage=6;
		if (w>0) {
			feats.add("PFW"+graph.getMot(w-1).getForme()+"S"+stage);
			feats.add("PPW"+graph.getMot(w-1).getPOS()+"S"+stage);
		}
		if (w<graph.getNbMots()-1) {
			feats.add("NFW"+graph.getMot(w+1).getForme()+"S"+stage);
			feats.add("NPW"+graph.getMot(w+1).getPOS()+"S"+stage);
		}
		if (w>1) {
			feats.add("PPPW"+graph.getMot(w-2).getPOS()+"S"+stage);
		}
		if (w<graph.getNbMots()-2) {
			feats.add("NNPW"+graph.getMot(w+2).getPOS()+"S"+stage);
		}
		if (w>2) {
			feats.add("PPPPW"+graph.getMot(w-3).getPOS()+"S"+stage);
		}
		if (w<graph.getNbMots()-3) {
			feats.add("NNNPW"+graph.getMot(w+3).getPOS()+"S"+stage);
		}

		return feats.toArray(new String[feats.size()]);
	}

	// ==============================================
	static void calcErrors(List<DetGraph> grecs, String reffile) {
		ArrayList<int[]> marks = new ArrayList<int[]>();
		GraphIO gio = new GraphIO(null);
		List<DetGraph> grefs = gio.loadAllGraphs(reffile);
		int ndel=0,nins=0,nsub=0,nhead=0,nok=0,ntot=0;
		try {
			PrintWriter detailedRes = FileUtils.writeFileUTF("detailed.res");
			for (int i=0;i<grefs.size();i++) {
				DetGraph gref = grefs.get(i);
				// debug: ne score pas les longues phrases !
				//if (maxlen2score>=0&&gref.getNbMots()>=maxlen2score) continue;

				DetGraph grec = grecs.get(i);
				ArrayList<Integer> errwords = new ArrayList<Integer>(); 
				for (int recmot=0;recmot<grec.getNbMots();recmot++) {
					int drec = grec.getDep(recmot);
					int dref = gref.getDep(recmot);

					// debug: ne score pas les longues deps !
					//					if (maxlen2score>=0&&drec>=0&&Math.abs(recmot-grec.getHead(drec))>maxlen2score) continue;

					ntot++;
					if (drec<0&&dref>=0) {
						errwords.add(recmot);
						ndel++;
						detailedRes.println(0);
					} else if (drec>=0&&dref<0) {
						errwords.add(recmot);
						nins++;
						detailedRes.println(0);
					} else if (drec>=0&&dref>=0) {
						int href = gref.getHead(dref);
						int hrec = grec.getHead(drec);
						if (href!=hrec) {
							errwords.add(recmot);
							nhead++;
							detailedRes.println(0);
						} else {
							if (gref.getDepLabel(dref).equals(grec.getDepLabel(drec))) {
								nok++;
								detailedRes.println(1);
							} else {
								errwords.add(recmot);
								nsub++;
								detailedRes.println(0);
							}
						}
					} else if (drec<0&&dref<0) {
						nok++;
						detailedRes.println(1);
					}
				}
				for (int j=0;j<errwords.size();j++) {
					int[] mark = {0,i,errwords.get(j)};
					marks.add(mark);
				}
			}
			detailedRes.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("acc counts: ntot="+ntot+" nok="+nok+" nsub="+nsub+" nhead="+nhead+" ndel="+ndel+" nins="+nins);
		float las = (float)nok/(float)ntot;
		System.out.println("LAS="+las);
		try {
			PrintWriter fout = new PrintWriter(new FileWriter("accout.log", true));
			fout.println("acc counts: "+ntot+" "+nok+" "+nsub+" "+nhead+" "+ndel+" "+nins);
			fout.println("LAS="+las);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayList<List<DetGraph>> gsx = new ArrayList<List<DetGraph>>();
		gsx.add(grecs); gsx.add(grefs);
		JSafran.save(gsx, "errors.xml", null, marks);
	}

	static void saveVoc(String vocf) {
		try {
			System.out.println("saving voc...");
			PrintWriter fout = FileUtils.writeFileUTF("feats.out.voc");
			for (String v : voc2idx.keySet()) {
				fout.println(v+" "+voc2idx.get(v));
			}
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// a utiliser avec liblinear
	static void saveFeats(String f) {
		try {
			HashMap<String, Integer> voc2idx = new HashMap<String, Integer>();

			if ((new File("feats.out.voc")).exists()) {
				System.out.println("loading voc ");
				BufferedReader ff = FileUtils.openFileUTF("feats.out.voc");
				for (;;) {
					String s = ff.readLine();
					if (s==null) break;
					int i=s.lastIndexOf(' ');
					voc2idx.put(s.substring(0,i), Integer.parseInt(s.substring(i+1)));
				}
				ff.close();
			}


			PrintWriter fout = FileUtils.writeFileUTF("feats.out");
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(f);
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					int d=g.getDep(i);
					if (d>=0) {
						int h = g.getHead(d);
						{
							String lab = "LAB"+g.getDepLabel(d);
							Integer featidx = voc2idx.get(lab);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(lab, featidx);}
							fout.print(featidx+" ");
						}
						ArrayList<Integer> feats = new ArrayList<Integer>();
						{
							String feat = "GOVF"+g.getMot(i).getForme();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}
						{
							String feat = "HEADF"+g.getMot(h).getForme();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}
						{
							String feat = "GOVP"+g.getMot(i).getPOS();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}
						{
							String feat = "HEADP"+g.getMot(h).getPOS();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}

						// special numeric feature !!
						{
							// DISTANCE
							Integer featidx = 1;
							int dist = h-i;
							fout.print(featidx+":"+dist+" ");
						}
						Collections.sort(feats);
						for (int x:feats) fout.print(x+":1 ");
						fout.println();
					}
				}
			}
			fout.close();
			saveVoc("feats.out.voc");
		} catch (IOException e) {
			ErrorsReporting.report(e);
		}
	}
	static void saveFeatsTest(String f) {
		try {
			HashMap<String, Integer> voc2idx = new HashMap<String, Integer>();
			System.out.println("loading voc ");
			BufferedReader ff = FileUtils.openFileUTF("feats.out.voc");
			for (;;) {
				String s = ff.readLine();
				if (s==null) break;
				int i=s.lastIndexOf(' ');
				voc2idx.put(s.substring(0,i), Integer.parseInt(s.substring(i+1)));
			}
			ff.close();

			PrintWriter fout = FileUtils.writeFileUTF("feats.out");
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs(f);
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					int d=g.getDep(i);
					if (d>=0) {
						int h = g.getHead(d);
						{
							String lab = "LAB"+g.getDepLabel(d);
							Integer featidx = voc2idx.get(lab);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(lab, featidx);}
							fout.print(featidx+" ");
						}
						ArrayList<Integer> feats = new ArrayList<Integer>();
						{
							String feat = "GOVF"+g.getMot(i).getForme();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}
						{
							String feat = "HEADF"+g.getMot(h).getForme();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}
						{
							String feat = "GOVP"+g.getMot(i).getPOS();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}
						{
							String feat = "HEADP"+g.getMot(h).getPOS();
							Integer featidx = voc2idx.get(feat);
							if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
							feats.add(featidx);
						}

						// special numeric feature !!
						{
							// DISTANCE
							Integer featidx = 1;
							int dist = h-i;
							fout.print(featidx+":"+dist+" ");
						}
						Collections.sort(feats);
						for (int x:feats) fout.print(x+":1 ");
						fout.println();
					}
				}
			}
			fout.close();
			System.out.println("saving voc...");
			fout = FileUtils.writeFileUTF("feats.out.voc");
			for (String v : voc2idx.keySet()) {
				fout.println(v+" "+voc2idx.get(v));
			}
			fout.close();
		} catch (IOException e) {
			ErrorsReporting.report(e);
		}
	}

	static List<FeatureNode> getFeats(DetGraph g, int w, int h, HashMap<String, Integer> voc2idx) {
		HashSet<Integer> feats = new HashSet<Integer>();
		{
			String feat = "Leftward";
			if (w<h) feat="Rightward";
			if (h<0) feat = "Root";
			Integer featidx = voc2idx.get(feat);
			if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
			feats.add(featidx);
		}
		if (h>=0) {
			String feat = "Close";
			int d = Math.abs(w-h);
			if (d>=3) feat="Far1";
			if (d>=6) feat="Far2";
			Integer featidx = voc2idx.get(feat);
			if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
			feats.add(featidx);
		}
		{
			String feat = "GOVF"+g.getMot(w).getForme();
			Integer featidx = voc2idx.get(feat);
			if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
			feats.add(featidx);
		}
		{
			String feat = "GOVP"+g.getMot(w).getPOS();
			Integer featidx = voc2idx.get(feat);
			if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
			feats.add(featidx);
		}
		
		if (true) {
		if (w>0){
			{
				String feat = "GP1F"+g.getMot(w-1).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "GP1P"+g.getMot(w-1).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (w>1){
			{
				String feat = "GP2F"+g.getMot(w-2).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "GP2P"+g.getMot(w-2).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (w>2){
			{
				String feat = "GP3F"+g.getMot(w-3).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "GP3P"+g.getMot(w-3).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (w<g.getNbMots()-1){
			{
				String feat = "GN1F"+g.getMot(w+1).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "GN1P"+g.getMot(w+1).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (w<g.getNbMots()-2){
			{
				String feat = "GN2F"+g.getMot(w+2).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "GN2P"+g.getMot(w+2).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (w<g.getNbMots()-3){
			{
				String feat = "GN3F"+g.getMot(w+3).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "GN3P"+g.getMot(w+3).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		}
		
		if (true) {
		if (h>0){
			{
				String feat = "HP1F"+g.getMot(h-1).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "HP1P"+g.getMot(h-1).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (h>1){
			{
				String feat = "HP2F"+g.getMot(h-2).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "HP2P"+g.getMot(h-2).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (h>=0&&h<g.getNbMots()-1){
			{
				String feat = "HN1F"+g.getMot(h+1).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "HN1P"+g.getMot(h+1).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		if (h>=0&&h<g.getNbMots()-2){
			{
				String feat = "HN2F"+g.getMot(h+2).getForme();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
			{
				String feat = "HN2P"+g.getMot(h+2).getPOS();
				Integer featidx = voc2idx.get(feat);
				if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
				feats.add(featidx);
			}
		}
		}
		if (h>=0) {
			String feat = "HEADF"+g.getMot(h).getForme();
			Integer featidx = voc2idx.get(feat);
			if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
			feats.add(featidx);
		}
		if (h>=0) {
			String feat = "HEADP"+g.getMot(h).getPOS();
			Integer featidx = voc2idx.get(feat);
			if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(feat, featidx);}
			feats.add(featidx);
		}

		// j'avais un Set pour eviter les doublons
		ArrayList<Integer> feats2 = new ArrayList<Integer>();
		feats2.addAll(feats);
		
		// special numeric feature !!
		ArrayList<FeatureNode> ks = new ArrayList<FeatureNode>();
		if (false&&h>=0) {
			// DISTANCE
			int dist = h-w;
			FeatureNode n = new FeatureNode(1, dist);
			ks.add(n);
		}
		Collections.sort(feats2);
		for (int x:feats2) {
			FeatureNode n = new FeatureNode(x, 1);
			ks.add(n);
		}
		return ks;
	}

	static Problem getFeats(List<DetGraph> gs, HashMap<String, Integer> voc2idx) {
		ArrayList<Integer> ys = new ArrayList<Integer>();
		ArrayList<List<FeatureNode>> xs = new ArrayList<List<FeatureNode>>();
		int nfeats=0;
		for (DetGraph g : gs) {
			for (int i=0;i<g.getNbMots();i++) {
				int d=g.getDep(i);
				if (d>=0) {
					String lab = "LAB"+g.getDepLabel(d);
					Integer featidx = voc2idx.get(lab);
					if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(lab, featidx);}
					ys.add(featidx);

					List<FeatureNode> ks = getFeats(g, i, g.getHead(d), voc2idx);
					for (FeatureNode ksn : ks)
						if (ksn.index>=nfeats) nfeats=ksn.index;
					xs.add(ks);
				} else {
					// root !!
					String lab = "LABROOT";
					Integer featidx = voc2idx.get(lab);
					if (featidx==null) {featidx=voc2idx.size()+2; voc2idx.put(lab, featidx);}
					ys.add(featidx);

					List<FeatureNode> ks = getFeats(g, i, -1, voc2idx);
					for (FeatureNode ksn : ks)
						if (ksn.index>=nfeats) nfeats=ksn.index;
					xs.add(ks);
				}
			}
		}

		Problem prob = new Problem();
		prob.bias=0; prob.l=xs.size(); prob.n=nfeats;
		int[] y = new int[ys.size()];
		FeatureNode[][] x = new FeatureNode[xs.size()][];
		// TODO
		prob.x=x; // prob.y=y
		for (int i=0;i<y.length;i++) {
			y[i]=ys.get(i);
			x[i] = xs.get(i).toArray(new FeatureNode[xs.get(i).size()]);
		}
		return prob;
	}

	static void loadvoc(String vocf) {
		try {
			System.out.println("loading voc ");
			BufferedReader ff = FileUtils.openFileUTF(vocf);
			for (;;) {
				String s = ff.readLine();
				if (s==null) break;
				int i=s.lastIndexOf(' ');
				int widx = Integer.parseInt(s.substring(i+1));
				String word = s.substring(0,i);
				voc2idx.put(word, widx);
				vocinv.put(widx, word);
			}
			ff.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void liblinearTrain(String f) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(f);
		Problem prob = getFeats(gs, voc2idx);

		Parameter parms = new Parameter(SolverType.valueOf("L2R_LR"), 1, 0.01);

		Model model = Linear.train(prob, parms);
		try {
			model.save(new File("liblinear.mod"));
			saveVoc("feats.out.voc");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * cette methode realise un test basic du modele liblinear, en calculant toutes les paires de mots possibles
	 * sans aucune contrainte.
	 * 
	 * @param f
	 */
	static void liblinearTest(String f) {
		HashMap<String, Integer> voc2idx = new HashMap<String, Integer>();
		if ((new File("feats.out.voc")).exists()) {
			try {
				System.out.println("loading voc ");
				BufferedReader ff = FileUtils.openFileUTF("feats.out.voc");
				for (;;) {
					String s = ff.readLine();
					if (s==null) break;
					int i=s.lastIndexOf(' ');
					voc2idx.put(s.substring(0,i), Integer.parseInt(s.substring(i+1)));
				}
				ff.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(f);

		try {
			Model model = Linear.loadModel(new File("liblinear.mod"));
			double[] probs = new double[model.getNrClass()];

			int nok=0,ntot=0;
			for (DetGraph g : gs) {
				float minprobroot = Float.MAX_VALUE;
				int bestroot = -1;
				int[] foundheads = new int[g.getNbMots()];
				int[] foundlabs  = new int[g.getNbMots()];
				for (int w=0;w<g.getNbMots();w++) {
					int besthead = -1;
					float bestprob = -Float.MAX_VALUE;
					int bestdeplab = -1;
					// teste tous les head possibles
					for (int h=0;h<g.getNbMots();h++) {
						if (h==w) continue;
						List<FeatureNode> fs = getFeats(g, w, h, voc2idx);
						// TODO
						int res = 0; //Linear.predictProbability(model, fs.toArray(new FeatureNode[fs.size()]),probs);
						for (double p : probs)
							if (p>bestprob) {
								bestprob=(float)p; besthead=h; bestdeplab=res;
							}
					}
					foundheads[w]=besthead;
					foundlabs[w]=bestdeplab;
					if (bestprob<minprobroot) {
						minprobroot=bestprob;
						bestroot=w;
					}
				}
				// on choisit un root
				for (int w=0;w<foundheads.length;w++) {
					int goldhead=-1;
					int golddeplab = -1;
					{
						int d = g.getDep(w);
						if (d>=0) {
							goldhead = g.getHead(d);
							golddeplab = voc2idx.get("LAB"+g.getDepLabel(d));
						}
					}
					if (bestroot==w) {
						if (goldhead<0) nok++;
					} else {
						if (goldhead==foundheads[w] && golddeplab==foundlabs[w]) nok++;
					}
				}
				ntot+=g.getNbMots();
			}
			float acc = (float)nok/(float)ntot;
			System.out.println("acc = "+nok+" "+ntot+" "+acc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String specifyName(String state,String prefix) {
		int i = state.indexOf('w');
		if (i<0) System.out.println("ERROR "+state+" .. "+prefix);
		return state.substring(0,i)+prefix+state.substring(i);
	}
	
	static void generateST(int rank, HashMap<String, Set<String>> trans, HashSet<String> stin, HashSet<String> stout) {
		MEMM.generate(rank);
		MEMM.factorise("graphs.out");
		try {
			BufferedReader f = new BufferedReader(new FileReader("graph.dot"));
			String s = f.readLine();
			for (;;) {
				s = f.readLine();
				if (s==null) break;
				int i = s.indexOf("->");
				if (i<0) continue;
				if (s.startsWith("IN ")) {
					String s2 = s.substring(i+2);
					s2=s2.replace(';', ' ').trim();
					stin.add(s2);
				} else if (s.indexOf(" -> OUT ;")>=0) {
					String s2 = s.substring(0,i);
					s2=s2.trim();
					stout.add(s2);
				} else if (s.charAt(0)=='R'||s.charAt(0)=='L') {
					String s1 = s.substring(0,i).trim();
					String s2 = s.substring(i+2);
					s2=s2.replace(';', ' ').trim();
					Set<String> st = trans.get(s1);
					if (st==null) {
						st = new HashSet<String>();
						trans.put(s1, st);
					}
					st.add(s2);
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// generate auto le graphe Gi
	static void generateGi(int rank) {
		try {
			// contient toutes les transitions
			HashMap<String, Set<String>> trans = new HashMap<String, Set<String>>();
			// liste des input trans qui ne dépassent jamais à gauche
			HashMap<String, Set<String>> fsm2input = new HashMap<String, Set<String>>();
			// liste des input trans qui dépassent à gauche (viennent en complément de fsm2inputTransAlwaysValid)
			// ces 2 listes forment une partition des inputTrans: il n'y a pas de redoncance !
			HashMap<String, Set<String>> fsm2inputBck = new HashMap<String, Set<String>>();
			
			// les 2 listes suivantes forment aussi une partition
			// liste des output trans qui lient a droite
			HashMap<String, Set<String>> fsm2outputFwd = new HashMap<String, Set<String>>();
			// liste des output trans qui ne lient pas a droite
			HashMap<String, Set<String>> fsm2output = new HashMap<String, Set<String>>();
			
			// G0
			{
				String s = "NA";
				HashSet<String> st = new HashSet<String>();
				st.add(s);
				trans.put(s, st); // boucle sur NA
				st = new HashSet<String>();
				st.add(s);
				fsm2input.put("G0", st);
				st = new HashSet<String>();
				st.add(s);
				fsm2output.put("G0", st);
				st = new HashSet<String>();
				fsm2outputFwd.put("G0", st);
			}
			
			if (rank>=1)  {
				// G1
				HashSet<String> st = new HashSet<String>();
				// successeurs de LA1 = LA1, G0
				trans.put("LA1", st);
				st.add("LA1");
				for (String G0in : fsm2input.get("G0")) st.add(G0in);
				// successeurs de RA1 = RA1, LA1, G0
				st = new HashSet<String>();
				trans.put("RA1", st);
				st.add("RA1"); st.add("LA1");
				Set<String> sts = fsm2inputBck.get("G0");
				if (sts!=null) for (String G0in : sts) st.add(G0in);
				sts = fsm2input.get("G0");
				if (sts!=null) for (String G0in : sts) st.add(G0in);
				// successeurs de G0 = LA1 + RA1
				for (String G0out : fsm2output.get("G0")) {
					sts = trans.get(G0out);
					if (sts==null) {
						sts = new HashSet<String>();
						trans.put(G0out, sts);
					}
					sts.add("LA1");
					sts.add("RA1");
				}
				// inputs de G1 = LA1 + inputs de G0
				st = new HashSet<String>();
				st.add("LA1");
				sts = fsm2input.get("G0");
				
				if (sts!=null) for (String G0in : sts) st.add(G0in);
				fsm2input.put("G1", st);
				
				// inputs sans contraintes = RA1
				st = new HashSet<String>();
				st.add("RA1");
				sts = fsm2inputBck.get("G0");
				if (sts!=null) for (String G0in : sts) st.add(G0in);
				fsm2inputBck.put("G1", st);
				// outputs de G1 = outputs de G0 + RA1
				st = new HashSet<String>();
				fsm2output.put("G1",st);
				st.add("RA1");
				for (String G0out : fsm2output.get("G0")) st.add(G0out);
				st = new HashSet<String>();
				fsm2outputFwd.put("G1",st);
				st.add("LA1");
				for (String G0out : fsm2outputFwd.get("G0")) st.add(G0out);
			}
			for (int r = 2;r<=rank;r++) {
				HashSet<String> st = new HashSet<String>();
				fsm2outputFwd.put("G"+r,st);
				st = new HashSet<String>();
				fsm2output.put("G"+r,st);
				st = new HashSet<String>();
				fsm2input.put("G"+r,st);
				st = new HashSet<String>();
				fsm2inputBck.put("G"+r,st);
				// successeurs de LAr = LAr, ST(r-1)
				st = new HashSet<String>();
				trans.put("LA"+r, st);
				// st = liste des etats suivants LAr
				HashSet<String> stLin = new HashSet<String>();
				HashSet<String> stLout = new HashSet<String>();
				HashMap<String, Set<String>> transinST = new HashMap<String, Set<String>>();
				generateST(r,transinST,stLin,stLout);
				// transfert les etats de ST dans Gr
				for (String sinst : transinST.keySet()) {
					String sinG = specifyName(sinst,"STL"+(r-1));
					HashSet<String> stl = new HashSet<String>();
					trans.put(sinG, stl);
					for (String xinst : transinST.get(sinst)) {
						sinG = specifyName(xinst,"STL"+(r-1));
						stl.add(sinG);
					}
				}
				// ajoute les entrees de ST a la suite de LAr (dans st)
				for (String s : stLin) {
					String sinG = specifyName(s,"STL"+(r-1));
					st.add(sinG);
				}
				
				// successeurs de ST(r-1) = LAr + G(r-1)/b + out/b
				
				// d'abord, out/b:
				for (String s : stLout) {
					String sinG = specifyName(s,"STL"+(r-1));
					fsm2outputFwd.get("G"+r).add(sinG);
				}
				
				// puis je cree la liste qui recevra tous les etats successeurs de STL(r-1)
				st = new HashSet<String>();
				st.add("LA"+r);
				st.addAll(fsm2input.get("G"+(r-1)));
				// j'ajoute cette liste aux etats "sortants" de ST(r-1):
				Set<String> sts=null;
				for (String s : stLout) {
					String sinG = specifyName(s,"STL"+(r-1));
					sts = trans.get(sinG);
					if (sts==null) {
						sts = new HashSet<String>();
						trans.put(sinG, sts);
					}
					sts.addAll(st);
				}
				
				// successeurs de RA(r): G(r-1) + STR(r-1) + LAr + out
				// st contient la liste des etats suivants RAr
				st = new HashSet<String>();
				st.add("LA"+r);
				st.addAll(fsm2input.get("G"+(r-1)));
				st.addAll(fsm2inputBck.get("G"+(r-1)));
				fsm2output.get("G"+r).add("RA"+r);
				// ne reste plus que STR(r-1): il faut d'abord le creer
				HashSet<String> stRin = new HashSet<String>();
				HashSet<String> stRout = new HashSet<String>();
				transinST = new HashMap<String, Set<String>>();
				generateST(r,transinST,stRin,stRout);
				// transfert les etats de ST dans Gr
				for (String sinst : transinST.keySet()) {
					String sinG = specifyName(sinst,"STR"+(r-1));
					HashSet<String> stl = new HashSet<String>();
					trans.put(sinG, stl);
					for (String xinst : transinST.get(sinst)) {
						sinG = specifyName(xinst,"STR"+(r-1));
						stl.add(sinG);
					}
				}

				// ajoute les entrees de STR a la suite de RAr (dans st)
				for (String s : stRin) {
					String sinG = specifyName(s,"STR"+(r-1));
					st.add(sinG);
				}
				trans.put("RA"+r, st);

				// c'est fini pour RAr;
				// inversement, de STR vers RAr
				for (String s : stRout) {
					String sinG = specifyName(s,"STR"+(r-1));
					sts = trans.get(sinG);
					if (sts==null) {
						sts = new HashSet<String>();
						trans.put(sinG, sts);
					}
					sts.add("RA"+r);
				}

				// successeurs de G(r-1) out: LAr + STR(r-1) + out
				
				st = new HashSet<String>();
				st.add("LA"+r);
				for (String s : stRin) {
					String sinG = specifyName(s,"STR"+(r-1));
					st.add(sinG);
				}
				for (String s : fsm2output.get("G"+(r-1))) {
					Set<String> successorsOfG = trans.get(s);
					if (successorsOfG==null) {
						successorsOfG = new HashSet<String>();
						trans.put(s,successorsOfG);
					}
					successorsOfG.addAll(st);
					fsm2output.get("G"+r).add(s);
				}

				for (String s : fsm2outputFwd.get("G"+(r-1))) {
					Set<String> gout = trans.get(s);
					if (gout==null) {
						gout = new HashSet<String>();
						trans.put(s,gout);
					}
					gout.add("LA"+r);
					fsm2outputFwd.get("G"+r).add(s);
				}

				// reste les entrees de Gr:
				for (String s : stRin) {
					String sinG = specifyName(s,"STR"+(r-1));
					fsm2inputBck.get("G"+r).add(sinG);
				}
				
				fsm2inputBck.get("G"+r).addAll(fsm2inputBck.get("G"+(r-1)));
				fsm2input.get("G"+r).add("LA"+r);
				fsm2input.get("G"+r).addAll(fsm2input.get("G"+(r-1)));
			}
			
			

			// echo print graph
			System.out.println("save graph in tmp.def"+rank);
			PrintWriter fout = new PrintWriter(new FileWriter("tmp.def"+rank));
			fout.println("HMM dethmm");
			fout.println("rank "+rank);
			HashSet<String> allstates = new HashSet<String>();
			for (String s : trans.keySet()) {
				allstates.add(s);
				for (String x : trans.get(s)) allstates.add(x);
			}
			for (String s : allstates)
				fout.println("STATE "+s);
			
			for (String s : trans.keySet()) {
				for (String x : trans.get(s)) {
					fout.println("TRANS "+s+" "+x+" "+1);
				}
			}
			// les input et output sont ceux du G de plus grand ordre
			// un "block" constraint est imposé sur l'arc d'entrée
			// tous les arcs de sortie sont valables
			for (String s : fsm2input.get("G"+rank)) {
				fout.println("IN "+s);
			}
			for (String s : fsm2output.get("G"+rank)) {
				fout.println("OUT "+s);
			}
			for (String s : fsm2outputFwd.get("G"+rank)) {
				fout.println("OUT "+s);
			}
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) throws Exception {
		String testfile = null;
		String trainfile = "train2011.xml";
		String maltfile = null;

		int niters = 100;

		boolean dotrain = true;

		for (int i=0;i<args.length;i++) {
			if (args[i].equals("-train")) {
				trainfile = args[++i];
			} else if (args[i].equals("-savefeats")) {
				trainfile = args[++i];
				saveFeats(trainfile);
				System.exit(1);
			} else if (args[i].equals("-gen")) {
				int rank = Integer.parseInt(args[++i]);
				generateGi(rank);
				MEMM hmm = MEMM.loadFromUrl("tmp.def"+rank);
				hmm.saveDot("hmm"+rank+".dot");
				System.exit(1);
			} else if (args[i].equals("-libtrain")) {
				trainfile = args[++i];
				liblinearTrain(trainfile);
				System.exit(1);
			} else if (args[i].equals("-forcealign")) {
				trainfile = args[++i];
				GraphIO gio = new GraphIO(null);
				List<DetGraph> gs = gio.loadAllGraphs(trainfile);
				if (maltfile==null) {
					graphMalt=null;
					parseGold(gs,null);
				} else {
					List<DetGraph> maltgs = gio.loadAllGraphs(maltfile);
					parseGold(gs,maltgs);
				}
				calcErrors(gs, trainfile);
				gio.save(gs, "res.xml");
				System.exit(1);
			} else if (args[i].equals("-naprior")) {
				naprior=Float.parseFloat(args[++i]);
			} else if (args[i].equals("-libtest")) {
				testfile = args[++i];
				liblinearTest(testfile);
				System.exit(1);
			} else if (args[i].equals("-notrain")) {
				dotrain=false;
			} else if (args[i].equals("-test")) {
				testfile = args[++i];
			} else if (args[i].equals("-hmm")) {
				hmmdef = args[++i];
			} else if (args[i].equals("-malt")) {
				maltfile = args[++i];
			} else if (args[i].equals("-rank")) {
				rank = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-force")) {
				//				forceActions(args[++i]);
			} else if (args[i].equals("-debug")) {
				//				debugnode=Integer.parseInt(args[++i]);
				//				System.out.println("debugnode: "+debugnode);
			} else if (args[i].equals("-niters")) {
				niters = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-calcacc")) {
				GraphIO gio = new GraphIO(null);
				//				maxlen2score = Integer.parseInt(args[++i]);
				List<DetGraph> grec = gio.loadAllGraphs(args[++i]);
				calcErrors(grec, args[++i]);
				System.exit(1);
			}
		}
		GraphIO gio = new GraphIO(null);
		if (dotrain) {
			List<DetGraph> gs = gio.loadAllGraphs(trainfile);
			if (maltfile==null)
				train(gs,null,niters);
			else {
				List<DetGraph> maltgs = gio.loadAllGraphs(maltfile);
				train(gs,maltgs,niters);
			}
		}
		if (testfile!=null) {
			List<DetGraph> gs = gio.loadAllGraphs(testfile);
			modname="memmtrainModel.txt";
			if (maltfile==null) {
				graphMalt=null;
				parse(gs,null);
			} else {
				List<DetGraph> maltgs = gio.loadAllGraphs(maltfile);
				parse(gs,maltgs);
			}
			calcErrors(gs, testfile);
			gio.save(gs, "res.xml");
		}
	}

}
