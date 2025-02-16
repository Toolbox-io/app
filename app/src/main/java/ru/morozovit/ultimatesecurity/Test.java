package ru.morozovit.ultimatesecurity;

import androidx.annotation.Keep;

@Keep
public class Test {
    static {
        System.loadLibrary("ultimatesecurity");
    }

    public native String error();
}
