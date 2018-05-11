package com.easylife.activity;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.easylife.entity.User;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.Permissions;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UserInfoActivity extends Activity {
    private TextView nickname;
    private TextView username;
    private TextView phoneNumber;
    private TextView changePassword;
    private TextView logout;
    private ImageView back;
    private RoundedImageView avatar;

    //调用系统相册-选择图片
    private static final int IMAGE = 1;
    private static final int REQUEST_ORIGINAL = 2;// 请求原图信号标识
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private String picPath;//图片存储路径


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);

        //初始化标题栏
        initActionBar();

        //初始化控件
        initwidget();

        //初始化用户信息
        initUserInfo();

        //设置点击事件
        setOnClickEvent();
    }

    //设置点击事件
    private void setOnClickEvent() {
        logout.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("退出账号");
            builder.setMessage("确定退出当前账号吗？");
            builder.setPositiveButton("退出", (dialog, which) -> {
                SharedPreferences preferences = getSharedPreferences("login_user", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(UserInfoActivity.this, LoginOrRegisterActivity.class);
                startActivity(intent);
                finish();
            });
            builder.setNegativeButton("取消", (dialog, listener) -> {
                dialog.dismiss();
            });
            builder.show();
        });

        back.setOnClickListener(v -> {
            startActivity(new Intent(UserInfoActivity.this,UserActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        avatar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(UserInfoActivity.this);
            AlertDialog dialog = builder.create();
            View view = LayoutInflater.from(this).inflate(R.layout.change_avatar_dialog, null);

            view.findViewById(R.id.camera_textview).setOnClickListener(view1 -> {
                //调用系统相机拍照
                picPath = Environment.getExternalStorageDirectory().getPath()+"/EasyLife/pic/IMG_" + System.currentTimeMillis() + "_temp.png";
                File file = new File(picPath);

                // android 7.0系统解决拍照的问题
                StrictMode.VmPolicy.Builder vmBuilder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(vmBuilder.build());
                vmBuilder.detectFileUriExposure();

                //获取相机权限
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    /* 相机请求码 */
                    final int REQUEST_CAMERA = 0;
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.CAMERA},REQUEST_CAMERA);
                }
                final Handler handler = new Handler();
                final Timer timer = new Timer(false);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(() -> {
                            if (ActivityCompat.checkSelfPermission(UserInfoActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                timer.cancel();
                                //第一种方法，获取压缩图
//                        Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                        // 启动相机
//                        startActivityForResult(intent1, REQUEST_THUMBNAIL);

                                //第二种方法，获取原图
                                String cameraPackageName = getCameraPhoneAppInfos(UserInfoActivity.this);
                                if (cameraPackageName == null) {
                                    cameraPackageName = "com.android.camera";
                                }
                                final Intent intent_camera = UserInfoActivity.this.getPackageManager()
                                        .getLaunchIntentForPackage(cameraPackageName);
                                Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (intent_camera != null) {
                                    intent2.setPackage(cameraPackageName);
                                }
                                Uri uri = Uri.fromFile(file);
                                //为拍摄的图片指定一个存储的路径
                                intent2.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                startActivityForResult(intent2, REQUEST_ORIGINAL);
                            }
                        });
                    }
                }, 0, 100);

                dialog.dismiss();
            });

            view.findViewById(R.id.album_textview).setOnClickListener(view2 -> {
                //调用相册
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Check if we have read permission
                int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // We don't have permission so prompt the user
                    ActivityCompat.requestPermissions(
                            this,
                            PERMISSIONS_STORAGE,
                            REQUEST_EXTERNAL_STORAGE
                    );
                }
                startActivityForResult(intent, IMAGE);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                dialog.dismiss();
            });

            dialog.setView(view);
            dialog.show();
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE && data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                if (c != null) {
                    c.moveToFirst();
                }
                int columnIndex = c.getColumnIndex(filePathColumns[0]);
                String imagePath = c.getString(columnIndex);
                showImage(imagePath);
                c.close();
            }

            if (requestCode == REQUEST_ORIGINAL){
                /**
                 * 这种方法是通过内存卡的路径进行读取图片，所以的到的图片是拍摄的原图
                 */
                showImage(picPath);
            }
        }
    }

    private void showImage(String imagePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.avatar_view, null);
        ImageView imageView = view.findViewById(R.id.avatar_view_imageview);
        try {
            imageView.setImageBitmap(BitmapFactory.decodeStream(new FileInputStream(imagePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        builder.setView(view);
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setNegativeButton("确定", (dialog, which) -> {

        });
        builder.show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP && event.getRepeatCount() == 0) {
                startActivity(new Intent(UserInfoActivity.this,UserActivity.class));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    //初始化标题栏
    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        View actionBarView = LayoutInflater.from(this).inflate(R.layout.user_actionbar, null);
        if (actionBar != null) {
            actionBar.setCustomView(actionBarView);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        }
    }

    //初始化用户信息
    private void initUserInfo() {
        User user = getCurrentUser();
        nickname.setText(user.getNickname());
        username.setText(user.getUsername());
        phoneNumber.setText(user.getMobilePhoneNumber());
    }

    //初始化控件
    private void initwidget() {
        nickname = findViewById(R.id.nickname_textview);
        username = findViewById(R.id.username_textview);
        phoneNumber = findViewById(R.id.phonenum_textview);
        changePassword = findViewById(R.id.pss_change_textview);
        logout = findViewById(R.id.logout_textview);
        back = findViewById(R.id.back_imageview);
        avatar = findViewById(R.id.avatar_imageview);
    }

    //获取本地用户
    public User getCurrentUser() {
        User currentUser;
        SharedPreferences preferences = getSharedPreferences("login_user", MODE_PRIVATE);
        currentUser = new User(
                preferences.getString("username", "null"),
                preferences.getString("nickname", "null"),
                preferences.getString("password", "null"),
                preferences.getString("user_phone", "null"));

        return currentUser;
    }

    // 对使用系统拍照的处理
    //尽量使用系统相机，屏蔽第三方拍照软件
    public String getCameraPhoneAppInfos(Activity context) {
        try {
            String strCamera = "";
            List<PackageInfo> packages = context.getPackageManager()
                    .getInstalledPackages(0);
            for (int i = 0; i < packages.size(); i++) {
                try {
                    PackageInfo packageInfo = packages.get(i);
                    String strLabel = packageInfo.applicationInfo.loadLabel(
                            context.getPackageManager()).toString();
                    // 一般手机系统中拍照软件的名字
                    if ("相机,照相机,照相,拍照,摄像,Camera,camera".contains(strLabel)) {
                        strCamera = packageInfo.packageName;
                        if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (strCamera != null) {
                return strCamera;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
