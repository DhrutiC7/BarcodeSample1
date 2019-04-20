package com.symbol.barcodesample1;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Floor extends Activity {
    private String activityName = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor);
        activityName =  getIntent().getStringExtra("activity_name");
    }

    public void selectStorage(View view) throws java.lang.ClassNotFoundException {

        switch (view.getId()) {

            case R.id.storage1:{
                Intent intent = new Intent(this, Class.forName(activityName));
//                intent.putExtra("floor_id", "0afd3c3f-ba17-44f7-9833-28cfb2151c6a");
                intent.putExtra("floor_id", "0afd3c3f-ba17-44f7-9833-28cfb2151c6a");//for stage
                startActivity(intent);
                break;
            }
            case R.id.storage2:{
                Intent intent = new Intent(this, Recieve.class);
 //               intent.putExtra("floor_id", "98d7968c-534a-4f73-90b4-4e071cea0812");
                intent.putExtra("floor_id", "98d7968c-534a-4f73-90b4-4e071cea0812");//for stage
                startActivity(intent);
                break;
            }
            case R.id.storage3:{
                Intent intent = new Intent(this, Recieve.class);
                intent.putExtra("floor_id", "82c9f995-89d1-4982-8f92-edc0fa141165");
                startActivity(intent);
                break;
            }
        }

    }
}
