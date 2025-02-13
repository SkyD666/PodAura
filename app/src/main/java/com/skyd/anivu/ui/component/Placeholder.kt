package com.skyd.anivu.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R

@Composable
fun EmptyPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    AnimatedPlaceholder(
        modifier = modifier,
        contentPadding = contentPadding,
        resId = R.raw.lottie_empty_1,
        tip = stringResource(id = R.string.empty_tip_1),
    )
}

@Composable
fun ErrorPlaceholder(
    text: String,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    AnimatedPlaceholder(
        modifier = modifier,
        contentPadding = contentPadding,
        resId = R.raw.lottie_error_1,
        tip = text,
        maxLines = 4,
    )
}

@Composable
fun AnimatedPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    @androidx.annotation.RawRes resId: Int,
    tip: String,
    maxLines: Int = Int.MAX_VALUE,
) {
    WithTextPlaceholder(
        modifier = modifier,
        contentPadding = contentPadding,
        text = tip,
        maxLines = maxLines,
    ) {
        PodAuraLottieAnimation(resId = resId)
    }
}

@Composable
fun WithTextPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    content: @Composable BoxScope.() -> Unit,
) {
    BasePlaceholder(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.sizeIn(maxHeight = 240.dp, maxWidth = 220.dp),
                contentAlignment = Alignment.Center,
                content = content,
            )
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = text,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
fun CircularProgressPlaceholder(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    BasePlaceholder(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun BasePlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}