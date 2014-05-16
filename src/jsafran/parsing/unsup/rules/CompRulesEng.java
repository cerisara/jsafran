package jsafran.parsing.unsup.rules;

import java.io.Serializable;
import java.util.*;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.Mot;
import jsafran.parsing.unsup.rules.CompRules.SingleRule;
import jsafran.parsing.unsup.SemanticProcesing;

/**
 * rules complementaires des rules definies dans synats.cfg
 * 
 * @author cerisara
 *
 * TODO: ALE: 1) split the initialization into rules
 *            2) don't allow that a rule modifies a dependent!! only possible to add dependents!!!!
 *
 */
public class CompRulesEng implements Rules, Serializable {
	private static final long serialVersionUID = 1L;
	//	public interface SingleRule {
	//		public int[] getApplicable(DetGraph g);
	//		public int[] applyRule(DetGraph g, int pos);
	//		public String getName();
	//	}
	ArrayList<SingleRule> localrules = new ArrayList<SingleRule>();
        boolean srl=false;
	//int[] chunks = null;
	//        List<DetGraph> list_gs=new ArrayList<DetGraph>();
	//        List<int[]> list_chunks= new ArrayList<int[]>();
	//        List<Integer> list_sentheads=new ArrayList<Integer>();
	//        int sentHead;
	// save, pour chaque graphe du corpus, sa version "collins only"
	HashMap<DetGraph, DetGraph> graphsBaseline = new HashMap<DetGraph, DetGraph>();


	public CompRulesEng() {
		//init naseem rules:
//		localrules.add(new r3NaseemInit());
//		System.out.println("Load r3NaseemInit () rule....");
//		localrules.add(new r4NaseemInit());
//		System.out.println("Load r4NaseemInit rule....");
//		localrules.add(new rDEP());
//		System.out.println("Load rDEP rule....");
//		localrules.add(new rPrepBefore());
//		System.out.println("Load rPrepBefore rule....");
//
//		localrules.add(new rPOS());
//		System.out.println("Load rPOS rule....");
//		localrules.add(new rCOORD1());
//		System.out.println("Load rCOORD1 rule....");
//		localrules.add(new rCOORD2());
//		System.out.println("Load rCOORD2 rule....");
//		localrules.add(new rCOORD3());
//		System.out.println("Load rCOORD3 rule....");
//		localrules.add(new rAint());
//		System.out.println("Load rAint rule....");
//		localrules.add(new rIM());
//		System.out.println("Load rIM rule....");
//		localrules.add(new rRBl());
//		System.out.println("Load rRBl rule....");
//		localrules.add(new rRBr());
//		System.out.println("Load rRBr rule....");
//		// ne sert a rien ici
////		localrules.add(new rREL());
//		System.out.println("Load rREL rule....");
//		localrules.add(new rSUJREL());
//		System.out.println("Load rSUJREL rule....");
//		localrules.add(new rSUJRIGHT());
//		System.out.println("Load rSUJRIGHT rule....");
//		localrules.add(new rVC());
//		System.out.println("Load rVC rule....");
//		localrules.add(new rNAME());
//		localrules.add(new rPRD());
//		localrules.add(new rTMP());
//		localrules.add(new rAMOD());
//		localrules.add(new rOPRD());
//		localrules.add(new rPOSTHON());
//		localrules.add(new rHMOD());
//		localrules.add(new rOBJ());
	}
        
        @Override
        public void setSRL(boolean s){
            System.out.println("srl set to:");
            srl=s;
        }

	@Override
	public int getNrules() {
		return localrules.size();
	}

	@Override
	public int[] getApplicable(int ridx, DetGraph g) {
		return localrules.get(ridx).getApplicable(g);
	}

	@Override
	public double getRulePrior(int ridx) {
		return 1;
	}

	@Override
	public int[] applyRule(int ridx, DetGraph g, int pos) {
		return localrules.get(ridx).applyRule(g, pos);

	}

	@Override
	public void applyDetRules(DetGraph g) {
		
	}

	@Override
	public String toString(int ridx) {
		if (ridx==0) return "SUFFIX";
		if (ridx==1) return "COORD1";
		if (ridx==2) return "COORD2";
		if (ridx==3) return "COORD3";
		if (ridx==4) return "SUBJECT";
		if (ridx==5) return "VC";
		if (ridx==6) return "REL";
		if (ridx==7) return "RBL";
		if (ridx==8) return "RBR";
		if (ridx==9) return "IM";
		if (ridx==10) return "AINT";
		if (ridx==11) return "DELDEP";
		return null;
	}

	@Override
	public void init(List<DetGraph> gs) {
                if (srl){
                    //only syntax??
                    localrules.add(new r3NaseemInit());
                    localrules.add(new rDEP());
                    localrules.add(new rCOORD1());
                    System.out.println("Load rCOORD1 rule....");
                    localrules.add(new rCOORD2());
                    System.out.println("Load rCOORD2 rule....");
                    localrules.add(new rCOORD3());
                    System.out.println("Load rCOORD3 rule....");
                    localrules.add(new rVC());
                    System.out.println("Load rVC rule....");
                    localrules.add(new rNAME());
                    localrules.add(new rAMOD());
                    localrules.add(new rHMOD());
                    localrules.add(new rPOSTHON());
                    localrules.add(new rIM());
                    localrules.add(new rPOS());
                    
                    //to add the possibility to have the syntatcti rules only
//                    localrules.add(new r4NaseemInit());
//                    localrules.add(new rPrepBefore());
//                    localrules.add(new rAint());
//                    localrules.add(new rSUJREL());
//                    localrules.add(new rSUJRIGHT());
//                    localrules.add(new rTMP());
//                    localrules.add(new rOPRD());
//                    localrules.add(new rOBJ());
                    localrules.add(new rRBl());
                    localrules.add(new rRBr());
                    localrules.add(new rPRD());
                    
                    
                    
                    //syntax + semantic
                    localrules.add(new r4NaseemInitSRL());
                    localrules.add(new r4NaseemInitSRLBis());
                    localrules.add(new rSUJRIGHTA0());
                    localrules.add(new rSUJRIGHTA1());
                    localrules.add(new rOBJSRLA1());
                    localrules.add(new rOBJSRLA0());
                    localrules.add(new rOPRDA1());
                    localrules.add(new rOPRDA2());
                    localrules.add(new rDIRA4());
                    localrules.add(new rPRDA1());
                    localrules.add(new rPRDA2());
                    localrules.add(new rPRDA3());
                    localrules.add(new rSUJRELA0());
                    localrules.add(new rTMPAM()); //TODO: when using srl, delete hte rule rtmp!!! otherwise the ambiguity augments without any sense
                    localrules.add(new rRBlAM());
                    localrules.add(new rRBrAM());
                    localrules.add(new rAintAM());
                    localrules.add(new rLGSA0());
                    localrules.add(new rPrepBeforeSRL());
                    localrules.add(new rPrepBeforeSRLbis());
                    
                }else{
                    localrules.add(new r3NaseemInit());
                    System.out.println("Load r3NaseemInit () rule....");
                    localrules.add(new r4NaseemInit());
                    System.out.println("Load r4NaseemInit rule....");
                    localrules.add(new rDEP());
                    System.out.println("Load rDEP rule....");
                    localrules.add(new rPrepBefore());
                    System.out.println("Load rPrepBefore rule....");

                    localrules.add(new rPOS());
                    System.out.println("Load rPOS rule....");
                    localrules.add(new rCOORD1());
                    System.out.println("Load rCOORD1 rule....");
                    localrules.add(new rCOORD2());
                    System.out.println("Load rCOORD2 rule....");
                    localrules.add(new rCOORD3());
                    System.out.println("Load rCOORD3 rule....");
                    localrules.add(new rAint());
                    System.out.println("Load rAint rule....");
                    localrules.add(new rIM());
                    System.out.println("Load rIM rule....");
                    localrules.add(new rRBl());
                    System.out.println("Load rRBl rule....");
                    localrules.add(new rRBr());
                    System.out.println("Load rRBr rule....");
                    // ne sert a rien ici
    //		localrules.add(new rREL());
                    System.out.println("Load rREL rule....");
                    localrules.add(new rSUJREL());
                    System.out.println("Load rSUJREL rule....");
                    localrules.add(new rSUJRIGHT());
                    System.out.println("Load rSUJRIGHT rule....");
                    localrules.add(new rVC());
                    System.out.println("Load rVC rule....");
                    localrules.add(new rNAME());
                    localrules.add(new rPRD());
                    localrules.add(new rTMP());
                    localrules.add(new rAMOD());
                    localrules.add(new rOPRD());
                    localrules.add(new rPOSTHON());
                    localrules.add(new rHMOD());
                    localrules.add(new rOBJ());
                }

                //TODO: is it ok here?
		//            list_gs=gs;
		for(DetGraph g: gs) {
			initGraph(g);
		}
		System.out.println("Finish English rules initialization...");
		//            JSafran.viewGraph(gs.get(0));
	}

	// =================================================================
        
        
	private	boolean ismod(Mot mmot) {
			if ( mmot.getPOS().equals("MD")) return true;
			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been", "should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
                        final String[] mods = {"been", "should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s","going"};
			for (String m : mods)
				if (mot.equals(m)) return true;
			return false;
		}
        

	private boolean isNumber(DetGraph g, int i) {
		char c=g.getMot(i).getForme().charAt(0);
		return (Character.isDigit(c));
	}


	// =================================================================
	/**
	 * Naseem et al. 2010 prechunking of NPs:
	 * 1) Identify non-recursive NP:
	 *  a- all nouns, pronouns and possessive markers are part of an NP
	 *  b- all adjectives, conjunctions and determiners immediately preceding an NP are part of the NP
	 * 2) The first verb or modal in the sentences is the headword
	 * 3)(Rule, not initialization) all words in an NP are headed by the last word in the NP
	 * 4)(Rule, not initialization)
	 *   a- the last word in an NP is headed by the word immediately before the NP if it is a preposition,
	 *   b- otherwise (the word immediately before an NP is not preposition), the last word in an NP is headed by the headword of the sentence if the NP is before the headword
	 *   c- otherwise, the las word in an NP is headed by the word preceding the NP
	 * 5)(Rule, not initialization) 
	 *  a- For the fist word set its head to be the head of the sentence.
	 *  b- For each other word, it's the previous word.
	 */
	public void initGraph(DetGraph g) {
		g.clearDeps();
		// 1) Identify non-recursive NP
		int[]chunks = new int[g.getNbMots()];
		Arrays.fill(chunks,-1);
		for (int i=0;i<g.getNbMots();i++) {
			String pos = g.getMot(i).getPOS();
			// a- all nouns, pronouns and possessive markers are part of an NP
			if (pos.startsWith("NN")||pos.startsWith("VBG")||pos.startsWith("PRP")||pos.startsWith("WP")||pos.startsWith("POS")||pos.startsWith("CD")||pos.startsWith("_")) {
//			if (pos.startsWith("NN")||pos.startsWith("PRP")||pos.startsWith("WP")||pos.startsWith("POS")||pos.startsWith("CD")||pos.startsWith("_")) {
				chunks[i]=0;
			}
		}
		for (int i=g.getNbMots()-2;i>=0;i--) {
			String pos = g.getMot(i).getPOS();
			// b- all adjectives, conjunctions and determiners immediately preceding an NP are part of the NP
			if (pos.startsWith("JJ")||pos.startsWith("CC")||pos.startsWith("DT")||pos.startsWith("CD")) {
				if (chunks[i+1]>=0) chunks[i]=chunks[i+1];
			}
		}
		// xtof: NPs boundaries ??
		int npidx=0;
		for (int i=0;i<g.getNbMots();i++) {
			if (chunks[i]>=0) {
				chunks[i]=npidx;
			} else if (i>0 && chunks[i-1]>=0) npidx++;
		}

		// mark NP as groups
		for (int i=0;i<g.getNbMots();i++) {
			if (chunks[i]>=0) {
				int j=i+1;
				for (;j<g.getNbMots();j++) {
					if (chunks[j]!=chunks[i]) break;
				}
				//g.addgroup(i, j-1, "GN"+chunks[i]);
				g.addgroup(i, j-1,""+(j-1) ); //save the last word in the group as the name of the group
				i=j-1;
			}
		}
		//add fixed links those that goes and are not sampled afterwardas
		int sentHead=findSentHead(g);
		if (sentHead>-1){
			for (int i=0;i<g.getNbMots();i++){
				String pos=g.getMot(i).getPOS();
				if (pos.equals(".")||pos.equals(",")||pos.equals('"')) g.ajoutDep("P", i, sentHead);
			}
		}

	}
	private int findSentHead(DetGraph g){
		int sentHead=-1;
		int altsent=-1; 
		for (int i=0;i<g.getNbMots();i++) {
			String pos = g.getMot(i).getPOS();
			if (pos.startsWith("VB")||pos.startsWith("MD")) {sentHead=i; break;}
			if (pos.startsWith("N")) altsent=i;
		}
		if (sentHead<0) {
			// sinon, premier head d'un NP sans PREP devant
			// TODO
			//                    System.out.println("No head for the sentence!: "+ g);
			//sentHead=altsent;
		}
		return sentHead;
	}


	// =================================================================
	/**
	 * 3)(Rule, it belongs to the part of initialization in Naseem 2010)
	 *  All words in an NP are headed by the last word in the NP
	 *
	 */
	class r3NaseemInit implements SingleRule {
		public String getName() {return "initNMOD";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				for (int j=0;j<g.groups.get(i).size()-1;j++) {
					pp.add(g.groups.get(i).get(j).getIndexInUtt()-1);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			int[] grss=g.getGroups(pos);
			int lastNPword = Integer.parseInt(g.getGroupName(grss[0]));
			String dlab="NMOD";
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep(dlab, pos,lastNPword );
			int[] res = {pos};
			return res;
		}
	}

	// =================================================================
	/**
	 *  4)(Rule, it belongs to the part of initialization in Naseem 2010)
	 *   a- the last word in an NP is headed by the word immediately before the NP if it is a preposition,
	 *   //b- otherwise (the word immediately before an NP is not preposition), the last word in an NP is headed by the headword of the sentence if the NP is before the headword
	 *   //c- otherwise, the las word in an NP is headed by the word preceding the NP
	 */

	class r4NaseemInit implements SingleRule {
		public String getName() {return "initPMOD";}

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				boolean treated = false;
				int precedingWord = g.groups.get(i).get(0).getIndexInUtt()-2;
				if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN")) {
					pp.add(i);
					treated=true;
				}
				if (treated) continue;
				
				int lastWordInNP = g.groups.get(i).get(g.groups.get(i).size()-1).getIndexInUtt()-1;
				int firstverb=-1;
				for (int j=0;j<g.getNbMots();j++) {
					if (g.getMot(j).getPOS().startsWith("VB")||g.getMot(j).getPOS().startsWith("MD")) {
						firstverb=j; break;
					}
				}
				if (firstverb>lastWordInNP) {
					pp.add(i);
					treated=true;
				}
				if (treated) continue;

				if (precedingWord>=0) pp.add(i);
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			int lastWordInNP = g.groups.get(pos).get(g.groups.get(pos).size()-1).getIndexInUtt()-1;
			int d = g.getDep(lastWordInNP);
			if (d>=0) g.removeDep(d);
			boolean treated = false;
			int precedingWord = g.groups.get(pos).get(0).getIndexInUtt()-2;
			if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN")) {
				g.ajoutDep("PMOD",lastWordInNP,precedingWord);
				treated=true;
			}
			int[] res = {lastWordInNP};
			if (treated) return res;
			
			int firstverb=-1;
			for (int j=0;j<g.getNbMots();j++) {
				if (g.getMot(j).getPOS().startsWith("VB")||g.getMot(j).getPOS().startsWith("MD")) {
					firstverb=j; break;
				}
			}
			if (firstverb>lastWordInNP) {
				g.ajoutDep("SBJ",lastWordInNP,firstverb);
				treated=true;
			}
			if (treated) return res;
			
			if (precedingWord>=0) {
				if (g.getMot(precedingWord).getPOS().startsWith("VB")||g.getMot(precedingWord).getPOS().startsWith("MD"))
					g.ajoutDep("OBJ",lastWordInNP,precedingWord);
				else g.ajoutDep("DEP",lastWordInNP,precedingWord);
				return res;
			}
			return null;
		}
	}

	// =================================================================
	/**
	 * 5)(Rule, not initialization)
	 *  a- For the fist word set its head to be the head of the sentence.
	 */

	class rDEP implements SingleRule {
		public String getName() {return "DEP";}
		public int[] getApplicable(DetGraph g) {
			if (g.getNbMots()==0) {
				int[] res = {}; return res;
			}
			ArrayList<Integer> pp = new ArrayList<Integer>();
			String w = g.getMot(0).getForme().toLowerCase();
			if (w.equals("but")||w.equals("or")||w.equals("and")||w.equals("then")) {
				int sentHead=findSentHead(g);
				if (sentHead>0) pp.add(-sentHead);
			}
			for (int i=0;i<g.getNbMots()-1;i++) {
				if (g.getMot(i).getPOS().startsWith("CD")&&g.getMot(i+1).getPOS().startsWith("CD")) {
					pp.add(i); i++;
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}
		public int[] applyRule(DetGraph g, int pos) {
			if (pos<0) {
				int senthead = -pos;
				g.ajoutDep("DEP", 0, senthead);
				int[] res = {pos};
				return res;
			} else {
				if (pos>0&&g.getMot(pos-1).equals("$")) {
					g.ajoutDep("DEP", pos, pos-1);
					g.ajoutDep("DEP", pos+1, pos-1);
					int[] res = {pos,pos+1};
					return res;
				} else {
					g.ajoutDep("DEP", pos, pos+1);
					int[] res = {pos};
					return res;
				}
			}
		}
	}

	// =================================================================
	/**
	 * 5)(Rule, not initialization)
	 *  //a- For the fist word set its head to be the head of the sentence.
	 *  b- For each other word, it's the previous word.
	 *  
	 *  Link any (NP,prep) with backward NMOD
	 */

	class rPrepBefore implements SingleRule {
		private ArrayList<Integer> gov  = new ArrayList<Integer>();
		private ArrayList<Integer> head = new ArrayList<Integer>();
		private ArrayList<Integer> labt = new ArrayList<Integer>();
		public String getName() {return "prepbefore";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().equals("IN")) {
					if (i==0) {
						int root = findSentHead(g);
						if (root>=0) {
							pp.add(head.size()); gov.add(i); head.add(root); labt.add(Dep.getType("ADV"));
							// it can also be a LOC or TMP
						} else {
							// TODO
						}
					} else {
						int[] grs = g.getGroupsThatEndHere(i-1);
						if (grs!=null&&grs.length>0) {
							// NP before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("NMOD"));
						} else if (g.getMot(i-1).getPOS().startsWith("VB")) {
							// verb before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("ADV"));
						} else if (g.getMot(i-1).getPOS().startsWith("IN")) {
							// prep before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("PMOD"));
						} else {
							// TODO
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int i) {
			int pos=gov.get(i);
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep(Dep.depnoms[labt.get(i)], pos, head.get(i));
			int[] res = {pos};
			return res;
		}
	}

	// =================================================================
	/**
	 * R2: Link any POS word to the preceding word with SUFFIX
	 */
	class rPOS implements SingleRule {
		public String getName() {return "SUFFIX";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=1;i<g.getNbMots();i++) {
				String pos = g.getMot(i).getPOS();
				if (pos.startsWith("POS")) {
					pp.add(i);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			String dlab="SUFFIX";
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep(dlab, pos, pos-1);
			int[] res = {pos};
			return res;
		}
	}

	// =================================================================
	/**
	 * R3-1 (for d=1): Link any CC word wt to wt-d with COORD.
	 * Also link the first following word wt'>wt that has the same postag than wt-d to wt with CONJ
	 */
	class rCOORD1 implements SingleRule {
		public String getName() {return "COORD1";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=1;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().startsWith("CC")) {
					// cherche si il y a le meme POS a droite
					String posl = g.getMot(i-1).getPOS();
					if (posl.length()>2) posl=posl.substring(0,2);
					for (int j=i+1;j<g.getNbMots()&&j<i+5;j++) {
						String posr = g.getMot(j).getPOS();
						if (posr.length()>2) posr=posr.substring(0,2);
						if (posr.equals(posl)) {
							pp.add(i);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			ArrayList<Integer> changed= new ArrayList<Integer>();
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("COORD", pos, pos-1);
			changed.add(pos);
			String posl = g.getMot(pos-1).getPOS();
			if (posl.length()>2) posl=posl.substring(0,2);
			for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
				String posr = g.getMot(i).getPOS();
				if (posr.length()>2) posr=posr.substring(0,2);
				if (posr.equals(posl)) {
					d = g.getDep(i);
					if (d>=0) g.removeDep(d);
					g.ajoutDep("CONJ", i, pos);
					changed.add(i);
				}
			}

			int[] res = new int[changed.size()];
			for (int i=0;i<res.length;i++) res[i] = changed.get(i);
			return res;
		}
	}

	// =================================================================
	/**
	 * R3-2 (for d=2): Link any CC word wt to wt-d with COORD.
	 * Also link the first following word wt'>wt that has the same postag than wt-d to wt with CONJ
	 */
	class rCOORD2 implements SingleRule {
		public String getName() {return "COORD2";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				if (i>=2 && g.getMot(i).getPOS().startsWith("CC")){
					// cherche si il y a le meme POS a droite
					String posl = g.getMot(i-2).getPOS();
					if (posl.length()>2) posl=posl.substring(0,2);
					for (int j=i+1;j<g.getNbMots()&&j<i+5;j++) {
						String posr = g.getMot(j).getPOS();
						if (posr.length()>2) posr=posr.substring(0,2);
						if (posr.equals(posl)) {
							pp.add(i);
						}
					}

				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			ArrayList<Integer> changed= new ArrayList<Integer>();
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("COORD", pos, pos-2);
			changed.add(pos);
			String posl = g.getMot(pos-2).getPOS();
			if (posl.length()>2) posl=posl.substring(0,2);
			for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
				String posr = g.getMot(i).getPOS();
				if (posr.length()>2) posr=posr.substring(0,2);
				if (posr.equals(posl)) {
					d = g.getDep(i);
					if (d>=0) g.removeDep(d);
					g.ajoutDep("CONJ", i, pos);
					changed.add(i);
				}
			}

			int[] res = new int[changed.size()];
			for (int i=0;i<res.length;i++) res[i] = changed.get(i);
			return res;
		}
	}

	// =================================================================
	/**
	 * R3-3 (for d=3): Link any CC word wt to wt-d with COORD.
	 * Also link the first following word wt'>wt that has the same postag than wt-d to wt with CONJ
	 */
	class rCOORD3 implements SingleRule {
		public String getName() {return "COORD3";}

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				if (i>=3 && g.getMot(i).getPOS().startsWith("CC")){
					// cherche si il y a le meme POS a droite
					String posl = g.getMot(i-3).getPOS();
					if (posl.length()>2) posl=posl.substring(0,2);
					for (int j=i+1;j<g.getNbMots()&&j<i+5;j++) {
						String posr = g.getMot(j).getPOS();
						if (posr.length()>2) posr=posr.substring(0,2);
						if (posr.equals(posl)) {
							pp.add(i);
						}
					}

				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			ArrayList<Integer> changed= new ArrayList<Integer>();
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("COORD", pos, pos-3);
			changed.add(pos);
			String posl = g.getMot(pos-3).getPOS();
			if (posl.length()>2) posl=posl.substring(0,2);
			for (int i=pos+1;i<g.getNbMots()&&i<pos+5;i++) {
				String posr = g.getMot(i).getPOS();
				if (posr.length()>2) posr=posr.substring(0,2);
				if (posr.equals(posl)) {
					d = g.getDep(i);
					if (d>=0) g.removeDep(d);
					g.ajoutDep("CONJ", i, pos);
					changed.add(i);
				}
			}

			int[] res = new int[changed.size()];
			for (int i=0;i<res.length;i++) res[i] = changed.get(i);
			return res;
		}
	}

	// =================================================================
	/**
	 * R4: Link any NN word wt to wt+k with SBJ, if wt+k is a verb
	 */
	class rSUJRIGHT implements SingleRule {
		public String getName() {return "SUBJECT";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				String postag = g.getMot(pos).getPOS();
				int[] grss=g.getGroups(pos);
				int lastNPword=-1;
				if (grss!=null && grss.length>0)  lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
				if (pos==lastNPword &&(postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))) {
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("MD") || (g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
							pp.add(pos);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			String postag = g.getMot(pos).getPOS();
			int[] grss=g.getGroups(pos);
			int lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
			if (lastNPword==pos && (postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))){
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
				for (int i=pos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("MD") || (g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
						g.ajoutDep("SBJ", pos, i);
						int[] res={pos};
						return res;
					}
				}
			}
			System.out.println("Error with rule SBJ "+pos+" "+g);
			return null;
		}
	}

        class rSUJREL implements SingleRule {
		public String getName() {return "SUBJECTREL";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				String postag = g.getMot(pos).getPOS();
				if (postag.equals("WDT")) {
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("MD") || (g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
							pp.add(pos);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			for (int i=pos+1;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().startsWith("MD") || (g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
					g.ajoutDep("SBJ", pos, i);
					if (pos>0) {
						String postag = g.getMot(pos-1).getPOS();
						if (postag.startsWith("NN")) {
							g.ajoutDep("NMOD", i, pos-1);
							int[] res={pos,i};
							return res;
						} 
					}
					int[] res={pos};
					return res;
				}
			}
			return null;
		}
	}

	// =================================================================
	/**
	 * R5: Link any verb wt to a modal wt-k with VC, with 1<=k<=5
	 */
	class rVC implements SingleRule{
		public String getName() {return "VC";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots();pos++) {
				if (g.getMot(pos).getPOS().startsWith("VB")||g.getMot(pos).getPOS().startsWith("TO")){
					int prevMD = -1;
					for (int i=pos-1;i>=0&&i>=pos-5;i--){
                                            if (ismod(g.getMot(i))) {
                                                    prevMD=i; break;
                                            } else {
                                                if (g.getMot(i).getPOS().startsWith("VB")) break;
                                                if (!g.getMot(i).getPOS().startsWith("RB")) break;
                                            }
                                        }
					if (prevMD>=0) {
						pp.add(pos);
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			for (int i=pos-1;i>=0;i--)
				if (ismod(g.getMot(i))) {
					int d = g.getDep(pos);
					if (d>=0) g.removeDep(d);
					g.ajoutDep("VC", pos, i);
					int[] res={pos};
					return res;
				}
			System.out.println("Error with rule VC "+pos+" "+g);
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if (false) return mmot.getPOS().equals("MD");
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}

	}


	// =================================================================
	/**
	 * R6: link any verb that is the head of a relative conjunction preceded by a noun, to this noun with NMOD
	 */
	class rREL implements SingleRule{
		public String getName() {return "REL";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=1;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getPOS().startsWith("WDT")) {
					int d = g.getDep(pos);
					if (d>=0) {
						int h = g.getHead(d);
						if (g.getMot(h).getPOS().startsWith("MD") || (g.getMot(h).getPOS().startsWith("VB") && !g.getMot(h).getPOS().startsWith("VBG"))) {
							String postag = g.getMot(pos-1).getPOS();
							if (postag.startsWith("NN")) {
								pp.add(pos);
							} 
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d = g.getDep(pos);
			int h = g.getHead(d);
			d = g.getDep(h);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("NMOD", h, pos-1);
			int[] res={pos};
			return res;
		}
	}


	// =================================================================
	/**
	 * R7: link ? 
	 */
	class rRBl implements SingleRule{
		public String getName() {return "RBl";}
		public int[] getApplicable(DetGraph g){
			int closest;
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=1;pos<g.getNbMots();pos++) {
				if (g.getMot(pos).getPOS().startsWith("RB")){
					closest = -1;
					for (int i=pos-1;i>=0;i--) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							if (closest<0||pos-i<closest-i)
								closest=i;
							break;
						}
					}
					if (closest>=0) 
						pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int closest = -1;
			for (int i=pos-1;i>=0;i--) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
					if (closest<0||pos-i<closest-i)
						closest=i;
					break;
				}
			}
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ADV", pos, closest);
			int[] res={pos};
			return res;
		}
	}




	// =================================================================
	class rRBr implements SingleRule{
		public String getName() {return "RBr";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getPOS().startsWith("RB")){
					int closest = -1;
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							closest=i;
							break;
						}
					}
					if (closest>=0) 
						pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int closest = -1;
			for (int i=pos+1;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
					closest=i;
					break;
				}
			}
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ADV", pos, closest);
			int[] res={pos};
			return res;
		}
	}

	// =================================================================
	class rIM implements SingleRule{
		public String getName() {return "IM";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=1;pos<g.getNbMots();pos++) {
				if (g.getMot(pos).getPOS().equals("VB")){
					if (g.getMot(pos-1).getForme().equals("to")) 
						pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("IM", pos, pos-1);
			int[] res={pos};
			return res;
		}
	}


	// =================================================================
	class rAint implements SingleRule{
		public String getName() {return "Aint";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots();pos++) {
				if (pos>0&&(g.getMot(pos).getForme().equals("n't")||g.getMot(pos).getForme().equals("not"))) {
					pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ADV", pos, pos-1);
			int[] res={pos};
			return res;
		}
	}

	// =================================================================
	class rNAME implements SingleRule{
		public String getName() {return "NAME";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getPOS().equals("NNP")&&g.getMot(pos+1).getPOS().equals("NNP")) {
					pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("NAME", pos, pos+1);
			int[] res={pos};
			return res;
		}
	}

	// =================================================================
	class rPRD implements SingleRule{
		public String getName() {return "PRD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			int etreavant = -1;
			for (int pos=0;pos<g.getNbMots();pos++) {
				if (g.getMot(pos).getLemme().equals("be")||g.getMot(pos).getLemme().equals("feel")) {
					etreavant=pos; continue;
				}
				if (etreavant>=0&&(g.getMot(pos).getPOS().startsWith("JJ")||g.getMot(pos).getPOS().startsWith("RB"))) {
					pp.add(pos);
				} else if (etreavant>=0&&g.getMot(pos).getForme().equals("to")) {
					pp.add(pos);
				} else if (etreavant>=0) {
					int[] gr=g.getGroupsThatEndHere(pos);
					if (gr!=null&&gr.length>0) pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			for (int i=pos-1;i>=0;i--) {
				if (g.getMot(i).getLemme().equals("be")||g.getMot(i).getLemme().equals("feel")) {
					g.ajoutDep("PRD", pos, i);
					int[] res={pos};
					return res;
				}
			}
			return null;
		}
	}

	// =================================================================
	class rTMP implements SingleRule{
		final String[] tps = {"today","tomorrow","yesterday"};
		public String getName() {return "TMP";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots();pos++) {
				boolean maybeTMP=false;
				if (Arrays.binarySearch(tps, g.getMot(pos).getLemme())>=0) maybeTMP=true;
				if (!maybeTMP) {
					String po = g.getMot(pos).getPOS();
					if (po.startsWith("RB")||po.startsWith("IN")) maybeTMP=true;
				}
				if (maybeTMP) {
					for (int i=pos-1;i>=0;i--) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							pp.add(pos); break;
						}
					}
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							pp.add(-pos); break;
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			if (pos>0) {
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				for (int i=pos-1;i>=0;i--) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						g.ajoutDep("TMP", pos, i);
						int[] res={pos};
						return res;
					}
				}
			} else {
				pos=-pos;
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				for (int i=pos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						g.ajoutDep("TMP", pos, i);
						int[] res={pos};
						return res;
					}
				}
			}
			return null;
		}
	}


	// =================================================================
	class rAMOD implements SingleRule{
		public String getName() {return "AMOD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				if ((g.getMot(pos).getPOS().startsWith("RB")||g.getMot(pos).getPOS().startsWith("JJ")) &&
						(g.getMot(pos+1).getPOS().startsWith("IN")||g.getMot(pos+1).getPOS().startsWith("TO"))) pp.add(pos);
				else if (g.getMot(pos).getPOS().startsWith("RB")&&g.getMot(pos+1).getPOS().startsWith("JJ")) pp.add(-pos-1);
				else if (g.getMot(pos).getPOS().startsWith("RB")&&g.getMot(pos+1).getPOS().startsWith("RB")) pp.add(pos);
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			if (pos>=0) {
				int d=g.getDep(pos+1);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("AMOD", pos+1, pos);
				int[] res={pos+1};
				return res;
			} else {
				pos=-pos-1;
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
				g.ajoutDep("AMOD", pos, pos+1);
				int[] res={pos};
				return res;
			}
		}
	}


	// =================================================================
	class rOPRD implements SingleRule{
		public String getName() {return "OPRD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getPOS().startsWith("VB")&&(g.getMot(pos+1).getPOS().startsWith("TO")||g.getMot(pos+1).getPOS().startsWith("VBG"))) pp.add(pos);
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			g.ajoutDep("OPRD", pos+1, pos);
			int[] res={pos};
			return res;
		}
	}
	// =================================================================
	class rPOSTHON implements SingleRule{
		public String getName() {return "POSTHON";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getPOS().startsWith("NNP")) {
					String nw = g.getMot(pos+1).getForme().toLowerCase().replace('.', ' ').trim();
					if (nw.equals("corp")||nw.equals("inc")||nw.equals("jr")||nw.equals("co")||nw.equals("ltd")) pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			g.ajoutDep("POSTHON", pos+1, pos);
			int[] res={pos};
			return res;
		}
	}
	// =================================================================
	class rHMOD implements SingleRule{
		public String getName() {return "HMOD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=1;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getForme().equals("_") && g.getMot(pos+1).getForme().equals("_")) {
					if (pos+3<g.getNbMots()&& g.getMot(pos+2).getForme().equals("_") && g.getMot(pos+3).getForme().equals("_")) {
						pp.add(-pos-1);
						pos+=3;
					} else {
						pp.add(pos);
						pos++;
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			if (pos>=0) {
				g.ajoutDep("HMOD", pos-1, pos+1);
				g.ajoutDep("HYPH", pos, pos-1);
				int[] res={pos-1,pos};
				return res;
			} else {
				pos=-pos-1;
				g.ajoutDep("HYPH", pos, pos-1);
				g.ajoutDep("HYPH", pos+2, pos+1);
				g.ajoutDep("HMOD", pos+1, pos+3);
				g.ajoutDep("HMOD", pos-1, pos+3);
				int[] res={pos-1,pos,pos+1,pos+2};
				return res;
			}
		}
	}
	// =================================================================
	class rOBJ implements SingleRule{
		public String getName() {return "OBJ";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbGroups();pos++) {
			    int headGN = Integer.parseInt(g.getGroupName(pos));
			    for (int i=headGN-1;i>=0;i--) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
				    pp.add(headGN);
				}
			    }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
		    for (int i=pos-1;i>=0;i--) {
			if (g.getMot(i).getPOS().startsWith("VB")) {
			    g.ajoutDep("OBJ",pos,i);
			    int[] res={pos};
			    return res;
			}
		    }
		    return null;
		}
	}
        
        // ###################################################################
        //      SEMANTIC RULES: 
        // ###################################################################

	// =================================================================
        
	class rOBJSRLA1 implements SingleRule{
		public String getName() {return "OBJA1";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbGroups();pos++) {
			    int headGN = Integer.parseInt(g.getGroupName(pos));
			    for (int i=headGN-1;i>=0;i--) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
				    pp.add(headGN);
				}
			    }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
		    for (int i=pos-1;i>=0;i--) {
			if (g.getMot(i).getPOS().startsWith("VB")) {
                            int[] depsWithPos=gsrl.getDeps(pos);
//                            int ds = gsrl.getDep(pos);
//                            if (ds>=0) 
//                                gsrl.removeDep(ds);
//                                gsrl.removeAllDeps(pos);
			    g.ajoutDep("OBJ",pos,i);
                            if (SemanticProcesing.isPredicate(g, i)) {
                                //first remove the dependence if any exist... 
                                for (int dd: depsWithPos){
                                    if (gsrl.getHead(dd)==i) gsrl.removeDep(dd); break;
                                }
                                gsrl.ajoutDep("A1", pos, i);
                            }
                            else{
                                //see if it's vc-chain... (RB|Mods)*VB
                                if (ismod(g.getMot(i))) {
                                    //look if there is a verb following the chain of modifiers or rb
                                    boolean foundVB=false;
                                    int posvb=0;
                                    for (int nw=i+1;nw<g.getNbMots();nw++){
                                        if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                            foundVB=true; posvb=nw;
                                            break;
                                        }
                                        if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                            break;
//                                        {//look for been able to: been is in mod, 
//                                            if (g.getNbMots()<=nw+1) break;
//                                            else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                    break;
//                                        }
                                    }
                                    //when we find the end of the vcchain add the semantic link
                                    //first remove the dependence if any exist... 
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                    }
                                    if (foundVB && pos!=posvb) gsrl.ajoutDep("A1", pos, posvb);
                                }
                            }
			    int[] res={pos};
			    return res;
			}
		    }
		    return null;
		}
                
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true;
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been", "should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

        // =================================================================
	class rOBJSRLA0 implements SingleRule{
		public String getName() {return "OBJA0";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbGroups();pos++) {
			    int headGN = Integer.parseInt(g.getGroupName(pos));
			    for (int i=headGN-1;i>=0;i--) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
				    pp.add(headGN);
				}
			    }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
		    for (int i=pos-1;i>=0;i--) {
			if (g.getMot(i).getPOS().startsWith("VB")) {
                            int[] depsWithPos=gsrl.getDeps(pos);
//                            int ds = gsrl.getDep(pos);
//                            if (ds>=0) gsrl.removeDep(ds);
//                            gsrl.removeAllDeps(pos);
                                //first remove the dependence if any exist... 
			    g.ajoutDep("OBJ",pos,i);
                            if (SemanticProcesing.isPredicate(g, i) && pos!=i){
                                for (int dd: depsWithPos){
                                    if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                }
                                gsrl.ajoutDep("A0", pos, i);
                                //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
				if (i+2<g.getNbMots()){
                                    if (g.getMot(i+1).getForme().equals("to") && g.getMot(i+2).getPOS().equals("VB") && pos!=i+2){ 
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==i+2) gsrl.removeDep(dd);
                                        }
                                        gsrl.ajoutDep("A0", pos, i+2);
                                    }
				}
                            }
                            else{
                                //see if it's vc-chain... (RB|Mods)*VB
                                if (ismod(g.getMot(i))) {
                                    //look if there is a verb following the chain of modifiers or rb
                                    boolean foundVB=false;
                                    int posvb=0;
                                    for (int nw=i+1;nw<g.getNbMots();nw++){
                                        if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                            foundVB=true; posvb=nw;
                                            break;
                                        }
                                        if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                break;
                                            //                                                //look for been able to: been is in mod, 
//                                        {if (g.getNbMots()<=nw+1) break;
//                                        else    if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                    break;
//                                        }
                                    }
                                    //when we find the end of the vcchain add the semantic link
                                    if (foundVB && pos!=posvb){
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                        }
                                        gsrl.ajoutDep("A0", pos, posvb);
                                        //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                        if (posvb+2<g.getNbMots()){
                                            if (g.getMot(posvb+1).getForme().equals("to") && g.getMot(posvb+2).getPOS().equals("VB") && pos!=posvb+2){ 
                                                for (int dd: depsWithPos){
                                                    if (gsrl.getHead(dd)==posvb+2) gsrl.removeDep(dd);
                                                }
                                                gsrl.ajoutDep("A0", pos, posvb+2);
                                            }
                                        }
                                    }
                                }
                            }
			    int[] res={pos};
			    return res;
			}
		    }
		    return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

        
        
	// =================================================================
	class rOPRDA1 implements SingleRule{
                private String[] a2oprd={"force","promise","estimate","issue","stand","shape","leave","come","schedule","tend","fail","oblige","compel",
                "forces","promises","estimates","issues","stands","shapes","leaves","comes","schedules","tends","fails","obliges","compels",
                "forced","promised","estimated","issued","standed","shaped","left","came","scheduled","tended","failed","obliged","compeled",
                "forcen","forcing","promising","estimating","issuing","standing","shaping","leaving","comming","scheduling","tending","failing","compeling",};
		public String getName() {return "OPRDA1";}
		public int[] getApplicable(DetGraph g){
                    Set<String> a2=new HashSet<String>();
                    for (String a:a2oprd) a2.add(a);
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
//                            if (g.getMot(pos).getPOS().startsWith("VB")&&(g.getMot(pos+1).getPOS().startsWith("TO")||g.getMot(pos+1).getPOS().startsWith("VBG"))) pp.add(pos);
                            if ((pos+1)<g.getNbMots()){
                                if  ((pos+2)<g.getNbMots()){
                                    if (g.getMot(pos).getPOS().startsWith("VB")&& (g.getMot(pos+1).getPOS().startsWith("TO")&&g.getMot(pos+2).getPOS().startsWith("VB") && !a2.contains(g.getMot(pos).getForme()) ||
                                        (g.getMot(pos+1).getPOS().startsWith("VBG")&& !a2.contains(g.getMot(pos).getForme()))))
                                            pp.add(pos+1);
                                }else{
                                    if (g.getMot(pos).getPOS().startsWith("VB")&& (g.getMot(pos+1).getPOS().startsWith("VBG")&& !a2.contains(g.getMot(pos).getForme())))
                                            pp.add(pos+1);
                                    
                                }
                            }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
                    g.ajoutDep("OPRD", pos, pos-1);
                    int[] depsWithPos=gsrl.getDeps(pos);
//                    int ds = gsrl.getDep(pos);
//                    if (ds>=0) gsrl.removeDep(ds);
                    if (SemanticProcesing.isPredicate(g, pos-1)&& pos!=pos-1) {
                        for (int dd: depsWithPos){
                            if (gsrl.getHead(dd)==pos-1) gsrl.removeDep(dd);
                        }
                        gsrl.ajoutDep("A1", pos, pos-1);
                    }
                    else{
                        //see if it's vc-chain... (RB|Mods)*VB
                        if (ismod(g.getMot(pos-1))) {
                            //look if there is a verb following the chain of modifiers or rb
                            boolean foundVB=false;
                            int posvb=0;
                            for (int nw=pos;nw<g.getNbMots()-1;nw++){
                                if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                    foundVB=true; posvb=nw;
                                    break;
                                }
                                if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                    break;
                                    //look for been able to: been is in mod, 
//                                {
//                                 if (g.getNbMots()<=nw+1) break;
//                                 else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                        break;
//                                }
                            }
                            //when we find the end of the vcchain add the semantic link
                            if (foundVB && (pos)!=posvb) {
                                for (int dd: depsWithPos){
                                    if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                }
                                gsrl.ajoutDep("A1", pos, posvb);
                            }
                        }
                    }
                    int[] res={pos};
                    return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
	// =================================================================
	class rOPRDA2 implements SingleRule{
                private String[] a2oprd={"force","promise","estimate","issue","stand","shape","leave","come","schedule","tend","fail","oblige","compel",
                "forces","promises","estimates","issues","stands","shapes","leaves","comes","schedules","tends","fails","obliges","compels",
                "forced","promised","estimated","issued","standed","shaped","left","came","scheduled","tended","failed","obliged","compeled",
                "forcen","forcing","promising","estimating","issuing","standing","shaping","leaving","comming","scheduling","tending","failing","compeling",};
		public String getName() {return "OPRDA2";}
		public int[] getApplicable(DetGraph g){
                    Set<String> a2=new HashSet<String>();
                    for (String a:a2oprd) a2.add(a);
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
//                            if (g.getMot(pos).getPOS().startsWith("VB")&&(g.getMot(pos+1).getPOS().startsWith("TO")||g.getMot(pos+1).getPOS().startsWith("VBG"))) pp.add(pos);
                            if ((pos+1)<g.getNbMots()){
                                if  ((pos+2)<g.getNbMots()){
                                    if (g.getMot(pos).getPOS().startsWith("VB") && (g.getMot(pos+1).getPOS().startsWith("TO") &&
                                         g.getMot(pos+2).getPOS().startsWith("VB") && a2.contains(g.getMot(pos).getForme()) ||
                                        (g.getMot(pos+1).getPOS().startsWith("VBG")&& a2.contains(g.getMot(pos).getForme()))))
                                            pp.add(pos+1);
                                }else{
                                    if (g.getMot(pos).getPOS().startsWith("VB")&& (g.getMot(pos+1).getPOS().startsWith("VBG")&& a2.contains(g.getMot(pos).getForme())))
                                            pp.add(pos+1);
                                    
                                }
                            }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
                    if (g.getMot(pos+1).getPOS().startsWith("TO")&&g.getMot(pos+2).getPOS().startsWith("DT")) g.ajoutDep("ADV", pos, pos-1);
                    else g.ajoutDep("OPRD", pos, pos-1);
                    int[] depsWithPos=gsrl.getDeps(pos);
//                    int ds = gsrl.getDep(pos);
//                    if (ds>=0) gsrl.removeDep(ds);
                    if (SemanticProcesing.isPredicate(g, pos-1) && pos!=(pos-1)){
                        for (int dd: depsWithPos){
                            if (gsrl.getHead(dd)==pos-1) gsrl.removeDep(dd);
                        }
                        gsrl.ajoutDep("A2", pos, pos-1);
                    }
                    else{
                        //see if it's vc-chain... (RB|Mods)*VB
                        if (ismod(g.getMot(pos-1))) {
                            //look if there is a verb following the chain of modifiers or rb
                            boolean foundVB=false;
                            int posvb=0;
                            for (int nw=pos;nw<g.getNbMots()-1;nw++){
                                if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                    foundVB=true; posvb=nw;
                                    break;
                                }
                                if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                    break;
                                    //look for been able to: been is in mod, 
//                                {
//                                    if (g.getNbMots()<=nw+1) break;
//                                    else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                        break;
//                                }
                            }
                            //when we find the end of the vcchain add the semantic link
                            if (foundVB && (pos)!=posvb) {
                                for (int dd: depsWithPos){
                                    if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                }
                                gsrl.ajoutDep("A2", pos, posvb);
                            }
                        }
                    }
                    int[] res={pos};
                    return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			rboolean ismod(Mot mmot)eturn false;
//		}
	}


	// =================================================================
	class rDIRA4 implements SingleRule{
		public String getName() {return "DIRA4";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
                            if ((pos+2)<g.getNbMots()){
                                if (g.getMot(pos).getPOS().startsWith("VB")&& g.getMot(pos+1).getPOS().startsWith("TO")&& (g.getMot(pos+2).getPOS().startsWith("CD")||g.getMot(pos+2).getPOS().startsWith("$") ))
                                         pp.add(pos);
                                
                            }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
                    g.ajoutDep("DIR", pos+1, pos);
                    int ds = gsrl.getDep(pos+1);
                    if (ds>=0) gsrl.removeDep(ds);
                    if (SemanticProcesing.isPredicate(g, pos) && pos!=(pos+1)) {
                        gsrl.ajoutDep("A4", pos+1, pos);
                    }
                    else{
                        //see if it's vc-chain... (RB|Mods)*VB
                        if (ismod(g.getMot(pos))) {
                            //look if there is a verb following the chain of modifiers or rb
                            boolean foundVB=false;
                            int posvb=0;
                            for (int nw=pos+1;nw<g.getNbMots()-1;nw++){
                                if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                    foundVB=true; posvb=nw;
                                    break;
                                }
                                if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                    break;
                                    //look for been able to: been is in mod, 
//                                {
//                                    if (g.getNbMots()<=nw+1) break;
//                                    else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                        break;
//                                }
                            }
                            //when we find the end of the vcchain add the semantic link
                            if (foundVB && (pos+1)!=posvb) gsrl.ajoutDep("A4", pos+1, posvb);
                        }
                    }
                    int[] res={pos};
                    return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
        
        // =================================================================
	class rTMPAM implements SingleRule{
		final String[] tps = {"today","tomorrow","yesterday"};
		public String getName() {return "TMPAM";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots();pos++) {
				boolean maybeTMP=false;
				if (Arrays.binarySearch(tps, g.getMot(pos).getForme())>=0) maybeTMP=true;
				if (!maybeTMP) {
					String po = g.getMot(pos).getPOS();
					if (po.startsWith("RB")||(po.startsWith("IN")&&!g.getMot(pos).getForme().toLowerCase().equals("because"))) maybeTMP=true;
				}
				if (maybeTMP) {
					for (int i=pos-1;i>=0;i--) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							pp.add(pos); break;
						}
					}
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							pp.add(-pos); break;
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
                    if (pos>0) {
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
                                int[]depsWithPos=gsrl.getDeps(pos);
//				int ds=gsrl.getDep(pos);
//				if (ds>=0) gsrl.removeDep(ds);
				for (int i=pos-1;i>=0;i--) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						g.ajoutDep("TMP", pos, i);
                                                if (SemanticProcesing.isPredicate(g, i) && pos!=i) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("AM", pos, i);
                                                }
                                                else{
                                                    //see if it's vc-chain... (RB|Mods)*VB
                                                    if (ismod(g.getMot(i))) {
                                                        //look if there is a verb following the chain of modifiers or rb
                                                        boolean foundVB=false;
                                                        int posvb=0;
                                                        for (int nw=i+1;nw<g.getNbMots();nw++){
                                                            if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                                foundVB=true; posvb=nw;
                                                                break;
                                                            }
                                                            if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                                break;
                                                                //look for been able to: been is in mod, 
//                                                            {
//                                                                if (g.getNbMots()<=nw+1) break;
//                                                                else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                                    break;
//                                                            }
                                                        }
                                                        //when we find the end of the vcchain add the semantic link
                                                        if (foundVB && pos!=posvb){
                                                            for (int dd: depsWithPos){
                                                                if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                            }
                                                            gsrl.ajoutDep("AM", pos, posvb);
                                                        }
                                                    }
                                                }
						int[] res={pos};
						return res;
					}
				}
			} else {
				pos=-pos;
				int d=g.getDep(pos);
				if (d>=0) g.removeDep(d);
                                int[]depsWithPos=gsrl.getDeps(pos);
//				int ds=gsrl.getDep(pos);
//				if (ds>=0) gsrl.removeDep(ds);
				for (int i=pos+1;i<g.getNbMots();i++) {
					if (g.getMot(i).getPOS().startsWith("VB")) {
						g.ajoutDep("TMP", pos, i);
                                                if (SemanticProcesing.isPredicate(g, i) && pos!=i){ 
                                                        for (int dd: depsWithPos){
                                                            if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                                        }
        						gsrl.ajoutDep("AM", pos, i);
                                                }
                                                else{
                                                    //see if it's vc-chain... (RB|Mods)*VB
                                                    if (ismod(g.getMot(i))) {
                                                        //look if there is a verb following the chain of modifiers or rb
                                                        boolean foundVB=false;
                                                        int posvb=0;
                                                        for (int nw=i+1;nw<g.getNbMots();nw++){
                                                            if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                                foundVB=true; posvb=nw;
                                                                break;
                                                            }
                                                            if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                                break;
                                                                //look for been able to: been is in mod, 
//                                                            {
//                                                                if (g.getNbMots()<=nw+1) break;
//                                                                else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                                    break;
//                                                            }
                                                        }
                                                        //when we find the end of the vcchain add the semantic link
                                                        if (foundVB && pos!=posvb){
                                                            for (int dd: depsWithPos){
                                                                if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                            }
                                                            gsrl.ajoutDep("AM", pos, posvb);
                                                        }
                                                    }
                                                }
						int[] res={pos};
						return res;
					}
				}
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
        
        
	// =================================================================
	/**
	 * R4: Link any NN word wt to wt+k with SBJ, if wt+k is a verb
	 */
	class rSUJRIGHTA0 implements SingleRule {
		public String getName() {return "SUBJECTA0";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				String postag = g.getMot(pos).getPOS();
				int[] grss=g.getGroups(pos);
				int lastNPword=-1;
				if (grss!=null && grss.length>0)  lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
				if (pos==lastNPword &&(postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))) {
					for (int i=pos+1;i<g.getNbMots();i++) {
						if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) /*&& !g.getMot(i).getPOS().startsWith("VBG")*/)) {
							pp.add(pos);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			String postag = g.getMot(pos).getPOS();
                        DetGraph gsrl=g.relatedGraphs.get(0);
			int[] grss=g.getGroups(pos);
                        
//                        gsrl.removeAllDeps(pos);
			int lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
			if (lastNPword==pos && (postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))){
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
                                int[] depsWithPos=gsrl.getDeps(pos);
//				int ds = gsrl.getDep(pos);
//				if (ds>=0) gsrl.removeDep(ds);
				for (int i=pos+1;i<g.getNbMots();i++) {
					if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) /*&& !g.getMot(i).getPOS().startsWith("VBG")*/)) {
						g.ajoutDep("SBJ", pos, i);
                                                if (SemanticProcesing.isPredicate(g, i) && pos!=i) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A0", pos, i);
                                                    //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                                    if (i+2<g.getNbMots()){
                                                        if (g.getMot(i+1).getForme().equals("to") && g.getMot(i+2).getPOS().equals("VB") && pos!=i+2){ 
                                                            for (int dd: depsWithPos){
                                                                if (gsrl.getHead(dd)==i+2) gsrl.removeDep(dd);
                                                            }
                                                            gsrl.ajoutDep("A0", pos, i+2);
                                                        }
                                                    }
                                                }
                                                else{
                                                    //see if it's vc-chain... (RB|Mods)*VB
                                                    if (ismod(g.getMot(i))) {
                                                        //look if there is a verb following the chain of modifiers or rb
                                                        boolean foundVB=false;
                                                        int posvb=0;
                                                        for (int nw=i+1;nw<g.getNbMots();nw++){
                                                            if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                                foundVB=true; posvb=nw;
                                                                break;
                                                            }
                                                            if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                                break;
                                                                //look for been able to: been is in mod, 
//                                                            {
//                                                                if (g.getNbMots()<=nw+1) break;
//                                                                else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                                    break;
//                                                            }
                                                        }
                                                        //when we find the end of the vcchain add the semantic link
                                                        if (foundVB && pos!=posvb) {
                                                            for (int dd: depsWithPos){
                                                                if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                            }
                                                            gsrl.ajoutDep("A0", pos, posvb);
                                                            //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                                            if (posvb+2<g.getNbMots()){
                                                                if (g.getMot(posvb+1).getForme().equals("to") && g.getMot(posvb+2).getPOS().equals("VB") && pos!=posvb+2){ 
                                                                    for (int dd: depsWithPos){
                                                                        if (gsrl.getHead(dd)==posvb+2) gsrl.removeDep(dd);
                                                                    }
                                                                    gsrl.ajoutDep("A0", pos, posvb+2);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
						int[] res={pos};
						return res;
					}
				}
			}
			System.out.println("Error with rule SBJ-A0 "+pos+" "+g);
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

	class rSUJRIGHTA1 implements SingleRule {
		public String getName() {return "SUBJECTA1";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				String postag = g.getMot(pos).getPOS();
				int[] grss=g.getGroups(pos);
				int lastNPword=-1;
				if (grss!=null && grss.length>0)  lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
				if (pos==lastNPword &&(postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))) {
					for (int i=pos+1;i<g.getNbMots();i++) {
                                            if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) /*&& !g.getMot(i).getPOS().startsWith("VBG")*/)) {
							pp.add(pos);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			String postag = g.getMot(pos).getPOS();
                        DetGraph gsrl=g.relatedGraphs.get(0);
			int[] grss=g.getGroups(pos);
			int lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
			if (lastNPword==pos && (postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))){
				int d = g.getDep(pos);
				if (d>=0) g.removeDep(d);
                                int[]depsWithPos=gsrl.getDeps(pos);
//				int ds = gsrl.getDep(pos);
//				if (ds>=0) gsrl.removeDep(ds);
				for (int i=pos+1;i<g.getNbMots();i++) {
					if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) /*&& !g.getMot(i).getPOS().startsWith("VBG")*/)) {
						g.ajoutDep("SBJ", pos, i);
                                                if (SemanticProcesing.isPredicate(g, i) && pos!=i) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A1", pos, i);
                                                    //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                                    if (i+2<g.getNbMots()){
                                                        if (g.getMot(i+1).getForme().equals("to") && g.getMot(i+2).getPOS().equals("VB") && pos!=i+2){ 
                                                                    for (int dd: depsWithPos){
                                                                        if (gsrl.getHead(dd)==i+2) gsrl.removeDep(dd);
                                                                    }
                                                                    if (SemanticProcesing.isPredicate(g, i+2) && pos!=i+2) gsrl.ajoutDep("A1", pos, i+2);
                                                        }
                                                    }
                                                }
                                                else{
                                                    //see if it's vc-chain... (RB|Mods)*VB
                                                    if (ismod(g.getMot(i))) {
                                                        //look if there is a verb following the chain of modifiers or rb
                                                        boolean foundVB=false;
                                                        int posvb=0;
                                                        for (int nw=i+1;nw<g.getNbMots();nw++){
                                                            if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                                foundVB=true; posvb=nw;
                                                                break;
                                                            }
                                                            if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                                break;
                                                                //look for been able to: been is in mod, 
//                                                            {if (g.getNbMots()<=nw+1) break;
//                                                              else  if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                                    break;
//                                                                }
                                                        }
                                                        //when we find the end of the vcchain add the semantic link
                                                        if (foundVB && pos!=posvb) {
                                                            for (int dd: depsWithPos){
                                                                if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                            }
                                                            gsrl.ajoutDep("A1", pos, posvb);
                                                        }
                                                    }
                                                }
						int[] res={pos};
						return res;
					}
				}
			}
			System.out.println("Error with rule SBJ-A1 "+pos+" "+g);
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
//	class rRIGHTA0 implements SingleRule {
//		public String getName() {return "RA0";}
//		// cree les GP avec des verbes: "il vient de manger"
//		// cree seulement le lien INTERNE au GP
//
//		public int[] getApplicable(DetGraph g) {
//			ArrayList<Integer> pp = new ArrayList<Integer>();
//
//			for (int pos=0;pos<g.getNbMots()-1;pos++) {
//				String postag = g.getMot(pos).getPOS();
//				int[] grss=g.getGroups(pos);
//				int lastNPword=-1;
//				if (grss!=null && grss.length>0)  lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
//				if (pos==lastNPword &&(postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))) {
//					for (int i=pos+1;i<g.getNbMots();i++) {
//						if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) && g.getMot(i).getPOS().startsWith("VBG"))) {
//							pp.add(pos);
//						}
//					}
//				}
//			}
//			int[] res = new int[pp.size()];
//			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
//			return res;
//		}
//
//		public int[] applyRule(DetGraph g, int pos) {
//			String postag = g.getMot(pos).getPOS();
//                        DetGraph gsrl=g.relatedGraphs.get(0);
//			int[] grss=g.getGroups(pos);
//			int lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
//			if (lastNPword==pos && (postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))){
////				int d = g.getDep(pos);
////				if (d>=0) g.removeDep(d);
//				int ds = gsrl.getDep(pos);
//				if (ds>=0) gsrl.removeDep(ds);
//				for (int i=pos+1;i<g.getNbMots();i++) {
//					if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) && g.getMot(i).getPOS().startsWith("VBG"))) {
//                                                gsrl.ajoutDep("A0", pos, i);
//						int[] res={pos};
//						return res;
//					}
//				}
//			}
//			System.out.println("Error with rule right A0 "+pos+" "+g);
//			return null;
//		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
//	}
//
//	class rRIGHTA1 implements SingleRule {
//		public String getName() {return "A1";}
//		// cree les GP avec des verbes: "il vient de manger"
//		// cree seulement le lien INTERNE au GP
//
//		public int[] getApplicable(DetGraph g) {
//			ArrayList<Integer> pp = new ArrayList<Integer>();
//
//			for (int pos=0;pos<g.getNbMots()-1;pos++) {
//				String postag = g.getMot(pos).getPOS();
//				int[] grss=g.getGroups(pos);
//				int lastNPword=-1;
//				if (grss!=null && grss.length>0)  lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
//				if (pos==lastNPword &&(postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))) {
//					for (int i=pos+1;i<g.getNbMots();i++) {
//                                            if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) && g.getMot(i).getPOS().startsWith("VBG"))) {
//							pp.add(pos);
//						}
//					}
//				}
//			}
//			int[] res = new int[pp.size()];
//			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
//			return res;
//		}
//
//		public int[] applyRule(DetGraph g, int pos) {
//			String postag = g.getMot(pos).getPOS();
//                        DetGraph gsrl=g.relatedGraphs.get(0);
//			int[] grss=g.getGroups(pos);
//			int lastNPword=Integer.parseInt(g.getGroupName(grss[0]));
//			if (lastNPword==pos && (postag.startsWith("NN")||postag.startsWith("VBG")||postag.startsWith("PRP")||postag.startsWith("WP")||postag.startsWith("CD"))){
//				int ds = gsrl.getDep(pos);
//				if (ds>=0) gsrl.removeDep(ds);
//				for (int i=pos+1;i<g.getNbMots();i++) {
//					if ((g.getMot(i).getPOS().startsWith("VB") && !ismod(g.getMot(i)) && g.getMot(i).getPOS().startsWith("VBG"))) {
//                                                gsrl.ajoutDep("A1", pos, i);
//						int[] res={pos};
//						return res;
//					}
//				}
//			}
//			System.out.println("Error with rule right A1 "+pos+" "+g);
//			return null;
//		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
//	}
	// =================================================================
	class rAintAM implements SingleRule{
		public String getName() {return "AintAM";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots();pos++) {
				if (pos>0&&(g.getMot(pos).getForme().equals("n't")||g.getMot(pos).getForme().equals("not"))) {
					pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                    DetGraph gsrl= g.relatedGraphs.get(0);
                    int posV;
                    boolean found=false;
                    for (posV=pos;posV<g.getNbMots()-1;posV++)
                        if (g.getMot(posV).getPOS().startsWith("VB")) {found=true;break;}
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ADV", pos, pos-1);
                        if (found) {
                            int[] depsWithPos=gsrl.getDeps(pos);
//                            int ds=gsrl.getDep(pos);
//                            if (ds>=0)gsrl.removeDep(ds);
                            if ((SemanticProcesing.isPredicate(g, posV)||g.getMot(posV).getForme().toLowerCase().equals("going")) && pos!=posV) {
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==posV) gsrl.removeDep(dd);
                                    }
                                    gsrl.ajoutDep("AM", pos, posV);
                            }
                            else{
                                //see if it's vc-chain... (RB|Mods)*VB
                                if (ismod(g.getMot(posV))) {
                                    //look if there is a verb following the chain of modifiers or rb
                                    boolean foundVB=false;
                                    int posvb=0;
                                    for (int nw=posV+1;nw<g.getNbMots()-1;nw++){
                                        if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                            foundVB=true; posvb=nw;
                                            break;
                                        }
                                        if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                            break;
                                            //look for been able to: been is in mod, 
//                                            if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                break;
                                    }
                                    //when we find the end of the vcchain add the semantic link
                                    if (foundVB && pos!=posvb) {
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                        }
                                        gsrl.ajoutDep("AM", pos, posvb);
                                    }
                                }
                            }
                            
                        }
			int[] res={pos};
			return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

        
        
//TODO: 
        
        //ADD A RULE FOR ALL THE MODIFIERS AM-TMP, AM-DIR, ETC (ACCORDING TO WHAT TITOV SAID THAT ARE FIXED LINKS
        
        
	// =================================================================
	/**
	 *  4)(Rule, it belongs to the part of initialization in Naseem 2010)
	 *   a- the last word in an NP is headed by the word immediately before the NP if it is a preposition,
	 *   //b- otherwise (the word immediately before an NP is not preposition), the last word in an NP is headed by the headword of the sentence if the NP is before the headword
	 *   //c- otherwise, the las word in an NP is headed by the word preceding the NP
	 */

	class r4NaseemInitSRL implements SingleRule {
		public String getName() {return "initPMODSRL";}

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				boolean treated = false;
				int precedingWord = g.groups.get(i).get(0).getIndexInUtt()-2;
				if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN")) {
					pp.add(i);
					treated=true;
				}
				if (treated) continue;
				
				int lastWordInNP = g.groups.get(i).get(g.groups.get(i).size()-1).getIndexInUtt()-1;
				int firstverb=-1;
				for (int j=0;j<g.getNbMots();j++) {
					if (g.getMot(j).getPOS().startsWith("VB")||g.getMot(j).getPOS().startsWith("MD")) {
						firstverb=j; break;
					}
				}
				if (firstverb>lastWordInNP) {
					pp.add(i);
					treated=true;
				}
				if (treated) continue;

				if (precedingWord>=0) pp.add(i);
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			int lastWordInNP = g.groups.get(pos).get(g.groups.get(pos).size()-1).getIndexInUtt()-1;
			int d = g.getDep(lastWordInNP);
			if (d>=0) g.removeDep(d);
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(lastWordInNP);
//                        int ds = gsrl.getDep(lastWordInNP);
//			if (ds>=0) gsrl.removeDep(lastWordInNP);
//                        gsrl.removeAllDeps(pos);
			boolean treated = false;
			int precedingWord = g.groups.get(pos).get(0).getIndexInUtt()-2;
			if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN")) {
				g.ajoutDep("PMOD",lastWordInNP,precedingWord);
				treated=true;
			}
			int[] res = {lastWordInNP};
			if (treated) return res;
			
			int firstverb=-1;
			for (int j=0;j<g.getNbMots();j++) {
				if (g.getMot(j).getPOS().startsWith("VB")||g.getMot(j).getPOS().startsWith("MD")) {
					firstverb=j; break;
				}
			}
			if (firstverb>lastWordInNP) {
				g.ajoutDep("SBJ",lastWordInNP,firstverb);
                                //if (!g.getMot(firstverb).getPOS().startsWith("MD")) gsrl.ajoutDep("A0", lastWordInNP,firstverb);
                                if (SemanticProcesing.isPredicate(g, firstverb) && lastWordInNP!=firstverb){
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==firstverb) gsrl.removeDep(dd);
                                    }
                                    gsrl.ajoutDep("A0", lastWordInNP,firstverb);
                                    //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                    if (firstverb+2<g.getNbMots()){
                                        if (g.getMot(firstverb+1).getForme().equals("to") && g.getMot(firstverb+2).getPOS().equals("VB") && lastWordInNP!=firstverb+2){ 
                                                for (int dd: depsWithPos){
                                                    if (gsrl.getHead(dd)==firstverb+2) gsrl.removeDep(dd);
                                                }
                                                gsrl.ajoutDep("A0", lastWordInNP, firstverb+2);
                                        }
                                    }
                                    
                                }
                                else{
                                    //see if it's vc-chain... (RB|Mods)*VB
                                    if (ismod(g.getMot(firstverb))) {
                                        //look if there is a verb following the chain of modifiers or rb
                                        boolean foundVB=false;
                                        int posvb=0;
                                        for (int nw=firstverb+1;nw<g.getNbMots()-1;nw++){
                                            if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                foundVB=true; posvb=nw;
                                                break;
                                            }
                                            if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                break;
                                                    //look for been able to: been is in mod, 
//                                            {
//                                                if (g.getNbMots()<=nw+1) break;
//                                                else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                        break;
//                                            }
                                        }
                                        //when we find the end of the vcchain add the semantic link
                                        if (foundVB && lastWordInNP!=posvb) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A0", lastWordInNP,posvb);
                                            //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                            if (posvb+2<g.getNbMots()){
                                                if (g.getMot(posvb+1).getForme().equals("to") && g.getMot(posvb+2).getPOS().equals("VB") && posvb+2!=lastWordInNP){ 
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb+2) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A0", lastWordInNP, posvb+2);
                                                }
                                            }
                                        }
                                    }
                                }
				treated=true;
			}
			if (treated) return res;
			
			if (precedingWord>=0) {
				if (g.getMot(precedingWord).getPOS().startsWith("VB")||g.getMot(precedingWord).getPOS().startsWith("MD"))
                                    {
                                        g.ajoutDep("OBJ",lastWordInNP,precedingWord);
                                        //if (!g.getMot(precedingWord).getPOS().startsWith("MD")) gsrl.ajoutDep("A1",lastWordInNP,precedingWord );
                                        if (SemanticProcesing.isPredicate(g, precedingWord) && lastWordInNP!=precedingWord ){
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==precedingWord) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A1", lastWordInNP,precedingWord);
                                        }
                                        else{
                                            //see if it's vc-chain... (RB|Mods)*VB
                                            if (ismod(g.getMot(precedingWord))) {
                                                //look if there is a verb following the chain of modifiers or rb
                                                boolean foundVB=false;
                                                int posvb=0;
                                                for (int nw=precedingWord+1;nw<g.getNbMots()-1;nw++){
                                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                        foundVB=true; posvb=nw;
                                                        break;
                                                    }
                                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                        break;
                                                        //look for been able to: been is in mod, 
//                                                    {if (g.getNbMots()<=nw+1) break;
//                                                    else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                                    }
                                                }
                                                //when we find the end of the vcchain add the semantic link
                                                if (foundVB && lastWordInNP!=posvb) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A1", lastWordInNP,posvb);
                                                    //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                                    if (posvb+2<g.getNbMots()){
                                                        if (g.getMot(posvb+1).getForme().equals("to") && g.getMot(posvb+2).getPOS().equals("VB") && posvb+2!=lastWordInNP){ 
                                                                    for (int dd: depsWithPos){
                                                                        if (gsrl.getHead(dd)==posvb+2) gsrl.removeDep(dd);
                                                                    }
                                                                    if (SemanticProcesing.isPredicate(g,posvb+2)) gsrl.ajoutDep("A1", lastWordInNP, posvb+2);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                    }
				else g.ajoutDep("DEP",lastWordInNP,precedingWord);
				return res;
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
	// =================================================================
	/**
	 *  4)(Rule, it belongs to the part of initialization in Naseem 2010)
	 *   a- the last word in an NP is headed by the word immediately before the NP if it is a preposition,
	 *   //b- otherwise (the word immediately before an NP is not preposition), the last word in an NP is headed by the headword of the sentence if the NP is before the headword
	 *   //c- otherwise, the las word in an NP is headed by the word preceding the NP
	 */

	class r4NaseemInitSRLBis implements SingleRule {
		public String getName() {return "initPMODSRLbis";}

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbGroups();i++) {
				boolean treated = false;
				int precedingWord = g.groups.get(i).get(0).getIndexInUtt()-2;
				if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN")) {
					pp.add(i);
					treated=true;
				}
				if (treated) continue;
				
				int lastWordInNP = g.groups.get(i).get(g.groups.get(i).size()-1).getIndexInUtt()-1;
				int firstverb=-1;
				for (int j=0;j<g.getNbMots();j++) {
					if (g.getMot(j).getPOS().startsWith("VB")||g.getMot(j).getPOS().startsWith("MD")) {
						firstverb=j; break;
					}
				}
				if (firstverb>lastWordInNP) {
					pp.add(i);
					treated=true;
				}
				if (treated) continue;

				if (precedingWord>=0) pp.add(i);
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			int lastWordInNP = g.groups.get(pos).get(g.groups.get(pos).size()-1).getIndexInUtt()-1;
			int d = g.getDep(lastWordInNP);
			if (d>=0) g.removeDep(d);
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(lastWordInNP);
//                        int ds = gsrl.getDep(lastWordInNP);
//			if (ds>=0) gsrl.removeDep(ds);
			boolean treated = false;
			int precedingWord = g.groups.get(pos).get(0).getIndexInUtt()-2;
			if (precedingWord>=0 && g.getMot(precedingWord).getPOS().startsWith("IN")) {
				g.ajoutDep("PMOD",lastWordInNP,precedingWord);
                                
				treated=true;
			}
			int[] res = {lastWordInNP};
			if (treated) return res;
			
			int firstverb=-1;
			for (int j=0;j<g.getNbMots();j++) {
				if (g.getMot(j).getPOS().startsWith("VB")||g.getMot(j).getPOS().startsWith("MD")) {
					firstverb=j; break;
				}
			}
			if (firstverb>lastWordInNP) {
				g.ajoutDep("SBJ",lastWordInNP,firstverb);
//                                if (!g.getMot(firstverb).getPOS().startsWith("MD")) gsrl.ajoutDep("A1", lastWordInNP,firstverb);
                                if (SemanticProcesing.isPredicate(g, firstverb) && lastWordInNP!=firstverb) {
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==firstverb) gsrl.removeDep(dd);
                                    }
                                    gsrl.ajoutDep("A1", lastWordInNP,firstverb);
                                }
                                else{
                                    //see if it's vc-chain... (RB|Mods)*VB
                                    if (ismod(g.getMot(firstverb))) {
                                        //look if there is a verb following the chain of modifiers or rb
                                        boolean foundVB=false;
                                        int posvb=0;
                                        for (int nw=firstverb+1;nw<g.getNbMots()-1;nw++){
                                            if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                foundVB=true; posvb=nw;
                                                break;
                                            }
                                            if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                break;
                                                        //look for been able to: been is in mod, 
//                                            {
//                                                if (g.getNbMots()<=nw+1) break;
//                                                else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                            }
                                        }
                                        //when we find the end of the vcchain add the semantic link
                                        if (foundVB && lastWordInNP!=posvb) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A1", lastWordInNP,posvb);
                                        }
                                    }
                                }
				treated=true;
			}
			if (treated) return res;
			
			if (precedingWord>=0) {
				if (g.getMot(precedingWord).getPOS().startsWith("VB")||g.getMot(precedingWord).getPOS().startsWith("MD"))
                                    {
                                        g.ajoutDep("OBJ",lastWordInNP,precedingWord);
//                                        if (!g.getMot(precedingWord).getPOS().startsWith("MD")) gsrl.ajoutDep("A0",lastWordInNP,precedingWord );
                                        if (SemanticProcesing.isPredicate(g, precedingWord) && lastWordInNP!=precedingWord) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==precedingWord) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A0", lastWordInNP,precedingWord);
                                            //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                            if (precedingWord+2<g.getNbMots()){
                                                if (g.getMot(precedingWord+1).getForme().equals("to") && g.getMot(precedingWord+2).getPOS().equals("VB") && precedingWord+2!=lastWordInNP){ 
                                                            for (int dd: depsWithPos){
                                                                if (gsrl.getHead(dd)==precedingWord+2) gsrl.removeDep(dd);
                                                            }
                                                            if (SemanticProcesing.isPredicate(g,precedingWord+2)) gsrl.ajoutDep("A0", lastWordInNP, precedingWord+2);
                                                }
                                            }
                                            
                                        }
                                        else{
                                            //see if it's vc-chain... (RB|Mods)*VB
                                            if (ismod(g.getMot(precedingWord))) {
                                                //look if there is a verb following the chain of modifiers or rb
                                                boolean foundVB=false;
                                                int posvb=0;
                                                for (int nw=precedingWord+1;nw<g.getNbMots()-1;nw++){
                                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                        foundVB=true; posvb=nw;
                                                        break;
                                                    }
                                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                        break;
                                                        //look for been able to: been is in mod, 
//                                                    {
//                                                        if (g.getNbMots()<=nw+1) break;
//                                                        else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                                    }
                                                }
                                                //when we find the end of the vcchain add the semantic link
                                                if (foundVB && lastWordInNP!=posvb){
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A0", lastWordInNP,posvb);
                                                    //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                                    if (posvb+2<g.getNbMots()){
                                                        if (g.getMot(posvb+1).getForme().equals("to") && g.getMot(posvb+2).getPOS().equals("VB") && lastWordInNP!=posvb+2){ 
                                                                for (int dd: depsWithPos){
                                                                    if (gsrl.getHead(dd)==posvb+2) gsrl.removeDep(dd);
                                                                }
                                                                if (SemanticProcesing.isPredicate(g,posvb+2)) gsrl.ajoutDep("A0", lastWordInNP, posvb+2);
                                                        }
                                                    }
                                                    
                                                }
                                            }
                                        }
                                        
                                    }
				else g.ajoutDep("DEP",lastWordInNP,precedingWord);
				return res;
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

        
        // =================================================================
	class rPRDA2 implements SingleRule{
		public String getName() {return "PRD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			int etreavant = -1;
			for (int pos=0;pos<g.getNbMots();pos++) {
                                String lemFeel=g.getMot(pos).getForme().toLowerCase();
                                if (lemFeel.equals("feel")||lemFeel.equals("feels")||lemFeel.equals("felt")||lemFeel.equals("feeling")) {                            
					etreavant=pos; continue;
				}
				if (etreavant>=0&&(g.getMot(pos).getPOS().startsWith("JJ")||g.getMot(pos).getPOS().startsWith("RB"))) {
					pp.add(pos);
				} else if (etreavant>=0&&g.getMot(pos).getForme().equals("to")) {
					pp.add(pos);
				} else if (etreavant>=0) {
					int[] gr=g.getGroupsThatEndHere(pos);
					if (gr!=null&&gr.length>0) pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(pos);
//                        int ds = gsrl.getDep(pos);
//			if (ds>=0) gsrl.removeDep(ds);
			for (int i=pos-1;i>=0;i--) {
                                String lem=SemanticProcesing.getLemmafromForm(g.getMot(i).getForme());
                                String lemFeel=g.getMot(i).getForme().toLowerCase();
				if (lem.equals("be")||lemFeel.equals("feel")||lemFeel.equals("feels")||lemFeel.equals("felt")||lemFeel.equals("feeling")) {
                                        if (SemanticProcesing.isPredicate(g, i) && pos!=i) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A2", pos, i);
                                        }
                                        else{
                                            //see if it's vc-chain... (RB|Mods)*VB
                                            if (ismod(g.getMot(i))) {
                                                //look if there is a verb following the chain of modifiers or rb
                                                boolean foundVB=false;
                                                int posvb=0;
                                                for (int nw=i+1;nw<g.getNbMots();nw++){
                                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                        foundVB=true; posvb=nw;
                                                        break;
                                                    }
                                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                        break;
                                                        //look for been able to: been is in mod, 
//                                                    {
//                                                        if (g.getNbMots()<=nw+1) break;
//                                                        else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                                    }
                                                }
                                                //when we find the end of the vcchain add the semantic link
                                                if (foundVB && pos!=posvb){
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A2", pos,posvb);
                                                }
                                            }
                                        }
                                        
					int[] res={pos};
					return res;
				}
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

        // =================================================================
	class rPRDA1 implements SingleRule{
		public String getName() {return "PRD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			int etreavant = -1;
			for (int pos=0;pos<g.getNbMots();pos++) {
                                String lemFeel=g.getMot(pos).getForme().toLowerCase();
                                if (lemFeel.equals("feel")||lemFeel.equals("feels")||lemFeel.equals("felt")||lemFeel.equals("feeling")) {                            
					etreavant=pos; continue;
				}
				if (etreavant>=0&&(g.getMot(pos).getPOS().startsWith("JJ")||g.getMot(pos).getPOS().startsWith("RB"))) {
					pp.add(pos);
				} else if (etreavant>=0&&g.getMot(pos).getForme().equals("to")) {
					pp.add(pos);
				} else if (etreavant>=0) {
					int[] gr=g.getGroupsThatEndHere(pos);
					if (gr!=null&&gr.length>0) pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(pos);
//                        int ds = gsrl.getDep(pos);
//			if (ds>=0) gsrl.removeDep(ds);
			for (int i=pos-1;i>=0;i--) {
                                String lem=SemanticProcesing.getLemmafromForm(g.getMot(i).getForme());
                                String lemFeel=g.getMot(i).getForme().toLowerCase();
				if (lem.equals("be")||lemFeel.equals("feel")||lemFeel.equals("feels")||lemFeel.equals("felt")||lemFeel.equals("feeling")) {
					g.ajoutDep("PRD", pos, i);
                                        if (SemanticProcesing.isPredicate(g, i) && pos!=i) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A1", pos, i);
                                        }
                                        else{
                                            //see if it's vc-chain... (RB|Mods)*VB
                                            if (ismod(g.getMot(i))) {
                                                //look if there is a verb following the chain of modifiers or rb
                                                boolean foundVB=false;
                                                int posvb=0;
                                                for (int nw=i+1;nw<g.getNbMots();nw++){
                                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                        foundVB=true; posvb=nw;
                                                        break;
                                                    }
                                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                        break;
                                                        //look for been able to: been is in mod, 
//                                                    {
//                                                        if (g.getNbMots()<=nw+1) break;
//                                                        if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                                    }
                                                }
                                                //when we find the end of the vcchain add the semantic link
                                                if (foundVB && pos!=posvb) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A1", pos,posvb);
                                                }
                                            }
                                        }
					int[] res={pos};
					return res;
				}
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
        // =================================================================
	class rPRDA3 implements SingleRule{
		public String getName() {return "PRD";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			int etreavant = -1;
			for (int pos=0;pos<g.getNbMots();pos++) {
				//if (g.getMot(pos).getLemme().equals("feel")) {
                                String lemFeel=g.getMot(pos).getForme().toLowerCase();
                                if (lemFeel.equals("feel")||lemFeel.equals("feels")||lemFeel.equals("felt")||lemFeel.equals("feeling")) {                            
					etreavant=pos; continue;
				}
				if (etreavant>=0&&(g.getMot(pos).getPOS().startsWith("JJ")||g.getMot(pos).getPOS().startsWith("RB"))) {
					pp.add(pos);
				} else if (etreavant>=0&&g.getMot(pos).getForme().equals("to")) {
					pp.add(pos);
				} else if (etreavant>=0) {
					int[] gr=g.getGroupsThatEndHere(pos);
					if (gr!=null&&gr.length>0) pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(pos);
//                        int ds = gsrl.getDep(pos);
//			if (ds>=0) gsrl.removeDep(ds);
			for (int i=pos-1;i>=0;i--) {
//				if (g.getMot(i).getLemme().equals("be")||g.getMot(i).getLemme().equals("feel")) {
                                String lem=SemanticProcesing.getLemmafromForm(g.getMot(i).getForme());
                                String lemFeel=g.getMot(i).getForme().toLowerCase();
				if (lem.equals("be")||lemFeel.equals("feel")||lemFeel.equals("feels")||lemFeel.equals("felt")||lemFeel.equals("feeling")) {
					g.ajoutDep("PRD", pos, i);
                                        if (SemanticProcesing.isPredicate(g, i) && pos!=i) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A3", pos, i);
                                        }
                                        else{
                                            //see if it's vc-chain... (RB|Mods)*VB
                                            if (ismod(g.getMot(i))) {
                                                //look if there is a verb following the chain of modifiers or rb
                                                boolean foundVB=false;
                                                int posvb=0;
                                                for (int nw=i+1;nw<g.getNbMots();nw++){
                                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                        foundVB=true; posvb=nw;
                                                        break;
                                                    }
                                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                        break;
                                                        //look for been able to: been is in mod, 
//                                                    {
//                                                        if (g.getNbMots()<=nw+1) break;
//                                                        else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                                    }
                                                }
                                                //when we find the end of the vcchain add the semantic link
                                                if (foundVB && pos!=posvb) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A3", pos,posvb);
                                                }
                                            }
                                        }
                                        
					int[] res={pos};
					return res;
				}
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
        
	// =================================================================        
        class rSUJRELA0 implements SingleRule {
		public String getName() {return "SUBJECTREL";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				String postag = g.getMot(pos).getPOS();
				if (postag.equals("WDT")) {
					for (int i=pos+1;i<g.getNbMots();i++) {
						if ((g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
							pp.add(pos);
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(pos);
//                        gsrl.removeAllDeps(pos);
//                        int ds = gsrl.getDep(pos);
//			if (ds>=0) gsrl.removeDep(ds);
			for (int i=pos+1;i<g.getNbMots();i++) {
				if ((g.getMot(i).getPOS().startsWith("VB") && !g.getMot(i).getPOS().startsWith("VBG"))) {
					g.ajoutDep("SBJ", pos, i);
                                        if (SemanticProcesing.isPredicate(g, i) && pos !=i) {
                                            for (int dd: depsWithPos){
                                                if (gsrl.getHead(dd)==i) gsrl.removeDep(dd);
                                            }
                                            gsrl.ajoutDep("A0", pos, i);
                                            //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                            if (i+2<g.getNbMots()){
                                                if (g.getMot(i+1).getForme().equals("to") && g.getMot(i+2).getPOS().equals("VB") && pos!=i+2){ 
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==i+2) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A0", pos, i+2);
                                                }
                                            }
                                        }
                                        else{
                                            //see if it's vc-chain... (RB|Mods)*VB
                                            if (ismod(g.getMot(i))) {
                                                //look if there is a verb following the chain of modifiers or rb
                                                boolean foundVB=false;
                                                int posvb=0;
                                                for (int nw=i+1;nw<g.getNbMots();nw++){
                                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                                        foundVB=true; posvb=nw;
                                                        break;
                                                    }
                                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                                        break;
                                                        //look for been able to: been is in mod, 
//                                                    {
//                                                        if (g.getNbMots()<=nw+1) break;
//                                                        else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                                            break;
//                                                    }
                                                }
                                                //when we find the end of the vcchain add the semantic link
                                                if (foundVB && pos!=posvb) {
                                                    for (int dd: depsWithPos){
                                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                                    }
                                                    gsrl.ajoutDep("A0", pos,posvb);
                                                    //look for the patter VB+To+VB in which case, if we add A0 to the first VB, add it also to the second
                                                    if (posvb+2<g.getNbMots()){
                                                        if (g.getMot(posvb+1).getForme().equals("to") && g.getMot(posvb+2).getPOS().equals("VB") && pos!=posvb+2){ 
                                                                    for (int dd: depsWithPos){
                                                                        if (gsrl.getHead(dd)==posvb+2) gsrl.removeDep(dd);
                                                                    }
                                                                    gsrl.ajoutDep("A0", pos, posvb+2);
                                                        }
                                                    }
                                                    
                                                }
                                            }
                                        }
                                        
					if (pos>0) {
						String postag = g.getMot(pos-1).getPOS();
						if (postag.startsWith("NN")) {
							g.ajoutDep("NMOD", i, pos-1);
							int[] res={pos,i};
							return res;
						} 
					}
					int[] res={pos};
					return res;
				}
			}
			return null;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        

        //        
//	// =================================================================
//	/**
//	 * R7: link ? 
//	 */
	class rRBlAM implements SingleRule{
		public String getName() {return "RBl";}
		public int[] getApplicable(DetGraph g){
			int closest;
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=1;pos<g.getNbMots();pos++) {
				if (g.getMot(pos).getPOS().startsWith("RB")){
					closest = -1;
					for (int i=pos-1;i>=0;i--) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							if (closest<0||pos-i<closest-i)
								closest=i;
							break;
						}
					}
					if (closest>=0) 
						pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(pos);
//                        int ds = gsrl.getDep(pos);
//			if (ds>=0) gsrl.removeDep(ds);
//                        gsrl.removeAllDeps(pos);
			int closest = -1;
			for (int i=pos-1;i>=0;i--) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
					if (closest<0||pos-i<closest-i)
						closest=i;
					break;
				}
			}
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ADV", pos, closest);
                        
                        if (SemanticProcesing.isPredicate(g, closest) && pos!=closest) {
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==closest) gsrl.removeDep(dd);
                            }
                            gsrl.ajoutDep("AM", pos, closest);
                        }
                        else{
                            //see if it's vc-chain... (RB|Mods)*VB
                            if (ismod(g.getMot(closest))) {
                                //look if there is a verb following the chain of modifiers or rb
                                boolean foundVB=false;
                                int posvb=0;
                                for (int nw=closest+1;nw<g.getNbMots()-1;nw++){
                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                        foundVB=true; posvb=nw;
                                        break;
                                    }
                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                        break;
                                        //look for been able to: been is in mod, 
//                                    {
//                                        if (g.getNbMots()<=nw+1) break;
//                                        else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                            break;
//                                    }
                                }
                                //when we find the end of the vcchain add the semantic link
                                if (foundVB && pos!=posvb){
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                    }
                                    gsrl.ajoutDep("AM", pos,posvb);
                                }
                            }
                        }
                        
			int[] res={pos};
			return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
	class rRBrAM implements SingleRule{
		public String getName() {return "RBr";}
		public int[] getApplicable(DetGraph g){
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int pos=0;pos<g.getNbMots()-1;pos++) {
				if (g.getMot(pos).getPOS().startsWith("RB")){
					int closest = -1;
					for (int i=pos+1;i<g.getNbMots();i++) {
						if (g.getMot(i).getPOS().startsWith("VB")) {
							closest=i;
							break;
						}
					}
					if (closest>=0) 
						pp.add(pos);
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;

		}
		public int[] applyRule(DetGraph g, int pos) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int[]depsWithPos=gsrl.getDeps(pos);
//                        int ds = gsrl.getDep(pos);
//			if (ds>=0) gsrl.removeDep(ds);
			int closest = -1;
			for (int i=pos+1;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().startsWith("VB")) {
					closest=i;
					break;
				}
			}
			int d=g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep("ADV", pos, closest);
                        if (SemanticProcesing.isPredicate(g, closest) && pos!=closest) {
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==closest) gsrl.removeDep(dd);
                            }
                            gsrl.ajoutDep("AM", pos, closest);
                        }
                        else{
                            //see if it's vc-chain... (RB|Mods)*VB
                            if (ismod(g.getMot(closest))) {
                                //look if there is a verb following the chain of modifiers or rb
                                boolean foundVB=false;
                                int posvb=0;
                                for (int nw=closest+1;nw<g.getNbMots()-1;nw++){
                                    if (g.getMot(nw).getPOS().startsWith("VB")&& SemanticProcesing.isPredicate(g,nw)){
                                        foundVB=true; posvb=nw;
                                        break;
                                    }
                                    if (!g.getMot(nw).getPOS().startsWith("RB")&&!ismod(g.getMot(nw)))
                                        break;
                                        //look for been able to: been is in mod, 
//                                    {
//                                        if (g.getNbMots()<=nw+1) break;
//                                        else if (!g.getMot(nw).getPOS().startsWith("JJ")&&!g.getMot(nw).getForme().toLowerCase().equals("able")&&!g.getMot(nw+1).getForme().toLowerCase().equals("to"))
//                                            break;
//                                    }
                                }
                                //when we find the end of the vcchain add the semantic link
                                if (foundVB && pos!=posvb) {
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==posvb) gsrl.removeDep(dd);
                                    }
                                    gsrl.ajoutDep("AM", pos,posvb);
                                }
                            }
                        }
                        
			int[] res={pos};
			return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
	// =================================================================
	/**
	 * 5)(Rule, not initialization)
	 *  //a- For the fist word set its head to be the head of the sentence.
	 *  b- For each other word, it's the previous word.
	 *  
	 *  Link any (NP,prep) with backward NMOD
	 */

	class rPrepBeforeSRL implements SingleRule {
		private ArrayList<Integer> gov  = new ArrayList<Integer>();
		private ArrayList<Integer> head = new ArrayList<Integer>();
		private ArrayList<Integer> labt = new ArrayList<Integer>();
		public String getName() {return "prepbefore";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().equals("IN")) {
					if (i==0) {
						int root = findSentHead(g);
						if (root>=0) {
							pp.add(head.size()); gov.add(i); head.add(root); labt.add(Dep.getType("ADV"));
							// it can also be a LOC or TMP
						} else {
							// TODO
						}
					} else {
						int[] grs = g.getGroupsThatEndHere(i-1);
						if (grs!=null&&grs.length>0) {
							// NP before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("NMOD"));
						} else if (g.getMot(i-1).getPOS().startsWith("VB")) {
							// verb before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("ADV"));
						} else if (g.getMot(i-1).getPOS().startsWith("IN")) {
							// prep before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("PMOD"));
						} else {
							// TODO
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int i) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
//                        int ds = gsrl.getDep(i);
//			if (ds>=0) gsrl.removeDep(ds);
			int pos=gov.get(i);
                        int[]depsWithPos=gsrl.getDeps(pos);
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep(Dep.depnoms[labt.get(i)], pos, head.get(i));
                        if (Dep.depnoms[labt.get(i)].equals("ADV")) {
                            if (SemanticProcesing.isPredicate(g,  head.get(i))){
                                //A3
                                if ((g.getMot(pos).getForme().startsWith("remain")||g.getMot(head.get(i)).getForme().startsWith("from"))&&pos!=head.get(i)){
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==head.get(i)) gsrl.removeDep(dd); break;
                                    }
                                    gsrl.ajoutDep("A3", pos, head.get(i));
                                }
                                //AM
                                else if (head.get(i)+1<g.getNbMots()){
                                    if ((g.getMot(head.get(i)+1).getPOS().startsWith("CD")||g.getMot(head.get(i)+1).getPOS().startsWith("$"))&& pos!=head.get(i)){
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==head.get(i)) gsrl.removeDep(dd); break;
                                        }
                                        gsrl.ajoutDep("AM", pos, head.get(i));
                                    }
                                }else
                                    if (pos!=head.get(i)) {
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==head.get(i)) gsrl.removeDep(dd); break;
                                        }
                                        gsrl.ajoutDep("A1", pos, head.get(i));
                                    }
                            }
                        }
			int[] res = {pos};
			return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}
        
	class rPrepBeforeSRLbis implements SingleRule {
		private ArrayList<Integer> gov  = new ArrayList<Integer>();
		private ArrayList<Integer> head = new ArrayList<Integer>();
		private ArrayList<Integer> labt = new ArrayList<Integer>();
		public String getName() {return "prepbefore";}
		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().equals("IN")) {
					if (i==0) {
						int root = findSentHead(g);
						if (root>=0) {
							pp.add(head.size()); gov.add(i); head.add(root); labt.add(Dep.getType("ADV"));
							// it can also be a LOC or TMP
						} else {
							// TODO
						}
					} else {
						int[] grs = g.getGroupsThatEndHere(i-1);
						if (grs!=null&&grs.length>0) {
							// NP before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("NMOD"));
						} else if (g.getMot(i-1).getPOS().startsWith("VB")) {
							// verb before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("ADV"));
						} else if (g.getMot(i-1).getPOS().startsWith("IN")) {
							// prep before
							pp.add(head.size()); gov.add(i); head.add(i-1); labt.add(Dep.getType("PMOD"));
						} else {
							// TODO
						}
					}
				}
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int i) {
                        DetGraph gsrl=g.relatedGraphs.get(0);
//                        int ds = gsrl.getDep(i);
//			if (ds>=0) gsrl.removeDep(ds);
			int pos=gov.get(i);
                        int[]depsWithPos=gsrl.getDeps(pos);
			int d = g.getDep(pos);
			if (d>=0) g.removeDep(d);
			g.ajoutDep(Dep.depnoms[labt.get(i)], pos, head.get(i));
                        if (Dep.depnoms[labt.get(i)].equals("ADV")) {
                            if (SemanticProcesing.isPredicate(g,  head.get(i))){
                                //A3
                                if ((g.getMot(pos).getForme().startsWith("remain")||g.getMot(head.get(i)).getForme().startsWith("from"))&& pos!=head.get(i)){
                                    for (int dd: depsWithPos){
                                        if (gsrl.getHead(dd)==head.get(i)) gsrl.removeDep(dd); break;
                                    }
                                    gsrl.ajoutDep("A3", pos, head.get(i));
                                }
                                //AM
                                else if (head.get(i)+1<g.getNbMots()){
                                    if ((g.getMot(head.get(i)+1).getPOS().startsWith("CD")||g.getMot(head.get(i)+1).getPOS().startsWith("$"))&& pos!=head.get(i)){
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==head.get(i)) gsrl.removeDep(dd); break;
                                        }
                                        gsrl.ajoutDep("AM", pos, head.get(i));
                                    }
                                }else 
                                    if (pos!=head.get(i)) {
                                        for (int dd: depsWithPos){
                                            if (gsrl.getHead(dd)==head.get(i)) gsrl.removeDep(dd); break;
                                        }
                                        gsrl.ajoutDep("A2", pos, head.get(i));
                                    }
                            }
                        }
			int[] res = {pos};
			return res;
		}
//		boolean ismod(Mot mmot) {
//			if ( mmot.getPOS().equals("MD")) return true; 
//			String mot = mmot.getForme().toLowerCase();
//			final String[] mods = {"been","should","have","had","has","is","be","are","was","were","would","could","will","may","might","can","do","does","did","'s"};
//			for (String m : mods)
//				if (mot.equals(m)) return true;
//			return false;
//		}
	}

        
        // =================================================================
	class rLGSA0 implements SingleRule {
		public String getName() {return "LGSA0";}
		// cree les GP avec des verbes: "il vient de manger"
		// cree seulement le lien INTERNE au GP

		public int[] getApplicable(DetGraph g) {
			ArrayList<Integer> pp = new ArrayList<Integer>();

			for (int pos=0;pos<g.getNbMots()-1;pos++) {
                            if (g.getMot(pos).getPOS().startsWith("VBN")&& g.getMot(pos+1).getForme().startsWith("by")){
							pp.add(pos);
                            }
			}
			int[] res = new int[pp.size()];
			for (int i=0;i<res.length;i++) res[i] = pp.get(i);
			return res;
		}

		public int[] applyRule(DetGraph g, int pos) {
			String postag = g.getMot(pos).getPOS();
                        DetGraph gsrl=g.relatedGraphs.get(0);
                        int d = g.getDep(pos+1);
                        if (d>=0) g.removeDep(d);
//                        int ds = gsrl.getDep(pos+1);
//                        if (ds>=0) gsrl.removeDep(ds);
                        int[]depsWithPos=gsrl.getDeps(pos+1);
                        g.ajoutDep("LGS", pos+1, pos);
                        if (SemanticProcesing.isPredicate(g, pos) && (pos+1)!=pos){
                            for (int dd: depsWithPos){
                                if (gsrl.getHead(dd)==pos) gsrl.removeDep(dd); break;
                            }
                            gsrl.ajoutDep("A0", pos+1, pos);
                        }
                        int[] res={pos};
                        return res;
		}
	}
        
//
}


