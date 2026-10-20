# টেস্ট পরিকল্পনা ও ফলাফল (Test Plan)

## ১. পরিধি

| স্তর | যন্ত্র | কমান্ড | CI-তে চলে? |
|---|---|---|---|
| Pure JVM unit | JUnit 4 | `./gradlew testDebugUnitTest` | ✅ |
| Android-runtime unit (Robolectric + in-memory Room) | Robolectric 4.14.1 | `testDebugUnitTest` | ✅ |
| Instrumented UI/DB | AndroidJUnitRunner | `connectedDebugAndroidTest` | ❌ (এমুলেটর নেই) |
| Static | Android Lint | `lintDebug` | ✅ |

## ২. কভারেজ ম্যাপ

### ২.১ মুদ্রা ও তারিখ (`MoneyTest`, `DatesTest`)
- বাংলা/ইংরেজি অঙ্ক, দক্ষিণ-এশীয় ও পশ্চিমা গ্রুপিং, ঋণাত্মক চিহ্ন
- পার্সিং: বেঙ্গলি অঙ্ক, ৳, কমা, ভুল ইনপুট প্রত্যাখ্যান
- শতাংশ half-up rounding, overflow guard
- মাস-শেষ clamp (৩১ জানু → ২৯ ফেব্রু), leap year, সাপ্তাহিক/নির্দিষ্ট দিন
- dayKey sortable, সপ্তাহ সোমবার শুরু

### ২.২ হিসাব ইঞ্জিন (`DomainCalculatorsTest`)
- লাইন টোটাল (দশমিক quantity), লাইন ছাড়/চার্জ, পাইকারি ছাড়
- নেতিবাচক payable ও overpay প্রত্যাখ্যান
- ঋণ: NONE/FLAT/REDUCING/CONTRACT সূচি, শেষ কিস্তিতে রাউন্ডিং অবশিষ্ট,
  payment split validations, সরল সুদ ৩৬৫ দিন ভিত্তি
- শ্রেণিবিন্যাস: transfer/savings/loan-proceeds আয়-ব্যয় নয়; component sum নিয়ম;
  net worth

### ২.৩ লেজার ও রিপোজিটরি (`LedgerRepositoryTest`, in-memory Room)
- আয়/ব্যয়ে ব্যালেন্স পরিবর্তন; negative-balance policy
- transfer invariant (মোট সম্পদ অপরিবর্তিত, income/expense = 0), same-account reject
- reversal: লেজার প্রভাব সরে, রেকর্ড থাকে, audit row, দ্বিতীয় reversal প্রত্যাখ্যান
- soft delete → totals থেকে বাদ → restore; permanent delete শুধু trash থেকে
- idempotency key duplicate প্রতিরোধ
- component mismatch প্রত্যাখ্যান
- কেনাকাটা: লাইন/ছাড়/চার্জ হিসাব, প্রাথমিক পরিশোধ, পরবর্তী পরিশোধ, overpay reject,
  সম্পূর্ণ নগদে দায় শূন্য
- ধার/ঋণ আলাদা; overpay reject; receivable/payable overpay reject
- ঋণ: সূচি তৈরি, কিস্তি পরিশোধ, overpay reject, principal-only completion নয়
- বাজেট: ব্যয় গণনা, transfer বাদ
- reconciliation সব ঠিকডেটাসেট-এ pass; অ্যাকাউন্ট ডিলিট blocker

### ২.৪ নিরাপত্তা ও ব্যাকআপ (`BackupCryptoTest`, `PinAndBackupTest`)
- AES-GCM round trip, wrong password, tampered ciphertext, foreign file, truncated,
  প্রতি বার নতুন salt/IV
- PIN: plaintext নয়, সঠিক/ভুল, ৩ ব্যর্থতায় backoff, দীর্ঘ lockout, PIN change,
  invalid format
- Backup serializer: round trip, unsupported schema, invalid JSON, duplicate IDs,
  broken FK, transfer without destination, out-of-bounds amount, unknown format

## ৩. ফলাফল (GitHub Actions)

> সংখ্যা ও ফলাফল বাস্তব CI রান থেকে নেওয়া; `docs/PROJECT_STATUS.md`-এ লিংক/রান আইডি।

- `testDebugUnitTest`: **৭৬ টেস্ট, সবগুলো পাস** (JVM + Robolectric)
- `lintDebug`: সফল
- `assembleDebug`: সফল, APK artifact আপলোড
- `connectedDebugAndroidTest`: **চালানো হয়নি** (এমুলেটর অনুপলব্ধ)

## ৪. ম্যানুয়াল smoke test (যা এখনো করা হয়নি)

নিচের ধাপগুলো একটি ডিভাইস/এমুলেটরে করতে হবে — CI এগুলো যাচাই করে না:

- [ ] প্রথম লঞ্চ → PIN তৈরি → অ্যাকাউন্ট → ড্যাশবোর্ড
- [ ] আয় ও ব্যয় যোগ → ব্যালেন্স আপডেট → অ্যাপ রিস্টার্টে টিকে থাকা
- [ ] ট্রান্সফার → মোট সম্পদ অপরিবর্তিত
- [ ] কেনাকাটা (৩ পণ্য) → বাকি → আংশিক পরিশোধ → বাকি শূন্য
- [ ] ধার দেওয়া → আদায় → বাকি
- [ ] এনজিও ঋণ (২৪ কিস্তি) → কিস্তি পরিশোধ → পরবর্তী কিস্তি সরে
- [ ] ব্যাকআপ → নতুন ইনস্টল → রিস্টোর → ডেটা ফিরে আসা
- [ ] ভুল পাসওয়ার্ডে রিস্টোর → পুরনো ডেটা অক্ষত
- [ ] PIN ভুল → backoff

## ৫. ভবিষ্যৎ

- `androidTest`: Room migration tests, Compose UI tests (onboarding, add income,
  add purchase, backup flow)
- Screenshot/golden tests with Paparazzi (ঐচ্ছিক)
- ১০,০০০ লেনদেনের পারফরম্যান্স বেঞ্চমার্ক
