package com.wagnod.avatarimageview.extensions

import android.content.Context

fun Context.dpToPx(dp: Int) : Float {
    return dp.toFloat() * this.resources.displayMetrics.density
}