# স্থাপত্য (Architecture)

## স্তরবিন্যাস

```
┌──────────────────────────────────────────────────────────────┐
│ Presentation (feature/*)                                     │
│  Compose screens · ViewModels · immutable UiState · Navigation│
└───────────────▲──────────────────────────────────────────────┘
                │ StateFlow / suspend calls
┌───────────────┴──────────────────────────────────────────────┐
│ Domain (domain/*)                                            │
│  Money · Dates · PurchaseCalculator · LoanCalculator        │
│  AccountingRules (classification, invariants)                │
│  → কোনো Android/UI নির্ভরতা নেই, তাই pure JVM টেস্টে চলে      │
└───────────────▲──────────────────────────────────────────────┘
                │
┌───────────────┴──────────────────────────────────────────────┐
│ Data (data/*)                                                │
│  Repositories → DAOs (Room) → SQLite                         │
│  Backup serializer/crypto · DataStore settings               │
└───────────────▲──────────────────────────────────────────────┘
                │
┌───────────────┴──────────────────────────────────────────────┐
│ Infrastructure (core/*)                                      │
│  PIN/Keystore · notification worker · formatting/validation   │
└──────────────────────────────────────────────────────────────┘
```

## একক Activity + Compose Navigation

`MainActivity` একমাত্র Activity। ভেতরে তিনটি অবস্থা:

1. `!onboardingComplete` → Onboarding NavHost (welcome → privacy → PIN → account → categories → done)
2. `locked` → `PinLockScreen`
3. অন্যথায় → `MainNavHost` (bottom bar: ড্যাশবোর্ড / লেনদেন / অ্যাকাউন্ট / আরও)

সব ফিচার স্ক্রিন `Routes` অবজেক্টের τύπος-safe রুট দিয়ে সংযুক্ত; প্রতিটি বাটন/কার্ডের
প্রকৃত গন্তব্য আছে।

## State management

- প্রতিটি ফিচারের `ViewModel` কনস্ট্রাক্টরে `AppContainer` নেয় (manual DI — কোনো
  reflection-heavy DI framework নেই, তাই R8/টেস্ট সরল)।
- ডেটা `Flow`/`StateFlow`; UI `collectAsState()` ব্যবহার করে।
- `viewModelScope`-এর বাইরে কোনো DB I/O নেই।
- ফর্ম সাবমিটে double-submit guard (`saving` flag)।

## লেজার ও ব্যালেন্স

- `txns` টেবিলই সত্যসূত্র। `accounts.openingBalancePaisa` শুধু শুরুর ব্যালেন্স।
- ব্যালেন্স SQL: একটি `LEFT JOIN` + `CASE` দিয়ে `INFLOW/OUTFLOW/TRANSFER` delta।
  (দেখুন `AccountDao.observeBalances`)
- কোনো cache balance নেই → reconciliation সহজ, bug-এর সুযোগ কম।
- সব multi-row write `db.withTransaction { }`-এ।

## প্যাকেজ মানচিত্র

| প্যাকেজ | দায়িত্ব |
|---|---|
| `core/formatting` | মুদ্রা, সংখ্যা, তারিখ |
| `core/security` | Keystore wrapper, PIN manager |
| `core/backup` | ফাইল ফরম্যাট + JSON serializer/validator |
| `core/notification` | WorkManager scheduler + reminder worker |
| `data/local/entity` | ১৯ Room entity + enum |
| `data/local/dao` | ১৮ DAO (aggregate SQL সহ) |
| `data/local/db` | database, converters, migration নীতি |
| `data/repository` | ১২ repository (transactional use-cases) |
| `domain/*` | pure Kotlin হিসাব ও নিয়ম |
| `feature/*` | ১৭ ফিচার মডিউল (VM + screen) |
| `ui/theme`,`ui/components` | design system |
| `ui/navigation` | route table |

## ত্রুটি ব্যবস্থাপনা

- Repository-নির্দিষ্ট `ValidationException`-এ Bengali বার্তা; UI `ErrorBanner`-এ দেখায়।
- অপ্রত্যাশিত exception-এ সাধারণ বার্তা; কোনো silently swallow নেই।
- PIN/keystore ব্যর্থতা degrade করে (অ্যাপ কাজ করে, নিরাপত্তা কমে) — লগ করা হয় না।

## পারফরম্যান্স

- তালিকা `LazyColumn` + stable keys + LIMIT/OFFSET pagination।
- `Flow` কেবল পরিবর্তিত টেবিলে emit করে (Room invalidation)।
- ব্যালেন্স/অ্যাগ্রিগেট SQL-এ — বড় ইতিহাসেও O(rows of that account)।
- কোটলিন Coroutines; main thread-এ DB/ক্রিপ্টো নেই।
