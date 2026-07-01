package com.gnd.publisher.integration.rss;

import java.net.URI;

public interface RssClient {

    String fetch(URI feedUri);
}
