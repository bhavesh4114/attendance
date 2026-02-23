package com.example.majuri_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class DailyAttendanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_attendance);

        RecyclerView recycler = findViewById(R.id.recyclerAttendance);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setNestedScrollingEnabled(false);

        AttendanceAdapter adapter = new AttendanceAdapter();
        adapter.setItems(getDummyCards());
        recycler.setAdapter(adapter);
    }

    private List<AttendanceCard> getDummyCards() {
        return Arrays.asList(
                new AttendanceCard("Rajesh Kumar", "Head Mason", 0, AttendanceCard.Status.PRESENT),
                new AttendanceCard("Amit Singh", "Mason", 2, AttendanceCard.Status.HALF_DAY),
                new AttendanceCard("Vikram Patel", "Labour", 0, AttendanceCard.Status.ABSENT),
                new AttendanceCard("Pooja Devi", "Helper", 1, AttendanceCard.Status.PRESENT)
        );
    }
}
