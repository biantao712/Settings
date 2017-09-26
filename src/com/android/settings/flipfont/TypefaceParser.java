package com.android.settings.flipfont;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/*
 *  Parser class to parse font definition xml-files
 */
 public class TypefaceParser extends DefaultHandler {

    // XML tag definitions
    private static final String NODE_FONT = "font";
    private static final String NODE_SANS = "sans";
    private static final String NODE_SERIF = "serif";
    private static final String NODE_MONOSPACE = "monospace";
    private static final String NODE_FILE = "file";
    private static final String NODE_FILENAME = "filename";
    private static final String NODE_DROIDNAME = "droidname";

    // XML attributes
    private static final String ATTR_NAME = "displayname";

    // Switches to know what level we are parsing
    private boolean in_font = false;
    private boolean in_sans = false;
    private boolean in_serif = false;
    private boolean in_monospace = false;
    private boolean in_file = false;
    private boolean in_filename = false;
    private boolean in_droidname = false;

    // Parsed data
    private Typeface mFont = null;
    private TypefaceFile mFontFile = null;


    /*
     * Method returns parsed data
     * @return Parsed typeface
     */
    public Typeface getParsedData() {
        return mFont;
    }

    @Override
    public void startDocument() throws SAXException {
        this.mFont = new Typeface();
    }

    @Override
    public void endDocument() throws SAXException {

    }

    /*
     * Element start
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {

        if (localName.equals(NODE_FONT)){
            this.in_font = true;
            String attrValue = atts.getValue(ATTR_NAME);
            mFont.setName(attrValue);
        }
        else if (localName.equals(NODE_SANS)) {
            this.in_sans = true;
        }
        else if (localName.equals(NODE_SERIF)) {
            this.in_serif = true;
        }
        else if (localName.equals(NODE_MONOSPACE)) {
            this.in_monospace = true;
        }
        else if (localName.equals(NODE_FILE)) {
            this.in_file = true;
            mFontFile = new TypefaceFile();
        }
        else if (localName.equals(NODE_FILENAME)) {
            this.in_filename = true;
        }
        else if (localName.equals(NODE_DROIDNAME)) {
            this.in_droidname = true;
        }

    }

    /*
     * End element
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException {

        if (localName.equals(NODE_FONT)){
            this.in_font = false;
        }
        else if (localName.equals(NODE_SANS)) {
            this.in_sans = false;
        }
        else if (localName.equals(NODE_SERIF)) {
            this.in_serif = false;
        }
        else if (localName.equals(NODE_MONOSPACE)) {
            this.in_monospace = false;
        }
        else if (localName.equals(NODE_FILE)) {
            this.in_file = false;
            if (mFontFile != null) {
                if (in_sans) {
                    mFont.mSansFonts.add(mFontFile);
                }
                else if (in_serif) {
                    mFont.mSerifFonts.add(mFontFile);
                }
                else if (in_monospace) {
                    mFont.mMonospaceFonts.add(mFontFile);
                }
            }
        }
        else if (localName.equals(NODE_FILENAME)) {
            this.in_filename = false;
        }
        else if (localName.equals(NODE_DROIDNAME)) {
            this.in_droidname = false;
        }

    }

    /*
     * Parsing the element contents
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char ch[], int start, int length) {
            if (in_filename) {
                this.mFontFile.setFileName(new String(ch, start, length));
            }
            else if (in_droidname) {
                this.mFontFile.setDroidName(new String(ch, start, length));
            }
    }

}
