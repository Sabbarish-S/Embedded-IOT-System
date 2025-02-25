package com.example.tarp_project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "RainAlertChannel";
    private boolean alertTriggered = false; // Flag to track if alert has been triggered

    private TextView temperatureText, altitudeText, humidityText, pressureText, rainIndicationText;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private DatabaseReference result;

    String rainIndication;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent(this, com.example.tarp_project.RainAlertService.class);
        startService(serviceIntent);

        // Initialize the views
        temperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        pressureText = findViewById(R.id.pressureText);
        rainIndicationText = findViewById(R.id.rainIndicationText);
        altitudeText = findViewById(R.id.altitudeText);

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("sensors");
        result = database.getReference("predictions");

        // Create notification channel
        createNotificationChannel();

        // Retrieve data from Firebase in real-time
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("DataSnapshot", "onDataChange: " + dataSnapshot.toString());
                double temperature = dataSnapshot.child("temperature").getValue(double.class);
                double altitude = dataSnapshot.child("altitude").getValue(double.class);
                double humidity = dataSnapshot.child("humidity").getValue(double.class);
                double pressure = dataSnapshot.child("pressure").getValue(double.class);
                rainIndication = dataSnapshot.child("rain_status").getValue(String.class);

                temperatureText.setText("Temperature: " + temperature + "°C");
                altitudeText.setText("Altitude: " + altitude + " m");
                humidityText.setText("Humidity: " + humidity + "%");
                pressureText.setText("Pressure: " + pressure + " hPa");
                rainIndicationText.setText("Rain Indication: " + rainIndication);

                // Check rain indication and trigger alert if not already triggered
                if (("Light rain detected".equalsIgnoreCase(rainIndication) || "Moderate rain detected".equalsIgnoreCase(rainIndication) || "Heavy rain detected".equalsIgnoreCase(rainIndication)) && !alertTriggered) {
                    sendRainAlertNotification();
                    playBeepSound();
                    alertTriggered = true; // Set flag to true to prevent multiple alerts
                } else if ("no rain".equalsIgnoreCase(rainIndication)) {
                    // Reset the flag when rain stops
                    alertTriggered = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle potential read failure
                Log.w("DatabaseError", "loadPost:onCancelled", error.toException());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Rain Alert Channel";
            String description = "Channel for Rain Alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendRainAlertNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.google.android.gms.base.R.drawable.common_google_signin_btn_icon_dark) // Make sure to add an alert icon in res/drawable
                .setContentTitle("Rain Alert")
                .setContentText("Rain indication is "+rainIndication)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void playBeepSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.beep); // Replace 'beep' with your sound file name
        mediaPlayer.setOnCompletionListener(mp -> mp.release()); // Release the MediaPlayer when done
        mediaPlayer.start(); // Start playing the beep sound
    }
}
