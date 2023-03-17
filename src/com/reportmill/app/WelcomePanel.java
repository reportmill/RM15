/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.util.Prefs;
import snap.util.SnapUtils;
import snap.view.*;
import snap.viewx.FilePanel;
import snap.web.*;

/**
 * An implementation of a panel to manage/open user Snap sites (projects).
 */
public class WelcomePanel extends ViewOwner {

    // Whether welcome panel is enabled
    private boolean  _enabled;

    // The FilePanel
    private FilePanel  _filePanel;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    public static final String JAVA_FILE_EXT = "rpt";

    /**
     * Constructor.
     */
    protected WelcomePanel()
    {
        // Set as Shared (there should only be one instance)
        _shared = this;
    }

    /**
     * Returns the shared instance.
     */
    public static WelcomePanel getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new WelcomePanel();
    }

    /**
     * Returns whether welcome panel is enabled.
     */
    public boolean isEnabled()  { return _enabled; }

    /**
     * Sets whether welcome panel is enabled.
     */
    public void setEnabled(boolean aValue)  { _enabled = aValue; }

    /**
     * Shows the welcome panel.
     */
    public void showPanel()
    {
        getWindow().setVisible(true);
        resetLater();
    }

    /**
     * Hides the welcome panel.
     */
    public void hide()
    {
        // Hide window and flush prefs
        getWindow().setVisible(false);
        Prefs.getDefaultPrefs().flush();
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Add WelcomePaneAnim view
        View anim = getTopGraphic();
        getUI(ChildView.class).addChild(anim, 0);
        anim.playAnimDeep();

        // Create OpenPanel
        _filePanel = createOpenPanel();
        View filePanelUI = _filePanel.getUI();
        filePanelUI.setGrowHeight(true);

        // Add FilePanel.UI to ColView
        ColView topColView = (ColView) getUI();
        ColView colView2 = (ColView) topColView.getChild(1);
        colView2.addChild(filePanelUI, 1);

        // Hide ProgressBar
        getView("ProgressBar").setVisible(false);

        // Configure Window: Add WindowListener to indicate app should exit when close button clicked
        WindowView win = getWindow();
        win.setTitle("Welcome");
        enableEvents(win, WinClose);
        getView("OpenButton", Button.class).setDefaultButton(true);
    }

    /**
     * Responds to UI changes.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle SamplesButton
        if (anEvent.equals("SamplesButton"))
            newFile(true);

        // Handle NewButton
        if (anEvent.equals("NewButton"))
            newFile(false);

        // Handle OpenPanelButton
        //if (anEvent.equals("OpenPanelButton")) showOpenPanel();

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFile();
            openFile(selFile);
        }

        // Handle QuitButton
        if (anEvent.equals("QuitButton"))
            App.quitApp();

        // Handle WinClosing
        if (anEvent.isWinClose())
            hide();
    }

    /**
     * Creates a new file.
     */
    protected void newFile(boolean showSamples)
    {
        // Get new editor pane
        RMEditorPane epane = newEditorPane().newDocument();

        // If alt is down, replace with movies sample
        if (showSamples)
            epane = RMEditorPaneUtils.openSample("Movies");

        // Make editor window visible, show doc inspector, and order front after delay to get focus back from inspector
        epane.setWindowVisible(true);
        epane.getInspectorPanel().showDocumentInspector();
        RMEditorPane ep = epane;
        runLater(() -> ep.getWindow().toFront());
        runLater(() -> ep.getTopToolBar().startSamplesButtonAnim());

        // Hide WelcomePanel
        hide();
    }

    /**
     * Opens a document for given file.
     */
    public void openFile(WebFile aFile)
    {
        // Get the new editor pane that will open the document
        RMEditorPane epane = newEditorPane();
        epane = epane.open(aFile);

        // If no document opened, just return
        if (epane == null) return;

        // Make editor window visible, show doc inspector, and order front after delay to get focus back from inspector
        epane.setWindowVisible(true);
        epane.getInspectorPanel().showDocumentInspector();
        RMEditorPane ep = epane;
        runLater(() -> ep.getWindow().toFront());

        // Hide WelcomePanel
        hide();
    }

    /**
     * Creates the OpenPanel to be added to WelcomePanel.
     */
    private FilePanel createOpenPanel()
    {
        // Add recent files
        WebSite recentFilesSite = RecentFilesSite.getShared();
        FilePanel.addDefaultSite(recentFilesSite);

        // Add DropBox
        String dropBoxEmail = DropBoxSite.getDefaultEmail();
        WebSite dropBoxSite = DropBoxSite.getSiteForEmail(dropBoxEmail);
        FilePanel.addDefaultSite(dropBoxSite);

        // Get path from open panel for supported file extensions
        String[] extensions = { JAVA_FILE_EXT };
        FilePanel filePanel = new FilePanel() {
            @Override
            protected void fireActionEvent(ViewEvent anEvent)
            {
                WelcomePanel.this.fireActionEventForObject("OpenButton", anEvent);
            }
        };

        // Config
        filePanel.setTypes(extensions);
        filePanel.setSelSite(recentFilesSite);

        // Return
        return filePanel;
    }


    /**
     * Creates a new editor for new or opened documents.
     */
    public RMEditorPane newEditorPane()
    {
        // Otherwise, return new pane with UI loaded
        RMEditorPane ep = new RMEditorPane();
        if (SnapUtils.isTeaVM)
            ep.getUI().getWindow().setMaximized(true);
        return ep;
    }

    /**
     * Load/configure top graphic WelcomePaneAnim.snp.
     */
    private View getTopGraphic()
    {
        // Unarchive WelcomePaneAnim.snp as DocView
        WebURL url = WebURL.getURL(WelcomePanel.class, "WelcomePanelAnim.snp");
        ChildView doc = (ChildView) new ViewArchiver().getViewForSource(url);

        // Get page and clear border/shadow
        ParentView page = (ParentView) doc.getChild(2);
        page.setBorder(null);
        page.setFill(null);
        page.setEffect(null);

        // Set BuildText, JavaText, LicenseText
        View buildText = doc.getChildForName("BuildText");
        View jvmText = doc.getChildForName("JVMText");
        View licText = doc.getChildForName("LicenseText");
        buildText.setText("Build: " + SnapUtils.getBuildInfo().trim());
        jvmText.setText("JVM: " + System.getProperty("java.runtime.version"));
        licText.setText(ReportMill.getLicense() == null ? "Unlicensed Copy" : "License: " + ReportMill.getLicense());

        // Return
        return doc;
    }
}