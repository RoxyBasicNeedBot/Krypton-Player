package dev.anilbeesetti.nextplayer.core.common

import kotlinx.coroutines.flow.MutableStateFlow

enum class MainTargetTab {
    HOME,
    EXPLORE,
}

object MediaPickerTabController {
    val targetTab = MutableStateFlow<MainTargetTab?>(null)

    fun openTab(tab: MainTargetTab) {
        targetTab.value = tab
    }
}
