package garbage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sun.org.apache.bcel.internal.generic.FCMPG;

import utils.FileUtils;
import utils.SuiteDeMots;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.Mot;

import corpus.text.TextSegments;
import corpus.text.TextSegments.segtypes;

public class AlignWithSTM {
	
	static int fmatch(int[] small, int posinsmall, int[] big) {
		int bestok=0;
		int bestpos=-1;
		for (int i=0;i<big.length;i++) {
			// cherche le nb de mots communs dans une fenetre small.length*2
			int nok=0;
			for (int j=0;j<30&&i+j<big.length;j++)
				for (int k=0;k<30;k++)
					if (small[k+posinsmall]==big[i+j]) {nok++; break;}
			if (nok>bestok) {
				bestok=nok; bestpos=i;
			}
		}
		System.out.println(bestpos+":"+bestok);
		return bestpos;
	}
	
	static void fin() throws Exception {
		ArrayList<Integer> gmot2stmmot = new ArrayList<Integer>();
		BufferedReader f = FileUtils.openFileUTF("posdegmots.txt");
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			String[] ss =s.split(" ");
			gmot2stmmot.add(Integer.parseInt(ss[1]));
		}
		f.close();
		
		// on recupere tous les mots + ponct des STM
		BufferedReader fs = FileUtils.openFileUTF("stmmots.txt");
		String s = fs.readLine();
		fs.close();
		String[] stmmots = s.split(" ");

		final String ponct = ",?.;:!";
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
		int midx=0;
		for (int i=84;i<gs.size();i++) {
			if (midx>=gmot2stmmot.size()) break;
			System.out.println("treating "+i+"/"+gs.size());
			String[] gmots = gs.get(i).getMots();
			SuiteDeMots smots = new SuiteDeMots(gmots);
			int x=gmot2stmmot.get(midx);
			midx+=gmots.length;
			int y=x+gmots.length*2;
			if (y>stmmots.length) y=stmmots.length;
			{
				System.out.println("GRAPH: "+gs.get(i).sent);
				StringBuilder sb = new StringBuilder();
				for (int z=x;z<y;z++) sb.append(stmmots[z]+' ');
				System.out.println("STM  : "+sb);
				
				x-=5;
				if (x<0) x=0;
				System.out.println("x y "+x+" "+y);
				SuiteDeMots tmots = new SuiteDeMots(Arrays.copyOfRange(stmmots, x, y));
				
				tmots.align(smots);
				for (int j=tmots.getNmots()-1;j>=1;j--) {
					// est-ce une ponct ?
					if (tmots.getMot(j).length()>0&&ponct.indexOf(tmots.getMot(j).charAt(0))>=0) {
						// est-elle après 1 mot du graphe ?
						int deb=-1;
						int[] w = tmots.getLinkedWords(j-1);
						if (w.length>0) {
							Arrays.sort(w);
							deb=w[w.length-1];
							if (!tmots.getMot(j-1).equals(smots.getMot(deb))) deb=-1;
						}
						if (deb>=0) {
								Mot m = new Mot(tmots.getMot(j),"p","PUN");
								gs.get(i).insertMot(deb+1, m);
						}
					}
				}
			}
		}
		gio.save(gs, "train2.xml");
	}
	
	public static void main(String args[]) throws Exception {
		fin();
	}
	public static void main7(String args[]) throws Exception {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
		ArrayList<String> gmots = new ArrayList<String>();
		for (int i=84;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			for (int j=0;j<g.getNbMots();j++) gmots.add(g.getMot(j).getForme());
		}
		// on a tous les mots des graphes
		
		// on recupere tous les mots + ponct des STM
		BufferedReader f = FileUtils.openFileUTF("stmmots.txt");
		String s = f.readLine();
		f.close();
		String[] stmmots = s.split(" ");
		System.out.println("nmots "+stmmots.length+" "+gmots.size());

		// build voc
		HashMap<String, Integer> voc = new HashMap<String, Integer>();
		int[] graphmots = new int[gmots.size()];
		for (int i=0;i<gmots.size();i++) {
			Integer j = voc.get(gmots.get(i));
			if (j==null) {
				j=voc.size();
				voc.put(gmots.get(i), j);
			}
			graphmots[i]=j;
		}
		int[] bigmots = new int[stmmots.length];
		for (int i=0;i<stmmots.length;i++) {
			Integer j = voc.get(stmmots[i]);
			if (j==null) {
				j=voc.size();
				voc.put(stmmots[i], j);
			}
			bigmots[i]=j;
		}
		
		// on prend des seqs de 30 mots
		int[] gmotposinstm = new int[gmots.size()];
		for (int i=0;i<graphmots.length-30;i++) {
			System.out.println("mot "+i+"/"+gmots.size());
			int pos = fmatch(graphmots,i,bigmots);
			gmotposinstm[i]=pos;
		}
		PrintWriter ff = FileUtils.writeFileUTF("posdegmots.txt");
		for (int i=0;i<gmots.size()-30;i++) {
			ff.println(i+" "+gmotposinstm[i]);
		}
		ff.close();
	}
	
	public static void main3(String args[]) throws Exception {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
		ArrayList<String> gmots = new ArrayList<String>();
		for (int i=84;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			for (int j=0;j<g.getNbMots();j++) gmots.add(g.getMot(j).getForme());
		}
		// on a tous les mots des graphes
		
		// on recupere tous les mots + ponct des STM
		ArrayList<String> stmmots = new ArrayList<String>();
		final String stmdir = "/home/xtof/corpus/stm4jsafran";
		File d = new File(stmdir);
		File[] stms = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".stm"));
			}
		});
		for (int i=0;i<stms.length;i++) {
			System.out.println("seg "+i+"/"+stms.length);
			TextSegments seg = new TextSegments();
			seg.preloadSTMFile(stms[i].getAbsolutePath(),true);
			for (int j=0;j<seg.getNbSegments();j++) {
				TextSegments newseg = seg.tokenizeBasic(j);
				newseg.tokenizeComments();
				for (int k=0;k<newseg.getNbSegments();k++)
					if (newseg.getSegmentType(k)==segtypes.mot)
						stmmots.add(newseg.getSegment(k));
			}
		}
		System.out.println("nmots "+stmmots.size()+" "+gmots.size());
		PrintWriter ff = FileUtils.writeFileUTF("stmmots.txt");
		for (String s : stmmots) {
			ff.print(s+" ");
		}
		ff.close();
	}
	
	public static void main2(String args[]) throws Exception {
		BufferedReader f = FileUtils.openFileUTF("/home/xtof/corpus/stm4jsafran/res5.txt");
		HashMap<String, List<Integer>> stm2graphsidx = new HashMap<String, List<Integer>>();
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			String[] ss =s.split(" ");
			if (ss.length==1) continue;
			String stm = ss[1];
			List<Integer> gs = stm2graphsidx.get(stm);
			if (gs==null) {
				gs = new ArrayList<Integer>();
				stm2graphsidx.put(stm, gs);
			}
			gs.add(Integer.parseInt(ss[0]));
		}
		f.close();
		
		for (String stm : stm2graphsidx.keySet()) {
			System.out.println(stm+" "+stm2graphsidx.get(stm));
		}
	}
	
	
	public static void oldmain(String args[]) {
		final String stmdir = "/home/xtof/corpus/ESTER2TRAIN/allstms";
		File d = new File(stmdir);

		File[] stms = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".stm"));
			}
		});
		TextSegments[] segs = new TextSegments[stms.length];
		SuiteDeMots[] stmmots = new SuiteDeMots[segs.length];
		for (int i=0;i<segs.length;i++) {
			System.out.println("seg "+i+"/"+segs.length);
			segs[i] = new TextSegments();
			segs[i].preloadSTMFile(stms[i].getAbsolutePath(),true);
			ArrayList<TextSegments> tmpmots = new ArrayList<TextSegments>();
			for (int j=0;j<segs[i].getNbSegments();j++) {
				TextSegments newseg = segs[i].tokenizeBasic(j);
				newseg.tokenizeComments();
				newseg.tokenizePonct();
				tmpmots.add(newseg);
			}
			segs[i].clear();
			ArrayList<String> segmots = new ArrayList<String>();
			for (TextSegments s : tmpmots) {
				for (int j=0;j<s.getNbSegments();j++) {
					segs[i].creeSegment(s.getSegmentStartPos(j), s.getSegmentEndPos(j), s.getSegmentType(j));
					segmots.add(segs[i].getSegment(j));
				}
			}
			stmmots[i]=new SuiteDeMots(segmots.toArray(new String[segmots.size()]));
		}

		GraphIO gio =new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("/home/xtof/git/jsafran/train2011.xml");
		for (int i=0;i<gs.size();i++) {
			String[] mots = gs.get(i).getMots();
			if (mots.length>4) {
				SuiteDeMots xmlmots = new SuiteDeMots(mots);
				for (int j=0;j<segs.length;j++) {
					float err = stmmots[j].lookForSubsuite(xmlmots);
					System.out.println("err "+err);
				}
			}
		}
	}
}
