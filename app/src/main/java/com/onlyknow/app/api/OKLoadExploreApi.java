package com.onlyknow.app.api;

import android.content.Context;
import android.os.AsyncTask;

import com.onlyknow.app.OKConstant;
import com.onlyknow.app.database.bean.OKCardBean;
import com.onlyknow.app.database.OKDatabaseHelper;
import com.onlyknow.app.utils.OKNetUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/12/22.
 */

public class OKLoadExploreApi extends OKBaseApi {
    private onCallBack mOnCallBack;
    private Context context;
    private LoadCardListTask mLoadCardListTask;

    public OKLoadExploreApi(Context con) {
        this.context = con;
    }

    public interface onCallBack {
        void exploreApiComplete(List<OKCardBean> list);
    }

    public void requestCardBeanList(Map<String, String> param, onCallBack mCallBack) {
        this.mOnCallBack = mCallBack;
        cancelTask();
        mLoadCardListTask = new LoadCardListTask();
        mLoadCardListTask.executeOnExecutor(exec, param);
    }

    public void cancelTask() {
        if (mLoadCardListTask != null && mLoadCardListTask.getStatus() == AsyncTask.Status.RUNNING) {
            mLoadCardListTask.cancel(true);
        }
    }

    private class LoadCardListTask extends AsyncTask<Map<String, String>, Void, List<OKCardBean>> {

        @Override
        protected List<OKCardBean> doInBackground(Map<String, String>... params) {
            if (isCancelled()) {
                return null;
            }
            if (OKNetUtil.isNet(context)) {
                OKBusinessApi mOKBusinessApi = new OKBusinessApi();
                List<OKCardBean> exploreCardList = mOKBusinessApi.getExploreCard(params[0]);
                if (exploreCardList != null) {
                    OKDatabaseHelper helper = OKDatabaseHelper.getHelper(context);
                    for (OKCardBean mCardBean : exploreCardList) {
                        try {
                            OKCardBean dbBean = helper.getCardDao().queryForId(mCardBean.getCARD_ID());
                            if (dbBean != null) {
                                mCardBean.setIS_READ(dbBean.IS_READ());
                                mCardBean.setREAD_DATE(dbBean.getREAD_DATE());
                                helper.getCardDao().createOrUpdate(mCardBean);
                            } else {
                                helper.getCardDao().create(mCardBean);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    return cardBeanListSorting(exploreCardList);
                }
            }
            return cardBeanListSorting(getDBCard(OKConstant.EXPLORE_COUNT));
        }

        @Override
        protected void onPostExecute(List<OKCardBean> okCardBeen) {
            if (isCancelled()) {
                return;
            }

            super.onPostExecute(okCardBeen);
            mOnCallBack.exploreApiComplete(okCardBeen);
        }
    }

    private List<OKCardBean> cardBeanListSorting(List<OKCardBean> aims) {
        if (OKConstant.getListCache(INTERFACE_EXPLORE) == null) {
            return aims;
        }
        List<OKCardBean> source = OKConstant.getListCache(INTERFACE_EXPLORE);
        for (int i = 0; i < source.size(); i++) {
            OKCardBean sourceBean = source.get(i);
            for (int p = 0; p < aims.size(); p++) {
                OKCardBean aimsBean = aims.get(p);
                if (sourceBean.getCARD_ID() == aimsBean.getCARD_ID()) {
                    source.set(i, aimsBean);
                    aims.remove(p);
                    break;
                }
            }
        }
        return aims;
    }

    private List<OKCardBean> getDBCard(long num) {
        // 加载本地数据 随机加载num条数据
        OKDatabaseHelper helper = OKDatabaseHelper.getHelper(context);
        try {
            List<OKCardBean> dbList = helper.getCardDao().queryBuilder().limit(num).query();
            return dbList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
