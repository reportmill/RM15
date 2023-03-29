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
    private boolean  _enabled = true;

    // The FilePanel
    private FilePanel  _filePanel;

    // The shared instance
    private static WelcomePanel _shared;

    // Constants
    public static final String RM_FILE_EXT = "rpt";
    public static final String PDF_FILE_EXT = "pdf";

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
        RMEditorPane editorPane = new RMEditorPane().newDocument();

        // Make editor window visible, show doc inspector, and order front after delay to get focus back from inspector
        editorPane.setWindowVisible(true);
        editorPane.getInspectorPanel().showDocumentInspector();
        runLater(() -> editorPane.getWindow().toFront());

        // Show Samples or start SamplesButtonAnim
        if (showSamples) //editorPane = RMEditorPaneUtils.openSample("Movies");
            runLaterDelayed(300, () -> editorPane.showSamples());
        else runLater(() -> editorPane.getTopToolBar().startSamplesButtonAnim());

        // Hide WelcomePanel
        hide();
    }

    /**
     * Opens a document for given file.
     */
    public void openFile(WebFile aFile)
    {
        // Get the new editor pane that will open the document
        RMEditorPane editorPane = new RMEditorPane();
        editorPane = editorPane.openSource(aFile);

        // If no document opened, just return
        if (editorPane == null) return;

        // Make editor window visible, show doc inspector, and order front after delay to get focus back from inspector
        editorPane.setWindowVisible(true);
        editorPane.getInspectorPanel().showDocumentInspector();
        RMEditorPane ep = editorPane;
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

        // Create/config FilePanel
        FilePanel filePanel = new FilePanel();
        String[] EXTENSIONS = { RM_FILE_EXT, PDF_FILE_EXT };
        filePanel.setTypes(EXTENSIONS);
        filePanel.setSelSite(recentFilesSite);
        filePanel.setActionHandler(e -> WelcomePanel.this.fireActionEventForObject("OpenButton", e));

        // Return
        return filePanel;
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