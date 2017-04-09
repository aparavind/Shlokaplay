package com.projects.aravind.shlokaplay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * this is the main activity class.
 */
public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, MediaController.MediaPlayerControl {

    private String shloka_json = "No JSON file selected";

    private final int ACTIVITY_JSON_LOC = 1;

    private JSONArray jArray;

    private int current_shloka = -1;

    private String BaseFolder;

    private MediaPlayer mediaPlayer;

    private MediaController mediaController;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void json_select_click(View view){
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("file/*");
        intent = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(intent, ACTIVITY_JSON_LOC);
    }

    public void load_previous_stage(View view){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String fname = sharedPref.getString(getString(R.string.json_file), null);
        int current_shloka = sharedPref.getInt(getString(R.string.current_shloka),-1);
        restore_info(fname, current_shloka);
        msgbox("The Earlier Settings have been loaded");
    }

    public void save_current_stage(View view){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.json_file), this.shloka_json);
        editor.putInt(getString(R.string.current_shloka),this.current_shloka);
        editor.commit();
        msgbox("The Current settings are saved");
    }


    public void restore_info(String fname,int ShlokaNo){
        TextView tmp;
        fname = fname.replace("/mimetype/", "");
        this.BaseFolder = fname.replaceAll("/[^/]+$", "/");
        this.shloka_json = fname;
        tmp = (TextView) findViewById( R.id.json_file_name);
        tmp.setText(this.shloka_json.replaceAll("^.*/",""));

        File file = new File(this.shloka_json);
        StringBuilder content;
        try {
            BufferedReader fr = new BufferedReader(new FileReader(file));
            content = new StringBuilder();
            String line;
            while ((line = fr.readLine()) != null){
                content.append(line);
                content.append("\n");
            }
            fr.close();
        } catch (java.io.IOException ex){
            msgbox("ERROR FILE " + this.shloka_json + " IS NOT OPENABLE!! and the error is " + ex);
            return;
        }
        try {
            JSONObject jo = new JSONObject(content.toString());
            this.jArray = jo.getJSONArray("Files");
        } catch (org.json.JSONException exc) {
            msgbox("ERROR FILE " + this.shloka_json + " JSON  IS NOT PARSEABLE!!");
            return;
        }

        this.current_shloka = ShlokaNo;
        tmp = (TextView)this.findViewById(R.id.ShlokaNo);
        tmp.setText(this.current_shloka + "/" + this.jArray.length());

        if (this.current_shloka == -1){
            this.current_shloka = 0;
            this.display_html();
            this.current_shloka = -1;
        } else {
            this.display_html();
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) return;
        Uri uri = data.getData();

        String fname = uri.getPath();

        if (requestCode == ACTIVITY_JSON_LOC) {
            restore_info(fname, -1);
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String [] proj      = {MediaStore.Images.Media.DATA};
        Cursor cursor       = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) return null;
        int column_index    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void play_first(View view)  {
        this.current_shloka = 0;
        this.do_all(0);
    }

    public void play_last(View view) {
        this.current_shloka = this.jArray.length() -1;
        this.do_all(0);
    }

    public void play_mp3(int loop){
        int ShlokaNo = this.current_shloka;

        if  (this.mediaPlayer == null) {
            this.mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            this.mediaController = new MediaController_2(this);
            this.mediaController.show(0);
        }
        this.setPreviousNextListener();
        if (mediaPlayer.isPlaying()){
            this.mediaPlayer.stop();
        }
        this.mediaPlayer.reset();
        this.mediaController.show(0);
        JSONObject oneObject;
        String ShlokaMP3Name;
        try {
            oneObject = this.jArray.getJSONObject(ShlokaNo);
        } catch (org.json.JSONException ex){
            msgbox("OHNO Unable to get the JSON content of #" + ShlokaNo);
            return;
        }
        try {
            ShlokaMP3Name = oneObject.getString("mp3");
        } catch (org.json.JSONException ex){
            msgbox("OHNO Unable to Read the content JSON content of #" + ShlokaNo);
            return;
        }
        try {
            this.mediaPlayer.setDataSource(this.BaseFolder + "/" + ShlokaMP3Name);
            this.mediaPlayer.prepare();
        } catch (java.io.IOException ex){
            msgbox(this.BaseFolder + "/" + ShlokaMP3Name + "OHNO Unable to Open Music File of #" + ShlokaNo);
            return;
        }

        if (loop == 1){
            this.mediaPlayer.setLooping(true);
        }

        this.mediaPlayer.start();
    }

    public int display_html(){
        int ShlokaNo = this.current_shloka;
        JSONObject oneObject;
        StringBuilder Shloka = new StringBuilder();
        String ShlokaLine;

        try {
            oneObject = this.jArray.getJSONObject(ShlokaNo);
        } catch (org.json.JSONException ex){
            msgbox("OHNO Unable to get the JSON content of #" + ShlokaNo);
            return 1;    // indicates exit at first level
        }
        // Pulling items from the array
        try {
            String ShlokaFileName = oneObject.getString("File");
            File Shloka_file = new File(this.BaseFolder + "/" + ShlokaFileName);
            try {
                BufferedReader Sr = new BufferedReader(new FileReader(Shloka_file));
                while ((ShlokaLine = Sr.readLine()) != null){
                    Shloka.append(ShlokaLine);
                    Shloka.append("\n");
                }
                Sr.close();
            } catch (FileNotFoundException ex){
                msgbox("OHNO ERROR unable to read the file " + Shloka_file);
                return 2;

            } catch (java.io.IOException ex) {
                msgbox("OHNO ERROR IO exception occurred while reading from file  " + Shloka_file);
                return 3;
            }

        } catch (org.json.JSONException je){
            msgbox("OHNO ERROR in parsing json");
            return 4;
        }

        TextView tmp = (TextView) findViewById(R.id.ShlokaView);
        tmp.setText(Html.fromHtml(Shloka.toString()));
        return 0;
    }

    public void play_next_song(View view)  {

            this.current_shloka++;
            this.do_all(0);
    }

    private void play_previous_song(View view)  {
            this.current_shloka--;
            this.do_all(0);
    }

    private void msgbox(String message) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(false);
        builder.setTitle("AlertDialog Title");
        builder.setMessage(message);
        builder.setPositiveButton("OK!!!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //
            }
        })
                .setNegativeButton("Cancel ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        // Create the AlertDialog object and return it
        builder.create().show();
    }


    private void do_all(int loop){

        if (this.current_shloka == -1){
            this.current_shloka = 0;
        }


        int ShlokaNo = this.current_shloka;
        if (ShlokaNo < 0){
            msgbox("Error in Getting shloka No");
            return;
        }

        if (ShlokaNo >= this.jArray.length()){
            msgbox("Error in Getting shloka No");
            this.current_shloka = this.jArray.length();
            return;
        }

        setShlokaNo();

        play_mp3(loop);
        display_html();

    }

    public void replay(View view){
        do_all(0);
    }

    public void set_continuous(View view){
        do_all(1);
    }

    private void setShlokaNo(){
        TextView  tmp = (TextView)this.findViewById(R.id.ShlokaNo);
        tmp.setText((this.current_shloka + 1) + "/" + this.jArray.length());
    }

    @Override
    public void start() {
        this.mediaPlayer.start();

    }

    @Override
    public void pause() {
        this.mediaPlayer.pause();
        this.mediaController.show(0);

    }

    @Override
    public int getDuration() {
        return this.mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return this.mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        this.mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return this.mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(this.findViewById(R.id.json_file_name));

        handler.post(new Runnable() {
            @Override
            public void run() {
                mediaController.setEnabled(true);
                mediaController.show(0);
            }
        });

    }

    private void setPreviousNextListener(){
        boolean playNext = true;
        boolean playPrevious = true;

        if (this.current_shloka == 0){
            playPrevious = false;
        } else if (this.current_shloka + 1 == this.jArray.length()){
            playNext = false;
        }

        View.OnClickListener pn = null;
        View.OnClickListener pp = null;
        if (playNext) {
            pn = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.play_next_song(v);
                }
            };
        }

        if (playPrevious) {
            pp = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.play_previous_song(v);
                }
            };
        }
        this.mediaController.setPrevNextListeners(pn, pp);
    }

}

