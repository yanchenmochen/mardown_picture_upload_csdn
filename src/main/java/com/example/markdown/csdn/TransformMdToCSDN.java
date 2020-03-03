package com.example.markdown.csdn;



import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.example.markdown.common.SimpleThreadPoolExecutor;
import com.example.markdown.utils.ThreadPoolUtil;
import com.example.markdown.webclient.SimpleHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;


import java.io.File;
import java.io.IOException;
import java.util.*;
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
    private final static String NEW_FILE_PARENT = "C:\\Users\\wanna\\Desktop";
    private final static String OLD_FILE_PARENT = "C:\\Users\\wanna\\Desktop\\Elasticsearch";
    private final static String COOKIE = "uuid_tt_dd=10_20909159520-1575376627407-209445; dc_session_id=10_1575376627407.467854; UN=lk142500; Hm_ct_6bcd52f51e9b3dce32bec4a3997715ac=6525*1*10_20909159520-1575376627407-209445!5744*1*lk142500; __gads=ID=ae5ef4c2ba9a5b1d:T=1582765794:S=ALNI_MYAk8LCv6pDIW5hvgwD_Xo2LflKDw; UserName=lk142500; UserInfo=f5a3cffb3e7e4be1a83be6a52f534e6a; UserToken=f5a3cffb3e7e4be1a83be6a52f534e6a; UserNick=%E6%80%9D%E5%BD%B1%E5%BD%B1%E6%80%9D; AU=1B4; BT=1583065151756; p_uid=U000000; searchHistoryArray=%255B%2522cmd%2522%255D; announcement=%257B%2522isLogin%2522%253Atrue%252C%2522announcementUrl%2522%253A%2522https%253A%252F%252Fblog.csdn.net%252Fblogdevteam%252Farticle%252Fdetails%252F103603408%2522%252C%2522announcementCount%2522%253A0%252C%2522announcementExpire%2522%253A3600000%257D; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1583209853,1583209971,1583216903,1583216963; c_ref=https%3A//blog.csdn.net/u014229652; Hm_lpvt_6bcd52f51e9b3dce32bec4a3997715ac=1583217290; dc_tos=q6luf8";
    static {
        httpClient = new SimpleHttpClient();
        httpClient.init();
        threadPoolExecutor = ThreadPoolUtil.getThreadPoolExecutor();
    }

    public static void main(String[] args) {
        List<File> allMdFile = findAllMdFile(OLD_FILE_PARENT);
        allMdFile.forEach((file) -> {
            threadPoolExecutor.execute(() -> handleFile(file));
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void handleFile(File file) {
        try {
            String name = file.getName();
            String fileString = FileUtils.readFileToString(file, Consts.UTF_8);
            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(fileString);
            while (matcher.find()) {
                String path = matcher.group(1);
                log.info("匹配图像路径: "+ path);
                String imgPath = OLD_FILE_PARENT + "\\imgs\\" + path;
                log.info(imgPath);
                File pic = new File(imgPath);
                if (pic.exists()) {

                    String url = uploadFileToCSDN(pic);

                    log.info(path + ":" + url);
                    fileString = fileString.replace(path, url);
                }
            }

            File newFile = new File(NEW_FILE_PARENT + name);
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


    private static String uploadFileToCSDN(File file) throws IOException {
        log.info("通过Https请求推送图片");
        HashMap headers = new HashMap();
        HashMap params = new HashMap();
        headers.put("cookie", COOKIE);
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

