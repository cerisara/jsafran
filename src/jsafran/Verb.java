package jsafran;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

public class Verb {
	private int id;
	private int idSentence;
	private int idMot;
	private String form;
	private String lemma;
	private String Categorie1, Categorie2, fonction;
	private boolean infoPassif;
	private boolean matchTreelex;
	private boolean matchXML;
	private String cadreObserve;
	private String cadreTreeLexTrouve;
	private String sens;
	private ArrayList<String[]> listDep = new ArrayList<String[]>();
	private ArrayList<DepSrl> listeDep = new ArrayList<DepSrl>();
	private ArrayList<String[]> listMot = new ArrayList<String[]>();
	
	public ArrayList<String[]> getListMot() {
		return listMot;
	}



	public void setListMot(ArrayList<String[]> listMot) {
		this.listMot = listMot;
	}



	private static String xmlPath = "./srl/result/xml/";
	
	
	static HashMap<String, ArrayList<String>> listVerbTreelex = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> mapVerbCadre = new HashMap<String, ArrayList<String>>();
	static ArrayList<String[]> listCadresTreeLex = new ArrayList<String[]>();
	static ArrayList<String> listTransitif = new ArrayList<String>();
	static ArrayList<String> listPerception = new ArrayList<String>();
	
	
	
	//constructeur nouvelle version
	//manque encore quelques éléments
	public Verb(int id, int idSentence, int idMot,
			String form, String lemma, String categorie1,
			String categorie2, String fonction, ArrayList<DepSrl> listeDep, ArrayList<String[]> listMot) {
		super();
		this.id = id;
		this.idSentence = idSentence;
		this.idMot = idMot;
		this.form = form;
		this.lemma = lemma;
		this.Categorie1 = categorie1;
		this.Categorie2 = categorie2;
		this.setFonction(fonction);
		this.setListeDep(listeDep);
		this.setListMot(listMot);
	}


	/**
	 * calcul des 2 premiers champs dans les relations SUJ:NP:0
	 */
	public void treatArg() {
		//pour chaque dependant de listeDep
		for (int i=0; i<listeDep.size(); i++){
			listeDep.get(i).treatDep(this.listMot);
		}
	}
	
	/**
	 * normalisation des structures passives et causatives
	 */
	public void treatVerb() {
		boolean applique = false; //indique la la regle s'est appliqué

		applique = regle_4_1();
		if (!applique) {
			applique = regle_4_2();
		}
		if (!applique) {
			applique = regle_4_3();
		}
		if (!applique)
		{
			applique = regle_4_4();
		}
		if (!applique)
		{
			applique = regle_4_5();
		}
		if (!applique)
		{
			applique = regle_4_6();
		}
		if (!applique)
		{
			applique = regle_4_7();
		}
		if (!applique)
		{
			applique = regle_4_8();
		}
		if (!applique)
		{
			applique = regle_4_9();
		}
	}

	/****************************
	 * 
	 * Liste des règles pour le traitement du verbe
	 * 
	 ****************************/
	

	private boolean regle_4_1() {
		if (this.lemma.equals("dater")) {
			return regle_5_1();
		} else return false;
	}
	

	private boolean regle_4_2() {
		if (this.fonction.equals("dep_coord")) {
			return regle_5_2();
		} else
			return false;
	}
	

	private boolean regle_4_4() {
		if (
			containArg("aux_pass")
			)
		{
			return regle_5_4_passif();
		}
		else
			return false;
	}
	

	private boolean regle_4_3() {
		if (
			containArg("aux_pass") &&
			this.Categorie2.equals("VINF")
			)
		{
			return regle_5_4_1();
		}
		else
			return false;
	}



	private boolean regle_4_5() {
		if (
			containArg("aux_caus")
			)
		{
			return regle_5_5_causatif();
		}
		else
			return false;
	}



	private boolean regle_4_6() {
		boolean end = false;
		if (
			(this.fonction.equals("obj") || this.fonction.equals("OBJ"))
			//voir les conditions pour lancer la règle 4_4
			)
		{
			int idParent = Integer.valueOf(this.listMot.get(this.idMot)[6])-1;
			if (
					this.listMot.get(idParent)[3].equals("V") &&
				Verb.listPerception.contains(this.listMot.get(idParent)[2])
				)
			{
				//System.out.println("lemme parent : "+listMot.get(idParent)[2]);
				end = regle_5_5_causatif();
			}
		}
		else
			end = false;
		return end;
	}
	

	private boolean regle_4_7() {
		if (
			this.Categorie2.equals("VINF") && 
			!this.lemma.equals("faire")
			)
		{
			return regle_5_3_1();
		}
		else
			return false;
	}
	

	private boolean regle_4_8() {
		if (
			this.Categorie2.equals("VPR") && 
			!this.lemma.equals("faire")
			)
		{
			return regle_5_3_3();
		}
		else
			return false;
	}
	

	private boolean regle_4_9() {
		boolean end = false;
		if (
			this.Categorie2.equals("VPP") && 
			!this.lemma.equals("faire")
			//mod(N)
			)
		{
				end = regle_5_3_4();
		}
		else
			end = false;
		return end;
	}

	/****************************
	 * 
	 * Liste des règles pour la normalisation des cadres
	 * 
	 ****************************/
	

	private boolean regle_5_1() {
		boolean end = false;
		int idParent = Integer.valueOf(this.listMot.get(this.idMot)[6])-1;
		if (
			this.fonction.equals("mod") &&
			this.listMot.get(idParent)[3].equals("N")
			)
		{
			int iter = 0, max = this.listMot.size();
			while (!end && iter<max)
			{
				if (
						this.listMot.get(iter)[7].equals("mod") &&
					(Integer.parseInt(this.listMot.get(iter)[6])-1) == this.idMot
					)
				{
					this.listeDep.add(new DepSrl(this.idMot, 
							this.idSentence, 
							this.idMot, 
							"", 
							"", 
							"NP", 
							"NP", 
							"SUJ"));
					this.listeDep.add(new DepSrl(idParent, 
							this.idSentence, 
							this.idMot, 
							"", 
							"", 
							"NP", 
							"NP", 
							"OBJ"));
					this.listeDep.add(new DepSrl(this.idMot, 
							this.idSentence, 
							iter, 
							"", 
							"", 
							"PP", 
							"PP", 
							"DE-OBJ"));
					this.listeDep.get(this.listeDep.size()-1).treatDep(this.listMot);
					end = true;
					
				}
				iter++;
			}
		}
		else
			end = false;
		return end;
	}
	

	private boolean regle_5_2() {
		boolean end = false;
		int idParent = Integer.valueOf(this.listMot.get(this.idMot)[6])-1;
		if (
			!containArg("SUJ") &&
			this.listMot.get(idParent)[7].equals("coord")
			)
		{
			idParent = Integer.valueOf(this.listMot.get(idParent)[6])-1;
			if (
				(this.listMot.get(idParent)[4].equals("V") || this.listMot.get(idParent)[4].equals("VS"))
				)
			{
				int iter = 0, max = this.listMot.size();
				while (!end && iter<max)
				{
					if (
						(this.listMot.get(iter)[7].equals("SUJ") || this.listMot.get(iter)[7].equals("suj")) &&
						(Integer.parseInt(this.listMot.get(iter)[6])-1) == idParent
						)
					{
						this.listeDep.add(new DepSrl(this.idMot, 
								this.idSentence, 
								iter, 
								"", 
								"", 
								this.listMot.get(iter)[3], 
								this.listMot.get(iter)[4], 
								"SUJ"));
						this.listeDep.get(this.listeDep.size()-1).treatDep(this.listMot);
						end = true;
						
					}
					iter++;
				}
			}
		}
		else
			end = false;
		return end;
	}
	
	//////////////////////
	//infinitifs et participes
	//////////////////////
	private boolean regle_5_3_1() {
		boolean end = false;
		int idParent = Integer.valueOf(this.listMot.get(this.idMot)[6])-1;
		
		if (
			(this.Categorie2.equals("VINF") || this.Categorie2.equals("VPinf")) &&
			this.fonction.equals("obj") &&
			(this.listMot.get(idParent)[4].equals("V") || this.listMot.get(idParent)[4].equals("VS"))			
			)
		{
			int iter = 0, max = this.listMot.size();
			while (!end && iter<max)
			{
				if (
					(this.listMot.get(iter)[7].equals("SUJ") || this.listMot.get(iter)[7].equals("suj")) &&
					(Integer.parseInt(this.listMot.get(iter)[6])-1) == idParent
					)
				{
					this.listeDep.add(new DepSrl(this.idMot, 
							this.idSentence, 
							iter, 
							"", 
							"", 
							this.listMot.get(iter)[3], 
							this.listMot.get(iter)[4], 
							"SUJ"));
					this.listeDep.get(this.listeDep.size()-1).treatDep(this.listMot);
					end = true;
					
				}
				iter++;
			}
		}
		if (!end)
		{
			end = regle_5_3_2();
		}
		return end;
	}
	
	
	private boolean regle_5_3_2() {
		this.listeDep.add(new DepSrl(this.idMot, 
					this.idSentence, 
					this.idMot, 
					"", 
					"", 
					"NP", 
					"NP", 
					"SUJ"));
		return true;
	}
	

	private boolean regle_5_3_3() {
		if (!containArg("SUJ"))
		{
			this.listeDep.add(new DepSrl(this.idMot, 
				this.idSentence, 
				this.idMot, 
				"", 
				"", 
				"NP", 
				"NP", 
				"SUJ"));
		}
		return true;
	}
	

	private boolean regle_5_3_4() {
		//changer le P-OBJ[par] en SUJ:NP
		boolean hadPObj = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith("P-OBJ") && 
				this.listeDep.get(i).getLemma().endsWith("par")){
				this.listeDep.get(i).setCat1("NP");
				this.listeDep.get(i).setCat2("NP");
				this.listeDep.get(i).setFonction("SUJ");
				hadPObj = true;
			}
		}
		if (!containArg("SUJ"))
		{
			this.listeDep.add(new DepSrl(this.idMot, 
					this.idSentence, 
					this.idMot, 
					"", 
					"", 
					"NP", 
					"NP", 
					"SUJ"));
		}
		//gérer le mod(N1)
		int idParent = Integer.valueOf(this.listMot.get(this.idMot)[6])-1;
		if (
			this.fonction.equals("mod") &&
			this.listMot.get(idParent)[3].equals("N")
			)
		{
		this.listeDep.add(new DepSrl(idParent, 
				this.idSentence, 
				this.idMot, 
				"", 
				"", 
				"NP", 
				"NP", 
				"OBJ"));
		}
		return true;
	}
	
	//////////////////////
	//passifs
	//////////////////////
	

	private boolean regle_5_4_1() {
		//changer le P-OBJ[par] en SUJ:NP
		boolean hadPObj = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith("P-OBJ") && 
				this.listeDep.get(i).getLemma().endsWith("par")){
				this.listeDep.get(i).setCat1("NP");
				this.listeDep.get(i).setCat2("NP");
				this.listeDep.get(i).setFonction("SUJ");
				hadPObj = true;
			}
		}
		if (!hadPObj)
		{
			this.listeDep.add(new DepSrl(this.idMot, 
					this.idSentence, 
					this.idMot, 
					"", 
					"", 
					"NP", 
					"NP", 
					"SUJ"));
		}
		this.listeDep.add(new DepSrl(this.idMot, 
				this.idSentence, 
				this.idMot, 
				"", 
				"", 
				"NP", 
				"NP", 
				"OBJ"));
		//enlever la mention aux_
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith("aux_pass")){
				this.listeDep.remove(i);
				break;
			}
		}
		return true;
	}
	

	private boolean regle_5_4_passif() {
		//on regroupe toute les regles du passif 5_2_2 à 5_2_7 en une seul
		//en effet, il s'agit de transformer le suj en obj et de transformé ou ajouté le P-obj en SUJ:NP
		//changer le suj en obj
		boolean end = false;
		end = regle_5_4_2();
		if (!end)
			end = regle_5_4_3();
		return end;
	}
	

	private boolean regle_5_4_2() {
		boolean end = false;
		int idParent = Integer.valueOf(this.listMot.get(this.idMot)[6])-1;
		
		if (
			this.fonction.equals("obj") &&
			(this.listMot.get(idParent)[4].equals("V") || this.listMot.get(idParent)[4].equals("VS"))			
			)
		{
			int iter = 0, max = this.listMot.size();
			while (!end && iter<max)
			{
				if (
					(this.listMot.get(iter)[7].equals("SUJ") || this.listMot.get(iter)[7].equals("suj")) &&
					(Integer.parseInt(this.listMot.get(iter)[6])-1) == idParent
					)
				{
					this.listeDep.add(new DepSrl(this.idMot, 
							this.idSentence, 
							iter, 
							"", 
							"", 
							this.listMot.get(iter)[3], 
							this.listMot.get(iter)[4], 
							"OBJ"));
					this.listeDep.get(this.listeDep.size()-1).treatDep(this.listMot);
					end = true;
					
				}
				iter++;
			}
		}
		if (end)
		{
			//changer le P-OBJ[par] en SUJ:NP
			boolean hadPObj = false;
			for (int i=0; i<this.listeDep.size(); i++){
				if (this.listeDep.get(i).getFonction().startsWith("P-OBJ") && 
					this.listeDep.get(i).getLemma().endsWith("par")){
					this.listeDep.get(i).setCat1("NP");
					this.listeDep.get(i).setCat2("NP");
					this.listeDep.get(i).setFonction("SUJ");
					hadPObj = true;
				}
			}
			if (!hadPObj)
			{
				this.listeDep.add(new DepSrl(this.idMot, 
						this.idSentence, 
						this.idMot, 
						"", 
						"", 
						"NP", 
						"NP", 
						"SUJ"));
			}
			//enlever la mention aux_
			for (int i=0; i<this.listeDep.size(); i++){
				if (this.listeDep.get(i).getFonction().startsWith("aux_pass")){
					this.listeDep.remove(i);
					break;
				}
			}
		}
		return end;
	}
	

	private boolean regle_5_4_3() {
		//on regroupe toute les regles du passif 5_2_2 à 5_2_7 en une seul
		//en effet, il s'agit de transformer le suj en obj et de transformé ou ajouté le P-obj en SUJ:NP
		//changer le suj en obj
		boolean hadSUJ = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (
				this.listeDep.get(i).getFonction().startsWith("SUJ")
				)
			{
				this.listeDep.get(i).setFonction("OBJ");
				hadSUJ = true;
			}
		}
		
		//changer le P-OBJ[par] en SUJ:NP
		boolean hadPObj = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith("P-OBJ") && 
				this.listeDep.get(i).getLemma().endsWith("par")){
				this.listeDep.get(i).setCat1("NP");
				this.listeDep.get(i).setCat2("NP");
				this.listeDep.get(i).setFonction("SUJ");
				hadPObj = true;
			}
		}
		if (!hadPObj)
		{
			this.listeDep.add(new DepSrl(this.idMot, 
					this.idSentence, 
					this.idMot, 
					"", 
					"", 
					"NP", 
					"NP", 
					"SUJ"));
		}
		//enlever la mention aux_
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith("aux_pass")){
				this.listeDep.remove(i);
				break;
			}
		}
		return true;
	}
	
	//////////////////////
	//causatifs
	//////////////////////

	private boolean regle_5_5_causatif() {
		boolean end = false;
		if (
			containArg("OBJ") &&
			!Verb.listTransitif.contains(this.lemma)
			)
		{
			end = regle_5_5_1();
		}
		else if (
				containArg("OBJ") &&
				Verb.listTransitif.contains(this.lemma)
				)
		{
			end = regle_5_5_2();
		}
		//enlever la mention aux_
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith("aux_caus")){
				this.listeDep.remove(i);
				break;
			}
		}
		return end;
	}



	private boolean regle_5_5_1() {
		boolean hadOBJ = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (
				this.listeDep.get(i).getFonction().startsWith("OBJ")
				)
			{
				this.listeDep.get(i).setFonction("SUJ");
				hadOBJ = true;
			}
		}
		this.listeDep.add(new DepSrl(this.idMot, 
				this.idSentence, 
				this.idMot, 
				"", 
				"", 
				"NP", 
				"NP", 
				"OBJ"));
		return hadOBJ;
	}



	private boolean regle_5_5_2() {
		boolean hadOBJ = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (
				this.listeDep.get(i).getFonction().startsWith("OBJ")
				)
			{
				this.listeDep.get(i).setFonction("SUJ");
				hadOBJ = true;
			}
		}
		return hadOBJ;
	}



	private boolean containCadre(String string, String string2) {
		boolean find = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith(string) &&
				this.listeDep.get(i).getCat2().startsWith(string2)){
				find=true;
				break;
			}
		}
		return find;
	}



	private boolean containArg(String arg) {
		boolean find = false;
		for (int i=0; i<this.listeDep.size(); i++){
			if (this.listeDep.get(i).getFonction().startsWith(arg)){
				find=true;
				break;
			}
		}
		return find;
	}



	public void researchCadre() {
		/*

		this.cadre = sortCadre(dep2cadre());
		
		//v4 on fait le test sur les xml enregistré dans une map
		// On regarde si la map possède la clé
		if (Verb.mapVerbCadre.containsKey(this.lemma))
		{
			// Si oui alors on fait la recherche parmis la liste des cadre contenu dans la map
			searchMapCadre();
		}
		else
		{
			// Si non on regarde si le fichier xml du verbe existe
			boolean exists = (new File(xmlPath+this.lemma.charAt(0)+"/"+this.lemma+".xml")).exists(); 
			if (exists)
			{
				// Si oui on ajoute à la map le verbe et la liste des cadres
				addXMLMap();
				// Puis on rechreche le cadre
				searchMapCadre();
			}
			else
			{
				// Si non verbe non trouvé (et pit etre gestion de la transition treelex+ >> xml ???
				this.cadreTrouve = "VERBE NON TROUVE";
				searchCadre();
			}
		}*/
		
		/*
		//v3 recherche parmis les verbes en xml
		//on verifie si le fichier XML existe
		boolean exists = (new File(xmlPath+this.lemma.charAt(0)+"/"+this.lemma+".xml")).exists(); 
		if (exists)
		{
		//si oui on recherche le cadre parmis les cadre sem puis parmis les otherframes
			searchXmlCadre();
		}
		//sinon on fait la recherche classique 
		else
		{
			searchCadre();
		}*/
		//v2 recherche parmis le tableau des dependances
		
		//Recherche du cadre syntaxique dans Treelex

		cadreObserve = sortCadre(dep2cadre());
		searchCadreTreeLex();
		
		//SI le cadre est trouvé (matchTreelex == true)
		//Alors on fait une recherche dans les Fichiers XML
		if (matchTreelex) {
			if (Verb.mapVerbCadre.containsKey(lemma)) {
				// Si oui alors on fait la recherche parmis la liste des cadre contenu dans la map
				searchMapCadre();
			} else {
				// Si non on regarde si le fichier xml du verbe existe
				boolean exists = (new File(xmlPath+this.lemma.charAt(0)+"/"+this.lemma+".xml")).exists(); 
				if (exists) {
					// Si oui on ajoute à la map le verbe et la liste des cadres
					addXMLMap();
					// Puis on rechreche le cadre
					searchMapCadre();
				}
			}
		}
	}



	public void searchMapCadre() {
		ArrayList<String> listMapCadre = Verb.mapVerbCadre.get(this.lemma);
		String result = new String("CADRE NON TROUVE");
		String sens = new String("SENS NON TROUVE");
		boolean findCadre = false;
		for (String currentCadre : listMapCadre)
		{
			String currentCadreWithoutTriplet = currentCadre.split("/t")[1].replaceAll(":[0-9]+", "");
			if (compareCadre(currentCadreWithoutTriplet.trim(), (this.cadreObserve.trim())))
			{
				findCadre = true;
				result = currentCadre.split("/t")[1];
				sens = currentCadre.split("/t")[0];
				//System.out.println(result);
				break;
			}
		}
		this.matchXML = findCadre;		
		this.cadreTreeLexTrouve = result;
		this.sens = sens;
	}



	public void addXMLMap() {
		try{
			ArrayList<String> listMapCadre = new ArrayList<String>();
	
			// création d'une fabrique de documents
			DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
			
			// création d'un constructeur de documents
			DocumentBuilder constructeur = fabrique.newDocumentBuilder();
			
			// lecture du contenu d'un fichier XML avec DOM
			File xml = new File(xmlPath+this.lemma.charAt(0)+"/"+this.lemma+".xml");
			Document document = constructeur.parse(xml);
			
			//traitement du document
			//on recherche la liste des frames
			Element racine = document.getDocumentElement();

			String tag = "frame";

			NodeList liste = racine.getElementsByTagName(tag);
			
			for (int i=0; i<liste.getLength(); i++) {
				Element e = (Element)liste.item(i);

				String cadre = "syntax";
				String roleset = "roleset";

				NodeList listeCadre = e.getElementsByTagName(cadre);
				NodeList listeRoleset = e.getElementsByTagName(roleset);
				Element c = (Element)listeCadre.item(0); //le cadre pour le roleset courant
				Element r = (Element)listeRoleset.item(0); //la descrioption pour le roleset courant
				if (!c.getParentNode().getNodeName().equals("otherframes"))
				{
					listMapCadre.add(r.getAttribute("id")+"/t"+c.getTextContent());
				}
			}

			/* ANCIENNE VERSION
			String tag = "syntax";

			NodeList liste = racine.getElementsByTagName(tag);
			
			for (int i=0; i<liste.getLength(); i++)
			{
				Element e = (Element)liste.item(i);
				if (!e.getParentNode().getNodeName().equals("otherframes"))
				{
					listMapCadre.add(e.getTextContent());
					System.out.println(" avec pour cadre : "+e.getTextContent());
				}
			}
			*/
			//ajout de la liste des cadre dans la map
			Verb.mapVerbCadre.put(this.lemma, listMapCadre);
		
		}catch(ParserConfigurationException pce){
			System.out.println("Erreur de configuration du parseur DOM");
			System.out.println("lors de l'appel à fabrique.newDocumentBuilder();");
		}catch(SAXException se){
			System.out.println("Erreur lors du parsing du document");
			System.out.println("lors de l'appel à construteur.parse(xml)");
		}catch(IOException ioe){
			System.out.println("Erreur d'entrée/sortie");
			System.out.println("lors de l'appel à construteur.parse(xml)");
		}
	}
	
	private void searchXmlCadre() {
		try{
			// création d'une fabrique de documents
			DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
			
			// création d'un constructeur de documents
			DocumentBuilder constructeur = fabrique.newDocumentBuilder();
			
			// lecture du contenu d'un fichier XML avec DOM
			File xml = new File(xmlPath+this.lemma.charAt(0)+"/"+this.lemma+".xml");
			Document document = constructeur.parse(xml);
			
			//traitement du document
			//on recherche la liste des frames
			Element racine = document.getDocumentElement();

			String tag = "syntax";

			NodeList liste = racine.getElementsByTagName(tag);
			
			int i=0;
			boolean find = false;
			String result = new String("CADRE NON TROUVE");
			while( i<liste.getLength() && !find){
				Element e = (Element)liste.item(i);
				String currentCadre = e.getTextContent();
				if (currentCadre.split(", ")[0].split(":").length == 3)
				{
					String currentCadreWithoutTriplet = currentCadre.replaceAll(":[0-9]+", "");
					//System.out.println("cadre de " + this.lemma+" : "+currentCadre+" sans triplet : "+currentCadreWithoutTriplet);
					if (compareCadre(currentCadreWithoutTriplet.trim(), (this.cadreObserve.trim())))
					{
						find = true;
						result = currentCadre;
					}
				}
				else
				{
					//autre cadre (constitué de doublet)
					if (compareCadre(currentCadre.trim(), (this.cadreObserve.trim())))
					{
						find = true;
						result = currentCadre;
					}
				}
				i++;
			}
		
		}catch(ParserConfigurationException pce){
			System.out.println("Erreur de configuration du parseur DOM");
			System.out.println("lors de l'appel à fabrique.newDocumentBuilder();");
		}catch(SAXException se){
			System.out.println("Erreur lors du parsing du document");
			System.out.println("lors de l'appel à construteur.parse(xml)");
		}catch(IOException ioe){
			System.out.println("Erreur d'entrée/sortie");
			System.out.println("lors de l'appel à construteur.parse(xml)");
		}
	}


	/**
	 * transforme les dependants en un cadre syntaxique
	 * @return
	 */
	private ArrayList<String> dep2cadre() {
		ArrayList<String> tabSubCadre = new ArrayList<String>();
		//pour chaque dependance
		//on l'ajoute au tableau des cadres
		for (int i=0; i<listeDep.size(); i++){
			tabSubCadre.add(listeDep.get(i).getFonction()+":"+listeDep.get(i).getCat2());
		}
		return tabSubCadre;
	}
	
	/**
	 * ordonne les elements du cadre syntaxique selon un ordre precis
	 * 
	 * @param tabSubCadre
	 * @return
	 */
	private String sortCadre(ArrayList<String> tabSubCadre) {
		String cadre = new String("");

		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("SUJ:")){		
				cadre += tabSubCadre.get(i)+", ";
				tabSubCadre.remove(i);
				break;
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("OBJ:")){		
				cadre += tabSubCadre.get(i)+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("A-OBJ:")){		
				cadre += tabSubCadre.get(i)+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("DE-OBJ:")){		
				cadre += tabSubCadre.get(i)+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("P-OBJ:")){		
				cadre += tabSubCadre.get(i).split("\t")[0]+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("ATS:")){		
				cadre += tabSubCadre.get(i).split("\t")[0]+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (tabSubCadre.get(i).startsWith("obj:en")){		
				cadre += tabSubCadre.get(i).split("\t")[0]+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (!tabSubCadre.get(i).equals("SUPPRESSION") && !tabSubCadre.get(i).startsWith("aux_")){
				cadre += tabSubCadre.get(i)+", ";
				tabSubCadre.set(i, "SUPPRESSION");
			}
		}
		
		for (int i=0; i<tabSubCadre.size(); i++){
			if (!tabSubCadre.get(i).equals("SUPPRESSION")){
				cadre += tabSubCadre.get(i)+", ";
			}
		}
		
		if (cadre.length()>2)
			cadre = cadre.substring(0, cadre.length()-2);
		
		return cadre;
	}


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String listeDep = new String();
		listeDep = "";
		for (String[] currentDep : listDep){
			listeDep += currentDep[0]+"("+currentDep[2]+") : ";
		}
		return "Verb [form=" + form 
				+ ", lemma=" + lemma 
				+ ", id=" + id 
				+ ", Categorie1=" + Categorie1 
				+ ", Categorie2=" + Categorie2
				+ ", cadre=" + cadreObserve
				+ ", cadreTrouve=" + cadreTreeLexTrouve
				+ ", infoPassif="+ infoPassif +listeDep+ "\n]";
	}
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param form
	 * @param lemma
	 * @param categorie1
	 * @param categorie2
	 * @param cadre 
	 * @param infoPassif
	 * @param idSentence 
	 * @param idMot 
	 */
	public Verb(int id, String form, String lemma, String categorie1,
			String categorie2, String cadre, boolean infoPassif, int idSentence, int idMot) {
		super();
		this.id = id;
		this.idSentence = idSentence;
		this.idMot = idMot;
		this.form = form;
		this.lemma = lemma;
		Categorie1 = categorie1;
		Categorie2 = categorie2;
		this.setCadre(cadre);
		this.infoPassif = infoPassif;
		searchCadreTreeLex();
	}
	/**
	 * @param form
	 * @param lemma
	 * @param categorie1
	 * @param categorie2
	 * @param infoPassif
	 */
	public Verb(int id, String form, String lemma, String categorie1, 
			boolean infoPassif) {
		super();
		this.id = id;
		this.form = form;
		this.lemma = lemma;
		Categorie1 = categorie1;
		Categorie2 = categorie1;
		this.infoPassif = infoPassif;
	}
	private void searchCadreTreeLex() {
		String result = "CADRE NON TROUVE";
		boolean findCadre = false, findVerb = false;
		for (String[] currentCouple : Verb.listCadresTreeLex) {
			if (currentCouple[0].equals(lemma)) {
				findVerb = true;
				if (compareCadre(currentCouple[1].trim(), cadreObserve.trim())) {
					findCadre = true;
					result = currentCouple[1];
					break;
				}
			}
		}
		if (!findVerb)
			result = "VERBE NON TROUVE";
		matchTreelex = findCadre;		
		cadreTreeLexTrouve = result;
	}
	
	
	private boolean compareCadre(String cadreTreelex, String cadreVerb) {
		boolean find = false;
		if (cadreVerb.trim().equals(cadreTreelex.trim()))
		{
			find=true;
		}
		else if (cadreTreelex.contains("("))
		{
			//on test les optionalités
			String eltOption = new String(), cadreSans = new String(), cadreAvec = new String();
			//System.out.println("Cadre : "+cadreTreelex);
			int ouverture = cadreTreelex.indexOf('(');
			int fermeture = cadreTreelex.indexOf(')');
			eltOption = cadreTreelex.substring(ouverture+1, fermeture);
			//System.out.println("partie optionelle : "+eltOption);
			String[] tabCadre = cadreTreelex.split(eltOption);
			cadreSans = tabCadre[0].substring(0, tabCadre[0].length()-3)+tabCadre[1].substring(1);
			cadreSans = cadreSans.replace(" ,", ",");
			//System.out.println("Verbe : "+this.lemma+" Cadre sans : "+cadreSans);
			cadreAvec = tabCadre[0].substring(0, tabCadre[0].length()-1)+eltOption+tabCadre[1].substring(1);
			//System.out.println("Verbe : "+this.lemma+" Cadre avec : "+cadreAvec);
			find = compareCadre(cadreSans, cadreVerb);
			if (!find)
				find = compareCadre(cadreAvec, cadreVerb);
		}
		return find;
	}



	/**
	 * @return the form
	 */
	public String getForm() {
		return form;
	}
	/**
	 * @return the lemma
	 */
	public String getLemma() {
		return lemma;
	}
	/**
	 * @return the categorie1
	 */
	public String getCategorie1() {
		return Categorie1;
	}
	/**
	 * @return the categorie2
	 */
	public String getCategorie2() {
		return Categorie2;
	}
	/**
	 * @return the infoPassif
	 */
	public boolean isInfoPassif() {
		return infoPassif;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @param form the form to set
	 */
	public void setForm(String form) {
		this.form = form;
	}
	/**
	 * @param lemma the lemma to set
	 */
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	/**
	 * @param categorie1 the categorie1 to set
	 */
	public void setCategorie1(String categorie1) {
		Categorie1 = categorie1;
	}
	/**
	 * @param categorie2 the categorie2 to set
	 */
	public void setCategorie2(String categorie2) {
		Categorie2 = categorie2;
	}
	public void setFonction(String fonction) {
		this.fonction = fonction;
	}



	public String getFonction() {
		return fonction;
	}



	/**
	 * @param infoPassif the infoPassif to set
	 */
	public void setInfoPassif(boolean infoPassif) {
		this.infoPassif = infoPassif;
	}
	public void addDep(String[] currentDep) {
		this.listDep.add(currentDep);
	}
	public void setCadre(String cadre) {
		this.cadreObserve = cadre;
	}
	public String getCadre() {
		return cadreObserve;
	}
	public String getSens() {
		return sens;
	}



	public void setSens(String sens) {
		this.sens = sens;
	}



	public void setCadreTrouve(String cadreTrouve) {
		this.cadreTreeLexTrouve = cadreTrouve;
	}
	public String getCadreTrouve() {
		return cadreTreeLexTrouve;
	}
	public void setIdSentence(int idSentence) {
		this.idSentence = idSentence;
	}
	public int getIdSentence() {
		return idSentence;
	}
	public void setIdMot(int idMot) {
		this.idMot = idMot;
	}
	public int getIdMot() {
		return idMot;
	}



	public void setListeDep(ArrayList<DepSrl> listeDep) {
		this.listeDep = listeDep;
	}



	public ArrayList<DepSrl> getListeDep() {
		return listeDep;
	}



	public boolean isMatchTreelex() {
		return matchTreelex;
	}



	public void setMatchTreelex(boolean matchTreelex) {
		this.matchTreelex = matchTreelex;
	}



	public boolean isMatchXML() {
		return matchXML;
	}



	public void setMatchXML(boolean matchXML) {
		this.matchXML = matchXML;
	}



	

}