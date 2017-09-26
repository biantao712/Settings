package com.android.settings.zenmotion2;

/**
 * Created by mark_guo on 2016/8/31.
 */
public class StringHelper {
    /**
     * 得到 全拼
     *
     * @param src
     * @return
     */
    public static String getPingYin(String src) {
        /*
        char[] t1 = null;
        t1 = src.toCharArray();
        String[] t2 = new String[t1.length];
        String t4 = "";
        int t0 = t1.length;
        for (int i = 0; i < t0; i++) {
                t4 += java.lang.Character.toString(t1[i]);
        }
        */
        String t4=HanziToPinyin.getInstance().transliterate(src);
        return t4;
    }

    /**
     * 得到首字母
     *
     * @param str
     * @return
     */
    public static String getHeadChar(String str) {

        String convert = "";
        char word = str.charAt(0);

        String pinyinArray = getPingYin(str);
        if (pinyinArray != null) {
            convert += pinyinArray.charAt(0);
        } else {
            convert += word;
        }
        return convert.toUpperCase();
    }

    /**
     * 得到中文首字母缩写
     *
     * @param str
     * @return
     */
    public static String getPinYinHeadChar(String str) {

        String convert = "";
        for (int j = 0; j < str.length(); j++) {
            char word = str.charAt(j);
            String pinyinArray = getPingYin(str);
            if (pinyinArray != null) {
                convert += pinyinArray.charAt(0);
            } else {
                convert += word;
            }
        }
        return convert.toUpperCase();      }
}
