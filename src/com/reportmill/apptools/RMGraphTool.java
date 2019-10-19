/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.AttributesPanel;
import com.reportmill.app.RMEditor;
import com.reportmill.base.RMGrouping;
import com.reportmill.graphics.RMColor;
import com.reportmill.graphics.RMStroke;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.ColorDock;

/**
 * This class provides RMEditor inspector support for RMGraph.
 */
public class RMGraphTool <T extends RMGraph> extends RMTool <T> implements RMSortPanel.Owner {
    
    // The sort panel
    RMSortPanel               _sortPanel;

    // Assistant tool for Bars
    RMGraphPartBarsTool       _barTool = new RMGraphPartBarsTool();
    
    // Assistant tool for Pie
    RMGraphPartPieTool        _pieTool = new RMGraphPartPieTool();
    
    // Assistant tool for ValueAxis
    RMGraphPartValueAxisTool  _valueAxisTool = new RMGraphPartValueAxisTool();
    
    // Assistant tool for LabelAxis
    RMGraphPartLabelAxisTool  _labelAxisTool = new RMGraphPartLabelAxisTool();
    
    // Assistant tool for Series
    RMGraphPartSeriesTool     _seriesTool = new RMGraphPartSeriesTool();
    
    // Assistant tool for 3D
    RMScene3DTool             _3dTool = new Scene3DTool();
    
    // The last graph that was the selected graph
    RMGraph                   _lastSelGraph;
    
    // Whether tool is current in mouse drag loop to change 3D
    boolean                   _inScene3DMouseLoop;

/**
 * Override to forward to proxy tools.
 */
public void setEditor(RMEditor anEditor)
{
    super.setEditor(anEditor);
    _barTool.setEditor(anEditor);
    _pieTool.setEditor(anEditor);
    _valueAxisTool.setEditor(anEditor);
    _labelAxisTool.setEditor(anEditor);
    _seriesTool.setEditor(anEditor);
    _3dTool.setEditor(anEditor);
}

/**
 * Initializes UI panel.
 */
protected void initUI()
{
    // Get SortPanel and configure (select TopN button, set UI bounds)
    _sortPanel = new RMSortPanel(this);
    _sortPanel.getUI().setBounds(4, 182, 248, 100);
    _sortPanel.setSelectedPane(1);
    
    // Add SortPanel to main tab content
    TitleView sortBox = getView("SortBox", TitleView.class);
    _sortPanel.getView("SortingLabel").setVisible(false);
    sortBox.setContent(_sortPanel.getUI());
    
    // Add ValueAxis UI
    TitleView valueAxisBox = getView("ValueAxisBox", TitleView.class);
    valueAxisBox.setContent(_valueAxisTool.getUI());
    
    // Add LabelAxis UI
    TitleView labelAxisBox = getView("LabelAxisBox", TitleView.class);
    labelAxisBox.setContent(_labelAxisTool.getUI());
    
    // Add Series UI
    TitleView seriesBox = getView("SeriesBox", TitleView.class);
    seriesBox.setContent(_seriesTool.getUI());
    
    // Add 3D UI
    TitleView thr3dBox = getView("3DBox", TitleView.class);
    thr3dBox.setContent(_3dTool.getUI());
    
    // Set values in SectionLayoutList and ItemsLayoutList
    setViewItems("SectionLayoutList", RMGraph.SectionLayout.values());
    setViewItems("ItemsLayoutList", RMGraph.ItemLayout.values());
    
    // Enable drop keys
    enableEvents("ListKeyText", DragDrop);
    enableEvents("FilterText", DragDrop);
    enableEvents("KeysText", DragDrop);
}

/**
 * Reset UI panel from currently selected graph.
 */
protected void resetUI()
{
    // Get currently selected graph (just return if null)
    RMGraph graph = getSelectedGraph(); if(graph==null) return;
    
    // If selected graph has changed, try to make Graph.ProxyShape consitent
    if(graph!=_lastSelGraph)
        selGraphChanged();
    
    // Ensure Bar/Pie specific tab is installed
    boolean isPie = graph.getType()==RMGraph.Type.Pie;
    TitleView typeBox = getView("TypeBox", TitleView.class);
    View typeUI = isPie? _pieTool.getUI() : _barTool.getUI();
    if(typeBox.getContent()!=typeUI) {
        typeBox.setText(isPie? "Pie" : "Bars");
        typeBox.setContent(typeUI);
    }
    
    // Update ListKeyText, FilterText
    setViewValue("ListKeyText", graph.getDatasetKey());
    setViewValue("FilterText", graph.getFilterKey());
    
    // Update KeysText
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<graph.getKeyCount();i++) { if(i>0) sb.append(", "); sb.append(graph.getKey(i)); }
    setViewValue("KeysText", sb.toString());
    
    // Set ValuesButton visible/invisible 
    _sortPanel.getView("ValuesButton").setVisible(graph.getGrouping().getKey()!=null);

    // Update BarVGraphButton, BarHGraphButton, PieGraphButton
    setViewValue("GraphTypeBarButton", graph.getType()==RMGraph.Type.Bar);
    setViewValue("GraphTypeBarHButton", graph.getType()==RMGraph.Type.BarH);
    setViewValue("GraphTypeAreaButton", graph.getType()==RMGraph.Type.Area);
    setViewValue("GraphTypeLineButton", graph.getType()==RMGraph.Type.Line);
    setViewValue("GraphTypeScatterButton", graph.getType()==RMGraph.Type.Scatter);
    setViewValue("GraphTypePieButton", graph.getType()==RMGraph.Type.Pie);
    
    // Reset SectionLayoutList and ItemsLayoutList
    setViewValue("SectionLayoutList", graph.getSectionLayout());
    setViewValue("ItemsLayoutList", graph.getItemsLayout());

    // Get the color dock, reset colors and update
    List <RMColor> colors = graph.getColors();
    ColorDock cdock = getView("ColorDock", ColorDock.class);
    cdock.setColors(colors);
    
    // Update ColorItemsCheckBox, ShowLegendCheckBox, Draw3DCheckBox
    setViewValue("ColorItemsCheckBox", graph.isColorItems());
    setViewValue("ShowLegendCheckBox", graph.isShowLegend());
    setViewValue("Draw3DCheckBox", graph.isDraw3D());
    
    // Update ColorKeyText
    setViewValue("ColorKeyText", graph.getColorKey());
    
    // Make SeriesBox visible if graph key is set
    getView("SeriesBox").setVisible(graph.getKeyCount()>0);
    
    // Make 3DBox visible if graph draws 3D
    getView("3DBox").setVisible(graph.isDraw3D());
    
    // Make MultiKeyLayoutBox visible is more than one key
    getView("MultiKeyLayoutBox").setVisible(graph.getKeyCount()>1);
    
    // Update ProxyLabel
    String str = "Font/Color changes now apply to ";
    RMShape proxy = graph.getProxyShape();
    if(proxy instanceof RMGraphPartValueAxis) str += "Value Axis";
    else if(proxy instanceof RMGraphPartLabelAxis) str += "Label Axis";
    else if(proxy instanceof RMGraphPartSeries) str += "Series " + (_seriesTool.getSelSeriesIndex() + 1);
    else str += "Graph";
    setViewValue("ProxyLabel", str);
    
    // Update SortPanel, 
    _sortPanel.resetLater();
    if(_barTool.isShowing())
        _barTool.resetLater();
    else if(_pieTool.isShowing())
        _pieTool.resetLater();
    if(_valueAxisTool.isShowing())
        _valueAxisTool.resetLater();
    if(_labelAxisTool.isShowing())
        _labelAxisTool.resetLater();
    if(_seriesTool.isShowing())
        _seriesTool.resetLater();
    if(_3dTool.isShowing())
        _3dTool.resetLater();
}

/**
 * Update currently selected graph from UI panel.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected graph (just return if null)
    RMGraph graph = getSelectedGraph(); if(graph==null) return;
    
    // Handle ListKeyText
    if(anEvent.equals("ListKeyText")) {
        graph.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        if(anEvent.isDragDrop())
            anEvent.dropComplete();
    }
    
    // Handle FilterText
    if(anEvent.equals("FilterText"))
        graph.setFilterKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        
    // Handle KeysMenuItem
    if(anEvent.equals("KeysMenuItem"))
        getEditorPane().getAttributesPanel().setVisibleName(AttributesPanel.KEYS);
    
    // Handle KeysText
    if(anEvent.equals("KeysText")) {
        
        // Get Key string and key strings
        String keysString = getViewStringValue("KeysText");
        if(anEvent.isDragDrop()) {
            if(keysString==null || keysString.length()==0) keysString = anEvent.getStringValue();
            else keysString += ',' + anEvent.getStringValue();
            anEvent.dropComplete();
        }
        String keyStrings[] = keysString.split(",");

        // Clear keys and (re)add
        graph.clearKeys();
        for(String ks : keyStrings) {
            String key = ks.trim().replace("@", "");
            if(key!=null && key.length()>0)
                graph.addKey(key);
        }
    }
    
    // Handle GraphTypeBarButton, GraphTypeBarHButton, GraphTypeAreaButton, GraphTypeLineButton, GraphTypeScatterButton
    if(anEvent.equals("GraphTypeBarButton")) graph.setType(RMGraph.Type.Bar);
    if(anEvent.equals("GraphTypeBarHButton")) graph.setType(RMGraph.Type.BarH);
    if(anEvent.equals("GraphTypeAreaButton")) graph.setType(RMGraph.Type.Area);
    if(anEvent.equals("GraphTypeLineButton")) graph.setType(RMGraph.Type.Line);
    if(anEvent.equals("GraphTypeScatterButton")) graph.setType(RMGraph.Type.Scatter);
    if(anEvent.equals("GraphTypePieButton")) graph.setType(RMGraph.Type.Pie);
    
    // Handle SectionLayoutList, ItemsLayoutList
    if(anEvent.equals("SectionLayoutList")) graph.setSectionLayout((RMGraph.SectionLayout)anEvent.getValue());
    if(anEvent.equals("ItemsLayoutList")) graph.setItemsLayout((RMGraph.ItemLayout)anEvent.getValue());
    
    // Handle ColorDock
    if(anEvent.equals("ColorDock")) {
        
        // Get ColorDock selected index/color
        ColorDock cdock = getView("ColorDock", ColorDock.class);
        int index = cdock.getSelIndex();
        RMColor color = RMColor.get(cdock.getColor(index));
        
        // Get copy of graph colors and make sure they are at least as long as selected index
        List <RMColor> colors = new ArrayList(graph.getColors());
        while(colors.size()<index+1) colors.add(RMColor.white);
        
        // Set color of selected index, remove trailing whites, and set new colors in graph
        colors.set(index, color); 
        while(colors.size()>1 && colors.get(colors.size()-1).equals(RMColor.white)) colors.remove(colors.size()-1);
        graph.setColors(colors);
    }

    // Handle ColorItemsCheckBox, ShowLegendCheckBox, Draw3DCheckBox
    if(anEvent.equals("ColorItemsCheckBox")) graph.setColorItems(anEvent.getBoolValue());
    if(anEvent.equals("ShowLegendCheckBox")) graph.setShowLegend(anEvent.getBoolValue());
    if(anEvent.equals("Draw3DCheckBox")) graph.setDraw3D(anEvent.getBoolValue());
    
    // Handle ColorKeyText
    if(anEvent.equals("ColorKeyText")) {
        String ckey = anEvent.getStringValue();
        graph.setColorKey(anEvent.getStringValue());
        if(ckey!=null && ckey.length()>0)
            graph.setColorItems(true);
    }
    
    // Handle any box click: Close other boxes
    if(anEvent.getName().endsWith("Box"))
        titleViewExpandedChanged(anEvent);
}

/**
 * Called when a TitleView.Expanded change happens.
 */
private void titleViewExpandedChanged(ViewEvent anEvent)
{
    // Get TitleView name and whether modifer key is down
    String name = anEvent.getName();
    boolean modDown = anEvent.isShiftDown() || anEvent.isShortcutDown();
    
    // If not one of the BoxNames, just return
    if(!ArrayUtils.contains(BoxNames, name)) return;
    
    // Get TitleView for name
    TitleView tview = getView(name, TitleView.class); if(tview==null) return;
    
    // If closing, clear Graph.ProxyShape and return
    if(tview.isExpanded()) {
        RMGraph graph = getSelectedGraph();
        graph.setProxyShape(null);
        return;
    }
    
    // Close other TitleViews
    if(!modDown) {
        for(String bname : BoxNames)
            if(!bname.equals(name))
                getView(bname, TitleView.class).setExpandedAnimated(false);
    }

    // Change Proxy
    RMGraph graph = getSelectedGraph();
    switch(name) {
        case "ValueAxisBox": graph.setProxyShape(graph.getValueAxis()); break;
        case "LabelAxisBox": graph.setProxyShape(graph.getLabelAxis()); break;
        case "SeriesBox": graph.setProxyShape(_seriesTool.getSelSeries()); break;
        default: graph.setProxyShape(null); break;
    }
}

// Array of TitleView names
private String BoxNames[] = { "SortBox", "ViewBox", "TypeBox", "ValueAxisBox", "LabelAxisBox", "SeriesBox", "3DBox" };

/**
 * Called when the selected graph changes.
 */
private void selGraphChanged()
{
    // Get selected graph
    RMGraph graph = getSelectedGraph();
    
    // If LastSelGraph not set, just set and return
    if(_lastSelGraph==null) {
        _lastSelGraph = graph; return; }
    
    // Make new graph.ProxyShape consistent with old
    RMShape proxyShape = _lastSelGraph.getProxyShape();
    if(proxyShape instanceof RMGraphPartValueAxis)
        graph.setProxyShape(graph.getValueAxis());
    else if(proxyShape instanceof RMGraphPartLabelAxis)
        graph.setProxyShape(graph.getLabelAxis());
    else if(proxyShape instanceof RMGraphPartSeries) {
        int ind = _seriesTool.getSelSeriesIndex();
        int ind2 = Math.min(ind, graph.getSeriesCount()-1);
        RMShape ps2 = ind2>=0? graph.getSeries(ind2) : null;
        graph.setProxyShape(ps2);
    }
    else graph.setProxyShape(null);
    
    // Set LastSelGraph to graph
    _lastSelGraph = graph;
}

/**
 * Returns the selected graph.
 */
public RMGraph getSelectedGraph()  { return getSelectedShape(); }

/**
 * Returns the graph's grouping.
 */
public RMGrouping getGrouping()
{
    RMGraph graph = getSelectedGraph();
    return graph!=null? graph.getGrouping() : null;
}

/**
 * Returns the class this tool edits.
 */
public Class getShapeClass()  { return RMGraph.class; }

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Inspector"; }

/**
 * Overridden to make graph super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make graph accept children.
 */
public boolean getAcceptsChildren(RMShape aShape)  { return true; }

/**
 * Overridden to make graph not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Override to suppress setting font on sample graph.
 */
@Override
public void setFontKeyDeep(RMEditor anEditor, RMShape aShape, String aKey, Object aVal)
{
    setFontKey(anEditor, aShape, aKey, aVal);
}

/**
 * Adds a new graph instance to the given editor with the given dataset key.
 */
public static void addGraph(RMEditor anEditor, String aKeyPath)
{
    // Get graph instance and set it's name and datasource
    RMGraph graph = createSampleGraph();
    graph.setName(aKeyPath);
    graph.setDatasetKey(aKeyPath);
    
    // Get graph parent and set location in middle of parent
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    graph.setXY(parent.getWidth()/2 - graph.getWidth()/2, parent.getHeight()/2 - graph.getHeight()/2);

    // Add graph
    anEditor.undoerSetUndoTitle("Add Bar Graph");
    parent.addChild(graph);

    // Select graph, select selectTool and redisplay
    anEditor.setCurrentToolToSelectTool();
    anEditor.setSelectedShape(graph);
}

/**
 * Creates a new sample graph instance.
 */
private static RMGraph createSampleGraph()
{
    // Create new 320x240(*90%) graph
    RMGraph graph = new RMGraph(); graph.setSize(288, 216);
    graph.setStroke(new RMStroke()); //setFill(newRMGradientFill(newRMColor(.85f,.88f,1),newRMColor(.58f,.65f,1),90));
    graph.setColor(RMColor.white);
    return graph;
}

/**
 * Handle MousePressed.
 */
public void mousePressed(T aGraph, ViewEvent anEvent)
{
    // See if 3D is available
    if(isSuperSelected(aGraph) && aGraph.isDraw3D()) {
        RMScene3D s3d = aGraph.get3D();
        s3d.processEvent(createShapeEvent(s3d, anEvent));
        getEditor().setSuperSelectedShape(aGraph);
        anEvent.consume();
        _inScene3DMouseLoop = true;
    }
}

/**
 * Event handler for editing.
 */
public void mouseDragged(T aGraph, ViewEvent anEvent)
{
    // If child is scene3d, forward mouse event to child and consume event
    if(_inScene3DMouseLoop) {
        RMScene3D s3d = aGraph.get3D();
        s3d.processEvent(createShapeEvent(s3d, anEvent));
        anEvent.consume();
    }
}

/**
 * Handle MouseReleased.
 */
public void mouseReleased(T aGraph, ViewEvent anEvent)
{
    if(_inScene3DMouseLoop) {
        RMScene3D s3d = aGraph.get3D();
        s3d.processEvent(createShapeEvent(s3d, anEvent));
        anEvent.consume();
        _inScene3DMouseLoop = false;
    }
}

/**
 * The Scene3DTool.
 */
protected class Scene3DTool extends RMScene3DTool {

    /** Override to get RMScene3D from GraphArea. */
    public RMShape getSelectedShape()
    {
        RMGraph graph = RMGraphTool.this.getSelectedShape();
        return graph!=null? graph.get3D() : null;
    }
}

}