package jsafran.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.maltparser.MaltConsoleEngine;
import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.options.OptionManager;
import org.maltparser.ml.lib.MaltLiblinearModel;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

/**
 * Attention: il y a des bugs dans MaltParser.
 * 
 * Notamment lorsqu'on utiliser plusieurs containers (?), il semble que le CombinedTableContainer se retrouve avec des cachedSymbol qui contiennent
 * des chars nulls, et ca fait planter KBestList.add(ActionCode)
 * 
 * 
 * .... ==> version plus simple avec experiments
 * 
 * @author xtof
 *
 */
public class Malt4Jsafran {

	public Malt4Jsafran() {
	}

	public void train(String modelName, List<DetGraph> gs) {
//		try {
			GraphIO.saveConLL06(gs, "tmpmalt.conll");
			
			File f = new File(modelName+".mco");
			if (f.exists()) f.delete();
			
			try {
				String cmd = "java -jar ../maltparser/dist/maltparser-1.7.2/maltparser-1.7.2.jar -c "+modelName+" -i tmpmalt.conll -m learn -ne true -nr false";
				String[] cmds = cmd.split(" ");
				ProcessBuilder pb = new ProcessBuilder(cmds);
				// not 1.6 compatible
//				pb.redirectOutput(new File("tmpmaltstdout.log"));
				Process p = pb.start();
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
//			new MaltParserService(0).runExperiment("-c "+modelName+" -i tmpmalt.conll -m learn -ne true -nr false");
//		} catch (MaltChainedException e) {
//			System.err.println("MaltParser exception : " + e.getMessage());
//		}
	}

	public void parse(String modelName, List<DetGraph> gs) {
//		try {
			GraphIO.saveConLL06(gs, "tmpmalt.conll");
			
			try {
				String cmd = "java -jar ../maltparser/dist/maltparser-1.7.2/maltparser-1.7.2.jar -c "+modelName+" -i tmpmalt.conll -o tmpmaltout.conll -m parse";
				String[] cmds = cmd.split(" ");
				ProcessBuilder pb = new ProcessBuilder(cmds);
				// not 1.6 compatible
//				pb.redirectOutput(new File("tmpmaltstdout.log"));
				Process p = pb.start();
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
//			MaltLiblinearModel.saveAndResetScores();
//			new MaltParserService(0).runExperiment("-c "+modelName+" -i tmpmalt.conll -o tmpmaltout.conll -m parse");
//			System.out.println("after XP "+MaltLiblinearModel.scores.size()+" "+MaltLiblinearModel.uttStartIdx.size()+" "+gs.size());
			List<DetGraph> gg = GraphIO.loadConll06("tmpmaltout.conll", false);
			for (int i=0;i<gg.size();i++) {
				JSafran.projectOnto(gg.get(i), gs.get(i), true, false, false);
//				gs.get(i).conf=(float)MaltLiblinearModel.getScoreUtt(i);
			}

			try {
				BufferedReader f = new BufferedReader(new FileReader("tmpmaltstdout.log"));
				for (int i=0;i<gs.size();i++) {
					float sc=0;
					int j=0;
					for (;;) {
						String s=f.readLine();
						if (s.startsWith("DETMALTNEWUTT")) break;
						int k=s.indexOf(' ');
						sc+=Float.parseFloat(s.substring(k+1));
						++j;
					}
					if (j==0) sc=0;
					else sc/=(float)j;
					gs.get(i).conf=sc;
				}
				f.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

//			MaltLiblinearModel.scores.clear();
//		} catch (MaltChainedException e) {
//			System.err.println("MaltParser exception : " + e.getMessage());
//		}
	}

	public void parse(String modelName, DetGraph g) {
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		gs.add(g);
		parse(modelName,gs);
	}

	static void test2() {
		GraphIO gio = new GraphIO(null);
		Malt4Jsafran parser = new Malt4Jsafran();

		if (true) {
			List<DetGraph> gs = gio.loadAllGraphs("../jsafran/tmp.xml");
			parser.train("tmpmod", gs);
		}
		if (true) {
			List<DetGraph> gs = gio.loadAllGraphs("../jsafran/test2009.xml");
			parser.parse("tmpmod", gs);
			gio.save(gs, "res1.xml");
		}

		if (true) {
			List<DetGraph> gs = gio.loadAllGraphs("../jsafran/test2009.xml");
			parser.train("tmpmod", gs);
		}
		
		{
			List<DetGraph> gs = gio.loadAllGraphs("../jsafran/test2009.xml");
			parser.parse("tmpmod", gs);
			gio.save(gs, "res2.xml");
		}
	}


	public static void main(String args[]) {
		test2();
	}
}
