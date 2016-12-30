/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;

/**
 * A class to hold a shape roll, scale and skew.
 */
public class RMShapeRSS implements Cloneable {

    // The shape roll
    double    roll;
    
    // The shape scale x
    double    scaleX = 1;
    
    // The shape scale y
    double    scaleY = 1;
    
    // The shape skew x
    double    skewX;
    
    // The shape skew y
    double    skewY;
    
/**
 * Standard clone implementation.
 */
public RMShapeRSS clone()
{
    try { return (RMShapeRSS)super.clone(); }
    catch(Exception e) { System.err.println(e); return null; }
}

}