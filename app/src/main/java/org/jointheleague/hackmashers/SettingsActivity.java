package org.jointheleague.hackmashers;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import java.util.Map;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Switch heatmapSwitch = (Switch) findViewById(R.id.stch);

        heatmapSwitch.setChecked(DataUtil.IS_HEATMAP);

        heatmapSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    DataUtil.IS_HEATMAP = true;
                } else {
                    DataUtil.IS_HEATMAP = false;
                }

            }
        });

        final SeekBar slider = (SeekBar) findViewById(R.id.seekBar);
        slider.setProgress((int)(DataUtil.HEATMAP_OPACITY * 100));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (slider.getId() == seekBar.getId()) {
                    if (fromUser) {
                        float realProgress = progress;
                        DataUtil.HEATMAP_OPACITY = realProgress / 100;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final SeekBar slider2 = (SeekBar) findViewById(R.id.seekBar2);

        slider2.setProgress(DataUtil.HEATMAP_RADIUS);

        slider2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(slider2.getId() == seekBar.getId())
                if (fromUser) {
                    float realProgress = progress;
                    DataUtil.HEATMAP_RADIUS = (int)(realProgress / 5) * 2 + 10;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });



    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Returns to map Activity
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
