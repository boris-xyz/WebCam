package com.kimdaq.livewebcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kimdaq.livewebcam.ui.theme.LiveWebCamTheme

class MainActivity : ComponentActivity() {

    private val webCam by lazy {
        WebCam.Builder()
            .setDriverId(0)
            .setPortNum(0)
            .setUsbManager(this)
            .build()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveWebCamTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LazyColumn {
                        controller(
                            onStartClick = {
                                webCam.connectUsb(this@MainActivity)
                            },
                            onEndClick = {
                                webCam.disconnect()
                            },
                        )
                        usbDevices()
                    }
                }
            }
        }
    }
}

fun LazyListScope.controller(
    modifier: Modifier = Modifier,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    item {
        Row(modifier = modifier) {
            Text(
                modifier = Modifier.clickable {
                    onEndClick()
                },
                text = stringResource(id = R.string.end),
                color = Color.Red,
            )
            Text(
                modifier = Modifier.clickable {
                    onStartClick()
                },
                text = stringResource(id = R.string.start),
                color = Color.Blue,
            )
        }
    }
}

fun LazyListScope.usbDevices(modifier: Modifier = Modifier) {
    item {
    }
}
