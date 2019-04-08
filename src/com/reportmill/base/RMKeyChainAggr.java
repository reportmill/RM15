/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.base.RMKeyChain.Op;
import java.lang.reflect.Method;
import java.util.*;
import snap.util.*;

/**
 * A class to evaluate keychains on lists slightly differently than normal objects.
 * Special behavior is this:
 *  - If keychain key is aggregate function, pass remainder to function to use on list items (instead of list).
 *  - If keychain function is aggregate function, pass args to function to use on list items (instead of list).
 *  - If List doesn't successfully evaluate keychain, try on first list item instead.
 */
public class RMKeyChainAggr {

/**
 * Evaluates a given keychain on a given List.
 */
public static Object getValue(Object aRoot, List aList, RMKeyChain aKeyChain)
{
    // Get local keyChain with separate tail if Chain
    RMKeyChain kchain = aKeyChain, tail = null;
    if(aKeyChain.getOp()==RMKeyChain.Op.Chain && aKeyChain.getChildCount()>0) {
        kchain = aKeyChain.getChildKeyChain(0); tail = aKeyChain.subchain(1); }
    
    // Handle Key: if aggregate method, return result of aggregate with tail
    if(kchain.getOp()==RMKeyChain.Op.Key) {
        
        // Get key and try to invoke built-in aggr
        String key = kchain.getChildString(0);
        Object value = invokeBuiltIn(key, aList, tail);
        if(value!="NOT_FOUND")
            return value;

        // Look for aggr method in registered classes
        Method method = getAggrMethod(key);
        if(method!=null) {
            try { return method.invoke(null, aList, tail); }
            catch(Exception e) { System.err.println("RMKeyChainAggr: Failed to eval " + aKeyChain); }
        }
    }
    
    // Handle FunctionCall: if aggregate method, return result of aggregate with args (eval tail too, if present) 
    else if(kchain.getOp()==RMKeyChain.Op.FunctionCall) {
        
        // Get key and FunctionCall args (simplify for common case of arg count zero or 1) try to invoke built-in aggr
        String key = kchain.getChildString(0);
        RMKeyChain args = kchain.getChildKeyChain(1);
        if(args.getChildCount()==0) args = null;
        else if(args.getChildCount()==1) args = args.getChildKeyChain(0);
        
        // Try to ivoke built-in aggr
        Object val = invokeBuiltIn(key, aList, args);
        if(val!="NOT_FOUND")
            return tail!=null? RMKeyChain.getValue(aRoot, val, tail) : val;
        
        // Look for aggr method in registered classes
        Method method = getAggrMethod(key);
        if(method!=null) {
            try { val = method.invoke(null, aList, args); }
            catch(Exception e) { System.err.println("RMKeyChainAggr: Failed to eval " + kchain); val = null; }
            return tail!=null? RMKeyChain.getValue(aRoot, val, tail) : val;
        }
    }
    
    // Otherwise try normal implementation
    Object value = RMKeyChain.getValueImpl(aRoot, aList, aKeyChain);
    
    // If that doesn't work, try to evaluate on first list item
    if(value==null && aList.size()>0)
        value = RMKeyChain.getValue(aRoot, aList.get(0), aKeyChain);
    
    // Return value
    return value;
}

/**
 * Tries to invoke built-in function.
 */
private static Object invokeBuiltIn(String aKey, List aList, RMKeyChain aKeyChain)
{
    // Check built-in aggrs. If found, call.
    switch(aKey) {
        case "total": return total(aList, aKeyChain);
        case "total2": return total2(aList, aKeyChain);
        case "totalX": return totalX(aList, aKeyChain);
        case "count": return count(aList, aKeyChain);
        case "countDeep": return countDeep(aList, aKeyChain);
        case "countUnique": return countUnique(aList, aKeyChain);
        case "average": return average(aList, aKeyChain);
        case "averageX": return averageX(aList, aKeyChain);
        case "min": return min(aList, aKeyChain);
        case "max": return max(aList, aKeyChain);
        case "get": return get(aList, aKeyChain);
        case "filter": return filter(aList, aKeyChain);
        case "group": return group(aList, aKeyChain);
        case "join": return join(aList, aKeyChain);
        case "listOf": return listOf(aList, aKeyChain);
        default: return "NOT_FOUND";
    }
}

/**
 * Returns a method for a method name (assuming {List,KeyChain} args).
 */
private synchronized static Method getAggrMethod(String aName)
{
    Method m = _methods.get(aName);
    if(m==null) _methods.put(aName, m=getAggrMethodImpl(aName));
    return m!=_emptyMeth? m : null;
}

/**
 * Returns a method for a method name (assuming {List,KeyChain} args).
 */
private static Method getAggrMethodImpl(String aName)
{
    // Lookup method on registered classes
    for(Class cls : RMKeyChainFuncs._funcClasses) { Method meth = ClassUtils.getMethod(cls, aName, _argClasses);
        if(meth!=null) return meth; }
        
    // Return null method placeholder, since method not found
    return _emptyMeth;
}

// Support for getAggrMethod
static Map <String,Method> _methods = new HashMap();
static Class _argClasses[] = { List.class, RMKeyChain.class };
static Method _emptyMeth = ClassUtils.getMethod(Object.class, "toString");

/**
 * Returns the total resulting from evaluating given keychain on given list of objects (nulls are zero).
 */
public static double total(List aList, RMKeyChain aKeyChain)
{
    double total = 0;
    
    // If should recurse, sum result of calling totalX on objects in list
    if(shouldRecurse(aList, aKeyChain)) {
        for(int i=0, iMax=aList.size(); i<iMax; i++) {
            Double value = total((List)aList.get(i), aKeyChain);
            total += value;
        }
    }
    
    // If shouldn't recurse, sum result of evaluating keychain on objects in list
    else for(int i=0, iMax=aList.size(); i<iMax; i++)
        total += RMKeyChain.getDoubleValue(aList.get(i), aKeyChain);

    return total;
}

/**
 * Returns the total resulting from evaluating given keychain on given list of objects (nulls are zero).
 */
public static double total2(List aList, RMKeyChain aKeyChain)  { return total(aList, aKeyChain); }

/**
 * Returns the total resulting from evaluating given keychain on given list of objects (nulls short circuit).
 */
public static Double totalX(List aList, RMKeyChain aKeyChain)
{
    double total = 0;

    // If should recurse, sum result of calling totalX on objects in list
    if(shouldRecurse(aList, aKeyChain)) {
        for(int i=0, iMax=aList.size(); i<iMax; i++) {
            Double value = totalX((List)aList.get(i), aKeyChain);
            if(value==null)
                return null;
            total += value;
        }
    }
    
    // If shouldn't recurse, sum result of evaluating keychain on objects in list
    else for(int i=0, iMax=aList.size(); i<iMax; i++) {
        Number value = (Number)RMKeyChain.getValue(aList.get(i), aKeyChain);
        if(value==null)
            return null;
        total += value.doubleValue();
    }

    return total;
}

/**
 * Returns the count of the given list or if keychain is present, all true values.
 */
public static int count(List aList, RMKeyChain aKeyChain)
{
    if(aKeyChain==null) return aList.size();
    
    // If list is an upper level group that requires recursion, tally the counts of all list objects
    int count = 0;
    if(shouldRecurse(aList, aKeyChain))
        for(int i=0, iMax=aList.size(); i<iMax; i++)
            count += count((List)aList.get(i), aKeyChain);
    
    // If list is simple list, increment count for every true value
    else for(int i=0, iMax=aList.size(); i<iMax; i++)
        if(SnapUtils.boolValue(RMKeyChain.getValue(aList.get(i), aKeyChain)))
            count++;
    
    return count;
}

/**
 * Returns the count of all leaf nodes in given list.
 */
public static int countDeep(List aList, RMKeyChain aKeyChain)
{
    if(shouldRecurse(aList, aKeyChain)) {
        int total = 0;
        for(int i=0, iMax=aList.size(); i<iMax; i++) total += countDeep((List)(aList.get(i)), aKeyChain);
        return total;
    }

    return count(aList, aKeyChain);
}

/**
 * Returns the count of the unique values for a given list and key chain.
 */
public static int countUnique(List aList, RMKeyChain aKeyChain)
{
    Set set = new HashSet();
    countUnique(aList, aKeyChain, set);
    return set.size();
}

/**
 * This is the worker method for countUnique.
 */
private static void countUnique(List aList, RMKeyChain aKeyChain, Set aSet)
{
    // If list is an upper level group that requires recursion, tally the counts of all list objects
    if(shouldRecurse(aList, aKeyChain))
        for(int i=0, iMax=aList.size(); i<iMax; i++)
            countUnique((List)aList.get(i), aKeyChain, aSet);
    
    // If list is simple list, increment count for every true value
    else for(int i=0, iMax=aList.size(); i<iMax; i++)
        aSet.add(RMKeyChain.getValue(aList.get(i), aKeyChain));
}

/**
 * Returns the average resulting by evaluating the keychain on given list objects.
 */
public static double average(List aList, RMKeyChain aKeyChain) 
{
    double total = total(aList, aKeyChain);
    int count = countDeep(aList, null);
    return count>0? (total/count) : 0;
}

/**
 * Returns the average resulting by evaluating the keychain on given list objects.
 */
public static Double averageX(List aList, RMKeyChain aKeyChain) 
{
    Double total = totalX(aList, aKeyChain);
    int count = countDeep(aList, null);
    return total!=null && count>0? (total/count) : null;
}

/**
 * Returns the minimum result of evaluating the keychain on given list objects.
 */
public static Object min(List aList, RMKeyChain aKeyChain) 
{
    Object minValue = null;
    
    // If there are child Lists, recurse
    if(shouldRecurse(aList, aKeyChain)) {
        for(int i=0, iMax=aList.size(); i<iMax; i++) { List list = (List)aList.get(i);
            Object value = min(list, aKeyChain);
            if(i==0 || minValue==null) minValue = value;
            else if(value!=null && RMSort.Compare(minValue, value)==RMSort.ORDER_DESCEND) minValue = value;
        }
    }

    // If no child Lists, just do simple min
    else for(int i=0, iMax=aList.size(); i<iMax; i++) { Object item = aList.get(i);
        Object value = RMKeyChain.getValue(item, aKeyChain);
        if(i==0 || minValue==null)
            minValue = value;
        else if(value!=null && RMSort.Compare(minValue, value)==RMSort.ORDER_DESCEND)
            minValue = value;
    }

    return minValue;
}

/**
 * Returns the maximum result of evaluating the keychain on given list objects.
 */
public static Object max(List aList, RMKeyChain aKeyChain)
{
    Object maxValue = null;
    
    // If there are child Lists, recurse
    if(shouldRecurse(aList, aKeyChain)) {
        for(int i=0, iMax=aList.size(); i<iMax; i++) { List list = (List)(aList.get(i));
            Object value = max(list, aKeyChain);
            if(i==0) maxValue = value;
            else if(RMSort.Compare(maxValue, value)==RMSort.ORDER_ASCEND) maxValue = value;
        }
    }

    // If no child Lists, just do simple max
    else for(int i=0, iMax=aList.size(); i<iMax; i++) { Object item = aList.get(i);
        Object value = RMKeyChain.getValue(item, aKeyChain);
        if(i==0) maxValue = value;
        else if(RMSort.Compare(maxValue, value)==RMSort.ORDER_ASCEND) maxValue = value;
    }

    return maxValue;
}

/**
 * Returns the specific object that meets the criteria.
 */
public static Object get(List aList, RMKeyChain aKeyChain)
{
    // Iterate over list objects and return the first that evaluates to true for key chain
    for(int i=0, iMax=aList.size(); i<iMax; i++)
        if(RMKeyChain.getBoolValue(aList.get(i), aKeyChain))
            return aList.get(i);
    
    // Return null if no objects return true for key  chain
    return null;
}

/**
 * Returns the original list with items that fail the given boolean key chain removed.
 */
public static List filter(List aList, RMKeyChain aKeyChain)
{
    // Create new list with same capacity as original
    List list = new ArrayList(aList.size());
    
    // Iterate over list objects and add to new list if they satisy key chain
    for(Object object : aList)
        if(RMKeyChain.getBoolValue(object, aKeyChain))
            list.add(object);
    
    // Return list
    return list;
}

/**
 * Returns the original list grouped by the given key chain.
 */
public static List group(List aList, RMKeyChain aKeyChain)
{
    // Get grouper
    RMGrouper grouper = new RMGrouper();
    
    // If keychain is Key, just add grouping for key
    if(aKeyChain.getOp()==RMKeyChain.Op.Key)
        grouper.addGroupingForKey(aKeyChain.getValueString());
    
    // If keychain is argList, add groupings for args
    else if(aKeyChain.getOp()==RMKeyChain.Op.ArgList)
        for(int i=0; i<aKeyChain.getChildCount(); i++)
            grouper.addGroupingForKey(aKeyChain.getChild(i).toString());
    
    // Add root level grouping, group objects and return group
    grouper.addGroupingForKey("RootLevel");
    RMGroup group = grouper.groupObjects(aList);
    return group;
}

/**
 * Aggregator version of join. aKeyChain is an arglist with 2 args, as in join(getName, "\n")
 * The first arg is the keychain to be evaluated for each object in list, and the second arg is the separator string.
 */
public static String join(List aList, RMKeyChain aKeyChain)
{
    // If keychain operator isn't arglist of count 2, return error message
    if(aKeyChain.getOp()!=RMKeyChain.Op.ArgList || aKeyChain.getChildCount()!=2)
        return "<Illegal arguments>";
        
    // Get parts key chain and delimiter (actually, probably would be better to do getKeyChainValue...)
    RMKeyChain keyChain = aKeyChain.getChildKeyChain(0);
    String delimeter = aKeyChain.getChildString(1);
        
    // Get parts
    List parts = new ArrayList(aList.size());
    for(int i=0, iMax=aList.size(); i<iMax; i++)
        parts.add(RMKeyChain.getValue(aList.get(i), keyChain));
        
    // Return string from parts and delimiter
    return ListUtils.joinStrings(parts, delimeter);
}

/**
 * Returns a list of objects by evaluating keychain on given list.
 */
public static List listOf(List aList, RMKeyChain aKeyChain)
{
    List parts = new ArrayList(aList.size());
    for(Object object : aList) parts.add(RMKeyChain.getValue(object, aKeyChain));
    return parts;
}

/**
 * Returns whether given list should be recursed into for aggregate calculations.
 */
static public boolean shouldRecurse(List aList, RMKeyChain aKeyChain)
{
    // Don't recurse if there are no objects
    if(aList.size()==0) return false;
    
    // Don't recurse if first element of aList is not a list (or if it's a Leaf RMGroup)
    Object first = aList.get(0);
    if(!(first instanceof List) || RMGroup.isLeaf(first))
        return false;
  
    // If aKeyChain is null, recurse (can't hurt, although probably only useful for countDeep)
    if(aKeyChain==null) return true;
    
    // If aKeyChain head has aggregate - don't recurse
    if(hasAggregate(aKeyChain))
        return false;
    
    // If keychain expressly starts with grouping key, don't recurse (maybe should also check for "this"?)
    if(aKeyChain.getOp()==RMKeyChain.Op.Key && aKeyChain.getValue().equals(RMGroup.getKey(aList)))
        return false;
    
    // If none of the above, return true
    return true;
}

/**
 * Returns whether key has an aggregate key in it.
 */
private static boolean hasAggregate(RMKeyChain aKC)
{
    // Check for aggregate method
    if(aKC.getOp()==Op.Key || aKC.getOp()==Op.FunctionCall) {
        String key = aKC.getChildString(0);
        Method method = RMKeyChainAggr.getAggrMethod(key);
        if(method != null)
            return true;
    }

    // Check children for aggregate
    for(int i=0, iMax=aKC.getChildCount(); i<iMax; i++)
        if(aKC.getChild(i) instanceof RMKeyChain && hasAggregate(aKC.getChildKeyChain(i)))
            return true;
    
    // Return false since above checks failed
    return false;
}

/**
 * Silly method to support DatasetKeys like: List1Key.List2Key, which returns a flattened Master/Detail list.
 */
public static Object getListValue(Object anObj, RMKeyChain aKeyChain)
{
    // Handle Op.Chain
    if(aKeyChain.getOp()==Op.Chain) {
        Object value = anObj; String key = null;
        for(int i=0, iMax=aKeyChain.getChildCount(); i<iMax; i++) { RMKeyChain child = aKeyChain.getChildKeyChain(i);
            if(value instanceof List) value = getMasterDetailList(anObj, (List)value, child, key);
            else value = RMKeyChain.getValue(anObj, value, child);
            key = child.getValueString();
        }
        return value;
    }
    
    // Otherwise, do normal version
    return RMKeyChain.getValue(anObj, anObj, aKeyChain);
}

/**
 * Intercept getKeyChainValue so we can coalate nested lists.
 */
private static List getMasterDetailList(Object aRoot, List aList, RMKeyChain aKeyChain, String aKey)
{
    List list2 = new ArrayList(aList.size());
    for(Object item : aList) {
       Object ival = RMKeyChain.getValue(aRoot, item, aKeyChain);
       if(ival instanceof List) { List list3 = (List)ival;
           for(Object it : list3) list2.add(new MasterDetail(item, it, aKey)); }
       else list2.add(new MasterDetail(item, ival, aKey));
    }
    return list2;
}

/**
 * An inner class to represent a flattened master detail object.
 */
private static class MasterDetail implements RMKey.Get {
    
    // Master/detail objects and the key that identifies the master
    Object _master, _detail; String _mkey;
    
    /** Creates a flattened master-detail object from a master, a detail and the relationship key. */
    public MasterDetail(Object aMaster, Object aDetail, String aKey) { _master=aMaster; _detail=aDetail; _mkey=aKey; }
    
    /** RMKey.Get implementation to return Master if given Master Key. */
    public Object getKeyValue(String aKey)
    {
        Object value = RMKey.getValue(_detail, aKey); if(value!=null) return value;
        if(aKey.equals(_mkey) || aKey.equals(_master.getClass().getSimpleName())) return _master;
        return null;
    }
}

}