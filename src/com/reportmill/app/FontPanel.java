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
    
    // The FamilyText TextField
    TextField       _familyText;
    
    // The FamilyList ListView
    ListView        _familyList;
    
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
    _familyList = getView("FamilyList", ListView.class);
    setViewItems(_familyList, getFamilyNames());
    
    // Get/configure FamilyTest
    _familyText = getView("FamilyText", TextField.class);
    enableEvents(_familyText, KeyType);
    _familyText.addPropChangeListener(pce -> {
        if(_familyText.isFocused()) _familyText.selectAll();
        else resetLater();
    }, View.Focused_Prop);
    
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
    RMFont font = RMEditorShapes.getFont(editor);
    
    // Get family name and size
    String familyName = font.getFamily();
    double size = font.getSize();
    
    // If FamilyText focused, set FamilyList to font family names matching FamilyText prefix
    if(_familyText.isFocused()) {
        boolean selEmpty = _familyText.isSelEmpty(); int selStart = _familyText.getSelStart();
        String text = _familyText.getText(); if(!selEmpty) text = text.substring(0, selStart);
        List <String> fnames = getFamilyNamesForPrefix(selStart>0? text : "");
        String fname = selStart>0? (fnames.size()>0? fnames.get(0) : text) : familyName;
        _familyList.setItems(fnames);
        _familyList.setSelectedItem(fname);
        if(fname!=null) {
            _familyText.setText(fname);
            _familyText.setSel(selStart, fname.length());
        }
    }
    
    // Do normal reset FamilyList, FamilyText
    else {
        _familyList.setItems((Object[])getFamilyNames());
        _familyList.setSelectedItem(familyName);
        _familyText.setText(familyName);
    }
    
    // Reset SizesList, SizeText, SizeThumb, and Bold, Italic, Underline and Outline buttons
    setViewValue("SizesList", (int)size);
    setViewValue("SizeText", StringUtils.toString(size) + " pt");
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
    
    // Handle FontSizeUpButton, FontSizeDownButton
    if(anEvent.equals("FontSizeUpButton")) { Font font = RMEditorShapes.getFont(editor);
        RMEditorShapes.setFontSize(editor, font.getSize()<16? 1 : 2, true); }
    if(anEvent.equals("FontSizeDownButton")) { Font font = RMEditorShapes.getFont(editor);
        RMEditorShapes.setFontSize(editor, font.getSize()<16? -1 : -2, true); }
    
    // Handle BoldButton, ItalicButton, UnderlineButton, OutlineButton
    if(anEvent.equals("BoldButton"))
        RMEditorShapes.setFontBold(editor, anEvent.getBoolValue());
    if(anEvent.equals("ItalicButton"))
        RMEditorShapes.setFontItalic(editor, anEvent.getBoolValue());
    if(anEvent.equals("UnderlineButton"))
        RMEditorShapes.setUnderlined(editor);
    if(anEvent.equals("OutlineButton"))
        RMEditorShapes.setTextBorder(editor);
    
    // Handle FontPickerButton
    if(anEvent.equals("FontPickerButton")) {
        RMFont ofont = RMEditorShapes.getFont(editor);
        Font font = new FontPicker().showPicker(editor, ofont);
        if(font!=null) { RMFont rfont = RMFont.get(font);
            RMEditorShapes.setFontFamily(editor, rfont); }
    }
    
    // Handle SizesList
    if(anEvent.equals("SizesList") && anEvent.getValue()!=null)
        RMEditorShapes.setFontSize(editor, anEvent.getFloatValue(), false);
    
    // Handle SizeText
    if(anEvent.equals("SizeText"))
        RMEditorShapes.setFontSize(editor, anEvent.getFloatValue(), false);

    // Handle FamilyList
    if(anEvent.equals("FamilyList") || (anEvent.equals("FamilyText") && anEvent.isActionEvent())) {
        String familyName = getViewStringValue("FamilyList");
        String fontName = RMFont.getFontNames(familyName)[0];
        RMFont font = RMFont.getFont(fontName, 12);
        RMEditorShapes.setFontFamily(editor, font);
    }
    
    // Handle FamilyText
    if(anEvent.equals("FamilyText")) {
        
        // Handle BackSpace key: fire deleteBackward(), since first normal key just clears completion text
        if(anEvent.isKeyType() && anEvent.isBackSpaceKey())
            _familyText.deleteBackward();
    }
    
    // Handle AllButton, PDFButton
    if(anEvent.equals("AllButton")) _showAll = true;
    if(anEvent.equals("PDFButton")) _showAll = false;
    
    // Handle FontNameComboBox
    if(anEvent.equals("FontNameComboBox")) {
        RMFont font = RMFont.getFont(anEvent.getStringValue(), 12);
        RMEditorShapes.setFontName(editor, font);
    }
}

/**
 * Return the family name for prefix.
 */
private List <String> getFamilyNamesForPrefix(String aPrefix)
{
    String fnames[] = getFamilyNames();
    List <String> list = new ArrayList();
    for(String name : fnames) if(StringUtils.startsWithIC(name, aPrefix)) list.add(name);
    return list;
}
    
/** Returns the name for the inspector window. */
public String getWindowTitle()  { return "Font Panel"; }

}