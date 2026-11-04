package de.anniversarycounter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auswahlliste in {@link CalendarImportActivity}. Die Checkbox zeigt nur den Zustand an und
 * reagiert nicht selbst auf Eingaben - getippt wird auf die ganze Zeile, sodass beim
 * Wiederverwenden einer Zeile kein Listener versehentlich die Auswahl aendert.
 */
class CalendarImportAdapter extends RecyclerView.Adapter<CalendarImportAdapter.ViewHolder> {

    interface OnSelectionChanged {
        void onSelectionChanged();
    }

    private final Context context;
    private final List<CalendarBirthdayReader.Candidate> items;
    private final Set<String> selectedKeys;
    private final OnSelectionChanged listener;
    private final DateTimeFormatter dateFormat;

    CalendarImportAdapter(Context context, List<CalendarBirthdayReader.Candidate> items,
                          Set<String> selectedKeys, OnSelectionChanged listener) {
        this.context = context;
        this.items = items;
        this.selectedKeys = new HashSet<>(selectedKeys);
        this.listener = listener;
        this.dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(context.getResources().getConfiguration().getLocales().get(0));
    }

    List<CalendarBirthdayReader.Candidate> selectedCandidates() {
        List<CalendarBirthdayReader.Candidate> selected = new ArrayList<>();
        for (CalendarBirthdayReader.Candidate candidate : items) {
            if (selectedKeys.contains(candidate.key())) {
                selected.add(candidate);
            }
        }
        return selected;
    }

    ArrayList<String> selectedKeys() {
        return new ArrayList<>(selectedKeys);
    }

    int selectedCount() {
        return selectedKeys.size();
    }

    int selectableCount() {
        int count = 0;
        for (CalendarBirthdayReader.Candidate candidate : items) {
            if (!candidate.alreadyImported) {
                count++;
            }
        }
        return count;
    }

    void setAllSelected(boolean selected) {
        selectedKeys.clear();
        if (selected) {
            for (CalendarBirthdayReader.Candidate candidate : items) {
                if (!candidate.alreadyImported) {
                    selectedKeys.add(candidate.key());
                }
            }
        }
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_import, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final CheckBox cbSelected;
        private final TextView tvTitle;
        private final TextView tvDetails;

        ViewHolder(View itemView) {
            super(itemView);
            cbSelected = itemView.findViewById(R.id.cbSelected);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDetails = itemView.findViewById(R.id.tvEventDetails);
        }

        void bind(CalendarBirthdayReader.Candidate candidate) {
            tvTitle.setText(candidate.title);
            cbSelected.setChecked(selectedKeys.contains(candidate.key()));
            cbSelected.setEnabled(!candidate.alreadyImported);
            itemView.setAlpha(candidate.alreadyImported ? 0.5f : 1f);

            String date = candidate.date.format(dateFormat);
            if (candidate.alreadyImported) {
                tvDetails.setText(context.getString(R.string.calendar_import_already_present, date));
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
                return;
            }
            tvDetails.setText(candidate.calendarName == null || candidate.calendarName.isEmpty()
                    ? date
                    : context.getString(R.string.calendar_import_details, date, candidate.calendarName));
            itemView.setOnClickListener(v -> {
                if (!selectedKeys.remove(candidate.key())) {
                    selectedKeys.add(candidate.key());
                }
                cbSelected.setChecked(selectedKeys.contains(candidate.key()));
                listener.onSelectionChanged();
            });
        }
    }
}
