package cn.xylin.skiprewardad.hook;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ApplovinAdHook extends BaseHook {
    private static final String LISTENER_FIELD = "skip_reward_applovin_listener";
    private static final String ANA_POCKET_PACKAGE = "us.ana_pocket.ANAPocket";
    private Object listener, anaListener, defReward;
    public ApplovinAdHook(Context ctx) {
        super(ctx);
    }
    @Override
    protected void runHook() throws Throwable {
        initDefaultReward();
        if (ANA_POCKET_PACKAGE.equals(context.getPackageName())) {
            hookAnaPocketWrapper();
            return;
        }
        claza = findClass("com.applovin.mediation.ads.MaxRewardedAd");
        if (claza == null) {
            return;
        }
        XposedBridge.hookAllMethods(claza, "setListener", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args.length == 1 && param.args[0] != null) {
                    listener = param.args[0];
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, LISTENER_FIELD, listener);
                }
            }
        });
        XposedBridge.hookAllMethods(claza, "showAd", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object maxAd = param.thisObject;
                Object targetListener = XposedHelpers.getAdditionalInstanceField(maxAd, LISTENER_FIELD);
                if (targetListener == null) {
                    targetListener = listener;
                }
                if (targetListener != null && maxAd != null) {
                    callMethod(targetListener, "onAdDisplayed", maxAd);
                    callMethod(targetListener, "onRewardedVideoStarted", maxAd);
                    callMethod(targetListener, "onUserRewarded", maxAd, defReward);
                    callMethod(targetListener, "onRewardedVideoCompleted", maxAd);
                    callMethod(targetListener, "onAdHidden", maxAd);
                    param.setResult(null);
                    log("ApplovinAd-发放奖励");
                }
            }
        });
    }

    private void initDefaultReward() {
        try {
            Class<?> rewardClass = findClass("com.applovin.impl.mediation.MaxRewardImpl");
            if (rewardClass != null) {
                defReward = rewardClass.getDeclaredMethod("createDefault").invoke(null);
            }
        } catch (Throwable ignore) {
        }
    }

    private void hookAnaPocketWrapper() {
        Class<?> maxAdWrapperClass = findClass("us.ana_pocket.ANAPocket.utils.MaxAdWrapper");
        Class<?> rewardedWrapperClass = findClass("us.ana_pocket.ANAPocket.utils.MaxRewardedAdWrapper");
        if (maxAdWrapperClass == null || rewardedWrapperClass == null) {
            return;
        }
        XposedBridge.hookAllMethods(maxAdWrapperClass, "setListener", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args.length == 1 && param.args[0] != null) {
                    anaListener = param.args[0];
                }
            }
        });
        XposedBridge.hookAllMethods(rewardedWrapperClass, "showAdImpl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object wrapper = param.thisObject;
                Object targetListener = anaListener;
                if (targetListener == null) {
                    targetListener = XposedHelpers.getStaticObjectField(maxAdWrapperClass, "listener");
                }
                if (targetListener != null && wrapper != null) {
                    callMethod(targetListener, "onAdDisplayed", wrapper);
                    callMethod(targetListener, "onUserRewarded", wrapper, defReward);
                    finishAnaPocketWrapper(wrapper);
                    callMethod(targetListener, "onAdHidden", wrapper);
                    param.setResult(null);
                    log("ApplovinAd-ANA Pocket wrapper reward");
                }
            }
        });
    }

    private void finishAnaPocketWrapper(Object wrapper) {
        try {
            XposedHelpers.setBooleanField(wrapper, "showing", false);
            XposedHelpers.setObjectField(wrapper, "adPlacement", null);
            callMethod(wrapper, "loadAd");
        } catch (Throwable ignore) {
        }
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
