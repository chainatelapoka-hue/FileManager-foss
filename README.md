# FileManager-foss

Android-এর জন্য একটি সরল, ওপেন-সোর্স ফাইল ম্যানেজার। Kotlin ও Jetpack Compose দিয়ে লেখা।

## ফিচার

- ফাইল ও ফোল্ডার ব্রাউজ, কপি, কাট/মুভ, পেস্ট, মুছে ফেলা, নাম পরিবর্তন
- নাম দিয়ে খোঁজা (সাব-ফোল্ডারসহ)
- সর্ট (নাম, আকার, তারিখ, ধরন) ও লুকানো ফাইল দেখা
- ZIP আনজিপ ও ZIP তৈরি
- Quick Access: Downloads, DCIM, Documents, Pictures, Movies
- Wi-Fi-তে FTP সার্ভার (Foreground Service সহ)
- Google Drive ও Dropbox সংযোগ (পরীক্ষামূলক)
- নিজের অ্যাপের APK ব্যাকআপ

## প্রয়োজনীয়তা

- Android Studio (সর্বশেষ স্থিতিশীল ভার্সন)
- Android 8.0 (API 26) বা তার উপরের ডিভাইস
- Android 11+-এ "All files access" অনুমতি প্রয়োজন

## চালানো

1. রেপো ক্লোন করুন:
   ```bash
   git clone https://github.com/chainatelapoka-hue/FileManager-foss.git
   ```
2. Android Studio-তে প্রজেক্ট খুলুন।
3. Dropbox ব্যবহার করতে চাইলে নিজের App Key বসান। এটি কোডে না রেখে `local.properties` বা `BuildConfig`-এ রাখা ভালো।
4. Run দিন।

## নিরাপত্তা ও সীমাবদ্ধতা

- FTP সার্ভার এনক্রিপশন ছাড়া কাজ করে। শুধু বিশ্বস্ত Wi-Fi-তে ব্যবহার করুন এবং শক্ত পাসওয়ার্ড দিন।
- ফোল্ডার মুছে ফেলা স্থায়ী। গুরুত্বপূর্ণ ফাইলের ব্যাকআপ রাখুন।
- Google Play-তে `MANAGE_EXTERNAL_STORAGE` অনুমতির জন্য নীতিগত যাচাই লাগতে পারে।

## লাইসেন্স

এই প্রকল্পের লাইসেন্স এখনো ঠিক করা হয়নি। প্রকাশের আগে একটি লাইসেন্স (যেমন MIT বা GPL-3.0) যোগ করুন।
