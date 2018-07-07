package com.github.yamill.orientation;

import android.content.res.Configuration;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class OrientationModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    final OrientationEventListener mOrientationEventListener;
    private Integer mOrientationValue;
    private String mOrientation;
    private String mSpecificOrientation;
    final private String[] mOrientations;

    private boolean mHostActive = false;

    public static final String LANDSCAPE = "LANDSCAPE";
    public static final String LANDSCAPE_LEFT = "LANDSCAPE-LEFT";
    public static final String LANDSCAPE_RIGHT = "LANDSCAPE-RIGHT";
    public static final String PORTRAIT = "PORTRAIT";
    public static final String PORTRAIT_UPSIDEDOWN = "PORTRAITUPSIDEDOWN";
    public static final String ORIENTATION_UNKNOWN = "UNKNOWN";

    private static final int ACTIVE_SECTOR_SIZE = 45;
    private final String[] ORIENTATIONS_PORTRAIT_DEVICE = {PORTRAIT, LANDSCAPE_RIGHT, PORTRAIT_UPSIDEDOWN, LANDSCAPE_LEFT};
    private final String[] ORIENTATIONS_LANDSCAPE_DEVICE = {LANDSCAPE_LEFT, PORTRAIT, LANDSCAPE_RIGHT, PORTRAIT_UPSIDEDOWN};

    public OrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        final ReactApplicationContext ctx = reactContext;

        mOrientations = isLandscapeDevice() ? ORIENTATIONS_LANDSCAPE_DEVICE : ORIENTATIONS_PORTRAIT_DEVICE;
        Log.d("mOrientations", mOrientations.toString());

        mOrientationEventListener = new OrientationEventListener(reactContext,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientationValue) {
                Log.d("onOrientationChanged", orientationValue + "");
                if (!mHostActive || isDeviceOrientationLocked() || !ctx.hasActiveCatalystInstance())
                    return;

                mOrientationValue = orientationValue;

                if (mOrientation != null || mSpecificOrientation != null) {
                    final int halfSector = ACTIVE_SECTOR_SIZE;
                    if ((orientationValue % 90) > halfSector
                            && (orientationValue % 90) < (90 - halfSector)) {
                        return;
                    }
                }

                final String orientation = getOrientationStringWhenChanging(orientationValue);
                final String specificOrientation = getSpecificOrientationString(orientationValue);

                final DeviceEventManagerModule.RCTDeviceEventEmitter deviceEventEmitter = ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

                if (!orientation.equals(mOrientation)) {
                    String tempName = orientation;
                    Log.d("thay doi","ban dau: "+ mOrientation +" sau: "+ orientation);
                    if(orientation == "EQUAL_BEFORE" && mOrientation != null ){
                        tempName = mOrientation;
                    }
                    mOrientation = orientation;
                    WritableMap params = Arguments.createMap();
                    params.putString("orientation", tempName);
                    deviceEventEmitter.emit("orientationDidChange", params);
                }

                if (!specificOrientation.equals(mSpecificOrientation)) {
                    mSpecificOrientation = specificOrientation;
                    WritableMap params = Arguments.createMap();
                    params.putString("specificOrientation", specificOrientation);
                    deviceEventEmitter.emit("specificOrientationDidChange", params);
                }
            }
        };
        ctx.addLifecycleEventListener(this);

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    @ReactMethod
    public void isOrientationLockedBySystem(Callback callback) {
        try {
            if (Settings.System.getInt(
                    getReactApplicationContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION
            ) == 1) {
                callback.invoke(null, false);
            } else {
                callback.invoke(null, true);
            };
        } catch (Settings.SettingNotFoundException e) {
            callback.invoke(e, null);
        }
    }

    @Override
    public String getName() {
        return "Orientation";
    }

    @ReactMethod
    public void getOrientation(Callback callback) {
        callback.invoke(null, mOrientation);
    }

    @ReactMethod
    public void lockToPortrait() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @ReactMethod
    public void lockToLandscape() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeLeft() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeRight() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @ReactMethod
    public void unlockAllOrientations() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public
    @Nullable
    Map<String, Object> getConstants() {
        HashMap<String, Object> constants = new HashMap<String, Object>();
        int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);
        if (orientation == "null") {
            constants.put("initialOrientation", null);
        } else {
            constants.put("initialOrientation", orientation);
        }

        return constants;
    }

    private boolean isDeviceOrientationLocked() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return false;
        }
        return Settings.System.getInt(
                activity.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 0;
    }

    private boolean isLandscapeDevice() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return false;
        }

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x > size.y;
    }

    private String getSpecificOrientationString(int orientationValue) {
        String o = "EQUAL_BEFORE";
        if (orientationValue < 0) return o =  ORIENTATION_UNKNOWN;
//        final int index = (int) ((float) orientationValue / 90.0 + 0.5) % 4;
        int index =0 ;
        if(orientationValue <292 && orientationValue >= 247){
            o = LANDSCAPE_LEFT;
        }
        if(orientationValue > 67 && orientationValue <=  112){
            o = LANDSCAPE_RIGHT;
        }
        if(orientationValue > 337 || orientationValue < 25){
            o =  PORTRAIT;
        }

        if(orientationValue > 157 && orientationValue < 202){
            o=  PORTRAIT_UPSIDEDOWN;
        }
        return o;
//        Log.d("Orientaion string", "getSpecificOrientationString: "+ " "+index+ " "+mOrientations[index]);
//        return mOrientations[index];
    }

    private String getOrientationString(int orientationValue) {
        if (orientationValue == Configuration.ORIENTATION_LANDSCAPE) {
            return "LANDSCAPE";
        } else if (orientationValue == Configuration.ORIENTATION_PORTRAIT) {
            return "PORTRAIT";
        } else if (orientationValue == Configuration.ORIENTATION_UNDEFINED) {
            return "UNKNOWN";
        } else {
            return "null";
        }
    }

    private String getOrientationStringWhenChanging(int orientationValue) {
        final String specificOrientation = getSpecificOrientationString(orientationValue);
        switch (specificOrientation) {
            case LANDSCAPE_LEFT:
            case LANDSCAPE_RIGHT:
                return LANDSCAPE;
            case PORTRAIT:
            case PORTRAIT_UPSIDEDOWN:
                return PORTRAIT;
            case "EQUAL_BEFORE":
                return "EQUAL_BEFORE";
            default:
                return ORIENTATION_UNKNOWN;
        }
    }

    @Override
    public void onHostResume() {
        mHostActive = true;
    }

    @Override
    public void onHostPause() {
        mHostActive = false;
    }

    @Override
    public void onHostDestroy() {
        mHostActive = false;
    }
}
