# 📄 Kdroid Database Policies Contribution Guide

<img src="/assets/header.png" style="border-radius: 10px;"/>


[![Maven Central](https://img.shields.io/maven-central/v/io.github.kdroidfilter.database/core)](https://search.maven.org/artifact/io.github.kdroidfilter.database/core)
[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/kdroidfilter/KDroidDatabase/release-sqlite-db.yml?branch=master)](https://github.com/kdroidfilter/KDroidDatabase/actions/workflows/release-sqlite-db.yml)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/kdroidfilter/KDroidDatabase)](https://github.com/kdroidfilter/KDroidDatabase/releases/latest)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

## 📑 Table of Contents
- [Introduction](#introduction)
- [Available Modes](#here-is-a-simple-tree-of-all-available-modes-)
- [Directory Structure](#️-directory-structure-)
- [Supported Policy Types & Templates](#-supported-policy-types--templates)
  - [FixedPolicy](#1️⃣-fixedpolicy)
  - [ModeBasedPolicy](#2️⃣-modebasedpolicy)
  - [MultiModePolicy](#3️⃣-multimodePolicy)
- [Checklist Before Submitting](#✅-checklist-before-submitting)
- [Gradle Tasks](#🛠️-gradle-tasks)
  - [Validating JSON Files](#validating-json-files)
- [Questions & Answers](#❓-questions--answers)
  - [What is the difference between LOCAL_ONLY and OFFLINE?](#what-is-the-difference-between-local_only-and-offline)
  - [Optional Flags: Content and Risk Warnings](#-optional-flags-content-and-risk-warnings)
  - [Why would an app require Play Store installation?](#why-would-an-app-require-play-store-installation)
  - [Example: Risky Applications](#-example-risky-applications)
- [Importing KDroid Database Modules](#importing-kdroid-database-modules)
  - [Core Module](#core-module-)
  - [Localization Module](#localization-module-)
  - [Downloader Module](#downloader-module-)
  - [DAO Module](#dao-module-)

## Introduction

Welcome to the community-driven database of kosher filtering rules for applications. This guide is primarily used by KDroid Filter but is freely available for anyone to use. Licensed under the LGPL, you may integrate these policies even in closed-source applications. At this stage, it is recommended to contribute only host-based rules, as the tools for detecting apps and UI nodes are not yet available.

The KDroid app store dynamically adapts its application listings based on the current **UserMode** (levels 0 to 5) to only show apps that are appropriate for each level:

* In **OFFLINE** (0) or **LOCAL\_ONLY** (1) modes, only updates for already installed apps that can work offline will be shown.
* In **NAVIGATION\_ONLY** mode (2), only apps in the **NAVIGATION** category will be visible.
* In **NAVIGATION\_AND\_MAIL\_ONLY** mode (3), apps in the **MAIL** category will also be included.
* In **REDUCED\_RISK** mode (4), only apps from trusted categories will appear: **TORAH**, **PRODUCTIVITY**, **TOOLS**, **FINANCE**, **MUSIC\_AUDIO**, and **HOME**.
* Finally, in **MOST\_OPEN** mode (5), all available apps will be listed.

For applications using a **ModeBasedPolicy**, if a rule is defined for a lower level (e.g. 1 or 2), any higher levels (3, 4, or 5) without an explicit configuration will automatically inherit that rule.

## Here is a simple tree of all available modes :

```text
AppPolicy
├── FixedPolicy
│   ├── "type": "Fixed"
│   ├── networkPolicy
│   │   ├── mode: FULL_OPEN | BLACKLIST | WHITELIST | LOCAL_ONLY | OFFLINE
│   │   └── spec: None | HostList{hosts…}
│   └── detectionRules […]
│
├── ModeBasedPolicy
│   ├── "type": "ModeBased"
│   ├── modePolicies
│   │   ├── OFFLINE → NetworkPolicy{…}
│   │   ├── NAVIGATION_ONLY → NetworkPolicy{…}
│   │   ├── NAVIGATION_AND_MAIL_ONLY → NetworkPolicy{…}
│   │   ├── REDUCED_RISK → NetworkPolicy{…}
│   │   └── MOST_OPEN → NetworkPolicy{…}
│   └── detectionRules […]
│
└── MultiModePolicy
    ├── "type": "MultiMode"
    ├── modeVariants
    │   ├── userMode: GPS_ONLY
    │   │   ├── variants
    │   │   │   ├── id: "strict",   policy: {mode: LOCAL_ONLY}
    │   │   │   └── id: "balanced", policy: {mode: WHITELIST, spec: HostList[…]}
    │   │   └── defaultVariantId: "balanced"
    │   └── userMode: MOST_OPEN
    │       ├── variants
    │       │   └── id: "open", policy: {mode: FULL_OPEN}
    │       └── defaultVariantId: "open"
    └── detectionRules […]
```

## 🗂️ Directory Structure

Place your JSON file under:

```
app-policies/<category>/<packageName>.json
```

Example:

```
app-policies/navigation/com.example.app.json
```

Each category folder (e.g., `communication`, `navigation`, `video`) helps avoid merge conflicts and keeps things tidy. 🚀

---

## 📐 Supported Policy Types & Templates

Below are the three policy types. Copy the template that matches your use case and fill in your data.

### 1️⃣ FixedPolicy

Use when the same network rules apply to all modes.

```json
{
  "type": "Fixed",
  "packageName": "com.waze",
  "category": "NAVIGATION",
  "networkPolicy": {
    "mode": "BLACKLIST",
    "spec": {
      "type": "HostList",
      "hosts": [
        "*.waze.com",
        "venue-image.waze.com",
        "ads-resources.waze.com",
        "ads-resources-legacy.waze.com",
        "adsassets.waze.com",
        "social.waze.co.il"
      ]
    }
  },
  "minimumVersionCode": 1030416
}
```

### 2️⃣ ModeBasedPolicy

Use when you need different rules per user mode.

```json
{
  "type": "ModeBased",
  "packageName": "com.google.android.gm",
  "category": "MAIL",
  "minimumVersionCode": 0,
  "modePolicies": {
    "NAVIGATION_AND_MAIL_ONLY": {
      "mode": "BLACKLIST",
      "desc": "Allow only mails and block Google Chat",
      "spec": {
        "type": "HostList",
        "hosts": [
          "HOST_OF_GOOGLE_CHAT"
        ]
      }
    },
    "REDUCED_RISK": {
      "mode": "FULL_OPEN"
    }
  }
}
```

> 🛠 **Optional Key: `desc`**
> You can add a `"desc"` field at the same level as `mode` or `spec` to describe what this policy does. It is intended for human readers and will be stripped out at compile time.
>
> 🔄 **Mode Inheritance**
> If you define a policy for `REDUCED_RISK` but do not provide one for a higher mode (e.g., `MOST_OPEN`), the `REDUCED_RISK` policy will automatically apply to those modes when no other configuration is available.

### 3️⃣ MultiModePolicy

Use when each user mode has multiple variants, each with its own rules and optional activity/node detections.

```json
{
  "type": "MultiMode",
  "packageName": "com.whatsapp",
  "category": "COMMUNICATION",
  "minimumVersionCode": 0,
  "modeVariants": [
    {
      "userMode": "MOST_OPEN",
      "variants": [
        {
          "id": "open",
          "label": "Fully open",
          "policy": { "mode": "FULL_OPEN" }
        },
        {
          "id": "restricted",
          "label": "Only messages, no photos, videos and calling",
          "policy": {
            "mode": "WHITELIST",
            "spec": {
              "type": "HostList",
              "hosts": [
                "v.whatsapp.net",
                "static.whatsapp.net"
              ]
            }
          }
        },
        {
          "id": "block_groups",
          "label": "Block groups",
          "policy": { "mode": "FULL_OPEN" },
          "detectionRules": [
            {
              "type": "NODE",
              "targets": ["TODO"],
              "condition": "ONLY_IF",
              "action": "KILL_APP"
            }
          ],
          "overrideDefaultRules": false,
          "configurationRequired": true,
          "configurationKey": "whatsapp_groups_prefs"
        }
      ],
      "defaultVariantId": "open"
    }
  ],
  "detectionRules": [
    {
      "type": "NODE",
      "targets": [
        "com.whatsapp:id/newsletter_quick_forwarding_pill_container_key"
      ],
      "condition": "ONLY_IF",
      "action": "KILL_APP",
      "desc": "Kill app when entering the WhatsApp Update channel"
    }
  ]
}
```

* Root-level `detectionRules` apply across all variants.
* Within each variant:

  * `detectionRules`: rules specific to that variant.
  * `overrideDefaultRules`: `true` (default) to use only variant rules, `false` to merge with root rules.

---

## ✅ Checklist Before Submitting

1. 🔎 **Validate JSON** with a linter (e.g., [jsonlint.com](https://jsonlint.com/)) or use the project's built-in validation task (see below).
2. 📂 **Place** your file under the correct category folder.
3. 🔢 **Include a minimum version code** (`minimumVersionCode`) for each app. This is imperative for proper app validation. While our system can try to determine this automatically, it's not 100% reliable and will always use the latest version available.
4. 🔐 **Include the SHA1 signature** (`sha1`) of the app's certificate. This is crucial for security verification. Our system can attempt to retrieve this, but it's not always reliable.
5. 📜 **Commit only** the JSON file—no code or docs changes.
6. 📝 **PR title** should clearly state the app package.

> **Note**: For system apps (category `SYSTEM`), the minimum version code and SHA1 signature are not required.
> 
> **Important**: If our system cannot determine the minimum version code or signature for a non-system app, the CI will fail. Make sure these values are either provided in the policy file or can be reliably retrieved from the Aptoide API.

CI will reject invalid JSON or misplaced files. Good luck! 🙌

---

## 🛠️ Gradle Tasks

The project includes several Gradle tasks to help with development and validation:

### Validating JSON Files

To validate all JSON policy files for correctness:

```bash
./gradlew :generators:policies:validateJson
```

This task checks all JSON files in the `app-policies` directory to ensure they:
- Have valid JSON syntax
- Conform to the expected policy structure
- Can be properly parsed by the application

The task will report any validation errors found, making it easier to identify and fix issues before submitting.

### Checking Policies

To check policies for missing version codes and signatures:

```bash
./gradlew :generators:policies:checkPolicies
```

This task verifies that all policy files have:
- A valid minimum version code (greater than 0)
- A valid SHA1 signature for the app's certificate

The system will attempt to retrieve missing values from the Aptoide API, but this is not always reliable. The task will report any policies that fail these checks, except for system apps which are exempt.

**Note**: This task is run as part of the CI pipeline. If the system cannot determine the minimum version code or signature for a non-system app, the CI will fail.

---

## ❓ Questions & Answers

### What is the difference between `LOCAL_ONLY` and `OFFLINE`?

Both modes block all outbound Internet traffic; the distinction is mainly about the user experience:

| Mode            | Behaviour                                                                                                                                                                                                | When to choose it?                                                                                                                            |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| **LOCAL\_ONLY** | KDroid filters the connection **as soon as the app is opened** and shows a notification prompting the user to enable “Local mode”. Only local network traffic (Wi‑Fi/LAN — e.g. 192.168.x.x) is allowed. | Apps whose **main purpose relies on the local network**, such as smart‑home controllers, drone control apps, or Wi‑Fi file transfer tools.    |
| **OFFLINE**     | No network traffic is allowed at all (neither Internet nor LAN). KDroid shows **no notification**.                                                                                                       | Apps that are **primarily offline** and nearly never need local network access, e.g. video players, document viewers, or single‑player games. |

**In short**

* Pick **`LOCAL_ONLY`** when the user needs to talk to nearby devices (NAS, Chromecast, smart lights, etc.). The notification helps them enable local mode quickly.
* Pick **`OFFLINE`** for apps that work perfectly without any network so you avoid showing an unnecessary notification.

---

### ⚠️ Optional Flags: Content and Risk Warnings

You can optionally include the following boolean flags in your policy files to help with sensitive app filtering and user awareness:

* **`hasUnmodestImage`**: Set to `true` if the app contains unfiltered, inappropriate or immodest visual content (e.g. uncovered women in media banners, icons, or previews). This indicates that the user must explicitly accept the risk in order to use the app.

* **`isPotentiallyDangerous`**: Set to `true` if the app can pose a technical or security risk, such as remote access tools or apps capable of controlling other devices over open internet connections. This flag helps apply stricter rules or prompt additional warnings.

* **`requiresPlayStoreInstallation`**: Set to `true` if the app must be installed from the Google Play Store for specific reasons (e.g., licensing requirements, app integrity verification, or access to Play Store-specific features). This indicates that the app should not be sideloaded from alternative sources.

* **`isRecommendedInStore`**: Set to `true` if the app should be recommended in the store. By default, this is set to `false`. When set to `true`, the app will be highlighted or featured in the store, making it more visible to users.

These fields are optional but enforced by default: apps with these flags will not appear in the store or be granted internet access unless the user has explicitly accepted the risk. Additionally, apps with `hasUnmodestImage` or `isPotentiallyDangerous` set to `true` will be excluded from the **REDUCED\_RISK** mode.

Apps marked with any of these flags will only appear in the store or receive internet access **if the user has explicitly acknowledged the risk** and chosen to enable them manually.

---

### Why would an app require Play Store installation?

Some applications must be installed exclusively from the Google Play Store for several important reasons:

1. **License Verification**: Many paid or subscription-based apps use Google Play's licensing services to verify purchases. When installed from alternative sources, these apps may not function properly as they cannot validate the purchase.

2. **App Integrity & Security**: The Play Store provides app signing and verification mechanisms that help ensure the app hasn't been tampered with. Apps handling sensitive data (like banking or payment apps) often require this security layer.

3. **Google Play Services Dependencies**: Some apps rely heavily on specific Google Play Services features that are only fully available for apps installed through official channels.

4. **In-App Purchases**: Applications that offer in-app purchases typically use Google Play's billing system, which may not function correctly when the app is sideloaded.

5. **Auto-Updates**: Apps installed from the Play Store can receive automatic updates, ensuring users always have the latest security patches and features.

When you mark an app with `requiresPlayStoreInstallation: true`, you're indicating to users that the app should not be sideloaded from alternative sources, as this could lead to functionality issues, security risks, or licensing problems.

---

### 🔄 Example: Risky Applications

#### 1. Bank App with Inappropriate Visuals

```json
{
  "type": "Fixed",
  "packageName": "com.example.fakebank",
  "category": "FINANCE",
  "minimumVersionCode": 100,
  "hasUnmodestImage": true,
  "isRecommendedInStore": false,
  "networkPolicy": {
    "mode": "FULL_OPEN"
  }
}
```

#### 2. Remote Access Tool

```json
{
  "type": "Fixed",
  "packageName": "com.example.remotecontrol",
  "category": "TOOLS",
  "minimumVersionCode": 50,
  "isPotentiallyDangerous": true,
  "networkPolicy": {
    "mode": "FULL_OPEN"
  }
}
```

#### 3. App Requiring Play Store Installation

```json
{
  "type": "Fixed",
  "packageName": "com.example.licensedapp",
  "category": "PRODUCTIVITY",
  "minimumVersionCode": 75,
  "requiresPlayStoreInstallation": true,
  "networkPolicy": {
    "mode": "FULL_OPEN"
  }
}
```

All of these apps are tagged for additional caution. They will only be functional or visible if the user has agreed to unlock them by accepting the risks.

---

## Importing KDroid Database Modules

KDroid Database provides multiple modules that you can use in your own projects:

### Core Module [![Maven Central](https://img.shields.io/maven-central/v/io.github.kdroidfilter.database/core)](https://search.maven.org/artifact/io.github.kdroidfilter.database/core)

The core module contains the essential data structures and enums for the KDroid Database, including the `AppCategory` enum and policy definitions.

```kotlin
dependencies {
    implementation("io.github.kdroidfilter.database:core:<version>")
}
```

### Localization Module [![Maven Central](https://img.shields.io/maven-central/v/io.github.kdroidfilter.database/localization)](https://search.maven.org/artifact/io.github.kdroidfilter.database/localization)

The localization module extends the core module to provide localized category names in multiple languages (English, Hebrew, and French). It includes platform-specific extensions for Android and JVM.

```kotlin
dependencies {
    implementation("io.github.kdroidfilter.database:localization:<version>")
}
```

### Downloader Module [![Maven Central](https://img.shields.io/maven-central/v/io.github.kdroidfilter.database.downloader/core)](https://search.maven.org/artifact/io.github.kdroidfilter.database.downloader/core)

The downloader module provides functionality to download the latest store and policies databases from GitHub releases. It includes methods to download databases for multiple languages.

```kotlin
dependencies {
    implementation("io.github.kdroidfilter.database.downloader:core:<version>")
}
```

#### Usage Example

```kotlin
// Create a DatabaseDownloader instance
val databaseDownloader = DatabaseDownloader()

// Download store databases for all languages (en, fr, he)
val outputDir = "path/to/output/directory"
val storeResults = databaseDownloader.downloadLatestStoreDatabases(outputDir)

// Check download results
storeResults.forEach { (language, success) ->
    if (success) {
        println("Successfully downloaded store database for $language")
    } else {
        println("Failed to download store database for $language")
    }
}

// Download policies database
val policiesResult = databaseDownloader.downloadLatestPoliciesDatabase(outputDir)
if (policiesResult) {
    println("Successfully downloaded policies database")
} else {
    println("Failed to download policies database")
}
```

### DAO Module [![Maven Central](https://img.shields.io/maven-central/v/io.github.kdroidfilter.database.dao/core)](https://search.maven.org/artifact/io.github.kdroidfilter.database.dao/core)

The DAO (Data Access Object) module provides a clean interface for database operations, abstracting away direct SQL queries. It includes DAOs for accessing application data and version information, as well as utility classes for working with the data.

```kotlin
dependencies {
    implementation("io.github.kdroidfilter.database.dao:core:<version>")
}
```

#### Key Components

- **ApplicationsDao**: Provides methods for loading and searching applications in the database, as well as checking and retrieving recommended applications
- **CategoriesDao**: Provides methods for retrieving categories and applications by category
- **VersionDao**: Handles database version management operations
- **AppInfoWithExtras**: A data class that extends the GooglePlayApplicationInfo model with additional information
- **Data Consistency Verification**: Utilities to ensure alignment between JSON policy files and database records

#### Features

- **Data Consistency Verification**: Ensure perfect alignment between JSON policy files and database records
  - Package name consistency validation
  - Category mapping verification
  - `isRecommendedInStore` flag synchronization
- **Integration with Release Fetcher**: Seamless connection with the Downloader module for reliable database retrieval
- **JUnit 5 Support**: Enhanced test framework with improved reporting and parallel test execution

#### Usage Example

```kotlin
// Load applications from the database
val applications = ApplicationsDao.loadApplicationsFromDatabase(
    database = database,
    deviceLanguage = "en",
    creator = { id, categoryLocalizedName, appInfo ->
        AppInfoWithExtras(
            id = id,
            categoryLocalizedName = categoryLocalizedName,
            app = appInfo
        )
    }
)

// Search for applications in the database
val searchResults = ApplicationsDao.searchApplicationsInDatabase(
    database = database,
    query = "calculator",
    deviceLanguage = "en",
    creator = { id, categoryLocalizedName, appInfo ->
        AppInfoWithExtras(
            id = id,
            categoryLocalizedName = categoryLocalizedName,
            app = appInfo
        )
    }
)

// Check if an application is recommended in the store
val isRecommended = ApplicationsDao.isRecommendedInStore(
    database = database,
    appId = "com.example.app"
)

// Get all applications that are recommended in the store
val recommendedApps = ApplicationsDao.getRecommendedApplications(
    database = database,
    deviceLanguage = "en",
    creator = { id, categoryLocalizedName, appInfo ->
        AppInfoWithExtras(
            id = id,
            categoryLocalizedName = categoryLocalizedName,
            app = appInfo
        )
    }
)

// Get all categories with localized names
val categories = CategoriesDao.getAllCategories(
    database = database,
    deviceLanguage = "en"
)

// Get applications by category ID
val appsByCategoryId = CategoriesDao.getApplicationsByCategoryId(
    database = database,
    categoryId = 1,
    deviceLanguage = "en",
    creator = { id, categoryLocalizedName, appInfo ->
        AppInfoWithExtras(
            id = id,
            categoryLocalizedName = categoryLocalizedName,
            app = appInfo
        )
    }
)

// Get applications by category name
val appsByCategoryName = CategoriesDao.getApplicationsByCategoryName(
    database = database,
    categoryName = "NAVIGATION",
    deviceLanguage = "en",
    creator = { id, categoryLocalizedName, appInfo ->
        AppInfoWithExtras(
            id = id,
            categoryLocalizedName = categoryLocalizedName,
            app = appInfo
        )
    }
)

// Get applications by category enum
val appsByCategory = CategoriesDao.getApplicationsByCategory(
    database = database,
    category = AppCategory.NAVIGATION,
    deviceLanguage = "en",
    creator = { id, categoryLocalizedName, appInfo ->
        AppInfoWithExtras(
            id = id,
            categoryLocalizedName = categoryLocalizedName,
            app = appInfo
        )
    }
)

// Get the current database version
val currentVersion = VersionDao.getCurrentVersion(database)

// Update the database version
val updateSuccess = VersionDao.updateVersion(database, "NEWVERSION")

```

#### Note on Sample Code

For a simplified overview of how to use the database, please consult the example in the `sample` directory. The sample demonstrates basic database operations including downloading, querying, and displaying data.

> ⚠️ **Important Warning**: The sample code uses `runBlocking` for database downloads, which is **prohibited** in production code. This is only done for demonstration purposes. In real applications, always use proper coroutine scopes and avoid blocking the main thread.

The DAO module is actively evolving to satisfy more needs and use cases. Contributions and pull requests are welcome to enhance its functionality and performance.
