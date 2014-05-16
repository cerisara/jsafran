package jsafran.parsing.unsupLinear;

import java.util.Arrays;
import java.util.Random;

/**
 * This linear model mimics the liblinear model, but:
 * - no bias (if you want one, just add a feature that is always true)
 * - boolean features
 * 
 * It is used in my own training procedures to avoid reparsing the whole corpus
 * 
 * It contains one weight per feature X label; it does not constrain the scores over all labels to sum to 1 or 0,
 * so be aware to use a discriminative loss function !
 * 
 * @author xtof
 *
 */
public class SimplifiedLinearModel {
    float[] w;
    int nfts,nlbs;
    float[] scorePerLab;
    
    public SimplifiedLinearModel(int nfeats, int nlabs) {
        nfts=nfeats;
        nlbs=nlabs;
        System.out.println("allocate w "+nfeats+" "+nlabs);
        w=new float[nfeats*nlabs];
        scorePerLab = new float[nlabs];
    }
    int getWidx(int feat, int lab) {
        int i=feat*nlbs+lab;
        if (i>=w.length) {
            System.out.println("ERROR "+feat+" "+lab+" "+w.length);
            String a=null; a.charAt(0);
        }
        return i;
    }
    public void randomInit() {
        Random random = new Random();
        for (int i = 0; i < nfts; i++) {
            float sum=0;
            for (int j=1;j<nlbs;j++) {
                int widx = getWidx(i, j);
                // precision should be at least 1e-4
                w[widx] = (float)(Math.round(random.nextDouble() * 100000.0) / 10000.0);
                sum+=w[widx];
            }
            {
                // force the sum of weights = 1
                int widx = getWidx(i, 0);
                w[widx]=1f-sum;
            }
        }
    }
    public float getNegError(ActionCorpus data) {
        float err=0;
        for (int i=0;i<data.actions.length;i++) {
            err+=getLabel(data.features[i])==data.actions[i]?0:1;
        }
        return -err;
    }
    public int getLabel(int[] feats) {
        Arrays.fill(scorePerLab, 0);
        for (int i=0;i<feats.length;i++) {
            if (feats[i]<0) {
                // no more active features
                break;
            }
            for (int j=0;j<scorePerLab.length;j++) {
                int widx = getWidx(feats[i], j);
                scorePerLab[j]+=w[widx];
            }
        }
        int bestlab = 0;
        for (int j=1;j<scorePerLab.length;j++) {
            if (scorePerLab[j]>scorePerLab[bestlab]) bestlab=j;
        }
        return bestlab;
    }
    public float getSumWeights() {
        float s=0f;
        for (float we : w) s+=we;
        return s;
    }
}
