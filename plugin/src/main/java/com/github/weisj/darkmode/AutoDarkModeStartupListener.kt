package com.github.weisj.darkmode

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

class AutoDarkModeStartupListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: List<String>) {
        ServiceManager.getService(AutoDarkMode::class.java).start()
    }

    override fun appStarting(projectFromCommandLine: Project?) {
        IntellijNotificationService.initialize()
    }

    override fun appClosing() {
        ServiceManager.getServiceIfCreated(AutoDarkMode::class.java)?.stop()
    }
}
