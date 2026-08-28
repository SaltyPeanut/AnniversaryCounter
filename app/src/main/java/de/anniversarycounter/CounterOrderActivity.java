package de.anniversarycounter;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Liste aller Zaehler, deren Reihenfolge sich per Ziehen am Griff-Symbol aendern laesst.
 * Diese Reihenfolge bestimmt die Seitenfolge im ViewPager der {@link MainActivity}, die
 * beim naechsten onResume ohnehin neu aus {@link AnniversaryCounterStore} laedt - hier muss also
 * nur die neue Reihenfolge gespeichert werden.
 */
public class CounterOrderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter_order);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        List<AnniversaryCounter> counters = AnniversaryCounterStore.loadAll(this);
        CounterOrderAdapter adapter = new CounterOrderAdapter(this, counters,
                orderedCounters -> AnniversaryCounterStore.saveAll(this, orderedCounters));

        RecyclerView recyclerView = findViewById(R.id.rvCounterOrder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder viewHolder,
                                   RecyclerView.ViewHolder target) {
                return adapter.onItemMove(
                        viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // ungenutzt, da keine Swipe-Richtung aktiviert ist
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false; // Drag startet ausschliesslich ueber den Griff, siehe ViewHolder
            }

            @Override
            public void clearView(RecyclerView rv, RecyclerView.ViewHolder viewHolder) {
                super.clearView(rv, viewHolder);
                adapter.persistOrder();
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);
        adapter.setTouchHelper(touchHelper);
    }
}
