package com.example.markdown.webclient;


import com.example.markdown.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * @author QinHe at 2019-09-19
 */
@SuppressWarnings("DuplicatedCode")
@Slf4j
@Component
public class SimpleHttpClient {

    private CloseableHttpClient httpClient;

    @PostConstruct
    public void init() {
        if (httpClient == null) {
            try {
                httpClient = createClient();
                log.info("start apache simple http client ... ");
            } catch (Exception e) {
                e.printStackTrace();
                log.info("start apache simple http client failed ... ");
            }
        }
    }

    private CloseableHttpClient createClient() throws NoSuchAlgorithmException, KeyManagementException, IOReactorException {

        //绕过证书验证，处理https请求
        SSLContext sslContext = createIgnoreVerifySSL();

        // 设置协议http和https对应的处理socket链接工厂的对象
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext, null, null, NoopHostnameVerifier.INSTANCE))
                .build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
        //最大连接数3000
        connManager.setMaxTotal(3000);
        //路由链接数400
        connManager.setDefaultMaxPerRoute(400);

        return HttpClientBuilder
                .create()
//                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setConnectionManager(connManager)
//                .setSSLContext(sslContext)
                .setDefaultRequestConfig(getDefaultRequestConfigBuilder().build())
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(200)
                .build();
    }

    /**
     * 设置信任自定义的证书
     *
     * @param keyStorePath 密钥库路径
     * @param keyStorepass 密钥库密码
     * @return
     */
    public static SSLContext custom(String keyStorePath, String keyStorepass) {
        SSLContext sc = null;
        FileInputStream instream = null;
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            instream = new FileInputStream(new File(keyStorePath));
            trustStore.load(instream, keyStorepass.toCharArray());
            // 相信自己的CA和所有自签名的证书
            sc = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
            e.printStackTrace();
        } finally {
            try {
                instream.close();
            } catch (IOException e) {
            }
        }
        return sc;
    }

    /**
     * 绕过验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws KeyStoreException
     */
    private SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] certificate, String authType) {
                return true;
            }
        };
        return SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
    }

    /**
     * 绕过验证2
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        sc.init(null, new TrustManager[]{trustManager}, null);
        return sc;
    }

    private RequestConfig.Builder getDefaultRequestConfigBuilder() {
        //2秒连接  5秒读取 5秒从连接池拿连接  超过了 均为超时
        return RequestConfig.custom()
                .setConnectTimeout(2000)
                .setSocketTimeout(30000)
                .setCircularRedirectsAllowed(true)
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setConnectionRequestTimeout(5000);
    }

    public HttpResponse get(String url, HttpHost proxy) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("user-agent", Constants.userAgentArray[new Random().nextInt(Constants.userAgentArray.length)]);
        if (proxy != null) {
            RequestConfig requestConfig = getDefaultRequestConfigBuilder().setProxy(proxy).build();
            request.setConfig(requestConfig);
        }
        return httpClient.execute(request);
    }

    public HttpResponse post(String url, HttpHost proxy, Map<String, String> headers,
                             Map<String, String> params, List<File> fileLists) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader("user-agent", Constants.userAgentArray[new Random().nextInt(Constants.userAgentArray.length)]);

        if (headers != null && headers.size() > 0) {
            for (String key : headers.keySet()) {
                request.setHeader(key, headers.get(key));
            }
        }

        if (proxy != null) {
            RequestConfig requestConfig = getDefaultRequestConfigBuilder().setProxy(proxy).build();
            request.setConfig(requestConfig);
        }

        //文件
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                StringBody stringBody = new StringBody(params.get(key), ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), Consts.UTF_8));
                multipartEntityBuilder.addPart(key, stringBody);
            }
        }
        if (fileLists != null && fileLists.size() > 0) {
            for (File file : fileLists) {
                multipartEntityBuilder.addBinaryBody("file", new FileInputStream(file), ContentType.parse(Files.probeContentType(Paths.get(file.getPath()))), file.getName());
            }
        }
        HttpEntity entity = multipartEntityBuilder.build();
        request.setEntity(entity);
        return httpClient.execute(request);
    }

    public String getContent(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();

            if (httpEntity.getContentEncoding() != null) {
                if ("gzip".equalsIgnoreCase(httpEntity.getContentEncoding().getValue())) {
                    httpEntity = new GzipDecompressingEntity(httpEntity);
                } else if ("deflate".equalsIgnoreCase(httpEntity.getContentEncoding().getValue())) {
                    httpEntity = new DeflateDecompressingEntity(httpEntity);
                }
            }
            return EntityUtils.toString(httpEntity);
        }
        return null;
    }

    public void close() {
        try {
            this.httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                this.httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, URISyntaxException, IOException {
        SimpleHttpClient httpClient = new SimpleHttpClient();
        httpClient.init();
        HttpHost proxy = null;
        proxy = new HttpHost("183.166.163.193", 9999);
        HttpResponse response = httpClient.get("https://www.baidu.com/", proxy);
        System.out.println(httpClient.getContent(response));
//        HashMap headers = new HashMap();
//        HashMap params = new HashMap();
//        headers.put("cookieCSDNStr", "uuid_tt_dd=10_19421718940-1552879238977-808250; smidV2=2019032014313912dc5e664206f560d22977afd9e3b61000907372354999fe0; _ga=GA1.2.1496562676.1553508754; UN=u014229652; Hm_ct_6bcd52f51e9b3dce32bec4a3997715ac=5744*1*u014229652!6525*1*10_19421718940-1552879238977-808250; dc_session_id=10_1562555978930.651271; UserName=u014229652; UserInfo=5da3f3220eae4655a780c3bea2f14c23; UserToken=5da3f3220eae4655a780c3bea2f14c23; UserNick=%E5%85%AD%E5%8F%94_; AU=8F1; BT=1565602394318; p_uid=U000000; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1563853925,1565602395; bubble=true; aliyun_webUmidToken=TB5C04671693B053AF7CF03C64FA25E5CAD3E5F4AEA69556A63CD6D3CF5; firstDie=1; dc_tos=py47tq");
//        params.put("shuiyin", "2");
//        HttpResponse httpResponse = httpClient.post(
//                "https://mp.csdn.net/UploadImage",
//                null,
//                headers,
//                params,
//                Collections.singletonList(new File("E:/anotes_and_information/malling/jenkins/h5-config/Snipaste_2019-07-31_11-26-38.png"))
//        );
//
//        System.out.println(httpClient.getContent(httpResponse));
        httpClient.close();
    }

}


