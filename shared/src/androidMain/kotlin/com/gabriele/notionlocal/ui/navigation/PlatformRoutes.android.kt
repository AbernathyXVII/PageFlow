package com.gabriele.notionlocal.ui.navigation

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.ui.screen.BackupScreen
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

@Composable
actual fun SettingsRoute(factory: ViewModelFactory, onBack: () -> Unit, onOpenPatchNotes: () -> Unit, onOpenStartup: () -> Unit) =
    SettingsScreen(factory = factory, onBack = onBack, onOpenPatchNotes = onOpenPatchNotes, onOpenStartup = onOpenStartup)

@Composable
actual fun BackupRoute(onBack: () -> Unit) = BackupScreen(onBack = onBack)
