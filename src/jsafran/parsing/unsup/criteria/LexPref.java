package jsafran.parsing.unsup.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.Mot;
import jsafran.parsing.unsup.Criterion;

public class LexPref extends Criterion {

	public ArrayList<String> forms2keep = new ArrayList<String>();
	public ArrayList<String> postag2keep = new ArrayList<String>();
	int nFramesDiff = 0;
	int[] metai;

	HashMap<Integer, Integer> wordidx2freq = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> depidx2freq = new HashMap<Integer, Integer>();
	
	public void init(List<DetGraph> gs) {
		clearCounts();
		
		// compute TFs
		for (DetGraph g : gs) {
			for (int i=0;i<g.getNbMots();i++) {
				int j = getWordIndex(g.getMot(i));
				Integer n = wordidx2freq.get(j);
				if (n==null) n=1;
				else n++;
				wordidx2freq.put(j, n);
			}
			for (Dep d : g.deps) {
				Integer n = depidx2freq.get(d.type);
				if (n==null) n=1;
				else n++;
				depidx2freq.put(d.type, n);
			}
		}

		{
			final int nFormsMin = 30;
			forms2keep.clear();
			postag2keep.clear();
			// uses FORMS for the most frequent words
			HashMap<String, Integer> vocForms =new HashMap<String, Integer>();
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					String w = g.getMot(i).getForme();
					Integer n = vocForms.get(w);
					if (n==null) n=0;
					n++;
					vocForms.put(w, n);
				}
			}
			for (String w : vocForms.keySet()) {
				if (vocForms.get(w)>=nFormsMin) forms2keep.add(w);
			}
			Collections.sort(forms2keep);
			System.out.println("keeping nFORMS= "+forms2keep.size());
			// si, au test, la forme n'est pas dans cette liste, alors on utilisera le POStag
		}
		{
			final int nPOSMin = 1;
			// uses FORMS for the most frequent words
			HashMap<String, Integer> vocPOS =new HashMap<String, Integer>();
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					String postag = g.getMot(i).getPOS();
					Integer n = vocPOS.get(postag);
					if (n==null) n=0;
					n++;
					vocPOS.put(postag, n);
				}
			}
			for (String postag : vocPOS.keySet()) {
				if (vocPOS.get(postag)>=nPOSMin) postag2keep.add(postag);
			}
			// si, au test, le POStag n'est pas dans cette liste, alors c'est:
			postag2keep.add("UNK");
			// on doit aussi considerer le ROOT de la phrase:
			postag2keep.add("ROOT");
			Collections.sort(postag2keep);
			System.out.println("keeping nPOS= "+postag2keep.size());
		}
		nFramesDiff=(forms2keep.size()+postag2keep.size());
		System.out.println("lexpref nFrames = "+nFramesDiff);
	}

	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		final int offset = forms2keep.size()+postag2keep.size();
		long[] res = new long[g.getNbMots()];
		metai = new int[res.length];
		for (int i=0;i<g.getNbMots();i++) {
			int d = g.getDep(i);
			if (d>=0) {
				metai[i]=(g.deps.get(d).type+1)*offset;
				res[i] = getWordIndex(g.getMot(g.getHead(d)));
			} else {
				metai[i]=0;
				res[i] = getWordIndex(null);
			}
			metai[i] += getWordIndex(g.getMot(i));
		}
		return res;
	}

	int getHeadFromIdx(int idx) {
		final int offset = forms2keep.size()+postag2keep.size();
		return (idx%(offset*offset))/offset;
	}
	int getGovFromIdx(int idx) {
		final int offset = forms2keep.size()+postag2keep.size();
		return ((idx%(offset*offset))%offset);
	}
	
	public String getWord4Index(int idx) {
		if (idx<forms2keep.size()) return forms2keep.get(idx);
		idx-=forms2keep.size();
		return postag2keep.get(idx);
	}
	
	public int getWordIndex(Mot m) {
		if (m==null) {
			// assume ROOT
			return forms2keep.size()+Collections.binarySearch(postag2keep, "ROOT");
		}
		int midx;
		int i=Collections.binarySearch(forms2keep, m.getForme());
		if (i>=0) midx = i;
		else {
			i=Collections.binarySearch(postag2keep, m.getPOS());
			if (i>=0) midx=forms2keep.size()+i;
			else midx=forms2keep.size()+Collections.binarySearch(postag2keep, "UNK");
		}
		return midx;
	}
	
	@Override
	public long getSpaceSize() {
		return nFramesDiff;
	}

	@Override
	public int getMetaIndex(int t) {
		return metai[t];
	}
}
