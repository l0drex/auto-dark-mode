package com.github.weisj.darkmode.platform.linux.gnome

import com.github.weisj.darkmode.platform.LibraryUtil
import com.github.weisj.darkmode.platform.settings.*
import com.google.auto.service.AutoService

@AutoService(SettingsContainerProvider::class)
class GnomeSettingsProvider : SingletonSettingsContainerProvider({ GnomeSettings }, enabled = LibraryUtil.isGnome)

data class GtkTheme(val name: String) : Comparable<GtkTheme> {
    override fun compareTo(other: GtkTheme): Int = name.compareTo(other.name)
}

object GnomeSettings : DefaultSettingsContainer() {

    /**
     * This enum holds default values for the light, dark, and high contrast GTK themes.
     * The defaults are a safe bet as they are included with almost any install of GTK.
     * Even if they're not present, the way the logic works in GnomeThemeMonitorService,
     * there won't be an issue and the plugin will fall back on the light theme.
     *
     * **See:** [Arch Wiki on GTK](https://wiki.archlinux.org/index.php/GTK#Themes)
     */
    private enum class DefaultGtkTheme(val info: GtkTheme) {
        DARK(GtkTheme("Adwaita-dark")),
        LIGHT(GtkTheme("Adwaita")),
        HIGH_CONTRAST(GtkTheme("HighContrast")),
    }

    private const val DEFAULT_GUESS_LIGHT_AND_DARK_THEMES = true

    @JvmField
    var darkGtkTheme = DefaultGtkTheme.DARK.info

    @JvmField
    var lightGtkTheme = DefaultGtkTheme.LIGHT.info

    @JvmField
    var highContrastGtkTheme = DefaultGtkTheme.HIGH_CONTRAST.info

    @JvmField
    var guessLightAndDarkThemes = DEFAULT_GUESS_LIGHT_AND_DARK_THEMES

    init {
        if (!GnomeLibrary.get().isLoaded) {
            throw IllegalStateException("Gnome library not loaded.")
        }
        group("Gnome Theme") {
            val installedThemes = GnomeThemeUtils.getInstalledThemes()
            /*
             * The default themes are added to this list. They would already be added to the list because of their
             * presence when initializing the `themes` vector in GnomeThemeUtils.cpp but because they are not
             * the same instance as the defaults, the dropdown list would default to random themes because
             * the instances of the three defaults couldn't be found in ChoiceProperty#choices.
             * For this reason, the default themes that the native code adds to this list are overwritten
             * with the instances created inside the enum constructors of DefaultGtkTheme.
             */
            val installedGtkThemes =
                mutableSetOf(DefaultGtkTheme.DARK.info, DefaultGtkTheme.LIGHT.info, DefaultGtkTheme.HIGH_CONTRAST.info)
                    .apply { addAll(installedThemes.map { GtkTheme(it) }) }
                    .toList()
                    .sorted()
            val gtkThemeRenderer = GtkTheme::name
            val gtkThemeTransformer = transformerOf(write = ::parseGtkTheme, read = ::readGtkTheme.or(""))

            persistentBooleanProperty(
                description = "Guess light/dark theme based on name",
                value = ::guessLightAndDarkThemes
            ) {
                inverted()
                control(withProperty(::lightGtkTheme), withProperty(::darkGtkTheme), withProperty(::highContrastGtkTheme))
            }

            persistentChoiceProperty(
                description = "Light GTK Theme",
                value = ::lightGtkTheme,
                transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.LIGHT.info)
            ) { choices = installedGtkThemes; renderer = gtkThemeRenderer }
            persistentChoiceProperty(
                description = "Dark GTK Theme",
                value = ::darkGtkTheme,
                transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.DARK.info)
            ) { choices = installedGtkThemes; renderer = gtkThemeRenderer }
            persistentChoiceProperty(
                description = "High Contrast GTK Theme",
                value = ::highContrastGtkTheme,
                transformer = gtkThemeTransformer.writeFallback(DefaultGtkTheme.HIGH_CONTRAST.info)
            ) { choices = installedGtkThemes; renderer = gtkThemeRenderer }
        }
    }

    private fun readGtkTheme(info: GtkTheme): String = info.name

    private fun parseGtkTheme(name: String): GtkTheme? = GtkTheme(name)
}
