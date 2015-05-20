package jsafran.parsing;

import jsafran.DetGraph;
import jsafran.JSafran;
import jsafran.Mot;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by xtof on 5/20/15.
 */
public class StanfordParser {
    private static int getGovIdx(String l) {
        if (l.startsWith("Parent = ")) return -1;
        int i=l.lastIndexOf('-');
        if (i<0) return -1;
        int j=l.indexOf('/',i);
        assert j>=0;
        return Integer.parseInt(l.substring(i+1,j));
    }
    private static String getGovStr(String l) {
        if (l.startsWith("Parent = ")) return null;
        int i=l.lastIndexOf('-');
        if (i<0) return null;
        int j=l.indexOf(',');
        assert j>=0;
        String s = l.substring(j+1,i).trim();
        return s;
    }
    private static String getPostagGov(String l) {
        if (l.startsWith("Parent = ")) return null;
        int j=l.lastIndexOf('/');
        int i=l.indexOf(')',j);
        String s = l.substring(j+1,i).trim();
        return s;
    }
    private static int getHeadIdx(String l) {
        if (l.startsWith("Parent = ")) return -1;
        int j=l.indexOf('/');
        int i=l.lastIndexOf('-', j);
        return Integer.parseInt(l.substring(i+1,j));
    }
    private static String getHeadStr(String l) {
        if (l.startsWith("Parent = ")) return null;
        int j=l.indexOf('/');
        int i=l.lastIndexOf('-', j);
        j=l.indexOf('(');
        assert j>=0;
        String s = l.substring(j+1,i).trim();
        return s;
    }
    private static String getPostagHead(String l) {
        if (l.startsWith("Parent = ")) return null;
        int j=l.indexOf('/');
        int i=l.indexOf(',', j);
        String s = l.substring(j+1,i).trim();
        return s;
    }
    private static String getDep(String l) {
        if (l.startsWith("Parent = ")) return null;
        int i=l.indexOf('(');
        return l.substring(0,i);
    }

    public static DetGraph parse(String u) {
        Stanford_Dep_Parser_CollapsedTree parser = new Stanford_Dep_Parser_CollapsedTree(u);
        String tree = parser.get_collapsed_dependency_parse_with_POS_tags_string();
        String[] lines = tree.split("\n");
        int nw=0;
        for (String l : lines) {
            int govidx = getGovIdx(l);
            if (govidx>0) { // 0 is for root
                int headidx = getHeadIdx(l);
                if (govidx>nw) nw=govidx;
                if (headidx>nw) nw=headidx;
            }
        }
        String[] words = new String[nw];
        String[] postags = new String[nw];
        ArrayList<Integer> gov = new ArrayList<Integer>();
        ArrayList<Integer> head = new ArrayList<Integer>();
        ArrayList<String> dep = new ArrayList<String>();
        for (String l : lines) {
            System.out.println("debugstanford "+l);
            int govidx = getGovIdx(l);
            if (govidx>0) { // 0 is for root
                govidx--;
                String govstr = getGovStr(l);
                int headidx = getHeadIdx(l)-1;
                String headstr = getHeadStr(l);
                String deplab = getDep(l);
                words[govidx]=govstr;
                postags[govidx]=getPostagGov(l);
                if (headidx>=0) {
                    words[headidx]=headstr;
                    postags[headidx]=getPostagHead(l);
                }
                dep.add(deplab);
                gov.add(govidx);
                head.add(headidx);
            }
        }
        System.out.println("ww "+ Arrays.toString(words));

        DetGraph g = new DetGraph();
        HashMap<Integer,Integer> idx2wd = new HashMap<Integer, Integer>();
        for (int i=0;i<words.length;i++) {
            if (words[i]!=null) {
                int newidx = g.getNbMots();
                idx2wd.put(i, newidx);
                g.addMot(newidx,new Mot(words[i],words[i],postags[i]));
            }
        }
        for (int i=0;i<gov.size();i++) {
            if (head.get(i)>=0)
                g.ajoutDep(dep.get(i), idx2wd.get(gov.get(i)), idx2wd.get(head.get(i)));
        }
        return g;
    }

    public static JMenu getMenu(final JSafran main) {
        JMenu menu = new JMenu("Stanford parser");
        JMenuItem parse = new JMenuItem("parse with Stanford parser");
        menu.add(parse);
        parse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i=0;i<main.allgraphs.size();i++) {
                    DetGraph g = StanfordParser.parse(main.allgraphs.get(i).toString());
                    main.allgraphs.set(i,g);
                }
                main.repaint();
            }
        });
        return menu;
    }

}
