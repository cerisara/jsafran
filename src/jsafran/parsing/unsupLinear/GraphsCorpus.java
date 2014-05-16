package jsafran.parsing.unsupLinear;

import java.util.List;

import jsafran.DetGraph;
import jsafran.GraphIO;

/**
 * Directly implements a corpus that uses dependency trees
 * 
 * TODO: the graphs in this corpus MUST NOT be modified. How to prevent that ?
 * 
 * @author xtof
 */
public class GraphsCorpus implements Corpus {
    List<DetGraph> gsTest, gsTrain;
    
    public GraphsCorpus() {
        GraphIO gio = new GraphIO(null);
        gsTest = gio.loadAllGraphs("test2009.xml");
        gsTrain = gio.loadAllGraphs("train2011.xml");
        System.out.println("corpus loaded "+gsTrain.size()+" "+gsTest.size());
        // filter-out sentences with only one word in the training
        for (int i=gsTrain.size()-1;i>=0;i--) {
            if (gsTrain.get(i).getNbMots()<=1) gsTrain.remove(i);
        }
        
        // debug
        // gsTrain=gsTrain.subList(0, 100);
        
        System.out.println("corpus filter "+gsTrain.size()+" "+gsTest.size());
    }
    
    public void buildCorpus(final TransitionParser parser) {
        parser.samplePaths(gsTrain);
        parser.samplePaths(gsTest);
    }

    @Override
    public List<DetGraph> getTest() {
        return gsTest;
    }

    @Override
    public List<DetGraph> getTrain() {
        return gsTrain;
    }
}
