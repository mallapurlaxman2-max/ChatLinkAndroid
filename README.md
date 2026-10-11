# ChatLink Android Chat Application

**Owner:** You (the recipient of this project)  
**Package:** `com.lucky.chatlink`  
**Version:** 1.0

## Features
- Native Android chat UI
- Host and Join modes over TCP sockets
- Connection status indicator
- Send and receive messages concurrently
- Disconnect / Exit control
- No third-party libraries or account required

## How to use
1. Install the same app on two Android phones.
2. Connect both phones to the same Wi-Fi network.
3. On phone A, tap **Host chat** and keep the app open.
4. Find phone A's Wi-Fi IP in Android Wi-Fi network details.
5. On phone B, enter that IP in the field and tap **Join chat**.
6. Send messages on either phone. Tap **Disconnect / Exit** to leave.

For testing on a single Android device, host and client must run as two separate app instances/processes; easiest is to use two phones on the same Wi-Fi.

## Build APK in Android Studio
1. Install Android Studio on a computer.
2. Open this extracted `ChatLinkAndroid` folder as a project.
3. Allow Gradle sync and install any SDK platform it requests (API 35).
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio will show the APK path, normally `app/build/outputs/apk/debug/app-debug.apk`.

## Important network notes
- This is a basic educational LAN chat. It does not encrypt messages, authenticate users, store chat history, or work across the public internet.
- Both phones should be on the same Wi-Fi; some guest Wi-Fi networks block device-to-device connections.
- If Android prompts about network access, allow it. If the connection fails, check the host IP and that port 12345 is not blocked.

## Source ownership
The source files are provided for you to use, modify, and distribute. No third-party app libraries are included. You may use your own name as the project author in your college submission.
