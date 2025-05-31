package com.example.gembos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ShapeableImageView profileImage;
    EditText aboutEditText;
    Button saveButton;
    String selectedImageUri = null;
    private String selectedImagePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profile_image);
        aboutEditText = findViewById(R.id.profile_about);
        saveButton = findViewById(R.id.btn_save_profile);
        TextView nameTextView = findViewById(R.id.profile_name);
        TextView phoneTextView = findViewById(R.id.profile_phone);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());


        dbHelper = new DBHelper(this);


        ProfileModel user = dbHelper.getUserByPhone();
        if (user != null) {
            nameTextView.setText(user.getName() + " " + user.getSurname());
            phoneTextView.setText(user.getPhoneNumber());
            if (user.getAbout() != null) {
                aboutEditText.setText(user.getAbout());
            }
        } else {
            nameTextView.setText("User not found.");
        }

        profileImage.setOnClickListener(v -> openImageChooser());

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String aboutText = aboutEditText.getText().toString();
                String phoneNumber = user.getPhoneNumber();

                dbHelper.updateUserProfile(phoneNumber, aboutText, selectedImagePath);
                Toast.makeText(ProfileActivity.this, "Profile updated.", Toast.LENGTH_SHORT).show();
            }
        });
        String imagePath = user.getProfileImage();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                profileImage.setImageURI(Uri.fromFile(imgFile));
            }
        }



    }



    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Profil Fotoğrafı Seç"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            profileImage.setImageURI(imageUri);

            String savedImagePath = saveImageToInternalStorage(imageUri);
            profileImage.setImageURI(Uri.fromFile(new File(savedImagePath)));
            selectedImagePath = savedImagePath;
        }
    }


    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            File directory = getFilesDir(); // internal app directory
            File file = new File(directory, "profile_image.jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
