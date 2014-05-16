package jsafran.parsing.unsupLinear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsafran.DetGraph;
import jsafran.parsing.ClassificationResult;

/**
 * Same as TransitionParser but does not use LIBLINEAR at all: instead use SimplifiedLinearModel
 * 
 * @author xtof
 *
 */
public class TransitionParserNoliblinear extends TransitionParser {
    SimplifiedLinearModel model;
    
    /**
     * Must be called before any training/testing
     * It generates random paths onto the given test corpus just to compute the set of (all) features
     * 
     * @param gold
     */
    public void initParser(Corpus c) {
        c.buildCorpus(this);
        model = new SimplifiedLinearModel(feat2idx.size(), OneAction.values().length);
        System.out.println("parser init done nfeats= "+feat2idx.size()+" "+OneAction.values().length);
    }
    
    public List<int[]> getAllFeats(final List<DetGraph> gs) {
        final ArrayList<int[]> allfeats = new ArrayList<int[]>();
        parse(gs, new actionDecider() {
            @Override
            public OneAction getAction(DetGraph g) {
                // decides based on the linear model
                getFeats(g);
                allfeats.add(Arrays.copyOf(feats, feats.length));
                int acidx=model.getLabel(feats);
                return OneAction.values()[acidx];
            }
        });
        return allfeats;
    }
    public List<float[]> getAllScores(final List<DetGraph> gs) {
        final ArrayList<float[]> allScores = new ArrayList<float[]>();
        parse(gs, new actionDecider() {
            @Override
            public OneAction getAction(DetGraph g) {
                // decides based on the linear model
                getFeats(g);
                int acidx=model.getLabel(feats);
                allScores.add(Arrays.copyOf(model.scorePerLab, model.scorePerLab.length));
                return OneAction.values()[acidx];
            }
        });
        return allScores;
    }
    
    public double testAndCalcUAS(final List<DetGraph> gs, final List<DetGraph> gold) {
        parse(gs, new actionDecider() {
            @Override
            public OneAction getAction(DetGraph g) {
                // decides based on the linear model
                getFeats(g);
                int acidx=model.getLabel(feats);
                return OneAction.values()[acidx];
            }
        });
        Scorer UASScorer = new Scorer() {
            @Override
            public double score(List<DetGraph> gs) {
                float[] las = ClassificationResult.calcErrors(gs, gold);
                float uas = las[1];
                return uas;
            }
        };
        double uas = UASScorer.score(gs);
        return uas;
    }
    
    // this one is so slow, it should never be used. Just for debugging purpose.
    public void trainRandomSample(Corpus c) {
//        RiskEstimator r = new RiskEstimator(priors);
        ArrayList<DetGraph> gsTest = new ArrayList<DetGraph>();
        for (int i=0;i<c.getTest().size();i++) {
            DetGraph g = c.getTest().get(i).clone();
            g.cursent=i;
            gsTest.add(g);
        }
        ArrayList<DetGraph> gsTrain = new ArrayList<DetGraph>();
        for (int i=0;i<c.getTrain().size();i++) {
            DetGraph g = c.getTrain().get(i).clone();
            g.cursent=i;
            gsTrain.add(g);
        }
        final int niters=1000000;
        double bestTrainUas=Double.MIN_VALUE;
        for (int i=0;i<niters;i++) {
            model.randomInit();
            double uas=testAndCalcUAS(gsTrain, c.getTrain());
            if (uas>bestTrainUas) {
                bestTrainUas=uas;
                uas=testAndCalcUAS(gsTest, c.getTest());
                System.out.println("UAS "+i+" "+bestTrainUas+" "+uas);
            }
        }
    }
    
    // =========================================
    void test1() {
        GraphsCorpus c = new GraphsCorpus();
        TransitionParserNoliblinear m = new TransitionParserNoliblinear();
        m.initParser(c);
        m.trainRandomSample(c);
    }
    
    public static void main(String args[]) {
        TransitionParserNoliblinear m = new TransitionParserNoliblinear();
        m.test1();
    }
}
