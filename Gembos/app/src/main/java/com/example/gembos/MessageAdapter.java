package com.example.gembos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private OnMessageClickListener listener;

    public MessageAdapter(List<Message> messages, OnMessageClickListener listener) {
        this.messageList = messages;
        this.listener = listener;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textSender, textMessage, textDate;
        ImageView iconEncrypted;

        public MessageViewHolder(View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.textContact);
            textMessage = itemView.findViewById(R.id.textMessagePreview);
            textDate = itemView.findViewById(R.id.textTime);
            iconEncrypted = itemView.findViewById(R.id.iconEncrypted);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(messageList.get(position).getSender());
                }
            });
        }
    }

    public interface OnMessageClickListener {
        void onMessageClick(String phoneNumber);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messageList.get(position);
        holder.textSender.setText(msg.getSender());
        holder.textMessage.setText(msg.getBody());

        if (msg.getBody().startsWith("[GEMBOS]")) {
            // Encrypted
            holder.iconEncrypted.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)); // Normal bg
        } else {
            // Unencrypted
            holder.iconEncrypted.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_red)); // Your red tone
            // Set sender text color to a color that contrasts with the red background
            holder.textSender.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black)); // Set sender text color to white
        }

        // Format date from timestamp string
        try {
            long millis = Long.parseLong(msg.getDate());
            String formattedDate = DateFormat.getDateTimeInstance().format(new Date(millis));
            holder.textDate.setText(formattedDate);
        } catch (Exception e) {
            holder.textDate.setText(msg.getDate()); // fallback
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}

