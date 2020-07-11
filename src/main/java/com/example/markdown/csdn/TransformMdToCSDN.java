package com.example.markdown.csdn;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.example.markdown.common.Constants;
import com.example.markdown.common.SimpleThreadPoolExecutor;
import com.example.markdown.utils.ThreadPoolUtil;
import com.example.markdown.webclient.SimpleHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;


import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author QinHe at 2019-09-27
 */
@Slf4j
public class TransformMdToCSDN {
    private final static SimpleHttpClient httpClient;
    private final static SimpleThreadPoolExecutor threadPoolExecutor;
    private final static String REGEX = "!\\[.*\\]\\((.*)\\)";
    private static Boolean continueThread = false;

    private final static String COOKIE = "uuid_tt_dd=10_21049097490-1590916203218-488716; dc_session_id=10_1590916203218.382273; __gads=ID=2dfe84575b089822:T=1590916205:S=ALNI_MZ2zf2I8ekbZdWhgBA5dD40XKdbqw; UN=lk142500; Hm_ct_6bcd52f51e9b3dce32bec4a3997715ac=6525*1*10_21049097490-1590916203218-488716!5744*1*lk142500; UserName=lk142500; UserInfo=893355363484458186334a2c5d42a09f; UserToken=893355363484458186334a2c5d42a09f; UserNick=%E6%80%9D%E5%BD%B1%E5%BD%B1%E6%80%9D; AU=1B4; BT=1592748401901; p_uid=U000000; Hm_up_6bcd52f51e9b3dce32bec4a3997715ac=%7B%22islogin%22%3A%7B%22value%22%3A%221%22%2C%22scope%22%3A1%7D%2C%22isonline%22%3A%7B%22value%22%3A%221%22%2C%22scope%22%3A1%7D%2C%22isvip%22%3A%7B%22value%22%3A%220%22%2C%22scope%22%3A1%7D%2C%22uid_%22%3A%7B%22value%22%3A%22lk142500%22%2C%22scope%22%3A1%7D%7D; announcement=%257B%2522isLogin%2522%253Atrue%252C%2522announcementUrl%2522%253A%2522https%253A%252F%252Flive.csdn.net%252Froom%252Fcompanyzh%252F5o1Kf1RQ%253Futm_source%253D1593515841%2522%252C%2522announcementCount%2522%253A0%257D; dc_sid=c1a2e19b202e45a55ad957e05a51d0c9; c_first_ref=www.baidu.com; c_utm_medium=fe.tool_bar_second.download.my_resources; c_first_page=https%3A//blog.csdn.net/pengjianbosoft/article/details/7629113; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1593832664,1593832741,1593834589,1593834614; MSG-SESSION=a414fe05-f3b5-4421-b783-2c8c03abb7fd; c_ref=https%3A//blog.csdn.net/lk142500; dc_tos=qcy5js; Hm_lpvt_6bcd52f51e9b3dce32bec4a3997715ac=1593869465";

    static {
        httpClient = new SimpleHttpClient();
        httpClient.init();
        threadPoolExecutor = ThreadPoolUtil.getThreadPoolExecutor();
    }

    public static void main(String[] args) throws Exception{


        //创建一个窗口，创建一个窗口
        CookieFrame cookieFrame = new CookieFrame("请设置用于与CSDN交互的Cookie");
        cookieFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //设置窗口大小
        cookieFrame.setSize(1000, 1000);

        //显示窗口
        cookieFrame.setVisible(true);

        synchronized (cookieFrame) {
            // 如果继续线程为false，则执行循环
            while (StringUtils.isEmpty(cookieFrame.getCookieCSDNStr())) {
                System.out.println("还未完成cookie的设置");
            }
        }


        FileDialog fileDialog = new FileDialog(new Frame(), "打开md文件", FileDialog.LOAD);
        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        String fileName = fileDialog.getFile();

        File fileToUpload = new File(directory, fileName);
        threadPoolExecutor.execute(() -> handleFile(fileToUpload, cookieFrame.getCookieCSDNStr()));

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void handleFile(File file, String csdnCookieStr) {
        try {
            String name = file.getName();
            String fileString = FileUtils.readFileToString(file, Consts.UTF_8);
            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(fileString);
            while (matcher.find()) {
                String path = matcher.group(1);
                log.info("匹配图像路径: " + path);
                String imgPath = file.getParent() + "\\" + Constants.IMG_DIR + "\\" + path;
                log.info(imgPath);
                File pic = new File(imgPath);

                if (pic.exists()) {
                    String url = uploadFileToCSDN(pic, csdnCookieStr);
                    log.info(path + ":" + url);
                    fileString = fileString.replace(path, url);
                }
            }

            File newFile = new File(file.getParent() + "\\csdn_" + name);
            if (!newFile.exists()) {
                log.info("创造新的文件");
                newFile.createNewFile();
            }
            FileUtils.writeStringToFile(newFile, fileString, Consts.UTF_8, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> findAllMdFile(String parentPath) {
        Collection<File> files = FileUtils.listFiles(new File(parentPath), new String[]{"md"}, true);
        return new ArrayList<>(files);
    }


    private static String uploadFileToCSDN(File file, String csdnCookieStr) throws IOException {
        assert !StringUtils.isEmpty(csdnCookieStr);
        log.info("通过Https请求推送图片");
        HashMap headers = new HashMap();
        HashMap params = new HashMap();
        headers.put("cookieCSDNStr", csdnCookieStr);
        params.put("shuiyin", "2");
        HttpResponse httpResponse = httpClient.post(
                "https://blog-console-api.csdn.net/v1/upload/img?shuiyin=2",
                null,
                headers,
                params,
                Collections.singletonList(file)
        );
        String url = "";
        String content = httpClient.getContent(httpResponse);
        log.info("content: " + content);
        if (StringUtils.isNotBlank(content)) {

            JSONObject jsonObject = JSON.parseObject(content);
            if (jsonObject != null) {
                url = jsonObject.getJSONObject("data").getString("url");
            }
        }
        log.info("url: " + url);
        return url;
    }

}

