/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.*;

/**
 * This shape manages a row of cells.
 */
public class RMCrossTabRow implements Cloneable, XMLArchiver.Archivable {

    // The table this row is associated with
    RMCrossTab                _table;
    
    // The table row height
    double                    _height = 25;
    
    // The row cells
    List <RMCrossTabCell>     _cells = new ArrayList();

    // The dividers between this column and the next
    List <RMCrossTabDivider>  _dividers = new ArrayList(), _dvdrPool = new ArrayList();

/**
 * Returns the index of this row in the table.
 */
public int getIndex()
{
    for(int i=0, iMax=_table.getRowCount(); i<iMax; i++) if(_table.getRow(i)==this) return i;
    return -1; // Return -1 since row not found
}

/**
 * Returns the number of cells in this row.
 */
public int getCellCount()  { return _cells.size(); }

/**
 * Returns the specific child cell at the given index in the list of unique cells for this row.
 */
public RMCrossTabCell getCell(int anIndex)  { return _cells.get(anIndex); }

/**
 * Sets the cell at given index.
 */
protected void setCell(RMCrossTabCell aCell, int anIndex)
{
    while(anIndex>=_cells.size()) _cells.add(null);
    _cells.set(anIndex, aCell);
}

/**
 * Adds a cell to end of row.
 */
public void addCell(RMCrossTabCell aCell)  { addCell(aCell, getCellCount()); }

/**
 * Adds a cell at given index.
 */
public void addCell(RMCrossTabCell aCell, int anIndex)  { _cells.add(anIndex, aCell); }

/**
 * Removes a cell at given index.
 */
public RMCrossTabCell removeCell(int anIndex)  { return _cells.remove(anIndex); }

/**
 * Returns the row y.
 */
public double getY()
{
    double y = 0; for(int i=0, iMax=getIndex(); i<iMax; i++) y += _table.getRow(i).getHeight();
    return (float)y;
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
    double dh = aHeight - _height; _height = aHeight;
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
 * Returns the divider count.
 */
public int getDividerCount()  { return getDividers().size(); }

/**
 * Returns the specific divider at given index. 
 */
public RMCrossTabDivider getDivider(int anIndex)  { return getDividers().get(anIndex); }

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
        if(cell!=null && cell.isVisible() && cell.getShowBottomBorder()) showBorder = true;
        else if(nextCell!=null && nextCell.isVisible() && nextCell.getShowTopBorder()) showBorder = true;
        
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
                divider._start = i; _dividers.add(divider);
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
 * Resets dividers so they will be recalculated.
 */
public void resetDividers()  { _dvdrPool.addAll(_dividers); _dividers.clear(); }

/**
 * Returns a divider from the pool of recently used dividers (or creates a new one).
 */
RMCrossTabDivider getDividerFromPool() { return _dvdrPool.size()>0? _dvdrPool.remove(0) : new RMCrossTabDivider(this); }

/**
 * Returns a basic clone of this object.
 */
public RMCrossTabRow clone()
{
    RMCrossTabRow clone = null;
    try { clone = (RMCrossTabRow)super.clone(); } catch(CloneNotSupportedException e) { }
    clone._cells = new ArrayList();
    clone._dividers = new ArrayList(); clone._dvdrPool = new ArrayList(); // Set new Dividers/DividerPool lists
    return clone;
}

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
public String toString()
{
    return getClass().getSimpleName() + ": height=" + getHeight() + ", cells=" + _cells.toString();
}

}