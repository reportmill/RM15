/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.awt.Color;
import java.io.*;
import java.util.Enumeration;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * Parses rtf data from a string and returns it as an xstring.
 */
public class RMRTFParser {

/**
 * Returns an xstring from the given rtf string and default font.
 */
public static RMXString parse(String rtf, RMFont baseFont)
{
    try { return parseRTF(rtf, baseFont); }
    catch(Exception e) { e.printStackTrace(); return null; }
}

/**
 * Returns an xstring from the given rtf string and default font.
 */
public static RMXString parseRTF(String rtf, RMFont baseFont) throws Exception
{
    // Use RTFEditorKit to do the real parsing work
    EditorKit kit = new RTFEditorKit();
    Document doc = kit.createDefaultDocument();
    Reader reader = new StringReader(rtf);
    kit.read(reader, doc, 0);
    
    // Now we'll walk through the document and piece together our XString
    ElementIterator elemIterator = new ElementIterator(doc);
    AbstractDocument.AbstractElement elem;
    
    // Declare return string and loop attribute variables
    RMXString result = new RMXString();
    RMFont font = baseFont; RMColor color = null; boolean underline = false;
    
    // Iterate over rtf elements
    while((elem = (AbstractDocument.AbstractElement)elemIterator.next()) != null) {
        
        // Handle content element
        if(elem.getName().equals("content")) {
            
            // Get content string
            String content = doc.getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());

            // Iterate over attribute names
            for(Enumeration e=elem.getAttributeNames(); e.hasMoreElements();) {
            
                // Get attribute and attribute name
                Object attr = e.nextElement();
                String attrName = attr.toString();
                
                // Handle bold
                if(attrName=="bold" && ((Boolean)elem.getAttribute(attr)) && font.getBold()!=null)
                    font = font.getBold();
                
                // Handle italic
                if(attrName=="italic" && ((Boolean)elem.getAttribute(attr)) && font.getItalic()!=null)
                    font = font.getItalic();

                // Handle underline
                if(attrName=="underline")
                    underline = (Boolean)elem.getAttribute(attr);
                
                // Handle foreground
                if(attrName=="foreground") {
                    Color c = (Color)elem.getAttribute(attr);
                    color = new RMColor(c.getRGB());
                }
                
                // Handle size
                if(attrName=="size") {
                    int size = (Integer)elem.getAttribute(attr);
                    font = font.deriveFont(size);
                }
                
                // Handle font family
                if(attrName=="family") {
                    String fontName = (String)elem.getAttribute(attr);
                    RMFont f = new RMFont(fontName, font.getSize());
                    if(!f.isSubstitute())
                        font = f;
                }
            }
            
            // Create new xstring for rtf run (string, font, color, underline) and add
            RMXString xstring = new RMXString(content, font, color); xstring.setUnderlined(underline);
            result.addString(xstring, result.length());
            
            // Reset font, color & underline
            font = baseFont; color = null; underline = false;
        }
    }
    
    // Return rtf xstring
    return result;
}

}