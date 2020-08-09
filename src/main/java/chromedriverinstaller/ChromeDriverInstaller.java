/*
Copyright 2019 Yusuke Yamamoto
 
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package chromedriverinstaller;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("WeakerAccess")
public final class ChromeDriverInstaller {
    private static final String VERSION = "85.0.4183.38";

    /**
     * ensure ChromeDriver is installed on the specified directory
     *
   chromedriver_linux32.zip
   chromedriver_linux64.zip
   chromedriver_mac64.zip
   chromedriver_win32.zip
     * https://chromedriver.storage.googleapis.com/index.html?path=2.36/
     *
     * @param installationRoot directory to be installed
     * @return path to the ChromeDriver binary
     */
    public static String ensureInstalled(String installationRoot) throws IOException {
        Path installRootPath = Paths.get(installationRoot);
        String fileName;
        String dirName = "chromedriver_";
        String os = System.getProperty("os.name").toLowerCase();
        String binName = "chromedriver";
        if (os.contains("nux")) {
            dirName += "_linux" + ("32".equals(System.getProperty("sun.arch.data.model")) ? "32" : "64");
        } else{
            if (os.startsWith("windows")) {
                dirName += "win32";
                binName += ".exe";
            } else if (os.contains("mac") || os.contains("darwin")) {
                dirName += "mac64";
            } else {
                throw new IllegalStateException("Unexpected os:" + os);
            }
        }
        fileName = dirName + ".zip";

        Path filePath = installRootPath.resolve(fileName);
        Path bin = installRootPath.resolve(binName);
        // download ChromeDriver
        if (!Files.exists(bin)) {
            Files.createDirectories(installRootPath);
            String downloadURL = "https://chromedriver.storage.googleapis.com/" + VERSION + "/" + fileName;
            //noinspection ResultOfMethodCallIgnored
            filePath.toFile().delete();
            URL url = new URL(downloadURL);
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                int code = conn.getResponseCode();
                if (code == 200) {
                    Files.copy(conn.getInputStream(), filePath);
                } else {
                    throw new IOException("URL[" + url + "] returns code [" + code + "].");
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            if (filePath.getFileName().toString().endsWith("tar.bz2")) {
                unTar(filePath.toFile().getAbsolutePath(), installRootPath);
            } else {
                unZip(installRootPath, filePath);
            }
            //noinspection ResultOfMethodCallIgnored
            bin.toFile().setExecutable(true);
        }
        String chromedriver = bin.toAbsolutePath().toFile().getAbsolutePath();
        System.setProperty("webdriver.chrome.driver", chromedriver);
        return chromedriver;
    }

    private static void unZip(Path root, Path archiveFile) throws IOException {
        ZipFile zip = new ZipFile(archiveFile.toFile());
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                Files.createDirectories(root.resolve(entry.getName()));
            } else {
                try (InputStream is = new BufferedInputStream(zip.getInputStream(entry))) {
                    Files.copy(is, root.resolve(entry.getName()));
                }
            }

        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void unTar(String s, Path root) throws IOException {
        File tarFile = File.createTempFile("driver", "tar");
        try (BZip2CompressorInputStream in = new BZip2CompressorInputStream(new FileInputStream(s));
             FileOutputStream out = new FileOutputStream(tarFile)) {
            IOUtils.copy(in, out);
        }

        File outputDir = root.toFile();
        outputDir.mkdirs();
        try (ArchiveInputStream is = new ArchiveStreamFactory()
                .createArchiveInputStream("tar", new FileInputStream(tarFile))) {
            ArchiveEntry entry;
            while ((entry = is.getNextEntry()) != null) {
                File out = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    try (OutputStream fos = new FileOutputStream(out)) {
                        IOUtils.copy(is, fos);
                    }
                }
            }
        } catch (ArchiveException e) {
            throw new IOException(e);
        }
        tarFile.delete();
    }
}
