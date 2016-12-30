/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;

/**
 *  This class represents a 3D point.
 */
public class RMPoint3D implements Cloneable {
    
    // X Y Z components
    public double x, y, z;
    
/** Creates a new point3d from the x, y, z coords. */
public RMPoint3D(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    
/** Transforms the point by the given transform3d. */
public RMPoint3D transform(RMTransform3D xform) { return xform.transform(this); }

/** Standard equals implementation. */
public boolean equals(Object anObj)
{
    RMPoint3D p = (RMPoint3D)anObj;
    return p==this || (p.x==x && p.y==y && p.z==z);
}

/** implements Cloneable interface */
public Object clone()  { return new RMPoint3D(x,y,z); }

/** Returns a string representation of the point. */
public String toString()  { return "Point [" + x + " " + y + " " + z + "]"; }

}