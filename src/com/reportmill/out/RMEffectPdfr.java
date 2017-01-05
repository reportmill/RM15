/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.out;
import com.reportmill.graphics.RMImageFill;
import com.reportmill.shape.*;
import snap.gfx.*;

/**
 * A PDF helper/writer class for RMEffect.
 */
public class RMEffectPdfr {

/**
 * Writes a shape with effect.
 */
public static void writeShapeEffect(RMShape aShape, RMPDFWriter aWriter)
{
    Effect eff = aShape.getEffect();
    if(eff instanceof BlurEffect) writeBlurEffect(aShape, (BlurEffect)eff, aWriter);
    else if(eff instanceof ShadowEffect) writeShadowEffect(aShape, (ShadowEffect)eff, aWriter);
    else if(eff instanceof ReflectEffect) writeRefectionEffect(aShape, (ReflectEffect)eff, aWriter);
    else if(eff instanceof EmbossEffect) writeEmbossEffect(aShape, (EmbossEffect)eff, aWriter);
}
    
/**
 * Writes pdf for given blur effect and shape.
 */
public static void writeBlurEffect(RMShape aShape, BlurEffect aBlur, RMPDFWriter aWriter)
{
    // If radius is less than 1, do default drawing and return
    if(aBlur.getRadius()<1) { RMShapePdfr.getPdfr(aShape).writeShapeAll(aShape, aWriter); return; }
    
    // Get effect image and image fill
    Image effImg = getEffectImage(aShape);
    RMImageFill ifill = new RMImageFill(effImg);
    
    // Get bounds for image fill and write
    Rect bounds = new Rect(-aBlur.getRadius()*2,-aBlur.getRadius()*2,effImg.getWidth(),effImg.getHeight());
    RMFillPdfr.writeImageFill(ifill, null, bounds, aWriter);
}

/**
 * Writes pdf for given shadow effect and shape.
 */
public static void writeShadowEffect(RMShape aShape, ShadowEffect aShadow, RMPDFWriter aWriter)
{
    // Get effect image and image fill
    Image effImg = getEffectImage(aShape);
    RMImageFill ifill = new RMImageFill(effImg);
    
    // Get bounds for image fill and write
    double rad = aShadow.getRadius(), x = -rad*2 + aShadow.getDX(), y = -rad*2 + aShadow.getDY();
    Rect bounds = new Rect(x, y, effImg.getWidth(), effImg.getHeight());
    RMFillPdfr.writeImageFill(ifill, null, bounds, aWriter);
    
    // Do normal pdf write
    RMShapePdfr.getPdfr(aShape).writeShapeAll(aShape, aWriter);
}
    
/**
 * Writes pdf for given reflection effect and shape.
 */
private static void writeRefectionEffect(RMShape aShape, ReflectEffect aReflect, RMPDFWriter aWriter)
{
    // If valid reflection and fade heights, do reflection
    if(aReflect.getReflectHeight()>0 && aReflect.getFadeHeight()>0) {
    
        // Get reflection image for shape and fill
        Image refImg = getEffectImage(aShape);
        RMImageFill ifill = new RMImageFill(refImg);
        
        // Get bounds of reflected image and write
        Rect bounds = aShape.getBoundsStroked();
        bounds = new Rect(bounds.getX(), bounds.getMaxY() + aReflect.getGap(), refImg.getWidth(), refImg.getHeight());
        RMFillPdfr.writeImageFill(ifill, null, bounds, aWriter);
    }
    
    // Do normal write pdf
    RMShapePdfr.getPdfr(aShape).writeShapeAll(aShape, aWriter);
}
    
/**
 * Writes pdf for given emboss effect and shape.
 */
private static void writeEmbossEffect(RMShape aShape, EmbossEffect anEmboss, RMPDFWriter aWriter)
{
    // Get effect image and image fill
    Image effectImage = getEffectImage(aShape);
    RMImageFill ifill = new RMImageFill(effectImage);
    
    // Get bounds for image fill and write
    Rect bounds = new Rect(0, 0, effectImage.getWidth(), effectImage.getHeight());
    RMFillPdfr.writeImageFill(ifill, null, bounds, aWriter);
}
    
/**
 * Returns the effect image.
 */
private static Image getEffectImage(RMShape aShape)
{
    Effect eff = aShape.getEffect();
    Rect bnds = aShape.getBoundsStrokedDeep();
    PainterDVR pntr = new PainterDVR();
    aShape.paintShapeAll(pntr);
    if(eff instanceof BlurEffect) return ((BlurEffect)eff).getBlurImage(pntr, bnds);
    if(eff instanceof EmbossEffect) return ((EmbossEffect)eff).getEmbossImage(pntr, bnds);
    if(eff instanceof ReflectEffect) return ((ReflectEffect)eff).getReflectImage(pntr, bnds);
    if(eff instanceof ShadowEffect) return ((ShadowEffect)eff).getShadowImage(pntr, bnds);
    throw new RuntimeException("RMEffectPdfr.getEffectImage: Effect not supported " + eff);
}
    
}