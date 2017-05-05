package com.example.aki.ip;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.aki.ip.filters.Filter;
import com.example.aki.ip.filters.NoneFilter;
import com.example.aki.ip.filters.convolution.StrokeEdgesFilter;
import com.example.aki.ip.filters.curve.CrossProcessCurveFilter;
import com.example.aki.ip.filters.curve.PortraCurveFilter;
import com.example.aki.ip.filters.curve.ProviaCurveFilter;
import com.example.aki.ip.filters.curve.VelviaCurveFilter;
import com.example.aki.ip.filters.mixer.RecolorCMVFilter;
import com.example.aki.ip.filters.mixer.RecolorRCFilter;
import com.example.aki.ip.filters.mixer.RecolorRGVFilter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

public class CameraActivity extends ActionBarActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    // Keys for storing the indices of the active filters.
    private static final String STATE_CURVE_FILTER_INDEX =
            "curveFilterIndex";
    private static final String STATE_MIXER_FILTER_INDEX =
            "mixerFilterIndex";
    private static final String STATE_CONVOLUTION_FILTER_INDEX =
            "convolutionFilterIndex";
    // The filters.
    private Filter[] mCurveFilters;
    private Filter[] mMixerFilters;
    private Filter[] mConvolutionFilters;
    // The indices of the active filters.
    private int mCurveFilterIndex;
    private int mMixerFilterIndex;
    private int mConvolutionFilterIndex;
    private static final String TAG =
            CameraActivity.class.getSimpleName();
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";
    private static final int MENU_GROUP_ID_SIZE = 2;
    private int mCameraIndex;
    private int mImageSizeIndex;
    private boolean mIsCameraFrontFacing;
    private int mNumCameras;
    private CameraBridgeViewBase mCameraView;
    private List<Camera.Size> mSupportedImageSizes;
    private boolean mIsPhotoPending;
    private Mat mBgr;
    private boolean mIsMenuLocked;
    private BaseLoaderCallback mLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(final int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV loaded successfully");
                            mCameraView.enableView();
                            mBgr = new Mat();
                            mCurveFilters = new Filter[] {
                                    new NoneFilter(),
                                    new PortraCurveFilter(),
                                    new ProviaCurveFilter(),
                                    new VelviaCurveFilter(),
                                    new CrossProcessCurveFilter()
                            };
                            mMixerFilters = new Filter[] {
                                    new NoneFilter(),
                                    new RecolorRCFilter(),
                                    new RecolorRGVFilter(),
                                    new RecolorCMVFilter()
                            };
                            mConvolutionFilters = new Filter[] {
                                    new NoneFilter(),
                                    new StrokeEdgesFilter()
                            };
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(
                    STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(
                    STATE_IMAGE_SIZE_INDEX, 0);
            mCurveFilterIndex = savedInstanceState.getInt(
                    STATE_CURVE_FILTER_INDEX, 0);
            mMixerFilterIndex = savedInstanceState.getInt(
                    STATE_MIXER_FILTER_INDEX, 0);
            mConvolutionFilterIndex = savedInstanceState.getInt(
                    STATE_CONVOLUTION_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mCurveFilterIndex = 0;
            mMixerFilterIndex = 0;
            mConvolutionFilterIndex = 0;
        }

        final Camera camera;
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.GINGERBREAD) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraIndex, cameraInfo);
            mIsCameraFrontFacing =
                    (cameraInfo.facing ==
                            Camera.CameraInfo.CAMERA_FACING_FRONT);
            mNumCameras = Camera.getNumberOfCameras();
            camera = Camera.open(mCameraIndex);
        } else {
            mIsCameraFrontFacing = false;
            mNumCameras = 1;
            camera = Camera.open();
        }
        final Camera.Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes =
                parameters.getSupportedPreviewSizes();
        final Camera.Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = new JavaCameraView(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);

    }

    public void onSaveInstanceState(Bundle savedInstanceState) {

                savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX,
                mImageSizeIndex);
        savedInstanceState.putInt(STATE_CURVE_FILTER_INDEX,
                mCurveFilterIndex);
        savedInstanceState.putInt(STATE_MIXER_FILTER_INDEX,
                mMixerFilterIndex);
        savedInstanceState.putInt(STATE_CONVOLUTION_FILTER_INDEX,
                mConvolutionFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void recreate () {
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }


    @Override
    public void onPause() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0,
                this, mLoaderCallback);
        mIsMenuLocked = false;
    }
    @Override
    public void onDestroy() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        if (mNumCameras < 2) {

            menu.removeItem(R.id.menu_next_camera);
        }
        int numSupportedImageSizes = mSupportedImageSizes.size();
        if (numSupportedImageSizes > 1) {
            final SubMenu sizeSubMenu = menu.addSubMenu(
                    R.string.menu_image_size);
            for (int i = 0; i < numSupportedImageSizes; i++) {
                final Camera.Size size = mSupportedImageSizes.get(i);
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE,
                        String.format("%dx%d", size.width, size.height));
            }
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mIsMenuLocked) {
            return true;
        }
        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            mImageSizeIndex = item.getItemId();
            recreate();

            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_next_curve_filter:
                mCurveFilterIndex++;
                if (mCurveFilterIndex == mCurveFilters.length) {
                    mCurveFilterIndex = 0;
                }
                return true;
            case R.id.menu_next_mixer_filter:
                mMixerFilterIndex++;
                if (mMixerFilterIndex == mMixerFilters.length) {
                    mMixerFilterIndex = 0;
                }
                return true;
            case R.id.menu_next_convolution_filter:
                mConvolutionFilterIndex++;
                if (mConvolutionFilterIndex ==
                        mConvolutionFilters.length) {
                    mConvolutionFilterIndex = 0;
                }
                return true;
            case R.id.menu_next_camera:
                mIsMenuLocked = true;
                mCameraIndex++;
                if (mCameraIndex == mNumCameras) {
                    mCameraIndex = 0;
                }
                recreate();

                return true;
            case R.id.menu_take_photo:
                mIsMenuLocked = true;
                mIsPhotoPending = true;

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
    }
    @Override
    public void onCameraViewStopped() {
    }
    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();


        mCurveFilters[mCurveFilterIndex].apply(rgba, rgba);
        mMixerFilters[mMixerFilterIndex].apply(rgba, rgba);
        mConvolutionFilters[mConvolutionFilterIndex].apply(rgba, rgba);

        if (mIsPhotoPending) {
            mIsPhotoPending = false;
            takePhoto(rgba);
        }

        if (mIsCameraFrontFacing) {

            Core.flip(rgba, rgba, 1);
        }

        return rgba;
    }

    private void takePhoto(final Mat rgba) {
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator +
                appName;
        final String photoPath = albumPath + File.separator +
                currentTimeMillis + LabActivity.PHOTO_FILE_EXTENSION;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE,
                LabActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " +
                    albumPath);
            onTakePhotoFailed();
            return;
        }

        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }
        Log.d(TAG, "Photo saved successfully to " + photoPath);
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }
            onTakePhotoFailed();
            return;
        }
        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH,
                photoPath);
        startActivity(intent);
    }
    private void onTakePhotoFailed() {
        mIsMenuLocked = false;
        final String errorMessage =
                getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
