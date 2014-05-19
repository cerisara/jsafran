package jsafran.parsing.unsupLinear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import utils.CurvePlotter;
import utils.QuickSort;

import jsafran.DetGraph;
import jsafran.parsing.ClassificationResult;
import jsafran.parsing.unsupLinear.TransitionParser.Scorer;

/**
 * 
 * @author xtof
 *
 */
public class MSTParser {
    public final byte NOARC=0, ARC=1, NACTIONS=2;
    // Number of max features slots
    final int nfeatsSlotsMax = 50;
    protected int[] feats = new int[nfeatsSlotsMax];
    protected HashMap<String, Integer> feat2idx = new HashMap<String, Integer>();
    static Random random = new Random();
    SimplifiedLinearModel model;
    // TODO: make priors depend on sentence length !
    final float[] priors = {0.8f,0.2f};

    /**
     * Must be called before any training/testing
     * It computes features for all possible word pairs
     * 
     * @param gold
     */
    public void initParser(Corpus c) {
        for (DetGraph g : c.getTrain())
            for (int i=0;i<g.getNbMots();i++)
                for (int j=0;j<g.getNbMots();j++)
                    if (i!=j) getFeats(g,i,j);
        for (DetGraph g : c.getTest())
            for (int i=0;i<g.getNbMots();i++)
                for (int j=0;j<g.getNbMots();j++)
                    if (i!=j) getFeats(g,i,j);
        model = new SimplifiedLinearModel(feat2idx.size(), NACTIONS);
        model.randomInit();
        System.out.println("parser init done nfeats= "+feat2idx.size()+" "+NACTIONS);
    }
    private int getIdx(String f) {
        Integer i = feat2idx.get(f);
        if (i==null) {
            i=feat2idx.size();
            feat2idx.put(f, i);
        }
        return i;
    }
    // warning: for now, the features must not depend on previously annotated arcs ! Only POStags and words.
    public int getFeats(DetGraph g, int i, int j) {
        int nfeats=0;
        {
            // GOV features
            String x=g.getMot(i).getForme();
            feats[nfeats++]=getIdx("GOVW"+x);
            x=g.getMot(i).getPOS();
            feats[nfeats++]=getIdx("GOVP"+x);
            if (i>0) {
                x=g.getMot(i-1).getPOS();
                feats[nfeats++]=getIdx("GOV1P"+x);
            }
        }
        {
            // HEAD features
            String x=g.getMot(j).getForme();
            feats[nfeats++]=getIdx("HEDW"+x);
            x=g.getMot(j).getPOS();
            feats[nfeats++]=getIdx("HEDP"+x);
            if (j>0) {
                x=g.getMot(j-1).getPOS();
                feats[nfeats++]=getIdx("HED1P"+x);
            }
        }
        {
            // joint (GOV,HEAD)
            String x=g.getMot(i).getPOS();
            String y=g.getMot(j).getPOS();
            feats[nfeats++]=getIdx("JOP"+x+y);
            x=g.getMot(j).getPOS();
            y=g.getMot(i).getForme();
            feats[nfeats++]=getIdx("JOW"+x+y);
        }
        {
            // distance features
            int d=j-i;
            String s="FARR";
            if (d==1) s="PUN";
            else if (d==-1) s="MUN";
            else if (d==2) s="PDX";
            else if (d==-2) s="MDX";
            else if (d==3) s="PTR";
            else if (d==-3) s="MTR";
            else if (d<0) s="FARL";
            feats[nfeats++]=getIdx("DIST"+s);
            {
                // joint (GOV,DIST)
                String x=g.getMot(i).getPOS();
                feats[nfeats++]=getIdx("JGD"+x+s);
            }
            {
                // joint (HEAD,DIST)
                String x=g.getMot(j).getPOS();
                feats[nfeats++]=getIdx("JHD"+x+s);
            }
        }
        {
            // biais par sentence length
            // bizarre ce biais semble ne servir a rien du tout !!
//            feats[nfeats++]=getIdx("LEN"+g.getNbMots());
        }
        feats[nfeats]=-1; // blocker/EOS that indicates the "end" of active features
        return nfeats;
    }

    public List<float[]> parseAndGetScores(List<DetGraph> gs) {
        ArrayList<float[]> scores = new ArrayList<float[]>();
        for (DetGraph g : gs) scores.addAll(parse(g));
        return scores;
    }
    
   /**
    *     
    1. Sort the edges of G into decreasing order by weight. Let T be the set of edges comprising the maximum weight spanning tree. Set T = ∅.
    2. Add the first edge to T.
    3. Add the next edge to T if and only if it does not form a cycle in T. If there are no remaining edges exit and report G to be disconnected.
    4. If T has n−1 edges (where n is the number of vertices in G) stop and output T . Otherwise go to step 3.
    * @param g
    * @return all scores
    */
   public List<float[]> parse(DetGraph g) {
       g.clearDeps();
       // init
       class Edge implements Comparable<Edge> {
           int go, h;
           float[] score;
           public Edge(DetGraph g, int gov, int head) {
               go=gov; h=head;
               int nfeats = getFeats(g, gov, head);
               model.getLabel(feats);
               score=Arrays.copyOf(model.scorePerLab, NACTIONS);
           }
           @Override
           public int compareTo(Edge o) {
               // TODO: use the absolute score of only the ARC lab ? Or the margin ?
               if (score[ARC]-score[NOARC]>o.score[ARC]-o.score[NOARC]) return -1;
               else if (score[ARC]-score[NOARC]<o.score[ARC]-o.score[NOARC]) return 1;
               else return 0;
           }
       }
       Edge[] edges = new Edge[g.getNbMots()*(g.getNbMots()-1)];
       for (int i=0,k=0;i<g.getNbMots();i++)
           for (int j=0;j<g.getNbMots();j++)
               if (i!=j) {
                   edges[k++]=new Edge(g,i,j);
               }
       
       Arrays.sort(edges);
       // I have checked: we get the edges in decreasing order of score
       ArrayList<Edge> T = new ArrayList<Edge>();
       for (int n=0;n<edges.length;n++) {
           if (T.size()==g.getNbMots()-1) break;
           Edge e = edges[n];
           {
               // check if there is not already an arc starting from this word
               int gov = e.go;
               int d=g.getDep(gov);
               if (d>=0) continue;
           }
           // add edge
           int d=g.ajoutDep("_", e.go, e.h);
           // check if cycle
           if (isCycle(g,e.go,e.h)) {
               // remove edge
               g.removeDep(d);
           } else T.add(e);
       }
       ArrayList<float[]> scores = new ArrayList<float[]>();
       for (int i=0;i<T.size();i++) scores.add(T.get(i).score);
       return scores;
   }
   private boolean isCycle(DetGraph g, int gov, int head) {
       if (head==gov) return true;
       int d=g.getDep(head);
       if (d<0) return false;
       int newhead = g.getHead(d);
       return isCycle(g, gov, newhead);
   }

   public double testAndCalcUAS(final List<DetGraph> gs, final List<DetGraph> gold) {
       parseAndGetScores(gs);
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

   public void trainUnsup(final Corpus c) {
       final List<DetGraph> gsTrain = c.getTrain();
       final RiskEstimator risk = new RiskEstimator(priors);
       final int nparms=feat2idx.size();
       
       // order the parms to search first the most useful parms
       class RiskScorer implements jsafran.parsing.unsupLinear.GradientMethod.Scorer {
           int[] featidx = new int[nparms];
           public RiskScorer() {
               System.out.println("creating riskscorer "+feat2idx.size());
               for (int i=0;i<nparms;i++) featidx[i]=i;
               float[] featocc = new float[nparms];
               Arrays.fill(featocc, 0);
               for (DetGraph g : gsTrain) {
                   for (int i=0;i<g.getNbMots();i++)
                       for (int j=0;j<g.getNbMots();j++) {
                           if (i==j) continue;
                           getFeats(g,i,j);
                           // use negative numbers because we want a reverse sort in the end
                           for (int k=0;k<feats.length;k++)
                               if (feats[k]<0) break;
                               else featocc[feats[k]]--;
                       }
               }
               QuickSort.sort(featocc, featidx);
               System.out.print("most important features: ");
               for (int i=0;i<10&&i<nparms;i++) {
                   String ff = "";
                   for (String f : feat2idx.keySet())
                       if (feat2idx.get(f).equals(featidx[i])) {ff=f; break;}
                   System.out.print(ff+" ");
               }
               System.out.println();
           }
           @Override
           public void setParm(int dim, float val) {
               model.w[featidx[dim]]=val;
           }
           @Override
           public float getParm(int dim) {
               return model.w[featidx[dim]];
           }
           @Override
           public int getNdims() {
               return model.w.length;
           }
           @Override
           public float getScore() {
               List<float[]> scores = parseAndGetScores(gsTrain);
               return -risk.getRisk(scores);
           }
       }
       jsafran.parsing.unsupLinear.GradientMethod.Scorer scorer = new RiskScorer();

       GradientMethod trainer = new GradientMethod(scorer);
       class Eval implements jsafran.parsing.unsupLinear.GradientMethod.Evaluator {
           List<DetGraph> gsTest, gold;
           public Eval() {
               gold=c.getTest();
               gsTest = new ArrayList<DetGraph>();
               for (int i=0;i<c.getTest().size();i++) {
                   DetGraph g = c.getTest().get(i).clone();
                   g.cursent=i;
                   gsTest.add(g);
               }
           }
           @Override
           public float eval() {
               return (float)testAndCalcUAS(gsTest, gold);
           }
       }
       trainer.setEvaluator(new Eval());

       //       trainer.trainMaxScore();
       trainer.trainMaxScoreLineSearch(-2, 2);
   }
   
   public void trainPerceptronSupervised(Corpus c) {
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
       RiskEstimator r = new RiskEstimator(priors);
       
       int plotidx=0;
//       int accPlot = CurvePlotter.addSerie("Acc",plotidx);
//       int acc0Plot = CurvePlotter.addSerie("Acc0",plotidx);
//       int acc1Plot = CurvePlotter.addSerie("Acc1",plotidx++);
       int uasPlotTest = CurvePlotter.addSerie("UASTest", plotidx);
       int uasPlotTrain = CurvePlotter.addSerie("UASTrain", plotidx++);
       int marginPlot = CurvePlotter.addSerie("Margin", plotidx++);
       int riskPlot = CurvePlotter.addSerie("Risk", plotidx++);
       
       /*
        * - generate the training corpus as (feats,label) and not as DetGraphs 
        */
       FeatsCorpus fc = new FeatsCorpus();
       List<DetGraph> trainc = c.getTrain();
       for (DetGraph g : trainc) {
           for (int i=0;i<g.getNbMots();i++) {
               int h=-1;
               int d=g.getDep(i);
               if (d>=0) h=g.getHead(d);
               for (int j=0;j<g.getNbMots();j++) {
                   if (i!=j) {
                       int lab=h==j?1:0;
                       int nf = getFeats(g,i,j);
                       fc.addInst(Arrays.copyOf(feats, nf), lab, g.getNbMots());
                   }
               }
           }
       }
       int[] dist = fc.getLabEmpiricalDistrib();
       System.out.println("distrib of labs: "+Arrays.toString(dist));
       int ntot0 = dist[0];
       int ntot1 = dist[1];
       /*
        * - For each erroneous example in the corpus:
        *   - update the weights by alpha * (score(gold lab) - score(best lab))
        */
       final int niters = 10000000;
       final float epsilon = 1f;
       // if epsilon is smaller than one, then the total margin will tend to decrease a bit, might be overtraining ?
       final float minMargin = 1f; // minimum margin in the difference of score to consider the example as correctly classified
       // the base Margin does not seem to be influencial at all
       {
           // initialize curves with init point
           float totalMargin=0;
           for (int i=0;i<fc.getNex();i++) {
               int[] obs = fc.getFeats(i);
               int goldLab = fc.getLab(i);
               int recLab = model.getLabel(obs);
               float margin;
               if (recLab!=goldLab)
                   // The margin is <0 if the gold label is ranked below than the label with the highest score
                   margin = model.scorePerLab[goldLab] - model.scorePerLab[recLab];
               else {
                   // look for the second best score
                   float secondBestScore = Float.MIN_VALUE;
                   for (int j=0;j<model.scorePerLab.length;j++)
                       if (j!=recLab && model.scorePerLab[j]>secondBestScore) secondBestScore=model.scorePerLab[j];
                   // here the margin is >=0
                   margin = model.scorePerLab[recLab] - secondBestScore;
               }
               totalMargin+=margin;
           }
           CurvePlotter.addPoint(marginPlot, 0, totalMargin);
           double uasTest=testAndCalcUAS(gsTest, c.getTest());
           CurvePlotter.addPoint(uasPlotTest, 0, uasTest);
           double uasTrain=testAndCalcUAS(gsTrain, c.getTrain());
           CurvePlotter.addPoint(uasPlotTrain, 0, uasTrain);
           List<float[]> scores = parseAndGetScores(gsTrain);
           float risk=r.getRisk(scores);
           CurvePlotter.addPoint(riskPlot, 0, risk);
       }
       
       for (int iter=1;iter<niters;iter++) {
           int nok=0, nok0=0, nok1=0;
           float totalMargin=0;
           for (int i=0;i<fc.getNex();i++) {
               int nw=fc.getSentenceLength(i)-1;
               int[] obs = fc.getFeats(i);
               int goldLab = fc.getLab(i);
               /*
                * Use an adaptive margin that becomes larger for the ARC class when imbalance is more and more in favor of NOARC
                */
               float margin2use=goldLab==0?minMargin:minMargin*nw*nw;
               int recLab = model.getLabel(obs);
               float margin;
               if (recLab!=goldLab)
                   // The margin is <0 if the gold label is ranked below than the label with the highest score
                   margin = model.scorePerLab[goldLab] - model.scorePerLab[recLab];
               else {
                   // look for the second best score
                   float secondBestScore = Float.MIN_VALUE;
                   for (int j=0;j<model.scorePerLab.length;j++)
                       if (j!=recLab && model.scorePerLab[j]>secondBestScore) secondBestScore=model.scorePerLab[j];
                   // here the margin is >=0
                   margin = model.scorePerLab[recLab] - secondBestScore;
               }
               totalMargin+=margin;
               if (margin<margin2use) {
                   // this example is considered as badly classified
                   for (int activeFeat : obs) {
                       model.w[model.getWidx(activeFeat, goldLab)] += epsilon;
                       model.w[model.getWidx(activeFeat, recLab)] -= epsilon;
                   }
               } else {
                   nok++;
                   if (goldLab==0) nok0++; else nok1++;
               }
           }
           float acc = (float)nok/(float)fc.getNex();
           float acc0 = (float)nok0/(float)ntot0;
           float acc1 = (float)nok1/(float)ntot1;
//           CurvePlotter.addPoint(accPlot, iter, acc);
//           CurvePlotter.addPoint(acc0Plot, iter, acc0);
//           CurvePlotter.addPoint(acc1Plot, iter, acc1);
           
           CurvePlotter.addPoint(marginPlot, iter, totalMargin);
           
           double uasTest=testAndCalcUAS(gsTest, c.getTest());
           CurvePlotter.addPoint(uasPlotTest, iter, uasTest);
           double uasTrain=testAndCalcUAS(gsTrain, c.getTrain());
           CurvePlotter.addPoint(uasPlotTrain, iter, uasTrain);
           List<float[]> scores = parseAndGetScores(gsTrain);
           float risk=r.getRisk(scores);
           CurvePlotter.addPoint(riskPlot, iter, risk);
           
           float sumOfWeights = model.getSumWeights();
           System.out.println("iter "+iter+" ACC "+acc+" "+acc0+" "+acc1+" UAS "+uasTest+" "+uasTrain+" RISK "+risk+" SoW "+sumOfWeights+" Margin "+totalMargin);
       }
   }
   
    // =====================================================
    
   // this one is so slow, it should never be used. Just for debugging purpose.
   public void trainRandomSample(Corpus c) {
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
       RiskEstimator r = new RiskEstimator(priors);
       for (int i=0;i<niters;i++) {
           model.randomInit();
           double uas=testAndCalcUAS(gsTrain, c.getTrain());
           if (uas>bestTrainUas) {
               bestTrainUas=uas;
               uas=testAndCalcUAS(gsTest, c.getTest());
               List<float[]> scores = parseAndGetScores(gsTrain);
               float risk=r.getRisk(scores);
               System.out.println("UAS "+i+" "+bestTrainUas+" "+uas+" "+risk);
           }
       }
   }

    static void test1() {
        GraphsCorpus c = new GraphsCorpus();
        MSTParser m = new MSTParser();
        m.initParser(c);
        m.trainPerceptronSupervised(c);
//        m.trainUnsup(c);
    }
    
    /**
     * Options for unsupervised training with the risk:
     * - Finite difference gradient descent 
     * - EM with analytical gradient of risk a.f.o. mu,sigma
     * 
     * @param args
     */
    public static void main(String args[]) {
        test1();
    }
}
