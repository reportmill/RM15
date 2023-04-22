/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.*;
import com.reportmill.shape.*;
import snap.geom.Rect;
import snap.gfx.*;
import snap.props.PropChange;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;
import snap.util.*;

import java.util.Objects;

/**
 * This class is a container for an RMEditor in an enclosing ScrollView with tool bars for editing.
 */
public class RMEditorPane extends RMViewerPane {

    // The menu bar owner
    private RMEditorPaneMenuBar  _menuBar;

    // The original editor, if in preview mode
    private RMEditor  _realEditor;

    // The shared editor inspector
    private InspectorPanel  _inspPanel = createInspectorPanel();

    // The shared attributes inspector (go ahead and create to get RMColorPanel created)
    private AttributesPanel  _attrsPanel = createAttributesPanel();

    // The image for a window frame icon
    private static Image _frameImg;

    /**
     * Constructor.
     */
    public RMEditorPane()
    {
        super();
    }

    /**
     * Returns the viewer as an editor.
     */
    public RMEditor getEditor()  { return (RMEditor) getViewer(); }

    /**
     * Overridden to return an RMEditor.
     */
    protected RMViewer createViewer()  { return new RMEditor(); }

    /**
     * Override to return as RMEditorPaneToolBar.
     */
    public RMEditorPaneToolBar getTopToolBar()  { return (RMEditorPaneToolBar) super.getTopToolBar(); }

    /**
     * Creates the top tool bar.
     */
    protected ViewOwner createTopToolBar()  { return new RMEditorPaneToolBar(this); }

    /**
     * Returns the SwingOwner for the menu bar.
     */
    public RMEditorPaneMenuBar getMenuBar()
    {
        if (_menuBar != null) return _menuBar;
        return _menuBar = createMenuBar();
    }

    /**
     * Creates the RMEditorPaneMenuBar for the menu bar.
     */
    protected RMEditorPaneMenuBar createMenuBar()  { return new RMEditorPaneMenuBar(this); }

    /**
     * Returns the datasource associated with the editor's document.
     */
    public RMDataSource getDataSource()
    {
        return getEditor().getDataSource();
    }

    /**
     * Sets the datasource associated with the editor's document.
     */
    public void setDataSource(RMDataSource aDataSource)
    {
        setDataSource(aDataSource, -1, -1);
    }

    /**
     * Sets the datasource for the panel.
     */
    public void setDataSource(RMDataSource aDataSource, double aX, double aY)
    {
        // Set DataSource in editor, show DataSource inspector, KeysBrowser and refocus window
        getEditor().setDataSource(aDataSource, aX, aY);
        if (getWindow().isVisible())
            getWindow().toFront();

        // Show KeysPanel (after delay, so XML icon animation can finish)
        runLaterDelayed(2000, () -> getAttributesPanel().setVisibleName(AttributesPanel.KEYS));
    }

    /**
     * Sets a datasource from a given URL at a given point (if dragged in).
     */
    public void setDataSource(WebURL aURL, double aX, double aY)
    {
        // Create DataSource and load dataset
        RMDataSource dataSource = new RMDataSource(aURL);
        try {
            dataSource.getDataset();
        }

        // If failed, get error message and run error panel
        catch (Throwable t) {

            // Get cause
            Throwable rootCause = t;
            while (rootCause.getCause() != null)
                rootCause = rootCause.getCause();

            // Get error message
            String error = StringUtils.wrap(rootCause.toString(), 40);
            rootCause.printStackTrace();

            // Run dialog box
            runLater(() -> {
                DialogBox dialogBox = new DialogBox("Error Parsing XML");
                dialogBox.setErrorMessage(error);
                dialogBox.showMessageDialog(getUI());
            });
            return;
        }

        // Set DataSource in editor, show DataSource inspector, KeysBrowser and refocus window
        setDataSource(dataSource, aX, aY);
    }

    /**
     * Returns whether editor is really doing editing.
     */
    public boolean isEditing()
    {
        return getEditor().isEditing();
    }

    /**
     * Sets whether editor is really doing editing.
     */
    public void setEditing(boolean aFlag)
    {
        // If editor already has requested editing state, just return
        if (aFlag == isEditing()) return;

        // Hide attributes drawer
        hideAttributesDrawer();

        // If not yet previewing, store current template then generate report and swap it in
        if (!aFlag) {

            // Cache current editor and flush any current editing
            _realEditor = getEditor();
            _realEditor.flushEditingChanges();

            // Get doc/dataset and generate report
            RMDocument doc = getDoc();
            Object dataset = _realEditor.getDataSourceDataset();
            RMDocument report = doc.generateReport(dataset);

            // Create new editor, set editing to false and set report document
            RMEditor editor = new RMEditor();
            editor.setEditing(false);
            editor.setDoc(report);

            // If generateReport hit any keyChain parsing errors, run message dialog
            if (RMKeyChain.getError() != null) {
                String err = RMKeyChain.getAndResetError();
                DialogBox dbox = new DialogBox("Error Parsing KeyChain");
                dbox.setErrorMessage(err);
                dbox.showMessageDialog(getUI());
            }

            // Set new editor
            setViewer(editor);
        }

        // If turning preview off, restore real editor
        else setViewer(_realEditor);

        // Focus on editor
        requestFocus(getEditor());
        resetLater();
    }

    /**
     * Initializes the UI.
     */
    protected View createUI()
    {
        // Get AttributesPanel (early so editor pane can register with correct ColorPanel)
        AttributesPanel attrPanel = getAttributesPanel();
        attrPanel.getUI();

        // Get InspectorPanel
        InspectorPanel inspPanel = getInspectorPanel();
        View inspPanelUI = inspPanel.getUI();
        inspPanelUI.setGrowHeight(true);

        // Create ColView to hold them
        ColView colView = new ColView();
        colView.setFillWidth(true);
        colView.setBorder(Color.LIGHTGRAY, 1);
        colView.addChild(inspPanelUI);

        // Create normal RMViewerPane BorderView UI and panels to right side
        BorderView bview = (BorderView) super.createUI();
        bview.setRight(colView);

        // Install AttributesPanel
        ParentView rbox = getRulerBox();
        attrPanel.getDrawer().showTabButton(rbox);

        // Create ColView holding MenuBar and EditorPane UI (with key listener so MenuBar catches shortcut keys)
        View mbarView = MenuBar.createMenuBarView(getMenuBar().getUI(), bview);
        return mbarView;
    }

    /**
     * Override to configure Window.
     */
    protected void initUI()
    {
        // Do normal version
        super.initUI();

        // Enable Events for editor
        enableEvents(getEditor(), MousePress, MouseRelease);

        // Listen for Editor PropChanges
        getEditor().addPropChangeListener(pc -> editorDidPropChange(pc));

        // Configure Window ClassName, Image and enable window events
        WindowView window = getWindow();
        window.setImage(getFrameIcon());
        enableEvents(window, WinClose);
        if (SnapUtils.isTeaVM)
            window.setMaximized(true);
    }

    /**
     * Updates the editor's UI panels.
     */
    protected void resetUI()
    {
        // Do normal update
        super.resetUI();

        // If title has changed, update window title
        if (isWindowVisible()) {
            String title = getWindowTitle();
            WindowView win = getWindow();
            if (!Objects.equals(title, win.getTitle())) {
                win.setTitle(title);
                win.setDocURL(getSourceURL());
            }
        }

        // Reset MenuBar, InspectorPanel and AttributesPanel
        if (!ViewUtils.isMouseDown()) getMenuBar().resetLater();
        if (getInspectorPanel().isResetWithEditor()) getInspectorPanel().resetLater();
        if (getAttributesPanel().isVisible() && !ViewUtils.isMouseDown()) getAttributesPanel().resetLater();
    }

    /**
     * Handles changes to the editor's UI controls.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Forward on to menu bar
        getMenuBar().dispatchEventToOwner(anEvent);

        // Do normal version
        super.respondUI(anEvent);

        // Handle PopupTrigger
        if (anEvent.isPopupTrigger() && !anEvent.isConsumed())
            runPopupMenu(anEvent);

            // If Editor.MouseClick and DataSource is set and we're editing and DataSource icon clicked, show DS Inspector
        else if (anEvent.isMouseClick() && getDataSource() != null && isEditing()) {
            Rect r = getEditor().getVisRect(); // Get visible rect
            if (anEvent.getX() > r.getMaxX() - 53 && anEvent.getY() > r.getMaxY() - 53) { // If DataSource icon clicked
                if (anEvent.isShortcutDown()) setDataSource(null); // If cmd key down, clear the DataSource
                else getInspectorPanel().setVisible(7); // otherwise show DataSource inspector
            }

            // If mouse isn't in lower right corner and DataSource inspector is showing, show shape specific inspector
            else if (getInspectorPanel().isShowingDataSource())
                getInspectorPanel().setVisible(0);
        }

        // Handle WinClosing
        else if (anEvent.isWinClose()) {
            close();
            anEvent.consume();
        }
        //else if(anEvent.isWinResized()) { //Dimension wsize=getWindow().getSize(), psize=getWindow().getPreferredSize();
        //if(Math.abs(wsize.width-psize.width)<=10) wsize.width = psize.width;
        //if(Math.abs(wsize.height-psize.height)<=10) wsize.height = psize.height;
        //if(getWindow().getWidth()!=wsize.width || getWindow().getHeight()!=wsize.height) getWindow().setSize(wsize);
    }

    /**
     * Returns the inspector panel (shared).
     */
    public InspectorPanel getInspectorPanel()
    {
        return _inspPanel;
    }

    /**
     * Creates the InspectorPanel.
     */
    protected InspectorPanel createInspectorPanel()
    {
        return new InspectorPanel(this);
    }

    /**
     * Returns the attributes panel (shared).
     */
    public AttributesPanel getAttributesPanel()
    {
        return _attrsPanel;
    }

    /**
     * Creates the AttributesPanel.
     */
    protected AttributesPanel createAttributesPanel()
    {
        return new AttributesPanel(this);
    }

    /**
     * Shows the AttributesPanel Drawer.
     */
    public void showAttributesDrawer()
    {
        getAttributesPanel().showDrawer();
    }

    /**
     * Hides the AttributesPanel Drawer.
     */
    public void hideAttributesDrawer()
    {
        getAttributesPanel().hideDrawer();
    }

    /**
     * Returns extension for editor document.
     */
    public String[] getFileExtensions()
    {
        return new String[]{".rpt", ".pdf"};
    }

    /**
     * Returns the description for the editor document for use in open/save panels.
     */
    public String getFileDescription()
    {
        return "ReportMill files (.rpt, .pdf)";
    }

    /**
     * Returns the window title: filename + path + optional "Doc edited asterisk + optional "Doc Scaled"
     */
    public String getWindowTitle()
    {
        // Get window title: "Filename - path"
        String title = "Untitled";
        WebURL sourceURL = getSourceURL();
        if (sourceURL != null) {
            String filename = sourceURL.getFilename();
            String filePath = PathUtils.getParent(sourceURL.getPath());
            title = filename + " - " + filePath;
        }

        // If has undos, add asterisk. If zoomed, add ZoomFactor
        if (getEditor().getUndoer() != null && getEditor().getUndoer().hasUndos())
            title = "* " + title;
        if (!MathUtils.equals(getEditor().getZoomFactor(), 1f))
            title += " @ " + Math.round(getEditor().getZoomFactor() * 100) + "%";

        // If previewing, add "(Previewing)" and return
        if (getEditor().isPreview())
            title += " (Previewing)";

        // Return
        return title;
    }

    /**
     * Creates a new default editor pane.
     */
    public RMEditorPane newDocument()
    {
        return openSource(new RMDocument(612, 792));
    }

    /**
     * Creates a new editor window from an open panel.
     */
    public RMEditorPane open(View aView)
    {
        // Get path from open panel for supported file extensions
        String path = FilePanel.showOpenPanel(aView, getFileDescription(), getFileExtensions());
        return openSource(path);
    }

    /**
     * Creates a new editor window by opening the document from the given source.
     */
    public RMEditorPane openSource(Object aSource)
    {
        // If document source is null, just return null
        if (aSource == null) return null;

        // Get Source URL
        WebURL sourceURL = WebURL.getURL(aSource);

        // If source is already opened, return editor pane
        if (!Objects.equals(sourceURL, getSourceURL())) {
            RMEditorPane[] editorPanes = WindowView.getOpenWindowOwners(RMEditorPane.class);
            for (RMEditorPane editorPane : editorPanes)
                if (Objects.equals(sourceURL, editorPane.getSourceURL()))
                    return editorPane;
        }

        // Load document
        RMDocument doc = null;
        try {
            doc = RMDocument.getDoc(aSource);
        }

        // If there was an XML parse error loading aSource, show error dialog
        catch (Exception e) {
            e.printStackTrace();
            String msg = StringUtils.wrap("Error reading file:\n" + e.getMessage(), 40);
            runLater(() -> {
                DialogBox dialogBox = new DialogBox("Error Reading File");
                dialogBox.setErrorMessage(msg);
                dialogBox.showMessageDialog(getUI());
            });
        }

        // If no document, just return null
        if (doc == null)
            return null;

        // If old version, warn user that saving document will make it unreadable by RM7
        if (doc.getVersion() < 7.0) {
            String msg = "This document has been upgraded from an older version.\n" +
                    "If saved, it will not open in earlier versions.";
            DialogBox dialogBox = new DialogBox("Warning: Document Upgrade");
            dialogBox.setWarningMessage(msg);
            dialogBox.showMessageDialog(getUI());
        }

        // Set document
        getViewer().setDoc(doc);

        // If source is URL, add to recent files
        if (sourceURL != null)
            RecentFiles.addURL(sourceURL);

        // Return
        return this;
    }

    /**
     * Saves the current editor document, running the save panel.
     */
    public void saveAs()
    {
        // Make sure editor isn't previewing
        setEditing(true);

        // Run save panel, set Document.Source to path and re-save (or just return if cancelled)
        String fileDescription = getFileDescription();
        String[] fileExtensions = getFileExtensions();
        String path = FilePanel.showSavePanel(getUI(), fileDescription, fileExtensions);
        if (path == null)
            return;

        // Set URL and save
        RMDocument doc = getDoc();
        WebURL docURL = WebURL.getURL(path);
        doc.setSourceURL(docURL);
        save();
    }

    /**
     * Saves the current editor document, running the save panel if needed.
     */
    public void save()
    {
        // If can't save to current source, do SaveAs instead
        WebURL url = getSourceURL();
        if (url == null) {
            saveAs();
            return;
        }

        // Make sure editor isn't previewing and has focus (to commit any inspector textfield changes)
        setEditing(true);
        getEditor().requestFocus();

        // Do actual save - if exception, print stack trace and set error string
        try { saveImpl(); }
        catch (Throwable e) {
            e.printStackTrace();
            String msg = "The file " + url.getPath() + " could not be saved (" + e + ").";
            DialogBox dialogBox = new DialogBox("Error on Save");
            dialogBox.setErrorMessage(msg);
            dialogBox.showMessageDialog(getUI());
            return;
        }

        // Add URL to RecentFiles, clear undoer and reset UI
        RecentFiles.addURL(url);
        getDoc().getUndoer().reset();
        resetLater();
    }

    /**
     * The real save method.
     */
    protected void saveImpl()
    {
        // Get doc file
        WebURL docURL = getSourceURL();
        WebFile docFile = docURL.getFile();
        if (docFile == null)
            docFile = docURL.createFile(false);

        // Get/set doc bytes
        RMDocument doc = getDoc();
        byte[] docBytes = doc.getBytes();
        docFile.setBytes(docBytes);

        // Save doc file
        docFile.save();
    }

    /**
     * Reloads the current editor document from the last saved version.
     */
    public void revert()
    {
        // Get filename (just return if null)
        WebURL sourceURL = getSourceURL();
        if (sourceURL == null)
            return;

        // Run option panel for revert confirmation (just return if denied)
        String msg = "Revert to saved version of " + sourceURL.getFilename() + "?";
        DialogBox dialogBox = new DialogBox("Revert to Saved");
        dialogBox.setQuestionMessage(msg);
        if (!dialogBox.showConfirmDialog(getUI()))
            return;

        // Re-open filename
        sourceURL.getFile().reload();
        openSource(sourceURL);
    }

    /**
     * Show samples.
     */
    public void showSamples()
    {
        setEditing(true);
        getTopToolBar().stopSamplesButtonAnim();
        hideAttributesDrawer();
        new SamplesPane().showSamples(this, url -> showSamplesDidReturnURL(url));
    }

    /**
     * Called when SamplesPane returns a URL.
     */
    private void showSamplesDidReturnURL(WebURL aURL)
    {
        // Hack support for "Add Movies Dataset" DatasetButton
        if (aURL == null) {
            RMEditorPaneUtils.connectToDataSource(this);
            return;
        }

        // Open URL
        openSource(aURL);
        getEditor().requestFocus();
        RecentFiles.addURL(aURL);
    }

    /**
     * Closes this editor pane
     */
    public boolean close()
    {
        // Make sure editor isn't previewing
        setEditing(true);

        // If unsaved changes, run panel to request save
        if (getEditor().undoerHasUndos()) {
            String fname = getSourceURL() == null ? "untitled document" : getSourceURL().getFilename();
            String msg = "Save changes to " + fname + "?";
            String[] options = { "Save", "Don't Save", "Cancel" };
            DialogBox dialogBox = new DialogBox("Unsaved Changes");
            dialogBox.setWarningMessage(msg);
            dialogBox.setOptions(options);
            switch (dialogBox.showOptionDialog(getUI(), "Save")) {
                case 0: save();
                case 1: break;
                default: return false;
            }
        }

        // Do real close (run later because Java 8 on MacOS sometimes leaves a zombie window after above dialog)
        runLater(() -> closeQuick());
        return true;
    }

    /**
     * Closes window without checking for save.
     */
    protected void closeQuick()
    {
        // Hide window
        getWindow().hide();

        // If another open editor is available focus on it, otherwise run WelcomePanel
        RMEditorPane editorPane = WindowView.getOpenWindowOwner(RMEditorPane.class);
        if (editorPane != null)
            editorPane.getEditor().requestFocus();
        else if (WelcomePanel.getShared().isEnabled())
            WelcomePanel.getShared().showPanel();
    }

    /**
     * Called when the app is about to exit to gracefully handle any open documents.
     */
    public void quit()
    {
        App.quitApp();
    }

    /**
     * Returns a popup menu for the editor.
     */
    public void runPopupMenu(ViewEvent anEvent)
    {
        // Get selected shape (just return if page is selected)
        Menu popupMenu = new Menu();
        RMShape selShape = getEditor().getSelectedOrSuperSelectedShape();
        if (selShape instanceof RMPage)
            return;

        // If RMTextShape, get copy of Format menu
        if (selShape instanceof RMTextShape) {
            RMTextShape text = (RMTextShape) selShape;

            // Get editor pane format menu and add menu items to popup
            Menu formatMenu = getMenuBar().getView("FormatMenu", Menu.class);
            Menu formatMenuCopy = (Menu) formatMenu.clone();
            for (MenuItem m : formatMenuCopy.getItems())
                popupMenu.addItem(m);

            // If structured tablerow, add AddColumnMenuItem and SplitColumnMenuItem
            if (text.isStructured()) {
                MenuItem mi = new MenuItem(); mi.setText("Add Column"); mi.setName("AddColumnMenuItem");
                popupMenu.addItem(mi);
                mi = new MenuItem(); mi.setText("Split Column"); mi.setName("SplitColumnMenuItem");
                popupMenu.addItem(mi);
            }
        }

        // Get copy of shapes menu and add menu items to popup
        Menu shapesMenu = getMenuBar().getView("ShapesMenu", Menu.class);
        Menu shapesMenuCopy = (Menu) shapesMenu.clone();
        for (MenuItem m : shapesMenuCopy.getItems())
            popupMenu.addItem(m);

        // Initialize popup menu items to send Events to menu bar
        popupMenu.setOwner(getMenuBar());
        popupMenu.show(getEditor(), anEvent.getX(), anEvent.getY());
        anEvent.consume();
    }

    /**
     * Called when Editor has prop change.
     */
    private void editorDidPropChange(PropChange aPC)
    {
        String propName = aPC.getPropName();
        switch (propName) {
            case RMEditor.SelShapes_Prop: resetLater(); break;
            case RMEditor.SuperSelShape_Prop: resetLater(); break;
        }
    }

    /**
     * Returns the icon for the editor window frame.
     */
    private static Image getFrameIcon()
    {
        if (_frameImg != null) return _frameImg;
        return _frameImg = Image.get(RMEditorPane.class, "ReportMill16x16.png");
    }

    /**
     * A class for any editor pane support panes.
     */
    public static class SupportPane extends ViewOwner {

        // The editor pane
        RMEditorPane _editorPane;

        /**
         * Creates a new SupportPane with given editor pane.
         */
        public SupportPane(RMEditorPane anEP)
        {
            _editorPane = anEP;
        }

        /**
         * Returns the EditorPane.
         */
        public RMEditorPane getEditorPane()  { return _editorPane; }

        /**
         * Sets the EditorPane.
         */
        public void setEditorPane(RMEditorPane anEP)  { _editorPane = anEP; }

        /**
         * Returns the editor.
         */
        public RMEditor getEditor()  { return _editorPane.getEditor(); }

        /**
         * Returns the title.
         */
        public String getWindowTitle()  { return "Inspector"; }
    }
}