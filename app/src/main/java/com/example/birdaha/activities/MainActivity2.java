package com.example.birdaha.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.birdaha.R;
import com.example.birdaha.users.Parent;
import com.example.birdaha.users.Student;
import com.example.birdaha.users.Teacher;
import com.example.birdaha.users.User;
import com.google.android.material.button.MaterialButton;

public class MainActivity2 extends AppCompatActivity {
    EditText username;
    EditText password;
    CheckBox checkBox;
    MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        username = (EditText)findViewById(R.id.username);
        password = (EditText)findViewById(R.id.password);
        checkBox = (CheckBox) findViewById(R.id.saveLoginCheckBox);
        loginButton = (MaterialButton) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkEditText()){
                    User user = identifyUser();
                    if(user == null){
                        Toast.makeText(MainActivity2.this, "Invalid username.", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        if(user instanceof Student){
                            Toast.makeText(MainActivity2.this, "Student logged in!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity2.this, StudentProfile.class);
                            intent.putExtra("user",user); // Send the object information to StudentProfile class
                            startActivity(intent);
                        }
                        else if(user instanceof Teacher){
                            Toast.makeText(MainActivity2.this, "Teacher logged in!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity2.this, TeacherProfile.class);
                            intent.putExtra("user",user);
                            startActivity(intent);
                        }
                        else if(user instanceof Parent){
                            Toast.makeText(MainActivity2.this, "Parent logged in!", Toast.LENGTH_SHORT).show();

                        }
                    }
                }
                else{
                    Toast.makeText(MainActivity2.this, "Please enter your username or password!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean checkEditText(){ // Checks whether the edit texts are empty or not
        String name = username.getText().toString();
        String pass = password.getText().toString();

        return !name.isEmpty() && !pass.isEmpty();
    }

    public User identifyUser(){ // Identifies the user from the entered nickname
        String name = username.getText().toString();
        char firstChar = name.charAt(0);
        if(firstChar == 'T'){
            return new Teacher(username.getText().toString(), password.getText().toString());
        }
        else if(firstChar == 'S'){
            return new Student(username.getText().toString(), password.getText().toString());
        }
        else if(firstChar == 'P'){
            return new Parent(username.getText().toString(), password.getText().toString());
        }
        else{
            return null;
        }
    }
}