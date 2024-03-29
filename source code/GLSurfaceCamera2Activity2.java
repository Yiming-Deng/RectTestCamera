package com.tencent.xlab.infinixcamera2.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CaptureResult;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lxj.xpopup.XPopup;
import com.tencent.xlab.infinixcamera2.R;
import com.tencent.xlab.infinixcamera2.camera.Camera2Proxy;
import com.tencent.xlab.infinixcamera2.utils.ImageUtil;
import com.tencent.xlab.infinixcamera2.utils.ImageUtils;
import com.tencent.xlab.infinixcamera2.utils.SystemUtil;
import com.tencent.xlab.infinixcamera2.view.Camera2GLSurfaceView;
import com.tencent.xlab.infinixcamera2.view.ExpListDrawerPopupView;
import com.tencent.xlab.infinixcamera2.view.ListDrawerPopupView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tencent.xlab.infinixcamera2.utils.ImageUtils.GALLERY_PATH;

public class GLSurfaceCamera2Activity extends AppCompatActivity
        implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, SensorEventListener {

    private static final String TAG = "GLSurfaceCamera2Act";

    private static final int CHOSE_PHOTO = 1;

    public static boolean saveYUV = false;
    public boolean saveRAW = true;

    private ImageView mCloseIv;
    private ImageView mSwitchCameraIv;
    private ImageView mTakePictureIv;
    private ImageView mPictureIv;
    private ImageView mSetFlashIv;
    private View view_mask_crop;

    private EditText et_count;
    public static TextView tv_iso;
    public static TextView tv_exp;
    private Button btn_iso;
    private Button btn_expTime;
    private RadioGroup rg_DataStyle;
    private Camera2GLSurfaceView mCameraView;

    private Camera2Proxy mCameraProxy;
    public static int flashState = 1;

    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView mTvLight;
    public static float mlight = 0;
    public static String lightmode;

    private ImageUtil imageUtil = new ImageUtil(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurface_camera2);
        mTvLight = findViewById(R.id.tv_light);
        // 获取传感器管理者对象
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // 获取光线传感器对象
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        initView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraProxy.stopPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraProxy.startPreview();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void initView() {
        mCloseIv = findViewById(R.id.toolbar_close_iv);
        mCloseIv.setOnClickListener(this);
        mSwitchCameraIv = findViewById(R.id.toolbar_switch_iv);
        mSwitchCameraIv.setOnClickListener(this);
        mTakePictureIv = findViewById(R.id.take_picture_iv);
        mTakePictureIv.setOnClickListener(this);
        mPictureIv = findViewById(R.id.picture_iv);
        mPictureIv.setOnClickListener(this);
        mSetFlashIv = findViewById(R.id.toolbar_flash_iv);
        mSetFlashIv.setOnClickListener(this);
        mPictureIv.setImageBitmap(ImageUtils.getLatestThumbBitmap());
        mCameraView = findViewById(R.id.camera_view);

        et_count = findViewById(R.id.et_count);
        tv_iso = findViewById(R.id.tv_iso);
        tv_exp = findViewById(R.id.tv_exp);
        mCameraProxy = mCameraView.getCameraProxy();

        btn_iso = findViewById(R.id.btn_iso);
        btn_iso.setOnClickListener(this);
        btn_expTime = findViewById(R.id.btn_expTime);
        btn_expTime.setOnClickListener(this);

        rg_DataStyle = findViewById(R.id.rg_DataStyle);
        rg_DataStyle.setOnCheckedChangeListener(this);

        // View
        view_mask_crop = findViewById(R.id.view_mask);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CHOSE_PHOTO) {
                String selectPath = imageUtil.handleImagePathOnKitKat(this, data);
                if (selectPath != null) {
                    Intent intent = new Intent(this, PhototActivity.class);
                    intent.putExtra("path", selectPath);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "image not loaded...");
                }
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_raw:
                //
                saveYUV = false;
                saveRAW = true;
                break;
            case R.id.rb_yuv:
                //
                saveYUV = true;
                saveRAW = false;
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar_close_iv:
                finish();
                break;
            case R.id.toolbar_switch_iv:
                mCameraProxy.switchCamera(mCameraView.getWidth(), mCameraView.getHeight());
                mCameraProxy.startPreview();
                break;
            case R.id.take_picture_iv:
                /*
                 * if (!TextUtils.isEmpty(et_count.getText())) {
                 * 
                 *//*
                    * //连拍
                    * Log.e(TAG, et_count.getText().toString());
                    * String text = et_count.getText().toString();
                    * 
                    * Pattern p = Pattern.compile("[0-9]*");
                    * Matcher m = p.matcher(text);
                    * if (m.matches()) {
                    * mCameraProxy.setImageAvailableListener(mOnBurstImageAvailableListener);
                    * mCameraProxy.captureImageBurst(Integer.parseInt(text));
                    * }
                    * p = Pattern.compile("[a-zA-Z]");
                    * m = p.matcher(text);
                    * if (m.matches()) {
                    * //输入的是字母
                    * }
                    * p = Pattern.compile("[\u4e00-\u9fa5]");
                    * m = p.matcher(text);
                    * if (m.matches()) {
                    * //输入的是汉字
                    * }
                    *//*
                       * } else {}
                       */
                mCameraProxy.setImageAvailableListener(mOnImageAvailableListener);
                if (!TextUtils.isEmpty(et_count.getText())) {
                    lightmode = String.valueOf(et_count.getText());
                }
                mCameraProxy.captureStillPicture(); // 拍照
                String str = Camera2Proxy.values;

                Toast.makeText(this, str, Toast.LENGTH_SHORT).show();

                break;
            case R.id.picture_iv:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CHOSE_PHOTO);
                break;
            case R.id.toolbar_flash_iv:
                mCameraProxy.setFlash(flashState);// 0:开启；1:关闭
                if (flashState == 0) {
                    flashState = 1;
                } else {
                    flashState = 0;
                }
                break;
            case R.id.btn_iso:
                new XPopup.Builder(GLSurfaceCamera2Activity.this)
                        .asCustom(new ListDrawerPopupView(GLSurfaceCamera2Activity.this, mCameraProxy))
                        .show();
                break;
            case R.id.btn_expTime:
                new XPopup.Builder(GLSurfaceCamera2Activity.this)
                        .asCustom(new ExpListDrawerPopupView(GLSurfaceCamera2Activity.this, mCameraProxy))
                        .show();
                break;
        }
    }

    private ImageReader.OnImageAvailableListener mOnBurstImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            try {
                new ImageSaver(image, mCameraProxy).run();
                CaptureResult captureResult = Camera2Proxy.captureRequests.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            image.close();
        }
    };

    public static int[] getViewLocal(View view) {
        int[] outLocation = new int[2];
        view.getLocationInWindow(outLocation);
        return outLocation;
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            new ImageSaveTask().execute(reader.acquireLatestImage()); // 保存图片
        }
    };

    private ByteBuffer imageToByteBuffer(final Image image) {
        final Rect crop = image.getCropRect();
        final int width = crop.width();
        final int height = crop.height();

        final Image.Plane[] planes = image.getPlanes();
        final byte[] rowData = new byte[planes[0].getRowStride()];
        final int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

        int channelOffset = 0;
        int outputStride = 0;

        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            if (planeIndex == 0) {
                channelOffset = 0;
                outputStride = 1;
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1;
                outputStride = 2;
            } else if (planeIndex == 2) {
                channelOffset = width * height;
                outputStride = 2;
            }

            final ByteBuffer buffer = planes[planeIndex].getBuffer();
            final int rowStride = planes[planeIndex].getRowStride();
            final int pixelStride = planes[planeIndex].getPixelStride();

            final int shift = (planeIndex == 0) ? 0 : 1;
            final int widthShifted = width >> shift;
            final int heightShifted = height >> shift;

            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

            for (int row = 0; row < heightShifted; row++) {
                final int length;

                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted;
                    buffer.get(output.array(), channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (widthShifted - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for (int col = 0; col < widthShifted; col++) {
                        output.array()[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return output;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mlight = event.values[0];
        StringBuffer buffer = new StringBuffer();
        buffer.append(mlight).append("lux");
        mTvLight.setText(buffer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     * @Override
     * public void onSensorChanged(SensorEvent event) {
     * mlight = event.values[0];
     * StringBuffer buffer = new StringBuffer();
     * buffer.append(mlight).append("lux");
     * mTvLight.setText(buffer);
     * }
     * 
     * @Override
     * public void onAccuracyChanged(Sensor sensor, int accuracy) {
     * 
     * }
     */

    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            // 此处预备保存一个txt文件
            // saveYUVData(images[0]);//存储YUV数据
            final ByteBuffer yuvBytes = imageToByteBuffer(images[0]);
            // Convert YUV to RGB
            final RenderScript rs = RenderScript.create(getBaseContext());
            Bitmap bitmap = Bitmap.createBitmap(images[0].getWidth(), images[0].getHeight(), Bitmap.Config.ARGB_8888);
            final Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);
            final Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
            allocationYuv.copyFrom(yuvBytes.array());
            ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
            scriptYuvToRgb.setInput(allocationYuv);
            scriptYuvToRgb.forEach(allocationRgb);
            allocationRgb.copyTo(bitmap);
            Bitmap mybitmap = bitmap;

            // Release

            // Release
            bitmap = rotateBitmap(bitmap, 90);
            Rect rect = null;
            int[] outLocation = getViewLocal(view_mask_crop);
            if (outLocation != null) {
                rect = new Rect(outLocation[0], outLocation[1],
                        view_mask_crop.getMeasuredWidth(),
                        view_mask_crop.getMeasuredHeight());
                // bitmap = scaleBitmap(rect, bitmap);
                float size = (float) (1.0 * getScreenwidth() / bitmap.getWidth());
                Log.e(TAG, "getScreenWidth: " + getScreenwidth() + " bitmap.getWidth(): " + bitmap.getWidth() + " size "
                        + size);
                Log.e(TAG, "Bitmap width: " + bitmap.getWidth() + "height: " + bitmap.getHeight());
                Log.e(TAG, "Screen_Height: " + getScreenHeight() + "Screen_width : " + getScreenwidth());
                Log.e(TAG, "Location[0]: " + outLocation[0] + " Location[1]: " + outLocation[1] + " View_Width: "
                        + view_mask_crop.getMeasuredWidth() + " View_Height: " + view_mask_crop.getMeasuredHeight());
                double scale1 = 1.00 * bitmap.getWidth() / (getScreenwidth() + 500);
                double scale2 = 1.00 * bitmap.getHeight() / (getScreenHeight());
                rect = scalRect(rect, scale1, scale2);
                Log.e(TAG, "rect.left: " + rect.left + " rect.top: " + rect.top + " rect.right:" + rect.right
                        + "  rect.bottom: " + rect.bottom);

                Log.e(TAG, "scale: " + scale1);
                Log.e(TAG, "scale: " + scale2);

            } else {
                et_count.setText("erore");

            }
            bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top - 300, rect.right, rect.bottom + 100, null, true);
            Log.e(TAG, "widht1   " + bitmap.getWidth() + "height1 " + bitmap.getHeight());

            if (ImageUtils.saveBitmap(bitmap)) {
                saveExif(ImageUtils.outFile);
            }
            images[0].close();
            bitmap.recycle();

            allocationYuv.destroy();
            allocationRgb.destroy();
            rs.destroy();
            return ImageUtils.getLatestThumbBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
        }

    }

    public int getScreenwidth() {
        return getDisplayMetrics().widthPixels;
    }

    private DisplayMetrics getDisplayMetrics() {
        return getResources().getDisplayMetrics();

    }

    public int getScreenHeight() {
        return getDisplayMetrics().heightPixels;
    }

    private Rect scalRect(Rect rect, double scale1, double scale2) {
        rect.left = (int) (rect.left * scale1);
        rect.top = (int) (rect.top * scale2);
        rect.right = (int) (rect.right * scale1);
        rect.bottom = (int) (rect.bottom * scale2);
        return rect;

    }

    public Bitmap rotateBitmap(Bitmap bitmap, int degress) {

        if (bitmap != null) {

            Matrix m = new Matrix();

            m.postRotate(degress);

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,

                    true);

            return bitmap;

        }

        return bitmap;

    }

    private void saveExif(File outFile) {
        try {
            ExifInterface exifInterface = new ExifInterface(outFile.getAbsolutePath());
            exifInterface.setAttribute(ExifInterface.TAG_ISO, String.valueOf(mCameraProxy.getIso()));
            double expTime = mCameraProxy.getmExpTime();
            Log.e(TAG, String.valueOf(expTime));
            exifInterface.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, String.valueOf(expTime));
            if (flashState == 0) {
                exifInterface.setAttribute(ExifInterface.TAG_FLASH, "1");
            } else {
                exifInterface.setAttribute(ExifInterface.TAG_FLASH, "0");
            }
            exifInterface.setAttribute(ExifInterface.TAG_MAKE, SystemUtil.getDeviceBrand());
            exifInterface.setAttribute(ExifInterface.TAG_MODEL, SystemUtil.getSystemModel());
            exifInterface.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    @SuppressLint("WrongConstant")
    private void saveYUVData(Image image) {
        String fileName = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".yuv";
        File file = new File(GALLERY_PATH, fileName);
        FileOutputStream output = null;
        ByteBuffer buffer;
        byte[] bytes;
        switch (image.getFormat()) {
            case ImageFormat.YUV_420_888:
                // "prebuffer" simply contains the meta information about the following planes.
                ByteBuffer prebuffer = ByteBuffer.allocate(16);
                prebuffer.putInt(image.getWidth())
                        .putInt(image.getHeight())
                        .putInt(image.getPlanes()[1].getPixelStride())
                        .putInt(image.getPlanes()[1].getRowStride());
                try {
                    output = new FileOutputStream(file);
                    output.write(prebuffer.array()); // write meta information to file
                    // Now write the actual planes.
                    for (int i = 0; i < 3; i++) {
                        buffer = image.getPlanes()[i].getBuffer();
                        bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
                        buffer.get(bytes); // copies image from buffer to byte array
                        output.write(bytes); // write the byte array to file
                    }
                    Log.d("YUV", "saveYUV. filepath: " + file.getAbsolutePath());
                    Log.d("YUV", image.getWidth() + "   height:" + image.getHeight());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // image.close(); // close this to free up buffer for other images
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case ImageFormat.RAW_SENSOR:
                Toast.makeText(GLSurfaceCamera2Activity.this, "RAW_SENSOR", 0).show();
                break;
        }
    }

    private class ImageSaver implements Runnable {
        private final Image mImage;
        private Camera2Proxy mCameraProxy;

        public ImageSaver(Image image, Camera2Proxy cameraProxy) {
            mImage = image;
            mCameraProxy = cameraProxy;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            saveYUVData(mImage);// 存储YUV数据
            final ByteBuffer yuvBytes = imageToByteBuffer(mImage);
            // Convert YUV to RGB
            final RenderScript rs = RenderScript.create(getBaseContext());
            final Bitmap bitmap = Bitmap.createBitmap(mImage.getWidth(), mImage.getHeight(), Bitmap.Config.ARGB_8888);
            final Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);
            final Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
            allocationYuv.copyFrom(yuvBytes.array());
            ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
            scriptYuvToRgb.setInput(allocationYuv);
            scriptYuvToRgb.forEach(allocationRgb);
            allocationRgb.copyTo(bitmap);

            // Release
            if (ImageUtils.saveBitmap(bitmap)) {
                saveExif(ImageUtils.outFile);
            }
            mImage.close();
            bitmap.recycle();
            allocationYuv.destroy();
            allocationRgb.destroy();
            rs.destroy();
        }
    }

}
