package corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.FileUtils;

import jsafran.DetGraph;

/**
 * This is the complete WSJ section of the PTB
 * 
 * To compute WSJ10, D. Klein says that he removes ponctuation first and obtain 7422 sentences.
 * This works ! I obtain the very same 7422 sentences !
 * 
 * @author xtof
 *
 */
public class WSJPennTreeBank {
    final static String dir = "/home/xtof/nas1/TALC/ExternalResources/Reference/Corpora/TreeBanks/English/PENN_TREEBANK/package/treebank_3/parsed/mrg/wsj";

    enum derniervut {none, leftpar, rightpar};

    public List<DetGraph> load() {
        ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
        try {
            File[] fs = FileUtils.getAllFilesRecurs(dir, ".mrg");
            int nsent=0;
            for (File f : fs) {
                BufferedReader bf = new BufferedReader(new FileReader(f));
                int npar=0;
                derniervut der = derniervut.none;
                int leftpos=-1;
                ArrayList<String> words = new ArrayList<String>();
                ArrayList<String> pos = new ArrayList<String>();
                for (;;) {
                    String s=bf.readLine();
                    if(s==null) break;
                    for (int i=0;i<s.length();i++) {
                        // this incremental way of processing sentences is a bit slow, but it's easier to program...
                        char c = s.charAt(i);
                        if (c=='(') {
                            der=derniervut.leftpar;
                            leftpos=i;
                            npar++;
                        } else if (c==')') {
                            if (der==derniervut.leftpar) {
                                // terminal
                                int j=s.indexOf(' ',leftpos);
                                String w = s.substring(j+1,i);
                                words.add(w);
                                String p = s.substring(leftpos+1,j);
                                pos.add(p);
                            }
                            der=derniervut.rightpar;
                            npar--;
                            if (npar==0) {
                                // End of sentence
                                nsent++;
                                DetGraph g = new DetGraph(words.toArray(new String[words.size()]));
                                for (int j=0;j<g.getNbMots();j++)
                                    g.getMot(j).setPOS(pos.get(j));
                                if (g.getNbMots()>0) gs.add(g);
                                words.clear();
                                pos.clear();
                            }
                        }
                    }
                }
                bf.close();
            }
            System.out.println(nsent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gs;
    }
    
    void delponct(List<DetGraph> gs) {
        String[] wordTags = {"CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT",
                "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB"
        };
        Arrays.sort(wordTags);
        for (int i=0;i<gs.size();i++) {
            DetGraph g = gs.get(i);
            for (int j=g.getNbMots()-1;j>=0;j--)
                if (Arrays.binarySearch(wordTags, g.getMot(j).getPOS())<0) g.delNodes(j, j);
        }
    }
    
    List<DetGraph> filter10(List<DetGraph> gs) {
        ArrayList<DetGraph> res = new ArrayList<DetGraph>();
        for (int i=0;i<gs.size();i++) {
            DetGraph g = gs.get(i);
            if (g.getNbMots()<=10) res.add(g);
        }
        return res;
    }
    
    public static List<DetGraph> getWSJ10() {
        WSJPennTreeBank c = new WSJPennTreeBank();
        List<DetGraph> gs = c.load();
        System.out.println(gs.size());
        c.delponct(gs);
        List<DetGraph> wsj10 = c.filter10(gs);
        return wsj10;
    }
    
    public static void main(String args[]) {
        WSJPennTreeBank c = new WSJPennTreeBank();
        List<DetGraph> gs = c.load();
        System.out.println(gs.size());
        c.delponct(gs);
        List<DetGraph> wsj10 = c.filter10(gs);
        System.out.println(wsj10.size());
    }
}
