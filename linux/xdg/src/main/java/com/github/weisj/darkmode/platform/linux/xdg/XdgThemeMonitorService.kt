// based on https://gist.github.com/DevSrSouza/b013d1a8119f50615a493b36cf0b9b56

package com.github.weisj.darkmode.platform.linux.xdg

import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.ThemeMonitorService
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.types.Variant

/**
 * Gets the active theme from the xdg-desktop portal via the color-scheme preference.
 *
 * @property themeMode Number that describes the active theme.
 * Values:
 *  0: no preference
 *  1: dark
 *  2: light
 */
class XdgThemeMonitorService : ThemeMonitorService, DBusSigHandler<FreedesktopInterface.SettingChanged> {
    private val connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)
    private val freedesktopInterface: FreedesktopInterface = connection.getRemoteObject(
        "org.freedesktop.portal.Desktop",
        "/org/freedesktop/portal/desktop",
        FreedesktopInterface::class.java
    )
    private var eventHandler: (() -> Unit)? = null

    private val themeMode: Number
        get() {
            val theme = freedesktopInterface.Read("org.freedesktop.appearance", "color-scheme")
            return (recursiveVariantValue(theme) as Number).toInt()
        }

    override val isDarkThemeEnabled: Boolean
        get() {
            return themeMode != 2
        }
    override val isHighContrastEnabled: Boolean
        get() {
            // No xdg preference for that available
            return false
        }
    override val isSupported: Boolean
        get() {
            return themeMode != 0
        }

    init {
        connection.addSigHandler(FreedesktopInterface.SettingChanged::class.java, this)
    }

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        if (eventHandler != null) {
            throw IllegalStateException("There is already an eventhandler initialized.")
        }

        eventHandler = callback
        return NativePointer(0L)
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        eventHandler = null
    }

    override fun handle(s: FreedesktopInterface.SettingChanged?) {
        eventHandler?.invoke()
    }

    private fun recursiveVariantValue(variant: Variant<*>): Any {
        val value = variant.value
        return if (value !is Variant<*>) value else recursiveVariantValue(value)
    }
}
