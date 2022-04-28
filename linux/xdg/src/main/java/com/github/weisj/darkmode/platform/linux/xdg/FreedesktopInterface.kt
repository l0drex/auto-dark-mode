package com.github.weisj.darkmode.platform.linux.xdg

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

enum class ThemeMode {
    LIGHT, DARK, ERROR
}

@DBusInterfaceName("org.freedesktop.portal.Settings")
interface FreedesktopInterface : DBusInterface {
    companion object {
        const val APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
        const val COLOR_SCHEME_KEY = "color-scheme"

        private val connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)
        private val freedesktopInterface: FreedesktopInterface = connection.getRemoteObject(
            "org.freedesktop.portal.Desktop",
            "/org/freedesktop/portal/desktop",
            FreedesktopInterface::class.java
        )

        val theme: ThemeMode
            get() {
                val theme = recursiveVariantValue(
                    freedesktopInterface.Read(
                        APPEARANCE_NAMESPACE,
                        COLOR_SCHEME_KEY
                    )
                ) as UInt32

                return when (theme.toInt()) {
                    1 -> ThemeMode.DARK
                    2 -> ThemeMode.LIGHT
                    else -> ThemeMode.ERROR
                }
            }

        fun addSettingChangedHandler(sigHandler: DBusSigHandler<SettingChanged>) =
            connection.addSigHandler(SettingChanged::class.java, sigHandler)

        fun removeSettingChangedHandler(sigHandler: DBusSigHandler<SettingChanged>) =
            connection.removeSigHandler(SettingChanged::class.java, sigHandler)

        /**
         * Unpacks a Variant recursively and returns the inner value.
         * @see Variant
         */
        private fun recursiveVariantValue(variant: Variant<*>): Any {
            val value = variant.value
            return if (value !is Variant<*>) value else recursiveVariantValue(value)
        }
    }

    fun Read(namespace: String, key: String): Variant<*>

    class SettingChanged(objectpath: String, namespace: String, key: String, value: Variant<Any>) :
        DBusSignal(objectpath, namespace, key, value) {
        val colorSchemeChanged: Boolean =
            namespace == APPEARANCE_NAMESPACE && key == COLOR_SCHEME_KEY
    }
}
