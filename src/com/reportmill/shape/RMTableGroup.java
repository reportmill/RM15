/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.gfx.*;
import snap.util.*;

/**
 * This class manages a hierarchy of tables so that multiple tables can be configured to display in the same area
 * of a given page. Each table will pick up exactly where the previous table ended.
 */
public class RMTableGroup extends RMParentShape {
    
    // The currently selected table in the hierarchy
    RMTable                     _mainTable;
    
    // Map of tables to its child tables
    Map <Object,List<RMTable>>  _tableMap = new Hashtable();

/**
 * Returns the currently selected table.
 */
public RMTable getMainTable() { return _mainTable; }

/**
 * Sets the currently selected table.
 */
public void setMainTable(RMTable aTable)
{
    // If table is null, use first table instead
    if(aTable==null) aTable = getChildTable(0);
    
    // If value already set, just return
    if(aTable==getMainTable()) return;
    
    // Set new main table and fire property change
    firePropChange("MainTable", _mainTable, _mainTable = aTable);
    
    // Call relayout to actually set new MainTable as child so changes are hidden from undo
    relayout();
}

/**
 * Returns the parent of the given table.
 */
public RMTable getParentTable(RMTable aTable)
{
    // Iterate over tables keys in table map
    for(Object table : _tableMap.keySet()) {
        List tables = _tableMap.get(table);
        if(ListUtils.containsId(tables, aTable))
            return table==this? null : (RMTable)table;
    }
    
    // Return null since table parent not found
    return null;
}

/**
 * Returns the index of the given table in its parent's child tables list.
 */
public int indexOf(RMTable aTable)
{
    List peers = getPeerTables(aTable);
    return ListUtils.indexOfId(peers, aTable);
}

/**
 * Returns the list of peer tables for a given table.
 */
public List <RMTable> getPeerTables(RMTable aTable)
{
    RMTable parentTable = getParentTable(aTable);
    return getChildTables(parentTable);
}

/**
 * Returns the previous peer table of the given table.
 */
public RMTable getPeerTablePrevious(RMTable aTable)
{
    List <RMTable> tables = getPeerTables(aTable);
    int index = ListUtils.indexOfId(tables, aTable);
    return ListUtils.get(tables, index - 1);
}

/**
 * Returns the next peer table of the given table.
 */
public RMTable getPeerTableNext(RMTable aTable)
{
    List <RMTable> tables = getPeerTables(aTable);
    int index = ListUtils.indexOfId(tables, aTable);
    return ListUtils.get(tables, index + 1);
}

/**
 * Returns the number of top level tables in this table group.
 */
public int getChildTableCount()  { return getChildTables().size(); }

/**
 * Returns the top level table at the given index for this table group.
 */
public RMTable getChildTable(int anIndex)  { return getChildTables().get(anIndex); }

/**
 * Returns the list of top level tables for this table group.
 */
public List <RMTable> getChildTables()  { return getChildTables(this); }

/**
 * Returns the number of child tables for the given parent table.
 */
public int getChildTableCount(Object aTable)  { return ListUtils.size(getChildTables(aTable)); }

/**
 * Returns the specific child table of the given table at the given index.
 */
public RMTable getChildTable(Object aTable, int anIndex)  { return getChildTables(aTable).get(anIndex); }

/**
 * Returns the list of child tables for the given table.
 */
public List <RMTable> getChildTables(Object aTable)  { return getChildTables(aTable, false); }

/**
 * Returns the list of child tables for the given table, creating the list if requested.
 */
public List getChildTables(Object aTable, boolean create)
{
    if(aTable==null) aTable = this;
    List childTables = _tableMap.get(aTable);
    if(childTables==null && create) {
        childTables = new Vector();
        _tableMap.put(aTable, childTables);
    }
    return childTables;
}

/**
 * Returns the complete dataset key for the current main table (pre-pending dataset keys of parents).
 */
public String getDatasetKey()
{
    // Declare variable for dataset key
    String datasetKey = null;
    
    // Iterate over parents to get combined dataset key
    for(RMTable table=getParentTable(_mainTable); table!=null; table=getParentTable(table))
        datasetKey = StringUtils.add(table.getDatasetKey(), datasetKey, ".");
    
    // Return dataset key
    return datasetKey;
}

/**
 * Adds the given table to the main table's list of peers.
 */
public void addPeerTable(RMTable aTable)  { addPeerTable(aTable, getMainTable()); }

/**
 * Adds the given table as a peer, and just after, the second given table.
 */
public void addPeerTable(RMTable aTable, RMTable afterThisTable)
{
    RMTable parentTable = getParentTable(afterThisTable);
    addChildTable(aTable, parentTable, afterThisTable);
}

/**
 * Adds the given table as a child to the main table.
 */
public void addChildTable(RMTable aTable)  { addChildTable(aTable, null, null); }

/**
 * Adds the given table as a child of the given parent table.
 */
public void addChildTable(RMTable aTable, RMTable aParentTable)  { addChildTable(aTable, aParentTable, null); }

/**
 * Adds the given table table as a child of the parent table (after the third given table).
 */
public void addChildTable(RMTable aTable, RMTable aParentTable, RMTable afterThisTable)
{
    // Get childTables for aParentTable and add after table
    List tables = getChildTables(aParentTable, true);
    int index = tables.indexOf(afterThisTable); if(index<0) index = tables.size(); else index++;
    tables.add(aTable);

    // Set MainTable
    setMainTable(aTable);
}

/**
 * This method moves a given table up or down in its peer list (used for Move Up & Move Down menus).
 */
public void moveTable(RMTable aTable, int interval)
{
    RMTable parentTable = getParentTable(aTable);
    List tables = getChildTables(parentTable);
    if(tables==null || !tables.contains(aTable))
        return;
        
    int currentIndex = tables.indexOf(aTable);
    int newIndex = currentIndex + interval;
    if(newIndex<0 || newIndex>=tables.size())
        return;
    
    tables.remove(currentIndex);
    tables.add(newIndex, aTable);
}

/**
 * This method moves a table to a new parent (used for Move In and Move Out menus).
 */
public void makeTableChildOfTable(RMTable aTable, Object newParent)
{    
    List peerTables = getPeerTables(aTable);
    peerTables.remove(aTable);
    List newParentsTables = getChildTables(newParent, true);
    newParentsTables.add(aTable);
}

/**
 * This method removes a table from the table group.
 */
public void removeTable(RMTable aTable)
{
    // Get parent table, peer tables, child tables
    RMTable parentTable = getParentTable(aTable);
    List <RMTable> peerTables = getPeerTables(aTable);
    List <RMTable> childTables = getChildTables(aTable);
    int index = peerTables.indexOf(aTable);
    
    // Remove any child tables of aTable
    for(int i=0, iMax=ListUtils.size(childTables); i<iMax; i++)
        removeTable(childTables.get(0));
    
    // Remove aTable
    peerTables.remove(aTable);

    // Make sure that _mainTable is a valid table - choose previous peer if available or parent of aTable
    if(_mainTable==aTable) {

        // Choose previous peer if available
        if(!peerTables.isEmpty()) {
            if(index < peerTables.size())
                setMainTable(peerTables.get(index));
            else setMainTable(ListUtils.getLast(peerTables));
        }

        // Choose parent if previous peer isn't available
        else if(parentTable!=null) {
            _tableMap.remove(parentTable);
            setMainTable(parentTable);
        }
    }
}

/**
 * Paints table group button after child table has been drawn.
 */
protected void paintShapeOver(Painter aPntr)
{
    // Do normal version (just return if not editing)
    super.paintShapeOver(aPntr); if(!RMShapePaintProps.isEditing(aPntr)) return;
    
    // Draw TableGroup button
    aPntr.drawButton(1, getHeight() - 18, 100, 18, false);
    aPntr.setColor(Color.DARKGRAY);
    aPntr.setFont(Font.Arial12.getBold());
    aPntr.drawString("Table Group", 14, getHeight() - 5);
}

/**
 * Override to paint table stroke on top.
 */
public boolean isStrokeOnTop()  { return true; }

/**
 * Override to reset child in bounds.
 */
protected void layoutImpl()
{
    // If no MainTable, just return
    if(_mainTable==null) return; //if(getChildCount()==0) return;
    
    // Make sure MainTable is only child
    if(getChildCount()!=1 || getChild(0)!=_mainTable) {
        while(getChildCount()>0) removeChild(0).setParent(this);
        addChild(_mainTable);
    }

    // Set MainTable bounds to full bounds
    RMShape child = getChild(0);
    child.setBounds(0, 0, getWidth(), getHeight());
}

/**
 * Override to set main table to first child table.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    return new RMTableGroupRPG(anRptOwner, this, null).rpgAll();
}

/**
 * Standard clone implementation.
 */
public RMTableGroup clone()
{
    // Do normal shape clone, main table and table map and return clone
    RMTableGroup clone = (RMTableGroup)super.clone();
    clone._mainTable = null;
    clone._tableMap = new HashMap();
    return clone;
}

/**
 * Override to clone child tables.
 */
public RMTableGroup cloneDeep()
{
    RMTableGroup clone = clone();
    cloneChildTables(clone, null, null);
    return clone;
}

/**
 * Clones child tables.
 */
private void cloneChildTables(RMTableGroup aClone, RMTable aParentTable, RMTable aParentTableClone)
{
    // Iterate over child tables for parent
    for(int i=0, iMax=getChildTableCount(aParentTable); i<iMax; i++) {
        RMTable table = getChildTable(aParentTable, i);
        RMTable tableClone = (RMTable)table.cloneDeep();
        aClone.addChildTable(tableClone, aParentTableClone);
        cloneChildTables(aClone, table, tableClone);
    }
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes, reset element name and return element
    XMLElement e = super.toXMLShape(anArchiver); e.setName("table-group");
    return e;
}

/**
 * XML archival - override to archive all child tables (not just visible one), recursively.
 */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
{
    toXMLChildTables(anArchiver, anElement, null);    
}

/**
 * XML archival to recursively archive table group hierarchy.
 */
protected void toXMLChildTables(XMLArchiver anArchiver, XMLElement anElement, RMTable aParentTable)
{
    // Get child tables
    List <RMTable> childTables = getChildTables(aParentTable);
    
    // Iterate over child tables and archive
    for(int i=0, iMax=ListUtils.size(childTables); i<iMax; i++) { RMTable table = childTables.get(i);
        
        
        XMLElement tableXML = table.toXML(anArchiver); // Archive table to xml
        anElement.add(tableXML);  // Add xml to parent
        toXMLChildTables(anArchiver, tableXML, table); // Recurse for child table's child tables
    }
}

/**
 * XML unarchival - overridden to unarchive all child tables, recursively.
 */
protected void fromXMLChildren(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive child tables (recursively)
    fromXMLChildTables(anArchiver, anElement, null);
    
    // SetMainTable to first table
    setMainTable(getChildTable(0));    
}

/**
 * XML archival to recursively unarchive table group hierarchy.
 */
protected void fromXMLChildTables(XMLArchiver anArchiver, XMLElement anElement, RMTable aParentTable)
{
    // Unarchive child tables (recursively) and add them back
    for(int i=anElement.indexOf("table"); i>=0; i=anElement.indexOf("table", i+1)) {
        
        // Get table xml, unarchive table, add child table and recurse for child table's child tables
        XMLElement tableXML = anElement.get(i);
        RMTable table = (RMTable)anArchiver.fromXML(tableXML, this);
        addChildTable(table, aParentTable);
        if(!MathUtils.equals(table.getWidth(), getWidth()) && anArchiver.getVersion()<14) fixWidths(table);
        fromXMLChildTables(anArchiver, tableXML, table);
    }
}

/** Editor method - indicates that table group children (tables) super select immediately. */
public boolean childrenSuperSelectImmediately()  { return true; }

/** Fix for old table groups that were resized and saved without viewing individual tables. */
private void fixWidths(RMTable t) {
    t.setWidth(getWidth()); for(int i=0,iMax=t.getChildCount();i<iMax;i++) fixWidths(t.getRow(i)); }
private void fixWidths(RMTableRow aRow) {
    if(aRow.getAlternates()!=null)
        for(RMShape alt : aRow.getAlternates().values()) if(alt!=aRow) fixWidths((RMTableRow)alt);
    double rw = aRow.getWidth(), dw = getWidth() - rw; aRow.setWidth(getWidth());
    if(aRow.isStructured())
        for(RMShape child : aRow.getChildren()) { double cw = child.getWidth(); child.setWidth(cw+cw/rw*dw); }
}

}