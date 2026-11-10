package com.gabriele.notionlocal.ui.navigation

import androidx.compose.ui.Modifier

/** Se la barra laterale si può fissare accanto alla pagina, come in Notion: sul PC sì, sul telefono no (non c'è spazio). */
expect val canPinSidebar: Boolean

/** Il cursore delle frecce a destra e a sinistra, sul bordo che ridimensiona la barra (sul telefono niente). */
expect fun Modifier.horizontalResizeCursor(): Modifier
