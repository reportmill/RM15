/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import snap.gfx.*;
import snap.view.ViewEvent;

/**
 * UI editing for ReflectEffect.
 */
public class ReflectEffectTool extends EffectTool {
    
/**
 * Called to reset UI controls.
 */
public void resetUI()
{
    // Get currently selected effect, and get as reflect (or create new if not available)
    Effect eff = getSelectedEffect();
    ReflectEffect reff = eff instanceof ReflectEffect? (ReflectEffect)eff : new ReflectEffect();
    
    // Update ReflectionHeightSpinner, FadeHeightSpinner, GapHeightSpinner
    setViewValue("ReflectionHeightSpinner", reff.getReflectHeight());
    setViewValue("FadeHeightSpinner", reff.getFadeHeight());
    setViewValue("GapHeightSpinner", reff.getGap());
}

/**
 * Responds to changes from the UI panel controls and updates currently selected shape.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected effect, and get as reflect (or create new if not available)
    Effect eff = getSelectedEffect();
    ReflectEffect reff = eff instanceof ReflectEffect? (ReflectEffect)eff : new ReflectEffect();
    
    // Handle ReflectionHeightSpinner, FadeHeightSpinner, GapHeightSpinner
    if(anEvent.equals("ReflectionHeightSpinner")) reff = reff.copyForReflectHeight(anEvent.getFloatValue());
    if(anEvent.equals("FadeHeightSpinner")) reff = reff.copyForFadeHeight(anEvent.getFloatValue());
    if(anEvent.equals("GapHeightSpinner")) reff = reff.copyForGap(anEvent.getFloatValue());
    
    // Set new effect
    setSelectedEffect(reff);
}

}