# KDroid Database Localization Module

This module extends the `AppCategory` enum from the core module to provide localized category names in English, Hebrew, and French, with English as the default.

## Features

- Get localized category names in English, Hebrew, and French
- Platform-specific extensions for Android and JVM
- Default to English for unsupported languages
- Case-insensitive language codes

## Installation

Add the localization module to your project:

```kotlin
dependencies {
    implementation("io.github.kdroidfilter.database:localization:0.1.0")
}
```

Note: The localization module automatically includes the core module as a dependency.

## Usage

### Common

```kotlin
// Get localized name with specific language
val englishName = LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "en")
val hebrewName = LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "he")
val frenchName = LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "fr")
```

### Android

```kotlin
// Using system default locale (automatically uses device language)
val localizedName = AppCategory.TORAH.getLocalizedName()
```

### JVM

```kotlin
// Using system default locale
val localizedName = AppCategory.TORAH.getLocalizedName()
```

## Supported Languages

- English (default)
- Hebrew
- French

## Implementation Details

The module consists of:

1. `LocalizedAppCategory` - Common class with localization logic for getting category names in different languages
2. Android extensions - For getting localized names based on the device's language settings
3. JVM extensions - For getting localized names based on the system's default locale

## How It Works

The module provides two main ways to get localized category names:

1. Direct access through the `LocalizedAppCategory` object, where you specify both the category and language:
   ```kotlin
   LocalizedAppCategory.getLocalizedName(AppCategory.TORAH, "he")
   ```

2. Platform-specific extension functions that automatically use the system's language:
   ```kotlin
   AppCategory.TORAH.getLocalizedName()
   ```
