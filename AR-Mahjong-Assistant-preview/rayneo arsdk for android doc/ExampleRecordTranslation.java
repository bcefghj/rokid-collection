package com.example.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.AudioDeviceInfo;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ExampleRecordTranslation {

    private static final String TAG = "tag259901 ExampleRecordTranslation";

    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private int _bufferSizeInBytes = 512;

    public void start(Context context, File recordingFile, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) {
        _bufferSizeInBytes = bufferSizeInBytes;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setParameters("audio_source_record=record_origin3");
        } else {
            Log.e(TAG, "AudioManager setParameters failed");
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);

        final int SPEAKER_MIC = 23;
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC && device.getId() == SPEAKER_MIC) {
                audioRecord.setPreferredDevice(device);
            }
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed, res: " + audioRecord.getState());
            audioRecord.release();
            audioRecord = null;
            return;
        }
        audioRecord.startRecording();
        StartSub(audioRecord, recordingFile);
    }

    private void StartSub(AudioRecord audioRecord, File recordingFile) {
        new Thread(() -> {
            byte[] audioBuffer = new byte[_bufferSizeInBytes];
            try (FileOutputStream fos = new FileOutputStream(recordingFile)) {
                while (AudioRecord.RECORDSTATE_RECORDING == audioRecord.getRecordingState()) {
                    int bytesRead = audioRecord.read(audioBuffer, 0, _bufferSizeInBytes);
                    if (bytesRead > 0) {
                        try {
                            fos.write(audioBuffer, 0, bytesRead);
                        } catch (Exception e) {
                            Log.e(TAG, "write err", e);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "readAudioData: Error saving audio data", e);
            }
        }).start();
    }

    public void release(Context context)  {
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setParameters("audio_source_record=off");
        } else {
            Log.e(TAG, "AudioManager setParameters failed");
        }
    }
}
