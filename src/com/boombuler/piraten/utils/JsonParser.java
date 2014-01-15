package com.boombuler.piraten.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonParser {
    public class ParseException extends Exception {
    }

    private String mInput;
    private int mPos;

    private JsonParser(String input) {
        mInput = input;
        mPos = 0;
    }

    private String readString() throws ParseException{
        char cur = next(true);
        if (cur != '"')
            throw new ParseException();
        StringBuilder result = new StringBuilder();

        boolean inEscape = false;

        while (!EOF()) {
            cur = next(false);

            if (inEscape) {
                if (cur == '"')
                    result.append('"');
                else if (cur == '\\')
                    result.append('\\');
                else if (cur == '/')
                    result.append('/');
                else if (cur == 'b')
                    result.append('\b');
                else if (cur == 'f')
                    result.append('\f');
                else if (cur == 'n')
                    result.append('\n');
                else if (cur == 'r')
                    result.append('\r');
                else if (cur == 't')
                    result.append('\t');
                else if (cur == 'u')
                    result.append((char) Integer.parseInt(String.valueOf(next(false) + next(false) + next(false) + next(false)), 16));
                else
                    throw new ParseException();
                inEscape = false;
            } else {
                if (cur == '\\') {
                    inEscape = true;
                    continue;
                }
                if (cur == '"')
                    return result.toString();
                result.append(cur);
            }
        }
        throw new ParseException();
    }

    private Object readObject() throws ParseException {
        char cur = next(true);
        if (cur != '{')
            throw new ParseException();

        JsonObject result = new JsonObject();

        if (peek(true) == '}') {
            next(true);
            return result;
        }

        while (!EOF()) {
            String key = readString();

            char c = next(true);
            if (c != ':')
                throw new ParseException();
            Object value = read();
            result.put(key, value);

            c = next(true);
            if (c == ',')
                continue;
            else if (c == '}')
                return result;
        }

        throw new ParseException();
    }

    private Object readArray() throws ParseException {
        char cur = next(true);
        if (cur != '[')
            throw new ParseException();

        JsonArray result = new JsonArray();

        if (peek(true) == ']') {
            next(true);
            return result;
        }

        while (!EOF()) {
            result.add(read());

            char c = next(true);
            if (c == ',')
                continue;
            else if (c == ']')
                return result;
        }

        throw new ParseException();
    }

    private Object readConst() throws ParseException {
        StringBuilder result = new StringBuilder();
        char c = peek(true);
        while (!EOF() && Character.isLetter(c)) {
            next(result.length() == 0);
            result.append(c);
            c = peek(false);
        }
        String s = result.toString();

        if ("null".equals(s))
            return null;
        else if ("true".equals(s))
            return Boolean.valueOf(true);
        else if ("false".equals(s))
            return Boolean.valueOf(false);
        else throw new ParseException();
    }

    private Object readNumber() throws ParseException {
        StringBuilder result = new StringBuilder();

        char c = next(true);
        if (c == '-' || Character.isDigit(c)) {
            result.append(c);
        }
        boolean isFloat = false;
        boolean isExp = false;

        while (!EOF()) {
            c = peek(false);
            if (Character.isDigit(c)) {
                next(false);
                result.append(c);
            } else if (c == '.' && !isFloat && !isExp) {
                isFloat = true;

                next(false);
                result.append(c);
            } else if ((c == 'e' || c == 'E') && !isExp) {
                isExp = true;

                next(false);
                result.append(c);
                c = next(false);
                if (Character.isDigit(c) || c == '+' || c == '-') {
                    result.append(c);
                }
            } else
                break;
        }
        if (isFloat || isExp)
            return Double.valueOf(result.toString());
        else
            return Integer.valueOf(result.toString());
    }

    private Object read() throws ParseException {
        char c = peek(true);
        if (c == '{')
            return readObject();
        else if (c == '"')
            return readString();
        else if (c == '[')
            return readArray();
        else if (Character.isLetter(c))
            return readConst();
        else if (Character.isDigit(c) || c == '-')
            return readNumber();
        throw new ParseException();
    }

    private boolean EOF() {
        return mPos >= mInput.length();
    }

    private char next(boolean onlyRelevant) throws ParseException{
        try
        {
            char result = mInput.charAt(mPos++);
            if (onlyRelevant) {
                while (!EOF() && Character.isWhitespace(result))
                    result = mInput.charAt(mPos++);
            }
            return result;
        }
        catch (Exception ex) {
            throw new ParseException();
        }
    }

    private char peek(boolean onlyRelevant) throws ParseException {
        int pos = mPos;
        try
        {
            return next(onlyRelevant);
        } finally {
            mPos = pos;
        }
    }


    public static Object Parse(String input) throws ParseException {
        return new JsonParser(input).read();
    }
}
