package com.example.deepseekstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.deepseekstream.di.appModules
import com.example.deepseekstream.ui.navigation.AppNavGraph
import com.example.deepseekstream.ui.theme.AppTheme
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (org.koin.core.context.GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModules)
            }
        }

        setContent {
            AppTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}
