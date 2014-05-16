package corpus;

import java.util.ArrayList;
import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;

/**
 * nouvelle version du corpus Ester 2009 qui permet d'acceder aux arbres syntaxiques +
 * aux fichiers .wav pour la partie test !
 * 
 * @author xtof
 *
 */
public class Ester2009 {
	final String[] testfiles = {
			"corpusSyntaxe/train/19991101_0700_0800_inter_0_23.xml",
			"corpusSyntaxe/train/19991101_0700_0800_inter_107_117.xml",
			"corpusSyntaxe/train/19991101_0700_0800_inter_118_128.xml",
			"corpusSyntaxe/train/19991101_0700_0800_inter_33_54.xml",
			"corpusSyntaxe/train/19991101_0700_0800_inter_81_95.xml",
			"corpusSyntaxe/train/19991101_0700_0800_inter_97_106.xml",
			"corpusSyntaxe/train/19991102_0700_0800_inter_0_24.xml",
			"corpusSyntaxe/train/19991102_0700_0800_inter_105_115.xml",
			"corpusSyntaxe/train/19991102_0700_0800_inter_116_130.xml",
			"corpusSyntaxe/train/19991102_0700_0800_inter_28_64.xml",
			"corpusSyntaxe/train/19991102_0700_0800_inter_70_87.xml",
			"corpusSyntaxe/train/19991102_0700_0800_inter_88_104.xml",
			"corpusSyntaxe/train/19991102_0800_0900_inter_0_19.xml",
			"corpusSyntaxe/train/19991103_0700_0800_inter_0_62.xml",
			"corpusSyntaxe/train/19991103_0700_0800_inter_65_79.xml",
			"corpusSyntaxe/train/19991103_0700_0800_inter_79_97.xml",
			"corpusSyntaxe/train/19991103_0700_0800_inter_97_133.xml"
	};
	
	final String[] trainfiles = {
			"corpusSyntaxe/trainem/logerot/19990629_1900_1920_inter_145_306.xml",
			"corpusSyntaxe/trainem/logerot/19991025_0700_0800_inter_499_747.xml",
			"corpusSyntaxe/trainem/logerot/19990629_1900_1920_inter_p1.xml",
			"corpusSyntaxe/trainem/20020705_1900_2000_inter_1.xml",
			"corpusSyntaxe/trainem/huel/19990628_1900_1920_inter_p1.xml",
			"corpusSyntaxe/trainem/huel/19990628_1900_1920_inter_103_206.xml",
			"corpusSyntaxe/trainem/perrin/19990701_1900_1920_inter_p1.xml",
			"corpusSyntaxe/trainem/perrin/19991025_0700_0800_inter_748_914.xml",
			"corpusSyntaxe/trainem/perrin/19991025_0800_0900_inter_0_185.xml",
			"corpusSyntaxe/trainem/19990702_1900_1920_inter_0_18_xtof.xml",
			"corpusSyntaxe/trainem/salcedo/19990630_1900_1920_inter_p1.xml",
			"corpusSyntaxe/trainem/salcedo/19991025_0700_0800_inter_0_249.xml",
			"corpusSyntaxe/trainem/salcedo/19990630_1900_1920_inter_147_293.xml",
	};
	
	public List<DetGraph> getTrain() {
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		GraphIO gio = new GraphIO(null);
		for (String s : trainfiles) {
			List<DetGraph> zz = gio.loadAllGraphs(s);
			for (int i=0;i<zz.size();i++) {
				DetGraph g = zz.get(i);
				g.comment=s+"_"+i;
				gs.add(g);
			}
		}
		return gs;
	}
	public List<DetGraph> getTest() {
		ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
		GraphIO gio = new GraphIO(null);
		for (String s : testfiles) {
			List<DetGraph> zz = gio.loadAllGraphs(s);
			for (int i=0;i<zz.size();i++) {
				DetGraph g = zz.get(i);
				g.comment=s+"_"+i;
				gs.add(g);
			}
		}
		return gs;
	}
}
