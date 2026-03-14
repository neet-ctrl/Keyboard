package juloo.keyboard2;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.widget.FloatingWidgetService;
import juloo.keyboard2.widget.OverlayPermissionHelper;

/**
 * Accessibility service that places a transparent 40dp strip on the right edge
 * of the screen. When the user draws a "C" gesture starting from that strip
 * (finger goes RIGHT → curves LEFT → comes back to RIGHT), the floating
 * clipboard history widget is opened — exactly as if the widget button was tapped.
 *
 * The strip only intercepts touches that begin inside it; all other touches
 * reach the underlying app normally.
 */
public class GestureAccessibilityService extends AccessibilityService {

    // Width of the transparent gesture trigger strip on the right edge (dp)
    private static final int STRIP_WIDTH_DP = 40;

    // Minimum leftward extent (px) the finger must travel to count as a C
    private static final float MIN_LEFT_EXTENT_DP = 80;

    // Maximum dy between start and end points — keeps C from being too diagonal
    private static final float MAX_Y_DIFF_DP = 180;

    // Minimum number of tracked points before we analyse the path
    private static final int MIN_POINTS = 8;

    private WindowManager   _wm;
    private View            _strip;
    private List<float[]>   _points = new ArrayList<>(); // x,y pairs
    private float           _density;

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        _density = getResources().getDisplayMetrics().density;
        createEdgeStrip();
        Toast.makeText(this, "C-Gesture clipboard activated ✓", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { /* not used */ }

    @Override
    public void onInterrupt() { /* not used */ }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeEdgeStrip();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Overlay strip
    // ─────────────────────────────────────────────────────────────────────────

    private void createEdgeStrip() {
        _wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        int stripPx = (int) (STRIP_WIDTH_DP * _density);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            stripPx,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSPARENT);

        params.gravity = Gravity.END | Gravity.TOP; // right edge

        _strip = new View(this);
        _strip.setBackgroundColor(0x00000000); // fully transparent
        _strip.setOnTouchListener(this::onStripTouch);

        _wm.addView(_strip, params);
    }

    private void removeEdgeStrip() {
        if (_wm != null && _strip != null) {
            try { _wm.removeView(_strip); } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Touch handling & C-shape detection
    // ─────────────────────────────────────────────────────────────────────────

    private boolean onStripTouch(View v, MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                _points.clear();
                _points.add(new float[]{ev.getRawX(), ev.getRawY()});
                return true; // claim the touch — all MOVE/UP events follow

            case MotionEvent.ACTION_MOVE:
                // Sample every point (getHistorySize helps on fast swipes)
                for (int h = 0; h < ev.getHistorySize(); h++) {
                    _points.add(new float[]{ev.getHistoricalRawX(h), ev.getHistoricalRawY(h)});
                }
                _points.add(new float[]{ev.getRawX(), ev.getRawY()});
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                _points.add(new float[]{ev.getRawX(), ev.getRawY()});
                if (isCGesture(_points)) {
                    openFloatingClipboard();
                }
                _points.clear();
                return true;
        }
        return false;
    }

    /**
     * Detect a rightward-opening "C" (or reverse-C) shape:
     *   - Starts and ends near the right edge (large raw X)
     *   - Reaches a leftward extreme in the middle of the path
     *   - Total leftward excursion > MIN_LEFT_EXTENT_DP
     *   - Vertical displacement between start and end < MAX_Y_DIFF_DP
     */
    private boolean isCGesture(List<float[]> pts) {
        if (pts.size() < MIN_POINTS) return false;

        float minExtentPx = MIN_LEFT_EXTENT_DP * _density;
        float maxYDiffPx  = MAX_Y_DIFF_DP * _density;

        float[] start = pts.get(0);
        float[] end   = pts.get(pts.size() - 1);

        // 1. Start y and end y should not be too far apart (avoid diagonal swipes)
        if (Math.abs(start[1] - end[1]) > maxYDiffPx) return false;

        // 2. Find leftmost x across the entire path
        float minX = start[0];
        int   minXIdx = 0;
        for (int i = 1; i < pts.size(); i++) {
            if (pts.get(i)[0] < minX) { minX = pts.get(i)[0]; minXIdx = i; }
        }

        // 3. The path must extend significantly to the left of the start point
        float leftExtent = start[0] - minX;
        if (leftExtent < minExtentPx) return false;

        // 4. The leftmost point must be in the middle 20%–80% of the path
        //    (not at the very start or end — that would be a plain swipe)
        float progress = (float) minXIdx / pts.size();
        if (progress < 0.15f || progress > 0.85f) return false;

        // 5. Both start and end must have returned close to the right edge
        //    (end X within 60% of the leftward excursion back towards start)
        float returnThreshold = start[0] - leftExtent * 0.5f;
        if (end[0] < returnThreshold) return false;

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Open the floating clipboard
    // ─────────────────────────────────────────────────────────────────────────

    private void openFloatingClipboard() {
        try {
            OverlayPermissionHelper.requestOverlayPermission(this);
        } catch (Exception e) {
            // Fallback: start service directly
            Intent intent = new Intent(this, FloatingWidgetService.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(intent);
        }
    }
}
