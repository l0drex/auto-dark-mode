package com.github.weisj.darkmode.platform.linux.xdg;

public class XdgNative {
    private XdgNative() {
        throw new IllegalStateException("Native methods holder");
    }

    static native String getCurrentTheme();

    static native long createEventHandler(final Runnable callback);

    static native void deleteEventHandler(final long handle);

    static native void init();
}
