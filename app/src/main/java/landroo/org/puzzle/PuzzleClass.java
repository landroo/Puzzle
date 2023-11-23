package landroo.org.puzzle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Shader;
import android.graphics.Matrix;
import android.util.Log;
import android.util.StringBuilderPrinter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rkovacs on 2015.09.15..
 */
public class PuzzleClass {

    private static final String TAG = "PuzzleClass";
    public static final double SEGMENTS = 8;// bezier curve seamless

    public List<Piece> items = new ArrayList<>();

    private Paint fillPaint;
    private Paint strokePaint;
    private Paint selectPaint;
    private Paint rotPaint;
    private Paint backPaint;

    public float xNum;// x size piece number
    public float yNum;// y size piece number
    public float width;// piece width
    public float height;// piece height

    private List<PointF> pin = new ArrayList<>();
    private String sides = "";

    public boolean template = false;
    public boolean help = false;

    // constructor
    public PuzzleClass()
    {
        strokePaint = new Paint();
        strokePaint.setDither(true);
        strokePaint.setColor(0x88A9D755);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(3);

        selectPaint = new Paint();
        selectPaint.setDither(true);
        selectPaint.setColor(0x88EC5940);
        selectPaint.setStyle(Paint.Style.STROKE);
        selectPaint.setAntiAlias(true);
        selectPaint.setStrokeWidth(3);

        rotPaint = new Paint();
        rotPaint.setDither(true);
        rotPaint.setColor(0x88209B76);
        rotPaint.setStyle(Paint.Style.STROKE);
        rotPaint.setAntiAlias(true);
        rotPaint.setStrokeWidth(3);

        backPaint = new Paint();
        backPaint.setDither(true);
        backPaint.setColor(0x88AAAAAA);
        backPaint.setStyle(Paint.Style.STROKE);
        backPaint.setAntiAlias(true);
        backPaint.setStrokeWidth(3);
    }

    public void newPuzzle(Bitmap fillBMP, float sx, float sy, float xNo, float yNo) {
        items.clear();
        pin.clear();

        BitmapShader fillBMPshader = new BitmapShader(fillBMP, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        fillPaint = new Paint();
        fillPaint.setColor(0xFFFFFFFF);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(fillBMPshader);

        xNum = xNo;
        yNum = yNo;

        width = Math.round((float) fillBMP.getWidth() / xNo);
        height = Math.round((float) fillBMP.getHeight() / yNo);

        if(width > height)
            createPin((int)height, (int)width);
        else
            createPin((int)width, (int)height);

        createPieces(sx, sy, (int)width, (int)height);
    }

    // cut the picture to random pieces
    public void createPieces(float sx, float sy, int width, int height) {
        int sides[][][] = createSides();
        int cnt = 0;
        for (int x = 0; x < xNum; x++) {
            for (int y = 0; y < yNum; y++) {
                Piece piece = new Piece(sx + x * width, sy + y * height, x * -width, y * -height, createShape(width, height, sides[x][y]), width, height, 0, x, y, null, null, cnt++, sx + x * width, sy + y * height);
                items.add(piece);
            }
        }

        // store the sides
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < sides.length; i++)
            for(int j = 0; j < sides[i].length; j++)
                for(int k = 0; k < sides[i][j].length; k++)
                    sb.append("" + sides[i][j][k]);
        this.sides = sb.toString();
    }

    // draw pieces to the canvas
    public void draw(Canvas canvas, float xPos, float yPos, float zx, float zy, int displayWidth, int displayHeight) {
        Matrix trans = new Matrix();

        if(template) {
            for (Piece item : items) {
                canvas.save();
                canvas.translate(xPos + item.ox * zx, yPos + item.oy * zy);
                canvas.drawPath(item.path, backPaint);
                canvas.restore();
            }
        }

        if(help) {
            for (Piece item : items) {
                trans.setTranslate(item.tx, item.ty);
                fillPaint.getShader().setLocalMatrix(trans);

                canvas.save();
                canvas.translate(xPos + item.ox * zx, yPos + item.oy * zy);
                canvas.drawPath(item.path, fillPaint);
                canvas.drawPath(item.path, backPaint);
                canvas.restore();
            }
        }
        else {
            for (Piece item : items) {

                // distance of the tile center from the rotation center
                final float dis = (float) Utils.getDist(zx, zy, (item.px + width / 2) * zx, (item.py + height / 2) * zy);
                // angle of the tile center from the rotation center
                final float ang = (float) Utils.getAng(zx, zy, (item.px + width / 2) * zx, (item.py + height / 2) * zy);

                // coordinates of the block after rotation
                final float cx = dis * (float) Math.cos((ang) * Utils.DEGTORAD) + xPos;
                final float cy = dis * (float) Math.sin((ang) * Utils.DEGTORAD) + yPos;

                if (cx >= -width && cx <= displayWidth + width && cy >= -height && cy <= displayHeight + height) {
                    trans.setTranslate(item.tx, item.ty);
                    fillPaint.getShader().setLocalMatrix(trans);

                    canvas.save();
                    canvas.translate(xPos + item.px * zx, yPos + item.py * zy);
                    canvas.scale(zx, zy);
                    canvas.rotate(item.getAngle(), item.u, item.v);
                    canvas.drawPath(item.path, fillPaint);
                    if (item.rotating)
                        canvas.drawPath(item.path, rotPaint);
                    else if (item.selected)
                        canvas.drawPath(item.path, selectPaint);
                    else
                        canvas.drawPath(item.path, strokePaint);
                    canvas.restore();
                }
            }
        }
    }

    // put the top the selected piece
    public void reorder(Piece item, int idx) {
        int last = items.size() - 1;
        Collections.swap(items, last, idx);

        int i;
        Piece p = item.linkFor;
        while(p != null){
            i = items.indexOf(p);
            last--;
            Collections.swap(items, last, i);
            p = p.linkFor;
        }
        p = item.linkBak;
        while(p != null){
            i = items.indexOf(p);
            last--;
            Collections.swap(items, last, i);
            p = p.linkBak;
        }
    }

    // set the random pins or holes to the shape
    private int[][][] createSides() {
        int sides[][][] = new int[(int) xNum][(int) yNum][4];
        int rs;

        for (int x = 0; x < xNum; x++) {
            for (int y = 0; y < yNum; y++) {
                rs = Utils.random(0, 1, 1);
                if (rs == 1) {
                    if (y > 0) {
                        sides[x][y - 1][2] = 1;// bottom side
                        sides[x][y][0] = 2;// up side
                    }
                } else {
                    if (y > 0) {
                        sides[x][y - 1][2] = 2;// bottom side
                        sides[x][y][0] = 1;// up side
                    }
                }

                rs = Utils.random(0, 1, 1);
                if (rs == 1) {
                    if (x > 0) {
                        sides[x - 1][y][1] = 1;// right side
                        sides[x][y][3] = 2;// left side
                    }
                } else {
                    if (x > 0) {
                        sides[x - 1][y][1] = 2;// right side
                        sides[x][y][3] = 1;// left side
                    }
                }
            }
        }

        return sides;
    }

    // cut the picture to puzzle shapes
    private List<PointF> createShape(int w, int h, int[] sides) {
        List<PointF> points = new ArrayList<>();

        points.add(new PointF(0, 0));
        addUpPin(points, w, h, sides[0]);
        points.add(new PointF(w, 0));
        addRightPin(points, w, h, sides[1]);
        points.add(new PointF(w, h));
        addBottomPin(points, w, h, sides[2]);
        points.add(new PointF(0, h));
        addLeftPin(points, w, h, sides[3]);
        points.add(new PointF(0, 0));

        return points;
    }

    // draw pin or a hole on the top
    private void addUpPin(List<PointF> points, int w, int h, int out) {
        if (out != 0) {
            // simple add the up pin
            if (out == 1)
                points.addAll(pin);
            // from up to down
            if (out == 2)
                for (int i = 0; i < pin.size(); i++)
                    points.add(new PointF(pin.get(i).x, -pin.get(i).y));
        }
    }

    // draw pin or a hole on the bottom
    private void addBottomPin(List<PointF> points, int w, int h, int out) {
        if (out != 0) {
            // shift down and reverse
            if (out == 1)
                for (int i = pin.size() - 1; i >= 0; i--)
                    points.add(new PointF(pin.get(i).x, -pin.get(i).y + h));
            // shift down and reverse and from up to down
            if (out == 2)
                for (int i = pin.size() - 1; i >= 0; i--)
                    points.add(new PointF(pin.get(i).x, pin.get(i).y + h));

        }
    }

    // draw a right pin or hole
    private void addRightPin(List<PointF> points, int w, int h, int out) {
        if (out != 0) {
            // turn rigth
            float[][] m = new float[pin.size()][2];
            for (int i = 0; i < pin.size(); i++) {
                m[i][0] = pin.get(i).x;
                m[i][1] = pin.get(i).y;
            }
            for (int i = 0; i < pin.size(); i++)
                Utils.rotateByNinetyToRight(m);

            if (out == 2)
                for (int i = 0; i < m.length; i++)
                    points.add(new PointF(m[i][0] + w, m[i][1]));

            if (out == 1)
                for (int i = 0; i < m.length; i++)
                    points.add(new PointF(-m[i][0] + w, m[i][1]));
        }
    }

    // draw a left pin or hole
    private void addLeftPin(List<PointF> points, int w, int h, int out) {
        if (out != 0) {
            // turn right
            float[][] m = new float[pin.size()][2];
            for (int i = 0; i < pin.size(); i++) {
                m[i][0] = pin.get(i).x;
                m[i][1] = pin.get(i).y;
            }
            for (int i = 0; i < pin.size(); i++)
                Utils.rotateByNinetyToRight(m);

            if (out == 1)
                for (int i = m.length - 1; i >= 0; i--)
                    points.add(new PointF(m[i][0], m[i][1]));

            if (out == 2)
                for (int i = m.length - 1; i >= 0; i--)
                    points.add(new PointF(-m[i][0], m[i][1]));
        }
    }

    // calculate Bezier curve
    public List<PointF> calcBezier(PointF anchor1, PointF anchor2, PointF control1, PointF control2) {
        double step = 1 / SEGMENTS;
        ArrayList<PointF> line = new ArrayList<PointF>();

        line.add(new PointF(anchor1.x, anchor1.y));

        PointF a1 = anchor1;
        PointF a2 = anchor2;

        PointF c1 = control1;
        PointF c2 = control2;

        double posx;
        double posy;

        //this loops draws each step of the curve
        for (double u = 0; u <= 1; u += step) {
            posx = Math.pow(u, 3) * (a2.x + 3 * (c1.x - c2.x) - a1.x) + 3 * Math.pow(u, 2) * (a1.x - 2 * c1.x + c2.x) + 3 * u * (c1.x - a1.x) + a1.x;
            posy = Math.pow(u, 3) * (a2.y + 3 * (c1.y - c2.y) - a1.y) + 3 * Math.pow(u, 2) * (a1.y - 2 * c1.y + c2.y) + 3 * u * (c1.y - a1.y) + a1.y;
            line.add(new PointF((float) posx, (float) posy));
        }

        // As a final step, make sure the curve ends on the second anchor
        line.add(new PointF(anchor2.x, anchor2.y));

        return line;
    }

    // create a side poly line
    private void createPin(int w, int h) {
        PointF anchor1, anchor2;
        PointF control1, control2;

        anchor1 = new PointF(0, 0);
        anchor2 = new PointF(w * 2 / 5, -h / 10);
        control1 = new PointF(w / 5, 0);
        control2 = new PointF(w / 2, h / 10);
        pin.addAll(calcBezier(anchor1, anchor2, control1, control2));

        anchor1 = new PointF(w * 2 / 5, -h / 10);
        anchor2 = new PointF(w * 2 / 3, -h / 5);
        control1 = new PointF(w / 5, -h * 2 / 5);
        control2 = new PointF(w * 4 / 5, -h / 3);
        pin.addAll(calcBezier(anchor1, anchor2, control1, control2));

        anchor1 = new PointF(w * 2 / 3, -h / 5);
        anchor2 = new PointF(w, 0);
        control1 = new PointF(w / 2, 0);
        control2 = new PointF(w / 5 * 4, 0);
        pin.addAll(calcBezier(anchor1, anchor2, control1, control2));
    }

    // check dropped piece
    public boolean linkPieces(Piece selItem) {
        //Log.i(TAG, "selItem " + selItem.getId());
        int next;
        for (Piece piece : items) {
            // not already connected
            if(!sameGroup(piece, selItem)) {
                // different pieces
                if(selItem.getId() != piece.getId()) {
                    // right side
                    next = selItem.checkNext(piece.sx, piece.sy);
                    if(next != 0) {
                        // close enough
                        if (selItem.checkClose(piece)) {
                            // same angle
                            if(selItem.checkAngle(piece.getAngle())) {
                                // the proper faces are each other
                                if(selItem.checkFace(piece, next)) {
                                    selItem.fixpos(next, piece);
                                    linkPiece(piece, selItem);
                                    setUV(selItem);

                                    return true;
                                }
                                //else
                                    //Log.i(TAG, "face error " + piece.getId() + " " + selItem.getId());
                            }
                            //else
                                //Log.i(TAG, "different angle " + piece.getId() + " " + selItem.getId());
                        }
                        //else
                            //Log.i(TAG, "too far " + piece.getId() + " " + selItem.getId());
                    }
                    //else
                        //Log.i(TAG, "not neighbor " + piece.getId() + " " + selItem.getId());
                }
                //else
                    //Log.i(TAG, "same piece " + piece.getId() + " " + selItem.getId());
            }
            //else
                //Log.i(TAG, "same group " + piece.getId() + " " + selItem.getId());
        }
        return false;
    }

    // find last linked pieces and linked them
    private void linkPiece(Piece piece, Piece selItem) {
        Piece item1 = null;
        Piece item2 = null;
        if(piece.linkBak == null){
            item1 = piece;
        }
        else {
            Piece p = piece.linkBak;
            while(p != null) {
                item1 = p;
                p = p.linkBak;
            }
        }

        if(selItem.linkFor == null) {
            item2 = selItem;
        }
        else {
            Piece p = selItem.linkFor;
            while(p != null){
                item2 = p;
                p = p.linkFor;
            }
        }

        item1.linkBak = item2;
        item2.linkFor = item1;
        Log.i(TAG, item2.getId() + " linked to " + item1.getId());
    }

    // check the two item is in the same group
    private boolean sameGroup(Piece item1, Piece item2)
    {
        if(item1.linkFor == null && item1.linkBak == null)
            return false;
        if(item2.linkFor == null && item2.linkBak == null)
            return false;

        if(sameLinkFor(item1, item2))
            return true;
        if(sameLinkBak(item1, item2))
            return true;

        return false;
    }

    private boolean sameLinkFor(Piece p1, Piece p2)
    {
        if(p1 == p2)
            return true;
        if(p1.linkFor != null)
            return sameLinkFor(p1.linkFor, p2);
        return false;
    }

    private boolean sameLinkBak(Piece p1, Piece p2)
    {
        if(p1 == p2)
            return true;
        if(p1.linkBak != null)
            return sameLinkBak(p1.linkBak, p2);
        return false;
    }

    // set the rotation center for the pieces
    private void setUV(Piece piece)
    {
        float maxX = 0;
        float maxY = 0;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;

        // go backward
        Piece p1 = piece.linkBak;
        Piece p2 = p1;
        while(p1 != null) {
            p2 = p1;
            p1 = p1.linkBak;
        }
        // go forward and check
        while(p2 != null) {
            if(p2.px < minX)
                minX = p2.px;
            if(p2.px + p2.width > maxX)
                maxX = p2.px + p2.width;
            if(p2.py < minY)
                minY = p2.py;
            if(p2.py + p2.height > maxY)
                maxY = p2.py + p2.height;
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
            if(p2.px < minX)
                minX = p2.px;
            if(p2.px + p2.width > maxX)
                maxX = p2.px + p2.width;
            if(p2.py < minY)
                minY = p2.py;
            if(p2.py + p2.height > maxY)
                maxY = p2.py + p2.height;
            p2 = p2.linkBak;
        }

        float ox = (maxX - minX) / 2;
        float oy = (maxY - minY) / 2;

        p1 = piece.linkBak;
        p2 = p1;
        while(p1 != null) {
            p2 = p1;
            p1 = p1.linkBak;
        }
        // go forward and check
        while(p2 != null) {
            p2.u = ox - (p2.px - minX);
            p2.v = oy - (p2.py - minY);
            //p2.px -= ox;
            //p2.py -= oy;
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
            p2.u = ox - (p2.px - minX);
            p2.v = oy - (p2.py - minY);
            //p2.px -= ox;
            //p2.py -= oy;
            p2 = p2.linkBak;
        }
    }

    // random place
    public void replacePieces(int deskWidth, int deskHeight, boolean easy)
    {
        for (Piece piece : items) {
            piece.px = Utils.random(10, (int)(deskWidth - piece.width - 10), 1);
            piece.py = Utils.random(10, (int)(deskHeight - piece.height - 10), 1);
            if(!easy)
                piece.setAngle(Utils.random(0, 360, 1));
        }
    }

    /**
     * serialize the piece array
     * @param context activity
     */
    public void savseState(Context context)
    {
        StringBuilder sb = new StringBuilder();
        for (Piece piece : items) {
            sb.append(piece.id);
            sb.append(",");
            sb.append(piece.sx);
            sb.append(",");
            sb.append(piece.sy);
            sb.append(",");
            sb.append(piece.px);
            sb.append(",");
            sb.append(piece.py);
            sb.append(",");
            sb.append(piece.getAngle());
            sb.append(",");
            sb.append(piece.tx);
            sb.append(",");
            sb.append(piece.ty);
            sb.append(",");
            sb.append(piece.u);
            sb.append(",");
            sb.append(piece.v);
            sb.append(",");
            sb.append(piece.width);
            sb.append(",");
            sb.append(piece.height);
            sb.append(",");
            if(piece.linkFor != null)
                sb.append(piece.linkFor.id);
            else
                sb.append(-1);
            sb.append(",");
            if(piece.linkBak != null)
                sb.append(piece.linkBak.id);
            else
                sb.append(-1);
            sb.append(",");
            sb.append(piece.ox);
            sb.append(",");
            sb.append(piece.oy);
            sb.append(";");
        }

        SharedPreferences outSettings = context.getSharedPreferences("org.landroo.puzzle_preferences", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = outSettings.edit();
        editor.putString("pieces", sb.toString());
        editor.putString("sides", sides);
        editor.putFloat("xnum", xNum);
        editor.putFloat("ynum", yNum);
        editor.putFloat("width", width);
        editor.putFloat("height", height);
        editor.apply();
    }

    /**
     * load last puzzle state
     * @param context activity
     * @param bitmap picture
     */
    public void loadState(Context context, Bitmap bitmap)
    {
        items.clear();
        pin.clear();

        SharedPreferences inSettings = context.getSharedPreferences("org.landroo.puzzle_preferences", context.MODE_PRIVATE);
        String pieces = inSettings.getString("pieces", "");
        this.sides = inSettings.getString("sides", "");
        xNum = inSettings.getFloat("xnum", 0);
        yNum = inSettings.getFloat("ynum", 0);
        width = inSettings.getFloat("width", 0);
        height = inSettings.getFloat("height", 0);

        BitmapShader fillBMPshader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        fillPaint = new Paint();
        fillPaint.setColor(0xFFFFFFFF);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShader(fillBMPshader);

        if(width > height)
            createPin((int)height, (int)width);
        else
            createPin((int)width, (int)height);

        int sides[][][] = new int[(int)xNum][(int)yNum][4];
        char[] arr = this.sides.toCharArray();
        int c = 0;
        for(int i = 0; i < sides.length; i++)
            for(int j = 0; j < sides[i].length; j++)
                for(int k = 0; k < sides[i][j].length; k++)
                    sides[i][j][k] = Integer.parseInt("" + arr[c++]);

        items.clear();
        String[] pieceArr = pieces.split(";");
        String[] data;
        int x, y;
        for (int i = 0; i < pieceArr.length; i++) {
            //Log.i(TAG, "piece " + pieceArr[i]);
            data = pieceArr[i].split(",");
            x = Integer.parseInt(data[1]);
            y = Integer.parseInt(data[2]);
            Piece piece = new Piece(
                    Float.parseFloat(data[3]),
                    Float.parseFloat(data[4]),
                    Float.parseFloat(data[6]),
                    Float.parseFloat(data[7]),
                    createShape((int)width, (int)height, sides[x][y]),
                    Float.parseFloat(data[10]),
                    Float.parseFloat(data[11]),
                    Float.parseFloat(data[5]),
                    x,y,
                    null,
                    null,
                    Integer.parseInt(data[0]),
                    Float.parseFloat(data[14]),
                    Float.parseFloat(data[15]));
            piece.linkForID = Integer.parseInt(data[12]);
            piece.linkBakID = Integer.parseInt(data[13]);
            items.add(piece);
        }
        //Log.i(TAG, "items " + items.size());
        for (Piece piece : items) {
            if(piece.linkForID != -1) {
                for (Piece linkFor : items){
                    if(linkFor.getId() == piece.linkForID) {
                        piece.linkFor = linkFor;
                        break;
                    }
                }
            }
            if(piece.linkBakID != -1) {
                for (Piece linkBak : items){
                    if(linkBak.getId() == piece.linkBakID) {
                        piece.linkBak = linkBak;
                        break;
                    }
                }
            }
        }
    }


}
