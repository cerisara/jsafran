package jsafran.parsing.unsup;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;

import utils.SimpleBarChart;

import jsafran.DetGraph;

/**
 * Super-class that computes a Multinomial-Dirichlet posterior
 * 
 * All criteria must subclass this class
 * 
 * change: now it depends on a meta-index returned by the sub-class
 * This meta-index splits the counts into independent clusters.
 * Typically, this meta-index = HEAD word (for frames, lexpref)
 * 
 * @author xtof
 *
 */
public abstract class Criterion {
	public final double alpha = 0.001;
	// meta-index X index X occ. counts
	public HashMap<Integer,HashMap<Long,Long>> counts = new HashMap<Integer, HashMap<Long,Long>>();
	// countstot est la somme des counts observes pour tous les slots possibles
	// meta-index X occ. tot
	public HashMap<Integer, Long> countstot = new HashMap<Integer, Long>();

	public int charid=0;
	DetGraph gg=null;
	/**
	 * pour debug: plot some PDFs for this sentence
	 * @param g
	 */
	void showOneUtt(DetGraph g) {
		long[] idxs = getCriterionIndexes(g);
		System.out.println("show one utt "+idxs.length+" "+charid);
		System.out.print("metai: ");
		for (int t=charid;t<idxs.length;t++) {
			System.out.print(getMetaIndex(t)+" ");
		}
		System.out.println();
		gg=g;
		for (int t=charid;t<idxs.length;t++) {
			charid=t;
			long idx = idxs[t];
			int metai = getMetaIndex(t);

			Long n = 0l;
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp==null)
				System.out.println("attempt to show word "+t+" "+metai+" "+tmp);
			else
				System.out.println("attempt to show word "+t+" "+metai+" "+tmp.size());

			if (tmp!=null) {
				// TODO: afficher la pdf des tmp
				double[] y = new double[tmp.size()];
				String[] x = new String[y.length];
				Arrays.fill(x, "");
				n = countstot.get(metai);
				if (n==null) n=0l;
				int i=0;
				for (long u:tmp.keySet()) {
					if (u==idx) x[i]=charid+"-"+u;
					double num = Math.log(alpha+(double)tmp.get(u));
					double sum = Math.log((double)n+(double)getSpaceSize()*alpha);
					num-=sum;
					y[i++]=num;
				}
				System.out.println("showing word "+charid+"/"+idxs.length+" "+n);
				JFrame barchart = SimpleBarChart.drawChart(y,x,false);
				class MyMouseAdapter extends MouseAdapter {
					int charidx=charid;
					public void mouseClicked(MouseEvent e) {
						System.out.println("mouse clicked ! "+charid);
						charid++;
						showOneUtt(gg);
					}
				}
				boolean hasit = false;
				for (MouseListener ma : barchart.getMouseListeners()) {
					if (ma instanceof MyMouseAdapter) {
						hasit=true; break;
					}
				}
				if (!hasit)
					barchart.addMouseListener(new MyMouseAdapter());
				break;
			}
		}
	}

	void saveCounts() {
		System.out.println("save "+counts.size());
		try {
			PrintWriter f = new PrintWriter(new FileWriter("counts.txt",true));
			f.println("counts 4 class "+this.getClass().getName());
			for (int metai : counts.keySet()) {
				f.print(metai+": ");
				HashMap<Long,Long> cos = counts.get(metai);
				if (cos!=null) {
					for (long i: cos.keySet()) {
						f.print(i+"-"+cos.get(i)+" ");
					}
				}
				f.println();
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param g
	 * @return all indexes of the target "structure/criterion" that occurs in the sentence
	 * There may be one (e.g. number of roots), or several (e.g. subcat frames of the verb "avoir")
	 */
	public abstract long[] getCriterionIndexes(DetGraph g);

	/**
	 * @param t
	 * @return the meta-index associated to the t^th entry in the tab returned by getCriterionIndexes
	 */
	public abstract int getMetaIndex(int t);

	/**
	 * 
	 * @return the number of different indexes that this criterion can have
	 * (e.g. 0,1,2,3+ for the number of roots)
	 */
	public abstract long getSpaceSize();

	HashMap<Integer, Integer> wordidx2freq = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> depidx2freq = new HashMap<Integer, Integer>();
	public void init(List<DetGraph> gs) {
		clearCounts();
	}

	public void clearCounts() {
		counts.clear();
		countstot.clear();
	}

	public void increaseCounts(DetGraph g) {
		long[] idxs = getCriterionIndexes(g);
		for (int t=0;t<idxs.length;t++) {
			long idx = idxs[t];
			int metai = getMetaIndex(t);
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp==null) {
				tmp = new HashMap<Long, Long>();
				counts.put(metai, tmp);
			}
			Long n = tmp.get(idx);
			if (n==null) n=1l;
			else n++;
			tmp.put(idx, n);
			n = countstot.get(metai);
			if (n==null) n=1l; else n++;
			countstot.put(metai, n);
		}
	}
	public void decreaseCounts(DetGraph g) {
		long[] idxs = getCriterionIndexes(g);
		for (int t=0;t<idxs.length;t++) {
			long idx = idxs[t];
			int metai = getMetaIndex(t);
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp==null) System.out.println("WARNING: decrease counts that do not exist ! "+g);
			Long n = tmp.get(idx);
			if (n==null) System.out.println("WARNING: decrease counts that do not exist ! "+g);
			else n--;
			if (n==0) {
				tmp.remove(idx);
				if (tmp.size()==0) counts.remove(tmp);
			} else tmp.put(idx, n);
			n = countstot.get(metai);
			n--;
			countstot.put(metai, n);
		}
	}

	// must be called after getCriterionIndexes, just like getMetaIndex
	// but while in getMetaIndex(i), i indexes the int[] tab,
	// here, i IS the value in one of int[] cell
	public double getLogPost(DetGraph g, int i, int meta) {
		Long n = 0l;
		HashMap<Long, Long> tmp = counts.get(meta);
		if (tmp!=null) {
			n = tmp.get(i);
			if (n==null) n=0l;
		}
		double num = Math.log(alpha+(double)n);
		n = countstot.get(meta);
		if (n==null) n=0l;
		double sum = Math.log((double)n+(double)getSpaceSize()*alpha);
		num-=sum;
		return num;
	}

	public double getLogPost(DetGraph g) {
		long[] idxs = getCriterionIndexes(g);
		double res=0;
		for (int t=0;t<idxs.length;t++) {
			long idx = idxs[t];
			int metai = getMetaIndex(t);

			Long n = 0l;
			HashMap<Long, Long> tmp = counts.get(metai);
			if (tmp!=null) {
				n = tmp.get(idx);
				if (n==null) n=0l;
			}
			double num = Math.log(alpha+(double)n);
			n = countstot.get(metai);
			if (n==null) n=0l;
			double sum = Math.log((double)n+(double)getSpaceSize()*alpha);
			num-=sum;

			res+=num;
		}
		return res;
	}
}
