package jsafran;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import plugins.applis.SimpleAligneur.Aligneur;

public class JSafran2Jtrans {
	static Thread jtransplay = null;
	private static Aligneur windowJtrans = null;
	public static JSafran main;
	
	public static void listenWithJtrans() {
		if (jtransplay!=null) {
			jtransplay.interrupt();
			return;
		}
		// Normalement, Jsafran garde la référence vers un fichier texte-source. Mais, des cas posent problème:
		// 1- lorsqu'il n'y a pas de texte d'origine, mais que les phrases sont éditées manuellement from scratch
		// 2- lorsque l'utilisateur a modifié le texte dans JSafran
		// 3- lorsque le fichier texte d'origine a disparu
		//
		// dans les cas 1+3, il faut sauver un nouveau fichier texte-source.
		// dans le cas 2, la tokenisation a été préservée même si les formes ont changé; on suppose que les nouveaux tokens gardent
		//     leur ancienne position. Attention, il peut y avoir de nouveaux tokens qui n'ont plus de position !

		// on commence par verifier si une fenetre jtrans est deja ouverte
		if (windowJtrans!=null) {
			if (windowJtrans.isVisible()) {
				if (main.allgraphs!=null&&main.allgraphs.size()>0) {
					// se positionne dans le fichier son
					DetGraph g = main.allgraphs.get(main.curgraph);
					System.out.println("position jtrans to "+g.getMot(main.editword).getDebPosInTxt());
					windowJtrans.gotoSourcePos(g.getMot(main.editword).getDebPosInTxt());
					System.err.println("start playing from jsafran...");
					windowJtrans.ctrlbox.getPlayerGUI().startPlaying();

					jtransplay = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(4000);
							} catch (InterruptedException e) {
							} finally {
								windowJtrans.ctrlbox.getPlayerGUI().stopPlaying();
								System.err.println("stop playing from jsafran");
							}
							jtransplay=null;
						}
					});
					jtransplay.start();
				}
				return;
			}
		}

		// on commence par vérifier si un fichier .jtrans est associé au fichier courant
		if (main.jtransfile!=null) {
			// si oui, alors pas de problèmes, il suffit de repérer les tokens qui matchent par leur position dans la source (même si la source est inaccessible !)
			windowJtrans = new Aligneur();
//			windowJtrans.run();
			windowJtrans.loadProject(main.jtransfile);
			if (main.allgraphs!=null&&main.allgraphs.size()>0) {
				// se positionne dans le fichier son
				DetGraph g = main.allgraphs.get(main.curgraph);
				windowJtrans.gotoSourcePos(g.getMot(0).getDebPosInTxt());
				// TODO: deiconifie + donne le focus a windowJtrans + positionne le scrolling pour avoir la phrase au milieu
			}
		} else {
			// sinon, on propose d'associer un fichier .jtrans existant ou d'aligner automatiquement avec JTrans
			int ret = JOptionPane.showConfirmDialog(null, "Do you want to reload a previous alignment ?");
			if (ret==JOptionPane.OK_OPTION) {
				JFileChooser jfc = new JFileChooser(new File("./"));
				ret = jfc.showOpenDialog(main.jf);
				if (ret==JFileChooser.APPROVE_OPTION) {
					if (jfc.getSelectedFile().exists()) {
						main.jtransfile = jfc.getSelectedFile().getAbsolutePath();
						listenWithJtrans();
					}
				}
			} else {
				// si le end-user a choisi d'aligner la source avec Jtrans:
				windowJtrans = new Aligneur();
//				windowJtrans.run();
/*
				if (main.curgraph<main.allgraphs.size()) {
					System.err.println("jtrans source "+main.allgraphs.get(main.curgraph).getSource());
					File srcf = new File(main.allgraphs.get(main.curgraph).getSource().getPath());
					System.err.println("debug1 "+srcf.getAbsolutePath());
					if (srcf.exists()) windowJtrans.loadtxt(srcf);
				}
				*/
				StringBuilder sb = new StringBuilder();
				for (int i=0;i<main.allgraphs.size();i++) {
					sb.append(main.allgraphs.get(i).toString());
					sb.append('\n');
				}
				windowJtrans.edit.setText(sb.toString());
				windowJtrans.repaint();
			}
		}
	}

}
