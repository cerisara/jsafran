package jsafran.parsing.unsupLinear;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import acousticModels.LogMath;

public class RiskEstimator {
    final int nlabs=2;
    final double minvar = 0.0001;
    double[][] means=new double[nlabs][nlabs], diagvars=new double[nlabs][nlabs];
    double[] tmp=new double[nlabs], gconst=new double[nlabs], logWeights=new double[nlabs];
    float[] linearWeights=new float[nlabs];
    List<float[]> scores;
    LogMath logMath = new LogMath();
    int[] ex2lab=null;

    public RiskEstimator(float[] classPriors) {
        assert classPriors.length==nlabs;
        linearWeights = Arrays.copyOf(classPriors, nlabs);
        for (int i=0;i<classPriors.length;i++) {
            logWeights[i]=logMath.linearToLog(classPriors[i]);
        }
    }
    
    public float getRisk(List<float[]> sc) {
        scores=sc;
        trainGaussians();
        float r=computeRisk();
        return r;
    }
    
    private float computeRisk() {
        float r = 0.5f*(1f + linearWeights[1]*(float)means[1][1] + linearWeights[0]*(float)means[0][0] + linearWeights[0]*(float)means[0][1] + linearWeights[1]*(float)means[1][0]);
        r+=linearWeights[0]*diagvars[0][1]*gauss(means[0][0],means[0][1]+1f,diagvars[0][0]+diagvars[0][1]);
        r+=linearWeights[1]*diagvars[1][0]*gauss(means[1][1],means[1][0]+1f,diagvars[1][1]+diagvars[1][0]);
        r+=linearWeights[0]*(means[0][0]-means[0][1]/2f-1)*(float)erf((means[0][0]-means[0][1]-1)/Math.sqrt(2f*(diagvars[0][0]+diagvars[0][1])));
        r+=linearWeights[1]*(means[1][1]-means[1][0]/2f-1)*(float)erf((means[1][1]-means[1][0]-1)/Math.sqrt(2f*(diagvars[1][1]+diagvars[1][0])));
        return r;
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

    private void trainGaussians() {
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
        int[] nex = new int[nlabs];
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
