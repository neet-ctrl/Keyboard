package juloo.keyboard2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class LauncherActivity extends Activity implements Handler.Callback
{
  /** Text is replaced when receiving key events. */
  TextView _tryhere_text;
  EditText _tryhere_area;
  /** Periodically restart the animations. */
  List<Animatable> _animations;
  Handler _handler;

  /** Whether we already showed the accessibility dialog this session. */
  private boolean _accessibilityDialogShown = false;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.launcher_activity);
    _tryhere_text = (TextView)findViewById(R.id.launcher_tryhere_text);
    _tryhere_area = (EditText)findViewById(R.id.launcher_tryhere_area);
    if (VERSION.SDK_INT >= 28)
      _tryhere_area.addOnUnhandledKeyEventListener(
          this.new Tryhere_OnUnhandledKeyEventListener());
    _handler = new Handler(getMainLooper(), this);
    View btnClipboard = findViewById(R.id.btn_clipboard_history);
    if (btnClipboard != null) {
        btnClipboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ClipboardHistoryActivity.class);
            startActivity(intent);
        });
    }
    View btnTypingMaster = findViewById(R.id.btn_typing_master);
    if (btnTypingMaster != null) {
        btnTypingMaster.setOnClickListener(v -> {
            Intent intent = new Intent(this, TypingMasterActivity.class);
            startActivity(intent);
        });
    }

    // Request overlay permission (needed for the floating clipboard widget)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
  }

  @Override
  public void onResume()
  {
    super.onResume();
    // Prompt for accessibility service the first time the user opens the app
    // (or after returning from Accessibility Settings). Only ask once per run.
    if (!_accessibilityDialogShown && !isAccessibilityServiceEnabled()) {
        _accessibilityDialogShown = true;
        showAccessibilityPermissionDialog();
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  //  Accessibility service helpers
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Returns true when GestureAccessibilityService is listed as an enabled
   * accessibility service on this device.
   */
  private boolean isAccessibilityServiceEnabled()
  {
    String expected = getPackageName() + "/"
        + GestureAccessibilityService.class.getName();
    String flat = Settings.Secure.getString(
        getContentResolver(),
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    if (TextUtils.isEmpty(flat)) return false;
    for (String entry : flat.split(":")) {
      if (entry.trim().equalsIgnoreCase(expected)) return true;
    }
    return false;
  }

  /**
   * Shows a one-time dialog explaining the C-gesture feature and guiding the
   * user to Accessibility Settings to enable it.
   */
  private void showAccessibilityPermissionDialog()
  {
    new AlertDialog.Builder(this)
        .setTitle("Enable C-Gesture Clipboard")
        .setMessage(
            "Draw a \u201CC\u201D shape on the RIGHT edge of your screen at any time "
            + "to instantly open the floating clipboard history \u2014 no matter which "
            + "app is open.\n\n"
            + "To activate this, please:\n"
            + "1\ufe0f\u20e3  Tap \u201cGo to Settings\u201d below.\n"
            + "2\ufe0f\u20e3  Find \u201cC-Gesture Clipboard\u201d in the list.\n"
            + "3\ufe0f\u20e3  Toggle it ON and accept the permission.\n\n"
            + "You can skip this and enable it later from Settings \u2192 Accessibility.")
        .setPositiveButton("Go to Settings", (d, w) -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        })
        .setNegativeButton("Skip for now", null)
        .setCancelable(true)
        .show();
  }

  // ─────────────────────────────────────────────────────────────────────────
  //  Standard Activity overrides
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  public void onStart()
  {
    super.onStart();
    _animations = new ArrayList<Animatable>();
    _animations.add(find_anim(R.id.launcher_anim_swipe));
    _animations.add(find_anim(R.id.launcher_anim_round_trip));
    _animations.add(find_anim(R.id.launcher_anim_circle));
    _handler.removeMessages(0);
    _handler.sendEmptyMessageDelayed(0, 500);
  }

  @Override
  public boolean handleMessage(Message _msg)
  {
    for (Animatable anim : _animations)
      anim.start();
    _handler.sendEmptyMessageDelayed(0, 3000);
    return true;
  }

  @Override
  public final boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.launcher_menu, menu);
    return true;
  }

  @Override
  public final boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == R.id.btnLaunchSettingsActivity)
    {
      Intent intent = new Intent(LauncherActivity.this, SettingsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }
    return super.onOptionsItemSelected(item);
  }

  public void launch_imesettings(View _btn)
  {
    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
  }

  public void launch_imepicker(View v)
  {
    InputMethodManager imm =
      (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    imm.showInputMethodPicker();
  }

  Animatable find_anim(int id)
  {
    ImageView img = (ImageView)findViewById(id);
    return (Animatable)img.getDrawable();
  }

  final class Tryhere_OnUnhandledKeyEventListener implements View.OnUnhandledKeyEventListener
  {
    public boolean onUnhandledKeyEvent(View v, KeyEvent ev)
    {
      // Don't handle the back key
      if (ev.getKeyCode() == KeyEvent.KEYCODE_BACK)
        return false;
      // Key release of modifiers would erase interesting data
      if (KeyEvent.isModifierKey(ev.getKeyCode()))
        return false;
      StringBuilder s = new StringBuilder();
      if (ev.isAltPressed()) s.append("Alt+");
      if (ev.isShiftPressed()) s.append("Shift+");
      if (ev.isCtrlPressed()) s.append("Ctrl+");
      if (ev.isMetaPressed()) s.append("Meta+");
      String kc = KeyEvent.keyCodeToString(ev.getKeyCode());
      s.append(kc.replaceFirst("^KEYCODE_", ""));
      _tryhere_text.setText(s.toString());
      return false;
    }
  }
}
