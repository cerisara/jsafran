package jsafran.parsing.unsup.criteria;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.Mot;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.rules.Rules;


/**
 * je ne m'appuie plus sur la classe Criterion, car l'idee d'avoir une classe generique pour tous les criteres n'est pas bonne: trop de choses specifiques a faire...
 * 
 * This class corresponds to the "Siblings model"
 * 
 * @author xtof
 *
 */
public class RuleCrit2 {
	int nrules=0;
	Rules rules;
	Voc voc;
	
	HashSet<String> aobjverbs = new HashSet<String>();
	HashSet<String> deobjverbs = new HashSet<String>();
	HashSet<String> objverbs = new HashSet<String>();
	final int nverblists = 4; // +1 car c'est le nb de valeurs que peut prendre cette feature
	
	DetTopology toposcore = new DetTopology();
	
	int[] rule4utt;
	
	// the hyperparameters (TODO: use them)
	double[] alphaR = null;
	double alphaW = 0.01f;
	
	// the counts:
	int[] countsR=null;
	// structure Y x Word => nb. occ.
	HashMap<Integer,HashMap<Integer, Integer>> countsYW = new HashMap<Integer, HashMap<Integer,Integer>>();
	
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
	
	public RuleCrit2(Rules r, Voc v) {
		loadList("res/aobj-verbs.txt",aobjverbs);
		loadList("res/deobj-verbs.txt",deobjverbs);
		loadList("res/obj-verbs.txt",objverbs);
		voc=v;
		rules=r;
		alphaR = new double[rules.getNrules()];
		for (int i=0;i<alphaR.length;i++) alphaR[i] = rules.getRulePrior(i);
		countsR = new int[rules.getNrules()];
		Arrays.fill(countsR,0);
	}

	public void saveStructLearned(String fbase, List<DetGraph> gs) {
		try {
			HashMap<Integer, Integer> y2tot = new HashMap<Integer, Integer>();
			PrintWriter f = new PrintWriter(new FileWriter(fbase+".y"));
			for (int y : countsYW.keySet()) {
				HashMap<Integer, Integer> w2co = countsYW.get(y);
				int cotot=0;
				for (int w : w2co.keySet()) {
					f.println("y="+y+" "+getStructFromY(y)+" "+voc.getWord4Index(w)+" "+w2co.get(w));
					cotot+=w2co.get(w);
				}
				y2tot.put(y, cotot);
			}
			f.close();
			
			HashMap<Integer, Integer> y2tot2 = new HashMap<Integer, Integer>();
			f = new PrintWriter(new FileWriter(fbase+".graphs"));
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				for (int j=0;j<g.getNbMots();j++) {
					int y=getSiblings(g, j);
					f.println(i+" "+j+" "+y);
					Integer co = y2tot2.get(y);
					if (co==null) co=1;
					else co++;
					y2tot2.put(y, co);
				}
			}
			f.close();
			
			// check
			for (int y : y2tot.keySet()) {
				Integer co1= y2tot.get(y);
				Integer co2= y2tot2.get(y);
				if (co1==null||co2==null||!co1.equals(co2)) {
					System.out.println("ERROR STRUCT "+y+" "+co1+" "+co2);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * cette methode doit etre appelee au moment ou on commence une phrase:
	 * elle permet de savoir quelles sont les rules qui ont ete appliquees lors des iterations precedentes pour chacun des mots de la phrase
	 * 
	 * @param word2rule
	 */
	public void setCurrentRulesPerWord(int[] word2rule) {
		rule4utt = word2rule;
	}
	
	private double getAlphaR(int r) {
		return 0.01;
	}
	private double getAlphaY(int y) {
		return 0.01;
	}
	private int getVocSize() {
		return voc.getVocSize();
	}
	private int getWord(DetGraph g, int i) {
		return voc.getWordIndex(g.getMot(i));
	}
	private int getSiblings(DetGraph g, int w) {
		Mot head = null;
		final int[] labs = {Dep.getType("SUJ"),Dep.getType("OBJ"),Dep.getType("POBJ")};
		
		// il faut reduire ici la taille de l'espace possible.
		// sol1 = garder seulement les Nl+Nr freres les + proches
		// sol2 = garder seulement certaines deps (ex: SUJ, OBJ, ...)
		
		int y = voc.getWordIndex(head);
		
		int d = g.getDep(w);
		if (d>=0) {
			head = g.deps.get(d).head;
			y = voc.getWordIndex(head);
			{
				y*=nverblists;
				if (aobjverbs.contains(head.getLemme())) y+=1;
				else if (deobjverbs.contains(head.getLemme())) y+=2;
				else if (objverbs.contains(head.getLemme())) y+=3;
			}
			final int headidx = head.getIndexInUtt()-1;
			// for now, I just count the nb of SUJ, OBJ, POBJ
			// idx = head*nmax^3*Nverblists + verblist*nmax^3 + nsuj*nmax^2 + nobj*nmax + npobj
			int[] ndeps = {0,0,0};
			for (Dep dep : g.deps) {
				int dh = dep.head.getIndexInUtt()-1;
				if (dh==headidx) {
					for (int i=0;i<ndeps.length;i++)
						if (dep.type==labs[i]) {
							ndeps[i]++;
							break;
						}
				}
			}
			for (int i=0;i<ndeps.length;i++)
				if (ndeps[i]>maxndeps) ndeps[i]=maxndeps;
			for (int i=0;i<ndeps.length;i++) {
				y*=maxndeps;
				y+=ndeps[i];
			}
		} else {
			// w is root
			y*=nverblists;
			for (int i=0;i<labs.length;i++)
				y*=maxndeps;
		}
		return y;
	}
	final int maxndeps = 5;
	// methode inverse:
	private String getStructFromY(int y) {
		int npobj = y%maxndeps;
		y-=npobj;
		y/=maxndeps;
		int nobj = y%maxndeps;
		y-=nobj;
		y/=maxndeps;
		int nsuj = y%maxndeps;
		y-=nsuj;
		y/=maxndeps;
		int vlist = y%nverblists;
		y/=nverblists;
		String h = voc.getWord4Index(y);
		return "HEAD="+h+" vlist="+vlist+" nsuj="+nsuj+" nobj="+nobj+" npobj="+npobj;
	}
	
	// rules contient toutes les regles qui ont ete appliquees sur cette phrase.
	// La meme regle peut apparaitre plusieurs fois dans la liste, si elle a ete appliquee a plusieurs positions
	// rules[0]=regle, rules[1]=position
	public double getLogPost(DetGraph g, List<int[]> rules) {
		double res=0;
		
		// ------------------------------------------------
		double term1 = 0;
		for (int i=0;i<rules.size();i++) {
			term1 += Math.log(getAlphaR(rules.get(i)[0])+countsR[rules.get(i)[0]]);
		}
		// I normalize the rules prior by the number of words in the sentence, in order to make comparable rules sequences that have different lengths.
		term1/=(double)rules.size();
		term1*=(double)g.getNbMots();
		
		// ------------------------------------------------
		int[] y = new int[g.getNbMots()];
		HashSet<Integer> ys = new HashSet<Integer>();
		for (int i=0;i<g.getNbMots();i++) {
			y[i] = getSiblings(g,i);
			ys.add(y[i]);
		}
		
		double term2 = 0;
		Integer[] yinu = ys.toArray(new Integer[ys.size()]);
		Arrays.sort(yinu);
		int nyustar[] = new int[yinu.length];
		Arrays.fill(nyustar,0);
		// counts (y,*)
		for (int i=0;i<y.length;i++) {
			int yidx = Arrays.binarySearch(yinu, y[i]);
			nyustar[yidx]++;
		}
		// for all y in u:
		for (int yidx=0;yidx<yinu.length;yidx++) {
			int yy = yinu[yidx];
			int sumnyw = 0;
			if (countsYW.get(yy)!=null)
				for (Integer nyw : countsYW.get(yy).values()) sumnyw+=nyw;
			for (int j=0;j<nyustar[yidx];j++) {
				term2 -= Math.log(getVocSize()*getAlphaY(yy)+sumnyw+j);
			}
		}

		// ------------------------------------------------
		double term3 = 0;
		HashMap<Integer, Integer> wordsinu = new HashMap<Integer, Integer>();
		for (int i=0;i<y.length;i++) {
			int ww = getWord(g,i);
			Integer n = wordsinu.get(ww);
			if (n==null) n=1;
			else n++;
			wordsinu.put(ww, n);
		}
		// pour chaque y de la phrase u
		for (int yy : yinu) {
			// pour chaque w dans u
			for (int ww : wordsinu.keySet()) {
				for (int j=0;j<wordsinu.get(ww);j++) {
					HashMap<Integer, Integer> countsw = countsYW.get(yy);
					if (countsw==null)
						term3 += Math.log(getAlphaY(yy)+j);
					else {
						Integer n = countsw.get(ww);
						if (n==null) n=0;
						term3 += Math.log(getAlphaY(yy)+n+j);
					}
				}
			}
		}

		res=term1+term2+term3;
		
		return res;
	}
	
	public void increaseCounts(DetGraph g, List<int[]> word2rule) {
		for (int[] rulepos : word2rule) {
			countsR[rulepos[0]]++;
		}
		for (int i=0;i<g.getNbMots();i++) {
			int y = getSiblings(g,i);
			int w = getWord(g, i);
			HashMap<Integer, Integer> ws = countsYW.get(y);
			if (ws==null) {
				ws = new HashMap<Integer, Integer>();
				countsYW.put(y, ws);
			}
			Integer n = ws.get(w);
			if (n==null) ws.put(w, 1);
			else ws.put(w, n+1);
		}
	}
	public void decreaseCounts(DetGraph g, List<int[]> word2rule) {
		for (int[] rulepos : word2rule) {
			countsR[rulepos[0]]--;
			if (countsR[rulepos[0]]<0) {
				countsR[rulepos[0]]=0;
				System.out.println("ERROR countsR "+rulepos[0]+" "+g);
			}
		}
		for (int i=0;i<g.getNbMots();i++) {
			int y = getSiblings(g,i);
			int w = getWord(g, i);
			HashMap<Integer, Integer> ws = countsYW.get(y);
			if (ws==null) {
				System.out.println("ERROR countsY "+y+" "+g);
			} else {
				Integer n = ws.get(w);
				if (n==null) System.out.println("ERROR countsY "+y+" "+w+" "+g);
				else {
					if (--n<=0) {
						ws.remove(w);
						if (ws.size()<=0) countsYW.remove(y);
					} else ws.put(w, n);
				}
			}
		}
	}
}
