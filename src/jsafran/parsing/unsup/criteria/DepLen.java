package jsafran.parsing.unsup.criteria;

import java.util.HashMap;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.parsing.unsup.Criterion;

/**
 * Si on autorise le treesearch a retourner tous les match pour un dep0, et non plus seulement le premier match
 * par dep0, alors le LAS diminue car on a beaucoup de deps longue-distance.
 * Je veux penaliser ici ces deps longues distances en considerant la distance d'une dep comme une distrib sparse.
 * 
 * @author xtof
 *
 */
public class DepLen extends Criterion {
	final int dmax=10;
	private LexPref lexpref;
	int spacesize;

	public void init(List<DetGraph> gs) {
		// lexpref doit etre deja initialise !
		spacesize=(dmax*2);
	}

	public DepLen(LexPref lp) {
		lexpref=lp;
	}
	
	int[] metai;
	
	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		long[] res = new long[g.getNbMots()];
		metai = new int[g.getNbMots()];
		for (int i=0;i<res.length;i++) {
			int d = g.getDep(i);
			int len = i+1; // par defaut = lien vers ROOT qui est le 1er mot de la phrase
			if (d>=0) {
				metai[i] = g.deps.get(d).type;
				int h = g.getHead(d);
				len = i-h;
			} else metai[i] = Dep.depnoms.length; // ROOT
			if (len<-dmax) len=-dmax;
			else if (len>dmax) len=dmax;
			// ex: dmax=2, idx=0(-2) 1(-1) 2(1) 3(2)
			if (len<0) res[i]=len+dmax;
			else res[i]=len+dmax-1;
		}
		return res;
	}

	@Override
	public long getSpaceSize() {
		return spacesize;
	}

	@Override
	public int getMetaIndex(int t) {
		return metai[t];
	}

	public static void gotoNextUncovered(JSafran main) {
		int worseutt = main.curgraph;
		int nworse = main.allgraphs.get(worseutt).getNbMots()-main.allgraphs.get(worseutt).deps.size();
		for (int j=main.curgraph+1;j<main.allgraphs.size();j++) {
			DetGraph g = main.allgraphs.get(j);
			int n=g.getNbMots()-g.deps.size();
			if (n>nworse) {
				nworse=n;
				worseutt=j;
			}
		}
		main.curgraph=worseutt;
		main.repaint();
	}
}
