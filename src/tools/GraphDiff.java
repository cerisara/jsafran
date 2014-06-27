package tools;

import java.util.List;

import utils.Wait;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

public class GraphDiff {
    public static void main(String args[]) {
        String[] fichs = {args[0],args[1]};
        
        List<DetGraph> gs[] = new List[2];
        GraphIO gio = new GraphIO(null);
        for (int i=0;i<2;i++) {
            if (fichs[i].endsWith(".xml")) gs[i] = gio.loadAllGraphs(fichs[i]);
            else if (fichs[i].endsWith(".conll06")) gs[i] = GraphIO.loadConll06(fichs[i], false);
            else if (fichs[i].endsWith(".conll")) gs[i] = GraphIO.loadConll06(fichs[i], false);
        }
        
        JSafran jf = new JSafran();
        jf.putInLevel(gs[0], 0);
        jf.putInLevel(gs[1], 1);
        
        int ok=0, tot=0, oklab=0, totlab=0;
        for (int i=0;i<gs[0].size();i++) {
            DetGraph g0 = gs[0].get(i), g1=gs[1].get(i);
            for (int j=0;j<g0.getNbMots();j++) {
                int[] ds0 = g0.getDeps(j);
                int[] ds1 = g1.getDeps(j);
                for (int k=0;k<ds0.length;k++) {
                    int h0=g0.getHead(ds0[k]);
                    boolean isok=false, isoklab=false;
                    for (int d:ds1)
                        if (g1.getHead(d)==h0) {
                            isok=true;
                            if (g1.deps.get(d).type==g0.deps.get(ds0[k]).type) {
                                isoklab=true;
                                break;
                            }
                        }
                    if (isok) ok++;
                    if (isoklab) oklab++;
                    else jf.addMark(i, j);
                    tot++; totlab++;
                }
                for (int k=0;k<ds1.length;k++) {
                    int h1=g1.getHead(ds1[k]);
                    boolean isok=false, isoklab=false;
                    for (int d:ds0)
                        if (g0.getHead(d)==h1) {
                            isok=true;
                            if (g0.deps.get(d).type==g1.deps.get(ds1[k]).type) {
                                isoklab=true;
                                break;
                            }
                        }
                    if (!isok) tot++;
                    if (!isoklab) {
                        totlab++;
                        jf.addMark(i, j);
                    }
                }
            }
        }
        int ndiffunlab = tot-ok;
        int ndifflab = totlab-oklab;
        System.out.println("ndifflab="+ndifflab+" ndiffunlab="+ndiffunlab+" ntotlab="+totlab+" ntotunlab="+tot);
        jf.initGUI();
        Wait.waitUser();
    }
}
