package jsafran.parsing.unsupLinear;

public class GradientMethod {
    /**
     * This is the function optimized during training
     * @author xtof
     */
    public interface Scorer {
        public float getScore();
        public int getNdims(); // nb of parameters
        public float getParm(int dim);
        public void  setParm(int dim, float val);
    }
    /**
     * Used to evaluate each move in terms of test F1, test accuracy...
     * This function is optional and does not impact training at all
     * @author xtof
     */
    public interface Evaluator {
        public float eval();
    }
    
    Scorer scorer;
    Evaluator eval = null;
    float[] grad;
    
    public GradientMethod(Scorer sc) {
        scorer=sc;
        grad = new float[scorer.getNdims()];
    }
    public void setEvaluator(Evaluator f) {
        eval=f;
    }
    
    /**
     * Most basic option: parse all parms one after the other
     */
    public void trainMaxScore() {
        final int niters = 10000;
        float eps = 0.01f; // TODO: adjust it auto
        float sc0 = scorer.getScore();
        System.out.println("gradient init "+sc0);
        for (int iter=0;iter<niters;iter++) {
            for (int i=0;i<scorer.getNdims();i++) {
                float v0 = scorer.getParm(i);
                scorer.setParm(i, v0+eps);
                float sc1 = scorer.getScore();
                scorer.setParm(i, v0);
                if (sc1==sc0) continue;
                grad[i]=(sc1-sc0)/eps;
                
                // we want to maximize score
                scorer.setParm(i, scorer.getParm(i)+grad[i]*eps);
                sc0 = scorer.getScore();
                System.out.println("gradient "+iter+" "+i+" "+sc0);
            }
        }
    }
    /**
     * Second option: explore gradient for one parm with line search
     */
    public void trainMaxScoreLineSearch(float minval, float maxval) {
        final int nbins = 10;
        float[] vals = new float[nbins];
        final int niters = 10000;
        float sc0 = scorer.getScore();
        if (eval!=null)
            System.out.println("gradient init "+sc0+" "+eval.eval());
        else
            System.out.println("gradient init "+sc0);
        for (int iter=0;iter<niters;iter++) {
            for (int i=0;i<scorer.getNdims();i++) {
                int bmax=0;
                for (int b=0;b<nbins;b++) {
                    float v = minval+(float)b*(maxval-minval)/(float)nbins;
                    scorer.setParm(i, v);
                    vals[b] = scorer.getScore();
                    if (vals[b]>vals[bmax]) bmax=b;
                }
                float v = minval+(float)bmax*(maxval-minval)/(float)nbins;
                scorer.setParm(i, v);
                if (eval!=null)
                    System.out.println("gradient "+iter+" "+i+" "+vals[bmax]+" "+eval.eval());
                else
                    System.out.println("gradient "+iter+" "+i+" "+vals[bmax]);
            }
        }
    }
}
