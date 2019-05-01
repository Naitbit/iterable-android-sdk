package com.iterable.iterableapi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by David Truong dt@iterable.com
 */
public class IterableNotificationBuilder extends NotificationCompat.Builder {
    static final String TAG = "IterableNotification";
    final Context context;

    private boolean isGhostPush;
    private String imageUrl;
    private String expandedContent;
    int requestCode;
    IterableNotificationData iterableNotificationData;

    /**
     * Creates a custom Notification builder
     * @param context
     * @param channelId
     */
    protected IterableNotificationBuilder(Context context, String channelId) {
        super(context, channelId);
        this.context = context;
    }

    /**
     * Sets the image url
     * @param imageUrl
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Sets the expanded content used for backwards compatibility up to Android API 23
     * @param content
     */
    public void setExpandedContent(String content) {
        this.expandedContent = content;
    }


    public void setIsGhostPush(boolean ghostPush) {
        isGhostPush = ghostPush;
    }

    public boolean isGhostPush() {
        return isGhostPush;
    }

    /**
     * Combine all of the options that have been set and return a new {@link Notification}
     * object.
     * Download any optional images
     */
    public Notification build() {
        NotificationCompat.Style style = null;

        if (this.imageUrl != null) {
            try {
                URL url = new URL(this.imageUrl);
                URLConnection connection = url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                Bitmap notificationImage = BitmapFactory.decodeStream(connection.getInputStream());
                if (notificationImage != null) {
                    style = new NotificationCompat.BigPictureStyle()
                            .bigPicture(notificationImage)
                            .setSummaryText(expandedContent);
                } else {
                    IterableLogger.e(TAG, "Notification image could not be loaded from url: " + this.imageUrl);
                }
            } catch (MalformedURLException e) {
                IterableLogger.e(TAG, e.toString());
            } catch (IOException e) {
                IterableLogger.e(TAG, e.toString());
            }
        }

        //Sets the default BigTextStyle if the imageUrl isn't set or cannot be loaded.
        if (style == null) {
            style = new NotificationCompat.BigTextStyle().bigText(expandedContent);
        }

        this.setStyle(style);

        return super.build();
    }

    /**
     * Creates a notification action button for a given JSON payload
     * @param context    Context
     * @param button     `IterableNotificationData.Button` object containing button information
     * @param extras     Notification payload
     */
    public void createNotificationActionButton(Context context, IterableNotificationData.Button button, Bundle extras) {
        Intent buttonIntent = new Intent(IterableConstants.ACTION_PUSH_ACTION);
        buttonIntent.setClass(context, IterablePushActionReceiver.class);
        buttonIntent.putExtras(extras);
        buttonIntent.putExtra(IterableConstants.REQUEST_CODE, requestCode);
        buttonIntent.putExtra(IterableConstants.ITERABLE_DATA_ACTION_IDENTIFIER, button.identifier);
        buttonIntent.putExtra(IterableConstants.ACTION_IDENTIFIER, button.identifier);

        PendingIntent pendingButtonIntent = PendingIntent.getBroadcast(context, buttonIntent.hashCode(),
                buttonIntent, 0);

        NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action
                .Builder(NotificationCompat.BADGE_ICON_NONE, button.title, pendingButtonIntent);

        if (button.buttonType.equals(IterableNotificationData.Button.BUTTON_TYPE_TEXT_INPUT)) {
            actionBuilder.addRemoteInput(new RemoteInput.Builder(IterableConstants.USER_INPUT).setLabel(button.inputPlaceholder).build());
        }

        addAction(actionBuilder.build());
    }

    /**
     * Creates and returns an instance of IterableNotification.
     * This is kept here for backwards compatibility.
     *
     * @param context
     * @param extras
     * @return Returns null if the intent comes from an Iterable ghostPush or it is not an Iterable notification
     */
    public static IterableNotificationBuilder createNotification(Context context, Bundle extras) {
        return IterableNotificationHelper.createNotification(context, extras);
    }

    /**
     * Posts the notification on device.
     * Only sets the notification if it is not a ghostPush/null iterableNotification.
     * This is kept here for backwards compatibility.
     *
     * @param context
     * @param iterableNotificationBuilder Function assumes that the iterableNotification is a ghostPush
     *                             if the IterableNotification passed in is null.
     */
    public static void postNotificationOnDevice(Context context, IterableNotificationBuilder iterableNotificationBuilder) {
        IterableNotificationHelper.postNotificationOnDevice(context, iterableNotificationBuilder);
    }

}
