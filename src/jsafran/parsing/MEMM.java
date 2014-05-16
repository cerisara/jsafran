package jsafran.parsing;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.maltparser.core.helper.HashSet;

import edu.stanford.nlp.util.Sets;

import jsafran.DetGraph;
import jsafran.Mot;

import utils.ErrorsReporting;
import utils.FileUtils;

public class MEMM {
	// NEW HMM model, that allows for several entry/exit transitions
	// and reads topology from file

	private String nom="HMM";
	private List<String> stateNames = new ArrayList<String>();
	// contient [state deb idx, state end idx, trans prob]
	private List<float[]> trans = new ArrayList<float[]>();
	private List<Integer> inputStates = new ArrayList<Integer>();
	private List<Integer> outputStates = new ArrayList<Integer>();
	private MEMMLattice lattice = null;

	public class MEMMLattice {
		double[][] obs2scores = null;
		int[][] bestPredecessors = null;

		public void startViterbi(int nobs) {
			obs2scores = new double[nobs][];
		}
		/**
		 * typically used in a backward A*
		 * 
		 * @param suffixPath : same length as the obs sequence, in 2 parts: [best path; fixed words]
		 * @param firstWordFixed : index of the start of the second part
		 * @return the probability of the (prefix = first part) only
		 */
		public double getBestPrefixPath(String[] suffixPath, int firstWordFixed) {
			double prob=1;
			int lastStateIdx;
			if (firstWordFixed>=obs2scores.length) {
				// same backtrack as usual Viterbi
				int bestlast = 0;
				int lastt = obs2scores.length-1;
				for (int i=1;i<obs2scores[lastt].length;i++) {
					if (obs2scores[lastt][i]>obs2scores[lastt][bestlast]) bestlast=i;
				}
				suffixPath[lastt]=getState(bestlast);
				firstWordFixed=obs2scores.length-1;
				prob = obs2scores[lastt][bestlast];
				lastStateIdx=bestlast;
			} else if (firstWordFixed<=0) {
				return 1;
			} else {
				lastStateIdx=getStateIdx(suffixPath[firstWordFixed]);
				int bestprec = bestPredecessors[firstWordFixed][lastStateIdx];
				prob = obs2scores[firstWordFixed-1][bestprec];
			}
			for (int t=firstWordFixed-1;t>=0;t--) {
				lastStateIdx = bestPredecessors[t+1][lastStateIdx];
				suffixPath[t]=getState(lastStateIdx);
			}
			return prob;
		}
		void setScoreAtObs(int t, double[] scores) {
			double[] s = Arrays.copyOf(scores, scores.length);
			obs2scores[t]=s;
		}
		void endViterbi(int[][] bestPrec) {
			bestPredecessors = bestPrec;
		}
	}

	public void saveLattice(boolean b) {
		if (b) lattice = new MEMMLattice();
		else lattice = null;
	}

	private int getStateIdx(String stateName) {
		for (int i=0;i<stateNames.size();i++) {
			if (stateNames.get(i).equals(stateName)) return i;
		}
		return -1;
	}
	private String getState(int sidx) {return stateNames.get(sidx);}

	public int getNstates() {return stateNames.size();}
	public void setNom(String n) {nom=n;}
	public int addState(String stateName) {
		int i=stateNames.size();
		stateNames.add(stateName);
		return i;
	}
	public void addTrans(String sdeb, String sfin, float trprob) {
		int s1 = getStateIdx(sdeb);
		int s2 = getStateIdx(sfin);
		float[] x = {s1,s2,trprob};
		trans.add(x);
	}

	public void addInputTrans(String stateName) {
		int i= getStateIdx(stateName);
		inputStates.add(i);
	}
	public void addOutputTrans(String stateName) {
		int i= getStateIdx(stateName);
		outputStates.add(i);
	}

	public void saveDot(String f) {
		try {
			PrintWriter fout = new PrintWriter(new FileWriter(f));
			fout.println("digraph XX {");
			for (int s : inputStates) {
				fout.println("IN -> "+getState(s)+" ;");
			}
			for (int s : outputStates) {
				fout.println(getState(s)+" -> OUT ;");
			}
			for (float[] tr : trans) {
				int s0 = (int)tr[0];
				int s1 = (int)tr[1];
				fout.println(getState(s0)+" -> "+getState(s1)+" ;");
			}
			fout.println("}");
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static MEMM loadFromUrl(String in) {
		MEMM hmm = new MEMM();
		try {
			InputStream io = FileUtils.findFileOrUrl(in);
			BufferedReader f = new BufferedReader(new InputStreamReader(io));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				s=s.trim();
				String[] x = s.split(" ");
				if (x[0].equals("HMM")) hmm.setNom(x[1]);
				else if (x[0].equals("STATE")) hmm.addState(x[1]);
				else if (x[0].equals("TRANS")) hmm.addTrans(x[1],x[2],Float.parseFloat(x[3]));
				else if (x[0].equals("IN")) hmm.addInputTrans(x[1]);
				else if (x[0].equals("OUT")) hmm.addOutputTrans(x[1]);
			}
			f.close();
			System.out.println("loaded "+hmm.getNstates());
		} catch (Exception e) {
			ErrorsReporting.report(e);
		}
		return hmm;
	}

	public String[] viterbi(int nObs, ObsTransProbComputer g, String[] forced) {
		if (lattice!=null) lattice.startViterbi(nObs);
		int[][] bestPrev = new int[nObs][getNstates()];
		double[][] scores = new double[2][getNstates()];
		int curt=0;
		Arrays.fill(scores[curt], -Double.MAX_VALUE);
		Arrays.fill(bestPrev[0], -1);
		for (int s1 : inputStates) {
			String curstate = getState(s1);
			if (forced!=null&&forced[0]!=null&&!curstate.startsWith(forced[0])) continue;
			double obsProb = g.getObsTransProba(0, "NONE", curstate);
			scores[0][s1] = Math.log(obsProb);
		}
		if (lattice!=null) lattice.setScoreAtObs(0, scores[0]);
		//			System.out.println("viterbi "+0+" "+Arrays.toString(scores[curt]));

		for (int t=1;t<nObs-1;t++) {
			int prevt=curt;
			curt=1-curt;
			Arrays.fill(scores[curt], -Double.MAX_VALUE);
			Arrays.fill(bestPrev[t], -1);
			for (int s1=0;s1<scores[prevt].length;s1++) {
				if (scores[prevt][s1]!=-Double.MAX_VALUE) {
					for (float[] tr : trans) {
						if (tr[0]==s1) {
							int s2 = (int)tr[1];
							if (forced!=null&&forced[t]!=null&&!getState(s2).startsWith(forced[t])) continue;
							double obsProb = g.getObsTransProba(t, getState(s1), getState(s2));
							obsProb *= tr[2];
							if (obsProb<=0||scores[prevt][s1]==-Double.MAX_VALUE) obsProb=-Double.MAX_VALUE;
							else obsProb = Math.log(obsProb)+scores[prevt][s1];
							if (obsProb>scores[curt][s2]) {
								scores[curt][s2]=obsProb;
								bestPrev[t][s2]=s1;
							}
						}
					}
				}
			}
			if (lattice!=null) lattice.setScoreAtObs(t, scores[curt]);
			//			System.out.println("viterbi "+t+" "+Arrays.toString(scores[curt]));
		}
		// TODO: ne pas calculer les scores des etats qui ne peuvent pas terminer parce qu'ils sont trop proches de la fin du signal ! Idem au debut...
		{
			int t=nObs-1;
			int prevt=curt;
			curt=1-curt;
			Arrays.fill(scores[curt], -Double.MAX_VALUE);
			Arrays.fill(bestPrev[t], -22);
			boolean noStatesFound=true;
			for (int s1=0;s1<scores[prevt].length;s1++) {
				if (scores[prevt][s1]!=-Double.MAX_VALUE) {
					for (float[] tr : trans) {
						if (tr[0]==s1) {
							int s2 = (int)tr[1];
							if (outputStates.contains(s2)) {
								if (forced!=null&&forced[t]!=null&&!getState(s2).startsWith(forced[t])) continue;
								double obsProb = g.getObsTransProba(t, getState(s1), getState(s2));
								obsProb *= tr[2];
								if (obsProb<=0||scores[prevt][s1]==-Double.MAX_VALUE) obsProb=-Double.MAX_VALUE;
								else obsProb = Math.log(obsProb)+scores[prevt][s1];
								if (obsProb>=scores[curt][s2]) {
									scores[curt][s2]=obsProb;
									bestPrev[t][s2]=s1;
									noStatesFound=false;
								}
							}
						}
					}
				}
			}
			if (noStatesFound) {
				System.out.println("ERROR backtrack");
				// TODO
			}
		}


		// backtrack
		int bestlast = 0;
		for (int i=1;i<scores[curt].length;i++) {
			if (scores[curt][i]>scores[curt][bestlast]) bestlast=i;
		}
		System.out.println("viterbi score "+scores[curt][bestlast]);
		String[] path = new String[nObs];
		for (int t=nObs-1;t>=0;t--) {
			path[t]=getState(bestlast);
			bestlast=bestPrev[t][bestlast];
		}

		if (lattice!=null) lattice.endViterbi(bestPrev);
		return path;
	}

	public interface ObsTransProbComputer {
		public double getObsTransProba(int obs, String prevState, String curState);
	}

	// =============================================================

	// genere tous les chemins pour le sous-graphe ST(i-1) du rang i
	static void generate(int rank) {
		try {
			PrintWriter fout = FileUtils.writeFileUTF("graphs.out");
			int nNodes = rank-1;
			DetGraph g = new DetGraph();
			g.addMot(0, new Mot("OUT","_","_"));
			for (int i=0;i<nNodes;i++)
				g.addMot(i+1, new Mot("N"+i,"_","_"));
			g.addMot(nNodes+1, new Mot("OUT","_","_"));
			generateAtRoot(fout, rank,g,0,1);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	static void generateAtRoot(PrintWriter fout, int rank, DetGraph g, int root, int curmot) {
		if (curmot>=g.getNbMots()-1) {
			int[] roots = g.getRoots();
			for (int r: roots) {
				if (r!=0&&r!=g.getNbMots()-1) return;
			}
			String s = "";
			for (int i=0;i<g.getNbMots();i++) {
				int d=g.getDep(i);
				if (d>=0) {
					s+=g.getDepLabel(d)+"w"+i+" ";
				}
			}
			System.out.println(s);
			fout.println(s);
			return;
		}
		// vers la gauche
		for (int i=1;i<rank;i++) {
			int h = curmot-i;
			if (h>=0&&h<g.getNbMots()) {
				if (checkProjectivity(g, curmot, h)) {
					DetGraph gg = g.getSubGraph(0);
					gg.ajoutDep("RA"+i, curmot, h);
					generateAtRoot(fout, rank, gg, root, curmot+1);
				}
			}
		}
		// vers la droite
		for (int i=1;i<rank;i++) {
			int h = curmot+i;
			if (h>=0&&h<g.getNbMots()) {
				if (checkProjectivity(g, curmot, h)) {
					DetGraph gg = g.getSubGraph(0);
					gg.ajoutDep("LA"+i, curmot, h);
					generateAtRoot(fout, rank, gg, root, curmot+1);
				}
			}
		}
	}
	public static boolean checkProjectivity(DetGraph g, int w, int h) {
		// tous les mots ont des deps jusque w
		for (int x=0;x<w;x++) {
			int d=g.getDep(x);
			int hh=g.getHead(d);
			// si head est entre w et h, et que x est en-dehors => non-proj
			if (hh>w&&hh<h && x<h) return false;
			// si x est entre w et h, et que head est en-dehors => non-proj
			if (x>h && (hh>w||hh<h)) return false;
		}
		// check also cycles:
		for (int x=h;;) {
			if (x==w) return false;
			int d=g.getDep(x);
			if (d<0) break;
			x=g.getHead(d);
		}
		return true;
	}
	public static class State {
		String nom;
		int id;
		Set<State> nexts = new HashSet<State>();
		Set<State> prevs = new HashSet<State>();
		public State(String n, int id) {nom=n;this.id=id;}
	}
	public static void factorise(String sequenceFile) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(FileUtils.findFileOrUrl(sequenceFile)));
			ArrayList<State> states = new ArrayList<State>();
			ArrayList<State> lastStates = new ArrayList<State>();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim();
				String[] ss = s.split(" ");
				State next=new State(ss[ss.length-1],states.size());
				lastStates.add(next);
				states.add(next);
				for (int i=ss.length-2;i>=0;i--) {
					State st=new State(ss[i],states.size());
					st.nexts.add(next);
					next.prevs.add(st);
					states.add(st);
					next=st;
				}
			}
			f.close();

			// factorise en partant de la fin
			// facile pour les derniers noeuds
			ArrayList<State> toremove = new ArrayList<MEMM.State>();
			for (int i=0;i<lastStates.size();i++) {
				for (int j=0;j<i;j++) {
					if (lastStates.get(i).nom.equals(lastStates.get(j).nom)) {
						// on peut les merger
						lastStates.get(j).prevs.addAll(lastStates.get(i).prevs);
						lastStates.get(j).nexts.addAll(lastStates.get(i).nexts);
						for (State st : lastStates.get(i).prevs) {
							st.nexts.remove(lastStates.get(i));
							st.nexts.add(lastStates.get(j));
						}
						states.remove(lastStates.get(i));
						toremove.add(lastStates.get(i));
						break;
					}
				}
			}
			lastStates.removeAll(toremove);
			/*
			toremove.clear();
			for (State st : lastStates) {
				toremove.addAll(st.prevs);
			}
			lastStates=toremove;
				*/
			// pour les noeuds precedents, il faut faire recursivement.
			factoriserecBcw(states,lastStates);

			// puis factorise du debut vers la fin:
			ArrayList<State> firstStates = new ArrayList<MEMM.State>();
			for (State st : states) {
				if (st.prevs.size()==0) firstStates.add(st);
			}
			toremove.clear();
			for (int i=0;i<firstStates.size();i++) {
				for (int j=0;j<i;j++) {
					if (firstStates.get(i).nom.equals(firstStates.get(j).nom)) {
						// on peut les merger
						firstStates.get(j).prevs.addAll(firstStates.get(i).prevs);
						firstStates.get(j).nexts.addAll(firstStates.get(i).nexts);
						// maj les "next" des prevs
						for (State st : firstStates.get(i).prevs) {
							st.nexts.remove(firstStates.get(i));
							st.nexts.add(firstStates.get(j));
						}
						// maj les "prevs" des next
						for (State st : firstStates.get(i).nexts) {
							st.prevs.remove(firstStates.get(i));
							st.prevs.add(firstStates.get(j));
						}
						states.remove(firstStates.get(i));
						toremove.add(firstStates.get(i));
						break;
					}
				}
			}
			firstStates.removeAll(toremove);
			for (State st : firstStates)
				factoriserecFwd(states,st);
			
			printGraph(states);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void factoriserecFwd(ArrayList<State> states, State root) {
		System.out.println("factorisefwd "+root.nom);
		if (root.nexts.size()<2) return;
		List<State> nextStates = new ArrayList<MEMM.State>(root.nexts);
		for (int i=0;i<nextStates.size();i++) {
			for (int j=0;j<i;j++) {
				if (nextStates.get(i).nom.equals(nextStates.get(j).nom)) {
					// il faut verifier que tous les predecesseurs sont les memes
					boolean oktomerge = true;
					if (nextStates.get(i).prevs.size()!=nextStates.get(j).prevs.size()) oktomerge=false;
					if (oktomerge)
						for (State predi : nextStates.get(i).prevs) {
							if (!nextStates.get(j).prevs.contains(predi))  {oktomerge=false; break;}
						}
					if (oktomerge) {
						// on peut les merger
						nextStates.get(j).prevs.addAll(nextStates.get(i).prevs);
						nextStates.get(j).nexts.addAll(nextStates.get(i).nexts);
						for (State stt : nextStates.get(i).prevs) {
							stt.nexts.remove(nextStates.get(i));
							stt.nexts.add(nextStates.get(j));
						}
						for (State stt : nextStates.get(i).nexts) {
							stt.prevs.remove(nextStates.get(i));
							stt.prevs.add(nextStates.get(j));
						}
						states.remove(nextStates.get(i));
						root.nexts.remove(nextStates.get(i));
						break;
					}
				}
			}
		}
		for (State st : root.nexts) {
			factoriserecFwd(states,st);
		}
	}

	private static void factoriserecBcw(ArrayList<State> states, ArrayList<State> fixedNodes) {
		for (State st : fixedNodes) {
			if (st.prevs.size()==0) continue;
			List<State> lastStates = new ArrayList<MEMM.State>(st.prevs);
			// seuls les predecesseurs de st sont mergeables
			ArrayList<State> toremove = new ArrayList<MEMM.State>();
			for (int i=0;i<lastStates.size();i++) {
				for (int j=0;j<i;j++) {
					if (lastStates.get(i).nom.equals(lastStates.get(j).nom)) {
						// on peut les merger
						lastStates.get(j).prevs.addAll(lastStates.get(i).prevs);
						lastStates.get(j).nexts.addAll(lastStates.get(i).nexts);
						for (State stt : lastStates.get(i).prevs) {
							stt.nexts.remove(lastStates.get(i));
							stt.nexts.add(lastStates.get(j));
						}
						states.remove(lastStates.get(i));
						toremove.add(lastStates.get(i));
						break;
					}
				}
			}
			st.prevs.removeAll(toremove);
			toremove.clear();
			for (State stt : lastStates) {
				toremove.addAll(stt.prevs);
			}
			factoriserecBcw(states,toremove);
		}
	}
	
	public static void printGraph(List<State> states) throws IOException {
		// dot DOT
		PrintWriter f = FileUtils.writeFileUTF("graph.dot");
		f.println("digraph XX {");
		for (State st : states) {
			if (st.prevs.size()==0) f.println("IN -> "+st.nom+"_"+st.id+" ;");
			if (st.nexts.size()==0) f.println(st.nom+"_"+st.id+" -> OUT ;");
			for (State tgt : st.nexts) {
				f.println(st.nom+"_"+st.id+" -> "+tgt.nom+"_"+tgt.id+" ;");
			}
		}
		f.println("}");
		f.close();
	}

	public static void main(String args[]) {
		generate(2);
		factorise("graphs.out");
	}
}
