package home.gz.com.sinvoicetool;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pc
 */
public class MainActivity extends AppCompatActivity implements SinRecognition.OnRecognitionListener {

    private String TAG = "MainActivity";
    private SinPlayer sinPlayer;
    private SinRecognition sinRecognition;
    // 识别成功
    private final static int MSG_SET_RECG_TEXT = 1;
    // 开始识别
    private final static int MSG_RECG_START = 2;
    // 识别结束
    private final static int MSG_RECG_END = 3;

    private EditText editText;
    private TextView editShowText;
    private RegHandler regHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.edText);
        editShowText = findViewById(R.id.edShowText);

        sinPlayer = new SinPlayer();
        sinRecognition = new SinRecognition();
        sinRecognition.setOnRecognitionListener(this);
        regHandler = new RegHandler(editShowText);
        findViewById(R.id.tvStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sinPlayer.startPlay(editText.getText().toString());
            }
        });
        findViewById(R.id.tvRecord).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "开始录音", Toast.LENGTH_SHORT).show();
                sinRecognition.startDecoding();
            }
        });
        this.checkPermissions(this.mNeedPermissions);
    }

    /**
     * 需要进行检测的权限数组
     */
    public String[] mNeedPermissions = {
            Manifest.permission.DISABLE_KEYGUARD,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };

    /**
     * 检测权限
     *
     * @param permissions
     */
    public boolean checkPermissions(String... permissions) {
        List<String> needRequestPermissionList = findDeniedPermissions(permissions);
        if (null != needRequestPermissionList && needRequestPermissionList.size() > 0) {
            // 申请权限
            ActivityCompat.requestPermissions(this, needRequestPermissionList.toArray(new String[needRequestPermissionList.size()]), 101);
        }
        return needRequestPermissionList.size() <= 0;
    }

    /**
     * 检测所需的权限是否在已授权的列表中
     *
     * @param permissions
     * @return
     */
    private List<String> findDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissionList = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                needRequestPermissionList.add(perm);
            }
        }
        return needRequestPermissionList;
    }

    private class RegHandler extends Handler {

        private StringBuilder mTextBuilder = new StringBuilder();

        private TextView editText;

        public RegHandler(TextView editText) {
            this.editText = editText;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    String ch = (String) msg.obj;
                    Toast.makeText(MainActivity.this, ch + "", Toast.LENGTH_SHORT).show();
                    mTextBuilder.append(CommonUtil.convertBitToString(ch));
                    if (editText != null) {
                        editText.setText(mTextBuilder.toString());
                    }
                    break;

                case MSG_RECG_START:
                    mTextBuilder.delete(0, mTextBuilder.length());
                    break;

                case MSG_RECG_END:
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRecognitionStart() {
        regHandler.sendEmptyMessage(MSG_RECG_START);
    }

    @Override
    public void onRecognition(String ch) {
        regHandler.sendMessage(regHandler.obtainMessage(MSG_SET_RECG_TEXT, ch));
    }

    @Override
    public void onRecognitionEnd() {
        regHandler.sendEmptyMessage(MSG_RECG_END);
    }

    @Override
    protected void onDestroy() {
        sinRecognition.stopDecoding();
        super.onDestroy();
    }
}
