package com.ocreaderindia.ocreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity {

    @Override

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("up","created");
                File photoFile = dispatchTakePictureIntent();
                //new UploadToServer().execute(photoFile);
            }
        });
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    public File dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                //Dothis
                       }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //File photoBinary = null;
                //photoBinary = convertToBinary(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                Log.i("pic","taken!");

                return photoFile;
            }
        }
        return null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent takePictureIntent) {
        if (requestCode == 1) {
            Bitmap photo = (Bitmap) takePictureIntent.getExtras().get("data");
            ImageView test = (ImageView) findViewById(R.id.imageView);
            test.setImageBitmap(photo);

            try {
                FileOutputStream out = new FileOutputStream("filename");
                photo.compress(Bitmap.CompressFormat.JPEG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    String mCurrentPhotoPath;
    String imageFileName;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        imageFileName = imageFileName + ".jpg";

        Log.i("path", mCurrentPhotoPath);
        Log.i("fname",imageFileName);
        return image;
    }

}
