/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.*;

/**
 * This Hashtable subclass provides page number information for table rows in a generated report for the purpose of
 * generating a table of contents. It is initialized from a previously generated report and provides Lists of table
 * row data for keys of the form "Table-DatasetKey.Table-Grouping-Key" from the original report. Tables based on these
 * Dataset keys can then reference any key from the original set of objects (and any key available to the Details table
 * row for that grouping) and also the keys "page" and "row".
 * 
 * Here's an example of using RMTableOfContents:
 *
 <pre>
    // Generate report normally
    RMDocument template = RMDocument.getDoc("MyTemplate.rpt");
    RMDocument report = template.generateReport(myObjects);
    
    // Get table of contents and generate that report
    RMTableOfContents toc = new RMTableOfContents(report);
    RMDocument tocTemplate = RMDocument.getDoc("MyTemplateTOC.rpt");
    RMDocument report2 = tocTemplate.generateReport(toc);
    
    // Append original report to table of contents and get PDF
    report2.addPages(report);
    report2.writePDF("MyReport.pdf");
</pre>
 *
 * In this example, the tocTemplate would be a simple template with a Table who's DatasetKey is equal to some
 * Table-DatasetKey.Table-Grouping-Key from the original report (eg., "getMovies.getStudio", for our typical Hollywood
 * example, where the original template has a "getMovies" table, with a grouping on the key "getSudio"). In addition to
 * a large header row (that maybe says, "Hollywood Report - - Table of Contents"), this table would have a details row
 * configured with a column of the form "@row@. @getStudio.getName@ ........ @page@". Additionally, the template might
 * want to set the URL for the Details table row to "Page:@page@" so that the table of contents entries are actually
 * hyperlinks to the appropriate page.
 * 
 * It may be interesting to note (as an implementation detail only) that the "page" key in an RMTableOfContents entry
 * actually returns the page number relative to the end of the document, in the form of a key itself, for example:
 * "@PageMax-N@" (which gets re-evaluated before PDF generation), so that it can compensate for the length of the
 * Table of Contents table itself. Also, RMTableOfContents uses lower case versions of the common keys @row@ and
 * @page@ so as not to conflict with the normal @Row@ and @Page@.
 */
public class RMTableOfContents extends HashMap {

/**
 * Creates a new table of contents instance.
 */
public RMTableOfContents(RMDocument aDoc)
{
    // Iterate over report pages to find any filled table shapes and collect row info from them
    for(int i=0, iMax=aDoc.getPageCount(); i<iMax; i++) { RMPage page = aDoc.getPage(i);
        findTable(page); }
}

/**
 * Finds tables to process for TOC info.
 */
private void findTable(RMShape aShape)
{
    // If shape is table RPG, get row info
    if(aShape instanceof RMTableRPG)
        processTable((RMTableRPG)aShape);
    
    // Otherwise, recurse
    else if(aShape instanceof RMParentShape) { RMParentShape parent = (RMParentShape)aShape;
        for(int i=0, iMax=parent.getChildCount(); i<iMax; i++)
            findTable(parent.getChild(i));
    }
}

/**
 * Gets TOC info from a given table.
 */
private void processTable(RMTableRPG aTable)
{
    // Iterate over child table rows, and add entries to appropriate list under grouping key
    for(int i=0, iMax=aTable.getChildCount(); i<iMax; i++) { RMTableRowRPG row = (RMTableRowRPG)aTable.getChild(i);
        
        // Just continue if not details row
        if(!row.isDetails()) continue;
        
        // Get original table row template and table
        RMTableRow templateRow = row.getTemplate();
        RMShape table = templateRow.getParent();
        
        // Get table dataset key (set to Objects if null)
        String datasetKey = table.getDatasetKey();
        if(datasetKey==null)
            datasetKey = "Objects";
        
        // Get map for table dataset key (create and add if missing)
        Map tableMap = (Map)get(datasetKey);
        if(tableMap==null)
            put(datasetKey, tableMap = new HashMap());

        // Get group for row (just continue if missing)
        RMGroup rowGroup = row.getGroup();
        if(rowGroup.getParent()==null)
            continue;
        
        // Get key for group
        String rowGroupKey = rowGroup.getParent().getKey();
        
        // Get list for for group (create and add if missing)
        List rowGroupList = (List)tableMap.get(rowGroupKey);
        if(rowGroupList==null) {
            rowGroupList = new ArrayList();
            tableMap.put(rowGroupKey, rowGroupList);
        }
        
        // Get last row group entered in our row group list
        RowGroup lastRowGroup = (RowGroup)ListUtils.getLast(rowGroupList);
        
        // Just continue if we've already seen this group
        if(lastRowGroup!=null && lastRowGroup._group==rowGroup)
            continue;
        
        // Add table of contents info for row and group
        rowGroupList.add(new RowGroup(row, rowGroup));
    }
}

/**
 * An inner class to assist flattening.
 */
public static class RowGroup implements RMKey.Get {

    // The row shape for this entry
    RMShape _row;
    
    // The row group for this entry
    Object  _group;
    
    /** Creates a new table of contents entry. */
    public RowGroup(RMShape row, Object group) { _row = row; _group = group; }
    
    /** Implements this method to provide page info. */
    public Object getKeyValue(String aKey)
    {
        // Return value for "page" (really returns another key)
        if(aKey.equals("page"))
            return RMKeyChain.getKeyChain("PageMax-" + (_row.pageMax() - _row.page()));
        
        // Return value for "row"
        if(aKey.equals("row"))
            return ((RMGroup)_group).index() + 1;
        
        // Forward all else on to group
        return RMKey.getValue(_group, aKey);
    }
    
    /** Standard equals implementation. */
    public boolean equals(Object anObj)
    {
        if(anObj==this) return true;
        RowGroup other = anObj instanceof RowGroup? (RowGroup)anObj : null; if(other==null) return false;
        return SnapUtils.equals(other._group, _group);
    }
}

/**
 * Checks for table of contents page in given template, and copies page as-is if found.
 */
public static boolean checkForTableOfContents(RMPage aPage)
{
    // If page has table with DatasetKey that starts with "TOC." return true
    RMTable table = aPage.getChildWithClass(RMTable.class);
    if(table!=null && table.getDatasetKey()!=null && table.getDatasetKey().startsWith("TOC."))
        return true;
    return false;
}

/**
 * Re-run report with table of contents.
 */
public static void rpgPage(ReportOwner anRptOwner, RMDocument aDocRPG, RMPage aPage, int anIndex)
{
    Map toc = new RMTableOfContents(aDocRPG);
    toc = Collections.singletonMap("TOC", toc);
    anRptOwner.pushDataStack(toc);
    RMParentShape crpg = (RMParentShape)anRptOwner.rpg(aPage, aDocRPG);
    if(crpg instanceof ReportOwner.ShapeList) {
        for(RMShape pg : crpg.getChildArray()) aDocRPG.addPage((RMPage)pg, anIndex++); }
    else aDocRPG.addPage((RMPage)crpg, anIndex);
    aDocRPG.setSelPageIndex(0);
}

}