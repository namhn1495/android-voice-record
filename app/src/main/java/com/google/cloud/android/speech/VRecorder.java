package com.google.cloud.android.speech;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import service.StreamVoiceGrpc;
import service.TextReply;
import service.VoiceRequest;

/**
 * Created by loda on 21/03/2018.
 */

public class VRecorder {
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int bufferSize;
    private BlockingQueue<VoiceRequest> inputQueue;
    private String host = "203.113.152.90";
    private int port = 8125;
//    private int port = 80;
    int BufferElements2Rec; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    private static final String TAG = "VRecorder";
    private BlockingQueue<TextReply> outputQueue = new LinkedBlockingQueue<>();
    ManagedChannel channel;
    StreamVoiceGrpc.StreamVoiceStub asyncStub;
    VoiceListener listener;
    Context context;
    public VRecorder(Context context) {
        this.context = context;

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
//        final int sizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            // For some readon we couldn't obtain a buffer size
            bufferSize = RECORDER_SAMPLERATE * 4 * 2;
        }
        BufferElements2Rec = bufferSize / BytesPerElement;
        Log.e(TAG, "VRecorder: buffer size" + bufferSize);
        Log.e(TAG, "VRecorder: buffer element" + BufferElements2Rec);
//        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        asyncStub = StreamVoiceGrpc.newStub(channel);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Log.e(TAG, "initRecorder: current "+am.getStreamVolume(AudioManager.STREAM_MUSIC));
        Log.e(TAG, "initRecorder: max "+am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    }

    public void addListener(VoiceListener listener){
        this.listener = listener;
    }

    private void initRecorder() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
//        recorder = new AudioRecord.Builder()
//                .setAudioFormat(new AudioFormat.Builder()
//                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                        .setSampleRate(RECORDER_SAMPLERATE)
//                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
//                        .build())
//                .setAudioSource(MediaRecorder.AudioSource.MIC)
//                .setBufferSizeInBytes(bufferSize)
//                .build();
    }


    public void startRecording() {
        if (recorder == null) {
            initRecorder();
        }
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                processBytes();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
        listener.onStart();
//        getDataThread = new Thread() {
//            @Override
//            public void run() {
//                while (isRecording) {
//                    TextReply textReply = poll_voice_response(10);
//                    if (textReply != null)
//                        Log.e(TAG, "on Data Thread: " + textReply.getText());
//                    else{
//                        Log.e(TAG, "on Data Thread: null ");
//
//                    }
//                }
//            }
//        };
//        getDataThread.start();
    }

    //    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private CountDownLatch processBytes() {
        // Write the output audio in byte

        short bData[] = new short[BufferElements2Rec];


        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<TextReply> responseObserver = new StreamObserver<TextReply>() {

            @Override
            public void onNext(TextReply asr_response) {

//                if (asr_response == null) {
//                    Log.e(TAG, "onNext: null");
//
//                };
                listener.onReconize(asr_response);
//                if (asr_response.getIsEOS()) {
//                    Log.e(TAG, "on Next: EOS");
//                }
//                if (asr_response.getIsFinalText()) {
//                    Log.e(TAG, "on Next: finalText: " + asr_response.getText());
//                    stopRecording();
//                } else if (asr_response.getText().length() > 0) {
//                    Log.e(TAG, "on Next: " + asr_response.getText());
////                    Message msg = handler.obtainMessage();
////                    msg.obj = asr_response.getText();
////                    handler.sendMessage(msg);
//
////                    try {
////                        outputQueue.put(asr_response);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                }
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                Log.e(TAG, "onError: sendvoice fail+ " + status);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                Log.e(TAG, "onCompleted");
                finishLatch.countDown();

            }
        };

        StreamObserver<VoiceRequest> requestObserver = asyncStub.sendVoice(responseObserver);

        boolean isStart = true;
        boolean isStartingSpeak = false;
        byte temp[];
        byte cache[] = new byte[0];
        while (isRecording) {
            // gets the voice output from microphone to byte format
            if (recorder.read(bData, 0, BufferElements2Rec) != -1) {
                temp = short2byte(bData);
                if (!isStartingSpeak) {
                    if (isHearingVoice(temp, temp.length)) {
                        isStartingSpeak = true;
                        Log.e(TAG, "processBytes: Starting Speak");
                    }
                }
                if (isStartingSpeak) {
                    cache = Bytes.concat(cache, temp);
                    if (cache.length == 8960) {
                        if (isStart) {
                            requestObserver.onNext(VoiceRequest.newBuilder().setByteBuff(ByteString.copyFrom(cache)).setIsStart(true).build());
                            isStart = false;
                        } else {
                            requestObserver.onNext(VoiceRequest.newBuilder().setByteBuff(ByteString.copyFrom(cache)).build());
                        }
                        cache = new byte[0];
//                        Log.e(TAG, "processBytes: Sending: 8960");
                    }
                }

//                Log.e(TAG, "processBytes: isHearing: " + isHearingVoice(bData, bData.length));
            }
//            Log.e(TAG, "record: " + bufferSize);
//            try {
//                Thread.sleep(250);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        requestObserver.onNext(VoiceRequest.newBuilder().setIsEnd(true).build());
        requestObserver.onCompleted();

        Log.e(TAG, "completed!");
        return finishLatch;
    }

    public void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
//            getDataThread = null;
            listener.onStop();
        }
    }


    private boolean isHearingVoice(byte[] buffer, int size) {
        for (int i = 0; i < size - 1; i += 2) {
            // The buffer has LINEAR16 in little endian.
            int s = buffer[i + 1];
            if (s < 0) s *= -1;
            s <<= 8;
            s += Math.abs(buffer[i]);
            if (s > 1500) {
                return true;
            }
        }
        return false;
    }



    public static  interface VoiceListener {
        public void onStart();
        public void onStop();
        public void onReconize(TextReply textReply);
    }


}
