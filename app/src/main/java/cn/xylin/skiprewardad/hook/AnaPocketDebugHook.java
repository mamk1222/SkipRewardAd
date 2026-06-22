package cn.xylin.skiprewardad.hook;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AnaPocketDebugHook extends BaseHook {
    public AnaPocketDebugHook(Context ctx) {
        super(ctx);
    }

    @Override
    protected void runHook() throws Throwable {
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args.length != 1 || !(param.args[0] instanceof String)) {
                    return;
                }
                String name = (String) param.args[0];
                if (!isAdClass(name) || isHooked(name)) {
                    return;
                }
                Class<?> clazz = (Class<?>) param.getResult();
                XposedBridge.log("AnaPocketDebug loaded: " + name);
                hookMethod(clazz, "show");
                hookMethod(clazz, "showAd");
                hookMethod(clazz, "showRewardedAd");
                hookMethod(clazz, "playAd");
                hookMethod(clazz, "loadAd");
                hookMethod(clazz, "load");
            }
        });
    }

    private void hookMethod(Class<?> clazz, String methodName) {
        try {
            XposedBridge.hookAllMethods(clazz, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("AnaPocketDebug call: " + clazz.getName() + "#" + methodName);
                }
            });
        } catch (Throwable ignore) {
        }
    }

    private boolean isAdClass(String name) {
        return name.startsWith("com.applovin.")
                || name.startsWith("com.google.android.gms.ads.")
                || name.startsWith("com.unity3d.ads.")
                || name.startsWith("com.ironsource.")
                || name.startsWith("com.bytedance.sdk.openadsdk.")
                || name.startsWith("com.mbridge.")
                || name.startsWith("com.vungle.")
                || name.startsWith("com.moloco.")
                || name.toLowerCase().contains("reward");
    }

    @Override
    protected String targetPackageName() {
        return "us.ana_pocket.ANAPocket";
    }

    @Override
    protected boolean isDebug() {
        return true;
    }
}
