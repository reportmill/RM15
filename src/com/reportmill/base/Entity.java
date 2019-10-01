/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This class represents an entity for a data source. It has a list of properties, some of which are simple
 * attributes and some of which are relationships.
 */
public class Entity implements XMLArchiver.Archivable {
    
    // The schema that owns this entity
    Schema             _schema;

    // Entity name
    String             _name;
    
    // Entity properties
    List <Property>    _props = new ArrayList();
    
    // The class that this entity represents
    Class              _class;
    
    // Cached lists of properties that are attributes (simple properties), relations, primaries, etc.
    List <Property>    _attrs, _relations, _primaries, _attrsSorted, _relationsSorted;
    
    // Constants for properties
    final public static String Name_Prop = "Name";

/**
 * Creates an empty entity.
 */
public Entity()  { }

/**
 * Creates an entity with the given name.
 */
public Entity(String aName)  { setName(aName); }

/**
 * Returns the schema that owns this entity.
 */
public Schema getSchema()  { return _schema; }

/**
 * Sets the schema that owns this entity.
 */
public void setSchema(Schema aSchema)  { _schema = aSchema; }

/**
 * Returns a named entity using entity resolver.
 */
public Entity getEntity(String aName)  { return _schema!=null? _schema.getEntity(aName) : null; }

/**
 * Returns the name of the entity.
 */
public String getName()  { return _name; }

/**
 * Sets the name of the entity.
 */
public void setName(String aName)
{
    String name = aName!=null? aName.trim().replace(" ", "") : null;
    if(SnapUtils.equals(name, _name)) return;
    _name = name;
}

/**
 * Returns the number of properties.
 */
public int getPropertyCount()  { return _props.size(); }

/**
 * Returns the property at the given index.
 */
public Property getProperty(int anIndex)  { return _props.get(anIndex); }

/**
 * Returns the list of properties.
 */
public List <Property> getProperties()  { return _props; }

/**
 * Sets a list of properties.
 */
public void setProperties(List <Property> theProps)
{
    while(getPropertyCount()>0) removeProperty(0);
    for(Property prop : theProps) addProperty(prop);
}

/**
 * Adds a given property.
 */
public void addProperty(Property aProp)
{
    String name = aProp.getName();
    Property duplicate = getProperty(name);
    int index = duplicate==null? getPropertyCount() : removeProperty(duplicate);
    if(name.equalsIgnoreCase("id")) index = 0;
    addProperty(aProp, index);
}

/**
 * Adds a given property at given index.
 */
public void addProperty(Property aProp, int anIndex)
{
    // Add property to list
    _props.add(anIndex, aProp);
    aProp.setEntity(this);  // Set Property.Entity to this
    _attrs = _attrsSorted = _relations = _relationsSorted = _primaries = null;  // Reset cached lists
}

/**
 * Adds given properties.
 */
public void addProperty(Property ... theProps)  { for(Property p : theProps) addProperty(p); }

/**
 * Removes a property at given index.
 */
public Object removeProperty(int anIndex)
{
    // Remove property from list
    Property prop = _props.remove(anIndex);
    _attrs = _attrsSorted = _relations = _relationsSorted = _primaries = null;  // Reset cached lists
    return prop;
}

/**
 * Removes the given property.
 */
public int removeProperty(Property aProp)
{
    int index = ListUtils.indexOfId(_props, aProp);
    if(index>=0) removeProperty(index);
    return index;
}

/**
 * Returns the property with the given name.
 */
public Property getProperty(String aName)
{
    // Get name (if it has prefix of a standard accessor, strip is/get)
    String name = aName; if(name==null || name.length()==0) return null;
    if(name.startsWith("is") && name.length()>2 && Character.isUpperCase(name.charAt(2))) name = name.substring(2);
    if(name.startsWith("get") && name.length()>3 && Character.isUpperCase(name.charAt(3))) name = name.substring(3);
    
    // Iterate over properties and return the first that matches given name
    for(Property property : getProperties())
        if(name.equalsIgnoreCase(property.getStandardName()))
            return property;
    return null;  // Return null since not found
}

/**
 * Returns the number of attributes.
 */
public int getAttributeCount()  { return getAttributes().size(); }

/**
 * Returns the attribute at the given index.
 */
public Property getAttribute(int anIndex)  { return getAttributes().get(anIndex); }

/**
 * Returns the list of attributes.
 */
public List <Property> getAttributes()
{
    // If already set, just return
    if(_attrs!=null) return _attrs;

    // Create and return
    List <Property> attrs = new ArrayList();
    for(int i=0, iMax=getPropertyCount(); i<iMax; i++) if(getProperty(i).isAttribute()) attrs.add(getProperty(i));
    return _attrs = attrs;
}

/**
 * Returns the attribute with the given name.
 */
public Property getAttribute(String aName)
{
    // Iterate over attributes and return first attribute with given name
    for(Property property : getAttributes())
        if(property.getName().equalsIgnoreCase(aName))
            return property;
    return null; // Return null since not found
}

/**
 * Returns the number of relations in the entity.
 */
public int getRelationCount()  { return getRelations().size(); }

/**
 * Returns the relation at the given index.
 */
public Property getRelation(int anIndex)  { return getRelations().get(anIndex); }

/**
 * Returns the list of relations in the entity.
 */
public List <Property> getRelations()
{
    // If already set, just return
    if(_relations!=null) return _relations;

    // Create and return
    List <Property> rels = new ArrayList();
    for(Property property : getProperties()) if(property.isRelation()) rels.add(property);
    return _relations = rels;
}

/**
 * Returns the relation for the given key path.
 */
public Property getRelation(String aName)
{
    Property prop = getProperty(aName);
    return prop!=null && prop.isRelation()? prop : null;
}

/**
 * Returns the attribute at the given index in a sorted attributes list.
 */
public Property getAttributeSorted(int anIndex)  { return getAttributesSorted().get(anIndex); }

/**
 * Returns the list of attributes sorted.
 */
private List <Property> getAttributesSorted()
{
    if(_attrsSorted!=null) return _attrsSorted;
    Collections.sort(_attrsSorted = new ArrayList(getAttributes()));
    return _attrsSorted;
}

/**
 * Returns the relation at the given index in the sorted list of relations.
 */
public Property getRelationSorted(int anIndex)  { return getRelationsSorted().get(anIndex); }

/**
 * Returns the list of relations sorted.
 */
public List <Property> getRelationsSorted()
{
    if(_relationsSorted!=null) return _relationsSorted;
    Collections.sort(_relationsSorted = new ArrayList(_relations));
    return _relationsSorted;
}

/**
 * Returns the primary key property.
 */
public Property getPrimary()  { List <Property> p = getPrimaries(); return p.size()>0? p.get(0) : null; }

/**
 * Returns the list of primary attributes for this entity.
 */
public List <Property> getPrimaries()
{
    // If already set, just return
    if(_primaries!=null) return _primaries;
    
    // Create and return
    List <Property> primes = new ArrayList();
    for(Property prop : getProperties()) if(prop.isPrimary()) primes.add(prop);
    return _primaries = primes;
}

/**
 * Returns the class that this entity represents.
 */
public Class getEntityClass()  { return _class; }

/**
 * Sets the class that this entity represents.
 */
public void setEntityClass(Class aClass)  { _class = aClass; }

/**
 * Returns the property with the given name.
 */
public Property getKeyPathProperty(String aKeyPath)
{
    String pnames[] = aKeyPath!=null? aKeyPath.split("\\.") : new String[0]; Property prop = null;
    for(String pname : pnames) {
        Entity entity = prop!=null? prop.getRelationEntity() : this; if(entity==null) return null;
        prop = entity.getProperty(pname); if(prop==null) break; }
    return prop;
}

/**
 * Standard equals method.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other entity
    if(anObj==this) return true;
    Entity other = anObj instanceof Entity? (Entity)anObj : null; if(other==null) return false;
    
    // Check Name, Properties
    if(!SnapUtils.equals(other._name, _name)) return false;
    if(!SnapUtils.equals(other._props, _props)) return false;
    return true;  // Return true since all checks passed
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get element named entity
    XMLElement e = new XMLElement("entity");
    
    // Archive Name and Properties
    if(_name!=null && _name.length()>0) e.add("name", _name);
    for(int i=0, iMax=getPropertyCount(); i<iMax; i++)
        e.add(getProperty(i).toXML(anArchiver));
        
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Entity fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Name and Properties
    _name = anElement.getAttributeValue("name", anElement.getName());
    for(int i=0, iMax=anElement.size(); i<iMax; i++)
        addProperty(new Property().fromXML(anArchiver, anElement.get(i)));
    
    // Return this entity
    return this;
}

/**
 * Returns a string representation of entity (its name).
 */
public String toString()  { return getName(); }

}