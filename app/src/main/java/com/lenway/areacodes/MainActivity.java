package com.lenway.areacodes;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity
{
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnGet = findViewById(R.id.btn_get);
        btnGet.setOnClickListener(mListener);

        try {
            String[] PERMISSIONS_STORAGE = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE" };
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(this,
                "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener mListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
            case R.id.btn_get:
                EditText edt = findViewById(R.id.edt_url);
                final String url = edt.getText().toString().trim();
                if (url != "")
                {
                    // 线程
                    Thread threadEn = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            String parentName = "";
                            final StringBuffer sb = new StringBuffer();
                            final SortedMap<String,String> mapCodes = new TreeMap<>();
                            final boolean success = jsoupCodes(mapCodes, url, parentName, true);

                            //获取键值Set
                            Set<String> keySet= mapCodes.keySet();
                            //将键值Set转成数组
                            Object[] keyArray = keySet.toArray();
                            //按照键值依序获取值对象
                            for(int i=0; i< keyArray.length; i++)
                            {
                                if (sb.length() > 0)
                                    sb.append("\r\n");

                                sb.append(keyArray[i] + " " + mapCodes.get(keyArray[i]));
                            }

                            mHandler.post(new Runnable() {
                                public void run() {
                                    appendData(sb.toString());

                                    showData("完成抓取数据" + (success ? "" : ", 发现异常退出"));
                                }
                            });
                        }
                    });
                    threadEn.start();
                }
                break;
            }
        }
    };

    private void showData(String str)
    {
        if (str == null || str.length() == 0)
            return;

        TextView txt = findViewById(R.id.txt_data);
        StringBuffer sb = new StringBuffer();
        sb.append(txt.getText().toString());
        if (sb.length() > 0)
        {
            sb.append("\r\n");
        }
        sb.append(str);
        txt.setText(sb.toString());
    }

    private void appendData(String strConentString) {
        // /storage/emulated/0/Android/data/包名/files/codes.txt
        String strFile =  getExternalFilesDir("").getAbsolutePath() + "/codes.txt";

        File fileExist = new File(strFile);
        if (fileExist.exists())
            fileExist.delete();

        if (true)
        {
            java.io.BufferedWriter out = null;
            try
            {
                out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(strFile, true)));
                out.write(strConentString);
                out.flush();
                out.close();
                out = null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (out != null)
                        out.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean jsoupCodes(SortedMap<String,String> mapCodes,  String url, String parentName, boolean errorBreak)
    {
        String strParentName = parentName.length() > 0 ? (" " + parentName + " ") : "";
        Log.e("codes", "正在抓取" + strParentName + "行政区划代码...");

        // TreeMap 排序
        try {
            Document doc = null;
            for (int time = 0; time < 10; time++)
            {
                try
                {
                    // 网络加载HTML文档
                    doc = Jsoup.connect(url)
                        .timeout(60000) // 设置超时时间
                        .get(); // 使用GET方法访问URL
                    break;
                }
                catch (Exception e)
                {
                    Log.e("codes", "抓取异常:" + url);
                    e.printStackTrace();
                    Thread.sleep(1000);
                }
            }

            if (doc == null)
            {
                return errorBreak ? false : true;
            }

            // provincetr, citytr, countytr, towntr, villagetr
            List<String> lst = Arrays.asList("provincetr", "citytr", "countytr", "towntr", "villagetr");
            Element eParent = doc.body();
            String baseUrl = eParent.baseUri();
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1);
            List<String> subUrls = new ArrayList<>();
            List<String> subNames = new ArrayList<>();
            for (int i = 0; i < lst.size(); i++)
            {
                String trClass = lst.get(i);
                Elements elements = eParent.select("tr." + trClass);
                if (elements != null && elements.size() > 0)
                {
                    for (Element element:elements){
                        Elements tds = element.select("td");
                        if (trClass == "provincetr")
                        {
                            // 省
                            for (Element eTd:tds)
                            {
                                String name = eTd.select("a").text();
                                String href = eTd.select("a").attr("href");
                                String code = href.replace(".html", "000000000");
                                if (!mapCodes.containsKey(code))
                                {
                                    mapCodes.put(code, name);
                                }

                                if (href != null && href != "")
                                {
                                    subNames.add(name);
                                    subUrls.add(baseUrl + href);
                                    Log.e("codes", "name:" + (parentName + name) + ", code:" + code + ", href:" + href);
                                }
                            }
                        }
                        else if (trClass == "villagetr")
                        {
                            // 村
                            String code = tds.get(0).text();
                            String name = tds.get(2).text();
                            if (!mapCodes.containsKey(code))
                            {
                                mapCodes.put(code, name);
                            }
                            Log.e("codes", "name:" + (parentName + name) + ", code:" + code);
                        }
                        else
                        {
                            // 市、县、镇
                            String code = tds.get(0).select("a").text();
                            String href = tds.get(0).select("a").attr("href");
                            String name = tds.get(1).select("a").text();
                            if (code == null || code == "" || name == null || name == "")
                            {
                                code = tds.get(0).text();
                                name = tds.get(1).text();
                            }

                            if (!mapCodes.containsKey(code))
                            {
                                mapCodes.put(code, name);
                            }

                            if (href != null && href != "")
                            {
                                subNames.add(name);
                                subUrls.add(baseUrl + href);
                                Log.e("codes", "name:" + (parentName + name) + ", code:" + code + ", href:" + href);
                            }
                        }
                    }
                }
            }

            System.gc();

            try
            {
                Thread.sleep(200);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            for (int i = 0; i < subUrls.size(); i++)
            {
                boolean success = jsoupCodes(mapCodes, subUrls.get(i), parentName + subNames.get(i), errorBreak);
                if (errorBreak && success == false)
                {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return errorBreak ? false : true;
        }

        return true;
    }
}
