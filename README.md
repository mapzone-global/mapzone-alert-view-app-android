# Mapzone Alert View App
Note: Contact to [Vietmap](https://zalo.me/3189066936017422854) support if you need Vietmap API credentials.

## Table of contents

- [Install](#install)
- [Permissions](#permissions)
- [Configure](#configure)
- [Start a route & feed GPS](#start-a-route--feed-gps)
- [Receive bitmaps & voice](#receive-bitmaps--voice)
- [API reference](#api-reference)
- [Server error codes](#server-error-codes)
- [Troubleshooting / quick fixes](#troubleshooting--quick-fixes)

---

## Install

The SDK is published on **JitPack**.

**1. Add the JitPack repository** — in `settings.gradle.kts`
(`dependencyResolutionManagement`) or your root `build.gradle`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2. Add the dependency** — in your app `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.mapzone-global:mapzone-alert-view-android:<lastest-version>")
}
```
---

## Permissions
Add these to your app `AndroidManifest.xml`:

```xml
<!-- Required: the SDK fetches segment data over the network -->
<uses-permission android:name="android.permission.INTERNET"/>

<!-- Required by your app to obtain GPS you feed into onLocation.
     On Android 12+ (API 31) a FINE request must be accompanied by COARSE. -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

If you keep alerting while the app is in the background (a navigation app
usually does), run your GPS collection in a **foreground service** and add the
matching permissions yourself:

```xml
<!-- Only if you run location in a foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION"/>
<!-- Android 13+ (API 33): to show the service notification -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```
---

## Configure

Create a `local.properties` file in your app's root directory and add your API credentials, `local.properties.example` shows the keys you need to fill.

Create one `AlertViewManager` and call `configure(...)` **before `start`**:

```java
AlertViewManager manager = new AlertViewManager();

manager.configure(
        baseUrl,        // String  — Vietmap service base URL
        apiKeyId,       // String  — your API key id
        apiKey,         // String  — your API key secret
        vehicleId,      // String  — your vehicle id
        vehicleType,    // int     — 1..9 (see Vehicle types)
        seats,          // int     — seats for coaches; 0 = default for the type
        weights,        // double  — gross weight (tonnes) for trucks; 0 = default
        maxSnapMeters); // double  — max snap distance (m); <= 0 = server default (~25 m)
```

> **Since 0.0.3:** the SDK no longer takes a `bundleId` parameter — it reads
> your app's package name (`applicationId`) itself. Make sure the
> `applicationId`/package name registered for your API key on the Vietmap
> console matches the one your app ships with.

Optional configuration (call after `configure`, before `start`):

| Method | Purpose |
|---|---|
| `setSegmentUrl(String url)` | Override the segment-fetch endpoint. Empty = derive from `baseUrl`; when set, the request goes to **exactly** that URL. |
| `setExtraHeaders(Map<String,String>)` | **Add** extra headers to the segment request. Returns `null` on success, or an error message. Cannot override the reserved header `Content-Type` (case-insensitive); a header key may not contain `:`. |
| `setExtraBodyFields(Map<String,String>)` | **Add** extra body fields (string values) to the segment request. Returns `null` on success. |
| `setMutedAlertTypes(VoiceAlertType...)` | Mute specific alert voices (camera, toll, …). Speed-limit & speeding voices can never be muted. |

`setExtraHeaders` / `setExtraBodyFields` reject empty keys, reserved keys, a
colon in a header key, and any value containing a newline; on rejection they
return a non-null message and **keep the previous extras**. All optional
settings persist across reroutes.

---

## Start a route & feed GPS

```java
// Start (or reroute — just call again with a new polyline):
manager.start(routePolyline);   // route geometry, encoded 1e6

// On every GPS frame from your location provider:
manager.onLocation(
        location.getLatitude(),
        location.getLongitude(),
        location.getBearing(),       // degrees, 0 = N, 90 = E
        speedKmh,                    // current speed in km/h
        location.getAccuracy());     // horizontal accuracy in metres

// When navigation ends (or before a reroute):
manager.reset();
```

> `onLocation` takes speed in **km/h** — the SDK converts to m/s internally.
> Feed frames at ~1 Hz: fixes arriving faster than ~1 Hz are dropped by the
> engine and the previous frame is kept.

---

## Receive bitmaps & voice

Register a `BitmapCallback` to drive your UI:

```java
manager.setBitmapCallback((currentBmp, speedStatus,
                           nextBmp, nextDistMeters,
                           cameraBmp, cameraDistMeters,
                           tollBmp, tollDistMeters,
                           voiceWav) -> {
    // currentBmp     : speed-sign for the current segment (null if no match)
    // speedStatus    : 0 = within limit, 1 = approaching (within 5 km/h), 2 = speeding
    // nextBmp        : preview of the next speed sign, with nextDistMeters ahead
    // cameraBmp      : preview of an upcoming speed camera, cameraDistMeters ahead
    // tollBmp        : preview of an upcoming toll booth, tollDistMeters ahead
    // voiceWav       : optional WAV bytes (PCM16 mono 22050 Hz), null if nothing to say
    runOnUiThread(() -> render(currentBmp, speedStatus, nextBmp, nextDistMeters,
                               cameraBmp, cameraDistMeters, tollBmp, tollDistMeters));
});
```

The callback fires on the **main thread**. Use `speedStatus` to colour your
speedometer (e.g. green / amber / red).

**Voice:** by default the SDK plays voice clips itself through a built-in
`MediaPlayer` priority queue — you don't have to do anything. Register a
`VoiceCallback` only if you need to take over playback (mixing, ducking, gating
on app state). Once you register it, the built-in player stops and you own
playback:

```java
manager.setVoiceCallback((wavBytes, trigger, priority) -> {
    // trigger  : native VoiceTrigger enum
    // priority : 0 = current (lowest, skipped first), 1 = normal, 2 = speeding
});
```

**Fetch results (optional):**

```java
manager.setResultCallback((success, errorCode, errorMessage) -> {
    // errorCode == 0           → success
    // errorCode  > 0           → server envelope code (see "Server error codes")
    // errorCode  < 0           → local SDK failure (see errorMessage)
});
```

---

## Troubleshooting / quick fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| No bitmaps at all | `start()` not called, or empty/invalid polyline | Pass a route polyline **encoded with precision 1e6** (the geometry from a Vietmap Directions response). Call `start` before `onLocation`. |
| `currentBmp` is always `null` | GPS point is too far from the route | Feed real GPS frames near the route; raise `maxSnapMeters` in `configure` (e.g. 30–50 m) if your GPS is noisy. |
| Some GPS frames seem ignored | Feeding faster than ~1 Hz | The engine drops fixes arriving faster than ~1 Hz and keeps the previous frame. Feed at the usual ~1 Hz GPS rate. |
| Result callback `errorCode 1001` | A route point is outside Vietnam | Validate coordinates before `start` (`lng∈[102,110]`, `lat∈[8,24]`). |
| Result callback `errorCode 3003` | Bad `vehicleType` | Pass a value in **1..9** (see vehicle types). |
| Result callback `errorCode 3004` | Route did not map-match | Verify the polyline is a real road route; check the encoding precision is `1e6`. |
| Result callback `errorCode 2000` | Device clock is off | The auth uses a server timestamp window (±10 s) — sync the device clock. |
| Result callback `errorCode 2002` | Wrong API key / secret | Re-check `apiKeyId` / `apiKey` passed to `configure`, and confirm your app's package name matches the one registered for the key (bundle id is now read automatically). |
| Result callback `errorCode < 0` | Local network / parse failure | Check connectivity, `baseUrl`, and the `INTERNET` permission; read `errorMessage`. |
| Speed shows wrong/zero limit | The link has no declared limit for this vehicle | `speed = 0` means "no declared limit" for that vehicle type — render it as unknown, not as 0 km/h. |
| Voice never plays | A `VoiceCallback` is registered but does nothing | Either implement playback in your callback, or **don't** register one and let the SDK's built-in player handle it. |
| A category's voice won't mute | Trying to mute speed/speeding | Speed-limit & speeding voices are never mutable by design. |
| Voice plays but I want it silent on some alerts | Need per-category muting | `setMutedAlertTypes(VoiceAlertType.TOLL, VoiceAlertType.SPEED_CAMERA, …)`; the bitmap still shows. |
| `setExtraHeaders` / `setExtraBodyFields` returns a non-null string | Reserved/empty key, colon in a header key, or newline value | Read the returned message; the previous extras are kept. Don't try to override reserved keys. |
| Native library fails to load | ABI not packaged | Ensure your build includes `arm64-v8a` (and the ABIs you target); `System.loadLibrary("alert_view_sdk")` needs the matching `.so`. |
| Wrong speed magnitude | Passing m/s | `onLocation` expects **km/h** — multiply m/s by 3.6. |
| No GPS reaches the SDK | Location permission not granted at runtime | Request `ACCESS_FINE_LOCATION` (with `ACCESS_COARSE_LOCATION` on Android 12+) at runtime before feeding `onLocation`. |

---

## Contact & support
[<img src="https://bizweb.dktcdn.net/100/415/690/themes/804206/assets/logo.png?1689561872933" height="40"/> </p>](https://maps.vietmap.vn/)
- [Website](https://maps.vietmap.vn/)
- [Documentation](https://maps.vietmap.vn/docs/)
- [Zalo OA](https://zalo.me/3189066936017422854)
