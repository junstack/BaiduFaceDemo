package com.das.face.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Base64OutputStream;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * created by jun on 2020/6/28
 * describe: 文件读取操作类
 */
public class FileUtils {

    /**
     * 获取缓存文件夹
     *
     * @param subDirName 缓存文件夹下子文件夹，若不存在自动创建
     * @return 指定的缓存文件夹
     */
    @Nullable
    public static File getCacheDir(Context context, String subDirName) {
        File dir;
        //判断sd卡是否可用
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            dir = context.getExternalCacheDir();//该方法可以获取到 SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
        } else {
            dir = context.getCacheDir();//获取/data/data/<application package>/cache目录
        }

        if (dir == null) {
            return null;
        }

        if (subDirName != null) {
            File subDir = new File(dir.getAbsolutePath() + "/" + subDirName);
            if (createDir(subDir)) {
                dir = subDir;
            } else {
                dir = null;
            }
        }

        return dir;
    }

    /**
     * 创建指定文件夹
     *
     * @param dir 指定文件夹
     * @return 是否创建成功
     */
    public static boolean createDir(File dir) {
        if (dir == null) {
            return false;
        }

        if (dir.exists() && dir.isDirectory()) {
            return true;
        } else {
            return dir.mkdirs();
        }
    }

    /**
     * 向指定文件写入字符串
     *
     * @param file 指定文件
     * @param text 要写入的字符串
     * @return 是否写入成功
     */
    public static boolean writeText(File file, String text) {
        FileOutputStream outputStream = null;

        try {
            if (file.exists()) {
                file.delete();
            }

            outputStream = new FileOutputStream(file.getAbsolutePath(), true);
            outputStream.write(text.getBytes());
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 向指定文件写入bitmap
     *
     * @param file   指定文件
     * @param bitmap 要写入的bitmap
     * @return 是否写入成功
     */
    public static boolean writeBitmap(File file, Bitmap bitmap) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file.getAbsolutePath());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // Bitmap.CompressFormat format 图像的压缩格式；int quality 图像压缩率，0-100。 0 压缩100%，100意味着不压缩；OutputStream stream 写入压缩数据的输出流；
            outputStream.flush();//仅仅是刷新缓冲区(一般写字符时要用,因为字符是先进入的缓冲区)，流对象还可以继续使用
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 向指定文件写入Base64
     *
     * @param file   指定文件
     * @param base64 要写入的Base64
     * @return 是否写入成功
     */
    public static boolean writeBase64String(File file, String base64) {
        FileOutputStream outputStream = null;
        try {
            if (file.exists()) {
                file.delete();
            }

            outputStream = new FileOutputStream(file.getAbsolutePath(), true);//append参数为true时，数据从文件尾部写入；append参数为false时，数据覆盖原文件。
            outputStream.write(Base64.decode(base64, Base64.DEFAULT));
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 从文件中读取字符串
     *
     * @param file 指定文件
     * @return 读取的字符串
     */
    public static String readText(File file) {
        if (!file.exists()) {
            return null;
        }
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file.getAbsolutePath());

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * 从指定文件读取Base64编码
     *
     * @param file 指定文件
     * @return Base64编码
     */
    public static String readBase64(File file) {
        if (!file.exists()) {
            return null;
        }
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        Base64OutputStream output64 = null;

        try {
            inputStream = new FileInputStream(file.getAbsolutePath());
            byte[] buffer = new byte[8192];
            int bytesRead;
            outputStream = new ByteArrayOutputStream();
            output64 = new Base64OutputStream(outputStream, Base64.DEFAULT);
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }
            return outputStream.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (output64 != null) {
                    output64.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * 从指定文件读取bitmap图片
     *
     * @param file 指定文件
     * @return bitmap图片
     */
    public static Bitmap readBitmap(File file) {
        if (!file.exists()) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        return bitmap;
    }

    /**
     * 清空缓存文件夹
     */
    public static void clearCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                clearDir(dir, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File dir = context.getExternalCacheDir();
            if (dir != null && dir.isDirectory()) {
                clearDir(dir, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @see #clearDir(File, boolean)
     */
    public static boolean clearDir(File dir) {
        return clearDir(dir, false);
    }

    /**
     * 清空指定文件夹
     *
     * @param dir       指定文件夹
     * @param deleteDir 是否清空后删除指定文件夹
     * @return 是否成功清空
     */
    public static boolean clearDir(File dir, boolean deleteDir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }

            if (deleteDir) {
                return deleteDir(dir);
            }
        }

        return true;
    }

    /**
     * 删除文件夹
     *
     * @param dir 指定文件夹
     * @return 是否删除成功
     */
    private static boolean deleteDir(File dir) {
        if (!clearDir(dir)) {
            return false;
        }

        if (dir != null) {
            return dir.delete();
        }

        return true;
    }
}
