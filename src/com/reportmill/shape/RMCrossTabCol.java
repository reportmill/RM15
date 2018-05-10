/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.*;

/**
 * Provides info for a column in a crosstab.
 */
public class RMCrossTabCol implements Cloneable, XMLArchiver.Archivable {

    // The table that owns this column
    RMCrossTab                _table;

    // The width of the column
    double                    _width = 120;
    
    // The row cells
    List <RMCrossTabCell>     _cells = new ArrayList();
    
    // The dividers between this column and the next
    List <RMCrossTabDivider>  _dividers = new ArrayList(), _dvdrPool = new ArrayList();

/**
 * Returns the index of this column in the table.
 */
public int getIndex()
{
    for(int i=0, iMax=_table.getColCount(); i<iMax; i++) if(_table.getCol(i)==this) return i;
    return -1; // Return -1 since column not found
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
 * Adds a cell at end of column.
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
 * Returns the x of this column.
 */
public float getX()
{
    double x = 0; for(int i=0, iMax=getIndex(); i<iMax; i++) x += _table.getCol(i).getWidth();
    return (float)x;
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
    if(aWidth==_width) return;
    double dw = aWidth - _width; _width = aWidth;
    if(_table!=null) _table.setWidth(_table.getWidth() + dw);
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
                divider = getDividerFromPool(); // Get/create new divider
                divider.setBounds(getMaxX(), cellY, 0, _table.getHeight() - cellY); // Init divider bounds
                divider._start = i; _dividers.add(divider); // Initialize divider start row and add to list
            }
        }
    }
    
    // If last divider didn't end prematurely, set its end index and return dividers
    if(divider!=null) divider._end = _table.getRowCount();
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
public RMCrossTabCol clone()
{
    RMCrossTabCol clone = null;
    try { clone = (RMCrossTabCol)super.clone(); } catch(CloneNotSupportedException e) { }
    clone._cells = new ArrayList();
    clone._dividers = new ArrayList(); clone._dvdrPool = new ArrayList(); // Set new Dividers/DividerPool lists
    return clone;
}
  
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

}