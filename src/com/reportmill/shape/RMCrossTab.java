/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import java.util.*;
import snap.util.*;

/**
 * Manages a table of RMCells, which is really a stack of RMCellRows.
 */
public class RMCrossTab extends RMParentShape {

    // The dataset key for this table
    String                   _datasetKey;

    // An optional key chain expression string used to prune the table list derived from dataset key
    String                   _filterKey;

    // A list of columns
    List <RMCrossTabCol>     _cols = new ArrayList();

    // A list of rows
    List <RMCrossTabRow>     _rows = new ArrayList();

    // The number of header rows for table
    int                      _headerRowCount;
    
    // The number of header columns for table
    int                      _headerColCount;
    
    // Bogus Row/Column for top border dividers and left border dividers
    RMCrossTabRow            _topBrdrRow = new RMCrossTabRow();
    RMCrossTabCol            _leftBrdrCol = new RMCrossTabCol();
    
    // The color of the header row and column
    static RMColor HEADER_ROW_COLOR = new RMColor(.75f);
    static RMColor HEADER_COLUMN_COLOR = new RMColor(.867f);

/**
 * Creates a new RMCrossTab.
 */
public RMCrossTab()
{
    _topBrdrRow._table = _leftBrdrCol._table = this; _topBrdrRow._height = 0; _leftBrdrCol._width = 0;
}

/**
 * Returns the dataset key.
 */
public String getDatasetKey()  { return _datasetKey; }

/**
 * Sets the dataset key.
 */
public void setDatasetKey(String aKey)  { _datasetKey = aKey; }

/**
 * Returns the optional key chain expression string used to prune the table list derived from dataset key.
 */
public String getFilterKey()  { return _filterKey; }

/**
 * Sets the optional key chain expression string used to prune the table list derived from dataset key.
 */
public void setFilterKey(String aKeyExpr)  { _filterKey = aKeyExpr; }

/**
 * Returns the number of rows.
 */
public int getRowCount()  { return _rows.size(); }

/**
 * Sets the number of rows.
 */
public void setRowCount(int aRowCount)
{
    while(aRowCount>getRowCount()) addRow(); // Add rows if needed
    while(aRowCount<getRowCount()) removeRow(getRowCount()-1); // Remove rows if needed
}

/**
 * Returns the specific row at the given index.
 */
public RMCrossTabRow getRow(int anIndex)  { return _rows.get(anIndex); }

/**
 * Adds a row.
 */
public void addRow()  { addRow(getRowCount()); }

/**
 * Adds a row at given index.
 */
public void addRow(int anIndex)
{
    RMCrossTabRow row = new RMCrossTabRow();
    for(int i=0, iMax=getColCount(); i<iMax; i++) row.addCell(createCell(anIndex, i, null));
    addRow(row, anIndex);
}

/**
 * Adds a row at the given index.
 * If reference row index is provided, the referenced row object is copied for new row.
 * If reference column index is provided, previous columns cells are merged, successive column cells are cloned.
 */
public void addRow(RMCrossTabRow aRow, int anIndex)
{
    // Add row
    _rows.add(anIndex, aRow);
    aRow._table = this;
        
    // Add Row cells to columns
    for(int i=0, iMax=getColCount(); i<iMax; i++) { RMCrossTabCol col = getCol(i);
        RMCrossTabCell cell = aRow.getCell(i);
        col.addCell(cell, anIndex); }
    
    // Validate cells, add Row.Height to CrossTab.Height and relayout
    validateCells();
    setHeight(getHeight() + aRow.getHeight());
    relayout(); repaint();
}

/**
 * Removes the row at the given index.
 */
public RMCrossTabRow removeRow(int anIndex)
{
    // Remove row
    RMCrossTabRow row = _rows.remove(anIndex);
    
    // Remove row cells from columns
    for(int i=0, iMax=getColCount(); i<iMax; i++) { RMCrossTabCol col = getCol(i);
        col.removeCell(anIndex); }
    
    // Validate cells, set CrossTab.Height to height minus removed row height
    validateCells();
    setHeight(getHeight() - row.getHeight());
    relayout(); repaint();
    
    // If row is header row, decrement header row count
    if(anIndex<_headerRowCount) _headerRowCount--;
    return row;
}

/**
 * Returns the number of rows for this table.
 */
public int getColCount()  { return _cols.size(); }

/**
 * Sets the number of columns for this table.
 */
public void setColCount(int aColumnCount)
{
    if(aColumnCount<1) return; // Require at least 1 column
    while(aColumnCount>getColCount()) addCol(); // Add cols if needed
    while(aColumnCount<getColCount()) removeCol(getColCount()-1);  // Remove cols if needed
}

/**
 * Returns the specific column at the given index.
 */
public RMCrossTabCol getCol(int anIndex)  { return _cols.get(anIndex); }

/**
 * Adds a column.
 */
public void addCol()  { addCol(getColCount()); }

/**
 * Adds a column.
 */
public void addCol(int anIndex)
{
    RMCrossTabCol col = new RMCrossTabCol();
    for(int i=0, iMax=getRowCount(); i<iMax; i++) col.addCell(createCell(i, anIndex, null));
    addCol(col, anIndex);
}

/**
 * Adds a column at the given index.
 * If reference column index is provided, the referenced column object is copied for new column.
 * If reference row index is provided, previous row cells are merged, successive row cells are cloned.
 */
public void addCol(RMCrossTabCol aCol, int anIndex)
{
    // Add column
    _cols.add(anIndex, aCol);
    aCol._table = this;
    
    // Add column cells to rows
    for(int i=0, iMax=getRowCount(); i<iMax; i++) { RMCrossTabRow row = getRow(i);
        RMCrossTabCell cell = aCol.getCell(i);
        row.addCell(cell, anIndex); }
    
    // Validate cells, add Column.Width to CrossTab.Width and layout
    validateCells();
    setWidth(getWidth() + aCol.getWidth());
    relayout(); repaint();
}

/**
 * Removes the column at the given index.
 */
public RMCrossTabCol removeCol(int anIndex)
{
    // Remove column
    RMCrossTabCol col = _cols.remove(anIndex);
    
    // Remove column cells from rows
    for(int i=0, iMax=getRowCount(); i<iMax; i++) { RMCrossTabRow row = getRow(i);
        row.removeCell(anIndex); }
    
    // Validate cells, set CrossTab.Width to width minus removed column width and layout
    validateCells();
    setWidth(getWidth() - col.getWidth());
    relayout(); repaint();
    
    // If column is header column, decrement header column count
    if(anIndex<_headerColCount) _headerColCount--;
    return col;
}

/**
 * Returns the specific row index for the given point.
 */
public int getRow(double aY)
{
    // Iterate over rows and if point is less than row max y, return row
    if(aY<0) return -1;  // If point is above table, return -1
    for(int i=0, iMax=getRowCount(); i<iMax; i++) if(aY<=getRow(i).getMaxY()) return i;
    return getRowCount(); // Return row count since point is below table
}

/**
 * Returns the specific column index for the given point.
 */
public int getCol(double anX)
{
    // Iterate over rows and if point is less than row max y, return row
    if(anX<0) return -1; // If point is left of table, return -1
    for(int i=0, iMax=getColCount(); i<iMax; i++) if(anX<=getCol(i).getMaxX()) return i;
    return getColCount(); // Return column count if point is right of table
}

/**
 * Returns the cell at the given row and column indexes.
 */
public RMCrossTabCell getCell(int aRow, int aColumn)  { return getRow(aRow).getCell(aColumn); }

/**
 * Sets the cell at the given row and column indexes.
 */
protected void setCell(RMCrossTabCell aCell, int aRow, int aCol, int aRowSpan, int aColSpan)
{
    aCell._row = aRow; aCell._col = aCol; aCell.setParent(this);
    aCell._rowSpan = aRowSpan; aCell._colSpan = aColSpan;
    for(int i=0; i<aRowSpan; i++) for(int j=0; j<aColSpan; j++) {
        getRow(aRow + i).setCell(aCell, aCol + j);
        getCol(aCol + j).setCell(aCell, aRow + i);
    }
    relayout(); repaint();
}

/**
 * Creates a new cell for the given row and column and reference cell.
 */
private RMCrossTabCell createCell(int row, int column, RMCrossTabCell aRefCell)
{
    // Create new cell
    RMCrossTabCell cell = new RMCrossTabCell();
    
    // If reference row cell is available, copy it's attributes (but not content)
    if(aRefCell!=null)
        cell.copyShape(aRefCell);
    
    // If adding header row-column cross-section cell, set not visible
    if(row<getHeaderRowCount() && column<getHeaderColCount())
        cell.setVisible(false);
    
    // If adding a header row, set header row attributes
    else if(row<getHeaderRowCount()) {
        cell.setColor(HEADER_ROW_COLOR); if(!cell.getFont().isBold()) cell.setFont(cell.getFont().getBold()); }

    // If adding header column, set header column attributes
    else if(column<getHeaderColCount()) {
        cell.setColor(HEADER_COLUMN_COLOR); if(!cell.getFont().isBold()) cell.setFont(cell.getFont().getBold()); }

    // Return new cell
    return cell;
}

/**
 * Returns the number of header rows for table.
 */
public int getHeaderRowCount()  { return _headerRowCount; }

/**
 * Sets the number of header rows for table.
 */
public void setHeaderRowCount(int aCount)
{
    while(_headerRowCount<aCount) { _headerRowCount++; addRow(_headerRowCount-1); } // If too few, add new
    while(_headerRowCount>aCount) removeRow(_headerRowCount-1); // If too many, remove
}

/**
 * Returns the number header columns for table.
 */
public int getHeaderColCount()  { return _headerColCount; }

/**
 * Sets the number header columns for table.
 */
public void setHeaderColCount(int aCount)
{
    while(_headerColCount<aCount) { _headerColCount++; addCol(_headerColCount-1); } // If too few, add new
    while(_headerColCount>aCount) removeCol(_headerColCount-1); // If too many, remove
}

/**
 * Merges a range of cells into one cell.
 */
public void mergeCells(int aRow1, int aCol1, int aRow2, int aCol2)
{
    RMCrossTabCell cell = getCell(aRow1, aCol1); // Get root cell
    setCell(cell, aRow1, aCol1, aRow2 - aRow1 + 1, aCol2 - aCol1 + 1); // Set as cell for span
}

/**
 * Splits a cell.
 */
public void splitCell(RMCrossTabCell aCell)
{
    int row = aCell.getRow(), col = aCell.getCol();
    int rspan = aCell.getRowSpan(), cspan = aCell.getColSpan();
    for(int i=row, iMax=row+rspan; i<iMax; i++)
        for(int j=col, jMax=col+cspan; j<jMax; j++) {
            if(i==row && j==col) continue;
            setCell(createCell(i, j, null), i, j, 1, 1);
        }
}

/**
 * Iterates over all cells and updates cell Row, Col, RowSpan, ColSpan.
 */
void validateCells()
{
    // Iterate over cells and reset each child._row to -1
    int rcount = getRowCount(), ccount = getColCount(); if(rcount==0 || ccount==0) return;
    for(int i=0, iMax=rcount; i<iMax; i++) for(int j=0, jMax=ccount; j<jMax; j++) getCell(i,j)._row = -1;
    
    // Iterate over cells and reset each child row, column, rspan, cspan and bounds
    for(int i=0, iMax=rcount; i<iMax; i++) { for(int j=0, jMax=ccount; j<jMax; j++) {
        RMCrossTabCell cell = getCell(i, j); if(cell.getRow()>=0) continue;
        cell._row = i; cell._col = j; cell._parent = this;
        int rspan = 1; while(j+rspan<ccount && getCell(i, j+rspan)==cell) rspan++; // Calculate row span
        int cspan = 1; while(i+cspan<rcount && getCell(i+cspan, j)==cell) cspan++; // Calculate col span
        cell._colSpan = rspan; cell._rowSpan = cspan; // Set rspan/cspan
    }}
}

/**
 * Override to layout cells.
 */
protected void layoutImpl()
{
    // Remove all children
    removeChildren();
    int rcount = getRowCount(), ccount = getColCount(); if(rcount==0 || ccount==0) return;
    
    // Validate cells and fix rows/columns width/height
    validateCells();
    setColumnsWidth(); setRowsHeight();
    
    // Iterate over cells and reset each child row, column, rspan, cspan and bounds
    for(int i=0, iMax=rcount; i<iMax; i++) {
        for(int j=0, jMax=ccount; j<jMax; j++) {
            
            // Get current loop cell and set cell row/column (just continue if row already set)
            RMCrossTabCell cell = getCell(i, j); if(cell.getRow()!=i || cell.getCol()!=j) continue;
            
            // Calculate cell width/height
            double width = 0; for(int k=0, kMax=cell.getColSpan(); k<kMax; k++) width += getCol(j+k).getWidth();
            double height = 0; for(int k=0, kMax=cell.getRowSpan(); k<kMax; k++) height += getRow(i+k).getHeight();
            
            // Set cell bounds and add child
            cell.setBounds(getCol(j).getX(), getRow(i).getY(), width, height);
            addChild(cell);
        }
    }
    
    // Add dividers for phantom top row and left column
    _topBrdrRow.resetDividers(); _leftBrdrCol.resetDividers();
    for(int j=0, jMax=_topBrdrRow.getDividerCount(); j<jMax; j++) { RMCrossTabDivider div = _topBrdrRow.getDivider(j);
        if(div.getLength()>0) addChild(div); }
    for(int j=0, jMax=_leftBrdrCol.getDividerCount(); j<jMax; j++) { RMCrossTabDivider div = _leftBrdrCol.getDivider(j);
        if(div.getLength()>0) addChild(div); }
    
    // Iterate over rows and add row dividers
    for(int i=0, iMax=rcount; i<iMax; i++) { RMCrossTabRow row = getRow(i); row.resetDividers();
        for(int j=0, jMax=row.getDividerCount(); j<jMax; j++) { RMCrossTabDivider div = row.getDivider(j);
            if(div.getLength()>0) addChild(div); }}
    
    // Iterate over columns and add column dividers
    for(int i=0, iMax=ccount; i<iMax; i++) { RMCrossTabCol col = getCol(i); col.resetDividers();
        for(int j=0, jMax=col.getDividerCount(); j<jMax; j++) { RMCrossTabDivider div = col.getDivider(j);
            if(div.getLength()>0) addChild(div); }}
}

/** Sets the columns width to CrossTab.Width. */
void setColumnsWidth()
{
    double cw = 0; for(int i=0, iMax=getColCount(); i<iMax; i++) cw += getCol(i).getWidth();
    double w = getWidth(); if(MathUtils.equals(w, cw)) return;
    double dw = w - cw; int cc = getColCount(), incr = 1; if(dw<0) { dw = -dw; incr = -1; if(w<cc*8) return; }
    while(dw>0) { RMCrossTabCol c = getCol(Math.abs(_ci%cc)); _ci += incr; if(incr<0 && c._width<8) continue;
       if(dw>=1) { c._width += incr; dw -= 1; }
       else { c._width += dw*incr; dw = 0; }
    }
}

/** Sets the rows height to a CrossTab.Height. */
void setRowsHeight()
{
    double rh = 0; for(int i=0, iMax=getRowCount(); i<iMax; i++) rh += getRow(i).getHeight();
    double h = getHeight(); if(MathUtils.equals(h, rh)) return;
    double dh = h - rh; int rc = getRowCount(), incr = 1; if(dh<0) { dh = -dh; incr = -1; if(h<rc*8) return; }
    while(dh>0) { RMCrossTabRow r = getRow(Math.abs(_ri%rc)); _ri += incr; if(incr<0 && r._height<8) continue;
       if(dh>=1) { r._height += incr; dh -= 1; }
       else { r._height += dh*incr; dh = 0; }
    }
}

/** Counters to consistently get new row/column to add/subtract width/height from. */
int _ci, _ri;

/**
 * Sets a reportmill for this crosstab (which really gets the dataset and calls setObjects).
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    return new RMCrossTabRPG().rpgCrossTab(anRptOwner, aParent, this);
}

/**
 * Divides the shape by a given amount from the top. Returns a clone of the given shape with bounds 
 * set to the remainder. Divies children among the two shapes (recursively calling divide shape for those stradling).
 */
public RMShape divideShapeFromTop(double anAmount)
{
    // Get whether to ReprintHeaderRows from parent crosstab frame
    RMCrossTabFrame crossTabFrame = getParent(RMCrossTabFrame.class);
    boolean reprintHeaderRows = crossTabFrame!=null? crossTabFrame.getReprintHeaderRows() : false;
    
    // Disable reprint header rows if amount isn't enough to accommodate at least one non-header row
    if(reprintHeaderRows && (getRowCount()==getHeaderRowCount() || getRow(getHeaderRowCount()).getMaxY()>=anAmount))
        reprintHeaderRows = false;
    
    // Create bottom shape, set Y/Height and add columns
    RMCrossTab bottom = clone(); bottom._rows = new ArrayList(); bottom._height = 0;
    
    // Add Header rows
    if(reprintHeaderRows) {
        for(int i=0, iMax=getHeaderRowCount(); i<iMax; i++) { RMCrossTabRow hrow = getRow(i);
            bottom.addRow(hrow.cloneDeep(), bottom.getRowCount()); }}
    
    // Get row index of divide
    int rowIndex = getRow(anAmount);
    
    // See if split row needs some cloned cells
    RMCrossTabRow row = getRow(rowIndex);
    for(int j=0, jMax=getColCount(); j<jMax; j++) { RMCrossTabCell cell = row.getCell(j);
        if(cell.getRow()!=rowIndex) { RMCrossTabCell clone = cell.clone();
            setCell(clone, rowIndex, j, cell.getRowEnd() - rowIndex + 1, cell.getColSpan());
            j = j + cell.getColSpan() - 1;
        }
    }
    
    // Remove rows from top and add to bottom
    while(rowIndex<getRowCount()) {
        RMCrossTabRow row2 = removeRow(rowIndex);
        bottom.addRow(row2, bottom.getRowCount());
    }
    
    // Force layout of parts and return bottom
    layout(); bottom.layout();
    return bottom;
}

/**
 * Override to trigger layout.
 */
public void setStroke(RMStroke aStroke)  { super.setStroke(aStroke); relayout(); }

/**
 * Standard clone implementation.
 */
public RMCrossTab clone()
{
    // Get standard clone and clone columns & rows
    RMCrossTab clone = (RMCrossTab)super.clone();
    clone._cols = new ArrayList(); for(RMCrossTabCol col : _cols) clone._cols.add(col.clone());
    clone._rows = new ArrayList(); for(RMCrossTabRow row : _rows) clone._rows.add(row.clone());
    for(int i=0, iMax=getColCount(); i<iMax; i++) clone.getCol(i)._table = clone;
    for(int i=0, iMax=getRowCount(); i<iMax; i++) clone.getRow(i)._table = clone;
    
    // Create new phantom topBrdrRow and leftBrdrRow and return clone
    clone._topBrdrRow = new RMCrossTabRow(); clone._topBrdrRow._table = clone; clone._topBrdrRow._height = 0;
    clone._leftBrdrCol = new RMCrossTabCol(); clone._leftBrdrCol._table = clone; clone._leftBrdrCol._width = 0;
    return clone;
}

/**
 * Clone deep.
 */
public RMCrossTab cloneDeep()
{
    // Get standard clone
    RMCrossTab clone = clone();
    
    // Iterate over rows and columns, clone cell and set in clone
    for(int i=0, iMax=getRowCount(); i<iMax; i++) {
        for(int j=0, jMax=getColCount(); j<jMax; j++) { RMCrossTabCell cell = getCell(i, j);
            if(cell.getRow()==i && cell.getCol()==j)
                clone.setCell(cell.clone(), i, j, cell.getRowSpan(), cell.getColSpan());
        }
    }
    
    // Do layout on clone and return
    clone.relayout();
    return clone;
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXMLShape(anArchiver); e.setName("cell-table");
    
    // Archive DatasetKey, FilterKey
    if(getDatasetKey()!=null) e.add("dataset-key", getDatasetKey());
    if(_filterKey!=null && _filterKey.length()>0) e.add("filter-key", _filterKey);
    
    // Archive HeaderRowCount, HeaderColumnCount
    e.add("header-row-count", getHeaderRowCount());
    e.add("header-column-count", getHeaderColCount());
    
    // Archive Columns, Rows
    for(int i=0, iMax=getColCount(); i<iMax; i++) e.add(getCol(i).toXML(anArchiver));
    for(int i=0, iMax=getRowCount(); i<iMax; i++) e.add(getRow(i).toXML(anArchiver));
    
    // Archive cells
    for(int i=0, iMax=getRowCount(); i<iMax; i++)
        for(int j=0, jMax=getColCount(); j<jMax; j++) { RMCrossTabCell cell = getCell(i,j);
            if(cell.getRow()!=i || cell.getCol()!=j) continue;
            XMLElement cxml = anArchiver.toXML(cell, this);
            cxml.add("row", i); cxml.add("col", j); e.add(cxml);
        }

    // Return element
    return e;
}

/**
 * XML archival - override to suppress archival of children.
 */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement) { }

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive DatasetKey, FilterKey
    _datasetKey = anElement.getAttributeValue("dataset-key");
    _filterKey = anElement.getAttributeValue("filter-key");

    // Unarchive HeaderRowCount, HeaderColumnCount
    _headerRowCount = anElement.getAttributeIntValue("header-row-count");
    _headerColCount = anElement.getAttributeIntValue("header-column-count");
    
    // Unarchive columns and add to CrossTab
    for(int i=anElement.indexOf("column"); i>=0; i=anElement.indexOf("column", i+1)) {
        XMLElement cxml = anElement.get(i);
        RMCrossTabCol col = anArchiver.fromXML(cxml, RMCrossTabCol.class, this);
        _cols.add(col); col._table = this;
    }
    
    // Unarchive rows and add to CrossTab
    for(int i=anElement.indexOf("row"), ri=0; i>=0; i=anElement.indexOf("row", i+1), ri++) {
        XMLElement rxml = anElement.get(i);
        RMCrossTabRow row = anArchiver.fromXML(rxml, RMCrossTabRow.class, this);
        _rows.add(ri, row); row._table = this;
        
        // Legacy: Unarchive row cells
        for(int j=rxml.indexOf("cell"), ci=0; j>=0; j=rxml.indexOf("cell", j+1)) { XMLElement cellX = rxml.get(j);
            RMCrossTabCell cell = anArchiver.fromXML(cellX, RMCrossTabCell.class, this);
            setCell(cell, ri, ci, 1, cell.getColSpan()); ci += cell.getColSpan();
        }
    }
    
    // Unarchive CrossTab cells
    for(int i=anElement.indexOf("cell"); i>=0; i=anElement.indexOf("cell", i+1)) { XMLElement cxml = anElement.get(i);
        RMCrossTabCell cell = anArchiver.fromXML(cxml, RMCrossTabCell.class, this);
        int row = cxml.getAttributeIntValue("row"), col = cxml.getAttributeIntValue("col");
        setCell(cell, row, col, cell.getRowSpan(), cell.getColSpan());
    }
}

}