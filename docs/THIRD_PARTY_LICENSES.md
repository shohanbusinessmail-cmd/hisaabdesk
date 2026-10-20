# তৃতীয় পক্ষের লাইসেন্স (Third-Party Licenses)

HisaabDesk-এর নিজস্ব সোর্স কোড MIT License-এর অধীনে (দেখুন `LICENSE`)। নিচের
লাইব্রেরিগুলো নিজ নিজ লাইসেন্সের অধীন এবং এগুলো এই প্রকল্পের মালিকানাধীন নয়।

## AndroidX / Jetpack (Apache License 2.0)

`androidx.core:core-ktx`, `androidx.activity:activity-compose`,
`androidx.lifecycle:*`, `androidx.navigation:navigation-compose`,
`androidx.room:*`, `androidx.work:work-runtime`, `androidx.datastore:preferences`,
`androidx.core:core-splashscreen`, `androidx.compose.ui`, `androidx.compose.material3`,
`androidx.compose.material:material-icons-extended`, `androidx.test:*`,
`androidx.room:room-testing`

Copyright © The Android Open Source Project
License: <https://www.apache.org/licenses/LICENSE-2.0>

## Kotlin & Kotlin Coroutines (Apache License 2.0)

`org.jetbrains.kotlin:*`, `org.jetbrains.kotlinx:kotlinx-coroutines-*`

Copyright © JetBrains s.r.o.
License: <https://www.apache.org/licenses/LICENSE-2.0>

## Compose Multiplatform/Compiler plugin (Apache License 2.0)

`org.jetbrains.kotlin.plugin.compose` — JetBrains, Apache 2.0।

## JUnit 4 (Eclipse Public License 1.0)

`junit:junit:4.13.2` — Copyright © JUnit contributors।
License: <https://www.eclipse.org/legal/epl-v10.html>

## Robolectric (MIT License)

`org.robolectric:robolectric` — Copyright © Robolectric contributors।
License: <https://github.com/robolectric/robolectric/blob/master/LICENSE>

## Google Truth (Apache License 2.0)

`com.google.truth:truth` — Copyright © Google LLC.
License: <https://www.apache.org/licenses/LICENSE-2.0>

## Gradle Wrapper (Apache License 2.0)

`gradle/wrapper/gradle-wrapper.jar` — Copyright © Gradle, Inc.
License: <https://www.apache.org/licenses/LICENSE-2.0>
Wrapper integrity CI-তে `gradle/actions/wrapper-validation` দিয়ে যাচাই হয়।

## GitHub Actions (প্রযোজ্য নয় — শুধু CI)

`actions/checkout`, `actions/setup-java`, `actions/upload-artifact`,
`gradle/actions/*` — CI زیرساخت; অ্যাপে অন্তর্ভুক্ত নয়।

## ফন্ট ও আইকন

- লঞ্চার আইকন **মূল ভেক্টর ডিজাইন** (এই প্রকল্পের অংশ, MIT)। কোনো ইন্টারনেট চিত্র নয়।
- UI আইকন: Material Icons (AndroidX `material-icons-extended`) — Apache 2.0।
- Bengali টেক্সট রেন্ডারিং ডিভাইসের সিস্টেম ফন্টে হয়; কোনো ফন্ট ফাইল বান্ডল করা হয়নি।

## নোট

- কোনো অ্যাড SDK, অ্যানালিটিক্স, ট্র্যাকিং বা প্রোপ্রাইটরি লাইব্রেরি নেই।
- কোনো paid API/সেবা নেই।
