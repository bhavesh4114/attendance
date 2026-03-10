package com.example.majuri_app;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shared password-change dialog flow for settings screens.
 */
public final class ChangePasswordHelper {

    private ChangePasswordHelper() {
    }

    public static void showChangePasswordDialog(AppCompatActivity activity) {
        if (activity == null || activity.isFinishing()) return;

        SessionManager sessionManager = new SessionManager(activity);
        String loginId = sessionManager.getLoggedInMobile();
        if (loginId == null || loginId.trim().isEmpty()) {
            Toast.makeText(activity, R.string.password_change_session_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        EditText etCurrentPassword = createPasswordField(activity, activity.getString(R.string.current_password));
        EditText etNewPassword = createPasswordField(activity, activity.getString(R.string.new_password));
        EditText etConfirmPassword = createPasswordField(activity, activity.getString(R.string.confirm_new_password));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(activity, 20);
        root.setPadding(p, p / 2, p, 0);
        root.addView(etCurrentPassword);
        root.addView(etNewPassword);
        root.addView(etConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.change_password)
                .setView(root)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.update_password, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String currentPassword = safe(etCurrentPassword);
                String newPassword = safe(etNewPassword);
                String confirmPassword = safe(etConfirmPassword);

                if (currentPassword.isEmpty()) {
                    etCurrentPassword.setError(activity.getString(R.string.error_enter_current_password));
                    etCurrentPassword.requestFocus();
                    return;
                }
                if (newPassword.isEmpty()) {
                    etNewPassword.setError(activity.getString(R.string.error_enter_new_password));
                    etNewPassword.requestFocus();
                    return;
                }
                if (newPassword.length() < 6) {
                    etNewPassword.setError(activity.getString(R.string.error_password_min_length));
                    etNewPassword.requestFocus();
                    return;
                }
                if (currentPassword.equals(newPassword)) {
                    etNewPassword.setError(activity.getString(R.string.error_new_password_same_as_current));
                    etNewPassword.requestFocus();
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    etConfirmPassword.setError(activity.getString(R.string.error_password_mismatch));
                    etConfirmPassword.requestFocus();
                    return;
                }

                BuilderDbHelper builderDbHelper = new BuilderDbHelper(activity);
                boolean currentValid = builderDbHelper.isBuilderPasswordValid(loginId, currentPassword);
                if (!currentValid) {
                    builderDbHelper.close();
                    etCurrentPassword.setError(activity.getString(R.string.error_current_password_incorrect));
                    etCurrentPassword.requestFocus();
                    return;
                }

                boolean updated = builderDbHelper.updateBuilderPassword(loginId, newPassword);
                builderDbHelper.close();

                if (updated) {
                    Toast.makeText(activity, R.string.password_updated_success, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(activity, R.string.password_updated_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private static EditText createPasswordField(Context context, String hint) {
        EditText field = new EditText(context);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        int marginBottom = dp(context, 12);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) field.getLayoutParams();
        params.bottomMargin = marginBottom;
        field.setLayoutParams(params);
        return field;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String safe(EditText et) {
        return et != null && et.getText() != null ? et.getText().toString() : "";
    }
}
