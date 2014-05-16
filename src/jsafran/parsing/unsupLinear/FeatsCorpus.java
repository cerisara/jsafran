package jsafran.parsing.unsupLinear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class FeatsCorpus implements CorpusF {
    ArrayList<int[]> features = new ArrayList<int[]>();
    ArrayList<Integer> labels = new ArrayList<Integer>();
    ArrayList<Integer> sentLen = new ArrayList<Integer>();
    
    public void addInst(int[] feats, int lab, int sentenceLength) {
        features.add(feats);
        labels.add(lab);
        sentLen.add(sentenceLength);
    }
    public int[] getLabEmpiricalDistrib() {
        HashSet<Integer> labs = new HashSet<Integer>();
        for (int l: labels) labs.add(l);
        int[] d = new int[labs.size()];
        Arrays.fill(d, 0);
        for (int l: labels) d[l]++;
        return d;
    }
    public int getNex() {return labels.size();}
    public int[] getFeats(int i) {
        return features.get(i);
    }
    public int getLab(int i) {
        return labels.get(i);
    }
    public int getSentenceLength(int i) {
        return sentLen.get(i);
    }
}
