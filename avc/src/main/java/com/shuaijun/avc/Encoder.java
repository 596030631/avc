package com.shuaijun.avc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Encoder {
    private static final String AVC = "video/avc";
    private final MediaCodec mediaCodec;
    private FileOutputStream outputStream;
    private byte[] mInfo = null;

    public Encoder(int width, int height, int frameRate, int bitRate) throws IOException {
        mediaCodec = MediaCodec.createEncoderByType(AVC);
        MediaFormat format = MediaFormat.createVideoFormat(AVC, width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void start(File outFile) throws FileNotFoundException {
        mediaCodec.start();
        outputStream = new FileOutputStream(outFile);
    }

    public int offerData(byte[] input, byte[] output) throws IOException {
        int pos = 0;
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            byte[] outData = new byte[bufferInfo.size];
            outputBuffer.get(outData);
            if (mInfo != null) {
                System.arraycopy(outData, 0, output, 0, outData.length);
                pos += outData.length;
            } else if (bufferInfo.flags == 2) {
                mInfo = new byte[outData.length];
                System.arraycopy(outData, 0, mInfo, 0, outData.length);
                System.arraycopy(outData, 0, output, pos, outData.length);
                pos += outData.length;
            } else {
                return -1;
            }
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }

        if (bufferInfo.flags == 1) {
            System.arraycopy(output, 0, input, 0, pos);
            System.arraycopy(mInfo, 0, output, 0, mInfo.length);
            System.arraycopy(input, 0, output, mInfo.length, pos);
            pos += mInfo.length;
        }
        outputStream.write(output, 0, pos);
        return pos;
    }

    public void release() {
        mediaCodec.stop();
        mediaCodec.release();
        try (FileOutputStream output = outputStream) {
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
