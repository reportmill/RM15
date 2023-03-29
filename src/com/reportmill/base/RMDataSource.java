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
    private WebURL  _url;

    // The datasource schema (a list of database entities)
    private Schema  _schema;

    // A dataset represented by this datasource
    private Map<String,Object>  _dataset;

    // Whether this datasource has a customized schema
    private boolean  _customSchema;

    // The URL of the document this DataSource belongs to
    private WebURL  _docURL;

    /**
     * Constructor.
     */
    public RMDataSource()  { }

    /**
     * Constructor for given source URL.
     */
    public RMDataSource(WebURL aURL)
    {
        _url = aURL;
    }

    /**
     * Returns the URL for this data source.
     */
    public WebURL getURL()  { return _url; }

    /**
     * Returns the name for this data source.
     */
    public String getName()
    {
        WebURL datasetURL = getURL();
        return datasetURL != null ? datasetURL.getFilenameSimple() : null;
    }

    /**
     * Returns the schema of represented datasource as a hierarchy of Entity and Property objects.
     */
    public Schema getSchema()
    {
        getDataset();
        return _schema;
    }

    /**
     * Returns a sample dataset of objects associated with the datasource.
     */
    public Map<String,Object> getDataset()
    {
        // If already set, just return
        if (_dataset != null) return _dataset;

        // Get dataset file - if not found, return stub
        WebFile datasetFile = getDatasetFile();
        if (datasetFile == null) {
            System.err.println("RMDataSource.getDataset: Dataset file not found for URL: " + getURL());
            _schema = new Schema("root");
            _schema.addEntity(new Entity("root"));
            return _dataset = new HashMap<>();
        }

        // Get bytes (if null, set stuff and bail)
        datasetFile.reload();
        byte[] bytes = datasetFile.getBytes();
        if (bytes == null) {
            _schema = new Schema("root");
            _schema.addEntity(new Entity("root"));
            return _dataset = new HashMap<>();
        }

        // Create XML reader and read dataset
        RMXMLReader reader = new RMXMLReader();
        try { _dataset = reader.readObject(bytes, _customSchema ? _schema : null); }
        catch (Throwable e) { throw new RuntimeException(e); }

        // If schema is null, set to reader schema
        if (!_customSchema)
            _schema = reader.getSchema();

        // Return
        return _dataset;
    }

    /**
     * Returns the dataset file from source URL.
     */
    private WebFile getDatasetFile()
    {
        // Get dataset file URL (just return if null)
        WebURL datasetURL = getURL();
        if (datasetURL == null)
            return null;

        // Get file - just return if found
        WebFile datasetFile = datasetURL.getFile();
        if (datasetFile != null)
            return datasetFile;

        // If Document.SourceURL available, look for dataset file in doc directory
        if (_docURL != null) {

            // Get document directory
            WebFile docFile = _docURL.getFile();
            WebFile docDir = docFile.getParent();

            // Look for same dataset filename in doc directory
            String datasetFilename = datasetURL.getFilename();
            datasetFile = docDir.getFileForName(datasetFilename);

            // If still not found, look for generic "Dataset.xml" in doc directory
            if (datasetFile == null) {
                datasetFile = docDir.getFileForName("Dataset.xml");

                // If still not found, look for xml file with Doc filename in doc directory
                if (datasetFile == null) {
                    String docFilename = _docURL.getFilename();
                    if (StringUtils.endsWithIC(docFilename, ".rpt")) {
                        String sisterName = StringUtils.replaceIC(docFilename, ".rpt", ".xml");
                        datasetFile = docDir.getFileForName(sisterName);
                    }
                }
            }

            // If dataset file was found, reset URL
            if (datasetFile != null)
                _url = datasetFile.getURL();
        }

        // Return
        return datasetFile;
    }

    /**
     * Returns a schema that may differ from the one stored in an XML file.
     */
    public boolean getCustomSchema()  { return _customSchema; }

    /**
     * Sets a schema that may differ from the one stored in an XML file.
     */
    public void setCustomSchema(boolean customSchema)
    {
        _customSchema = customSchema;
        _dataset = null;
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(XMLArchiver anArchiver)
    {
        // Get xml for basic datasource attributes
        XMLElement e = new XMLElement("datasource");

        // Archive SourceURL
        String surl = getURL() != null ? getURL().getString() : null;
        if (surl != null && surl.endsWith("HollywoodDB.xml")) surl = "Jar:/com/reportmill/examples/HollywoodDB.xml";
        if (surl != null) e.add("source", surl);

        // Archive schema if customSchema
        if (_customSchema) {
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
        if (schema != null) {
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
    public String toString()
    {
        return super.toString() + " " + getName();
    }
}