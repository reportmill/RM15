/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.graphics.*;
import com.reportmill.shape.RMShape;
import java.util.*;
import snap.geom.Point;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.geom.Transform;
import snap.gfx.*;
import snappdf.*;
import snappdf.write.PDFPageWriter;
import snappdf.write.SnapPaintPdfr;

/**
 * Utility methods to write PDF for RMFill.
 */
public class RMFillPdfr {

    /**
     * Writes a given shape fill.
     */
    public static void writeShapeFill(RMShape aShape, RMFill aFill, RMPDFWriter aWriter)
    {
        // Handle RMGradientFill and RMImageFill
        if (aFill instanceof RMGradientFill) writeGradientFill(aShape, (RMGradientFill) aFill, aWriter);
        else if (aFill instanceof RMImageFill) writeImageFill(aShape, (RMImageFill) aFill, aWriter);

            // Handle color fill
        else {
            Shape path = aShape.getPath();
            SnapPaintPdfr.writeShapeFill(path, aFill.getColor(), aWriter);
        }
    }

    /**
     * Writes pdf for the path filled with a shading pattern defined by the RMGradientFill
     */
    public static void writeGradientFill(RMShape aShape, RMGradientFill aFill, PDFWriter aWriter)
    {
        // Get shape path and PDF page and write path
        Shape path = aShape.getPath();
        PDFPageWriter pdfPage = aWriter.getPageWriter();
        pdfPage.writePath(path);

        // Get the xref table so we can add objects to it
        PDFXTable xref = aWriter.getXRefTable();

        // Set the fill colorspace to the pattern colorspace (subspace is always rgb)
        pdfPage.appendln("/Pattern cs");

        // Create pdf functions that interpolate linearly between color stops
        int stopCount = aFill.getStopCount();
        ArrayList<Map<String,Object>> functionsList = new ArrayList<>(stopCount);
        Map<String,Object> functionDict = null;
        String outerBounds = "", outerDomain = "", encode = "";
        for (int i = 0; i < stopCount - 1; ++i) {
            functionDict = new Hashtable<>(5);
            RMColor c0 = aFill.getStopColor(i);
            RMColor c1 = aFill.getStopColor(i + 1);
            double d0 = aFill.getStopOffset(i);
            double d1 = aFill.getStopOffset(i + 1);
            functionDict.put("FunctionType", "2");
            functionDict.put("Domain", "[0 1]");
            functionDict.put("N", "1");
            functionDict.put("C0", c0);
            functionDict.put("C1", c1);
            functionsList.add(functionDict);

            // add endpoints to Domain & Bounds arrays of stitching function
            if (i == 0)
                outerDomain += d0;
            else outerBounds += " " + d0;
            if (i == stopCount - 2)
                outerDomain += " " + d1;

            // all input to sub-functions mapped to range 0-1
            encode += "0 1 ";
        }

        // If there are multiple stops, create a stitching function to combine all the functions
        if (stopCount > 2) {
            functionDict = new Hashtable<>(5);
            functionDict.put("FunctionType", "3");
            functionDict.put("Functions", functionsList);
            functionDict.put("Domain", "[" + outerDomain + "]");
            functionDict.put("Bounds", "[" + outerBounds + "]");
            functionDict.put("Encode", "[" + encode + "]");
        }

        // Create a shading dictionary for the gradient
        Map<String,Object> shadingDict = new Hashtable<>(4);
        boolean isRadial = aFill.isRadial();

        shadingDict.put("ShadingType", isRadial ? "3" : "2");  // radial or axial shading
        shadingDict.put("ColorSpace", "/DeviceRGB");  // rgb colorspace
        shadingDict.put("AntiAlias", "true");
        shadingDict.put("Function", xref.addObject(functionDict));

        // Get gradient paint and start/end
        GradientPaint gpnt = aFill.snap().copyForRect(aShape.getBoundsInside());
        Point startPt = new Point(gpnt.getStartX(), gpnt.getStartY()), endPt = new Point(gpnt.getEndX(), gpnt.getEndY());

        // In pdf, coordinates of the gradient axis are defined in pattern space.  Pattern space is the same as the
        // page's coordinate system, and doesn't get affected by changes to the ctm. Since the RMGradient returns
        // points in the shape's coordinate system, we have to transform them into pattern space (page space).
        RMShape page = aShape.getPageShape();
        Transform patternSpaceTransform = aShape.getLocalToParent(page);
        patternSpaceTransform.transformPoint(startPt);
        patternSpaceTransform.transformPoint(endPt);

        // add in flip
        startPt.y = page.getFrameMaxY() - startPt.y;
        endPt.y = page.getFrameMaxY() - endPt.y;

        // Add the newly calculated endpoints to the shading dictionary
        List<Double> coordsList = new ArrayList<>(4);
        coordsList.add(startPt.x);
        coordsList.add(startPt.y);
        if (isRadial) {
            coordsList.add(0d); // start radius = 0
            coordsList.add(coordsList.get(0));
            coordsList.add(coordsList.get(1)); // end point is same as start point
            coordsList.add(endPt.getDistance(startPt)); // end radius is the distance between the start & end points
            shadingDict.put("Extend", "[false true]"); // set radial shading to extend beyond end circle
        }
        else {
            coordsList.add(endPt.x);
            coordsList.add(endPt.y);
        }
        shadingDict.put("Coords", coordsList);

        // Create a new pattern dictionary for the gradient
        Map<String,String> patternDict = new Hashtable<>(10);
        patternDict.put("Type", "/Pattern");
        patternDict.put("PatternType", "2");
        patternDict.put("Shading", xref.addObject(shadingDict)); // pat.put("Matrix", patternSpaceTransform);

        // Set the pattern for fills
        pdfPage.append('/').append(pdfPage.addPattern(patternDict)).appendln(" scn");

        // Write fill operator
        pdfPage.append('f');

        // If path winding rule odd, write odd fill operator
        //if(path.getWindingRule()==RMPath.WIND_EVEN_ODD) pdfPage.append('*');

        // Write trailing newline
        pdfPage.appendln();
    }

    /**
     * Writes given RMImageFill to a PDF file.
     */
    public static void writeImageFill(RMShape aShape, RMImageFill anImageFill, RMPDFWriter aWriter)
    {
        writeImageFill(anImageFill, aShape.getPath(), aShape.getBoundsInside(), aWriter);
    }

    /**
     * Writes given RMImageFill to a PDF file.
     */
    public static void writeImageFill(RMImageFill anImageFill, Shape aPath, Rect bounds, RMPDFWriter aWriter)
    {
        // Get image (just return if missing) and image name and add image to writer
        Image img = anImageFill.getImage();
        if (img == null) return;
        String iname = aWriter.getImageName(img);
        aWriter.addImage(img);

        // Get PDF page and gsave
        PDFPageWriter pdfPage = aWriter.getPageWriter();
        pdfPage.gsave();

        // If path was provided, clip to it
        if (aPath != null) {
            pdfPage.writePath(aPath);
            pdfPage.appendln(" W n");
        }

        // If scaled, translate to shape center, scale and return
        if (anImageFill.getScaleX() != 1 || anImageFill.getScaleY() != 1) {

            // Get shape width and height
            double width = bounds.getWidth();
            double height = bounds.getHeight();

            // Get transform with translate to shape center, scale, and translate back
            Transform t = new Transform();
            t.translate(width / 2, height / 2);
            t.scale(anImageFill.getScaleX(), anImageFill.getScaleY());
            t.translate(-width / 2, -height / 2);

            // Apply transform
            pdfPage.writeTransform(t);

            // Transform bounds to enclose rotated and scaled image space
            t.invert();
            t.transformRect(bounds);

            // If not STYLE_TILE, scale enclosing bounds by image fill scale
            if (!anImageFill.isTiled()) {
                Transform t2 = new Transform();
                t2.translate(width / 2, height / 2);
                t2.scale(anImageFill.getScaleX(), anImageFill.getScaleY());
                t2.translate(-width / 2, -height / 2);
                t2.transformRect(bounds);
            }
        }

        // If fill style tile, stamp image edge 2 edge, left 2 right, top 2 bottom (could use PDF tiling patterns instead)
        if (anImageFill.isTiled()) {

            // Get image width/height, which becomes scale of image coords (except PDF images write out scale of 1x1)
            double width = img.getWidth();
            double height = img.getHeight();

            // Get starting x and y
            double startX = bounds.x + anImageFill.getX();
            while (startX > bounds.x) startX -= width;
            double startY = bounds.y + anImageFill.getY();
            while (startY > bounds.y) startY -= height;

            // Iterate left to right over shape width and top to bottom over shape height
            for (double x = startX, xMax = bounds.getMaxX(); x < xMax; x += width) {
                for (double y = startY, yMax = bounds.getMaxY(); y < yMax; y += height) {

                    // Gsave, scale CTM, Do image and Grestore
                    pdfPage.gsave();
                    pdfPage.writeTransform(width, 0, 0, -height, x, height + y);
                    pdfPage.appendln("/" + iname + " Do");
                    pdfPage.grestore();
                }
            }
        }

        // All other fillStyles smack image in imageBounds:
        // Apply CTM (image coords flipped from page coords - (0,0) at upper-left)
        else {
            double x = anImageFill.getX() + bounds.x, y = anImageFill.getY() + bounds.getMaxY();
            double width = bounds.width, height = bounds.height;
            pdfPage.writeTransform(width, 0, 0, -height, x, y);
            pdfPage.appendln("/" + iname + " Do");
        }

        // Grestore
        pdfPage.grestore();
    }

}