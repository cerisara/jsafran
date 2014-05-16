/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jsafran.parsing.unsup;

import com.sun.org.apache.bcel.internal.generic.SWITCH;
import java.util.ArrayList;
import java.util.List;
import jsafran.DetGraph;

/**
 *
 * @author ale
 */
public class SemanticProcesing {
        private static int vact=0;
        private static int vpas=1;
        public static boolean isFrench=true;
        

	private static String[]coreRarr={"A0","A1","A2","A3","A4"};
        private static String[]coarseRarr={"A0","A1","A2","A3","A4","AM"};
        private static String[]secRarr={"A2","A3","A4","AM","N"};
        private static String[]primRarr={"A0","A1","N"};
        public static ArrayList<String>coreRoles;
        public static ArrayList<String>coarseRoles;
        public static ArrayList<String>primRoles;
        public static ArrayList<String>secRoles;
        
        public static void loadRoles(){
            coreRoles=new ArrayList<String>();
            coarseRoles=new ArrayList<String>();
            primRoles=new ArrayList<String>();
            secRoles=new ArrayList<String>();
            
            //load the roles into the arraylist, easiest to look afterwards
            for (int i=0;i<coreRarr.length;i++){
                coreRoles.add(coreRarr[i]);
            }
            for (int i=0;i<coarseRarr.length;i++){
                coarseRoles.add(coarseRarr[i]);
            }
            for (int i=0;i<primRarr.length;i++){
                primRoles.add(primRarr[i]);
            }
            for (int i=0;i<secRarr.length;i++){
                secRoles.add(secRarr[i]);
            }
        }
        
        public static String getRoleName(int ind){
            String nam;
            loadRoles();
            nam=coarseRoles.get(ind);
            return nam;
        }
        public static int getRoleInd(String nam){
            loadRoles();
            int ind=coarseRoles.indexOf(nam);
            return ind;
        }

        public static int getprimRoleInd(String nam){
            loadRoles();
            int ind=primRoles.indexOf(nam);
            return ind;
        }
        
        public static int getprimRoleSize(){
            loadRoles();
            return primRoles.size();
        }
        public static int getsecRoleSize(){
            loadRoles();
            return secRoles.size();
        }
        
        public static int getSecRoleInd(String nam){
            loadRoles();
            int ind=secRoles.indexOf(nam);
            return ind;
        }
            /**
            * gold predicate is when it's a predicate (verb) in the gold anotation and has children in the semantic part...
            * @param g
            * @param i
            * @return 
            */
            public static boolean isGoldPredicate(DetGraph g, int i){
                boolean ispred=false;
                if (g.getMot(i).getPOS().startsWith("VB")) {
                    List<Integer> childrenP=g.getFils(i);
                    if (childrenP.size()>0) ispred=true;
                }
                return ispred;
            }
            
            
            public static String getLemmafromFormFr(String f){
                String lem=f;
                boolean found=false;
                String[] beLems={"suis","es","est","sommes","êtes", "sont", "été", "étais", "était", "étions", "étiez", "étaient", "fus", "fut", 
                                 "fûmes", "fûtes", "furent", "serai", "seras", "sera", "serons", "serez", "seront", "serais", "serions", "serait", 
                                 "seraient", "sois", "soit", "soyons", "soyez", "soient", "sois"};
                String[] doLems={"do","does","did","done","doing"};
                String[] haveLems={"ai","as","a","avons","avez","ont","avais","avait","avions","aviez","avaient", "aurai","auras","aurons", "aurez",
                                   "auront","eus","eut","eûmes","eûtes","eurent","aie","ait","aies","ayons","ayez", "aient","ayant"};
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
            

            public static String getLemmafromForm(String f){
                f=f.toLowerCase();
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
            public static  boolean isPredicate(DetGraph g, int i){
                if (isFrench) {
                    boolean ispred=false;
                    if (g.getMot(i).getPOS().startsWith("V")){
                        ispred=true;
                        if (g.getNbMots()-1>i){
                        if (g.getMot(i+1).getPOS().startsWith("V"))ispred=false;
                        }
                        String lem=getLemmafromFormFr(g.getMot(i).getLemme());
                        if (lem.equals("be"))ispred=false;
                    }
                    return ispred;
                    
                }else return isPredicateEng(g, i);
            }
            
            public static  boolean isPredicateEng(DetGraph g, int i){
            boolean isPred=false;
            boolean mayBeAux=false;
            //verbal predicates don't include "to be"
            String lem=g.getMot(i).getLemme();
//            if (g.getMot(i).getLemme().equals(g.getMot(i).getForme()))
                lem=getLemmafromForm(g.getMot(i).getForme());
            if (g.getMot(i).getPOS().startsWith("VB") && !lem.equals("be") && !lem.contains("'")) {
                //identify by using the VC:  V1 -(VC)-> V2 : only V2 can be a predicate
                isPred=true;
                boolean isGoing=false;
                if (lem.contains("going")){
                    if (i>0 && getLemmafromForm(g.getMot(i-1).getForme()).equals("be")) isGoing=true;
                    else if (i>1 && g.getMot(i).getPOS().startsWith("RB") && getLemmafromForm(g.getMot(i-2).getForme()).equals("be")) isGoing=true;
                }
                if (lem.contains("have")||lem.contains("do")||isGoing) {
                    mayBeAux=true;
                    boolean got=false; boolean govb=false;
                    for (int n=i+1;n<g.getNbMots();n++){
                        //going is aux if: be + RB* + going + to + VB
                        if (lem.contains("going")){
                            if (g.getMot(n).getPOS().equals("TO"))got=true;
                            if (g.getMot(n).getPOS().equals("VB"))govb=true;
                            if (got&&govb) break;
                        }
                        //auxiliary if have+RB*+VBN/VBD || have+IN+ time NP?(the past) +VBD
                        if (lem.contains("have")){
                            if (g.getMot(n).getPOS().equals("VBN")&& g.getMot(n).getForme().equals("been")) break; //have been sth. or have been doing --> have is aux.
                            if (!g.getMot(n).getPOS().equals("RB")&&!g.getMot(n).getPOS().equals("VBD")&&!g.getMot(n).getPOS().equals("VBN")){
                                mayBeAux=false;
                                break;
                            }
                        }
                        //auxiliary if do+RB*+VB || VBP+PRP+VB (...do we expect) || do + RB* + IN+ Dt+NNS(by any means)
                        //see: utterance 925,1509,1515, of test
                        if (lem.contains("do")){
                            if (g.getMot(n).getPOS().equals("VB")) break;
                            if (!g.getMot(n).getPOS().equals("RB")){
                                mayBeAux=false; break;
                            }
                        }
                        if (!mayBeAux) break;
                    }
                    if (lem.contains("going")&& !(got&&govb) ) mayBeAux=false;
                }
                else mayBeAux=false;
                
                if (mayBeAux){
                    isPred=false;
                }
            }
            return isPred;
        }

        //semantic part: TODO!
        /**
         * return the voice of the predicate..
         * A priori, very basic: only considers the postag of the verb, and the postag and lemma of the previous words???
         * @param g
         * @param i
         * @return
         */
        public static int getVoice(DetGraph g, int i) {
            String posV= g.getMot(i).getPOS();
            if (posV.equals("VBN")){
                //if previous word (or word before) is have: Activ
                String prevVerb;
                for (int pv=i; pv>0; pv--){
                    if (g.getMot(pv).getPOS().startsWith("VB")){
                        //String lempv=g.getMot(pv).getLemme();
                        String lempv=getLemmafromForm(g.getMot(pv).getForme());
                        if (lempv.startsWith("be")) return vpas;
                        if (lempv.startsWith("have")) return vact;
                    }
                }
                if (g.getMot(i+1).getForme().startsWith("by")) return vpas;
                return vact;
            }else return vact;
        }




}
