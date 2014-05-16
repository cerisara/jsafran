package corpus.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.FileUtils;

/**
 * represents a list of segments from a corpus
 * These can come from a tokenizer or a segmenter or ...
 * 
 * @author cerisara
 *
 */
public class TextSegments {
	public enum segtypes {phrase, mot, ponct, comment, bruit};

	// il peut y avoir 1 source qui est un fichier texte, eventuellement très long
	private URI source = null;
	// ou alors une source inconnue/éphémère, et on stocke alors la chaîne ici
	private String sourcedup = null;
	private List<Long> segmentStartPos, segmentEndPos;
	private List<segtypes> segmentTypes;
	private final long eollen = "\n".length();

	public TextSegments() {
		segmentStartPos=new ArrayList<Long>();
		segmentEndPos=new ArrayList<Long>();
		segmentTypes=new ArrayList<TextSegments.segtypes>();
	}
	
	public void clear() {
		segmentStartPos.clear();
		segmentEndPos.clear();
		segmentTypes.clear();
	}
	
	public String toString() {
		String s = "";
		for (int i=0;i<getNbSegments();i++)
			s+=("seg "+i+" "+getSegment(i)+":"+getSegmentType(i)+" ");
		return s;
	}

	public URI getSource() {return source;}
	public void setSource(URI uri) {source=uri;}
	public int getNbSegments() {return segmentStartPos.size();}
	public String getSegment(int numseg) {
		if (sourcedup!=null) {
			return sourcedup.substring((int)getSegmentStartPos(numseg),(int)getSegmentEndPos(numseg));
		} else {
			// TODO avec le buffer
			// pour le moment, je relis le fichier depuis le debut
			long wanteddeb = getSegmentStartPos(numseg);
			long wantedfin = getSegmentEndPos(numseg);
			assert wantedfin>wanteddeb;
			try {
				File f0 = new File(source);
				BufferedReader f;
				if (!isISO) f = FileUtils.openFileUTF(f0.getAbsolutePath());
				else f = FileUtils.openFileISO(f0.getAbsolutePath());
				long memdeb=0;
				for (;;) {
					String s = f.readLine();
					if (s==null) break;
					long memfin = memdeb+s.length();
					if (wanteddeb>=memdeb && wantedfin<=memfin) {
						int relposdeb = (int)(wanteddeb-memdeb);
						int relposfin = (int)(wantedfin-memdeb);
						f.close();
						return s.substring(relposdeb, relposfin);
					} else if (wanteddeb>=memfin) {
					} else {
						System.out.println("ERROR: unsupported position for now "+s);
						System.out.println(memdeb+" "+wanteddeb+" "+memfin+" "+wantedfin);
						f.close();
						System.exit(1);
					}
					memdeb=memfin+eollen;
				}
				System.out.println("WARNING segment not found !");
				f.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	public long getSegmentStartPos(int numseg) {
		return segmentStartPos.get(numseg);
	}
	public long getSegmentEndPos(int numseg) {
		return segmentEndPos.get(numseg);
	}
	public segtypes getSegmentType(int numseg) {
		return segmentTypes.get(numseg);
	}
	/**
	 * sauve dans un fichier séparé les segments et leur type
	 * @param tofile
	 */
	public void save(PrintWriter tofile) {
		tofile.println("textsegment");
		tofile.println("source "+source);
		tofile.println("sourcestr "+sourcedup);
		tofile.println("segments "+segmentStartPos.size());
		for (int i=0;i<segmentStartPos.size();i++) {
			tofile.println(segmentStartPos.get(i)+" "+segmentEndPos.get(i)+" "+segmentTypes.get(i));
		}
	}
	/** 
	 * si on a deja lu la 1ere ligne, on la mets dans firstline, sinon, firstline=null
	 */
	public void load(BufferedReader f, String firstline) {
		try {
			if (firstline==null) {
				firstline=f.readLine();
			}
			assert firstline.startsWith("textsegment");
			String s;
			for (;;) {
				s = f.readLine();
				if (s.startsWith("segments ")) break;
				else if (s.startsWith("source ")) source=URI.create(s.substring(7));
				else if (s.startsWith("sourcestr ")) sourcedup=s.substring(10);
			}
			int nsegs = Integer.parseInt(s.substring(12));
			for (int i=0;i<nsegs;i++) {
				s=f.readLine();
				String[] ss = s.split(" ");
				segmentStartPos.add(Long.parseLong(ss[0]));
				segmentEndPos.add(Long.parseLong(ss[1]));
				segmentTypes.add(segtypes.valueOf(ss[2]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// compare 2 TextSegments pour retrouver quel segment dans le 2ème "correspond"
	// au segment target du 1er, en se basant sur les positions lorsque les 2 TextSegments
	// pointent vers la meme source;
	// sinon, utilise des distances de StringEdit pour tenter de faire correspondre les
	// deux sources "différentes"
	public int getMatchingSegment(TextSegments segs2, int segid) {
		// TODO
		return -1;
	}

	public int creeSegment(long posdeb, long posfin, segtypes typ) {
		// TODO : verifier s'il y a conflit avec un segment existant
		segmentStartPos.add(posdeb);
		segmentEndPos.add(posfin);
		segmentTypes.add(typ);
		return segmentTypes.size()-1;
	}
	public void insertSegment(int pos, long posdeb, long posfin, segtypes typ) {
		// TODO : verifier s'il y a conflit avec un segment existant
		segmentStartPos.add(pos,posdeb);
		segmentEndPos.add(pos,posfin);
		segmentTypes.add(pos,typ);
	}

	/**
	 * ajoute une "string" aux segments existants, en creant un segment pour celle-ci
	 * @param txt
	 */
	public int addOneString(String txt) {
		long pos;
		if (sourcedup==null) {
			pos=0;
			sourcedup=txt;
		} else {
			pos=sourcedup.length()+1;
			sourcedup=sourcedup+" "+txt;
		}
		long pos2=sourcedup.length();
		if (pos2>pos)
			return creeSegment(pos, pos2, segtypes.phrase);
		else return -1;
	}
	
	public static boolean isISO = false;
	
	// charge un fichier texte avec une tokenisation par phrase (séparateur \n)
	// il n'y donc un segment par phrase
	public void preloadTextFile(String txtfile) {
		try {
			source = (new File(txtfile)).toURI();
			BufferedReader f;
			if (isISO)
				f = FileUtils.openFileISO(txtfile);
			else
				f = FileUtils.openFileUTF(txtfile);
			boolean hasSeenAccent = false;
			long pos=0;
			for (int i=0;;i++) {
				String s = f.readLine();
				if (s==null) break;
				if (!hasSeenAccent&&s.indexOf('é')>=0) hasSeenAccent=true;
				long pos2 = pos+s.length();
				if (pos2>pos)
					creeSegment(pos, pos2, segtypes.phrase);
				pos=pos2+eollen;
			}
			f.close();
			if (!hasSeenAccent) System.err.println("WARNING TextSegments: I suspect wrong file encoding has been used... May be think about option -iso ? "+isISO);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// idem mais depuis un fichier STM
	public void preloadSTMFile(String stmfile, boolean isUTF) {
		System.out.println("preload STM with isUTF="+isUTF);
		isISO = !isUTF;
		try {
			source = (new File(stmfile)).toURI();
			BufferedReader f;
			if (isUTF) {
				f = FileUtils.openFileUTF(stmfile);
			} else {
				// les vieux STM sont en ISO
				f = FileUtils.openFileISO(stmfile);
			}
			long pos=0;
			for (int i=0;;i++) {
				String s = f.readLine();
				if (s==null) break;
				long pos2 = pos+s.length();
				if (pos2>pos) {
					int start =s.indexOf("male>");
					if (start>=0) {
						pos+=start+5;
						creeSegment(pos, pos2, segtypes.phrase);
					}
				}
				pos=pos2+eollen;
			}
			f.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isDelim(char c) {
		if (Character.isWhitespace(c)) return true;
		// TODO: ajouter les apostrophes windows
		if (c=='\'') return true;
		return false;
	}
	
	/** tokenise avec des caractères séparateurs et qqs règles simples
	 et créé les segments mots, qui peuvent contenir de la ponctuation !
	 pour un seule segment-phrase !
	 */
	public TextSegments tokenizeBasic(int segphrase) {
		String s = getSegment(segphrase);
		TextSegments res = new TextSegments();
		if (sourcedup!=null) {
			res.sourcedup=sourcedup;
		} else {
			res.source=source;
		}
		long pos=getSegmentStartPos(segphrase);
		int c=0;
		while (c<s.length()) {
			while (c<s.length()&&isDelim(s.charAt(c))) c++;
			if (c>=s.length()) break;
			int d=c+1;
			while (d<s.length()&&!isDelim(s.charAt(d))) d++;
			String mot = s.substring(c,d);
			if (d<s.length()&&s.charAt(d)=='\'') {
				// nouveau mot
				++d;
				// cas particulier
				if (mot.endsWith("aujourd")) {
					while (Character.isWhitespace(s.charAt(d))) d++;
					if (s.substring(d).startsWith("hui")) d+=3;
					else
						System.out.println("WARNING aujourd'hui "+s);
				}
				System.err.println("tokenizebasic creemot "+(pos+c)+" "+(pos+d)+" ["+mot+']');
				res.creeSegment(pos+c, pos+d, segtypes.mot);
			} else if (mot.length()>0) {
				// nouveau mot
				System.err.println("tokenizebasic creemot "+(pos+c)+" "+(pos+d)+" ["+mot+']');
				res.creeSegment(pos+c, pos+d, segtypes.mot);
			}
			c=d;
		}
		return res;
	}

	/**
	 * reconsidere toutes les phrases et les segmente avec la ponctuation
	 * 
	 * @deprecated NON ! utiliser plutot la segmentation avec ponctuation qui permet de separer les phrases ensuite !
	 */
	public void segmentePhrases() {
		for (int i=0;i<getNbSegments();i++) {
System.out.println("debug "+i+" "+getNbSegments());
			String seg = getSegment(i);
			System.out.println("seg "+seg);
			if (seg.length()>0) {
				int j=seg.indexOf('!');
				if (j>0) {
					String s = seg.substring(j+1).trim();
					if (s.length()>0) {
						insertSegment(i+1, getSegmentStartPos(i)+j+1, getSegmentEndPos(i), segtypes.phrase);
						long newend = getSegmentEndPos(i)+j+1;
						segmentEndPos.set(i,newend);
					}
				}
				j=seg.indexOf('?');
				if (j>0) {
					String s = seg.substring(j+1).trim();
					if (s.length()>0) {
						insertSegment(i+1, getSegmentStartPos(i)+j+1, getSegmentEndPos(i), segtypes.phrase);
						long newend = getSegmentEndPos(i)+j+1;
						segmentEndPos.set(i,newend);
					}
				}
				j=seg.indexOf('.');
				if (j>0) {
					String s = seg.substring(j+1).trim();
					if (s.length()>0) {
						// il faut un espace après: "http://a.b.fr" "4.5"
						if (s.charAt(0)==' ') {
							// il ne faut pas une majuscule juste avant "M. Jean"
							if (!Character.isUpperCase(seg.charAt(j-1))) {
								long newend = getSegmentEndPos(i)+j+1;
								insertSegment(i+1, newend, getSegmentEndPos(i), segtypes.phrase);
								segmentEndPos.set(i,newend);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * reconsidère tous les segments-mots et extrait la ponctuation
	 */
	public void tokenizePonct() {
		for (int i=0;i<getNbSegments();i++) {
			String seg = getSegment(i);
			if (seg.length()>0) {
				int relpos = seg.length()-1;
				while (relpos>=0&&Character.isWhitespace(seg.charAt(relpos))) relpos--;
				char c = seg.charAt(relpos);
				switch (c) {
				case '.':
					// TODO: cas particulier des ACRONYMES
				case ',':
					// cas particulier des virgules dans les nombres: pas de pb, car la virgule est au milieu du mot, pas a la fin !
					// TODO: si on a une virgule qui sépare 2 mots mais on a oublié l'espace !
				case '!':
				case ':':
				case ';':
				case '/':
				case '?':
					// pour toute ponctuation, on recule d'une case
					relpos--;
					final String pctmarks="!:;,?. ";
					while (relpos>=0 && pctmarks.indexOf(seg.charAt(relpos))>=0) relpos--;
					segtypes pcttype = segtypes.ponct;
					if (c=='/') pcttype = segtypes.comment;
					if (relpos<0) {
						// tout le mot est ponctuation
						segmentTypes.set(i, pcttype);
					} else {
						// il y a un mot devant
						long offset = getSegmentStartPos(i);
						insertSegment(++i, offset+(long)relpos+1, segmentEndPos.get(i-1), pcttype);
						segmentEndPos.set(i-1, offset+(long)relpos+1);
					}
					break;
				default:
					// c'est un mot; on le laisse
				}
			}
		}
	}
	/** reconsidere tous les segments de mots existants et les reclasse en
	commentaires, bruits selon leur écriture
	*/
	public void tokenizeComments() {
		for (int i=0;i<getNbSegments();i++) {
			String seg = getSegment(i).trim();
			if (seg.length()==0) continue;
			char c =seg.charAt(0);
			switch(c) {
			case '[':
				segmentTypes.set(i, segtypes.comment);
				int j = seg.indexOf(']');
				if (j>=0) {
					// pas de pb, fin du commentaire dans le meme segment
				} else {
					// cherche la fin du commentaire (mais pas trop loin)
					for (j=i+1;j<getNbSegments()&&j<i+10;j++) {
						if (getSegment(j).trim().endsWith(""+']')) {
							for (int k=i+1;k<=j;k++) segmentTypes.set(k, segtypes.comment);
							i=j;
							break;
						}
					}
					// si on ne trouve pas la fin, tant pis, on ne commente que le debut...
				}
				break;
			case '(':
				segmentTypes.set(i, segtypes.comment);
				j = seg.indexOf(')');
				if (j>=0) {
					// pas de pb, fin du commentaire dans le meme segment
				} else {
					// cherche la fin du commentaire (mais pas trop loin)
					for (j=i+1;j<getNbSegments()&&j<i+10;j++) {
						if (getSegment(j).trim().endsWith(""+')')) {
							for (int k=i+1;k<=j;k++) segmentTypes.set(k, segtypes.comment);
							i=j;
							break;
						}
					}
					// si on ne trouve pas la fin, tant pis, on ne commente que le debut...
				}
				break;
			case '*':
				segmentTypes.set(i, segtypes.bruit);
				break;
			default: break;
			}
		}
	}

	/**
	 * @deprecated ne plus utiliser, car la source est immutable. Utiliser a la placec tokenizeComments()
	 * @param s
	 * @return
	 */
	public static String removeComments(String s) {
		String s2 = "";
		int i =  s.indexOf('[');
		int prev=0;
		while (i>=0) {
			s2+=s.substring(prev,i);
			int k = s.indexOf(']',i+1);
			if (k<0) {
				System.err.println("WARNING: [] non trouves "+s);
				prev=i;
				break;
			}
			prev=k+1;
			i = s.indexOf('[',prev);
		}
		s2+=s.substring(prev);
		s=s2;
		//		s=s.replaceAll("\\[^*\\]", "");
		s=s.replaceAll("\\*", "");
		s=s.replaceAll("\\^", "");
		s=s.replaceAll("\\(", "");
		s=s.replaceAll("\\)", "");
		s=s.replaceAll("  *", " ");
		return s;
	}
	/**
	 * @deprecated ne plus utiliser, car la source est immutable
	 * @param s
	 * @return
	 */
	public static String removePonct(String s) {
		s=s.replace('.', ' ');
		s=s.replace(',', ' ');
		s=s.replace(';', ' ');
		s=s.replace(':', ' ');
		s=s.replace('!', ' ');
		s=s.replace('?', ' ');
		s=s.replace('(', ' ');
		s=s.replace(')', ' ');
		s=s.replace('"', ' ');
		s=s.replace('_', ' ');
		s=s.replace('=', ' ');
		s=s.replaceAll("  *", " ");
		return s;
	}
}
