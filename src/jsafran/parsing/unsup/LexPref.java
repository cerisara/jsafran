package jsafran.parsing.unsup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

/**
 * represents a syntactic frame, or another more general unsupervised criterion.
 * It keeps counts of occurrences of a given variable and applies multinomial-dirichlet
 * 
 * @author xtof
 *
 */
public class LexPref {

	final double alpha = 0.001;
	HashMap<String, int[]> dep2frameCounts = new HashMap<String, int[]>();
	ArrayList<String> forms2keep = new ArrayList<String>();
	ArrayList<String> postag2keep = new ArrayList<String>();
	int nFramesDiff = 0;

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
	public double getLogDirMult(DetGraph g, int w) {
		// recupere l'ID de la frame du mot i
		int fridx = getFrameIndex(g,w);
		// calcule le posterior smoothed (avec les counts)
		String key=getKey(g, w);

		double anneal = 1;
		//		if (anneal>1) anneal=1;
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
		//		System.out.println("dbug logpost "+fridx+" "+num+" "+sum);
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

	public static void debug(String countsfile, String xmllist) {
		GraphIO gio = new GraphIO(null);
		ArrayList<DetGraph> allGraphs = new ArrayList<DetGraph>();
		int testpos = 0;
		String testFile;
		try {
			BufferedReader f = new BufferedReader(new FileReader(xmllist));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				// le dernier fichier est tjs le fichier de test
				testFile=s;
				testpos=allGraphs.size();
				List<DetGraph> gs = gio.loadAllGraphs(s);
				allGraphs.addAll(gs);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		LexPref m = new LexPref(allGraphs);
		System.out.println("forms to keep: "+m.forms2keep);
		
		m.loadCounts(countsfile);
		String key = "SBJ";
			
		for (int depidx=0;depidx<m.forms2keep.size()+m.postag2keep.size();depidx++) {
			for (int headidx=0;headidx<m.forms2keep.size()+m.postag2keep.size();headidx++) {
				int fridx = headidx*(m.forms2keep.size()+m.postag2keep.size())+depidx;
			
				double anneal = 1;
				int[] frcounts = m.dep2frameCounts.get(key);
				double num;
				if (frcounts==null) {
					num = Math.log(m.alpha);
					double sum = m.nFramesDiff*m.alpha;
					sum = Math.log(sum);
					num-=sum;
				} else {
					num = Math.log(m.alpha+anneal*frcounts[fridx]);
					double sum = 0;
					for (int co : frcounts) sum+=m.alpha+anneal*co;
					sum = Math.log(sum);
					num-=sum;
				}
				
				String w,h;
				if (depidx<m.forms2keep.size()) w=m.forms2keep.get(depidx);
				else w=m.postag2keep.get(depidx-m.forms2keep.size());
				if (headidx<m.forms2keep.size()) h=m.forms2keep.get(headidx);
				else h=m.postag2keep.get(headidx-m.forms2keep.size());
				
				System.out.println(w+"\t"+h+"\t"+num);
			}
		}
	}

	public static void main(String[] args) {
		debug("lexcounts.init","all.xmll");
	}
}
