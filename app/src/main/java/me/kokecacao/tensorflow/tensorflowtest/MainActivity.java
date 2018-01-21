package me.kokecacao.tensorflow.tensorflowtest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static int RESULT_LOAD_IMAGE = 1;
    private final static float LOWEST_SCORE = 0.6f;

    public List<String> labels = null;
    public TensorFlowInferenceInterface tfInterface;
    public TFDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check for permission
        checkForPermission();

        //check for permission for camera for OpenCV
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Give first an explanation, if needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }


        Button ballButton = (Button) findViewById(R.id.ballButton);
        ballButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //change here to test
                startActivity(new Intent(MainActivity.this, ColorBlobDetectionActivity.class));
            }
        });

        // create button listener
        Button button = (Button) findViewById(R.id.loadImageButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        //load TensorFlow Files
        detector = new TFDetector("labels.txt", "model.pb", getAssets());
        loadFiles();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            if (!hasPermission()) {
                AlertBox.sendAlert(this, "Alert", "require permission for the picture");
                return;
            }

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            //displace original image
            Bitmap tempImage = BitmapFactory.decodeFile(picturePath);
            ImageView imageView = (ImageView) findViewById(R.id.loadPictureView);
            imageView.setImageBitmap(tempImage);

            //load and process image -> 300X300 image
            Bitmap bitmapInput = Bitmap.createBitmap(detector.getInputSize(), detector.getInputSize(), Bitmap.Config.ARGB_8888);

            final Matrix originToInput = Utils.getImageTransformationMatrix(
                    tempImage.getWidth(), tempImage.getHeight(),detector.getInputSize(), detector.getInputSize(),0, false);
            final Canvas canvas = new Canvas(bitmapInput);
            canvas.drawBitmap(tempImage, originToInput, null);
            final List<TFResult> results = detector.detect(bitmapInput, LOWEST_SCORE);

//            //to a list of bit
//            int[] pixels = new int[300 * 300];
//            bitmapInput.getPixels(pixels, 0, bitmapInput.getWidth(), 0, 0, bitmapInput.getWidth(), bitmapInput.getHeight());
//            byte[] byteInput = new byte[pixels.length * 3];
//            for (int i = 0; i < pixels.length; ++i) {
//                byteInput[i * 3 + 2] = (byte) (pixels[i] & 0xFF);
//                byteInput[i * 3 + 1] = (byte) ((pixels[i] >> 8) & 0xFF);
//                byteInput[i * 3 + 0] = (byte) ((pixels[i] >> 16) & 0xFF);
//            }
//            //feed to 'image_tensor'
//            tfInterface.feed("image_tensor", byteInput, 1, 300, 300, 3);
//            tfInterface.run(new String[]{"detection_boxes", "detection_scores","detection_classes"}, false);
//
//            //preparing outcome
//            float[] boxes = new float[NUMBER_OF_OUTCOMES * 4]; //top left bottom right
//            float[] scores = new float[NUMBER_OF_OUTCOMES];
//            float[] classes = new float[NUMBER_OF_OUTCOMES];
//
//            tfInterface.fetch("detection_boxes", boxes);
//            tfInterface.fetch("detection_scores", scores);
//            tfInterface.fetch("detection_classes", classes);
//
//            //construct recognized image
//            List<TFResult> results = new ArrayList<>();
//            for (int i = 0; i < classes.length; i++) {
//                if (scores[i] > LOWEST_SCORE) {
//                    //left、top、right、bottom <- different from the data we get out of tensorflow
//                    RectF box = new RectF(
//                            boxes[4 * i + 1] * 300,
//                            boxes[4 * i] * 300,
//                            boxes[4 * i + 3] * 300,
//                            boxes[4 * i + 2] * 300);
//                    results.add(new TFResult(labels.get((int) classes[i]), scores[i], box));
//                }
//            }

            //display image
            final Bitmap copiedImage = tempImage.copy(Bitmap.Config.ARGB_8888, true);
            final Canvas resultCanvas = new Canvas(copiedImage);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.0f);
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize((float) (0.04 * copiedImage.getWidth()));

            //inverse matrix to get the right location
            Matrix inputToOrigin = new Matrix();
            originToInput.invert(inputToOrigin);

            //displace
            for (TFResult result : results) {
                RectF box = result.getBox();
                inputToOrigin.mapRect(box); //?
                resultCanvas.drawRect(box, paint);
                resultCanvas.drawText(result.getLabel(), box.left, box.top, textPaint);
            }
            imageView.setImageBitmap(copiedImage);
        }
    }
    public void checkForPermission() {
        if (!hasPermission()) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    RESULT_LOAD_IMAGE);
            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique
            return;
        }
        return;
    }

    public boolean hasPermission() {
        return (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private void loadFiles() {
        //reading files: labels.text
        labels = new ArrayList<>();
        try {
            InputStream labelsStream = getAssets().open("labels.txt");
            BufferedReader buffer = new BufferedReader(new InputStreamReader(labelsStream));
            while(buffer.readLine() != null) {
                labels.add(buffer.readLine());
            }
            buffer.close();
        } catch (IOException e) {
            // cannot read the lable.text
            e.printStackTrace();
        }
        //reading files: model.pb
        tfInterface = new TensorFlowInferenceInterface(getAssets(), "model.pb");
    }
}
