package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;



public class SyncGithubToGitlab {

    private static final Logger logger = LogManager.getLogger(SyncGithubToGitlab.class);

    private static final int thread_num = 8;

    class Worker extends Thread {
        private final int i;

    public Worker(int i) {
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
                if (cnt % thread_num != i) {
                    ++cnt;
                    continue;
                }
                ++cnt;

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
                    continue;
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

    public static void main(String[] args) {

        if (!Util.loadGitlabSecret()) {
            logger.error("load gitlab_secret.json error!");
            System.exit(-1);
        }
        List<Worker> threads = Lists.newArrayList();
        for (int i = 0; i < thread_num; ++i) {
            Worker worker = new Worker(i);
            worker.start();
            threads.add(worker);
        }
    }
}
