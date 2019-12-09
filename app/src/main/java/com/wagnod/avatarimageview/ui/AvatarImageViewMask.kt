package com.wagnod.avatarimageview.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRectF
import com.wagnod.avatarimageview.R
import com.wagnod.avatarimageview.extensions.dpToPx

class AvatarImageViewMask @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_SIZE = 40
        private const val DEFAULT_BORDER_WIDTH = 2
        private const val DEFAULT_BORDER_COLOR = Color.WHITE
    }

    private var initials: String = "??"
    @Px
    var borderWidth: Float = context.dpToPx(DEFAULT_BORDER_WIDTH)
    @ColorInt
    private var borderColor: Int = Color.WHITE

    // сглаживание диагональных линий, рисуемых объектом
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // наш бордер
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // размер нашей View
    private val viewRect = Rect()

    private lateinit var resultBm: Bitmap
    private lateinit var maskBm: Bitmap
    private lateinit var srcBm: Bitmap

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarImageViewMask)
        try {
            initials = ta.getString(R.styleable.AvatarImageViewMask_aivm_initials) ?: "??"
            borderWidth = ta.getDimension(
                R.styleable.AvatarImageViewMask_aivm_borderWidth,
                context.dpToPx(DEFAULT_BORDER_WIDTH)
            )
            borderColor = ta.getColor(R.styleable.AvatarImageViewMask_aivm_borderColor, DEFAULT_BORDER_COLOR)

            scaleType = ScaleType.CENTER_CROP
            setup()
        } finally {
            ta.recycle()
        }
    }

    private fun setup() {
        with(maskPaint) {
            color = Color.RED
            // Чтобы занимал полностью всё при отрисовке
            style = Paint.Style.FILL
        }
        with(borderPaint) {
            // чисто обводка
            style = Paint.Style.STROKE
            // то, что определили в init()
            color = borderColor
            strokeWidth = borderWidth
        }
    }

    private fun prepareBitmaps(w: Int, h: Int) {
        // ARGB_888 - Способна принимать RGB + Alpha-канал (все цвета + прозрачность). Но т.к. хранит много,
        // места в памяти занимает тоже много
        // ALPHA_8 - занимаем меньше места в памяти => производительность лучше. Принимает только альфа-канал,
        // поверх маски мы будем накладывать изображение, и нам нет необходимости учитывать все остальные цвета,
        // которые есть в этой BitMap
        maskBm = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        resultBm = maskBm.copy(Bitmap.Config.ARGB_8888, true)

        val maskCanvas = Canvas(maskBm)
        // Отрисуем наш круг
        maskCanvas.drawOval(viewRect.toRectF(), maskPaint)

        // Наложение одной битмапы на другую. Берём пересечение
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // Получаем исходный BitMap, корвертим нашу картинку в BitMap
        srcBm = drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888)

        // Штука, которая будет складывать наши BitMap
        val resultCanvas = Canvas(resultBm)

        // На результирующей отрисуем сначала Bitmap маски, затем Bitmap источника
        resultCanvas.drawBitmap(maskBm, viewRect, viewRect, null)
        // Нужно быть внимательным и следить, что maskPaint стоит вместо null, иначе получим исходную картинку
        resultCanvas.drawBitmap(srcBm, viewRect, viewRect, maskPaint)
    }

    // Не стоит производить всякие вычисления, особенно, свзяанные с размерами в callbackах onMeasure(),
    // так как измерение View происходит в несколько подходов, и будут вызываться в несколько раз
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        Log.d("AvatarImageViewMask", """
//          onMeasure()
//          width: ${MeasureSpec.toString(widthMeasureSpec)}
//          height: ${MeasureSpec.toString(heightMeasureSpec)}
//        """.trimIndent())

        val initSize = resolveDefaultSize(widthMeasureSpec)
        setMeasuredDimension(initSize, initSize)
//        Log.d("AvatarImageViewMask", "onMeasure() after set size: $measuredWidth $measuredHeight")
    }

    // Все изменения, связанные с размерами View, все вычисления, мы можем прекрасно производить в этом методе,
    // потому что в нём мы уже точно знаем, какие размеры имеет наше View
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("AvatarImageViewMask", "onSizeChanged()")

        if (w == 0) {
            return
        }
        // Когда уже знаем ширину и высоту BitMap, инициализируем размеры нашего примоугольника,
        // которые ограничивает нашу View
        with (viewRect) {
            top = 0
            left = 0
            right = w
            bottom = h
        }
        prepareBitmaps(w, h)
    }

    // Не нужен, т.к. у нашей вьюшки не будет детей
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.d("AvatarImageViewMask", "onLayout()")
    }

    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
        Log.d("AvatarImageViewMask", "onDraw()")
        // В onDraw() не надо создавать никаких объектов. Это надо делать на более ранних этапах. Например, в setup()
        // Тут только отрисовываем объекты. Иначе будем наблюдать вибрации при изменении размеров и цвеирв.
        // Это связано с тем, что GC будет вычищать объекты, которые были аллоцированы, и это будет
        // снижать производительность нашей View

        // 2 аргумент - исходный размер
        // 3 аргумент - конечный размер
        canvas.drawBitmap(resultBm, viewRect, viewRect, null)
        // уменьшим размер border, а то он чёт вылазит за границы :((
        val half = (borderWidth / 2).toInt()
        viewRect.inset(half, half)
        // нарисуем овал исходной ширины поверх нашего исображения
        canvas.drawOval(viewRect.toRectF(), borderPaint)
    }

    private fun resolveDefaultSize(spec: Int): Int {
        return when (MeasureSpec.getMode(spec)) {
            MeasureSpec.UNSPECIFIED -> {
                context.dpToPx(DEFAULT_SIZE).toInt()
            }
            else -> MeasureSpec.getSize(spec)
        }
    }
}