package  com.example.camera;


import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.common.MLPosition;
import com.huawei.hms.mlsdk.face.MLFace;
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer;
import com.huawei.hms.mlsdk.face.MLFaceShape;
import com.example.camera.utils.imageHelper;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static com.huawei.hms.mlsdk.face.MLFaceShape.TYPE_FACE;
import static com.huawei.hms.mlsdk.face.MLFaceShape.TYPE_LEFT_EYE;
import static com.huawei.hms.mlsdk.face.MLFaceShape.TYPE_RIGHT_EYE;

public class PsActivity extends AppCompatActivity {

    //Log variable
    private static final String TAG = "PsActivity";

    // photo default values
    private float mHue = 0;
    private float mSaturation = 1;
    private float mLum = 1;
    private int eyevalue = 0;
    private int facevalue = 0;

    //xml file variables
    private SeekBar seekBarEye;
    private SeekBar seekBarFace;
    private ImageView photo;
    private Bitmap beauty;

    //mlkit classiffier variables
    private MLFaceAnalyzer analyzer;
    private Point lefteyepoint;
    private Point righteyepoint;
    private float lefteyeleft;
    private float lefteyetop;
    private float lefteyeright;
    private float lefteyebottom;
    private float righteyeleft;
    private float righteyetop;
    private float righteyeright;
    private float righteyebottom;
    private List<MLPosition> leftfacePoints = null;
    private List<MLPosition> rightfacePoints = null;

    // Bitmap variables
    private Bitmap bitmap;
    private Bitmap initBeauty;

    // dinamic permissions
    private static final String[] PERMISSION_EXTERNAL_STORAGE = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_EXTERNAL_STORAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ps);

        initialize();
        obtainBitmap();
        setImage(bitmap);
        seekBarEye.setOnSeekBarChangeListener(onSeekBarChangeListener);
        seekBarFace.setOnSeekBarChangeListener(onSeekBarChangeListener);
    }

    // preparatory function
    private void initialize(){
        photo = findViewById(R.id.photo);
        seekBarEye = findViewById(R.id.seekbareye);
        seekBarFace = findViewById(R.id.seekbarface);
        verifyStoragePermissions(PsActivity.this);
    }

    // Obtain th ephoto to be modified
    private void obtainBitmap() {
        Intent intent = getIntent();
        // photo from 'take photo' task
        String filepath = intent.getStringExtra("filepath");
        if (filepath != null) {
            try {
                bitmap = imageHelper.getScaleBitmap(filepath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //photo from 'select photo'task
        String string = intent.getStringExtra("uri");
        if (string != null) {
            Uri uri = Uri.parse(string);
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                bitmap = imageHelper.scaleBitmap(bitmap, 0.5f);
            } catch (IOException error) {
                error.getStackTrace();
            }
        }
    }

    //show the photo on the PS screen
    private void setImage(Bitmap bitmap) {
        beauty = imageHelper.drawBitmap(bitmap, mHue, mSaturation, mLum);
        photo.setImageBitmap(beauty);
        initBeauty = beauty.copy(beauty.getConfig(), false);
       analyzerFace(beauty);
    }

    // Face detection with Huawei ml.kit
    private void analyzerFace(final Bitmap bitmap) {
        //detection
        MLFrame frame = MLFrame.fromBitmap(bitmap);
        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer();
        Task<List<MLFace>> task = analyzer.asyncAnalyseFrame(frame);

        task.addOnSuccessListener(new OnSuccessListener<List<MLFace>>() {
            @Override
            public void onSuccess(List<MLFace> faces) {
                //  success
                if (faces.size() <= 0) {
                    return;
                }
                //Face contour coordinates
                MLFaceShape face = faces.get(0).getFaceShape(TYPE_FACE);
                //left Face contour coordinates
                if (leftfacePoints != null) {
                    leftfacePoints.clear();
                }
                leftfacePoints = new ArrayList<>();
                for (int i = 0; i < face.getPoints().size() / 2; i++) {
                    leftfacePoints.add(face.getPoints().get(i));
                }
                // right Face contour coordinates
                if (rightfacePoints != null) {
                    rightfacePoints.clear();
                }
                rightfacePoints = new ArrayList<>();
                for (int i = face.getPoints().size() / 2; i < face.getPoints().size(); i++) {
                    rightfacePoints.add(face.getPoints().get(i));
                }
                // left eye coordinates
                MLFaceShape lefteye = faces.get(0).getFaceShape(TYPE_LEFT_EYE);
                lefteyeleft = lefteye.getPoints().get(0).getX();
                lefteyetop = lefteye.getPoints().get(0).getY();
                lefteyeright = lefteye.getPoints().get(0).getX();
                lefteyebottom = lefteye.getPoints().get(0).getY();
                for (int i = 0; i < lefteye.getPoints().size(); i++) {
                    if (lefteye.getPoints().get(i).getX() < lefteyeleft) {
                        lefteyeleft = lefteye.getPoints().get(i).getX();
                    }
                    if (lefteye.getPoints().get(i).getX() > lefteyeright) {
                        lefteyeright = lefteye.getPoints().get(i).getX();
                    }
                    if (lefteye.getPoints().get(i).getX() < lefteyetop) {
                        lefteyetop = lefteye.getPoints().get(i).getY();
                    }
                    if (lefteye.getPoints().get(i).getX() > lefteyebottom) {
                        lefteyebottom = lefteye.getPoints().get(i).getY();
                    }
                }
                lefteyepoint = new Point((int) ((lefteyeleft + lefteyeright) / 2), (int) ((lefteyetop + lefteyebottom) / 2));

                // right eye coordinates
                MLFaceShape righteye = faces.get(0).getFaceShape(TYPE_RIGHT_EYE);
                righteyeleft = righteye.getPoints().get(0).getX();
                righteyetop = righteye.getPoints().get(0).getY();
                righteyeright = righteye.getPoints().get(0).getX();
                righteyebottom = righteye.getPoints().get(0).getY();
                for (int i = 0; i < righteye.getPoints().size(); i++) {
                    if (righteye.getPoints().get(i).getX() < righteyeleft) {
                        righteyeleft = righteye.getPoints().get(i).getX();
                    }
                    if (righteye.getPoints().get(i).getX() > righteyeright) {
                        righteyeright = righteye.getPoints().get(i).getX();
                    }
                    if (righteye.getPoints().get(i).getX() < righteyetop) {
                        righteyetop = righteye.getPoints().get(i).getY();
                    }
                    if (righteye.getPoints().get(i).getX() > righteyebottom) {
                        righteyebottom = righteye.getPoints().get(i).getY();
                    }
                }
                righteyepoint = new Point((int) ((righteyeleft + righteyeright) / 2), (int) ((righteyetop + righteyebottom) / 2));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // detect fail
                Log.e(TAG, "e=" + e.getMessage());
            }
        });
    }
    
    // when seekBar is changed
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            switch (seekBar.getId()) {
                case R.id.seekbareye:// big eye seekbar

                    if (facevalue > 0 && leftfacePoints != null && rightfacePoints != null) {
                        beauty = imageHelper.slimFace(initBeauty, rightfacePoints, leftfacePoints, facevalue);
                        beauty = imageHelper.drawBitmap(beauty, mHue, mSaturation, mLum);
                    } else {
                        beauty = imageHelper.drawBitmap(initBeauty, mHue, mSaturation, mLum);
                    }

                    eyevalue = progress;
                    beauty = imageHelper.magnifyEyes(beauty, lefteyepoint, righteyepoint, (int) ((lefteyeright - lefteyeleft) / 2 * 3), (int) ((righteyeright - righteyeleft) / 2 * 3), progress);
                    break;

                case R.id.seekbarface://thine face seekbar

                    if (eyevalue > 0 && lefteyepoint != null && righteyepoint != null) {
                        beauty = imageHelper.magnifyEyes(initBeauty, lefteyepoint, righteyepoint, (int) ((lefteyeright - lefteyeleft) / 2 * 3), (int) ((righteyeright - righteyeleft) / 2 * 3), eyevalue);
                        beauty = imageHelper.drawBitmap(beauty, mHue, mSaturation, mLum);
                    } else {
                        beauty = imageHelper.drawBitmap(initBeauty, mHue, mSaturation, mLum);
                    }
                    facevalue = progress;
                    beauty = imageHelper.slimFace(beauty, rightfacePoints, leftfacePoints, progress);
                    break;
                default:
                    break;
            }
            photo.setImageBitmap(beauty);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    
    // permission 
    private void verifyStoragePermissions(Activity activity) {
        int permissionWrite = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionWrite != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_EXTERNAL_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

}