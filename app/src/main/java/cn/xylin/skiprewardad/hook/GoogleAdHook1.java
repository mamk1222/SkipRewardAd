package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GoogleAdHook1 extends BaseHook {
    private static final String CALLBACK_FIELD = "skip_reward_google_full_screen_callback";
    private Object defReward, listener;
    private Class<?> clazc, rewardedInterstitialAdClass;
    public GoogleAdHook1(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        try {
            defReward = findClass("com.google.android.gms.ads.rewarded.RewardItem").getDeclaredField("DEFAULT_REWARD").get(null);
        } catch (Throwable ignore) {
            return;
        }
        claza = findClass("com.google.android.gms.ads.rewarded.RewardedAd");
        rewardedInterstitialAdClass = findClass("com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd");
        clazb = findClass("com.google.android.gms.ads.rewarded.RewardedAdLoadCallback");
        clazc = findClass("com.google.android.gms.ads.OnUserEarnedRewardListener");
        if (claza == null || clazb == null || clazc == null) {
            return;
        }
        hookRewardedShow(claza);
        hookRewardedShow(rewardedInterstitialAdClass);
        XposedBridge.hookAllMethods(claza, "load", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (Object obj : param.args) {
                    if (!clazb.isInstance(obj) || isHooked(clazb.getName())) {
                        continue;
                    }
                    XposedBridge.hookAllMethods(obj.getClass(), "onAdLoaded", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args.length == 0 || param.args[0] == null) {
                                return;
                            }
                            Class<?> clazz = param.args[0].getClass();
                            if (isHooked(clazz.getName())) {
                                return;
                            }
                            XposedBridge.hookAllMethods(clazz, "setFullScreenContentCallback", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args.length == 1 && param.args[0] != null) {
                                        listener = param.args[0];
                                        XposedHelpers.setAdditionalInstanceField(param.thisObject, CALLBACK_FIELD, listener);
                                    }
                                }
                            });
                            hookRewardedShow(clazz);
                        }
                    });
                    break;
                }
            }
        });
        if (rewardedInterstitialAdClass != null) {
            XposedBridge.hookAllMethods(rewardedInterstitialAdClass, "load", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    for (Object obj : param.args) {
                        if (obj == null || isHooked(obj.getClass().getName())) {
                            continue;
                        }
                        XposedBridge.hookAllMethods(obj.getClass(), "onAdLoaded", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args.length == 0 || param.args[0] == null) {
                                    return;
                                }
                                hookRewardedShow(param.args[0].getClass());
                            }
                        });
                        break;
                    }
                }
            });
        }
    }

    private void hookRewardedShow(Class<?> clazz) {
        if (clazz == null || isHooked(clazz.getName() + "#show")) {
            return;
        }
        try {
            XposedBridge.hookAllMethods(clazz, "setFullScreenContentCallback", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length == 1 && param.args[0] != null) {
                        listener = param.args[0];
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, CALLBACK_FIELD, listener);
                    }
                }
            });
        } catch (Throwable ignore) {
        }
        XposedBridge.hookAllMethods(clazz, "show", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object rewardListener = null;
                for (Object obj : param.args) {
                    if (clazc.isInstance(obj)) {
                        rewardListener = obj;
                        break;
                    }
                }
                if (rewardListener == null) {
                    return;
                }
                Object callback = XposedHelpers.getAdditionalInstanceField(param.thisObject, CALLBACK_FIELD);
                if (callback == null) {
                    callback = listener;
                }
                if (callback != null) {
                    callMethod(callback, "onAdShowedFullScreenContent");
                }
                callMethod(rewardListener, "onUserEarnedReward", defReward);
                if (callback != null) {
                    callMethod(callback, "onAdDismissedFullScreenContent");
                }
                param.setResult(null);
                log("GoogleAd1-发放奖励");
            }
        });
    }

    @Override
    protected String targetPackageName() {
        return null;
    }
    @Override
    protected boolean isTarget() {
        return true;
    }
}
