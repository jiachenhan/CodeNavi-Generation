/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package repair.common;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;

import java.io.*;
import java.util.*;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static boolean moveFile(String src, String tar) {
        File file = new File(src);
        if (file.exists()) {
            try {
                FileUtils.moveFile(file, new File(tar));
            } catch (IOException e) {
                logger.error("Backup previous out file failed! " + src);
                return false;
            }
        }
        return true;
    }

    public static boolean deleteDirs(File dir) {
        boolean result = true;
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                logger.error("Delete directory failed!", e);
                result = false;
            }
        }
        return result;
    }

    public static boolean deleteDirs(String... dirs) {
        boolean result = true;
        File file;
        for (String dir : dirs) {
            file = new File(dir);
            if (file.exists()) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    logger.error("Delete directory failed!", e);
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean deleteFiles(File f) {
        boolean result = true;
        if (f.exists()) {
            try {
                FileUtils.forceDeleteOnExit(f);
            } catch (IOException e) {
                logger.error("Delete file failed!", e);
                result = false;
            }
        }
        return result;
    }

    public static boolean deleteFiles(String... files) {
        boolean result = true;
        File file;
        for (String f : files) {
            file = new File(f);
            if (file.exists()) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    logger.error("Delete file failed!", e);
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean copyDir(File srcFile, File tarFile) {
        try {
            FileUtils.copyDirectory(srcFile, tarFile);
        } catch (IOException e) {
            logger.error("Copy dir from " + srcFile.getAbsolutePath() +
                    " to " + tarFile.getAbsolutePath() + " " + "failed", e);
            return false;
        }
        return true;
    }

    public static boolean copyDir(String src, String tar) {
        return copyDir(new File(src), new File(tar));
    }

    public static boolean safeCollectionEqual(Set<String> c1, Set<String> c2) {
        if (c1 == c2) return true;
        if (c1 == null || c2 == null) {
            return false;
        }
        if (c1.size() == c2.size()) {
            for (String s : c1) {
                if (!c2.contains(s)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean safeBufferEqual(StringBuffer s1, StringBuffer s2) {
        if (s1 == s2) return true;
        if (s1 == null || s2 == null) return false;
        return s1.toString().equals(s2.toString());
    }

    public static boolean safeStringEqual(String s1, String s2) {
        if(s1 == s2) return true;
        if(s1 == null) return false;
        return s1.equals(s2);
    }

    public static String join(char delimiter, String... element) {
        return join(delimiter, Arrays.asList(element));
    }

    public static String join(char delimiter, List<String> elements) {
        StringBuffer buffer = new StringBuffer();
        if (elements.size() > 0) {
            buffer.append(elements.get(0));
        }
        for (int i = 1; i < elements.size(); i++) {
            buffer.append(delimiter);
            buffer.append(elements.get(i));
        }
        return buffer.toString();
    }

    public static boolean checkCompatiblePut(String obj1, String obj2, Map<String, String> map) {
        if(map.containsKey(obj1)) {
            if (!map.get(obj1).equals(obj2)) {
                return false;
            }
        } else {
            map.put(obj1, obj2);
        }
        return true;
    }

    public static boolean checkCompatibleBidirectionalPut(MoNode obj1, MoNode obj2, Map<MoNode, MoNode> map) {
        MoNode n1 = map.get(obj1);
        MoNode n2 = map.get(obj2);
        if (n1 == null && n2 == null) {
            map.put(obj1, obj2);
            map.put(obj2, obj1);
            return true;
        }
        if (n1 == obj2 && n2 == obj1) {
            return true;
        }
        return false;
    }


    private static int binarySearch(double [] prob, int b, int e, double r) {
        if (b == e - 1) {
            return b;
        }
        int mid = (b + e) / 2;
        if (r < prob[mid]) {
            return binarySearch(prob, b, mid, r);
        } else {
            return binarySearch(prob, mid, e, r);
        }
    }

    public synchronized static void serialize(Serializable object, String fileName) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    public synchronized static Serializable deserialize(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
        return (Serializable) objectInputStream.readObject();
    }


    private static List<String> getJarFile(File path) {
        List<String> jars = new ArrayList<>();
        if (path.isFile()) {
            String file = path.getAbsolutePath();
            if (file.endsWith(".jar")) {
                jars.add(file);
            }
        } else if (path.isDirectory()) {
            File[] files = path.listFiles();
            for (File f : files) {
                jars.addAll(getJarFile(f));
            }
        }
        return jars;
    }

}
