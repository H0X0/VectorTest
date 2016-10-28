package com.johnsonandschraft.h0x0.vectortest;

import android.os.AsyncTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by h0x0 on 10/29/2015.
 *
 */
public class SVGParser extends AsyncTask<InputStream, Integer, List<SVGDrawElement>> {
    private XmlPullParser parser;
    private List<SVGDrawElement> elements;
    private WeakReference<VectorView> viewWeakReference;
//    private Map<String, List<String[]>> styleClass;
//    private Stack<List<String[]>> styleStack;
    private Map<String, String> classMap;
    private Stack<Integer> parents;
    private boolean parsingElement = false;

    private boolean cssParse = false;
    private static final String RX_CSS = "\\.(\\w+)\\{([^\\}]*)\\}\\s*";
    private static final Pattern CSS_PATTERN = Pattern.compile(RX_CSS);
//    SVGDrawElement curElement;

    public SVGParser(VectorView view) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        viewWeakReference = new WeakReference<>(view);
        elements = new ArrayList<>();
        classMap = new HashMap<>();
        parents = new Stack<>();
    }

    @Override
    protected List<SVGDrawElement> doInBackground(InputStream... params) {
        for (InputStream inputStream : params) {
            try {
                parser.setInput(inputStream, null);
                int action;
                while ((action = parser.nextToken()) != XmlPullParser.END_DOCUMENT) {
                    switch (action) {
                        case XmlPullParser.START_TAG:
                            _startTag();
                            break;
                        case XmlPullParser.END_TAG:
                            _endTag();
                            break;
                        case XmlPullParser.TEXT:
                            _text();
                            break;
                        case XmlPullParser.CDSECT:
                            _cdata();
                            break;
                    }
                    if (isCancelled()) break;
                }
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
        }
        return elements;
    }

    private static final String TAG_STYLE = "style";
    private static final String VAL_CSS = "text/css";
    private void _startTag() {
//        int attributeCount = parser.getAttributeCount();
        SVGDrawElement.SVGDrawType type = SVGDrawElement.SVGTypeFromTag(parser.getName());
        switch (type) {
            case None:
                String tag = parser.getName();
                if (tag.equalsIgnoreCase(TAG_STYLE)) {
                    cssParse = true;
                }
                break;
            default:
                _newSVGElement(type);
        }
    }

    private void _newSVGElement(SVGDrawElement.SVGDrawType type) {
        SVGDrawElement current = new SVGDrawElement(type);
        if (!parents.isEmpty()) {
            current.inherit(elements.get(parents.peek()));
        }

        for (int i =0, j = parser.getAttributeCount(); i < j; i++) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            current.setAttributeValue(attr, value);
        }

        int index = elements.size();
        elements.add(current);
        parents.push(index);
        parsingElement = true;
    }

    private void  _endTag() {
        if (parsingElement) publishProgress(parents.pop());
        parsingElement = false;
        cssParse = false;
    }

    private void _text() {
        if (parents.isEmpty())return;
        if (parsingElement) elements.get(parents.peek()).setText(parser.getText());
    }

    private void _cdata() {
        if (cssParse) {
            String data = parser.getText();
            Matcher matcher = CSS_PATTERN.matcher(data);
            for (int i = 0; matcher.find(i); i = matcher.end()) {
                String className = matcher.group(1);
                String styleSpec = matcher.group(2);
                try {
                    classMap.put(className, styleSpec);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        VectorView view = viewWeakReference.get();
        if (view == null) {
            cancel(true);
            return;
        } else if (isCancelled()) {
            //// TODO: 10/30/2015
        }

        for (Integer i : values) {
            SVGDrawElement element = elements.get(i);
            if (element != null) view.addElement(element, i);
        }
    }

    @Override
    protected void onPostExecute(List<SVGDrawElement> svgDrawElements) {
        super.onPostExecute(svgDrawElements);
    }

    //    public SVGParser(InputStream stream) throws XmlPullParserException {
//        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
//        factory.setNamespaceAware(true);
//        parser = factory.newPullParser();
//        parser.setInput(stream, null);
//        elements = new ArrayList<>();
//    }
//
//    @Override
//    public void run() {
//        int event = 0;
//        try {
//            event = parser.getEventType();
//        } catch (XmlPullParserException e) {
//            e.printStackTrace();
//        }
//        while (event != XmlPullParser.END_DOCUMENT) {
//            switch (event) {
//                case XmlPullParser.START_DOCUMENT:
//                    break;
//                case XmlPullParser.CDSECT:
//                    break;
//                case XmlPullParser.START_TAG:
//                    break;
//                case XmlPullParser.END_TAG:
//                    break;
//            }
//            try {
//                event = parser.next();
//            } catch (XmlPullParserException | IOException e){
//                e.printStackTrace();
//            }
//        }
//    }
}
