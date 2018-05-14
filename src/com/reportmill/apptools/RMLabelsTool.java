/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.base.RMGrouping;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.SnapUtils;
import snap.util.StringUtils;
import snap.view.*;

/**
 * A tool for UI editing of labels shape. 
 */
public class RMLabelsTool extends RMTool implements RMSortPanel.Owner {

    // The sort panel
    RMSortPanel        _sortPanel;
    
    // The list of label formats
    List               _labelFormats;
    
    // The currently selected label format
    LabelFormat        _selectedLabelFormat;

/**
 * Initialize UI panel for this tool.
 */
protected void initUI()
{
    // Set model for LabelFormatsComboBox
    setViewItems("LabelFormatsComboBox", getLabelFormats());
    setViewSelIndex("LabelFormatsComboBox", 0); // Shouldn't need this, but I do
    
    // Get SortPanel, configure and install
    _sortPanel = new RMSortPanel(this);
    _sortPanel.getUI().setBounds(4, 163, 267, 100);
    getUI(ChildView.class).addChild(_sortPanel.getUI());
    enableEvents("ListKeyText", DragDrop);
}

/**
 * Reset UI panel from currently selected labels shape.
 */
public void resetUI()
{
    // Get currently selected labels shape (just return if null)
    RMLabels labels = (RMLabels)getSelectedShape(); if(labels==null) return;
    
    // Update ListKeyText, NumRowsText, NumColumnsText
    setViewValue("ListKeyText", labels.getDatasetKey());
    setViewValue("NumRowsText", labels.getNumberOfRows());
    setViewValue("NumColumnsText", labels.getNumberOfColumns());
    
    // Update LabelsWidthText, LabelsHeightText, SpacingWidthText, SpacingHeightText
    setViewValue("LabelWidthText", getUnitsFromPoints(labels.getLabelWidth()));
    setViewValue("LabelHeightText", getUnitsFromPoints(labels.getLabelHeight()));
    setViewValue("SpacingWidthText", getUnitsFromPoints(labels.getSpacingWidth()));
    setViewValue("SpacingHeightText", getUnitsFromPoints(labels.getSpacingHeight()));
    
    // Update LabelFormatsComboBox selection
    setViewSelIndex("LabelFormatsComboBox", Math.max(0, getLabelFormats().indexOf(_selectedLabelFormat)));

    // Update SortPanel
    _sortPanel.resetUI();
}

/**
 * Update currently selected labels shape from UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected labels shape (just return if null)
    RMLabels labels = (RMLabels)getSelectedShape(); if(labels==null) return;

    // Handle ListKeyText
    if(anEvent.equals("ListKeyText")) labels.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));

    // Handle NumRowsText, NumColumnsText
    if(anEvent.equals("NumRowsText")) {
        labels.setNumberOfRows(anEvent.getIntValue()); _selectedLabelFormat = null; labels.fixSize(); }
    if(anEvent.equals("NumColumnsText")) {
        labels.setNumberOfColumns(anEvent.getIntValue()); _selectedLabelFormat = null; labels.fixSize(); }
    
    // Handle LabelWidthText, LabelHeightText
    if(anEvent.equals("LabelWidthText")) {
        double f = getPointsFromUnits(anEvent.getFloatValue());
        labels.getLabel().setWidth(f); _selectedLabelFormat = null; labels.fixSize(); }
    if(anEvent.equals("LabelHeightText")) {
        double f = getPointsFromUnits(anEvent.getFloatValue());
        labels.getLabel().setHeight(f); _selectedLabelFormat = null; labels.fixSize(); }
    
    // Handle SpacingWidthText, SpacingHeightText
    if(anEvent.equals("SpacingWidthText")) {
        labels.setSpacingWidth(getPointsFromUnits(anEvent.getFloatValue()));
        _selectedLabelFormat = null; labels.fixSize(); }
    if(anEvent.equals("SpacingHeightText")) {
        labels.setSpacingHeight(getPointsFromUnits(anEvent.getFloatValue()));
        _selectedLabelFormat = null; labels.fixSize(); }
    
    // Handle LabelFormatsComboBox
    if(anEvent.equals("LabelFormatsComboBox")) {
        
        // If "Custom" is select, just return
        if(anEvent.getSelIndex()==0) { _selectedLabelFormat = null; return; }
        
        // Get selected label format and set number of rows/cols, width/height, spacing x/y
        _selectedLabelFormat = (LabelFormat)anEvent.getValue();
        labels.setNumberOfRows(_selectedLabelFormat.rowCount);
        labels.setNumberOfColumns(_selectedLabelFormat.colCount);
        labels.getLabel().setWidth(_selectedLabelFormat.labelWidth);
        labels.getLabel().setHeight(_selectedLabelFormat.labelHeight);
        labels.setSpacingWidth(_selectedLabelFormat.spacingX);
        labels.setSpacingHeight(_selectedLabelFormat.spacingY);
        
        // Fix size, repaint
        labels.fixSize();
        labels.getDocument().repaint();
        
        // Reset document margin
        labels.getDocument().setMargins(_selectedLabelFormat.leftMargin, _selectedLabelFormat.rightMargin,
                _selectedLabelFormat.topMargin, _selectedLabelFormat.bottomMargin);
        
        // Reset document size and label frame
        labels.getDocument().setSize(_selectedLabelFormat.pageWidth, _selectedLabelFormat.pageHeight);
        labels.setFrameXY(_selectedLabelFormat.leftMargin, _selectedLabelFormat.topMargin);
    }
}

/**
 * Returns the selected labels shape.
 */
public RMLabels getLabels()  { return (RMLabels)getSelectedShape(); }

/**
 * Returns the grouping for the selected labels shape.
 */
public RMGrouping getGrouping()  { RMLabels labels = getLabels(); return labels!=null? labels.getGrouping() : null; }

/**
 * Returns the shape class handled by this tool.
 */
public Class getShapeClass()  { return RMLabels.class; }

/**
 * Returns the window title for this tool.
 */
public String getWindowTitle()  { return "Labels Inspector"; }

/**
 * Overridden to make labels super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make labels not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Adds a new labels shape to editor.
 */
public static void addLabels(RMEditor anEditor, String aKeyPath)
{
    anEditor.undoerSetUndoTitle("Add Labels"); // Get new RMLabels with aKeyPath and move to center of page
    RMLabels labels = new RMLabels(); // Create new labels shape
    anEditor.getSelPage().addChild(labels); // Add to selected page (use editor to get undo)
    labels.setName(aKeyPath); // Set labels name
    labels.setDatasetKey(aKeyPath); // Set labels dataset key
    RMPage page = anEditor.getSelPage(); // Get selected page
    labels.setXY((page.getWidth() - labels.getWidth())/2, (page.getHeight() - labels.getHeight())/2); // Set location
    anEditor.setCurrentToolToSelectTool(); // Reset tool to select tool
    anEditor.setSelectedShape(labels); // Select labels
}

/**
 * Returns the list of standard Avery label formats.
 */
public List getLabelFormats()
{
    // If formats list has already been loaded, just return it
    if(_labelFormats!=null) return _labelFormats;
    
    // Create new list with dummy "custom" format first
    _labelFormats = new ArrayList();
    _labelFormats.add(new LabelFormat("Custom",0,0,0,0,0,0,0,0,0,0,0,0));
    
    // Load formats from avery-formats text file
    String text = SnapUtils.getText(getClass(), "RMLabelsToolAveryFormats.txt");
    if(text==null) { System.err.println("RMLabelsTool: Couldn't read Formats.txt"); return _labelFormats; }
    String lines[] = text.split("\n");
    
    // Iterate over lines
    for(String line : lines) {
        List lineParts = StringUtils.separate(line, ",");
        String name = (String)lineParts.get(0);
        float width = StringUtils.floatValue((String)lineParts.get(1));
        float height = StringUtils.floatValue((String)lineParts.get(2));
        float spaceWidth = StringUtils.floatValue((String)lineParts.get(3));
        float spaceHeight = StringUtils.floatValue((String)lineParts.get(4));
        int rows = Integer.parseInt((String)lineParts.get(5));
        int cols = Integer.parseInt((String)lineParts.get(6));
        float pageWidth = StringUtils.floatValue((String)lineParts.get(7));
        float pageHeight = StringUtils.floatValue((String)lineParts.get(8));
        float topMargin = StringUtils.floatValue((String)lineParts.get(9));
        float leftMargin = StringUtils.floatValue((String)lineParts.get(10));
        float bottomMargin = StringUtils.floatValue((String)lineParts.get(11));
        float rightMargin = StringUtils.floatValue((String)lineParts.get(12));
        LabelFormat newFormat = new LabelFormat(name, width, height, spaceWidth, spaceHeight, rows, cols, 
            pageWidth, pageHeight, topMargin, leftMargin, bottomMargin, rightMargin);
        _labelFormats.add(newFormat);
    }
    
    // Return label formats list
    return _labelFormats;
}

/**
 * An inner class to describe a label format.
 */
protected class LabelFormat {

    // Label width and label height
    float labelWidth, labelHeight;
    
    // Label spacing X and Y
    float spacingX, spacingY;
    
    // Page width and page height
    float pageWidth, pageHeight;
    
    // Label margins
    float topMargin, leftMargin, bottomMargin, rightMargin;
    
    // Number of rows and columns
    int	  rowCount, colCount;
    
    // Format name
    String formatName;
    
    /** Creates a new label format. */
    public LabelFormat(String name, float w, float h, float sx, float sy, int r, int c, float pw, float ph,
        float tm, float lm, float bm, float rm)
    {
        formatName = name; labelWidth = w*72; labelHeight = h*72; spacingX = sx*72; spacingY = sy*72;
        rowCount = r; colCount = c; pageWidth = pw*72; pageHeight = ph*72;
        topMargin = tm*72; leftMargin = lm*72; bottomMargin = bm*72; rightMargin = rm*72;
    }
    
    /** Returns string representation of label format (the format name). */
    public String toString()  { return formatName; }
}

}