package com.ocreaderindia.ocreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    @Override

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("up", "created");
                selectImage();
            }
        });
    }

    private String selectedImagePath = "";
    final private int PICK_IMAGE = 1;
    final private int CAPTURE_IMAGE = 2;
    private String imgPath;
    public String SERVERURL = "http://10.22.13.194/upload.php";

    private void selectImage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
// builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Take a Photo",
                        "Choose from Gallery"},
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Intent intent1 = new Intent(
                                        MediaStore.ACTION_IMAGE_CAPTURE);
                                intent1.putExtra(MediaStore.EXTRA_OUTPUT,
                                        setImageUri());
                                startActivityForResult(intent1, CAPTURE_IMAGE);
                                break;
                            case 1:
                                // GET IMAGE FROM THE GALLERY
                                Intent intent = new Intent();
                                intent.setType("image/*");
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(
                                        Intent.createChooser(intent, ""),
                                        PICK_IMAGE);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.show();

        //** Send image and offload image processing task  to server by starting async task **
        ServerTask task = new ServerTask();
//        Log.i("TAGpath", imgPath);
        task.execute( Environment.getExternalStorageDirectory().toString() +imgPath);

    }


    public Uri setImageUri() {

        File file = new File(Environment.getExternalStorageDirectory(), "image" + new Date().getTime() + ".png");
        Uri imgUri = Uri.fromFile(file);
        this.imgPath = file.getAbsolutePath();
        return imgUri;

    }


    public String getImagePath() {
        return imgPath;
    }

//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        ImageView imageView = (ImageView) findViewById(R.id.imageView);
//        if (resultCode != Activity.RESULT_CANCELED) {
//            if (requestCode == PICK_IMAGE) {
//                selectedImagePath = getAbsolutePath(data.getData());
//                System.out.println("path" + selectedImagePath);
//                imageView.setImageBitmap(decodeFile(selectedImagePath));
//                new PostDataAsyncTask().execute(selectedImagePath);
//
//            } else if (requestCode == CAPTURE_IMAGE) {
//                selectedImagePath = getImagePath();
//                System.out.println("path" + selectedImagePath);
//                imageView.setImageBitmap(decodeFile(selectedImagePath));
//                new PostDataAsyncTask().execute(selectedImagePath);
//
//
//            } else {
//                super.onActivityResult(requestCode, resultCode, data);
//            }
//        }
//    }


    public Bitmap decodeFile(String path) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            // Find the correct scale value. It should be the power of
            // 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE
                    && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getAbsolutePath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};

        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }


    //*******************************************************************************
    //Push image processing task to server
    //*******************************************************************************

    public class ServerTask extends AsyncTask<String, Integer, Void> {
        public byte[] dataToServer;

        //Task state
        private final int UPLOADING_PHOTO_STATE = 0;
        private final int SERVER_PROC_STATE = 1;

        private ProgressDialog dialog;

        //upload photo to server
        HttpURLConnection uploadPhoto(FileInputStream fileInputStream) {

            final String serverFileName = "test" + (int) Math.round(Math.random() * 1000) + ".jpg";
            final String lineEnd = "\r\n";
            final String twoHyphens = "--";
            final String boundary = "*****";

            try {
                URL url = new URL(SERVERURL);
                // Open a HTTP connection to the URL
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // Allow Inputs
                conn.setDoInput(true);
                // Allow Outputs
                conn.setDoOutput(true);
                // Don't use a cached copy.
                conn.setUseCaches(false);

                // Use a post method.
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + serverFileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of maximum size
                int bytesAvailable = fileInputStream.available();
                int maxBufferSize = 1024;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[] buffer = new byte[bufferSize];

                // read file and write it into form...
                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                publishProgress(SERVER_PROC_STATE);
                // close streams
                fileInputStream.close();
                dos.flush();

                return conn;
            } catch (MalformedURLException ex) {
                Log.e("TAG", "error: " + ex.getMessage(), ex);
                return null;
            } catch (IOException ioe) {
                Log.e("TAG", "error: " + ioe.getMessage(), ioe);
                return null;
            }
        }

        //get image result from server and display it in result view
        void getResultImage(HttpURLConnection conn) {
            // retrieve the response from server
            InputStream is;
            try {
                is = conn.getInputStream();
                //get result image from server

                Log.e("TAGresult", "reply received");
            } catch (IOException e) {
                Log.e("TAG", e.toString());
                e.printStackTrace();
            }
        }

        //Main code for processing image algorithm on the server

        void processImage(String inputImageFilePath) {
            publishProgress(UPLOADING_PHOTO_STATE);
            File inputFile = new File(inputImageFilePath);
            try {

                //create file stream for captured image file
                FileInputStream fileInputStream = new FileInputStream(inputFile);

                //upload photo
                final HttpURLConnection conn = uploadPhoto(fileInputStream);

                //get processed photo from server
                if (conn != null) {
                    getResultImage(conn);
                }
                fileInputStream.close();
            } catch (FileNotFoundException ex) {
                Log.e("TAG", ex.toString());
            } catch (IOException ex) {
                Log.e("TAG", ex.toString());
            }
        }

        protected void onPreExecute() {
//            this.dialog.setMessage("Photo captured");
 //           this.dialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {            //background operation
            String uploadFilePath = params[0];
            processImage(uploadFilePath);
        return null;
        }

//        //progress update, display dialogs
//        @Override
//        protected void onProgressUpdate(Integer... progress) {
//            if (progress[0] == UPLOADING_PHOTO_STATE) {
//                dialog.setMessage("Uploading");
//                dialog.show();
//            } else if (progress[0] == SERVER_PROC_STATE) {
//                if (dialog.isShowing()) {
//                    dialog.dismiss();
//                }
//                dialog.setMessage("Processing");
//                dialog.show();
//            }
//        }

//        @Override
//        protected void onPostExecute(Void param) {
//            if (dialog.isShowing()) {
//                dialog.dismiss();
//            }
//        }
    }
}

