# PDF Unlocker Implementation Plan

## Goals

Build an offline-only Android app that lets a user select an encrypted PDF, enter its password, remove password protection using PDFBox Android, and save the unlocked PDF to a user-chosen location.

Core constraints:

- Offline only
- No ads
- No analytics
- No telemetry or background network behavior
- Material 3 UI
- User controls both input PDF selection and output save location

## Current Project Baseline

- Single Android application module: `:app`
- Package namespace: `com.amit.pdfunlocker`
- UI framework: Jetpack Compose with Material 3
- Main entry point: `MainActivity`
- Min SDK: 26
- Target SDK: 36

## Proposed Project Structure

Keep the app simple and focused:

```text
app/src/main/java/com/amit/pdfunlocker/
  MainActivity.kt
  PdfUnlockerApp.kt
  ui/
    UnlockScreen.kt
    components/
      FileSelectionCard.kt
      PasswordField.kt
      SaveLocationRow.kt
      UnlockProgressDialog.kt
  domain/
    PdfUnlockResult.kt
    PdfUnlocker.kt
  data/
    PdfUnlockerRepository.kt
  util/
    FileNameUtils.kt
    UriUtils.kt
```

This structure separates:

- Compose UI from PDF processing
- Android URI/content handling from PDFBox logic
- Result/error modeling from screen state

## Dependencies

Add PDFBox Android:

```kotlin
implementation("com.tom-roush:pdfbox-android:<version>")
```

Keep existing Material 3 and Compose dependencies.

Do not add:

- Ad SDKs
- Analytics SDKs
- Crash reporting SDKs
- Network libraries unless a future requirement explicitly needs them

## Android Permissions And Privacy

Prefer Android Storage Access Framework instead of broad storage permissions.

Use:

- `ACTION_OPEN_DOCUMENT` for selecting the encrypted PDF
- `ACTION_CREATE_DOCUMENT` for choosing the save location

Avoid:

- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`
- Internet permission

The manifest should not include:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

This keeps the app offline-only by design.

## User Flow

1. User opens app.
2. User taps a file picker button.
3. App launches `ACTION_OPEN_DOCUMENT` filtered to `application/pdf`.
4. User selects encrypted PDF.
5. App displays selected file name.
6. User enters PDF password.
7. User taps unlock/save action.
8. App launches `ACTION_CREATE_DOCUMENT` with a suggested unlocked file name.
9. User chooses output location.
10. App reads the input PDF through `ContentResolver`.
11. App loads the PDF with PDFBox Android and the provided password.
12. App removes security protection.
13. App writes the unlocked PDF to the output URI.
14. App shows success state with a clear confirmation.

## UI Plan

Use a single-screen Material 3 Compose interface.

Primary screen sections:

- Top app bar with app name
- PDF selection area
- Password input field
- Save/unlock primary action
- Inline validation and error messages
- Progress indicator while unlocking
- Success confirmation after save

Expected states:

- No PDF selected
- PDF selected, password empty
- Ready to unlock
- Save location picker pending
- Unlocking in progress
- Success
- Error

Material 3 components:

- `Scaffold`
- `TopAppBar`
- `OutlinedButton`
- `Button`
- `OutlinedTextField`
- `LinearProgressIndicator` or modal progress indicator
- `SnackbarHost`
- `Card` only where it frames a specific repeated or functional item

Password field behavior:

- Hidden by default
- Visibility toggle icon
- Supports paste
- Does not persist password across app restarts
- Clears password after successful unlock

## State Management

For the first implementation, keep state local to the screen or use a small state holder.

Suggested screen state:

```kotlin
data class UnlockUiState(
    val selectedPdfUri: Uri? = null,
    val selectedPdfName: String? = null,
    val password: String = "",
    val isUnlocking: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
```

If the screen grows later, introduce a `ViewModel`.

## PDF Unlocking Logic

Create a `PdfUnlocker` domain class responsible for PDFBox operations.

Expected behavior:

1. Initialize PDFBox Android if required by the library.
2. Copy input URI stream into a temporary file or byte buffer.
3. Load the PDF using the supplied password.
4. Verify the document is encrypted.
5. Remove security:

```kotlin
document.isAllSecurityToBeRemoved = true
```

6. Save the unlocked document to the output stream.
7. Close all streams and the PDF document safely.

Important handling:

- Use `use {}` blocks for streams and documents where possible.
- Run PDF processing on `Dispatchers.IO`.
- Never log the password.
- Never store the password.
- Delete temporary files after completion if temp files are used.

## Error Handling

Map technical errors to user-friendly messages.

Cases to handle:

- No PDF selected
- Empty password
- User cancels input picker
- User cancels output picker
- Wrong password
- PDF is not encrypted
- Corrupt or unsupported PDF
- Cannot read selected file
- Cannot write to selected save location
- Out-of-memory or very large file failure

Suggested result model:

```kotlin
sealed interface PdfUnlockResult {
    data object Success : PdfUnlockResult
    data class Error(val message: String) : PdfUnlockResult
}
```

## Storage Access Framework Details

Input picker:

```kotlin
ActivityResultContracts.OpenDocument()
```

MIME type:

```kotlin
application/pdf
```

Output picker:

```kotlin
ActivityResultContracts.CreateDocument("application/pdf")
```

Suggested output filename:

```text
<original-name>-unlocked.pdf
```

The app should only process the selected URI after the user has chosen the output URI.

## Offline-Only Verification

Implementation should ensure:

- No `INTERNET` permission in manifest
- No network dependencies
- No analytics or ad dependencies
- No background workers
- No remote config
- No crash reporting SDK

Optional future verification:

- Add a simple Gradle/dependency review checklist before release.
- Add a manifest inspection step in release QA.

## Implementation Phases

### Phase 1: Dependencies And Domain Layer

- Add PDFBox Android dependency.
- Create result model.
- Create PDF unlocker class.
- Implement password-based unlock flow.
- Add unit-testable filename helper.

### Phase 2: File Picker And Save Picker

- Add input PDF picker using Storage Access Framework.
- Add output document picker.
- Resolve selected PDF display name through `ContentResolver`.
- Generate suggested unlocked filename.

### Phase 3: Material 3 UI

- Replace starter greeting screen.
- Build single-screen unlock UI.
- Add password visibility toggle.
- Add disabled/enabled button states.
- Add progress and success/error feedback.

### Phase 4: Error Handling And Polish

- Map PDFBox exceptions to clear UI messages.
- Clear password on successful unlock.
- Prevent duplicate unlock requests while processing.
- Ensure temp files are cleaned up.
- Test with encrypted, unencrypted, wrong-password, and corrupt PDFs.

### Phase 5: Release Readiness

- Confirm no internet permission.
- Confirm no ad, analytics, or telemetry dependencies.
- Test on API 26 and latest target SDK device/emulator.
- Test save behavior with Downloads, Documents, and external providers.
- Validate large PDF behavior.

## Manual Test Checklist

- App launches to unlock screen.
- Selecting cancel from input picker leaves screen stable.
- Selecting a PDF displays its name.
- Empty password prevents unlocking.
- Wrong password shows friendly error.
- Correct password creates an unlocked PDF.
- User can choose output location.
- Canceling save picker does not process the PDF.
- Unencrypted PDF shows a sensible message.
- No password appears in logs.
- App works without network access.

## Non-Goals For Initial Version

- Batch unlocking
- Drag-and-drop
- Recent files list
- Password persistence
- Cloud upload
- In-app PDF preview
- Ads
- Analytics
- Any online service integration
