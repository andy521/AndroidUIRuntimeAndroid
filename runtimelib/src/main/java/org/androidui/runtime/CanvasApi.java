package org.androidui.runtime;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.Log;

import java.util.ArrayDeque;

/**
 * Created by linfaxin on 15/12/14.
 *
 */
public class CanvasApi {
    private static String TAG = "CanvasApi";
    public static boolean DEBUG = false;
    private static Paint clearPaint = new Paint();
    static {
        clearPaint.setColor(Color.WHITE);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }
    private static Pools.Pool<CanvasPaint> sPaintPool = new Pools.SynchronizedPool<CanvasPaint>(30);
    private static CanvasPaint obtainPaint(){
        CanvasPaint paint = sPaintPool.acquire();
        if(paint==null) return new CanvasPaint();
        return paint;
    }
    private static void releasePaint(CanvasPaint paint){
        sPaintPool.release(paint);
    }

    Canvas canvas;
    private Rect mRectTemp = new Rect();
    private RectF mRectFTemp = new RectF();
    private float[] mRadiiTemp = new float[8];
    private Path mPathTemp = new Path();
    protected Matrix mTempMatrix = new Matrix();
    protected final float[] mTempValue = new float[9];
    private ArrayDeque<CanvasPaint> savedPaints = new ArrayDeque<CanvasPaint>();
    protected CanvasPaint mPaint;

    public CanvasApi(@Nullable Canvas canvas) {
        this.canvas = canvas;
        mPaint = new CanvasPaint();
        mPaint.setAntiAlias(true);

        mTempMatrix.reset();
        mTempMatrix.getValues(mTempValue);
    }

    public void reset(Canvas canvas){
        if(this.canvas!=canvas){
            if(canvas instanceof BitmapCanvas){
                if(((BitmapCanvas) canvas).bitmap!=null) ((BitmapCanvas) canvas).bitmap.recycle();
            }
        }
        this.canvas = canvas;

        for(CanvasPaint paint : savedPaints){
            releasePaint(paint);
        }
        savedPaints.clear();

        mPaint.reset();
    }

    public void recycle(){
        if(DEBUG) Log.d(TAG, "recycle");
        Canvas canvas = getCanvas();
        if(canvas instanceof BitmapCanvas){
            if(((BitmapCanvas) canvas).bitmap!=null) ((BitmapCanvas) canvas).bitmap.recycle();
        }
        this.canvas = null;

        releasePaint(mPaint);
        for(CanvasPaint paint : savedPaints){
            releasePaint(paint);
        }
        savedPaints.clear();
        mPaint = null;
    }

    @Nullable
    public Canvas getCanvas(){
        return canvas;
    }

    private void applyPaintStyle(int fillStyle){
        Paint.Style style;
        switch (fillStyle){
            case 0:style = Paint.Style.FILL;break;
            case 1:style = Paint.Style.STROKE;break;
            case 2:style = Paint.Style.FILL_AND_STROKE;break;
            default:style = Paint.Style.FILL;break;
        }

        if(style == Paint.Style.STROKE){
            mPaint.setColor(mPaint.strokeColor);
        }else{
            mPaint.setColor(mPaint.fillColor);
        }

        mPaint.setStyle(style);
    }


    public void translate(float tx, float ty){
        if(DEBUG) Log.d(TAG, "translate, tx:"+tx+", ty:"+ty);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            canvas.translate(tx, ty);
        }
    }

    public void scale(float sx, float sy){
        if(DEBUG) Log.d(TAG, "scale, sx:"+sx+", sy:"+sy);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            canvas.scale(sx, sy);
        }
    }

    public void rotate(float degrees){
        if(DEBUG) Log.d(TAG, "rotate, degrees:"+degrees);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            canvas.rotate(degrees);
        }
    }

    public void concat(float MSCALE_X, float MSKEW_X, float MTRANS_X, float MSKEW_Y, float MSCALE_Y, float MTRANS_Y){
        if(DEBUG) Log.d(TAG, "concat, MSCALE_X:" + MSCALE_X + ", MSKEW_X:" + MSKEW_X
                + ", MTRANS_X:" + MTRANS_X + ", MSKEW_Y:" + MSKEW_Y + ", MSCALE_Y:" + MSCALE_Y + ", MTRANS_Y:" + MTRANS_Y);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            mTempValue[Matrix.MSCALE_X] = MSCALE_X;
            mTempValue[Matrix.MSKEW_X] = MSKEW_X;
            mTempValue[Matrix.MTRANS_X] = MTRANS_X;
            mTempValue[Matrix.MSKEW_Y] = MSKEW_Y;
            mTempValue[Matrix.MSCALE_Y] = MSCALE_Y;
            mTempValue[Matrix.MTRANS_Y] = MTRANS_Y;
            mTempMatrix.setValues(mTempValue);
            canvas.concat(mTempMatrix);
        }
    }


    public void drawColor(int color){
        if(DEBUG) Log.d(TAG, "drawColor, color:" + Integer.toHexString(color));
        Canvas canvas = getCanvas();
        if(canvas!=null){
            canvas.drawColor(color);
        }
    }

    public void clearColor(){
        if(DEBUG) Log.d(TAG,"clearColor");
        Canvas canvas = getCanvas();
        if(canvas!=null){
            canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        }
    }

    public void drawRect(float left, float top, float width, float height, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawRect, left:" + left + ", top:" + top + ", width:" + width + ", height:" + height+ ", fillStyle:" + fillStyle);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            applyPaintStyle(fillStyle);
            canvas.drawRect(left, top, width + left, top + height, mPaint);
        }
    }

    public void clipRect(float left, float top, float width, float height){
        if(DEBUG) Log.d(TAG,"clipRect, left:" + left + ", top:" + top + ", width:" + width + ", height:" + height);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            canvas.clipRect(left, top, width + left, height + top);
        }
    }

    public void save(){
        if(DEBUG) Log.d(TAG,"save");
        Canvas canvas = getCanvas();
        if(canvas!=null) canvas.save();
        //save paint (color, alpha ...)
        CanvasPaint savePaint = obtainPaint();
        savePaint.set(mPaint);
        savedPaints.add(savePaint);
    }

    public void restore(){
        if(DEBUG) Log.d(TAG,"restore");
        Canvas canvas = getCanvas();
        if(canvas!=null) canvas.restore();
        //restore paint
        CanvasPaint savedPaint = savedPaints.pollLast();
        mPaint.set(savedPaint);
        releasePaint(savedPaint);
    }

    public void drawCanvas(CanvasApi drawCanvasApi, float offsetX, float offsetY){
        if(DEBUG) Log.d(TAG,"drawCanvas, offsetX:" + offsetX + ", offsetY:" + offsetY);
        drawCanvasApi.drawFromParentCanvas(this, offsetX, offsetY);
    }

    protected void drawFromParentCanvas(CanvasApi parentCanvasApi, float offsetX, float offsetY){
        Canvas parentCanvas = parentCanvasApi.getCanvas();
        Canvas drawCanvas = getCanvas();
        if(parentCanvas!=null && drawCanvas!=null){
            if(drawCanvas instanceof BitmapCanvas){
                Bitmap bitmap = ((BitmapCanvas) drawCanvas).bitmap;
                if(bitmap!=null && !bitmap.isRecycled()){
                    parentCanvas.drawBitmap(bitmap, offsetX, offsetY, mPaint);
                }
            }else{
                throw new RuntimeException("can only draw canvas create with createCanvas");
            }
        }
    }

    public void drawImage(ImageApi imageApi, float left, float top){
        if(DEBUG) Log.d(TAG,"drawImage");
        Canvas canvas = getCanvas();
        if(canvas!=null) {
            Bitmap bitmap = imageApi.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                canvas.drawBitmap(bitmap, left, top, mPaint);
            }
        }
    }
    public void drawImage(ImageApi imageApi, float dstLeft, float dstTop, float dstRight, float dstBottom){
        if(DEBUG) Log.d(TAG,"drawImage, dstLeft:" + dstLeft + ", dstTop:" + dstTop + ", dstRight:" + dstRight + ", dstBottom:" + dstBottom);
        Canvas canvas = getCanvas();
        if(canvas!=null) {
            Bitmap bitmap = imageApi.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                mRectFTemp.set(dstLeft, dstTop, dstRight, dstBottom);
                canvas.drawBitmap(bitmap, null, mRectFTemp, mPaint);
            }
        }
    }
    public void drawImage(ImageApi imageApi, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom){
        if(DEBUG) Log.d(TAG,"drawImage, srcLeft:" + srcLeft + ", srcTop:" + srcTop + ", srcRight:" + srcRight + ", srcBottom:" + srcBottom
                + ", dstLeft:" + dstLeft + ", dstTop:" + dstTop + ", dstRight:" + dstRight + ", dstBottom:" + dstBottom);
        Canvas canvas = getCanvas();
        if(canvas!=null) {
            Bitmap bitmap = imageApi.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                mRectTemp.set((int)srcLeft, (int)srcTop, (int)srcRight, (int)(srcBottom));
                mRectFTemp.set(dstLeft, dstTop, dstRight, dstBottom);
                canvas.drawBitmap(bitmap, mRectTemp, mRectFTemp, mPaint);
            }
        }
    }

    public void drawText(String text, float x, float y, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawText, text:" + text + ", x:" + x + ", y:" + y + ", fillStyle:" + fillStyle);
        Canvas canvas = getCanvas();
        if(canvas!=null) {
            applyPaintStyle(fillStyle);
            canvas.drawText(text, x, y, mPaint);
        }
    }

    public void setColor(int color, int fillStyle){
        if(DEBUG) Log.d(TAG,"setColor, color:" + color);
        switch (fillStyle){
            case 0://Paint.Style.FILL;
                mPaint.fillColor = color;
                mPaint.setColor(color);
                break;
            case 1://Paint.Style.STROKE;
                mPaint.strokeColor = color;
                break;
            case 2://Paint.Style.FILL_AND_STROKE;
                mPaint.strokeColor = color;
                mPaint.fillColor = color;
                mPaint.setColor(color);
                break;
            default://Paint.Style.FILL;
                mPaint.fillColor = color;
                mPaint.setColor(color);
                break;
        }
    }

    public void multiplyGlobalAlpha(float alpha){
        if(DEBUG) Log.d(TAG,"multiplyGlobalAlpha, alpha:" + alpha);
        mPaint.setGlobalAlpha(alpha * mPaint.getGlobalAlpha());
    }

    public void setGlobalAlpha(float alpha){
        if(DEBUG) Log.d(TAG,"setGlobalAlpha, alpha:" + alpha);
        mPaint.setGlobalAlpha(alpha);
    }

    public void setTextAlign(String textAlign){
        if(DEBUG) Log.d(TAG,"setTextAlign, textAlign:" + textAlign);
        Paint.Align align = Paint.Align.valueOf(textAlign.toUpperCase());
        if(align!=null){
            mPaint.setTextAlign(align);
        }
    }

    public void setLineWidth(float lineWidth){
        if(DEBUG) Log.d(TAG,"setLineWidth, lineWidth:" + lineWidth);
        mPaint.setStrokeWidth(lineWidth);
    }

    public void setLineCap(String cap){
        if(DEBUG) Log.d(TAG,"setLineCap, cap:" + cap);
        Paint.Cap c = Paint.Cap.valueOf(cap.toUpperCase());
        if(c!=null){
            mPaint.setStrokeCap(c);
        }
    }

    public void setLineJoin(String lineJoin){
        if(DEBUG) Log.d(TAG,"setLineJoin, lineJoin:" + lineJoin);
        Paint.Join join = Paint.Join.valueOf(lineJoin.toUpperCase());
        if(join!=null){
            mPaint.setStrokeJoin(join);
        }
    }

    public void setShadow(float radius, float dx, float dy, int color){
        if(DEBUG) Log.d(TAG,"setShadow, radius:" + radius + ", dx:" + dx + ", dy:" + dy + ", color:" + color);
        mPaint.setShadowLayer(radius, dx, dy, color);
    }

    public void setFontSize(float textSize){
        if(DEBUG) Log.d(TAG,"setFontSize, textSize:" + textSize);
        mPaint.setTextSize(textSize);
    }

    public void drawOval(float left, float top, float right, float bottom, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawOval, left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom+ ", fillStyle:" + fillStyle);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            applyPaintStyle(fillStyle);
            mRectFTemp.set(left, top, right, bottom);
            canvas.drawOval(mRectFTemp, mPaint);
        }
    }

    public void drawCircle(float cx, float cy, float radius, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawCircle, cx:" + cx + ", cy:" + cy + ", radius:" + radius + ", fillStyle:" + fillStyle);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            applyPaintStyle(fillStyle);
            canvas.drawCircle(cx, cy, radius, mPaint);
        }
    }

    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean useCenter, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawArc, left:" + left + ", top:" + top + ", right:" + right + ", bottom:" + bottom
                + ", startAngle:" + startAngle + ", sweepAngle:" + sweepAngle + ", useCenter:" + useCenter + ", fillStyle:" + fillStyle);
        Canvas canvas = getCanvas();
        if(canvas!=null) {
            applyPaintStyle(fillStyle);
            mRectFTemp.set(left, top, right, bottom);
            canvas.drawArc(mRectFTemp, startAngle, sweepAngle, useCenter, mPaint);
        }
    }

    public void drawRoundRect(float left, float top, float width, float height, float radiusTopLeft,
                              float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int fillStyle){
        if(DEBUG) Log.d(TAG,"drawRoundRect, left:" + left + ", top:" + top + ", width:" + width + ", height:" + height
                + ", radiusTopLeft:" + radiusTopLeft+ ", radiusTopRight:" + radiusTopRight+ ", radiusBottomRight:"
                + radiusBottomRight+ ", radiusBottomLeft:" + radiusBottomLeft+ ", fillStyle:" + fillStyle);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            applyPaintStyle(fillStyle);

            mPathTemp.reset();
            mRectFTemp.set(left, top, left + width, top + height);
            mRadiiTemp[0] = mRadiiTemp[1] = radiusTopLeft;
            mRadiiTemp[2] = mRadiiTemp[3] = radiusTopRight;
            mRadiiTemp[4] = mRadiiTemp[5] = radiusBottomRight;
            mRadiiTemp[6] = mRadiiTemp[7] = radiusBottomLeft;
            mPathTemp.addRoundRect(mRectFTemp, mRadiiTemp, Path.Direction.CW);
            canvas.drawPath(mPathTemp, mPaint);
        }
    }


    public void clipRoundRect(float left, float top, float width, float height, float radiusTopLeft,
                              float radiusTopRight, float radiusBottomRight, float radiusBottomLeft){
        Canvas canvas = getCanvas();
        if(canvas!=null){
            mRectFTemp.set(left, top, left + width, top + height);
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP){
                mPathTemp.reset();
                mRadiiTemp[0] = mRadiiTemp[1] = radiusTopLeft;
                mRadiiTemp[2] = mRadiiTemp[3] = radiusTopRight;
                mRadiiTemp[4] = mRadiiTemp[5] = radiusBottomRight;
                mRadiiTemp[6] = mRadiiTemp[7] = radiusBottomLeft;
                mPathTemp.addRoundRect(mRectFTemp, mRadiiTemp, Path.Direction.CW);
                canvas.clipPath(mPathTemp);
            } else {//android 4.0-4.2 not support, android 4.3 & 4.4 draw bug.
                canvas.clipRect(mRectFTemp);
            }
        }
    }

    public void setFont(String fontName) {
        if(DEBUG) Log.d(TAG,"setFont, fontName:" + fontName);
        Canvas canvas = getCanvas();
        if(canvas!=null){
            mPaint.setTypeface(Typeface.create(fontName, Typeface.NORMAL));
        }
    }


    public static class BitmapCanvas extends Canvas{
        private Bitmap bitmap;

        public BitmapCanvas(Bitmap bitmap) {
            super(bitmap);
            this.bitmap = bitmap;
        }

        @Override
        public void setBitmap(Bitmap bitmap) {
            super.setBitmap(bitmap);
            this.bitmap = bitmap;
        }
    }

    protected static class CanvasPaint extends Paint{
        int strokeColor = Color.BLACK;
        int fillColor = Color.BLACK;
        float globalAlpha = 1;
        int color = Color.BLACK;

        @Override
        public void reset() {
            super.reset();
            strokeColor = Color.BLACK;
            fillColor = Color.BLACK;
        }

        public void setGlobalAlpha(float alpha) {
            globalAlpha = alpha;
            int newAlpha = (int) (Color.alpha(color) * alpha);//multiply alpha to color
            super.setAlpha(newAlpha);
        }

        public float getGlobalAlpha() {
            return globalAlpha;
        }

        @Override
        public void setColor(int color) {
            this.color = color;
            int newAlpha = (int) (Color.alpha(color) * globalAlpha);//multiply alpha
            super.setColor(Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color)));
        }

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public void set(Paint src) {
            super.set(src);
            if(src instanceof CanvasPaint){
                strokeColor = ((CanvasPaint) src).strokeColor;
                fillColor = ((CanvasPaint) src).fillColor;
                setGlobalAlpha(((CanvasPaint) src).globalAlpha);
                setColor(((CanvasPaint) src).color);
            }
        }
    }
}
