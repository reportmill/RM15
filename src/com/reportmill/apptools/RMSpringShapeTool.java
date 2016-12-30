/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;

/**
 * Tool for RMSpringShape.
 */
public class RMSpringShapeTool <T extends RMSpringShape> extends RMParentShapeTool <T> {

/**
 * Override to return shape class.
 */
public Class <T> getShapeClass()  { return (Class<T>)RMSpringShape.class; }

/**
 * Returns whether a given shape is super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Returns whether a given shape accepts children.
 */
public boolean getAcceptsChildren(RMShape aShape)  { return true; }

}
