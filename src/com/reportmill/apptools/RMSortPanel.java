/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.base.RMGrouping;
import com.reportmill.base.RMSort;
import com.reportmill.shape.*;
import snap.gfx.Image;
import snap.util.StringUtils;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * Provides UI for configuring a grouping for a tool.
 */
public class RMSortPanel extends ViewOwner {

    // The owner of this sort panel
    Owner               _owner;

    // The sorts table
    TableView <RMSort>  _sortsTable;
    
    // Images used for panel
    Image SortAscIcon = getImage("SortAscending.png");
    Image SortDescIcon = getImage("SortDescending.png");
    
/**
 * Creates a new sort panel instance.
 */
public RMSortPanel(Owner anOwner)  { _owner = anOwner; }

/**
 * An interface for SortPanelOwner
 */
public interface Owner {

    // SortPanel calls this as first line of SortPanel respondUI
    public void respondUI(ViewEvent anEvent);
    
    // Returns the selected shape that is being edited
    public RMShape getSelectedShape();
    
    // Returns the grouping that we modify
    public RMGrouping getGrouping();
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get sorts table and configure
    _sortsTable = getView("SortsTable", TableView.class);
    _sortsTable.setCellConfigure(this :: configureSortsTable);
    enableEvents(_sortsTable, MouseRelease, DragDrop); // So we get called for click on sort order
    
    // Configure TopNSortButton
    enableEvents("TopNSortButton", MouseRelease);
    enableEvents("TopNKeyText", DragDrop);
}

/**
 * Resets the UI controls.
 */
public void resetUI()
{
    // Get grouping
    RMGrouping grouping = _owner.getGrouping();
    
    // If grouping is null, disable everything and return
    if(grouping==null) { getUI().setEnabled(false); return; }
    else getUI().setEnabled(true);
    
    // Update SortButton, TopNButton, ValuesButton
    int sp_index = getViewSelIndex("SortPanel");
    setViewValue("SortButton", sp_index==0);
    setViewValue("TopNButton", sp_index==1);
    setViewValue("ValuesButton", sp_index==2);
    
    // Update SortingTable
    _sortsTable.setItems(grouping.getSorts());
    _sortsTable.setSelIndex(grouping.getSelectedSortIndex()); //_sortsTable.repaint();
    
    // Update TopNKeyText, TopNCountText, TopNInclCheckBox, TopNPadCheckBox
    setViewValue("TopNKeyText", grouping.getTopNSort().getKey());
    setViewValue("TopNCountText", grouping.getTopNSort().getCount());
    setViewValue("TopNInclCheckBox", grouping.getTopNSort().getIncludeOthers());
    setViewValue("TopNPadCheckBox", grouping.getTopNSort().getPad());
    
    // Update TopNSortButton
    Image simage = grouping.getTopNSort().getOrder()==RMSort.ORDER_DESCEND? SortDescIcon : SortAscIcon;
    getView("TopNSortButton", Label.class).setImage(simage);
    
    // Update ValuesText, SortOnValuesCheckBox, IncludeValuesCheckBox
    setViewValue("ValuesText", grouping.getValuesString());
    setViewValue("SortOnValuesCheckBox", grouping.getSortOnValues());
    setViewValue("IncludeValuesCheckBox", grouping.getIncludeValues());
}

/**
 * Responds to changes to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Forward on to tool respondUI
    _owner.respondUI(anEvent);
    
    // Get the selected shape
    RMShape shape = _owner.getSelectedShape();
    
    // Get current grouping
    RMGrouping grouping = _owner.getGrouping(); if(grouping==null) return;
    
    // Handle SortButton, TopNButton, ValuesButton
    if(anEvent.equals("SortButton")) setViewSelIndex("SortPanel", 0);
    if(anEvent.equals("TopNButton")) setViewSelIndex("SortPanel", 1);
    if(anEvent.equals("ValuesButton")) setViewSelIndex("SortPanel", 2);
    
    // Handle SortingTable
    if(anEvent.equals("SortsTable")) {
        
        // Handle drop: Get drop string and add sort
        if(anEvent.isDragDrop()) {    //int toRow = _sortsTable.rowAtPoint(anEvent.getLocation());
            String string = anEvent.getClipboard().getString(); string = StringUtils.delete(string, "@");
            shape.undoerSetUndoTitle("Add Sort Order");
            grouping.addSort(new RMSort(string));
            anEvent.dropComplete();
        }
        
        // Handle selection
        else {
        
            // Get row and column of SortingTable selection and set grouping SelectedSortIndex
            int row = _sortsTable.getSelRow();
            int col = _sortsTable.getSelCol();
            grouping.setSelectedSortIndex(row);
        
            // If selected sort order column, flip selected sort
            if(anEvent.isMouseRelease() && col==1) {
                shape.undoerSetUndoTitle("Flip Sort Ordering");
                grouping.getSelectedSort().toggleOrder();
                _sortsTable.updateItems();
            }
        }
    }
    
    // Handle AddSortMenuItem
    if(anEvent.equals("AddSortMenuItem")) {
        
        // Get key from input dialog
        DialogBox dbox = new DialogBox("Add Sorting Key"); dbox.setQuestionMessage("Sorting Key:");
        String key = dbox.showInputDialog(getUI(), null);
        
        // If key was entered, add it to grouping
        if(key!=null && key.length()>0) {
            shape.undoerSetUndoTitle("Add Sort Order");
            grouping.addSort(new RMSort(key));
        }
    }
    
    // Handle RemoveSortMenuItem
    if(anEvent.equals("RemoveSortMenuItem") && grouping.getSelectedSort()!=null) {
        shape.undoerSetUndoTitle("Remove Sort Order");
        grouping.removeSort(grouping.getSelectedSort());
    }
    
    // Handle MoveSortUpMenuItem
    if(anEvent.equals("MoveSortUpMenuItem")) {
        int loc = _sortsTable.getSelRow();
        if(loc>0)
            grouping.moveSort(loc, loc - 1);        
    }
    
    // Handle MoveSortDownMenuItem
    if(anEvent.equals("MoveSortDownMenuItem")) {
        int loc = _sortsTable.getSelRow();
        if(loc<_sortsTable.getRowCount() - 1)
            grouping.moveSort(loc, loc + 1);        
    }
    
    // Handle TopNKeyText
    if(anEvent.equals("TopNKeyText")) {
        shape.undoerSetUndoTitle("TopN Sort Change");
        grouping.getTopNSort().setKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        if(grouping.getTopNSort().getCount()==0)
            grouping.getTopNSort().setCount(5);
        if(anEvent.isDragDrop())
            anEvent.dropComplete();
    }
    
    // Handle TopNCountText
    if(anEvent.equals("TopNCountText")) {
        shape.undoerSetUndoTitle("TopN Count Change");
        grouping.getTopNSort().setCount(anEvent.getIntValue());
    }
    
    // Handle TopNSortButton, TopNInclCheckBox, TopNPadCheckBox
    if(anEvent.equals("TopNSortButton")) grouping.getTopNSort().toggleOrder();
    if(anEvent.equals("TopNInclCheckBox")) grouping.getTopNSort().setIncludeOthers(anEvent.getBoolValue());
    if(anEvent.equals("TopNPadCheckBox")) grouping.getTopNSort().setPad(anEvent.getBoolValue());
    
    // Handle ValuesText, SortOnValuesCheckBox, IncludeValuesCheckBox
    if(anEvent.equals("ValuesText")) grouping.setValuesString(anEvent.getStringValue());
    if(anEvent.equals("SortOnValuesCheckBox")) grouping.setSortOnValues(anEvent.getBoolValue());
    if(anEvent.equals("IncludeValuesCheckBox")) grouping.setIncludeValues(anEvent.getBoolValue());
}

/**
 * Sets the selected pane.
 */
public void setSelectedPane(int anIndex)  { setViewValue("SortPanel", anIndex); }

/**
 * Configure SortsTable.
 */
public void configureSortsTable(ListCell <RMSort> aCell)
{
    // Get sort
    RMSort sort = aCell.getItem(); if(sort==null) return;
    int col = aCell.getCol();
    
    // Handle column 0
    if(col==0) { String key = sort.getKey();
        aCell.setText(key);
        aCell.setToolTip(key);
    }
        
    // Handle column 1
    if(col==1) {
        if(sort.getOrder()==RMSort.ORDER_ASCEND) aCell.setImage(SortAscIcon);
        else aCell.setImage(SortDescIcon);
        aCell.setText(null);
    }
}

}