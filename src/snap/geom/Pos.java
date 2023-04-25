/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.geom;
import static snap.geom.HPos.*;
import static snap.geom.VPos.*;

/**
 * A class to represent a position on both horizonal and vertical axis (e.g. TOP_LEFT, BOTTOM_RIGHT, etc.).
 */
public enum Pos {

    // Top-Left
    TOP_LEFT(TOP, LEFT),

    // Top-Center
    TOP_CENTER(TOP, HPos.CENTER),

    // Top-Right
    TOP_RIGHT(TOP, RIGHT),

    // Center-Left
    CENTER_LEFT(VPos.CENTER, LEFT),

    // Center
    CENTER(VPos.CENTER, HPos.CENTER),

    // Center-Right
    CENTER_RIGHT(VPos.CENTER, RIGHT),

    // Bottom-Left
    BOTTOM_LEFT(BOTTOM, LEFT),

    // Bottom-Center
    BOTTOM_CENTER(BOTTOM, HPos.CENTER),

    // Bottom-Right
    BOTTOM_RIGHT(BOTTOM, RIGHT);

    // Baseline. Probably wont do this as Pos
    //BASELINE_LEFT(BASELINE, LEFT),
    //BASELINE_CENTER(BASELINE, HPos.CENTER),
    //BASELINE_RIGHT(BASELINE, RIGHT);

    // The Vertical position
    private final VPos vpos;

    // The Horizontal position
    private final HPos hpos;

    // Constant for all Outer Pos
    public static final Pos[] ALL_OUTER = new Pos[] {
            Pos.TOP_LEFT, Pos.TOP_CENTER, Pos.TOP_RIGHT,
            Pos.CENTER_LEFT, Pos.CENTER_RIGHT,
            Pos.BOTTOM_LEFT, Pos.BOTTOM_CENTER, Pos.BOTTOM_RIGHT
    };

    /**
     * Create new Pos.
     */
    Pos(VPos vpos, HPos hpos)
    {
        this.vpos = vpos; this.hpos = hpos;
    }

    /**
     * Returns the Horizontal position.
     */
    public HPos getHPos()  { return hpos; }

    /**
     * Returns the vertical position.
     */
    public VPos getVPos()  { return vpos; }

    /**
     * Returns whether position is side.
     */
    public boolean isSide()
    {
        return this == TOP_CENTER || this == CENTER_LEFT || this == CENTER_RIGHT || this == BOTTOM_CENTER;
    }

    /**
     * Returns whether position is corner.
     */
    public boolean isCorner()
    {
        return this == TOP_LEFT || this == TOP_RIGHT || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }

    /**
     * Returns the opposing position.
     */
    public Pos getOpposing()
    {
        switch (this) {
            case TOP_LEFT: return BOTTOM_RIGHT;
            case TOP_CENTER: return BOTTOM_CENTER;
            case TOP_RIGHT: return BOTTOM_LEFT;
            case CENTER_LEFT: return CENTER_RIGHT;
            case CENTER_RIGHT: return CENTER_LEFT;
            case BOTTOM_LEFT: return TOP_RIGHT;
            case BOTTOM_CENTER: return TOP_CENTER;
            case BOTTOM_RIGHT: return TOP_LEFT;
            default: return this;
        }
    }

    /**
     * Returns the Side most associated with position (corners go to LEFT/RIGHT, center to null).
     */
    public Side getSide()
    {
        switch (this) {
            case TOP_LEFT: return Side.LEFT;
            case TOP_CENTER: return Side.TOP;
            case TOP_RIGHT: return Side.RIGHT;
            case CENTER_LEFT: return Side.LEFT;
            case CENTER_RIGHT: return Side.RIGHT;
            case BOTTOM_LEFT: return Side.LEFT;
            case BOTTOM_CENTER: return Side.BOTTOM;
            case BOTTOM_RIGHT: return Side.RIGHT;
            default: return null;
        }
    }

    /**
     * Returns whether position is top or bottom.
     */
    public boolean isLeftOrRight()
    {
        Side side = getSide();
        return side != null && side.isLeftOrRight();
    }

    /**
     * Returns whether position is top or bottom.
     */
    public boolean isTopOrBottom()
    {
        Side side = getSide();
        return side != null && side.isTopOrBottom();
    }

    /**
     * Returns the Pos for a string.
     */
    public static Pos get(String aString)
    {
        String str = aString.toUpperCase();
        if (str.equals("LEFT"))
            return CENTER_LEFT;
        if (str.equals("RIGHT"))
            return CENTER_RIGHT;
        return valueOf(str);
    }

    /**
     * Returns a pos for given HPos and VPos.
     */
    public static Pos get(HPos aHP, VPos aVP)
    {
        if (aHP == null)
            aHP = HPos.CENTER;
        if (aVP == null)
            aVP = VPos.CENTER;
        for (Pos pos : values())
            if (pos.getHPos() == aHP && pos.getVPos() == aVP)
                return pos;
        throw new RuntimeException("Pos.get(H,V): Invalid arguement");
    }
}