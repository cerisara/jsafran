package jsafran.parsing;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import opennlp.maxent.BasicEventStream;
import opennlp.maxent.DataStream;
import opennlp.maxent.EvalParameters;
import opennlp.maxent.EventStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.OnePassRealValueDataIndexer;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.RealBasicEventStream;
import opennlp.maxent.RealValueFileEventStream;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import utils.FileUtils;

public class MaxEntOpenNLP {
	public static boolean USE_SMOOTHING = false;
	public static double SMOOTHING_OBSERVATION = 0.9;

	GISModel model;
	boolean real=false;

	public static void main(String[] args) {
		if (args[0].equals("-train")) {
			String[] aa = new String[args.length-1];
			System.arraycopy(args, 1, aa, 0, aa.length);
			train(args);
		} else if (args[0].equals("-test")) {
			MaxEntOpenNLP m = new MaxEntOpenNLP();
			m.loadModel(args[1]);
			m.test(args[2]);
		} else if (args[0].equals("-inspect")) {
			MaxEntOpenNLP m = new MaxEntOpenNLP();
			m.loadModel(args[1]);
//			m.inspect();
		} else if (args[0].equals("-unittest")) {
			unitest();
		}
	}

	public int getNbClasses() {
		return model.getNumOutcomes();
	}

	public float proba = Float.NaN;
	/**
	 * Les feats passés en paramètre doivent avoir la target_class en derniere position !!
	 * Le target class doit contenir la String correspondant au resultat POSITIF (de la detection)
	 */
	public String getBestResult(String[] feats) {
		return getBestResult(feats, null);
	}
	public String getBestResult(String[] feats, ClassificationResult fullres) {
		String[] onlycontext = Arrays.copyOfRange(feats, 0, feats.length-1);
		double[] ocs;
		if (!real) {
			ocs = model.eval(onlycontext);
		} else {
			float[] values = RealValueFileEventStream.parseContexts(onlycontext);
			ocs = model.eval(onlycontext,values);
		}
		if (fullres!=null) {
			for (int i=0;i<ocs.length;i++) {
				fullres.addOneClass(model.getOutcome(i), (float)ocs[i]);
			}
		}
		String res = model.getBestOutcome(ocs);
		if (feats[feats.length-1]==null) {
			// retourne la proba de la meilleure solution trouvee
			Arrays.sort(ocs);
			proba = (float)ocs[ocs.length-1];
		} else {
			// retourne la proba de la solution suggérée
			int idx = model.getIndex(feats[feats.length-1]);
			if (idx<0||idx>=ocs.length) {
				System.out.println("ERROR for feats "+idx+" "+ocs.length+" "+Arrays.toString(feats));
				proba=0;
			} else
				proba = (float)ocs[idx];
		}
		return res;
	}

	/**
	 * Les feats passés en paramètre doivent avoir la target_class en derniere position !!
	 * 
	 */
	public float getScore(String[] feats) {
		String[] onlycontext = Arrays.copyOfRange(feats, 0, feats.length-1);
		double[] ocs;
		if (!real) {
			ocs = model.eval(onlycontext);
		} else {
			float[] values = RealValueFileEventStream.parseContexts(onlycontext);
			ocs = model.eval(onlycontext,values);
		}

		//        System.out.println("DEBUGGETSCORE "+model.getAllOutcomes(ocs));
		for (int i=0;i<ocs.length;i++) {
			if (model.getOutcome(i).equals(feats[feats.length-1])) return (float)ocs[i];
		}
		System.out.println("WARNING MAXENTMOD GETSCORE "+Arrays.toString(feats)+" "+ocs.length);
		return 0;
	}

	public HashMap<String, Float> analyseGUI(String[] features) {
		class Viewer extends JPanel implements ActionListener {
			JCheckBox[] cb = null;
			Box outnom = Box.createVerticalBox();
			Box featnom = Box.createVerticalBox();
			String[] feats = null;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				recompute(null);
			}

			class Res implements Comparable<Res> {
				float prob; String outcome;
				@Override
				public int compareTo(Res o) {
					if (prob>o.prob) return -1;
					else if (prob<o.prob) return 1;
					else return 0;
				}
				public JLabel getPanel() {
					return new JLabel(outcome+" : "+prob);
				}
			}
			
			public Viewer() {
				Box b0 = Box.createHorizontalBox();
				add(b0);
				b0.add(Box.createHorizontalGlue());
				b0.add(featnom);
				b0.add(Box.createHorizontalGlue());
				b0.add(outnom);
				b0.add(Box.createHorizontalGlue());
			}
			
			public void recompute(String[] features) {
				if (cb==null) {
					feats=features;
					cb = new JCheckBox[features.length];
					for (int i=0;i<features.length;i++) {
						cb[i] = new JCheckBox(features[i],true);
						cb[i].addActionListener(this);
						featnom.add(cb[i]);
					}
				}
				
				ArrayList<String> newfeats = new ArrayList<String>();
				assert feats.length==cb.length;
				for (int i=0;i<feats.length;i++) {
					if (cb[i].isSelected()) newfeats.add(feats[i]);
				}
				
				double[] ocs = model.eval(newfeats.toArray(new String[newfeats.size()]));
				Res[] ress = new Res[ocs.length];
				for (int i=0;i<ocs.length;i++) {
					ress[i]=new Res(); ress[i].prob=(float)ocs[i]; ress[i].outcome=model.getOutcome(i);
				}
				Arrays.sort(ress);
				
				outnom.removeAll();
				for (int i=0;i<ocs.length;i++) {
					outnom.add(ress[i].getPanel());
				}
				outnom.validate();
				outnom.repaint();
			}
		}
		final Viewer viewer = new Viewer();
		viewer.recompute(features);
		JOptionPane.showMessageDialog(null, viewer);
		
		double[] ocs = model.eval(features);
		HashMap<String, Float> res = new HashMap<String, Float>();
		for (int i=0;i<ocs.length;i++) {
			String label = model.getOutcome(i);
			float proba = (float)ocs[i];
			res.put(label, proba);
		}
		return res;
	}
	
	public HashMap<String, Float> parse(String[] features) {
		double[] ocs = model.eval(features);
		HashMap<String, Float> res = new HashMap<String, Float>();
		for (int i=0;i<ocs.length;i++) {
			String label = model.getOutcome(i);
			float proba = (float)ocs[i];
			res.put(label, proba);
		}
		return res;
	}
	
	private void eval (String predicates, boolean real) {
		String[] features = predicates.split(" ");
		double[] ocs;
		if (!real) {
			ocs = model.eval(features);
		}
		else {
			float[] values = RealValueFileEventStream.parseContexts(features);
			ocs = model.eval(features,values);
		}
		System.out.println("For context: " + predicates+ "\n" + model.getAllOutcomes(ocs) + "\n");
	}

	public void test(String datafile) {
		try {
			DataStream ds =
				new PlainTextByLineDataStream(
						new FileReader(new File(datafile)));
			while (ds.hasNext()) {
				String s = (String)ds.nextToken();
				eval(s.substring(0, s.lastIndexOf(' ')),real);
			}
			return;
		}
		catch (Exception e) {
			System.out.println("Unable to read from specified file: "+datafile);
			System.out.println();
			e.printStackTrace();
		}
	}

	public void loadModel(String filename) {
		try {
			model = (GISModel)(new SuffixSensitiveGISModelReader(new File(filename)).getModel());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * le fichier texte doit contenir un exemple par ligne,
	 * Le fichier texte est ouvert au format natif de la machine (UTF8 ou ISO...)
	 * les features etant separees par des whitespace et la derniere feature = target_class
	 * 
	 * @param args
	 */
	public static String train(String[] args) {
		int ai = 0;
		boolean real = false;
		int niters=100;
		while (args[ai].startsWith("-")) {
			if (args[ai].equals("-real")) {
				real = true;
			} else if (args[ai].equals("-smooth")) {
				USE_SMOOTHING=true;
			} else if (args[ai].equals("-niters")) {
				niters=Integer.parseInt(args[++ai]);
			}
			else {
				System.err.println("Unknown option: "+args[ai]);
			}
			ai++;
		}
		String dataFileName = new String(args[ai]);
		String modelFileName =
			dataFileName.substring(0,dataFileName.lastIndexOf('.'))
			+ "Model.txt";
		try {
			FileReader datafr = new FileReader(new File(dataFileName));
			EventStream es;
			if (!real) { 
				es = new BasicEventStream(new PlainTextByLineDataStream(datafr));
			}
			else {
				es = new RealBasicEventStream(new PlainTextByLineDataStream(datafr));
			}
			GIS.SMOOTHING_OBSERVATION = SMOOTHING_OBSERVATION;
			GISModel model;
			if (!real) {
				model = GIS.trainModel(es,niters,0,USE_SMOOTHING,true);
			}
			else {
				model = GIS.trainModel(100, new OnePassRealValueDataIndexer(es,0), USE_SMOOTHING);
			}

			File outputFile = new File(modelFileName);
			GISModelWriter writer =
				new SuffixSensitiveGISModelWriter(model, outputFile);
			writer.persist();
			return modelFileName;
		} catch (Exception e) {
			System.out.print("Unable to create model due to exception: ");
			System.out.println(e);
			e.printStackTrace();
			return null;
		}
	}

/*
	public void inspect() {
		Context[] PARAMS;
		String[] OUTCOME_LABELS;
		int CORRECTION_CONSTANT;
		double CORRECTION_PARAM;
		String[] PRED_LABELS;
		Object[] data = model.getDataStructures();

		PARAMS = (Context[]) data[0];
		IndexHashTable<String> pmap = (IndexHashTable<String>) data[1];
		OUTCOME_LABELS = (String[]) data[2];
		CORRECTION_CONSTANT = ((Integer) data[3]).intValue();
		CORRECTION_PARAM = ((Double) data[4]).doubleValue();
		PRED_LABELS = new String[pmap.size()];
	    pmap.toArray(PRED_LABELS);

		int numParams = 0;
		for (int pid = 0; pid < PARAMS.length; pid++) {
			int[] predkeys = PARAMS[pid].getOutcomes();
			int numActive = predkeys.length;
			int[] activeOutcomes = predkeys;
			double[] activeParams = PARAMS[pid].getParameters();
			String s = PRED_LABELS[pid]+" \t";
			for (int i=0;i<activeOutcomes.length;i++) {
				s+=OUTCOME_LABELS[activeOutcomes[i]]+":"+activeParams[i]+" ";
			}
			System.out.println(s);
			numParams += numActive;
		}

	}
	*/
		
	public static String train(String[][] feats, String[] labels, int niters) {
		return train(feats,labels,"maxentdat.tmp",niters);
	}
	public static String train(String[][] feats, String[] labels, String featfile, int niters) {
		assert feats.length==labels.length;
		try {
			PrintWriter f = new PrintWriter(new FileWriter(featfile));
			for (int i=0;i<feats.length;i++) {
				String[] e = feats[i];
				String s = "";
				for (String x : e) {
					s+=x+" ";
				}
				s+=labels[i];
				f.println(s);
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String[] args = {"-niters",""+niters,featfile};
		System.out.println("train and save mod "+featfile+" "+niters);
		String modfile = MaxEntOpenNLP.train(args);
		return modfile;
	}

	static void unitest() {
		String[][] ex = {
				{"a","b","a-b","x"},
				{"a","c","a-c","y"},
				{"d","c","d-c","x"},
				{"d","b","d-b","y"}
		};

		try {
			PrintWriter f = FileUtils.writeFileUTF("ff.ff");
			for (String[] e : ex) {
				String s = "";
				for (String x : e) {
					s+=x+" ";
				}
				s=s.trim();
				f.println(s);
			}
			f.close();
		} catch (Exception e) {}

		String[] args = {"ff.ff"};
		String modfile = MaxEntOpenNLP.train(args);

		MaxEntOpenNLP m = new MaxEntOpenNLP();
		m.loadModel(modfile);
		String[] e = ex[0];
		float p = m.getScore(e);
		System.out.println("score "+Arrays.toString(e)+" "+p);
	}

}
