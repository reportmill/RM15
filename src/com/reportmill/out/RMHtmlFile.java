/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.base.ReportMill;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import snap.gfx.*;
import snap.util.*;

/**
 * Generates an HTML file.
 */
public class RMHtmlFile {

    // The document
    RMDocument             _doc;

    // The HTML image root
    String                 _imageRoot = "images";

    // A map of files
    Map <String,byte[]>    _files = new HashMap();
    
    // Whether to show border around page
    boolean                _showBorder = true;

/**
 * Creates a new RMHtmlFile for given document.
 */
public RMHtmlFile(RMDocument aDoc)  { _doc = aDoc; }

/**
 * Returns whether to show border around page(s).
 */
public boolean getShowBorder()  { return _showBorder; }

/**
 * Sets whether to show border around page(s).
 */
public void setShowBorder(boolean aValue)  { _showBorder = aValue; }

/**
 * Returns document as HTML XML.
 */
public XMLElement getXML()
{
    // Validate and resolve doc page references
    _doc.layoutDeep();
    _doc.resolvePageReferences();
    
    // Add watermark
    ReportMill.lc(_doc);

    // Create element for HTML and body and add pages
    XMLElement html = new XMLElement("html");
    XMLElement body = new XMLElement("body"); html.addElement(body);
    XMLElement div = new XMLElement("div"); body.addElement(div);
    for(int i=0;i<_doc.getPageCount(); i++) { RMPage page = _doc.getPage(i);
        XMLElement pageXML = toXML(page); div.addElement(pageXML); }
    return html;
}

/**
 * Returns a HTML XML string for document.
 */
public String getString()
{
    XMLElement xml = getXML();
    StringBuffer sb = new StringBuffer("<!DOCTYPE html>\n");
    xml.write(sb, "  ");
    return sb.toString();
}

/**
 * Returns bytes.
 */
public byte[] getBytes()  { return getString().getBytes(); }

/**
 * Returns bytes for top level div tag.
 */
public byte[] getDivBytes()  { return getXML().get("body").get("div").getBytes(); }

/**
 * Returns the image bytes.
 */
public Map <String,byte[]> getImageBytes()  { return _files; }

/**
 * Returns the image bytes.
 */
public byte[] getImageBytes(String aName)  { return _files.get(aName); }

/**
 * Writes HTML to file.
 */
public void write(String aPath)
{
    // Write bytes
    try { FileUtils.writeBytes(new File(aPath), getBytes()); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Write images
    File dir = new File(aPath); dir = dir.getParentFile();
    dir = new File(dir, _imageRoot); dir.mkdirs();
    for(String iname : _files.keySet()) { byte bytes[] = _files.get(iname);
        File file = new File(dir, iname);
        try { FileUtils.writeBytes(file, bytes); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
}

/**
 * Writes a shape to SVG HTML XML.
 */
protected XMLElement toXML(RMShape aShape)
{
    RMHtmlHelper hpr = RMHtmlHelper.getHelper(aShape);
    return hpr.toHTML(aShape, this);
}

/**
 * A class to do the work of writing HTML for other classes.
 */
private static abstract class RMHtmlHelper <T> {

    // A map of helpers for classes
    static Map <Class,RMHtmlHelper>  _helpers = new HashMap();

    /** Returns an SVG XML for given shape. */
    public abstract XMLElement toHTML(T anObj, RMHtmlFile aWriter);
    
    /** Returns a writer for an object. */
    public static RMHtmlHelper getHelper(Object anObj)
    {
        Class cls = anObj instanceof Class? (Class)anObj : anObj!=null? anObj.getClass() : null;
        RMHtmlHelper hpr = _helpers.get(cls);
        if(hpr==null) _helpers.put(cls, hpr=getHelperImpl(cls));
        return hpr;
    }
    
    /** Returns a writer for a class. */
    private static RMHtmlHelper getHelperImpl(Class aClass)
    {
        if(RMPage.class.isAssignableFrom(aClass)) return new RMPageHpr();
        if(RMTextShape.class.isAssignableFrom(aClass)) return new RMTextShapeHpr();
        if(RMImageShape.class.isAssignableFrom(aClass)) return new RMImageShapeHpr();
        if(RMShape.class.isAssignableFrom(aClass)) return new RMShapeHpr();
        return null;
    }
}

/**
 * An HTML helper for RMShape.
 */
private static class RMShapeHpr <T extends RMShape> extends RMHtmlHelper<T> {

    /** Returns an SVG XML for given shape. */
    public XMLElement toHTML(T aShape, RMHtmlFile aWriter)
    {
        XMLElement xml = createElement(aShape, aWriter);
        writeShape(aShape, aWriter, xml);
        writeChildren(aShape, aWriter, xml);
        writeClose(aShape, aWriter, xml);
        return xml;
    }
    
    /** Returns an SVG XML for given shape. */
    protected XMLElement createElement(T aShape, RMHtmlFile aWriter)  { return new XMLElement("g"); }
    
    /** Writes main element. */
    protected void writeShape(T aShape, RMHtmlFile aFile, XMLElement anXML)
    {
        // Get Fill/Stroke/Effect
        RMFill fill = aShape.getFill();
        RMStroke stroke = aShape.getStroke();
        Effect effect = aShape.getEffect();
        
        // Write fill and effect
        String fillString = writeFill(aShape, fill, aFile, anXML);
        String filterString = writeFilter(aShape, effect, aFile, anXML);
        
        // Add advanced transforms
        if(aShape.isRSS()) {
            StringMaker sm = new StringMaker();
            sm.append("translate(").append(aShape.getX() + aShape.getWidth()/2).append(',');
            sm.append(aShape.getY() + aShape.getHeight()/2).append(") ");
            if(aShape.getRoll()!=0) sm.append("rotate(").append(aShape.getRoll()).append(") ");
            if(aShape.getScaleX()!=1 || aShape.getScaleY()!=1)
                sm.append("scale(").append(aShape.getScaleX()).append(',').append(aShape.getScaleY()).append(") ");
            sm.append("translate(").append(-aShape.getWidth()/2).append(',').append(-aShape.getHeight()/2).append(")");
            anXML.add("transform", sm.toString());
        }
        else if(aShape.getX()!=0 || aShape.getY()!=0) {
            StringMaker sm = new StringMaker();
            sm.append("translate(").append(aShape.getX()).append(',').append(aShape.getY()).append(") ");
            anXML.add("transform", sm.toString());
        }
        
        // Add path
        XMLElement pathXML = null;
        if(fill!=null || stroke!=null || effect!=null) {
            pathXML = new XMLElement("path");
            pathXML.add("d", new SVGPathMaker().append(aShape.getPath()).toString());
            anXML.addElement(pathXML);
        
            // Do fill
            pathXML.add("fill", fillString);
        
            // Do stroke
            if(stroke!=null) {
                pathXML.add("stroke", '#' + stroke.getColor().toHexString());
                pathXML.add("stroke-width", stroke.getWidth());
            }
            
            // Do filter
            if(filterString!=null) pathXML.add("filter", filterString);
        }
    }
    
    /** Writes a fill. */
    protected String writeFill(T aShape, RMFill aFill, RMHtmlFile aFile, XMLElement anXML)
    {
        // Handle RMImageFill
        if(aFill instanceof RMImageFill) { RMImageFill ifill = (RMImageFill)aFill;
            Image img = ifill.getImage();
            String id = imageName(aFile, aShape, img);
            XMLElement fxml = new XMLElement("pattern");
            fxml.add("id", id);
            fxml.add("width", ifill.isTiled()? img.getWidth() : aShape.getWidth());
            fxml.add("height", ifill.isTiled()? img.getHeight() : aShape.getHeight());
            fxml.add("patternUnits", "userSpaceOnUse");
            XMLElement ixml = new XMLElement("image");
            ixml.add("xlink:href", aFile._imageRoot + '/' + id);
            ixml.add("width", ifill.isTiled()? img.getWidth() : aShape.getWidth());
            ixml.add("height", ifill.isTiled()? img.getHeight() : aShape.getHeight());
            if(!ifill.isTiled()) ixml.add("preserveAspectRatio", "none");
            fxml.addElement(ixml);
            anXML.addElement(fxml);
            return "url(#" + id + ")";
        }
        
        // Handle RMGradientFill
        if(aFill instanceof RMGradientFill) { RMGradientFill gfill = (RMGradientFill)aFill;
            XMLElement gxml = new XMLElement("linearGradient");
            gxml.add("id", "grad");
            gxml.add("x1", "0%"); gxml.add("y1", "0%"); gxml.add("x2", "100%"); gxml.add("y2", "0%");
            for(int i=0, iMax=gfill.getStopCount(); i<iMax; i++) {
                XMLElement sxml = new XMLElement("stop");
                sxml.add("offset", Math.round(gfill.getStopOffset(i)*100) + "%");
                sxml.add("style", "stop-color:" + '#' + gfill.getStopColor(i).toHexString());
                gxml.addElement(sxml);
            }
            anXML.addElement(gxml);
            return "url(#grad)";
        }
        
        // Handle anything else
        return aFill!=null? ('#' + aFill.getColor().toHexString()) : "none";
    }
    
    /** Writes a fill. */
    protected String writeFilter(T aShape, Effect anEffect, RMHtmlFile aFile, XMLElement anXML)
    {
        // Handle ShadowEffect
        if(anEffect instanceof ShadowEffect) { ShadowEffect shadow = (ShadowEffect)anEffect;
            XMLElement filter = new XMLElement("filter");
            filter.add("id", "filt"); filter.add("width", "200%"); filter.add("height", "200%");
            XMLElement offset = new XMLElement("feOffset");
            offset.add("result", "offOut"); offset.add("in", "SourceAlpha");
            offset.add("dx", shadow.getDX()); offset.add("dy", shadow.getDY());
            filter.add(offset);
            XMLElement blur = new XMLElement("feGaussianBlur");
            blur.add("result", "blurOut"); blur.add("in", "offOut"); blur.add("stdDeviation", shadow.getRadius()/2);
            filter.add(blur);
            XMLElement blend = new XMLElement("feBlend");
            blend.add("in", "SourceGraphic"); blend.add("in2", "blurOut"); blend.add("mode", "normal");
            filter.add(blend);
            anXML.add(filter);
            return "url(#filt)";
        }
        
        // Handle BlurEffect
        else if(anEffect instanceof BlurEffect) { BlurEffect blurEf = (BlurEffect)anEffect;
           XMLElement filter = new XMLElement("filter");
           filter.add("id", "filt");
           XMLElement blur = new XMLElement("feGaussianBlur");
           blur.add("stdDeviation", blurEf.getRadius()/2);
           filter.add(blur);
           anXML.add(filter);
           return "url(#filt)";
        }
        
        return null;
    }
    
    /** Writes shape children to XML. */
    protected void writeChildren(T aShape, RMHtmlFile aWriter, XMLElement anXML)
    {
        for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++) { RMShape child = aShape.getChild(i);
            XMLElement childXML = aWriter.toXML(child);
            anXML.addElement(childXML);
        }
    }
    
    /** Writes shape close for XML. */
    public void writeClose(T aShape, RMHtmlFile aWriter, XMLElement anXML)  { }
    
    /** Returns a unique image name for a shape with an image fill. */
    protected String imageName(RMHtmlFile aWriter, RMShape aShape, Image anImage)
    {
        // See if imageBytes are already in _files, if so return respective key
        byte imageBytes[] = anImage.getBytes();
        for(String key : aWriter._files.keySet()) {
            byte bytes[] = aWriter._files.get(key);
            if(ArrayUtils.equals(imageBytes, bytes))
                return key;
        }
    
        // Get name from aShape (or use sequential name: img0, img1, etc...)
        String name = aShape.getName(); if(name==null) name = "img" + aWriter._files.size();
        String filename = aWriter._imageRoot + name + "." + anImage.getType();
        aWriter._files.put(filename, imageBytes);
        return filename;
    }
}

/**
 * RMHtmlHelper implementation for RMPage.
 */
private static class RMPageHpr <T extends RMPage> extends RMShapeHpr <T> {

    /** Returns an SVG XML for given shape. */
    protected XMLElement createElement(T aShape, RMHtmlFile aWriter)  { return new XMLElement("svg"); }
    
    /** Writes main element. */
    protected void writeShape(T aShape, RMHtmlFile aWriter, XMLElement anXML)
    {
        super.writeShape(aShape, aWriter, anXML);
        anXML.add("width", (int)aShape.getWidth());
        anXML.add("height", (int)aShape.getHeight());
        
        if(aWriter.getShowBorder()) {
            XMLElement border = new XMLElement("rect");
            border.add("width", (int)aShape.getWidth());
            border.add("height", (int)aShape.getHeight());
            border.add("fill", "white");
            border.add("stroke", "black");
            anXML.addElement(border);
        }
    }
}

/**
 * RMHtmlHelper for RMTextShape.
 */
private static class RMTextShapeHpr <T extends RMTextShape> extends RMShapeHpr <T> {

    /** Returns an SVG XML for given shape. */
    protected XMLElement createElement(T aShape, RMHtmlFile aWriter)  { return new XMLElement("g"); }
    
    /** Writes main element. */
    protected void writeShape(T aTextShape, RMHtmlFile aWriter, XMLElement anXML)
    {
        // Do normal version
        super.writeShape(aTextShape, aWriter, anXML);
        
        // Create text element
        XMLElement text = new XMLElement("text");
        
        // Iterate over runs and create text spans
        TextBox tbox = aTextShape.getTextBox();
        for(TextBoxLine line : tbox.getLines())
        for(TextBoxRun run : line.getRuns()) {
            String str = run.getString(); Font rfont = run.getFont(); Color rcolor = run.getColor();
            double x = run.getX(), y = line.getBaseline();
            while(str.length()>0 && str.charAt(0)==' ') { x += rfont.charAdvance(' '); str = str.substring(1); }
            XMLElement tspan = new XMLElement("tspan");
            tspan.add("x", x); tspan.add("y", y);
            tspan.add("font-family", rfont.getFamily());
            tspan.add("font-style", rfont.isItalic()? "italic" : "normal");
            tspan.add("font-weight", rfont.isBold()? "bold" : "normal");
            tspan.add("font-size", (int)rfont.getSize());
            if(!rcolor.equals(RMColor.black)) tspan.add("fill", '#' + rcolor.toHexString());
            tspan.setValue(str);
            text.addElement(tspan);
        }
        
        // If only one tspan, replace text element with it
        if(text.getElementCount()==1) { text = text.getElement(0); text.setName("text"); }
        anXML.addElement(text);
    }
}

/**
 * An RMHtmlHelper implementation for RMImageShape.
 */
private static class RMImageShapeHpr <T extends RMImageShape> extends RMShapeHpr <T> {

    /** Returns an SVG XML for given shape. */
    protected XMLElement createElement(T aShape, RMHtmlFile aWriter)  { return new XMLElement("g"); }

    /** Writes main element. */
    protected void writeShape(T anImageShape, RMHtmlFile aWriter, XMLElement anXML)
    {
        // Do normal version
        super.writeShape(anImageShape, aWriter, anXML);
        
        // Create image element, configure and add to parent XML element
        XMLElement ixml = new XMLElement("image");
        Image img = anImageShape.getImage(); if(img==null) return;
        Rect bounds = anImageShape.getImageBounds();
        if(bounds.getX()!=0) ixml.add("x", bounds.getX());
        if(bounds.getY()!=0) ixml.add("y", bounds.getY());
        ixml.add("width", bounds.getWidth());
        ixml.add("height", bounds.getHeight());
        ixml.add("xlink:href", aWriter._imageRoot + '/' + imageName(aWriter, anImageShape, img));
        if(!anImageShape.getPreserveRatio()) ixml.add("preserveAspectRatio", "none");
        anXML.addElement(ixml);
    }
}

/**
 * Makes a path string from an AWT Path.
 */
private static class SVGPathMaker extends StringMaker {

    /** Returns an XML for a AWT Shape. */
    public SVGPathMaker append(Shape aShape)
    {
        PathIter pi = aShape.getPathIter(null);
        double pts[] = new double[6];
        while(pi.hasNext()) {
            switch(pi.getNext(pts)) {
                case MoveTo: moveTo(pts[0], pts[1]); break;
                case LineTo: lineTo(pts[0], pts[1]); break;
                case QuadTo: quadTo(pts[0], pts[1], pts[2], pts[3]); break;
                case CubicTo: cubeTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]); break;
                case Close: closePath(); break;
            }
        }
        return this;
    }
    
    /** MoveTo. */
    public void moveTo(double anX, double aY)  { append("M").append(anX).append(' ').append(aY).append(' '); }
    
    /** LineTo. */
    public void lineTo(double anX, double aY)  { append("L").append(anX).append(' ').append(aY).append(' '); }
    
    /** QuadTo. */
    public void quadTo(double aX1, double aY1, double aX2, double aY2)
    {
        append("Q").append(aX1).append(' ').append(aY1).append(' ');
        append(aX2).append(' ').append(aY2).append(' ');
    }
    
    /** CubeTo. */
    public void cubeTo(double aX1, double aY1, double aX2, double aY2, double aX3, double aY3)
    {
        append("C"); append(aX1).append(' ').append(aY1).append(' ');
        append(aX2).append(' ').append(aY2).append(' ').append(aX3).append(' ').append(aY3).append(' ');
    }
    
    /** ClosePath. */
    public void closePath()  { append("Z "); }
}

/**
 * Like StringBuffer but with double formatting.
 */
private static class StringMaker {

    // The StringBuffer and a DecimalFormat for doubles
    StringBuffer     _sb = new StringBuffer();
    DecimalFormat    _format = new DecimalFormat("#.###");
    
    /** Appends a string. */
    public StringMaker append(char aChar)  { _sb.append(aChar); return this; }
    
    /** Appends a string. */
    public StringMaker append(String aString)  { _sb.append(aString); return this; }
    
    /** Appends a string. */
    public StringMaker append(double aValue)
    {
        if(aValue==(int)aValue) _sb.append((int)aValue);
        else _sb.append(_format.format(aValue));
        return this;
    }
    
    /** Returns string. */
    public String toString()  { return _sb.toString(); }
}

}