package com.purrfolio.mco1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Rounded line icons for forms, account settings, records, and sharing. */
public class DetailIcon extends View {
    private final String kind;
    private final int tint;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Canvas canvas;

    public DetailIcon(Context context, String kind, int tint) {
        super(context);
        this.kind = kind;
        this.tint = tint;
        paint.setStrokeWidth(1.7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    private void line(float... points) {
        Path path = new Path(); path.moveTo(points[0], points[1]);
        for (int i=2; i<points.length; i+=2) path.lineTo(points[i], points[i+1]);
        canvas.drawPath(path,paint);
    }
    private void circle(float x,float y,float radius) { canvas.drawCircle(x,y,radius,paint); }
    private void box(float l,float t,float r,float b,float radius) {
        canvas.drawRoundRect(l,t,r,b,radius,radius,paint);
    }
    private void box(float l,float t,float r,float b) { box(l,t,r,b,2f); }
    private void path(float[] start, float[]... curves) {
        Path p = new Path(); p.moveTo(start[0],start[1]);
        for (float[] curve:curves) {
            if (curve.length==2) p.lineTo(curve[0],curve[1]);
            else if (curve.length==4) p.quadTo(curve[0],curve[1],curve[2],curve[3]);
            else p.cubicTo(curve[0],curve[1],curve[2],curve[3],curve[4],curve[5]);
        }
        canvas.drawPath(p,paint);
    }
    private static float[] p(float... values) { return values; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); canvas=c;
        float scale=Math.min(getWidth(),getHeight())/24f;
        c.save(); c.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2); c.scale(scale,scale);
        paint.setColor(tint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f);
        switch (kind) {
            case "Sparkle": line(12,1,15,9,23,12,15,15,12,23,9,15,1,12,9,9,12,1); break;
            case "Warning": line(12,2,23,22,1,22,12,2);line(12,8,12,15);circle(12,19,1);break;
            case "Progress":
                paint.setStrokeWidth(3.4f);paint.setColor(Color.parseColor("#443C55"));circle(12,12,9);
                String[] colors={"#D8B4F8","#91E5C8","#FFD18C"};
                for(int i=0;i<3;i++){paint.setColor(Color.parseColor(colors[i]));c.drawArc(3,3,21,21,-90+i*96,92,false,paint);}break;
            case "Person": path(p(4,21),p(4,18),p(4,11,20,11,20,18),p(20,21));circle(12,6,4);break;
            case "Mail": box(2,5,22,20);line(3,6,12,13,21,6);break;
            case "Lock": box(5,10,19,22);c.drawArc(8,1,16,15,180,180,false,paint);line(8,6,8,10);line(16,6,16,10);break;
            case "Camera": box(2,6,22,21,3);circle(12,13,4);line(7,6,9,3,15,3,17,6);break;
            case "Settings": circle(12,12,7);circle(12,12,2.5f);for(int i=0;i<8;i++){double r=i*Math.PI/4;line((float)(12+7*Math.cos(r)),(float)(12+7*Math.sin(r)),(float)(12+10*Math.cos(r)),(float)(12+10*Math.sin(r)));}break;
            case "Bell": path(p(4,18),p(7,15,7,8),p(7,1,17,1,17,8),p(17,15,20,18));c.drawArc(9,18,15,23,0,180,false,paint);break;
            case "Phone": path(p(4,2),p(8,2),p(10,7),p(7,10),p(9,15,14,17),p(17,14),p(22,17),p(21,21),p(10,26,-3,10,4,2));break;
            case "Location": path(p(12,23),p(32,4,12,-6,5,5),p(0,12,9,20,12,23));circle(12,8,3);break;
            case "Globe": circle(12,12,10);c.drawOval(7,2,17,22,paint);line(2,12,22,12);line(4,6,20,6);line(4,18,20,18);break;
            case "Moon": path(p(16,2),p(3,0,-3,19,12,22),p(17,23,21,18,22,14),p(10,20,6,9,16,2));break;
            case "Database": c.drawOval(4,2,20,8,paint);line(4,5,4,19);line(20,5,20,19);c.drawArc(4,9,20,15,0,180,false,paint);c.drawArc(4,16,20,22,0,180,false,paint);break;
            case "Trash": line(3,5,21,5);box(6,5,18,22,1);line(9,5,9,2,15,2,15,5);line(10,9,10,18);line(14,9,14,18);break;
            case "Down": line(6,9,12,15,18,9);break;
            case "Clock": circle(12,12,10);line(12,5,12,12,17,14);break;
            case "Search": circle(10,10,7);line(15,15,22,22);break;
            case "Filter": for(float y:new float[]{5,12,19})line(2,y,22,y);circle(8,5,2);circle(16,12,2);circle(8,19,2);break;
            case "Document": case "Medical": line(5,2,15,2,21,8,21,22,5,22,5,2);line(15,2,15,8,21,8);if(kind.equals("Medical")){line(9,14,17,14);line(13,10,13,18);}else{line(9,12,17,12);line(9,16,17,16);line(9,19,14,19);}break;
            case "Folder": line(2,21,2,4,9,4,12,8,22,8,22,21,2,21);break;
            case "Download": line(12,2,12,16);line(7,11,12,16,17,11);line(3,16,3,22,21,22,21,16);break;
            case "Expand": line(9,3,3,3,3,9);line(15,3,21,3,21,9);line(3,15,3,21,9,21);line(15,21,21,21,21,15);break;
            case "Print": box(2,7,22,18);box(6,2,18,7,0);box(6,14,18,23,0);break;
            case "Photo": box(2,2,22,22);circle(8,8,2);line(3,19,10,12,14,16,17,12,22,18);break;
            case "Cloud": path(p(7,18),p(-5,18,2,8,7,9),p(6,-2,22,-2,20,10),p(28,12,22,20,18,18));line(12,23,12,12);line(8,16,12,12,16,16);break;
            case "Wifi": c.drawArc(1,4,23,24,220,100,false,paint);c.drawArc(5,9,19,24,220,100,false,paint);circle(12,20,1);line(2,2,22,23);break;
            case "Star": Path star=new Path();for(int i=0;i<10;i++){double t=-Math.PI/2+i*Math.PI/5,r=i%2==0?10:4.5;float x=(float)(12+r*Math.cos(t)),y=(float)(12+r*Math.sin(t));if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();c.drawPath(star,paint);break;
            case "Megaphone": line(2,9,9,9,21,3,21,20,9,15,2,15,2,9);line(6,15,8,22,12,22,10,15);break;
            case "Play": paint.setStyle(Paint.Style.FILL);Path play=new Path();play.moveTo(8,4);play.lineTo(21,12);play.lineTo(8,20);play.close();c.drawPath(play,paint);break;
            case "Stop": paint.setStyle(Paint.Style.FILL);box(5,5,19,19);break;
            case "Logout": line(12,3,4,3,4,21,12,21);line(9,12,22,12);line(17,7,22,12,17,17);break;
            case "Sex": circle(12,8,6);line(12,14,12,23);line(8,19,16,19);break;
            case "Cat": path(p(3,3),p(9,7),p(16,7),p(22,3),p(21,16),p(20,24,4,24,3,16));circle(8,13,.6f);circle(16,13,.6f);line(11,17,13,17);break;
            default: circle(12,12,9);line(12,6,12,13);circle(12,18,.6f);
        }
        c.restore();
    }
}
