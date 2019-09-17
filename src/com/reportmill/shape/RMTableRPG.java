/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import java.util.*;
import snap.util.ListUtils;

/**
 * Performs RPG for a table.
 */
public class RMTableRPG extends RMParentShape {

   // The ReportOwner
   ReportOwner       _rptOwner;
   
   // The Table
   RMTable           _table;
   
   // Whether table should paginate
   boolean           _paginating = true;
   
   // The last row added to page
   RMTableRowRPG     _lastRow;
   
   // The next page
   RMTableRPG        _nextPage;
   
   // Top RPG
   RMTableRowRPG     _topRow = new RMTableRowRPG();
   
   // Counters for Page, PageBreak and PageBreakPage
   int               _page, _pageBreak = 1, _pageBreakPage = 1;
   
   // Whether this page encountered a page break
   boolean           _doPageBreak;
   
   // The preferred height of table
   double            _prefHeight;

/**
 * Creates a new RMTableRPG for ReportOwner and Table.
 */
public RMTableRPG(ReportOwner anRptOwner, RMTable aTable)
{
    _rptOwner = anRptOwner; _table = aTable;
    if(aTable!=null) copyShape(aTable);
}

/**
 * Do RPG.
 */
public RMShape rpgAll()
{
    // If not paginating, set height arbitrarily large
    if(!_rptOwner.getPaginate()) {
        _paginating = false; _prefHeight = getHeight(); setHeight(Float.MAX_VALUE); } //setAutosizing("--~,-~-");
    
    // Do report generation for table
    rpgTable(_table);
    
    // If not paginating, reset height and set PrefHeight
    if(_prefHeight>0) {
        setHeight(_prefHeight);
        if(getChildCount()>0) _prefHeight = getChildLast().getFrameMaxY();
    }
    
    // Get return shape - convert to ColumnsPage if needed
    RMTableRPG rshape = this;
    if(_table.getColumnCount()>1)
        rshape = makeColumns();
    
    // If only one page generated, return it, otherwise return ShapeList
    ReportOwner.ShapeList slist = new ReportOwner.ShapeList(); //if(_nextPage==null) return this;
    for(RMTableRPG pg=rshape; pg!=null; pg=pg._nextPage) slist.addChild(pg);
    return slist;
}

/**
 * Do RPG.
 */
protected void rpgTable(RMTable aTable)
{
    // Get grouped objects
    RMGroup group = getGroup(aTable);
    
    // Add Rows for group
    RMTableRPG page = this; while(page._nextPage!=null) page = page._nextPage; page._table = aTable;
    RMTableRowRPG lastRow = null;
    while(!page.addRows(group, page._topRow, lastRow)) {
        lastRow = page._lastRow;
        page = page.addPage();
    }
    
    // Move rows for last page to bottom
    page.moveRowsToBottom();
}

/**
 * Returns the group for a given table.
 */
protected RMGroup getGroup(RMTable aTable)
{
    List dataset = _rptOwner.getKeyChainListValue(aTable.getDatasetKey()); // Get dataset
    if(dataset==null) dataset = new ArrayList();
    dataset = DataUtils.getFilteredList(dataset, aTable.getFilterKey()); // Apply FilterKey
    return aTable.getGrouper().groupObjects(dataset); // Do grouping
}

/**
 * Returns the last page.
 */
public RMTableRPG getPageLast()  { RMTableRPG p = this; while(p._nextPage!=null) p = p._nextPage; return p; }

/**
 * Adds a new page.
 */
protected RMTableRPG addPage()
{
    // Move rows to bottom
    moveRowsToBottom();
    
    // Create page, reset ivars, return
    RMTableRPG npage = _nextPage = createPage(); npage._table = _table;
    npage._page = _page + 1;
    npage._pageBreak = _pageBreak; npage._pageBreakPage = _pageBreakPage + 1;
    if(_doPageBreak) { npage._pageBreak++; npage._pageBreakPage = 1; }
    return npage;
}

/**
 * Creates a page.
 */
protected RMTableRPG createPage()  { return new RMTableRPG(_rptOwner, _table); }

/**
 * Adds table rows for a group.
 */
protected boolean addRows(RMGroup aGroup, RMTableRowRPG aParentRPG, RMTableRowRPG theLastRow)
{
    // Get header, details summary rows
    RMTableRow headerRow = _table.getRow(aGroup.getKey() + " Header");
    RMTableRow detailsRow = _table.getRow(aGroup.getKey() + " Details");
    RMTableRow summaryRow = _table.getRow(aGroup.getKey() + " Summary");
    
    // If we previously hit page break (presumably on last detail row of group), return for new page
    if(_doPageBreak) {
        _lastRow = new RMTableRowRPG(); _lastRow._group = aGroup;
        return false;
    }

    // Add header row for group, if header row exists
    if(headerRow!=null && (!aGroup.isEmpty() || headerRow.getPrintEvenIfGroupIsEmpty())) {
        
        // Declare RowRPG, Version
        RMTableRowRPG rowRPG = null; String version = null;
        
        // If LastRow provided, either use Split, Reprint, or set empty Reprint
        if(theLastRow!=null) {
            if(theLastRow._row==headerRow) {
                rowRPG = theLastRow._split; theLastRow = null; }
            else if(!headerRow.getReprintWhenWrapped()) {
                rowRPG = new RMTableRowRPG(); rowRPG._row = headerRow; rowRPG._group = aGroup; }
            else version = RMTableRow.VersionReprint;
        }
        
        // If no RowRPG, do normal row RPG
        if(rowRPG==null) {
            rowRPG = new RMTableRowRPG(); rowRPG.rpgAll(_rptOwner, headerRow, aGroup, version); }
        
        // Add HeaderRow RowRPG
        if(!addRow(rowRPG, aParentRPG))
            return false;
        aParentRPG = rowRPG;
    }
    
    // Add details rows for group
    int added = 0;
    for(int i=0, iMax=aGroup.size(); i<iMax; i++,added++) { RMGroup childGroup = aGroup.getGroup(i);
    
        // Get parentRPG so we can reset if details row is present
        RMTableRowRPG parentRPG = aParentRPG;
    
        // If LastRow given, skip groups until we get to row
        if(theLastRow!=null && !theLastRow._group.isAncestor(childGroup)) {
            if(theLastRow._group==childGroup) { if(theLastRow._row==null) theLastRow = null; }
            else continue;
        }
        
        // Add details row for group, if details row exists
        if(detailsRow!=null) {
            
            // Declare RowRPG, Version
            RMTableRowRPG rowRPG = null; String version = null;
            
            // If LastRow provided, either use Split, Reprint, or set empty Reprint
            if(theLastRow!=null) {
                if(theLastRow._row==detailsRow) {
                    rowRPG = theLastRow._split; theLastRow = null; }
                else if(!detailsRow.getReprintWhenWrapped()) {
                    rowRPG = new RMTableRowRPG(); rowRPG._row = detailsRow; rowRPG._group = childGroup; }
                else version = RMTableRow.VersionReprint;
            }
            
            // If no RowRPG, do normal row RPG
            if(rowRPG==null) {
                rowRPG = new RMTableRowRPG();
                rowRPG.rpgAll(_rptOwner, detailsRow, childGroup, version);
            }
            
            // Add DetailsRow RowRPG
            if(!addRow(rowRPG, aParentRPG)) {
                break; } 
            parentRPG = rowRPG;
        }
        
        // If lower level groupings exist, recurse
        if(!childGroup.isLeaf()) {
            if(!addRows(childGroup, parentRPG, theLastRow))
                break;
        } 
        
        // If child table exists, recurse (for RMTableGroupRPG)
        else if(!addChildTableRows(childGroup, parentRPG, theLastRow))
            break;
        
        // Clear LastRow
        theLastRow = null;

        // If paginating, check for PageBreak (can be explicit or triggered by DetailsRow.PageBreakKey)
        if(_paginating && i+1<iMax) {
            
            // If at PageBreakGroupIndex, set DoPageBreak
            int pbIndex = _table.getPageBreakGroupIndex();
            if(pbIndex>=0 && pbIndex>=aGroup.getParentCount())
                _doPageBreak = true;
                
            // If DetailsRow.PageBreakKey evals true, set DoPageBreak
            String pbKey = detailsRow!=null? detailsRow.getPageBreakKey() : null;
            if(pbKey!=null && RMKeyChain.getBoolValue(childGroup, pbKey))
                _doPageBreak = true;
            
            // If DoPageBreak, create place-holder LastRow for next group
            if(_doPageBreak) {
                _lastRow = new RMTableRowRPG(); _lastRow._group = aGroup.getGroup(i+1);
                added++; break;
            }
        }
    }
    
    // Add Running summary (if grouping didn't finish and one is available)
    if(added<aGroup.size()) {
        
        // If no Running Summary row, just return false
        if(summaryRow==null || !summaryRow.hasVersion("Running")) return false;
        
        // Get first real group on page and its index
        RMGroup pageStartRowGroup = aGroup; int pageStartRowIndex = 0;
        for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMTableRowRPG row = (RMTableRowRPG)getChild(i);
            if(row.getGroup().getParentCount()>pageStartRowGroup.getParentCount()) {
                pageStartRowGroup = row.getGroup(); pageStartRowIndex = i; }}
            
        // Try to add Running summary by progressively removing last row until there is room
        while(getChildCount()>pageStartRowIndex) {
            RMTableRowRPG oldLastRow = _lastRow; _lastRow = (RMTableRowRPG)getChildLast();
            RMGroup group = new RMGroup.Running(aGroup, pageStartRowGroup, oldLastRow.getGroup());
            RMTableRowRPG row = new RMTableRowRPG(); row.rpgAll(_rptOwner, summaryRow, group, "Running");
            if(addRow(row, aParentRPG)) { _lastRow = oldLastRow; break; }
            removeRow(_lastRow);
            while(_lastRow.isSummary()) removeRow(_lastRow=(RMTableRowRPG)getChildLast());
            while(!isSatisfied(_lastRow._parentRPG, -1)) removeRow(_lastRow=(RMTableRowRPG)getChildLast());
        }
        
        // Running summary always returns false
        return false;
    }
    
    // Add summary rows for group
    if(summaryRow!=null && (!aGroup.isEmpty() || summaryRow.getPrintEvenIfGroupIsEmpty())) {
        
        // Get summary row group: If Running is present, reset group to page groups
        RMGroup group = aGroup;
        if(summaryRow.hasVersion("Running")) {
            for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMTableRowRPG row = (RMTableRowRPG)getChild(i);
                if(row.getGroup().getParentCount()>group.getParentCount()) group = row.getGroup(); }
            group = new RMGroup.Running(aGroup, group, null); }
        
        // If no header/details, then this row shouldn't do widow/orphan
        if(headerRow==null && detailsRow==null) summaryRow.setNumberOfChildrenToStayWith(0);
        
        // Create filled Summary row
        RMTableRowRPG row = new RMTableRowRPG();
        row.rpgAll(_rptOwner, summaryRow, group, null);
        
        // If last summary was split, use remainder instead
        if(theLastRow!=null && theLastRow._row==summaryRow && theLastRow._split!=null) {
            row = theLastRow._split; theLastRow = null;
        }
        
        // Add row (return if failed)
        if(!addRow(row, aParentRPG))
            return false;
    }
    
    // Return true
    return true;
}

/**
 * A hook to add rows for child tables (RMTableGroup).
 */
protected boolean addChildTableRows(RMGroup aGroup, RMTableRowRPG aParentRPG, RMTableRowRPG theLastRow)
{
    return true;
}

/**
 * Adds a row to this TableRPG.
 */
boolean addRow(RMTableRowRPG aRow, RMTableRowRPG aParentRPG)
{
    // Add row below last row
    aRow.setY(_lastRow!=null? _lastRow.getFrameMaxY() : 0);
    addChild(_lastRow=aRow);
    aParentRPG.addChildRPG(aRow);
    
    // If within table bounds, return true
    if(aRow.getFrameMaxY()<=getHeight()) return true;
    
    // If parent would be satisfied with a split and row can split, split it and return false
    if(isSatisfied(aParentRPG, 0) && splitRow(aRow))
        return false;
    
    // If this was the only row on page, just return true
    if(getChildCount()==1)
        return true;
    
    // If summary row didn't fit, remove it and make previous child LastRow (return if none)
    if(aRow.isSummary()) {
        removeRow(aRow);
        if(aRow._row.getNumberOfChildrenToStayWith()==0) return false;
        if(aParentRPG.getChildRPGCount()>0) _lastRow = (RMTableRowRPG)getChildLast();
        else return false;
    }
    
    // Iterate up parentRPGs while unsatisfied constraints
    RMTableRowRPG lastRow = _lastRow, parentRPG = lastRow._parentRPG;
    while(parentRPG!=_topRow && !isSatisfied(parentRPG, -1)) {
        lastRow = parentRPG; parentRPG = parentRPG._parentRPG; }
    
    // If no valid parentRPG on page, return true so we keep all rows
    if(parentRPG==_topRow && parentRPG.getChildRPGCount()==1)
        return true;
    
    // Remove invalid rpgRows and return false
    removeRow(_lastRow=lastRow);
    return false;
}

/**
 * Returns whether RMTableRowRPG is satisfied.
 */
boolean isSatisfied(RMTableRowRPG aParentRPG, int aMod)
{
    // If parent has what it needs, return true
    int has = aParentRPG.getChildRPGCount() + aMod; // Mod might be -1 if there is a child we are planning to remove
    int needs = aParentRPG._row!=null? aParentRPG._row.getNumberOfChildrenToStayWith() : 0;
    if(has>=needs)
        return true;
    
    // Otherwise, if all upper parents are first of their kind on page (effectively headers), we have to be satisfied
    boolean tops = true;
    for(RMTableRowRPG p=aParentRPG._parentRPG; p!=null && tops; p=p._parentRPG) tops = p.getChildRPGCount()==1;
    return tops;
}

/**
 * Removes a row from this TableRPG.
 */
void removeRow(RMTableRowRPG aRow)
{
    for(int i=aRow.getChildRPGCount()-1; i>=0; i--) removeRow(aRow._childRPGs.get(i));
    aRow.removeFromParent();
    ListUtils.removeId(aRow._parentRPG._childRPGs, aRow);
}

/**
 * Splits a row.
 */
boolean splitRow(RMTableRowRPG aRow)
{
    // Get template for added row
    RMTableRow template = aRow._row;
    
    // If remaining space in table isn't enough, just return
    double remainingHeight = getHeight() - aRow.getFrameY();
    double minSplitHeight = template.getMinSplitHeight();
    if(remainingHeight<minSplitHeight)
        return false;

    // If available space in row after split wouldn't be enough, just return
    double minSplitRemainderHeight = template.getMinSplitRemainderHeight();
    if(aRow.getHeight() < minSplitHeight + minSplitRemainderHeight)
        return false;
    
    // Get next row
    int nextRowIndex = aRow.indexOf() + 1;
    RMShape nextRow = nextRowIndex<getChildCount()? getChild(nextRowIndex) : null;
    
    // Calculate how much of the row overruns bottom of this page (and how much underruns)
    double maxY = nextRow!=null? nextRow.getFrameY() : getHeight();
    double part1Height = maxY - aRow.getFrameY();
    double part2Height = aRow.getHeight() - part1Height;
    
    // If part2Height isn't at least minSplitRemainderHeight, we should reduce part1Height
    if(part2Height<minSplitRemainderHeight)
        part1Height = aRow.getHeight() - minSplitRemainderHeight;

    // Divide row by part1Height, grow to best height and validate
    RMTableRowRPG split = (RMTableRowRPG)aRow.divideShapeFromTop(part1Height); aRow._split = split;
    split.setBestHeight();
    split.layoutDeep();
    return true;
}

/**
 * Scoots any rows that request it to the bottom of the page.
 */
protected void moveRowsToBottom()
{
    // Iterate over rows to see if any want to be at the bottom of page
    double shift = 0;
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMTableRowRPG rowRPG = (RMTableRowRPG)getChild(i);
        if(shift==0 && rowRPG._row.getMoveToBottom()) { shift = getHeight() - getChildLast().getFrameMaxY(); }
        if(shift>0) rowRPG.setY(rowRPG.getY() + shift);
    }
}

/**
 * This method moves each groups the resulting set of table pages into a parent shape to accommodate aTable's 
 * getNumberOfColumns (spaced by its getColumnSpacing). tables for each explicit page break into encompassing
 * shapes that hold numberOfColumns tables (spaced apart by _columnSpacing).
 */
protected RMTableRPG makeColumns()
{
    // Get number of columns aTable is requesting, the max number available and the spacing between them
    int columnCount = _table.getColumnCount();
    double spacing = _table.getColumnSpacing();
    double width = _table.getWidth()*columnCount + spacing*(columnCount-1);
    
    // Create first ColumnsPage
    RMTableRPG columnsPage = new RMTableRPG(_rptOwner, null);
    columnsPage.setBounds(_table.getX(), _table.getY(), width, _table.getHeight());
    
    // Iterate over pages and add to ColumnsPages
    RMTableRPG page = this, cpage = columnsPage;
    while(page!=null) {
        for(int i=0; i<columnCount && page!=null; i++, page=page._nextPage) {
            page.setXY(i*(page.getWidth()+spacing),0);
            cpage.addChild(page);
        }
        if(page!=null) {
            cpage._nextPage = new RMTableRPG(_rptOwner, null); cpage = cpage._nextPage;
            cpage.setBounds(_table.getX(), _table.getY(), width, _table.getHeight());
        }
    }

    // Return first ColumnsPage
    return columnsPage;
}

/**
 * Returns the page break
 */
public int getPageBreak()  { return _pageBreak; }

/**
 * Returns the page break max.
 */
public int getPageBreakMax()  { return _nextPage!=null? _nextPage.getPageBreakMax() : _pageBreak; }

/**
 * Returns the page break page.
 */
public int getPageBreakPage()  { return _pageBreakPage; }

/**
 * Returns the page break page max.
 */
public int getPageBreakPageMax()
{
    return _nextPage!=null && _nextPage._pageBreakPage!=1? _nextPage.getPageBreakPageMax() : _pageBreakPage;
}

/**
 * Override to return PrefHeight is not paginating.
 */
protected double getPrefHeightImpl(double aWidth)  { return _prefHeight>0? _prefHeight : getHeight(); }

/**
 * Override to paint table stroke on top.
 */
public boolean isStrokeOnTop()  { return true; }

/** Override to make selectable. */
public boolean superSelectable()  { return true; }

}