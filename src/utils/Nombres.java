package utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jsafran.DetGraph;
import jsafran.GraphIO;

/**
 * Identifie les nombres et les entités nommées associées
 * 
 * 
 * @author cerisara
 *
 */
public class Nombres {
	static enum numcat {entier, reel, acronyme, bizarre, partofnb, nan};
	
	static final String[] nbs0 = {"un","deux","trois","quatre","cinq","six","sept","huit","neuf","dix","onze","douze","treize","quatorze","quinze","seize","vingt",
			"trente","quarante","cinquante","soixante","cent","mille","million","milliard",
			"nonente","moins"};
	
	static int isin(String[] tab, String tok) {
		String t = tok.trim().toLowerCase();
		for (int i=0;i<tab.length;i++)
			if (tab[i].equals(t)) return i;
		return -1;
	}
	
	// test si on a un nombre ecrit en toute lettre
	static boolean testPart2(String tok) {
		int j;
		if ((j=isin(nbs0,tok))>=0) {
			return true;
		}
		return false;
	}
	static boolean testPart(String tok) {
		String[] toks = tok.split("-_");
		if (toks.length<1) return false;
		if (toks.length==1) return testPart2(tok);
		for (String s : toks)
			if (!testPart2(s)) return false;
		return true;
	}
	
	numcat[] types=null;
	float[]  vals=null;
	
	// je suppose que les tokens sont séparés par des espaces
	void parse(String[] tokens) {
		types = new numcat[tokens.length];
		vals = new float[tokens.length];
		Arrays.fill(types, numcat.nan);
		Arrays.fill(vals,Float.NaN);
		for (int i=0;i<tokens.length;i++) {
			if (Pattern.matches("\\d+", tokens[i])) {
				types[i]=numcat.entier;
				vals[i]=Float.parseFloat(tokens[i]);
				// System.out.println("entier "+tokens[i]);
			} else if (Pattern.matches("\\d+[.,]\\d+", tokens[i])) {
				types[i]=numcat.reel;
				vals[i]=Float.parseFloat(tokens[i]);
				// System.out.println("réel "+tokens[i]);
			} else if (Pattern.matches("[A-Za-z]+.*\\d+.*", tokens[i])) {
				types[i]=numcat.acronyme;
				// System.out.println("acronyme "+tokens[i]);
			} else if (Pattern.matches(".*\\d+.*", tokens[i])) {
				types[i]=numcat.bizarre;
				// System.out.println("truc bizarre "+tokens[i]);
			} else if (testPart(tokens[i])) {
				types[i]=numcat.partofnb;
			}
		}
		// traiter les cas particuliers (un, virgule) en fonction du contexte
		casparticuliers(tokens,types);
		parseNombresEnToutesLettres(tokens,types, vals);
	}
	
	void parseNombresEnToutesLettres(String[] toks, numcat[] types, float[] vals) {
		// TODO: suite de chiffres ou un seul chiffre ?
		for (int i=0;i<toks.length;i++) {
			if (types[i]==numcat.partofnb) {
				
			}
		}
	}
	void casparticuliers(String[] toks, numcat[] types) {
		for (int i=0;i<toks.length;i++) {
			if (types[i]!=numcat.partofnb) continue;
			String t = toks[i].trim().toLowerCase();
			if (t.equals("un")) {
				if (toks.length==1) continue;
				if (i>0) {
					if (types[i-1]==numcat.partofnb) continue;
				}
				if (i<toks.length) {
					if (types[i+1]==numcat.partofnb) continue;
				}
				// TODO "il y a un enfant" vs. "il y a un enfant, pas deux"
				types[i]=numcat.nan;
			} else if (t.equals("virgule")) {
				if (i>0&&(types[i-1]==numcat.entier||types[i-1]==numcat.partofnb)) continue;
				types[i]=numcat.nan;
			}
		}
	}
	
	public static void main(String args[]) {
		Nombres m = new Nombres();
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("../EN/fred.xml");
		for (DetGraph g : gs) {
			m.parse(g.getMots());
		}
	}
}
