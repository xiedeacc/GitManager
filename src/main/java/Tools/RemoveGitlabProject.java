package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class RemoveGitlabProject {
    private static final Logger logger = LogManager.getLogger(RemoveGitlabProject.class);

    public static void main(String[] args) {
        if (!Util.loadGitlabSecret()) {
            logger.error("load gitlab_secret.json error!");
            System.exit(-1);
        }

        List<String> repos = Lists.newArrayList();
        //Util.loadFile("D:\\code\\java\\GitManager\\src\\main\\resources\\sync_github_gitlab_repos.txt", repos);
        Util.loadFile("/root/src/java/GitManager/src/main/resources/sync_github_gitlab_repos.txt", repos);
        for (String repo : repos) {
            try {
                if (repo.startsWith("#")) {
                    logger.info("ignore: " + repo);
                    continue;
                }

                String full_path = Util.getFullPath(repo);
                String gitlab_url = Util.getGitlabUrl(full_path);
                if (!Util.deleteProject(gitlab_url)) {
                    logger.error("delete repo error: " + full_path);
                    continue;
                }

                logger.info("delete repo success: " + full_path);
            } catch (Exception e) {
                logger.info("process error: " + repo);
                e.printStackTrace();
            }
        }
    }
}
