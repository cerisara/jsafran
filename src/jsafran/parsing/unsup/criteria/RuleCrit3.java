package jsafran.parsing.unsup.criteria;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import opennlp.tools.util.HashList;
import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.rules.Rules;


/**
 * 
 * This class implements the "Frame model" back
 * 
 * @author xtof
 *
 */
public class RuleCrit3 implements Serializable {
	private static final long serialVersionUID = 1L;
	int nrules=0;
	public Rules rules;
	public Voc voc;

//	HashSet<String> aobjverbs = new HashSet<String>();
//	HashSet<String> deobjverbs = new HashSet<String>();
//	HashSet<String> objverbs = new HashSet<String>();
//	final int nverblists = 4; // +1 car c'est le nb de valeurs que peut prendre cette feature

	DetTopology toposcore = new DetTopology();

//	int[] rule4utt;

	// the counts:
	// H x D x noccs
	HashMap<Integer,HashMap<Integer, Integer>> countsHD = new HashMap<Integer, HashMap<Integer,Integer>>();
	// H x W x noccs
	HashMap<Integer,HashMap<Integer, Integer>> countsHW = new HashMap<Integer, HashMap<Integer,Integer>>();
	// WARNING: we must keep and update in increaseCounts()/decreaseCounts() in countsHD.get(h).get(-1) the TOTAL over all d

	public static RuleCrit3 loadCounts(String infilename, Rules rules) {
		try {
			
			DataInputStream f = new DataInputStream(new FileInputStream(infilename));
			double logpost = f.readDouble();
			System.out.println("load best counts logpost on train: "+logpost);
			Voc voc = Voc.load(f);
			RuleCrit3 m = new RuleCrit3(rules, voc);
			m.voc=voc;
			{
				int n=f.readInt();
				for (int i=0;i<n;i++) {
					int x=f.readInt();
					int nn=f.readInt();
					HashMap<Integer, Integer> h = new HashMap<Integer, Integer>();
					m.countsHD.put(x, h);
					for (int j=0;j<nn;j++) {
						int y=f.readInt();
						int z=f.readInt();
						h.put(y, z);
					}
				}
			}
			{
				int n=f.readInt();
				for (int i=0;i<n;i++) {
					int x=f.readInt();
					int nn=f.readInt();
					HashMap<Integer, Integer> h = new HashMap<Integer, Integer>();
					m.countsHW.put(x, h);
					for (int j=0;j<nn;j++) {
						int y=f.readInt();
						int z=f.readInt();
						h.put(y, z);
					}
				}
			}
			f.close();
			return m;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public void saveCounts(String outfilename, double logpost) {
		try {
			DataOutputStream f = new DataOutputStream(new FileOutputStream(outfilename));
			f.writeDouble(logpost);
			voc.save(f);
			{
				f.writeInt(countsHD.size());
				for (int i=0;i<countsHD.size();i++) {
					HashMap<Integer, Integer> h = countsHD.get(i);
					if (h!=null) {
						f.writeInt(h.size());
						for (int k : h.keySet()) {
							f.writeInt(k);
							f.writeInt(h.get(k));
						}
					} else f.writeInt(0);
				}
			}
			{
				f.writeInt(countsHW.size());
				for (int i=0;i<countsHW.size();i++) {
					HashMap<Integer, Integer> h = countsHW.get(i);
					if (h!=null) {
						f.writeInt(h.size());
						for (int k : h.keySet()) {
							f.writeInt(k);
							f.writeInt(h.get(k));
						}
					} else f.writeInt(0);
				}
			}
			f.close();
			System.out.println("saved best counts in "+outfilename+" logpost "+logpost);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// for debugging !
	HashMap<Integer,HashMap<Integer, Integer>> prevHD = null;
	public void debugCounts() {
		// cette fonction est appelee a la fin d'une iter
		int n=0, nn=0;
		for (int x : countsHD.keySet()) {
			n+=countsHD.get(x).get(-1);
			int nnn=0;
			for (int y : countsHD.get(x).keySet()) {
				if (y>=0) {
					nnn+=countsHD.get(x).get(y);
				}
			}
			if (nnn!=countsHD.get(x).get(-1)) {
				System.out.println("ERROR TOT "+nnn+" "+countsHD.get(x).get(-1));
			}
			nn+=nnn;
		}
		int m=0;
		for (int x : countsHW.keySet()) {
			m+=countsHW.get(x).get(-1);
		}
		System.out.println("TOT COUNTS HD "+n+" "+nn+" HW "+m);

		if (false) {
			if (prevHD!=null) {
				int z=0;
				for (int h : countsHD.keySet()) {
					HashMap<Integer, Integer> d = countsHD.get(h);
					HashMap<Integer, Integer> prevd = prevHD.get(h);
					if (d.size()!=prevd.size()) z++;
					else {
						for (int df : d.keySet()) {
							if (!prevd.containsKey(df)) {z++;
							System.out.println("deleted "+df);
							break;}
							if (prevd.get(df)!=d.get(df)){z++;
							System.out.println("changed "+prevd.get(df)+" "+d.get(df));
							break;}
						}
					}
				}
				System.out.println("ndchanged "+z+" "+countsHD.size());
			}
			// deep copy
			prevHD = new HashMap<Integer, HashMap<Integer,Integer>>();
			for (int h : countsHD.keySet()) {
				HashMap<Integer, Integer> d = countsHD.get(h);
				HashMap<Integer, Integer> dd = new HashMap<Integer, Integer>();
				prevHD.put(h, dd);
				for (int a : d.keySet()) dd.put(a, d.get(a));
			}
		}
	}

	private void loadList(String file, HashSet<String> list) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("ISO-8859-1")));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) continue;
				String[] ss = s.split(" ");
				list.add(ss[0].toLowerCase());
			}
			f.close();
			System.out.println("load list "+file+" "+list.size());
		} catch (Exception e) {
			System.out.println("ERROR load lists");
		}
	}

	public RuleCrit3(Rules r, Voc v) {
		/*
		loadList("res/aobj-verbs.txt",aobjverbs);
		loadList("res/deobj-verbs.txt",deobjverbs);
		loadList("res/obj-verbs.txt",objverbs);
		 */
		voc=v;
		rules=r;
		ndeps = new int[getLabs2track().length];
	}

	@Deprecated
	public void saveStructLearned(String fbase, List<DetGraph> gs) {
		resetCache();
		try {
			PrintWriter f = new PrintWriter(new FileWriter(fbase+".y"));
			for (int h : countsHD.keySet()) {
				HashMap<Integer, Integer> d2co = countsHD.get(h);
				for (int d : d2co.keySet()) {
					if (d<0) continue;
					f.println("h="+h+" "+voc.getWord4Index(h)+" d="+d+" "+getDepsFromFrame(d)+" co="+d2co.get(d));
				}
			}
			f.close();

			f = new PrintWriter(new FileWriter(fbase+".graphs"));
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				for (int j=0;j<g.getNbMots();j++) {
					int h = getWord(g, j);
					int d=getFrameD(g, j);
					f.println(i+" "+j+" "+h+" "+d);
				}
			}
			f.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected double getAlphaD(int d) {
		return 0.1;
	}
	protected double getAlphaW(int w) {
		return 0.000001;
	}
	private int getVocSize() {
		return voc.getVocSize();
	}
	private int getWord(DetGraph g, int i) {
		return voc.getWordIndex(g.getMot(i));
	}

	int getFrameD(DetGraph g, int i) {
		if (g==lastgComputed) return lastD[i];
		precomputeStructures(g);
		return lastD[i];
	}
	int[] getChildren(DetGraph g, int i) {
		if (g==lastgComputed) return lastW[i];
		precomputeStructures(g);
		return lastW[i];
	}
	int getTotal(HashMap<Integer,HashMap<Integer, Integer>> counts, int x) {
		HashMap<Integer, Integer> co = counts.get(x);
		if (co==null) return 0;
		Integer n = co.get(-1);
		assert n!=null; // otherwise, that's because it has not been correctly updated in inc/dec...
		return n;
	}
	int getCount(HashMap<Integer,HashMap<Integer, Integer>> counts, int x, int y) {
		HashMap<Integer, Integer> co = counts.get(x);
		if (co==null) return 0;
		Integer n = co.get(y);
		if (n==null) return 0;
		return n;
	}

	// methode inverse:
	private String getDepsFromFrame(int y) {
		int npobj = y%maxndeps;
		y-=npobj;
		y/=maxndeps;
		int nobj = y%maxndeps;
		y-=nobj;
		y/=maxndeps;
		int nsuj = y%maxndeps;
		return " nsuj="+nsuj+" nobj="+nobj+" npobj="+npobj;
	}

	private void updateCounts(boolean inc, DetGraph g, HashMap<Integer, HashMap<Integer, Integer>> countshd, HashMap<Integer, HashMap<Integer, Integer>> countshw) {
		for (int i=0;i<g.getNbMots();i++) {
			int h = getWord(g, i);
			HashMap<Integer, Integer> d2co = countshd.get(h);
			if (d2co==null) {
				d2co = new HashMap<Integer, Integer>(); countshd.put(h, d2co);
				d2co.put(-1, 0); // total for one h over all d
			}
			int d = getFrameD(g, i);
			Integer co=d2co.get(d);

			if (inc) {
				if (co==null) co=1; else co++;
				d2co.put(d, co);
				int tot=d2co.get(-1);
				d2co.put(-1, ++tot);
			} else {
				co--;
				if (co==0) d2co.remove(d);
				else if (co<0) System.out.println("ERROR negative counts !");
				d2co.put(d, co);
				int tot=d2co.get(-1);
				d2co.put(-1, --tot);
				if (tot<0) System.out.println("ERROR negative counts tot !");
				assert tot>=0;
			}

			HashMap<Integer, Integer> w2co = countshw.get(h);
			if (w2co==null) {
				w2co = new HashMap<Integer, Integer>(); countshw.put(h, w2co);
				w2co.put(-1,0);
			}
			int[] ws = getChildren(g, i);
			for (int w : ws) {
				co=w2co.get(w);
				if (inc) {
					if (co==null) co=1; else co++;
					int tot=w2co.get(-1)+1;
					w2co.put(-1, tot);
				} else {
					co--;
					if (co<=0) w2co.remove(w);
					int tot=w2co.get(-1)-1;
					w2co.put(-1, tot);
					assert tot>=0;
				}
				w2co.put(w, co);
			}
		}

	}

	public double getTotalLogPost(List<DetGraph> gs) {
		double dentropy = 0;
		for (int h : countsHD.keySet()) {
			double hh = 0;
			HashMap<Integer, Integer> theta = countsHD.get(h);
			if (theta.get(-1)>0)
				for (int d : theta.keySet()) {
					if (d>=0) {
						double p = (double)theta.get(d)/(double)theta.get(-1);
						if (p>0) hh+=-p*Math.log(p);
					}
				}
			dentropy+=hh;
		}
		double wentropy = 0;
		for (int h : countsHW.keySet()) {
			double hh = 0;
			HashMap<Integer, Integer> theta = countsHW.get(h);
			if (theta.get(-1)>0)
				for (int w : theta.keySet()) {
					if (w>=0) {
						double p = (double)theta.get(w)/(double)theta.get(-1);
						if (p>0) hh+=-p*Math.log(p);
					}
				}
			wentropy+=hh;
		}
		System.out.println("entropy: "+dentropy+" "+wentropy);

		double res=0;
		for (DetGraph g: gs) {
			HashMap<Integer, HashMap<Integer, Integer>> countsHDu = new HashMap<Integer, HashMap<Integer,Integer>>();
			HashMap<Integer, HashMap<Integer, Integer>> countsHWu = new HashMap<Integer, HashMap<Integer,Integer>>();
			updateCounts(true, g, countsHDu, countsHWu);

			for (int h : countsHDu.keySet()) {
				HashMap<Integer, Integer> theta = countsHD.get(h);
				if (theta!=null&&theta.get(-1)>0) 
					for (int d : countsHDu.get(h).keySet()) {
						if (d>=0) {
							double like = Math.log((double)theta.get(d)/(double)theta.get(-1));
							like *= (double)countsHDu.get(h).get(d);
							res+=like;
						}
					}
				theta = countsHW.get(h);
				if (theta!=null&&theta.get(-1)>0) 
					for (int w : countsHWu.get(h).keySet()) {
						if (w>=0) {
							double like = Math.log((double)theta.get(w)/(double)theta.get(-1));
							like *= (double)countsHWu.get(h).get(w);
							res+=like;
						}
					}
			}
		}
		return res;
	}

	// rules contient toutes les regles qui ont ete appliquees sur cette phrase.
	// La meme regle peut apparaitre plusieurs fois dans la liste, si elle a ete appliquee a plusieurs positions
	// rules[0]=regle, rules[1]=position
	public double getLogPost(DetGraph g, List<int[]> rs) {
		double res=0;

		// ------------------------------------------------
		// counts n^u_{h,d} and n^u_{h,w} for the current sentence
		// (the same counts n^{-u}_{h,d} for the rest of the corpus are in countsHD and countsHW)
		HashMap<Integer, HashMap<Integer, Integer>> countsHDu = new HashMap<Integer, HashMap<Integer,Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> countsHWu = new HashMap<Integer, HashMap<Integer,Integer>>();
		updateCounts(true, g, countsHDu, countsHWu);

		// on doit avoir exactement les memes h dans les 2 hashmaps
		assert countsHDu.size()==countsHWu.size();

		for (int h : countsHDu.keySet()) {
			// term1
			HashMap<Integer, Integer> d2co = countsHDu.get(h);
			double t1b=0;
			int nhutot=0;
			for (int d : d2co.keySet()) {
				if (d<0) continue;
				int nhdu = d2co.get(d);
				nhutot+=nhdu;
				for (int i=0;i<nhdu;i++) {
					t1b += Math.log(getAlphaD(d)+getCount(countsHD,h,d)+i);
				}
			}
			// we assume symetric alpha !
			double AD = getAlphaD(0)*getDsize();
			double t1a=0;
			int nsum = getTotal(countsHD,h);
			for (int i=0;i<nhutot;i++) {
				t1a -= Math.log(AD+nsum+i);
			}
			// il y a un "D" par mot; donc sur la phrase, la somme des counts pour calculer term1 = nb_mots
			double term1 = t1a+t1b;

			// term2
			HashMap<Integer, Integer> w2co = countsHWu.get(h);
			double t2b=0;
			int nhutot2=0;
			if (w2co!=null)
				for (int w : w2co.keySet()) {
					if (w<0) continue;
					int nhwu = w2co.get(w);
					nhutot2+=nhwu;
					for (int i=0;i<nhwu;i++) {
						t2b += Math.log(getAlphaW(w)+getCount(countsHW,h,w)+i);
					}
				}
			// we assume symetric alpha !
			double AW = getAlphaW(0)*getWsize();
			double t2a=0;
			nsum = getTotal(countsHW,h);
			for (int i=0;i<nhutot2;i++) {
				t2a -= Math.log(AW+nsum+i);
			}
			// Attention ! Une phrase sans aucune dep aura 0 fils, donc term2 est calculé avec un nombre variable de counts totaux,
			// ce qui peut poser pb pour sampler/comparer les posterior entre eux !
			double term2 = t2a+t2b;

//			System.out.println("logpostdebug h="+h+" "+t1a+" "+t1b+" "+t2a+" "+t2b+" "+res);
			
			//			System.out.println("logpostdebug h="+h+" "+t1a+" "+t1b+" "+t2a+" "+t2b+" "+res);

			res+=term1+term2;

		}
		return res;
	}

	public void increaseCounts(DetGraph g, List<int[]> word2rule) {
		updateCounts(true, g, countsHD, countsHW);
	}
	public void decreaseCounts(DetGraph g, List<int[]> word2rule) {
		updateCounts(false, g, countsHD, countsHW);
	}
	public void resetCache() {
		lastgComputed=null;
	}
	// **************** Frame computation

	protected int[] getLabs2track() {
		final int[] labs = {
				Dep.getType("MOD"),
				Dep.getType("SUJ"),Dep.getType("OBJ"),Dep.getType("POBJ")};
		return labs;
	}
	final int maxndeps = 5;
	private DetGraph lastgComputed = null;
	private int[] lastD;
	private int[][] lastW;
	private int[] ndeps;
	private void precomputeStructures(DetGraph g){
		lastD = new int[g.getNbMots()];
		lastW = new int[g.getNbMots()][];

		HashMap<Integer, List<Dep>> head2frame = new HashMap<Integer, List<Dep>>();
		for (Dep d : g.deps) {
			int head = d.head.getIndexInUtt()-1;
			List<Dep> fr = head2frame.get(head);
			if (fr==null) {
				fr = new ArrayList<Dep>();
				head2frame.put(head, fr);
			}
			fr.add(d);
		}
		for (int i=0;i<g.getNbMots();i++) {
			List<Dep> fr = head2frame.get(i);
			int nfils = 0;
			if (fr!=null) nfils=fr.size();
			lastW[i] = new int[nfils];
			for (int j=0;j<nfils;j++) {
				lastW[i][j]=getWord(g, fr.get(j).gov.getIndexInUtt()-1);
			}

			// for now, I just count the nb of SUJ, OBJ, POBJ
			// idx = nsuj*nmax^2 + nobj*nmax + npobj
			int y=0;
			Arrays.fill(ndeps, 0);
			if (fr!=null)
				for (Dep dep : fr) {
					for (int k=0;k<ndeps.length;k++)
						if (dep.type==getLabs2track()[k]) {
							ndeps[k]++;
							break;
						}
				}
			for (int k=0;k<ndeps.length;k++)
				if (ndeps[k]>maxndeps) ndeps[k]=maxndeps;
			for (int k=0;k<ndeps.length;k++) {
				y*=maxndeps;
				y+=ndeps[k];
			}
			lastD[i]=y;
		}

		lastgComputed=g;
	}
	private int getDsize(){
		return maxndeps*maxndeps*maxndeps;
	}
	private int getWsize(){
		return getVocSize();
	}


}
