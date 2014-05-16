package jsafran.parsing.unsup.criteria;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.Mot;
import jsafran.parsing.unsup.Criterion;

/**
 * P(#filsL_D1 ... #filsL_Dn #filsR_D1 ... #filsR_Dn|H)
 * 
 * @author xtof
 *
 */
public class Nfils extends Criterion {
	final int nmax=3;
	private LexPref lexpref;
	long spacesize;

	public void init(List<DetGraph> gs) {
		// lexpref doit etre deja initialise !
		spacesize=(long)Math.pow(nmax,Dep.depnoms.length+Dep.depnoms.length);
		System.out.println("nfils spacesize "+spacesize+" "+(int)spacesize);
	}

	public Nfils(LexPref lp) {
		lexpref=lp;
	}
	
	int[] metai;
	
	@Override
	public long[] getCriterionIndexes(DetGraph g) {
		long[] res = new long[g.getNbMots()];
		Arrays.fill(res,0);
		int[][][] co = new int[g.getNbMots()][2][Dep.depnoms.length];
		metai = new int[g.getNbMots()];
		for (int i=0;i<res.length;i++) {
			Arrays.fill(co[i][0],0);
			Arrays.fill(co[i][1],0);
			metai[i] = lexpref.getWordIndex(g.getMot(i));
		}
		for (Dep dep : g.deps) {
			int h=dep.head.getIndexInUtt()-1;
			int d=dep.type;
			int dir = dep.head.getIndexInUtt()-dep.gov.getIndexInUtt()>0?0:1;
			if (co[h][dir][d]<nmax-1) co[h][dir][d]++;
		}
		for (int i=0;i<res.length;i++) {
			long offset=1;
			for (int j=0;j<co[i][0].length;j++) {
				res[i]+=co[i][0][j]*offset;
				offset*=nmax;
			}
			for (int j=0;j<co[i][1].length;j++) {
				res[i]+=co[i][1][j]*offset;
				offset*=nmax;
			}
			if (res[i]>=spacesize || offset>spacesize || res[i]<0)
				System.out.println("WARNING !!! "+res[i]+" "+spacesize+" "+offset);
		}
		return res;
	}

	int[][] getNfils(long idx) {
		int[][] res = new int[2][Dep.depnoms.length];
		for (int dir=0;dir<2;dir++) {
			for (int i=0;i<Dep.depnoms.length;i++) {
				res[dir][i]=(int)(idx%nmax);
				idx/=nmax;
			}
		}
		return res;
	}
	
	@Override
	public long getSpaceSize() {
		return spacesize;
	}

	@Override
	public int getMetaIndex(int t) {
		return metai[t];
	}

	
	public static void main(String args[]) throws Exception {
		Dep.loadConfig(new FileInputStream("jsynats.cfg"));
		System.out.println("deps "+Arrays.toString(Dep.depnoms));
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("test2009.xml");
		LexPref lp = new LexPref();
		lp.init(gs);
		Nfils c = new Nfils(lp);
		c.init(gs);
		
		BufferedReader f= new BufferedReader(new InputStreamReader(System.in));
		for (;;) {
			String s = f.readLine();
			s=s.trim();
			if (s.length()==0) break;
			String[] ss = s.split(" ");
			if (ss[0].equals("I")) {
				int idx = Integer.parseInt(ss[1]);
				String w = lp.getWord4Index(idx);
				System.out.println(w);
			} else if (ss[0].equals("W")) {
				Mot m = new Mot(ss[1], ss[1], ss[1]);
				int i = lp.getWordIndex(m);
				System.out.println(i);
			} else if (ss[0].equals("J")) {
				long idx = Long.parseLong(ss[1]);
				int[][] nfils = c.getNfils(idx);
				for (int i=0;i<nfils[0].length;i++) {
					System.out.println(Dep.depnoms[i]+"L "+nfils[0][i]);
				}
				for (int i=0;i<nfils[1].length;i++) {
					System.out.println(Dep.depnoms[i]+"R "+nfils[1][i]);
				}
			}
		}
	}
}
