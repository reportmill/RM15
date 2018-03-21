/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import java.util.*;
import snap.util.*;
import snap.view.*;

/**
 * UI editing for undos.
 */
public class UndoInspector extends RMEditorPane.SupportPane {
    
    // A list of sources we've seen, so we can generate small ids
    List         _sources = new ArrayList();

/**
 * Creates a new UndoInspector.
 */
public UndoInspector(RMEditorPane anEP)  { super(anEP); }

/**
 * Updates the UI controls from the current undoer.
 */
public void resetUI()
{
    // Get undoer undos and redos
    Undoer undoer = getEditor().getUndoer();
    List <UndoSet> undos = undoer.getUndoSets();
    List <UndoSet> redos = undoer.getRedoSets();
    
    // Get titles (reversed)
    String titles[] = new String[undos.size()];
    for(int i=0, iMax=undos.size(); i<iMax; i++) titles[i] = undos.get(iMax-1-i).getFullUndoTitle();
    
    // Reload data, preserving selection
    int index = getViewSelIndex("UndosList");
    setViewItems("UndosList", titles);
    if(index<undos.size()) setViewSelIndex("UndosList", index);

    // Replace with titles
    titles = new String[redos.size()];
    for(int i=0, iMax=redos.size(); i<iMax; i++) titles[i] = redos.get(iMax-1-i).getFullRedoTitle();
    
    // Reload data, preserving selection
    index = getViewSelIndex("RedosList");
    setViewItems("RedosList", titles);
    if(index<redos.size()) setViewSelIndex("RedosList", index);
}

/**
 * Responds to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle UndoList
    if(anEvent.equals("UndosList")) {
        
        // Get selected index (just return if null)
        
        int index = getViewItems("UndosList").size() - 1 - getViewSelIndex("UndosList"); if(index<0) return;
        
        // Get undoer and undo event
        Undoer undoer = getEditor().getUndoer();
        UndoSet undoEvent = undoer.getUndoSets().get(index);
        
        // Add to changes jlist
        setViewItems("ChangesList", undoEvent.getChanges().toArray());

        // Clear redo selection
        setViewSelIndex("RedosList", -1);
        setViewValue("ChangeText", "");
    }

    // Handle RedoList
    if(anEvent.equals("RedosList")) {

        // Get selected index (just return if null)
        int index = getViewItems("RedosList").size() - 1 - getViewSelIndex("UndosList"); if(index<0) return;
        
        // Get all redos
        List <UndoSet> redos = getEditor().getUndoer().getRedoSets();
        
        // Get selected redo
        UndoSet undoEvent = redos.get(index);
        
        // Add to object jlist
        setViewItems("ChangesList", undoEvent.getChanges().toArray());
        
        // Clear undo selection
        setViewSelIndex("UndosList", -1);
        setViewValue("ChangeText", "");
    }
    
    // Handle ChangeList
    if(anEvent.equals("ChangesList")) {
        PropChange e = (PropChange)getViewSelItem("ChangesList");
        int index = e.getIndex();
        setViewValue("ChangeText", e.getSource().getClass().getSimpleName() + "(" + getId(e.getSource()) + ") " +
            e.getPropertyName() + " " + e.getOldValue() + " " + e.getNewValue() + " " +
            (index>=0? index : "") + "\n\n" + e.getSource());
    }
}

/**
 * Returns a unique id for given source.
 */
public int getId(Object anObj)
{
    int id = ListUtils.indexOfId(_sources, anObj);
    if(id<0) { id = _sources.size(); _sources.add(anObj); }
    return id;
}

/**
 * Returns the string used in the inspector window title.
 */
public String getWindowTitle()  { return "Undo Inspector"; }

}