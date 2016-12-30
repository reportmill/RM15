/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;

/**
 * A Painter.Props implementation to provide selection information about shapes.
 */
public class RMShapePaintProps extends Painter.Props {

/**
 * Returns whether painting is for editor.
 */
public boolean isEditing()  { return false; }

/**
 * Returns whether given shape is selected.
 */
public boolean isSelected(RMShape aShape)  { return false; }

/**
 * Returns whether given shape is super selected.
 */
public boolean isSuperSelected(RMShape aShape)  { return false; }

/**
 * Returns whether given shape is THE super selected shape.
 */
public boolean isSuperSelectedShape(RMShape aShape)  { return false; }

/**
 * Returns the props for a painter.
 */
public static RMShapePaintProps get(Painter aPntr)
{
    Painter.Props props = aPntr.getProps();
    return props instanceof RMShapePaintProps? (RMShapePaintProps)props : _props;
}

/**
 * Returns whether painter is editing.
 */
public static boolean isEditing(Painter aPntr)  { return get(aPntr).isEditing(); }

/**
 * Returns whether given shape is selected.
 */
public static boolean isSelected(Painter aPntr, RMShape aShape)  { return get(aPntr).isSelected(aShape); }

/**
 * Returns whether given shape is super selected.
 */
public static boolean isSuperSelected(Painter aPntr, RMShape aShape)  { return get(aPntr).isSuperSelected(aShape); }

// Shared props for normal paint case
private static RMShapePaintProps _props = new RMShapePaintProps();

}