package com.onlyknow.app.ui.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.onlyknow.app.OKConstant;
import com.onlyknow.app.R;
import com.onlyknow.app.api.OKBusinessApi;
import com.onlyknow.app.database.OKDatabaseHelper;
import com.onlyknow.app.database.bean.OKCardAndCommentBean;
import com.onlyknow.app.database.bean.OKCardBean;
import com.onlyknow.app.database.bean.OKCardBindBean;
import com.onlyknow.app.database.bean.OKSearchBean;
import com.onlyknow.app.database.bean.OKUserInfoBean;
import com.onlyknow.app.ui.OKBaseActivity;
import com.onlyknow.app.ui.view.OKCircleImageView;
import com.onlyknow.app.ui.view.OKSEImageView;
import com.onlyknow.app.utils.OKDeviceInfoUtil;
import com.onlyknow.app.utils.OKLogUtil;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OKCardWZActivity extends OKBaseActivity {
    private OKCircleImageView imageHeadPortrait;
    private TextView textNickName, textSignature, textContentTitle, textContent, textZan, textWatch, textComment, textTag, textLink, textDate;
    private Button buttonAttention;
    private OKSEImageView imageViewZAN, imageViewSC, imageViewPL;
    private LinearLayout linearLayoutZY;

    private OKCardBean mCardBean;
    private int mInterfaceType, mPosition, mCardId;

    private CardBindTask mCardBindTask;
    private OKCardBindBean mCardBindBean;
    private CardTask mCardTask;

    private UMShareListener mShareListener = new UMShareListener() {

        @Override
        public void onStart(SHARE_MEDIA share_media) {
        }

        @Override
        public void onResult(SHARE_MEDIA share_media) {
            showSnackbar(linearLayoutZY, "分享成功", "");
        }

        @Override
        public void onError(SHARE_MEDIA share_media, Throwable throwable) {
            showSnackbar(linearLayoutZY, "分享失败", "ErrorCode: " + throwable.getMessage());
        }

        @Override
        public void onCancel(SHARE_MEDIA share_media) {
            showSnackbar(linearLayoutZY, "分享取消", "");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ok_activity_card_text);
        initUserInfoSharedPreferences();
        initSettingSharedPreferences();
        initSystemBar(this);
        mInterfaceType = getIntent().getExtras().getInt(INTENT_KEY_INTERFACE_TYPE);
        mPosition = getIntent().getExtras().getInt(INTENT_KEY_LIST_POSITION);
        mCardId = getIntent().getExtras().getInt(INTENT_KEY_LIST_CARD_ID);
        mCardBean = loadCardBean();

        findView();
        init();
        addCardBrowsing();
    }

    private OKCardBean loadCardBean() {
        if (mInterfaceType == INTERFACE_CARD_AND_COMMENT) {
            List<OKCardAndCommentBean> list = OKConstant.getListCache(INTERFACE_CARD_AND_COMMENT);
            if (list.size() == 0 || mPosition >= list.size()) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            OKCardAndCommentBean bean = list.get(mPosition);
            if (bean == null || bean.getOKCardBean() == null) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            mCardBean = bean.getOKCardBean();
            if (mCardBean == null || mCardBean.getCARD_ID() != mCardId) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            mCardBean.setIS_READ(true);
            mCardBean.setREAD_DATE(OKConstant.getNowDate());
            bean.setOKCardBean(mCardBean);
            list.set(mPosition, bean);
        } else if (mInterfaceType == INTERFACE_SEARCH) {
            List<OKSearchBean> list = OKConstant.getListCache(INTERFACE_SEARCH);
            if (list.size() == 0 || mPosition >= list.size()) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            OKSearchBean searchBean = list.get(mPosition);
            if (searchBean == null || searchBean.getCardBean() == null) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            mCardBean = searchBean.getCardBean();
            if (mCardBean == null || mCardBean.getCARD_ID() != mCardId) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            mCardBean.setIS_READ(true);
            mCardBean.setREAD_DATE(OKConstant.getNowDate());
            searchBean.setCardBean(mCardBean);
            list.set(mPosition, searchBean);
        } else {
            List<OKCardBean> list = OKConstant.getListCache(mInterfaceType);
            if (list.size() == 0 || mPosition >= list.size()) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            mCardBean = list.get(mPosition);
            if (mCardBean == null || mCardBean.getCARD_ID() != mCardId) {
                finish();
                showSnackbar(linearLayoutZY, "数据源错误", "ErrorCode :" + OKConstant.DATA_SOURCE_ERROR);
                return null;
            }
            mCardBean.setIS_READ(true);
            mCardBean.setREAD_DATE(OKConstant.getNowDate());
            list.set(mPosition, mCardBean);
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    OKDatabaseHelper helper = OKDatabaseHelper.getHelper(OKCardWZActivity.this);
                    helper.getCardDao().createOrUpdate(mCardBean);
                } catch (SQLException e) {
                    e.printStackTrace();
                    OKLogUtil.print("卡片记录更新错误 ErrorMsg :" + e.getMessage());
                }
            }
        }.start();
        return mCardBean;
    }

    @Override
    public void onResume() {
        mCardBindTask = new CardBindTask();
        Map<String, String> map = new HashMap<>();
        map.put("username", USER_INFO_SP.getString(OKUserInfoBean.KEY_USERNAME, ""));
        map.put("username2", mCardBean.getUSER_NAME());
        map.put("card_id", Integer.toString(mCardBean.getCARD_ID()));
        mCardBindTask.executeOnExecutor(exec, map);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCardBindTask != null && mCardBindTask.getStatus() == AsyncTask.Status.RUNNING) {
            mCardBindTask.cancel(true); // 如果线程已经在执行则取消执行
        }

        if (mCardTask != null && mCardTask.getStatus() == AsyncTask.Status.RUNNING) {
            mCardTask.cancel(true); // 如果线程已经在执行则取消执行
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mToolbar != null) {
            mToolbar.setTitle("");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ok_menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_message_jubao) {
            Intent intent = new Intent();
            intent.setClass(OKCardWZActivity.this, OKRePortActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("JUBAO_TYPE", "CARD");
            bundle.putString("JUBAO_CARD_ID", Integer.toString(mCardBean.getCARD_ID()));
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    private void findView() {
        super.findCommonToolbarView(this);
        setSupportActionBar(mToolbar);

        mToolbarBack.setVisibility(View.VISIBLE);
        mToolbarSharing.setVisibility(View.VISIBLE);
        mToolbarTitle.setVisibility(View.VISIBLE);

        imageHeadPortrait = (OKCircleImageView) findViewById(R.id.MESSAGE_top_biaoti_imag);
        textNickName = (TextView) findViewById(R.id.MESSAGE_top_biaoti_text);
        textSignature = (TextView) findViewById(R.id.MESSAGE_top_qianmin_text);
        textContentTitle = (TextView) findViewById(R.id.message_neiron_biaoti_text);
        textContent = (TextView) findViewById(R.id.message_neiron_text);
        textZan = (TextView) findViewById(R.id.MESSAGE_top_zan_text);
        textWatch = (TextView) findViewById(R.id.MESSAGE_top_shouchang_text);
        textComment = (TextView) findViewById(R.id.MESSAGE_top_pinglun_text);
        textTag = (TextView) findViewById(R.id.MESSAGE_biaoqian_text);
        buttonAttention = (Button) findViewById(R.id.MESSAGE_top_guanzhu_but);
        textLink = (TextView) findViewById(R.id.message_link_text);
        textDate = (TextView) findViewById(R.id.message_date_text);
        linearLayoutZY = (LinearLayout) findViewById(R.id.MESSAGE_top_zhuye_layout);
        imageViewZAN = (OKSEImageView) findViewById(R.id.MESSAGE_top_zan_imag);
        imageViewSC = (OKSEImageView) findViewById(R.id.MESSAGE_top_shouchang_imag);
        imageViewPL = (OKSEImageView) findViewById(R.id.MESSAGE_top_pinglun_imag);
    }

    private void init() {
        if (mInterfaceType != INTERFACE_HISTORY) {
            List<OKCardBean> mHistoryList = OKConstant.getListCache(INTERFACE_HISTORY);
            if (mHistoryList == null || mHistoryList.size() == 0) {
                mHistoryList = new ArrayList<>();
                mHistoryList.add(mCardBean);
                OKConstant.putListCache(INTERFACE_HISTORY, mHistoryList);
            } else {
                for (int i = 0; i < mHistoryList.size(); i++) {
                    OKCardBean mCB = mHistoryList.get(i);
                    if (mCardBean.getCARD_ID() == mCB.getCARD_ID()) {
                        mHistoryList.set(i, mCardBean);
                        break;
                    } else if (i == mHistoryList.size() - 1) {
                        mHistoryList.add(mCardBean);
                        OKConstant.putListCache(INTERFACE_HISTORY, mHistoryList);
                    }
                }
            }
        }
        // 字体设置
        if (SETTING_SP.getString("FONT", "NORM").equals("MAX")) {
            textContent.setTextSize(25);
        } else if (SETTING_SP.getString("FONT", "NORM").equals("CENTRE")) {
            textContent.setTextSize(22);
        } else if (SETTING_SP.getString("FONT", "NORM").equals("MIN")) {
            textContent.setTextSize(16);
        } else {
            textContent.setTextSize(20);
        }
        // 获取标题图片信息
        GlideRoundApi(imageHeadPortrait, mCardBean.getTITLE_IMAGE_URL(), R.drawable.touxian_placeholder, R.drawable.touxian_placeholder);
        if (mCardBean.getCONTENT_TITLE_TEXT().length() >= 12) {
            mToolbarTitle.setText(mCardBean.getCONTENT_TITLE_TEXT().substring(0, 12) + "...");
        } else {
            mToolbarTitle.setText(mCardBean.getCONTENT_TITLE_TEXT());
        }
        textNickName.setText(mCardBean.getTITLE_TEXT());
        textSignature.setText("这个人很懒 , 什么都没有留下!");
        textZan.setText("" + mCardBean.getZAN_NUM());
        textWatch.setText("" + mCardBean.getSHOUCHAN_NUM());
        textComment.setText("" + mCardBean.getPINGLUN_NUM());
        textTag.setText(mCardBean.getLABELLING());
        textContentTitle.setText(mCardBean.getCONTENT_TITLE_TEXT());
        textContent.setText(mCardBean.getCONTENT_TEXT());
        textLink.setText(mCardBean.getMESSAGE_LINK());
        textDate.setText(formatTime(mCardBean.getCREATE_DATE()) + " 发表");

        mToolbarSharing.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ShareAction mShareAction = new ShareAction(OKCardWZActivity.this);
                String content = mCardBean.getCONTENT_TITLE_TEXT() + "\r\n" + mCardBean.getCONTENT_TEXT();
                mShareAction.withText(content).setDisplayList(SHARE_MEDIA.SINA, SHARE_MEDIA.QQ, SHARE_MEDIA.WEIXIN).setCallback(mShareListener).open();
            }
        });

        buttonAttention.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (USER_INFO_SP.getBoolean("STATE", false)) {
                    mCardTask = new CardTask("ATTENTION");
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("username", USER_INFO_SP.getString(OKUserInfoBean.KEY_USERNAME, ""));
                    params.put("username2", mCardBean.getUSER_NAME());
                    params.put("card_id", "");
                    params.put("message", "");
                    params.put("date", OKConstant.getNowDate());
                    params.put("type", "GUANZHU");
                    mCardTask.executeOnExecutor(exec, params);
                } else {
                    startUserActivity(null, OKLoginActivity.class);
                }
            }
        });

        mToolbarBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                OKCardWZActivity.this.finish();
            }
        });

        linearLayoutZY.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mInterfaceType != INTERFACE_HOME) {
                    Bundle bundle = new Bundle();
                    bundle.putString(OKUserInfoBean.KEY_USERNAME, mCardBean.getUSER_NAME());
                    bundle.putString(OKUserInfoBean.KEY_NICKNAME, mCardBean.getTITLE_TEXT());
                    startUserActivity(bundle, OKHomePageActivity.class);
                } else {
                    finish();
                }
            }
        });

        imageViewZAN.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (USER_INFO_SP.getBoolean("STATE", false)) {
                    if (mCardBindBean != null && mCardBindBean.IS_ZAN()) {
                        showSnackbar(v, "您已经点赞了", "");
                        return;
                    }

                    mCardTask = new CardTask("ZAN");
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("username", USER_INFO_SP.getString(OKUserInfoBean.KEY_USERNAME, ""));
                    params.put("username2", "");
                    params.put("card_id", Integer.toString(mCardBean.getCARD_ID()));
                    params.put("message", "");
                    params.put("date", OKConstant.getNowDate());
                    params.put("type", "ZAN");
                    mCardTask.executeOnExecutor(exec, params);
                } else {
                    startUserActivity(null, OKLoginActivity.class);
                }
            }
        });

        imageViewSC.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (USER_INFO_SP.getBoolean("STATE", false)) {
                    if (mCardBindBean != null && mCardBindBean.IS_WATCH()) {
                        showSnackbar(v, "您已经收藏了", "");
                        return;
                    }

                    mCardTask = new CardTask("WATCH");
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("username", USER_INFO_SP.getString(OKUserInfoBean.KEY_USERNAME, ""));
                    params.put("username2", "");
                    params.put("card_id", Integer.toString(mCardBean.getCARD_ID()));
                    params.put("message", "");
                    params.put("date", OKConstant.getNowDate());
                    params.put("type", "SHOUCHAN");
                    mCardTask.executeOnExecutor(exec, params);
                } else {
                    startUserActivity(null, OKLoginActivity.class);
                }
            }
        });

        imageViewPL.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(OKUserInfoBean.KEY_USERNAME, mCardBean.getUSER_NAME());
                bundle.putInt(OKCardBean.KEY_CARD_ID, mCardBean.getCARD_ID());
                bundle.putInt("DH_ID", mInterfaceType);
                bundle.putInt("POS", mPosition);
                startUserActivity(bundle, OKCommentActivity.class);
            }
        });

        textLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("WEB_LINK", textLink.getText().toString());
                Intent intent = new Intent();
                intent.setClass(OKCardWZActivity.this, OKBrowserActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    private void addCardBrowsing() {
        new Thread() {
            @Override
            public void run() {
                OKDeviceInfoUtil equipmentInformation = new OKDeviceInfoUtil(OKCardWZActivity.this);
                String equipment = equipmentInformation.getIMIE();

                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm");

                Map<String, String> map = new HashMap<String, String>();
                map.put("card_id", Integer.toString(mCardBean.getCARD_ID()));
                map.put("username", USER_INFO_SP.getString(OKUserInfoBean.KEY_USERNAME, "Anonymous"));
                map.put("equipment", equipment);
                map.put("date", dateFormat.format(now));
                new OKBusinessApi().addCardBrowsing(map);
            }
        }.start();
    }

    private class CardTask extends AsyncTask<Map<String, String>, Void, Boolean> {
        private String Type = "";

        public CardTask(String type) {
            this.Type = type;
        }

        @Override
        protected Boolean doInBackground(Map<String, String>... params) {
            if (isCancelled()) {
                return false;
            }
            return new OKBusinessApi().updateCardInfo(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (isCancelled()) {
                return;
            }

            if (aBoolean) {
                if (Type.equals("ATTENTION")) {
                    if (mCardBindBean == null) {
                        mCardBindBean = new OKCardBindBean();
                    }
                    mCardBindBean.setIS_ATTENTION(true);

                    buttonAttention.setText("已关注");
                    buttonAttention.setEnabled(false);
                } else if (Type.equals("WATCH")) {
                    if (mCardBindBean == null) {
                        mCardBindBean = new OKCardBindBean();
                    }
                    mCardBindBean.setIS_WATCH(true);

                    int count = mCardBean.getSHOUCHAN_NUM() + 1;
                    textWatch.setText("" + count);
                    textWatch.setTextColor(getResources().getColor(R.color.fenhon));
                } else if (Type.equals("ZAN")) {
                    if (mCardBindBean == null) {
                        mCardBindBean = new OKCardBindBean();
                    }
                    mCardBindBean.setIS_ZAN(true);
                    int count = mCardBean.getZAN_NUM() + 1;
                    textZan.setText("" + count);
                    textZan.setTextColor(getResources().getColor(R.color.fenhon));
                }
            } else {
                showSnackbar(linearLayoutZY, "操作失败,请重试", "");
            }
        }
    }

    private class CardBindTask extends AsyncTask<Map<String, String>, Void, OKCardBindBean> {

        @Override
        protected OKCardBindBean doInBackground(Map<String, String>... params) {
            if (isCancelled()) {
                return null;
            }

            return new OKBusinessApi().getCardBind(params[0]);
        }

        @Override
        protected void onPostExecute(OKCardBindBean cardBindBean) {
            super.onPostExecute(cardBindBean);
            if (isCancelled() || cardBindBean == null) {
                return;
            }

            mCardBindBean = cardBindBean;

            if (mCardBindBean.isCardRemove()) {
                imageViewZAN.setEnabled(false);
                imageViewSC.setEnabled(false);
                imageViewPL.setEnabled(false);
                mToolbarTitle.setText("该卡片已被用户删除");
                showSnackbar(linearLayoutZY, "该卡片已被用户删除", "");
            }

            if (mCardBindBean.IS_ATTENTION()) {
                buttonAttention.setText("已关注");
                buttonAttention.setEnabled(false);
            }

            if (mCardBindBean.IS_WATCH()) {
                textWatch.setTextColor(getResources().getColor(R.color.fenhon));
            }
            if (mCardBindBean.IS_ZAN()) {
                textZan.setTextColor(getResources().getColor(R.color.fenhon));
            }
            if (TextUtils.isEmpty(mCardBindBean.getQIANMIN()) || mCardBindBean.getQIANMIN().equals("NULL")) {
                textSignature.setText("这个人很懒 , 什么都没有留下!");
            } else {
                textSignature.setText(mCardBindBean.getQIANMIN());
            }
            textZan.setText("" + mCardBindBean.getZAN_COUNT());
            textWatch.setText("" + mCardBindBean.getWATCH_COUNT());
            textComment.setText("" + mCardBindBean.getPINLUN_COUNT());
        }
    }
}
