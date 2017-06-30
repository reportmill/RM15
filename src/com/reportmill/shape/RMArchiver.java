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
public RMParentShape getParentShape(Object aSource)  { return (RMParentShape)getShape(aSource, null); }

/**
 * Creates a document.
 */
public RMShape getShape(Object aSource, Archivable aRootObj)
{
    // If source is a document, just return it
    if(aSource instanceof RMDocument) return (RMDocument)aSource;
    
    // Get URL and/or bytes (complain if not found)
    WebURL url = null; try { url = WebURL.getURL(aSource); } catch(Exception e) { }
    byte bytes[] = url!=null? (url.getFile()!=null? url.getFile().getBytes() : null) : SnapUtils.getBytes(aSource);
    if(bytes==null)
        throw new RuntimeException("RMArchiver.getShape: Cannot read source: " + (url!=null? url : aSource));
    
    // If PDF, return PDF Doc
    if(bytes!=null && RMImageDataPDF.canRead(bytes))
        return getDocPDF(url!=null? url : bytes, aRootObj instanceof RMDocument? (RMDocument)aRootObj : null);

    // Create archiver, read, set source and return
    setRootObject(aRootObj);
    RMShape shape = (RMShape)readObject(url!=null? url : bytes);
    if(shape instanceof RMParentShape) { RMParentShape pshp = (RMParentShape)shape;
        pshp.setSourceURL(url);
        pshp.layout();
    }
    return shape;
}

/**
 * Creates a document.
 */
public RMDocument getDoc(Object aSource, Archivable aBaseDoc)
{
    RMShape shape = getShape(aSource, aBaseDoc);
    RMDocument doc = shape instanceof RMDocument? (RMDocument)shape : null;
    if(doc==null) { doc = new RMDocument(shape.getWidth(), shape.getHeight()); doc.getPage(0).addChild(shape); }
    doc.setSourceURL(getSourceURL());
    return doc;
}

/**
 * Creates a new document from a PDF source.
 */
private RMDocument getDocPDF(Object aSource, RMDocument aBaseDoc)
{
    // Get/create new document (with no pages)
    RMDocument doc = aBaseDoc!=null? aBaseDoc : new RMDocument();
    while(doc.getPageCount()>0) doc.removePage(0);
    
    // Get image data for source and iterate over each PDF page and create/add document page
    RMImageData imageData = RMImageData.getImageData(aSource);
    for(int i=0, iMax=imageData.getPageCount(); i<iMax; i++) { RMImageData pageData = imageData.getPage(i);
        RMPage page = doc.addPage();
        page.setSize(pageData.getImageWidth(), pageData.getImageHeight());
        page.addChild(new RMImageShape(pageData));
        if(i==0) doc.setSize(page.getWidth(), page.getHeight());
    }
    
    // Return doc
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
        System.err.println("RMFormatStub: Unknown format type " + type); return null;
    }
}

}