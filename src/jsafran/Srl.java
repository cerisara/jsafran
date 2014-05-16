package jsafran;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*; 
import javax.xml.transform.dom.*; 
import javax.xml.transform.stream.*; 

public class Srl {
	
	final static boolean detdebug = false;
	
	//v2
	//les différents graphes
	static List<DetGraph> graphNormal = new LinkedList<DetGraph>();
	static List<DetGraph> graphSrl = new LinkedList<DetGraph>();
	static LinkedList<DetGraph> graphTreelex = new LinkedList<DetGraph>();
	static LinkedList<DetGraph> graphXML = new LinkedList<DetGraph>();
	static ArrayList<Integer> noMatchTreelex = new ArrayList<Integer>();
	static ArrayList<Integer> noMatchXML = new ArrayList<Integer>();
	static ArrayList<Integer> matchTreelex = new ArrayList<Integer>();
	static ArrayList<Integer> matchXML = new ArrayList<Integer>();
	
	//Les stats
	static int nbVerbe=0, nbMot=0, nbPhrase=0, nbVerbeMatchTreelex=0, nbVerbeMatchXML=0;

	//les elements d'utilisations
	static boolean dejaAppele = false; // mis à true lors du premier appel
	static int nbVerbeMatch=0, nbVerbeNoMatch=0;
	
	//v1
	static String resultPath = "./srl/result/";
	static String statPath = "./srl/result/stat";
	static String mappingPath = "./srl/mapping/";
	static String propbankPath = "./srl/ressources/propbank/data/frames";
	static String transitifPath = "./srl/ressources/data/transitif.txt";
	static String perceptionPath = "./srl/ressources/data/perception.txt";
	static String treelexPath = "./srl/ressources/TreeLex/dico.treelex.revu2.txt";
	static String dicovalencePath = "./srl/ressources/dicovalence/latest_utf8.txt";
	static String p7Path = "./srl/ressources/P7_DEPENDENCY_TREEBANK/0000ftb.dep2.conll";
	static String p7VerbPath = "./srl/ressources/propbank-p7/frames-fr/";
	private String filePath ;
	static ArrayList<Verb> listVerb = new ArrayList<Verb>();
	static ArrayList<Dicovalence> listDicovalence = new ArrayList<Dicovalence>();
	static ArrayList<Treelex> listTreelex = new ArrayList<Treelex>();
	private static ArrayList<String> listUnfind = new ArrayList<String>();
	private static ArrayList<String> listVerbP7 = new ArrayList<String>();
	private static ArrayList<String> listPhrase = new ArrayList<String>();
	private static ArrayList<String> listFonctionParasite = new ArrayList<String>();
	private static ArrayList<String> listVerbNonTraite = new ArrayList<String>();
	private static ArrayList<String> listVerbfail = new ArrayList<String>();
	
	private void loadTreelex() {
		try{
			// Création du flux bufférisé sur un FileReader, immédiatement suivi par un 
			// try/finally, ce qui permet de ne fermer le flux QUE s'il le reader
			// est correctement instancié (évite les NullPointerException)
			BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(treelexPath),Charset.forName("UTF-8")));

			try {

				String currentVerb = "", line;
				boolean transitif = false; 
				while ((line = buff.readLine()) != null) {
					ArrayList<String> currentListCadre = new ArrayList<String>();
					line = line.trim();
					if (!line.contains("all tokens:") && !line.contains("all verbs \\(types\\)") ){
						if (line.startsWith("==="))
						{
							currentVerb = line.split(" \\(")[0].replace("===", "");
							transitif = false;
						}
						else if (line.length()==0 && !currentVerb.equals("") && !currentListCadre.isEmpty()){
							currentVerb = "";
						}
						else if (line.length()!=0) {
							line = line.replace(" :", ":");
							line = line.replace(": ", ":");
							line = line.replace(",", ", ");
							line = line.replace(",  ", ", ");
							line = line.replace("))", ")");
							line = line.replaceAll("\\([0-9]*\\)", "");
							line = line.replaceAll("0", "O");
							
							String[] couple = new String[2];
							couple[0] = currentVerb;
							couple[1] = line.trim();
							Verb.listCadresTreeLex.add(couple);
							if (!transitif && couple[1].contains(" OBJ")){
								Verb.listTransitif.add(currentVerb);
								transitif = true;
							}
						}
					}
				}
				File dirResult = new File (resultPath);

				if (!dirResult.isDirectory())
					dirResult.mkdirs();
				FileWriter fileVerb = null;
				try{
					fileVerb = new FileWriter(resultPath+"/listecadre", true);
					for (String[] currentCouple : Verb.listCadresTreeLex) {
					    fileVerb.write(currentCouple[0]+" : "+currentCouple[1]+"\n");
					}
				}catch(IOException ex){
					ex.printStackTrace();
				}finally{
					if(fileVerb != null){
						try {
							fileVerb.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				FileWriter fileVerbTrans = null;
				try{
					fileVerbTrans = new FileWriter(resultPath+"/listetransitif", true);					
					for (String currentTransitif : Verb.listTransitif) {
						fileVerbTrans.write(currentTransitif+"\n");
					}
				}catch(IOException ex){
					ex.printStackTrace();
				}finally{
					if(fileVerbTrans != null){
						try {
							fileVerbTrans.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} finally {
				// dans tous les cas, on ferme nos flux
				buff.close();
			}
		} catch (IOException ioe) {
			// erreur de fermeture des flux
			System.out.println("Erreur --" + ioe.toString());
		}
		
		if (detdebug) {
			// debug
			try {
				PrintWriter ff = new PrintWriter(new OutputStreamWriter(new FileOutputStream("debug.txt"),Charset.forName("UTF-8")));
				for (int i=0;i<Verb.listCadresTreeLex.size();i++) {
					ff.println("cadre "+i+" "+Arrays.toString(Verb.listCadresTreeLex.get(i)));
				}
				ff.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Srl() {
		super();
	}

	private static void writeVerb() {
		// TODO Auto-generated method stub
		File dirResult = new File (resultPath);

		if (!dirResult.isDirectory())
			dirResult.mkdirs();

		// For each verb in listVerb 
		for(Verb currentVerb : listVerb){
			// Look if first letter folder exist, create if not
			char firstLetter = currentVerb.getLemma().toLowerCase().charAt(0);
			File dirLetter = new File (resultPath+"/"+firstLetter);

			if (!dirLetter.isDirectory())
				dirLetter.mkdirs();
			// Look if lemma file exist, create if not
			// Add to file the info of the verb
			FileWriter fileVerb = null;
			try{
				fileVerb = new FileWriter(resultPath+"/"+firstLetter+"/"+currentVerb.getLemma().toLowerCase(), true);
				fileVerb.write(currentVerb.toString()+"\n");
			}catch(IOException ex){
				ex.printStackTrace();
			}finally{
				if(fileVerb != null){
					try {
						fileVerb.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		FileWriter fileVerbUnFind = null;
		try{
			fileVerbUnFind = new FileWriter(resultPath+"/verbUnFind", true);
			for (String unFind : listUnfind)
			{
				fileVerbUnFind.write(unFind.toString()+"\n");
			}
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(fileVerbUnFind != null){
				try {
					fileVerbUnFind.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		FileWriter fileFonctionParasite = null;
		try{
			fileFonctionParasite = new FileWriter(resultPath+"/fonctionParasite", true);
			for (String parasite : listFonctionParasite)
			{
				fileFonctionParasite.write(parasite.toString()+"\n");
			}
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(fileFonctionParasite != null){
				try {
					fileFonctionParasite.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public List<DetGraph> changeGraph(int typeGraph, List<DetGraph> currentGraph, String filename) {
		//le graphe de retour
		List<DetGraph> retour = null;
		
		
		//on test s'il s'agit du premier appel
		if (!dejaAppele)
		{
			boolean existsConf = (new File("./srl/config.conf")).exists(); 
			if (existsConf) {
				loadConf();
			}
			boolean exists = (new File(resultPath+"xml")).exists(); 
			if (!exists) {
				loadXML();
			}
			loadTreelex();
			loadTransitif();
			loadPerception();
			//on génère les 6 graphes
			//graphNormal = currentGraph;
			
			//genereGraphe2(currentGraph);
			genereGraphe(currentGraph, filename);
			//treatAllGraphe(currentGraph);
			dejaAppele = true;
		}
		switch (typeGraph){
		case 1: //cas ou on demande l'affichage du graphe normal
			retour = graphNormal;
			break;
			
		case 2: //cas ou on demande l'affichage du graphe srl calculé
			retour = graphSrl;
			break;
			
		case 3: //cas ou on demande l'affichage du graphe des phrases non matché
			retour = graphNoMatchNormal();
			break;
			
		case 4: //cas ou on demande l'affichage du graphe des phrases non matché avec le graphe de dépendance total
			retour = graphNoMatchTreelex();
			break;
			
		case 5: //cas ou on demande l'affichage du graphe des phrases matché
			retour = graphMatchNormal();
			break;
			
		case 6: //cas ou on demande l'affichage du graphe des phrases matché avec le graphe de dépendance total
			retour = graphMatchTreelex();
			break;
			
		case 7: //cas ou on demande l'affichage du graphe des phrases non matché
			retour = graphNoMatchXMLNormal();
			break;
			
		case 8: //cas ou on demande l'affichage du graphe des phrases non matché avec le graphe de dépendance total
			retour = graphNoMatchXML();
			break;
			
		case 9: //cas ou on demande l'affichage du graphe des phrases matché
			retour = graphMatchXMLNormal();
			break;
			
		case 10: //cas ou on demande l'affichage du graphe des phrases matché avec le graphe de dépendance total
			retour = graphMatchXML();
			break;
			
		default: //cas ou on demande l'affichage du graphe srl calculé
			retour = graphSrl;
			break;
		
		}
		return retour;
	}
	
	/*
	private void treatAllGraphe(LinkedList<DetGraph> currentGraph) {
		ArrayList<Sentence> listPhrase = new ArrayList<Sentence>();
		for (int i=0;i<currentGraph.size();i++) {
			//System.out.println(currentGraph.get(i));
			//out.processGraph(currentGraph.get(i));
			//treatPhraseGraph(currentGraph.get(i), i);
			listPhrase.add(new Sentence(currentGraph.get(i), i));
			nbPhrase++;
		}
		treatStat();
	}*/

	private void treatPhraseGraph(DetGraph phraseGraph, int currentPhrase) {
		// TODO Auto-generated method stub
		ArrayList<String[]> listMot = new ArrayList<String[]>();
		ArrayList<ArrayList<String>> listMot2 = new ArrayList<ArrayList<String>>();
		DetGraph g1, g2, g3m, g4m, g3nm, g4nm;
		boolean phraseMatchTreelex = true, phraseMatchXML = true;
		int numPhrase = currentPhrase +1;
		
		//on génère le tableau CONLL du mot
		listMot = getTabMot(phraseGraph);
		//on genere aussi la liste2 (a fusionner avec la une ensuite)
		for (String[] currentWord : listMot){
			ArrayList<String> listEltMot = new ArrayList<String>();
			for (String elt : currentWord)
				listEltMot.add(elt);
			listMot2.add(listEltMot);
		}
		//on récupère du main le graphe de la phrase tois fois
		g1 = phraseGraph.getSubGraph(0); // graphe complet avec les dep normal
		g2 = phraseGraph.getSubGraph(0); // graphe avec les dep Srl complete
		g3m = phraseGraph.getSubGraph(0); // graphe complet avec les resultat de treelex
		g4m = phraseGraph.getSubGraph(0); // graphe avec les resultat de XML
		g3nm = phraseGraph.getSubGraph(0); // graphe complet avec les resultat de treelex
		g4nm = phraseGraph.getSubGraph(0); // graphe avec les resultat de XML
		
		//on efface les dependances
		g2.clearDeps();
		g3m.clearDeps();
		g4m.clearDeps();
		g3nm.clearDeps();
		g4nm.clearDeps();
		
		int nbPred = 0; // nombre de predicat

		//on traite la phrase pour les dep srl
		//Pour chaque mot de la phrase
		for (String[] currentWord : listMot){
			//Si c'est un verbe, 
			if (
				(currentWord[3].equals("V") || currentWord[3].equals("VINF") || currentWord[3].equals("VPP") || currentWord[3].equals("VPR") || currentWord[3].equals("VS")) &&
				!(currentWord[7].equals("aux_pass") || currentWord[7].equals("aux_tps"))){
				ArrayList<DepSrl> listeDep = new ArrayList<DepSrl>(); //liste des depdendant du verbe courant

				//on cherche les dependances du verbe
				for (String[] line2 : listMot) {
					if (line2[6].equals(currentWord[0]) &&      //est un dependant
						!line2[7].equals("coord") &&            // on ne tient pas compte des elt suivants
						!line2[7].equalsIgnoreCase("ponct") && 
						!line2[7].equals("mod") && 
						!line2[7].equals("aux_tps") && 
						!line2[7].equalsIgnoreCase("PONCT") && 
						!line2[7].equalsIgnoreCase("dep") && 
						!line2[7].equals("det") && 
						!line2[7].equalsIgnoreCase("mod_rel")){
						//on ajoute la dependance à la liste
						listeDep.add(new DepSrl(Integer.parseInt(line2[0])-1, 
												currentPhrase, 
												Integer.parseInt(currentWord[0])-1, 
												line2[1], 
												line2[2], 
												line2[3], 
												line2[4], 
												line2[7]));
						//on map les dependances trouvées (fait lors de la construction de la dependance ?
					}
				}
				//on crée le verbe
				//on ajoute les deps au verbe (dans le constructeur)
				int idDuVerbe = Integer.parseInt(currentWord[0]);
				Verb currentVerb = new Verb(idDuVerbe, 
											currentPhrase, 
											Integer.parseInt(currentWord[0])-1, 
											currentWord[1], 
											currentWord[2], 
											currentWord[3], 
											currentWord[4], 
											currentWord[7],
											listeDep,
											listMot);
				//on traite les dependances
				currentVerb.treatArg();
				currentVerb.treatVerb();
				//on cherche les cadres
				currentVerb.researchCadre();
				//on creer les dependance dans le graphe
				for (int i=0; i<currentVerb.getListeDep().size(); i++){
					//pour chaque dependance, on créé le lien dans les graphes qu'il faut :
					//g2 dans tous les cas
					String currentSousCadre = currentVerb.getListeDep().get(i).getFonction()+":"+ currentVerb.getListeDep().get(i).getCat2();
					String currentSousCadreSem = currentSousCadre;
					
					//On regarde si le cadre sementique est trouvé pour modifier les labels
					if (currentVerb.isMatchXML() && currentVerb.getCadreTrouve().split(", ")[0].split(":").length == 3)
					{
						String[] tabSousCadre = currentVerb.getCadreTrouve().split(", ");
						for (String sousCadre : tabSousCadre)
						{
							if (sousCadre.contains(currentSousCadre))
							{
								sousCadre = sousCadre.replace(")", "");
								sousCadre = sousCadre.replace("(", "");
								currentSousCadreSem = sousCadre;
								String role = "_";
								if (currentSousCadreSem.split(":").length==3)
									role = currentSousCadreSem.split(":")[2];
									                                   
								currentVerb.getListeDep().get(i).setRole(role);
								//currentSousCadre = sousCadre.split(":")[2];
								//System.out.println(currentSousCadre + "" +currentVerb.getListeDep().get(i).getRole());
								break;
							}	
						} 
					}
					
														
					// TRAITEMENT DU GRAPHE TREELEX
					//On ajoute la dep à g6 si le cadre n'a pas été trouvé
					if (!currentVerb.isMatchTreelex())
					{
						g3nm.ajoutDep(currentSousCadre, 
								currentVerb.getListeDep().get(i).getId(), 
								currentVerb.getListeDep().get(i).getIdVerbe());
					}
					else
					{
						//g4 si le verbe est trouvé
						g3m.ajoutDep(currentSousCadre, 
								currentVerb.getListeDep().get(i).getId(), 
								currentVerb.getListeDep().get(i).getIdVerbe());
					}
					
					//CAS ou le cadre est trouvé dans les fichiers XML
					if (!currentVerb.isMatchXML())
					{
						g4nm.ajoutDep(currentSousCadreSem, 
								currentVerb.getListeDep().get(i).getId(), 
								currentVerb.getListeDep().get(i).getIdVerbe());
					}
					else
					{
						//g4 si le verbe est trouvé
						g4m.ajoutDep(currentSousCadreSem, 
								currentVerb.getListeDep().get(i).getId(), 
								currentVerb.getListeDep().get(i).getIdVerbe());
					}
					
					//Dans tous les cas on ajoute les dependance dans g2
					g2.ajoutDep(currentSousCadreSem,
								currentVerb.getListeDep().get(i).getId(), 
								currentVerb.getListeDep().get(i).getIdVerbe());
				}
				if (!currentVerb.isMatchTreelex()){
					phraseMatchTreelex = false;
					nbVerbeNoMatch ++;
					listUnfind.add(currentVerb.getIdSentence()+"\t"+currentVerb.getLemma()+'\t'+currentVerb.getCadre());
					//System.out.println("Verbe No Match "+currentVerb.getLemma());
				}
				else
				{
					nbVerbeMatch++;
					nbVerbeMatchTreelex++;
				}
				if (!currentVerb.isMatchXML()){
					phraseMatchXML = false;
				}
				else
				{
					int idVerbe = Integer.parseInt(currentWord[0])-1;
					String fillpred = "=Y";
					if (listMot2.get(idVerbe).size()==10)
					{
						//on ajoute en fin de tableau
						listMot2.get(idVerbe).add(fillpred);
					}
					else
					{
						//on modifie la colonne 10 du tableau
						listMot2.get(idVerbe).set(10, fillpred);
					}
					
					int nbColPred = (11+nbPred*2);
					if (listMot2.get(idVerbe).size()<=nbColPred)
					{
						while (listMot2.get(idVerbe).size()<nbColPred)
							listMot2.get(idVerbe).add("_");
							
						//on ajoute en fin de tableau
						listMot2.get(idVerbe).add(currentVerb.getSens());
					}
					else
					{
						//on modifie la colonne 10 du tableau
						listMot2.get(idVerbe).set(nbColPred, currentVerb.getSens());
					}
					
					//on gère les dépendants

					for (int i=0; i<currentVerb.getListeDep().size(); i++)
					{
						int idDep = currentVerb.getListeDep().get(i).getId();
						String role = currentVerb.getListeDep().get(i).getRole();

						System.out.println("DEBUG ROLE "+role);
						
						nbColPred = (12+nbPred*2);
						if (listMot2.get(idDep).size()<=nbColPred)
						{
							while (listMot2.get(idDep).size()<nbColPred)
								listMot2.get(idDep).add("_");
								
							//on ajoute en fin de tableau
							listMot2.get(idDep).add(role);
						}
						else
						{
							//on modifie la colonne 10 du tableau
							listMot2.get(idDep).set(nbColPred, role);
						}
					}
					nbVerbeMatchXML++;
					nbPred++;
				}
				listVerb.add(currentVerb);
				nbVerbe ++;
				//on ajoute le verbe à la liste des verbes
			}
		}
		//on ajoute les graphes gX aux listes de graphes
		graphNormal.add(g1);
		graphSrl.add(g2);
		if (phraseMatchTreelex)
		{
			graphTreelex.add(g3m);
			matchTreelex.add(numPhrase);
		}
		else
		{
			graphTreelex.add(g3nm);
			noMatchTreelex.add(numPhrase);
		}
		if (phraseMatchXML)
		{
			graphXML.add(g4m);
			matchXML.add(numPhrase);
		}
		else
		{
			graphXML.add(g4nm);
			noMatchXML.add(numPhrase);
		}
		listMot.clear();
		writeCONLL(listMot2);
		listMot2.clear();
		nbPred=0;
	}

	private ArrayList<String[]> getTabMot(DetGraph phraseGraph) {
		ArrayList<String[]> listMot = new ArrayList<String[]>();
		String sourceHead = "UNK";
		for (int i=0;i<phraseGraph.getNbMots();i++) {
			Mot m = phraseGraph.getMot(i);
			String lemme = m.getLemme();
			String forme = m.getForme();
			String tag = m.getPOS();

			if (forme==null||forme.length()==0) {
				forme = "UNK"; lemme = "UNK"; tag = "UNK";
			} else if (lemme==null||lemme.length()==0)
				lemme = ""+forme;
			if (tag==null||tag.length()==0) tag="UNK";

			sourceHead=tag;

			int[] deps = phraseGraph.getDeps(i);
			if (deps.length<=0) {
				String[] mots = {
						(i+1)+"",
						forme,
						lemme,
						sourceHead,
						tag,
						"_",
						"0",
						"ROOT",
						"_",
						"_"
				};
				listMot.add(mots);
			} else if (deps.length>1){
				// cas multiple heads
				System.out.println("erreur");
			} else {
				int dep = deps[0];
				String rel = phraseGraph.getDepLabel(dep);
				int head = phraseGraph.getHead(dep);
				Mot mhead = phraseGraph.getMot(head);
				int mheadidx;
				if (mhead==null) mheadidx = 0;
				else mheadidx=mhead.getIndexInUtt();
				String[] mots = {
						(i+1)+"",
						forme,
						lemme,
						sourceHead,
						tag,
						"_",
						mheadidx+"",
						rel,
						"_",
						"_"
				};
				listMot.add(mots);
			}
		}
		return listMot;
	}

	private void loadPerception() {
		String filePath = perceptionPath;

		Scanner scanner;
		try {
			scanner = new Scanner(new File(filePath),"UTF-8");
			// On boucle sur chaque champ detecté
			while (scanner.hasNextLine()) {
			    String line = scanner.nextLine();
			    Verb.listPerception.add(line);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void loadTransitif() {
		String filePath = transitifPath;

		Scanner scanner;
		try {
			scanner = new Scanner(new File(filePath),"UTF-8");

			// On boucle sur chaque champ detecté
			while (scanner.hasNextLine()) {
			    String line = scanner.nextLine();
			    Verb.listTransitif.add(line);
			}
	
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void loadConf() {
		String filePath = "./srl/config.conf";

		Scanner scanner;
		try {
			scanner = new Scanner(new File(filePath));

			// On boucle sur chaque champ detecté
			while (scanner.hasNextLine()) {
			    String line = scanner.nextLine();
			    
			    //faites ici votre traitement
			    if (line.startsWith("TL"))
			    {
			    	treelexPath = line.substring(3);
			    }
			    else if (line.startsWith("PBK"))
			    {
			    	propbankPath = line.substring(4)+"data/frames";
			    }
			    else if (line.startsWith("PBKP7"))
			    {
			    	propbankPath = line.substring(4)+"frames-fr/";
			    }
			    else if (line.startsWith("DV"))
			    {
			    	dicovalencePath = line.substring(3);
			    }
			    else if (line.startsWith("P7"))
			    {
			    	p7Path = line.substring(3);
			    }
			    else if (line.startsWith("RP"))
			    {
			    	resultPath = line.substring(3);
			    }
			    else if (line.startsWith("STP"))
			    {
			    	statPath = line.substring(4);
			    }
			    else if (line.startsWith("MP"))
			    {
			    	mappingPath = line.substring(3);
			    }
			}
	
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	private LinkedList<DetGraph> graphMatchTreelex() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : matchTreelex)
		{
			DetGraph graph;
			graph = graphTreelex.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private LinkedList<DetGraph> graphMatchNormal() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : matchTreelex)
		{
			DetGraph graph;
			graph = graphNormal.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private LinkedList<DetGraph> graphNoMatchTreelex() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : noMatchTreelex)
		{
			DetGraph graph;
			graph = graphTreelex.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private LinkedList<DetGraph> graphNoMatchNormal() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : noMatchTreelex)
		{
			DetGraph graph;
			graph = graphNormal.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}
	
	
	private LinkedList<DetGraph> graphMatchXML() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : matchXML)
		{
			DetGraph graph;
			graph = graphXML.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private LinkedList<DetGraph> graphMatchXMLNormal() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : matchXML)
		{
			DetGraph graph;
			graph = graphNormal.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private LinkedList<DetGraph> graphNoMatchXML() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : noMatchXML)
		{
			DetGraph graph;
			graph = graphXML.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private LinkedList<DetGraph> graphNoMatchXMLNormal() {
		LinkedList<DetGraph> retour = new LinkedList<DetGraph>();
		for (int numPhrase : noMatchXML)
		{
			DetGraph graph;
			graph = graphNormal.get(numPhrase);
			retour.add(graph);
		}
		return retour;
	}

	private void genereGraphe2(LinkedList<DetGraph> mainGraph) {
		for (DetGraph graph : mainGraph){
			for (int i=0;i<graph.getNbMots();i++) {
				Mot m = graph.getMot(i);
				// pour avoir le POStag:
				String postag = m.getPOS(); // .mot pour la forme, et aussi .lemme
				String mot = m.getForme(); // .mot pour la forme, et aussi .lemme
				String lemme = m.getLemme(); // .mot pour la forme, et aussi .lemme
				// pour acceder au HEAD et a la DEP:
				int dep = graph.getDep(i);
				if (postag.equals("V"))
				{
					System.out.print("Verb " + lemme+" ");
					if (dep>=0) {
						int head = graph.getHead(dep); // indice du HEAD
						Mot dependant = graph.getMot(head);
						String label = graph.getDepLabel(dep);
						System.out.print(label+":"+dependant.getPOS()+", ");
					} // sinon, pas de DEP
					System.out.println(" ");
				}
			}
		}
	}

	private void genereGraphe(List<DetGraph> mainGraph, String filename) {
		//Cette méthode vise à générer les graphes
		//on copie dans le premier graphe le mainGraph
		DetGraph g1, g2, g3m, g4m, g3nm, g4nm;
		int numLine=0, numPhrase=0, currentPhrase =0;
		String line;
		ArrayList<String[]> listMot = new ArrayList<String[]>();
		ArrayList<ArrayList<String>> listMot2 = new ArrayList<ArrayList<String>>();
		boolean phraseMatchTreelex = true, phraseMatchXML = true;
		
		//suppression du fichier CONLL de resultat
		File path = new File(resultPath+"/resultSense.conll");
		if (path.exists()) {
			path.delete();
		}
		
		try{
			// Création du flux bufférisé sur un FileReader, immédiatement suivi par un 
			// try/finally, ce qui permet de ne fermer le flux QUE s'il le reader
			// est correctement instancié (évite les NullPointerException)
			BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(filename),Charset.forName("UTF-8")));
			try {
				// lecture du fichier
				// division en phrases
				// traitement des phrases
				DetGraph graph;
				while ((line = buff.readLine()) != null) {
					numLine++;
					String[] ss = line.split("\t");
					if  (!ss[0].isEmpty()) {
						nbMot++;
					}
					// xtof: test sur la fin de phrase plutot que le debut de la phrase suivante !
					if (line.trim().length()==0) {
//						if (ss.length==10 && Integer.parseInt(ss[0])==1) {
//						if (numPhrase>0){ //test pour ne pas faire le traitement sur la premiere phrase
						{
							//ici on traite la phrase
//							currentPhrase = numPhrase - 1;
							currentPhrase = numPhrase;
							phraseMatchTreelex = true;
							phraseMatchXML = true;
							
							graph = mainGraph.get(currentPhrase);
							
							//on récupère du main le graphe de la phrase tois fois
							g1 = graph.getSubGraph(0); // graphe complet avec les dep normal
							g2 = graph.getSubGraph(0); // graphe avec les dep Srl complete
							g3m = graph.getSubGraph(0); // graphe complet avec les resultat de treelex
							g4m = graph.getSubGraph(0); // graphe avec les resultat de XML
							g3nm = graph.getSubGraph(0); // graphe complet avec les resultat de treelex
							g4nm = graph.getSubGraph(0); // graphe avec les resultat de XML
							
							//on efface les dependances
							g2.clearDeps();
							g3m.clearDeps();
							g4m.clearDeps();
							g3nm.clearDeps();
							g4nm.clearDeps();
							
							int nbPred = 0; // nombre de predicat

							//on traite la phrase pour les dep srl
							//Pour chaque mot de la phrase
							for (String[] currentWord : listMot){
								//Si c'est un verbe, 
								if (((currentWord[3].equals("V"))) && !(currentWord[7].equals("aux_pass") || currentWord[7].equals("aux_tps"))){
									ArrayList<DepSrl> listeDep = new ArrayList<DepSrl>(); //liste des depdendant du verbe courant

									//on cherche les dependances du verbe
									for (String[] line2 : listMot) {
										if (line2[6].equals(currentWord[0]) &&      //est un dependant
											!line2[7].equals("coord") &&            // on ne tient pas compte des elt suivants
											!line2[7].equalsIgnoreCase("ponct") && 
											!line2[7].equals("mod") && 
											!line2[7].equals("aux_tps") && 
											!line2[7].equalsIgnoreCase("PONCT") && 
											!line2[7].equalsIgnoreCase("dep") && 
											!line2[7].equals("det") && 
											!line2[7].equalsIgnoreCase("mod_rel")){
											//on ajoute la dependance à la liste
											listeDep.add(new DepSrl(Integer.parseInt(line2[0])-1, 
																	currentPhrase, 
																	Integer.parseInt(currentWord[0])-1, 
																	line2[1], 
																	line2[2], 
																	line2[3], 
																	line2[4], 
																	line2[7]));
											//on map les dependances trouvées (fait lors de la construction de la dependance ?
										}
									}
									
									//on crée le verbe
									//on ajoute les deps au verbe (dans le constructeur)
									int idDuVerbe = numLine - (listMot.size()-Integer.parseInt(currentWord[0]));
									Verb currentVerb = new Verb(idDuVerbe, 
																currentPhrase, 
																Integer.parseInt(currentWord[0])-1, 
																currentWord[1], 
																currentWord[2], 
																currentWord[3], 
																currentWord[4], 
																currentWord[7],
																listeDep,
																listMot);
									
									//on traite les dependances
									currentVerb.treatArg();
									currentVerb.treatVerb();
									
									//on cherche les cadres _syntaxiques_ 
									currentVerb.researchCadre();
									//on creer les dependance dans le graphe
									for (int i=0; i<currentVerb.getListeDep().size(); i++){
										//pour chaque dependance, on créé le lien dans les graphes qu'il faut :
										//g2 dans tous les cas
										String currentSousCadre = currentVerb.getListeDep().get(i).getFonction()+":"+ currentVerb.getListeDep().get(i).getCat2();
										String currentSousCadreSem = currentSousCadre;
										
										//On regarde si le cadre sementique est trouvé pour modifier les labels
										if (currentVerb.isMatchXML() && currentVerb.getCadreTrouve().split(", ")[0].split(":").length == 3)
										{
											String[] tabSousCadre = currentVerb.getCadreTrouve().split(", ");
											for (String sousCadre : tabSousCadre)
											{
												if (sousCadre.contains(currentSousCadre))
												{
													sousCadre = sousCadre.replace(")", "");
													sousCadre = sousCadre.replace("(", "");
													currentSousCadreSem = sousCadre;
													String role = "_";
													if (currentSousCadreSem.split(":").length==3)
														role = currentSousCadreSem.split(":")[2];
													
													currentVerb.getListeDep().get(i).setRole(role);
													//currentSousCadre = sousCadre.split(":")[2];
													//System.out.println(currentSousCadre + "" +currentVerb.getListeDep().get(i).getRole());
													break;
												}	
											} 
										}
										
																			
										// TRAITEMENT DU GRAPHE TREELEX
										//On ajoute la dep à g6 si le cadre n'a pas été trouvé
										if (!currentVerb.isMatchTreelex())
										{
											g3nm.ajoutDep(currentSousCadre, 
													currentVerb.getListeDep().get(i).getId(), 
													currentVerb.getListeDep().get(i).getIdVerbe());
										}
										else
										{
											//g4 si le verbe est trouvé
											g3m.ajoutDep(currentSousCadre, 
													currentVerb.getListeDep().get(i).getId(), 
													currentVerb.getListeDep().get(i).getIdVerbe());
										}
										
										//CAS ou le cadre est trouvé dans les fichiers XML
										if (!currentVerb.isMatchXML())
										{
											g4nm.ajoutDep(currentSousCadreSem, 
													currentVerb.getListeDep().get(i).getId(), 
													currentVerb.getListeDep().get(i).getIdVerbe());
										}
										else
										{
											//g4 si le verbe est trouvé
											g4m.ajoutDep(currentSousCadreSem, 
													currentVerb.getListeDep().get(i).getId(), 
													currentVerb.getListeDep().get(i).getIdVerbe());
											//il faut ajouter à listMot le resultat
											/*
											int idDep = currentVerb.getListeDep().get(i).getId();
											while (listMot2.get(idDep).size()<(11+nbPred+2))
											{
												listMot2.get(idDep).add("_");
												//System.out.println(listMot2.get(idVerbe));
											}
											listMot2.get(idDep).add((12+nbPred*2), currentSousCadreSem.split(":")[2]);
											*/
										}
										
										//Dans tous les cas on ajoute les dependance dans g2
										g2.ajoutDep(currentSousCadreSem,
													currentVerb.getListeDep().get(i).getId(), 
													currentVerb.getListeDep().get(i).getIdVerbe());
									}
									if (!currentVerb.isMatchTreelex()){
										phraseMatchTreelex = false;
										nbVerbeNoMatch ++;
										listUnfind.add(currentVerb.getIdSentence()+"\t"+currentVerb.getLemma()+'\t'+currentVerb.getCadre());
										//System.out.println("Verbe No Match "+currentVerb.getLemma());
									} else {
										nbVerbeMatch++;
										nbVerbeMatchTreelex++;
									}
									if (!currentVerb.isMatchXML()){
										phraseMatchXML = false;
									} else {
										int idVerbe = Integer.parseInt(currentWord[0])-1;
										String fillpred = "=Y";
										if (listMot2.get(idVerbe).size()==10)
										{
											//on ajoute en fin de tableau
											listMot2.get(idVerbe).add(fillpred);
										}
										else
										{
											//on modifie la colonne 10 du tableau
											listMot2.get(idVerbe).set(10, fillpred);
										}
										
										if (listMot2.get(idVerbe).size()==11)
										{
											while (listMot2.get(idVerbe).size()<11)
												listMot2.get(idVerbe).add("_");
												
											//on ajoute en fin de tableau
											listMot2.get(idVerbe).add(currentVerb.getSens());
										}
										else
										{
											//on modifie la colonne 10 du tableau
											listMot2.get(idVerbe).set(11, currentVerb.getSens());
										}
										
										//on gère les dépendants
										int nbColPred = (12+nbPred);
										
										String[] tabSens = getDepSens(g4m, idVerbe);

										for (int i=0; i<tabSens.length; i++)
										{
											int idDep = Integer.parseInt(tabSens[i].split(":")[0]);
											if (idDep != idVerbe)
											{
												if (listMot2.get(idDep).size()<=nbColPred)
												{
													while (listMot2.get(idDep).size()<nbColPred)
														listMot2.get(idDep).add("_");
														
													//on ajoute en fin de tableau
													listMot2.get(idDep).add(tabSens[i].split(":")[1]);
												}
												else
												{
													//on modifie la colonne 10 du tableau
													listMot2.get(idDep).set(nbColPred, tabSens[i].split(":")[1]);
												}
											}
										}
										nbVerbeMatchXML++;
										nbPred++;
									}
									listVerb.add(currentVerb);
									nbVerbe ++;
									//on ajoute le verbe à la liste des verbes
								}
							}
							//on ajoute les graphes gX aux listes de graphes
							graphNormal.add(g1);
							graphSrl.add(g2);
							if (phraseMatchTreelex)
							{
								graphTreelex.add(g3m);
								matchTreelex.add(numPhrase);
							}
							else
							{
								graphTreelex.add(g3nm);
								noMatchTreelex.add(numPhrase);
							}
							if (phraseMatchXML)
							{
								graphXML.add(g4m);
								matchXML.add(numPhrase);
							}
							else
							{
								graphXML.add(g4nm);
								noMatchXML.add(numPhrase);
							}
							listMot.clear();
							writeCONLL(listMot2);
							listMot2.clear();
							nbPred=0;
						}
						numPhrase++;
						nbPhrase++;
					}
					if (ss.length>1)
					{
						ArrayList<String> listEltMot = new ArrayList<String>();
						for (String elt : ss)
							listEltMot.add(elt);
						listMot.add(ss);
						listMot2.add(listEltMot);
					}
				}
			} finally {
				// dans tous les cas, on ferme nos flux
				buff.close();
			}
		} catch (IOException ioe) {
			// erreur de fermeture des flux
			System.out.println("Erreur --" + ioe.toString());
		}
		//writeVerb();
		boolean exists = (new File(resultPath+"result.conll")).exists(); 
		if (exists)
		{
			new File(resultPath+"result.conll").delete();
		}
		try {
			Syntex2conll out = new Syntex2conll(new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultPath+"result.conll"),Charset.forName("UTF-8"))));
			for (int i=0;i<graphSrl.size();i++) {
				out.processGraph(graphSrl.get(i));
			}
			out.terminate();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		treatStat();
		/*System.out.println("Number of verb " + nbVerbe);
		System.out.println("Number of verb find and match in treelex " + nbVerbeMatch);
		System.out.println("Number of verb no find or no match in treelex " + nbVerbeNoMatch);*/
	}


	private String[] getDepSens(DetGraph g4m, int idVerbe) {

		ArrayList<String> tab = new ArrayList<String>();
		for (int laDep=0;laDep<g4m.deps.size();laDep++)
		{
			Dep dep = g4m.deps.get(laDep);
			if (dep.head.getIndexInUtt()==(idVerbe+1) && dep.toString().split(":").length==3)
			{
				tab.add((dep.gov.getIndexInUtt()-1)+":"+dep.toString().split(":")[2]);
			}
		}
		String[] r=new String[tab.size()];
		for (int i=0;i<tab.size();i++) r[i] = tab.get(i);
		return r;
	}

	private void writeCONLL(ArrayList<ArrayList<String>> listMot2) {
		FileWriter fileConll = null;
		try{
			fileConll = new FileWriter(resultPath+"/resultSense.conll", true);
			int tailleMax = 0;
			for (ArrayList<String> mot : listMot2)
			{
				if (tailleMax < mot.size())
					tailleMax = mot.size();
			}
			for (ArrayList<String> mot : listMot2)
			{
				while (mot.size()<tailleMax)
					mot.add("_");
				String sortie="";
				for (String elt : mot)
				{
					sortie = sortie.concat(elt+"\t");
				}
				sortie.trim();
				fileConll.write(sortie+"\n");
			}
			fileConll.write("\n");
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(fileConll != null){
				try {
					fileConll.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void treatStat() {
		// TODO Auto-generated method stub
		//On test si le fichier existe
		boolean exists = (new File(statPath)).exists(); 
		String[] tabOld = null;
		String line = null;
		if (exists)
		{
			//on lis les infos
			Scanner scanner;
			try {
				scanner = new Scanner(new File(statPath));
				// On boucle sur chaque champ detecté
				while (scanner.hasNextLine()) {
				    line = scanner.nextLine();
				} 			

				scanner.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				line = "0\t0\t0\t0\t0";
			}

			//on garde en mémoire les dernières info dans info_old
		}
		else
		{
			//on met dans old des infos vide
			line = "0\t0\t1\t0\t0";
		}
		
		tabOld = line.split("\t");
		int nbPhrase_old= Integer.parseInt(tabOld[0]);
		int nbMot_old=Integer.parseInt(tabOld[1]);
		int nbVerbe_old=Integer.parseInt(tabOld[2]);
		int nbVerbeMatchTreelex_old=Integer.parseInt(tabOld[3]);
		int nbVerbeMatchXML_old=Integer.parseInt(tabOld[4]);
		
		float proportionTreelex;
		float proportionTreelex_old;
		float proportionXML;
		float proportionXML_old;
		if (nbVerbe !=0 )
		{
			proportionTreelex = nbVerbeMatchTreelex*100/nbVerbe;
			proportionXML = nbVerbeMatchXML*100/nbVerbe;
		}
		else
		{
			proportionTreelex = 0;
			proportionXML = 0;
		}
		if (nbVerbe_old !=0 )
		{
			proportionTreelex_old = nbVerbeMatchTreelex_old*100/nbVerbe_old;
			proportionXML_old = nbVerbeMatchXML_old*100/nbVerbe_old;
		}
		else
		{
			proportionTreelex_old = 0;
			proportionXML_old = 0;
		}
		
		//on ajoute les infos courante en fin de fichier ssi elles sont différente des _old
		if (nbPhrase_old!=nbPhrase ||
			nbMot_old!=nbMot ||
			nbVerbe_old!=nbVerbe ||
			nbVerbeMatchTreelex_old!=nbVerbeMatchTreelex ||
			nbVerbeMatchXML_old!=nbVerbeMatchXML)
		{
			FileWriter writer = null;
			String texte = nbPhrase+"\t"+nbMot+"\t"+nbVerbe+"\t"+nbVerbeMatchTreelex+"\t"+nbVerbeMatchXML+"\n";
			try{
			     writer = new FileWriter(statPath, true);
			     writer.write(texte,0,texte.length());
			}catch(IOException ex){
			    ex.printStackTrace();
			}finally{
			  if(writer != null){
			     try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
			}
			
			
		}
		//on affiche les infos
		System.out.println("Number of sentence : " + nbPhrase);
		System.out.println("Number of word     : " + nbMot);
		System.out.println("Number of verb     : " + nbVerbe);
		System.out.println("Number of Verb Match in Treelex     : "+ nbVerbeMatchTreelex_old +" -> " + nbVerbeMatchTreelex + "(" + (nbVerbeMatchTreelex-nbVerbeMatchTreelex_old)+")");
		System.out.println("Proportion of Verb Match in Treelex : "+ proportionTreelex_old +" -> " + proportionTreelex + "(" + (proportionTreelex-proportionTreelex_old)+")");
		System.out.println("Number of Verb Match in XML         : "+ nbVerbeMatchXML_old +" -> " + nbVerbeMatchXML + "(" + (nbVerbeMatchXML-nbVerbeMatchXML_old)+")" );
		System.out.println("Proportion of Verb Match in Treelex : "+ proportionXML_old +" -> " + proportionXML + "(" + (proportionXML-proportionXML_old)+")");
		
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		int borneInf = 0;
		int borneSup = -10;
		
		/*loadP7(borneInf, borneSup);
		
		loadTreelexBis();
		loadDicovalence();
		fusionLexicon();*/
		loadXML();
		
	}
	
	
	private static void loadXML() {
		// TODO Auto-generated method stub
		File dirResult = new File (resultPath+"xml");

		if (!dirResult.isDirectory())
			dirResult.mkdirs();
		
		File repertoire = new File (p7VerbPath);
		convertXml(repertoire);

		//createXml("./srl/ressources/propbank-p7/frames-fr/f/faire");
		//createXml("./srl/ressources/propbank-p7/frames-fr/l/lier");
		
		FileWriter fileVerbUnFind = null;
		try{
			Collections.sort(listVerbNonTraite);
			fileVerbUnFind = new FileWriter(resultPath+"/xml/verbNonTraite", false);
			for (String unFind : listVerbNonTraite)
			{
				fileVerbUnFind.write(unFind.toString()+"\n");
			}
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(fileVerbUnFind != null){
				try {
					fileVerbUnFind.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		FileWriter fileVerbfail = null;
		try{
			Collections.sort(listVerbfail);
			fileVerbfail = new FileWriter(resultPath+"/xml/verbFail", false);
			for (String unFind : listVerbfail)
			{
				fileVerbfail.write(unFind.toString()+"\n");
			}
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(fileVerbfail != null){
				try {
					fileVerbfail.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static void convertXml ( File repertoire ) {
		//System.out.println ( repertoire.getAbsolutePath());

		if ( repertoire.isDirectory ( ) ) {
			File[] list = repertoire.listFiles();
			if (list != null){
				for ( int i = 0; i < list.length; i++) {
					// Appel récursif sur les sous-répertoires
					if (!list[i].getName().equals(".svn"))
						convertXml( list[i]);
				} 
			} else {
				System.err.println(repertoire + " : Erreur de lecture.");
			}
		} 
		else if (repertoire.isFile())
		{
			String[] tabRepParent = repertoire.getParent().split("/");
			String nomRepParent = tabRepParent[tabRepParent.length-1];
			if (nomRepParent.length()==1)
			{
				//System.out.println ( repertoire.getParent());
				createXml(repertoire.getAbsolutePath());
			}
			else
			{
				//System.out.println ( repertoire.getName());
			}
		}
	} 

	private static void createXml(String absolutePath) {

		try{
			
			//préparation des principaux elements
			String nomVerbe = absolutePath.split("/")[absolutePath.split("/").length-1];
			char firstLetter = nomVerbe.toLowerCase().charAt(0);
			String nomFichier = resultPath+"xml/"+firstLetter+"/"+nomVerbe+".xml";

			File dirResult = new File (resultPath+"xml/"+firstLetter);

			if (!dirResult.isDirectory())
				dirResult.mkdirs();
			
			// Création d'un nouveau DOM
			DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
			DocumentBuilder constructeur = fabrique.newDocumentBuilder();
			Document document = constructeur.newDocument();
			
			// Propriétés du DOM
			document.setXmlVersion("1.0");
			document.setXmlStandalone(true);
			
			// Création de l'arborescence du DOM
			Element racine = document.createElement("predicate");
			racine.setAttribute("lemma", nomVerbe);
			
			Element note = document.createElement("note");
			note.setTextContent("Frames file for 'présenter based on sentences in P7 dependency treebank.");
			racine.appendChild(note);
			
			Element frames = document.createElement("frames");
			
			Element otherframes = document.createElement("otherframes");
			
			//On ouvre le fichier
			
			Scanner scanner=new Scanner(new File(absolutePath));

			// On boucle sur chaque champ detecté
			boolean verbTraite = true;
			String[] tabLine;
			//System.out.println(nomVerbe);
			ArrayList<String> listLigneFrame = new ArrayList<String>();
			while (scanner.hasNextLine() && verbTraite) {
			    String line = scanner.nextLine(), freq;	
				//System.out.println(line);	
				if (line.contains("TL Frames") || line.contains("null") || line.contains("DV Senses") || line.contains("P7 Sentences"))
				{
					verbTraite = false;
				}
				else
				{
					//faites ici votre traitement
				    switch (typeLine(line, nomVerbe))
					{
					case 0: //cas ou la ligne est le lemme
						break;
					case 1: //cas ou la ligne est un cadre semantique
						//System.out.println(line);
						if (!listLigneFrame.isEmpty() && 
							listLigneFrame.get(0).startsWith("syntax") && 
							listLigneFrame.get(1).startsWith("freq"))
						{
							Element syntax = document.createElement("syntax");
							syntax.setAttribute("freq", listLigneFrame.get(1).split("%srl%")[1]);
							syntax.setTextContent(listLigneFrame.get(0).split("%srl%")[1]);
							//System.out.println(listLigneFrame.get(1).split("%srl%")[1]);
							if (listLigneFrame.size()==2 )
							{
								//cas other frame								
								otherframes.appendChild(syntax);
							}
							else
							{
								if (!isTriplet(listLigneFrame.get(0).split("%srl%")[1]))
								{
									listVerbfail.add(nomVerbe);
									//System.out.println(nomVerbe + " "+listLigneFrame.get(0).split("%srl%")[1]);
								}
								//cas frame semantique
								//creation des elements
								Element frame = document.createElement("frame");
								Element roleset = document.createElement("roleset");
								Element roles = document.createElement("roles");
								Element examples = document.createElement("examples");
								
								//recherche du roleset
								for (String ligne : listLigneFrame)
								{
									if (ligne.startsWith("roleset"))
									{
										tabLine = ligne.split("%srl%")[1].split(" ", 2);
										roleset.setAttribute("id", tabLine[0]);
										roleset.setAttribute("descr", tabLine[1]);
									}
									else if (ligne.startsWith("role"))
									{
										tabLine = ligne.split("%srl%")[1].split(" ", 2);
										
										Element role = document.createElement("role");
										role.setAttribute("descr", tabLine[1]);
										role.setAttribute("n", tabLine[0]);
										
										//ajout à roles
										roles.appendChild(role);
									}
									else if (ligne.startsWith("example"))
									{
										Element example = document.createElement("example");
										example.setTextContent(ligne.split("%srl%")[1]);
										
										//ajout à roles
										examples.appendChild(example);
									}
								}
								//liaison des elements
								frame.appendChild(syntax);
								roleset.appendChild(roles);
								frame.appendChild(roleset);
								frame.appendChild(examples);
								frames.appendChild(frame);
							}
							//ici le traitement pour enregistrer la frame dans frame ou otherframe
						}
						listLigneFrame.clear();
						
						tabLine = line.split(", ");
						if (tabLine[tabLine.length-1].split(" \\(").length==2)
						{
							freq = tabLine[tabLine.length-1].split(" \\(")[1].replaceAll("\\)", "");
						}
						else
						{
							freq = "0";
						}
						
						listLigneFrame.add("syntax%srl%"+line.replaceAll("\\([0-9]*\\)", "").trim());
						listLigneFrame.add("freq%srl%"+freq);
						
						break;
					case 2: //cas ou la ligne est un role	
						listLigneFrame.add("role%srl%"+line.trim());
						break;
					case 3: //cas ou la ligne est un exemple	
						listLigneFrame.add("example%srl%"+line.trim());
						break;
					case 4:
						break;
					case 5: //cas ou la ligne est un roleset
						listLigneFrame.add("roleset%srl%"+line.trim());
						break;
					case 6:
					default: //autre cas
						break;
					}
				}
			}

			if (!listLigneFrame.isEmpty() && 
				listLigneFrame.get(0).startsWith("syntax") && 
				listLigneFrame.get(1).startsWith("freq"))
			{
				Element syntax = document.createElement("syntax");
				syntax.setAttribute("freq", listLigneFrame.get(1).split("%srl%")[1]);
				syntax.setTextContent(listLigneFrame.get(0).split("%srl%")[1]);
				//System.out.println(listLigneFrame.get(1).split("%srl%")[1]);
				if (listLigneFrame.size()==2 )
				{
					//cas other frame								
					otherframes.appendChild(syntax);
				}
				else
				{
					//cas frame semantique
					//creation des elements
					Element frame = document.createElement("frame");
					Element roleset = document.createElement("roleset");
					Element roles = document.createElement("roles");
					
					//recherche du roleset
					for (String ligne : listLigneFrame)
					{
						if (ligne.startsWith("roleset"))
						{
							tabLine = ligne.split("%srl%")[1].split(" ", 2);
							roleset.setAttribute("id", tabLine[0]);
							roleset.setAttribute("descr", tabLine[1]);
						}
						else if (ligne.startsWith("role"))
						{
							tabLine = ligne.split("%srl%")[1].split(" ", 2);
							
							Element role = document.createElement("role");
							role.setAttribute("descr", tabLine[1]);
							role.setAttribute("n", tabLine[0]);
							
							//ajout à roles
							roles.appendChild(role);
						}
						else if (ligne.startsWith("example"))
						{
							Element example = document.createElement("example");
							example.setTextContent(ligne.split("%srl%")[1]);
							
							//ajout à roles
							roles.appendChild(example);
						}
					}
					//liaison des elements
					frame.appendChild(syntax);
					roleset.appendChild(roles);
					frame.appendChild(roleset);
					frames.appendChild(frame);
				}
				//ici le traitement pour enregistrer la frame dans frame ou otherframe
			}
			listLigneFrame.clear();
			scanner.close();


			//ajout de l'elt frames à la racine
			frames.appendChild(otherframes);
			
			//ajout de l'elt frames à la racine
			racine.appendChild(frames);
			
			document.appendChild(racine);
			
			//Sauvegarde du DOM dans un fichier XML
			if (verbTraite)
			{
				transformerXml(document, nomFichier);
			}
			else
			{
				listVerbNonTraite.add(nomVerbe);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	private static boolean isTriplet(String cadre) {
		// TODO Auto-generated method stub
		boolean triplet = true;
		String[] tabCadre = cadre.split(", ");
		for (String elt: tabCadre)
		{
			if (elt.split(":").length != 3)
				triplet = false;
		}
		return triplet;
	}

	public static void transformerXml(Document document, String fichier) {
        try {
            // Création de la source DOM
            Source source = new DOMSource(document);
    
            // Création du fichier de sortie
            File file = new File(fichier);
            Result resultat = new StreamResult(fichier);
    
            // Configuration du transformer
            TransformerFactory fabrique = TransformerFactory.newInstance();
            Transformer transformer = fabrique.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            
            // Transformation
            transformer.transform(source, resultat);
        }catch(Exception e){
        	e.printStackTrace();	
        }
    }

	private static int typeLine(String line, String nomVerbe) {
		// TODO Auto-generated method stub*
		int type = 6;
		
		if (line.startsWith("==="))
		{
			type = 0;
		}
		else if (line.trim().startsWith("SUJ") ||
				line.trim().startsWith("ATS") ||
				line.trim().startsWith("OBJ") ||
				line.trim().startsWith("DE-OBJ") ||
				line.trim().startsWith("A-OBJ") ||
				line.trim().startsWith("obj") ||
				line.trim().startsWith("ATO") ||
				line.trim().startsWith("refl"))
		{
			String[] tabLine = line.split(", ");
			/*if (tabLine[0].split(":").length==3)
				type = 1;
			else
				type = 4;*/
			type = 1;
		}
		else if (line.startsWith("          "))
		{
			type = 2;
		}
		else if (line.trim().startsWith(nomVerbe))
		{
			type = 5;
		}
		else if (line.trim().equals(""))
		{
			type = 6;
		}
		else
		{
			type = 3;
		}
		return type;
	}

	private static void loadP7(int borneInf, int borneSup) {
		// TODO Auto-generated method stub
		//on parcours le fichier sans lire les phrase jusqu'à la phrase de même nombre que borneInf.
		try{
			// Création du flux bufférisé sur un FileReader, immédiatement suivi par un 
			// try/finally, ce qui permet de ne fermer le flux QUE s'il le reader
			// est correctement instancié (évite les NullPointerException)
			BufferedReader buff = new BufferedReader(new FileReader(p7Path));
			try {
				// lecture du fichier
				// division en phrases
				// traitement des phrases
				int numPhrase=0;
				String line, phrase="";
				boolean terminer = false; 
				while ((line = buff.readLine()) != null && !terminer) {
					String[] ss = line.split("\t");
					if (ss.length==1)
					{
						listPhrase.add(numPhrase, phrase);
						phrase = "";
						numPhrase++;
						if (numPhrase==borneSup)
							terminer = true;
					}
					else
					{	
						if (numPhrase>=borneInf && ss[3].equals("V"))
						{
							if (!isVerbP7(ss[2]))
							{
								listVerbP7.add(ss[2]);
								Treelex currentVerb = new Treelex();
								currentVerb.setLemma(ss[2]);
								currentVerb.getListPhrase().add(numPhrase);
								listTreelex.add(currentVerb);
							}
							else
							{
								if (!listTreelex.get(searchVerbTreelex(ss[2])).getListPhrase().contains(numPhrase))
									listTreelex.get(searchVerbTreelex(ss[2])).getListPhrase().add(numPhrase);
							}
						}
						phrase = phrase+" "+ss[1];
					}
				}
			} finally {
				// dans tous les cas, on ferme nos flux
				buff.close();
			}
		} catch (IOException ioe) {
			// erreur de fermeture des flux
			System.out.println("Erreur --" + ioe.toString());
		}
		//On parcours le fichier en sauvegardant les verbes trouvé
		//On s'arette si on est en fin de fichier ou qu'on a atteint borneSup
	}

	private static boolean isVerbP7(String verbe) {
		// TODO Auto-generated method stub
		boolean trouver = false;
		int iter = 0;
		while (!trouver && iter < listVerbP7.size())
		{
			if (listVerbP7.get(iter).equals(verbe))
				trouver=true;
			iter++;
		}
		return trouver;
	}

	private static void fusionLexicon() {
		// TODO Auto-generated method stub
		File dirResult = new File (resultPath);

		if (!dirResult.isDirectory())
			dirResult.mkdirs();

		FileWriter fileFusion = null;
		try{
			fileFusion = new FileWriter(resultPath+"/fusion", false);
			/////////////////
			//Partie qui sert  ajouter les elements d'entete au dbut du fichier comme treelex 
			try{
				// CrÃ©ation du flux buffÃ©risÃ© sur un FileReader, immÃ©diatement suivi par un 
				// try/finally, ce qui permet de ne fermer le flux QUE s'il le reader
				// est correctement instanciÃ© (Ã©vite les NullPointerException)
				BufferedReader buff = new BufferedReader(new FileReader(treelexPath));

				try {
					String line;
					while ((line = buff.readLine()) != null && !line.startsWith("===")) {
						fileFusion.write(line+"\n");
					}

					
				} finally {
					// dans tous les cas, on ferme nos flux
					buff.close();
				}
			} catch (IOException ioe) {
				// erreur de fermeture des flux
				System.out.println("Erreur --" + ioe.toString());
			}
			//Fin de l'ajout de l'entete de fichier
			////////////////
			for (Treelex currentVerb : listTreelex)
			{
				// Look if first letter folder exist, create if not
				char firstLetter = currentVerb.getLemma().toLowerCase().charAt(0);
				File dirLetter = new File (resultPath+"/"+firstLetter);

				if (!dirLetter.isDirectory())
					dirLetter.mkdirs();
				
				FileWriter fileFusion2 = null;
				try{
					fileFusion2 = new FileWriter(resultPath+"/"+firstLetter+"/"+currentVerb.getLemma().toLowerCase(), false);
					//Ici on ajoute le leme du verbe
					fileFusion.write(currentVerb.getFirstLine()+"\n\n");
					fileFusion2.write(currentVerb.getFirstLine()+"\n\n");
					//Pour chaque roleset d'une traduction, on fait la liste des cadres
					fileFusion.write("TL Frames\n\n");
					fileFusion2.write("TL Frames\n\n");
					for (String currentCadre : currentVerb.getListCadreBrut())
					{
						fileFusion.write("   "+currentCadre.trim()+"\n");
						fileFusion2.write("   "+currentCadre.trim()+"\n");
					}
					fileFusion.write("\nP7 Sentences\n\n");
					fileFusion2.write("\nP7 Sentences\n\n");
					for (int currentPhrase : currentVerb.getListPhrase())
					{
						fileFusion.write("   "+listPhrase.get(currentPhrase).trim()+"\n");
						fileFusion2.write("   "+listPhrase.get(currentPhrase).trim()+"\n");
					}
					fileFusion.write("\nDV Senses\n\n");
					fileFusion2.write("\nDV Senses\n\n");
					//Pour chaque valence du verbe,
					int verbId = 0;
					for (Dicovalence currentValence : currentVerb.getListDicovalence())
					{
						verbId++;
						
						//On ouvre le block verb ID DV Translation
						fileFusion.write("   ============================\n   "+currentVerb.getLemma()+"."+verbId+" "+currentValence.getTraduction()+"\n   ============================\n");
						fileFusion2.write("   ============================\n   "+currentVerb.getLemma()+"."+verbId+" "+currentValence.getTraduction()+"\n   ============================\n");
						fileFusion.write("      DV example: "+currentValence.getExemple()+"\n");
						fileFusion2.write("      DV example: "+currentValence.getExemple()+"\n");
						
						//Pour chaque traduction, on récupère le premier fichier xml
						String[] tabTrad = currentValence.getTraduction().split("[,;] ");
						boolean fileFind = false;
						int iter = 0;
						while (!fileFind && iter < tabTrad.length){
							File verbFile = new File (propbankPath+"/"+tabTrad[iter]+".xml");
							if (fileFind = verbFile.exists())
							{
								BufferedReader buff = new BufferedReader(new FileReader(propbankPath+"/"+tabTrad[iter]+".xml"));
	
								try {
									String line;
									ArrayList<String> listRole = new ArrayList<String>();
									boolean isExample = false;
									while ((line = buff.readLine()) != null) {
										if (line.startsWith("<roleset")){
											//dbut du role set, on recherche l'id et le nom
											String id = line.split("id=\"")[1].split("\"")[0];
											String name = line.split("name=\"")[1].split("\"")[0];

											fileFusion.write("\n      PBK\n"+"      "+id+" "+name+"\n");
											fileFusion2.write("\n      PBK\n"+"      "+id+" "+name+"\n");
										}
										else if (line.contains("<role ")){
											listRole.add(line);
											/*//dbut du role set, on recherche l'id et le nom
											String descr = line.split("descr=\"")[1].split("\"")[0];
											String n = line.split("n=\"")[1].split("\"")[0];
	
											fileFusion.write(n+" "+descr+"\n");
											fileFusion2.write(n+" "+descr+"\n");*/
										}
										else if (line.contains("<text>")){
											//dbut du texte d'exemple
											isExample = true;
											fileFusion.write("          ");
											fileFusion2.write("          ");
										}
										else if (isExample){
											//dbut du texte d'exemple
											isExample = false;
											fileFusion.write(line.trim());
											fileFusion2.write(line.trim());
										}
										else if (line.contains("</text>")){
											//dbut du texte d'exemple
											isExample = false;
											fileFusion.write("\n");
											fileFusion2.write("\n");
										}
										else if (line.startsWith("</roleset>")){
											//Pour chaque roleset d'une traduction, on fait la liste des cadres
											for (String currentRole : listRole)
											{//dbut du role set, on recherche l'id et le nom
												String descr = currentRole.split("descr=\"")[1].split("\"")[0];
												String n = currentRole.split("n=\"")[1].split("\"")[0];
		
												fileFusion.write("          "+n+" "+descr+"\n");
												fileFusion2.write("          "+n+" "+descr+"\n");
											}
											listRole.clear();
											
										}
									}
									fileFusion.write("\n");
									fileFusion2.write("\n");
	
									
								} finally {
									// dans tous les cas, on ferme nos flux
									buff.close();
								}
							}
							iter++;
							fileFusion.write("\n");
							fileFusion2.write("\n");
						}
					}
				}catch(IOException ex){
					ex.printStackTrace();
				}finally{
					if(fileFusion2 != null){
						try {
							fileFusion2.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}catch(IOException ex){
			ex.printStackTrace();
		}finally{
			if(fileFusion != null){
				try {
					fileFusion.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

	private static void loadTreelexBis() {
		try{
			// CrÃ©ation du flux buffÃ©risÃ© sur un FileReader, immÃ©diatement suivi par un 
			// try/finally, ce qui permet de ne fermer le flux QUE s'il le reader
			// est correctement instanciÃ© (Ã©vite les NullPointerException)
			BufferedReader buff = new BufferedReader(new FileReader(treelexPath));

			try {

				String line, lemma = null, firstLine = null;
				boolean firstVerb = true;
				ArrayList<String> listCadreBrut = new ArrayList<String>();
				ArrayList<String> listCadrePropre = new ArrayList<String>();
				while ((line = buff.readLine()) != null) {
					if (!line.contains("all tokens:") && !line.contains("all verbs (types)") ){ //la ligne n'est une des lignes de dÃ©part
						if (firstVerb)
							firstVerb = false;
						else if (line.length()==0){ //si la ligne est vide mais pas la prÃ©cÃ©dente
							if (isVerbP7(lemma))
							{
								int idVerbe = searchVerbTreelex(lemma);
								listTreelex.get(idVerbe).setLemma(lemma);
								listTreelex.get(idVerbe).setFirstLine(firstLine);
								ArrayList<String> temp = new ArrayList<String>(listCadreBrut);
								ArrayList<String> temp2 = new ArrayList<String>(listCadrePropre);
								listTreelex.get(idVerbe).setListCadreBrut(temp);
								listTreelex.get(idVerbe).setListCadrePropre(temp2);
								//on enregistre le verbe courant dans la liste ou pas
								listCadreBrut.clear();
								listCadrePropre.clear();
							}
						}
						else if (line.startsWith("===")){
							firstLine = line;
							lemma = line.split(" \\(frames")[0].replace("===", "");
						}
						else {
							listCadreBrut.add(line);
							line = line.replace(" :", ":");
							line = line.replace(": ", ":");
							line = line.replace(",", ", ");
							line = line.replace(",  ", ", ");
							line = line.replace("))", ")");
							line = line.replaceAll("\\([0-9]*\\)", "");
							line = line.replaceAll("0", "O");
							listCadrePropre.add(line.trim());
						}
						
					}

				}

				Treelex currentVerb = new Treelex();
				currentVerb.setLemma(lemma);
				currentVerb.setFirstLine(firstLine);
				ArrayList<String> temp = new ArrayList<String>(listCadreBrut);
				ArrayList<String> temp2 = new ArrayList<String>(listCadrePropre);
				currentVerb.setListCadreBrut(temp);
				currentVerb.setListCadrePropre(temp2);
				//on enregistre le verbe courant dans la liste
				listTreelex.add(currentVerb);
				listCadreBrut.clear();
				listCadrePropre.clear();

				
			} finally {
				// dans tous les cas, on ferme nos flux
				buff.close();
			}
		} catch (IOException ioe) {
			// erreur de fermeture des flux
			System.out.println("Erreur --" + ioe.toString());
		}
	}

	private static void loadDicovalence() {
		// TODO Auto-generated method stub
		try{
			// CrÃ©ation du flux buffÃ©risÃ© sur un FileReader, immÃ©diatement suivi par un 
			// try/finally, ce qui permet de ne fermer le flux QUE s'il le reader
			// est correctement instanciÃ© (Ã©vite les NullPointerException)
			BufferedReader buff = new BufferedReader(new FileReader(dicovalencePath));

			try {

				String line, lemma = null, exemple = null, traduction = null;
				int compteur = 0;
				while ((line = buff.readLine()) != null) {
					if (!line.startsWith("#")){ //la ligne n'est pas un commentaire
						if (line.length()==0){ //si la ligne est vide mais pas la prÃ©cÃ©dente
							if (compteur == 2){
								int idTreelex = searchVerbTreelex(lemma);
								if (idTreelex != -1){
									if (lemma!=null && exemple!=null && traduction!=null)
									{
										Dicovalence currentVerb = new Dicovalence();
										currentVerb.setLemma(lemma);
										currentVerb.setExemple(exemple);
										currentVerb.setTraduction(traduction);
										//on enregistre le verbe courant dans la liste
										listTreelex.get(idTreelex).getListDicovalence().add(currentVerb);
										listDicovalence.add(currentVerb);
									}
									lemma = null;
									exemple = null;
									traduction = null;
								}
								compteur = 0;
							}
							compteur++;
						}
						else if (line.startsWith("VERB")){
							lemma = line.split("/")[1];
						}
						else if (line.startsWith("EG")){
							exemple = line.split("\t")[1];
						}
						else if (line.startsWith("TR_EN")){
							String[] tabTrad = line.split("\t");
							if (tabTrad.length>1)
								traduction = tabTrad[1];
							else
								traduction = null;
						}
						
					}

				}

				
			} finally {
				// dans tous les cas, on ferme nos flux
				buff.close();
			}
		} catch (IOException ioe) {
			// erreur de fermeture des flux
			System.out.println("Erreur --" + ioe.toString());
		}		
	}

	private static int searchVerbTreelex(String lemma) {
		// TODO Auto-generated method stub
		int retour = -1, iter = 0;
		boolean find=false;
		while (iter < listTreelex.size() && !find)
		{
			if (listTreelex.get(iter).getLemma().equals(lemma)){
				retour = iter;
				find = true;
			}
			iter++;
		}
		return retour;
	}

	public String changeTitle(String title, String nomView) {
		// TODO Auto-generated method stub
		if (title.contains(" --- current view : "))
			title = title.split(" --- current view : ")[0];
		
		title = title+" --- current view : "+nomView;
		
		return title;
	}
}