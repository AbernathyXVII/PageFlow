package com.gabriele.notionlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.NotebookRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.platform.PlatformContext
import kotlin.reflect.KClass

/**
 * Factory manuale per i ViewModel. Niente Hilt/Koin di proposito: per
 * un progetto di questa dimensione una factory scritta a mano è più
 * semplice da leggere e capire, e in futuro si può sempre migrare a un
 * framework DI se il progetto cresce molto.
 *
 * Nel codice comune dal 04/11/2026: la stessa sul telefono e sul PC. I
 * ViewModel si chiedono per classe Kotlin (`KClass`), il modo della libreria
 * multipiattaforma, che è anche quello che usa il telefono.
 */
class ViewModelFactory(private val context: PlatformContext) : ViewModelProvider.Factory {

    private val database by lazy { AppDatabase.getInstance(context) }

    /**
     * Visibile anche fuori perché qualche lavoro non appartiene a nessuna
     * schermata: all'avvio si svuota il cestino e si controlla che la
     * pagina da aprire esista ancora, prima che una schermata ci sia.
     */
    val pageRepository by lazy {
        PageRepository(database).also { repository ->
            // "Move to database" (vedi `PageRepository.imageCopier`).
            repository.imageCopier = { imageStore.copy(it) }
        }
    }
    private val databaseRepository by lazy { DatabaseRepository(database) }
    val imageStore by lazy { PageImageStore(context) }
    /** I quaderni, come quelli di OneNote: vedi `NotebookEntity`. */
    val notebookRepository by lazy { NotebookRepository(database, pageRepository) }

    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return when {
            modelClass == PageEditorViewModel::class ->
                PageEditorViewModel(pageRepository, imageStore) as T

            modelClass == DatabaseViewModel::class ->
                DatabaseViewModel(databaseRepository, pageRepository) as T

            modelClass == SearchViewModel::class ->
                SearchViewModel(pageRepository) as T

            modelClass == SidebarViewModel::class ->
                SidebarViewModel(pageRepository) as T

            modelClass == PagePickerViewModel::class ->
                PagePickerViewModel(pageRepository) as T

            modelClass == FavoritesViewModel::class ->
                FavoritesViewModel(pageRepository) as T

            modelClass == TrashViewModel::class ->
                TrashViewModel(pageRepository, imageStore) as T

            modelClass == StartupViewModel::class ->
                StartupViewModel(pageRepository) as T

            modelClass == TemplatesViewModel::class ->
                TemplatesViewModel(pageRepository) as T

            modelClass == InkViewModel::class ->
                InkViewModel(database) as T

            modelClass == NotebooksViewModel::class ->
                NotebooksViewModel(notebookRepository, imageStore) as T

            modelClass == NotebookSectionViewModel::class ->
                NotebookSectionViewModel(notebookRepository, imageStore) as T

            else -> throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.simpleName}")
        }
    }
}
