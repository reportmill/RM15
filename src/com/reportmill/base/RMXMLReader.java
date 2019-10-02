/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This class creates an object graph of collections (Map/List) and core Java types from a given XML source.
 * This works best when an RMSchema is provided or an <RMSchema> tag is present in the xml (otherwise one is generated).
 */  
public class RMXMLReader {
    
    // The name of the top level element that was read
    String                    _name;
    
    // The schema of the read data (either loaded from RMSchema tag, reverse engineered, or provided to readObject)
    Schema                    _schema;
    
    // The resource elements in the xml file (RMResource tags)
    List <XMLElement>          _resources = new ArrayList();
    
    // A cache of lists for specific element names
    Map <String, List <Map>>  _entityLists = new LinkedHashMap();
    
/**
 * Creates an uninitialized reader.
 */
public RMXMLReader() { }

/**
 * Returns a map loaded from the given XML source.
 */
public Map readObject(Object aSource)  { return readObject(aSource, null); }

/**
 * Returns a map loaded from the given XML source with the given XML schema.
 */
public Map readObject(Object aSource, Schema aSchema)
{
    // Get root element for source
    XMLElement rootXML = XMLElement.getElement(aSource);
    
    // If root is null, return null
    if(rootXML==null)
        return null;

    // Get root name
    _name = rootXML.getName();

    // Get schema element if present (and remove it from root)
    XMLElement schema = rootXML.get("RMSchema");
    if(schema!=null)
        rootXML.removeElement(schema);
                
    // If schema is provided, set it
    if(aSchema!=null)
        _schema = aSchema;
    
    // Otherwise, if schema element is available, create and read schema
    else if(schema!=null)
        _schema = new Schema(_name).fromXML(null, schema);
            
    // Otherwise, reverse engineer it from element
    else _schema = new RMSchemaMaker().getSchema(rootXML);
    
    // Make sure schema has root entity
    _schema.getRootEntity();
    
    // Iterate over resources, remove from root element and add to resources list
    for(int i=rootXML.indexOf("RMResource"); i>=0; i=rootXML.indexOf("RMResource", i))
        _resources.add(rootXML.removeElement(i));
    
    // Create root map
    Map rootMap = new LinkedHashMap();

    // Read rootMap from root element (recursively reads everything)
    read(rootXML, rootMap, _name);
    
    // Return root map
    return rootMap;
}

/**
 * Loads given map with collections & core types from given XML element, according to schema.
 */
private void read(XMLElement anElement, Map aMap, String anEntityName)
{
    // Get entity, return if not found (should never happen, but I've seen it - maybe somehow with core types?)
    Entity entity = _schema.getEntity(anEntityName);
    if(entity==null) {
        System.err.println("RMXMLReader: Couldn't find entity named " + anEntityName); return; }
    
    // Iterate over entity properties
    for(int i=0, iMax=entity.getPropertyCount(); i<iMax; i++) { Property prop = entity.getProperty(i);
        
        // If property is plain attribute, get string for property, convertToType and put in Map
        if(prop.isAttribute()) {
            
            // Get property name and value string
            String propName = prop.getName();
            String valueStr = anElement.getAttributeValue(propName);
            
            // If null, see if there is a child xml element
            if(valueStr==null) {
                XMLElement valueEle = anElement.get(propName);
                valueStr = valueEle==null? null : valueEle.getValue();
            }
            
            // Get property value for string and add to map
            Object value = prop.convertValue(valueStr);
            if(value!=null)
                aMap.put(propName, value);
        }
        
        // Handle Relations
        else readRelation(anElement, aMap, prop);        
    }
}

/**
 * Loads given map with collections & core types from given XML element, according to schema.
 */
private void readRelation(XMLElement anElement, Map aMap, Property aRelation)
{
    // Get property name and property relation entity name
    String propertyName = aRelation.getName();
    String relationEntityName = aRelation.getRelationEntityName();
    if(relationEntityName==null || relationEntityName.startsWith("[")) // Just return if null or Array class
        return;

    // Handle to-many relation: get children and iterate over them to recursively read and add to to-many list
    if(aRelation.isToMany()) {
        
        // Declare variable for list
        List list = null;
        
        // Iterate over child elements with name key
        for(int j=anElement.indexOf(propertyName); j>=0; j=anElement.indexOf(propertyName, j+1)) {
        
            // Create list if needed
            if(list==null)
                aMap.put(propertyName, list = new ArrayList());

            // Get child xml element
            XMLElement child = anElement.get(j);
            
            // Get unique map for child xml element
            Map map = getUniqueMap(child, relationEntityName);
            
            // Add to list
            list.add(map);
            
            // Recurse into read
            read(child, map, relationEntityName);
        }
    }
    
    // Handle to-one relation: get Map for child and recurse
    else {
        
        // Get xml element for to-one relation property
        XMLElement childXML = anElement.get(propertyName);
        
        // If no child element, but there is a child attribute, create a bogus element
        if(childXML==null && anElement.hasAttribute(propertyName)) {
            Entity relationEntity = aRelation.getRelationEntity();
            Property primary = relationEntity!=null? relationEntity.getPrimary() : null;
            String primaryName = primary!=null? primary.getName() : "ID";
            String primaryValue = anElement.getAttributeValue(propertyName);
            childXML = new XMLElement(propertyName);
            childXML.add(primaryName, primaryValue);
        }
        
        // If xml element found, get unique map for child xml, add to parent and recurse
        if(childXML!=null) {
            Map map = getUniqueMap(childXML, relationEntityName);
            aMap.put(propertyName, map);
            read(childXML, map, aRelation.getRelationEntityName());
        }
    }
}


/**
 * Returns a unique map for the given xml element and entity name using primary keys
 */
private Map getUniqueMap(XMLElement anElement, String anEntityName)
{
    // Get list for entity name
    List <Map> entityMaps = getEntityList(anEntityName);
    
    // Get entity and list of primaries
    Entity entity = getSchema().getEntity(anEntityName);
    List <? extends Property> primaries = entity.getPrimaries();
    
    // Create map with primary key values from element
    Map map1 = new LinkedHashMap();
    
    // Add primary key values from element
    for(Property property : primaries) {

        // Get primary value string (just return new map if any primary key is null)
        String valueString = anElement.getAttributeValue(property.getName());
        if(valueString==null)
            return map1;
        
        // Get value and add to map
        Object value = property.convertValue(valueString);
        if(value!=null)
            map1.put(property.getName(), value);
    }
    
    // Iterate over entity maps
    if(primaries.size()>0) {
        for(int i=0, iMax=entityMaps.size(); i<iMax; i++) { Map map2 = entityMaps.get(i);
            
            // Iterate over primary properties to see if maps' primary property(s) match
            for(int j=0, jMax=primaries.size(); j<jMax && map2!=null; j++) { Property property = primaries.get(j);
                
                // Get primary property values and if they differ, break
                Object map1PrimaryValue = map1.get(property.getName());
                Object map2PrimaryValue = map2.get(property.getName());
                if(!SnapUtils.equals(map1PrimaryValue, map2PrimaryValue))
                    map2 = null;
            }
    
            // If loop map matched all primaries for element, return it
            if(map2!=null)
                return map2;
        }
    }
    
    // Add map1 to entity maps and return it
    entityMaps.add(map1);
    return map1;
}

/**
 * Returns the name of the root element.
 */
public String getName()  { return _name; }

/**
 * Returns a hierarchy of RMEntity objects describing the XML.
 */
public Schema getSchema()  { return _schema; }

/**
 * Returns the resources read from last source.
 */
public List <XMLElement> getResources()  { return _resources; }

/**
 * Returns the entity maps.
 */
public Map <String, List <Map>> getEntityLists()  { return _entityLists; }

/**
 * Returns the individual list of maps for the given entity name.
 */
public List <Map> getEntityList(String aName)
{
    // Get the list for the given element name (if absent, create and add)
    List list = _entityLists.get(aName);
    if(list==null)
        _entityLists.put(aName, list = new ArrayList());
    return list;
}

}