package cz.malyzajic.coney.java;

import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;
import cz.malyzajic.coney.Bot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author daop
 */
public class WebUpdateChecker implements Bot {

    private final String name;
    private String nick;
    private Channel channel;
    private boolean takeAll = false;
    private Runner runner;
    private final Set<String> mem = new HashSet<>();

    public WebUpdateChecker(String name) {
        this.name = name;
        init();
    }

    @Override
    public String getNick() {
        return nick;
    }

    @Override
    public String getName() {
        return name;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isTakeAll() {
        return takeAll;
    }

    @Override
    public void setTakeAll(boolean takeAll) {
        this.takeAll = takeAll;
    }

    @Override
    public String getResponse(String request, User sender) {
        String result = null;

        try {
//            result = getName() + ":: request :: " + request;

        } catch (Exception ex) {
            Logger.getLogger(WebUpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    private void init() {
        try {

            if (nick == null) {
                nick = name;
            }
            runner = new Runner();
            runner.setContext(this);
            runner.start();
        } catch (Exception ex) {
            Logger.getLogger(WebUpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendMessage(String txt) {

        if (channel != null) {
            channel.sendMessage("Labka.cz update: " + txt);
        }
    }

    @Override
    public boolean takeAll() {
        return takeAll;
    }

    private String getContent(String web) {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        StringBuilder bldr = new StringBuilder();

        try {

            prepareHttpsMethod();

            url = new URL(web);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                bldr.append(line);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(WebUpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException ex) {
            Logger.getLogger(WebUpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                // nothing to see here
            }
        }
        return bldr.toString();
    }

    private void prepareHttpsMethod() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

            }
        };

        SSLContext sc;
        sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private class Runner extends Thread {

        WebUpdateChecker context = null;

        public WebUpdateChecker getContext() {
            return context;
        }

        public void setContext(WebUpdateChecker context) {
            this.context = context;
        }

        @Override
        public void run() {
            boolean run = true;
            if (context == null) {
                run = false;
            }

            while (run) {
                try {
                    String data = context.getContent("https://labka.cz/wiki/doku.php?id=start&do=recent");
                    parseData(data);
                    Thread.sleep(1000 * 60 * 2);
                } catch (InterruptedException ex) {
                    run = false;

                }
            }

        }

        private void parseData(String data) {
            boolean reloaded = mem.isEmpty();
            Document doc = Jsoup.parse(data);
            if (doc != null) {
                Elements links = doc.select(".li");
                if (links != null) {
                    for (Element e : links) {
                        String md5 = getMd5(e.html());
                        if (!mem.contains(md5)) {
                            if (!reloaded) {
                                String info = getInfoMessage(e);
                                sendMessage(info);
                            }
                            mem.add(md5);
                        }

                    }
                }
            }
        }

        private String getMd5(String html) {
            String result = null;
            try {
                System.out.println("html" + html);
                byte[] bytesOfMessage = html.getBytes("UTF-8");

                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(bytesOfMessage);
                result = new String(thedigest);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
                Logger.getLogger(WebUpdateChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
            return result;
        }

        private String getInfoMessage(Element e) {
            String result = null;
            if (e != null) {
                String user = e.getElementsByClass("user").get(0).html();
                String title = e.getElementsByClass("wikilink1").get(0).attr("title");
                String url = e.getElementsByClass("wikilink1").get(0).attr("href");

                result = Jsoup.parse(user).text() + " :: " + title + " :: " + "https://labka.cz" + url;
            }
            return result;
        }

    }

}
