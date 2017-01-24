/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.base.RMFormat;
import com.reportmill.base.RMNumberFormat;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Font;
import snap.util.*;
import snap.view.*;
import snap.viewx.ColorButton;

/**
 * Tool bar for RMEditorPane.
 */
public class RMEditorPaneToolBar extends RMEditorPane.SupportPane {

    // The font face ComboBox
    ComboBox          _fontFaceComboBox;
    
    // The font size ComboBox
    ComboBox          _fontSizeComboBox;
    
    // The toolbar tools
    RMTool            _toolBarTools[];
    
/**
 * Creates a new editor pane tool bar.
 */
public RMEditorPaneToolBar(RMEditorPane anEP)
{
    super(anEP);
    _toolBarTools = createToolBarTools();
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Get/configure FontFaceComboBox
    _fontFaceComboBox = getView("FontFaceComboBox", ComboBox.class);
    _fontFaceComboBox.setItems((Object[])Font.getFamilyNames());
    
    // Get/configure FontSizeComboBox
    _fontSizeComboBox = getView("FontSizeComboBox", ComboBox.class);
    Object sizes[] = { 6, 8, 9, 10, 11, 12, 14, 16, 18, 22, 24, 36, 48, 64, 72, 96, 128, 144 };
    _fontSizeComboBox.setItems(sizes);
}

/**
 * Updates the UI panel controls.
 */
protected void resetUI()
{
    // Get the editor
    RMEditor editor = getEditor();
    Font font = RMEditorShapes.getFont(editor);
    
    // Update UndoButton, RedoButton
    Undoer undoer = editor.getUndoer();
    setViewEnabled("UndoButton", undoer!=null && undoer.getUndoSetLast()!=null);
    setViewEnabled("RedoButton", undoer!=null && undoer.getRedoSetLast()!=null);
    
    // Update MoneyButton, PercentButton, CommaButton
    RMFormat fmt = RMEditorShapes.getFormat(editor);
    RMNumberFormat nfmt = fmt instanceof RMNumberFormat? (RMNumberFormat)fmt : null;
    setViewValue("MoneyButton", nfmt!=null && nfmt.isLocalCurrencySymbolUsed());
    setViewValue("PercentButton", nfmt!=null && nfmt.isPercentSymbolUsed());
    setViewValue("CommaButton", nfmt!=null && nfmt.isGroupingUsed());
        
    // Reset PreviewEditButton state if out of sync
    if(getViewBoolValue("PreviewEditButton")==getEditorPane().isEditing())
        setViewValue("PreviewEditButton", !getEditorPane().isEditing());

    // Get selected tool button name and button - if found and not selected, select it
    String toolButtonName = editor.getCurrentTool().getClass().getSimpleName() + "Button";
    ToggleButton toolButton = getView(toolButtonName, ToggleButton.class);
    if(toolButton!=null && !toolButton.isSelected())
        toolButton.setSelected(true);
        
    // Reset FontFaceComboBox, FontSizeComboBox
    _fontFaceComboBox.setSelectedItem(font.getFamily());
    _fontSizeComboBox.setText(StringUtils.toString(font.getSize()) + " pt");
        
    // Reset BoldButton, ItalicButton, UnderlineButton
    setViewValue("BoldButton", font.isBold());
    setViewEnabled("BoldButton", font.getBold()!=null);
    setViewValue("ItalicButton", font.isItalic());
    setViewEnabled("ItalicButton", font.getItalic()!=null);
    setViewValue("UnderlineButton", RMEditorShapes.isUnderlined(editor));
    
    // Update AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton, AlignTopButton, AlignMiddleButton
    RMTypes.AlignX alignX = RMEditorShapes.getAlignmentX(editor);
    setViewValue("AlignLeftButton", alignX==RMTypes.AlignX.Left);
    setViewValue("AlignCenterButton", alignX==RMTypes.AlignX.Center);
    setViewValue("AlignRightButton", alignX==RMTypes.AlignX.Right);
    setViewValue("AlignFullButton", alignX==RMTypes.AlignX.Full);
}

/**
 * Responds to UI panel control changes.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Get the editor
    RMEditorPane epane = getEditorPane();
    RMEditor editor = getEditor();
    
    // Handle File NewButton, OpenButton, SaveButton, PreviewPDFButton, PreviewHTMLButton, PrintButton
    if(anEvent.equals("NewButton")) epane.respondUI(anEvent);
    if(anEvent.equals("OpenButton")) epane.respondUI(anEvent);
    if(anEvent.equals("SaveButton")) epane.respondUI(anEvent);
    if(anEvent.equals("PreviewPDFButton")) epane.respondUI(anEvent);
    if(anEvent.equals("PreviewHTMLButton")) epane.respondUI(anEvent);
    if(anEvent.equals("PrintButton")) epane.respondUI(anEvent);
        
    // Handle Edit CutButton, CopyButton, PasteButton, DeleteButton
    if(anEvent.equals("CutButton")) epane.respondUI(anEvent);
    if(anEvent.equals("CopyButton")) epane.respondUI(anEvent);
    if(anEvent.equals("PasteButton")) epane.respondUI(anEvent);
    if(anEvent.equals("DeleteButton")) editor.delete();
        
    // Handle Edit UndoButton, RedoButton
    if(anEvent.equals("UndoButton")) epane.respondUI(anEvent);
    if(anEvent.equals("RedoButton")) epane.respondUI(anEvent);
    
    // Handle FillColorButton, StrokeColorButton, TextColorButton
    if(anEvent.equals("FillColorButton"))
        RMEditorShapes.setColor(editor, RMColor.get(anEvent.getView(ColorButton.class).getColor()));
    if(anEvent.equals("StrokeColorButton"))
        RMEditorShapes.setStrokeColor(editor, RMColor.get(anEvent.getView(ColorButton.class).getColor()));
    if(anEvent.equals("TextColorButton"))
        RMEditorShapes.setTextColor(editor, RMColor.get(anEvent.getView(ColorButton.class).getColor()));

    // Handle MoneyButton: If currently selected format is number format, add or remove dollars
    RMFormat fmt = RMEditorShapes.getFormat(editor);
    RMNumberFormat nfmt = fmt instanceof RMNumberFormat? (RMNumberFormat)fmt : null;
    if(anEvent.equals("MoneyButton")) {
        if(nfmt==null) RMEditorShapes.setFormat(editor, RMNumberFormat.CURRENCY);
        else { nfmt = nfmt.clone(); // Clone it
            nfmt.setLocalCurrencySymbolUsed(!nfmt.isLocalCurrencySymbolUsed()); // Toggle whether $ is used
            RMEditorShapes.setFormat(editor, nfmt); }
    }
    
    // Handle PercentButton: If currently selected format is number format, add or remove percent symbol
    if(anEvent.equals("PercentButton")) {
        if(nfmt==null) RMEditorShapes.setFormat(editor, new RMNumberFormat("#,##0.00 %"));
        else { nfmt = nfmt.clone(); // Clone it
            nfmt.setPercentSymbolUsed(!nfmt.isPercentSymbolUsed()); // Toggle whether percent symbol is used
            RMEditorShapes.setFormat(editor, nfmt); }
    }
    
    // Handle CommaButton: If currently selected format is number format, add or remove grouping
    if(anEvent.equals("CommaButton")) {
        if(nfmt==null) RMEditorShapes.setFormat(editor, new RMNumberFormat("#,##0.00"));
        else { nfmt = nfmt.clone();
            nfmt.setGroupingUsed(!nfmt.isGroupingUsed()); // Toggle whether grouping is used
            RMEditorShapes.setFormat(editor, nfmt); }
    }
    
    // Handle DecimalAddButton: If currently selected format is number format, add decimal
    if(anEvent.equals("DecimalAddButton") && nfmt!=null) {
        nfmt = nfmt.clone();
        nfmt.setMinimumFractionDigits(nfmt.getMinimumFractionDigits()+1); // Add decimal digits
        nfmt.setMaximumFractionDigits(nfmt.getMinimumFractionDigits());
        RMEditorShapes.setFormat(editor, nfmt);
    }
    
    // Handle DecimalRemoveButton: If currently selected format is number format, remove decimal digits
    if(anEvent.equals("DecimalRemoveButton") && nfmt!=null) {
        nfmt = nfmt.clone(); // Clone it
        nfmt.setMinimumFractionDigits(nfmt.getMinimumFractionDigits()-1); // Remove decimal digits
        nfmt.setMaximumFractionDigits(nfmt.getMinimumFractionDigits());
        RMEditorShapes.setFormat(editor, nfmt);
    }
    
    // Handle Preview/Edit button and PreviewMenuItem
    if(anEvent.equals("PreviewEditButton") || anEvent.equals("PreviewMenuItem")) {
        
        // Hack to open edited file: Get filename (create file if missing) and open file in TextEdit
        if(anEvent.isAltDown()) {
            String fname = getEditor().getDocument().getFilename();
            if(fname==null) { fname = SnapUtils.getTempDir() + "RMDocument.rpt"; editor.getDocument().write(fname); }
            String commands[] = { "open",  "-e", fname };
            try { Runtime.getRuntime().exec(commands); }
            catch(Exception e) { e.printStackTrace(); }
        }
        
        // Normal preview
        else getEditorPane().setEditing(!getEditorPane().isEditing());
    }
    
    // Handle PreviewXMLMenuItem
    if(anEvent.equals("PreviewXMLMenuItem"))
        RMEditorPaneUtils.previewXML(getEditorPane());

    // Handle ToolButton(s)
    if(anEvent.getName().endsWith("ToolButton")) {
        for(RMTool tool : _toolBarTools)
            if(anEvent.getName().startsWith(tool.getClass().getSimpleName())) {
                getEditor().setCurrentTool(tool); break; }
    }
    
    // Handle FontFaceComboBox
    if(anEvent.equals("FontFaceComboBox")) {
        String familyName = anEvent.getText();
        String fontNames[] = Font.getFontNames(familyName); if(fontNames==null || fontNames.length==0) return;
        String fontName = fontNames[0];
        Font font = Font.get(fontName, 12);
        RMEditorShapes.setFontFamily(editor, font);
        editor.requestFocus();
    }
    
    // Handle FontSizeComboBox
    if(anEvent.equals("FontSizeComboBox")) {
        RMEditorShapes.setFontSize(editor, anEvent.getFloatValue(), false);
        editor.requestFocus();
    }
    
    // Handle FontSizeUpButton, FontSizeDownButton
    if(anEvent.equals("FontSizeUpButton")) { Font font = RMEditorShapes.getFont(editor);
        RMEditorShapes.setFontSize(editor, font.getSize()<16? 1 : 2, true); }
    if(anEvent.equals("FontSizeDownButton")) { Font font = RMEditorShapes.getFont(editor);
        RMEditorShapes.setFontSize(editor, font.getSize()<16? -1 : -2, true); }
    
    // Handle BoldButton, ItalicButton, UnderlineButton
    if(anEvent.equals("BoldButton")) RMEditorShapes.setFontBold(editor, anEvent.getBoolValue());
    if(anEvent.equals("ItalicButton")) RMEditorShapes.setFontItalic(editor, anEvent.getBoolValue());
    if(anEvent.equals("UnderlineButton")) RMEditorShapes.setUnderlined(editor);
        
    // Handle AlignLeftButton, AlignCenterButton, AlignRightButton, AlignFullButton
    if(anEvent.equals("AlignLeftButton")) RMEditorShapes.setAlignmentX(editor, RMTypes.AlignX.Left);
    if(anEvent.equals("AlignCenterButton")) RMEditorShapes.setAlignmentX(editor, RMTypes.AlignX.Center);
    if(anEvent.equals("AlignRightButton")) RMEditorShapes.setAlignmentX(editor, RMTypes.AlignX.Right);
    if(anEvent.equals("AlignFullButton")) RMEditorShapes.setAlignmentX(editor, RMTypes.AlignX.Full);
    
    // Handle AddTableButton, AddGraphButton, AddLabelsButton, AddCrossTabFrameButton
    if(anEvent.equals("AddTableButton")) RMTableTool.addTable(getEditor(), null);
    if(anEvent.equals("AddGraphButton")) RMGraphTool.addGraph(getEditor(), null);
    if(anEvent.equals("AddLabelsButton")) RMLabelsTool.addLabels(getEditor(), null);
    if(anEvent.equals("AddCrossTabFrameButton")) RMCrossTabTool.addCrossTab(getEditor(), null);
    
    // Handle AddCrossTabButton, AddImagePlaceHolderMenuItem, AddSubreportMenuItem
    if(anEvent.equals("AddCrossTabButton")) RMCrossTabTool.addCrossTab(getEditor());
    if(anEvent.equals("AddImagePlaceHolderMenuItem")) RMEditorShapes.addImagePlaceholder(getEditor());
    if(anEvent.equals("AddSubreportMenuItem")) RMEditorShapes.addSubreport(getEditor());
    
    // Handle ConnectToDataSourceMenuItem
    if(anEvent.equals("ConnectToDataSourceMenuItem") || anEvent.equals("ConnectToDataSourceButton"))
        RMEditorPaneUtils.connectToDataSource(getEditorPane());
}

/**
 * Creates the list of tool instances for tool bar.
 */
protected RMTool[] createToolBarTools()
{
    List <RMTool> tools = new ArrayList();
    RMEditor editor = getEditor();
    tools.add(editor.getSelectTool());
    tools.add(editor.getTool(RMLineShape.class));
    tools.add(editor.getTool(RMRectShape.class));
    tools.add(editor.getTool(RMOvalShape.class));
    tools.add(editor.getTool(RMTextShape.class));
    tools.add(editor.getTool(RMPolygonShape.class));
    tools.add(new RMPolygonShapeTool.PencilTool(editor));
    return tools.toArray(new RMTool[0]);
}

}