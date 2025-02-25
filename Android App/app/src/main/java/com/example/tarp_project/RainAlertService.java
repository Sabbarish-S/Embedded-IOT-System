package com.example.tarp_project;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RainAlertService extends Service {

    private static final String CHANNEL_ID = "RainAlertServiceChannel";
    private DatabaseReference myRef;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();


        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("sensors");

        // Listen for changes in Firebase
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String rainStatus = dataSnapshot.child("rain_status").getValue(String.class);
                if (!"no rain".equalsIgnoreCase(rainStatus)) {
                    sendRainAlertNotification(rainStatus);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("RainAlertService", "DatabaseError", error.toException());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rain Alert Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private NotificationCompat.Builder getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Rain Alert Service Running")
                .setContentText("Monitoring rain alerts in the background")
                .setSmallIcon(com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private void sendRainAlertNotification(String rainStatus) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.google.firebase.appcheck.interop.R.drawable.common_google_signin_btn_icon_dark)
                .setContentTitle("Rain Alert")
                .setContentText("Rain indication is " + rainStatus)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(2, builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service will run until explicitly stopped
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
