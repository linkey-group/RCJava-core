package com.rcjava.client;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.util.Arrays;

/**
 * @author zyf
 */
public class HttpsClientTest {

    @Test
    void testHttpsRequest() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(new File("jks/ssl/serverr1.jks"), "123".toCharArray(), new TrustSelfSignedStrategy()) // use null as second param if you don't have a separate key password
                .loadKeyMaterial(new File("jks/ssl/clientr1.jks"), "123".toCharArray(), "123".toCharArray())
                .build();
//        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER)
        HttpClient httpsClient = HttpClients.custom().setSSLContext(sslContext).build();
        HttpGet get = new HttpGet("https://localhost:8083/chaininfo");
        System.out.println(get.getRequestLine());
        HttpResponse response = httpsClient.execute(get);
        System.out.println(response);
        HttpEntity resEntity = response.getEntity();
        String str = EntityUtils.toString(resEntity, "UTF-8");
        JSONObject result = JSONObject.parseObject(str);
        System.out.println(result);
    }

    @Test
    void testHttpsRequestBySelenium() throws InterruptedException {

//        System.setProperty("webdriver.chrome.driver", "/Users/zyf/IdeaProjects/RCJava-core/chrome/chromedriver");
//        System.setProperty("webdriver.chrome.driver", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        System.setProperty("webdriver.chrome.driver", "chrome/chromedriver");

        ChromeOptions opts = new ChromeOptions();
        opts.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        opts.setCapability(CapabilityType.SUPPORTS_ALERTS, true);

        ChromeDriver driver = new ChromeDriver(opts);
        driver.get("https://localhost:8888");

        String text = driver.findElement(By.cssSelector("body > pre")).getText();

        System.out.println(text.equals("Connected"));

        Thread.sleep(50000);  // Let the user actually see something!

        driver.quit();
    }

}
