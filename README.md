<div align="center">

<img src="https://img.shields.io/badge/Dopamine-YouTube%20Clone-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="Dopamine"/>

# 🎬 Dopamine

### *Stream All Premium Features of YouTube — Without the Price Tag.*

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Material3](https://img.shields.io/badge/UI-Material%203-757575?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Hilt](https://img.shields.io/badge/DI-Hilt-FF6F00?style=flat-square&logo=google&logoColor=white)](https://dagger.dev/hilt)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen?style=flat-square)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

</div>

---

## 📥 Download APK

<p align="center">
  <a href="https://github.com/rajatt04/Dopamine/releases/download/1.0/app-universal-release.apk">
    <img src="https://img.shields.io/badge/Download-Dopamine%20APK-FF6B6B?style=for-the-badge&logo=android&logoColor=white" />
  </a>
</p>
<p align="center">
  ⚡ Fast • 🎯 Minimal • 📱 Built with Kotlin & MVVM
</p>

---

## 📖 About

**Dopamine** is a feature-rich YouTube client for Android, engineered from the ground up with modern Android architecture in mind. It delivers a clean, ad-free, distraction-free experience with a stunning Material 3 UI — powered by premium typography and a thoughtfully crafted design system.

> *Why settle for the default when you can have Dopamine?*

---

## ✨ Features

| Feature | Description |
|---|---|
| 🏠 **Home Feed** | Curated trending videos, categorized by Tech, Sports, and Coding playlists |
| 🔍 **Search** | Full YouTube search with chip filters and a sleek results grid |
| 📺 **YouTube Player** | Embedded, full-featured YouTube player powered by the official Android YouTube Player library |
| 📱 **Shorts** | Dedicated Shorts feed with vertical swipe navigation |
| 📡 **Channel Pages** | Full channel profile: banner, logo, subscriber count, playlists, and descriptions |
| 🕓 **Watch History** | Offline-first local watch history with one-tap clear |
| 🎵 **Playlists** | Browse and play curated playlists per channel |
| 👨‍💻 **About Developer** | Stylish developer card fetched dynamically from a remote API |
| 🌙 **Dark Mode** | Full Day/Night theme support via Material 3 DayNight |
| ⚡ **Offline-First** | Local Room database caching for a resilient user experience |

---

## 📸 Screenshots

<div align="center">

| Home | Trending | Search | Player |
|:---:|:---:|:---:|:---:|
| ![Home](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_20260417_193440.jpg?raw=true) | ![Trending](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_20260417_193551.jpg?raw=true) | ![Search](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_20260417_193619.jpg?raw=true) | ![Player](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_20260417_193832.jpg?raw=true) |

| Channel | Playlists | Shorts | Profile |
|:---:|:---:|:---:|:---:|
| ![Channel](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_20260417_193855.jpg?raw=true) | ![Playlists](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_20260417_194002.jpg?raw=true) | ![Shorts](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_2026_0417_193705.jpg?raw=true) | ![Profile](https://github.com/rajatt04/Student-Management-System/blob/sms-main/Projects/Dopamine/Screenshot_2026_0417_193814.jpg?raw=true) |

</div>

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Material Design 3 |
| **Architecture** | MVVM + Repository Pattern |
| **Dependency Injection** | Hilt (Dagger) |
| **Networking** | Ktor Client (CIO engine) |
| **Serialization** | Kotlinx Serialization |
| **Image Loading** | Glide |
| **Local Database** | Room (with KTX + LiveData) |
| **Video Playback** | Android YouTube Player |
| **Async** | Kotlin Coroutines + LiveData |
| **Build System** | Gradle (Version Catalog) |

---

## 🏗️ Architecture

Dopamine is built on **Clean MVVM** with a strict separation of concerns:

```
app/
├── activities/        # UI Layer — Activities & Fragments
├── fragments/
├── adapters/          # RecyclerView Adapters
├── viewModels/        # ViewModel Layer — business logic
├── utilities/         # Helpers, constants

Youtube/ (module)
├── repository/        # Data Layer — Ktor API calls
├── viewModels/        # Module-level ViewModels
├── model/             # Kotlinx Serializable data models
├── utilities/         # YoutubeClient, Resource wrapper

Database/ (module)
├── dao/               # Room DAO interfaces
├── di/                # Hilt Database Module
├── repository/        # Database Repository
├── viewModel/         # DatabaseViewModel
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio **Hedgehog** or later
- JDK 17+
- A valid **YouTube Data API v3** key from [Google Cloud Console](https://console.cloud.google.com)

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/rajatt04/Dopamine.git
   cd Dopamine
   ```

2. **Add your API keys to `local.properties`:**
   ```properties
   API_KEYS=your_primary_api_key
   EXTRA_KEYS=your_backup_api_key
   ```

3. **Build & Run** via Android Studio or:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📦 Release

**v1.0 — Stable** (`version dopamine_20240704_01.phone.stable.dynamic`)

- ✅ Hilt Dependency Injection fully integrated
- ✅ Material 3 UI with premium custom typography
- ✅ Lottie removed — replaced with native Material components
- ✅ Offline-first Room database for watch history
- ✅ Multi-ABI APK splits (`x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a`)
- ✅ Firebase & ExoMedia fully removed — clean architecture

---

## 👨‍💻 Author

<div align="center">

**🌊 Rajat Kevat**

[![Portfolio](https://img.shields.io/badge/Portfolio-rajatt04.github.io-0A66C2?style=flat-square&logo=github&logoColor=white)](https://rajatt04.github.io)
[![Email](https://img.shields.io/badge/Email-kevatrajat29%40gmail.com-EA4335?style=flat-square&logo=gmail&logoColor=white)](mailto:kevatrajat29@gmail.com)

*Crafted with ❤️ and a lot of late nights.*

</div>

---

<div align="center">

**If you found this project useful, drop a ⭐ on the repo — it means a lot!**

</div>
