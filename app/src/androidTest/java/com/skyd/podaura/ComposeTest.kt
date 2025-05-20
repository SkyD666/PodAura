package com.skyd.podaura

import android.util.Log
import androidx.core.graphics.contains
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeTest {
    private lateinit var device: UiDevice

    @Before
    fun startMainActivity() {
        device = UiDevice.getInstance(getInstrumentation())
    }

    val pkg = "com.skyd.anivu"

    @Test
    fun test1() {
        repeat(1) {
            device.executeShellCommand("am start -n $pkg/com.skyd.podaura.ui.activity.MainActivity")
            each()
        }
    }

    fun each() {
        assert(device.wait(Until.hasObject(By.res("FeedLazyColumn")), 5_000))
        val feedLazyColumn = device.findObject(By.res("FeedLazyColumn"))
        Log.e("TAG", "before scroll")
        feedLazyColumn.scroll(Direction.DOWN, 2.6f)
        Log.e("TAG", "before sleep")
        val feeds = feedLazyColumn.findObjects(By.res("FeedItem"))
        var feed: UiObject2
        Log.e("TAG", "before do")
        do {
            feed = feeds.random()
            Log.e("TAG", "each: ${feedLazyColumn.visibleBounds} - ${feed.visibleCenter}")
        } while (!feedLazyColumn.visibleBounds.contains(feed.visibleCenter))
        feed.click()

        assert(device.wait(Until.hasObject(By.res("ArticleLazyVerticalGrid")), 5_000))
        val articleLazyVerticalGrid = device.findObject(By.res("ArticleLazyVerticalGrid"))
        articleLazyVerticalGrid.scroll(Direction.DOWN, 6f)
        articleLazyVerticalGrid.findObjects(By.res("ArticleItem")).random().click()

        assert(device.wait(Until.hasObject(By.res("ReadColumn")), 5_000))
        val readColumn = device.findObject(By.res("ReadColumn"))
        readColumn.scroll(Direction.DOWN, 6f)
    }
}