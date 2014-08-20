package net.geekfrikinerd.pixelfromimage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import net.geekfrikinerd.pixelcolorfromimage.R;

public class PickerColorFromImage extends Activity implements OnClickListener {

    private static final String TAG = "COLORPICKER";
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_FILE = 2;

    private TextView colorRGB;
    private ImageView imgSource1;
    private Button btnAccept;

    private int colorInt;
    private int tolerance;
    
    private OnTouchListener imgSourceOnTouchListener
            = new OnTouchListener() {
        Matrix matrix = new Matrix();
        Matrix savedMatrix = new Matrix();

        // We can be in one of these 4 states
        static final int NONE = 0;
        static final int DRAG = 1;
        static final int ZOOM = 2;
        static final int PICK = 3;
        int mode = NONE;

        // Remember some things for zooming
        PointF start = new PointF();
        PointF mid = new PointF();
        float oldDist = 1f;

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            // Handle touch events here...
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    Log.d(TAG, "mode=DRAG");
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    Log.d(TAG, "oldDist=" + oldDist);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                        Log.d(TAG, "mode=ZOOM");
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = PICK;
                    float eventX = event.getX();
                    float eventY = event.getY();
                    float[] eventXY = new float[]{eventX, eventY};

                    Matrix invertMatrix = new Matrix();
                    ((ImageView) view).getImageMatrix().invert(invertMatrix);

                    invertMatrix.mapPoints(eventXY);
                    int x = (int) eventXY[0];
                    int y = (int) eventXY[1];

                    Drawable imgDrawable = ((ImageView) view).getDrawable();
                    Bitmap bitmap = ((BitmapDrawable) imgDrawable).getBitmap();

                    //Limit x, y range within bitmap
                    if (x < 0) {
                        x = 0;
                    } else if (x > bitmap.getWidth() - 1) {
                        x = bitmap.getWidth() - 1;
                    }

                    if (y < 0) {
                        y = 0;
                    } else if (y > bitmap.getHeight() - 1) {
                        y = bitmap.getHeight() - 1;
                    }

                    int touchedRGB = bitmap.getPixel(x, y);
                    colorInt = touchedRGB;
                    colorRGB.setText("touched color: " + "#" + Integer.toHexString(touchedRGB));
                    colorRGB.setTextColor(touchedRGB);
                    Log.d(TAG, "mode=PICK");
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY()
                                - start.y);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        Log.d(TAG, "newDist=" + newDist);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }

            imgSource1.setImageMatrix(matrix);
            return true;
        }

   
    /** Determine the space between the first two fingers */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    /** Calculate the mid point of the first two fingers */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    };
    
    @Override
	public void onClick(View v) {
		Intent data = new Intent();
		data.putExtra("color",colorInt);
		data.putExtra("tolerance",tolerance);
		setResult(RESULT_OK,data);
		finish();
	}
    
    //captured picture uri
    private Uri mImageCaptureUri;
    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.picker_color_layout);

        colorRGB = (TextView) findViewById(R.id.colorrgb);
        imgSource1 = (ImageView) findViewById(R.id.source1);
        btnAccept = (Button) findViewById(R.id.btnAccept);

        imgSource1.setOnTouchListener(imgSourceOnTouchListener);
        btnAccept.setOnClickListener(this);

        captureImageInitialization();
        dialog.show();
    }

    private void captureImageInitialization() {
        /**
         * a selector dialog to display two image source options, from camera
         * ???Take from camera??? and from existing files ???Select from gallery???
         */
        final String[] items = new String[]{"Take from camera",
                "Select from gallery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_item, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select Image");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) { // pick from
                // camera
                if (item == 0) {
                    /**
                     * To take a photo from camera, pass intent action
                     * ???MediaStore.ACTION_IMAGE_CAPTURE??? to open the camera app.
                     */
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    /**
                     * Also specify the Uri to save the image on specified path
                     * and file name. Note that this Uri variable also used by
                     * gallery app to hold the selected image path.
                     */
                    mImageCaptureUri = Uri.fromFile(new File(Environment
                            .getExternalStorageDirectory(), "tmp_avatar_"
                            + String.valueOf(System.currentTimeMillis())
                            + ".jpg"));

                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                            mImageCaptureUri);

                    try {
                        intent.putExtra("return-data", true);

                        startActivityForResult(intent, PICK_FROM_CAMERA);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    // pick from file
                    /**
                     * To select an image from existing files, use
                     * Intent.createChooser to open image chooser. Android will
                     * automatically display a list of supported applications,
                     * such as image gallery or file manager.
                     */
                    Intent intent = new Intent();

                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent,
                            "Complete action using"), PICK_FROM_FILE);
                }
            }
        });

        dialog = builder.create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            dialog.show();
        }else {

            Bitmap photo = null;
            if (requestCode == PICK_FROM_FILE) {
                mImageCaptureUri = data.getData();
                try {
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageCaptureUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (requestCode == PICK_FROM_CAMERA) {
                photo = BitmapFactory.decodeFile(mImageCaptureUri.getPath());
                File f = new File(mImageCaptureUri.getPath());
                //Delete the temporary image
                if (f.exists())
                    f.delete();
            }
            imgSource1.setImageBitmap(photo);
        }

    }


    }
