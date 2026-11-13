# AI Cash Counter — Android project

This is a native Android starter app based on the uploaded **Ai cash counter** reference document.

## Included
- Indian denominations: ₹500, ₹200, ₹100, ₹50, ₹20, ₹10, ₹5, ₹2 and ₹1
- Tap + / −, or type the number of notes/coins
- Live grand total and total pieces
- Amount in Indian number words
- Extra amount field (for calculator results, bundles or digital payments)
- Save counts locally on the phone and view history
- Export a text receipt and copy the total
- Clear all counts

## Important AI-scan limitation
The reference document's “AI Scan” handler generates random sample counts after a delay; it does not perform image recognition. The document does not include a trained banknote-recognition model or dataset. This Android starter therefore explains that limitation instead of showing random counts as if they were real AI results. A real camera-based AI scanner is a separate next step requiring a trained model and test images.

## Build an APK
1. Open this project in Android Studio on a computer.
2. Let Gradle sync and install the Android SDK Platform 35 if prompted.
3. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

This project is source code, not a precompiled APK. It has not been built or tested on a physical device in this environment.
