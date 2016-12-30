/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.*;

/**
 * Returns a list of graph intervals given a range and count.
 * If count is zero, this class will determine a proper count.
 */
public class RMGraphIntervals {

    // The list of interval numbers
    List <Float>     _intervals;

/**
 * Returns graph intervals for a given graph.
 */
public static RMGraphIntervals getGraphIntervals(RMGraphRPG aGraphRPG)
{
    // Get graph
    RMGraph graph = aGraphRPG.getGraph();
    
    // Declare variable for min value
    float minVal = graph.getValueAxis().getAxisMin();
    
    // If minVal not defined, iterate over sections and get min value
    if(minVal==Float.MIN_VALUE)
        for(int i=0, iMax=aGraphRPG.getSectionCount(); i<iMax; i++) { RMGraphSection section = aGraphRPG.getSection(i);
            minVal = i==0? section.getMinValue() : Math.min(section.getMinValue(), minVal); }
    
    // Declare variable for max value
    float maxVal = graph.getValueAxis().getAxisMax();

    // If maxVal not defined, iterate over sections and get max value
    if(maxVal==Float.MIN_VALUE)
        for(int i=0, iMax=aGraphRPG.getSectionCount(); i<iMax; i++) { RMGraphSection section = aGraphRPG.getSection(i);
            maxVal = i==0? section.getMaxValue() : Math.max(section.getMaxValue(), maxVal); }
    
    // Get _intervals and _maxValue for data values
    return new RMGraphIntervals(minVal, maxVal, graph.getValueAxis().getAxisCount());
}

/**
 * Return well-chosen intervals given a min/max value. For instance, (1,4) would return
 *    (1,2,3,4,5), while (17,242) would return (50,100,150,200,250). Useful methods for graphing.
 */
public RMGraphIntervals(float minValue, float maxValue, int aCount)
{
    // Get intervals for range
    _intervals = getIntervalsFor(minValue, maxValue);
    
    // If there is explicit count, redefine intervals
    if(aCount>0) {
        float minInterval = getInterval(0);
        float maxInterval = getIntervalLast();
        float totalInterval = maxInterval - minInterval;
        _intervals.clear();
        for(int i=0; i<=aCount; i++)
            _intervals.add(minInterval + i*totalInterval/aCount);
    }
}

/**
 * Returns the number of intervals for this filled graph.
 */
public int getIntervalCount()  { return _intervals.size(); }

/**
 * Returns the individual interval at a given index as a float value.
 */
public Float getInterval(int anIndex)  { return _intervals.get(anIndex); }

/**
 * Returns the last interval as a float value.
 */
public Float getIntervalLast()  { return getInterval(getIntervalCount()-1); }

/**
 * Return well-chosen intervals given a min/max value. For instance, (1,4) would return
 *    (1,2,3,4,5), while (17,242) would return (50,100,150,200,250). Useful methods for graphing.
 */
private List <Float> getIntervalsFor(float aMinValue, float aMaxValue)
{
    // If we were sent NANs, just return a bogus interval
    if(Float.isNaN(aMinValue) || Float.isNaN(aMaxValue) || Float.isInfinite(aMinValue) || Float.isInfinite(aMaxValue))
        return getIntervalsFor(5, 0);
        
    // If both max & min values greater than zero, just return intervalsFor maxValue
    if(aMaxValue>=0 && aMinValue>=0)
        return getIntervalsFor(aMaxValue);
        
    // If maxVal is positive and greater in magnitude than minVal, find intervals for maxVal
    // such that unused intervals are sufficient for minVal
    if(aMaxValue>=0 && aMaxValue>=Math.abs(aMinValue)) {
        
        // Keep going till loop short circuits
        while(true) {
            
            // Get intervals for max value
            List <Float> intervals = getIntervalsFor(aMaxValue);
            float interval = intervals.get(1) - intervals.get(0);
            
            // If the lesser value can fit in unused intervals, do a shift and return
            if(Math.abs(aMinValue) < (6 - intervals.size())*interval) {
                
                // Get first interval
                float firstInterval = intervals.get(0);

                // 
                while(aMinValue < firstInterval) {
                    firstInterval -= interval;
                    intervals.add(0, firstInterval);
                }
                
                // Return intervals
                return intervals;
            }
            
            // Bump max value and redo
            aMaxValue = 5*interval + .1f*interval;
        }
    }
    
    // If min/max aren't predominantly positive, get intervals for flipped & negated min/max...
    List <Float> intervals = getIntervalsFor(-aMaxValue, -aMinValue);
    
    // ...then flip & negate them
    ListUtils.reverse(intervals);
    for(int i=0, iMax=intervals.size(); i<iMax; i++)
        intervals.set(i, -intervals.get(i));
    
    // Return intervals
    return intervals;
}

/**
 * Returns well-chosen intervals from zero to a given a value. Finds the first multiple of {5,10 or 25}*10^n that
 * equals or exceeds max value, then divides by 5. This method could probably be done a lot simpler.
 */
private List <Float> getIntervalsFor(float maxValue)
{
    // Declare and initialize arrays of "pretty" series prototypes
    int series[][] = { { 1, 2, 3, 4, 5 }, { 2, 4, 6, 8, 10 }, { 5, 10, 15, 20, 25 } };
    float rootValue = 1;
    int i;

    while(true) {

        // If maxValue is less than 2.5, we cycle down through series*[.1, .01, .001, .0001, ...]
        if(maxValue < 2.5) {
            rootValue *= .1;
            for(i=2; i>=0; i--) {
                if(maxValue > series[i][4]*rootValue)
                    break;
            }

            // If loop exited prematurely, then acceptable series is previous series
            if(i>=0) {
                i++;
                if(i==3) {
                    i = 0; rootValue *= 10; }
                break;
            }
        }

        // If maxValue is greater than 2.5, we cycle up through each series*[1,10,100,100,...]
        else {
            for(i=0; i<3; i++) {
                if(maxValue <= series[i][4]*rootValue)
                    break;
            }
            if(i<3)
                break;
            rootValue *= 10;
        }

        // If root Value is zero we break. I don't understand this yet
        if(MathUtils.equalsZero(rootValue)) {
            rootValue = 1000; i = 0; break; }
    }

    // Create intervals array and add first 4 values
    List array = new ArrayList(6);
    array.add(0f);
    array.add(series[i][0]*rootValue);
    array.add(series[i][1]*rootValue);
    array.add(series[i][2]*rootValue);
    
    // If 5th value is needed, add it
    if(maxValue > series[i][2]*rootValue)
      array.add(series[i][3]*rootValue);
    
    // If 6th value is needed, add it
    if(maxValue > series[i][3]*rootValue)
      array.add(series[i][4]*rootValue);

    // Return intervals
    return array;
}

}