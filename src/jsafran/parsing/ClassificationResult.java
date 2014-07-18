package jsafran.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;

public class ClassificationResult {
	public static ArrayList<int[]> errors;
	
	public static boolean isWordCorrect(DetGraph grec, DetGraph gref, int w) {
        int d=grec.getDep(w);
        int dg=gref.getDep(w);
	    if (d<0 && dg>=0) return false;
	    int h=grec.getHead(d);
	    int hg=gref.getHead(dg);
	    if (h!=hg) return false;
        int l=grec.deps.get(d).type;
        int lg=gref.deps.get(dg).type;
        if (l!=lg) return false;
        return true;
	}
	
	public static float[] calcErrors(List<DetGraph> grecs, List<DetGraph> grefs) {
		errors = new ArrayList<int[]>();
		int ndel=0,nins=0,nsub=0,nhead=0,nokLAS=0,ntot=0,nokUAS=0;
		int[][] perdep = new int[Dep.depnoms.length][2]; // 0=nOK 1=ntot (ref)
		for (int i=0;i<perdep.length;i++) Arrays.fill(perdep[i], 0);
//		System.out.println("calcErrors nrefs="+grefs.size()+" nrecs="+grecs.size());
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
				//					if (maxlen2score>=0&&drec>=0&&Math.abs(recmot-grec.getHead(drec))>maxlen2score) continue;

				ntot++;
				if (drec<0&&dref>=0) {
					errwords.add(recmot);
					ndel++;
				} else if (drec>=0&&dref<0) {
					errwords.add(recmot);
					nins++;
				} else if (drec>=0&&dref>=0) {
					int href = gref.getHead(dref);
					int hrec = grec.getHead(drec);
					if (href!=hrec) {
						errwords.add(recmot);
						nhead++;
					} else {
						nokUAS++;
						String reclab = grec.getDepLabel(drec);
						// on supprime les "_" en debut de deplab
						if (reclab.charAt(0)=='_') reclab = reclab.substring(1);
						if (gref.getDepLabel(dref).equals(reclab)) {
							perdep[gref.deps.get(dref).type][0]++;
							nokLAS++;
						} else {
							errwords.add(recmot);
							nsub++;
						}
					}
				} else if (drec<0&&dref<0) {
					nokLAS++;
					nokUAS++;
				}
			}
			for (int w : errwords) {
				int[] newerr = {1,i,w};
				errors.add(newerr);
			}
			for (Dep d : gref.deps) {
				perdep[d.type][1]++;
			}
		}
//		System.out.println("acc counts: ntot="+ntot+" nok="+nokLAS+" nsub="+nsub+" nhead="+nhead+" ndel="+ndel+" nins="+nins);
		float[] LasUas = new float[perdep.length*2+2];
		LasUas[0] = (float)nokLAS/(float)ntot;
		LasUas[1] = (float)nokUAS/(float)ntot;
		for (int i=0;i<perdep.length;i++) {
			LasUas[i*2+2]=(float)perdep[i][0]/(float)perdep[i][1];
//			System.out.println("LASDEP "+Dep.depnoms[i]+" "+perdep[i][0]+" "+perdep[i][1]+" "+LasUas[i*2+2]);
			LasUas[i*2+3]=perdep[i][1];
		}
		return LasUas;
	}

	// old stuff

	class OneRes implements Comparable<OneRes> {
		String output;
		float p;
		@Override
		public int compareTo(OneRes o) {
			if (p>o.p) return -1;
			else if (p<o.p) return 1;
			else return 0;
		}
	}
	ArrayList<OneRes> outputs = new ArrayList<ClassificationResult.OneRes>();

	public void addOneClass(String output, float proba) {
		OneRes o = new OneRes(); o.output=output; o.p=proba;
		outputs.add(o);
	}

	public String[] getNbest(int n, List<Float> scores) {
		String[] res = new String[n];
		Collections.sort(outputs);
		// ils sont maintenant classés par ordre décroissant
		for (int i=0;i<n;i++) {
			res[i]=outputs.get(i).output;
			if (scores!=null) scores.add(outputs.get(i).p);
		}
		return res;
	}
}
