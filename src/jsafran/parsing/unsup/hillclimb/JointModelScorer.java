/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jsafran.parsing.unsup.hillclimb;

import java.util.*;
import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.parsing.unsup.OrderRoles;
import jsafran.parsing.unsup.SemanticProcesing;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.rules.Rules;

/**
 * Scorer using the discriminative Joint syntactic and semantic bayesian model (sent to naacl2013). 
 * TODO: see how to normalize for all possible scores, is it necessary now? 
 * TODO: see the  Penalties for cycles and repeated core roles. they sum it's not ok...
 * @author ale
 */
public class JointModelScorer implements Scorer{
    /*********************constants**************************************/
    final int maxndeps = 5;
    final int vact=0;
    final int vpas=1;
    final int rpleft=0;
    final int rpright=1;
    int nrules=0;
    //Rules rules;
    Voc voc;
    int numArgs=6; //start by considering A0, A1, A2, A3, A4, AM
    private String[]rolarr={"A0","A1","A2","A3","A4","AM"};
    public ArrayList<String>roles=new ArrayList<String>();
    
    boolean usePenalties=false;
    
    /*********************precomputed structures***************************/
    //precomputed structures for syntax
    private DetGraph lastgComputed = null; //last seen syntactic graph
    private int[] lastD;
    private int[][] lastW;
    private int[] ndeps; 
    //precomputed structures for semantics
    private DetGraph lastgComputedSRL = null; //last seen semantic graph
    private ArrayList<Integer> predicates;
    private int[] voices;
    private int[][] argsPerPred;
    private int[] ordsPerPred; 
    private int[][] wasPerPred;
    private int[][] rpsPerPred;
    
    
    /************************ the counts:**********************************/
    // H x D x noccs
    HashMap<Integer,HashMap<Integer, Integer>> countsHD = new HashMap<Integer, HashMap<Integer,Integer>>();
    // H x W x noccs
    HashMap<Integer,HashMap<Integer, Integer>> countsHW = new HashMap<Integer, HashMap<Integer,Integer>>();
    // H x Vo x O x noccs
    HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoO = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
    // H x Vo x O x A x noccs
    HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHOA = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
    // H x A x Wa x noccs
    HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWa = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
    // H x Vo x A x Rp x noccs
    HashMap<Integer,HashMap<Integer,HashMap<Integer, Integer>>>countsVoARp = new HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>>();
    // WARNING: we must keep and update in increaseCounts()/decreaseCounts() in countsHD.get(h).get(-1) the TOTAL over all d

    public JointModelScorer(Voc v) {
		voc=v;
                ndeps = new int[getLabs2track().length];
                //load the roles
                for (int i=0;i<rolarr.length;i++){
                    roles.add(rolarr[i]);
                }
    }

    @Override
    public double getScore(DetGraph g) {
        double score=getLogPost(g);
        
        int penalties=0;
        //I could also weight it down if it contains repeated arcs. 
        if (usePenalties){
            penalties+=penaltyCycles(g)*10;
            penalties+=penaltySRL(g);
        }
        return score+penalties;
    }

    @Override
    public void incCounts(DetGraph g) {
                lastgComputed=null;
        lastgComputedSRL=null;

        updateCounts(true, g,countsHD, countsHW, countsHVoO,countsHOA,countsHAWa,countsVoARp);
    }

    @Override
    public void decCounts(DetGraph g) {
        //assuming that we decrease counts before searching a rules sequence for each utterance, before decreasing counts
        //reset the precomputed structures used by updateCounts
        lastgComputed=null;
        lastgComputedSRL=null;
        updateCounts(false, g, countsHD, countsHW, countsHVoO,countsHOA,countsHAWa,countsVoARp);

    }
    

    
    /*******************Model Hyperparameters***************************/
    //alphas for the syntactic part
    protected double getAlphaD(int d) {
            return 0.001;
    }
    protected double getAlphaW(int w) {
            return 0.001;
    }
    //alphas for the semantic part
    protected double getAlphaO(int d) {
            return 0.001;
    }
    protected double getAlphaA(int d) {
            return 0.001;
    }
    protected double getAlphaWa(int d) {
            return 0.001;
    }
    protected double getAlphaRp(int d) {
            return 0.001;
    }
    
    
   /***************************************************************************************************************
                                            Penalties
                                            * todo: see why they add!!!!!!!
                                       
    ***************************************************************************************************************/
        static int penaltySRL(DetGraph g) {
            int penaltyRepArgs=0;
		if (g.relatedGraphs!=null && g.relatedGraphs.size()>0) {
                    DetGraph gg = g.relatedGraphs.get(0);
                    for (int i=0;i<gg.getNbMots();i++){
                        if (g.getMot(i).getPOS().startsWith("VB")){
                            boolean hasA0=false;
                            boolean hasA1=false;
                            boolean hasA2=false;
                            boolean hasA3=false;
                            boolean hasA4=false;
                            List<Integer> childre= gg.getFils(i);
                            for (int c:childre){
                                int[]deps=gg.getDeps(c);
                                for (int d:deps){
                                    if (gg.getDepLabel(d).equals("A0")) {
                                        if (hasA0) penaltyRepArgs+=100;
                                        else hasA0=true;
                                    }
                                    else{
                                        if (gg.getDepLabel(d).equals("A1")) {
                                            if (hasA1) penaltyRepArgs+=100;
                                            else hasA1=true;
                                        }else{
                                            if (gg.getDepLabel(d).equals("A2")) {
                                                if (hasA2) penaltyRepArgs+=100;
                                                else hasA2=true;
                                            }else{
                                                if (gg.getDepLabel(d).equals("A3")) {
                                                    if (hasA3) penaltyRepArgs+=100;
                                                    else hasA3=true;
                                                }else{
                                                    if (gg.getDepLabel(d).equals("A4")) {
                                                        if (hasA4) penaltyRepArgs+=100;
                                                        else hasA4=true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return penaltyRepArgs;
        }

	static int penaltyCycles(DetGraph g) {
		HashSet<Integer> dejavus = new HashSet<Integer>();
		int ncycles = 0;
		for (Dep de : g.deps) {
			dejavus.clear();
			int gov = de.gov.getIndexInUtt()-1;
			int head = de.head.getIndexInUtt()-1;
			int w = head;
			dejavus.add(gov);
			dejavus.add(head);
			for (;;) {
				int d = g.getDep(w);
				if (d<0) break;
				w=g.getHead(d);
				if (dejavus.contains(w)) {
					ncycles++;
					break;
				}
				dejavus.add(w);
			}
		}
		return ncycles;
	}
    

   /***************************************************************************************************************
                                            Update Counts
    ***************************************************************************************************************/
         //updateCounts(inc, g, countsHD, countsHW, countsHVoO,countsHVoOA,countsHAWa,countsHVoARp);
        private void updateCounts(boolean inc, DetGraph g, 
                                //syntactic part
                                HashMap<Integer, HashMap<Integer, Integer>> countshd,
                                HashMap<Integer, HashMap<Integer, Integer>> countshw,
                                //semantic part
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshvo,
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>>  countshoa,
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshaw,
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>>  countsvar) {
            DetGraph gsr= g.relatedGraphs.get(0);
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
				else if (co<0) System.out.println("ERROR negative counts updateCounts! "+d+" "+i+" "+g);
				d2co.put(d, co);
				int tot=d2co.get(-1);
				d2co.put(-1, --tot);
				if (tot<0) System.out.println("ERROR negative counts tot updateCounts!");
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

                        //semantic part, only update when h is a predicate
                        if (isPredicate(g, i)){
                            //countshvo
                            int vo=getVoice(g,i);
                            int o=getOrder(g,i);
                            int[] as=getArgs(g,i);
                            int[] was=getWordsSRL(g,i);
                            int[] ras=getRelposSRL(g,i);
                            updateCountsHVoO(inc, h, vo, o, countshvo);
                            //countshvoa:
                            updateCountsHOA(inc, h,  o, as, countshoa);
                            //countshaw
                            updateCountsHAW(inc, h, as, was, countshaw);
                            //countshvar
                            updateCountsVoARp(inc, vo, as, ras, countsvar);
                        }
		}

	}

        private void updateCountsHVoO(boolean inc, int h, int vo, int o,
                                      HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshvo){
            Integer c1=-1;
            HashMap<Integer, Integer> o2co;
            HashMap<Integer, HashMap<Integer, Integer>> vo2co = countshvo.get(h);
            if (vo2co==null) {
                    o2co=new HashMap<Integer, Integer>();
                    o2co.put(-1, 0); // total for one h over all d
                    vo2co=new HashMap<Integer, HashMap<Integer, Integer>>();
                    vo2co.put(vo, o2co);
                    countshvo.put(h, vo2co);
                    c1=0;
            }else{
                o2co=vo2co.get(vo);
                if (o2co==null){
                    o2co=new HashMap<Integer, Integer>();
                    o2co.put(-1,0);
                    vo2co.put(vo, o2co);
                    countshvo.put(h, vo2co);
                    c1=0;
                }
            }
            if(c1==-1) c1=o2co.get(o);

            if (inc) {
                    if (c1==null) c1=1; else c1++;
                    o2co.put(o, c1);
                    int tot=o2co.get(-1);
                    o2co.put(-1, ++tot);
            } else {
                    c1--;
                    if (c1==0) o2co.remove(o);
                    else if (c1<0) System.out.println("ERROR negative counts updateCountsHVoO!");
                    o2co.put(o, c1);
                    int tot=o2co.get(-1);
                    o2co.put(-1, --tot);
                    if (tot<0) System.out.println("ERROR negative counts tot updateCountsHVoO!");
                    assert tot>=0;
            }
            vo2co.put(vo,o2co);
            countshvo.put(h, vo2co);

        }

        private void updateCountsHOA(boolean inc, int h, int o, int[]as,
                                      HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshoa){
            HashMap<Integer, Integer> a2co;
            HashMap<Integer, HashMap<Integer, Integer>> oa2co=countshoa.get(h);;
                if (oa2co==null){
                    a2co=new HashMap<Integer, Integer>();
                    a2co.put(-1, 0);
                    oa2co=new HashMap<Integer, HashMap<Integer, Integer>>();
                    oa2co.put(o, a2co);
                    countshoa.put(h,oa2co);
                }else{
                    a2co=oa2co.get(o);
                    if (a2co==null){
                        a2co=new HashMap<Integer, Integer>();
                        a2co.put(-1, 0);
                        oa2co.put(o, a2co);
                    }
                }
            for (int a:as){
                Integer c2=a2co.get(a);
                if (inc) {
                        if (c2==null) c2=1; else c2++;
                        a2co.put(a, c2);
                        int tot=a2co.get(-1);
                        a2co.put(-1, ++tot);
                } else {
                        c2--;
                        if (c2==0) a2co.remove(a);
                        else if (c2<0) System.out.println("ERROR negative counts updateCountsHVoOA!");
                        a2co.put(a, c2);
                        int tot=a2co.get(-1);
                        a2co.put(-1, --tot);
                        if (tot<0) System.out.println("ERROR negative counts tot updateCountsHVoOA!");
                        assert tot>=0;
                }
            }

        }

        private void updateCountsHAW(boolean inc, int h, int[] as, int[] ws,
                                      HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshaw){
            HashMap<Integer, HashMap<Integer,Integer>> aw2co= countshaw.get(h);
            HashMap<Integer,Integer> w2co;
            if (aw2co==null){
                aw2co= new HashMap<Integer, HashMap<Integer, Integer>>();
                for (int a:as){
                     w2co=new HashMap<Integer, Integer>();
                     w2co.put (-1,0);
                     aw2co.put(a,w2co);
                }
                countshaw.put(h, aw2co);
            }else{
                for (int a:as){
                    w2co=aw2co.get(a);
                    if (w2co==null){
                        w2co=new HashMap<Integer, Integer>();
                        w2co.put(-1,0);
                        aw2co.put(a,w2co);
                    }
                }
            }
            for (int ind=0;ind<as.length;ind++){
                int a=as[ind];
                int w=ws[ind];
                w2co=aw2co.get(a);
                Integer c2=w2co.get(w);
                if (inc) {
                        if (c2==null) c2=1; else c2++;
                        w2co.put(w, c2);
                        int tot=w2co.get(-1);
                        w2co.put(-1, ++tot);
                } else {
                        c2--;
                        if (c2==0) w2co.remove(w);
                        else if (c2<0) System.out.println("ERROR negative counts updateCountsHAW!");
                        w2co.put(w, c2);
                        int tot=w2co.get(-1);
                        w2co.put(-1, --tot);
                        if (tot<0) System.out.println("ERROR negative counts tot updateCountsHAW!");
                        assert tot>=0;
                }

            }
        }

        private void updateCountsVoARp(boolean inc,  int vo, int[] as, int[] rps,
                                     HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> countsvar){
            HashMap<Integer,Integer> r2co;
            HashMap<Integer, HashMap<Integer,Integer>> ar2co=countsvar.get(vo);
                if (ar2co==null){
                    ar2co=new HashMap<Integer, HashMap<Integer,Integer>>();
                    for (int a:as){
                         r2co=new HashMap<Integer, Integer>();
                         r2co.put (-1,0);
                         ar2co.put(a,r2co);
                    }
                    countsvar.put(vo, ar2co);
                }else{
                    for (int a:as){
                        r2co=ar2co.get(a);
                        if (r2co==null){
                            r2co=new HashMap<Integer, Integer>();
                            r2co.put(-1, 0);
                            ar2co.put(a,r2co);
                        }
                    }
                }
            for (int ind=0;ind<as.length;ind++){
                int a=as[ind];
                int r=rps[ind];
                r2co=ar2co.get(a);
                if (r2co==null){
                    r2co=new HashMap<Integer, Integer>();
                     r2co.put (-1,0);
                     ar2co.put(a,r2co);
                }
                Integer c2=r2co.get(r);
                if (inc) {
                        if (c2==null) c2=1; else c2++;
                        r2co.put(r, c2);
                        int tot=r2co.get(-1);
                        r2co.put(-1, ++tot);
                } else {
                        c2--;
                        if (c2==0) r2co.remove(r);
                        else if (c2<0) System.out.println("ERROR negative counts updateCountsHVoARp!");
                        r2co.put(r, c2);
                        int tot=r2co.get(-1);
                        r2co.put(-1, --tot);
                        if (tot<0) System.out.println("ERROR negative counts tot updateCountsHVoARp!");
                        assert tot>=0;
                }

            }
        }
        
        
        private boolean isPredicate(DetGraph g, int i){
            DetGraph gsr= g.relatedGraphs.get(0);
            if (gsr==lastgComputedSRL) return predicates.contains(i);
            precomputeStructuresSRL(g);
            return predicates.contains(i);
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


        int getVoice(DetGraph g, int i) {
            DetGraph gsr= g.relatedGraphs.get(0);
            //get the position of the predicate in the array
            int pos=predicates.indexOf(i);
            if (gsr==lastgComputedSRL) return voices[pos];
            precomputeStructuresSRL(g);
            pos=predicates.indexOf(i);
            return voices[pos];
        }

        /**
         * Return the order in which the primary roles appear wrt the predicate
         * where primary roles are: A0, A1
         * so it can return one of the following orders
         * A0 A1 P, A0 P A1, P A0 A1,
         * A1 A0 P, A1 P A0, P A1 A0,
         * A0 P, P A0,
         * A1 P, P A1
         * @param gsrl
         * @param i
         * @return
         */
        int getOrder(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
            int pos=predicates.indexOf(i);
		if (gsrl==lastgComputed) return ordsPerPred[pos];
		precomputeStructuresSRL(g);
                pos=predicates.indexOf(i);
		return ordsPerPred[pos];
        }

        /**
         *
         * @param gsrl: the graph for the semantic role labelling part
         * @param i; the position of the predicate, we asume it's a predicate (todo see if it's ok)
         * @return an array with the arguments of the predicate (the ids of the roles)
         */
        int[] getArgs(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
            int pos=predicates.indexOf(i);
		if (gsrl==lastgComputed) return argsPerPred[pos];
		precomputeStructuresSRL(g);
		pos=predicates.indexOf(i);
                return argsPerPred[pos];
        }

        int[] getWordsSRL(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
            int pos=predicates.indexOf(i);
		if (gsrl==lastgComputed) return wasPerPred[pos];
		precomputeStructuresSRL(g);
                pos=predicates.indexOf(i);
		return wasPerPred[pos];
        }

        int[] getRelposSRL(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
            int pos=predicates.indexOf(i);
		if (gsrl==lastgComputed) return rpsPerPred[pos];
		precomputeStructuresSRL(g);
                pos=predicates.indexOf(i);
		return rpsPerPred[pos];
        }
    
    
   /***************************************************************************************************************
   Normalization to make the number of dependents the same for all graphs: 
   * TODO: see how to model this, we should somehow nivel the number of dependents for all the graphs... 
   * 
   * TODO: solution make as it were only one dependent: so square root of n (or n') --> in log is:
   * 1/n ....
   * 
   * 
    ***************************************************************************************************************/
        private int getNumArgss(DetGraph g, boolean semantic){
            int num=0;
            DetGraph gaux=(semantic)? g.relatedGraphs.get(0):g;
            for (int i=0;i<gaux.getNbMots();i++){
                if (semantic){
                    boolean isPred=SemanticProcesing.isPredicate(gaux, i);
                    if (isPred){
                        List<Integer> childre= gaux.getFils(i);
                        num+=childre.size();
                    }
                }else{
                    List<Integer> childre= gaux.getFils(i);
                    num+=childre.size();
                }
                
            }
            return num;
        }

        private int[]getLowHi(int[] values){
            int[] res=new int[values.length];
            int hi=0;
            for (int ind=0;ind<values.length;ind++){
                if(values[ind]> hi)hi=values[ind];
            }
            for (int ind=0;ind<values.length;ind++){
                res[ind]=hi-values[ind];
            }
            return res;
        }

        
        
   /***************************************************************************************************************
                                            Get log post for Sampling
    ***************************************************************************************************************/
            /**
         * //term3=sum_h[sum_vo[(sum_o sum_i log(no + alpha + i)-(sum_i log(sum_o (no+alpha+i)))]]
         * @param h
         * @param countsHVoOu
         * @return 
         */
        private double calculTerm3(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoOu){
            double t31=0;
            double t32=0;
            for(int h:countsHVoOu.keySet()){
                HashMap<Integer,HashMap<Integer,Integer>> vo2co = countsHVoOu.get(h);
                if (vo2co!=null){
                    for (int v:vo2co.keySet()){
                        int totnou=0;
                        //numerator
                        HashMap<Integer,Integer> o2co=vo2co.get(v);
                        for (int o:o2co.keySet() ){
                            if (o<0) continue;
                            int nou=o2co.get(o);
                            totnou+=nou;
                            for (int i=0; i<nou;i++){
                                //log
                                t31+= Math.log(getAlphaO(o)+getCount4(countsHVoO, h, v, o)+i);
                            }
                        }
                        //denominator
                        //we assume symetric alpha
                        double AO = getAlphaO(0)*getOsize();
                        int nsum=getTotal4(countsHVoO, h,v);
                        for (int i=0;i<totnou;i++){
                            t32 -= Math.log(AO+nsum+i);
                        }
                    }
                }
            }
            double term3 = t31+t32;
            return term3;
        }
        
        private double calculTerm4(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHOAu){
                double t41=0;
                double t42=0;
            for(int h:countsHOAu.keySet()){
                   HashMap<Integer, HashMap<Integer, Integer>>oa2co=countsHOAu.get(h);
                    for (int o: oa2co.keySet()){
                        //numerator
                        int totnau=0;
                        HashMap<Integer,Integer> a2co=oa2co.get(o);
                        for (int a:a2co.keySet()){
                            int nau=a2co.get(a);
                            totnau+=nau;
                            for (int i=0;i<nau;i++){
                                t41+=Math.log(getAlphaA(0)+getCount4(countsHOA, h, o, a)+i);
                            }
                        }
                        //denominator
                        double AA = getAlphaA(0)*getAsize();
                        int nsum=getTotal4(countsHOA, h,o);
                        for (int i=0;i<totnau;i++){
                            t42 -= Math.log(AA+nsum+i);
                        }
                    }
                }
                double term4 = t41+t42;
                return term4;
        }
        
        /**
         * //term4=sum_h[sum_a[(sum_w sum_i log(nwa + alpha + i)-(sum_i log(sum_w (nwa+alpha+i)))]]
         * @param h
         * @param countsHAWau
         * @return 
         */
        private double calculTerm5(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWau){
            double t41=0;
            double t42=0;
            for(int h:countsHAWau.keySet()){
                HashMap<Integer,HashMap<Integer, Integer>>aw2co= countsHAWau.get(h);
                if (aw2co!=null){
                    for (int a:aw2co.keySet()){
                        HashMap<Integer, Integer> w2co=aw2co.get(a);
                        int totnwu=0;
                        //numerator
                        for(int w:w2co.keySet()){
                            int nwu=w2co.get(w);
                            totnwu+=nwu;
                            for (int i=0;i<nwu;i++){
                                double alpha=getAlphaWa(0);//aux structure to re-use the procedure
                                t41+=Math.log(alpha+getCount4(countsHAWa, h, a, w)+i);
                            }
                        }
                        //denominator
                        double alpha= getAlphaWa(0);
                        double AWA=alpha*getWasize();
                        int nsum=getTotal4(countsHAWa, h, a);
                        for (int i=0; i<totnwu;i++){
                            t42 -= Math.log(AWA+nsum+i);
                        }
                    }
                }
            }
            double term4=t41+t42;
            return term4;
        }
 
        private double calculTerm6(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoARpu){
                       double t61=0;
                        double t62=0;
                        for (int v:countsVoARpu.keySet()){
                            HashMap<Integer,HashMap<Integer,Integer>> ar2co=countsVoARpu.get(v);
                            for (int a:ar2co.keySet()){
                                HashMap<Integer, Integer>r2co=ar2co.get(a);
                                int totnru=0;
                                //numerator
                                for (int r: r2co.keySet()){
                                    int nru=r2co.get(r);
                                    totnru+=nru;
                                    for (int i=0;i<nru;i++){
                                        t61+=Math.log(getAlphaRp(0)+getCount4(countsVoARp, v, a, r)+i);
                                    }
                                }
                                //denominator
                                double AR=getAlphaRp(0)*getRPsize();
                                int nsum=getTotal4(countsVoARp,v, a);
                                for (int i=0;i<totnru;i++){
                                    t62-=Math.log(AR+nsum+i);
                                }
                            }
                        }
                        double term6=t61+t62;
                        return term6;
        }        
        

    // rules contient toutes les regles qui ont ete appliquees sur cette phrase.
    // La meme regle peut apparaitre plusieurs fois dans la liste, si elle a ete appliquee a plusieurs positions
	// rules[0]=regle, rules[1]=position
	public double getLogPost(DetGraph g) {
            //get the number of 
            double res=0;

		// ------------------------------------------------
		// counts n^u_{h,d} and n^u_{h,w} for the current sentence
		// (the same counts n^{-u}_{h,d} for the rest of the corpus are in countsHD and countsHW)
		HashMap<Integer, HashMap<Integer, Integer>> countsHDu = new HashMap<Integer, HashMap<Integer,Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> countsHWu = new HashMap<Integer, HashMap<Integer,Integer>>();
                //semantic part
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoOu = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHOAu = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWau = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoARpu = new HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>>();


		updateCounts(true, g, countsHDu, countsHWu, countsHVoOu,countsHOAu,countsHAWau,countsVoARpu);

		// on doit avoir exactement les memes h dans les 2 hashmaps
		assert countsHDu.size()==countsHWu.size();
                assert countsHVoOu.size()==countsHDu.size();
                assert countsHDu.size()==countsHAWau.size();
                assert countsHAWau.size()==countsHOAu.size();
                assert countsHOAu.size()==countsVoARpu.size();
                double term1=0,term2=0,term3=0,term4=0,term5=0,term6=0;

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
			term1+= t1a+t1b;
                }
                for (int h : countsHWu.keySet()) {
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
			int nsum = getTotal(countsHW,h);
			for (int i=0;i<nhutot2;i++) {
				t2a -= Math.log(AW+nsum+i);
			}
			// Attention ! Une phrase sans aucune dep aura 0 fils, donc term2 est calculé avec un nombre variable de counts totaux,
			// ce qui peut poser pb pour sampler/comparer les posterior entre eux !
			term2+= t2a+t2b;
                }
                        //semantic part:
                //term3=sum_h[sum_vo[(sum_o sum_i log(no + alpha + i)-(sum_i log(sum_o (no+alpha+i)))]]
                term3+=calculTerm3(countsHVoOu);
                //term4=sum_h[sum_vo[sum_o[(sum_a sum_i log(na + alpha + i)-(sum_i log(sum_a(na+alpha+i)))]]]
                term4+=calculTerm4(countsHOAu);
                //term5=sum_h[sum_a[(sum_w sum_i log(nwa + alpha + i)-(sum_i log(sum_w (nwa+alpha+i)))]]
                term5+=calculTerm5(countsHAWau);
                 //term6=sum_h[sum_vo[sum_a[(sum_rp (sum_i log(nrp + alpha + i)-(sum_i log(sum_rp(nrp+alpha+i)))]]]
                term6+=calculTerm6(countsVoARpu);
                double syntp = term1+term2;
                double semp  = term3+term4+term5+term6;
                        res+=syntp+semp;
		return res;
	}
        
        int getCount(HashMap<Integer,HashMap<Integer, Integer>> counts, int x, int y) {
		HashMap<Integer, Integer> co = counts.get(x);
		if (co==null) return 0;
		Integer n = co.get(y);
		if (n==null) return 0;
		return n;
	}

	int getCount4(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> counts, int x, int y, int z) {
		HashMap<Integer, HashMap<Integer, Integer>> co = counts.get(x);
		if (co==null) return 0;
                HashMap<Integer, Integer> co2 = co.get(y);
                if (co2==null) return 0;
		Integer n = co2.get(z);
		if (n==null) return 0;
		return n;
	}
        
        int getTotal4(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> counts, int x, int y) {
		HashMap<Integer, HashMap<Integer, Integer>> co1 = counts.get(x);
		if (co1==null) return 0;
                HashMap<Integer, Integer> co= co1.get(y);
                if (co==null) return 0;
		Integer n = co.get(-1);
		assert n!=null; // otherwise, that's because it has not been correctly updated in inc/dec...
		return n;
	}
        
        
        int getTotal(HashMap<Integer,HashMap<Integer, Integer>> counts, int x) {
		HashMap<Integer, Integer> co = counts.get(x);
		if (co==null) return 0;
		Integer n = co.get(-1);
		assert n!=null; // otherwise, that's because it has not been correctly updated in inc/dec...
		return n;
	}
        
        
        
        private int getDsize(){
                return maxndeps*maxndeps*maxndeps;
        }
        private int getWsize(){
                return getVocSize();
        }

        private int getOsize() {
        OrderRoles o=new OrderRoles();
        return o.getNumOrders();
        }

        private int getAsize() {
        return numArgs;
        }

        private int getWasize() {
        return getVocSize();
        }

        private int getRPsize() {
        return 2;
        }

        
        
        
    /***************************************************************************************************************
                                            methods for precomputing structures (used by updateCounts)
    ***************************************************************************************************************/
    
        protected int[] getLabs2track() {
            final int[] labs = {
                            Dep.getType("MOD"),
                            Dep.getType("SUJ"),Dep.getType("OBJ"),Dep.getType("POBJ")};
            return labs;
        }

        //syntax
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
						if (dep.type==getLabs2track()[k]) {//if (dep.type==labs[k]) {
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
        
        //semantics 
	private void precomputeStructuresSRL(DetGraph g){
            boolean debug=false;
            DetGraph gsrl= g.relatedGraphs.get(0);
            lastgComputedSRL=gsrl;
            ArrayList<ArrayList<Integer>> args=new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> wors=new ArrayList<ArrayList<Integer>>();
            ArrayList<ArrayList<Integer>> rps=new ArrayList<ArrayList<Integer>>();
            ArrayList<Integer> ors=new ArrayList<Integer>();
            ArrayList<Integer> vois=new ArrayList<Integer>();
            predicates= new ArrayList<Integer>();
            for (int i=0;i<g.getNbMots();i++){
                int v=isPredicateAux(g, i);
                if (v>=0){
                    List<Integer> childre= gsrl.getFils(i);
                    if (childre.size()>0){
                        //add to predicates 
                        predicates.add(i);
                        vois.add(v);
                        ArrayList<Integer> as=new ArrayList<Integer>();
                        ArrayList<Integer> ws=new ArrayList<Integer>();
                        ArrayList<Integer> ps=new ArrayList<Integer>();
                        String dstr;
                        String ord="";
                        boolean writePred=false;
                        if (debug && childre.size()>0)System.out.println(g+"\npredicate:"+i);
                        for (int c:childre){
                            int[] ds=gsrl.getDeps(c);
                            dstr=gsrl.getDepLabel(gsrl.getDep(c));
                            //load arguments
                            for (int d:ds){
                                if (gsrl.getHead(d)==i) dstr=gsrl.getDepLabel(d);
                            }
                            assert(roles.contains(dstr));
                            if (debug && childre.size()>0)System.out.println("childre:"+c+" - "+dstr+" hasta ahora:"+ord);
                            int indgold= roles.indexOf(dstr);
                            as.add(indgold); 
                            //load words
                            int ind=voc.getWordIndex(gsrl.getMot(c));
                            ws.add(ind);
                            //load relative pos
                            if (c<i) ps.add(rpleft);
                            else ps.add(rpright);
                            //load orders
                            if ((dstr.equals("A0")||dstr.equals("A1"))&&ord.length()<=7){
                                if ((c>i||c<i) && writePred)
                                    ord+=dstr+"-";
                                if (c<i && !writePred){
                                    ord+=dstr+"-P-";
                                    writePred=true;
                                }
                                if(c>i && !writePred){
                                    ord+="P-"+dstr+"-";
                                    writePred=true;
                                }
                            }

                        }
                        if (ord.isEmpty()) ord="P";
                        //remove the last "-";
                        if (ord.endsWith("-")){
                            ord= ord.substring(0, ord.length()-1);
                        }
                        OrderRoles o=new OrderRoles();
                        if (debug)System.out.println("g:"+g+" verb:"+i);
                        if (debug)System.out.println("order:"+ord);
                        ors.add(o.getOrderId(ord));
                        //add the args, words and relative pos
                        args.add(as);
                        wors.add(ws);
                        rps.add(ps);
                    }
                }
            }
            //pass everything to arrays 
            if (ors.size()>0){
                ordsPerPred=new int[ors.size()];
                for (int j=0;j<ors.size();j++){ ordsPerPred[j]=ors.get(j);}
            }
            if (args.size()>0){
                argsPerPred= new int[predicates.size()][];
                for (int i=0;i<predicates.size();i++){
                    int[] ass=new int[args.get(i).size()];
                    for (int j=0;j<args.get(i).size();j++){ 
                        ass[j]=args.get(i).get(j);
                    }
                    argsPerPred[i]=ass;
                }
            }
            if (wors.size()>0){
                wasPerPred=new int[predicates.size()][];
                for (int i=0;i<predicates.size();i++){
                    int[] wss=new int[wors.get(i).size()];
                    for (int j=0;j<wors.get(i).size();j++){ 
                        wss[j]=wors.get(i).get(j);
                    }
                    wasPerPred[i]=wss;
                }
            }
            if (rps.size()>0){
                rpsPerPred=new int[predicates.size()][];
                for (int i=0;i<predicates.size();i++){
                    int[] rss=new int[rps.get(i).size()];
                    for (int j=0;j<rps.get(i).size();j++){ 
                        rss[j]=rps.get(i).get(j);
                    }
                    rpsPerPred[i]=rss;
                }
            }
            if(vois.size()>0){
                voices=new int[vois.size()];
                assert (vois.size()==predicates.size());
                for (int j=0;j<vois.size();j++){ voices[j]=vois.get(j);}
            }
        
        }
        
        //Checks if the word is a predicate, and loads the voice variable
        private int isPredicateAux(DetGraph g, int i){
            int voice=-1;
            String posV= g.getMot(i).getPOS();
            //verbal predicates don't include "to be"
            if (SemanticProcesing.isPredicate(g, i)){
                voice = vact;
                //evaluate the voice:
                    if (posV.equals("VBN")){
                        for (int pv=i; pv>0; pv--){
                            if (g.getMot(pv).getPOS().startsWith("VB")){
                                String lempv=getLemmafromForm(g.getMot(pv).getForme());
                                if (lempv.startsWith("be")) {
                                    voice=vpas;
                                    break;
                                }else if (lempv.startsWith("have")) {
                                    voice=vact;
                                    break;
                                }
                            }
                        }
                        if (voice == -1 ) {
                            voice =vact;
                            if ( (i+1)< g.getNbMots()){
                                String l=getLemmafromForm(g.getMot(i+1).getForme());
                               //if ( g.getMot(i+1).getLemme().startsWith("by"))
                                if ( l.startsWith("by"))
                                     voice=vpas;
                            }
                        }
                    }//else voice = vact;
            }
            return voice;
        }
        
        public String getLemmafromForm(String f){
            String lem=f;
            boolean found=false;
            String[] beLems={"am","'m","are","'re","is","'s","was","were","been","being"};
            String[] doLems={"do","does","did","done","doing"};
            String[] haveLems={"have","has","had","having"};
            for (String l:beLems){
                if (l.equals(f)) {
                    lem="be";
                    found=true;
                    break;
                }
            }
            if (!found){
                for (String l:doLems){
                    if (l.equals(f)) {
                        lem="do";
                        found=true;
                        break;
                    }
                }
            }
            if (!found){
                for (String l:haveLems){
                    if (l.equals(f)) {
                        lem="have";
                        found=true;
                        break;
                    }
                }
            }
            return lem;
        }


    
        
   /***************************************************************************************************************/
    
    
}
