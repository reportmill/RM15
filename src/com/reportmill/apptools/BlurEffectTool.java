/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import snap.gfx.*;
import snap.view.ViewEvent;

/**
 * UI editing for BlurEffect.
 */
public class BlurEffectTool extends EffectTool {

    /**
     * Called to reset UI controls.
     */
    public void resetUI()
    {
        // Get currently selected effect and blur effect
        Effect eff = getSelectedEffect();
        BlurEffect beff = eff instanceof BlurEffect ? (BlurEffect) eff : new BlurEffect();

        // Set BlurRadiusSpinner, BlurRadiusSlider
        setViewValue("BlurRadiusSpinner", beff.getRadius());
        setViewValue("BlurRadiusSlider", beff.getRadius());
    }

    /**
     * Responds to changes from the UI panel controls and updates currently selected shape.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Get currently selected effect and blur effect
        Effect eff = getSelectedEffect();
        BlurEffect beff = eff instanceof BlurEffect ? (BlurEffect) eff : new BlurEffect();

        // Handle BlurRadiusSpinner
        if (anEvent.equals("BlurRadiusSpinner"))
            beff = new BlurEffect(anEvent.getIntValue());

        // Handle BlurRadiusSlider
        if (anEvent.equals("BlurRadiusSlider")) {
            beff = new BlurEffect(anEvent.getIntValue());
            setViewValue("BlurRadiusSpinner", anEvent.getIntValue());
        }

        // Set new shadow effect
        setSelectedEffect(beff);
    }

}