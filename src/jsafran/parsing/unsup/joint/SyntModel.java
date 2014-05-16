package jsafran.parsing.unsup.joint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.parsing.unsup.joint.RulesDepthFirst.Model;

/**
 * This model comes from the newmemm/SparseRules.
 * It was the model that gave the best results on the WSJ10
 * 
 * @author xtof
 *
 */
public class SyntModel implements Model {
	Frame syntFrames;
	LexPref lexpref;

	public SyntModel(List<DetGraph> gs) {
		syntFrames = new Frame(gs);
		lexpref = new LexPref(gs);
	}
	
	@Override
	public float getUnnormalizedPosterior(DetGraph g) {
		double p=0;
		// pour chaque mot, détermine sa Frame en fonction de ses fils
		for (int i=0;i<g.getNbMots();i++) {
			double postFrame = syntFrames.getLogDirMult(g,i);
			double postLex = lexpref.getLogDirMult(g, i);
			// penalite pour non-proj
			p+=projectivePenalty(g);
			p+=postFrame;
			p+=postLex;
		}
		return (float)p;
	}

	@Override
	public void updateCounts(boolean inc, DetGraph g) {
		if (inc) {
			syntFrames.increaseFrameCounts(g);
			lexpref.increaseFrameCounts(g);
		} else {
			syntFrames.decreaseFrameCounts(g);
			lexpref.decreaseFrameCounts(g);
		}
	}

	@Override
	public float getJoint(DetGraph g) {
		return (float)(syntFrames.getJoint(g)+lexpref.getJoint(g));
	}

	@Override
	public void save(String nom) {
		syntFrames.saveCounts(nom+"frcounts.");
		lexpref.saveCounts(nom+"lexcounts.");
	}

	@Override
	public int getTot() {
		return syntFrames.getTot()+lexpref.getTot();
	}

	double projectivePenalty(DetGraph g) {
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
						}
					}
				} else {
					for (int j=i-1;j>h;j--) {
						d=g.getDep(j);
						if (d>=0) {
							int hh=g.getHead(d);
							if (hh>i||hh<h) ncrosscut++;
						}
					}
				}
			}
		}
		return -(double)ncrosscut*100.0;
	}

	class Frame {
		int nFramesDiff = -1;
		int[] multiplier = null;

		final int ndepsmax = 4;
		final double alpha = 0.001;
		HashMap<String, int[]> pos2frameCounts = new HashMap<String, int[]>();
		HashSet<String> forms2keep = new HashSet<String>();

		public int getTot() {
			int t=0;
			for (int[] vals : pos2frameCounts.values()) {
				for (int v: vals) t+=v;
			}
			return t;
		}
		
		// WARNING: these must be sorted alphabetically !
		final String[] depsKeptInFrames = {"OBJ","SBJ"};
		
		public Frame(List<DetGraph> gs) {
			nFramesDiff = (int)Math.pow(ndepsmax+1, depsKeptInFrames.length);
			multiplier = new int[depsKeptInFrames.length];
			multiplier[0]=1;
			for (int i=1;i<multiplier.length;i++) multiplier[i]=multiplier[i-1]*(ndepsmax+1);
			
			final int noccsmin = 100;
			// uses FORMS for the most frequent words
			HashMap<String, Integer> voc =new HashMap<String, Integer>();
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					String w = g.getMot(i).getForme();
					Integer n = voc.get(w);
					if (n==null) n=0;
					n++;
					voc.put(w, n);
				}
			}
			for (String w : voc.keySet()) {
				if (voc.get(w)>=noccsmin) forms2keep.add(w);
			}
			System.out.println("keeping nforms= "+forms2keep.size());
			System.out.println("nFrames = "+nFramesDiff);
		}
		// returns the "key" on which the frame is conditioned
		public String getKey(DetGraph g, int w) {
			String form = g.getMot(w).getForme();
			if (forms2keep.contains(form)) return form;
			return g.getMot(w).getPOS();
		}
		public void decreaseFrameCounts(DetGraph g) {
			for (int w=0;w<g.getNbMots();w++) {
				int fridx = getFrameIndex(g, w);
				String key=getKey(g, w);
				int[] frcounts = pos2frameCounts.get(key);
				frcounts[fridx]--;
				if (frcounts[fridx]<0) {
					System.out.println("ERROR fr counts "+frcounts[fridx]);
				}
			}
		}
		public void increaseFrameCounts(DetGraph g) {
			for (int w=0;w<g.getNbMots();w++) {
				int fridx = getFrameIndex(g, w);
				String key=getKey(g, w);
				int[] frcounts = pos2frameCounts.get(key);
				if (frcounts==null) {
					frcounts = new int[nFramesDiff];
					pos2frameCounts.put(key, frcounts);
				}
				if (fridx<0||fridx>=frcounts.length) {
					System.out.println("ERROR frcounts "+key+" "+w+" "+fridx+" "+nFramesDiff);
					JSafran.viewGraph(g,w);
				}
				frcounts[fridx]++;
			}
		}
		public int getFrameIndex(DetGraph g, int w) {
			List<Integer> fils = g.getFils(w);
			
			final int[] co = new int[depsKeptInFrames.length];
			Arrays.fill(co, 0);
			
			for (int f : fils) {
				int d = g.getDep(f);
				String lab = g.getDepLabel(d);
				int i = Arrays.binarySearch(depsKeptInFrames, lab);
				if (i>=0) co[i]++;
			}
			int fridx = 0;
			for (int i=0;i<co.length;i++) {
				if (co[i]>ndepsmax) co[i]=ndepsmax;
				fridx+=co[i]*multiplier[i];
			}
			return fridx;
		}
		final double MINILOGPROB = -1000;
		// don't smooth !
		public double getJoint(DetGraph g) {
			double j=0;
			for (int h=0;h<g.getNbMots();h++) {
				int fridx = getFrameIndex(g,h);
				String key=getKey(g, h);
				int[] frcounts = pos2frameCounts.get(key);
				if (frcounts==null) j+=MINILOGPROB;
				else {
					double num = Math.log(frcounts[fridx]);
					double sum = 0;
					for (int co : frcounts) sum+=co;
					sum = Math.log(sum);
					num-=sum;
					j+=num;
				}
			}
			return j;
		}
		public double getLogDirMult(DetGraph g, int h) {
			// recupere l'ID de la frame du mot i
			int fridx = getFrameIndex(g,h);
			// calcule le posterior smoothed (avec les counts)
			String key=getKey(g, h);

			double anneal = 1;
//			if (anneal>1) anneal=1;
			int[] frcounts = pos2frameCounts.get(key);
			if (frcounts==null) {
				double num = Math.log(alpha);
				double sum = nFramesDiff*alpha;
				sum = Math.log(sum);
				num-=sum;
				return num;
			}
			double num = Math.log(alpha+anneal*frcounts[fridx]);
			double sum = 0;
			// le modele est P(Fr|H), donc on somme le denom sur les Fr = somme(n_{h,*})
			// ceci est generatif SI les frames sont observees, discriminant SI H est observe
			for (int co : frcounts) sum+=alpha+anneal*co;
			sum = Math.log(sum);
//			System.out.println("dbug logpost "+fridx+" "+num+" "+sum);
			num-=sum;
			return num;
		}
		public void saveCounts(String countsfile) {
			try {
				PrintWriter f = new PrintWriter(new FileWriter(countsfile));
				for (String pos : pos2frameCounts.keySet()) {
					f.println(pos+" "+Arrays.toString(pos2frameCounts.get(pos)));
				}
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		public void loadCounts(String countsfile) {
			System.out.println("nframesdiff "+nFramesDiff);
			pos2frameCounts.clear();
			try {
				BufferedReader f = new BufferedReader(new FileReader(countsfile));
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					int i=s.indexOf(' ');
					String pos = s.substring(0,i);
					StringTokenizer st = new StringTokenizer(s.substring(i+1), " [],");
					int[] co = new int[nFramesDiff];
					pos2frameCounts.put(pos, co);
					for (i=0;i<nFramesDiff;i++) {
						String x = null;
						for (;;) {
							x = st.nextToken();
							if (x.length()>0) break;
						}
						co[i]=Integer.parseInt(x);
					}
				}
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class LexPref {
		final double alpha = 0.001;
		HashMap<String, int[]> dep2frameCounts = new HashMap<String, int[]>();
		ArrayList<String> forms2keep = new ArrayList<String>();
		ArrayList<String> postag2keep = new ArrayList<String>();
		int nFramesDiff = 0;

		public int getTot() {
			int t=0;
			for (int[] vals : dep2frameCounts.values()) {
				for (int v: vals) t+=v;
			}
			return t;
		}

		public LexPref(List<DetGraph> gs) {
			{
				final int nFormsMin = 100;
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
				final int nPOSMin = 50;
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
			nFramesDiff=(forms2keep.size()+postag2keep.size())*(forms2keep.size()+postag2keep.size());
			System.out.println("nFrames = "+nFramesDiff);
		}
		// returns the "key" on which the frame is conditioned
		public String getKey(DetGraph g, int w) {
			int d = g.getDep(w);
			if (d<0) return "ROOT";
			return g.getDepLabel(d);
		}
		public void decreaseFrameCounts(DetGraph g) {
			for (int w=0;w<g.getNbMots();w++) {
				int fridx = getFrameIndex(g, w);
				String key=getKey(g, w);
				int[] frcounts = dep2frameCounts.get(key);
				frcounts[fridx]--;
				if (frcounts[fridx]<0) {
					System.out.println("ERROR fr counts "+frcounts[fridx]);
				}
			}
		}
		public void increaseFrameCounts(DetGraph g) {
			for (int w=0;w<g.getNbMots();w++) {
				int fridx = getFrameIndex(g, w);
				String key=getKey(g, w);
				int[] frcounts = dep2frameCounts.get(key);
				if (frcounts==null) {
					frcounts = new int[nFramesDiff];
					dep2frameCounts.put(key, frcounts);
				}
				if (fridx<0||fridx>=frcounts.length) {
					System.out.println("ERROR frcounts "+key+" "+w+" "+fridx+" "+nFramesDiff);
					JSafran.viewGraph(g,w);
				}
				frcounts[fridx]++;
			}
		}
		public int getFrameIndex(DetGraph g, int w) {
			int headidx;
			int d = g.getDep(w);
			if (d>=0) {
				int h = g.getHead(d);
				int i=Collections.binarySearch(forms2keep, g.getMot(h).getForme());
				if (i>=0) headidx = i;
				else {
					i=Collections.binarySearch(postag2keep, g.getMot(h).getPOS());
					if (i>=0) headidx=forms2keep.size()+i;
					else headidx=forms2keep.size()+Collections.binarySearch(postag2keep, "UNK");
				}
			} else {
				headidx = forms2keep.size()+Collections.binarySearch(postag2keep, "ROOT");
			}
			headidx*=(forms2keep.size()+postag2keep.size());
			
			int depidx;
			int i=Collections.binarySearch(forms2keep, g.getMot(w).getForme());
			if (i>=0) depidx = i;
			else {
				i=Collections.binarySearch(postag2keep, g.getMot(w).getPOS());
				if (i>=0) depidx=forms2keep.size()+i;
				else depidx=forms2keep.size()+Collections.binarySearch(postag2keep, "UNK");
			}
			
			headidx+=depidx;
			return headidx;
		}
		final double MINILOGPROB = -1000;
		// don't smooth !
		public double getJoint(DetGraph g) {
			double j=0;
			for (int h=0;h<g.getNbMots();h++) {
				int fridx = getFrameIndex(g,h);
				String key=getKey(g, h);
				int[] frcounts = dep2frameCounts.get(key);
				if (frcounts==null) j+=MINILOGPROB;
				else {
					double num = Math.log(frcounts[fridx]);
					double sum = 0;
					for (int co : frcounts) sum+=co;
					sum = Math.log(sum);
					num-=sum;
					j+=num;
				}
			}
			return j;
		}
		public double getLogDirMult(DetGraph g, int w) {
			// recupere l'ID de la frame du mot i
			int fridx = getFrameIndex(g,w);
			// calcule le posterior smoothed (avec les counts)
			String key=getKey(g, w);

			double anneal = 1;
//			if (anneal>1) anneal=1;
			int[] frcounts = dep2frameCounts.get(key);
			if (frcounts==null) {
				double num = Math.log(alpha);
				double sum = nFramesDiff*alpha;
				sum = Math.log(sum);
				num-=sum;
				return num;
			}
			double num = Math.log(alpha+anneal*frcounts[fridx]);
			double sum = 0;
			for (int co : frcounts) sum+=alpha+anneal*co;
			sum = Math.log(sum);
//			System.out.println("dbug logpost "+fridx+" "+num+" "+sum);
			num-=sum;
			return num;
		}
		public void saveCounts(String countsfile) {
			try {
				PrintWriter f = new PrintWriter(new FileWriter(countsfile));
				for (String pos : dep2frameCounts.keySet()) {
					f.println(pos+" "+Arrays.toString(dep2frameCounts.get(pos)));
				}
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		public void loadCounts(String countsfile) {
			System.out.println("nframesdiff "+nFramesDiff);
			dep2frameCounts.clear();
			try {
				BufferedReader f = new BufferedReader(new FileReader(countsfile));
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					int i=s.indexOf(' ');
					String pos = s.substring(0,i);
					StringTokenizer st = new StringTokenizer(s.substring(i+1), " [],");
					int[] co = new int[nFramesDiff];
					dep2frameCounts.put(pos, co);
					for (i=0;i<nFramesDiff;i++) {
						String x = null;
						for (;;) {
							x = st.nextToken();
							if (x.length()>0) break;
						}
						co[i]=Integer.parseInt(x);
					}
				}
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
