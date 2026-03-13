package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.preference.Preference;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;

public class SettingsActivity extends PreferenceActivity
{
  private static final int REQUEST_RESTORE_FILE = 1001;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);

    findPreference("learned_words_list").setOnPreferenceClickListener(p -> {
        Suggestions suggestions = new Suggestions(null);
        java.util.List<String> words = suggestions.getDictionary();
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.pref_learned_words_title);
        if (words == null || words.isEmpty()) {
            builder.setMessage("No learned words yet.");
        } else {
            String[] wordsArray = words.toArray(new String[0]);
            builder.setItems(wordsArray, null);
        }
        builder.setPositiveButton("OK", null);
        builder.show();
        return true;
    });

    // ── BACKUP ────────────────────────────────────────────────────────────────
    // Writes JSON to a file and shares the file URI.
    // This avoids TransactionTooLargeException that occurs when passing large
    // strings via Intent.EXTRA_TEXT — supports infinite clip counts.
    findPreference("backup_data").setOnPreferenceClickListener(p -> {
        java.io.File backupFile = BackupRestoreSystem.createBackupFile(this);
        if (backupFile != null) {
            try {
                Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this, "juloo.keyboard2.provider", backupFile);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Keyboard Backup");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.pref_backup_title)));
                Toast.makeText(this, R.string.backup_success, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Could not create backup file.", Toast.LENGTH_SHORT).show();
        }
        return true;
    });

    // ── RESTORE ───────────────────────────────────────────────────────────────
    // Launches the system file picker so the user selects their backup .json
    // file directly — no size limit, works for any number of clips.
    findPreference("restore_data").setOnPreferenceClickListener(p -> {
        Intent pickIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        pickIntent.setType("*/*");
        startActivityForResult(
            Intent.createChooser(pickIntent, getString(R.string.pref_restore_title)),
            REQUEST_RESTORE_FILE);
        return true;
    });

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_RESTORE_FILE && resultCode == RESULT_OK && data != null) {
      Uri uri = data.getData();
      if (uri != null) {
        boolean ok = BackupRestoreSystem.restoreBackupFromUri(this, uri);
        if (ok) {
          Toast.makeText(this, R.string.restore_success, Toast.LENGTH_LONG).show();
          recreate();
        } else {
          Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  void fallbackEncrypted()
  {
    finish();
  }

  protected void onStop()
  {
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
