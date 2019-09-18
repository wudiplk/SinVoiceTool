package home.gz.com.sinvoicetool;

/**
 * @author Wudi
 * @date 2018/10/26
 */
public class SinRecognition implements SinRecord.SinRecordCallBack, SinDecode.SinDecodeCallBack {

    private final static String TAG = "SinRecognition";
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;
    /**
     * 当前状态
     */
    private int mState;

    /**
     * 数据缓存
     */
    private SinBuffer sinBuffer;

    /**
     * 录制音频存储
     */
    private SinRecord sinRecord;

    /**
     * 录音线程
     */
    private Thread mRecordThread;
    /**
     * 解码线程
     */
    private Thread mRecognitionThread;

    /**
     * 音频解码
     */
    private SinDecode sinDecode;


    public SinRecognition() {
        mState = STATE_STOP;
        sinBuffer = new SinBuffer();

        sinRecord = new SinRecord();
        sinRecord.setSinDecodingCallBack(this);

        sinDecode = new SinDecode();
        sinDecode.setSinDecodeCallBack(this);
    }


    @Override
    public SinBuffer.SinBufferData getDecodingBuff() {
        return sinBuffer.getFirstProducerNode();
    }

    @Override
    public void freeRecordBuffer(SinBuffer.SinBufferData sinBufferData) {
        if(sinBufferData!=null) {
            sinBuffer.putSinBufferConsumeData(sinBufferData);
        }
    }

    /**
     * 开始解码
     */
    public void startDecoding() {
        if (mState == STATE_STOP) {
            mState = STATE_PENDING;
            // 开始解码线程
            mRecognitionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sinDecode.start();
                }
            });
            if (mRecognitionThread != null) {
                mRecognitionThread.start();
            }

            // 开始录音线程
            mRecordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sinRecord.start();
                }
            });
            if (mRecordThread != null) {
                mRecordThread .start();
            }
            mState = STATE_START;
        }
    }

    public void stopDecoding() {
        //录音器停止
        sinRecord.stop();
        if (null != mRecordThread) {
            try {
                mRecordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mRecordThread = null;
            }
        }
        // 解码器停止
        sinDecode.stop();
        SinBuffer.SinBufferData sinBufferData = new SinBuffer.SinBufferData(0);
        sinBuffer.putSinBufferConsumeData(sinBufferData);
        if (null != mRecognitionThread) {
            try {
                mRecognitionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mRecognitionThread = null;
            }
        }

        sinBuffer.resetSinBuffer();
        mState = STATE_STOP;
    }

    @Override
    public SinBuffer.SinBufferData getDecodeBuffer() {
        return sinBuffer.getSinBufferConsumeData();
    }

    @Override
    public void freeDecodeBuffer(SinBuffer.SinBufferData sinBufferData) {
        if(sinBufferData!=null) {
            sinBuffer.putBufferData(sinBufferData);
        }
    }

    public interface OnRecognitionListener {
        /**
         * 解码开始
         */
        void onRecognitionStart();

        /**
         * 解码中
         *
         * @param ch
         */
        void onRecognition(String ch);

        /**
         * 解码结束
         */
        void onRecognitionEnd();
    }

    public void setOnRecognitionListener(OnRecognitionListener onRecognitionListener) {
        if (sinDecode != null) {
            sinDecode.setOnRecognitionListener(onRecognitionListener);
        }
    }
}
