package com.purrfolio.mco1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

/** Concentric wellness arcs around the pet photo, with clickable photo and badges. */
public class PetRings extends FrameLayout {
    private static final class Badge {
        final String kind; final int tint; final float x,y;
        Badge(String kind,int tint,float x,float y){this.kind=kind;this.tint=tint;this.x=x;this.y=y;}
    }
    private static final class Marker {
        final View view; final Badge badge;
        Marker(View view,Badge badge){this.view=view;this.badge=badge;}
    }
    private final Bitmap photo;
    private final boolean light;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Marker> badges=new ArrayList<>();
    private final View photoTapTarget;

    public PetRings(Context context,Bitmap photo,boolean light,Runnable onPhotoClick){
        super(context);this.photo=photo;this.light=light;
        setClipChildren(false);
        addView(new View(context){@Override protected void onDraw(Canvas canvas){drawRings(canvas,getWidth(),getHeight());}},
                new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        photoTapTarget=new View(context);
        photoTapTarget.setClickable(true);
        photoTapTarget.setContentDescription("Pet photo. Tap to add, change, or remove it.");
        photoTapTarget.setOnClickListener(v->onPhotoClick.run());
        addView(photoTapTarget,new LayoutParams(1,1));
        Badge[] definitions={new Badge("Energy",Color.rgb(255,209,140),296,160),
                new Badge("Food",Color.rgb(243,182,215),88,114),
                new Badge("Water",Color.rgb(155,203,255),220,246),
                new Badge("Mood",Color.rgb(145,229,200),32,207)};
        for(Badge badge:definitions){
            FrameLayout marker=new FrameLayout(context);
            GradientDrawable background=new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(light?Color.WHITE:Color.rgb(36,34,47));
            background.setStroke(dp(2),badge.tint);
            marker.setBackground(background);
            marker.setContentDescription(badge.kind);
            marker.addView(new CareArtwork(context,badge.kind,badge.tint),new LayoutParams(dp(22),dp(22),Gravity.CENTER));
            addView(marker,new LayoutParams(1,1));badges.add(new Marker(marker,badge));
        }
    }
    public PetRings(Context context,Bitmap photo,boolean light){this(context,photo,light,()->{});}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}

    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        super.onLayout(changed,left,top,right,bottom);
        int w=right-left,h=bottom-top;
        float scale=Math.min(w,h)/320f,offsetX=(w-320*scale)/2,offsetY=(h-320*scale)/2;
        int photoSize=(int)(130*scale),photoLeft=(int)(offsetX+95*scale),photoTop=(int)(offsetY+95*scale);
        photoTapTarget.layout(photoLeft,photoTop,photoLeft+photoSize,photoTop+photoSize);
        for(Marker marker:badges){
            int size=(int)(34*scale),x=(int)(offsetX+(marker.badge.x-17)*scale),y=(int)(offsetY+(marker.badge.y-17)*scale);
            marker.view.layout(x,y,x+size,y+size);
        }
    }

    private void drawRings(Canvas canvas,int w,int h){
        float scale=Math.min(w,h)/320f;
        canvas.save();canvas.translate((w-320*scale)/2,(h-320*scale)/2);canvas.scale(scale,scale);
        int[] colors={Color.rgb(255,209,140),Color.rgb(216,180,248),Color.rgb(243,182,215)};
        float[] radii={136,105,85};
        for(int i=0;i<3;i++){
            float radius=radii[i];paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(23);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(light?Color.rgb(232,223,237):Color.rgb(35,32,44));
            canvas.drawCircle(160,160,radius,paint);
            paint.setColor(colors[i]);
            if(i==1)paint.setShader(new LinearGradient(160,55,220,265,colors[1],Color.rgb(155,203,255),Shader.TileMode.CLAMP));
            canvas.drawArc(160-radius,160-radius,160+radius,160+radius,i==2?120:-90,i==2?92:90+i*55,false,paint);
            paint.setShader(null);
            if(i==0){paint.setColor(Color.rgb(145,229,200));canvas.drawArc(24,24,296,296,120,40,false,paint);}
        }
        canvas.save();Path clip=new Path();clip.addCircle(160,160,65,Path.Direction.CW);canvas.clipPath(clip);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(photo,new Rect((int)(photo.getWidth()*.18f),(int)(photo.getHeight()*.22f),
                (int)(photo.getWidth()*.83f),(int)(photo.getHeight()*.87f)),new RectF(90,90,230,230),paint);
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(colors[1]);
        canvas.drawCircle(160,160,68,paint);canvas.restore();
    }
}
