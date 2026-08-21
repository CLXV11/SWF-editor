package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainAppScaffold
import com.example.ui.viewmodel.SwfEditorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SwfEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent if SWF file was opened directly with this app
        intent?.data?.let { uri ->
            viewModel.openSwfFromUri(uri, this)
        }

        setContent {
            MainAppScaffold(viewModel = viewModel)
        }
    }
}
