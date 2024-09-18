package repair.modify.apply.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchAlgorithm {
    private final static Logger logger = LoggerFactory.getLogger(MatchAlgorithm.class);

    /**
     * from genpat
     */
    public static <T> Map<Integer, Integer> LCSMatch(List<T> src, List<T> tar, Comparator<T> comparator) {
        enum LCSDirection {
            UP, LEFT, UP_LEFT
        }

        Map<Integer, Integer> map = new HashMap<>();
        int srcLen = src.size();
        int tarLen = tar.size();
        if (srcLen == 0 || tarLen == 0) {
            return map;
        }
        int[][] score = new int[srcLen + 1][tarLen + 1];

        // LCS matching with path retrieval
        LCSDirection[][] path = null;
        try {
            path = new LCSDirection[srcLen + 1][tarLen + 1];
        } catch (OutOfMemoryError e) {
            logger.error("OutOfMemoryError when matching!");
            return map;
        }
        for (int i = 0; i < srcLen; i++) {
            for (int j = 0; j < tarLen; j++) {
                if (comparator.compare(src.get(i), tar.get(j)) > 0) {
                    score[i + 1][j + 1] = score[i][j] + 1;
                    path[i + 1][j + 1] = LCSDirection.UP_LEFT;
                } else {
                    int left = score[i + 1][j];
                    int up = score[i][j + 1];
                    if (left >= up) {
                        score[i + 1][j + 1] = left;
                        path[i + 1][j + 1] = LCSDirection.LEFT;
                    } else {
                        score[i + 1][j + 1] = up;
                        path[i + 1][j + 1] = LCSDirection.UP;
                    }
                }
            }
        }

        for (int i = srcLen, j = tarLen; i > 0 && j > 0; ) {
            switch (path[i][j]) {
                case UP_LEFT:
                    map.put(i - 1, j - 1);
                    i--;
                    j--;
                    break;
                case LEFT:
                    j--;
                    break;
                case UP:
                    i--;
                    break;
                default:
                    logger.error("should not happen!");
                    System.exit(0);
            }
        }

        assert map.size() == score[srcLen][tarLen];
        return map;
    }
}
