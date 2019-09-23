/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.base.Entity;
import com.reportmill.shape.*;
import snap.util.StringUtils;
import snap.view.*;

/**
 * This class handles UI editing of table groups.
 */
public class RMTableGroupTool <T extends RMTableGroup> extends RMParentShapeTool <T> {

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get TableTree and configure
    TreeView tablesTree = getView("TablesTree", TreeView.class);
    enableEvents(tablesTree, DragEvents); enableEvents(tablesTree, MouseRelease);
    //renderer.setClosedIcon(null); renderer.setOpenIcon(null); renderer.setLeafIcon(null);
    enableEvents("DatasetKeyText", DragDrop); enableEvents("MainTableNameText", DragDrop);
}

/**
 * Updates UI panel.
 */
public void resetUI()
{
    // Get currently selected table group (just return if null)
    RMTableGroup tableGroup = getSelectedShape(); if(tableGroup==null) return;
    RMTable mainTable = tableGroup.getMainTable();
    
    // Create root node for table group and add child tables to it
    TreeView tablesTree = getView("TablesTree", TreeView.class);
    tablesTree.setResolver(new TGTreeResolver(tableGroup));
    tablesTree.setItems(tableGroup.getChildTables());
    tablesTree.expandAll();
    tablesTree.setSelItem(getMainTable());
    
    // Update DatasetKeyText, StartOnNewPageCheckBox, MainTableNameText
    setViewValue("DatasetKeyText", mainTable.getDatasetKey());
    setViewValue("StartOnNewPageCheckBox", mainTable.getStartingPageBreak());
    setViewValue("MainTableNameText", mainTable.getName());
}

/**
 * Respond to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected table group and main table (just return if null)
    RMTableGroup tableGroup = getSelectedShape(); if(tableGroup==null) return;
    RMTable mainTable = tableGroup.getMainTable();
    
    // Handle DatasetKeyText, StartOnNewPageCheckBox, MainTableNameText
    if(anEvent.equals("DatasetKeyText")) { String value = anEvent.getStringValue().replace("@", "");
        mainTable.setDatasetKey(value); }
    if(anEvent.equals("StartOnNewPageCheckBox"))
        mainTable.setStartingPageBreak(anEvent.getBoolValue());
    if(anEvent.equals("MainTableNameText"))
        mainTable.setName(anEvent.getStringValue());
    
    // Handle MainTableNameText
    if(anEvent.equals("MainTableNameText") && anEvent.isDragDrop())
        mainTable.setName(anEvent.getStringValue().replace("@", ""));

    // Handle AddPeerMenuItem
    if(anEvent.equals("AddPeerMenuItem")) {

        // If asked to add peer to table that is already a level or more down, call addChildTableToTable on it's parent
        if(tableGroup.getParentTable(mainTable) != null) {
            addChildTable(tableGroup.getParentTable(mainTable), mainTable, null); return; }
        
        addPeerTable(mainTable);
    }
    
    // Handle AddChildMenuItem
    if(anEvent.equals("AddChildMenuItem"))
    	addChildTable(mainTable, null, null);
    
    // Handle RemoveTableMenuItem (short-circuit and beep if main table is only top level table)
    if(anEvent.equals("RemoveTableMenuItem")) {
        if(tableGroup.getParentTable(mainTable)==null && tableGroup.getPeerTables(mainTable).size()==1) beep();
        else tableGroup.removeTable(mainTable);
    }
    
    // Handle PasteTableMenuItem
    if(anEvent.equals("PasteTableMenuItem")) {
        Object pasteShape = RMEditorClipboard.getShapeFromClipboard(getEditor());
        if(pasteShape instanceof RMTable) {
            tableGroup.undoerSetUndoTitle("Paste Table");
            tableGroup.addPeerTable((RMTable)pasteShape);
        }
    }
    
    // Handle MoveUpMenuItem
    if(anEvent.equals("MoveUpMenuItem"))
        tableGroup.moveTable(mainTable, -1);
    
    // Handle MoveDownMenuItem
    if(anEvent.equals("MoveDownMenuItem"))
        tableGroup.moveTable(mainTable, 1);
        
    // Handle MoveInMenuItem
    if(anEvent.equals("MoveInMenuItem")) {
        RMTable tableBefore = tableGroup.getPeerTablePrevious(mainTable); if(tableBefore==null) return;
        tableGroup.makeTableChildOfTable(mainTable, tableBefore);
    }
    
    // Handle MoveOutMenuItem
    if(anEvent.equals("MoveOutMenuItem")) {
        RMTable parentTable = tableGroup.getParentTable(mainTable);
        RMTable parentTableParentTable = tableGroup.getParentTable(parentTable);
        tableGroup.makeTableChildOfTable(mainTable, parentTableParentTable);
    }
    
    // Handle KeysButton
    if(anEvent.equals("KeysButton"))
        getEditorPane().getAttributesPanel().setVisibleName(AttributesPanel.KEYS);
    
    // Handle TableTree
    if(anEvent.equals("TablesTree")) {
        
        // Handle MouseRelease
        if(anEvent.isMouseRelease()) {
            if(anEvent.getClickCount()==2) {
                RMTable table = (RMTable)anEvent.getSelItem();
                getEditor().setSuperSelectedShape(table);
            }
        }
        
        // Handle TableTree DragEvent
        else if(anEvent.isDragEvent()) {
            anEvent.acceptDrag(); // DnDConstants.ACTION_COPY_OR_MOVE
            if(anEvent.isDragDrop()) {
                
                // If drag is String, accept text
                if(anEvent.getClipboard().hasString()) {
                    String dropString = anEvent.getClipboard().getString();
                    dropString = StringUtils.delete(dropString, "@"); // Bogus - delete @ signs
                    addChildTable(getMainTable(), null, dropString);
                    anEvent.dropComplete();  // Register drop complete
                }
                // If not StringFlavor, reject drop //else dtde.rejectDrop();
            }
        }
        
        // Handle TableTree selection event: Get selected table node and set table for node
        else {
            RMTable table = (RMTable)anEvent.getSelItem();
            tableGroup.setMainTable(table);
        }
    }
}

/**
 * Adds a new child table to given table after the other given child table with given dataset key.
 */
private void addChildTable(RMTable toTable, RMTable afterTable, String aKey)
{
    RMTableGroup tgroup = getSelectedShape();

    // Get new table, set default size and add grouping key for root entity (with structured tablerow)
    RMTable table = new RMTable(aKey==null? "Objects" : aKey);
    table.setSize(tgroup.getWidth(), tgroup.getHeight());
    
    // Add table (recording it for Undo)
    tgroup.undoerSetUndoTitle("Add Table to Table Group");
    tgroup.addChildTable(table, toTable, afterTable);
}

/**
 * Adds a new peer table after given table.
 */
private void addPeerTable(RMTable afterThisTable)
{
    RMTableGroup tgroup = getSelectedShape();

    // Get table and set defalt size and add grouping key for root entity (with structured tablerow)
    RMTable table = new RMTable("Objects");
    table.setSize(tgroup.getWidth(), tgroup.getHeight());

    // Add table (recording it for Undo)
    tgroup.undoerSetUndoTitle("Add Table to Table Group");
    tgroup.addPeerTable(table, afterThisTable);
}

/**
 * Returns the shape class for this tool (table group).
 */
public Class getShapeClass()  { return RMTableGroup.class; }

/**
 * Returns the display name for this inspector.
 */
public String getWindowTitle()  { return "Table Group Inspector"; }

/**
 * Overridden to make graph super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make graph not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Returns the given shape's dataset entity.
 */
public Entity getDatasetEntity(RMShape aShape)
{
    // Get the table group's main table
    RMTableGroup tableGroup = (RMTableGroup)aShape;
    RMTable table = tableGroup.getMainTable();
    if(table==null)
        return tableGroup.getDatasetEntity();
    
    // Return the main table's dataset entity
    return table.getDatasetEntity();
}

/**
 * Returns the main table for the current table group.
 */
public RMTable getMainTable()
{
    RMTableGroup tgroup = getSelectedShape();
    return tgroup!=null? tgroup.getMainTable() : null;
}

/**
 * MousePressed.
 */
public void mousePressed(T aTableGroup, ViewEvent anEvent)
{
    // If selected, forward on to main table, to potentially super select structured table row
    RMEditor editor = getEditor();
    if(editor.getSelectedOrSuperSelectedShape()==aTableGroup) {
        RMTable mainTable = aTableGroup.getMainTable();                // Get main table
        getTool(mainTable).processEvent(mainTable, anEvent);    // Forward on
    }
}

/** An inner class to provide tree item info. */
private static class TGTreeResolver extends TreeResolver <RMTable> {
    
    /** Create a new TreeItem for parent and item. */
    public TGTreeResolver(RMTableGroup aTG) { _tgroup = aTG; }  RMTableGroup _tgroup;
    
    /** Whether given object is a parent (has children). */
    public RMTable getParent(RMTable aTable)  { return _tgroup.getParentTable(aTable); }

    /** Whether given object is a parent (has children). */
    public boolean isParent(RMTable aTable)  { return _tgroup.getChildTableCount(aTable)>0; }

    /** Over ride to return child tables as TreeItem array. */
    public RMTable[] getChildren(RMTable aTable)  { return _tgroup.getChildTables(aTable).toArray(new RMTable[0]); }
    
    // The name of given item
    public String getText(RMTable aTable)
    {
        if(aTable==null) return "TableGroup";
        String tname = aTable.getName(), tkey = aTable.getDatasetKey();
        return StringUtils.length(tname)>0? tname : StringUtils.length(tkey)>0? tkey : "Table";
    }
}

}