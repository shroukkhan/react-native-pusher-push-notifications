package com.b8ne.RNPusherPushNotifications;

import android.app.Activity;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.firebase.messaging.RemoteMessage;
import com.pusher.pushnotifications.PushNotificationReceivedListener;
import com.pusher.pushnotifications.PushNotifications;
import com.pusher.pushnotifications.SubscriptionsChangedListener;

import java.util.Map;
import java.util.Set;

//
// TODO: verify the android manifest after https://docs.pusher.com/beams/reference/android

/**
 * Created by bensutter on 13/1/17.
 * https://docs.pusher.com/beams/getting-started/android/init-beams
 */

public class PusherWrapper {

    private String registeredEvent = "registered";
    private String notificationEvent = "notification";
    private ReactContext context;


    public PusherWrapper(String appKey, final ReactContext context) {
        Log.d("PUSHER_WRAPPER", "Creating Pusher with App Key: " + appKey);
        System.out.print("Creating Pusher with App Key: " + appKey);

        try {
            // the new-ed version of PushNotificationsInstance doesn't seem to have
            // an instance of setOnMessageReceivedListenerForVisibleActivity, so
            // use the singleton version.
            // this.pushNotifications = new PushNotificationsInstance(context, appKey);
            // Does .start actually throw an exception?
            PushNotifications.start(context, appKey);
            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(registeredEvent, null);
            this.context = context;
        } catch (Exception ex) {
            Log.d("PUSHER_WRAPPER", "Exception in PusherWrapper " + ex.getMessage());
            System.out.print("Exception in PusherWrapper " + ex.getMessage());
        }
    }

    public void onResume(final Activity activity) {
        // SEE: https://docs.pusher.com/beams/reference/android
        Log.i("PUSHER_WRAPPER", "onResume subscribing with activity " + getActivityName(activity));
        System.out.print("onResume subscribing with activity " + getActivityName(activity));

        PushNotifications.setOnMessageReceivedListenerForVisibleActivity(activity, new PushNotificationReceivedListener() {
            @Override
            public void onMessageReceived(RemoteMessage remoteMessage) {
                // Arguments.createMap seems to be for testing
                // see: https://github.com/facebook/react-native/blob/master/ReactAndroid/src/main/java/com/facebook/react/bridge/WritableNativeMap.java#L16
                //WritableMap map = Arguments.createMap();

                RemoteMessage.Notification notification = remoteMessage.getNotification();

                if (notification != null) {
                    final WritableMap dataMap = new WritableNativeMap();
                    dataMap.putString("body", notification.getBody());
                    dataMap.putString("title", notification.getTitle());
                    dataMap.putString("tag", notification.getTag());
                    dataMap.putString("click_action", notification.getClickAction());
                    dataMap.putString("icon", notification.getIcon());
                    dataMap.putString("color", notification.getColor());
                    context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(notificationEvent, dataMap);
                    Log.d("PUSHER_WRAPPER", "Notification received: " + notification.toString());
                } else {
                    final WritableMap dataMap = new WritableNativeMap();
                    final WritableMap userInfoMap = new WritableNativeMap();
                    final WritableMap silentMap = new WritableNativeMap();

                    final Map<String, String> messageData = remoteMessage.getData();
                    Log.d("PUSHER_WRAPPER", "Silent Notification received" + messageData.toString());

                    dataMap.putString("body", messageData.get("body"));
                    dataMap.putString("title", messageData.get("title"));
                    userInfoMap.putMap("data", dataMap);
                    silentMap.putMap("userInfo", userInfoMap);

                    context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(notificationEvent, silentMap);

                }
            }
        });

    }

    public void onDestroy(Activity activity) {
        Log.i("PUSHER_WRAPPER", "onDestroy: " + getActivityName(activity));
    }

    public void onPause(Activity activity) {
        Log.i("PUSHER_WRAPPER", "onPause: " + getActivityName(activity));
    }

    private String getActivityName(Activity activity) {
        return activity.getClass().getSimpleName();
    }

    public void subscribe(final String interest, final Callback errorCallback, final Callback successCallback) {
        Log.d("PUSHER_WRAPPER", "Subscribing to " + interest);
        System.out.print("Subscribing to " + interest);
        try {
            //this.pushNotifications.subscribe(interest);
            PushNotifications.subscribe(interest);
            Log.d("PUSHER_WRAPPER", "Success! " + interest);
            System.out.print("Success! " + interest);
            successCallback.invoke();
        } catch (Exception ex) {
            Log.d("PUSHER_WRAPPER", "Exception in PusherWrapper " + ex.getMessage());
            System.out.print("Exception in PusherWrapper.subscribe " + ex.getMessage());
            // historically this is expecting a statusCode as first arg
            errorCallback.invoke(0, ex.getMessage());
        }
    }

    public void unsubscribe(final String interest, final Callback errorCallback, final Callback successCallback) {
        Log.d("PUSHER_WRAPPER", "Unsubscribing from " + interest);
        System.out.print("Unsubscribing from " + interest);
        try {
            PushNotifications.unsubscribe(interest);
            if (successCallback != null) {
                successCallback.invoke();
            }
        } catch (Exception ex) {
            Log.d("PUSHER_WRAPPER", "Exception in PusherWrapper.unsubscribe " + ex.getMessage());
            System.out.print("Exception in PusherWrapper.unsubscribe " + ex.getMessage());
            // historically this is expecting a statusCode as first arg
            if (errorCallback != null) {
                errorCallback.invoke(0, ex.getMessage());
            }
        }
    }

    public void unsubscribeAll(final Callback errorCallback, final Callback successCallback) {

        try {
            PushNotifications.unsubscribeAll();
            if (successCallback != null) {
                successCallback.invoke();
            }
        } catch (Exception ex) {
            Log.d("PUSHER_WRAPPER", "Exception in PusherWrapper.unsubscribeAll " + ex.getMessage());
            System.out.print("Exception in PusherWrapper.unsubscribeAll " + ex.getMessage());
            // historically this is expecting a statusCode as first arg
            if (errorCallback != null) {
                errorCallback.invoke(0, ex.getMessage());
            }
        }
    }

    public void getSubscriptions(final Callback subscriptionCallback, final Callback errorCallback) {
        try {
            Set<String> subscriptionSet = PushNotifications.getSubscriptions();
            WritableArray subscriptions = new WritableNativeArray();
            for (String subscription : subscriptionSet) {
                subscriptions.pushString(subscription);
            }
            subscriptionCallback.invoke(subscriptions);
        } catch (Exception ex) {
            Log.d("PUSHER_WRAPPER", "Exception in PusherWrapper.getSubscriptions " + ex.getMessage());
            System.out.print("Exception in PusherWrapper.getSubscriptions " + ex.getMessage());
            // historically this is expecting a statusCode as first arg
            if (errorCallback != null) {
                errorCallback.invoke(0, ex.getMessage());
            }
        }
    }

    public void setOnSubscriptionsChangedListener(final Callback subscriptionChangedListener) {

        PushNotifications.setOnSubscriptionsChangedListener(new SubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged(Set<String> interestSet) {
                // call
                WritableArray interests = new WritableNativeArray();
                for (String interest : interestSet) {
                    interests.pushString(interest);
                }
                subscriptionChangedListener.invoke(interests);
            }
        });
    }


}

