package dev.roxy.kryptonplayer.core.common

import kotlinx.coroutines.flow.MutableStateFlow

enum class MainTargetTab {
    HOME,
    SEARCH,
    EXPLORE,
}

object MediaPickerTabController {
    val targetTab = MutableStateFlow<MainTargetTab?>(null)

    fun openTab(tab: MainTargetTab) {
        targetTab.value = tab
    }
}
