package jsafran.parsing.unsup.rules;

import java.util.HashMap;

/**
 * 
 * contient des valeurs associees a des variables pendant le search
 * 
 * @author xtof
 *
 */
public class VarContainer {
	class Variable {
		char nom;
		int[] vals;
	}
	
	HashMap<Character, Variable> var2index = new HashMap<Character, Variable>();
	
	private Variable getOrAddVar(char c) {
		Variable v = var2index.get(c);
		if (v==null) {
			v=new Variable();
			var2index.put(c, v);
		}
		return v;
	}
	
	public int[] getVals(char c) throws NoVarFound {
		Variable v=var2index.get(c);
		if (v==null) throw new NoVarFound();
		return v.vals;
	}
	
	public int getVal(char c) throws NoVarFound, MultipleValsFound {
		Variable v=var2index.get(c);
		if (v==null) throw new NoVarFound();
		if (v.vals.length>1) throw new MultipleValsFound();
		return v.vals[0];
	}
	
	public void setVar(char c, int val) {
		int[] tmp = {val};
		Variable v = getOrAddVar(c);
		v.vals=tmp;
	}
	
	public class NoVarFound extends Exception {
	}
	public class MultipleValsFound extends Exception {
	}
}
