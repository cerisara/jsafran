package jsafran.correction;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.Mot;

/**
 * Je veux utiliser cette classe pour faire de l'Interactive Machine Learning
 * en projetant une annotation manuelle sur les instances les plus proches, au sens de
 * distances d'arbre.
 * Pour cela, il faut definir ces distances predefinies (c'est ce que fait Projection)
 * ce qui permet de trouver toutes les instances qui sont a une distance fixe de l'exemple
 * annoté.
 * TODO: pour pouvoir appliquer cela a l'annotation d'EN, il faut aussi construire des
 * transfos sur les sequences.
 * TODO: il faut encore sauvegarder toutes les structures corrigees manuellement ainsi que
 * les distances utilisees pour rechercher les exemples proches et le taux de projection
 * reussie/verifiee par le user (pour pouvoir construire un modele MBL probabiliste correcteur ensuite)
 * 
 * @author cerisara
 *
 */
public class Projection {
	Box predefs = null;
	JSafran js;

	public Projection(JSafran main) {
		js=main;
	}

	/**
	 * je suppose que la partie "search" matche la sequence de mots a annoter
	 * @param minw
	 * @param maxw
	 * @return
	 */
	private String getTransfo4seq(int minw, int maxw) {
		String s="";
		DetGraph g = js.allgraphs.get(js.curgraph);
		// suppression systematique des groupes
		s+="-gdep0,dep"+(maxw-minw)+" ";
		// ajoute des groupes ?
		if (g.groups!=null) {
			for (int i=g.groups.size()-1;i>=0;i--) {
				List<Mot> motsdugroupe = g.groups.get(i);
				if (motsdugroupe.get(0).getIndexInUtt()-1==minw &&
						motsdugroupe.get(motsdugroupe.size()-1).getIndexInUtt()-1==maxw) {
					s+="g=dep0,dep"+(maxw-minw)+","+g.groupnoms.get(i)+" ";
				}
			}
		}
		// suppression systematique des deps qui ont un lien interne
		for (int i=minw;i<=maxw;i++) {
			int d=g.getDep(i);
			if (d>=0) {
				int h=g.getHead(d);
				if (h>=minw&&h<=maxw) {
					s+="-dep"+(i-minw)+" ";
				}
			}
		}
		// ajout des deps internes
		for (int i=minw;i<=maxw;i++) {
			int d=g.getDep(i);
			if (d>=0) {
				int h=g.getHead(d);
				if (h>=minw&&h<=maxw) {
					s+="dep"+(i-minw)+",dep"+(h-minw)+","+g.getDepLabel(d)+" ";
				}
			}
		}
		s=s.trim();
		if (s.length()>0) s="=> "+s;
		return s;
	}
	private String getTransfo4head(int minw, int maxw) {
		String s="";
		DetGraph g = js.allgraphs.get(js.curgraph);
		// suppression systematique des groupes
		s+="-gdep0,dep"+(maxw-minw)+" ";
		// ajoute des groupes ?
		if (g.groups!=null) {
			for (int i=g.groups.size()-1;i>=0;i--) {
				List<Mot> motsdugroupe = g.groups.get(i);
				if (motsdugroupe.get(0).getIndexInUtt()-1==minw &&
						motsdugroupe.get(motsdugroupe.size()-1).getIndexInUtt()-1==maxw) {
					s+="g=dep0,dep"+(maxw-minw)+","+g.groupnoms.get(i)+" ";
				}
			}
		}
		s=s.trim();
		if (s.length()>0) s="=> "+s;
		return s;
	}
	
	public boolean allSecTriplets = false;
	
	private void createPredefs() {
		predefs = Box.createHorizontalBox();
		final JCheckBox allSecondaryTriplets = new JCheckBox("All triplets 2,3,...");
		final ButtonGroup seqGroup = new ButtonGroup();
		final JCheckBox seq1 = new JCheckBox("Seq1");
		seqGroup.add(seq1);
		final JCheckBox seq2 = new JCheckBox("Seq2");
		seqGroup.add(seq2);
		
		predefs.add(Box.createHorizontalGlue());
		predefs.add(allSecondaryTriplets);
		predefs.add(Box.createHorizontalGlue());
		predefs.add(seq1);
		predefs.add(Box.createHorizontalGlue());
		predefs.add(seq2);
		predefs.add(Box.createHorizontalGlue());

		allSecondaryTriplets.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				allSecTriplets=allSecondaryTriplets.isSelected();
			}
		});
		seq1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String pref="";
				if (seq1.isSelected()) {
					int minw = js.editword;
					int maxw = minw;
					if (js.editword0>=0) {
						if (js.editword0>maxw) maxw=js.editword0;
						else if (js.editword0<minw) minw=js.editword0;
						for (int w=minw;w<=maxw;w++) pref+="f="+js.allgraphs.get(js.curgraph).getMot(w).getForme()+",_,_ ";
						for (int w=minw+1;w<=maxw;w++) pref+="dep"+(w-minw)+"=dep"+(w-minw-1)+"+1 ";
					} else {
						pref = "f="+js.allgraphs.get(js.curgraph).getMot(js.editword).getForme()+",_,_ ";
					}
//					pref+=getTransfo4seq(minw,maxw);
				}
				ta.setText(pref);
				ta.repaint();
			}
		});
		seq2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String pref="";
				if (seq2.isSelected()) {
					int minw = js.editword;
					int maxw = minw+1;
					for (int w=minw;w<=maxw;w++) {
						if (w>=js.allgraphs.get(js.curgraph).getNbMots())
							pref+="f="+js.allgraphs.get(js.curgraph).getMot(minw).getForme()+",_,_ ";
						else
							pref+="f="+js.allgraphs.get(js.curgraph).getMot(w).getForme()+",_,_ ";
					}
					for (int w=minw+1;w<=maxw;w++) pref+="dep"+(w-minw)+"=dep"+(w-minw-1)+"+1 ";
				}
				ta.setText(pref);
				ta.repaint();
			}
		});
	}

	JTextArea ta = new JTextArea(15,30);
	public String showInputDialog(final String message, final String prefill)
	{
		if (predefs==null) createPredefs();

		String data = null;
		class GetData extends JDialog implements ActionListener
		{
			JScrollPane sta = new JScrollPane(ta);
			JButton btnOK = new JButton("   OK   ");
			JButton btnCancel = new JButton("Cancel");
			String str = null;
			public GetData(String prefill)
			{
				setModal(true);
				if (prefill!=null) ta.setText(prefill);
				getContentPane().setLayout(new BorderLayout());
				setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				setLocation(400,300);
				Box bup = Box.createVerticalBox();
				bup.add(new JLabel(message));
				bup.add(predefs);
				getContentPane().add(bup,BorderLayout.NORTH);
				getContentPane().add(sta,BorderLayout.CENTER);
				JPanel jp = new JPanel();
				btnOK.addActionListener(this);
				btnCancel.addActionListener(this);
				jp.add(btnOK);
				jp.add(btnCancel);
				getContentPane().add(jp,BorderLayout.SOUTH);
				pack();
				setVisible(true);
			}
			public void actionPerformed(ActionEvent ae)
			{
				if(ae.getSource() == btnOK) str = ta.getText();
				dispose();
			}
			public String getData(){return str;}
		}
		data = new GetData(prefill).getData();
		return data;
	}
}
