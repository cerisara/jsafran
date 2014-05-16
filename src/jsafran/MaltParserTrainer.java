package jsafran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.maltparser.MaltParserService;

public class MaltParserTrainer {

	public final static int SVM = 0;
	public final static int SVMSPLIT = 1;
	public final static int LINEAR = 2;
	public final static int CASCADE = 3;

	public List<DetGraph> cascadeGraphs = null;

	public MaltParserTrainer() {
	}

	public void train(String outdir, String models, List<DetGraph> graphs, int mode) {
		train(outdir,models,graphs,mode,true);
	}
	public void train(String outdir, String models, List<DetGraph> graphs, int mode, boolean retag) {

		System.out.println("START TRAIN");

		final String maltargsLINEAR   = "-c "+models+" -l liblinear -m learn -i output.conll";
		final String maltargsSVMSPLIT   = "-c "+models+" -l libsvm -d POSTAG -s Input[0] -T 1000 -m learn -i output.conll";
		final String maltargsSVM   = "-c "+models+" -l libsvm -m learn -i output.conll";

		String maltargs;
		switch (mode) {
		case SVM: maltargs=maltargsSVM;
		break;
		case SVMSPLIT: maltargs=maltargsSVMSPLIT;
		break;
		case LINEAR: maltargs=maltargsLINEAR;
		break;
		default:
			System.err.println("ERROR: training mode unknown "+mode);
			return;
		}

		try {
			if (retag)
				System.out.println("retag + convert to conll..");
			else
				System.out.println("convert to conll..");
			final Syntex2conll conllwriter = new Syntex2conll(new PrintWriter(new OutputStreamWriter(new FileOutputStream("output.conll"),Charset.forName("UTF-8"))));
			for (int i=0;i<graphs.size();i++) {
				if (retag)
					POStagger.tag(graphs.get(i));
				if (mode==CASCADE)
					conllwriter.cascadeGraph=cascadeGraphs.get(i);
				conllwriter.processGraph(graphs.get(i));
			}
			conllwriter.terminate();
			
			new MaltParserService(0).runExperiment(maltargs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void train(String outdir, String models, File trainingDir, int mode) {
		File[] fichs = trainingDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String s = name.toLowerCase();
				return s.endsWith(".xml")||s.endsWith(".conll");
			}
		});
		ArrayList<String> ff = new ArrayList<String>();
		System.out.println("training with files:");
		for (int i=0;i<fichs.length;i++) {
			String f = fichs[i].getAbsolutePath();
			System.out.println(f);
			ff.add(f);
		}
		train(outdir,models,ff,mode);
	}
	public void train(String outdir, String models, ArrayList<String> trainxmll, int mode) {
		final ArrayList<DetGraph> graphs = new ArrayList<DetGraph>();
		for (int i=0;i<trainxmll.size();i++) {
			SyntaxGraphs m = new SyntaxGraphs(new GraphProcessor() {
				@Override
				public void terminate() {
				}
				@Override
				public void processGraph(DetGraph graph) {
					graphs.add(graph);
				}
			});
			m.parse(trainxmll.get(i));
		}
		train(outdir,models,graphs,mode);
	}

	public static void main(String args[]) throws IOException {
		String filelist = "train.xmll";
		if (args.length>0) filelist = args[0];
		MaltParserTrainer m = new MaltParserTrainer();
		BufferedReader f = new BufferedReader(new FileReader(filelist));
		ArrayList<String> trainxmll = new ArrayList<String>();
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			trainxmll.add(s);
		}
		f.close();

		m.train("/tmp/", "svmmods", trainxmll,SVMSPLIT);
	}
}
