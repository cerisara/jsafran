package jsafran;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import jsafran.audio.AudioAccess;
import jsafran.constituants.FrenchTreeBank;
import jsafran.constituants.PennTreeBank;
import jsafran.corpus.GitCorpus;
import jsafran.correction.Correction;
import jsafran.correction.EntitesNommees;
import jsafran.dislocations.DepEvaluation;
import jsafran.dislocations.DislocAnnot;
import jsafran.parsing.Malt4Jsafran;
import jsafran.parsing.NodesSeq;
import jsafran.parsing.unsup.DelPonct;
import jsafran.parsing.unsup.UnsupParsing;
import jsafran.parsing.unsup.criteria.DepLen;
import jsafran.ponctuation.UttSegmenter;
import jsafran.searchfilters.QuickSearch;
import jsafran.searchfilters.SortedTreeDist;
import jsafran.searchfilters.SubGraphList;
import jsafran.searchfilters.SubTreeSelection;

import opennlp.tools.postag.POSTagger;

import org.maltparser.core.options.OptionManager;

import utils.FileUtils;
import utils.SuiteDeMots;

public class JSafranGUI extends JPanel {
	JSafran main;
	Srl srl = new Srl();
	static int WINWIDTH = 1200;
	static int WINHEIGHT = 800;
	JScrollPane scrollPane;
	JMenu sentannot;
	public JLabel infos=new JLabel(" ");
	private DislocAnnot dislocannot=null;
	private AudioAccess audioPlayer = new AudioAccess();

	final static String tmpconll = "tmp_srl_graphs.conll";

	public void updateInfos() {
		if (main.curgraph<0||main.curgraph>=main.allgraphs.size()) {
			infos.setText("out of graph");
		} else {
			if (main.allgraphs!=null&&main.curgraph>=0&&main.curgraph<main.allgraphs.size()&&main.allgraphs.get(main.curgraph)!=null)
			infos.setText("mot: "+main.editword+" note: "+main.allgraphs.get(main.curgraph).comment);
		}
		infos.repaint();
	}
	
	private static JSafranGUI safranPanel = null;
	public static JSafranGUI getSafranPanel() {
		// ce n'est pas vraiment un singleton...
		return safranPanel;
	}
	
	public static JSafranGUI createJsafranPanel(JSafran m) {
		safranPanel = new JSafranGUI(m);
		return safranPanel;
	}
	public static JFrame createJsafranWindow(JSafran m) {
		JFrame jf = new JFrame("Dependency trees manager");
//		JFrame jf = new JFrame("Jsafran");
		final JSafranGUI safranPanel = new JSafranGUI(m);
		jf.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				safranPanel.main.quitNoSave();
			}
		});
		jf.setSize(WINWIDTH, WINHEIGHT);
		jf.getContentPane().setLayout(new GridLayout());
		jf.getContentPane().add(safranPanel);
//		safranPanel.setPreferredSize(new Dimension(WINWIDTH, WINHEIGHT));
		jf.setVisible(true);
//		jf.getContentPane().list();
		safranPanel.setupKeys(jf);
		return jf;
	}
	
	private JSafranGUI(JSafran m) {
		super();
		safranPanel=this;
		main=m;
		scrollPane = new JScrollPane(main,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
//		Box b0 = Box.createVerticalBox();
//		b0.add(infos);
//		b0.add(scrollPane);
//		add(b0);
		setLayout(new BorderLayout());
		add(infos,BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		setFocusable(true);
		
		setupMouse();
		setupKeys(this);
	}
	
	public void oldMethodForMaltParser(ActionEvent e) {
		String cmd = e.getActionCommand();
		File fdir = new File(".");
		String wdir = fdir.getAbsolutePath();

		// bugfix: si aucun parser n'est encore cree, il faut en creer un avant pour
		// eviter un segfault ensuite
		if (cmd.equals("Paris7 mods")) {
			if (main.maltparser==null)
				main.restartParser();
			main.mods = "p7mods";
			main.freeParser();
			try {
				OptionManager.instance().overloadOptionValue(0, "config", "url", "file:///"+wdir+"/p7mods.mco");
				OptionManager.instance().generateMaps();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			main.restartParser();
		} else if (cmd.equals("ESTER mods")) {
			if (main.maltparser==null)
				main.restartParser();
			main.mods = "svmmods";
			main.freeParser();
			try {
				OptionManager.instance().overloadOptionValue(0, "config", "url", "file:///"+wdir+"/svmmods.mco");
				OptionManager.instance().generateMaps();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			main.restartParser();
		} else if (cmd.equals("TreeTagger")) {
			POStagger.useOpenNLP=false;
		} else if (cmd.equals("openNLPtagger")) {
			POStagger.useOpenNLP=true;
		} else {
			System.err.println("WARNING: action unknown "+cmd);
		}
	}
	
	GitCorpus gitcorp = new GitCorpus();
	
	public JMenuBar createMenus() {
		
		JMenuBar menus = new JMenuBar();
		
		// **************************************************************
		JMenu menufich = new JMenu("File");
		menus.add(menufich);

		JMenuItem gitpull = new JMenuItem("get From Server");
		menufich.add(gitpull);
		gitpull.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						main.allgraphs=gitcorp.getFromGit();
						main.filename=gitcorp.currentlyLoaded.getAbsolutePath();
						main.repaint();
					}
				}, "pull thread");
				t.start();
			}
		});
		JMenuItem gitpush = new JMenuItem("send To Server");
		menufich.add(gitpush);
		gitpush.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (gitcorp.currentlyLoaded==null) return;
				String nom = gitcorp.currentlyLoaded.getAbsolutePath();
				main.save(nom);
				gitcorp.sendToGit();
			}
		});
		JMenuItem gitraz = new JMenuItem("reset&clear local copy");
		menufich.add(gitraz);
		gitraz.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gitcorp.reset();
			}
		});
		
		menufich.addSeparator();
		
		JMenuItem load = new JMenuItem("load XML");
		menufich.add(load);
		load.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.load();
			}
		});
		
		JMenuItem loadtrs = new JMenuItem("load TRS");
		menufich.add(loadtrs);
		loadtrs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = GraphIO.loadTRS(null);
				main.repaint();
			}
		});
/*
		JMenuItem loadlist = new JMenuItem("load XML list");
		menufich.add(loadlist);
		loadlist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File xmllist = null;
				File wdir = new File(".");
				JFileChooser chooser = new JFileChooser(wdir);
				int returnVal = chooser.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					xmllist = chooser.getSelectedFile();
				}
				if (xmllist!=null) {
					GraphIO gio = new GraphIO(null);
					main.allgraphs = gio.loadList(xmllist.getAbsolutePath());
					main.curgraph=gio.lastchunk;
					main.repaint();
				}
			}
		});
		*/
		JMenuItem loadAddLevel = new JMenuItem("load in new level");
		menufich.add(loadAddLevel);
		loadAddLevel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.changeLevel(true);
				System.err.println("debug "+main.levels.size());
				main.levels.get(main.curlevel).clear();
				GraphIO gio = new GraphIO(main.jf);
				List<DetGraph> gs = gio.loadAllGraphs(null);
				main.levels.get(main.curlevel).addAll(gs);
				repaint();
			}
		});
		
//		JMenuItem loadAlign = new JMenuItem("load XML & align");
//		menufich.add(loadAlign);
//		loadAlign.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.loadAlign();
//			}
//		});
//		JMenuItem loadIntercale = new JMenuItem("load XML & intercale");
//		menufich.add(loadIntercale);
//		loadIntercale.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.loadIntercale(false);
//			}
//		});
//		JMenuItem loadCIntercale = new JMenuItem("load XML & concat/intercale");
//		menufich.add(loadCIntercale);
//		loadCIntercale.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.loadIntercale(true);
//			}
//		});
//		JMenuItem loadPassage = new JMenuItem("load format passage");
//		loadPassage.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.loadPassage();
//			}
//		});
//		menufich.add(loadPassage);
		JMenuItem load06 = new JMenuItem("load graphs CoNLL06...");
		load06.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.loadConll06();
			}
		});
		menufich.add(load06);
		JMenuItem load08 = new JMenuItem("load graphs CoNLL08...");
		load08.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.loadConll08();
			}
		});
		menufich.add(load08);
		JMenuItem load08F = new JMenuItem("load graphs CoNLL08 FrameNet...");
		load08F.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.loadConll08Framenet();
			}
		});
		menufich.add(load08F);
		JMenuItem load09 = new JMenuItem("load graphs CoNLL09...");
		load09.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.loadConll09();
			}
		});
		menufich.add(load09);
		JMenuItem loadstm = new JMenuItem("load graphs STM...");
		loadstm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.loadSTM(null);
			}
		});
		menufich.add(loadstm);
		
		menufich.addSeparator();
		JMenuItem saveas = new JMenuItem("save as...");
		saveas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.save(null);
			}
		});
		menufich.add(saveas);
		JMenuItem savesel = new JMenuItem("save selected graphs");
		menufich.add(savesel);
		savesel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.saveSelection();
			}
		});
		JMenuItem exportconll = new JMenuItem("save graphs CoNLL06");
		menufich.add(exportconll);
		exportconll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.exportConll();
			}
		});
		JMenuItem save08 = new JMenuItem("save graphs CoNLL08...");
		save08.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GraphIO.saveConLL08(Srl.graphNormal,Srl.graphSrl,null);
			}
		});
		menufich.add(save08);
		JMenuItem save09 = new JMenuItem("save graphs CoNLL09...");
		save09.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Srl.graphNormal.size()==0) Srl.graphNormal = main.allgraphs;
				GraphIO.saveConLL09(Srl.graphNormal,Srl.graphSrl,null);
			}
		});
		menufich.add(save09);
		JMenuItem save05 = new JMenuItem("save props CoNLL05 (only SRL)...");
		save05.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GraphIO.saveConll05SRL(Srl.graphNormal,Srl.graphSrl,null,false);
			}
		});
		menufich.add(save05);
		JMenuItem load05 = new JMenuItem("load props CoNLL05 (only SRL) in new level");
		load05.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<DetGraph> gs = GraphIO.loadConll05SRL(main.allgraphs,null);
				main.levels.add(gs);
				main.curlevel=main.levels.size()-1;
				main.repaint();
			}
		});
		menufich.add(load05);
		
		JMenuItem exporttxt = new JMenuItem("export txt");
		menufich.add(exporttxt);
		exporttxt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.exportTxt();
			}
		});
		
		menufich.addSeparator();
//		JMenuItem find = new JMenuItem("find dependency");
//		find.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.find();
//			}
//		});
//		menufich.add(find);
//		JMenuItem findw = new JMenuItem("find word");
//		findw.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.findWord();
//			}
//		});
//		menufich.add(findw);
/*
		JMenuItem splitCorp = new JMenuItem("split corpus in N chunks");
		menufich.add(splitCorp);
		splitCorp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.splitCorpus();
			}
		});
		JMenuItem splitTrainTest = new JMenuItem("split corpus in Train/Test randomly");
		menufich.add(splitTrainTest);
		splitTrainTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.randomSplit();
			}
		});
		menufich.addSeparator();
*/
		JMenuItem quit = new JMenuItem("quit");
		menufich.add(quit);
		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.quitNoSave();
			}
		});
		menufich.addSeparator();
		JMenuItem audio = new JMenuItem("listen to Audio [k]");
		menufich.add(audio);
		audio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				audioPlayer.listen(main.allgraphs, main.curgraph, main.editword);
			}
		});
		JMenuItem jtrans = new JMenuItem("launch JTrans [k]");
		menufich.add(jtrans);
		jtrans.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JSafran2Jtrans.main=main;
				JSafran2Jtrans.listenWithJtrans();
			}
		});

		// **************************************************************
		JMenu menuPS = new JMenu("Constituants");
		menus.add(menuPS);
		JMenuItem loadftb = new JMenuItem("load graphs FrenchTreeBank...");
		loadftb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FrenchTreeBank ftb = new FrenchTreeBank(main);
				ftb.loadOverride();
			}
		});
		menuPS.add(loadftb);
		JMenuItem saveptb = new JMenuItem("save graphs PennTreeBank...");
		saveptb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PennTreeBank ptb = new PennTreeBank(main);
				ptb.saveCurLevel();
			}
		});
		menuPS.add(saveptb);
		JMenuItem split = new JMenuItem("split compound words");
		split.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FrenchTreeBank.splitCompoundWords(main.allgraphs);
			}
		});
		menuPS.add(split);
		
		// **************************************************************
		JMenu menuedit = new JMenu("Edit");
		menus.add(menuedit);

		JMenuItem insertUtt = new JMenuItem("insert new empty sentence");
		menuedit.add(insertUtt);
		insertUtt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs.add(main.curgraph, new DetGraph());
				main.repaint();
			}
		});
		JMenuItem removeGraph = new JMenuItem("remove graph");
		menuedit.add(removeGraph);
		removeGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearGraph();
			}
		});
		JMenuItem removeAllGraph = new JMenuItem("remove all graphs");
		menuedit.add(removeAllGraph);
		removeAllGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearAllGraph();
			}
		});
		JMenuItem removeEmtpyGraph = new JMenuItem("remove empty graphs");
		menuedit.add(removeEmtpyGraph);
		removeEmtpyGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearEmptyGraph();
			}
		});
		JMenuItem removePosIdxGraph = new JMenuItem("del graphs w/ indexes>0");
		menuedit.add(removePosIdxGraph);
		removePosIdxGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearPosIdxGraph();
			}
		});
		
		JMenuItem cleardep = new JMenuItem("clear dependencies");
		menuedit.add(cleardep);
		cleardep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearDeps();
			}
		});
		JMenuItem clearall = new JMenuItem("clear all deps");
		menuedit.add(clearall);
		clearall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearAll();
			}
		});
		
		menuedit.addSeparator();
		
		JMenuItem subtree = new JMenuItem("get subtree");
		menuedit.add(subtree);
		subtree.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DetGraph g = SubTree.getSubTree(main.allgraphs.get(main.curgraph), main.editword, 10);
				main.allgraphs.add(main.curgraph+1, g);
				main.repaint();
			}
		});
		JMenuItem includingsubtrees = new JMenuItem("save including subtrees");
		menuedit.add(includingsubtrees);
		includingsubtrees.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DetGraph g = SubTree.getSubTree(main.allgraphs.get(main.curgraph), main.editword, 10);
				SubTree.saveIncludingSubtrees(g,main.allgraphs,"subtrees.xml");
			}
		});
		JMenuItem allsubtrees = new JMenuItem("save all subtrees");
		menuedit.add(allsubtrees);
		allsubtrees.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SubTree.saveAllSubtrees(main.allgraphs, 10, "allsubtrees.xml");
			}
		});

		menuedit.addSeparator();

		JMenuItem tolatex = new JMenuItem("tolatex");
		menuedit.add(tolatex);
		tolatex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.printToLatex();
			}
		});
		JMenuItem delNextWords = new JMenuItem("del following words");
		menuedit.add(delNextWords);
		delNextWords.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.delNextWords();
			}
		});
		JMenuItem delPrevWords = new JMenuItem("del previous words");
		menuedit.add(delPrevWords);
		delPrevWords.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.delPrevWords();
			}
		});

		JMenuItem tokenize = new JMenuItem("retokenize");
		menuedit.add(tokenize);
		tokenize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.retokenize();
//				main.retokenizeMultiMots();
			}
		});

		JMenuItem segmente = new JMenuItem("segmente with ponct");
		menuedit.add(segmente);
		segmente.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UttSegmenter.segmente(main.allgraphs);
				// main.uttsegmente();
			}
		});

		JMenuItem pctdel  = new JMenuItem("del ponct");
		menuedit.add(pctdel);
		pctdel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				main.delponct();
				DelPonct.delPonct(main.allgraphs);
				repaint();
			}
		});

		menuedit.addSeparator();

		JMenu levels = new JMenu("Levels");
		menuedit.add(levels);
		JMenuItem addlevel = new JMenuItem("add level [Shift-l]");
		JMenuItem dellevel = new JMenuItem("del level");
		JMenuItem gotolevel = new JMenuItem("goto next level [l]");
		levels.add(addlevel); levels.add(gotolevel); levels.add(dellevel);
		addlevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.changeLevel(true);
			}
		});
		gotolevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.changeLevel(false);
			}
		});
		dellevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.delLevel();
			}
		});
		JMenuItem projectdeps = new JMenuItem("project deps to lower level");
		levels.add(projectdeps);
		projectdeps.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.project2LowerLevel(true,false,false);
			}
		});
		JMenuItem projectgrps = new JMenuItem("project groups to lower level");
		levels.add(projectgrps);
		projectgrps.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.project2LowerLevel(false,true,false);
			}
		});
		JMenuItem projectpos = new JMenuItem("project postags to lower level");
		levels.add(projectpos);
		projectpos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.project2LowerLevel(false,false,true);
			}
		});
		
		JMenu marks = new JMenu("Marks");
		menuedit.add(marks);
		JMenuItem addmark = new JMenuItem("add mark [m]");
		JMenuItem delmark = new JMenuItem("del all marks [Shift-m]");
		JMenuItem gotomark = new JMenuItem("goto next mark [Ctrl-m]");
		JMenuItem rmutt = new JMenuItem("remove marked utterances");
		marks.add(addmark); marks.add(delmark); marks.add(gotomark);
		marks.add(rmutt);
		addmark.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.addMark();
			}
		});
		delmark.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.clearMarks();
			}
		});
		gotomark.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.gotoNextMark();
			}
		});
		rmutt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.rmMarkedUtt();
			}
		});
		
		JMenu searchmenu = new JMenu("Search");
		menuedit.add(searchmenu);
		JMenuItem search = new JMenuItem("search anything [Ctrl-f or F2]");
		searchmenu.add(search);
		search.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.look4tree();
			}
		});
		JMenuItem searchMultiHead = new JMenuItem("search multiple heads");
		searchmenu.add(searchMultiHead);
		searchMultiHead.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.look4multihead();
			}
		});
		JMenuItem searchword = new JMenuItem("search word [Shift-w]");
		searchmenu.add(searchword);
		searchword.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				QuickSearch.searchWord(main);
			}
		});
		JMenuItem cycles = new JMenuItem("search cycles");
		searchmenu.add(cycles);
		cycles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.cycles();
			}
		});
		JMenuItem unproj = new JMenuItem("search non-proj");
		searchmenu.add(unproj);
		unproj.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.nonproj();
			}
		});

		// **************************************************************
		
		JMenu parsing = new JMenu("Parsing");
		menus.add(parsing);
		
		parsing.add(MateParser.getMenu(main));
		
		JMenu maltparser = new JMenu("Malt parser");
		parsing.add(maltparser);
		JMenuItem trainmalt = new JMenuItem("train");
		maltparser.add(trainmalt);
		trainmalt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Malt4Jsafran m = new Malt4Jsafran();
				m.train("maltmods", main.allgraphs);
			}
		});
		JMenuItem parsemalt = new JMenuItem("parse");
		maltparser.add(parsemalt);
		parsemalt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Malt4Jsafran m = new Malt4Jsafran();
				m.parse("maltmods", main.allgraphs);
				main.repaint();
			}
		});
		
		/*
		parsing.addSeparator();

		{
			ButtonGroup group = new ButtonGroup();
			JRadioButtonMenuItem treetagger = new JRadioButtonMenuItem("TreeTagger");
			treetagger.setSelected(true);
			treetagger.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					POStagger.useOpenNLP=false;
				}
			});
			group.add(treetagger);
			parsing.add(treetagger);
			JRadioButtonMenuItem openNLPtagger = new JRadioButtonMenuItem("openNLPtagger");
			openNLPtagger.setSelected(false);
			openNLPtagger.addActionListener(this);
			group.add(openNLPtagger);
			parsing.add(openNLPtagger);
		}
*/			
		
//		parsing.addSeparator();

		/*
		JMenuItem calcScore2 = new JMenuItem("[p] reparse with Malt");
		parsing.add(calcScore2);
		calcScore2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.maltparse();
			}
		});
		JMenuItem parseall = new JMenuItem("parseall w/ Malt");
		parsing.add(parseall);
		parseall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.parseall();
			}
		});
		*/
	
//		JMenuItem train = new JMenuItem("retrain Malt");
//		parsing.add(train);
//		train.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				main.train();
//			}
//		});
		
		parsing.addSeparator();
		
		JMenuItem trainmemm = new JMenuItem("train MEMM parser");
		parsing.add(trainmemm);
		trainmemm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NodesSeq.train(main.allgraphs, 200);
			}
		});
		JMenuItem parsememm = new JMenuItem("parse with MEMM parser");
		parsing.add(parsememm);
		parsememm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NodesSeq.parse(main.allgraphs);
			}
		});
		parsing.addSeparator();
		
		JMenuItem tagall = new JMenuItem("tag all: Treetagger");
		parsing.add(tagall);
		tagall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.tagall();
			}
		});
		JMenuItem testmatetag = new JMenuItem("tag: MATE");
		parsing.add(testmatetag);
		testmatetag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MateParser.getMateParser(main).testMateTagger();
			}
		});

		/*
		JMenuItem pct = new JMenuItem("ponct train");
		parsing.add(pct);
		pct.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PonctModel mod = new PonctModel("tmppctmod");
				mod.train(main.allgraphs);
			}
		});
		JMenuItem pctinf = new JMenuItem("ponct generate");
		parsing.add(pctinf);
		pctinf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PonctModel mod = new PonctModel("tmppctmod");
				mod.genPonct(main.allgraphs,false);
				repaint();
			}
		});
		JMenuItem pcteval = new JMenuItem("ponct eval");
		parsing.add(pcteval);
		pcteval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PonctModel mod = new PonctModel("tmppctmod");
				mod.genPonct(main.allgraphs,true);
				repaint();
			}
		});
		*/
		
		UnsupParsing uns = new UnsupParsing(main);
		JMenu unsup = uns.getMenu();
		menus.add(unsup);
		
		// **************************************************************
		JMenu groupmenu = new JMenu("Groupes");
		menus.add(groupmenu);
		JMenuItem setgroup = new JMenuItem("add new group [g]");
		groupmenu.add(setgroup);
		setgroup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.defgroup();
			}
		});
		JMenuItem delgroup = new JMenuItem("del new group [BACKSPACE]");
		groupmenu.add(delgroup);
		delgroup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GroupManager.delgroup(main.allgraphs.get(main.curgraph), main.editword);
				repaint();
			}
		});
		JMenuItem groupmngr = new JMenuItem("open group manager [Shift-g]");
		groupmenu.add(groupmngr);
		groupmngr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GroupManager.showGroups(main.allgraphs.get(main.curgraph));
			}
		});
		JMenuItem saveStanford = new JMenuItem("save groups in StanfordNER format");
		groupmenu.add(saveStanford);
		saveStanford.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GroupManager.saveGroups(main.allgraphs);
			}
		});

		// **************************************************************
		sentannot = new JMenu("Sentence_level");

		for (int i=0;i<main.sentenceKeywords.size();i++) {
			JCheckBoxMenuItem ss = new JCheckBoxMenuItem(main.sentenceKeywords.get(i));
			final int j = i;
			ss.addItemListener(new ItemListener() {
				int idx=j;
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (!main.triggerEvents) return;
					int addOrRemove = e.getStateChange();
					if (addOrRemove == ItemEvent.SELECTED)
						main.addSentenceAnnotation(idx);
					else if (addOrRemove == ItemEvent.DESELECTED)
						main.removeSentenceAnnotation(idx);
				}
			});
			sentannot.add(ss);
		}

		sentannot.addSeparator();
		JMenuItem exportAnnot = new JMenuItem("Remove all sents but the ones with these annots");
		sentannot.add(exportAnnot);
		exportAnnot.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				main.filterGraphsByAnnotations();
			}
		});
//		JMenuItem searchInNotes = new JMenuItem("Search in notes");
//		sentannot.add(searchInNotes);
//		searchInNotes.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				main.findNote();
//			}
//		});

		menus.add(sentannot);
		
		// **************************************************************
		JMenu menustats = new JMenu("Stats");
		menus.add(menustats);
		JMenuItem ngraphsParPhrase = new JMenuItem("nb graphs / utt");
		ngraphsParPhrase.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Stats s = new Stats(main);
				s.calcNgraphs(main.allgraphs);
			}
		});
		menustats.add(ngraphsParPhrase);
		JMenuItem ndepsParPOS = new JMenuItem("distrib dep=f(pos)");
		ndepsParPOS.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Stats s = new Stats(main);
				s.calcNdeps(main.allgraphs);
			}
		});
		menustats.add(ndepsParPOS);
		JMenuItem distdep = new JMenuItem("distrib pos=f(dep)");
		distdep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Stats s = new Stats(main);
				s.calcNPOS(main.allgraphs);
			}
		});
		menustats.add(distdep);
		
		// **************************************************************
		JMenu menu = new JMenu("Preferences");
		menus.add(menu);
		JMenuItem prefs = new JMenuItem("font size");
		prefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.getSettings(0);
			}
		});
		menu.add(prefs);
		JMenuItem prefs2 = new JMenuItem("vertical pos");
		prefs2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.getSettings(1);
			}
		});
		menu.add(prefs2);

		// **************************************************************
		JMenu menuSrl = new JMenu("Srl");
		JMenuItem showMain = new JMenuItem("(1) dep-P7");
		menuSrl.add(showMain);
		showMain.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Srl.graphNormal.size()==main.allgraphs.size()) {
					// deja charges
					main.allgraphs=Srl.graphNormal;
					repaint();
					return;
				}
				// je re-sauve les graphes courant en .conll avant de les recharger dans la suite:
				PrintWriter f;
				try {
					f = FileUtils.writeFileUTF("tmp_srl_graphs.conll");
					Syntex2conll conllsaver = new Syntex2conll(f);
					for (DetGraph g : main.allgraphs) {
						conllsaver.processGraph(g);
					}
					f.println();
					f.close();
				} catch (UnsupportedEncodingException e1) {
					JOptionPane.showMessageDialog(null, "encodage UTF8 non supporte !");
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(null, "probleme enregistrement CONLL !");
				}
				
//				main.allgraphs = srl.changeGraph(1, main.allgraphs, main.gio.readFile.getAbsolutePath());
				main.allgraphs = srl.changeGraph(1, main.allgraphs, tmpconll);
				infos.setText("Dependency view");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(1) dep-P7"));
				repaint();
			}
		});
		JMenuItem showNoMatchTreelex = new JMenuItem("(2) dep-notinTL");
		menuSrl.add(showNoMatchTreelex);
		showNoMatchTreelex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(3, main.allgraphs, tmpconll);
				infos.setText("Dependencies that are not in TreeLex");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(2) dep-notinTL"));
				repaint();
			}
		});
		menus.add(menuSrl);
		JMenuItem showMatchTreelex = new JMenuItem("(3) dep-allinTL");
		menuSrl.add(showMatchTreelex);
		showMatchTreelex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(5, main.allgraphs, tmpconll);
				infos.setText("Dependencies that are in TreeLex");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(3) dep-allinTL"));
				repaint();
			}
		});
		JMenuItem showNoMatchXMLSrl = new JMenuItem("(7) dep-nogrid");
		menuSrl.add(showNoMatchXMLSrl);
		showNoMatchXMLSrl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(7, main.allgraphs, tmpconll);
				infos.setText("Dependencies that do not have any thematic grid");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(7) dep-nogrid"));
				repaint();
			}
		});
		menus.add(menuSrl);
		JMenuItem showMatchXML = new JMenuItem("(9) dep-allinPBK");
		menuSrl.add(showMatchXML);
		showMatchXML.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(9, main.allgraphs, tmpconll);
				infos.setText("Dependencies that do have a thematic grid");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(9) dep-allinPBK"));
				repaint();
			}
		});
		menuSrl.addSeparator();
		JMenuItem showSrl = new JMenuItem("(4) res-P7");
		menuSrl.add(showSrl);
		showSrl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Srl.graphSrl.size()==main.allgraphs.size()) {
					// deja charges
					main.allgraphs = Srl.graphSrl;
					repaint();
					return;
				}
				main.allgraphs = srl.changeGraph(2, main.allgraphs, tmpconll);
				infos.setText("SRL");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(4) res-P7"));
				repaint();
			}
		});
		JMenuItem showNoMatchTreelexSrl = new JMenuItem("(5) frame-notinTL");
		menuSrl.add(showNoMatchTreelexSrl);
		showNoMatchTreelexSrl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(4, main.allgraphs, tmpconll);
				infos.setText("SRL that are not in TreeLex");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(5) frame-notinTL"));
				repaint();
			}
		});
		JMenuItem showMatchTreelexSrl = new JMenuItem("(6) TLframe-allinTL");
		menuSrl.add(showMatchTreelexSrl);
		showMatchTreelexSrl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(6, main.allgraphs, tmpconll);
				infos.setText("SRL that are in TreeLex");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(6) TLframe-allinTL"));
				repaint();
			}
		});
		JMenuItem showNoMatchXML = new JMenuItem("(8) res-nogrid");
		menuSrl.add(showNoMatchXML);
		showNoMatchXML.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(8, main.allgraphs, tmpconll);
				infos.setText("SRL that have not thematic grid");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(8) res-nogrid"));
				repaint();
			}
		});
		JMenuItem showMatchXMLSrl = new JMenuItem("(0) srl-allinPBK");
		menuSrl.add(showMatchXMLSrl);
		showMatchXMLSrl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.allgraphs = srl.changeGraph(10, main.allgraphs, tmpconll);
				infos.setText("SRL that have a thematic grid");
//				main.jf.setTitle(srl.changeTitle(getTitle(), "(0) srl-allinPBK"));
				repaint();
			}
		});
		menus.add(menuSrl);
				
		// **************************************************************
/*
		JMenu modes = new JMenu("Modes");
		menus.add(modes);
		JMenuItem disloc = new JMenuItem("Dislocation");
		modes.add(disloc);
		JMenuItem eval = new JMenuItem("Evaluation");
		modes.add(eval);
		JMenuItem correction = new JMenuItem("Correction");
		modes.add(correction);
		disloc.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (dislocannot==null) dislocannot = new DislocAnnot(main);
				else dislocannot=null;
			}
		});
		eval.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DepEvaluation.eval(main);
			}
		});
		correction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Correction.mode(main);
			}
		});
		JMenuItem entitesNommes = new JMenuItem("Named Entity");
		modes.add(entitesNommes);
		entitesNommes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EntitesNommees.mode(main);
			}
		});
*/
		
		// **************************************************************
		JMenu menu2 = new JMenu("Help");
		JMenuItem usage = new JMenuItem("usage");
		menu2.add(usage);
		usage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main.help();
			}
		});
		menus.add(menu2);
		return menus;
	}

//	boolean isShifted = false, isCtrl=false;
	public void setupKeys(final Container c) {
		c.addKeyListener(new KeyAdapter() {
/*
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == 16) isShifted=false;
				else if (e.getKeyCode() == 17) {
					isCtrl=false;
				}
			}
			*/
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10) { // ENTER
					// confirme l'annotation de cette phrase
					main.terminateEdition();
					main.confirmUttOK(2);
					main.repaint();
					if (main.actionListener!=null) main.actionListener.actionPerformed(new ActionEvent(main, 0, "ENTER"));
				} else if (e.getKeyCode() == 8) { // BACKSPACE
					GroupManager.delgroup(main.allgraphs.get(main.curgraph), main.editword);
					repaint();
//				} else if (e.getKeyCode() == 16) { // SHIFT
//					isShifted = true; main.editword0=main.editword;
//				} else if (e.getKeyCode() == 17) { // CTRL
//					isCtrl = true;
				} else if (e.getKeyCode() == 27) { // ESC : annule toute operation en cours
					// finalise aussi les infos de correction apportees par le user
					updateInfos();
					main.stopallproc();
					if (dislocannot!=null) dislocannot.pressESC();
					SubTreeSelection.cancelSelection();
					audioPlayer.stop();
					repaint();
				} else if (e.getKeyCode() == 113) { // F2
					// cherche une structure arborescente
					main.look4tree();
				} else if (e.getKeyCode() == 114) { // F3
					// cherche un groupe de mots
					main.look4group();
				} else if (e.getKeyCode() == 115) { // F4
					// switch entre FORME / POSTAG pour un mot pour chercher une sequence
					main.switchLook4();
				} else if (e.getKeyCode() == 116) { // F5
					// projette l'annotation du groupe
	//				main.projectGroupAnnotation();
				} else if (e.getKeyCode() == 117) { // F6
					// clear les roles SRL
					System.err.println("clear SRL roles...");
					for (DetGraph g : Srl.graphSrl) g.deps.clear();
					System.err.println("done !");
				} else if (e.getKeyCode() == 118) { // F7
					if (e.isControlDown()) QuickSearch.nextsearch(main);
					else QuickSearch.qsearch(main);
				} else if (e.getKeyCode() == 119) { // F8
					if (!e.isControlDown()) {
						System.err.println("search a subtree");
						sgl = SubTreeSelection.getTreeSelecter().findClosest(main.allgraphs);
					}
					int[] r = sgl.next();
					if (r!=null) {
						main.curgraph=r[0]; main.editword=r[1]-1;
						main.gotoword(1);
					}
				} else if (e.getKeyCode() == 120) { // F9
					List<int[]> marks = SortedTreeDist.sortClosestBranches(main.allgraphs,main.curgraph,main.editword);
					main.clearMarks();
					for (int[] m : marks) {
						main.curgraph=m[0]; main.editword=m[1]; main.addMark();
					}
					System.err.println("all marks added ");
					main.rewindMarks(); main.gotoNextMark();
				} else if (e.getKeyCode() == 123) { // F12 (F1=112, puis +1...)
					// confirme niveau 3
					main.terminateEdition();
					main.confirmUttOK(3);
				} else if (e.getKeyChar() == ' ') {
					main.selection();
				} else if (e.getKeyCode() == 65) { // a
					if (e.isControlDown()) {
						main.curgraph=main.allgraphs.size()-1;
						main.seldeb=0;
						repaint();
					} else {
						// add/replace the same previous link
						main.repeatAddLink();
					}
				} else if (e.getKeyCode() == 70) { // f
					if (e.isControlDown()) { // Ctrl-f = find
						// cherche une structure arborescente
						main.look4tree();
					}
				} else if (e.getKeyChar() == 'n') { // add a note to the utterance
					main.addNote();
				} else if (e.getKeyChar() == 'i') { // capture screenshot
					main.screenshot();
				} else if (e.getKeyChar() == 's') { // add word to selection
					SubTreeSelection.getTreeSelecter().addWordAndDep(main.allgraphs.get(main.curgraph),main.editword);
					repaint();
				} else if (e.getKeyCode() == 87) { // W = merge/split/search words
					if (e.isControlDown()) main.splitWord();
					else if (e.isShiftDown()) QuickSearch.searchWord(main);
					else main.mergeWords();
				} else if (e.getKeyChar() == 'x') { // del word
					main.delCurWord();
				} else if (e.getKeyChar() == 'z') {
					main.showpostag = !main.showpostag;
					repaint();
				} else if (e.getKeyChar() == 'Z') {
					main.showdeps = !main.showdeps;
					repaint();
				} else if (e.getKeyChar() == 'L') { // create level
					main.changeLevel(true);
				} else if (e.getKeyChar() == 'l') { // change level
					main.changeLevel(false);
				} else if (e.getKeyChar() == 'k') { // listen to Audio
					audioPlayer.listen(main.allgraphs, main.curgraph, main.editword);
				} else if (e.getKeyCode() == 77) { //  M = MARK
					if (e.isShiftDown()) {
						if (e.isControlDown()) main.gotoPrevMark();
						else main.clearMarks();
					} else if (e.isControlDown()) {
						main.gotoNextMark();
					} else main.addMark();
				} else if (e.getKeyChar() == 'p' || e.getKeyChar()=='P') { // POStags
					main.editPOStag();
				} else if (e.getKeyChar() == 'g' || e.getKeyChar() == 'G') { // definition d'un groupe
					if (e.isShiftDown()) {
						GroupManager.showGroups(main.allgraphs.get(main.curgraph));
					} else main.defgroup();
				} else if (e.getKeyChar() == 'q') { // toggle show group names
					main.showgroupnames=!main.showgroupnames;
					main.repaint();
				} else if (e.getKeyChar() == 'j') { // join 
					// following utterance into the current one
						main.joinSentences();
				} else if (e.getKeyChar() == 'c' && main.editmode) {
					main.decoupPhrase();
					scrollPane.repaint();
				} else if ((e.getKeyChar() == 'd'||e.getKeyChar() == 'D') && main.editmode) { // edition du lien
					if (main.editlink) {
						main.terminateLinkEdition();
						if (Correction.corr!=null) Correction.corr.getNewGraph();
					} else {
						if (e.isShiftDown())
							main.changeHead(true);
						else {
							main.changeHead(false);
							if (Correction.corr!=null) Correction.corr.getPrevGraph();
							if (EntitesNommees.corr!=null) EntitesNommees.corr.saveGraph(main.curgraph);
						}
					}
					scrollPane.repaint();
				} else if (e.getKeyChar() == 'e') { // edit sentence
					if (EntitesNommees.corr!=null) EntitesNommees.corr.doit();
					else editSentence();
				} else if (e.getKeyChar() == 'h') { // go to next less covered word
					DepLen.gotoNextUncovered(main);
				} else if (e.getKeyChar() == 'o') { // insert new empty sentence
					main.allgraphs.add(main.curgraph+1, new DetGraph());
					repaint();
				} else if (e.getKeyChar() == '+') { // insert a word 
					
				} else if (e.getKeyCode() == 39) { // fleche a droite
					if (!e.isShiftDown()) main.editword0=-1;
					if (e.isControlDown()) main.gotoword(0);
					else {
						if (e.isShiftDown() && main.editword0<0) main.editword0=main.editword;
						main.gotoword(1);
					}
				} else if (e.getKeyCode() == 37) { // fleche a gauche
					if (!e.isShiftDown()) main.editword0=-1;
					if (e.isControlDown()) main.gotoword(0);
					else {
						if (e.isShiftDown() && main.editword0<0) {
							main.editword0=main.editword;
						}
						main.gotoword(-1);
					}
				} else if (e.getKeyCode() == 38) { // fleche en haut
					if (main.editlink) {
						if (main.deppanel == null)
							main.createDepPanel();
					} else {
						if (e.isControlDown())  { // Ctrl-fleche pour indiquer le num de la ligne
							main.terminateEdition();
							main.jumpToUtt();
						} else { // fleche tout court pour avancer de un
							main.terminateEdition();
							if (main.curgraph>0) {
								main.curgraph--;
								main.changedSentence();
							}
							main.editword = 0;
							updateInfos();
							repaint();
							checkXscroll(main.xmotedit);
						}
					}
				} else if (e.getKeyCode() == 40) { // fleche en bas
					if (main.editlink) {
						if (main.deppanel == null)
							main.createDepPanel();
					} else {
						if (e.isControlDown())  { // Ctrl-fleche pour indiquer le num de la ligne
							main.terminateEdition();
							main.jumpToUtt();
						} else { // fleche tout court pour avancer de un
							main.terminateEdition();
							if (main.curgraph<main.allgraphs.size()-1) {
								main.curgraph++;
								main.changedSentence();
							}
							main.editword = 0;
							updateInfos();
							repaint();
							checkXscroll(main.xmotedit);
						}
					}
				} else if (e.getKeyChar() == '1') {
					main.allgraphs = srl.changeGraph(1, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(1) dep-P7"));
					repaint();
				} else if (e.getKeyChar() == '2') {
					main.allgraphs = srl.changeGraph(3, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(2) dep-notinTL"));
					repaint();
				} else if (e.getKeyChar() == '3') {
					main.allgraphs = srl.changeGraph(5, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(3) dep-allinTL"));
					repaint();
				} else if (e.getKeyChar() == '4') {
					main.allgraphs = srl.changeGraph(2, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(4) res-P7"));
					repaint();
				} else if (e.getKeyChar() == '5') {
					main.allgraphs = srl.changeGraph(4, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(5) frame-notinTL"));
					repaint();
				} else if (e.getKeyChar() == '6') {
					main.allgraphs = srl.changeGraph(6, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(6) TLframe-allinTL"));
					repaint();
				} else if (e.getKeyChar() == '7') {
					main.allgraphs = srl.changeGraph(7, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(7) dep-nogrid"));
					repaint();
				} else if (e.getKeyChar() == '8') {
					main.allgraphs = srl.changeGraph(8, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(7) dep-nogrid"));
					repaint();
				} else if (e.getKeyChar() == '9') {
					main.allgraphs = srl.changeGraph(9, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(9) dep-allinPBK"));
					repaint();
				} else if (e.getKeyChar() == '0') {
					main.allgraphs = srl.changeGraph(10, main.allgraphs, tmpconll);
//					main.jf.setTitle(srl.changeTitle(getTitle(), "(0) srl-allinPBK"));
					repaint();
				} else {
					// System.err.println("key "+e.getKeyCode());
				}
				c.repaint();
			}
		});
	}
	SubGraphList sgl;
	void setupMouse() {
		main.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				System.err.println("mouse "+e.getX());
				main.selectWordAtX(e.getX());
				if (dislocannot!=null) dislocannot.clicWord();
				updateInfos();
				requestFocusInWindow();
			}
		});
	}
	public int getScrollbarPosition() {
		return scrollPane.getHorizontalScrollBar().getValue();
	}
	void checkXscroll(int xmot) {
		int debscrollbar = xmot - scrollPane.getHorizontalScrollBar().getVisibleAmount() / 2;
		if (debscrollbar < 0)
			debscrollbar = 0;
		scrollPane.getHorizontalScrollBar().setValue(debscrollbar);
		scrollPane.repaint();
	}
	
	void editSentence() {
		if (main.allgraphs.size()==0) {
			DetGraph g = new DetGraph();
			g.cursent=1;
			main.allgraphs.add(g);
			main.curgraph=0;
		}
		String s="";
		DetGraph g = main.allgraphs.get(main.curgraph);
		for (int i=0;i<g.getNbMots();i++) {
			s+=g.getMot(i).getForme()+" ";
		}
		JTextField tf = new JTextField(s,s.length()+10);
		JScrollPane tff = new JScrollPane(tf);
		tff.setPreferredSize(new Dimension((int)((float)this.getWidth()*0.8f), 50));
		tf.requestFocus();
		JOptionPane.showMessageDialog(null, tff, "edit sentence", JOptionPane.QUESTION_MESSAGE);
		s=tf.getText();
		DetGraph newg = SyntaxGraphs.tokenizeSentence(s);
		newg.cursent=g.cursent;
		newg.comment=g.comment;
		SuiteDeMots s0 = new SuiteDeMots(g.getMots());
		SuiteDeMots s1 = new SuiteDeMots(newg.getMots());
		s0.align(s1);
		// recopie les dependances
		for (int i=0;i<g.getNbMots();i++) {
			int[] targets = s0.getLinkedWords(i);
			if (targets.length==0) continue;
			newg.getMot(targets[0]).setPOS(g.getMot(i).getPOS());
			newg.getMot(targets[0]).setlemme(g.getMot(i).getLemme());
			int[] deps = g.getDeps(i);
			for (int j=0;j<deps.length;j++) {
				int[] newheads = s0.getLinkedWords(g.getHead(deps[j]));
				if (newheads.length>0) {
					newg.ajoutDep(g.getDepLabel(deps[j]), targets[0], newheads[0]);
				}
			}
		}
		JSafran.projectOnto(g, newg, false, true, false);
		main.allgraphs.set(main.curgraph,newg);
		main.repaint();
	}
}
