package com.ocreaderindia.ocreader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class UploadToServer extends AsyncTask <String, String, String> {

    TextView messageText;
    int serverResponseCode = 0;
    ProgressDialog dialog = null;

    /**
     * *******  File Path ************
     */

    String upLoadServerUri = "http://10.22.13.194";

    @Override
    protected String doInBackground(String... arg0) {
        //public int doinBackground(String sourceFileUri,String imageFileName) {

        String imageFileName = arg0[0];
        String sourceFileUri = arg0[1];
        final String uploadFileName = imageFileName;
        Log.i("path", sourceFileUri);
        Log.i("name", imageFileName);

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            //Do-something.
            // return 0;
        } else {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);
                Log.i("url", "opened");

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                //dos.writeBytes("Content-Disposition: form-data; name= ; filename= " + fileName + lineEnd);
                dos.writeBytes(("Content-Disposition: form-data; name=\"" + "uploaded_file" + "\"\r\n"));
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

//                    runOnUiThread(new Runnable() {
//                        public void run() {
//
//                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
//                                    +" http://www.androidexample.com/media/uploads/"
//                                    +uploadFileName;
//
//                            messageText.setText(msg);
//                            Toast.makeText(UploadToServer.this, "File Upload Complete.",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });

                    Log.i("code", serverResponseMessage);
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        messageText.setText("MalformedURLException Exception : check script url.");
//                        Toast.makeText(UploadToServer.this, "MalformedURLException",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        messageText.setText("Got Exception : see logcat ");
//                        Toast.makeText(UploadToServer.this, "Got Exception : see logcat ",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }
            dialog.dismiss();
            // return serverResponseCode;

        } // End else blockdev
    return "ok";
    }

    protected void onPostExecute(String result) {

        Log.i("post", "okayy");

    }
}