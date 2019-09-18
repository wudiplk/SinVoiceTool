package home.gz.com.sinvoicetool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * @author Wudi
 * @date 2018/10/26
 */
public class SinPcmPlayer {
    /**
     * 播放状态，用于控制播放或者是停止
     */
    private int mState;
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    /**
     * 音频播放
     */
    private AudioTrack audioTrack;
    /**
     * 播放字节数
     */
    private long playedCharLength = 0;

    public SinPcmPlayer() {
        mState = STATE_STOP;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, CommonUtil.DEFAULT_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, SinBuffer.DEFAULT_BUFFER_SIZE, AudioTrack.MODE_STREAM);
    }

    public void start() {
        if(mState==STATE_STOP&&audioTrack!=null) {
            mState = STATE_START;
            playedCharLength=0;
            if(sinPcmCallback!=null) {
                // 初始化AudioTrack对象(音频流类型，采样率，通道，格式，缓冲区大小，模式)
                while (mState == STATE_START) {
                    SinBuffer.SinBufferData sinBufferData = sinPcmCallback.getPcmBuffer();
                    if (sinBufferData != null && sinBufferData.shortData != null) {
                        if (playedCharLength == 0) {
                            audioTrack.play();
                        }
                        int len = audioTrack.write(sinBufferData.getShortData(), 0, sinBufferData.getBufferSize());

                        playedCharLength += len;

                        // 释放已播放的数据
                        sinPcmCallback.freePcmBuffer(sinBufferData);
                    }else {
                        break;
                    }
                }
                if (STATE_STOP == mState) {
                    audioTrack.pause();
                    audioTrack.flush();
                    audioTrack.stop();
                    audioTrack.release();
                }
            }else {
                throw new IllegalArgumentException("PcmCallback can't be null");
            }
        }
    }
    public void stop() {
        if (STATE_START == mState && null != audioTrack) {
            mState = STATE_STOP;
        }
    }
    private SinPcmCallback sinPcmCallback;

    public interface SinPcmCallback {
        /**
         * 获取队列中的数据
         * @return
         */
        SinBuffer.SinBufferData getPcmBuffer();

        /**
         * 释放队列中的资源
         * @param buffer
         */
        void freePcmBuffer(SinBuffer.SinBufferData buffer);
    }

    public void setSinPcmCallback(SinPcmCallback sinPcmCallback) {
        this.sinPcmCallback = sinPcmCallback;
    }
}
