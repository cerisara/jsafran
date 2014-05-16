package jsafran.parsing.unsup.criteria;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import jsafran.DetGraph;
import jsafran.parsing.unsup.Criterion;

/**
 * actually more general than just verbs now...
 * 
 * A frame = a 5-tuplet (AUX,MOD,SUJ,NONE,NONE) in sorted order (except for NONE that is in the end) !!
 * each frame is associated with the HEAD.
 * So, the index of a (HEAD,Frame) = index(HEAD)*#Frames + index(Frame)
 * 
 * This version optimizes sparsity in the _total search space_
 * This is different from the more "intuitive" version that would minimize sparsity for every possible HEAD.
 * But this would require writing a new class, and Gillenwater has shown this may be worse.
 * 
 * But on the other hand, optimizing sparsity in the _total_ search space creates problems when drawing the
 * plate diagram: it's not easy to exploit both total space sparsity and naive Bayes combination of criteria !
 * It may thus be best to rather optimize local sparsity; I'll generalize Criterion to do this.
 * 
 * TODO: other improvements TODO:
 * - add Java rules on top of cfg RULES
 * - CC: special rule that removes a complete subtree on its left, parses the resulting sentence and computes its score,
 *   then removes a complete subtree on its right, parses + scores, and builds a PDF based on the left + right scores
 * - ...
 * 
 * @author xtof
 *
 */
public class VerbFrames extends Criterion {
	
	// max nb of dependents for a given head (if there are more, the repeated are removes, and then the last ones are removed)
	final int ndeps = 5;
	final String[] deptypes = { // they are sorted !
			"APPOS",
			"ATTO",
			"ATTS",
			"AUX",
			"CC",
			"COMP",
			"DET",
			"DUMMY",
			"JUXT",
			"MOD",
			"MultiMots",
			"NONE",
			"OBJ",
			"POBJ",
			"REF",
			"SUJ",
	};
	final int noneidx = Arrays.binarySearch(deptypes, "NONE");
	final int nframes = (int)Math.pow(deptypes.length,ndeps);
	private LexPref lexpref;
	boolean isLexPrefInitialized = false;
	final int nPerType[] = new int[deptypes.length];
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("KNOWN FRAMES : \n");
		for (int head : counts.keySet()) {
			HashMap<Long, Long> tmp = counts.get(head);
			for (long idx: tmp.keySet()) {
				Long n = tmp.get(idx);
				if (n>0) {
					int z = (int)idx;
					sb.append("("+lexpref.getWord4Index(head)+")\t");
					for (int i=0;i<ndeps-1;i++) {
						int m = (int)(z%deptypes.length);
						sb.append(deptypes[m]+"\t");
						z/=deptypes.length;
					}
					sb.append(deptypes[z]+"\t");
					sb.append(n+"\n");
				}
			}
		}
		return sb.toString();
	}
	
	@Override
	public long getSpaceSize() {
		final int r = nframes;
		return r;
	}
	public void init(List<DetGraph> gs) {
		isLexPrefInitialized=true;
	}

	public VerbFrames(LexPref lp) {
		lexpref=lp;
		System.out.println("nframes "+nframes);
	}
	
	int[] metai;
	
	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		long[] res = new long[g.getNbMots()];
		metai = new int[g.getNbMots()];
		for (int i=0;i<g.getNbMots();i++) {
			metai[i] = lexpref.getWordIndex(g.getMot(i));
			res[i] = getIndex(g, i);
		}
		return res;
	}

	private int getIndex(DetGraph g, int v) {
		List<Integer> fils = g.getFils(v);
		Arrays.fill(nPerType, 0);
		int ntot=0;
		for (int w : fils) {
			int d = g.getDep(w);
			String l = g.getDepLabel(d);
			int type = Arrays.binarySearch(deptypes, l);
			if (type>=0) {
				nPerType[type]++;
				ntot++;
			} // sinon, on ne tient pas compte de cette dep type
		}
		while (ntot>=ndeps) {
			// first, remove duplicates
			final String[] duplicateOrder = {"MOD","DUMMY","JUXT","CC"};
			boolean nochange=true;
			for (String mod : duplicateOrder) {
				final int i = Arrays.binarySearch(deptypes, mod);
				if (nPerType[i]>1) {
					nPerType[i]--;
					ntot--;
					nochange=false;
					break;
				}
			}
			if (nochange) break;
		}
		while (ntot>=ndeps) {
			// second, remove the first ones
			for (int i=0;i<nPerType.length;i++) {
				if (nPerType[i]>0) {
					nPerType[i]--;
					break;
				}
			}
			ntot--;
		}
		
		int idx = 0;
		int mult = 1;
		for (int i=0;i<deptypes.length;i++) {
			while (nPerType[i]>0) {
				idx+=mult*i;
				nPerType[i]--;
				mult*=deptypes.length;
			}
		}
		// on complete avec les NONE
		for (int i=ntot;i<ndeps;i++) {
			idx+=mult*noneidx;
			mult*=deptypes.length;
		}
		assert mult==Math.pow(deptypes.length, ndeps);
		return idx;
	}

	@Override
	public int getMetaIndex(int t) {
		return metai[t];
	}
}
