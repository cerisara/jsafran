package jsafran.dislocations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

public class DepEvaluation {
	public static void eval(JSafran main) {
		JOptionPane.showMessageDialog(null, "This compares the arcs in the current file to a gold file.\n"+
				"Please Choose next the gold file (must be in XML or conll06 format)");
		JFileChooser jfc = new JFileChooser(new File("."));
		int returnVal = jfc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File fgold = jfc.getSelectedFile();
			GraphIO gio = new GraphIO(null);
			List<DetGraph> golds = gio.loadAllGraphs(fgold.getAbsolutePath());
			if (golds.size()!=main.allgraphs.size()) {
				JOptionPane.showMessageDialog(null, "ERROR: le gold file et le fichier chargé n'ont pas le même nombre de graphes ! "+golds.size()+" "+main.allgraphs.size());
			} else {
				compareGraphs(golds,main.allgraphs);
			}
		}
	}
	
	public static void compareGraphs(List<DetGraph> gold, List<DetGraph> reco) {
		ArrayList<Integer> FP = new ArrayList<Integer>();
		ArrayList<Integer> FN = new ArrayList<Integer>();
		ArrayList<Integer> TP = new ArrayList<Integer>();
		HashMap<String, Integer> deplab2index = new HashMap<String, Integer>();
		for (int i=0;i<gold.size();i++) {
			// TODO: warning si pas le mm nb de mots
			DetGraph go = gold.get(i);
			DetGraph re = reco.get(i);
			
			for (int j=0;j<go.getNbMots();j++) {
				int[] depsg = go.getDeps(j);
				for (int depg: depsg) {
					String labg = go.getDepLabel(depg);
					int headg = go.getHead(depg);
					Integer labidx = deplab2index.get(labg);
					if (labidx==null) {
						labidx=deplab2index.size();
						deplab2index.put(labg, labidx);
						FP.add(0); FN.add(0); TP.add(0);
					}
					// cherche si la dep gold a ete reconnue
					int[] depsr = re.getDeps(j);
					boolean hasbeenfound = false;
					for (int depr : depsr) {
						int headr = re.getHead(depr);
						String labr = re.getDepLabel(depr);
						if (headr==headg && labr.equals(labg)) {
							hasbeenfound=true; break;
						}
					}
					if (hasbeenfound) {
						int co = TP.get(labidx);
						TP.set(labidx,co+1);
					} else {
						int co = FN.get(labidx);
						FN.set(labidx,co+1);
						// TODO: ajouter une dep en couleur
					}
				}
			}

			for (int j=0;j<re.getNbMots();j++) {
				int[] depsr = re.getDeps(j);
				for (int depr: depsr) {
					String labr = re.getDepLabel(depr);
					int headr = re.getHead(depr);
					Integer labidx = deplab2index.get(labr);
					if (labidx==null) {
						labidx=deplab2index.size();
						deplab2index.put(labr, labidx);
						FP.add(0); FN.add(0); TP.add(0);
					}
					// cherche si la dep reconnue existe dans le gold
					int[] depsg = go.getDeps(j);
					boolean hasbeenfound = false;
					for (int depg : depsg) {
						int headg = go.getHead(depg);
						String labg = go.getDepLabel(depg);
						if (headr==headg && labr.equals(labg)) {
							hasbeenfound=true; break;
						}
					}
					if (hasbeenfound) {
						// on l'a deja compte en positif !
					} else {
						int co = FP.get(labidx);
						FP.set(labidx,co+1);
						// TODO: changer la dep en couleur
					}
				}
			}
		
		}
		String s="";
		for (String lab : deplab2index.keySet()) {
			int i=deplab2index.get(lab);
			float prec = (float)TP.get(i)/(float)(TP.get(i)+FP.get(i));
			int nprec = TP.get(i)+FP.get(i);
			float confintprec = 1.96f*(float)Math.sqrt(prec*(1f-prec)/nprec);
			float recall = (float)TP.get(i)/(float)(TP.get(i)+FN.get(i));
			int nrecall = TP.get(i)+FN.get(i);
			float confintrecall = 1.96f*(float)Math.sqrt(recall*(1f-recall)/nrecall);
			
			float fmes = 2f*prec*recall/(prec+recall);
			s       += lab+":PRECISION = "+prec+" +/- "+confintprec+" "+nprec+"\n";
			s       += lab+":RECALL    = "+recall+" +/- "+confintrecall+" "+nrecall+"\n";
			s       += lab+":F-MESURE  = "+fmes;
		}
		JOptionPane.showMessageDialog(null, s);
	}
}
