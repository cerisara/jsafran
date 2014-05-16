/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jsafran.parsing.unsup.hillclimb;

import java.util.ArrayList;
import java.util.Arrays;
import jsafran.Dep;
import jsafran.DetGraph;

/**
 *
 * @author ale
 */
public class FrRules {
	

	public ArrayList<OneRule> rs = new ArrayList<OneRule>();
	public FrRules() {
		rs.add(new AdvUnderSuj());
		rs.add(new DbleNoms());
		rs.add(new Det());
		rs.add(new ObjUnderSuj());
		rs.add(new PObjUnderSuj());
		rs.add(new Se());
		rs.add(new SujNom());
		rs.add(new SujPro());
		rs.add(new YV());
	}
	
	// ==================================================
	// var tmp used in rules
    	final int[] maxpos = new int[1000];
	final int[] maxpos2 = new int[maxpos.length];
	// utility
	public static boolean isIn(String x, String[] list) {
		for (int i=0;i<list.length;i++)
			if (list[i].equals(x)) return true;
		return false;
	}

        public ArrayList<OneRule> getAllRules(){
            ArrayList<OneRule>handrules1=new ArrayList<OneRule>();
		handrules1.add(new SujPro());
		handrules1.add(new SujNom());
		handrules1.add(new Det());
		handrules1.add(new YV());
		handrules1.add(new Se()); // TODO "se" = dummy
		handrules1.add(new AdvUnderSuj());
		handrules1.add(new DbleNoms());
		handrules1.add(new ObjUnderSuj()); // TODO: "nous" "me" "te" "se" "vous" = REF
		handrules1.add(new PObjUnderSuj());
                return handrules1;
            
        }
        /*********************************************************************************************/
    	class SujPro implements OneRule {
		final String[] propers = {"c'","ça","ceci","cela","elle","il","ils","je","j'","nous","on","tu","vous"};

                @Override
                public int[] getApplicable(DetGraph g) {
                    int p=0;
                    for (int i=0;i<g.getNbMots();i++) {
                            String f=g.getMot(i).getForme().toLowerCase();
                            if (isIn(f, propers)) {
                                    int d=g.getDep(i);
                                    if (d<0)
                                            for (int j=i+1;j<g.getNbMots();j++)
                                                    if (g.getMot(j).getPOS().startsWith("V"))
//                                                            maxpos[p++]=i;
                                                        maxpos[p++]=i*1000+j;
                            }
                    }
                    return Arrays.copyOf(maxpos, p);
                }

                @Override
                public void apply(DetGraph g, int hashCode) {
			int pos = hashCode/1000;
			int j = hashCode%1000;
                        int d=g.getDep(pos);
                        if (d>=0) g.removeDep(d);
                        g.ajoutDep("SUJ", pos, j);
                }
	}
        
	class SujNom implements OneRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			int p=0;
			for (int i=0;i<g.getNbMots();i++) {
				if (g.getMot(i).getPOS().charAt(0)=='N') {
					int d=g.getDep(i);
					if (d<0)
						for (int j=i+1;j<g.getNbMots();j++)
							if (g.getMot(j).getPOS().startsWith("V"))
								maxpos[p++]=i*1000+j;
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int j = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("SUJ", pos, j);
		}
	}
        /*********************************************************************************************/
	class Det implements OneRule {
		final String[] dets = {"aucun","aucune","ce","ces","cet","cette","des","l'","la","le","les","leur","leurs","ma","mes","mon","nos","notre","plusieurs","quel","quelle","quels","quelles","quelque","sa","ses","son","ta","tes","ton","tout","toute","un","une","vos","votre"};
		@Override
		public int[] getApplicable(DetGraph g) {
			int p=0;
			for (int i=0;i<g.getNbMots();i++) {
				String f=g.getMot(i).getForme().toLowerCase();
				if (isIn(f,dets)) {
					int d=g.getDep(i);
					if (d<0)
						for (int j=i+1;j<g.getNbMots()&&j<i+5;j++)
							if (g.getMot(j).getPOS().startsWith("N"))
								maxpos[p++]=i*1000+j;
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int j = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("DET", pos, j);
		}
	}
        /*********************************************************************************************/
	class YV implements OneRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			int p=0;
			for (int i=0;i<g.getNbMots();i++) {
				String f=g.getMot(i).getForme();
				if (f.equals("y")) {
					int d=g.getDep(i);
					if (d<0)
						for (int j=i+1;j<g.getNbMots()&&j<i+5;j++)
							if (g.getMot(j).getPOS().startsWith("V"))
								maxpos[p++]=i*1000+j;
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int j = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("DUMMY", pos, j);
		}
	}

        
        /*********************************************************************************************/
	class Se implements OneRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			int p=0;
			for (int i=0;i<g.getNbMots();i++) {
				String f=g.getMot(i).getForme();
				if (f.equals("s'")||f.equals("se")) {
					int d=g.getDep(i);
					if (d<0)
						for (int j=i+1;j<g.getNbMots()&&j<i+5;j++)
							if (g.getMot(j).getPOS().startsWith("V"))
								maxpos[p++]=i*1000+j;
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int j = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("REF", pos, j);
		}
	}

    
        /*********************************************************************************************/
	class AdvUnderSuj implements OneRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			final int sujt = Dep.getType("SUJ");
			int p=0;
			for (int i=0;i<g.deps.size();i++) {
				if (g.deps.get(i).type==sujt) {
					int gov=g.deps.get(i).gov.getIndexInUtt()-1;
					int head=g.deps.get(i).head.getIndexInUtt()-1;
					if (gov<head) {
						for (int j=gov+1;j<head;j++) {
							if (g.getMot(j).getPOS().startsWith("ADV")) {
								int d=g.getDep(j);
								if (d<0) {
									// il reste des ADV non liés sous le SUJ
									maxpos[p++]=j*1000+head;
									break;
								}
							}
						}
					}
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int j = hashCode/1000;
                    int head = hashCode%1000;
                    int d=g.getDep(j);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("MOD", j, head);
		}
	}


        /*********************************************************************************************/
	class DbleNoms implements OneRule {
		@Override
		public int[] getApplicable(DetGraph g) {
			int p=0;
			for (int i=0;i<g.getNbMots()-1;i++) {
				if (g.getMot(i).getPOS().equals("NAM")&&g.getMot(i+1).getPOS().equals("NAM")) {
					int d=g.getDep(i+1);
					if (d<0) {
						maxpos[p++]=(i+1)*1000+i;
					}
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int j = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("MOD", pos, j);
		}
	}


        /*********************************************************************************************/
	class ObjUnderSuj implements OneRule {
		final String[] cod={"l'","la","le","les","m'","me","nous","t'","te","vous"};
		@Override
		public int[] getApplicable(DetGraph g) {
			final int sujt = Dep.getType("SUJ");
			int p=0;
			for (int i=0;i<g.deps.size();i++) {
				if (g.deps.get(i).type==sujt) {
					int gov=g.deps.get(i).gov.getIndexInUtt()-1;
					int head=g.deps.get(i).head.getIndexInUtt()-1;
					if (gov<head) {
						for (int j=gov+1;j<head;j++) {
							if (isIn(g.getMot(j).getForme(),cod)) {
								int d=g.getDep(j);
								if (d<0) {
									// il reste des PRO non liés sous le SUJ
									maxpos[p++]=j*1000+head;
									break;
								}
							}
						}
					}
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int head = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("OBJ", pos,head );
                }
	}
        
        /*********************************************************************************************/
	class PObjUnderSuj implements OneRule {
		final String[] coi={"lui","leur","m'","me","nous","t'","te","vous"};
		@Override
		public int[] getApplicable(DetGraph g) {
			final int sujt = Dep.getType("SUJ");
			int p=0;
			for (int i=0;i<g.deps.size();i++) {
				if (g.deps.get(i).type==sujt) {
					int gov=g.deps.get(i).gov.getIndexInUtt()-1;
					int head=g.deps.get(i).head.getIndexInUtt()-1;
					if (gov<head) {
						for (int j=gov+1;j<head;j++) {
							if (isIn(g.getMot(j).getForme(),coi)) {
								int d=g.getDep(j);
								if (d<0) {
									// il reste des PRO non liés sous le SUJ
									maxpos[p++]=j*1000+head;
									break;
								}
							}
						}
					}
				}
			}
			return Arrays.copyOf(maxpos, p);
		}
		@Override
		public void apply(DetGraph g, int hashCode) {
                    int pos = hashCode/1000;
                    int head = hashCode%1000;
                    int d=g.getDep(pos);
                    if (d>=0) g.removeDep(d);
                    g.ajoutDep("POBJ", pos, head);
		}
	}

        /*********************************************************************************************/
//	class Atts implements OneRule {
//		final String[] verbesetat = {"être","paraître","devenir"};
//		@Override
//		public int[] getApplicable(DetGraph g) {
//			int p=0;
//			for (int i=0;i<g.getNbMots();i++) {
//				// TODO: utiliser plutot une liste de toutes les formes fléchies
//				String l = g.getMot(i).getLemme();
//				if (isIn(l, verbesetat)) {
//					for (int j=i+1;j<g.getNbMots();j++) {
//						String pos = g.getMot(j).getPOS();
//						if (pos.charAt(0)=='V') break;
//						if (pos.startsWith("ADJ") || pos.startsWith("NOM")) {
//							int d=g.getDep(j);
//							if (d<0) {
//								maxpos[p++]=i;
//								maxpos2[i]=j;
//							}
//							break;
//						}
//					}
//				}
//			}
//			return Arrays.copyOf(maxpos, p);
//		}
//
//		@Override
//		public void apply(DetGraph g, int hashCode) {
//			g.ajoutDep("ATTS", maxpos2[pos], pos);
//			return true;
//		}
//		
//	}
        
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        /*********************************************************************************************/
        
}
