/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.lang.reflect.*;
import java.util.*;
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
    
    // The schema currently being configure
    Schema                 _schema;

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
    _schema = new Schema(sname);
    
    // Clear processed list, set DepthLimit
    _processed.clear(); _depthLimit = aDepthLimit;
    
    // Configure entity for object and return schema
    getEntity(anObject, null, aName, 0);
    return _schema;
}

/**
 * This method returns an Entity with properties by iterating over given object methods, fields, or (Map) keys.
 * It recurses into method return values to given depth.
 */
private Entity getEntity(Object anObj, Class aClass, String aKey, int aDepth)
{
    // If object is null, just return
    if(anObj==null) return null;
    
    // If depth exceeds DepthLimit, just return null
    if(aDepth>getDepthLimit())
        return null;

    // Get local reference to object, potentially converted from non-standard type (eg, WebObjects/NSArray)
    Object obj = ReportMill.convertFromAppServerType(anObj);
    
    // If object is core type, just return (should only happen if given a list of core types)
    if(obj==null || obj instanceof String || obj instanceof StringBuffer || obj instanceof Number ||
        obj instanceof Date || obj instanceof Boolean || obj instanceof Character ||
        obj instanceof byte[] || obj instanceof Enum)
        return null;
    
    // If object has already been processed, return, otherwise, add to list
    for(int index = ListUtils.indexOfId(_processed, anObj); index>=0;)
        return _processedEntities.get(index);
    
    // Handle Maps special
    if(obj instanceof Map)
        return getEntityForMap((Map)obj, aKey, aDepth);
    
    // Get object class and class name
    Class objClass = aClass!=null? aClass : obj.getClass();
    String className = _schema.getEntityCount()==0? aKey : objClass.getSimpleName();

    // Get entity for class name and class
    Entity entity = getEntityForNameAndClass(className, objClass);

    // Handle fields
    if(getIncludeFields()) {
        
        // Iterate over fields and load into map (if field should be ignored, just continue)
        Field fields[] = objClass.getFields();
        for(Field field : fields) { if(!isValidField(field, objClass)) continue;
            
            // Get field value and add and/or configure property
            Object val = null; try { val = field.get(anObj); } catch(Exception e) { }
            getProperty(val, field.getType(), field.getName(), aDepth, entity);
        }    
    }
    
    // Iterate over methods and load into map
    Method methods[] = objClass.getMethods();
    for(Method meth : methods) {
    
        // If invalid method, just continue
        if(!isValidMethod(meth, objClass))
            continue;
        
        // Get method return value
        Object rval = null; try { rval = meth.invoke(anObj); }
        catch(Exception e) { }
        
        // Get property name and add and/or configure property
        String mname = meth.getName();
        String pname = getUseGetAndIsMethodsOnly()? RMKey.getStandard(mname) : mname;
        getProperty(rval, meth.getReturnType(), pname, aDepth, entity);
    }
    
    // Add object/entity to processed lists and return entity
    _processed.add(anObj); _processedEntities.add(entity);
    return entity;
}

/**
 * This is a special version of getEntity for Maps. It's much simpler - just iterates over keys.
 */
private Entity getEntityForMap(Map aMap, String aKey, int aDepth)
{
    // Get entity for map key (if absent, create and add to schema)
    Entity entity = _schema.getEntity(aKey);
    if(entity==null) {
        _schema.addEntity(entity = new Entity(aKey));
        entity.setEntityClass(Map.class);
    }

    // Iterate over keys and add and/or configure property for each key
    for(Map.Entry entry : (Set<Map.Entry>)aMap.entrySet()) { Object key = entry.getKey();
        if(key instanceof String && isValidIdentifier((String)key))
            getProperty(entry.getValue(), null, (String)key, aDepth+1, entity);
    }

    // Add object/entity to processed lists and return entity
    _processed.add(aMap); _processedEntities.add(entity);
    return entity;
}

/**
 * Returns a Schema Entity for given name and class.
 * It currently supports entity sub-classes (e.g., Vehicle->Car, Vehicle->Truck) by folding them into the common
 * super-class (e.g., Vehicle).
 */
protected Entity getEntityForNameAndClass(String aName, Class aClass)
{
    // If there is already an entity for given name, return it
    Entity entity = _schema.getEntity(aName);
    if(entity!=null)
        return entity;

    // If entity not found, iterate over entities to see if superclass entity is available
    for(int i=0, iMax=_schema.getEntityCount(); i<iMax; i++) {
    
        // Get loop entity and class
        Entity ent2 = _schema.getEntity(i);
        Class eclass = ent2.getEntityClass();
    
        // If existing entity is superclass of Obj.Class, use it instead
        if(eclass.isAssignableFrom(aClass))
            return ent2;
            
        // If Obj.Class is superclass of existing entity, rename existing entity to new common super-class and use it
        else if(aClass.isAssignableFrom(eclass)) {
            renameEntity(ent2, aName); //ent2.setName(aName);
            ent2.setEntityClass(aClass);
            return ent2;
        }
    }
    
    // If entity still not found, create and add to schema
    _schema.addEntity(entity = new Entity(aName));
    entity.setEntityClass(aClass);
    return entity;
}

/**
 * Resets an Entity name for Schema for case when substituting a common superclass for a previously encountered
 * subclass. Resets the name everywhere (including entity property relation names).
 */
private void renameEntity(Entity anEntity, String aName)
{
    // Reset name in given entity
    String oldName = anEntity.getName();
    anEntity.setName(aName);
    
    // Iterate over all entity properties and reset any relation properties that pointed to old name
    for(Entity entity : _schema.getEntities()) {
        for(Property prop : entity.getProperties()) {
            String rname = prop.getRelationEntityName();
            if(rname!=null && rname.equals(oldName))
                prop.setRelationEntityName(aName);
        }
    }
}

/**
 * Configures a property for a property value.
 */
protected void getProperty(Object aValue, Class aClass, String aKey, int aDepth, Entity anEntity)
{
    // Get local reference to value, potentially converted from non-standard type (eg, WebObjects/NSArray)
    Object value = ReportMill.convertFromAppServerType(aValue);
    
    // If value is null or Array (of non-byte) just return (can't get info from null, can't 
    if(value==null || (value.getClass().isArray() && !(value instanceof byte[])))
        return;
    
    // Get property from entity (if absent, create and add)
    Property prop = anEntity.getProperty(aKey);
    if(prop==null)
        anEntity.addProperty(prop = new Property(aKey));
    
    // Handle String, Number, DateTime, Boolean, Binary
    if(value instanceof String || value instanceof StringBuffer) prop.setType(Property.Type.String);
    else if(value instanceof Number) prop.setType(Property.Type.Number);
    else if(value instanceof Date) prop.setType(Property.Type.Date);
    else if(value instanceof Boolean) prop.setType(Property.Type.Boolean);
    else if(value instanceof byte[]) prop.setType(Property.Type.Binary);
    else if(value instanceof Enum) prop.setType(Property.Type.Enum);
    
    // Handle relations
    else {
        
        // Get relation key
        String relKey = anEntity.getName() + '.' + prop.getName();
    
        // Handle List (RelationList)
        if(value instanceof List) { List list = (List)value;
        
            // Set type to List
            prop.setType(Property.Type.RelationList);
            
            // Iterate over significant number of items to ensure we get right relation class
            for(int j=0,jMax=Math.min(list.size(),getBreadthLimit()); j<jMax; j++) { Object item = list.get(j);
                Entity entity = getEntity(item, null, relKey, aDepth+1);
                if(entity!=null)
                    prop.setRelationEntityName(entity.getName());
            }
        }
        
        // Handle POJO/Map (Relation - everything not covered above)
        else {
            
            // Set type to Relation and recurse to check entity relation class
            prop.setType(Property.Type.Relation);
            Entity entity = getEntity(value, null, relKey, aDepth+1);
            if(entity!=null)
                prop.setRelationEntityName(entity.getName());
        }
    }
}

/**
 * Returns whether given method is valid method.
 */
protected boolean isValidMethod(Method aMeth, Class aClass)
{
    // If method has no return value or any parameters, return false
    if(aMeth.getReturnType()==void.class || aMeth.getParameterTypes().length>0)
        return false;
        
    // If method is non-public or is static return false
    int mods = aMeth.getModifiers();
    if(!Modifier.isPublic(mods) || Modifier.isStatic(mods))
        return false;
        
    // If method doesn't conform to UseGetAndIsMethodsOnly
    String methodName = aMeth.getName();
    if(getUseGetAndIsMethodsOnly() && !methodName.startsWith("get") && !methodName.startsWith("is"))
        return false;
        
    // If method should be ignored, return false
    if(ignoreMember(methodName))
        return false;
    if(ignoreMember(aClass.getName(), methodName))
        return false;
    if(ignoreClass(aMeth.getReturnType()))
        return false;
        
    // Return true since all checks passed
    return true;
}

/**
 * Returns whether given method is valid method.
 */
protected boolean isValidField(Field aField, Class aClass)
{
    // If field is non-public or is static return false
    int mods = aField.getModifiers();
    if(!Modifier.isPublic(mods) || Modifier.isStatic(mods))
        return false;
        
    // If field should be ignored, just continue
    String fname = aField.getName();
    if(ignoreMember(fname))
        return false;
    if(ignoreMember(aClass.getName(), fname))
        return false;
    if(ignoreClass(aField.getType()))
        return false;
    
    // Return true since all checks passed
    return true;
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