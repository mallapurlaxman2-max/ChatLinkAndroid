package com.lucky.chatlink;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int PORT = 12345;
    private LinearLayout root, messages;
    private EditText ipInput, messageInput;
    private TextView status;
    private Button hostButton, joinButton, sendButton, exitButton;
    private volatile boolean running = false;
    private volatile Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread networkThread;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        setStatus("Not connected", "#64748b");
        addSystemMessage("Welcome to ChatLink. Host on one phone and join from another phone on the same Wi-Fi.");
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    private android.graphics.drawable.GradientDrawable bg(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(12));
        root.setBackgroundColor(Color.rgb(245,247,251));

        TextView title = text("ChatLink", 28, Color.rgb(20,35,65));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("Simple peer-to-peer chat", 14, Color.rgb(100,116,139));
        root.addView(subtitle);

        status = text("Not connected", 14, Color.WHITE);
        status.setPadding(dp(12), dp(9), dp(12), dp(9));
        status.setBackground(bg(Color.rgb(100,116,139), 12));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(14); sp.bottomMargin = dp(12);
        root.addView(status, sp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        hostButton = button("Host chat", Color.rgb(37,99,235));
        joinButton = button("Join chat", Color.rgb(15,118,110));
        actions.addView(hostButton, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams jp = new LinearLayout.LayoutParams(0, dp(46), 1);
        jp.leftMargin = dp(8); actions.addView(joinButton, jp);
        root.addView(actions);

        ipInput = new EditText(this);
        ipInput.setSingleLine(true);
        ipInput.setHint("Host IP address (e.g. 192.168.1.5)");
        ipInput.setTextSize(14);
        ipInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        ipInput.setPadding(dp(12), 0, dp(12), 0);
        ipInput.setBackground(bg(Color.WHITE, 10));
        LinearLayout.LayoutParams ipp = new LinearLayout.LayoutParams(-1, dp(48));
        ipp.topMargin = dp(10); ipp.bottomMargin = dp(12);
        root.addView(ipInput, ipp);

        TextView chatLabel = text("MESSAGES", 12, Color.rgb(100,116,139));
        chatLabel.setTypeface(null, Typeface.BOLD);
        root.addView(chatLabel);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(bg(Color.WHITE, 12));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(10), dp(10), dp(10), dp(10));
        scroll.addView(messages);
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(-1, 0, 1);
        scp.topMargin = dp(7); scp.bottomMargin = dp(10);
        root.addView(scroll, scp);

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        messageInput = new EditText(this);
        messageInput.setSingleLine(true);
        messageInput.setHint("Type a message...");
        messageInput.setTextSize(15);
        messageInput.setPadding(dp(12), 0, dp(12), 0);
        messageInput.setBackground(bg(Color.WHITE, 10));
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, dp(48), 1));
        sendButton = button("Send", Color.rgb(37,99,235));
        LinearLayout.LayoutParams sendp = new LinearLayout.LayoutParams(dp(78), dp(48));
        sendp.leftMargin = dp(8); composer.addView(sendButton, sendp);
        root.addView(composer);

        exitButton = button("Disconnect / Exit", Color.rgb(185,28,28));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(44));
        ep.topMargin = dp(10); root.addView(exitButton, ep);

        setContentView(root);

        hostButton.setOnClickListener(v -> startHosting());
        joinButton.setOnClickListener(v -> joinHost());
        sendButton.setOnClickListener(v -> sendMessage());
        exitButton.setOnClickListener(v -> disconnect(true));
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(13);
        b.setAllCaps(false); b.setBackground(bg(color, 10));
        return b;
    }

    private void setStatus(String value, String colorHex) {
        runOnUiThread(() -> {
            status.setText("●  " + value);
            try { status.setBackground(bg(Color.parseColor(colorHex), 12)); } catch (Exception ignored) {}
        });
    }

    private void addSystemMessage(String message) { addBubble("SYSTEM", message, false); }
    private void addBubble(String who, String message, boolean mine) {
        runOnUiThread(() -> {
            TextView t = text(who + "\n" + message, 14, mine ? Color.WHITE : Color.rgb(30,41,59));
            t.setPadding(dp(11), dp(8), dp(11), dp(8));
            t.setBackground(bg(mine ? Color.rgb(37,99,235) : Color.rgb(241,245,249), 12));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.topMargin = dp(5); p.bottomMargin = dp(5);
            p.gravity = mine ? Gravity.RIGHT : Gravity.LEFT;
            t.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.78));
            messages.addView(t, p);
            if (messages.getParent() instanceof ScrollView) {
                ScrollView sv = (ScrollView) messages.getParent();
                sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void startHosting() {
        if (running) { addSystemMessage("Already connected or waiting."); return; }
        running = true;
        setStatus("Hosting — waiting for a client on port " + PORT, "#b45309");
        addSystemMessage("Host started. Keep this screen open. Give the other person your phone's Wi-Fi IP.");
        hostButton.setEnabled(false);
        networkThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                Socket accepted = serverSocket.accept();
                if (!running) { accepted.close(); return; }
                socket = accepted;
                setupStreams();
                setStatus("Connected to " + accepted.getInetAddress().getHostAddress(), "#15803d");
                addSystemMessage("Client connected.");
                listenLoop();
            } catch (Exception e) {
                if (running) addSystemMessage("Host error: " + e.getMessage());
                setStatus("Disconnected", "#64748b");
            } finally { closeNetwork(); }
        }, "chat-host");
        networkThread.start();
    }

    private void joinHost() {
        if (running) { addSystemMessage("Already connected or waiting."); return; }
        String host = ipInput.getText().toString().trim();
        if (host.isEmpty()) { ipInput.setError("Enter the host phone's Wi-Fi IP"); return; }
        running = true;
        setStatus("Connecting to " + host + ":" + PORT, "#b45309");
        networkThread = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, PORT), 8000);
                socket = s;
                setupStreams();
                setStatus("Connected to " + host, "#15803d");
                addSystemMessage("Connected to host.");
                listenLoop();
            } catch (Exception e) {
                if (running) addSystemMessage("Connection failed: " + e.getMessage());
                setStatus("Connection failed", "#b91c1c");
            } finally { closeNetwork(); }
        }, "chat-client");
        networkThread.start();
    }

    private void setupStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private void listenLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.equals("/__CHATLINK_EXIT__")) {
                    addSystemMessage("The other person disconnected.");
                    break;
                }
                addBubble("THEY", line, false);
            }
        } catch (IOException e) {
            if (running) addSystemMessage("Connection lost.");
        }
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;
        if (!running || writer == null || socket == null || socket.isClosed()) {
            addSystemMessage("Connect first. Tap Host chat on one phone or Join chat on the other.");
            return;
        }

        // Run network transmission in a background thread to prevent NetworkOnMainThreadException
        new Thread(() -> {
            try {
                writer.println(message.replace("\n", " "));
                if (writer.checkError()) throw new IOException("Could not send message");
                runOnUiThread(() -> {
                    addBubble("YOU", message, true);
                    messageInput.setText("");
                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addSystemMessage("Send failed: " + e.getMessage());
                    disconnect(false);
                });
            }
        }).start();
    }

    private void closeNetwork() {
        running = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        reader = null; writer = null; socket = null; serverSocket = null;
        runOnUiThread(() -> {
            hostButton.setEnabled(true);
            setStatus("Not connected", "#64748b");
        });
    }

    private void disconnect(boolean tellPeer) {
        running = false;
        new Thread(() -> {
            try {
                if (tellPeer && writer != null) writer.println("/__CHATLINK_EXIT__");
            } catch (Exception ignored) {}
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        }).start();

        addSystemMessage("Disconnected.");
        setStatus("Not connected", "#64748b");
        hostButton.setEnabled(true);
    }

    @Override public void onBackPressed() {
        disconnect(true);
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        disconnect(true);
        super.onDestroy();
    }
}
