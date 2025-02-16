package com.example.util.simpletimetracker.core.delegates.iconSelection.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.delegates.iconSelection.viewData.IconSelectionCategoryInfoViewData
import com.example.util.simpletimetracker.core.delegates.iconSelection.viewData.IconSelectionCategoryViewData
import com.example.util.simpletimetracker.core.delegates.iconSelection.viewData.IconSelectionSelectorStateViewData
import com.example.util.simpletimetracker.core.delegates.iconSelection.viewData.IconSelectionSwitchViewData
import com.example.util.simpletimetracker.core.delegates.iconSelection.viewData.IconSelectionTypeViewData
import com.example.util.simpletimetracker.core.delegates.iconSelection.viewData.IconSelectionViewData
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.IconEmojiMapper
import com.example.util.simpletimetracker.core.mapper.IconImageMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.model.AppColor
import com.example.util.simpletimetracker.domain.model.IconImageState
import com.example.util.simpletimetracker.domain.model.IconType
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.emoji.EmojiViewData
import com.example.util.simpletimetracker.navigation.params.screen.EmojiSelectionDialogParams
import javax.inject.Inject

class IconSelectionMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val iconImageMapper: IconImageMapper,
    private val iconEmojiMapper: IconEmojiMapper,
    private val colorMapper: ColorMapper,
) {

    fun mapIconImageData(
        newColor: AppColor,
        search: String,
        isDarkTheme: Boolean,
    ): List<ViewHolderType> {
        val isSearching = search.isNotBlank()
        val actualSearch = search.lowercase().split(" ")
        val iconCategories = iconImageMapper.getAvailableImages(
            loadSearchHints = isSearching,
        )
        return iconCategories.toList().mapIndexed { index, (category, images) ->
            val categoryViewData = IconSelectionCategoryInfoViewData(
                type = IconSelectionTypeViewData.Image(category.type, index.toLong()),
                text = category.name,
                isLast = index == iconCategories.size - 1,
            )
                .let(::listOf)
                .takeIf { !isSearching } // Don't show category on search.
                .orEmpty()

            val iconsViewData = images.mapNotNull {
                if (isSearching) {
                    if (!containsSearch(actualSearch, it.iconName, it.iconSearch)) {
                        return@mapNotNull null
                    }
                }

                mapImageViewData(
                    iconName = it.iconName,
                    iconResId = it.iconResId,
                    newColor = newColor,
                    isDarkTheme = isDarkTheme,
                )
            }

            categoryViewData + iconsViewData
        }.flatten()
    }

    fun mapIconEmojiData(
        newColor: AppColor,
        search: String,
        isDarkTheme: Boolean,
    ): List<ViewHolderType> {
        val isSearching = search.isNotBlank()
        val actualSearch = search.lowercase().split(" ")
        val iconCategories = iconEmojiMapper.getAvailableEmojis(
            loadSearchHints = isSearching,
        )
        return iconCategories.toList().mapIndexed { index, (category, codes) ->
            val categoryViewData = IconSelectionCategoryInfoViewData(
                type = IconSelectionTypeViewData.Emoji(category.type, index.toLong()),
                text = category.name,
                isLast = index == iconCategories.size - 1,
            )
                .let(::listOf)
                .takeIf { !isSearching } // Don't show category on search.
                .orEmpty()

            val codesViewData = codes.mapNotNull {
                if (isSearching) {
                    if (!containsSearch(actualSearch, it.emojiSearch)) {
                        return@mapNotNull null
                    }
                }

                mapEmojiViewData(
                    codes = it.emojiCode,
                    newColor = newColor,
                    isDarkTheme = isDarkTheme,
                )
            }

            categoryViewData + codesViewData
        }.flatten()
    }

    fun mapIconImageCategories(
        selectedIndex: Long,
    ): List<ViewHolderType> {
        return iconImageMapper.getAvailableCategories().mapIndexed { index, iconImageCategory ->
            IconSelectionCategoryViewData(
                type = IconSelectionTypeViewData.Image(iconImageCategory.type, index.toLong()),
                categoryIcon = iconImageCategory.categoryIcon,
                selected = selectedIndex == index.toLong(),
            )
        }
    }

    fun mapIconEmojiCategories(
        selectedIndex: Long,
    ): List<ViewHolderType> {
        return iconEmojiMapper.getAvailableEmojiCategories().mapIndexed { index, iconEmojiCategory ->
            IconSelectionCategoryViewData(
                type = IconSelectionTypeViewData.Emoji(iconEmojiCategory.type, index.toLong()),
                categoryIcon = iconEmojiCategory.categoryIcon,
                selected = selectedIndex == index.toLong(),
            )
        }
    }

    fun mapToIconSwitchViewData(iconType: IconType): List<ViewHolderType> {
        return listOf(
            IconType.IMAGE,
            IconType.TEXT,
            IconType.EMOJI,
        ).map {
            IconSelectionSwitchViewData(
                iconType = it,
                name = mapToFilterTypeName(it),
                isSelected = it == iconType,
            )
        }
    }

    fun mapToIconSelectorViewData(
        iconImageState: IconImageState,
        iconType: IconType,
        isDarkTheme: Boolean,
    ): IconSelectionSelectorStateViewData {
        return if (iconType == IconType.TEXT) {
            IconSelectionSelectorStateViewData.None
        } else {
            val theme = if (isDarkTheme) R.style.AppThemeDark else R.style.AppTheme
            IconSelectionSelectorStateViewData.Available(
                state = iconImageState,
                searchButtonIsVisible = true,
                searchButtonColor = when (iconImageState) {
                    is IconImageState.Chooser -> R.attr.appInactiveColor
                    is IconImageState.Search -> R.attr.colorSecondary
                }.let { resourceRepo.getThemedAttr(it, theme) },
            )
        }
    }

    fun mapEmojiSelectionParams(
        color: AppColor,
        emojiCodes: String,
    ): EmojiSelectionDialogParams {
        return EmojiSelectionDialogParams(
            color = EmojiSelectionDialogParams.Color(
                colorId = color.colorId,
                colorInt = color.colorInt,
            ),
            emojiCodes = listOf(emojiCodes) + iconEmojiMapper.toSkinToneVariations(emojiCodes),
        )
    }

    private fun mapToFilterTypeName(iconType: IconType): String {
        return when (iconType) {
            IconType.IMAGE -> R.string.change_record_type_icon_image_hint
            IconType.TEXT -> R.string.change_record_type_icon_text_hint
            IconType.EMOJI -> R.string.change_record_type_icon_emoji_hint
        }.let(resourceRepo::getString)
    }

    private fun mapImageViewData(
        iconName: String,
        iconResId: Int,
        newColor: AppColor,
        isDarkTheme: Boolean,
    ): ViewHolderType {
        return IconSelectionViewData(
            iconName = iconName,
            iconResId = iconResId,
            colorInt = newColor
                .let { colorMapper.mapToColorInt(it, isDarkTheme) },
        )
    }

    private fun mapEmojiViewData(
        codes: String,
        newColor: AppColor,
        isDarkTheme: Boolean,
    ): ViewHolderType {
        return EmojiViewData(
            emojiText = iconEmojiMapper.toEmojiString(codes),
            emojiCodes = codes,
            colorInt = newColor
                .let { colorMapper.mapToColorInt(it, isDarkTheme) },
        )
    }

    // If search text is several words - all words should be found.
    private fun containsSearch(
        searchParts: List<String>,
        vararg searchableField: String,
    ): Boolean {
        return searchParts.all { part ->
            searchableField.any { it.contains(part) }
        }
    }
}