/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.geom.Path2D;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.util.*;

/**
 * This stroke subclass strokes the rectangular border of a given shape, with option include/exclude
 * individual sides.
 */
public class RMBorderStroke extends RMStroke {

    // Whether to show left border
    boolean _showLeft = true;

    // Whether to show right border
    boolean _showRight = true;

    // Whether to show top border
    boolean _showTop = true;

    // Whether to show left border
    boolean _showBottom = true;

    /**
     * Constructor.
     */
    public RMBorderStroke()
    {
        super();
    }

    /**
     * Constructor for sides.
     */
    public RMBorderStroke(boolean showTop, boolean showRight, boolean showBottom, boolean showLeft)
    {
        this();
        _showLeft = showLeft;
        _showRight = showRight;
        _showTop = showTop;
        _showBottom = showBottom;
    }

    /**
     * Returns whether to show left border.
     */
    public boolean isShowLeft()  { return _showLeft; }

    /**
     * Returns whether to show right border.
     */
    public boolean isShowRight()  { return _showRight; }

    /**
     * Returns whether to show top border.
     */
    public boolean isShowTop()  { return _showTop; }

    /**
     * Returns whether to show bottom border.
     */
    public boolean isShowBottom()  { return _showBottom; }

    /**
     * Returns whether to show all borders.
     */
    public boolean isShowAll()  { return _showLeft && _showRight && _showTop && _showBottom; }

    /**
     * Returns the path to be stroked, transformed from the input path.
     */
    public Shape getStrokePath(Shape aShape)
    {
        // If showing all borders, just return bounds
        Rect rect = aShape.getBounds();
        if (isShowAll())
            return rect;
        boolean showTop = isShowTop();
        boolean showRight = isShowRight();
        boolean showBottom = isShowBottom();
        boolean showLeft = isShowLeft();
        double shapeW = rect.width;
        double shapeH = rect.height;

        // Otherwise, build path based on sides showing and return
        Path2D path = new Path2D();
        if (showTop) {
            path.moveTo(0, 0);
            path.lineTo(shapeW, 0);
        }
        if (showRight) {
            if (!showTop) path.moveTo(shapeW, 0);
            path.lineTo(shapeW, shapeH);
        }
        if (showBottom) {
            if (!showRight) path.moveTo(shapeW, shapeH);
            path.lineTo(0, shapeH);
        }
        if (showLeft) {
            if (!showBottom) path.moveTo(0, shapeH);
            path.lineTo(0, 0);
        }

        // Return
        return path;
    }

    /**
     * Returns a duplicate stroke with new ShowTop.
     */
    public RMBorderStroke deriveTop(boolean aValue)
    {
        RMBorderStroke newStroke = clone();
        newStroke._showTop = aValue;
        return newStroke;
    }

    /**
     * Returns a duplicate stroke with new ShowRight.
     */
    public RMBorderStroke deriveRight(boolean aValue)
    {
        RMBorderStroke newStroke = clone();
        newStroke._showRight = aValue;
        return newStroke;
    }

    /**
     * Returns a duplicate stroke with new ShowBottom.
     */
    public RMBorderStroke deriveBottom(boolean aValue)
    {
        RMBorderStroke newStroke = clone();
        newStroke._showBottom = aValue;
        return newStroke;
    }

    /**
     * Returns a duplicate stroke with new ShowLeft.
     */
    public RMBorderStroke deriveLeft(boolean aValue)
    {
        RMBorderStroke newStroke = clone();
        newStroke._showLeft = aValue;
        return newStroke;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        // Check identity, super, class and get other
        if (anObj == this) return true;
        if (!super.equals(anObj)) return false;
        if (!(anObj instanceof RMBorderStroke)) return false;
        RMBorderStroke other = (RMBorderStroke) anObj;

        // Check ShowLeft, ShowRight, ShowTop, ShowBottom
        if (other._showLeft != _showLeft) return false;
        if (other._showRight != _showRight) return false;
        if (other._showTop != _showTop) return false;
        if (other._showBottom != _showBottom) return false;

        // Return true since all checks passed
        return true;
    }

    /**
     * Standard clone implementation.
     */
    public RMBorderStroke clone()
    {
        return (RMBorderStroke) super.clone();
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        // Archive basic stroke attributes
        XMLElement e = super.toXML(anArchiver);
        e.add("type", "border");

        // Archive ShowLeft, ShowRight, ShowTop, ShowBottom
        if (!isShowLeft()) e.add("show-left", false);
        if (!isShowRight()) e.add("show-right", false);
        if (!isShowTop()) e.add("show-top", false);
        if (!isShowBottom()) e.add("show-bottom", false);
        return e;
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic stroke attributes
        super.fromXML(anArchiver, anElement);

        // Unarchive ShowLeft, ShowRight, ShowTop, ShowBottom
        if (anElement.hasAttribute("show-left")) _showLeft = anElement.getAttributeBoolValue("show-left");
        if (anElement.hasAttribute("show-right")) _showRight = anElement.getAttributeBoolValue("show-right");
        if (anElement.hasAttribute("show-top")) _showTop = anElement.getAttributeBoolValue("show-top");
        if (anElement.hasAttribute("show-bottom")) _showBottom = anElement.getAttributeBoolValue("show-bottom");
        return this;
    }
}