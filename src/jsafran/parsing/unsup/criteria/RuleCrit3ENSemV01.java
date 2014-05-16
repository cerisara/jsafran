package jsafran.parsing.unsup.criteria;

import java.io.BufferedReader;
import java.io.FileReader;

import jsafran.Dep;
import jsafran.parsing.unsup.Voc;
import jsafran.parsing.unsup.rules.Rules;

public class RuleCrit3ENSemV01 extends RuleCrit3SemV01 {

	float alphaD = 0.1f;
	float alphaW = 0.0001f;

	public RuleCrit3ENSemV01(Rules r, Voc v) {
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
