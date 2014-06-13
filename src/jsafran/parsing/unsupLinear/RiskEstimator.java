package jsafran.parsing.unsupLinear;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import acousticModels.LogMath;

/**
 * Observations:
 * - le risk contraint tres instable: pourquoi ? r0 est stable, r1 est instable
 * - mu0 diminue vers 1, mu1 reste autour de 23
 * - var0 est stable, mais var1 devient tres petite et instable, et chaque fois qu'elle est seuillee a 1e-4, on a des sauts de r1 vers des valeurs tres grandes
 * 
 * 
 * @author xtof
 *
 */
public class RiskEstimator {
    final int nlabs=2;
    final double minvar = 0.0001;
    /*
     * means[0][0] = mu_{0,0} = score of class 0 | observations from class 0
     * means[0][1] = mu_{0,1} = score of class 1 | observations from class 0 = -means[0][0]
     * means[1][0] = mu_{1,0} = score of class 0 | observations from class 1
     * means[1][1] = mu_{1,0} = score of class 1 | observations from class 1 = -means[1][0]
     */
    double[][] means=new double[nlabs][nlabs], diagvars=new double[nlabs][nlabs];
    double[] tmp=new double[nlabs], gconst=new double[nlabs], logWeights=new double[nlabs];
    float[] labelPriors=new float[nlabs];
    // each contains the two scores as they are computed in ConstrainedLinearModel = scores of class 0 (both from 0-examples of 1-examples because we don't know to which class each example belongs)
    // scores[0] = mu_{*,0} .. shall be used to compute Gaussian mu_{0,0} and Gaussian mu_{1,0}
    // scores[1] = mu_{*,1}
    List<float[]> scores;
    LogMath logMath = new LogMath();
    int[] ex2lab=null;
    double r0,r1; // decomposition of the risk into the risk per Y

    public RiskEstimator(float[] classPriors) {
        assert classPriors.length==nlabs;
        labelPriors = Arrays.copyOf(classPriors, nlabs);
        for (int i=0;i<classPriors.length;i++) {
            logWeights[i]=logMath.linearToLog(classPriors[i]);
        }
    }
    
    public float getRisk(List<float[]> sc) {
        scores=sc;
        trainGaussians();
        System.out.println("vars "+diagvars[0][0]+" "+diagvars[0][1]+" "+diagvars[1][1]+" "+diagvars[1][0]);
//        System.out.println("trainViterbi nex: "+nex[0]+" "+nex[1]);
        float r=computeRisk();
        return r;
    }
    
    /**
     * This is the risk without constraints.
     * I developed it because it allows to compute gradients of the Gaussian means.
     * However, these gradients are only valid for a given clustering. So it's not possible to formally derive the global maximum with the gradients
     * because of these discontinuities. So there is no real gain in removing constraints, and I come back to the constrained version of the risk shown next.
     * 
     * Les XPs montrent que ce risk non contraint est beaucoup plus stable lorsqu'il est utilise avec un supervised training
     * 
     * @return
     */
    private float computeRiskunconstrained() {
        float r = 0.5f*(1f + labelPriors[0]*(float)means[0][1] + labelPriors[1]*(float)means[1][0]);
        r+=labelPriors[0]*diagvars[0][1]*gauss(means[0][0],means[0][1]+1f,diagvars[0][0]+diagvars[0][1]);
        r+=labelPriors[1]*diagvars[1][0]*gauss(means[1][1],means[1][0]+1f,diagvars[1][1]+diagvars[1][0]);
        r+=labelPriors[0]/2.0*(means[0][0]-means[0][1]-1)*(float)erf((means[0][0]-means[0][1]-1)/Math.sqrt(2f*(diagvars[0][0]+diagvars[0][1])));
        r+=labelPriors[1]/2.0*(means[1][1]-means[1][0]-1)*(float)erf((means[1][1]-means[1][0]-1)/Math.sqrt(2f*(diagvars[1][1]+diagvars[1][0])));
        return r;
    }
    /**
     * Ce risk contraint est instable lorsqu'il est utilise avec du supervised training !!
     * 
     * @return
     */
    private float computeRiskconstrained() {
        final double sqrtpi = Math.sqrt(Math.PI);
        final double s0 = Math.sqrt(diagvars[0][0]);
        double r = labelPriors[0]*(1f-2f*means[0][0])/(4f*s0*sqrtpi)*(1f+erf((0.5f-means[0][0])/s0));
        r+= labelPriors[0]/2f/Math.PI * Math.exp(-(0.5-means[0][0])*(0.5-means[0][0])/diagvars[0][0]);
        r0=r;
        final double s1 = Math.sqrt(diagvars[1][1]);
        r1=0;
        r1+= labelPriors[1]*(1f+2f*means[1][1])/(4f*s1*sqrtpi)*(1f-erf((-0.5f-means[1][1])/s1));
        r1+= labelPriors[1]/2f/Math.PI * Math.exp(-(-0.5-means[1][1])*(-0.5-means[1][1])/diagvars[1][1]);
        r+=r1;
        return (float)r;
    }
    private float computeRisk() {
//        return computeRiskunconstrained();
        return computeRiskconstrained();
    }
    
    private float gauss(double x, double mu, double var) {
        double d=x-mu;
        d*=d;
        d/=var;
        double e=Math.exp(-0.5*d);
        double g=1/Math.sqrt(2*Math.PI*var);
        return (float)(e*g);
    }
    
    // fractional error in math formula less than 1.2 * 10 ^ -7.
    // although subject to catastrophic cancellation when z in very close to 0
    // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
    public static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                t * ( 1.00002368 +
                        t * ( 0.37409196 + 
                                t * ( 0.09678418 + 
                                        t * (-0.18628806 + 
                                                t * ( 0.27886807 + 
                                                        t * (-1.13520398 + 
                                                                t * ( 1.48851587 + 
                                                                        t * (-0.82215223 + 
                                                                                t * ( 0.17087277))))))))));
        if (z >= 0) return  ans;
        else        return -ans;
    }

    /**
     * We know the priors of the 2 classes. We are only using here the score of class 0, so we know that both Gaussians will be ordered as:
     * mu_1 < mu_0
     * 
     * So we just have to find the Gaussian parameters so that P(1)% of the examples are classified into class 1, idem for class 0
     * 
     * We thus order all examples according to their score, and then initially affect the first P(1)% of them to Gaussian mu_1
     * We then compute the Gaussian parameters, and reaffect the examples: if there are less examples classified into class 1, then we add one more
     * example to class 1, and so on...
     */
    private void trainGaussians() {
        // order the scores of class 0
        float[] sc0 = new float[scores.size()];
        for (int i=0;i<sc0.length;i++) sc0[i] = scores.get(i)[0];
        Arrays.sort(sc0);
        
        // init affect
        int lim = (int)((float)sc0.length*labelPriors[0]);
        calcGaussParms(sc0,lim);
    }
    private void calcGaussParms(float[] sc0, int lim) {
        means[0][0]=0; means[1][0]=0;
        float s1=0, s0=0;
        // we know the scores of class 0 will be lower on class1-examples than on class0-examples
        for (int i=0;i<lim;i++) {
            means[1][0]+=sc0[i];
            s1+=sc0[i]*sc0[i];
        }
        means[1][0]/=(float)lim;
        means[1][1]=-means[1][0];
        for (int i=lim;i<sc0.length;i++) {
            means[0][0]+=sc0[i];
            s0+=sc0[i]*sc0[i];
        }
        means[0][0]/=(float)(sc0.length-lim);
        means[0][1]=-means[0][0];
        
        diagvars[0][0] = s0/(float)(sc0.length-lim) - means[0][0]*means[0][0];
        diagvars[1][0] = s1/(float)lim - means[1][0]*means[1][0];
        diagvars[0][1] = diagvars[0][0];
        diagvars[1][1] = diagvars[1][0];
        
        for (int y=0;y<2;y++) {
            double logdet=logMath.linearToLog(diagvars[y][0]);
            double co=(double)logMath.linearToLog(2.0*Math.PI) +logdet;
            co/=2.0;
            gconst[y]=co;
        }
    }

    /**
     * This procedure follows a classical EM, but it from time to time leads to putting nearly all examples into one class, while we really
     * want to enforce the knwon labels prior !
     */
    private void trainGaussiansEM() {
        // TODO: don't retrain the Gaussians from scratch, but rather do a few iterations from their last values
        train1gauss();
        split();
        final int niters=50;
        for (int iter=0;iter<niters;iter++) {
            trainViterbi();
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] deepCopyOf(T[] array) {
        if (0 >= array.length) return array;
        return (T[]) deepCopyOf(
                array, 
                Array.newInstance(array[0].getClass(), array.length), 
                0);
    }
    private static Object deepCopyOf(Object array, Object copiedArray, int index) {
        if (index >= Array.getLength(array)) return copiedArray;
        Object element = Array.get(array, index);
        if (element.getClass().isArray()) {
            Array.set(copiedArray, index, deepCopyOf(
                    element,
                    Array.newInstance(
                            element.getClass().getComponentType(),
                            Array.getLength(element)),
                    0));
        } else {
            Array.set(copiedArray, index, element);
        }
        return deepCopyOf(array, copiedArray, ++index);
    }
    private double getLoglike(double[] means, double[] diagvars, double gconst, float[] z) {
        for (int k=0;k<nlabs;k++)
            tmp[k]=(z[k]-means[k])/diagvars[k];
        double o=0;
        for (int j=0;j<nlabs;j++) o += (z[j]-means[j])*tmp[j];
        o/=2.0;
        double loglikeYt = - gconst - o;
        return loglikeYt;
    }
    
    int[] nex;
    /**
     * reassigns each frame to one mixture, and retrain the mean and var
     * 
     * @param c
     */
    private void trainViterbi() {
        double[][] means0 = deepCopyOf(means);
        double[][] diagvars0 = deepCopyOf(diagvars);
        double[] gconst0 = Arrays.copyOf(gconst, gconst.length);
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
            Arrays.fill(diagvars[i], 0);
        }
        nex = new int[nlabs];
        Arrays.fill(nex, 0);
        if (ex2lab==null) ex2lab = new int[scores.size()];
        else assert scores.size()==ex2lab.length;
        for (int ex=0;ex<scores.size();ex++) {
            float[] z = scores.get(ex);
            Arrays.fill(tmp, 0);
            for (int y=0;y<nlabs;y++) tmp[y]=logWeights[y] + getLoglike(means0[y], diagvars0[y], gconst0[y], z);
            int besty=0;
            for (int y=1;y<nlabs;y++)
                if (tmp[y]>tmp[besty]) besty=y;
            nex[besty]++;
            ex2lab[ex]=besty;
            for (int i=0;i<nlabs;i++) {
                means[besty][i]+=z[i];
            }
        }
        
        for (int y=0;y<nlabs;y++) {
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) means[y][i]=0;
            else
                for (int i=0;i<nlabs;i++) {
                    means[y][i]/=(float)nex[y];
                }
        }
        for (int ex=0;ex<scores.size();ex++) {
            float[] z = scores.get(ex);
            int besty = ex2lab[ex];
            for (int i=0;i<nlabs;i++) {
                tmp[i] = z[i]-means[besty][i];
                diagvars[besty][i]+=tmp[i]*tmp[i];
            }
        }
        for (int y=0;y<nlabs;y++) {
            double logdet=0;
            if (nex[y]==0)
                for (int i=0;i<nlabs;i++) {
                    diagvars[y][i] = minvar;
                    logdet += logMath.linearToLog(diagvars[y][i]);
                }
            else
                for (int i=0;i<nlabs;i++) {
                    diagvars[y][i] /= (double)nex[y];
                    if (diagvars[y][i] < minvar) diagvars[y][i]=minvar;
                    logdet += logMath.linearToLog(diagvars[y][i]);
                }
            double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) +logdet;
            co/=2.0;
            gconst[y]=co;
        }
        
    }
    
    /**
     * Assuming all mixtures are initially equal, moves away in opposite directions every mixture
     */
    private void split() {
        final double ratio = 0.1;
        for (int y=1;y<nlabs;y++) {
            for (int i=0;i<nlabs;i++) {
                means[y][i]=means[0][i];
                diagvars[y][i]=diagvars[0][i];
            }
        }
        for (int y=0;y<nlabs;y++) {
            if (y%2==0)
                for (int i=0;i<nlabs;i++)
                    means[y][i]+=Math.sqrt(diagvars[y][i])*ratio;
            else
                for (int i=0;i<nlabs;i++)
                    means[y][i]-=Math.sqrt(diagvars[y][i])*ratio;
        }
    }
    
    private void train1gauss() {
        for (int i=0;i<nlabs;i++) {
            Arrays.fill(means[i], 0);
        }
        for (int ex=0;ex<scores.size();ex++) {
            float[] z = scores.get(ex);
            for (int i=0;i<nlabs;i++) {
                means[0][i]+=z[i];
            }
        }
        for (int i=0;i<nlabs;i++) {
            means[0][i]/=(float)scores.size();
            for (int j=1;j<nlabs;j++) means[j][i]=means[0][i];
        }
        Arrays.fill(diagvars[0], 0);
        for (int ex=0;ex<scores.size();ex++) {
            float[] z = scores.get(ex);
            for (int i=0;i<nlabs;i++) {
                tmp[i] = z[i]-means[0][i];
            }
            for (int i=0;i<nlabs;i++) {
                diagvars[0][i]+=tmp[i]*tmp[i];
            }
        }
        
        // precompute gconst
        /*
         * log de
         * (2pi)^{d/2} * |Covar|^{1/2} 
         */
        double det=1;
        for (int i=0;i<nlabs;i++) {
            diagvars[0][i] /= (double)scores.size();
            if (diagvars[0][i] < minvar) diagvars[0][i]=minvar;
            det *= diagvars[0][i];
        }
        double co=(double)nlabs*logMath.linearToLog(2.0*Math.PI) + logMath.linearToLog(det);
        co/=2.0;
        for (int i=0;i<nlabs;i++) gconst[i]=co;        
    }
}
