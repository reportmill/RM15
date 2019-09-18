/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;

/**
 * This class represents a line that divedes a cell row or cell column.
 */
public class RMCrossTabDivider extends RMLineShape {

    // The row or column this divider is associated with
    RMCrossTabSpan  _span;
    
    // The starting row index (if column divider) or column index (if row divider) for this divider
    int             _start;
    
    // The ending row index (if column divider) or column index (if row divider) for this divider
    int             _end;

/**
 * Creates a new divider for given row or column.
 */
public RMCrossTabDivider(RMCrossTabSpan aSpan)  { _span = aSpan; }

/**
 * Returns whether divider is a row divider.
 */
public boolean isRowDivider()  { return _span instanceof RMCrossTabRow; }

/**
 * Returns whether divider is a column divider. 
 */
public boolean isColDivider()  { return _span instanceof RMCrossTabCol; }

/**
 * Returns the divder row as a cell row.
 */
public RMCrossTabRow getRow()  { return (RMCrossTabRow)_span; }

/**
 * Returns the divider column as cell column.
 */
public RMCrossTabCol getCol()  { return (RMCrossTabCol)_span; }

/**
 * Returns the table this divider is a part of.
 */
public RMCrossTab getTable()  { return isRowDivider()? getRow()._table : getCol()._table; }

/**
 * Returns the divider row to the right of the divider.
 */
public RMCrossTabRow getNextRow()
{
    // Iterate over rows and return the one after this one
    RMCrossTab table = getTable();
    for(int i=0, iMax=table.getRowCount(); i<iMax; i++)
        if(table.getRow(i)==getRow() && i+1<iMax)
            return table.getRow(i+1);
    return null; // Return null if no row after this divider
}

/**
 * Returns the divider column below the divider.
 */
public RMCrossTabCol getNextCol()
{
    // Iterate over columns and return the one after this divider
    RMCrossTab table = getTable();
    for(int i=0, iMax=table.getColCount(); i<iMax; i++)
        if(table.getCol(i)==getCol() && i+1<iMax)
            return table.getCol(i+1);
    return null; // Return null if no column after this divider
}

/**
 * Returns the starting row index (if column divider) or column index (if row divider) for this divider.
 */
public int getStart()  { return _start; }

/**
 * Returns the ending row index (if column divider) or column index (if row divider) for this divider.
 */
public int getEnd()  { return _end; }

/**
 * Returns the length of the divider in terms of rows (or columns).
 */
public int getLength()  { return _end - _start; }

}