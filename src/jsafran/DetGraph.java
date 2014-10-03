package jsafran;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import corpus.text.TextSegments;

import utils.DeepEquals;
import utils.SuiteDeMots;

/**
 * cette classe code un graphe syntaxique en dependances en memoire
 * (de maniere a rendre independent la structure du graphe d'un format particulier,
 * comme celui de Syntex, ou celui de CONLL)
 * 
 * cette classe depend de Dep qui contient l'ensemble des dependances possibles
 * et de Mot qui contient la liste des mots de la phrase
 * 
 * @author cerisara
 *
 */
public class DetGraph implements Serializable {
	private static final long serialVersionUID = 1L;

	// syntax first, semantic second
	public List<DetGraph> relatedGraphs = null;
	
	public HashMap<Integer,Mot> mots = new HashMap<Integer,Mot>();
	public ArrayList<Dep> deps = new ArrayList<Dep>();

	// TODO: mettre les groups en private
	public ArrayList<ArrayList<Mot>> groups = null;
	public ArrayList<String> groupnoms = null;

	int[] relTypes;
	public String sent, comment=null;
	public float conf=Float.NaN;
	public int cursent=0;

	private URL source = null;

	HashSet<Integer> constraints = new HashSet<Integer>();

	public void setSource(URL src) {source=src;}
	public URL getSource() {return source;}

	public DetGraph() {}
	public DetGraph(String[] mots) {
		for (int i=0;i<mots.length;i++) {
			String w = mots[i];
			addMot(i, new Mot(w,w,"UNK"));
		}
	}
	
	public int countNonProjectiveArcs() {
		int ncrosscut=0;
		for (int i=0;i<getNbMots();i++) {
			int d=getDep(i);
			if (d>=0) {
				int h=getHead(d);
				if (h>i) {
					for (int j=i+1;j<h;j++) {
						d=getDep(j);
						if (d>=0) {
							int hh=getHead(d);
							if (hh>h||hh<i) ncrosscut++;
						} else ncrosscut++;
					}
				} else {
					for (int j=i-1;j>h;j--) {
						d=getDep(j);
						if (d>=0) {
							int hh=getHead(d);
							if (hh>i||hh<h) ncrosscut++;
						} else ncrosscut++;
					}
				}
			}
		}
		return ncrosscut;
	}
	
	public void clearGroups() {
		if (groups!=null) {
			groups.clear();
			groupnoms.clear();
		}
	}
	// fast methods for accessing groups
	public int getNbGroups() {
		if (groups==null) return 0;
		return groups.size();
	}
	public String getGroupName(int i) {
		return groupnoms.get(i);
	}
	public List<String> getGroupNoms(int w) {
		ArrayList<String> res = new ArrayList<String>();
		if (groups==null||groups.size()==0) return res;
		Mot m = getMot(w);
		for (int i=0;i<groups.size();i++) {
			ArrayList<Mot> mts = groups.get(i);
			if (mts.contains(m)) res.add(groupnoms.get(i));
		}
		return res;
	}
	public void addgroup(int motdeb, int motfin, String gnom) {
            int a=motdeb,b=motfin;
            if (a>b) {
                a=b; b=motdeb;
            }
		if (groups==null) {
			groups = new ArrayList<ArrayList<Mot>>();
			groupnoms = new ArrayList<String>();
		}
		ArrayList<Mot> motsdugroup = new ArrayList<Mot>();
		for (int i=a;i<=b;i++) motsdugroup.add(getMot(i));
		// on verifie qu'il n'y a pas deja un groupe
		for (int j=0;j<groups.size();j++)
			if (DeepEquals.deepEquals(motsdugroup, groups.get(j))) {
				if (gnom.equals(groupnoms.get(j))) return;
			}
		groups.add(motsdugroup);
		groupnoms.add(gnom);
	}
	public int[] getGroupsThatStartHere(int mot) {
		if (groups==null) {
			return null;
		}
		ArrayList<Integer> gr = new ArrayList<Integer>();
		Mot m = getMot(mot);
		for (int i=0;i<groups.size();i++) {
			if (groups.get(i).get(0)==m) gr.add(i);
		}
		int[] r = new int[gr.size()];
		for (int z=0;z<r.length;z++) r[z]=gr.get(z);
		return r;
	}
	public int[] getGroupsThatEndHere(int mot) {
		if (groups==null) {
			return null;
		}
		ArrayList<Integer> gr = new ArrayList<Integer>();
		Mot m = getMot(mot);
		for (int i=0;i<groups.size();i++) {
			if (groups.get(i).get(groups.get(i).size()-1)==m) gr.add(i);
		}
		int[] r = new int[gr.size()];
		for (int z=0;z<r.length;z++) r[z]=gr.get(z);
		return r;
	}
	public List<Integer> getGroups(Mot m) {
		ArrayList<Integer> gr = new ArrayList<Integer>();
		if (groups==null) return gr;
		for (int i=0;i<groups.size();i++) {
			if (groups.get(i).contains(m)) gr.add(i);
		}
		return gr;
	}
	public int[] getGroups(int mot) {
		if (groups==null) {
			return null;
		}
		ArrayList<Integer> gr = new ArrayList<Integer>();
		Mot m = getMot(mot);
		for (int i=0;i<groups.size();i++) {
			if (groups.get(i).contains(m)) gr.add(i);
		}
		int[] r = new int[gr.size()];
		for (int z=0;z<r.length;z++) r[z]=gr.get(z);
		return r;
	}
	public List<Mot> getGroupAllMots(int i) {
		return groups.get(i);
	}
	public Mot getGroupFirstMot(int i) {
		return groups.get(i).get(0);
	}

	/**
	 * test si 2 graphes sont les memes _au niveau des dependances+POStag_
	 */
	@Override
	public boolean equals(Object g) {
		DetGraph gg = (DetGraph)g;
		if (getNbMots()!=gg.getNbMots()) return false;
		for (int i=0;i<getNbMots();i++) {
			if (!getMot(i).getPOS().equals(gg.getMot(i).getPOS())) return false;
			int depa=getDep(i);
			int depb=gg.getDep(i);
			if (depa*depb<0) return false;
			if (depa>=0) {
				int heada=getHead(depa);
				int headb=gg.getHead(depb);
				if (heada!=headb) return false;
				if (!getDepLabel(depa).equals(gg.getDepLabel(depb))) return false;
			}
		}
		return true;
	}

	public String[] getMots() {
		String[] ss = new String[getNbMots()];
		for (int i=0;i<getNbMots();i++) {
			ss[i] = getMot(i).getForme();
		}
		return ss;
	}

        //works with no trees and return an ordered list
        private void getAncestorsOrdered0(int node,List<Integer> dejavu) {
		dejavu.add(node);
		int[] deps = getDeps(node);
		for (int i=0;i<deps.length;i++) {
			int h = getHead(deps[i]);
			if (!dejavu.contains(h)) getAncestorsOrdered0(h,dejavu);
		}
        }
	public List<Integer> getAncestorsOrdered(int node) {
            List<Integer> dejavu = new ArrayList<Integer>();
            getAncestorsOrdered0(node,dejavu);
            List<Integer> list = new ArrayList<Integer>(dejavu);
            return list;
	}

	public int checkCycles() {
		for (int i=0;i<getNbMots();i++) {
			HashSet<Integer> dejavus = new HashSet<Integer>();
			for (int h=i;;) {
				if (dejavus.contains(h)) return h;
				dejavus.add(h);
				int d=getDep(h);
				if (d<0) break;
				h=getHead(d);
			}
		}
		return -1;
	}
	
//Todo: see I think this version of getAncestors works with graph (no trees), and the other only with trees...
//        private Set<Integer> getAncestors0(int node,Set<Integer> dejavu) {
//		dejavu.add(node);
//		int[] deps = getDeps(node);
//		for (int i=0;i<deps.length;i++) {
//			int h = getHead(deps[i]);
//			if (!dejavu.contains(h)) dejavu.addAll(getAncestors0(h,dejavu));
//		}
//                return dejavu;
//        }
//	public List<Integer> getAncestors(int node) {
//            Set<Integer> dejavu = new HashSet<Integer>();
//            dejavu=getAncestors0(node,dejavu);
//            List<Integer> list = new ArrayList<Integer>(dejavu);
//            return list;
//	}

	public List<Integer> getAncestors(int node) {
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(node);
		int[] deps = getDeps(node);
		for (int i=0;i<deps.length;i++) {
			int h = getHead(deps[i]);
			if (l.contains(h)) continue;
			List<Integer> ll = getAncestors(h);
			l.addAll(ll);
		}
		return l;
	}

	public int getDepth(int mot) {
		int dmax=0;
		List<Integer> fils = getFils(mot);
		for (int f : fils) {
			int d=getDep(f);
			if (d>dmax) dmax=d;
		}
		return 1+dmax;
	}

	public int[] getRoots() {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i=0;i<getNbMots();i++)
			if (getDeps(i).length==0) res.add(i);
		int[] r = new int[res.size()];
		for (int i=0;i<r.length;i++) r[i]=res.get(i);
		return r;
	}

	public static DetGraph getErreurGraph() {
		Mot m = new Mot("ERROR",0);
		DetGraph g = new DetGraph();
		g.addMot(0, m);
		return g;
	}

	public void copyDepsFrom(DetGraph g) {
		SuiteDeMots s0 = new SuiteDeMots(g.getMots());
		SuiteDeMots s1 = new SuiteDeMots(getMots());
		s0.align(s1);
		deps.clear();
		for (int i=0;i<s0.getNmots();i++) {
			int[] deps = g.getDeps(i);
			if (deps.length==0) continue;
			int[] w = s0.getLinkedWords(i);
			for (int j=0;j<w.length;j++) {
				int head0 = g.getHead(deps[j]);
				int[] heads1 = s0.getLinkedWords(head0);
				if (heads1.length>0) {
					ajoutDep(g.getDepLabel(deps[j]), w[j], heads1[0]);
				}
			}
		}
	}

	public void insertMot(int pos, Mot m) {
		for (int i=mots.size()-1;i>=pos;i--) {
			Mot mo = mots.get(i);
			mo.setIndexInUtt(mo.getIndexInUtt()+1);
			mots.put(i+1, mo);
		}
		addMot(pos, m);
	}

	public String printGroups() {
		if (groups==null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<groups.size();i++) {
			sb.append(groupnoms.get(i));
			sb.append(" : [");
			sb.append(groups.get(i).get(0).getForme());
			sb.append(" .. ");
			sb.append(groups.get(i).get(groups.get(i).size()-1).getForme());
			sb.append("] ");
		}
		return sb.toString();
	}
	public String printDeps() {
		StringBuilder sb = new StringBuilder();
		for (Dep d : deps) {
			sb.append(d.getHead().getForme()+"°-"+d.getGov().getForme()+" ");
		}
		return sb.toString();
	}
	public String toString() {
		String s = "";
		for (int i=0;i<getNbMots();i++)
			s+=getMot(i).getForme()+" ";
		return s;
	}

	public void paintGraphToLatex(int idx, PrintWriter f) {
		float alpha = 0.2f;
		float beta = 0.7f;

		String sidx = idx+": ";
		// on n'affiche pas le numero de la phrase s'il est <0
		if (idx<0) sidx="";

		// nb de mots
		int nmots = getNbMots();
		// nb de chars et milieu des mots
		float[] mids = new float[nmots];
		float[] ends = new float[nmots];
		float nchars = sidx.length();
		nchars += getMot(0).getForme().length()+1;
		mids[0] = nchars/2;
		ends[0] = nchars;
		for (int i=1;i<nmots;i++) {
			int motw = getMot(i).getForme().length();
			mids[i] = nchars+motw/2+1;
			ends[i] = nchars+motw+1;
			nchars+=motw+1;
		}
		System.err.println("latex: "+nchars);
		// arc le + long en chars
		float maxArcLen = 0;
		for (int i=0;i<nmots;i++) {
			int dep = getDep(i);
			if (dep>=0) {
				float len = Math.abs(mids[getHead(dep)]-mids[i]);
				if (len>maxArcLen) maxArcLen=len;
			}
		}
		float maxheight = 6f+(float)maxArcLen*beta;

		f.println("\\hspace{-7cm}");
		f.println("\\begin{picture}("+nchars+","+maxheight+")");
		f.println("\\put(0,0){"+sidx+"}");
		String m = getMot(0).getForme().replace("%", "\\%").replaceAll("_", "");
		f.println("\\put("+sidx.length()+",0){"+m+"}");
		for (int i=1;i<nmots;i++) {
			m = getMot(i).getForme().replace("%", "\\%").replaceAll("_", "");
			f.println("\\put("+ends[i-1]+",0){"+m+"}");
		}
		for (int i=0;i<nmots;i++) {
			int dep = getDep(i);
			if (dep<0) continue;
			int head = getHead(dep);
			float d = Math.abs(mids[i]-mids[head]);
			float y1 = 1f+d*alpha;
			float y2 = 1f+d*beta;
			float xm = (float)(mids[i]+mids[head])/2f;
			f.println("\\qbezier("+mids[i]+","+y1+")("+xm+","+y2+")("+mids[head]+","+(y1+0.1)+")");
			f.println("\\put("+(mids[head]-0.1)+","+y1+"){\\circle*{0.6}}");
			String lab = getDepLabel(dep).replaceAll("_", "");
			y1+=1;
			if (head<i)
				f.println("\\put("+(mids[i]-1.2)+","+y1+"){\\rotatebox{90}{\\scalebox{0.8}{"+lab+"}}}");
			else
				f.println("\\put("+(mids[i]+0.3)+","+y1+"){\\rotatebox{90}{\\scalebox{0.8}{"+lab+"}}}");
		}
		f.println("\\end{picture}");
	}

	public void clear() {
		mots.clear();
		deps.clear();
	}
	public void clearDeps() {
		deps.clear();
	}
	public void clearAutoDeps() {
		for (int i=0;i<getNbMots();i++) {
			int[] dep = getDeps(i);
			if (dep.length>0) {
				if (!isConstrainted(i)) {
					for (int d : dep) {
						removeDep(d);
					}
				}
			}
		}
	}

	public String getSentence() {
		String s="";
		for (int i=0;i<getNbMots();i++) {
			Mot m = getMot(i);
			String forme = m.getForme();
			s+=forme+" ";
		}
		return s;
	}

	/**
	 * si on autorise plusieurs heads, alors conserve toutes les deps
	 * sinon, conserve la dep sortante depuis le 1er mot
	 * 
	 * TODO: rassembler les TextSegments
	 * 
	 * @param w
	 */
	public void mergeTwoWords(int w, boolean allowMultipleHeads, char separator) {
		if (w>=getNbMots()-1) return;
		Mot m1 = mots.get(w);
		Mot m2 = mots.get(w+1);
		m1.setForme(m1.getForme()+separator+m2.getForme());
		m1.setlemme(m1.getLemme()+separator+m2.getLemme());
		// decale les mots suivants
		int n = getNbMots();
		for (int i=w+2;i<n;i++) {
			Mot m = mots.get(i);
			m.setIndexInUtt(m.getIndexInUtt()-1);
			mots.put(i-1, m);
		}
		mots.remove(n-1);
		// fix les dep
		int depSortanteMot2 = -1;
		int depSortanteMot1 = -1;
		for (int i=deps.size()-1;i>=0;i--) {
			Dep d = deps.get(i);
			if (d.gov==m2) {
				d.gov=m1;
				if (d.head==m1) removeDep(i);
				else depSortanteMot2=i;
			} else if (d.head==m2) {
				d.head=m1;
				if (d.gov==m1) removeDep(i);
			} else if (d.gov==m1) {
				depSortanteMot1=i;
			}
		}
		if (!allowMultipleHeads && depSortanteMot1>=0 && depSortanteMot2>=0) {
			removeDep(depSortanteMot2);
		}
	}
	public void mergeNWords(int w, int n, boolean allowMultipleHeads, char separator) {
		for (int i=1;i<n;i++)
			mergeTwoWords(w, allowMultipleHeads,separator);
	}

	/**
	 * retourne l'indice (l'id) de la (premiere) dependance partant d'un mot donne
	 * @param motGoverned
	 * @return
	 */
	public int getDep(int motGoverned) {
		for (int i=0;i<deps.size();i++) {
			Dep dep = deps.get(i);
			if (dep.gov!=null&&dep.gov.getIndexInUtt()-1==motGoverned) return i;
		}
		return -1;
	}
	public int[] getDeps(int motGoverned) {
		ArrayList<Integer> dd = new ArrayList<Integer>();
		for (int i=0;i<deps.size();i++) {
			Dep dep = deps.get(i);
			if (dep.gov!=null&&dep.gov.getIndexInUtt()-1==motGoverned) dd.add(i);
		}
		int[] r=new int[dd.size()];
		for (int i=0;i<dd.size();i++) r[i] = dd.get(i);
		return r;
	}
	/**
	 * retourne l'indice (l'id) de la ième dependance partant d'un mot donne
	 */
	public int getDep(int motGoverned, int depnum) {
		int depidx=0;
		for (int i=0;i<deps.size();i++) {
			Dep dep = deps.get(i);
			if (dep.gov!=null&&dep.gov.getIndexInUtt()-1==motGoverned) {
				if (depidx==depnum)
					return i;
				else depidx++;
			}
		}
		return -1;
	}
	public void changeDepGov(int dep, int newgov) {
		deps.get(dep).gov = getMot(newgov);
	}
	public void changeDepHead(int dep, int newhead) {
		deps.get(dep).head = getMot(newhead);
	}
	public void setConstrained(int gov) {
		constraints.add(gov);
	}
	public boolean isConstrainted(int gov) {
		return constraints.contains(gov);
	}
	public int getGoverned(int dep) {
		if (deps.get(dep).gov!=null)
			return deps.get(dep).gov.getIndexInUtt()-1;
		else return -1;
	}
	public String getDepLabel(int dep) {
		Dep d = deps.get(dep);
		if (d==null) return "ROOT";
		return d.toString();
	}
	public int getDepType(int dep) {
		Dep d = deps.get(dep);
		return d.type;
	}
	public int getHead(int dep) {
		if (dep>=0&&dep<deps.size()) {
			Dep d = deps.get(dep);
			if (d!=null) {
				if (d.head==null) return -d.grouphead-2;
				else return d.head.getIndexInUtt()-1;
			}
			else return -1;
		} else return -1;
	}
	public void setDepIdx(int dep, int newtype) {
		Dep d = deps.get(dep);
		if (newtype<Dep.depnoms.length)
			d.type=newtype;
	}
	public void cycleTypesInc(int dep) {
		Dep d = deps.get(dep);
		int newtype = d.type+1;
		if (newtype>=Dep.depnoms.length) newtype=0;
		d.type=newtype;
	}
	public void cycleTypesDec(int dep) {
		Dep d = deps.get(dep);
		d.type--;
		int newtype = d.type-1;
		if (newtype<0) newtype=Dep.depnoms.length-1;
		d.type=newtype;
	}
	public void removeDep(int dep) {
		deps.remove(dep);
	}
	public void removeAllDeps(int motGov) {
		int[] deps = getDeps(motGov);
		Arrays.sort(deps);
		for (int i=deps.length-1;i>=0;i--) {
			removeDep(deps[i]);
		}
	}
	public void setHead(int dep, int newhead) {
		if (dep<0||dep>=deps.size()) return;
		Dep d = deps.get(dep);
		if (d!=null)
			d.head = mots.get(newhead);
	}

	public int getRightmostNode(int root) {
		List<Integer> fils = getFils(root);
		if (fils.size()==0) return root;
		int rightmost=root;
		for (int f : fils) {
			int z = getRightmostNode(f);
			if (z>rightmost) rightmost=z;
		}
		return rightmost;
	}
	
	public int getLeftmostSubtreeNode(int root) {
		List<Integer> fils = getFils(root);
		if (fils.size()==0) return -1;
		Collections.sort(fils);
		int leftmost=fils.get(0);
		for (int f : fils) {
			int z = getLeftmostNode(f);
			if (z<leftmost) leftmost=z;
		}
		return leftmost;
	}
	public int getRightmostSubtreeNode(int root) {
		List<Integer> fils = getFils(root);
		if (fils.size()==0) return -1;
		Collections.sort(fils);
		int rightmost=fils.get(fils.size()-1);
		for (int f : fils) {
			int z = getRightmostNode(f);
			if (z<rightmost) rightmost=z;
		}
		return rightmost;
	}
	public int getLeftmostNode(int root) {
		List<Integer> fils = getFils(root);
		if (fils.size()==0) return root;
		int leftmost=root;
		for (int f : fils) {
			int z = getLeftmostNode(f);
			if (z<leftmost) leftmost=z;
		}
		return leftmost;
	}

	public List<Integer> getFils(int root) {
		ArrayList<Integer> fils = new ArrayList<Integer>();
		Mot rt = getMot(root);
		for (Dep d:deps) {
			if (d.head==rt) fils.add(d.gov.getIndexInUtt()-1);
		}
		return fils;
	}
	public List<Mot> getFils(Mot root) {
		ArrayList<Mot> fils = new ArrayList<Mot>();
		for (Dep d:deps) {
			if (d.head==root) fils.add(d.gov);
		}
		return fils;
	}

	public Mot getMot(int midx) {
		return mots.get(midx);
	}

	/**
	 * Use this method when there is no source file for the text
	 * such as when loading from CoNLL files, or directly editing in JSafran.
	 * @param idx commence a 0
	 * @param m
	 */
	public void addMot(int idx, Mot m) {
		mots.put(idx,m);
		m.setIndexInUtt(idx+1);
	}

	public int getNbMots() {
		return mots.size();
	}

	public void ajoutDepF2G(String label, int motgov, int grouphead) {
		int dep = Dep.getType(label);
		Dep d = new Dep();
		d.gov=mots.get(motgov);
		if (d.gov==null) {
			System.err.println("ERRRRRRR  "+label+" "+motgov);
			stop();
			return;
		}
		d.head=null; d.grouphead = grouphead;
		d.type = dep;
		deps.add(d);
	}
	public void ajoutDepG2F(String label, int groupgov, int mothead) {
		int dep = Dep.getType(label);
		Dep d = new Dep();
		d.gov=null; d.groupgov = groupgov;
		d.head=mots.get(mothead);
		if (d.head==null) {
			System.err.println("ERRRRRRR  "+label+" "+mothead);
			stop();
			return;
		}
		d.type = dep;
		deps.add(d);
	}
	public void ajoutDepG2G(String label, int groupgov, int grouphead) {
		int dep = Dep.getType(label);
		Dep d = new Dep();
		d.gov=null; d.groupgov = groupgov;
		d.head=null; d.grouphead = grouphead;
		d.type = dep;
		deps.add(d);
	}

	public int ajoutDep(String label, int mot1, int mot2) {
		return ajoutDep(Dep.getType(label),mot1,mot2);
	}
	public boolean isCycle(int gov, int head) {
		int w=head;
		while (getDep(w)>=0) {
			int dep = getDep(w);
			w=getHead(dep);
			if (w==gov) return true;
		}
		return false;
	}
	public int ajoutDep(int label, int mot1, int mot2) {
		Dep d = new Dep();
		// mot1 est le gouverne, mot2 est le head
		// si mot2 est ROOT (-1), alors on aura d.mot2=null
		d.gov=mots.get(mot1);

		if (d.gov==null) {
			System.err.println("ERRRRRRR  "+label+" "+mot1+" "+mot2+" "+mots.size());
			stop();
			return -1;
		}

		d.head=mots.get(mot2);
		d.type = label;

		deps.add(d);
		return deps.size()-1;
		/*
		int i=0;
		for (i=0;i<deps.size();i++) {
			Dep dd = deps.get(i);
			// pas le droit d'avoir 2 heads pour un meme mot
			if (dd.mot1==d.mot1) break;
		}
		if (i>=deps.size()) {
			// on a verifie que le mot n'avait pas d'autre head
			// TODO: on verifie maintenant que le lien ne cree pas de cycles
			int w=mot2;
			while (getDep(w)>=0) {
				int dep = getDep(w);
				w=getHead(dep);
			}
			if (w!=mot1) deps.add(d);
			else System.err.println("warning: cycle detecte: "+toString());
		}
		 */
	}
	void stop() {
		String s = null;
		s.charAt(0);
	}

	public int ajoutDepTmp(int label, int mot1, int mot2) {
		Dep d = new Dep();
		// mot1 est le gouverne, mot2 est le head
		// si mot2 est ROOT (-1), alors on aura d.mot2=null
		d.gov=mots.get(mot1);
		d.head=mots.get(mot2);
		d.type = label;
		int dd = deps.size();
		deps.add(d);
		return dd;
	}

	/**
	 * @param pere: Attention ! ce sont les index tels que vus de l'exterieur, qui commencent a 0 !!
	 * @param fils
	 * @param score
	 */
	public void setDepScore(int pere, int fils, float score) {
		Mot mpere = mots.get(pere+1);
		Mot mfils = mots.get(fils+1);
		Dep d=null;
		for (int i=0;i<deps.size();i++) {
			d=deps.get(i);
			if (d.gov==mpere && d.head==mfils) break;
		}
		d.score=score;
	}

	/**
	 * supprime les noeuds (et leurs arcs) de mot1 a mot2 inclus
	 * retourne true ssi il ne reste aucun noeud
	 */
	public boolean delNodes(int mot1, int mot2) {
		int nmots = mots.size();
		ArrayList<Mot> mots2del = new ArrayList<Mot>();
		for (int i=mot1;i<=mot2;i++) {
			mots2del.add(getMot(i));
		}
		// on detruit d'abord les dependances
		for (int i=deps.size()-1;i>=0;i--) {
			Dep dep = deps.get(i);
			if (mots2del.contains(dep.gov)||mots2del.contains(dep.head)) {
				deps.remove(i);
			}
		}
		// on decale/detruit les groupes
		if (groups!=null) {
			for (int i=groups.size()-1;i>=0;i--) {
				ArrayList<Mot> gr = groups.get(i);
				for (int j=gr.size()-1;j>=0;j--) {
					int w=gr.get(j).getIndexInUtt()-1;
					if (w>=mot1&&w<=mot2) {
						gr.remove(j);
					}
				}
				if (gr.size()==0) {
					groups.remove(i);
					groupnoms.remove(i);
				}
			}
		}
		// on detruit ensuite les noeuds
		for (int i=mots2del.size()-1;i>=0;i--) {
			Mot m = mots2del.get(i);
			mots.remove(m.getIndexInUtt()-1);
		}
		// on decale les indices suivants
		int delta = mot2-mot1+1;
		for (int i=mot2+1;i<nmots;i++) {
			Mot m = mots.remove(i);
			m.setIndexInUtt(m.getIndexInUtt()-delta);
			mots.put(m.getIndexInUtt()-1, m);
		}
		if (mots.size()==0) return true;
		else return false;
	}
	public void append(DetGraph subgraph) {
		int idx=mots.size();
		for (int i=0;i<subgraph.getNbMots();i++) {
			Mot m = subgraph.getMot(i);
			addMot(idx+i, m);
		}
		sent += " "+subgraph.sent;
		for (int i=0;i<subgraph.deps.size();i++) {
			Dep d = subgraph.deps.get(i);
			deps.add(d);
		}
		if (comment==null) comment = subgraph.comment;
		else comment += subgraph.comment;
		// TODO check sources identiques
		if (subgraph.groups!=null) {
			if (groups==null) {
				groups = new ArrayList<ArrayList<Mot>>();
				groupnoms = new ArrayList<String>();
			}
			groups.addAll(subgraph.groups);
			groupnoms.addAll(subgraph.groupnoms);
		}
	}

	/**
	 * les 2 methodes suivantes permettent d'extraire un sous-graphe autour d'un mot
	 * celle-ci extrait un sous-graphe autour d'une dependance partant du mot
	 * @param mot
	 * @return
	 */
	public DetGraph extractSubgraphFromDep(int mot) {
		int min=mot, max=mot;
		int[] deps = getDeps(mot);
		for (int i=0;i<deps.length;i++) {
			int head = getHead(deps[i]);
			if (head<min) min=head;
			else if (head>max) max=head;
		}
		if (--min<0) min=0;
		if (++max>=getNbMots()) max=getNbMots()-1;
		return getSubGraph(min, max);
	}
	/**
	 * de mot1 a la fin
	 * @param mot1
	 * @return
	 */
	public DetGraph getSubGraph(int mot1) {
		DetGraph gg = getSubGraph(mot1,mots.size()-1);
		if (relatedGraphs!=null&&relatedGraphs.size()>0) {
			gg.relatedGraphs=new ArrayList<DetGraph>();
			for (int i=0;i<relatedGraphs.size();i++) {
				// TODO: do it recursively ?
				// TODO: move this to the getSubGraph(x,y) function
				gg.relatedGraphs.add(relatedGraphs.get(i).getSubGraph(mot1));
			}
		}
		return gg;
	}
	public DetGraph clone() {
		return getSubGraph(0);
	}
	/**
	 * de mot1 a mot2 inclus !
	 */
	public DetGraph getSubGraph(int mot1, int mot2) {
		DetGraph g = new DetGraph();
		if (comment!=null&&comment.length()>0)
			g.comment=""+comment;
		if (source!=null) g.source=source;
		g.cursent=cursent;
		g.sent="";
		for (int i=mot1, j=0;i<=mot2;i++,j++) {
			Mot m = getMot(i);
			Mot mm = m.clone();
			mm.setIndexInUtt(j+1);
			g.addMot(j, mm);
			g.sent+=mm.getForme()+" ";
		}
		for (int i=mot1, j=0;i<=mot2;i++,j++) {
			int[] deps =  getDeps(i);
			for (int dep : deps) {
				int head = getHead(dep);
				if (head>=mot1&&head<=mot2) {
					g.ajoutDep(getDepLabel(dep), j, head-mot1);
				}
			}
		}
		if (groups!=null) {
			g.groupnoms=new ArrayList<String>();
			g.groups=new ArrayList<ArrayList<Mot>>();
			for (int i=0;i<groups.size();i++) {
				ArrayList<Mot> l = groups.get(i);
				int wmin = l.get(0).getIndexInUtt()-1;
				int wmax = l.get(l.size()-1).getIndexInUtt()-1;
				if (wmin>=mot1&&wmin<=mot2&&wmax>=mot1&&wmax<=mot2) {
					g.groupnoms.add(groupnoms.get(i));
					ArrayList<Mot> gs = new ArrayList<Mot>();
					for (int j=wmin;j<=wmax;j++) gs.add(g.getMot(j-mot1));
					g.groups.add(gs);
				}
			}
		}
		return g;
	}
	/**
	 * extrait (en supprimant les noeuds du graphe d'origine selon boolean) le sous-arbre complet
	 * contenant un mot donne.
	 */
	public DetGraph extractSubTreeContaining(int mot, boolean removeWords) {
		DetGraph g = getSubGraph(0);
		HashSet<Integer> noeudsin = new HashSet<Integer>();
		remonteBranche(noeudsin,mot);
		for (int i=0;i<getNbMots();i++)
			if (!noeudsin.contains(i)) {
				remonteBrancheJusque(noeudsin,i,false);
			}
		for (int i=getNbMots()-1;i>=0;i--) {
			if (!noeudsin.contains(i))
				g.delNodes(i, i);
			else if (removeWords) delNodes(i, i);
		}
		return g;
	}
	private void remonteBranche(HashSet<Integer> noeudsin, int mot){
		noeudsin.add(mot);
		int[] deps = getDeps(mot);
		for (int i=0;i<deps.length;i++)
			remonteBranche(noeudsin,getHead(deps[i]));
	}
	private boolean remonteBrancheJusque(HashSet<Integer> noeudsin, int mot, boolean isOK){
		if (noeudsin.contains(mot)) return true;
		if (isOK) noeudsin.add(mot);
		int[] deps = getDeps(mot);
		for (int i=0;i<deps.length;i++) {
			if (remonteBrancheJusque(noeudsin,getHead(deps[i]),isOK)) {
				isOK=true;
				noeudsin.add(mot);
			}
		}
		return isOK;
	}

	public static PrintWriter saveHeader(String nom) {
		PrintWriter fout = null;
		try {
			fout = new PrintWriter(new FileWriter(nom));
			saveHeader(fout);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fout;
	}
	public static void saveHeader(PrintWriter fout) {
		fout.println("<checkenc é>");
		fout.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
		fout.println("<!DOCTYPE syntex SYSTEM \"SyntexX.DTD\">");
		fout.println("<syntex>");
	}
	public void save(PrintWriter fout) {
		fout.println("<SEQ id=\"T_"+cursent+"\">");
		if (sent!=null)
			fout.println("<TXT>"+sent+"</TXT>");
		if (comment!=null)
			fout.println("<CMT>"+comment+"</CMT>");
		if (conf!=Float.NaN)
			fout.println("<CONF>"+conf+"</CONF>");
		if (source!=null)
			fout.println("<SOURCE>"+source+"</SOURCE>");
		fout.println("<tokens>");
		for (int i=0;i<getNbMots();i++) {
			fout.println("<t i=\""+(i+1)+"\" l=\""+getMot(i).getLemme()+"\" f=\""+getMot(i).getForme()+"\" c=\""+getMot(i).getPOS()+"\" p=\""+"\" srcpos=\""+getMot(i).getDebPosInTxt()+";"+getMot(i).getEndPosInTxt()+"\"/>");
		}
		fout.println("</tokens>");
		if (groups!=null) {
			for (int i=0;i<groups.size();i++) {
				ArrayList<Mot> motsdugroup = groups.get(i);
				String nom="UNK";
				if (groupnoms.size()>=i) nom = groupnoms.get(i);
				fout.println("<group> "+nom+" "+(motsdugroup.get(0).getIndexInUtt()-1)+" "+(motsdugroup.get(motsdugroup.size()-1).getIndexInUtt()-1));
			}
		}
		fout.println("<dependances>");
		for (int i=0;i<getNbMots();i++) {
			int[] deps = getDeps(i);
			for (int j=0;j<deps.length;j++) {
				int dep = deps[j];
				if (dep<0) continue;
				int mot1 = getHead(dep)+1;
				int mot2 = getGoverned(dep)+1;
				String lab = getDepLabel(dep);
				fout.println("<d r=\""+lab+"\" s=\""+mot2+"\" c=\""+mot1+"\"/>");
			}
		}
		fout.println("</dependances>");
		fout.println("<constraints>");
		for (int c : constraints)
			fout.println(c);
		fout.println("</constraints>");
		fout.println("</SEQ>");
		fout.flush();
	}

}
