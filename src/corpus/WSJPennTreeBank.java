package corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import utils.FileUtils;

import jsafran.DetGraph;

/**
 * This is the complete WSJ section of the PTB
 * 
 * @author xtof
 *
 */
public class WSJPennTreeBank {
    final static String dir = "/home/xtof/nas1/TALC/ExternalResources/Reference/Corpora/TreeBanks/English/PENN_TREEBANK/package/treebank_3/parsed/mrg/wsj";
    
    public List<DetGraph> load() {
        ArrayList<DetGraph> gs = new ArrayList<DetGraph>();
        try {
            File[] fs = FileUtils.getAllFilesRecurs(dir, ".mrg");
            int nsent=0;
            for (File f : fs) {
                BufferedReader bf = new BufferedReader(new FileReader(f));
                int npar=0;
                for (;;) {
                    String s=bf.readLine();
                    if(s==null) break;
                    for (int i=0;i<s.length();i++) {
                        char c = s.charAt(i);
                        if (c=='(') npar++;
                        else if (c==')') {
                            npar--;
                            if (npar==0) {
                                // End of sentence
                                nsent++;
                            }
                        }
                    }
                }
                bf.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gs;
    }
    
    public static void main(String args[]) {
        WSJPennTreeBank c = new WSJPennTreeBank();
        List<DetGraph> gs = c.load();
        System.out.println(gs.size());
    }
}
