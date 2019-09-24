/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.view.*;
import snap.viewx.DialogBox;

/**
 * Provides UI inspection for GraphPartBars.
 */
public class RMGraphPartPieTool extends RMTool {

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Configure ExtrusionComboBox
    setViewItems("ExtrusionComboBox", RMGraphPartPie.EXTRUSIONS);
}

/**
 * Resets the UI panel controls.
 */
public void resetUI()
{
    // Get the selected part pie (just return if null)
    RMGraphPartPie pie = getSelectedShape(); if(pie==null) return;

    // Update WedgeLinesCheckBox
    setViewValue("WedgeLinesCheckBox", pie.getDrawWedgeLabelLines());
    
    // Update ExtrusionComboBox
    String extrusionKey = pie.getExtrusionKey();
    setViewSelItem("ExtrusionComboBox", extrusionKey);
    
    // Update HoleRatioSlider, HoleRatioText
    setViewValue("HoleRatioSlider", pie.getHoleRatio()*100);
    setViewValue("HoleRatioText", Math.round(pie.getHoleRatio()*100));
}

/**
 * Responds to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the selected part pie (just return if null)
    RMGraphPartPie pie = getSelectedShape(); if(pie==null) return;

    // Handle WedgeLinesCheckBox
    if(anEvent.equals("WedgeLinesCheckBox"))
        pie.setDrawWedgeLabelLines(anEvent.getBoolValue());

    // Handle ExtrusionComboBox
    if(anEvent.equals("ExtrusionComboBox")) {
        pie.setExtrusionKey(anEvent.getStringValue());
        if(pie.getExtrusionKey().equals(RMGraphPartPie.EXTRUDE_CUSTOM)) {
            DialogBox dbox = new DialogBox("Custom Extrusion Key Panel"); dbox.setQuestionMessage("Extrusion Key:");
            String key = dbox.showInputDialog(getUI(), pie.getExtrusionKey());
            if(key!=null)
                pie.setExtrusionKey(key);
        }
    }
    
    // Handle HoleRatioSlider, HoleRatioText
    if(anEvent.equals("HoleRatioSlider") || anEvent.equals("HoleRatioText")) {
        pie.undoerSetUndoTitle("Hole Ratio Change");
        double hratio = anEvent.getFloatValue()/100;
        pie.setHoleRatio(hratio);
    }
}

/**
 * Returns the currently selected RMGraphPartPie.
 */
public RMGraphPartPie getSelectedShape()
{
    RMGraph graph = getSelectedGraph();
    return graph!=null? graph.getPie() : null;
}

/**
 * Returns the currently selected graph shape.
 */
public RMGraph getSelectedGraph()
{
    RMShape selShape = super.getSelectedShape();
    return selShape instanceof RMGraph? (RMGraph)selShape : null;
}

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Pie Wedge Inspector"; }

}