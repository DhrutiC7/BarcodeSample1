package com.symbol.barcodesample1;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void showSection(View view) {

        switch (view.getId()) {

            case R.id.production:{
                Intent intent = new Intent(this, Production.class);
                startActivity(intent);
                break;
            }
            case R.id.receive:{
                Intent intent = new Intent(this, Floor.class);
                startActivity(intent);
                break;
            }
        }

    }
}
