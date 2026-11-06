package com.purrfolio.mco1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class Waveform extends View {
    private final int tint;
    private final boolean circular;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Waveform(Context context, int tint, boolean circular) {
        super(context);
        this.tint = tint;
        this.circular = circular;
    }

    public Waveform(Context context, int tint) { this(context, tint, false); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f, centerY = getHeight() / 2f;
        paint.setColor(tint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(getWidth() / 100f);
        if (circular) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3 * getResources().getDisplayMetrics().density);
            float radius = Math.min(getWidth(), getHeight()) * .42f;
            paint.setAlpha(85);
            canvas.drawCircle(centerX, centerY, radius, paint);
            paint.setAlpha(255);
            canvas.drawArc(centerX-radius+10, centerY-radius+10, centerX+radius-10,
                    centerY+radius-10, 135f, 290f, false, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        int count = circular ? 5 : 45;
        float span = getWidth() * (circular ? .24f : .92f);
        float step = span / count;
        float[] circularAmplitudes = {.25f, .5f, .8f, .5f, .25f};
        for (int i = 0; i < count; i++) {
            float amplitude = circular ? circularAmplitudes[i] :
                    (float)(.2 + Math.abs(Math.sin(i * .63)) * Math.abs(Math.sin(i * .15)) * .75);
            float height = getHeight() * (circular ? .3f : .85f) * amplitude;
            float x = centerX - span/2 + i*step;
            canvas.drawLine(x, centerY-height/2, x, centerY+height/2, paint);
        }
    }
}
