package com.example.camera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import com.huawei.hms.mlsdk.common.MLPosition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class imageHelper {
  private static final String TAG = "ImageHelper";

  public static Bitmap drawBitmap(Bitmap bm, float hue, float saturation, float lum) {
    //create hue matrix
    ColorMatrix hueMatrix = new ColorMatrix();
    hueMatrix.setRotate(0, hue);//0:Red
    hueMatrix.setRotate(1, hue);//1:Green
    hueMatrix.setRotate(2, hue);//2:Blue

    //create saturation matrix
    ColorMatrix saturationMatrix = new ColorMatrix();
    //set saturation
    saturationMatrix.setSaturation(saturation);

    //create lum matrix
    ColorMatrix lumMatrix = new ColorMatrix();
    //set lum
    lumMatrix.setScale(lum, lum, lum, 1);

    //mix matrix
    ColorMatrix imageMatrix = new ColorMatrix();
    imageMatrix.postConcat(hueMatrix);
    imageMatrix.postConcat(saturationMatrix);
    imageMatrix.postConcat(lumMatrix);


    Paint paint = new Paint();
    paint.setColorFilter(new ColorMatrixColorFilter(imageMatrix));

    Bitmap bitmap = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawBitmap(bm, 0, 0, paint);
    return bitmap;
  }


  /**
   * Eye enlargement algorithm
   *
   * @param bitmap           original bitmap
   * @param leftCenterPoint  center point of left eye
   * @param rightCenterPoint center point of right eye
   * @param leftradius       left eye magnification radius
   * @param rightradius      right eye magnification radius
   * @param sizeLevel        level  [0,4]
   */
  public static Bitmap magnifyEyes(Bitmap bitmap, Point leftCenterPoint, Point rightCenterPoint, int leftradius, int rightradius, float sizeLevel) {

    if (leftCenterPoint == null && rightCenterPoint == null) {
      return null;
    }
    if (leftCenterPoint != null) {
      bitmap = magnifyEye(bitmap, leftCenterPoint, leftradius, sizeLevel);
    }
    if (rightCenterPoint != null) {
      bitmap = magnifyEye(bitmap, rightCenterPoint, rightradius, sizeLevel);
    }
    return bitmap;
  }

  public static Bitmap magnifyEye(Bitmap bitmap, Point centerPoint, int radius, float sizeLevel) {
    Bitmap resultBitmap = bitmap.copy(bitmap.getConfig(), true);
    int left = Math.max(centerPoint.x - radius, 0);
    int top = Math.max(centerPoint.y - radius, 0);
    int right = centerPoint.x + radius > bitmap.getWidth() ? bitmap.getWidth() - 1 : centerPoint.x + radius;
    int bottom = centerPoint.y + radius > bitmap.getHeight() ? bitmap.getHeight() - 1 : centerPoint.y + radius;
    int powRadius = radius * radius;

    int offsetX, offsetY, powDistance, powOffsetX, powOffsetY;

    int disX, disY;

    //When it is negative, it is reduced
    float strength = (5 + sizeLevel * 2) / 10;

    for (int i = top; i <= bottom; i++) {
      offsetY = i - centerPoint.y;
      for (int j = left; j <= right; j++) {
        offsetX = j - centerPoint.x;
        powOffsetX = offsetX * offsetX;
        powOffsetY = offsetY * offsetY;
        powDistance = powOffsetX + powOffsetY;

        if (powDistance <= powRadius) {
          double distance = Math.sqrt(powDistance);
          double sinA = offsetX / distance;
          double cosA = offsetY / distance;

          double scaleFactor = distance / radius - 1;
          scaleFactor = (1 - scaleFactor * scaleFactor * (distance / radius) * strength);

          distance = distance * scaleFactor;
          disY = (int) (distance * cosA + centerPoint.y + 0.5);
          disY = checkY(disY, bitmap);
          disX = (int) (distance * sinA + centerPoint.x + 0.5);
          disX = checkX(disX, bitmap);
          //the central point is not handled
          if (!(j == centerPoint.x && i == centerPoint.y)) {
            resultBitmap.setPixel(j, i, bitmap.getPixel(disX, disY));
          }
        }
      }
    }
    return resultBitmap;
  }

  private static int checkY(int disY, Bitmap bitmap) {
    if (disY < 0) {
      disY = 0;
    } else if (disY >= bitmap.getHeight()) {
      disY = bitmap.getHeight() - 1;
    }
    return disY;
  }

  private static int checkX(int disX, Bitmap bitmap) {
    if (disX < 0) {
      disX = 0;
    } else if (disX >= bitmap.getWidth()) {
      disX = bitmap.getWidth() - 1;
    }
    return disX;
  }

  public static Bitmap getScaleBitmap(String filepath) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 2;
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(filepath);
  }

  private static final int WIDTH = 200;
  private static final int HEIGHT = 200;

  /**
   * face slim algorithm
   *
   * @param bitmap         original bitmap
   * @param leftFacePoint  the list of left face
   * @param rightFacePoint the list of right face
   * @param level          level
   */
  public static Bitmap slimFace(Bitmap bitmap, List<MLPosition> leftFacePoint, List<MLPosition> rightFacePoint, int level) {
    if (bitmap == null) {
      return null;
    }
    if (leftFacePoint == null || leftFacePoint.size() <= 0) {
      return null;
    }
    if (rightFacePoint == null || rightFacePoint.size() <= 0) {
      return null;
    }
    //the number of intersection coordinates
    int count = (WIDTH + 1) * (HEIGHT + 1);

    //coordinates used to save count
    float[] verts = new float[count * 2];


    float bmWidth = bitmap.getWidth();
    float bmHeight = bitmap.getHeight();

    int index = 0;
    for (int i = 0; i < HEIGHT + 1; i++) {
      float fy = bmHeight * i / HEIGHT;
      for (int j = 0; j < WIDTH + 1; j++) {
        float fx = bmWidth * j / WIDTH;
        //the x-axis coordinates are placed on the even number
        verts[index * 2] = fx;
        //the y-axis coordinates are placed in odd digits位
        verts[index * 2 + 1] = fy;
        index += 1;
      }
    }
    int r = 90 + 15 * level;
    warp(verts, leftFacePoint.get(4).getX(), leftFacePoint.get(4).getY(), (leftFacePoint.get(4).getX() + rightFacePoint.get(4).getX()) / 2, leftFacePoint.get(4).getY(), r);
    warp(verts, leftFacePoint.get(6).getX(), leftFacePoint.get(6).getY(), (leftFacePoint.get(6).getX() + rightFacePoint.get(6).getX()) / 2, leftFacePoint.get(6).getY(), r);
    warp(verts, leftFacePoint.get(8).getX(), leftFacePoint.get(8).getY(), (leftFacePoint.get(8).getX() + rightFacePoint.get(8).getX()) / 2, leftFacePoint.get(8).getY(), r);
    warp(verts, leftFacePoint.get(10).getX(), leftFacePoint.get(10).getY(), (leftFacePoint.get(10).getX() + rightFacePoint.get(10).getX()) / 2, leftFacePoint.get(10).getY(), r);
    warp(verts, leftFacePoint.get(12).getX(), leftFacePoint.get(12).getY(), (leftFacePoint.get(12).getX() + rightFacePoint.get(12).getX()) / 2, leftFacePoint.get(12).getY(), r);
    warp(verts, leftFacePoint.get(14).getX(), leftFacePoint.get(14).getY(), (leftFacePoint.get(14).getX() + rightFacePoint.get(14).getX()) / 2, leftFacePoint.get(14).getY(), r);
    warp(verts, leftFacePoint.get(16).getX(), leftFacePoint.get(16).getY(), (leftFacePoint.get(16).getX() + rightFacePoint.get(16).getX()) / 2, leftFacePoint.get(16).getY(), r);

    warp(verts, rightFacePoint.get(4).getX(), rightFacePoint.get(4).getY(), (leftFacePoint.get(4).getX() + rightFacePoint.get(4).getX()) / 2, rightFacePoint.get(4).getY(), r);
    warp(verts, rightFacePoint.get(6).getX(), rightFacePoint.get(6).getY(), (leftFacePoint.get(6).getX() + rightFacePoint.get(6).getX()) / 2, rightFacePoint.get(6).getY(), r);
    warp(verts, rightFacePoint.get(8).getX(), rightFacePoint.get(8).getY(), (leftFacePoint.get(8).getX() + rightFacePoint.get(8).getX()) / 2, rightFacePoint.get(8).getY(), r);
    warp(verts, rightFacePoint.get(10).getX(), rightFacePoint.get(10).getY(), (leftFacePoint.get(10).getX() + rightFacePoint.get(10).getX()) / 2, rightFacePoint.get(10).getY(), r);
    warp(verts, rightFacePoint.get(12).getX(), rightFacePoint.get(12).getY(), (leftFacePoint.get(12).getX() + rightFacePoint.get(12).getX()) / 2, rightFacePoint.get(12).getY(), r);
    warp(verts, rightFacePoint.get(14).getX(), rightFacePoint.get(14).getY(), (leftFacePoint.get(14).getX() + rightFacePoint.get(14).getX()) / 2, rightFacePoint.get(14).getY(), r);
    warp(verts, rightFacePoint.get(16).getX(), rightFacePoint.get(16).getY(), (leftFacePoint.get(16).getX() + rightFacePoint.get(16).getX()) / 2, rightFacePoint.get(16).getY(), r);

    Bitmap resultBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(resultBitmap);
    canvas.drawBitmapMesh(bitmap, WIDTH, HEIGHT, verts, 0, null, 0, null);
    return resultBitmap;
  }

  private static void warp(float verts[], float startX, float startY, float endX, float endY, int r) {
    //calculate drag distance
    float ddPull = (endX - startX) * (endX - startX) + (endY - startY) * (endY - startY);
    float dPull = (float) Math.sqrt(ddPull);
    //dPull = screenWidth - dPull >= 0.0001f ? screenWidth - dPull : 0.0001f;
    if (dPull < 2 * r) {
      dPull = 2 * r;
    }

    int powR = r * r;
    int index = 0;
    int offset = 1;
    for (int i = 0; i < HEIGHT + 1; i++) {
      for (int j = 0; j < WIDTH + 1; j++) {
        //the boundary area is not treated
        if (i < offset || i > HEIGHT - offset || j < offset || j > WIDTH - offset) {
          index = index + 1;
          continue;
        }
        //calculate the distance between each coordinate point and the touch point
        float dx = verts[index * 2] - startX;
        float dy = verts[index * 2 + 1] - startY;
        float dd = dx * dx + dy * dy;

        if (dd < powR) {
          //twist
          double e = (powR - dd) * (powR - dd) / ((powR - dd + dPull * dPull) * (powR - dd + dPull * dPull));
          double pullX = e * (endX - startX);
          double pullY = e * (endY - startY);
          verts[index * 2] = (float) (verts[index * 2] + pullX);
          verts[index * 2 + 1] = (float) (verts[index * 2 + 1] + pullY);
        }
        index = index + 1;
      }
    }
  }



  public static Bitmap scaleBitmap(Bitmap origin, float ratio) {
    if (origin == null) {
      return null;
    }
    int width = origin.getWidth();
    int height = origin.getHeight();
    Matrix matrix = new Matrix();
    matrix.preScale(ratio, ratio);
    Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
    if (newBM.equals(origin)) {
      return newBM;
    }
    origin.recycle();
    return newBM;
  }

  public static int readPictureDegree(String path) {
    int degree = 0;
    try {
      ExifInterface exifInterface = new ExifInterface(path);
      int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          degree = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          degree = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          degree = 270;
          break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return degree;
  }
  public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
    Bitmap returnBm = null;
    // 根据旋转角度，生成旋转矩阵
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    try {
      // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
      returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    } catch (OutOfMemoryError e) {
    }
    if (returnBm == null) {
      returnBm = bitmap;
    }
    if (bitmap != returnBm) {
      bitmap.recycle();
    }
    return returnBm;
  }
  public static Bitmap getCompressPhoto(String path) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = false;
    options.inSampleSize = 10; // 图片的大小设置为原来的十分之一
    Bitmap bmp = BitmapFactory.decodeFile(path, options);
    options = null;
    return bmp;
  }

  public static String savePhotoToSD(Bitmap mbitmap, String path) {
    FileOutputStream outStream = null;
    String fileName = path;
    try {
      outStream = new FileOutputStream(fileName);
      // 把数据写入文件，100表示不压缩
      mbitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
      return fileName;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      try {
        if (outStream != null) {
          // 记得要关闭流！
          outStream.close();
        }
        if (mbitmap != null) {
          mbitmap.recycle();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}