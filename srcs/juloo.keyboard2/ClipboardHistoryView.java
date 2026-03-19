package juloo.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;

public final class ClipboardHistoryView extends NonScrollListView
  implements ClipboardHistoryService.OnClipboardHistoryChange
{
  List<String> _history;
  ClipboardHistoryService _service;
  ClipboardEntriesAdapter _adapter;

  public ClipboardHistoryView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    _history = Collections.EMPTY_LIST;
    _adapter = this.new ClipboardEntriesAdapter();
    _service = ClipboardHistoryService.get_service(ctx);
    if (_service != null)
    {
      _service.set_on_clipboard_history_change(this);
      _history = _service.clear_expired_and_get_history();
    }
    setAdapter(_adapter);
  }

  public void pin_entry(int pos)
  {
    if (pos >= _history.size()) return;
    ClipboardPinView v = (ClipboardPinView)((ViewGroup)getParent().getParent()).findViewById(R.id.clipboard_pin_view);
    String clip = _history.get(pos);
    v.add_entry(clip);
    _service.remove_history_entry(clip);
    update_data();
  }

  public void paste_entry(int pos)
  {
    if (pos >= _history.size()) return;
    ClipboardHistoryService.paste(_history.get(pos));
  }

  @Override
  public void on_clipboard_history_change() { update_data(); }

  @Override
  protected void onWindowVisibilityChanged(int visibility)
  {
    if (visibility == View.VISIBLE) update_data();
  }

  void update_data()
  {
    if (_service != null) {
        _history = _service.clear_expired_and_get_history();
        _adapter.notifyDataSetChanged();
    }
    invalidate();
  }

  class ClipboardEntriesAdapter extends BaseAdapter
  {
    public ClipboardEntriesAdapter() {}

    @Override
    public int getCount() { return _history.size(); }
    @Override
    public Object getItem(int pos) { return _history.get(pos); }
    @Override
    public long getItemId(int pos) { return _history.get(pos).hashCode(); }

    @Override
    public View getView(final int pos, View v, ViewGroup _parent)
    {
      if (v == null)
        v = View.inflate(getContext(), R.layout.clipboard_history_entry, null);

      // 1. Text Set करना
      TextView tv = v.findViewById(R.id.clipboard_entry_text);
      tv.setText(_history.get(pos));

      // 2. पुराने Buttons को पूरी तरह से हटाना/छुपाना
      View addPinBtn = v.findViewById(R.id.clipboard_entry_addpin);
      View pasteBtn = v.findViewById(R.id.clipboard_entry_paste);
      if (addPinBtn != null) addPinBtn.setVisibility(View.GONE);
      if (pasteBtn != null) pasteBtn.setVisibility(View.GONE);

      // 3. पूरे View पर Click -> Paste करना
      v.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              paste_entry(pos);
          }
      });

      // 4. Swipe Gesture Logic (Left: Delete, Right: Pin)
      v.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
          @Override
          public void onSwipeLeft() {
              // Left Swipe: Delete
              _service.remove_history_entry(_history.get(pos));
              update_data();
          }

          @Override
          public void onSwipeRight() {
              // Right Swipe: Pin
              pin_entry(pos);
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
      public boolean onDown(MotionEvent e) { return false; } // Click pass-through के लिए false

      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
          float diffX = e2.getX() - e1.getX();
          if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
            if (diffX > 0) onSwipeRight();
            else onSwipeLeft();
            return true;
          }
        } catch (Exception exception) { exception.printStackTrace(); }
        return false;
      }
    }

    public void onSwipeRight() {}
    public void onSwipeLeft() {}
  }
}
