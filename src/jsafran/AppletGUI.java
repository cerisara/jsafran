package jsafran;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

public class AppletGUI extends JApplet implements FocusListener {
	//Called when this applet is loaded into the browser.
	public void init() {
		//Execute a job on the event-dispatching thread:
		//creating this applet's GUI.
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					createGUI();
				}
			});
		} catch (Exception e) { 
			System.err.println("createGUI didn't successfully complete");
		}
	}
	public void start() {
		if (pan!=null) pan.requestFocusInWindow();
	}
    public String getAppletInfo() {
        return "Title: JSafran\n"
               + "Author: Christophe Cerisara\n"
               + "A lightweight version of the JSafran application designed to run in w browser.";
    }
    
    JSafranGUI pan=null;
    
	/**
     * Create the GUI. For thread safety, this method should
     * be invoked from the event-dispatching thread.
     */
    private void createGUI() {
    	System.out.println("creating GUI");
    	setFocusable(true);
    	JSafran main = new JSafran();
    	pan = JSafranGUI.createJsafranPanel(main);
    	main.safranPanel=pan;
    	setJMenuBar(pan.createMenus());
    	pan.setOpaque(true);
    	pan.setBackground(Color.white);
    	setContentPane(pan);
    	main.load("res:/ex.xml");
    	System.out.println("GUI created !");
    	
    	addFocusListener(this);
    }
    
	@Override
	public void focusGained(FocusEvent e) {
		System.out.println("gained focus");
	}
	@Override
	public void focusLost(FocusEvent e) {
		System.out.println("lost focus");
	}
}
