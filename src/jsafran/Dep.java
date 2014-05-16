package jsafran;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;

public class Dep implements Serializable {
	private static final long serialVersionUID = 1L;

	static int idco=0;

	public int type, id;
	public Mot gov, head;
	int groupgov=-1, grouphead=-1;
	float score=0f;

	
    public boolean equals(Object dd) {
        Dep d = (Dep)dd;
        return gov==d.gov&&head==d.head&&type==d.type;
    }
	public int hashCode() {
		return type;
	}
	
	// cette liste est completee au fur et a mesure qu'on appelle getType()
	// on peut aussi definir les POStags dans le fichier jsynats.cfg
	public static String[] depnoms = {
		"MOD","MODV", "MODN","COMP", "MODP","MODAdj","MODAdv","APPOS", "OBJ", "POBJ", "SUJ", "ATTS", "DET", "ATTO", "AUX", "CC", "REF", "JUXT","MultiMots","DUMMY"
	};

	public static void loadConfig(InputStream is) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(is));
			ArrayList<String> deps = new ArrayList<String>();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("DEP")) {
					String[] ss = s.split(" ");
					deps.add(ss[1]);
				}
			}
			if (deps.size()>0) {
				Dep.depnoms = new String[0];
				for (int i=0;i<deps.size();i++)
					Dep.addType(deps.get(i));
			}
		} catch (Exception e) {}
	}
	public static int getType(String nom) {
		int i;
		for (i=0;i<depnoms.length;i++)
			if (nom.equals(depnoms[i])) break;
		if (i<depnoms.length) return i;
		else {
//			System.err.println("dep inconnue ! "+nom);
//			return -1;
			return Dep.addType(nom);
		}
	}
	public static int addType(String nom) {
		String[] newdeps = new String[depnoms.length+1];
		int i = depnoms.length;
		System.arraycopy(depnoms, 0, newdeps, 0, i);
		newdeps[i]=""+nom;
		depnoms=newdeps;
		return i;
	}
	public Dep() {
		id=idco++;
	}
	public String toString() {
		if (type<0||type>=depnoms.length) return "?";
		return depnoms[type];
//		return id+depnoms[type]+"_"+score;
	}
	public String toStringDetailed() {
		if (type<0||type>=depnoms.length) return "?";
		return depnoms[type]+" "+gov.getForme()+" "+head.getForme();
	}
	public Mot getHead() {
		return head;
	}
	public Mot getGov() {
		return gov;
	}
	
	// ===================================================
	// nouvelles fonctions pour s'abstraire d'un ensemble de deps particulier
	
	// on liste en fait tous les ensembles de dependances connus
	public boolean isSujet(String depnom) {
		final String[] sujets={"SUJ"};
		return eq(depnom,sujets);
	}
	
	private boolean eq(String unknownDep, String[] ds) {
		for (int i=0;i<ds.length;i++) {
			if (ds[i].equals(unknownDep)) return true;
		}
		return false;
	}
}
