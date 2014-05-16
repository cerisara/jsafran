package jsafran.parsing.unsup.criteria;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import jsafran.Dep;
import jsafran.DetGraph;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.rules.Rules;

public class RuleCrit3EN extends RuleCrit3 {

	float alphaD = 0.1f;
	float alphaW = 0.001f;

	public RuleCrit3EN(Rules r, Voc v) {
		super(r,v);

		try {
			BufferedReader f=new BufferedReader(new FileReader("/tmp/alphas"));
			String s=f.readLine();
			String[] ss = s.split(" ");
			alphaD = Float.parseFloat(ss[0]);
			alphaW = Float.parseFloat(ss[1]);
			f.close();
		} catch (Exception e) {
		}
		System.out.println("ALPHAS "+alphaD+" "+alphaW);
	}

	public double getLogPost(DetGraph g, List<int[]> rs) {
		double logp = super.getLogPost(g, rs);
		if (false)
			for (int i=0;i<g.getNbMots();i++) {
				int d=g.getDep(i);
				if (d>=0) {
					int h=g.getHead(d);
					String l=g.getDepLabel(d);
					logp+=Math.log(getDirectionProba(l, h, i));
				}
			}
		return logp;
	}

	double getDirectionProba(String dt, int head, int child) {
		if (dt.equals("NMOD")) {
			return head>child?0.9:0.1;
		} else if (dt.equals("SBJ")) {
			return head>child?0.95:0.05;
		} else if (dt.equals("PMOD")) {
			return head<child?0.9:0.1;
		} else if (dt.equals("OBJ")) {
			return head<child?0.9:0.1;
		} else if (dt.equals("ADV")) {
			return head<child?0.6:0.4;
		} else if (dt.equals("VC")) {
			return head<child?0.99:0.01;
		} else if (dt.equals("PRD")) {
			return head<child?0.99:0.01;
		} else if (dt.equals("DEP")) {
			return head<child?0.3:0.6;
		} else if (dt.equals("NAME")) {
			return head<child?0.01:0.99;
		} else if (dt.equals("TMP")) {
			return head<child?0.55:0.45;
		} else if (dt.equals("AMOD")) {
			return 0.5;
		} else if (dt.equals("COORD")) {
			return head<child?0.99:0.01;
		} else if (dt.equals("APPO")) {
			return head<child?0.99:0.01;
		} else if (dt.equals("IM")) {
			return head==child-1?0.99:0.01;
		} else if (dt.equals("CONJ")) {
			return head<child?0.99:0.01;
		}
		return 1;
	}

	@Override
	protected int[] getLabs2track() {
		final int[] labs = {
				Dep.getType("NMOD"),
				Dep.getType("SBJ"),Dep.getType("VC"),Dep.getType("PRD")};
		return labs;
	}

	@Override
	protected double getAlphaD(int d) {
		return alphaD;
	}
	@Override
	protected double getAlphaW(int w) {
		return alphaW;
	}
}
