package garbage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import utils.FileUtils;

public class UseFeats {
	public static void main(String args[])  throws IOException {
		BufferedReader f= FileUtils.openFileUTF("dev.conll");
		PrintWriter g=FileUtils.writeFileUTF("dev2.conll");
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			s=s.trim();
			if (s.length()==0) {
				g.println(s);
				continue;
			}
			String[] ss =s.split("\\t");
			// cheating
			ss[6] = ss[10];
			for (int i=0;i<ss.length-1;i++)
				g.print(ss[i]+"\t");
			g.println(ss[ss.length-1]);
		}
		g.close();
		f.close();
	}
}
