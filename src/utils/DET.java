package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class DET {
	
	public static boolean withX = true;
	
	ArrayList<Float> scores = new ArrayList<Float>();
	ArrayList<Boolean> isgood = new ArrayList<Boolean>();
	
	public void updateExample(boolean isGoldPositif, float scoreIsPositif) {
		scores.add(scoreIsPositif);
		isgood.add(isGoldPositif);
	}
	
	public void calcROCwithRv0() {
		StringBuilder sbref = new StringBuilder("ref<-c(");
		StringBuilder sbrec = new StringBuilder("rec<-c(");
		int i;
		for (i=0;i<scores.size()-1;i++) {
			sbref.append(isgood.get(i)?"1,":"0,");
			sbrec.append(scores.get(i)+",");
		}
		sbref.append(isgood.get(i)?"1)":"0)");
		sbrec.append(scores.get(i)+")");
		
		try {
			PrintWriter fout = FileUtils.writeFileUTF("roc.R");
			fout.println("library(pROC)");
			fout.println("options(pROCProgress = list(name = \"none\"))");
			fout.println(sbref);
			fout.println(sbrec);
			fout.println("rocobj <- plot.roc(ref,rec,percent=TRUE,ci=TRUE,print.auc=TRUE)");
			fout.println("ciobj <- ci.se(rocobj,specificities=seq(0, 100, 20))");
			fout.println("plot(ciobj, type=\"shape\", col=\"#1c61b6AA\")");
			fout.close();
			
			File f = new File("roc.R");
			System.out.println("R script saved in "+f.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void calcROCwithR() {
		calcROCwithRv0();
		// utilise rocplus, qui a besoin du treatment = {scores des vrai positifs}
		//       et du control = {scores des vrais negatifs}
		
		StringBuilder streatment = new StringBuilder("treat<-c(");
		StringBuilder scontrol = new StringBuilder("contr<-c(");
		int i;
		for (i=0;i<scores.size();i++) {
			if (isgood.get(i)) {
				streatment.append(scores.get(i)+",");
			} else {
				scontrol.append(scores.get(i)+",");
			}
		}
		streatment.deleteCharAt(streatment.length()-1);
		streatment.append(')');
		scontrol.deleteCharAt(scontrol.length()-1);
		scontrol.append(')');
		
		try {
			PrintWriter fout = FileUtils.writeFileUTF("r.R");
			fout.println("library(rocplus)");
			fout.println(streatment);
			fout.println(scontrol);
			fout.println("rocplus(contr,treat,texture=\"rough\")");
			fout.close();
			
			File f = new File("r.R");
			System.out.println("R script saved in "+f.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getNex() {return scores.size();}
	/**
	 * nouvelle version beaucoup plus rapide
	 * 
	 * @return
	 */
	public float computeEER() {
		float smin=scores.get(0), smax=scores.get(0);
		for (int i=1;i<scores.size();i++) {
			if (scores.get(i)>smax) smax=scores.get(i);
			else if (scores.get(i)<smin) smin=scores.get(i);
		}
		// divise en 10 parties
		float seuilgauche=smin, seuildroit=smax;
		float frt=0,fat=0;
		while (smin<smax) {
			float sdelta = (smax-smin)/10f;
			if (sdelta==0) {
				System.out.println("ERROR DET EER "+smin+" "+smax);
				break;
			}
			float oldseuil=smin;
			boolean getout=false;
			for (float seuil=smin+sdelta;seuil<smax;seuil+=sdelta) {
				if (seuil==oldseuil) {
					seuildroit = seuil;
					System.out.println("WARNING DET EER "+smin+" "+smax+" "+sdelta+" "+seuil);
					getout=true;
					break;
				}
				int FA=0, FR=0, TA=0, TR=0;
				for (int i=0;i<scores.size();i++) {
					if (scores.get(i)<seuil) {
						if (isgood.get(i)) FR++;
						else TR++;
					} else {
						if (isgood.get(i)) TA++;
						else FA++;
					}
				}
				frt = (float)FR/(float)(TA+FR);
				fat = (float)FA/(float)(TR+FA);
				if (frt<fat) seuilgauche = seuil;
				else {
					seuildroit = seuil;
					// inutile d'aller plus loin
					break;
				}
				oldseuil=seuil;
			}
			System.out.println("smin "+smin+" smax "+smax+" "+frt+" "+fat);
			if (getout||Math.abs(fat-frt)<0.001) break;
			smin=seuilgauche; smax=seuildroit;
		}
		return (fat+frt)/2;
	}
	public float computeEERold() {
		float frgauche=-Float.MAX_VALUE, frdroit=Float.MAX_VALUE, fagauche=-Float.MAX_VALUE, fadroit=Float.MAX_VALUE;
		
		class Item implements Comparable<Item> {
			float sc;
			boolean isGood;
			public int compareTo(Item it) {
				if (sc>it.sc) return 1;
				else if (sc<it.sc) return -1;
				return 0;
			}
		}
		ArrayList<Item> items = new ArrayList<Item>();
		for (int i=0;i<scores.size();i++) {
			Item it = new Item();
			it.sc=scores.get(i);
			it.isGood=isgood.get(i);
			items.add(it);
		}
		Collections.sort(items);

		System.out.println("sorted collection "+items.size());
		System.out.println("collection min "+items.get(0).sc);
		System.out.println("collection max "+items.get(items.size()-1).sc);

		float sc=Float.NaN;
		for (int i=0;i<items.size();i++) {
			// on rejette jusqu'a l'exemple i inclu et tous ceux qui ont le meme score !
			Item it=items.get(i);
			if (it.sc==sc) continue;
			sc=it.sc;
			it=null;
			int FA=0, FR=0, TA=0, TR=0;
			for (int j=0;j<=i;j++) {
				it = items.get(j);
				if (it.isGood) FR++;
				else TR++;
			}
			for (int j=i+1;j<items.size();j++) {
				it = items.get(j);
				if (it.isGood) TA++;
				else FA++;
			}
			float frt = (float)FR/(float)(TA+FR);
			float fat = (float)FA/(float)(TR+FA);
			if (frt<fat) {
				// on est a gauche du EER
				if (frt>frgauche) {
					frgauche=frt;
					fagauche=fat;
				}
			} else {
				// on est a droite du EER
				if (frt<frdroit) {
					frdroit=frt;
					fadroit=fat;
					break; // inutile d'aller plus loin
				}
			}
		}
		
		// interpolation lineaire entre les points gauche et droit
		/*
		 * a frg + b = fag
		 * a frd + b = fad
		 * a (frd-frg) = (fad-fag)
		 * b = fag - a * frg
		 */

		System.out.println("interpol: point gauche: "+frgauche+" "+fagauche);
		System.out.println("interpol: point droit: "+frdroit+" "+fadroit);
		float diffx = (frdroit-frgauche);
		if (diffx==0) {
			// verticale ! x=frdroit, donc l'intersection est y=frdroit
			return frdroit;
		} else {
			float a = (fadroit-fagauche)/diffx;
			float b = fagauche - a * frgauche;
			System.out.println("interpol a b "+a+" "+b);
			/*
			 * on cherche le point de cette droite tel que ax+b=x
			 * (1-a)x=b
			 */
			float EER = b/(1f-a);
			return EER;
		}
	}
	public void showDET() {
		try {
			PrintWriter f = new PrintWriter(new FileWriter("/tmp/tt"));
			float scmin = scores.get(0);
			float scmax = scmin;
			for (int i=0;i<scores.size();i++)
				if (scmin>scores.get(i)) scmin=scores.get(i);
				else if (scmax<scores.get(i)) scmax=scores.get(i);
			float delta = (scmax-scmin)/50f;
			System.err.println("seuils min/max "+scmin+" "+scmax+" "+delta+" nex "+scores.size());
			scmin -= delta;
			scmax += delta;
			float seuil = scmin;
			while (seuil<=scmax) {
				int FA=0, FR=0, TA=0, TR=0;
				for (int i=0;i<scores.size();i++) {
					float r = scores.get(i);
					if (isgood.get(i)) {
						if (r>seuil) TA++;
						else {
							FR++;
						}
					} else {
						if (r>seuil) FA++;
						else TR++;
					}
				}
				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				f.println(frt+" "+fat);
				seuil+=delta;
			}
			f.println();
			f.println("0 1");
			f.println("1 0");
			f.close();
			f = new PrintWriter(new FileWriter("/tmp/ttt"));
			f.println("plot \"/tmp/tt\" notitle with lines");
			f.close();
			String gnucmd = findGnuplot();
			Runtime.getRuntime().exec(gnucmd+" /tmp/ttt -");
			BufferedReader ff = new BufferedReader(new InputStreamReader(System.in));
			ff.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String findGnuplot() {
		String[] cmds = {
				"gnuplot", "D:/xtof/softs/gnuplot/binary/gnuplot.exe"
		};
		String xcmd=cmds[0];
		for (String cmd : cmds) {
			try {
				Process p = Runtime.getRuntime().exec(cmd+" -V");
				InputStream is = p.getInputStream();
				p.waitFor();
				BufferedReader fis = new BufferedReader(new InputStreamReader(is));
				String s = fis.readLine();
				int ret = p.exitValue();
				if (ret==0)
					System.out.println("gnuplot trouvé ! "+cmd);
				xcmd=cmd;
				break;
			} catch (IOException e) {
				System.out.println("gnuplot non trouvé... cherche encore...");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return xcmd;
	}

	public float getAcc() {
		int nok = 0;
		for (int i=0;i<isgood.size();i++) {
			if (isgood.get(i)) {
				nok++;
			}
		}
		float r = (float)nok/(float)isgood.size();
		return r;
	}
	
	public float[] calcF(float p, float pi, float r, float ri) {
		float fmin=Float.MAX_VALUE,fmax=-Float.MAX_VALUE;
		
		float pmin = p-pi; if (pmin<0) pmin=0;
		float pmax = p+pi; if (pmax>1) pmax=1;
		float dp = (pmax-pmin)/10f;
		if (dp<0.00001) dp=0.00001f;

		float rmin = r-ri; if (rmin<0) rmin=0;
		float rmax = r+ri; if (rmax>1) rmax=1;
		float dr = (rmax-rmin)/10f;
		if (dr<0.00001) dr=0.00001f;
		
		for (float pp=pmin;pp<=pmax;pp+=dp) {
			for (float rr=rmin;rr<=rmax;rr+=dr) {
				float f = 2.f*pp*rr/(pp+rr);
				if (f<fmin) fmin=f;
				else if (f>fmax) fmax=f;
			}
		}
		float[] res = {fmin,fmax};
		return res;
	}
	
	/**
	 * je trace tous les seuils entre tous les points pour avoir la meilleure courbe possible !
	 */
	public void showDET2() {
		class Item implements Comparable<Item> {
			float sc;
			boolean isGood;
			public int compareTo(Item it) {
				if (sc>it.sc) return 1;
				else if (sc<it.sc) return -1;
				return 0;
			}
		}
		ArrayList<Item> items = new ArrayList<Item>();
		for (int i=0;i<scores.size();i++) {
			Item it = new Item();
			it.sc=scores.get(i);
			it.isGood=isgood.get(i);
			items.add(it);
		}
		Collections.sort(items);

		try {
			PrintWriter f = new PrintWriter(new FileWriter("/tmp/tt"));
			{
				// premier point: on accepte tous les items, sauf ceux de score -inf
				// tous les items de score -inf ne font pas partie de la courbe, mais ils sont rejetes systématiquement
				int FA=0, FR=0, TA=0, TR=0;
				for (int j=0;j<items.size();j++) {
					if (items.get(j).sc==-Float.MAX_VALUE) {
						// tous ceux-la, on les rejette
						if (items.get(j).isGood) FR++;
						else TR++;
					} else {
						// et ceux-ci, on les accepte
						if (items.get(j).isGood) TA++;
						else FA++;
					}
				}
				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				float prec = (float)TA/(float)(TA+FA);
				int nprec = TA+FA;
				float confintprec = 1.96f*(float)Math.sqrt(prec*(1f-prec)/nprec);
				float rapp = (float)TA/(float)(TA+FR);
				int nrecall = TA+FR;
				float confintrecall = 1.96f*(float)Math.sqrt(rapp*(1f-rapp)/nrecall);
				float acc  = (float)(TA+TR)/(float)(TA+TR+FA+FR);
				f.println(frt+" "+fat);
				
				// calcule la F-mes avec confint
				float[] fmes = calcF(prec,confintprec,rapp,confintrecall);
				System.out.println("FR="+FR+" TR="+TR+" FA="+FA+" TA="+TA+" frt="+frt+" fat="+fat+ " prec="+prec+"(+/-"+confintprec+") rapp="+rapp+"(+/-"+confintrecall+") fmes=["+fmes[0]+","+fmes[1]+"] acc="+acc+" thr=-inf");
			}
			float fmesmaxglob=-Float.MAX_VALUE, fmesmaxint=0;
			for (int i=0;i<items.size();i++) {
				// on rejette jusqu'a l'exemple i inclu et tous ceux qui ont le meme score !
				Item it=items.get(i);
				float sc = it.sc;
				// on "saute" le bloc des items qui ont le meme score, car faire varier le threshold ne permet
				// pas de les distinguer les uns des autres; ils sont donc tous inclus dans le meme point de la courbe
				{
					int j=i+1;
					for (;j<items.size();j++)
						if (items.get(j).sc!=sc) break;
					i=j-1;
				}
				it=null;
				int FA=0, FR=0, TA=0, TR=0;
				for (int j=0;j<=i;j++) {
					// tous ceux-la, on les rejette
					it = items.get(j);
					if (it.isGood) FR++;
					else TR++;
				}
				for (int j=i+1;j<items.size();j++) {
					// et ceux-ci, on les accepte
					it = items.get(j);
					if (it.isGood) TA++;
					else FA++;
				}
				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				float prec = (float)TA/(float)(TA+FA);
				int nprec = TA+FA;
				float confintprec = 1.96f*(float)Math.sqrt(prec*(1f-prec)/nprec);
				float rapp = (float)TA/(float)(TA+FR);
				int nrecall = TA+FR;
				float confintrecall = 1.96f*(float)Math.sqrt(rapp*(1f-rapp)/nrecall);
				float acc  = (float)(TA+TR)/(float)(TA+TR+FA+FR);
				f.println(frt+" "+fat);
				// calcule la F-mes avec confint
				float[] fmes = calcF(prec,confintprec,rapp,confintrecall);
				float fmesmid = (fmes[0]+fmes[1])/2f;
				if (fmesmid>fmesmaxglob) {
					fmesmaxglob=fmesmid; fmesmaxint=fmesmid-fmes[0];
				}
				System.out.println("FR="+FR+" TR="+TR+" FA="+FA+" TA="+TA+" frt="+frt+" fat="+fat+ " prec="+prec+"(+/-"+confintprec+") rapp="+rapp+"(+/-"+confintrecall+") fmes=["+fmes[0]+","+fmes[1]+"] acc="+acc+" thr=-inf");
			}
			System.out.println("EER="+computeEER());
			System.out.println("best f-mes: "+fmesmaxglob+" +/-"+fmesmaxint);

			f.println();
			f.println("0 1");
			f.println("1 0");
			f.close();
			f = new PrintWriter(new FileWriter("/tmp/ttt"));
			f.println("plot \"/tmp/tt\" notitle with lines");
			f.close();
			if (withX) {
				String gnucmd = findGnuplot();
				Runtime.getRuntime().exec(gnucmd+" /tmp/ttt -");
				BufferedReader ff = new BufferedReader(new InputStreamReader(System.in));
				ff.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void unittest() {
		boolean[] po = {true,true,false,false,true};
		float[] sc   = {10f,5f,7f,3f,6f};
		DET d = new DET();
		for (int i=0;i<po.length;i++) {
			d.updateExample(po[i], sc[i]);
			d.calcROCwithR();
		}
	}
	
	public static void main(String args[]) throws IOException {
		unittest();
//		DET d = new DET();
//		d.findGnuplot();
	}
	public static void oldmain(String args[]) throws IOException {
		// load un ARFF file et affiche la DET
		for (int z=0;z<args.length;z++) {
			PrintWriter fout = new PrintWriter(new FileWriter("/tmp/tt"+z));
			BufferedReader f = new BufferedReader(new FileReader(args[z]));
			int FRpos=-1, FApos=-1, Tpos=-1, TRpos=-1, TApos=-1;
			int attpos=0;
			for (;;) {
				String s = f.readLine();
				if (s.indexOf("@data")>=0) break;
				if (s.indexOf("@attribute")>=0) {
					if (s.indexOf("False Negatives")>=0) FRpos=attpos;
					if (s.indexOf("False Positives")>=0) FApos=attpos;
					if (s.indexOf("True Positives")>=0) TApos=attpos;
					if (s.indexOf("True Negatives")>=0) TRpos=attpos;
					if (s.indexOf("Threshold")>=0) Tpos=attpos;
					attpos++;
				}
			}
			if (FRpos==-1||FApos==-1||Tpos==-1||TRpos==-1||TApos==-1) {
				System.err.println("ERROR: FR/FA/thres non trouves !");
				return;
			}
			float frgauche=-Float.MAX_VALUE, frdroit=Float.MAX_VALUE, fagauche=-Float.MAX_VALUE, fadroit=Float.MAX_VALUE;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] ss = s.split(",");
				float FR = Float.parseFloat(ss[FRpos]);
				float FA = Float.parseFloat(ss[FApos]);
				float TR = Float.parseFloat(ss[TRpos]);
				float TA = Float.parseFloat(ss[TApos]);

				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				fout.println(frt+" "+fat);

				if (frt<fat) {
					// on est a gauche du EER
					if (frt>frgauche) {
						frgauche=frt;
						fagauche=fat;
					}
				} else if (frdroit==Float.MAX_VALUE) {
					// on est a droite du EER
					if (frt<frdroit) {
						frdroit=frt;
						fadroit=fat;
					}
				}

			}
			System.out.println("interpol: point gauche: "+frgauche+" "+fagauche);
			System.out.println("interpol: point droit: "+frdroit+" "+fadroit);
			float diffx = (frdroit-frgauche);
			if (diffx==0) {
				// verticale ! x=frdroit, donc l'intersection est y=frdroit
				System.out.println("EER= "+frdroit);
			} else {
				float a = (fadroit-fagauche)/diffx;
				float b = fagauche - a * frgauche;
				System.out.println("interpol a b "+a+" "+b);
				/*
				 * on cherche le point de cette droite tel que ax+b=x
				 * (1-a)x=b
				 */
				float EER = b/(1f-a);
				System.out.println("EER= "+EER);
			}

			f.close();
			fout.println("0 1");
			fout.println("1 0");
			fout.close();
		}
		PrintWriter ff = new PrintWriter(new FileWriter("/tmp/ttt"));
		ff.print("plot ");
		int z;
		for (z=0;z<args.length-1;z++) {
			ff.print("\"/tmp/tt"+z+"\" title \""+args[z]+"\" with lines,");
		}
		ff.print("\"/tmp/tt"+z+"\" title \""+args[z]+"\" with lines");
		ff.println();
		ff.close();
		Runtime.getRuntime().exec("gnuplot /tmp/ttt -");
		BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
		f.readLine();
	}
	
}
