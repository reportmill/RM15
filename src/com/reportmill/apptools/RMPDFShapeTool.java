/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.List;
import snap.util.*;
import snap.view.*;
import snap.viewx.FilePanel;

/**
 * Provides UI for RMPDFShape editing.
 */
public class RMPDFShapeTool <T extends RMPDFShape> extends RMTool <T> {
    
/**
 * Returns the class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMPDFShape.class; }

/**
 * Returns the string used for the inspector window title.
 */
public String getWindowTitle() { return "PDF Shape Tool"; }

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
    RMPDFShape image = getSelectedShape(); if(image==null) return;
    RMPDFData idata = image.getPDFData();
    
    // Reset KeyText, PageText, MarginsText, GrowToFitCheckBox, PreserveRatioCheckBox
    setViewValue("KeyText", image.getKey());
    setViewValue("PageText", image.getPageIndex()+1);
    setViewValue("PaddingText", StringUtils.toString(getUnitsFromPoints(image.getPadding())));
    setViewValue("GrowToFitCheckBox", image.isGrowToFit());
    setViewValue("PreserveRatioCheckBox", image.getPreserveRatio());
    
    // Reset TypeLabel
    if(idata==null) setViewValue("TypeLabel", "");
    else setViewValue("TypeLabel", "Type: PDF\nSize: " + idata.getWidth() + "x" + idata.getHeight()+
        " (" + (int)(image.getWidth()/idata.getWidth()*image.getScaleX()*100) + "%)");
    
    // Reset SaveButton enabled
    setViewEnabled("SaveButton", idata!=null);
}

/**
 * Updates the currently selected image from the UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get selected image and images (just return if null)
    RMPDFShape image = getSelectedShape(); if(image==null) return;
    List <RMPDFShape> images = (List)getSelectedShapes();

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
        for(RMPDFShape im : images) im.setPadding(anEvent.getIntValue());
    
    // Handle GrowToFitCheckBox, PreserveRatioCheckBox
    if(anEvent.equals("GrowToFitCheckBox")) for(RMPDFShape im : images) im.setGrowToFit(anEvent.getBoolValue());
    if(anEvent.equals("PreserveRatioCheckBox")) for(RMPDFShape im:images) im.setPreserveRatio(anEvent.getBoolValue());
    
    // Handle SaveButton
    if(anEvent.equals("SaveButton")) {
        RMPDFData idata = image.getPDFData(); if(idata==null) return;
        String path = FilePanel.showSavePanel(getEditor(), "PDF File", "pdf"); if(path==null) return;
        SnapUtils.writeBytes(idata.getBytes(), path);
    }
}

/**
 * Returns the image used to represent shapes that this tool represents.
 */
protected snap.gfx.Image getImageImpl()  { return getToolForClass(RMImageShape.class).getImage(); }

}