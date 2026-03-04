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

    findPreference("backup_data").setOnPreferenceClickListener(p -> {
        String backup = BackupRestoreSystem.createBackup(this);
        if (backup != null) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, backup);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.pref_backup_title)));
            Toast.makeText(this, R.string.backup_success, Toast.LENGTH_SHORT).show();
        }
        return true;
    });

    findPreference("restore_data").setOnPreferenceClickListener(p -> {
        // In a real app we'd use a file picker, but to keep it simple and within Replit constraints
        // we'll use a dialog to paste the backup string or just show how it would be done.
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.pref_restore_title);
        final android.widget.EditText input = new android.widget.EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Restore", (dialog, which) -> {
            if (BackupRestoreSystem.restoreBackup(this, input.getText().toString())) {
                Toast.makeText(this, R.string.restore_success, Toast.LENGTH_LONG).show();
                // Refresh activity to reflect changes
                recreate();
            } else {
                Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
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

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
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
