/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Size;

/**
 * This class represents a simple geometric size. Legacy, use snap.gfx.Size.
 */
public class RMSize extends Size {
    
/** Creates a size initialized to 0,0. */
public RMSize() { }

/** Creates a size initialized to the given width and height. */
public RMSize(double w, double h)  { super(w,h); }

}