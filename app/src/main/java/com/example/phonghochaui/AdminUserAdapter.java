package com.example.phonghochaui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonghochaui.data.model.AdminUser;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends BaseAdapter {

    public interface OnLockActionListener {
        void onLockAction(AdminUser user);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final String currentUserId;
    private final OnLockActionListener listener;
    private final List<AdminUser> users = new ArrayList<>();

    public AdminUserAdapter(
            Context context,
            String currentUserId,
            OnLockActionListener listener
    ) {
        this.context = context;
        this.currentUserId =
                currentUserId == null ? "" : currentUserId;
        this.listener = listener;
        inflater = LayoutInflater.from(context);
    }

    public void updateData(List<AdminUser> newData) {
        users.clear();

        if (newData != null) {
            users.addAll(newData);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public AdminUser getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(
                    R.layout.item_admin_user,
                    parent,
                    false
            );

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        bind(holder, getItem(position));

        return convertView;
    }

    private void bind(
            ViewHolder holder,
            AdminUser user
    ) {
        holder.tvName.setText(user.getDisplayName());

        holder.tvRole.setText(
                roleLabel(user.getRole())
        );

        holder.tvCode.setText(context.getString(
                R.string.admin_user_haui_code,
                valueOrUnavailable(user.getHauiCode())
        ));

        holder.tvEmail.setText(context.getString(
                R.string.admin_user_email,
                valueOrUnavailable(user.getEmail())
        ));

        holder.tvCreated.setText(context.getString(
                R.string.admin_user_created_at,
                formatDateTime(user.getCreatedAt())
        ));

        holder.tvLastSignIn.setText(context.getString(
                R.string.admin_user_last_sign_in,
                formatDateTime(user.getLastSignInAt())
        ));

        boolean currentUser =
                currentUserId.equals(user.getId());

        if (user.isLocked()) {
            holder.tvStatus.setText(
                    R.string.admin_user_status_locked
            );

            holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.haui_red
                    )
            );

            holder.btnAction.setText(
                    R.string.unlock_account
            );

            holder.btnAction.setTextColor(Color.WHITE);

            holder.btnAction.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.rgb(24, 133, 81)
                    )
            );
        } else {
            holder.tvStatus.setText(
                    R.string.admin_user_status_active
            );

            holder.tvStatus.setTextColor(
                    Color.rgb(24, 133, 81)
            );

            holder.btnAction.setText(
                    currentUser
                            ? R.string.current_account
                            : R.string.lock_account
            );

            holder.btnAction.setTextColor(Color.WHITE);

            holder.btnAction.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    context,
                                    R.color.haui_red
                            )
                    )
            );
        }

        holder.btnAction.setEnabled(!currentUser);

        holder.btnAction.setAlpha(
                currentUser ? 0.55f : 1f
        );

        holder.btnAction.setOnClickListener(view -> {
            if (!currentUser && listener != null) {
                listener.onLockAction(user);
            }
        });
    }

    private String roleLabel(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            return context.getString(
                    R.string.admin_user_role_admin
            );
        }

        if ("student".equalsIgnoreCase(role)) {
            return context.getString(
                    R.string.admin_user_role_student
            );
        }

        return context.getString(
                R.string.admin_user_role_unknown
        );
    }

    private String valueOrUnavailable(String value) {
        return value == null || value.trim().isEmpty()
                ? context.getString(
                R.string.information_not_available
        )
                : value.trim();
    }

    private String formatDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return context.getString(
                    R.string.information_not_available
            );
        }

        String text = value.trim();

        if (text.length() >= 16
                && text.charAt(4) == '-'
                && text.charAt(7) == '-') {
            String time = text.substring(11, 16);

            return text.substring(8, 10)
                    + "/"
                    + text.substring(5, 7)
                    + "/"
                    + text.substring(0, 4)
                    + " "
                    + time;
        }

        return text;
    }

    private static class ViewHolder {

        final TextView tvName;
        final TextView tvRole;
        final TextView tvCode;
        final TextView tvEmail;
        final TextView tvCreated;
        final TextView tvLastSignIn;
        final TextView tvStatus;
        final MaterialButton btnAction;

        ViewHolder(View view) {
            tvName = view.findViewById(
                    R.id.tvAdminUserName
            );

            tvRole = view.findViewById(
                    R.id.tvAdminUserRole
            );

            tvCode = view.findViewById(
                    R.id.tvAdminUserCode
            );

            tvEmail = view.findViewById(
                    R.id.tvAdminUserEmail
            );

            tvCreated = view.findViewById(
                    R.id.tvAdminUserCreated
            );

            tvLastSignIn = view.findViewById(
                    R.id.tvAdminUserLastSignIn
            );

            tvStatus = view.findViewById(
                    R.id.tvAdminUserStatus
            );

            btnAction = view.findViewById(
                    R.id.btnAdminUserLockAction
            );
        }
    }
}