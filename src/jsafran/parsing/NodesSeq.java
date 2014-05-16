package jsafran.parsing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

/**
 * contains a sequence of nodes, each node being a subtree composed of adjacent words
 * 
 * @author xtof
 *
 */
public class NodesSeq {
	DetGraph g;
	// doit toujours etre monotone, et avec des nodes numerotes en +1
	int[] word2node;
	public static SelectionModel selectionModel;

	public final static int nHMMs = 1;

	private static int maxlen2score=-1;

	/**
	 * Autorise les multiple roots qui forment des graphes séparés
	 * Supprime les liens non-projectifs
	 * 
	 * @param g
	 * @return
	 */
	private static List<Integer> countNonProjectivity(DetGraph g, boolean corrige) {
		ArrayList<Integer> badwords = new ArrayList<Integer>();
		boolean wasproj=true;
		for (int i=0;i<g.getNbMots();i++) {
			int d=g.getDep(i);
			if (d>=0) {
				int h=g.getHead(d);
				int w1 = i, w2=h;
				if (w1>w2) {w1=h;w2=i;}
				for (int j=w1+1;j<w2;j++) {
					int d2=g.getDep(j);
					if (d2<0) {
						// il y a un ROOT sous une dépendance
						badwords.add(i);
						wasproj=false;
						if (corrige) {
							// est-ce qu'on peut supporter ce type de non-projectivité ?
							g.removeDep(d); ndepdel++;
						}
						break;
					}
					int h2=g.getHead(d2);
					if (h2<w1||h2>w2) {
						// il y a 2 arcs qui se croisent
						badwords.add(i);
						wasproj=false;
						if (corrige) {
							// je supprime le premier arc (supprimer plutot l'arc le plus long ?)
							g.removeDep(d);
							ndepdel++;
						}
						break;
					}
				}
			}
		}
		return badwords;
	}
	public NodesSeq() {
	}

	static int nonproj=0,proj=0,ndepdel=0;
	public NodesSeq(DetGraph gr) {
		if (countNonProjectivity(gr,true).size()>0) {
			nonproj++;
		} else proj++;
		g=gr;
		word2node = new int[g.getNbMots()];
		for (int i=0;i<word2node.length;i++) word2node[i]=i;

	}
	//	public boolean equals(Object o) {
	//		NodesSeq q = (NodesSeq)o;
	//		return Arrays.deepEquals(word2node, q.word2node);
	//	}
	public int getNodeRoot(int node) {
		int[] ws = getNodeFirstAndLastNodes(node);
		int w = ws[0];
		HashSet<Integer> dejavu = new HashSet<Integer>();
		dejavu.add(w);
		for (;;) {
			int d = g.getDep(w);
			if (d<0) return w;
			int h = g.getHead(d);
			if (h<ws[0]) return w;
			if (h>=ws[1]) return w;
			w=h;
			if (dejavu.contains(w)) {
				System.out.println("ERROR CYCLE ! "+w+" "+g.getMot(w)+Arrays.toString(ws));
				JSafran.viewGraph(g,w);
			}
			dejavu.add(w);
		}
	}
	/**
	 * 
	 * @param node
	 * @return 2-dim tab, with first and last (exclusive) nodes
	 */
	public int[] getNodeFirstAndLastNodes(int node) {
		int w1;
		for (w1=0;;w1++) {
			if (word2node[w1]==node) break;
		}
		int w2=w1;
		for (;w2<word2node.length;w2++) {
			if (word2node[w2]>node) break;
		}
		int[] res = {w1,w2};
		return res;
	}
	public int tmphead;
	public String getLink(int node) {
		tmphead=-1;

		int nodehead = getNodeRoot(node);
		int d=g.getDep(nodehead);
		if (d>=0) {
			int h=g.getHead(d);
			int headnode = word2node[h];
			if (headnode==node-1) {
				tmphead=h;
				return "RA"+g.getDepLabel(d);
			} else if (headnode==node+1) {
				tmphead=h;
				return "LA"+g.getDepLabel(d);
			} else if (headnode==node+2) {
				tmphead=h;
				return "LB"+g.getDepLabel(d);
			} else if (headnode==node-2) {
				tmphead=h;
				return "RB"+g.getDepLabel(d);
			} else if (headnode==node) {
				System.out.println("WARNING: link to same node ?? cycle ??");
			}
		}
		return "NA";
	}
	public int getNnodes() {
		if (word2node==null) return 0;
		return word2node[word2node.length-1]+1;
	}
	public DetGraph getGraph() {
		return g;
	}

	public void mergeNodes(String[] acts) {
		if (word2node==null) return;
		assert acts.length==getNnodes();
		
		int[] actidx2word = new int[acts.length];
		for (int i=0;i<acts.length;i++) actidx2word[i]=getNodeRoot(i);
		
		// left-right
		for (int i=acts.length-1;i>=0;i--) {
			if (acts[i].startsWith("LB")) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i+2]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i+1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			} else if (acts[i].startsWith("L")) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i+1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			}
		}
		// right-left
		for (int i=0;i<acts.length;i++) {
			if (acts[i].startsWith("RB")) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i-2]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
				node2move = word2node[actidx2word[i-1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			} else if (acts[i].startsWith("R")) {
				int node2move = word2node[actidx2word[i]];
				int targetnode = word2node[actidx2word[i-1]];
				for (int j=0;j<word2node.length;j++)
					if (word2node[j]==node2move) word2node[j]=targetnode;
			}
		}
				
		// translate tous les index pour qu'ils soient continus
		HashSet<Integer> vals = new HashSet<Integer>();
		for (int j=0;j<word2node.length;j++)
			vals.add(word2node[j]);
		Integer[] xvals = vals.toArray(new Integer[vals.size()]);
		Arrays.sort(xvals);

		int[] replace = new int[xvals[xvals.length-1]+1];
		Arrays.fill(replace, -1);
		for (int j=0;j<xvals.length;j++) replace[xvals[j]]=j;
		for (int j=0;j<word2node.length;j++)
			word2node[j]=replace[word2node[j]];
	}

	public int[] getWordsInNode(int node) {
		int[] ws = getNodeFirstAndLastNodes(node);
		int[] res = new int[ws[1]-ws[0]];
		for (int i=0;i<res.length;i++) res[i]=ws[0]+i;
		return res;
	}

	public String toString() {
		return Arrays.toString(word2node);
	}

	public static String getBroadPOS(String pos) {
		int i=pos.indexOf(':');
		if (i>=0) return pos.substring(0,i);
		return pos;
	}

	String[] calcFeats(int w, int h, int stage0, int nodew, int nodeh, String prevAct) {
//		return calcFeatsBest(w, h, stage0, nodew, nodeh, prevAct);
		return calcFeatsRevu(w, h, stage0, nodew, nodeh, prevAct);
	}
	String[] calcFeatsRevu(int w, int h, int stage0, int nodew, int nodeh, String prevAct) {
		ArrayList<String> feats = new ArrayList<String>();

		{
			// candidats gauche
			if (nodew>0) {
				for (int x : getWordsInNode(nodew-1)) {
					if (!checkProjectivity(g, w, x)) continue;
					if (h<0||x==h||nodeh==nodew+1) {
						feats.add("JointPWithL"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
						feats.add("JointBWithL"+getBroadPOS(g.getMot(w).getPOS())+"-"+getBroadPOS(g.getMot(x).getPOS()));
						feats.add("JointFWithL"+g.getMot(w).getForme()+"-"+g.getMot(x).getForme());
						feats.add("JointLWithL"+g.getMot(w).getLemme()+"-"+g.getMot(x).getLemme());
					}
				}
			} else feats.add("NOLEFT");
			if (nodew>1) {
				for (int x : getWordsInNode(nodew-2)) {
					if (!checkProjectivity(g, w, x)) continue;
					if (h<0||x==h||nodeh==nodew+1) {
						feats.add("JointPWithLL"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
						feats.add("JointBWithLL"+getBroadPOS(g.getMot(w).getPOS())+"-"+getBroadPOS(g.getMot(x).getPOS()));
						feats.add("JointFWithLL"+g.getMot(w).getForme()+"-"+g.getMot(x).getForme());
						feats.add("JointLWithLL"+g.getMot(w).getLemme()+"-"+g.getMot(x).getLemme());
					}
				}
			} else feats.add("NOLLEFT");
		}
		{
			// candidats droite
			if (nodew<getNnodes()-1) {
				for (int x : getWordsInNode(nodew+1)) {
					if (!checkProjectivity(g, w, x)) continue;
					if (h<0||x==h||nodeh==nodew-1) {
						feats.add("JointPWithR"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
						feats.add("JointBWithR"+getBroadPOS(g.getMot(w).getPOS())+"-"+getBroadPOS(g.getMot(x).getPOS()));
						feats.add("JointFWithR"+g.getMot(w).getForme()+"-"+g.getMot(x).getForme());
						feats.add("JointLWithR"+g.getMot(w).getLemme()+"-"+g.getMot(x).getLemme());
					}
				}
			} else feats.add("NORIGHT");
			if (nodew<getNnodes()-2) {
				for (int x : getWordsInNode(nodew+2)) {
					if (!checkProjectivity(g, w, x)) continue;
					if (h<0||x==h||nodeh==nodew-1) {
						feats.add("JointPWithRR"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
						feats.add("JointBWithRR"+getBroadPOS(g.getMot(w).getPOS())+"-"+getBroadPOS(g.getMot(x).getPOS()));
						feats.add("JointFWithRR"+g.getMot(w).getForme()+"-"+g.getMot(x).getForme());
						feats.add("JointLWithRR"+g.getMot(w).getLemme()+"-"+g.getMot(x).getLemme());
					}
				}
			} else feats.add("NORRIGHT");
		}

		return feats.toArray(new String[feats.size()]);
	}

	String[] calcFeatsBest(int w, int h, int stage0, int nodew, int nodeh, String prevAct) {
		ArrayList<String> feats = new ArrayList<String>();

		if (false) {
			// transition features
			String typeAct = "N";
			if (prevAct.startsWith("LB")) typeAct="LB";
			else if (prevAct.startsWith("RB")) typeAct="RB";
			else typeAct = ""+prevAct.charAt(0);
			int i=prevAct.indexOf("2w");
			int headWord = -1;
			if (i>=0) {
				headWord=Integer.parseInt(prevAct.substring(i+2));
				prevAct=prevAct.substring(0,i);
			}
			String dep="NODEP";
			prevAct=prevAct.substring(2).trim();
			if (prevAct.length()>0) dep=prevAct;
			
			feats.add("PACTyp"+typeAct);
			feats.add("PACDep"+dep);
			if (headWord>=0) {
				// equivalent du head(stack(0)) de Nivre
				feats.add("PACTHead"+g.getMot(headWord).getForme());
			}
		}

		{
			final int maxn = 10;
			int nn = nodew-1;
			if (nn>maxn) nn=maxn;
			feats.add("NNODESL"+nn);
			nn = getNnodes()-nodew-1;
			if (nn>maxn) nn=maxn;
			feats.add("NNODESR"+nn);
		}

		// features du gouverné
		feats.add("FW"+g.getMot(w).getForme());
		feats.add("LW"+g.getMot(w).getLemme());
		feats.add("PW"+g.getMot(w).getPOS());

		//		feats.add("FPW"+g.getMot(w).getLemme()+"-"+g.getMot(w).getPOS());
		// tous les mots du sous-arbre courant: peut etre utile pour choisir lorsque le root est une prep par exemple !
		for (int x : getWordsInNode(nodew)) {
			if (x!=w) {
				feats.add("CURF"+g.getMot(x).getForme());
				feats.add("CURP"+g.getMot(x).getPOS());
			}
		}

		// features du gouverneur gauche
		if (nodew>0) {
			for (int x : getWordsInNode(nodew-1)) {
				if (!checkProjectivity(g, w, x)) continue;
				if (h<0||x==h||nodeh==nodew+1) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("LEFTW"+g.getMot(x).getForme());
					feats.add("LEFTL"+g.getMot(x).getLemme());
					feats.add("LEFTP"+g.getMot(x).getPOS());
					feats.add("LEFTPD"+g.getMot(x).getPOS()+"-"+dist);
					feats.add("JOINTLEFTP"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
					feats.add("JOINTLEFTW"+g.getMot(w).getLemme()+"-"+g.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew-1);
			feats.add("LEFTROOTP"+g.getMot(root).getPOS());
			//			feats.add("LEFTROOTF"+g.getMot(root).getForme());
			feats.add("LEFTROOTL"+g.getMot(root).getLemme());
			feats.add("RMOSTP"+g.getMot(g.getRightmostNode(root)).getPOS());
		} else feats.add("NOLEFT");
		
		if (true&&nodew>1) {
			for (int x : getWordsInNode(nodew-2)) {
				if (!checkProjectivity(g, w, x)) continue;
				if (h<0||x==h||nodeh!=nodew-2) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("LLEFTW"+g.getMot(x).getForme());
					feats.add("LLEFTL"+g.getMot(x).getLemme());
					feats.add("LLEFTP"+g.getMot(x).getPOS());
					feats.add("LLEFTPD"+g.getMot(x).getPOS()+"-"+dist);
					feats.add("LJOINTLEFTP"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
					feats.add("LJOINTLEFTW"+g.getMot(w).getLemme()+"-"+g.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew-2);
			feats.add("LLEFTROOTP"+g.getMot(root).getPOS());
			//			feats.add("LEFTROOTF"+g.getMot(root).getForme());
			feats.add("LLEFTROOTL"+g.getMot(root).getLemme());
			feats.add("LRMOSTP"+g.getMot(g.getRightmostNode(root)).getPOS());
		} else feats.add("NOLLEFT");

		// features du gouverneur droit
		if (nodew<getNnodes()-1) {
			for (int x : getWordsInNode(nodew+1)) {
				if (!checkProjectivity(g, w, x)) continue;
				if (h<0||x==h||nodeh==nodew-1) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("RIGHTW"+g.getMot(x).getForme());
					feats.add("RIGHTL"+g.getMot(x).getLemme());
					feats.add("RIGHTP"+g.getMot(x).getPOS());
					feats.add("RIGHTPD"+g.getMot(x).getPOS()+"-"+dist);
					feats.add("JOINTRIGHTP"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
					feats.add("JOINTRIGHTW"+g.getMot(w).getLemme()+"-"+g.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew+1);
			feats.add("RIGHTROOTP"+g.getMot(root).getPOS());
			//			feats.add("RIGHTROOTF"+g.getMot(root).getForme());
			feats.add("RIGHTROOTL"+g.getMot(root).getLemme());
			feats.add("LMOSTP"+g.getMot(g.getLeftmostNode(root)).getPOS());
		} else feats.add("NORIGHT");
		
		if (true&&nodew<getNnodes()-2) {
			for (int x : getWordsInNode(nodew+2)) {
				if (!checkProjectivity(g, w, x)) continue;
				if (h<0||x==h||nodeh!=nodew+2) {
					int dist=Math.abs(x-w);
					if (dist>=3) dist=3;
					feats.add("RRIGHTW"+g.getMot(x).getForme());
					feats.add("RRIGHTL"+g.getMot(x).getLemme());
					feats.add("RRIGHTP"+g.getMot(x).getPOS());
					feats.add("RRIGHTPD"+g.getMot(x).getPOS()+"-"+dist);
					feats.add("RJOINTRIGHTP"+g.getMot(w).getPOS()+"-"+g.getMot(x).getPOS());
					feats.add("RJOINTRIGHTW"+g.getMot(w).getLemme()+"-"+g.getMot(x).getPOS());
				}
			}
			int root=getNodeRoot(nodew+2);
			feats.add("RRIGHTROOTP"+g.getMot(root).getPOS());
			//			feats.add("RIGHTROOTF"+g.getMot(root).getForme());
			feats.add("RRIGHTROOTL"+g.getMot(root).getLemme());
			feats.add("RLMOSTP"+g.getMot(g.getLeftmostNode(root)).getPOS());
		} else feats.add("NORRIGHT");

		// features indep des noeuds
		int uttlen = g.getNbMots()%5;
		if (uttlen>30) uttlen=30;
		feats.add("UTTLEN"+uttlen);

		int stage = stage0;
		if (stage>=6) stage=6;
		if (w>0) {
			feats.add("PFW"+g.getMot(w-1).getForme()+"S"+stage);
			feats.add("PPW"+g.getMot(w-1).getPOS()+"S"+stage);
		}
		if (w<g.getNbMots()-1) {
			feats.add("NFW"+g.getMot(w+1).getForme()+"S"+stage);
			feats.add("NPW"+g.getMot(w+1).getPOS()+"S"+stage);
		}
		if (w>1) {
			feats.add("PPPW"+g.getMot(w-2).getPOS()+"S"+stage);
		}
		if (w<g.getNbMots()-2) {
			feats.add("NNPW"+g.getMot(w+2).getPOS()+"S"+stage);
		}
		if (w>2) {
			feats.add("PPPPW"+g.getMot(w-3).getPOS()+"S"+stage);
		}
		if (w<g.getNbMots()-3) {
			feats.add("NNNPW"+g.getMot(w+3).getPOS()+"S"+stage);
		}

		return feats.toArray(new String[feats.size()]);
	}

	static void calcErrors(List<DetGraph> grecs, String reffile) {
		ArrayList<int[]> marks = new ArrayList<int[]>();
		GraphIO gio = new GraphIO(null);
		List<DetGraph> grefs = gio.loadAllGraphs(reffile);
		int ndel=0,nins=0,nsub=0,nhead=0,nok=0,ntot=0;
		try {
			PrintWriter detailedRes = FileUtils.writeFileUTF("detailed.res");
			for (int i=0;i<grefs.size();i++) {
				DetGraph gref = grefs.get(i);
				// debug: ne score pas les longues phrases !
				//if (maxlen2score>=0&&gref.getNbMots()>=maxlen2score) continue;

				DetGraph grec = grecs.get(i);
				ArrayList<Integer> errwords = new ArrayList<Integer>(); 
				for (int recmot=0;recmot<grec.getNbMots();recmot++) {
					int drec = grec.getDep(recmot);
					int dref = gref.getDep(recmot);

					// debug: ne score pas les longues deps !
					if (maxlen2score>=0&&drec>=0&&Math.abs(recmot-grec.getHead(drec))>maxlen2score) continue;

					ntot++;
					if (drec<0&&dref>=0) {
						errwords.add(recmot);
						ndel++;
						detailedRes.println(0);
					} else if (drec>=0&&dref<0) {
						errwords.add(recmot);
						nins++;
						detailedRes.println(0);
					} else if (drec>=0&&dref>=0) {
						int href = gref.getHead(dref);
						int hrec = grec.getHead(drec);
						if (href!=hrec) {
							errwords.add(recmot);
							nhead++;
							detailedRes.println(0);
						} else {
							if (gref.getDepLabel(dref).equals(grec.getDepLabel(drec))) {
								nok++;
								detailedRes.println(1);
							} else {
								errwords.add(recmot);
								nsub++;
								detailedRes.println(0);
							}
						}
					} else if (drec<0&&dref<0) {
						nok++;
						detailedRes.println(1);
					}
				}
				for (int j=0;j<errwords.size();j++) {
					int[] mark = {0,i,errwords.get(j)};
					marks.add(mark);
				}
			}
			detailedRes.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("acc counts: ntot="+ntot+" nok="+nok+" nsub="+nsub+" nhead="+nhead+" ndel="+ndel+" nins="+nins);
		float las = (float)nok/(float)ntot;
		System.out.println("LAS="+las);
		try {
			PrintWriter fout = new PrintWriter(new FileWriter("accout.log", true));
			fout.println("acc counts: "+ntot+" "+nok+" "+nsub+" "+nhead+" "+ndel+" "+nins);
			fout.println("LAS="+las);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayList<List<DetGraph>> gsx = new ArrayList<List<DetGraph>>();
		gsx.add(grecs); gsx.add(grefs);
		JSafran.save(gsx, "errors.xml", null, marks);
	}

	public static void parse(List<DetGraph> gs) {
		selectionModel = new SelectionModel("maxentdatModel.txt");
		ArrayList<HMM> hmmPerStage = new ArrayList<HMM>();
		for (int i=0;i<nHMMs;i++) {
			hmmPerStage.add(HMM.load("detmod",i));
		}

		for (DetGraph g : gs) {
			float fullprob = 1f;
			System.out.println("UTT "+g);
			g.clearDeps();
			NodesSeq seq = new NodesSeq(g);
			int stage=0;
			for (int hmmidx=0;;stage++) {
				if (hmmidx>0) {
					// seulement a stage 0 !!
					forcedActions=null;
				}
				String[] acts = hmmPerStage.get(hmmidx).decode(g,seq,stage);
				if (acts==null) break;
				fullprob*=hmmPerStage.get(hmmidx).finalProb;

				List<Integer> newdeps = HMM.applyActions(g, seq, acts);
				seq.mergeNodes(acts);
				if (seq.getNnodes()<=1) break;
				if (hmmidx<nHMMs-1) hmmidx++;
			}
			System.out.println("NSTAGES "+stage+" NWORDS "+g.getNbMots()+" "+fullprob);
			g.conf=fullprob;
		}
	}
	
	static void test(String testfile) {
		selectionModel = new SelectionModel("maxentdatModel.txt");

		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(testfile);
		ArrayList<HMM> hmmPerStage = new ArrayList<HMM>();
		for (int i=0;i<nHMMs;i++) {
			if (false&&i==0) hmmPerStage.add(HMM.load("unsupmod",i));
			else
				hmmPerStage.add(HMM.load("detmod",i));
		}
		if (debugnode>=0) hmmPerStage.get(0).debugNode=debugnode;

		for (DetGraph g : gs) {
			float fullprob = 1f;
			System.out.println("UTT "+g);
			DetGraph gg = g.getSubGraph(0);
			g.clearDeps();
			NodesSeq seq = new NodesSeq(g);
			int stage=0;
			for (int hmmidx=0;;stage++) {
				if (hmmidx>0) {
					// seulement a stage 0 !!
					forcedActions=null;
				}
				String[] acts = hmmPerStage.get(hmmidx).decode(g,seq,stage);
				if (acts==null) break;
				fullprob*=hmmPerStage.get(hmmidx).finalProb;

				List<Integer> newdeps = HMM.applyActions(g, seq, acts);

				// debug pour savoir si je peux gagner en ameliorant la selection du head dans un sous-arbre
				if (false) {
					for (int z:newdeps) {
						int d=g.getDep(z);
						int hrec = g.getHead(d);
						String labrec = g.getDepLabel(d);
						d=gg.getDep(z);
						if (d>=0) {
							int href = gg.getHead(d);
							String labref = gg.getDepLabel(d);
							int[] wordsInNode = seq.getWordsInNode(seq.word2node[hrec]);
							Arrays.sort(wordsInNode);
							if (labrec.equals(labref) && hrec!=href &&
									Arrays.binarySearch(wordsInNode, href)>=0) {
								// erreur de choix du word head
								// on ne corrige que si ca ne cree pas de non-proj
								DetGraph g3 = g.getSubGraph(0);
								d=g3.getDep(z);
								g3.removeDep(d);
								if (seq.checkProjectivity(g3, z, href)) {
									d=g.getDep(z);
									g.removeDep(d);
									g.ajoutDep(labrec, z, href);
								}
							}
						}
					}
				}

				// debug pour savoir ce que je peux gagner au max en me focalisant sur le 1er stage:
				if (false) {
					if (stage==0) {
						for (int z:newdeps) {
							int d=g.getDep(z);
							int d2=gg.getDep(z);
							g.removeDep(d);
							acts[seq.word2node[z]]="NA";
							if (d2>=0) {
								int href = gg.getHead(d2);
								int delta = seq.word2node[href]-seq.word2node[z];
								if (delta==1||delta==-1) {
									String labref = gg.getDepLabel(d2);
									g.ajoutDep(labref, z, href);
									if (delta==1)
										acts[seq.word2node[z]]="LA"+labref+"2w"+href;
									else
										acts[seq.word2node[z]]="RA"+labref+"2w"+href;
								}
							}
						}
					}
				}

				DetGraph[] xgs = {g,gg};
//				JSafran.viewGraph(xgs);
				seq.mergeNodes(acts);
				if (seq.getNnodes()<=1) break;
				if (hmmidx<nHMMs-1) hmmidx++;
			}
			System.out.println("NSTAGES "+stage+" NWORDS "+g.getNbMots()+" "+fullprob);
			g.conf=fullprob;
		}
		gio.save(gs, "rec.xml");

		calcErrors(gs,testfile);
	}

	/**
	 * Warning: can only be used for the previous or next subtrees, as they are both full trees
	 * and there are no root nodes in between.
	 * OK, I have improved it so that it only checks the words that are within the HEAD subtree,
	 * as the (eventual) intermediate subtrees are linked anyway to either left or right...
	 * 
	 * @param g
	 * @param w
	 * @param h
	 * @return
	 */
	public boolean checkProjectivity(DetGraph g, int w, int h) {
		int nodehead = word2node[h];
		int[] ws = getWordsInNode(nodehead);
		Arrays.sort(ws);
		if (w<h) {
			for (int j=ws[0];j<h;j++) {
				int d=g.getDep(j);
				if (d<0) return false;
				int hh=g.getHead(d);
				if (hh<w||hh>h) return false;
			}
		} else {
			for (int j=h+1;j<=ws[ws.length-1];j++) {
				int d=g.getDep(j);
				if (d<0) return false;
				int hh=g.getHead(d);
				if (hh<h||hh>w) return false;
			}
		}
		return true;
	}

	/*
	 * priors venant du oracle: nNA=33899 nLA=17511 nRA=27160
	 */
	static void train(String trainfile, int niters) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(trainfile);
		train(gs,niters);
	}
	public static void train(List<DetGraph> gs, int niters) {
		
		selectionModel = new SelectionModel();
		int nna=0, nla=0, nra=0;
		int nUttWithSeveralTrees=0;
		ArrayList<HMM> hmmPerStage = new ArrayList<HMM>();
		for (DetGraph g : gs) {
			DetGraph gg = g.getSubGraph(0);
			gg.clearDeps();
			NodesSeq seq = new NodesSeq(g);
			NodesSeq gseq = new NodesSeq(gg);
			if (seq.getNnodes()==0) gseq=new NodesSeq();
			for (int stage=0, hmmidx=0;;stage++) { // pour tous les stages
				while (hmmPerStage.size()<=hmmidx) hmmPerStage.add(new HMM(hmmPerStage.size()));
				HMM hmm = hmmPerStage.get(hmmidx);

				String[] acts = HMM.calcOracle(seq);
				
				System.out.println("oracle acts: "+Arrays.toString(acts));
				if (acts==null) {
					// plus d'action liante ! on doit donc avoir des multiple roots ?!
					nUttWithSeveralTrees++;
					break;
				}
				selectionModel.accumulate(gseq,acts);

				// stats
				for (String a : acts)
					switch (a.charAt(0)) {
					case 'N': nna++; break;
					case 'L': nla++; break;
					case 'R': nra++; break;
					}

				for (int node=0;node<seq.getNnodes();node++) {
					String prevact="NONE";
					if (node>0) prevact = acts[node-1];
					String[] feats = gseq.calcFeats(gseq.getNodeRoot(node),-1,stage,node,-1,prevact);
					hmm.accumulate(feats,acts[node]);
				}

				seq.mergeNodes(acts);
				HMM.applyActions(gg, gseq, acts);
				gseq.word2node=Arrays.copyOf(seq.word2node, seq.word2node.length);

				if (seq.getNnodes()<=1) break;

				if (hmmidx<nHMMs-1) hmmidx++; // je cree un HMM pour les N premiers stages, puis un seul pour les suivants
			}
		}
		selectionModel.finalizeTraining();
		for (HMM hmm : hmmPerStage) hmm.trainHMM("detmod", niters);
		System.out.println("end of training:");
		System.out.println("nb de phrases avec plusieurs arbres = "+nUttWithSeveralTrees+" / "+gs.size());
		System.out.println("nb de dependances supprimees = "+ndepdel);
		System.out.println("nb de phrases proj / non-porj = "+proj+" "+nonproj);
		System.out.println("nb d'actions oracles NA, LA et RA: "+nna+" "+nla+" "+nra);
	}

	public static String[] forcedActions = null;

	private static void forceActions(String file) {
		try {
			BufferedReader f = FileUtils.openFileUTF(file);
			String s = f.readLine();
			f.close();
			String[] ss = s.split(" ");
			forcedActions=ss;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static int debugnode=-1;
	
	public static void main(String args[]) throws Exception {
		final String testfile1 = "../jsafran/traintest.xml";
		final String testfile2 = "../jsafran/test2009.xml";
		final String testfile3 = "/home/xtof/corpus/P7_DEPENDENCY_TREEBANK/p7_test_utf.conll";
		final String trainfile1 = "/home/xtof/corpus/P7_DEPENDENCY_TREEBANK/p7_train_utf.conll";
		final String trainfile2 = "../jsafran/train2011.xml";

		String testfile = testfile2, trainfile = trainfile2;
		int niters = 100;

		boolean dotrain = true;
		
		for (int i=0;i<args.length;i++) {
			if (args[i].equals("-train")) {
				trainfile = args[++i];
			} else if (args[i].equals("-notrain")) {
				dotrain=false;
			} else if (args[i].equals("-test")) {
				testfile = args[++i];
			} else if (args[i].equals("-force")) {
				forceActions(args[++i]);
			} else if (args[i].equals("-debug")) {
				debugnode=Integer.parseInt(args[++i]);
				System.out.println("debugnode: "+debugnode);
			} else if (args[i].equals("-niters")) {
				niters = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-calcacc")) {
				GraphIO gio = new GraphIO(null);
				maxlen2score = Integer.parseInt(args[++i]);
				List<DetGraph> grec = gio.loadAllGraphs(args[++i]);
				calcErrors(grec, args[++i]);
				System.exit(1);
			}
		}

		if (true) {
			if (dotrain) train(trainfile,niters);
			test(testfile);
			//			test(testfile1);
		} else {
			// calcule les detailed res pour reco du MaltParser
			GraphIO gio = new GraphIO(null);
			List<DetGraph> gs = gio.loadAllGraphs("../jsafran/train2011.xml");
			ArrayList<int[]> marks = new ArrayList<int[]>();
			for (int i=0;i<gs.size();i++) {
				DetGraph g =gs.get(i);
				List<Integer> bads = countNonProjectivity(g, false);
				for (int b : bads) {
					int[] mark = {0,i,b};
					marks.add(mark);
				}
			}
			ArrayList<List<DetGraph>> gsx = new ArrayList<List<DetGraph>>();
			gsx.add(gs);
			JSafran.save(gsx, "bads.xml", null, marks);
			//			List<DetGraph> gs = gio.loadAllGraphs("../jsafran/maltrec.conll");
			//			calcErrors(gs, "../jsafran/test2009.xml");
		}
	}
}
