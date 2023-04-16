/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import snap.geom.*;
import snap.gfx.*;
import snap.text.TextBox;
import snap.text.TextBoxLine;
import snap.text.TextBoxRun;

/**
 * Utility methods for some esoteric text functionality.
 */
public class RMTextShapeUtils {

    /**
     * Returns an RMPolygon shape with the glyph path for the chars in this text. Assumes all runs have same visual attrs.
     */
    public static RMPolygonShape getTextPathShape(RMTextShape aText)
    {
        // Create polygon for text path with attributes from text shape
        Shape charsShape = getTextOutlineShape(aText);
        RMPolygonShape polygon = new RMPolygonShape(charsShape);
        polygon.copyShape(aText);

        // Set polygon color to run or outline color and stroke and return
        polygon.setColor(aText.getTextColor());
        Border brdr = aText.getTextBorder();
        polygon.setStroke(brdr != null ? new RMStroke(RMColor.get(brdr.getColor()), brdr.getWidth()) : null);
        return polygon;
    }

    /**
     * Returns a path for all text chars.
     */
    public static Shape getTextOutlineShape(RMTextShape aText)
    {
        // Create path and establish bounds of text
        Path2D outlineShape = new Path2D();
        outlineShape.moveTo(0, 0);
        outlineShape.moveTo(aText.getWidth(), aText.getHeight());

        // Iterate over text runs
        TextBox textBox = aText.getTextBox();
        for (TextBoxLine line : textBox.getLines()) {
            for (TextBoxRun run : line.getRuns()) { //if(run.length()==0 || run.isTab()) continue;
                String str = run.getString();
                Font font = run.getFont();
                double charSpacing = run.getStyle().getCharSpacing();
                Shape runOutlineShape = font.getOutline(str, run.getX(), line.getBaseline(), charSpacing);
                outlineShape.appendShape(runOutlineShape);
            }
        }

        // Return
        return outlineShape;
    }

    /**
     * Returns a group shape with a text shape for each individual character in this text shape.
     */
    public static RMShape getTextCharsShape(RMTextShape aText)
    {
        // Get shape for chars
        RMParentShape textCharsShape = new RMSpringShape();
        textCharsShape.copyShape(aText);

        // Iterate over runs
        TextBox textBox = aText.getTextBox();
        for (TextBoxLine line : textBox.getLines())
            for (TextBoxRun run : line.getRuns()) { //if(run.length()==0 || run.isTab()) continue;

                // Get run font and run bounds
                Font font = run.getFont();
                Rect runBounds = new Rect(run.getX(), line.getY(), 0, line.getHeight()); // run y/height instead?

                // Iterate over run chars
                for (int i = 0, iMax = run.length(); i < iMax; i++) {
                    char c = run.charAt(i);

                    // Get char advance (just continue if zero)
                    double advance = font.charAdvance(c);
                    if (advance <= 0)
                        continue;

                    // If non-space character, create glyph shape
                    if (c != ' ') {
                        Rect glyphBounds = font.getCharBounds(c);
                        RMXString gstring = aText.getXString().substring(run.getStart() + i, run.getStart() + i + 1);
                        RMTextShape glyphShape = new RMTextShape(gstring);
                        glyphShape.setAutosizing("~-~,~-~");

                        textCharsShape.addChild(glyphShape);
                        runBounds.width = Math.ceil(Math.max(advance, glyphBounds.getMaxX()));
                        Rect glyphFrame = getBoundsFromTextBounds(aText, runBounds);
                        glyphShape.setFrame(glyphFrame);
                    }

                    // Increase bounds by advance
                    runBounds.x += advance;
                }
            }

        // Return
        return textCharsShape;
    }

    /**
     * Returns bounds from given text bounds, adjusted to account for text margins.
     */
    private static Rect getBoundsFromTextBounds(RMTextShape aText, Rect aRect)
    {
        Insets textMargin = aText.getMargin();
        double rectX = aRect.x - textMargin.left;
        double rectY = aRect.y - textMargin.top;
        double rectW = aRect.width + textMargin.getWidth();
        double rectH = aRect.height + textMargin.getHeight();
        return new Rect(rectX, rectY, rectW, rectH);
    }
}