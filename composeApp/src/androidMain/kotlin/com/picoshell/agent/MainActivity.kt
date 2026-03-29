package com.picoshell.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.picoshell.ui.state.ShellPresenter
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val presenter: ShellPresenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val localModelPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                presenter.importLocalModel(uri?.toString())
            }

            App(
                presenter = presenter,
                onAddLocalModel = {
                    localModelPicker.launch(arrayOf("*/*"))
                },
            )
        }
    }
}
