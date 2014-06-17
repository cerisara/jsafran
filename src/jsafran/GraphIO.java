package jsafran;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jsafran.parsing.ClassificationResult;

import corpus.text.TextSegments;
import corpus.text.TextSegments.segtypes;

import utils.FileUtils;
import utils.Interruptable;
import utils.Wait;

public class GraphIO implements GraphProcessor {
	boolean loadedfromconll = false;
	public static final String POSD = "POSD", FEATS = "FEATS";

	JFrame jf;

	private LinkedList<DetGraph> allGraphsTmp = null;

	public File readFile = null;

	public static boolean isTxtUTF = true;

	/**
	 * 
	 * @param fich :
	 *            fichier principal
	 */
	public GraphIO(JFrame parent) {
		jf = parent;
	}

	public static List<DetGraph> loadTxt(String filename) {
		return loadTxt(filename,false,true);
	}
	public static List<DetGraph> loadTxt(String filename, boolean withPonct) {
		return loadTxt(filename,false,withPonct);
	}
	public static List<DetGraph> loadSTM(String filename,boolean withPonct) {
		return loadTxt(filename,true,withPonct);
	}
	public static boolean withProgress = true;
	private static List<DetGraph> loadTxt(String filename, boolean isSTM, final boolean withPonct) {
		System.err.println("GraphIO.loadTxt()");
		final TextSegments segs = new TextSegments();
		if (isSTM) segs.preloadSTMFile(filename,isTxtUTF);
		else segs.preloadTextFile(filename);
		System.err.println("phrases found "+segs.getNbSegments());

		final List<DetGraph> gdeps = new ArrayList<DetGraph>();
		if (withProgress) {
			final utils.ProgressDialog waiting = new utils.ProgressDialog((JFrame)null,null,"please wait: loading...");
			Interruptable searchingproc = new Interruptable() {
				private boolean tostop=false;
				public void stopit() {
					tostop=true;
				}
				@Override
				public void run() {
					try {
						for (int i=0;i<segs.getNbSegments();i++) {
							if (tostop) break;
							waiting.setProgress((float)i/(float)segs.getNbSegments());
							DetGraph g = new DetGraph();
							g.cursent=i;
							g.setSource(segs.getSource().toURL());
							gdeps.add(g);
							int motidx=0;
							TextSegments segsmots = segs.tokenizeBasic(i);
							segsmots.tokenizePonct();
							segsmots.tokenizeComments();
							for (int j=0;j<segsmots.getNbSegments();j++) {
								if (segsmots.getSegmentType(j)==segtypes.mot ||
										(withPonct && segsmots.getSegmentType(j)==segtypes.ponct)) {
									Mot m = new Mot(segsmots.getSegment(j).trim(), 0);
									m.setPosInTxt(segsmots.getSegmentStartPos(j), segsmots.getSegmentEndPos(j));
									g.addMot(motidx++,m);
								}
							}
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			};
			waiting.setRunnable(searchingproc);
			waiting.setVisible(true);
		} else {
			try {
				for (int i=0;i<segs.getNbSegments();i++) {
					DetGraph g = new DetGraph();
					g.cursent=i;
					g.setSource(segs.getSource().toURL());
					gdeps.add(g);
					int motidx=0;
					TextSegments segsmots = segs.tokenizeBasic(i);
					segsmots.tokenizePonct();
					segsmots.tokenizeComments();
					for (int j=0;j<segsmots.getNbSegments();j++) {
						if (segsmots.getSegmentType(j)==segtypes.mot ||
								(withPonct && segsmots.getSegmentType(j)==segtypes.ponct)) {
							Mot m = new Mot(segsmots.getSegment(j).trim(), 0);
							m.setPosInTxt(segsmots.getSegmentStartPos(j), segsmots.getSegmentEndPos(j));
							g.addMot(motidx++,m);
						}
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return gdeps;
	}
	/**
	 * @deprecated remplacé par TextSegments
	 * @param filename
	 * @return
	 */
	public static List<DetGraph> oldloadTxt(String filename) {
		System.err.println("loading texte");
		List<DetGraph> gdeps = new ArrayList<DetGraph>();
		String s=null;
		final char[] delims = {' ','\t','-'};
		try {
			BufferedReader f = FileUtils.openFileUTF(filename);
			File ff = new File(filename);
			URL srcurl = ff.toURI().toURL();
			long pos=0;
			for (int cursent=0;;cursent++) {
				s=f.readLine();
				if (s==null) break;
				DetGraph gdep = new DetGraph();
				gdep.setSource(srcurl);
				int c=0, motidx=0;
				while (c<s.length()) {
					int d=s.length();
					for (char x : delims) {
						int i = s.indexOf(x,c+1);
						if (i>=0&&i<d) d=i;
					}
					String mot = s.substring(c,d).trim();
					if (mot.length()>0) {
						// nouveau mot
						Mot m = new Mot(mot, 0);
						m.setPosInTxt(pos+c, pos+d);
						gdep.addMot(motidx++, m);
					}
					c=d;
				}
				gdeps.add(gdep);
				pos+=s.length();
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("ERREUR pour "+s);
			e.printStackTrace();
		}
		return gdeps;
	}

	public static List<DetGraph> loadConll06(String filename, boolean addAgreement) {
		List<DetGraph> gdeps = new ArrayList<DetGraph>();
		String s=null;
		try {
			BufferedReader f = FileUtils.openFileUTF(filename);
			DetGraph gdep = new DetGraph();
			int motidx=0;
			ArrayList<Integer> onedeps = new ArrayList<Integer>();
			ArrayList<String> onedepslabs = new ArrayList<String>();
			for (;;) {
				s=f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) {
					if (gdep.getNbMots()>0) {
						// fin de phrase
						assert gdep.getNbMots()==onedeps.size();
						assert onedeps.size()==onedepslabs.size();
						for (int i=0;i<onedeps.size();i++) {
							if (onedeps.get(i)>=0)
								gdep.ajoutDep(onedepslabs.get(i), i, onedeps.get(i));
						}
						gdeps.add(gdep);
					}
					gdep=new DetGraph();
					motidx=0;
					onedeps.clear(); onedepslabs.clear();
					continue;
				}
				StringTokenizer st = new StringTokenizer(s,"\t");
				String col = st.nextToken(); // col1 = numero du mot
				col = st.nextToken(); // col2 = forme
				String mot=""+col;
				col = st.nextToken(); // col3 = lemme
				String lemme=""+col;
				col = st.nextToken(); // col4 = pos1
				String postag=""+col;
				Mot m = new Mot(mot, lemme, postag);
				col = st.nextToken(); // col5 = pos2 (plus précis)
				if (!col.equals("_")) m.addField(POSD, col);
				col = st.nextToken(); // col6 = genre,nb,pers,mode,...
                if (!col.equals("_")) m.addField(FEATS, col);
				if (addAgreement) {
					// je suppose qu'on a le format du FTB
					String ag = getAgreement(col);
					if (ag!=null) m.setPOS(m.getPOS()+ag);
				}
				gdep.addMot(motidx, m);
				col = st.nextToken(); // col7 = dep
				onedeps.add(Integer.parseInt(col)-1);
				col = st.nextToken(); // col8 = deplab
				onedepslabs.add(col);
				motidx++;
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("ERREUR pour "+s);
			e.printStackTrace();
		}
		return gdeps;
	}

	private static String getAgreement(String s) {
		String[] ss = s.split("\\|");
		String ag = "";
		for (String x : ss) {
			if (x.startsWith("n=")) {
				// nombre
				ag+=":"+x;
			} else if (x.startsWith("g=")) {
				// genre
				ag+=":"+x;
			}
		}
		return ag;
	}

	/**
	 * 
	 * @param filename
	 * @return 2 sets of graphs: one for deps, one for sem. roles
	 */
	public static List<DetGraph>[] loadConll08(String filename) {
		List<DetGraph> gdeps = new ArrayList<DetGraph>();
		List<DetGraph> groles = new ArrayList<DetGraph>();
		String s=null;
		try {
			BufferedReader f = FileUtils.openFileUTF(filename);
			DetGraph gdep = new DetGraph();
			DetGraph gsrl;
			int motidx=0;
			ArrayList<Integer> onedeps = new ArrayList<Integer>();
			ArrayList<String> onedepslabs = new ArrayList<String>();
			ArrayList<Integer> predicats = new ArrayList<Integer>();
			ArrayList<String[]> args = new ArrayList<String[]>();
                        ArrayList<String> sensee = new ArrayList<String>();
			for (;;) {
				s=f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) {
					if (gdep.getNbMots()>0) {
						// fin de phrase
						assert gdep.getNbMots()==onedeps.size();
						assert onedeps.size()==onedepslabs.size();
						assert onedeps.size()==args.size();
						gsrl = gdep.getSubGraph(0);
                                                for (int i=0;i<gsrl.getNbMots();i++) gsrl.getMot(i).setPOS("_"); //to load here the sense of the verb...
						for (int i=0;i<onedeps.size();i++) {
							if (onedeps.get(i)>=0) // sauf le ROOT
								gdep.ajoutDep(onedepslabs.get(i), i, onedeps.get(i));
							assert args.get(i).length==predicats.size();
							for (int j=0;j<args.get(i).length;j++) {
								if (!args.get(i)[j].equals("_")) {
									gsrl.ajoutDep(args.get(i)[j], i, predicats.get(j));
								}
								gsrl.getMot(i).setPOS(sensee.get(i));
							}
						}
						gdeps.add(gdep);
						groles.add(gsrl);
					}
					gdep=new DetGraph();
					motidx=0;
					onedeps.clear(); onedepslabs.clear();
					predicats.clear(); args.clear();
                                        sensee.clear();
					continue;
				}
				StringTokenizer st = new StringTokenizer(s,"\t");
				String col = st.nextToken(); // col1 = numero du mot
				col = st.nextToken(); // col2 = forme
				String mot=""+col;
				col = st.nextToken(); // col3 = lemme
				String lemme=""+col;
				col = st.nextToken(); // col4 = pos1
				String postag=""+col;
				Mot m = new Mot(mot,lemme,postag);
				col = st.nextToken(); // col5 = pos2
				col = st.nextToken(); // col6 = forme ?
				col = st.nextToken(); // col7 = lemme ?
				col = st.nextToken(); // col8 = pos ?
				gdep.addMot(motidx, m);
				col = st.nextToken(); // col9 = dep
				onedeps.add(Integer.parseInt(col)-1);
				col = st.nextToken(); // col10 = deplab
				onedepslabs.add(col);
				col = st.nextToken(); // col11 = predicats
                                sensee.add(col);
				if (!col.equals("_")) predicats.add(motidx);
				ArrayList<String> arg = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					col = st.nextToken(); // col12+ = arguments
					arg.add(col);
				}
				String[] targ = new String[arg.size()];
				arg.toArray(targ);
				args.add(targ);
				motidx++;
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("ERREUR pour "+s);
			e.printStackTrace();
		}
		List gs[] = {gdeps,groles};
		return gs;
	}
	/**
         * FrameNet format in DetGraphs: 
         * - the frames spans are in groups, where <<predType-predId>> 
         *   where predType is the frame name and predId is numbered with the number of the predicate in the sentence (0,1,2, ..)
         * - the argument spans are in groups, where each group is labeled with: <<argType-argId>> 
         *   where argType is the role and argId is numbered with the number of argument in the sentence (0,1,2, ..)
         * - the relations (depLabel) are labeled: <<argType-argId-predType-predId>>, 
         *   where argType and argId idem as argument groups, and predType and predId idem as frames gropus
	 * @param filename
	 * @return 2 sets of graphs: one for deps, one for sem. roles
	 */
	public static List<DetGraph>[] loadConll08FrameNet(String filename) {
                int numArgsInUtt=0;
                int numPredsInUtt=0;
		List<DetGraph> gdeps = new ArrayList<DetGraph>();
		List<DetGraph> groles = new ArrayList<DetGraph>();
		String s=null;
		try {
			BufferedReader f = FileUtils.openFileUTF(filename);
			DetGraph gdep = new DetGraph();
			DetGraph gsrl;
			int motidx=0;
			ArrayList<Integer> onedeps = new ArrayList<Integer>();
			ArrayList<String> onedepslabs = new ArrayList<String>();
			ArrayList<Integer> predicats = new ArrayList<Integer>();
			//ArrayList<String[]> args = new ArrayList<String[]>();
                        ArrayList<ArrayList<String>> argslist = new ArrayList<ArrayList<String>>();
                        ArrayList<ArrayList<Integer>> argslistPos = new ArrayList<ArrayList<Integer>>();
                        ArrayList<ArrayList<int[]>> argslistPosSegment = new ArrayList<ArrayList<int[]>>();
                        //to keep track of the argument spans
                        ArrayList<String> predsInComments=new ArrayList<String>();
                        ArrayList<int[]> predsInCommentsPos=new ArrayList<int[]>();
                        ArrayList<ArrayList<String>> argsIncomments=new ArrayList<ArrayList<String>>();
                        ArrayList<ArrayList<int[]>> argsIncommentsPos=new ArrayList<ArrayList<int[]>>();
                        ArrayList<String>args=new ArrayList<String>();
                        ArrayList<int[]>argsPos=new ArrayList<int[]>();
                        int lineNumber=0;
			for (;;) {
                                lineNumber++;
                                int numColumn=0;
				s=f.readLine();
				if (s==null) break;
                                if (s.startsWith("# RID:")){
                                    continue;
                                }
                                if (s.startsWith("# Mapped annotation")){
                                    continue;
                                }
                                
                                //process coments, in framenet contain the info about the groups or argument spans 
                                /*  # RID: 559
                                    # Frame "Communication"
                                    #     FEE: 4
                                    #     Communicator: 2
                                    #     Message: 6, 7, 8, 9, 10, 11, 12, 13 */
                                //if (lineNumber<6) continue;
                                if (s.startsWith("SKIPPED")) continue;    
                                if (s.startsWith("#")){
                                    if (s.startsWith("# Mapped annotation onto")){
//                                        readMap=true;
                                        continue;
                                    }
                                    
                                    if (!s.startsWith("# RID:")){
                                        if (s.startsWith("# Frame")){
                                            predsInComments.add(s.split(" ")[2].replaceAll("\"", "")+"--"+numPredsInUtt);
                                            numPredsInUtt++;
                                            args=new ArrayList<String>();
                                            argsPos=new ArrayList<int[]>();
                                            argsIncomments.add(args);
                                            argsIncommentsPos.add(argsPos);
                                        }
                                        else {
                                            if (s.startsWith("#     FEE:")){
                                                int[] span=new int[s.split("\\W+").length-2];
                                                for (int indw=2;indw<s.split("\\W+").length;indw++){
                                                    //System.out.println("index:"+indw+"--"+Integer.parseInt(s.split("\\W+")[indw].trim())+"   --span:"+Arrays.toString(span));
                                                    span[indw-2]=Integer.parseInt(s.split("\\W+")[indw].trim())-1;
                                                }
                                                //predsInCommentsPos.add(Integer.parseInt(s.substring(s.indexOf(":")+1).trim()));
                                                predsInCommentsPos.add(span);
                                                
                                            ArrayList<String> arggg=new ArrayList<String>();
                                            ArrayList<Integer> argggApos=new ArrayList<Integer>();
                                            ArrayList<int[]> argggPosSegment=new ArrayList<int[]>();
                                            argslist.add(arggg);
                                            argslistPos.add(argggApos);
                                            argslistPosSegment.add(argggPosSegment);
                                                
                                            }
                                            else{ //it is an argument... we can extract the span
                                                if (s.split("[\\:\\,\\#]+\\s*").length<2) continue; //"\\W+"
                                                //add the element in //"\\s+"
                                                //int idArg=argsIncomments.get(predsInComments.size()-1).size();
                                                String argName=s.split("[\\:\\,\\#]+\\s*")[1]+"--"+numArgsInUtt;
                                                numArgsInUtt++;
                                                ArrayList<String>cura=argsIncomments.get(predsInComments.size()-1);
                                                cura.add(argName);
                                                argsIncomments.set(predsInComments.size()-1, cura);
                                                //args.add(argName);
                                                //get the args spans
                                                //System.out.println("array:"+Arrays.toString(s.split("\\W+"))+" -->"+s.split("\\W+")[2]);
                                                int[] span=new int[s.split("[\\:\\,\\#]+\\s*").length-2];
                                                for (int indw=2;indw<s.split("[\\:\\,\\#]+\\s*").length;indw++){
                                                    //System.out.println("index:"+indw+"--"+Integer.parseInt(s.split("\\W+")[indw].trim())+"   --span:"+Arrays.toString(span));
                                                    if(s.split("[\\:\\,\\#]+\\s*")[indw].trim().length()>0) span[indw-2]=Integer.parseInt(s.split("[\\:\\,\\#]+")[indw].trim())-1;
                                                }
                                                //see if the span is not continous
                                                int last=0;
                                                int indBreak=-1;
                                                for (int indw=0;indw<span.length;indw++){
                                                    if (indw==0) {
                                                        last= span[indw];
                                                    }
                                                    else {
                                                        if (span[indw]!=last+1) {
                                                            indBreak=indw;
                                                            break;
                                                        }else {
                                                            last=span[indw];
                                                        }
                                                    }
                                                }
                                                if (indBreak!=-1){
                                                    //TODO: what should I do now??? create two arguments???? where should I add the links??
                                                    int[]firstPart=new int[indBreak];
                                                    int[]secondPart=new int[span.length-indBreak];
                                                    for (int indw=0;indw<span.length;indw++){
                                                        if (indw<indBreak) firstPart[indw]=span[indw];
                                                        else{
                                                            secondPart[indw-indBreak]=span[indw];
                                                        }
                                                    }
                                                    argName=s.split("[\\:\\,\\#]+\\s*")[1]+"--"+numArgsInUtt;//args.size();
                                                    numArgsInUtt++;
                                                    cura=argsIncomments.get(predsInComments.size()-1);
                                                    cura.add(argName);
                                                    argsIncomments.set(predsInComments.size()-1, cura);
                                                    //args.add(argName);
                                                    //argsPos.add(firstPart);
                                                    //argsPos.add(secondPart);
                                                    ArrayList<int[]>curaPos=argsIncommentsPos.get(predsInCommentsPos.size()-1);
                                                    curaPos.add(firstPart);
                                                    curaPos.add(secondPart);
                                                    argsIncommentsPos.set(predsInCommentsPos.size()-1, curaPos);
                                                }else{
                                                    //argsPos.add(span);
                                                    ArrayList<int[]>curaPos=argsIncommentsPos.get(predsInCommentsPos.size()-1);
                                                    curaPos.add(span);
                                                    argsIncommentsPos.set(predsInCommentsPos.size()-1, curaPos);
                                                    
                                                }
                                                
                                            }
                                        }                                            
                                    }   
                                }else{
                                    s=s.trim();
                                    if (s.length()==0) {
                                            if (gdep.getNbMots()>0) {
                                                    // fin de phrase
                                                    assert gdep.getNbMots()==onedeps.size();
                                                    assert onedeps.size()==onedepslabs.size();
                                                    assert onedeps.size()==argslist.size();
                                                    gsrl = gdep.getSubGraph(0);
                                                    for (int i=0;i<onedeps.size();i++) {
                                                            if (onedeps.get(i)>=0) // sauf le ROOT
                                                                    gdep.ajoutDep(onedepslabs.get(i), i, onedeps.get(i));
                                                    }
                                                    //for (int pp=0;pp<predicats.size();pp++){
                                                    for (int pp=0;pp<predicats.size();pp++){    
                                                        //System.out.println("predis:"+predicats.toString()+" args:"+argslist);
                                                        int firstMot=predsInCommentsPos.get(pp)[0];
                                                        int lastMot=predsInCommentsPos.get(pp)[predsInCommentsPos.get(pp).length-1];
                                                        String groupName=predsInComments.get(pp);
                                                        //System.out.println("first mot:"+firstMot+" lastMOt:"+lastMot+ " groupName:"+groupName+ " Num mots: "+gsrl.getNbMots()+"num groups:"+gsrl.getNbGroups());
                                                        gsrl.addgroup(firstMot, lastMot, ""+groupName);
                                                        assert argslist.get(pp).size()==predicats.size();
                                                        for (int aa=0;aa<argslist.get(pp).size();aa++){
                                                            if (!argslist.get(pp).get(aa).equals("_")) {
//                                                                System.out.println("Add link from: "+ argslistPos.get(pp).get(aa)+ " to: "+predicats.get(pp)+" "+gsrl.deps.size());
                                                                gsrl.ajoutDep(argslist.get(pp).get(aa), argslistPos.get(pp).get(aa), predicats.get(pp));
                                                                //add the group for the argument span
                                                                firstMot=argslistPosSegment.get(pp).get(aa)[0];
                                                                lastMot=argslistPosSegment.get(pp).get(aa)[argslistPosSegment.get(pp).get(aa).length-1];
                                                                groupName=argslist.get(pp).get(aa);
                                                                //System.out.println("first mot:"+firstMot+" lastMOt:"+lastMot+ " groupName:"+groupName+ " Num mots: "+gsrl.getNbMots()+"num groups:"+gsrl.getNbGroups());
                                                                gsrl.addgroup(firstMot, lastMot, ""+groupName);
                                                            }
                                                        }
                                                    }
                                                    gdeps.add(gdep);
                                                    groles.add(gsrl);
                                            }
                                            gdep=new DetGraph();
                                            motidx=0;
                                            numArgsInUtt=0;
                                            onedeps.clear(); onedepslabs.clear();
                                            predicats.clear(); argslist.clear();
                                            predsInComments.clear();predsInCommentsPos.clear();
                                            argsIncomments.clear();argsIncommentsPos.clear();
                                            argslistPos.clear();argslistPosSegment.clear();
                                            continue;
                                    }
                                    StringTokenizer st = new StringTokenizer(s,"\t");
                                    //1  - Token ID in surface order, starting at 1 for each sentence
                                    String col = st.nextToken(); // col1 = numero du mot
                                    numColumn++;
                                    // 2  - Literal form of the word 
                                    col = st.nextToken(); // col2 = forme
                                    numColumn++;
                                    String mot=""+col;
                                    //3  - Lemmatized token according to TreeTagger 
                                    col = st.nextToken(); // col3 = lemme
                                    numColumn++;
                                    String lemme=""+col;
                                    //4  - POS tag in FrameNet
                                    col = st.nextToken(); // col4 = pos1
                                    numColumn++;
                                    String postag=""+col;
                                    Mot m = new Mot(mot,lemme,postag);
                                    //5  - POS tag according to TreeTagger
                                    col = st.nextToken(); // col5 = pos2
                                    numColumn++;
                                    //6  - 
                                    // 7  -  6-8 are empty for comptability with the CoNLL 2008 format.
                                    //8  - 
                                    col = st.nextToken(); // col6 = forme ?
                                    numColumn++;
                                    col = st.nextToken(); // col7 = lemme ?
                                    numColumn++;
                                    col = st.nextToken(); // col8 = pos ?
                                    numColumn++;
                                    gdep.addMot(motidx, m);
                                    //9  - Token ID of syntactic heads
                                    col = st.nextToken(); // col9 = dep
                                    numColumn++;
                                    if (!col.equals("_")) onedeps.add(Integer.parseInt(col)-1);
                                    else onedeps.add(-1);
                                    //10 - grammatical relation to syntactic heads
                                    col = st.nextToken(); // col10 = deplab
                                    numColumn++;
                                    onedepslabs.add(col);
                                    //An even number of subsequent fields contain frame information:
                                    //11 - Frame label of the first frame if annotated on this token
                                    //12 - Frame elements of the first frame if annotated on this token
                                    //13 - Frame label of the second frame ...
                                    //14 - Frame elements of the second frame...
                                    //etc.
                                    while (st.hasMoreTokens()) {
                                        col = st.nextToken(); // col11 = predicats
                                        numColumn++;
                                        //see if it's predicate
                                        //if numColumn = 11 or 13 or ... it's predicate, otherwise if it's = 12,14, ... it's argument of the frame that is in the prev column
                                        if (!col.equals("_")&&(numColumn % 2 != 0)) {
                                            predicats.add(motidx); //TODO add as predicate something els????
                                        }else{//if it has something it is an argument
                                            if (!col.equals("_")){
                                               //find the correct predicate
                                                int indPred=(numColumn-10)/2-1;
                                                
                                               // add it in the right predicate
                                               ArrayList<String> currentArgs=argslist.get(indPred);
                                               ArrayList<Integer> currApos=argslistPos.get(indPred);
                                               ArrayList<int[]> currSegment=argslistPosSegment.get(indPred);
                                               //TODO add also the id of the pred and arg chunk!!!
                                               //get the name of the relation
                                               for (int ia=0;ia<argsIncomments.get(indPred).size();ia++){
                                                   if (argsIncomments.get(indPred).get(ia).startsWith(col)){
//                                                       System.out.println("poss:"+Arrays.toString(argsIncommentsPos.get(indPred).get(ia)));
                                                       boolean encontro=false; 
                                                       for (int indArray=0;indArray<argsIncommentsPos.get(indPred).get(ia).length;indArray++){
                                                           if (argsIncommentsPos.get(indPred).get(ia)[indArray]==motidx) {
                                                               encontro=true;
                                                               break;
                                                           }
                                                       }
                                                       if (encontro){
                                                        currentArgs.add(argsIncomments.get(indPred).get(ia)+"--"+predsInComments.get(indPred)); 
                                                        currSegment.add(argsIncommentsPos.get(indPred).get(ia));
                                                        currApos.add(motidx);
                                                        break;
                                                       }
                                                   }
                                               }
                                               argslistPosSegment.add(currSegment);
                                               argslist.set(indPred, currentArgs);
                                               argslistPos.set(indPred, currApos);
                                            }
                                        }
                                    }                                    
                                    motidx++;
                                    
                                }
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("ERREUR pour "+s);
			e.printStackTrace();
		}
		List gs[] = {gdeps,groles};
		return gs;
	}
	/**
	 * WARNING ! the predicate sense is stored in the "POStag" field of the DetGraph; it's OK, because the real POStag is stored in
	 * the associated syntactic graph.
	 * 
	 * @param filename
	 * @return 2 sets of graphs: one for deps, one for sem. roles
	 */
	public static List<DetGraph>[] loadConll09(String filename) {
		System.out.println("load CONLL09");
		List<DetGraph> gdeps = new ArrayList<DetGraph>();
		List<DetGraph> groles = new ArrayList<DetGraph>();
		String s=null;
		int l=0;
		try {
			BufferedReader f = FileUtils.openFileUTF(filename);
			DetGraph gdep = new DetGraph();
			DetGraph gsrl=null;
			int motidx=0;
			ArrayList<Integer> onedeps = new ArrayList<Integer>();
			ArrayList<String> onedepslabs = new ArrayList<String>();
			ArrayList<Integer> predicats = new ArrayList<Integer>();
			ArrayList<String[]> args = new ArrayList<String[]>();
			ArrayList<String> sensee = new ArrayList<String>();
			for (l=0;;l++) {
				s=f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) { // end of sentence reached
					if (gdep.getNbMots()>0) { // the sentence is not empty
						assert onedeps.size()==onedepslabs.size();
						assert onedeps.size()==args.size();
						gsrl = gdep.clone();
						for (int i=0;i<gsrl.getNbMots();i++) gsrl.getMot(i).setPOS("_");
						for (int i=0;i<onedeps.size();i++) {
							if (onedeps.get(i)>=0) {
								gdep.ajoutDep(onedepslabs.get(i), i, onedeps.get(i));
							}
							assert args.get(i).length==predicats.size();
							for (int j=0;j<args.get(i).length;j++) {
								if (!args.get(i)[j].equals("_")) {
									gsrl.ajoutDep(args.get(i)[j], i, predicats.get(j));
								}
								gsrl.getMot(i).setPOS(sensee.get(i));
							}
						}
						gdeps.add(gdep);
						groles.add(gsrl);
					}
					gdep=new DetGraph();
					motidx=0;
					onedeps.clear(); onedepslabs.clear();
					predicats.clear(); args.clear();
					sensee.clear();
					continue;
				}
				StringTokenizer st = new StringTokenizer(s,"\t");
				String col = st.nextToken(); // col1 = numero du mot
				col = st.nextToken(); // col2 = forme
				String mot=""+col;
				col = st.nextToken(); // col3 = lemme
				String lemme=""+col;
				col = st.nextToken(); // col4 = lemme bis ?
				col = st.nextToken(); // col5 = pos1
				col = st.nextToken(); // col6 = pos2
				String postag=""+col;
				Mot m = new Mot(mot,lemme,postag);
				col = st.nextToken(); // col7 = _ ?
				col = st.nextToken(); // col8 = _ ?
				gdep.addMot(motidx, m);
				col = st.nextToken(); // col9 = head GOLD
				col = st.nextToken(); // col10 = head REC
				int head = Integer.parseInt(col)-1;
				onedeps.add(head);
				col = st.nextToken(); // col11 = deplab GOLD
				col = st.nextToken(); // col12 = deplab REC
				onedepslabs.add(col);
				col = st.nextToken(); // col13 = Y ou _ s'il y a predicat
				boolean predfound=false;
                if (!col.equals("_")) {
                    predfound=true;
                    predicats.add(motidx);
                }
				col = st.nextToken(); // col14 = predicat sense
				if (!col.equals("_") && !predfound) {
				    // warning: sometimes, the Y in previous column does not exist !
				    predicats.add(motidx);
				}
				sensee.add(col);
				ArrayList<String> arg = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					col = st.nextToken(); // col15+ = arguments
					arg.add(col);
				}
				String[] targ = new String[arg.size()];
				arg.toArray(targ);
				args.add(targ);
				motidx++;
			}
			f.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("ERREUR ligne "+l+" string "+s);
			e.printStackTrace();
		}
		List gs[] = {gdeps,groles};
		System.out.println("load finished "+gs.length+" "+gs[0].size());
		return gs;
	}

	public static void saveConLL06(List<DetGraph> gs, String filename) {
		try {
			PrintWriter fout = FileUtils.writeFileUTF(filename);
			Syntex2conll conllwriter = new Syntex2conll(fout);
			for (DetGraph g : gs) {
				conllwriter.processGraph(g);
			}
			conllwriter.terminate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param gs = syntax graphs, with words
	 * @param propsfile = without words, only semantic relations to chunks
	 * @return a graph list that contains only SRL graphs, with SRL arcs pointing to the first word of the chunk, and "group" to represent chunks
	 */
	public static List<DetGraph> loadConll05SRL(List<DetGraph> gs, String propsfile) {
		if (propsfile==null) {
			GraphIO gio = new GraphIO(null);
			propsfile=gio.askForSaveName();
		}
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		try {
			BufferedReader f = new BufferedReader(new FileReader(propsfile));
			int vidx=0,widx=0,gidx=0;
			DetGraph g = new DetGraph();
			Stack<String>[] args = null;
			ArrayList<Integer> verbs = new ArrayList<Integer>();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) {
					// End of sentence
					if (g.getNbMots()>0) {
						res.add(g);
						// check utt length
						DetGraph gptb = gs.get(gidx);
						if (g.getNbMots()!=gptb.getNbMots()) System.out.println("WARNING nb words differ "+gidx+" "+g.getNbMots()+" "+gptb.getNbMots()+" "+gs.get(gidx));
						else {
							for (int i=0;i<g.getNbMots();i++) {
								g.getMot(i).setForme(gptb.getMot(i).getForme());
								g.getMot(i).setlemme(gptb.getMot(i).getLemme());
								g.getMot(i).setPOS(gptb.getMot(i).getPOS());
							}
						}
						if (g.groups!=null)
							for (int gri=0;gri<g.groups.size();gri++) {
								List<Mot> gr = g.groups.get(gri);
								int k=g.groupnoms.get(gri).lastIndexOf('_');
								int verb = verbs.get(Integer.parseInt(g.groupnoms.get(gri).substring(k+1)));
								String lab = g.groupnoms.get(gri).substring(0,k);
								g.ajoutDep(lab, gr.get(0).getIndexInUtt()-1, verb);
							}
					}
					g=new DetGraph();
					vidx=widx=0;
					args=null;
					verbs.clear();
					gidx++;
				} else {
					// it's a word
					StringTokenizer st = new StringTokenizer(s);
					String w = st.nextToken().trim();
					g.addMot(widx,new Mot(w, w, w));
					boolean isVerb=false;
					if (!w.equals("-")) {
						isVerb=true;
					}
					int nverbs = st.countTokens();
					if (args==null) {
						args = new Stack[nverbs];
						for (int i=0;i<nverbs;i++) args[i]=new Stack<String>();
					}
					for (int i=0;i<nverbs;i++) {
						String x = st.nextToken().trim();
//						if (isVerb&&i==vidx) continue;
						String a=null;
						for (int j=0;j<x.length();j++) {
							if (x.charAt(j)=='(') {
								if (a!=null) args[i].push(a+"_"+widx);
								a="";
							} else if (x.charAt(j)=='*') {
								if (a!=null) args[i].push(a+"_"+widx);
								a=null;
							} else if (x.charAt(j)==')') {
								if (a!=null) args[i].push(a+"_"+widx);
								if (args[i].size()==0) System.out.println("ERROR "+i+" "+j+" "+s);
								a=args[i].pop();
								int k=a.lastIndexOf('_');
								int wdeb=Integer.parseInt(a.substring(k+1));
								String lab = a.substring(0, k);
								if (!lab.equals("V")&&!lab.equals("N")) {
									g.addgroup(wdeb, widx, lab+"_"+i);
								}
								a=null;
							} else {
								a=a+x.charAt(j);
							}
						}
					}
					if (isVerb) {
						verbs.add(widx);
						vidx++;
					}
					widx++;
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	
	/**
	 * save "props" file as expected by the Conll'05 SRL evaluation script srl-eval.pl
	 * 
	 * @param gsdeps
	 * @param gssrl
	 * @param filename
	 */
	public static void saveConll05SRL(List<DetGraph> gsdeps, List<DetGraph> gssrl, String filename, boolean groupsAsChunks) {
		if (filename==null) {
			GraphIO gio = new GraphIO(null);
			filename=gio.askForSaveName();
		}
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), Charset.forName("UTF-8")));
			for (int gi=0;gi<gssrl.size();gi++) {
				DetGraph gsrl = gssrl.get(gi);
				DetGraph gdep = gsdeps.get(gi);
				HashSet<Mot> verbset = new HashSet<Mot>();
				for (Dep d : gsrl.deps) {
					verbset.add(d.head);
				}
				ArrayList<Integer> verbstmp = new ArrayList<Integer>();
				for (Mot m : verbset) verbstmp.add(m.getIndexInUtt()-1);
				Collections.sort(verbstmp);
				String[][] cols = new String[gsrl.getNbMots()][verbstmp.size()+1];
				for (int i=0;i<cols.length;i++) {
					Arrays.fill(cols[i], "*");
					cols[i][0]="-";
				}
				for (int vi=0;vi<verbstmp.size();vi++) {
					int v = verbstmp.get(vi);
					cols[v][0]=gsrl.getMot(v).getForme();
					cols[v][vi+1]="("+gsrl.getMot(v).getPOS().charAt(0)+"*)";
				}
				for (Dep d : gsrl.deps) {
					int v=d.head.getIndexInUtt()-1;
					int c=verbstmp.indexOf(v)+1;
					int w = d.gov.getIndexInUtt()-1;
					if (groupsAsChunks) {
						// assumes SRL arcs point to the first word of the group
						int[] grps = gsrl.getGroupsThatStartHere(w);
						for (int gr : grps) {
							int lw = w;
							int rw = gsrl.groups.get(gr).get(gsrl.groups.get(gr).size()-1).getIndexInUtt()-1;
							cols[lw][c]="("+Dep.depnoms[d.type]+cols[lw][c];
							cols[rw][c]=cols[rw][c]+")";
						}
					} else if (w!=v) {
						int h=gdep.checkCycles();
						if (h>=0) {
							System.out.println("ERROR CYCLES ! "+h);
							JSafran.viewGraph(gdep);
						}
						int lw = gdep.getLeftmostNode(w);
						if (lw>0&&gdep.getMot(lw-1).getPOS().startsWith("IN")) {
							// prepositional phrase: we must include the prep !
							lw--;
						}
						int rw = gdep.getRightmostNode(w);
						// if the predicate is in the span, then the span shall get split in two, excluding the subtree from the predicate
						if (v==lw) {
							lw++;
							cols[lw][c]="("+Dep.depnoms[d.type]+cols[lw][c];
							cols[rw][c]=cols[rw][c]+")";
						} else if (v==rw) {
							rw--;
							cols[lw][c]="("+Dep.depnoms[d.type]+cols[lw][c];
							cols[rw][c]=cols[rw][c]+")";
						} else if (v>lw&&v<rw) {
							cols[lw][c]="("+Dep.depnoms[d.type]+cols[lw][c];
							cols[v-1][c]=cols[v-1][c]+")";
							cols[v+1][c]="("+Dep.depnoms[d.type]+cols[v+1][c];
							cols[rw][c]=cols[rw][c]+")";
						} else {
							cols[lw][c]="("+Dep.depnoms[d.type]+cols[lw][c];
							cols[rw][c]=cols[rw][c]+")";
						}
					}
				}
				
				for (int i=0;i<cols.length;i++) {
					for (int j=0;j<cols[i].length-1;j++) {
						f.print(cols[i][j]+"\t");
					}
					f.println(cols[i][cols[i].length-1]);
				}
				f.println();
			}
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void saveConLL08(List<DetGraph> gsdeps, List<DetGraph> gssrl, String filename) {
		assert gsdeps.size()==gssrl.size();
		if (filename==null) {
			GraphIO gio = new GraphIO(null);
			filename=gio.askForSaveName();
		}
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), Charset.forName("UTF-8")));
			// chaque dependendance est consideree comme un argument d'un predicat
			for (int gi=0;gi<gsdeps.size();gi++) {
                DetGraph gdep = gsdeps.get(gi);
			    DetGraph gsrl;
                if (gssrl!=null&&gssrl.size()>gi) {
                    gsrl = gssrl.get(gi);
                } else {
                    gsrl = gdep.getSubGraph(0);
                    gsrl.deps.clear();
                }
				assert gsrl.getNbMots()==gdep.getNbMots();
				// nb de predicats ?
				HashSet<Integer> predicats = new HashSet<Integer>();
				for (int i=0;i<gsrl.getNbMots();i++) {
					int[] deps = gsrl.getDeps(i);
					for (int dep: deps) {
						int head = gsrl.getHead(dep);
						predicats.add(head);
					}
				}
				int[] preds = new int[predicats.size()];
				Iterator<Integer> predit = predicats.iterator();
				for (int i=0;i<preds.length;i++) {
					preds[i] = predit.next();
				}
				Arrays.sort(preds);

				// sauvegarde des lignes
				for (int i=0;i<gsrl.getNbMots();i++) {
                    String dpos = gdep.getMot(i).getField(POSD);
                    if (dpos==null) dpos="_";
                    // there are no FEATS in conll08 !!
//                    String feat=gdep.getMot(i).getField(FEATS);
//                    if (feat==null) feat="_";

					// partie syntaxe
					int dep = gdep.getDep(i);
					String s=(i+1)+"\t"+gdep.getMot(i).getForme()+"\t"+gdep.getMot(i).getLemme()+"\t"+gdep.getMot(i).getPOS()+"\t"+dpos+"\t"+gdep.getMot(i).getForme()+"\t"+gdep.getMot(i).getLemme()+"\t"+gdep.getMot(i).getPOS()+"\t";
					if (dep>=0) {
						int head = gdep.getHead(dep)+1;
						String deplab = gdep.getDepLabel(dep);
						s+=head+"\t"+deplab;
					} else {
						s+="0"+"\t"+"ROOT";
					}
					// partie SRL
					// les predicats
					{
						int j = Arrays.binarySearch(preds, i);
						if (j>=0) {
							s+="\t"+gsrl.getMot(i).getLemme()+"."+j;
						} else {
							s+="\t"+"_";
						}
					}
					// les arguments
					String[] args = new String[predicats.size()];
					Arrays.fill(args, "_");
					int[] deps = gsrl.getDeps(i);
					for (int d : deps) {
						int head = gsrl.getHead(d);
						int j = Arrays.binarySearch(preds, head);
						assert j>=0;
						String lab = gsrl.getDepLabel(d);
						if (false) {
							// conversion en A0...
							String[] labs =  lab.split(":");
							if (labs.length>=3)
								args[j]="A"+labs[2];
						} else {
							args[j]=lab;
						}
					}
					for (String x : args) {
						s+="\t"+x;
					}
					f.println(s);
				}
				f.println();
			}
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	/**
	 * WARNING: we assume that the predicate sense is stored in the POSTAG field of the semantic DetGraph
	 * 
	 * @param gsdeps
	 * @param gssrl
	 * @param filename
	 */
	public static void saveConLL09(List<DetGraph> gsdeps, List<DetGraph> gssrl, String filename) {
		if (filename==null) {
			GraphIO gio = new GraphIO(null);
			filename=gio.askForSaveName();
		}
		try {
			System.err.println("nutts "+gsdeps.size());
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), Charset.forName("UTF-8")));
			// chaque dependendance est consideree comme un argument d'un predicat
			for (int gi=0;gi<gsdeps.size();gi++) {
				DetGraph gdep = gsdeps.get(gi);
				DetGraph gsrl;
				if (gssrl!=null&&gssrl.size()>gi) {
					gsrl = gssrl.get(gi);
				} else {
					gsrl = gdep.getSubGraph(0);
					gsrl.deps.clear();
				}
				assert gsrl.getNbMots()==gdep.getNbMots();
				// nb de predicats ?
				HashSet<Integer> predicats = new HashSet<Integer>();
				for (int i=0;i<gsrl.getNbMots();i++) {
					String sense = gsrl.getMot(i).getPOS();
					if (!sense.equals("_")) predicats.add(i);
				}
				int[] preds = new int[predicats.size()];
				Iterator<Integer> predit = predicats.iterator();
				for (int i=0;i<preds.length;i++) {
					preds[i] = predit.next();
				}
				Arrays.sort(preds);

				//				ID FORM LEMMA PLEMMA POS PPOS FEAT PFEAT HEAD PHEAD DEPREL PDEPREL FILLPRED PRED APREDs 

				// sauvegarde des lignes
				for (int i=0;i<gsrl.getNbMots();i++) {
					// partie syntaxe
					int dep = gdep.getDep(i);
					// bugfix: les scripts de conll09 n'acceptent pas les espaces dans un mot !
					String forme2 = gdep.getMot(i).getForme().trim().replace(' ', '_');
					String lemme2 = gdep.getMot(i).getLemme().trim().replace(' ', '_');
					String dpos = gdep.getMot(i).getField(POSD);
					if (dpos==null) dpos="_";
					String feat=gdep.getMot(i).getField(FEATS);
					if (feat==null) feat="_";
					if (false) {
						// cheat: ajoute les deps comme feats !
						if (dep>=0) feat = gdep.getDepLabel(dep);
					}
					if (false) {
						// ajoute les groupes (entites nommees ?) comme features
						int[] grps = gdep.getGroups(i);
						if (grps!=null&&grps.length>0) {
							feat="";
							int grp=0;
							for (grp=0;grp<grps.length-1;grp++) feat+=gdep.groupnoms.get(grps[grp])+"|";
							feat+=gdep.groupnoms.get(grps[grp]);
						}
					}
					String ligne=(i+1)+"\t"+forme2+"\t"+lemme2+"\t"+lemme2+"\t"+gdep.getMot(i).getPOS()+"\t"+dpos+"\t"+feat+"\t"+feat+"\t";
					if (dep>=0) {
						int head = gdep.getHead(dep)+1;
						String deplab = gdep.getDepLabel(dep);
						// bugfix pour corriger certaines deps vers ROOT qui sont mal lues en conll08:
						if (deplab.equals("ROOT"))
							ligne+="0\t0\tROOT\tROOT";
						else
							ligne+=head+"\t"+head+"\t"+deplab+"\t"+deplab;
					} else {
						ligne+="0\t0\tROOT\tROOT";
					}
					// partie SRL
					// les predicats
					{
						int j = Arrays.binarySearch(preds, i);
						if (j>=0) {
							ligne+="\tY\t"+gsrl.getMot(i).getPOS();
						} else {
							ligne+="\t_\t_";
						}
					}
					// les arguments
					String[] args = new String[predicats.size()];
					Arrays.fill(args, "_");
					int[] deps = gsrl.getDeps(i);
					for (int d : deps) {
						int head = gsrl.getHead(d);
						int j = Arrays.binarySearch(preds, head);
						if (j<0) {
							System.out.println("ERROR saved conll09 "+Arrays.toString(preds)+" "+head+" "+i);
							DetGraph[] gg = {gdep,gsrl};
							JSafran.viewGraph(gg);
						}
						String lab = gsrl.getDepLabel(d);
						args[j]=lab;
					}
					for (String x : args) {
						ligne+="\t"+x;
					}
					f.println(ligne);
				}
				f.println();
			}
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}


	public static void main(String args[]) {
		GraphIO gio = new GraphIO(null);
		int ai=0;
		List<List<DetGraph>> gs = new ArrayList<List<DetGraph>>();
		while (ai<args.length) {
			if (args[ai].equals("-savelab")) {
				// TODO: old stuff, fix it !
				try {
					BufferedReader f = new BufferedReader(new FileReader(args[0]));
					for (int i=0;;i++) {
						String s = f.readLine();
						if (s==null) break;
						s=s.trim();
						if (s.length()==0) continue;
						List<DetGraph> graphs =gio.loadAllGraphs(s);
						DetGraph gall = new DetGraph();
						for (DetGraph g : graphs) gall.append(g);
						String lab = FileUtils.noExt(s)+".lab";
						System.err.println("save in "+lab);
						gio.saveLab(gall, lab);
					}
					f.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (args[ai].equals("-loadSTM")) {
				// TODO: old stuff, fix it !
				isTxtUTF=false;
				List<DetGraph> graphs =loadSTM(args[1], true);
				gio.save(graphs,"output.xml");
			} else if (args[ai].equals("-loadtxt")) {
			    List<DetGraph> tmpgs = loadTxt(args[++ai]);
                gs.add(tmpgs);
			} else if (args[ai].equals("-loadxml")) {
				List<DetGraph> tmpgs = gio.loadAllGraphs(args[++ai]);
				gs.add(tmpgs);
            } else if (args[ai].equals("-loadconll06")) {
                List<DetGraph> tmpgs = loadConll06(args[++ai], false);
                gs.add(tmpgs);
			} else if (args[ai].equals("-loadconll09")) {
				List<DetGraph>[] tmpgs = loadConll09(args[++ai]);
				gs.add(tmpgs[0]);
				gs.add(tmpgs[1]);
				
			} else if (args[ai].equals("-cleardeps")) {
				for (List<DetGraph> gg: gs)
					for (DetGraph g : gg) g.deps.clear();
            } else if (args[ai].equals("-saveconll09")) {
                if (gs.size()==1) {
                    // create an empty SRL graph
                    ArrayList<DetGraph> gsrl = new ArrayList<DetGraph>();
                    gs.add(gsrl);
                    for (int i=0;i<gs.get(0).size();i++) {
                        DetGraph g = gs.get(0).get(i).clone();
                        g.clearDeps();
                        gsrl.add(g);
                    }
                }
                saveConLL09(gs.get(0), gs.get(1), args[++ai]);
            } else if (args[ai].equals("-saveconll06")) {
                saveConLL06(gs.get(0), args[++ai]);
			} else if (args[ai].equals("-saveLevel")) {
				int l = Integer.parseInt(args[++ai]);
				gio.save(gs.get(l), "level.xml");
			} else if (args[ai].equals("-head")) {
				int n=Integer.parseInt(args[++ai]);
				for (int i=0;i<gs.size();i++) {
					gs.set(i, gs.get(i).subList(0, n));
				}
            } else if (args[ai].equals("-eval")) {
                // assumes that the gold graphs are in gs.get(0) and the infered graphs in gs.get(1)
                float[] las = ClassificationResult.calcErrors(gs.get(1), gs.get(0));
                System.out.println("LAS "+Arrays.toString(las));
			} else if (args[ai].equals("-tailhead")) {
				int n=Integer.parseInt(args[++ai]);
				for (int i=0;i<gs.size();i++) {
					gs.set(i, gs.get(i).subList(n,gs.get(i).size()));
				}
			}
			ai++;
		}
	}

	public ArrayList<DetGraph> loadList(List<String> list) {
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		for (String fich: list) {
			System.err.println("loading "+fich);
			List<DetGraph> gs = loadAllGraphs(fich);
			for (int i=0;i<gs.size();i++) {
				res.add(gs.get(i));
			}
		}
		return res;
	}
	public int lastchunk=0;
	public LinkedList<DetGraph> loadList(String list) {
		LinkedList<DetGraph> res = new LinkedList<DetGraph>();
		try {
			System.err.println("loading "+list);
			BufferedReader f = new BufferedReader(new FileReader(list));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				List<DetGraph> gs = loadAllGraphs(s);
				if (gs.size()>0) lastchunk=res.size();
				for (int i=0;i<gs.size();i++) {
					res.add(gs.get(i));
				}
			}
			f.close();
			return res;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}


	public List<DetGraph> loadAllGraphs(String nom) {
		return loadAllGraphs(nom, -1);
	}
	public List<DetGraph> loadAllGraphs(String nom, int nmax) {
		allGraphsTmp = new LinkedList<DetGraph>();
		if (nom == null) {
			File wdir = new File(".");
//			System.err.println("current dir " + wdir.getAbsolutePath());
			JFileChooser chooser = new JFileChooser(wdir);
			int returnVal = chooser.showOpenDialog(jf);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				readFile = chooser.getSelectedFile();
				nom=readFile.getAbsolutePath();
			} else
				return null;
		}
		if (nom.endsWith(".txt")) {
			readFile = new File(nom);
			return loadTxt(readFile.getAbsolutePath());
		} else {
			// vieille version qui utilise SyntaxGraph
			// je ne l'utilise plus pour le texte, car le parser de texte dans GraphIO
			// est meilleur car il conserve la position dans le texte d'origine
			if (nom.endsWith(".conll") || nom.endsWith(".CONLL"))
				loadedfromconll = true;
			final SyntaxGraphs m = new SyntaxGraphs(this);
			m.parse(nom, 0, nmax);
		}
//		System.err.println("fini loading graphs ");
		return allGraphsTmp;

		/*
		Thread thr = new Thread(new Runnable() {
			public void run() {
				m.parse(f.getAbsolutePath(), 0);
				System.err.println("fini loading graphs ");
			}
		});
		thr.start();
		 */
	}

	public void terminate() {
	}

	public void processGraph(DetGraph g) {
		allGraphsTmp.add(g);
		if (jf!=null)
			if (allGraphsTmp.size()%5==0) {
				jf.repaint();
			}
	}

	boolean confirm() {
		int a = JOptionPane.showConfirmDialog(jf,
				"Le fichier .XML precedent sera ecrase. Sauver quand meme ?");
		if (a == JOptionPane.OK_OPTION)
			return true;
		else
			return false;
	}

	public String askForSaveName() {
		System.err.println("ask for name");
		File wdir;
		if (readFile==null)
			wdir = new File(".");
		else
			wdir = readFile.getParentFile();
		JFileChooser chooser = new JFileChooser(wdir);
		int returnVal = chooser.showSaveDialog(jf);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			readFile = chooser.getSelectedFile();
		} else
			return null;
		String s = readFile.getAbsolutePath();
		return s;
	}

	public static PrintWriter saveEntete(String nom) {
		try {
			PrintWriter ff = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom), Charset.forName("UTF-8")));
			ff.println("<checkenc é>");
			ff.println("<encoding=UTF-8/>");
			return ff;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// regenere systematiquement le texte
	public static void fixTxt(DetGraph g) {
		if (true||g.sent==null) {
			g.sent="";
			for (int i=0;i<g.getNbMots();i++) {
				g.sent+=g.getMot(i).getForme()+" ";
			}
		}
	}

	public String save(List<DetGraph> graphs, String nom) {
		if (nom==null)
			nom=askForSaveName();
		PrintWriter ff = saveEntete(nom);
		for (int i = 0; i < graphs.size(); i++) {
			DetGraph g = graphs.get(i);
			// fix comments
			if (g.comment!=null&&g.comment.trim().length()==0) g.comment=null;
			fixTxt(g);
			g.save(ff);
		}
		ff.close();
		System.err.println("graph saved in " + nom);
		return nom;
	}

	public String saveLab(DetGraph graph, String nom) {
		if (nom==null)
			nom=askForSaveName();
		try {
			PrintWriter f = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nom), Charset.forName("UTF-8")));
			for (int i=0;i<graph.getNbMots();i++) {
				f.println(graph.getMot(i).getForme());
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nom;
	}
	
	public static List<DetGraph> loadTRS(String trsfile) {
		if (trsfile==null) {
			GraphIO gio = new GraphIO(null);
			trsfile=gio.askForSaveName();
		}
		ArrayList<DetGraph> res = new ArrayList<DetGraph>();
		try {
			BufferedReader f = FileUtils.openFileISO(trsfile);
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				if (s.startsWith("<Sync time=")) {
				} else if (s.startsWith("</Turn>")) {
					DetGraph g = new DetGraph();
					StringTokenizer st = new StringTokenizer(sb.toString());
					int idx=0;
					while (st.hasMoreTokens()) {
						String w = st.nextToken();
						g.addMot(idx++, new Mot(w, w, w));
					}
					if (g.getNbMots()>0) res.add(g);
					sb = new  StringBuilder();
				} else {
					if (s.length()==0||s.charAt(0)=='<' || s.equals("#")) continue;
					s=s.trim();
					if (s.length()<=0) continue;
					String[] ss = s.split(" ");
					for (int i=0;i<ss.length;i++) {
						sb.append(ss[i]+" ");
					}
				}
			}
			f.close();
			return res;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
