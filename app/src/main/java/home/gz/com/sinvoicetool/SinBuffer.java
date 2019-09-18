package home.gz.com.sinvoicetool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Wudi
 * @date 2018/10/17
 * 数据的存储采用阻塞队列（BlockingQueue）
 */
public class SinBuffer {
    private final static String TAG = "SinBuffer";

    /**
     * 默认缓冲区大小 大于最100ms 内的采样率
     */
    public final static int DEFAULT_BUFFER_SIZE = 4096;
    /**
     * 默认缓冲区数量
     */
    public final static int DEFAULT_BUFFER_COUNT = 3;

    /**
     * 生产队列
     */
    private BlockingQueue<SinBufferData> sinBufferDataProducer;

    /**
     * 消费队列
     */
    private BlockingQueue<SinBufferData> sinBufferDataConsumer;

    // 静态空缓冲区
    private static SinBuffer.SinBufferData sEmptyBuffer = new SinBuffer.SinBufferData(0);

    public static SinBuffer.SinBufferData getsEmptyBuffer() {
        return sEmptyBuffer;
    }


    public SinBuffer() {
        this(DEFAULT_BUFFER_COUNT, DEFAULT_BUFFER_SIZE);
    }

    public SinBuffer(int mSinBufferCount, int mSinBufferSize) {
        sinBufferDataProducer = new LinkedBlockingDeque<>(mSinBufferCount);
        // 需要结束缓存，所以数量比生产者要+1
        sinBufferDataConsumer = new LinkedBlockingDeque<>(mSinBufferCount + 1);
        // 初始化生产者对立
        for (int i = 0; i < mSinBufferCount; i++) {
            try {
                sinBufferDataProducer.put(new SinBufferData(mSinBufferSize));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 重置数据
     */
    public void resetSinBuffer() {
        // 将生产者的空头结点剔除
        int size = sinBufferDataProducer.size();
        for (int i = 0; i < size; i++) {
            SinBufferData sinBufferData = sinBufferDataProducer.peek();
            if (null == sinBufferData || null == sinBufferData.shortData) {
                sinBufferDataProducer.poll();
            }
        }
        // 将消费者中的非空数据添加到生产者当中
        size = sinBufferDataConsumer.size();
        for (int i = 0; i < size; i++) {
            SinBufferData sinBufferData = sinBufferDataConsumer.poll();
            if (null != sinBufferData && null != sinBufferData.shortData) {
                sinBufferDataProducer.add(sinBufferData);
            }
        }
    }

    /**
     * 获取生产者头节点
     */
    public SinBuffer.SinBufferData getFirstProducerNode() {
        return getBufferDataImpl(sinBufferDataProducer);
    }


    /**
     * 从消费者队列获取
     *
     * @return
     */
    public SinBuffer.SinBufferData getSinBufferConsumeData() {
        return getBufferDataImpl(sinBufferDataConsumer);
    }

    /**
     * 加入到消费者和队列
     * @param sinBufferData
     * @return
     */
    public boolean putSinBufferConsumeData(SinBufferData sinBufferData) {
        return putBufferDataImpl(sinBufferDataConsumer, sinBufferData);
    }

    /**
     * 写入数据到生产者队列
     *
     * @param sinBufferData
     * @return
     */
    public boolean putBufferData(SinBufferData sinBufferData) {
        return putBufferDataImpl(sinBufferDataProducer, sinBufferData);
    }

    /**
     * 读取缓存数据，FIFO
     *
     * @param dataBlockingQueue
     * @return
     */
    private SinBufferData getBufferDataImpl(BlockingQueue<SinBufferData> dataBlockingQueue) {
        if (dataBlockingQueue != null) {
            try {
                return dataBlockingQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 写入缓存数据
     *
     * @param dataBlockingQueue
     * @param sinBufferData
     * @return
     */
    private boolean putBufferDataImpl(BlockingQueue<SinBufferData> dataBlockingQueue, SinBufferData sinBufferData) {
        if (dataBlockingQueue != null && sinBufferData != null) {
            try {
                dataBlockingQueue.put(sinBufferData);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static class SinBufferData {
        /**
         * 数据存储采用16喂short
         */
        public byte[] shortData;
        /**
         * 填充体积大小
         */
        private int bufferSize;

        /**
         * 最大填充提及
         */
        private int maxBufferSize;

        /**
         * 静态的空缓冲区
         */
        public SinBufferData(int mMaxBufferSize) {
            this.maxBufferSize = mMaxBufferSize;
            bufferSize = 0;
            if (mMaxBufferSize > 0) {
                shortData = new byte[mMaxBufferSize];
            } else {
                shortData = null;
            }
        }

        public byte[] getShortData() {
            return shortData;
        }

        public void setShortData(byte[] shortData) {
            this.shortData = shortData;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        public int getMaxBufferSize() {
            return maxBufferSize;
        }

        public void setMaxBufferSize(int maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            shortData = new byte[maxBufferSize];
        }

    }
}
