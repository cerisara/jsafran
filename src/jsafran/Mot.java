package jsafran;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Mot implements Serializable {
	private static final long serialVersionUID = 1L;
	private String mot, lemme, postag;
	// further store other fields: more precise POStags, features...
	private HashMap<String,String> otherFields = null;
	// index dans la phrase; les index commencent a 1
	private int index;
	// position dans le "fichier" texte source
	private long posdebInSource=-1, posendInSource=-1;
	
	public Mot(String forme, String lemme, String pos, int idxInUtt) {
		setIndexInUtt(idxInUtt);
		this.mot=forme;
		this.lemme=lemme;
		this.postag=pos;
	}
	public Mot(String forme, String lemme, String pos) {
		this.mot=forme;
		this.lemme=lemme;
		this.postag=pos;
	}
	public Mot(String forme, int idxInUtt) {
		setIndexInUtt(idxInUtt);
		mot=forme;
		lemme=""+forme;
		postag="unk";
	}
	
	private static Mot rootNode = null;
	public static Mot getRootNode() {
	    if (rootNode==null) {
	        final String rt = "ROOT";
	        rootNode=new Mot(rt, rt, rt);
	    }
	    return rootNode;
	}
	
	public void setField(String key, String v) {
	    if (otherFields==null) otherFields=new HashMap<String, String>();
	    otherFields.put(key, v);
	}
	public String getField(String k) {return otherFields==null?null:otherFields.get(k);}
	public String getPOS() {return postag;}
	public String getForme() {return mot;}
	public String getLemme() {return lemme;}
	public void setPOS(String x) {postag=x;}
	public void setForme(String x) {mot=x;}
	public void setlemme(String x) {lemme=x;}
	
	public String toString() {
		return mot;
	}
	public int getIndexInUtt() {
		return index;
	}
	public void setIndexInUtt(int idx) {
		index=idx;
	}
	/**
	 * @param deb
	 * @param end exclus !
	 */
	public void setPosInTxt(long deb, long end) {
		posdebInSource=deb; posendInSource=end;
	}
	public long getDebPosInTxt() {return posdebInSource;}
	public long getEndPosInTxt() {return posendInSource;}
	public Mot clone() {
		Mot m = new Mot(mot,lemme,postag,index);
		m.posdebInSource=posdebInSource;
		m.posendInSource=posendInSource;
		if (otherFields!=null) {
		    m.otherFields = new HashMap<String, String>();
		    for (String k : otherFields.keySet()) {
		        m.otherFields.put(k, otherFields.get(k));
		    }
		}
		return m;
	}
	/**
	 * pour savoir si c'est de la ponctuation, on suppose que le treetagger a
	 * prealablement rempli le champs postag !
	 * @return
	 */
	public boolean ispunct() {
		return postag.startsWith("PON");
	}
}
