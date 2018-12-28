package com.reportmill.graphics;
import snap.gfx.*;

/**
 * This class represents a 3D shape to be rendered in a G3DView.
 */
public abstract class Shape3D {

    // Shape fill
    Color      _fill;
    
    // Shape stroke
    Stroke     _stroke = Stroke.Stroke1;
    
    // Shape Stroke color
    Color      _strokeColor;
    
    // Shape opacity
    double     _opacity = 1;
    
    // The bounds
    Rect       _bounds = new Rect(), _boundsLocal;
    
//
public double x()  { return _bounds.x; }
public double y()  { return _bounds.y; }
public double getWidth()  { return _bounds.width; }
public double getHeight()  { return _bounds.height; }

/**
 * Returns bounds of shape.
 */
public Rect getBounds()  { return _bounds; }

/**
 * Sets bounds of shape.
 */
public void setBounds(Rect aRect) { _bounds = aRect.clone(); _boundsLocal = new Rect(0, 0, aRect.width, aRect.height); }

/**
 * Sets the x/y.
 */
public void setXY(double aX, double aY)  { setBounds(new Rect(x(), y(), getWidth(), getHeight())); }

/**
 * Returns the bounds inside.
 */
public Rect getBoundsLocal()  { return _boundsLocal; }

/**
 * Returns the color of shape.
 */
public Color getColor()  { return _fill; }

/**
 * Sets the color of shape.
 */
public void setColor(Color aColor)  { _fill = aColor; }

/**
 * Returns the stroke of shape.
 */
public Stroke getStroke()  { return _stroke; }

/**
 * Sets the stroke of shape.
 */
public void setStroke(Stroke aStroke)  { _stroke = aStroke; }

/**
 * Returns the stroke color of shape.
 */
public void setStroke(Color aColor, double aWidth)  { setStrokeColor(aColor); setStroke(new Stroke(aWidth)); }

/**
 * Returns the stroke color of shape.
 */
public Color getStrokeColor()  { return _strokeColor; }

/**
 * Sets the stroke color of shape.
 */
public void setStrokeColor(Color aColor)  { _strokeColor = aColor; }

/**
 * Returns the opacity of shape.
 */
public double getOpacity()  { return _opacity; }

/**
 * Sets the opacity of shape.
 */
public void setOpacity(double aValue)  { _opacity = aValue; }

/**
 * Can go soon?
 */
public void addChild(Shape3D aShape)
{
    System.err.println("Shape3D.addChild: Not impled");
}

/**
 * Returns the array of Path3D that can render this shape.
 */
public abstract Path3D[] getPath3Ds();

}