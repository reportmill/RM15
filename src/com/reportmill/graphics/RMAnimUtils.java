/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import snap.util.*;

/**
 * This class holds a number of convenient static methods used for animation stuff.
 */
public class RMAnimUtils {

/**
 * Returns whether a given list contains a given float time.
 */
public static boolean containsTime(List <? extends Number> aList, float aTime)
{
    return indexOfTime(aList, aTime)>=0;
}

/**
 * Returns the index of a given time in the given list.
 */
public static int indexOfTime(List <? extends Number> aList, float aTime)
{
    // Iterate over times until we find one less than ourselves (return nil if that doesn't happen)
    for(int i=0, iMax=ListUtils.size(aList); i<iMax; i++) {
        float time = aList.get(i).floatValue();
        if(MathUtils.equals(time, aTime))
            return i;
        if(time > aTime)
            return -1;
    }

    // Return -1 since time not found
    return -1;
}

/**
 * Returns the index of the time in the given list that is closest without going past the given time.
 */
public static int indexOfTimeBeforeTime(List <? extends Number> aList, float aTime)
{
    // Iterate over times until we find one less than given time
    for(int i=aList.size()-1; i>=0; i--)
        if(aList.get(i).floatValue() < aTime)
            return i;

    // Return -1 if no time was found prior to given time
    return -1;
}

/**
 * Returns the index of the time in the given list that is closest but just past the given time.
 */
public static int indexOfTimeAfterTime(List <? extends Number> aList, float aTime)
{
    // Iterate over times until we find one greater than given time
    for(int i=0, iMax=aList.size(); i<iMax; i++)
        if(aList.get(i).floatValue() > aTime)
            return i;

    // Return -1 if no time was found greater than given time
    return -1;
}

/**
 * Returns the time in the given list that is just before the given time (or null if no such time).
 */
public static Number timeBeforeTime(List <? extends Number> aList, float aTime)
{
    int index = indexOfTimeBeforeTime(aList, aTime);
    return index<0? null : aList.get(index);
}

/**
 * Returns the time in the given list that is just after the given time (or null if no such time).
 */
public static Number timeAfterTime(List <? extends Number> aList, float aTime)
{
    int index = indexOfTimeAfterTime(aList, aTime);
    return index<0? null : aList.get(index);
}

/**
 * Adds a given time to the given list at the proper ordered index.
 */
public static void addTime(List <Number> aList, Number aTime)
{
    float timeValue = aTime.floatValue();
    int index = 0;

    // Iterate through self
    while(index < aList.size()) {
        float value = aList.get(index).floatValue();

        if(MathUtils.equals(timeValue, value))
            break;

        else if(value > timeValue) {
            aList.add(index, aTime);
            break;
        }

        index++;
    }

    // If index is equal to _timesWithRecords count, then we didn't encounter a timeWR <= aTime, so add it
    if(index == aList.size())
        aList.add(aTime);
}

/**
 * Adds the list of times in list 2 to the given list of times in list 1 at the proper ordered indexes.
 */
public static void addTimes(List <Number> aList, List <Number> theTimes)
{
    if(theTimes!=null)
        for(int i=0, iMax=theTimes.size(); i<iMax; i++)
            addTime(aList, theTimes.get(i));
}

/**
 * Removes the times between the two given times (not including the two times).
 */
public static void removeTimeBetweenTimes(List <? extends Number> aList, float startTime, float endTime)
{
    int startIndex = indexOfTimeAfterTime(aList, startTime);

    if(startIndex >= 0) {
        int endIndex = indexOfTimeBeforeTime(aList, endTime);

        if(endIndex >= 0)
            for(int i=startIndex; i<=endIndex; i++)
                aList.remove(startIndex);
    }
}

}