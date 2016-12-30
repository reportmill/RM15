/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;

/**
 * This class represents a simple 2D transform.
 */
public class RMTransform extends Transform {
    
    // Identity transform
    public static final RMTransform identity = new RMTransform();

/** Creates a new identity transform. */
public RMTransform() { }

/** Creates a transform initialized to given matrix components. */
public RMTransform(double a, double b, double c, double d, double tx, double ty)  { super(a,b,c,d,tx,ty); }

}