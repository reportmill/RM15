/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
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

    // The graph area value axis
    RMGraphPartValueAxis      _valueAxis = new RMGraphPartValueAxis();
    
    // The graph area label axis
    RMGraphPartLabelAxis      _labelAxis = new RMGraphPartLabelAxis();
    
    // The graph area bar shape
    RMGraphPartBars           _bars = new RMGraphPartBars();
    
    // The graph area pie shape
    RMGraphPartPie            _pie = new RMGraphPartPie();
    
    // The graph area 3D shape
    RMScene3D                 _3d;

    // The graph area series shape
    List <RMGraphPartSeries>  _series = new ArrayList();
    
    // Whether to draw graph in 3D
    boolean                   _draw3D = true;
    
    // Whether graph should color individual items
    boolean                   _colorItems;
    
    // This list of colors this graph uses
    List                      _colors;
    
    // The shared default list of colors all graphs use
    static List <RMColor>     _defaultColors;
    
    // Constants for Graph type
    public enum Type { Bar, BarH, Area, Line, Scatter, Pie };
    
    // Constants for section layouts
    public enum SectionLayout { Merge, Separate };
    
    // Constants for item layouts
    public enum ItemLayout { Abreast, Stacked, Layered };
    
/**
 * Creates an RMGraph.
 */
public RMGraph()
{
    _pie._parent = this;
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
    // Make sure there are a sufficient number of series and return series at given index
    while(anIndex>=_series.size()) _series.add(new RMGraphPartSeries(this));
    return _series.get(anIndex);
}

/**
 * Returns the 3d shape.
 */
public RMScene3D get3D()
{
    // If already set, just return
    if(_3d!=null) return _3d;

    // Create and return
    RMScene3D p3d = new RMScene3D(); p3d.setDepth(100); p3d.setYaw(8); p3d.setPitch(11); p3d.setFocalLength(8*72);
    p3d.getCamera().addPropChangeListener(pc -> thr3DPropChange(pc));
    return _3d = p3d;
}

// Called when 3D changes to sync to graph.
void thr3DPropChange(PropChange anEvent)
{
    repaint();
    RMScene3D p3d = getChildCount()>0 && getChild(0) instanceof RMScene3D? (RMScene3D)getChild(0) : null;
    if(p3d!=null) p3d.copy3D(get3D());
}

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
        legend.setColor(RMColor.white); legend.setStrokeColor(RMColor.black);
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
    _colorItems = aValue; relayout();
    if(getLegend()!=null) getLegend().resetItems();
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
    
    // Reset parts and make sure they are installed
    _parts = new RMShape[2 + getKeyCount()]; _partNames = new String[_parts.length];
    _parts[0] = getValueAxis(); _parts[1] = getLabelAxis();
    _partNames[0] = "Value Axis"; _partNames[1] = "Label Axis";
    for(int i=0; i<getKeyCount(); i++) { _parts[i+2] = getSeries(i); _partNames[i+2] = "Series "+(i+1); }
    for(RMShape part : _parts) if(part.getParent()!=this) addChild(part);
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
    RMGraph graph = (RMGraph)cloneDeep(); graph.clearKeys(); // Clear and add graph keys
    for(int i=0, iMax=Math.max(getKeyCount(), 1); i<iMax; i++) graph.addKey("test" + i);
    graph.setFilterKey(null); // Reset filter key
    
    // Do rpg for graph and return
    ReportOwner ro = new ReportOwner(); ro.addModelObject(getSampleObjects());
    RMParentShape graphRPG = graph.rpgAll(ro, graph, true);
    graphRPG.setXY(0,0); graphRPG.layout(); // Move to (0,0) and layout
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
    RMGraph.Type type = getType();
    RMParentShape rpg = null;
    if(type==RMGraph.Type.Bar || type==RMGraph.Type.BarH) rpg = new RMGraphRPGBar(this, anRptOwner).getGraphShape();
    else if(type==RMGraph.Type.Pie) rpg = new RMGraphRPGPie(this, anRptOwner).getGraphShape();
    else rpg = new RMGraphRPGLine(this, anRptOwner).getGraphShape(); // Type Area, Line, Scatter
    if(!isSample) rpgBindings(anRptOwner, rpg);
    return rpg;
}

/**
 * Override to suppress background paint.
 */
public void paintShape(Painter aPntr)  { }

/**
 * Override to paint GraphArea parts.
 */
protected void paintShapeOver(Painter aPntr)
{
    // Do normal version
    super.paintShapeOver(aPntr);
    
    // Get font/metrics
    if(_font==null) _font = new Font("Arial", 10);
    
    // Get FontBoxes
    aPntr.setFont(_font); aPntr.setStroke(Stroke.Stroke1);
    _fboxes = new FontBox[getPartCount()]; double x = 3, h = 14;
    for(int i=0, iMax=getPartCount(); i<iMax; i++) { String string = getPartName(i);
        Rect fbox = aPntr.getStringBounds(string);
        double w = fbox.getWidth(), fa = Math.ceil(_font.getAscent());
        double y = getHeight() - h - 3, dy = (h - fbox.getHeight())/2, by = y + dy + fa;
        _fboxes[i] = new FontBox(x, y, w + 10, h, x + 5, by);
        x += 3 + w + 10;
    }
    
    for(int i=0, iMax=getPartCount(); i<iMax; i++) { String part = getPartName(i); FontBox fbox = getFontBox(i);
        boolean isSel = RMShapePaintProps.isSelected(aPntr, getPart(i));
        Color c = Color.LIGHTBLUE.brighter().brighter(); if(isSel) c = c.brighter();
        aPntr.setFont(_font); if(isSel) aPntr.setFont(_font.getBold());
        aPntr.setColor(c); aPntr.fill(fbox);
        aPntr.setColor(Color.BLACK); aPntr.draw(fbox);
        aPntr.drawString(part, fbox.getTextX(), fbox.getTextY());
    }
}

RMShape _parts[] = new RMShape[0]; String _partNames[] = new String[0];
Font _font;
FontBox _fboxes[];

/**
 * Returns the number of graph parts.
 */
public int getPartCount()  { return getParts().length; }

/**
 * Returns the individual part at index.
 */
public RMShape getPart(int anIndex)  { return getParts()[anIndex]; }

/**
 * Returns the individual part at index.
 */
public String getPartName(int anIndex)  { return _partNames[anIndex]; }

/**
 * Returns the parts.
 */
public RMShape[] getParts()  { return _parts; }

/**
 * Returns the font box for part at index.
 */
public FontBox getFontBox(int anIndex)  { return _fboxes!=null? _fboxes[anIndex] : null; }

/**
 * Returns the part rect.
 */
public static class FontBox extends Rect {
    double _tx, _ty;
    public FontBox(double anX, double aY, double aW, double aH, double aTX, double aTY)
    { super(anX, aY, aW, aH); _tx = aTX; _ty = aTY; }
    public double getTextX()  { return _tx; }
    public double getTextY()  { return _ty; }
}

/**
 * Override to trigger relayout.
 */
public void setFill(RMFill aFill)  { super.setFill(aFill); relayout(); }

/**
 * Sets the stroke for this shape, with an option to turn on drawsStroke.
 */
public void setStroke(RMStroke aStroke)  { super.setStroke(aStroke); relayout(); }

/**
 * Standard clone implementation.
 */
public RMGraph clone()
{
    RMGraph clone = (RMGraph)super.clone();
    clone._keys = new ArrayList(_keys);
    clone._grouping = _grouping.clone();
    clone._valueAxis = (RMGraphPartValueAxis)_valueAxis.clone();
    clone._labelAxis = (RMGraphPartLabelAxis)_labelAxis.clone();
    clone._bars = (RMGraphPartBars)_bars.clone();
    clone._pie = (RMGraphPartPie)_pie.clone();
    clone._3d = (RMScene3D)get3D().clone();
    clone._series = SnapUtils.cloneDeep(_series); for(RMGraphPartSeries s : clone._series) s._graph = clone;
    return clone;
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
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
        RMGraphPartSeries series = new RMGraphPartSeries(this);
        series.fromXML(anArchiver, sxml);
        _series.add(series);
    }
    
    // Unarchive Draw3d, ColorItems
    setDraw3D(anElement.getAttributeBoolValue("draw-3d", true));
    if(anElement.hasAttribute("ColorItems")) setColorItems(anElement.getAttributeBoolValue("ColorItems"));
    
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