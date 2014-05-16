package corpus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import jsafran.DetGraph;
import jsafran.GraphProcessor;
import jsafran.SyntaxGraphs;

public class P7corpus implements GraphProcessor {

	final String corpdir = "../../svn/aligne/srl/ressources/P7_DEPENDENCY_TREEBANK/ftb.dep.conll.v6.gz";

	ArrayList<DetGraph> graphs = new ArrayList<DetGraph>();
	BufferedReader f;
	
	public List<DetGraph> getTest() {
		if (graphs.size()>0) {
			int debtrain = graphs.size()/10;
			return graphs.subList(0,debtrain);
		}
		try {
			f = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(corpdir)),Charset.forName("ISO-8859-1")));
			SyntaxGraphs reader = new SyntaxGraphs(this);
			reader.parse(f, SyntaxGraphs.CONLL);
			// 10 % reserves au test
			int debtrain = graphs.size()/10;
			return graphs.subList(0,debtrain);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<DetGraph> getTrain() {
		if (graphs.size()>0) {
			int debtrain = graphs.size()/10;
			return graphs.subList(debtrain,graphs.size());
		}
		try {
			f = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(corpdir)),Charset.forName("ISO-8859-1")));
			SyntaxGraphs reader = new SyntaxGraphs(this);
			reader.parse(f, SyntaxGraphs.CONLL);
			// 10 % reserves au test
			int debtrain = graphs.size()/10;
			return graphs.subList(debtrain,graphs.size());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void processGraph(DetGraph graph) {
		graphs.add(graph);
	}

	@Override
	public void terminate() {
		try {
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
