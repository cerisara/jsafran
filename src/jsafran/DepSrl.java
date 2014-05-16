package jsafran;

import java.util.ArrayList;

public class DepSrl {
	private int id;
	private int idPhrase;
	private int idVerbe;
	private String form;
	private String lemma;
	private String cat1;
	private String cat2;
	private String fonction;
	private String role;
	
	public DepSrl(int id, int idPhrase, int idVerbe, String form, String lemma,
			String cat1, String cat2, String fonction) {
		super();
		this.id = id;
		this.idPhrase = idPhrase;
		this.idVerbe = idVerbe;
		this.form = form;
		this.lemma = lemma;
		this.cat1 = cat1;
		this.cat2 = mapCat(cat2);
		this.fonction = mapFonct(fonction);
	}

	private String mapFonct(String fonction) {
		// TODO Auto-generated method stub)		
		if (fonction.equals("suj")){
			fonction="SUJ";
		}
		else if (fonction.equals("obj") || fonction.equals("obj1")){
			fonction="OBJ";
		}
		else if (fonction.equals("de_obj")){
			fonction="DE-OBJ";
		}
		else if (fonction.equals("a_obj")){
			fonction="A-OBJ";
		}
		else if (fonction.equals("p_obj")){
			fonction="P-OBJ";
		}
		else if (fonction.equals("ats")){
			fonction="ATS";
		}
		else if (fonction.equals("ato")){
			fonction="ATO";
		}
			
		return fonction;
	}

	private String mapCat(String cat) {
		// TODO Auto-generated method stub
		if (cat.equals("N")){
			cat="NP";
		}
		else if (cat.equals("C")){
			cat="Ssub";
		}
		else if (cat.equals("P")){
			cat="PP";
		}
		else if (cat.equals("VINF")){
			cat="VPinf";
		}
		else if (cat.equals("ADV")){
			cat="AdP";
		}
		else if (cat.equals("VPR")){
			cat="VPpart";
		}
		else if (cat.equals("A")){
			cat="AP";
		}
		return cat;
	}
	
	

	public void treatDep(ArrayList<String[]> listMot) {
		// TODO Auto-generated method stub
		//Ici on traite les arguments de la dépendance
		boolean applique = false; //indique la la regle s'est appliqué
		String lemmaVerbe = listMot.get(idVerbe)[2];
		if (this.fonction.equals("SUJ")){ // traitement du cas SUJ
			applique = regle_3_1_1(lemmaVerbe);
			if (!applique){
				applique = regle_3_1_2();
			}
			if (!applique){
				applique = regle_3_1_3();
			}
			if (!applique){
				applique = regle_3_1_4();
			}
			if (!applique){
				applique = regle_3_1_5();
			}
			if (!applique){
				applique = regle_3_1_rien();
			}
		} else if (this.fonction.equals("OBJ")){ // traitement du cas OBJ 
			applique = regle_3_2_1();
			if (!applique){
				applique = regle_3_2_2();
			}
			if (!applique){
				applique = regle_3_2_3();
			}
			if (!applique){
				applique = regle_3_2_4();
			}
			if (!applique){
				applique = regle_3_2_5(listMot);
			}
			if (!applique){
				applique = regle_3_2_rien();
			}
		} else if (this.fonction.equals("A-OBJ")){ // traitement du cas A-OBJ
			applique = regle_3_3_1(listMot);
			if (!applique){
				applique = regle_3_3_2(listMot);
			}
			if (!applique){
				applique = regle_3_3_3(listMot);
			}
			if (!applique){
				applique = regle_3_3_rien();
			}
		} else if (this.fonction.equals("DE-OBJ")){ // traitement du cas DE-OBJ
			applique = regle_3_4_1(listMot);
			if (!applique){
				applique = regle_3_4_2(listMot);
			}
			if (!applique){
				applique = regle_3_4_3();
			}
			if (!applique){
				applique = regle_3_4_4();
			}
			if (!applique){
				applique = regle_3_4_rien();
			}
		} else if (this.fonction.equals("P-OBJ")){ // traitement du cas P-OBJ
			applique = regle_3_5_1(listMot);
			if (!applique){
				applique = regle_3_5_2(listMot);
			}
			if (!applique){
				applique = regle_3_5_3();
			}
			if (!applique){
				applique = regle_3_5_4();
			}
			if (!applique){
				applique = regle_3_5_rien();
			}
		} else if (this.fonction.equals("ATS")){ // traitement du cas ATS
			applique = regle_3_6_1();
		} else if (this.fonction.equals("ATO")){ // traitement du cas ATO
			applique = regle_3_7_1();
		} else if (this.fonction.equals("aff")){ // traitement des cas refl et obj
			applique = regle_3_8_1();
			if (!applique){
				applique = regle_3_8_2();
			}
			if (!applique){
				applique = regle_3_8_3();
			}
			if (!applique){
				applique = regle_3_8_4();
			}
			if (!applique){
				applique = regle_3_8_rien();
			}
		} else {
//			this.fonction = this.fonction;
//			this.cat2 = this.cat2;
			applique = false;
		}
	}

	/****************************
	 * 
	 * Liste des règles pour le sujet
	 * @param lemmaVerbe 
	 * 
	 ****************************/
	

	private boolean regle_3_1_1(String lemmaVerbe) {
		// TODO Auto-generated method stub
		if (
			lemmaVerbe.equals("falloir")
			)
		{
			this.cat2 = "il";
			return true;
		}
		else
			return false;
	}
	

	private boolean regle_3_1_2() {
		if (
			this.cat2.equals("AP") ||
			this.cat2.equals("NP") ||
			this.cat2.equals("ET") ||
			this.cat2.equals("CL") ||
			this.cat2.equals("D") ||
			this.cat2.equals("PRO") ||
			this.cat2.equals("P+PRO") ||
			this.cat2.equals("P+D") ||
			//ancienne version gardé pour la compatibilité
			this.cat2.equals("A") ||
			this.cat2.equals("N") ||
			this.cat2.equals("NPP") ||
			this.cat2.equals("NC") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("CLS") ||
			this.cat2.equals("PROREL") ||
			this.cat2.equals("PROWH") ||
			this.cat2.equals("CLR") ||
			this.cat2.equals("CS") || //ici c'est peut etre Ssub (CS=>C)
			this.cat2.equals("CLO") ||
			this.cat2.equals("AdP") ||
			this.cat2.equals("ADV") ||
			this.cat2.equals("DET") ||
			this.cat2.equals("CS") ||
			this.cat2.equals("ADJ")
			)
		{
			this.cat2 = "NP";
			return true;
		}
		else
			return false;
	}
	
	private boolean regle_3_1_3() {
		if (this.cat2.equals("P"))
		{
			this.cat2="PP";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_1_4() {
		if (
			this.cat2.equals("C") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("V") ||
			this.cat2.equals("CC")
			)
		{
			this.cat2="Ssub";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_1_5() {
		if (this.cat2.equals("VINF"))
		{
			this.cat2="VPinf";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_1_rien() {
		if (this.cat2.equals("VPP"))
		{
			this.cat2="VPpart";
		}
		else
		{
			//this.cat2 = this.cat2;
		}
		return false;
	}

	/****************************
	 * 
	 * Liste des règles pour l'objet
	 * 
	 ****************************/
	

	private boolean regle_3_2_1() {
		if (
			this.cat2.equals("NP") ||
			this.cat2.equals("ET") ||
			this.cat2.equals("CL") ||
			this.cat2.equals("D") ||
			this.cat2.equals("PRO") ||
			this.cat2.equals("P") ||
			this.cat2.equals("I") ||
			//ancienne version gardé pour la compatibilité
			this.cat2.equals("N") ||
			this.cat2.equals("NPP") ||
			this.cat2.equals("NC") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("PROREL") ||
			this.cat2.equals("CLO") ||
			this.cat2.equals("CLR") ||
			this.cat2.equals("PROWH") ||
			this.cat2.equals("DET") ||
			this.cat2.equals("P+D") ||
			this.cat2.equals("AdP") ||
			this.cat2.equals("ADV") ||
			this.cat2.equals("ADJ")
			)
		{
			this.cat2 = "NP";
			return true;
		}
		else
			return false;
	}
	
	private boolean regle_3_2_2() {
		if (
			this.cat2.equals("A") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("ADJ") ||
			this.cat2.equals("ADJWH")
			)
		{
			this.cat2="AP";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_2_3() {
		if (
			this.cat2.equals("C") ||
			this.cat2.equals("V") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("CS") ||
			this.cat2.equals("CC")
			)
		{
			this.cat2="Ssub";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_2_4() {
		if (
			this.cat2.equals("VPpart") ||
			this.cat2.equals("VPP")
			)
		{
			this.cat2="VPinf";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_2_5(ArrayList<String[]> listMot) {
		if (this.lemma.equals("de") || this.lemma.equals("du")){
			int iter = 0, max = listMot.size();
			boolean end = false;
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[4].equals("VPinf") || listMot.get(iter)[4].equals("VINF"))
					)
				{
					this.cat2 = "VPinf";
					end = true;
				}
				iter++;
			}
			if (end){
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}

	private boolean regle_3_2_rien() {
		if (
			this.cat2.equals("AdP") ||
			this.cat2.equals("ADV")
			)
		{
			this.cat2="AdP";
		}
		else if (this.cat2.equals("VINF"))
		{
			this.cat2="VPinf";
		}
		else
		{
			// this.cat2 = this.cat2;
		}
		return false;
	}

	/****************************
	 * 
	 * Liste des règles pour A-OBJ
	 * 
	 ****************************/
	

	private boolean regle_3_3_1(ArrayList<String[]> listMot) {
		boolean end = false;
		if (this.lemma.equals("à")){
			int iter = 0, max = listMot.size();
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[4].equals("VPinf") || listMot.get(iter)[4].equals("VINF"))
					)
				{
					this.cat2 = "VPinf";
					end = true;
				}
				iter++;
			}
			if (!end){
				// this.cat2 = this.cat2;
				/*if (!listFonctionParasite.contains(subFonct+this.cat2))
					listFonctionParasite.add(subFonct+this.cat2);*/
			}
		}
		return end;
	}
	
	private boolean regle_3_3_2(ArrayList<String[]> listMot) {
		boolean end = false;
		if (this.lemma.equals("à")){
			int iter = 0, max = listMot.size();
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[3].equals("NP") || listMot.get(iter)[3].equals("N"))
					)
				{
					this.cat2 = "PP";
					end = true;
				}
				iter++;
			}
			if (!end){
				// this.cat2 = this.cat2;
				/*if (!listFonctionParasite.contains(subFonct+this.cat2))
					listFonctionParasite.add(subFonct+this.cat2);*/
			}
		}
		return end;
	}

	private boolean regle_3_3_3(ArrayList<String[]> listMot) {
		if (
			this.cat2.equals("CL") ||
			this.cat2.equals("P+PRO") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("P") ||
			this.cat2.equals("P+D") ||
			this.cat2.equals("CLO") ||
			this.cat2.equals("CLR") ||
			this.cat2.equals("PRO")
			)
		{
			this.cat2 = "PP";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_3_rien() {
		if (
			this.cat2.equals("P")
			)
		{
			this.cat2="PP";
		}
		else
		{
			//this.cat2 = this.cat2;
		}
		return false;
	}

	/****************************
	 * 
	 * Liste des règles pour DE-OBJ
	 * 
	 ****************************/
	

	private boolean regle_3_4_1(ArrayList<String[]> listMot) {
		boolean end = false;
		if (this.lemma.equals("de") || this.lemma.equals("du")){
			int iter = 0, max = listMot.size();
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[4].equals("VPinf") || listMot.get(iter)[4].equals("VINF"))
					)
				{
					this.cat2 = "VPinf";
					end = true;
				}
				iter++;
			}
			if (!end){
				//this.cat2 = this.cat2;
			/*if (!listFonctionParasite.contains(subFonct+this.cat2))
				listFonctionParasite.add(subFonct+this.cat2);*/
			}
		}
		return end;
	}
	
	private boolean regle_3_4_2(ArrayList<String[]> listMot) {
		boolean end = false;
		if (this.lemma.equals("de") || this.lemma.equals("du")){
			int iter = 0, max = listMot.size();
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[3].equals("NP") || listMot.get(iter)[3].equals("N"))
					)
				{
					this.cat2 = "PP";
					end = true;
				}
				iter++;
			}
			if (!end){
				//this.cat2 = this.cat2;
			/*if (!listFonctionParasite.contains(subFonct+this.cat2))
				listFonctionParasite.add(subFonct+this.cat2);*/
			}
		}
		return end;
	}

	private boolean regle_3_4_3() {
		if (
			this.cat2.equals("C") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("CS") ||
			this.cat2.equals("CC")
			)
		{
			this.cat2 = "Ssub";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_4_4() {
		if (
			this.lemma.equals("dont") ||
			this.lemma.equals("en") ||
			this.lemma.equals("duquel") ||
			this.lemma.equals("se")
			)
		{
			this.cat2 = "PP";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_4_rien() {
		if (
			this.cat2.equals("P")
			)
		{
			this.cat2="PP";
		}
		else
		{
			//this.cat2 = this.cat2;
		}
		return false;
	}

	/****************************
	 * 
	 * Liste des règles pour P-OBJ
	 * 
	 ****************************/
	

	private boolean regle_3_5_1(ArrayList<String[]> listMot) {
		boolean end = false;
		if (this.cat2.equals("PP")){
			int iter = 0, max = listMot.size();
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[4].equals("VPinf") || listMot.get(iter)[4].equals("VINF"))
					)
				{
					this.cat2 = "VPinf";
					end = true;
				}
				iter++;
			}
			if (!end){
				//this.cat2 = this.cat2;
			/*if (!listFonctionParasite.contains(subFonct+this.cat2))
				listFonctionParasite.add(subFonct+this.cat2);*/
			}
		}
		return end;
	}
	
	private boolean regle_3_5_2(ArrayList<String[]> listMot) {
		boolean end = false;
		if (this.cat2.equals("PP")){
			int iter = 0, max = listMot.size();
			while (!end && iter<max)
			{
				if (
					Integer.parseInt(listMot.get(iter)[6]) == (this.id+1) && 
					(listMot.get(iter)[7].equals("OBJ") || listMot.get(iter)[7].equals("obj")) && 
					(listMot.get(iter)[3].equals("NP") || listMot.get(iter)[3].equals("N"))
					)
				{
					this.cat2 = "PP";
					end = true;
				}
				iter++;
			}
			if (!end){
				//this.cat2 = this.cat2;
			/*if (!listFonctionParasite.contains(subFonct+this.cat2))
				listFonctionParasite.add(subFonct+this.cat2);*/
			}
		}
		return end;
	}

	private boolean regle_3_5_3() {
		if (
			this.cat2.equals("C") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("CC") ||
			this.cat2.equals("CLO") ||
			this.cat2.equals("CLR")
			)
		{
			this.cat2 = "Ssub";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_5_4() {
		if (
			this.cat2.equals("PP") ||
			//ce qui suis c'est les modifications supposées
			this.cat2.equals("P") ||
			this.cat2.equals("P+D")
			)
		{
			this.cat2="PP";
			return true;
		}
		else
			return false;
	}

	private boolean regle_3_5_rien() {
		// this.cat2 = this.cat2;
		return false;
	}

	/****************************
	 * 
	 * Liste des règles pour ATS
	 * 
	 ****************************/
	

	private boolean regle_3_6_1() {
		boolean end = false;
		if (
			this.fonction.equals("ATS")
			)
		{
			this.cat2 = "XP";
		}
		return end;
	}

	/****************************
	 * 
	 * Liste des règles pour ATO
	 * 
	 ****************************/
	

	private boolean regle_3_7_1() {
		boolean end = false;
		if (
			this.fonction.equals("ATO")
			)
		{
			this.cat2 = "XP";
		}
		return end;
	}

	/****************************
	 * 
	 * Liste des règles pour refl et obj
	 * 
	 ****************************/
	

	private boolean regle_3_8_1() {
		boolean end = false;
		if (
			this.lemma.equals("en")
			)
		{
			this.fonction = "obj";
			this.cat2 = "en";
			end = true;
		}
		return end;
	}
	

	private boolean regle_3_8_2() {
		boolean end = false;
		if (
			this.lemma.equals("y")
			)
		{
			this.fonction = "obj";
			this.cat2 = "y";
			end = true;
		}
		return end;
	}
	

	private boolean regle_3_8_3() {
		boolean end = false;
		if (
			this.cat2.equals("CLR")
			)
		{
			this.fonction = "refl";
			this.cat2 = "CL";
			end = true;
		}
		return end;
	}
	

	private boolean regle_3_8_4() {
		boolean end = false;
		if (
			this.cat2.equals("CLO")
			)
		{
			this.fonction = "OBJ";
			this.cat2 = "NP";
		}
		return end;
	}
	

	private boolean regle_3_8_rien() {
		boolean end = false;
//		this.fonction = this.fonction;
//		this.cat2 = this.cat2;
		return end;
	}

	
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getIdPhrase() {
		return idPhrase;
	}

	public void setIdPhrase(int idPhrase) {
		this.idPhrase = idPhrase;
	}

	public int getIdVerbe() {
		return idVerbe;
	}

	public void setIdVerbe(int idVerbe) {
		this.idVerbe = idVerbe;
	}

	public String getForm() {
		return form;
	}

	public void setForm(String form) {
		this.form = form;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getCat1() {
		return cat1;
	}

	public void setCat1(String cat1) {
		this.cat1 = cat1;
	}

	public String getCat2() {
		return cat2;
	}

	public void setCat2(String cat2) {
		this.cat2 = cat2;
	}

	public String getFonction() {
		return fonction;
	}

	public void setFonction(String fonction) {
		this.fonction = fonction;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}
