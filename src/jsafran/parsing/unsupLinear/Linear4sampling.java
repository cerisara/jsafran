package jsafran.parsing.unsupLinear;

import java.util.HashMap;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.SolverType;

/**
 * This is a specific case of liblinear classifier, which does exactly
 * the same scoring, but at the same time, also estimates which delta
 * in the model's weights would lead to a different result.
 * This is useful to speed-up MCMC sampling.
 * 
 * @author xtof
 *
 */
public class Linear4sampling extends Linear {
	public static HashMap<Integer, Double> dims2change = new HashMap<Integer, Double>();
	
    public static double predict(Model model, Feature[] x) {
        double[] dec_values = new double[model.nr_class];
        double res = predictValues(model, x, dec_values);
        return res;
    }
    
    public static double predictValues(Model model, Feature[] x, double[] dec_values) {
        int n;
        if (model.bias >= 0)
            n = model.nr_feature + 1;
        else
            n = model.nr_feature;

        double[] w = model.w;

        int nr_w;
        if (model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS)
            nr_w = 1;
        else
            nr_w = model.nr_class;

        for (int i = 0; i < nr_w; i++)
            dec_values[i] = 0;

        for (Feature lx : x) {
            int idx = lx.getIndex();
            // the dimension of testing data may exceed that of training
            if (idx <= n) {
                for (int i = 0; i < nr_w; i++) {
                    dec_values[i] += w[(idx - 1) * nr_w + i] * lx.getValue();
                }
            }
        }
        
        // which delta-weights could have lead to a different result ?
        // only consider the margin between the first and second best class
        int bestLab=0;
        for (int i = 0; i < nr_w; i++) {
        	if (dec_values[i]>dec_values[bestLab]) bestLab=i;
        }
        int bestBadLab=0;
        for (int i = 0; i < nr_w; i++) {
        	if (i!=bestLab)
        		if (dec_values[i]>dec_values[bestBadLab]) bestBadLab=i;
        }
        double minDelta4change = dec_values[bestLab]-dec_values[bestBadLab];
        // it is always >=0
        dims2change.clear();
        for (Feature lx : x) {
            int idx = lx.getIndex();
            // the dimension of testing data may exceed that of training
            if (idx <= n) {
            	// the contrib of this feature to the score is
            	// weight * feat_val
            	// 2 ways to change the result: decrease the bestLab contrib, or increase the bestBadLab contrib
            	// (because we change only 1 dim at a time ! If we allow to change 2 dims, then we could compute the minimum change in both bestLab and bestBadLab dimensions !)
            	// decrease the bestLab contrib:
            	// w'*v-w*v=-minDelta
            	// w'=w-minDelta/v
            	if (lx.getValue()!=0) {
            		int dim2change = (idx - 1) * nr_w + bestLab;
            		// I double the change because after normalization, it'll be smaller and I want to be sure to move far enough
            		double neww = w[dim2change]-2.*minDelta4change/lx.getValue();
            		Double d = dims2change.get(dim2change);
            		if (d==null) dims2change.put(dim2change, neww);
            		else {
            			// keep only the minimum delta that leads to a change
            			if (Math.abs(d-w[dim2change])>Math.abs(neww-w[dim2change]))
            				dims2change.put(dim2change, neww);
            		}
            	}
            	// increase the bestBadLab contrib:
            	// w'*v-w*v=minDelta
            	// w'=w+minDelta/v
            	if (lx.getValue()!=0) {
            		int dim2change = (idx - 1) * nr_w + bestBadLab;
            		double neww = w[dim2change]+2.*minDelta4change/lx.getValue();
            		Double d = dims2change.get(dim2change);
            		if (d==null) dims2change.put(dim2change, neww);
            		else {
            			// keep only the minimum delta that leads to a change
            			if (Math.abs(d-w[dim2change])>Math.abs(neww-w[dim2change]))
            				dims2change.put(dim2change, neww);
            		}
            	}
            }
        }

        if (model.nr_class == 2) {
            if (model.solverType.isSupportVectorRegression())
                return dec_values[0];
            else
                return (dec_values[0] > 0) ? model.label[0] : model.label[1];
        } else {
            int dec_max_idx = 0;
            for (int i = 1; i < model.nr_class; i++) {
                if (dec_values[i] > dec_values[dec_max_idx]) dec_max_idx = i;
            }
            return model.label[dec_max_idx];
        }
    }
}
