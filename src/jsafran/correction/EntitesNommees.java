package jsafran.correction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * En mode entités nommées, dès qu'on commence à corriger un graphe, ce graphe
 * est sauvegardé pour faire un diff plus tard.
 * Ensuite, le user peut sélectionner un groupe de mots et appuyer sur 'e' pour
 * créer une requete automatique de recherche de cette séquence, et en meme temps
 * que la requete de recherche, un diff est fait entre ce groupe maintenant et
 * avant modification: s'il y a différence, une transformation est également
 * automatiquement générée
 * 
 * @author cerisara
 *
 */
public class EntitesNommees {
	private JSafran js = null;
	public static EntitesNommees corr = null;
	
	public static void mode(JSafran main) {
		if (corr==null) {
			System.out.println("get in named entity mode");
			corr = new EntitesNommees();
			corr.js=main;
		} else {
			System.out.println("get out named entity mode");
			corr=null;
		}
	}

	
	int curgraphSaved = -1;
	DetGraph gsaved = null;
	public void saveGraph(int gidx) {
		if (curgraphSaved==gidx) return;
		gsaved = js.allgraphs.get(gidx).getSubGraph(0);
	}
	
	int prevm1=-1, prevm2=-1;
	public void doit() {
		// détermine lq séquence
		int m1 = js.editword, m2=m1;
		if (js.editword0>=0) m1=js.editword0;
		if (m1>m2) {int c=m1; m1=m2; m2=c;}
		
		// fait le diff avec le graphe avant modification sur cette séquence et génère la transfo
		ArrayList<Integer> changedGov = new ArrayList<Integer>();
		ArrayList<Integer> changedHead = new ArrayList<Integer>();
		ArrayList<String> changedlab = new ArrayList<String>();
		for (int i=m1;i<=m2;i++) {
			int newdep = js.allgraphs.get(js.curgraph).getDep(i);
			if (newdep<0) {
				// non, je ne supprime jamais de deps
				// changedGov.add(i-m1); changedHead.add(-1); changedlab.add(null);
			} else {
				int newhead = js.allgraphs.get(js.curgraph).getHead(newdep);
				if (newhead>=m1&&newhead<=m2) {
					String newlab = js.allgraphs.get(js.curgraph).getDepLabel(newdep);
					changedGov.add(i-m1); changedHead.add(newhead-m1); changedlab.add(newlab);
				}
			}
		}
		
		// calcule la requete de recherche de la sequence de mots
		if (prevm1==m1&&prevm2==m2) {
			// TODO: requete avec dist=1 mettre dans js.pref
		} else {
			// requete avec dist=0
			js.pref="";
			for (int w=m1;w<=m2;w++) js.pref+="f="+js.allgraphs.get(js.curgraph).getMot(w).getForme()+",_,_ ";
			for (int w=m1+1;w<=m2;w++) js.pref+="dep"+(w-m1)+"=dep"+(w-m1-1)+"+1 ";
			prevm1=m1; prevm2=m2;
		}
		
		// calcule la transfo eventuelle
		if (changedGov.size()>0) {
			js.pref+=" => ";
			// supprime toutes les deps a supprimer
			for (int i:changedGov) {
				js.pref+="-dep"+i+" ";
			}
			// ajoute les nouvelles
			for (int i=0;i<changedGov.size();i++) {
				int depi = changedGov.get(i);
				int headi = changedHead.get(i);
				if (headi>=0)
					js.pref+="dep"+depi+",dep"+headi+","+changedlab.get(i);
			}
		}
		
		js.look4tree();
	}
	
	
	/**
	 * repère les séquences de forme identique et projette l'annotation courante dessus
	 * (projection = ajout d'une EN, remplacement/suppression d'une EN de même span
	 * 
	 * @param m
	 * @return
	 */
	public static String getFilterDist0(JSafran m) {
		DetGraph g = m.allgraphs.get(m.curgraph);
		// cherche une sequence selectionnee
		int m1 = m.editword, m2=m.editword;
		if (m.editword0>=0) m1=m.editword0;
		if (m1>m2) {int z=m1; m1=m2; m2=z;}
		
		// cherche les nouveaux groupes pour cette séquence
		ArrayList<ArrayList<Mot>> newgroup = g.groups;
		if (newgroup==null) newgroup = new ArrayList<ArrayList<Mot>>();
		for (int gidx=0;gidx<newgroup.size();gidx++) {
			if (newgroup.get(gidx).get(0).getIndexInUtt()-1==m1 &&
					newgroup.get(gidx).get(0).getIndexInUtt()-1==m2) {
				// on a trouve le nouveau groupe
				// il faut l'ajouter/le remplacer 
				break;
			}
		}
		
		int[] gr = g.getGroups(m.editword);
		
		// on s'arrete au premier groupe différent (le user ne doit modifier qu'un groupe apres l'autre !)
		
//		List<List<Mot>> oldgroup = utt2group.get(m.curgraph);
		
		return null;
	}
}
