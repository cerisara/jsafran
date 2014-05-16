package jsafran.parsing.unsup.criteria;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.parsing.unsup.Criterion;

public class GeneralFrames extends Criterion {
	final int nslots = 5;
	LexPref lexpref;
	
	// head index -> dep type -> nb_fils (slot) -> counts
	private HashMap<Integer, HashMap<Integer, HashMap<Integer,Integer>>> counts = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
	
	public GeneralFrames(LexPref lp) {
		lexpref=lp;
	}
	
	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		return null;
	}

	@Override
	public long getSpaceSize() {
		final int r = nslots;
		return r;
	}

	final double logpost_rienvu = Math.log(alpha)-Math.log((double)getSpaceSize()*alpha);
	
	public double getLogPost(DetGraph g) {
		// contient l'indice du SLOT observe sur chaque mot de la phrase et pour chaque dep possible
		// on a donc nmots * ndeps log-post par phrases
		int[][] slots = new int[g.getNbMots()][Dep.depnoms.length];
		
		for (Dep d : g.deps) {
			// cumul for each word in the sentence the number of deps
			int n=slots[d.head.getIndexInUtt()-1][d.type];
			if (n<nslots-1) {
				slots[d.head.getIndexInUtt()-1][d.type]++;
			}
		}
		
		// then compute for each word the log-post
		double logpost=0;
		for (int i=0;i<slots.length;i++) {
			int headidx=lexpref.getWordIndex(g.getMot(i));
			HashMap<Integer,HashMap<Integer,Integer>> tmp = counts.get(headidx);
			if (tmp==null) {
				// on n'a jamais rien observe pour un HEAD comme celui-ci
				for (int j=0;j<slots[i].length;j++) logpost+=logpost_rienvu;
			} else {
				for (int j=0;j<slots[i].length;j++) {
					Integer n=0;
					HashMap<Integer,Integer> tmp2 = tmp.get(j);
					if (tmp2==null) {
						// on n'a jamais rien observe pour des (HEAD,DEP) comme celui-ci
						logpost+=logpost_rienvu;
					} else {
						n=tmp2.get(slots[i][j]);
						if (n==null) n=0;
						double countstot=0; for (int v : tmp2.values()) countstot+=v;
						double num = Math.log(alpha+(double)n);
						double sum = Math.log(countstot+(double)getSpaceSize()*alpha);
						num-=sum;
						logpost+=num;
					}
				}
			}
		}
		return logpost;
	}
	
	public void increaseCounts(DetGraph g) {
		int[][] slots = new int[g.getNbMots()][Dep.depnoms.length];
		for (Dep d : g.deps) {
			// cumul for each word in the sentence the number of deps of a given type
			int n=slots[d.head.getIndexInUtt()-1][d.type];
			if (n<nslots-1) {
				slots[d.head.getIndexInUtt()-1][d.type]++;
			}
		}
		for (int i=0;i<slots.length;i++) {
			int headidx=lexpref.getWordIndex(g.getMot(i));
			HashMap<Integer,HashMap<Integer,Integer>> tmp = counts.get(headidx);
			if (tmp==null) {
				tmp = new HashMap<Integer, HashMap<Integer,Integer>>();
				counts.put(headidx, tmp);
			}
			for (int j=0;j<slots[i].length;j++) {
				HashMap<Integer,Integer> tmp2 = tmp.get(j);
				if (tmp2==null) {
					tmp2 = new HashMap<Integer, Integer>();
					tmp.put(j, tmp2);
				}
				Integer n = tmp2.get(slots[i][j]);
				if (n==null) n=1;
				else n++;
				tmp2.put(slots[i][j], n);
			}
		}
	}
	
	public void decreaseCounts(DetGraph g) {
		int[][] slots = new int[g.getNbMots()][Dep.depnoms.length];
		for (Dep d : g.deps) {
			// cumul for each word in the sentence the number of deps
			int n=slots[d.head.getIndexInUtt()-1][d.type];
			if (n<nslots-1) {
				slots[d.head.getIndexInUtt()-1][d.type]++;
			}
		}
		for (int i=0;i<slots.length;i++) {
			int headidx=lexpref.getWordIndex(g.getMot(i));
			HashMap<Integer,HashMap<Integer,Integer>> tmp = counts.get(headidx);
			if (tmp==null) System.out.println("WARNING1: decrease counts that do not exist ! "+g);
			else {
				for (int j=0;j<slots[i].length;j++) {
					HashMap<Integer,Integer> tmp2 = tmp.get(j);
					if (tmp2==null) {
						System.out.println("WARNING2: decrease counts that do not exist ! "+j+" "+Arrays.toString(Dep.depnoms)+" "+g);
						System.exit(1);
					}
					else {
						Integer n = tmp2.get(slots[i][j]);
						if (n==null) System.out.println("WARNING3: decrease counts that do not exist ! "+g);
						else {
							n--;
							if (n<0) System.out.println("WARNING4: decrease counts that do not exist ! "+g);
							else if (n==0) tmp2.remove(slots[i][j]);
							else tmp2.put(slots[i][j], n);
						}
					}
				}
			}
		}
	}

	@Override
	public int getMetaIndex(int t) {
		// TODO Auto-generated method stub
		return 0;
	}
}
