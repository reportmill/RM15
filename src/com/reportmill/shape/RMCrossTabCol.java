/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.*;

/**
 * Provides info for a column in a crosstab.
 */
public class RMCrossTabCol extends RMCrossTabSpan implements XMLArchiver.Archivable {

    // The width of the column
    double                     _width = 120;

/**
 * Returns the index of this column in the table.
 */
public int getIndex()
{
    for(int i=0, iMax=_table.getColCount(); i<iMax; i++) if(_table.getCol(i)==this) return i;
    return -1;
}

/**
 * Returns the x of this column.
 */
public double getX()
{
    double x = 0; for(int i=0, iMax=getIndex(); i<iMax; i++) x += _table.getCol(i).getWidth();
    return x;
}

/**
 * Returns the width of this column.
 */
public double getWidth()  { return _width; }

/**
 * Sets the width of this column.
 */
public void setWidth(double aWidth)
{
    // If already set, just return
    if(aWidth==_width) return;
    
    // Calculate change and set new value
    double dw = aWidth - _width; _width = aWidth;
    
    // Set new table width
    if(_table!=null) _table.setWidth(_table.getWidth() + dw);
    
    // Set cell widths
    for(int i=0, iMax=getCellCount(); i<iMax; i++)
        getCell(i).setWidth(aWidth);
}

/**
 * Returns the max x of this column.
 */
public double getMaxX()  { return getX() + getWidth(); }

/**
 * Returns the best height for the row.
 */
public double getBestWidth()
{
    double pw = getWidth();
    for(int i=0, iMax=getCellCount(); i<iMax; i++) { RMCrossTabCell cell = getCell(i);
        if(cell.getColSpan()>1) continue; pw = Math.max(pw, cell.getBestWidth()); }
    return pw;
}

/**
 * Returns the dividers for this column.
 */
public List <RMCrossTabDivider> getDividers()
{
    // If dividers already loaded, return dividers
    if(_dividers.size()>0) return _dividers;
    
    // Get/create first divider, init Bounds and Start and add
    int column = getIndex();
    RMCrossTabDivider divider = getDividerFromPool();
    divider.setBounds(Math.round(getMaxX()), 0, 0, _table.getHeight()); divider._start = 0;
    _dividers.add(divider);
    
    // Iterate over rows to see if additional dividers need to be added
    for(int i=0, iMax=_table.getRowCount(); i<iMax; i++) {
        
        // Get cell at row/col (can be null for phantom column) and NextCell
        RMCrossTabCell cell = column>=0? _table.getCell(i, column) : null;
        RMCrossTabCell nextCell = column+1<_table.getColCount()? _table.getCell(i, column+1) : null;
        
        // Draw border if first cell shows right border or second cell shows left border
        boolean showBorder = false;
        if(cell!=null && cell.isVisible() && cell.isShowRightBorder()) showBorder = true;
        else if(nextCell!=null && nextCell.isVisible() && nextCell.isShowLeftBorder()) showBorder = true;
        
        // Get cell y
        double cellY = cell!=null? cell.getY() : nextCell.getY();
        
        // If row cell crosses divider, end divider
        if((cell!=null && cell.getColEnd()!=column) || !showBorder) {
            
            // If divider in progress, set truncated end
            if(divider!=null) {
                divider.setHeight(cellY - divider.getY()); divider._end = i; }
            
            // Stop divider in progress
            divider = null;
        }
        
        // If row cell doesn't cross divider, continue (or start) divider
        else {
            
            // If divider is null, get/create a new one
            if(divider==null) {
                divider = getDividerFromPool();
                divider.setBounds(getMaxX(), cellY, 0, _table.getHeight() - cellY);
                _dividers.add(divider); divider._start = i;
            }
        }
    }
    
    // If last divider didn't end prematurely, set its end index and return dividers
    if(divider!=null) divider._end = _table.getRowCount();
    return _dividers;
}

/**
 * Returns a basic clone of this object.
 */
public RMCrossTabCol clone()  { return (RMCrossTabCol)super.clone(); }
  
/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = new XMLElement("column"); e.add("width", getWidth()); return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    _width = anElement.getAttributeFloatValue("width"); return this;
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "RMCrossTabCol: width=" + getWidth() + ", cells=" + _cells.toString(); }

}