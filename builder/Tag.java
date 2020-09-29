/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.io.*;

/* ------------------------------------------------------------------------ *\
 *                                   Tag                                    *
\* ------------------------------------------------------------------------ */

public abstract class Tag {

    /**
     * This is the exception class thrown when there is an XML parsing error.
     */
    public static class ParseException extends RuntimeException {
        public ParseException(String msg) {
            super(msg);
        }
    }

    static int lastCh;
    static int lineNo;

    public static ParseException parseException(String msg) {
        if (msg == null) {
            return new ParseException("line "+lineNo);
        } else {
            return new ParseException("line "+lineNo+": "+msg);
        }
    }

    /*
     * read
     */
    private static int read(InputStream in) throws IOException {
        int ch;

        for (;;) {
            ch = in.read();
            if (lastCh != '\n' || ch != '/') {
                break;
            }
            while (ch != '\n') {
                ch = in.read();
            }
        }
//System.out.print((char)ch);
        lastCh = ch;
        if (ch == '\n') {
            lineNo++;
        }
        return ch;
    }

    /*
     * create
     */
    public static Tag create(InputStream in) throws IOException {
        return create(in, read(in));
    }

    /*
     * create
     */
    public synchronized static Tag create(InputStream in, int ch) throws IOException {
        lastCh = '\n';
        lineNo = 1;
        return create(in, null, ch);
    }

    /*
     * create
     */
    private static Tag create(InputStream in, final String parentTag, int ch) throws IOException {

        Tag first = null;
        Tag last  = null;

        for (;;) {

            StringBuffer sb     = new StringBuffer();
            boolean hasContents = true;
            boolean isEndTag    = false;

            for (;;) {
                int spaces = 0;

                /*
                 * Skip any space characters and count them
                 */
                while (ch == ' ') {
                    ch = read(in);
                    spaces++;
                }

                /*
                 * Skip any other white space
                 */
                while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                    ch = read(in);
                }

                /*
                 * Return null at EOF
                 */
                if (ch == -1) {
                    if (parentTag != null) {
                        throw parseException("EOF while parsing content for tag '"+parentTag+"'");
                    }
                    return first;
                }

                /*
                 * If the first character is not a '<' then this must be data of some kind
                 */
                if (ch != '<') {
                    int xch = ch;
                    while (spaces-- > 0) {
                        sb.append(' ');
                    }
                    while (ch != '<') {
                        sb.append((char)ch);
                        ch = read(in);
                    }
                    if (Tag.create(in, parentTag, '<') != null) {
                        throw parseException("xch=" + xch + " ch=" + ch +
                            " callersTag=" + parentTag + " sb=" + sb.toString());
                    }
                    Tag data = new DataTag(sb.toString());
                    if (last != null) {
                        throw parseException("Data tag cannot have a nested tag");
                    }
                    return data;
                }

                /*
                 * Get the first character of the tag name
                 */
                ch = read(in);

                /*
                 * If it is a '!' then this is a comment
                 */
                if (ch == '!') {
                    int last1 = ch;
                    int last2 = ch;
                    while (ch != '>' || last1 != '-' || last2 != '-') {
                        last2 = last1;
                        last1 = ch;
                        ch = read(in);
                        if (ch == -1) {
                            throw parseException("EOF in comment");
                        }
                    }
                    ch = read(in);
                    continue;
                }

                break;
            }

            /*
             * If it is a '/' then this is an end tag
             */
            if (ch == '/') {
                isEndTag = true;
                ch = read(in);
            }

            /*
             * Read to the end of the tag name
             */
            while (ch != '>') {
                if (ch == -1) {
                    throw parseException("Couldn't find '>'");
                }

               /*
                * If this is a '/' then it may be the last character of a tag In this case this
                * a simple tag with no end pair. Otherwise it may be a '/' in a attribute as in
                * <suite xmlns="http://www.sun.com/squawk/version/1.1">
                */
                if (ch == '/') {
                    ch = read(in);
                    if (ch == '>') {
                        hasContents = false;
                        break;
                    }
                    sb.append('/');
                }
                sb.append((char)ch);
                ch = read(in);
            }

            /*
             * Turn it into a string and check the length
             */
            String name = sb.toString();
            if (name.length() == 0) {
                throw parseException("Empty tag name");
            }

            /*
             * If it is an end tag then check the name and return null
             */
            if (isEndTag) {
                if (!hasContents) {
                    throw parseException("Malformed tag name: "+name); // i.e is </foo> not <foo/>
                }
                if (!name.equals(parentTag)) {
                    throw parseException("Expected </"+parentTag+"> not </"+name+">");
                }
                return first;
            }

            /*
             * Split the tag name from any attributes
             */
            int index = name.indexOf(' ');
            String attr = "";
            if (index > 0) {
                attr = name.substring(index);
                name = name.substring(0, index);
            }

            /*
             * Get the contents if there is any
             */
            Tag t;
            if (hasContents) {
                t = new ContentTag(name, attr, Tag.create(in, name, read(in)), null);  // name, attributes, content, next
            } else {
                t = new SimpleTag( name, attr,                                 null);  // name, attributes,          next
            }

            if (last != null) {
                last.setNext(t);
            }

            last = t;

            if (first == null) {
                first = last;
            }

            ch = read(in);
        }
    }

/*if[DEBUG.LOADER]*/
    /**
     * Command line interface for testing parser.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        System.out.print("The graph is:" + Tag.create(System.in));
    }
/*end[DEBUG.LOADER]*/

    /**
     * Return the parsed (well-formed) XML input as a string.
     * @return the parsed (well-formed) XML input as a string.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        appendTo(sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Insert 'level' tabs into a string buffer.
     * @param sb
     * @param level
     */
    final void tab(StringBuffer sb, int level) {
        sb.append('\n');
        for (int i = 0 ; i < level ; i++) {
             sb.append("  ");
        }
    }

    /*
     * setNext
     */
    public void setNext(Tag next) {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * isName
     */
    public boolean isName(String name) {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * checkName
     */
    public void checkName(String name) {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * getName
     */
    public String getName() {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * getContent
     */
    public Tag getContent() {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * getNext
     */
    public Tag getNext() {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * getData
     */
    public String getData() {
        throw parseException("Invalid call "+this.getClass());
    }

    /*
     * appendTo
     */
    public abstract void appendTo(StringBuffer sb, int level);

    /**
     * Count the number of tags at the same level as a specified tag.
     * @param tag The first tag.
     * @param name If non-null, then each tag is tested to ensure that
     * its name is this value (i.e. ensure this is a list of uniform tag types).
     * @return the number of tags.
     * @throws RuntimeException if this tag is not a content tag
     */
    public static int countTags(Tag tag, String name) {
        int count = 0;
        while (tag != null) {
            if (name != null) {
                tag.checkName(name);
            }
            count++;
            tag = tag.getNext();
        }
        return count;
    }

    /**
     * Decode any "&#n;" sequences in a specified string.
     * @param str The string to decode
     * @return the given string with all "&#n;" sequences decoded.
     */
    public static String unescape(String str) {
        if (str.indexOf('&') == -1) {
            return str;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0 ; i < str.length() ;) {
            int ch = str.charAt(i++);
            if (ch == '&') {
                ch = str.charAt(i++);
                if (ch != '#') {
                    throw parseException("Missing '#' at index "+(i-1)+" in encoded string: "+str);
                }
                int val = 0;
                for (;;) {
                    ch = str.charAt(i++);
                    if (ch == ';') {
                        sb.append((char)val);
                        break;
                    }
                    if (ch < '0' || ch > '9') {
                        throw parseException("Expected a digit at index "+(i-1)+" in encoded string: "+str);
                    }
                    val = (val * 10) + (ch - '0');

                }
            } else {
                sb.append((char)ch);
            }
        }
        return sb.toString();
    }


/* ------------------------------------------------------------------------ *\
 *                                 SimpleTag                                *
\* ------------------------------------------------------------------------ */

    private static class SimpleTag extends Tag {

        String name;
        String attr;
        Tag    next;

        /*
         * SimpleTag
         */
        SimpleTag(String name, String attr, Tag next) {
            this.name = name;
            this.attr = attr;
            this.next = next;
        }

        /*
         * setNext
         */
        public void setNext(Tag next) {
            this.next = next;
        }

        /*
         * isName
         */
        public boolean isName(String name) {
            return this.name.equals(name);
        }

        /*
         * checkName
         */
        public void checkName(String name) {
            if (!isName(name)) {
                throw parseException("Expecting "+name+ " not "+this.name);
            }
        }

        /*
         * getName
         */
        public String getName() {
            return name;
        }

        /*
         * getNext
         */
        public Tag getNext() {
            return next;
        }

        /*
         * appendTo
         */
        public void appendTo(StringBuffer sb, int level) {
            tab(sb, level);
            sb.append('<');
            sb.append(name);
            sb.append(attr);
            sb.append("/>");
            if (next != null) {
                next.appendTo(sb, level);
            }
        }
    }

/* ------------------------------------------------------------------------ *\
 *                                ContentTag                                *
\* ------------------------------------------------------------------------ */

    private static class ContentTag extends SimpleTag {

        Tag content;

        /*
         * ContentTag
         */
        ContentTag(String name, String attr, Tag content, Tag next) {
            super(name, attr, next);
            this.content = content;
//if (content == null) throw new Error("name="+name+" attr="+attr);
        }

        /*
         * getContext
         */
        public Tag getContent() {
            return content;
        }

        /*
         * appendTo
         */
        public void appendTo(StringBuffer sb, int level) {
            tab(sb, level);
            sb.append('<');
            sb.append(name);
            sb.append(attr);
            sb.append('>');
            if (content != null) {
                content.appendTo(sb, level+1);
            }
            tab(sb, level);
            sb.append("</");
            sb.append(name);
            sb.append('>');
            if (next != null) {
                next.appendTo(sb, level);
            }
        }

    }

/* ------------------------------------------------------------------------ *\
 *                                  DataTag                                 *
\* ------------------------------------------------------------------------ */

    private static class DataTag extends Tag {

        String data;

        /*
         * DataTag
         */
        DataTag(String data) {
            this.data = data;
        }

        /*
         * getData
         */
        public String getData() {
            return data;
        }

        /*
         * appendTo
         */
        public void appendTo(StringBuffer sb, int level) {
            tab(sb, level);
            sb.append(data);
        }
    }
}