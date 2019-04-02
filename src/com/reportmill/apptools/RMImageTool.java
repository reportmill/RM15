/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.List;
import snap.gfx.Image;
import snap.util.*;
import snap.view.*;
import snap.viewx.FilePanel;

/**
 * Provides UI for RMImage shape editing.
 */
public class RMImageTool <T extends RMImageShape> extends RMTool <T> {
    
/**
 * Returns the class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMImageShape.class; }

/**
 * Returns the string used for the inspector window title.
 */
public String getWindowTitle() { return "Image Tool"; }

/**
 * Initialize UI.
 */
protected void initUI()  { enableEvents("KeyText", DragDrop); }

/**
 * Updates the UI controls from the currently selected image.
 */
public void resetUI()
{    
    // Get selected image shape and image (just return if null)
    RMImageShape imgShp = getSelectedShape(); if(imgShp==null) return;
    Image img = imgShp.getImage();
    
    // Reset KeyText, MarginsText, GrowToFitCheckBox, PreserveRatioCheckBox
    setViewValue("KeyText", imgShp.getKey());
    setViewValue("PaddingText", StringUtils.toString(getUnitsFromPoints(imgShp.getPadding())));
    setViewValue("GrowToFitCheckBox", imgShp.isGrowToFit());
    setViewValue("PreserveRatioCheckBox", imgShp.getPreserveRatio());
    
    // Reset RoundingThumb and RoundingText
    setViewValue("RoundingThumb", imgShp.getRadius());
    setViewValue("RoundingText", imgShp.getRadius());
    
    // Reset TypeLabel
    if(img==null) setViewValue("TypeLabel", "");
    else setViewValue("TypeLabel", "Type: " + img.getType() + "\nSize: " + img.getPixWidth() + "x" + img.getPixHeight()+
        " (" + (int)(imgShp.getWidth()/img.getWidth()*imgShp.getScaleX()*100) + "%)");
    
    // Reset SaveButton, JPEGButton enabled
    setViewEnabled("SaveButton", img!=null);
    setViewEnabled("JPEGButton", img!=null && !img.getType().equals("jpg"));
}

/**
 * Updates the currently selected image from the UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get selected image shape and image shapes (just return if null)
    RMImageShape imgShp = getSelectedShape(); if(imgShp==null) return;
    List <RMImageShape> images = (List)getSelectedShapes();

    // Handle KeyText
    if(anEvent.equals("KeyText"))
        imgShp.setKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        
    // Handle KeysButton
    if(anEvent.equals("KeysButton"))
        getEditorPane().getAttributesPanel().setVisibleName(AttributesPanel.KEYS);

    // Handle PaddingText
    if(anEvent.equals("PaddingText"))
        for(RMImageShape im : images) im.setPadding(anEvent.getIntValue());
    
    // Handle GrowToFitCheckBox, PreserveRatioCheckBox
    if(anEvent.equals("GrowToFitCheckBox"))
        for(RMImageShape im : images) im.setGrowToFit(anEvent.getBoolValue());
    if(anEvent.equals("PreserveRatioCheckBox"))
        for(RMImageShape im : images) im.setPreserveRatio(anEvent.getBoolValue());
    
    // Handle Rounding Radius Thumb & Text
    if(anEvent.equals("RoundingThumb") || anEvent.equals("RoundingText")) {
        imgShp.undoerSetUndoTitle("Rounding Change");
        float value = anEvent.getFloatValue();
        for(RMImageShape im : images) {
            im.setRadius(value);
            if(im.getStroke()==null)
                im.setStroke(new RMStroke());
        }
    }
    
    // Handle SaveButton
    if(anEvent.equals("SaveButton")) {
        Image img = imgShp.getImage(); if(img==null) return;
        String type = img.getType(); if(StringUtils.length(type)==0) return;
        String path = FilePanel.showSavePanel(getEditor(), type.toUpperCase() + " File", type); if(path==null) return;
        SnapUtils.writeBytes(img.getBytes(), path);
    }
    
    // Handle JPEGButton
    if(anEvent.equals("JPEGButton")) {
        Image img = imgShp.getImage(); if(img==null) return;
        byte jpegBytes[] = img.getBytesJPEG();
        imgShp.setImageForSource(jpegBytes);
    }
}

}