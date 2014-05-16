package jsafran.parsing.unsup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * represents a syntactic frame, or another more general unsupervised criterion.
 * It keeps counts of occurrences of a given variable and applies multinomial-dirichlet
 * 
 * TODO: make an interface to facilitate the definition and multiplication of different frames
 * 
 * new frame model: uses all deps, but only 1-gram and 2-grams
 * 
 * dep vide = Ndeps
 * fridx = depA * (Ndeps+1) + depB
 * 
 * @author xtof
 *
 */
public class Frame {
	final String[] depsOfFrames = {"AUX","DET","OBJ","POBJ","SUJ"};
//	final String[] depsOfFrames = {"OBJ","SUJ"};
	int nFramesDiff = -1;

	final double alpha = 0.001;
	HashMap<String, int[]> pos2frameCounts = new HashMap<String, int[]>();
	HashSet<String> forms2keep = new HashSet<String>();
	HashMap<String, Integer> alldeps = new HashMap<String, Integer>();

	public Frame(List<DetGraph> gs) {
		final int noccsmin = 100;
		alldeps.put("EMPTY",0);
		// uses FORMS for the most frequent words
		HashMap<String, Integer> voc =new HashMap<String, Integer>();
		for (DetGraph g : gs) {
			for (Dep d : g.deps) {
				String lab =Dep.depnoms[d.type];
				if (!alldeps.containsKey(lab)) {
					int i=alldeps.size();
					alldeps.put(lab,i);
				}
			}
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
		nFramesDiff = alldeps.size()*alldeps.size();
		System.out.println("keeping nforms= "+forms2keep.size());
		System.out.println("nFrames = "+alldeps.size()+" "+nFramesDiff);
	}
	// returns the "key" on which the frame is conditioned
	public String getKey(DetGraph g, int w) {
		String form = g.getMot(w).getForme();
		if (forms2keep.contains(form)) return form;
		return g.getMot(w).getPOS();
	}
	public void decreaseFrameCounts(DetGraph g) {
		for (int w=0;w<g.getNbMots();w++) {
			int[] frsidx = getFrameIndex(g, w);
			String key=getKey(g, w);
			int[] frcounts = pos2frameCounts.get(key);
			for (int fridx : frsidx) {
				frcounts[fridx]--;
				if (frcounts[fridx]<0) {
					System.out.println("ERROR fr counts "+frcounts[fridx]);
				}
			}
		}
	}
	public void increaseFrameCounts(DetGraph g) {
		for (int w=0;w<g.getNbMots();w++) {
			int[] frsidx = getFrameIndex(g, w);
			String key=getKey(g, w);
			int[] frcounts = pos2frameCounts.get(key);
			for (int fridx : frsidx) {
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
	}
	private int[] getFrameIndex(DetGraph g, int w) {
		List<Integer> fils = g.getFils(w);
		int[] res = new int[fils.size()+1];
		
		final int[] co = new int[nFramesDiff];
		Arrays.fill(co, 0);
		
		for (int i=0;i<fils.size()-1;i++) {
			int d = g.getDep(fils.get(i));
			String lab = g.getDepLabel(d);
			int dA = alldeps.get(lab);
			d = g.getDep(fils.get(i+1));
			lab = g.getDepLabel(d);
			int dB = alldeps.get(lab);
			res[i]=dA*alldeps.size()+dB;
		}
		{
			int dA = alldeps.get("EMPTY");
			if (fils.size()>0) {
				int d = g.getDep(fils.get(fils.size()-1));
				String lab = g.getDepLabel(d);
				dA = alldeps.get(lab);
			}
			res[res.length-1]=dA*alldeps.size()+alldeps.get("EMPTY");
		}
		return res;
	}
	public double getLogDirMult(DetGraph g, int w) {
		// recupere l'ID de la frame du mot i
		int[] frsidx = getFrameIndex(g,w);
		// calcule le posterior smoothed (avec les counts)
		String key=getKey(g, w);

		double anneal = 1;
//		if (anneal>1) anneal=1;
		int[] frcounts = pos2frameCounts.get(key);
		if (frcounts==null) {
			double num = Math.log(alpha);
			double sum = nFramesDiff*alpha;
			sum = Math.log(sum);
			num-=sum;
			return num;
		}
		double avg=0;
		for (int fridx : frsidx) {
			double num = Math.log(alpha+anneal*frcounts[fridx]);
			double sum = 0;
			for (int co : frcounts) sum+=alpha+anneal*co;
			sum = Math.log(sum);
			num-=sum;
			avg += num;
		}
		avg /= (double)frsidx.length;
		return avg;
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
