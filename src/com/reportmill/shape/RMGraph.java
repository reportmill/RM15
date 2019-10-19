/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMFormat;
import com.reportmill.base.RMGrouping;
import com.reportmill.graphics.*;
import java.util.*;
import java.util.List;
import snap.gfx.*;
import snap.util.*;

/**
 * The RMGraph class provides a template and the machinery for generating graphs. It primarily encapsulates a list key
 * for defining the set of objects to graph and a keys string for defining the attributes of those objects to graph.
 * For instance, the list key might be "Movies" and the keys string might be "revenue". You can programatically create
 * a graph like this:
 * <p><blockquote><pre>
 *  RMGraph graph = new RMGraph();
 *  graph.setBounds(0, 0, 640, 480);
 *  graph.setDatasetKey("Movies");
 *  graph.setKeysString("revenue");
 *  graph.setType(RMGraph.TYPE_PIE);
 *  </pre></blockquote>
 */
public class RMGraph extends RMParentShape {
    
    // The dataset key used to get graph objects
    String                    _datasetKey;
    
    // An optional key expression used to limit the table list derived from dataset key
    String                    _filterKey;
    
    // The list of keys to be graphed
    List <String>             _keys = new ArrayList();
    
    // The type of graph
    Type                      _type = Type.Bar;
    
    // The graph object grouper
    RMGrouping                _grouping = new RMGrouping("Objects");
    
    // The the layout of section values
    SectionLayout             _sectionLayout = SectionLayout.Merge;
    
    // The layout of the section items values
    ItemLayout                _itemsLayout = ItemLayout.Abreast;

    // The graph area bar shape
    RMGraphPartBars           _bars = new RMGraphPartBars();
    
    // The graph area pie shape
    RMGraphPartPie            _pie = new RMGraphPartPie();
    
    // The graph area value axis
    RMGraphPartValueAxis      _valueAxis = new RMGraphPartValueAxis();
    
    // The graph area label axis
    RMGraphPartLabelAxis      _labelAxis = new RMGraphPartLabelAxis();
    
    // The graph area series shape
    List <RMGraphPartSeries>  _series = new ArrayList();
    
    // The graph area 3D shape
    RMScene3D                 _3d;

    // Whether to draw graph in 3D
    boolean                   _draw3D = true;
    
    // Whether graph should color individual items
    boolean                   _colorItems;
    
    // A key that can be evaluated on graph data item to return color string
    String                    _colorKey;
    
    // This list of colors this graph uses
    List                      _colors;
    
    // A standin shape for editor to allow setting fill, stroke, font of component shapes
    RMShape                   _proxyShape;
    
    // Whether to ignore proxy
    boolean                   _proxyDisable;
    
    // The shared default list of colors all graphs use
    static List <RMColor>     _defaultColors;
    
    // Constants for Graph type
    public enum Type { Bar, BarH, Area, Line, Scatter, Pie };
    
    // Constants for section layouts
    public enum SectionLayout { Merge, Separate };
    
    // Constants for item layouts
    public enum ItemLayout { Abreast, Stacked, Layered };
    
    // Constants for properties
    public static final String ProxyShape_Prop = "ProxyShape";
    
/**
 * Creates an RMGraph.
 */
public RMGraph()
{
    _bars._parent = this;
    _pie._parent = this;
    _valueAxis._parent = this;
    _labelAxis._parent = this;
}
    
/**
 * Returns the dataset key associated with the graph.
 */
public String getDatasetKey()  { return _datasetKey; }

/**
 * Sets the dataset key associated with the graph.
 */
public void setDatasetKey(String aKeyPath)
{
    _datasetKey = aKeyPath; // Set dataset key
    _grouping.setKey(_datasetKey==null? "Objects" : _datasetKey); // Rename grouping
}

/**
 * Returns the optional key chain (expression) used to limit the table list derived from dataset key.
 */
public String getFilterKey()  { return _filterKey; }

/**
 * Sets the optional key chain (expression) used to limit the table list derived from dataset key.
 */
public void setFilterKey(String aKeyExpr)  { _filterKey = aKeyExpr; }

/**
 * Returns the number of keys for this graph.
 */
public int getKeyCount()  { return _keys.size(); }

/**
 * Returns the specific key at the given index.
 */
public String getKey(int anIndex)  { return _keys.get(anIndex); }

/**
 * Adds a key.
 */
public void addKey(String aKey)
{
    _keys.add(aKey);
    relayout();
}

/**
 * Removes a key.
 */
public void removeKey(int anIndex)
{
    _keys.remove(anIndex);
    relayout();
}

/**
 * Removes all keys.
 */
public void clearKeys()  { _keys.clear(); relayout(); }

/**
 * Returns the graph type (TYPE_BAR, TYPE_PIE, etc.).
 */
public Type getType()  { return _type; }

/**
 * Sets the graph type (TYPE_BAR, TYPE_PIE, etc.).
 */
public void setType(Type aType)
{
    if(_type==aType) return;
    firePropChange("Type", _type, _type=aType);
    relayout();
}

/**
 * Returns the graph type as a simple string: bar, pie or hbar.
 */
public String getGraphTypeString()
{
    if(_type==Type.Bar) return "bar"; if(_type==Type.BarH) return "hbar"; if(_type==Type.Area) return "area";
    if(_type==Type.Line) return "line"; if(_type==Type.Scatter) return "scatter"; if(_type==Type.Pie) return "pie";
    return null;
}

/**
 * Sets the graph type as a simple string: bar, pie or hbar.
 */
public void setGraphTypeString(String aString)
{
    if(aString.equals("bar")) setType(Type.Bar);
    else if(aString.equals("hbar")) setType(Type.BarH);
    else if(aString.equals("area")) setType(Type.Area);
    else if(aString.equals("line")) setType(Type.Line);
    else if(aString.equals("scatter")) setType(Type.Scatter);
    else if(aString.equals("pie")) setType(Type.Pie);
}

/**
 * Returns the graph grouping.
 */
public RMGrouping getGrouping()  { return _grouping; }

/**
 * Returns the layout of series values.
 */
public SectionLayout getSectionLayout()  { return _sectionLayout; }

/**
 * Sets the layout of series values.
 */
public void setSectionLayout(SectionLayout aLayout)
{
    if(aLayout==getSectionLayout()) return;
    firePropChange("SectionLayout", _sectionLayout, _sectionLayout = aLayout);
    relayout(); repaint();
}

/**
 * Returns the layout of section items.
 */
public ItemLayout getItemsLayout()  { return _itemsLayout; }

/**
 * Sets the layout of section items.
 */
public void setItemsLayout(ItemLayout aLayout)
{
    if(aLayout==getItemsLayout()) return;
    firePropChange("ItemsLayout", _itemsLayout, _itemsLayout = aLayout);
    relayout(); repaint();
}

/**
 * Returns whether graph area is considered vertical.
 */
public boolean isVertical()  { return getType()!=RMGraph.Type.BarH; }

/**
 * Returns whether section items layout is abreast.
 */
public boolean isAbreast()  { return getItemsLayout()==ItemLayout.Abreast; }

/**
 * Returns whether section items layout is stacked.
 */
public boolean isStacked()  { return getItemsLayout()==ItemLayout.Stacked; }

/**
 * Returns whether section items layout is layered.
 */
public boolean isLayered()  { return getItemsLayout()==ItemLayout.Layered; }

/**
 * Overrides shape implementation to repaint parent too.
 */
public void repaint()
{
    //if(getParent()!=null) getParent().repaint();
    super.repaint();
}

/**
 * Overrides to relayout legend too.
 */
public void relayout()
{
    super.relayout();
    RMGraphLegend leg = getLegend(); if(leg!=null)  leg.resetItems();
}

/**
 * Returns the value axis shape.
 */
public RMGraphPartValueAxis getValueAxis()  { return _valueAxis; }

/**
 * Returns the label axis shape.
 */
public RMGraphPartLabelAxis getLabelAxis()  { return _labelAxis; }

/**
 * Returns the bars shape.
 */
public RMGraphPartBars getBars()  { return _bars; }

/**
 * Returns the pie shape.
 */
public RMGraphPartPie getPie()  { return _pie; }

/**
 * Returns the number of series.
 */
public int getSeriesCount()  { return getKeyCount(); }

/**
 * Returns the individual series object and the given index.
 */
public RMGraphPartSeries getSeries(int anIndex)
{
    // Make sure there are a sufficient number of series
    while(anIndex>=_series.size())
        addSeries(new RMGraphPartSeries());
        
    // Return series at given index
    return _series.get(anIndex);
}

/**
 * Adds a new series.
 */
protected void addSeries(RMGraphPartSeries aSeries)
{
    _series.add(aSeries);
    aSeries._graph = this;
    aSeries.setParent(this);
}

/**
 * Returns the 3d shape.
 */
public RMScene3D get3D()
{
    // If already set, just return
    if(_3d!=null) return _3d;

    // Create and return
    RMScene3D p3d = new RMScene3D();
    p3d.setDepth(100); p3d.setYaw(8); p3d.setPitch(11); p3d.setFocalLength(8*72);
    p3d.getCamera().addPropChangeListener(pc -> thr3DPropChange(pc));
    return _3d = p3d;
}

// Called when 3D changes to sync to graph.
void thr3DPropChange(PropChange anEvent)
{
    repaint();
    RMScene3D p3d = getChildCount()>0 && getChild(0) instanceof RMScene3D? (RMScene3D)getChild(0) : null;
    if(p3d!=null)
        p3d.copy3D(get3D());
}

/**
 * Returns whether the graph draws in 3D.
 */
public boolean isDraw3D()  { return _draw3D; }

/**
 * Returns whether the graph draws in 3D.
 */
public boolean getDraw3D()  { return _draw3D; }

/**
 * Sets whether the graph draws in 3D.
 */
public void setDraw3D(boolean aFlag)
{
    firePropChange("Draw3D", _draw3D, _draw3D = aFlag);
    relayout();
}

/**
 * Returns the child of the graph that represents the legend (if present).
 */
public RMGraphLegend getLegend()
{
    return getParent()!=null? getParent().getChildWithClass(RMGraphLegend.class) : null;
}

/**
 * Returns whether the graph shows a legend.
 */
public boolean isShowLegend()  { return getLegend()!=null; }

/**
 * Sets whether the graph shows a legend.
 */
public void setShowLegend(boolean aFlag)
{
    // Get legend and graphArea
    RMGraphLegend legend = getLegend();

    // If legend requested, but not present, create it, configure and add to parent
    if(aFlag && legend==null && getParent()!=null) {
        legend = new RMGraphLegend();
        legend.setColor(RMColor.white);
        legend.setStrokeColor(RMColor.black);
        legend.setEffect(new ShadowEffect(5, new Color(0,0,0,.65),5,5));
        legend.setBounds(getMaxX() + 5, getY(), 90, 40);
        getParent().addChild(legend); // Add just before area
    }

    // If aFlag is false and legend is present, remove it
    else if(!aFlag && legend!=null)
        legend.getParent().removeChild(legend);
}

/**
 * Returns whether graph should color individual items.
 */
public boolean isColorItems()  { return _colorItems; }

/**
 * Sets whether graph should color individual items.
 */
public void setColorItems(boolean aValue)
{
    if(aValue==isColorItems()) return;
    firePropChange("ColorItems", _colorItems, _colorItems = aValue);
    relayout();
    if(getLegend()!=null)
        getLegend().resetItems();
}

/**
 * Returns the key evaluated on graph data item to return color string.
 */
public String getColorKey()  { return _colorKey; }

/**
 * Sets the key evaluated on graph data item to return color string.
 */
public void setColorKey(String aKey)
{
    // If value already set, just return
    String key = aKey!=null && aKey.length()>0? aKey : null;
    if(SnapUtils.equals(key, getColorKey())) return;
    
    // Set value and update graph/legend
    firePropChange("ColorKey", _colorKey, _colorKey = key);
    relayout();
    if(getLegend()!=null)
        getLegend().resetItems();
}

/**
 * Returns the number of colors set for this graph.
 */
public int getColorCount()  { return getColors().size(); }

/**
 * Returns the specific color at the given index. Automatically wraps if index exceeds color count.
 */
public RMColor getColor(int anIndex)  { return getColors().get(anIndex%getColorCount()); }

/**
 * Returns the list of colors to be used by this graph (or the default graph colors, if null).
 */
public List <RMColor> getColors()  { return _colors==null? getDefaultColors() : _colors; }

/**
 * Sets the list of colors to be used by this graph.
 */
public void setColors(List aColorList)  { _colors = aColorList; relayout(); }

/**
 * Returns the default list of colors to be used by any graph without an explicit list of colors.
 */
public static List getDefaultColors()
{
    // If default colors haven't been created, create them
    if(_defaultColors==null) {
        _defaultColors = Arrays.asList(
            new RMColor("#5064CD"),
            new RMColor("#50AF64"),
            new RMColor("#CD5050"), //new RMColor(200/255f, 0f, 0f), // Red
            //new RMColor(0f, 200/255f, 0f), // Green //new RMColor(0f, 0f, 200/255f), // Blue
            new RMColor(0f, 200/255f, 200/255f), // Cyan
            new RMColor(200/255f, 0f, 200/255f), // Magenta
            new RMColor(200/255f, 200/255f, 0f), // Yellow
            new RMColor(255/255f, 127/255f, 0f), // Orange
            new RMColor(127/255f, 0/255f, 127/255f), // Purple
            new RMColor(153/255f, 102/255f, 51/255f)); // Brown
    }

    // Return default colors list
    return _defaultColors;
}

/**
 * Sets the default list of colors to be used by any graph without an explicit list of colors.
 */
public static void setDefaultColors(List aList)  { _defaultColors = aList; }

/**
 * Override to create/add new graph content.
 */
protected void layoutImpl()
{
    // Recreate and add sample graph
    removeChildren();
    addChild(createSampleGraph());
}

/**
 * Override to suppress selection of children.
 */
protected boolean isHittable(RMShape aChild)  { return false; }

/**
 * Returns a graph area configured like this one showing sample data.
 */
private RMShape createSampleGraph()
{
    // Get copy of graph so we can apply sample data, keys, axis, etc.
    RMGraph graph = (RMGraph)cloneDeep();
    
    // Clear and add graph keys, reset filter key
    graph.clearKeys();
    for(int i=0, iMax=Math.max(getKeyCount(), 1); i<iMax; i++)
        graph.addKey("test" + i);
    graph.setFilterKey(null);
    
    // Do rpg for graph and return
    ReportOwner rownr= new ReportOwner();
    rownr.addModelObject(getSampleObjects());
    RMParentShape graphRPG = graph.rpgAll(rownr, graph, true);
    graphRPG.setXY(0,0);
    graphRPG.layout();
    return graphRPG;
}

/**
 * Returns a list of sample objects for this graph.
 */
private List getSampleObjects()
{
    // Get key count (make it at least 1) and create some sample values
    int keyCount = getKeyCount(); if(keyCount==0) keyCount = 1;
    float values[] = { .9f, .7f, .6f, .45f, .35f, .2f };
    if(getType().equals(Type.Pie) && keyCount>1) values = new float[] { .9f };
    
    // If value axis has max value set, adjust sample values
    if(getValueAxis().getAxisMax()!=Float.MIN_VALUE)
        for(int i=0; i<values.length; i++)
            values[i] = values[i]*getValueAxis().getAxisMax();
    
    // Calc # of sample objects: if 1 key, use 6, 2 keys use 5, 3 keys use 4
    int sampleCount = keyCount==1? 6 : keyCount==2? 5 : 4;
    if(getType().equals(Type.Pie) && keyCount>1)
        sampleCount = 1;
    
    // Create sample objects: start with hard coded values and diminish by fraction for each series
    List objects = new ArrayList();
    for(int i=0; i<sampleCount; i++) {
        Map map = new HashMap(); objects.add(map); // Create object and add to list
        for(int j=0; j<keyCount; j++) map.put("test" + j, values[i]/(j+1));
    }
    
    // Return objects
    return objects;
}

/**
 * Set ReportMill (which tries to get a dataset from reportmill and calls setObjects).
 */
public RMParentShape rpgAll(ReportOwner anRptOwner, RMShape aParent)  { return rpgAll(anRptOwner, aParent, false); }

/**
 * Set ReportMill (which tries to get a dataset from reportmill and calls setObjects).
 */
public RMParentShape rpgAll(ReportOwner anRptOwner, RMShape aParent, boolean isSample)
{
    _proxyDisable = true;
    RMGraph.Type type = getType();
    RMParentShape rpg = null;
    if(type==RMGraph.Type.Bar || type==RMGraph.Type.BarH) rpg = new RMGraphRPGBar(this, anRptOwner).getGraphShape();
    else if(type==RMGraph.Type.Pie) rpg = new RMGraphRPGPie(this, anRptOwner).getGraphShape();
    else rpg = new RMGraphRPGLine(this, anRptOwner).getGraphShape(); // Type Area, Line, Scatter
    if(!isSample) rpgBindings(anRptOwner, rpg);
    _proxyDisable = false;
    return rpg;
}

/**
 * Override to suppress ProxyShape.
 */
public void paint(Painter aPntr)
{
    _proxyDisable = true;
    super.paint(aPntr);
    _proxyDisable = false;
}

/**
 * Override to suppress background paint.
 */
public void paintShape(Painter aPntr)  { }

/**
 * Return ProxyShape.
 */
public RMShape getProxyShape()  { return _proxyShape; }

/**
 * Sets the ProxyShape.
 */
public void setProxyShape(RMShape aShape)
{
    if(aShape==_proxyShape) return;
    firePropChange(ProxyShape_Prop, _proxyShape, _proxyShape = aShape);
}

/**
 * Whether to use proxy.
 */
private boolean useProxy()
{
    if(_proxyShape==null) return false;
    if(isInLayout()) return false;
    if(_proxyDisable) return false;
    return true;
}

/**
 * Override to allow for ProxyShape.
 */
public RMFill getFill()
{
    return useProxy()? _proxyShape.getFill() : super.getFill();
}

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setFill(RMFill aFill)
{
    if(_proxyShape!=null)
        _proxyShape.setFill(aFill);
    else super.setFill(aFill);
    relayout();
}

/**
 * Override to allow for ProxyShape.
 */
public RMStroke getStroke()  { return useProxy()? _proxyShape.getStroke() : super.getStroke(); }

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setStroke(RMStroke aStroke)
{
    if(_proxyShape!=null)
        _proxyShape.setStroke(aStroke);
    else super.setStroke(aStroke);
    relayout();
}

/**
 * Override to allow for ProxyShape.
 */
public Effect getEffect()
{
    return useProxy()? _proxyShape.getEffect() : super.getEffect();
}

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setEffect(Effect anEffect)
{
    if(_proxyShape!=null)
        _proxyShape.setEffect(anEffect);
    else super.setEffect(anEffect);
    relayout();
}

/**
 * Override to allow for ProxyShape.
 */
public RMFont getFont()
{
    return useProxy()? _proxyShape.getFont() : super.getFont();
}

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setFont(RMFont aFont)
{
    if(_proxyShape!=null)
        _proxyShape.setFont(aFont);
    else super.setFont(aFont);
    relayout();
}

/**
 * Override to allow for ProxyShape.
 */
public RMColor getTextColor()  { return useProxy()? _proxyShape.getTextColor() : super.getTextColor(); }

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setTextColor(RMColor aColor)
{
    if(_proxyShape!=null)
        _proxyShape.setTextColor(aColor);
    else super.setTextColor(aColor);
    relayout();
}

/**
 * Override to allow for ProxyShape.
 */
public double getOpacity()
{
    return useProxy()? _proxyShape.getOpacity() : super.getOpacity();
}

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setOpacity(double aValue)
{
    if(_proxyShape!=null)
        _proxyShape.setOpacity(aValue);
    else super.setOpacity(aValue);
    relayout();
}

/**
 * Override to allow for ProxyShape.
 */
public RMFormat getFormat()  { return useProxy()? _proxyShape.getFormat() : super.getFormat(); }

/**
 * Override to allow for ProxyShape and trigger relayout.
 */
public void setFormat(RMFormat aFormat)
{
    if(_proxyShape!=null)
        _proxyShape.setFormat(aFormat);
    else super.setFormat(aFormat);
    relayout();
}

/**
 * Standard clone implementation.
 */
public RMGraph clone()
{
    _proxyDisable = true;
    RMGraph clone = (RMGraph)super.clone();
    clone._keys = new ArrayList(_keys);
    clone._grouping = _grouping.clone();
    clone._valueAxis = (RMGraphPartValueAxis)_valueAxis.clone();
    clone._labelAxis = (RMGraphPartLabelAxis)_labelAxis.clone();
    clone._bars = (RMGraphPartBars)_bars.clone();
    clone._pie = (RMGraphPartPie)_pie.clone();
    clone._3d = (RMScene3D)get3D().clone();
    clone._series = new ArrayList();
    for(RMGraphPartSeries s : _series) {
        RMGraphPartSeries s2 = (RMGraphPartSeries)s.clone();
        clone.addSeries(s2);
    }
    _proxyDisable = false;
    return clone;
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    _proxyDisable = true;
    // Archive basic shape attributes and reset element name and set type
    XMLElement e = super.toXMLShape(anArchiver); e.setName("graph");
    e.add("type", getGraphTypeString());
    
    // Archive DatasetKey, FilterKey
    if(_datasetKey!=null && _datasetKey.length()>0) e.add("list-key", _datasetKey);
    if(_filterKey!=null && _filterKey.length()>0) e.add("filter-key", _filterKey);
    
    // Archive keys
    for(int i=0, iMax=getKeyCount(); i<iMax; i++)
        e.add("key" + i, getKey(i));
    
    // Archive grouping - old style (get grouping+xml - if any elements/attrs from grouping xml, add to new xml)
    RMGrouping grouping = getGrouping();
    XMLElement groupingXML = grouping.toXML(anArchiver);
    if(groupingXML.size()>0 || groupingXML.getAttributeCount()>0)
        e.addAll(groupingXML);

    // Archive SectionLayout, ItemsLayout
    if(getSectionLayout()!=SectionLayout.Merge) e.add("series-layout", getSectionLayout());
    if(getItemsLayout()!=ItemLayout.Abreast) e.add("items-layout", getItemsLayout());
    
    // Archive ValueAxis, LabelAxis, Bar, Pie, 3D
    XMLElement valueAxis = anArchiver.toXML(_valueAxis, this);
    if(valueAxis.getAttributeCount()+valueAxis.size()>0) e.add(valueAxis);
    XMLElement labelAxis = anArchiver.toXML(_labelAxis, this);
    if(labelAxis.getAttributeCount()+labelAxis.size()>0) e.add(labelAxis);
    XMLElement bars = anArchiver.toXML(_bars, this);
    if(bars.getAttributeCount()+bars.size()>0) e.add(bars);
    XMLElement pie = anArchiver.toXML(_pie, this);
    if(pie.getAttributeCount()+pie.size()>0) e.add(pie);
    XMLElement td = anArchiver.toXML(_3d, this);
    if(td.getAttributeCount()+td.size()>0) e.add(td);

    // Archive series
    for(int i=0; i<getSeriesCount(); i++) { RMGraphPartSeries series = getSeries(i);
        e.add(anArchiver.toXML(series, this)); }
    
    // Archive Draw3d, ColorItems
    if(!_draw3D) e.add("draw-3d", false);
    if(isColorItems()) e.add("ColorItems", true);
    
    // Archive ColorKey
    if(getColorKey()!=null) e.add("ColorKey", getColorKey());
    
    // Archive colors
    if(!getColors().equals(getDefaultColors())) {
        StringBuffer cbuf = new StringBuffer();
        for(int i=0, iMax=getColorCount(); i<iMax; i++) {
            cbuf.append("#").append(getColor(i).toHexString());
            if(i+1<iMax) cbuf.append(" ");
        }
        e.add("colors", cbuf);
    }
    
    // Return xml element
    _proxyDisable = false;
    return e;
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive type
    setGraphTypeString(anElement.getAttributeValue("type", "bar"));
    
    // Unarchive DatasetKey, FilterKey and Keys
    setDatasetKey(anElement.getAttributeValue("list-key", _datasetKey));
    setFilterKey(anElement.getAttributeValue("filter-key"));
    for(int i=0; anElement.hasAttribute("key"+i); i++)
        addKey(anElement.getAttributeValue("key"+i));
    
    // Unarchive Keys (legacy)
    if(anElement.hasAttribute("keys")) { String kstring = anElement.getAttributeValue("keys");
        String keys[] = kstring.split(",");
        for(String key : keys) if(key.trim().length()>0) addKey(key.trim());
    }
    
    // Unarchive Grouping (Legacy: load from <grouping> child element)
    getGrouping().fromXML(anArchiver, anElement);
    XMLElement gxml = anElement.getElement("grouping");
    if(gxml!=null) {
        getGrouping().fromXML(anArchiver, gxml);
        if(anElement.getElements("grouping").size()>1) System.err.println("RMGraph.fromXML: Multiple groups not read");
    }
        
    // Unarchive SectionLayout, ItemsLayout
    if(anElement.hasAttribute("series-layout"))
        setSectionLayout(SectionLayout.valueOf(anElement.getAttributeValue("series-layout"))); 
    if(anElement.hasAttribute("items-layout"))
        setItemsLayout(ItemLayout.valueOf(anElement.getAttributeValue("items-layout"))); 
    
    // Unarchive ValueAxis, LabelAxis, Bars, Pie, 3D
    XMLElement valueAxis = anElement.get("value-axis");
    if(valueAxis!=null) _valueAxis.fromXML(anArchiver, valueAxis);
    XMLElement labelAxis = anElement.get("label-axis");
    if(labelAxis!=null) _labelAxis.fromXML(anArchiver, labelAxis);
    XMLElement bars = anElement.get("bars");
    if(bars!=null) _bars.fromXML(anArchiver, bars);
    XMLElement pie = anElement.get("pie");
    if(pie!=null) _pie.fromXML(anArchiver, pie);
    XMLElement td = anElement.get("scene3d");
    if(td!=null) get3D().fromXML(anArchiver, td);
    
    // Unarchive series
    for(int i=anElement.indexOf("series"); i>=0; i=anElement.indexOf("series", i+1)) { XMLElement sxml =anElement.get(i);
        RMGraphPartSeries series = new RMGraphPartSeries();
        series.fromXML(anArchiver, sxml);
        addSeries(series);
    }
    
    // Unarchive Draw3d, ColorItems
    setDraw3D(anElement.getAttributeBoolValue("draw-3d", true));
    if(anElement.hasAttribute("ColorItems")) setColorItems(anElement.getAttributeBoolValue("ColorItems"));
    
    // Unarchive ColorKey
    if(anElement.hasAttribute("ColorKey")) setColorKey(anElement.getAttributeValue("ColorKey"));
    
    // Unarchive colors: Get colors string, string array and create colors list and set
    if(anElement.hasAttribute("colors")) {
        String colorsString = anElement.getAttributeValue("colors");
        String cols[] = colorsString.split("\\ ");
        List colors = new ArrayList(); for(int i=0; i<cols.length; i++) colors.add(new RMColor(cols[i]));
        _colors = colors;
    }
}

/** XML archival - override shape implementation to suppress archival of children. */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)  { }

/** XML unarchival - override to suppress unarchival of children (don't really need this). */
protected void fromXMLChildren(XMLArchiver anArchiver, XMLElement anElement)  { }

/** Legacy unarchival. */
public RMShape fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    XMLElement gaxml = anElement.getElement("graph-area"); if(gaxml==null) return super.fromXML(anArchiver, anElement);
    RMParentShape frameShape = new RMParentShape(); frameShape.fromXML(anArchiver, anElement);
    for(XMLAttribute at : anElement.getAttributes()) if(!gaxml.hasAttribute(at.getName())) gaxml.addAttribute(at);
    fromXML(anArchiver, gaxml);
    frameShape.addChild(this, 0);
    return frameShape;
}

}