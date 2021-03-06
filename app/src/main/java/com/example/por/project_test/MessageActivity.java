package com.example.por.project_test;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MessageActivity extends AppCompatActivity implements HttpRequestCallback {
    ListView listView_message;
    Button bt_send, bt_file;
    RSAEncryption rsaEncryption;
    AESEncryption aesEncryption;
    EditText et_message;
    static String id, token, shareedkey;
    static int REQUEST_FILE = 1;
    String friendid, publickey, usernameFriend;
    int lastMessageId;
    ArrayList<MessageInfo> messageInfos;
    MessageAdapter messageAdapter;
    boolean isRequesting, isMessage = false,download=true;
    Timer t;
    MessageInfo tmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        lastMessageId = 0;
        listView_message = (ListView) findViewById(R.id.listview_message);
        et_message = (EditText) findViewById(R.id.et_message);
        bt_send = (Button) findViewById(R.id.bt_send_message);
        bt_file = (Button) findViewById(R.id.bt_file);


        final Intent i = getIntent();
        friendid = i.getStringExtra("friendid");
        publickey = i.getStringExtra("publickey");

        shareedkey = checkhashkey();
        aesEncryption = new AESEncryption(shareedkey);


        SharedPreferences sp = getSharedPreferences("MySetting", MODE_PRIVATE);
        id = sp.getString("user_id_current", "-1");
        token = sp.getString("token", "-1");

        messageInfos = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, R.layout.message, R.id.tv_message_adapter, messageInfos, token, id);
        listView_message.setAdapter(messageAdapter);
        usernameFriend = i.getStringExtra("frienduser");
        setTitle(usernameFriend);

        listView_message.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                MessageInfo message = messageInfos.get(i);
                String url = BackgoundWorker.url_server + "download_file.php?messageid=" + message.message_id + "&token=" + token + "&userid=" + id;
                String filename = message.filename;
                if (message.type.equals("file")) {
                    Intent intent = new Intent(MessageActivity.this, DownloadFileService.class);
                    intent.putExtra("url", url);
                    intent.putExtra("filename", filename);
                    intent.putExtra("sharedkey", shareedkey);
                    startService(intent);
                } else if (message.type.equals("map")) {
                    Intent intent = new Intent(MessageActivity.this, MapsActivity.class);
                    intent.putExtra("lat", message.latitude);
                    intent.putExtra("lon", message.longtitude);
                    startActivity(intent);
                }
            }
        });
        bt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String str_message = et_message.getText().toString().trim();
                if (str_message.isEmpty() || str_message.length() == 0 || str_message.equals("")) {
                    return;
                } else {
                    if (shareedkey == null) {
                        genSharedKey();
                    }
//                    //secure
//                    aesEncryption = new AESEncryption(shareedkey);
//                    str_message = aesEncryption.encrypt(str_message);
////                    Log.d("str message", str_message);
//
//secure
                    aesEncryption = new AESEncryption(shareedkey);
                    String message = str_message;
                    str_message = aesEncryption.encrypt(str_message);
//                    Log.d("str message", str_message);

//                    new BackgoundWorker(MessageActivity.this).execute("sendmessage", id, friendid, str_message, "text", "", "", "", token);
//                    et_message.setText("");

                    /******************/

                    tmp = new MessageInfo(10000, message, Integer.parseInt(friendid), Integer.parseInt(id), "", "", "", "", "", "text");
                    MessageActivity.this.messageInfos.add(tmp);
                    messageAdapter.notifyDataSetChanged();
                    isMessage = true;
                    /******************/


                    new BackgoundWorker(MessageActivity.this).execute("sendmessage", id, friendid, str_message, "text", "", "", "", token);
                    et_message.setText("");
                }


            }
        });
        bt_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (shareedkey == null) {
                    genSharedKey();
                }

                CharSequence colors[] = new CharSequence[]{"FIle", "Share Location"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle("Share");
                builder.setItems(colors, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (!checkFilePermission()) {
                                return;
                            }
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);//แสดงไฟล์เฉพาะactivityเปิดได้
                            intent.setType("*/*");//(image/jpg)
                            startActivityForResult(intent, REQUEST_FILE);
                        } else if (which == 1) {
                            Intent intent = new Intent(MessageActivity.this, MapsActivity.class);
                            intent.putExtra("friendid", friendid);
                            intent.putExtra("sharedkey", shareedkey);
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
                    new BackgoundWorker(MessageActivity.this).execute("readmessage", id, friendid, lastMessageId + "", token);
                }
            }
        }, 500, 500);
    }

    private void genSharedKey() {
        while ((shareedkey == null) || (shareedkey.length() != 32)) {
            shareedkey = new BigInteger(160, new SecureRandom()).toString(32);
            shareedkey = "00000000000000000000000000000000".substring(shareedkey.length()) + shareedkey;
        }

        SharedPreferences.Editor editor = getSharedPreferences("MySetting", MODE_PRIVATE).edit();
        editor.putString("SHARED_KEY:" + friendid, shareedkey);
        editor.apply();

        rsaEncryption = new RSAEncryption(this);
        String sharedKeyMessage = rsaEncryption.RSAEncrypt(publickey, shareedkey);

        new BackgoundWorker(this).execute("sendmessage", id, friendid, sharedKeyMessage, "authen", "", "", "", token);

        SharedPreferences sp = getSharedPreferences("MySetting", MODE_PRIVATE);
        String mySharedKeyMessage = rsaEncryption.RSAEncrypt(sp.getString("publickey", "-1"), shareedkey);
        new BackgoundWorker(this).execute("sendmessage", id, id, mySharedKeyMessage, "authen", "", "", "", token, friendid);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {//data สิ่งที่activityกลับมาคืนเรา
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();

            GetFile getFile = new GetFile(this);
            String filename = getFile.getFileName(uri);
            byte[] filedata = getFile.getData(uri);

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
            String md5 = GetMD5.getMD5EncryptedString(Base64.encodeToString(filedata, Base64.DEFAULT));

            String encryptFile = aesEncryption.encrypt(filedata);
//            String encryptFile = Base64.encodeToString(filedata,Base64.DEFAULT);//no encrypt

            tmp = new MessageInfo(10000, "", Integer.parseInt(friendid), Integer.parseInt(id), filename, "", "", "", "", "file");
            MessageActivity.this.messageInfos.add(tmp);
            messageAdapter.notifyDataSetChanged();
            isMessage = true;


            new BackgoundWorker(MessageActivity.this).execute("sendmessage", id, friendid, encryptFile, "file", filename, "", "", token, md5);


        }
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
        ArrayList<MessageInfo> messageInfos = new ArrayList<>();
        for (Object o : mesObjects) {

            if (o instanceof MessageInfo) {//เข็คoใช่objectของclassหรือไม่
                MessageInfo mo = (MessageInfo) o;
                //secure
                switch (mo.type) {
                    case "authen":
                        if (shareedkey == null) {
                            rsaEncryption = new RSAEncryption(this);
                            shareedkey = rsaEncryption.RSADecrypt(mo.authen);
                            aesEncryption = new AESEncryption(shareedkey);

                            SharedPreferences.Editor editor = getSharedPreferences("MySetting", MODE_PRIVATE).edit();
                            editor.putString("SHARED_KEY:" + friendid, shareedkey);
                            editor.apply();
                        }
                        break;
                    case "map":
                        try {
                            mo.latitude = Double.parseDouble(aesEncryption.decrypt(mo.tmpLat));
                            mo.longtitude = Double.parseDouble(aesEncryption.decrypt(mo.tmpLon));
                            messageInfos.add(mo);
                        } catch (Exception e) {
                            e.printStackTrace();
                            mo.message = "failed to decrypt...";
                            messageInfos.add(mo);
                        }
                        break;
                    case "text":
                        try {

                            if (isMessage) {
                                int lastMessage = this.messageInfos.size() - 1;
                                this.messageInfos.remove(lastMessage);
                                isMessage = false;
                            }


                            mo.message = aesEncryption.decrypt(mo.message);


                            messageAdapter.notifyDataSetChanged();

                            messageInfos.add(mo);


                        } catch (Exception e) {
                            e.printStackTrace();
                            mo.message = "failed to decrypt...";
                            messageInfos.add(mo);
                        }
                        break;
                    default:
                        if (isMessage) {
                            int lastMessage = this.messageInfos.size() - 1;
                            this.messageInfos.remove(lastMessage);
                            isMessage = false;
                        }
                        messageAdapter.notifyDataSetChanged();
                        messageInfos.add(mo);
                        break;
                }


            }

        }

        if (messageInfos.size() > 0) {
            this.messageInfos.addAll(messageInfos);
            lastMessageId = messageInfos.get(messageInfos.size() - 1).message_id;//ขนาดของตัวมัน-1 ถ้ามี20 ได้19
            messageAdapter.notifyDataSetChanged();
        }
        isRequesting = false;
        bt_send.setEnabled(true);
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

    public String checkhashkey() {
        SharedPreferences sp = getSharedPreferences("MySetting", MODE_PRIVATE);
//        return sp.getString("SHARED_KEY:" + friendid, "1234567890asdfgh1234567890asdfgh");
        return sp.getString("SHARED_KEY:" + friendid, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(MessageActivity.this, ContactActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_message, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Call_Single:
                Intent i = new Intent(MessageActivity.this, CallSingleActivity.class);
                i.putExtra("friendid", friendid);
                i.putExtra("username_friend", usernameFriend);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

