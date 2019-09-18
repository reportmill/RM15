/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * This class provides support for UI editing of RMCrossTab.
 */
public class RMCrossTabTool <T extends RMCrossTab> extends RMTool <T> {

    // The currently selected divider
    RMCrossTabDivider  _divider;
    
    // Whether popup trigger has been encountered during mouse loop
    boolean            _popupTriggered;
    
/**
 * Called to initialize UI.
 */
protected void initUI()  { enableEvents("DatasetKeyText", DragDrop); enableEvents("FilterKeyText", DragDrop); }

/**
 * Resets the UI from current selected crosstab.
 */
public void resetUI()
{
    // Get currently selected crosstab (just return if null)
    RMCrossTab table = getTable(); if(table==null) return;
    
    // Update DatasetKeyText, FilterKeyText, RowCountSpinner, ColCountSpinner, HeaderRow/HeaderColCountSpinner
    setViewValue("DatasetKeyText", table.getDatasetKey());
    setViewValue("FilterKeyText", table.getFilterKey());
    setViewValue("RowCountSpinner", table.getRowCount());
    setViewValue("ColCountSpinner", table.getColCount());
    setViewValue("HeaderRowCountSpinner", table.getHeaderRowCount());
    setViewValue("HeaderColCountSpinner", table.getHeaderColCount());
}

/**
 * Updates currently selected crosstab from UI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected CrossTab and cell (just return if null)
    RMCrossTab ctab = getTable(); if(ctab==null) return;
    RMCrossTabCell cell = getCell();
    
    // Handle DatasetKeyText, FilterKeyText
    if(anEvent.equals("DatasetKeyText")) ctab.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));
    if(anEvent.equals("FilterKeyText")) ctab.setFilterKey(StringUtils.delete(anEvent.getStringValue(), "@"));

    // Handle RowsSpinner: Get count, make sure it's at least 1 and at least the number of header rows, and set
    if(anEvent.equals("RowCountSpinner")) {
        int count = anEvent.getIntValue();
        count = Math.max(count, 1); count = Math.max(count, ctab.getHeaderRowCount());
        ctab.setRowCount(count);
    }
    
    // Handle ColumnsSpinner: Get count, make sure it's at least 1 and at least the number of header columns, and set
    if(anEvent.equals("ColCountSpinner")) {
        int count = anEvent.getIntValue();
        count = Math.max(count, 1); count = Math.max(count, ctab.getHeaderColCount());
        ctab.setColCount(count);
    }
    
    // Handle HeaderRowCountSpinner, HeaderColCountSpinner
    if(anEvent.equals("HeaderRowCountSpinner"))
        if(ctab.getRowCount()-ctab.getHeaderRowCount()+anEvent.getIntValue()>0)
            ctab.setHeaderRowCount(anEvent.getIntValue());
    if(anEvent.equals("HeaderColCountSpinner"))
        if(ctab.getColCount()-ctab.getHeaderColCount()+anEvent.getIntValue()>0)
            ctab.setHeaderColCount(anEvent.getIntValue());
    
    // Handle ClearContentsMenuItem
    if(anEvent.equals("ClearContentsMenuItem"))
        for(int i=0, iMax=getEditor().getSelectedOrSuperSelectedShapeCount(); i<iMax; i++)
            if(getEditor().getSelectedOrSuperSelectedShape(i) instanceof RMCrossTabCell) {
                getEditor().getSelectedOrSuperSelectedShape(i).repaint();
                ((RMCrossTabCell)getEditor().getSelectedOrSuperSelectedShape(i)).clearContents();
            }
    
    // Handle AddRowAboveMenuItem, AddRowBelowMenuItem, AddColBeforeMenuItem, AddColAfterMenuItem
    if(anEvent.equals("AddRowAboveMenuItem") && cell!=null) ctab.addRow(cell.getRow());
    if(anEvent.equals("AddRowBelowMenuItem") && cell!=null) ctab.addRow(cell.getRowEnd() + 1);
    if(anEvent.equals("AddColBeforeMenuItem") && cell!=null) ctab.addCol(cell.getCol());
    if(anEvent.equals("AddColAfterMenuItem") && cell!=null) ctab.addCol(cell.getColEnd() + 1);
    
    // Handle RemoveRowMenuItem, RemoveColMenuItem
    if(anEvent.equals("RemoveRowMenuItem") && cell!=null) {
        ctab.removeRow(cell.getRow()); getEditor().setSuperSelectedShape(ctab); }
    if(anEvent.equals("RemoveColMenuItem") && cell!=null) {
        ctab.removeCol(cell.getCol()); getEditor().setSuperSelectedShape(ctab); }
    
    // Handle MergeCellsMenuItem
    if(anEvent.equals("MergeCellsMenuItem") && cell!=null) {
        
        // Get selected cell row/col min/min and expand to total row & col min/max
        int rowMin = cell.getRow(), rowMax = cell.getRow();
        int colMin = cell.getCol(), colMax = cell.getCol();
        for(int i=1; i<getEditor().getSelectedShapeCount(); i++) { 
            cell = (RMCrossTabCell)getEditor().getSelectedShape(i);
            rowMin = Math.min(rowMin, cell.getRow()); rowMax = Math.max(rowMax, cell.getRow());
            colMin = Math.min(colMin, cell.getCol()); colMax = Math.max(colMax, cell.getCol());
        }
            
        // Have table merge cells and super-select cell
        ctab.mergeCells(rowMin, colMin, rowMax, colMax);
        getEditor().setSuperSelectedShape(ctab.getCell(rowMin, colMin));
    }
    
    // Handle SplitCellMenuItem
    if(anEvent.equals("SplitCellMenuItem") && cell!=null)
        ctab.splitCell((RMCrossTabCell)getEditor().getSelectedOrSuperSelectedShape());
}

/**
 * Event handling - overridden to set a custom cursor.
 */
public void mouseMoved(T aCTab, ViewEvent anEvent)
{
    // Get shape under point
    RMShape shape = getEditor().getShapeAtPoint(anEvent.getX(), anEvent.getY());
    
    // If shape is a divider, set RESIZE_CURSOR
    if(shape instanceof RMCrossTabDivider) { RMCrossTabDivider divider = (RMCrossTabDivider)shape;
        if(divider.isRowDivider()) getEditor().setCursor(Cursor.N_RESIZE);
        else getEditor().setCursor(Cursor.W_RESIZE);
        anEvent.consume(); // Consume event
    }
    
    // If shape is cell, set TEXT_CURSOR
    else if(shape instanceof RMCrossTabCell) {
        getEditor().setCursor(Cursor.TEXT);
        anEvent.consume();
    }
        
    // Otherwise, do default behavior
    else super.mouseMoved(aCTab, anEvent);
}

/**
 * Handles Shape MousePressed.
 */
public void mousePressed(T aCTab, ViewEvent anEvent)
{
    // If event is popup trigger, run crosstab popup
    if(anEvent.isPopupTrigger()) { _popupTriggered = true; runContextMenu(anEvent); return; }
    
    // If super selected, ensure that cell under event point is super selected
    if(getEditor().getSuperSelectedShape()==aCTab) {
        
        // Get the event point in crosstab coords and cell under point
        Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aCTab);
        RMShape piece = aCTab.getChildContaining(point);
        
        // Clear divider
        _divider = null;
        
        // If hit cell: Record DownPoint and consume
        if(piece instanceof RMCrossTabCell) {
            _downPoint = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aCTab);
            anEvent.consume();
        }
        
        // If hit divider, set it
        else if(piece instanceof RMCrossTabDivider) {
            _divider = (RMCrossTabDivider)piece; // Set divider
            getEditor().setSelectedShape(_divider); // Select divider
            anEvent.consume(); // Consume event
        }
    }
}

/**
 * Handle CrossTab mouse dragged.
 */
public void mouseDragged(T aCTab, ViewEvent anEvent)
{
    // If popup trigger, consume event and return
    if(anEvent.isPopupTrigger() || _popupTriggered) { _popupTriggered = true; anEvent.consume(); return; }
    
    // If no divider, select cells in selection rect
    if(_divider==null) {
        
        // Get event point int table coords and cell rect for DownPoint and EventPoint
        Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aCTab);
        Rect crect = getCellRect(aCTab, Rect.get(_downPoint, point)); crect.snap();
        int cx = (int)crect.getX(), cy = (int)crect.getY(), cw = (int)crect.getWidth(), ch = (int)crect.getHeight();
        
        // Create/fill list with selected cells (unique)
        List cells = new ArrayList();
        for(int i=cy; i<=cy+ch; i++)
            for(int j=cx; j<=cx+cw; j++)
                if(!cells.contains(aCTab.getCell(i, j)))
                    cells.add(aCTab.getCell(i, j));
        
        // Select cells
        getEditor().setSelectedShapes(cells);
    }
    
    // If divider is row divider, resize rows
    else if(_divider.isRowDivider()) {
        
        // Get event point in table coords and divider move delta
        Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aCTab);
        double delta = point.y - _divider.getY();
        
        // Get divider row and resize
        RMCrossTabRow row = _divider.getRow();
        if(row.getIndex()>=0) row.setHeight(row.getHeight() + delta);
    }
    
    // If divider is column divider, resize columns
    else {
        
        // Get event point in table coords and divider move delta
        Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aCTab);
        double delta = point.x - _divider.getX();
        
        // Get divider column and resize
        RMCrossTabCol column = _divider.getCol();
        if(column.getIndex()>=0) column.setWidth(column.getWidth() + delta);
    }
    
    // Register for layout/repaint
    aCTab.relayout(); aCTab.repaint();
}

/**
 * Handle crosstab mouse released.
 */
public void mouseReleased(T aCTab, ViewEvent anEvent)
{
    // If popup trigger, consume event and return
    if(anEvent.isPopupTrigger() || _popupTriggered) {
        if(!_popupTriggered) runContextMenu(anEvent); _popupTriggered = false; anEvent.consume(); return; }
}

/**
 * Key event handler for crosstab editing.
 */
public void processKeyEvent(T aCTab, ViewEvent anEvent)
{
    // If event isn't typed or pressed, just return
    if(anEvent.isKeyPress() && anEvent.isKeyType()) return;
    
    // Have table register for repaint (and thus undo)
    aCTab.repaint();
    
    // Get key code
    RMEditor editor = getEditor();
    int keyCode = anEvent.getKeyCode();
    
    // If backspace or delete key is pressed, remove selected divider
    if(editor.getSelectedShape() instanceof RMCrossTabDivider) {
        
        // If key was backspace or delete, remove selected grouping
        if(keyCode==KeyCode.BACK_SPACE || keyCode==KeyCode.DELETE) {
            
            // Get the selected divider
            RMCrossTabDivider divider = (RMCrossTabDivider)editor.getSelectedShape();
            
            // If divider is last column or row divider, just beep
            if(divider.isColDivider()? divider.getNextCol()==null : divider.getNextRow()==null)
                beep();
            
            // If column divider, merge cells around divider
            else if(divider.isColDivider()) {
                
                // Get column index and iterate over divider rows and merge cells on either side of divider
                int col = divider.getCol().getIndex();
                for(int i=divider.getStart(), iMax=divider.getEnd(); i<iMax; i++)
                    aCTab.mergeCells(i, col, i, col+1);
            }
            
            // If row divider, merge cells around divider
            else {
                
                // Get divider row index and iterate over divider columns and merge cells on either side of divider
                int row = divider.getRow().getIndex();
                for(int i=divider.getStart(), iMax=divider.getEnd(); i<iMax; i++)
                    aCTab.mergeCells(row, i, row+1, i);
            }
            
            // Selected crosstab
            editor.setSuperSelectedShape(aCTab);
        }
    }
    
    // If selected shape is cell, either change selection or super select
    if(editor.getSelectedShape() instanceof RMCrossTabCell) {
        
        // Get selected cell
        RMCrossTabCell cell = (RMCrossTabCell)editor.getSelectedShape();
        
        // If key is right arrow or tab, move forward
        if(keyCode==KeyCode.RIGHT || (keyCode==KeyCode.TAB && !anEvent.isShiftDown()))
            editor.setSelectedShape(cell.getCellAfter());
        
        // If key is left arrow or shift tab, move backward
        else if(keyCode==KeyCode.LEFT || (keyCode==KeyCode.TAB && anEvent.isShiftDown()))
            editor.setSelectedShape(cell.getCellBefore());

        // If key is down arrow or enter, move down
        else if(keyCode==KeyCode.DOWN || (keyCode==KeyCode.ENTER && !anEvent.isShiftDown()))
            editor.setSelectedShape(cell.getCellBelow());
        
        // If key is up arrow or shift-enter, move up
        else if(keyCode==KeyCode.UP || (keyCode==KeyCode.ENTER && anEvent.isShiftDown()))
            editor.setSelectedShape(cell.getCellAbove());
        
        // If key has meta-down or control-down, just return
        else if(anEvent.isMetaDown() || anEvent.isControlDown())
            return;
        
        // If key char is control character or undefined, just return
        else if(anEvent.getKeyChar()==KeyCode.CHAR_UNDEFINED || Character.isISOControl(anEvent.getKeyChar()))
            return;
        
        // If key is anything else, superselect cell and forward key press to cell tool
        else {
            editor.setSuperSelectedShape(cell);
            getTool(cell).processKeyEvent(cell, anEvent);
        }
    }
    
    // Consume event
    anEvent.consume();
}

/**
 * Runs a context menu for the given event.
 */
public void runContextMenu(ViewEvent anEvent)
{
    // Create PopupMenu and configure
    Menu pmenu = new Menu();
    MenuItem mitem = createMenuItem("Clear Contents", "ClearContentsMenuItem"); pmenu.addItem(mitem);
    pmenu.addSeparator();
    mitem = createMenuItem("Add Row Above", "AddRowAboveMenuItem"); pmenu.addItem(mitem);
    mitem = createMenuItem("Add Row Below", "AddRowBelowMenuItem"); pmenu.addItem(mitem);
    pmenu.addSeparator();
    mitem = createMenuItem("Add Column Before", "AddColBeforeMenuItem"); pmenu.addItem(mitem);
    mitem = createMenuItem("Add Column After", "AddColAfterMenuItem"); pmenu.addItem(mitem);
    pmenu.addSeparator();
    mitem = createMenuItem("Remove Row", "RemoveRowMenuItem"); pmenu.addItem(mitem);
    mitem = createMenuItem("Remove Column", "RemoveColMenuItem"); pmenu.addItem(mitem);
    pmenu.addSeparator();
    mitem = createMenuItem("Merge Cells", "MergeCellsMenuItem"); pmenu.addItem(mitem);
    mitem = createMenuItem("Split Cell", "SplitCellMenuItem"); pmenu.addItem(mitem);
    
    // Run menu and consume event
    pmenu.setOwner(this);
    pmenu.show(anEvent.getView(), anEvent.getX(), anEvent.getY());
    anEvent.consume();
}

// Creates a new menu item
private MenuItem createMenuItem(String t, String n)  { MenuItem mi = new MenuItem();
    mi.setText(t); mi.setName(n); return mi; }

/**
 * Highlights the selected cells or dividers.
 */
public void paintHandles(T aShape, Painter aPntr, boolean isSuperSelected)
{
    // If not super-selected just do normal paintHandles and return
    if(!isSuperSelected) { super.paintHandles(aShape, aPntr, isSuperSelected); return; }
    
    // Get the table and declare rect to highlight
    RMEditor editor = getEditor();
    RMCrossTab table = getTable();
    Shape drawShape = null;
    
    // If super selected shape is RMCrossTabCell, get path for ouset bounds in editor coords
    if(editor.getSuperSelectedShape() instanceof RMCrossTabCell) {
        RMShape cell = editor.getSuperSelectedShape();
        Rect rect = cell.getBoundsInside(); rect.inset(-2.5f, -2.5f);
        drawShape = editor.convertFromShape(rect, cell);
    }
    
    // If selected shape is RMCrossTabCell, get composite area of bounds for all selected cell (outset) bounds
    else if(editor.getSelectedShape() instanceof RMCrossTabCell) {
        
        // Get composite area of selected shapes, get bounds of selected shapes ouset by 2
        for(int i=0, iMax=editor.getSelectedShapeCount(); i<iMax; i++) {
            Rect bounds = editor.getSelectedShape(i).getBounds(); bounds.inset(-2.5f, -2.5f);
            drawShape = drawShape!=null? Shape.add(drawShape, bounds) : bounds; }
        
        // Get shape of bounds transformed to editor
        if(drawShape!=null) drawShape = editor.convertFromShape(drawShape, table);
    }
    
    // If selected shape is divider, get path for divider outset bounds in editor coords
    else if(editor.getSelectedShape() instanceof RMCrossTabDivider) {
        RMShape shape = editor.getSelectedShape(); // Get current loop selected shape
        Rect bounds = shape.getBounds(); bounds.inset(-2.5f, -2.5f); // Get bounds outset by 3
        drawShape = editor.convertFromShape(bounds, table);
    }
    
    // If draw shape is non-null, stroke it
    if(drawShape!=null) {
        aPntr.setStroke(new Stroke(4*editor.getZoomFactor()));
        aPntr.setColor(new Color(253, 219, 19));
        aPntr.draw(drawShape);
    }
    
    // Do normal paintHandles
    super.paintHandles(aShape, aPntr, isSuperSelected);
}

/**
 * Returns the selected table.
 */
public RMCrossTab getTable()
{
    // Get editor and selected shape
    RMEditor editor = getEditor(); if(editor==null) return null;
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    
    // Iterate up chain until table is found and return
    while(shape!=null && !(shape instanceof RMCrossTab)) shape = shape.getParent();
    return (RMCrossTab)shape;
}

/**
 * Returns the selected cell.
 */
public RMCrossTabCell getCell()
{
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape(); // Get editor selected or super selected shape
    return shape instanceof RMCrossTabCell? (RMCrossTabCell)shape : null;
}

/**
 * Returns the cell rect for the given rectangle.
 */
private Rect getCellRect(RMCrossTab aCTab, Rect aRect)
{
    // Get row min & max and col min & max and return rect
    int rowMin = MathUtils.clamp(aCTab.getRow(aRect.getY()), 0, aCTab.getRowCount() - 1);
    int rowMax = MathUtils.clamp(aCTab.getRow(aRect.getMaxY()), 0, aCTab.getRowCount() - 1);
    int colMin = MathUtils.clamp(aCTab.getCol(aRect.getX()), 0, aCTab.getColCount() - 1);
    int colMax = MathUtils.clamp(aCTab.getCol(aRect.getMaxX()), 0, aCTab.getColCount() - 1);
    return new Rect(colMin, rowMin, colMax - colMin, rowMax - rowMin);
}

/**
 * Returns the shape class this tool edits (RMTable).
 */
public Class getShapeClass()  { return RMCrossTab.class; }

/**
 * Returns the display name for this tool ("Table Inspector").
 */
public String getWindowTitle()  { return "CrossTab Inspector"; }

/**
 * Overridden to make crosstab super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make crosstab ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Returns the number of handles for this shape.
 */
public int getHandleCount(T aShape)
{
    // If crosstab parent isn't crosstab frame, return normal tool implementation
    if(!(aShape.getParent() instanceof RMCrossTabFrame)) return super.getHandleCount(aShape);
    return 3;  // crosstab in crosstab frame has 3 handles
}

/**
 * Editor method.
 */
public Point getHandlePoint(T aShape, int aHandle, boolean isSuperSelected)
{
    // If crosstab parent isn't crosstab frame, return normal tool implementation
    if(!(aShape.getParent() instanceof RMCrossTabFrame))
        return super.getHandlePoint(aShape, aHandle, isSuperSelected);
    
    // Call base tool implementation with base tool handle
    return super.getHandlePoint(aShape, getBaseHandle(aHandle), isSuperSelected);
}

/**
 * Returns the cursor for given handle.
 */
public Cursor getHandleCursor(T aShape, int aHandle)
{
    // If crosstab parent isn't crosstab frame, return normal tool implementation
    if(!(aShape.getParent() instanceof RMCrossTabFrame))
        return super.getHandleCursor(aShape, aHandle);
    
    // Call base tool implementation with base tool handle
    return super.getHandleCursor(aShape, getBaseHandle(aHandle));    
}

/**
 * Editor method.
 */
public void moveShapeHandle(T aShape, int aHandle, Point aPoint)
{
    // If crosstab parent isn't crosstab frame, return normal tool implementation
    if(!(aShape.getParent() instanceof RMCrossTabFrame))
        super.moveShapeHandle(aShape, aHandle, aPoint);
    
    // Call base tool implementation with base tool handle
    else getToolForClass(RMShape.class).moveShapeHandle(aShape, getBaseHandle(aHandle), aPoint);
}

/**
 * Returns the base handle for the crosstab child handle.
 */
private int getBaseHandle(int aHandle)
{
    if(aHandle==0) return HandleSE; // Remap handle 0
    if(aHandle==1) return HandleE; // Remap handle 1
    return HandleS; // Remap handle 2
}

/**
 * Adds a crosstab to the given editor with the given list key.
 */
public static void addCrossTab(RMEditor anEditor, String aKeyPath)
{
    // Create CrossTab frame and set DatasetKey
    RMCrossTabFrame ctab = new RMCrossTabFrame();
    ctab.getTable().setDatasetKey(aKeyPath);

    // Get parent for shape add and set ctab shape location in middle of parent
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    ctab.setXY((parent.getWidth() - ctab.getWidth())/2, (parent.getHeight() - ctab.getHeight())/2);
    
    // Add table, select table, select selectTool and redisplay
    anEditor.undoerSetUndoTitle("Add CrossTab");
    parent.addChild(ctab);
    anEditor.setCurrentToolToSelectTool();
    anEditor.setSelectedShape(ctab);
}

/**
 * Adds a crosstab to the given editor with the given list key.
 */
public static void addCrossTab(RMEditor anEditor)
{
    // Create and configure default table
    RMCrossTab ctab = new RMCrossTab();
    ctab.setRowCount(3); ctab.setColCount(3); ctab.setHeaderRowCount(1);
    
    // Get parent for shape add and set ctab shape location in middle of parent
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    ctab.setXY((parent.getWidth() - ctab.getWidth())/2, (parent.getHeight() - ctab.getHeight())/2);
    
    // Add table, select table, select selectTool and redisplay
    anEditor.undoerSetUndoTitle("Add Simple Table");
    parent.addChild(ctab);
    anEditor.setCurrentToolToSelectTool();
    anEditor.setSelectedShape(ctab);
}

}