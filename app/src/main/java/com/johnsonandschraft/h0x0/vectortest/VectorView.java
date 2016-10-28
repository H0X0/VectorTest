package com.johnsonandschraft.h0x0.vectortest;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.OverScroller;

import java.util.ArrayList;


/**
 * Created by h0x0 on 10/20/2015.
 *
 */
public class VectorView extends View {

    private static final float MAX_SCALE = 150f;
    private static final float MIN_SCALE = 0.1f;

    public interface VectorElement extends Parcelable{
        void render(@NonNull Canvas canvas);
        RectF getBounds();
        void setOnUpdateListener(VectorView listener);
        void inherit(VectorElement parent);
//        void setAttribute(String attribute, String Value);
    }

    private Paint contentPaint;

//    private List<PathHolder> paths;
    private ArrayList<VectorElement> vectorElements;
//    private Path dst = new Path();
    private RectF bounds, content, dstRect;

    private Matrix m;
    private float[] mValues = new float[9];

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private OverRunner overRunner = new OverRunner();

    private class OverRunner implements Runnable {
        @Override
        public void run() {
            final PointF force = getResistance();
            if (force.x == 0 && force.y == 0) return;

            m.postTranslate(force.x, force.y);
            VectorView.this.invalidate();
            compatPostOnAnimation(this);
        }
    }

    private class FlingRunner implements Runnable {
        private OverScroller scroller;
        int curX, curY;

        public FlingRunner(int velocityX, int velocityY) {
//            scroller = new OverScroller(getContext(), new OvershootInterpolator());
//            scroller = new OverScroller(getContext(), new AccelerateDecelerateInterpolator());
            scroller = new OverScroller(getContext(), new BounceInterpolator());
            final int startX = (int) getX();
            final int startY = (int) getY();

            m.mapRect(dstRect, content);
            final int minX = (int) (bounds.left - dstRect.width() / 2);
            final int maxX = (int) (bounds.right + dstRect.width() / 2);
            final int minY = (int) (bounds.top - dstRect.height() / 2);
            final int maxY = (int) (bounds.bottom + dstRect.height() / 2);

            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);

            curX = startX;
            curY = startY;
        }

        @Override
        public void run() {
//            final PointF force = getResistance();

            if (scroller.isFinished()) {
                scroller = null;
                compatPostOnAnimation(overRunner);
            } else if (scroller.computeScrollOffset()) {
                m.postTranslate(scroller.getCurrX() - getX(), scroller.getCurrY() - getY());
//                fixTranslate();
                VectorView.this.invalidate();
                compatPostOnAnimation(this);
            }
        }

        public void cancelFling() {
            if (scroller != null) scroller.forceFinished(true);
        }
    }

    private ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener
            = new ScaleGestureDetector.OnScaleGestureListener() {
        private float curScale;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            final float dScale = detector.getScaleFactor() - curScale;
            final float s = (dScale > 0 && getScaleX() >= MAX_SCALE) ? 1f :
                    (dScale < 0 && getScaleX() <= MIN_SCALE) ? 1f :
                    1 + dScale;
            m.postScale(s, s, detector.getFocusX(), detector.getFocusY());
            curScale += dScale;
//            fixTranslate();
            invalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            curScale = 1;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            compatPostOnAnimation(overRunner);
        }
    };

    private GestureDetector.OnGestureListener onGestureListener
            = new GestureDetector.OnGestureListener() {
        FlingRunner flingRunner;

        @Override
        public boolean onDown(MotionEvent e) {
            if (flingRunner != null) flingRunner.cancelFling();
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            compatPostOnAnimation(overRunner);
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            m.postTranslate(-distanceX, -distanceY);
//            fixTranslate();
            invalidate();
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            reset();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            flingRunner = new FlingRunner((int) velocityX, (int) velocityY);
            compatPostOnAnimation(flingRunner);
            return false;
        }

    };

    public VectorView(Context context) {
        super(context);
        _init(context, null, -1);
    }

    public VectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _init(context, attrs, -1);
    }

    public VectorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init(context, attrs, defStyleAttr);
    }
    @SuppressWarnings("UnusedDeclaration")
    private void _init(Context context, AttributeSet attrs, int defStyleAttr) {
        m = new Matrix();
        vectorElements = new ArrayList<>();

        content = new RectF();
        contentPaint = new Paint();
        contentPaint.setColor(Color.WHITE);
        dstRect = new RectF();

        gestureDetector = new GestureDetector(context, onGestureListener);
        scaleGestureDetector = new ScaleGestureDetector(context, onScaleGestureListener);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private static final String SAVE_INSTANCE = "save_instance";
    private static final String SAVE_M_VALUES = "m_values";

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable(SAVE_INSTANCE, super.onSaveInstanceState());
        state.putFloatArray(SAVE_M_VALUES, mValues);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle martin = new Bundle();
            mValues = martin.getFloatArray(SAVE_M_VALUES);
            m.setValues(mValues);
//            if (m.isAffine()) m.reset();
            Parcelable superState = martin.getParcelable(SAVE_INSTANCE);
            super.onRestoreInstanceState(superState);
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds = new RectF(0f, 0f, (float) w, (float) h);
        reset();
    }

    public void reset() {
        m.reset();

        if (content != null && content.width() > 0) {
            //debug
            final float sX = bounds.width() / content.width();
            final float sY = bounds.height() / content.height();
            final float s = sX < sY ? sX : sY;
//            m.setScale(s, s);
            //debug
            final float tX = bounds.centerX() - content.centerX() * s;
            final float tY = bounds.centerY() - content.centerY() * s;
//            m.postTranslate(tX, tY);
            m.setTranslate(tX, tY);
            m.postScale(s, s, tX, tY);
        }

        invalidate();
    }

    private PointF getResistance() {
        float forceX = 0, forceY = 0;
        m.mapRect(dstRect, content);

        if (dstRect.width() < bounds.width()) {
            if (dstRect.left < bounds.left) {
                forceX = pushitrealgood(bounds.left - dstRect.left);
            } else if (dstRect.right > bounds.right) {
                forceX = pushitrealgood(bounds.right - dstRect.right);
            }
        } else {
            if (dstRect.left > bounds.left) {
                forceX = pushitrealgood(bounds.left - dstRect.left);
            } else if (dstRect.right < bounds.right) {
                forceX = pushitrealgood(bounds.right - dstRect.right);
            }
        }

        if (dstRect.height() < bounds.height()) {
            if (dstRect.top < bounds.top) {
                forceY = pushitrealgood(bounds.top - dstRect.top);
            } else if (dstRect.bottom > bounds.bottom) {
                forceY = pushitrealgood(bounds.bottom - dstRect.bottom);
            }
        } else {
            if (dstRect.top > bounds.top) {
                forceY = pushitrealgood(bounds.top - dstRect.top);
            } else if (dstRect.bottom < bounds.bottom) {
                forceY = pushitrealgood(bounds.bottom - dstRect.bottom);
            }
        }
        return new PointF(forceX, forceY);
    }

    private float pushitrealgood(float distance) {
        return (float) Math.sqrt(distance * Math.signum(distance)) * Math.signum(distance);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.LTGRAY);

        canvas.save();
        canvas.concat(m);
        canvas.drawRect(content, contentPaint);
        for (VectorElement element : vectorElements) {
            element.render(canvas);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean tf = gestureDetector.onTouchEvent(event);
        tf = tf || scaleGestureDetector.onTouchEvent(event);
        return tf || super.onTouchEvent(event);
    }

    @Override
    public float getX() {
        m.getValues(mValues);
        return mValues[Matrix.MTRANS_X] + m.mapRadius(content.centerX());
    }

    @Override
    public float getY() {
        m.getValues(mValues);
        return mValues[Matrix.MTRANS_Y] + m.mapRadius(content.centerY());
    }

    @Override
    public float getScaleX() {
        m.getValues(mValues);
        return mValues[Matrix.MSCALE_X];
    }

    @SuppressWarnings("UnusedDeclaration")
    public void addElement(VectorElement element, int index) {
        vectorElements.add(index, element);
        _addElement(element);
    }

    public void addElement(VectorElement element) {
        vectorElements.add(element);
        _addElement(element);
    }

    private void _addElement(VectorElement element) {
        content.union(element.getBounds());
        element.setOnUpdateListener(this);
        this.invalidate();
    }

    public void onElementUpdate(VectorElement element) {
        content.union(element.getBounds());
        invalidate();
    }

    public VectorElement getLastElement() {
        if (vectorElements.isEmpty()) return null;
        return vectorElements.get(vectorElements.size() - 1);
    }

//    private class PathHolder implements Serializable {
//        public Path path;
//        public Paint paint;
//    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void compatPostOnAnimation(Runnable runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postOnAnimation(runnable);
        } else {
            postDelayed(runnable, 1000/60);
        }
    }

//    public enum SVGElementType {tobe, completed}
//    public class SVGElement implements Parcelable {
//        private SVGElement type;
//        private Paint paint;
//
//        public SVGElement(){
//
//        }
//
//        public SVGElement(String descriptor){
//            _fromDescriptor(descriptor);
//        }
//
//        private void _fromDescriptor(String descriptor) {
//
//        }
//
//        public void render(@NonNull Canvas canvas, Matrix m) {
//
//        }
//
//        @Override
//        public int describeContents() {
//            return 0;
//        }
//
//        @Override
//        public void writeToParcel(Parcel dest, int flags) {
//
//        }
//
//        public final Creator<SVGElement> CREATOR = new Creator<SVGElement>() {
//            @Override
//            public SVGElement createFromParcel(Parcel source) {
//                return null;
//            }
//
//            @Override
//            public SVGElement[] newArray(int size) {
//                return new SVGElement[0];
//            }
//        };
//    }
}
