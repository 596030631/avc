package com.shuaijun.avc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.SystemClock;

import java.io.BufferedOutputStream;
import java.io.File;
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
    private final static String TAG = "MediaCodec";

    private static final Executor thread = Executors.newSingleThreadExecutor();

    private static final Queue<byte[]> yuv420spQUeue = new LinkedBlockingQueue<>();

    private boolean isRunning = false;

    private final int TIMEOUT_USE = 12000;

    private MediaCodec mediaCodec;
    int mWidth = 640;
    int mHeight = 480;
    int mFrameRate = 25;
    public byte[] configByte;
    private BufferedOutputStream outputStream;

    private void initMediaCodec() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800 * 1024);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void stopEncoder() {
        isRunning = false;
        yuv420spQUeue.clear();
    }

    public void startEncoder(File outFile) {
        thread.execute(() -> {
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(outFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaCodec.start();
            isRunning = true;
            yuv420spQUeue.clear();
            byte[] input;
            long pts;
            long generateIndex = 0;
            try {
                while (isRunning) {
                    if (yuv420spQUeue.isEmpty()) {
                        SystemClock.sleep(300);
                        continue;
                    }
                    input = yuv420spQUeue.poll();
                    if (input == null) continue;
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
                    int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, this.TIMEOUT_USE);
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
                            outputStream.write(keyframe, 0, keyframe.length);
                        } else {
                            outputStream.write(outData, 0, outData.length);
                        }
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USE);
                    }
                }
                outputStream.flush();
                outputStream.close();
                mediaCodec.stop();
                initMediaCodec();
            } catch (Exception t) {
                t.printStackTrace();
            }
        });
    }

    public void offerByteBuffer(ByteBuffer byteBuffer) {
        while (yuv420spQUeue.size() > 10) {
            yuv420spQUeue.poll();
        }
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes, 0, bytes.length);
        yuv420spQUeue.offer(bytes);
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }

    public void release() {
        mediaCodec.release();
    }

    public static AvcEncoder getInstance() {
        return Holder.ins;
    }

    private static class Holder {
        private static final AvcEncoder ins = new AvcEncoder();
    }

    private AvcEncoder() {
        //
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        initMediaCodec();
    }
}