package com.example.qrcodeapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Button generate;
    private ImageView download,share,scan;
    private EditText mytext;
    private ImageView qr_code;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        generate = findViewById(R.id.generate);
        scan = findViewById(R.id.scan);
        mytext = findViewById(R.id.text);
        qr_code = findViewById(R.id.qrcode);
        download = findViewById(R.id.download);
        share = findViewById(R.id.share);

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateQRCode();
            }

            private void generateQRCode() {
                String text = mytext.getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        Bitmap bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 500, 500);
                        qr_code.setImageBitmap(bitmap);
                    } catch (WriterException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error generating QR code", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter text to generate QR code", Toast.LENGTH_SHORT).show();
                }
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanQRCode();
            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadQRCode();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareQRCode();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanQRCode();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scanQRCode() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 2);
            return;
        }

        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(false); // Allow both portrait and landscape orientations
        integrator.setPrompt("Scanning");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    private void downloadQRCode() {
        qr_code.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(qr_code.getDrawingCache());
        qr_code.setDrawingCacheEnabled(false);

        if (bitmap != null) {
            try {
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File qrCodeFile = new File(directory, "QRCode.png");

                FileOutputStream outputStream = new FileOutputStream(qrCodeFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();

                Toast.makeText(MainActivity.this, "QR code saved to Pictures folder", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No QR code to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        qr_code.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(qr_code.getDrawingCache());
        qr_code.setDrawingCacheEnabled(false);

        if (bitmap != null) {
            try {
                File cachePath = new File(getCacheDir(), "images");
                if (!cachePath.exists()) {
                    cachePath.mkdirs();
                }
                File qrCodeFile = new File(cachePath, "QRCode.png");

                FileOutputStream outputStream = new FileOutputStream(qrCodeFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, "com.example.qrcodeapplication.fileprovider", qrCodeFile));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share QR code"));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No QR code to share", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String scannedData = result.getContents();
            if (scannedData != null && !scannedData.isEmpty()) {
                showScannedDataDialog(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showScannedDataDialog(final String scannedData) {
        if (isURL(scannedData)) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(scannedData));
            startActivity(browserIntent);
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Scan Result")
                    .setMessage(scannedData)
                    .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clipData = ClipData.newPlainText("Scanned Data", scannedData);
                            clipboardManager.setPrimaryClip(clipData);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .show();
        }
    }

    private boolean isURL(String text) {
        return Patterns.WEB_URL.matcher(text).matches();
    }
}
