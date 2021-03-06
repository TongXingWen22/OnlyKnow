package com.onlyknow.app.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;

import com.caimuhao.rxpicker.RxPicker;
import com.caimuhao.rxpicker.bean.ImageItem;
import com.caimuhao.rxpicker.utils.RxPickerImageLoader;
import com.onlyknow.app.GlideApp;
import com.onlyknow.app.OKConstant;
import com.onlyknow.app.R;
import com.onlyknow.app.api.OKBusinessApi;
import com.onlyknow.app.database.bean.OKUserInfoBean;
import com.onlyknow.app.ui.OKBaseActivity;
import com.onlyknow.app.ui.view.OKSEImageView;
import com.onlyknow.app.utils.OKBase64Util;
import com.onlyknow.app.utils.OKDeviceInfoUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class OKFeedBackActivity extends OKBaseActivity {
    private AppCompatButton mAppCompatButtonSend;
    private OKSEImageView mImageViewAddTuPian, mImageViewClear;
    private EditText mEditTextNeiRon;

    private String mFilePath = "";

    private FeedBackTask mFeedBackTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ok_activity_feedback);
        initUserInfoSharedPreferences();
        initSystemBar(this);
        findView();
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFeedBackTask != null && mFeedBackTask.getStatus() == AsyncTask.Status.RUNNING) {
            mFeedBackTask.cancel(true);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mToolbar != null) {
            mToolbar.setTitle("");
        }
    }

    private void init() {
        mImageViewAddTuPian.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                RxPicker.init(new LoadImage());
                RxPicker.of().single(false).camera(true).limit(1, 1).start(OKFeedBackActivity.this).subscribe(new ImageSelectResult());
            }
        });

        mAppCompatButtonSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (USER_INFO_SP.getBoolean("STATE", false)) {
                    if (mEditTextNeiRon.getText().toString().length() >= 100) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("username", USER_INFO_SP.getString(OKUserInfoBean.KEY_USERNAME, "Anonymous"));
                        map.put("equipment", new OKDeviceInfoUtil(OKFeedBackActivity.this).getIMIE());
                        map.put("message", mEditTextNeiRon.getText().toString());
                        map.put("baseimag", mFilePath);
                        map.put("date", OKConstant.getNowDate());
                        mFeedBackTask = new FeedBackTask();
                        mFeedBackTask.executeOnExecutor(exec, map);
                        showProgressDialog("正在提交信息...");
                    } else {
                        showSnackbar(v, "反馈意见必须大于100字符", "");
                    }
                } else {
                    startUserActivity(null, OKLoginActivity.class);
                }
            }
        });

        mImageViewClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mFilePath = "";
                GlideApi(mImageViewAddTuPian, R.drawable.add_image_black, R.drawable.add_image_black, R.drawable.add_image_black);
            }
        });

        mToolbarBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void findView() {
        super.findCommonToolbarView(this);
        setSupportActionBar(mToolbar);
        mToolbarBack.setVisibility(View.VISIBLE);
        mToolbarTitle.setVisibility(View.VISIBLE);
        mToolbarTitle.setText("意见反馈");
        mAppCompatButtonSend = (AppCompatButton) findViewById(R.id.Feedback_TiJiaoBtn);
        mImageViewAddTuPian = (OKSEImageView) findViewById(R.id.Feedback_input_imag);
        mImageViewClear = (OKSEImageView) findViewById(R.id.Feedback_clear_imag);
        mEditTextNeiRon = (EditText) findViewById(R.id.Feedback_input_text);
    }

    private class FeedBackTask extends AsyncTask<Map<String, String>, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Map<String, String>... params) {
            if (isCancelled()) {
                return false;
            }

            Map<String, String> map = params[0];
            String filePath = map.get("baseimag");
            if (!TextUtils.isEmpty(filePath)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;
                options.inSampleSize = 1;
                Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                map.put("baseimag", OKBase64Util.BitmapToBase64(bitmap));
            }
            return new OKBusinessApi().feedBack(map);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (isCancelled()) {
                return;
            }
            if (aBoolean) {
                showSnackbar(mAppCompatButtonSend, "反馈成功", "");
            } else {
                showSnackbar(mAppCompatButtonSend, "反馈失败,请检查网络", "");
            }
            closeProgressDialog();
        }
    }

    private class LoadImage implements RxPickerImageLoader {
        @Override
        public void display(ImageView imageView, String path, int width, int height) {
            GlideApp.with(imageView.getContext()).load(path).error(R.drawable.add_image_black).centerCrop().override(width, height).into(imageView);
        }
    }

    private class ImageSelectResult implements Consumer<List<ImageItem>> {
        @Override
        public void accept(@NonNull List<ImageItem> imageItems) throws Exception {
            if (imageItems == null || imageItems.size() == 0) {
                showSnackbar(mToolbarAddImage, "未获选择图片", "");
                return;
            }
            String fp = imageItems.get(0).getPath();
            String gs = fp.substring(fp.lastIndexOf(".") + 1, fp.length());
            if (gs.equalsIgnoreCase("gif")) {
                showSnackbar(mAppCompatButtonSend, "您不能选择动图", "");
                return;
            }
            mFilePath = imageItems.get(0).getPath();
            GlideApi(mImageViewAddTuPian, mFilePath, R.drawable.add_image_black, R.drawable.add_image_black);
        }
    }
}
