package jsafran;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;

/**
 * charge le Malt Parser au debut, puis
 * l'appelle a chaque nouvelle phrase
 * 
 * @author cerisara
 *
 */
public class MaltParserThread {
	static String wdir = "/tmp";
	String[] wdirs;
	String maltargs;

	private MaltParserService maltparser[] = null;

	public MaltParserThread(String modfile, int nthreads, int mode) {

		try {
			maltparser = new MaltParserService[nthreads];
			for (int i=0;i<nthreads;i++) maltparser[i]=new MaltParserService(i);

			File curdir = (new File(modfile)).getParentFile();
			if (curdir==null) curdir = new File(".");

			System.out.println("maltparserthread init "+modfile+" curdir "+curdir.getAbsolutePath());

			final String maltargsLINEAR   = "-u file:///"+curdir.getAbsolutePath()+"/"+modfile+".mco -l liblinear -m parse";
			final String maltargsSVMSPLIT = "-u file:///"+curdir.getAbsolutePath()+"/"+modfile+".mco -l libsvm -d POSTAG -s Input[0] -T 1000 -m parse";
			final String maltargsSVM      = "-u file:///"+curdir.getAbsolutePath()+"/"+modfile+".mco -l libsvm -m parse";

			switch (mode) {
			case MaltParserTrainer.SVM: maltargs=maltargsSVM;
			break;
			case MaltParserTrainer.SVMSPLIT: maltargs=maltargsSVMSPLIT;
			break;
			case MaltParserTrainer.LINEAR: maltargs=maltargsLINEAR;
			break;
			default:
				System.err.println("ERROR: training mode unknown "+mode);
				return;
			}

			for (int i=0;i<nthreads;i++) {
				System.out.println("initialize parser thread "+i);
				maltparser[i].initializeParserModel(maltargs);
			}
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
	}

	public void stopParsers() {
		try {
			for (int i=0;i<maltparser.length;i++) maltparser[i].terminateParserModel();
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
	}

	DetGraph res;

	/**
	 * ne fait pas de retagging !!
	 * 
	 * peut etre appele plusieurs fois dans des threads separes
	 * 
	 */
	public DetGraph parseWithConstraints(DetGraph g, boolean withConstraints) {
		return parseWithConstraints(g,withConstraints,false);
	}
	public DetGraph parseWithConstraints(DetGraph g, boolean withConstraints, boolean quiet) {
		res = g.getSubGraph(0);
		res.clearDeps();
		parse(0,res);
		return res;
	}
	private void parse(int container, DetGraph g) {
		g.clearDeps();
		String[] tokens=null;
		ArrayList<Integer> wordmap = new ArrayList<Integer>();
		if (JSafran.delPonctBeforeMalt) {
			System.out.println("del ponct before parsing "+g.cursent);
			final String ponct = ",;:!?./§\"";
			ArrayList<String> toks = new ArrayList<String>();
			for (int i=0;i<g.getNbMots();i++) {
				String s = g.getMot(i).getForme();
				boolean onlyponct=true;
				for (int k=0;k<s.length();k++) {
					char c = s.charAt(k);
					if (ponct.indexOf(c)<0) {onlyponct=false; break;}
				}
				if (!onlyponct) {
					wordmap.add(i);
					toks.add(s);
				}
			}
			tokens = new String[toks.size()];
			for (int j=0;j<tokens.length;j++) {
				int i=wordmap.get(j);
				tokens[j]=(j+1)+"\t"+g.getMot(i).getForme()+"\t"+g.getMot(i).getLemme()+"\t"+g.getMot(i).getPOS()+"\t"+g.getMot(i).getPOS();
			}
		} else {
			tokens = new String[g.getNbMots()];
			for (int i=0;i<tokens.length;i++) {
				wordmap.add(i);
				tokens[i]=(i+1)+"\t"+g.getMot(i).getForme()+"\t"+g.getMot(i).getLemme()+"\t"+g.getMot(i).getPOS()+"\t"+g.getMot(i).getPOS();
			}
		}
		try {
			String[] outtoks = maltparser[container].parseTokens(tokens);
			for (int i=0;i<tokens.length;i++) {
				String[] cols = outtoks[i].split("\t");
				int head = Integer.parseInt(cols[5])-1;
				if (head>=0) g.ajoutDep(cols[6], wordmap.get(i), wordmap.get(head));
			}
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
	}
	public void parseList(final List<DetGraph> gs) {
		Thread[] parsers = new Thread[maltparser.length];
		final ArrayBlockingQueue<Integer> todo = new ArrayBlockingQueue<Integer>(parsers.length*2);
		for (int i=0;i<parsers.length;i++) {
			final int j=i;
			parsers[i]=new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						for (;;) {
							int n = todo.take();
							if (n<0) {
								todo.put(-1);
								break;
							}
							DetGraph g = gs.get(n);
							parse(j,g);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			},"malt thread "+i);
			parsers[i].start();
		}

		try {
			for (int i=0;i<gs.size();i++)
				todo.put(i);
			todo.put(-1);
			for (int i=0;i<parsers.length;i++) parsers[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * calcule le posterior cumule du parsing
	 */
	public float getScore() {
		return 0;
	}

	// elimine les contraintes non-projectives
	public void checkProjectivity(DetGraph g) {
		boolean changed = false;
		for (;;) {
			changed=false;
			ArrayList<Integer> left = new ArrayList<Integer>();
			ArrayList<Integer> right = new ArrayList<Integer>();
			for (int i=0;i<g.getNbMots();i++) {
				int dep = g.getDep(i);
				if (dep>=0) {
					int a = i, b=g.getHead(dep);
					if (a>b) {a=b;b=i;}
					int j;
					for (j=0;j<left.size();j++) {
						if (a<left.get(j)&&b>left.get(j)&&b<right.get(j)) {
							// projectif !
							System.err.println("REMOVE NONPROJ CONSTRAINT1");
							g.removeDep(dep);
							changed=true;
							break;
						} else if (a>left.get(j)&&a<right.get(j)&&b>right.get(j)) {
							// projectif !
							System.err.println("REMOVE NONPROJ CONSTRAINT2");
							g.removeDep(dep);
							changed=true;
							break;
						}
					}
					if (j>=left.size()) {
						left.add(a); right.add(b);
					}
					// verifie aussi les formes A B C (A=head(C) B=head(A))
					int head = g.getHead(dep);
					int dep2 = g.getDep(head);
					if (dep2>=0) {
						int head2 = g.getHead(dep2);
						if (Math.abs(i-head2)<Math.abs(i-head) && Math.signum(i-head2)==Math.signum(head2-head)) {
							System.err.println("REMOVE NONPROJ CONSTRAINT3 "+i+" "+head+" "+g.getHead(dep2));
							g.removeDep(dep);
							changed=true;
						} else {
							// verifier les cycles
							if (head2==i) {
								System.err.println("REMOVE CYCLE");
								g.removeDep(dep);
								changed=true;
							}
						}
					}
				} else {
					// lie au ROOT: il ne faut pas traverser une autre liaison ?
				}
			}
			if (!changed) break;
		}
	}

	public static void main(String args[]) {

		String modfile = Config.mod4maltparser;
		final int mode = MaltParserTrainer.LINEAR;
		int nthread = 1;
		int bin=0,nbins=1;
		boolean constrained = true;

		for (int a=0;a<args.length;a++) {
			if (args[a].equals("-parse")) {
				MaltParserThread parser = new MaltParserThread(modfile,nthread,mode);
				GraphIO gio = new GraphIO(null);
				List<DetGraph> gs = gio.loadAllGraphs(args[a+1]);
				PrintWriter fout = gio.saveEntete("output"+bin+".xml");
				int blocsize=gs.size()/nbins;
				int firstgraph=blocsize*bin;
				int lastGraphExcluded=blocsize*(bin+1);
				if (bin==nbins-1) lastGraphExcluded=gs.size();
				for (int i=firstgraph;i<lastGraphExcluded;i++) {
					System.out.println("sentence "+i+"/"+gs.size());
					DetGraph gg = parser.parseWithConstraints(gs.get(i),constrained);
					gg.conf=parser.getScore();
					gg.save(fout);
				}
				parser.stopParsers();
				fout.close();
			} else if (args[a].equals("-noconstraints")) {
				constrained=false;
			} else if (args[a].equals("-mod")) {
				modfile = args[++a];
			} else if (args[a].equals("-bin")) {
				bin = Integer.parseInt(args[++a]);
				nbins = Integer.parseInt(args[++a]);
			} else if (args[a].equals("-nthreads")) {
				nthread = Integer.parseInt(args[++a]);
			} else if (args[a].equals("-wdir")) {
				MaltParserThread.wdir = args[++a];
			}
		}
	}
}
