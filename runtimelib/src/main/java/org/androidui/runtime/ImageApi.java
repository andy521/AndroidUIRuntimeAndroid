package org.androidui.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import org.androidui.runtime.image.ImageLoader;

import java.io.File;

/**
 * Created by linfaxin on 15/12/15.
 *
 */
public class ImageApi {
    private static final String URL_BASE64PNG_PRE = "data:image/png;base64,";
    private static final String URL_BASE64JPG_PRE = "data:image/jpg;base64,";
    private static final String URL_BASE64GIF_PRE = "data:image/gif;base64,";

    static void initImageLoader(Context context, boolean d){
        File imgDir = new File(context.getExternalCacheDir(), "AndroidUIRuntime/.img/");
        ImageLoader.DEBUG = d;
        ImageLoader.init(context, imgDir);
    }

    private Bitmap bitmap;
    private String bitmapUrl;
    private String loadingUrl;
    private ImageLoader.LoadImageTask loadingTask;
    private RuntimeBridge bridge;

    public ImageApi(RuntimeBridge bridge) {
        this.bridge = bridge;
    }

    public void loadImage(String url){
        if(TextUtils.equals(url, loadingUrl)) return;
        loadingUrl = url;
        loadImageImpl();
    }

    private void loadImageImpl(){

        //synchronous if base64
        if(loadingUrl.startsWith(URL_BASE64PNG_PRE)){
            String base64 = loadingUrl.substring(URL_BASE64PNG_PRE.length());
            byte[] bitmapArray = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            onImageLoadFinish(loadingUrl, bitmap, true);
            return;
        }
        if(loadingUrl.startsWith(URL_BASE64JPG_PRE) || loadingUrl.startsWith(URL_BASE64GIF_PRE)){
            String base64 = loadingUrl.substring(URL_BASE64JPG_PRE.length());
            byte[] bitmapArray = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            onImageLoadFinish(loadingUrl, bitmap);
            return;
        }

        if(loadingTask!=null) loadingTask.cancel(true);
        loadingTask = ImageLoader.getBitmapInBg(loadingUrl, new ImageLoader.ImageLoadingListener() {
            @Override
            public void onBitmapLoadFinish(Bitmap bitmap, String url) {
                onImageLoadFinish(url, bitmap);
            }
        });
    }

    private void onImageLoadFinish(String url, Bitmap bitmap){
        onImageLoadFinish(url, bitmap, false);
    }
    private void onImageLoadFinish(String url, Bitmap bitmap, boolean parseBorder){
        if(url == null || !url.equals(loadingUrl)) return;
        if(bitmap!=null){
            if(parseBorder){
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] leftBorder = new int[height-2];
                int[] topBorder = new int[width-2];
                int[] rightBorder = new int[height-2];
                int[] bottomBorder = new int[width-2];
                bitmap.getPixels(leftBorder, 0, 1, 0, 1, 1, height-2);
                bitmap.getPixels(topBorder, 0, width-2, 1, 0, width-2, 1);
                bitmap.getPixels(rightBorder, 0, 1, width-1, 1, 1, height-2);
                bitmap.getPixels(bottomBorder, 0, width-2, 1, height-1, width-2, 1);
                bridge.notifyImageLoadFinish(ImageApi.this, bitmap.getWidth(), bitmap.getHeight(),
                        leftBorder, topBorder, rightBorder, bottomBorder);
            }else{
                bridge.notifyImageLoadFinish(ImageApi.this, bitmap.getWidth(), bitmap.getHeight());
            }
        }else{
            bridge.notifyImageLoadError(ImageApi.this);
        }
//        if(this.bitmap!=null){
//            this.bitmap.recycle();
//        }
        this.bitmap = bitmap;
        this.bitmapUrl = url;
    }

    public void recycle(){
        //not recycle here, ImageLoader will manager them.
//        if(bitmap!=null){
//            bitmap.recycle();
//        }

    }

    public Bitmap getBitmap() {
        if(bitmap!=null && bitmap.isRecycled() && TextUtils.equals(bitmapUrl, loadingUrl)){
            loadImageImpl();
        }
        if(bitmap!=null && bitmap.isRecycled()){
            return null;
        }
        if(!TextUtils.isEmpty(bitmapUrl)){
            ImageLoader.notifyUrlUse(bitmapUrl);
        }
        return bitmap;
    }
}
