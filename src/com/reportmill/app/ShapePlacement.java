/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import java.text.DecimalFormat;
import java.util.List;
import snap.gfx.*;
import snap.util.MathUtils;
import snap.view.*;

/**
 * This class provides UI editing for the currently selected shapes location and size.
 */
public class ShapePlacement extends RMEditorPane.SupportPane {
    
    // The Autosizing Panel
    AutosizingPanel     _autosizingPanel;

    // A formatter for bounds fields
    DecimalFormat       _fmt = new DecimalFormat("0.##");
    
/**
 * Creates a new ShapePlacement pane.
 */
public ShapePlacement(RMEditorPane anEP)  { super(anEP); }

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Get AutosizingPanel
    _autosizingPanel = getView("AutosizingPanel", AutosizingPanel.class);
}

/**
 * Updates UI controls from current selection.
 */
public void resetUI()
{
    // Get currently selected shape
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    
    // Update LockedCheckBox
    setViewValue("LockedCheckBox", shape.isLocked());
    
    // Update XThumb & XText
    setViewValue("XThumb", getUnitsFromPoints(shape.getFrameX()));
    setViewValue("XText", getUnitsFromPointsStr(shape.getFrameX()));
    
    // Update YThumb & YText
    setViewValue("YThumb", getUnitsFromPoints(shape.getFrameY()));
    setViewValue("YText", getUnitsFromPointsStr(shape.getFrameY()));
    
    // Update WThumb & WText
    setViewValue("WThumb", getUnitsFromPoints(shape.width()));
    setViewValue("WText", getUnitsFromPointsStr(shape.width()));
    
    // Update HThumb & HText
    setViewValue("HThumb", getUnitsFromPoints(shape.height()));
    setViewValue("HText", getUnitsFromPointsStr(shape.height()));
    
    // Update RotationThumb and RotationText
    setViewValue("RotationThumb", shape.getRoll());
    setViewValue("RotationText", shape.getRoll());
    
    // Update ScaleXThumb and ScaleXText
    setViewValue("ScaleXThumb", shape.getScaleX());
    setViewValue("ScaleXText", shape.getScaleX());
    
    // Update ScaleYThumb and ScaleYText
    setViewValue("ScaleYThumb", shape.getScaleY());
    setViewValue("ScaleYText", shape.getScaleY());
    
    // Update SkewXThumb and SkewXText
    setViewValue("SkewXThumb", shape.getSkewX());
    setViewValue("SkewXText", shape.getSkewX());
    
    // Update SkewYThumb and SkewYText
    setViewValue("SkewYThumb", shape.getSkewY());
    setViewValue("SkewYText", shape.getSkewY());
    
    // Update MinWText and MinHText
    setViewValue("MinWText", shape.isMinWidthSet()? shape.getMinWidth() : "-");
    setViewValue("MinHText", shape.isMinHeightSet()? shape.getMinHeight() : "-");
    
    // Update PrefWText and PrefHText
    setViewValue("PrefWText", shape.isPrefWidthSet()? shape.getPrefWidth() : "-");
    setViewValue("PrefHText", shape.isPrefHeightSet()? shape.getPrefHeight() : "-");
    
    // Disable if document or page
    getUI().setEnabled(!(shape instanceof RMDocument || shape instanceof RMPage));
    
    // Update AutosizingPanel
    _autosizingPanel.setAutosizing(shape.getAutosizing());
}

/**
 * Updates currently selected shape from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected editor, document and shapes
    RMEditor editor = getEditor();
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    List <? extends RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();

    // Handle LockedCheckBox
    if(anEvent.equals("LockedCheckBox")) { boolean value = anEvent.getBoolValue();
        for(RMShape shp : shapes) shp.setLocked(value); }

    // Handle X ThumbWheel and Text
    if(anEvent.equals("XThumb") || anEvent.equals("XText")) {
        editor.undoerSetUndoTitle("Location Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        for(RMShape shp : shapes) shp.setFrameX(value);
    }
    
    // Handle Y ThumbWheel and Text
    if(anEvent.equals("YThumb") || anEvent.equals("YText")) {
        editor.undoerSetUndoTitle("Location Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        for(RMShape shp : shapes) shp.setFrameY(value);
    }
    
    // Handle Width ThumbWheel and Text
    if(anEvent.equals("WThumb") || anEvent.equals("WText")) {
        editor.undoerSetUndoTitle("Size Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        if(Math.abs(value)<.1) value = MathUtils.sign(value)*.1f;
        for(RMShape shp : shapes) shp.setWidth(value);
    }
    
    // Handle Height ThumbWheel and Text
    if(anEvent.equals("HThumb") || anEvent.equals("HText")) {
        editor.undoerSetUndoTitle("Size Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        if(Math.abs(value)<.1) value = MathUtils.sign(value)*.1f;
        for(RMShape shp : shapes) shp.setHeight(value);
    }
    
    // Handle ScaleX/ScaleY Thumb & Text
    if(anEvent.equals("ScaleXThumb") || anEvent.equals("ScaleXText") ||
        anEvent.equals("ScaleYThumb") || anEvent.equals("ScaleYText")) {
        shape.undoerSetUndoTitle("Scale Change");
        float value = anEvent.getFloatValue();
        boolean symmetrical = getViewBoolValue("ScaleSymetricCheckBox");
        
        // Handle ScaleX (and symmetrical)
        if(anEvent.equals("ScaleXThumb") || anEvent.equals("ScaleXText") || symmetrical)
            for(RMShape s : shapes)
                s.setScaleX(value);

        // Handle ScaleY (and symmetrical)
        if(anEvent.equals("ScaleYThumb") || anEvent.equals("ScaleYText") || symmetrical)
            for(RMShape s : shapes)
                s.setScaleY(value);
    }

    // Handle SkewX Thumb & Text
    if(anEvent.equals("SkewXThumb") || anEvent.equals("SkewXText")) {
        shape.undoerSetUndoTitle("Skew Change");
        float value = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setSkewX(value);
    }

    // Handle SkewY Thumb & Text
    if(anEvent.equals("SkewYThumb") || anEvent.equals("SkewYText")) {
        shape.undoerSetUndoTitle("Skew Change");
        float value = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setSkewY(value);
    }
    
    // Handle MinWText & MinHText
    if(anEvent.equals("MinWText"))
        for(RMShape shp : shapes) shp.setMinWidth(anEvent.getFloatValue());
    if(anEvent.equals("MinHText"))
        for(RMShape shp : shapes) shp.setMinHeight(anEvent.getFloatValue());
    
    // Handle MinWSyncButton & MinHSyncButton
    if(anEvent.equals("MinWSyncButton"))
        for(RMShape shp : shapes) shp.setMinWidth(shp.getWidth());
    if(anEvent.equals("MinHSyncButton"))
        for(RMShape shp : shapes) shp.setMinHeight(shp.getHeight());

    // Handle PrefWText & PrefHText
    if(anEvent.equals("PrefWText"))
        for(RMShape shp : shapes) shp.setPrefWidth(anEvent.getFloatValue());
    if(anEvent.equals("PrefHText"))
        for(RMShape shp : shapes) shp.setPrefHeight(anEvent.getFloatValue());
    
    // Handle PrefWSyncButton & PrefHSyncButton
    if(anEvent.equals("PrefWSyncButton"))
        for(RMShape shp : shapes) shp.setPrefWidth(shp.getWidth());
    if(anEvent.equals("PrefHSyncButton"))
        for(RMShape shp : shapes) shp.setPrefHeight(shp.getHeight());
        
    // Handle Rotation Thumb & Text
    if(anEvent.equals("RotationThumb") || anEvent.equals("RotationText")) {
        shape.undoerSetUndoTitle("Rotation Change");
        float value = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setRoll(value);
    }

    // Handle AutosizingPanel
    if(anEvent.equals("AutosizingPanel"))
        for(RMShape shp : shapes)
            shp.setAutosizing(_autosizingPanel.getAutosizing());
            
    // Handle ResetAutosizingButton
    if(anEvent.equals("ResetAutosizingButton"))
        for(RMShape shp : shapes)
            shp.setAutosizing("--~,--~");
}

/**
 * Converts from shape units to tool units.
 */
public double getUnitsFromPoints(double aValue)
{
    RMEditor editor = getEditor(); RMDocument doc = editor.getDoc();
    return doc!=null? doc.getUnitsFromPoints(aValue) : aValue;
}

/**
 * Converts from shape units to tool units.
 */
public String getUnitsFromPointsStr(double aValue)  { return _fmt.format(getUnitsFromPoints(aValue)); }

/**
 * Converts from tool units to shape units.
 */
public double getPointsFromUnits(double aValue)
{
    RMEditor editor = getEditor(); RMDocument doc = editor.getDoc();
    return doc!=null? doc.getPointsFromUnits(aValue) : aValue;
}

/**
 * Returns the name to be used in the inspector's window title.
 */
public String getWindowTitle()  { return "Placement Inspector"; }

/**
 * An inner class to provide a simple springs and struts control.
 */
public static class AutosizingPanel extends View {
    
    // Autosizing string
    String    _autosizing = "-~~,-~~";
    
    // Autosizing spring/strut images
    Image      _images[];
    
    // Constants for images
    public static final int BACKGROUND = 0;
    public static final int OUTER_HORIZONTAL_SPRING = 1;
    public static final int OUTER_VERTICAL_SPRING = 2;
    public static final int OUTER_HORIZONTAL_STRUT = 3;
    public static final int OUTER_VERTICAL_STRUT = 4;
    public static final int INNER_HORIZONTAL_SPRING = 5;
    public static final int INNER_VERTICAL_SPRING = 6;
    public static final int INNER_HORIZONTAL_STRUT = 7;
    public static final int INNER_VERTICAL_STRUT = 8;

    /** Creates a new autosizing panel. */
    public AutosizingPanel()
    {
        // Get image names
        String imageNames[] = { "SpringsBack.png", "SpringOuterX.png", "SpringOuterY.png", "StrutOuterX.png",
            "StrutOuterY.png", "SpringX.png", "SpringY.png", "StrutX.png", "StrutY.png" };

        // Create images array and load images
        _images = new Image[imageNames.length];
        for(int i=0; i<imageNames.length; ++i)
            _images[i] = Image.get(getClass(), imageNames[i]);
        
        // Add mouse listener to send action
        enableEvents(MouseRelease, Action);
    }
    
    /** ProcessEvent. */
    protected void processEvent(ViewEvent anEvent)
    {
        if(!isEnabled() || !anEvent.isMouseEvent()) return;
        StringBuffer sb = new StringBuffer(_autosizing);
        Point p = new Point(anEvent.getX(), anEvent.getY());
        double w = getWidth(), h = getHeight();
        
        if(p.getDistance(w/8,h/2)<w/8) sb.setCharAt(0, sb.charAt(0)=='-'? '~' : '-');
        else if(p.getDistance(w*3/8,h/2)<w/8) sb.setCharAt(1, sb.charAt(1)=='-'? '~' : '-');
        else if(p.getDistance(w*5/8,h/2)<w/8) sb.setCharAt(1, sb.charAt(1)=='-'? '~' : '-');
        else if(p.getDistance(w*7/8,h/2)<w/8) sb.setCharAt(2, sb.charAt(2)=='-'? '~' : '-');
        else if(p.getDistance(w/2,h/8)<w/8) sb.setCharAt(4, sb.charAt(4)=='-'? '~' : '-');
        else if(p.getDistance(w/2,h*3/8)<w/8) sb.setCharAt(5, sb.charAt(5)=='-'? '~' : '-');
        else if(p.getDistance(w/2,h*5/8)<w/8) sb.setCharAt(5, sb.charAt(5)=='-'? '~' : '-');
        else if(p.getDistance(w/2,h*7/8)<w/8) sb.setCharAt(6, sb.charAt(6)=='-'? '~' : '-');
        
        // Set new autosizing string, send node event and repaint
        _autosizing = sb.toString();
        fireActionEvent(anEvent);
        repaint();
    }
    
    /** Returns autosizing string. */
    public String getAutosizing() { return _autosizing; }
    
    /** Sets autosizing string. */
    public void setAutosizing(String aString) { _autosizing = aString; repaint(); }
    
    /** Paints the component. */
    public void paintFront(Painter aPntr)
    {
        double w = getWidth(), h = getHeight();
        aPntr.setColor(Color.WHITE); aPntr.fillRect(0, 0, w, h);
        aPntr.setColor(Color.BLACK); aPntr.drawRect(0, 0, w, h);
        aPntr.drawImage(_images[BACKGROUND], 24, 24);
        
        // Draw horizontal springs (left, middle, right)
        Image i1 = _images[_autosizing.charAt(0)=='-'? OUTER_HORIZONTAL_STRUT : OUTER_HORIZONTAL_SPRING];
        Image i2 = _images[_autosizing.charAt(1)=='-'? INNER_HORIZONTAL_STRUT : INNER_HORIZONTAL_SPRING];
        Image i3 = _images[_autosizing.charAt(2)=='-'? OUTER_HORIZONTAL_STRUT : OUTER_HORIZONTAL_SPRING];
        aPntr.drawImage(i1, 0, 41); aPntr.drawImage(i2, 25, 41); aPntr.drawImage(i3, 73, 41);
        
        // Draw vertical springs (top, middle, bottom)
        Image i4 = _images[_autosizing.charAt(4)=='-'? OUTER_VERTICAL_STRUT : OUTER_VERTICAL_SPRING];
        Image i5 = _images[_autosizing.charAt(5)=='-'? INNER_VERTICAL_STRUT : INNER_VERTICAL_SPRING];
        Image i6 = _images[_autosizing.charAt(6)=='-'? OUTER_VERTICAL_STRUT : OUTER_VERTICAL_SPRING];
        aPntr.drawImage(i4, 41, 0); aPntr.drawImage(i5, 41, 25); aPntr.drawImage(i6, 41, 73);
        
        // If disabled then dim everything out
        if(!isEnabled()) { aPntr.setColor(new Color(1d,.5)); aPntr.fillRect(0, 0, w, h); }
    }
}

}