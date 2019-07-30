/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.io.*;
import java.util.*;
import snap.util.*;

/**
 * This class evaluates a string expression on a given object: RMKeyChain.getValue(object, expression).
 */
public class RMKeyChain {
    
    // The operator of key chain
    Op            _op;
    
    // Possible children of key chain
    Object        _children;
    
    // Whether key has a page reference
    Boolean       _hasPageReference;
    
    // A shared map of previously encountered key chains
    static Map <Object,RMKeyChain>  _keyChains = new Hashtable();
    
    // A thread local to vend per assignment maps
    static ThreadLocal <Map> _assTL = new ThreadLocal() { public Object initialValue() { return new HashMap(); } };

    // KeyChain Operators
    public enum Op {
        Literal, Add, Subtract, Multiply, Divide, Mod, Negate,
        Equal, NotEqual, GreaterThan, LessThan, GreaterThanOrEqual, LessThanOrEqual, And, Or, Not,
        Key, ArrayIndex, FunctionCall, ArgList, Chain, Conditional, Assignment
    };
    
/**
 * This is interface is implemented by objects that can get key chain values themselves.
 */
public interface Get {
    public Object getKeyChainValue(Object aRoot, RMKeyChain aKeyChain);
}

/**
 * Creates a new RMKeyChain.
 */
protected RMKeyChain()  { }

/**
 * Creates a new RMKeyChain with op.
 */
protected RMKeyChain(Op anOp)  { _op = anOp; }

/**
 * Returns a keyChain for aSource (should be a String or existing RMKeyChain).
 */
public static RMKeyChain getKeyChain(Object aSource)
{
    // If passed key chain, just return it
    if(aSource instanceof RMKeyChain)
	    return (RMKeyChain)aSource;
        
    // If not passed string, return Null KeyChain
    if(!(aSource instanceof String) || ((String)aSource).length()==0)
        return new RMKeyChain(Op.Literal);
        
    // Get KeyChain (create and cache if needed) and return
    RMKeyChain kchain = _keyChains.get(aSource);
    if(kchain==null)
        _keyChains.put(aSource, kchain = createKeyChain((String)aSource));
    return kchain;
}

/**
 * Returns a keyChain for aSource (should be a String or existing RMKeyChain).
 */
private static synchronized RMKeyChain createKeyChain(String aString)
{
    KeyChain kc = KeyChain.getKeyChain(aString);
    return getRM(kc);
}

/**
 * Returns an RMKeyChain for given KeyChain.
 */
private static RMKeyChain getRM(KeyChain aKC)
{
    RMKeyChain kc = new RMKeyChain(); kc._op = getRM(aKC.getOp());
    for(int i=0;i<aKC.getChildCount();i++) { Object child = aKC.getChild(i);
        if(child instanceof KeyChain) kc.addChild(getRM((KeyChain)child));
        else kc.addChild(child);
    }
    return kc;
}

/**
 * Returns an RMKeyChain for given KeyChain.
 */
private static RMKeyChain.Op getRM(KeyChain.Op anOp)
{
    switch(anOp) {
        case Literal: return RMKeyChain.Op.Literal;
        case Add: return RMKeyChain.Op.Add; case Subtract: return RMKeyChain.Op.Subtract;
        case Multiply: return RMKeyChain.Op.Multiply; case Divide: return RMKeyChain.Op.Divide;
        case Mod: return RMKeyChain.Op.Mod; case Negate: return RMKeyChain.Op.Negate;
        case Equal: return RMKeyChain.Op.Equal; case NotEqual: return RMKeyChain.Op.NotEqual;
        case GreaterThan: return RMKeyChain.Op.GreaterThan; case LessThan: return RMKeyChain.Op.LessThan;
        case GreaterThanOrEqual: return RMKeyChain.Op.GreaterThanOrEqual;
        case LessThanOrEqual: return RMKeyChain.Op.LessThanOrEqual;
        case And: return RMKeyChain.Op.And; case Or: return RMKeyChain.Op.Or;
        case Not: return RMKeyChain.Op.Not; case Key: return RMKeyChain.Op.Key;
        case ArrayIndex: return RMKeyChain.Op.ArrayIndex; case FunctionCall: return RMKeyChain.Op.FunctionCall;
        case ArgList: return RMKeyChain.Op.ArgList; case Chain: return RMKeyChain.Op.Chain;
        case Conditional: return RMKeyChain.Op.Conditional; case Assignment: return RMKeyChain.Op.Assignment;
        default: throw new RuntimeException("RMKeyChain.getRM(op): Unsupported op: " + anOp);
    }
}

/**
 * Returns a thread-local assignments map.
 */
public static Map getAssignments()  { return _assTL.get(); }

/**
 * Returns the top level operator of the keychain.
 */
public Op getOp()  { return _op; }

/**
 * Returns the value of the keychain.
 */
public Object getValue()  { return _children; }

/**
 * Returns the value of the keychain as a string.
 */
public String getValueString()  { return _children instanceof String? (String)_children : null; }

/**
 * Returns the number of children in the keychain.
 */
public int getChildCount()  { return _children instanceof List? ((List)_children).size() : _children!=null? 1 : 0; }

/**
 * Returns the child at the given index in the keychain.
 */
public Object getChild(int anIndex)
{
    if(_children instanceof List) return ((List)_children).get(anIndex);
    if(anIndex==0) return _children;
    return null;
}

/**
 * Adds a child to the end of the keychain's child list.
 */
protected void addChild(Object child)
{
    if(_children==null) _children = child; // If first child, just set Children to point to it
    else if(_children instanceof List) ((List)_children).add(child); // If Children already list, just add child
    else { List c = new ArrayList(4); c.add(_children); c.add(child); _children = c; } // Else, create list and add
}

/**
 * Returns the child at the given index in the keychain as a string.
 */
public String getChildString(int i)  { Object o = getChild(i); return o==null? "<null string>" : o.toString(); }

/**
 * Returns the child at the given index in the keychain as a keychain.
 */
public RMKeyChain getChildKeyChain(int i)  { return (RMKeyChain)getChild(i); }

/**
 * Override to give list chance to implement this.
 */
public RMKeyChain subchain(int anIndex)
{
    int ccount = getChildCount(); if(anIndex+1==ccount) return getChildKeyChain(anIndex);
    RMKeyChain kc = new RMKeyChain(Op.Chain); for(int i=anIndex; i<ccount; i++) kc.addChild(getChild(i));
    return kc;
}

/**
 * Returns the result of evaluating the given key chain on the given object.
 */
public static Object getValue(Object anObj, Object aKeyChain)  { return getValue(anObj, getKeyChain(aKeyChain)); }

/**
 * Returns the result of evaluating the given key chain on the given object.
 */
public static Object getValue(Object anObj, RMKeyChain aKeyChain)  { return getValue(anObj, anObj, aKeyChain); }

/**
 * Returns the result of evaluating the given key chain on the given object.
 */
public static Object getValue(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    // If object is null, just return null
    if(anObj==null) return null;
    
    // If list, use aggregator
    if(anObj instanceof List && !RMGroup.isLeaf(anObj) && !RMGroup.isTopNOthers(anObj))
        return RMKeyChainAggr.getValue(aRoot, (List)anObj, aKeyChain);
    
    // If object implements getKeyChainValue, just forward on to it
    if(anObj instanceof RMKeyChain.Get) 
        return ((RMKeyChain.Get)anObj).getKeyChainValue(aRoot, aKeyChain);

    // Invoke the general implementation
    return getValueImpl(aRoot, anObj, aKeyChain);
}

/**
 * Returns the result of evaluating the given key chain on the given object.
 * Broken out so objects can implement custom getKeyChainValue but still have access to default implementation.
 */
public static Object getValueImpl(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    // Evaluate key chain based on operator type
    switch(aKeyChain.getOp()) {
    
        // Handle Literals: String, Number, Null
        case Literal: return aKeyChain.getValue();
        
        // Handle binary math ops: Add, Subtract, Multiply, Divide, Mod
        case Add: case Subtract:
        case Multiply: case Divide: case Mod: return getValueBinaryMathOp(aRoot, anObj, aKeyChain);
        
        // Handle Negate
        case Negate: {
            Object o1 = getValue(aRoot, anObj, aKeyChain.getChildKeyChain(0));
            return o1 instanceof Number? MathUtils.negate((Number)o1) : null;
        }
            
        // Handle binary compare ops: GreaterThan, LessThan, Equal, NotEqual, GreaterThanOrEqual, LessThanOrEqual
        case GreaterThan: case LessThan:
        case Equal: case NotEqual:
        case GreaterThanOrEqual: case LessThanOrEqual: return getValueBinaryCompareOp(aRoot, anObj, aKeyChain);
            
        // Handle Not
        case Not: return !getBoolValue(aRoot, anObj, aKeyChain.getChildKeyChain(0));
            
        // Handle binary logical ops: And, Or
        case And: case Or: {
            boolean b1 = getBoolValue(aRoot, anObj, aKeyChain.getChildKeyChain(0));
            boolean b2 = getBoolValue(aRoot, anObj, aKeyChain.getChildKeyChain(1));
            return  aKeyChain.getOp()==Op.And? (b1 && b2) : (b1 || b2);
        }
            
        // Handle basic Key
        case Key: {
            Object value = RMKey.getValue(anObj, aKeyChain.getValueString());
            if(value==null) value = getAssignments().get(aKeyChain.getValue());
            return value;
        }
             
        // Handle ArrayIndex
        case ArrayIndex: return getValueArrayIndex(aRoot, anObj, aKeyChain);
            
        // Handle FunctionCall
        case FunctionCall: return getValueFunctionCall(aRoot, anObj, aKeyChain);
            
        // Handle Chain
        case Chain: return getValueChain(aRoot, anObj, aKeyChain);
            
        // Handle Conditional
        case Conditional: {
            boolean result = getBoolValue(aRoot, anObj, aKeyChain.getChildKeyChain(0));
            if(result) return getValue(aRoot, anObj, aKeyChain.getChildKeyChain(1));
            return aKeyChain.getChildCount()==3? getValue(aRoot, anObj, aKeyChain.getChildKeyChain(2)) : null;
        }

        // Handle Assignment
        case Assignment: {
            Object value = getValue(aRoot, anObj, aKeyChain.getChildKeyChain(1));
            getAssignments().put(aKeyChain.getChildString(0), value); return "";
        }
        
        // Handle the impossible
        default: throw new RuntimeException("RMKeyChain.getValueImpl: Invalid op " + aKeyChain.getOp());
    }
}

/**
 * Returns a boolean value.
 */
private static boolean getBoolValue(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    Object value = getValue(aRoot, anObj, aKeyChain);
    return SnapUtils.boolValue(value);
}

/**
 * Returns the result of evaluating the given key chain with binary math operator.
 */
private static Object getValueBinaryMathOp(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    // Get value of operands
    Object o1 = getValue(aRoot, anObj, aKeyChain.getChildKeyChain(0));
    Object o2 = getValue(aRoot, anObj, aKeyChain.getChildKeyChain(1));
    
    // If non-numeric operand values (except add), just return
    if(!(o1 instanceof Number && o2 instanceof Number) && aKeyChain.getOp()!=Op.Add) return null;
    
    // Handle Math ops: Add, Subtract, Multiply, Divide, Mod
    switch(aKeyChain.getOp()) {
        case Add: return add(o1, o2);
        case Subtract: return MathUtils.subtract((Number)o1, (Number)o2);
        case Multiply: return MathUtils.multiply((Number)o1, (Number)o2);
        case Divide: return MathUtils.divide((Number)o1, (Number)o2);
        case Mod: return MathUtils.mod(SnapUtils.doubleValue(o1), SnapUtils.doubleValue(o2));
        default: throw new RuntimeException("RMKeyChain.getValueBinaryMathOp: Not a math op.");
    }
}

/**
 * Returns the sum of the two given objects (assumed to be strings or numbers).
 */
private static Object add(Object obj1, Object obj2)
{
    // If strings, do string concat (accounting for nulls)
    if(obj1 instanceof String || obj2 instanceof String)
        try { return (obj1==null? "" : obj1.toString()) + (obj2==null? "" : obj2.toString()); }
        catch(Exception e) { return null; }
        
    // If numbers, do Math.add()
    if(obj1 instanceof Number || obj2 instanceof Number)
        return MathUtils.add(SnapUtils.numberValue(obj1), SnapUtils.numberValue(obj2));
    
    // If nulls, just return null
    if(obj1==null && obj2==null) return null;
    
    // Fallback, try to add as strings or bail with null
    try { return (obj1==null? "" : obj1.toString()) + (obj2==null? "" : obj2.toString()); }
    catch(Exception e) { return null; }
}

/**
 * Returns the result of evaluating the given key chain with binary compare operator.
 */
private static Object getValueBinaryCompareOp(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    // Get value of operands
    Object o1 = getValue(aRoot, anObj, aKeyChain.getChildKeyChain(0));
    Object o2 = getValue(aRoot, anObj, aKeyChain.getChildKeyChain(1));

    // Handle binary compare ops: GreaterThan, LessThan, Equal, NotEqual, GreaterThanOrEqual, LessThanOrEqual
    switch(aKeyChain.getOp()) {
        case GreaterThan: return RMSort.Compare(o1, o2)==RMSort.ORDER_DESCEND;
        case LessThan: return RMSort.Compare(o1, o2)==RMSort.ORDER_ASCEND;
        case Equal: return RMSort.Compare(o1, o2)==RMSort.ORDER_SAME;
        case NotEqual: return RMSort.Compare(o1, o2)!=RMSort.ORDER_SAME;
        case GreaterThanOrEqual: return RMSort.Compare(o1, o2)!=RMSort.ORDER_ASCEND;
        case LessThanOrEqual: return RMSort.Compare(o1, o2)!=RMSort.ORDER_DESCEND;
        default: throw new RuntimeException("RMKeyChain.getValueBinaryCompareOp: Not a compare op.");
    }
}

/**
 * Returns the result of evaluating the given ArrayIndex KeyChain on the given object.
 */
private static Object getValueArrayIndex(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    RMKeyChain arrayKeyChain = aKeyChain.getChildKeyChain(0);
    Object o1 = getValue(anObj, arrayKeyChain); if(!(o1 instanceof List)) return null;
    RMKeyChain indexKeyChain = aKeyChain.getChildKeyChain(1);
    int index = getIntValue(aRoot, indexKeyChain);
    return ListUtils.get((List)o1, index);
}

/**
 * Returns the result of evaluating the given Function KeyChain on the given object.
 */
private static Object getValueFunctionCall(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    // If functionCall was found, invoke it and return
    RMKeyChainFuncs fcall = RMKeyChainFuncs.getFunctionCall(aRoot, anObj, aKeyChain);
    if(fcall!=null)
        try { return fcall.invoke(anObj); } // Invoke method
        catch(Exception e) { System.err.println(e); }
    return null;
}

/**
 * Returns the result of evaluating the given Chain KeyChain on the given object.
 */
private static Object getValueChain(Object aRoot, Object anObj, RMKeyChain aKeyChain)
{
    Object value = anObj;
    for(int i=0, iMax=aKeyChain.getChildCount(); i<iMax; i++) { RMKeyChain child = aKeyChain.getChildKeyChain(i);
        value = getValue(aRoot, value, child);
        if(value instanceof List && i+1<iMax)
            return RMKeyChainAggr.getValue(aRoot, (List)value, aKeyChain.subchain(i+1));
    }
    return value;
}

/** Convenience - returns a string for an object and key chain. */
public static String getStringValue(Object anObj, Object aKeyChain)
{ return SnapUtils.stringValue(getValue(anObj, aKeyChain)); }

/** Convenience - returns a number for an object and key chain. */
public static Number getNumberValue(Object anObj, Object aKeyChain)
{ return SnapUtils.numberValue(getValue(anObj, aKeyChain)); }

/** Convenience - returns an int for an object and key chain. */
public static int getIntValue(Object anObj, Object aKeyChain)
{ return SnapUtils.intValue(getValue(anObj, aKeyChain)); }

/** Convenience - returns a double for an object and key chain. */
public static double getDoubleValue(Object anObj, Object aKeyChain)
{ return SnapUtils.doubleValue(getValue(anObj, aKeyChain)); }

/** Convenience - returns a boolean for an object and key chain. */
public static boolean getBoolValue(Object anObj, Object aKeyChain)
{ return SnapUtils.boolValue(getValue(anObj, aKeyChain)); }

/**
 * Convenience - returns a list for an object and key chain.
 */
public static List getListValue(Object anObj, Object aKeyChain)
{
    RMKeyChain kc = getKeyChain(aKeyChain);
    Object value = RMKeyChainAggr.getListValue(anObj, kc);
    return value instanceof List? (List)value : value!=null? Arrays.asList(value) : null;
}

/**
 * Returns whether given key has a Page/PageMax key reference.
 */
public boolean hasPageReference()
{
    if(_hasPageReference==null)
        _hasPageReference = anyKeyReferencesKey("Page") && !toString().contains("Page.") ||
            anyKeyReferencesKey("PageMax") || anyKeyReferencesKey("PageBreak") || anyKeyReferencesKey("PageBreakMax") ||
            anyKeyReferencesKey("PageBreakPage") || anyKeyReferencesKey("PageBreakPageMax");
    return _hasPageReference;
}

/**
 * Returns whether given key is present anywhere in expression.
 */
private boolean anyKeyReferencesKey(String aKey)
{
    // If operator is key, return if key is equals to key chain value
    if(getOp()==Op.Key) return aKey.equals(getValue());
    
    // Iterate over children and forward on
    for(int i=0, iMax=getChildCount(); i<iMax; i++)
        if(getChild(i) instanceof RMKeyChain && getChildKeyChain(i).anyKeyReferencesKey(aKey))
            return true;
    
    // Return false since key chain type not applicable
    return false;
}

/**
 * Returns whether key contains given op.
 */
public boolean hasOp(Op anOp)
{
    if(getOp()==anOp) return true; // If this keychain's operator is given op, return true
    for(int i=0, iMax=getChildCount(); i<iMax; i++) // If any child operators match given op, return true
        if(getChild(i) instanceof RMKeyChain && getChildKeyChain(i).hasOp(anOp))
            return true;
    return false; // Return false since all checks failed
}

/**
 * Returns the last error encountered by the key chain parser (or null).
 */
public static String getError()  { return KeyChain.getError(); }

/**
 * Returns the last error encountered by the key chain parser and resets parser.
 */
public static String getAndResetError()  { return KeyChain.getAndResetError(); }

/**
 * Returns a string representation of the key chain.
 */
public String toString()
{
    // Get op (just return ValueString if Key or Literal)
    Op op = getOp(); if(op==Op.Key || op==Op.Literal) return getValueString();
    
    // Otherwise, build string in StringBuffer
    StringBuffer sb = new StringBuffer(); int cc = getChildCount();
    switch(op) {
        case Add: sb.append(getChild(0)).append('+').append(getChild(1)); break;
        case Subtract: sb.append(getChild(0)).append('-').append(getChild(1)); break;
        case Multiply: sb.append(getChild(0)).append('*').append(getChild(1)); break;
        case Divide: sb.append(getChild(0)).append('/').append(getChild(1)); break;
        case Mod: sb.append(getChild(0)).append('%').append(getChild(1)); break;
        case Negate: sb.append('-').append(getChild(0)); break;
        case GreaterThan: sb.append(getChild(0)).append('>').append(getChild(1)); break;
        case LessThan: sb.append(getChild(0)).append('<').append(getChild(1)); break;
        case GreaterThanOrEqual: sb.append(getChild(0)).append(">=").append(getChild(1)); break;
        case LessThanOrEqual: sb.append(getChild(0)).append("<=").append(getChild(1)); break;
        case Equal: sb.append(getChild(0)).append("==").append(getChild(1)); break;
        case NotEqual: sb.append(getChild(0)).append("!=").append(getChild(1)); break;
        case Not: sb.append('!').append(getChild(0)); break;
        case And: sb.append(getChild(0)).append(" && ").append(getChild(1)); break;
        case Or: sb.append(getChild(0)).append(" || ").append(getChild(1)); break;
        case ArrayIndex: sb.append(getChild(0)).append('[').append(getChild(1)).append(']'); break;
        case FunctionCall: sb.append(getChild(0)).append('(').append(getChild(1)).append(')'); break;
        case ArgList: for(int i=0,iMax=cc;i<iMax;i++) { if(i>0) sb.append(','); sb.append(getChild(i)); } break;
        case Conditional: sb.append(getChild(0)).append('?').append(getChild(1));
            if(cc>2) sb.append(':').append(getChild(2)); break;
        case Chain: for(int i=0,iMax=cc;i<iMax; i++) { if(i>0) sb.append('.'); sb.append(getChild(i)); } break;
    }
    return sb.toString();
} 

/**
 * Adds a class to the list of classes that RM queries for functions.
 */
public static void addFunctionClass(Class aClass) { RMKeyChainFuncs.addFunctionClass(aClass); }

/**
 * Simple main implementation, so RM's expressions can be used for simple math.
 */
public static void main(String args[]) throws IOException
{
    // If there is an arg, evaluate it, otherwise if no args, read from standard in until control-d
    if(args.length>0 && args[0].length()>0) { Object value = RMKeyChain.getValue(new Object(), args[0]);
        System.out.println(value instanceof Number? SnapUtils.getBigDecimal(value) : value); }
    else { BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        for(String ln=rdr.readLine(); ln!=null; ln=rdr.readLine()) main(new String[] { ln }); }
}

}