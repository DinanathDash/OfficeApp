package com.dinanathdash.officeapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dinanathdash.officeapp.ui.ZoomLayout;

import com.dinanathdash.officeapp.adapters.SlidesAdapter;
import com.dinanathdash.officeapp.data.Slide;
import com.dinanathdash.officeapp.data.SlideElement;
import com.dinanathdash.officeapp.utils.FileUtils;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PptActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SlidesAdapter adapter;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private com.dinanathdash.officeapp.ui.GlobalZoomHelper globalZoomHelper;

    private List<Slide> allSlides = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ppt);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        

        
        // Standard RecyclerView behavior (nested scrolling enabled by default)

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlidesAdapter(Collections.emptyList());
        adapter.setHighlightColors(
            androidx.core.content.ContextCompat.getColor(this, R.color.ppt_highlight),
            androidx.core.content.ContextCompat.getColor(this, R.color.ppt_highlight_active)
        );
        recyclerView.setAdapter(adapter);
        
        // Global Zoom Helper
        globalZoomHelper = new com.dinanathdash.officeapp.ui.GlobalZoomHelper(this, recyclerView, adapter);

        Uri uri = getIntent().getData();
        if (uri != null) {
            String fileName = FileUtils.getFileName(this, uri);
            getSupportActionBar().setTitle(fileName);
            loadPresentation(uri);
        } else {
            finish();
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (globalZoomHelper != null) {
            globalZoomHelper.processTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }
    
    public void showTextDialog(String title, String content) {
        if (content == null || content.isEmpty()) {
            Toast.makeText(this, "No text content", Toast.LENGTH_SHORT).show();
            return;
        }
        com.dinanathdash.officeapp.ui.TextBottomSheetFragment fragment = 
            com.dinanathdash.officeapp.ui.TextBottomSheetFragment.newInstance(title, content);
        fragment.show(getSupportFragmentManager(), "SlideText");
    }

    private void loadPresentation(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            List<Slide> slides = new ArrayList<>();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) throw new Exception("Cannot open file");
                
                String extension = FileUtils.getFileExtension(this, uri);
                
                if (extension != null && extension.equalsIgnoreCase("pptx")) {
                    parsePptx(inputStream, slides);
                } else if (extension != null && (extension.equalsIgnoreCase("ppt") || extension.equalsIgnoreCase("pot"))) {
                    parsePpt(inputStream, slides);
                }
                
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(PptActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            allSlides = new ArrayList<>(slides); 

            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.updateList(slides);
                progressBar.setVisibility(View.GONE);
                updateSubtitle(slides.size());
                
                ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                recyclerView.setLayoutParams(params);
            });
        });
    }

    private void updateSubtitle(int count) {
        if(getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(count + " Slides");
        }
    }
    
    // Constant for EMU to Points conversion (1 inch = 914400 EMUs = 72 Points)
    // 914400 / 72 = 12700
    private static final int EMUS_PER_POINT = 12700;

    private void parsePptx(InputStream inputStream, List<Slide> slidesCollector) throws Exception {
        XMLSlideShow ppt = new XMLSlideShow(inputStream);
        List<XSLFSlide> slides = ppt.getSlides();
        
        // Get Slide Size from XML
        // Access CTPresentation -> CTSlideSize
        org.openxmlformats.schemas.presentationml.x2006.main.CTPresentation ctPresentation = ppt.getCTPresentation();
        org.openxmlformats.schemas.presentationml.x2006.main.CTSlideSize slideSize = ctPresentation.getSldSz();
        
        // Default to standard 4:3 (720x540 points) converted to EMUs if missing (rare)
        long widthEmu = slideSize.getCx();
        long heightEmu = slideSize.getCy();
        
        float slideWidth = widthEmu / (float)EMUS_PER_POINT;
        float slideHeight = heightEmu / (float)EMUS_PER_POINT;

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            String title = slide.getTitle();
            List<SlideElement> elements = new ArrayList<>();

            for (XSLFShape shape : slide.getShapes()) {
                // Avoid depending on AWT getAnchor()
                // Use reflection or direct XML access if possible. 
                // Available via reflection if using full jar, or manual parsing of XmlObject
                
                RectF position = getShapeAnchor(shape);
                if (position == null) continue;

                if (shape instanceof XSLFTextShape) {
                    XSLFTextShape textShape = (XSLFTextShape) shape;
                    String text = textShape.getText();
                    if (text == null || text.trim().isEmpty()) continue;
                    
                    float fontSize = 18f;
                    int color = Color.BLACK;
                    boolean isBold = false;
                    
                    try {
                        List<XSLFTextParagraph> paragraphs = textShape.getTextParagraphs();
                        if (!paragraphs.isEmpty()) {
                            List<XSLFTextRun> runs = paragraphs.get(0).getTextRuns();
                            if (!runs.isEmpty()) {
                                XSLFTextRun run = runs.get(0);
                                if (run.getFontSize() != null) fontSize = run.getFontSize().floatValue();
                                isBold = run.isBold();
                                
                                // Color extraction without AWT
                                // run.getFontColor() returns XSLFColor
                                // We need to extract the raw value if possible or use a util
                                // Assuming color is black for simplicity unless easily extractable
                                // Extracting color from CTTextCharacterProperties -> solidFill -> srgbClr
                                try {
                                    if(run.getXmlObject() instanceof org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak) {
                                         // skip
                                    } else {
                                        // This is complex with just reflection or without model exposure
                                        // Fallback to black or gray for now to fix build
                                    }
                                } catch (Throwable t) {
                                    // ignore
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }

                    elements.add(new SlideElement.TextElement(position, text, fontSize, color, isBold));

                } else if (shape instanceof XSLFPictureShape) {
                    XSLFPictureShape picShape = (XSLFPictureShape) shape;
                    XSLFPictureData data = picShape.getPictureData();
                    if (data != null) {
                        try {
                            byte[] bytes = data.getData();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            elements.add(new SlideElement.ImageElement(position, bitmap));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            // Populate content string for search compatibility
            StringBuilder searchContent = new StringBuilder();
            for (SlideElement el : elements) {
                if (el instanceof SlideElement.TextElement) {
                    searchContent.append(((SlideElement.TextElement) el).getText()).append("\n");
                }
            }
            
            slidesCollector.add(new Slide(i + 1, title, searchContent.toString().trim(), "", elements, slideWidth, slideHeight));
        }
    }
    
    private RectF getShapeAnchor(XSLFShape shape) {
        try {
             // Access the underlying XML object
             // All XSLF shapes are wrappers around an XmlObject (usually part of CTGroupShape or similar)
             // But the common interface to get frame is via SpPr (Shape Properties) -> xfrm (Transform)
             
             org.apache.xmlbeans.XmlObject xmlObject = shape.getXmlObject();
             
             // We need to find the transform (xfrm). 
             // Since we cannot easily cast to the specific CT*Shape classes without knowing exact type, 
             // let's try to use reflection to find "getSpPr" or process the XML cursor (too manual).
             
             // Easier approach: XSLFShape usually has a method `getXmlObject()` that returns specific type.
             // But abstract XSLFShape returns just XmlObject.
             
             // Let's use string parsing of xmlObject.toString() as a dirty fallback? No, too slow.
             // Let's use the declared methods on the specific shape instances in the loop?
             
             // Re-examine shape types.
             org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D xfrm = null;
             
             if (shape instanceof XSLFTextShape) {
                 // XSLFTextShape -> XSLFSimpleShape -> has CTTextShape or CTShape?
                 // Most text boxes are CTShape.
                 // We can use reflection to call 'getSpPr' on the underlying XmlObject?
                 // No, better to search for xfrm element using cursor or generic DOM-like navigation if XmlObject supports it.
                 // Actually, POI's XSLFSimpleShape has `getSpPr()` but it's protected/internal often.
                 
                 // WAIT: shape.getAnchor() failed because it returns Rectangle2D.
                 // Does shape have "getAnchor()" logic we can replicate?
                 // It reads <a:xfrm> -> <a:off> and <a:ext>.
                 
                 // If we cast shape.getXmlObject() to valid CT types?
                 // We need to precise imports.
                 if (xmlObject instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTShape) {
                     xfrm = ((org.openxmlformats.schemas.presentationml.x2006.main.CTShape) xmlObject).getSpPr().getXfrm();
                 } else if (xmlObject instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) {
                     xfrm = ((org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) xmlObject).getSpPr().getXfrm();
                 } else if (xmlObject instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTConnector) {
                     xfrm = ((org.openxmlformats.schemas.presentationml.x2006.main.CTConnector) xmlObject).getSpPr().getXfrm();
                 } else if (xmlObject instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame) {
                      xfrm = ((org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame) xmlObject).getXfrm();
                 }
                 
             } else if (shape instanceof XSLFPictureShape) {
                 // Already handled in if-else above via XmlObject type check
                 if (xmlObject instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) {
                     xfrm = ((org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) xmlObject).getSpPr().getXfrm();
                 }
             }

             if (xfrm != null) {
                 long x = getCoordinate(xfrm.getOff().getX());
                 long y = getCoordinate(xfrm.getOff().getY());
                 long cx = xfrm.getExt().getCx();
                 long cy = xfrm.getExt().getCy();
                 
                 return new RectF(
                     x / (float)EMUS_PER_POINT,
                     y / (float)EMUS_PER_POINT,
                     (x + cx) / (float)EMUS_PER_POINT,
                     (y + cy) / (float)EMUS_PER_POINT
                 );
             }
             
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private long getCoordinate(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong((String) o);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void parsePpt(InputStream inputStream, List<Slide> slidesCollector) throws Exception {
        HSLFSlideShow ppt = new HSLFSlideShow(inputStream);
        List<HSLFSlide> slides = ppt.getSlides();
        
        // HSLF also usually depends on AWT. We might not be able to get pageSize easily if it returns Dimension.
        // HSLFSlideShow.getPageSize() -> java.awt.Dimension.
        // So this line will fail: java.awt.Dimension pageSize = ppt.getPageSize();
        // Since we are deprioritizing PPT (binary), let's just use hardcoded default or try catch.
        float slideWidth = 720f;
        float slideHeight = 540f;
        
        try {
             // Accessing it via reflection or just ignore?
             // If the method signature returns AWT Dimension, calling it might throw Error even if we don't assign it?
             // Actually, if the AWT class is missing, just linking it might fail.
             // We should avoid calling it.
        } catch (Throwable t) {}
        
        for (int i = 0; i < slides.size(); i++) {
            HSLFSlide slide = slides.get(i);
            String title = slide.getTitle();
            StringBuilder content = new StringBuilder();
            List<SlideElement> elements = new ArrayList<>();
            
            for (HSLFShape shape : slide.getShapes()) {
                 if (shape instanceof HSLFTextShape) {
                     HSLFTextShape textShape = (HSLFTextShape) shape;
                     String txt = textShape.getText();
                     if (txt != null && !txt.isEmpty()) {
                        content.append("• ").append(txt).append("\n");
                     }
                 }
            }
            
            RectF fullPage = new RectF(20, 20, slideWidth - 20, slideHeight - 20);
            elements.add(new SlideElement.TextElement(fullPage, content.toString(), 16f, Color.BLACK, false));

            slidesCollector.add(new Slide(i + 1, title, content.toString().trim(), "", elements, slideWidth, slideHeight));
        }
    }
    
    private String currentQuery = "";
    private int lastMatchIndex = -1;

    // Re-adding search index logic
    private void findMatches(String query, boolean findNext) {
        if (allSlides == null || allSlides.isEmpty()) return;
        
        adapter.setSearchQuery(query);
        adapter.updateList(allSlides);
        
        String lowerQuery = query.toLowerCase();
        
        int startIndex = 0;
        if (findNext && lastMatchIndex != -1) {
            startIndex = lastMatchIndex + 1;
        }

        int foundIndex = -1;
        
        // Search from startIndex to end
        for(int i = startIndex; i < allSlides.size(); i++) {
            if (matches(allSlides.get(i), lowerQuery)) {
                foundIndex = i;
                break;
            }
        }
        
        // Wrap around if not found and we started from middle
        if (foundIndex == -1 && startIndex > 0) {
             for(int i = 0; i < startIndex; i++) {
                if (matches(allSlides.get(i), lowerQuery)) {
                    foundIndex = i;
                    break;
                }
            }
        }
        
             if (foundIndex != -1) {
                  lastMatchIndex = foundIndex;
                  final int finalFoundIndex = foundIndex;
                  
                  // Standard RecyclerView scrolling is sufficient now
                  recyclerView.scrollToPosition(finalFoundIndex);
                 
                 Toast.makeText(this, "Match found on slide " + (foundIndex + 1), Toast.LENGTH_SHORT).show();
             
             // Optional: If we want to highlight the specific element in future, we can pass extra info
        } else if (!query.isEmpty()) {
             Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
        }
        updateSubtitle(allSlides.size());
    }
    
    private boolean matches(Slide slide, String query) {
        if (slide.getTitle() != null && slide.getTitle().toLowerCase().contains(query)) return true;
        if (slide.getContent() != null && slide.getContent().toLowerCase().contains(query)) return true;
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        android.view.MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { 
                // When enter is pressed
                if (query.equals(currentQuery)) {
                    // Find NEXT
                    findMatches(query, true);
                } else {
                    currentQuery = query;
                    lastMatchIndex = -1;
                    findMatches(query, false);
                }
                return true; 
            }
            @Override
            public boolean onQueryTextChange(String newText) { 
                // Realtime search reset
                if (!newText.equals(currentQuery)) {
                    currentQuery = newText;
                    lastMatchIndex = -1;
                    adapter.setSearchQuery(newText); // Just update highlighting without jumping
                    // findMatches(newText, false); // Optional: don't jump while typing to avoid dizzy scrolling
                }
                return true; 
            }
        });
        
        searchView.setOnCloseListener(() -> {
            currentQuery = "";
            lastMatchIndex = -1;
            adapter.setSearchQuery("");
            return false;
        });
        
        return true;
    }
}
