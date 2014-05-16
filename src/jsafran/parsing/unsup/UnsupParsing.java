package jsafran.parsing.unsup;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.sound.midi.Sequence;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import utils.SimpleBarChart;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;
import jsafran.parsing.ClassificationResult;
import jsafran.parsing.ClassificationResultSRL;
import jsafran.parsing.unsup.criteria.DepLen;
import jsafran.parsing.unsup.criteria.DepthFirst;
import jsafran.parsing.unsup.criteria.LexPref;
import jsafran.parsing.unsup.criteria.RuleCrit3;
import jsafran.parsing.unsup.criteria.RuleCrit3EN;
import jsafran.parsing.unsup.criteria.RuleCrit3ENSem;
import jsafran.parsing.unsup.criteria.RuleCrit3Sem;
import jsafran.parsing.unsup.criteria.RuleCrit3ENSemV01;
import jsafran.parsing.unsup.criteria.RuleCrit3SemV01;
import jsafran.parsing.unsup.criteria.TopologyLattice;
import jsafran.parsing.unsup.criteria.VerbFrames;
import jsafran.parsing.unsup.hillclimb.Search;
import jsafran.parsing.unsup.rules.Rules;
import jsafran.parsing.unsup.rules.RulesCfgfile;
import jsafran.parsing.unsup.rules.RulesEditor;

/**
 * first res on FTB: LAS averaged over last 10 iterations, 50 iterations done
 * 
 * Random: 51.8
 * Verb Frames: 53.2
 * Lex Pref: 52.5impossible d'acceder au fichier de config... try in the jar 
 * Both: 54.0
 * 
 * tests: ajouter plus d'iters n'ameliore pas...
 * 
 * TODO: remore depsSaved: clear = del all deps but _*
 * 
 * @author xtof
 *
 */
public class UnsupParsing {

	static boolean doTraining = true;
	static final String countsfilename = "bestcounts.bin";

	// please don't change its default value, as it can now be set by cmd-line option
	boolean oracle = false;
	boolean doSRL=false;
	boolean isConll05=false;
	boolean versionV01=false;
	boolean drawBarChart=false;
	boolean goldInit=false;

	double penalization=-5;
	ArrayList<Double> hiCurrentSamplingPerGraph;

	JSafran m;
	Rules rules;
	Random rand = new Random();
	int testFrom=0;
	RuleCrit3 crit;
	RuleCrit3ENSem critsem;
	RuleCrit3ENSemV01 critsemV01;

	private ArrayList<Integer> ambiguity = new ArrayList<Integer>();

	public static int gidx=-1;
	private DetGraph goldG = null; // for oracle
	private DetGraph goldGSRL = null; // for oracle

	// pour detecter au cours des iterations tous les mots qui ne sont traites par aucune regle
	class MotTraite {
		int gidx, widx;
		public MotTraite(int g, int w) {gidx=g; widx=w;}
		public boolean equals(Object o) {
			MotTraite m = (MotTraite)o;
			return (m.gidx==gidx && m.widx==widx);
		}
		public int hashCode() {
			return gidx+widx;
		}
	}

	public static String help() {
		final String[] hs = {
				"Unsupervised parsing is based on the following principle:",
				"1- You define rules that generate dependency arcs",
				"2- A Gibbs samping algorithm iteratively chooses the rules to apply",
				"",
				"The rules are defined aither in Java, or in the configuration file (graph manip. language)",
				"Shipped-in Wall Street Journal rules are only defined in Java in the source code, while",
				"French rules are implemented both in Java-code and in the text configuration file",
				"",
				"If you use >=2 levels, with the gold parses in level0, then the LAS and UAS will be reported after every iteration",
				"In this case, you can load in level=1 both a train+test corpus; you then have to manually set the first",
				"utterance of the test corpus in level 1 by going to that sentence and then using menu --set TEST offset--",
		};
		StringBuilder sb = new StringBuilder();
		for (String hh : hs) {
			sb.append(hh); sb.append('\n');
		}
		return sb.toString();
	}

	public JMenu getMenu() {
		JMenu dmen = new JMenu("Unsupervised");
		{
			JMenuItem m1 = new JMenuItem("help");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					JOptionPane.showMessageDialog(null, help());
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("del long sentences...");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String s=JOptionPane.showInputDialog("Enter maximum sentence length:");
					if (s!=null) {
						final int len = Integer.parseInt(s);
						for (int i=m.allgraphs.size()-1;i>=0;i--) {
							if (m.allgraphs.get(i).getNbMots()>len) m.allgraphs.remove(i);
						}
						m.repaint();
					}
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("parse all ENGLISH");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RulesCfgfile.ENGLISH=true;
					parse(m.allgraphs,m.levels.size()>1?m.levels.get(0):null,true);
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("parse all FRENCH");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RulesCfgfile.ENGLISH=false;
					doTraining=true;
					parse(m.allgraphs,m.levels.size()>1?m.levels.get(0):null,true);
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("parse all FRENCH (no training)");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RulesCfgfile.ENGLISH=false;
					doTraining=false;
					parse(m.allgraphs,m.levels.size()>1?m.levels.get(0):null,true);
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("parse all joint English");
			m1.addActionListener(new ActionListener() {
				//gold for srl in level 1!!!
				@Override
				public void actionPerformed(ActionEvent arg0) {
					doSRL=true;
					parseSRL(m.allgraphs,m.levels.size()>=2?m.levels.get(0):null,m.levels.size()>=3?m.levels.get(1):null,true);
				}
			});
			dmen.add(m1);

		}
		{
			JMenuItem m1 = new JMenuItem("parse all joint French");
			m1.addActionListener(new ActionListener() {
				//gold for srl in level 1!!!
				@Override
				public void actionPerformed(ActionEvent arg0) {
					doSRL=true;
                                        //doTraining=false;
					RulesCfgfile.ENGLISH=false;
					//parse(m.allgraphs,m.levels.size()>1?m.levels.get(0):null,true);
					parseSRL(m.allgraphs,m.levels.size()>=2?m.levels.get(0):null,m.levels.size()>=3?m.levels.get(1):null,true);
				}
			});
			dmen.add(m1);

		}
		{
			JMenuItem m1 = new JMenuItem("parse one");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					parseOne();
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("compute LAS");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					List<DetGraph> golds = m.levels.get(0);
					List<DetGraph> recs = m.allgraphs.subList(m.allgraphs.size()-m.levels.get(0).size(), m.allgraphs.size());
					float[] acc = ClassificationResult.calcErrors(recs, golds);
					m.marks = ClassificationResult.errors;
					JOptionPane.showMessageDialog(null, "LAS="+acc[0]+" UAS="+acc[1]);
					m.repaint();
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("set TEST offset");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					setTestFrom(m.curgraph);
				}
			});
			dmen.add(m1);
		}
		dmen.addSeparator();
		{
			JMenuItem m1 = new JMenuItem("save structures");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					crit.saveStructLearned("struct.txt",m.allgraphs);
				}
			});
			dmen.add(m1);
		}
		//		{
		//			JMenuItem m1 = new JMenuItem("save counts");
		//			m1.addActionListener(new ActionListener() {
		//				@Override
		//				public void actionPerformed(ActionEvent arg0) {
		//					for (Criterion c : criteria) c.saveCounts();
		//				}
		//			});
		//			dmen.add(m1);
		//		}
		//		{
		//			JMenuItem m1 = new JMenuItem("show post");
		//			m1.addActionListener(new ActionListener() {
		//				@Override
		//				public void actionPerformed(ActionEvent arg0) {
		//					computeLogPost(m.allgraphs);
		//					for (Criterion c : criteria) {
		//						c.charid=0;
		//						c.showOneUtt(m.allgraphs.get(m.curgraph));
		//						break;
		//					}
		//				}
		//			});
		//			dmen.add(m1);
		//		}
		//		{
		//			final JCheckBoxMenuItem m1 = new JCheckBoxMenuItem("check coverage");
		//			m1.addActionListener(new ActionListener() {
		//				@Override
		//				public void actionPerformed(ActionEvent arg0) {
		//					checkCoverage=m1.isSelected();
		//				}
		//			});
		//			dmen.add(m1);
		//		}
		//		{
		//			JMenuItem m1 = new JMenuItem("apply all rules");
		//			m1.addActionListener(new ActionListener() {
		//				@Override
		//				public void actionPerformed(ActionEvent arg0) {
		//					for (Criterion c : criteria) c.init(m.allgraphs);
		//					createRules();
		//					rules.init(m.allgraphs);
		//					for (DetGraph g : m.allgraphs) init(g);
		//					((RulesJava)rules).createAllFirst(m.allgraphs);
		//					for (DetGraph g : m.allgraphs) increaseCounts(g);
		//					m.repaint();
		//				}
		//			});
		//			dmen.add(m1);
		//		}
		//		{
		//			JMenuItem m1 = new JMenuItem("get Oracle rules");
		//			m1.addActionListener(new ActionListener() {
		//				@Override
		//				public void actionPerformed(ActionEvent arg0) {
		//					oracleRules(m.levels.get(0));
		//				}
		//			});
		//			dmen.add(m1);
		//		}
		{
			JMenuItem m1 = new JMenuItem("debug");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					double ptot=0;
					for (DetGraph g : m.allgraphs) {
						ptot+=crit.getLogPost(g, null);
					}
					System.out.println("Total posterior "+ptot);
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("open rules editor");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RulesEditor.getEditor();
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("apply rules from editor");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// TODO: get and add marks
					for (int i=0;i<m.allgraphs.size();i++)
						RulesEditor.getEditor().executeRule(m.allgraphs.get(i));
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("sample rules from editor");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RulesEditor.treesframe=m;
					RulesEditor.getEditor().parse(m.allgraphs, m.levels.get(0));
				}
			});
			dmen.add(m1);
		}
		{
			JMenuItem m1 = new JMenuItem("just one utt");
			m1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					RulesEditor.treesframe=m;
					ArrayList<DetGraph> tmpgraphs = new ArrayList<DetGraph>();
					tmpgraphs.add(m.allgraphs.get(m.curgraph));
					RulesEditor.getEditor().parse(tmpgraphs, null);
				}
			});
			dmen.add(m1);
		}
		return dmen;
	}

	void setTestFrom(int testgidx) {
		testFrom = testgidx;
	}

	LexPref lp;
	VerbFrames framecrit;
	DepLen deplen;
	Voc voc = new Voc();

	public UnsupParsing(JSafran main) {
		m=main;
	}

	JLabel unsuplab = new JLabel("unsupervised parsing running...          ");
	JLabel unsupacc = new JLabel("performances... ???");
	JFrame uf=null;
	boolean stopit = false;
	private void unsupGUI() {
		unsupacc.setPreferredSize(new Dimension(200,150));
		JPanel unsupgui = new JPanel();
		JButton stop = new JButton("stopit");
		uf = new JFrame("unsupervised parsing");
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				stopit=true;
			}
		});
		Box b1 = Box.createHorizontalBox();
		b1.add(Box.createHorizontalGlue());
		b1.add(stop);
		b1.add(Box.createHorizontalGlue());
		/*
		final JRadioButton en = new JRadioButton("English",true); RulesCfgfile.ENGLISH=true;
		en.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (en.isSelected()) {
					RulesCfgfile.ENGLISH=true;
				}
			}
		});
		final JRadioButton fr = new JRadioButton("French",false);
		fr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (fr.isSelected()) {
					RulesCfgfile.ENGLISH=false;
				}
			}
		});
		ButtonGroup bg = new ButtonGroup();
		bg.add(en);bg.add(fr);
		b1.add(en);
		b1.add(Box.createHorizontalGlue());
		b1.add(fr);
		b1.add(Box.createHorizontalGlue());
		 */
		Box b0 = Box.createVerticalBox();
		b0.add(Box.createVerticalGlue());
		b0.add(unsuplab);
		b0.add(Box.createVerticalGlue());
		b0.add(unsupacc);
		b0.add(Box.createVerticalGlue());
		b0.add(b1);
		//		b0.add(Box.createVerticalGlue());
		//		b0.add(new JScrollPane(unsupacc, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		b0.add(Box.createVerticalGlue());

		unsupgui.add(b0);

		uf.getContentPane().add(unsupgui);
		uf.setSize(500,200);
		uf.setVisible(true);
	}

	private void createRules() {
		rules = new RulesCfgfile(doSRL);

                // rules = new RulesJava();
		if (RulesCfgfile.ENGLISH) {
			if (doSRL) {
				rules.setSRL(true);
				if (versionV01) critsemV01=new RuleCrit3ENSemV01(rules, voc);
				else critsem = new RuleCrit3ENSem(rules, voc);

			}
			else crit = new RuleCrit3EN(rules, voc);
                }
                else{
			if (doSRL) {
				rules.setSRL(true);
                                //TODO:ALE!! SEE WHAT TO PUT HERE
                                critsem = new RuleCrit3ENSem(rules, voc);

			}
			else {//crit = new RuleCrit3(rules,voc);
				if (doTraining) crit = new RuleCrit3(rules,voc);
				else {
					crit = RuleCrit3.loadCounts(countsfilename,rules);
					voc = crit.voc;
				}
			}
                }
	}

	// assume global unsup parsing have already been done
	void parseOne() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				DetGraph g = m.allgraphs.get(m.curgraph);
				giter=0;
				int gidx=0;
				int debsav = debug;
				debug=6;
				sample(g,gidx);
				debug=debsav;
				double probApres = crit.getLogPost(g,graph2word2ruleApplied.get(g));
				System.out.println("prob apres: "+probApres);
			}
		});
		t.start();
	}
	public void parse(final List<DetGraph> gs, final List<DetGraph> golds, boolean withGUI) {
		if (golds.size()<gs.size() && testFrom==0) {
			testFrom=gs.size()-golds.size();
			System.out.println("auto set test from "+testFrom);
		}

		// reload every time the set of rules
		createRules();

		// asynchrone pour qu'on puisse voir l'evolution du parsing
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				if (stopit) return;
				sampleAll(gs, golds,null);
				// reinit to allow for a new parsing from now on
				stopit=false;
				uf.dispose();
			}
		},"Unsup Parsing");
		t.start();

		if (withGUI) unsupGUI();
	}

	private List<DetGraph> addGraphs(List<DetGraph> gs){
		for (int i=0;i<gs.size();i++){
			DetGraph g=gs.get(i);
			g.relatedGraphs=new ArrayList<DetGraph>();
			DetGraph g1= g.clone();
			g1.deps.clear();
			g.relatedGraphs.add(g1);
			//                gs.set(i, g);
		}
		return gs;
	}
	//parseSRL(m.allgraphs,m.levels.size()>1?m.levels.get(0):null,m.levels.size()>1?m.levels.get(1):null,true);
	public void parseSRL(final List<DetGraph> gs, final List<DetGraph> golds, final List<DetGraph> goldsSRL,boolean withGUI) {
		if (golds.size()<gs.size() && testFrom==0) {
			testFrom=gs.size()-golds.size();
			System.out.println("auto set test from "+testFrom);
		}

		// reload every time the set of rules
		createRules();
		//for each graph, add the link for the new level
		final List<DetGraph> newgs=addGraphs(gs);
		// asynchrone pour qu'on puisse voir l'evolution du parsing
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				if (stopit) return;
				sampleAll(newgs, golds,goldsSRL);
				// reinit to allow for a new parsing from now on
				stopit=false;
				uf.dispose();
			}
		},"Unsup Parsing");
		t.start();

		if (withGUI) unsupGUI();
	}

//<<<<<<< HEAD

        /*** for the gold initialization of syntax and semantics***/
        private List<DetGraph> initializeGraphGold(List<DetGraph>gs,List<DetGraph> golds,List<DetGraph> goldsSRL){
            String[] cor={"A0","A1","A2","A3","A4"};
            //String[] coarseRarr={"A0","A1","A2","A3","A4","AM"};
            //Set<String> setcoarseRoles=new HashSet<String>();
            Set<String> setcor=new HashSet<String>();
            for (String s:cor) setcor.add(s);
            for (int ind=0;ind<gs.size();ind++){
                DetGraph g= gs.get(ind);
                for (Dep d:golds.get(ind).deps){
                    String lref= d.toString();
                    int gov=d.gov.getIndexInUtt()-1;
                    int he=d.head.getIndexInUtt()-1;
                    g.ajoutDep(lref, gov, he);
                }
                DetGraph gsrl=gs.get(ind).relatedGraphs.get(0);
                for (int i=0;i<goldsSRL.get(ind).deps.size();i++){
                    Dep depe=goldsSRL.get(ind).deps.get(i);
                    if (depe.getHead().getPOS().startsWith("VB")){
                        //change the type in case it start with C or R or 
        		String lref="NONE";
                        lref= depe.toString();
                        int gov=depe.getGov().getIndexInUtt()-1;
                        int head=depe.getHead().getIndexInUtt()-1;
                        //lref= gref.getDepLabel(dep.id);
                        if (setcor.contains(lref)){
                            if (lref.equals("AM-EXT")||lref.startsWith("AM")) lref="AM";
                            if (lref.contains("AM-")) lref="AM";
                            gsrl.ajoutDep(lref, gov, head);
                        }
                        else{
                            if (lref.startsWith("AM")||lref.startsWith("C-")){
                                String newlref="";
                                if (lref.startsWith("AM")) newlref="AM";
                                if (lref.startsWith("C-")) newlref=lref.substring(2,lref.length());
                                if (lref.startsWith("R-")) newlref=lref.substring(2,lref.length());
                                if (lref.contains("AM-")) newlref="AM";
                                gsrl.ajoutDep(newlref,gov ,head);
                            }
                        }
                        gs.get(ind).relatedGraphs.set(0,gsrl );
                    }
                }
                
            }
            return gs;
        }
        
//=======
//	/*** for the gold initialization of syntax and semantics***/
//	private List<DetGraph> initializeGraphGold(List<DetGraph>gs,List<DetGraph> golds,List<DetGraph> goldsSRL){
//		String[] cor={"A0","A1","A2","A3","A4"};
//		//String[] coarseRarr={"A0","A1","A2","A3","A4","AM"};
//		//Set<String> setcoarseRoles=new HashSet<String>();
//		Set<String> setcor=new HashSet<String>();
//		for (String s:cor) setcor.add(s);
//		for (int ind=0;ind<gs.size();ind++){
//			DetGraph g= gs.get(ind);
//			for (Dep d:golds.get(ind).deps){
//				String lref= d.toString();
//				int gov=d.gov.getIndexInUtt()-1;
//				int he=d.head.getIndexInUtt()-1;
//				g.ajoutDep(lref, gov, he);
//			}
//			DetGraph gsrl=gs.get(ind).relatedGraphs.get(0);
//			for (int i=0;i<goldsSRL.get(ind).deps.size();i++){
//				Dep depe=goldsSRL.get(ind).deps.get(i);
//				if (depe.getHead().getPOS().startsWith("VB")){
//					//change the type in case it start with C or R or 
//					String lref="NONE";
//					lref= depe.toString();
//					int gov=depe.getGov().getIndexInUtt()-1;
//					int head=depe.getHead().getIndexInUtt()-1;
//					//lref= gref.getDepLabel(dep.id);
//					if (setcor.contains(lref)){
//						if (lref.equals("AM-EXT")||lref.startsWith("AM")) lref="AM";
//						if (lref.contains("AM-")) lref="AM";
//						gsrl.ajoutDep(lref, gov, head);
//					}
//					else{
//						if (lref.startsWith("AM")||lref.startsWith("C-")){
//							String newlref="";
//							if (lref.startsWith("AM")) newlref="AM";
//							if (lref.startsWith("C-")) newlref=lref.substring(2,lref.length());
//							if (lref.startsWith("R-")) newlref=lref.substring(2,lref.length());
//							if (lref.contains("AM-")) newlref="AM";
//							gsrl.ajoutDep(newlref,gov ,head);
//						}
//					}
//					gs.get(ind).relatedGraphs.set(0,gsrl );
//				}
//			}
//
//		}
//		return gs;
//	}
//
//>>>>>>> fcad07fe9c04079d9e23deb8ccd5ef06d59dd16b
	private boolean randomInit=false;
	public static int giter=-1;
	private int recsrllevel05=-1, goldsrllevel05=-1;
	private static List<DetGraph> alltrees;
	//
	//
	// main method
	//
	//
	void sampleAll(List<DetGraph> gs,List<DetGraph> golds,List<DetGraph> goldsSRL) {
		alltrees=gs;
		hiCurrentSamplingPerGraph=new ArrayList<Double>();
		// used to average the performances amongst the last (10) iterations
		final int navgperf = 10;
		float[][] avgperf = new float[navgperf][2];
		int perfi=0;
		if (doSRL) ClassificationResultSRL.loadRoles();

		// computes the POSTAGS and FORMS to keep and the back-offing scheme
		voc.init(gs);
		// computes the GN chunks + immediate deps
		rules.init(gs);
		//gold initialization: 
		if (goldInit)gs=initializeGraphGold(gs, golds, goldsSRL);

		if (m!=null) m.repaint();
		// initialize the counts
		for (gidx=0;gidx<gs.size();gidx++) {
			DetGraph g=gs.get(gidx);
			// random init
			//			sample(g, gidx);
			// graph2word2ruleApplied DOIT etre initialise dans rules.init() !!
			//			graph2word2ruleApplied.put(g,new ArrayList<int[]>());
			if (doTraining) {
				if (doSRL) {
					if (versionV01) critsemV01.increaseCounts(g,graph2word2ruleApplied.get(g));
					else critsem.increaseCounts(g,graph2word2ruleApplied.get(g));
				} else crit.increaseCounts(g, graph2word2ruleApplied.get(g));
			}
		}

		//print logpost at initialization
		// TODO: pourquoi ce logpost a l'init est bcp plus eleve que lors des 1eres iterations ??
		double logpost;
		double[]resLogpost=null;
		if (doSRL) {
			//                    if (doSRL) logpost=versionV01? critsemV01.getTotalLogPost(gs): critsem.getTotalLogPost(gs,hiCurrentSamplingPerGraph); // todo semantic part!
			if (versionV01) logpost=critsemV01.getTotalLogPost(gs);
			else{
				resLogpost=critsem.getTotalLogPost(gs,hiCurrentSamplingPerGraph);
				logpost=resLogpost[0];
			}
		}
		else logpost=crit.getTotalLogPost(gs);
		if (golds!=null) {
			List<DetGraph> recs = gs.subList(testFrom, gs.size());
			if (doSRL&&goldsSRL!=null){
                            if (RulesCfgfile.ENGLISH==false){
                                Double[] ressrl=ClassificationResultSRL.calcErrorsFrenchSRL(recs, goldsSRL, true);
                            }else{
				float[] accSRL= ClassificationResultSRL.calcErrors(recs, goldsSRL,true);
				System.out.println("SRL: ");
				for (int ii=0;ii<accSRL.length;ii++){
					String label=(ii==0)?"Identification task, core arguments":(ii==1)?"Identification task, coarse arguments":(ii==2)?"joint (id+clas) task, core arguments":"joint (id+clas) task, coarse arguments";
					System.out.println(label+" F1="+accSRL[ii]);
				}
				System.out.println("");
                            }
			}

			float[] acc = ClassificationResult.calcErrors(recs, golds);
			if (resLogpost!=null)   System.out.println("iter -1 avg-logpost "+logpost+" logpostSynt "+resLogpost[1]+" logpostSem "+resLogpost[2]+" LAS "+acc[0]+" UAS "+acc[1]+'\n');
			else   System.out.println("iter -1 avg-logpost "+logpost+" LAS "+acc[0]+" UAS "+acc[1]+'\n');
			for (int i=0;i<Dep.depnoms.length;i++) {
				if (acc[i*2+3]>0)
					System.out.println("perdep: \t"+Dep.depnoms[i]+"=\t"+acc[i*2+2]+" \t"+acc[i*2+3]);
			}
			unsupacc.repaint();
			avgperf[perfi][0]=acc[0];
			avgperf[perfi++][1]=acc[1];
			float mlas=0, muas=0;
			for (int i=0;i<navgperf;i++) {mlas+=avgperf[i][0]; muas+=avgperf[i][1];}
			mlas/=(float)navgperf; muas/=(float)navgperf;
			System.out.println("averaged LAS "+mlas+" "+muas);
			if (perfi>=avgperf.length) perfi=0;
			if (uf!=null) uf.repaint();
		}
		//JSafran.viewGraph(gs);

		// ************************** End of initialization **************************

		if (debug>=4) {
			System.out.println("graph init ok "+gs.size());
			JSafran.viewGraph(gs.get(0));
		}
		int niters = Integer.MAX_VALUE;
		//		if (oracle) niters=1;
		//		else niters=200; // pour le dev
		double bestlogpost = -Double.MAX_VALUE;
		for (int iter=0;iter<niters;iter++) {
			System.out.println("Iteration: "+ iter+"/"+niters);
			ambiguity.clear();
			giter=iter;
			//			if (iter==0) randomInit=true;
			//			else randomInit=false;
			// je ne dois quitter la boucle que quand tout le corpus est passe, sinon il y a incoherence dans les structures sauvees
			if (stopit) break;
			for (gidx=0;gidx<gs.size();gidx++) {
				//                            if (gidx==979) continue;
				unsuplab.setText("unsupervised parsing running... "+iter+" "+gidx+"/"+gs.size());
				System.out.print("\r unsupervised parsing running... "+iter+" "+gidx+"/"+gs.size());
				System.out.flush();
				unsuplab.repaint();
				if (debug>=4) System.out.println("graph "+gidx);
				DetGraph g = gs.get(gidx);
				// all the sampling for one sentence is done in this method
				if (golds!=null&&gidx>=testFrom) goldG = golds.get(gidx-testFrom);
				else goldG=null;
				if (goldsSRL!=null&&gidx>=testFrom) goldGSRL = goldsSRL.get(gidx-testFrom);
				else goldGSRL=null;
				//old stuff??
				//                                sample(g,gidx);
				//				double probApres = crit.getLogPost(g,graph2word2ruleApplied.get(g));
				//				logpost+=probApres;
				if (false) drawBarChart=true;
				sample(g,gidx);
			}

			// just compute logpost, with the current counts
			//                        if (doSRL) logposti=versionV01? critsemV01.getTotalLogPost(gs): critsem.getTotalLogPost(gs,hiCurrentSamplingPerGraph); // todo semantic part!
			double logposti;
			double[]resLogposti=null;
			if (doSRL) {
				//                    if (doSRL) logpost=versionV01? critsemV01.getTotalLogPost(gs): critsem.getTotalLogPost(gs,hiCurrentSamplingPerGraph); // todo semantic part!
				if (versionV01) logposti=critsemV01.getTotalLogPost(gs);
				else{
					resLogposti=critsem.getTotalLogPost(gs,hiCurrentSamplingPerGraph);
					logposti=resLogposti[0];
				}
			}
			else logposti=crit.getTotalLogPost(gs);

			if (doTraining&&logposti>bestlogpost) {
                              if (!doSRL) crit.saveCounts(countsfilename, logposti);
			}

			float coverage  = printCoverage(gs);
			float ambiguity = printAmbiguity(gs);

			// now computes the accuracy for the current iteration, and the average...
			if (golds!=null) {
				List<DetGraph> recs = gs.subList(testFrom, gs.size());
				if (doSRL&&goldsSRL!=null){
                                    if (RulesCfgfile.ENGLISH==false){
                                        Double[] ressrl=ClassificationResultSRL.calcErrorsFrenchSRL(recs, goldsSRL, true);
                                    }else{
					float[] accSRL= ClassificationResultSRL.calcErrors(recs, goldsSRL,true);
					System.out.println("SRL: ");
					for (int ii=0;ii<accSRL.length;ii++){
						String label=(ii==0)?"Identification task, core arguments":(ii==1)?"Identification task, coarse arguments":(ii==2)?"joint (id+clas) task, core arguments":"joint (id+clas) task, coarse arguments";
						System.out.println(label+" F1="+accSRL[ii]);
					}
					System.out.println("");
                                    }
				}

				float[] acc = ClassificationResult.calcErrors(recs, golds);
				//				System.out.println("iter "+iter+" avg-logpost "+logposti+" LAS "+acc[0]+" UAS "+acc[1]+'\n');
				if (resLogposti!=null)   System.out.println("iter "+iter+" avg-logpost "+logposti+" logpostSynt "+resLogposti[1]+" logpostSem "+resLogposti[2]+" LAS "+acc[0]+" UAS "+acc[1]+'\n');
				else   System.out.println("iter "+iter+" avg-logpost "+logposti+" LAS "+acc[0]+" UAS "+acc[1]+'\n');

				for (int i=0;i<Dep.depnoms.length;i++) {
					if (acc[i*2+3]>0)
						System.out.println("perdep: \t"+Dep.depnoms[i]+"=\t"+acc[i*2+2]+" \t"+acc[i*2+3]);
				}

				unsupacc.setText("iter="+iter+" LAS="+acc[0]+" UAS="+acc[1]+" coverage="+coverage+" ambiguity="+ambiguity);
				unsupacc.repaint();

				avgperf[perfi][0]=acc[0];
				avgperf[perfi++][1]=acc[1];
				float mlas=0, muas=0;
				for (int i=0;i<navgperf;i++) {mlas+=avgperf[i][0]; muas+=avgperf[i][1];}
				mlas/=(float)navgperf; muas/=(float)navgperf;
				System.out.println("averaged LAS "+mlas+" "+muas);
				if (perfi>=avgperf.length) perfi=0;
				if (uf!=null) uf.repaint();
			} else {
				System.out.println("iter "+iter+" logpost "+logposti);
			}
			if (doSRL && isConll05) {
				List<DetGraph> recs = gs.subList(testFrom, gs.size());
				float fmes=-1;
				if (recsrllevel05<0) {
					goldsrllevel05 = m.addLevel();
					recsrllevel05 = m.addLevel();
					fmes=ClassificationResultSRL.evalconll05(recs,m.levels.get(goldsrllevel05),m.levels.get(recsrllevel05),-1,null);
				} else {
					fmes=ClassificationResultSRL.evalconll05(recs,m.levels.get(goldsrllevel05),null,-1,null);
				}
				m.repaint();
			}

			if (!doSRL)crit.debugCounts();

			if (iter==8) {
				GraphIO gio = new GraphIO(null);
				gio.save(gs, "iter8.xml");
			}
			if (iter==150) {
				GraphIO gio = new GraphIO(null);
				gio.save(gs, "iter150.xml");
			}
			if (iter>1) oracle=false; // only init+1iter with oracle, next iterations continue as normal !
		}
		// save infered SRL graphs
		if (doSRL) ClassificationResultSRL.saveSRLgraphs(gs);
		if (oracle) {
			GraphIO gio = new GraphIO(null);
			gio.save(gs, "orgraphs.xml");
		}
	}

	private float printAmbiguity(List<DetGraph> gs) {
		int nsols=0;
		for (int ns: ambiguity) nsols+=ns;
		float a = (float)nsols/(float)ambiguity.size();
		System.out.println("ambiguity "+nsols+" "+ambiguity.size()+" "+a);
		return a;
	}
	private float printCoverage(List<DetGraph> gs) {
		int nw=0, nrts=0;
		for (DetGraph g : gs) {
			nw+=g.getNbMots();
			// -1 car on autorise un ROOT par phrase
			nrts+=g.getNbMots()-g.deps.size()-1;
		}
		float c = (float)(nw-nrts)/(float)nw;
		System.out.println("coverage = "+nrts+" "+nw+" "+c);
		return c;
	}

	int sample_Mult(double[] th) {
		double s=0;
		for (int i=0;i<th.length;i++) s+=th[i];
		s *= rand.nextDouble();
		for (int i=0;i<th.length;i++) {
			s-=th[i];
			if (s<0) return i;
		}
		return 0;
	}

	static double addLog(double x, double y) {
		if (x==-Double.MAX_VALUE) { return y; }
		if (y==-Double.MAX_VALUE) { return x; }

		if (x-y > 16) { return x; }
		else if (x > y) { return x + Math.log(1 + Math.exp(y-x)); }
		else if (y-x > 16) { return y; }
		else { return y + Math.log(1 + Math.exp(x-y)); }
	}
	public static void normalizeLog(double[] x) {
		double s;
		int i;
		s = -Double.MAX_VALUE;
		for (i=0; i<x.length; i++) s = addLog(s, x[i]);
		for (i=0; i<x.length; i++) x[i] = Math.exp(x[i] - s);
	}


	int debug=0;
	// this list stores all the rules that have been applied onto all sentences at the previous iteration.
	// it is needed to update the counts for R
	// it must be updated every time a rule is applied 
	// int[] = {rule idx, position}
	// il est static pour pouvoir y acceder depuis RulesCfg sans avoir a modifier les signatures dans l'interface Rules
	public static HashMap<DetGraph, List<int[]>> graph2word2ruleApplied = new HashMap<DetGraph, List<int[]>>();

	private int getNumArgss(DetGraph g, boolean semantic){
		int num=0;
		DetGraph gaux=(semantic)? g.relatedGraphs.get(0):g;
		for (int i=0;i<gaux.getNbMots();i++){
			if (semantic){
				boolean isPred=SemanticProcesing.isPredicate(gaux, i);
				if (isPred){
					List<Integer> childre= gaux.getFils(i);
					num+=childre.size();
				}
			}else{
				List<Integer> childre= gaux.getFils(i);
				num+=childre.size();
			}

		}
		return num;
	}
	private int[]getLowHi(int[] values){
		int[] res=new int[values.length];
		int hi=0;
		for (int ind=0;ind<values.length;ind++){
			if(values[ind]> hi)hi=values[ind];
		}
		for (int ind=0;ind<values.length;ind++){
			res[ind]=hi-values[ind];
		}
		return res;
	}

	void sample(DetGraph g, int gidx) {
		// on commence par "vider" toutes les regles de la phrase courante
		List<int[]> rulesAppliedForCurrentGraph = graph2word2ruleApplied.get(g);
		if (rulesAppliedForCurrentGraph==null) {
			rulesAppliedForCurrentGraph=new ArrayList<int[]>();
			graph2word2ruleApplied.put(g, rulesAppliedForCurrentGraph);
		}
		if (doSRL){ if (versionV01)critsemV01.resetCache(); else critsem.resetCache();}
		else crit.resetCache();

		if ((!randomInit) && doTraining) {
			if (doSRL){if (versionV01)critsemV01.decreaseCounts(g,rulesAppliedForCurrentGraph); 
			else critsem.decreaseCounts(g,rulesAppliedForCurrentGraph);}
			else crit.decreaseCounts(g,rulesAppliedForCurrentGraph);
		}

		rulesAppliedForCurrentGraph.clear();
		// idem: reset des deps
		for (int i=g.deps.size()-1;i>=0;i--) {
			if (Dep.depnoms[g.deps.get(i).type].charAt(0)!='_') g.deps.remove(i);
		}
		if (doSRL){
			// idem: reset des deps
			DetGraph gsrl=g.relatedGraphs.get(0);
			//                    gsrl.deps.clear();
			for (int i=gsrl.deps.size()-1;i>=0;i--) {
				if (Dep.depnoms[gsrl.deps.get(i).type].charAt(0)!='_') gsrl.deps.remove(i);
			}
		}

		// contains (1) the topological score + (2) the rules seq. applied
		final ArrayList<int[]> seqrul = new ArrayList<int[]>();
		final ArrayList<Double> geneScore = new ArrayList<Double>();
		final ArrayList<Integer> numSyntDeps = new ArrayList<Integer>();
		final ArrayList<Integer> numSemDeps = new ArrayList<Integer>();
		final ArrayList<Integer> topoScore = new ArrayList<Integer>();
		final ArrayList<DetGraph> generatedGraphs = new ArrayList<DetGraph>();

		// build the lattice of "best" rules sequences regarding topological score only
		final DepthFirst depthFirst = new DepthFirst();
		depthFirst.addPathScorer(new DepthFirst.PathScorer() {
			@Override
			public int getScore(DetGraph g, List<Integer> rulesApplied) {
				//				System.out.println("found ruleseq "+rulesApplied);

				// rulesApplied contient une rule par mot de la phrase, -1 lorsqu'il n'y a pas de rule pour ce mot
				// chaque "rule" = (hand-crafted rule, position)
				// une "rule" peut donc etre dupliquee dans la liste

				// 1- compute the generative score and saves it along with (rules seq, topol. score)
				// 2- compute the topological score only and returns it; it'll be used by depthFirst for pruning
				// sizearr
				assert rulesApplied.size()==g.getNbMots();
				int[] seq = new int[rulesApplied.size()];
				for (int i=0;i<rulesApplied.size();i++) seq[i]=rulesApplied.get(i);
				seqrul.add(seq);

				// compute gene score
				ArrayList<int[]> rs = new ArrayList<int[]>();
				for (int i=0;i<rulesApplied.size();i++) {
					int rr = rulesApplied.get(i);
					if (rr>=0) {
						/*
						 * rs n'est utilisee que pour calculer le term1 = rule prior. Le term2 depend du tree.
						 * Mais pour calculer le term1, je "duplique" le prior de la rule pour chaque mot qu'elle modifie,
						 * afin d'avoir une proba comparable entre sequences.
						 * Pour les mots sans aucune regle, on n'ajoute rien, ce qui reste le cas pour toutes les sequences
						 */
						int[] r = {depthFirst.getRuleID(rr)}; // la position est inutile
						rs.add(r);
					}

				}
				if (doSRL) {if (versionV01) critsemV01.resetCache();
				else critsem.resetCache();}
				else crit.resetCache();
				double genesc;
				if (doSRL) genesc= (versionV01)?critsemV01.getLogPost(g, rs):critsem.getLogPost(g, rs);
				else genesc=crit.getLogPost(g, rs);
				g.conf=(float)genesc;
				geneScore.add(genesc);
				int nsyn=getNumArgss(g, false);
				numSyntDeps.add(nsyn);
				if (doSRL) {
					int nsem=getNumArgss(g, true);
					numSemDeps.add(nsem);
				}
				for (int i=0;i<rulesApplied.size();i++) {
					int r=rulesApplied.get(i);
					if (r>=0)
						genesc += Math.log(rules.getRulePrior(depthFirst.getRuleID(r)));
				}

				// compute topo score
				// this topological score is the full score: it includes ncross + nroots + cycles
				int topoSc = TopologyLattice.getTopoScore(g);
				topoScore.add(topoSc);
				if (oracle) generatedGraphs.add(g.getSubGraph(0));

				g.comment="score topo="+topoSc+" gene="+genesc;
				//				JSafran.viewGraph(g);

				return topoSc;
			}
			@Override
			public int getPruningScore(DetGraph g, int i) {
				int ncrossSc = TopologyLattice.getPruningScore(g,i);
				return ncrossSc;
			}
		});
		depthFirst.setGraph(rules,g);
		int bestTopoScore = depthFirst.depthFirst();
		//		System.out.println("nrulesseq "+gidx+" "+seqrul.size());

		if (seqrul.size()==0 ) {
			// no rule can be applied
			if (doSRL){
				if (versionV01) {
					critsemV01.resetCache();
					if (doTraining) critsemV01.increaseCounts(g,rulesAppliedForCurrentGraph);
				}
				else{
					critsem.resetCache();
					if (doTraining) critsem.increaseCounts(g,rulesAppliedForCurrentGraph);
				}
			}else {
				crit.resetCache();
				if (doTraining) crit.increaseCounts(g,rulesAppliedForCurrentGraph);
			}
			return;
		}

		// Now, all the rules seq. have been evaluated
		// 1- remove all rules seq. but the ones with the best topol. score
		for (int i=seqrul.size()-1;i>=0;i--) {
			if (topoScore.get(i)>bestTopoScore) {
				topoScore.remove(i);
				seqrul.remove(i);
				geneScore.remove(i);
				if (doSRL)
					numSemDeps.remove(i);
				numSyntDeps.remove(i);
				if (oracle) generatedGraphs.remove(i);
			}
		}

		ambiguity.add(seqrul.size());
		//                System.out.println("ambiguity:"+ambiguity);
		//                System.out.println("seqrul: "+seqrul);
		// 2- sample one of them according to their generative score
		double[] post = new double[seqrul.size()];

		int sizearr=numSyntDeps.size();
		int[]sum=new int[sizearr];
		for (int i=0;i<sum.length;i++){
			if (doSRL)
				sum[i]=numSyntDeps.get(i)+numSemDeps.get(i);
			else
				sum[i]=numSyntDeps.get(i);
		}
		int[]lowhi=getLowHi(sum);
		for (int i=0;i<post.length;i++) {
			//post[i] = geneScore.get(i);
			post[i] = geneScore.get(i) + (double)(lowhi[i])* penalization;

		}

		if (randomInit) {
			//                    System.out.println("Nb. of rules: "+ rules.getNrules()+" "+seqrul.size());
			Arrays.fill(post, 1);
		}
		normalizeLog(post);
		int sampledZ = sample_Mult(post);

		hiCurrentSamplingPerGraph.add((double)lowhi[sampledZ]* penalization);

		if (drawBarChart) SimpleBarChart.drawChart(post);

		final int orchoice=3;
		if (oracle && goldG!=null) {
			if (orchoice==3 && goldGSRL!=null) {
				// semantic oracle based on conll08 scoring !
				ArrayList<DetGraph> golds = new ArrayList<DetGraph>();
				golds.add(goldGSRL);
				int besti=-1; float bestfm=-Float.MAX_VALUE;
				for (int i=0;i<post.length;i++) {
					ArrayList<DetGraph> recs = new ArrayList<DetGraph>();
					recs.add(generatedGraphs.get(i));
					//float[] fms=  float[4]; //fmeasure for identification (core and coarse) and clasification (core and coarse)
					float[] fms=ClassificationResultSRL.calcErrors(recs, golds,false);
					float fm=fms[3]; //clasification for coarse... 
					if (besti<0||fm>bestfm) {
						bestfm=fm;
						besti=i;
					}
				}
				sampledZ=besti;
			}else 	{
				if (orchoice==2) {
					// semantic oracle based on conll05 scoring !
					int besti=-1; float bestfm=-Float.MAX_VALUE;
					for (int i=0;i<post.length;i++) {
						ArrayList<DetGraph> recs = new ArrayList<DetGraph>();
						recs.add(generatedGraphs.get(i));
						float fm = ClassificationResultSRL.evalconll05(recs, null, null, gidx, alltrees);
						if (besti<0||fm>bestfm) {
							bestfm=fm;
							besti=i;
						}
					}
					sampledZ=besti;
				} else {

					// syntactic oracle
					ArrayList<DetGraph> golds = new ArrayList<DetGraph>();
					golds.add(goldG);
					int besti=-1; float bestlas=-Float.MAX_VALUE;
					for (int i=0;i<post.length;i++) {
						ArrayList<DetGraph> recs = new ArrayList<DetGraph>();
						recs.add(generatedGraphs.get(i));

						float[] acc = ClassificationResult.calcErrors(recs, golds);
						if (besti<0||acc[orchoice]>bestlas) {
							bestlas=acc[orchoice];
							besti=i;
						}
					}
					sampledZ=besti;

				}}
			if (false&&giter>0) {
				// DEBUG
				// the counts have been computed on the oracle trees
				int wouldbechosen = 0;
				for (int i=0;i<post.length;i++) {
					if (post[i]>post[wouldbechosen]) wouldbechosen=i;
				}
				if (post[wouldbechosen]>2.*post[sampledZ]) {
					// it's a very bad instance

					DetGraph gor = g.clone();
					int[] sampledRules = seqrul.get(sampledZ);
					for (int w=0;w<gor.getNbMots();w++) {
						if (sampledRules[w]>=0) {
							depthFirst.applyRule(gor, sampledRules[w], w);
						}
					}
					{
						// compute all terms in the score

						if (versionV01){RuleCrit3SemV01.debugOn=true;
						critsemV01.resetCache();
						}
						else {
							RuleCrit3Sem.debugOn=true;
							critsem.resetCache();
						}
						ArrayList<int[]> rs = new ArrayList<int[]>();
						for (int i=0;i<sampledRules.length;i++) {
							int rr = sampledRules[i];
							if (rr>=0) {
								int[] r = {depthFirst.getRuleID(rr)}; // la position est inutile
								rs.add(r);
							}
						}
						System.out.println("ORACLE SCORES");
						if (versionV01) {
							critsemV01.getLogPost(gor, rs);
							RuleCrit3SemV01.debugOn=false;
						}
						else {
							critsem.getLogPost(gor, rs);
							RuleCrit3Sem.debugOn=false;
						}
					}

					DetGraph gwould = g.clone();
					sampledRules = seqrul.get(wouldbechosen);
					for (int w=0;w<gwould.getNbMots();w++) {
						if (sampledRules[w]>=0) {
							depthFirst.applyRule(gwould, sampledRules[w], w);
						}
					}
					{
						// compute all terms in the score
						if (versionV01){
							RuleCrit3SemV01.debugOn=true;
							critsemV01.resetCache();
						}
						else {
							RuleCrit3Sem.debugOn=true;
							critsem.resetCache();
						}
						ArrayList<int[]> rs = new ArrayList<int[]>();
						for (int i=0;i<sampledRules.length;i++) {
							int rr = sampledRules[i];
							if (rr>=0) {
								int[] r = {depthFirst.getRuleID(rr)}; // la position est inutile
								rs.add(r);
							}
						}
						System.out.println("BEST SEQ SCORES");
						if (versionV01) {
							RuleCrit3SemV01.debugOn=false;
							critsemV01.getLogPost(gor, rs);
						}
						else {
							RuleCrit3Sem.debugOn=false;
							critsem.getLogPost(gor, rs);
						}
					}

					DetGraph[] gs = {gor,gor.relatedGraphs.get(0),gwould,gwould.relatedGraphs.get(0)};
					System.out.println("orprob="+post[sampledZ]+" bestseqprob="+post[wouldbechosen]);
					JSafran.viewGraph(gs);
				}
			}
		}

		// apply the chosen rules
		int[] sampledRules = seqrul.get(sampledZ);
		//                System.out.println("\nseqrul for :"+g);
		//                for (int[]sq:seqrul) {for (int s:sq) System.out.print("+"+s); System.out.println("\notro");
		//                }
		//                System.out.println("sampled:");
		//                for(int s:sampledRules) System.out.print("s:"+s);
		//                System.out.println("fin sampled");
		for (int w=0;w<g.getNbMots();w++) {
			if (sampledRules[w]>=0) {
				depthFirst.applyRule(g, sampledRules[w], w);
			}
		}

		// add them to the list of applied rules
		for (int w=0;w<g.getNbMots();w++) {
			if (sampledRules[w]>=0) {
				int[] rulpos = {depthFirst.getRuleID(sampledRules[w]),depthFirst.getRulePos(sampledRules[w])};
				rulesAppliedForCurrentGraph.add(rulpos);
			}
		}

		rules.applyDetRules(g);
		if (m!=null) m.repaint();
		//JSafran.viewGraph(g);
		//JSafran.viewGraph(g.relatedGraphs.get(0));

		if (doSRL){
			if (versionV01){
				critsemV01.resetCache();
				if (doTraining) critsemV01.increaseCounts(g,rulesAppliedForCurrentGraph);
			}else{
				critsem.resetCache();
				if (doTraining) critsem.increaseCounts(g,rulesAppliedForCurrentGraph);
			}
		}else{
			crit.resetCache();
			if (doTraining) crit.increaseCounts(g,rulesAppliedForCurrentGraph);
		}

		// optional: removes the cycles "randomly" in the end !
		if (true) {
			boolean changed=false;
			for (;;) {
				int h=g.checkCycles();
				if (h<0) break;
				int d=g.getDep(h);
				if ((!changed) && doTraining) {
					if (doSRL){
						if (versionV01) critsemV01.decreaseCounts(g,rulesAppliedForCurrentGraph);
						else critsem.decreaseCounts(g,rulesAppliedForCurrentGraph);
					}else{
						crit.decreaseCounts(g,rulesAppliedForCurrentGraph);
					}
					changed=true;
				}
				g.removeDep(d);
			}
			if (changed) {
				if (doSRL){
					if (versionV01){
						critsemV01.resetCache();
						if (doTraining) critsemV01.increaseCounts(g,rulesAppliedForCurrentGraph);
					}else{
						critsem.resetCache();
						if (doTraining) critsem.increaseCounts(g,rulesAppliedForCurrentGraph);
					}
				}else{
					crit.resetCache();
					if (doTraining) crit.increaseCounts(g,rulesAppliedForCurrentGraph);
				}
			}
		}
	}

	public static void main(String args[]) {
		UnsupParsing u = new UnsupParsing(null);
		GraphIO gio = new GraphIO(null);
		int i=0;
		if (args[i].equals("-fr")) {
			RulesCfgfile.ENGLISH=false;
			++i;
		}
		if (args[i].equals("-or")) {
			u.oracle=true;
			++i;
		}

		List<DetGraph> golds = gio.loadAllGraphs(args[i++]);
		List<DetGraph> train = new ArrayList<DetGraph>();
		if (i<args.length) train = gio.loadAllGraphs(args[i++]);

		u.createRules();
		u.testFrom=train.size();
		for (int j=0;j<golds.size();j++)
			train.add(golds.get(j).getSubGraph(0));
		for (DetGraph g : train) g.clearDeps();

		u.sampleAll(train,golds,null);
	}
}
