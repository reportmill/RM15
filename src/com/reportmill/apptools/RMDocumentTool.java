/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.app.RMEditorProxGuide;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * This class provides UI for configuring an RMDocument.
 */
public class RMDocumentTool <T extends RMDocument> extends RMTool <T> {
    
    // The array of supported paper sizes
    static Size      _paperSizes[];
    
    // The array of supported paper size names
    static String    _paperSizeNames[];

/**
 * Returns the class that tool edits.
 */
public Class getShapeClass()  { return RMDocument.class; }

/**
 * Returns the name to be show in inspector window.
 */
public String getWindowTitle()  { return "Document Inspector"; }

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Set PaperSizeComboBox paper size names and UnitComboBox Unit values
    setViewItems("PaperSizeComboBox", _paperSizeNames);
    setViewItems("UnitComboBox", RMDocument.Unit.values());
}

/**
 * Resets the UI controls from the current document.
 */
public void resetUI()
{
    // Get currently selected document and its page size
    RMDocument doc = getSelectedShape(); if(doc==null) return;
    RMPage page = doc.getSelPage();
    
    // Set PageWidthText and PageHeightText
    setViewValue("PageWidthText", getUnitsFromPointsStr(page.getWidth()));
    setViewValue("PageHeightText", getUnitsFromPointsStr(page.getHeight()));
    
    // Update PaperSizeComboBox: Get index of PaperName for Page.Size and set SelIndex
    int sindex = 0; for(int i=1; i<_paperSizeNames.length && sindex==0; i++) { Size size = _paperSizes[i];
        if(size.equals(page.getSize()) || size.equals(page.getHeight(), page.getWidth())) sindex = i; }
    setViewSelIndex("PaperSizeComboBox", sindex); // default to "custom"
    
    // Reset Units and orientation controls
    setViewValue("UnitComboBox", doc.getUnit());
    setViewValue("PortraitRadioButton", page.getHeight()>=page.getWidth());
    setViewValue("LandscapeRadioButton", page.getWidth()>page.getHeight());
    
    // Reset Margin controls
    setViewValue("LeftMarginText", getUnitsFromPointsStr(doc.getMarginLeft()));
    setViewValue("RightMarginText", getUnitsFromPointsStr(doc.getMarginRight()));
    setViewValue("TopMarginText", getUnitsFromPointsStr(doc.getMarginTop()));
    setViewValue("BottomMarginText", getUnitsFromPointsStr(doc.getMarginBottom()));
    setViewValue("DrawMarginCheckBox", doc.getShowMargin());
    setViewValue("SnapMarginCheckBox", doc.getSnapMargin());
    
    // Reset Grid controls
    setViewValue("ShowGridCheckBox", doc.getShowGrid());
    setViewValue("SnapGridCheckBox", doc.getSnapGrid());
    setViewValue("GridSpacingText", getUnitsFromPointsStr(doc.getGridSpacing()));
    
    // Reset Page Layout controls and null string text
    setViewValue("SingleRadio", doc.getPageLayout()==RMDocument.PageLayout.Single);
    setViewValue("DoubleRadio", doc.getPageLayout()==RMDocument.PageLayout.Double);
    setViewValue("FacingRadio", doc.getPageLayout()==RMDocument.PageLayout.Facing);
    setViewValue("ContinuousRadio", doc.getPageLayout()==RMDocument.PageLayout.Continuous);
    setViewValue("NullStringText", doc.getNullString());
    
    // Repaint PageSizeView
    getView("PageSizeView", PageSizeView.class).repaint();
    
    // Update ProximityGuideCheckBox, PaginateCheckBox, CompressCheckBox
    setViewValue("ProximityGuideCheckBox", RMEditorProxGuide.isEnabled());
    setViewValue("PaginateCheckBox", doc.isPaginate());
    setViewValue("CompressCheckBox", doc.getCompress());
}

/**
 * Responds to controls in UI to update current document.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get current document and page (just return if null)
    RMDocument doc = getSelectedShape(); if(doc==null) return;
    RMPage page = doc.getSelPage();
    
    // Set boolean for whether we need to resize window
    boolean resizeWindow = false;

    // Handle PageWidthText, PageHeightText
    if(anEvent.equals("PageWidthText") || anEvent.equals("PageHeightText")) {
        float w = getViewFloatValue("PageWidthText");
        float h = getViewFloatValue("PageHeightText");
        doc.setPageSize(getPointsFromUnits(w), getPointsFromUnits(h));
        resizeWindow = true;
    }
    
    // Handle PaperSizeComboBox
    if(anEvent.equals("PaperSizeComboBox")) {
        String sizeKey = anEvent.getStringValue();
        if(!sizeKey.equals("Custom")) {
            int index = ArrayUtils.indexOf(_paperSizeNames, sizeKey);
            double w = _paperSizes[index].width;
            double h = _paperSizes[index].height;
            doc.setPageSize(w, h);
        }
        resizeWindow = true;
    }
    
    // Handle UnitComboBox
    if(anEvent.equals("UnitComboBox"))
        doc.setUnit((RMDocument.Unit)anEvent.getValue());
    
    // Handle PortraitRadioButton, LandscapeRadioButton
    if((anEvent.equals("PortraitRadioButton") && page.getWidth()>page.getHeight()) ||
        (anEvent.equals("LandscapeRadioButton") && page.getHeight()>page.getWidth())) {
        doc.setPageSize(page.getHeight(), page.getWidth());
        resizeWindow = true;
    }
    
    // Handle margin Texts
    if(anEvent.equals("LeftMarginText") || anEvent.equals("RightMarginText") ||
        anEvent.equals("TopMarginText") || anEvent.equals("BottomMarginText")) {
        float l = getViewFloatValue("LeftMarginText");
        float r = getViewFloatValue("RightMarginText");
        float t = getViewFloatValue("TopMarginText");
        float b = getViewFloatValue("BottomMarginText");
        doc.setMargins(getPointsFromUnits(l), getPointsFromUnits(r), getPointsFromUnits(t), getPointsFromUnits(b));
    }

    // Handle DrawMarginCheckBox, SnapMarginCheckBox
    if(anEvent.equals("DrawMarginCheckBox")) doc.setShowMargin(anEvent.getBoolValue());
    if(anEvent.equals("SnapMarginCheckBox")) doc.setSnapMargin(anEvent.getBoolValue());
    
    // Handle ShowGridCheckBox, SnapGridCheckBox, GridSpacingText
    if(anEvent.equals("ShowGridCheckBox")) doc.setShowGrid(anEvent.getBoolValue());
    if(anEvent.equals("SnapGridCheckBox")) doc.setSnapGrid(anEvent.getBoolValue());
    if(anEvent.equals("GridSpacingText")) doc.setGridSpacing(getPointsFromUnits(anEvent.getFloatValue()));
    
    // Handle Page Layout options: SingleRadio, DoubleRadio, FacingRadio and ContinuousRadio
    if(anEvent.equals("SingleRadio") || anEvent.equals("DoubleRadio") ||
        anEvent.equals("FacingRadio") || anEvent.equals("ContinuousRadio")) {
        String name = StringUtils.delete(anEvent.getName(), "Radio");
        doc.setPageLayout(name);
        resizeWindow = true;
    }

    // Handle NullStringText
    if(anEvent.equals("NullStringText")) doc.setNullString(anEvent.getStringValue());
    
    // Handle ProximityGuideCheckBox, PaginateCheckBox, CompressCheckBox
    if(anEvent.equals("ProximityGuideCheckBox")) RMEditorProxGuide.setEnabled(anEvent.getBoolValue());
    if(anEvent.equals("PaginateCheckBox")) doc.setPaginate(anEvent.getBooleanValue());
    if(anEvent.equals("CompressCheckBox")) doc.setCompress(anEvent.getBoolValue());
    
    // If page size changed, make sure window is right size
    if(resizeWindow) {
        if(getEditorPane().getWindow().isVisible() && !getEditorPane().getWindow().isMaximized())
            getEditorPane().getWindow().pack();
        getView("PageSizeView").repaint();
    }
}

/**
 * Overrides tool method to declare that documents have no handles.
 */
public int getHandleCount(T aShape)  { return 0; }

// Initialize page sizes and paper size names
static {
    _paperSizeNames = new String[15]; _paperSizes = new Size[15];
    _paperSizeNames[0] = "Custom"; _paperSizes[0] = new Size(612, 792);
    _paperSizeNames[1] = "Letter"; _paperSizes[1] = new Size(612, 792);
    _paperSizeNames[2] = "Legal"; _paperSizes[2] = new Size(612, 1008);
    _paperSizeNames[3] = "Tabloid"; _paperSizes[3] = new Size(792, 1224);
    _paperSizeNames[4] = "Exec"; _paperSizes[4] = new Size(540, 720);
    _paperSizeNames[5] = "#10 Env"; _paperSizes[5] = new Size(684, 306);
    _paperSizeNames[6] = "Banner"; _paperSizes[6] = new Size(500, 100);
    _paperSizeNames[7] = "Small"; _paperSizes[7] = new Size(320, 240);
    _paperSizeNames[8] = "Medium"; _paperSizes[8] = new Size(640, 480);
    _paperSizeNames[9] = "Large"; _paperSizes[9] = new Size(800, 600);
    _paperSizeNames[10] = "A3"; _paperSizes[10] = new Size(842, 1190);
    _paperSizeNames[11] = "A4"; _paperSizes[11] = new Size(595, 842);
    _paperSizeNames[12] = "A5"; _paperSizes[12] = new Size(420, 595);
    _paperSizeNames[13] = "B4"; _paperSizes[13] = new Size(729, 1032);
    _paperSizeNames[14] = "B5"; _paperSizes[14] = new Size(516, 729);
}

/** An inner class to render Page control. */
public static class PageSizeView extends View {
    public void paintFront(Painter aPntr) {
        RMDocumentTool docTool = getOwner(RMDocumentTool.class);
        RMEditor editor = docTool.getEditor();
        Size pageSize = editor.getDoc().getPageSize();
        double maxHeight = Math.max(17*72, pageSize.height);
        double scale = (getHeight()-10)/maxHeight;
        double pageW = Math.round(pageSize.width*scale);
        double pageH = Math.round(pageSize.height*scale);
        double pageX = Math.round((getWidth() - pageW)/2f);
        double pageY = Math.round((getHeight() - pageH)/2f);
        aPntr.setColor(Color.BLACK); aPntr.fillRect(pageX+5, pageY+5, pageW, pageH);
        aPntr.setColor(Color.WHITE); aPntr.fillRect(pageX, pageY, pageW, pageH);
        aPntr.setColor(Color.BLACK); aPntr.drawRect(pageX, pageY, pageW, pageH);
    }
}

}