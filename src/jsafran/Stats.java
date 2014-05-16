package jsafran;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * calcule des stats sur les graphes et les integre a JSynATSGUI
 * 
 * @author xtof
 *
 */
public class Stats {
	JSafran main;
	TreeMap<Integer, Integer> nGraphsParUtt = new TreeMap<Integer, Integer>();
	
	TreeMap<String, TreeMap<String, Integer>> pos2dep = new TreeMap<String, TreeMap<String,Integer>>();
	
	public Stats(JSafran m) {main=m;}
	
	public void calcNdeps(List<DetGraph> gs) {
		// calcule le nb de deps qui partent d'un POS
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				String pos = g.getMot(j).getPOS();
				TreeMap<String, Integer> dep2n = pos2dep.get(pos);
				if (dep2n==null) {
					dep2n = new TreeMap<String, Integer>();
					pos2dep.put(pos, dep2n);
				}
				int[] deps = g.getDeps(j);
				for (int k=0;k<deps.length;k++) {
					String dep = g.getDepLabel(deps[k]);
					Integer co = dep2n.get(dep);
					if (co==null) {
						dep2n.put(dep, 1);
					} else {
						dep2n.put(dep, co+1);
					}
				}
			}			
		}
		showDepsPerPos(0);
	}

	// TODO
	public void calcNPOS(List<DetGraph> gs) {
		// calcule la distrib des POS en head d'une dep
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			for (int j=0;j<g.getNbMots();j++) {
				int[] dep = g.getDeps(j);
				for (int d : dep) {
					String lab = g.getDepLabel(d);
					TreeMap<String, Integer> pos2n = pos2dep.get(lab);
					if (pos2n==null) {
						pos2n = new TreeMap<String, Integer>();
						pos2dep.put(lab, pos2n);
					}
					String poshead = g.getMot(g.getHead(d)).getPOS();
					Integer co = pos2n.get(poshead);
					if (co==null) {
						pos2n.put(poshead, 1);
					} else {
						pos2n.put(poshead, co+1);
					}
				}
			}			
		}
		showDepsPerPos(1);
	}

	public void calcNgraphs(List<DetGraph> gs) {
		// calcule le nombre de graphes par phrase
		for (int i=0;i<gs.size();i++) {
			DetGraph g = gs.get(i);
			int ngraphs = 0;
			for (int j=0;j<g.getNbMots();j++) {
				int[] deps = g.getDeps(j);
				if (deps.length==0) ngraphs++;
			}
			Integer co=nGraphsParUtt.get(ngraphs);
			if (co==null) {
				nGraphsParUtt.put(ngraphs, 1);
			} else {
				nGraphsParUtt.put(ngraphs, co+1);
			}
		}
		showGraphsParUtt();
	}
	
	// ================== GUI part
	
	public void showDepsPerPos(final int config) {
		int n=0;
		for (String pos : pos2dep.keySet()) {
			TreeMap<String, Integer> dep2n = pos2dep.get(pos);
			for (String dep : dep2n.keySet()) {
				int co=dep2n.get(dep);
				n+=co;
			}
		}
		final String[] listData = new String[n];
		int i=0;
		for (String pos : pos2dep.keySet()) {
			TreeMap<String, Integer> dep2n = pos2dep.get(pos);
			for (String dep : dep2n.keySet()) {
				int co=dep2n.get(dep);
				listData[i++]=pos+" "+dep+" "+co;
			}
		}
		final JList list = new JList(listData);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("mouse clicked "+main.curgraph);
				int idx = list.getSelectedIndex();
				String[] ss = listData[idx].split(" ");
				String pos=ss[0];
				String dep=ss[1];
				System.out.println("lookfour "+pos+" "+dep);
				// cherche le graphe suivant avec cette config
				boolean found=false;
				b:
				for (int i=main.curgraph+1;i<main.allgraphs.size();i++) {
					DetGraph g = main.allgraphs.get(i);
					for (int j=0;j<g.getNbMots();j++) {
						if (config==0) {
							String pos2 = g.getMot(j).getPOS();
							if (pos2.equals(pos)) {
								int[] deps = g.getDeps(j);
								for (int k=0;k<deps.length;k++) {
									String dep2 = g.getDepLabel(deps[k]);
									if (dep2.equals(dep)) {
										main.curgraph=i;
										main.changedSentence();
										main.repaint();
										found=true;
										break b;
									}
								}
							}
						} else if (config==1) {
							for (int d : g.getDeps(j)) {
								String lab = g.getDepLabel(d);
								if (lab.equals(pos)) {
									String head = g.getMot(g.getHead(d)).getPOS();
									if (head.equals(dep)) {
										main.curgraph=i;
										main.changedSentence();
										main.repaint();
										found=true;
										break b;
									}
								}
							}
						}
					}			
				}
				if (!found) {
					// TODO: recommencer du debut
				}
			}
		});
		JScrollPane slist = new JScrollPane(list);
		JFrame fen = new JFrame("statistiques");
		fen.getContentPane().add(slist);
		fen.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fen.setSize(150,300);
		fen.setVisible(true);
	}
	
	public void showGraphsParUtt() {
		final String[] listData = new String[nGraphsParUtt.size()];
		int i=0;
		for (int n : nGraphsParUtt.keySet()) {
			String s = n+" "+nGraphsParUtt.get(n);
			listData[i++]=s;
		}
		final JList list = new JList(listData);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx = list.getSelectedIndex();
				String[] ss = listData[idx].split(" ");
				int ng = Integer.parseInt(ss[0]);
				// cherche le graphe suivant avec ce nb de graphes
				boolean found=false;
				for (int i=main.curgraph+1;i<main.allgraphs.size();i++) {
					DetGraph g = main.allgraphs.get(i);
					int ngraphs = 0;
					for (int j=0;j<g.getNbMots();j++) {
						int[] deps = g.getDeps(j);
						if (deps.length==0) ngraphs++;
					}
					if (ngraphs==ng) {
						main.curgraph=i;
						main.changedSentence();
						main.repaint();
						found=true;
						break;
					}
				}
				if (!found) {
					// TODO: recommencer du debut
				}
			}
		});
		JScrollPane slist = new JScrollPane(list);
		JFrame fen = new JFrame("statistiques");
		fen.getContentPane().add(slist);
		fen.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fen.setSize(150,300);
		fen.setVisible(true);
	}
}
