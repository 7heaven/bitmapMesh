package com.example.bitmapmesh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

public class BitmapMeshView extends View {

    private Bitmap bitmap;
    private Bitmap shadowMask;
    private Paint paint;
    private Shader maskShader;
    private int maxAlpha = 0xFF;

    private int width, height;
    private int centerX, centerY;

    private int bitmapWidth = 30;
    private int bitmapHeight = 10;

    private int touchX;
    private int touchY;

    private final static int insDistance = 30;

    private boolean newApiFlag;

    private int delayOffsetX;

    private AccelerateInterpolator interpolator;

    private Handler handler = new Handler();
    private Runnable delayRunnable = new Runnable() {
        @Override
        public void run() {
            delayOffsetX += (touchX - delayOffsetX) * 0.5F;

            handler.postDelayed(this, 20);
            invalidate();
        }
    };

    public BitmapMeshView(Context context) {
        this(context, null);
    }

    public BitmapMeshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("NewApi")
    public BitmapMeshView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.snipper);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        handler.post(delayRunnable);

        newApiFlag = Build.VERSION.SDK_INT >= 18;

        interpolator = new AccelerateInterpolator();

        if (!newApiFlag) {

            // 硬件加速不支持drawBitmapMesh的colors绘制的情况下,在原bitmap的上层覆盖一个半透明带阴影的bitmap以实现阴影功能

            shadowMask =
                    Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                            Bitmap.Config.ARGB_8888);

            Canvas maskCanvas = new Canvas(shadowMask);

            float singleWave = bitmap.getWidth() / bitmapWidth * 6.28F;
            int blockPerWave = (int) (singleWave / (bitmap.getWidth() / bitmapWidth));

            if (blockPerWave % 2 == 0)
                blockPerWave++;

            float offset =
                    (float) ((bitmap.getWidth() / singleWave - Math.floor(bitmap.getWidth()
                            / singleWave)) * singleWave);

            int[] colors = new int[blockPerWave];
            float[] offsets = new float[blockPerWave];

            Log.d("singleWave:" + singleWave, "blockPerWave:" + blockPerWave);


            float perOffset = 1.0F / blockPerWave;

            int halfWave = (int) Math.floor((float) blockPerWave / 2.0F);

            int perAlpha = maxAlpha / (halfWave - 1);

            for (int i = -halfWave; i < halfWave + 1; i++) {
                int ii = halfWave - Math.abs(i);
                int iii = i + halfWave;
                colors[iii] =
                        (int) (perAlpha * Math.sin((float) ii / (float) blockPerWave * 3.14F)) << 24;

                offsets[iii] = perOffset * iii;

                Log.d("index:" + i, "colors:0x" + Integer.toHexString(colors[iii]) + ", offset:"
                        + offsets[iii]);
            }

            maskShader =
                    new LinearGradient(offset, 0, singleWave + offset, 0, colors, offsets,
                            Shader.TileMode.REPEAT);

            paint.setShader(maskShader);
            maskCanvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
            paint.setShader(null);

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        centerX = width / 2;
        centerY = height / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchX = (int) event.getX();
                touchY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = (int) event.getX();
                touchY = (int) event.getY();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }

        invalidate();

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float[] verts = new float[(bitmapWidth + 1) * (bitmapHeight + 1) * 2];
        int[] colors = new int[(bitmapWidth + 1) * (bitmapHeight + 1)];
        int index = 0;

        float ratio = (float) touchX / (float) width;
        int alpha = 0;
        for (int y = 0; y <= bitmapHeight; y++) {

            float fy = height / bitmapHeight * y;
            float longDisSide = touchY > height - touchY ? touchY : height - touchY;
            float longRatio = Math.abs(fy - touchY) / longDisSide;

            longRatio = interpolator.getInterpolation(longRatio);

            float realWidth = longRatio * (touchX - delayOffsetX);

            for (int x = 0; x <= bitmapWidth; x++) {

                verts[index * 2 + 0] = (width * ratio - (realWidth)) / bitmapWidth * x;


                float gap = 60.0F * (1.0F - ratio);

                float realHeight = height - ((float) Math.sin((x * 2) * 0.5F) * gap + gap);

                float offsetY = realHeight / bitmapHeight * y;

                verts[index * 2 + 1] = (height - realHeight) / 2 + offsetY;

                int px = (int) verts[index * 2 + 0];
                int py = (int) verts[index * 2 + 1];

                px = px < 0 ? 0 : px;
                py = py < 0 ? 0 : py;

                px = px > bitmap.getWidth() ? bitmap.getWidth() : px;
                py = py > bitmap.getHeight() ? bitmap.getHeight() : py;

                int color;

                int channel = 255 - (int) (height - realHeight) * 2;
                if (channel < 255) {
                    alpha = (int) ((255 - channel) / 120.0F * maxAlpha) * 4;
                }
                if (newApiFlag) {
                    channel = channel < 0 ? 0 : channel;
                    channel = channel > 255 ? 255 : channel;

                    color = 0xFF000000 | channel << 16 | channel << 8 | channel;

                    colors[index] = color;
                }

                index += 1;
            }
        }

        canvas.drawBitmapMesh(bitmap, bitmapWidth, bitmapHeight, verts, 0, colors, 0, null);
        if (!newApiFlag) {
            alpha = alpha > 255 ? 255 : alpha;
            alpha = alpha < 0 ? 0 : alpha;
            paint.setAlpha(alpha);
            canvas.drawBitmapMesh(shadowMask, bitmapWidth, bitmapHeight, verts, 0, null, 0, paint);
            paint.setAlpha(255);
        }
    }
}
