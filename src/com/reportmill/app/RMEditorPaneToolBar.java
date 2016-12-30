/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.ColorButton;

/**
 * Tool bar for RMEditorPane.
 */
public class RMEditorPaneToolBar extends RMEditorPane.SupportPane {

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
 * Updates the UI panel controls.
 */
protected void resetUI()
{
    // Get the editor
    RMEditor editor = getEditor();
    
    // Update UndoButton, RedoButton
    Undoer undoer = editor.getUndoer();
    setViewEnabled("UndoButton", undoer!=null && undoer.getUndoSetLast()!=null);
    setViewEnabled("RedoButton", undoer!=null && undoer.getRedoSetLast()!=null);
    
    // Reset PreviewEditButton state if out of sync
    if(getViewBoolValue("PreviewEditButton")==getEditorPane().isEditing())
        setViewValue("PreviewEditButton", !getEditorPane().isEditing());

    // Get selected tool button name and button - if found and not selected, select it
    String toolButtonName = editor.getCurrentTool().getClass().getSimpleName() + "Button";
    ToggleButton toolButton = getView(toolButtonName, ToggleButton.class);
    if(toolButton!=null && !toolButton.isSelected())
        toolButton.setSelected(true);
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