package com.johnsonandschraft.h0x0.vectortest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by h0x0 on 10/29/2015.
 *
 */
public class SVGDrawElement implements VectorView.VectorElement {
    @SuppressWarnings("UnusedDeclaration")
    private static final String DEBUG_TAG = "SVGDrawElement";

    private static final String PARSE_FAIL = "Unable to parse attribute %s: %s";

    public enum SVGDrawType {Circle, Ellipse, Line, Path, Polygon, Polyline, Rectangle, Text, None}

    private static final String TAG_CIRCLE = "circle";
    private static final String TAG_ELLIPSE = "ellipse";
    private static final String TAG_LINE = "line";
    private static final String TAG_PATH = "path";
    private static final String TAG_POLY = "polygon";
    private static final String TAG_P_LINE = "polyline";
    private static final String TAG_RECT = "rect";
    private static final String TAG_TEXT = "text";

    private static final String RX_SEP = "[,\\s]";
//    private static final String RX_NUM = "[\\-\\d\\.]";
    private static final String RX_POINT = "(\\s*[\\-\\d\\.]+)" + RX_SEP + "([\\-\\d\\.]+)\\s*";
    private static final Pattern POINT_PATTERN = Pattern.compile(RX_POINT);
    private static final String RX_PATH = "((?i:[MLHVCSQTAZ]))([\\d\\.,\\-\\s]+)";
    private static final Pattern PATH_PATTERN = Pattern.compile(RX_PATH);
    private static final String RX_TRANS = "\\s*(\\w+)\\(([\\-\\d\\.,\\s]+)\\)";
    private static final Pattern TRANS_PATTERN = Pattern.compile(RX_TRANS);

    public static SVGDrawType SVGTypeFromTag(String tagName) {
        SVGDrawType retVal;
        tagName = tagName.trim();
        if (tagName.equalsIgnoreCase(TAG_CIRCLE)) {
            retVal = SVGDrawType.Circle;
        } else if (tagName.equalsIgnoreCase(TAG_ELLIPSE)) {
            retVal = SVGDrawType.Ellipse;
        } else if (tagName.equalsIgnoreCase(TAG_LINE)) {
            retVal = SVGDrawType.Line;
        } else if (tagName.equalsIgnoreCase(TAG_PATH)) {
            retVal = SVGDrawType.Path;
        } else if (tagName.equalsIgnoreCase(TAG_POLY)) {
            retVal = SVGDrawType.Polygon;
        } else if (tagName.equalsIgnoreCase(TAG_P_LINE)) {
            retVal = SVGDrawType.Polyline;
        } else if (tagName.equalsIgnoreCase(TAG_RECT)) {
            retVal = SVGDrawType.Rectangle;
        } else if (tagName.equalsIgnoreCase(TAG_TEXT)) {
            retVal = SVGDrawType.Text;
        } else {
            retVal = SVGDrawType.None;
        }
        return retVal;
    }

    //easy to parcel
    private SVGDrawType type;
    private String id;
//    private String descriptor;
    private ArrayList<PointF> points;
    private ArrayList<PointF> radii;
    private RectF bounds;
    private Matrix transform;

    //hard to parcel
    private Paint fill, stroke;                                                                     //String style; String strokeStyle
    private Path path;                                                                              //String path_data

    //parcel helpers
    private String style, pathData;
    private float[] mValues;
//    public SVGDrawElement() {
//        _init();
//    }

    //needn't parcel
    VectorView vectorView;

    public SVGDrawElement(SVGDrawType type) {
        _init();
        setType(type);
    }

    private static final String DEF_STYLE = "fill=black; stroke=none;";
    private void _init() {
        type = SVGDrawType.None;

        bounds = new RectF();

        fill = new Paint();
        fill.setAntiAlias(true);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFF000000);

        stroke = new Paint();
        stroke.setAntiAlias(true);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setAlpha(0x00);

        style = DEF_STYLE;

        transform = new Matrix();
    }

    @Override
    public void render(@NonNull Canvas canvas) {
        canvas.save();
        canvas.concat(transform);
        switch (type) {
            case Circle:
                canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, fill);
                canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, stroke);
                break;
            case Ellipse:
                canvas.drawOval(bounds, fill);
                canvas.drawOval(bounds, stroke);
                break;
            case Line:
            case Polyline:
                PointF previous = points.get(0);
                for (PointF current : points) {
                    if (current.equals(previous)) continue;
                    canvas.drawLine(previous.x, previous.y, current.x, current.y, stroke);
                    previous = current;
                }
                break;
            case Path:
            case Polygon:
            case Text:
                canvas.drawPath(path, fill);
                canvas.drawPath(path, stroke);
                if (!transform.isIdentity()) Log.d(DEBUG_TAG, String.format("Transform [%f,%f - %f,%f]", bounds.left, bounds.top, bounds.right, bounds.bottom));
                break;
            case Rectangle:
                if (radii.size() == 0) {
                    canvas.drawRect(bounds, fill);
                    canvas.drawRect(bounds, stroke);
                } else {
                    canvas.drawRoundRect(bounds, radii.get(0).x, radii.get(0).y, fill);
                    canvas.drawRoundRect(bounds, radii.get(0).x, radii.get(0).y, stroke);
                }
                break;
        }

        canvas.restore();
    }

    @Override
    public RectF getBounds() {
        return bounds;
    }

    @Override
    public void inherit(VectorView.VectorElement parent) {
        bounds.set(parent.getBounds());
        if (parent instanceof SVGDrawElement) {
            setStyle(((SVGDrawElement) parent).getStyle());
            setTransform(((SVGDrawElement) parent).transform);
        }
    }

    @Override
    public void setOnUpdateListener(VectorView listener) {
        vectorView = listener;
    }

    private static final String ATTR_CX = "cx";
    private static final String ATTR_CY = "cy";
    private static final String ATTR_R = "r";
    private static final String ATTR_X = "x";
    private static final String ATTR_X1 = "x1";
    private static final String ATTR_X2 = "x2";
    private static final String ATTR_DX = "dx";
    private static final String ATTR_Y = "y";
    private static final String ATTR_Y1 = "y1";
    private static final String ATTR_Y2 = "y2";
    private static final String ATTR_DY = "dy";
    private static final String ATTR_W = "width";
    private static final String ATTR_H = "height";
    private static final String ATTR_RX = "rx";
    private static final String ATTR_RY = "ry";
    private static final String ATTR_POINTS = "points";
    private static final String ATTR_FILL = "fill";
    private static final String ATTR_FILL_OP = "fill-opacity";
    private static final String ATTR_FILL_RULE = "fill-rule";
    private static final String ATTR_OP = "opacity";
    private static final String ATTR_STROKE = "stroke";
    private static final String ATTR_STROKE_OP = "stroke-opacity";
    private static final String ATTR_STR_W = "stroke-width";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_ID = "id";
    private static final String ATTR_D = "d";
    private static final String ATTR_FONT = "font";
    private static final String ATTR_F_SIZE = "font-size";
    private static final String ATTR_F_ANCHOR = "text-anchor";
    private static final String ATTR_TRANSF = "transform";

    public void setAttributeValue(String attribute, String value) {
        boolean set = false;
        switch (type) {
            case Circle:
                set = setCircleAttr(attribute, value);
                break;
            case Ellipse:
                set = setEllipseAttr(attribute, value);
                break;
            case Line:
                set = setLineAttr(attribute, value);
                break;
            case Polygon:
                set = setPolygonAttr(attribute, value);
                break;
            case Polyline:
                set = setPolylineAttr(attribute, value);
                break;
            case Path:
                set = setPathAttr(attribute, value);
                break;
            case Rectangle:
                set = setRectAttr(attribute, value);
                break;
            case Text:
                set = setTextAttr(attribute, value);
                break;
        }

        if (set) transform.mapRect(bounds);

        if (attribute.equalsIgnoreCase(ATTR_ID )) {
            id = value;
            set = true;
        }

        if (!set) setPaintAttr(attribute, value);
        if (vectorView != null) vectorView.onElementUpdate(this);
    }

    private boolean setCircleAttr(String attr, String value) {
        boolean set = false;

        float f = 0f;
        try {
            f = Float.parseFloat(value);
        } catch (Exception ignored) {}

        if (attr.equalsIgnoreCase(ATTR_CX)) {
            bounds.set(
                    f - bounds.width() / 2, bounds.top,
                    f + bounds.width() / 2, bounds.bottom
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_CY)) {
            bounds.set(
                    bounds.left, f - bounds.height() / 2,
                    bounds.right, f + bounds.height() / 2
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_R)) {
            bounds.set(
                    bounds.centerX() - f, bounds.centerY() - f,
                    bounds.centerX() + f, bounds.centerY() + f
            );
            set = true;
        }

        return set;
    }

    private boolean setEllipseAttr(String attr, String value) {
        boolean set = false;

        float f = 0f;
        try {
            f = Float.parseFloat(value);
        } catch (Exception ignored) {
        }
        if (attr.equalsIgnoreCase(ATTR_CX)) {
            bounds.set(
                    f - bounds.width() / 2, bounds.top,
                    f + bounds.width() / 2, bounds.bottom
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_CY)) {
            bounds.set(
                    bounds.left, f - bounds.height() / 2,
                    bounds.right, f + bounds.height() / 2
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_RX)) {
            bounds.set(
                    bounds.centerX() - f, bounds.top,
                    bounds.centerX() + f, bounds.bottom
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_RY)) {
            bounds.set(
                    bounds.left, bounds.centerY() - f,
                    bounds.right, bounds.centerY() + f
            );
            set = true;
        }
        return set;
    }

    private boolean setLineAttr(String attr, String value) {
        boolean set = false;

        float f = 0f;
        try {
            f = Float.parseFloat(value);
        } catch (Exception ignored) {
        }
        if (attr.equalsIgnoreCase(ATTR_X1)) {
            points.get(0).set(f, points.get(0).y);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_X2)) {
            points.get(1).set(f, points.get(1).y);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_Y1)) {
            points.get(0).set(points.get(0).x, f);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_Y2)) {
            points.get(1).set(points.get(1).x, f);
            set = true;
        }

        pathBounds();

        return set;
    }

    private boolean setPolygonAttr(String attr, String value) {
        boolean set = false;
        if (attr.equalsIgnoreCase(ATTR_POINTS)) {
            Matcher pointMatcher = POINT_PATTERN.matcher(value);
            boolean first = true;

            for (int i = 0; pointMatcher.find(i); i = pointMatcher.end()) {
                float pX = Float.parseFloat(pointMatcher.group(1));
                float pY = Float.parseFloat(pointMatcher.group(2));
                points.add(new PointF(pX, pY));
                if (first) { path.moveTo(pX, pY); first= false; }
                else path.lineTo(pX, pY);
            }
            path.close();
            path.computeBounds(bounds, false);
            set = true;
        }
        return set;
    }

    private boolean setPolylineAttr(String attr, String value) {
        boolean set = false;
        if (attr.equalsIgnoreCase(ATTR_POINTS)) {
            points.addAll(parsePoints(value));
            set = true;
            pathBounds();
        }
        return set;
    }

    private boolean setPathAttr(String attr, String value) {
        boolean set = false;
        if (attr.equalsIgnoreCase(ATTR_D)) {
            Matcher matcher = PATH_PATTERN.matcher(value);
            if (!matcher.find()) return true;

            for (int i = 0; matcher.find(i); i = matcher.end()) {
                String op = matcher.group(1);
                String[] vals = matcher.group(2).trim().split("[\\s,]");

                switch (op) {
                    case "M":
                        if (!checkArgs(vals, 2, op)) break;
                        path.moveTo(Float.parseFloat(vals[0]), Float.parseFloat(vals[1]));
                        set = true;
                        break;
                    case "m":
                        if (!checkArgs(vals, 2, op)) break;
                        path.rMoveTo(Float.parseFloat(vals[0]), Float.parseFloat(vals[1]));
                        set = true;
                        break;
                    case "L":
                        if (!checkArgs(vals, 2, op)) break;
                        path.lineTo(Float.parseFloat(vals[0]), Float.parseFloat(vals[1]));
                        set = true;
                        break;
                    case "l":
                        if (!checkArgs(vals, 2, op)) break;
                        path.rLineTo(Float.parseFloat(vals[0]), Float.parseFloat(vals[1]));
                        set = true;
                        break;
                    case "H":
                        set = true;
                        //// TODO: 11/4/2015
                        break;
                    case  "h":
                        set = true;
                        //// TODO: 11/4/2015
                        break;
                    case "V":
                        set = true;
                        //// TODO: 11/4/2015
                        break;
                    case "v":
                        set = true;
                        ///// TODO: 11/4/2015
                        break;
                    case "C":
                        if (!checkArgs(vals, 6, op)) break;
                        path.cubicTo(
                                Float.parseFloat(vals[0]), Float.parseFloat(vals[1]),
                                Float.parseFloat(vals[2]), Float.parseFloat(vals[3]),
                                Float.parseFloat(vals[4]), Float.parseFloat(vals[4])
                        );
                        set = true;
                        break;
                    case "c":
                        if (!checkArgs(vals, 6, op)) break;
                        path.rCubicTo(
                                Float.parseFloat(vals[0]), Float.parseFloat(vals[1]),
                                Float.parseFloat(vals[2]), Float.parseFloat(vals[3]),
                                Float.parseFloat(vals[4]), Float.parseFloat(vals[4])
                        );
                        set = true;
                        break;
                    //// TODO: 11/4/2015 poly-bezier support
                    case "S":
                        //// TODO: 11/4/2015
                        set = true;
                        break;
                    case "s":
                        //// TODO: 11/4/2015
                        set = true;
                        break;
                    case "Q":
                        if (!checkArgs(vals, 4, op)) break;
                        path.quadTo(
                                Float.parseFloat(vals[0]), Float.parseFloat(vals[1]),
                                Float.parseFloat(vals[2]), Float.parseFloat(vals[3])
                        );
                        set = true;
                        break;
                    case "q":
                        if (!checkArgs(vals, 4, op)) break;
                        path.rQuadTo(
                                Float.parseFloat(vals[0]), Float.parseFloat(vals[1]),
                                Float.parseFloat(vals[2]), Float.parseFloat(vals[3])
                        );
                        set = true;
                        break;
                    //// TODO: 11/4/2015 poly-bezier support
                    case "T":
                        //// TODO: 11/4/2015
                        set = true;
                        break;
                    case "t":
                        //// TODO: 11/4/2015
                        set = true;
                        break;
                    case "A":
                        //// TODO: 11/4/2015
                        set = true;
                        break;
                    case "a":
                        //// TODO: 11/4/2015
                        set = true;
                        break;
                    case "Z":
                    case "z":
                        path.close();
                        set = true;
                        break;
                }
            }
        }
        path.computeBounds(bounds, false);
        return set;
    }

    private boolean setRectAttr(String attr, String value) {
        boolean set = false;

        float f = 0f;
        try {
            f = Float.parseFloat(value);
        } catch (Exception ignored) {
        }

        if (attr.equalsIgnoreCase(ATTR_X)) {
            bounds.set(f, bounds.top, f + bounds.width(), bounds.bottom);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_Y)) {
            bounds.set(bounds.left, f, bounds.right, f + bounds.height());
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_W)) {
            bounds.set(bounds.left, bounds.top, bounds.left + f, bounds.bottom);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_H)) {
            bounds.set(bounds.left, bounds.top, bounds.right, bounds.top + f);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_RX)) {
            if (radii.size() > 0) {
                radii.set(0, new PointF(f, radii.get(0).y));
            } else {
                radii.add(new PointF(f, 0));
            }
        } else if (attr.equalsIgnoreCase(ATTR_RY)) {
            if (radii.size() > 0) {
                radii.set(0, new PointF(radii.get(0).x, f));
            } else {
                radii.add(new PointF(0, f));
            }
        }
        return set;
    }

    private boolean setTextAttr(String attr, String value) {
        boolean set = false;
        float f = 0f;
        try { f = Float.parseFloat(value); } catch (NumberFormatException ignored) {}
        if (attr.equalsIgnoreCase(ATTR_X)) {
            points.get(0).set(f, points.get(0).y);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_Y)) {
            points.get(0).set(points.get(0).x, f);
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_DX)) {
            points.get(0).offset(f, 0);
        } else if (attr.equalsIgnoreCase(ATTR_DY)) {
            points.get(0).offset(0, f);
        } else if (attr.equalsIgnoreCase(ATTR_TRANSF)) {
            setTransform(parseTransform(value));
        }
        return set;
    }

    private static final String RX_RGB = "(?i:rgb\\(\\s*\\d\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\))";
    private static final String VAL_NONE = "none";
    private static final String VAL_FILL_NONZERO = "nonzero";
    private static final String VAL_FILL_EVENODD = "evenodd";
    private static final String VAL_ANCHOR_START = "start";
    private static final String VAL_ANCHOR_MIDDLE = "middle";
    private static final String VAL_ANCHOR_END = "end";
    private boolean setPaintAttr(String attr, String value) {
        boolean set = false;
        int color = -1;
        try { color = Color.parseColor(value); } catch (Exception ignored) { }
        if (color < 0 && value.matches(RX_RGB)) {
            final String[] rgb = value
                    .substring(value.indexOf("(") + 1, value.lastIndexOf(")"))
                    .split(RX_SEP);
            try {
                color = Color.rgb(
                        Integer.parseInt(rgb[0].trim()),
                        Integer.parseInt(rgb[1].trim()),
                        Integer.parseInt(rgb[2].trim())
                );
            } catch (NumberFormatException ignored) {
                Log.e(DEBUG_TAG, String.format(PARSE_FAIL, attr, value));
            }
        }

        if (attr.equalsIgnoreCase(ATTR_FILL)) {
            if (value.equalsIgnoreCase(VAL_NONE)) {
                fill.setAlpha(0);
                return true;
            }
            fill.setARGB(
                    fill.getAlpha() == 0 ? 0xFF : fill.getAlpha(),
                    Color.red(color), Color.green(color), Color.blue(color)
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_FILL_OP)) {
            try {
                fill.setAlpha((int) (Float.parseFloat(value) * 0xFF));
                set = true;
            } catch (Exception ignored) {
                Log.e(DEBUG_TAG, String.format(PARSE_FAIL, attr, value));
            }
        } else if (attr.equalsIgnoreCase(ATTR_FILL_RULE)) {
            if (value.equalsIgnoreCase(VAL_FILL_EVENODD)) {
                path.setFillType(Path.FillType.EVEN_ODD);
                 set = true;
            } else if (value.equalsIgnoreCase(VAL_FILL_NONZERO)) {
                path.setFillType(Path.FillType.WINDING);
                set = true;
            }
        } else if (attr.equalsIgnoreCase(ATTR_FONT)) {
            //// TODO: 11/4/2015  This is going to take a while
        } else if (attr.equalsIgnoreCase(ATTR_F_ANCHOR)) {
            if (value.equalsIgnoreCase(VAL_ANCHOR_START)) {
                //// TODO: 11/4/2015 support rtl text
                fill.setTextAlign(Paint.Align.LEFT);
                set = true;
            } else if (value.equalsIgnoreCase(VAL_ANCHOR_MIDDLE)) {
                fill.setTextAlign(Paint.Align.CENTER);
                set = true;
            } else if (value.equalsIgnoreCase(VAL_ANCHOR_END)) {
                fill.setTextAlign(Paint.Align.RIGHT);
                set = true;
            }
        } else if (attr.equalsIgnoreCase(ATTR_F_SIZE)) {
            try {
                fill.setTextSize(Float.parseFloat(value));
                set = true;
            } catch (NumberFormatException e) {
                Log.w(DEBUG_TAG, String.format(PARSE_FAIL, attr, value));
            }
        } else if (attr.equalsIgnoreCase(ATTR_OP)) {
            try {
                final int alpha = (int) (Float.parseFloat(value) * 0xFF);
                fill.setAlpha(alpha);
                stroke.setAlpha(alpha);
                set = true;
            } catch (Exception ignored) {
                Log.e(DEBUG_TAG, String.format(PARSE_FAIL, attr, value));
            }
        } else if (attr.equalsIgnoreCase(ATTR_STROKE)) {
            if (value.equalsIgnoreCase(VAL_NONE)) {
                stroke.setAlpha(0);
            }
            stroke.setARGB(
                    stroke.getAlpha() == 0 ? 0xFF : stroke.getAlpha(),
                    Color.red(color), Color.green(color), Color.blue(color)
            );
            set = true;
        } else if (attr.equalsIgnoreCase(ATTR_STROKE_OP)) {
            try {
                stroke.setAlpha((int) (Float.parseFloat(value) * 0xFF));
                set = true;
            } catch (Exception ignored) {
                Log.e(DEBUG_TAG, String.format(PARSE_FAIL, attr, value));
            }
        } else if (attr.equalsIgnoreCase(ATTR_STR_W)) {
            try {
                float w = Float.parseFloat(value);
                stroke.setStrokeWidth(w);
                set = true;
            } catch (Exception ignored) { }
        } else if (attr.equalsIgnoreCase(ATTR_STYLE)) {
            setStyle(value);
        }
        if (set) _updateStyleString(attr, value);
        return set;
    }

    private static final String RX_STYLE = "\\s*%s\\s*\\:\\s\\w\\;";
    private static final String FRM_STYLE_ATTR = " %s:%s;";
    private void _updateStyleString(String attr, String value) {
        String m = String.format(RX_STYLE, attr);
        if (style.matches(m)) {
            style.replace(m, String.format(FRM_STYLE_ATTR, attr, value));
        } else {
            style += String.format(FRM_STYLE_ATTR, attr, value);
        }
    }

    public void setStyle(String style) {
        for (String pair : style.split(";")) {
            int semi = pair.indexOf(":");
            if (semi < 0 || semi == pair.length() - 1) continue;
            String attr = pair.substring(0, semi);
            String val = pair.substring(semi + 1);
//            Log.d(DEBUG_TAG, String.format("Attr: %s - Val: %s", attr, val));
            setPaintAttr(attr, val);
        }
    }

    public void setType(SVGDrawType type) {
        this.type = type;
        switch (type) {
            case Line:
                points = new ArrayList<>(2);
                points.add(new PointF());
                points.add(new PointF());
                break;
            case Path:
            case Polygon:
            case Polyline:
                points = new ArrayList<>();
                radii = new ArrayList<>();
                path = new Path();
            case Rectangle:
                radii = new ArrayList<>();
                break;
            case Text:
                points = new ArrayList<>();
                points.add(new PointF());
                path = new Path();
        }
    }

    private void pathBounds() {
        float left = -1, top = -1, right = -1, bottom = -1;
        boolean first = true;
        for (PointF p : points) {
            if (first) {
                left = right = p.x;
                top = bottom = p.y;
                first = false;
            } else {
                left = left < p.x ? left : p.x;
                top = top < p.y ? top : p.y;
                right = right > p.x ? right : p.x;
                bottom = bottom > p.y ? bottom : p.y;
            }
            bounds.set(left, top, right, bottom);
            vectorView.onElementUpdate(this);
        }
    }

    private List<PointF> parsePoints(String points) {
        ArrayList<PointF> pointFs = new ArrayList<>();
        Matcher matcher = POINT_PATTERN.matcher(points);
        if (!matcher.find()) return pointFs;

        for (int i = 0; matcher.find(i); i = matcher.end()) {
            float x = Float.parseFloat(matcher.group(1));
            float y = Float.parseFloat(matcher.group(2));
            pointFs.add(new PointF(x, y));
        }

        return pointFs;
    }

    private static final String PATH_FAIL = "Not enough arguments for path instruction %s: %d";
    private static boolean checkArgs(String[] values, int length, String tag) {
        if (values.length < length) {
            if (tag != null) Log.w(DEBUG_TAG, String.format(PATH_FAIL, tag, length));
            return false;
        }
        return true;
    }

//    public void setDescriptor(String descriptor) {
//        this.descriptor = descriptor;
//    }

    public String getStyle() {

        return null;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return pathData;
    }

    public void setText(String text) {
        pathData = text;

        if (type == SVGDrawType.Path) setPathAttr(ATTR_D, text);
        else if (type == SVGDrawType.Text) {
            fill.getTextPath(
                    text,
                    0, text.length(),
                    points.get(0).x, points.get(0).y,
                    path
            );
            path.computeBounds(bounds, false);
            transform.mapRect(bounds);
            if (vectorView != null) vectorView.onElementUpdate(this);
        }
    }

    public void setTransform(Matrix transform) {
        this.transform.set(transform);
    }

    public void concat(Matrix other) {
        this.transform.postConcat(other);
    }

    private static final String TRANS_MATRIX = "matrix";
    private static final String TRANS_TRANSLATE = "translate";
    private static final String TRANS_SCALE = "scale";
    private static final String TRANS_ROTATE = "rotate";
    private static final String TRANS_SKEW_X = "skewX";
    private static final String TRANS_SKEW_Y = "skewY";
    public static Matrix parseTransform(String transString, Matrix previous) {
        Matcher matcher = TRANS_PATTERN.matcher(transString);
        if (!matcher.find()) return previous;

        for (int i = 0; matcher.find(i); i = matcher.end()) {
            String op = matcher.group(1);
            String[] values = matcher.group(2).split(RX_SEP);
            float x = 0, y = 0, a = 0, b = 0, c = 0, d = 0;
            switch (op) {
                case TRANS_MATRIX:
                    if (checkArgs(values, 6, op)) {
                        float[] tValues = new float[9];
                        tValues[0] = a;
                        tValues[1] = c;
                        tValues[2] = x;
                        tValues[3] = b;
                        tValues[3] = d;
                        tValues[3] = y;
                        tValues[6] = 0.0f;
                        tValues[7] = 0.0f;
                        tValues[8] = 1.0f;
                        previous.setValues(tValues);
                    }
                    break;
                case TRANS_TRANSLATE:
                    if (checkArgs(values, 2, null)) y = Float.parseFloat(values[1]);
                    if (checkArgs(values, 1, op)) x = Float.parseFloat(values[0]);
                    previous.postTranslate(x,y);
                    break;
                case TRANS_SCALE:
                    if (checkArgs(values, 1, op)) a = Float.parseFloat(values[0]);
                    else continue;
                    if (checkArgs(values, 2, null)) d = Float.parseFloat(values[1]);
                    else d = a;
                    previous.postTranslate(a, d);
                    break;
                case TRANS_ROTATE:
                    if (checkArgs(values, 3, null)) {
                        a = Float.parseFloat(values[0]);
                        x = Float.parseFloat(values[1]);
                        y = Float.parseFloat(values[2]);
                        previous.postRotate(a, x, y);
                    } else if (checkArgs(values, 1, op)) {
                        a = Float.parseFloat(values[0]);
                        previous.postRotate(a);
                    }
                    break;
                case TRANS_SKEW_X:
                    if (checkArgs(values, 1, op)) {
                        a = Float.parseFloat(values[0]);
                        previous.postSkew(a, 0);
                    }
                    break;
                case TRANS_SKEW_Y:
                    if (checkArgs(values, 1, op)) {
                        a = Float.parseFloat(values[0]);
                        previous.postSkew(0, a);
                    }
                    break;
            }
        }
        return previous;
    }


    public static Matrix parseTransform(String transString) {
        return parseTransform(transString, new Matrix());
    }

    @Override
    public int describeContents() {
        return 0;
    }

//    private static final String SAVE_DESC = "descriptor";
    private static final String SAVE_TYPE = "type";
    private static final String SAVE_ID = "id";
    private static final String SAVE_POINTS = "points";
    private static final String SAVE_RADII = "radii";
    private static final String SAVE_TRANS = "transform";
    private static final String SAVE_STYLE = "style";
    private static final String SAVE_P_DATA = "path-data";

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle martin = new Bundle();

        martin.putString(SAVE_TYPE, type.toString());
        martin.putString(SAVE_ID, id);
        martin.putParcelableArray(SAVE_POINTS, (PointF[]) points.toArray());
        martin.putParcelableArray(SAVE_RADII, (PointF[]) radii.toArray());
        transform.getValues(mValues);
        martin.putFloatArray(SAVE_TRANS, mValues);
        //// TODO: 11/5/2015 package style
        martin.putString(SAVE_STYLE, style);

        martin.putString(SAVE_P_DATA, pathData);

        dest.writeBundle(martin);
    }

    public SVGDrawElement(Parcel source) {
        _init();
        Bundle martin = source.readBundle();

        String temp = martin.getString(SAVE_TYPE);
        type = temp == null ? SVGDrawType.None : Enum.valueOf(SVGDrawType.class, temp);

        id = martin.getString(SAVE_ID);

        points = new ArrayList<>();
        PointF[] tempArray = (PointF[]) martin.getParcelableArray(SAVE_POINTS);
        if (tempArray != null) points.addAll(Arrays.asList(tempArray));

        radii = new ArrayList<>();
        tempArray = (PointF[]) martin.getParcelableArray(SAVE_RADII);
        if (tempArray != null) radii.addAll(Arrays.asList(tempArray));

        mValues = martin.getFloatArray(SAVE_TRANS);
        transform.setValues(mValues);

        style = martin.getString(SAVE_STYLE);
        if (style != null) setStyle(style);

        pathData = martin.getString(SAVE_P_DATA);
        if (pathData != null) setText(pathData);
    }

    public static final Creator<SVGDrawElement> CREATOR = new Creator<SVGDrawElement>() {
        @Override
        public SVGDrawElement createFromParcel(Parcel source) {
            return new SVGDrawElement(source);
        }

        @Override
        public SVGDrawElement[] newArray(int size) {
            return new SVGDrawElement[size];
        }
    };
}
