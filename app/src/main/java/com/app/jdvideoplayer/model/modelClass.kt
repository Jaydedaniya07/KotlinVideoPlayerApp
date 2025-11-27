package com.app.jdvideoplayer.model

import android.graphics.Bitmap
import java.io.Serializable

data class DemoClass(
    val image : Bitmap,
    val name : String,
    var isSelected : Boolean
) : Serializable