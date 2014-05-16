package jsafran.parsing.unsup.joint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import utils.SimpleBarChart;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.unsup.UnsupParsing;
import jsafran.parsing.unsup.Voc;

/**
 * Performs a depth first on the tree = all permutations of all applicable rules on the current sentence.
 * A rule is "applicable" if its word-related preconditions match some position in the sentence.
 * (There may be other preconditions that do not depend on the observed words, but on some latent variable; such preconditions are not
 * considered when computing the set of applicable rules, but, during the search, if they do not match, the function "applyRule()" then returns FALSE,
 * and the current branch must be pruned immediately.)
 * 
 * Each "rule" = (hand-defined rule, position in the sentence)
 * 
 * Now, the rules are not decomposed any more word by word, nor dep by dep: a rule may generate 0, 1 or more deps, as well as other latent variables
 * (head of the sentence, groups, ...). The rules may depend on the output of other rules.
 * But the rules must implement the "unapply()" method.
 * 
 * @author xtof
 *
 */
public class RulesDepthFirst {
	public interface UserRule {
		public boolean applyRule(DetGraph g, int position);
		public void unapplyRule(DetGraph g, int position);
		public int[] isApplicable(DetGraph g);
	}
	public interface Model {
		public float getUnnormalizedPosterior(DetGraph g);
		public void updateCounts(boolean inc, DetGraph g);
		public float getJoint(DetGraph g);
		public void save(String nom);
		// for debug: returns the total nb of instances
		public int getTot(); 
	}
	
	public List<UserRule> handRules;
	private Model model;
	
	public int pruningLevel = 1;
	
	public RulesDepthFirst(List<UserRule> rules, RulesDepthFirst.Model mod) {
		handRules=rules;
		model=mod;
	}
	
	// TODO: use it for pruning
	private HashMap<DetGraph,Float> bestScoresPerUtt = new HashMap<DetGraph, Float>();

	/**
	 * This method shuffles the applicable rules seq randomly and apply them.
	 * IT DOES NOT INCREASE THE COUNTS: the caller MUST do it !
	 * 
	 * Returns the list of rules+position applied;
	 * 
	 * @param g
	 */
	public List<Integer> init(DetGraph g) {
		// compute the list of applicable rules with preconditions on words:
		rules.clear(); rpos.clear(); curg=g;
		for (int i=0;i<handRules.size();i++) {
			int[] pos = handRules.get(i).isApplicable(g);
			for (int j=0;j<pos.length;j++) {
				rules.add(i);
				rpos.add(pos[j]);
			}
		}
//		System.out.println("napplicable init "+rules.size());
		rulesSeqs.clear(); posteriorPotential.clear();
		ArrayList<Integer> rule2apply = new ArrayList<Integer>();
		for (int i=0;i<rules.size();i++) {
			rule2apply.add(i);
		}
		Collections.shuffle(rule2apply);
		ArrayList<Integer> appliedRules = new ArrayList<Integer>();
/* debug: this is just to simulate the previous XP conditions, without any rule at initialisation.
 * 
		for (int i=0;i<rules.size();i++) {
			if (handRules.get(rules.get(rule2apply.get(i))).applyRule(g, rpos.get(rule2apply.get(i)))) {
				appliedRules.add(rules.get(rule2apply.get(i)));
				appliedRules.add(rpos.get(rule2apply.get(i)));
			}
		}
		*/
		return appliedRules;
	}
	
	/**
	 * 
	 * @param g
	 * 
	 * Before calling the search, you MUST decrease the counts of the model for the current sentence
	 * The search does not touch the counts; it only uses them to compute the posteriors;
	 * it then returns a double list with the rules-seq and the associated posterior.
	 * 
	 * The caller will then typically sample one of these rules-seq, apply it and increase the counts accordingly.
	 * 
	 */
	public double[] search(DetGraph g) {
		// compute the list of applicable rules with preconditions on words:
		rules.clear(); rpos.clear(); curg=g;
		for (int i=0;i<handRules.size();i++) {
			int[] pos = handRules.get(i).isApplicable(g);
			for (int j=0;j<pos.length;j++) {
				rules.add(i);
				rpos.add(pos[j]);
			}
		}
		// debug
		if (false) {
			System.out.println("rules applicable: "+rules.size()+" "+g);
/*			for (int i=0;i<rules.size();i++) {
				System.out.println("rule "+rules.get(i).toString()+" @pos "+rpos.get(i)+" "+g.getNbMots());
			}
*/
		}
		
//		System.out.println("napplicable "+rules.size());
		rulesSeqs.clear(); posteriorPotential.clear(); curRseq.clear();
		resdepsdbug.clear();
		exploredBranches.clear();
		searchRecurs(new SearchNode());
		
		double[] posts = new double[posteriorPotential.size()];
		for (int i=0;i<posts.length;i++) posts[i]=posteriorPotential.get(i);

//		System.out.println(posts.length);
//		SimpleBarChart.drawChart(posts);
		
		return posts;
	}
	// result of the search (for the current sentence) is in these 2 variables:
	public ArrayList<int[]> rulesSeqs = new ArrayList<int[]>();
	public ArrayList<Float> posteriorPotential = new ArrayList<Float>();
	// 4 debug only
	public ArrayList<int[]> resdepsdbug = new ArrayList<int[]>();
	
	// temporary variables:
	private Stack<Integer> curRseq = new Stack<Integer>();
	public ArrayList<Integer> rules = new ArrayList<Integer>();
	public ArrayList<Integer> rpos  = new ArrayList<Integer>();
	private DetGraph curg = null;
	
	/**
	 * immutable
	 * 
	 * Shall be adapted to include additional info (like utt. root)
	 * 
	 * @author xtof
	 */
	final class SearchNode {
		final private int[] govs, heads, types; // SORTED
		final private int[] rulesApplied; // SORTED
		public SearchNode() {
			rulesApplied=new int[0];
			govs = heads = types = new int[0];
		}
		public String toString() {
			return Arrays.toString(rulesApplied);
		}
		private SearchNode(int[] g, int[] h, int[] t, int[] r) {
			govs=g; heads=h; types=t; rulesApplied=r;
		}
		public SearchNode extend(List<Dep> ds, int ruleApplied) {
			int[] gv, hd, ty, ra;
			if (ruleApplied>=0) {
				int i=Arrays.binarySearch(rulesApplied, ruleApplied);
				if (i>=0) ra=rulesApplied;
				else {
					ra = Arrays.copyOf(rulesApplied, rulesApplied.length+1);
					i=-i-1;
					for (int j=ra.length-2;j>=i;j--) ra[j+1]=ra[j];
					ra[i]=ruleApplied;
				}
			} else ra = rulesApplied;
			int[] govtmp = govs;
			int[] headtmp = heads;
			int[] tytmp = types;
			for (Dep d : ds) {
				int i=Arrays.binarySearch(govtmp, d.gov.getIndexInUtt()-1);
				if (i<0) {
					gv = Arrays.copyOf(govtmp, govtmp.length+1);
					hd = Arrays.copyOf(headtmp, govtmp.length+1);
					ty = Arrays.copyOf(tytmp, govtmp.length+1);
					i=-i-1;
					for (int j=gv.length-2;j>=i;j--) {
						gv[j+1]=gv[j]; hd[j+1]=hd[j]; ty[j+1]=ty[j];
					}
					gv[i]=d.gov.getIndexInUtt()-1; hd[i]=d.head.getIndexInUtt()-1; ty[i]=d.type;
				} else {
					int i2=i+1; for (;i2<govtmp.length&&govtmp[i2]==govtmp[i];i2++);
					int j=Arrays.binarySearch(headtmp, i, i2, d.head.getIndexInUtt()-1);
					if (j<0) {
						gv = Arrays.copyOf(govtmp, govtmp.length+1);
						hd = Arrays.copyOf(headtmp, govtmp.length+1);
						ty = Arrays.copyOf(tytmp, govtmp.length+1);
						j=-j-1;
						for (int k=hd.length-2;k>=j;k--) {
							gv[k+1]=gv[k]; hd[k+1]=hd[k]; ty[k+1]=ty[k];
						}
						gv[j]=d.gov.getIndexInUtt()-1; hd[j]=d.head.getIndexInUtt()-1; ty[j]=d.type;
					} else {
						int j2=j+1; for (;j2<headtmp.length&&headtmp[j2]==headtmp[j];j2++);
						int k=Arrays.binarySearch(tytmp, j, j2, d.type);
						if (k<0) {
							gv = Arrays.copyOf(govtmp, govtmp.length+1);
							hd = Arrays.copyOf(headtmp, govtmp.length+1);
							ty = Arrays.copyOf(tytmp, govtmp.length+1);
							k=-k-1;
							for (int l=ty.length-2;l>=k;l--) {
								gv[l+1]=gv[l]; hd[l+1]=hd[l]; ty[l+1]=ty[l];
							}
							gv[k]=d.gov.getIndexInUtt()-1; hd[k]=d.head.getIndexInUtt()-1; ty[k]=d.type;
						} else {
							gv=govtmp; hd=headtmp; ty=tytmp;
						}
					}
				}
				govtmp = gv;
				headtmp=hd;
				tytmp=ty;
			}
			if (ra==rulesApplied&&govtmp==govs&&headtmp==heads&&tytmp==types) {
				// the new node is the same as the old node !!!
				return null;
			}
			return new SearchNode(govtmp, headtmp, tytmp, ra);
		}
		@Override
		public boolean equals(Object o) {
			SearchNode n=(SearchNode)o;
			if (!Arrays.equals(govs, n.govs)) return false;
			if (!Arrays.equals(heads, n.heads)) return false;
			if (!Arrays.equals(types, n.types)) return false;
			if (!Arrays.equals(rulesApplied, n.rulesApplied)) return false;
			return true;
		}
		@Override
		public int hashCode() {
			int h=0;
			for (int i=0;i<govs.length;i++) h+=govs[i];
			for (int i=0;i<rulesApplied.length;i++) h+=rulesApplied[i];
			return h;
		}
	}
	HashSet<SearchNode> exploredBranches = new HashSet<RulesDepthFirst.SearchNode>();
	/**
	 * At each "recur", we may either stop the application of rules,
	 * or apply one of the rules that have not been applied so far.
	 * 
	 * Far too slow. Rather defines an object = a node in the search tree
	 * that contains all the info about the state of the search (e.g. = (deps+utt. ROOT) + rulesApplied)
	 * and during the search, factorize all branches as soon as they produce the same node
	 * 
	 * @param ridx
	 */
	private void searchRecurs(SearchNode sn) {
//		System.out.println("recurs "+exploredBranches.size()+" "+curRseq);
		if (true) {
			// we may stop
			float sc = getUnnormalizedPosterior(curg);
			int[] rs = new int[curRseq.size()];
			for (int i=0;i<curRseq.size();i++) rs[i] = curRseq.get(i);
			rulesSeqs.add(rs);
			posteriorPotential.add(sc);

			// for debug only: store all generated graphs
			if (false) {
				int[] resdeps = new int[curg.getNbMots()*2];
				resdepsdbug.add(resdeps);
				for (int i=0;i<curg.getNbMots();i++) {
					int dd=curg.getDep(i);
					if (dd>=0) {
						resdeps[i*2]=curg.getHead(dd);
						resdeps[i*2+1]=curg.deps.get(dd).type;
					} else {
						resdeps[i*2]=-1;
						resdeps[i*2+1]=-1;
					}
				}

				if (false) {
					// debug
					System.out.println("rules: "+Arrays.toString(rs));
					JSafran.viewGraph(curg);
				}				
			}
		}

		// ... or apply one of the rules that have not been already applied
		for (int i=0;i<rules.size();i++) {
			// don't apply the rule if it has already been applied
			if (curRseq.contains(i)) continue;
			// don't apply the rule if all requirements are not met
DetGraph g0 = curg.clone();
			if (!handRules.get(rules.get(i)).applyRule(curg, rpos.get(i))) {
				// the rule cannot be applied, because of some requirement that is not matched...
//				int nompos = rpos.get(i)/curg.getNbMots();
//				int verbpos = rpos.get(i)%curg.getNbMots();
//				System.out.println("reject "+curRseq+" "+nompos+" "+verbpos);
//				JSafran.viewGraph(curg);
				continue;
			}
			
			// now, the rule has been applied !
			// don't apply the rule if pruning says so
			if (pruningLevel>0) {
				// TODO better topological score pruning
				if (!checkTopo()) {
					handRules.get(rules.get(i)).unapplyRule(curg, rpos.get(i));
					continue;
				}
			}
			// don't apply the rule if this branch can be factorized with a previous branch
			SearchNode sn2 = sn.extend(curg.deps, rules.get(i));
			if (sn2==null||exploredBranches.contains(sn2)) {
				handRules.get(rules.get(i)).unapplyRule(curg, rpos.get(i));
				continue;
			}
			
			curRseq.add(i);
			searchRecurs(sn2);
			handRules.get(rules.get(i)).unapplyRule(curg, rpos.get(i));
			curRseq.pop();
		}
		// when the full subtree of this search node has been explored, we save this node.
		exploredBranches.add(sn);
	}

	boolean checkTopo() {
		for (int i=0;i<curg.deps.size();i++) {
			Dep d = curg.deps.get(i);
			for (int j=i+1;j<curg.deps.size();j++) {
				Dep dd = curg.deps.get(j);
				int a=d.gov.getIndexInUtt()-1;
				int b=d.head.getIndexInUtt()-1;
				if (a>b) {int c=a;a=b;b=c;}
				int aa=dd.gov.getIndexInUtt()-1;
				int bb=dd.head.getIndexInUtt()-1;
				if (aa>bb) {int c=aa;aa=bb;bb=c;}
				if (aa<a) {
					int c=aa,e=bb;
					aa=a; bb=b; a=c; b=e;
				}
				if (aa<b && aa>a && bb>b) return false;
			}
		}
		return true;
	}
	
	// the higher the better
	private float getUnnormalizedPosterior(DetGraph g) {
		return model.getUnnormalizedPosterior(g);
	}
	
	/**
	 * counts en generatifs entre un HEAD=CLASSE=MIXIDX et un MOT=OBS
	 * 
	 * @author xtof
	 *
	 */
	static class Counts {
		int[][] co = null;

		public int getSum(int h) {
			return co[h][co[h].length-1];
		}
		public int getCounts(int h, int w) {
			if (co==null) return 0;
			return co[h][w];
		}
		public void incCount(int h, int w) {
			co[h][w]++;
			co[h][co[h].length-1]++;
		}
		public void razCount(int h, int w) {
			co[h][co[h].length-1]-=co[h][w];
			co[h][w]=0;
		}
		public void decCount(int h, int w) {
			co[h][w]--;
			if (co[h][w]<0) {
				co[h][w]=0;
				System.out.println("ERROR neg counts !");
			} else {
				co[h][co[h].length-1]--;
			}
		}
		public void init(int nheads, int nwords) {
			co = new int[nheads][nwords+1];
		}
		public void save(DataOutputStream f) throws IOException {
			f.writeInt(co.length);
			for (int i=0;i<co.length;i++) {
				f.writeInt(co[i].length);
				for (int j=0;j<co[i].length;j++) {
					f.writeInt(co[i][j]);
				}
			}
		}
		public void load(DataInputStream f) throws IOException {
			co = new int[f.readInt()][];
			for (int i=0;i<co.length;i++) {
				co[i] = new int[f.readInt()];
				for (int j=0;j<co[i].length;j++) {
					co[i][j] = f.readInt();
				}
			}
		}
	}
	
	/**
	 * 
	 * @param sampledR
	 * @param g
	 * @return the stack of all rules applied, in the same format as init()
	 */
	public Stack<Integer> applyChosenRule(int sampledR, DetGraph g) {
		int[] res = rulesSeqs.get(sampledR);
		curRseq.clear();
		for (int i=0;i<res.length;i++) {
			int handr = rules.get(i);
			int pos = rpos.get(i);
			handRules.get(handr).applyRule(g, pos);
			curRseq.add(rules.get(i));
			curRseq.add(rpos.get(i));
		}
		return curRseq;	
	}
	
	// ============================================================================================

	private Random rand = new Random();
	int sample_Mult(double[] th) {
		double s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextDouble();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}
	
	// ============================================================================================

	public static void main(String args[]) throws Exception {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("test2009.xml");
		ArrayList<DetGraph> glist = new ArrayList<DetGraph>();
		for (int i=0,j=0;i<gs.size();i++) {
			if (gs.get(i).getNbMots()<=15) {
				glist.add(gs.get(i));
				++j;
//				if (j>50) break;
			}
		}
		System.out.println("corp size "+glist.size());
		final Voc voc = new Voc();
		voc.init(glist);
		
		class R1 implements UserRule {
			String d;
			int deptyp;
			public R1(String dlab) {
				d=dlab;
				deptyp=Dep.getType(d);
			}
			@Override
			public boolean applyRule(DetGraph g, int position) {
				int nompos = position/g.getNbMots();
				int verbpos = position%g.getNbMots();
				int de=g.getDep(nompos);
				if (de>=0) return false;
				g.ajoutDep(d, nompos, verbpos);
				return true;
			}
			@Override
			public void unapplyRule(DetGraph g, int position) {
				int nompos = position/g.getNbMots();
				int[] ds = g.getDeps(nompos);
				for (int i: ds) {
					if (g.deps.get(i).type==deptyp) {
						g.deps.remove(i); return;
					}
				}
			}
			@Override
			public int[] isApplicable(DetGraph g) {
				int[] npos = null, vpos=null;
				ArrayList<Integer> noms = new ArrayList<Integer>();
				ArrayList<Integer> verbs = new ArrayList<Integer>();
				for (int i=0;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("N")) noms.add(i);
					else if (g.getMot(i).getPOS().startsWith("V")) verbs.add(i);
				}
				npos=new int[noms.size()*verbs.size()];
				vpos = new int[npos.length];
				for (int i=0,k=0;i<noms.size();i++) {
					for (int j=0;j<verbs.size();j++) {
						npos[k]=noms.get(i); vpos[k++]=verbs.get(j);
					}
				}
				int[] res = new int[npos.length];
				for (int i=0;i<res.length;i++) {
					res[i]=npos[i]*g.getNbMots()+vpos[i];
				}
				return res;
			}
		}
		ArrayList<UserRule> rules = new ArrayList<UserRule>();
		rules.add(new R1("SUJ"));
		rules.add(new R1("OBJ"));
		
		class MyModel implements RulesDepthFirst.Model {
			final float alpha=0.01f;
			float atot;
			Counts co = new Counts();
			
			public double getAlpha(int w) {return alpha;}
			
			public void save(String nom) {
				try {
					DataOutputStream f = new DataOutputStream(new FileOutputStream(nom));
					voc.save(f);
					co.save(f);
					f.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			public MyModel() {
				co.init(voc.getVocSize(), voc.getVocSize());
				atot=0;
				for (int i=0;i<voc.getVocSize();i++) {
					String w = voc.getWord4Index(i);
					if (w.equals("ROOT")||w.equals("UNK")) continue;
					atot+=getAlpha(i);
				}
			}
			
			public int getTot() {
				int tot=0;
				for (int h=0;h<voc.getVocSize();h++)
					tot+=co.getSum(h);
				return tot;
			}
			
			public void updateCounts(boolean inc, DetGraph g) {
				updateCounts(co, inc, g);
			}
			private void updateCounts(Counts cc, boolean inc, DetGraph g) {
				if (inc) {
					for (int i=0;i<g.getNbMots();i++) {
						int w=voc.getWordIndex(g.getMot(i));
						int d=g.getDep(i);
						if (d<0) {
							int h=voc.getWordIndex(null);
							cc.incCount(h, w);
						} else {
							int h=voc.getWordIndex(g.getMot(g.getHead(d)));
							cc.incCount(h, w);
						}
					}
				} else {
					for (int i=0;i<g.getNbMots();i++) {
						int w=voc.getWordIndex(g.getMot(i));
						int d=g.getDep(i);
						if (d<0) {
							int h=voc.getWordIndex(null);
							cc.decCount(h, w);
						} else {
							int h=voc.getWordIndex(g.getMot(g.getHead(d)));
							cc.decCount(h, w);
						}
					}
				}
			}
			
			public float getJoint(DetGraph g) {
				double post=0;
				for (int i=0;i<g.getNbMots();i++) {
					int w=voc.getWordIndex(g.getMot(i));
					int h;
					int d=g.getDep(i);
					if (d<0) h=voc.getWordIndex(null);
					else h=voc.getWordIndex(g.deps.get(d).head);
					post+=Math.log(voc.getUnnormalizedPrior(h));
					int n=co.getCounts(h, w);
					int s=co.getSum(h);
					post+=Math.log((double)n/(double)s);
				}
				return (float)post;
			}
			
			@Override
			public float getUnnormalizedPosterior(DetGraph g) {
				// the posterior computed here corresponds to the "simple generative mixture" in acl2012/rulesSampler.tex
				
				Counts cou = new Counts();
				cou.init(voc.getVocSize(), voc.getVocSize());
				updateCounts(cou, true, g);
				HashSet<Integer> hinu = new HashSet<Integer>();
				int[] hs = new int[g.getNbMots()];
				int[] ws = new int[g.getNbMots()];
				double post=0;
				for (int i=0;i<g.getNbMots();i++) {
					int w=voc.getWordIndex(g.getMot(i));
					int h;
					int d=g.getDep(i);
					if (d<0) h=voc.getWordIndex(null);
					else h=voc.getWordIndex(g.deps.get(d).head);

					hs[i]=h; ws[i]=w;
					hinu.add(h);
					
					post+=Math.log(voc.getUnnormalizedPrior(h));
				}
				
				for (int h : hinu) {
					int nnegu = co.getSum(h);
					int nu = cou.getSum(h);
					int ntot=nnegu+nu;
					for (int j=nnegu;j<ntot;j++) {
						post-=Math.log(atot + j);
					}
				}
				
				for (int i=0;i<g.getNbMots();i++) {
					int nu=cou.getCounts(hs[i], ws[i]);
					if (nu==0) continue;
					int nnegu = co.getCounts(hs[i], ws[i]);
					int ntot = nu+nnegu;
					for (int j=nnegu;j<ntot;j++) {
						post+=Math.log(getAlpha(ws[i])+i);
					}
					cou.razCount(hs[i], ws[i]);
				}
				return (float)post;
			}
		}

		RulesDepthFirst.Model mod = new MyModel();
		RulesDepthFirst search = new RulesDepthFirst(rules, mod);
		HashMap<DetGraph, List<Integer>> graph2rules = new HashMap<DetGraph, List<Integer>>();
		for (DetGraph gt : glist) {
			gt.clearDeps();
			List<Integer> l = search.init(gt);
			graph2rules.put(gt, l);
			mod.updateCounts(true, gt);
		}
		
		// just after init
		{
			float joint=0;
			for (DetGraph gt : glist) {
				joint += mod.getJoint(gt);
			}
			System.out.println("init joint "+joint+" tot "+mod.getTot());
		}
		
		final int niters=1000;
		for (int iter=0;iter<niters;iter++) {
			float joint=0;
			for (int gi=0;gi<glist.size();gi++) {
//				System.out.print("graph "+gi+"/"+glist.size()+"\r");
				DetGraph g = glist.get(gi);
				
				// decrease counts
				List<Integer> rapp = graph2rules.get(g);
				mod.updateCounts(false, g);
				for (int i=0;i<rapp.size();) {
					int r=rapp.get(i++);
					int p=rapp.get(i++);
					rules.get(r).unapplyRule(g, p);
				}
				if (g.deps.size()>0) System.out.println("ERROR UNAPPLY "+g.deps.size());
				rapp.clear();
				// compute post distrib
				double[] post = search.search(g);
				
				// sample
				UnsupParsing.normalizeLog(post);
				int sampledR = search.sample_Mult(post);
				
				// apply chosen rule
				Stack<Integer> rulesapp = search.applyChosenRule(sampledR,g);
				for (int i : rulesapp) rapp.add(i);
				
				// increase counts
				mod.updateCounts(true, g);
				
				joint+=mod.getJoint(g);
			}
			System.out.println("iter "+iter+" joint "+joint+" tot "+mod.getTot());
			if (iter%100==0) mod.save("mods.iter"+iter);
		}
	}
}
