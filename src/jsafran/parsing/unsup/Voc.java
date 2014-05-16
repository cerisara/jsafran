package jsafran.parsing.unsup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jsafran.DetGraph;
import jsafran.Mot;

public class Voc {
	public ArrayList<String> forms2keep = new ArrayList<String>();
	public ArrayList<String> postag2keep = new ArrayList<String>();

	public int nFormsMin=5, nPOSMin=1;
	
	int[] wordPriors;
	
	public int getVocSize() {
		return forms2keep.size()+postag2keep.size();
	}
	
	public void init(List<DetGraph> gs) {
		System.out.println("create VOC "+gs.size());
		{
			forms2keep.clear();
			postag2keep.clear();
			// uses FORMS for the most frequent words
			HashMap<String, Integer> vocForms =new HashMap<String, Integer>();
			for (DetGraph g : gs) {
				for (int i=0;i<g.getNbMots();i++) {
					String w = g.getMot(i).getForme().toLowerCase();
//					String w = g.getMot(i).getLemme();
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
		
		{
			// priors computation
			wordPriors = new int[getVocSize()];
			Arrays.fill(wordPriors, 1); // smoothing
			int nu=0;
			for (DetGraph g : gs) {
				nu++;
				for (int i=0;i<g.getNbMots();i++) {
					int w = getWordIndex(g.getMot(i));
					wordPriors[w]++;
				}
			}
			int w=getWordIndex(null);
			wordPriors[w]+=nu;
		}
	}
	
	public int getUnnormalizedPrior(int w) {
		return wordPriors[w];
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
	
	public static Voc load(DataInputStream f) {
		Voc m = new Voc();
		try {
			int n =f.readInt();
			for (int i=0;i<n;i++) m.forms2keep.add(f.readUTF());
			n =f.readInt();
			for (int i=0;i<n;i++) m.postag2keep.add(f.readUTF());
			n =f.readInt();
			m.wordPriors = new int[n];
			for (int i=0;i<n;i++) m.wordPriors[i]=f.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return m;
	}
	public void save(DataOutputStream f) {
		try {
			f.writeInt(forms2keep.size());
			for (int i=0;i<forms2keep.size();i++) f.writeUTF(forms2keep.get(i));
			f.writeInt(postag2keep.size());
			for (int i=0;i<postag2keep.size();i++) f.writeUTF(postag2keep.get(i));
			f.writeInt(wordPriors.length);
			for (int i=0;i<wordPriors.length;i++) f.writeInt(wordPriors[i]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
