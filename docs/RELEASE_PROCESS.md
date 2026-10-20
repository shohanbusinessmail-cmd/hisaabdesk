# রিলিজ প্রক্রিয়া (Release Process)

## ০. সারাংশ

| ধাপ | কে করে | কী |
|---|---|---|
| 1 | ডেভেলপার | সংস্করণ বাড়ানো, CHANGELOG |
| 2 | CI | compile → unit test → lint → debug APK |
| 3 | ডেভেলপার | PR → main merge |
| 4 | ডেভেলপার | `vX.Y.Z` ট্যাগ push |
| 5 | Actions | release workflow: টেস্ট → release APK → apksigner verify → SHA-256 → GitHub Release |
| 6 | ডেভেলপার | রিলিজ পেজ যাচাই, APK ইনস্টল টেস্ট |

## ১. সংস্করণ

- `app/build.gradle.kts`-এ `versionName = "X.Y.Z"`, `versionCode` = পূর্ণসংখ্যা (প্রতি রিলিজে +১)।
- Semantic versioning: bugfix = patch, ফিচার = minor, ভাঙা পরিবর্তন = major।
- ট্যাগ `vX.Y.Z`; workflow যাচাই করে ট্যাগ ও `versionName` মিলছে কিনা (না মিললে fail)।

## ২. সাইনিং কী (গুরুত্বপূর্ণ)

একই application ID-তে আপডেট দিতে **একই সাইনিং কী** লাগবে। কী হারালে আপডেট দেওয়া যাবে না।

### ২.১ একটি রিলিজ কী তৈরি (একবারই)

```bash
keytool -genkeypair -v \
  -keystore release.p12 -storetype PKCS12 \
  -alias hisaabdesk -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=HisaabDesk,O=Shohan Khan,C=BD"
```

### ২.২ GitHub Secrets যোগ করা (repo admin প্রয়োজন)

Settings → Secrets and variables → Actions → New repository secret:

| Secret | মান |
|---|---|
| `HISAABDESK_KEYSTORE_BASE64` | `base64 -w0 release.p12` এর ফল |
| `HISAABDESK_KEYSTORE_PASSWORD` | keystore পাসওয়ার্ড |
| `HISAABDESK_KEY_ALIAS` | `hisaabdesk` |
| `HISAABDESK_KEY_PASSWORD` | key পাসওয়ার্ড |

> **এই সেশনের সীমাবদ্ধতা:** এজেন্টের GitHub integration টোকেনে repository-secret
> লেখার অনুমতি নেই (403)। তাই secrets স্বয়ংক্রিয়ভাবে যোগ করা যায়নি। owner হিসেবে
> আপনি উপরের চারটি secret যোগ করলে পরবর্তী `workflow_dispatch`/ট্যাগ রিলিজেই
> **প্রকৃত সাইন করা** APK তৈরি হবে।

### ২.৩ সাইনিং ছাড়া কী হয়

secrets না থাকলে workflow:

- `-PhisaabdeskDebugSigning=true` দিয়ে release variant বানায় (বাস্তব application ID,
  কিন্তু **debug key**) — ইনস্টলযোগ্য, তবে প্রোডাকশন স্বাক্ষর নয়;
- APK-এর নাম `hisaabdesk-X.Y.Z-debugsigned-dev.apk`;
- GitHub Release **pre-release** হিসেবে প্রকাশ হয় এবং নোটে স্পষ্ট সতর্কতা থাকে।

এটি কখনো "production signed" হিসেবে উপস্থাপন করা হয় না।

## ৩. রিলিজ ধাপ

```bash
# ১. সংস্করণ বাড়ান
#    app/build.gradle.kts: versionName "1.0.1", versionCode 2
#    CHANGELOG.md আপডেট
git add -A && git commit -m "release: v1.0.1"
git push

# ২. main-এ merge নিশ্চিত করুন, তারপর ট্যাগ
git tag -a v1.0.1 -m "HisaabDesk 1.0.1"
git push origin v1.0.1
```

## ৪. ওয়ার্কফ্লো যা করে (`android-release.yml`)

1. ট্যাগ ফরম্যাট যাচাই (`v*.*.*`) + `versionName` মিল
2. JDK 17, wrapper validation
3. signing secrets থাকলে keystore restore
4. `testDebugUnitTest` (আবশ্যক)
5. `assembleRelease` (signed অথবা debug-key dev fallback)
6. `aapt2 dump badging` — package id যাচাই
7. `apksigner verify` — স্বাক্ষর যাচাই (ব্যর্থ হলে fail)
8. SHA-256 checksum
9. artifact আপলোড
10. Release notes তৈরি (Bengali) + GitHub Release (APK + checksum)

## ৫. রিলিজের পরে যাচাই

```bash
gh release view v1.0.1
gh release download v1.0.1
sha256sum hisaabdesk-1.0.1*.apk   # checksum.txt এর সাথে মিলিয়ে দেখুন
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs hisaabdesk-*.apk
```
তারপর একটি ডিভাইসে ইনস্টল করে smoke test (docs/TEST_PLAN.md §৪)।

## ৬. ভুল হলে

- বিল্ড fail → রিলিজ তৈরি হয় না (workflow এমনভাবে লেখা)।
- ভুল APK আপলোড হলে: `gh release delete-asset`, ঠিক করে নতুন workflow_dispatch।
- ট্যাগ মুছবেন না যদি না রিলিজও মুছেন (`gh release delete vX.Y.Z --yes`)।

## ৭. স্টোর/পরিবেশ

- শুধু GitHub-hosted free runner ব্যবহার হয় (paid CI নেই)।
- পাবলিক রিপোজিটরিতে Actions মিনিট free; ব্যক্তিগত রিপোজিটরিতে কোটা প্রযোজ্য।
- Play Store-এ প্রকাশের জন্য AAB লাগবে — এই প্রকল্প APK-কেন্দ্রিক (সরাসরি ইনস্টল)।
