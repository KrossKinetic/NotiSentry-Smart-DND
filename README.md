# NotiSentry

NotiSentry is a modern Android application designed to help users reclaim their focus by intelligently managing, filtering, and summarizing notifications. Built with the latest Android technologies, this app provides a robust solution to notification overload.

## ✨ Features

* **Focus Mode**: A strict whitelisting system where users can select specific apps they want to receive notifications from. All other notifications are silently blocked and saved.
* **Intelligent Summarization**: Utilizes on-device AI (Gemini Nano via ML Kit) to generate concise, bullet-pointed summaries of all blocked notifications.
* **Organized Summary View**: Blocked notification summaries are neatly categorized by day ("Today," "Yesterday," and "Archives") in an expandable list for easy viewing.
* **Persistent History**: All blocked notifications and generated summaries are saved persistently on the device using a Room database, ensuring no data is lost when the app is closed.
* **Modern, Clean UI**: Built entirely with Jetpack Compose and Material 3 design principles for a beautiful and intuitive user experience.
* **Privacy-Focused**: All notification processing and AI summarization happens entirely on the user's device. No notification data ever leaves the phone.

---
## 🏛️ Architecture

NotiSentry is built using a modern, scalable, and testable architecture based on Google's official recommendations. It follows a unidirectional data flow pattern, with the Repository acting as the single source of truth for all application data.

+----------------+      +------------------+      +------------------+      +---------+
|                |      |                  |      |                  |      |         |
|  Android OS    |----->| NotiSentryService|----->|   Application    |----->|  Room   |
| (Notifications)|      |    (Worker)      |      |   Repository     |      | Database|
|                |      |                  |      | (Source of Truth)|      | &       |
+----------------+      +------------------+      |                  |<-----|DataStore|
                                                  +--------^---------+      +---------+
                                                           |
                                                           | (Collects Flows)
                                                           |
                                                  +--------v----------+
                                                  |                  |
                                                  |   AppViewModel   |
                                                  | (UI State Holder)|
                                                  +--------^----------+
                                                           |
                                                           | (Sends Events Up,
                                                           |  Receives State Down)
                                                           |
                                                  +--------v---------+
                                                  |                  |
                                                  |   Compose UI     |
                                                  |   (The Screen)   |
                                                  +------------------+

---
## 🛠️ Tech Stack & Key Libraries

* **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 components.
* **Architecture**: MVVM (Model-View-ViewModel) with a Repository pattern.
* **Asynchronicity**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html) for managing background tasks and reactive data streams.
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) for managing dependencies and creating a scalable, testable codebase.
* **Database**: [Room](https://developer.android.com/jetpack/androidx/releases/room) for persistent, on-device storage of notifications and summaries.
* **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for navigating between screens.
* **Settings**: [Jetpack DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) for persistently storing simple user preferences.
* **On-Device AI**: [ML Kit Summarization API](https://developers.google.com/ml-kit/language/summarization) (powered by Gemini Nano).
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for efficiently loading application icons.
* **Background Service**: `NotificationListenerService` to capture system-wide notifications.

---
## 🚀 Setup & Configuration

1.  **Clone the repository.**
2.  **Open in Android Studio.**
3.  **Build the project:** Gradle will automatically download and sync all the necessary dependencies.
4.  **Run the app:** On an emulator or physical device.
5.  **Grant Permission:** On first launch, the app will guide you to the system settings to grant "Notification Access," which is required for the core functionality.
