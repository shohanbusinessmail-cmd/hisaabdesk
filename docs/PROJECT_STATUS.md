# প্রকল্পের অবস্থা (Project Status)

সর্বশেষ হালনাগাদ: ২০২৬-১০-২০

এই ডকুমেন্ট **সৎ অবস্থা** লিখে রাখে — কী যাচাই হয়েছে, কী হয়নি। কোনো যাচাই না করা
দাবি এখানে "সম্পন্ন" হিসেবে লেখা হয় না।

## ১. সম্পন্ন কাজ (কোড)

- Gradle 8.11.1 wrapper, AGP 8.7.3, Kotlin 2.0.21, KSP, version catalog।
- Design system: Material 3, শুধু লাইট সাদা-সবুজ থিম, বাংলা টাইপোগ্রাফি।
- Launcher icon: adaptive vector + all-density PNGs।
- সম্পূর্ণ বাংলা `strings.xml` (কেন্দ্রীয় শব্দভাণ্ডার)।
- Room ডেটাবেজ: ১৯ টেবিল, FK/indices, enum converters, schema export।
- লেজার-ভিত্তিক হিসাব: ব্যালেন্স কখনো cache নয় — SQL দিয়ে লেজার থেকে পুনঃহিসাব।
- টেস্টেড ফাইন্যান্সিয়াল কোর: Money (পয়সা), Dates (মাস-শেষ), PurchaseCalculator,
  LoanCalculator (FLAT/REDUCING/SIMPLE/CONTRACT/NONE)।
- রিপোজিটরি: Account/Category/Contact/Shop, Transaction (balance guard, reversal,
  soft-delete, idempotency), Shopping (accrual + payments + overpay guard),
  Obligation (lend/borrow/receivable/payable + component checks), Loan (schedule +
  payment allocation + completion), Planning (budget/goal/recurring),
  Report (dashboard aggregates, net worth, reconciliation), Backup।
- UI: dashboard, transactions (list/detail/forms), accounts, transfer, categories,
  shopping (list/form/detail/payments/shops), contacts, debts (lending/borrowing),
  receivables/payables, NGO/bank loans, savings, goals, budgets, bills, calendar,
  reports + CSV, backup/restore, settings, security, about, trash, reconcile।
- নিরাপত্তা: PBKDF2 PIN, Keystore-wrapped verifier, throttle, auto-lock।
- ব্যাকআপ: AES-256-GCM + PBKDF2, versioned JSON, strict validation, transactional
  restore।
- WorkManager reminder worker (বিল + কিস্তি), notification channel।
- Unit টেস্ট স্যুট: Money, Dates, calculators, AccountingRules, BackupCrypto,
  BackupSerializer, PIN, Room ledger/shop/loan/obligation/budget/reconcile।

## ২. চলমান / অসম্পূর্ণ

- **সংযুক্তি (receipt attachments)**: DB টেবিল ও FileProvider আছে; ফাইল কপি +
  UI পিকার সম্পূর্ণ নয় → অসম্পূর্ণ হিসেবে গণ্য।
- **Merge-mode restore**: নেই; শুধু full replace (ডকুমেন্টেড)।
- **Instrumented UI টেস্ট**: লেখা হয়নি (এমুলেটর প্রয়োজন)।
- **Migration টেস্ট**: v1-এ এখনো কোনো migration নেই; ভবিষ্যতের জন্য কাঠামো প্রস্তুত।

## ৩. বিল্ড ও টেস্ট ফলাফল (যাচাইকৃত)

| কী | ফলাফল | প্রমাণ |
|---|---|---|
| GitHub Actions CI | ✅ সফল (run 39496517444) | compile → unit test → lint → assembleDebug সব ধাপ success |
| Unit টেস্ট | ✅ **৭৬ টেস্ট পাস, ০ ব্যর্থ** | JUnit XML, CI artifact `unit-test-report` |
| Android Lint | ✅ সফল (`lintDebug`) | CI log |
| Debug APK | ✅ তৈরি, ১৮,৫৪১,৭৯০ বাইট | CI artifact `hisaabdesk-debug-apk` |
| Gradle wrapper validation | ✅ সফল | CI log |
| Release APK (v1.0.1) | ✅ প্রকাশিত — `hisaabdesk-1.0.1-debugsigned-dev.apk` (১২,৭৮৩,৯৫৮ বাইট)<br>SHA-256 `8c62de89bfb574e7a40888c424b498a8bef48a2149257b160d77fab6d1eb68bd`<br>**pre-release, debug-key signed** | [রিলিজ পেজ](https://github.com/shohanbusinessmail-cmd/hisaabdesk/releases/tag/v1.0.1) |
| APK manifest (CI notice) | `package: name='com.shohankhan.hisaabdesk' versionCode='2' versionName='1.0.1' compileSdkVersion='35'` | check-run notice (release run 39502447696) |
| APK signer (CI notice) | `C=US, O=Android, CN=Android Debug` → debug key, প্রোডাকশন নয় | check-run notice |
| apksigner verify | ✅ সফল | release run 39502447696 |
| v1.0.0 release | ✅ প্রকাশিত (pre-release) `hisaabdesk-1.0.0-debugsigned-dev.apk`, SHA-256 `d5f3996…a301e4` | রিলিজ পেজ |
| এমুলেটর/ডিভাইস ইনস্টলেশন | ❌ **যাচাই করা হয়নি** — স্যান্ডবক্সে এমুলেটর/ডিভাইস নেই | — |

> নোট: এই স্যান্ডবক্সে ইন্টারনেট শুধু GitHub/npm/PyPI-তে সীমিত; Google Maven
> (`dl.google.com`, `maven.google.com`) ব্লকড। তাই **স্থানীয় Gradle build অসম্ভব**;
> বিল্ড ও টেস্ট GitHub Actions-এ চলে, যা Android SDK সহ পূর্ণ পরিবেশ দেয়।

## ৪. জানা সমস্যা

- `lintDebug` তে warning থাকতে পারে (unused import ইত্যাদি); error থাকলে ঠিক করা হবে।
- কিছু স্ক্রিনে ফর্ম state process death-এ পুনরুদ্ধার হয় না (ViewModel-এ থাকে,
  `SavedStateHandle` ব্যবহার হয়নি)।
- Shop form-এ দোকান সম্পাদনার আলাদা স্ক্রিন নেই (তালিকা থেকে যোগ + ইনলাইন)।
- Reports-এ শুধু বার-চার্ট (রেখাচিত্র/পাই নেই) — ইচ্ছাকৃত সরলতা।

## ৫. যাচাই না করা দাবি (স্পষ্টভাবে)

- "সব স্ক্রিন কাজ করে" — শুধু কম্পাইল + কোড পর্যালোচনা; রানটাইম ক্লিক-থ্রু
  (প্রতিটি ফর্ম/ফিল্টার) যাচাই করা হয়নি।
- "APK ইনস্টল হয়েছে এবং চালু হয়েছে" — **যাচাই করা হয়নি** (এমুলেটর/ডিভাইস নেই)।
- "প্রোডাকশন-সাইন করা রিলিজ" — **নয়**; v1.0.0 debug key দিয়ে সাইন করা pre-release।
- "সব acceptance criteria পূরণ" — নয়; instrumented UI টেস্ট, ডিভাইস ভেরিফিকেশন
  ও production signing বাকি।

## ৬. বাহ্যিক যা প্রয়োজন (এবং কী ব্লকড)

1. **রিলিজ সাইনিং secrets** — ব্লকড: এজেন্টের GitHub integration টোকেনে repository
   secrets লেখার অনুমতি নেই (`403 Resource not accessible by integration`)।
   চারটি secret (নাম docs/RELEASE_PROCESS.md §২.২) owner যোগ করলেই পরবর্তী রান
   প্রকৃত সাইন করা APK তৈরি করবে। তার আগে release workflow **debug-key signed,
   স্পষ্টভাবে লেবেল করা pre-release** তৈরি করে — কোনো "production signed" দাবি ছাড়াই।
2. Android ডিভাইসে ম্যানুয়াল smoke test (docs/TEST_PLAN.md §৪) — এজেন্টের কাছে
   ডিভাইস নেই, তাই বাকি।
3. Actions মিনিট/কোটা — পাবলিক রিপোজিটরিতে বিনামূল্যে; কোনো paid CI নেই।

## ৭. পরবর্তী ধাপ

1. ✅ CI green (compile → ৭৬ টেস্ট → lint → APK)।
2. ✅ রিলিজ প্রকাশ (v1.0.0, v1.0.1) — pre-release, debug-key signed, checksum সহ।
3. ⏳ **সাইনিং secrets যোগ করা** (owner) → পরবর্তী ট্যাগে প্রকৃত সাইন করা APK।
4. ⏳ ডিভাইস/এমুলেটরে ম্যানুয়াল smoke test (docs/TEST_PLAN.md §৪)।
5. ⏳ সংযুক্তি (রসিদ) UI সম্পূর্ণ করা, `SavedStateHandle` যোগ, instrumented UI টেস্ট।
6. ⏳ `app/schemas/` (Room exported schema) — প্রথম লোকাল/CI বিল্ডে উৎপন্ন হয়;
   পরবর্তী পরিবর্তনে কমিট করতে হবে (বর্তমানে রিপোতে নেই)।
