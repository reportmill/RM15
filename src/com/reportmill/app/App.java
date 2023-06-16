/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.gfx.GFXEnv;
import snap.util.*;
import snap.view.ViewTheme;
import snap.view.ViewUtils;
import snap.view.WindowView;
import snap.viewx.DialogBox;
import snap.viewx.ExceptionReporter;

/************************************* - All files should be 120 chars wide - *****************************************/

/**
 * This is the main class for the ReportMill app.
 */
public class App {

    // Whether app is in process of quiting
    private static boolean _quiting;

    /**
     * Standard main method.
     */
    public static void main(String[] args)
    {
        ViewUtils.runLater(() -> startApp(args));
    }

    /**
     * Starts the app.
     */
    public static void startApp(String[] args)
    {
        // Set app is true
        ReportMill.isApp = true;

        // Set default preferences
        Prefs prefs = Prefs.getPrefsForName("/com/reportmill");
        Prefs.setDefaultPrefs(prefs);

        // Install Exception reporter
        ExceptionReporter er = new ExceptionReporter("ReportMill");
        er.setToAddress("support@reportmill.com");
        er.setInfo("ReportMill Version " + ReportMill.getVersion() + ", Build Date: " + ReportMill.getBuildInfo());
        Thread.setDefaultUncaughtExceptionHandler(er);

        // Set Theme
        ViewTheme.setThemeForName("Light");

        // Run welcome panel
        WelcomePanel.getShared().showPanel();
    }

    /**
     * Quits the app (can be invoked by anyone).
     */
    public static void quitApp()
    {
        // Get open editor panes
        if (_quiting) return;
        _quiting = true;
        RMEditorPane[] editorPanes = WindowView.getOpenWindowOwners(RMEditorPane.class);

        // Iterate over open Editors to see if any have unsaved changes
        int answer = 0;
        for (int i = 0, iMax = editorPanes.length; i < iMax && iMax > 1; i++) {
            RMEditorPane editorPane = editorPanes[i];

            // Turn off editor preview
            editorPane.setEditing(true);

            // If editor has undos, run Review Unsaved panel and break
            if (editorPane.getEditor().undoerHasUndos()) {
                DialogBox dialogBox = new DialogBox("Review Unsaved Documents");
                dialogBox.setWarningMessage("There are unsaved documents");
                dialogBox.setOptions("Review Unsaved", "Quit Anyway", "Cancel");
                answer = dialogBox.showOptionDialog(editorPane.getEditor(), "Review Unsaved");
                break;
            }
        }

        // If user hit Cancel, just go away
        if (answer == 2) {
            _quiting = false;
            return;
        }

        // Disable welcome panel
        boolean old = WelcomePanel.getShared().isEnabled();
        WelcomePanel.getShared().setEnabled(false);

        // If Review Unsaved, iterate through _editors to see if they should be saved or if user wants to cancel instead
        if (answer == 0) {
            for (RMEditorPane editorPane : editorPanes)
                if (!editorPane.close()) {
                    WelcomePanel.getShared().setEnabled(old);
                    _quiting = false;
                    return;
                }
        }

        // Flush Properties to registry and exit
        try { Prefs.getDefaultPrefs().flush(); }
        catch (Exception e) { e.printStackTrace(); }
        GFXEnv.getEnv().exit(0);
    }
}