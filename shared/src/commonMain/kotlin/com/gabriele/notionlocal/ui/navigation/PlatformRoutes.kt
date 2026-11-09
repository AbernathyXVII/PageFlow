package com.gabriele.notionlocal.ui.navigation

import androidx.compose.runtime.Composable
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/*
 * **Le schermate che sono ancora solo del telefono** (dal 09/11/2026, per la
 * navigazione nel codice comune): sul telefono "Settings" e "Backup" di
 * sempre; sul PC, per ora, una pagina vuota con la freccia per tornare.
 */

@Composable
expect fun SettingsRoute(factory: ViewModelFactory, onBack: () -> Unit, onOpenPatchNotes: () -> Unit, onOpenStartup: () -> Unit)

@Composable
expect fun BackupRoute(onBack: () -> Unit)
