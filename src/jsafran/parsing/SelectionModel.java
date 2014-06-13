package jsafran.parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import utils.ErrorsReporting;

import jsafran.DetGraph;

/**
 * Model qui choisit, étant donné un gouverné identifié, un deplabel et un ensemble de candidats,
 * le meilleur candidat possible pour le head
 * 
 * @author xtof
 *
 */
public class SelectionModel {
	// nouvelle version avec Naive Bayes

	final float MINPROB = 0.0001f;

	// modelise P(a|b) où a doit etre une observation; on a donc un modele generatif
	class Bigram {
		HashMap<Integer, HashMap<Integer, Integer>> ab2co = new HashMap<Integer, HashMap<Integer,Integer>>();
		public void accumulate(int var, int obs) {
			// accumulate the counts #(a,b)
			HashMap<Integer, Integer> l = ab2co.get(var);
			if (l==null) {
				l = new HashMap<Integer, Integer>();
				ab2co.put(var, l);
			}
			Integer co = l.get(obs);
			if (co==null) {
				co=1;
			} else co++;
			l.put(obs, co);
		}
		public void finalizeTraining() {
		}
		public float getLike(int var, int obs) {
			HashMap<Integer, Integer> varFixed = ab2co.get(var);
			if (varFixed==null) return MINPROB;
			Integer coab = varFixed.get(obs);
			if (coab==null) {
				return MINPROB;
			}
			float coax=0;
			for (int c : varFixed.values()) coax+=c;
			float p = (float)coab/(float)coax;
			if (p<MINPROB) p=MINPROB;
			return p;
		}
		void save(PrintWriter f) {
			f.println(ab2co.size());
			for (int a : ab2co.keySet()) {
				HashMap<Integer, Integer> l = ab2co.get(a);
				f.println(l.size());
				for (int b : l.keySet()) {
					f.println(a+" "+b+" "+l.get(b));
				}
			}
		}
		public void load(BufferedReader f)  {
			ab2co.clear();
			try {
				String s=f.readLine();
				int n=Integer.parseInt(s);
				for (int i=0;i<n;i++) {
					s = f.readLine();
					int nn = Integer.parseInt(s);
					for (int j=0;j<nn;j++) {
						s = f.readLine();
						String[] ss =s.split(" ");
						int a = Integer.parseInt(ss[0]);
						int b = Integer.parseInt(ss[1]);
						int co = Integer.parseInt(ss[2]);
						HashMap<Integer, Integer> l = ab2co.get(a);
						if (l==null) {
							l = new HashMap<Integer, Integer>();
							ab2co.put(a, l);
						}
						l.put(b, co);
					}
				}
			} catch (Exception e) {
				ErrorsReporting.report(e);
			}
		}
	}
	class Unigram {
		HashMap<Integer, Integer> a2co = new HashMap<Integer, Integer>();
		public void accumulate(int var) {
			Integer co = a2co.get(var);
			if (co==null) {
				co=1;
			} else co++;
			a2co.put(var, co);
		}
		public void finalizeTraining() {
		}
		public float getLike(int var) {
			Integer varFixed = a2co.get(var);
			if (varFixed==null) return MINPROB;
			float coax=0;
			for (int c : a2co.values()) coax+=c;
			float p = (float)varFixed/(float)coax;
			if (p<MINPROB) p=MINPROB;
			return p;
		}
		void save(PrintWriter f) {
			f.println(a2co.size());
			for (int a : a2co.keySet()) {
				int co = a2co.get(a);
				f.println(a+" "+co);
			}
		}
		public void load(BufferedReader f)  {
			a2co.clear();
			try {
				String s=f.readLine();
				int n=Integer.parseInt(s);
				for (int i=0;i<n;i++) {
					s = f.readLine();
					String[] ss =s.split(" ");
					int a = Integer.parseInt(ss[0]);
					int co = Integer.parseInt(ss[1]);
					a2co.put(a, co);
				}
			} catch (Exception e) {
				ErrorsReporting.report(e);
			}
		}
	}
	class BoolUnigram {
		int[] co={0,0};
		public void accumulate(boolean a) {
			if (a) co[0]++;
			else co[1]++;
		}
		public void finalizeTraining() {
		}
		public float getLike(boolean b) {
			if (b) return (float)co[0]/(float)(co[0]+co[1]);
			else return (float)co[1]/(float)(co[0]+co[1]);
		}
		public void save(PrintWriter f) {
			f.println(co[0]+" "+co[1]);
		}
		public void load(BufferedReader f)  {
			try {
				String s=f.readLine();
				String[] ss =s.split(" ");
				co[0] = Integer.parseInt(ss[0]);
				co[1] = Integer.parseInt(ss[1]);
			} catch (Exception e) {
				ErrorsReporting.report(e);
			}
		}
	}

	Bigram fgov_dep = new Bigram(), pgov_dep = new Bigram(), fhead_dep=new Bigram();
	Bigram phead_dep = new Bigram();
	Bigram fgov_fhead = new Bigram(), pgov_phead = new Bigram();
	BoolUnigram headIsRoot = new BoolUnigram();
	Unigram fhead = new Unigram();
	Unigram phead = new Unigram();
	
	HashMap<String, Integer> voc = new HashMap<String, Integer>();
	private int getVocIdx(String w) {
		Integer i = voc.get(w);
		if (i==null) {
			i=voc.size(); voc.put(w, i);
		}
		return i;
	}

	public void train(MEMMparser m, String[] acts) {
		if (acts==null) return;
		for (int i=0;i<m.getNnodes();i++) {
			if (MEMMparser.getActType(acts[i])==MEMMparser.acttype.NA) continue;
			String dep = MEMMparser.getActDep(acts[i]);
			int head = MEMMparser.getActHeadword(acts[i]);
			accumulate(m, i, head, dep);
		}
	}
	public void accumulate(MEMMparser m, int wnode, int head, String dep) {
		DetGraph g = m.graph;
		int gov = m.getNodeRoot(wnode);
		int deplab = getVocIdx(dep);
		int govForm = getVocIdx(g.getMot(gov).getForme());
		int hf = getVocIdx(g.getMot(head).getForme());
		int wp = getVocIdx(g.getMot(gov).getPOS());
		int hp = getVocIdx(g.getMot(head).getPOS());
		// le HEAD est la variable
		// lorsqu'il n'est pas considéré, c'est la forme ou le POS qui est la variable
		fgov_dep.accumulate(govForm,deplab);
		fhead_dep.accumulate(hf,deplab);
		fgov_fhead.accumulate(hf,govForm);
		pgov_dep.accumulate(wp,deplab);
		phead_dep.accumulate(hp, deplab);
		pgov_phead.accumulate(hp,wp);
		headIsRoot.accumulate((m.getNodeRoot(m.word2node[head])==head));
		fhead.accumulate(hf);
		phead.accumulate(hp);
	}
	public void finalizeTrain() {
		fgov_dep.finalizeTraining();
		fhead_dep.finalizeTraining();
		pgov_dep.finalizeTraining();
		phead_dep.finalizeTraining();
		fgov_fhead.finalizeTraining();
		pgov_phead.finalizeTraining();
		headIsRoot.finalizeTraining();
		fhead.finalizeTraining();
		phead.finalizeTraining();

		try {
			PrintWriter f = new PrintWriter(new FileWriter("headselmod.txt"));
			f.println(voc.size());
			for (String s : voc.keySet())
				f.println(s+" "+voc.get(s));
			fgov_dep.save(f);
			fhead_dep.save(f);
			pgov_dep.save(f);
			phead_dep.save(f);
			fgov_fhead.save(f);
			pgov_phead.save(f);
			headIsRoot.save(f);
			fhead.save(f);
			phead.save(f);
			f.close();
		} catch (Exception e) {
			ErrorsReporting.report(e);
		}
	}
	public void load() {
		try {
			BufferedReader f=new BufferedReader(new FileReader("headselmod.txt"));
			int v = Integer.parseInt(f.readLine());
			voc.clear();
			for (int i=0;i<v;i++) {
				String s = f.readLine();
				String[] ss = s.split(" ");
				voc.put(ss[0], Integer.parseInt(ss[1]));
			}
			fgov_dep.load(f);
			fhead_dep.load(f);
			pgov_dep.load(f);
			phead_dep.load(f);
			fgov_fhead.load(f);
			pgov_phead.load(f);
			headIsRoot.load(f);
			fhead.load(f);
			phead.load(f);
			f.close();
		} catch (Exception e) {
			ErrorsReporting.report(e);
		}
	}
	
	public int getBestHead(MEMMparser m, int wnode, int hnode, String dep) {
		int bestx=m.getNodeRoot(hnode);
//		if (true) return bestx;
		float bestp=-Float.MAX_VALUE;
		int w = m.getNodeRoot(wnode);
		for (int x : m.getWordsInNode(hnode)) {
			if (!m.checkProjectivity(m.graph, w, x)) continue;
			float p = getLike(m, wnode, x, dep);
			if (p>bestp) {
				bestp=p; bestx=x;
			}
		}
		return bestx;
	}
	public float getLike(MEMMparser m, int wnode, int head, String dep) {
		DetGraph g = m.graph;
		int gov = m.getNodeRoot(wnode);
		int d = getVocIdx(dep);
		int wf = getVocIdx(g.getMot(gov).getForme());
		int hf = getVocIdx(g.getMot(head).getForme());
		int wp = getVocIdx(g.getMot(gov).getPOS());
		int hp = getVocIdx(g.getMot(head).getPOS());

		float p = 1f;
//		p*=fgov_dep.getLike(wf,d);
//		p*=fhead_dep.getLike(hf, d);
//		p*=pgov_dep.getLike(wp, d);
//		p*=phead_dep.getLike(hp, d);
//		p*=fgov_fhead.getLike(hf,wf);// p*= fhead.getLike(hf);
		p*=pgov_phead.getLike(hp,wp);// p*= phead.getLike(hp);
		p*=headIsRoot.getLike((m.getNodeRoot(m.word2node[head])==head));
		return p;
	}

	// ====================================================
	// ancienne version pour NodesSeq

	String modfile;
	MaxEntOpenNLP mod=null;

	ArrayList<String[]> feats = new ArrayList<String[]>();
	ArrayList<String> labels = new ArrayList<String>();

	public SelectionModel() {
	}
	public SelectionModel(String modf) {
		modfile=modf;
		loadMaxEntMod();
	}

	public void loadMaxEntMod() {
		mod = new MaxEntOpenNLP();
		mod.loadModel(modfile);
		System.out.println("loaded "+modfile);
	}

	public static String getBroadPOS(String pos) {
		int i=pos.indexOf(':');
		if (i>=0) return pos.substring(0,i);
		return pos;
	}

	/**
	 * @param g
	 * @param gov
	 * @param head
	 * @param dep
	 * @return
	 */
	private String[] calcFeats(DetGraph g, int gov, int head, NodesSeq seq) {
		ArrayList<String> feats = new ArrayList<String>();

		// HEAD et GOV
		feats.add("GovF"+g.getMot(gov).getForme());
		feats.add("GovP"+g.getMot(gov).getPOS());
		feats.add("GovB"+getBroadPOS(g.getMot(gov).getPOS()));
		feats.add("HeadF"+g.getMot(head).getForme());
		feats.add("HeadP"+g.getMot(head).getPOS());
		feats.add("HeadB"+getBroadPOS(g.getMot(head).getPOS()));
		feats.add("GovP-HeadP"+g.getMot(gov).getPOS()+"-"+g.getMot(head).getPOS());
		feats.add("GovF-HeadF"+g.getMot(gov).getForme()+"-"+g.getMot(head).getForme());

		// DEPENDANCES DU HEAD
		int d=g.getDep(head);
		if (d>=0) {
			int hh = g.getHead(d);
			feats.add("HeadDep"+g.getDepLabel(d));
			feats.add("HeadHeadP"+g.getMot(hh).getPOS());
			feats.add("HeadHeadF"+g.getMot(hh).getForme());

			int dd=g.getDep(hh);
			if (dd>=0) {
				feats.add("HeadHeadDep"+g.getDepLabel(dd));
				int hhh = g.getHead(dd);
				feats.add("HeadHeadHeadF"+g.getMot(hhh).getForme());
			} else feats.add("NoHeadHeadDep");
		} else feats.add("NoHeadDep");
		{
			List<Integer> fs = g.getFils(head);
			if (fs.size()==0) feats.add("NoHeadFils");
			else {
				for (int f : fs) {
					d=g.getDep(f);
					feats.add("HeadFilsDep"+g.getDepLabel(d));
					feats.add("HeadFilsP"+g.getMot(f).getPOS());
					feats.add("HeadFilsF"+g.getMot(f).getForme());
					feats.add("HeadFilsDepF"+g.getDepLabel(d)+"-"+g.getMot(f).getForme());
				}
			}
		}

		if (head<gov) feats.add("LT");
		else feats.add("RT");
		return feats.toArray(new String[feats.size()]);
	}

	public float getProb(DetGraph g, int govWord, int headWord, String dep, NodesSeq seq) {
		String[] feats = calcFeats(g, govWord, headWord, seq);
		HashMap<String, Float> probs = mod.parse(feats);
		Float p = probs.get(dep);
		if (p==null) return 0;
		return p;
	}

	public void accumulate(NodesSeq seq, String[] acts) {
		for (int i=0;i<acts.length;i++) {
			if (acts[i].charAt(0)!='N') {
				int j=acts[i].indexOf("2w");
				int h = Integer.parseInt(acts[i].substring(j+2));
				int w = seq.getNodeRoot(i);
				String[] fts=calcFeats(seq.g, w, h, seq);
				feats.add(fts);
				labels.add(acts[i].substring(2,j));
			}
		}
	}

	public void finalizeTraining() {
		String[][] xfeats = feats.toArray(new String[feats.size()][]);
		String[] xlabs = labels.toArray(new String[labels.size()]);
		modfile = MaxEntOpenNLP.train(xfeats, xlabs, 100);
		System.out.println("saved modile "+modfile);
		loadMaxEntMod();
		xfeats=null; xlabs=null;
	}
}
