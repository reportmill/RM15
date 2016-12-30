/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.MathUtils;

/**
 * Defines a region of coordinates along with a list of shapes that occupy that region.
 */
public class RMShapeTable {

/**
 * A class to represent an interval 
 */
public static class Span implements Comparable {

    /** Creates a new span. */
    public Span(double aStart, double anEnd)  { this.start = aStart; this.end = anEnd; }  double start, end;
    
    /** Returns the span length. */
    public double getLength()  { return end - start; }
    
    /** Returns whether given value is contained in the span (inclusive). */
    public boolean contains(double aValue)  { return MathUtils.lte(start, aValue) && MathUtils.lte(aValue, end); }
    
    /** Returns whether given span intersects this span. */
    public boolean intersects(Span aSpan)
    {
        return MathUtils.equals(this.start, aSpan.start) || MathUtils.equals(this.end, aSpan.end) ||
               MathUtils.lt(aSpan.start, end) && MathUtils.gt(aSpan.end, start);
    }
    
    /** Returns string representation of span. */
    public String toString()  { return "Span { start: " + start + ", end: " + end + " }"; }

    /** Comparable implementation. */
    public int compareTo(Object aSpan)  { return new Double(start).compareTo(((Span)aSpan).start); }
}

/**
 * A class to represent a list of spans.
 */
public static class SpanList extends ArrayList <Span> {

    /** Adds a span to a list of spans, either by extending an existing span or actually adding it to the list. */
    public void addSpan(Span aSpan)
    {
        // If empty span, just return
        if(MathUtils.lte(aSpan.end, aSpan.start)) return;
        
        // Iterate over spans and extends any overlapping span (and return)
        for(Span span : this) {
            
            // If given span starts inside loop span and ends after, extend current span, remove from list and re-add
            if(span.contains(aSpan.start) && !span.contains(aSpan.end)) {
                span.end = aSpan.end; this.remove(span); addSpan(span); return; }
            
            // If given span starts before loop span and ends inside, extend current span, remove from list and re-add
            if(!span.contains(aSpan.start) && span.contains(aSpan.end)) {
                span.start = aSpan.start; this.remove(span); addSpan(span); return; }
            
            // If loop span contains given span, just return
            if(span.contains(aSpan.start) && span.contains(aSpan.end))
                return;
        }
        
        // Since no overlapping span, add span
        add(aSpan);
    }
    
    /** Removes a span from a list of spans, either by reducing a span or by removing a span. */
    public void removeSpan(Span aSpan)
    {
        // Iterate over spans and reduce any that need to be reduced
        for(Span span : this) {
            
            // If given span starts in loop span and ends outside, reduce loop span to given span start
            if(span.contains(aSpan.start) && !span.contains(aSpan.end))
                span.end = aSpan.start;
            
            // If given span starts outside loop span and ends in span, reset loop span start to given span end
            if(!span.contains(aSpan.start) && span.contains(aSpan.end))
                span.start = aSpan.end;
            
            // If loop span contains given span, remove given span and add two spans
            if(span.contains(aSpan.start) && span.contains(aSpan.end)) {
                this.remove(span); addSpan(new Span(span.start, aSpan.start));
                addSpan(new Span(aSpan.end, span.end)); return; }
            
            // If given span contains loop span, remove it and re-run
            if(aSpan.contains(span.start) && aSpan.contains(span.end)) {
                this.remove(span); removeSpan(aSpan); return; }
        }
    }
}

}