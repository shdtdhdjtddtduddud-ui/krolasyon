package com.krolasyon.realweather.client;

/**
 * Oculus/Iris ile uyum: bir shader paketi açık mı ve adı ne? Paketler vanilya sisini genellikle yok sayar; o zaman
 * hava sisi kameranın çevresindeki derinlik testli pus katmanlarıyla verilir.
 */
public final class ShaderCompat {
    private ShaderCompat() {}

    private static long checkedAt = -1000;
    private static boolean active;
    private static String pack = "";

    private static void refresh(long now) {
        if (now - checkedAt < 40) return;
        checkedAt = now;
        active = false;
        pack = "";
        try {
            Object api = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            active = (Boolean) api.getClass().getMethod("isShaderPackInUse").invoke(api);
        } catch (Throwable ignored) {
            return;
        }
        if (!active) return;
        for (String cls : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
            try {
                Object o = Class.forName(cls).getMethod("getCurrentPackName").invoke(null);
                if (o != null) {
                    pack = o.toString();
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }

    public static boolean shaders(long now) {
        refresh(now);
        return active;
    }

    /** Krolasyon Fantasy paketi mi? */
    public static boolean packIsKrolasyon(long now) {
        refresh(now);
        return pack.toLowerCase(java.util.Locale.ROOT).contains("krolasyon");
    }

    public static String packName() {
        return pack;
    }
}
