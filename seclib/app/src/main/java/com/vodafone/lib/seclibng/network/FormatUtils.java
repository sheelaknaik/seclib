/*
 * Copyright (C) 2017.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vodafone.lib.seclibng.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

class FormatUtils {

    public static String formatHeaders(List<HttpHeader> httpHeaders, boolean withMarkup) {
        StringBuilder out = new StringBuilder();
        if (httpHeaders != null) {
            for (HttpHeader header : httpHeaders) {
                out.append((withMarkup) ? "<b>" : "").append(header.getName()).append(": ").append((withMarkup) ? "</b>" : "").append(header.getValue()).append((withMarkup) ? "<br />" : "\n");
            }
        }
        return out.toString();
    }

    public static String formatJson(String json) {
        String jsonString;
        try {
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(json);
            jsonString = JsonConverter.getInstance().toJson(je);
            return jsonString;
        } catch (Exception e) {
            return json;
        }
    }

    public static String formatXml(String xml) {
        try {
            String xmlString;
            Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            Source xmlSource = new SAXSource(new InputSource(new ByteArrayInputStream(xml.getBytes())));
            StreamResult res = new StreamResult(new ByteArrayOutputStream());
            serializer.transform(xmlSource, res);

            xmlString= new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());

            return xmlString;
        } catch (Exception e) {
            return xml;
        }
    }

    public static String formatByteCount(long bytes, @SuppressWarnings("SameParameterValue") boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


}