package com.reportmill.app;
import com.reportmill.shape.*;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebURL;

/**
 * A class to show samples.
 */
public class SamplesPane extends ViewOwner {
    
    //
    RMEditorPane _epane;
    SheetDialogBox   _dbox;
    
    // The selected index
    int            _selIndex;
    
    // The effects
    ShadowEffect    _shadow, _selShadow;
    
    // Samples
    static final String S1 = "Blank.rpt";
    static final String S2 = "BasicMovies.rpt";
    static final String S3 = "BasicGraph.rpt";
    static final String S4 = "TableCTabGraph.rpt";
    static final String S5 = "CrossTab.rpt";
    static final String S6 = "TableGroup.rpt";
    static final String S7 = "Graphics.rpt";
    static final String SALL[] = { S1, S2, S3, S4, S5, S6, S7 };
    static final Image DOC_IMAGES[] = new Image[SALL.length];
    
/**
 * Shows the samples pane.
 */
public void showSamples(RMEditorPane anEP)
{
    _epane = anEP;
    ChildView aView = (ChildView)anEP.getUI();

    _dbox = new SheetDialogBox();
    _dbox.setContent(getUI());
    _dbox.showConfirmDialog(aView);
}

/**
 * Called when dialog box closed.
 */
void dialogBoxClosed()
{
    if(_dbox._cancelled) return;
    if(_selIndex==0 && _epane.getDoc().getPage(0).getChildCount()==0) return;
    _epane.getEditor().setDoc(getDoc(_selIndex));
}

/**
 * Creates UI.
 */
protected View createUI()
{
    // Create Shadows
    _shadow = new ShadowEffect(); _selShadow = new ShadowEffect(10, Color.get("#038ec3"), 0, 0);
    
    // Create main ColView to hold RowViews for samples
    ColView colView = new ColView(); colView.setSpacing(25); colView.setPadding(25,15,20,15);
    colView.setAlign(Pos.TOP_CENTER); colView.setFillWidth(true);
    colView.setFill(new Color(.97,.97,1d)); colView.setBorder(Color.GRAY,1);
    
    // Create RowViews
    RowView rowView = null;
    for(int i=0; i<SALL.length; i++) { String name = SALL[i];
    
        // Create/add new RowView for every three samples
        if(i%3==0) {
            rowView = new RowView(); rowView.setAlign(Pos.CENTER);
            colView.addChild(rowView);
        }
        
        // Create ImageViewX for sample
        ImageView iview = new ImageView(); iview.setPrefSize(getDocSize(i)); iview.setFill(Color.WHITE);
        iview.setName("ImageView" + String.valueOf(i)); iview.setEffect(i==0? _selShadow : _shadow);
        
        // Create label for sample
        Label label = new Label(name); label.setFont(Font.Arial13); label.setPadding(3,4,3,4);
        label.setLeanY(VPos.BOTTOM); if(i==0) { label.setFill(Color.BLUE); label.setTextFill(Color.WHITE); }
        
        // Create/add ItemBox for Sample and add ImageView + Label
        ColView ibox = new ColView(); ibox.setPrefSize(175,175); ibox.setAlign(Pos.TOP_CENTER);
        ibox.setChildren(iview, label); ibox.setPadding(0,0,8,0);
        ibox.setName("ItemBox" + String.valueOf(i));
        ibox.addEventHandler(e -> itemBoxWasPressed(ibox, e), MousePress);
        rowView.addChild(ibox);
    }
    
    // Create ScrollView
    ScrollView scroll = new ScrollView(colView); scroll.setPrefHeight(420);
    scroll.setShowHBar(false); scroll.setShowVBar(true);
    
    // Create top level box to hold ColView and label
    ColView boxView = new ColView(); boxView.setSpacing(8); boxView.setFillWidth(true);
    Label label = new Label("Select a template:"); label.setFont(Font.Arial16.getBold());
    boxView.setChildren(label, scroll);
    return boxView;
}

/**
 * Initialize UI.
 */
protected void initUI()
{
    loadImagesInBackground();
    //createImages();
}

/**
 * Called when template ItemBox is clicked.
 */
void itemBoxWasPressed(ColView anItemBox, ViewEvent anEvent)
{
    String name = anItemBox.getName();
    int index = StringUtils.intValue(name);
    ColView oldItemBox = getView("ItemBox" + _selIndex, ColView.class);
    oldItemBox.getChild(0).setEffect(_shadow);
    Label oldLabel = (Label)oldItemBox.getChild(1); oldLabel.setFill(null); oldLabel.setTextFill(null);
    anItemBox.getChild(0).setEffect(_selShadow);
    Label newLabel = (Label)anItemBox.getChild(1); newLabel.setFill(Color.BLUE); newLabel.setTextFill(Color.WHITE);
    _selIndex = index;
    if(anEvent.getClickCount()>1) _dbox.confirm();
}

/**
 * Returns the doc at given index.
 */
static RMDocument getDoc(int anIndex)
{
    String name = SALL[anIndex];
    String urls = "http://reportmill.com/jars/samples/" + name;
    WebURL url = WebURL.getURL(urls);
    byte bytes[] = url.getBytes();
    if(bytes==null) { System.err.println("SamplesPane.getDoc: Couldn't load " + url); return null; }
    RMDocument doc = RMDocument.getDoc(bytes);
    return doc;
}

/**
 * Returns the doc thumnail image at given index.
 */
public Image getDocImage(int anIndex)
{
    // If image already set, just return
    Image img = DOC_IMAGES[anIndex]; if(img!=null) return img;
    
    // Get image name, URL string, URL and Image. Then make sure image is loaded by requesting Image.Native.
    String name = SALL[anIndex].replace(".rpt", ".png");
    String urls = "http://reportmill.com/jars/samples/" + name;
    WebURL imgURL = WebURL.getURL(urls);
    img = DOC_IMAGES[anIndex] = Image.get(imgURL);
    img.getNative();
    return img;
}

/**
 * Returns size of doc at given index.
 */
static Size getDocSize(int anIndex)
{
    return new Size(102,132);
}

/**
 * Loads the thumbnail image for each sample in background thread.
 */
void loadImagesInBackground()
{
    // If app thread, come back in background thread
    if(isEventThread()) { new Thread(() -> loadImagesInBackground()).start(); return; }
    
    // Iterate over sample names and load/set images
    for(int i=0; i<SALL.length; i++) { int index = i;
        Image img = getDocImage(i);
        runLater(() -> setImage(img, index));
    }
}

/** Called after an image is loaded to set in ImageView in app thread. */
void setImage(Image anImg, int anIndex)
{
    ImageView iview = getView("ImageView" + String.valueOf(anIndex), ImageView.class);
    iview.setImage(anImg);
}

/**
 * Creates images (only needed when updating).
 */
static void createImages()
{
    for(int i=0; i<SALL.length; i++) {
        RMDocument doc = getDoc(i); if(doc==null) continue;
        doc = doc.generateReport();
        doc.getPage(0).setPaintBackground(false);
        Size size = getDocSize(i); int index = i;
        Image img = createImage(doc.getPage(0), size.width, size.height);
        byte bytes[] = img.getBytesPNG();
        new java.io.File("/tmp/gallery").mkdir();
        SnapUtils.writeBytes(bytes, "/tmp/gallery/" + SALL[i].replace(".rpt", ".png"));
    }
}

/**
 * Returns an image for the given shape, with given background color (null for clear) and scale.
 */
static Image createImage(RMShape aShape, double aW, double aH)
{
    // Create new image
    int w = (int)Math.round(aW), h = (int)Math.round(aH);
    Image img = Image.get(w, h, false);
    
    // Create painter and configure
    Painter pntr = img.getPainter(); pntr.setImageQuality(1);
    
    // Fill background
    pntr.setColor(Color.WHITE); pntr.fillRect(0,0,w,h);
    pntr.setColor(Color.GRAY); pntr.drawRect(.5,.5,w-1,h-1);

    // Paint shape and return image
    RMShapeUtils.layoutDeep(aShape);
    RMShapeUtils.paintShape(pntr, aShape, new Rect(0,0,w,h), 1d/6);
    pntr.flush();
    return img;
}

/**
 * A DialogBox subclass that shows as a sheet.
 */
private class SheetDialogBox extends DialogBox {
    
    ChildView _cview, _clipBox;
    boolean _cancelled;
    
    /**
     * Show Dialog in sheet.
     */
    protected boolean showPanel(View aView)
    {
        _cview = aView instanceof ChildView? (ChildView)aView : null;
        if(_cview==null) return super.showPanel(aView);
        
        for(View v : _cview.getChildren()) v.setPickable(false);
        
        View ui = getUI(); ui.setManaged(false); //ui.setLeanX(HPos.CENTER);
        ui.setFill(ViewUtils.getBackFill()); ui.setBorder(Color.DARKGRAY, 1);
        Size size = ui.getPrefSize(); ui.setSize(size);
        
        _clipBox = new BoxView(ui); _clipBox.setSize(size); _clipBox.setManaged(false); _clipBox.setLeanX(HPos.CENTER);
        _clipBox.setClipToBounds(true);
        _cview.addChild(_clipBox);
        ui.setTransY(-size.height); ui.getAnim(1000).setTransY(-1).play();
        
        // Make sure stage and Builder.FirstFocus are focused
        runLater(() -> notifyDidShow());
    
        return true;
    }
    
    /**
     * Hide dialog.
     */
    protected void hide()
    {
        View ui = getUI();
        ui.getAnimCleared(1000).setTransY(-ui.getHeight()).setOnFinish(a -> hideFinished()).play();
    }
    
    void hideFinished()
    {
        _cview.removeChild(_clipBox); //_cview.removeChild(getUI());
        for(View v : _cview.getChildren()) v.setPickable(true);
        dialogBoxClosed();
    }
    
    /** Hides the dialog box. */
    public void confirm()  { _cancelled = false; hide(); }
    
    /** Cancels the dialog box. */
    public void cancel()  { _cancelled = true; hide(); }
}

}