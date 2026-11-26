package com.tvremote.app;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Trust-all TLS helper, shared by TvClient and Ws (dedup only — behaviour unchanged).
 *
 * التليفزيونات بتستخدم شهادات موقّعة من نفسها، فبنقبل أي شهادة هنا عمدًا؛
 * الأمان بييجي من الاقتران بالكود (السر مربوط بمفاتيح الطرفين)، مش من الشهادة.
 */
final class TlsTrust {

    private TlsTrust() { }

    static X509TrustManager trustAllManager() {
        return new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) { }
            public void checkServerTrusted(X509Certificate[] c, String a) { }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
    }

    static SSLContext trustAllContext(KeyManager[] keyManagers, SecureRandom random)
            throws GeneralSecurityException {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(keyManagers, new TrustManager[]{trustAllManager()}, random);
        return ctx;
    }
}
