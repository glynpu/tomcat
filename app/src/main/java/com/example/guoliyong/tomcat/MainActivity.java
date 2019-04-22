package com.example.guoliyong.tomcat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private boolean IsRecording;
    private Thread recordingThread;
    private Thread playBackThread;
    private boolean shouldContinue = false;
    public static final int SAMPLE_DURATION_MS = 100000;
    public static final int SAMPLE_RATE = 16000;
    public static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    public short[] recordingBuffer = new short[RECORDING_LENGTH];
    protected int recordingOffset = 0;
    public int newRecordingOffset = 0;

    public FloatingActionButton btnRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取录音button 按钮
        btnRecord = (FloatingActionButton) findViewById(R.id.btn_record);

        // 为录音button 按钮添加监听事件
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePressRecordButtion();
            }
        });

    }

    public void handlePressRecordButtion() {
        if (IsRecording) {
            IsRecording = false;
            btnRecord.setImageResource(R.drawable.ic_mic_white_36dp);

            stopRecording();
            plackBack();

        } else {
            btnRecord.setImageResource(R.drawable.ic_media_stop);
            startRecording();
            IsRecording = true;

        }
    }

    public void plackBack() {
        playBackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                playWavByteBuffer();
                recordingOffset = 0;
                newRecordingOffset = 0;
            }
        });
        playBackThread.start();
    }

    public void startRecording() {
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();

    }

    public void stopRecording() {
        shouldContinue = false;
        recordingThread = null;

    }


    public void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];
        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            return;
        }
        record.startRecording();
        int secondCopyLength, firstCopyLength;
        while (shouldContinue) {
            System.out.println("newRecordingOffset: " + newRecordingOffset);
            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            newRecordingOffset = recordingOffset + numberRead;
            secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            firstCopyLength = numberRead - secondCopyLength;
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
            }
        }
        record.stop();
        record.release();
    }

    public void playWavByteBuffer() {
        int play_sample_rate = 24000;
        int minBufferSize = AudioTrack.getMinBufferSize(play_sample_rate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, play_sample_rate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
        int bufferSize = 512;
        byte[] data = ShortToByte_Twiddle_Method(recordingBuffer, newRecordingOffset);
        int i = 0;
        byte[] s = new byte[bufferSize];
        try {
            InputStream input = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(input);
            at.play();
            while ((i = dis.read(s, 0, bufferSize)) > -1) {
                at.write(s, 0, i);
            }
            at.stop();
            at.release();
            dis.close();
            input.close();
        } catch (FileNotFoundException e) {
            // TODO
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    byte[] ShortToByte_Twiddle_Method(short[] input, int offset) {
        int short_index, byte_index;
//        int iterations = input.length;
        int iterations = offset;
        byte[] buffer = new byte[iterations * 2];
        short_index = byte_index = 0;
        for (/*NOP*/; short_index != iterations; /*NOP*/) {
            buffer[byte_index] = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);
            ++short_index;
            byte_index += 2;
        }
        return buffer;
    }
}
