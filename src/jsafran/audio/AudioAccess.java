package jsafran.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import plugins.utils.FileUtils;

import tools.audio.Player;
import utils.SuiteDeMots;

import jsafran.DetGraph;

public class AudioAccess {
	static int nmotsInSeg = 30;
	String wavfile = null;
	File jtrfile = null;
	private Player player=new Player();

	public AudioInputStream getAudioStreamFromSec(float sec) {
		if (wavfile==null) return null;
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavfile));
			if (ais==null) {
				JOptionPane.showMessageDialog(null, "ERROR no audiostream from "+wavfile);
				return null;
			}
			AudioFormat format = ais.getFormat();
			float frPerSec = format.getFrameRate();
			int byPerFr = format.getFrameSize();
			float fr2skip = frPerSec*sec;
			long by2skip = (long)(fr2skip*(float)byPerFr);
			ais.skip(by2skip);
			return ais;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void listen(List<DetGraph> gs, int gidx, int widx) {
		if (jtrfile==null) {
			JOptionPane.showMessageDialog(null, "Give me the JTRANS file please !");
			JFileChooser jfc = new JFileChooser();
			int ret=jfc.showOpenDialog(null);
			if (ret==JFileChooser.APPROVE_OPTION) {
				jtrfile = jfc.getSelectedFile();
			}
		}
		int debfr = getDebPlayback(gs, gidx, widx, jtrfile);
		System.out.println("debframe "+debfr);
		File wavf=new File(wavfile);
		if (!wavf.exists()) {
			JOptionPane.showMessageDialog(null, "Give me the WAV file please !");
			JFileChooser jfc = new JFileChooser();
			int ret=jfc.showOpenDialog(null);
			if (ret==JFileChooser.APPROVE_OPTION) {
				File f = jfc.getSelectedFile();
				wavf = f;
			}
		}
		// play
		float sec = (float)debfr/100f;
		player.play(getAudioStreamFromSec(sec));
	}

	public void stop() {
		player.stopPlaying();
	}
	
	class Player {
		private int mixidx=-1;
		private SourceDataLine line=null;
		private AudioFormat format=null;
		private AudioInputStream data;
		private boolean stop=false;
		private boolean isplaying=false;

		public void setMixer(int m) {mixidx=m;}

		public void stopPlaying() {
			stop=true;
		}

		public boolean isPlaying() {
			return isplaying;
		}

		private void openLine(int mixidx) {
			System.out.println("openline "+mixidx+" "+line+" "+format);
			if (mixidx<0) {
				try {
					line = AudioSystem.getSourceDataLine(format);
					line.open(format);
				} catch (LineUnavailableException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Veuillez fermer tous les logiciels qui pourraient \nï¿½tre en train d'utiliser la ligne audio");
				}
			} else {
				if (isPlaying()) stopPlaying();
				Mixer.Info[] mixersinfo = AudioSystem.getMixerInfo();
				Mixer mix = AudioSystem.getMixer(mixersinfo[mixidx]);
				DataLine.Info lineinfo = new DataLine.Info(SourceDataLine.class,format);
				try {
					line = (SourceDataLine)mix.getLine(lineinfo);
					System.out.println("line = "+line);
					line.open();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void play(AudioInputStream ais) {
			data=ais;
			format = ais.getFormat();
			openLine(mixidx);
			stop = false;
			line.start();
			Thread remplisseurThread = new Remplisseur();
			remplisseurThread.setPriority(Thread.MAX_PRIORITY);
			remplisseurThread.setName("Thread Remplisseur");
			remplisseurThread.start();
			isplaying=true;
			line.start();
		}

		private class Remplisseur extends Thread {
			public Remplisseur() {
				super("PlayerRemplisseurThread");
			}
			public void run(){
				//Remplissage du buffer
				byte[] frame = new byte[2];

				//on continue Ã  la remplir
				try {
					while (!stop) {
						int nread;
						nread = data.read(frame);
						if (nread<0) break;
						line.write(frame, 0, nread);
					}
					if (!stop)
						while (line.available()>0) //on laisse le temps Ã  la line de se vider
							Thread.sleep(100);
					line.flush();
					line.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				isplaying=false;
			}
		}
	}

	/**
	 * 
	 * @return le temps de debut du playback
	 */
	public int getDebPlayback(List<DetGraph> gs, int gidx, int widx, File jtrans) {
		ArrayList<String> motsjsafran = new ArrayList<String>();
		
		DetGraph g = gs.get(gidx);
		for (int j=widx;j<g.getNbMots();j++) {
			motsjsafran.add(g.getMot(j).getForme().toLowerCase());
			if (motsjsafran.size()>nmotsInSeg) break;
		}
		if (motsjsafran.size()<nmotsInSeg)
			for (int i=gidx+1;i<gs.size();i++) {
				g = gs.get(i);
				for (int j=0;j<g.getNbMots();j++) {
					motsjsafran.add(g.getMot(j).getForme().toLowerCase());
					if (motsjsafran.size()>nmotsInSeg) break;
				}
				if (motsjsafran.size()>nmotsInSeg) break;
			}
		int nmotsjsaf = motsjsafran.size();
		String[] motsjsaf = motsjsafran.toArray(new String[nmotsjsaf]);
		SuiteDeMots sjsaf = new SuiteDeMots(motsjsaf);
		
		ArrayList<String> jtransmots = new ArrayList<String>();
		ArrayList<Integer> jtransdeb = new ArrayList<Integer>();
		ArrayList<Integer> jtransfin = new ArrayList<Integer>();
		try {
			BufferedReader f= new BufferedReader(new FileReader(jtrans));
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				if (s.startsWith("wavname=")) {
					wavfile=s.substring(9).trim();
					continue;
				}
				if (s.contains("listeelements")) {
					int n=Integer.parseInt(s.substring(14));
					for (int i=0;i<n;i++) f.readLine();
					int firstframe = Integer.parseInt(f.readLine());
					int nwords = Integer.parseInt(f.readLine());
					for (int i=0;i<nwords;i++) {
						jtransmots.add(f.readLine().toLowerCase());
						s=f.readLine();
						String[] ss = s.split(" ");
						jtransdeb.add(Integer.parseInt(ss[0]));
						jtransfin.add(Integer.parseInt(ss[1]));
					}
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int bestdeb=0;
		int besterr = Integer.MAX_VALUE;
		for (int i=0;i<jtransmots.size()-nmotsInSeg;i++) {
			String[] stjtr = new String[nmotsInSeg];
			for (int j=0;j<nmotsInSeg;j++) stjtr[j]=jtransmots.get(i+j);
			SuiteDeMots sjtr = new SuiteDeMots(stjtr);
			
			sjsaf.align(sjtr);
			int nerr = sjsaf.getDelError()+sjsaf.getInsError()+sjsaf.getSubstError();
			if (nerr<besterr) {
				bestdeb=i; besterr=nerr;
			}
		}
		System.out.print("bestalign ");
		for (int i=0;i<20;i++) {
			System.out.print(jtransmots.get(bestdeb+i)+" ");
		}
		System.out.println();
		return jtransdeb.get(bestdeb);
	}
	
}
