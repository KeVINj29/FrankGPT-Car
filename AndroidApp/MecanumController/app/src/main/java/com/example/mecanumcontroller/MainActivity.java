package com.example.mecanumcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MecanumController";
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-0000-1101-000000000000");

    // Conexión y Estado
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectedThread mConnectedThread;
    private String address = "";
    private String lastJoystickCommand = "S";
    private String lastRightJoystickCommand = "S";
    private String lastDpadCommand = "S";
    private boolean isFullScreenFPV = false;

    // UI Components
    private TextView statusText;
    private MaterialButton btnUp, btnDown, btnLeft, btnRight, btnStop, btnAvoid, btnFollow, btnRotL, btnRotR, btnLine;
    private MaterialButton btnUL, btnUR, btnLL, btnLR, btnConnect, btnConnectCam, btnCloseCam, btnReconnectGamepad;
    private WebView videoStream;
    private View movementPad, modesPanel, connectionBar;

    // Handlers para Control Continuo
    private final Handler autoFireHandler = new Handler(Looper.getMainLooper());
    private Runnable autoFireRunnable;
    private long lastCommandTime = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        setupListeners();
        configurarStreaming();

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth no compatible", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initUI() {
        statusText = findViewById(R.id.statusText);
        btnUp = findViewById(R.id.btnUp); btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft); btnRight = findViewById(R.id.btnRight);
        btnStop = findViewById(R.id.btnStop); btnAvoid = findViewById(R.id.btnAvoid);
        btnFollow = findViewById(R.id.btnFollow);
        btnLine = findViewById(R.id.btnLine);
        btnRotL = findViewById(R.id.btnRotL); btnRotR = findViewById(R.id.btnRotR);
        btnUL = findViewById(R.id.btnUL); btnUR = findViewById(R.id.btnUR);
        btnLL = findViewById(R.id.btnLL); btnLR = findViewById(R.id.btnLR);
        btnConnect = findViewById(R.id.btnConnect);
        btnConnectCam = findViewById(R.id.btnConnectCam);
        btnCloseCam = findViewById(R.id.btnCloseCam);
        btnReconnectGamepad = findViewById(R.id.btnReconnectGamepad);
        videoStream = findViewById(R.id.videoStream);
        movementPad = findViewById(R.id.movementPad);
        modesPanel = findViewById(R.id.modesPanel);
        connectionBar = findViewById(R.id.connectionBar);

        Animation flickerAnim = AnimationUtils.loadAnimation(this, R.anim.flicker);
        statusText.startAnimation(flickerAnim);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarStreaming() {
        WebSettings ws = videoStream.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        videoStream.setWebViewClient(new WebViewClient());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        btnConnect.setOnClickListener(v -> mostrarDispositivosVinculados());
        btnConnectCam.setOnClickListener(v -> mostrarDialogoIP());
        btnCloseCam.setOnClickListener(v -> pararStreaming());
        btnReconnectGamepad.setOnClickListener(v -> {
            mostrarDispositivosVinculados();
            btnReconnectGamepad.setVisibility(View.GONE);
        });

        setupMovementButton(btnUp, "A"); setupMovementButton(btnDown, "B");
        setupMovementButton(btnLeft, "C"); setupMovementButton(btnRight, "D");
        setupMovementButton(btnRotL, "E"); setupMovementButton(btnRotR, "F");
        setupMovementButton(btnUL, "G"); setupMovementButton(btnUR, "H");
        setupMovementButton(btnLL, "I"); setupMovementButton(btnLR, "J");

        if (btnLine != null) {
            btnLine.setOnClickListener(v -> {
                detenerTodo(); // Detiene cualquier hilo de repetición de movimiento manual
                enviarComando("X");
                Toast.makeText(this, "Modo Seguidor de Línea: ON", Toast.LENGTH_SHORT).show();
            });
        }

        btnAvoid.setOnClickListener(v -> enviarComando("T"));
        btnFollow.setOnClickListener(v -> enviarComando("W"));
        btnStop.setOnClickListener(v -> { detenerTodo(); enviarComando("S"); });
    }

    private void mostrarDialogoIP() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("IP ESP32-CAM");
        final EditText input = new EditText(this);
        input.setHint("192.168.4.1");
        builder.setView(input);
        builder.setPositiveButton("CONECTAR", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (ip.isEmpty()) ip = "192.168.4.1";
            iniciarStreaming(ip);
        });
        builder.show();
    }

    private void iniciarStreaming(String ip) {
        String url = "http://" + ip;
        videoStream.loadUrl(url);
        videoStream.setVisibility(View.VISIBLE);
        btnCloseCam.setVisibility(View.VISIBLE);

        if (isControllerConnected()) {
            activarFPVTotal();
        }
    }

    private void pararStreaming() {
        videoStream.loadUrl("about:blank");
        videoStream.setVisibility(View.GONE);
        btnCloseCam.setVisibility(View.GONE);
        if (isFullScreenFPV) recreate();
    }

    private void activarFPVTotal() {
        isFullScreenFPV = true;
        movementPad.setVisibility(View.GONE);
        modesPanel.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        connectionBar.setVisibility(View.GONE);
        btnCloseCam.setVisibility(View.GONE);

        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) videoStream.getLayoutParams();
        lp.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
        lp.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
        lp.setMargins(0,0,0,0);
        videoStream.setLayoutParams(lp);
        Toast.makeText(this, "MODO FPV TOTAL - R2 para volver", Toast.LENGTH_LONG).show();
    }

    private void conectarBluetooth() {
        if (address.isEmpty()) return;
        new Thread(() -> {
            try {
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return;

                try {
                    btSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                    btSocket.connect();
                } catch (IOException e) {
                    btSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                    btSocket.connect();
                }

                runOnUiThread(() -> {
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                    statusText.setText("SYSTEM_ONLINE");
                    statusText.setTextColor(0xFF00FF00);
                    Toast.makeText(this, "Robot Conectado", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("CONNECTION_ERROR");
                    statusText.setTextColor(0xFFFF1744);
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mostrarDispositivosVinculados();
        }
    }

    private void mostrarDispositivosVinculados() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
            return;
        }
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            List<String> names = new ArrayList<>();
            final List<String> addresses = new ArrayList<>();
            for (BluetoothDevice d : pairedDevices) { names.add(d.getName()); addresses.add(d.getAddress()); }
            new AlertDialog.Builder(this).setTitle("Seleccionar Robot").setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
                address = addresses.get(which);
                conectarBluetooth();
            }).show();
        }
    }

    private boolean isControllerConnected() {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int id : deviceIds) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null) continue;
            int sources = device.getSources();
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMovementButton(MaterialButton btn, String comando) {
        if (btn == null) return;
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                detenerTodo();
                autoFireRunnable = () -> { enviarComando(comando); autoFireHandler.postDelayed(autoFireRunnable, 150); };
                autoFireHandler.post(autoFireRunnable);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                detenerTodo(); enviarComando("S");
            }
            return false;
        });
    }

    private void detenerTodo() { if (autoFireRunnable != null) autoFireHandler.removeCallbacks(autoFireRunnable); }

    private synchronized void enviarComando(String letra) {
        if (mConnectedThread != null) mConnectedThread.write("%" + letra + "#");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_BUTTON) != 0 && event.getRepeatCount() == 0) {
            if (isFullScreenFPV && keyCode == KeyEvent.KEYCODE_BUTTON_R2) { recreate(); return true; }
            detenerTodo();
            switch (keyCode) {
                case KeyEvent.KEYCODE_BUTTON_L1: enviarComando("E"); return true;
                case KeyEvent.KEYCODE_BUTTON_R1: enviarComando("F"); return true;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    if (btnReconnectGamepad.getVisibility() == View.VISIBLE) {
                        btnReconnectGamepad.setVisibility(View.GONE);
                    } else {
                        btnReconnectGamepad.setVisibility(View.VISIBLE);
                    }
                    return true;
                case KeyEvent.KEYCODE_BUTTON_Y:  enviarComando("X"); return true; // Linea
                case KeyEvent.KEYCODE_BUTTON_X:  enviarComando("W"); return true; // Follow
                case KeyEvent.KEYCODE_BUTTON_A:  enviarComando("T"); return true; // Evitador
                case KeyEvent.KEYCODE_BUTTON_B:  enviarComando("S"); return true; // Salir (Stop)
                case KeyEvent.KEYCODE_DPAD_UP:    enviarComando("A"); return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:  enviarComando("B"); return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:  enviarComando("C"); return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: enviarComando("D"); return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_BUTTON) != 0) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BUTTON_L1: case KeyEvent.KEYCODE_BUTTON_R1:
                case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
                    enviarComando("S"); return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && event.getAction() == MotionEvent.ACTION_MOVE) {
            float xLeft = event.getAxisValue(MotionEvent.AXIS_X);
            float yLeft = event.getAxisValue(MotionEvent.AXIS_Y);
            double magLeft = Math.hypot(xLeft, yLeft);
            String leftCommand = "S";
            if (magLeft > 0.25) {
                detenerTodo();
                double angle = Math.toDegrees(Math.atan2(-yLeft, xLeft));
                if (angle < 0) angle += 360;
                if (angle >= 337.5 || angle < 22.5)       leftCommand = "D";
                else if (angle >= 22.5  && angle < 67.5)  leftCommand = "H";
                else if (angle >= 67.5  && angle < 112.5) leftCommand = "A";
                else if (angle >= 112.5 && angle < 157.5) leftCommand = "G";
                else if (angle >= 157.5 && angle < 202.5) leftCommand = "C";
                else if (angle >= 202.5 && angle < 247.5) leftCommand = "I";
                else if (angle >= 247.5 && angle < 292.5) leftCommand = "B";
                else if (angle >= 292.5 && angle < 337.5) leftCommand = "J";
            }
            if (!leftCommand.equals(lastJoystickCommand)) { enviarComando(leftCommand); lastJoystickCommand = leftCommand; }
            float xRight = event.getAxisValue(MotionEvent.AXIS_Z);
            if (xRight == 0) xRight = event.getAxisValue(MotionEvent.AXIS_RX);
            String rightCommand = "S";
            if (xRight < -0.3) rightCommand = "E"; else if (xRight > 0.3) rightCommand = "F";
            if (!rightCommand.equals(lastRightJoystickCommand)) { enviarComando(rightCommand); lastRightJoystickCommand = rightCommand; }
            float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
            String currentDpad = "S";
            if (hatY == -1) currentDpad = "A"; else if (hatY == 1) currentDpad = "B";
            else if (hatX == -1) currentDpad = "C"; else if (hatX == 1) currentDpad = "D";
            if (!currentDpad.equals(lastDpadCommand)) { if (!currentDpad.equals("S")) detenerTodo(); enviarComando(currentDpad); lastDpadCommand = currentDpad; }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onPause() { super.onPause(); detenerTodo(); enviarComando("S"); disconnectBluetooth(); }
    @Override
    protected void onDestroy() { super.onDestroy(); detenerTodo(); disconnectBluetooth(); }

    private void disconnectBluetooth() {
        if (mConnectedThread != null) { mConnectedThread.cancel(); mConnectedThread = null; }
        try { if (btSocket != null) btSocket.close(); } catch (IOException e) {}
        btSocket = null;
    }

    private class ConnectedThread extends Thread {
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            OutputStream tmp = null;
            try { tmp = socket.getOutputStream(); } catch (IOException e) {}
            mmOutStream = tmp;
        }
        public void write(String input) {
            try { if (mmOutStream != null) mmOutStream.write(input.getBytes()); } catch (IOException e) {}
        }
        public void cancel() { try { if (mmOutStream != null) mmOutStream.close(); } catch (IOException e) {} }
    }
}
