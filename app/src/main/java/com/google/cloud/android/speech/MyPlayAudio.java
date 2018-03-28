package com.google.cloud.android.speech;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Created by loda on 22/03/2018.
 */

public class MyPlayAudio {
    private final int SAMPLE_RATE = 16000;
    private final int CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
    private int bufferSize;
    private AudioTrack player;
    private MyPlayAudio(){
        bufferSize  = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            // For some readon we couldn't obtain a buffer size
            bufferSize = SAMPLE_RATE * CHANNELS * 2;
        }
        player = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }
    private static MyPlayAudio instance;

    public int getSAMPLE_RATE() {
        return SAMPLE_RATE;
    }

    public int getCHANNELS() {
        return CHANNELS;
    }

    public AudioTrack getPlayer() {

        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
    }

    public static MyPlayAudio getInstance() {
        if(instance == null){
            instance = new MyPlayAudio();
        }
        return instance;
    }
}
