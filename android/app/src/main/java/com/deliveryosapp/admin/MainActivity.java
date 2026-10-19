package com.deliveryosapp.admin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // Debe coincidir con PUSH_CHANNEL_ORDERS en app.py del servidor DeliveryOS.
    // Los canales de Android son inmutables: para cambiar sonido o vibracion hay que
    // crear un id nuevo (pedidos_v4, ...) y borrar el anterior.
    private static final String ORDERS_CHANNEL = "pedidos_v3";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createOrdersChannel();
    }

    private void createOrdersChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        // Canales de builds anteriores (sonido default / vibracion corta)
        nm.deleteNotificationChannel("pedidos");
        nm.deleteNotificationChannel("pedidos_v2");
        if (nm.getNotificationChannel(ORDERS_CHANNEL) != null) return;

        NotificationChannel ch = new NotificationChannel(ORDERS_CHANNEL, "Pedidos nuevos",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Pedidos por aceptar o rechazar");
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/pedido");
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build();
        ch.setSound(sound, attrs);
        ch.enableVibration(true);
        // ~8 s de vibracion insistente, a la par del timbre pedido.wav
        ch.setVibrationPattern(new long[]{0, 700, 300, 700, 300, 700, 300, 700, 300, 700, 300, 700, 300, 700, 300, 700});
        ch.enableLights(true);
        ch.setLightColor(Color.parseColor("#FF6B00"));
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(true);
        nm.createNotificationChannel(ch);
    }
}
