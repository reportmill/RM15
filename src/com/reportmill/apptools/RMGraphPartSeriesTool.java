/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import com.reportmill.shape.RMGraphPartSeries.LabelPos;
import snap.view.*;

/**
 * Provides UI editing for graph part series.
 */
public class RMGraphPartSeriesTool <T extends RMGraphPartSeries> extends RMTool <T> {

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Set LabelPositionsList Model and CellConfigure
    ListView <LabelPos> labelPositionsList = getView("LabelPositionsList", ListView.class);
    labelPositionsList.setItems(RMGraphPartSeries.LabelPos.values());
    labelPositionsList.setCellConfigure(this :: configureLabelsPositionListCell);
    enableEvents("TitleText", DragDrop);
    enableEvents("SeriesText", DragDrop);
}

/**
 * Resets UI panel controls.
 */
public void resetUI()
{
    // Get the selected series shape
    RMGraphPartSeries series = getSelectedShape(); if(series==null) return;
    
    // Update TitleText, SeriesText, LabelRollSpinner, LabelPositionsList
    setViewValue("TitleText", series.getTitle());
    setViewValue("SeriesText", series.getLabelShape(series.getPosition()).getText());
    setViewValue("LabelRollSpinner", series.getRoll());
    setViewSelItem("LabelPositionsList", series.getPosition());
}

/**
 * Responds to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the selected series shape
    RMGraphPartSeries series = getSelectedShape(); if(series==null) return;
    
    // Handle TitleText, SeriesText, LabelRollSpinner, LabelPositionsList
    if(anEvent.equals("TitleText")) series.setTitle(anEvent.getStringValue());
    if(anEvent.equals("SeriesText")) series.getLabelShape(series.getPosition()).setText(anEvent.getStringValue());
    if(anEvent.equals("LabelRollSpinner")) series.setRoll(anEvent.getFloatValue());
    if(anEvent.equals("LabelPositionsList")) series.setPosition(LabelPos.valueOf(anEvent.getStringValue()));
    
    // Rebuild Graph
    RMGraph graph = (RMGraph)series.getParent();
    graph.relayout(); graph.repaint();
}

/**
 * Override to customize KeyFramesList rendering.
 */
private void configureLabelsPositionListCell(ListCell <LabelPos> aCell)
{
    LabelPos item = aCell.getItem(); if(item==null) return;
    RMGraphPartSeries series = getSelectedShape(); if(series==null) return;
    boolean active = series.getLabelShape(item).length()>0;
    if(active) aCell.setFont(aCell.getFont().getBold());
}

/**
 * Override to return tool shape class.
 */
public Class <T> getShapeClass()  { return (Class<T>)RMGraphPartSeries.class; }

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Series Inspector"; }

/**
 * Override to remove handles.
 */
public int getHandleCount(T aShape)  { return 0; }

}