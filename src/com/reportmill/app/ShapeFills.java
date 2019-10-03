/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.List;
import snap.gfx.*;
import snap.view.*;

/**
 * This class provides UI for editing the currently selected shapes stroke, fill, effect, transparency.
 */
public class ShapeFills extends RMEditorPane.SupportPane {
    
    // The RMFillTool
    RMFillTool      _fillTool = new RMFillTool();
    
    // The EffectTool
    EffectTool      _effectTool = new EffectTool();
    
/**
 * Creates a new ShapeFills pane.
 */
public ShapeFills(RMEditorPane anEP)
{
    super(anEP);
    _fillTool.setEditorPane(anEP);
    _effectTool.setEditorPane(anEP);
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get array of known stroke names and initialize StrokeComboBox
    int scount = _fillTool.getStrokeCount();
    Object snames[] = new String[scount]; for(int i=0;i<scount;i++) snames[i] = _fillTool.getStroke(i).getName();
    setViewItems("StrokeComboBox", snames);
    
    // Get array of known fill names and initialize FillComboBox
    int fcount = _fillTool.getFillCount();
    Object fnames[] = new String[fcount]; for(int i=0;i<fcount;i++) fnames[i] = _fillTool.getFill(i).getName();
    setViewItems("FillComboBox", fnames);
    
    // Get array of known effect names and initialize EffectComboBox
    int ecount = _effectTool.getEffectCount();
    Object enames[] = new String[ecount]; for(int i=0;i<ecount;i++) enames[i] = _effectTool.getEffect(i).getName();
    setViewItems("EffectComboBox", enames);
}

/**
 * Reset UI controls from current selection.
 */
public void resetUI()
{
    // Get currently selected shape
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    
    // Get stroke from shape (or default, if not available)
    RMStroke stroke = shape.getStroke(); if(stroke==null) stroke = new RMStroke();

    // Update StrokeCheckBox, StrokeComboBox
    setViewValue("StrokeCheckBox", shape.getStroke()!=null);
    setViewValue("StrokeComboBox", stroke.getName());
    
    // Get stroke tool, install tool UI in stroke panel and ResetUI
    RMFillTool stool = _fillTool.getTool(stroke);
    getView("StrokePane", BoxView.class).setContent(stool.getUI());
    stool.resetLater();
    
    // Get fill from shape (or default, if not available)
    RMFill fill = shape.getFill(); if(fill==null) fill = new RMFill();

    // Update FillCheckBox, FillComboBox
    setViewValue("FillCheckBox", shape.getFill()!=null);
    setViewValue("FillComboBox", fill.getName());
    
    // Get fill tool, install tool UI in fill panel and ResetUI
    RMFillTool ftool = _fillTool.getTool(fill);
    getView("FillPane", BoxView.class).setContent(ftool.getUI());
    ftool.resetLater();
    
    // Get effect from shape (or default, if not available)
    Effect effect = shape.getEffect(); if(effect==null) effect = new ShadowEffect();

    // Update EffectCheckBox, EffectComboBox
    setViewValue("EffectCheckBox", shape.getEffect()!=null);
    setViewValue("EffectComboBox", effect.getName());
    
    // Get effect tool, install tool UI in effect panel and ResetUI
    EffectTool etool = _effectTool.getTool(effect);
    getView("EffectPane", BoxView.class).setContent(etool.getUI());
    etool.resetLater();
    
    // Update TransparencySlider, TransparencyText (transparency is opposite of opacity and on 0-100 scale)
    double transparency = 100 - shape.getOpacity()*100;
    setViewValue("TransparencySlider", transparency);
    setViewValue("TransparencyText", transparency);
}

/**
 * Updates currently selected shapes from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the current editor and currently selected shapes list (just return if null)
    RMEditor editor = getEditor(); if(editor==null) return;
    RMShape shape = editor.getSelectedOrSuperSelectedShape(); if(shape==null) return;
    List <RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();
    
    // Handle StrokeCheckBox: Iterate over shapes and add stroke if not there or remove if there
    if(anEvent.equals("StrokeCheckBox")) {
        boolean selected = anEvent.getBoolValue();
        for(RMShape s : shapes) {
            if(selected && s.getStroke()==null) s.setStroke(new RMStroke()); // If requested and missing, add
            if(!selected && s.getStroke()!=null) s.setStroke(null); // If turned off and present, remove
        }
    }
    
    // Handle StrokeComboBox: Get selected stroke instance and iterate over shapes and add stroke if not there
    if(anEvent.equals("StrokeComboBox")) {
        RMStroke newStroke = _fillTool.getStroke(anEvent.getSelIndex());
        for(RMShape s : shapes) s.setStroke(newStroke.clone());
    }

    // Handle FillCheckBox: Iterate over shapes and add fill if not there or remove if there
    if(anEvent.equals("FillCheckBox")) {
        boolean selected = anEvent.getBoolValue();
        for(RMShape s : shapes) {
            if(selected && s.getFill()==null) s.setFill(new RMFill()); // If requested and missing, add
            if(!selected && s.getFill()!=null) s.setFill(null); // If turned off and present, remove
        }
    }
    
    // Handle FillComboBox: Get selected fill instance and iterate over shapes and add fill if not there
    if(anEvent.equals("FillComboBox")) {
        RMFill newFill = _fillTool.getFill(anEvent.getSelIndex());
        for(RMShape s : shapes) s.setFill(newFill.deriveFill(s.getFill()));
    }

    // Handle EffectCheckBox: Iterate over shapes and add effect if not there or remove if there
    if(anEvent.equals("EffectCheckBox")) {
        boolean selected = anEvent.getBoolValue();
        for(RMShape s : shapes) {
            if(selected && s.getEffect()==null) s.setEffect(new ShadowEffect()); // If requested and missing, add
            if(!selected && s.getEffect()!=null) s.setEffect(null); // If turned off and present, remove
        }
    }
    
    // Handle EffectComboBox: Get selected effect instance and iterate over shapes and add effect if not there
    if(anEvent.equals("EffectComboBox")) {
        Effect eff = _effectTool.getEffect(anEvent.getSelIndex());
        for(RMShape s : shapes) s.setEffect(eff);
    }

    // Handle Transparency Slider and Text
    if(anEvent.equals("TransparencySlider") || anEvent.equals("TransparencyText")) {
        shape.undoerSetUndoTitle("Transparency Change");
        double eval = anEvent.equals("TransparencySlider")? anEvent.getIntValue() : anEvent.getFloatValue();
        double val = 1 - eval/100;
        for(RMShape s : shapes)
            s.setOpacity(val);
    }
}

/**
 * Returns the display name for the inspector.
 */
public String getWindowTitle()  { return "Paint/Fill Inspector"; }

}