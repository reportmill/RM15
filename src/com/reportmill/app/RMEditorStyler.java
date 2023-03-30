package com.reportmill.app;
import com.reportmill.graphics.*;
import com.reportmill.shape.RMDocument;
import com.reportmill.shape.RMPage;
import com.reportmill.shape.RMShape;
import com.reportmill.shape.RMTextShape;
import snap.gfx.*;
import snap.styler.Styler;
import snap.util.Convert;
import snap.view.View;
import snap.view.ViewUtils;

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
        return getBorderForShape(shape);
    }

    /**
     * Sets the currently selected border.
     */
    public void setBorder(Border aBorder)
    {
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            setBorderForShape(shape, aBorder);
    }

    /**
     * Returns the fill of currently selected view.
     */
    public Paint getFill()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return getFillForShape(shape);
    }

    /**
     * Sets the fill of currently selected views.
     */
    public void setFill(Paint aPaint)
    {
        // If meta key down, convert color to gradient (Might be a duplication & hack - but this can get called directly)
        if (aPaint instanceof Color && ViewUtils.isMetaDown()) {
            Color color1 = getFill() != null ? getFillColor() : Color.CLEARWHITE;
            aPaint = new GradientPaint(color1, (Color) aPaint, 0);
        }

        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            setFillForShape(shape, aPaint);
    }

    /**
     * Returns the text color current text.
     */
    public Color getTextColor()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return getTextColorForShape(shape);
    }

    /**
     * Sets the text color current text.
     */
    public void setTextColor(Color aColor)
    {
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            setTextColorForShape(shape, aColor);
    }

    /**
     * Returns the outline state of the currently selected shape (null if none).
     */
    public Border getTextBorder()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return getTextBorderForShape(shape);
    }

    /**
     * Sets the outline state of the currently selected shapes.
     */
    public void setTextBorder(Border aBorder)
    {
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            setTextBorderForShape(shape, aBorder);
    }

    /**
     * Returns whether there is a text outline.
     */
    public boolean isTextOutlined()  { return getTextBorder() != null; }

    /**
     * Sets the currently selected shapes to be outlined.
     */
    public void setTextOutlined(boolean aValue)
    {
        // Get new text border and color and set
        Border newTextBorder = aValue ? Border.blackBorder() : null;
        Color newTextColor = aValue ? Color.WHITE : Color.BLACK;
        setTextBorder(newTextBorder);
        setTextColor(newTextColor);
    }

    /**
     * Returns the font of editor's selected shape.
     */
    public Font getFont()
    {
        RMShape shape = _editor.getSelectedOrSuperSelectedShape();
        return getFontForShape(shape);
    }

    /**
     * Sets the current font.
     */
    public void setFont(Font aFont)
    {
        List<RMShape> shapes = _editor.getSelectedOrSuperSelectedShapes();
        for (RMShape shape : shapes)
            setFontForShape(shape, aFont);
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
     * Returns the currently selected border.
     */
    public Border getBorderForShape(RMShape aShape)
    {
        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null)
            return textEditor.getTextBorder();

        // Convert stroke
        RMStroke stroke = aShape.getStroke();
        return convertStrokeToBorder(stroke);
    }

    /**
     * Sets the currently selected border.
     */
    public void setBorderForShape(RMShape aShape, Border aBorder)
    {
        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null) {
            textEditor.setTextBorder(aBorder);
            return;
        }

        // Get stroke for border and set
        RMStroke stroke = convertBorderToStroke(aBorder);
        aShape.setStroke(stroke);
    }

    /**
     * Returns the fill of currently selected view.
     */
    public Paint getFillForShape(RMShape aShape)
    {
        // If shape is doc/page, return default color
        if ((aShape instanceof RMPage || aShape instanceof RMDocument) && aShape.getFill() == null)
            return Color.WHITE;

        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null)
            return textEditor.getColor();

        // Get shape fill, convert and return
        RMFill shapeFill = aShape.getFill();
        Paint fill = shapeFill != null ? shapeFill.snap() : null;
        return fill;
    }

    /**
     * Sets the fill of currently selected views.
     */
    public void setFillForShape(RMShape aShape, Paint aPaint)
    {
        // If shape is doc/page, suppress
        if ((aShape instanceof RMPage || aShape instanceof RMDocument) && aShape.getFill() == null)
            return;

        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null) {
            Color color = aPaint != null ? aPaint.getColor() : null;
            RMColor rmcolor = color != null ? RMColor.get(color) : null;
            textEditor.setColor(rmcolor);
            return;
        }

        // Get fill for paint
        RMFill fill = null;
        if (aPaint instanceof Color)
            fill = new RMFill(RMColor.get((Color) aPaint));
        else if (aPaint instanceof GradientPaint)
            fill = new RMGradientFill((GradientPaint) aPaint);
        else if (aPaint instanceof ImagePaint)
            fill = new RMImageFill((ImagePaint) aPaint);

        // Set in shape
        aShape.setFill(fill);
    }

    /**
     * Returns the text color current text.
     */
    public Color getTextColorForShape(RMShape aShape)
    {
        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null)
            return textEditor.getColor();

        // Return
        return aShape.getTextColor();
    }

    /**
     * Sets the text color current text.
     */
    public void setTextColorForShape(RMShape aShape, Color aColor)
    {
        // Get text color
        RMColor textColor = RMColor.get(aColor);

        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null) {
            textEditor.setColor(textColor);
            return;
        }

        // Set in shape
        aShape.setTextColor(textColor);
    }

    /**
     * Returns the outline state of the currently selected shape (null if none).
     */
    public Border getTextBorderForShape(RMShape aShape)
    {
        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null)
            return textEditor.getTextBorder();

        // Handle RMTextShape
        if (aShape instanceof RMTextShape)
            return ((RMTextShape) aShape).getTextBorder();

        // Return not found
        return null;
    }

    /**
     * Sets the outline state of the currently selected shapes.
     */
    public void setTextBorderForShape(RMShape aShape, Border aBorder)
    {
        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null) {
            setUndoTitle("Make Outlined");
            textEditor.setTextBorder(aBorder);
            return;
        }

        // Handle RMTextShape
        if (aShape instanceof RMTextShape) {
            setUndoTitle("Make Outlined");
            ((RMTextShape) aShape).setTextBorder(aBorder);
        }
    }

    /**
     * Returns the font of editor's selected shape.
     */
    public Font getFontForShape(RMShape aShape)
    {
        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null)
            return textEditor.getFont();

        // Return shape font
        return aShape.getFont();
    }

    /**
     * Sets the current font.
     */
    public void setFontForShape(RMShape aShape, Font aFont)
    {
        RMFont font = RMFont.get(aFont);

        // Handle RMTextShape + TextEditorSet
        RMTextEditor textEditor = getTextEditorForShape(aShape);
        if (textEditor != null) {
            textEditor.setFont(font);
            return;
        }

        // Set in shape
        aShape.setFont(font);
    }

    /**
     * Returns the TextEditor if shape is RMTextShape with TextEditorSet.
     */
    private static RMTextEditor getTextEditorForShape(RMShape aShape)
    {
        // Handle RMTextShape with TextEditorSet
        if (aShape instanceof RMTextShape) {
            RMTextShape textShape = (RMTextShape) aShape;
            if (textShape.isTextEditorSet())
                return textShape.getTextEditor();
        }

        // Return not found
        return null;
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
