package jsafran.ponctuation;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;
import jsafran.POStagger;

public class PonctFeatures {

	public static final String noponct = "nopct";
	final static String[] allponcts = {".",",",";",":","!","?","...","\"","/","(",")"};
	final static String[] names = {"point","virg","ptvirg","dblpt","excl","inter","susp","guill","divis","parenl","parend"};
	
	static String getPonctSymb(String rec) {
		if (rec.equals(noponct)) return "";
		else return ".";
	}
	static String getPonctClass(String symb) {
		if (symb.equals(".") || symb.equals("!") || symb.equals("?")) {
			return "EOS";
		}
		return noponct;
	}
	
	String decapitalise(String mot) {
		if (mot.length()<=1) return mot.toLowerCase();
		if (Character.isUpperCase(mot.charAt(0)) && !Character.isUpperCase(mot.charAt(1)))
			return Character.toLowerCase(mot.charAt(0))+mot.substring(1);
		return mot;
	}

	/**
	 * calcule les features en supprimant la segmentation des graphes passés en paramètre
	 * et sauvegarde les features dans un fichier.
	 * 
	 * On passe en parametres les marques de ponctuation que l'on doit reconnaitre;
	 * cette liste peut etre vide pour le test. Si elle n'est pas vide, des qu'une de ces
	 * ponctuations est detectee, les features generent la target class associee.
	 * Les autres ponctuations ne sont pas prises en compte
	 * 
	 * retourne la liste des indices des mots associes a chaque feature
	 * 
	 * @param fout
	 * @param graphs
	 */
	public static List<Integer>[] saveFeats(PrintWriter fout, List<DetGraph> graphs) {
		String mot = null, pos=null;
		// la derniere col = target class
		ArrayList<String[]> feats = new ArrayList<String[]>();
		// indexes des mots associes a chaque feature-vect
		ArrayList<Integer> wordsIndexes = new ArrayList<Integer>();
		// indexes des graphes associes a chaque feature-vect
		ArrayList<Integer> graphsIndexes = new ArrayList<Integer>();
		for (int gi=0;gi<graphs.size();gi++) {
			DetGraph g = graphs.get(gi);
			for (int i=0;i<g.getNbMots();i++) {
				mot = g.getMot(i).getForme();
				pos = g.getMot(i).getPOS();
				if (POStagger.isPonct(pos)) {
					if (feats.size()>0) {
						String[] fs = feats.get(feats.size()-1);
						if (fs[fs.length-1].equals(noponct)) {
							fs[fs.length-1] = getPonctClass(mot);
						}
					}
				} else {
					String[] fs = new String[3];
					fs[0] = mot; fs[1] = pos;
					fs[fs.length-1] = noponct;
					feats.add(fs);
					wordsIndexes.add(i);
					graphsIndexes.add(gi);
				}
				while (feats.size()>1) {
					String[] fs = feats.remove(0);
					for (int j=0;j<fs.length-1;j++) {
						String s = fs[j];
						fout.print(s+"\t");
					}
					fout.println(fs[fs.length-1]);
				}
			}
			while (feats.size()>0) {
				String[] fs = feats.remove(0);
				for (int j=0;j<fs.length-1;j++) {
					String s = fs[j];
					fout.print(s+"\t");
				}
				fout.println(fs[fs.length-1]);
			}
		}
		List[] indexes = {graphsIndexes,wordsIndexes};
		return indexes;
	}

/*	
	public static String getPonctSymb(String name) {
		for (int i=0;i<names.length;i++) {
			if (name.equals(names[i])) return allponcts[i];
		}
		return noponct;
	}
	public static String getPonctName(String ponct) {
		for (int i=0;i<allponcts.length;i++) {
			if (ponct.equals(allponcts[i])) return names[i];
		}
		return noponct;
	}
	*/
}
