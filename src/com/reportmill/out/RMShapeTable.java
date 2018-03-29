/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Rect;

/**
 * This class creates a table from a given list of shapes.
 */
public class RMShapeTable {

    // The columns
    List <Double>  _cols = new ArrayList();
    
    // The rows
    List <Double>  _rows = new ArrayList();
    
    // The cells
    Cell           _cells[][];
    
    // Cells within this horizontal or vertical distance are considered to be aligned.
    static final double CELL_ALIGNMENT_TOLERANCE = .5;
    
/**
 * Returns the number of columns.
 */
public int getColCount()  { return _cols.size(); }

/**
 * Returns the individual column at index.
 */
public double getColWidth(int anIndex)  { return _cols.get(anIndex); }

/**
 * Returns the number of rows.
 */
public int getRowCount()  { return _rows.size(); }

/**
 * Returns the individual row at index.
 */
public double getRowHeight(int anIndex)  { return _rows.get(anIndex); }

/**
 * Returns the cell at index.
 */
public Cell getCell(int aRow, int aCol)  { return _cells[aRow][aCol]; }

/**
 * Takes a flattened list of shapes to build table.
 * topLevel is a common ancestor of all the shapes (usually the RMPage)
 * minTableRect, if non-null, specifies a minimum size and origin for the final table.
 */
public static RMShapeTable createTable(List <RMShape> theShapes, RMShape topLevel, Rect minTableRect)
{
    // Get number of shapes (just return if no shapes and no min-table rect)
    int shapeCount = theShapes.size(); if(shapeCount==0 && minTableRect==null) return null;
    
    int arraySizes = 2*shapeCount + (minTableRect!=null ? 2 : 0);
    double rowStarts[] = new double[arraySizes];
    double colStarts[] = new double[arraySizes];
    Rect cellRects[] = new Rect[shapeCount];
    Rect bounds, maxBounds = null;
    
    // Iterate over shapes: Fill row & column boundary arrays   (beware of roll/scale/skew)
    for(int i=0; i<shapeCount; i++) { RMShape shape = theShapes.get(i);
        
        // Get shape bounds
        if(shape instanceof RMTextShape)
            bounds = shape.localToParent(shape.getBoundsInside(),topLevel).getBounds();
        else bounds = shape.localToParent(shape.getBoundsMarked(), topLevel).getBounds();

        // Toss away cells whose size is less than the alignment tolerance.
        if(bounds.getWidth()<=CELL_ALIGNMENT_TOLERANCE || bounds.getHeight()<=CELL_ALIGNMENT_TOLERANCE) {
            cellRects[i] = null; continue; }
        
        rowStarts[2*i] = bounds.getY(); rowStarts[2*i+1] = bounds.getMaxY();
        colStarts[2*i] = bounds.getX(); colStarts[2*i+1] = bounds.getMaxX();
        cellRects[i] = bounds;
        
        // Bounds of final table is union of all the cell rects
        if(maxBounds==null) maxBounds = bounds.clone();
        else maxBounds.union(bounds);
    }
    
    // Add row/column boundaries to represent the full table bounds
    if(minTableRect != null) {
        rowStarts[2*shapeCount] = minTableRect.getY(); rowStarts[2*shapeCount+1] = minTableRect.getMaxY();
        colStarts[2*shapeCount] = minTableRect.getX(); colStarts[2*shapeCount+1] = minTableRect.getMaxX();
        if(maxBounds==null) maxBounds = minTableRect.clone();
        else maxBounds.union(minTableRect);
    }
    
    // Sort both arrays min->max
    Arrays.sort(rowStarts);
    Arrays.sort(colStarts);
    
    // Remove duplicates
    int numRowBoundaries = uniqueArray(rowStarts, CELL_ALIGNMENT_TOLERANCE);
    int numColBoundaries = uniqueArray(colStarts, CELL_ALIGNMENT_TOLERANCE);

    // If no rows or boundaries were defined, just return
    if(numRowBoundaries<2 || numColBoundaries<2)
        return null;
    
    int numRows = numRowBoundaries-1;
    int numCols = numColBoundaries-1;
    Cell cells[][] = new Cell[numRows][numCols];
    
    // Walk through converted rects and assign row-column spans
    for(int i=0; i<shapeCount; i++) {
        
        // skip any cells we've tossed earlier
        if(cellRects[i]==null) continue;
        
        // find origin row,column
        int col = binarySearch(colStarts, 0, numColBoundaries-1, cellRects[i].getX(), CELL_ALIGNMENT_TOLERANCE);
        int row = binarySearch(rowStarts, 0, numRowBoundaries-1, cellRects[i].getY(), CELL_ALIGNMENT_TOLERANCE);
        if(row<0 || col<0 || row>=numRows || col>=numCols)
            throw new RuntimeException("Internal Error : search failed");
        if(cells[row][col] != null) {
            printOverlapWarning(); continue; }
        
        // Create a new cell for shape and add to cells
        RMShape shape = theShapes.get(i);
        Cell newCell = new Cell(); newCell._cshape = shape;
        RMShape s = shape; while(s!=null && s.getFill()==null) s = s.getParent();
        if(s!=null) newCell.setFill(s.getFill());
        cells[row][col] = newCell;
        
        // Declare variables for row span & column span
        int rowspan, colspan;
        
        // Find the rowspan
        for(rowspan=1; row+rowspan<numRowBoundaries; ++rowspan) {
            if(Math.abs(rowStarts[row+rowspan] - cellRects[i].getMaxY()) <= CELL_ALIGNMENT_TOLERANCE)
                break;
            if(row+rowspan==numRows) // Another option is to make loop only go to numRows and check (or not) once at end
                throw new RuntimeException("Internal error: couldn't find last row boundary");
            if(cells[row+rowspan][col]!=null) printOverlapWarning();
            else cells[row+rowspan][col] = newCell;
        }
        
        // Find column span
        for(colspan=1; col+colspan<numColBoundaries; ++colspan) {
            if(Math.abs(colStarts[col+colspan] - cellRects[i].getMaxX()) <= CELL_ALIGNMENT_TOLERANCE)
                break;
            if(col+colspan == numCols)
                throw new RuntimeException("Internal error: couldn't find last column boundary");
            for(int r = 0; r < rowspan; ++r) {
                if(cells[row+r][col+colspan] != null) printOverlapWarning();
                else cells[row+r][col+colspan] = newCell;
            }
        }
        
        newCell.setVals(row, col, rowspan, colspan);
        
        // Set the cell's frame and add it to the table
        newCell.setFrame(colStarts[col] - maxBounds.getX(), rowStarts[row] - maxBounds.getY(),
            colStarts[col+colspan]-colStarts[col], rowStarts[row+rowspan]-rowStarts[row]);
    }
    
    // Set bounds of new table to cover all the cells
    RMShapeTable stable = new RMShapeTable(); //newTable.setFrame(maxBounds);
    stable._cells = cells;
    
    // Create rows & columns.
    for(int i=0; i<numRows; ++i) stable._rows.add(rowStarts[i+1]-rowStarts[i]);
    for(int i=0; i<numCols; ++i) stable._cols.add(colStarts[i+1]-colStarts[i]);
    
    // Create empty cells for any area that not covered by a real cell
    fillInCells(cells, rowStarts, colStarts, maxBounds);
    _pow = false;
    
    // Return table
    return stable;
}

/**
 * Removes any duplicate entries in sorted array.
 * Elements are considered to be duplicates if they are within the specified tolerance of each other.
 * Returns the number of unique entries in the array.
 */
static int uniqueArray(double array[], double tolerance)
{
    int n = array.length; if(n==0) return 0;
    int last_unique_entry = 0;
    for(int i=1; i<n; i++)
        if(Math.abs(array[i]-array[last_unique_entry])>tolerance)
            array[++last_unique_entry] = array[i];
    return last_unique_entry+1;
}

/** 
 * Like the Arrays.binarySearch(), but allows you to specify a starting range 
 * in the array as well as a floating-point tolerance for equality comparisons.
 */
static int binarySearch(double array[], int first, int last, double value, double tolerance)
{
    // Iterate while first is less than last    
    while(first<=last) {
        int middle = (first+last)/2;
        if(Math.abs(array[middle]-value) <= tolerance) return middle;
        else if (value<array[middle]) last = middle-1;
        else first = middle+1;
    }
    
    // Return -1 since value not found
    return -1;
}

/**
 * Creates RMCells for any empty (null) cells.
 * Tries to coalesce neighboring empty cells into rectangular regions.
 */
static void fillInCells(Cell cells[][], double rowStarts[], double colStarts[], Rect maxBounds)
{
    // Iterate over cells
    for(int row=0, rowCount=cells.length; row<rowCount; row++) {
        for(int col=0, colCount=cells[row].length; col<colCount; col++) {
            
            // Find a horizontal span of 1 or more null cells and fill them with a new cell
            if(cells[row][col]==null) {
                
                Cell newCell = new Cell();
                
                // Find colspan
                int colspan = 0;
                do {
                    cells[row][col+colspan] = newCell; ++colspan;
                } while((col+colspan<colCount) && (cells[row][col+colspan]==null));
                
                // Cell now spans 1 row and colspan cols, extend rowspan if cells col->col+colspan below also empty
                int rowspan = 1, c;
                while((row+rowspan<rowCount) && (cells[row+rowspan][col]==null)) {
                    
                    // 
                    for(c=0; c<colspan; ++c)
                        if(cells[row+rowspan][col+c]!=null)
                            break;
                    
                    // found matching empty region.  Fill in the array
                    if(c==colspan) {
                        for(c=0; c<colspan; ++c)
                            cells[row+rowspan][col+c] = newCell;
                        ++rowspan;
                    }
                    else break;
                }
                
                newCell.setVals(row, col, rowspan, colspan);
                
                // Set the cell's bounds
                newCell.setFrame(colStarts[col] - maxBounds.getX(), rowStarts[row] - maxBounds.getY(),
                    colStarts[col+colspan]-colStarts[col], rowStarts[row+rowspan]-rowStarts[row]);
                
                // Make it invisible
                newCell.setVisible(false);
                
                // Skip over ones we've just filled in
                col += colspan-1;
                
                // If we've filled in multiple entire rows, skip them now, too.
                if(col==0 && colspan==colCount) 
                    row += rowspan-1;
            }
        }
    }
}

/** Prints overlap warning once. */
static void printOverlapWarning()  { if(!_pow) System.out.println("Warning: overlapping shapes"); _pow = true; }
static boolean _pow; 

/**
 * A class for cells.
 */
public static class Cell extends RMShape {
    
    // Ivars
    int _row, _col, _rspan, _cspan;
    
    /** Returns the row index. */
    public int getRow()  { return _row; }
    
    /** Returns the col index. */
    public int getColumn()  { return _col; }
    
    /** Returns the row span. */
    public int getRowSpan()  { return _rspan; }
    
    /** Returns the col span. */
    public int getColumnSpan()  { return _cspan; }
    
    /** Returns the shape. */
    public RMShape getCellShape()  { return _cshape; } RMShape _cshape;
    
    /** Sets values for cell. */
    void setVals(int row, int col, int rspan, int cspan)  { _row = row; _col = col; _rspan = rspan; _cspan = cspan; }
}

}