package com.skyd.podaura.model.bean

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon

data class MoreBean(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val shape: RoundedPolygon,
    val shapeColor: Color,
    val action: () -> Unit,
) : BaseBean