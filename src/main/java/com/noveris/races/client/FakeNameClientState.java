package com.noveris.races.client;

public final class FakeNameClientState {
    public static String nickname = "", pronouns = "", format = "normal", color = "white";
    public static boolean open;
    private FakeNameClientState() {}
    public static void accept(String n, String p, String f, String c, boolean show) {
        nickname=n; pronouns=p; format=f; color=c; open=show;
    }
}
