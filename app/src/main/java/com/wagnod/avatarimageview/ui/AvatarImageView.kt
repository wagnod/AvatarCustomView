package com.wagnod.avatarimageview.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.Px
import androidx.core.animation.doOnRepeat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRectF
import com.wagnod.avatarimageview.R
import com.wagnod.avatarimageview.extensions.dpToPx
import kotlin.math.max
import kotlin.math.truncate

class AvatarImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_SIZE = 40
        private const val DEFAULT_BORDER_WIDTH = 2
        private const val DEFAULT_BORDER_COLOR = Color.WHITE

        // background в режими инициалов
        val bgColor = arrayOf(
            Color.parseColor("#7BC862"),
            Color.parseColor("#E17076"),
            Color.parseColor("#FAA774"),
            Color.parseColor("#6EC9CB"),
            Color.parseColor("#65AADD"),
            Color.parseColor("#A695E7"),
            Color.parseColor("#EE7AAE"),
            Color.parseColor("#2196F3")
        )
    }

    private var size = 0
    private var initials: String = "??"
    @Px
    var borderWidth: Float = context.dpToPx(DEFAULT_BORDER_WIDTH)
    @ColorInt
    private var borderColor: Int = Color.WHITE

    // наш бордер
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // размер нашей View
    private val viewRect = Rect()
    private val borderRect = Rect()

    // аватар или инициалы
    private var isAvatarMode = true

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AvatarImageView)
        try {
            initials = ta.getString(R.styleable.AvatarImageView_aiv_initials) ?: "??"
            borderWidth = ta.getDimension(
                R.styleable.AvatarImageView_aiv_borderWidth,
                context.dpToPx(DEFAULT_BORDER_WIDTH)
            )
            borderColor =
                ta.getColor(R.styleable.AvatarImageView_aiv_borderColor, DEFAULT_BORDER_COLOR)

            scaleType = ScaleType.CENTER_CROP
            setup()
        } finally {
            ta.recycle()
        }

        setOnLongClickListener {
            handleLongClick()
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

    // Будем вызывать этот метод, если находимся в avatar-моде
    private fun prepareShader(w: Int, h: Int) {
        if (w == 0 || drawable == null) {
            return
        }
        val srcBm = drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888)
        avatarPaint.shader = BitmapShader(srcBm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    // Не стоит производить всякие вычисления, особенно, свзяанные с размерами в callbackах onMeasure(),
    // так как измерение View происходит в несколько подходов, и будут вызываться в несколько раз
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val initSize = resolveDefultSize(widthMeasureSpec)
        setMeasuredDimension(max(initSize, size), max(initSize, size))
    }

    // Все изменения, связанные с размерами View, все вычисления, мы можем прекрасно производить в этом методе,
    // потому что в нём мы уже точно знаем, какие размеры имеет наше View
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("AvatarImageView", "onSizeChanged()")

        if (w == 0) {
            return
        }
        // Когда уже знаем ширину и высоту BitMap, инициализируем размеры нашего примоугольника,
        // которые ограничивает нашу View
        with(viewRect) {
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
        Log.d("AvatarImageView", "onLayout()")
    }

    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
        Log.d("AvatarImageView", "onDraw()")
        // В onDraw() не надо создавать никаких объектов. Это надо делать на более ранних этапах. Например, в setup()
        // Тут только отрисовываем объекты. Иначе будем наблюдать вибрации при изменении размеров и цвеирв.
        // Это связано с тем, что GC будет вычищать объекты, которые были аллоцированы, и это будет
        // снижать производительность нашей View

        if (drawable != null && isAvatarMode) {
            drawAvatar(canvas)
        } else {
            drawInitials(canvas)
        }

        // уменьшим размер border, а то он чёт вылазит за границы :((
        // но при лонг клике тогда будет уменьшаться каждый раз. Чтобы исправить это, созданим новый квадрат
        val half = (borderWidth / 2).toInt()
        borderRect.set(viewRect)
        borderRect.inset(half, half)
        // нарисуем овал исходной ширины поверх нашего исображения
        canvas.drawOval(borderRect.toRectF(), borderPaint)
    }

    // Чтобы текущее состояние сохранялось - нужно обязательно указать нашей View ID!!!
    override fun onSaveInstanceState(): Parcelable? {
        Log.d("AvatarImageView", "onSaveInstanceState() $id")
        val savedState = SavedState(super.onSaveInstanceState())
        savedState.isAvatarMode = isAvatarMode
        savedState.borderWidth = borderWidth
        savedState.borderColor = borderColor
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        Log.d("AvatarImageView", "onRestoreInstanceState() $id")
        if (state is SavedState) {
            super.onRestoreInstanceState(state)
            isAvatarMode = state.isAvatarMode
            borderWidth = state.borderWidth
            borderColor = state.borderColor

            with(borderPaint) {
                color = borderColor
                strokeWidth = borderWidth
            }
        } else {
            // Возвращаем родительский метод
            super.onRestoreInstanceState(state)
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        if (isAvatarMode) {
            prepareShader(width, height)
        }
        Log.d("AvatarImageView", "setImageBitmap()")
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (isAvatarMode) {
            prepareShader(width, height)
        }
        Log.d("AvatarImageView", "setImageDrawable()")
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        if (isAvatarMode) {
            prepareShader(width, height)
        }
        Log.d("AvatarImageView", "setImageResource()")
    }

    fun setInitials(initials: String) {
        this.initials = initials
        if (!isAvatarMode) {
            invalidate()
        }
    }

    fun setBorderColor(@ColorInt borderColor: Int) {
        this.borderColor = borderColor
        borderPaint.color = borderColor
        invalidate()
    }

    fun setBorderWidth(@Dimension width: Int) {
        borderWidth = context.dpToPx(width)
        // strokeWidth задает тощину обводки для фигуры
        borderPaint.strokeWidth = borderWidth
        invalidate()
    }

    private fun resolveDefultSize(spec: Int): Int {
        return when (MeasureSpec.getMode(spec)) {
            MeasureSpec.UNSPECIFIED -> {
                context.dpToPx(DEFAULT_SIZE).toInt()
            }
            else -> MeasureSpec.getSize(spec)
        }
    }

    private fun drawAvatar(canvas: Canvas) {
        canvas.drawOval(viewRect.toRectF(), avatarPaint)
    }

    private fun initialsToColor(letters: String) : Int{
        val b = letters[0].toByte()
        val len = bgColor.size
        val d = b / len.toDouble()
        val index = ((d - truncate(d)) * len).toInt()
        return bgColor[index]
    }

    private fun drawInitials(canvas: Canvas) {
        initialsPaint.color = initialsToColor(initials)
        canvas.drawOval(viewRect.toRectF(), initialsPaint)
        with(initialsPaint) {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = height * 0.33f
        }
        val offsetY = (initialsPaint.descent() + initialsPaint.ascent()) / 2
        // Сейчас отрисовка инициалов начинается с верхней части центра. Надо исправить.
        // Исправим путём опускания на половину текущего размера шрифта
        canvas.drawText(initials, viewRect.exactCenterX(), viewRect.exactCenterY() - offsetY, initialsPaint)
    }

    private fun toggleMode() {
        isAvatarMode = !isAvatarMode
        invalidate()
    }

    // При долгом клике на аватарку она должна превратиться в инициалы
    private fun handleLongClick() : Boolean {
        // анимируем изменение нашей ширины, которая будет изменяться в 2 раза
        val va = ValueAnimator.ofInt(width, 2 * width).apply {
            duration = 600
            // линейное увеличение ширины
            interpolator = LinearInterpolator()
            // чтобы не view не расширялся безгранично. как только изменит нашу ширину и высоту,
            // то начнёт возвращаться в то же положение, из которого пришёл
            repeatMode = ValueAnimator.REVERSE
            repeatCount = 1

        }
        va.addUpdateListener {
            // ещё одно свойство AvatarImageView
            size = it.animatedValue as Int
            // когда изменяем size на каждом шаге анимации, нужно это зарегистрировать
            requestLayout()
        }
        // Появился в KTX. Когда анимация начнёт повторяться == ровно середина нашей анимации
        va.doOnRepeat {
            toggleMode()
        }
        va.start()

        return true
    }

    private class SavedState : BaseSavedState, Parcelable {
        var isAvatarMode: Boolean = true
        var borderWidth: Float = 0f
        var borderColor: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        // Восстановим наше значение из парсела
        constructor(src: Parcel) : super(src) {
            isAvatarMode = src.readInt() == 1
            borderWidth = src.readFloat()
            borderColor = src.readInt()
        }

        override fun writeToParcel(dst: Parcel, flags: Int) {
            super.writeToParcel(dst, flags)
            // Записываем в Parcel
            // Порядок считывания и записи из парсела должен соответствовать друг другу
            dst.writeInt(if (isAvatarMode) 1 else 0)
            dst.writeFloat(borderWidth)
            dst.writeInt(borderColor)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel) = SavedState(parcel)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}