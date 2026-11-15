package com.example.musicplayer

import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {
    @Test
    fun launchesAndConnectsToPlaybackService() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val deadline = SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(10)
            var connected = false
            var status = ""
            var error = ""

            while (!connected && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity { activity ->
                    connected = activity.findViewById<Button>(R.id.buttonPlayPause).isEnabled
                    status = activity.findViewById<TextView>(R.id.textStatus).text.toString()
                    error = activity.findViewById<TextView>(R.id.textError).text.toString()
                }
                if (!connected) SystemClock.sleep(100)
            }

            assertTrue("播放服务未连接：$status $error", connected)
        }
    }
}
