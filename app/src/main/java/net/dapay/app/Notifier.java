package net.dapay.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by gabriel on 12/11/16.
 */

public class Notifier {
    private Context mContext;
    private Activity mActivity;
    NotificationManager mNotificationManager;
    int mLastNotificationID = 0;

    private static Notifier INSTANCE = null;
    private Notifier(Context context, Activity a) {
        this.mContext = context;
        this.mActivity = a;
        mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }
    public static Notifier getInstance(Context context, Activity a) {
        if (INSTANCE == null)
            INSTANCE = new Notifier(context, a);
        return INSTANCE;
    }
    public static Notifier getInstance() {
        return INSTANCE;
    }

    public void NotifyUser(Bill bill, int id_title, int id_details) {
        NotifyUser(bill, mContext.getString(id_title), mContext.getString(id_details));
    }

    public void NotifyUser(Bill bill, String title, String details) {
        // TODO: make the builder member, built once only
        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setContentTitle(title);
        builder.setContentText(details);
        builder.setSmallIcon(mContext.getApplicationInfo().icon);

        Intent intent = new Intent(mActivity, BillActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("wallet_address", bill.getWalletID());
        intent.putExtras(bundle);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(PayActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        Notification n = builder.build();

        mNotificationManager.notify(++mLastNotificationID, n);
    }
}
