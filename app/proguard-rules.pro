# HisaabDesk R8/ProGuard rules.
# minify is currently disabled in build.gradle.kts; these rules are kept so that
# minification can be enabled safely later. Release builds therefore ship
# unobfuscated code (documented in docs/PROJECT_STATUS.md).

# Keep Room entities for backup reflection-free serialization safety.
-keep class com.shohankhan.hisaabdesk.data.local.entity.** { *; }

# Keep domain models used by the JSON backup serializer.
-keep class com.shohankhan.hisaabdesk.domain.model.** { *; }

# Coroutines internals
-dontwarn kotlinx.coroutines.**
