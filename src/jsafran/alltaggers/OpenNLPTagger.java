package jsafran.alltaggers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import corpus.BDLex;
import corpus.DicoEntry;
import corpus.DicoEntry.POStag;
import opennlp.maxent.EventStream;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.TwoPassDataIndexer;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.postag.POSEventStream;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.postag.TagDictionary;


public class OpenNLPTagger implements TagDictionary {
	public static void main (String[] args) throws IOException, InterruptedException {
//	
//			List<String> mots = new ArrayList<String>();
//
//			List<String> postags = new ArrayList<String>();
//			String InFile = args[0];
//			Scanner scanner=new Scanner(new File(InFile),"UTF-8");
//			String[] tab;
//			@SuppressWarnings("unused")
//			String num;
//			String tag;
//			String nom;
//			while (scanner.hasNextLine()) {
//				String line = scanner.nextLine();
//				tab = line.split("\t");
//
//				if (line.length()!=0)
//				{
//					mots.add(tab[1]);
//					postags.add(tab[3]);
//				}
//				else{
//					mots.add("");
//					postags.add("");
//				}
//			}
//
//			scanner.close();
//			trainModels(mots, postags);
		
//			List<String> words = new ArrayList<String>();
//			List<String> mots = new ArrayList<String>();
//			List<String> gold = new ArrayList<String>();
//			String InFile = args[0];
//			Scanner scanner=new Scanner(new File(InFile),"UTF-8");
//			String[] tab;
//			double total = 0;
//			double working = 0;
//
//			while (scanner.hasNextLine()) {
//				String line = scanner.nextLine();
//				tab = line.split("\t");
//
//				if (line.length()!=0)
//				{
//					mots.add(tab[1]);
//					gold.add(tab[3]);
//				}
//				
//				else{
//					mots = getPostags(mots);
//					int i = 0;
//					while (i < mots.size()){
//						total++;
//						if (mots.get(i).equalsIgnoreCase(gold.get(i))){
//							working++;
//						}
//						i++;
//					}
//					mots.clear();
//					gold.clear();
//				}
//			}
//			double score = working/total;
//			System.out.print("Le programme a assigné correctement " + working + " tags sur un total de " + total + " mots � tagger. Son taux d'erreur est de " + (1 - score) + ".");
//
//			
		
	}
	
	BDLex bdlex = BDLex.getBDLex();
	POSTagger tagger;
	public OpenNLPTagger() {
		File modelFile = new File("res/Parameters.bin");
		try {
			GISModel mod = new SuffixSensitiveGISModelReader(modelFile).getModel();
			tagger = new POSTaggerME(mod,(Dictionary)null,this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * retourne les POStags pour chaque "mot" = "token"
	 * Le programme a assigné correctement 1890.0 tags sur un total de 1941.0 mots � tagger. Son taux d'erreur est de 0.026275115919629055.
	 * Modèle appris de p7_train_utf.conll
	 * Test réalisé sur p7_test_utf.conll
	 * @param mots
	 * @return
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public List<String> getPostags(List<String> mots) {
		@SuppressWarnings("unchecked")
		List<String> postags = tagger.tag(mots);
		return postags;
	}

	// TODO: faut-il ajouter des espaces ?
	public static String trainModels(List<String> mots, List<String> postags) throws IOException {
		File outFile = new File("res/Parameters.bin");
		int cutoff = 5;
		int iterations = 100;
		GISModel mod;
		EventStream es;
		File temp = File.createTempFile("tmp",".txt");
		temp.deleteOnExit();
		BufferedWriter doc=new BufferedWriter(new FileWriter(temp));
		int i = 0;
		while(i < mots.size() ){
			if (mots.get(i).equals("") && postags.get(i).equals("")){
				doc.newLine();
				
				i++;
			}
			else{
				doc.write(mots.get(i)+"_"+postags.get(i)+" ");
				
				i++;
			}
		}
		doc.flush();
		doc.close();
		es = new POSEventStream(new PlainTextByLineDataStream(
				new InputStreamReader(new FileInputStream(temp))));
		mod = opennlp.maxent.GIS.trainModel(iterations, new TwoPassDataIndexer(es, cutoff));
		System.out.println("Saving the model as: " + outFile);
		new SuffixSensitiveGISModelWriter(mod, outFile).persist();
		System.out.println(outFile.getAbsolutePath());
		return outFile.getAbsolutePath();
	}

	@Override
	public String[] getTags(String word) {
		if (false) {
			return null;
		} else {
			List<DicoEntry> es = bdlex.getDicoEntries(word);
			if (es==null||es.size()==0) return null;
			HashSet<String> possibletags= new HashSet<String>();
			for (DicoEntry e : es) {
				String[] p = getP7POS(e.getPOStag());
				if (p!=null)
					possibletags.addAll(Arrays.asList(p));
			}
			String[] res = new String[possibletags.size()];
			possibletags.toArray(res);
			if (res.length<=0) return null;
			return res;
		}
	}
	
	static String[] getP7POS(POStag p) {
		switch (p) {
		case adv: {
			String[] x = {"ADV","ADVWH"};
			return x;
		}
		case det: {
			String[] x = {"DETWH","DET","P","P+D"};
			return x;
		}
		case adjnommasc:
		case adjnom:
		case adj:
		case adjnomfem: {
			String[] x = {"ADJWH","ADJ"};
			return x;
		}
		case conj: {
			String[] x = {"CC","CS"};
			return x;
		}
		case verb: {
			String[] x = {"V","VPP","VIMP","VINF","VPR","VS"};
			return x;
		}
		case nom: {
			String[] x = {"NC","NPP"};
			return x;
		}
		case prep: {
			String[] x = {"P","P+D"};
			return x;
		}
		case pron: {
			String[] x = {"PRO","CLS","P+PRO","PROWH","CLR"};
			return x;
		}
		default: return null;
		}
	}
}
