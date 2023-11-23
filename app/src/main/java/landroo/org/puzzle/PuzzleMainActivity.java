package landroo.org.puzzle;
/*
Jigsaw puzzle
Simple jigsaw puzzle application.
- Choice a picture or photo, set the sze of the pieces and begin with a difficulty.
- You could scroll and zoom the desktop and change the pattern with double tap.
- Drag a piece to move or tap to rotate.
- Double tap for picture preview, or select form the menu.

v 1.0

Egyszerű Puzzle alkalmazás.
- Válassz egy tájképet vagy egy fotót, állítsd be a méretet és kezd a kiválasztott nehézséggel.
- Az asztal görgethető és nagyítható és megváltoztatható a mintázata dupla érintéssel.
- A darabok mozgathatóak vagy érintéssel forgathatóak.
- A kép előnézetért dupla érintés egy darabra, vagy választható a menüből.

v 1.0

tasks:
picture preview for help    OK  picture is complete message __  piece emboss effect     __
select background           OK

bugs:
replacement after connect   __  size increase error         OK

*/
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;
import org.landroo.view.ScaleView;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class PuzzleMainActivity extends Activity implements UIInterface {

    private static final String TAG = "PuzzleMainActivity";
    private static final int SCROLL_INTERVAL = 10;
    private static final int SCROLL_ALPHA = 500;
    private static final int SCROLL_SIZE = 10;
    private static final int MINIMAL_SIZE = 40;
    private static final int GAP = 10;

    // virtual desktop
    private PuzzleView puzzleView;
    private int displayWidth;
    private int displayHeight;
    private int deskWidth;
    private int deskHeight;

    // scroll plane
    private ScaleView scaleView;
    private float sX = 0;
    private float sY = 0;
    private float mX = 0;
    private float mY = 0;
    private float zoomX = 1;
    private float zoomY = 1;
    private float xPos;
    private float yPos;
    private boolean afterMove = false;
    private double startAng = 0;

    // user event handler
    private UI ui = null;
    private float scrollX = 0;
    private float scrollY = 0;
    private float scrollMul = 1;
    private Timer scrollTimer = null;
    private Paint scrollPaint1 = new Paint();
    private Paint scrollPaint2 = new Paint();
    private int scrollAlpha = SCROLL_ALPHA;
    private int scrollBar = 0;
    private float barPosX = 0;
    private float barPosY= 0;
    private float xDest;
    private float yDest;
    private int halfX;
    private int halfY;
    private boolean scrollTo = false;

    private Timer selectTimer = new Timer();

    // background
    private int tileSize = 80;
    private Bitmap backBitmap;
    private Drawable backDrawable;// background bitmap drawable
    private boolean staticBack = false;// fix or scrollable background
    private int backColor = Color.LTGRAY;// background color
    private float rotation = 0;
    private float rx = 0;
    private float ry = 0;
    private boolean longPress = false;

    // puzzle
    private Bitmap fillBMP = null;
    private PuzzleClass puzzleClass;
    private Piece selItem = null;
    private Piece lastItem = null;
    private boolean easy = true;
    private String back = "grid";

    private Paint infoPaint = new Paint();

    private String imageName;
    private int pieceSize = 2;
    private boolean help = false;
    private String helpStr = "";
    private boolean backPressed = true;
    private boolean bFirst = true;
    private boolean popupShown = false;
    private boolean paused = false;

    private MainMenu mainMenu;

    private Handler mainHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            int what = msg.what;
            if(bundle.getInt("what") != 0)
                what = bundle.getInt("what");
            switch (what) {
                case 1:
                    puzzleView.postInvalidate();
                    break;
                case 100:// new image selected
                    imageName = bundle.getString("image_name");
                    pieceSize = bundle.getInt("size");
                    newGame(true, true, imageName, pieceSize + 1);
                    puzzleView.postInvalidate();
                    break;
                case 200:// easy
                    easy = true;
                    newGame(true, false, imageName, pieceSize + 1);
                    puzzleView.postInvalidate();

                    popupShown = false;
                    backPressed = false;

                    break;
                case 201:// hard
                    easy = false;
                    newGame(false, false, imageName, pieceSize + 1);
                    puzzleView.postInvalidate();

                    popupShown = false;
                    backPressed = false;

                    break;
                case 300:// back pressed once
                    popupShown = false;
                    break;
            }
            return true;
        }
    });

    // main view
    private class PuzzleView extends ViewGroup {

        public PuzzleView(Context context) {
            super(context);
        }

        // draw items
        @Override
        protected void dispatchDraw(@NonNull Canvas canvas) {
            drawBack(canvas);
            drawItems(canvas);
            drawScrollBars(canvas);
            drawInfo(canvas);

            super.dispatchDraw(canvas);
        }

        @Override
        protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
            // main
            View child = this.getChildAt(0);
            if (child != null)
                child.layout(0, 0, displayWidth, displayHeight);

            if(bFirst)
            {
                mainMenu.showMessagePoup(imageName, pieceSize);
                popupShown = true;

                bFirst = false;
            }

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(displayWidth, displayHeight);
            // main
            View child = this.getChildAt(0);
            if (child != null)
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_puzzle_main);

        Display display = getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        puzzleView = new PuzzleView(this);
        setContentView(puzzleView);

        LayoutInflater layoutInflater = (LayoutInflater) PuzzleMainActivity.this.getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        mainMenu = new MainMenu(PuzzleMainActivity.this, puzzleView, layoutInflater, displayWidth, displayHeight, mainHandler);

        ui = new UI(this);

        deskWidth = displayWidth * 3;
        deskHeight = displayHeight * 3;
        scaleView = new ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, puzzleView);

        infoPaint.setColor(0xFF444444);
        if (displayWidth < displayHeight) infoPaint.setTextSize(displayWidth / 20);
        else infoPaint.setTextSize(displayHeight / 20);

        scrollPaint1.setColor(Color.GRAY);
        scrollPaint1.setAntiAlias(true);
        scrollPaint1.setDither(true);
        scrollPaint1.setStyle(Paint.Style.STROKE);
        scrollPaint1.setStrokeJoin(Paint.Join.ROUND);
        scrollPaint1.setStrokeCap(Paint.Cap.ROUND);
        scrollPaint1.setStrokeWidth(SCROLL_SIZE);

        scrollPaint2.setColor(Color.CYAN);
        scrollPaint2.setAntiAlias(true);
        scrollPaint2.setDither(true);
        scrollPaint2.setStyle(Paint.Style.STROKE);
        scrollPaint2.setStrokeJoin(Paint.Join.ROUND);
        scrollPaint2.setStrokeCap(Paint.Cap.ROUND);
        scrollPaint2.setStrokeWidth(SCROLL_SIZE);

        if(!back.equals("")) {
            int resId = getResources().getIdentifier(back, "drawable", getPackageName());
            backBitmap = BitmapFactory.decodeResource(getResources(), resId);
            backDrawable = new BitmapDrawable(backBitmap);
            backDrawable.setBounds(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        }

        helpStr = getResources().getString(R.string.help);

        //Log.i(TAG, "created");
    }

    /**
     * Start a new game or draw a preview
     * @param easy      difficulty easy or hard
     * @param preview   preview or game
     * @param name      picture name
     * @param size      selected size (1-10)
     */
    private void newGame(boolean easy, boolean preview, String name, int size)
    {
        if(name.equals(""))
            return;

        if(loadBitmap(name)) {
            float w = fillBMP.getWidth();
            float h = fillBMP.getHeight();
            //Log.i(TAG, "bitmap: " + w + " " + h);
            int dv;
            int ms = MINIMAL_SIZE; // minimal piece size
            float ra, xp, yp;
            if (w > h) {
                dv = (int) (w / ms / 10 * size);
                ra = h / w;
                xp = dv;
                yp = dv * ra;

            } else {
                dv = (int) (w / ms / 10 * size);
                ra = w / h;
                xp = dv * ra;
                yp = dv;
            }
            //Log.i(TAG, "" + (int) xp + " " + (int) yp);
            puzzleClass = new PuzzleClass();
            puzzleClass.newPuzzle(fillBMP, (deskWidth - fillBMP.getWidth()) / 2, (deskHeight - fillBMP.getHeight()) / 2, (int) xp, (int) yp);
            if (!preview)
                puzzleClass.replacePieces(deskWidth, deskHeight, easy);

            zoomX = 1;
            zoomY = 1;

            help = true;
            puzzleClass.help = true;
            helpStr = getResources().getString(R.string.help);

            puzzleView.postInvalidate();
        }
    }

    /**
     * Load the picture
     * @param name  Picture name
     * @return      success
     */
    private boolean loadBitmap(String name)
    {
        try{
            if(fillBMP != null) {
                fillBMP.recycle();
                fillBMP = null;
            }

            if(!name.contains("/")) {
                int resId = getResources().getIdentifier(name, "drawable", getPackageName());
                fillBMP = BitmapFactory.decodeResource(getResources(), resId);
            }
            else {
                BitmapFactory.Options bfo = new BitmapFactory.Options();
                bfo.inSampleSize = 2;
                File file = new File(name);
                if (file.exists()) {
                    fillBMP = BitmapFactory.decodeFile(file.getAbsolutePath(), bfo);
                }
            }

            deskWidth = fillBMP.getWidth() * 3;
            deskHeight = fillBMP.getHeight() * 3;
            scaleView = new ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, puzzleView);
        }
        catch (OutOfMemoryError ex) {
            Log.i(TAG, "Out of memory load bitmap");
            return false;
        }

        return true;
    }

    /**
     * override back pressed
     */
    @Override
    public void onBackPressed()
    {
        if(backPressed) {
            super.onBackPressed();
        }
        else
        {
            if(!popupShown)
            {
                mainMenu.showMessagePoup(imageName, pieceSize);
                popupShown = true;
                backPressed = true;
            }
        }
    }

    /**
     * override on pause
     */
    @Override
    public void onPause()
    {
        paused = true;

        if(mainMenu.popupWindow != null)
            mainMenu.popupWindow.dismiss();

        if(selectTimer != null)
        {
            selectTimer.cancel();
            selectTimer = null;
        }

        if(scrollTimer != null)
        {
            scrollTimer.cancel();
            scrollTimer = null;
        }

        saveState();

        //Log.i(TAG, "paused");
        super.onPause();
    }

    /**
     * override on resume
     */
    @Override
    protected void onResume()
    {
        paused = false;

        scrollTimer = new Timer();
        scrollTimer.scheduleAtFixedRate(new ScrollTask(), 0, SCROLL_INTERVAL);

        if(loadState()) {
            bFirst = false;
            backPressed = false;
        }

        //Log.i(TAG, "resumed");
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_puzzle_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            help = true;
            puzzleClass.help = true;
            helpStr = getResources().getString(R.string.help);
            puzzleView.postInvalidate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // main touch event
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return ui.tapEvent(event);
    }

    // begin a gesture
    @Override
    public void onDown(float x, float y) {
        afterMove = false;
        scrollAlpha = SCROLL_ALPHA;

        scaleView.onDown(x, y);

        scrollBar = checkBars(x, y);
        if(scrollBar == 1) {
            barPosX = x - barPosX;
        }
        else if(scrollBar == 2) {
            barPosY = y - barPosY;
        }
        else if (lastItem != null) {
            setLinkFor(lastItem, false, false);
            setLinkBak(lastItem, false, false);
            lastItem = null;
            selectTimer.cancel();
        }

        sX = x / zoomX;
        sY = y / zoomY;

        mX = x / zoomX;
        mY = y / zoomY;

        xPos = scaleView.xPos();
        yPos = scaleView.yPos();

        if(!help) {
            if (selItem != null && selItem.rotating) {// start to rotate the selected piece
                startAng = Utils.getAng((selItem.px + selItem.u) * zoomX + xPos, (selItem.py + selItem.v) * zoomY + yPos, mX * zoomX, mY * zoomY);
                selItem.rotating = true;
            } else {// put the top the selected piece
                selItem = null;
                if (puzzleClass != null) {
                    for (int i = puzzleClass.items.size() - 1; i >= 0; i--) {
                        Piece item = puzzleClass.items.get(i);
                        if (item.isInside(x - xPos, y - yPos, zoomX, zoomY)) {
                            puzzleClass.reorder(item, i);
                            //Piece p = item.linkFor;
                            selItem = item;
                            selItem.selected = true;
                            break;
                        }
                    }
                }
            }
        }
        else {
            puzzleView.postInvalidate();
        }
    }

    // finish tap
    @Override
    public void onUp(float x, float y) {
        scaleView.onUp(x, y);

        if (selItem != null) {
            //new CheckTask().execute(0);
            boolean link = puzzleClass.linkPieces(selItem);
            if(link) {
                setLinkFor(selItem, false, true);
                setLinkBak(selItem, false, true);

                lastItem = selItem;
                selectTimer.cancel();
                selectTimer = new Timer();
                selectTimer.scheduleAtFixedRate(new selectTimerTask(), 3000, 1000);
            }
            else {
                setLinkFor(selItem, false, false);
                setLinkBak(selItem, false, false);
            }
            selItem = null;
            afterMove = true;
            puzzleView.postInvalidate();
        }

        longPress = false;

        scrollMul = 0;
    }

    // a single tap
    @Override
    public void onTap(float x, float y) {
        scrollAlpha = SCROLL_ALPHA;

        xPos = scaleView.xPos();
        yPos = scaleView.yPos();

        if(help) {
            help = false;
            puzzleClass.help = false;
            helpStr = "";
            puzzleView.postInvalidate();
        } else if (selItem == null && !easy) {
            for (int i = puzzleClass.items.size() - 1; i >= 0; i--) {
                Piece item = puzzleClass.items.get(i);
                if (item.isInside(x - xPos, y - yPos, zoomX, zoomY)) {
                    puzzleClass.reorder(item, i);
                    selItem = item;
                    selItem.rotating = true;
                    setLinkFor(selItem, true, false);
                    setLinkBak(selItem, true, false);
                    //Log.i(TAG, "rotating " + selItem.x + selItem.y);
                    break;
                }
            }
        }
    }

    @Override
    public void onHold(float x, float y) {
        if(longPress)
            return;

        longPress = true;
    }

    // move finger
    @Override
    public void onMove(float x, float y) {
        scrollAlpha = SCROLL_ALPHA;

        mX = x / zoomX;
        mY = y / zoomY;

        if(scrollBar != 0) {
            // vertical scroll
            if(scrollBar == 1) {
                float xp = -(x - barPosX) / (displayWidth / (deskWidth * zoomX));
                Log.i(TAG, "" + xp);
                if(xp < 0 && xp > displayWidth - deskWidth * zoomX) {
                    xPos = xp;
                }
            }
            else {
                float yp = -(y - barPosY) / (displayHeight / (deskHeight * zoomY));
                Log.i(TAG, "" + yp);
                if(yp < 0 && yp > displayHeight - deskHeight * zoomY) {
                    yPos = yp;
                }
            }
            scaleView.setPos(xPos, yPos);
            puzzleView.postInvalidate();
        }
        else if (selItem != null) {
            if (selItem.isInside(x - xPos, y - yPos, zoomX, zoomY)) {// move piece
                movePiece();
            } else if (selItem.rotating) {// rotate piece
                startAng = setAngle(selItem);
            }

            puzzleView.postInvalidate();
        } else
            scaleView.onMove(x, y);

        sX = mX;
        sY = mY;
    }

    @Override
    public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2) {
        if (!afterMove)
            scaleView.onSwipe(direction, velocity, x1, y1, x2, y2);
    }

    @Override
    public void onDoubleTap(float x, float y) {
        if (selItem == null) {
            boolean bOk = false;
            for (int i = puzzleClass.items.size() - 1; i >= 0; i--) {
                Piece item = puzzleClass.items.get(i);
                if (item.isInside(x - xPos, y - yPos, zoomX, zoomY)) {
                    bOk = true;
                    break;
                }
            }

            if(bOk){
                help = true;
                puzzleClass.help = true;
                helpStr = getResources().getString(R.string.help);
                puzzleView.postInvalidate();
            }
            else
            {
                if(back.equals("grid"))
                    back = "grid1";
                else if(back.equals("grid1"))
                    back = "grid2";
                else if(back.equals("grid2"))
                    back = "";
                else if(back.equals(""))
                    back = "grid";

                if(!back.equals("")) {
                    int resId = getResources().getIdentifier(back, "drawable", getPackageName());
                    backBitmap = BitmapFactory.decodeResource(getResources(), resId);
                    backDrawable = new BitmapDrawable(backBitmap);
                    backDrawable.setBounds(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
                }
                else if(backBitmap != null){
                    backBitmap.recycle();
                    backBitmap = null;
                    backDrawable = null;
                }
                puzzleView.postInvalidate();
            }
        }
    }

    @Override
    public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff) {
        scaleView.onZoom(mode, x, y, distance, xdiff, ydiff);

        zoomX = scaleView.getZoomX();
        zoomY = scaleView.getZoomY();
    }

    @Override
    public void onRotate(int mode, float x, float y, float angle) {

    }

    @Override
    public void onFingerChange() {

    }

    // draw background
    private void drawBack(Canvas canvas) {
        if (backDrawable != null) {
            // static back or tiles
            if (staticBack) {
                backDrawable.setBounds(0, 0, displayWidth, displayHeight);
                backDrawable.draw(canvas);
            } else {
                if (scaleView != null) {
                    xPos = scaleView.xPos();
                    yPos = scaleView.yPos();
                }
                for (float x = 0; x < deskWidth; x += tileSize) {
                    for (float y = 0; y < deskHeight; y += tileSize) {
                        // distance of the tile center from the rotation center
                        final float dis = (float) Utils.getDist(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY);
                        // angle of the tile center from the rotation center
                        final float ang = (float) Utils.getAng(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY);

                        // coordinates of the block after rotation
                        final float cx = dis * (float) Math.cos((rotation + ang) * Utils.DEGTORAD) + rx * zoomX + xPos;
                        final float cy = dis * (float) Math.sin((rotation + ang) * Utils.DEGTORAD) + ry * zoomY + yPos;

                        if (cx >= -tileSize && cx <= displayWidth + tileSize && cy >= -tileSize && cy <= displayHeight + tileSize) {
                            backDrawable.setBounds(0, 0, (int) (tileSize * zoomX) + 1, (int) (tileSize * zoomY) + 1);

                            canvas.save();
                            canvas.rotate(rotation, rx * zoomX + xPos, ry * zoomY + yPos);
                            canvas.translate(x * zoomX + xPos, y * zoomY + yPos);
                            backDrawable.draw(canvas);
                            canvas.restore();
                        }
                    }
                }
            }
        } else {
            canvas.drawColor(backColor);
        }
    }

    // draw piece information
    private void drawInfo(Canvas canvas) {

        if (selItem != null) {
            canvas.drawText("" + (int)selItem.px + "-" + (int)selItem.py + " " + (int) selItem.getAngle() + "°", 10, infoPaint.getTextSize(), infoPaint);
        }
        else if(help) {
            canvas.drawText(helpStr, 10, infoPaint.getTextSize(), infoPaint);
        }
        else if(puzzleClass != null){
            canvas.drawText("" + (int)puzzleClass.xNum + "x" + (int)puzzleClass.yNum + " " + (int)(puzzleClass.xNum * puzzleClass.yNum), 10, infoPaint.getTextSize(), infoPaint);
        }
    }

    // draw puzzle pieces
    private void drawItems(Canvas canvas) {
        if (scaleView != null) {
            xPos = scaleView.xPos();
            yPos = scaleView.yPos();
        }
        if(puzzleClass != null)
            puzzleClass.draw(canvas, xPos, yPos, zoomX, zoomY, displayWidth, displayHeight);
    }

    // next linked items
    private void moveLinkFor(Piece piece, float dx, float dy) {
        piece.px += dx;
        piece.py += dy;
        piece.selected = true;
        if (piece.linkFor != null) {
            //Log.i(TAG, "linkFor: " + piece.getId());
            moveLinkFor(piece.linkFor, dx, dy);
        }
    }

    // previous linked items
    private void moveLinkBak(Piece piece, float dx, float dy) {
        piece.px += dx;
        piece.py += dy;
        piece.selected = true;
        if (piece.linkBak != null) {
            //Log.i(TAG, "linkBak: " + piece.getId());
            moveLinkBak(piece.linkBak, dx, dy);
        }
    }

    /**
     * set the angle of the linked pieces
     * @param piece piece
     * @return angle
     */
    private double setAngle(Piece piece)
    {
        double dAng = Utils.getAng((selItem.px + selItem.u) * zoomX + xPos, (selItem.py + selItem.v) * zoomY + yPos, mX * zoomX, mY * zoomY);
        float max = 1;
        if (max >= 90) max = 90;
        if (max >= 1) dAng = (((int) (dAng / max)) * max);

        piece.setAngle(selItem.getAngle() + (float) (dAng - startAng));

        // go backward
        Piece p1 = piece.linkBak;
        Piece p2 = p1;
        while(p1 != null) {
            p2 = p1;
            p1 = p1.linkBak;
        }
        // go forward and check
        while(p2 != null) {
            p2.setAngle(selItem.getAngle() + (float) (dAng - startAng));
            p2.rotating = true;
            p2 = p2.linkFor;
        }

        // go forward
        p1 = piece.linkFor;
        p2 = p1;
        while(p1 != null) {
            p2 = p1;
            p1 = p1.linkFor;
        }
        // go backward and check
        while(p2 != null) {
            p2.setAngle(selItem.getAngle() + (float) (dAng - startAng));
            p2.rotating = true;
            p2 = p2.linkBak;
        }

        return dAng;
    }

    /**
     * next linked items
     * @param piece
     * @param rot
     * @param sel
     */
    private void setLinkFor(Piece piece, boolean rot, boolean sel) {
        if(piece != null) {
            piece.rotating = rot;
            piece.selected = sel;
            if (piece.linkFor != null) {
                //Log.i(TAG, "linkFor: " + piece.getId());
                setLinkFor(piece.linkFor, rot, sel);
            }
        }
    }

    /**
     * previous linked items
     * @param piece
     * @param rot
     * @param sel
     */
    private void setLinkBak(Piece piece, boolean rot, boolean sel) {
        if(piece != null) {
            piece.rotating = rot;
            piece.selected = sel;
            if (piece.linkBak != null) {
                //Log.i(TAG, "linkme: " + piece.getId());
                setLinkBak(piece.linkBak, rot, sel);
            }
        }
    }

    /**
     * after a success drop remove the highlight
     */
    class selectTimerTask extends TimerTask {
        public void run() {
            //Log.i(TAG, "timer " + lastItem);
            setLinkFor(lastItem, false, false);
            setLinkBak(lastItem, false, false);
            lastItem = null;
            selectTimer.cancel();
            mainHandler.sendEmptyMessage(1);
        }
    }

    /**
     * show position indicators
     * @param canvas canvas
     */
    private void drawScrollBars(Canvas canvas)
    {
        float x, y;
        float xSize = displayWidth / ((deskWidth * zoomX) / displayWidth);
        float ySize = displayHeight / ((deskHeight * zoomY) / displayHeight);

        x = (displayWidth / (deskWidth * zoomX)) * -xPos;
        y = displayHeight - SCROLL_SIZE - 2;
        if(xSize < displayWidth) {
            if (scrollBar == 1) {
                canvas.drawLine(x, y, x + xSize, y, scrollPaint2);
            }
            else {
                canvas.drawLine(x, y, x + xSize, y, scrollPaint1);
            }
        }

        x = displayWidth - SCROLL_SIZE - 2;
        y = (displayHeight / (deskHeight * zoomY)) * -yPos;
        if(ySize < displayHeight) {
            if (scrollBar == 2) {
                canvas.drawLine(x, y, x, y + ySize, scrollPaint2);
            }
            else {
                canvas.drawLine(x, y, x, y + ySize, scrollPaint1);
            }
        }
    }

    /**
     *  scroll task for scrolling background
     */
    class ScrollTask extends TimerTask
    {
        public void run()
        {
            if(paused)
                return;
            try {
                boolean redraw = false;

                // scroll to selected object
                if (scrollMul < .05f) {
                    scrollTo = false;
                    scrollX = 0;
                    scrollY = 0;
                    //rectGroup.setToPos(selGroupId, rectSize / 2);
                } else {
                    if (scrollTo) {
                        if ((int) Math.abs(xDest - xPos) < halfX || (int) Math.abs(yDest - yPos) < halfY)
                            scrollMul -= 0.05f;
                        else scrollMul += 0.05f;
                        redraw = true;
                    }
                }
                // left and top scroll in zoomed
                if (xPos + scrollX < displayWidth - deskWidth * zoomX || xPos + scrollX > 0) {
                    scrollX = 0;
                }
                if (yPos + scrollY < displayHeight - deskHeight * zoomY || yPos + scrollY > 0) {
                    scrollY = 0;
                }
                // auto scroll paper
                if (selItem != null && scrollX != 0 || scrollY != 0) {
                    xPos += scrollX * scrollMul;
                    yPos += scrollY * scrollMul;
                    selItem.px -= scrollX * scrollMul / zoomX;
                    selItem.py -= scrollY * scrollMul / zoomY;
                    if (selItem.linkFor != null)
                        moveLinkFor(selItem.linkFor, -scrollX * scrollMul / zoomX, -scrollY * scrollMul / zoomY);
                    if (selItem.linkBak != null)
                        moveLinkBak(selItem.linkBak, -scrollX * scrollMul / zoomX, -scrollY * scrollMul / zoomY);
                    if(scrollMul < 10) scrollMul += 0.01f;
                    scaleView.setPos(xPos, yPos);
                }
                if (scrollAlpha > 32) {
                    scrollAlpha--;
                    if (scrollAlpha > 255) scrollPaint1.setAlpha(255);
                    else scrollPaint1.setAlpha(scrollAlpha);
                    redraw = true;
                }
                if (redraw)
                    puzzleView.postInvalidate();
            }
            catch(Exception ex) {
                Log.i(TAG, "ScrollTask " + ex);
            }
        }
    }

    /**
     * scroll the background when item is outside the screen
     */
    private void movePiece()
    {
        boolean bOK = true;

        float dx = mX - sX;
        float dy = mY - sY;

        scrollX = 0;
        scrollY = 0;

        // item inside the desk
        if((selItem.px + selItem.width) + dx > deskWidth && dx > 0) bOK = false;
        if((selItem.px) + dx < 0 && dx < 0) bOK = false;
        if((selItem.py + selItem.height) + dy > deskHeight && dy > 0) bOK = false;
        if((selItem.py) + dy < 0 && dy < 0) bOK = false;

        float scx = scrollX;
        float scy = scrollY;

        // scroll background under item
        if(xPos + (selItem.px + selItem.width) * zoomX >= displayWidth) scrollX = -1f;// scroll left
        if(xPos + (selItem.px) * zoomX <= 0) scrollX = 1f;// scroll right
        if(yPos + (selItem.py + selItem.height) * zoomY >= displayHeight) scrollY = -1f;// scroll up
        if(yPos + (selItem.py) * zoomY <= 0) scrollY = 1f;// scroll down

        if(scx != scrollX && scrollMul == 0) scrollMul = 1;
        if(scy != scrollY && scrollMul == 0) scrollMul = 1;
        if(scrollX == 0.0 && scrollY == 0.0) scrollMul = 1;

        if(bOK)
        {
            selItem.px += mX - sX;
            selItem.py += mY - sY;
            if(selItem.linkFor != null)
                moveLinkFor(selItem.linkFor, mX - sX, mY - sY);
            if(selItem.linkBak != null)
                moveLinkBak(selItem.linkBak, mX - sX, mY - sY);
        }
    }

    private void saveState() {
        SharedPreferences outSettings = getSharedPreferences("org.landroo.puzzle_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = outSettings.edit();
        editor.putString("image_name", imageName);
        editor.putInt("piece_size", pieceSize);
        editor.putBoolean("easy", easy);
        editor.putString("back", back);
        editor.apply();
        //Log.i(TAG, "save " + imageName);
        if(puzzleClass != null) {
            puzzleClass.savseState(this);
            puzzleClass = new PuzzleClass();
        }

        if(fillBMP != null) {
            fillBMP.recycle();
            fillBMP = null;
        }
    }

    private boolean loadState() {
        SharedPreferences inSettings = getSharedPreferences("org.landroo.puzzle_preferences", MODE_PRIVATE);
        imageName = inSettings.getString("image_name", "");
        pieceSize = inSettings.getInt("piece_size", 2);
        easy = inSettings.getBoolean("easy", true);
        back = inSettings.getString("back", "grid");
        //Log.i(TAG, "load " + imageName);
        if(imageName.equals(""))
            return false;

        if(loadBitmap(imageName)) {
            puzzleClass = new PuzzleClass();
            puzzleClass.loadState(this, fillBMP);
        }

        if(!back.equals("")) {
            int resId = getResources().getIdentifier(back, "drawable", getPackageName());
            backBitmap = BitmapFactory.decodeResource(getResources(), resId);
            backDrawable = new BitmapDrawable(backBitmap);
            backDrawable.setBounds(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        }

        return true;
    }

    /**
     * check tap on scroll bars
     * @param x float position x
     * @param y float position y
     * @return  int 1 vertical scroll bar 2 horizontal scroll bar
     */
    private int checkBars(float x, float y) {
        float px, py;
        float xSize = displayWidth / ((deskWidth * zoomX) / displayWidth);
        float ySize = displayHeight / ((deskHeight * zoomY) / displayHeight);
        px = (displayWidth / (deskWidth * zoomX)) * -xPos;
        py = displayHeight - SCROLL_SIZE - 2;
        //Log.i(TAG, "" + x + " " + xp + " " + (x+ xSize) + " " + y + " " + yp + " " + (y + SCROLL_SIZE));
        if(x > px && y > py - GAP && x < px + xSize && y < py + SCROLL_SIZE + GAP && xSize < displayWidth) {
            barPosX = px;
            return 1;
        }

        px = displayWidth - SCROLL_SIZE - 2;
        py = (displayHeight / (deskHeight * zoomY)) * -yPos;
        if(x > px - GAP && y > py && x < px + SCROLL_SIZE + GAP && y < py + ySize && ySize < displayHeight) {
            barPosY = py;
            return 2;
        }

        return 0;
    }

}
