package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


class Work extends Thread {
    private static final Logger logger = LogManager.getLogger(SyncGithubToGitlab.class);
    private int i;

    public Work(int i) {
        this.i = i;
    }

    @Override
    public void run() {
        List<String> repos = Lists.newArrayList();
        //Util.loadFile("D:\\code\\java\\GitManager\\src\\main\\resources\\sync_github_gitlab_repos.txt", repos);
        Util.loadFile("/root/src/java/GitManager/src/main/resources/sync_github_gitlab_repos.txt", repos);
        int cnt = 0;
        for (String repo : repos) {
            try {
                if (repo.startsWith("#")) {
                    logger.info("ignore: " + repo);
                    continue;
                }

                if (cnt % 8 != i) {
                    ++cnt;
                    continue;
                }
                ++cnt;

                String full_path = Util.getFullPath(repo);
                if (Util.isGitRepository(full_path)) {
                    if (!Util.changeRemoteUrl(full_path, repo)) {
                        logger.error("change url error: " + repo);
                        continue;
                    }
                } else if (Util.isDirExists(full_path)) {
                    if (!Util.deleteDir(full_path)) {
                        logger.error("delete error: " + repo);
                        continue;
                    }
                    if (!Util.cloneProject(repo)) {
                        logger.error("clone error: " + repo);
                        continue;
                    }
                } else if (!Util.isDirExists(full_path)) {
                    if (!Util.cloneProject(repo)) {
                        logger.error("clone error: " + repo);
                        continue;
                    }
                }

                if (!Util.updateProject(full_path)) {
                    logger.error("update error: " + repo);
                }

                String gitlab_url = Util.getGitlabUrl(full_path);
                if (!Util.changeRemoteUrl(full_path, gitlab_url)) {
                    logger.error("change url error: " + gitlab_url);
                    continue;
                }
                if (!Util.pushProject(full_path)) {
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

public class SyncGithubToGitlab {

    private static final Logger logger = LogManager.getLogger(SyncGithubToGitlab.class);

    public static void main(String[] args) {

        if (!Util.loadGitlabSecret()) {
            logger.error("load gitlab_secret.json error!");
            System.exit(-1);
        }
        Work work0 = new Work(0);
        Work work1 = new Work(1);
        Work work2 = new Work(2);
        Work work3 = new Work(3);
        Work work4 = new Work(4);
        Work work5 = new Work(5);
        Work work6 = new Work(6);
        Work work7 = new Work(7);
        Work work8 = new Work(8);

        work0.start();
        work1.start();
        work2.start();
        work3.start();
        work4.start();
        work5.start();
        work6.start();
        work7.start();
        work8.start();
    }
}
