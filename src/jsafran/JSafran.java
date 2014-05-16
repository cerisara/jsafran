package jsafran;


import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jsafran.correction.Projection;
import jsafran.parsing.unsup.rules.RulesCfgfile;
import jsafran.searchfilters.TreeSearch;
import jsafran.searchfilters.TreeSearchCallback;
import corpus.text.TextSegments;
import corpus.text.TextSegments.segtypes;

import utils.FileUtils;
import utils.Interruptable;
import utils.SuiteDeMots;

/**
 * 
 * @author xtof
 *
 */

public class JSafran extends JPanel {
	public List<DetGraph> allgraphs = new ArrayList<DetGraph>();
	public int curgraph = 0;
	public int editword = 0, editword0=-1;

	public String jtransfile = null;
	HashSet<String> postags = new HashSet<String>();
	
	// contient tous les niveaux des graphes
	public List<List<DetGraph>> levels = new ArrayList<List<DetGraph>>();
	public int curlevel=0;

	// ========================================
	// PARSER
	final int parsingMode = MaltParserTrainer.SVMSPLIT;
	public String mods = "mods2011";
	MaltParserThread maltparser = null;
	POStagger postagger;
	// ========================================

	GraphIO gio = null;

	String filename;

	int seldeb = -1;

	JFrame jf=null;
	JSafranGUI safranPanel=null;
	GraphPainter graphpainter=null;
	int VERTBASELINE = 200;
	int xmotedit = -1;

	boolean editmode = true, editlink = false;

	public int curdep = -1;

	final int Ngraphs = 3;

	public static boolean delPonctBeforeMalt=false;

	// panel listant toutes les dependances possibles
	JFrame deppanel = null;
	JList deplist = null;

	// contient les mots de la derniere phrase du fichier _edited
	ArrayList<String> motsDeLaDernierePhraseEditee = new ArrayList<String>();

	// contient les annotations du niveau "Sentence"
	ArrayList<String> sentenceKeywords = new ArrayList<String>();

	// contient les expressions multimots pour la re-tokenization
	ArrayList<String> multimots = new ArrayList<String>();

	boolean showpostag = false, showgroupnames = true;
	boolean showdeps = true;

	public ActionListener actionListener = null;

	List<DetGraph> graphs0 = null;
	int[] formoupos = null;
	String[] group = null;
	int graphs0idx=-1;
	Color normal;

	public static JSafran viewGraph(DetGraph g) {
		return viewGraph(g,true);
	}
	public static JSafran viewGraph(DetGraph g, int mot) {
		quitSoft=true;
		JSafran viewer = viewGraph(g,false);
		viewer.editword=mot;
		viewer.repaint();
		viewer.safranPanel.checkXscroll(viewer.xmotedit);
		try {
			synchronized (viewer) {
				viewer.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return viewer;
	}
	public static JSafran viewGraph(DetGraph g, boolean isblocking) {
		quitSoft=true;
		String[] savedeps = new String[Dep.depnoms.length];
		System.arraycopy(Dep.depnoms, 0, savedeps, 0, savedeps.length);
		// attention: les deps sont ecrasees
		JSafran viewer = new JSafran();
		Dep.depnoms = savedeps;

		viewer.allgraphs.add(g);
		viewer.initGUI();
		viewer.repaint();
		if (isblocking) {
			try {
				synchronized (viewer) {
					viewer.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return viewer;
	}
	public static JSafran viewGraph(List<DetGraph> gs, boolean isblocking) {
		quitSoft=true;
		String[] savedeps = new String[Dep.depnoms.length];
		System.arraycopy(Dep.depnoms, 0, savedeps, 0, savedeps.length);
		// attention: les deps sont ecrasees
		JSafran viewer = new JSafran();
		Dep.depnoms = savedeps;

		viewer.allgraphs.addAll(gs);
		viewer.initGUI();
		viewer.repaint();
		if (isblocking) {
			try {
				synchronized (viewer) {
					viewer.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return viewer;
	}
	public static JSafran viewGraph(DetGraph[] g) {
		quitSoft=true;
		String[] savedeps = new String[Dep.depnoms.length];
		System.arraycopy(Dep.depnoms, 0, savedeps, 0, savedeps.length);
		JSafran viewer = new JSafran();
		Dep.depnoms = savedeps;

		for (int i=0;i<g.length;i++) {
			viewer.allgraphs.add(g[i]);
		}
		viewer.initGUI();
		viewer.repaint();
		try {
			synchronized (viewer) {
				viewer.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return viewer;
	}
	public static JSafran viewGraph(List<DetGraph> g) {
		quitSoft=true;
		String[] savedeps = new String[Dep.depnoms.length];
		System.arraycopy(Dep.depnoms, 0, savedeps, 0, savedeps.length);
		JSafran viewer = new JSafran();
		Dep.depnoms = savedeps;

		viewer.allgraphs=g;
		viewer.initGUI();
		viewer.repaint();
		try {
			synchronized (viewer) {
				viewer.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return viewer;
	}
	public static JSafran viewGraph(List<DetGraph>[] g) {
		quitSoft=true;
		String[] savedeps = new String[Dep.depnoms.length];
		System.arraycopy(Dep.depnoms, 0, savedeps, 0, savedeps.length);
		JSafran viewer = new JSafran();
		Dep.depnoms = savedeps;

		viewer.allgraphs=g[0];
		viewer.initGUI();
		for (int i=1;i<g.length;i++) {
			while (viewer.levels.size()<=i) viewer.addLevel();
			viewer.levels.set(i, g[i]);
		}
		viewer.repaint();
		try {
			synchronized (viewer) {
				viewer.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return viewer;
	}
	public static JSafran viewGraph(DetGraph[] g, int mot) {
		quitSoft=true;
		String[] savedeps = new String[Dep.depnoms.length];
		System.arraycopy(Dep.depnoms, 0, savedeps, 0, savedeps.length);
		JSafran viewer = new JSafran();
		Dep.depnoms = savedeps;

		for (int i=0;i<g.length;i++) {
			viewer.allgraphs.add(g[i]);
		}
		viewer.initGUI();
		viewer.repaint();
		viewer.editword=mot;
		viewer.repaint();
		viewer.safranPanel.checkXscroll(viewer.xmotedit);
		try {
			synchronized (viewer) {
				viewer.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return viewer;
	}
	public void addGraph(DetGraph g) {allgraphs.add(g); repaint();}

	public void repaint() {
		if (jf!=null) {
			if (allgraphs!=null&&allgraphs.size()>0) {
				// check whether to add a new level
				DetGraph g = allgraphs.get(0);
				if (g!=null&&g.relatedGraphs!=null) {
					for (int i=0;i<g.relatedGraphs.size();i++) {
						DetGraph gg = g.relatedGraphs.get(i);
						boolean isAlreadyShown = false;
						for (int j=0;j<levels.size();j++) {
							if (levels.get(j).get(0)==gg) {
								isAlreadyShown=true; break;
							}
						}
						if (!isAlreadyShown) {
							ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
							levels.add(gs);
							for (int j=0;j<allgraphs.size();j++) {
								gs.add(j, allgraphs.get(j).relatedGraphs.get(i));
							}
						}
					}
				}
			}
			
			safranPanel.updateInfos();
			jf.repaint();
		}
	}

	void createDepPanel() {
		deppanel = new JFrame("Choose dependency");
		deplist = new JList(Dep.depnoms);
		JScrollPane scroll = new JScrollPane(deplist);
		deppanel.getContentPane().add(scroll);
		deppanel.setSize(500, 700);
		deppanel.setVisible(true);

		deplist.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10) { // ENTER
					valideDepChoice();
				}
			}
		});
	}

	void freeParser() {
		if (maltparser!=null) {
			maltparser.stopParsers();
			//			maltparser=null;
		}
	}
	void restartParser() {
		freeParser();
		int ncores = Runtime.getRuntime().availableProcessors();
		System.err.println("ncores "+ncores);
		maltparser = new MaltParserThread(mods,ncores, parsingMode);
	}

	void delNextWords() {
		DetGraph g = allgraphs.get(curgraph);
		if (editword>=0&&editword<g.getNbMots()) {
			g.delNodes(editword+1, g.getNbMots()-1);
			repaint();
		}
	}
	void delPrevWords() {
		DetGraph g = allgraphs.get(curgraph);
		if (editword>=0&&editword<g.getNbMots()) {
			g.delNodes(0,editword-1);
			repaint();
		}
	}
	void delCurWord() {
		DetGraph g = allgraphs.get(curgraph);
		if (editword>=0&&editword<g.getNbMots()) {
			g.delNodes(editword,editword);
			repaint();
		}
	}

	private void valideDepChoice() {
		int i = deplist.getSelectedIndex();
		allgraphs.get(curgraph).setDepIdx(curdep, i);
		if (deppanel != null) {
			deppanel.dispose();
			deppanel = null;
		}
		jf.repaint();
	}
	
	JDialog pmen = null;
	JList postlist;
	void editPOStag() {
		if (pmen==null) {
			pmen = new JDialog((Frame)jf, true);
			String[] pp = postags.toArray(new String[postags.size()]);
			Arrays.sort(pp);
			postlist = new JList(pp);
			postlist.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == 10) { // ENTER
						Mot m = allgraphs.get(curgraph).getMot(editword);
						String p = (String)postlist.getSelectedValue();
						m.setPOS(p);
						jf.repaint();
						pmen.setVisible(false);
					} else if (e.getKeyCode() == 27) { // ESC
						pmen.setVisible(false);
					}
				}
			});
			pmen.add(postlist);
			pmen.pack();
		} 
		pmen.setVisible(true);
		postlist.requestFocus();
	}
	
	void find() {
		deppanel = new JFrame("Choose dependency");
		deplist = new JList(Dep.depnoms);
		JScrollPane scroll = new JScrollPane(deplist);
		deppanel.getContentPane().add(scroll);
		deppanel.setSize(500, 700);
		deppanel.setVisible(true);

		deplist.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10) { // ENTER
					int depidx = deplist.getSelectedIndex();
					for (int i = curgraph + 1; i < allgraphs.size(); i++) {
						DetGraph g = allgraphs.get(i);
						for (int d = 0; d < g.deps.size(); d++) {
							if (g.deps.get(d).type == depidx) {
								curgraph = i;
								deppanel.dispose();
								deppanel = null;
								repaint();
								return;
							}
						}
						System.err.println("dep not found !");
					}
					deppanel.dispose();
					deppanel = null;
				}
			}
		});
	}

	String lastNoteSearch="";
	void findNote() {
		String s = JOptionPane.showInputDialog("Enter regexp to search for in notes:",lastNoteSearch);
		if (s==null) return;
		lastNoteSearch=s;
		Pattern pat = Pattern.compile(s);
		for (int i = curgraph + 1; i < allgraphs.size(); i++) {
			DetGraph g = allgraphs.get(i);
			if (g.comment!=null) {
				//				g.comment.indexOf(s)>=0) {
				System.err.println("DEBUPATTERN "+pat.pattern());
				Matcher mat = pat.matcher(g.comment);
				if (mat.find()) {
					System.err.println("pattern found "+s+" "+mat.start()+" "+mat.end());
					curgraph = i;
					repaint();
					return;
				}
			}
		}
		System.err.println("note not found !");
	}

	void findWord() {
		final JFrame jj = new JFrame("Enter word form");
		final JTextField tt = new JTextField(30);
		tt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String w = tt.getText().trim();
				for (int i = curgraph + 1; i < allgraphs.size(); i++) {
					DetGraph g = allgraphs.get(i);
					for (int widx = 0; widx < g.getNbMots(); widx++) {
						if (g.getMot(widx).getForme().equals(w)) {
							curgraph = i;
							jj.dispose();
							repaint();
							return;
						}
					}
				}
				System.err.println("word not found !");
				jj.dispose();
			}
		});
		jj.getContentPane().add(tt);
		jj.setSize(500, 50);
		jj.setVisible(true);
	}

	void joinSentences() {
		if (seldeb>=0) {
			int gmin=seldeb, gmax=curgraph;
			if (gmin>gmax) {int x=gmin; gmin=gmax; gmax=x;}
			seldeb = gmin; curgraph=gmax;
			for (int i=gmax-1;i>=gmin;i--) {
				System.err.println("debug "+i);
				allgraphs.get(i).append(allgraphs.get(i+1));
				allgraphs.remove(i+1);
				curgraph--;
				jf.repaint();
			}
		}
		seldeb=-1;
	}

	void addNote() {
		String initval = allgraphs.get(curgraph).comment;
		String c = JOptionPane.showInputDialog("Editer le commentaire:",initval);
		if (c==null) return;
		c=c.replace('é', 'e');
		c=c.replace('è', 'e');
		c=c.replace('ê', 'e');
		c=c.replace('ë', 'e');
		c=c.replace('à', 'a');
		c=c.replace('â', 'a');
		c=c.replace('ä', 'a');
		c=c.replace('ç', 'c');
		c=c.replace('ù', 'u');
		c=c.replace('û', 'u');
		c=c.replace('ü', 'u');
		c=c.replace('î', 'i');
		c=c.replace('ô', 'o');
		c=c.replace('ö', 'o');
		allgraphs.get(curgraph).comment = c;

		/*
		final JTextField tt = new JTextField(100);
		if (allgraphs.get(curgraph).comment != null)
			tt.setText(allgraphs.get(curgraph).comment);
		final JFrame jj = new JFrame("Enter a note for the current sentence");
		tt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String c = tt.getText().trim();
				c=c.replace('é', 'e');
				c=c.replace('è', 'e');
				c=c.replace('ê', 'e');
				c=c.replace('ë', 'e');
				c=c.replace('à', 'a');
				c=c.replace('â', 'a');
				c=c.replace('ä', 'a');
				c=c.replace('ç', 'c');
				c=c.replace('ù', 'u');
				c=c.replace('û', 'u');
				c=c.replace('ü', 'u');
				c=c.replace('î', 'i');
				c=c.replace('ô', 'o');
				c=c.replace('ö', 'o');
				allgraphs.get(curgraph).comment = c;
				jj.dispose();
			}
		});
		jj.getContentPane().add(tt);
		jj.setSize(1500, 50);
		jj.setVisible(true);
		 */
	}

	void screenshot() {
		int width = 50;
		int height = 100;
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		int maxx = graphpainter.paintGraph(curgraph, 80, g, false);
		height = graphpainter.hmax + 80;
		System.err.println("screenshot w="+maxx+" h="+height);
		img = new BufferedImage(maxx, height, BufferedImage.TYPE_INT_RGB);
		g = img.getGraphics();
		g.fillRect(0, 0, maxx, height);
		g.setColor(Color.black);
		graphpainter.paintGraph(curgraph, height - 40, g, false);

		try {
			ImageIO.write(img, "jpg", new File("/tmp/tt.jpg"));
			System.err.println("screenshot saved in /tmp/tt.jpg");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void clearGraph() {
		allgraphs.remove(curgraph);
		if (curgraph>=allgraphs.size()) curgraph--;
		repaint();
	}
	void clearAllGraph() {
		allgraphs.clear();
		curgraph=0;
		repaint();
	}
	void clearEmptyGraph() {
		for (int i=allgraphs.size()-1;i>=0;i--) {
			if (allgraphs.get(i).getNbMots()<=0) allgraphs.remove(i);
		}
		repaint();
	}
	void clearPosIdxGraph() {
		for (int i=allgraphs.size()-1;i>=0;i--) {
			DetGraph g = allgraphs.get(i);
			if (g.cursent>0) allgraphs.remove(i);
		}
		repaint();
	}
	void clearDeps() {
		allgraphs.get(curgraph).deps.clear();
		repaint();
	}
	void clearAll() {
		for (int i=0;i<allgraphs.size();i++)
			allgraphs.get(i).deps.clear();
		repaint();
	}

	public void gotoword(int delta) {
		if (editlink) {
			int head = allgraphs.get(curgraph).getHead(curdep) + delta;
			if (head>=0&&head<allgraphs.get(curgraph).getNbMots())
				allgraphs.get(curgraph).setHead(curdep,head);
		} else {
			if (delta==0) {
				// demande l'indice du mot
				String r=JOptionPane.showInputDialog("word index:");
				if (r!=null)
					editword = Integer.parseInt(r);
			} else {
				editword+=delta;
			}
		}
		if (editword<0) editword=0;
		else if (editword>=allgraphs.get(curgraph).getNbMots())
			editword=allgraphs.get(curgraph).getNbMots()-1;

		//		if (!safranPanel.isShifted) editword0=-1;
		safranPanel.updateInfos();

		// attention ! on ne peut pas appeler repaint() ici car on est dans le AWT thread !
		new Thread(new Runnable() {
			@Override
			public void run() {
				xmotedit=-1;
				repaint();
				while (xmotedit<0) {
					Thread.yield();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				safranPanel.checkXscroll(xmotedit);
				repaint();
			}
		}).start();
	}

	// TODO: create a new textSegment
	void splitWord() {
		DetGraph g = allgraphs.get(curgraph);
		Mot m = g.getMot(editword);
		String[] ss = m.getForme().split(" ");
		m.setForme(ss[0]);
		for (int i = 1; i < ss.length; i++) {
			Mot mm = new Mot(ss[i],ss[i],m.getPOS());
			g.insertMot(editword + i, mm);
		}
		repaint();
	}

	void mergeWords() {
		allgraphs.get(curgraph).mergeTwoWords(editword,true,' ');
		repaint();
	}
	public static void delponct(DetGraph g) {
		final String ponct = ",;:!?./§\"";
		for (int j=g.getNbMots()-1;j>=0;j--) {
			String s = g.getMot(j).getForme().trim();
			boolean onlyponct=true;
			for (int k=0;k<s.length();k++) {
				char c = s.charAt(k);
				if (ponct.indexOf(c)<0) {onlyponct=false; break;}
			}
			if (onlyponct) {
				g.delNodes(j, j);
			}
		}
	}
	void delponct() {
		for (int i=0;i<allgraphs.size();i++) {
			DetGraph g = allgraphs.get(i);
			delponct(g);
		}
	}

	void maltparse() {
		if (maltparser==null) restartParser();
		DetGraph g = allgraphs.get(curgraph);
		postagger.tag(g);
		DetGraph gg = maltparser.parseWithConstraints(g,false);
		allgraphs.set(curgraph, gg);
		float scoreParsing = maltparser.getScore();
		gg.conf=scoreParsing;
		repaint();
	}
	void parseall() {
		if (maltparser==null) restartParser();
		maltparser.parseList(allgraphs);
		System.err.println("parsing done");
		repaint();
	}
	void tagall() {
		for (int i=0;i<allgraphs.size();i++) {
			DetGraph g = allgraphs.get(i);
			postagger.tag(g);
		}
		repaint();
	}
	void train() {
		int rep = JOptionPane.showConfirmDialog(jf, "Training is done using all the files in a given directory. Training will override svmmods.mco models. Do you confirm you want to retrain models ?");
		if (rep==JOptionPane.OK_OPTION) {
			JFileChooser jfc = new JFileChooser(".");
			jfc.setDialogTitle("Choose the directoy with training files");
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			jfc.setMultiSelectionEnabled(false);
			int ret = jfc.showOpenDialog(jf);
			if (ret == JFileChooser.APPROVE_OPTION) {
				File traindir = jfc.getSelectedFile();
				MaltParserTrainer trainer = new MaltParserTrainer();
				trainer.train(".", "svmmods", traindir, parsingMode);
				System.err.println("training done");

				JOptionPane.showMessageDialog(null, "Training done !");

			}
		}
	}

	void decoupPhrase() {
		DetGraph g1 = allgraphs.get(curgraph).getSubGraph(0,editword);
		DetGraph g2 = allgraphs.get(curgraph).getSubGraph(editword + 1);
		if (g1.getNbMots() > 0 && g2.getNbMots() > 0) {
			allgraphs.set(curgraph, g1);
			allgraphs.add(curgraph + 1, g2);
		}
	}

	void confirmUttOK(float niveau) {
		DetGraph g = allgraphs.get(curgraph);
		g.conf = niveau;
	}

	void selection() {
		if (seldeb < 0)
			seldeb = curgraph;
		else
			seldeb = -1;
		repaint();
	}

	boolean triggerEvents = true;
	void changedSentence() {
		if (jf==null) return;
		if (curgraph<=0||curgraph>=allgraphs.size()) return;
		DetGraph g = allgraphs.get(curgraph);
		triggerEvents=false;
		for (int i=0;i<sentenceKeywords.size();i++) {
			if (g.comment!=null&&g.comment.indexOf(sentenceKeywords.get(i))>=0) {
				((JCheckBoxMenuItem)safranPanel.sentannot.getItem(i)).setSelected(true);
			} else {
				((JCheckBoxMenuItem)safranPanel.sentannot.getItem(i)).setSelected(false);
			}
		}
		triggerEvents=true;
		repaint();
	}
	void filterGraphsByAnnotations() {
		ArrayList<Integer> annotSet = new ArrayList<Integer>();
		DetGraph g = allgraphs.get(curgraph);
		for (int i=0;i<sentenceKeywords.size();i++) {
			if (g.comment!=null&&g.comment.indexOf(sentenceKeywords.get(i))>=0) {
				annotSet.add(i);
			}
		}
		System.err.println("filter with annots "+annotSet.size());
		Collections.sort(annotSet);
		for (int j=allgraphs.size()-1;j>=0;j--) {
			g = allgraphs.get(j);
			ArrayList<Integer> gSet = new ArrayList<Integer>();
			for (int i=0;i<sentenceKeywords.size();i++) {
				if (g.comment!=null&&g.comment.indexOf(sentenceKeywords.get(i))>=0) {
					gSet.add(i);
				}
			}

			if(gSet.size() == 0){
				allgraphs.remove(j);
				continue;
			}
			boolean blCommCont = true;
			for(int i=0; i<annotSet.size();i++){

				blCommCont = blCommCont && !gSet.contains(annotSet.get(i));

			}
			if(blCommCont)
				allgraphs.remove(j);


			/*else {
				Collections.sort(gSet);
				int i=0;
				for (i=0;i<gSet.size();i++)
					if (gSet.get(i)!=annotSet.get(i)) break;
				if (i<gSet.size()) {
					System.err.println("removing "+j);
					allgraphs.remove(j);
				}
				System.err.println("candidate "+j+" "+gSet.size()+" "+i);
			}*/
		}
		curgraph=0;
		repaint();
	}

	void jumpToUtt() {
		final JFrame jj = new JFrame("Enter utterance Index");
		final JTextField tt = new JTextField("     " + curgraph);
		tt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int u = Integer.parseInt(tt.getText().trim());
				jj.dispose();
				curgraph = u;
				changedSentence();
				repaint();
			}
		});
		jj.getContentPane().add(tt);
		jj.setSize(500, 50);
		jj.setVisible(true);
	}

	void terminateLinkEdition() {
		editlink = false;
		// check whether the modified link should be removed
		if (curdep >= 0) {
			if (allgraphs.get(curgraph).getGoverned(curdep) == allgraphs.get(curgraph).getHead(curdep)) {
				allgraphs.get(curgraph).removeDep(curdep);
			}
		}
		// no more selected link
		curdep = -1;
	}
	void terminateEdition() {
		terminateLinkEdition();
		editword = 0;
	}

	public int changeHead(boolean addAnotherDep) {
		if (allgraphs.get(curgraph) == null) {
			System.err
			.println("ERROR: vous ne pouvez pas editer un graphe vide !");
			return -1;
		}
		// on cherche un lien existant
		curdep = allgraphs.get(curgraph).getDep(editword);
		if (curdep < 0 || addAnotherDep) {
			// pas de lien/head, on en ajoute un
			curdep = allgraphs.get(curgraph).ajoutDepTmp(0, editword, editword);
			System.err.println("edit dep "+curdep);
		}
		editlink = true;
		return curdep;
	}

	void help() {
		JFrame jfh = new JFrame("help");
		JTextArea harea = new JTextArea();
		harea.setEditable(false);
		try {
			BufferedReader f = new BufferedReader(new FileReader("usage.txt"));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				harea.append(s+"\n");
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		jfh.getContentPane().add(new JScrollPane(harea));
		jfh.setSize(700, 500);
		jfh.setVisible(true);
	}

	void exportTxt() {
		String s = gio.askForSaveName();
		if (s==null) return;
		try {
			PrintWriter fout = new PrintWriter(new FileWriter(s));
			for (int i=0;i<allgraphs.size();i++) {
				StringBuilder sb=new StringBuilder();
				DetGraph g = allgraphs.get(i);
				for (int j=0;j<g.getNbMots();j++) {
					sb.append(g.getMot(j).getForme()+" ");
				}
				String res = sb.toString().trim();
				fout.println(res);
			}
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void exportConll() {
		if (gio == null) {
			JOptionPane.showMessageDialog(null, "Vous devez d'abord sauver le fichier au format XML !","warning export CoNLL",JOptionPane.WARNING_MESSAGE);
			return;
		}
		String s = gio.askForSaveName();
		if (s==null) return;
		try {
			Syntex2conll out = new Syntex2conll(new PrintWriter(new OutputStreamWriter(new FileOutputStream(s),Charset.forName("UTF-8"))));
			for (int i=0;i<allgraphs.size();i++) {
				out.processGraph(allgraphs.get(i));
			}
			out.terminate();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @deprecated
	 * @param direct
	 * @return
	 */
	boolean save(boolean direct) {
		if (direct) {
			if (filename!=null) {
				if (gio==null) gio = new GraphIO(jf);
				save(filename);
				return true;
			} else {
				JOptionPane.showMessageDialog(jf, "Erreur: aucun nom de fichier connu ? Utilisez saveAs");
				return false;
			}
		}
		if (gio == null) gio = new GraphIO(null);
		filename = gio.save(allgraphs,null);
		save(filename);
		return true;
	}

	public static boolean quitSoft=false;
	public void quitNoSave() {
		jf.dispose();
		synchronized (this) {
			notify();
		}
		if (!quitSoft)
			System.exit(0);
	}

	/**
	 * enleve l'extension d'un nom de fichier
	 */
	public static String noExt(String fich) {
		int i=fich.lastIndexOf('.');
		if (i<0) return fich;
		return fich.substring(0,i);
	}

	void randomSplit() {
		String s=JOptionPane.showInputDialog("Quel ratio entre les 2 parties (partie 1 / partie 2) ?");
		float p1 = Float.parseFloat(s);
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		for (int i=0;i<allgraphs.size();i++) gs.add(allgraphs.get(i));
		Collections.shuffle(gs);
		int n1 = (int)((float)allgraphs.size()*p1);
		GraphIO gio = new GraphIO(null);
		gio.save(gs.subList(0, n1), "random1.xml");
		gio.save(gs.subList(n1,gs.size()), "random2.xml");
	}
	void splitCorpus() {
		String s=JOptionPane.showInputDialog("en combien de parties faut-il splitter le fichier ?");
		if (s==null || s.length()==0) return;
		int ns = Integer.parseInt(s);
		String nomChunks = "chunk";
		if (filename!=null) {
			nomChunks = noExt(filename)+"_chunk";
		}
		splitn(nomChunks,ns);
		JOptionPane.showMessageDialog(null, "Les parties du corpus sont dans les fichiers "+nomChunks+"*.xml");
	}
	void splitn(String basenom, int nsplits) {
		int n = allgraphs.size()/nsplits;
		for (int i=0;i<nsplits-1;i++) {
			seldeb=i*n;
			curgraph=(i+1)*n-1;

			GraphIO gg = new GraphIO(jf);
			LinkedList<DetGraph> glist = new LinkedList<DetGraph>();
			for (int j = seldeb; j <= curgraph; j++)
				glist.add(allgraphs.get(j));
			gg.save(glist,basenom+i+".xml");
		}
		seldeb=(nsplits-1)*n;
		GraphIO gg = new GraphIO(jf);
		LinkedList<DetGraph> glist = new LinkedList<DetGraph>();
		for (int j = seldeb; j < allgraphs.size(); j++)
			glist.add(allgraphs.get(j));
		int i = nsplits-1;
		gg.save(glist,basenom+i+".xml");
	}

	void addSentenceAnnotation(int idx) {
		DetGraph g = allgraphs.get(curgraph);
		String annot = sentenceKeywords.get(idx);
		// check if not already there
		int i=-1;
		if (g.comment!=null)
			i=g.comment.indexOf(annot);
		else g.comment="";
		if (i<0) {
			g.comment = annot+" "+g.comment;
			System.err.println("adding sentence-level annotation "+annot);
		}
		repaint();
	}
	void removeSentenceAnnotation(int idx) {
		DetGraph g = allgraphs.get(curgraph);
		String annot = sentenceKeywords.get(idx);
		// check if not already there
		int i=g.comment.indexOf(annot);
		if (i>=0) {
			int end = i+annot.length();
			if (end>=g.comment.length())
				g.comment = g.comment.substring(0,i);
			else
				g.comment = g.comment.substring(0,i)+g.comment.substring(i+annot.length());
		}
		repaint();
	}

	void uttsegmente() {
		boolean withPonct=true;
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		for (int i=0;i<allgraphs.size();i++) {
			DetGraph g=allgraphs.get(i);
			String s = g.getSentence();
			g.clear();
			TextSegments segs = new TextSegments();
			boolean loaded=false;
			if (g.getSource()!=null) {
				try {
					segs.setSource(g.getSource().toURI());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				segs.preloadTextFile(g.getSource().getPath());
				loaded=true;
			}
			if (!loaded)
				segs.addOneString(s);
			segs.segmentePhrases();
			for (int z=0;z<segs.getNbSegments();z++) {
				DetGraph gg = g.getSubGraph(0); gg.clear();
				int motidx=0;
				TextSegments segsmots = segs.tokenizeBasic(i);
				segsmots.tokenizePonct();
				segsmots.tokenizeComments();
				for (int j=0;j<segsmots.getNbSegments();j++) {
					if (segsmots.getSegmentType(j)==segtypes.mot ||
							(withPonct && segsmots.getSegmentType(j)==segtypes.ponct)) {
						Mot m = new Mot(segsmots.getSegment(j).trim(), 0);
						m.setPosInTxt(segsmots.getSegmentStartPos(j), segsmots.getSegmentEndPos(j));
						g.addMot(motidx++,m);
					}
				}
				gs.add(gg);
			}
		}
		allgraphs=gs;
		repaint();
	}

	public void retokenize() {
		for (int i=0;i<allgraphs.size();i++) {
			DetGraph g = allgraphs.get(i);
			for (int j=g.getNbMots()-1;j>=0;j--) {
				String forme = g.getMot(j).getForme();
				String s = forme.replaceAll("_", " ").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\*", "");
				s=s.replaceAll("'", "' ");
				s=s.replaceAll("  +"," ");
				s=s.replace("aujourd' hui", "aujourd'hui");
				// TODO: séparer la ponctuation collée aux mots
				s=s.trim();
				StringTokenizer st = new StringTokenizer(s);
				if (!st.hasMoreTokens()) {
					// il faut supprimer le mot
					g.delNodes(j, j);
				} else {
					String ss = st.nextToken();
					if (!ss.equals(forme)) {
						// le mot a changé
						g.getMot(j).setForme(ss);
						g.getMot(j).setlemme(ss);
					}
					// on ajoute les autres mots créés
					int k=1;
					while (st.hasMoreTokens()) {
						ss = st.nextToken();
						Mot m = new Mot(ss, ss, g.getMot(j).getPOS());
						g.insertMot(j+k, m);
					}
				}
			}
		}
	}

	void retokenizeMultiMots() {
		for (int i=0;i<allgraphs.size();i++) {
			DetGraph g=allgraphs.get(i);
			for (int j=0;j<g.getNbMots()-1;j++) {
				deux:
					for (int k=0;k<multimots.size();k++) {
						String mm = multimots.get(k);
						int a=0, b=0, c=0;
						while (g.getMot(j+a).getForme().charAt(b) == mm.charAt(c)) {
							if (++c>=mm.length()) {
								// multimot trouve !
								while (--a>=0) g.mergeTwoWords(j,true,' ');
								break deux;
							}
							if (++b>=g.getMot(j+a).getForme().length()) {
								b=0; a++;
								if (j+a>=g.getNbMots()) break;
								if (mm.charAt(c++)!=' ') break;
							}
						}
					}
			}
		}
		repaint();
	}

	JFrame graphlist = null;

	void moveInGraphList(List<Integer> gidx) {
		graphlist = new JFrame("choose graph");
		graphlist.setSize(400,500);
		graphlist.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				graphlist.dispose();
				graphlist=null;
			}
		});
		final Integer[] ll = gidx.toArray(new Integer[gidx.size()]);
		final JList lst = new JList(ll);
		lst.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				curgraph=ll[lst.getSelectedIndex()];
				repaint();
			}
		});
		graphlist.getContentPane().add(lst);
		graphlist.setVisible(true);
	}

	private void loadConfig(InputStream is) {
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(is));
			ArrayList<String> deps = new ArrayList<String>();
			sentenceKeywords.clear();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("DEP")) {
					String[] ss = s.split(" ");
					deps.add(ss[1]);
					if (fastlabel==null) fastlabel=ss[1];
				} else if (s.startsWith("POSTAG ")) {
					String[] ss =s.split(" ");
					postags.add(ss[1]);
				} else if (s.startsWith("MALTMODS")) {
					String[] ss =s.split(" ");
					mods = ss[1];
				} else if (s.startsWith("MALTNOPONCT")) {
					String[] ss =s.split(" ");
					delPonctBeforeMalt = Boolean.parseBoolean(ss[1]);
				} else if (s.startsWith("SENTKEYWORD")) {
					String[] ss =s.split(" ");
					sentenceKeywords.add(ss[1]);
				} else if (s.startsWith("MULTIMOT")) {
					int i=s.indexOf(' ');
					multimots.add(s.substring(i+1).trim().toLowerCase());
				} else if (s.startsWith("POSCOLUMN")) {
					int i=s.indexOf(' ');
					SyntaxGraphs.poscolumn = Integer.parseInt(s.substring(i+1));
				} else if (s.startsWith("ENCODING")) {
					int i=s.indexOf(' ');
					SyntaxGraphs.forceEncoding=s.substring(i+1).trim();
				}
			}
			if (deps.size()>0) {
				Dep.depnoms = new String[0];
				for (int i=0;i<deps.size();i++)
					Dep.addType(deps.get(i));
			}
		} catch (Exception e) {}
	}
	private void loadFromConfig() {
		try {
			FileInputStream f0 = new FileInputStream("jsynats.cfg");
			BufferedReader f = new BufferedReader(new InputStreamReader(f0));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				if (s.startsWith("LOAD")) {
					String[] ss = s.split(" ");
					loadInLevel(Integer.parseInt(ss[1]),ss[2]);
				}
			}
		} catch (Exception e) {}
	}

	private void loadConfig() {
		try {
			FileInputStream f = new FileInputStream("jsynats.cfg");
			loadConfig(f);
			f.close();
		} catch (Exception e) {
			// on est peut-etre dans une applet: il faut charger la config dans les ressources du jar
			System.err.println("impossible d'acceder au fichier de config... try in the jar !");
			InputStream configstream = getClass().getResourceAsStream("/jsynats.cfg");
			loadConfig(configstream);
		}
	}

	public JSafran() {
		loadConfig();
	}
	public void initGUI() {
		jf = JSafranGUI.createJsafranWindow(this);
		safranPanel = JSafranGUI.getSafranPanel();
		JMenuBar bar = safranPanel.createMenus();
		jf.setJMenuBar(bar);
		jf.validate();
		jf.repaint();
	}

	public void printToLatex() {
		try {
			PrintWriter f = new PrintWriter(new FileWriter("/tmp/graphs.tex"));
			f.println("\\documentclass[a0paper,12pt,landscape]{article}");
			f.println("\\usepackage[utf8]{inputenc}");
			f.println("\\usepackage[francais]{babel}");
			f.println("\\usepackage{graphicx}");
			f.println("\\begin{document}");
			f.println("\\setlength{\\unitlength}{0.4em}");
			f.println("\\scriptsize");
			for (int i = 0; i < allgraphs.size(); i++) {
				DetGraph g = allgraphs.get(i);
				g.paintGraphToLatex(g.cursent,f);
				f.println();
			}
			f.println("\\end{document}");
			f.close();
			System.err.println("saved latex into /tmp/graphs.tex");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void extractOne() {
		try {
			File f0 = new File("extracted.xml");
			boolean exist = f0.exists();
			PrintWriter f = new PrintWriter(new FileWriter("extracted.xml",
					true));
			if (!exist)
				DetGraph.saveHeader(f);
			allgraphs.get(curgraph).save(f);
			f.close();
			System.err.println("graph extracted...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void saveSelection() {
		if (seldeb < 0) {
			JOptionPane.showMessageDialog(jf,"You must first select a range of sentences before saving them !");
			return;
		}
		int a = seldeb;
		int b = curgraph;
		if (a > b) {
			a = curgraph;
			b = seldeb;
		}
		JFileChooser jfc = new JFileChooser("./");
		if (filename!=null) {
			jfc.setSelectedFile(new File("train/",filename));
		}
		int ret = jfc.showSaveDialog(jf);
		if (ret==JFileChooser.APPROVE_OPTION) {
			File f = jfc.getSelectedFile();
			File d = f.getParentFile();
			if (!d.exists()) {
				JOptionPane.showMessageDialog(jf, "directory "+d.getAbsolutePath()+" does not exist !");
				return;
			}
			if (f.exists()) {
				int r = JOptionPane.showConfirmDialog(jf, "file "+f.getName()+" exists. Are you sure you want to overwrite it ?");
				if (r!=JOptionPane.OK_OPTION) return;
			}
			GraphIO gg = new GraphIO(jf);
			LinkedList<DetGraph> glist = new LinkedList<DetGraph>();
			for (int i = a; i <= b; i++) {
				glist.add(allgraphs.get(i));
			}
			// TODO: appeler save(String) !!!
			gg.save(glist,f.getAbsolutePath());
			JOptionPane.showMessageDialog(jf, "saved in " + f.getAbsolutePath());
		}
	}

	/**
	 * assumes names in the form fich_deb_fin.ext
	 */
	private String getBaseName(String name) {
		int i, j = name.length();
		j = name.lastIndexOf('.');
		i = name.lastIndexOf('_');
		if (i >= 0) {
			int k = name.lastIndexOf('_', i - 1);
			if (k >= 0)
				j = k;
		}
		i = name.lastIndexOf('/');
		if (i < 0)
			i = 0;
		else
			i++;
		String pre = name.substring(i, j);
		return pre;
	}

	private void extractTo() {
		GraphIO gg = new GraphIO(jf);
		LinkedList<DetGraph> glist = new LinkedList<DetGraph>();
		for (int i = 0; i <= curgraph; i++)
			glist.add(allgraphs.get(i));
		System.err.println("extracted " + glist.size());
		gg.save(glist,"extractto.xml");
	}

	void getSettings(int validx) {
		final int vv = validx;
		final JFrame jj = new JFrame("enter new value");
		final JTextField jtf = new JTextField();
		switch (validx) {
		case 0:
			jtf.setText("     " + graphpainter.fontDim);
			break;
		case 1:
			jtf.setText("     " + VERTBASELINE);
			break;
		}
		jtf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int v = Integer.parseInt(jtf.getText().trim());
				switch (vv) {
				case 0:
					graphpainter.fontDim = v;
					break;
				case 1:
					VERTBASELINE = v;
					break;
				}
				repaint();
				jj.dispose();
			}
		});
		jj.getContentPane().add(jtf);
		jj.pack();
		jj.setVisible(true);
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (graphpainter==null) {
			graphpainter = new GraphPainter(this);
		}
		if (curgraph<0) curgraph=0;
		graphpainter.paintGraph(curgraph - 1, VERTBASELINE, g, false);
		graphpainter.paintGraph(curgraph + 1, VERTBASELINE + VERTBASELINE + VERTBASELINE, g,
				false);
		graphpainter.paintGraph(curgraph, VERTBASELINE + VERTBASELINE, g,
				editmode);
		int lev=0, nlevs=0;
		int utt=0, nutts=0;
		int wrd=0, nwrds=0;
		int nmrks=0;
		if (levels!=null) {lev=curlevel; nlevs=levels.size();}
		if (allgraphs!=null) {utt=curgraph; nutts=allgraphs.size();}
		if (allgraphs!=null&&allgraphs.size()>0&&curgraph>=0&&curgraph<allgraphs.size()&&allgraphs.get(curgraph)!=null	) {
			wrd=editword; nwrds=allgraphs.get(curgraph).getNbMots();
		}
		if (marks!=null) {nmrks=marks.size();}
		if (jf!=null)
			g.drawString("lev="+lev+"/"+nlevs+" utt="+utt+"/"+nutts+" wrd="+wrd+"/"+nwrds+" mrk="+curmark+"/"+nmrks, 40+safranPanel.getScrollbarPosition(), 40);
	}

	public void delLevel() {
		if (levels.size()<=1) return;
		levels.remove(curlevel--);
		if (curlevel<0) curlevel=0;
		allgraphs=levels.get(curlevel);
		repaint();
	}

	public static void projectOnto(DetGraph gfrom, DetGraph gto, boolean deps, boolean groups, boolean postags) {
		SuiteDeMots sfrom = new SuiteDeMots(gfrom.getMots());
		SuiteDeMots sto = new SuiteDeMots(gto.getMots());
		sfrom.align(sto);
		if (deps) {
			gto.clearDeps();
			for (int j=0;j<sfrom.getNmots();j++) {
				int[] ws = sfrom.getLinkedWords(j);
				if (ws.length<=0) continue;
				int[] ds = gfrom.getDeps(j);
				for (int d : ds) {
					int headfrom = gfrom.getHead(d);
					int[] heads = sfrom.getLinkedWords(headfrom);
					if (heads.length>=1) {
						gto.ajoutDep(gfrom.getDepLabel(d), ws[0], heads[0]);
					}
				}
			}
		}
		if (postags) {
			for (int j=0;j<sfrom.getNmots();j++) {
				int[] ws = sfrom.getLinkedWords(j);
				if (ws.length<=0) continue;
				for (int wto : ws) {
					gto.getMot(wto).setPOS(gfrom.getMot(j).getPOS());
					gto.getMot(wto).setlemme(gfrom.getMot(j).getLemme());
				}
			}
		}
		if (groups&&gfrom.groups!=null) {
			for (int i=0;i<gfrom.groupnoms.size();i++) {
				List<Mot> motsdugroupe = gfrom.groups.get(i);
				int w1=motsdugroupe.get(0).getIndexInUtt()-1;
				int w2=motsdugroupe.get(motsdugroupe.size()-1).getIndexInUtt()-1;
				int tw1=-1;
				while (w1<gfrom.getNbMots()) {
					int[] ws = sfrom.getLinkedWords(w1);
					if (ws.length>0) {
						tw1=ws[0];
						break;
					}
					w1++;
				}
				if (tw1<0) {
					System.err.println("WARNING: group1 perdu ! "+i+" "+gfrom+" "+gto);
					sfrom.printLinks();
					continue;
				}
				int tw2=-1;
				while (w2>=w1) {
					int[] ws = sfrom.getLinkedWords(w2);
					if (ws.length>0) {
						tw2=ws[ws.length-1];
						break;
					}
					w2--;
				}
				if (tw2<0) {
					System.err.println("WARNING: group2 perdu ! "+i+" "+gfrom+" "+gto);
					sfrom.printLinks();
					continue;
				}
				gto.addgroup(tw1, tw2, gfrom.groupnoms.get(i));
			}
		}
	}
	public void project2LowerLevel(boolean deps, boolean groups, boolean postags) {
		if (levels.size()<2) return;
		int lowerLevel = curlevel-1;
		if (lowerLevel<0) lowerLevel=levels.size()-1;
		List<DetGraph> gsFrom = levels.get(curlevel);
		List<DetGraph> gsTo = levels.get(lowerLevel);
		int ngs = gsFrom.size();
		if (ngs>gsTo.size()) ngs=gsTo.size();
		for (int i=0;i<ngs;i++) {
			DetGraph gfrom = gsFrom.get(i);
			DetGraph gto = gsTo.get(i);
			projectOnto(gfrom, gto, deps, groups, postags);
		}
		repaint();
	}

	public int addLevel() {
		List<DetGraph> gs = new ArrayList<DetGraph>();
		for (DetGraph g : allgraphs) {
			DetGraph gg = g.getSubGraph(0);
			gs.add(gg);
		}
		levels.add(gs);
		curlevel=levels.size()-1;
		allgraphs=levels.get(curlevel);
		return curlevel;
	}
	public void putInLevel(List<DetGraph> graphs, int level) {
		while (level>=levels.size()) addLevel();
		levels.get(level).clear();
		levels.get(level).addAll(graphs);
		curlevel=level;
		allgraphs=levels.get(level);
	}
	public void changeLevel(final boolean create) {
		Thread tt = new Thread(new Runnable() {
			@Override
			public void run() {
				if (curlevel>=levels.size()) {
					// sauve les graphes courants dans les niveaux car ils ne l'etaient pas
					// ceci ne doit survenir que pour le 1er niveau
					levels.add(allgraphs);
				}
				if (create) {
					curlevel=addLevel();
				} else {
					if (++curlevel>=levels.size()) curlevel=0;
				}
				allgraphs=levels.get(curlevel);
				if (curgraph>=allgraphs.size()) curgraph=0;
				repaint();
			}
		});
		tt.start();
		try {
			tt.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// level + graph + mot
	public ArrayList<int[]> marks = new ArrayList<int[]>();
	private int curmark=0;
	public void addMark(int graph, int word) {
		int[] m = {curlevel,graph,word};
		marks.add(m);
	}
	public void addMark() {
		int[] m = {curlevel,curgraph,editword};
		marks.add(m);
		repaint();
	}
	public void clearMarks() {
		marks.clear();
		repaint();
	}
	public void rewindMarks() {
		curmark=0;
	}
	void rmMarkedUtt() {
		if (marks.size()<=0) return;
		ArrayList<Integer> utts = new ArrayList<Integer>();
		for (int[] m : marks) {
			if (!utts.contains(m[1])) utts.add(m[1]);
		}
		Collections.sort(utts);
		for (int i=utts.size()-1;i>=0;i--) {
			allgraphs.remove((int)utts.get(i));
		}
		clearMarks();
	}
	public void gotoNextMark() {
		if (marks.size()<=0) return;
		if (curmark>=marks.size()) curmark=0;
		curlevel = marks.get(curmark)[0];
		curgraph = marks.get(curmark)[1];
		editword = marks.get(curmark)[2]-1;
		gotoword(1);
		curmark++;
	}
	public void gotoPrevMark() {
		if (marks.size()<=0) return;
		if (curmark<0) curmark=marks.size()-1;
		curlevel = marks.get(curmark)[0];
		curgraph = marks.get(curmark)[1];
		editword = marks.get(curmark)[2]-1;
		gotoword(1);
		curmark--;
	}
	public List<Integer> getAllMarks(int utt) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int[] m : marks) {
			if (m[1]==utt) res.add(m[2]);
		}
		return res;
	}

	String fastlabel=null;

	public void repeatAddLink() {
		int[] d=allgraphs.get(curgraph).getDeps(editword);
		if (d.length>0) {
			int h=allgraphs.get(curgraph).getHead(d[0]);
			allgraphs.get(curgraph).removeAllDeps(editword);
			allgraphs.get(curgraph).ajoutDep(fastlabel, editword, h);
			repaint();
		}
	}

	public void load() {
		load(null);
	}

	void loadPassage() {
		/*
		File wdir = new File("../passage/corpus/dev");
		if (!wdir.exists()) wdir = new File(".");
		System.err.println("current dir " + wdir.getAbsolutePath());
		JFileChooser chooser = new JFileChooser(wdir);
		int returnVal = chooser.showOpenDialog(null);
		String passfile;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			passfile = chooser.getSelectedFile().getAbsolutePath();
		} else return;

		final ArrayList<PassGraph> passgraphs = PassGraph.load(passfile);
		allgraphs.clear();
		new Thread() {
			public void run() {
				for (int i=0;i<passgraphs.size();i++) {
					DetGraph g = postagger.getDetGraph(passgraphs.get(i));
					allgraphs.add(g);
					if (i%10==0) repaint();
				}
			}
		}.start();
		 */
	}

	void loadAlign() {
		boolean copyDeps = false;
		int rep =  JOptionPane.showConfirmDialog(null, "Do you want to copy the current links onto the loaded sentences ?");
		copyDeps = (rep == JOptionPane.OK_OPTION);

		// cree un graphe "concatene" avec tous les graphes courant
		DetGraph oldBigGraph = allgraphs.get(0).getSubGraph(0, allgraphs.get(0).getNbMots()-1);
		for (int i=1;i<allgraphs.size();i++)
			oldBigGraph.append(allgraphs.get(i).getSubGraph(0));

		// charge la 2eme serie de graphes
		gio = new GraphIO(jf);
		List<DetGraph> allgraphs2 = gio.loadAllGraphs(null);

		DetGraph newBigGraph = allgraphs2.get(0);
		for (int i=1;i<allgraphs2.size();i++)
			newBigGraph.append(allgraphs2.get(i));

		// aligne les 2 big graphs
		SuiteDeMots olds = new SuiteDeMots(oldBigGraph.getMots());
		SuiteDeMots news = new SuiteDeMots(newBigGraph.getMots());
		olds.align(news);
		// resegmente les nouveaux graphes
		int oldbigidx=0;
		HashMap<Integer, DetGraph> newGraphs = new HashMap<Integer, DetGraph>();
		for (int i=0;i<allgraphs.size();i++) {
			int minForEnd = oldbigidx;
			DetGraph g = allgraphs.get(i);
			int nbWordsSeen = 1;
			int[] w = olds.getLinkedWords(oldbigidx);
			while (w.length==0 && ++nbWordsSeen<g.getNbMots()) {
				w = olds.getLinkedWords(++oldbigidx);
			}
			if (w.length==0) {
				oldbigidx++;
				System.err.println("warning: pas de deb ! "+i);
				continue;
			}
			int newGraphDeb = w[0];

			// pointe vers le dernier mot de la phrase
			oldbigidx+=g.getNbMots()-nbWordsSeen;
			int tmpidx = oldbigidx++;
			w = olds.getLinkedWords(tmpidx);
			while (w.length==0 && tmpidx>minForEnd+1) {
				w = olds.getLinkedWords(--tmpidx);
			}
			if (w.length==0) {
				System.err.println("warning; pas de fin ! "+i);
				continue;
			}
			int newGraphEnd = w[w.length-1];

			DetGraph g2 = newBigGraph.getSubGraph(newGraphDeb, newGraphEnd);
			g2.cursent=-g.cursent;
			newGraphs.put(i, g2);

			if (copyDeps) g2.copyDepsFrom(g);
		}
		System.err.println("resegmenting done "+newGraphs.size());
		// insert graphs
		for (int i=allgraphs.size()-1;i>=0;i--) {
			DetGraph g = newGraphs.get(i);
			if (g==null) continue;
			allgraphs.add(i+1, g);
		}
		repaint();
	}
	void loadIntercale(boolean concat) {
		System.err.println("load intercale "+concat);
		gio = new GraphIO(jf);
		if (concat) {
			concat4diff(allgraphs);
			loadInsert(null,1);
		} else {
			List<DetGraph> gs = gio.loadAllGraphs(null);
			int j=1,i=0;
			for (i=0;i<gs.size()&&j<allgraphs.size();i++) {
				allgraphs.add(j, gs.get(i));
				j+=2;
			}
			for (;i<gs.size();i++) allgraphs.add(gs.get(i));
		}
		jf.repaint();
	}
	void loadInsert(String nom, int everyN) {
		List<DetGraph> allgraphs2 = gio.loadAllGraphs(nom);

		concat4diff(allgraphs2);

		int idx0=0;
		int idx1=0;
		while (idx0<allgraphs.size() && idx1<allgraphs2.size()) {
			idx0+=everyN;
			allgraphs.add(idx0++, allgraphs2.get(idx1++));
		}
		jf.repaint();
	}

	private void loadList(String list) {
		try {
			BufferedReader f = new BufferedReader(new FileReader(list));
			GraphIO gio = new GraphIO(null);
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				List<DetGraph> gs = gio.loadAllGraphs(s);
				for (int i=0;i<gs.size();i++) {
					allgraphs.add(gs.get(i));
				}
			}
			f.close();
		} catch (IOException e) {

		}
	}

	void defgroup() {
		String groupnom = JOptionPane.showInputDialog("type du groupe:");
		if (groupnom==null) return;
		groupnom = groupnom.trim();
		if (groupnom.length()==0) return;
		int minw = editword;
		int maxw = minw;
		if (editword0>=0) {
			if (editword0>maxw) maxw=editword0;
			else if (editword0<minw) minw=editword0;
		}
		allgraphs.get(curgraph).addgroup(minw, maxw, groupnom);
		repaint();
	}

	// nouveau load qui charge tout en mode synchrone
	public void load(String nom) {load(nom,-1);}
	public void loadInLevel(int lev, String nom) {
		if (levels.size()==0&&lev==0) {
			load(nom);
			return;
		}
		System.out.println("loading "+nom+" in level "+lev);
		while (levels.size()<=lev) {
			changeLevel(true);
			levels.get(curlevel).clear();
		}
		GraphIO gio = new GraphIO(null);
		List<DetGraph> allgraphs2 = gio.loadAllGraphs(nom);
		curlevel=lev;
		curgraph=levels.get(lev).size();
		levels.get(lev).addAll(allgraphs2);
		allgraphs=levels.get(curlevel);
		repaint();
	}
	public void load(String nom, int nmax) {
		System.err.println("JSAFRAN.LOAD() "+nmax);
		if (nom==null) {
			JFileChooser jfc = new JFileChooser(new File("."));
			int rep = jfc.showOpenDialog(null);
			if (rep==JFileChooser.APPROVE_OPTION) {
				nom = jfc.getSelectedFile().getAbsolutePath();
			} else return;
		}

		System.err.println("loading "+nom+" "+jf+" "+allgraphs.size());
		gio = new GraphIO(jf);
		//		jf.setTitle("JSynATS " + nom);
		if (allgraphs.size()>0) {
			curgraph=allgraphs.size();
			// append
			List<DetGraph> allgraphs2 = gio.loadAllGraphs(nom);
			if (allgraphs2!=null)
				for (int i=0;i<allgraphs2.size();i++)
					allgraphs.add(allgraphs2.get(i));
		} else {
			System.err.println("load XML");
			try {
				BufferedReader f;
				if (nom.startsWith("res:")) {
					f=new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(nom.substring(4)), Charset.forName("UTF-8")));
				} else f = FileUtils.openFileUTF(nom);
				String s = f.readLine();
				System.err.println("loaded first line: "+s);
				assert s.startsWith("<checkenc");
				s = f.readLine();
				assert s.startsWith("<encoding");
				s = f.readLine();
				if (s!=null&&s.startsWith("jtransfile ")) {
					jtransfile = s.substring(11);
					s=f.readLine();
				}
				if (s!=null&&s.startsWith("nlevels ")) {
					int nl = Integer.parseInt(s.substring(8));
					s = f.readLine();
					assert s.startsWith("ngraphs ");
					int ngs = Integer.parseInt(s.substring(8));
					// TODO: si il y a plusieurs levels, ceci va planter !!!
					if (nmax>=0&&nmax<ngs) ngs=nmax;
					levels.clear();
					for (int l=0;l<nl;l++) {
						final ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
						final SyntaxGraphs m = new SyntaxGraphs(new GraphProcessor() {
							@Override
							public void terminate() {}
							@Override
							public void processGraph(DetGraph graph) {
								gs.add(graph);
							}
						});
						m.parseXML(f,ngs);
						levels.add(gs);
					}
					curlevel=0;
					allgraphs = levels.get(0);
					filename=nom;
				} else {
					allgraphs = gio.loadAllGraphs(nom);
				}
				// lecture des marks
				s = f.readLine();
				if (s!=null && s.startsWith("<marks>")) {
					int n = Integer.parseInt(s.substring(s.indexOf('>')+2));
					System.err.println("nmarks "+n);
					for (int i=0;i<n;i++) {
						s = f.readLine().replace('[', ' ').replace(']', ' ').trim();
						String[] ss = s.split(",");
						int[] r = new int[ss.length];
						for (int j=0;j<r.length;j++) r[j]=Integer.parseInt(ss[j].trim());
						marks.add(r);
					}
				}
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (curgraph>=allgraphs.size()) curgraph=0;
		}
		if (!nom.startsWith("res:"))
			filename=(new File(nom)).getAbsolutePath();
		if (jf!=null) {
			jf.setTitle(filename);
			repaint();
			changedSentence();
		}
	}

	// mode utilise pour comparer plusieurs annotations du meme fichier:
	// affiche chaque annotation sur une ligne, en "concatenant" les phrases ayant ete decoupees...
	private static void concat4diff(List<DetGraph> glist) {
		System.err.println("concatene les fichiers decoupes...");
		int oldidx=-2;
		for (int i=0;i<glist.size();i++) {
			if (glist.get(i).cursent==oldidx) {
				// meme phrase
				glist.get(i-1).append(glist.get(i));
				glist.remove(i--);
			} else {
				oldidx=glist.get(i).cursent;
			}
		}
	}

	void selectWordAtX(int x) {
		int selectedWord=-1;
		int mind = Integer.MAX_VALUE;
		for (int i=0;i<graphpainter.middle.length;i++) {
			int d=Math.abs(x-graphpainter.middle[i]);
			if (d<mind) {
				mind=d;
				selectedWord=i;
			}
		}
		if (selectedWord>=0) {
			if (editlink) {
				allgraphs.get(curgraph).setHead(curdep, selectedWord);
			} else {
				editword=selectedWord;
			}
		}
		repaint();
	}

	public void loadConll06() {
		JFileChooser jfc = new JFileChooser(new File("."));
		int rep = jfc.showOpenDialog(null);
		if (rep==JFileChooser.APPROVE_OPTION) {
			File file = jfc.getSelectedFile();
			List<DetGraph> gs = GraphIO.loadConll06(file.getAbsolutePath(),true);
			curgraph=0;
			allgraphs = gs;
			if (levels.size()!=0) levels.set(curlevel, allgraphs);
			Srl.graphNormal=gs;
			Srl.graphSrl = null;
			repaint();
		}
	}
	public void loadConll08() {
		JFileChooser jfc = new JFileChooser(new File("."));
		int rep = jfc.showOpenDialog(null);
		if (rep==JFileChooser.APPROVE_OPTION) {
			File file = jfc.getSelectedFile();
			List<DetGraph>[] gs = GraphIO.loadConll08(file.getAbsolutePath());
			curgraph=0;
			allgraphs = gs[0];
			changeLevel(true);
            levels.get(curlevel).clear();
            levels.get(curlevel).addAll(gs[1]);
			Srl.graphNormal=gs[0];
			Srl.graphSrl = gs[1];
			repaint();
		}
	}
	public void loadConll08Framenet() {
		JFileChooser jfc = new JFileChooser(new File("."));
		int rep = jfc.showOpenDialog(null);
		if (rep==JFileChooser.APPROVE_OPTION) {
			File file = jfc.getSelectedFile();
			List<DetGraph>[] gs = GraphIO.loadConll08FrameNet(file.getAbsolutePath());
			curgraph=0;
			allgraphs = gs[0];
			changeLevel(true);
            levels.get(curlevel).clear();
            levels.get(curlevel).addAll(gs[1]);
			Srl.graphNormal=gs[0];
			Srl.graphSrl = gs[1];
			repaint();
		}
	}
	public void loadConll09() {
		JFileChooser jfc = new JFileChooser(new File("."));
		int rep = jfc.showOpenDialog(null);
		if (rep==JFileChooser.APPROVE_OPTION) {
			File file = jfc.getSelectedFile();
			List<DetGraph>[] gs = GraphIO.loadConll09(file.getAbsolutePath());
			curgraph=0;
			putInLevel(gs[0], 0);
			putInLevel(gs[1], 1);
			Srl.graphNormal=gs[0];
			Srl.graphSrl = gs[1];
			curlevel=1;
			repaint();
		}
	}
	public void loadSTM(String nom) {
		if (nom==null) {
			JFileChooser jfc = new JFileChooser(new File("."));
			int rep = jfc.showOpenDialog(null);
			if (rep==JFileChooser.APPROVE_OPTION) {
				File file = jfc.getSelectedFile();
				nom = file.getAbsolutePath();
			} else return;
		}
		try {
			BufferedReader f = FileUtils.openFileISO(nom);
			int u=0;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.charAt(0)!=';') {
					int i = s.indexOf("male>");
					if (i>=0) {
						s=s.substring(i+6);
						s=s.replaceAll("\\[.*\\]", "");
						String[] ss = s.split(" ");
						DetGraph g = new DetGraph(); g.cursent=u++;
						for (int j=0;j<ss.length;j++) {
							Mot m = new Mot(ss[j],0);
							g.addMot(j, m);
						}
						allgraphs.add(g);
					}
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		repaint();
	}

	public static void saveMarks(List<int[]> marks, PrintWriter fout) {
		fout.println("<marks> "+marks.size());
		for (int[] m : marks) {
			fout.println(Arrays.toString(m));
		}
		fout.println("</marks>");
	}
	public static void save(List<List<DetGraph>> gs, String outname, String jtransfile, List<int[]> marks) {
		GraphIO gio = new GraphIO(null);
		PrintWriter fout = gio.saveEntete(outname);
		if (jtransfile!=null)
			fout.println("jtransfile "+jtransfile);
		fout.println("nlevels "+gs.size());
		List<DetGraph> allgraphs = gs.get(0);
		fout.println("ngraphs "+allgraphs.size());
		for (int l=0;l<gs.size();l++) {
			allgraphs = gs.get(l);
			for (int i=0;i<allgraphs.size();i++) {
				allgraphs.get(i).save(fout);
			}
		}
		if (marks.size()>0) saveMarks(marks, fout);
		fout.close();
	}
	public void save(String nom) {
		if (nom==null) {
			String dir0 = "./";
			if (filename!=null) {
				File ft = new File(filename);
				String dir1=ft.getParent();
				if (dir1!=null) dir0=dir1;
			}
			JFileChooser jfc = new JFileChooser(dir0);
			if (filename!=null) {
				jfc.setSelectedFile(new File(filename));
			}
			int ret = jfc.showSaveDialog(jf);
			if (ret==JFileChooser.APPROVE_OPTION) {
				File f = jfc.getSelectedFile();
				nom=f.getAbsolutePath();
			}
		}
		filename = nom;
		List<List<DetGraph>> gs = levels;
		if (gs==null||gs.size()==0) {
			gs = new ArrayList<List<DetGraph>>();
			gs.add(allgraphs);
		}
		JSafran.save(gs, nom, jtransfile, marks);
	}

	public String getMot(Mot m, int formoupos) {
		switch (formoupos) {
		case 0: return m.getForme();
		case 1: return m.getPOS();
		}
		return m.getForme();
	}
	void delLook4groups() {
		for (DetGraph g:graphs0) {
			while (g.groups!=null&&g.groups.size()>0&&g.groupnoms.get(g.groupnoms.size()-1).equals("searchres")) {
				g.groups.remove(g.groups.size()-1);
				g.groupnoms.remove(g.groupnoms.size()-1);
			}
		}
	}

	void nonproj() {
		final ArrayList<DetGraph> matchinggraphs = new ArrayList<DetGraph>();
		int gi=0;
		for (DetGraph g : allgraphs) {
			for (int i=0;i<g.getNbMots();i++) {
				int d = g.getDep(i);
				if (d<0) continue; // ne considere par les NA comme des non-proj
				int h = g.getHead(d);
				for (int j=0;j<i;j++) {
					int dd = g.getDep(j);
					if (dd>=0) {
						int hh = g.getHead(dd);
						if (hh>i&&hh<h || hh<i&&hh>h&&j<h || hh<h&&j>h&&j<i || hh>i&&j<i&&j>h) {
							g.addgroup(i, i, "searchres");
							addMark(gi, i);
							matchinggraphs.add(g);
							break;
						}
					}
				}
			}
			gi++;
		}
		normal = getBackground();
		setBackground(new Color(0.9f,1f,0.9f));
		if (editword0>=0) {
			if (editword0>editword) {
				int i = editword; editword=editword0; editword0=i;
			}
		}
		graphs0=allgraphs;
		graphs0idx=curgraph;
		allgraphs=matchinggraphs;
		curgraph=0;
		repaint();
	}

	void cycles() {
		final ArrayList<DetGraph> matchinggraphs = new ArrayList<DetGraph>();
		int gi=0;
		for (DetGraph g : allgraphs) {
			b:
				for (int i=0;i<g.getNbMots();i++) {
					HashSet<Integer> dejavus = new HashSet<Integer>();
					int j=i;
					while (g.getDep(j)>=0) {
						if (dejavus.contains(j)) {
							g.addgroup(j, j, "searchres");
							addMark(gi, j);
							matchinggraphs.add(g);
							break b;
						}
						dejavus.add(j);
						j=g.getHead(g.getDep(j));
					}
				}
		gi++;
		}
		normal = getBackground();
		setBackground(new Color(0.9f,1f,0.9f));
		if (editword0>=0) {
			if (editword0>editword) {
				int i = editword; editword=editword0; editword0=i;
			}
		}
		graphs0=allgraphs;
		graphs0idx=curgraph;
		allgraphs=matchinggraphs;
		curgraph=0;
		repaint();
	}

	public void look4multihead() {
		clearMarks();
		for (int i=0;i<allgraphs.size();i++) {
			DetGraph g = allgraphs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				int[] d = g.getDeps(j);
				if (d.length>1) {
					addMark(i, j);
				}
			}
		}
		repaint();
	}

	public String pref = null;

	public void look4tree() {
		if (graphs0!=null) {
			getOutOfSearchMode();
			return;
		}
		// version multi-lines
		graphs0=allgraphs;

		/*
		if (pref==null) {
			if (editword0>=0) {
				pref="";
				int minw = editword;
				int maxw = minw;
				if (editword0>maxw) maxw=editword0;
				else if (editword0<minw) minw=editword0;
				for (int w=minw;w<=maxw;w++) pref+="f="+allgraphs.get(curgraph).getMot(w).getForme()+",_,_ ";
				for (int w=minw+1;w<=maxw;w++) pref+="dep"+(w-minw)+"=dep"+(w-minw-1)+"+1 ";
			}
		}
		 */
		Projection bigOptionPane=new Projection(this);
		String rules = bigOptionPane.showInputDialog("Enter search/replace expression",pref);
		if (rules==null) {
			graphs0=null; return;
		}
		ArrayList<String> positiveRules = new ArrayList<String>();
		ArrayList<String> negativeRules = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(rules, "\n");
		while (st.hasMoreTokens()) {
			String rule = st.nextToken().trim();
			if (rule.length()==0 || rule.charAt(0)=='#') continue;
			if (rule.charAt(0)=='-') {
				System.err.println("WARNING: negative rules unstable !");
				negativeRules.add(rule.substring(1));
			} else {
				positiveRules.add(rule);
			}
		}
		final ArrayList<DetGraph> matchinggraphs = new ArrayList<DetGraph>();
		final TreeSearch[] positiveSearchers = new TreeSearch[positiveRules.size()];
		final TreeSearch[] negSearchers = new TreeSearch[negativeRules.size()];
		final ArrayList<String> foundwords = new ArrayList<String>();
		for (int i=0;i<positiveRules.size();i++) { 
			positiveSearchers[i] = TreeSearch.create(positiveRules.get(i), new TreeSearchCallback() {
				@Override
				public boolean callbackOneMatch(DetGraph g, HashMap<String, Object> vars) {
					Integer dep0 = (Integer)vars.get(TreeSearch.dependent+"0");
					g.addgroup(dep0, dep0, "searchres");
					Integer gid = (Integer)vars.get("GRAPHID");
					int[] m = {0,gid,dep0};
					marks.add(m);
					if (matchinggraphs.size()==0||matchinggraphs.get(matchinggraphs.size()-1)!=g)
						matchinggraphs.add(g);
					// cette fonction est appelee pour chaque mot qui matche
					foundwords.add(TreeSearch.getMatchingWords(g,vars));
					return true;
				}
			});
			positiveSearchers[i].onlyfirstmatch=!bigOptionPane.allSecTriplets;
		}
		for (int i=0;i<negativeRules.size();i++)
			negSearchers[i] = TreeSearch.create(negativeRules.get(i), new TreeSearchCallback() {
				@Override
				public boolean callbackOneMatch(DetGraph g, HashMap<String, Object> vars) {
					matchinggraphs.remove(g);
					return true;
				}
			});
		final utils.ProgressDialog waiting = new utils.ProgressDialog((JFrame)null,null,"please wait: searching...");
		Interruptable searchingproc = new Interruptable() {
			private boolean tostop=false;
			public void stopit() {
				tostop=true;
			}
			@Override
			public void run() {
				if (positiveSearchers.length==0) {
					matchinggraphs.addAll(allgraphs);
				} else
					for (int i=0;i<allgraphs.size();i++) {
						if (tostop) break;
						DetGraph g = allgraphs.get(i);
						for (TreeSearch searcher : positiveSearchers)
							searcher.findAll(g,i);
						waiting.setProgress((float)i/(float)allgraphs.size());
					}
				for (int i=matchinggraphs.size()-1;i>=0;i--) {
					DetGraph g = matchinggraphs.get(i);
					for (TreeSearch searcher : negSearchers)
						searcher.findAll(g,-1);
					waiting.setProgress((float)i/(float)allgraphs.size());
				}
			}
		};
		tempprocess.add(searchingproc);
		waiting.setRunnable(searchingproc);
		waiting.setVisible(true);
		tempprocess.remove(searchingproc);
		allgraphs=matchinggraphs;

		// sauvegarde des mots qui matchent
		TreeSearch.saveMatchingWords(foundwords);

		// affichage des res:
		graphs0idx=curgraph;
		normal = getBackground();
		setBackground(new Color(0.9f,1f,0.9f));
		if (editword0>=0) {
			if (editword0>editword) {
				int i = editword; editword=editword0; editword0=i;
			}
		}
		pref=rules;
		curgraph=0;
		editword--; gotoword(1);
	}
	public void stopallproc() {
		for (Interruptable p : tempprocess) p.stopit();
	}
	public static ArrayList<Interruptable> tempprocess = new ArrayList<Interruptable>();
	private void getOutOfSearchMode() {
		boolean goback = true;
		//		int res = JOptionPane.showConfirmDialog(null, "do you want to keep this result set, and delete the other original graphs ?");
		//		if (res==JOptionPane.OK_OPTION) goback=false;
		delLook4groups();
		formoupos=null;
		if (goback) {
			// remise des graphes d'origine
			allgraphs = graphs0;
			curgraph = graphs0idx;
		}
		graphs0=null; graphs0idx=-1;
		setBackground(normal);
		repaint();
	}
	void look4group() {
		if (graphs0!=null) {
			getOutOfSearchMode();
			return;
		}
		normal = getBackground();
		setBackground(new Color(0.9f,1f,0.9f));
		if (editword0>=0) {
			if (editword0>editword) {
				int i = editword; editword=editword0; editword0=i;
			}
			group = new String[editword-editword0+1];
		} else {
			group = new String[1];
		}

		// par defaut, on cherche les mots identiques
		formoupos = new int[group.length];
		Arrays.fill(formoupos, 0);
		graphs0=allgraphs;
		graphs0idx=curgraph;
		graphs0.get(graphs0idx).addgroup(editword0, editword, "searchres");
		creategroup();
		updateLookForList();
	}
	void creategroup() {
		ArrayList<Mot> gr = graphs0.get(graphs0idx).groups.get(graphs0.get(graphs0idx).groups.size()-1);
		int firstmot = gr.get(0).getIndexInUtt()-1;
		int lastmot  = gr.get(gr.size()-1).getIndexInUtt()-1;
		for (int i=firstmot;i<=lastmot;i++)
			group[i-firstmot]=getMot(graphs0.get(graphs0idx).getMot(i), formoupos[i-firstmot]);
		System.err.println("newsearch pattern: "+Arrays.toString(group));
	}
	void switchLook4() {
		ArrayList<Mot> gr = graphs0.get(graphs0idx).groups.get(graphs0.get(graphs0idx).groups.size()-1);
		int firstmot = gr.get(0).getIndexInUtt()-1;
		int lastmot  = gr.get(gr.size()-1).getIndexInUtt()-1;
		delLook4groups();
		graphs0.get(graphs0idx).addgroup(firstmot, lastmot, "searchres");
		int motidx = editword-firstmot;
		formoupos[motidx] = 1-formoupos[motidx];
		creategroup();
		updateLookForList();
	}
	void updateLookForList() {
		// recherche des graphes qui matchent
		List<DetGraph> graphs1 = new ArrayList<DetGraph>();
		graphs1.add(graphs0.get(graphs0idx));
		for (int j=graphs0idx+1;j<graphs0.size();j++) {
			DetGraph g = graphs0.get(j);
			boolean doesmatch = false;
			for (int k=0;k<=g.getNbMots()-group.length;k++) {
				int l=0;
				for (;l<group.length;l++)
					if (!getMot(g.getMot(k+l),formoupos[l]).equals(group[l])) break;
				if (l==group.length) {
					doesmatch=true;
					g.addgroup(k, k+group.length-1, "searchres");
					k+=group.length-1;
				}
			}
			if (doesmatch) graphs1.add(g);
		}
		allgraphs=graphs1;
		curgraph=0;
		repaint();
	}
	/*
	void projectGroupAnnotation() {
		if (graphs0!=null) {
			// QUE en mode search
			// le graphe 0 est la reference
			DetGraph g0 = allgraphs.get(0);
			int g0deb = g0.groups.get(g0.groups.size()-1).get(0);
			int g0fin = g0.groups.get(g0.groups.size()-1).get(g0.groups.get(g0.groups.size()-1).size()-1);
			for (int i=1;i<allgraphs.size();i++) {
				DetGraph g = allgraphs.get(i);
				for (int gidx=g.groups.size()-1;gidx>=0;gidx--) {
					if (!g.groupnoms.get(gidx).equals("searchres")) break;
					int gdeb = g.groups.get(gidx).get(0);
					for (int j=g0deb;j<=g0fin;j++) {
						int dep = g0.getDep(j);
						if (dep>=0) {
							int head = g0.getHead(dep);
							if (head>=g0deb&&head<=g0fin) {
								// on projette cette dep
								head-=g0deb;
								int gov = j-g0deb;
								String dlab = g0.getDepLabel(dep);
								dep = g.getDep(gov+gdeb);
								if (dep>=0) g.removeDep(dep);
								g.ajoutDep(dlab, gov+gdeb, head+gdeb);
							}
						}
					}
				}
			}
			repaint();
		}
	}
	 */

	public static void main(String args[]) throws Exception {
		if (args.length>0&&(args[0].endsWith("-help")||args[0].startsWith("-h"))) {
			System.err.println("usage: java jsafran.JSafran fichier_a_analyser");
			return;
		}
		JSafran n = new JSafran();
		int nmax=-1;
		for (int arg=0;arg<args.length;arg++) {
			if (args[arg].charAt(0)=='-') {
				if (args[arg].equals("-retag")) {
					if (args[arg+1].endsWith(".xmll")) {
						System.err.println("load list");
						n.loadList(args[arg+1]);
					} else {
						n.load(args[arg+1]);
					}
					n.tagall();
					n.save(FileUtils.noExt(args[arg+1])+"_treetagged.xml");
					break;
				} else if (args[arg].equals("-fr")) {
					RulesCfgfile.ENGLISH=false;
					n.initGUI();
					if (++arg>=args.length) {
						n.loadFromConfig();
					} else {
						n.load(args[arg],nmax);
					}
					n.repaint();
				} else if (args[arg].equals("-poscol")) {
					int col=Integer.parseInt(args[++arg]);
					SyntaxGraphs.poscolumn=col;
				} else if (args[arg].equals("-totxt")) {
					n.load(args[arg+1]);
					for (DetGraph g : n.allgraphs) System.out.println(g);
					return;
				} else if (args[arg].equals("-conll09to06")) {
					List<DetGraph>[] gs = GraphIO.loadConll09(args[++arg]);
					GraphIO.saveConLL06(gs[0], "output.conll");
					break;
				} else if (args[arg].equals("-toconll")) {
					final String[] aa = {args[arg+1]};
					Syntex2conll.main(aa);
					break;
				} else if (args[arg].equals("-nmax")) {
					nmax =Integer.parseInt(args[++arg]);
				} else if (args[arg].equals("-split")) {
					int nchunks = Integer.parseInt(args[++arg]);
					n.load(args[++arg]);
					n.splitn("chunk",nchunks);
				} else if (args[arg].equals("-empty")) {
					n.initGUI();
					n.repaint();
					break;
				}
			} else {
				n.initGUI();
				if (args[arg].endsWith(".xml"))
					n.load(args[arg],nmax);
				else if (args[arg].endsWith(".conll09")) {
					List<DetGraph>[] gs = GraphIO.loadConll09(args[arg]);
					n.curgraph=0;
					n.putInLevel(gs[0], 0);
					n.putInLevel(gs[1], 1);
					Srl.graphNormal=gs[0];
					Srl.graphSrl = gs[1];
					n.curlevel=1;
				}

				// on peut intercaler d'autres annotations
				int i=arg+1;
				while (i<args.length) {
					n.changeLevel(true);
					n.levels.get(n.curlevel).clear();
					GraphIO gio = new GraphIO(null);
					List<DetGraph> gs = gio.loadAllGraphs(args[i]);
					n.levels.get(n.curlevel).addAll(gs);
					i++;
				}
				n.repaint();
				break;
			}
		}
		if (args.length==0) {
			// pas d'argument
			n.initGUI();
			n.loadFromConfig();
			n.repaint();
		}
	}
}
