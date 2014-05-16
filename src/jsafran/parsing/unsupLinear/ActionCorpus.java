package jsafran.parsing.unsupLinear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsafran.DetGraph;
import jsafran.parsing.unsupLinear.TransitionParser.actionDecider;
import jsafran.parsing.unsupLinear.TransitionParser.OneAction;

/**
 * Ideally, training a parser shall be done with the objective of maximizing the UAS, but this implies to reparse the whole corpus
 * for each dimension (finite difference candidate move) and at each iteration of gradient.
 * This is far too slow. So the trick is to compute the ideal actions, and rather project the problem of parsing into a problem of
 * classifying predefined features into these oracle actions. This is suboptimal, but way faster.
 * 
 * This class is the result of such a projection.
 * 
 * @author xtof
 *
 */
public class ActionCorpus implements Corpus {
    int[][] features;
    int[] actions;
    GraphsCorpus gc;
    
    public void buildCorpus(final TransitionParser parser) {
        gc = new GraphsCorpus();
        gc.buildCorpus(parser);
        buildCorpus(parser, gc.gsTrain);
    }
    private void buildCorpus(final TransitionParser parser, final List<DetGraph> gold) {
        final ArrayList<Integer> labs = new ArrayList<Integer>();
        final ArrayList<int[]> fts = new ArrayList<int[]>();
        
        ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
        for (int i=0;i<gold.size();i++) {
            DetGraph g = gold.get(i).clone();
            g.cursent=i;
            gs.add(g);
        }
        for (DetGraph g : gs) {
            g.clearDeps();
            parser.parse(g, new actionDecider() {
                @Override
                public OneAction getAction(DetGraph grec) {
                    DetGraph g = gold.get(grec.cursent);
                    OneAction res=jsafran.parsing.unsupLinear.TransitionParser.OneAction.SHIFT;
                    if (parser.nstack>0) {
                        int st = parser.stack[parser.nstack-1];
                        int in = parser.input[parser.ninput-1];
                        int d = g.getDep(st);
                        if (d>=0) {
                            // stack has a head
                            int sthd = g.getHead(d);
                            if (sthd==in) res=jsafran.parsing.unsupLinear.TransitionParser.OneAction.LA;
                        }
                        if (res==jsafran.parsing.unsupLinear.TransitionParser.OneAction.SHIFT) {
                            d = g.getDep(in);
                            if (d>=0) {
                                // input has a head
                                int inhd = g.getHead(d);
                                if (inhd==st) {
                                    res=jsafran.parsing.unsupLinear.TransitionParser.OneAction.RA;
                                }
                            }
                            if (res==jsafran.parsing.unsupLinear.TransitionParser.OneAction.SHIFT) {
                                d = g.getDep(st);
                                if (d>=0) {
                                    // stack has a head
                                    int leftdep = g.getLeftmostSubtreeNode(in);
                                    if (leftdep>=0&&leftdep<st) {
                                        res=jsafran.parsing.unsupLinear.TransitionParser.OneAction.REDUCE;
                                    } else {
                                        d = g.getDep(in);
                                        if (d>=0) {
                                            // input has a head
                                            int inhd = g.getHead(d);
                                            if (inhd<st) res=jsafran.parsing.unsupLinear.TransitionParser.OneAction.REDUCE;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    int nfeats = parser.getFeats(grec);
                    int[] feats = Arrays.copyOf(parser.getFeats(),nfeats);
                    fts.add(feats);
                    // WARNING: actions are coded from 0, while in LIBINEAR, they're coded from 1 !!
                    int y = res.ordinal();
                    labs.add(y);
                    return res;
                }
            });
        }
        features = new int[labs.size()][];
        actions = new int[labs.size()];
        for (int i=0;i<actions.length;i++) {
            actions[i] = labs.get(i);
            features[i]=fts.get(i);
        }
        System.out.println("corpus built");
    }
    @Override
    public List<DetGraph> getTest() {
        return gc.getTest();
    }
    @Override
    public List<DetGraph> getTrain() {
        return gc.getTrain();
    }
}
