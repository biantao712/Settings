/*
 * Arica_Chen  20130812
 * LiveDemo
 */

package com.android.settings;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

public class LiveDemoUnit {
 public static  int AccFlagRead()
    {
        String strPath = "/ADF/ADF";
        File file = new File(strPath);
        if (!file.exists()){
            strPath = "/dev/block/ADF";
            file = new File(strPath);
            if (!file.exists()){
                strPath = "/dev/block/demoflag";
                file = new File(strPath);
                if (!file.exists()){
                    return 0;
                }
            }
        }

        int flag = 0;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(strPath, "r");
            if(randomAccessFile.length() >= 4){
                 flag = randomAccessFile.readInt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
                if(randomAccessFile != null){
                try {
					randomAccessFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
           }
        }
        if(flag>2)
            flag = 0;
        return flag;
     }
}
