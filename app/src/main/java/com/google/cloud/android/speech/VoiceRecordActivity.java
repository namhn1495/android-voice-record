/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.speech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import service.TextReply;


public class VoiceRecordActivity extends AppCompatActivity implements MessageDialogFragment.Listener, View.OnClickListener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final String TAG = "MainActivity";

    private VRecorder mVoiceRecorder;
    private final VRecorder.VoiceListener mVoiceCallback = new VRecorder.VoiceListener() {


        @Override
        public void onStart() {

            Log.e(TAG, "onStart: Start record");

        }

        @Override
        public void onStop() {
            Log.e(TAG, "onStop: Stop Recording" );

        }

        @Override
        public void onReconize(final TextReply textReply) {
            if (textReply.getIsFinalText()) {
                Log.e(TAG, "on Next: finalText: " + textReply.getText());
                stopVoiceRecorder();
            }
            if (textReply.getText().length() > 0) {
                Log.e(TAG, "on Next: " + textReply.getText());
                if (mText != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (textReply.getIsFinalText()) {
                                mText.setText(null);
                                mAdapter.addResult(textReply.getText());
                                mRecyclerView.smoothScrollToPosition(0);
                            } else {
                                mText.setText(textReply.getText());
//                                mAdapter.addResult(textReply.getText());
//                                mRecyclerView.smoothScrollToPosition(0);
                            }
                        }
                    });
                }
            }


        }


    };

    // Resource caches

    // View references
    private TextView mText;
    private Button btnRecord;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

//        final Resources resources = getResources();
//        final Resources.Theme theme = getTheme();
//        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
//        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mText = (TextView) findViewById(R.id.text);
        btnRecord = (Button) findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        final ArrayList<String> results = savedInstanceState == null ? null :
                savedInstanceState.getStringArrayList(STATE_RESULTS);
        mAdapter = new ResultAdapter(results);
        mRecyclerView.setAdapter(mAdapter);
        mVoiceRecorder = new VRecorder(this);
        mVoiceRecorder.addListener(mVoiceCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
//            startVoiceRecorder();
            Log.e(TAG, "onStart: granted");

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
            Log.e(TAG, "onStart: granted");

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            Log.e(TAG, "onStart: granted");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVoiceRecorder();
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startVoiceRecorder();
                Log.e(TAG, "onRequestPermissionsResult: granted");
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.voice_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
                Intent intent = new Intent(this,Text2VoiceActivity.class);
                startActivity(intent);

//                mSpeechService.recognizeInputStream(getResources().openRawResource(R.raw.audio));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stopRecording();
        }
        mVoiceRecorder.startRecording();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnRecord.setEnabled(false);
            }
        });
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stopRecording();
//            mVoiceRecorder = null;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnRecord.setEnabled(true);
                }
            });
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }


    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_record:{
                startVoiceRecorder();
            }
            default:{

            }
        }

    }


    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView text;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_result, parent, false));
            text = (TextView) itemView.findViewById(R.id.text);
        }

    }

    private static class ResultAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final ArrayList<String> mResults = new ArrayList<>();

        ResultAdapter(ArrayList<String> results) {
            if (results != null) {
                mResults.addAll(results);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.text.setText(mResults.get(position));
        }

        @Override
        public int getItemCount() {
            return mResults.size();
        }

        void addResult(String result) {
            mResults.add(0, result);
            notifyItemInserted(0);
        }

        public ArrayList<String> getResults() {
            return mResults;
        }

    }

}
