# Implementation Plan - Fix 16KB Page Size Alignment Error

The error `program alignment (4096) cannot be smaller than system page size (16384)` occurs because the app is running on an Android device with a 16KB page size (likely Android 15/16 with `targetSdk 36`), but the `libgfortran.so.3` library (bundled with `numpy`/`pandas`) was compiled with 4KB alignment.

To fix this, we need to ensure the project uses 16KB-aligned native libraries.

## User Review Required

> [!IMPORTANT]
> This fix involves upgrading the Python version in the app. Chaquopy 17.0.0 supports 16KB page sizes, but the best compatibility and most up-to-date 16KB-aligned wheels are provided for **Python 3.13**.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Bidness_Time/Documents/GitHub/SotACalc/android/app/build.gradle.kts)
- Update `chaquopy.defaultConfig.version` to `"3.13"`.
- Ensure `pip` installs for `numpy` and `pandas` pull the latest versions compatible with 16KB.
- (Optional) If you must stay on 3.10, we will try to force a refresh of the wheels, but 3.13 is strongly recommended for 16KB devices.

### Local Environment (User Action)
- Since you are using `buildPython` pointing to an Anaconda `py310` environment, you should ideally create a `py313` environment or let Chaquopy use its default build Python if compatible.

## Verification Plan

### Manual Verification
- Rebuild the app and run it on the 16KB device/emulator.
- Verify that `Python.start()` completes without the `libgfortran.so.3` alignment error.
- Verify that NumPy and Pandas can be imported successfully.
