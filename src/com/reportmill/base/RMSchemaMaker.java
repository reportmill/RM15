/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.lang.reflect.*;
import java.util.*;
import snap.data.*;
import snap.util.*;

/**
 * This class makes a schema from an object graph, given a top level object.
 */
public class RMSchemaMaker {

    // The limit of recursion for relationships
    int                    _depthLimit = 3;
    
    // The maximum number of items to write for lists/array relationships.
    int                    _breadthLimit = 100;
    
    // Whether to only use getX/isX accessor methods (as opposed to any method returning a value)
    boolean                _useGetAndIsMethodsOnly = true;
    
    // Whether to include fields
    boolean                _includeFields = false;
    
    // The set of method return types (or field types) to ignore
    Set <String>           _ignoredClasses = new HashSet();
    
    // The set of method/field names to ignore
    Set <String>           _ignoredMembers = new HashSet();
    
    // Used for pruning more specific Class/member branches
    Map <String,Set>       _ignoredClassMembers = new Hashtable();

    // The list of objects that have already been processed
    List                   _processed = new ArrayList();

    // The list of object entities that have already been processed
    List <Entity>          _processedEntities = new ArrayList();

    // A map of property classes
    Map <Property, Class>  _propertyClasses = new HashMap();
    
/**
 * Creates a new schema maker.
 */
public RMSchemaMaker()
{
    // Initialize ignored Methods/Classes with those we know are useless
    String ignores[] = { "clone", "getClass", "hashCode", "toString" };
    for(String s : ignores) addIgnoreMember(s);
    addIgnoreClass("java.lang.Class");
}

/**
 * Returns the limit of recursion for relationships.
 */
public int getDepthLimit()  { return _depthLimit; }

/**
 * Returns the maximum number of items to write for lists/array relationships.
 */
public int getBreadthLimit()  { return _breadthLimit; }

/**
 * Sets the maximum number of items to write for lists/array relationships.
 */
public void setBreadthLimit(int aLimit)  { _breadthLimit = aLimit; }

/**
 * Returns whether to only use getX/isX accessor methods (as opposed to any method returning a value).
 */
public boolean getUseGetAndIsMethodsOnly()  { return _useGetAndIsMethodsOnly; }

/**
 * Sets whether to only use getX/isX accessor methods (as opposed to any method returning a value).
 */
public void setUseGetAndIsMethodsOnly(boolean aFlag)  { _useGetAndIsMethodsOnly = aFlag; }

/**
 * Returns whether to include fields.
 */
public boolean getIncludeFields()  { return _includeFields; }

/**
 * Sets whether to include fields.
 */
public void setIncludeFields(boolean aFlag)  { _includeFields = aFlag; }

/**
 * Tells schema maker to ignore any members encountered with the given class.
 */
public void addIgnoreClass(Class aClass)  { addIgnoreClass(aClass.getName()); }

/**
 * Tells schema maker to ignore any members encountered with the given class (by class name).
 */
public void addIgnoreClass(String aClassName)  { _ignoredClasses.add(aClassName); }

/**
 * Tells schema maker to ignore any members encountered with the given name.
 */
public void addIgnoreMember(String aName)  { _ignoredMembers.add(aName); }

/**
 * Tells schema maker to ignore members for a particular class.
 */
public void addIgnoreMember(Class aClass, String aMemberName)  { addIgnoreMember(aClass.getName(), aMemberName); }

/**
 * Tells schema maker to ignore members for a particular class name.
 */
public void addIgnoreMember(String aClassName, String aMemberName)
{
    // Get ignored class members set (if not present, create and set) and add member name to set
    Set members = _ignoredClassMembers.get(aClassName);
    if(members==null) _ignoredClassMembers.put(aClassName, members = new HashSet());
    members.add(aMemberName);
}

/**
 * This code returns a schema for a given object graph of Java primitives (Map, List, String, Number, Date).
 */
public Schema getSchema(Object anObject, String aName, int aDepthLimit)
{
    // Get schema name and create new schema
    String sname = aName!=null? aName : "Root";
    Schema schema = new Schema(sname);
    
    // Clear processed list, set DepthLimit
    _processed.clear(); _depthLimit = aDepthLimit;
    
    // Configure entity for object and return schema
    getEntity(anObject, null, aName, 0, schema);
    return schema;
}

/**
 * This is the recursive version of get schema that does the real work.
 * This code currently supports entity subclasses (eg, Vehicle->Car, Vehicle->Truck) by folding them into their base
 * class (eg, Vehicle).
 */
private Entity getEntity(Object anObject, Class aClass, String aKey, int aDepth, Schema aSchema)
{
    // If object is null, just return
    if(anObject==null) return null;
    
    // If depth exceeds DepthLimit, just return null
    if(aDepth>getDepthLimit())
        return null;

    // Get local reference to object, potentially converted from non-standard type (eg, WebObjects/NSArray)
    Object object = ReportMill.convertFromAppServerType(anObject);
    if(object==null)
        return null;
    
    // If object is core type, just return (should only happen if given a list of core types)
    if(object instanceof String || object instanceof StringBuffer || object instanceof Number ||
        object instanceof Date || object instanceof Boolean || object instanceof Character ||
        object instanceof byte[] || object instanceof Enum)
        return null;
    
    // If object has already been processed, return, otherwise, add to list
    for(int index = ListUtils.indexOfId(_processed, anObject); index>=0;)
        return _processedEntities.get(index);
    
    // Handle Maps special
    if(object instanceof Map) { Map map = (Map)object;

        // Get entity for map key (if absent, create and add to schema)
        Entity entity = aSchema.getEntity(aKey);
        if(entity==null) {
            aSchema.addEntity(entity = new Entity(aKey)); entity.setEntityClass(Map.class); }

        // Iterate over keys and add and/or configure property for each key
        for(Map.Entry entry : (Set<Map.Entry>)map.entrySet()) { Object key = entry.getKey();
            if(key instanceof String && isValidIdentifier((String)key))
                getProperty(entry.getValue(), null, (String)key, aDepth+1, entity); }

        // Add object/entity to processed lists and return entity
        _processed.add(anObject); _processedEntities.add(entity);
        return entity;
    }
    
    // Get object class
    Class objClass = aClass!=null? aClass : object.getClass();
    
    // Get standard class name
    String className = aSchema.getEntityCount()==0? aKey : objClass.getSimpleName();

    // If there is an entity for standard class name, return it
    Entity entity = aSchema.getEntity(className);

    // If entity not found, iterate over entities to see if superclass entity is available
    for(int i=0, iMax=aSchema.getEntityCount(); i<iMax && entity==null; i++) { Entity e2 = aSchema.getEntity(i);
        if(e2.getEntityClass().isAssignableFrom(objClass))
            entity = e2;
        else if(objClass.isAssignableFrom(e2.getEntityClass())) {
            e2.setName(className);
            e2.setEntityClass(objClass);
            entity = e2;
        }
    }
    
    // If entity still not found, create and add to schema
    if(entity==null) {
        aSchema.addEntity(entity = new Entity(className));
        entity.setEntityClass(objClass);
    }
    
    // Handle fields
    if(getIncludeFields()) { Field fields[] = objClass.getFields();
        
        // Iterate over fields and load into map
        for(int i=0; i<fields.length; i++) { Field field = fields[i]; String fieldName = field.getName();
            
            // If field should be ignored, just continue
            if(ignoreMember(fieldName) || ignoreMember(objClass.getName(), fieldName) || ignoreClass(field.getType()) ||
                !Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                continue;
            
            // Get field value
            Object obj = null;  try { obj = field.get(anObject); }
            catch(Exception e) { }
            
            // Add and/or configure property
            getProperty(obj, field.getType(), fieldName, aDepth, entity);
        }    
    }
    
    // Get object methods
    Method methods[] = objClass.getMethods();
    
    // Iterate over methods and load into map
    for(int i=0; i<methods.length; i++) { Method method = methods[i];
        
        // Get method name and modifiers
        String methodName = method.getName();
        int modifiers = method.getModifiers();
        
        // Just continue if: (1) method should be ignored or (2) has no return value or (3) is non-public or
        //  (4) is static or (5) arg count not zero or (6) doesn't conform to _useGetAndIsMethodsOnly
        if(ignoreMember(methodName) || ignoreMember(objClass.getName(), methodName) ||
            method.getReturnType()==void.class || ignoreClass(method.getReturnType()) ||
            !Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || method.getParameterTypes().length>0 ||
            (getUseGetAndIsMethodsOnly() && !methodName.startsWith("get") && !methodName.startsWith("is")))
            continue;

        // Get object
        Object obj = null;
        try { obj = method.invoke(anObject, new Object[0]); }
        catch(Exception e) { }
        
        // Get property name
        String pname = getUseGetAndIsMethodsOnly()? RMKey.getStandard(methodName) : methodName;
        if(pname.length()>0 && Character.isDigit(pname.charAt(0)))
            pname = methodName;
        
        // Add and/or configure property
        getProperty(obj, method.getReturnType(), pname, aDepth, entity);
    }
    
    // Add object/entity to processed lists
    _processed.add(anObject); _processedEntities.add(entity);
    
    // Return entity
    return entity;
}

/**
 * Configures a property for a property value.
 */
public void getProperty(Object aValue, Class aClass, String aKey, int aDepth, Entity anEntity)
{
    // Get local reference to value, potentially converted from non-standard type (eg, WebObjects/NSArray)
    Object value = ReportMill.convertFromAppServerType(aValue);
    
    // If value is null or Array (of non-byte) just return (can't get info from null, can't 
    if(value==null || (value.getClass().isArray() && !(value instanceof byte[])))
        return;
    
    // Get property from entity (if absent, create and add)
    Property property = anEntity.getProperty(aKey);
    if(property==null)
        anEntity.addProperty(property = new Property(aKey)); //property.setNumberType(RMProperty.NumberType.Byte);
    
    // Handle String, Number, DateTime, Boolean, Binary
    if(value instanceof String || value instanceof StringBuffer)
        property.setType(Property.Type.String);
    else if(value instanceof Number)
        property.setType(Property.Type.Number); //aProperty.promoteNumberType((Number)aValue);
    else if(value instanceof Date)
        property.setType(Property.Type.Date); //aProperty.promoteDateType((Date)aValue);
    else if(value instanceof Boolean)
        property.setType(Property.Type.Boolean);
    else if(value instanceof byte[])
        property.setType(Property.Type.Binary);
    else if(value instanceof Enum)
        property.setType(Property.Type.Enum);
    
    // Handle relations
    else {
        
        // Get relation key
        String relationKey = anEntity.getName() + '.' + property.getName();
    
        // Handle List (RelationList)
        if(value instanceof List) { List list = (List)value;
            property.setType(Property.Type.RelationList);
            for(int j=0,jMax=Math.min(list.size(),getBreadthLimit()); j<jMax; j++) { Object item = list.get(j);
                Entity entity = getEntity(item, null, relationKey, aDepth+1, anEntity.getSchema());
                if(entity!=null)
                    property.setRelationEntityName(entity.getName());
            }
        }
        
        // Handle POJO/Map (Relation - everything not covered above)
        else {
            property.setType(Property.Type.Relation);
            Entity entity = getEntity(value, null, relationKey, aDepth+1, anEntity.getSchema());
            if(entity!=null)
                property.setRelationEntityName(entity.getName());
        }
    }
}

/**
 * Returns whether schema maker should ignore any member with the given name.
 */
protected boolean ignoreMember(String aName)  { return matches(aName, _ignoredMembers); }

/**
 * Returns whether schema maker should ignore the specific class/member combination.
 */
protected boolean ignoreMember(String aClassName, String aMemberName)
{ 
    Set <String> members = _ignoredClassMembers.get(aClassName); if(members==null) return false;
    return matches(aMemberName, members);
}

/**
 * Returns whether schema maker should ignore any member with the given name.
 */
protected boolean ignoreClass(Class aClass)  { return matches(aClass.getName(), _ignoredClasses); }

/**
 * Returns whether given name matches any string in set, with support for prefix/suffix wildcard char '*'.
 */
private boolean matches(String aName, Set <String> theNames)
{
    // Iterate over set of names
    for(String name : theNames) {
        
        // If starts with wildcard, match against contains or ends with
        if(name.startsWith("*")) {
            if(name.endsWith("*")) { if(aName.contains(name.substring(1, name.length()-1))) return true; }
            else if(name.endsWith(name.substring(1))) return true;
        }
        
        // If ends with wildcard, match with startsWith
        else if(name.endsWith("*")) { if(aName.startsWith(name.substring(0,name.length()-1))) return true; }
        
        // Otherwise match with equals
        else if(name.equals(aName)) return true;
    }
    
    // Return false since didn't match
    return false;
}

/**
 * Creates and returns a schema for a given xml element tree.
 */
public Schema getSchema(XMLElement anElement)
{
    Schema schema = new Schema(anElement.getName());  // Create schema
    getSchema(anElement, schema);                     // Build schema
    return schema;                                    // Return schema
}

/**
 * Builds out schema from the given XML element (recursive).
 */
private void getSchema(XMLElement anElement, Schema aSchema)
{
    // Get element name
    String elementName = anElement.getName();
    
    // Get entity for element name
    Entity entity = aSchema.getEntity(elementName);
    
    // If entity hasn't been encountered yet, create it and add to schema
    if(entity==null)
        aSchema.addEntity(entity = new Entity(elementName));

    // Add attributes to aMap (and make sure they are reflected in entity)
    for(int i=0, iMax=anElement.getAttributeCount(); i<iMax; i++) { XMLAttribute attr = anElement.getAttribute(i);
        
        // Get attribute name and property
        String name = attr.getName();
        Property property = entity.getAttribute(name);
        
        // If property is hasn't been encountered yet, create it
        if(property==null)
            entity.addProperty(property = new Property(name, Property.Type.Date));

        // Set property type from sample string
        property.setTypeFromSample(attr.getValue());
    }
    
    // Iterate over elements - they can either be attributes, relations or tomany-relations
    for(int i=0, iMax=anElement.size(); i<iMax; i++) { XMLElement elmt = anElement.get(i);
        
        // Get element name and property
        String name = elmt.getName();
        Property property = entity.getProperty(name);
        
        // If property hasn't been encountered yet, create it
        if(property==null)
            entity.addProperty(property = new Property(name, Property.Type.Date));

        // If property is currently an attribute, see if it needs to be promoted to relation
        if(property.isAttribute()) {
        
            // If current element indeed has no children or attributes, update property type for element text & continue
            if(elmt.size() + elmt.getAttributeCount()==0) {
                property.setTypeFromSample(elmt.getValue());
                continue;
            }
            
            // Since property has attributes or children, promote to relation
            property.setType(Property.Type.Relation);
            property.setRelationEntityName(name);
        }
        
        // Since property is a relation, see if it needs to be promoted to 'to-many'
        if(!property.isToMany() && anElement.getElementCount(name)>1)
            property.setType(Property.Type.RelationList);
        
        // Recurse into relation element
        getSchema(elmt, aSchema);
    }
}

/**
 * Returns whether key is valid key identifier.
 */
private static boolean isValidIdentifier(String aStr)
{
    if(aStr.length()==0 || !Character.isJavaIdentifierStart(aStr.charAt(0))) return false;
    for(int i=1, iMax=aStr.length(); i<iMax; i++) if(!Character.isJavaIdentifierPart(aStr.charAt(i))) return false;
    return true;
}

}