/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.*;
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
    private RMEditorPane _editorPane;

    // Whether to show all fonts (or PDF only)
    private boolean _showAll = true;

    // The PDF Family names
    private String[] _pdfFonts;

    // All PDF built in font family names.
    private static String[] _pdfBuiltIns = {"Arial", "Helvetica", "Times", "Courier", "Symbol", "ZapfDingbats"};

    /**
     * Creates a new FontPanel for EditorPane.
     */
    public FontPanel(RMEditorPane anEP)
    {
        _editorPane = anEP;
    }

    /**
     * Returns the Editor.
     */
    public RMEditor getEditor()
    {
        return _editorPane.getEditor();
    }

    /**
     * Returns the EditorPane.
     */
    public RMEditorPane getEditorPane()
    {
        return _editorPane;
    }

    /**
     * Returns the array of font family names.
     */
    public String[] getFamilyNames()
    {
        return _showAll ? Font.getFamilyNames() : getPDFFamilyNames();
    }

    /**
     * Returns the array of font family names.
     */
    public String[] getPDFFamilyNames()
    {
        if (_pdfFonts != null) return _pdfFonts;

        Font[] pdfFonts = ArrayUtils.mapNonNull(_pdfBuiltIns, fontName -> Font.getFont(fontName, 12), Font.class);
        String[] familyNames = ArrayUtils.map(pdfFonts, font -> font.getFamily(), String.class);
        return _pdfFonts = familyNames;
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get/configure FamilyList
        ListView<String> familyList = getView("FamilyList", ListView.class);
        familyList.setItems(getFamilyNames());

        // Get/configure FamilyComboBox
        ComboBox<String> familyComboBox = getView("FamilyComboBox", ComboBox.class);
        familyComboBox.setListView(familyList);

        // Configure SizesList
        setViewItems("SizesList", new Object[]{6, 8, 9, 10, 11, 12, 14, 16, 18, 22, 24, 36, 48, 64, 72, 96, 128, 144});

        // Configure SizeText
        TextField sizeText = getView("SizeText", TextField.class);
        sizeText.addPropChangeListener(pce -> {
            if (sizeText.isFocused()) sizeText.selectAll();
        }, View.Focused_Prop);
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
        setViewValue("SizesList", (int) size);
        setViewValue("SizeText", StringUtils.toString(size) + " pt");
        setViewValue("BoldButton", font.isBold());
        setViewEnabled("BoldButton", font.getBold() != null);
        setViewValue("ItalicButton", font.isItalic());
        setViewEnabled("ItalicButton", font.getItalic() != null);
        setViewValue("UnderlineButton", RMEditorUtils.isUnderlined(editor));

        // Update OutlineButton
        RMEditorStyler editorStyler = editor.getStyler();
        setViewValue("OutlineButton", editorStyler.isTextOutlined());

        // Get font names in currently selected font's family
        String[] familyNames = RMFont.getFontNames(font.getFamily());

        // Reset FontNameComboBox Items, SelItem and Enabled
        setViewItems("FontNameComboBox", familyNames);
        String fn = font.getFontFile().getNativeName();
        setViewSelItem("FontNameComboBox", fn);
        setViewEnabled("FontNameComboBox", familyNames.length > 1);
    }

    /**
     * Respond to UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Get current editor
        RMEditor editor = getEditor();
        RMEditorStyler editorStyler = editor.getStyler();

        // Handle FontSizeUpButton, FontSizeDownButton
        if (anEvent.equals("FontSizeUpButton")) {
            Font font = RMEditorUtils.getFont(editor);
            RMEditorUtils.setFontSize(editor, font.getSize() < 16 ? 1 : 2, true);
        }
        if (anEvent.equals("FontSizeDownButton")) {
            Font font = RMEditorUtils.getFont(editor);
            RMEditorUtils.setFontSize(editor, font.getSize() < 16 ? -1 : -2, true);
        }

        // Handle BoldButton, ItalicButton, UnderlineButton, OutlineButton
        if (anEvent.equals("BoldButton")) RMEditorUtils.setFontBold(editor, anEvent.getBoolValue());
        if (anEvent.equals("ItalicButton")) RMEditorUtils.setFontItalic(editor, anEvent.getBoolValue());
        if (anEvent.equals("UnderlineButton")) RMEditorUtils.setUnderlined(editor);
        if (anEvent.equals("OutlineButton"))
            editorStyler.setTextOutlined(anEvent.getBoolValue());

        // Handle FontPickerButton
        if (anEvent.equals("FontPickerButton")) {
            RMFont ofont = RMEditorUtils.getFont(editor);
            Font font = new FontPicker().showPicker(getEditorPane().getUI(), ofont);
            if (font != null) {
                RMFont rfont = RMFont.get(font);
                RMEditorUtils.setFontFamily(editor, rfont);
            }
        }

        // Handle SizesList
        if (anEvent.equals("SizesList") && anEvent.getValue() != null)
            RMEditorUtils.setFontSize(editor, anEvent.getFloatValue(), false);

        // Handle SizeText
        if (anEvent.equals("SizeText"))
            RMEditorUtils.setFontSize(editor, anEvent.getFloatValue(), false);

        // Handle FamilyList, FamilyComboBox
        if (anEvent.equals("FamilyList") || (anEvent.equals("FamilyComboBox") && anEvent.isActionEvent())) {
            String familyName = getViewStringValue("FamilyList");
            String[] fontNames = RMFont.getFontNames(familyName);
            if (fontNames.length == 0)
                return;
            String fontName = fontNames[0];
            RMFont font = RMFont.getFont(fontName, 12);
            RMEditorUtils.setFontFamily(editor, font);
        }

        // Handle AllButton, PDFButton
        if (anEvent.equals("AllButton")) _showAll = true;
        if (anEvent.equals("PDFButton")) _showAll = false;

        // Handle FontNameComboBox
        if (anEvent.equals("FontNameComboBox")) {
            RMFont font = RMFont.getFont(anEvent.getStringValue(), 12);
            RMEditorUtils.setFontName(editor, font);
        }
    }

    /**
     * Returns the name for the inspector window.
     */
    public String getWindowTitle()  { return "Font Panel"; }
}