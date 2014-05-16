package jsafran;

import java.awt.FlowLayout;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import utils.ErrorsReporting;
import utils.FileUtils;

public class GroupManager {
	public static void delgroup(DetGraph g, int wordidx) {
		int[] gs = g.getGroups(wordidx);
		Arrays.sort(gs);
		for (int i=gs.length-1;i>=0;i--) {
			g.groupnoms.remove(gs[i]);
			g.groups.remove(gs[i]);
		}
	}
	public static void saveGroups(java.util.List<DetGraph> gs) {
		try {
			// nb de groupes differents
			HashSet<String> groups = new HashSet<String>();
			for (int i=0;i<gs.size();i++) {
				DetGraph g = gs.get(i);
				if (g.groupnoms!=null)
					groups.addAll(g.groupnoms);
			}
			
			for (String gr : groups) {
				PrintWriter fout = FileUtils.writeFileUTF("groups."+gr+".tab");
				for (int i=0;i<gs.size();i++) {
					DetGraph g = gs.get(i);
					boolean isInGroup = false;
					for (int j=0;j<g.getNbMots();j++) {
						String lab = "NO";
						int[] grps = g.getGroups(j);
						boolean grfound = false;
						if (grps!=null&&grps.length>0) {
							for (int k=0;k<grps.length;k++)
								if (g.groupnoms.get(grps[k]).equals(gr)) {
									grfound=true;
									break;
								}
						}
						if (!isInGroup&&grfound) lab=gr+"B";
						else if (isInGroup&&grfound) lab=gr+"I";
						isInGroup=grfound;
						fout.println(g.getMot(j).getForme()+"\t"+g.getMot(j).getPOS()+"\t"+lab);
					}
					fout.println();
				}
				fout.close();
			}
			ErrorsReporting.report("groups saved in groups.*.tab");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void showGroups(final DetGraph g) {
		System.out.println("GROUP manager");
		if (g.groups==null || g.groups.size()==0) return;
		final JFrame jf = new JFrame("Groups manager");
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.getContentPane().setLayout(new FlowLayout());
		
		final List glist = new List();
		for (int i=0;i<g.groups.size();i++) {
			String mots = "";
			for (int j=0;j<g.groups.get(i).size();j++) {
				mots += g.groups.get(i).get(j).getForme()+" ";
			}
			if (g.groupnoms.size()>=i)
				glist.add(g.groupnoms.get(i)+" "+mots);
			else
				glist.add("UNK "+mots);
		}
		
		final JScrollPane sglist = new JScrollPane(glist);
		
		JButton delbutton = new JButton("del group");
		delbutton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int idx = glist.getSelectedIndex();
				glist.remove(idx);
				g.groups.remove(idx);
				if (g.groupnoms.size()>=idx)
					g.groupnoms.remove(idx);
				jf.repaint();
			}
		});
		
		jf.getContentPane().add(delbutton);
		jf.getContentPane().add(sglist);
		jf.setSize(600, 600);
		jf.setVisible(true);
	}
}
