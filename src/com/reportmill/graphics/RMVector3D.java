/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;

/**
 * This class represents a 3D vector.
 */
public class RMVector3D {
    
    // X Y Z components
    double x, y, z;
    
/**
 * Creates a new vector from the given coords.
 */
public RMVector3D(double x, double y, double z)  { this.x = x; this.y = y; this.z = z; }

/**
 * Creates a new vector from the given vector.
 */
public RMVector3D(RMVector3D aVector)  { x = aVector.x; y = aVector.y; z = aVector.z; }
    
/**
 * Returns the magnitude of the vector.
 */
public double getMagnitude()  { return Math.sqrt(x*x + y*y + z*z); }
    
/**
 * Makes the vector unit length.
 */
public RMVector3D normalize()  { double t = getMagnitude(); x /= t; y /= t; z /= t; return this; }

/**
 * Add the given vector to this.
 */
public void add(RMVector3D aVector)  { x += aVector.x; y += aVector.y; z += aVector.z; }
    
/**
 * Returns the vector perpendicular to the receiver and the given vector.
 */
public RMVector3D getCrossProduct(RMVector3D v2)
{
    // Get cross components
    double a = y*v2.z - v2.y*z;
    double b = z*v2.x - v2.z*x;
    double c = x*v2.y - v2.x*y;
    
    // Return new vecotr with components (normalized)
    return new RMVector3D(a, b, c).normalize();
}
    
/**
 * Returns the dot product of the receiver and the given vector.
 */
public double getDotProduct(RMVector3D v2)  { return x*v2.x + y*v2.y + z*v2.z; }

/**
 * Returns whether given vector is in same general direction of this (with option to include perpendiculars).
 */
public boolean isAligned(RMVector3D aVector, boolean includePerpendiculars)
{
    return !isAway(aVector, !includePerpendiculars);
}

/**
 * Returns whether given vector is pointing away from the direction of this (with option to include perpendiculars).
 */
public boolean isAway(RMVector3D aVector, boolean includePerpendiculars)
{
    // Get normalized version of this vector
    RMVector3D v1 = getMagnitude()==1? this : new RMVector3D(this).normalize();
    
    // Get normalized version of given vector
    RMVector3D v2 = aVector.getMagnitude()==1? aVector : new RMVector3D(aVector).normalize();
    
    // Dot of normalized vectors GT 0: angle<90deg, EQ 0: angle==90deg, LT 0: angle>90deg
    double dot = v1.getDotProduct(v2);
    
    // Return whether angle is less than zero (or equal zero for perpendicular)
    return dot<0 || (dot==0 && includePerpendiculars);
}
    
/**
 * Returns the angle between the receiver and the given vector.
 */
public double getAngleBetween(RMVector3D aVector)
{
    double m1 = getMagnitude();
    double m2 = aVector.getMagnitude();
    double m3 = m1*m2;
    return Math.toDegrees(Math.acos(getDotProduct(aVector)/m3));
}
    
/**
 * Makes this receiver point in the opposite direction.
 */
public void negate()  { x = -x; y = -y; z = -z; }
    
/**
 * Transforms the vector by the given transform3d.
 */
public RMVector3D transform(RMTransform3D aTransform)  { return aTransform.transform(this); }

/**
 * Returns a string representation of the vector.
 */
public String toString()  { return "Vector [" + x + " " + y + " " + z + "]"; }

}