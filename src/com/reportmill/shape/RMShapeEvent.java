/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.Point;
import snap.view.*;

/**
 * A ViewEvent subclass which is sent to an RMShape's event listeners.
 */
public class RMShapeEvent extends ViewEvent {

    // The original event
    ViewEvent           _event;
    
    // The point in shape coords
    Point               _point;
    
/**
 * Creates a new RMShapeEvent.
 */
public RMShapeEvent(RMShape aShape, ViewEvent anEvent, Point aPoint, ViewEvent.Type aType)
{
    setView(anEvent.getView()); setEvent(_event = anEvent); _point = aPoint; setType(aType);
}

/** Returns whether alt key is down. */
public boolean isAltDown()  { return _event.isAltDown(); }

/** Returns whether control key is down. */
public boolean isControlDown()  { return _event.isControlDown(); }

/** Returns whether "meta" key is down (the command key on Mac with no equivalent on Windows). */
public boolean isMetaDown()  { return _event.isMetaDown(); }

/** Returns whether shift key is down. */
public boolean isShiftDown()  { return _event.isShiftDown(); }

/** Returns whether shortcut key is pressed. */
public boolean isShortcutDown()  { return _event.isShortcutDown(); }

/** Returns whether popup trigger is down. */
public boolean isPopupTrigger()  { return _event.isPopupTrigger(); }

/** Override to return point in shape coords. */
public double getX()  { return _point.getX(); }

/** Override to return point in shape coords. */
public double getY()  { return _point.getY(); }

/** Overrides to return point in shape coords. */
public Point getPoint()  { return _point; }

/** Override to forward to encapsulated event. */
public int getClickCount()  { return _event.getClickCount(); }

/** Override to forward to encapsulated event. */
public void consume()  { super.consume(); _event.consume(); }

/** Override to forward to encapsulated event. */
protected Type getTypeImpl()  { return _event.getType(); }

}