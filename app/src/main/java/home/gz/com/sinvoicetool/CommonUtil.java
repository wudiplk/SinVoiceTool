package home.gz.com.sinvoicetool;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wudi
 * @date 2018/10/16
 */
public class CommonUtil {
    private static final String TAG = "CommonUtil";
    /**
     * ASCII 最小和最大字符限定
     */
    private final static int MIN_ASCII = 32, MAX_ASCII = 127;
    /**
     * 默认采样率
     */
    public final static int DEFAULT_SAMPLE_RATE = 44100;

    /**
     * 默认每个字发送的正弦波持续时间
     */
    public final static int DEFAULT_DURATION = 100;

    /**
     * 字符映射频率，采用二进制传输 8820代表符号间隔， 5512：代表1与开始标志  4409 ：代表0   3150 ：代表结束标志
     * 5  8  10  14
     */
    public static final int[] CODE_FREQUENCY = new int[]{8820, 5512, 4409, 3150};

    private static List<Integer> mCodes = new ArrayList<Integer>();

    /**
     * 将字符串转化为二进制流
     *
     * @param text
     * @return
     */
    public static List<Integer> convertStringToBit(String text) {
        // 存放二进制流
        mCodes.clear();
        if (!TextUtils.isEmpty(text)) {
            char[] chars = text.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                addBitString(Integer.toBinaryString(chars[i]));
                mCodes.add(3);
                Log.d(TAG, String.valueOf(mCodes));
            }
        }
        return mCodes;
    }

    private static void addBitString(String bitString) {
        Log.d(TAG, "二进制编码：" + bitString);
        for (int j = 0; j < bitString.length(); j++) {
            if (bitString.charAt(j) == '1') {
                mCodes.add(1);
            } else {
                mCodes.add(2);
            }
            mCodes.add(0);
        }
    }


    /**
     * 将二进制转化为字符串
     *
     * @param bitStream
     * @return
     */
    public static String convertBitToString(String bitStream) {
        String text = "";
        if (!TextUtils.isEmpty(bitStream)) {
            Log.d(TAG, "二进制解码： " + bitStream);
            int[] temp = bitToIntArray(bitStream);
            int sum = 0;
            for (int i = 0; i < temp.length; i++) {
                sum += temp[temp.length - 1 - i] << i;
            }
            text = String.valueOf((char) sum);
        }
        return text;
    }

    /**
     * 将二进制字符串转换成int数组
     *
     * @param binStr
     * @return
     */
    public static int[] bitToIntArray(String binStr) {
        char[] temp = binStr.toCharArray();
        int[] result = new int[temp.length];
        for (int i = 0; i < temp.length; i++) {
            result[i] = temp[i] - 48;
        }
        return result;
    }

}
