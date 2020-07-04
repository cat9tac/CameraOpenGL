package com.example.camera2textureview;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Camera2TextureViewMainActivity extends AppCompatActivity {
    private static final String TAG = "CAM_Camera2Texture";
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private CameraManager mCameraManager;
    private CameraCharacteristics[] mCameraCharacteristics;
    private String[] mCameraIdList;
    private Handler mCameraBackgroudHandler;
    private HandlerThread mCameraBackgroudThread;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private ReentrantLock mLock;
    private SessionConfiguration mSessionConfiguration;
    private SurfaceTexture mSurfaceTexture;
    private List<Surface> mSurfaces;
    private CaptureRequest.Builder mPreviewBuild;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.textureview);
        createBackgroudThead();

    }

    private void createBackgroudThead() {
        if(mCameraBackgroudThread == null) {
            mCameraBackgroudThread = new HandlerThread("cameraBackground");
            mCameraBackgroudThread.start();
            mCameraBackgroudHandler = new Handler(mCameraBackgroudThread.getLooper());
        }
    }

    private void destroyBackgroudThead() {
        mCameraBackgroudThread.quitSafely();
        try {
            mCameraBackgroudThread.join();
            mCameraBackgroudThread = null;
            mCameraBackgroudHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }









    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG,"[opencamera onOpened]");

            mCameraDevice = camera;
            mSurfaces = Arrays.asList(new Surface(mSurfaceTexture));
            try {
                mCameraDevice.createCaptureSession(mSurfaces, mSessionStateCallback,mCameraBackgroudHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG,"[opencamera onDisconnected]");

            //mLock.unlock();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG,"[opencamera onError]");
            //mLock.unlock();
        }
    };

    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG,"[captureSession onConfigured]");
            mCaptureSession = session;
            //mLock.unlock();
            startPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            //mLock.unlock();
        }
    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG,"[onSurfaceTextureAvailable]");
            mSurfaceTexture = surface;
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG,"[onSurfaceTextureSizeChanged]   width:" + width +"  height:" + height);
            mSurfaceTexture = surface;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG,"[onSurfaceTextureDestroyed]");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void prepareCamea(){
        if (shouldRequestCameraPermission()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE);
        }
        Log.d(TAG,"[prepareCamea]");

        if (mCameraManager == null) {
            mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            try {
                mCameraIdList = mCameraManager.getCameraIdList();
            } catch (CameraAccessException e) {
                mCameraIdList = new String[]{"0"};
                e.printStackTrace();
            }

            String idString = "";
            for(String id:mCameraIdList){
                idString = idString+id+", ";
            }
            Log.d(TAG,"[prepareCamea]  mCameraIdList = " + idString);

            try {
                mCameraCharacteristics = new CameraCharacteristics[mCameraIdList.length];
                for (int i = 0; i < mCameraIdList.length; i++) {
                    mCameraCharacteristics[i] = mCameraManager.getCameraCharacteristics(mCameraIdList[i]);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera(){
        prepareCamea();

        try {
            Log.d(TAG,"[openCamera]");
            // mLock.lock();
            Log.d(TAG,"[openCamera]  cameraId:"+mCameraIdList[0]);
            mCameraManager.openCamera(mCameraIdList[0], mCameraDeviceStateCallback, mCameraBackgroudHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        Log.d(TAG,"[starPreview]");
        try {
            mPreviewBuild = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuild.addTarget(new Surface(mSurfaceTexture));
            setUpCaptureRequestBuilder(mPreviewBuild);
            mCaptureSession.setRepeatingRequest(mPreviewBuild.build(),mSessionCaptureCallback,mCameraBackgroudHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder){
        builder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.JPEG_ORIENTATION,90);
    }

    private void closeCamera(){
        try {
            if(mCaptureSession != null) {
                mCaptureSession.abortCaptures();
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if(mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        Log.d(TAG,"[onResume]");
        super.onResume();
        createBackgroudThead();
        if(mTextureView.isAvailable()){
            openCamera();
        }else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }


    }


    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        destroyBackgroudThead();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    //请求权限
    private boolean shouldRequestCameraPermission() {
        if (checkSelfPermission(PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
        }
    }
}