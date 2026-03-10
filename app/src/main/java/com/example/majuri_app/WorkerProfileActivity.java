package com.example.majuri_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WorkerProfileActivity extends AppCompatActivity {

    public static final String EXTRA_WORKER_ID = "extra_worker_id";

    private View loadingView;
    private View contentView;
    private View errorView;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_profile);

        loadingView = findViewById(R.id.loadingView);
        contentView = findViewById(R.id.contentView);
        errorView = findViewById(R.id.errorView);
        tvError = findViewById(R.id.tvError);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        long workerId = getIntent().getLongExtra(EXTRA_WORKER_ID, -1L);
        if (workerId <= 0L) {
            showError(getString(R.string.worker_not_found));
            return;
        }

        loadWorkerProfile(workerId);
    }

    private void loadWorkerProfile(long workerId) {
        showLoading();
        try {
            WorkerDbHelper dbHelper = new WorkerDbHelper(this);
            WorkerProfile worker = dbHelper.getWorkerProfileById(workerId);
            dbHelper.close();

            if (worker == null) {
                showError(getString(R.string.worker_not_found));
                return;
            }
            bindWorker(worker);
            showContent();
        } catch (Exception e) {
            showError(getString(R.string.error_loading_worker_profile));
            Toast.makeText(this, getString(R.string.error_loading_worker_profile), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindWorker(WorkerProfile worker) {
        setText(R.id.tvWorkerName, worker.getFullName());
        setText(R.id.tvWorkerRole, worker.getRole());
        setText(R.id.tvWorkerPhone, worker.getPhone());
        setText(R.id.tvWorkerStatus, worker.isActive() ? getString(R.string.active) : getString(R.string.inactive));
        setText(R.id.tvWorkerJoinDate, emptyAsDash(worker.getJoinDate()));
        setText(R.id.tvWorkerAddress, emptyAsDash(worker.getAddress()));
        setText(R.id.tvWorkerDocuments, emptyAsDash(worker.getDocuments()));
    }

    private void setText(int viewId, String value) {
        TextView tv = findViewById(viewId);
        if (tv != null) {
            tv.setText(value != null ? value : "-");
        }
    }

    private String emptyAsDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private void showLoading() {
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (contentView != null) contentView.setVisibility(View.GONE);
        if (errorView != null) errorView.setVisibility(View.GONE);
    }

    private void showContent() {
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (contentView != null) contentView.setVisibility(View.VISIBLE);
        if (errorView != null) errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (contentView != null) contentView.setVisibility(View.GONE);
        if (errorView != null) errorView.setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setText(message);
    }
}
