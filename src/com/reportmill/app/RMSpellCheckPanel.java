/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import snap.text.SpellCheck;
import snap.view.*;
import snap.viewx.SpellCheckPanel;

/**
 * This class just runs a simple modal panel to register user name and email on first run.
 */
public class RMSpellCheckPanel extends SpellCheckPanel {

    // The editor
    private RMEditor _editor;

    // The current text being checked
    private RMTextShape _workingText;

    /**
     * Constructor.
     */
    public RMSpellCheckPanel()
    {
        super();
        SpellCheck.setSharedClass(RMSpellCheck.class);;
    }

    /**
     * Runs the spell check panel.
     */
    @Override
    public void show(View aView)
    {
        _editor = (RMEditor) aView;
        super.show(aView);
    }

    /**
     * Returns the text that this spell check panel should be checking.
     */
    protected String getTextString()
    {
        RMTextShape textShape = getTextShape();
        return textShape != null ? textShape.getText() : null;
    }

    protected int getTextLength()
    {
        RMTextEditor textEditor = _editor.getTextEditor();
        return textEditor.length();
    }

    protected int getSelEnd()
    {
        RMTextEditor textEditor = _editor.getTextEditor();
        return textEditor.getSelEnd();
    }

    protected void setSel(int aStart, int anEnd)
    {
        RMTextEditor textEditor = _editor.getTextEditor();
        textEditor.setSel(aStart, anEnd);
        RMTextShape textShape = getTextShape();
        textShape.repaint();
    }

    protected void replaceChars(String aString)
    {
        RMTextEditor textEditor = _editor.getTextEditor();
        textEditor.replace(aString);
    }

    /**
     * Returns the text that this spell check panel should be checking.
     */
    private RMTextShape getTextShape()
    {
        // Get main editor and selected shape
        RMShape selShape = _editor.getSelectedOrSuperSelectedShape();

        // If shape has changed do the right thing
        if (selShape != _workingText) {

            // If new shape is text, make it the working text
            if (selShape instanceof RMTextShape)
                _workingText = (RMTextShape) selShape;

            // Otherwise, set workingText to null
            else _workingText = null;
        }

        // Make sure working text is superselected
        if (_workingText != null && _workingText != _editor.getSuperSelectedShape()) {
            _editor.setSuperSelectedShape(_workingText);
            _editor.getTextEditor().setSel(0);
        }

        // Return
        return _workingText;
    }
}