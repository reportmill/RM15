/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.base.RMGrouper;
import com.reportmill.base.RMGrouping;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * This class provides UI editing for Tables.
 */
public class RMTableTool <T extends RMTable> extends RMParentShapeTool <T> implements RMSortPanel.Owner {
    
    // The grouping table
    TableView <RMGrouping>  _groupingTable;
    
    // The sort panel
    RMSortPanel             _sortPanel;
    
    // Used for splitshape editing in shape editing mouse loop
    Point                   _lastMousePoint;

    // Used for splitshape editing in shape editing mouse loop
    int                     _resizeBarIndex;

    // Constants for images used by inspector
    Image PageBreakIcon   = getImage("GroupPagebreak.png");
    Image NoPageBreakIcon = getImage("GroupNobreak.png");

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get grouping and configure
    _groupingTable = getView("GroupingTable", TableView.class);
    _groupingTable.setCellConfigure(this :: configureGroupingTable);
    enableEvents(_groupingTable, MouseRelease, DragDrop); // So we get called for click on PageBreakIcon
    
    // Get SortPanel, configure and add
    _sortPanel = new RMSortPanel(this);
    _sortPanel.getUI().setBounds(4, 170, 267, 100);
    getUI(ChildView.class).addChild(_sortPanel.getUI());
    
    // Enable text drop string
    enableEvents("ListKeyText", DragDrop); enableEvents("FilterKeyText", DragDrop);
}

/**
 * Updates UI panel from currently  table
 */
public void resetUI()
{
    // Get currently selected table, grouper and grouping (just return if null)
    RMTable table = getTable(); if(table==null) return;
    RMGrouper grouper = table.getGrouper();
    RMGrouping grouping = getGrouping();
    
    // Update ListKeyText, FilterKeyText, NumColumnsText, ColumnSpacingText
    setViewValue("ListKeyText", table.getDatasetKey());
    setViewValue("FilterKeyText", table.getFilterKey());
    setViewValue("NumColumnsText", table.getColumnCount());
    setViewValue("ColumnSpacingText", getUnitsFromPoints(table.getColumnSpacing()));
    
    // Update HeaderCheckBox, DetailsCheckBox, SummaryCheckBox
    setViewValue("HeaderCheckBox", grouping.getHasHeader());
    setViewValue("DetailsCheckBox", grouping.getHasDetails());
    setViewValue("SummaryCheckBox", grouping.getHasSummary());
    
    // Update GroupingTable
    _groupingTable.setItems(grouper.getGroupings());
    _groupingTable.setSelIndex(grouper.getSelectedGroupingIndex());
    
    // Update TableGroupButton text
    String buttonText = table.getParent() instanceof RMTableGroup? "Ungroup TableGroup" : "Make TableGroup";
    setViewText("TableGroupButton", buttonText);
    
    // Update SortPanel
    _sortPanel.resetUI();
}

/**
 * Updates currently selected table from UI panel.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected table, grouper and grouping (just return if null)
    RMTable table = getTable(); if(table==null) return;
    RMGrouper grouper = table.getGrouper();
    RMGrouping grouping = getGrouping();
    
    // Handle ListKeyText
    if(anEvent.equals("ListKeyText")) {
         table.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));
         if(anEvent.isDragDrop())
             anEvent.dropComplete();
    }
    
    // Handle FilterKeyText
    if(anEvent.equals("FilterKeyText")) {
        table.setFilterKey(StringUtils.delete(anEvent.getStringValue(), "@"));
         if(anEvent.isDragDrop())
             anEvent.dropComplete();
    }

    // Handle AddGroupMenuItem
    if(anEvent.equals("AddGroupMenuItem")) {
        
        // Run input dialog to get new group key
        DialogBox dbox = new DialogBox("Add Grouping Key"); dbox.setQuestionMessage("Enter Grouping Key:");
        String key = dbox.showInputDialog(getUI(), null);
        
        // If key was returned, add grouping
        if(key!=null)
            addGroupingKey(key);
    }
    
    // Handle RemoveGroupMenuItem
    if(anEvent.equals("RemoveGroupMenuItem")) {
        
        // If grouping isn't last grouping, remove grouping
        if(grouping!=grouper.getGroupingLast())
            table.removeGrouping(grouping);
        
        // Otherwise beep
        else beep();
    }
    
    // Handle GroupingTable
    if(anEvent.equals("GroupingTable")) {
        
        // Handle DropEvent: Get drop string and add grouping
        if(anEvent.isDragDrop()) {  //int toRow = _groupingTable.rowAtPoint(anEvent.getLocation());
            String string = anEvent.getClipboard().getString().replace("@", "");
            addGroupingKey(string);
            anEvent.dropComplete();
        }
        
        // Handle SelectionEvent and MouseClick
        else {
            
            // Update grouper SelectedGroupingIndex
            int row = _groupingTable.getSelRow();
            int col = _groupingTable.getSelCol();
            grouper.setSelectedGroupingIndex(row);
            
            // If MouseClick, set or reset PageBreakGroupIndex
            if(anEvent.isMouseClick() && col==1)
                table.setPageBreakGroupIndex(row==table.getPageBreakGroupIndex()? -1 : row);
            _groupingTable.updateItems();
        }
    }
    
    // Handle MoveGroupUpMenuItem
    if(anEvent.equals("MoveGroupUpMenuItem")) {
        int loc = _groupingTable.getSelRow();
        if(loc>0)
            table.moveGrouping(loc, loc - 1);
    }
    
    // Handle MoveGroupDownMenuItem
    if(anEvent.equals("MoveGroupDownMenuItem")) {
        int loc = _groupingTable.getSelRow();
        if(loc<_groupingTable.getRowCount() - 1)
            table.moveGrouping(loc, loc + 1);
    }
    
    // Handle HeaderCheckBox, DetailsCheckBox, SummaryCheckBox
    if(anEvent.equals("HeaderCheckBox")) grouping.setHasHeader(anEvent.getBoolValue());
    if(anEvent.equals("DetailsCheckBox")) grouping.setHasDetails(anEvent.getBoolValue());
    if(anEvent.equals("SummaryCheckBox")) grouping.setHasSummary(anEvent.getBoolValue());
    
    // Handle NumColumnsText, ColumnSpacingText
    if(anEvent.equals("NumColumnsText")) table.setColumnCount(anEvent.getIntValue());
    if(anEvent.equals("ColumnSpacingText")) table.setColumnSpacing(getPointsFromUnits(anEvent.getFloatValue()));
    
    // Handle TableGroupButton
    if(anEvent.equals("TableGroupButton")) {
        
        // Get table parent
        RMParentShape parent = table.getParent();
        
        // If in TableGroup, get out of it
        if(parent instanceof RMTableGroup) {
            RMTableGroup tableGroup = (RMTableGroup)parent; tableGroup.repaint();
            tableGroup.removeTable(table);
            tableGroup.getParent().addChild(table);
            table.setFrame(tableGroup.getFrame());
            if(tableGroup.getChildTableCount()==0)
                tableGroup.getParent().removeChild(tableGroup);
            getEditor().setSelectedShape(table);
        }
        
        // If not in TableGroup, create one and add
        else {
            
            // Create new table group
            RMTableGroup tableGroup = new RMTableGroup();

            // Configure tableGroup
            tableGroup.copyShape(table);
            tableGroup.addPeerTable(table);

            // Add TableGroup to table's parent and select tableGroup
            tableGroup.undoerSetUndoTitle("Make TableGroup");
            parent.removeChild(table);
            parent.addChild(tableGroup);
            getEditor().setSelectedShape(tableGroup);
        }
    }
}

/**
 * Returns the selected table.
 */
public RMTable getTable()
{
    // Get editor selected or super selected shape - if shape isn't table, go up hierarchy until table is found
    RMShape shape = getEditor()!=null? getEditor().getSelectedOrSuperSelectedShape() : null;
    while(shape!=null && !(shape instanceof RMTable))
        shape = shape.getParent();
    
    // Return shape as table
    return (RMTable)shape;
}

/**
 * Returns the selected grouping for this table.
 */
public RMGrouping getGrouping()
{
    RMTable table = getTable();
    return table!=null? table.getGrouper().getSelectedGrouping() : null;
}

/**
 * Configures GroupingTable.
 */
public void configureGroupingTable(ListCell <RMGrouping> aCell)
{
    // Get table and grouping
    RMTable table = getTable(); if(table==null) return;
    RMGrouping grouping = aCell.getItem(); if(grouping==null) return;
    int row = aCell.getRow(), col = aCell.getCol();
    
    // Handle column 0
    if(col==0) { String key = grouping.getKey();
        aCell.setText(key);
        aCell.setToolTip(key);
    }
    
    // Handle column 1
    else {
        aCell.setImage(table.getPageBreakGroupIndex()==row? PageBreakIcon : NoPageBreakIcon);
        aCell.setText(null);
    }
}

/**
 * Returns the shape class this tool edits (RMTable).
 */
public Class getShapeClass()  { return RMTable.class; }

/**
 * Returns the display name for this tool ("Table Inspector").
 */
public String getWindowTitle()  { return "Table Inspector"; }

/**
 * Overridden to make table super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make table not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Adds a grouping key to the currently selected table.
 */
public void addGroupingKey(String aKey)
{
    getTable().undoerSetUndoTitle("Add Grouping");
    getTable().addGroupingKey(aKey, 0);
}

/**
 * Adds a new table to the given editor with the given dataset key.
 */
public static void addTable(RMEditor anEditor, String aKeyPath)
{
    // Create new default table for key path
    RMTable table = new RMTable(aKeyPath==null? "Objects" : aKeyPath);

    // Set table location in middle of selected shape
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    table.setXY(parent.getWidth()/2 - table.getWidth()/2, parent.getHeight()/2 - table.getHeight()/2);

    // Add table
    anEditor.undoerSetUndoTitle("Add Table");
    parent.addChild(table);

    // Select table, select selectTool and redisplay
    anEditor.setCurrentToolToSelectTool();
    anEditor.setSelectedShape(table);
}

/**
 * MouseMoved implementation to update cursor for resize bars.
 */
public void mouseMoved(T aTable, ViewEvent anEvent)
{
    // Get event point in table coords and resize bar for point
    RMEditor editor = getEditor();
    Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), aTable);
    int resizeBarIndex = aTable.getResizeBarAtPoint(point);

    // If resize bar is under point, set cursor
    if(resizeBarIndex>=0) {
        
        // Get the table row above resize bar
        RMTableRow tableRow = (RMTableRow)aTable.getChild(resizeBarIndex);
        
        // If point is before resize bar controls, set cursor to N resize, otherwise default
        if(point.x<getResizeBarPopupX(tableRow) - 20) editor.setCursor(Cursor.N_RESIZE);
        else editor.setCursor(Cursor.DEFAULT);
        anEvent.consume();  // Consume event
    }
    
    // Otherwise, do normal mouse moved
    else super.mouseMoved(aTable, anEvent);
}

/**
 * Event handling for table editing.
 */
public void mousePressed(T aTable, ViewEvent anEvent)
{
    // Initialize resize bar index
    _resizeBarIndex = -1;
    
    // Get event point in table coords
    RMEditor editor = getEditor();
    Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), aTable);
    
    // If table isn't super selected, forward to TableRow and return
    if(!isSuperSelected(aTable)) {
        RMTableRow tableRow = (RMTableRow)aTable.getChildContaining(point); // Get hit table row
        if(tableRow!=null && tableRow.isStructured()) // If table row is structured
            getTool(tableRow).processEvent(tableRow, anEvent);
        return;
    }
    
    // If we're not editor super selected shape, just return
    if(editor.getSuperSelectedShape()!=aTable) return;
    
    // If point hit's table row, just return
    if(aTable.getChildContaining(point)!=null) return;
    
    // Since we are the editor super selected shape, consume event to indicate we'll handle events
    anEvent.consume();
    
    // If point is inside table group button, super select table group 
    if(aTable.getParent() instanceof RMTableGroup && point.x<100 && point.y>aTable.getHeight()-18)
        editor.setSuperSelectedShape(aTable.getParent());
    
    // Get resize bar index for point
    _resizeBarIndex = aTable.getResizeBarAtPoint(point);
    
    // If no selected resize bar, just return
    if(_resizeBarIndex == -1) return;
    
    // Get the table row above resize bar
    RMTableRow tableRow = (RMTableRow)aTable.getChild(_resizeBarIndex);
    
    // Get the x location of resize bar popup menu
    double resizeBarPopupX = getResizeBarPopupX(tableRow);

    // If downPoint is on version, run its context menu
    if(point.x > resizeBarPopupX) {
        editor.setSuperSelectedShape(tableRow); // Super select resize bar table row and run popup menu
        runMenuForShape(aTable, resizeBarPopupX, aTable.getResizeBarBounds(_resizeBarIndex).getMaxY());
        _resizeBarIndex = -1; // Reset resize bar index
    }

    // If downPoint is on structuredButton, change structured state of child
    else if(point.x > resizeBarPopupX - 20) {
        aTable.undoerSetUndoTitle("Turn Table Row Structuring " + (tableRow.isStructured()? "Off" : "On"));
        tableRow.repaint(); // Register table row for repaint
        tableRow.setStructured(!tableRow.isStructured()); // Toggle structured setting
        _resizeBarIndex = -1; // Reset resize bar index
    }

    // Set last mouse point to down point
    _lastMousePoint = point;
    
    // Set undo title
    aTable.undoerSetUndoTitle("Resize Table Row");
}

/**
 * Returns the x location of the given resize bar popup.
 */
public double getResizeBarPopupX(RMTableRow aTableRow)
{
    String version = aTableRow.getVersion(); // Get table row version string 
    double versionWidth = Font.Arial12.getStringAdvance(version); // Get width of version
    return aTableRow.getWidth() - versionWidth - 13; // Get start of version
}

/**
 * Event handling for table editing.
 */
public void mouseDragged(T aTable, ViewEvent anEvent)
{
    // If no resize bar selected, just return
    if(_resizeBarIndex<0) return;
    
    // Get event point in table coords
    Point downPoint = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aTable);
    
    // Get change in Height and child for current ResizeBarIndex
    double dh = downPoint.y - _lastMousePoint.y;
    
    // Get table row for resize bar
    RMShape tableRow = aTable.getChild(_resizeBarIndex);

    // Make sure dh doesn't cause row to be smaller than zero or cause last row to go below bottom of table
    dh = MathUtils.clamp(dh, -Math.abs(tableRow.height()), Math.abs(aTable.height()) -
        aTable.getResizeBarBounds(aTable.getChildCount()-1).getMaxY());
    
    // Update last mouse point, rese table row height and repaint table
    _lastMousePoint.y += dh;
    tableRow.setHeight(tableRow.height() + dh);
    aTable.relayout(); aTable.repaint();
}

/**
 * Event handling for table editing.
 */
public void mouseReleased(T aTable, ViewEvent anEvent)
{
    if(_resizeBarIndex<0) return;                                         // If no resize bar selected, just return
    getEditor().setSuperSelectedShape(aTable.getChild(_resizeBarIndex));  // Super select child above resize bar
}

/**
 * Opens a popup menu specific for table row divider under mouse.
 */
public void runMenuForShape(RMShape aShape, double x, double y)
{
    RMEditor editor = getEditor(); // Get editor
    RMTableRow tableRow = (RMTableRow)editor.getSuperSelectedShape(); // Get table row
    RMTableRowTool tableRowTool = (RMTableRowTool)getTool(tableRow); // Get table row tool
    Menu pmenu = tableRowTool.getPopupMenu(tableRow); // Fill menu
    Point point = editor.convertFromShape(x, y, aShape); // Get point in editor
    pmenu.show(editor, point.x, point.y); // Show popup menu
}

/**
 * Overrides shape implementation to declare no handles when the child of a table group.
 */
public int getHandleCount(T aShape)
{
    return aShape.getParent() instanceof RMTableGroup? 0 : super.getHandleCount(aShape);
}

}