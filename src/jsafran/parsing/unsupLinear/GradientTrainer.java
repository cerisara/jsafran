package jsafran.parsing.unsupLinear;

import java.util.Arrays;
import java.util.Random;

public class GradientTrainer {
    public interface Scorer {
        /**
         * compute the score obtained with these parameters
         */
        public float getScore(float[] parms);
    }
    /**
     * maximize the score using a gradient approach based on finite-state difference combined with line-search
     */
    public void train(float[] parms, Scorer scorer) {
        final float[] linesearch = {-0.1f,-0.05f,0.05f,0.1f};
        float[] grad = new float[parms.length];
        for (int iter=0;iter<100000;iter++) {
            float curval = scorer.getScore(parms);
            for (int i=0;i<parms.length;i++) {
                System.out.println(iter+" "+curval+" "+i+"/"+parms.length);
                float bestScoreGain=Float.MIN_VALUE;
                float p=parms[i];
                float bestNewParm=p;
                for (float r:linesearch) {
                    float delta = r*p;
                    if (delta==0) delta=0.0001f;
                    else if (Math.abs(delta)<0.0001) delta=Math.signum(delta)*0.0001f;
                    float newp = p+delta;
                    parms[i]=newp;
                    float scoreGain = scorer.getScore(parms)-curval;
                    if (scoreGain>bestScoreGain) {
                        bestScoreGain=scoreGain; bestNewParm=newp;
                    }
                }
                parms[i]=p;
                grad[i]=bestNewParm;
            }
            for (int i=0;i<parms.length;i++) {
                parms[i]=grad[i];
            }
        }
    }

    // --------------------------------------
    // below exactly the same thing as before, but in double[] to allow for double parms !
    // TODO: find a better way to support that
    public interface ScorerDouble {
        /**
         * compute the score obtained with these parameters
         */
        public float getScore(double[] parms);
    }
    /**
     * maximize the score using a gradient approach based on finite-state difference combined with line-search
     */
    public void train(double[] parms, ScorerDouble scorer) {
//        final float[] linesearch = {-0.1f,-0.05f,0.05f,0.1f};
        final float[] linesearch = {-0.05f,0.05f};
        float linemult=1f;
        double[] grad = new double[parms.length];
        for (int iter=0;iter<100000;iter++) {
            float curval = scorer.getScore(parms);
            double largestChange=0;
            int ninc=0, ndec=0, neq=0;
            for (int i=0;i<parms.length;i++) {
                System.out.println("debug "+i+"/"+parms.length);
                float bestScoreGain=Float.MIN_VALUE;
                double p=parms[i];
                double bestNewParm=p;
                for (float r:linesearch) {
                    r*=linemult;
                    double delta = r*p;
                    if (delta==0) delta=0.0001f;
                    else if (Math.abs(delta)<0.0001) delta=Math.signum(delta)*0.0001f;
                    double newp = p+delta;
                    parms[i]=newp;
                    float scoreGain = scorer.getScore(parms)-curval;
                    if (scoreGain>bestScoreGain) {
                        bestScoreGain=scoreGain; bestNewParm=newp;
                        ninc++;
                    } else if (scoreGain==bestScoreGain) neq++;
                    else ndec++;
                }
                parms[i]=p;
                grad[i]=bestNewParm;
                if (Math.abs(bestNewParm-p)>largestChange) largestChange=Math.abs(bestNewParm-p);
            }
            String suff="";
            if (ninc==0&&neq==0&&ndec>0) {
                linemult*=2f;
                suff="local optimum detected; jumping ! "+linemult;
                // TODO: if jumping becomes too large, rather moves toward a random position
            } else if (linemult>1)
                linemult/=2f;
            System.out.println(iter+" "+curval+" maxmodif "+largestChange+" "+ninc+" "+ndec+" "+neq+" "+suff);
            for (int i=0;i<parms.length;i++) {
                parms[i]=grad[i];
            }
        }
    }

	// ============================================
	// below: for unit tests
	
	class UniformCorpus {
	    final int nobs = 10000;
	    final float mean=5;
	    Random r = new Random();
	    float[] obs;
	    public UniformCorpus() {
	        obs = new float[nobs];
	        for (int i=0;i<nobs;i++) obs[i]=(float)r.nextGaussian()+mean;
	    }
	}
	
	class GaussianMod implements Scorer {
	    final float dp = -0.5f*(float)Math.log(2.*Math.PI);
	    float[] parms = {0,1}; // mean and var
	    float[] corp;
	    
	    public GaussianMod(float[] data) {
	        corp=data;
	    }
	    float getLogLike(float x) {
            float mean = parms[0];
            float var =  parms[1];
	        float logl = dp - 0.5f*(float)Math.log(var);
	        logl -= 0.5f* (mean-x)*(mean-x)/var;
	        return logl;
	    }
        @Override
        public float getScore(float[] parms) {
            float loglike = 0;
            for (int i=0;i<corp.length;i++) loglike += getLogLike(corp[i]);
            return loglike;
        }
	}
	
	public static void main(String args[]) {
	    GradientTrainer m = new GradientTrainer();
	    UniformCorpus data = m.new UniformCorpus();
	    GaussianMod mod = m.new GaussianMod(data.obs);
	    System.out.println("training start");
	    m.train(mod.parms, mod);
	    System.out.println("params trained: "+Arrays.toString(mod.parms));
	}
}
