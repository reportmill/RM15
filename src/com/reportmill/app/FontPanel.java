/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.*;
import snap.gfx.Font;
import snap.view.*;

/**
 * This class is a Font panel for selecting fonts. It lets the user easily select a font family,
 * font size and font style (bold, italic, underline, outline). It also has a convenient slider for interactively
 * changing the size and a text field for specifically setting a size. In addition, there is a pick list that
 * shows all the individual fonts available for a given family.
 */
public class FontPanel extends ViewOwner {
    
    // The EditorPane
    RMEditorPane    _editorPane;

/**
 * Creates a new FontPanel for EditorPane.
 */
public FontPanel(RMEditorPane anEP)  { _editorPane = anEP; }

/**
 * Returns the Editor.
 */
public RMEditor getEditor()  { return _editorPane.getEditor(); }

/**
 * Returns the EditorPane.
 */
public RMEditorPane getEditorPane()  { return _editorPane; }

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    setViewItems("FamilyList", RMFont.getFamilyNames());
    setViewItems("SizesList", new Object[] { 6,8,9,10,11,12,14,16,18,22,24,36,48,64,72,96,128,144 }); 
}

/**
 * Reset UI from the current selection.
 */
public void resetUI()
{
    // Get current font
    RMEditor editor = getEditor();
    RMFont font = RMEditorShapes.getFont(editor);
    
    // Get family name and size
    String familyName = font.getFamily();
    double size = font.getSize();
    
    // Reset FamilyList, SizesList, SizeText, SizeThumb, and Bold, Italic, Underline and Outline buttons
    setViewValue("FamilyList", familyName);
    setViewValue("FamilyText", familyName);
    setViewValue("SizesList", (int)size);
    setViewValue("SizeText", "" + size + " pt"); //setNodeValue("SizeThumb", size);
    setViewValue("BoldButton", font.isBold());
    setViewEnabled("BoldButton", font.getBold()!=null);
    setViewValue("ItalicButton", font.isItalic());
    setViewEnabled("ItalicButton", font.getItalic()!=null);
    setViewValue("UnderlineButton", RMEditorShapes.isUnderlined(editor));
    setViewValue("OutlineButton", RMEditorShapes.getTextBorder(editor)!=null);
    
    // Get font names in currently selected font's family
    String familyNames[] = RMFont.getFontNames(font.getFamily());
    
    // Reset FontNameComboBox Items, SelectedItem and Enabled
    setViewItems("FontNameComboBox", familyNames);
    String fn = font.getFontFile().getNativeName(); setViewSelectedItem("FontNameComboBox", fn);
    setViewEnabled("FontNameComboBox", familyNames.length>1);
}

/**
 * Respond to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get current editor
    RMEditor editor = getEditor();
    
    // Handle FontSizeUpButton
    if(anEvent.equals("FontSizeUpButton")) { Font font = RMEditorShapes.getFont(editor);
        RMEditorShapes.setFontSize(editor, font.getSize()<16? 1 : 2, true); }
    
    // Handle FontSizeDownButton
    if(anEvent.equals("FontSizeDownButton")) { Font font = RMEditorShapes.getFont(editor);
        RMEditorShapes.setFontSize(editor, font.getSize()<16? -1 : -2, true); }
    
    // Handle BoldButton
    if(anEvent.equals("BoldButton"))
        RMEditorShapes.setFontBold(editor, anEvent.getBoolValue());
    
    // Handle ItalicButton
    if(anEvent.equals("ItalicButton"))
        RMEditorShapes.setFontItalic(editor, anEvent.getBoolValue());
    
    // Handle UnderlineButton
    if(anEvent.equals("UnderlineButton"))
        RMEditorShapes.setUnderlined(editor);
    
    // Handle OutlineButton
    if(anEvent.equals("OutlineButton"))
        RMEditorShapes.setTextBorder(editor);
    
    // Handle SizeThumbwheel
    //if(anEvent.equals("SizeThumb")) RMEditorShapes.setFontSize(editor, anEvent.getIntValue(), false);
    
    // Handle SizesList
    if(anEvent.equals("SizesList") && anEvent.getValue()!=null)
        RMEditorShapes.setFontSize(editor, anEvent.getFloatValue(), false);
    
    // Handle SizeText
    if(anEvent.equals("SizeText")) {
        RMEditorShapes.setFontSize(editor, anEvent.getFloatValue(), false);
        editor.requestFocus();
    }

    // Handle FamilyList
    if(anEvent.equals("FamilyList")) {
        String familyName = getViewStringValue("FamilyList");
        String fontName = RMFont.getFontNames(familyName)[0];
        RMFont font = RMFont.getFont(fontName, 12);
        RMEditorShapes.setFontFamily(editor, font);
    }
    
    // Handle FontNameComboBox
    if(anEvent.equals("FontNameComboBox")) {
        RMFont font = RMFont.getFont(anEvent.getStringValue(), 12);
        RMEditorShapes.setFontName(editor, font);
    }
}
    
/** Returns the name for the inspector window. */
public String getWindowTitle()  { return "Font Panel"; }

}