/*
 * MIT License
 *
 * Copyright (c) 2020-2022 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.weisj.darkmode.platform.linux.xdg

import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant


class FreedesktopConnection {
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
                    FreedesktopInterface.APPEARANCE_NAMESPACE,
                    FreedesktopInterface.COLOR_SCHEME_KEY
                )
            ) as UInt32

            return when (theme.toInt()) {
                1 -> ThemeMode.DARK
                2 -> ThemeMode.LIGHT
                else -> ThemeMode.ERROR
            }
        }

    fun addSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>) =
        connection.addSigHandler(FreedesktopInterface.SettingChanged::class.java, sigHandler)

    fun removeSettingChangedHandler(sigHandler: DBusSigHandler<FreedesktopInterface.SettingChanged>) =
        connection.removeSigHandler(FreedesktopInterface.SettingChanged::class.java, sigHandler)

    /**
     * Unpacks a Variant recursively and returns the inner value.
     * @see Variant
     */
    private fun recursiveVariantValue(variant: Variant<*>): Any {
        val value = variant.value
        return if (value !is Variant<*>) value else recursiveVariantValue(value)
    }
}
