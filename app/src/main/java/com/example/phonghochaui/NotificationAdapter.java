package com.example.phonghochaui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonghochaui.data.model.NotificationItem;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;

    private final List<NotificationItem> notifications =
            new ArrayList<>();

    public NotificationAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(
            List<NotificationItem> newData
    ) {
        notifications.clear();

        if (newData != null) {
            notifications.addAll(newData);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return notifications.size();
    }

    @Override
    public NotificationItem getItem(int position) {
        return notifications.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
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
                    R.layout.item_notification,
                    parent,
                    false
            );

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        bind(
                holder,
                getItem(position)
        );

        return convertView;
    }

    private void bind(
            ViewHolder holder,
            NotificationItem item
    ) {
        holder.tvTitle.setText(
                item.getTitle()
        );

        holder.tvContent.setText(
                item.getContent()
        );

        holder.tvTime.setText(
                formatCreatedAt(
                        item.getCreatedAt()
                )
        );

        boolean approved =
                "booking_approved".equalsIgnoreCase(
                        item.getNotificationType()
                );

        boolean rejected =
                "booking_rejected".equalsIgnoreCase(
                        item.getNotificationType()
                );

        if (approved) {
            holder.tvType.setText(
                    R.string.notification_type_approved
            );

            holder.tvType.setTextColor(
                    Color.rgb(24, 133, 81)
            );

        } else if (rejected) {
            holder.tvType.setText(
                    R.string.notification_type_rejected
            );

            holder.tvType.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.haui_red
                    )
            );

        } else {
            holder.tvType.setText(
                    R.string.notification_type_general
            );

            holder.tvType.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.haui_blue
                    )
            );
        }

        holder.tvNew.setVisibility(
                item.isRead()
                        ? View.GONE
                        : View.VISIBLE
        );

        holder.card.setCardBackgroundColor(
                item.isRead()
                        ? ContextCompat.getColor(
                        context,
                        R.color.haui_card
                )
                        : Color.rgb(255, 249, 230)
        );

        holder.card.setStrokeColor(
                item.isRead()
                        ? ContextCompat.getColor(
                        context,
                        R.color.haui_outline
                )
                        : ContextCompat.getColor(
                        context,
                        R.color.haui_yellow
                )
        );
    }

    private String formatCreatedAt(String value) {
        if (value == null || value.length() < 19) {
            return value == null ? "" : value;
        }

        try {
            SimpleDateFormat parser =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss",
                            Locale.ROOT
                    );

            parser.setTimeZone(
                    TimeZone.getTimeZone("UTC")
            );

            Date date = parser.parse(
                    value.substring(0, 19)
            );

            if (date == null) {
                return value;
            }

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "dd/MM/yyyy • HH:mm",
                            Locale.getDefault()
                    );

            formatter.setTimeZone(
                    TimeZone.getTimeZone(
                            "Asia/Ho_Chi_Minh"
                    )
            );

            return formatter.format(date);

        } catch (
                ParseException
                | RuntimeException exception
        ) {
            return value;
        }
    }

    private static class ViewHolder {

        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvNew;
        final TextView tvContent;
        final TextView tvType;
        final TextView tvTime;

        ViewHolder(View view) {
            card = view.findViewById(
                    R.id.cardNotification
            );

            tvTitle = view.findViewById(
                    R.id.tvNotificationTitle
            );

            tvNew = view.findViewById(
                    R.id.tvNotificationNew
            );

            tvContent = view.findViewById(
                    R.id.tvNotificationContent
            );

            tvType = view.findViewById(
                    R.id.tvNotificationType
            );

            tvTime = view.findViewById(
                    R.id.tvNotificationTime
            );
        }
    }
}