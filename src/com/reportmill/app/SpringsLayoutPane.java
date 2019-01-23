/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.RMShape;
import java.util.List;
import snap.gfx.*;
import snap.view.*;

/**
 * Provides UI editing for shapes with SpringsLayout.
 */
public class SpringsLayoutPane extends RMEditorPane.SupportPane {

    // The Autosizing Panel
    AutosizingPanel    _autosizingPanel;

/**
 * Creates a new SpringsLayoutPane.
 */
public SpringsLayoutPane(RMEditorPane anEP)  { super(anEP); }

/**
 * Override to get AutosizingPanel. 
 */
protected void initUI()  { _autosizingPanel = getView("AutosizingPanel", AutosizingPanel.class); }

/**
 * ResetUI.
 */
public void resetUI()
{
    RMEditor editor = getEditor();
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    _autosizingPanel.setAutosizing(shape.getAutosizing());
}

/**
 * Responds to UI control changes.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected editor and selected shapes
    RMEditor editor = getEditor();
    List <? extends RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();
    
    // Handle AutosizingPanel
    if(anEvent.equals("AutosizingPanel"))
        for(RMShape shape : shapes)
            shape.setAutosizing(_autosizingPanel.getAutosizing());
            
    // Handle ResetAutosizingButton
    if(anEvent.equals("ResetAutosizingButton"))
        for(RMShape shape : shapes)
            shape.setAutosizing("--~,--~");
}

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
            _images[i] = Image.get(SpringsLayoutPane.class, imageNames[i]);
        
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