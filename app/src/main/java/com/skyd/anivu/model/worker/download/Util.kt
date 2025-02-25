package com.skyd.anivu.model.worker.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.ProxyInfo
import android.util.Log
import com.skyd.anivu.R
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.ifNullOfBlank
import com.skyd.anivu.ext.toDecodedUrl
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.anivu.model.bean.download.bt.TorrentFileBean
import com.skyd.anivu.model.preference.proxy.ProxyHostnamePreference
import com.skyd.anivu.model.preference.proxy.ProxyModePreference
import com.skyd.anivu.model.preference.proxy.ProxyPasswordPreference
import com.skyd.anivu.model.preference.proxy.ProxyPortPreference
import com.skyd.anivu.model.preference.proxy.ProxyTypePreference
import com.skyd.anivu.model.preference.proxy.ProxyUsernamePreference
import com.skyd.anivu.model.preference.proxy.UseProxyPreference
import com.skyd.anivu.model.preference.transmission.TorrentDhtBootstrapsPreference
import com.skyd.anivu.model.repository.download.bt.BtDownloadManager
import com.skyd.anivu.model.repository.download.bt.BtDownloadManagerIntent
import kotlinx.coroutines.runBlocking
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.FileStorage
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.TorrentStatus
import org.libtorrent4j.Vectors
import org.libtorrent4j.swig.add_torrent_params
import org.libtorrent4j.swig.error_code
import org.libtorrent4j.swig.libtorrent
import org.libtorrent4j.swig.settings_pack
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun isTorrentMimetype(mimetype: String?): Boolean {
    return Regex("^application(s)?/x-bittorrent$").matches(mimetype.orEmpty())
}

fun doIfMagnetOrTorrentLink(
    link: String,
    mimetype: String? = null,
    onMagnet: ((String) -> Unit)? = null,
    onTorrent: ((String) -> Unit)? = null,
    onSupported: ((String) -> Unit)? = null,
    onUnsupported: ((String) -> Unit)? = null,
) {
    ifMagnetLink(
        link = link,
        onMagnet = {
            onMagnet?.invoke(link)
            onSupported?.invoke(link)
        },
        onUnsupported = {
            if (
                isTorrentMimetype(mimetype) ||
                Regex("^(http|https)://.*\\.torrent$").matches(link)
            ) {
                onTorrent?.invoke(link)
                onSupported?.invoke(link)
            } else {
                onUnsupported?.invoke(link)
            }
        },
    )
}

fun ifMagnetLink(
    link: String,
    onMagnet: ((String) -> Unit)? = null,
    onUnsupported: ((String) -> Unit)? = null,
) {
    if (link.startsWith("magnet:")) {
        onMagnet?.invoke(link)
    } else {
        onUnsupported?.invoke(link)
    }
}

fun TorrentStatus.State.toDisplayString(context: Context): String {
    return when (this) {
        TorrentStatus.State.CHECKING_FILES -> context.getString(R.string.torrent_status_checking_files)
        TorrentStatus.State.DOWNLOADING_METADATA -> context.getString(R.string.torrent_status_downloading_metadata)
        TorrentStatus.State.DOWNLOADING -> context.getString(R.string.torrent_status_downloading)
        TorrentStatus.State.FINISHED -> context.getString(R.string.torrent_status_finished)
        TorrentStatus.State.SEEDING -> context.getString(R.string.download_seeding)
        TorrentStatus.State.CHECKING_RESUME_DATA -> context.getString(R.string.torrent_status_checking_resume_data)
        TorrentStatus.State.UNKNOWN -> ""
    }
}

internal fun initProxySettings(context: Context, settings: SettingsPack): SettingsPack {
    val dataStore = context.dataStore

    if (!dataStore.getOrDefault(UseProxyPreference)) {
        return settings
    }

    val proxyType: String
    val proxyHostname: String
    val proxyPort: Int
    val proxyUsername: String
    val proxyPassword: String
    if (dataStore.getOrDefault(ProxyModePreference) == ProxyModePreference.AUTO_MODE) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val proxyInfo: ProxyInfo? = cm.defaultProxy
        if (proxyInfo != null) {
            proxyType = ProxyTypePreference.HTTP
            proxyHostname = proxyInfo.host
            proxyPort = proxyInfo.port
            proxyUsername = ""
            proxyPassword = ""
        } else {
            // No proxy
            return settings
        }
    } else {
        proxyType = dataStore.getOrDefault(ProxyTypePreference)
        proxyHostname = dataStore.getOrDefault(ProxyHostnamePreference)
        proxyPort = dataStore.getOrDefault(ProxyPortPreference)
        proxyUsername = dataStore.getOrDefault(ProxyUsernamePreference)
        proxyPassword = dataStore.getOrDefault(ProxyPasswordPreference)
    }

    return settings.setInteger(
        settings_pack.int_types.proxy_type.swigValue(),
        toSettingsPackProxyType(proxyType).swigValue()
    ).setString(
        settings_pack.string_types.proxy_hostname.swigValue(),
        proxyHostname
    ).setInteger(
        settings_pack.int_types.proxy_port.swigValue(),
        proxyPort
    ).run {
        val dhtBootstrapNodes = dataStore.getOrDefault(TorrentDhtBootstrapsPreference)
            .joinToString(",")
        if (dhtBootstrapNodes.isNotBlank()) {
            setString(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes)
        } else {
            this
        }
    }.run {
        if (proxyUsername.isBlank() || proxyPassword.isBlank()) {
            clear(settings_pack.string_types.proxy_username.swigValue())
            clear(settings_pack.string_types.proxy_password.swigValue())
            this
        } else {
            setString(
                settings_pack.string_types.proxy_username.swigValue(),
                proxyUsername
            ).setString(
                settings_pack.string_types.proxy_password.swigValue(),
                proxyPassword
            )
        }
    }
}

internal fun toSettingsPackProxyType(proxyType: String): settings_pack.proxy_type_t {
    return when (proxyType) {
        ProxyTypePreference.HTTP -> return settings_pack.proxy_type_t.http
        ProxyTypePreference.SOCKS4 -> return settings_pack.proxy_type_t.socks4
        ProxyTypePreference.SOCKS5 -> return settings_pack.proxy_type_t.socks5
        else -> settings_pack.proxy_type_t.http
    }
}

internal fun getWhatPausedState(oldState: BtDownloadInfoBean.DownloadState?) =
    when (oldState) {
        BtDownloadInfoBean.DownloadState.Seeding,
        BtDownloadInfoBean.DownloadState.Completed,
        BtDownloadInfoBean.DownloadState.SeedingPaused -> {
            BtDownloadInfoBean.DownloadState.SeedingPaused
        }

        else -> {
            BtDownloadInfoBean.DownloadState.Paused
        }
    }

internal fun updateDownloadState(
    link: String,
    downloadState: BtDownloadInfoBean.DownloadState,
) {
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadState(
            link = link,
            downloadState = downloadState,
        )
    )
}

internal fun updateDownloadStateAndSessionParams(
    link: String,
    sessionStateData: ByteArray,
    downloadState: BtDownloadInfoBean.DownloadState,
) {
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadState(
            link = link, downloadState = downloadState,
        )
    )
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateSessionParams(
            link = link, sessionStateData = sessionStateData,
        )
    )
}

internal fun updateDescriptionInfoToDb(link: String, description: String) {
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadDescription(
            link = link,
            description = description,
        )
    )
}

internal fun updateTorrentFilesToDb(
    link: String,
    savePath: String,
    files: FileStorage,
) {
    val list = mutableListOf<TorrentFileBean>()
    runCatching {
        for (i in 0..<files.numFiles()) {
            list.add(
                TorrentFileBean(
                    link = link,
                    path = File(savePath, files.filePath(i)).path,
                    size = files.fileSize(i),
                )
            )
        }
    }.onFailure { it.printStackTrace() }
    BtDownloadManager.sendIntent(BtDownloadManagerIntent.UpdateTorrentFiles(list))
}

internal fun updateNameInfoToDb(link: String, name: String?) {
    if (name.isNullOrBlank()) return
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadName(
            link = link,
            name = name,
        )
    )
}

internal fun updateProgressInfoToDb(link: String, progress: Float) {
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadProgress(
            link = link,
            progress = progress,
        )
    )
}

internal fun updateSizeInfoToDb(link: String, size: Long) {
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadSize(
            link = link,
            size = size,
        )
    )
}

/**
 * 添加新的下载信息（之前没下载过的）
 */
internal fun addNewDownloadInfoToDbIfNotExists(
    forceAdd: Boolean = false,
    link: String,
    name: String?,
    path: String,
    progress: Float,
    size: Long,
    downloadRequestId: String,
) = runBlocking {
    if (!forceAdd) {
        val video = BtDownloadManager.getDownloadInfo(link = link)
        if (video != null) return@runBlocking
    }
    BtDownloadManager.sendIntent(
        BtDownloadManagerIntent.UpdateDownloadInfo(
            BtDownloadInfoBean(
                link = link,
                name = name.ifNullOfBlank {
                    link.substringAfterLast('/')
                        .toDecodedUrl()
                        .validateFileName()
                },
                path = path,
                downloadDate = System.currentTimeMillis(),
                size = size,
                progress = progress,
                downloadRequestId = downloadRequestId,
            )
        )
    )
}

fun serializeResumeData(name: String, params: AddTorrentParams) {
    val resume = File(Const.TORRENT_RESUME_DATA_DIR, name)
    if (!resume.exists()) resume.createNewFile()
    val data = libtorrent.write_resume_data(params.swig()).bencode()
    try {
        FileOutputStream(resume).use { it.write(Vectors.byte_vector2bytes(data)) }
    } catch (e: IOException) {
        Log.e("serializeResumeData", "Error saving resume data")
    }
}

fun readResumeData(name: String): add_torrent_params? {
    val resume = File(Const.TORRENT_RESUME_DATA_DIR, name)
    if (!resume.exists()) return null
    try {
        val data = resume.readBytes()
        val ec = error_code()
        val p: add_torrent_params =
            libtorrent.read_resume_data_ex(Vectors.bytes2byte_vector(data), ec)
        require(ec.value() == 0) { "Unable to read the resume data: " + ec.message() }
        return p
    } catch (e: Throwable) {
        Log.w("readResumeData", "Unable to set resume data: $e")
    }
    return null
}