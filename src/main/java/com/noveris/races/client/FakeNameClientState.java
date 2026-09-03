package com.noveris.races.client;

public final class FakeNameClientState {
    public static String nickname = "", pronouns = "", format = "normal", color = "white", prefix = "none";
    public static boolean open;
    private FakeNameClientState() {}
    public static void accept(String n, String p, String f, String c, String x, boolean show) {
        nickname=n; pronouns=p; format=f; color=c; prefix=x; open=show;
    }
}
