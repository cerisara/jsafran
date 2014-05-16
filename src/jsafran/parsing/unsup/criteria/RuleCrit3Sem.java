package jsafran.parsing.unsup.criteria;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.management.relation.RoleStatus;
import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;
import jsafran.parsing.unsup.UnsupParsing;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.OrderRoles;
import jsafran.parsing.unsup.SemanticProcesing;
import jsafran.parsing.unsup.rules.Rules;


/**
 *
 * This class implements the "Frame model" back
 *
 * @author xtof
 * Ale: TODO:
 *  1) implement the functions get... for semantic
 *
 */
public class RuleCrit3Sem {
    boolean french=true;
    boolean debugNanlog=true;
	int nrules=0;
	Rules rules;
	Voc voc;
        int numArgs=6; //start by considering A0, A1, A2, A3, A4, AM
//        HashMap<String, Integer> rolesDict= new HashMap<String, Integer>();
//        HashMap<Integer,String> invrolesDict= new HashMap<Integer,String>();
        private String[]rolarr={"A0","A1","A2","A3","A4","AM"};
        public ArrayList<String>roles=new ArrayList<String>();


        int vact=0;
        int vpas=1;
        int rpleft=0;
        int rpright=1;

	HashSet<String> aobjverbs = new HashSet<String>();
	HashSet<String> deobjverbs = new HashSet<String>();
	HashSet<String> objverbs = new HashSet<String>();
	final int nverblists = 4; // +1 car c'est le nb de valeurs que peut prendre cette feature

	DetTopology toposcore = new DetTopology();

	int[] rule4utt;

	// the counts:
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


	// for debugging !
        //semantic part TODO!!!
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
                
                //semantic part
                n=0; nn=0;
                for (int h:countsHVoO.keySet()){
                    for (int v:countsHVoO.get(h).keySet()){
                        int nnn=0;
                        n+=countsHVoO.get(h).get(v).get(-1);
                        for (int o:countsHVoO.get(h).get(v).keySet()){
                            if (o>=0) nnn+=countsHVoO.get(h).get(v).get(o);
                        }
                        if (nnn!=countsHVoO.get(h).get(v).get(-1))
                            System.out.println("ERROR TOT!!!!!!! countsHVoO nnn="+nnn+" counts in -1="+countsHVoO.get(h).get(v).get(-1));
                        nn+=nnn;
                    }
                }
                System.out.println("TOT COUNTS countsHVoO "+n+" "+nn);
                n=0; nn=0;
                for ( int h:countsHOA.keySet()){
                        for (int o:countsHOA.get(h).keySet()){
                            int nnn=0;
                            n+=countsHOA.get(h).get(o).get(-1);
                            for (int a:countsHOA.get(h).get(o).keySet()){
                                 if (a>=0) nnn+=countsHOA.get(h).get(o).get(a);
                            }
                            nn+=nnn;
                            if (nnn!=countsHOA.get(h).get(o).get(-1))
                                System.out.println("ERROR TOT!!!!!!! countsHVoOA nnn="+nnn+" counts in -1= "+countsHOA.get(h).get(o).get(-1));
                            
                        }
                }
                System.out.println("TOT COUNTS countsHVoOA "+n+" "+nn);
                n=0; nn=0;
                for ( int h:countsHAWa.keySet()){
                    for (int a:countsHAWa.get(h).keySet()){
                            int nnn=0;
                            n+=countsHAWa.get(h).get(a).get(-1);
                            for (int wa:countsHAWa.get(h).get(a).keySet()){
                                if (wa>=0) nnn+=countsHAWa.get(h).get(a).get(wa);
                            }
                            nn+=nnn;
                            if (nnn!=countsHAWa.get(h).get(a).get(-1))
                                System.out.println("ERROR TOT!!!!!!! countsHAWa  nnn="+nnn+" counts in -1="+countsHAWa.get(h).get(a).get(-1));
                     }
                 }
                

                n=0; nn=0;
                    for (int v:countsVoARp.keySet()){
                        for (int o:countsVoARp.get(v).keySet()){
                            int nnn=0;
                            n+=countsVoARp.get(v).get(o).get(-1);
                            for (int a:countsVoARp.get(v).get(o).keySet()){
                                 if (a>=0)nnn+=countsVoARp.get(v).get(o).get(a);
                            }
                            nn+=nnn;
                            if (nnn!=countsVoARp.get(v).get(o).get(-1))
                                System.out.println("ERROR TOT!!!!!!! countsHVoARp  nnn="+nnn+" counts in -1="+countsVoARp.get(v).get(o).get(-1));
                        }
                    }
                System.out.println("TOT COUNTS countsHVoARp "+n+" "+nn);
                
                
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

	public RuleCrit3Sem(Rules r, Voc v) {
		/*
		loadList("res/aobj-verbs.txt",aobjverbs);
		loadList("res/deobj-verbs.txt",deobjverbs);
		loadList("res/obj-verbs.txt",objverbs);
		 */
		voc=v;
		rules=r;
                ndeps = new int[getLabs2track().length];
                //load the roles
                for (int i=0;i<rolarr.length;i++){
                    roles.add(rolarr[i]);
                }
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

        /**
         * Checks if the word is a predicate, and loads the voice variable
         * @param g
         * @param i
         * @return 
         */
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


        int getTotal(HashMap<Integer,HashMap<Integer, Integer>> counts, int x) {
		HashMap<Integer, Integer> co = counts.get(x);
		if (co==null) return 0;
		Integer n = co.get(-1);
		assert n!=null; // otherwise, that's because it has not been correctly updated in inc/dec...
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

        int getTotal5(HashMap<Integer,HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>> counts, int x, int y, int z) {
                HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>co2=counts.get(x);
                if (co2==null) return 0;
                HashMap<Integer, HashMap<Integer, Integer>> co1 = co2.get(y);
		if (co1==null) return 0;
                HashMap<Integer, Integer> co= co1.get(z);
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

	int getCount4(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> counts, int x, int y, int z) {
		HashMap<Integer, HashMap<Integer, Integer>> co = counts.get(x);
		if (co==null) return 0;
                HashMap<Integer, Integer> co2 = co.get(y);
                if (co2==null) return 0;
		Integer n = co2.get(z);
		if (n==null) return 0;
		return n;
	}

	int getCount5(HashMap<Integer,HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>> counts, int x, int y, int z, int w) {
		HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> co = counts.get(x);
		if (co==null) return 0;
		HashMap<Integer, HashMap<Integer, Integer>> co2 = co.get(y);
		if (co2==null) return 0;
                HashMap<Integer, Integer> co3 = co2.get(z);
                if (co3==null) return 0;
		Integer n = co3.get(w);
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
            //HashMap<Integer, HashMap<Integer, Integer>> voa2co = countshoa.get(h);
//            if (voa2co==null) {
//                oa2co = new HashMap<Integer, HashMap<Integer, Integer>>();
//                a2co= new HashMap<Integer, Integer>();
//                a2co.put(-1, 0);
//                oa2co.put(o, a2co);
//                voa2co=new HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>();
//                voa2co.put(vo, oa2co);
//                countshvoa.put(h, voa2co);
//            }else{
//                oa2co=voa2co.get(vo);
                if (oa2co==null){
                    a2co=new HashMap<Integer, Integer>();
                    a2co.put(-1, 0);
                    oa2co=new HashMap<Integer, HashMap<Integer, Integer>>();
                    oa2co.put(o, a2co);
//                    voa2co.put(vo, oa2co);
                    countshoa.put(h,oa2co);
                }else{
                    a2co=oa2co.get(o);
                    if (a2co==null){
                        a2co=new HashMap<Integer, Integer>();
                        a2co.put(-1, 0);
                        oa2co.put(o, a2co);
                    }
                }
//            }
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
            //HashMap<Integer, HashMap<Integer, HashMap<Integer,Integer>>> var2co= countshvar.get(h);
            HashMap<Integer,Integer> r2co;
            HashMap<Integer, HashMap<Integer,Integer>> ar2co=countsvar.get(vo);
//            if (var2co==null){
//                var2co= new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
//                ar2co= new HashMap<Integer, HashMap<Integer,Integer>>();
//                for (int r:rps){
//                     r2co=new HashMap<Integer, Integer>();
//                     r2co.put (-1,0);
//                     ar2co.put(r,r2co);
//                }
//                var2co.put(vo, ar2co);
//                countshvar.put(h, var2co);
//            }else{
//                ar2co=var2co.get(vo);
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
//            }
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
//                            if (countshvar==countsHVoARp)
//System.out.println("debug updateco "+inc+" "+h+" "+vo+" "+Arrays.toString(as)+" "+Arrays.toString(ras)+" "+g);
//                            System.out.println("debug cotables "+countshvar);
                            updateCountsVoARp(inc, vo, as, ras, countsvar);
                        }
		}

	}

        private void getEntropys(List<DetGraph> gs){
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
                //entropy for the semantic par??
                double oentropy=0;
                for (int h: countsHVoO.keySet()){
                    //TODO
                }
                double aentropy=0;
                for (int h: countsHOA.keySet()){
                    //TODO
                }
                double waentropy=0;
                for (int h: countsHAWa.keySet()){
                    //TODO
                }
                double rpentropy=0;
                for (int v: countsVoARp.keySet()){
                    //TODO
                }
		System.out.println("entropy: "+dentropy+" "+wentropy);
        }
        
        private double termTotalLogPost(int h,  HashMap<Integer,HashMap<Integer,Integer>> theta2, int hi, HashMap<Integer,HashMap<Integer,Integer>> countsu ){
            
            HashMap<Integer, Integer> theta ;
            double res=0;
            if (countsu!=null ){
                for (int v:countsu.keySet()){
                    theta=theta2.get(v);
                    for (int o:countsu.get(v).keySet()){
                        if (o>=0){
//                            double like = Math.log((double)theta.get(o)/(double)theta.get(-1));
//                            like *= (double)theta.get(o);
                            double like=ldf_Mult(true, o, theta, 0, hi);
                            res+=like*(double)countsu.get(v).get(o);
                        }
                    }
                }
            }
            return res;
        }

        /*******************************************************************
         * stats functions!! TODO: create a class??
         * @param x
         * @return 
         */
        private double boundprob(double x){
            return (((x)<-300)?(-300):((((x)>300)?(300):(x))));
        }

        public double ldf_Mult(boolean normalize, int x, HashMap<Integer,Integer> th,int lo, int hi){
            double res=0;
            if ((x<lo)||(x>hi)) { res= -0/1; }
            else{
                //double s=0;
                //for (int i=0; i<hi-lo; i++) { s+=th[i]; }
                //the sum should be in the th.get(-1)??? 
                double s=th.get(-1);
                
                if (s==0) {
                    return 0;
//                    String q=null;
//                    q.charAt(0);
                }
                
                res=boundprob(Math.log(th.get(x-lo))-Math.log(s));
                if (debugNanlog){
                    if (Double.isInfinite(res) || Double.isInfinite(s) )
                            System.out.println("Infinite: s:"+s+"    res:"+res);
                    if (Double.isNaN(res) || Double.isNaN(s) )
                            System.out.println("Nan: s:"+s+"    res:"+res);
                }
            }
            return res;
        }
        
        private double ldf_Mult_smooth(boolean normalize, double eta,int x, HashMap<Integer,Integer> th,int lo, int hi){
        //private double ldf_Mult_smooth(boolean normalize, double eta,int x, int[]th,int lo, int hi){
            double res=0;
            if ((x<lo)||(x>hi)) { res= -0/1; }
            else{
                double s=th.get(-1)+ eta*(hi-lo);
                //for (int i=0; i<hi-lo; i++) { s+=th[i]; }
                //the sum should be in the th.get(-1)??? 
                res=boundprob(Math.log(th.get(x-lo)+eta)-Math.log(s));
            }
            return res;
        }
        /*****************************************************************/

        static void testUnit() {
            GraphIO gio = new GraphIO(null);
            gio.loadAllGraphs("");
        }

	public double[] getTotalLogPost(List<DetGraph> gs,ArrayList<Double> hilow) {
            debugCounts();
		double[] res=new double[3];
                double terms=0;
                double termsSynt=0;
                double termsSem=0;
		for (int indgr=0;indgr<gs.size();indgr++) {
                    double term1=0;
                    double term2=0;
                    double term3=0;
                    double term4=0;
                    double term5=0;
                    double term6=0;
//                        DetGraph gsrl=gssrl.get(graphindex);
//                    DetGraph gsrl=gs.relatedGraphs.get(0);
                        DetGraph g=gs.get(indgr);
                        //syntactic part
			HashMap<Integer, HashMap<Integer, Integer>> countsHDu = new HashMap<Integer, HashMap<Integer,Integer>>();
			HashMap<Integer, HashMap<Integer, Integer>> countsHWu = new HashMap<Integer, HashMap<Integer,Integer>>();
                        //semantic part
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoOu = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHOAu = new HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWau = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoARpu = new HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>>();

			updateCounts(true, g, countsHDu, countsHWu, countsHVoOu,countsHOAu,countsHAWau,countsVoARpu);
                        //term1
			for (int h : countsHDu.keySet()) {
				HashMap<Integer, Integer> theta = countsHD.get(h);
				if (theta!=null&&theta.get(-1)>0)
					for (int d : countsHDu.get(h).keySet()) {
						if (d>=0) {
//							double like = Math.log((double)theta.get(d))-Math.log((double)theta.get(-1));
//							like *= (double)countsHDu.get(h).get(d);

                                                    double like = ldf_Mult(true, d, theta, 0, maxndeps);
							term1+=like*(double)countsHDu.get(h).get(d);
                                                        if (debugNanlog){
                                                            if (Double.isInfinite(term1) || Double.isNaN(term1) )
                                                                    System.out.println("countsHD Error term1:"+term1); break;
                                                        }
						}
					}
                        }
                        //term2
                        for (int h : countsHWu.keySet()) {
				HashMap<Integer,Integer>theta = countsHW.get(h);
				if (theta!=null&&theta.get(-1)>0)
					for (int w : countsHWu.get(h).keySet()) {
						if (w>=0) {
//							double like = Math.log((double)theta.get(w)/(double)theta.get(-1));
//							like *= (double)countsHWu.get(h).get(w);
                                                        double like=ldf_Mult(true, w, theta, 0, getVocSize());
							term2+=like*(double)countsHWu.get(h).get(w);
                                                        if (debugNanlog){
                                                            if (Double.isInfinite(term2) || Double.isNaN(term2) )
                                                                    System.out.println("countsHW Error term2:"+term2); break;
                                                        }
                                                        
						}
					}
                        }
                        
                        //semantic part:
                        //term3
                        for (int h : countsHVoOu.keySet()) {
                                HashMap<Integer,HashMap<Integer,Integer>> theta2;
                                theta2= countsHVoO.get(h);
                                term3+=termTotalLogPost(h, theta2,getOsize(),countsHVoOu.get(h));
                                if (debugNanlog){
                                    if (Double.isInfinite(term3) || Double.isNaN(term3) )
                                            System.out.println("countsHVoO Error term3:"+term3); break;
                                }
                        }
                        //term4
                        for (int h : countsHAWau.keySet()) {
                                HashMap<Integer,HashMap<Integer,Integer>> theta2;
                                theta2 = countsHAWa.get(h);
                                term4+=termTotalLogPost(h, theta2,getWasize(),countsHAWau.get(h));
                                if (debugNanlog){
                                    if (Double.isInfinite(term4) || Double.isNaN(term4) )
                                            System.out.println("countsHAWa Error term4:"+term4); break;
                                }
                        }
                        
                        //term5=sum_h[sum_vo[sum_o[(sum_a sum_i log(na + alpha + i)-(sum_i log(sum_a(na+alpha+i)))]]]
                        for (int h : countsHOAu.keySet()) {
                                HashMap<Integer,HashMap<Integer,Integer>> theta2=countsHOA.get(h);
//                                HashMap<Integer,HashMap<Integer,HashMap<Integer,Integer>>> theta3 = countsHVoOA.get(h);
                                term5+=termTotalLogPost(h, theta2, getAsize(),countsHOAu.get(h));
                                if (debugNanlog){
                                    if (Double.isInfinite(term5) || Double.isNaN(term5) )
                                            System.out.println("countsHVoOA Error term5:"+term5); break;
                                }
//                                if (theta3!=null){
//                                    for (int v:theta3.keySet()){
//                                        theta2=theta3.get(v);
//                                        term5+=termTotalLogPost(v, theta2, getAsize(),countsHVoOAu.get(h).get(v));
//                                        if (debugNanlog){
//                                            if (Double.isInfinite(term5) || Double.isNaN(term5) )
//                                                    System.out.println("countsHVoOA Error term5:"+term5); break;
//                                        }
//                                        
//                                    }
//                                }
                        }
                        
                        //term6
                        for (int v : countsVoARpu.keySet()) {
                                HashMap<Integer,HashMap<Integer,Integer>> theta2=countsVoARp.get(v);
                                term6+=termTotalLogPost(v, theta2, getRPsize(),countsVoARpu.get(v));
                                if (debugNanlog){
                                    if (Double.isInfinite(term6) || Double.isNaN(term6) )
                                            System.out.println("countsHVoARp Error term6:"+term6);
                                    break;
                                }
//				HashMap<Integer,HashMap<Integer,HashMap<Integer,Integer>>> theta3 = countsVoARp.get(h);
//				if (theta3!=null)
//					for (int v : theta3.keySet()) {
//                                            theta2=theta3.get(v);
//                                            term6+=termTotalLogPost(v, theta2, getRPsize(),countsHVoARpu.get(h).get(v));
//                                            if (debugNanlog){
//                                                if (Double.isInfinite(term6) || Double.isNaN(term6) )
//                                                        System.out.println("countsHVoARp Error term6:"+term6);
//                                                break;
//                                            }
//                                            
//                                        }
//
			}
                    double penalization=0;
                    if (hilow!=null &&hilow.size()>0)
                             penalization=hilow.get(indgr);
                    double tt=term1+term2+term3+term4+term5+term6 + penalization;    
                    
                    terms+=tt;
                    termsSynt+= term1+term2+ penalization;
                    termsSem+= term3+term4+term5+term6+ penalization;
                    //System.out.println("Sum Terms graph="+tt+"  Term1="+term1+ " term2="+term2 + " term3="+term3 + " term4="+term4 +" term5="+term5 + " term6="+term6  );                    
                }
//                System.out.println("Term1="+term1+ " term2="+term2 + " term3="+term3 + " term4="+term4 +" term5="+term5 + " term6="+term6  );
//                res=term1+term2+term3+term4+term5+term6;
		res[0]=terms;
                res[1]= termsSynt;
                res[2]= termsSem;
                
                return res;
	}

        
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
                                    if (debugOn) System.out.println("debugT41 o="+o+" a="+a+" nau="+nau+" i="+i+" co="+getCount4(countsHOA, h, o, a)+" alpha="+getAlphaA(0)
                                                    +" sumavantlog="+(getAlphaA(0)+getCount4(countsHOA, h, o, a)+i));
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
	public double getLogPost(DetGraph g, List<int[]> rs) {
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
 
                        //total: syntax + semantic
//            			res+=term1+term2+term3+term4+term5+term6;
                        double syntp = term1+term2;
                        double semp  = term3+term4+term5+term6;
            			res+=syntp+semp;
//         if (debugOn) 
//        	 //System.out.println("debugscores h="+h+" "+term1+" "+term2+" "+term3+" "+term4+" "+term5+" "+term6+" synt="+syntp+" sem="+semp+" tot="+res);
//        	 if (term4!=0)
//        		 System.out.println("debugscores t4 num="+t41+" denom="+t42);
		
		return res;
	}

	public static boolean debugOn=false;
	
	public void increaseCounts(DetGraph g,List<int[]> word2rule) {
		updateCounts(true, g,countsHD, countsHW, countsHVoO,countsHOA,countsHAWa,countsVoARp);
	}
	public void decreaseCounts(DetGraph g,List<int[]> word2rule) {
		updateCounts(false, g, countsHD, countsHW, countsHVoO,countsHOA,countsHAWa,countsVoARp);
	}
	public void resetCache() {
		lastgComputed=null;
                lastgComputedSRL=null;
	}
	// **************** Frame computation

        protected int[] getLabs2track() {
		final int[] labs = {
				Dep.getType("MOD"),
				Dep.getType("SUJ"),Dep.getType("OBJ"),Dep.getType("POBJ")};
		return labs;
	}

//	final int[] labs = {
//			Dep.getType("MOD"),
//			Dep.getType("SUJ"),Dep.getType("OBJ"),Dep.getType("POBJ")};
	final int maxndeps = 5;
	private DetGraph lastgComputed = null;
	private int[] lastD;
	private int[][] lastW;
	private int[] ndeps; // = new int[labs.length];
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
        /*******************************************************
         * precomputation for the srl part
         * @return 
         */
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

        /**
         * Checks if the word is a predicate, and loads the voice variable
         * @param g
         * @param i
         * @return 
         */
        private int isPredicateAux(DetGraph g, int i){
            int voice=-1;
//            String lem=getLemmafromForm(g.getMot(i).getForme());
//            boolean isPred=false;
//            boolean mayBeAux=false;
            String posV= g.getMot(i).getPOS();
            //verbal predicates don't include "to be"
            if (SemanticProcesing.isPredicate(g, i)){
                voice = vact;
                //evaluate the voice:
                    if (posV.equals("VBN")){
                        for (int pv=i; pv>0; pv--){
                            if (g.getMot(pv).getPOS().startsWith("VB")){
//                                String lempv=g.getMot(pv).getLemme();
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

        //*************french
            public String getLemmafromFormFr(String f){
                String lem=f;
                boolean found=false;
                String[] beLems={"suis","es","est","sommes","êtes", "sont", "été", "étais", "était", "étions", "étiez", "étaient", "fus", "fut", 
                                 "fûmes", "fûtes", "furent", "serai", "seras", "sera", "serons", "serez", "seront", "serais", "serions", "serait", 
                                 "seraient", "sois", "soit", "soyons", "soyez", "soient", "sois"};
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

        /**
         * Checks if the word is a predicate, and loads the voice variable
         * @param g
         * @param i
         * @return 
         */
        private int isPredicateAuxFr(DetGraph g, int i){
            int voice=-1;
//            String lem=getLemmafromForm(g.getMot(i).getForme());
//            boolean isPred=false;
//            boolean mayBeAux=false;
            String posV= g.getMot(i).getPOS();
            //verbal predicates don't include "to be"
            //if (SemanticProcesing.isPredicate(g, i)){
            if (posV.startsWith("V")){
                voice = vact;
                //evaluate the voice:
                    if (posV.startsWith("V")){
                        for (int pv=i; pv>0; pv--){
                            if (g.getMot(pv).getPOS().startsWith("V")){
//                                String lempv=g.getMot(pv).getLemme();
                                String lempv=getLemmafromFormFr(g.getMot(pv).getForme());
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
                                String l=getLemmafromFormFr(g.getMot(i+1).getForme());
                               //if ( g.getMot(i+1).getLemme().startsWith("by"))
                                if ( l.startsWith("par"))
                                     voice=vpas;
                            }
                        }
                    }//else voice = vact;
            }
            return voice;
        }
        
        
        private DetGraph lastgComputedSRL = null;
        private ArrayList<Integer> predicates;
        private int[] voices;
	private int[][] argsPerPred;
	private int[] ordsPerPred; 
        private int[][] wasPerPred;
        private int[][] rpsPerPred;
        
        
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
                int v=(french)?isPredicateAuxFr(g, i):isPredicateAux(g, i);
                if (v>=0){
                    List<Integer> childre= gsrl.getFils(i);
//                    if (childre.size()<=0)
//                        System.out.println("empty!"+i+" -"+g);
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
    //                    System.out.println("predicate"+i+" args:"+as);
    //                    System.out.println("predicate"+i+" orders:"+ ord);
    //                    System.out.println("predicate"+i+" ws:"+ws);
                        
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



}
