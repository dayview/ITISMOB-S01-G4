package com.purrfolio.mco1

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

/** Original vector artwork shared by care controls; never font/emoji glyphs. */
class CareArtwork @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var kind: String = ""
    private var tint: Int = Color.rgb(216,180,248)
    constructor(context: Context, kind: String, tint: Int = Color.rgb(216,180,248)) : this(context) {
        this.kind = kind
        this.tint = tint
    }
    fun setArtwork(kind: String, tint: Int) {
        this.kind = kind
        this.tint = tint
        invalidate()
    }
    init {
        if (attrs != null) context.obtainStyledAttributes(attrs, R.styleable.CareArtwork).use { values ->
            kind = values.getString(R.styleable.CareArtwork_artKind).orEmpty()
            tint = values.getColor(R.styleable.CareArtwork_artTint, tint)
        }
    }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lavender = Color.rgb(216,180,248)
    private val pink = Color.rgb(243,182,215)
    private val peach = Color.rgb(255,205,172)
    private val dark = Color.rgb(30,27,40)
    private fun fill(c: Canvas, color: Int, block: () -> Unit) { p.color=color;p.style=Paint.Style.FILL;block() }
    private fun stroke(color: Int, w: Float=3f) { p.color=color;p.style=Paint.Style.STROKE;p.strokeWidth=w;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND }
    private fun path(c: Canvas, vararg xy: Float) { val q=Path();q.moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)q.lineTo(xy[i],xy[i+1]);c.drawPath(q,p) }
    override fun onDraw(c: Canvas) {
        super.onDraw(c);c.save();val s=min(width,height)/48f;c.translate((width-48*s)/2,(height-48*s)/2);c.scale(s,s)
        when(kind) {
            "Google" -> {stroke(Color.rgb(66,133,244),6f);c.drawArc(7f,7f,41f,41f,-40f,85f,false,p);p.color=Color.rgb(52,168,83);c.drawArc(7f,7f,41f,41f,45f,90f,false,p);p.color=Color.rgb(251,188,5);c.drawArc(7f,7f,41f,41f,135f,65f,false,p);p.color=Color.rgb(234,67,53);c.drawArc(7f,7f,41f,41f,200f,85f,false,p);p.color=Color.rgb(66,133,244);c.drawLine(25f,24f,41f,24f,p);c.drawLine(41f,24f,40f,31f,p)}
            "Facebook" -> {fill(c,Color.rgb(80,133,239)){c.drawCircle(24f,24f,20f,p)};fill(c,Color.rgb(24,25,35)){val q=androidx.core.graphics.PathParser.createPathFromPathData("M27,43 V27 H33 L34,20 H27 V16 Q27,12 31,12 H35 V6 H30 Q20,6 20,16 V20 H15 V27 H20 V43 Z");c.drawPath(q!!,p)}}
            "Apple" -> {fill(c,tint){val q=androidx.core.graphics.PathParser.createPathFromPathData("M25,12 C24,6 29,2 34,2 C34,7 30,12 25,12 M25,15 C16,9 7,16 9,27 C10,35 16,44 20,43 C24,40 26,40 30,43 C34,44 39,36 41,31 C32,27 32,18 39,15 C34,10 29,13 25,15 Z");c.drawPath(q!!,p)}}
            "Feeding" -> { fill(c,lavender){val q=Path();q.moveTo(11f,15f);q.lineTo(5f,36f);q.cubicTo(5f,44f,43f,44f,43f,36f);q.lineTo(37f,15f);q.close();c.drawPath(q,p)};stroke(dark,2f);c.drawOval(10f,10f,38f,23f,p);fill(c,peach){c.drawOval(10f,9f,38f,20f,p)} }
            "Medication" -> {c.rotate(40f,24f,24f);fill(c,lavender){c.drawRoundRect(14f,3f,34f,45f,10f,10f,p)};c.save();c.clipRect(10f,0f,38f,24f);fill(c,pink){c.drawRoundRect(14f,3f,34f,45f,10f,10f,p)};c.restore();stroke(dark,1.5f);c.drawLine(14f,24f,34f,24f,p)}
            "Water" -> {stroke(Color.rgb(155,203,255),3.5f);val q=Path();q.moveTo(24f,5f);q.cubicTo(18f,15f,11f,24f,12f,32f);q.cubicTo(13f,47f,36f,47f,36f,32f);q.cubicTo(36f,24f,29f,13f,24f,5f);c.drawPath(q,p)}
            "Playtime" -> {fill(c,pink){c.drawCircle(24f,24f,20f,p)};stroke(dark,2.8f);c.drawArc(9f,10f,40f,42f,178f,147f,false,p);val q=Path();q.moveTo(17f,7f);q.cubicTo(12f,22f,28f,24f,25f,43f);q.moveTo(31f,7f);q.cubicTo(15f,18f,23f,31f,39f,30f);c.drawPath(q,p)}
            "Vet Visit" -> {stroke(lavender,3.5f);path(c,10f,6f,10f,19f);c.drawArc(10f,9f,28f,30f,0f,180f,false,p);path(c,28f,19f,28f,6f);path(c,7f,6f,13f,6f);path(c,25f,6f,31f,6f);val q=Path();q.moveTo(19f,30f);q.lineTo(19f,34f);q.cubicTo(19f,46f,38f,47f,38f,32f);c.drawPath(q,p);c.drawCircle(38f,28f,4f,p)}
            "Grooming" -> {c.rotate(40f,24f,24f);fill(c,lavender){c.drawRoundRect(14f,4f,31f,28f,3f,3f,p);c.drawRoundRect(19f,25f,27f,46f,4f,4f,p)};stroke(dark,1.5f);for(i in 0..5)c.drawLine(24f,8f+i*3f,32f,8f+i*3f,p)}
            "Home" -> {stroke(tint,2.5f);path(c,5f,23f,24f,7f,43f,23f);path(c,11f,21f,11f,42f,20f,42f,20f,29f,29f,29f,29f,42f,37f,42f,37f,21f)}
            "Pets", "Vaccinations" -> {stroke(tint,2.6f);val q=Path();q.moveTo(14f,25f);q.cubicTo(1f,35f,8f,46f,20f,39f);q.cubicTo(26f,36f,32f,46f,40f,38f);q.cubicTo(47f,30f,30f,16f,25f,20f);q.cubicTo(21f,20f,18f,23f,14f,25f);c.drawPath(q,p);c.drawOval(5f,13f,14f,24f,p);c.drawOval(15f,4f,23f,16f,p);c.drawOval(27f,4f,35f,16f,p);c.drawOval(37f,13f,45f,24f,p)}
            "Care", "Notes" -> {stroke(tint,2.5f);c.drawRoundRect(9f,9f,39f,42f,5f,5f,p);c.drawLine(16f,5f,16f,14f,p);c.drawLine(32f,5f,32f,14f,p);path(c,16f,24f,22f,24f);path(c,28f,24f,32f,24f);path(c,16f,33f,28f,33f)}
            "More" -> fill(c,tint){for(x in listOf(9f,24f,39f))c.drawCircle(x,24f,2.5f,p)}
            "Plus" -> {stroke(tint,2.6f);path(c,24f,9f,24f,39f);path(c,9f,24f,39f,24f)}
            "Back" -> {stroke(tint,2.5f);path(c,29f,10f,16f,24f,29f,38f)}
            "Close" -> {stroke(tint,2.3f);path(c,13f,13f,35f,35f);path(c,35f,13f,13f,35f)}
            "Arrow" -> {stroke(tint,2.3f);path(c,9f,24f,38f,24f);path(c,26f,12f,38f,24f,26f,36f)}
            "Chevron" -> {stroke(tint,2.5f);path(c,19f,12f,30f,24f,19f,36f)}
            "Check" -> {stroke(tint,3.3f);path(c,13f,24f,21f,32f,35f,16f)}
            "Energy", "Allergies" -> fill(c,Color.rgb(255,209,140)){val q=Path();q.moveTo(26f,2f);q.lineTo(9f,26f);q.lineTo(22f,26f);q.lineTo(18f,45f);q.lineTo(39f,19f);q.lineTo(26f,19f);q.close();c.drawPath(q,p)}
            "Food" -> {stroke(pink,3f);path(c,13f,5f,13f,42f);path(c,7f,5f,7f,17f,19f,17f,19f,5f);path(c,34f,5f,34f,42f);c.drawOval(29f,4f,37f,23f,p)}
            "Mood" -> {fill(c,pink){c.drawCircle(24f,24f,20f,p)};fill(c,dark){c.drawCircle(17f,19f,2f,p);c.drawCircle(31f,19f,2f,p);c.drawArc(14f,22f,34f,37f,0f,180f,true,p)}}
            "Mic" -> {fill(c,tint){c.drawRoundRect(18f,3f,30f,30f,6f,6f,p)};stroke(tint,3f);c.drawArc(12f,15f,36f,36f,0f,180f,false,p);path(c,24f,36f,24f,44f);path(c,18f,44f,30f,44f)}
            "Wave" -> {stroke(tint,3f);for(i in 0..4){val h=listOf(8,18,30,20,10)[i];c.drawLine(8f+i*8,24f-h/2,8f+i*8,24f+h/2,p)}}
            "Link" -> {stroke(tint,3f);c.rotate(40f,24f,24f);c.drawRoundRect(16f,2f,32f,28f,8f,8f,p);c.drawRoundRect(16f,20f,32f,46f,8f,8f,p)}
            "Eye" -> {stroke(pink,3f);val q=Path();q.moveTo(3f,24f);q.quadTo(24f,1f,45f,24f);q.quadTo(24f,47f,3f,24f);c.drawPath(q,p);c.drawCircle(24f,24f,7f,p)}
            "Share" -> {stroke(tint,2.5f);path(c,14f,17f,8f,17f,8f,43f,40f,43f,40f,17f,34f,17f);path(c,24f,30f,24f,3f);path(c,16f,11f,24f,3f,32f,11f)}
            "QR", "Microchip" -> {stroke(tint,3f);c.drawRect(5f,5f,19f,19f,p);c.drawRect(29f,5f,43f,19f,p);c.drawRect(5f,29f,19f,43f,p);path(c,30f,29f,30f,37f,42f,37f,42f,44f);path(c,38f,27f,43f,27f)}
            "Edit" -> {stroke(tint,3f);path(c,10f,34f,8f,42f,17f,40f,40f,14f,34f,8f,10f,34f);path(c,29f,14f,35f,20f)}
            "Heart" -> {fill(c,pink){val q=Path();q.moveTo(24f,42f);q.cubicTo(-15f,15f,12f,-5f,24f,13f);q.cubicTo(39f,-5f,63f,15f,24f,42f);c.drawPath(q,p)}}
            "Weight" -> {stroke(tint,3f);c.drawRoundRect(6f,14f,42f,43f,4f,4f,p);c.drawArc(17f,3f,31f,23f,180f,180f,false,p);p.style=Paint.Style.FILL;p.textSize=17f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText("kg",14f,35f,p)}
            "Emergency" -> {fill(c,pink){c.drawRoundRect(6f,6f,42f,42f,6f,6f,p)};stroke(Color.WHITE,4f);path(c,24f,15f,24f,33f);path(c,15f,24f,33f,24f)}
        };c.restore()
    }
}

/** Rounded-cap concentric arcs with a clipped photographic center and mood badges. */
class PetRings(context: Context, private val photo: Bitmap, private val light: Boolean) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val s=min(width,height)/320f;c.save();c.translate((width-320*s)/2,(height-320*s)/2);c.scale(s,s)
        val colors=intArrayOf(Color.rgb(255,209,140),Color.rgb(216,180,248),Color.rgb(243,182,215))
        for(i in 0..2){val r=listOf(136f,105f,85f)[i];p.style=Paint.Style.STROKE;p.strokeWidth=23f;p.strokeCap=Paint.Cap.ROUND;p.color=if(light)Color.rgb(232,223,237) else Color.rgb(35,32,44);c.drawCircle(160f,160f,r,p)
            p.color=colors[i];if(i==1)p.shader=LinearGradient(160f,55f,220f,265f,colors[1],Color.rgb(155,203,255),Shader.TileMode.CLAMP);c.drawArc(160-r,160-r,160+r,160+r,if(i==2)120f else -90f,if(i==2)92f else 90f+i*55f,false,p);p.shader=null
            if(i==0){p.color=Color.rgb(145,229,200);c.drawArc(24f,24f,296f,296f,120f,40f,false,p)}
        }
        c.save();val clip=Path();clip.addCircle(160f,160f,65f,Path.Direction.CW);c.clipPath(clip);p.style=Paint.Style.FILL;c.drawBitmap(photo,Rect((photo.width*.18f).toInt(),(photo.height*.22f).toInt(),(photo.width*.83f).toInt(),(photo.height*.87f).toInt()),RectF(90f,90f,230f,230f),p);c.restore()
        p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=colors[1];c.drawCircle(160f,160f,68f,p)
        for((x,y,col) in listOf(Triple(296f,160f,Color.rgb(145,229,200)),Triple(92f,278f,Color.rgb(145,229,200)),Triple(220f,244f,Color.rgb(155,203,255)))){
            p.style=Paint.Style.FILL;p.color=Color.rgb(16,16,20);c.drawCircle(x,y,17f,p);p.color=col;c.drawCircle(x,y,13f,p);p.color=Color.rgb(16,16,20);c.drawCircle(x-4,y-3,1.8f,p);c.drawCircle(x+4,y-3,1.8f,p);c.drawArc(x-7,y-1,x+7,y+9,0f,180f,true,p)
        };c.restore()
    }
}

