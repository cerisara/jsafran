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
public class RuleCrit3SemV01{
	int nrules=0;
	Rules rules;
	Voc voc;
        //final int numArgs=7; //start by considering A0, A1, A2, A3, A4, AM and N for none
        final int numAM=3;
//        final String[]primRoles={"A0","A1","N"};
//        final String[]secRoles={"A2","A3","A4","A5","AM","N"};
//        HashMap<String, Integer> rolesDict= new HashMap<String, Integer>();
//        HashMap<Integer,String> invrolesDict= new HashMap<Integer,String>();
        private String[]rolarr={"A0","A1","A2","A3","A4","A5","AM"};
        
        public ArrayList<String>roles=new ArrayList<String>();
        public HashMap<String,Integer> dictDepRelPos = new HashMap<String,Integer>();
        public HashMap<Integer,String> invdictDepRelPos= new HashMap<Integer,String>();

        int vact=0; int vpas=1; int vnone=2;
//        int rpleft=0;2
//        int rpright=1;

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
        // H x Vo x O x noccs --order for primary roles
        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoO = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
        // H x A x Wa x noccs --words for primary roles
        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWa = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
        // Vo x A x DRp x noccs  --depedency label and relative position for primary roles
        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoADRp = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>();
        // H x A2 x noccs  --secondary roles
        HashMap<Integer,HashMap<Integer, Integer>> countsHA2 = new HashMap<Integer,HashMap<Integer, Integer>>();
        // H x A2 x Wa2 x noccs --words for secondary roles
        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHA2Wa2 = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
        // Vo x A2 x DRp2 x noccs  --depedency label and relative position for secondary roles
        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoA2DRp2 = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>();
        
        int[]voiceperH;
        
        
        // WARNING: we must keep and update in increaseCounts()/decreaseCounts() in countsHD.get(h).get(-1) the TOTAL over all d
        
        private boolean isPrimaryRole(String role){
            if (role.equals("A0")||role.equals("A1")) return true;
            else return false;
        }

        private int getIndexDictDepRelPos(DetGraph g, int child, int pred, String relPos){
            //find the label of the dependency relation
            String depLab;
            int[] ds=g.getDeps(child);
            depLab=g.getDep(child)>0?g.getDepLabel(g.getDep(child)):"ROOT";
            for (int d:ds){
                if (g.getHead(d)==pred) depLab=g.getDepLabel(d);
            }
            
            int ind;
            String jointStr=depLab+"-"+relPos;
            ind= (dictDepRelPos.containsKey(jointStr))?dictDepRelPos.get(jointStr):-1;
            if (ind<0){
                //add the value in the dictionary and inverse dictionary and return new value
                int pos=dictDepRelPos.size();
                dictDepRelPos.put(jointStr, pos);
                invdictDepRelPos.put(pos,jointStr);
                ind=pos;
            }
            return ind;
        }
        
	// for debugging !
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
		//System.out.println("TOT COUNTS HD "+n+" "+nn+" HW "+m);
                //semantic part:countsHVoO, countsHAWa, countsVoADRp, countsHA2, countsHA2Wa2,countsVoA2DRp2
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
                for (int v:countsVoADRp.keySet()){
                    for (int o:countsVoADRp.get(v).keySet()){
                        int nnn=0;
                        n+=countsVoADRp.get(v).get(o).get(-1);
                        for (int a:countsVoADRp.get(v).get(o).keySet()){
                             if (a>=0)nnn+=countsVoADRp.get(v).get(o).get(a);
                        }
                        nn+=nnn;
                        if (nnn!=countsVoADRp.get(v).get(o).get(-1))
                            System.out.println("ERROR TOT!!!!!!! countsVoADRp  nnn="+nnn+" counts in -1="+countsVoADRp.get(v).get(o).get(-1));
                    }
                }
                n=0; nn=0;
                for(int h:countsHA2.keySet()){
                    int nnn=0;
                    n+=countsHA2.get(h).get(-1);
                    for(int a:countsHA2.get(h).keySet()){
                        if(a>=0) nnn+=countsHA2.get(h).get(a);
                    }
                    nn+=nnn;
                    if (nnn!=countsHA2.get(h).get(-1))
                        System.out.println("ERROR TOT!!!!!!! countsHA2  nnn="+nnn+" counts in -1="+countsHA2.get(h).get(-1));
                }

                n=0; nn=0;
                for ( int h:countsHA2Wa2.keySet()){
                    for (int a:countsHA2Wa2.get(h).keySet()){
                            int nnn=0;
                            n+=countsHA2Wa2.get(h).get(a).get(-1);
                            for (int wa:countsHA2Wa2.get(h).get(a).keySet()){
                                if (wa>=0) nnn+=countsHA2Wa2.get(h).get(a).get(wa);
                            }
                            nn+=nnn;
                            if (nnn!=countsHA2Wa2.get(h).get(a).get(-1))
                                System.out.println("ERROR TOT!!!!!!! countsHA2Wa2  nnn="+nnn+" counts in -1="+countsHA2Wa2.get(h).get(a).get(-1));
                     }
                 }
                
                n=0; nn=0;
                for (int v:countsVoA2DRp2.keySet()){
                    for (int o:countsVoA2DRp2.get(v).keySet()){
                        int nnn=0;
                        n+=countsVoA2DRp2.get(v).get(o).get(-1);
                        for (int a:countsVoA2DRp2.get(v).get(o).keySet()){
                             if (a>=0)nnn+=countsVoA2DRp2.get(v).get(o).get(a);
                        }
                        nn+=nnn;
                        if (nnn!=countsVoA2DRp2.get(v).get(o).get(-1))
                            System.out.println("ERROR TOT!!!!!!! countsVoA2DRp2  nnn="+nnn+" counts in -1="+countsVoA2DRp2.get(v).get(o).get(-1));
                    }
                }
                
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

	public RuleCrit3SemV01(Rules r, Voc v) {
		/*
		loadList("res/aobj-verbs.txt",aobjverbs);
		loadList("res/deobj-verbs.txt",deobjverbs);
		loadList("res/obj-verbs.txt",objverbs);
		 */
		voc=v;
		rules=r;
                ndeps = new int[getLabs2track().length];
                dictDepRelPos.put("N", -1);
                invdictDepRelPos.put(-1,"N");
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
		return 0.0001;
	}
	protected double getAlphaW(int w) {
		return 0.00001;
	}
        //alphas for the semantic part
	private double getAlphaO(int d) {
		return 0.01;
	}
	private double getAlphaA2(int d) {
		return 0.00000001;
	}
	private double getAlphaWa(int d) {
		return 0.00000001;
	}
	private double getAlphaDRp(int d) {
		return 0.000001;
	}
	private double getAlphaWa2(int d) {
		return 0.001;
	}
	private double getAlphaDRp2(int d) {
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
        //TODO: CHANGE ADD IN SYNTAX THE DEPENDENCY SEE IN SEMANTIC 
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
//            int pos=predicates.indexOf(i);
            int pos=i;
            if (gsr==lastgComputedSRL) return voices[pos];
            precomputeStructuresSRL(g);
//            pos=predicates.indexOf(i);
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
//            int pos=predicates.indexOf(i);
            int pos=i;

            if (gsrl==lastgComputed) return ordsPerPred[pos];
		precomputeStructuresSRL(g);
//                pos=predicates.indexOf(i);
		return ordsPerPred[pos];
        }

        /**
         * Get the arguments corresponding to the primary roles: A0, A1 (initially)
         * @param gsrl: the graph for the semantic role labelling part
         * @param i; the position of the predicate, we asume it's a predicate (todo see if it's ok)
         * @return an array with the arguments of the predicate (the ids of the roles)
         */
        int[] getPrimArgs(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
//            int pos=predicates.indexOf(i);
                        int pos=i;

		if (gsrl==lastgComputed) return primArgsPerPred[pos];
		precomputeStructuresSRL(g);
//		pos=predicates.indexOf(i);
                return primArgsPerPred[pos];
        }

        /**
         *Get the arguments corresponding to the secondary roles: A2+ and AM
         * @param gsrl: the graph for the semantic role labelling part
         * @param i; the position of the predicate, we asume it's a predicate (todo see if it's ok)
         * @return an array with the arguments of the predicate (the ids of the roles)
         */
        int[] getSecArgs(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
//            int pos=predicates.indexOf(i);
                        int pos=i;

		if (gsrl==lastgComputed) return secArgsPerPred[pos];
		precomputeStructuresSRL(g);
//		pos=predicates.indexOf(i);
                return secArgsPerPred[pos];
        }

        
        int[] getPrimWordsSRL(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
//            int pos=predicates.indexOf(i);
                        int pos=i;

		if (gsrl==lastgComputed) return primWasPerPred[pos];
		precomputeStructuresSRL(g);
//                pos=predicates.indexOf(i);
		return primWasPerPred[pos];
        }

        int[] getSecWordsSRL(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
//            int pos=predicates.indexOf(i);
                        int pos=i;
		if (gsrl==lastgComputed) return secWasPerPred[pos];
		precomputeStructuresSRL(g);
//                pos=predicates.indexOf(i);
		return secWasPerPred[pos];
        }

        int[] getPrimDepRelposSRL(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
//            int pos=predicates.indexOf(i);
                        int pos=i;
		if (gsrl==lastgComputed) return primDrpsPerPred[pos];
		precomputeStructuresSRL(g);
//                pos=predicates.indexOf(i);
		return primDrpsPerPred[pos];
        }

        int[] getSecDepRelposSRL(DetGraph g, int i) {
            DetGraph gsrl= g.relatedGraphs.get(0);
//            int pos=predicates.indexOf(i);
                        int pos=i;
            if (gsrl==lastgComputed) return secDrpsPerPred[pos];
            precomputeStructuresSRL(g);
//            pos=predicates.indexOf(i);
            return secDrpsPerPred[pos];
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
//            System.out.println("inc:"+inc+" h:"+h+" vo:"+vo+" o:"+o);
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

        private void updateCountsHAWa(boolean inc, int h, int[] as, int[] ws,
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
        
        
        
        //updateCountsVoADRp
        private void updateCountsVoADRp(boolean inc, int vo, int[] pas, int[] pdrps,
                                     HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> countsvadr){
            HashMap<Integer, HashMap<Integer,Integer>> adr2co= countsvadr.get(vo);
            HashMap<Integer, Integer> dr2co;
            if (adr2co==null){
                adr2co=new HashMap<Integer, HashMap<Integer,Integer>>();
                for (int a:pas){
                     dr2co=new HashMap<Integer, Integer>();
                     dr2co.put (-1,0);
                     adr2co.put(a,dr2co);
                }
                countsvadr.put(vo, adr2co);
            }else{
                for (int a:pas){
                    dr2co=adr2co.get(a);
                    if (dr2co==null){
                        dr2co=new HashMap<Integer, Integer>();
                        dr2co.put(-1, 0);
                        adr2co.put(a,dr2co);
                    }
                }
            }
            for (int ind=0;ind<pas.length;ind++){
                int a=pas[ind];
                int r=pdrps[ind];
                dr2co=adr2co.get(a);
                if (dr2co==null){
                    dr2co=new HashMap<Integer, Integer>();
                     dr2co.put (-1,0);
                     adr2co.put(a,dr2co);
                }
                Integer c2=dr2co.get(r);
                if (inc) {
                        if (c2==null) c2=1; else c2++;
                        dr2co.put(r, c2);
                        int tot=dr2co.get(-1);
                        dr2co.put(-1, ++tot);
                } else {
                        c2--;
                        if (c2==0) dr2co.remove(r);
                        else if (c2<0) System.out.println("ERROR negative counts updateCountsHVoARp!");
                        dr2co.put(r, c2);
                        int tot=dr2co.get(-1);
                        dr2co.put(-1, --tot);
                        if (tot<0) System.out.println("ERROR negative counts tot updateCountsHVoARp!");
                        assert tot>=0;
                }
            }
            
        }

        private void updateCountsHA2(boolean inc, int h, int[]sas,
                                      HashMap<Integer,HashMap<Integer, Integer>> countsha2){
            HashMap<Integer, Integer> a2co=countsha2.get(h);
            if (a2co==null){
                a2co=new HashMap<Integer, Integer>();
                a2co.put(-1, 0);
                countsha2.put(h, a2co);
            }
            for (int a:sas){
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

        
        //updateCounts(inc, g, countsHD, countsHW, countsHVoO,countsHVoOA,countsHAWa,countsHVoARp);
        private void updateCounts(boolean inc, DetGraph g, 
                                //syntactic part
                                HashMap<Integer, HashMap<Integer, Integer>> countshd,
                                HashMap<Integer, HashMap<Integer, Integer>> countshw,
                                //semantic part
                                // H x Vo x O x noccs --order for primary roles
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshvo,
                                // H x A x Wa x noccs --words for primary roles
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countshawa,
                                // Vo x A x DRp x noccs  --depedency label and relative position for primary roles
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsvoadr,
                                // H x A2 x noccs  --secondary roles
                                HashMap<Integer,HashMap<Integer, Integer>> countsha2,
                                // H x A2 x Wa2 x noccs --words for secondary roles
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsha2wa2,
                                // Vo x A2 x DRp2 x noccs  --depedency label and relative position for secondary roles
                                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsvoa2dr2) {
            DetGraph gsr= g.relatedGraphs.get(0);
            voiceperH=new int[g.getNbMots()];
            Arrays.fill(voiceperH, -1); //set all to null and fill only those belonging to a predicate (done in updateCounts)
//            System.out.println("antes");
            debugCounts();
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
                            voiceperH[i]=vo;
                            int o=getOrder(g,i);
                            int[] primas=getPrimArgs(g,i);
                            int[] secas=getSecArgs(g,i);
                            int[] primwas=getPrimWordsSRL(g,i);
                            int[] secwas=getSecWordsSRL(g,i);
                            int[] primdras=getPrimDepRelposSRL(g,i);
                            int[] secdras=getSecDepRelposSRL(g,i);
                            updateCountsHVoO(inc, h, vo, o, countshvo);
                            updateCountsHAWa(inc, h, primas, primwas, countshawa);
                            updateCountsVoADRp(inc, vo, primas, primdras, countsvoadr);
                            updateCountsHA2(inc, h, secas, countsha2);
                            //updateCountsHA2Wa2(inc, h, secas, secwas, countsha2wa2);
                            updateCountsHAWa(inc, h, secas, secwas, countsha2wa2);
                            //updateCountsVoA2DRp2(inc, vo, secas, secdras, countsvoa2dr2);
                            updateCountsVoADRp(inc, vo, secas, secdras, countsvoa2dr2);
                            //countshaw
                            
                            //countshvar
//                            if (countshvar==countsHVoARp)
//System.out.println("debug updateco "+inc+" "+h+" "+vo+" "+Arrays.toString(as)+" "+Arrays.toString(ras)+" "+g);
//                            System.out.println("debug cotables "+countshvar);
                            
                        }
		}
//                            System.out.println("despues");
//            debugCounts();


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
                double waentropy=0;
                for (int h: countsHAWa.keySet()){
                    //TODO
                }
                double rpentropy=0;
                for (int h: countsVoADRp.keySet()){
                    //TODO
                }
                double a2entropy=0;
                for (int h: countsHA2.keySet()){
                    //TODO
                }
                double wa2entropy=0;
                for (int h: countsHA2Wa2.keySet()){
                    //TODO
                }
                double rp2entropy=0;
                for (int h: countsVoA2DRp2.keySet()){
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
                res=boundprob(Math.log(th.get(x-lo))-Math.log(s));
            }
            return res;
        }
        
        private double ldf_Mult_smooth(boolean normalize, double eta,int x, HashMap<Integer,Integer> th,int lo, int hi){
        //private double ldf_Mult_smooth(boolean normalize, double eta,int x, int[]th,int lo, int hi){
            double res=0;
            if ((x<lo)||(x>hi)) { res= -0/1; }
            else{
                double s=th.get(-1)+eta*(hi-lo);
                //for (int i=0; i<hi-lo; i++) { s+=th[i]; }
                //the sum should be in the th.get(-1)??? 
                res=boundprob(Math.log(th.get(x-lo)+eta)-Math.log(s));
            }
            return res;
        }
        /*****************************************************************/

        
        public double getTotalLogPost(List<DetGraph> gs) {
            //debugCounts();
                boolean debugNanlog=true;
                //List<DetGraph> gs= gss[0];
                //List<DetGraph> gssrl= gss[1];
                int graphindex=0;
                double terms=0;
		for (DetGraph g: gs) {
                    double term1=0;
                    double term2=0;
                    double term3=0;
                    double term4=0;
                    double term5=0;
                    double term6=0;
                    double term7=0;
                    double term8=0;
//                        DetGraph gsrl=gssrl.get(graphindex);
//                    DetGraph gsrl=gs.relatedGraphs.get(0);
                        graphindex++;
                        //syntactic part
			HashMap<Integer, HashMap<Integer, Integer>> countsHDu = new HashMap<Integer, HashMap<Integer,Integer>>();
			HashMap<Integer, HashMap<Integer, Integer>> countsHWu = new HashMap<Integer, HashMap<Integer,Integer>>();
                        //semantic part
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoOu = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWau = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoADRpu = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
                        HashMap<Integer,HashMap<Integer,  Integer>> countsHA2u = new HashMap<Integer, HashMap<Integer,Integer>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHA2Wa2u = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                        HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoA2DRp2u = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();

			updateCounts(true, g, countsHDu, countsHWu, countsHVoOu,countsHAWau,countsVoADRpu,countsHA2u,countsHA2Wa2u,countsVoA2DRp2u);
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
                        //term3=sum_h[sum_vo[(sum_o sum_i log(no + alpha + i)-(sum_i log(sum_o (no+alpha+i)))]] 
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
                        //term4=sum_h[sum_a[(sum_w sum_i log(nwa + alpha + i)-(sum_i log(sum_w (nwa+alpha+i)))]]
                        for (int h : countsHAWau.keySet()) {
                                HashMap<Integer,HashMap<Integer,Integer>> theta2;
                                theta2 = countsHAWa.get(h);
                                term4+=termTotalLogPost(h, theta2,getWasize(),countsHAWau.get(h));
                                if (debugNanlog){
                                    if (Double.isInfinite(term4) || Double.isNaN(term4) )
                                            System.out.println("countsHAWa Error term4:"+term4); break;
                                }
                        }
                        
                        //term5
                        //term5=sum_v[sum_a[(sum_dr sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr (nwdr+alpha+i)))]]
                        for (int v:countsVoADRpu.keySet()){
                            HashMap<Integer,HashMap<Integer,Integer>>theta2;
                            theta2=countsVoADRp.get(v);
                            term5+=termTotalLogPost(v, theta2, getDRPsize(), countsVoADRpu.get(v));
                                if (debugNanlog){
                                    if (Double.isInfinite(term5) || Double.isNaN(term5) )
                                            System.out.println("countsVoADRp Error term5:"+term5); break;
                                }
                        }
                        
                        //term6 countsHA2
                        for (int h:countsHA2u.keySet()){
                            HashMap<Integer,Integer>theta =countsHA2.get(h);
                            if (theta!=null&&theta.get(-1)>0)
                                    for (int a2 : countsHA2u.get(h).keySet()) {
                                            if (a2>=0) {
                                                    double like=ldf_Mult(true, a2, theta, 0, getA2size());
                                                    term6+=like*(double)countsHA2u.get(h).get(a2);
                                                    if (debugNanlog){
                                                        if (Double.isInfinite(term6) || Double.isNaN(term6) )
                                                                System.out.println("countsHA2 Error term6:"+term6); break;
                                                    }

                                            }
                                    }
                        
                        }
                        //term7=sum_h[sum_a2[(sum_w sum_i log(nwa2 + alpha + i)-(sum_i log(sum_w (nwa2+alpha+i)))]]
                        for (int h : countsHA2Wa2u.keySet()) {
                                HashMap<Integer,HashMap<Integer,Integer>> theta2;
                                theta2 = countsHA2Wa2.get(h);
                                term7+=termTotalLogPost(h, theta2,getWasize(),countsHA2Wa2u.get(h));
                                if (debugNanlog){
                                    if (Double.isInfinite(term7) || Double.isNaN(term7) )
                                            System.out.println("countsHA2Wa2 Error term7:"+term7); break;
                                }
                        }
                        
                        //term8=sum_v[sum_a2[(sum_dr2 sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr2 (nwdr+alpha+i)))]]
                        for (int v:countsVoA2DRp2u.keySet()){
                            HashMap<Integer,HashMap<Integer,Integer>>theta2;
                            theta2=countsVoA2DRp2.get(v);
                            term8+=termTotalLogPost(v, theta2, getDRPsize(), countsVoA2DRp2u.get(v));
                                if (debugNanlog){
                                    if (Double.isInfinite(term8) || Double.isNaN(term8) )
                                            System.out.println("countsVoA2DRp2 Error term8:"+term8); break;
                                }
                        }
                        double tt=term1+term2+term3+term4+term5+term6;    
                        terms+=tt;
                        
//			for (int h : countsHDu.keySet()) {
//                                //semantic part:
//                                HashMap<Integer,HashMap<Integer,Integer>> theta2;
//                                //term3=sum_h[sum_vo[(sum_o sum_i log(no + alpha + i)-(sum_i log(sum_o (no+alpha+i)))]] 
//                                theta2= countsHVoO.get(h);
//                                res+=termTotalLogPost(h, theta2,getOsize());
//                                //term4=sum_h[sum_a[(sum_w sum_i log(nwa + alpha + i)-(sum_i log(sum_w (nwa+alpha+i)))]]
//                                theta2 = countsHAWa.get(h);
//                                res+=termTotalLogPost(h, theta2,getWasize());
//                                //term5=sum_v[sum_a[(sum_dr sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr (nwdr+alpha+i)))]]
//                                theta2 = countsVoADRp.get(h);
//                                res+=termTotalLogPost(h, theta2,getDRPsize());                                //term6
//                                //term 6
//				theta = countsHA2.get(h);
//				if (theta!=null&&theta.get(-1)>0)
//					for (int w : theta.keySet()) {
//						if (w>=0) {
////							double like = Math.log((double)theta.get(w)/(double)theta.get(-1));
////							like *= (double)theta.get(w);
//                                                        double like=ldf_Mult(true, w, theta, 0, getA2size());
//							res+=like;
//						}
//					}
//                                
//                                //term7=sum_h[sum_a2[(sum_w sum_i log(nwa2 + alpha + i)-(sum_i log(sum_w (nwa2+alpha+i)))]]
//                                theta2 = countsHA2Wa2.get(h);
//                                res+=termTotalLogPost(h, theta2,getWasize());
//                                //term8=sum_v[sum_a2[(sum_dr2 sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr2 (nwdr+alpha+i)))]]
//                                theta2 = countsVoA2DRp2.get(h);
//                                res+=termTotalLogPost(h, theta2,getDRPsize());
//                                indexh++;
//
//			}
		}
		return terms;
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
        
        /**
         * //term4=sum_h[sum_a[(sum_w sum_i log(nwa + alpha + i)-(sum_i log(sum_w (nwa+alpha+i)))]]
         * @param h
         * @param countsHAWau
         * @return 
         */
        private double calculTerm4and7(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWau,boolean primary){
            double t41=0;
            double t42=0;
            //aux structure to re-use the procedure
            HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWaAux=primary?countsHAWa:countsHA2Wa2;
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
                                double alpha= primary?getAlphaWa(0):getAlphaWa2(0);//aux structure to re-use the procedure
                                t41+=Math.log(alpha+getCount4(countsHAWaAux, h, a, w)+i);
                            }
                        }
                        //denominator
                        double alpha= primary?getAlphaWa(0):getAlphaWa2(0);//aux structure to re-use the procedure
                        double AWA=alpha*getWasize();
                        int nsum=getTotal4(countsHAWaAux, h, a);
                        for (int i=0; i<totnwu;i++){
                            t42 -= Math.log(AWA+nsum+i);
                        }
                    }
                }
            }
            double term4=t41+t42;
            return term4;
        }

        /**
         * //term5=sum_v[sum_a[(sum_dr sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr (nwdr+alpha+i)))]]
         * @return 
         */
        private double calculTerm5and8(HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoADRpu, boolean primary){
            double t51=0;
            double t52=0;
            //aux structure to re-use the procedure
            HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoADRpAux= (primary)? countsVoADRp: countsVoA2DRp2;
            for (int vo:countsVoADRpu.keySet()){
                //int vo=voiceperH[indexh]; //TODO see if ok????
                HashMap<Integer,HashMap<Integer, Integer>>adr2co= countsVoADRpu.get(vo);
                if (adr2co!=null){
                    for (int a:adr2co.keySet()){
                        HashMap<Integer, Integer>dr2co=adr2co.get(a);
                        int totndru=0;
                        //numerator
                        for(int dr:dr2co.keySet()){
                            int ndru=dr2co.get(dr);
                            totndru+=ndru;
                            for (int i=0;i<ndru;i++){
                                //t51+=Math.log(getAlphaDRp(0)+getCount4(countsVoADRp,vo, a, dr)+i);
                                double alpha=primary?getAlphaDRp(0):getAlphaDRp2(0);//aux structure to re-use the procedure
                                t51+=Math.log(alpha+getCount4(countsVoADRpAux,vo, a, dr)+i);
                            }
                        }
                        //denominator
                        double alpha=primary?getAlphaDRp(0):getAlphaDRp2(0);//aux structure to re use the procedure
                        double ADRA=alpha*getDRPsize();
                        //int nsum=getTotal4(countsVoADRp, vo, a);
                        int nsum=getTotal4(countsVoADRpAux, vo, a);
                        for (int i=0; i<totndru;i++){
                            t52 -= Math.log(ADRA+nsum+i);
                        }
                    }
                }
            }
            double term5=t51+t52;
            return term5;
        }

        private double calculTerm6(HashMap<Integer,HashMap<Integer,  Integer>> countsHA2u ){
            double t61=0;
            double t62=0;
            for (int h : countsHA2u.keySet()) {            
                HashMap<Integer, Integer> a22co = countsHA2u.get(h);
                int nha2utot=0;
                if (a22co!=null){
                    for (int a:a22co.keySet()){
                        //numerator
                        if (a<0) continue;
                        int nha2u = a22co.get(a);
                        nha2utot+=nha2u;
                        for (int i=0;i<nha2u;i++) {
                                t61 += Math.log(getAlphaA2(a)+getCount(countsHA2,h,a)+i);
                        }
                    }
                    // we assume symetric alpha !
                    double AA2 = getAlphaA2(0)*getA2size();
                    int nsuma2 = getTotal(countsHA2,h);
                    for (int i=0;i<nha2utot;i++) {
                            t62 -= Math.log(AA2+nsuma2+i);
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
            DetGraph gsrl=g.relatedGraphs.get(0);
		double res=0;

		// ------------------------------------------------
		// counts n^u_{h,d} and n^u_{h,w} for the current sentence
		// (the same counts n^{-u}_{h,d} for the rest of the corpus are in countsHD and countsHW)
		HashMap<Integer, HashMap<Integer, Integer>> countsHDu = new HashMap<Integer, HashMap<Integer,Integer>>();
		HashMap<Integer, HashMap<Integer, Integer>> countsHWu = new HashMap<Integer, HashMap<Integer,Integer>>();
                //semantic part
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHVoOu = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHAWau = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoADRpu = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
                HashMap<Integer,HashMap<Integer,  Integer>> countsHA2u = new HashMap<Integer, HashMap<Integer,Integer>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsHA2Wa2u = new HashMap<Integer, HashMap<Integer,HashMap<Integer, Integer>>>();
                HashMap<Integer,HashMap<Integer, HashMap<Integer, Integer>>> countsVoA2DRp2u = new HashMap<Integer, HashMap<Integer,HashMap<Integer,Integer>>>();
                

		updateCounts(true, g, countsHD, countsHW, countsHVoO,countsHAWa,countsVoADRp,countsHA2,countsHA2Wa2,countsVoA2DRp2);

		// on doit avoir exactement les memes h dans les 2 hashmaps
		assert countsHDu.size()==countsHWu.size();
                assert countsHVoOu.size()==countsHDu.size();
                assert countsHDu.size()==countsHAWau.size();
                assert countsHAWau.size()==countsHA2Wa2u.size();
                assert countsVoADRpu.size()==countsVoA2DRp2u.size(); //TODO SEE ASSERTS!!!!!!!!
                double term1=0,term2=0,term3=0,term4=0,term5=0,term6=0,term7=0,term8=0;
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

//			System.out.println("logpostdebug h="+h+" "+t1a+" "+t1b+" "+t2a+" "+t2b+" "+res);

			//			System.out.println("logpostdebug h="+h+" "+t1a+" "+t1b+" "+t2a+" "+t2b+" "+res);

                        //semantic part:
                        //term3=sum_h[sum_vo[(sum_o sum_i log(no + alpha + i)-(sum_i log(sum_o (no+alpha+i)))]]
                        term3+= calculTerm3(countsHVoOu);

                        //term4=sum_h[sum_a[(sum_w sum_i log(nwa + alpha + i)-(sum_i log(sum_w (nwa+alpha+i)))]]
                        term4+=calculTerm4and7(countsHAWau,true);

                        //term5=sum_v[sum_a[(sum_dr sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr (nwdr+alpha+i)))]]
                        term5+=calculTerm5and8(countsVoADRpu, true);
                                      
                        term6+=calculTerm6(countsHA2u);
                        
                        //term7=sum_h[sum_a2[(sum_w sum_i log(nwa2 + alpha + i)-(sum_i log(sum_w (nwa2+alpha+i)))]]
                        term7+=calculTerm4and7(countsHA2Wa2u, false);
                        
                        //term8=sum_v[sum_a2[(sum_dr2 sum_i log(nwdr + alpha + i)-(sum_i log(sum_dr2 (nwdr+alpha+i)))]]
                        term8+=calculTerm5and8(countsVoA2DRp2u, false);
                        //total: syntax + semantic
//            			res+=term1+term2+term3+term4+term5+term6;
                        double syntp = term1+term2;
                        double semp  = term3+term4+term5+term6+term7+term8;
            			res+=syntp+semp;
             //if (debugOn) 
        	 //System.out.println("debugscores h="+h+" "+term1+" "+term2+" "+term3+" "+term4+" "+term5+" "+term6+" synt="+syntp+" sem="+semp+" tot="+res);
   //     	 if (term4!=0)
//        		 System.out.println("debugscores t4 num="+t41+" denom="+t42);
		return res;
	}

	public static boolean debugOn=false;
	
	public void increaseCounts(DetGraph g,List<int[]> word2rule) {
		updateCounts(true, g,countsHD, countsHW, countsHVoO,countsHAWa,countsVoADRp,countsHA2,countsHA2Wa2,countsVoA2DRp2);
	}
	public void decreaseCounts(DetGraph g,List<int[]> word2rule) {
		updateCounts(false, g, countsHD, countsHW, countsHVoO,countsHAWa,countsVoADRp,countsHA2,countsHA2Wa2,countsVoA2DRp2);
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
         * @return 0=
         */
        private int isPredicateAux(DetGraph g, int i){
            int voice=vnone;
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

        
        private DetGraph lastgComputedSRL = null;
        private ArrayList<Integer> predicates;
        private int[] voices;
	private int[] ordsPerPred; 
        private int[][] primArgsPerPred;
	private int[][] secArgsPerPred;
        private int[][] primWasPerPred;
        private int[][] secWasPerPred;
        private int[][] primDrpsPerPred;
        private int[][] secDrpsPerPred;
        //dependents: "NMOD,PMOD,SBJ,OBJ,DEP,ADV,SUFFIX,COORD,CONJ,VC,IM,NAME,PRD,TMP,OPRD,AMOD,POSTHON,HMOD,HYPH,DIR,LGS
        final int maxNumAllDep=21;
	private void precomputeStructuresSRL(DetGraph g){
            boolean debug=false;
            DetGraph gsrl= g.relatedGraphs.get(0);
            lastgComputedSRL=gsrl;
            int numberSecRoles=getA2size();
            int numberPrimRoles=getAsize();
            voices= new int[g.getNbMots()];
            ordsPerPred= new int[g.getNbMots()];
            primArgsPerPred = new int[g.getNbMots()][numberPrimRoles];
            secArgsPerPred = new int[g.getNbMots()][numberSecRoles];
            primWasPerPred= new  int[g.getNbMots()][numberPrimRoles];
            secWasPerPred= new  int[g.getNbMots()][numberSecRoles];
            primDrpsPerPred= new  int[g.getNbMots()][numberPrimRoles];
            secDrpsPerPred= new  int[g.getNbMots()][numberSecRoles];
            predicates= new ArrayList<Integer>();
            for (int i=0;i<g.getNbMots();i++){
                int v=isPredicateAux(g, i);
                voices[i]=v;
                if (v<1){//none is not a predicate put none to all the elements...
                    //order=none
                    OrderRoles o=new OrderRoles();
                    ordsPerPred[i]=o.getOrderId("N"); //o.getOrderId("N");
                    //arguments primary roles
                    Arrays.fill(primArgsPerPred[i], getAsize());
                    Arrays.fill(primWasPerPred[i], getWasize());
                    Arrays.fill(primDrpsPerPred[i], getDRPsize());
                    //arguments secondary roles
                    Arrays.fill(secArgsPerPred[i], getA2size());
                    Arrays.fill(secWasPerPred[i], getWasize());
                    Arrays.fill(secDrpsPerPred[i], getDRPsize());
                }
                else{ //active or pasive, it means it's a verb
                    List<Integer> childre= gsrl.getFils(i);
                    //arguments primary roles
                    Arrays.fill(primArgsPerPred[i], getAsize());
                    Arrays.fill(primWasPerPred[i], getWasize());
                    Arrays.fill(primDrpsPerPred[i], getDRPsize());
                    //arguments secondary roles
                    Arrays.fill(secArgsPerPred[i], getA2size());
                    Arrays.fill(secWasPerPred[i], getWasize());
                    Arrays.fill(secDrpsPerPred[i], getDRPsize());
                    //replace the none in the case of the arguments...
                    if (childre.size()>0){
                        //add to predicates 
                        predicates.add(i);
                        String dstr;
                        String ord="";
                        boolean writePred=false;
                        if (debug && childre.size()>0)System.out.println(g+"\npredicate:"+i);
                        String aux="";
                        for (int c:childre){
                            int[] ds=gsrl.getDeps(c);
                            dstr=gsrl.getDepLabel(gsrl.getDep(c));
                            //load arguments
                            for (int d:ds){
                                if (gsrl.getHead(d)==i) dstr=gsrl.getDepLabel(d);
                            }
                            assert(roles.contains(dstr));
                            if (debug)aux+=dstr;
                            
                            if (debug && childre.size()>0)System.out.println("childre:"+c+" - "+dstr+" hasta ahora:"+ord);
                            //int indrole= roles.indexOf(dstr);
                            int ind=voc.getWordIndex(gsrl.getMot(c));
                            String relPos=(c<i)?"L":"R";
                            int inddrp=getIndexDictDepRelPos(g,c,i, relPos);
                            //add primary roles
                            if (isPrimaryRole(dstr)){
                                int indrole=SemanticProcesing.getprimRoleInd(dstr);
                                primArgsPerPred[i][indrole]=indrole;
                                primWasPerPred[i][indrole]=ind;
                                primDrpsPerPred[i][indrole]=inddrp;
                            }
                            //secondary roles
                            else{
                                int indrole=SemanticProcesing.getSecRoleInd(dstr);
                                if (indrole==-1)
                                //int ir=indrole-2; //TODO: see if ok
                                secArgsPerPred[i][indrole]=indrole;
                                secWasPerPred[i][indrole]=ind;
                                secDrpsPerPred[i][indrole]=inddrp;
                            }
//                            if (indrole==roles.size()-1) { //it's an AM
//                            }
                            //load orders
                            if (isPrimaryRole(dstr) && ord.length()<=7){
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
                        if (debug) System.out.println("Orden: "+ord+ "-"+aux);
                        //remove the last "-";
                        if (ord.endsWith("-")){
                            ord= ord.substring(0, ord.length()-1);
                        }
                        OrderRoles o=new OrderRoles();
                        if (debug)System.out.println("g:"+g+" verb:"+i);
                        if (debug)System.out.println("order:"+ord);
                        ordsPerPred[i]=(o.getOrderId(ord));
                        
                    }
                }
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
        //return roles.size()+1;
        return SemanticProcesing.getprimRoleSize();
    }

    private int getWasize() {
	return getVocSize()+1; //for the "null" word
    }

    private int getDRPsize() {
        return maxNumAllDep*2 +1; //*2 because of"left"/"right", +1 because of the "null"
    }

    private int getA2size() {
        //return secRoles.length+1;
        return SemanticProcesing.getsecRoleSize();
    }



}
