/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import com.reportmill.graphics.*;
import java.util.*;
import snap.util.*;
import snap.web.WebURL;

/**
 * This class handles RM document archival.
 */
public class RMArchiver extends XMLArchiver {

/**
 * Returns a parent shape for source.
 */
public RMDocument getDoc(Object aSource)  { return getDoc(aSource, null); }

/**
 * Creates a document.
 */
public RMDocument getDoc(Object aSource, RMDocument aBaseDoc)
{
    // If source is a document, just return it
    if(aSource instanceof RMDocument) return (RMDocument)aSource;
    
    // Get URL and/or bytes (complain if not found)
    WebURL url = WebURL.getURL(aSource);
    byte bytes[] = url!=null? url.getBytes() : SnapUtils.getBytes(aSource);
    if(bytes==null)
        throw new RuntimeException("RMArchiver.getDoc: Cannot read source: " + (url!=null? url : aSource));
    
    // If PDF, return PDF Doc
    if(bytes!=null && RMPDFData.canRead(bytes))
        return RMPDFShape.getDocPDF(url!=null? url : bytes, aBaseDoc);

    // Create archiver, read, set source and return
    setRootObject(aBaseDoc);
    
    RMDocument doc = (RMDocument)readObject(url!=null? url : bytes);
    
    // Set Source URL and return
    doc.setSourceURL(getSourceURL());
    return doc;
}

/**
 * Returns the class map.
 */
public Map <String, Class> getClassMap()  { return _rmCM!=null? _rmCM : (_rmCM=createClassMap()); }
static Map <String, Class> _rmCM;

/**
 * Creates the class map.
 */
protected Map <String, Class> createClassMap()
{
    // Create class map and add classes
    Map classMap = new HashMap();
    
    // Shape classes
    classMap.put("arrow-head", RMLineShape.ArrowHead.class);
    classMap.put("cell-table", RMCrossTab.class);
    classMap.put("cell-table-frame", RMCrossTabFrame.class);
    classMap.put("document", RMDocument.class);
    classMap.put("flow-shape", RMParentShape.class);
    classMap.put("graph", RMGraph.class);
    classMap.put("graph-legend", RMGraphLegend.class);
    classMap.put("image-shape", RMImageShape.class);
    classMap.put("label", RMLabel.class);
    classMap.put("labels", RMLabels.class);
    classMap.put("line", RMLineShape.class);
    classMap.put("oval", RMOvalShape.class);
    classMap.put("page", RMPage.class);
    classMap.put("polygon", RMPolygonShape.class);
    classMap.put("rect", RMRectShape.class);
    classMap.put("shape", RMParentShape.class);
    classMap.put("spring-shape", RMSpringShape.class);
    classMap.put("subreport", RMSubreport.class);
    classMap.put("switchshape", RMSwitchShape.class);
    classMap.put("table", RMTable.class);
    classMap.put("table-group", RMTableGroup.class);
    classMap.put("tablerow", RMTableRow.class);
    classMap.put("text", RMTextShape.class);
    classMap.put("linked-text", RMLinkedText.class);
    classMap.put("scene3d", RMScene3D.class);
    classMap.put("ViewShape", ViewShape.class);
    
    // Graphics
    classMap.put("color", RMColor.class);
    classMap.put("font", RMFont.class);
    classMap.put("format", RMFormatStub.class);
    classMap.put("pgraph", RMParagraph.class);
    classMap.put("xstring", RMXString.class);
    
    // Strokes
    classMap.put("stroke", RMStroke.class); classMap.put("double-stroke", RMStroke.class);
    classMap.put("border-stroke", "com.reportmill.graphics.RMBorderStroke");
    
    // Fills
    classMap.put("fill", RMFill.class);
    classMap.put("gradient-fill", RMGradientFill.class);
    classMap.put("radial-fill", RMGradientFill.class);
    classMap.put("image-fill", RMImageFill.class);
    classMap.put("contour-fill", "com.reportmill.graphics.RMContourFill");
    
    // Effects
    classMap.put("blur-effect", "snap.gfx.BlurEffect");
    classMap.put("shadow-effect", "snap.gfx.ShadowEffect");
    classMap.put("reflection-effect", "snap.gfx.ReflectEffect");
    classMap.put("emboss-effect", "snap.gfx.EmbossEffect");

    // Sorts, Grouping
    classMap.put("sort", "com.reportmill.base.RMSort");
    classMap.put("top-n-sort", "com.reportmill.base.RMTopNSort");
    classMap.put("value-sort", "com.reportmill.base.RMValueSort");
    classMap.put("grouper", RMGrouper.class);
    classMap.put("grouping", RMGrouping.class);
    
    // Return classmap
    return classMap;
}

/**
 * A class to unarchive formats as proper subclass based on type attribute.
 */
public static class RMFormatStub implements Archivable {

    /** Implement fromXML to return proper format based on type attribute. */
    public XMLElement toXML(XMLArchiver anArchive)  { return null; }
    public Object fromXML(XMLArchiver anArchiver, XMLElement anElmnt)    
    {
        String type = anElmnt.getAttributeValue("type","");
        if(type.equals("number")) return anArchiver.fromXML(anElmnt, RMNumberFormat.class,null);
        if(type.equals("date")) return anArchiver.fromXML(anElmnt, RMDateFormat.class, null);
        if(type.length()>0) System.err.println("RMFormatStub: Unknown format type " + type);
        return null;
    }
}

}