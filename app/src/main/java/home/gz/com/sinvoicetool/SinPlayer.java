package home.gz.com.sinvoicetool;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wudi
 * @date 2018/10/17
 */
public class SinPlayer implements SinEncoder.SinGeneratorCallback, SinPcmPlayer.SinPcmCallback {

    /**
     * 存储二进制流
     */
    private List<Integer> codeStream = new ArrayList<>();

    /**
     * pcm播放器
     */
    private SinPcmPlayer sinPcmPlayer;
    /**
     * 编码器
     */
    private SinEncoder sinEncoder;

    /**
     * 缓存
     */
    private SinBuffer sinBuffer;

    /**
     * 播放线程
     */
    private Thread mPlayThread;
    /**
     * 编码线程
     */
    private Thread mEncodeThread;

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STATE_PENDING = 3;
    private int mState;

    public SinPlayer() {
        mState = STATE_STOP;

        this.sinBuffer = new SinBuffer();

        this.sinEncoder = new SinEncoder();
        this.sinEncoder.setSinGeneratorCallback(this);

        this.sinPcmPlayer = new SinPcmPlayer();
        this.sinPcmPlayer.setSinPcmCallback(this);
    }


    public void startPlay(String text) {
        if (!TextUtils.isEmpty(text)) {
            mState = STATE_PENDING;
            this.codeStream = CommonUtil.convertStringToBit(text);
            mPlayThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sinPcmPlayer.start();
                }
            });
            if (mPlayThread != null) {
                mPlayThread.start();
            }
            mEncodeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sinEncoder.start(codeStream);
                    stopPre();

                }
            });
            if (mEncodeThread != null) {
                mEncodeThread.start();
            }
            mState = STATE_START;
        }
    }

    @Override
    public SinBuffer.SinBufferData getGenBuffer() {
        return sinBuffer.getFirstProducerNode();
    }

    @Override
    public void freeGenBuffer(SinBuffer.SinBufferData buffer) {
        sinBuffer.putSinBufferConsumeData(buffer);
    }

    public void stopPlay() {
        if (STATE_START == mState) {
            mState = STATE_PENDING;
            sinEncoder.stop();
            if (null != mEncodeThread) {
                try {
                    mEncodeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mEncodeThread = null;
                }
            }
        }
    }

    /**
     * 播放结束后自动停止
     */
    private void stopPre() {
        if (sinEncoder.isStop()) {
            sinPcmPlayer.stop();
        }
        // 清除缓存
        sinBuffer.putSinBufferConsumeData(SinBuffer.getsEmptyBuffer());

        if (null != mPlayThread) {
            try {
                mPlayThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mPlayThread = null;
            }
        }
        sinBuffer.resetSinBuffer();
        mState = STATE_STOP;

        sinEncoder.stop();
        sinPcmPlayer.stop();

    }

    @Override
    public SinBuffer.SinBufferData getPcmBuffer() {
        return sinBuffer.getSinBufferConsumeData();
    }

    @Override
    public void freePcmBuffer(SinBuffer.SinBufferData buffer) {
        sinBuffer.putBufferData(buffer);
    }
}
