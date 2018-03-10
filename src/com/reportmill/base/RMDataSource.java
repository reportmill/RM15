/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;
import snap.web.*;

/**
 * This class is used as a proxy for objects passed to document's generateReport() method. It provides schema
 * information of the object graph (in the form of Schema, Entity and Property objects) and it provides sample data
 * (mostly in the form of Java Collections and core types: List, Map, String, Number, Date).
 */
public class RMDataSource implements XMLArchiver.Archivable {
    
    // The source URL of the data
    WebURL        _url;
    
    // The datasource schema (a list of database entities)
    Schema        _schema;
    
    // A dataset represented by this datasource
    Map           _dataset;
    
    // Whether this datasource has a customized schema
    boolean       _customSchema;

    // The URL of the document this DataSource belongs to
    WebURL        _docURL;

/**
 * Creates a plain datasource.
 */
public RMDataSource()  { }

/**
 * Creates a datasource from a given source with a given name (can be null).
 */
public RMDataSource(WebURL aURL)  { _url = aURL; }

/**
 * Returns the URL for this data source.
 */
public WebURL getURL()  { return _url; }

/**
 * Returns the name for this data source.
 */
public String getName()  { return getURL()!=null? getURL().getPathNameSimple() : null; }

/**
 * Returns the schema of represented datasource as a hierarchy of Entity and Property objects.
 */
public Schema getSchema()  { getDataset(); return _schema; }

/**
 * Returns a sample dataset of objects associated with the datasource.
 */
public Map getDataset()
{
    // If already set, just return
    if(_dataset!=null || getURL()==null) return _dataset;

    // Get source file
    WebURL url = getURL();
    WebFile file = url.getFile();
    
    // If file not found, see if it is in same directory as Document.SourceURL
    if(file==null && _docURL!=null) {
        WebFile dfile = _docURL.getFile(), dir = dfile.getParent();
        file = dir.getFile(url.getPathName());
        if(file==null) file = dir.getFile("Dataset.xml");
        if(file!=null) _url = file.getURL();
    }
    
    // Get bytes (if null, set stuff and bail)
    if(file!=null) file.reload();
    byte bytes[] = file!=null? file.getBytes() : null;
    if(bytes==null) {
        _schema = new Schema("root"); _schema.addEntity(new Entity("root"));
        return _dataset = new HashMap();
    }
    
    // Create XML reader and read dataset
    RMXMLReader reader = new RMXMLReader();
    try { _dataset = reader.readObject(bytes, _customSchema? _schema : null); }
    catch(Throwable e) { throw new RuntimeException(e); }
    
    // If schema is null, set to reader schema
    if(!_customSchema)
        _schema = reader.getSchema();        
    
    // Return dataset
    return _dataset;
}

/**
 * Returns a schema that may differ from the one stored in an XML file.
 */
public boolean getCustomSchema()  { return _customSchema; }

/**
 * Sets a schema that may differ from the one stored in an XML file.
 */
public void setCustomSchema(boolean customSchema)  { _customSchema = customSchema; _dataset = null; }

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get xml for basic datasource attributes
    XMLElement e = new XMLElement("datasource");
    
    // Archive SourceURL
    String surl = getURL()!=null? getURL().getString() : null;
    if(surl!=null && surl.endsWith("HollywoodDB.xml")) surl = "Jar:/com/reportmill/examples/HollywoodDB.xml";
    if(surl!=null) e.add("source", surl);
        
    // Archive schema if customSchema
    if(_customSchema) {
        XMLElement schema = _schema.toXML(anArchiver);
        schema.setName("custom-schema");
        e.add(schema);
    }

    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public RMDataSource fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive URL for xml file
    String urls = anElement.getAttributeValue("source");
    _url = WebURL.getURL(urls);

    // If custom schema element present, unarchive schema
    XMLElement schema = anElement.get("custom-schema");
    if(schema!=null) {
        _customSchema = true;
        _schema = new Schema().fromXML(anArchiver, schema);
    }
    
    // Cache document archiver SourceURL and return this datasource
    _docURL = anArchiver.getSourceURL();
    return this;
}

/**
 * Returns a string representation of the datasource (just its name).
 */
public String toString()  { return super.toString() + " " + getName(); }

}