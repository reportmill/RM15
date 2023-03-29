package com.reportmill.app;
import com.reportmill.graphics.*;
import com.reportmill.shape.RMShape;
import snap.gfx.*;
import snap.styler.Styler;
import snap.util.Convert;
import snap.view.View;
import java.util.List;

/**
 * Sets style attributes.
 */
public class RMEditorStyler extends Styler {

    // The editor
    private RMEditor _editor;

    /**
     * Creates EditorStyler.
     */
    public RMEditorStyler(RMEditor anEditor)
    {
        _editor = anEditor;
    }

    /**
     * Returns the currently selected border.
     */
    public Border getBorder()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        RMStroke stroke = shape.getStroke();
        return convertStrokeToBorder(stroke);
    }

    /**
     * Sets the currently selected border.
     */
    public void setBorder(Border aBorder)
    {
        // Get stroke for border
        RMStroke stroke = convertBorderToStroke(aBorder);

        // Set in shapes
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            shape.setStroke(stroke);
    }

    /**
     * Returns the fill of currently selected view.
     */
    public Paint getFill()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        RMFill fill = shape.getFill();
        return fill.snap();
    }

    /**
     * Sets the fill of currently selected views.
     */
    public void setFill(Paint aPaint)
    {
        // Get fill for paint
        RMFill fill = null;

        // Set in shapes
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            shape.setFill(fill);
    }

    /**
     * Returns the text color current text.
     */
    public Color getTextColor()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return shape.getTextColor();
    }

    /**
     * Sets the text color current text.
     */
    public void setTextColor(Color aColor)
    {
        // Get text color
        RMColor textColor = RMColor.get(aColor);

        // Set in shapes
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            shape.setTextColor(textColor);
    }

    /**
     * Returns the font of editor's selected shape.
     */
    public Font getFont()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return shape.getFont();
    }

    /**
     * Sets the current font.
     */
    public void setFont(Font aFont)
    {
        // Get font
        RMFont font = RMFont.get(aFont);

        // Set in shapes
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            shape.setFont(font);
    }

    /**
     * Returns the outline state of the currently selected shape (null if none).
     */
    public Border getTextBorder()
    {
        return null;
    }

    /**
     * Sets the outline state of the currently selected shapes.
     */
    public void setTextBorder(Border aBorder)
    {
        setUndoTitle("Make Outlined");
    }

    /**
     * Returns the current effect.
     */
    public Effect getEffect()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return shape.getEffect();
    }

    /**
     * Sets the current effect.
     */
    public void setEffect(Effect anEffect)
    {
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            shape.setEffect(anEffect);
    }

    /**
     * Returns the current opacity.
     */
    public double getOpacity()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return shape.getOpacity();
    }

    /**
     * Sets the currently selected opacity.
     */
    public void setOpacity(double aValue)
    {
        setUndoTitle("Transparency Change");
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            shape.setOpacity(aValue);
    }

    /**
     * Returns the client View.
     */
    public View getClientView()  { return _editor; }

    /**
     * Sets undo title.
     */
    public void setUndoTitle(String aTitle)
    {
        _editor.undoerSetUndoTitle(aTitle);
    }

    /**
     * Returns a border for given stroke.
     */
    private static Border convertStrokeToBorder(RMStroke aStroke)
    {
        // Handle null
        if (aStroke == null)
            return null;

        // Handle BorderStroke
        if (aStroke instanceof RMBorderStroke) {
            RMBorderStroke bs = (RMBorderStroke) aStroke;
            return new Borders.EdgeBorder(bs.isShowTop(), bs.isShowRight(), bs.isShowBottom(), bs.isShowLeft());
        }

        // Handle normal stroke
        Color color = aStroke.getColor();
        Stroke stroke = aStroke.snap();
        return new Borders.LineBorder(color, stroke);
    }

    /**
     * Returns a border for given stroke.
     */
    private static RMStroke convertBorderToStroke(Border aBorder)
    {
        // Handle null
        if (aBorder == null)
            return null;

        // Handle EdgeBorder
        if (aBorder instanceof Borders.EdgeBorder) {
            Borders.EdgeBorder eb = (Borders.EdgeBorder) aBorder;
            return new RMBorderStroke(eb.isShowTop(), eb.isShowRight(), eb.isShowBottom(), eb.isShowLeft());
        }

        // Handle normal stroke
        RMColor color = RMColor.get(aBorder.getColor());
        Stroke borderStroke = aBorder.getStroke();
        RMStroke stroke = new RMStroke(color, borderStroke.getWidth());
        if (borderStroke.getDashArray() != null) {
            float[] dashArray = Convert.doubleArrayToFloat(borderStroke.getDashArray());
            stroke = stroke.deriveDashArray(dashArray);
        }
        if (borderStroke.getDashOffset() != 0)
            stroke = stroke.deriveDashPhase((float) borderStroke.getDashOffset());
        return stroke;
    }
}
