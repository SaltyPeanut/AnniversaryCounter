package de.anniversarycounter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Treibt das ViewPager2 mit einer Seite pro {@link AnniversaryCounter}.
 *
 * Der Sekundentakt aktualisiert nur aktuell angehaengte ViewHolder direkt
 * (siehe {@link #tick()}), statt ueber ein volles Rebind zu gehen, damit
 * das Wischen zwischen Seiten nicht flackert.
 */
public class CounterPagerAdapter extends RecyclerView.Adapter<CounterPagerAdapter.ViewHolder> {

    private final Context context;
    private final List<AnniversaryCounter> counters = new ArrayList<>();
    private final Map<String, Boolean> showTotalsById = new HashMap<>();
    private final Set<ViewHolder> attached = new HashSet<>();

    public CounterPagerAdapter(Context context) {
        this.context = context;
    }

    public void setCounters(List<AnniversaryCounter> newCounters) {
        counters.clear();
        counters.addAll(newCounters);
        notifyDataSetChanged();
    }

    public AnniversaryCounter getCounter(int position) {
        if (position < 0 || position >= counters.size()) {
            return null;
        }
        return counters.get(position);
    }

    /** Wird jede Sekunde vom Ticker in MainActivity aufgerufen. */
    public void tick() {
        for (ViewHolder holder : attached) {
            holder.refresh();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_counter_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(counters.get(position));
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        attached.add(holder);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        attached.remove(holder);
    }

    @Override
    public int getItemCount() {
        return counters.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvSubtitle;
        private final TextView tvCounter;
        private final TextView tvStart;
        private AnniversaryCounter counter;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvCounter = itemView.findViewById(R.id.tvCounter);
            tvStart = itemView.findViewById(R.id.tvStart);

            tvCounter.setOnClickListener(v -> {
                if (counter == null) {
                    return;
                }
                boolean current = Boolean.TRUE.equals(showTotalsById.get(counter.id));
                showTotalsById.put(counter.id, !current);
                refresh();
            });

            tvCounter.setOnLongClickListener(v -> {
                share();
                return true;
            });
        }

        void bind(AnniversaryCounter counter) {
            this.counter = counter;
            refresh();
        }

        void refresh() {
            if (counter == null) {
                return;
            }

            Typeface textTypeface = AppSettings.typeface(context, counter, AppSettings.Target.TEXT);
            int textColor = AppSettings.textColor(context, counter, AppSettings.Target.TEXT);
            tvName.setTypeface(textTypeface);
            tvName.setTextColor(textColor);
            tvSubtitle.setTypeface(textTypeface);
            tvSubtitle.setTextColor(textColor);
            tvStart.setTypeface(textTypeface);

            tvCounter.setTypeface(AppSettings.typeface(context, counter, AppSettings.Target.NUMBERS));
            tvCounter.setTextColor(AppSettings.textColor(context, counter, AppSettings.Target.NUMBERS));

            if (counter.showName && counter.name != null && !counter.name.isEmpty()) {
                tvName.setVisibility(View.VISIBLE);
                tvName.setText(counter.name);
            } else {
                tvName.setVisibility(View.GONE);
            }

            DateDiff diff = DateDiff.between(counter.start, LocalDateTime.now());
            boolean showTotals = Boolean.TRUE.equals(showTotalsById.get(counter.id));

            if (counter.showMessage) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(AnniversaryCounterStore.subtitleText(context, counter, diff.future));
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }
            tvCounter.setText(showTotals ? diff.formatTotals(context) : diff.formatBreakdown(context));
            tvStart.setText(context.getString(
                    R.string.start_prefix, counter.start.format(AnniversaryCounter.DISPLAY_FORMAT)));
        }

        private void share() {
            if (counter == null) {
                return;
            }
            DateDiff diff = DateDiff.between(counter.start, LocalDateTime.now());
            String text = AnniversaryCounterStore.subtitleText(context, counter, diff.future)
                    + "\n" + diff.formatBreakdown(context).replace("\n", " ");

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)));
        }
    }
}
