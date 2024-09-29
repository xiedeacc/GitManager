package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;


public class FixFullPath {

    private static final Logger logger = LogManager.getLogger(FixFullPath.class);

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        List<String> repos = Lists.newArrayList();
        Util.loadFile("D:\\code\\java\\\\GitManager\\src\\main\\resources\\repos_map", repos);
        List<FixFullPath.Item> items = Lists.newArrayList();
        for (String line : repos) {
            String[] parts = line.split(",");
            if (parts.length != 2) {
                logger.error("wrong line: " + line);
            }
            int id = Integer.valueOf(parts[0]);
            String fullpath = parts[1];
            String sha256 = Util.getProjectIdSha256(id);
            String dir_path = Util.getProjectHashPath(sha256);

            FixFullPath.Item item = new FixFullPath.Item();
            item.setId(id);
            item.setFull_path(fullpath);
            item.setDir_path(dir_path);
            items.add(item);
        }

        items.sort(new Comparator<FixFullPath.Item>() {
            @Override
            public int compare(FixFullPath.Item p1, FixFullPath.Item p2) {
                return Integer.compare(p1.id, p2.id);
            }
        });

        for (FixFullPath.Item item : items) {
            logger.info(item.getId() + "," + item.getDir_path() + "," + item.getFull_path());
        }
    }

    static class Item {
        int id;
        String dir_path;
        String full_path;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getDir_path() {
            return dir_path;
        }

        public void setDir_path(String dir_path) {
            this.dir_path = dir_path;
        }

        public String getFull_path() {
            return full_path;
        }

        public void setFull_path(String full_path) {
            this.full_path = full_path;
        }
    }
}
