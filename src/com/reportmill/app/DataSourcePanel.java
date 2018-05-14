/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.base.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.view.*;

/**
 * This class manages UI display and editing of editor's datasource.
 */
public class DataSourcePanel extends RMEditorPane.SupportPane {
    
    // The index of the selected entity in the current datasource
    int        _selectedEntityIndex = -1;
    
    // The index of the selected property in the current datasource
    int        _selectedPropertyIndex = -1;
    
    // Data types
    String     _types[] = { "String", "Number", "Boolean", "Date", "DateTime", "Binary",
                                "To-One Relation", "To-Many Relation" };
/**
 * Creates a new DataSourcePanel.
 */
public DataSourcePanel(RMEditorPane anEP)  { super(anEP); }

/**
 * Initialize UI panel for the datasource inspector.
 */
protected void initUI()
{
    // Set TypeComboBox model
    setViewItems("TypeComboBox", _types);
}

/**
 * Resets UI from the current editor's datasource.
 */
public void resetUI()
{
    // Get selected datasource (just return if null)
    RMDataSource ds = getDataSource(); if(ds==null) return;
    
    // Update DSLabel
    setViewValue("DSLabel", ds.getName());
    
    // Update EntitiesList items and selected index
    Schema schema = getSchema();
    setViewItems("EntitiesList", schema!=null? schema.getEntities() : null);
    setViewSelIndex("EntitiesList", _selectedEntityIndex);
    
    // Update PropertiesList items and selected index
    Entity entity = getSelEntity();
    setViewItems("PropertiesList", entity!=null? entity.getProperties() : null);
    setViewSelIndex("PropertiesList", _selectedPropertyIndex);
    
    // Make property panel invisible/visible
    Property prop = getSelProperty();
    setViewSelIndex("CardPanel", prop==null? 0 : 1);

    // Update TypeComboBox, SubtypeComboBox, PrimaryCheckBox
    if(prop!=null) {
        setViewValue("TypeComboBox", prettyType(prop.getType()));
        setViewValue("SubtypeComboBox", prop.getRelationEntityName());
        setViewValue("PrimaryCheckBox", prop.isPrimary());
    }
    
    // Update SubtypeComboBox items and selected item
    List <String> subtypes = new ArrayList(); subtypes.add("None");
    if(schema!=null) for(Entity e : schema.getEntities()) subtypes.add(e.getName());
    setViewItems("SubtypeComboBox", subtypes);
    setViewSelItem("SubtypeComboBox", prop!=null? prop.getRelationEntityName() : null);
}

/**
 * Responds to changes from the datasource inspector panel's controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get selected datasource
    RMDataSource ds = getDataSource();

    // Handle EntitiesList: Reset SelectedEntityIndex and SelectedPropertyIndex
    if(anEvent.equals("EntitiesList")) {
        _selectedEntityIndex = anEvent.getSelIndex(); _selectedPropertyIndex = -1; }
    
    // Handle PropertiesList, TypeComboBox, SubtypeComboBox
    if(anEvent.equals("PropertiesList")) _selectedPropertyIndex = anEvent.getSelIndex();
    if(anEvent.equals("TypeComboBox")) {
        getSelProperty().setType(unprettyType(anEvent.getStringValue())); ds.setCustomSchema(true); }
    if(anEvent.equals("SubtypeComboBox")) {
        getSelProperty().setRelationEntityName(anEvent.getStringValue()); ds.setCustomSchema(true); }
    
    // Handle PrimaryCheckBox
    if(anEvent.equals("PrimaryCheckBox")) {
        getSelProperty().setPrimary(anEvent.getBooleanValue()); ds.setCustomSchema(true); }
}

/**
 * Returns the datasource for the editor document.
 */
private RMDataSource getDataSource()
{
    RMDocument doc = getEditor().getDoc();
    return doc!=null? doc.getDataSource() : null;
}

/**
 * Returns the schema for the datasource.
 */
private Schema getSchema()
{
    RMDataSource dsrc = getDataSource();
    return dsrc!=null? dsrc.getSchema() : null;
}

/**
 * Returns the selected entity of the current datasource.
 */
private Entity getSelEntity()
{
    // Get schema (if null, return null)
    Schema schema = getSchema(); if(schema==null) return null;
    
    // If selected entity index is greater than schema entity count, reset
    if(_selectedEntityIndex>=schema.getEntityCount())
        _selectedEntityIndex = schema.getEntityCount() - 1;
    
    // If selected entity index less than zero, return null
    if(_selectedEntityIndex<0)
        return null;
    
    // Return selected entity
    return schema.getEntity(_selectedEntityIndex);
}

/**
 * Returns the selected property as indicated by the PropertiesList.
 */
private Property getSelProperty()
{
    // Get entity (if null, return null)
    Entity entity = getSelEntity(); if(entity==null) return null;
    
    // If selected property index is greater than entity property count, reset
    if(_selectedPropertyIndex>=entity.getPropertyCount())
        _selectedPropertyIndex = entity.getPropertyCount() - 1;
    
    // If selected property index is less than zero, return null
    if(_selectedPropertyIndex<0)
        return null;
    
    // Return entity property
    return entity.getProperty(_selectedPropertyIndex);
}

/**
 * Returns the name to be used in the inspector panel window title.
 */
public String getWindowTitle()  { return "Datasource Inspector"; }

/**
 * Returns the "user-friendly" name for the given property type name.
 */
private String prettyType(Property.Type aType)
{
    if(aType==null) return "None";
    if(aType==Property.Type.Relation) return "To-One Relation";
    if(aType==Property.Type.RelationList) return "To-Many Relation";
    return aType.toString();
}

/**
 * Returns the internal name for the user-friendly property name.
 */
private Property.Type unprettyType(String aType)
{
    if(aType==null || aType.length()==0) return null;
    if(aType.equals("To-One Relation")) return Property.Type.Relation;
    if(aType.equals("To-Many Relation")) return Property.Type.RelationList;
    return Property.Type.valueOf(aType);
}

}