/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import java.io.File;
import java.util.*;
import java.util.prefs.*;
import snap.util.*;
import snap.view.*;

/**
 * A custom class.
 */
public class RecentFilesPanel extends ViewOwner {
    
    // The DialogBox
    DialogBox     _dbox;

/**
 * Shows the RecentFilesPanel.
 */
public boolean showPanel()
{
    // Create DialogBox with UI, and showConfirmDialog (just return if cancelled)
    _dbox = new DialogBox("Recent Files"); _dbox.setContent(getUI()); _dbox.setOptions("Open", "Cancel");
    if(!_dbox.showConfirmDialog(null)) return false;
    
    // If not cancelled, open selected item
    File file = (File)getViewSelectedItem("FilesList"); if(file==null) return false;
    com.reportmill.app.Welcome.getShared().open(file.getAbsolutePath());
    return true;
}

/**
 * Create UI.
 */
protected View createUI()
{
    ListView lview = new ListView(); lview.setName("FilesList"); enableEvents(lview, MouseRelease);
    ScrollView spane = new ScrollView(); spane.setContent(lview); spane.setPrefSize(250,300);
    return spane;
}

/**
 * Reset UI.
 */
protected void resetUI()
{
    setViewItems("FilesList", getRecentFiles());
    getView("FilesList", ListView.class).setItemKey("Name");
    if(getViewSelectedIndex("FilesList")<0) setViewSelectedIndex("FilesList", 0);
}

/**
 * Respond to any selection from the RecentFiles menu
 */
public void respondUI(ViewEvent anEvent) 
{
    // Handle ClearRecentMenuItem
    if(anEvent.equals("ClearRecentMenuItem"))
        clearRecentFiles();
    
    // Handle RecentFileMenuItem
    /*else if(anEvent.equals("RecentFileMenuItem")) {
        
        // Get index and path
        int index = findMenuItem(_menu, anEvent.getTarget(JMenuItem.class)); if(index<0) return;
        String path = _paths.get(index);
            
        // If EditorMenuBar is present, have the editor pane do it
        if(_editorMenuBar!=null) {
                
            // Create new editor pane. If successful, rebuild the menu and open editor
            RMEditorPane epane = ClassUtils.newInstance(_editorMenuBar.getEditorPane()).open(path);
            if(epane!=null) { rebuildMenu(); epane.setWindowVisible(true); }
        }
        
        // Otherwise, have the Welcome panel open the doc
        else com.reportmill.app.Welcome.getShared().open(path);
    }*/
    
    // Handle FilesList MouseClick
    if(anEvent.equals("FilesList") && anEvent.getClickCount()>1)
        if(_dbox!=null) _dbox.confirm();
}

/**
 * Returns the list of the recent documents as a list of strings.
 */
public static List <String> getRecentPaths()
{
    List <File> files = getRecentFiles();
    List <String> paths = new ArrayList(); for(File file : files) paths.add(file.getAbsolutePath());
    return paths;
}

/**
 * Returns the list of the recent documents as a list of strings.
 */
public static List <File> getRecentFiles()
{
    // Get prefs for RecentDocuments (just return if missing)
    Preferences prefs = PrefsUtils.prefs();
    try { if(!prefs.nodeExists("RecentDocuments")) return new ArrayList(); }
    catch(BackingStoreException bse) { return new ArrayList(); }
    prefs = prefs.node("RecentDocuments");
    
    // Add to the list only if the file is around and readable
    List list = new ArrayList();
    for(int i=0; ; i++) {
        String fname = prefs.get("index"+i, null); if(fname==null) break;
        File file = new File(fname);
        if(file.exists() && file.canRead())
            list.add(file);
    }
    
    // Return list
    return list;
}

/**
 * Adds a new file to the list and updates the users preferences.
 */
public static void addRecentFile(String aPath)
{
    // Get the doc list from the preferences
    String path = aPath; if(StringUtils.startsWithIC(path, "file:")) path = path.substring(5);
    List <String> docs = getRecentPaths();
    
    // Remove the path (if it was there) and add to front of list
    docs.remove(path); docs.add(0, path);
    
    // Add at most 10 files to the preferences list
    Preferences prefs = PrefsUtils.prefs().node("RecentDocuments");
    for(int i=0; i<docs.size() && i<10; i++) 
        prefs.put("index"+i, docs.get(i));
}

/**
 * Clears recent documents from preferences.
 */
public void clearRecentFiles()
{
    Preferences p = PrefsUtils.prefs();
    try { if(p.nodeExists("RecentDocuments")) p.node("RecentDocuments").removeNode(); }
    catch(BackingStoreException e) { }
}

/**
 * Returns a menu for recent files.
 */
public Menu getMenu()
{
    Menu menu = new Menu();
    List <File> files = getRecentFiles();
    for(File file : files) {
        MenuItem mi = new MenuItem(); mi.setName("RecentFilesMenuItem"); mi.setText(file.getName());
        menu.addItem(mi);
    }
    menu.setOwner(this);
    return menu;
}

}