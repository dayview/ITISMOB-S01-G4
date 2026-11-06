package com.purrfolio.mco1;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.graphics.PathParser;

/** Canvas artwork shared by care controls and the bottom navigation. */
public class CareArtwork extends View {
    private String kind="";
    private int tint=Color.rgb(216,180,248);
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int lavender=Color.rgb(216,180,248),pink=Color.rgb(243,182,215);
    private final int peach=Color.rgb(255,205,172),dark=Color.rgb(30,27,40);

    public CareArtwork(Context context){this(context,(AttributeSet)null,0);}
    public CareArtwork(Context context,AttributeSet attrs){this(context,attrs,0);}
    public CareArtwork(Context context,AttributeSet attrs,int defStyleAttr){
        super(context,attrs,defStyleAttr);
        if(attrs!=null){
            TypedArray values=context.obtainStyledAttributes(attrs,R.styleable.CareArtwork);
            try {String value=values.getString(R.styleable.CareArtwork_artKind);kind=value==null?"":value;
                tint=values.getColor(R.styleable.CareArtwork_artTint,tint);
            } finally {values.recycle();}
        }
    }
    public CareArtwork(Context context,String kind,int tint){this(context);this.kind=kind;this.tint=tint;}
    public CareArtwork(Context context,String kind){this(context,kind,Color.rgb(216,180,248));}
    public void setArtwork(String kind,int tint){this.kind=kind;this.tint=tint;invalidate();}
    private int iconInk(){return (getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES?dark:Color.rgb(110,91,129);}
    private void fill(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);}
    private void stroke(int color,float width){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
    private void line(Canvas c,float... xy){Path p=new Path();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);c.drawPath(p,paint);}
    private void path(Canvas c,Path p){c.drawPath(p,paint);}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.save();float scale=Math.min(getWidth(),getHeight())/48f;
        c.translate((getWidth()-48*scale)/2,(getHeight()-48*scale)/2);c.scale(scale,scale);
        Path q;
        switch(kind){
            case "Google":fill(tint);paint.setTextSize(39);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);c.drawText("G",24,37,paint);break;
            case "Facebook":fill(tint);path(c,PathParser.createPathFromPathData("M27,43 V27 H33 L34,20 H27 V16 Q27,12 31,12 H35 V6 H30 Q20,6 20,16 V20 H15 V27 H20 V43 Z"));break;
            case "Apple":fill(tint);path(c,PathParser.createPathFromPathData("M25,12 C24,6 29,2 34,2 C34,7 30,12 25,12 M25,15 C16,9 7,16 9,27 C10,35 16,44 20,43 C24,40 26,40 30,43 C34,44 39,36 41,31 C32,27 32,18 39,15 C34,10 29,13 25,15 Z"));break;
            case "Feeding":fill(lavender);q=new Path();q.moveTo(11,15);q.lineTo(5,36);q.cubicTo(5,44,43,44,43,36);q.lineTo(37,15);q.close();path(c,q);stroke(iconInk(),2);c.drawOval(10,10,38,23,paint);fill(peach);c.drawOval(10,9,38,20,paint);break;
            case "Medication":c.rotate(40,24,24);fill(lavender);c.drawRoundRect(14,3,34,45,10,10,paint);c.save();c.clipRect(10,0,38,24);fill(pink);c.drawRoundRect(14,3,34,45,10,10,paint);c.restore();stroke(iconInk(),1.5f);c.drawLine(14,24,34,24,paint);break;
            case "Water":stroke(Color.rgb(155,203,255),3.5f);q=new Path();q.moveTo(24,5);q.cubicTo(18,15,11,24,12,32);q.cubicTo(13,47,36,47,36,32);q.cubicTo(36,24,29,13,24,5);path(c,q);break;
            case "Playtime":fill(pink);c.drawCircle(24,24,20,paint);stroke(iconInk(),2.8f);c.drawArc(9,10,40,42,178,147,false,paint);q=new Path();q.moveTo(17,7);q.cubicTo(12,22,28,24,25,43);q.moveTo(31,7);q.cubicTo(15,18,23,31,39,30);path(c,q);break;
            case "Vet Visit":stroke(lavender,3.5f);line(c,10,6,10,19);c.drawArc(10,9,28,30,0,180,false,paint);line(c,28,19,28,6);line(c,7,6,13,6);line(c,25,6,31,6);q=new Path();q.moveTo(19,30);q.lineTo(19,34);q.cubicTo(19,46,38,47,38,32);path(c,q);c.drawCircle(38,28,4,paint);break;
            case "Grooming":c.rotate(40,24,24);fill(lavender);c.drawRoundRect(14,4,31,28,3,3,paint);c.drawRoundRect(19,25,27,46,4,4,paint);stroke(iconInk(),1.5f);for(int i=0;i<=5;i++)c.drawLine(24,8+i*3,32,8+i*3,paint);break;
            case "Home":stroke(tint,2.5f);line(c,5,23,24,7,43,23);line(c,11,21,11,42,20,42,20,29,29,29,29,42,37,42,37,21);break;
            case "Pets":case "Vaccinations":stroke(tint,2.6f);q=new Path();q.moveTo(14,25);q.cubicTo(1,35,8,46,20,39);q.cubicTo(26,36,32,46,40,38);q.cubicTo(47,30,30,16,25,20);q.cubicTo(21,20,18,23,14,25);path(c,q);c.drawOval(5,13,14,24,paint);c.drawOval(15,4,23,16,paint);c.drawOval(27,4,35,16,paint);c.drawOval(37,13,45,24,paint);break;
            case "Care":case "Notes":stroke(tint,2.5f);c.drawRoundRect(9,9,39,42,5,5,paint);c.drawLine(16,5,16,14,paint);c.drawLine(32,5,32,14,paint);line(c,16,24,22,24);line(c,28,24,32,24);line(c,16,33,28,33);break;
            case "More":fill(tint);for(float x:new float[]{9,24,39})c.drawCircle(x,24,2.5f,paint);break;
            case "Plus":stroke(tint,2.6f);line(c,24,9,24,39);line(c,9,24,39,24);break;
            case "Back":stroke(tint,2.5f);line(c,29,10,16,24,29,38);break;
            case "Close":stroke(tint,2.3f);line(c,13,13,35,35);line(c,35,13,13,35);break;
            case "Arrow":stroke(tint,2.3f);line(c,9,24,38,24);line(c,26,12,38,24,26,36);break;
            case "Chevron":stroke(tint,2.5f);line(c,19,12,30,24,19,36);break;
            case "Check":stroke(tint,3.3f);line(c,13,24,21,32,35,16);break;
            case "Energy":case "Allergies":fill(Color.rgb(255,209,140));q=new Path();q.moveTo(26,2);q.lineTo(9,26);q.lineTo(22,26);q.lineTo(18,45);q.lineTo(39,19);q.lineTo(26,19);q.close();path(c,q);break;
            case "Food":stroke(pink,3);line(c,13,5,13,42);line(c,7,5,7,17,19,17,19,5);line(c,34,5,34,42);c.drawOval(29,4,37,23,paint);break;
            case "Mood":int moodInk=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES?dark:Color.rgb(98,81,112);fill(pink);c.drawCircle(24,24,20,paint);fill(moodInk);c.drawCircle(17,19,2,paint);c.drawCircle(31,19,2,paint);c.drawArc(14,22,34,37,0,180,true,paint);break;
            case "Mic":fill(tint);c.drawRoundRect(18,3,30,30,6,6,paint);stroke(tint,3);c.drawArc(12,15,36,36,0,180,false,paint);line(c,24,36,24,44);line(c,18,44,30,44);break;
            case "Wave":stroke(tint,3);int[] heights={8,18,30,20,10};for(int i=0;i<5;i++)c.drawLine(8+i*8,24-heights[i]/2f,8+i*8,24+heights[i]/2f,paint);break;
            case "Link":stroke(tint,3);c.rotate(40,24,24);c.drawRoundRect(16,2,32,28,8,8,paint);c.drawRoundRect(16,20,32,46,8,8,paint);break;
            case "Eye":stroke(pink,3);q=new Path();q.moveTo(3,24);q.quadTo(24,1,45,24);q.quadTo(24,47,3,24);path(c,q);c.drawCircle(24,24,7,paint);break;
            case "Share":stroke(tint,2.5f);line(c,14,17,8,17,8,43,40,43,40,17,34,17);line(c,24,30,24,3);line(c,16,11,24,3,32,11);break;
            case "QR":case "Microchip":stroke(tint,3);c.drawRect(5,5,19,19,paint);c.drawRect(29,5,43,19,paint);c.drawRect(5,29,19,43,paint);line(c,30,29,30,37,42,37,42,44);line(c,38,27,43,27);break;
            case "Edit":stroke(tint,3);line(c,10,34,8,42,17,40,40,14,34,8,10,34);line(c,29,14,35,20);break;
            case "Heart":fill(pink);q=new Path();q.moveTo(24,42);q.cubicTo(-15,15,12,-5,24,13);q.cubicTo(39,-5,63,15,24,42);path(c,q);break;
            case "Weight":stroke(tint,3);c.drawRoundRect(6,14,42,43,4,4,paint);c.drawArc(17,3,31,23,180,180,false,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(17);paint.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("kg",14,35,paint);break;
            case "Emergency":fill(pink);c.drawRoundRect(6,6,42,42,6,6,paint);stroke(Color.WHITE,4);line(c,24,15,24,33);line(c,15,24,33,24);break;
        }
        c.restore();
    }
}
