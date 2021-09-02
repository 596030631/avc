package com.shuaijun.avc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.IntDef;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;


public class AvcEncoder {

    private static final String TAG = "AvcEncoder";
    private static final int TIMEOUT_USE = 12000;
    private static final Queue<byte[]> yuv420spQueue = new LinkedBlockingQueue<>();
    private static final Executor encoderThread = Executors.newSingleThreadExecutor();

    private static final int STEP_IDLE = 0;
    private static final int STEP_READY = 1;
    private static final int STEP_START_RECORDING = 2;
    private static final int STEP_STOP_RECORDING = 3;
    private static final int STEP_END = 4;

    @IntDef({STEP_IDLE, STEP_READY, STEP_START_RECORDING, STEP_STOP_RECORDING, STEP_END})
    @interface WorkStep {
    }

    @WorkStep
    private int workStatus = STEP_IDLE;

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final int FRAME_RATE = 25;

    private MediaCodec mediaCodec;
    public byte[] configByte;
    private BufferedOutputStream outputStream;
    private long index = 0L;

    private synchronized void initMediaCodec() {
        try {
            if (mediaCodec == null) mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", WIDTH, HEIGHT);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 2);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            workStatus = STEP_READY;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopEncoder() {
        encoderThread.execute(() -> {
            if (workStatus == STEP_START_RECORDING) {
                workStatus = STEP_STOP_RECORDING;
                while (!yuv420spQueue.isEmpty()) {
                    index = encoderAvc(yuv420spQueue.poll(), index);
                }
                try (BufferedOutputStream o = outputStream;) {
                    o.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaCodec.stop();
                initMediaCodec();
                workStatus = STEP_END;
            }
        });
    }

    public void startEncoder(File outFile) {
        if (workStatus == STEP_READY) {
            yuv420spQueue.clear();
            FileOutputStream fo;
            try {
                fo = new FileOutputStream(outFile);
                outputStream = new BufferedOutputStream(fo);
                mediaCodec.start();
                workStatus = STEP_START_RECORDING;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    public void offerByteBuffer(ByteBuffer byteBuffer) {
        switch (workStatus) {
            case STEP_IDLE:
                Log.w("et_log", "未初始化,无法提交");
                break;
            case STEP_END:
                Log.w("et_log", "处理结束,无法提交");
                break;
            case STEP_START_RECORDING:
            case STEP_READY:
                Log.w("et_log", "提交数据");
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes, 0, bytes.length);
                yuv420spQueue.offer(bytes);
                if (yuv420spQueue.size() > 10) {
                    encoderThread.execute(() -> {
                        while (!yuv420spQueue.isEmpty()) {
                            index = encoderAvc(yuv420spQueue.poll(), index);
                        }
                    });
                }
                break;
            case STEP_STOP_RECORDING:
                stopEncoder();
                break;
        }
    }

    private long encoderAvc(byte[] input, long generateIndex) {
        if (input == null) return generateIndex;
        long pts;
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            pts = computePresentationTime(generateIndex);
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
            generateIndex += 1;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USE);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            byte[] outData = new byte[bufferInfo.size];
            outputBuffer.get(outData);
            if (bufferInfo.flags == BUFFER_FLAG_CODEC_CONFIG) {
                configByte = new byte[bufferInfo.size];
                configByte = outData;
            } else if (bufferInfo.flags == BUFFER_FLAG_KEY_FRAME) {
                byte[] keyframe = new byte[bufferInfo.size + configByte.length];
                System.arraycopy(configByte, 0, keyframe, 0, configByte.length);
                System.arraycopy(outData, 0, keyframe, configByte.length, outData.length);
                saveToFile(keyframe);
            } else {
                saveToFile(outData);
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USE);
        }
        return generateIndex;
    }

    private void saveToFile(byte[] outData) {
        try {
            outputStream.write(outData, 0, outData.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }

    public void release() {
        if (mediaCodec != null) mediaCodec.release();
    }


    public static AvcEncoder getInstance() {
        return Holder.ins;
    }

    private static class Holder {
        private static final AvcEncoder ins = new AvcEncoder();
    }

    private AvcEncoder() {
        initMediaCodec();
    }
}