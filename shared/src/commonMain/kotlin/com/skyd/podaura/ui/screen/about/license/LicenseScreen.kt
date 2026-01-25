package com.skyd.podaura.ui.screen.about.license

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.plus
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.model.bean.LicenseBean
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.license_screen_name


@Serializable
data object LicenseRoute

@Composable
fun LicenseScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                title = { Text(text = stringResource(Res.string.license_screen_name)) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) {
        val dataList = remember { getLicenseList() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(vertical = 7.dp) + it,
        ) {
            items(items = dataList) { item ->
                LicenseItem(item)
            }
        }
    }
}

@Composable
private fun LicenseItem(data: LicenseBean) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .clickable { uriHandler.safeOpenUri(data.link) }
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = data.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    modifier = Modifier.padding(start = 5.dp),
                    text = data.license,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = data.link,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getLicenseList(): List<LicenseBean> {
    return mutableListOf(
        LicenseBean(
            name = "Android Open Source Project",
            license = "Apache-2.0",
            link = "https://source.android.com/",
        ),
        LicenseBean(
            name = "Material Components for Android",
            license = "Apache-2.0",
            link = "https://github.com/material-components/material-components-android",
        ),
        LicenseBean(
            name = "Koin",
            license = "Apache-2.0",
            link = "https://github.com/InsertKoinIO/koin",
        ),
        LicenseBean(
            name = "Ktor",
            license = "Apache-2.0",
            link = "https://github.com/ktorio/ktor",
        ),
        LicenseBean(
            name = "Coil",
            license = "Apache-2.0",
            link = "https://github.com/coil-kt/coil",
        ),
        LicenseBean(
            name = "kotlinx.coroutines",
            license = "Apache-2.0",
            link = "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        LicenseBean(
            name = "kotlinx.serialization",
            license = "Apache-2.0",
            link = "https://github.com/Kotlin/kotlinx.serialization",
        ),
        LicenseBean(
            name = "MaterialKolor",
            license = "MIT",
            link = "https://github.com/jordond/MaterialKolor"
        ),
        LicenseBean(
            name = "Read You",
            license = "GPL-3.0",
            link = "https://github.com/Ashinch/ReadYou",
        ),
        LicenseBean(
            name = "Ksoup",
            license = "MIT",
            link = "https://github.com/fleeksoft/ksoup",
        ),
        LicenseBean(
            name = "mpv-android",
            license = "MIT",
            link = "https://github.com/mpv-android/mpv-android",
        ),
        LicenseBean(
            name = "Compottie",
            license = "MIT",
            link = "https://github.com/alexzhirkevich/compottie",
        ),
        LicenseBean(
            name = "Reorderable",
            license = "Apache-2.0",
            link = "https://github.com/Calvin-LL/Reorderable",
        ),
        LicenseBean(
            name = "XmlUtil",
            license = "Apache-2.0",
            link = "https://github.com/pdvrieze/xmlutil",
        ),
        LicenseBean(
            name = "Ketch",
            license = "Apache-2.0",
            link = "https://github.com/khushpanchal/Ketch",
        ),
        LicenseBean(
            name = "AtomicFU",
            license = "Apache-2.0",
            link = "https://github.com/Kotlin/kotlinx-atomicfu",
        ),
        LicenseBean(
            name = "kotlinx-datetime",
            license = "Apache-2.0",
            link = "https://github.com/Kotlin/kotlinx-datetime",
        ),
        LicenseBean(
            name = "kotlinx-io",
            license = "Apache-2.0",
            link = "https://github.com/Kotlin/kotlinx-io",
        ),
        LicenseBean(
            name = "Kermit",
            license = "Apache-2.0",
            link = "https://github.com/touchlab/Kermit",
        ),
        LicenseBean(
            name = "FileKit",
            license = "MIT",
            link = "https://github.com/vinceglb/FileKit",
        ),
        LicenseBean(
            name = "kotlin-codepoints",
            license = "MIT",
            link = "https://github.com/cketti/kotlin-codepoints",
        ),
        LicenseBean(
            name = "hash",
            license = "Apache-2.0",
            link = "https://github.com/KotlinCrypto/hash",
        ),
        LicenseBean(
            name = "HtmlAnnotator",
            license = "Apache-2.0",
            link = "https://github.com/RavenLiao/HtmlAnnotator",
        ),
        LicenseBean(
            name = "Java Native Access (JNA)",
            license = "LGPL-2.1",
            link = "https://github.com/java-native-access/jna",
        ),
    ).apply {
        if (platform == Platform.Windows || platform == Platform.Linux) {
            add(
                LicenseBean(
                    name = "prettytime",
                    license = "Apache-2.0",
                    link = "https://github.com/ocpsoft/prettytime",
                )
            )
        }
    }.sortedBy { it.name }
}