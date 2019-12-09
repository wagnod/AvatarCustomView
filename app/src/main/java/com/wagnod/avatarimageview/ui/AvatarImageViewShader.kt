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

class AvatarImageViewShader @JvmOverloads constructor(
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
    private val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // наш бордер
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // размер нашей View
    private val viewRect = Rect()

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarImageViewShader)
        try {
            initials = ta.getString(R.styleable.AvatarImageViewShader_aivs_initials) ?: "??"
            borderWidth = ta.getDimension(
                R.styleable.AvatarImageViewShader_aivs_borderWidth,
                context.dpToPx(DEFAULT_BORDER_WIDTH)
            )
            borderColor = ta.getColor(R.styleable.AvatarImageViewShader_aivs_borderColor, DEFAULT_BORDER_COLOR)

            scaleType = ScaleType.CENTER_CROP
            setup()
        } finally {
            ta.recycle()
        }
    }

    private fun setup() {
        with(borderPaint) {
            // чисто обводка
            style = Paint.Style.STROKE
            // то, что определили в init()
            color = borderColor
            strokeWidth = borderWidth
        }
    }

    private fun prepareShader(w: Int, h: Int) {
        val srcBm = drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888)
        // Так же получаем исходную BitMap из drawable.
        // Первый аргумент - наша битмапа
        // Второй и третий аргумент - по х и y растягивает края картинки на все свободное пространство (CLAMP)
        avatarPaint.shader = BitmapShader(srcBm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    // Не стоит производить всякие вычисления, особенно, свзяанные с размерами в callbackах onMeasure(),
    // так как измерение View происходит в несколько подходов, и будут вызываться в несколько раз
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        Log.d("AvatarImageViewShader", """
//          onMeasure()
//          width: ${MeasureSpec.toString(widthMeasureSpec)}
//          height: ${MeasureSpec.toString(heightMeasureSpec)}
//        """.trimIndent())

        val initSize = resolveDefultSize(widthMeasureSpec)
        setMeasuredDimension(initSize, initSize)
//        Log.d("AvatarImageViewShader", "onMeasure() after set size: $measuredWidth $measuredHeight")
    }

    // Все изменения, связанные с размерами View, все вычисления, мы можем прекрасно производить в этом методе,
    // потому что в нём мы уже точно знаем, какие размеры имеет наше View
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("AvatarImageViewShader", "onSizeChanged()")

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
        prepareShader(w, h)
    }

    // Не нужен, т.к. у нашей вьюшки не будет детей
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.d("AvatarImageViewShader", "onLayout()")
    }

    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
        Log.d("AvatarImageViewShader", "onDraw()")
        // В onDraw() не надо создавать никаких объектов. Это надо делать на более ранних этапах. Например, в setup()
        // Тут только отрисовываем объекты. Иначе будем наблюдать вибрации при изменении размеров и цвеирв.
        // Это связано с тем, что GC будет вычищать объекты, которые были аллоцированы, и это будет
        // снижать производительность нашей View

        canvas.drawOval(viewRect.toRectF(), avatarPaint)
        // уменьшим размер border, а то он чёт вылазит за границы :((
        val half = (borderWidth / 2).toInt()
        viewRect.inset(half, half)
        // нарисуем овал исходной ширины поверх нашего исображения, закрасив borderPaint`ом
        canvas.drawOval(viewRect.toRectF(), borderPaint)
    }

    private fun resolveDefultSize(spec: Int): Int {
        return when (MeasureSpec.getMode(spec)) {
            MeasureSpec.UNSPECIFIED -> {
                context.dpToPx(DEFAULT_SIZE).toInt()
            }
            else -> MeasureSpec.getSize(spec)
        }
    }

}