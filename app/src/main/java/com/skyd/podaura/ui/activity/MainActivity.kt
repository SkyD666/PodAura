package com.skyd.podaura.ui.activity

import android.os.Bundle
import com.skyd.podaura.ui.screen.AppEntrance


class MainActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentBase {
            AppEntrance()
        }
    }
}