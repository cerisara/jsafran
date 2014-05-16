package jsafran.parsing.unsupLinear;

import java.util.List;

import jsafran.DetGraph;

/**
 * Corpus that contains dependency trees
 * @author xtof
 *
 */
public interface Corpus {
    public void buildCorpus(TransitionParser parser);
    public List<DetGraph> getTest();
    public List<DetGraph> getTrain();
}

/**
 * Same Corpus but that contains features and labels only.
 * Note that sentence length is important so it is kept.
 * 
 * @author xtof
 *
 */
interface CorpusF {
    public void addInst(int[] feats, int lab, int sentenceLength);
    public int getNex();
    public int[] getFeats(int i);
    public int getLab(int i);
    public int getSentenceLength(int i);
}
