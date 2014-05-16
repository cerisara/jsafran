package jsafran.ponctuation;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import utils.FileUtils;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.objectbank.ObjectBank;

import jsafran.DetGraph;
import jsafran.Mot;
import jsafran.POStagger;

public class PonctModel {
	public static float tolerance = 0.001f;
	
	String modelfile;
	final String featsfile = "data.feats";
	
	String[] propdefs = {
			"map = word=0,tag=1,answer=2",
			"useWord=true",
			"useTags=true",
			"usePrev=true",
			"useNext=true",
			"backgroundSymbol="+PonctFeatures.noponct,
			"tolerance="+tolerance,
			
			"trainFile="+featsfile,
			"serializeTo="+modelfile,
	};
	
	public PonctModel(String modname) {
		modelfile = modname+".pctmod";
		propdefs[propdefs.length-1]="serializeTo="+modelfile;
	}

	public static void delPonct(List<DetGraph> gs) {
		for (DetGraph g : gs) {
			for (int i=g.getNbMots()-1;i>=0;i--) {
				if (POStagger.isPonct(g.getMot(i).getPOS())) g.delNodes(i, i);
			}
		}
	}
	
	public void genPonct(List<DetGraph> gs, boolean calcFmes) {
		try {
			CRFClassifier ponctClassif = CRFClassifier.getClassifier(new File(modelfile));
			int nins=0, ndel=0, nmatch=0, nsub=0, ntot=0;

			PrintWriter featsout = FileUtils.writeFileUTF(featsfile);
			List<Integer>[] indexes = PonctFeatures.saveFeats(featsout, gs);
			featsout.close();
			ObjectBank<List<CoreLabel>> fs = ponctClassif.makeObjectBankFromFile(featsfile);
			System.out.println("fs size "+fs.size());
			assert fs.size()==1;

			for (List<CoreLabel> ffs : fs) {
				List<CoreLabel> res = ponctClassif.classify(ffs);
				assert res.size()==indexes[0].size();
				assert res.size()==indexes[1].size();
				for (int i=res.size()-1;i>=0;i--) {
					CoreLabel lab = res.get(i);
					String rec = lab.get(AnswerAnnotation.class);
					ntot++;
					int gi = indexes[0].get(i);
					int wi = indexes[1].get(i);
//					System.out.println("debug giwi "+i+" "+gi+" "+wi+" "+gs.get(gi).getNbMots());
					if (calcFmes) {
						if (wi+1>=gs.get(gi).getNbMots()) {
//							System.out.println("warning: ponct apres la phrase ?");
							// pas de ponctuation finale: c'est une erreur !
							nins++;
						} else {
							String ref = PonctFeatures.getPonctClass(gs.get(gi).getMot(wi+1).getForme());
							if (ref.equals(PonctFeatures.noponct)) {
								if (!rec.equals(PonctFeatures.noponct))
									nins++;
							} else {
								if (ref.equals(rec)) nmatch++;
								else if (rec.equals(PonctFeatures.noponct)) ndel++;
								else nsub++;
							}
						}
					} else {
						if (!rec.equals(PonctFeatures.noponct)) {
							String symb = PonctFeatures.getPonctSymb(rec);
							if (!symb.equals(PonctFeatures.noponct)) {
								System.out.println("insert "+symb+" "+gi+" "+wi);
								Mot m = new Mot(symb,symb,"ponct");
								gs.get(gi).insertMot(wi+1, m);
							}
						}
					}
				}
			}
			System.out.println("getout");
			if (calcFmes) {
				System.out.println("nins "+nins+" ndel "+ndel+" nsub "+nsub+" nmatch "+nmatch);
				int tp=nmatch, fp=nins, fn=ndel, tn=ntot-nmatch-nins;
				float prec = (float)tp/(float)(tp+fp);
				float recall = (float)tp/(float)(tp+fn);
				float acc = (float)(tp+tn)/(float)(tp+tn+fp+fn);
				float fmes = 2f*prec*recall/(prec+recall);
				System.out.println("p="+prec+" r="+recall+" acc="+acc+" f="+fmes);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void train(List<DetGraph> gs) {
		File ftmp = new File(".");
		final String[] parms = {"-prop",ftmp.getAbsolutePath()+"/detpct.props"};
		try {
			PrintWriter f = FileUtils.writeFileUTF(ftmp.getAbsolutePath()+"/detpct.props");
			for (String s : propdefs) {
				f.println(s);
			}
			f.close();
			PrintWriter featsout = FileUtils.writeFileUTF(featsfile);
			PonctFeatures.saveFeats(featsout, gs);
			featsout.close();
			CRFClassifier.main(parms);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
