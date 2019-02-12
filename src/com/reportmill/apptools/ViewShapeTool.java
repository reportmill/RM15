package com.reportmill.apptools;
import com.reportmill.shape.ViewShape;
import snap.view.*;

/**
 * An RMTool subclass to edit ViewShape.
 */
public class ViewShapeTool <T extends ViewShape> extends RMTool <T> {

    // The main TabView
    TabView          _tabView;
    
    // The Text tab
    Tab              _textFieldTab;
    
    // The Button tab
    Tab              _buttonTab;
    
    // The Type selection ListView
    ListView         _typeList;
    
    // The TextView
    TextView         _textView;
    
/**
 * Override to customize UI.
 */
protected void initUI()
{
    _tabView = getUI(TabView.class);
    _textFieldTab = _tabView.getTab(1);
    _buttonTab = _tabView.getTab(2);
    
    _typeList = getView("TypeListView", ListView.class);
    _typeList.setItems("TextField", "Button", "RadioButton", "CheckBox", "ListView", "ComboBox");
    
    _textView = getView("TextView", TextView.class);
    _textView.setFireActionOnFocusLost(true);
}

/**
 * Reset UI.
 */
protected void resetUI()
{
    ViewShape vshape = getSelectedShape();
    String type = vshape.getViewType();
    
    _typeList.setSelItem(type);
    
    _textFieldTab.setVisible(type.equals(ViewShape.TextField_Type));
    _buttonTab.setVisible(type.equals(ViewShape.Button_Type));
    _textView.setText(vshape.getText());
}

/**
 * Respond to UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    ViewShape vshape = getSelectedShape();
    
    // Handle TypeListView
    if(anEvent.equals(_typeList)) {
        String type = anEvent.getStringValue();
        vshape.setViewType(type);
        vshape.setStandardSize();
    }
    
    // Handle TextView
    if(anEvent.equals("TextView")) {
        String text = anEvent.getStringValue();
        vshape.setText(text);
    }
}

}