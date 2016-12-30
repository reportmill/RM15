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
    // Get selected image, image fill, image data and fill style (just return if null)
    RMImageShape image = getSelectedShape(); if(image==null) return;
    RMImageData idata = image.getImageData();
    
    // Reset KeyText, PageText, MarginsText, GrowToFitCheckBox, PreserveRatioCheckBox
    setViewValue("KeyText", image.getKey());
    setViewValue("PageText", image.getPageIndex()+1);
    setViewValue("PaddingText", StringUtils.toString(getUnitsFromPoints(image.getPadding())));
    setViewValue("GrowToFitCheckBox", image.isGrowToFit());
    setViewValue("PreserveRatioCheckBox", image.getPreserveRatio());
    
    // Reset RoundingThumb and RoundingText
    setViewValue("RoundingThumb", image.getRadius());
    setViewValue("RoundingText", image.getRadius());
    
    // Reset TypeLabel
    if(idata==null || idata==RMImageData.EMPTY) setViewValue("TypeLabel", "");
    else setViewValue("TypeLabel", "Type: " + idata.getType() + "\nSize: " + idata.getWidth() + "x" + idata.getHeight()+
        " (" + (int)(image.getWidth()/idata.getWidth()*image.getScaleX()*100) + "%)");
    
    // Reset SaveButton enabled
    setViewEnabled("SaveButton", idata!=null);
    
    // Reset JPEGButton enabled
    setViewEnabled("JPEGButton", idata!=null && !idata.getType().equals("jpg"));
}

/**
 * Updates the currently selected image from the UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get selected image and images (just return if null)
    RMImageShape image = getSelectedShape(); if(image==null) return;
    List <RMImageShape> images = (List)getSelectedShapes();

    // Handle KeyText
    if(anEvent.equals("KeyText"))
        image.setKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        
    // Handle KeysButton
    if(anEvent.equals("KeysButton"))
        getEditorPane().getAttributesPanel().setVisibleName(AttributesPanel.KEYS);

    // Handle PageText
    if(anEvent.equals("PageText"))
        image.setPageIndex(anEvent.getIntValue()-1);
    
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
        image.undoerSetUndoTitle("Rounding Change");
        float value = anEvent.getFloatValue();
        for(RMImageShape im : images) {
            im.setRadius(value);
            if(im.getStroke()==null)
                im.setStroke(new RMStroke());
        }
    }
    
    // Handle SaveButton
    if(anEvent.equals("SaveButton")) {
        RMImageData idata = image.getImageData(); if(idata==null) return;
        String type = idata.getType(); if(StringUtils.length(type)==0) return;
        FileChooser fc = getEnv().getFileChooser(); fc.setDesc(type.toUpperCase() + " File"); fc.setExts(type);
        String path = fc.showOpenPanel(getEditor());
        //String path = FileChooserUtils.showChooser(true, getEditor(), type.toUpperCase() + " File", "." + type);
        if(path==null) return;
        SnapUtils.writeBytes(idata.getBytes(), path);
    }
    
    // Handle JPEGButton
    if(anEvent.equals("JPEGButton")) {
        RMImageData idata = image.getImageData(); if(idata==null) return;
        Image img = idata.getImage(); if(img==null) return;
        byte jpegBytes[] = img.getBytesJPEG();
        image.setImageData(jpegBytes);
    }
}

}