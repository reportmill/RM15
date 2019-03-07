/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.shape.*;
import java.util.*;

/**
 * This class is used to write a String representation of an RMDocument.
 */
public class RMStringWriter {

/**
 * Returns a byte array holding an ASCII representation of a given document.
 */
public static byte[] delimitedAsciiBytes(RMDocument aDoc, String fieldDelim, String recordDelim, boolean quoteFields)
{
    // Generate delimited string
    String s = delimitedString(aDoc, fieldDelim, recordDelim, quoteFields);
    
    // Return bytes
    try { return s.getBytes("ISO-8859-1"); }
    catch(Exception e) { System.err.println(e); return null; }
}

/**
 * Returns a String holding the delimited data for a given document.
 */
public static String delimitedString(RMDocument aDoc, String fieldDelim, String recordDelim, boolean quoteFields)
{
    // Validate and resolve page references
    aDoc.layoutDeep();
    aDoc.resolvePageReferences();
    
    // Create new string buffer
    StringBuffer sb = new StringBuffer();

    // Append shapes
    for(int i=0, iMax=aDoc.getPages().size(); i<iMax; i++)
        appendDelimited(sb, aDoc.getPage(i), fieldDelim, recordDelim, quoteFields);

    // Return string
    return sb.toString();
}

/**
 * Appends a string representation of the given shape to the given string buffer.
 */
static void appendDelimited(StringBuffer aSB, RMShape aShape, String fieldD, String recD, boolean quoteFields)
{
    // If table row, iterate over children (sorted by minX) and append their strings separated by fieldDelim
    if(aShape instanceof RMTableRowRPG && aShape.getChildCount()>0) {
        
        // Get sorted children
        List <RMShape> children = RMShapeUtils.getShapesSortedByFrameX(aShape.getChildren());

        // Iterate over children
        for(int i=0, iMax=children.size(); i<iMax; i++) { RMShape child = children.get(i);

            // Handle text children
            if(child instanceof RMTextShape) { RMTextShape text = (RMTextShape)child;
                if(quoteFields)
                    aSB.append('\"').append(text.getXString().getText()).append('\"').append(fieldD);
                else aSB.append(text.getXString().getText()).append(fieldD);
            }
        }
        
        // Trim last field delimiter and add record delimiter
        if(aSB.toString().endsWith(fieldD))
            aSB.delete(aSB.length()-fieldD.length(), aSB.length());
        aSB.append(recD);
    }
    
    // Handle RMCrossTab
    else if(aShape instanceof RMCrossTab) { RMCrossTab table = (RMCrossTab)aShape;
        
        // Iterate over rows
        for(int i=0, iMax=table.getRowCount(); i<iMax; i++) { RMCrossTabRow row = table.getRow(i);
            
            // Iterate over row cells and add cell string plus field delimiter
            for(int j=0, jMax=row.getCellCount(); j<jMax; j++)
                if(quoteFields)
                    aSB.append('\"').append(row.getCell(j).getXString().getText()).append('\"').append(fieldD);
                else aSB.append(row.getCell(j).getXString().getText()).append(fieldD);
            
            // Trim last field delimiter and add record delimiter
            aSB.delete(aSB.length()-fieldD.length(), aSB.length());
            aSB.append(recD);
        }
    }

    // Otherwise descend into shape
    else for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        appendDelimited(aSB, aShape.getChild(i), fieldD, recD, quoteFields);
}

}