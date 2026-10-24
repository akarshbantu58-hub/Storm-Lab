# StormLab (prototype 0.1)

A read-only Bluetooth Low Energy (BLE) discovery app for investigating the boAt Storm Call 3 from an Android tablet.

## What it does
- Scans nearby BLE devices.
- Connects to a selected device using GATT.
- Lists discovered service and characteristic UUIDs and characteristic properties.
- Does not write commands, update firmware, or flash the watch.

## Build
Open **Actions** → **Build StormLab APK** → **Run workflow**. When it finishes, open the run and download the `StormLab-debug-apk` artifact. Extract it and install `app-debug.apk`.

## Safety and limitations
This app does not install Android/Wear OS on the watch, provide a browser, or play YouTube. Finding BLE services does not prove the watch protocol is compatible. Do not send arbitrary writes or firmware images; unverified firmware can permanently damage the watch.

## Custom firmware investigation

See [docs/CUSTOM_FIRMWARE_ROADMAP.md](docs/CUSTOM_FIRMWARE_ROADMAP.md) for the hardware evidence, recovery requirements, safe companion-feature plan, and current blockers. Watch firmware flashing remains disabled until exact-model compatibility and a reliable recovery path are verified.
