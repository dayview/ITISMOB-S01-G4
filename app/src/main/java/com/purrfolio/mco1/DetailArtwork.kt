package com.purrfolio.mco1

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

/** Pastel decoration behind the native ID content; never a flattened screen. */
class PetIdBackground(private val horizontal:Boolean):android.graphics.drawable.Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c:Canvas){
        val w=bounds.width().toFloat();val h=bounds.height().toFloat()
        val rect=RectF(bounds);val clip=Path().apply{addRoundRect(rect,26f*w/365f,26f*w/365f,Path.Direction.CW)}
        c.save();c.clipPath(clip)
        p.shader=LinearGradient(0f,0f,w,h,intArrayOf(Color.parseColor("#FFF0F5"),Color.parseColor("#E2C3FA")),null,Shader.TileMode.CLAMP);c.drawRect(rect,p);p.shader=null
        p.color=Color.parseColor("#DCC0FA")
        c.drawCircle(w*.98f,h*.02f,w*.17f,p);c.drawCircle(-w*.06f,h*.57f,w*.18f,p);c.drawCircle(w*.99f,h*.95f,w*.12f,p)
        fun heart(x:Float,y:Float,s:Float){c.save();c.translate(x,y);c.scale(s,s);p.color=Color.parseColor("#F3ADD3");val path=Path();path.moveTo(0f,1f);path.cubicTo(-2f,-.3f,-.9f,-1.6f,0f,-.6f);path.cubicTo(.9f,-1.6f,2f,-.3f,0f,1f);c.drawPath(path,p);c.restore()}
        fun paw(x:Float,y:Float,s:Float){p.color=Color.parseColor("#BF91E9");c.drawOval(x-s*.6f,y,x+s*.6f,y+s*.8f,p);for(i in 0..3){val angle=Math.PI+(i+1)*Math.PI/5;c.drawCircle(x+cos(angle).toFloat()*s,y+sin(angle).toFloat()*s,s*.28f,p)}}
        if(horizontal){heart(w*.49f,h*.36f,w*.025f);paw(w*.94f,h*.89f,w*.028f)}else{heart(w*.12f,h*.10f,w*.035f);heart(w*.87f,h*.58f,w*.03f);paw(w*.86f,h*.31f,w*.055f);paw(w*.12f,h*.91f,w*.04f)}
        c.restore()
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:ColorFilter?){p.colorFilter=filter}
    @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

/** Rounded line icons for forms, account settings, records and sharing. */
class DetailIcon(context: Context, private val kind: String, private val tint: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth=1.7f; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND
    }
    override fun onDraw(c: Canvas) {
        val scale=min(width,height)/24f
        c.save();c.translate((width-24*scale)/2,(height-24*scale)/2);c.scale(scale,scale)
        paint.color=tint;paint.style=Paint.Style.STROKE
        fun line(vararg points:Float){val p=Path();p.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)p.lineTo(points[i],points[i+1]);c.drawPath(p,paint)}
        fun circle(x:Float,y:Float,r:Float)=c.drawCircle(x,y,r,paint)
        fun box(l:Float,t:Float,r:Float,b:Float,rad:Float=2f)=c.drawRoundRect(l,t,r,b,rad,rad,paint)
        when(kind){
            "Sparkle"->{line(12f,1f,15f,9f,23f,12f,15f,15f,12f,23f,9f,15f,1f,12f,9f,9f,12f,1f)}
            "Progress"->{paint.strokeWidth=3.4f;paint.color=Color.parseColor("#443C55");circle(12f,12f,9f);listOf("#D8B4F8","#91E5C8","#FFD18C").forEachIndexed{i,hex->paint.color=Color.parseColor(hex);c.drawArc(3f,3f,21f,21f,-90f+i*96f,92f,false,paint)}}
            "Person"->{circle(12f,6f,4f);val p=Path();p.moveTo(4f,21f);p.lineTo(4f,18f);p.cubicTo(4f,11f,20f,11f,20f,18f);p.lineTo(20f,21f);p.close();c.drawPath(p,paint)}
            "Mail"->{box(2f,5f,22f,20f);line(3f,6f,12f,13f,21f,6f)}
            "Lock"->{box(5f,10f,19f,22f);c.drawArc(8f,1f,16f,15f,180f,180f,false,paint);line(8f,6f,8f,10f);line(16f,6f,16f,10f)}
            "Camera"->{box(2f,6f,22f,21f,3f);circle(12f,13f,4f);line(7f,6f,9f,3f,15f,3f,17f,6f)}
            "Settings"->{circle(12f,12f,7f);circle(12f,12f,2.5f);for(i in 0..7){val r=i*Math.PI/4;line((12+7*cos(r)).toFloat(),(12+7*sin(r)).toFloat(),(12+10*cos(r)).toFloat(),(12+10*sin(r)).toFloat())}}
            "Bell"->{val p=Path();p.moveTo(4f,18f);p.quadTo(7f,15f,7f,8f);p.cubicTo(7f,1f,17f,1f,17f,8f);p.quadTo(17f,15f,20f,18f);p.close();c.drawPath(p,paint);c.drawArc(9f,18f,15f,23f,0f,180f,false,paint)}
            "Phone"->{val p=Path();p.moveTo(4f,2f);p.lineTo(8f,2f);p.lineTo(10f,7f);p.lineTo(7f,10f);p.quadTo(9f,15f,14f,17f);p.lineTo(17f,14f);p.lineTo(22f,17f);p.lineTo(21f,21f);p.cubicTo(10f,26f,-3f,10f,4f,2f);c.drawPath(p,paint)}
            "Location"->{val p=Path();p.moveTo(12f,23f);p.cubicTo(32f,4f,12f,-6f,5f,5f);p.cubicTo(0f,12f,9f,20f,12f,23f);c.drawPath(p,paint);circle(12f,8f,3f)}
            "Globe"->{circle(12f,12f,10f);c.drawOval(7f,2f,17f,22f,paint);line(2f,12f,22f,12f);line(4f,6f,20f,6f);line(4f,18f,20f,18f)}
            "Moon"->{val p=Path();p.moveTo(16f,2f);p.cubicTo(3f,0f,-3f,19f,12f,22f);p.cubicTo(17f,23f,21f,18f,22f,14f);p.cubicTo(10f,20f,6f,9f,16f,2f);c.drawPath(p,paint)}
            "Database"->{c.drawOval(4f,2f,20f,8f,paint);line(4f,5f,4f,19f);line(20f,5f,20f,19f);c.drawArc(4f,9f,20f,15f,0f,180f,false,paint);c.drawArc(4f,16f,20f,22f,0f,180f,false,paint)}
            "Trash"->{line(3f,5f,21f,5f);box(6f,5f,18f,22f,1f);line(9f,5f,9f,2f,15f,2f,15f,5f);line(10f,9f,10f,18f);line(14f,9f,14f,18f)}
            "Down"->line(6f,9f,12f,15f,18f,9f)
            "Clock"->{circle(12f,12f,10f);line(12f,5f,12f,12f,17f,14f)}
            "Search"->{circle(10f,10f,7f);line(15f,15f,22f,22f)}
            "Filter"->{for(y in listOf(5f,12f,19f))line(2f,y,22f,y);circle(8f,5f,2f);circle(16f,12f,2f);circle(8f,19f,2f)}
            "Document","Medical"->{line(5f,2f,15f,2f,21f,8f,21f,22f,5f,22f,5f,2f);line(15f,2f,15f,8f,21f,8f);if(kind=="Medical"){line(9f,14f,17f,14f);line(13f,10f,13f,18f)}else{line(9f,12f,17f,12f);line(9f,16f,17f,16f);line(9f,19f,14f,19f)}}
            "Folder"->{line(2f,21f,2f,4f,9f,4f,12f,8f,22f,8f,22f,21f,2f,21f)}
            "Download"->{line(12f,2f,12f,16f);line(7f,11f,12f,16f,17f,11f);line(3f,16f,3f,22f,21f,22f,21f,16f)}
            "Expand"->{line(9f,3f,3f,3f,3f,9f);line(15f,3f,21f,3f,21f,9f);line(3f,15f,3f,21f,9f,21f);line(15f,21f,21f,21f,21f,15f)}
            "Print"->{box(2f,7f,22f,18f);box(6f,2f,18f,7f,0f);box(6f,14f,18f,23f,0f)}
            "Photo"->{box(2f,2f,22f,22f);circle(8f,8f,2f);line(3f,19f,10f,12f,14f,16f,17f,12f,22f,18f)}
            "Cloud"->{val p=Path();p.moveTo(7f,18f);p.cubicTo(-5f,18f,2f,8f,7f,9f);p.cubicTo(6f,-2f,22f,-2f,20f,10f);p.cubicTo(28f,12f,22f,20f,18f,18f);c.drawPath(p,paint);line(12f,23f,12f,12f);line(8f,16f,12f,12f,16f,16f)}
            "Wifi"->{c.drawArc(1f,4f,23f,24f,220f,100f,false,paint);c.drawArc(5f,9f,19f,24f,220f,100f,false,paint);circle(12f,20f,1f);line(2f,2f,22f,23f)}
            "Star"->{val p=Path();for(i in 0..9){val t=-Math.PI/2+i*Math.PI/5;val r=if(i%2==0)10.0 else 4.5;val x=(12+r*cos(t)).toFloat();val y=(12+r*sin(t)).toFloat();if(i==0)p.moveTo(x,y)else p.lineTo(x,y)};p.close();c.drawPath(p,paint)}
            "Megaphone"->{line(2f,9f,9f,9f,21f,3f,21f,20f,9f,15f,2f,15f,2f,9f);line(6f,15f,8f,22f,12f,22f,10f,15f)}
            "Play"->{paint.style=Paint.Style.FILL;val p=Path();p.moveTo(8f,4f);p.lineTo(21f,12f);p.lineTo(8f,20f);p.close();c.drawPath(p,paint)}
            "Stop"->{paint.style=Paint.Style.FILL;box(5f,5f,19f,19f)}
            "Logout"->{line(12f,3f,4f,3f,4f,21f,12f,21f);line(9f,12f,22f,12f);line(17f,7f,22f,12f,17f,17f)}
            "Sex"->{circle(12f,8f,6f);line(12f,14f,12f,23f);line(8f,19f,16f,19f)}
            "Cat"->{val p=Path();p.moveTo(3f,3f);p.lineTo(9f,7f);p.lineTo(16f,7f);p.lineTo(22f,3f);p.lineTo(21f,16f);p.cubicTo(20f,24f,4f,24f,3f,16f);p.close();c.drawPath(p,paint);circle(8f,13f,.6f);circle(16f,13f,.6f);line(11f,17f,13f,17f)}
            else->{circle(12f,12f,9f);line(12f,6f,12f,13f);circle(12f,18f,.6f)}
        }
        c.restore()
    }
}

class Waveform(context:Context, private val tint:Int, private val circular:Boolean=false):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas){
        val centerX=width/2f;val centerY=height/2f
        p.color=tint;p.strokeCap=Paint.Cap.ROUND;p.strokeWidth=width/100f
        if(circular){p.style=Paint.Style.STROKE;p.strokeWidth=3*resources.displayMetrics.density;val r=min(width,height)*.42f;p.alpha=85;c.drawCircle(centerX,centerY,r,p);p.alpha=255;c.drawArc(centerX-r+10,centerY-r+10,centerX+r-10,centerY+r-10,135f,290f,false,p)}
        p.style=Paint.Style.FILL
        val count=if(circular)5 else 45;val span=width*(if(circular).24f else .92f);val step=span/count
        for(i in 0 until count){val amplitude=if(circular)listOf(.25,.5,.8,.5,.25)[i].toFloat() else (.2+abs(sin(i*.63))*abs(sin(i*.15))* .75).toFloat();val h=height*(if(circular).3f else .85f)*amplitude;val x=centerX-span/2+i*step;c.drawLine(x,centerY-h/2,x,centerY+h/2,p)}
    }
}

/** Deliberately non-scannable sample, visibly labelled by its containing card. */
class DemoQr(context:Context):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas){val s=min(width,height)/29f;c.save();c.translate((width-29*s)/2,(height-29*s)/2);c.scale(s,s);p.color=Color.WHITE;c.drawRect(0f,0f,29f,29f,p);p.color=Color.BLACK
        fun finder(x:Int,y:Int){c.drawRect(x.toFloat(),y.toFloat(),x+7f,y+7f,p);p.color=Color.WHITE;c.drawRect(x+1f,y+1f,x+6f,y+6f,p);p.color=Color.BLACK;c.drawRect(x+2f,y+2f,x+5f,y+5f,p)}
        for(y in 2..26)for(x in 2..26){if((x*17+y*31+x*y)%7<3)c.drawRect(x.toFloat(),y.toFloat(),x+1f,y+1f,p)}
        p.color=Color.WHITE;c.drawRect(1f,1f,10f,10f,p);c.drawRect(19f,1f,28f,10f,p);c.drawRect(1f,19f,10f,28f,p);p.color=Color.BLACK;finder(2,2);finder(20,2);finder(2,20)
        p.color=Color.WHITE;c.drawRect(10f,10f,20f,20f,p);p.color=Color.rgb(149,91,195);p.textSize=3.7f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText("DEMO",10f,16f,p);c.restore()
    }
}
