package com.skyd.anivu.model.bean

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.skyd.anivu.base.BaseBean

data class MoreBean(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val shape: Shape,
    val shapeColor: Color,
    val action: () -> Unit,
) : BaseBean
