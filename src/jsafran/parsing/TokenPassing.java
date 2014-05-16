package jsafran.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenPassing {
	final static boolean secondbest=false;

	List<ArrayList<String>> paths = new ArrayList<ArrayList<String>>();
	List<ArrayList<String>> secondpaths = new ArrayList<ArrayList<String>>();
	List<Float> pathProb = new ArrayList<Float>();
	List<Float> secondpathProb = new ArrayList<Float>();
	StateNameTransformer transf = null;

	// From a metaState to all possible states
	Map<String, Map<String, Float>> possibleTrans;

	// temporary variables used during extension
	List<ArrayList<String>> newpaths=null;
	List<ArrayList<String>> secondnewpaths=null;
	List<Float> newpathProb=null;
	List<Float> secondnewpathProb=null;

	public TokenPassing(String[] initStates, float[] initProbs, StateNameTransformer metastatedef) {
		transf=metastatedef;
		assert initStates.length==initProbs.length;
		for (int i=0;i<initStates.length;i++) {
			ArrayList<String> p = new ArrayList<String>();
			p.add(initStates[i]);
			paths.add(p);
			pathProb.add(initProbs[i]);
		}
	}

	public void createExtension() {
		newpaths = new ArrayList<ArrayList<String>>();
		newpathProb = new ArrayList<Float>();
		secondnewpaths = new ArrayList<ArrayList<String>>();
		secondnewpathProb = new ArrayList<Float>();
	}
	public void finalizeExtension() {
		paths=newpaths;
		pathProb=newpathProb;
		secondpaths=secondnewpaths;
		secondpathProb=secondnewpathProb;
	}
	private String getMetaState(String st) {
		if (transf==null) return st;
		return transf.transform(st);
	}

	private int getSamePath(String lastState, List<ArrayList<String>> newpaths) {
		String y=lastState;
		if (transf!=null) y=transf.transform(y);
		for (int i=0;i<newpaths.size();i++) {
			String x = newpaths.get(i).get(newpaths.get(i).size()-1);
			if (transf!=null) {
				if (transf.transform(x).equals(y)) return i;
			}
		}
		return -1;
	}

	/**
	 * 
	 * @param possibleTransitions: allowed transitions from a metaState to all specific states !!
	 * 
	 * @param newStatesScores : for each specific state
	 */
	public void extend(Map<String, Map<String, Float>> possibleTransitions, Map<String,Float> newStatesScores) {
		possibleTrans = new HashMap<String, Map<String,Float>>();
		for (String mot1: possibleTransitions.keySet()) {
			Map<String, Float> mm = possibleTransitions.get(mot1);
			HashMap<String, Float> mmi = new HashMap<String, Float>();
			possibleTrans.put(mot1, mmi);
			for (String mot2: mm.keySet()) {
				mmi.put(mot2, mm.get(mot2));
			}
		}
		for (int i=0;i<paths.size();i++) {
			List<String> path = paths.get(i);
			String laststate = path.get(path.size()-1);
//			String lastmot = laststate;
			
			String lastmot = laststate.substring(0,2);
			if (!possibleTransitions.keySet().contains(lastmot)) continue;
			extendPath(path,pathProb.get(i),newStatesScores);
		}
		if (secondbest) {
			for (int i=0;i<secondpaths.size();i++) {
				List<String> path = secondpaths.get(i);
				String laststate = path.get(path.size()-1);
				Map<String, Float> nexts = possibleTrans.get(getMetaState(laststate));
				if (nexts!=null) {
					for (String nextState : nexts.keySet()) {
						Float newStateScore = newStatesScores.get(nextState);
						assert newStateScore!=null;
						float transprob = nexts.get(nextState);
						float oldPathProb = secondpathProb.get(i);
						float newprob = oldPathProb*transprob*newStateScore;

						// Un path venant du previous 2best ne peut pas etre meilleur que le new 1best, mais il peut 
						// etre meilleur que le new 2best !
						int samePath = getSamePath(nextState,secondnewpaths);
						if (samePath>=0) {
							if (newprob>secondnewpathProb.get(samePath)) {
								ArrayList<String> newpath = new ArrayList<String>();
								newpath.addAll(path); newpath.add(nextState);
								secondnewpaths.set(samePath, newpath);
								secondnewpathProb.set(samePath, newprob);
							}
						} else {
							ArrayList<String> newpath = new ArrayList<String>();
							newpath.addAll(path); newpath.add(nextState);
							secondnewpaths.add(newpath);
							secondnewpathProb.add(newprob);
						}
					}
				}
			}
		}
	}
	public void extendPath(List<String> path, float pathprob, Map<String,Float> newStatesScores) {
		String laststate = path.get(path.size()-1);
		Map<String, Float> nexts = possibleTrans.get(getMetaState(laststate));
		if (nexts!=null) {
			for (String nextState : nexts.keySet()) {
				Float newStateScore = newStatesScores.get(nextState);

//				if (nextState.equals("NA") && path.size()==1 && path.get(0).equals("LAsuj"))
//					System.out.println("debug "+newStateScore+" "+pathprob);

				if (newStateScore==null)
					System.out.println("DEBUG STATE SCORE NULL "+nextState);
				float transprob = nexts.get(nextState);
				float oldPathProb = pathprob;
				float newprob = oldPathProb*transprob*newStateScore;

				// 1st order Markov
				int samePath = getSamePath(nextState,newpaths);
				if (samePath>=0) {
					if (newprob>newpathProb.get(samePath)) {
						if (secondbest) {
							// l'ancien best peut devenir le meilleur 2eme...
							int samePath2 = getSamePath(nextState,secondnewpaths);
							if (samePath2>=0) {
								if (newpathProb.get(samePath)>secondnewpathProb.get(samePath2)) {
									secondnewpaths.set(samePath2,newpaths.get(samePath));
									secondnewpathProb.set(samePath2,newpathProb.get(samePath));
								}
							} else {
								secondnewpaths.add(newpaths.get(samePath));
								secondnewpathProb.add(newpathProb.get(samePath));
							}
						}
						// le nouveau chemin remplace l'ancien best
						ArrayList<String> newpath = new ArrayList<String>();
						newpath.addAll(path); newpath.add(nextState);
						newpaths.set(samePath, newpath);
						newpathProb.set(samePath, newprob);
					} else if (secondbest) {
						// le nouveau path n'est peut-etre pas meilleur que le previous 1best, mais il est peut-etre meilleur que le previous 2-best
						int samePath2 = getSamePath(nextState,secondnewpaths);
						if (samePath2>=0) {
							if (newprob>secondnewpathProb.get(samePath2)) {
								ArrayList<String> newpath = new ArrayList<String>();
								newpath.addAll(path); newpath.add(nextState);
								secondnewpaths.set(samePath2,newpath);
								secondnewpathProb.set(samePath2,newprob);
							}
						} else {
							ArrayList<String> newpath = new ArrayList<String>();
							newpath.addAll(path); newpath.add(nextState);
							secondnewpaths.add(newpath);
							secondnewpathProb.add(newprob);
						}
					}
				} else {
					ArrayList<String> newpath = new ArrayList<String>();
					newpath.addAll(path); newpath.add(nextState);
					newpaths.add(newpath);
					newpathProb.add(newprob);
				}
			}
		}
	}

	/**
	 * 
	 * @param prob: (output): prob of the best path
	 * @return
	 */
	public List<String> getBestPath(float[] prob) {
		assert prob==null||prob.length==1;
		int bestp = 0;
		for (int i=1;i<pathProb.size();i++) {
			if (pathProb.get(i)>pathProb.get(bestp)) bestp=i;
		}
		if (prob!=null)
			prob[0]=pathProb.get(bestp);
		return paths.get(bestp);
	}

	public String[] getLastStates(List<Float> probs) {
		String[] res = new String[paths.size()];
		for (int i=0;i<res.length;i++) {
			res[i]=paths.get(i).get(paths.get(i).size()-1);
			if (probs!=null) probs.add(pathProb.get(i));
		}
		return res;
	}
	public List<Float> getAllPathProbs() {
		return pathProb;
	}
	public List<ArrayList<String>> getAllPaths() {
		return paths;
	}
	public List<Float> getAllPathProbs2() {
		return secondpathProb;
	}
	public List<ArrayList<String>> getAllPaths2() {
		return secondpaths;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TokenPassing:\n");
		for (int i=0;i<paths.size();i++) {
			sb.append(i+": "+pathProb.get(i)+" : "+paths.get(i)+'\n');
		}
		return sb.toString();
	}

	/**
	 * used to replace the full name of the state, which is saved in the path, by an
	 * "equivalence class" of names to group together states that may not be dissociated.
	 * In other words, to handle "meta-states".
	 * 
	 * @author xtof
	 *
	 */
	public interface StateNameTransformer {
		public String transform(String s);
	}
}
