/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.*;
import java.util.ArrayList;
import java.util.List;
import snap.gfx.*;
import snap.util.*;
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
    
    // Whether to show all fonts (or PDF only)
    boolean         _showAll = true;
    
    // The PDF Family names
    String          _pdfFonts[];

    // All PDF built in font family names.
    static String   _pdfBuiltIns[] = { "Arial", "Helvetica", "Times", "Courier", "Symbol", "ZapfDingbats" };

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
 * Returns the array of font family names.
 */
public String[] getFamilyNames()
{
    return _showAll? Font.getFamilyNames() : getPDFFamilyNames();
}

/**
 * Returns the array of font family names.
 */
public String[] getPDFFamilyNames()
{
    if(_pdfFonts!=null) return _pdfFonts;
    List pdfs = new ArrayList();
    for(String name : _pdfBuiltIns) { Font font = Font.get(name, 12);
        if(font!=null) pdfs.add(font.getFamily()); }
    return _pdfFonts = (String[])pdfs.toArray(new String[0]);
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get/configure FamilyList
    ListView <String> familyList = getView("FamilyList", ListView.class);
    familyList.setItems(getFamilyNames());
    
    // Get/configure FamilyComboBox
    ComboBox familyComboBox = getView("FamilyComboBox", ComboBox.class);
    familyComboBox.setListView(familyList);
    
    // Configure SizesList
    setViewItems("SizesList", new Object[] { 6,8,9,10,11,12,14,16,18,22,24,36,48,64,72,96,128,144 });
    
    // Configure SizeText
    TextField sizeText = getView("SizeText", TextField.class);
    sizeText.addPropChangeListener(pce -> { if(sizeText.isFocused()) sizeText.selectAll(); }, View.Focused_Prop);
}

/**
 * Reset UI from the current selection.
 */
public void resetUI()
{
    // Get current font
    RMEditor editor = getEditor();
    RMFont font = RMEditorUtils.getFont(editor);
    
    // Get family name and size
    String familyName = font.getFamily();
    double size = font.getSize();
    
    // Reset FamilyList, SizesList, SizeText, SizeThumb, and Bold, Italic, Underline and Outline buttons
    setViewItems("FamilyList", getFamilyNames());
    setViewSelItem("FamilyList", familyName);
    setViewValue("SizesList", (int)size);
    setViewValue("SizeText", StringUtils.toString(size) + " pt");
    setViewValue("BoldButton", font.isBold());
    setViewEnabled("BoldButton", font.getBold()!=null);
    setViewValue("ItalicButton", font.isItalic());
    setViewEnabled("ItalicButton", font.getItalic()!=null);
    setViewValue("UnderlineButton", RMEditorUtils.isUnderlined(editor));
    setViewValue("OutlineButton", RMEditorUtils.getTextBorder(editor)!=null);
    
    // Get font names in currently selected font's family
    String familyNames[] = RMFont.getFontNames(font.getFamily());
    
    // Reset FontNameComboBox Items, SelItem and Enabled
    setViewItems("FontNameComboBox", familyNames);
    String fn = font.getFontFile().getNativeName(); setViewSelItem("FontNameComboBox", fn);
    setViewEnabled("FontNameComboBox", familyNames.length>1);
}

/**
 * Respond to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get current editor
    RMEditor editor = getEditor();
    
    // Handle FontSizeUpButton, FontSizeDownButton
    if(anEvent.equals("FontSizeUpButton")) { Font font = RMEditorUtils.getFont(editor);
        RMEditorUtils.setFontSize(editor, font.getSize()<16? 1 : 2, true); }
    if(anEvent.equals("FontSizeDownButton")) { Font font = RMEditorUtils.getFont(editor);
        RMEditorUtils.setFontSize(editor, font.getSize()<16? -1 : -2, true); }
    
    // Handle BoldButton, ItalicButton, UnderlineButton, OutlineButton
    if(anEvent.equals("BoldButton")) RMEditorUtils.setFontBold(editor, anEvent.getBoolValue());
    if(anEvent.equals("ItalicButton")) RMEditorUtils.setFontItalic(editor, anEvent.getBoolValue());
    if(anEvent.equals("UnderlineButton")) RMEditorUtils.setUnderlined(editor);
    if(anEvent.equals("OutlineButton")) RMEditorUtils.setTextBorder(editor);
    
    // Handle FontPickerButton
    if(anEvent.equals("FontPickerButton")) {
        RMFont ofont = RMEditorUtils.getFont(editor);
        Font font = new FontPicker().showPicker(getEditorPane().getUI(), ofont);
        if(font!=null) { RMFont rfont = RMFont.get(font);
            RMEditorUtils.setFontFamily(editor, rfont); }
    }
    
    // Handle SizesList
    if(anEvent.equals("SizesList") && anEvent.getValue()!=null)
        RMEditorUtils.setFontSize(editor, anEvent.getFloatValue(), false);
    
    // Handle SizeText
    if(anEvent.equals("SizeText"))
        RMEditorUtils.setFontSize(editor, anEvent.getFloatValue(), false);

    // Handle FamilyList, FamilyComboBox
    if(anEvent.equals("FamilyList") || (anEvent.equals("FamilyComboBox") && anEvent.isActionEvent())) {
        String familyName = getViewStringValue("FamilyList");
        String fontNames[] = RMFont.getFontNames(familyName); if(fontNames.length==0) return;
        String fontName = fontNames[0];
        RMFont font = RMFont.getFont(fontName, 12);
        RMEditorUtils.setFontFamily(editor, font);
    }
    
    // Handle AllButton, PDFButton
    if(anEvent.equals("AllButton")) _showAll = true;
    if(anEvent.equals("PDFButton")) _showAll = false;
    
    // Handle FontNameComboBox
    if(anEvent.equals("FontNameComboBox")) {
        RMFont font = RMFont.getFont(anEvent.getStringValue(), 12);
        RMEditorUtils.setFontName(editor, font);
    }
}

/** Returns the name for the inspector window. */
public String getWindowTitle()  { return "Font Panel"; }

}