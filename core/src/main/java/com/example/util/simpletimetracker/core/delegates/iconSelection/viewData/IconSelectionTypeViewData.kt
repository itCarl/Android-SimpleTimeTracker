package com.example.util.simpletimetracker.core.delegates.iconSelection.viewData

import com.example.util.simpletimetracker.domain.model.IconEmojiType
import com.example.util.simpletimetracker.domain.model.IconImageType

sealed class IconSelectionTypeViewData {
    abstract val id: Long

    data class Image(
        val type: IconImageType,
        override val id: Long,
    ) : IconSelectionTypeViewData()

    data class Emoji(
        val type: IconEmojiType,
        override val id: Long,
    ) : IconSelectionTypeViewData()
}