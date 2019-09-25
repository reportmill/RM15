/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.view.*;
import snap.viewx.*;
import snap.web.*;
import snap.util.*;

/**
 * This class is a container for an RMEditor in an enclosing ScrollView with tool bars for editing.
 */
public class RMEditorPane extends RMViewerPane {

    // The menu bar owner
    RMEditorPaneMenuBar    _menuBar;

    // The original editor, if in preview mode
    RMEditor               _realEditor;
    
    // The shared editor inspector
    InspectorPanel         _inspPanel = createInspectorPanel();
    
    // The shared attributes inspector (go ahead and create to get RMColorPanel created)
    AttributesPanel        _attrsPanel = createAttributesPanel();
    
    // The image for a window frame icon
    private static Image   _frameImg;

/**
 * Creates a new EditorPane.
 */
public RMEditorPane()  { }

/**
 * Returns the viewer as an editor.
 */
public RMEditor getEditor()  { return (RMEditor)getViewer(); }

/**
 * Overridden to return an RMEditor.
 */
protected RMViewer createViewer()  { return new RMEditor(); }

/**
 * Override to return as RMEditorPaneToolBar.
 */
public RMEditorPaneToolBar getTopToolBar()  { return (RMEditorPaneToolBar)super.getTopToolBar(); }

/**
 * Creates the top tool bar.
 */
protected ViewOwner createTopToolBar()  { return new RMEditorPaneToolBar(this); }

/**
 * Returns the SwingOwner for the menu bar.
 */
public RMEditorPaneMenuBar getMenuBar()  { return _menuBar!=null? _menuBar : (_menuBar = createMenuBar()); }

/**
 * Creates the RMEditorPaneMenuBar for the menu bar.
 */
protected RMEditorPaneMenuBar createMenuBar()  { return new RMEditorPaneMenuBar(this); }

/**
 * Returns the datasource associated with the editor's document.
 */
public RMDataSource getDataSource()  { return getEditor().getDataSource(); }

/**
 * Sets the datasource associated with the editor's document.
 */
public void setDataSource(RMDataSource aDataSource)  { setDataSource(aDataSource, -1, -1); }

/**
 * Sets the datasource for the panel.
 */
public void setDataSource(RMDataSource aDataSource, double aX, double aY)
{
    // Set DataSource in editor, show DataSource inspector, KeysBrowser and refocus window
    getEditor().setDataSource(aDataSource, aX, aY);
    if(getWindow().isVisible())
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
    RMDataSource dsource = new RMDataSource(aURL);
    try { dsource.getDataset(); }
    
    // If failed, get error message and run error panel
    catch(Throwable t) {
        while(t.getCause()!=null) t = t.getCause(); // Get root cause
        String e1 = StringUtils.wrap(t.toString(), 40);
        Object line = RMKey.getValue(t, "LineNumber"), column = RMKey.getValue(t, "ColumnNumber");
        if(line!=null || column!=null) e1 += "\nLine: " + line + ", Column: " + column;
        else t.printStackTrace();
        String error = e1;
        runLater(() -> {
            DialogBox dbox = new DialogBox("Error Parsing XML"); dbox.setErrorMessage(error);
            dbox.showMessageDialog(getUI()); });
        return;
    }        
        
    // Set DataSource in editor, show DataSource inspector, KeysBrowser and refocus window
    setDataSource(dsource, aX, aY);
}

/**
 * Returns whether editor is really doing editing.
 */
public boolean isEditing()  { return getEditor().isEditing(); }

/**
 * Sets whether editor is really doing editing.
 */
public void setEditing(boolean aFlag)
{
    // If editor already has requested editing state, just return
    if(aFlag == isEditing()) return;
    
    // Hide attributes drawer
    hideAttributesDrawer();
    
    // If not yet previewing, store current template then generate report and swap it in
    if(!aFlag) {
                
        // Cache current editor and flush any current editing
        _realEditor = getEditor();
        _realEditor.flushEditingChanges();
        
        // Generate report and restore filename
        RMDocument report = getDoc().generateReport(getEditor().getDataSourceDataset());
        
        // Create new editor, set editing to false and set report document
        RMEditor editor = new RMEditor();
        editor.setEditing(false);
        editor.setDoc(report);
        
        // If generateReport hit any keyChain parsing errors, run message dialog
        if(RMKeyChain.getError()!=null) { String err = RMKeyChain.getAndResetError();
            DialogBox dbox = new DialogBox("Error Parsing KeyChain"); dbox.setErrorMessage(err);
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
    BorderView bview = (BorderView)super.createUI();
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
    WindowView win = getWindow();
    win.setImage(getFrameIcon());
    enableEvents(win, WinClose);
}

/**
 * Updates the editor's UI panels.
 */
protected void resetUI()
{
    // Do normal update
    super.resetUI();
    
    // If title has changed, update window title
    if(isWindowVisible()) {
        String title = getWindowTitle();
        WindowView win = getWindow();
        if(!SnapUtils.equals(title, win.getTitle())) {
            win.setTitle(title);
            win.setDocURL(getSourceURL());
        }
    }
    
    // Reset MenuBar, InspectorPanel and AttributesPanel
    if(!ViewUtils.isMouseDown()) getMenuBar().resetLater();
    if(getInspectorPanel().isResetWithEditor()) getInspectorPanel().resetLater();
    if(getAttributesPanel().isVisible() && !ViewUtils.isMouseDown()) getAttributesPanel().resetLater();
}

/**
 * Handles changes to the editor's UI controls.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Forward on to menu bar
    getMenuBar().fireEvent(anEvent);
    
    // Do normal version
    super.respondUI(anEvent);
    
    // Handle PopupTrigger
    if(anEvent.isPopupTrigger() && !anEvent.isConsumed())
        runPopupMenu(anEvent);
    
    // If Editor.MouseClick and DataSource is set and we're editing and DataSource icon clicked, show DS Inspector
    else if(anEvent.isMouseClick() && getDataSource()!=null && isEditing()) {
        Rect r = getEditor().getVisRect(); // Get visible rect
        if(anEvent.getX()>r.getMaxX()-53 && anEvent.getY()>r.getMaxY()-53) { // If DataSource icon clicked
            if(anEvent.isShortcutDown()) setDataSource(null); // If cmd key down, clear the DataSource
            else getInspectorPanel().setVisible(7); // otherwise show DataSource inspector
        }
    
        // If mouse isn't in lower right corner and DataSource inspector is showing, show shape specific inspector
        else if(getInspectorPanel().isShowingDataSource())
            getInspectorPanel().setVisible(0);
    }
    
    // Handle WinClosing
    else if(anEvent.isWinClose()) {
        close(); anEvent.consume(); }
    //else if(anEvent.isWinResized()) { //Dimension wsize=getWindow().getSize(), psize=getWindow().getPreferredSize();
        //if(Math.abs(wsize.width-psize.width)<=10) wsize.width = psize.width;
        //if(Math.abs(wsize.height-psize.height)<=10) wsize.height = psize.height;
        //if(getWindow().getWidth()!=wsize.width || getWindow().getHeight()!=wsize.height) getWindow().setSize(wsize);
}

/**
 * Returns the inspector panel (shared).
 */
public InspectorPanel getInspectorPanel()  { return _inspPanel; }

/**
 * Creates the InspectorPanel.
 */
protected InspectorPanel createInspectorPanel()  { return new InspectorPanel(this); }

/**
 * Returns the attributes panel (shared).
 */
public AttributesPanel getAttributesPanel()  { return _attrsPanel; }

/**
 * Creates the AttributesPanel.
 */
protected AttributesPanel createAttributesPanel()  { return new AttributesPanel(this); }

/**
 * Shows the AttributesPanel Drawer.
 */
public void showAttributesDrawer()  { getAttributesPanel().showDrawer(); }

/**
 * Hides the AttributesPanel Drawer.
 */
public void hideAttributesDrawer()  { getAttributesPanel().hideDrawer(); }

/**
 * Returns extension for editor document.
 */
public String[] getFileExtensions()  { return new String[] { ".rpt", ".pdf"}; }

/**
 * Returns the description for the editor document for use in open/save panels.
 */
public String getFileDescription()  { return "ReportMill files (.rpt, .pdf)"; }

/**
 * Returns the window title.
 */
public String getWindowTitle()
{
    // Get window title: Basic filename + optional "Doc edited asterisk + optional "Doc Scaled"
    String title = getSourceURL()!=null? getSourceURL().getPath() : null; if(title==null) title = "Untitled";

    // If has undos, add asterisk. If zoomed, add ZoomFactor
    if(getEditor().getUndoer()!=null && getEditor().getUndoer().hasUndos()) title = "* " + title;
    if(!MathUtils.equals(getEditor().getZoomFactor(), 1f))
        title += " @ " + Math.round(getEditor().getZoomFactor()*100) + "%";

    // If previewing, add "(Previewing)" and return
    if(getEditor().isPreview()) title += " (Previewing)";
    return title;
}

/**
 * Creates a new default editor pane.
 */
public RMEditorPane newDocument()  { return open(new RMDocument(612, 792)); }

/**
 * Creates a new editor window from an open panel.
 */
public RMEditorPane open(View aView)
{
    // Get path from open panel for supported file extensions
    String path = FilePanel.showOpenPanel(aView, getFileDescription(), getFileExtensions());
    return open(path);
}

/**
 * Creates a new editor window by opening the document from the given source.
 */
public RMEditorPane open(Object aSource)
{
    // If document source is null, just return null
    if(aSource==null) return null;
    
    // Get Source URL
    WebURL url = WebURL.getURL(aSource);
    
    // If source is already opened, return editor pane
    if(!SnapUtils.equals(url, getSourceURL())) {
        RMEditorPane epanes[] = WindowView.getOpenWindowOwners(RMEditorPane.class);
        for(RMEditorPane epane : epanes)
            if(SnapUtils.equals(url, epane.getSourceURL()))
                return epane;
    }
    
    // Load document
    RMDocument doc = null; try { doc = RMDocument.getDoc(aSource); }
    
    // If there was an XML parse error loading aSource, show error dialog
    catch(Exception e) {
        e.printStackTrace();
        String msg = StringUtils.wrap("Error reading file:\n" + e.getMessage(), 40);
        runLater(() -> {
            DialogBox dbox = new DialogBox("Error Reading File"); dbox.setErrorMessage(msg);
            dbox.showMessageDialog(getUI()); });
    }
    
    // If no document, just return null
    if(doc==null) return null;

    // If old version, warn user that saving document will make it unreadable by RM7
    if(doc.getVersion()<7.0) {
        String msg = "This document has been upgraded from an older version.\n" +
            "If saved, it will not open in earlier versions.";
        DialogBox dbox = new DialogBox("Warning: Document Upgrade"); dbox.setWarningMessage(msg);
        dbox.showMessageDialog(getUI());
    }
    
    // Set document
    getViewer().setDoc(doc);
    
    // If source is string, add to recent files menu
    if(url!=null) RecentFiles.addPath("RecentDocuments", url.getPath(), 10);
    
    // Return the editor
    return this;
}

/**
 * Saves the current editor document, running the save panel.
 */
public void saveAs()
{
    // Make sure editor isn't previewing
    setEditing(true);
    
    // Get extensions - if there is an existing extension, make sure it's first in the exts array
    String exts[] = getFileExtensions();
    if(getSourceURL()!=null && FilePathUtils.getExtension(getSourceURL().getPath())!=null) {
        List ex = new ArrayList(Arrays.asList(exts));
        ex.add(0, FilePathUtils.getExtension(getSourceURL().getPath()));
        exts = (String[])ex.toArray(new String[ex.size()]);
    }
    
    // Run save panel, set Document.Source to path and re-save (or just return if cancelled)
    String path = FilePanel.showSavePanel(getUI(), getFileDescription(), exts); if(path==null) return;
    getDoc().setSourceURL(WebURL.getURL(path));
    save();
}

/**
 * Saves the current editor document, running the save panel if needed.
 */
public void save()
{
    // If can't save to current source, do SaveAs instead
    WebURL url = getSourceURL(); if(url==null) { saveAs(); return; }
    
    // Make sure editor isn't previewing and has focus (to commit any inspector textfield changes)
    setEditing(true);
    getEditor().requestFocus();
    
    // Do actual save - if exception, print stack trace and set error string
    try { saveImpl(); }
    catch(Throwable e) {
        e.printStackTrace();
        String msg = "The file " + url.getPath() + " could not be saved (" + e + ").";
        DialogBox dbox = new DialogBox("Error on Save"); dbox.setErrorMessage(msg);
        dbox.showMessageDialog(getUI());
        return;
    }
    
    // Add URL.String to RecentFilesMenu, clear undoer and reset UI
    RecentFiles.addPath("RecentDocuments", url.getPath(), 10);
    getDoc().getUndoer().reset();
    resetLater();
}

/**
 * The real save method.
 */
protected void saveImpl() throws Exception
{
    WebURL url = getSourceURL();
    WebFile file = url.getFile();
    if(file==null) file = url.createFile(false);
    file.setBytes(getDoc().getBytes());
    file.save();
}

/**
 * Reloads the current editor document from the last saved version.
 */
public void revert()
{
    // Get filename (just return if null)
    WebURL surl = getSourceURL(); if(surl==null) return;

    // Run option panel for revert confirmation (just return if denied)
    String msg = "Revert to saved version of " + surl.getPathName() + "?";
    DialogBox dbox = new DialogBox("Revert to Saved"); dbox.setQuestionMessage(msg);
    if(!dbox.showConfirmDialog(getUI())) return;
        
    // Re-open filename
    getSourceURL().getFile().reload();
    open(getSourceURL());
}

/**
 * Closes this editor pane
 */
public boolean close()
{
    // Make sure editor isn't previewing
    setEditing(true);
    
    // If unsaved changes, run panel to request save
    if(getEditor().undoerHasUndos()) {
        String fname = getSourceURL()==null? "untitled document" : getSourceURL().getPathName();
        String msg = "Save changes to " + fname + "?", options[] = { "Save", "Don't Save", "Cancel" };
        DialogBox dbox = new DialogBox("Unsaved Changes"); dbox.setWarningMessage(msg); dbox.setOptions(options);
        switch(dbox.showOptionDialog(getUI(), "Save")) {
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
    RMEditorPane epane = WindowView.getOpenWindowOwner(RMEditorPane.class);
    if(epane!=null)
        epane.getEditor().requestFocus();
    else if(Welcome.getShared().isEnabled())
        Welcome.getShared().runWelcome();
}

/**
 * Called when the app is about to exit to gracefully handle any open documents.
 */
public void quit()  { App.quitApp(); }

/**
 * Returns a popup menu for the editor.
 */
public void runPopupMenu(ViewEvent anEvent)
{
    // Get selected shape (just return if page is selected)
    Menu pmenu = new Menu();
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    if(shape instanceof RMPage) return;

    // If RMTextShape, get copy of Format menu
    if(shape instanceof RMTextShape) { RMTextShape text = (RMTextShape)shape;

        // Get editor pane format menu and add menu items to popup
        Menu formatMenu = getMenuBar().getView("FormatMenu", Menu.class);
        Menu formatMenuCopy = (Menu)formatMenu.clone();
        for(MenuItem m : formatMenuCopy.getItems()) pmenu.addItem(m);

        // If structured tablerow, add AddColumnMenuItem and SplitColumnMenuItem
        if(text.isStructured()) { MenuItem mi;
            mi = new MenuItem(); mi.setText("Add Column"); mi.setName("AddColumnMenuItem"); pmenu.addItem(mi);
            mi = new MenuItem(); mi.setText("Split Column"); mi.setName("SplitColumnMenuItem"); pmenu.addItem(mi);
        }
    }
    
    // Get copy of shapes menu and add menu items to popup
    Menu shapesMenu = getMenuBar().getView("ShapesMenu", Menu.class);
    Menu shapesMenuCopy = (Menu)shapesMenu.clone();
    for(MenuItem m : shapesMenuCopy.getItems()) pmenu.addItem(m);
    
    // Initialize popup menu items to send Events to menu bar
    pmenu.setOwner(getMenuBar());
    pmenu.show(getEditor(), anEvent.getX(), anEvent.getY());
    anEvent.consume();
}

/**
 * Called when Editor has prop change.
 */
private void editorDidPropChange(PropChange aPC)
{
    String pname = aPC.getPropName();
    switch(pname) {
        case RMEditor.SelShapes_Prop: resetLater(); break;
        case RMEditor.SuperSelShape_Prop: resetLater(); break;
    }
}

/**
 * Returns the icon for the editor window frame.
 */
private static Image getFrameIcon()
{
    return _frameImg!=null? _frameImg : (_frameImg=Image.get(RMEditorPane.class, "ReportMill16x16.png"));
}

/**
 * A class for any editor pane support panes.
 */
public static class SupportPane extends ViewOwner {
    
    // The editor pane
    RMEditorPane         _editorPane;
    
    /** Creates a new SupportPane with given editor pane. */
    public SupportPane(RMEditorPane anEP)  { _editorPane = anEP; }
    
    /** Returns the EditorPane. */
    public RMEditorPane getEditorPane()  { return _editorPane; }
    
    /** Sets the EditorPane. */
    public void setEditorPane(RMEditorPane anEP)  { _editorPane = anEP; }
    
    /** Returns the editor. */
    public RMEditor getEditor()  { return _editorPane.getEditor(); }
    
    /** Returns the title. */
    public String getWindowTitle()  { return "Inspector"; }
}

}