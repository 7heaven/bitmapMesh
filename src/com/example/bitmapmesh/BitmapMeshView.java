package com.example.bitmapmesh;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

public class BitmapMeshView extends View {

    private Bitmap bitmap;
    private Paint paint;

    private int width, height;
    private int centerX, centerY;

    private int bitmapWidth = 30;
    private int bitmapHeight = 10;

    private int touchX;
    private int touchY;

    private final static int insDistance = 30;

    private boolean newApiFlag;

    private int delayOffsetX;

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

    public BitmapMeshView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        handler.post(delayRunnable);

        newApiFlag = Build.VERSION.SDK_INT >= 18;
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
        for (int y = 0; y <= bitmapHeight; y++) {
            // float fy = bitmap.getHeight() * y / 10;
            // float realWidth =
            // width
            // - ((float) Math.sin(y * (height / bitmapHeight) * 0.5F)
            // * Math.abs(delayOffsetX - touchX) + (Math.abs(delayOffsetX
            // - touchX) / 2));

            float fy = height / bitmapHeight * y;
            float longDisSide = touchY > height - touchY ? touchY : height - touchY;
            float longRatio = Math.abs(fy - touchY) / longDisSide;

            longRatio = new AccelerateInterpolator().getInterpolation(longRatio);

            float realWidth = longRatio * (touchX - delayOffsetX);

            for (int x = 0; x <= bitmapWidth; x++) {

                verts[index * 2 + 0] = (width * ratio - (realWidth)) / bitmapWidth * x;

                // float realHeight =
                // x % 2 == 0 ? height : height
                // * (0.6F + ((float) touchX / (float) width) * 0.4F);

                float gap = 60.0F * (1.0F - ratio);

                float realHeight =
                        height
                                - ((float) Math.sin((x * (width / bitmapWidth)) * 0.5F) * gap + (gap / 2));

                // float realHeightForOffset =
                // (height - realHeight) > width / (bitmapWidth / 2) ? height - width
                // / (bitmapWidth / 2) : realHeight;

                float offsetY = realHeight / bitmapHeight * y;

                verts[index * 2 + 1] = (height - realHeight) / 2 + offsetY;

                int px = (int) verts[index * 2 + 0];
                int py = (int) verts[index * 2 + 1];

                px = px < 0 ? 0 : px;
                py = py < 0 ? 0 : py;

                px = px > bitmap.getWidth() ? bitmap.getWidth() : px;
                py = py > bitmap.getHeight() ? bitmap.getHeight() : py;

                int color;

                int channel = 255 - (int) (height - realHeight);
                channel = channel < 0 ? 0 : channel;
                channel = channel > 255 ? 255 : channel;

                color = 0xFF000000 | channel << 16 | channel << 8 | channel;

                colors[index] = color;

                index += 1;
            }
        }

        canvas.drawBitmapMesh(bitmap, bitmapWidth, bitmapHeight, verts, 0, colors, 0, null);
    }
}
