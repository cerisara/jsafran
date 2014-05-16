package jsafran.ponctuation;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.Mot;

/**
 * resegmente les phrases en utilisant la ponctuation
 * 
 * @author xtof
 *
 */
public class UttSegmenter {
	public static void main(String args[]) {
		GraphIO gio =new GraphIO(null);
		System.err.println("loading...");
		List<DetGraph> gs = gio.loadAllGraphs(args[0]);
		segmente(gs);
		gio.save(gs, "output.xml");
	}
	
	/**
	 * version + rapide
	 * Je supprime toutes les deps, mais je garde les groupes !
	 * supprime aussi les sources et les pos dans la source
	 * @param gs
	 */
	public static void segmente(List<DetGraph> gs) {
		final String finalponct = "!?.";
		for (int z=0;z<gs.size();z++) {
			DetGraph g=gs.get(z);
			System.err.println(z+"\t"+gs.size()+" "+g.getNbMots());
			for (int i=0;i<g.getNbMots()-1;i++) {
				if (finalponct.indexOf(g.getMot(i).getForme().charAt(0))>=0) {
					// decoup la phrase
					DetGraph g1 = g.getSubGraph(0,i);
					DetGraph g2 = g.getSubGraph(i + 1);
					if (g1.getNbMots() > 0 && g2.getNbMots() > 0) {
						gs.set(z, g1);
						gs.add(z + 1, g2);
						g=null;
						break;
					}
				}
			}
			if (g==null) {
				// point final, ok
			} else if (g.getNbMots()==0) {
				gs.remove(z);
				--z;
			} else if (finalponct.indexOf(g.getMot(g.getNbMots()-1).getForme().charAt(0))>=0 || z==gs.size()-1) {
				// point final, ok
			} else {
				// il faut joindre
				g.append(gs.get(z+1));
				gs.remove(z+1);
				--z;
			}
		}
	}
	/**
	 * suppose que la ponctuation est séparée des mots: chaque symbole de ponctuation constitue donc un token a part
	 * 
	 * @deprecated cette version est trop longue pour les tres gros corpus
	 * @param gs
	 * @return
	 */
	public static void segmente_v0(List<DetGraph> gs) {
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		DetGraph bigone = new DetGraph();
		bigone.setSource(gs.get(0).getSource());
		HashMap<Integer, URL> word2src = new HashMap<Integer, URL>();
		URL lastSource=null;
		for (int i=0;i<gs.size();i++) {
			if (gs.get(i).getSource()!=null) {
				// on ne conserve que les *changements* de source
				URL newsource = gs.get(i).getSource();
				if (lastSource==null || !newsource.equals(lastSource)) {
					word2src.put(bigone.getNbMots(), newsource);
					lastSource=newsource;
				}
			}
			bigone.append(gs.get(i));
		}
		System.out.println("bigdeb "+bigone.getNbMots());
		int deb=0;
		final String finalponct = "!?.";
		URL prevSource = null;
		for (int i=0;i<bigone.getNbMots();i++) {
			if (word2src.keySet().contains(i)) {
				if (i>0&&deb<i) {
					// on segmente systematiquement aux changements de source
					DetGraph g = bigone.getSubGraph(deb, i-1);
					g.setSource(prevSource);
					res.add(g);
					deb=i;
				}
				prevSource=word2src.get(i);
			}
			String x = bigone.getMot(i).getForme();
			if (x.length()>0) {
				char c = x.charAt(0);
				if (finalponct.indexOf(c)>=0) {
					DetGraph g = bigone.getSubGraph(deb, i);
					g.setSource(prevSource);
					res.add(g);
					deb=i+1;
				}
			}
		}
		if (deb<bigone.getNbMots()) {
			DetGraph g=bigone.getSubGraph(deb, bigone.getNbMots()-1);
			g.setSource(prevSource);
			res.add(g);
		}
		gs.clear();
		gs.addAll(res);
		postcheck(gs);
	}
	
	/**
	 * verifie que les phrases obtenues ne sont pas trop longues: sinon, utilise des heuristiques pour les decouper
	 * @param gs
	 */
	private static void postcheck(List<DetGraph> gs) {
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			if (g.getNbMots()>50) {
				System.out.println("postcheck segmenter "+i+" "+g.getNbMots());
				boolean hasBeenCut = false;
				// découpe avec la ponctuation ;:
				for (int j=g.getNbMots()*1/4;j<g.getNbMots()*3/4;j++) {
					String w=g.getMot(j).getForme();
					char c = w.charAt(0);
					if (c==';'||c==':') {
						DetGraph g1=g.getSubGraph(0, j);
						DetGraph g2=g.getSubGraph(j+1, g.getNbMots()-1);
						gs.set(i, g1);
						gs.add(i+1, g2);
						System.out.println("cut by :; "+g1.getNbMots()+" "+g2.getNbMots());
						hasBeenCut=true; break;
					}
				}
				
				if (!hasBeenCut) {
					// découpe avec les virgules
					for (int j=g.getNbMots()*1/3;j<g.getNbMots()*2/3;j++) {
						String w=g.getMot(j).getForme();
						char c = w.charAt(0);
						if (c==',') {
							DetGraph g1=g.getSubGraph(0, j);
							DetGraph g2=g.getSubGraph(j+1, g.getNbMots()-1);
							gs.set(i, g1);
							gs.add(i+1, g2);
							System.out.println("cut by , "+g1.getNbMots()+" "+g2.getNbMots());
							hasBeenCut=true; break;
						}
					}
				}

				if (!hasBeenCut) {
					// decoupe avec la position dans le fichier source
					int dmax=-1, bestj=0;
					for (int j=g.getNbMots()*1/3;j<g.getNbMots()*2/3;j++) {
						int d = (int)(g.getMot(j).getDebPosInTxt()-g.getMot(j-1).getEndPosInTxt());
						if (d>dmax) {dmax=d; bestj=j;}
					}
					if (dmax>=0) {
						DetGraph g1=g.getSubGraph(0, bestj-1);
						DetGraph g2=g.getSubGraph(bestj, g.getNbMots()-1);
						gs.set(i, g1);
						gs.add(i+1, g2);
						System.out.println("cut by src "+g1.getNbMots()+" "+g2.getNbMots());
						hasBeenCut=true;
					}
				}

				if (!hasBeenCut) {
					// decoupe au pif
					int bestj=g.getNbMots()/2;
					DetGraph g1=g.getSubGraph(0, bestj-1);
					DetGraph g2=g.getSubGraph(bestj, g.getNbMots()-1);
					gs.set(i, g1);
					gs.add(i+1, g2);
					System.out.println("cut by rand "+g1.getNbMots()+" "+g2.getNbMots());
					hasBeenCut=true;
				}
				
				// TODO: reprendre le decoupage qu'il pouvait y avoir initialement ?
				// TODO: utiliser un segmenteur automatique ?
				
				if (hasBeenCut) i--; // je repasse sur la phrase qui vient d'etre reduite
			}
		}
	}
}
