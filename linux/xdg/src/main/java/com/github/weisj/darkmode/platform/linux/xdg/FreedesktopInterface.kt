package com.github.weisj.darkmode.platform.linux.xdg

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.freedesktop.portal.Settings")
interface FreedesktopInterface : DBusInterface {
    fun Read(namespace: String, key: String): Variant<*>

    class SettingChanged(objectpath: String, namespace: String, key: String, value: Variant<Any>) : DBusSignal(objectpath, namespace, key, value)
}
