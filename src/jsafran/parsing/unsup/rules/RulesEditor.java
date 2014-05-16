package jsafran.parsing.unsup.rules;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.Mot;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.criteria.TopologyLattice;
import jsafran.parsing.unsup.hillclimb.OneRule;
import jsafran.parsing.unsup.hillclimb.RulesSeq;
import jsafran.parsing.unsup.hillclimb.Scorer;
import jsafran.parsing.unsup.hillclimb.Search;

/**
 * 
 * TODO: the idea of writing rules from graph is bad: let's rather use patterns like "link any NOUN to the first next VERB with SUJ"
 * along with definitions of NOUN, VERB, ...
 * 
 * The best option I think=
 * - use patterns in REAL ENGLISH, with "slots" that can be changed
 * - write a java function for each pattern
 * 
 * 
 * @author xtof
 *
 */
public class RulesEditor extends JFrame implements OneRule {
	JLabel helpRules, helpMain, helpConds, helpRes;
	JTextArea txtMain, txtConds, txtRes;
	JComboBox listConds, listRes;
	JComboBox listMain;
	JTextField rname = new JTextField();

	DefaultListModel mod = new DefaultListModel();
	JList rlist = new JList(mod);
	JScrollPane rlistscroll = new JScrollPane(rlist);

	TreeMap<String, Integer> name2rule = new TreeMap<String, Integer>();
	ArrayList<String> rulesmain = new ArrayList<String>();
	ArrayList<String> rulescond = new ArrayList<String>();
	ArrayList<String> rulesres = new ArrayList<String>();

	// used only when ativating a single rule to save the list of all rules:
	private ArrayList<String> rulesmaincach = new ArrayList<String>();
	private ArrayList<String> rulescondcach = new ArrayList<String>();
	private ArrayList<String> rulesrescach = new ArrayList<String>();

	// the order of enum constants is important !
	enum mainst {position,form,lemma,postag,nohead,hasFunction};
	enum condst {position,form,lemma,postag,firstAfter,firstBefore,hasNoSon,nohead,isHeadOf};
	enum resst {addDep};

	private static RulesEditor red = null;
	public static JSafran treesframe = null;
	public boolean doApply = true;
	private ArrayList<Integer> hashfound = new ArrayList<Integer>();
	private HashMap<Character,Integer> allvars = new HashMap<Character, Integer>();
	private HashMap<Integer,Character> allvarsinv = new HashMap<Integer, Character>();

	int singleRule = -1;
	public static boolean pauseIter=false;

	// TODO: remplacer les hashcodes par ceci ! 
	private VarContainer var2val = new VarContainer();
	
	public static RulesEditor getEditor() {
		if (red==null) red = new RulesEditor();
		return red;
	}

	private RulesEditor() {
		super("rules editor");
		initGUI();
		initHelp();
		loadRules("rules.cfg");
		repaint();
	}

	private void printInLabel(JLabel jl, String txt) {
		int w = (int)((float)jl.getWidth()+0.8f);
		String html = 
				"<html><body width='"+w+"px'>" +
						txt +
						"</body></html>";
		jl.setText(html);
	}

	private void mainAction() {
		int c = listMain.getSelectedIndex();
		String y="";
		switch (mainst.values()[c]) {
		case position: y="X.position="; break;
		case form: y="X.form="; break;
		case lemma: y="X.lemma="; break;
		case postag: y="X.postag="; break;
		case nohead: y="X.nohead"; break;
		case hasFunction: y="X.hasFunction="; break;
		}
		txtMain.append(y);
		txtMain.requestFocus();
		txtMain.repaint();
	}
	private void condAction() {
		int c = listConds.getSelectedIndex();
		String y="";
		switch (condst.values()[c]) {
		case form: y="Y.form="; break;
		case lemma: y="Y.lemma="; break;
		case postag: y="Y.postag="; break;
		case firstAfter: y="Y.firstAfter=X\n"; break;
		case firstBefore: y="Y.firstBefore=X\n"; break;
		case position: y="Y.position=X+"; break;
		case hasNoSon: y="Y.hasNoSon=dep_label"; break;
		case nohead: y="Y.nohead"; break;
		case isHeadOf: y="Z.isHeadOf=X"; break;
		}
		txtConds.append(y);
		txtConds.requestFocus();
		txtConds.repaint();
	}
	private void resAction() {
		int c = listRes.getSelectedIndex();
		String y="";
		switch (resst.values()[c]) {
		case addDep: y="X.addDep=YSUJ"; break;
		}
		txtRes.append(y);
		txtRes.requestFocus();
		txtRes.repaint();
	}

	private void initHelp() {
		printInLabel(helpRules, "Enter here the rule name");
		printInLabel(helpMain, "Enter here the main word(s) X to look for");
		printInLabel(helpConds, "Enter here the other word(s) to look for");
		printInLabel(helpRes, "Enter here the result: arcs or groups created");

		for (mainst c: mainst.values()) listMain.addItem(c.name());
		listMain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {mainAction();}
		});
		for (condst c: condst.values()) listConds.addItem(c.name());
		listConds.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {condAction();}
		});
		for (resst c: resst.values()) listRes.addItem(c.name());
		listRes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {resAction();}
		});
	}

	void newrule() {
		String nam = rname.getText().trim();
		if (nam.length()==0) JOptionPane.showMessageDialog(null, "must provide a name to the rule");
		else if (name2rule.containsKey(nam)) JOptionPane.showMessageDialog(null, "this rule already exists");
		else {
			int ridx=rulesmain.size();
			name2rule.put(nam, ridx);
			rulesmain.add("");
			rulescond.add("");
			rulesres.add("");
			mod.addElement(nam);
			rlist.setSelectedIndex(ridx);
		}
		repaint();
	}

	int prevselected=-1;
	void selectRule() {
		String s = (String) rlist.getSelectedValue();
		int ridx = name2rule.get(s);
		txtMain.setText(rulesmain.get(ridx));
		txtConds.setText(rulescond.get(ridx));
		txtRes.setText(rulesres.get(ridx));
		prevselected=ridx;
		repaint();
	}

	String cleanRules(String s) {
		int i=s.length()-1;
		while (i>=0&&s.charAt(i)=='\n') i--;
		if (i==s.length()-1) return s;
		return s.substring(0,i+1);
	}

	void loadRules(String fn) {
		System.out.println("load rules "+fn);
		try {
			BufferedReader f = new BufferedReader(new FileReader(fn));
			for (;;) {
				String n = f.readLine();
				if (n==null) break;
				name2rule.put(n, rulesmain.size());
				mod.addElement(n);
				String smain = "";
				for (;;) {
					String s = f.readLine();
					if (s.startsWith("====")) break;
					smain+=s+'\n';
				}
				smain=cleanRules(smain);
				rulesmain.add(smain);
				String sconds = "";
				for (;;) {
					String s = f.readLine();
					if (s.startsWith("====")) break;
					sconds+=s+'\n';
				}
				sconds=cleanRules(sconds);
				rulescond.add(sconds);
				String sres = "";
				for (;;) {
					String s = f.readLine();
					if (s.startsWith("====")) break;
					sres+=s+'\n';
				}
				sres=cleanRules(sres);
				rulesres.add(sres);
			}
			f.close();
			repaint();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void saveRules(String fn) {
		try {
			PrintWriter f = new PrintWriter(new FileWriter(fn));
			for (String n : name2rule.keySet()) {
				f.println(n);
				int r = name2rule.get(n);
				f.println(rulesmain.get(r));
				f.println("====");
				f.println(rulescond.get(r));
				f.println("====");
				f.println(rulesres.get(r));
				f.println("====");
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initGUI() {
		final Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

		JMenuBar menubar = new JMenuBar();
		setJMenuBar(menubar);
		JMenu debug = new JMenu("rules");
		menubar.add(debug);
		//		JMenuItem run = new JMenuItem("run");
		//		debug.add(run);
		//		run.addActionListener(new ActionListener() {
		//			@Override
		//			public void actionPerformed(ActionEvent arg0) {
		//				// TODO
		//			}
		//		});
		JMenuItem save = new JMenuItem("save");
		debug.add(save);
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkChanges();
				saveRules("rules.cfg");
			}
		});
		JMenuItem stopsoft = new JMenuItem("stopit before next iter");
		debug.add(stopsoft);
		stopsoft.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopit(false);
			}
		});
		JMenuItem stophard = new JMenuItem("stopit NOW !");
		debug.add(stophard);
		stophard.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopit(true);
			}
		});
		JMenuItem test1rule = new JMenuItem("Toggle: only this rule");
		debug.add(test1rule);
		test1rule.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (singleRule>=0) {
					// unhighlight the chosen rule
					singleRule=-1;
					rlist.setEnabled(true);
					rulesmain.clear(); rulescond.clear(); rulesres.clear();
					rulesmain.addAll(rulesmaincach);
					rulescond.addAll(rulescondcach);
					rulesres.addAll(rulesrescach);
					// cache the main rules list
					rulesmaincach.clear(); rulescondcach.clear(); rulesrescach.clear();
				} else {
					// highlight the chosen rule
					singleRule=rlist.getSelectedIndex();
					rlist.setEnabled(false);
					// cache the main rules list
					rulesmaincach.clear(); rulesmaincach.addAll(rulesmain);
					rulescondcach.clear(); rulescondcach.addAll(rulescond);
					rulesrescach.clear();  rulesrescach.addAll(rulesres);
					// keep only one rule
					rulesmain.clear(); rulescond.clear(); rulesres.clear();
					rulesmain.add(rulesmaincach.get(singleRule));
					rulescond.add(rulescondcach.get(singleRule));
					rulesres.add(rulesrescach.get(singleRule));
				}
				repaint();
			}
		});
		JMenuItem pause = new JMenuItem("Toggle: pause btw iters");
		debug.add(pause);
		pause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pauseIter=!pauseIter;
			}
		});

		Box b0 = Box.createHorizontalBox();
		getContentPane().add(b0);

		Box bRules = Box.createVerticalBox(); bRules.setBorder(loweredetched);
		Box bMain = Box.createVerticalBox(); bMain.setBorder(loweredetched);
		Box bConds = Box.createVerticalBox(); bConds.setBorder(loweredetched);
		Box bRes = Box.createVerticalBox(); bRes.setBorder(loweredetched);
		b0.add(bRules); b0.add(bMain); b0.add(bConds); b0.add(bRes);


		helpRules = new JLabel("                                    ");
		helpRules.setBorder(loweredetched);
		bRules.add(helpRules);
		helpMain = new JLabel("                                    ");
		helpMain.setBorder(loweredetched);
		bMain.add(helpMain);
		helpConds = new JLabel("                                    ");
		helpConds.setBorder(loweredetched);
		bConds.add(helpConds);
		helpRes = new JLabel("                                    ");
		helpRes.setBorder(loweredetched);
		bRes.add(helpRes);

		Box newr = Box.createHorizontalBox();
		bRules.add(newr);
		newr.setMaximumSize(new Dimension(10000, 20));
		newr.add(rname);
		JButton newrul = new JButton("new rule");
		newr.add(newrul);
		newrul.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				newrule();
			}
		});
		bRules.add(rlistscroll);
		rlist.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				checkChanges();
				selectRule();
			}
		});

		// menu deroulant avec predefined variables
		listMain=new JComboBox();
		bMain.add(listMain);
		listConds=new JComboBox();
		bConds.add(listConds);
		listRes=new JComboBox();
		bRes.add(listRes);

		txtMain=new JTextArea();
		txtMain.setBorder(loweredetched);
		bMain.add(txtMain);
		txtConds=new JTextArea();
		txtConds.setBorder(loweredetched);
		bConds.add(txtConds);
		txtRes=new JTextArea();
		txtRes.setBorder(loweredetched);
		bRes.add(txtRes);

		setSize(1200, 600);
		setVisible(true);
	}

	private void checkChanges() {
		int ridx = prevselected;
		if (ridx>=0) {
			{
				String txt0 = txtMain.getText();
				String txt1 = rulesmain.get(ridx);
				if (!txt0.equals(txt1)) {
					System.out.println("update rule main...");
					rulesmain.set(ridx, txt0);
				}
			}
			{
				String txt0 = txtConds.getText();
				String txt1 = rulescond.get(ridx);
				if (!txt0.equals(txt1)) {
					System.out.println("update rule conds...");
					rulescond.set(ridx, txt0);
				}
			}
			{
				String txt0 = txtRes.getText();
				String txt1 = rulesres.get(ridx);
				if (!txt0.equals(txt1)) {
					System.out.println("update rule res...");
					rulesres.set(ridx, txt0);
				}
			}
		}
	}

	private int getAbsolutePosition(String s) {
		int i=s.indexOf('=');
		int j=Integer.parseInt(s.substring(i+1));
		return j;
	}

	// TODO: plutot que de tester des string, utiliser un codage int pour les conditions
	private boolean isForm(String s) {
		return s.substring(2).startsWith("form");
		//		return (s.charAt(2)=='f');
	}
	private boolean isHeadOf(String s) {
		return s.substring(2).startsWith("isHeadOf");
	}
	private boolean isNoHead(String s) {
		return (s.charAt(2)=='n');
	}
	private boolean isPOS(String s) {
		return (s.charAt(5)=='t');
	}
	private boolean isLemma(String s) {
		return (s.charAt(2)=='l');
	}
	private boolean isPosition(String s) {
		return (s.charAt(5)=='i');
	}
	private boolean isFirstAfter(String s) {
		return (s.charAt(7)=='A');
	}
	private boolean isFirstBefore(String s) {
		return (s.charAt(7)=='B');
	}
	private boolean isAddDep(String s) {
		return (s.charAt(2)=='a');
	}
	private boolean isHasNoSon(String s) {
		return (s.charAt(2)=='h');
	}
	private boolean isHasFunction(String s) {
		return s.substring(2).startsWith("hasFunction");
	}

	private String[] getMainTest() {
		String s=txtMain.getText();
		String[] res = s.split("\n");
		return res;
	}
	private String[] getMainTest(int r) {
		String s=rulesmain.get(r);
		String[] res = s.split("\n");
		return res;
	}
	private String[] getCondTest() {
		String s=txtConds.getText();
		String[] res = s.split("\n");
		return res;
	}
	private String[] getCondTest(int r) {
		String s=rulescond.get(r);
		String[] res = s.split("\n");
		return res;
	}
	private String[] getResTest() {
		String s=txtRes.getText();
		String[] res = s.split("\n");
		return res;
	}
	private String[] getResTest(int r) {
		String s=rulesres.get(r);
		String[] res = s.split("\n");
		return res;
	}

	// ===================================================================
	public void executeRule(DetGraph g) {
		int ridx=rlist.getSelectedIndex();
		executeRule(ridx, g);
	}
	public void executeRule(int ridx, DetGraph g) {
		String[] st = getMainTest(ridx);
		int wmin=0, wmax=g.getNbMots();
		int j=0;
		if (isPosition(st[j])) {
			int pos=getAbsolutePosition(st[j]);
			if (pos<0||pos>=g.getNbMots()) return;
			wmin=pos;wmax=pos+1;
			++j;
		}
		for (int wi=wmin;wi<wmax;wi++) {
			boolean allcondsok = true;
			mainconds:
				for (int i=j;i<st.length;i++) {
					if (st[i].trim().length()==0) continue;
					mainst elt2check = null;
					Pattern pat=null;
					if (isPOS(st[i])) {
						elt2check=mainst.postag; pat=getPattern(st[i].substring(9));
					} else if (isLemma(st[i])) {
						elt2check=mainst.lemma; pat=getPattern(st[i].substring(8));
					} else if (isForm(st[i])) {
						elt2check = mainst.form; pat=getPattern(st[i].substring(7));
					} else if (isHasFunction(st[i])) {
						elt2check = mainst.hasFunction; pat=getPattern(st[i].substring(14));
					} else if (isNoHead(st[i])) {
						elt2check = mainst.nohead;
					} else {
						JOptionPane.showMessageDialog(null, "ERROR main condition unknown: "+st[i]);
					}
					String w=null;
					boolean still2check=true;
					switch(elt2check) {
					case form:  w=g.getMot(wi).getForme(); break;
					case postag: w=g.getMot(wi).getPOS(); break;
					case lemma: w=g.getMot(wi).getLemme(); break;
					case hasFunction: 
					{
						int d=g.getDep(wi);
						if (d>=0) w=g.getDepLabel(d);
					}
					break;
					case nohead:
					{
						int d=g.getDep(wi);
						if (d>=0) {
							//						if (g.getDepLabel(d).equals(st[i].substring(8))) {
							allcondsok=false;
							break mainconds;
							//						}
						}
						still2check=false;
					}
					break;
					}
					if (still2check&&!doesMatch(pat, w)) {
						allcondsok=false;
						break;
					}
				}
			if (allcondsok) {
				stage2(ridx,g,wi);
			}
		}
	}

	private void stage3(int ridx, DetGraph g, HashMap<Character, Integer> var2pos) {
		if (doApply) {
			String[] st = getResTest(ridx);
			for (int i=0;i<st.length;i++) {
				if (isAddDep(st[i])) {
					char varx = st[i].charAt(0);
					int k=st[i].indexOf('=');
					char vary=st[i].charAt(k+1);
					int posx=var2pos.get(varx);
					int posy=var2pos.get(vary);
					String lab=st[i].substring(k+2);
					int d=g.getDep(posx);
					if (d>=0) g.removeDep(d);
					g.ajoutDep(lab, posx, posy);
				}
			}
		} else {
			// a ce niveau, le hash code est l'ensemble des valeurs affectees aux vars
			// on y ajoutera ensuite l'indice de la regle
			int h = getHash(var2pos);
			hashfound.add(h);
		}
	}

	boolean negpat=false;
	private Pattern getPattern(String s) {
		if (s.charAt(0)=='!') {
			negpat=true;
			Pattern pat = Pattern.compile(s.substring(1));
			return pat;
		} else {
			negpat=false;
			Pattern pat = Pattern.compile(s);
			return pat;
		}
	}
	private boolean doesMatch(Pattern pat, String w) {
		if (w==null) return false;
		if (negpat) return !pat.matcher(w).matches();
		else return pat.matcher(w).matches();
	}

	private boolean checkcond(String s, DetGraph g, int w, Map<Character,Integer> var2pos) {
		s=s.trim();
		if (s.length()==0) return true; // au cas ou on a des lignes vides !
		condst elt2check = null;
		Pattern pat=null;
		if (isPOS(s)) {
			elt2check=condst.postag; pat=getPattern(s.substring(9));
		} else if (isLemma(s)) {
			elt2check=condst.lemma; pat=getPattern(s.substring(8));
		} else if (isForm(s)) {
			elt2check=condst.form; pat=getPattern(s.substring(7));
		} else if (isHasNoSon(s)) {
			elt2check=condst.hasNoSon;
		} else if (isNoHead(s)) {
			elt2check=condst.nohead;
		} else if (isHeadOf(s)) {
			elt2check=condst.isHeadOf;
		}
		if (elt2check==null) {
			JOptionPane.showMessageDialog(null, "ERROR condition unknown "+s);
			return false;
		}
		switch(elt2check) {
		case form: if (doesMatch(pat, g.getMot(w).getForme())) return true; break;
		case postag: if (doesMatch(pat, g.getMot(w).getPOS())) return true; break;
		case lemma: if (doesMatch(pat, g.getMot(w).getLemme())) return true; break;
		case nohead: if (g.getDep(w)<0) return true; break;
		case isHeadOf:
		{
			int i=s.indexOf('='); char x=s.charAt(i+1);
			int xi = var2pos.get(x);
			int d=g.getDep(xi);
			if (d>=0) {
				int h=g.getHead(d);
				char z=s.charAt(0);
				var2pos.put(z, h);
				return true;
			}
		}
		break;
		case hasNoSon:
			String lab = s.substring(11);
			int labt = Dep.getType(lab);
			boolean hasSon=false;
			Mot ymot=g.getMot(w);
			for (Dep d : g.deps)
				if (labt==d.type && d.head==ymot) {hasSon=true; break;}
			return !hasSon;
		}
		return false;
	}

	private void stage2(int ridx, DetGraph g, int pos) {
		String[] st = getCondTest(ridx);

		HashMap<Character, Integer> var2pos = new HashMap<Character, Integer>();
		var2pos.put('X', pos);
		{
			int i=0;
			char vary = st[i].charAt(0);
			if (isPosition(st[i])) {
				// relative position
				int k=st[i].indexOf('=');
				char varx=st[i].charAt(k+1);
				int posx=var2pos.get(varx);
				int posy=posx;
				int j=st[i].indexOf('+');
				if (j>=0) {
					posy+=Integer.parseInt(st[i].substring(j+1));
				} else {
					j=st[i].indexOf('-');
					if (j>=0) {
						posy-=Integer.parseInt(st[i].substring(j+1));
					} else System.out.println("PROBLME REL POS "+st[i]);
				}
				if (posy<0||posy>=g.getNbMots()) return;

				// we found the position of Y, we now check the next conditions: if there are not satisfied, then we don't retain this word !
				// in the meantime, we put the position of Y in the list to eventually use this information, but we may remove it afterwards !
				HashMap<Character, Integer> origvars = new HashMap<Character, Integer>();
				origvars.putAll(var2pos);
				var2pos.put(vary, posy);
				int l;
				for (l=i+1;l<st.length;l++) {
					if (!checkcond(st[l], g, posy, var2pos)) break;
				}
				if (l<st.length) {
					// don't match; must remove all variables that have been added
					var2pos.clear();
					var2pos.putAll(origvars);
				}
			} else if (isFirstAfter(st[i])) {
				int k=st[i].indexOf('=');
				char varx=st[i].charAt(k+1);
				int posx=var2pos.get(varx);
				var2pos.remove(vary);
				for (int j=posx+1;j<g.getNbMots();j++) {
					/*
					 * ATTENTION: lorsqu'on cherche "le premier apres/avant", il y a 2 types de conditions, qui sont separees par une ligne vide
					 * les premieres conditions (avant la ligne) doivent toutes etres respectees avant de s'arreter sur un mot
					 * Lorsqu'on s'est arrete sur un mot, on verifie les conditions suivante: si elles ne sont pas verifiees, la regle nest pas applicable !
					 */

					// 1er type de conditions
					boolean condsOK=true;
					int l;
					for (l=i+1;l<st.length;l++) {
						String s=st[l].trim();
						if (s.length()==0) break;
						if (!checkcond(st[l], g, j, var2pos)) {condsOK=false; break;}
					}
					// 2eme type de conditions
					if (condsOK) {
						HashMap<Character, Integer> origvars = new HashMap<Character, Integer>();
						origvars.putAll(var2pos);
						var2pos.put(vary, j);
						for (++l;l<st.length;l++) {
							if (!checkcond(st[l], g, j, var2pos)) {condsOK=false; break;}
						}
						if (!condsOK) {
							// don't match; must remove all variables that have been added
							var2pos.clear();
							var2pos.putAll(origvars);
						}
						// on ne cherche pas de mots plus loin
						break;
					}
				}
			} else if (isFirstBefore(st[i])) {
				int k=st[i].indexOf('=');
				char varx=st[i].charAt(k+1);
				int posx=var2pos.get(varx);
				var2pos.remove(vary);

				for (int posy=posx-1;posy>=0;posy--) {
					// 1er type de conditions
					boolean condsOK=true;
					int l;
					for (l=i+1;l<st.length;l++) {
						String s=st[i].trim();
						if (s.length()==0) break;
						if (!checkcond(st[l], g, posy, var2pos)) {
							condsOK=false; break;}
					}
					// 2eme type de conditions
					if (condsOK) {
						HashMap<Character, Integer> origvars = new HashMap<Character, Integer>();
						origvars.putAll(var2pos);
						var2pos.put(vary, posy);
						for (++l;l<st.length;l++) {
							if (!checkcond(st[l], g, posy, var2pos)) {condsOK=false; break;}
						}
						if (!condsOK) {
							// don't match; must remove all variables that have been added
							var2pos.clear();
							var2pos.putAll(origvars);
						}
						// on ne cherche pas de mots plus loin
						break;
					}
				}
			}
			if (var2pos.containsKey(vary)) stage3(ridx,g,var2pos);
		}
	}

	/**
	 * look into all rules to list all possible vars used in the rules;
	 * this number is used to compute the hash codes
	 */
	private void initNvars() {
		allvars.clear();
		allvarsinv.clear();
		for (String s:rulesmain) {
			String[] st = s.split("\n");
			for (String sx : st) {
				String sy=sx.trim();
				if (sy.length()>0) {
					char c = sy.charAt(0);
					if (!allvars.containsKey(c)) {
						int i=allvars.size();
						allvars.put(c,i);
					}
				}
			}
		}
		for (String s:rulescond) {
			String[] st = s.split("\n");
			for (String sx : st) {
				String sy=sx.trim();
				if (sy.length()>0) {
					char c = sy.charAt(0);
					if (!allvars.containsKey(c)) {
						int i=allvars.size();
						allvars.put(c,i);
					}
				}
			}
		}
		for (char c : allvars.keySet()) {
			allvarsinv.put(allvars.get(c), c);
		}
	}

	void stopit(boolean hardStop) {
		if (hardStop) Search.stopitNow=true;
		else Search.stopitBeforeNextIter=true;
	}

	public void parse(final List<DetGraph> gs, final List<DetGraph> golds) {
		final OneRule r = this;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				RulesSeq.allrules.clear();
				RulesSeq.allrules.add(r);
				initNvars();
				Search search = new Search(golds,gs);
				Voc v = new Voc();
				v.init(gs);
				search.scorer=new Combiner();
				search.listeners.add(new Search.listenr() {
					@Override
					public void endOfIter() {
						if (treesframe!=null) {
							treesframe.repaint();
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

	@Override
	public int[] getApplicable(DetGraph g) {
		ArrayList<Integer> hash = new ArrayList<Integer>();
		doApply=false;
		final int nrules = rulesmain.size();
		for (int r=0;r<nrules;r++) {
			hashfound.clear();
			executeRule(r,g);
			// ajoute l'indice de la rule au hash
			for (int h : hashfound) {
				hash.add(h*nrules+r);
			}
		}
		// par defaut, apply doit tjrs etre a true
		doApply=true;
		int[] res = new int[hash.size()];
		for (int i=0;i<res.length;i++) res[i]=hash.get(i);
		return res;
	}

	@Override
	public void apply(DetGraph g, int hashCode) {
		final int nrules = rulesmain.size();
		int r=hashCode%nrules;
		int h=hashCode/nrules;
		HashMap<Character, Integer> var2pos = dehash(h);
		stage3(r, g, var2pos);
	}

	/*
	 * Attention: le hash inclut toutes les vars impliquees dans une rule, donc on peut par exemple lier a la suite un meme pronom
	 * en tant que sujet d'un verbe, puis d'un autre verbe.
	 * Pour eviter cela, il faut donc soit modifier la definition du HASH en considerant 2 classes de variables: celles qui ne doivent pas etre repliquees
	 * et celles qui le peuvent (mais c'est complique, et discutable...)
	 * soit en ajoutant une contrainte aux regles (comme SUJ) pour refuser d'appliquer une regle si il y a deja une dep SUJET
	 */
	private HashMap<Character, Integer> dehash(int h) {
		HashMap<Character, Integer> res = new HashMap<Character, Integer>();
		for (int f=0;;f++) {
			int n=h%100;
			if (n>0) {
				char var=allvarsinv.get(f);
				res.put(var, n-1);
			}
			h/=100;
			if (h<1) break;
		}
		// ex: h=603
		// f=0, n=3, X=3
		// f=1, n=6, Y=6
		return res;
	}
	private int getHash(HashMap<Character, Integer> var2pos) {
		int h=0;
		for (Character var : var2pos.keySet()) {
			int pos=var2pos.get(var)+1;
			int fact=allvars.get(var);
			// la position maximale est < 100
			// on eleve a la puissance de 100
			for (int i=0;i<fact;i++) pos*=100;
			h+=pos;
		}
		// ex: X(0)=3,Y(0)=6
		// pos=3, fact=0: h+=3
		// pos=6, fact=1: h+=600
		return h;
	}

	public static class Combiner implements Scorer {

		LexPref s1 = new LexPref();
		TopoScorer s2 = new TopoScorer();
		DeplenScorer s3 = new DeplenScorer();

		final Scorer[] scorers = {s1,s2,s3};
		final double[] weights = {0,0.8,0.2};

		@Override
		public double getScore(DetGraph g) {
			double sc = 0;
			for (Scorer s : scorers) sc+=s.getScore(g);
			return sc;
		}
		@Override
		public void incCounts(DetGraph g) {
			for (Scorer s : scorers) s.incCounts(g);
		}
		@Override
		public void decCounts(DetGraph g) {
			for (Scorer s : scorers) s.decCounts(g);
		}

	}

	public static class DeplenScorer implements Scorer {
		@Override
		public double getScore(DetGraph g) {
			if (g.deps.size()==0) return 0;
			int ltot=0;
			for (Dep d : g.deps) {
				ltot+=Math.abs(d.gov.getIndexInUtt()-d.head.getIndexInUtt());
			}
			return -(double)ltot/(double)g.deps.size();
		}
		@Override
		public void incCounts(DetGraph g) {
		}
		@Override
		public void decCounts(DetGraph g) {
		}
	}
	
	public static class LexPref implements Scorer {
		HashMap<String, Integer> voc = new HashMap<String,Integer>();
		// final int maxnvoc = 50000;
		final int maxnvoc = 10000;
		final byte[][] counts = new byte[maxnvoc][maxnvoc];
		final int[] sums = new int[maxnvoc];

		private int getVoc(String mot) {
			Integer i = voc.get(mot);
			if (i==null) {
				// commence a 1, car garde 0 pour ROOT
				i=voc.size()+1; voc.put(mot, i);
			}
			return i;
		}

		@Override
		public double getScore(DetGraph g) {
			double sc=0;
			for (int i=0;i<g.getNbMots();i++) {
				int w=getVoc(g.getMot(i).getForme());
				int d=g.getDep(i);
				int h=0;
				if (d>=0) h=getVoc(g.getMot(g.getHead(d)).getForme());
				double s = (double)counts[w][h]/(double)(sums[h]+1);
				sc+=s;
			}
			return sc;
		}

		@Override
		public void incCounts(DetGraph g) {
			for (int i=0;i<g.getNbMots();i++) {
				int w=getVoc(g.getMot(i).getForme());
				int d=g.getDep(i);
				int h=0;
				if (d>=0) h=getVoc(g.getMot(g.getHead(d)).getForme());
				counts[w][h]++;
				sums[h]++;
			}
		}

		@Override
		public void decCounts(DetGraph g) {
			for (int i=0;i<g.getNbMots();i++) {
				int w=getVoc(g.getMot(i).getForme());
				int d=g.getDep(i);
				int h=0;
				if (d>=0) h=getVoc(g.getMot(g.getHead(d)).getForme());
				counts[w][h]--;
				sums[h]--;
			}
		}
	}

	public static class TopoScorer implements Scorer {
		@Override
		public double getScore(DetGraph g) {
			int s = TopologyLattice.getTopoScore(g);
			return -s;
		}
		@Override
		public void incCounts(DetGraph g) {
		}
		@Override
		public void decCounts(DetGraph g) {
		}
	}
	
	public static void main(String args[]) {
		RulesEditor m = RulesEditor.getEditor();
		
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs("y.xml");
		
		Search.niters=1;
		Search.nstarts=1;
		m.parse(gs, null);
		JSafran j = JSafran.viewGraph(gs);
		m.treesframe=j;
	}
}
