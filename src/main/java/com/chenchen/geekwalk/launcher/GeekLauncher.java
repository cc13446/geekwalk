package com.chenchen.geekwalk.launcher;

import io.vertx.core.Launcher;

@SuppressWarnings("checkstyle:UncommentedMain")
public class GeekLauncher extends Launcher {
    public static void main(String[] args) {
        new GeekLauncher().dispatch(args);
    }
}
