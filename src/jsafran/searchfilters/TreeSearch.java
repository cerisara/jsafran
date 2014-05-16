package jsafran.searchfilters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import utils.FileUtils;

import jsafran.DetGraph;
import jsafran.Mot;

/**
 * conjonction de (gov,head,deplab)
 * f=form l=lemme p=postag
 * 
 * exemple:
 * (X,Y,"MODN") => (X,Y,"MOD")
 * 
 * 
 * tests en combinant ETB2009 et ETB2010:
 * - baseline: ETB2009: LAS = 71.98%
 * - combi avec v1 = DISFL ==> DUMMY : LAS = 71.08%
 * - combi avec v2 = dble SUJ ==> APPOS : LAS = 71.08% : j'oublie la normalisation des dble sujets pour le moment
 * 
 * analyse des pertes:
 * - surtout DUMMY+++ et CC
 * 
 * Dans le test, les DUMMY, lorsqu'ils representent une DISFL, sont codes ainsi:
 * - vers la gauche lorsque c'est une hesitation
 * - vers la droite lorsque c'est une correction (donc, vers l'element corrigé): directement vers le 1er mot corrigé !
 * Sinon, vers le verbe lorsque c'est un reflexif/att du verbe
 * 
 * IMPORTANT: j'ai modifie le test2009.xml pour respecter ces principes !
 * 
 * Nouvelle baseline:
 * - LAS = 72.25%
 * - combi avec v1 ==> LAS = 71.46%
 * il y a un mismatch: dans etb2010, les dislf. hesitations partent vers la droite !
 * 
 * ==> j'ajoute le support de contraintes comme head1=gov1+1
 * ==> je converti tous les euh+DUMMY a droite vers la gauche
 * - combi avec v1 ==> LAS = 72.19% ==> ceci est le version v3 du corpus (derivee de v1 avec correction des DUMMY)
 * faiblesse restante: APPOS
 * 
 * ==> je "corrige" les APPOS dans le test:
 * nouvelle baseline: LAS=72.48%
 * combi avec v3: LAS=72.46%
 * 
 * BUG: ETB2010 lie le sujet au verbe principal, pas a l'AUX !!
 * AUX ==> SUJ lié au verbe principal ==> évitera la projectivité
 * 
 * transfo de ETB2009 avec la regle:
 * _,_,SUJ head0,_,AUX => gov0,head1,SUJ head0,head1,AUX
 * 
 * Nouvelle baseline: LAS=71.94%
 * combi avec v3: LAS = 72.00%
 * 
 * bug found avec les AUX:
 * nouvelle transfo:
 * _,_,_ head0,_,AUX => gov0,head1,lab0 head0,head1,AUX
 * puis retransfo: _,_,SUJ head0,_,AUX => gov0,head1,SUJ head0,head1,AUX
 * 
 * Nouvelle baseline: LAS = 71.81%
 * combi avec v3: LAS = 72.37%
 * 
 * Ajout des nouvelles annotations Melynda 2010: memes perfs !
 * 
 * @author cerisara
 *
 */
public class TreeSearch {
	
	public final static String dependent="dep", governor="head", deplabel="rel";
	
	List<String> leftET = new ArrayList<String>();
	List<String> rightET = new ArrayList<String>();
	List<String> contraintes = new ArrayList<String>();
	private TreeSearchCallback callback=null;
	
	private TreeSearch(){};
	public static TreeSearch create(String rule, TreeSearchCallback cb) {
		TreeSearch t = new TreeSearch();
		t.callback=cb;
		int i=rule.indexOf("=>");
		if (i<0) i=rule.length();
		else {
			String rightp = rule.substring(i+2).trim();
			String[] rs = rightp.split(" ");
			for (String s : rs) {
				t.rightET.add(s);
			}
		}
		String leftp = rule.substring(0,i).trim();
		String[] rs = leftp.split(" ");
		for (String s : rs) {
			if (s.indexOf(',')>=0)
				t.leftET.add(s);
			else
				t.contraintes.add(s);
		}
		System.out.println("create search filter: "+t.contraintes.size()+" "+t.leftET.size()+" "+t.rightET.size());
		return t;
	}
	
	// utilise seulement pour convertir les dep labels en integer !
	HashMap<String, Integer> labsidx = new HashMap<String, Integer>();
	
	/**
	 * mot-clefs speciaux: nf, rootof
	 */
	private int getval(String s, HashMap<String, Object> vars) {
		if (s.equals("_")) return -1;
		if (s.startsWith("nf")) {
			// cas particulier: nb de fils
			Integer v = (Integer)vars.get(s.substring(2));
			if (v==null) {
				System.out.println("ERROR contrainte egal "+s);
				return -1;
			}
			return gtmp.getFils(v).size();
		} else if (s.startsWith("nw")) {
			// nb de mots de la phrase
			return gtmp.getNbMots();
		} else if (s.startsWith("rootof")) {
			// indice du mot qui est la racine du sous-arbre contenant un mot donné
			Integer v = (Integer)vars.get(s.substring(6));
			if (v==null) {
				System.out.println("ERROR contrainte egal "+s);
				return -1;
			}
			int i=v;
			for (int j=0;j<1000000;j++) {
				int dep=gtmp.getDep(i);
				if (dep<0) return i;
				i=gtmp.getHead(dep);
			}
			//				System.out.println("ERROR cycle !"+s);
			return -1;
		} else if (s.startsWith(deplabel)) {
			String d1 = (String)vars.get(s);
			Integer didx = labsidx.get(d1);
			if (didx==null) {
				didx=labsidx.size();
				labsidx.put(d1, didx);
			}
			return didx;
		} else if (s.equals("ROOT")) {
			Integer didx = labsidx.get(s);
			if (didx==null) {
				didx=labsidx.size();
				labsidx.put(s, didx);
			}
			return didx;
		} else {
			Integer v = (Integer)vars.get(s);
			if (v==null) return Integer.parseInt(s);
			return v;
		}
	}
	private int getvalComplex(String s, HashMap<String, Object> vars) {
		int val;
		int j=s.indexOf('+');
		if (j>=0) {
			// addition
			val = getval(s.substring(0,j),vars);
			val += getval(s.substring(j+1),vars);
		} else {
			j=s.indexOf('-');
			if (j>=0) {
				// soustraction
				val = getval(s.substring(0,j),vars);
				val -= getval(s.substring(j+1),vars);
			} else {
				// valeur simple
				val = getval(s,vars);
			}
		}
		return val;
	}

	private DetGraph gtmp;
	
	private boolean isInGroup(int gov, String targetGroupRegexp) {
		List<String> grps = gtmp.getGroupNoms(gov);
		for (String gn : grps) {
			if (formMatches(gn, targetGroupRegexp)) return true;
		}
		return false;
	}
	private boolean checkGroup(String s, HashMap<String, Object> vars) {
		int i=s.indexOf(".g=");
		if (i>=0) {
			// TODO: check for SAME group = dep0.g=dep1.g
			String targetGroupRegexp = s.substring(i+3);
			int gov=getvalComplex(s.substring(0, i), vars);
			return isInGroup(gov, targetGroupRegexp);
		}
		i=s.indexOf(".g!=");
		if (i>=0) {
			String targetGroupRegexp = s.substring(i+4);
			int gov=getvalComplex(s.substring(0, i), vars);
			return !isInGroup(gov, targetGroupRegexp);
		}
		return false;
	}
	
	private boolean checkConstraints(DetGraph g, HashMap<String, Object> vars) {
		gtmp=g;
		for (String s : contraintes) {
			int i=s.indexOf('=');
			if (i>=0) {
				// contrainte d'egalite
				
				// cas particulier: recherche dans les notes
				if (s.startsWith("n=")) {
					// cherche dans les notes: rien d'autre n'est autorise dans ce cas !
					if (g.comment!=null&&g.comment.indexOf(s.substring(2))>=0) {
						return true;
					}
					return false;
				} else if (s.indexOf(".g")>=0) {
					// contrainte sur les groupes
					if (!checkGroup(s,vars)) return false;
					continue;
				}
				if (i>0&&s.charAt(i-1)=='>') {
					// superieur ou egal
					Integer val1 = (Integer)vars.get(s.substring(0,i-1));
					if (val1==null) {
						System.out.println("ERROR contrainte >= "+s);
					} else {
						String ss = s.substring(i+1);
						int val2 = getvalComplex(ss,vars);
						if (val1<val2) return false;
					}
				} else if (i>0&&s.charAt(i-1)=='!') {
					// différent de
					int val1 = getvalComplex(s.substring(0, i-1), vars);
					int val2 = getvalComplex(s.substring(i+1), vars);
					if (val1==val2) return false;
				} else {
					int val1 = getvalComplex(s.substring(0,i), vars);
					String ss = s.substring(i+1);
					int val2 = getvalComplex(ss,vars);
					if (val2!=val1) return false;
				}
			} else {
				i=s.indexOf('>');
				if (i>=0) {
					// contrainte de succession
					Integer val1 = (Integer)vars.get(s.substring(0,i));
					if (val1==null) {
						System.out.println("ERROR contrainte > "+s);
					} else {
						String ss = s.substring(i+1);
						int val2 = getvalComplex(ss,vars);
						if (val1<=val2) return false;
					}
				} else {
					i=s.indexOf('<');
					if (i>=0) {
						// contrainte de precedence
						Integer val1 = (Integer)vars.get(s.substring(0,i));
						if (val1==null) {
							System.out.println("ERROR contrainte < "+s);
						} else {
							String ss = s.substring(i+1);
							int val2 = getvalComplex(ss,vars);
							if (val1>=val2) return false;
						}
					} else {
						i=s.indexOf("..proj..");
						if (i>=0) {
							// tous les mots entre doivent avoir des liens non-proj
							String ss = s.substring(0,i);
							int val1 = getvalComplex(ss,vars);
							ss = s.substring(i+8);
							int val2 = getvalComplex(ss,vars);
							if (val2<=val1) return false;
							for (int gi=val1+1;gi<val2;gi++) {
								int d = g.getDep(gi);
								if (d<0) return false;
								int h=g.getHead(d);
								if (h<val1||h>val2) return false;
							}
						} else {
							i=s.indexOf("..nop:");
							if (i>=0) {
								String ss = s.substring(0,i);
								int val1 = getvalComplex(ss,vars);
								int j=s.lastIndexOf('.');
								ss = s.substring(j+1);
								int val2 = getvalComplex(ss,vars);
								if (val2<=val1) return false;
								String regexp = s.substring(i+6,j-1);
								for (j=val1+1;j<val2;j++) {
									if (formMatches(g.getMot(j).getPOS(), regexp)) return false;
								}
							} else {
								i=s.indexOf("..onlyp:");
								if (i>=0) {
									String ss = s.substring(0,i);
									int val1 = getvalComplex(ss,vars);
									int j=s.lastIndexOf('.');
									ss = s.substring(j+1);
									int val2 = getvalComplex(ss,vars);
									if (val2<=val1) return false;
									String regexp = s.substring(i+8,j-1);
									for (j=val1+1;j<val2;j++) {
										if (!formMatches(g.getMot(j).getPOS(), regexp)) return false;
									}
								} else {
									i=s.indexOf("..hasAncestor..");
									if (i>=0) {
										String ss = s.substring(0,i);
										int val1 = getvalComplex(ss,vars);
										int j=s.lastIndexOf('.');
										ss = s.substring(j+1);
										int val2 = getvalComplex(ss,vars);
										if (val2<=val1) return false;
										int co=0;
										for (int h=val1;co<50;co++) {
											int d=g.getDep(h);
											if (d<0) return false;
											h=g.getHead(d);
											if (h==val2) break;
										}
										if (co>=50) return false;
									} else {
										i=s.indexOf("..closest..");
										if (i>=0) {
											onlyfirstmatch=false;
											String dep1 = s.substring(0,i);
											int val1 = getvalComplex(dep1,vars);
											if (closestSecondPass>=0) {
												// on a deja cherche les candidats; il ne doit en rester plus qu'un seul !
												assert closestCandidates.size()==1;
												if (val1==closestSecondPass) return true;
												else return false;
											} else {
												int j=s.lastIndexOf('.');
												String dep2 = s.substring(j+1);
												int val2 = getvalComplex(dep2,vars);
												// WARNING: second term MUST be already defined ! (e.g. dep0..closest..dep1 is FORBIDDEN)
												closestCandidates.put(val1,val1-val2);
												return false;
											}
										} else {
											System.out.println("ERROR contrainte "+s);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean formMatches(String forme, String expr) {
		// cas particulier:
		if (expr.charAt(0)=='*') {
			// c'est une regexp !
			return Pattern.matches(expr.substring(1),forme);
		}
		// disjunction:
		int i=expr.indexOf('|');
		if (i>=0) {
			String[] ss = expr.split("\\|");
			for (String s : ss) {
				if (forme.equals(s)) return true;
			}
			return false;
		}
		// conjonction:
		i=expr.indexOf('&');
		if (i>=0) {
			String[] ss = expr.split("\\&");
			for (String s : ss) {
				if (!forme.equals(s)) return false;
			}
			return true;
		}
		// cas simple:
		if (forme.equals(expr)) return true;
		return false;
	}
	
	/**
	 * 
	 * @param g
	 * @param leftidx
	 * @param depidx
	 * @param vars
	 * @return true IFF there is a match
	 */
	private boolean findAllrec(DetGraph g, int leftidx, int depidx, HashMap<String, Object> vars) {
		gtmp=g;
		for (int i=0;i<leftidx;i++) {
			Integer useddep = (Integer)vars.get(dependent+i);
			if (useddep!=null && useddep==depidx) return false;
		}
		String r=leftET.get(leftidx);
		// essaye de matcher la regle r sur le mot-gov i
		r=r.replace('(', ' ').replace(')',' ').trim();
		String[] rs = r.split(",");

		//		System.out.println("searching: "+Arrays.toString(rs));
		if (rs.length>1) {
			// gov:
			if (rs[0].equals("_")) {
			} else if (rs[0].startsWith("f=")) {
				// forme imposee
				if (!formMatches(g.getMot(depidx).getForme().toLowerCase(),rs[0].substring(2))) return false;
			} else if (rs[0].startsWith("f|p=")) {
				int z=rs[0].indexOf('=',4);
				if (!formMatches(g.getMot(depidx).getForme().toLowerCase(),rs[0].substring(4,z)) &&
						!formMatches(g.getMot(depidx).getPOS(),rs[0].substring(z+1))) return false;
			} else if (rs[0].startsWith("f|g=")) {
				int z=rs[0].indexOf('=',4);
				if (!formMatches(g.getMot(depidx).getForme().toLowerCase(),rs[0].substring(4,z)) &&
						!isInGroup(depidx,rs[0].substring(z+1))) return false;
			} else if (rs[0].startsWith("f!=")) {
				// forme imposee
				if (formMatches(g.getMot(depidx).getForme().toLowerCase(),rs[0].substring(3))) return false;
			} else if (rs[0].startsWith("F=")) {
				// forme imposee
				if (!formMatches(g.getMot(depidx).getForme(),rs[0].substring(2))) return false;
			} else if (rs[0].startsWith("F!=")) {
				// forme imposee
				if (formMatches(g.getMot(depidx).getForme(),rs[0].substring(3))) return false;
			} else if (rs[0].startsWith("l=")) {
				// lemme impose
				if (!formMatches(g.getMot(depidx).getLemme(),rs[0].substring(2))) return false;
			} else if (rs[0].startsWith("l!=")) {
				// lemme impose
				if (formMatches(g.getMot(depidx).getLemme(),rs[0].substring(3))) return false;
			} else if (rs[0].startsWith("p=")) {
				// postag impose
				if (!formMatches(g.getMot(depidx).getPOS(),rs[0].substring(2))) return false;
			} else if (rs[0].startsWith("p!=")) {
				// postag impose
				if (formMatches(g.getMot(depidx).getPOS(),rs[0].substring(3))) return false;
			} else if (rs[0].startsWith("g=")) {
				// group impose
				int[] groups = g.getGroups(depidx);
				if (groups==null) return false;
				boolean foundone = false;
				for (int gr : groups) {
					if (formMatches(g.groupnoms.get(gr),rs[0].substring(2))) {
						// on verifie que c'est le 1er mot du groupe
						List<Mot> motsdugroupe = g.groups.get(gr);
						if (motsdugroupe.get(0).getIndexInUtt()-1==depidx) foundone=true;
					}
				}
				if (!foundone) return false;
			} else {
				// variable
				// doit etre de la forme gov1 head2, ...
				if (vars.containsKey(rs[0])) {
					// cette variable est deja affectee: il faut verifier si ca matche ce mot
					int m = (Integer)vars.get(rs[0]);
					if (m!=depidx) return false;
				} else {
					// var non affectee: on l'affecte
					vars.put(rs[0], depidx);
				}
			}
			vars.put(dependent+leftidx, depidx);
			
			// head:
			int dep = g.getDep(depidx);
			int head = -1; // ROOT par defaut
			if (dep>=0) head = g.getHead(dep);
			
			if (rs[1].equals("_")) {
			} else if (rs[1].startsWith("f=")) {
				// forme imposee
				if (head>=0&&!formMatches(g.getMot(head).getForme(),rs[1].substring(2))) return false;
			} else if (rs[1].startsWith("f!=")) {
				// forme imposee
				if (head>=0&&formMatches(g.getMot(head).getForme(),rs[1].substring(3))) return false;
			} else if (rs[1].startsWith("l=")) {
				// lemme impose
				if (head>=0&&!formMatches(g.getMot(head).getLemme(),rs[1].substring(2))) return false;
			} else if (rs[1].startsWith("l!=")) {
				// lemme impose
				if (head>=0&&formMatches(g.getMot(head).getLemme(),rs[1].substring(3))) return false;
			} else if (rs[1].startsWith("p=")) {
				// postag imposelab
				if (head>=0&&!formMatches(g.getMot(head).getPOS(),rs[1].substring(2))) return false;
			} else if (rs[1].startsWith("p!=")) {
				// postag impose
				if (head>=0&&formMatches(g.getMot(head).getPOS(),rs[1].substring(3))) return false;
			} else {
				// variable
				if (vars.containsKey(rs[1])) {
					// cette variable est deja affectee: il faut verifier si ca matche ce mot
					int m = (Integer)vars.get(rs[1]);
					if (m!=head) return false;
				} else {
					// var non affectee: on l'affecte
					vars.put(rs[1], head);
				}
			}
			vars.put(governor+leftidx, head);
		}
		if (rs.length>2) {
			// deplab
			int dep = g.getDep(depidx);
			String lab = "ROOT";
			if (dep>=0) lab=g.getDepLabel(dep);
			if (rs[2].equals("_")) {
				// label quelconque OK
			} else {
				// on peut avoir un ou plusieurs labels, ou une negation
				String x = rs[2];
				if (x.charAt(0)=='!') {
					x=rs[2].substring(1);
					String[] ss = x.split("\\|");
					// si on en trouve un dans la conjonction qui matche, alors ce n'est pas bon
					for (String xs : ss) {
						// si on en trouve un dans la disjunction qui matche, alors c'est bon
						if (lab.equals(xs)) return false;
					}
				} else {
					String[] ss = x.split("\\|");
					boolean oneIsOk = false;
					for (String xs : ss) {
						// si on en trouve un dans la disjunction qui matche, alors c'est bon
						if (lab.equals(xs)) {oneIsOk=true; break;}
					}
					if (!oneIsOk) return false;
				}
			}
			vars.put(deplabel+leftidx, lab);
		}
		if (leftidx>=leftET.size()-1) {
			// fin de la recursion

			if (!checkConstraints(g,vars)) return false;
			
			// TODO: verifier qu'en mettant le callback ici ne pose pas de pb pour le search F2 normal !
			if (!callback.callbackOneMatch(g, vars)) return true;
			if (rightET.size()>0) {
				replace(g,vars);
			}
			return true;
		}
		boolean didmatch=false;
		for (int i=0;i<g.getNbMots();i++) {
			// TODO: clone costs a lot ! replace !
			HashMap<String, Object> vars2 = (HashMap<String, Object>)vars.clone();
			if (findAllrec(g, leftidx+1, i, vars2)) didmatch=true;
			// Si on a un match avec dep0, on ne cherche pas d'autres dep1... qui matche ce dep0, on passe direct a dep0+1
			if (onlyfirstmatch&&didmatch&&vars.get(dependent+"0")!=null) return true;
		}
		return didmatch;
	}
	private HashMap<Integer,Integer> closestCandidates = new HashMap<Integer,Integer>();
	private int closestSecondPass=-1;
	
	/**
	 * if true, stop searching after one full match has been found for dep0
	 * this avoids multiple heads when using => dep0,dep1,...
	 */
	public boolean onlyfirstmatch = true;
	
	private void replace(DetGraph g, HashMap<String, Object> vars) {
		// on supprime d'abord toutes les deps qui matchaient
		// non: dorenavant, il faut explicitement detruire les deps
		// puis on ajoute les nouvelles deps
		for (String r : rightET) {
			if (r.charAt(0)=='-') {
				if (r.startsWith("-"+dependent)) {
					// detruit toutes les deps sortant d'un mot
					int gov = getvalComplex(r.substring(1), vars);
					assert gov>=0;
					int[] deps = g.getDeps(gov);
					Arrays.sort(deps);
					for (int i=deps.length-1;i>=0;i--) {
						g.removeDep(deps[i]);
					}
				} else if (r.startsWith("-"+deplabel+"=!")) {
					// detruit toutes les deps des phrases conservees ayant des labels autre que X
					String lab2keep = r.substring(6);
					for (int i=g.deps.size()-1;i>=0;i--) {
						if (g.getDepLabel(i).equals(lab2keep)) {
						} else {
							g.removeDep(i);
						}
					}
				} else if (r.startsWith("-"+deplabel+"=")) {
					// detruit toutes les deps des phrases conservees ayant le label X
					String lab2keep = r.substring(5);
					for (int i=g.deps.size()-1;i>=0;i--) {
						if (g.getDepLabel(i).equals(lab2keep)) {
							g.removeDep(i);
						} else {
						}
					}
				} else if (r.startsWith("-g"+dependent)) {
					int z = r.indexOf(',',1);
					if (z>=0) {
						// detruit tous les groupes sur cette sequence
						int gov1 = getvalComplex(r.substring(2,z), vars);
						int gov2 = getvalComplex(r.substring(z+1), vars);
						for (int i=g.groups.size()-1;i>=0;i--) {
							List<Mot> motsdugroupe = g.groups.get(i);
							if (motsdugroupe.get(0).getIndexInUtt()-1==gov1 &&
									motsdugroupe.get(motsdugroupe.size()-1).getIndexInUtt()-1==gov2) {
								g.groupnoms.remove(i);
								g.groups.remove(i);
							}
						}
					} else {
						// detruit tous les groupes débutant sur ce mot
						int gov = getvalComplex(r.substring(2), vars);
						for (int i=g.groups.size()-1;i>=0;i--) {
							List<Mot> motsdugroupe = g.groups.get(i);
							if (motsdugroupe.get(0).getIndexInUtt()-1==gov) {
								g.groupnoms.remove(i);
								g.groups.remove(i);
							}
						}
					}
				}
				continue;
			}
			r=r.replace('(', ' ').replace(')',' ').trim();
			String[] rs = r.split(",");
			if (rs[0].startsWith("g=")) {
				// on cree un groupe 
				int m1 = getvalComplex(rs[0].substring(2), vars);
				int m2 = getvalComplex(rs[1], vars);
				String groupname = rs[2];
				g.addgroup(m1, m2, groupname);
			} else {
				// on cree une dep
				int gov = getvalComplex(rs[0], vars);
				assert gov>=0;
				int head = getvalComplex(rs[1], vars);
				if (head<0) {
					// on lie au ROOT, donc on n'ajoute pas de dep
				} else {
					String lab=(String)vars.get(rs[2]);
					if (lab==null) lab=rs[2];
					// verifie qu'il n'y a pas deja une telle relation
					boolean isalreadythere = false;
					int[] ds = g.getDeps(gov);
					for (int d : ds) {
						if (g.getHead(d)==head && g.getDepLabel(d).equals(lab)) {isalreadythere=true; break;}
					}
					if (!isalreadythere) {
						g.ajoutDep(lab, gov, head);
					}
				}
			}
		}
	}
	
	/**
	 * just like replace, but does not modify the graph; instead check whether actually doing the modification would create new deps
	 * that were not here before.
	 * 
	 * @param g
	 * @param vars
	 * @return
	 */
	public boolean checkNewDeps(DetGraph g, HashMap<String, Object> vars) {
		for (String r : rightET) {
			if (r.charAt(0)=='-') continue;
			r=r.replace('(', ' ').replace(')',' ').trim();
			String[] rs = r.split(",");
			if (rs[0].startsWith("g=")) {
				// return true signifie qu'il y a creation d'une nouvelle dep
				return true;
			} else {
				// on cree une dep
				int gov = getvalComplex(rs[0], vars);
				int d=g.getDep(gov);
				int head = getvalComplex(rs[1], vars);
				if (head<0) {
					// on lie au ROOT, donc on n'ajoute pas de dep
					if (d>=0) {
						// on ne remplace JAMAIS une dep commencant par '_'
						if (g.getDepLabel(d).charAt(0)=='_') return false;
						else return true;
					}
				} else {
					String lab=(String)vars.get(rs[2]);
					if (lab==null) lab=rs[2];
					// verifie qu'il n'y a pas deja une telle relation
					boolean isalreadythere = false;
					int[] ds = g.getDeps(gov);
					for (int dd : ds) {
						if (g.getDepLabel(dd).charAt(0)=='_'||(g.getHead(dd)==head && g.getDepLabel(dd).equals(lab))) {isalreadythere=true; break;}
					}
					if (!isalreadythere) return true;
				}
			}
		}
		// nothing new is created !
		return false;
	}
	
	public static void saveMatchingWords(List<String> words) {
		try {
			PrintWriter f = FileUtils.writeFileUTF("matchingwords.txt");
			for (String s : words) {
				f.println(s);
			}
			f.close();
			System.out.println("saved matching words");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getMatchingWords(DetGraph g, HashMap<String, Object> vars) {
		String res = "";
		for (String key : vars.keySet()) {
			if (key.startsWith(dependent)||key.startsWith(governor)) {
				Integer motidx = (Integer)vars.get(key);
				if (motidx!=null&&motidx>=0)
					res+=key+"="+g.getMot(motidx).getForme()+" ";
			}
		}
		return res;
	}
	
	public void findAll(DetGraph g, int gidx) {
		for (int i=0;i<g.getNbMots();i++) {
			HashMap<String, Object> vars = new HashMap<String, Object>();
			vars.put("GRAPHID", gidx);
			
			// IMPORTANT: this suports only ONE=A SINGLE constraint ..closest.. !!!
			closestSecondPass=-1;
			closestCandidates.clear();
			boolean b = findAllrec(g,0,i,vars);
			if (closestCandidates.size()>0) {
				int closestx=-1; int dist=Integer.MAX_VALUE;
				for (int x : closestCandidates.keySet()) {
					int d=Math.abs(closestCandidates.get(x));
					if (d<dist) {dist=d; closestx=x;}
				}
				closestSecondPass=closestx;
				closestCandidates.clear();
				findAllrec(g,0,i,vars);
			}
		}
	}

	public static void main(String args[]) {
	}
}
