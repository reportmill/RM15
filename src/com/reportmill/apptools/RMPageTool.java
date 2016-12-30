/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.gfx.Image;
import snap.view.*;

/**
 * This class provides UI editing for RMPage shapes.
 */
public class RMPageTool <T extends RMPage> extends RMParentShapeTool <T> {
    
    // The Layers table
    TableView <RMPageLayer>  _layersTable;

    // Icons
    Image                    _visibleIcon = getImage("LayerVisible.png");
    Image                    _invisibleIcon = getImage("LayerInvisible.png");
    Image                    _lockedIcon = getImage("LayerLocked.png");

/**
 * Initialize UI panel for this tool.
 */
protected void initUI()
{
    // Configure LayersTable CellConfigure and MouseClicks
    _layersTable = getView("LayersTable", TableView.class);
    _layersTable.setCellConfigure(this :: configureLayersTable);
    enableEvents(_layersTable, MouseRelease);
    enableEvents("DatasetKeyText", DragDrop);
}

/**
 * Updates the UI controls from currently selected page.
 */
public void resetUI()
{
    // Get currently selected page (just return if null)
    RMPage page = getSelectedShape(); if(page==null) return;
        
    // Update AddButton, RemoveButton, RenameButton, MergeButton enabled state
    setViewEnabled("AddButton", page.getLayerCount()>0);
    setViewEnabled("RemoveButton", page.getLayerCount()>1);
    setViewEnabled("RenameButton", page.getLayerCount()>0);
    setViewEnabled("MergeButton", page.getLayerCount()>1 && page.getSelectedLayerIndex()>0);
    
    // Update layers table selection
    _layersTable.setItems(page.getLayers());
    _layersTable.setSelectedIndex(page.getSelectedLayerIndex());
}

/**
 * Updates currently selected page from the UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected page (just return if null)
    RMPage page = getSelectedShape(); if(page==null) return;

    // Handle DatasetKeyText
    if(anEvent.equals("DatasetKeyText") && anEvent.isDragDropEvent())
        page.setDatasetKey(anEvent.getStringValue().replace("@", ""));
    
    // Handle AddButton
    if(anEvent.equals("AddButton"))
        page.addLayerNamed("Layer " + (page.getLayerCount() + 1));

    // Handle RemoveButton
    if(anEvent.equals("RemoveButton")) {
        int index = getViewSelectedIndex("LayersTable");
        page.removeLayer(index);
    }

    // Handle MergeButton
    if(anEvent.equals("MergeButton")) {
        
        // Get selected layer and index
        RMPageLayer layer = page.getSelectedLayer();
        int index = page.getSelectedLayerIndex();
        
        // If index is less than layer count
        if(index<page.getLayerCount()) {
            RMPageLayer resultingLayer = page.getLayer(index - 1);
            resultingLayer.addChildren(layer.getChildren());
            layer.removeChildren();
            page.removeLayer(layer);
        }
    }
    
    // Handle RenameButton
    if(anEvent.equals("RenameButton")) {
        int srow = getViewSelectedIndex("LayersTable");
        RMPageLayer layer = page.getLayer(srow);
        DialogBox dbox = new DialogBox("Rename Layer"); dbox.setQuestionMessage("Layer Name:");
        String newName = dbox.showInputDialog(getUI(), layer.getName());
        if(newName!=null && newName.length()>0)
            layer.setName(newName);
    }
    
    // Handle LayersTable
    if(anEvent.equals("LayersTable")) {
        
        // Handle MouseClick event - have page select new table row
        int row = _layersTable.getSelectedIndex(); if(row<0) return;
        RMPageLayer layer = page.getLayer(row);
        page.selectLayer(layer);
        
        // If column one was selected, cycle through layer states
        int col = _layersTable.getSelectedCol();
        if(anEvent.isMouseClick() && col==1) {
            int state = layer.getLayerState();
            if(state==RMPageLayer.StateVisible) layer.setLayerState(RMPageLayer.StateInvisible);
            else if(state==RMPageLayer.StateInvisible) layer.setLayerState(RMPageLayer.StateLocked);
            else layer.setLayerState(RMPageLayer.StateVisible);
            _layersTable.updateItems();
        }
    }

    // Handle AllVisibleButton
    if(anEvent.equals("AllVisibleButton"))
        for(int i=0, iMax=page.getLayerCount(); i<iMax; i++)
            page.getLayer(i).setLayerState(RMPageLayer.StateVisible);
    
    // Handle AllVisibleButton
    if(anEvent.equals("AllInvisibleButton"))
        for(int i=0, iMax=page.getLayerCount(); i<iMax; i++)
            page.getLayer(i).setLayerState(RMPageLayer.StateInvisible);
    
    // Handle AllLockedButton
    if(anEvent.equals("AllLockedButton"))
        for(int i=0, iMax=page.getLayerCount(); i<iMax; i++)
            page.getLayer(i).setLayerState(RMPageLayer.StateLocked);
}

/**
 * Configure LayersTable: Set image for second column.
 */
public void configureLayersTable(ListCell <RMPageLayer> aCell)
{
    if(aCell.getCol()!=1) return;
    RMPage page = getSelectedShape(); if(page==null) return; int index = aCell.getRow();
    RMPageLayer layer = index>=0 && index<page.getLayerCount()? page.getLayer(index) : null;
    int state = layer!=null? layer.getLayerState() : -1;
    if(state==RMPageLayer.StateVisible) aCell.setImage(_visibleIcon);
    else if(state==RMPageLayer.StateInvisible) aCell.setImage(_invisibleIcon);
    else if(state==RMPageLayer.StateLocked) aCell.setImage(_lockedIcon);
    aCell.setText(null);
}

/**
 * Returns the shape class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMPage.class; }

/**
 * Returns the name to be used for this tool in the inspector window title.
 */
public String getWindowTitle()  { return "Page Inspector"; }

/**
 * Overrides tool method to declare that pages have no handles.
 */
public int getHandleCount(T aShape)  { return 0; }

}