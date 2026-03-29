package com.picoshell.agent

import androidx.compose.runtime.Composable
import com.picoshell.ui.screens.ShellApp
import com.picoshell.ui.state.ShellPresenter

@Composable
fun App(
    presenter: ShellPresenter,
    onAddLocalModel: () -> Unit,
) {
    ShellApp(
        presenter = presenter,
        onAddLocalModel = onAddLocalModel,
    )
}
