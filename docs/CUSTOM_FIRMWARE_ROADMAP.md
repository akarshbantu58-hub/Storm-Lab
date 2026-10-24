# StormLab Custom Firmware Investigation Roadmap

## Goal

Explore whether additional features can be added to the boAt Storm Call 3 while preserving its stock Crest+ OS. Use the Galaxy Tab A9 and GitHub Actions as the development environment.

## Current decision

**Do not flash or write vendor-specific GATT characteristics.** The project currently has no verified firmware image, chipset/SoC identification, flash map, bootloader/recovery documentation, model-specific update protocol, or proven recovery procedure for this exact device.

A Bluetooth characteristic with WRITE or WRITE_NO_RESPONSE properties is not proof that it accepts firmware. Standard BLE identity fields are not proof of OS-level access.

## Known device evidence

- Product: boAt Storm Call 3
- Bluetooth name observed: STORMCALL3_1A46
- Standard Device Information model field: WA34V10
- Firmware revision read over BLE: V3.00.58
- Manufacturer field read over BLE: KaHa Pte Ltd
- Hardware revision field read over BLE: V3.0
- BLE services discovered: standard Generic Access, Generic Attribute, Device Information, Battery, plus vendor-specific services
- Official product support describes the watch as running Crest+ OS and directs users to the boAt Crest app for normal watch functionality.

These values identify observed fields; they do not identify the exact chipset or prove that third-party firmware is possible.

## Workstreams

### A. Companion features (start here)

Build features in the Android StormLab app without replacing or patching the watch OS:
- Preserve BLE scan, connection, read-only GATT discovery, paired-device listing, theme toggle, diagnostic report, saved standard readings, OS-readiness screen, and firmware-readiness screen.
- Prefer standard, documented Bluetooth services and explicit user consent.
- Add a feature only when its protocol and expected behavior are known.
- Do not infer vendor command formats from UUIDs or characteristic properties.

### B. Hardware and recovery feasibility

Only promote this workstream when evidence is available:
1. Find a trustworthy technical document for the exact Storm Call 3 / WA34V10 hardware.
2. Identify the exact SoC, flash type/capacity, memory map, and signed-image requirements from reliable documentation.
3. Establish whether an official or documented update/recovery interface exists.
4. Confirm an exact-match firmware package and its provenance, integrity, and compatibility.
5. Confirm a recovery route and failure behavior before any write is considered.

If any critical item remains unknown, keep firmware writes disabled.

### C. Build and release discipline

- Use GitHub Actions to compile the Android companion app.
- Test app changes on the tablet without sending writes to the watch.
- Keep commits small and preserve existing features.
- Save the build run URL and commit SHA for each tested change.
- Never label a build as watch firmware unless it is actually a firmware image for a verified target. The current APK is an Android companion app.

## Readiness gates

| Gate | Required evidence | Current status |
|---|---|---|
| Exact chipset/SoC | Reliable model-specific hardware documentation or verified read-only evidence | Unknown |
| Flash layout | Verified memory/partition map | Unknown |
| Bootloader/update access | Documented exact-model method | Unknown |
| Firmware package | Authentic, exact-hardware compatible image | Not found |
| Recovery path | Verified recovery procedure and needed tools | Not established |
| Safe installation | Compatibility and recovery verified together | Blocked |

## Immediate next steps

1. Keep the watch on its stock firmware and continue using the official companion app for normal functionality.
2. Preserve the current StormLab read-only diagnostic workflow.
3. Add useful Android-side features first, one at a time.
4. Update this document only when evidence is verified; do not convert guesses into implementation assumptions.

## Non-goals for the current prototype

- No guessed vendor-specific BLE writes.
- No bootloader/recovery entry attempts.
- No firmware transfer, erase, unlock, or flash operation.
- No claims that a generic smartwatch firmware will work on this model.
