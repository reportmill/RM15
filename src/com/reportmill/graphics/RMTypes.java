/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.HPos;
import snap.gfx.VPos;

/**
 * Common types.
 */
public interface RMTypes {

    // Constants for X/Y alignment
    public enum Align {
        TopLeft, TopCenter, TopRight,
        CenterLeft, Center, CenterRight,
        BottomLeft, BottomCenter, BottomRight
    }
    
    // Constants for horizontal alignment
    public enum AlignX { Left, Right, Center, Full;
    
        // Conversions to/from Snap
        public HPos hpos() { return this==Left? HPos.LEFT : this==Center? HPos.CENTER : HPos.RIGHT; }
        public static AlignX get(HPos aPos) { return aPos==HPos.LEFT? Left : aPos==HPos.CENTER? Center : Right; }
    }

    // Constants for vertical alignment
    public enum AlignY { Top, Middle, Bottom;
    
        // Conversions to/from Snap
        public VPos vpos() { return this==Top? VPos.TOP : this==Middle? VPos.CENTER : VPos.BOTTOM; }
        public static AlignY get(VPos aPos) { return aPos==VPos.TOP? Top : aPos==VPos.CENTER? Middle : Bottom; }
    }
}