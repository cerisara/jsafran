package jsafran.constituants;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import jsafran.DetGraph;
import jsafran.JSafran;

public class PennTreeBank {
	JSafran m;
	
	public PennTreeBank(JSafran main) {
		m=main;
	}
	
	public void saveCurLevel() {
		JFileChooser jfc = new JFileChooser("./");
		int ret = jfc.showSaveDialog(jfc);
		if (ret==JFileChooser.APPROVE_OPTION) {
			File f = jfc.getSelectedFile();
			String nom=f.getAbsolutePath();
			save(m.allgraphs,nom);
		}
	}
	
	public static void save(List<DetGraph> gs, String outfile) {
		try {
			PrintWriter f = new PrintWriter(new FileWriter(outfile));
			String off="";
			for (int gi=0;gi<gs.size();gi++) {
				DetGraph g = gs.get(gi);
				if (g.getNbMots()==0) continue;
				f.println(off+"( (S");
				off+="    ";
				off+="    ";
				
				ArrayList<Integer>[] groupstarts = new ArrayList[g.getNbMots()];
				ArrayList<Integer>[] groupends = new ArrayList[g.getNbMots()];
				for (int i=0;i<groupstarts.length;i++) {
					groupstarts[i] = new ArrayList<Integer>();
					groupends[i] = new ArrayList<Integer>();
				}
				if (g.groups!=null)
					for (int i=0;i<g.groups.size();i++) {
						int w=g.groups.get(i).get(0).getIndexInUtt()-1;
						groupstarts[w].add(i);
						w=g.groups.get(i).get(g.groups.get(i).size()-1).getIndexInUtt()-1;
						groupends[w].add(i);
					}
				// sort groups by longest first
				for (int i=0;i<g.getNbMots();i++) {
					ArrayList<Integer> sorted = new ArrayList<Integer>();
					ArrayList<Integer> lens = new ArrayList<Integer>();
					for (int j : groupstarts[i]) {
						int len = g.groups.get(j).size();
						boolean inserted=false;
						for (int k=0;k<sorted.size();k++)
							if (len>lens.get(k)) {
								inserted=true;
								sorted.add(k, j);
								lens.add(k,len);
								break;
							}
						if (!inserted) {
							sorted.add(j);
							lens.add(len);
						}
					}
					groupstarts[i]=sorted;
				}
				// sort end-groups by shortest first
				for (int i=0;i<g.getNbMots();i++) {
					ArrayList<Integer> sorted = new ArrayList<Integer>();
					ArrayList<Integer> lens = new ArrayList<Integer>();
					for (int j : groupends[i]) {
						int len = g.groups.get(j).size();
						boolean inserted=false;
						for (int k=0;k<sorted.size();k++)
							if (len<lens.get(k)) {
								inserted=true;
								sorted.add(k, j);
								lens.add(k,len);
								break;
							}
						if (!inserted) {
							sorted.add(j);
							lens.add(len);
						}
					}
					groupends[i]=sorted;
				}
				
				for (int i=0;i<g.getNbMots();i++) {
					for (int j:groupstarts[i]) {
						f.println(off+"("+g.groupnoms.get(j)+" ");
						off+="    ";
					}
					String wf = g.getMot(i).getForme();
					wf=wf.replace('(', '-');
					wf=wf.replace(')', '-');
					wf=wf.trim();
					f.print(off+"("+g.getMot(i).getPOS()+" "+wf+") ");
					for (int j:groupends[i]) {
						f.print(")");
						off=off.substring(4);
					}
					f.println();
				}
				f.println(off+"))");
				off=off.substring(8);
			}
			f.close();
			System.out.println("PTB saved !");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
