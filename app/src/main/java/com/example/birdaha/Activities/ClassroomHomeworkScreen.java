package com.example.birdaha.Activities;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.birdaha.Adapters.HomeworkAdapter;
import com.example.birdaha.Classrooms.Classroom;
import com.example.birdaha.General.HomeworksTeacher;
import com.example.birdaha.General.HwModel;
import com.example.birdaha.General.StudentModel;
import com.example.birdaha.General.UpdateRespond;
import com.example.birdaha.R;
import com.example.birdaha.Utilities.ClassroomHomeworkViewInterface;

import com.example.birdaha.Users.Teacher;
import com.google.gson.Gson;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import retrofit2.http.Path;

public class ClassroomHomeworkScreen extends AppCompatActivity implements ClassroomHomeworkViewInterface {

    interface AddHomework{
        @GET("/api/v1/teacher/homeworks/{classroomId}")
        Call<HomeworksTeacher> getHomeworks(@Path("classroomId") int classroomId);
        @POST("/api/v1/homework/add")
        Call<UpdateRespond> addHomework(@Body HwModel hwmodel);

        @POST("/api/v1/homework/update")
        Call<UpdateRespond> updateHomework(@Body HwModel hwModel);
    }
    SearchView search;

    List<HwModel> hwModels = new ArrayList<>();

    private ArrayList<HwModel> expiredHws;
    private ArrayList<HwModel> ongoingHws;
    private ArrayList<StudentModel> students;
    private RecyclerView recyclerView;
    private Context context;
    private ClassroomHomeworkViewInterface homeworkViewInterface;

    Button addingHomeworkButton;
    Button gradeButton;
    private ImageView homeworkImage;

    private String image;

    private HomeworkAdapter homeworkAdapter;

    private AlertDialog filterDialog = null;


    private ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if(result != null){
                    Uri imageUri = result;
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        homeworkImage.setImageBitmap(bitmap);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        image = Base64.encodeToString(byteArray,Base64.DEFAULT);
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_homework_screen);

        recyclerView = findViewById(R.id.hwRecyclerView_classroom);
        expiredHws = new ArrayList<>();
        ongoingHws = new ArrayList<>();

        context = this;
        homeworkViewInterface = this;

        search = findViewById(R.id.searchView_homework);

        addingHomeworkButton = findViewById(R.id.adding_hw_btn);
        gradeButton = findViewById(R.id.grade_btn);

        Classroom classroom = null;

        Intent intent = getIntent();
        if(intent != null){
            classroom = (Classroom) intent.getSerializableExtra("classroom");
            //hwModels = (ArrayList<HwModel>) intent.getSerializableExtra("homeworks");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://sinifdoktoruadmin.online/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        if(classroom != null){
            AddHomework getHomework = retrofit.create(AddHomework.class);
            getHomework.getHomeworks(classroom.getClassroom_id()).enqueue(new Callback<HomeworksTeacher>() {
                @Override
                public void onResponse(Call<HomeworksTeacher> call, Response<HomeworksTeacher> response) {
                    if(response.isSuccessful() && response.body() != null){
                        HomeworksTeacher models = response.body();
                        hwModels = models.getHomeworks();
                        students = models.getStudents();
                        for(HwModel o : hwModels)
                        {

                            LocalDate today = LocalDate.now();
                            LocalDate localDate = LocalDate.parse(o.getDue_date());

                            if(localDate.isBefore(today))
                                expiredHws.add(o);
                            else
                                ongoingHws.add(o);
                        }

                        sortListByDate(hwModels);

                        homeworkAdapter = new HomeworkAdapter(context,(ArrayList<HwModel>) hwModels, homeworkViewInterface);
                        recyclerView.setAdapter(homeworkAdapter);
                        Toast.makeText(ClassroomHomeworkScreen.this, "Ödevler Listeleniyor", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(ClassroomHomeworkScreen.this, "Response Unsuccessful", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<HomeworksTeacher> call, Throwable t) {
                    Toast.makeText(ClassroomHomeworkScreen.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("Fail",t.getMessage());
                }
            });
        }

        // Set the layout manager for the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Find a view for filtering homework items and set a click listener
        View baselineFilterView = findViewById(R.id.filterView);
        baselineFilterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverlay(); // Call the showOverlay() method when clicked
            }
        });



        addingHomeworkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAssignmentDialog();
            }
        });


        // Set a listener for the SearchView to handle query text changes
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                homeworkAdapter.getFilter().filter(newText);
                return true;
            }
        });

        // Set a listener for closing the SearchView
        search.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return false;
            }
        });

        addingHomeworkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAssignmentDialog();
            }
        });

        gradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int homeworkId = 2280;
                Classroom classroom = (Classroom) intent.getSerializableExtra("classroom");
                Intent homeworkGradeIntent = new Intent(ClassroomHomeworkScreen.this, HomeworkStudentsScreen.class);

                homeworkGradeIntent.putExtra("students", (Serializable) students);
                homeworkGradeIntent.putExtra("classroom", classroom);
                homeworkGradeIntent.putExtra("homeworkId", homeworkId);
                startActivity(homeworkGradeIntent);

            }
        });


        // Set a listener for the SearchView to handle query text changes
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                homeworkAdapter.getFilter().filter(newText);
                return true;
            }
        });

        // Set a listener for closing the SearchView
        search.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return false;
            }
        });

    }

    private void showAddAssignmentDialog() {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ödev Ekleme"); // Set the dialog title

        // Inflate the layout for the add homework form
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_homework, null);
        builder.setView(dialogView);
        homeworkImage = dialogView.findViewById(R.id.homework_image);

        // Find the select image button
        Button selectImageButton = dialogView.findViewById(R.id.selectImageButton);



        // Set a click listener for the "Select Image" button
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryLauncher.launch("image/*");
            }
        });


        // Find views in the dialog layout
        EditText hw_Name = dialogView.findViewById(R.id.lectureNameEditText);
        //EditText assignmentDescriptionEditText = dialogView.findViewById(R.id.add_announcement_teacher_name);
        EditText hw_dueDate = dialogView.findViewById(R.id.hw_deadline_content);
        EditText hw_content = dialogView.findViewById(R.id.hw_content_content);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Create the dialog
        final AlertDialog dialog = builder.create();

        // Set a click listener for the "Save" button in the dialog
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text entered in the title and description EditText fields
                String hw_name = hw_Name.getText().toString();
                String hw_date = hw_dueDate.getText().toString();
                String hw_info = hw_content.getText().toString();


                Intent intent = getIntent();
                if(intent != null){
                    Teacher teacher = (Teacher) intent.getSerializableExtra("teacher");
                    Classroom classroom = (Classroom) intent.getSerializableExtra("classroom");
                    HwModel hwModel = new HwModel(classroom.getClassroom_id(),teacher.getTeacher_id(),teacher.getCourse().getName(),hw_date,hw_name,hw_info,image);
                    hwModel.setGetImage(image);

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("http://sinifdoktoruadmin.online/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    AddHomework addHomework = retrofit.create(AddHomework.class);
                    addHomework.addHomework(hwModel).enqueue(new Callback<UpdateRespond>() {
                        @Override
                        public void onResponse(Call<UpdateRespond> call, Response<UpdateRespond> response) {
                            if(response.isSuccessful() && response.body() != null){
                                hwModels.add(hwModel);

                                for(HwModel o : hwModels)
                                {
                                    LocalDateTime today = LocalDateTime.now();
                                    LocalDateTime localDate = LocalDate.parse(o.getDue_date()).atStartOfDay();

                                    if(localDate.isAfter(today))
                                        ongoingHws.add(o);
                                    else
                                        expiredHws.add(o);
                                }

                                sortListByDate(hwModels);

                                homeworkAdapter.notifyDataSetChanged();
                                Toast.makeText(ClassroomHomeworkScreen.this, "Ödev başarıyla eklendi", Toast.LENGTH_SHORT).show();
                                Log.d("Response",new Gson().toJson(response.body()));
                            }
                            else{
                                Log.d("ResponseError",new Gson().toJson(response.body()));
                                Log.d("Response",String.valueOf(response.code()));
                            }
                        }

                        @Override
                        public void onFailure(Call<UpdateRespond> call, Throwable t) {
                            Log.d("Error",t.getMessage());
                        }
                    });
                }

                // Create a new HwModel object with the entered title and description
                //HwModel newHomework = new HwModel();

                // Add the new homework to the hwModels list
                //hwModels.add(newHomework);

                // Notify the adapter that the data set has changed
                //homeworkAdapter.notifyDataSetChanged();

                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }


    /**
     * Displays an overlay dialog for filtering options.
     */
    private void showOverlay() {
        if(filterDialog == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = LayoutInflater.from(this);
            View overlayView = inflater.inflate(R.layout.filter_overlay, null);
            builder.setView(overlayView);

            filterDialog = builder.create();

            // Set the dialog window attributes to make it a small overlay
            WindowManager.LayoutParams layoutParams = filterDialog.getWindow().getAttributes();

            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.TOP | Gravity.CENTER;
            filterDialog.getWindow().setAttributes(layoutParams);

            // Find the checkboxes in the overlay layout

            CheckBox checkBox1 = overlayView.findViewById(R.id.checkBox);
            CheckBox checkBox2 = overlayView.findViewById(R.id.checkBox2);

            checkBox1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    hwModels.clear();

                    if(checkBox2.isChecked())
                        hwModels.addAll(expiredHws);

                    if(isChecked)
                        hwModels.addAll(ongoingHws);


                    if(!isChecked && !checkBox2.isChecked())
                    {
                        hwModels.addAll(expiredHws);
                        hwModels.addAll(ongoingHws);
                    }


                    sortListByDate(hwModels);
                    HomeworkAdapter homeworkAdapter = new HomeworkAdapter(context, hwModels, homeworkViewInterface);
                    recyclerView.setAdapter(homeworkAdapter);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                }
            });

            checkBox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    hwModels.clear();

                    if(checkBox1.isChecked())
                        hwModels.addAll(ongoingHws);

                    if(isChecked)
                        hwModels.addAll(expiredHws);


                    if(!isChecked && !checkBox1.isChecked())
                    {
                        hwModels.addAll(expiredHws);
                        hwModels.addAll(ongoingHws);
                    }

                    sortListByDate(hwModels);
                    HomeworkAdapter homeworkAdapter = new HomeworkAdapter(context, hwModels, homeworkViewInterface);
                    recyclerView.setAdapter(homeworkAdapter);
                    recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                }
            });

        }

        // Add any additional customization or logic to the checkboxes here

        filterDialog.show();
    }

    @Override
    public void onClassroomHomeworkItemClick(HwModel clickedItem, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        LayoutInflater inflater = LayoutInflater.from(view.getContext());

        View overlayView = inflater.inflate(R.layout.overlay_homework_layout, null);
        EditText courseName = overlayView.findViewById(R.id.homework_detail_course_name);
        EditText title = overlayView.findViewById(R.id.homework_detail_title);
        EditText dueDate = overlayView.findViewById(R.id.homework_detail_duedate);
        EditText content = overlayView.findViewById(R.id.homework_detail_content);
        Button editButton = overlayView.findViewById(R.id.editButton);
        Button saveButton = overlayView.findViewById(R.id.saveButton);
        ImageView imageView = overlayView.findViewById(R.id.homework_detail_image);

        courseName.setEnabled(false);
        title.setEnabled(false);
        dueDate.setEnabled(false);
        content.setEnabled(false);

        courseName.setText(clickedItem.getCourse_name());
        title.setText(clickedItem.getTitle());
        dueDate.setText(clickedItem.getDue_date());
        content.setText(clickedItem.getInfo());

        Intent intent = getIntent();
        if(intent != null){
            Teacher currentTeacher = (Teacher) intent.getSerializableExtra("teacher");
            if(currentTeacher.getTeacher_id() != clickedItem.getTeacher_id()){
                editButton.setEnabled(false);
                saveButton.setEnabled(false);
            }
        }

        // If the clickedItem has no image, do not open the full screen view
        if(clickedItem.getGetImage() != null){
            byte[] imageBytes = Base64.decode(clickedItem.getGetImage(), Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(imageBytes,0, imageBytes.length);
            Glide.with(ClassroomHomeworkScreen.this)
                    .load(decodedImage)
                    .into(imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Dialog dialog = new Dialog(ClassroomHomeworkScreen.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    dialog.setContentView(R.layout.dialog_full_screen_image);

                    ImageView fullScreenImage = dialog.findViewById(R.id.fullScreenImageView);
                    fullScreenImage.setImageBitmap(decodedImage);
                    fullScreenImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            });
        }

        editButton.setOnClickListener(v -> {
            // Enable EditTexts to make them editable
            title.setEnabled(true);
            dueDate.setEnabled(true);
            content.setEnabled(true);
            content.requestFocus();
        });

        saveButton.setOnClickListener(v -> {
            int index = hwModels.indexOf(clickedItem);
            // Save the edited text
            String updatedTitle = title.getText().toString();
            String updatedDueDate = dueDate.getText().toString();
            String updatedContent = content.getText().toString();

            clickedItem.setTitle(updatedTitle);
            clickedItem.setDue_date(updatedDueDate);
            clickedItem.setInfo(updatedContent);
            clickedItem.setImage(image);
            clickedItem.setGetImage(image);

            System.out.println("hw id:" + clickedItem.getHomework_id());
            System.out.println("teacher id:" + clickedItem.getTeacher_id());
            System.out.println("classroom id: " + clickedItem.getClassroom_id());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://sinifdoktoruadmin.online/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            AddHomework updateHomework = retrofit.create(AddHomework.class);
            updateHomework.updateHomework(clickedItem).enqueue(new Callback<UpdateRespond>() {
                @Override
                public void onResponse(Call<UpdateRespond> call, Response<UpdateRespond> response) {
                    if(response.isSuccessful() && response.body() != null){
                        Toast.makeText(ClassroomHomeworkScreen.this, "Ödev düzenlendi", Toast.LENGTH_SHORT).show();
                        hwModels.set(index,clickedItem);
                        homeworkAdapter.notifyDataSetChanged();
                    }
                    else{
                        Log.d("ResponseUpdate",new Gson().toJson(response.body()));
                        Log.d("ResponseUpdateCode",String.valueOf(response.code()));
                    }
                }

                @Override
                public void onFailure(Call<UpdateRespond> call, Throwable t) {
                    Log.d("Error",t.getMessage());
                }
            });
            title.setEnabled(false);
            dueDate.setEnabled(false);
            content.setEnabled(false);
        });

        builder.setView(overlayView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sortListByDate(ArrayList<HwModel> list){
        ZoneId turkeyZone = ZoneId.of("Europe/Istanbul");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now(turkeyZone);

        Comparator<HwModel> dateComparator = (date1, date2) -> {
            LocalDate localDate1 = LocalDate.parse(date1.getDue_date(), formatter);
            LocalDate localDate2 = LocalDate.parse(date2.getDue_date(), formatter);

            if (localDate1.isEqual(today)) {
                return -1; // Bugünkü tarihleri en önce sırala
            } else if (localDate2.isEqual(today)) {
                return 1; // Bugünkü tarihleri en önce sırala
            } else if (localDate1.isBefore(today) && localDate2.isBefore(today)) {
                return localDate2.compareTo(localDate1); // Geçmiş tarihleri büyükten küçüğe sırala
            } else if (localDate1.isAfter(today) && localDate2.isAfter(today)) {
                return localDate1.compareTo(localDate2); // Gelecek tarihleri küçükten büyüğe sırala
            } else if (localDate1.isBefore(today) && localDate2.isAfter(today)) {
                return 1; // Geçmiş tarihleri gelecek tarihlerden sonra sırala
            } else {
                return -1; // Gelecek tarihleri geçmiş tarihlerden önce sırala
            }
        };

        Collections.sort(list, dateComparator);
    }

}