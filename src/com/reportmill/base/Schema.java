/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This class describes the structure of a data source by managing a list of entities.
 */
public class Schema implements XMLArchiver.Archivable {

    // The schema name
    String           _name;

    // The list of entities
    List <Entity>    _entities = new ArrayList();
    
    // The root entity - one that usually just contains relations for the other entities
    Entity           _rootEntity;
    
/**
 * Creates a new empty schema.
 */
public Schema() { }

/**
 * Creates a new schema with the given name.
 */
public Schema(String aName)  { setName(aName); }

/**
 * Returns the name of the entity.
 */
public String getName()  { return _name; }

/**
 * Sets the name of the entity.
 */
public void setName(String aName)  { _name = aName; }

/**
 * Returns the number of entities in this schema.
 */
public int getEntityCount()  { return getEntities().size(); }

/**
 * Returns the specific entity at the given index.
 */
public Entity getEntity(int anIndex)  { return getEntities().get(anIndex); }

/**
 * Returns the list of entities.
 */
public List <Entity> getEntities()  { return _entities; }

/**
 * Adds an entity to the list.
 */
public void addEntity(Entity anEntity)
{
    if(ListUtils.containsId(getEntities(), anEntity)) return;
    addEntity(anEntity, getEntityCount());
}
    
/**
 * Adds an entity to the list.
 */
public void addEntity(Entity anEntity, int anIndex)
{
    // Add entity, set Entity.Schema and clear RootEntity
    getEntities().add(anIndex, anEntity);
    anEntity.setSchema(this); _rootEntity = null;
}

/**
 * Removes an entity at given index.
 */
public Entity removeEntity(int anIndex)
{
    // Remove entity and clear RootEntity
    Entity entity = getEntities().remove(anIndex);
    _rootEntity = null;
    return entity;
}

/**
 * Removes an entity from the list.
 */
public int removeEntity(Entity anEntity)
{
    int index = ListUtils.indexOfId(getEntities(), anEntity);
    if(index>=0) removeEntity(index);
    return index;
}

/**
 * Returns the schema entity with the given name.
 */
public Entity getEntity(String aName)
{
    // Iterate over entities and return the first that matches given name
    for(Entity entity : getEntities())
        if(entity.getName().equalsIgnoreCase(aName))
            return entity;
    
    // Return null
    return null;
}

/**
 * Returns the root entity.
 */
public Entity getRootEntity()
{
    // If root entity not present, see if schema has root entity otherwise create it
    if(_rootEntity==null) {
        _rootEntity = getEntity(getName());
        if(_rootEntity==null)
            _rootEntity = createRootEntity();
    }
    
    // Return root entity
    return _rootEntity;
}

/**
 * Creates a root entity.
 */
protected Entity createRootEntity()
{
    // Create new entity with schema name
    Entity rootEntity = new Entity(); rootEntity.setName(getName()!=null? getName() : "RootEntity");
    rootEntity.setSchema(this);
    
    // Iterate over entities
    for(Entity entity : getEntities()) {
        Property property = new Property(entity.getName(), Property.Type.RelationList);
        property.setRelationEntity(entity);
        rootEntity.addProperty(property);
    }

    // Return root entity
    return rootEntity;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other schema
    if(anObj==this) return true;
    Schema other = anObj instanceof Schema? (Schema)anObj : null; if(other==null) return false;
    
    // Check Name, Entities
    if(!SnapUtils.equals(other._name, _name)) return false;
    if(!other._entities.equals(_entities)) return false;
    return true;  // Return true since all checks passed
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get element named entity
    XMLElement e = new XMLElement("RMSchema");
    
    // Archive Name and Entities
    if(getName()!=null && getName().length()>0) e.add("name", getName());
    for(int i=0, iMax=getEntityCount(); i<iMax; i++)
        e.add(getEntity(i).toXML(anArchiver));
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Schema fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Name
    if(anElement.hasAttribute("name")) setName(anElement.getAttributeValue("name"));
    
    // If schema has properties in it, load root entity (Compatibility fix for custom schemas)
    if(anElement.get("property")!=null) {
        
        // Create and add new root entity to hold root level properties
        Entity rootEntity = new Entity(getName());
        addEntity(rootEntity);
        
        // Iterate over property element and add properties (and remove property elements)
        for(int i=anElement.indexOf("property"); i>=0; i=anElement.indexOf("property", i)) {
            XMLElement pxml = anElement.removeElement(i);
            Property property = new Property().fromXML(anArchiver, pxml);
            rootEntity.addProperty(property);
        }
    }
    
    // Unarchive entities
    for(XMLElement element : anElement.getElements())
        addEntity(new Entity().fromXML(anArchiver, element));

    // Return this schema
    return this;
}

/**
 * Returns schema name.
 */
public String toString()  { return getName(); }

}