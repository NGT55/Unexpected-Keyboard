package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public final class ClipboardPinView extends NonScrollListView
{
  static final String PERSIST_FILE_NAME = "clipboards";
  static final String PERSIST_PREF = "pinned";

  List<String> _entries;
  ClipboardPinEntriesAdapter _adapter;
  SharedPreferences _persist_store;

  public ClipboardPinView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    _entries = new ArrayList<String>();
    _persist_store = null;
    try
    {
      _persist_store = ctx.getSharedPreferences("pinned_clipboards", Context.MODE_PRIVATE);
      load_from_prefs(_persist_store, _entries);
    }
    catch (Exception _e) {}
    _adapter = this.new ClipboardPinEntriesAdapter();
    setAdapter(_adapter);
  }

  public void add_entry(String text)
  {
    _entries.add(text);
    _adapter.notifyDataSetChanged();
    persist();
    invalidate();
  }

  public void remove_entry(int pos)
  {
    if (pos < 0 || pos >= _entries.size()) return;
    _entries.remove(pos);
    _adapter.notifyDataSetChanged();
    persist();
    invalidate();
  }

  public void paste_entry(int pos)
  {
    if (pos >= _entries.size()) return;
    ClipboardHistoryService.paste(_entries.get(pos));
  }

  static void load_from_prefs(SharedPreferences store, List<String> dst)
  {
    String arr_s = store.getString(PERSIST_PREF, null);
    if (arr_s == null) return;
    try
    {
      JSONArray arr = new JSONArray(arr_s);
      for (int i = 0; i < arr.length(); i++)
        dst.add(arr.getString(i));
    }
    catch (JSONException _e) {}
  }

  void persist()
  {
    if (_persist_store == null) return;
    JSONArray arr = new JSONArray();
    for (int i = 0; i < _entries.size(); i++)
      arr.put(_entries.get(i));
    _persist_store.edit().putString(PERSIST_PREF, arr.toString()).apply();
  }

  class ClipboardPinEntriesAdapter extends BaseAdapter
  {
    public ClipboardPinEntriesAdapter() {}

    @Override
    public int getCount() { return _entries.size(); }
    @Override
    public Object getItem(int pos) { return _entries.get(pos); }
    @Override
    public long getItemId(int pos) { return _entries.get(pos).hashCode(); }

    @Override
    public View getView(final int pos, View v, ViewGroup _parent)
    {
      if (v == null)
        v = View.inflate(getContext(), R.layout.clipboard_pin_entry, null);

      // 1. Text Set करना
      TextView tv = v.findViewById(R.id.clipboard_pin_text);
      tv.setText(_entries.get(pos));

      // 2. Buttons को छुपाना (Paste and Remove icons)
      View pasteBtn = v.findViewById(R.id.clipboard_pin_paste);
      View removeBtn = v.findViewById(R.id.clipboard_pin_remove);
      if (pasteBtn != null) pasteBtn.setVisibility(View.GONE);
      if (removeBtn != null) removeBtn.setVisibility(View.GONE);

      // 3. Click to Paste (पूरे item पर)
      v.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              paste_entry(pos);
          }
      });

      // 4. Swipe to Delete (Left Swipe Only)
      v.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
          @Override
          public void onSwipeLeft() {
              remove_entry(pos);
          }
      });

      return v;
    }
  }

  // --- Swipe Helper Class ---
  class OnSwipeTouchListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    public OnSwipeTouchListener(Context ctx) {
      gestureDetector = new GestureDetector(ctx, new GestureListener());
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      return gestureDetector.onTouchEvent(event);
    }
    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
      private static final int SWIPE_THRESHOLD = 100;
      private static final int SWIPE_VELOCITY_THRESHOLD = 100;
      @Override
      public boolean onDown(MotionEvent e) { return false; }
      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float diffX = e2.getX() - e1.getX();
        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
          if (diffX < 0) onSwipeLeft(); // Left Swipe logic
          return true;
        }
        return false;
      }
    }
    public void onSwipeLeft() {}
  }
}
