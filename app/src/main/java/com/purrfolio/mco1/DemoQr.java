package com.purrfolio.mco1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Decorative sample QR, intentionally not scannable. */
public class DemoQr extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DemoQr(Context context) { super(context); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth(), getHeight()) / 29f;
        canvas.save();
        canvas.translate((getWidth()-29*scale)/2, (getHeight()-29*scale)/2);
        canvas.scale(scale, scale);
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, 29, 29, paint);
        paint.setColor(Color.BLACK);
        for (int y = 2; y <= 26; y++) for (int x = 2; x <= 26; x++) {
            if ((x*17+y*31+x*y)%7 < 3) canvas.drawRect(x,y,x+1,y+1,paint);
        }
        paint.setColor(Color.WHITE);
        canvas.drawRect(1,1,10,10,paint);
        canvas.drawRect(19,1,28,10,paint);
        canvas.drawRect(1,19,10,28,paint);
        finder(canvas,2,2); finder(canvas,20,2); finder(canvas,2,20);
        paint.setColor(Color.WHITE);
        canvas.drawRect(10,10,20,20,paint);
        paint.setColor(Color.rgb(149,91,195));
        paint.setTextSize(3.7f);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("DEMO",10,16,paint);
        canvas.restore();
    }

    private void finder(Canvas canvas, int x, int y) {
        paint.setColor(Color.BLACK); canvas.drawRect(x,y,x+7,y+7,paint);
        paint.setColor(Color.WHITE); canvas.drawRect(x+1,y+1,x+6,y+6,paint);
        paint.setColor(Color.BLACK); canvas.drawRect(x+2,y+2,x+5,y+5,paint);
    }
}
