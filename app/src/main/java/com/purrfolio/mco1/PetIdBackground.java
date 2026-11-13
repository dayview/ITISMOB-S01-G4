package com.purrfolio.mco1;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Pastel decoration behind the native pet ID content. */
public class PetIdBackground extends Drawable {
    private final boolean horizontal;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PetIdBackground(boolean horizontal) { this.horizontal = horizontal; }

    @Override public void draw(Canvas canvas) {
        float w = getBounds().width(), h = getBounds().height();
        RectF rect = new RectF(getBounds());
        Path clip = new Path();
        clip.addRoundRect(rect,26*w/365f,26*w/365f,Path.Direction.CW);
        canvas.save(); canvas.clipPath(clip);
        paint.setShader(new LinearGradient(0,0,w,h,
                new int[]{Color.parseColor("#FFF0F5"),Color.parseColor("#E2C3FA")},
                null,Shader.TileMode.CLAMP));
        canvas.drawRect(rect,paint); paint.setShader(null);
        paint.setColor(Color.parseColor("#DCC0FA"));
        canvas.drawCircle(w*.98f,h*.02f,w*.17f,paint);
        canvas.drawCircle(-w*.06f,h*.57f,w*.18f,paint);
        canvas.drawCircle(w*.99f,h*.95f,w*.12f,paint);
        if (horizontal) { heart(canvas,w*.49f,h*.36f,w*.025f); paw(canvas,w*.94f,h*.89f,w*.028f); }
        else {
            heart(canvas,w*.12f,h*.10f,w*.035f); heart(canvas,w*.87f,h*.58f,w*.03f);
            paw(canvas,w*.86f,h*.31f,w*.055f); paw(canvas,w*.12f,h*.91f,w*.04f);
        }
        canvas.restore();
    }

    private void heart(Canvas canvas,float x,float y,float size) {
        canvas.save(); canvas.translate(x,y); canvas.scale(size,size);
        paint.setColor(Color.parseColor("#F3ADD3"));
        Path path = new Path(); path.moveTo(0,1);
        path.cubicTo(-2f,-.3f,-.9f,-1.6f,0,-.6f);
        path.cubicTo(.9f,-1.6f,2f,-.3f,0,1);
        canvas.drawPath(path,paint); canvas.restore();
    }

    private void paw(Canvas canvas,float x,float y,float size) {
        paint.setColor(Color.parseColor("#BF91E9"));
        canvas.drawOval(x-size*.6f,y,x+size*.6f,y+size*.8f,paint);
        for (int i=0;i<4;i++) {
            double angle = Math.PI+(i+1)*Math.PI/5;
            canvas.drawCircle(x+(float)Math.cos(angle)*size,y+(float)Math.sin(angle)*size,size*.28f,paint);
        }
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
