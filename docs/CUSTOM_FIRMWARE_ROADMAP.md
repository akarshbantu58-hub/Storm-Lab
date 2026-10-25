# StormLab Custom Firmware Investigation Roadmap

## Goal

Explore whether a calculator can be added to the boAt Storm Call 3 itself, ideally using its existing menu and visual style, while preserving stock Crest+ OS. Use the Galaxy Tab A9 and GitHub Actions for companion-app development.

## Current decision

**Watch-side app installation is not yet verified. Do not flash or write vendor-specific GATT characteristics.** The project has no verified firmware image, chipset/SoC identification, flash map, bootloader/recovery documentation, model-specific update protocol, or proven recovery procedure for this exact device.

A Bluetooth characteristic with WRITE or WRITE_NO_RESPONSE properties is not proof that it accepts firmware. Standard BLE identity fields are not proof of OS-level access.

## Known device evidence

- Product: boAt Storm Call 3
- Bluetooth name observed: STORMCALL3_1A46
- Standard Device Information model field: WA34V10
- Firmware revision read over BLE: V3.00.58
- Manufacturer field read over BLE: KaHa Pte Ltd
- Hardware revision field read over BLE: V3.0
- Display: 240 × 284 pixels, per official product support
- Operating system: Crest+ OS, per official product support
- Official support documentation directs users to the boAt Crest companion app and describes watch-face customization and built-in features. The public documentation reviewed does not document a third-party app SDK or a procedure for installing arbitrary executable apps on this model.

Official reference: https://support.boat-lifestyle.com/articles/wearables/storm-call-3/686bdba77adcb3300d7f1d4c

These values identify observed fields and documented product features; they do not identify the exact chipset or prove that third-party firmware or app installation is possible.

## Workstreams

### A. Verify whether apps are supported

1. Ask boAt support whether model WA34V10, hardware V3.0, firmware V3.00.58 supports third-party apps or a developer SDK.
2. Request official model-specific documentation for the app runtime, packaging format, installation process, and supported APIs, if any.
3. If boAt confirms a supported mechanism, build a small calculator prototype against that documented interface and match the existing watch UI only to the extent supported by its documented design system.
4. If no supported mechanism exists, keep the watch unchanged and clearly label any watch-sized calculator preview as a companion-app mockup, not installed watch software.

### B. Hardware and recovery feasibility

Only promote firmware work when evidence is available:
1. Find trustworthy technical documentation for the exact Storm Call 3 / WA34V10 hardware.
2. Identify the exact SoC, flash type/capacity, memory map, and signed-image requirements from reliable documentation.
3. Establish whether an official or documented update/recovery interface exists.
4. Confirm an exact-match firmware package and its provenance, integrity, and compatibility.
5. Confirm a recovery route and failure behavior before any write is considered.

If any critical item remains unknown, keep firmware writes disabled.

### C. Companion features and release discipline

- Preserve BLE scan, connection, read-only GATT discovery, paired-device listing, theme toggle, diagnostic report, saved standard readings, OS-readiness screen, firmware-readiness screen, and calculator in the Android StormLab app.
- Prefer standard, documented Bluetooth services and explicit user consent.
- Do not infer vendor command formats from UUIDs or characteristic properties.
- Use GitHub Actions to compile the Android companion app; an APK is not watch firmware.
- Keep commits small and preserve existing features. Record build run URLs and commit SHAs.

## Readiness gates

| Gate | Required evidence | Current status |
|---|---|---|
| Third-party app support | Official model-specific confirmation or SDK | Not documented / unverified |
| Exact chipset/SoC | Reliable model-specific hardware documentation or verified read-only evidence | Unknown |
| Flash layout | Verified memory/partition map | Unknown |
| Bootloader/update access | Documented exact-model method | Unknown |
| Firmware package | Authentic, exact-hardware compatible image | Not found |
| Recovery path | Verified recovery procedure and needed tools | Not established |
| Safe installation | Compatibility and recovery verified together | Blocked |

## Immediate next steps

1. Obtain a model-specific answer from boAt about third-party app support and developer tooling.
2. Preserve the watch's stock firmware and read-only StormLab diagnostics while waiting.
3. If a documented app mechanism is confirmed, implement a calculator using it. Otherwise, do not present a mockup or Android APK as a watch-side installation.
4. Update this roadmap only when evidence is verified; do not convert guesses into implementation assumptions.

## Non-goals for the current prototype

- No guessed vendor-specific BLE writes.
- No bootloader/recovery entry attempts.
- No firmware transfer, erase, unlock, or flash operation.
- No claims that a generic smartwatch firmware will work on this model.
