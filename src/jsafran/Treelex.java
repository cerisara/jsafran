package jsafran;

import java.util.ArrayList;

public class Treelex {

	private String lemma;
	private String firstLine;
	private ArrayList<String> listCadreBrut = new ArrayList<String>();
	private ArrayList<String> listCadrePropre = new ArrayList<String>();
	private ArrayList<Dicovalence> listDicovalence = new ArrayList<Dicovalence>();
	private ArrayList<Integer> listPhrase = new ArrayList<Integer>();
	
	public String getLemma() {
		return lemma;
	}
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	public String getFirstLine() {
		return firstLine;
	}
	public void setFirstLine(String firstLine) {
		this.firstLine = firstLine;
	}
	public ArrayList<String> getListCadreBrut() {
		return listCadreBrut;
	}
	public void setListCadreBrut(ArrayList<String> listCadreBrut) {
		this.listCadreBrut = listCadreBrut;
	}
	public ArrayList<String> getListCadrePropre() {
		return listCadrePropre;
	}
	public void setListCadrePropre(ArrayList<String> listCadrePropre) {
		this.listCadrePropre = listCadrePropre;
	}
	public ArrayList<Dicovalence> getListDicovalence() {
		return listDicovalence;
	}
	public void setListDicovalence(ArrayList<Dicovalence> listDicovalence) {
		this.listDicovalence = listDicovalence;
	}
	public void setListPhrase(ArrayList<Integer> listPhrase) {
		this.listPhrase = listPhrase;
	}
	public ArrayList<Integer> getListPhrase() {
		return listPhrase;
	}

	
	
}
