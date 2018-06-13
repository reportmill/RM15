/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import snap.gfx.*;

/**
 * Utility methods for some esoteric text functionality.
 */
public class RMTextShapeUtils {

/**
 * Returns a path for all text chars.
 */
public static Shape getTextPath(RMTextShape aText)
{
    // Create path and establish bounds of text
    Path path = new Path(); path.moveTo(0,0); path.moveTo(aText.getWidth(), aText.getHeight());
    
    // Iterate over text runs
    TextBox tbox = aText.getTextBox();
    for(TextBoxLine line : tbox.getLines())
    for(TextBoxRun run : line.getRuns()) { //if(run.length()==0 || run.isTab()) continue;
        String str = run.getString(); double cspace = run.getStyle().getCharSpacing();
        path.append(run.getFont().getOutline(str, run.getX(), line.getBaseline(), cspace));
    }
    
    // Return path
    return path;
}

/**
 * Returns an RMPolygon shape with the glyph path for the chars in this text. Assumes all runs have same visual attrs.
 */
public static RMPolygonShape getTextPathShape(RMTextShape aText)
{
    // Create polygon for text path with attributes from text shape
    RMPolygonShape polygon = new RMPolygonShape(getTextPath(aText));
    polygon.copyShape(aText);
    
    // Set polygon color to run or outline color and stroke and return
    polygon.setColor(aText.getTextColor());
    Border brdr = aText.getTextBorder();
    polygon.setStroke(brdr!=null? new RMStroke(RMColor.get(brdr.getColor()), brdr.getWidth()) : null);
    return polygon;
}

/**
 * Returns a group shape with a text shape for each individual character in this text shape.
 */
public static RMShape getTextCharsShape(RMTextShape aText)
{
    // Get shape for chars
    RMParentShape charsShape = new RMSpringShape(); charsShape.copyShape(aText);
    
    // Iterate over runs
    TextBox tbox = aText.getTextBox();
    for(TextBoxLine line : tbox.getLines())
    for(TextBoxRun run : line.getRuns()) { //if(run.length()==0 || run.isTab()) continue;
    
        // Get run font and run bounds
        Font font = run.getFont();
        Rect runBounds = new Rect(run.getX(), line.getY(), 0, line.getHeight()); // run y/height instead?
        
        // Iterate over run chars
        for(int i=0, iMax=run.length(); i<iMax; i++) { char c = run.charAt(i);
            
            // Get char advance (just continue if zero)
            double advance = font.charAdvance(c); if(advance<=0) continue;
            
            // If non-space character, create glyph shape
            if(c != ' ') {
                Rect glyphBounds = font.getCharBounds(c);
                RMXString gstring = aText.getXString().substring(run.getStart() + i, run.getStart() + i + 1);
                RMTextShape glyph = new RMTextShape(gstring); glyph.setAutosizing("~-~,~-~");

                charsShape.addChild(glyph);
                runBounds.width = Math.ceil(Math.max(advance, glyphBounds.getMaxX()));
                glyph.setFrame(getBoundsFromTextBounds(aText, runBounds));
            }

            // Increase bounds by advance
            runBounds.x += advance;
        }
    }

    // Return chars shape
    return charsShape;
}

/**
 * Returns bounds from given text bounds, adjusted to account for text margins.
 */
private static Rect getBoundsFromTextBounds(RMTextShape aText, Rect aRect)
{
    double rx = aRect.getX(), ry = aRect.getY(), rw = aRect.getWidth(), rh = aRect.getHeight();
    rx -= aText.getMarginLeft(); rw += (aText.getMarginLeft() + aText.getMarginRight());
    ry -= aText.getMarginTop(); rh += (aText.getMarginTop() + aText.getMarginBottom());
    return new Rect(rx,ry,rw,rh);
}

}