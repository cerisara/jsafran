package jsafran.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.SshSessionFactory;
import utils.FileUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import jsafran.DetGraph;
import jsafran.GraphIO;
import jsafran.JSafran;

/**
 * checks out and in corpora directly from Jsafran main window to a git server.
 * 
 * How to do it:
 * The idea is to ship within JSafran a private key that correspond to a single "externalUser" in gittalc1
 * So that anybody can pull/push without registering.
 * 
 * It may be possible to consider also writing the MAC address of the computer from which
 * the commit has been made in a secret file that is also commited...
 * 
 * @author cerisara
 *
 */
public class GitCorpus {
	public static void main(String args[]) {
		System.out.println("OK");
		GitCorpus m = new GitCorpus();
		m.getFromGit();
		File f=new File(".");
		System.out.println("cloned in "+f.getAbsolutePath());
	}
	
	
	final static public String CORPGIT = "jsafran_tmp_gitdir";
	// TODO: the (eventual) layers are linked from within each DetGraph
	
	public File currentlyLoaded = null;

	private void guiSolve(List<String> sol1, List<String> sol2) {
		GraphIO gio =new GraphIO(null);
		try {
			PrintWriter y = FileUtils.writeFileUTF("tmpjsacflt1.xml");
			y.println("<checkenc é>");
			y.println("<encoding=UTF-8/>");
			for (String s : sol1) {
				y.println(s);
			}
			y.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<DetGraph> gsA = gio.loadAllGraphs("tmpjsacflt1.xml");
		try {
			PrintWriter y = FileUtils.writeFileUTF("tmpjsacflt2.xml");
			y.println("<checkenc é>");
			y.println("<encoding=UTF-8/>");
			for (String s : sol2) {
				y.println(s);
			}
			y.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<DetGraph> gsB = gio.loadAllGraphs("tmpjsacflt2.xml");
		List<DetGraph>[] gss = new List[2];
		gss[0] = gsA; gss[1]=gsB;
		JSafran fr = JSafran.viewGraph(gss);
		List<DetGraph> chosen = fr.levels.get(fr.curlevel);
		gio.save(chosen, "tmpjsacflt3.xml");
		sol1.clear();
		try {
			BufferedReader x = FileUtils.openFileUTF("tmpjsacflt3.xml");
			for (;;) {
				String s = x.readLine();
				if (s==null) break;
				sol1.add(s);
			}
			x.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void solveConflict() {
		File cdir = new File(CORPGIT);
		File[] fs = cdir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".xml")) return true;
				return false;
			}
		});
		for (File f : fs) {
			System.out.println("solving conflict for file "+f.getAbsolutePath());
			try {
				BufferedReader x = FileUtils.openFileUTF(f.getAbsolutePath());
				int nconflicts=0;
				int confpos=0;
				ArrayList<String> stack = new ArrayList<String>();
				ArrayList<String> stackfin = new ArrayList<String>();
				ArrayList<String> stack2= new ArrayList<String>();
				for (;;) {
					String s = x.readLine();
					if (s==null) break;
					if (confpos==0&&s.startsWith("<SEQ ")) {stackfin.addAll(stack); stack.clear(); stack2.clear(); stack.add(s);}
					else if (confpos==0&&s.startsWith("<<<<<<< ")) {confpos++; stack2.addAll(stack);}
					else if (confpos==1&&s.equals("=======")) {confpos++;}
					else if (confpos==2&&s.startsWith(">>>>>>> ")) {
						confpos++;
						nconflicts++;}
					else if (confpos==3&&s.startsWith("</SEQ>")) {
						stack.add(s);
						stack2.add(s);
						guiSolve(stack,stack2);
						confpos=0;
					}
					else if (confpos<2) stack.add(s);
					else if (confpos==2) stack2.add(s);
					else if (confpos>2) {stack2.add(s); stack.add(s);}
				}
				x.close();
				
				PrintWriter y = FileUtils.writeFileUTF(f);
				for (String s : stackfin) {
					y.println(s);
				}
				y.close();
				
				System.out.println("\t found "+nconflicts+" conflicts");
				if (nconflicts>0) {
					add(f);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		commit();
	}
	
	private void pull() {
		commit();
		JSch.setConfig("StrictHostKeyChecking", "no");
		JSch sch = new JSch();
		try {
			final String prvkey = CORPGIT+"_k/id_rsa";
			
			class M extends JschConfigSessionFactory {
				@Override
				protected void configure(Host arg0, Session arg1) {
					try {
						JSch sc = getJSch(arg0, null);
						sc.removeAllIdentity();
						sc.addIdentity(prvkey);
					} catch (JSchException e) {
						e.printStackTrace();
					}
				}
			}
			M m = new M();
			SshSessionFactory.setInstance(m);
			
			localRepo = new FileRepository(CORPGIT+ "/.git");
			git = new Git(localRepo);
			PullResult res = git.pull().call();
			if (res.getMergeResult().getMergeStatus()==MergeStatus.CONFLICTING) {
				System.out.println("pull done found conflicts");
				solveConflict();
			} else 
				System.out.println("pull done noconflicts "+res);
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void add(File f) {
		try {
			localRepo = new FileRepository(CORPGIT+ "/.git");
			git = new Git(localRepo);
			DirCache res = git.add().addFilepattern(f.getName()).call();
			System.out.println("add done w/ "+f.getAbsolutePath()+" "+res);
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void commit() {
		JSch.setConfig("StrictHostKeyChecking", "no");
		JSch sch = new JSch();
		try {
			final String prvkey = CORPGIT+"_k/id_rsa";
			
			class M extends JschConfigSessionFactory {
				@Override
				protected void configure(Host arg0, Session arg1) {
					try {
						JSch sc = getJSch(arg0, null);
						sc.removeAllIdentity();
						sc.addIdentity(prvkey);
					} catch (JSchException e) {
						e.printStackTrace();
					}
				}
			}
			M m = new M();
			SshSessionFactory.setInstance(m);
			
			localRepo = new FileRepository(CORPGIT+ "/.git");
			git = new Git(localRepo);
			RevCommit res = git.commit().setAll(true).setMessage("USERID "+System.getenv("USER")).call();
			System.out.println("commit res: "+res);
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void push() {
		commit();
		JSch.setConfig("StrictHostKeyChecking", "no");
		JSch sch = new JSch();
		try {
			final String prvkey = CORPGIT+"_k/id_rsa";
			
			class M extends JschConfigSessionFactory {
				@Override
				protected void configure(Host arg0, Session arg1) {
					try {
						JSch sc = getJSch(arg0, null);
						sc.removeAllIdentity();
						sc.addIdentity(prvkey);
					} catch (JSchException e) {
						e.printStackTrace();
					}
				}
			}
			M m = new M();
			SshSessionFactory.setInstance(m);
			
			localRepo = new FileRepository(CORPGIT+ "/.git");
			git = new Git(localRepo);
			Iterable<PushResult> res2 = git.push().setPushAll().call();
			System.out.println("push done ");
			for (PushResult r : res2) System.out.println("pushres "+r);
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendToGit() {
		checkGit();
		push();
	}
	
	public List<DetGraph> getFromGit() {
		checkGit();
		pull();

		File f = new File(CORPGIT);
		File[] fs = f.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".xml")) return true;
				return false;
			}
		});
		if (fs.length==0) {
			currentlyLoaded=null;
			return null;
		}
		
		GraphIO gio = new GraphIO(null);
		currentlyLoaded=fs[0];
		return gio.loadAllGraphs(fs[0].getAbsolutePath());
	}

	public void reset() {
		FileUtils.recursiveDelete(CORPGIT);
	}
	
	public void checkGit() {
		System.out.println("check pwd "+((new File(".")).getAbsolutePath()));
		// $HOME peut poser des problemes, si il existe un $HOME/.ssh qui contient des clefs SSH: ces clefs seront utilisees a la place de la clef qu'on definit ci-dessous !
		System.out.println("check HOME "+System.getenv("HOME"));
/*
		ProcessBuilder pb = new ProcessBuilder();
		Map<String,String> env = pb.environment();
	env.remove("HOME");
	*/
		
		File gitdir = new File(CORPGIT);
		if (gitdir.exists()) return;
		
		{
			System.out.println("checking... First time detected: cloning dir from server...");
			// create the key file
			File f = new File(CORPGIT+"_k");
			f.mkdirs();
			PrintWriter pf;
			try {
				pf = new PrintWriter(new FileWriter(CORPGIT+"_k/id_rsa"));
				pf.println("-----BEGIN RSA PRIVATE KEY-----");
				pf.println("MIIEpAIBAAKCAQEAylhHrUwQXPBlGEu9Io/gXTv94zGBUbFvPWoWELAkodq19DAJ");
				pf.println("HIiteZ/JBWqif/d1G5zV2lz2o0+3uZ3v0FsvfCaUBCqPKhRlTKSEC+Nd1oAf03GM");
				pf.println("Kc3B+FA9e/AW8fkqcP0G3FYBw/SibVJId+pGk+6NMvB7b8iBoZsL+w/wqlrP5+4I");
				pf.println("8QFB8KsBX0oESb55UZxQvAlC/QJ3FT4P90EuC4HmJ3H1cPqKPV4TMspDbrfkB2K0");
				pf.println("Hi5hSlSTpdhN9X75TTDr575LEph2zXw0y3+At5K33tqoN1gftROczy8QdXoS/CW2");
				pf.println("bfP98ADuZ+6hTF98sFRzC7tWfy5gT9hgnqr2aQIDAQABAoIBAQDGOePGLu0cz1iK");
				pf.println("i7A3CrInRF091EK3EFdE2AfTw2uvsD5ugRx7+p3Pt6xMBSI6sObl7ShHqqjoZnE7");
				pf.println("gzDrclk7i+OXjUYBWEfA5K6DovNL7uq+zs8cKPlsVNdW8mbYGFvuosK02gESwjbZ");
				pf.println("WDGsYDNSOvHSxGp4oPn1opoDE6OWjnBZtT4whVSPNppQCe1HfsFNopAIsOpuGghW");
				pf.println("8ZkNjXDsYwgcG5c0QTeRQmGyDSKEuRhr2rpevjLIJqQk5sNhMYKm1RQ6jiaZh3/x");
				pf.println("ti/IAQa4aR8E4pDqMOHbxrzQ4D0hEAgsDiBMEhEjqK62z14TIc99yqG3runF1iXU");
				pf.println("7AiXW6GBAoGBAOqQCrBVYr07gPC/lwn40xCYjbhObEDnUyV5Ylv7pM5GC89mOrvj");
				pf.println("HrCHKogmHv153QK48rBrZKCuPKn55CHdNqu1/l/0/8gJIPTMZmouDa9CG3PwXmgT");
				pf.println("uwhZqCnYQcljmTZJ0cqWYckiJbni+9GqxXRpMIWu2f4p7haOBIY1kPJxAoGBANzW");
				pf.println("c8bDcFcti7uopE2WFol+Tczea3Hi0gDE8rjYyY2K941gwDqgmgYhAbu7vAOOPvpg");
				pf.println("BWxzxhDjw67JzbFzuIkk8/DlwYBzUZUNDPcqOhFxMSuDVLVxqrhFpn/ZZ394QMTa");
				pf.println("FJ3cUpVOn+JCHsCMkaDlOPFH99Ls17WLAIJsj895AoGAZ3JFNIVDuZ8Pe52TWBOD");
				pf.println("yjLtdZYoies7MTC6X0S45zvfI4W7a3d75nCGImtliXaAMR4t8f46795H5NLPeNvd");
				pf.println("q5bk657aW3cjMLMgi71pzZxDDTu59v6Uotcfoey8/rtNK7McsdLmp1TG/JcNZeeB");
				pf.println("k5h8jMiTXVNSrGHPvvzKwxECgYALdBfnHCXMb7FKl1GGMJ2UnfddA8Ag1Pm9Tnmk");
				pf.println("OqIhdC6op3bw03mJjdawLOlwacU8aRR+7nY8VAPHIfJIHM9aVY9NVC04A0Mc7uNY");
				pf.println("QllmpC8/qX0QOAf5rKsZAGaMxujdDM567X1e/wftzS4ZIHFBHgJZCTmCOARsBvyM");
				pf.println("xyKiSQKBgQCxMaDG+dtACg/ifLZfxCWk6xXm06Pn0h1mEWDnSiZ3dokoQh3wcw8W");
				pf.println("A0rfg3HgUO7SSQn2KQ8YRrhKGe76dfEIOZmrUcfv2aILtDydNcYt5knXkWh2iO25");
				pf.println("XesNuSNWiMidkftIP1UX2SOsqjjLBJrnX4nTEUwGBH6iJtiDHtV6Sw==");
				pf.println("-----END RSA PRIVATE KEY-----");
				pf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		JSch.setConfig("StrictHostKeyChecking", "no");
		JSch sch = new JSch();
		try {
			final String prvkey = CORPGIT+"_k/id_rsa";
			
			class M extends JschConfigSessionFactory {
				@Override
				protected void configure(Host arg0, Session arg1) {
					try {
						JSch sc = getJSch(arg0, null);
						sc.removeAllIdentity();
						sc.addIdentity(prvkey);
					} catch (JSchException e) {
						e.printStackTrace();
					}
				}
			}
			M m = new M();
			SshSessionFactory.setInstance(m);
			
			CloneCommand c = Git.cloneRepository();
			c.setURI("ssh://git@152.81.128.45/xtof/jsafrancorpus");
			c.setDirectory(new File(CORPGIT));
			c.call();
			
			// TODO: est-ce que la session est fermee auto a la fin du call ?

			System.out.println("disconnected");
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	// =====================================
	// TUTO


	private String localPath, remotePath;
	private Repository localRepo;
	private Git git;

	public void init() throws IOException {
		localPath = "/home/me/repos/mytest";
		remotePath = "git@github.com:me/mytestrepo.git";
		localRepo = new FileRepository(localPath + "/.git");
		git = new Git(localRepo);        
	}

	public void testCreate() throws IOException {
		Repository newRepo = new FileRepository(localPath + ".git");
		newRepo.create();
	}

	public void testClone() throws IOException, NoFilepatternException {     
		try {
			Git.cloneRepository() 
			.setURI(remotePath)
			.setDirectory(new File(localPath))
			.call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}  
	}

	public void testAdd() throws IOException, NoFilepatternException { 
		File myfile = new File(localPath + "/myfile");
		myfile.createNewFile();
		try {
			git.add()
			.addFilepattern("myfile")
			.call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void testCommit() throws IOException, NoHeadException, NoMessageException,     ConcurrentRefUpdateException, JGitInternalException, WrongRepositoryStateException {
		try {
			git.commit()
			.setMessage("Added myfile")
			.call();
		} catch (UnmergedPathsException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void testPush() throws IOException, JGitInternalException, InvalidRemoteException {     
		try {
			git.push()
			.call();
		} catch (TransportException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}    

	public void testTrackMaster() throws IOException, JGitInternalException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException {     
		try {
			git.branchCreate() 
			.setName("master")
			.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
			.setStartPoint("origin/master")
			.setForce(true)
			.call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}

	public void testPull() throws IOException, WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException, RefNotFoundException, NoHeadException {
		try {
			git.pull()
			.call();
		} catch (TransportException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
	}
}
