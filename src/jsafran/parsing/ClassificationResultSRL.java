/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jsafran.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import jsafran.DetGraph;
import jsafran.Dep;
import jsafran.GraphIO;
import jsafran.parsing.unsup.SemanticProcesing;

/**
 * The measure traditionally used in srl is the F1 measure:
 *  In the semantic part, we use as evaluation metric the F-Measure, which is defined as the harmonic mean of precision of recall: 
 * f=(2xpxr) /(p+r)
 * According to: "EVALUATION OF SEMANTIC ROLE LABELING AND DEPENDENCY PARSING OF AUTOMATIC SPEECH RECOGNITION OUTPUT"
 * "Recall (R) is the ratio of correct arcs (#corr) in the system output compared to the reference (#ref), R = #corr/#ref.
 * Precision (P) is the proportion of correct arcs in the system output (#hyp), P = #corr/#hyp. 
 * F1-score (referred to as F-score or F-measure) is the harmonic mean between recall and precision, F = 2P R/(P + R)
 * where precision is defined as: 
 * 
 * We carried out two evaluations: first on core arguments only, without regard to modifier arguments, 
 * and second on coarse arguments, considering also a single AM to match all the modifiers arguments.  
 * @author ale
 */
public class ClassificationResultSRL {
	private static boolean debflag=false;
	private static Set<String> predicates=new HashSet<String>();
	private static HashMap<String,Integer> goldRoles=new HashMap<String,Integer>();
	private static HashMap<String,Integer> foundRoles=new HashMap<String,Integer>();
	public static ArrayList<int[]> errors;
	private static String[]coreRarr={"A0","A1","A2","A3","A4"};
	private static String[]coarseRarr={"A0","A1","A2","A3","A4","AM"};
	public static ArrayList<String>coreRoles=new ArrayList<String>();
	public static ArrayList<String>coarseRoles=new ArrayList<String>();

	public static void loadRoles(){
		//load the roles into the arraylist, easiest to look afterwards
		for (int i=0;i<coreRarr.length;i++){
			coreRoles.add(coreRarr[i]);
		}
		for (int i=0;i<coarseRarr.length;i++){
			coarseRoles.add(coarseRarr[i]);
		}
	}


	private static String getRoleLabel(DetGraph gref, int c, int head){
		int dref=-1; String lref="NONE";
		int[]deps=gref.getDeps(c);
		for (int d:deps){
			if (gref.getHead(d)==head)dref=d;
		}
		if (dref>=0) lref= gref.getDepLabel(dref);
		//add to the dictionary of gold roles;
		if (lref.startsWith("AM")) lref="AM";
		if (lref.startsWith("C-")) lref=lref.substring(2,lref.length());
                if (lref.startsWith("R-")) lref=lref.substring(2,lref.length());
		//if (lref.equals("NONE")) return null;
                return lref; 
	}

	/**
	 * To get a comparison with previous state of the art in SRI (semantic role induction/clasification part of the SRL)
	 * Evaluates using the approach to evaluate unsupervised works: for each verb, we compute the purity
	 * and collocation, then we average over all verbs. (see Lang and Lapata 2011, split merge)
	 * TODO: improve performance!!!!!!!!!!!!!
	 * @param grecs
	 * @param grefs
	 * @return 
	 */
	private static Double[] calcErrorsUnsupSRL(List<DetGraph> grecs, List<DetGraph> grefs,boolean print){
		Double[] res= new Double[3]; 
		int totalcoverageinallverbs=0;
		double cero=0;
		//get the list of verbs which are predicates
		for (int i=0;i<grefs.size();i++) {
			DetGraph gref = grefs.get(i);
			for (int recmot=0;recmot<gref.getNbMots();recmot++) {
				if (SemanticProcesing.isGoldPredicate(gref, recmot)) {
					predicates.add(gref.getMot(recmot).getLemme());
					List<Integer> childrenP=gref.getFils(recmot);
					for (int c:childrenP){
						//get dep zith pred
						String lref= getRoleLabel(gref,c,recmot);
						int s=goldRoles.size();
						if (!goldRoles.containsKey(lref)) goldRoles.put(lref, s); 
					}
				}
			}
			//extract the found roles
			DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
			for (int recmot=0;recmot<grec.getNbMots();recmot++) {
				if (SemanticProcesing.isPredicate(grec, recmot)) {
					List<Integer> childrenP=grec.getFils(recmot);
					for (int c:childrenP){
						//get dep zith pred
						String lrec=getRoleLabel(grec, c, recmot);
						//add to the dictionary of gold roles;
						if (!foundRoles.containsKey(lrec)) foundRoles.put(lrec, foundRoles.size()); 
					}
				}
			}
			//add none? 
			if (!foundRoles.containsKey("NONE")) foundRoles.put("NONE", foundRoles.size());
                        if (!goldRoles.containsKey("NONE")) goldRoles.put("NONE", goldRoles.size());
		}

		String[] arrpred=(String[])predicates.toArray(new String[predicates.size()]);
		//for purity and collocation
		//keeps the intersections, where the first link represents the gold role and the second the sample id,
		//so totalIntersecs[0][0]=totalA0and0
		ArrayList<ArrayList<Double>> totalIntersecs;
		ArrayList<Double>intersecsSamples;
		//keeps the number of gold frames
		ArrayList<Double> totalGold;
		//to calculate the maximum (max_j(Gj interseccion Ci)
		ArrayList<Double> maxjs;
		//to calculate the maximum (max_i(Gj interseccion Ci)
		ArrayList<Double> maxis;
		//the purity per verb
		Double[]purity=new Double[arrpred.length];
		//collocation per verb
		Double[]collocation=new Double[arrpred.length];
		//f1 measure per verb...
		Double[]f1=new Double[arrpred.length];
		Integer[]coverageperverb=new Integer[arrpred.length];

		for (int verbid=0;verbid<arrpred.length;verbid++){
			String lem=arrpred[verbid];
			//evaluate the purity and collocation for this verb
			//initialize structures
			int perVerbCoverage=0;
			totalGold=new ArrayList<Double>();
			totalIntersecs=new ArrayList<ArrayList<Double>>();
			for (int i=0;i<goldRoles.keySet().size();i++){
				totalGold.add(cero);
			}
			for (int i=0;i<goldRoles.size();i++){
				intersecsSamples=new ArrayList<Double>();
				for (int j=0;j<foundRoles.size();j++){ //one extra to represent the null= when no role is sample or is a role not in the list
					intersecsSamples.add(cero);
				}
				totalIntersecs.add(intersecsSamples);
			}
			//go through the arguments found in the gold for the evaluation part of the data
			for (int i=0;i<grefs.size();i++) {
				DetGraph gref = grefs.get(i);
				DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
				for (int recmot=0;recmot<gref.getNbMots();recmot++) {
					if (lem.equals(gref.getMot(recmot).getLemme())&&
							SemanticProcesing.isGoldPredicate(gref, recmot)){ 
						//for each argument
						List<Integer> childrenP=gref.getFils(recmot);
						for (int c:childrenP){
							perVerbCoverage++;
							totalcoverageinallverbs++;
							//get dep zith pred
							String lref= getRoleLabel(gref,c,recmot);
							int gind=goldRoles.get(lref);
							//add to the counts..
							totalGold.set(gind,totalGold.get(gind)+1);
							//if the argument was identified by the rules, fill the corresponding intersections,
							String lrec= getRoleLabel(grec,c,recmot);
							int gindf=foundRoles.get(lrec);
							//add to the counts.. 
							//get the entry for the gold, and increment in that entry the entry for the sample
							intersecsSamples=totalIntersecs.get(gind);
							intersecsSamples.set(gindf, intersecsSamples.get(gindf)+1);
							totalIntersecs.set(gind, intersecsSamples);
						}
					}                    
				}               
			}
			//purity=Sum_i(max_j(Gj interseccion Ci))
			maxjs=new ArrayList<Double>();
			double verbpurity=0;
			for (int i=0;i<foundRoles.size();i++){
				maxjs.add(cero);
				for (int j=0;j<goldRoles.size();j++){
					if (totalIntersecs.get(j).get(i)>maxjs.get(i)) maxjs.set(i, totalIntersecs.get(j).get(i));
				}
				verbpurity+=maxjs.get(i);
			}
			verbpurity=verbpurity/perVerbCoverage;

			//collocation=Sum_j(max_i(Gj interseccion Ci))
					maxis=new ArrayList<Double>();
			double verbcollocation=0;
			for (int i=0;i<goldRoles.size();i++){
				maxis.add(cero);
				for (int j=0;j<foundRoles.size();j++){
					if (totalIntersecs.get(i).get(j)>maxis.get(i)) maxis.set(i, totalIntersecs.get(i).get(j));
				}
				verbcollocation+=maxis.get(i);
			}
			verbcollocation=verbcollocation/perVerbCoverage;

			double verbf1=(2*verbpurity*verbcollocation)/(verbpurity+verbcollocation);

			purity[verbid]=verbpurity;
			collocation[verbid]=verbcollocation;
			f1[verbid]=verbf1;
			coverageperverb[verbid]=perVerbCoverage;

		} 
		//the average is weighted (micro average see Lapata)
		double globalPurity=0;
		double globalColl=0;
		double wglobalPurity=0;
		double wglobalColl=0;

		double microAvF1=0;
		double macroAvF1=0;
		double totalVerbscovered=0;

		for (int indv=0;indv<purity.length;indv++){
			//get the proportion of coverage of the verb:
			double weigth=(double)coverageperverb[indv]/(double)totalcoverageinallverbs;
			//            System.out.println("purity:"+purity[indv]+" weiht: "+ weigth);
			if (coverageperverb[indv]>0){
				totalVerbscovered+=coverageperverb[indv];
				wglobalColl+=collocation[indv]*weigth;
				wglobalPurity+=purity[indv]*weigth;

				globalColl+=collocation[indv];
				globalPurity+=purity[indv];
				macroAvF1+=f1[indv];
			}
		}
		microAvF1=(2*wglobalPurity*wglobalColl)/(wglobalPurity+wglobalColl);
                if (print){
                    System.out.println("Weighted purity:"+wglobalPurity);
                    System.out.println("Weighted collocation:"+wglobalColl);
                    System.out.println("Weighted Micro Av.F1: "+ microAvF1);
                }
		res[0]=wglobalPurity;//purity
		res[1]=wglobalColl;//colocation
		res[2]=microAvF1;//fmeasure
		return res;
	} 

//	/**
//	 * To get a comparison with previous state of the art in SRI (semantic role induction/clasification part of the SRL)
//	 * Evaluates using the approach to evaluate unsupervised works: for each verb, we compute the purity
//	 * and collocation, then we average over all verbs. (see Lang and Lapata 2011, split merge)
//	 * TODO: improve performance!!!!!!!!!!!!!
//	 * @param grecs
//	 * @param grefs
//	 * @return 
//	 */
//	private static Double[] calcErrorsUnsupSRL(List<DetGraph> grecs, List<DetGraph> grefs,boolean print){
//		Double[] res= new Double[3]; 
//		int totalcoverageinallverbs=0;
//		double cero=0;
//		//get the list of verbs which are predicates
//		for (int i=0;i<grefs.size();i++) {
//			DetGraph gref = grefs.get(i);
//			for (int recmot=0;recmot<gref.getNbMots();recmot++) {
//				if (SemanticProcesing.isGoldPredicate(gref, recmot)) {
//					predicates.add(gref.getMot(recmot).getLemme());
//					List<Integer> childrenP=gref.getFils(recmot);
//					for (int c:childrenP){
//						//get dep zith pred
//						String lref= getRoleLabel(gref,c,recmot);
//                                                if (lref!=null){
//                                                    int s=goldRoles.size();
//                                                    if (!goldRoles.containsKey(lref)) goldRoles.put(lref, s); 
//                                                }
//					}
//				}
//			}
//			//extract the found roles
//			DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
//			for (int recmot=0;recmot<grec.getNbMots();recmot++) {
//				if (SemanticProcesing.isPredicate(grec, recmot)) {
//					List<Integer> childrenP=grec.getFils(recmot);
//					for (int c:childrenP){
//						//get dep zith pred
//						String lrec=getRoleLabel(grec, c, recmot);
//						//add to the dictionary of gold roles;
//                                                if (lrec!=null){
//                                                    if (!foundRoles.containsKey(lrec)) foundRoles.put(lrec, foundRoles.size()); 
//                                                }
//					}
//				}
//			}
//			//add none? 
//			//if (!foundRoles.containsKey("NONE")) foundRoles.put("NONE", foundRoles.size());
//		}
//
//		String[] arrpred=(String[])predicates.toArray(new String[predicates.size()]);
//		//for purity and collocation
//		//keeps the intersections, where the first link represents the gold role and the second the sample id,
//		//so totalIntersecs[0][0]=totalA0and0
//		ArrayList<ArrayList<Double>> totalIntersecs;
//		ArrayList<Double>intersecsSamples;
//		//keeps the number of gold frames
//		ArrayList<Double> totalGold;
//		//to calculate the maximum (max_j(Gj interseccion Ci)
//		ArrayList<Double> maxjs;
//		//to calculate the maximum (max_i(Gj interseccion Ci)
//		ArrayList<Double> maxis;
//		//the purity per verb
//		Double[]purity=new Double[arrpred.length];
//		//collocation per verb
//		Double[]collocation=new Double[arrpred.length];
//		//f1 measure per verb...
//		Double[]f1=new Double[arrpred.length];
//		Integer[]coverageperverb=new Integer[arrpred.length];
//
//		for (int verbid=0;verbid<arrpred.length;verbid++){
//			String lem=arrpred[verbid];
//			//evaluate the purity and collocation for this verb
//			//initialize structures
//			int perVerbCoverage=0;
//			totalGold=new ArrayList<Double>();
//			totalIntersecs=new ArrayList<ArrayList<Double>>();
//			for (int i=0;i<goldRoles.keySet().size();i++){
//				totalGold.add(cero);
//			}
//			for (int i=0;i<goldRoles.size();i++){
//				intersecsSamples=new ArrayList<Double>();
//				for (int j=0;j<foundRoles.size();j++){ //one extra to represent the null= when no role is sample or is a role not in the list
//					intersecsSamples.add(cero);
//				}
//				totalIntersecs.add(intersecsSamples);
//			}
//			//go through the arguments found in the gold for the evaluation part of the data
//			for (int i=0;i<grefs.size();i++) {
//				DetGraph gref = grefs.get(i);
//				DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
//				for (int recmot=0;recmot<gref.getNbMots();recmot++) {
//					if (lem.equals(gref.getMot(recmot).getLemme())&&
//							SemanticProcesing.isGoldPredicate(gref, recmot)){ 
//						//for each argument
//						List<Integer> childrenP=gref.getFils(recmot);
//						for (int c:childrenP){
//							perVerbCoverage++;
//							totalcoverageinallverbs++;
//							//get dep zith pred
//							String lref= getRoleLabel(gref,c,recmot);
//                                                        if (lref!=null){
//                                                            int gind=goldRoles.get(lref);
//                                                            //add to the counts..
//                                                            totalGold.set(gind,totalGold.get(gind)+1);
//                                                            //if the argument was identified by the rules, fill the corresponding intersections,
//                                                            String lrec= getRoleLabel(grec,c,recmot);
//                                                            if (lrec!=null){
//                                                                int gindf=foundRoles.get(lrec);
//                                                                //add to the counts.. 
//                                                                //get the entry for the gold, and increment in that entry the entry for the sample
//                                                                intersecsSamples=totalIntersecs.get(gind);
//                                                                intersecsSamples.set(gindf, intersecsSamples.get(gindf)+1);
//                                                                totalIntersecs.set(gind, intersecsSamples);
//                                                            }
//                                                        }
//						}
//					}                    
//				}               
//			}
//			//purity=Sum_i(max_j(Gj interseccion Ci))
//			maxjs=new ArrayList<Double>();
//			double verbpurity=0;
//			for (int i=0;i<foundRoles.size();i++){
//				maxjs.add(cero);
//				for (int j=0;j<goldRoles.size();j++){
//					if (totalIntersecs.get(j).get(i)>maxjs.get(i)) maxjs.set(i, totalIntersecs.get(j).get(i));
//				}
//				verbpurity+=maxjs.get(i);
//			}
//			verbpurity=verbpurity/perVerbCoverage;
//
//			//collocation=Sum_j(max_i(Gj interseccion Ci))
//					maxis=new ArrayList<Double>();
//			double verbcollocation=0;
//			for (int i=0;i<goldRoles.size();i++){
//				maxis.add(cero);
//				for (int j=0;j<foundRoles.size();j++){
//					if (totalIntersecs.get(i).get(j)>maxis.get(i)) maxis.set(i, totalIntersecs.get(i).get(j));
//				}
//				verbcollocation+=maxis.get(i);
//			}
//			verbcollocation=verbcollocation/perVerbCoverage;
//
//			double verbf1=(2*verbpurity*verbcollocation)/(verbpurity+verbcollocation);
//
//			purity[verbid]=verbpurity;
//			collocation[verbid]=verbcollocation;
//			f1[verbid]=verbf1;
//			coverageperverb[verbid]=perVerbCoverage;
//
//		} 
//		//the average is weighted (micro average see Lapata)
//		double globalPurity=0;
//		double globalColl=0;
//		double wglobalPurity=0;
//		double wglobalColl=0;
//
//		double microAvF1=0;
//		double macroAvF1=0;
//		double totalVerbscovered=0;
//
//		for (int indv=0;indv<purity.length;indv++){
//			//get the proportion of coverage of the verb:
//			double weigth=(double)coverageperverb[indv]/(double)totalcoverageinallverbs;
//			//            System.out.println("purity:"+purity[indv]+" weiht: "+ weigth);
//			if (coverageperverb[indv]>0){
//				totalVerbscovered+=coverageperverb[indv];
//				wglobalColl+=collocation[indv]*weigth;
//				wglobalPurity+=purity[indv]*weigth;
//
//				globalColl+=collocation[indv];
//				globalPurity+=purity[indv];
//				macroAvF1+=f1[indv];
//			}
//		}
//		microAvF1=(2*wglobalPurity*wglobalColl)/(wglobalPurity+wglobalColl);
//                if (print){
//                    System.out.println("Weighted purity:"+wglobalPurity);
//                    System.out.println("Weighted collocation:"+wglobalColl);
//                    System.out.println("Weighted Micro Av.F1: "+ microAvF1);
//                }
//		res[0]=wglobalPurity;//purity
//		res[1]=wglobalColl;//colocation
//		res[2]=microAvF1;//fmeasure
//		return res;
//	} 

        

        
	public static void saveSRLgraphs(List<DetGraph> grecs) {
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		for (int i=0;i<grecs.size();i++) {
			gs.add(grecs.get(i).relatedGraphs.get(0));
		}
		GraphIO gio = new GraphIO(null);
		gio.save(gs, "recsrl.xml");
		GraphIO.saveConll05SRL(grecs, gs, "recsrl.props", false);

		for (DetGraph g : gs) {
			for (Dep d : g.deps) {
				d.type=Dep.getType("MOD");
			}
		}
		GraphIO.saveConll05SRL(grecs, gs, "recsrl0.props", false);

	}

	public static float evalconll05(List<DetGraph> grecsynt, List<DetGraph> recsrllevel, List<DetGraph> goldsrllevel, int onlyutt, List<DetGraph> alltrees) {
		float fmes=-1f;
		String gold = "corp05g.props";
		File f =new File(gold);
		if (!f.exists()) return fmes;
		// show all in levels
		if (recsrllevel!=null) {
			for (int i=0;i<grecsynt.size();i++) {
				recsrllevel.set(i, grecsynt.get(i).relatedGraphs.get(0));
			}
			if (goldsrllevel!=null) {
				// load gold to display
				List<DetGraph> goldgs = GraphIO.loadConll05SRL(grecsynt, gold);
				for (int i=0;i<grecsynt.size();i++) {
					goldsrllevel.set(i, goldgs.get(i));
				}
			}
		}
		// compute score
		final int typ0 = Dep.getType("MOD");
		ArrayList<DetGraph> recgs = new ArrayList<DetGraph>();
		for (DetGraph g : grecsynt) {
			DetGraph gg = g.relatedGraphs.get(0).clone();
			recgs.add(gg);
			for (Dep d : gg.deps) {
				d.type=typ0;
			}
		}
		GraphIO.saveConll05SRL(grecsynt, recgs, "rectmp.props", false);
		if (onlyutt>=0) {
			List<DetGraph> propgraphs = GraphIO.loadConll05SRL(alltrees,gold);
			gold = gold+"2";
			ArrayList<DetGraph> gssrl = new ArrayList<DetGraph>();
			gssrl.add(propgraphs.get(onlyutt));
			GraphIO.saveConll05SRL(grecsynt, gssrl, gold, true);
		}
		try {
			System.out.println("EXECUTING SRLEVAL");
			ProcessBuilder pb = new ProcessBuilder("perl","srl-eval.pl",gold,"rectmp.props");
			pb.redirectErrorStream(true);
			Process p = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String resultLine = in.readLine();
			while (resultLine != null) {
				if (resultLine.contains("Overall")) {
					StringTokenizer st = new StringTokenizer(resultLine);
					if (st.countTokens()==7) {
						for (int z=0;z<6;z++) st.nextToken();
						fmes = Float.parseFloat(st.nextToken());
					}
				}
			  System.out.println(resultLine);
			  resultLine = in.readLine();
			}
//			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fmes;
	}
        
        private static int getindexRole(String roleLabel, boolean coarse){
            int indrole;
            if (roleLabel.equals("NONE")){
                if (coarse)return coarseRoles.size();
                else return coreRoles.size();
            }
            if (coarse) indrole=coarseRoles.indexOf(roleLabel);
            else indrole=coreRoles.indexOf(roleLabel);
            return indrole;
        }

	public static float[] calcErrors(List<DetGraph> grecs, List<DetGraph> grefs, boolean print) {
            int classesCore=coreRarr.length+1;
            int classesCoarse=coarseRarr.length+1;
            errors = new ArrayList<int[]>();
            //tp, fp and fn per class
            int[]truePosCoarse= new int[classesCoarse];
            int[]truePosCore= new int[classesCore];
            int[]falsePosCoarse= new int[classesCoarse];
            int[]falsePosCore= new int[classesCore];
            int[]falseNegCoarse= new int[classesCoarse];
            int[]falseNegCore= new int[classesCore];
            //initialize to zero
            Arrays.fill(truePosCoarse,0);
            Arrays.fill(truePosCore,0);
            Arrays.fill(falsePosCoarse,0);
            Arrays.fill(falsePosCore,0);
            Arrays.fill(falseNegCoarse,0);
            Arrays.fill(falseNegCore,0);
		//for identification
		int tposIdentCore=0;
		int tposIdentCoarse=0;
		int tnegIdentCore=0;
		int tnegIdentCoarse=0;
		int fposIdentCore=0;
		int fposIdentCoarse=0;
		int fnegIdentCore=0;
		int fnegIdentCoarse=0;
		//for clasification
                int correctCore=0;
                int correctCoarse=0;
                int goldCore=0;
                int goldCoarse=0;
                int hypCore=0;
                int hypCoarse=0;

                //for each sentence
		for (int i=0;i<grefs.size();i++) {
			StringBuffer debug=new StringBuffer();
			boolean write=false;
			ArrayList<Integer> errwords = new ArrayList<Integer>();
			DetGraph gref = grefs.get(i);
			DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
			debug.append("\nG:"+gref);
			for (int recmot=0;recmot<grec.getNbMots();recmot++) {
				if (SemanticProcesing.isPredicate(grec, recmot)){
					debug.append("\nPredicate: "+recmot+" "+gref.getMot(recmot).getForme());
					debug.append("\n");
					List<Integer> childrenP=gref.getFils(recmot);
					for (int c:childrenP){
						int drec = -1;//grec.getDep(c);
						int dref = -1;//gref.getDep(c);
						//get dep zith pred
						int[]deps=gref.getDeps(c);
						for (int d:deps){
							if (gref.getHead(d)==recmot)dref=d;
						}
						deps=grec.getDeps(c);
						for (int d:deps){
							if (grec.getHead(d)==recmot)drec=d;
						}
						if (drec!=dref && true){
							debug.append(" *  Ref: ");
							String l1="",l2="";
							if (dref>=0){l1=gref.getDepLabel(dref); debug.append(c+" "+l1);}
							else debug.append("none");
							debug.append("  rec: ");
							if (drec>=0){l2=grec.getDepLabel(drec); debug.append(c+" "+l2);}
							else debug.append("none"); 
							if (l1!=l2) write=true;
						}
						//ref= exist, guess = none ==> id: fn, clas: fn
                                                if (drec<0&&dref>=0) {
							//if is an argument to consider:
							String lref=gref.getDepLabel(dref);
							if (lref.startsWith("AM")) lref="AM";
							if (lref.startsWith("C-")) lref=lref.substring(2,lref.length());
                                                        if (lref.startsWith("R-")) lref=lref.substring(2,lref.length());
							if (coarseRoles.contains(lref)){
								errwords.add(i);
								if (lref.equals("AM")) {
                                                                    goldCoarse++;
									//false negative identification
									fnegIdentCoarse++;
									//false negative clasification
                                                                        //hypCoarse++;
                                                                        int indrole=getindexRole(lref,true);
                                                                        falseNegCoarse[indrole]++;
								}else{
                                                                    goldCore++;
                                                                    goldCoarse++;
									//false negative identification
									fnegIdentCore++;
									fnegIdentCoarse++;
									//false negative clasification
                                                                        //hypCore++;
                                                                        //hypCoarse++;
                                                                        int indrole=getindexRole(lref,false);
                                                                        falseNegCoarse[indrole]++;
                                                                        falseNegCore[indrole]++;
								}

							}
                                                
						} else if (drec<0&&dref<0) { //ref=none, guess= none ==> only for identif. tn
							//true negative for identification and clasification
							tnegIdentCoarse++;
							tnegIdentCore++;
						} else if (drec>=0&&dref>=0) { //for id: tp; for class: see values, can be tp, fn, or fp 
							//if is an argument to consider:
							String lref=gref.getDepLabel(dref);
							String lrec=grec.getDepLabel(drec);
							//int indf=coreRoles.indexOf(lref);
							//int indc=coreRoles.indexOf(lrec);
							if (lref.startsWith("AM")) lref="AM";
							if (coarseRoles.contains(lref)){
								//true positive for identification
								if (lref.equals("AM")){
                                                                    goldCoarse++;
									tposIdentCoarse++;
									//see clasification
									if (lrec.equals("AM")){
                                                                            hypCoarse++;
                                                                            correctCoarse++;
                                                                            int indrole=getindexRole(lrec,true);
                                                                            truePosCoarse[indrole]++;
									}else{
                                                                            hypCoarse++;
                                                                            hypCore++;
                                                                            int indrole=getindexRole(lref,true);
                                                                            int indrolec=getindexRole(lrec,true);
                                                                            falseNegCoarse[indrole]++;
                                                                            falsePosCoarse[indrolec]++;
									}
								}else{ //the ref is a core role
                                                                    goldCoarse++;
                                                                    goldCore++;
									tposIdentCore++;
									tposIdentCoarse++;
									if (lrec.equals(lref)){
										//tp for the role
                                                                            correctCore++;
                                                                            correctCoarse++;
                                                                            hypCore++;
                                                                            hypCoarse++;
                                                                            int indrole=getindexRole(lrec,false);
                                                                            truePosCoarse[indrole]++;
                                                                            truePosCore[indrole]++;
									}else{ //the rec is not the same
										//for the ref role, it's false negative
                                                                            hypCore++;
                                                                            hypCoarse++;
                                                                            int indrole=getindexRole(lref,false);
                                                                            falseNegCoarse[indrole]++;
                                                                            falseNegCore[indrole]++;
                                                                            if (lrec.equals("AM")){
                                                                                int indrolec=getindexRole(lrec,true);
                                                                                falsePosCoarse[indrolec]++;
                                                                            }else{
                                                                                int indrolec=getindexRole(lrec,false);
                                                                                falsePosCoarse[indrolec]++;
                                                                                falsePosCore[indrolec]++;
                                                                            }
									}
								}
							}
						}                            
					}
					List<Integer> childrenF=gref.getFils(recmot);
					for (int c:childrenF){
						int drec = grec.getDep(c);
						int dref = gref.getDep(c);
						//get dep zith pred
						int[]deps=gref.getDeps(c);
						for (int d:deps){
							if (gref.getHead(d)==recmot)dref=d;
						}
						deps=grec.getDeps(c);
						for (int d:deps){
							if (grec.getHead(d)==recmot)drec=d;
						}
						if (drec>=0&&dref<0) { //ref=none, guess=exist ==> id: fp, clas: fp
								//if is an argument to consider:
									String lrec=grec.getDepLabel(drec);
									if (lrec.startsWith("AM")) lrec="AM";
									if (coarseRoles.contains(lrec)){
										errwords.add(i);
										if (lrec.equals("AM")) {
											//false positive identification
											fposIdentCoarse++;
											//false negative clasification
                                                                                        hypCoarse++;
                                                                                        int indrole=getindexRole(lrec,true);
                                                                                        falsePosCoarse[indrole]++;
                                                                                        
										}else{
											//false positive identification
											fposIdentCore++;
											fposIdentCoarse++;
											//false negative clasification
                                                                                        hypCoarse++;
                                                                                        hypCore++;
                                                                                        int indrole=getindexRole(lrec,false);
                                                                                        falsePosCoarse[indrole]++;
                                                                                        falsePosCore[indrole]++;
										}
									}
                                                }
					}                        
				}



			}
			if (debflag && write)System.out.println(debug.toString());
			for (int w : errwords) {
				int[] newerr = {1,i,w};
				errors.add(newerr);
			}

		}
                if (print){
                    System.out.println("\nSRL:");
                    System.out.println("Core: ID : TP="+tposIdentCore+" TN="+tnegIdentCore+" FP="+fposIdentCore+" FN="+fnegIdentCore);
                    System.out.println("Coarse: ID : TP="+tposIdentCoarse+" TN="+tnegIdentCoarse+" FP="+fposIdentCoarse+" FN="+fnegIdentCoarse);
                    System.out.println("Core: corrects="+correctCore+" Gold="+goldCore+" hyp="+hypCore);
                    System.out.println("Coarse: corrects="+correctCoarse+" Gold="+goldCoarse+" hyp="+hypCoarse);
                }
                if (true){
                    //print the average of the precision and rec
                    //get the micro av. for p and r, then compute normally the fmeasure
                    int totTP=0;
                    int totTPFP=0;
                    int totTPFN=0;
                    int totTPcore=0;
                    int totTPFPcore=0;
                    int totTPFNcore=0;
                    for (int i=0;i<coarseRarr.length;i++){
                        totTP+=truePosCoarse[i];
                        totTPFP+=truePosCoarse[i]+falsePosCoarse[i];
                        totTPFN+=truePosCoarse[i]+falseNegCoarse[i];
                    }
                    for (int i=0;i<coreRarr.length;i++){
                        totTPcore+=truePosCore[i];
                        totTPFPcore+=truePosCore[i]+falsePosCore[i];
                        totTPFNcore+=truePosCore[i]+falseNegCore[i];
                    }
                    if (totTP>0){
                        System.out.println("totales: totTP:"+totTP+" totTPFP:"+totTPFP+" totTPFN:"+totTPFN);
                        System.out.println("totales core: totTP:"+totTPcore+" totTPFP:"+totTPFPcore+" totTPFN:"+totTPFNcore);
                        double fmicroCore=0;
                        double fmicroCoarse=0;
                        double purCore=0;double reCore=0;
                        purCore=(totTPcore>0)?((double)totTPcore/totTPFPcore):0;
                        reCore=(totTPcore>0)?((double)totTPcore/totTPFNcore):0;
                        double purCoarse=(totTP>0)?((double)totTP/totTPFP):0;
                        double reCoarse=(totTP>0)?((double)totTP/totTPFN):0;
                        fmicroCore=(purCore*reCore>0)?((2.0*purCore*reCore)/(purCore+reCore)):0;
                        fmicroCoarse=(purCoarse*reCoarse>0)?((2.0*purCoarse*reCoarse)/(purCoarse+reCoarse)):0;
                        System.out.println("F1 micro Core="+fmicroCore);
                        System.out.println("Precision Coarse= "+ purCoarse+ " Recall= "+reCoarse);
                        System.out.println("F1 micro Coarse="+fmicroCoarse);
                        //print the macro fmeasure: av. fmeasure for each class then get av. between fmeasures
                        double[]fsmacroCoarse= new double[classesCoarse];
                        double[]fsmacroCore= new double[classesCore];
                        double[]puritiesCoarse= new double[classesCoarse];
                        double[]puritiesCore= new double[classesCore];
                        double[]recallsCoarse= new double[classesCoarse];
                        double[]recallsCore= new double[classesCore];
                        double sumfsmacroCoarse=0;
                        double sumfsmacroCore=0;
                        for (int i=0;i<classesCoarse;i++){
                            if (truePosCoarse[i]>0){
                            puritiesCoarse[i]=(double)truePosCoarse[i]/(truePosCoarse[i]+falsePosCoarse[i]);
                            recallsCoarse[i]=(double)truePosCoarse[i]/(truePosCoarse[i]+falseNegCoarse[i]);
                            if (puritiesCoarse[i]>0 && recallsCoarse[i]>0) fsmacroCoarse[i]=(double)2.0*puritiesCoarse[i]*recallsCoarse[i]/(puritiesCoarse[i]+recallsCoarse[i]);
                            else fsmacroCoarse[i]=0;
                            sumfsmacroCoarse+=fsmacroCoarse[i];
                            }
                        }
                        for (int i=0;i<classesCore;i++){
                            if (truePosCore[i]>0){
                            puritiesCore[i]=(double)truePosCore[i]/(truePosCore[i]+falsePosCore[i]);
                            recallsCore[i]=(double)truePosCore[i]/(truePosCore[i]+falseNegCore[i]);
                            if (puritiesCore[i]>0 && recallsCore[i]>0)  fsmacroCore[i]=(double)2.0*puritiesCore[i]*recallsCore[i]/(puritiesCore[i]+recallsCore[i]);
                            else fsmacroCore[i]=0;
                            sumfsmacroCore+=fsmacroCore[i];
                            }
                        }
                        double fmacroCoarse,fmacroCore;
                        fmacroCoarse=(double)sumfsmacroCoarse/(double)classesCoarse;
                        fmacroCore=(double)sumfsmacroCore/(double)classesCore; 
                        System.out.println("F1 macro Core="+fmacroCore);
                        System.out.println("F1 macro Coarse="+fmacroCoarse);
                    }
                }
		float[] res= new float[4]; //fmeasure for identification (core and coarse) and clasification (core and coarse)
		float[] precision = new float[4];
		float[] recall = new float[4];
		//precision = tp/(tp+fp)
		precision[0]=(float)tposIdentCore/((float)(tposIdentCore+fposIdentCore));
		precision[1]=(float)tposIdentCoarse/((float)(tposIdentCoarse+fposIdentCoarse));
		precision[2]=(float)correctCore/((float)(goldCore));
		precision[3]=(float)correctCoarse/((float)(goldCoarse));
		//recall = tp/(tp+fn)
		recall[0]=(float)tposIdentCore/((float)(tposIdentCore+fnegIdentCore));
		recall[1]=(float)tposIdentCoarse/((float)(tposIdentCoarse+fnegIdentCoarse));
		recall[2]=(float)correctCore/((float)(hypCore));
		recall[3]=(float)correctCoarse/((float)(hypCoarse));
		//fm=
		for (int i=0;i<res.length;i++){
			res[i]=(2*precision[i]*recall[i])/(precision[i]+recall[i]);
		}
		calcErrorsUnsupSRL(grecs, grefs,print);
		//System.out.println("acc counts: ntot="+ntot+" nok="+nokLAS+" nsub="+nsub+" nhead="+nhead+" ndel="+ndel+" nins="+nins);
		return res;
	}

 /*******************************************************************French***************************************************/
        private static String getRolePropBank(String verbNetRole){
            String pbRole=null;
            boolean considerGold=false;
            if (verbNetRole.startsWith("Agent")||verbNetRole.startsWith("Experiencer")||verbNetRole.startsWith("Cause")) {
                pbRole="A0";
                considerGold=true;
            }
            if (verbNetRole.startsWith("Theme")||verbNetRole.startsWith("Patient")||verbNetRole.startsWith("Topic")||verbNetRole.startsWith("PredAtt")){
                pbRole="A1";
                considerGold=true;
            }
            if (verbNetRole.startsWith("NULL")||verbNetRole.startsWith("Start")||verbNetRole.startsWith("End")||verbNetRole.startsWith("Extent")||
                verbNetRole.startsWith("Location")||verbNetRole.startsWith("Instrument")||verbNetRole.startsWith("Beneficiary")){
                pbRole="Other";
                considerGold=true;
                if (verbNetRole.startsWith("NULL")||verbNetRole.startsWith("Extent"))
                    considerGold=false;
            }
            if (considerGold) return pbRole;
            else return null;
//            if (verbNetRole.startsWith("Agent")||verbNetRole.startsWith("Exper")) pbRole="A0";
//            if (verbNetRole.startsWith("Patient")||verbNetRole.startsWith("Theme")||verbNetRole.startsWith("Topic")||verbNetRole.startsWith("Pred")) pbRole="A1";
//            else pbRole="AM";
            
        }
        
//	public static float[] calcErrorsFrenchSRL(List<DetGraph> grecs, List<DetGraph> grefs, boolean print) {
//            int classesCore=coreRarr.length+1;
//            int classesCoarse=coarseRarr.length+1;
//            errors = new ArrayList<int[]>();
//            //tp, fp and fn per class
//            int[]truePosCoarse= new int[classesCoarse];
//            int[]falsePosCoarse= new int[classesCoarse];
//            int[]falseNegCoarse= new int[classesCoarse];
//            //initialize to zero
//            Arrays.fill(truePosCoarse,0);
//            Arrays.fill(falsePosCoarse,0);
//            Arrays.fill(falseNegCoarse,0);
//		//for identification
//		int tposIdentCoarse=0;
//		int tnegIdentCoarse=0;
//		int fposIdentCoarse=0;
//		int fnegIdentCoarse=0;
//		//for clasification
//                int correctCoarse=0;
//                int goldCoarse=0;
//                int hypCoarse=0;
//                //for each sentence
//		for (int i=0;i<grefs.size();i++) {
//                        
//			StringBuffer debug=new StringBuffer();
//			boolean write=false;
//			ArrayList<Integer> errwords = new ArrayList<Integer>();
//			DetGraph gref = grefs.get(i);
//			DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
//			debug.append("\nG:"+gref);
//			for (int recmot=0;recmot<grec.getNbMots();recmot++) {
//                                    List<Integer> childrenF=gref.getFils(recmot);
//                                    for (int c:childrenF){
//                                            int drec = grec.getDep(c);
//                                            int dref = gref.getDep(c);
//                                            //get dep zith pred
//                                            int[]deps=gref.getDeps(c);
//                                            for (int d:deps){
//                                                    if (gref.getHead(d)==recmot)dref=d;
//                                            }
//                                            deps=grec.getDeps(c);
//                                            for (int d:deps){
//                                                    if (grec.getHead(d)==recmot)drec=d;
//                                            }
//                                            
//                                            //if the reference exists but not the infered 
//                                            if (drec<0&&dref>=0) {
//                                                    //if is an argument to consider:
//                                                    String lref=gref.getDepLabel(dref);
//                                                    lref=getRolePropBank(lref);
//                                                    goldCoarse++;
//                                                    fnegIdentCoarse++;
//                                                    int indrole=getindexRole(lref,true);
//                                                    falseNegCoarse[indrole]++;
//                                            } else if (drec<0&&dref<0) { //ref=none, guess= none ==> only for identif. tn
//                                                    //true negative for identification and clasification
//                                                    tnegIdentCoarse++;
//                                            } else if (drec>=0&&dref>=0) { //for id: tp; for class: see values, can be tp, fn, or fp 
//                                                    //if is an argument to consider:
//                                                    String lref=gref.getDepLabel(dref);
//                                                    String lrec=grec.getDepLabel(drec);
//                                                    lref=getRolePropBank(lref);
//                                                    goldCoarse++;
//                                                    tposIdentCoarse++;
//                                                    if (lrec.equals(lref)){
//                                                        hypCoarse++;
//                                                        correctCoarse++;
//                                                        int indrole=getindexRole(lrec,true);
//                                                        truePosCoarse[indrole]++;
//                                                    }else{
//                                                        hypCoarse++;
//                                                        int indrole=getindexRole(lref,true);
//                                                        int indrolec=getindexRole(lrec,true);
//                                                        falseNegCoarse[indrole]++;
//                                                        falsePosCoarse[indrolec]++;
//                                                    }
//                                                    
//                                            }
//                                    }
//                    }
//                }
//                if (print){
//                    System.out.println("\nSRL:");
//                    System.out.println("Coarse: ID : TP="+tposIdentCoarse+" TN="+tnegIdentCoarse+" FP="+fposIdentCoarse+" FN="+fnegIdentCoarse);
//                    System.out.println("Coarse: corrects="+correctCoarse+" Gold="+goldCoarse+" hyp="+hypCoarse);
//                }
//                if (true){
//                    //print the average of the precision and rec
//                    //get the micro av. for p and r, then compute normally the fmeasure
//                    int totTP=0;
//                    int totTPFP=0;
//                    int totTPFN=0;
//                    for (int i=0;i<coarseRarr.length;i++){
//                        totTP+=truePosCoarse[i];
//                        totTPFP+=truePosCoarse[i]+falsePosCoarse[i];
//                        totTPFN+=truePosCoarse[i]+falseNegCoarse[i];
//                    }
//                    if (totTP>0){
//                        System.out.println("totales: totTP:"+totTP+" totTPFP:"+totTPFP+" totTPFN:"+totTPFN);
//                        double fmicroCoarse=0;
//                        double purCoarse=(totTP>0)?((double)totTP/totTPFP):0;
//                        double reCoarse=(totTP>0)?((double)totTP/totTPFN):0;
//                        fmicroCoarse=(purCoarse*reCoarse>0)?((2.0*purCoarse*reCoarse)/(purCoarse+reCoarse)):0;
//                        System.out.println("Precision Coarse= "+ purCoarse+ " Recall= "+reCoarse);
//                        System.out.println("F1 micro Coarse="+fmicroCoarse);
//                        //print the macro fmeasure: av. fmeasure for each class then get av. between fmeasures
//                        double[]fsmacroCoarse= new double[classesCoarse];
//                        double[]fsmacroCore= new double[classesCore];
//                        double[]puritiesCoarse= new double[classesCoarse];
//                        double[]puritiesCore= new double[classesCore];
//                        double[]recallsCoarse= new double[classesCoarse];
//                        double[]recallsCore= new double[classesCore];
//                        double sumfsmacroCoarse=0;
//                        double sumfsmacroCore=0;
//                        for (int i=0;i<classesCoarse;i++){
//                            if (truePosCoarse[i]>0){
//                            puritiesCoarse[i]=(double)truePosCoarse[i]/(truePosCoarse[i]+falsePosCoarse[i]);
//                            recallsCoarse[i]=(double)truePosCoarse[i]/(truePosCoarse[i]+falseNegCoarse[i]);
//                            if (puritiesCoarse[i]>0 && recallsCoarse[i]>0) fsmacroCoarse[i]=(double)2.0*puritiesCoarse[i]*recallsCoarse[i]/(puritiesCoarse[i]+recallsCoarse[i]);
//                            else fsmacroCoarse[i]=0;
//                            sumfsmacroCoarse+=fsmacroCoarse[i];
//                            }
//                        }
//                        double fmacroCoarse,fmacroCore;
//                        fmacroCoarse=(double)sumfsmacroCoarse/(double)classesCoarse;
//                        fmacroCore=(double)sumfsmacroCore/(double)classesCore; 
//                        System.out.println("F1 macro Core="+fmacroCore);
//                        System.out.println("F1 macro Coarse="+fmacroCoarse);
//                    }
//                }
//		float[] res= new float[4]; //fmeasure for identification (core and coarse) and clasification (core and coarse)
//		float[] precision = new float[4];
//		float[] recall = new float[4];
//		//precision = tp/(tp+fp)
//		precision[0]=(float)tposIdentCoarse/((float)(tposIdentCoarse+fposIdentCoarse));
//		precision[1]=(float)correctCoarse/((float)(goldCoarse));
//		//recall = tp/(tp+fn)
//		recall[0]=(float)tposIdentCoarse/((float)(tposIdentCoarse+fnegIdentCoarse));
//		recall[1]=(float)correctCoarse/((float)(hypCoarse));
//		//fm=
//		for (int i=0;i<res.length;i++){
//			res[i]=(2*precision[i]*recall[i])/(precision[i]+recall[i]);
//		}
//		calcErrorsUnsupSRL(grecs, grefs,print);
//		//System.out.println("acc counts: ntot="+ntot+" nok="+nokLAS+" nsub="+nsub+" nhead="+nhead+" ndel="+ndel+" nins="+nins);
//		return res;
//	}
        
        
	public static Double[] calcErrorsFrenchSRL(List<DetGraph> grecs, List<DetGraph> grefs, boolean print) {
		Double[] res= new Double[3]; 
		int totalcoverageinallverbs=0;
		double cero=0;
		//get the list of verbs which are predicates
		for (int i=0;i<grefs.size();i++) {
			DetGraph gref = grefs.get(i);
			for (int recmot=0;recmot<gref.getNbMots();recmot++) {
                            List<Integer> childrenP=gref.getFils(recmot);
                            if (childrenP.size()>0){
					predicates.add(gref.getMot(recmot).getLemme());
					for (int c:childrenP){
						//get dep zith pred
						String lref= getRoleLabel(gref,c,recmot);
                                                lref=getRolePropBank(lref);
						int s=goldRoles.size();
						if (!goldRoles.containsKey(lref)) goldRoles.put(lref, s); 
					}
                            }
				
			}
			//extract the found roles
			DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
			for (int recmot=0;recmot<grec.getNbMots();recmot++) {
				List<Integer> childrenP=grec.getFils(recmot);
				if (childrenP.size()>0) {
					for (int c:childrenP){
						//get dep zith pred
						String lrec=getRoleLabel(grec, c, recmot);
						//add to the dictionary of gold roles;
						if (!foundRoles.containsKey(lrec)) foundRoles.put(lrec, foundRoles.size()); 
					}
				}
			}
			//add none? 
			if (!foundRoles.containsKey("NONE")) foundRoles.put("NONE", foundRoles.size());
                        if (!goldRoles.containsKey("NONE")) goldRoles.put("NONE", goldRoles.size());
		}

		String[] arrpred=(String[])predicates.toArray(new String[predicates.size()]);
		//for purity and collocation
		//keeps the intersections, where the first link represents the gold role and the second the sample id,
		//so totalIntersecs[0][0]=totalA0and0
		ArrayList<ArrayList<Double>> totalIntersecs;
		ArrayList<Double>intersecsSamples;
		//keeps the number of gold frames
		ArrayList<Double> totalGold;
		//to calculate the maximum (max_j(Gj interseccion Ci)
		ArrayList<Double> maxjs;
		//to calculate the maximum (max_i(Gj interseccion Ci)
		ArrayList<Double> maxis;
		//the purity per verb
		Double[]purity=new Double[arrpred.length];
		//collocation per verb
		Double[]collocation=new Double[arrpred.length];
		//f1 measure per verb...
		Double[]f1=new Double[arrpred.length];
		Integer[]coverageperverb=new Integer[arrpred.length];

		for (int verbid=0;verbid<arrpred.length;verbid++){
			String lem=arrpred[verbid];
			//evaluate the purity and collocation for this verb
			//initialize structures
			int perVerbCoverage=0;
			totalGold=new ArrayList<Double>();
			totalIntersecs=new ArrayList<ArrayList<Double>>();
			for (int i=0;i<goldRoles.keySet().size();i++){
				totalGold.add(cero);
			}
			for (int i=0;i<goldRoles.size();i++){
				intersecsSamples=new ArrayList<Double>();
				for (int j=0;j<foundRoles.size();j++){ //one extra to represent the null= when no role is sample or is a role not in the list
					intersecsSamples.add(cero);
				}
				totalIntersecs.add(intersecsSamples);
			}
			//go through the arguments found in the gold for the evaluation part of the data
			for (int i=0;i<grefs.size();i++) {
				DetGraph gref = grefs.get(i);
				DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
				for (int recmot=0;recmot<gref.getNbMots();recmot++) {
					if (lem.equals(gref.getMot(recmot).getLemme())){ 
						//for each argument
						List<Integer> childrenP=gref.getFils(recmot);
						for (int c:childrenP){
							perVerbCoverage++;
							totalcoverageinallverbs++;
							//get dep zith pred
							String lref= getRoleLabel(gref,c,recmot);
                                                        lref=getRolePropBank(lref);
							int gind=goldRoles.get(lref);
							//add to the counts..
							totalGold.set(gind,totalGold.get(gind)+1);
							//if the argument was identified by the rules, fill the corresponding intersections,
							String lrec= getRoleLabel(grec,c,recmot);
							int gindf=foundRoles.get(lrec);
							//add to the counts.. 
							//get the entry for the gold, and increment in that entry the entry for the sample
							intersecsSamples=totalIntersecs.get(gind);
							intersecsSamples.set(gindf, intersecsSamples.get(gindf)+1);
							totalIntersecs.set(gind, intersecsSamples);
						}
					}                    
				}               
			}
			//purity=Sum_i(max_j(Gj interseccion Ci))
			maxjs=new ArrayList<Double>();
			double verbpurity=0;
			for (int i=0;i<foundRoles.size();i++){
				maxjs.add(cero);
				for (int j=0;j<goldRoles.size();j++){
					if (totalIntersecs.get(j).get(i)>maxjs.get(i)) maxjs.set(i, totalIntersecs.get(j).get(i));
				}
				verbpurity+=maxjs.get(i);
			}
			verbpurity=verbpurity/perVerbCoverage;

			//collocation=Sum_j(max_i(Gj interseccion Ci))
					maxis=new ArrayList<Double>();
			double verbcollocation=0;
			for (int i=0;i<goldRoles.size();i++){
				maxis.add(cero);
				for (int j=0;j<foundRoles.size();j++){
					if (totalIntersecs.get(i).get(j)>maxis.get(i)) maxis.set(i, totalIntersecs.get(i).get(j));
				}
				verbcollocation+=maxis.get(i);
			}
			verbcollocation=verbcollocation/perVerbCoverage;

			double verbf1=(2*verbpurity*verbcollocation)/(verbpurity+verbcollocation);

			purity[verbid]=verbpurity;
			collocation[verbid]=verbcollocation;
			f1[verbid]=verbf1;
			coverageperverb[verbid]=perVerbCoverage;

		} 
		//the average is weighted (micro average see Lapata)
		double globalPurity=0;
		double globalColl=0;
		double wglobalPurity=0;
		double wglobalColl=0;

		double microAvF1=0;
		double macroAvF1=0;
		double totalVerbscovered=0;

		for (int indv=0;indv<purity.length;indv++){
			//get the proportion of coverage of the verb:
			double weigth=(double)coverageperverb[indv]/(double)totalcoverageinallverbs;
			//            System.out.println("purity:"+purity[indv]+" weiht: "+ weigth);
			if (coverageperverb[indv]>0){
				totalVerbscovered+=coverageperverb[indv];
				wglobalColl+=collocation[indv]*weigth;
				wglobalPurity+=purity[indv]*weigth;

				globalColl+=collocation[indv];
				globalPurity+=purity[indv];
				macroAvF1+=f1[indv];
			}
		}
		microAvF1=(2*wglobalPurity*wglobalColl)/(wglobalPurity+wglobalColl);
                if (print){
                    System.out.println("Weighted purity:"+wglobalPurity);
                    System.out.println("Weighted collocation:"+wglobalColl);
                    System.out.println("Weighted Micro Av.F1: "+ microAvF1);
                }
		res[0]=wglobalPurity;//purity
		res[1]=wglobalColl;//colocation
		res[2]=microAvF1;//fmeasure
		return res;
	} 
        
        
        
        
 /*******************************************************************French***************************************************/
        
        
	@Deprecated
	public static float[] calcErrorsOld(List<DetGraph> grecs, List<DetGraph> grefs) {
		errors = new ArrayList<int[]>();
		int[] totalCoreRolesRef= new int[coreRoles.size()];
		int totalAMRef=0;
		int[]tpperCoreRoleLab=new int[coreRoles.size()];
		int tpperAMLab=0;
		int[]fpperCoreRoleLab=new int[coreRoles.size()];
		int fpperAMLab=0;
		int[] tnperCoreRoleLab=new int[coreRoles.size()];
		int tnperAMLab=0;
		int[]fnperCoreRoleLab=new int[coreRoles.size()];
		int fnperAMLab=0;
		//for identification
		int tposIdentCore=0;
				int tposIdentCoarse=0;
				int tnegIdentCore=0;
				int tnegIdentCoarse=0;
				int fposIdentCore=0;
				int fposIdentCoarse=0;
				int fnegIdentCore=0;
				int fnegIdentCoarse=0;
				int tposClasCore=0;
				int tposClasCoarse=0;
				int tnegClasCore=0;
				int tnegClasCoarse=0;
				int fposClasCore=0;
				int fposClasCoarse=0;
				int fnegClasCore=0;
				int fnegClasCoarse=0;

				//initialize all arrays
				Arrays.fill(tpperCoreRoleLab, 0);
				Arrays.fill(totalCoreRolesRef, 0);
				Arrays.fill(fpperCoreRoleLab, 0);
				Arrays.fill(tnperCoreRoleLab, 0);
				Arrays.fill(fnperCoreRoleLab, 0);
				//for each sentence
				for (int i=0;i<grefs.size();i++) {
					ArrayList<Integer> errwords = new ArrayList<Integer>();
					DetGraph gref = grefs.get(i);
					DetGraph grec = grecs.get(i).relatedGraphs.get(0);//get the srl level from the link with the syntactic
					for (int recmot=0;recmot<grec.getNbMots();recmot++) {
						int drec = grec.getDep(recmot);
						int dref = gref.getDep(recmot);
						if (drec<0&&dref>=0) {
							//if is an argument to consider:
							String lref=gref.getDepLabel(dref);
							if (lref.startsWith("AM")) lref="AM";
							if (coarseRoles.contains(lref)){
								errwords.add(i);
								if (lref.equals("AM")) {
									//false negative identification
									fnegIdentCoarse++;
									totalAMRef++;
									//false negative clasification
									fnperAMLab++;
									fnegClasCore++;
									fnegClasCoarse++;
								}else{
									//false negative identification
									int ind=coreRoles.indexOf(lref);
									fnegIdentCore++;
									fnegIdentCoarse++;
									totalCoreRolesRef[ind]=totalCoreRolesRef[ind]+1;
									//false negative clasification
									fnperCoreRoleLab[ind]=fnperCoreRoleLab[ind]+1;
									fnegClasCoarse++;
								}

							}
						} else if (drec>=0&&dref<0) {
							//if is an argument to consider:
							String lrec=grec.getDepLabel(drec);
							if (lrec.startsWith("AM")) lrec="AM";
							if (coarseRoles.contains(lrec)){
								errwords.add(i);

								if (lrec.equals("AM")) {
									//false positive identification
									fposIdentCoarse++;
									totalAMRef++;
									//false negative clasification
									fpperAMLab++;
									fposClasCoarse++;
								}else{
									//false positive identification
									int ind=coreRoles.indexOf(lrec);
									fposIdentCore++;
									fposIdentCoarse++;
									totalCoreRolesRef[ind]=totalCoreRolesRef[ind]+1;
									//false negative clasification
									fpperCoreRoleLab[ind]=fpperCoreRoleLab[ind]+1;
									fposClasCore++;
									fposClasCoarse++;
								}
							}
						} else if (drec<0&&dref<0) {
							//true negative for identification and clasification
							tnegClasCoarse++;
							tnegClasCore++;
							tnegIdentCoarse++;
							tnegIdentCore++;
							//todo see if it's ok to inc the value for every role
							tnperAMLab++;
							for (int in=0;in<tnperCoreRoleLab.length;in++){
								tnperCoreRoleLab[in]=tnperCoreRoleLab[in]+1;
							}

						} else if (drec>=0&&dref>=0) {
							//if is an argument to consider:
							String lref=gref.getDepLabel(dref);
							String lrec=grec.getDepLabel(drec);
							int indf=coreRoles.indexOf(lref);
							int indc=coreRoles.indexOf(lrec);
							if (lref.startsWith("AM")) lref="AM";
							if (coarseRoles.contains(lref)){
								//true positive for identification
								if (lref.equals("AM")){
									tposIdentCoarse++;
									//see clasification
									if (lrec.equals("AM")){
										tpperAMLab++;
										tposClasCoarse++;
									}else{
										fpperAMLab++;
										fpperCoreRoleLab[indc]=fpperCoreRoleLab[indc]+1;
										fposClasCore++;
										fposClasCoarse++;
									}
								}else{ //the ref is a core role
									if (lrec.equals(lref)){
										//tp for the role
										tposIdentCoarse++;
										tposIdentCore++;
									}else{ //the rec is not the same
										//for the ref role, it's false negative
										fnegClasCore++;
										fnegClasCoarse++;
										fnperCoreRoleLab[indf]=fnperCoreRoleLab[indf]+1;
										if (lrec.equals("AM")){
											fposClasCoarse++;
											fpperAMLab++;
										}else{
											fposClasCore++;
											fposClasCoarse++;
											fpperCoreRoleLab[indc]=fpperCoreRoleLab[indc]+1;
										}
									}
								}
							}
						}
					}

					for (int w : errwords) {
						int[] newerr = {1,i,w};
						errors.add(newerr);
					}

				}
				//p=tp/(tp+fp)
				float[] precisionPerRole = new float[6];
				//p=tp/(tp+fn)
				float[] recallPerRole = new float[6];
				float[] fmesPerRole = new float[6];
				//am
				precisionPerRole[5]= (float)tpperAMLab/((float)tpperAMLab+fpperAMLab);
				recallPerRole[5]= (float)tpperAMLab/((float)tpperAMLab+fnperAMLab);
				fmesPerRole[5]=(2*precisionPerRole[5]*recallPerRole[5])/(precisionPerRole[5]+recallPerRole[5]);
				//core


				for (int i=0;i<coreRoles.size();i++){
					precisionPerRole[i]= (float)tpperCoreRoleLab[i]/((float)tpperCoreRoleLab[i]+fpperCoreRoleLab[i]);
					recallPerRole[i]= (float)tpperCoreRoleLab[i]/((float)tpperCoreRoleLab[i]+fnperCoreRoleLab[i]);
					fmesPerRole[i]=(2*precisionPerRole[i]*recallPerRole[i])/(precisionPerRole[i]+recallPerRole[i]);
					//                System.out.println("Precision "+coreRoles.get(i)+" = "+precisionPerRole[i]);
					//                System.out.println("Recall "+coreRoles.get(i)+" = "+recallPerRole[i]);
					//                System.out.println("F1 "+coreRoles.get(i)+" = "+fmesPerRole[i]);
				}
				//            System.out.println("Precision AM= "+precisionPerRole[5]);
				//            System.out.println("Recall  AM= "+recallPerRole[5]);
				//            System.out.println("F1 AM= "+fmesPerRole[5]);

				System.out.println("\nSRL:");
				System.out.println("Core: ID : TP="+tposIdentCore+" TN="+tnegIdentCore+" FP="+fposIdentCore+" FN="+fnegIdentCore);
				System.out.println("Coarse: ID : TP="+tposIdentCoarse+" TN="+tnegIdentCoarse+" FP="+fposIdentCoarse+" FN="+fnegIdentCoarse);
				System.out.println("Core: LAB : TP="+tposClasCoarse+" TN="+tnegClasCore+" FP="+fposClasCore+" FN="+fnegClasCore);
				System.out.println("Coarse: LAB : TP="+tposClasCoarse+" TN="+tnegClasCoarse+" FP="+fposClasCoarse+" FN="+fnegClasCoarse);

				//System.out.println("acc counts: ntot="+ntot+" nok="+nokLAS+" nsub="+nsub+" nhead="+nhead+" ndel="+ndel+" nins="+nins);
				return fmesPerRole;
	}

}