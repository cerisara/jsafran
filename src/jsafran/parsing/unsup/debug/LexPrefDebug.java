package jsafran.parsing.unsup.debug;
import java.util.HashMap;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.unsup.Criterion;
import jsafran.parsing.unsup.criteria.LexPref;

/**
 * There is a big problem with the current computation of lexpref:
 * its score on gold parses is lower than its score on "right-attach" baseline.
 * 
 * This is due to "n-gram regularity". The worst example is
 * "... l'avenir en filigrane"
 * GOLD: "en" -> "avenir"
 * RIGHT-ATTACH: "en" -> "filigrane"
 * 
 * for R-ATT:
 * 	P(W|H)=P(en|filigrane) is very high, because filigrane is always preceeded by en
 *  P(W|H)=P(filigrane|l') is low
 * for GOLD:
 * 	P(W|H)=P(en|avenir) is much lower, because avenir is linked to many different words.
 * 	P(W|H)=P(avenir|en) is low, because en preceedes many words
 * 
 * TODO: pour pallier à ce problème, il faut modéliser P(W,H) !!
 * 
 * @author cerisara
 *
 */
public class LexPrefDebug {
	static double computeLogPost(List<DetGraph> gs, Criterion c) {
		c.clearCounts();
		c.init(gs);
		
		for (int gidx=0;gidx<gs.size();gidx++) {
			DetGraph g = gs.get(gidx);
			c.increaseCounts(g);
		}
		
		double logpost = 0;
		for (int gidx=0;gidx<gs.size();gidx++) {
			DetGraph g = gs.get(gidx);
			double probApres = c.getLogPost(g);
			logpost+=probApres;
		}
		logpost/=(double)gs.size();
		System.out.println("avg log-posterior: "+logpost);
		return logpost;
	}
	
	public static double[] getLogPost(DetGraph g, Criterion c) {
		long[] idxs = c.getCriterionIndexes(g);
		double[] res= new double[idxs.length];
		for (int t=0;t<idxs.length;t++) {
			long idx = idxs[t];
			int metai = c.getMetaIndex(t);
			
			Long n = 0l;
			HashMap<Long, Long> tmp = c.counts.get(metai);
			if (tmp!=null) {
				n = tmp.get(idx);
				if (n==null) n=0l;
			}
			double num = Math.log(c.alpha+(double)n);
			n = c.countstot.get(metai);
			if (n==null) n=0l;
			double sum = Math.log((double)n+(double)c.getSpaceSize()*c.alpha);
			num-=sum;

			res[t]=num;
		}
		return res;
	}

	public static void main(String[] args) {
//		final String dir = "C:/xtof/git/jsafran/";
		final String dir = "/home/xtof/git/jsafran/";
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gsok = gio.loadAllGraphs(dir+"test2009.xml");
		List<DetGraph> gsko = gio.loadAllGraphs(dir+"tt0.xml");
		
		LexPref lpok = new LexPref();
		lpok.init(gsok);
		LexPref lpko = new LexPref();
		lpko.init(gsko);
		
		double logpostok = computeLogPost(gsok, lpok);
		double logpostko = computeLogPost(gsko, lpko);
		
		System.out.println("nforms "+lpok.forms2keep.size()+" "+lpok.postag2keep.size());
		System.out.println("logpost ok "+logpostok);
		System.out.println("logpost ko "+logpostko);
		
		int nok=0,nko=0;
		int gworse=-1;
		double maxdiff=0;
		for (int i=0;i<gsok.size();i++) {
			DetGraph gok = gsok.get(i);
			DetGraph gko = gsko.get(i);
			double postok = lpok.getLogPost(gok);
			double postko = lpko.getLogPost(gko);
			if (postok>=postko) nok++;
			else nko++;
			double diff = postko-postok;
			if (diff>maxdiff) {
				maxdiff=diff;
				gworse=i;
			}
		}
		System.out.println("nok "+nok+" nko "+nko);
		
		{
			// Show the worse at the level of SENTENCE
			int i = gworse;
			DetGraph gok = gsok.get(i);
			DetGraph gko = gsko.get(i);
			double postok = lpok.getLogPost(gok);
			double postko = lpko.getLogPost(gko);
			System.out.println("worse pok="+postok+" pko="+postko);
			DetGraph[] gg = {gok,gko};
//			JSafran.viewGraph(gg);
		}
		
		{
			// Show the worse at the level of WORD 
			
			maxdiff=0;
			int wworse=-1;
			for (int i=0;i<gsok.size();i++) {
				DetGraph gok = gsok.get(i);
				DetGraph gko = gsko.get(i);
				double[] postok = getLogPost(gok,lpok);
				double[] postko = getLogPost(gko,lpko);
				for (int j=0;j<postok.length;j++) {
					double diff = postko[j]-postok[j];
					if (diff>maxdiff) {
						maxdiff=diff;
						gworse=i;
						wworse=j;
					}
				}
			}
			DetGraph gok = gsok.get(gworse);
			DetGraph gko = gsko.get(gworse);
			DetGraph[] gg = {gok,gko};
			System.out.println("word worse "+wworse);
			JSafran.viewGraph(gg,wworse);
		}
	}
}
