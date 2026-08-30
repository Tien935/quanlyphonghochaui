package com.example.phonghochaui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonghochaui.data.model.ScheduleItem;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class AdminScheduleAdapter extends BaseAdapter {

    public interface ScheduleActionListener {
        void onEdit(ScheduleItem schedule);

        void onDelete(ScheduleItem schedule);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final ScheduleActionListener listener;
    private final List<ScheduleItem> schedules = new ArrayList<>();

    public AdminScheduleAdapter(
            Context context,
            ScheduleActionListener listener
    ) {
        this.context = context;
        this.listener = listener;
        inflater = LayoutInflater.from(context);
    }

    public void updateData(List<ScheduleItem> newData) {
        schedules.clear();
        if (newData != null) {
            schedules.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return schedules.size();
    }

    @Override
    public ScheduleItem getItem(int position) {
        return schedules.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(
                    R.layout.item_admin_schedule,
                    parent,
                    false
            );
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ScheduleItem schedule = getItem(position);
        bind(holder, schedule, position);
        return convertView;
    }

    private void bind(
            ViewHolder holder,
            ScheduleItem schedule,
            int position
    ) {
        boolean showDateHeader = position == 0
                || !schedule.getStudyDate().equals(
                getItem(position - 1).getStudyDate()
        );
        holder.dateHeader.setVisibility(
                showDateHeader ? View.VISIBLE : View.GONE
        );
        holder.dateHeader.setText(formatDateHeader(schedule.getStudyDate()));

        holder.time.setText(context.getString(
                R.string.schedule_time_multiline,
                shortTime(schedule.getStartTime()),
                shortTime(schedule.getEndTime())
        ));

        String subject = schedule.getSubjectName();
        holder.subject.setText(subject.isEmpty()
                ? context.getString(R.string.schedule_subject_unknown)
                : subject);

        String subjectCode = schedule.getSubjectCode();
        holder.subjectCode.setText(subjectCode);
        holder.subjectCode.setVisibility(
                subjectCode.isEmpty() ? View.GONE : View.VISIBLE
        );

        holder.location.setText(buildLocation(schedule));
        holder.lecturer.setText(buildLecturer(schedule));

        TypeStyle typeStyle = typeStyle(schedule.getScheduleType());
        holder.type.setText(typeStyle.label);
        holder.type.setTextColor(typeStyle.foreground);
        holder.typeCard.setCardBackgroundColor(typeStyle.background);
        holder.time.setTextColor(typeStyle.foreground);
        holder.timeCard.setCardBackgroundColor(typeStyle.background);

        holder.card.setOnClickListener(view -> listener.onEdit(schedule));
        holder.more.setOnClickListener(view -> showActions(view, schedule));
    }

    private void showActions(View anchor, ScheduleItem schedule) {
        PopupMenu menu = new PopupMenu(context, anchor);
        menu.getMenu().add(context.getString(R.string.schedule_edit_action));
        menu.getMenu().add(context.getString(R.string.schedule_delete_action));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals(
                    context.getString(R.string.schedule_edit_action)
            )) {
                listener.onEdit(schedule);
            } else {
                listener.onDelete(schedule);
            }
            return true;
        });
        menu.show();
    }

    private String buildLocation(ScheduleItem schedule) {
        List<String> parts = new ArrayList<>();
        if (!schedule.getRoomCode().isEmpty()) {
            parts.add(context.getString(
                    R.string.schedule_room_short,
                    schedule.getRoomCode()
            ));
        }
        if (!schedule.getBuildingCode().isEmpty()) {
            parts.add(context.getString(
                    R.string.schedule_building_short,
                    schedule.getBuildingCode()
            ));
        }
        if (!schedule.getCampusCode().isEmpty()) {
            parts.add(schedule.getCampusCode());
        }

        if (parts.isEmpty()) {
            return context.getString(R.string.information_not_available);
        }
        return join(parts, " • ");
    }

    private String buildLecturer(ScheduleItem schedule) {
        String name = schedule.getLecturerName();
        String code = schedule.getLecturerHauiCode();

        if (name.isEmpty() && code.isEmpty()) {
            return context.getString(R.string.schedule_no_lecturer);
        }
        if (name.isEmpty()) {
            return context.getString(R.string.schedule_lecturer_format, code);
        }
        if (code.isEmpty()) {
            return context.getString(R.string.schedule_lecturer_format, name);
        }
        return context.getString(
                R.string.schedule_lecturer_code_format,
                name,
                code
        );
    }

    private TypeStyle typeStyle(String type) {
        if ("self_study".equalsIgnoreCase(type)) {
            return new TypeStyle(
                    context.getString(R.string.schedule_type_self_study),
                    ContextCompat.getColor(context, R.color.schedule_self_study),
                    ContextCompat.getColor(context, R.color.schedule_self_study_surface)
            );
        }
        if ("other".equalsIgnoreCase(type)) {
            return new TypeStyle(
                    context.getString(R.string.schedule_type_other),
                    ContextCompat.getColor(context, R.color.schedule_other),
                    ContextCompat.getColor(context, R.color.schedule_other_surface)
            );
        }
        return new TypeStyle(
                context.getString(R.string.schedule_type_study),
                ContextCompat.getColor(context, R.color.schedule_study),
                ContextCompat.getColor(context, R.color.schedule_study_surface)
        );
    }

    private String formatDateHeader(String isoDate) {
        if (isoDate.isEmpty()) {
            return context.getString(R.string.information_not_available);
        }

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        parser.setLenient(false);
        try {
            Date date = parser.parse(isoDate);
            if (date == null) {
                return isoDate;
            }
            SimpleDateFormat output = new SimpleDateFormat(
                    "EEEE, dd/MM/yyyy",
                    new Locale("vi", "VN")
            );
            String value = output.format(date);
            if (value.isEmpty()) {
                return isoDate;
            }
            return value.substring(0, 1).toUpperCase(
                    new Locale("vi", "VN")
            ) + value.substring(1);
        } catch (ParseException exception) {
            return isoDate;
        }
    }

    private String shortTime(String value) {
        return value.length() >= 5 ? value.substring(0, 5) : value;
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static class TypeStyle {
        final String label;
        final int foreground;
        final int background;

        TypeStyle(String label, int foreground, int background) {
            this.label = label;
            this.foreground = foreground;
            this.background = background;
        }
    }

    private static class ViewHolder {
        final TextView dateHeader;
        final MaterialCardView card;
        final MaterialCardView timeCard;
        final TextView time;
        final TextView subject;
        final TextView subjectCode;
        final TextView location;
        final TextView lecturer;
        final MaterialCardView typeCard;
        final TextView type;
        final View more;

        ViewHolder(View view) {
            dateHeader = view.findViewById(R.id.tvScheduleDateHeader);
            card = view.findViewById(R.id.cardScheduleItem);
            timeCard = view.findViewById(R.id.cardScheduleTime);
            time = view.findViewById(R.id.tvScheduleTime);
            subject = view.findViewById(R.id.tvScheduleSubject);
            subjectCode = view.findViewById(R.id.tvScheduleSubjectCode);
            location = view.findViewById(R.id.tvScheduleLocation);
            lecturer = view.findViewById(R.id.tvScheduleLecturer);
            typeCard = view.findViewById(R.id.cardScheduleType);
            type = view.findViewById(R.id.tvScheduleType);
            more = view.findViewById(R.id.btnScheduleMore);
        }
    }
}

