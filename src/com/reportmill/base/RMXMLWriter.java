/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This class turns any Java dataset into XML. The most common use of this class is simply the writeObject()
 * method, which takes a Java dataset and results in a very clean XML description (at the given path):
 * <p><blockquote><pre>
 *   new RMXMLWriter().writeObject(myObjects, "MyDataset.xml");
 * </pre></blockquote><p>
 * The Java dataset can take almost any form: a list, map or hierarchy of EJBs, custom Java
 * classes, Java collections classes or even JDBC ResultSets. The resulting XML file is a very clean description of
 * all the public fields, methods and keys/values in the dataset (out to three levels deep, which is the default
 * depth limit).
 */
public class RMXMLWriter {

    // Used to create a schema
    RMSchemaMaker           _schemaMaker;
    
    // Describes entities and properties of object graph
    Schema                  _schema;
    
    // Tracks circular references
    List                    _processedMaps = new ArrayList(100);
    
    // Tracks circular references
    List <XMLElement>       _processedElements = new ArrayList(100);
    
    // Tracks circular references
    List <Entity>           _processedEntities = new ArrayList(100);
    
    // A map of unique id's for properties
    Map <Property,Integer>  _propertyNextUniqueID = new HashMap();
    
    // String used for Schema id tag
    public static final String TAG_ID = "id";

/**
 * Returns the schema maker.
 */
public RMSchemaMaker getSchemaMaker()  { return _schemaMaker!=null? _schemaMaker : (_schemaMaker=createSchemaMaker()); }

/**
 * Creates a new schema maker.
 */
protected RMSchemaMaker createSchemaMaker()  { return new RMSchemaMaker(); }

/** Returns whether to only use getX/isX accessor methods (as opposed to any method returning a value). */
public boolean getUseGetAndIsMethodsOnly()  { return getSchemaMaker().getUseGetAndIsMethodsOnly(); }

/** Sets whether to only use getX/isX accessor methods (as opposed to any method returning a value). */
public void setUseGetAndIsMethodsOnly(boolean aFlag)  { getSchemaMaker().setUseGetAndIsMethodsOnly(aFlag); }

/** Returns whether to include fields. */
public boolean getIncludeFields()  { return getSchemaMaker().getIncludeFields(); }

/** Sets whether to include fields. */
public void setIncludeFields(boolean aFlag)  { getSchemaMaker().setIncludeFields(aFlag); }

/** Tells writer to ignore any member of the given class from the serialized object graph. */
public void ignoreClass(Class aClass)  { getSchemaMaker().addIgnoreClass(aClass); }

/** Tells writer to ignore any member of the given class name from the serialized object graph. */
public void ignoreClass(String aClassName)  { getSchemaMaker().addIgnoreClass(aClassName); }

/** Tells writer to ignore any member with the given method/field name from the serialized object graph. */
public void ignoreMember(String aFieldName)  { getSchemaMaker().addIgnoreMember(aFieldName); }

/** Tells writer to ignore any member with the given class - member combination. */
public void ignoreMember(Class aClass, String aName)  { getSchemaMaker().addIgnoreMember(aClass, aName); }

/** Tells writer to ignore any member with the given classname - member combination. */
public void ignoreMember(String aClassName, String aName)  { getSchemaMaker().addIgnoreMember(aClassName, aName); }

/**
 * Returns the maximum number of items to write for lists/array relationships.
 */
public int getBreadthLimit()  { return getSchemaMaker().getBreadthLimit(); }

/**
 * Sets the maximum number of items to write for lists/array relationships.
 */
public void setBreadthLimit(int aLimit)  { getSchemaMaker().setBreadthLimit(aLimit); }

/**
 * Writes given Java dataset to given path as XML, out to three levels deep, which is default depth limit.
 */
public void writeObject(Object anObj, String aPath)  { writeObject(anObj, aPath, 5); }

/**
 * Writes given Java dataset to given path as XML, out to given depth limit.
 */
public void writeObject(Object anObj, String aPath, int dos)  { writeObject(anObj, null, aPath, dos); }

/**
 * Writes given Java dataset and optional userInfo to given path as XML, out to given depth limit.
 */
public void writeObject(Object anObj, Object userInfo, String aPath, int aDepthLimit)
{
    // Get XML element for object, userinfo and depth limit
    XMLElement rootElement = getXML(anObj, userInfo, aDepthLimit);
    if(rootElement==null)
        return;
    
    // Make sure path has .xml at end
    if(!aPath.endsWith(".xml"))
        aPath += ".xml";
    
    // Write to path
    byte bytes[] = rootElement.getBytes();
    try { FileUtils.writeBytes(new java.io.File(aPath), bytes); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/**
 * Returns an xml XMLElement tree for given Java dataset and optional userInfo, out to given depth limit.
 */
public XMLElement getXML(Object anObj, Object userInfo, int aDepthLimit)
{
    // If object is list, turn into map
    if(anObj instanceof List)
        anObj = Collections.singletonMap("objects", anObj);
    
    // If userInfo is non-null, then fold anObj and userInfo into anObj
    if(userInfo!=null) {
        Map map = new HashMap();
        anObj = ReportMill.convertFromAppServerType(anObj);
        userInfo = ReportMill.convertFromAppServerType(userInfo);
        if(anObj instanceof Map) map.putAll((Map)anObj);
        else if(anObj instanceof List) map.put("objects", anObj);
        if(userInfo instanceof Map) map.putAll((Map)userInfo);
        else if(userInfo!=null) map.put("userInfo", userInfo);
        anObj = map;
    }
    
    // Return xml for schema and map
    return getXML(anObj, null, aDepthLimit);
}

/**
 * Returns an element tree for given collections map and schema.
 */
public XMLElement getXML(Object anObj, Schema aSchema, int aDepthLimit)
{
    // Set schema - or create it with schema maker if not provided
    _schema = aSchema;
    if(_schema==null)
        _schema = getSchemaMaker().getSchema(anObj, "Root", aDepthLimit);
    
    // Create new root xml element
    XMLElement rootXML = new XMLElement(_schema.getName());
    
    // WriteMap on rootXML to get things started (ignores List/Map entries)
    writeXML(anObj, rootXML, _schema.getRootEntity());
    
    // Call writeMapDeep on previously written maps (this results in better ordering than just writeMapDeep on root)
    for(int i=0; i<_processedMaps.size(); i++) {
        Object object = _processedMaps.get(i);
        XMLElement xml = _processedElements.get(i);
        Entity entity = _processedEntities.get(i); 
        writeXMLDeep(object, xml, entity);
    }
    
    // Add schema xml to root element
    rootXML.add(_schema.toXML(null));
    
    // Clear _processed lists
    _processedMaps.clear();
    _processedElements.clear();
    _processedEntities.clear();
    
    // Return root element
    return rootXML;
}

/**
 * Returns the schema.
 */
public Schema getSchema()  { return _schema; }

/**
 * Adds XML Attributes to an XML Element for entries of core types in a Map.
 */
private void writeXML(Object anObject, XMLElement anElement, Entity anEntity)
{
    // If given map already processed, just add reference
    int processedIndex = ListUtils.indexOfId(_processedMaps, anObject);
    if(processedIndex>=0) {
        XMLElement orig = _processedElements.get(processedIndex);
        writeXMLReference(anElement, orig, anEntity);
        return;
    }

    // Add map, element and entity to processed lists
    _processedMaps.add(anObject);
    _processedElements.add(anElement);
    _processedEntities.add(anEntity);
    
    // Iterate over entity properties
    for(Property property : anEntity.getProperties()) {
        
        // If simple attribute, get value and add to xml
        if(property.isAttribute()) {
            Object value = RMKey.getValue(anObject, property.getName());
            if(value!=null)
                anElement.add(property.getName(), property.toString(value));
        }
    }
}

/**
 * Handles adding sub-elements for List/Map entries in a Map.
 */
private void writeXMLDeep(Object anObject, XMLElement anElement, Entity anEntity)
{
    // Bogus - return if entityMap not found (see next bogus)
    if(anEntity==null)
        return;
    
    // Iterate over entity properties (threw concurrent excpt with iterator - I shouldn't just cover it up like this)
    for(int i=0, iMax=anEntity.getPropertyCount(); i<iMax; i++) { Property property = anEntity.getProperty(i);
        
        // Skip simple attributes
        if(property.isAttribute())
            continue;
    
        // Get value for property from given object (just continue if null)
        Object value = RMKey.getValue(anObject, property.getName());
        if(value==null)
            continue;
        
        // Get relation entity (if absent, just skip - not sure why this might happen)
        Entity relationEntity = property.getRelationEntity();
        if(relationEntity==null) {
            System.err.println("RMXMLWriter:writeXMLDeep: Null Relation Entity for " + property.getName()); continue; }
        
        // If value is List, iterate over items and create element for each, add to parent and recursively write deep
        if(value instanceof List) { List list = (List)value;
            for(int j=0,jMax=Math.min(list.size(),getBreadthLimit()); j<jMax; j++) { Object item = list.get(j);
                XMLElement e = new XMLElement(property.getName());
                anElement.add(e);
                writeXML(item, e, relationEntity);
            }
        }
        
        // If value is not List, create new element for it, add to parent and recursively write deep
        else {
            XMLElement e = new XMLElement(property.getName());
            anElement.add(e);
            writeXML(value, e, relationEntity);
        }
    }
}

/**
 * Handles writing a reference to a previously defined element.
 */
private void writeXMLReference(XMLElement anElement, XMLElement origElement, Entity anEntity)
{
    // Get primary property (create and add one to entity if missing)
    Property property = anEntity.getPrimary();
    if(property==null) {
        property = new Property(TAG_ID, Property.Type.Number);
        property.setPrimary(true);
        property.setPrivate(true);
        anEntity.addProperty(property);
    }
    
    // Get primary value string for original element (add if absent)
    String value = origElement.getAttributeValue(property.getName());
    if(value==null)
        origElement.add(property.getName(), value = "" + getPropertyNextUniqueID(property));
    
    // If given element is non-null, install original element's id in it
    if(anElement!=null)
        anElement.add(property.getName(), value);
}

/**
 * Returns the next unique id for given property.
 */
public int getPropertyNextUniqueID(Property aProperty)
{
    Integer id = _propertyNextUniqueID.get(aProperty);
    if(id==null) id = 1; else id++;
    _propertyNextUniqueID.put(aProperty, id);
    return id;
}

}