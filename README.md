# 🎯 Vision Hunt Online

An interactive Android game leveraging On-Device Computer Vision & Artificial Intelligence (AI) that connects real-world physical object hunting with live events and competitive leaderboards.

---

## 🎮 Game Mechanics

1. **Real-World Object Hunting**: Players aim their device's camera at required real-world objects to detect and discover them.
2. **Rarity & Points System**:
   * **Common**: 10 Points
   * **Uncommon**: 20 Points
   * **Rare**: 30 Points
3. **AI Vision Models**:
   * **Live Continuous Scan (EfficientDet-Lite3)**: Real-time live camera analysis with an 85%+ confidence threshold for automatic task completion.
   * **On-Demand Capture Scan (YOLO World ONNX)**: Freeze-frame image analysis with bounding box displays for detections above 30%+, requiring a 75%+ confidence score to complete tasks.
4. **Events & Leaderboards**: Live seasonal quests and competitive leaderboards with real-time score synchronization.

---

## 🛠️ Tech Stack

* **Language & Platform**: Native Android (Kotlin), Material Design 3.
* **On-Device Artificial Intelligence**:
  * TensorFlow Lite (`efficientdet_lite3.tflite`)
  * ONNX Runtime Android (`yolo_world_int8.onnx`)
* **Camera & Visualization**: AndroidX CameraX API, Custom Canvas Bounding-Box Overlay.
* **Architecture & State**: MVVM Architecture, Kotlin Coroutines & StateFlow, View Binding.
* **Storage & Networking**: SQLite Local Database, Native HttpURLConnection REST API with PHP Backend.
* **UI/UX Design**: 3D Layering Game UI, Adaptive Light/Dark Theme, Swipe-to-Dismiss Banners.

---

## 🔒 Fair Play & Privacy

* **Local Image Storage**: Captured photos are saved locally on the player's device for privacy verification before prize distribution.
* **Fair Play Anti-Cheat**: Photographed items must be genuine real-world objects; digital screens, books, or printed photos are prohibited.
