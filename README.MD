# 📄 Kdroid Database Policies Contribution Guide

Welcome to the community-driven database of kosher filtering rules for applications. This guide is primarily used by KDroid Filter but is freely available for anyone to use. Licensed under the LGPL, you may integrate these policies even in closed-source applications. At this stage, it is recommended to contribute only host-based rules, as the tools for detecting apps and UI nodes are not yet available.

The KDroid app store dynamically adapts its application listings based on the current **UserMode** (levels 0 to 5) to only show apps that are appropriate for each level:

* In **OFFLINE** (0) or **LOCAL\_ONLY** (1) modes, only updates for already installed apps that can work offline will be shown.
* In **GPS\_ONLY** mode (2), only apps in the **NAVIGATION** category will be visible.
* In **GPS\_AND\_MAIL** mode (3), apps in the **MAIL** category will also be included.
* In **REDUCED\_RISK** mode (4), only apps from trusted categories will appear: **TORAH**, **PRODUCTIVITY**, **TOOLS**, **FINANCE**, **MUSIC\_AUDIO**, and **HOME**.
* Finally, in **MOST\_OPEN** mode (5), all available apps will be listed.

For applications using a **ModeBasedPolicy**, if a rule is defined for a lower level (e.g. 1 or 2), any higher levels (3, 4, or 5) without an explicit configuration will automatically inherit that rule.

Here is a simple tree of all available modes :
---

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

1. 🔎 **Validate JSON** with a linter (e.g., [jsonlint.com](https://jsonlint.com/)).
2. 📂 **Place** your file under the correct category folder.
3. 📜 **Commit only** the JSON file—no code or docs changes.
4. 📝 **PR title** should clearly state the app package.

CI will reject invalid JSON or misplaced files. Good luck! 🙌

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

These fields are optional but enforced by default: apps with these flags will not appear in the store or be granted internet access unless the user has explicitly accepted the associated risk.

Apps marked with either of these flags will only appear in the store or receive internet access **if the user has explicitly acknowledged the risk** and chosen to enable them manually.

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

Both of these apps are tagged for additional caution. They will only be functional or visible if the user has agreed to unlock them by accepting the risks.
