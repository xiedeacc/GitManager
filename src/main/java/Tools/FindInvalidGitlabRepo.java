package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gitlab4j.api.models.Project;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindInvalidGitlabRepo {

    private static final Logger logger = LogManager.getLogger(SyncGithubToGitlab.class);

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        List<String> repos = Lists.newArrayList();
        //Util.loadFile("/root/src/java/GitManager/src/main/resources/gitlab_repos.txt", repos);
        Util.loadFile("D:\\code\\java\\\\GitManager\\src\\main\\resources\\gitlab_repos.txt", repos);
        List<String> right_repos = Lists.newArrayList();
        List<String> invalidate_repos = Lists.newArrayList();
        int cnt = 0;
        for (int i = 0; i < repos.size(); ) {
            String line = repos.get(i);
            if (line.contains(".git")) {
                if (i + 1 < repos.size() && repos.get(i + 1).contains("fullpath")) {
                    right_repos.add(line);
                    right_repos.add(repos.get(i + 1));
                    i = i + 2;
                    continue;
                }
            }
            // logger.error("invalid: " + line);
            invalidate_repos.add(line);
            ++cnt;
            ++i;
        }
        logger.info("invalid num: " + cnt);

        Map<String, Integer> id_sha256  = new HashMap<>();
        for (int i = 0; i < 30000; ++i) {
            String input = String.valueOf(i);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            id_sha256.put(hexString.toString(), i);
        }

        for (String invalidate_repo : invalidate_repos) {
            String hash = invalidate_repo.substring(44);
            hash = hash.substring(0, 64);
            int project_id = id_sha256.get(hash);
            Project project = Util.getProject(project_id);
            if (project == null) {
                logger.error("cannot find repo, hash: " + hash + ", project id: "  + project_id + ", path: " + invalidate_repo);
            } else {
                logger.info(invalidate_repo + " repo project_id: " + project_id + ", name: " + project.getPathWithNamespace() );
            }
        }
    }
}
