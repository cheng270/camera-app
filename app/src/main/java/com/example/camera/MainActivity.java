package com.example.camera;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.camera.utils.imageHelper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    //xml file variables
    private Button mBtnTakePhoto;
    private Button mBtnSelectPhoto;

    //Log variable
    private static final String TAG="MainActivity";

    // Photo file path
    private String SD_CARD_TEMP_DIR = Environment.getExternalStorageDirectory()
            + File.separator + "tmp.jpg";

    // angle of a photo
    private int angle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();

        //click on take photo
        mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission(v);
            }
        });

        //click on select photo
        mBtnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission(v);
            }
        });
    }

    // preparatory function
    private void initialize(){
        mBtnSelectPhoto = findViewById(R.id.btn_selectPhoto);
        mBtnTakePhoto = findViewById(R.id.btn_takePhoto);
        //uri privacy
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            builder.detectFileUriExposure();
        }
    }

    // ask permission or start task
    private void checkCameraPermission(View v){
        // check permission of camera
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            //with permission
            switch (v.getId()) {
                case R.id.btn_takePhoto:
                    takePhoto();
                    break;
                case R.id.btn_selectPhoto:
                    choosePhoto();
                    break;
                default:
                    break;
            }
        }
    }

    // take photo task
    private void takePhoto(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(SD_CARD_TEMP_DIR)));
        startActivityForResult(cameraIntent, 1);
    }

    // select photo task
    private void choosePhoto() {
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intentToPickPic, 2);
    }

    // activity result
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // take photo task
        if (resultCode == RESULT_OK && requestCode  == 1){
            // obtain the angle of a original picture and turn into 0`
             angle = imageHelper.readPictureDegree(SD_CARD_TEMP_DIR);
            Bitmap bmp = imageHelper.getCompressPhoto(SD_CARD_TEMP_DIR);
            Bitmap bitmap = imageHelper.rotaingImageView(angle, bmp);
            //save the new picture
            SD_CARD_TEMP_DIR = imageHelper.savePhotoToSD(bitmap, SD_CARD_TEMP_DIR);
            // start new intent
            Intent intent = new Intent(MainActivity.this, PsActivity.class);
            intent.putExtra("filepath", SD_CARD_TEMP_DIR);
            startActivity(intent);
            // show  the directory on screen
            Toast.makeText(this, SD_CARD_TEMP_DIR, Toast.LENGTH_LONG).show();
            android.os.Process.killProcess(android.os.Process.myPid());

        }
        // select photo result
        if (resultCode == RESULT_OK && requestCode == 2) {
            // obtain uri
            Uri uri = data.getData();
            if (uri != null) {
                //start a new intent
                Intent intent = new Intent(MainActivity.this, PsActivity.class);
                intent.putExtra("uri", uri.toString());
                startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }


}