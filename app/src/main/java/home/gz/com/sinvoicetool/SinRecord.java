package home.gz.com.sinvoicetool;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * @author Wudi
 * @date 2018/10/26
 */
public class SinRecord {

    private AudioRecord record;

    private int mState;
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    public SinRecord() {

    }

    /**
     * 录制音频，开始解码
     */
    public void start() {
        mState = STATE_START;
        record=new AudioRecord(MediaRecorder.AudioSource.MIC
                , CommonUtil.DEFAULT_SAMPLE_RATE
                , AudioFormat.CHANNEL_IN_MONO
                , AudioFormat.ENCODING_PCM_16BIT
                , SinBuffer.DEFAULT_BUFFER_SIZE);
        record.startRecording();
        while (mState == STATE_START) {
            SinBuffer.SinBufferData data = sinRecordCallBack.getDecodingBuff();
            if (null != data) {
                if (null != data.shortData) {
                    int bufferReadResult = record.read(data.shortData, 0, SinBuffer.DEFAULT_BUFFER_SIZE);
                    data.setBufferSize(bufferReadResult);
                    sinRecordCallBack.freeRecordBuffer(data);
                } else {
                    // 结束输入
                    break;
                }
            } else {
                break;
            }
        }
        record.stop();
        record.release();
    }
    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    private SinRecordCallBack sinRecordCallBack;

    public interface SinRecordCallBack {
        /**
         * 获取录音缓存空间
         *
         * @return
         */
        SinBuffer.SinBufferData getDecodingBuff();

        /**
         * 释放缓存空间
         *
         * @param sinBufferData
         */
        void freeRecordBuffer(SinBuffer.SinBufferData sinBufferData);
    }

    public void setSinDecodingCallBack(SinRecordCallBack sinRecordCallBack) {
        this.sinRecordCallBack = sinRecordCallBack;
    }
}
