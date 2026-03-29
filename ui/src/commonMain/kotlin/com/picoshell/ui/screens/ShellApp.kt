package com.picoshell.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.picoshell.ui.state.ShellPresenter
import com.picoshell.ui.theme.PicoShellTheme

private enum class ShellTab(val title: String) {
    Dashboard("Dashboard"),
    Models("Models"),
    Services("Services"),
}

@Composable
fun ShellApp(
    presenter: ShellPresenter,
    onAddLocalModel: () -> Unit,
) {
    PicoShellTheme {
        val state by presenter.uiState.collectAsState()
        var selectedTab by remember { mutableStateOf(ShellTab.Dashboard) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                    ShellTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) },
                        )
                    }
                }

                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                ) { tab ->
                    when (tab) {
                        ShellTab.Dashboard -> DashboardScreen(
                            state = state,
                            presenter = presenter,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ShellTab.Models -> ModelsScreen(
                            state = state,
                            presenter = presenter,
                            onAddLocalModel = onAddLocalModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ShellTab.Services -> ServicesScreen(
                            state = state,
                            presenter = presenter,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
