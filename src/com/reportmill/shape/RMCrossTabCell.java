/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMGroup;
import com.reportmill.base.RMGrouping;
import snap.gfx.*;
import snap.util.*;

/**
 * A text shape subclass used to form the basis of a table.
 */
public class RMCrossTabCell extends RMTextShape {

    // The row this cell originates in
    int           _row = -1;
    
    // The column this cell originates in
    int           _col = -1;

    // The number of rows this cell spans
    int           _rowSpan = 1;

    // The number of columns this cell spans
    int           _colSpan = 1;
    
    // Whether cell shows left border
    boolean       _showLeftBorder = true;
    
    // Whether cell shows right border
    boolean       _showRightBorder = true;
    
    // Whether cell shows top border
    boolean       _showTopBorder = true;
    
    // Whether cell shows bottom border
    boolean       _showBottomBorder = true;
    
    // The grouping for this cell
    RMGrouping    _grouping;
    
    // The group for this cell (during RPG)
    RMGroup       _group;
    
    // The default cell text margin 
    static Insets _defaultMargin = new Insets(5);

/**
 * Creates a new RMCrossTabCell.
 */
public RMCrossTabCell()
{
    setAlignmentY(AlignY.Middle);
    getRichText().setDefaultLineStyle(TextLineStyle.DEFAULT_CENTERED);
}

/**
 * Returns the table for this cell.
 */
public RMCrossTab getTable()  { return (RMCrossTab)getParent(); }

/**
 * Returns the row of this cell.
 */
public int getRow()  { return _row; }

/**
 * Returns the column of this cell.
 */
public int getCol()  { return _col; }

/**
 * Returns the number of columns this cell spans.
 */
public int getColSpan()  { return _colSpan; }

/**
 * Returns the number of rows this cell spans.
 */
public int getRowSpan()  { return _rowSpan; }

/**
 * Returns the end row of this cell.
 */
public int getRowEnd()  { return _row + _rowSpan - 1; }

/**
 * Returns whether cell shows left border.
 */
public boolean isShowLeftBorder()  { return _showLeftBorder; }

/**
 * Sets whether cell shows left border.
 */
public void setShowLeftBorder(boolean aFlag)  { _showLeftBorder = aFlag; }

/**
 * Returns whether cell shows right border.
 */
public boolean isShowRightBorder()  { return _showRightBorder; }

/**
 * Sets whether cell shows right border.
 */
public void setShowRightBorder(boolean aFlag)  { _showRightBorder = aFlag; }

/**
 * Returns whether cell shows top border.
 */
public boolean isShowTopBorder()  { return _showTopBorder; }

/**
 * Sets whether cell shows top border.
 */
public void setShowTopBorder(boolean aFlag)  { _showTopBorder = aFlag; }

/**
 * Returns whether cell shows bottom border.
 */
public boolean isShowBottomBorder()  { return _showBottomBorder; }

/**
 * Sets whether cell shows bottom border.
 */
public void setShowBottomBorder(boolean aFlag)  { _showBottomBorder = aFlag; }

/** 
 * Convenience method to set all borders at once
 */
public void setShowBorders(boolean left, boolean right, boolean top, boolean bottom)
{
    setShowLeftBorder(left); setShowRightBorder(right); setShowTopBorder(top); setShowBottomBorder(bottom);
}

/**
 * Returns the grouping.
 */
public RMGrouping getGrouping()  { return _grouping; }

/**
 * Sets the grouping.
 */
public void setGrouping(RMGrouping aGrouping)  { _grouping = aGrouping; }

/**
 * Returns the group for this cell.
 */
public RMGroup getGroup()  { return _group; }

/**
 * Sets the group for this cell.
 */
protected void setGroup(RMGroup aGroup)  { _group = aGroup; }

/**
 * Returns the end column of this cell.
 */
public int getColEnd()  { return _col + _colSpan -1; }

/**
 * Returns the cell before, or to the left of, this cell.
 */
public RMCrossTabCell getCellBefore()
{
    // Get cell row and cell previous column
    int row = getRow();
    int col = getCol() - 1;
    
    // If column is beyond bounds, decrement row, reset column
    if(col<0) {
        row--; // Decrement row
        if(row<0) row = getTable().getRowCount() - 1; // If row is beyond bounds, reset row to last row
        col = getTable().getColCount() - 1; // Reset column
    }
    
    // Return cell at previous row/col
    return getTable().getCell(row, col);
}

/**
 * Returns the cell after, or to the right of, this cell.
 */
public RMCrossTabCell getCellAfter()
{
    // Get cell row and next column
    int row = getRow();
    int col = getColEnd() + 1;
    
    // If column is beyond column bounds, increment row, reset column
    if(col>=getTable().getColCount()) {
        row++; // Increment row
        if(row>=getTable().getRowCount()) row = 0; // If row is beyond row bounds, reset row
        col = 0; // Reset column
    }
    
    // Return cell at next row/col
    return getTable().getCell(row, col);
}

/**
 * Returns the cell just above this cell.
 */
public RMCrossTabCell getCellAbove()
{
    int row = getRow() - 1; // Get cell row above
    if(row<0) row = getTable().getRowCount() - 1; // If row is less than zero, reset to last row
    return getTable().getCell(row, getCol()); // Return cell at row & column
}

/**
 * Returns the cell just below this cell.
 */
public RMCrossTabCell getCellBelow()
{
    int row = getRowEnd() + 1; // Get cell row below - i
    if(row>=getTable().getRowCount()) row = 0; // If row is beyond row count, reset to first row
    return getTable().getCell(row, getCol()); // Return cell at row & column
}

/**
 * Returns whether this cell is a column header cell.
 */
public boolean isColHeader()
{
    return getRow()<getTable().getHeaderRowCount() && getCol()>=getTable().getHeaderColCount();
}

/**
 * Returns whether this cell is a row header cell.
 */
public boolean isRowHeader()
{
    return getCol()<getTable().getHeaderColCount() && getRow()>=getTable().getHeaderRowCount();
}

/**
 * Returns the default text margin for RMCell (top=5, left=5, right=5, bottom=5).
 */
public Insets getMarginDefault()  { return _defaultMargin; }

/**
 * Clears the text and grouping from this cell.
 */
public void clearContents()  { setText(""); setGrouping(null); }

/**
 * Handles painting a crosstab cell.
 */
protected void paintShape(Painter aPntr)
{
    // Draw normal text
    super.paintShape(aPntr);
    
    // If cell has grouping, paint corner grouping indicator
    RMShapePaintProps props = RMShapePaintProps.get(aPntr);
    if(props.isEditing() && (isRowHeader() || isColHeader())) {
        double x = Math.round(getMaxX()) - getX();
        double y = Math.round(getY()) - getY();
        aPntr.setColor(Color.BLACK);
        aPntr.setFont(new Font("Arial", 8));
        aPntr.drawString(getGrouping()!=null? "G" : "H", x-8, y+8);
    }
}

/**
 * Standard clone implementation.
 */
public RMCrossTabCell clone()
{
    RMCrossTabCell clone = (RMCrossTabCell)super.clone();
    if(_grouping!=null) clone._grouping = _grouping.clone();
    return clone;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXML(anArchiver); e.setName("cell");
    
    // Archive ColumnSpan, RowSpan, LeftBorder, RightBorder, TopBorder, BottomBorder
    if(getColSpan()>1) e.add("column-span", getColSpan());
    if(getRowSpan()>1) e.add("row-span", getRowSpan());
    if(!isShowLeftBorder()) e.add("show-left-border", false);
    if(!isShowRightBorder()) e.add("show-right-border", false);
    if(!isShowTopBorder()) e.add("show-top-border", false);
    if(!isShowBottomBorder()) e.add("show-bottom-border", false);

    // Archive grouping
    if(getGrouping()!=null) e.add(getGrouping().toXML(anArchiver));
    
    // Remove unnecessary bounds attributes
    e.removeAttribute("x"); e.removeAttribute("y");
    e.removeAttribute("width"); e.removeAttribute("height");
    
    // Override RMText vertical alignment: if is RMCell's default, ALIGN_MIDDLE, remove attribute
    if(getAlignmentY()==AlignY.Middle) e.removeAttribute("valign");
        
    // Override RMText vertical alignment: if is RMText's default, ALIGN_TOP, add attribute
    else if(getAlignmentY()==AlignY.Top)
        e.add("valign", getAlignmentY().toString().toLowerCase());
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXML(anArchiver, anElement);
    
    // Unarchive ColumnSpan, RowSpan, LeftBorder, RightBorder, TopBorder, BottomBorder
    _colSpan = anElement.getAttributeIntValue("column-span", 1);
    _rowSpan = anElement.getAttributeIntValue("row-span", 1);
    if(anElement.hasAttribute("show-left-border"))
        _showLeftBorder = anElement.getAttributeBoolValue("show-left-border");
    if(anElement.hasAttribute("show-right-border"))
        _showRightBorder = anElement.getAttributeBoolValue("show-right-border");
    if(anElement.hasAttribute("show-top-border"))
        _showTopBorder = anElement.getAttributeBoolValue("show-top-border");
    if(anElement.hasAttribute("show-bottom-border"))
        _showBottomBorder = anElement.getAttributeBoolValue("show-bottom-border");
    
    // Unarchive grouping
    if(anElement.get("grouping")!=null)
        _grouping = anArchiver.fromXML(anElement.get("grouping"), RMGrouping.class, null);
    
    // Return this cell
    return this;
}
    
}