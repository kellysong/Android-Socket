package com.sjl.socket.test;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sjl.socket.test.util.NetUtils;
import com.yanzhenjie.loading.dialog.LoadingDialog;

import java.util.LinkedList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ServerManager mServerManager;

    private Button mBtnStart;
    private Button mBtnStop;
    private TextView mTvMessage;

    private LoadingDialog mDialog;
    private String mRootUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBtnStart = findViewById(R.id.btn_start);
        mBtnStop = findViewById(R.id.btn_stop);
        mTvMessage = findViewById(R.id.tv_message);

        mBtnStart.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);

        // AndServer run in the service.
        mServerManager = new ServerManager(this);
        mServerManager.register();

        // startServer;
        if (NetUtils.getNetworkAvailableType(this) == 0) {
//            mBtnStart.performClick();
        } else {
            mTvMessage.setText(R.string.no_wlan);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mServerManager.unRegister();
        mServerManager.stopServer();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_start: {
                if (NetUtils.getNetworkAvailableType(this) != 0) {
                    Toast.makeText(this, "远程管理需要连接WLAN", Toast.LENGTH_SHORT).show();
                    return;
                }
                showDialog();
                mServerManager.startServer();
                break;
            }
            case R.id.btn_stop: {
                showDialog();
                mServerManager.stopServer();
                break;
            }
        }
    }

    /**
     * Start notify.
     */
    public void onServerStart(String ip) {
        closeDialog();
        mBtnStart.setVisibility(View.GONE);
        mBtnStop.setVisibility(View.VISIBLE);

        if (!TextUtils.isEmpty(ip)) {
            List<String> addressList = new LinkedList<>();
            mRootUrl = ip + ":" + CoreService.PORT;
            addressList.add("当前连接WLAN:" + NetUtils.getConnectWifiSsid(this));
            addressList.add(mRootUrl);
            mTvMessage.setText(TextUtils.join("\n", addressList));
        } else {
            mRootUrl = null;
            mTvMessage.setText(R.string.server_ip_error);
        }
    }

    /**
     * Error notify.
     */
    public void onServerError(String message) {
        closeDialog();
        mRootUrl = null;
        mBtnStart.setVisibility(View.VISIBLE);
        mBtnStop.setVisibility(View.GONE);
        mTvMessage.setText(message);
        mServerManager.stopServer();
    }

    /**
     * Stop notify.
     */
    public void onServerStop() {
        closeDialog();
        mRootUrl = null;
        mBtnStart.setVisibility(View.VISIBLE);
        mBtnStop.setVisibility(View.GONE);
        mTvMessage.setText(R.string.server_stop_succeed);
    }

    private synchronized void showDialog() {
        if (mDialog == null) mDialog = new LoadingDialog(this);
        if (!mDialog.isShowing()) mDialog.show();
    }

    private void closeDialog() {
        if (mDialog != null && mDialog.isShowing()) mDialog.dismiss();
    }
}