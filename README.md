# 🎯 Vision Hunt Online

An interactive Android game leveraging On-Device Computer Vision & Artificial Intelligence (AI) that connects real-world physical object hunting with live events and competitive leaderboards.

---

## 🎮 Game Mechanics

1. **Real-World Object Hunting**: Players aim their device's camera at required real-world objects to detect and discover them.
2. **Rarity & Points System**:
   * **Common**: 10 Points
   * **Uncommon**: 20 Points
   * **Rare**: 30 Points
3. **AI Vision Detection**: Real-time live camera analysis (EfficientDet-Lite3) with an 85%+ confidence threshold for automatic task completion and verification.
4. **Events & Leaderboards**: Live seasonal quests and competitive leaderboards with real-time score synchronization.

---

## 🛠️ Tech Stack

* **Language & Platform**: Native Android (Kotlin), Material Design 3.
* **On-Device Artificial Intelligence**: TensorFlow Lite (`efficientdet_lite3.tflite`).
* **Camera & Visualization**: AndroidX CameraX API, Custom Canvas Bounding-Box Overlay.
* **Architecture & State**: MVVM Architecture, Kotlin Coroutines & StateFlow, View Binding.
* **Storage & Networking**: SQLite Local Database, Native HttpURLConnection REST API with PHP Backend.
* **UI/UX Design**: 3D Layering Game UI, Adaptive Light/Dark Theme, Swipe-to-Dismiss Banners.

---

## 🔒 Fair Play & Privacy

* **Local Image Storage**: Captured photos are saved locally on the player's device for privacy verification before prize distribution.
* **Fair Play Anti-Cheat**: Photographed items must be genuine real-world objects; digital screens, books, or printed photos are prohibited.
