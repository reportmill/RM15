/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.*;

/**
 * This shape manages a row of cells.
 */
public class RMCrossTabRow extends RMCrossTabSpan implements XMLArchiver.Archivable {

    // The table row height
    double                    _height = 25;
    
/**
 * Returns the index of this row in the table.
 */
public int getIndex()
{
    for(int i=0, iMax=_table.getRowCount(); i<iMax; i++) if(_table.getRow(i)==this) return i;
    return -1;
}

/**
 * Returns the row y.
 */
public double getY()
{
    double y = 0; for(int i=0, iMax=getIndex(); i<iMax; i++) y += _table.getRow(i).getHeight();
    return y;
}

/**
 * Returns the row height.
 */
public double getHeight()  { return _height; }

/**
 * Sets the row height.
 */
public void setHeight(double aHeight)
{
    // If already set, just return
    //if(aHeight==_height) return;
    
    // Calculate change and set new value
    double dh = aHeight - _height; _height = aHeight;
    
    // Set new table height
    if(_table!=null) _table.setHeight(_table.getHeight() + dh);
}

/**
 * Returns the row max y.
 */
public double getMaxY()  { return getY() + getHeight(); }

/**
 * Returns the best height for the row.
 */
public double getBestHeight()
{
    double ph = getHeight();
    for(int i=0, iMax=getCellCount(); i<iMax; i++) { RMCrossTabCell cell = getCell(i); if(cell.getRowSpan()>1) continue;
        ph = Math.max(ph, cell.getBestHeight()); }
    return ph;
}

/**
 * Returns the dividers for this column.
 */
public List <RMCrossTabDivider> getDividers()
{
    // If dividers already loaded, return dividers
    if(_dividers.size()>0) return _dividers;
    
    // Get/create first divider, init Bounds and Start and add
    int row = getIndex();
    RMCrossTabDivider divider = getDividerFromPool();
    divider.setBounds(0, Math.round(getMaxY()), _table.getWidth(), 0); divider._start = 0;
    _dividers.add(divider);
    
    // Iterate over columns to see if additional dividers need to be added
    for(int i=0, iMax=_table.getColCount(); i<iMax; i++) {
        
        // Get cell at row/col (can be null for phantom top border row) and NextCell
        RMCrossTabCell cell = row>=0? _table.getCell(row, i) : null;
        RMCrossTabCell nextCell = row+1<_table.getRowCount()? _table.getCell(row+1, i) : null;
        
        // Draw border if first cell shows bottom border or second cell shows top border
        boolean showBorder = false;
        if(cell!=null && cell.isVisible() && cell.isShowBottomBorder()) showBorder = true;
        else if(nextCell!=null && nextCell.isVisible() && nextCell.isShowTopBorder()) showBorder = true;
        
        // Get cell x
        double cellX = cell!=null? cell.getX() : nextCell.getX();
        
        // If column cell crosses divider, end divider
        if((cell!=null && cell.getRowEnd()!=row) || !showBorder) {
            
            // If divider in progress, set truncated end
            if(divider!=null) {
                divider.setWidth(cellX - divider.getX()); divider._end = i; }
            
            // Stop divider in progress
            divider = null;
        }
        
        // If column cell doesn't cross divider, continue (or start) divider
        else {
            
            // If divider is null, get/create a new one
            if(divider==null) {
                divider = getDividerFromPool();
                divider.setBounds(cellX, getMaxY(), _table.getWidth() - cellX, 0);
                _dividers.add(divider); divider._start = i;
            }
        }
    }
    
    // If last divider didn't end prematurely, set its end index
    if(divider!=null)
        divider._end = _table.getColCount();
    
    // Clear divider pool and return dividers
    return _dividers;
}

/**
 * Returns a basic clone of this object.
 */
public RMCrossTabRow clone()  { return (RMCrossTabRow)super.clone(); }

/**
 * Returns a clone of row including a clone of cells.
 */
public RMCrossTabRow cloneDeep()
{
    // Do normal clone
    RMCrossTabRow clone = clone();
    
    // Clone cells (or re-use, if span is more than 1)
    for(int i=0, iMax=getCellCount(); i<iMax; i++) { RMCrossTabCell cell = getCell(i);
        RMCrossTabCell cloneCell = cell.getCol()==i? cell.clone() : clone.getCell(i-1);
        clone.addCell(cloneCell);
    }
    
    // Return clone
    return clone;
}
  
/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = new XMLElement("row"); e.add("height", getHeight()); return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    _height = anElement.getAttributeFloatValue("height"); // Unarchive width
    return this;
}

/**
 * Standard toString implementation.
 */
public String toString()  { return "RMCrossTabRow: height=" + getHeight() + ", cells=" + _cells.toString(); }

}