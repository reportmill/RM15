/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.ReportMill;
import snap.props.PropChange;
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

        // Create FilePanel
        _filePanel = createFilePanel();
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

        // Handle OpenButton
        if (anEvent.equals("OpenButton")) {
            WebFile selFile = _filePanel.getSelFileAndAddToRecentFiles();
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

        // Make editor window visible
        editorPane.setWindowVisible(true);

        // Show Samples or start SamplesButtonAnim
        if (showSamples)
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
        RMEditorPane editorPane = new RMEditorPane().openSource(aFile);
        if (editorPane == null)
            return;

        // Make editor window visible
        editorPane.setWindowVisible(true);

        // Hide WelcomePanel
        hide();
    }

    /**
     * Creates the FilePanel to be added to WelcomePanel.
     */
    private FilePanel createFilePanel()
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

        // Add PropChangeListener
        filePanel.addPropChangeListener(pc -> filePanelDidPropChange(pc));

        // Return
        return filePanel;
    }

    /**
     * Called when FilePanel does prop change.
     */
    private void filePanelDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();

        // Handle SelSite change:
        if (propName.equals(FilePanel.SelSite_Prop)) {
            WebSite selSite = _filePanel.getSelSite();;
            boolean minimize = !(selSite instanceof RecentFilesSite);
            setTopGraphicMinimized(minimize);
        }

        // Handle SelFile change: Update OpenButton.Enabled
        else if (propName.equals(FilePanel.SelFile_Prop)) {
            boolean isOpenFileSet = _filePanel.getSelFile() != null;
            getView("OpenButton").setEnabled(isOpenFileSet);
        }
    }

    /**
     * Load/configure top graphic WelcomePaneAnim.snp.
     */
    private View getTopGraphic()
    {
        // Unarchive WelcomePaneAnim.snp as DocView
        WebURL url = WebURL.getURL(WelcomePanel.class, "WelcomePanelAnim.snp");
        ChildView topGraphic = (ChildView) new ViewArchiver().getViewForSource(url);

        // Get page and clear border/shadow
        ParentView page = (ParentView) topGraphic.getChild(2);
        page.setBorder(null);
        page.setFill(null);
        page.setEffect(null);

        // Set BuildText, JavaText, LicenseText
        View buildText = topGraphic.getChildForName("BuildText");
        View jvmText = topGraphic.getChildForName("JVMText");
        View licText = topGraphic.getChildForName("LicenseText");
        buildText.setText("Build: " + SnapUtils.getBuildInfo().trim());
        jvmText.setText("JVM: " + (SnapUtils.isTeaVM ? "TeaVM" : System.getProperty("java.runtime.version")));
        licText.setText(ReportMill.getLicense() == null ? "Unlicensed Copy" : "License: " + ReportMill.getLicense());

        // Configure TopGraphic to call setTopGraphicMinimized() on click
        topGraphic.addEventHandler(e -> setTopGraphicMinimized(!isTopGraphicMinimized()), View.MouseRelease);

        // Return
        return topGraphic;
    }

    /**
     * Returns whether top graphic is minimized.
     */
    private boolean isTopGraphicMinimized()
    {
        ChildView mainView = getUI(ChildView.class);
        View topGraphic = mainView.getChild(0);
        return topGraphic.getHeight() < 200;
    }

    /**
     * Toggles the top graphic.
     */
    private void setTopGraphicMinimized(boolean aValue)
    {
        // Just return if already set
        if (aValue == isTopGraphicMinimized()) return;

        // Get TopGraphic
        ChildView mainView = getUI(ChildView.class);
        ChildView topGraphic = (ChildView) mainView.getChild(0);

        // Show/hide views below the minimize size
        topGraphic.getChild(2).setVisible(!aValue);
        ColView topGraphicColView = (ColView) topGraphic.getChild(1);
        for (int i = 2; i < topGraphicColView.getChildCount(); i++)
            topGraphicColView.getChild(i).setVisible(!aValue);

        // Handle Minimize: Size PrefHeight down
        if (aValue)
            topGraphic.getAnimCleared(600).setPrefHeight(140);

        // Handle normal: Size PrefHeight up
        else {
            topGraphic.setClipToBounds(true);
            topGraphic.getAnimCleared(600).setPrefHeight(240);
        }

        // Start anim
        topGraphic.playAnimDeep();
    }
}