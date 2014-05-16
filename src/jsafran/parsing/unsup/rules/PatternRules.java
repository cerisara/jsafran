package jsafran.parsing.unsup.rules;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.hillclimb.OneRule;
import jsafran.parsing.unsup.hillclimb.RulesSeq;
import jsafran.parsing.unsup.hillclimb.Search;
import jsafran.parsing.unsup.rules.RulesEditor.Combiner;

/**
 * TODO: add length constraints to some rules: DET cannot go beyond 4 words, same for clitic-OBJ and clitic-POBJ...
 * 
 * @author xtof
 *
 */
public class PatternRules implements OneRule {

	public float getScore(int idx) {
		int rule = applicableHandRules.get(idx);
		return allRulePriors.get(rule);
	}
	
	void initRule(String s) {
		// every rule must start with this 2 words:
		int off=s.indexOf("For all ");
		if (off<0) {
			if (s.startsWith("class ")) initClasse(s);
			return;
		}
		
		float prior=1f;
		if (off>0) {
			String score=s.substring(0, off).trim();
			if (score.length()>0) {
				prior = Float.parseFloat(score);
			}
			s=s.substring(off);
		}

		if (
				s.indexOf("link to a ")>=0 &&
				s.indexOf("on the right with ")>=0) {
			allRules.add(new R1(s));
		} else if (
				s.indexOf("link to a ")>=0 &&
				s.indexOf("on the left with ")>=0) {
			allRules.add(new R2(s));
		} else if (
				s.indexOf("link to the first ")>=0 &&
				s.indexOf("without ")>=0 &&
				s.indexOf("on the right with ")>=0) {
			allRules.add(new R9(s));
		} else if (
				s.indexOf("link to the first ")>=0 &&
				s.indexOf("within ")>=0 &&
				s.indexOf("on the right with ")>=0) {
			allRules.add(new R10(s));
		} else if (
				s.indexOf("link to the first ")>=0 &&
				s.indexOf("on the right with ")>=0) {
			allRules.add(new R3(s));
		} else if (
				s.indexOf("link to the first ")>=0 &&
				s.indexOf("on the left with ")>=0) {
			allRules.add(new R4(s));
		} else if (
				s.indexOf("link to the word just before with ")>=0) {
			allRules.add(new R5(s));
		} else if (
				s.indexOf("that is ")>=0 &&
				s.indexOf("link its head to the first ")>=0 &&
				s.indexOf("on the left with ")>=0) {
			allRules.add(new R6(s));
		} else if (
				s.indexOf("link to the ")>=0 &&
				s.indexOf("just before with ")>=0) {
			allRules.add(new R7(s));
			// we now want all rules to be independent _in the first step_
			//		} else if (s.indexOf(" dep ")>=0 &&
			//				s.indexOf("link all children to its head")>=0) {
			//			allRules.add(new R8(s));
		} else {
			System.out.println("ERROR in rule definition "+s);
			return;
		}
		allRulePriors.add(prior);
	}

	void initClasse(String s) {
		final String pref = "class ";
		int off = pref.length();
		int i=s.indexOf(' ',off);
		String classname = s.substring(off, i);
		off=i+1;
		i=s.indexOf(' ',off);
		String elt = s.substring(off,i);
		off=i+1;
		String pat = s.substring(off);
		System.out.println("new class "+classname+" "+elt+" "+pat);
		ClasseDeMots cl = new ClasseDeMots(classname,mott.valueOf(elt),pat);
		classesDeMots.put(cl.nom, cl);
	}

	// =========================================

	public interface PatternRule {
		public String getString();
		public void createRuleFromDefinition(String definition);
		// must check all conditions to apply, especially if some dep already exists
		// must also be able to check whether it has already been applied, in which case it shall not be applied twice !
		// shall save internally the graph + all actions for each index, so that we can call apply only with the index afterwards
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps);
		public void apply(int idx, Map<String, Object> vars);
	}

	class R1 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String deplab = "SUJ";

		public R1(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			final String s1="For all ", s2="link to a ", s3="on the right with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			deplab=definition.substring(off);

			System.out.println("debug cl "+cl1+" "+cl2+" "+deplab);
			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=0;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i))) {
					for (int j=i+1;j<g.getNbMots();j++) {
						if (pats[1].match(g.getMot(j))) {
							govs.add(i);
							heads.add(j);
						}
					}
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				if (canRemoveDeps||d<0) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(idx), heads.get(idx));
		}
	}

	class R2 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String deplab = "SUJ";

		public R2(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="link to a ", s3="on the left with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			deplab=definition.substring(off);

			System.out.println("debug cl "+cl1+" "+cl2+" "+deplab);
			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=1;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i))) {
					for (int j=i-1;j>=0;j--) {
						if (pats[1].match(g.getMot(j))) {
							govs.add(i);
							heads.add(j);
						}
					}
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				if (d<0||canRemoveDeps) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(idx), heads.get(idx));
		}
	}

	class R3 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String deplab = "SUJ";

		public R3(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="link to the first ", s3="on the right with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			deplab=definition.substring(off);

			System.out.println("debug cl "+cl1+" "+cl2+" "+deplab);
			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=0;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i))) {
					for (int j=i+1;j<g.getNbMots();j++) {
						if (pats[1].match(g.getMot(j))) {
							govs.add(i);
							heads.add(j);
							break;
						}
					}
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				if (d<0||canRemoveDeps) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(idx), heads.get(idx));
		}
	}

	class R4 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String deplab = "SUJ";

		public R4(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="link to the first ", s3="on the left with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			deplab=definition.substring(off);

			System.out.println("debug cl "+cl1+" "+cl2+" "+deplab);
			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=0;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i))) {
					for (int j=i-1;j>=0;j--) {
						if (pats[1].match(g.getMot(j))) {
							govs.add(i);
							heads.add(j);
							break;
						}
					}
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				if (d<0||canRemoveDeps) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(idx), heads.get(idx));
		}
	}

	class R5 implements PatternRule {
		String s;
		ClasseDeMots pat=null;
		String deplab = "SUJ";

		public R5(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="link to the word just before with ";
			final int l1=s1.length(), l2=s2.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			deplab=definition.substring(off);

			pat=classesDeMots.get(cl1);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=1;i<g.getNbMots();i++) {
				if (pat.match(g.getMot(i))) {
					govs.add(i);
					heads.add(i-1);
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				if (d<0||canRemoveDeps) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(idx), heads.get(idx));
		}
	}

	class R6 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String depcond = "SUJ";
		String deplab = "MOD";

		public R6(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="that is ", s3="link its head to the first ", s4="on the left with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length(), l4=s4.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			depcond = definition.substring(off, end);
			off=end+1+l3;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l4;
			deplab=definition.substring(off);

			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=1;i<g.getNbMots()-1;i++) {
				if (!pats[0].match(g.getMot(i))) continue;
				int nounbef = -1;
				for (int j=i-1;j>=0;j--) {
					if (pats[1].match(g.getMot(j))) {nounbef=j; break;}
				}
				if (nounbef<0) continue;
				govs.add(i); // ce n'est pas le vrai gov, mais on ne connait pas encore le head de i !!
				heads.add(nounbef);
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				if (d<0) continue; // il n'a pas de HEAD
				if (!g.getDepLabel(d).equals(depcond)) continue; // il n'a pas la bonne dep
				int h=g.getHead(d);
				d=g.getDep(h);
				// on n'ecrase pas de deps existantes
				if (d<0||canRemoveDeps) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			int h=gg.getHead(d);
			d=gg.getDep(h);
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, h, heads.get(idx));
		}
	}

	class R7 implements PatternRule {
		String s;
		ClasseDeMots pats[]={null,null};
		String deplab = "SUJ";

		public R7(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="link to the ", s3="just before with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			deplab=definition.substring(off);

			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=1;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i)) && pats[1].match(g.getMot(i-1))) {
					govs.add(i);
					heads.add(i-1);
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			if (g!=gg) initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				if (d<0||canRemoveDeps) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int d=gg.getDep(govs.get(idx));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(idx), heads.get(idx));
		}
	}

	class R8 implements PatternRule {
		String s;
		int deptyp=-1;
		DetGraph gg;

		public R8(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all dep ", s2="link all children to its head";
			final int l1=s1.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			deptyp=Dep.getType(definition.substring(off, end));
		}

		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			gg=g;
			ArrayList<Integer> lres = new ArrayList<Integer>();
			int[] linkto = new int[g.getNbMots()];
			Arrays.fill(linkto,0);
			for (Dep d : g.deps) {
				if (d.type==deptyp) {
					linkto[d.gov.getIndexInUtt()-1]=d.head.getIndexInUtt(); // commence a 1
				}
			}
			for (Dep d : g.deps) {
				int h=d.head.getIndexInUtt()-1;
				if (linkto[h]>0) {
					// there is a link that goes to that head
					linkto[h]=-linkto[h];
				}
			}
			for (int i=0;i<linkto.length;i++) {
				if (linkto[i]<0) lres.add(i);
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			int h=gg.getHead(gg.getDep(idx));
			Mot oldh = gg.getMot(idx);
			Mot newh = gg.getMot(h);
			for (Dep d : gg.deps) {
				if (d.head==oldh) d.head=newh;
			}
		}
	}

	class R9 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String deplab = "SUJ", nodepson="SUJ";
		int nodeptyp=-1;

		public R9(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			System.out.println("debug def "+definition);
			final String s1="For all ", s2="link to the first ", s3="without ", s4="on the right with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length(), l4=s4.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			end=definition.indexOf(' ',off);
			nodepson=definition.substring(off, end);
			nodeptyp=Dep.getType(nodepson);
			off=end+1+l4;
			deplab=definition.substring(off);

			System.out.println("debug cl "+cl1+" "+cl2+" "+deplab);
			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=0;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i))) {
					for (int j=i+1;j<g.getNbMots();j++) {
						if (pats[1].match(g.getMot(j))) {
							Mot rt = g.getMot(j);
							boolean hasbaddep=false;
							for (Dep d:g.deps) {
								if (d.head==rt&&nodeptyp==d.type) {
									hasbaddep=true; break;
								}
							}
							if (!hasbaddep) {
								govs.add(i);
								heads.add(j);
								break;
							}
						}
					}
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			// on recalcul tjrs les deps car cela depend des deps qui viennent juste d'etre ajoutees !
			initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				// je ne stocke pas l'index, car celui-ci peut etre ecrase ensuite
				// mais je stocke le gov; le head sera recalcule dans apply
				// on sait qu'il n'y a qu'un seul head possible par gov selon le contexte
				if (d<0||canRemoveDeps) lres.add(govs.get(i));
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			initApplicableForGraph(gg);
			int i=govs.indexOf(idx);
			int d=gg.getDep(govs.get(i));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(i), heads.get(i));
		}
	}

	class R10 implements PatternRule {
		String s;
		ClasseDeMots[] pats = {null,null};
		String deplab = "SUJ";
		int maxlen = 5;

		public R10(String st) {s=st; createRuleFromDefinition(s);}

		@Override
		public String getString() { return s;}

		@Override
		public void createRuleFromDefinition(String definition) {
			final String s1="For all ", s2="link to the first ", s3="within ", s4="words on the right with ";
			final int l1=s1.length(), l2=s2.length(), l3=s3.length(), l4=s4.length();
			int off=l1;
			int end=definition.indexOf(' ',off);
			String cl1=definition.substring(off, end);
			off=end+1+l2;
			end=definition.indexOf(' ',off);
			String cl2=definition.substring(off, end);
			off=end+1+l3;
			end=definition.indexOf(' ',off);
			maxlen=Integer.parseInt(definition.substring(off, end));
			off=end+1+l4;
			deplab=definition.substring(off);

			System.out.println("debug cl "+cl1+" "+cl2+" "+deplab);
			pats[0]=classesDeMots.get(cl1);
			pats[1]=classesDeMots.get(cl2);
		}

		DetGraph gg;
		ArrayList<Integer> govs = new ArrayList<Integer>();
		ArrayList<Integer> heads = new ArrayList<Integer>();

		private void initApplicableForGraph(DetGraph g) {
			govs.clear(); heads.clear();
			for (int i=0;i<g.getNbMots();i++) {
				if (pats[0].match(g.getMot(i))) {
					for (int j=i+1;j<g.getNbMots()&&j<i+maxlen;j++) {
						if (pats[1].match(g.getMot(j))) {
							govs.add(i);
							heads.add(j);
							break;
						}
					}
				}
			}
			gg=g;
		}

		/**
		 * PROBLEM: the search re-applies all rules in the sequence all the time,
		 * which means that the indexes of the rules applied must tbe saved as long as we have not changed graphs.
		 */
		@Override
		public int[] getApplicable(DetGraph g, Map<String, Object> vars, boolean canRemoveDeps) {
			// on recalcul tjrs les deps car cela depend des deps qui viennent juste d'etre ajoutees !
			initApplicableForGraph(g);
			ArrayList<Integer> lres = new ArrayList<Integer>();
			for (int i=0;i<govs.size();i++) {
				int gov = govs.get(i);
				int d=g.getDep(gov);
				// on n'ecrase pas de deps existantes
				// je ne stocke pas l'index, car celui-ci peut etre ecrase ensuite
				// mais je stocke le gov; le head sera recalcule dans apply
				// on sait qu'il n'y a qu'un seul head possible par gov selon le contexte
				if (d<0||canRemoveDeps) lres.add(govs.get(i));
			}
			int[] res = new int[lres.size()];
			for (int i=0;i<res.length;i++)  res[i]=lres.get(i);
			return res;
		}

		@Override
		public void apply(int idx, Map<String, Object> vars) {
			initApplicableForGraph(gg);
			int i=govs.indexOf(idx);
			int d=gg.getDep(govs.get(i));
			if (d>=0) gg.removeDep(d);
			gg.ajoutDep(deplab, govs.get(i), heads.get(i));
		}
	}

	// =========================================

	public enum mott {forme, postag, lemme};

	// contient les vars associees a chaque graphe
	// cela ne sert a rien, sauf a stocker des decisions prises par les regles appliquees qui ne peuvent pas etre stockees dans les arbres (comme le ROOT de l'arbre)
	HashMap<DetGraph, Map<String, Object>> g2vars = new HashMap<DetGraph, Map<String,Object>>();
	ArrayList<PatternRule> allRules = new ArrayList<PatternRules.PatternRule>();
	ArrayList<Float> allRulePriors = new ArrayList<Float>();

	class ClasseDeMots {
		public ClasseDeMots(String name, mott t, String p) {
			nom=name; mtyp=t; pat=Pattern.compile(p);
		}
		String nom;
		Pattern pat;
		mott mtyp;
		public boolean match(Mot m) {
			String w=null;
			switch (mtyp) {
			case forme: w = m.getForme(); break;
			case postag: w = m.getPOS(); break;
			case lemme: w = m.getLemme(); break;
			}
			return pat.matcher(w).matches();
		}
		public String toString() {
			return nom+" "+pat+" "+mtyp;
		}
	}

	HashMap<String, ClasseDeMots> classesDeMots = new HashMap<String, PatternRules.ClasseDeMots>();

	public PatternRules() {
		initPatternRules();
	}

	void initPatternRules() {
		try {
			BufferedReader f = new BufferedReader(new FileReader("patternRules.txt"));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.charAt(0)!='%') initRule(s);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	ArrayList<Integer> applicableHandRules = new ArrayList<Integer>();
	ArrayList<Integer> applicableLocalIndexes = new ArrayList<Integer>();
	DetGraph seeng = null;

	/**
	 * This one may remove old deps that are "in the way"
	 */
	public int[] getApplicable(DetGraph g, boolean canRemoveDep) {
		if (seeng!=g) {
			applicableHandRules.clear();
			applicableLocalIndexes.clear();
			seeng=g;
		}
		ArrayList<Integer> lres = new ArrayList<Integer>();
		Map<String,Object> vars = g2vars.get(g);
		if (vars==null) g2vars.put(g, vars);
		int nHandRules = allRules.size();
		for (int i=0;i<nHandRules;i++) {
			int[] local = allRules.get(i).getApplicable(g, vars, canRemoveDep);
			for (int l : local) {
				for (int j=0;j<applicableHandRules.size();j++) {
					if (applicableHandRules.get(j)==i && applicableLocalIndexes.get(j)==l) {
						lres.add(j); break;
					}
				}
				lres.add(applicableHandRules.size());
				applicableHandRules.add(i);
				applicableLocalIndexes.add(l);
			}
		}
		int[] res = new int[lres.size()];
		for (int i=0;i<res.length;i++) res[i]=lres.get(i);
		return res;
	}
	
	/**
	 * This one considers the rule is not applicable when there is already a dep there
	 */
	@Override
	public int[] getApplicable(DetGraph g) {
		return getApplicable(g, false);
	}

	@Override
	public void apply(DetGraph g, int index) {
		Map<String,Object> vars = g2vars.get(g);
		int chosenRule = applicableHandRules.get(index);
		int chosenIndex = applicableLocalIndexes.get(index);
		allRules.get(chosenRule).apply(chosenIndex,vars);
	}

	public static JSafran treesframe = null;
	public void parse(final List<DetGraph> gs, final List<DetGraph> golds) {
		final OneRule r = this;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				// Search calls hillclimbingsearch, which relies on RulesSeq to apply the rules
				RulesSeq.allrules.clear();
				RulesSeq.allrules.add(r);
				Search search = new Search(golds,gs);
				Voc v = new Voc();
				v.init(gs);
				search.scorer=new Combiner();
				search.listeners.add(new Search.listenr() {
					@Override
					public void endOfIter() {
						if (treesframe!=null) {
							treesframe.repaint();
							if (false) {
								// debug
								try {
									BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
									f.readLine();
								} catch (IOException e) {

								}
							}
						}
					}
				});
				Search.stopitBeforeNextIter=false;
				Search.stopitNow=false;
				search.search();
			}
		}, "rulesSampler");
		t.start();
	}

	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		java.util.List<DetGraph> gs = gio.loadAllGraphs("xx.xml");
		//		 java.util.List<DetGraph> gs = gio.loadAllGraphs("test2009.xml");
		//		java.util.List<DetGraph> gs = gio.loadAllGraphs("train2011.xml");
		ArrayList<DetGraph> golds = new ArrayList<DetGraph>();
		for (DetGraph g : gs) {
			golds.add(g.clone());
			g.clearDeps();
		}
		golds=null;
		Search.niters=10;
		Search.nstarts=100;
		PatternRules m = new PatternRules();
		JSafran j = JSafran.viewGraph(gs,false);
		j.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				Search.stopitBeforeNextIter=true;
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		m.treesframe=j;
		m.parse(gs, golds);
	}
}
