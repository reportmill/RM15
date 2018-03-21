/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.ListUtils;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * Tool for Switch shape.
 */
public class RMSwitchShapeTool <T extends RMSwitchShape> extends RMParentShapeTool <T> {

/**
 * Returns the shape class this tool edits.
 */
public Class getShapeClass() { return RMSwitchShape.class; }

/**
 * Returns the window title for this tool
 */
public String getWindowTitle() { return "SwitchShape Inspector"; }

/**
 * Initialize UI panel for this tool.
 */
protected void initUI()
{
    ListView <String> switchList = getView("SwitchList", ListView.class);
    switchList.setCellConfigure(this :: configureSwitchList);
}

/**
 * Reset UI panel controls
 */
public void resetUI()
{
    // Get currently selected switch shape and versions (just return if null)
    RMSwitchShape shape = getSelectedShape(); if(shape==null) return;
    List <String> versions = getVersionNames(); if(versions==null) return;
    
    // Update SwitchList Items and SelItem
    setViewItems("SwitchList", versions);
    setViewSelItem("SwitchList", shape.getVersion());
    
    // Update VersionKeyText
    setViewValue("VersionKeyText", shape.getVersionKey());
}

public void respondUI(ViewEvent anEvent)
{
    // Get currently selected switch shape (just return if null)
    RMSwitchShape shape = getSelectedShape(); if(shape==null) return;
    
    // Register for repaint (and thus undo)
    shape.repaint();

    // Handle SwitchList
    if(anEvent.equals("SwitchList")) {
        shape.undoerSetUndoTitle("Change Version");
        shape.setVersion(anEvent.getStringValue());
    }
    
    // Handle ClearButton (either remove version or beep if they try to remove standard)
    if(anEvent.equals("ClearButton")) {
        String version = shape.getVersion();
        if(version.equals(shape.getDefaultVersionName()))
            beep();
        else {
            shape.undoerSetUndoTitle("Remove Version");
            shape.removeVersion(version);
        }
    }
    
    // Handle _addButton
    if(anEvent.equals("AddButton")) {
        String msg = "Enter label for custom version:", title = "Custom Version";
        DialogBox dbox = new DialogBox(title); dbox.setQuestionMessage(msg);
        String version = dbox.showInputDialog(getUI(), null);
        if(version != null) {
            shape.repaint(); // Because last one is eaten by OptionPane event loop
            shape.undoerSetUndoTitle("Change Version");
            shape.setVersion(version);
        }
    }
    
    // Handle VersionKeyText
    if(anEvent.equals("VersionKeyText"))
        shape.setVersionKey(anEvent.getStringValue());
}

/**
 * Get list of shape versions, plus default versions.
 */
public List <String> getVersionNames()
{
    RMSwitchShape s = getSelectedShape(); if(s==null) return Collections.emptyList();
    List names = s.getVersionNames();
    ListUtils.moveToFront(names, "Standard");
    return names;
}

/**
 * Configure SortsTable.
 */
public void configureSwitchList(ListCell <String> aCell)
{
    RMSwitchShape shape = getSelectedShape(); if(shape==null) return;
    String version = aCell.getItem(); if(version==null) return;
    if(shape.hasVersion(version)) aCell.setFont(aCell.getFont().getBold());
}

}