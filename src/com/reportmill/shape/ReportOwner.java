/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import java.util.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * A base class that loads and runs reports.
 */
public class ReportOwner implements RMKeyChain.Get {

    // The template
    RMDocument       _template;
    
    // The string used to represent null values
    String           _nstring;

    // Whether to paginate
    boolean          _paginate = true;
    
    // The main model object
    Map              _model = new HashMap();
    
    // Objects from user's dataset to be queried for key substitution
    List             _dataStack = new ArrayList();
    
    // The list of objects passed to generateReport, if called with just a list
    List             _defaultList;
    
    // Shapes that contain page keys
    List             _pageRefShapes = new ArrayList();
    
    // Provides a hook for didFillShape notification
    ReportMill.Listener  _listener;

/**
 * Returns the template.
 */
public RMDocument getTemplate()  { return _template!=null? _template : (_template=createTemplate()); }

/**
 * Creates the template.
 */
protected RMDocument createTemplate()
{
    WebURL url = WebURL.getURL(getClass(), getClass().getSimpleName() + ".rpt");
    return RMDocument.getDoc(url);
}

/**
 * Sets the template.
 */
protected void setTemplate(RMDocument aDoc)  { _template = aDoc; }

/**
 * Returns the string used to represent null values.
 */
public String getNullString()  { return _nstring; }

/**
 * Sets the string used to represent null values.
 */
public void setNullString(String aString)  { _nstring = aString; }

/**
 * Returns whether this reportmill paginates.
 */
public boolean getPaginate()  { return _paginate; }

/**
 * Sets whether this reportmill paginates.
 */
public void setPaginate(boolean aFlag)  { _paginate = aFlag; }

/**
 * Returns the model object.
 */
public Object getModel()  { return _model; }

/**
 * Sets objects in this reportmill.
 */
public void addModelObject(Object anObj)
{
    // Convert object to standard type
    Object obj = convertToStandardType(anObj); if(obj==null) return;
    
    // if object is ResultSet, convert to List of Maps
    if(obj instanceof java.sql.ResultSet) obj = RMSQLUtils.getMaps((java.sql.ResultSet)obj, 0);

    // If object is List, make it DefaultList
    if(obj instanceof List) //_model.put("RMDefaultObjectList", obj);
        _defaultList = (List)obj;
        
    // If object is Map, replace any ResultSets with List
    else if(obj instanceof Map) { Map map = (Map)obj; map = RMSQLUtils.getMapsDeep(map, 2);
        _model.putAll(map); }
    
    // If ReportMill.Listener set Listener
    else if(obj instanceof ReportMill.Listener) _listener = (ReportMill.Listener)obj;
        
    // Add objects and userInfo to DataStack
    else if(obj!=null)
        pushDataStack(obj);
    
    // If model map has value, make sure is at front of stack
    if(_model.size()>0 && (_dataStack.size()==0 || _dataStack.get(0)!=_model)) _dataStack.add(0, _model);
}

/**
 * Adds a data object to the data object list.
 */
public void pushDataStack(Object anObj)  { _dataStack.add(anObj); }

/**
 * Removes a specific object at given index in list.
 */
public Object popDataStack()  { return _dataStack.remove(_dataStack.size()-1); }

/**
 * Returns the last data object in the data objects list.
 */
public Object peekDataStack()  { return ListUtils.getLast(_dataStack); }

/**
 * Generates the report.
 */
public RMDocument generateReport()
{
    // If objects and user info is null, add a bogus object so keychain assignments will work (probably silly)
    if(_dataStack.size()==0) addModelObject(new Object());

    // Generate report and return    
    RMDocument doc = (RMDocument)rpg(getTemplate(), null); doc._reportOwner = this;
    return doc;
}

/**
 * Performs RPG on a given shape.
 */
public RMShape rpg(RMShape aShape, RMShape aParent)
{
    RMShape rpg = aShape.rpgAll(this, aParent);
    if(_listener!=null) _listener.didFillShape(aShape, rpg);
    return rpg;
}

/**
 * Returns the list of page reference shapes.
 */
public List <RMShape> getPageReferenceShapes()  { return _pageRefShapes; }

/**
 * Sets the list of page reference shapes.
 */
public void setPageReferenceShapes(List aList)  { _pageRefShapes = aList; }

/**
 * Registers a shape with a page key in it.
 */
public void addPageReferenceShape(RMShape aShape)  { ListUtils.addUniqueId(_pageRefShapes, aShape); }

/**
 * RMKeyChain.Get implementation to run against DataStack.
 */
public Object getKeyChainValue(Object aRoot, RMKeyChain aKeyChain)
{
    // If Op is Key, Chain or FunctionCall, evaluate against DataStack objects
    RMKeyChain.Op op = aKeyChain.getOp();
    if(op==RMKeyChain.Op.Key || op==RMKeyChain.Op.Chain || op==RMKeyChain.Op.FunctionCall) {

        // If FunctionCall, try it on 'this' first to keep broader scoping (works for static functions only)
        if(op==RMKeyChain.Op.FunctionCall) {
            Object val = RMKeyChain.getValueImpl(aRoot, this, aKeyChain);
            if(val!=null)
                return val;
        }
        
        // Check for "Root" key (evaluates key remainder on DataStack root)
        if(op==RMKeyChain.Op.Chain) { String key = aKeyChain.getChildString(0);
            if(key.equals("Root")) {
                Object dso = _dataStack.get(0), val = RMKeyChain.getValue(aRoot, dso, aKeyChain.subchain(1));
                if(val!=null)
                    return val;
            }
        }
        
        // Try to evaluate KeyChain against DataStack objects
        for(int i=_dataStack.size()-1; i>=0; i--) { Object dso = _dataStack.get(i);
            Object val = RMKeyChain.getValue(aRoot, dso, aKeyChain);
            if(val!=null)
                return val;
        }

        // If Key wasn't evaluated above, see if it is a special ReportMill key
        if(op==RMKeyChain.Op.Key) { String key = aKeyChain.getValueString();
            if(key.equals("Date")) return new Date();  // Date key
            else if(key.equals("PageBreakPage")) System.err.println("Need to fix PageBreakPage"); // PageBreak keys
            else if(key.startsWith("RM")) return getRMKey(key); // RM Keys
        }
        
        // Return null since KeyChain value not found
        return null;
    }
    
    // If Op not a Key or FunctionCall, do normal version
    return RMKeyChain.getValueImpl(aRoot, this, aKeyChain);
}

/**
 * Returns a list for the given keychain.
 */
public List getKeyChainListValue(String aKeyChain)
{
    // Call RMKeyChain.getListValue on DataStack first item
    List list = RMKeyChain.getListValue(peekDataStack(), aKeyChain);
    
    // If list not found, call RMKeyChain.getListValue on Model
    if(list==null && peekDataStack()!=_model)
        list = RMKeyChain.getListValue(_model, aKeyChain);
        
    // If list not found, use DefaultList (might also be null). Return list.
    if(list==null)
        list = _defaultList;
    return list;
}

/**
 * Called by various objects to convert objects to generic types.
 */
protected Object convertToStandardType(Object anObj)
{
    if(ReportMill.appServer!=null) return ReportMill.appServer.convertFromAppServerType(anObj);
    if(anObj instanceof Set) return new ArrayList((Set)anObj); // If object is Set, return List
    else if(anObj instanceof Object[]) return Arrays.asList((Object[])anObj); // If object is array, return List
    return anObj; // Return object
}

/**
 * Performs page substitutions on any text fields that were identified as containing @Page@ keys.
 */
public void resolvePageReferences()
{
    // Iterate over page reference shapes and have them resolve
    List <RMShape> prshapes = getPageReferenceShapes();
    for(int i=0, iMax=prshapes.size(); i<iMax; i++) { RMShape shape = prshapes.get(i);
        
        // Create page info map
        Map info = new HashMap();
        info.put("Page", shape.page());
        info.put("PageMax", shape.pageMax());
        info.put("PageBreak", shape.getPageBreak());
        info.put("PageBreakMax", shape.getPageBreakMax());
        info.put("PageBreakPage", shape.getPageBreakPage());
        info.put("PageBreakPageMax", shape.getPageBreakPageMax());
        
        // Resolve page references with page info map
        shape.resolvePageReferences(this, info);
    }
    
    // Clear page reference shapes list
    prshapes.clear();
}

/**
 * Returns a value for some silly RM defined keys.
 */
private Object getRMKey(String key)
{
    if(key.equals("RMRandom")) return MathUtils.randomInt();
    if(key.equals("RMVersion"))
        return String.format("ReportMill %f (Build Date: %s)", ReportMill.getVersion(), ReportMill.getBuildInfo());
    if(key.equals("RMUser")) return System.getProperty("user.name");
    if(key.equals("RMUserHome")) return System.getProperty("user.home");
    if(key.equals("RMProps")) return System.getProperties().toString();
    if(key.equals("RMJeff")) return "Jeffrey James Martin";
    if(key.equals("RMLogo")) return "http://mini.reportmill.com/images/RM-Logo.gif";
    if(key.equals("RMHTML")) return "<html><b>Howdy Doody</b></html>";
    if(key.equals("RMJapanese"))
        return new String("\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c\u306e\u4eba\u3005\uff01");
    if(key.equals("RMFlowers")) return WebURL.getURL(getClass(), "/snap/viewx/pkg.images/tulips.jpg");
    return null;
}

/**
 * A shape class to represent multiple pages of shapes.
 */
public static class ShapeList extends RMParentShape {
    public int removeChild(RMShape aChild)  { return -1; }
}

}