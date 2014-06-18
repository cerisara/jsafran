package jsafran;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Arrays;
import jsafran.searchfilters.SubTreeSelection;

public class GraphPainter {
	JSafran main;
	int fontDim = 18;
	int hmax;

	public GraphPainter(JSafran m) {main=m;}

	int[] middle;

	/**
	 * retourne la position extreme droite
	 */
	int paintGraph(int graphidx, int baseline, Graphics g, boolean editmode) {
		try {
			if (graphidx < 0 || graphidx >= main.allgraphs.size()
					|| main.allgraphs.get(graphidx) == null)
				return -1;
			int nbnodes = main.allgraphs.get(graphidx).getNbMots();

			Graphics2D g2 = (Graphics2D) g;
			Font font = new java.awt.Font("Arial Unicode MS", java.awt.Font.PLAIN, fontDim);
//			        Font.decode("arial PLAIN " + fontDim);
			Font fontedit = new java.awt.Font("Arial Unicode MS", java.awt.Font.ITALIC, fontDim);
//			        Font.decode("arial ITALIC " + fontDim);
			Font fontconf = Font.decode("times PLAIN 9");
			FontRenderContext frc = g2.getFontRenderContext();

			/*
			 * Affichage utterance number
			 */
			int pos = 10;
			TextLayout layout, layoutPOSTAG, layoutGroupName;
			//		layout = new TextLayout(""+graphidx,font,frc);
			layout = new TextLayout(""+main.allgraphs.get(graphidx).cursent,font,frc);
			if (main.allgraphs.get(graphidx).comment != null
					&& main.allgraphs.get(graphidx).comment.trim().length() > 0) {
				Color c = g2.getColor();
				g2.setColor(Color.yellow);
				layout.draw(g2, pos, baseline);
				g2.setColor(c);
			} else {
				layout.draw(g2, pos, baseline);
			}

			/*
			 * Affichage confiance
			 */
			layout = new TextLayout("" + main.allgraphs.get(graphidx).conf, fontconf,
					frc);
			layout.draw(g2, pos, baseline + 10);

			/*
			 * Affichage des mots lineairement
			 */
			Color normalcolor = g.getColor();
			pos = 50;
			middle = new int[nbnodes]; // contient la position X du milieu
			// de chaque mot
			Font fontpostag = Font.decode("system PLAIN "
					+ (int) ((float) fontDim * 0.6f));
			int firstWordDrawn = main.editword-40;
			if (firstWordDrawn<0) firstWordDrawn=0;
			int lastWordDrawn = main.editword+40;
			if (lastWordDrawn>nbnodes) lastWordDrawn=nbnodes;
			for (int wordidx=firstWordDrawn; wordidx < lastWordDrawn; wordidx++) {
				if (main.allgraphs.get(graphidx).getMot(wordidx) == null
						|| main.allgraphs.get(graphidx).getMot(wordidx).toString().length() == 0)
					continue;
				if (main.allgraphs.get(graphidx).conf >= 3)
					g2.setColor(Color.blue);
				if (SubTreeSelection.isWordSelected(main.allgraphs.get(graphidx), wordidx))
					g2.setColor(Color.orange);
				if (editmode) {
					if (main.editword0>=0) {
						// selection de plusieurs mots
						if ((wordidx>=main.editword&&wordidx<=main.editword0) || (wordidx>=main.editword0&&wordidx<=main.editword)) {
							layout = new TextLayout(main.allgraphs.get(graphidx).getMot(wordidx)
									.toString(), fontedit, frc);
							g2.setColor(Color.red);
							main.xmotedit = pos;
						} else {
							layout = new TextLayout(main.allgraphs.get(graphidx).getMot(wordidx)
									.toString(), font, frc);
						}
					} else {
						// pas de selection de mots
						if (wordidx == main.editword) {
							// affichage du mot courant en rouge
							layout = new TextLayout(main.allgraphs.get(graphidx).getMot(wordidx)
									.toString(), fontedit, frc);
							g2.setColor(Color.red);
							main.xmotedit = pos;
							//						main.scrollRectToVisible(new Rectangle(pos, baseline, 100, 10));
						} else {
							layout = new TextLayout(main.allgraphs.get(graphidx).getMot(wordidx)
									.toString(), font, frc);
						}
					}
				} else {
					layout = new TextLayout(main.allgraphs.get(graphidx).getMot(wordidx)
							.toString(), font, frc);
				}
				layout.draw(g2, pos, baseline);
				g2.setColor(normalcolor);
				int deltax = (int) layout.getAdvance();
				middle[wordidx] = pos + deltax / 2;

				if (main.showpostag) {
					if (main.allgraphs.get(graphidx).getMot(wordidx).getPOS()==null) {
						main.allgraphs.get(graphidx).getMot(wordidx).setPOS("UNK");
					}
					layoutPOSTAG = new TextLayout(
							main.allgraphs.get(graphidx).getMot(wordidx).getPOS(), fontpostag,
							frc);
					layoutPOSTAG.draw(g2, pos, baseline + 30);
					int deltaxpostag = (int) layoutPOSTAG.getAdvance();
					if (deltaxpostag > deltax)
						deltax = deltaxpostag;
				}

				pos += deltax + fontDim * 1.6;
			}

			if (pos > main.getWidth()) {
				main.setSize(pos, 200);
				main.setMinimumSize(new Dimension(pos, 200));
				main.setPreferredSize(new Dimension(pos, 200));
				main.safranPanel.scrollPane.validate();
				main.jf.setVisible(true);
			}

			/*
			 * Affichage des groupes
			 */
			if (main.allgraphs.get(graphidx).groups!=null) {
				int[] groupOff = new int[main.allgraphs.get(graphidx).groups.size()]; // position verticale du groupe, a cause des overlap
				Arrays.fill(groupOff, 0);
				int[] groupwmin = new int[groupOff.length]; // conserve les limites des groupes, pour calculer les overlap
				int[] groupwmax = new int[groupOff.length];
				for (int gi=0;gi<main.allgraphs.get(graphidx).groups.size();gi++) {
					ArrayList<Mot> motsDuGroup=main.allgraphs.get(graphidx).groups.get(gi);
					int wmin = motsDuGroup.get(0).getIndexInUtt()-1;
					if (wmin<firstWordDrawn||wmin>=lastWordDrawn) continue;
					int wmax = motsDuGroup.get(motsDuGroup.size()-1).getIndexInUtt()-1;
					if (wmax<firstWordDrawn||wmax>=lastWordDrawn) continue;
					groupwmin[gi]=wmin; groupwmax[gi]=wmax;
					// est-ce qu'il overlap un groupe precedent ?
					for (int gii=0;gii<gi;gii++) {
						if (wmin<=groupwmin[gii]&&wmax>=groupwmax[gii]&&groupOff[gi]<=groupOff[gii]) groupOff[gi]=groupOff[gii]+12;
						else if (wmin<=groupwmin[gii]&&wmax>=groupwmin[gii]&&groupOff[gi]<=groupOff[gii]) groupOff[gi]=groupOff[gii]+12;
						else if (wmin>=groupwmin[gii]&&wmax<=groupwmax[gii]&&groupOff[gi]<=groupOff[gii]) groupOff[gi]=groupOff[gii]+12;
						else if (wmin<=groupwmax[gii]&&wmax>=groupwmax[gii]&&groupOff[gi]<=groupOff[gii]) groupOff[gi]=groupOff[gii]+12;
					}
					g.drawLine(middle[wmin]-5, baseline+5+groupOff[gi], middle[wmax]+5, baseline+5+groupOff[gi]);
					if (main.showgroupnames) {
						layoutGroupName = new TextLayout(main.allgraphs.get(graphidx).groupnoms.get(gi),fontpostag, frc);
						layoutGroupName.draw(g2, middle[wmin], baseline+15+groupOff[gi]);
					}
				}
			}

			/*
			 * Affichage des liens
			 */
			if (main.showdeps) {
				hmax = 0;
				font = Font.decode("system PLAIN " + (int) ((float) fontDim * 0.8f));
				for (int i = firstWordDrawn; i < lastWordDrawn; i++) {
					int[] deps = main.allgraphs.get(graphidx).getDeps(i);
					Color thisDepColor = normalcolor;
					if (deps.length>1) {
						thisDepColor = Color.green;
					}
					g2.setColor(thisDepColor);
					for (int didx=0;didx<deps.length;didx++) {
						int dep = deps[didx];
						int head = main.allgraphs.get(graphidx).getHead(dep);
						if (head<firstWordDrawn||head>=lastWordDrawn) continue;

						if (SubTreeSelection.isDepSelected(main.allgraphs.get(graphidx), i, head))
							g2.setColor(Color.orange);
						else
							g2.setColor(thisDepColor);

						// System.err.println("head de "+i+" "+head);
						int leftx, rightx, deltai;
						if (head > i) {
							leftx = middle[i];
							rightx = middle[head];
							deltai = head - i;
						} else {
							leftx = middle[head];
							rightx = middle[i];
							deltai = i - head;
						}
						int h = deltai * fontDim;
						if (h > 100)
							h = 100;
						int larg = rightx - leftx;
						if (h > hmax)
							hmax = h;

						if (editmode && i == main.editword && main.editlink)
							g2.setColor(Color.RED);

						int ang = (int)(900f/(float)h);
						if (head > i) {
							// le head est a droite
							g.drawArc(leftx, baseline - fontDim - h, larg, h, ang, 180-ang);
							int[] xx = {leftx,leftx-3,leftx+3};
							int[] yy = {baseline-fontDim-h/2,baseline-fontDim-h/2-8,baseline-fontDim-h/2-8};
							g.fillPolygon(xx,yy,3);
							//						g.drawArc(rightx - 2, baseline - fontDim - h / 2, 4, 4, 0, 360);
						} else {
							// le head est a gauche
							g.drawArc(leftx, baseline - fontDim - h, larg, h, 0, 180-ang);
							int[] xx = {rightx,rightx-3,rightx+3};
							int[] yy = {baseline-fontDim-h/2,baseline-fontDim-h/2-8,baseline-fontDim-h/2-8};
							g.fillPolygon(xx,yy,3);
							//						g.drawArc(leftx - 2, baseline - fontDim - h / 2, 4, 4, 0, 360);
						}

						// ecrire le label de la dependence:
						String label = main.allgraphs.get(graphidx).getDepLabel(dep);
						if (label == null)
							continue;
						if (main.editlink && i == main.curdep)
							layout = new TextLayout(label, font, frc);
						else
							layout = new TextLayout(label, font, frc);
						int lenx = (int) layout.getAdvance();
						layout.draw(g2, (leftx + rightx - lenx) / 2, baseline - fontDim - h);

						if (editmode && i == main.editword && main.editlink)
							g2.setColor(thisDepColor);
					}
				}
			}
			g2.setColor(normalcolor);

			/*
			 * affichage selection
			 */
			if (main.seldeb >= 0) {
				int a = main.seldeb, b = main.curgraph;
				if (a > b) {
					a = main.curgraph;
					b = main.seldeb;
				}
				if (graphidx >= a && graphidx <= b) {
					g2.setColor(Color.orange);
					g2.fillRect(0, baseline - main.VERTBASELINE / 2, 10, main.VERTBASELINE);
					g2.setColor(normalcolor);
				}
			}

			return pos;
		} catch (Exception e) {
			return 10;
		}
	}
}
