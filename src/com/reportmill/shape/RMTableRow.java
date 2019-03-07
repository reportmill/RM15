/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;

/**
 * This class models a table row in a table.
 */
public class RMTableRow extends RMSwitchShape {

    // The table row title
    String            _title;
    
    // Whether table row children are automatically laid out from left to right
    boolean           _structured;
    
    // Whether this row should force row above to have same column positions
    boolean           _syncStructureWithRowAbove;
    
    // Whether this row should force alternate versions to have same column positions
    boolean           _syncStructureWithAlternates;
    
    // The number of children this row needs to have to stay on the current page (widow/orphan stuff)
    int               _numberOfChildrenToStayWith = 1;
    
    // Whether this row should be reprinted if a child group was broken across page boundary
    boolean           _reprintWhenWrapped = true;
    
    // Whether this row should print, even if it has no child objects (really for Header/Summary rows)
    boolean           _printEvenIfNoObjectsInGroup = true;
    
    // Whether this row should move to the bottom of the table after being filled (page summary type stuff)
    boolean           _moveToBottom;
    
    // The minimum height of the top part of this row, if split across page boundary
    double            _minSplitHeight = 72;
    
    // The minimum height of the bottom part of this row, if split across page boundary
    double            _minSplitRemainderHeight = 72;
    
    // The key used to determine whether row should force a page break
    String            _pageBreakKey;
    
    // Whether to delete vertical spans of hidden shapes
    boolean           _deleteVerticalSpansOfHiddenShapes;
    
    // Whether to shift shapes below hidden shapes up
    boolean           _shiftShapesBelowHiddenShapesUp;
    
    // Common version constants
    public static final String VersionStandard = "Standard";
    public static final String VersionFirstOnly = "First Only";
    public static final String VersionReprint = "Reprint";
    public static final String VersionAlternate = "Alternate";
    public static final String VersionTopNOthers = "TopN Others";
    public static final String VersionSplitHeader = "Split Header";
    
    // Constants for the methods to resize row columns when row is resized
    public enum ColumnResizeMode { AllColumns, LastColumn }; // NextColumn, NextColumns, NoResize
    
/**
 * Creates a plain, unstructured table row.
 */    
public RMTableRow()  { setSize(400, 18); }

/**
 * Creates a table row allowing the user to specify whether it should be structured.
 */
public RMTableRow(boolean isStructured)
{
    // Do normal RMTableRow init
    this();
    
    // Set structured (and if isStructured, set number of columns to 4 by default)
    _structured = isStructured;
    if(_structured)
        setNumberOfColumns(4);
}

/**
 * Returns the parent of this table row.
 */
public RMTable getTable()  { return ClassUtils.getInstance(getParent(), RMTable.class); }

/**
 * Returns the title for this table row.
 */
public String getTitle()  { return _title!=null? _title : "UnknownTitle"; }

/**
 * Sets the title for this table row.
 */
public void setTitle(String aTitle)  { _title = aTitle; }

/**
 * Returns whether this table row is structured.
 */
public boolean isStructured()  { return _structured; }

/**
 * Sets whether this table row should be structured.
 */
public void setStructured(boolean aFlag)
{
    // If value already set, just return
    if(aFlag==_structured) return;
    
    // Register for repaint (and thus undo)
    repaint();

    // Set value and fire property change
    firePropChange("Structured", _structured, _structured = aFlag);
    
    // If turning structuring on and there are no children, add some
    if(_structured) {
        if(getChildCount()==0)
            setNumberOfColumns(4);
    }

    // If turning structuring off, remove all empty texts
    else for(int i=getChildCount()-1; i>=0; i--) { RMShape child = getChild(i);
        child.setAutosizing("--~,--~");
        if(child instanceof RMTextShape && ((RMTextShape)child).length()==0)
            removeChild(i);
    }
}

/**
 * Returns whether table row should synchronize the column widths of the row immediately above it.
 */
public boolean getSyncStructureWithRowAbove()  { return _syncStructureWithRowAbove; }

/**
 * Sets whether table row should synchronize the column widths of the row immediately above it.
 */
public void setSyncStructureWithRowAbove(boolean aFlag)
{
    // If value already set, just return
    if(_syncStructureWithRowAbove==aFlag) return;
    
    // Set value and fire property change
    firePropChange("SyncStructureWithRowAbove", _syncStructureWithRowAbove, _syncStructureWithRowAbove = aFlag);
    
    // If there is a row above, sync its structure with this row
    if(getRowAbove()!=null)
        getRowAbove().syncStructureWithShape(this);
}

/**
 * Returns whether table row should synchronize the column widths of alternate versions.
 */
public boolean getSyncStructureWithAlternates()  { return _syncStructureWithAlternates; }

/**
 * Sets whether table row should synchronize the column widths of alternate versions.
 */
public void setSyncStructureWithAlternates(boolean aFlag)
{
    // If value already set, just return
    if(_syncStructureWithAlternates==aFlag) return;
    
    // Set value and fire property change
    firePropChange("SyncStructureWithAlternates", _syncStructureWithAlternates, _syncStructureWithAlternates = aFlag);
    
    // Iterate over alternates and sync structure
    if(_syncStructureWithAlternates && getAlternates()!=null)
        for(RMTableRow alternate : (Collection<RMTableRow>)(Collection)getAlternates().values())
            if(alternate!=this)
                alternate.syncStructureWithShape(this);
}

/**
 * Returns the column at the given index (assumes row is structured and column is RMTextShape).
 */
public RMTextShape getColumn(int anIndex)  { return ClassUtils.getInstance(getChild(anIndex), RMTextShape.class); }

/**
 * Returns the number of columns in this table row (really just child count).
 */
public int getNumberOfColumns()  { return getChildCount(); }

/**
 * Sets the number of columns in this table row.
 */
public void setNumberOfColumns(int aCount) 
{
    // If count is less than one, make it one
    if(aCount<1) aCount = 1;

    // Get average width of children
    double width = getChildCount()==0? 120 : RMShapeUtils.getAverageWidth(getChildren());
    
    // Iterate over missing children range and add children
    for(int i=getChildCount(); i<aCount; i++) {
        RMTextShape child = new RMTextShape();
        child.setX(10000); child.setWidth(width);
        addChild(child);
    }

    // If count is less than number of children, remove some children from end
    for(int i=getChildCount()-1; i>=aCount; i--)
        removeChild(i);

    // Set structured to true and layout
    setStructured(true);
}

/**
 * Returns the number of children this table row needs to be accompanied by, if some children run off page bottom.
 */
public int getNumberOfChildrenToStayWith()  { return _numberOfChildrenToStayWith; }

/**
 * Sets the number of children this table row needs to be accompanied by, if some children run off page bottom.
 */
public void setNumberOfChildrenToStayWith(int aValue)
{
    firePropChange("NumberOfChildrenToStayWith", _numberOfChildrenToStayWith, _numberOfChildrenToStayWith = aValue);
}

/**
 * Returns whether this row is reprinted on a new page when it's children cross a page boundary.
 */
public boolean getReprintWhenWrapped()  { return _reprintWhenWrapped; }

/**
 * Sets whether this row is reprinted on a new page when it's children cross a page boundary.
 */
public void setReprintWhenWrapped(boolean aFlag)
{
    firePropChange("ReprintWhenWrapped", _reprintWhenWrapped, _reprintWhenWrapped = aFlag);
}

/**
 * Returns whether this row should print even if it has no children (assumed to be a header/summary row).
 */
public boolean getPrintEvenIfGroupIsEmpty()  { return _printEvenIfNoObjectsInGroup; }

/**
 * Returns whether this row should print even if it has no children (assumed to be a header/summary row).
 */
public void setPrintEvenIfGroupIsEmpty(boolean aFlag)
{
    firePropChange("PrintEvenIfGroupIsEmpty", _printEvenIfNoObjectsInGroup, _printEvenIfNoObjectsInGroup = aFlag);
}

/**
 * Returns whether this row should be moved to the bottom of the table during report generation.
 */
public boolean getMoveToBottom()  { return _moveToBottom; }

/**
 * Sets whether this row should be moved to the bottom of the table during report generation.
 */
public void setMoveToBottom(boolean aFlag)
{
    firePropChange("MoveToBottom", _moveToBottom, _moveToBottom = aFlag);
}

/**
 * Returns the minimum distance in points from the top of the row that this row can split.
 */
public double getMinSplitHeight()  { return _minSplitHeight; }

/**
 * Sets the minimum distance in points from the top of the row that this row can split.
 */
public void setMinSplitHeight(double aValue)
{
    firePropChange("MinSplitHeight", _minSplitHeight, _minSplitHeight = aValue);
}

/**
 * Returns the minimum distance in points from the bottom of the row that this row can split.
 */
public double getMinSplitRemainderHeight()  { return _minSplitRemainderHeight; }

/**
 * Sets the minimum distance in points from the top of the row that this row can split.
 */
public void setMinSplitRemainderHeight(double aValue)
{
    firePropChange("MinSplitRemainderHeight", _minSplitRemainderHeight, _minSplitRemainderHeight = aValue);
}

/**
 * Returns the key used to determine whether row should force a page break.
 */
public String getPageBreakKey()  { return _pageBreakKey; }

/**
 * Sets the key used to determine whether row should force a page break.
 */
public void setPageBreakKey(String aString)
{
    firePropChange("PageBreakKey", _pageBreakKey, _pageBreakKey = aString);
}

/**
 * Returns whether layout deletes vertical spans of hidden shapes.
 */
public boolean getDeleteVerticalSpansOfHiddenShapes()  { return _deleteVerticalSpansOfHiddenShapes; }

/**
 * Sets whether layout deletes vertical spans of hidden shapes.
 */
public void setDeleteVerticalSpansOfHiddenShapes(boolean aValue)
{
    _deleteVerticalSpansOfHiddenShapes = aValue; // Set value
    if(aValue) setShiftShapesBelowHiddenShapesUp(false); // If setting true, turn off shift shapes
}

/**
 * Returns whether to shift shapes below hidden shapes up. 
 */
public boolean getShiftShapesBelowHiddenShapesUp()  { return _shiftShapesBelowHiddenShapesUp; }

/**
 * Sets whether to shift shapes below hidden shapes up. 
 */
public void setShiftShapesBelowHiddenShapesUp(boolean aValue)
{
    _shiftShapesBelowHiddenShapesUp = aValue; // Set value
    if(aValue) setDeleteVerticalSpansOfHiddenShapes(false); // If setting true, turn off delete spans
}

/**
 * The syncStructureWithShape() method sync's the column widths of receiver to those of given shape.
 */
public void syncStructureWithShape(RMParentShape aShape)
{
    // Register for repaint
    repaint();
    
    // Cache structured setting and set to false
    boolean s = _structured; _structured = false;
    
    // Real work
    if(aShape.getChildCount()>0 && aShape.getChildCount()==getChildCount()) {
        
        // Get shape children sorted by x
        List <RMShape> children = RMShapeUtils.getShapesSortedByFrameX(_children);
        List <RMShape> schildren = RMShapeUtils.getShapesSortedByFrameX(aShape.getChildren());

        // Iterate over children an align
        for(int i=0, iMax=children.size(); i<iMax; i++) { RMShape child = children.get(i), schild = schildren.get(i);
            child._x = schild._x; child.setWidth(schild._width); }
    }
    
    // Get the table row above and sync
    if(_syncStructureWithRowAbove && getRowAbove()!=null)
        getRowAbove().syncStructureWithShape(this);
    
    // Reset structured setting
    _structured = s;
}

/**
 * Returns the row above this one in the template.
 */
public RMTableRow getRowAbove()
{
    // Get index of this table row in parent table and return preceeding row
    int index = indexOf(); if(index<=0 || getTable()==null) return null;
    return getTable().getRow(index-1);
}

/**
 * Overrides standard shape method to turn off structuring if child isn't text.
 */
public void addChild(RMShape aChild, int anIndex)
{
    // If structured and trying to add something other than text, automatically turn off structuring
    if(_structured && !(aChild instanceof RMTextShape))
        setStructured(false);
    
    // Do normal add child
    super.addChild(aChild, Math.min(anIndex, getChildCount()));
}

/**
 * Overrides switch shape method to specify additional attributes to be transferred when setVersion is called.
 */
protected void transferAttributes(RMSwitchShape toShape)
{
    // Do normal transfer attributes
    super.transferAttributes(toShape);
    
    // Get toShape as table row and also do structured and move to bottom settings
    RMTableRow otherRow = (RMTableRow)toShape;
    otherRow._structured = _structured;
    otherRow._moveToBottom = _moveToBottom;
}

/** Editor method - states that children super-select immediately when table row is structured. */
public boolean childrenSuperSelectImmediately()  { return _structured; }

/**
 * Paints table row.
 */
protected void paintShape(Painter aPntr)
{
    // Do normal version (just return if not editing)
    super.paintShape(aPntr); if(!RMShapePaintProps.isEditing(aPntr) || !isStructured()) return;
    
    // Iterate over children sorted by X and draw divider lines
    aPntr.setColor(Color.DARKGRAY); aPntr.setStroke(Stroke.Stroke1); aPntr.setAntialiasing(false);    
    Rect bounds = getBoundsInside();
    List <RMShape> children = RMShapeUtils.getShapesSortedByFrameX(getChildren());
    for(RMShape child : children)
        aPntr.drawLine(child.getX(), bounds.y, child.getX(), bounds.getMaxY());
    aPntr.setAntialiasing(true);
}

/**
 * Returns clip shape for shape.
 */
public Shape getClipShape()  { return !isStructured()? getBoundsInside() : null; }

/**
 * Override to layout children and maybe sync with other rows.
 */
protected void layoutImpl()
{
    // If not structured or no children, just return
    if(!isStructured() || getChildCount()==0) return;
    
    // Layout children edge to edge by iterating over children and setting successive x values
    List <RMShape> children = getChildren(); double width = 0;
    for(RMShape child : children) {
        child.setBounds(width, 0, child.getWidth(), getHeight());
        width += child.getWidth();
    }
    
    // If total width doesn't equal parent width, divy up and add to each child by ratio of their current sizes
    double pwidth = getWidth();
    if(!MathUtils.equals(width,pwidth)) { double extra = pwidth - width, x = 0;
        for(RMShape child : children) {
            double ow = child.getWidth(), nw = ow + ow/width*extra;
            child.setX(x); child.setWidth(nw); x += nw;
        }
    }
    
    // Sync Structure With Alternates
    if(getSyncStructureWithAlternates() && getAlternates()!=null)
        for(RMTableRow alternate : (Collection<RMTableRow>)(Collection)getAlternates().values())
            alternate.syncStructureWithShape(this);

    // Sync Structure With Row Above
    if(getSyncStructureWithRowAbove() && getRowAbove()!=null)
        getRowAbove().syncStructureWithShape(this);
}

/**
 * Override to optimize structured case.
 */
protected double getPrefHeightImpl(double aWidth)
{
    // If not structured or no children, just return normal version
    if(!isStructured() || getChildCount()==0) return super.getPrefHeightImpl(aWidth);
    
    // Return max of current row height and max child best size
    double max = getHeight();
    for(RMShape child : getChildren()) max = Math.max(max, child.getPrefHeight());
    return max;
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and switch shape attributes and reset element name
    XMLElement e = super.toXMLShape(anArchiver);
    e.setName("tablerow");
    
    // Archive Title, Structured
    if(_title!=null) e.add("title", getTitle());
    if(!_structured) e.add("structured", false);
    
    // Archive SyncStructureWithRowAbove, SyncStructureWithAlternates
    if(_syncStructureWithRowAbove) e.add("sync-pars", true);
    if(_syncStructureWithAlternates) e.add("sync-alts", true);
        
    // Archive NumberOfChildrenToStayWith, ReprintWhenWrapped, PrintEvenIfNoObjectsInGroup, MoveToBottom
    if(_numberOfChildrenToStayWith!=1) e.add("stay-with", _numberOfChildrenToStayWith);
    if(!_reprintWhenWrapped) e.add("reprint", false);
    if(!_printEvenIfNoObjectsInGroup) e.add("print-always", false);
    if(_moveToBottom) e.add("move-to-bottom", true);
        
    // Archive MinSplitHeight, MinSplitRemainderHeight
    if(_minSplitHeight!=72) e.add("min-split", _minSplitHeight);
    if(_minSplitRemainderHeight!=72) e.add("min-remain", _minSplitRemainderHeight);
    
    // Archive PageBreakKey
    if(getPageBreakKey()!=null && getPageBreakKey().length()>0)
        e.add("page-break-key", getPageBreakKey());
    
    // Archive DeleteVerticalSpansOfHiddenShapes, ShiftShapesBelowHiddenShapesUp
    if(getDeleteVerticalSpansOfHiddenShapes())
        e.add("DeleteVerticalSpansOfHiddenShapes", getDeleteVerticalSpansOfHiddenShapes());
    if(getShiftShapesBelowHiddenShapesUp())
        e.add("ShiftShapesBelowHiddenShapesUp", getShiftShapesBelowHiddenShapesUp());
    
    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes and switch shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive Title, Structured
    _title = anElement.getAttributeValue("title");
    _structured = anElement.getAttributeBoolValue("structured", true);
    
    // Unarchive SyncStructureWithRowAbove, SyncStructureWithAlternates
    _syncStructureWithRowAbove = anElement.getAttributeBoolValue("sync-pars", false);
    _syncStructureWithAlternates = anElement.getAttributeBoolValue("sync-alts", false);
        
    // Unarchive NumberOfChildrenToStayWith, ReprintWhenWrapped, PrintEvenIfNoObjectsInGroup, MoveToBottom
    setNumberOfChildrenToStayWith(anElement.getAttributeIntValue("stay-with", 1));
    setReprintWhenWrapped(anElement.getAttributeBoolValue("reprint", true));
    setPrintEvenIfGroupIsEmpty(anElement.getAttributeBoolValue("print-always", true));
    setMoveToBottom(anElement.getAttributeBoolValue("move-to-bottom", false));
        
    // Unarchive MinSplitHeight, MinSplitRemainderHeight
    setMinSplitHeight(anElement.getAttributeFloatValue("min-split", 72));
    setMinSplitRemainderHeight(anElement.getAttributeFloatValue("min-remain", 72));
    
    // Unarchive PageBreakKey
    if(anElement.hasAttribute("page-break-key"))
        setPageBreakKey(anElement.getAttributeValue("page-break-key"));
    
    // Unarchive DeleteVerticalSpansOfHiddenShapes, ShiftShapesBelowHiddenShapesUp
    if(anElement.hasAttribute("DeleteVerticalSpansOfHiddenShapes"))
        setDeleteVerticalSpansOfHiddenShapes(anElement.getAttributeBoolValue("DeleteVerticalSpansOfHiddenShapes"));
    if(anElement.hasAttribute("ShiftShapesBelowHiddenShapesUp"))
        setShiftShapesBelowHiddenShapesUp(anElement.getAttributeBoolValue("ShiftShapesBelowHiddenShapesUp"));
    
    // Legacy: Remove layout attribute if present
    if(anElement.getElement("layout")!=null) { XMLElement lxml = anElement.removeElement("layout");
        if(lxml.hasAttribute("DeleteVerticalSpansOfHiddenShapes")) setDeleteVerticalSpansOfHiddenShapes(true);
        if(lxml.hasAttribute("ShiftShapesBelowHiddenShapesUp")) setShiftShapesBelowHiddenShapesUp(true); }
}

/**
 * Override to make sure structured children are sorted by X (wasn't necessarily so before RM14).
 */
protected void fromXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXMLChildren(anArchiver, anElement);
    if(isStructured()) RMShapeUtils.sortByX(_children);
}

/**
 * Returns a string description of the table row.
 */
public String toString()
{
    // Get string for table row + title, add text from each column and return string
	StringBuffer sb = new StringBuffer("RMTableRow(").append(getTitle()).append("): ");
    for(int i=0, iMax=getChildCount(); i<iMax; i++)
        if(getColumn(i)!=null) sb.append(getColumn(i).getText()).append(i+1<iMax? ", " : "");
    return sb.toString();
}

}