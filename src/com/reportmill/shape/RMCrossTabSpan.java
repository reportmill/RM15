/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;

/**
 * Base class for RMCrossTabCol and RMCrossTabRow.
 */
public class RMCrossTabSpan implements Cloneable {

    // The table that owns this span
    RMCrossTab                _table;
    
    // The span cells
    List <RMCrossTabCell>     _cells = new ArrayList();
    
    // The dividers between this span and the next
    List <RMCrossTabDivider>  _dividers = new ArrayList();

    // The divider cache to store dividers when rebuilding
    List <RMCrossTabDivider>  _divPool = new ArrayList();

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
 * Returns the divider count.
 */
public int getDividerCount()  { return getDividers().size(); }

/**
 * Returns the specific divider at given index. 
 */
public RMCrossTabDivider getDivider(int anIndex)  { return getDividers().get(anIndex); }

/**
 * Returns the dividers for this span.
 */
public List <RMCrossTabDivider> getDividers()  { return _dividers; }
    
/**
 * Resets dividers so they will be recalculated.
 */
public void resetDividers()  { _divPool.addAll(_dividers); _dividers.clear(); }

/**
 * Returns an available divider.
 */
protected RMCrossTabDivider getDividerFromPool()
{
    RMCrossTabDivider div = _divPool.size()>0? _divPool.remove(0) : new RMCrossTabDivider(this);
    if(_table.getStroke()!=null) div.setStroke(_table.getStroke());
    return div;
}

/**
 * Returns a basic clone of this object.
 */
public RMCrossTabSpan clone()
{
    RMCrossTabSpan clone = null;
    try { clone = (RMCrossTabSpan)super.clone(); } catch(CloneNotSupportedException e) { }
    clone._cells = new ArrayList();
    clone._dividers = new ArrayList();
    clone._divPool = new ArrayList();
    return clone;
}
  
}