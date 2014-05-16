package jsafran.dislocations;

import javax.swing.JOptionPane;

import jsafran.DetGraph;
import jsafran.JSafran;

public class DislocAnnot {
	JSafran js;
	
	public DislocAnnot(JSafran main) {
		js=main;
		String s = "You enter dislocation quick annotation mode:\n";
		s+="clic on (1) the Disloc word and (2) its reference; the list will automatically pass to the next utterance\n";
		s+="you can go back with the up and down arrows\n";
		s+="press ESC to remove all DISLOC dependencies of the current utterance\n";
		s+="\n";
		s+="select the menu again to quit this mode";
		JOptionPane.showMessageDialog(null, s);
	}
	
	private int step=0;
	private int[] w = {-1,-1};
	
	public void clicWord() {
		if (js.editword<0) return;
		w[step]=js.editword;
		if (step==1) {
			js.allgraphs.get(js.curgraph).ajoutDep("DISLOC", w[0], w[1]);
			js.repaint();
			if (js.curgraph<js.allgraphs.size()) js.curgraph++;
		}
		step=1-step;
	}
	
	public void pressESC() {
		DetGraph g = js.allgraphs.get(js.curgraph);
		for (int i=0;i<g.getNbMots();i++) {
			int[] deps = g.getDeps(i);
			for (int d : deps) {
				String lab = g.getDepLabel(d);
				if (lab.equals("DISLOC")) g.removeDep(d);
			}
		}
		js.repaint();
	}
}
