// based on https://gist.github.com/DevSrSouza/b013d1a8119f50615a493b36cf0b9b56

package com.github.weisj.darkmode.platform.linux.xdg

import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.ThemeMonitorService
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.freedesktop.portal.Settings")
interface FreedesktopInterface : DBusInterface {
    fun Read(namespace: String, key: String): Variant <*>
}

class XdgThemeMonitorService : ThemeMonitorService {
    private val connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)
    private val freedesktopInterface : FreedesktopInterface = connection.getRemoteObject(
        "org.freedesktop.portal.Desktop",
        "/org/freedesktop/portal/desktop",
        FreedesktopInterface::class.java
    )

    private val themeMode: Number
        get() {
            val theme = freedesktopInterface.Read("org.freedesktop.appearance", "color-scheme")
            return recursiveVariantValue(theme) as Number
        }

    override val isDarkThemeEnabled: Boolean
        get() {
            return themeMode != 2
        }
    override val isHighContrastEnabled: Boolean
        get() = TODO("No xdg preference for that available")
    override val isSupported: Boolean
        get() = TODO("Not yet implemented")

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        return NativePointer(XdgNative.createEventHandler(callback))
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        XdgNative.deleteEventHandler(eventHandle.pointer)
    }

    override fun install() {
        XdgNative.init()
    }

    private fun recursiveVariantValue(variant: Variant<*>): Any {
        val value = variant.value
        return if (value !is Variant<*>) value else recursiveVariantValue(value)
    }
}
