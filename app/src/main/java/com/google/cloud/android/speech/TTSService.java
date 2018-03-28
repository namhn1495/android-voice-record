package com.google.cloud.android.speech;

import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by loda on 22/03/2018.
 */

public class TTSService extends AsyncTask<String, String, InputStream> {


    public TTSService() {
        //set context variables if required
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected InputStream doInBackground(String... g) {
        InputStream result = null;

        try {
            String fileURL = g[0];
            String voiceContent = g[1];
            String personalVoice = g[2];
            String key = g[3];

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("data", voiceContent);
            params.put("voices", personalVoice);
            params.put("key", key);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(param.getKey()/*URLEncoder.encode(param.getKey(), "UTF-8")*/);
                postData.append('=');
                postData.append(String.valueOf(param.getValue())/*URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8")*/);
            }

            byte[] postDataBytes = new byte[0];

            postDataBytes = postData.toString().getBytes("UTF-8");


            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            httpConn.setDoOutput(true);
            httpConn.getOutputStream().write(postDataBytes);

            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // opens input stream from the HTTP connection
                InputStream inputstream = httpConn.getInputStream();
                return inputstream;
            }

            httpConn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;


    }

    @Override
    protected void onPostExecute(final InputStream inputStream) {
        final AudioTrack player = MyPlayAudio.getInstance().getPlayer();

        new Thread(){
            @Override
            public void run() {
                player.play();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                byte buff[] = new byte[8000];
                int k;
                try {
                    while ((k = bufferedInputStream.read(buff)) != -1) {
                        player.write(buff, 0, k);
                    }
                    player.stop();
                    player.release();
                } catch (IOException e) {
                    e.printStackTrace();
                }            }
        }.start();
        Log.e("MainActivity", "onPostExecute: on receive voice" );
    }
    //    private Thread sendTextMess = null;
//    private AudioTrack player;
//    private static final String TAG = "TTSService";
//    private final SendTextMessageGrpc.SendTextMessageStub asyncStub;
//    private final ManagedChannel channel;
//    private int timeout = 10000;
//    private static HashFunction hashFunction = Hashing.md5();
//    private static String dialog_id = hashFunction.hashLong(System.currentTimeMillis()).toString();
//    static String account = "Test_CSKH";
//    private String host = "203.113.152.90";
//    private int port = 8125;
//
//
//
//    public TTSService() {
//        this.channel = ManagedChannelBuilder.forAddress(host, port)
//                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
//                // needing certificates.
//                .usePlaintext(true)
//                .build();
//        asyncStub = SendTextMessageGrpc.newStub(channel);
//    }
//
//    public TTSService(ManagedChannel channel) {
//        this.channel = channel;
//        asyncStub = SendTextMessageGrpc.newStub(channel);
//    }
//
//    public TTSService(String host, int port) {
//        this(ManagedChannelBuilder.forAddress(host, port)
//                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
//                // needing certificates.
//                .usePlaintext(true)
//                .build());
//    }
//
//    public TTSService(String host, int port, int timeout) {
//        this(ManagedChannelBuilder.forAddress(host, port)
//                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
//                // needing certificates.
//                .usePlaintext(true)
//                .build());
//        if (timeout >= 1)
//            this.timeout = timeout;
//        else {
//            System.out.println("Wrong timeout argument, default timeout 10000s is selected");
//        }
//    }
//
//    private void sendTextMessage(String inputText, boolean requireTTS) {
//        long available_mem = Runtime.getRuntime().freeMemory();
//        long total_mem = Runtime.getRuntime().totalMemory();
//        long current_time = System.currentTimeMillis();
//        String mess_id = hashFunction.hashString("" + available_mem + total_mem + current_time, StandardCharsets.UTF_8).toString();
//        if (requireTTS) {
//            //chon 1 trong 3 giong doc: phamtienquan-cleanbytool-balance-3k7    doanngocle-balanced-cleanbytool-fixtext     trinhthiviettrinh
//            String personalVoice = "phamtienquan-cleanbytool-balance-3k7";
//            mess_id += "-$_$-" + personalVoice + "__TTS__";
//        }
//        Log.e(TAG, "send_message: send" );
//
//        send_message(TextMessage.newBuilder().setDialogId(dialog_id).setMessId(mess_id).setAccount(account).setText(inputText).build());
//    }
//
//    public void tts(final String mess) {
//        if (player != null) {
//            player.release();
//            player.stop();
//        }
//        player = MyPlayAudio.getInstance().getPlayer();
//        Log.e(TAG, "send_message: send" );
//        sendTextMess = new Thread() {
//            @Override
//            public void run() {
//                sendTextMessage(mess, true);
//            }
//        };
//        sendTextMess.start();
//    }
//
//    public void stop() {
//        if (player != null) {
//            player.stop();
//            player.release();
//            player = null;
//            sendTextMess = null;
//        }
//    }
//
//
//    public CountDownLatch send_message(TextMessage message) {
//        final CountDownLatch finishLatch = new CountDownLatch(1);
//        player.play();
//        Log.e(TAG, "send_message: send" );
//        StreamObserver<TextReturnMessage> responseObserver = new StreamObserver<TextReturnMessage>() {
//            @Override
//            public void onNext(TextReturnMessage textReturnMessage) {
//                Log.e(TAG, "onNext: " + textReturnMessage.getText());
//                if (textReturnMessage != null) {
//
//                    byte[] tmp = textReturnMessage.getByteBuff().toByteArray();
//                    player.write(tmp, 0, tmp.length);
//                    // audioPlayer.append(tmp);
//                    Log.e(TAG, "sendTextToBot: length" + textReturnMessage.getByteBuff().toByteArray().length);
//                    Log.e(TAG, textReturnMessage.getText());
//                }
//
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                Status status = Status.fromThrowable(throwable);
//                Log.e(TAG, "onError: " + status);
//                finishLatch.countDown();
//            }
//
//            @Override
//            public void onCompleted() {
//                Log.e(TAG, "onCompleted: ");
//                finishLatch.countDown();
//            }
//        };
//        Log.e(TAG, "send_message: Sending" );
//        StreamObserver<TextMessage> request = asyncStub.sendTextMessage(responseObserver);
//        request.onNext(message);
//        request.onNext(TextMe);
//        request.onCompleted();
//        return finishLatch;
//    }


}
