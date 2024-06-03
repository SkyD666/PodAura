package com.skyd.anivu.ext

import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.model.preference.IgnoreUpdateVersionPreference
import com.skyd.anivu.model.preference.Settings
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.appearance.DateStylePreference
import com.skyd.anivu.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.anivu.model.preference.appearance.TextFieldStylePreference
import com.skyd.anivu.model.preference.appearance.ThemePreference
import com.skyd.anivu.model.preference.appearance.feed.FeedGroupExpandPreference
import com.skyd.anivu.model.preference.behavior.PickImageMethodPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.anivu.model.preference.behavior.article.DeduplicateTitleInDescPreference
import com.skyd.anivu.model.preference.behavior.feed.HideEmptyDefaultPreference
import com.skyd.anivu.model.preference.data.OpmlExportDirPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.anivu.model.preference.data.autodelete.UseAutoDeletePreference
import com.skyd.anivu.model.preference.player.HardwareDecodePreference
import com.skyd.anivu.model.preference.player.PlayerDoubleTapPreference
import com.skyd.anivu.model.preference.player.PlayerShow85sButtonPreference
import com.skyd.anivu.model.preference.player.PlayerShowScreenshotButtonPreference

fun Preferences.toSettings(): Settings {
    return Settings(
        // Appearance
        theme = ThemePreference.fromPreferences(this),
        darkMode = DarkModePreference.fromPreferences(this),
        feedGroupExpand = FeedGroupExpandPreference.fromPreferences(this),
        textFieldStyle = TextFieldStylePreference.fromPreferences(this),
        dateStyle = DateStylePreference.fromPreferences(this),
        navigationBarLabel = NavigationBarLabelPreference.fromPreferences(this),

        // Update
        ignoreUpdateVersion = IgnoreUpdateVersionPreference.fromPreferences(this),

        // Behavior
        deduplicateTitleInDesc = DeduplicateTitleInDescPreference.fromPreferences(this),
        articleTapAction = ArticleTapActionPreference.fromPreferences(this),
        articleSwipeLeftAction = ArticleSwipeLeftActionPreference.fromPreferences(this),
        hideEmptyDefault = HideEmptyDefaultPreference.fromPreferences(this),
        pickImageMethod = PickImageMethodPreference.fromPreferences(this),

        // Player
        playerDoubleTap = PlayerDoubleTapPreference.fromPreferences(this),
        playerShow85sButton = PlayerShow85sButtonPreference.fromPreferences(this),
        playerShowScreenshotButton = PlayerShowScreenshotButtonPreference.fromPreferences(this),
        hardwareDecode = HardwareDecodePreference.fromPreferences(this),

        // Data
        useAutoDelete = UseAutoDeletePreference.fromPreferences(this),
        autoDeleteArticleFrequency = AutoDeleteArticleFrequencyPreference.fromPreferences(this),
        autoDeleteArticleBefore = AutoDeleteArticleBeforePreference.fromPreferences(this),
        opmlExportDir = OpmlExportDirPreference.fromPreferences(this),
    )
}
