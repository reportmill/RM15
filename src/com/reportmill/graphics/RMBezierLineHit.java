/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;
import snap.util.MathUtils;

/**
 * Hit detection for bezier and a line.
 */
public class RMBezierLineHit {

/**
 * Returns the hit info for this bezier and a given line.
 */
public static RMHitInfo getHitInfo(RMBezier aBezier, RMLine aLine, int which)
{
    // If line is completely above/below/right/left of bezier, return false 
    if(aLine.getMinX()>aBezier.getMaxX() || aLine.getMaxX()<aBezier.getMinX() ||
        aLine.getMinY()>aBezier.getMaxY() || aLine.getMaxY()<aBezier.getMinY())
        return null;

    // Get coefficients of bezier
    double C1x[] = getMonomialCoefficientsX(aBezier);
    double C1y[] = getMonomialCoefficientsY(aBezier);
    
    // If horizontal line, reduces to solution of single cubic: at^3+bt^2+ct+x0-other._sp.y=0
    if(MathUtils.equals(aLine._spy, aLine._epy)) {
        C1y[0] -= aLine._spy;
        return intersectOneDimension(C1y, C1x, aLine._spx, aLine._epx, which);
    }
    
    // If vertical line, same as above, but use the x curve and y extent
    else if(MathUtils.equals(aLine._spx, aLine._epx)) {
        C1x[0] -= aLine._spx;
        return intersectOneDimension(C1x, C1y, aLine._spy, aLine._epy, which);
    }
    
    // For all other lines, rotate the line and the curve so the line becomes horizontal and try again.
    // Since the curves are parametric, the s,t values of the intersection of the rotated curves will be the same as
    // the non-rotated ones.
    else {
        
        // Get angle from horizontal
        double angle = Math.atan2(aLine._epy-aLine._spy, aLine._epx-aLine._spx);
        
        // Get transform to horizontal
        Transform rot = Transform.getRotate(-angle);
        
        // Get rotated line
        Point lp1 = rot.transform(aLine.getSP(), new Point());
        Point lp2 = rot.transform(aLine.getEP(), new Point());
        RMLine line2 = new RMLine(lp1, lp2);
        
        // Get rotated bezier
        Point bp1 = rot.transform(aBezier.getSP(), new Point());
        Point bp2 = rot.transform(aBezier.getCP1(), new Point());
        Point bp3 = rot.transform(aBezier.getCP2(), new Point());
        Point bp4 = rot.transform(aBezier.getEP(), new Point());
        RMBezier bez2 = new RMBezier(bp1, bp2, bp3, bp4);
        
        // Check that it did what we expected so we don't go into infinite recursion
        if(!MathUtils.equals(line2._spy, line2._epy)) {
            
            // If line is almost horizontal, close enough for me
            if(Math.abs(line2._spy-line2._epy)<.01)
                line2._epy = line2._spy;

            // Otherwise, complain
            else {
                System.err.println("error finding line-bezier intersection (angle="+(-angle*180/Math.PI)+")");
                return null;
            }
        }
        
        // Try again with rotated line and bezier 
        return getHitInfo(bez2, line2, which);
    }
}

/**
 * Returns an array of 4 doubles, which are the coefficients in the cubics x(t)=ax*t^3+bx*t^2+cx*t+x0.
 */ 
private static double[] getMonomialCoefficientsX(RMBezier aBezier)
{
    double cx = 3*(aBezier._cp1x - aBezier._spx);
    double bx = 3*(aBezier._cp2x - aBezier._cp1x) - cx;
    double ax = aBezier._epx - aBezier._spx-bx-cx;
    double coeffs[] = new double[4];
    coeffs[3] = ax; coeffs[2] = bx;
    coeffs[1] = cx; coeffs[0] = aBezier._spx;
    return coeffs;
}

/**
 * Returns an array of 4 doubles, which are the coefficients in the cubics y(t)=ay*t^3+by*t^2+cy*t+x0.
 */ 
private static double[] getMonomialCoefficientsY(RMBezier aBezier)
{
    double cy = 3*(aBezier._cp1y - aBezier._spy);
    double by = 3*(aBezier._cp2y - aBezier._cp1y) - cy;
    double ay = aBezier._epy - aBezier._spy-by-cy;
    double coeffs[] = new double[4];
    coeffs[3] = ay; coeffs[2] = by;
    coeffs[1] = cy; coeffs[0] = aBezier._spy;
    return coeffs;
}

/**
 * When one dimension (x for vertical lines, or y for horizontal lines) is constant, there's only one cubic to solve.
 */
private static RMHitInfo intersectOneDimension(double cubicCoefs[], double oppositeCoeffs[],
        double lineMin, double lineMax, int which)
{
    double roots[] = new double[3];
    
    int nroots = solve_cubic(cubicCoefs, roots);
    
    double minS = 2;
    double minT = 2;
    
    int totalHits = 0;
       
    for(int i=0; i<nroots; ++i) {
        
        double s = roots[i];
        
        // If root is in valid parametric range for bezier
        if(s>=0 && s<=1) {
            
            // Evaluate the parametric for line
            double s2 = s;
            double x = oppositeCoeffs[0];
            
            for(int j=1; j<=3; ++j) {
                x += oppositeCoeffs[j]*s2;
                s2 *= s;
            }
            
            // find t for line
            double t = (x-lineMin)/(lineMax-lineMin);
            
            // If it's within the range of 0-1, it's valid.  Find the first one (smallest t)
            if(t>=0 && t<=1) {
                
                // Increment total hits
                totalHits++;
                
                // See if min roots need to be updated
                if((which==0 && s<minS) || (which==1 && t<minT)) {
                    minS = s;
                    minT = t;
                }
            }
        }
    }
    
    // If no hits, return null
    if(totalHits==0)
        return null;
       
    // Return hit info for bezier
    if(which==0)
        return new RMHitInfo(totalHits, minS, minT, 0);
    
    // Return hit info for line
    return new RMHitInfo(totalHits, minT, minS, 0);
}

/**
 * private version of RMMath.equalsZero() that uses higher precision.
 */
private static boolean equalsZero(double n)  { return Math.abs(n)<1e-9; }

/**
 * Closed-form cubic polynomial solver. Returns 0-3 for the number of real roots found.
 * Coefficients array should be an array of 4 doubles ordered so the polynomial looks like this:
 * Sum(coefficients[p]*t^p)
 * 
 * From Graphics Gems I, (with many comments removed) Cubic & Quartic Roots (p. 404) by Jochen Schwarze
 */
private static int solve_cubic(double coefficients[], double roots[])
{
    // If 4th coefficient is zero, solve quadratic    
    if(coefficients[3]==0) 
        return solve_quadratic(coefficients,roots);
    
    double A = coefficients[2]/coefficients[3];
    double B = coefficients[1]/coefficients[3];
    double C = coefficients[0]/coefficients[3];
    
    double sq_A = A*A;
    
    double p = (-sq_A/3 + B)/3;
    double q = (2.0/27 * A * sq_A - A*B/3 + C)/2;
    double cb_p = p*p*p;
    
    double D = q*q+cb_p;
    
    int n;
    
    // D==0 case
    if(equalsZero(D)) {
        
        // q==0 case
        if(equalsZero(q)) {
            roots[0] = 0;
            n = 1;
        }
        
        // something
        else {
            double u = cbrt(-q);
            roots[0] = 2*u;
            roots[1] = -u;
            n = 2;
        }
    }
    
    // D less than zero case
    else if (D<0) {
      double phi = Math.acos(-q/Math.sqrt(-cb_p))/3;
      double t = 2*Math.sqrt(-p);
      roots[0] = t*Math.cos(phi);
      roots[1] = -t*Math.cos(phi+Math.PI/3);
      roots[2] = -t*Math.cos(phi-Math.PI/3);
      n = 3;
    }
    
    // D greater than zero case
    else {
        double sqrt_D = Math.sqrt(D);
        double u = cbrt(sqrt_D - q);
        double v = -cbrt(sqrt_D+q);
        roots[0] = u + v;
        n = 1;
    }
    
    // Ah, the old sub
    double sub = A/3;
    
    for(int i=0; i<n; i++) 
        roots[i] -= sub;
    
    // Return the number of roots
    return n;
}

/**
 * Good old-fashioned quadratic equation
 */
private static int solve_quadratic(double coefficients[], double roots[])
{
    double A = coefficients[2];
    if(A==0)
        return solve_linear(coefficients, roots);
    
    double B = coefficients[1];
    double C = coefficients[0];
    double rad = B*B-4*A*C;
    if (rad<0) return 0;
    rad = Math.sqrt(rad);
    roots[0] = (-B+rad)/(2*A);
    roots[1] = (-B-rad)/(2*A);
    return 2;
}

/**
 * Solve linear equation.
 */
private static int solve_linear(double coefficients[], double roots[])
{
    // infinity is allowed, just not 0/0
    if((coefficients[0]!=0) && (coefficients[1]!=0)) {
        roots[0] = -coefficients[0]/coefficients[1];
        return 1;
    }
    
    return 0;
}

/**
 * Returns cube root of given value (this is available in Java 5 as Math.cbrt()).
 */
private static double cbrt(double aValue)
{
    if(aValue<0)
        return -Math.pow(-aValue, 1/3.0);
    return Math.pow(aValue, 1/3.0);
}

}