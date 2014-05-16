package jsafran.parsing.semisup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import utils.Debug;

import corpus.Ester2;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.Mot;
import jsafran.parsing.unsup.Voc;

/**
 * - parse the full ESTER2 with MATE : this is the starting point of inference
 * - Inference on this corpus with a simple lex-pref model (with Dirichlet prior):
 *   arcs with a low lexpref score may be moved to another head in the neighbourhood to increase the lexpref score
 * - retrain the MATE on this modified corpus
 * 
 * PB1: lexpref is not the first feature to consider (deplen, projectivity, contexts, ...)
 * TODO: use a niaive bayes to combine all these features together
 * 
 * @author xtof
 *
 */
public class LexPref {
	Voc voc = new Voc();
	int niters=1000;
	
	ArrayList<DetGraph> allgs = new ArrayList<DetGraph>();
	int[][][] thetas;
	int[][] depPriors;
	final float alpha = 0.01f;
	
	void initvoc() {
		voc.nFormsMin=999999999;
		Ester2 m = new Ester2();
		for (;;) {
			List<DetGraph> gs = m.getNextFileParsed();
			if (gs==null) break;
			allgs.addAll(gs);
		}
		System.out.println("intializing voc... "+allgs.size());
		voc.init(allgs);
		System.out.println("voc init done !");
	}
	
	void allocateThetas() {
		long ram=(long)voc.getVocSize()*(long)voc.getVocSize()*(long)Dep.depnoms.length/(long)(1024*1024);
		System.out.println("estimation of memory required "+ram+" Mb");
		thetas = new int[Dep.depnoms.length][voc.getVocSize()][voc.getVocSize()+1];
		depPriors = new int[voc.getVocSize()][Dep.depnoms.length+1];
	}
	
	public LexPref() {
		initvoc();
		allocateThetas();
		initThetas();
	}
	
	void increaseThetas(DetGraph g) {
		for (int i=0;i<g.getNbMots();i++) {
			Mot gov = g.getMot(i);
			int d=g.getDep(i);
			// we don't consider for now at all the roots...
			if (d>=0) {
				Mot head=g.deps.get(d).head;
				int vgov = voc.getWordIndex(gov);
				int vhead = voc.getWordIndex(head);
				int deplab = g.deps.get(d).type;
				thetas[deplab][vhead][vgov]++;
				thetas[deplab][vhead][thetas[deplab][vhead].length-1]++;
				depPriors[vhead][deplab]++;
				depPriors[vhead][depPriors[vhead].length-1]++;
			}
		}
	}
	void decreaseThetas(DetGraph g) {
		for (int i=0;i<g.getNbMots();i++) {
			Mot gov = g.getMot(i);
			int d=g.getDep(i);
			// we don't consider for now at all the roots...
			if (d>=0) {
				Mot head=g.deps.get(d).head;
				int vgov = voc.getWordIndex(gov);
				int vhead = voc.getWordIndex(head);
				int deplab = g.deps.get(d).type;
				thetas[deplab][vhead][vgov]--;
				thetas[deplab][vhead][thetas[deplab][vhead].length-1]--;
				depPriors[vhead][deplab]--;
				depPriors[vhead][depPriors[vhead].length-1]--;
			}
		}
	}
	
	float getScore(DetGraph g, int i) {
		Mot gov = g.getMot(i);
		int d=g.getDep(i);
		// we don't consider for now at all the roots...
		if (d>=0) {
			Mot head=g.deps.get(d).head;
			int vgov = voc.getWordIndex(gov);
			int vhead = voc.getWordIndex(head);
			int deplab = g.deps.get(d).type;
			// we can have a null denom here !
			// so we use Dirichlet
			float num = (float)thetas[deplab][vhead][vgov] + alpha; 
			float denom = (float)thetas[deplab][vhead][thetas[deplab][vhead].length-1] + alpha*(float)voc.getVocSize();
			num/=denom;
			num *= (float)depPriors[vhead][deplab]/(float)depPriors[vhead][depPriors[vhead].length-1];
			return num;
		} else return -1;
	}
	
	float calcLogPost() {
		float logpost=0;
		for (int gi=0;gi<allgs.size();gi++) {
			DetGraph g = allgs.get(gi);
			for (int i=0;i<g.getNbMots();i++) {
				Mot gov = g.getMot(i);
				int d=g.getDep(i);
				// we don't consider for now at all the roots...
				if (d>=0) {
					Mot head=g.deps.get(d).head;
					int vgov = voc.getWordIndex(gov);
					int vhead = voc.getWordIndex(head);
					int deplab = g.deps.get(d).type;
					// we cannot have a null denominator !
					float localpost = (float)thetas[deplab][vhead][vgov]/(float)thetas[deplab][vhead][thetas[deplab][vhead].length-1];
					localpost*=(float)depPriors[vhead][deplab]/(float)depPriors[vhead][depPriors[vhead].length-1];
					logpost+=Math.log(localpost);
				}
			}
		}
		return logpost;
	}
	
	private void insertBadScore(float sc, int z) {
		for (int i=badidx-1;i>=0;i--) {
			if (sc>=badScores[i]) {
				++i;
				if (badidx<nbads) badidx++;
				for (int j=badidx-1;j>i;j--) {
					badScores[j]=badScores[j-1];
					badWord[j]=badWord[j-1];
				}
				badScores[i]=sc; badWord[i]=z;
				return;
			}
		}
		if (badidx<nbads) badidx++;
		for (int j=badidx-1;j>0;j--) {
			badScores[j]=badScores[j-1];
			badWord[j]=badWord[j-1];
		}
		badScores[0]=sc; badWord[0]=z;
	}
	
	
	final int nbads=10;
	int[] badWord = new int[nbads];
	float[] badScores = new float[nbads];
	int badidx=0;
	void sampleAction(DetGraph g) {
		badidx=0;
		for (int i=0;i<g.getNbMots();i++) {
			float sc = getScore(g,i);
			if (sc<0||sc>0.1) continue;
			if (badidx<badScores.length || sc<badScores[nbads-1]) {
				insertBadScore(sc,i);
			}
		}
		if (Debug.debug && badWord.length>0) {
			{
				int dt=Dep.getType("COMP");
				Mot m = new Mot("DET:ART", "DET:ART", "DET:ART");
				int h=voc.getWordIndex(m);
				for (int i=0;i<thetas[dt][h].length-1;i++) {
					System.out.println("DET-NOM: "+voc.getWord4Index(i)+" : "+thetas[dt][h][i]);
				}
				System.out.println("total nocc "+thetas[dt][h][thetas[dt][h].length-1]);
			}
			System.out.println("badwords "+Arrays.toString(badWord));
			System.out.println("badscores "+Arrays.toString(badScores));
			JSafran.viewGraph(g);
		}
		
		// can we change the head and label of bad words to make them better ?
		ArrayList<Integer> gov = new ArrayList<Integer>();
		ArrayList<Integer> newhead = new ArrayList<Integer>();
		ArrayList<Integer> newdeplab = new ArrayList<Integer>();
		ArrayList<Float> scoreGain = new ArrayList<Float>();
		for (int i=0;i<badidx;i++) {
			int curgov = badWord[i];
			int d=g.getDep(curgov);
			if (d<0) {
				// we do not add a new dep, maybe we should, but I'm afrait it'll introduce lots of noise... ?
				continue;
			}
			Dep curdep = g.deps.get(d);
			int savdeptyp = curdep.type;
			Mot savdephead = curdep.head;
			for (int k=0;k<Dep.depnoms.length;k++) {
				for (int j=curgov-1;j>=0;j--) {
					curdep.type=k;
					curdep.head=g.getMot(j);
					// we should check at least topological constraints,
					// better, we should reparse in force mode and compute the full sentence score
					float sc = getScore(g,curgov);
					// may be we should only consider big gains
					if (sc>badScores[i]) {
						gov.add(curgov); newhead.add(j); newdeplab.add(k); scoreGain.add(sc-badScores[i]);
					}
				}
				// may be we should limit the length of deps we add ?
				for (int j=curgov+1;j<g.getNbMots();j++) {
					curdep.type=k;
					curdep.head=g.getMot(j);
					// we should check at least topological constraints,
					// better, we should reparse in force mode and compute the full sentence score
					float sc = getScore(g,curgov);
					if (sc>badScores[i]) {
						gov.add(curgov); newhead.add(j); newdeplab.add(k); scoreGain.add(sc-badScores[i]);
					}
				}
			}
			// restore the dep
			curdep.type=savdeptyp;
			curdep.head=savdephead;
		}
		if (gov.size()>0) {
			// ok we can now sample one of them from the score distrib !
			float[] scdist = new float[scoreGain.size()];
			// the original dep must be part of these options
			int bestz=0; float bestzsc=scoreGain.get(0);
			for (int i=0;i<scdist.length;i++) {
				scdist[i]=scoreGain.get(i);
				if (scdist[i]>bestzsc) {bestzsc=scdist[i]; bestz=i;}
			}
			
			// int r = sample_Mult(scdist);
			int r=bestz;
			
			// we now apply it
			int d=g.getDep(gov.get(r));
			Dep dd = g.deps.get(d);

			System.out.println("debug modified "+(dd.gov.getIndexInUtt()-1)+" <- "+(dd.head.getIndexInUtt()-1)+" "+Dep.depnoms[dd.type]+
					"gain "+scdist[r]);

			dd.head=g.getMot(newhead.get(r));
			dd.type=newdeplab.get(r);
			
			JSafran.viewGraph(g);

		}
	}
	
	Random rand = new Random();
	int sample_Mult(float[] th) {
		float s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextFloat();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}
	
	void initThetas() {
		for (DetGraph g : allgs) increaseThetas(g);
	}
	
	void runInference() {
		for (int iter=0;iter<niters;iter++) {
			for (int gi=0;gi<allgs.size();gi++) {
				DetGraph g = allgs.get(gi);
				decreaseThetas(g);
				sampleAction(g);
				increaseThetas(g);
			}
			// this may be done inside the gi loop...
			float logPost = calcLogPost();
			System.out.println("iter "+iter+" logpost "+logPost);
		}
	}
	
	public static void main(String args[]) {
		LexPref m = new LexPref();
		m.runInference();
	}
}
