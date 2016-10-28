package com.johnsonandschraft.h0x0.vectortest;

import android.content.res.XmlResourceParser;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    @SuppressWarnings("UnusedDeclaration")
    private static final String DEBUG_TAG = "MainActivity";

//    XmlResourceParser parser;
//    VectorView view;
//    Stack<List<String[]>> groupStack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VectorView view = (VectorView) findViewById(R.id.vector_view);
        SVGParser parser = new SVGParser(view);
        try {
            InputStream stream = getAssets().open(getPackageResourcePath().concat("/xml/test.xml"));
            parser.execute(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        view = (VectorView) findViewById(R.id.vector_view);
//        VectorView view = (VectorView) findViewById(R.id.vector_view);
//        SVGParser parser = new SVGParser(view);
//        String svg = getResources().getXml(R.xml.test).getText();
//        XmlResourceParser parser = getResources().getXml(R.xml.test);
//        groupStack = new Stack<>();
//        parser = getResources().getXml(R.xml.test);
//        int action;
//        try {
//            while ((action = parser.next())!= XmlResourceParser.END_DOCUMENT){
//                switch (action) {
//                    case XmlResourceParser.START_TAG:
//                        _startTag();
//                        break;
//                    case XmlResourceParser.END_TAG:
//                        _endTag();
//                        break;
//                    case XmlResourceParser.TEXT:
//                        _text();
//                        break;
//                }
//            }
//        } catch (XmlPullParserException | IOException ignored) {}
//        view.reset();
    }

//    private void _startTag() {
//        int attributeCount = parser.getAttributeCount();
//        String tagName, attributeName, attributeValue;
//
//        tagName = parser.getName();
//        SVGDrawElement element = null;
////        element.setType(tagName);
//        switch (SVGDrawElement.SVGTypeFromTag(tagName)) {
//            case Circle:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Circle);
//                break;
//            case Ellipse:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Ellipse);
//                break;
//            case Line:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Line);
//                break;
//            case Path:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Path);
//                break;
//            case Polygon:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Polygon);
//                break;
//            case Polyline:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Polyline);
//                break;
//            case Rectangle:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Rectangle);
//                break;
//            case Text:
//                element = new SVGDrawElement(SVGDrawElement.SVGDrawType.Text);
//                break;
//            case None:
//                if (tagName.equalsIgnoreCase("g")) {
//                    pushGroup();
//                }
//        }
//        if (element == null) return;
//        for (int i = 0; i < attributeCount; i++) {
//            attributeName = parser.getAttributeName(i);
//            attributeValue = parser.getAttributeValue(i);
//            element.setAttributeValue(attributeName, attributeValue);
//        }
//
//        if (!groupStack.empty()) {
//            for (String[] attr : groupStack.peek()) {
//                element.setAttributeValue(attr[0], attr[1]);
//            }
//        }
//
//        view.addElement(element);
//    }
//
//    private void _endTag() {
//        if (parser.getName().equalsIgnoreCase("g") && !groupStack.empty()) groupStack.pop();
//    }
//
//    private void _text() {
//        SVGDrawElement svgDrawElement = (SVGDrawElement) view.getLastElement();
//        svgDrawElement.setText(parser.getText());
//    }
//
//    private void pushGroup() {
//        ArrayList<String[]> attrList= new ArrayList<>();
//        for (int i = parser.getAttributeCount(); i > 0; i--) {
//            String[] tmp = new String[2];
//            tmp[0] = parser.getAttributeName(i - 1);
//            tmp[1] = parser.getAttributeValue(i - 1);
//            attrList.add(tmp);
//        }
//        groupStack.push(attrList);
//    }
}
