package cn.xylin.skiprewardad.hook;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MolocoAdHook extends BaseHook {
    private Object defAd;

    public MolocoAdHook(Context ctx) {
        super(ctx);
    }

    @Override
    protected void runHook() throws Throwable {
        Class<?> rewardedAdClass = findClass("com.moloco.sdk.internal.publisher.H");
        Class<?> fullscreenAdClass = findClass("com.moloco.sdk.internal.publisher.v");
        Class<?> rewardedListenerClass = findClass("com.moloco.sdk.publisher.RewardedInterstitialAdShowListener");
        Class<?> adShowListenerClass = findClass("com.moloco.sdk.publisher.AdShowListener");
        Class<?> molocoAdClass = findClass("com.moloco.sdk.publisher.MolocoAd");
        if (molocoAdClass == null) {
            return;
        }

        defAd = XposedHelpers.newInstance(molocoAdClass, "Moloco", "", null, null);
        if (rewardedAdClass != null && rewardedListenerClass != null) {
            XposedHelpers.findAndHookMethod(rewardedAdClass, "show", rewardedListenerClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object listener = param.args.length == 1 ? param.args[0] : null;
                    if (listener == null) {
                        return;
                    }
                    dispatchReward(listener);
                    param.setResult(null);
                    log("MolocoAd-发放奖励");
                }
            });
        }

        if (fullscreenAdClass != null && adShowListenerClass != null) {
            XposedHelpers.findAndHookMethod(fullscreenAdClass, "show", adShowListenerClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object listener = param.args.length == 1 ? param.args[0] : null;
                    if (listener == null) {
                        return;
                    }
                    dispatchReward(listener);
                    param.setResult(null);
                    log("MolocoAd-底层发放奖励");
                }
            });
        }
    }

    private void dispatchReward(Object listener) {
        callMethod(listener, "onAdShowSuccess", defAd);
        callMethod(listener, "onRewardedVideoStarted", defAd);
        callMethod(listener, "onUserRewarded", defAd);
        callMethod(listener, "onRewardedVideoCompleted", defAd);
        callMethod(listener, "onAdHidden", defAd);
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
