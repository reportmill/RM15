/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.shape.*;
import com.reportmill.shape.RMGraphPartSeries.LabelPos;
import snap.util.StringUtils;
import snap.view.*;

/**
 * RMTool subclass to provide UI editing for RMGraphPartSeries.
 */
public class RMGraphPartSeriesTool <T extends RMGraphPartSeries> extends RMTool <T> {

    // The selected series index
    int      _selIndex;
    
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
    
    // Update TitleText, LabelPositionsList
    setViewValue("TitleText", series.getTitle());
    setViewSelItem("LabelPositionsList", series.getPosition());
    
    // Update SeriesText, LabelRollSpinner
    setViewValue("SeriesText", series.getLabelShape(series.getPosition()).getText());
    setViewValue("LabelRollSpinner", series.getRoll());
    
    // Update SeriesButtons
    RMGraph graph = getSelGraph();
    int seriesCount = graph.getSeriesCount();
    View seriesRowView = getView("SeriesButton1").getParent();
    seriesRowView.setVisible(seriesCount>1);

    // Update SeriesButtons
    if(seriesRowView.isVisible()) {
        int selIndex = getSelSeriesIndex() + 1;
        for(int i=1; i<=3; i++) {
            ToggleButton btn = getView("SeriesButton" + i, ToggleButton.class);
            btn.setSelected(i==selIndex);
            btn.setVisible(i-1<seriesCount);
        }
    }
}

/**
 * Responds to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the selected series shape
    RMGraphPartSeries series = getSelectedShape(); if(series==null) return;
    
    // Handle TitleText, SeriesText
    if(anEvent.equals("TitleText"))
        series.setTitle(anEvent.getStringValue());
    if(anEvent.equals("LabelPositionsList"))
        series.setPosition(LabelPos.valueOf(anEvent.getStringValue()));

    // LabelRollSpinner, LabelPositionsList
    if(anEvent.equals("SeriesText"))
        series.setLabelText(series.getPosition(), anEvent.getStringValue());
    if(anEvent.equals("LabelRollSpinner"))
        series.setRoll(anEvent.getFloatValue());
    
    // Handle SeriesButton
    String name = anEvent.getName();
    if(name.startsWith("SeriesButton")) {
        
        // Set SelSeriesIndex for button
        int ind = StringUtils.intValue(name);
        setSelSeriesIndex(ind);
        
        // Set Graph.ProxyShape
        RMGraph graph = getSelGraph();
        RMShape ps = graph.getSeries(ind);
        graph.setProxyShape(ps);
    }
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
 * Returns the selected series index.
 */
public int getSelSeriesIndex()
{
    RMGraph graph = getSelGraph();
    _selIndex = Math.max(_selIndex, graph!=null? graph.getSeriesCount() -1 : -1);
    return _selIndex;
}

/**
 * Sets the selected series index.
 */
public void setSelSeriesIndex(int anIndex)
{
    if(anIndex==_selIndex) return;
    _selIndex = anIndex;
}

/**
 * Returns the currently selected RMGraphPartSeries.
 */
public T getSelectedShape()  { return (T)getSelSeries(); }

/**
 * Returns the currently selected RMGraphPartSeries.
 */
public RMGraphPartSeries getSelSeries()
{
    RMGraph graph = getSelGraph(); if(graph==null) return null;
    int ind = getSelSeriesIndex(); if(ind<0) return null;
    return graph.getSeries(ind);
}

/**
 * Returns the currently selected graph area shape.
 */
public RMGraph getSelGraph()
{
    RMEditor e = getEditor(); if(e==null) return null;
    RMShape selShape = e.getSelectedOrSuperSelectedShape();
    return selShape instanceof RMGraph? (RMGraph)selShape : null;
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