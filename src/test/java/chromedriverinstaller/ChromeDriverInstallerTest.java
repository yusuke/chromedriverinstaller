/*
Copyright 2019 Yusuke Yamamoto
 
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package chromedriverinstaller;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.HashMap;

class ChromeDriverInstallerTest {

    @org.junit.jupiter.api.Test
    void ensureInstalled() {
        boolean headless = false;
        String downloadDirectory = "/tmp";
        try {
            String path = ChromeDriverInstaller.ensureInstalled("/tmp/chromedriver");
            ChromeOptions options = new ChromeOptions();
            if (headless) {
                options.addArguments("--headless");
            }
            options.addArguments("--user-agent=\"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0)\"");

            HashMap<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("profile.default_content_settings.popups", 0);
            chromePrefs.put("download.default_directory", downloadDirectory);
            options.setExperimentalOption("prefs", chromePrefs);
            System.setProperty("webdriver.chrome.driver", path);
            ChromeDriver driver = new ChromeDriver(options);
            driver.get("https://yusuke.blog");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (java.lang.ExceptionInInitializerError e) {
            throw new RuntimeException("Chrome not installed", e);
        }
    }
}