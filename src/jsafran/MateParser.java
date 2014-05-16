package jsafran;

import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import utils.FileUtils;

import is2.parser.Decoder;
import is2.parser.Options;
import is2.parser.ParallelExtract;
import is2.parser.Parameters;
import is2.parser.ParametersFloat;
import is2.parser.Parser;
import is2.parser.Pipe;
import is2.tag3.Tagger;
import is2.util.OptionsSuper;

public class MateParser {
	Decoder decoder = null;
	File ftmp=null, outf=null;
	OptionsSuper options;
	Pipe pipe;
	Parameters params;
	JSafran js;
	private static String modfile = "mate.mods";

	static MateParser singleton=null;
	public static MateParser getMateParser(JSafran js) {
		if (singleton==null) singleton = new MateParser(js);
		return singleton;
	}
	public MateParser(JSafran main) {
		js=main;
	}
	public static void setMods(String modfich) {
		if (modfile.equals(modfich)) return;
		modfile = modfich;
		singleton=null;
	}

	public DetGraph parseMateOneUtt(DetGraph g) {
		// save les graphes en conll09
		try {
			if (decoder==null) {
				ftmp = File.createTempFile("matetmp", ".conll");
				System.out.println("creating tmp file "+ftmp.getAbsolutePath());
				outf = File.createTempFile("mateout", ".conll");
				System.out.println("creating tmp file "+outf.getAbsolutePath());
			}
			
			ArrayList<DetGraph> gs = new ArrayList<DetGraph>(); gs.add(g);
			GraphIO.saveConLL09(gs, null, ftmp.getAbsolutePath());
			if (decoder==null) {
				final String[] args={"-test",ftmp.getAbsolutePath(),"-model",modfile,"-out",outf.getAbsolutePath()};
				options = new Options(args);
				pipe = new Pipe(options);
				params = new ParametersFloat(0);  // total should be zero and the parameters are later read
				System.out.println("loading MATE models... "+modfile);
				decoder = Parser.readAll(options, pipe, params);
			}

			// y a-t-il des deps que l'on doit forcer ?
			boolean shallForceDeps = false;
			HashMap<Integer, Object[]> forceddeps = new HashMap<Integer, Object[]>();
			for (int i=0;i<g.getNbMots();i++) {
				int d = g.getDep(i);
				if (d>=0) {
					shallForceDeps=true;
					int h = g.getHead(d);
					String l=g.getDepLabel(d);
					Object[] x = {h,l};
					forceddeps.put(i, x);
				}
			}
			
			// debug: if we wan to override existing deps, uncomment this line
			// shallForceDeps=false;
			
			
			if (shallForceDeps) {
				HashMap<Integer, Integer> forcedheads = new HashMap<Integer, Integer>();
				for (int gov : forceddeps.keySet()) {
					Object[] x = forceddeps.get(gov);
					forcedheads.put(gov+1, (Integer)x[0]+1);
				}
				ParallelExtract.forcedHeads=forcedheads;
			} else ParallelExtract.forcedHeads=null;

			Parser.outputParses(options,pipe, decoder, params);
			List<DetGraph> graphParsedtmp = GraphIO.loadConll09(outf.getAbsolutePath())[0];

			if (graphParsedtmp.size()>0) {
				DetGraph gg = graphParsedtmp.get(0);
				if (shallForceDeps) {
					for (int gov : forceddeps.keySet()) {
						Object[] x = forceddeps.get(gov);
						int d = gg.getDep(gov);
						if (d>=0)
							gg.removeDep(d);
						else
							System.out.println("warning: il devrait y avoir un head a forced head !");
						gg.ajoutDep((String)x[1], gov, (Integer)x[0]);
					}
				}
				gg.setSource(g.getSource());
				for (int i=0;i<g.getNbMots();i++) {
					gg.getMot(i).setPosInTxt(g.getMot(i).getDebPosInTxt(), g.getMot(i).getEndPosInTxt());
				}
				return gg;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return g;
	}
	void parseMateOneUtt(int graph) {
		DetGraph gg = parseMateOneUtt(js.allgraphs.get(graph));
		if (gg!=null) {
			js.allgraphs.set(graph, gg);
			js.repaint();
		}
	}
	void parseMate() {
		int selmin = 0, selmax=js.allgraphs.size()-1;
		if (js.seldeb >= 0) {
			// il y a une selection: on ne parse que la selection
			selmin = js.seldeb;
			selmax = js.curgraph;
			if (selmin > selmax) {
				selmin = js.curgraph;
				selmax = js.seldeb;
			}
		}
		for (int i=selmin;i<=selmax;i++) {
			if (js.allgraphs.get(i).getNbMots()<=1) continue;
			System.out.println("parsing "+i);
			parseMateOneUtt(i);
			js.repaint();
		}
	}

	public static void parseAllWithGroups(List<DetGraph> gs) {
		MateParser parser = MateParser.getMateParser(null);
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			if (g.getNbMots()<=1) continue;
			DetGraph gg=parser.parseMateOneUtt(g);
			JSafran.projectOnto(gg, g, true, false,false);
		}
	}

	static void parseAll(List<DetGraph> gs) {
		String ftmp = "_mate_tmp.conll";
		String outf = "_mate_out.conll";
		GraphIO.saveConLL09(gs, null, ftmp);
		final String[] args={"-test",ftmp,"-model",modfile,"-out",outf};
		try {
			Options options = new Options(args);
			Pipe pipe = new Pipe(options);
			Parameters params = new ParametersFloat(0);  // total should be zero and the parameters are later read
			System.out.println("loading MATE models... "+modfile);

			Decoder decoder = Parser.readAll(options, pipe, params);
			ParallelExtract.forcedHeads=null;
			Parser.outputParses(options,pipe, decoder, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public void trainMate(List<DetGraph> gs) {
		// save les graphes en conll09
		String ftmp = "_mate_tmp.conll";
		GraphIO.saveConLL09(gs, null, ftmp);
		final String[] args={"-train",ftmp,"-model",modfile};
		try {
			Parser.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	void trainMate() {
		trainMate(js.allgraphs);
	}

	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		List<DetGraph> gs = gio.loadAllGraphs(args[1]);
		if (args[0].equals("-train")) 
			trainMate(gs);
		else if (args[0].equals("-parse")) {
			// nouvelle version: preserver les groupes en projetant les deps sur les graphes d'origine
			parseAllWithGroups(gs);
			gio.save(gs, FileUtils.noExt(args[1])+"_mate.xml");
		} else if (args[0].equals("-test")) {
			// vieille version
			MateParser parser = new MateParser(null);
			ArrayList<DetGraph> resgs = new ArrayList<DetGraph>();
			for (DetGraph g : gs) {
				resgs.add(parser.parseMateOneUtt(g));
			}
			GraphIO.saveConLL06(resgs, "output.conll");
		}
	}

	void trainMateTagger() {
		// save les graphes en conll09
		String ftmp = "_mate_tmp.conll";
		GraphIO.saveConLL09(js.allgraphs, null, ftmp);
		String modfile = "matetag.mods";
		final String[] args={"-train",ftmp,"-model",modfile};
		try {
			Tagger.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	void testMateTagger() {
		// save les graphes en conll09
		String ftmp = "_mate_tmp.conll";
		String outf = "_mate_out.conll";
		GraphIO.saveConLL09(js.allgraphs, null, ftmp);
		String modfile = "matetag.mods";
		final String[] args={"-test",new File(ftmp).getAbsolutePath(),"-model",modfile,"-out",new File(outf).getAbsolutePath()};
		try {
			Tagger.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		js.allgraphs=GraphIO.loadConll09(outf)[0];
		js.repaint();
	}

	public static JMenu getMenu(final JSafran main) {
		JMenu menu = new JMenu("Mate parser");
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem estermods = new JRadioButtonMenuItem("ESTER mods");
		estermods.setSelected(false);
		estermods.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				POStagger.setFrenchModels();
				MateParser.setMods("mate.mods.ETB");
			}
		});
		group.add(estermods);
		menu.add(estermods);
		JRadioButtonMenuItem p7mods = new JRadioButtonMenuItem("Paris7 mods");
		p7mods.setSelected(true);
		p7mods.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				POStagger.setFrenchModels();
				MateParser.setMods("mate.mods");
			}
		});
		group.add(p7mods);
		menu.add(p7mods);
		JRadioButtonMenuItem wsjmods = new JRadioButtonMenuItem("WSJ mods");
		wsjmods.setSelected(true);
		wsjmods.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				POStagger.setEnglishModels();
				MateParser.setMods("mate.mods.WSJ");
			}
		});
		group.add(wsjmods);
		menu.add(wsjmods);
		JRadioButtonMenuItem usermods = new JRadioButtonMenuItem("mate.mods");
		usermods.setSelected(true);
		usermods.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				POStagger.setEnglishModels();
				MateParser.setMods("mate.mods");
			}
		});
		group.add(usermods);
		menu.add(usermods);
		menu.addSeparator();

		JMenuItem trainmate = new JMenuItem("train MATE parser");
		menu.add(trainmate);
		trainmate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MateParser.getMateParser(main).trainMate();
			}
		});
		JMenuItem parsemate = new JMenuItem("parse MATE parser");
		menu.add(parsemate);
		parsemate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MateParser.getMateParser(main).parseMate();
			}
		});
		JMenuItem parsemateone = new JMenuItem("parse MATE parser single utt [Shift-p]");
		menu.add(parsemateone);
		parsemateone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MateParser.getMateParser(main).parseMateOneUtt(main.curgraph);
			}
		});
		JMenuItem trainmatetag = new JMenuItem("train MATE tagger");
		menu.add(trainmatetag);
		trainmatetag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MateParser.getMateParser(main).trainMateTagger();
			}
		});
		return menu;
	}
}
