package com.aprax.htmlrun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aprax.htmlrun.runner.RunnerViewModel
import com.aprax.htmlrun.ui.RunnerScreen
import com.aprax.htmlrun.ui.theme.HtmlRunnerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RunnerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HtmlRunnerTheme {
                RunnerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.persist()
    }
}