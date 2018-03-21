/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.view.ViewEvent;

/**
 * Tool for visual editing RMScene3D.
 */
public class RMScene3DTool <T extends RMScene3D> extends RMTool <T> {
    
    // The scene3d control for rotating selected scene3d
    RMTrackballControl  _sceneControl;
    
/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get TrackballControl
    _sceneControl = getView("TrackballControl", RMTrackballControl.class);
    
    // Initialize RenderingComboBox
    setViewItems("RenderingComboBox", new String[] { "Real 3D", "Pseudo 3D" });
}

/**
 * Updates UI panel from currently selected scene3d.
 */
public void resetUI()
{
    // Get the selected scene
    RMScene3D scene = getSelectedShape(); if(scene==null) return;
    
    // Reset Rendering radio buttons
    setViewSelIndex("RenderingComboBox", scene.isPseudo3D()? 1 : 0);
    
    // Reset YawSpinner, PitchSpinner, RollSpinner
    setViewValue("YawSpinner", Math.round(scene.getYaw()));
    setViewValue("PitchSpinner", Math.round(scene.getPitch()));
    setViewValue("RollSpinner", Math.round(scene.getRoll3D()));
    
    // Reset scene control
    _sceneControl.syncFrom(scene);
    
    // Reset Depth slider/text
    setViewValue("DepthSlider", scene.getDepth());
    setViewValue("DepthText", scene.getDepth());
    
    // Reset Field of view slider/text
    setViewValue("FOVSlider", scene.getFocalLength()/72);
    setViewValue("FOVText", scene.getFocalLength()/72);
}

/**
 * Updates currently selected scene 3d from UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the currently selected scene3d
    RMScene3D scene = getSelectedShape(); if(scene==null) return;
    
    // Handle RenderingComboBox
    if(anEvent.equals("RenderingComboBox")) {
        scene.setPseudo3D(anEvent.getSelIndex()==1);
        scene.setDefaultViewSettings();
    }
    
    // Handle YawSpinner, PitchSpinner, RollSpinner
    if(anEvent.equals("YawSpinner"))
        scene.setYaw(anEvent.getFloatValue());
    if(anEvent.equals("PitchSpinner"))
        scene.setPitch(anEvent.getFloatValue());
    if(anEvent.equals("RollSpinner"))
        scene.setRoll3D(anEvent.getFloatValue());

    // Handle Scene3DControl
    if(anEvent.equals("TrackballControl"))
        _sceneControl.syncTo(scene);
    
    // Handle DepthSlider and DepthText
    if(anEvent.equals("DepthSlider") || anEvent.equals("DepthText"))
        scene.setDepth(anEvent.equals("DepthSlider")? anEvent.getIntValue() : anEvent.getFloatValue());

    // Handle FOVSlider or FOVText
    if(anEvent.equals("FOVSlider") || anEvent.equals("FOVText"))
        scene.setFocalLength(anEvent.equals("FOVSlider")? anEvent.getIntValue()*72 : anEvent.getFloatValue()*72);
}

/**
 * Returns the class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMScene3D.class; }

/**
 * Returns the name of this tool for the inspector window.
 */
public String getWindowTitle()  { return "Scene3D Inspector"; }

/**
 * Overridden to make scene3d super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make scene3d not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Event handler for editing.
 */    
public void mousePressed(T aScene3D, ViewEvent anEvent)
{
    // If shape isn't super selected, just return
    if(!isSuperSelected(aScene3D)) return;
    
    // Forward mouse pressed to scene and consume event
    aScene3D.processEvent(createShapeEvent(aScene3D, anEvent));
    anEvent.consume();
}

/**
 * Event handler for editing.
 */
public void mouseDragged(T aScene3D, ViewEvent anEvent)
{
    // Forward mouse pressed to scene and consume event
    aScene3D.processEvent(createShapeEvent(aScene3D, anEvent));
    anEvent.consume();
}

/**
 * Event handler for editing.
 */
public void mouseReleased(T aScene3D, ViewEvent anEvent)
{
    // Forward mouse pressed to scene and consume event
    aScene3D.processEvent(createShapeEvent(aScene3D, anEvent));
    anEvent.consume();
}

}