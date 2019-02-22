/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Point;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * This class provides UI editing for table rows.
 */
public class RMTableRowTool <T extends RMTableRow> extends RMParentShapeTool <T> {

/**
 * Initialize UI.
 */
protected void initUI()  { enableEvents("VersionKeyText", DragDrop); enableEvents("PageBreakKeyText", DragDrop); }

/**
 * Update UI controls.
 */
public void resetUI()
{
    // Get selected table row (just return if null)
    RMTableRow trow = getSelectedShape(); if(trow==null) return;
    
    // Update StructuredCheckBox
    setViewValue("StructuredCheckBox", trow.isStructured());
    
    // Update NumOfColumnsText
    setViewValue("NumOfColumnsText", trow.getNumberOfColumns());
    setViewEnabled("NumOfColumnsText", trow.isStructured());
    
    // Update SyncParentCheckBox
    setViewValue("SyncParentCheckBox", trow.getSyncStructureWithRowAbove());
    setViewEnabled("SyncParentCheckBox", trow.isStructured());
    
    // Update SyncAlternatesCheckBox
    setViewValue("SyncAlternatesCheckBox", trow.getSyncStructureWithAlternates());
    setViewEnabled("SyncAlternatesCheckBox", trow.isStructured());
    
    // Update StayWithChildrenCheckBox, ReprintCheckBox
    setViewValue("StayWithChildrenCheckBox", trow.getNumberOfChildrenToStayWith()>0);
    setViewValue("ReprintCheckBox", trow.getReprintWhenWrapped());
    
    // Update NumOfChildrenText
    setViewValue("NumOfChildrenText", trow.getNumberOfChildrenToStayWith());
    setViewEnabled("NumOfChildrenText", trow.getNumberOfChildrenToStayWith() > 0);
    
    // Update PrintIfNoObjectsCheckBox, MoveToBottomCheckBox
    setViewValue("PrintIfNoObjectsCheckBox", trow.getPrintEvenIfGroupIsEmpty());
    setViewValue("MoveToBottomCheckBox", trow.getMoveToBottom());
    
    // Update MinSplitHeightText, MinRemainderHeightText
    setViewValue("MinSplitHeightText", getUnitsFromPoints(trow.getMinSplitHeight()));
    setViewValue("MinRemainderHeightText", getUnitsFromPoints(trow.getMinSplitRemainderHeight()));
    
    // Update VersionKeyText, PageBreakKeyText, DeleteVSpansCheckBox, ShiftShapesCheckBox
    setViewValue("VersionKeyText", trow.getVersionKey());
    setViewValue("PageBreakKeyText", trow.getPageBreakKey());
    setViewValue("DeleteVSpansCheckBox", trow.getDeleteVerticalSpansOfHiddenShapes());
    setViewValue("ShiftShapesCheckBox", trow.getShiftShapesBelowHiddenShapesUp());
}

/**
 * Handle UI changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected table row (just return if null)
    RMTableRow trow = getSelectedShape(); if(trow==null) return;
    trow.repaint();

    // Handle StructuredCheckBox
    if(anEvent.equals("StructuredCheckBox")) trow.setStructured(anEvent.getBoolValue());
    
    // Handle NumOfColumnsText    
    if(anEvent.equals("NumOfColumnsText")) trow.setNumberOfColumns(anEvent.getIntValue());

    // Handle SyncParentCheckBox, SyncAlternatesCheckBox
    if(anEvent.equals("SyncParentCheckBox")) trow.setSyncStructureWithRowAbove(anEvent.getBoolValue());
    if(anEvent.equals("SyncAlternatesCheckBox")) trow.setSyncStructureWithAlternates(anEvent.getBoolValue());

    // Handle StayWithChildrenCheckBox, ReprintCheckBox
    if(anEvent.equals("StayWithChildrenCheckBox"))
        trow.setNumberOfChildrenToStayWith(anEvent.getBoolValue()? 1 : 0);
    if(anEvent.equals("ReprintCheckBox"))
        trow.setReprintWhenWrapped(anEvent.getBooleanValue());
    
    // Handle NumOfChildrenText
    if(anEvent.equals("NumOfChildrenText"))
        trow.setNumberOfChildrenToStayWith(anEvent.getIntValue());

    // Handle PrintIfNoObjectsCheckBox, MoveToBottomCheckBox
    if(anEvent.equals("PrintIfNoObjectsCheckBox")) trow.setPrintEvenIfGroupIsEmpty(anEvent.getBoolValue());
    if(anEvent.equals("MoveToBottomCheckBox")) trow.setMoveToBottom(anEvent.getBoolValue());

    // Handle MinSplitHeightText, MinRemainderHeightText
    if(anEvent.equals("MinSplitHeightText")) trow.setMinSplitHeight(getPointsFromUnits(anEvent.getFloatValue()));
    if(anEvent.equals("MinRemainderHeightText"))
        trow.setMinSplitRemainderHeight(getPointsFromUnits(anEvent.getFloatValue()));

    // Handle VersionKeyText, PageBreakKeyText, DeleteVSpansCheckBox, ShiftShapesCheckBox
    if(anEvent.equals("VersionKeyText")) { String value = anEvent.getStringValue().replace("@", "");
        trow.setVersionKey(value); }
    if(anEvent.equals("PageBreakKeyText")) trow.setPageBreakKey(anEvent.getStringValue());
    if(anEvent.equals("DeleteVSpansCheckBox")) trow.setDeleteVerticalSpansOfHiddenShapes(anEvent.getBoolValue());
    if(anEvent.equals("ShiftShapesCheckBox")) trow.setShiftShapesBelowHiddenShapesUp(anEvent.getBoolValue());
        
    // Handle PopupMenu
    if(anEvent.equals("SetVersionMenuItem")) setVersionFromMenu(anEvent.getText());
    if(anEvent.equals("RemoveMenuItem")) removeVersionCurrent();
    if(anEvent.getName().startsWith("AddVersionMenuItem ")) {
        String name = anEvent.getName().substring("AddVersionMenuItem ".length());
        addVersionFromMenu(name); }
}

/**
 * Loads a popup menu with menus specific for currently selected table row.
 */
public Menu getPopupMenu(RMTableRow aTableRow)
{
    // Create pop up menu
    Menu menu = new Menu();

    // Get list of alternates names. Make sure it has current mode
    List <String> names = aTableRow.getAlternates()!=null? new ArrayList(aTableRow.getAlternates().keySet()) :
        new ArrayList();

    // Make sure names array has current version
    if(!names.contains(aTableRow.getVersion()))
        names.add(aTableRow.getVersion());

    // Sort Alternates and make sure Standard band is listed first
    Collections.sort(names); names.remove("Standard"); names.add(0, "Standard");

    // Add menu items for each version
    for(String name : names) {
        MenuItem item = new MenuItem(); item.setText(name); item.setName("SetVersionMenuItem");
        menu.addItem(item);
    }

    // Add a menu divider
    menu.addSeparator();

    // Add 'Remove' menu item
    MenuItem item = new MenuItem(); item.setText("Remove"); item.setName("RemoveMenuItem");
    menu.addItem(item);
    
    // Add AddVersionMenuItem(s) for versions that aren't present
    String namesAll[] = { "First Only", "Reprint", "Alternate", "Running", "TopN Others", "Split Header", "Custom..." };
    for(String name : namesAll) {
        if(names.contains(name)) continue;
        item = new MenuItem(); item.setText(name); item.setName("AddVersionMenuItem " + name);
        menu.addItem(item);
    }

    // Init popup menu and return
    menu.setOwner(this);
    return menu;
}

/**
 * Sets the version of the currently selected table row.
 */
public void setVersionFromMenu(String aVersion)
{
    // Get editor and selected TableRow
    RMEditor editor = getEditor();
    RMTableRow tableRow = (RMTableRow)editor.getSelectedOrSuperSelectedShape();
    
    // Set version
    tableRow.repaint(); // Register table row for repaint
    tableRow.undoerSetUndoTitle("Version Change"); // Set undo title
    tableRow.setVersion(aVersion); // Set table row version
    tableRow.getParent().repaint(); // Register table for repaint
    editor.setSuperSelectedShape(tableRow); // Super select table row
}

/**
 * Adds a new version to the currently selected table row.
 */
public void addVersionFromMenu(String aVersion)
{
    // Get main editor and selected TableRow
    RMEditor editor = getEditor();
    RMTableRow tableRow = (RMTableRow)editor.getSelectedOrSuperSelectedShape();

    // Get name of Custom Version if requested
    if(aVersion.equals("Custom...")) {
        DialogBox dbox = new DialogBox("Custom Alternate");
        dbox.setQuestionMessage("Input label for custom table row version");
        aVersion = dbox.showInputDialog(editor, "");
    }
    
    // If version string is invalid, just return
    if(StringUtils.length(aVersion)==0) return;
    
    // If version already exists, set version instead
    if(tableRow.hasVersion(aVersion)) {
        setVersionFromMenu(aVersion); return; }
    
    // Set version
    tableRow.repaint(); // Register table row for repaint
    tableRow.undoerSetUndoTitle("Add New Version"); // Set undo title
    tableRow.setVersion(aVersion); // Set version
    editor.setSelectedShapes(tableRow.getChildren()); // Select new table row children
}

/**
 * Removes the currently selected version from the currently selected table row.
 */
public void removeVersionCurrent()
{
    // Get main editor and selected TableRow
    RMEditor editor = getEditor();
    RMTableRow tableRow = (RMTableRow)editor.getSelectedOrSuperSelectedShape();
    
    // Register table row for repaint (thus undo)
    tableRow.repaint();
    
    // Get current table row version
    String version = tableRow.getVersion();

    // Complain and return if user tries to remove Standard version
    if(version.equals("Standard"))
        beep();

    // Remove version (with undo grouping)
    else {
        tableRow.undoerSetUndoTitle("Remove Version");
        tableRow.removeVersion(version);
    }
    
    // Register table for repaint
    tableRow.getParent().repaint();
}

/**
 * Adds a column to the currently selected table row.
 */
public static void addColumn(RMEditor anEditor)
{
    // Get currently selected editor and selected shape
    RMShape shape = anEditor.getSelectedOrSuperSelectedShape();
    
    // Get currently selected table row (by iterating up selected shape's ancestor list)
    while(shape!=null && !(shape instanceof RMTableRow))
        shape = shape.getParent();
    
    // If no currently selected table row, just return
    if(shape==null) return;
    
    // Add column
    RMTableRow tableRow = (RMTableRow)shape; // Get the table row
    tableRow.setNumberOfColumns(tableRow.getNumberOfColumns()+1); // Increment ColumnCount
    anEditor.setSuperSelectedShape(tableRow.getChildLast()); // Super-Select last child
}

/**
 * Returns the class that this tool is responsible for (RMTableRow).
 */
public Class getShapeClass()  { return RMTableRow.class; }

/**
 * Returns the name that should be used in the inspector window.
 */
public String getWindowTitle()  { return "Table Row Inspector"; }

/**
 * Overridden to make table row not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * MouseMoved implementation to update cursor for resize bars.
 */
public void mouseMoved(T aTableRow, ViewEvent anEvent)
{
    // If structured
    if(!aTableRow.isStructured()) { super.mouseMoved(aTableRow, anEvent); return; }
        
    // Get handle shape
    RMShapeHandle shapeHandle = getShapeHandleAtPoint(anEvent.getPoint());
    
    // If shape handle is non-null, see if it's a structured text that needs special cursor
    if(shapeHandle!=null) {

        // If shape handle shape is structured text, set cursor, consume event and return
        if(shapeHandle.shape instanceof RMTextShape && ((RMTextShape)shapeHandle.shape).isStructured()) {
            if(shapeHandle.handle==HandleNW) getEditor().setCursor(Cursor.W_RESIZE);
            else getEditor().setCursor(Cursor.E_RESIZE);
            anEvent.consume(); return;
        }
    }
    
    // Do normal mouse moved
    super.mouseMoved(aTableRow, anEvent);
}

/**
 * Mouse pressed implementation to make sure structured table row columns get selected.
 */
public void mousePressed(T aTableRow, ViewEvent anEvent)
{
    // If selected and structured, select child
    RMEditor editor = getEditor();
    if(aTableRow.isStructured() && aTableRow!=editor.getSuperSelectedShape().getParent()) {
        
        // Get the point and child at point
        Point point = editor.convertToShape(anEvent.getX(), anEvent.getY(), aTableRow);
        RMShape child = aTableRow.getChildContaining(point);
        
        // If child was hit, super select it and resend event
        if(child!=null) {
            editor.setSuperSelectedShape(child); // Select child
            editor.getSelectTool().setRedoMousePressed(true); // Have SelectTool resend event
        }
    }
}

/**
 * Overrides tool method to declare that table rows have no handles.
 */
public int getHandleCount(T aShape)  { return 0; }

}