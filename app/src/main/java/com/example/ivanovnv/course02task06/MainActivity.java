package com.example.ivanovnv.course02task06;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_CODE = 12345;
    private Button mStartDownloadButton;
    private EditText mUrlEditText;
    private ArrayList<String> mValidExtensions = new ArrayList<String>(){{
                                                                    add("jpeg"); 
                                                                    add("png"); 
                                                                    add("bmp");}};
    private DownloadManager mDownloadManager;
    private long refid;
    private Button mShowButton;
    private String mFileName;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermissionIfNeeded();

        mStartDownloadButton = findViewById(R.id.bt_download);
        mUrlEditText = findViewById(R.id.et_url);
        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mShowButton = findViewById(R.id.bt_show_image);
        mImageView = findViewById(R.id.iv_image);

    }

    private boolean requestRuntimePermissionIfNeeded() {
        if (!isPermissionGranted()) {
            requestRuntimePermission();
            return false;
        } else {
            return true;
        }
    }

    private void requestRuntimePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.ad_rationale)
                    .setPositiveButton(getString(R.string.ad_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            return ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode != REQUEST_CODE) return;
        if(grantResults.length != 1) return;

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           // checkUrlAndDownload(mUrlEditText.getText().toString());
        } else {
             new AlertDialog.Builder(this)
                    .setMessage(R.string.add_perm_later)
                    .setPositiveButton(getString(R.string.ad_ok), null)
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mStartDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestRuntimePermissionIfNeeded()) {
                    checkUrlAndDownload(mUrlEditText.getText().toString());
                }
            }
        });

        registerReceiver(mDownloadBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImageFileToImageView();
            }
        });
    }

    private void loadImageFileToImageView() {
      /*  File imgFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + mFileName);

        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            mImageView.setImageBitmap(bitmap);
        }*/

      mImageView.setImageURI(mDownloadManager.getUriForDownloadedFile(refid));
    }

    private void checkUrlAndDownload(String string) {
        if (string.isEmpty()) {
            Toast.makeText(this, R.string.download_error_empty, Toast.LENGTH_SHORT).show();
        } else {
            if(!URLUtil.isValidUrl(string)) {
                Toast.makeText(this, R.string.download_error_not_url, Toast.LENGTH_SHORT).show();
            } else {
                   if(string.contains(".") && mValidExtensions.contains(string.substring(string.lastIndexOf(".")+1).
                                                        toLowerCase())) {
                        downloadFile(string);
                    } else {
                        Toast.makeText(this, R.string.download_error_file_type, Toast.LENGTH_SHORT).show();
                    }
            }
        }
    }

    private void downloadFile(String string) {
        Uri uri = Uri.parse(string);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        mFileName = (new File(string)).getName();

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)
                .setAllowedOverRoaming(false)
                .setDescription("Загрузка изображения")
                .setTitle("Загрузка изображения")
                .setVisibleInDownloadsUi(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mFileName);

        refid = mDownloadManager.enqueue(request);
    }


    @Override
    protected void onStop() {
        super.onStop();

        mStartDownloadButton.setOnClickListener(null);
        mShowButton.setOnClickListener(null);
        unregisterReceiver(mDownloadBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mStartDownloadButton = null;
        mUrlEditText = null;
        mDownloadManager = null;
        mShowButton = null;
        mImageView = null;


    }

    BroadcastReceiver mDownloadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == refid) {
                mShowButton.setEnabled(true);
                Toast.makeText(context, R.string.download_complete, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
