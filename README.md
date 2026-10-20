# হিসাবডেস্ক (HisaabDesk)

**অফলাইন-ফার্স্ট বাংলা ব্যক্তিগত অর্থ ও হিসাব ব্যবস্থাপনা অ্যাপ (Android)**

হিসাবডেস্ক একটি সম্পূর্ণ অফলাইন Android অ্যাপ। আপনার আয়-ব্যয়, দোকানের বাকি, ধার-দেনা,
পাওনা-দেনা, এনজিও/ব্যাংক ঋণ, সঞ্চয়, বাজেট, বিল ও রিপোর্ট — সব এক জায়গায়, আপনার ফোনেই
সংরক্ষিত। কোনো ক্লাউড অ্যাকাউন্ট, ইন্টারনেট বা বিজ্ঞাপন নেই।

| বিষয় | তথ্য |
|---|---|
| অ্যাপের নাম | হিসাবডেস্ক (HisaabDesk) |
| Application ID | `com.shohankhan.hisaabdesk` |
| সংস্করণ | ১.০.০ (versionCode 1) |
| ন্যূনতম Android | Android 8.0 (API 26) |
| Target SDK | 35 |
| ভাষা | বাংলা (বাংলাদেশ), bn-BD |
| মুদ্রা | বাংলাদেশি টাকা (৳), পূর্ণসংখ্যা পয়সা |
| থিম | শুধুমাত্র লাইট মোড (সাদা-সবুজ) |
| ডেটা | Room (SQLite), সম্পূর্ণ ডিভাইসে |
| মালিক | শোহান খান · helloiamshohan@gmail.com |

রিপোজিটরি: <https://github.com/shohanbusinessmail-cmd/hisaabdesk>

---

## বৈশিষ্ট্য

- **অ্যাকাউন্ট ও ব্যালেন্স** — নগদ, ব্যাংক, MFS, ওয়ালেট, সঞ্চয়; শুরুর ব্যালেন্স,
  অ্যাকাউন্টভিত্তিক লেনদেন, সংরক্ষণাগার, ন্যূনতম ব্যালেন্স নিয়ন্ত্রণ।
- **আয় ও ব্যয়** — ক্যাটাগরি/সাবক্যাটাগরি, ব্যক্তি/প্রতিষ্ঠান, রসিদ নম্বর, নোট,
  পরিশোধ পদ্ধতি, সম্পাদনা ও সুরক্ষিত বাতিল।
- **টাকা স্থানান্তর** — উৎস ও গন্তব্য লিংকড; কোনো আয়/ব্যয় তৈরি করে না; ফি আলাদা ব্যয়।
- **কেনাকাটা ও দোকান বাকি** — একাধিক পণ্য সারি, পরিমাণ × একক দাম, লাইন ছাড়/চার্জ,
  পাইকারি ছাড়, পরিশোধযোগ্য, আংশিক পরিশোধ, দোকানভিত্তিক সারসংক্ষেপ, overpayment নিষিদ্ধ।
- **ধার-দেনা** — ধার নেওয়া ও ধার দেওয়া আলাদা; আংশিক পরিশোধ, আসল/সুদের অংশ, মেয়াদ।
- **পাওনা ও দেনা** — আলাদা তালিকা, আংশিক আদায়/পরিশোধ, মেয়াদোত্তীর্ণ অবস্থা।
- **এনজিও ও ব্যাংক ঋণ** — কিস্তি সূচি (সাপ্তাহিক/মাসিক/নির্দিষ্ট দিন), ক্যালেন্ডার-সচেতন
  মাস-শেষ হিসাব, আসল/সুদ/ফি অংশবিভাজন, চুক্তির মোট ইনপুট, মডেল স্পষ্টভাবে চিহ্নিত।
- **সঞ্চয় ও লক্ষ্য** — জমা, উত্তোলন, মুনাফা, স্থানান্তর; জরুরি তহবিল/শিক্ষা/অন্য লক্ষ্য।
- **বাজেট** — দৈনিক/সাপ্তাহিক/মাসিক/নির্দিষ্ট, ক্যাটাগরি ও অ্যাকাউন্ট ভিত্তিক সীমা।
- **বিল ও নিয়মিত লেনদেন** — রিমাইন্ডার, পেমেন্ট ক্যালেন্ডার, "পরিশোধ রেকর্ড" ব্যবহারকারীর
  নিশ্চিতকরণে; স্বয়ংক্রিয়ভাবে লেনদেন তৈরি হয় না।
- **রিপোর্ট** — তারিখ-সীমা, ক্যাটাগরি বিভাজন, মাসিক আয়-ব্যয় বার চার্ট, সম্পদ-দায়,
  নিট সম্পদ, CSV রপ্তানি (UTF-8 BOM সহ)।
- **নিরাপত্তা** — বাধ্যতামূলক পিন (PBKDF2-HMAC-SHA256, ২,১০,০০০ পুনরাবৃত্তি, র‍্যান্ডম
  salt, Android Keystore-সুরক্ষিত), ভুল চেষ্টায় exponential backoff, স্বয়ংক্রিয় লক।
- **এনক্রিপ্টেড ব্যাকআপ** — AES-256-GCM + PBKDF2 (.hisaabdesk ফাইল), রিস্টোরের আগে
  পূর্ণ যাচাই, ব্যর্থ হলে পুরনো ডেটা অটুট (transaktion rollback)।
- **বাংলা সংখ্যা** — বাংলা/ইংরেজি अंক পছন্দ, ৳১২,৫০,০০০.০০ ধরনের দক্ষিণ-এশীয় গ্রুপিং।

---

## প্রযুক্তি স্ট্যাক

| স্তর | প্রযুক্তি |
|---|---|
| ভাষা | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 (শুধু লাইট) |
| স্থাপত্য | MVVM + Repository + Room, StateFlow, Coroutines |
| নেভিগেশন | Navigation Compose |
| ডেটাবেজ | Room 2.6.1 (SQLite), schema export |
| ব্যাকগ্রাউন্ড | WorkManager 2.9.1 |
| পছন্দ | DataStore Preferences |
| বিল্ড | Gradle 8.11.1 (wrapper), AGP 8.7.3, JDK 17 |
| টেস্ট | JUnit 4, Robolectric, Room in-memory, Truth |

পূর্ণ নির্ভরশীল তালিকা: `gradle/libs.versions.toml`।

---

## কীভাবে ক্লোন ও বিল্ড করবেন

```bash
git clone https://github.com/shohanbusinessmail-cmd/hisaabdesk.git
cd hisaabdesk
```

### Android Studio
1. Android Studio (Ladybug বা নতুন) → **Open** → `hisaabdesk` ফোল্ডার।
2. Gradle sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন (JDK 17 প্রয়োজন)।
3. **Run ▶** চাপুন (ডিভাইস/এমুলেটর API 26+)।

### কমান্ড লাইন (ডিবাগ APK)

```bash
./gradlew assembleDebug
```

আউটপুট: `app/build/outputs/apk/debug/app-debug.apk`
(ডিবাগ অ্যাপের ID: `com.shohankhan.hisaabdesk.debug` — একই সাথে ইনস্টল থাকতে পারে)।

### টেস্ট ও লিন্ট

```bash
./gradlew testDebugUnitTest      # শুধু unit টেস্ট
./gradlew lintDebug              # Android lint
./gradlew assembleDebug          # ডিবাগ APK
```

### রিলিজ APK (সাইনিং সহ)

প্রথমে `keystore.properties` তৈরি করুন (git-এ কমিট করবেন না):

```properties
storeFile=/absolute/path/to/release.p12
storePassword=...
keyAlias=hisaabdesk
keyPassword=...
storeType=PKCS12
```

তারপর:

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

CI সাইনিং: রিপোজিটরি Secrets-এ
`HISAABDESK_KEYSTORE_BASE64`, `HISAABDESK_KEYSTORE_PASSWORD`,
`HISAABDESK_KEY_ALIAS`, `HISAABDESK_KEY_PASSWORD` যোগ করুন।
বিস্তারিত: [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md)।

---

## APK ইনস্টল ও আপডেট

1. GitHub Releases পেজ থেকে সর্বশেষ `hisaabdesk-x.y.z.apk` ডাউনলোড করুন।
2. ফোনে Settings → "Install unknown apps" permission দিন (ফাইল ম্যানেজার/ব্রাউজারের জন্য)।
3. APK খুলে ইনস্টল করুন।
4. **আপডেট**: একই সাইনিং কী দিয়ে সাইন করা নতুন APK ইনস্টল করলে ডেটা অটুট থাকে।
   ভিন্ন কী দিয়ে সাইন করা হলে Android আপডেট Accept করবে না (আনইনস্টল করতে হবে,
   ডেটা মুছে যাবে) — তাই আগে ব্যাকআপ নিন।
5. নতুন ফোনে: অ্যাপ ইনস্টল → ব্যাকআপ রিস্টোর (পাসওয়ার্ড সহ)।

---

## ব্যাকআপ ও পুনরুদ্ধার

- **ব্যাকআপ তৈরি**: সেটিংস → ব্যাকআপ ও রিস্টোর → পাসওয়ার্ড (৮+ অক্ষর) → ফাইল সংরক্ষণ।
  ফাইলটি `AES-256-GCM` এনক্রিপ্টেড `.hisaabdesk` ফরম্যাটে যায়।
- **পুনরুদ্ধার**: ব্যাকআপ পাসওয়ার্ড দিন → ফাইল নির্বাচন → সারসংক্ষেপ দেখুন → নিশ্চিত করুন।
  রিস্টোর একটিমাত্র DB transaction-এ হয়; যাচাই বা রিস্টোর ব্যর্থ হলে পুরনো ডেটা অক্ষত থাকে।
- **পাসওয়ার্ড ভুলে গেলে** ফাইল খোলা সম্ভব নয়। পাসওয়ার্ড নিরাপদ জায়গায় রাখুন।
- ব্যাকআপ ফাইলে PIN সংরক্ষিত হয় না; PIN শুধু ডিভাইসে থাকে।

---

## অনুমতি (Permissions)

| অনুমতি | কেন |
|---|---|
| `POST_NOTIFICATIONS` | বিল/কিস্তির রিমাইন্ডার (ব্যবহারকারী চালু করলে; অস্বীকার করলেও অ্যাপ কাজ করে) |

**কোনো INTERNET অনুমতি নেই।** কোনো storage অনুমতি নেই (SAF ব্যবহৃত হয়)।
কোনো contacts/location/SMS/call-log অনুমতি নেই। কোনো analytics/ad SDK নেই।

---

## গোপনীয়তা ও নিরাপত্তা

- সব ডেটা অ্যাপ-প্রাইভেট Room ডেটাবেজে; `allowBackup=false` (ADB/ক্লাউড ব্যাকআপ বন্ধ)।
- PIN কখনো প্লেইন টেক্সটে সংরক্ষিত হয় না; verifier Android Keystore-এ মোড়া থাকে।
- **সীমাবদ্ধতা (সৎভাবে)**: ডেটাবেজ **at-rest এনক্রিপ্টেড নয়** (SQLCipher নেই); সুরক্ষা
  Android app sandbox + ডিভাইস এনক্রিপশনের উপর নির্ভরশীল। PIN শুধু UI লক।
  বিস্তারিত: [docs/SECURITY.md](docs/SECURITY.md)।

---

## ডেটাবেজ মাইগ্রেশন নীতি

- `HisaabDatabase` সংস্করণ বাড়ালে প্রতিটি ধাপে স্পষ্ট `Migration` যোগ করতে হবে।
- `fallbackToDestructiveMigration` **কখনোই** ব্যবহার করা হয় না।
- Schema JSON (`app/schemas/`) রিপোজিটরিতে সংরক্ষিত থাকে।
- মাইগ্রেশন টেস্ট: `app/src/androidTest/` (instrumented; এমুলেটর প্রয়োজন)।

---

## GitHub Actions

- `.github/workflows/android-ci.yml` — main-এ push, main লক্ষ্য PR, manual dispatch:
  wrapper validation → compile → unit tests → lint → debug APK → artifact আপলোড।
- `.github/workflows/android-release.yml` — `v*.*.*` tag বা manual dispatch:
  ট্যাগ/সংস্করণ যাচাই → টেস্ট → signed release APK → apksigner verify → SHA-256 →
  GitHub Release-এ APK + checksum।
- প্রয়োজনীয় সেটিংস: Actions নিষ্ক্রিয় থাকলে চালু করুন; রিলিজের জন্য উপরে উল্লিখিত secrets।

### সংস্করণ ট্যাগ

```bash
git tag -a v1.0.1 -m "HisaabDesk 1.0.1"
git push origin v1.0.1
```

ট্যাগের সংস্করণ `app/build.gradle.kts`-এর `versionName`-এর সাথে মিলতে হবে।

---

## টেস্টিং অবস্থা

সর্বশেষ যাচাইকৃত ফলাফল: [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)।
সংক্ষেপে —

- Unit টেস্ট: মুদ্রা ফরম্যাটিং, তারিখ/মাস-শেষ, কেনাকাটার হিসাব, ঋণের কিস্তি ও
  বরাদ্দ, ব্যালেন্স, ট্রান্সফার invariant, reversal, idempotency, ব্যাকআপ crypto ও
  validation, PIN throttling।
- Database টেস্ট: Robolectric in-memory Room।
- UI টেস্ট: instrumentation (`androidTest`) — এমুলেটর উপলব্ধ না হলে CI-তে চালানো হয় না।

---

## পরিচিত সীমাবদ্ধতা

- শুধু লাইট থিম; ডার্ক মোড নেই (ইচ্ছাকৃত)।
- শুধু BDT; মাল্টি-কারেন্সি নেই।
- সঞ্চয়/ঋণের চক্রবৃদ্ধি সুদ অটো-মডেল নেই — ব্যবহারকারীর ইনপুট প্রয়োজন।
- ব্যাংক/এনজিও চুক্তি অনুমান করা হয় না; মডেল-হিসাব স্পষ্টভাবে চিহ্নিত।
- সংযুক্তি (রসিদ ছবি) সংরক্ষণের মৌলিক ভিত্তি আছে; ফাইল কপি ও UI পিকার এখনো
  সম্পূর্ণ নয় (docs/PROJECT_STATUS.md দেখুন)।
- merge-mode রিস্টোর নেই; শুধু পূর্ণ প্রতিস্থাপন (ডকুমেন্টেড)।
- এমুলেটরে UI টেস্ট চালানো হয়নি (স্যান্ডবক্সে এমুলেটর নেই)।

---

## লাইসেন্স

এই প্রকল্পের সোর্স কোড **MIT License** — [LICENSE](LICENSE)।
তৃতীয় পক্ষের লাইব্রেরি নিজ নিজ লাইসেন্সের অধীন — [docs/THIRD_PARTY_LICENSES.md](docs/THIRD_PARTY_LICENSES.md)।

## মালিক ও যোগাযোগ

শোহান খান · helloiamshohan@gmail.com
