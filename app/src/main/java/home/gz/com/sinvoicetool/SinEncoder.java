package home.gz.com.sinvoicetool;

import android.util.Log;

import java.util.List;

/**
 * @author Wudi
 * @date 2018/10/16
 */
public class SinEncoder {

    private final static String TAG = "SinEncoder";
    /**
     * 状态判断
     */
    private static final int STATE_START = 1;
    private static final int STATE_STOP = 2;
    private int mState;

    public SinEncoder() {
        mState = STATE_STOP;
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    public void start(List<Integer> codeStream) {
        if (STATE_STOP == mState) {
            mState = STATE_START;
            for (int i = 0; i < codeStream.size(); i++) {
                // 根据正弦波公式进行转换
                if(mState==STATE_START) {
                    genSinEquation(CommonUtil.CODE_FREQUENCY[codeStream.get(i)]
                            , CommonUtil.DEFAULT_SAMPLE_RATE
                            , CommonUtil.DEFAULT_DURATION);
                }else {
                    Log.d(TAG,"force stop");
                }
            }
        }
    }

    /**
     * 每个采样点的存储方式，采用16为PCM存储
     */
    private final int MAX_SHORT = 32768;
    private final int MIN_SHORT = -32768;

    /**
     * @param frequency  生成正弦波的频率
     * @param sampleRate 默认采样率
     * @param duration   传输每个字持续时间(duration)，即每个字传输周期正弦波持续时间
     * @return
     */
    private void genSinEquation(int frequency, int sampleRate, int duration) {
        if (mState == STATE_START) {
            // 从消费队列中获取
            SinBuffer.SinBufferData sinBufferData = sinGeneratorCallback.getGenBuffer();
            // 每个字发出频率的采样点数
            int frameRateCount = (duration * sampleRate) / 1000;
            // 步数
            double thea = 0;

            int n = MAX_SHORT / 2;

            int index = 0;
            // 正弦波 的频率
            double thetaIncrement = (frequency / (double) sampleRate) * 2 * Math.PI;

            Log.d(TAG, "thetaIncrement " + String.valueOf(thetaIncrement) + "frequency  " + frequency);
            for (int frame = 0; frame < frameRateCount; frame++) {
                if (mState == STATE_START) {
                    // 算出不同点的正弦值
                    int out = (int) (Math.sin(thea) * n) + 128;
                    if (index >= SinBuffer.DEFAULT_BUFFER_SIZE - 1) {
                        // 超过限定值之后重置大小
                        sinBufferData.setBufferSize(index);
                        sinGeneratorCallback.freeGenBuffer(sinBufferData);
                        index = 0;
                        sinBufferData = sinGeneratorCallback.getGenBuffer();
                    }
                    // 转码为short类型并保存，& 0xff是为了防止负数转换出现异常
                    sinBufferData.shortData[index++] = (byte) (out & 0xff);
                    if (MAX_SHORT == n * 2) {
                        sinBufferData.shortData[index++] = (byte) ((out >> 8) & 0xff);
                    }
                    thea += thetaIncrement;
                }
            }
            // 加入到消费队列中去
            if (sinBufferData != null) {
                sinBufferData.setBufferSize(index);
                sinGeneratorCallback.freeGenBuffer(sinBufferData);
            }
        }
    }

    private SinGeneratorCallback sinGeneratorCallback;

    public boolean isStop() {
        return (STATE_STOP == mState);
    }

    public interface SinGeneratorCallback {
        /**
         * 获取队列中的数据
         * @return
         */
        SinBuffer.SinBufferData getGenBuffer();

        /**
         * 释放队列中的资源
         * @param buffer
         */
        void freeGenBuffer(SinBuffer.SinBufferData buffer);
    }

    public void setSinGeneratorCallback(SinGeneratorCallback sinGeneratorCallback) {
        this.sinGeneratorCallback = sinGeneratorCallback;
    }
}


