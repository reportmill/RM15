/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.text.SimpleDateFormat;
import java.util.*;
import snap.util.*;

/**
 * This class describes an attribute of an entity.
 */
public class Property implements Comparable, XMLArchiver.Archivable {

    // The entity that owns this property
    Entity         _entity;
    
    // The name of this property
    String         _name;
    
    // The property type: String, Number, Date, Map (to-one relation), List (to-many relation)
    Type           _type = Type.String;
    
    // Whether this property is a primary key
    boolean        _primary;
    
    // Whether this property is private
    boolean        _private;
    
    // The number type when type is Number
    NumberType     _numberType = getNumberTypeDefault();
    
    // The entity that describes the relation object(s)
    Entity         _relEntity;
    
    // The entity name that describes the relation object(s)
    String         _relEntityName;
    
    // Constants for property types
    public enum Type { String, Number, Date, Boolean, Enum, Binary, Relation, RelationList, Other }
    
    // Constants for number types
    public enum NumberType { Byte, Short, Integer, Long, Float, Double, Decimal }
    
    // Constants for date types
    public enum DateType { DateOnly, DateTime }

/**
 * Creates a new property.
 */
public Property()  { }

/**
 * Creates a new property with given name.
 */
public Property(String aName)  { setName(aName); }

/**
 * Creates a new property with given name and type.
 */
public Property(String aName, Type aType)  { setName(aName); setType(aType); }

/**
 * Creates a new property with given name and type.
 */
public Property(String aName, Object aType)
{
    setName(aName);
    if(aType instanceof Type) setType((Type)aType);
    else if(aType instanceof NumberType) setNumberType((NumberType)aType);
}

/**
 * Returns the entity that owns this property.
 */
public Entity getEntity()  { return _entity; }

/**
 * Sets the entity that owns this property.
 */
public void setEntity(Entity anEntity)  { _entity = anEntity; }

/**
 * Returns the name of this property.
 */
public String getName()  { return _name; }

/**
 * Sets the name of this property.
 */
public void setName(String aName)
{
    String name = aName!=null? aName.trim().replace(" ", "") : null;
    _name = name;
}

/**
 * Returns the name in a standard format (strip is/get prefix and start with capital letter).
 */
public String getStandardName()  { return Key.getStandard(getName()); }

/**
 * Returns the type of this property.
 */
public Type getType()  { return _type; }

/**
 * Sets the type of this property.
 */
public void setType(Type aType)  { _type = aType; }

/**
 * Sets the type from a given name.
 */
public void setTypeName(String aName)
{
    // Get name ensuring first character is upper case
    String type = StringUtils.firstCharUpperCase(aName);
    
    // Do some legacy conversions
    if(type.equals("Map")) type = Type.Relation.toString();
    else if(type.equals("DateTime")) type = Type.Date.toString();
    else if(type.equals("List")) type = Type.RelationList.toString();
    else if(type.equals("Decimal")) type = Type.Number.toString();
    else if(type.equals("Base64Binary")) type = Type.Binary.toString();
    
    // Set type
    setType(Type.valueOf(type));
}

/**
 * Sets property type from sample string - tries to discern whether string represents a date or number.
 */
public void setTypeFromSample(String aSample)
{
    // Handle null, empty string or Type already String
    if(aSample==null || aSample.length()==0 || getType()==Type.String) return;
    
    // If type still assumed Date, try two common date formats and return if either work, otherwise change to Number
    if(getType()==Type.Date) {
        try { new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(aSample); return; }
        catch(Exception e) { }
        try { new SimpleDateFormat("yyyy-MM-dd").parse(aSample); return; }
        catch(Exception e) { setType(Type.Number); }
    }
    
    // If type still assumed Number, try common number format and return if it works, otherwise change to String
    if(getType()==Type.Number) {
        try { Float.parseFloat(aSample); return; }
        catch(Exception e) { setType(Type.String); }
    }
}

/**
 * Returns whether this property is a primary key.
 */
public boolean isPrimary()  { return _primary; }

/**
 * Sets whether this property is a primary key.
 */
public void setPrimary(boolean isPrimary) { _primary = isPrimary; }

/**
 * Returns whether this property is private.
 */
public boolean isPrivate()  { return _private; }

/**
 * Sets whether this property is private.
 */
public void setPrivate(boolean isPrivate)  { _private = isPrivate; }

/**
 * Returns the number type.
 */
public NumberType getNumberType()  { return _numberType; }

/**
 * Sets the number type.
 */
public void setNumberType(NumberType aNumberType)
{
    if(aNumberType==_numberType) return;
    setType(Type.Number);  // Ensure type is number
    _numberType = aNumberType;
}

/**
 * Return default number type.
 */
public NumberType getNumberTypeDefault()  { return NumberType.Double; }

/**
 * Returns whether this property is a simple attribute.
 */
public boolean isAttribute()  { return getType()!=Type.Relation && getType()!=Type.RelationList; }

/**
 * Returns whether this property is a relation.
 */
public boolean isRelation()  { return getType()==Type.Relation || getType()==Type.RelationList; }

/**
 * Returns whether this property is a to many relation.
 */
public boolean isToMany()  { return getType()==Type.RelationList; }

/**
 * Sets whether this property is a to many relation.
 */
public void setToMany(boolean aValue)  { setType(aValue? Type.RelationList : Type.Relation); }

/**
 * Returns the relation entity.
 */
public Entity getRelationEntity()
{
    if(_relEntity!=null) return _relEntity;
    Entity ent = getEntity();
    String name = getRelationEntityName();
    Entity relEntity = ent!=null && name!=null? ent.getEntity(name) : null;
    return _relEntity = relEntity;
}

/**
 * Sets the relation entity.
 */
public void setRelationEntity(Entity anEntity)
{
    _relEntity = anEntity;
    if(_relEntity!=null) setRelationEntityName(_relEntity.getName());
}

/**
 * Returns the name of the entity that this relation property points to. 
 */
public String getRelationEntityName()  { return _relEntityName; }

/**
 * Sets the name of the entity that this relation property points to. 
 */
public void setRelationEntityName(String aName)  { _relEntityName = aName; }

/**
 * Converts an arbitrary object to property type.
 */
public Object convertValue(Object anObj)
{
    // Do basic conversion
    Object value = DataUtils.convertValue(anObj, getType(), getNumberType());
    
    // Special case for Binary - gets converted to Base64 below in toString()
    if(value==null && getType()==Type.Binary && anObj instanceof String)
        try { value = ASCIICodec.decodeBase64((String)anObj); }
        catch(Exception e) { }
    
    // Return value
    return value;
}

/**
 * Returns a string representation of given value according to property type.
 */
public String toString(Object aValue)
{
    // Get value as property type
    Object value = convertValue(aValue);
    
    // Handle specific property types
    switch(getType()) {
        case Number: return DataUtils.toString((Number)value);
        case Date: return DataUtils.toString((Date)value);
        default: return SnapUtils.stringValue(value);
    }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other property
    if(anObj==this) return true;
    Property other = anObj instanceof Property? (Property)anObj : null; if(other==null) return false;
    
    // Check Name, Type
    if(!SnapUtils.equals(other._name, _name)) return false;
    if(!SnapUtils.equals(other._type, _type)) return false;
    
    // Check Primary, Private
    if(other._primary!=_primary) return false;
    if(other._private!=_private) return false;
    
    // Check NumberType, RelationEntityName
    if(getType()==Type.Number && other._numberType!=_numberType) return false;
    if(!SnapUtils.equals(other._relEntityName, _relEntityName)) return false;
    
    // Return true since all checks passed
    return true;
}

/**
 * Implements comparable to compare based on name.
 */
public int compareTo(Object anObj)
{
    return anObj instanceof Property? getName().compareTo(((Property)anObj).getName()) : 0;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Create new element for property
    XMLElement e = new XMLElement("property");
    
    // Archive Name, Type
    if(_name!=null && _name.length()>0) e.add("name", _name);
    if(getType()!=null) e.add("type", getType());
    
    // Archive Primary, Private
    if(_primary) e.add("primary", true);
    if(_private) e.add("private", true);
    
    // Archive NumberType
    if(getType()==Type.Number && getNumberType()!=null) e.add("number-type", getNumberType());

    // Archive RelationEntityName
    if(getRelationEntityName()!=null && getRelationEntityName().length()>0)
        e.add("RelationEntityName", getRelationEntityName());
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Property fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Name, Type
    _name = anElement.getAttributeValue("name", anElement.getName());
    if(anElement.hasAttribute("type")) setTypeName(anElement.getAttributeValue("type"));
    
    // Unarchive Primary, Private
    _primary = anElement.getAttributeBoolValue("primary");
    _private = anElement.getAttributeBoolValue("private");

    // Unarchive NumberType
    if(anElement.hasAttribute("number-type"))
        setNumberType(NumberType.valueOf(anElement.getAttributeValue("number-type")));
    
    // Unarchive RelationEntityName
    if(anElement.hasAttribute("RelationEntityName"))
        setRelationEntityName(anElement.getAttributeValue("RelationEntityName"));
    
    // Unarchive legacy relation values
    if(anElement.hasAttribute("subtype")) setRelationEntityName(anElement.getAttributeValue("subtype"));
    else if(anElement.hasAttribute("relation-entity-name"))
        setRelationEntityName(anElement.getAttributeValue("relation-entity-name"));
    
    // Return this property
    return this;
}

/**
 * Returns a string representation of this property (its name).
 */
public String toString()  { return getName(); }

}