package Tools;

import Common.Util;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Worker extends Thread {
    private static final Logger logger = LogManager.getLogger(Worker.class);
    private final int i;
    private int thread_num;
    private final List<String> repos;

    public Worker(int i, List<String> repos) {
        this.i = i;
        this.repos = repos;
    }

    @Override
    public void run() {
        String repo = null;
        while (true) {
            try {
                synchronized (repos) {
                    if (repos.isEmpty()) {
                        break;
                    }
                    repo = repos.removeFirst();
                }
                if (repo.startsWith("#")) {
                    logger.info("thread_num: " + i + " ignore: " + repo);
                    continue;
                }

                String full_path = Util.getFullPath(repo);
                if (Util.isGitRepository(full_path)) {
                    if (!Util.changeRemoteUrl(full_path, repo)) {
                        logger.error("thread_num: " + i + " change url error: " + repo);
                        continue;
                    }
                } else if (Util.isDirExists(full_path)) {
                    if (!Util.deleteDir(full_path)) {
                        logger.error("thread_num: " + i + " delete error: " + repo);
                        continue;
                    }
                    if (!Util.cloneProject(repo)) {
                        logger.error("thread_num: " + i + " clone error: " + repo);
                        continue;
                    }
                } else if (!Util.isDirExists(full_path)) {
                    if (!Util.cloneProject(repo)) {
                        logger.error("thread_num: " + i + " clone error: " + repo);
                        continue;
                    }
                }

                if (!Util.updateProject(full_path)) {
                    logger.error("thread_num: " + i + " update error: " + repo);
                    continue;
                }

                String gitlab_url = Util.getGitlabUrl(full_path);
                if (!Util.changeRemoteUrl(full_path, gitlab_url)) {
                    logger.error("thread_num: " + i + " change url error: " + gitlab_url);
                    continue;
                }
                if (!Util.pushProject(full_path)) {
                    logger.error("thread_num: " + i + " push error: " + repo);
                    continue;
                } else {
                    logger.info("thread_num: " + i + " push success: " + repo);
                }

                Util.changeRemoteUrl(full_path, repo);
            } catch (Exception e) {
                logger.info("thread_num: " + i + " process error: " + repo);
                e.printStackTrace();
            }
        }
    }
}

public class SyncGithubToGitlab {
    private static final Logger logger = LogManager.getLogger(SyncGithubToGitlab.class);
    private static final int thread_num = 8;
    public static void main(String[] args) {
        if (!Util.loadGitlabSecret()) {
            logger.error("load gitlab_secret.json error!");
            System.exit(-1);
        }

        List<String> repos = Lists.newArrayList();
        //Util.loadFile("D:\\code\\java\\GitManager\\src\\main\\resources\\sync_github_gitlab_repos.txt", repos);
        Util.loadFile("/root/src/java/GitManager/src/main/resources/sync_github_gitlab_repos.txt", repos);
        List<Worker> threads =  Lists.newArrayList();
        for (int i = 0; i < thread_num; ++i) {
            Worker worker = new Worker(i, repos);
            worker.start();
            threads.add(worker);
        }
    }
}
