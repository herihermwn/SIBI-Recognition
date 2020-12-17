package org.adarmawan117.recognition.sibi.view.category;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.adarmawan117.recognition.sibi.DetectorActivity;
import org.adarmawan117.recognition.sibi.R;
import org.adarmawan117.recognition.sibi.env.CategoryData;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView backButton;
    private ProgressBar progressBar;
    private RecyclerView categoryList;
    private CategoryListAdapter listHeroAdapter = new CategoryListAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        init();
        getDataFromFirestore();
    }

    private void init() {
        // Initialize variable
        db = FirebaseFirestore.getInstance();
        backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.ProgressBar);
        categoryList = findViewById(R.id.category_list);
        categoryList.setHasFixedSize(true);

        // Set action on click
        backButton.setOnClickListener(v -> backToMain());
    }

    private void getDataFromFirestore() {
        CollectionReference sibi = db.collection("SIBI-Database");
        DocumentReference category = sibi.document("sibi-data");

        // Request data from firestore
        category.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    List<HashMap<String, CategoryData>> categorys = (List<HashMap<String, CategoryData>>) document.get("categorys");

                    ArrayList<CategoryData> categoryList = new ArrayList<>();

                    for (int i = 0; i < categorys.size(); i++) {
                        categoryList.add(new CategoryData(
                                categorys.get(i).values().toArray()[0].toString(),
                                categorys.get(i).values().toArray()[2].toString(),
                                Integer.parseInt(categorys.get(i).values().toArray()[1].toString())
                        ));

                    }

                    showRecyclerList(categoryList);
                }
            }
        });
    }

    private void showRecyclerList(ArrayList<CategoryData> list) {
        // Set data to Adapter
        listHeroAdapter.setListCategory(list);

        // Set Recylerview to view
        categoryList.setLayoutManager(new LinearLayoutManager(this));
        categoryList.setAdapter(listHeroAdapter);

        // Remove progress bar from view
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        backToMain();
    }

    private void backToMain() {
        Intent moveIntent = new Intent(CategoryActivity.this, DetectorActivity.class);
        startActivity(moveIntent);
        finish();
    }

}

