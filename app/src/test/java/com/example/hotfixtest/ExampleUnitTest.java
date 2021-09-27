package com.example.hotfixtest;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws IOException {

        assertEquals(4, 2 + 2);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Process process = Runtime.getRuntime().exec("java -version");
        InputStream io = process.getErrorStream();
        int len;
        byte[] buffer = new byte[4096];
        while ((len = io.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }

        System.out.println(new String(bos.toByteArray()));
    }
}