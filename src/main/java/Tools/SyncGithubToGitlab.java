package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class SyncGithubToGitlab {
    private static final Logger logger = LogManager.getLogger(SyncGithubToGitlab.class);

    public static void main(String[] args) {
        if (!Util.loadGitlabSecret()) {
            logger.error("load gitlab_secret.json error!");
            System.exit(-1);
        }

        List<String> repos = Lists.newArrayList();
        Util.loadFile("sync_github_gitlab_repos.txt", repos);
        for (String repo : repos) {
            try {
                if (repo.startsWith("#")) {
                    logger.info("ignore: " + repo);
                    continue;
                }

                String full_path = Util.getFullPath(repo);
                if (Util.isGitRepository(full_path)) {
                    if (!Util.changeRemoteUrl(full_path, repo)) {
                        logger.error("change url error: " + repo);
                        continue;
                    }
                    if(!Util.updateProject(full_path)) {
                        logger.error("update error: " + repo);
                        continue;
                    }
                } else if (Util.isDirExists(full_path)) {
                    if (!Util.deleteDir(full_path)) {
                        logger.error("delete error: " + repo);
                        continue;
                    }
                    if(!Util.cloneProject(repo)) {
                        logger.error("clone error: " + repo);
                        continue;
                    }
                } else if (!Util.isDirExists(full_path)) {
                    if(!Util.cloneProject(repo)) {
                        logger.error("clone error: " + repo);
                        continue;
                    }
                }

                if (!Util.checkoutAllBranch(full_path)) {
                    logger.error("checkout all branch error: " + repo);
                }

                String gitlab_url = Util.getGitlabUrl(full_path);
                if (!Util.changeRemoteUrl(full_path, gitlab_url)) {
                    logger.error("change url error: " + gitlab_url);
                    continue;
                }
                if(!Util.pushProject(full_path)) {
                    logger.error("push error: " + repo);
                    continue;
                } else {
                    logger.info("push success: " + repo);
                }

                Util.changeRemoteUrl(full_path, repo);
            } catch (Exception e) {
                logger.info("process error: " + repo);
                e.printStackTrace();
            }
        }
    }
}
