/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Color;
import snap.util.*;
import snap.view.*;

/**
 * This class provides UI editing for shape animation.
 */
public class Animation extends RMEditorPane.SupportPane implements RMAnimator.Listener {
    
    // The key frames JList
    ListView <Integer>  _keyFramesList;
    
    // The changes JList
    ListView            _changesList;
    
    // Whether to allow update to time slider/text
    boolean             _update = true;
    
    // The list of key frames
    List <Integer>      _keyFrames;
    
    // The list of key frames for selected shapes
    List <Integer>      _selectedShapesKeyFrames;
    
    // The list of changes (keys) for key frame and selected shape
    List <String>       _changes = new Vector();

/**
 * Creates a new Animation pane for EditorPane.
 */
public Animation(RMEditorPane anEP)  { super(anEP); }

/**
 * Initialize UI for this inspector.
 */
protected void initUI()
{
    // Get KeyFrameList and customize
    _keyFramesList = getView("KeyFrameList", ListView.class);
    _keyFramesList.setCellConfigure(this :: configureKeyFrameList);
    
    // Get ChangesList and customize
    _changesList = getView("ChangesList", ListView.class);
    
    // Configure InterpolationComboBox
    updateInterpolatorCombobox();
}

/**
 * Populates the combobox with all the interpolator names if necessary.
 */
public void updateInterpolatorCombobox()
{
    String interps[] = new String[Interpolator.getInterpolatorCount()];
    for(int i=0; i<Interpolator.getInterpolatorCount(); i++) interps[i] = Interpolator.getInterpolator(i).getName();
    setViewItems("InterpolationComboBox", interps);
}

/**
 * Updates the UI panel controls from the current selection.
 */
public void resetUI()
{
    // Get the editor and current animator
    RMEditor editor = getEditor();
    RMAnimator animator = getAnimator(false);
    
    // If animator is null, replace with default instance
    if(animator==null) {
        animator = new RMAnimator();
        animator.setOwner(editor.getSuperSelectedParentShape());
    }
    
    // If animator is running, just return
    if(animator.isRunning()) return;
    
    // Get the currently selected shape and shapes
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    List shapes = editor.getSelectedOrSuperSelectedShapes();
    
    // Update TimeText, TimeSlider and TimeSlider Maximum
    setViewValue("TimeText", format(animator.getTimeSeconds()));
    setViewValue("TimeSlider", Math.round(animator.getTimeSeconds()*animator.getFrameRate()));
    getView("TimeSlider", Slider.class).setMax(Math.round(animator.getMaxTimeSeconds()*animator.getFrameRate()));
    
    // Update LoopCheckBox
    setViewValue("LoopCheckBox", animator.getLoops());
    
    // Update FrameRateText
    setViewValue("FrameRateText", animator.getFrameRate());
    
    // Update MaxTimeText
    setViewValue("MaxTimeText", animator.getMaxTimeSeconds());
    
    // If there wasn't really an animator, just return
    //if(getAnimator(false)==null) return;
    
    // Add this inspector as listener
    animator.addAnimatorListener(this);
    
    // Get animator key frames
    _keyFrames = animator.getKeyFrameTimes();
    
    // Get selected shapes key frames
    _selectedShapesKeyFrames = shape.isRoot()? null : animator.getKeyFrameTimes(shapes, true);
    
    // Reset KeyFrameList KeyFrames
    setViewItems(_keyFramesList, _keyFrames);

    // Get animator selected frame indices (start and end)
    int frameStartIndex = _keyFrames.indexOf(animator.getScopeTime());
    int frameEndIndex = _keyFrames.indexOf(animator.getTime());
    
    // If animator selected frames are adjacent, just select animator time
    if(frameEndIndex==frameStartIndex+1)
        frameStartIndex++;
        
    // If KeyFramesList and animator still don't match, reset keyFrameList
    _keyFramesList.setSelectionInterval(frameStartIndex, frameEndIndex);
    
    // Clear list of changes
    _changes.clear();
    
    // Get currently selected shape timeline and key frame
    RMTimeline timeline = shape.getTimeline();
    RMKeyFrame keyFrame = timeline!=null? timeline.getKeyFrameAt(animator.getTime()) : null;
    
    // If frame isn't empty, set changes to attributes at time
    if(keyFrame!=null)
        for(RMKeyValue kval : keyFrame.getKeyValues())
            _changes.add(kval.getKey());
    
    // Update ChangesList Changes
    setViewItems(_changesList, _changes);
        
    // Get selected change
    String change = getViewStringValue(_changesList);
    
    // Get key/value for change
    RMKeyValue keyValue = keyFrame!=null && change!=null? keyFrame.getKeyValue(shape, change) : null;
    
    // Get interpolator for change
    Interpolator interp = keyValue!=null? keyValue.getInterpolator() : null;
    
    // Update InterpolationComboBox (and enabled status)
    updateInterpolatorCombobox();
    setViewEnabled("InterpolationComboBox", keyValue!=null);
    setViewValue("InterpolationComboBox", interp!=null? interp.getName() : "Linear");

    // Update HelpText - one frame selected
    if(frameEndIndex-frameStartIndex>1) {
        String ts = getKeyFrameFormatted(frameStartIndex);
        setViewValue("HelpText", "All changes are made relative to start of selected range (" + ts + ").");
    }
    
    // Update HelpText - multiple frames selected
    else if(frameStartIndex>0 && frameStartIndex<=getKeyFrameCount()) {
        String ts = getKeyFrameFormatted(frameStartIndex - 1);
        setViewValue("HelpText", "Select multiple key frames to make changes across a range.\n" +
            "All changes are made relative to previous key frame (" + ts + ").");
    }
    
    // Update HelpText - all frames selected
    else setViewValue("HelpText", "Select multiple key frames to make changes across a range.\n" +
        "All changes are made relative to previous key frame.");
}

/**
 * Responds to changes from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the current animator (just return if null) - if running, stop it
    RMAnimator animator = getAnimator(true); if(animator==null) return;
    if(animator.isRunning())
        animator.stop();
    
    // Handle TimeSlider or TimeTextField
    if(anEvent.equals("TimeSlider"))
        setTimeSeconds(anEvent.getFloatValue()/animator.getFrameRate());

    // Handle TimeTextField
    if(anEvent.equals("TimeText"))
        setTimeSeconds(anEvent.getFloatValue());

    // Handle PlayButton
    if(anEvent.equals("PlayButton")) {
        animator.setResetTimeOnStop(true);
        animator.play();
    }
    
    // Handle StopButton
    if(anEvent.equals("StopButton"))
        animator.stop();
    
    // Handle StepButton
    if(anEvent.equals("StepButton"))
        setTime(animator.getTime() + animator.getInterval());
    
    // Handle BackButton
    if(anEvent.equals("BackButton"))
        setTime(animator.getTime() - animator.getInterval());
    
    // Handle LoopCheckBox
    if(anEvent.equals("LoopCheckBox"))
        animator.setLoops(anEvent.getBoolValue());

    // Handle FrameRateText
    if(anEvent.equals("FrameRateText"))
        animator.setFrameRate(anEvent.getFloatValue());

    // Handle MaxTimeText
    if(anEvent.equals("MaxTimeText"))
        animator.setMaxTimeSeconds(anEvent.getFloatValue());

    // Handle KeyFrameList
    if(anEvent.getView()==_keyFramesList) {
        int index = _keyFramesList.getSelectedIndexMax();
        if(index>=0 && index<getKeyFrameCount()) {
            if(index!=_keyFramesList.getSelectedIndexMin())
                setTimeForScopedKeyFrame(getKeyFrame(index), getKeyFrame(_keyFramesList.getSelectedIndexMin()));
            else setTime(getKeyFrame(index));
        }
    }

    // Handle freezeFrameButton
    if(anEvent.equals("FreezeFrameButton"))
        animator.addFreezeFrame();

    // Handle ShiftKeyFramesMenuItem
    if(anEvent.equals("ShiftFramesMenuItem")) {
        
        // Run option panel
        int time = animator.getTime();
        String msg = "Shift key frames from time " + time/1000f + " to end by time:";
        DialogBox dbox = new DialogBox("Shift Key Frames"); dbox.setQuestionMessage(msg);
        String shiftString = dbox.showInputDialog(getUI(), "0.0");
        int shift = shiftString==null? 0 : Math.round(StringUtils.floatValue(shiftString)*1000);

        // Shift frames
        if(shift!=0)
            animator.shiftFrames(time, shift);
    }

    // Handle ScaleFramesMenuItem
    if(anEvent.equals("ScaleFramesMenuItem")) {
        
        // Run option panel
        int maxTime = animator.getMaxTime();
        String msg = "Scale key frames from current frame to new max time";
        DialogBox dbox = new DialogBox("Scale Key Frames"); dbox.setQuestionMessage(msg);
        String newMaxTimeString = dbox.showInputDialog(getUI(), Float.toString(maxTime/1000f));
        int newMaxTime = newMaxTimeString==null? maxTime : Math.round(StringUtils.floatValue(newMaxTimeString)*1000);

        // Scale frames
        if(newMaxTime!=maxTime)
            animator.scaleFrames(animator.getTime(), newMaxTime);
    }
    
    // Handle DeleteButton
    if(anEvent.equals("DeleteButton"))
        delete();
    
    // Handle interpolationCombo
    if(anEvent.equals("InterpolationComboBox")) {
        RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
        RMTimeline timeline = shape.getTimeline();
        String interpName = anEvent.getStringValue();
        String change = (String)getViewSelectedItem(_changesList);
        RMKeyFrame keyFrame = timeline!=null? timeline.getKeyFrameAt(animator.getTime()) : null;
        RMKeyValue keyValue = keyFrame!=null? keyFrame.getKeyValue(shape, change) : null;
        if(keyValue!=null) {
            Interpolator interp = Interpolator.getInterpolator(interpName);
            keyValue.setInterpolator(interp); // Should derive instead?
        }
    }
}

/**
 * Returns the current animator from main editor super selected shape.
 */
private RMAnimator getAnimator(boolean create)
{
    RMEditor editor = getEditor();
    return editor!=null? editor.getSuperSelectedShape().getChildAnimator(create) : null;
}

/**
 * Returns the number of key frames for the current animator.
 */
private int getKeyFrameCount()  { return _keyFrames==null? 0 : _keyFrames.size(); }

/**
 * Returns the float time value of the key frame at the given index.
 */
private Integer getKeyFrame(int anIndex)  { return anIndex>=0? _keyFrames.get(anIndex) : null; }

/**
 * Returns the float time value of the key frame at the given index as a formatted string.
 */
private String getKeyFrameFormatted(int anIndex)  { return format(getKeyFrame(anIndex)); }

/**
 * Returns whether frame is "Freezable" (is intermediate frame with no changes).
 */
public boolean isFreezableFrame()
{
    RMAnimator anim = getAnimator(false);
    return anim!=null && !anim.isRunning() && anim.canFreezeFrame();
}

/**
 * Sets the time of the current animator to the given time.
 */
public void setTime(int aTime)  { setTimeForScopedKeyFrame(aTime, null); }

/**
 * Sets the time of the current animator to the given time.
 */
public void setTimeForScopedKeyFrame(int aTime, Integer aScope)
{
    RMAnimator animator = getAnimator(true);

    getEditor().undoerSetUndoTitle("Time Change");

    // Perform time change
    _update = false;
    animator.setScopeTime(aScope);
    animator.setTime(aTime);
    _update = true;
    
    if(aScope==null) {
        setViewSelectedIndex(_keyFramesList, -1);
        setViewValue(_keyFramesList, animator.getTime());
        setViewSelectedIndex(_changesList, -1);
    }
}

/**
 * Sets the time of the current animator to the given time.
 */
public void setTimeSeconds(float aTime)  { setTime(Math.round(aTime*1000)); }

/**
 * Handles delete of key frame(s) or change(s).
 */
public void delete()
{
    // Get editor and animator (just return if null)
    RMEditor editor = getEditor();
    RMAnimator animator = getAnimator(false); if(animator==null) return;
    
    // Get list, helper and selected range
    int keyFrameStart = _keyFramesList.getSelectedIndexMin();
    int keyFrameEnd = _keyFramesList.getSelectedIndexMax();
    int time = getKeyFrame(keyFrameStart);
    int time2 = getKeyFrame(keyFrameEnd);
    
    // If trying to delete frame 0, just beep and return
    if(keyFrameEnd==0) { beep(); return; }
    
    // If changes are selected, just delete them
    if(getViewSelectedIndex(_changesList)>=0) {
        
        // Get selected items from changes list
        String change = (String)getViewSelectedItem(_changesList);

        // Iterate over selected shapes
        for(int i=0, iMax=editor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
            
            // Get current loop selected shape and timeline (just continue if null)
            RMShape shape = editor.getSelectedOrSuperSelectedShape(i);
            RMTimeline timeline = shape.getTimeline(); if(timeline==null) continue;
            
            // Remove change from timeline for time range
            timeline.removeKeyFrameKeyValues(shape, change, time, time2, true);
            
            // Reset time
            shape.getUndoer().disable();
            int t = timeline.getTime(); timeline.setTime(0); timeline.setTime(t);
            shape.getUndoer().enable();
        }
    }

    // If no changes are selected, prompt user to delete all changes for key frame
    else {
        
        // Run panel for whether to delete changes
        String msg = "Do you really want to delete all changes\n" + "associated with this key frame(s)?";
        DialogBox dbox = new DialogBox("Delete Key Frame"); dbox.setWarningMessage(msg);
        dbox.setOptions("Delete", "Cancel");
        int response = dbox.showOptionDialog(getUI(), "Delete");
        
        // If approved, remove frames and reset main editor pane
        if(response==0)
            animator.removeFramesBetweenTimes(time, time2, true);
    }
}

/**
 * Override to customize KeyFramesList rendering.
 */
private void configureKeyFrameList(ListCell <Integer> aCell)
{
    // Get item time formatted and set
    Integer item = aCell.getItem(); if(item==null) return;
    String str = format(item/1000f); aCell.setText(str);
    
    // If not relevant to selected shape make brighter
    if(!aCell.isSelected() && !ListUtils.contains(_selectedShapesKeyFrames, item))
        aCell.setTextFill(Color.LIGHTGRAY);
}

/** Animator Listener method. */
public void animatorStarted(RMAnimator anAnimator)  { }
public void animatorStopped(RMAnimator anAnimator)  { }

/**
 * Animator Listener method : updates time slider and time text when animator has been updated.
 */
public void animatorUpdated(RMAnimator anAnimator)
{
    if(_update && anAnimator==getAnimator(true)) {
        setViewValue("TimeSlider", Math.round(anAnimator.getTimeSeconds()*anAnimator.getFrameRate()));
        setViewValue("TimeText", format(anAnimator.getTimeSeconds()));
    }
}

/**
 * Returns the name for this inspector.
 */
public String getWindowTitle()  { return "Animation"; }

/** Formats a number to 3 decimal places. */
private String format(double aValue)  { return String.format("%.3f", aValue); }

}