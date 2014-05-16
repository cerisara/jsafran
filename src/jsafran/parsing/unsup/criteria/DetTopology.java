package jsafran.parsing.unsup.criteria;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;

/**
 * On pourrait ne pas ajouter le cout topologique dans les critere, mais seulement comme contrainte a priori sur les arcs autorises.
 * Mais on devrait alors enumerer tous les arcs autorises possibles.
 * Je prefere donc l'ajouter comme critere.
 * De plus, ceci permet d'avoir une contrainte topologique soft
 * 
 * @author xtof
 *
 */
public class DetTopology implements DetCriterion {

	final double costHigh = 1000, costMed = 10, costLow=1;
	
	int penaltyRoots(DetGraph g) {
		int n=0;
		for (int i=0;i<g.getNbMots();i++) {
			int d=g.getDep(i);
			if (d<0) n++;
		}
		if (n==0) return 10;
		return n-1;
	}
	
	int penaltyCycles(DetGraph g) {
		final int maxncycles=10;
		HashSet<Integer> dejavus = new HashSet<Integer>();
		int ncycles = 0;
		for (Dep de : g.deps) {
			dejavus.clear();
			int gov = de.gov.getIndexInUtt()-1;
			int head = de.head.getIndexInUtt()-1;
			int w = head;
			dejavus.add(gov);
			dejavus.add(head);
			for (;;) {
				int d = g.getDep(w);
				if (d<0) break;
				w=g.getHead(d);
				if (dejavus.contains(w)) {
					ncycles++;
					if (ncycles>maxncycles) return maxncycles;
					break;
				}
				dejavus.add(w);
			}
		}
		return ncycles;
	}
	
	int penaltyProj(DetGraph g) {
		final int maxncrosscut=1000000;
		int ncrosscut=0;
		for (int i=0;i<g.getNbMots();i++) {
			int d=g.getDep(i);
			if (d>=0) {
				int h=g.getHead(d);
				if (h>i) {
					for (int j=i+1;j<h;j++) {
						d=g.getDep(j);
						if (d>=0) {
							int hh=g.getHead(d);
							if (hh>h||hh<i) ncrosscut++;
						} else ncrosscut++;
					}
				} else {
					for (int j=i-1;j>h;j--) {
						d=g.getDep(j);
						if (d>=0) {
							int hh=g.getHead(d);
							if (hh>i||hh<h) ncrosscut++;
						} else ncrosscut++;
					}
				}
			}
			if (ncrosscut>=maxncrosscut) return maxncrosscut;
		}
		return ncrosscut;
	}
	
	/*
	 * Le nb moyen de fils sur train2011 croit rapidement en fct de la longueur de la phrase pour atteindre 0.9
	 * pour des phrases de 10 mots puis converge lentement vers 0.99 à la limite.
	 * 
	 * Le nb max de fils augmente rapidement jusqu'a 8 puis oscille entre 4 et 7.
	 * 
	 * TODO: it's too slow !! optimize it !
	 */
	int penaltyNFils(DetGraph g) {
		// penalty sur nb de fils max
		int pen=0;
		for (int i=0;i<g.getNbMots();i++) {
			int nf = g.getFils(i).size();
			if (nf>6) pen+=nf-6;
		}
		
		// penalty sur nb de fils moyen
		float avgnf = 0;
		for (int i=0;i<g.getNbMots();i++) {
			avgnf+=g.getFils(i).size();
		}
		avgnf/=(float)g.getNbMots();
		if (avgnf<0.5) pen+=(int)((0.5f-avgnf)*10f);
		return pen;
	}
	int penaltyLength(DetGraph g) {
		int pen=0;
		for (Dep d : g.deps) {
			int len = Math.abs(d.head.getIndexInUtt()-d.gov.getIndexInUtt());
			pen += len;
		}
		return pen;
	}
	
	final double logpOK=Math.log(0.9), logpKO=Math.log(0.1), logpUNK=Math.log(0.5);
	final String[] posLeaf = {"DET","PRO","ADV","ADJ"};
	final String[] posNotLeaf = {"VER","KON","NOM"};
	float priorLeaves(DetGraph g) {
		float prior=0;
		for (int i=0;i<g.getNbMots();i++) {
			String p = g.getMot(i).getPOS();
			boolean shallBeLeaf=false, shallNotBeLeaf=false;
			for (String s : posLeaf) {
				if (p.startsWith(s)) {shallBeLeaf=true; break;}
			}
			if (!shallBeLeaf) {
				for (String s : posNotLeaf) {
					if (p.startsWith(s)) {shallNotBeLeaf=true; break;}
				}
			}
			if (!shallBeLeaf&&!shallNotBeLeaf) prior+=logpUNK;
			else {
				boolean isleaf = g.getFils(i).size()==0;
				if (shallBeLeaf) prior+=isleaf?logpOK:logpKO;
				else if (shallNotBeLeaf) prior+=isleaf?logpKO:logpOK;
			}
		}
		return prior*10;
	}
	
	@Override
	public double getPenalty(DetGraph g) {
		double p=0;
		p+=-(double)penaltyRoots(g)*costHigh;
		p+=-(double)penaltyCycles(g)*costHigh;
		p+=-(double)penaltyProj(g)*costMed;
		p+=-(double)penaltyLength(g)*costLow;
//		p+=-(double)penaltyNFils(g)*costMed;
		
//		p+=(double)priorLeaves(g);
		return p;
	}

	/**
	 * le main sert seulement à valider sur un corpus les seuils choisis pour les penalty
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("/home/xtof/git/jsafran/train2011.xml");
		
		HashMap<String, Integer> pos2ntot = new HashMap<String, Integer>();
		HashMap<String, Integer> pos2leaves = new HashMap<String, Integer>();
		
		for (DetGraph g : gs) {
			for (int i=0;i<g.getNbMots();i++) {
				List<Integer> fils = g.getFils(i);
				if (fils.size()==0) {
					Integer n = pos2leaves.get(g.getMot(i).getPOS());
					if (n==null) n=1; else n++;
					pos2leaves.put(g.getMot(i).getPOS(),n);
				}
				{
					Integer n = pos2ntot.get(g.getMot(i).getPOS());
					if (n==null) n=1; else n++;
					pos2ntot.put(g.getMot(i).getPOS(),n);
				}
			}
		}
		
		try {
			PrintWriter f = new PrintWriter(new FileWriter("/tmp/toto"));
			for (String p: pos2ntot.keySet()) {
				Integer n = pos2leaves.get(p);
				if (n==null) n=0;
				float r = (float)n/(float)pos2ntot.get(p);
				f.println(p+" "+r);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
