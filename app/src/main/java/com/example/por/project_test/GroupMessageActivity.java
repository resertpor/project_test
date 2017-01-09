package com.example.por.project_test;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class GroupMessageActivity extends AppCompatActivity implements HttpRequestCallback {

    static String id, token;
    String groupId, groupName, type;
    boolean isRequesting;
    ArrayList<GroupMessageInfo> groupMessageInfos;
    GroupMessageAdapter groupMessageAdapter;
    int lastMessageId;
    static int REQUEST_FILE = 1;
    Button bt_group_file, bt_group_send_message;
    EditText et_group_message;
    ListView listView_group_message;
    Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);

        bt_group_file = (Button) findViewById(R.id.bt_group_file);
        et_group_message = (EditText) findViewById(R.id.et_group_message);
        bt_group_send_message = (Button) findViewById(R.id.bt_group_send_message);
        listView_group_message = (ListView) findViewById(R.id.listview_group_message);

        Intent i = getIntent();
        groupId = i.getStringExtra("groupid");
        setTitle(groupName = i.getStringExtra("groupname"));

        SharedPreferences sp = getSharedPreferences("MySetting", MODE_PRIVATE);
        id = sp.getString("user_id_current", "-1");
        token = sp.getString("token", "-1");

        listView_group_message.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GroupMessageInfo groupMessageInfo = groupMessageInfos.get(position);
                String url = BackgoundWorker.url_server + "downloadfile.php?messageid=" + groupMessageInfo.group_message_id + "&token=" + token + "&userid=" + id;
                String filename = groupMessageInfo.filename;
                if (groupMessageInfo.type.equals("file")) {
                    Intent intent = new Intent(GroupMessageActivity.this, DownloadFileService.class);
                    intent.putExtra("url", url);
                    intent.putExtra("filename", filename);
                    startService(intent);
                } else if (groupMessageInfo.type.equals("map")) {
                    Intent intent = new Intent(GroupMessageActivity.this, MapsActivity.class);
                    intent.putExtra("lat", groupMessageInfo.latitude);
                    intent.putExtra("lon", groupMessageInfo.longtitude);
                    startActivity(intent);
                }
            }
        });

        bt_group_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str_message = et_group_message.getText().toString().trim();
                if (str_message.isEmpty() || str_message.length() == 0 || str_message.equals("") || str_message == null) {
                    return;
                } else {
                    type = "sendmessagegroup";
                    BackgoundWorker backgoundWorker = new BackgoundWorker(GroupMessageActivity.this);
                    backgoundWorker.execute(type, id, token, groupId, str_message, "text");
                    et_group_message.setText("");
                }
            }
        });

        bt_group_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence colors[] = new CharSequence[]{"FIle", "Share Location"};

                AlertDialog.Builder builder = new AlertDialog.Builder(GroupMessageActivity.this);
                builder.setTitle("Share");
                builder.setItems(colors, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (checkFilePermission() == false) {
                                return;
                            }
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);//แสดงไฟล์เฉพาะactivityเปิดได้
                            intent.setType("*/*");//(image/jpg)
                            startActivityForResult(intent, REQUEST_FILE);
                        } else if (which == 1) {
                            Intent intent = new Intent(GroupMessageActivity.this, MapsActivity.class);
                            intent.putExtra("groupid", groupId);
                            startActivity(intent);


                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onResume() {
        startFetch();
        super.onResume();
    }

    @Override
    protected void onPause() {
        t.cancel();
        super.onPause();
    }

    private void startFetch() {
        t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isRequesting) {
                    return;
                } else {
                    isRequesting = true;
                    String type1 = "readmessage";
                    BackgoundWorker backgoundWorker = new BackgoundWorker(GroupMessageActivity.this);
                    backgoundWorker.execute(type1, id, groupId, lastMessageId + "", token);
                }
            }
        }, 500, 500);
    }

    @Override
    public void onResult(String[] result, ArrayList<Object> mesObjects) {
        if (mesObjects == null && result == null) {
            isRequesting = false;
            return;
        } else if ((result != null) && (result[1].equals(BackgoundWorker.FALSE))) {

            Toast.makeText(this, result[0], Toast.LENGTH_SHORT).show();
            return;

        } else if (mesObjects == null) {
            return;
        }
        ArrayList<GroupMessageInfo> messageInfos = new ArrayList<>();
        for (Object o : mesObjects) {

            if (o instanceof GroupMessageInfo) {//เข็คoใช่objectของclassหรือไม่
                GroupMessageInfo mo = (GroupMessageInfo) o;
                if (mo.type.equals("authen")) {

                } else if (mo.type.equals("map")) {
                    try {
                        mo.latitude = Double.parseDouble((mo.tmpLat));
                        mo.longtitude = Double.parseDouble((mo.tmpLon));
                        messageInfos.add(mo);
                    } catch (Exception e) {
                    }
                } else if (mo.type.equals("text")) {
                    try {
//                        mo.message = (mo.message);
                        groupMessageInfos.add(mo);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mo.message = "failed to decrypt...";
                        groupMessageInfos.add(mo);
                    }
                } else {
                    groupMessageInfos.add(mo);
                }
            }
        }
        if (groupMessageInfos.size() > 0) {
            this.groupMessageInfos.addAll(messageInfos);
            lastMessageId = messageInfos.get(messageInfos.size() - 1).group_message_id;//ขนาดของตัวมัน-1 ถ้ามี20 ได้19
            groupMessageAdapter.notifyDataSetChanged();
        }

        isRequesting = false;
        bt_group_send_message.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            String filename = getFileName(uri);
            byte[] filedata = getData(uri);

            if (filedata == null) {
                return;
            }
            if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                Bitmap bm = BitmapFactory.decodeByteArray(filedata, 0, filedata.length);
                Bitmap resized = null;

                if (bm.getWidth() > bm.getHeight()) {
                    resized = Bitmap.createScaledBitmap(bm, 800, (int) (800 * ((float) bm.getHeight() / bm.getWidth())), true);
                } else {
                    resized = Bitmap.createScaledBitmap(bm, (int) (800 * ((float) bm.getWidth() / bm.getHeight())), 800, true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                filedata = baos.toByteArray();
            }

//            String encryptFile = encrypt(filedata);
//            String encryptFile = Base64.encodeToString(filedata,Base64.DEFAULT);//no encrypt

            String type2 = "sendmessage";

            BackgoundWorker backgoundWorker = new BackgoundWorker(GroupMessageActivity.this);
//            backgoundWorker.execute(type2, id, groupId, encryptFile, "file", filename, "", "", token);


        }
    }

    private boolean checkFilePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {//เแอพนี้ช็คว่ามีสิทธอ่านไฟล์รึยัง

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);//ถ้ายังก็ขอ
            return false;
        }
        return true;
    }

    private byte[] getData(Uri uri) {//อ่านไฟล์โดยให้pathไปreturnเป็นbyte binaryกลับมา
        byte[] result = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            if (inputStream.available() > 10e6) {
                Toast.makeText(this, "File size must be 10mb", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];//อ่านทั้ฝหมด16kb ในเgoogleบอกเร็วสุด
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();//เขียนข้อไปให้หมด

            result = buffer.toByteArray();
            inputStream.close();//ถ้าไม่ปิดแสดงว่ามีคนใช้อยู่จะลบไม่ได้


        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_groupchat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invite_friend:
                Intent i = new Intent(GroupMessageActivity.this, InviteGroupActivity.class);
                i.putExtra("groupid", groupId);
                i.putExtra("groupname", groupName);
                startActivity(i);
                return true;
            case R.id.member:
                Intent i2 = new Intent(GroupMessageActivity.this, MemberGroupActivity.class);
                i2.putExtra("groupid", groupId);
                i2.putExtra("groupname", groupName);
                startActivity(i2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
