package com.example.majuri_app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AttendanceManagementActivity extends AppCompatActivity {
    public static final String EXTRA_FORCE_USER_FLOW = "extra_force_user_flow";
    private static final int DUTY_ACTION_NONE = 0;
    private static final int DUTY_ACTION_START = 1;
    private static final int DUTY_ACTION_END = 2;

    private TextView tvMonthYear;
    private TextView summaryPresent;
    private TextView summaryAbsent;
    private TextView summaryHalfDay;
    private TextView tvTotalWorkers;
    private Calendar calendar;
    private AttendanceManagementAdapter adapter;
    private SessionManager sessionManager;
    private boolean forceUserFlow;
    private final SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat dutyTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> selfieCaptureLauncher;
    private long pendingDutyWorkerId = -1L;
    private int pendingDutyAction = DUTY_ACTION_NONE;
    private String pendingPhotoPath;
    private FaceVerificationHelper faceVerificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerResultLaunchers();
        setContentView(R.layout.activity_attendance_management);
        faceVerificationHelper = new FaceVerificationHelper(this);

        sessionManager = new SessionManager(this);
        forceUserFlow = getIntent() != null && getIntent().getBooleanExtra(EXTRA_FORCE_USER_FLOW, false);
        calendar = Calendar.getInstance();
        tvMonthYear = findViewById(R.id.tvMonthYear);
        summaryPresent = findViewById(R.id.summaryPresent);
        summaryAbsent = findViewById(R.id.summaryAbsent);
        summaryHalfDay = findViewById(R.id.summaryHalfDay);
        tvTotalWorkers = findViewById(R.id.tvTotalWorkers);

        updateMonthLabel();
        updateWeekDates();
        setupMonthArrows();
        installBackHandler();

        adapter = new AttendanceManagementAdapter();
        adapter.setOnSummaryChangedListener((present, halfDay, absent) -> {
            summaryPresent.setText(String.valueOf(present));
            summaryHalfDay.setText(String.valueOf(halfDay));
            summaryAbsent.setText(String.valueOf(absent));
        });
        adapter.setOnDutyActionListener(new AttendanceManagementAdapter.OnDutyActionListener() {
            @Override
            public void onStartDutyRequested(AttendanceStaffItem item, int adapterPosition) {
                if (item == null || item.getWorkerDbId() <= 0L) {
                    Toast.makeText(AttendanceManagementActivity.this, R.string.duty_start_photo_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                pendingDutyWorkerId = item.getWorkerDbId();
                pendingDutyAction = DUTY_ACTION_START;
                requestCameraAndStartCapture();
            }

            @Override
            public void onEndDutyRequested(AttendanceStaffItem item, int adapterPosition) {
                if (item == null || item.getWorkerDbId() <= 0L) {
                    return;
                }
                pendingDutyWorkerId = item.getWorkerDbId();
                pendingDutyAction = DUTY_ACTION_END;
                requestCameraAndStartCapture();
            }
        });

        RecyclerView recyclerStaff = findViewById(R.id.recyclerStaff);
        recyclerStaff.setLayoutManager(new LinearLayoutManager(this));
        recyclerStaff.setAdapter(adapter);
        recyclerStaff.setNestedScrollingEnabled(false);

        loadAttendanceForSelectedDate();

        findViewById(R.id.btnSearch).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.calendar), Toast.LENGTH_SHORT).show());

        View btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        if (btnSaveAttendance != null) {
            btnSaveAttendance.setOnClickListener(v -> saveAttendanceForSelectedDate());
        }

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttendanceForSelectedDate();
    }

    @Override
    protected void onDestroy() {
        if (faceVerificationHelper != null) {
            faceVerificationHelper.close();
        }
        super.onDestroy();
    }

    private void installBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToHomeByRole();
            }
        });
    }

    private void setupMonthArrows() {
        findViewById(R.id.btnMonthPrev).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthLabel();
            updateWeekDates();
            loadAttendanceForSelectedDate();
        });
        findViewById(R.id.btnMonthNext).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonthLabel();
            updateWeekDates();
            loadAttendanceForSelectedDate();
        });
    }

    private void updateMonthLabel() {
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        String monthUpper = month != null ? month.toUpperCase(Locale.US) : "";
        int year = calendar.get(Calendar.YEAR);
        tvMonthYear.setText(getString(R.string.month_year_format, monthUpper, year));
    }

    private void updateWeekDates() {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDay = Math.max(1, Math.min(day - 3, Math.max(1, maxDays - 6)));

        int[] ids = {R.id.dateMon, R.id.dateTue, R.id.dateWed, R.id.dateThu, R.id.dateFri, R.id.dateSat, R.id.dateSun};
        for (int i = 0; i < ids.length; i++) {
            int dateNumber = startDay + i;
            ((TextView) findViewById(ids[i])).setText(String.valueOf(dateNumber));
        }
    }

    private String getSelectedDateKey() {
        return dateKeyFormat.format(calendar.getTime());
    }

    private void loadAttendanceForSelectedDate() {
        String attendanceDate = getSelectedDateKey();

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        List<WorkerListItem> workers = dbHelper.getAllWorkers();
        Map<Long, Integer> savedStatus = dbHelper.getAttendanceStatusByDate(attendanceDate);
        Set<Long> dutyStartedIds = dbHelper.getDutyStartedWorkerIdsForDate(attendanceDate);
        Set<Long> dutyEndedIds = dbHelper.getDutyEndedWorkerIdsForDate(attendanceDate);
        boolean locked = dbHelper.isAttendanceLockedForDate(attendanceDate);
        dbHelper.close();

        List<AttendanceStaffItem> list = new ArrayList<>();
        for (WorkerListItem worker : workers) {
            int status = savedStatus.containsKey(worker.getId())
                    ? savedStatus.get(worker.getId())
                    : AttendanceStaffItem.STATUS_PRESENT;
            String workerCode = getString(R.string.worker_code_format, Math.max(worker.getId(), 0L));
            list.add(new AttendanceStaffItem(
                    worker.getId(),
                    worker.getName(),
                    worker.getRole(),
                    workerCode,
                    status
            ));
        }

        adapter.setItems(list);
        adapter.setDutyStartedWorkerIds(dutyStartedIds);
        adapter.setDutyEndedWorkerIds(dutyEndedIds);
        adapter.setEditable(!locked);

        if (tvTotalWorkers != null) {
            tvTotalWorkers.setText(getString(R.string.total_workers_with_count, list.size()));
        }

        updateSaveButtonState(locked);
    }

    private void saveAttendanceForSelectedDate() {
        String attendanceDate = getSelectedDateKey();

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        if (dbHelper.isAttendanceLockedForDate(attendanceDate)) {
            dbHelper.close();
            adapter.setEditable(false);
            updateSaveButtonState(true);
            Toast.makeText(this, R.string.attendance_already_locked, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = dbHelper.saveAttendanceForDate(attendanceDate, adapter.getItems());
        dbHelper.close();

        if (saved) {
            adapter.setEditable(false);
            updateSaveButtonState(true);
            NotificationStore.pushNotification(this, "Attendance Saved", "Attendance has been locked for " + attendanceDate + ".");
            Toast.makeText(this, R.string.attendance_saved_locked, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.attendance_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSaveButtonState(boolean locked) {
        View btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        if (btnSaveAttendance != null) {
            btnSaveAttendance.setEnabled(!locked);
            btnSaveAttendance.setAlpha(locked ? 0.6f : 1f);
        }
    }

    private void navigateToHomeByRole() {
        boolean isAdmin = sessionManager != null && sessionManager.isAdmin();
        Class<?> home = forceUserFlow
                ? UserDashboardActivity.class
                : (isAdmin ? DashboardActivity.class : UserDashboardActivity.class);
        Intent target = new Intent(this, home);
        target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(target);
        finish();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        boolean useUserNav = forceUserFlow || (sessionManager != null && !sessionManager.isAdmin());

        bottomNav.getMenu().clear();
        if (useUserNav) {
            bottomNav.inflateMenu(R.menu.menu_user_dashboard_nav);
            bottomNav.setSelectedItemId(R.id.nav_user_home);
            bottomNav.setOnItemSelectedListener(item -> onUserNavItemSelected(item.getItemId()));
        } else {
            bottomNav.inflateMenu(R.menu.menu_dashboard_bottom_nav);
            bottomNav.setSelectedItemId(R.id.nav_attendance);
            bottomNav.setOnItemSelectedListener(item -> onAdminNavItemSelected(item.getItemId()));
        }
    }

    private boolean onUserNavItemSelected(int id) {
        if (id == R.id.nav_user_home) {
            navigateToHomeByRole();
            return true;
        }
        if (id == R.id.nav_user_workers) {
            Intent intent = new Intent(this, WorkersListActivity.class);
            intent.putExtra(WorkersListActivity.EXTRA_FORCE_USER_FLOW, true);
            startActivity(intent);
            finish();
            return true;
        }
        if (id == R.id.nav_user_payslips) {
            Toast.makeText(this, getString(R.string.user_dashboard_payslips), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.nav_user_profile) {
            Toast.makeText(this, getString(R.string.nav_profile), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private boolean onAdminNavItemSelected(int id) {
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_workers) {
            startActivity(new Intent(this, WorkersListActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_attendance) {
            return true;
        }
        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return true;
        }
        return false;
    }

    private void registerResultLaunchers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                    if (!cameraGranted) {
                        Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                        clearPendingDutyCaptureState(false);
                        return;
                    }
                    openCameraForDutyProof();
                }
        );

        selfieCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                        persistDutyProofAfterCapture();
                    } else {
                        Toast.makeText(this, getDutyPhotoRequiredMessage(), Toast.LENGTH_SHORT).show();
                        clearPendingDutyCaptureState(true);
                    }
                }
        );
    }

    private void requestCameraAndStartCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCameraForDutyProof();
            return;
        }
        permissionLauncher.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void openCameraForDutyProof() {
        if (pendingDutyWorkerId <= 0L) return;

        try {
            File imageFile = createDutyProofImageFile(pendingDutyWorkerId);
            pendingPhotoPath = imageFile.getAbsolutePath();
            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, R.string.camera_app_not_found, Toast.LENGTH_SHORT).show();
                clearPendingDutyCaptureState(true);
                return;
            }
            selfieCaptureLauncher.launch(intent);
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show();
            clearPendingDutyCaptureState(true);
        }
    }

    private File createDutyProofImageFile(long workerId) throws IOException {
        File parent = new File(getExternalFilesDir(null), "attendance_proofs");
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create proof folder");
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("duty_" + workerId + "_" + timestamp + "_", ".jpg", parent);
    }

    private void persistDutyProofAfterCapture() {
        if (pendingDutyWorkerId <= 0L || pendingPhotoPath == null || pendingPhotoPath.trim().isEmpty()) {
            Toast.makeText(this, getDutySaveFailedMessage(), Toast.LENGTH_SHORT).show();
            clearPendingDutyCaptureState(true);
            return;
        }
        if (pendingDutyAction != DUTY_ACTION_START && pendingDutyAction != DUTY_ACTION_END) {
            clearPendingDutyCaptureState(true);
            return;
        }

        String attendanceDate = getSelectedDateKey();
        String dutyTime = dutyTimeFormat.format(new Date());
        String location = readLastKnownLocationLabel();

        if (pendingDutyAction == DUTY_ACTION_START) {
            validateFaceAndCompleteDutyStart(attendanceDate, dutyTime, location);
            return;
        }

        if (pendingDutyAction == DUTY_ACTION_END) {
            verifyFaceAndCompleteDutyEnd(attendanceDate, dutyTime, location);
            return;
        }

        clearPendingDutyCaptureState(true);
    }

    private void validateFaceAndCompleteDutyStart(String attendanceDate, String dutyStartTime, String startLocation) {
        long workerId = pendingDutyWorkerId;
        String startImagePath = pendingPhotoPath;

        faceVerificationHelper.validateSingleFace(startImagePath, (valid, message) -> {
            if (!valid) {
                Toast.makeText(AttendanceManagementActivity.this, R.string.selfie_face_not_detected, Toast.LENGTH_SHORT).show();
                clearPendingDutyCaptureState(true);
                return;
            }

            WorkerDbHelper dbHelper = new WorkerDbHelper(AttendanceManagementActivity.this);
            boolean saved = dbHelper.saveDutyStartProof(
                    workerId,
                    attendanceDate,
                    dutyStartTime,
                    startLocation,
                    startImagePath
            );
            dbHelper.close();

            if (saved) {
                adapter.markDutyStarted(workerId, true);
                Toast.makeText(AttendanceManagementActivity.this, R.string.duty_started_with_proof, Toast.LENGTH_SHORT).show();
                clearPendingDutyCaptureState(false);
            } else {
                Toast.makeText(AttendanceManagementActivity.this, R.string.duty_start_photo_save_failed, Toast.LENGTH_SHORT).show();
                clearPendingDutyCaptureState(true);
            }
        });
    }

    private void verifyFaceAndCompleteDutyEnd(String attendanceDate, String dutyEndTime, String endLocation) {
        long workerId = pendingDutyWorkerId;
        String endImagePath = pendingPhotoPath;

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        String startImagePath = dbHelper.getDutyStartImagePath(workerId, attendanceDate);
        dbHelper.close();

        if (startImagePath == null || startImagePath.trim().isEmpty()) {
            Toast.makeText(this, R.string.duty_start_selfie_missing, Toast.LENGTH_SHORT).show();
            clearPendingDutyCaptureState(true);
            return;
        }

        Toast.makeText(this, R.string.face_verification_in_progress, Toast.LENGTH_SHORT).show();
        faceVerificationHelper.verifyFaces(startImagePath, endImagePath, new FaceVerificationHelper.Callback() {
            @Override
            public void onVerified(boolean matched, float similarity, String message) {
                if (!matched) {
                    Toast.makeText(AttendanceManagementActivity.this, R.string.face_mismatch_error, Toast.LENGTH_SHORT).show();
                    clearPendingDutyCaptureState(true);
                    return;
                }

                WorkerDbHelper endDbHelper = new WorkerDbHelper(AttendanceManagementActivity.this);
                boolean saved = endDbHelper.saveDutyEndProof(
                        workerId,
                        attendanceDate,
                        dutyEndTime,
                        endLocation,
                        endImagePath
                );
                endDbHelper.close();

                if (saved) {
                    adapter.markDutyEnded(workerId, true);
                    Toast.makeText(AttendanceManagementActivity.this, R.string.duty_ended_with_proof, Toast.LENGTH_SHORT).show();
                    clearPendingDutyCaptureState(false);
                } else {
                    Toast.makeText(AttendanceManagementActivity.this, R.string.duty_end_photo_save_failed, Toast.LENGTH_SHORT).show();
                    clearPendingDutyCaptureState(true);
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(AttendanceManagementActivity.this, R.string.face_verification_failed, Toast.LENGTH_SHORT).show();
                clearPendingDutyCaptureState(true);
            }
        });
    }

    private String readLastKnownLocationLabel() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fineGranted && !coarseGranted) {
            return getString(R.string.location_permission_denied);
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            return getString(R.string.location_unavailable);
        }

        Location best = null;
        try {
            List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) continue;
                if (best == null || location.getAccuracy() < best.getAccuracy()) {
                    best = location;
                }
            }
        } catch (SecurityException ignored) {
            return getString(R.string.location_permission_denied);
        }

        if (best == null) {
            return getString(R.string.location_unavailable);
        }

        return String.format(Locale.US, "%.6f, %.6f", best.getLatitude(), best.getLongitude());
    }

    private void clearPendingDutyCaptureState(boolean deletePhotoFile) {
        if (deletePhotoFile && pendingPhotoPath != null && !pendingPhotoPath.trim().isEmpty()) {
            File f = new File(pendingPhotoPath);
            if (f.exists()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        pendingDutyWorkerId = -1L;
        pendingDutyAction = DUTY_ACTION_NONE;
        pendingPhotoPath = null;
    }

    private int getDutyPhotoRequiredMessage() {
        return pendingDutyAction == DUTY_ACTION_END
                ? R.string.duty_end_photo_required
                : R.string.duty_start_photo_required;
    }

    private int getDutySaveFailedMessage() {
        return pendingDutyAction == DUTY_ACTION_END
                ? R.string.duty_end_photo_save_failed
                : R.string.duty_start_photo_save_failed;
    }
}
