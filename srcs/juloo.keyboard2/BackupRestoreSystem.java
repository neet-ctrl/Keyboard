package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import juloo.keyboard2.ClipboardHistoryService.HistoryEntry;

public class BackupRestoreSystem {
    private static final String BACKUP_FILE_NAME = "keyboard_backup.json";

    public static String createBackup(Context context) {
        try {
            org.json.JSONObject backup = new org.json.JSONObject();
            
            // 1. Settings
            SharedPreferences settings = DirectBootAwarePreferences.get_shared_preferences(context);
            org.json.JSONObject settingsJson = new org.json.JSONObject();
            for (Map.Entry<String, ?> entry : settings.getAll().entrySet()) {
                Object value = entry.getValue();
                // Ensure we handle sets correctly for JSON
                if (value instanceof java.util.Set) {
                    org.json.JSONArray setArray = new org.json.JSONArray();
                    for (Object item : (java.util.Set<?>) value) {
                        setArray.put(item);
                    }
                    settingsJson.put(entry.getKey(), setArray);
                } else {
                    settingsJson.put(entry.getKey(), value);
                }
            }
            backup.put("settings", settingsJson);

            // 2. Clipboard History
            ClipboardHistoryService clipboardService = ClipboardHistoryService.get_service(context);
            if (clipboardService != null) {
                org.json.JSONArray clipboardJson = new org.json.JSONArray();
                List<HistoryEntry> history = clipboardService.get_history_entries();
                for (HistoryEntry entry : history) {
                    org.json.JSONObject item = new org.json.JSONObject();
                    item.put("content", entry.content);
                    item.put("timestamp", entry.timestamp);
                    item.put("description", entry.description);
                    item.put("version", entry.version);
                    clipboardJson.put(item);
                }
                backup.put("clipboard", clipboardJson);
            }

            // 3. Learned Words
            java.io.File learnedFile = new java.io.File(context.getFilesDir(), "user_dictionary.txt");
            if (learnedFile.exists()) {
                org.json.JSONArray learnedJson = new org.json.JSONArray();
                java.util.Scanner s = new java.util.Scanner(learnedFile);
                while (s.hasNextLine()) {
                    learnedJson.put(s.nextLine());
                }
                s.close();
                backup.put("learned_words", learnedJson);
            }

            return backup.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean restoreBackup(Context context, String backupData) {
        try {
            org.json.JSONObject backup = new org.json.JSONObject(backupData);

            // 1. Restore Settings
            if (backup.has("settings")) {
                org.json.JSONObject settingsJson = backup.getJSONObject("settings");
                SharedPreferences.Editor editor = DirectBootAwarePreferences.get_shared_preferences(context).edit();
                java.util.Iterator<String> keys = settingsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = settingsJson.get(key);
                    if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                    else if (value instanceof Integer) editor.putInt(key, (Integer) value);
                    else if (value instanceof Long) editor.putLong(key, (Long) value);
                    else if (value instanceof Float) editor.putFloat(key, (Float) value);
                    else if (value instanceof String) editor.putString(key, (String) value);
                    else if (value instanceof org.json.JSONArray) {
                        org.json.JSONArray array = (org.json.JSONArray) value;
                        java.util.Set<String> set = new java.util.HashSet<>();
                        for (int i = 0; i < array.length(); i++) {
                            set.add(array.getString(i));
                        }
                        editor.putStringSet(key, set);
                    }
                }
                editor.apply();
            }

            // 2. Restore Clipboard (Append mode as requested)
            if (backup.has("clipboard")) {
                org.json.JSONArray clipboardJson = backup.getJSONArray("clipboard");
                ClipboardHistoryService clipboardService = ClipboardHistoryService.get_service(context);
                if (clipboardService != null) {
                    for (int i = 0; i < clipboardJson.length(); i++) {
                        org.json.JSONObject item = clipboardJson.getJSONObject(i);
                        clipboardService.add_clip_with_metadata(
                            item.getString("content"),
                            item.optString("description", ""),
                            item.optString("version", "")
                        );
                    }
                }
            }

            // 3. Restore Learned Words
            if (backup.has("learned_words")) {
                org.json.JSONArray learnedJson = backup.getJSONArray("learned_words");
                java.io.File learnedFile = new java.io.File(context.getFilesDir(), "user_dictionary.txt");
                java.io.FileWriter writer = new java.io.FileWriter(learnedFile, true); // Append mode
                for (int i = 0; i < learnedJson.length(); i++) {
                    writer.write(learnedJson.getString(i) + "\n");
                }
                writer.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
