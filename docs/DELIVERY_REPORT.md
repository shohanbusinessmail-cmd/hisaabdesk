# চূড়ান্ত ডেলিভারি রিপোর্ট (Delivery Report)

তারিখ: ২০২৬-১০-২০ · প্রকল্প: হিসাবডেস্ক (HisaabDesk) ১.০.১

> এই রিপোর্টে প্রতিটি দাবির পাশে তার প্রমাণের ধরন আছে। যা যাচাই করা হয়নি, তা
> স্পষ্টভাবে "যাচাই হয়নি" লেখা।

## ১. রিপোজিটরি

| বিষয় | মান |
|---|---|
| নাম | `shohanbusinessmail-cmd/hisaabdesk` |
| URL | https://github.com/shohanbusinessmail-cmd/hisaabdesk |
| দৃশ্যমানতা | পাবলিক |
| ডিফল্ট শাখা | `main` |
| সর্বশেষ main কমিট | `7ee7c5f` (PR #4 merge) |
| সোর্স ফাইল | ১১০টি ট্র্যাক করা ফাইল |

## ২. অ্যাপ্লিকেশন

| বিষয় | মান |
|---|---|
| নাম | হিসাবডেস্ক (HisaabDesk) |
| Application ID | `com.shohankhan.hisaabdesk` |
| সংস্করণ | ১.০.১ (versionCode 2) |
| ন্যূনতম Android | 8.0 (API 26) |
| Target/Compile SDK | 35 |
| বিল্ড ভ্যারিয়েন্ট | `release` (debug-key signed development build) |
| ভাষা/মুদ্রা | বাংলা (bn-BD) / BDT ৳, পূর্ণসংখ্যা পয়সা |

### বাস্তবায়িত মডিউল

ড্যাশবোর্ড · আয় · ব্যয় · ট্রান্সফার · অ্যাকাউন্ট · ক্যাটাগরি · কেনাকাটা ও দোকান বাকি ·
কন্টাক্ট · ধার নেওয়া/দেওয়া · পাওনা · দেনা · এনজিও ঋণ · ব্যাংক ঋণ · সঞ্চয় · লক্ষ্য ·
বাজেট · বিল/নিয়মিত লেনদেন · পেমেন্ট ক্যালেন্ডার · রিপোর্ট + CSV · নিরাপত্তা (PIN) ·
এনক্রিপ্টেড ব্যাকআপ/রিস্টোর · সেটিংস · ট্র্যাশ · রিকনসিলিয়েশন · সম্পর্কে।

## ৩. APK

| বিষয় | মান |
|---|---|
| রিলিজ পেজ (সর্বশেষ) | https://github.com/shohanbusinessmail-cmd/hisaabdesk/releases/tag/v1.0.1 |
| সরাসরি APK | https://github.com/shohanbusinessmail-cmd/hisaabdesk/releases/download/v1.0.1/hisaabdesk-1.0.1-debugsigned-dev.apk |
| ফাইলের নাম | `hisaabdesk-1.0.1-debugsigned-dev.apk` |
| আকার | ১২,৭৮৩,৯৫৮ বাইট |
| SHA-256 | `8c62de89bfb574e7a40888c424b498a8bef48a2149257b160d77fab6d1eb68bd` |
| স্বাক্ষর | **debug key** (`C=US, O=Android, CN=Android Debug`) — প্রোডাকশন স্বাক্ষর নয় |
| রিলিজ ধরন | **Pre-release** (স্পষ্টভাবে লেবেল করা) |
| APK manifest (CI-verified) | `package: name='com.shohankhan.hisaabdesk' versionCode='2' versionName='1.0.1' compileSdkVersion='35'` |
| apksigner verify | ✅ সফল (CI) |
| SHA-256 যাচাই | GitHub asset digest-এর সাথে মিলে যায় |

v1.0.0-ও প্রকাশিত (pre-release): `hisaabdesk-1.0.0-debugsigned-dev.apk`, ১২,৭৮৩,৯৫০ বাইট,
SHA-256 `d5f399600507a9a84a2e89a5ea09db051313768c23de90397c9e01eb82a301e4`।

**সাইনিং সীমাবদ্ধতা:** রিপোজিটরি secrets যোগ করার অনুমতি এজেন্ট টোকেনে নেই
(`403 Resource not accessible by integration`)। তাই প্রকৃত release key দিয়ে সাইন করা
APK এখনো তৈরি হয়নি। owner চারটি secret যোগ করলে (`docs/RELEASE_PROCESS.md §২.২`)
পরবর্তী ট্যাগে প্রকৃত সাইন করা APK তৈরি হবে। debug-signed build ইনস্টলযোগ্য, কিন্তু
একই কী ছাড়া ভবিষ্যতে আপডেট দেওয়া যাবে না।

## ৪. টেস্ট

| কী | ফলাফল |
|---|---|
| Unit/Robolectric টেস্ট | **৭৬টি চালানো হয়েছে, ৭৬টি পাস, ০ ব্যর্থ** (`testDebugUnitTest`) |
| Android Lint | ✅ সফল (`lintDebug`) |
| Kotlin compile (debug) | ✅ সফল |
| Debug APK assemble | ✅ সফল (১৮,৫৪১,৭৯০ বাইট artifact) |
| Release APK assemble | ✅ সফল (v1.0.1) |
| Instrumented UI টেস্ট | ❌ **লেখা/চালানো হয়নি** |
| এমুলেটর/ডিভাইসে ইনস্টল | ❌ **যাচাই করা হয়নি** (স্যান্ডবক্সে ডিভাইস নেই) |
| ম্যানুয়াল ক্লিক-থ্রু | ❌ **করা হয়নি** |

টেস্ট কভারেজ: মুদ্রা/সংখ্যা ফরম্যাটিং, তারিখ ও মাস-শেষ, কেনাকাটার হিসাব, ঋণ সূচি ও
বরাদ্দ, লেজার ব্যালেন্স, ট্রান্সফার invariant, reversal, idempotency, soft-delete,
দোকান বাকি, ধার/পাওনা/দেনা, বাজেট, রিকনসিলিয়েশন, ব্যাকআপ crypto ও validation,
PIN throttling।

CI রান (প্রমাণ):
- CI: `39503050734` (PR #4) — success · main push `39503490041`
- Release v1.0.1: `39502447696` — success (সব ধাপ)
- Release v1.0.0: `39499806311` — success

## ৫. নিরাপত্তা

- PIN: PBKDF2-HMAC-SHA256, ২,১০,০০০ iteration, ১৬-বাইট random salt, ২৫৬-bit,
  Android Keystore-এ AES-GCM-wrapped verifier, constant-time তুলনা, exponential backoff।
- PIN কখনো প্লেইন টেক্সটে সংরক্ষিত নয় (টেস্টে যাচাই)।
- ব্যাকআপ: AES-256-GCM + PBKDF2 (২,১০,০০০), প্রতি ফাইলে নতুন salt+IV, ভুল পাসওয়ার্ড/
  টেম্পার/ট্রাঙ্কেশন প্রত্যাখ্যান (টেস্টে যাচাই)।
- রিস্টোর: পূর্ণ ভ্যালিডেশন → এক transaction-এ প্রতিস্থাপন → ব্যর্থতায় rollback।
- অনুমতি: শুধু `POST_NOTIFICATIONS`। **INTERNET অনুমতি নেই।**
- **সীমাবদ্ধতা:** ডেটাবেজ at-rest এনক্রিপ্টেড নয় (app sandbox + ডিভাইস এনক্রিপশন নির্ভর)।

## ৬. অটোমেশন

| ফাইল | ট্রিগার | ফল |
|---|---|---|
| `.github/workflows/android-ci.yml` | main push, PR→main, dispatch | ✅ পাস |
| `.github/workflows/android-release.yml` | `v*.*.*` tag, dispatch | ✅ পাস (v1.0.0, v1.0.1) |

CI: wrapper validation → compile → unit tests → lint → debug APK artifact + রিপোর্ট।
Release: ট্যাগ/সংস্করণ যাচাই → টেস্ট → release APK → package/version hard-check →
apksigner verify → SHA-256 → GitHub Release (APK + checksum)।

প্রয়োজনীয় secrets (production signing): `HISAABDESK_KEYSTORE_BASE64`,
`HISAABDESK_KEYSTORE_PASSWORD`, `HISAABDESK_KEY_ALIAS`, `HISAABDESK_KEY_PASSWORD`।

## ৭. জানা সীমাবদ্ধতা (সম্পূর্ণ তালিকা)

1. Release APK debug-key signed (production signing secrets নেই)।
2. ডিভাইস/এমুলেটরে ইনস্টল ও রানটাইম যাচাই হয়নি।
3. Instrumented UI টেস্ট নেই।
4. সংযুক্তি (রসিদ ছবি) — DB/FileProvider زیرساخت আছে, UI অসম্পূর্ণ।
5. Merge-mode restore নেই (শুধু full replace)।
6. শুধু BDT, শুধু লাইট থিম, শুধু বাংলা UI।
7. সঞ্চয়/ঋণের চক্রবৃদ্ধি মডেল নেই — ব্যবহারকারীর ইনপুট প্রয়োজন।
8. `app/schemas/` এখনো কমিট করা হয়নি (বিল্ডে উৎপন্ন হয়)।
9. Biometric unlock নেই (ইচ্ছাকৃত)।
10. Reports-এ শুধু বার-চার্ট (লাইন/পাই নেই)।

## ৮. ব্যবহারকারীর পরবর্তী করণীয়

1. উপরের ৪টি secret যোগ করুন → `versionCode`/`versionName` বাড়িয়ে নতুন ট্যাগ দিন →
   প্রকৃত সাইন করা APK পাবেন।
2. একটি Android ডিভাইসে APK ইনস্টল করে `docs/TEST_PLAN.md §৪`-এর smoke test চালান।
3. সাইনিং কী ও পাসওয়ার্ড নিরাপদে সংরক্ষণ করুন (`/home/user/HISAABDESK_SIGNING_CREDENTIALS.txt`
   সেশন-ওয়ার্কস্পেসে আছে; রিপোতে কমিট করা হয়নি)।
