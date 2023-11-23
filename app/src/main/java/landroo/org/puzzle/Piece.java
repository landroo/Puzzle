package landroo.org.puzzle;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rkovacs on 2015.09.15..
 */
public class Piece {
    private static final String TAG = "Piece";
    private static final int GAP = 10;

    public float px;// X position
    public float py;// Y position

    public float tx;// image translation X
    public float ty;// image translation Y

    public float ox;// original X position
    public float oy;// original Y position

    public int sx;// start X
    public int sy;// start Y

    public int id;

    public boolean selected = false;

    public List<PointF> points = new ArrayList<>();
    public Path path = new Path();

    public boolean rotating = false;
    private float angle = 0;

    public float width;
    public float height;

    public float u, v;

    public Piece linkFor = null;
    public Piece linkBak = null;

    public int linkForID = -1;
    public int linkBakID = -1;

    private float[] xarr;
    private float[] yarr;

    /**
     * constructor
     * @param px    float position x
     * @param py    float position y
     * @param tx    float translate x
     * @param ty    float translate y
     * @param w     float width
     * @param h     float height
     * @param sx    int start x
     * @param sy    int start y
     * @param id    int id
     */
    public Piece(float px, float py, float tx, float ty, float w, float h, int sx, int sy, int id) {
        this.px = px;
        this.py = py;

        this.ox = px;
        this.oy = py;

        this.tx = tx;
        this.ty = ty;

        this.width = w;
        this.height = h;

        this.u = w / 2;
        this.v = h / 2;

        this.sx = sx;
        this.sy = sy;

        this.id = id;

        points.add(new PointF(0, 0));
        points.add(new PointF(w, 0));
        points.add(new PointF(w, h));
        points.add(new PointF(0, h));

        xarr = new float[points.size()];
        yarr = new float[points.size()];

        path.moveTo(0, 0);
        path.lineTo(w, 0);
        path.lineTo(w, h);
        path.lineTo(0, h);
    }

    /**
     * copy constructor
     * @param px    float position x
     * @param py    float position y
     * @param tx    float translate x
     * @param ty    float translate y
     * @param shape PointF list shape
     * @param w     float width
     * @param h     float height
     * @param ang   float rotation angle
     * @param sx    int start x
     * @param sy    int start y
     * @param linked    Piece linked piece
     * @param linkme    Piece link me pieces
     * @param id    int id
     * @param ox    float original x
     * @param oy    float original y
     */
    public Piece(float px, float py, float tx, float ty, List<PointF> shape, float w, float h, float ang, int sx, int sy, Piece linked, Piece linkme, int id, float ox, float oy) {
        this.px = px;
        this.py = py;

        this.ox = ox;
        this.oy = oy;

        this.tx = tx;
        this.ty = ty;

        this.angle = ang;

        this.width = w;
        this.height = h;

        this.u = w / 2;
        this.v = h / 2;

        this.sx = sx;
        this.sy = sy;

        this.linkFor = linked;
        this.linkBak = linkme;

        this.id = id;

        points.addAll(shape);

        xarr = new float[points.size()];
        yarr = new float[points.size()];

        boolean first = true;
        for (PointF pnt : shape) {
            if (first) {
                path.moveTo(pnt.x, pnt.y);
                first = false;
            }
            path.lineTo(pnt.x, pnt.y);
        }
    }

    /**
     * tap point is inside the piece
     * @param posx  float position x
     * @param posy  float position y
     * @param zx    float zoom x
     * @param zy    float zoom y
     * @return      boolean is inside
     */
    public boolean isInside(float posx, float posy, float zx, float zy) {
        int i = 0;
        PointF pf;
        for (PointF pnt : points) {
            pf = Utils.rotatePnt((px + u) * zx, (py + v) * zy, (pnt.x + px) * zx, (pnt.y + py) * zy, angle * Utils.DEGTORAD);
            xarr[i] = pf.x;
            yarr[i] = pf.y;
            i++;
        }
        boolean bIn = Utils.ponitInPoly(points.size(), xarr, yarr, posx, posy);
        //Log.i(TAG, "" + posx + " " + px + " " + xarr[0]);
        return bIn;
    }

    /**
     * set the rotation angle of the piece
     * @param ang   float angle in degree
     */
    public void setAngle(float ang) {
        float angle = ang;
        while (angle > 360)
            angle -= 360;
        while (angle < 0)
            angle += 360;
        this.angle = angle;
    }

    /**
     * get angle
     * @return return the angle of the piece
     */
    public float getAngle() {
        return this.angle;
    }

    /**
     * pieces center close each other
     * @param piece Piece next piece
     * @return  boolean close enough
     */
    public boolean checkClose(Piece piece) {
        PointF p1 = Utils.rotatePnt(piece.px + piece.u, piece.py + piece.v, piece.px + width / 2, piece.py + height / 2, piece.getAngle() * Utils.DEGTORAD);
        PointF p2 = Utils.rotatePnt(px + u, py + v, px + width / 2, py + height / 2, getAngle() * Utils.DEGTORAD);
        double dist = Utils.getDist(p1.x, p1.y, p2.x, p2.y);
        //Log.i(TAG, "Dist: " + dist + " " + width);
        if ((dist < width + GAP && dist > width - GAP) || (dist < height + GAP && dist > height - GAP)) {
            return true;
        }

        return false;
    }

    /**
     * check the next piece is on right side
     * @param piece Piece next piece
     * @param side  int piece side
     * @return      boolean right faces
     */
    public boolean checkFace(Piece piece, int side) {
        int pos = 0;
        PointF p1 = Utils.rotatePnt(piece.px + piece.u, piece.py + piece.v, piece.px + width / 2, piece.py + height / 2, piece.getAngle() * Utils.DEGTORAD);
        PointF p2 = Utils.rotatePnt(px + u, py + v, px + width / 2, py + height / 2, getAngle() * Utils.DEGTORAD);
        double dir = Utils.getAng(p1.x, p1.y, p2.x, p2.y) - getAngle();
        while (dir > 360)
            dir -= 360;
        while (dir < 0)
            dir += 360;
        //Log.i(TAG, "Side: " + side + " " + dir + " " + getAngle() + " " + piece.getAngle());
        if (side == 1 && dir > 85 && dir < 95) {
            //Log.i(TAG, "over");
            pos |= 1;
        }

        if (side == 2 && dir > 175 && dir < 185) {
            //Log.i(TAG, "right");
            pos |= 2;
        }

        if (side == 4 && dir > 265 && dir < 275) {
            //Log.i(TAG, "under");
            pos |= 4;
        }

        if (side == 8 && ((dir > 355 && dir < 360) || (dir > 0 && dir < 5))) {
            //Log.i(TAG, "left");
            pos |= 8;
        }

        return pos != 0;
    }

    // pieces next to each other
    public int checkNext(int x, int y) {
        int side = 0;
        // over
        if (this.sx == x && this.sy - 1 == y)
            side |= 1;
        // right
        if (this.sx + 1 == x && this.sy == y)
            side |= 2;
        // under
        if (this.sx == x && this.sy + 1 == y)
            side |= 4;
        // left
        if (this.sx - 1 == x && this.sy == y)
            side |= 8;
        //Log.i(TAG, "next " + side);
        return side;
    }

    // the angle is same
    public boolean checkAngle(double ang) {
        boolean bOK = false;
        double angle = getAngle() - ang;
        while (angle > 360)
            angle -= 360;
        while (angle < 0)
            angle += 360;
        if ((angle >= 355 && angle <= 360) || (angle >= 0 && angle <= 5))
            bOK = true;
        //Log.i(TAG, "angle " + angle + " " + ang + " " + getAngle());
        return bOK;
    }

    public int getId() {
        return id;
    }

    // fix neighbors positions
    public int fixpos(int side, Piece piece) {
        float posx = piece.px;
        float posy = piece.py;
        float ang = getAngle();
        float dx = 0;
        float dy = 0;
        // over
        if(side == 1) {
            dx = this.px - posx;
            dy = this.py - posy - height;
        }
        // right
        if(side == 2) {
            dx = this.px - posx + width;
            dy = this.py - posy;
        }
        // under
        if(side == 4) {
            dx = this.px - posx;
            dy = this.py - posy + height;
        }
        // left
        if(side == 8) {
            dx = this.px - posx - width;
            dy = this.py - posy;
        }

        int cnt = 0;
        if(side != 0) {
            piece.setAngle(ang);
            piece.px += dx;
            piece.py += dy;
            //Log.i(TAG, "my id " + getId() + " linkFor: " + (piece.linkFor != null) + " linkBak: " + (piece.linkBak != null));
            int cnt1 = 0;
            if(piece.linkFor != null)
                cnt1 = fixLinkFor(piece.linkFor, dx, dy, ang, cnt1);
            int cnt2 = 0;
            if(piece.linkBak != null)
                cnt2 = fixLinkBak(piece.linkBak, dx, dy, ang, cnt2);
            cnt = cnt1 + cnt2;
        }
        //Log.i(TAG, "Fix: " + side + " " + dx + " " + dy);
        return cnt;
    }

    private int fixLinkFor(Piece piece, float dx, float dy, float ang, int cnt)
    {
        //Log.i(TAG, "fix linkFor " + piece.id);
        piece.px += dx;
        piece.py += dy;
        piece.setAngle(ang);
        if(piece.linkFor != null)
            cnt = fixLinkFor(piece.linkFor, dx, dy, ang, cnt);
        return ++cnt;
    }

    private int fixLinkBak(Piece piece, float dx, float dy, float ang, int cnt)
    {
        //Log.i(TAG, "fix linkBak " + piece.id);
        piece.px += dx;
        piece.py += dy;
        piece.setAngle(ang);
        if(piece.linkBak != null)
            cnt = fixLinkBak(piece.linkBak, dx, dy, ang, cnt);
        return ++cnt;
    }

}
