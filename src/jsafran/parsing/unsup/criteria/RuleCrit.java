package jsafran.parsing.unsup.criteria;

import java.util.Arrays;
import java.util.HashMap;

import jsafran.DetGraph;
import jsafran.parsing.unsup.Criterion;
import jsafran.parsing.unsup.Voc;

/**
 * We now sample directly the rules, which generate the words: P(W|R)
 * This is the compound Mult-Dir that is modeled here.
 * 
 * by default, "no rule" = -1
 * 
 * TODO:
 * - (i) model rules' priors with a Mult-Dir; (ii) with asymetric priors, in favor of specific rules (== order rules in a few classes)
 * - init (i) with random sampling; (ii) without any counts: only priors 
 */
public class RuleCrit extends Criterion {
	Voc voc;
	
	public RuleCrit(Voc v) {
		voc=v;
	}
	
	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		long[] res = new long[g.getNbMots()];
		for (int i=0;i<res.length;i++) {
			res[i] = voc.getWordIndex(g.getMot(i));
		}
		return res;
	}

	@Override
	public int getMetaIndex(int t) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getSpaceSize() {
		return voc.forms2keep.size()+voc.postag2keep.size();
	}

	public double getLogPost(DetGraph g, int[] word2rule) {
		double res=0;
		// idx contient les mots de la phrase
		long[] idxs = getCriterionIndexes(g);
		assert idxs.length==g.getNbMots();
		assert word2rule.length==g.getNbMots();
		for (int i=0;i<g.getNbMots();i++) {
			// metai contient la rule
			int metai = word2rule[i];

			// partie P(W|R)
			long idx = idxs[i];
			Long n = 0l;
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp!=null) {
				n = tmp.get(idx);
				if (n==null) n=0l;
			}
			double num = Math.log(alpha+(double)n);
			n = countstot.get(metai);
			if (n==null) n=0l;
			double sum = Math.log((double)n+(double)getSpaceSize()*alpha);
			num-=sum;

			// partie P(R)
			
			
			res+=num;
		}
		return res;
	}
	
	public void increaseCounts(DetGraph g, int[] word2rule) {
		long[] idxs = getCriterionIndexes(g);
		assert idxs.length==g.getNbMots();
		assert word2rule.length==g.getNbMots();
		for (int t=0;t<idxs.length;t++) {
			long idx = idxs[t];
			int metai = word2rule[t];
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp==null) {
				tmp = new HashMap<Long, Long>();
				counts.put(metai, tmp);
			}
			Long n = tmp.get(idx);
			if (n==null) n=1l;
			else n++;
			tmp.put(idx, n);
			n = countstot.get(metai);
			if (n==null) n=1l; else n++;
			countstot.put(metai, n);
		}
	}
	public void decreaseCounts(DetGraph g, int[] word2rule) {
		long[] idxs = getCriterionIndexes(g);
		assert idxs.length==g.getNbMots();
		assert word2rule.length==g.getNbMots();
		for (int t=0;t<idxs.length;t++) {
			long idx = idxs[t];
			int metai = word2rule[t];
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp==null) System.out.println("WARNING: decrease counts that do not exist ! "+g);
			Long n = tmp.get(idx);
			if (n==null) System.out.println("WARNING: decrease counts that do not exist ! "+g);
			else n--;
			if (n==0) {
				tmp.remove(idx);
				if (tmp.size()==0) counts.remove(tmp);
			} else tmp.put(idx, n);
			n = countstot.get(metai);
			n--;
			countstot.put(metai, n);
		}
	}

}
