package home.gz.com.sinvoicetool;

import android.util.Log;

/**
 * @author Wudi
 * @date 2018/10/26
 */
public class SinDecode {
    private final static String TAG = "SinDecode";
    /**
     * 记录起始状态
     */
    private int mState;
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    /**
     * 判断是否是周期性音频，并计算周期数的怕段
     */
    private int mStep;
    private final static int STATE_STEP1 = 1;
    private final static int STATE_STEP2 = 2;
    /**
     * 开始标志的周期数 结束标志的周期数
     */
    private final static int START_CIRCLE = 8;
    private final static int END_CIRCLE = 14;

    /**
     * 是否是发送的音频开始解码
     */
    private boolean mIsBeginning = false;
    private boolean mStartingDet = false;
    private int mStartingDetCount;
    private boolean mIsStartCounting = false;

    /**
     * 正在解码中的相关参数
     */
    private int mRegValue;
    private int mRegCount;
    private boolean mIsRegStart = false;
    private int mPreRegCircle = -1;
    /**
     * 几率音频数据周期
     */
    private int mCirclePointCount = 0;

    public SinDecode() {
        mState = STATE_STOP;
    }

    public void start() {
        if (mState == STATE_STOP) {
            mState = STATE_START;
            if (sinDecodeCallBack != null) {
                mCirclePointCount = 0;
                mIsStartCounting = false;
                mStep = STATE_STEP1;
                mIsBeginning = false;
                mStartingDet = false;
                mStartingDetCount = 0;

                // 通知主页面面开始解码
                if (onRecognitionListener != null) {
                    onRecognitionListener.onRecognitionStart();
                }

                // 执行解码
                while (mState == STATE_START) {
                    SinBuffer.SinBufferData sinBufferData = sinDecodeCallBack.getDecodeBuffer();
                    if (sinBufferData != null) {
                        if (sinBufferData.shortData != null) {
                            //
                            process(sinBufferData);
                            // 释放资源
                            sinDecodeCallBack.freeDecodeBuffer(sinBufferData);
                        }
                    }
                }

                // 通知主页面解码结束
                if (onRecognitionListener != null) {
                    onRecognitionListener.onRecognitionEnd();
                }

            }
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    /**
     * 解码执行
     *
     * @param sinBufferData
     */
    private void process(SinBuffer.SinBufferData sinBufferData) {
        int size = sinBufferData.getBufferSize();
        short sh = 0;

        for (int i = 0; i < size; i++) {
            short sh1 = sinBufferData.shortData[i];
            // 有符号转化为无符号位
            sh1 &= 0xff;
            short sh2 = sinBufferData.shortData[++i];
            // <<      :     左移运算符，num << 1,相当于num乘以2
            // 在编码中   bufferData.byteData[mFilledSize++] = (byte) ((out >> 8) & 0xff);
            sh2 <<= 8;
            // 位运算，求两者之和
            sh = (short) ((sh1) | (sh2));
            if (!mIsStartCounting) {
                // 判断此频率是否是有周期的，进行滤波处理
                if (STATE_STEP1 == mStep) {
                    // 通过一次高低峰值对比
                    if (sh < 0) {
                        mStep = STATE_STEP2;
                    }
                } else if (STATE_STEP2 == mStep) {
                    if (sh > 0) {
                        mIsStartCounting = true;
                        mCirclePointCount = 0;
                        mStep = STATE_STEP1;
                    }
                }
            } else {
                // 计算周期正负变换
                ++mCirclePointCount;
                if (STATE_STEP1 == mStep) {
                    if (sh < 0) {
                        mStep = STATE_STEP2;
                    }
                } else if (STATE_STEP2 == mStep) {
                    if (sh > 0) {
                        Log.d(TAG, "mCirclePointCount " + mCirclePointCount);
                        // 预处理 。取周期中间值，增加传输容错率
                        int circleCount = preRecognition(mCirclePointCount);
                        // 识别语音，处理音频周期
                        recognition(circleCount);
                        mCirclePointCount = 0;
                        mStep = STATE_STEP1;
                    }
                }
            }
        }
    }


    /**
     * 处理周期数偏差
     *
     * @param circleCount
     * @return
     */
    private int preRecognition(int circleCount) {
        switch (circleCount) {
            case 4:
            case 5:
            case 6:
                circleCount = 5;
                break;
            case 7:
            case 8:
            case 9:
                circleCount = 8;
                break;
            case 10:
            case 11:
            case 12:
                circleCount = 11;
                break;
            case 13:
            case 14:
            case 15:
                circleCount = 14;
                break;
            default:
                circleCount = 0;
                break;
        }

        return circleCount;
    }

    private StringBuffer stringBuffer = new StringBuffer();

    /**
     * 正式处理周期
     *
     * @param circleCount
     */
    private void recognition(int circleCount) {
        // 判断是否是指定的字符开头
        if (!mIsBeginning) {
            if (!mStartingDet) {
                // 8的周期取样数量 对应的是指定开头字符 1所对应周期 ,开始标志 进入取样模式
                if (START_CIRCLE == circleCount) {
                    mStartingDet = true;
                    mStartingDetCount = 0;
                    Log.d("周期 起始  ", circleCount + "");
                }
            } else {
                // 再次为 8 的话计数+1，否则重置
                if (START_CIRCLE == circleCount) {
                    ++mStartingDetCount;
                    Log.d("周期 再次  ", circleCount + "");
                    //  计算周期采样数为 8的 周期数大于最小周期采样数的时候，开始记录有效数字周期数
                    if (mStartingDetCount >= END_CIRCLE) {
                        mIsBeginning = true;
                        mIsRegStart = false;
                        mRegCount = 0;
                    }
                } else {
                    mStartingDet = false;
                }
            }
        } else {
            // 如果是1开头且判断是否是在注册文本中的数字
            if (!mIsRegStart) {
                // 是否有效值注册开始
                if (circleCount > 0) {
                    mRegValue = circleCount;
                    // 判断是 文本表对应注册的文本数字
                    mIsRegStart = true;
                    mRegCount = 1;
                    Log.d("周期 确定  ", circleCount + "");
                }
            } else {
                // 判断是文本表对应的数字后就不再判断，直接进行计数
                if (circleCount == mRegValue) {
                    ++mRegCount;
                    Log.d("周期 计数", " mRegCount" + mRegCount + " Value " + mRegValue);
                    // 当注册的计数个数大于最小计数个数的时候
                    if (mRegCount >= END_CIRCLE) {
                        if (mPreRegCircle != mRegValue) {

                            if (null != onRecognitionListener) {
                                if (circleCount == 8) {
                                    stringBuffer.append("1");
                                }
                                if (circleCount == 11) {
                                    stringBuffer.append("0");
                                }
                            }
                            if (circleCount == 14) {
                                onRecognitionListener.onRecognition(stringBuffer.toString());
                                Log.d("CommonUtil", stringBuffer.toString());
                                stringBuffer = new StringBuffer();
                            }
                            mPreRegCircle = mRegValue;
                        }

                        mIsRegStart = false;
                    }
                } else {
                    mIsRegStart = false;
                }
            }
        }
    }


    public interface SinDecodeCallBack {
        /**
         * 获取解码数据
         *
         * @return
         */
        SinBuffer.SinBufferData getDecodeBuffer();

        /**
         * 释放缓存数据
         *
         * @param sinBufferData
         */
        void freeDecodeBuffer(SinBuffer.SinBufferData sinBufferData);
    }

    private SinDecodeCallBack sinDecodeCallBack;

    public void setSinDecodeCallBack(SinDecodeCallBack sinDecodeCallBack) {
        this.sinDecodeCallBack = sinDecodeCallBack;
    }

    public SinRecognition.OnRecognitionListener onRecognitionListener;

    public void setOnRecognitionListener(SinRecognition.OnRecognitionListener onRecognitionListener) {
        this.onRecognitionListener = onRecognitionListener;
    }
}
