/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.apple.eawt.*;
import com.apple.eawt.AppEvent.*;
import com.reportmill.base.ReportMill;
import javax.swing.SwingUtilities;
import snap.util.*;
import snap.view.WindowView;
import snap.viewx.DialogBox;
import snap.viewx.ExceptionReporter;

/************************************* - All files should be 120 chars wide - *****************************************/

/**
 * This is the main class for the ReportMill app. You can run it from the command line like this:
 *
 *     prompt> java -cp ReportMill.jar com.reportmill.App
 * 
 */
public class App {
    
    // Whether app is in process of quiting
    static boolean _quiting;

/**
 * This is the static main method, called by Java when launching with com.reportmill.App.
 */
public static void main(String args[])  { new App(args); }

/**
 * Creates a new app instance.
 */
public App(String args[])
{
    // Set app is true
    ReportMill.isApp = true;
    
    // Set default preferences
    Prefs.setPrefsDefault(Prefs.getPrefs(com.reportmill.Shell.class));
    
    // Mac specific stuff
    if(SnapUtils.isMac) new AppleAppHandler().init();
    
    // Install Exception reporter
    ExceptionReporter er = new ExceptionReporter("ReportMill"); er.setToAddress("support@reportmill.com");
    er.setInfo("ReportMill Version " + ReportMill.getVersion() + ", Build Date: " + ReportMill.getBuildInfo());
    Thread.setDefaultUncaughtExceptionHandler(er);
    
    // Run welcome panel
    Welcome.getShared().runWelcome();
}

/**
 * Quits the app (can be invoked by anyone).
 */
public static void quitApp()
{
    // Get open editor panes
    if(_quiting) return; _quiting = true;
    RMEditorPane epanes[] = WindowView.getOpenWindowOwners(RMEditorPane.class);

    // Iterate over open Editors to see if any have unsaved changes
    int answer = 0;
    for(int i=0, iMax=epanes.length; i<iMax && iMax>1; i++) { RMEditorPane epane = epanes[i];
        
        // Turn off editor preview
        epane.setEditing(true);
        
        // If editor has undos, run Review Unsaved panel and break
        if(epane.getEditor().undoerHasUndos()) {
            DialogBox dbox = new DialogBox("Review Unsaved Documents");
            dbox.setWarningMessage("There are unsaved documents");
            dbox.setOptions("Review Unsaved", "Quit Anyway", "Cancel");
            answer = dbox.showOptionDialog(epane.getEditor(), "Review Unsaved");
            break;
        }
    }

    // If user hit Cancel, just go away
    if(answer==2) { _quiting = false; return; }
    
    // Disable welcome panel
    boolean old = Welcome.getShared().isEnabled(); Welcome.getShared().setEnabled(false);

    // If Review Unsaved, iterate through _editors to see if they should be saved or if user wants to cancel instead
    if(answer==0)
        for(RMEditorPane epane : epanes)
            if(!epane.close()) {
                Welcome.getShared().setEnabled(old); _quiting = false; return; }

    // Flush Properties to registry and exit
    try { Prefs.get().flush(); } catch(Exception e) { e.printStackTrace(); }
    System.exit(0);
}

/**
 * A class to handle apple events.
 */
private static class AppleAppHandler implements PreferencesHandler, QuitHandler, OpenFilesHandler {

    /** Initializes Apple Application handling. */
    public void init()
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true"); // 1.4
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "RMStudio 14");
        Application app = Application.getApplication();
        app.setPreferencesHandler(this); app.setQuitHandler(this); app.setOpenFileHandler(this);
    }

    /** Handle Preferences. */
    public void handlePreferences(PreferencesEvent arg0)  { new PreferencesPanel().showPanel(null); }

    /** Handle Preferences. */
    public void openFiles(OpenFilesEvent anEvent)
    {
        java.io.File file = anEvent.getFiles().size()>0? anEvent.getFiles().get(0) : null; if(file==null) return;
        SwingUtilities.invokeLater(() -> Welcome.getShared().open(file.getPath()));
    }

    /** Handle QuitRequest. */
    public void handleQuitRequestWith(QuitEvent arg0, QuitResponse arg1)
    {
        App.quitApp();
        if(_quiting) arg1.cancelQuit();
    }
}

}