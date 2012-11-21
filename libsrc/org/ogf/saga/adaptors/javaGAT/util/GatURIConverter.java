package org.ogf.saga.adaptors.javaGAT.util;

import java.net.URISyntaxException;

import org.gridlab.gat.URI;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.impl.SagaObjectBase;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatURIConverter {

    private static Logger logger = LoggerFactory.getLogger(GatURIConverter.class);

    public static URI cvtToGatURI(URL url) throws URISyntaxException {

        URI uri;
        String scheme = url.getScheme();
        String userInfo = url.getUserInfo();
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        String query = url.getQuery();
        String fragment = url.getFragment();

        StringBuffer u = new StringBuffer();
        if (scheme != null && ! scheme.equals("")) {
            u.append(scheme);
            u.append(":");
        }
        if (host != null && ! host.equals("")) {
            u.append("//");
            if (userInfo != null && ! userInfo.equals("")) {
                u.append(userInfo);
                u.append("@");
            }
            u.append(host);
            if (port >= 0) {
                u.append(":");
                u.append(port);
            }
        }

        if (scheme != null && ! scheme.equals("")) {
            // This is the work-around to obtain uri's that
            // JavaGAT understands.
            if (host != null && ! host.equals("")) {
                u.append("/");
            } else {
                u.append("///");
            }
        }
        u.append(path);
        if (query != null && ! query.equals("")) {
            u.append("?");
            u.append(query);
        }
        if (fragment != null && ! fragment.equals("")) {
            u.append("#");
            u.append(fragment);
        }

        uri = new URI(u.toString());

        if (logger.isDebugEnabled()) {
            logger.debug("URL " + url + " converted to " + uri);
        }
        return uri;
    }

    public static URL cvtToSagaURL(URI uri) throws NotImplementedException,
            BadParameterException, NoSuccessException {
        return URLFactory.createURL(SagaObjectBase.MY_FACTORY, uri.toString()).normalize();
    }

}
