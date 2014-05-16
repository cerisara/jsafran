package jsafran.parsing.unsup.rules;

import java.util.List;

import jsafran.DetGraph;

public interface Rules {
	public int getNrules();
	public int[] getApplicable(int ridx, DetGraph g);
	public double getRulePrior(int ridx);
	/**
	 * 
	 * @return list of words from which the dep has changed
	 */
	public int[] applyRule(int ridx, DetGraph g, final int pos);
	// les DETrules modifient seulement l'affichage des deps, mais plus le calcul du logpost !
	public void applyDetRules(DetGraph g);
	public String toString(int ridx);
	public void init(List<DetGraph> gs);
        public void setSRL(boolean srl);
}
