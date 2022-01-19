 package com.mallika.photoeditor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();



    }

    //variables to check android permission
    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PERMISSION_COUNT = 2;

    //check all the permission granted by user
    @SuppressLint("NewApi")
    private boolean notPermissions(){
        for(int i=0; i <PERMISSION_COUNT; i++){
            if(checkSelfPermission(PERMISSIONS[i])!=PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;

    }

    static {
        System.loadLibrary("photoEditor");
    }

    private  static native void blackAndWhite(int[] pixels, int width, int height);

    @Override
    protected void onResume(){
        super.onResume();

        //Check each time user gets back to app if permission denied in settings
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && notPermissions()){
            requestPermissions(PERMISSIONS,REQUEST_PERMISSIONS);
        }
    }

    //permission granted results to proceed further step if granted else clearApplication user data
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_PERMISSIONS && grantResults.length>0){
            if(notPermissions()){
                ((ActivityManager) this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                recreate();
            }
        }
    }

    private static final int REQUEST_PICK_IMAGE = 12345;
    private ImageView imageView;

    private void init(){


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        imageView = findViewById(R.id.imageView);

        //Check if device has Camera
        if(!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            findViewById(R.id.takePhotoButton).setVisibility(View.GONE);
        }

        final Button selectImageButton = findViewById(R.id.selectImageButton);
        final Button takePhotoButton = findViewById(R.id.takePhotoButton);

        selectImageButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");

                final Intent picIntent = new Intent(Intent.ACTION_PICK);
                picIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                final Intent chooseIntent = Intent.createChooser(intent,"Select image");
                startActivityForResult(chooseIntent,REQUEST_PICK_IMAGE);

            }
        });

        takePhotoButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(getPackageManager()) != null){
                    //Create file for the photo which was just taken
                    final File photoFile = createImageFile();
                    imageUri = Uri.fromFile(photoFile);
                    final SharedPreferences myPrefs = getSharedPreferences(appID,0);
                    myPrefs.edit().putString("path",photoFile.getAbsolutePath()).apply();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Your camera is not compatible", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final Button blackAndWhite = findViewById(R.id.blackAndWhite);
        blackAndWhite.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                new Thread(){
                    public void run(){
                        blackAndWhite(pixels, width, height);

                        bitmap.setPixels(pixels, 0 , width, 0, 0, width, height);
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });

        final Button saveImage = findViewById(R.id.saveImage);
        saveImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogueClickListener =  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if(i == DialogInterface.BUTTON_POSITIVE){
                            final File outFile = createImageFile();
                            try (FileOutputStream out = new FileOutputStream(outFile)){
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                imageUri = Uri.parse("File://"+outFile);
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE));
                                Toast.makeText(getApplicationContext(),"The Image was saved", Toast.LENGTH_SHORT).show();
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            }


                        }
                    }
                };
                builder.setMessage("Save Current photo to Gallery?").setPositiveButton("Yes",dialogueClickListener)
                        .setNegativeButton("No",dialogueClickListener).show();
            }

        });

        final Button back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener(){
            @Override
                    public void onClick(View view){
                        findViewById(R.id.selectImageButton).setVisibility(View.GONE);
                        findViewById(R.id.saveImage).setVisibility(View.VISIBLE);
                        editMode = false;
            }
        });

    }


    public void OnBackPressed(){
        super.onBackPressed();
        if(editMode){
            findViewById(R.id.selectImageButton).setVisibility(View.GONE);
            findViewById(R.id.saveImage).setVisibility(View.VISIBLE);
            editMode = false;


        }
        else {
            super.onBackPressed();
        }
    }
    private  static int REQUEST_IMAGE_CAPTURE = 1012;
    private Uri imageUri;
    private static final  String appID = "photoEditor";


    private File createImageFile(){
       final String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
       final String imageFileName = "/JPEG_"+timestamp+".jpg";
       final File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
       System.out.println("File Saved as "+storageDir+imageFileName);
       return new File(storageDir+imageFileName);

    }

    private boolean editMode = false;
    private Bitmap bitmap;
    private int width = 0;
    private int height = 0;
    private static final int MAX_PIXEL_COUNT = 2048;
    private int[] pixels;
    private int pixelCount = 0;

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(resultCode != RESULT_OK){
            return;
        }
        if(requestCode == REQUEST_IMAGE_CAPTURE){
            if(imageUri == null){
                final SharedPreferences p = getSharedPreferences(appID,0);
                final String path = p.getString("path","");
                if(path.length()<1){
                    recreate();
                    return;
                }

                imageUri = Uri.parse("File://"+path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageUri));
        }
        else if(data == null){
            recreate();
            return;

        }
        else if(requestCode == REQUEST_PICK_IMAGE){
            imageUri = data.getData();
        }
        final ProgressDialog dialogue = ProgressDialog.show(MainActivity.this,"Loading","Please wait", true);

        editMode = true;
        findViewById(R.id.welcome_screen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);

        new Thread(){
            public void run(){
                bitmap = null;
                final BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
                bmpOptions.inBitmap = bitmap;
                bmpOptions.inJustDecodeBounds = true;
                try(InputStream input = getContentResolver().openInputStream(imageUri)){
                    bitmap = BitmapFactory.decodeStream(input , null, bmpOptions);

                }
                catch (IOException e){
                    e.printStackTrace();
                }

                bmpOptions.inJustDecodeBounds = false;
                width = bmpOptions.outWidth;
                height = bmpOptions.outHeight;

                int resizescale = 1;
                if(width>MAX_PIXEL_COUNT){
                    resizescale = width/MAX_PIXEL_COUNT;
                }
                else if(height>MAX_PIXEL_COUNT){
                    resizescale = height/MAX_PIXEL_COUNT;
                }
                if(width/resizescale > MAX_PIXEL_COUNT || height/resizescale > MAX_PIXEL_COUNT){
                    resizescale++;
                }
                bmpOptions.inSampleSize = resizescale;
                InputStream input = null;
                try{
                    input = getContentResolver().openInputStream(imageUri);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    recreate();
                    return;
                }

                bitmap = BitmapFactory.decodeStream(input,null,bmpOptions);
                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {

                        imageView.setImageBitmap(bitmap);
                        dialogue.cancel();
                    }
                });
                width = bitmap.getWidth();
                height = bitmap.getHeight();
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);

                pixelCount = width * height;
                pixels = new int[pixelCount];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);



            }
        }.start();
        //dialogue.cancel();

    }
}