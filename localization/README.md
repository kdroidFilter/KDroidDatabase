# KDroid Database Localization Module

This module extends the `AppCategory` enum from the core module to provide localized category names in English, Hebrew, and French, with English as the default.

## Features

- Get localized category names in English, Hebrew, and French
- Platform-specific extensions for Android and JVM
- Default to English for unsupported languages
- Case-insensitive language codes

## Usage

### Common

```kotlin
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.localization.LocalizedAppCategory

// Get localized name with specific language
val englishName = LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "en")
val hebrewName = LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "he")
val frenchName = LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "fr")
```

### Android

```kotlin
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.localization.getLocalizedName
import android.content.Context

// In an Activity or Fragment
val context: Context = this
val localizedName = AppCategory.TORAH.getLocalizedName(context)

// With a specific locale
val locale = Locale("he")
val hebrewName = AppCategory.TORAH.getLocalizedName(locale)
```

### JVM

```kotlin
import io.github.kdroidfilter.database.core.AppCategory
import io.github.kdroidfilter.database.localization.getLocalizedName
import java.util.Locale

// Using system default locale
val localizedName = AppCategory.TORAH.getLocalizedName()

// With a specific locale
val locale = Locale("fr")
val frenchName = AppCategory.TORAH.getLocalizedName(locale)
```

## Supported Languages

- English (default)
- Hebrew
- French

## Implementation Details

The module consists of:

1. `LocalizedAppCategory` - Common class with localization logic
2. Android extensions - For getting localized names based on Android context
3. JVM extensions - For getting localized names based on JVM system locale