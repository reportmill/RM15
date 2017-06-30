package com.reportmill.app;
import java.awt.Dimension;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import snap.view.*;

/**
 * A custom class.
 */
public class AppletRM extends JApplet {

    JLabel _label;
    
/**
 * Create new AppletTest.
 */
public AppletRM()
{
    System.out.println("AppletRM create");
    resize(1000,1000);
}

public void init()
{
    System.out.println("AppletRM init");
    
    _label = new JLabel("Launching AppletRM..."); _label.setHorizontalAlignment(SwingConstants.CENTER);
    setContentPane(_label);

    Timer timer = new Timer();
    TimerTask task = new TimerTask() { public void run() { init2(); }};
    timer.schedule(task, 2000);
}

public void init2()
{
    System.out.println("AppletRM init2");
    
    RMEditorPane epane = new RMEditorPane();
    System.out.println("AppletRM init2 new edpane");
    epane = epane.newDocument();
    System.out.println("AppletRM init2 new doc");
    JComponent ecomp = epane.getRootView().getNative(JComponent.class);
    System.out.println("AppletRM init2 root view");
    ecomp.setPreferredSize(new Dimension(1000,1000));
    setContentPane(ecomp);
    ViewUtils.setShowing(epane.getRootView(), true);
    revalidate(); repaint();
    System.out.println("AppletRM init2 done");
}

/*public static void main(String args[])
{
    JFrame frame = new JFrame();
    AppletRM arm = new AppletRM(); arm.init2();
    frame.setContentPane(arm);
    frame.setBounds(400,400,1000,1000);
    frame.setVisible(true);
}*/

}