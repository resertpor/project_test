package com.example.por.project_test;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Por on 10/9/2016.
 */

class GroupMessageAdapter extends ArrayAdapter<GroupMessageInfo> {
    private final Context ctx;
    List<GroupMessageInfo> value;
    String user_id_current;
    ImageView img_file;
    String token, id;

    GroupMessageAdapter(Context ctx, int resource, int textViewResourceId, ArrayList<GroupMessageInfo> value, String token, String id) {
        super(ctx, resource, textViewResourceId, value);
        this.ctx = ctx;
        this.value = value;
        SharedPreferences sp = ctx.getSharedPreferences("MySetting", Context.MODE_PRIVATE);
        user_id_current = sp.getString("user_id_current", "-1");
        this.token = token;
        this.id = id;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        TextView textView, tv_time_sender, tv_time_receiver;
        Drawable img;

        String filename;
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.groupmessage, parent, false);
        LinearLayout linearLayout = (LinearLayout) rowView.findViewById(R.id.ln_group_reciever);
        LinearLayout linearLayout2 = (LinearLayout) rowView.findViewById(R.id.ln_group_sender);

        tv_time_sender = (TextView) rowView.findViewById(R.id.tv_group_sender_time);
        tv_time_sender.setText(value.get(position).time);
        tv_time_receiver = (TextView) rowView.findViewById(R.id.tv_group_reciver_time);
        tv_time_receiver.setText(value.get(position).time);

        if (user_id_current.equals(value.get(position).message_sender_id + "")) {
            linearLayout2.setVisibility(View.VISIBLE);
            textView = (TextView) rowView.findViewById(R.id.tv_group_message_adapter);
            img_file = (ImageView) rowView.findViewById(R.id.img_group_upload);

        } else {
            linearLayout.setVisibility(View.VISIBLE);
            textView = (TextView) rowView.findViewById(R.id.tv_groupmessage_left);

            TextView tv_user_from = (TextView) rowView.findViewById(R.id.tv_group_from);

            tv_user_from.setText(value.get(position).username);
            img_file = (ImageView) rowView.findViewById(R.id.img_group_download);
        }
        if (value.get(position).message_status == 4) {
            TextView tv_read = (TextView) rowView.findViewById(R.id.tv_read);
            tv_read.setVisibility(View.VISIBLE);
        }

        if (value.get(position).type.equals("file")) {
            filename = value.get(position).filename;


            if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                String url = BackgoundWorker.url_server + "download_filegroup.php?messageid=" + value.get(position).group_message_id + "&token=" + token + "&userid=" + id;
                img_file.setVisibility(View.VISIBLE);
                textView.setVisibility(View.GONE);

                String msgId = value.get(position).group_message_id + "";

                if (ImageCacheUtils.hasCache(ctx, msgId)) {
                    Bitmap bm = ImageCacheUtils.load(ctx, msgId);
                    if (bm != null) {
                        img_file.setImageBitmap(bm);
                    }
                } else {
                    new LoadImageTask(ctx, msgId, img_file).execute(url,"group");
                }
            } else {
                textView.setText(value.get(position).filename);
                img = rowView.getResources().getDrawable(R.drawable.file);
                img.setBounds(0, 0, 70, 70);
                textView.setCompoundDrawables(img, null, null, null);
            }
        } else if (value.get(position).type.equals("text")) {
            textView.setText(value.get(position).message);
        } else if (value.get(position).type.equals("map")) {
            textView.setText("Location");
            img = rowView.getResources().getDrawable(R.drawable.makermap);
            img.setBounds(0, 0, 70, 70);
            textView.setCompoundDrawables(img, null, null, null);
        }
        return rowView;
    }

}
