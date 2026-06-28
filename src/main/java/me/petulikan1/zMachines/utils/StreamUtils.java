package me.petulikan1.zMachines.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.petulikan1.zMachines.config.json.Json;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class StreamUtils {

    /**
     * @return String
     * @apiNote Read InputStream and convert into String with {@link System#lineSeparator()} as separator of lines
     */
    public static String fromStream(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) channel.size());
            channel.read(buffer);
            channel.close();
            buffer.flip();
            return decode(buffer);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decode(ByteBuffer buffer) {
        char[] charBuffer = new char[buffer.remaining()];
        int charPos = 0;

        while (buffer.hasRemaining()) {
            int firstByte = buffer.get() & 0xFF;

            if (firstByte <= 0x7F) {
                // 1-byte (ASCII)
                charBuffer[charPos++] = (char) firstByte;
            } else if (firstByte >> 5 == 0x6) {
                // 2-byte
                int secondByte = buffer.get() & 0xFF;
                charBuffer[charPos++] = (char) ((firstByte & 0x1F) << 6 | secondByte & 0x3F);
            } else if (firstByte >> 4 == 0xE) {
                // 3-byte
                int secondByte = buffer.get() & 0xFF;
                int thirdByte = buffer.get() & 0xFF;
                charBuffer[charPos++] = (char) ((firstByte & 0x0F) << 12 | (secondByte & 0x3F) << 6 | thirdByte & 0x3F);
            } else if (firstByte >> 3 == 0x1E) {
                // 4-byte
                int secondByte = buffer.get() & 0xFF;
                int thirdByte = buffer.get() & 0xFF;
                int fourthByte = buffer.get() & 0xFF;
                int codePoint = (firstByte & 0x07) << 18 | (secondByte & 0x3F) << 12 | (thirdByte & 0x3F) << 6 | fourthByte & 0x3F;
                codePoint -= 0x10000;
                charBuffer[charPos++] = (char) ((codePoint >> 10) + 0xD800);
                charBuffer[charPos++] = (char) ((codePoint & 0x3FF) + 0xDC00);
            } else {
                throw new IllegalArgumentException("Invalid UTF-8 encoding detected.");
            }
        }
        return new String(charBuffer, 0, charPos);
    }

    public static String decode(byte[] bytes) {
        char[] charBuffer = new char[bytes.length];
        int charPos = 0;

        for (int i = 0; i < bytes.length; ++i) {
            int firstByte = bytes[i] & 0xFF;

            if (firstByte <= 0x7F) {
                // 1-byte (ASCII)
                charBuffer[charPos++] = (char) firstByte;
            } else if (firstByte >> 5 == 0x6) {
                // 2-byte
                int secondByte = bytes[++i] & 0xFF;
                charBuffer[charPos++] = (char) ((firstByte & 0x1F) << 6 | secondByte & 0x3F);
            } else if (firstByte >> 4 == 0xE) {
                // 3-byte
                int secondByte = bytes[++i] & 0xFF;
                int thirdByte = bytes[++i] & 0xFF;
                charBuffer[charPos++] = (char) ((firstByte & 0x0F) << 12 | (secondByte & 0x3F) << 6 | thirdByte & 0x3F);
            } else if (firstByte >> 3 == 0x1E) {
                // 4-byte
                int secondByte = bytes[++i] & 0xFF;
                int thirdByte = bytes[++i] & 0xFF;
                int fourthByte = bytes[++i] & 0xFF;
                int codePoint = (firstByte & 0x07) << 18 | (secondByte & 0x3F) << 12 | (thirdByte & 0x3F) << 6 | fourthByte & 0x3F;
                codePoint -= 0x10000;
                charBuffer[charPos++] = (char) ((codePoint >> 10) + 0xD800);
                charBuffer[charPos++] = (char) ((codePoint & 0x3FF) + 0xDC00);
            } else {
                throw new IllegalArgumentException("Invalid UTF-8 encoding detected.");
            }
        }
        return new String(charBuffer, 0, charPos);
    }

    /**
     * @return String
     * @apiNote Read InputStream and convert into String with {@link System#lineSeparator()} as separator of lines
     */
    public static String fromStream(InputStream stream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 4096);
            StringBuilder sb = new StringBuilder(2048);
            String content;
            while ((content = br.readLine()) != null) {
                if (!sb.isEmpty())
                    sb.append(System.lineSeparator());
                sb.append(content);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return List<String>
     * @apiNote Read InputStream and convert into List<String> without seperator of lines
     */
    public static List<String> fromStreamToList(InputStream stream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 4096);
            List<String> lines = new ArrayList<>();
            String content;
            while ((content = br.readLine()) != null)
                lines.add(content);
            br.close();
            return lines;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return InputStream
     * @apiNote Write String into created InputStream
     */
    public static InputStream toStream(String text) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(text);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] fileToBytes(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             ByteArrayOutputStream byteOutput = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteOutput.write(buffer, 0, bytesRead);
            }

            return byteOutput.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] calculateSHA256(String filePath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return digest.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] calculateSHA256(File file) {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return digest.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String calculateSHA256Hex(String filePath) {
        return bytesToHex(calculateSHA256(filePath));
    }

    public static String calculateSHA256Hex(File file) {
        return bytesToHex(calculateSHA256(file));
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return null;
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] fileToBytes(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ByteArrayOutputStream byteOutput = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteOutput.write(buffer, 0, bytesRead);
            }

            return byteOutput.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void bytesToFile(byte[] bytes, String filePath) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void bytesToFile(byte[] bytes, File file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @return InputStream
     * @apiNote Write String into created InputStream
     */
    public static InputStream toStreamObject(Object obj) {
        return toStream(Json.writer().write(obj));
    }

    /**
     * @return Object
     * @apiNote Read Object from InputStream
     */
    public static Object fromStreamObject(InputStream stream) {
        return Json.reader().read(fromStream(stream));
    }

    /**
     * @return Object
     * @apiNote Read Object from InputStream
     */
    public static Object fromStreamObject(File file) {
        return Json.reader().read(fromStream(file));
    }
}
