package de.anniversarycounter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Treibt die Liste in {@link CounterOrderActivity}. Der Griff ({@link ViewHolder#ivHandle})
 * startet ueber den uebergebenen {@link ItemTouchHelper} den Drag; die eigentliche
 * Umsortierung passiert schon waehrend des Ziehens in {@link #onItemMove}, damit die
 * Liste sichtbar mitwandert, statt erst beim Loslassen zu springen.
 */
public class CounterOrderAdapter extends RecyclerView.Adapter<CounterOrderAdapter.ViewHolder> {

    interface OnOrderChanged {
        void onOrderChanged(List<AnniversaryCounter> counters);
    }

    private final Context context;
    private final List<AnniversaryCounter> counters;
    private final OnOrderChanged listener;
    private ItemTouchHelper touchHelper;

    CounterOrderAdapter(Context context, List<AnniversaryCounter> counters, OnOrderChanged listener) {
        this.context = context;
        this.counters = counters;
        this.listener = listener;
    }

    void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(counters, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    /** Wird aufgerufen, sobald ein Drag beendet ist - persistiert die dann gueltige Reihenfolge. */
    void persistOrder() {
        listener.onOrderChanged(counters);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_counter_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(counters.get(position));
    }

    @Override
    public int getItemCount() {
        return counters.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvLabel;
        private final View ivHandle;

        ViewHolder(View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tvCounterLabel);
            ivHandle = itemView.findViewById(R.id.ivDragHandle);

            ivHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && touchHelper != null) {
                    touchHelper.startDrag(this);
                }
                return false;
            });
        }

        void bind(AnniversaryCounter counter) {
            tvLabel.setText(counter.displayLabel(context));
        }
    }
}
