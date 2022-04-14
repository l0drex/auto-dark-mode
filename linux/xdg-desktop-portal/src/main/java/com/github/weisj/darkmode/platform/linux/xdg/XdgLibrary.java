package com.github.weisj.darkmode.platform.linux.xdg;

import com.github.weisj.darkmode.platform.AbstractPluginLibrary;
import com.github.weisj.darkmode.platform.LibraryUtil;
import com.github.weisj.darkmode.platform.PluginLogger;

public class XdgLibrary extends AbstractPluginLibrary {
    private static final String PROJECT_NAME = "auto-dark-mode-linux-xdg";
    private static final String PATH = "/com/github/weisj/darkmode/" + PROJECT_NAME + "/linux-x86-64/";
    private static final String DLL_NAME = "lib" + PROJECT_NAME + ".so";
    private static final XdgLibrary instance = new XdgLibrary();

    protected XdgLibrary() {
        super(PATH, DLL_NAME, PluginLogger.getLogger((XdgLibrary.class)));
    }

    static XdgLibrary get() {
        instance.updateLibrary();
        return instance;
    }

    @Override
    protected boolean canLoad() {
        // todo check the preference is implemented
        return LibraryUtil.isX64;
    }
}
