# 🤖 Jarvis AI - Android Autonomous Voice Assistant

**Jarvis AI** is a next-generation Android AI voice companion inspired by advanced AI assistants like **LIA AI**. It features an animated 3D Glowing Core Reactor, real-time voice streaming with Gemini AI, and native Android device function-calling automation.

---

## ⚡ Core Features

1. **3D Glowing Core Reactor (Inspired by LIA AI "Forge")**
   - Interactive visual core powered by HTML5/Three.js Canvas rendering.
   - Dynamic real-time states: `IDLE` (steady blue pulse), `LISTENING` (cyan ripple wave), `THINKING` (fast purple gyro spin), `SPEAKING` (emerald sound react).

2. **Google Gemini LLM Intelligence**
   - Integrated with Gemini 1.5 Flash for lowest latency conversational intelligence.
   - Supports natural voice conversations in **English, Hindi, and Hinglish**.

3. **Autonomous Device Function Calling**
   - 📱 **App Launcher**: Opens apps on voice command (*"YouTube kholo"*, *"Camera open karo"*).
   - 📞 **Direct Calls**: Calls phone numbers/contacts (*"Rahul ko call lagao"*).
   - ⏰ **Alarms & Timers**: Sets alarms directly via Android AlarmManager (*"Kal subah 7 baje ka alarm lagao"*).
   - 🌐 **Web Search**: Instant online searches via Google Assistant search intents.
   - 💬 **WhatsApp Automation**: Prefills and sends WhatsApp messages.

4. **Background Voice Foreground Service**
   - `JarvisVoiceService` maintains an ongoing notification and microphone audio pipeline for background responsiveness.

---

## 🚀 How to Build APK via GitHub Actions

A ready-to-run GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. You do not need to install Android SDK or heavy tools on your phone!

### Step 1: Initialize Git and Commit
Inside Termux, run:
```bash
cd /data/user/0/com.termux/files/home/.gemini/antigravity-cli/scratch/jarvis-ai
git init
git add .
git commit -m "Initial commit of Jarvis AI Android app"
```

### Step 2: Push to Your GitHub Account
Create a new repository on GitHub (e.g. `Jarvis-AI`), then link and push:
```bash
git branch -M main
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/Jarvis-AI.git
git push -u origin main
```

### Step 3: Download Your APK
1. Open your repository on GitHub in any browser.
2. Click on the **Actions** tab at the top.
3. You will see the workflow **"Build Jarvis AI APK"** running.
4. Once it completes (green checkmark, ~2-3 minutes), click on the run.
5. Under the **Artifacts** section at the bottom, click on **`JarvisAI-Debug-APK`**.
6. Download the zip file, extract it, and install `app-debug.apk` directly on your Android phone!

---

## 🔑 Setup in the App
1. When you first open Jarvis AI, tap the **Settings (⚙️)** icon in the top right.
2. Paste your free Google Gemini API Key from [Google AI Studio](https://aistudio.google.com).
3. Tap **Save Key**.
4. Grant Microphone and Phone permissions when prompted, and Jarvis is online!
