/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.notification.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;
import uk.ac.bbsrc.tgac.miso.integration.util.SignatureHelper;

import java.util.*;

/**
 * uk.ac.bbsrc.tgac.miso.notification.util
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @date 07-Dec-2010
 * @since 0.1.5
 */
public class NotificationUtils {
  protected static final Logger log = LoggerFactory.getLogger(NotificationUtils.class);

  public static <T> Message<T> buildSimpleMultipartMessage(T payload) {
    Message<T> message = MessageBuilder.withPayload(payload).setHeader("Content-Type", "multipart/form-data").build();
    return message;
  }

  public static <T> Message<T> buildSimplePostMessage(T payload) {
    Message<T> message = MessageBuilder.withPayload(payload).setHeader("Content-Type", "x-www-form-urlencoded").build();
    return message;
  }

  public static <T> Message<T> buildSimpleMessage(T payload) {
    return MessageBuilder.withPayload(payload).build();
  }

  public Set<Map<String, String>> splitMessage(Message<Map<String, String>> message) {
    Set<Map<String, String>> outset = new HashSet<Map<String, String>>();
    Map<String, String> payload = message.getPayload();
    for (String key : payload.keySet()) {
      //each map value is a JSONArray string
      JSONArray a = JSONArray.fromObject(payload.get(key));
      for (JSONObject o : (Iterable<JSONObject>)a) {
        Map<String, String> runMap = new HashMap<String, String>();
        JSONArray aa = new JSONArray();
        aa.add(o);
        runMap.put(key, aa.toString());
        outset.add(runMap);
      }
    }
    log.info("Split a single message payload into a " + outset.size() + " sized set...");
    return outset;
  }

  public Map<String, Object> signMessageHeaders(Message<?> message) {
    MessageHeaders headers = message.getHeaders();
    String url = headers.get("url", String.class);

    Map<String, Object> newheaders = new HashMap<String, Object>();
    newheaders.put("Accept", "*/*");
    newheaders.put(SignatureHelper.APIKEY_HEADER,
                SignatureHelper.API_KEY);
    newheaders.put(SignatureHelper.TIMESTAMP_HEADER,
                "\"" + System.currentTimeMillis() + "\"");

    //sign only those headers that are Strings
    Map<String, List<String>> stringHeaders = new HashMap<String, List<String>>();
    for (String key : headers.keySet()) {
      if (headers.get(key) instanceof String) {
        List<String> ss = new ArrayList<String>();
        ss.add(headers.get(key, String.class));
        stringHeaders.put(key, ss);
      }
      newheaders.put(key, headers.get(key));
    }

    try {
      log.debug("HEADERS -> " + stringHeaders + ":" + url + ":" + SignatureHelper.PRIVATE_KEY);

       newheaders.put(SignatureHelper.SIGNATURE_HEADER,
                   SignatureHelper.createSignature(stringHeaders, url, SignatureHelper.PRIVATE_KEY));
    } catch (Exception e) {
       e.printStackTrace();
    }
    return newheaders;
  }
}
