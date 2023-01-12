package wut.mini.comicstranslator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private Button mLoadPDF;
    private Button mWriteToken;
    private String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (android.os.Build.VERSION.SDK_INT >= 30) {
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            Intent intentSettings = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
            startActivity(intentSettings);
        }

        Log.d("lis", String.valueOf(getResources().getDisplayMetrics().densityDpi));

        //Loader.load(opencv_java.class)
        OpenCVLoader.initDebug();

        //wpisz token
        mWriteToken = (Button)findViewById(R.id.getToken);
        mWriteToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Wpisz token DeepL");

                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        token = input.getText().toString();
                    }
                });
                builder.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        //wczytanie pdf
        mLoadPDF = (Button)findViewById(R.id.loadPDF);
        mLoadPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    Intent intent = new Intent(MainActivity.this, FileListActivity.class);
                    String path = Environment.getExternalStorageDirectory().getPath();
                    intent.putExtra("path", path);
                    intent.putExtra("token", token);
                    startActivity(intent);
                }else {
                    requestPermission();
                }
            }
        });
    }

    //test menadzera plikow
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        }else
            return false;
    }

    private void requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this, "Nie ma zezwolenia", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);

    }

}