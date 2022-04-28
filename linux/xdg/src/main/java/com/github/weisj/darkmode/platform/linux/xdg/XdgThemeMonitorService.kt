// based on https://gist.github.com/DevSrSouza/b013d1a8119f50615a493b36cf0b9b56

package com.github.weisj.darkmode.platform.linux.xdg

import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.ThemeMonitorService
import org.freedesktop.dbus.interfaces.DBusSigHandler

class XdgThemeMonitorService : ThemeMonitorService, DBusSigHandler<FreedesktopInterface.SettingChanged> {
    private var eventHandler: (() -> Unit)? = null

    override val isDarkThemeEnabled: Boolean = FreedesktopInterface.theme == ThemeMode.DARK
    override val isSupported: Boolean = FreedesktopInterface.theme != ThemeMode.ERROR
    override val isHighContrastEnabled: Boolean = false  // No xdg preference for that available

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        check(eventHandler == null) { "Event handler already initialized" }

        FreedesktopInterface.addSettingChangedHandler(this)
        eventHandler = callback
        return NativePointer(0L)
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        FreedesktopInterface.removeSettingChangedHandler(this)
        eventHandler = null
    }

    override fun handle(signal: FreedesktopInterface.SettingChanged) {
        if (signal.colorSchemeChanged) {
            eventHandler?.invoke()
        }
    }
}
