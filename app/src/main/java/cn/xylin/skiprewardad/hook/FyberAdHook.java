package cn.xylin.skiprewardad.hook;

import android.app.Activity;
import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FyberAdHook extends BaseHook {
    public FyberAdHook(Context ctx) {
        super(ctx);
    }

    @Override
    protected void runHook() throws Throwable {
        Class<?> controllerClass = findClass("com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController");
        if (controllerClass == null) {
            return;
        }
        XposedHelpers.findAndHookMethod(controllerClass, "show", Activity.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object controller = param.thisObject;
                Object spot = XposedHelpers.callMethod(controller, "getAdSpot");
                Object rewardedListener = XposedHelpers.callMethod(controller, "getRewardedListener");
                Object eventsListener = XposedHelpers.callMethod(controller, "getEventsListener");
                if (spot == null || (rewardedListener == null && eventsListener == null)) {
                    return;
                }
                if (eventsListener != null) {
                    callMethod(eventsListener, "onAdImpression", spot);
                }
                if (rewardedListener != null) {
                    callMethod(rewardedListener, "onAdRewarded", spot);
                }
                if (eventsListener != null) {
                    callMethod(eventsListener, "onAdDismissed", spot);
                }
                param.setResult(null);
                log("FyberAd-发放奖励");
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
