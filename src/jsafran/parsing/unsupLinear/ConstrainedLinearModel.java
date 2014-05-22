package jsafran.parsing.unsupLinear;

import java.util.Random;

/**
 * This linear model mimics the liblinear model, but:
 * - no bias (if you want one, just add a feature that is always true)
 * - boolean features
 * - assumes 2 classes only: 0 and 1
 * - assumes score_1(X) = -score_0(X)
 * 
 * It thus contains one weight per feature
 * 
 * @author xtof
 *
 */
public class ConstrainedLinearModel {
    float[] w;
    int nfts;
    // we "simulate" as if we had 2 scores, but they really are the opposite one from the other
    float[] scorePerLab = {0,0};
    
    public ConstrainedLinearModel(int nfeats) {
        nfts=nfeats;
        System.out.println("allocate w "+nfeats+" ");
        w=new float[nfeats];
    }
    public void randomInit() {
        Random random = new Random();
        float s=0;
        for (int i = 0; i < nfts; i++) {
            // precision should be at least 1e-4
            w[i] = (float)(Math.round(random.nextDouble() * 100000.0) / 10000.0);
            s+=w[i];
        }
        // forces the sum of weights to be 0
        float m=s/(float)nfts;
        for (int i = 0; i < nfts; i++) {
            w[i]-=m;
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
        scorePerLab[0]=0;
        for (int i=0;i<feats.length;i++) {
            if (feats[i]<0) {
                // no more active features
                break;
            }
            scorePerLab[0] += w[feats[i]];
        }
        scorePerLab[1]=-scorePerLab[0];
        int bestlab = scorePerLab[0]>=0?0:1;
        return bestlab;
    }
    public float getSumWeights() {
        float s=0f;
        for (float we : w) s+=we;
        return s;
    }
    public void updateWeight(int feat, int lab, float epsilon) {
        float delta = lab==0?epsilon/2f:-epsilon/2f;
        w[feat]+=delta;
    }
}
