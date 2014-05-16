package jsafran;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

/**
 * convertit en CONLL mais verifie aussi les modifications de Syntex (ce sont des bugs de Syntex !)
 * qui peuvent amener des erreurs dans le scoring:
 * dans ce cas, je remplace les mots de la phrase par des mot inconnus (qui ne seront pas scores
 * par Syntex en terme de CM)
 * 
 * @author xtof
 *
 */
public class Syntex2conll implements GraphProcessor {
	
	public DetGraph cascadeGraph = null;
	
	PrintWriter fout;
	public int sentidx = -1;
	int cursent=-1;
	
	public Syntex2conll(PrintWriter f) {
		fout = f;
	}
	public void terminate() {
		if (cursent>=0)
			fout.write("\n");
		fout.close();
	}
	private String sourceHead = "UNK";
	public void processGraph(DetGraph detgraph) {
		if (cursent>=0)
			fout.println();
		cursent++;
		if (sentidx>=0&&cursent!=sentidx) return;
		for (int i=0;i<detgraph.getNbMots();i++) {
			Mot m = detgraph.getMot(i);
			String lemme = m.getLemme();
			String forme = m.getForme();
			String tag = m.getPOS();

			if (forme==null||forme.length()==0) {
				forme = "UNK"; lemme = "UNK"; tag = "UNK";
			} else if (lemme==null||lemme.length()==0)
				lemme = ""+forme;
			if (tag==null||tag.length()==0) tag="UNK";
			
			if (cascadeGraph!=null) {
				int dep = cascadeGraph.getDep(i);
				if (dep>=0) {
					sourceHead = cascadeGraph.getDepLabel(dep);
				} else {
					sourceHead="NODEP";
				}
			} else sourceHead=tag;
			
			String feat = "_";
			// j'ajoute les groupes en FEATS:
			int[] g = detgraph.getGroups(i);
			if (g!=null&&g.length>0) {
				List<Mot> motsdugroupe = detgraph.groups.get(g[0]);
				if (motsdugroupe.size()>0&&motsdugroupe.get(0)==m)
					feat = "B"+detgraph.groupnoms.get(g[0]);
				else
					feat = "I"+detgraph.groupnoms.get(g[0]);
			}
			
			int[] deps = detgraph.getDeps(i);
			if (deps.length<=0) {
				fout.println((i+1)+"\t"+forme+"\t"+lemme+"\t"+sourceHead+"\t"+tag+"\t"+feat+"\t0\tROOT\t"+"_\t"+"_");
			} else if (deps.length>1){
				// cas multiple heads
				solveConflicts(detgraph,i,deps);
			} else {
				int dep = deps[0];
				String rel = detgraph.getDepLabel(dep);
				int head = detgraph.getHead(dep);
				Mot mhead = detgraph.getMot(head);
				int mheadidx;
				if (mhead==null) mheadidx = 0;
				else mheadidx=mhead.getIndexInUtt();
				fout.println((i+1)+"\t"+forme+"\t"+lemme+"\t"+sourceHead+"\t"+tag+"\t"+feat+"\t"+mheadidx+"\t"+rel+"\t"+"_\t"+"_");
			}
		}
	}
	
	private void printrel(DetGraph detgraph, int dep, int gov) {
		String rel = detgraph.getDepLabel(dep);
		int head = detgraph.getHead(dep);
		Mot mhead = detgraph.getMot(head);
		int mheadidx;
		if (mhead==null) mheadidx = 0;
		else mheadidx=mhead.getIndexInUtt();
		Mot m = detgraph.getMot(gov);
		String lemme = m.getLemme();
		String forme = m.getForme();
		String tag = m.getPOS();
		fout.println((gov+1)+"\t"+forme+"\t"+lemme+"\t"+sourceHead+"\t"+tag+"\t"+"_\t"+mheadidx+"\t"+rel+"\t"+"_\t"+"_");
	}
	
	private void solveConflicts(DetGraph detgraph, int gov, int[] deps) {
		// suppression des GRP
		int ndeps = deps.length;
		for (int i=0;i<ndeps;i++) {
			String lab = detgraph.getDepLabel(deps[i]);
			if (lab.startsWith("GRP")) {
				int dd = deps[i];
				deps[i] = deps[--ndeps];
				i--;
				detgraph.removeDep(dd);
			}
		}
		if (ndeps==0) return;
		if (ndeps==1) printrel(detgraph,deps[0],gov);
		else {
			int sol = 2;
			switch (sol) {
			case 1:
				printrel(detgraph,deps[0],gov);
				return;
			case 2:
				int ibest=0, posbest=detgraph.getHead(deps[0])-gov;
				for (int i=1;i<ndeps;i++) {
					int pos = detgraph.getHead(deps[i])-gov;
					if (Math.abs(pos)<Math.abs(posbest) || pos<posbest) {
						ibest=i; posbest=pos;
					}
				}
				printrel(detgraph,deps[ibest],gov);
				return;
			case 3:
				// ajoute lien 
//				detgraph.ajoutDep(type1, gov, head1);
//				detgraph.ajoutDep("MH"+type2, head2, gov);
				break;
			}
		}
	}
	
	public static void main(String args[]) throws IOException {
		// TODO: Malt peut-il marcher en UTF ??
//		Syntex2conll n = new Syntex2conll(new PrintWriter(new OutputStreamWriter(new FileOutputStream("output.conll"),Charset.forName("ISO-8859-1"))));
		Syntex2conll n = new Syntex2conll(new PrintWriter(new OutputStreamWriter(new FileOutputStream("output.conll"),Charset.forName("UTF-8"))));
		SyntaxGraphs m = new SyntaxGraphs(n);
		m.parse(args[0]);
	}
}
