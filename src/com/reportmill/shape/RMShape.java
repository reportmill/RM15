/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.*;
import snap.util.*;
import snap.util.XMLArchiver.*;
import snap.view.*;

/**
 * This class is the basis for all graphic elements in a ReportMill document. You'll rarely use this class directly,
 * however, it encapsulates all the basic shape attributes and the most common methods used in template manipulation,
 * like setX(), setY(), setWidth(), setColor(), etc.
 *
 * Here's an example of programatically adding a watermark to a document:
 * <p><blockquote><pre>
 *   RMFont font = RMFont.getFont("Arial Bold", 72);
 *   RMColor color = new RMColor(.9f, .9f, .9f);
 *   RMXString string = new RMXString("REPORTMILL", font, color);
 *   RMText shape = new RMText(string);
 *   myDocument.getPage(0).addChild(shape);
 *   shape.setBounds(36, 320, 540, 140);
 *   shape.setRoll(45);
 *   shape.setOpacity(.667f);
 * </pre></blockquote>
 */
public class RMShape implements Cloneable, RMTypes, Archivable, Key.GetSet {

    // X location of shape
    double         _x = 0;
    
    // Y location of shape
    double         _y = 0;
    
    // Width of shape
    double         _width = 0;
    
    // Height of shape
    double         _height = 0;
    
    // An array to hold optional roll/scale/skew values
    double         _rss[];
    
    // The stroke for this shape
    RMStroke       _stroke = null;
    
    // The fill for this shape
    RMFill         _fill = null;
    
    // The effect for this shape
    Effect         _effect = null;
    
    // The opacity of shape
    double         _opacity = 1;
    
    // Whether this shape is visible
    boolean        _visible = true;
    
    // The parent of this shape
    RMParentShape  _parent = null;
    
    // A string describing how this shape should autosize in an RMSpringShape
    String         _asize; Object _springInfo;
    
    // Map to hold less used attributes (name, url, etc.)
    RMSharedMap    _attrMap = SHARED_MAP;
    
    // The PropChangeSupport
    PropChangeSupport  _pcs = PropChangeSupport.EMPTY;

    // A shared/root RMSharedMap (cloned to turn on shared flag)
    static final RMSharedMap SHARED_MAP = new RMSharedMap().clone();
    
    // Constants for properties
    public static final String X_Prop = "X";
    public static final String Y_Prop = "Y";
    public static final String Width_Prop = "Width";
    public static final String Height_Prop = "Height";
    public static final String Roll_Prop = "Roll";
    public static final String ScaleX_Prop = "ScaleX";
    public static final String ScaleY_Prop = "ScaleY";
    public static final String SkewX_Prop = "SkewX";
    public static final String SkewY_Prop = "SkewY";
    public static final String Stroke_Prop = "Stroke";
    public static final String Fill_Prop = "Fill";
    public static final String Effect_Prop = "Effect";
    public static final String Opacity_Prop = "Opacity";
    public static final String Name_Prop = "Name";
    public static final String Visible_Prop = "Visible";
    public static final String Locked_Prop = "Locked";
    public static final String MinWidth_Prop = "MinWidth";
    public static final String MinHeight_Prop = "MinHeight";
    public static final String PrefWidth_Prop = "PrefWidth";
    public static final String PrefHeight_Prop = "PrefHeight";
    
/**
 * Returns raw x location of shape. Developers should use the more common getX, which presents positive x.
 */
public double x()  { return _x; }

/**
 * Returns raw y location of shape. Developers should use the more common getY, which presents positive y.
 */
public double y()  { return _y; }

/**
 * Returns raw width of shape. Developers should use the more common getWidth, which presents positive width.
 */
public double width()  { return _width; }

/**
 * Returns raw height of shape. Developers should use the more common getHeight, which presents positive height.
 */
public double height()  { return _height; }

/**
 * Returns raw x, y, width and height of shape as rect (preserves possible negative sizes).
 */
public Rect bounds()  { return new Rect(x(), y(), width(), height()); }

/**
 * Returns the X location of the shape.
 */
public double getX()  { return _width<0? _x + _width : _x; }

/**
 * Sets the X location of the shape.
 */
public void setX(double aValue)
{
    if(_x==aValue) return;
    repaint();
    firePropChange(X_Prop, _x, _x = aValue);
    repaint();
}

/**
 * Returns the Y location of the shape.
 */
public double getY()  { return _height<0? _y + _height : _y; }

/**
 * Sets the Y location of the shape.
 */
public void setY(double aValue)
{
    if(_y==aValue) return;
    repaint();
    firePropChange(Y_Prop, _y, _y = aValue);
    repaint();
}

/**
 * Returns the width of the shape.
 */
public double getWidth()  { return _width<0? -_width : _width; }

/**
 * Sets the width of the shape.
 */
public void setWidth(double aValue)
{
    double old = getWidth(); if(aValue==old) return;
    if(old>aValue) repaint();
    firePropChange(Width_Prop, _width, _width = aValue);
    if(old<aValue) repaint();
}

/**
 * Returns the height of the shape.
 */
public double getHeight()  { return _height<0? -_height : _height; }

/**
 * Sets the height of the shape.
 */
public void setHeight(double aValue)
{
    double old = getHeight(); if(aValue==old) return;
    if(old>aValue) repaint();
    firePropChange(Height_Prop, _height, _height = aValue);
    if(old<aValue) repaint();
}

/**
 * Returns the max X of the shape (assumes not rotated, scaled or skewed).
 */
public double getMaxX()  { return getX() + getWidth(); }

/**
 * Returns the max Y of the shape (assumes not rotated, scaled or skewed).
 */
public double getMaxY()  { return getY() + getHeight(); }

/**
 * Returns the XY location of the shape as a point.
 */
public Point getXY()  { return new Point(getX(), getY()); }

/**
 * Sets the X and Y location of the shape to the given point (convenience).
 */
public void setXY(Point aPoint)  { setXY(aPoint.getX(), aPoint.getY()); }

/**
 * Sets the X and Y location of the shape to the given point (convenience).
 */
public void setXY(double anX, double aY)  { setX(anX); setY(aY); }

/**
 * Returns the size of the shape.
 */
public Size getSize()  { return new Size(getWidth(), getHeight()); }

/**
 * Sets the size of the shape.
 */
public void setSize(Size aSize)  { setSize(aSize.getWidth(), aSize.getHeight()); }

/**
 * Sets the size of the shape.
 */
public void setSize(double aWidth, double aHeight)  { setWidth(aWidth); setHeight(aHeight); }

/**
 * Returns the X, Y, width and height of the shape as a rect (use getFrame if shape has roll/scale/skew).
 */
public Rect getBounds()  { return new Rect(getX(), getY(), getWidth(), getHeight()); }

/**
 * Sets X, Y, width and height of shape to dimensions in given rect.
 */
public void setBounds(Rect aRect) { setBounds(aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight()); }

/**
 * Sets X, Y, width and height of shape to given dimensions.
 */
public void setBounds(double anX, double aY, double aW, double aH) { setX(anX); setY(aY); setWidth(aW); setHeight(aH); }

/**
 * Returns the rect in parent coords that fully encloses the shape.
 */
public Rect getFrame()
{
    if(isRSS()) return localToParent(getBoundsInside()).getBounds();
    return getBounds();
}

/**
 * Sets the bounds of the shape such that it exactly fits in the given parent coord rect.
 */
public void setFrame(Rect aRect)  { setFrame(aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight()); }

/**
 * Sets the bounds of the shape such that it exactly fits in the given parent coord rect.
 */
public void setFrame(double anX, double aY, double aWidth, double aHeight)
{
    setFrameXY(anX, aY);
    setFrameSize(aWidth, aHeight);
}

/**
 * Returns the X of the rect that fully encloses the shape in parent coords.
 */
public double getFrameX()  { return isRSS()? getFrameXY().x : getX(); }

/**
 * Sets a shape's X such that its bounds rect (in parent coords) has origin at the given X.
 */
public void setFrameX(double anX)  { double x = _x + anX - getFrameX(); setX(x); }

/**
 * Returns the Y of the rect that fully encloses the shape in parent coords.
 */
public double getFrameY()  { return isRSS()? getFrameXY().y : getY(); }

/**
 * Sets a shape's Y such that its bounds rect (in parent coords) has origin at the given Y.
 */
public void setFrameY(double aY)  { double y = _y + aY - getFrameY(); setY(y); }

/**
 * Returns the width of the rect that fully encloses the shape in parent coords.
 */
public double getFrameWidth()  { return isRSS()? getFrame().width : getWidth(); }

/**
 * Returns the height of the rect that fully encloses the shape in parent coords.
 */
public double getFrameHeight()  { return isRSS()? getFrame().height : getHeight(); }

/**
 * Returns the origin of the shape's bounds rect in parent coords.
 */ 
public Point getFrameXY()  { return isRSS()? new Point(getFrame().getXY()) : getXY(); }

/**
 * Sets a shape's origin such that its bounds rect (in parent coords) has origin at the given point.
 */
public void setFrameXY(Point aPoint)  { setFrameXY(aPoint.getX(), aPoint.getY()); }

/**
 * Sets a shape's origin such that its frame (enclosing rect in parent coords) will have the given X and Y.
 */
public void setFrameXY(double anX, double aY)  { setFrameX(anX); setFrameY(aY); }

/**
 * Sets the height of the rect that fully encloses the shape in parent coords.
 */
public void setFrameSize(double aWidth, double aHeight)
{
    // If shape not rotated, scaled or skewed, just set and return
    if(!isRSS()) {
        if(_width<0) { setX(_x + (aWidth+_width)); aWidth = -aWidth; }
        if(_height<0) { setY(_y + (aHeight+_height)); aHeight = -aHeight; }
        setSize(aWidth, aHeight); return;
    }
    
    // Convert X & Y axis to parent coords
    Transform toParent = getLocalToParent();
    Size x_axis = new Size(_width, 0); toParent.transformVector(x_axis);
    Size y_axis = new Size(0, _height); toParent.transformVector(y_axis);

    // Scale widths of X & Y axes in parent coords by ratio of NewWidth/OldWidth
    double sizeByRatio1 = Math.abs(aWidth)/(Math.abs(x_axis.width) + Math.abs(y_axis.width));
    x_axis.width *= sizeByRatio1; y_axis.width *= sizeByRatio1;
    
    // Scale heights of X & Y axes in parent coords by ratio of NewHeight/OldHeight
    double sizeByRatio2 = Math.abs(aHeight)/(Math.abs(x_axis.height) + Math.abs(y_axis.height));
    x_axis.height *= sizeByRatio2; y_axis.height *= sizeByRatio2;

    // Cache current bounds origin (this shouldn't change)
    Point origin = getFrameXY();
    
    // Reset current Skew and convert X & Y axis from parent coords
    setSkewXY(0, 0);
    Transform fromParent = getParentToLocal();
    fromParent.transformVector(x_axis);
    fromParent.transformVector(y_axis);

    // Set the size to compensate for the skew
    setSize(x_axis.width, y_axis.height);

    // Calculate new skew angles (or roll, if width or height is zero)
    if(width()==0)
        setRoll(getRoll() - Math.toDegrees(Math.atan(y_axis.width/y_axis.height)));
    else if(height()==0)
        setRoll(getRoll() - Math.toDegrees(Math.atan(x_axis.height/x_axis.width)));
    else {
        setSkewX(Math.toDegrees(Math.atan(x_axis.height/x_axis.width)));
        setSkewY(Math.toDegrees(Math.atan(y_axis.width/y_axis.height)));
    }

    // Reset original bounds origin (it may have been effected by skew changes)
    setFrameXY(origin);
}

/**
 * Returns the max X of the shape's frame.
 */
public double getFrameMaxX()  { return isRSS()? getFrame().getMaxX() : getMaxX(); }

/**
 * Returns the max Y of the shape's frame.
 */
public double getFrameMaxY()  { return isRSS()? getFrame().getMaxY() : getMaxY(); }

/**
 * Returns the origin point of the shape in parent's coords.
 */
public Point getXYP()  { return localToParent(0,0); }

/**
 * Sets the origin point of the shape to the given X and Y in parent's coords.
 */
public void setXYP(double anX, double aY)
{
    // If rotated-scaled-skewd, get XY in parent coords and set XY as an offset from parent
    if(isRSS()) {
        Point p = getXYP();
        setXY(_x + anX - p.getX(), _y + aY - p.getY());
    }

    // If not rotated-scaled-skewed, just set x/y (adjusted if width/height are negative)
    else setXY(_width<0? anX-_width : anX, _height<0? aY-_height : aY);    
}

/**
 * Offsets the X and Y location of the shape by the given dx & dy amount (convenience).
 */
public void offsetXY(double dx, double dy)  { setXY(_x + dx, _y + dy); }

/**
 * Returns the bounds of the shape in the shape's own coords.
 */
public Rect getBoundsInside()  { return new Rect(0, 0, getWidth(), getHeight()); }

/**
 * Returns the bounds of the shape in the shape's own coords.
 */
public Rect getBoundsLocal()  { return new Rect(0, 0, getWidth(), getHeight()); }

/**
 * Returns the bounds of the path associated with this shape in local coords, adjusted to account for stroke width.
 */
public Rect getBoundsStroked()
{
    Rect bnds = getBoundsInside();
    RMStroke stroke = getStroke(); if(stroke==null) return bnds;
    bnds.inset(-stroke.getWidth()/2); return bnds;
}

/**
 * Returns the marked bounds of this shape and it's children.
 */
public Rect getBoundsStrokedDeep()
{
    // Get normal marked bounds and union with children BoundsStrokedDeep (converted to this shape coords)
    Rect bounds = getBoundsStroked();
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i); if(!child.isVisible()) continue;
        Rect cbounds = child.getBoundsStrokedDeep();
        cbounds = child.localToParent(cbounds).getBounds();
        bounds.unionEvenIfEmpty(cbounds); }
    return bounds;
}

/**
 * Returns the bounds of the path associated with this shape in local coords, adjusted to account for stroke width.
 */
public Rect getBoundsMarked()
{
    Rect bounds = getBoundsStroked();
    if(getEffect()!=null) bounds = getEffect().getBounds(bounds);
    return bounds;
}

/**
 * Returns the marked bounds of this shape and it's children.
 */
public Rect getBoundsMarkedDeep()
{
    // Get normal marked bounds and union with children BoundsMarkedDeep (converted to this shape coords)
    Rect bounds = getBoundsMarked();
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i); if(!child.isVisible()) continue;
        Rect cbounds = child.getBoundsMarkedDeep();
        cbounds = child.localToParent(cbounds).getBounds();
        bounds.unionEvenIfEmpty(cbounds); }
    return bounds;
}

/**
 * Returns the roll of the shape.
 */
public double getRoll()  { return _rss==null? 0 : _rss[0]; }

/**
 * Sets the roll of the shape.
 */
public void setRoll(double aValue)
{
    aValue = Math.round(aValue*100)/100d; if(aValue==getRoll()) return;
    repaint();
    firePropChange(Roll_Prop, getRSS()[0], _rss[0] = aValue);
    repaint();
}

/**
 * Returns the scale of the X axis of the shape.
 */
public double getScaleX()  { return _rss==null? 1 : _rss[1]; }

/**
 * Sets the scale of the X axis of the shape.
 */
public void setScaleX(double aValue)
{
    double old = getScaleX(); aValue = Math.round(aValue*100)/100d; if(aValue==old) return;
    if(old>aValue) repaint();
    firePropChange(ScaleX_Prop, old, getRSS()[1] = aValue);
    if(old<aValue) repaint();
}

/**
 * Returns the scale of the Y axis of the shape.
 */
public double getScaleY()  { return _rss==null? 1 : _rss[2]; }

/**
 * Sets the scale of the Y axis of the shape.
 */
public void setScaleY(double aValue)
{
    double old = getScaleY(); aValue = Math.round(aValue*100)/100d; if(aValue==old) return;
    if(old>aValue) repaint();
    firePropChange(ScaleY_Prop, old, getRSS()[2] = aValue);
    if(old<aValue) repaint();
}

/**
 * Sets the scale of the X and Y axis.
 */
public void setScaleXY(double sx, double sy)  { setScaleX(sx); setScaleY(sy); }

/**
 * Returns the skew of the X axis of the shape.
 */
public double getSkewX()  { return _rss==null? 0 : _rss[3]; }

/**
 * Sets the skew of the X axis of the shape.
 */
public void setSkewX(double aValue)
{
    aValue = Math.round(aValue*100)/100d; if(aValue==getSkewX()) return;
    repaint();
    firePropChange(SkewX_Prop, getRSS()[3], _rss[3] = aValue);
    repaint();
}

/**
 * Returns the skew of the Y axis of the shape.
 */
public double getSkewY()  { return _rss==null? 0 : _rss[4]; }

/**
 * Sets the skew of the Y axis of the shape.
 */
public void setSkewY(double aValue)
{
    aValue = Math.round(aValue*100)/100d; if(aValue==getSkewY()) return;
    repaint();
    firePropChange(SkewY_Prop, getRSS()[4], _rss[4] = aValue);
    repaint();
}

/**
 * Sets the skew of the X and Y axis.
 */
public void setSkewXY(double skx, double sky)  { setSkewX(skx); setSkewY(sky); }

/**
 * Returns whether the shape has been rotated, scaled or skewed (for efficiency).
 */
public boolean isRSS()  { return _rss!=null; }

/**
 * Returns the roll scale skew array: [ Roll, ScaleX, ScaleY, SkewX, SkewY ].
 */
protected double[] getRSS()  { return _rss!=null? _rss : (_rss=new double[] { 0, 1, 1, 0, 0 }); }

/**
 * Returns the stroke for this shape.
 */
public RMStroke getStroke()  { return _stroke; }

/**
 * Sets the stroke for this shape, with an option to turn on drawsStroke.
 */
public void setStroke(RMStroke aStroke)
{
    if(SnapUtils.equals(getStroke(), aStroke)) return;
    repaint();
    firePropChange(Stroke_Prop, _stroke, _stroke = aStroke);
}

/**
 * Sets the stroke for this shape, with an option to turn on drawsStroke.
 */
public void setStroke(Color aColor, double aWidth)  { setStroke(new RMStroke(aColor, aWidth)); }

/**
 * Returns the fill for this shape.
 */
public RMFill getFill()  { return _fill; }

/**
 * Sets the fill for this shape.
 */
public void setFill(RMFill aFill)
{
    if(SnapUtils.equals(getFill(), aFill)) return;
    repaint();
    firePropChange(Fill_Prop, _fill, _fill = aFill);
}

/**
 * Returns the effect for this shape.
 */
public Effect getEffect()  { return _effect; }

/**
 * Sets the effect for this shape.
 */
public void setEffect(Effect anEffect)
{
    if(SnapUtils.equals(getEffect(), anEffect)) return;
    repaint();
    firePropChange(Effect_Prop, _effect, _effect = anEffect); _pdvr1 = _pdvr2 = null;
}

/**
 * Returns the color of the shape.
 */
public RMColor getColor()  { return getFill()==null? RMColor.black : getFill().getColor(); }

/**
 * Sets the color of the shape.
 */
public void setColor(RMColor aColor)
{
    // Set color
    if(aColor==null) setFill(null);
    else if(getFill()==null) setFill(new RMFill(aColor));
    else setFill(getFill().copyForColor(aColor));
}

/**
 * Returns the stroke color of the shape.
 */
public RMColor getStrokeColor()  { return getStroke()==null? RMColor.black : getStroke().getColor(); }

/**
 * Sets the stroke color of the shape.
 */
public void setStrokeColor(RMColor aColor)
{
    if(aColor==null) setStroke(null);
    else if(getStroke()==null) setStroke(new RMStroke(aColor, 1));
    else setStroke(getStroke().deriveColor(aColor));
}

/**
 * Returns the stroke width of the shape's stroke in printer points.
 */
public float getStrokeWidth()  { return getStroke()==null? 0 : getStroke().getWidth(); }

/**
 * Sets the stroke width of the shape's stroke in printer points.
 */
public void setStrokeWidth(float aValue)
{
    if(getStroke()==null) setStroke(new RMStroke(RMColor.black, aValue));
    else setStroke(getStroke().deriveWidth(aValue));
}

/**
 * Returns the opacity of the shape (1 for opaque, 0 for transparent).
 */
public double getOpacity()  { return _opacity; }

/**
 * Sets the opacity of the shape (1 for opaque, 0 for transparent).
 */
public void setOpacity(double aValue)
{
    if(aValue==getOpacity()) return; // If value already set, just return
    repaint(); // Register repaint
    firePropChange(Opacity_Prop, _opacity, _opacity = aValue);
}

/**
 * Returns the combined opacity of this shape and its parent.
 */
public double getOpacityDeep()
{
    double op = getOpacity();
    for(RMShape s=_parent; s!=null; s=s._parent) op *= s.getOpacity();
    return op;
}

/**
 * Returns whether this shape is visible.
 */
public boolean isVisible()  { return _visible; }

/**
 * Sets whether this shape is visible.
 */
public void setVisible(boolean aValue)
{
    if(isVisible()==aValue) return;
    firePropChange(Visible_Prop, _visible, _visible = aValue);
}

/**
 * Returns the autosizing settings as a string with hyphens for struts and tilde for sprints (horiz,vert).
 */
public String getAutosizing()  { return _asize!=null? _asize : getAutosizingDefault(); }

/**
 * Sets the autosizing settings as a string with hyphens for struts and tilde for sprints (horiz,vert).
 */
public void setAutosizing(String aValue)
{
    if(aValue!=null && (aValue.length()<7 || !(aValue.charAt(0)=='-' || aValue.charAt(0)=='~'))) {
        System.err.println("RMShape.setAutosizing: Invalid string: " + aValue); return; }
    if(SnapUtils.equals(aValue, _asize)) return;
    firePropChange("Autosizing", _asize, _asize = aValue);
}

/**
 * Returns the autosizing default.
 */
public String getAutosizingDefault()  { return "--~,--~"; }

/**
 * Returns whether this shape is hittable in its parent.
 */
public boolean isHittable()  { return isVisible() && (_parent==null || _parent.isHittable(this)); }

/**
 * Returns the text color for the shape.
 */
public RMColor getTextColor()  { return RMColor.black; }

/**
 * Sets the text color for the shape.
 */
public void setTextColor(RMColor aColor) { }

/**
 * Returns whether font has been set.
 */
public boolean isFontSet()  { return false; }

/**
 * Returns the font for the shape (defaults to parent font).
 */
public RMFont getFont()  { return getParent()!=null? getParent().getFont() : null; }

/**
 * Sets the font for the shape.
 */
public void setFont(RMFont aFont)  { }

/**
 * Returns whether the shape is underlined.
 */
public boolean isUnderlined()  { return false; }

/**
 * Sets the shape to underline.
 */
public void setUnderlined(boolean aFlag)  { }

/**
 * Returns the alignment.
 */
public Pos getAlignment()
{
    AlignX ax = getAlignmentX(); AlignY ay = getAlignmentY();
    if(ax==AlignX.Left && ay==AlignY.Top) return Pos.TOP_LEFT;
    if(ax==AlignX.Center && ay==AlignY.Top) return Pos.TOP_CENTER;
    if(ax==AlignX.Right && ay==AlignY.Top) return Pos.TOP_RIGHT;
    if(ax==AlignX.Left && ay==AlignY.Middle) return Pos.CENTER_LEFT;
    if(ax==AlignX.Center && ay==AlignY.Middle) return Pos.CENTER;
    if(ax==AlignX.Right && ay==AlignY.Middle) return Pos.CENTER_RIGHT;
    if(ax==AlignX.Left && ay==AlignY.Bottom) return Pos.BOTTOM_LEFT;
    if(ax==AlignX.Center && ay==AlignY.Bottom) return Pos.BOTTOM_CENTER;
    return Pos.BOTTOM_RIGHT;
}

/**
 * Sets the alignment.
 */
public void setAlignment(Pos aPos)
{
    switch(aPos.getHPos()) {
        case LEFT: setAlignmentX(AlignX.Left); break; 
        case CENTER: setAlignmentX(AlignX.Center); break;
        case RIGHT: setAlignmentX(AlignX.Right); break; }
    switch(aPos.getVPos()) {
        case TOP: setAlignmentY(AlignY.Top); break; 
        case CENTER: setAlignmentY(AlignY.Middle); break;
        case BOTTOM: setAlignmentY(AlignY.Bottom); break; }
}

/**
 * Returns the horizontal alignment.
 */
public AlignX getAlignmentX()  { return AlignX.Left; }

/**
 * Sets the horizontal alignment.
 */
public void setAlignmentX(AlignX anAlignX)  { }

/**
 * Returns the vertical alignment.
 */
public AlignY getAlignmentY()  { return AlignY.Top; }

/**
 * Sets the vertical alignment.
 */
public void setAlignmentY(AlignY anAlignX)  { }

/**
 * Returns the format for the shape.
 */
public RMFormat getFormat()  { return null; } //if(getBindingCount()>0) return (RMFormat)getBinding(0).getFormat();

/**
 * Sets the format for the shape.
 */
public void setFormat(RMFormat aFormat)
{
    // Add format to first binding
    //if((aFmt==null || aFmt instanceof java.text.Format) && getBindingCount()>0) getBinding(0).setFormat(aFmt);
    
    // Pass down to children
    for(int i=0, iMax=getChildCount(); i<iMax; i++)
        getChild(i).setFormat(aFormat);
}
    
/**
 * Returns the name for the shape.
 */
public String getName()  { return (String)get("Name"); }

/**
 * Sets the name for the shape.
 */
public void setName(String aName)
{
    if(SnapUtils.equals(aName, getName())) return;
    Object oldVal = put("Name", StringUtils.min(aName));
    firePropChange("Name", oldVal, StringUtils.min(aName));
}

/**
 * Sets the URL for the shape.
 */
public String getURL()  { return (String)get("RMShapeURL"); }

/**
 * Returns the URL for the shape.
 */
public void setURL(String aURL)
{
    if(SnapUtils.equals(aURL, getURL())) return;
    Object oldVal = put("RMShapeURL", StringUtils.min(aURL));
    firePropChange("RMShapeURL", oldVal, aURL);
}

/**
 * Returns the locked state of the shape (really just to prevent location/size changes in the editor).
 */
public boolean isLocked()  { return SnapUtils.boolValue(get("Locked")); }

/**
 * Sets the locked state of the shape (really just to prevent location/size changes in the editor).
 */
public void setLocked(boolean aValue)
{
    if(aValue==isLocked()) return;
    Object oldVal = put("Locked", aValue);
    firePropChange(Locked_Prop, oldVal, aValue);
}

/**
 * Returns the Object associated with the given name for the shape.
 * This is a general purpose property facility to allow shapes to hold many less common properties without the overhead
 * of explicitly including ivars for them. The map that holds these properties is shared so that there is only ever one
 * instance of the map for each unique permutation of attributes.
 */
public Object get(String aName)  { return _attrMap.get(aName); }

/**
 * Returns the value associated with given key, using the given default if not found.
 */
public Object get(String aName, Object aDefault)  { Object val = get(aName); return val!=null? val : aDefault; }

/**
 * Sets a value to be associated with the given name for the shape.
 */
public Object put(String aName, Object anObj)
{
    // If map shared, clone it for real
    if(_attrMap.isShared) _attrMap = _attrMap.cloneReal();
    
    // Put value (or remove if null)
    return anObj!=null? _attrMap.put(aName, anObj) : _attrMap.remove(aName);
}

/**
 * Returns the shape's path.
 */
public Shape getPath()  { return new Rect(0, 0, getWidth(), getHeight()); }

/**
 * Returns the parent of this shape.
 */
public RMParentShape getParent()  { return _parent; }

/**
 * Sets the parent of this shape (called automatically by addChild()).
 */
public void setParent(RMParentShape aShape)  { _parent = aShape; }

/**
 * Returns the first parent with given class by iterating up parent hierarchy.
 */
public <T extends RMShape> T getParent(Class<T> aClass)
{
    for(RMShape s=getParent(); s!=null; s=s.getParent()) if(aClass.isInstance(s)) return (T)s;
    return null; // Return null since parent of class wasn't found
}

/**
 * Removes this shape from it's parent.
 */
public void removeFromParent()  { if(_parent!=null) _parent.removeChild(this); }

/**
 * Returns the index of this child in its parent.
 */
public int indexOf()  { return _parent!=null? _parent.indexOfChild(this) : -1; }

/**
 * Returns the child count.
 */
public int getChildCount()  { return 0; }

/**
 * Returns the child at given index.
 */
public RMShape getChild(int anIndex)  { return null; }

/**
 * Returns the children list.
 */
public List <RMShape> getChildren()  { return Collections.emptyList(); }

/**
 * Returns the top level shape (usually an RMDocument).
 */
public RMShape getRootShape()  { return _parent!=null? _parent.getRootShape() : this; }

/**
 * Returns the RMDocument ancestor of this shape.
 */
public RMDocument getDoc()  { return _parent!=null? _parent.getDoc() : null; }

/**
 * Returns the RMDocument ancestor of this shape.
 */
public RMDocument getDocument()  { return getDoc(); }

/**
 * Returns the RMPage ancestor of this shape (or null if not there).
 */
public RMParentShape getPageShape()  { return _parent!=null? _parent.getPageShape() : (RMParentShape)this; }

/**
 * Returns the undoer for this shape (or null if not there).
 */
public Undoer getUndoer()  { return _parent!=null? _parent.getUndoer() : null; }

/**
 * Undoer convenience - sets title of next registered undo.
 */
public void undoerSetUndoTitle(String aTitle) { Undoer u = getUndoer(); if(u!=null) u.setUndoTitle(aTitle); }

/**
 * Undoer convenience - disable the undoer.
 */
public void undoerDisable()  { Undoer u = getUndoer(); if(u!=null) u.disable(); }

/**
 * Undoer convenience - enables the undoer.
 */
public void undoerEnable()  { Undoer u = getUndoer(); if(u!=null) u.enable(); }

/**
 * Editor method - returns whether this shape is at the top level (usually RMPage).
 */
public boolean isRoot()  { return getAncestorCount()<2; }

/**
 * Returns the number of ancestors (from this shape's parent up to the document).
 */
public int getAncestorCount()  { return _parent!=null? getParent().getAncestorCount() + 1 : 0; }

/**
 * Returns the ancestor at the given index (parent is ancestor 0).
 */
public RMShape getAncestor(int anIndex)  { return anIndex==0? getParent() : getParent().getAncestor(anIndex-1); }

/**
 * Returns true if given shape is one of this shape's ancestors.
 */
public boolean isAncestor(RMShape aShape)  { return aShape==_parent || (_parent!=null && _parent.isAncestor(aShape)); }

/**
 * Returns true if given shape is one of this shape's descendants.
 */
public boolean isDescendant(RMShape aShape)  { return aShape!=null && aShape.isAncestor(this); }

/**
 * Converts a point from local to parent.
 */
public Point localToParent(double aX, double aY)
{
    if(isTransformSimple()) return new Point(aX+getX(),aY+getY());
    return getLocalToParent().transform(aX, aY);
}

/**
 * Converts a point from local to given parent.
 */
public Point localToParent(double aX, double aY, RMShape aPar)
{
    Point point = new Point(aX,aY);
    for(RMShape n=this;n!=aPar&&n!=null;n=n.getParent()) {
        if(n.isTransformSimple()) point.offset(n.getX(),n.getY());
        else point = n.localToParent(point.x,point.y); }
    return point;
}

/**
 * Converts a point from local to parent.
 */
public Point localToParent(Point aPoint)  { return localToParent(aPoint.x, aPoint.y); }

/**
 * Converts a point from local to given parent.
 */
public Point localToParent(Point aPoint, RMShape aPar)  { return localToParent(aPoint.x, aPoint.y, aPar); }

/**
 * Converts a shape from local to parent.
 */
public Shape localToParent(Shape aShape)  { return aShape.copyFor(getLocalToParent()); }

/**
 * Converts a point from local to given parent.
 */
public Shape localToParent(Shape aShape, RMShape aPar)  { return aShape.copyFor(getLocalToParent(aPar)); }

/**
 * Converts a point from parent to local.
 */
public Point parentToLocal(double aX, double aY)
{
    if(isTransformSimple()) return new Point(aX-getX(),aY-getY());
    return getParentToLocal().transform(aX, aY);
}

/**
 * Converts a point from given parent to local.
 */
public Point parentToLocal(double aX, double aY, RMShape aPar)  { return getParentToLocal(aPar).transform(aX,aY); }

/**
 * Converts a point from parent to local.
 */
public Point parentToLocal(Point aPoint)  { return parentToLocal(aPoint.x, aPoint.y); }

/**
 * Converts a point from given parent to local.
 */
public Point parentToLocal(Point aPoint, RMShape aPar)  { return parentToLocal(aPoint.x, aPoint.y, aPar); }

/**
 * Converts a shape from parent to local.
 */
public Shape parentToLocal(Shape aShape)  { return aShape.copyFor(getParentToLocal()); }

/**
 * Converts a shape from parent to local.
 */
public Shape parentToLocal(Shape aShape, RMShape aPar)  { return aShape.copyFor(getParentToLocal(aPar)); }

/**
 * Returns the transform.
 */
public Transform getLocalToParent()  { return getTransform(); }

/**
 * Returns the transform.
 */
public Transform getLocalToParent(RMShape aPar)
{
    Transform tfm = getLocalToParent();
    for(RMShape shp=getParent(); shp!=aPar && shp!=null; shp=shp.getParent()) {
        if(shp.isTransformSimple()) tfm.preTranslate(shp.getX(),shp.getY());
        else tfm.multiply(shp.getLocalToParent());
    }
    return tfm;
}

/**
 * Returns the transform from parent to local coords.
 */
public Transform getParentToLocal()
{
    if(isTransformSimple()) return new Transform(-getX(), -getY());
    Transform tfm = getLocalToParent(); tfm.invert(); return tfm;
}

/**
 * Returns the transform from parent to local coords.
 */
public Transform getParentToLocal(RMShape aPar)  { Transform tfm = getLocalToParent(aPar); tfm.invert(); return tfm; }

/**
 * Returns whether transform to parent is simple (contains no rotate, scale, skew).
 */
public boolean isTransformSimple()  { return !isRSS(); }

/**
 * Returns the transform to this shape from its parent.
 */
public Transform getTransform()
{
    // Create transform (if not rotated/scaled/skewed, just translate and return)
    Transform t = new Transform(getX(),getY()); if(!isRSS()) return t;
    
    // Get location, size, point of rotation, rotation, scale, skew
    double w = getWidth(), h = getHeight(), prx = w/2, pry = h/2;
    double roll = getRoll(), sx = getScaleX(), sy = getScaleY(), skx = getSkewX(), sky = getSkewY();
    
    // Transform about point of rotation and return
    t.translate(prx, pry);
    if(roll!=0) t.rotate(roll);
    if(sx!=1 || sy!=1) t.scale(sx, sy);
    if(skx!=0 || sky!=0) t.skew(skx, sky);
    t.translate(-prx, -pry); return t;
}

/**
 * Returns whether shape minimum width is set.
 */
public boolean isMinWidthSet()  { return get(MinWidth_Prop)!=null; }

/**
 * Returns the shape minimum width.
 */
public double getMinWidth()  { Double w = (Double)get(MinWidth_Prop); return w!=null? w : 0; }

/**
 * Sets the shape minimum width.
 */
public void setMinWidth(double aWidth)
{
    double w = aWidth<=0? 0 : aWidth; if(w==getMinWidth()) return;
    firePropChange(MinWidth_Prop, put(MinWidth_Prop, w), w);
}

/**
 * Returns whether shape minimum height is set.
 */
public boolean isMinHeightSet()  { return get(MinHeight_Prop)!=null; }

/**
 * Returns the shape minimum height.
 */
public double getMinHeight()  { Double h = (Double)get(MinHeight_Prop); return h!=null? h : 0; }

/**
 * Sets the shape minimum height.
 */
public void setMinHeight(double aHeight)
{
    double h = aHeight<=0? 0 : aHeight; if(h==getMinHeight()) return;
    firePropChange(MinHeight_Prop, put(MinHeight_Prop, h), h);
}

/**
 * Sets the shape minimum size.
 */
public void setMinSize(double aWidth, double aHeight)  { setMinWidth(aWidth); setMinHeight(aHeight); }

/**
 * Returns whether shape preferred width is set.
 */
public boolean isPrefWidthSet()  { return get(PrefWidth_Prop)!=null; }

/**
 * Returns the shape preferred width.
 */
public double getPrefWidth()
{
    Double v = (Double)get(PrefWidth_Prop); if(v!=null) return v;
    return getPrefWidthImpl(-1);
}

/**
 * Sets the shape preferred width.
 */
public void setPrefWidth(double aWidth)
{
    double w = aWidth<=0? 0 : aWidth; if(w==getPrefWidth()) return;
    firePropChange(PrefWidth_Prop, put(PrefWidth_Prop, w), w);
}

/**
 * Returns whether shape preferred height is set.
 */
public boolean isPrefHeightSet()  { return get(PrefHeight_Prop)!=null; }

/**
 * Returns the shape preferred height.
 */
public double getPrefHeight()
{
    Double v = (Double)get(PrefHeight_Prop); if(v!=null) return v;
    return getPrefHeightImpl(-1);
}

/**
 * Sets the shape preferred height.
 */
public void setPrefHeight(double aHeight)
{
    double h = aHeight<=0? 0 : aHeight; if(h==getPrefHeight()) return;
    firePropChange(PrefHeight_Prop, put(PrefHeight_Prop, h), h);
}

/**
 * Computes the preferred width for given height.
 */
protected double getPrefWidthImpl(double aHeight)  { return getWidth(); }

/**
 * Computes the preferred height for given width.
 */
protected double getPrefHeightImpl(double aWidth)  { return getHeight(); }

/**
 * Returns the best width for current height.
 */
public double getBestWidth()  { return Math.max(getMinWidth(), getPrefWidth()); }

/**
 * Returns the best height for current width.
 */
public double getBestHeight()  { return Math.max(getMinHeight(), getPrefHeight()); }

/**
 * Sets the shape to its best height (which is just the current height for most shapes).
 */
public void setBestHeight()  { setHeight(getBestHeight()); }

/**
 * Sets the shape to its best size.
 */
public void setBestSize()
{
    setWidth(getBestWidth());
    setHeight(getBestHeight());
}

/**
 * Divides this shape by given amount from top edge. Returns remainder shape with bounds set to the remainder.
 */
public RMShape divideShapeFromTop(double anAmt)
{
    // Create remainder shape
    RMShape newShape = createDivideShapeRemainder((byte)0);

    // Get bounds for this shape and remainder bounds (split by amount from top)
    Rect bnds0 = getFrame();
    Rect bnds1 = bnds0.clone(); bnds0.height = anAmt; bnds1.y += anAmt; bnds1.height -= anAmt;
    
    // Set this shape's new bounds and NewShape bounds as remainder
    setFrame(bnds0); newShape.setFrame(bnds1);
    return newShape;
}

/**
 * Divides this shape by given amount from left edge. Returns remainder shape with bounds set to the remainder.
 */
public RMShape divideShapeFromLeft(double anAmt)
{
    // Create remainder shape
    RMShape newShape = createDivideShapeRemainder((byte)1);

    // Get bounds for this shape and remainder bounds (split by amount from left)
    Rect bnds0 = getFrame();
    Rect bnds1 = bnds0.clone(); bnds0.width = anAmt; bnds1.x += anAmt; bnds1.width -= anAmt;
    
    // Set this shape's new bounds and NewShape bounds as remainder
    setFrame(bnds0); newShape.setFrame(bnds1);
    return newShape;
}

/**
 * Creates a shape suitable for the "remainder" portion of a divideShape call (just a clone by default).
 */
protected RMShape createDivideShapeRemainder(byte anEdge)  { return clone(); }

/**
 * Returns whether shape accepts mouse events (true if URL is present).
 */
public boolean acceptsMouse()  { return getURL()!=null; }

/**
 * Handle shape events.
 */
public void processEvent(ViewEvent anEvent)  { }

/**
 * Returns whether this shape is hit by the point, given in this shape's parent's coords.
 */
public boolean contains(Point aPoint)
{
    // Get line width to be used in contain test
    double lineWidth = getStrokeWidth();
    
    // If polygon or line, make line width effectively at least 8, so users will have a better shot of selecting it
    if(this instanceof RMPolygonShape || this instanceof RMLineShape)
        lineWidth = Math.max(8, getStrokeWidth());
    
    // Get bounds, adjusted for line width
    Rect bounds = getBoundsInside();
    bounds.inset(-lineWidth/2, -lineWidth/2);

    // If point isn't even in bounds rect, just return false
    if(!bounds.contains(aPoint.getX(), aPoint.getY()))
        return false;
    
    // Get shape in bounds rect and return whether shape intersects point
    Shape path = getPath();
    return path.contains(aPoint.getX(), aPoint.getY(), lineWidth);
}

/**
 * Returns whether this shape is hit by the path, given in this shape's parent's coords.
 */
public boolean intersects(Shape aPath)
{
    // Get line width to be used in intersects test
    float lineWidth = getStrokeWidth();
    
    // Get bounds, adjusted for line width
    Rect bounds = getBoundsInside();
    bounds.inset(-lineWidth/2, -lineWidth/2);

    // If paths don't even intersect bounds, just return false
    if(!aPath.getBounds().intersectsRect(bounds))
        return false;
    
    // Get shape in bounds and return whether shape intersects given path
    Shape path = getPath();
    return path.intersects(aPath, lineWidth);
}

/**
 * Returns the dataset key associated with this shape.
 */
public String getDatasetKey()  { return null; }

/**
 * Returns the entity this shape should show in keys browser.
 */
public Entity getDatasetEntity()
{
    // Get parent and parent entity (just return null, if null)
    RMShape parent = getParent(); if(parent==null) return null;
    Entity parentEntity = parent.getDatasetEntity(); if(parentEntity==null) return null;
    
    // Get Property/RelationEntity for Shape.DatasetKey
    Property prop = getDatasetKey()!=null? parentEntity.getKeyPathProperty(getDatasetKey()) : null;
    Entity entity = prop!=null && prop.isRelation()? prop.getRelationEntity() : null;
    return entity!=null? entity : parentEntity;
}

/**
 * Returns the property names for helper's instance class.
 */
public String[] getPropNames()
{
    return new String[] { Visible_Prop, X_Prop, Y_Prop, Width_Prop, Height_Prop, Roll_Prop, ScaleX_Prop, ScaleY_Prop,
        "Font", "TextColor", "FillColor", "StrokeColor", "URL" };
}

/**
 * Returns the number of bindings associated with shape.
 */
public int getBindingCount()  { List bindings = getBindings(false); return bindings!=null? bindings.size() : 0; }

/**
 * Returns the individual binding at the given index.
 */
public Binding getBinding(int anIndex)  { return getBindings(true).get(anIndex); }

/**
 * Returns the list of bindings, with an option to create if missing.
 */
protected List <Binding> getBindings(boolean doCreate)
{
    List <Binding> bindings = (List)get("RMBindings");
    if(bindings==null && doCreate) put("RMBindings", bindings = new ArrayList());
    return bindings;
}

/**
 * Adds the individual binding to the shape's bindings list.
 */
public void addBinding(Binding aBinding)
{
    removeBinding(aBinding.getPropertyName()); // Remove current binding for property name (if it exists)
    List <Binding> bindings = getBindings(true); // Add binding
    bindings.add(aBinding);
    aBinding.setView(this); // Set binding width to this shape
}

/**
 * Removes the binding at the given index from shape's bindings list.
 */
public Binding removeBinding(int anIndex)  { return getBindings(true).remove(anIndex); }

/**
 * Returns the individual binding with the given property name.
 */
public Binding getBinding(String aPropertyName)
{
    // Iterate over bindings and return the first that matches given property name
    for(int i=0, iMax=getBindingCount(); i<iMax; i++)
        if(getBinding(i).getPropertyName().equals(aPropertyName))
            return getBinding(i);
    return null; // Return null since binding not found
}

/**
 * Removes the binding with given property name.
 */
public boolean removeBinding(String aPropertyName)
{
    // Iterate over binding and remove given binding
    for(int i=0, iMax=getBindingCount(); i<iMax; i++)
        if(getBinding(i).getPropertyName().equals(aPropertyName)) {
            removeBinding(i); return true; }
    return false; // Return false since binding not found
}

/**
 * Adds a binding for given name and key.
 */
public void addBinding(String aPropName, String aKey)  { addBinding(new Binding(aPropName, aKey)); }

/**
 * Standard implementation of Object clone. Null's out shape's parent and children.
 */
public RMShape clone()
{
    // Do normal version
    RMShape clone = null; try { clone = (RMShape)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
    
    // Clear Parent and PropChangeSupport
    clone._parent = null;
    clone._pcs = PropChangeSupport.EMPTY;
    
    // Clone Rotate/Scale/Skew array
    if(_rss!=null) clone._rss = Arrays.copyOf(_rss,_rss.length);
    
    // Copy attributes map
    clone._attrMap = _attrMap.clone();
    
    // Clone bindings and add to clone (with hack to make sure clone has it's own, non-shared, attr map)
    for(int i=0, iMax=getBindingCount(); i<iMax; i++) {
        if(i==0) clone.put("RMBindings", null);
        clone.addBinding(getBinding(i).clone());
    }
    
    // Return clone
    return clone;
}

/**
 * Clones all attributes of this shape with complete clones of its children as well.
 */
public RMShape cloneDeep()  { return clone(); }

/**
 * Copies basic shape attributes from given RMShape (location, size, fill, stroke, roll, scale, name, url, etc.).
 */
public void copyShape(RMShape aShape)
{
    // Copy bounds
    setBounds(aShape._x, aShape._y, aShape._width, aShape._height);
    
    // Copy roll, scale, skew
    if(aShape.isRSS()) {
        setRoll(aShape.getRoll());
        setScaleXY(aShape.getScaleX(), aShape.getScaleY());
        setSkewXY(aShape.getSkewX(), aShape.getSkewY());
    }
    
    // Copy Stroke, Fill, Effect
    setStroke(aShape.getStroke());
    setFill(aShape.getFill());
    setEffect(aShape.getEffect());
    
    // Copy Opacity and Visible
    setOpacity(aShape.getOpacity());
    setVisible(aShape.isVisible());
    
    // Copy Name, URL, Autosizing
    setName(aShape.getName());
    setURL(aShape.getURL());
    setAutosizing(aShape.getAutosizing());
    
    // Copy bindings
    while(getBindingCount()>0) removeBinding(0);
    for(int i=0, iMax=aShape.getBindingCount(); i<iMax; i++)
        addBinding(aShape.getBinding(i).clone());
}

/**
 * Generate report with report owner.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    RMShape clone = rpgShape(anRptOwner, aParent);
    rpgBindings(anRptOwner, clone);
    return clone;
}

/**
 * Generate report with report owner.
 */
protected RMShape rpgShape(ReportOwner anRptOwner, RMShape aParent)  { return clone(); }

/**
 * Report generation for URL and bindings.
 */
public void rpgBindings(ReportOwner anRptOwner, RMShape aShapeRPG)
{
    // Clone URL
    if(getURL()!=null && getURL().length()>0 && getURL().indexOf('@')>=0) {
        RMXString url = new RMXString(getURL()).rpgClone(anRptOwner, null, aShapeRPG, false);
        aShapeRPG.setURL(url.getText());
    }
    
    // Iterate over bindings and evaluate
    for(int i=0; i<getBindingCount(); i++) { Binding binding = getBinding(i);
        
        // Get PropertyName, Key and Value (just continue if empty key)
        String pname = binding.getPropertyName(); if(pname==null) continue;
        String key = binding.getKey(); if(key==null || key.length()==0) continue;
        Object value = RMKeyChain.getValue(anRptOwner, key);
        
        // Handle Font
        if(pname.equals("Font")) {
            
            // Get value as string (if zero length, just continue)
            String fs = value instanceof String? (String)value : null; if(fs==null || fs.length()==0) continue;
            
            // If string has underline in it, underline and delete
            if(StringUtils.indexOfIC(fs, "Underline")>=0) {
                aShapeRPG.setUnderlined(true); fs = StringUtils.deleteIC(fs, "Underline").trim(); }
            
            // Get size from string (if found, strip size from string)
            int sizeIndex = fs.lastIndexOf(" ");
            double size = sizeIndex<0 ? 0 : SnapUtils.floatValue(fs.substring(sizeIndex+1));
            if(size>0) fs = fs.substring(0, Math.max(sizeIndex, 0)).trim();
            else size = getFont()==null? 12 : getFont().getSize();
            
            // Get root font (use default font if not found), and modified font
            RMFont font = getFont(); if(font==null) font = RMFont.getDefaultFont();
            if(fs.equalsIgnoreCase("Bold")) font = font.getBold();
            else if(fs.equalsIgnoreCase("Italic")) font = font.getItalic();
            else if(fs.length()>0) // If there is anything in string, try to parse font name
                font = new RMFont(fs, size);
            
            // Get font at right size and apply it
            font = font.deriveFont(size);
            aShapeRPG.setFont(font);
        }

        // Handle FillColor, StrokeColor, TextColor
        else if(pname.equals("FillColor")) { RMColor color = RMColor.get(value); 
            if(color!=null) aShapeRPG.setColor(color); }
        else if(pname.equals("StrokeColor")) { RMColor color = RMColor.get(value); 
            if(color!=null) aShapeRPG.setStrokeColor(color); }
        else if(pname.equals("TextColor")) { RMColor color = RMColor.get(value); 
            if(color!=null) aShapeRPG.setTextColor(color); }
        
        // Handle others: X, Y, Width, Height, Visible, URL
        else RMKey.setValueSafe(aShapeRPG, pname, value);
    }
}

/**
 * Replaces all @Page@ style keys with their actual values for this shape and it's children.
 */
protected void resolvePageReferences(ReportOwner aRptOwner, Object userInfo)
{
    // If URL has @-sign, do rpg clone in case it is page reference
    if(getURL()!=null && getURL().length()>0 && getURL().indexOf('@')>=0) {
        RMXString url = new RMXString(getURL()).rpgClone(aRptOwner, userInfo, null, false);
        setURL(url.getText());
    }
}

/**
 * Called to relayout.
 */
public void relayout()  { }

/**
 * Called to notify parents to relayout because preferred sizes have potentially changed.
 */
public void relayoutParent()
{
    RMParentShape par = getParent(); if(par==null) return;
    par.relayout(); par.relayoutParent();
}

/**
 * Called to register view for repaint.
 */
public void repaint()
{
    RMShape root = getRootShape(); if(root==null) return;
    root.repaint(this);
}

/** Called to register view for repaint. */
protected void repaint(RMShape aShape)  { }

/** Editor method - indicates whether this shape can be super selected. */
public boolean superSelectable()  { return getClass()==RMParentShape.class; }

/** Editor method. */
public boolean acceptsChildren()  { return getClass()==RMParentShape.class; }

/** Editor method. */
public boolean childrenSuperSelectImmediately()  { return _parent==null; }

/**
 * Page number resolution.
 */
public int page()  { return _parent!=null? _parent.page() : 0; }

/**
 * Page number resolution.
 */
public int pageMax()  { return _parent!=null? _parent.pageMax() : 0; }

/**
 * Returns the "PageBreak" for this shape as defined by shapes that define a page break (currently only RMTable).
 */
public int getPageBreak()  { return _parent!=null? _parent.getPageBreak() : 0; }

/**
 * Returns the "PageBreakMax" for this shape as defined by shapes that define a page break (currently only RMTable).
 */
public int getPageBreakMax()  { return _parent!=null? _parent.getPageBreakMax() : 0; }

/**
 * Returns the "PageBreakPage" for this shape, or the page number relative to the last page break,
 * as defined by shapes that define explicit page breaks (currently only RMTable).
 */
public int getPageBreakPage()  { return _parent!=null? _parent.getPageBreakPage() : 0; }

/**
 * Returns the "PageBreakPageMax" for this shape, or the max page number relative to the last and next page breaks,
 * as defined by shapes that define explicit page breaks (currently only RMTable).
 */
public int getPageBreakPageMax()  { return _parent!=null? _parent.getPageBreakPageMax() : 0; }

/**
 * Top-level generic shape painting - sets transform and opacity then does a paintAll.
 * If a effect is present, has it paint instead of doing paintAll.
 */
public void paint(Painter aPntr)
{
    // Clone graphics
    aPntr.save();
    
    // Apply transform for shape
    if(isRSS()) aPntr.transform(getTransform());
    else aPntr.translate(getX(), getY());
    
    // If shape bounds don't intersect clip bounds, just return
    Rect cbounds = aPntr.getClipBounds();
    if(cbounds!=null && !getBoundsMarkedDeep().intersects(cbounds)) {
        aPntr.restore(); return; }
    
    // If shape is semi-transparent, apply composite
    if(getOpacityDeep()!=1) {
        RMShapePaintProps props = RMShapePaintProps.get(aPntr);
        double op = props.isEditing()? Math.max(.15, getOpacityDeep()) : getOpacityDeep();
        aPntr.setOpacity(op);
    }
    
    // If shape has a effect, have it paint
    if(getEffect()!=null) { Effect eff = getEffect();
        PainterDVR pdvr = new PainterDVR(aPntr);
        paintShapeAll(pdvr);
        if(!pdvr.equals(_pdvr1)) {
            _pdvr1 = pdvr; _pdvr2 = new PainterDVR();
            eff.applyEffect(pdvr, _pdvr2, getBoundsStrokedDeep());
        }
        _pdvr2.exec(aPntr);
    }
    
    // Otherwise paintShapeAll
    else paintShapeAll(aPntr);
    
    // Dispose of graphics
    aPntr.restore();
}

// DVR painters for caching effect drawing
PainterDVR _pdvr1, _pdvr2;

/**
 * Calls paintShape, paintShapeChildren and paintShapeOver.
 */
public void paintShapeAll(Painter aPntr)
{
    // Get graphics
    boolean didGsave = false;
    
    // If clipping, clip to shape
    if(getClipShape()!=null) {
        aPntr.save(); didGsave = true;
        aPntr.clip(getClipShape());
    }
        
    // Have shape paint only itself
    paintShape(aPntr);
    
    // Have shape paint children
    paintShapeChildren(aPntr);
    
    // If graphics copied, dispose
    if(didGsave) aPntr.restore();
        
    // Have shape paint over
    paintShapeOver(aPntr);
}

/**
 * Basic shape painting - paints shape fill and stroke.
 */
protected void paintShape(Painter aPntr)
{
    // Get fill/stroke
    RMFill fill = getFill(); RMStroke stroke = getStroke();
    
    // Paint fill
    if(fill!=null) { //getFill().paint(aPntr, this);
        aPntr.setPaint(fill.snap().copyFor(getBoundsInside()));
        aPntr.fill(getPath());
    }
    
    // Paint stroke
    if(stroke!=null && !isStrokeOnTop()) { //getStroke().paint(aPntr, this);
        aPntr.setPaint(stroke.getColor());
        aPntr.setStroke(stroke.snap());
        Shape path = getPath(), spath = stroke.getStrokePath(path);
        aPntr.draw(spath);
    }
}

/**
 * Paints shape children.
 */
protected void paintShapeChildren(Painter aPntr)
{
    for(int i=0, iMax=getChildCount(); i<iMax; i++) { RMShape child = getChild(i);
        if(child.isVisible())
            child.paint(aPntr); }
}

/**
 * Paints after (on top) of children.
 */
protected void paintShapeOver(Painter aPntr)
{
    RMStroke stroke = getStroke();
    if(stroke!=null && isStrokeOnTop()) { //getStroke().paint(aPntr, this);
        aPntr.setPaint(stroke.getColor());
        aPntr.setStroke(stroke.snap());
        Shape path = getPath(), spath = stroke.getStrokePath(path);
        aPntr.draw(spath);
    }
}

/**
 * Returns whether to stroke on top.
 */
public boolean isStrokeOnTop()  { return false; }

/**
 * Returns clip shape for shape.
 */
public Shape getClipShape()  { return null; }

/**
 * Add listener.
 */
public void addPropChangeListener(PropChangeListener aLsnr)
{
    if(_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
    _pcs.addPropChangeListener(aLsnr);
}

/**
 * Remove listener.
 */
public void removePropChangeListener(PropChangeListener aLsnr)  { _pcs.removePropChangeListener(aLsnr); }

/**
 * Adds a deep change listener to shape to listen for shape changes and property changes received by shape.
 */
public void addDeepChangeListener(DeepChangeListener aLsnr)
{
    if(_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
    _pcs.addDeepChangeListener(aLsnr);
}

/**
 * Removes a deep change listener from shape.
 */
public void removeDeepChangeListener(DeepChangeListener aLsnr)  { _pcs.removeDeepChangeListener(aLsnr); }

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal)
{
    if(!_pcs.hasListener(aProp)) return;
    PropChange pc = new PropChange(this, aProp, oldVal, newVal);
    firePropChange(pc);
}

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
{
    if(!_pcs.hasListener(aProp)) return;
    PropChange pc = new PropChange(this, aProp, oldVal, newVal, anIndex);
    firePropChange(pc);
}

/**
 * Fires a given property change.
 */
protected void firePropChange(PropChange aPC)
{
    _pcs.firePropChange(aPC);
}

/**
 * Returns the value for given key.
 */
public Object getKeyValue(String aPropName)
{
    // Handle properties
    switch(aPropName) {
        case X_Prop: return getX();
        case Y_Prop: return getY();
        case Width_Prop: return getWidth();
        case Height_Prop: return getHeight();
        case Roll_Prop: return getRoll();
        case ScaleX_Prop: return getScaleX();
        case ScaleY_Prop: return getScaleY();
        case SkewX_Prop: return getSkewX();
        case SkewY_Prop: return getSkewY();
        case Stroke_Prop: return getStroke();
        case Fill_Prop: return getFill();
        case Effect_Prop: return getEffect();
        case Opacity_Prop: return getOpacity();
        case Name_Prop: return getName();
        case Visible_Prop: return isVisible();
        case Locked_Prop: return isLocked();
        case MinWidth_Prop: return getMinWidth();
        case MinHeight_Prop: return getMinHeight();
        case PrefWidth_Prop: return getPrefWidth();
        case PrefHeight_Prop: return getPrefHeight();
        default: return Key.getValueImpl(this, aPropName);
    }
}

/**
 * Sets the value for given key.
 */
public void setKeyValue(String aPropName, Object aValue)
{
    // Handle properties
    switch(aPropName) {
        case X_Prop: setX(SnapUtils.doubleValue(aValue)); break;
        case Y_Prop: setY(SnapUtils.doubleValue(aValue)); break;
        case Width_Prop: setWidth(SnapUtils.doubleValue(aValue)); break;
        case Height_Prop: setHeight(SnapUtils.doubleValue(aValue)); break;
        case Roll_Prop: setRoll(SnapUtils.doubleValue(aValue)); break;
        case ScaleX_Prop: setScaleX(SnapUtils.doubleValue(aValue)); break;
        case ScaleY_Prop: setScaleY(SnapUtils.doubleValue(aValue)); break;
        case SkewX_Prop: setSkewX(SnapUtils.doubleValue(aValue)); break;
        case SkewY_Prop: setSkewY(SnapUtils.doubleValue(aValue)); break;
        case Stroke_Prop: setStroke(aValue instanceof RMStroke? (RMStroke)aValue : null); break;
        case Fill_Prop: setFill(aValue instanceof RMFill? (RMFill)aValue : null); break;
        case Effect_Prop: setEffect(aValue instanceof Effect? (Effect)aValue : null); break;
        case Opacity_Prop: setOpacity(SnapUtils.doubleValue(aValue)); break;
        case Name_Prop: setName(SnapUtils.stringValue(aValue)); break;
        case Visible_Prop: setVisible(SnapUtils.boolValue(aValue)); break;
        case Locked_Prop: setLocked(SnapUtils.boolValue(aValue)); break;
        case MinWidth_Prop: setMinWidth(SnapUtils.doubleValue(aValue)); break;
        case MinHeight_Prop: setMinHeight(SnapUtils.doubleValue(aValue)); break;
        case PrefWidth_Prop: setPrefWidth(SnapUtils.doubleValue(aValue)); break;
        case PrefHeight_Prop: setPrefHeight(SnapUtils.doubleValue(aValue)); break;
        default: Key.setValueReflectSafe(this, aPropName, aValue);
    }
}

/**
 * XML Archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element called shape
    XMLElement e = new XMLElement("shape");
    
    // Archive name
    if(getName()!=null && getName().length()>0) e.add("name", getName());
    
    // Archive X, Y, Width, Height
    if(_x!=0) e.add("x", _x);
    if(_y!=0) e.add("y", _y);
    if(_width!=0) e.add("width", _width);
    if(_height!=0) e.add("height", _height);
    
    // Archive Roll, ScaleX, ScaleY, SkewX, SkewY
    if(getRoll()!=0) e.add("roll", getRoll());
    if(getScaleX()!=1) e.add("scalex", getScaleX());
    if(getScaleY()!=1) e.add("scaley", getScaleY());
    if(getSkewX()!=0) e.add("skewx", getSkewX());
    if(getSkewY()!=0) e.add("skewy", getSkewY());

    // Archive Stroke, Fill, Effect
    if(getStroke()!=null) e.add(anArchiver.toXML(getStroke(), this));
    if(getFill()!=null) e.add(anArchiver.toXML(getFill(), this));
    if(getEffect()!=null) e.add(anArchiver.toXML(getEffect(), this));
    
    // Archive font
    if(isFontSet()) e.add(getFont().toXML(anArchiver));
    
    // Archive Opacity, Visible
    if(getOpacity()<1) e.add("opacity", getOpacity());
    if(!isVisible()) e.add("visible", false);
    
    // Archive URL
    if(getURL()!=null && getURL().length()>0) e.add("url", getURL());
    
    // Archive MinWidth, MinHeight, PrefWidth, PrefHeight
    if(isMinWidthSet()) e.add(MinWidth_Prop, getMinWidth());
    if(isMinHeightSet()) e.add(MinHeight_Prop, getMinHeight());
    if(isPrefWidthSet()) e.add(PrefWidth_Prop, getPrefWidth());
    if(isPrefHeightSet()) e.add(PrefHeight_Prop, getPrefHeight());
    
    // Archive Autosizing
    if(!getAutosizing().equals(getAutosizingDefault())) e.add("asize", getAutosizing());
    
    // Archive Locked
    if(isLocked()) e.add("locked", true);
    
    // Archive bindings
    for(int i=0, iMax=getBindingCount(); i<iMax; i++)
        e.add(getBinding(i).toXML(anArchiver));

    // Return the element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive name
    setName(anElement.getAttributeValue("name"));
    
    // Unarchive X, Y, Width, Height
    _x = anElement.getAttributeFloatValue("x", 0);
    _y = anElement.getAttributeFloatValue("y", 0);
    _width = anElement.getAttributeFloatValue("width", 0);
    _height = anElement.getAttributeFloatValue("height", 0);
    
    // Unarchive Roll, ScaleX, ScaleY, SkewX, SkewY
    setRoll(anElement.getAttributeFloatValue("roll"));
    setScaleX(anElement.getAttributeFloatValue("scalex", 1));
    setScaleY(anElement.getAttributeFloatValue("scaley", 1));
    setSkewX(anElement.getAttributeFloatValue("skewx", 0));
    setSkewY(anElement.getAttributeFloatValue("skewy", 0));

    // Unarchive Stroke 
    for(int i=anArchiver.indexOf(anElement, RMStroke.class); i>=0; i=-1) {
        RMStroke stroke = (RMStroke)anArchiver.fromXML(anElement.get(i), this);
        setStroke(stroke);
    }
    
    // Unarchive Fill 
    for(int i=anArchiver.indexOf(anElement, RMFill.class); i>=0; i=-1) {
        RMFill fill = (RMFill)anArchiver.fromXML(anElement.get(i), this);
        setFill(fill);
    }
    
    // Unarchive Effect
    for(int i=anArchiver.indexOf(anElement, Effect.class); i>=0; i=-1) {
        Effect fill = (Effect)anArchiver.fromXML(anElement.get(i), this);
        setEffect(fill);
    }
    
    // Unarchive font
    XMLElement fontXML = anElement.getElement("font");
    if(fontXML!=null) setFont((RMFont)anArchiver.fromXML(fontXML, this));
    
    // Unarchive Opacity, Visible
    setOpacity(anElement.getAttributeFloatValue("opacity", 1));
    if(anElement.hasAttribute("visible")) _visible = anElement.getAttributeBoolValue("visible");
    
    // Unarchive URL
    setURL(anElement.getAttributeValue("url"));
    
    // Unarchive MinWidth, MinHeight, PrefWidth, PrefHeight
    if(anElement.hasAttribute(MinWidth_Prop)) setMinWidth(anElement.getAttributeFloatValue(MinWidth_Prop));
    if(anElement.hasAttribute(MinHeight_Prop)) setMinHeight(anElement.getAttributeFloatValue(MinHeight_Prop));
    if(anElement.hasAttribute(PrefWidth_Prop)) setPrefWidth(anElement.getAttributeFloatValue(PrefWidth_Prop));
    if(anElement.hasAttribute(PrefHeight_Prop)) setPrefHeight(anElement.getAttributeFloatValue(PrefHeight_Prop));
    
    // Unarchive Autosizing
    String asize = anElement.getAttributeValue("asize");
    if(asize==null) asize = anElement.getAttributeValue("LayoutInfo");
    if(asize!=null) setAutosizing(asize);
    
    // Unarchive Locked
    setLocked(anElement.getAttributeBoolValue("locked"));
    
    // Unarchive bindings
    for(int i=anElement.indexOf("binding"); i>=0; i=anElement.indexOf("binding",i+1)) { XMLElement bxml=anElement.get(i);
        addBinding(new Binding().fromXML(anArchiver, bxml)); }

    // Unarchive property keys (legacy)
    for(int i=anElement.indexOf("property-key"); i>=0; i=anElement.indexOf("property-key", i+1)) {
        XMLElement prop = anElement.get(i); String name = prop.getAttributeValue("name");
        if(name.equals("FontColor")) name = "TextColor"; if(name.equals("IsVisible")) name = "Visible";
        String key = prop.getAttributeValue("key"); addBinding(new Binding(name, key));
    }

    // Return this shape
    return this;
}

/**
 * Standard to string implementation (prints class name and shape bounds).
 */
public String toString()
{
    StringBuffer sb = new StringBuffer(getClass().getSimpleName()).append(' ');
    if(getName()!=null) sb.append(getName()).append(' ');
    sb.append(getFrame().toString());
    return sb.toString();
}

/**
 * A HashMap subclass to hold uncommon attributes, with Shared flag to indicate whether it has to be copied when modded.
 */
private static class RMSharedMap extends HashMap {
    
    // Whether this map is being shared
    boolean isShared = false;
    
    /** Overrides hashtable method to just mark hashtable shared and return it. */
    public RMSharedMap clone()  { isShared = true; return this; }

    /** Provides real clone implementation. */
    public RMSharedMap cloneReal() { RMSharedMap cln = (RMSharedMap)super.clone(); cln.isShared = false; return cln; }
}

}