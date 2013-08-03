package com.boombuler.piraten.map;

import android.app.Activity;
import android.app.ProgressDialog;
import com.boombuler.piraten.utils.MyHttpClient;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;


public class FetchServerList implements Runnable {

    private Activity mContext;
    private String mUrl;
    private Runnable mOnCompleteListener;
    private ProgressDialog mProgressDlg;

    public FetchServerList(Activity context) {
        mContext = context;
        mUrl = mContext.getString(R.string.server_list_url);
    }

    @Override
    public void run() {
        DBAdapter dba = new DBAdapter(mContext);
        try
        {
            MyHttpClient client = new MyHttpClient(mContext);
            HttpResponse response = client.execute(new HttpGet(mUrl));
            JsonObject serverList = JsonObject.readFrom(EntityUtils.toString(response.getEntity(), "UTF-8"));

            dba.open();
            dba.beginTransaction();

            dba.ClearServers();
            JsonArray servers = serverList.get("ServerList").asArray();
            for (JsonValue sVal : servers ) {
                JsonObject server = sVal.asObject();

                dba.InsertServer(server.get("ID").asString(), server.get("Name").asString(),
                        server.get("Info").asString(), server.get("URL").asString());
            }

            JsonArray devIds = serverList.get("Development").asArray();
            for (JsonValue sVal : devIds) {
                dba.SetDevServer(sVal.asString());
            }

            dba.setTransactionSuccessful();
        } catch (Exception e) {
        } finally {
            dba.endTransaction();
            dba.close();
        }
        if (mOnCompleteListener != null)
            mContext.runOnUiThread(mOnCompleteListener);
        if (mProgressDlg != null) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDlg.dismiss();
                }
            });
        }
    }

    public void setOnCompleteListener(Runnable listener) {
        mOnCompleteListener = listener;
    }

    public  void setProgressDlg(ProgressDialog dlg) {
        mProgressDlg = dlg;
    }


    public static void Load(Activity context, Runnable onComplete) {
        final ProgressDialog progressDlg = new ProgressDialog(context);
        progressDlg.setOwnerActivity(context);
        progressDlg.setCancelable(false);
        progressDlg.setCanceledOnTouchOutside(false);
        progressDlg.setIndeterminate(true);
        progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDlg.setTitle(R.string.settings);
        progressDlg.setMessage(context.getString(R.string.fetching_server_list));
        progressDlg.show();

        FetchServerList fetch = new FetchServerList(context);
        fetch.setOnCompleteListener(onComplete);
        fetch.setProgressDlg(progressDlg);
        new Thread(fetch).start();
    }
}
