/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.MathUtils;
import snap.view.View;

/**
 * Handles editor methods specific to rulers.
 */
public class RMEditorRuler extends View {

    // The ruler editor
    RMEditor                  _editor;

    // The ruler orientation
    byte                      _orientation;
    
    // Units of measure
    RMDocument.Unit           _unitOfMeasure;
    
    // Zoom Factor
    double                    _zoomFactor;
    
    // The mouse point
    Point                     _mouse = new Point();
    
    // Ruler constants
    static final int          _rulerWidth = 20;
    static final Font     _font = Font.Arial10.deriveFont(8);
    static final Stroke       _mouseStroke = Stroke.Stroke1.copyForDashes(2, 2);
    
    // Constants for ruler orientation
    public static final byte HORIZONTAL = 0;
    public static final byte VERTICAL = 1;
    
    // Tab Stop Icons
    //static Icon _tabLeft, _tabRight, _tabCenter _tabDecimal;
    
/**
 * Creates a new editor ruler.
 */
public RMEditorRuler(RMEditor owner, byte orientation)
{
    _editor = owner;
    _orientation = orientation;
    _unitOfMeasure = _editor.getDocument().getUnit();
    _zoomFactor = _editor.getZoomFactor();
    setFill(Color.LIGHTGRAY); setBorder(Border.createLineBorder(Color.BLACK,1));
}

/**
 * Returns the current document.
 */
public RMDocument getDoc()  { return _editor.getDocument(); }

/**
 * Returns whether horizontal.
 */
public boolean isHorizontal()  { return _orientation==HORIZONTAL; }
    
public int getUnitWidth()
{
    double unitWidth = _unitOfMeasure==RMDocument.Unit.CM || _unitOfMeasure==RMDocument.Unit.MM? 57 : 72;
    return (int)Math.round(unitWidth*_zoomFactor);
}
        
/** Override to control size. */
protected double getPrefWidthImpl(double aH)  { return isHorizontal()? getDoc().getWidth() : _rulerWidth; }

/** Override to control size. */
protected double getPrefHeightImpl(double aW)  { return isHorizontal()? _rulerWidth : getDoc().getHeight(); }

/**
 * Sets the mouse point.
 */
public void setMousePoint(Point aPoint)  { _mouse = aPoint; repaint(); }

/**
 * Paint.
 */
protected void paintFront(Painter aPntr)
{
    aPntr.setFont(_font);
    if(isHorizontal()) paintHor(aPntr);
    else paintVer(aPntr);
}

/**
 * Paint Horizontal.
 */
protected void paintHor(Painter aPntr)
{
    // Get ruler bounds in doc coords
    Rect vrect = _editor.getVisRect();
    Rect bnds = new Rect(vrect.getX()-_rulerWidth, 0, getWidth(), _rulerWidth);
    bnds = _editor.convertToShape(bnds, getDoc()).getBounds();
    
    // Scale and translate ruler to doc coords
    aPntr.save();
    double scale = getWidth()/bnds.getWidth(); aPntr.scale(scale,1);
    aPntr.translate(-bnds.getX(), 0);
    
    // Get basic coords
    double usize = getUnitWidth();
    double x = MathUtils.floor(bnds.getX(),usize), maxx = bnds.getMaxX(), h = getHeight(), hh = h/2;
    double dx = usize, dxm = 9;
    
    // Paint ticks/labels
    while(x<maxx) {
        String str = String.valueOf((int)Math.round(x)); aPntr.drawString(str,x+2,_font.getAscent()+2);
        aPntr.drawLine(x,0,x,h);
        for(double x2=x+dxm, mx2=x+dx/2-1; x2<mx2; x2+=dxm) aPntr.drawLine(x2,h,x2,h-5);
        double midx = x+dx/2; aPntr.drawLine(midx,h,midx,h-hh);
        for(double x2=midx+dxm, mx2=x+dx-1; x2<mx2; x2+=dxm) aPntr.drawLine(x2,h,x2,h-5);
        x += dx;
    }
    
    // Paint shape position
    Rect bounds = getShapeBounds();
    if(bounds!=null) {
        aPntr.setColor(new Color(1,.5)); aPntr.fillRect(bounds.getX(), 0, bounds.getWidth(), getHeight()); }
        
    // Paint mouse position
    Point mp = _editor.convertToShape(_mouse.x, _mouse.y, getDoc());
    aPntr.setColor(Color.BLACK); aPntr.setStroke(_mouseStroke);
    aPntr.drawLine(mp.getX(), 0, mp.getX(), getHeight());
    aPntr.restore();
}

/**
 * Paint Vertical.
 */
protected void paintVer(Painter aPntr)
{
    // Get ruler bounds in doc coords
    Rect vrect = _editor.getVisRect();
    Rect bnds = new Rect(0, vrect.getY(), _rulerWidth, getHeight());
    bnds = _editor.convertToShape(bnds, getDoc()).getBounds();
    
    // Scale ruler to doc coords
    aPntr.save();
    double scale = getHeight()/bnds.getHeight(); aPntr.scale(1,scale);
    aPntr.translate(0, -bnds.getY());
    
    // Get basic coords
    double usize = getUnitWidth();
    double y = MathUtils.floor(bnds.getY(),usize), maxy = bnds.getMaxY(), w = getWidth(), hw = w/2;
    double dy = usize, dym = 9;
    
    // Paint ticks/labels
    while(y<maxy) {
        String str = String.valueOf((int)Math.round(y)); aPntr.drawString(str,2, y+_font.getAscent()+2);
        aPntr.drawLine(0,y,w,y);
        for(double y2=y+dym, my2=y+dy/2-1; y2<my2; y2+=dym) aPntr.drawLine(w-5,y2,w,y2);
        double my = y+dy/2; aPntr.drawLine(w,my,w-hw,my);
        for(double y2=my+dym, my2=y+dy-1; y2<my2; y2+=dym) aPntr.drawLine(w,y2,w-5,y2);
        y += dy;
    }
    
    // Paint shape position
    Rect bounds = getShapeBounds();
    if(bounds!=null) {
        aPntr.setColor(new Color(1,.5)); aPntr.fillRect(0, bounds.getY(), getWidth(), bounds.getHeight()); }
        
    // Paint mouse position
    Point mp = _editor.convertToShape(_mouse.x, _mouse.y, getDoc());
    aPntr.setColor(Color.BLACK); aPntr.setStroke(_mouseStroke);
    aPntr.drawLine(0, mp.getY(), getWidth(), mp.getY());
    aPntr.restore();
}

/** Returns the current shape bounds. */
private Rect getShapeBounds()
{
    RMShape shape = _editor.getSelectedOrSuperSelectedShape();
    if(shape==null || shape instanceof RMDocument || shape instanceof RMPage) return null;
    Rect sbnds = shape.getBoundsInside(); shape.convertRectToShape(sbnds, null); return sbnds;
}

}