package Common;

import Exceptions.IllegalFormatException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private static final Logger logger = LogManager.getLogger(Util.class);
    public static GitlabSecret gitlabSecret;

    public static GitLabApi gitLabApi;

    static {
        loadGitlabSecret();
    }

    public static boolean loadGitlabSecret() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream =
                     Util.class.getClassLoader().getResourceAsStream("gitlab_secret.json")) {
            if (inputStream == null) {
                throw new RuntimeException("File not found!");
            }
            gitlabSecret = objectMapper.readValue(inputStream, GitlabSecret.class);
            String jsonString = objectMapper.writeValueAsString(gitlabSecret);
            logger.info("loaed gitlab config: " + jsonString);
            gitLabApi = new GitLabApi(gitlabSecret.API_URL_BASE, gitlabSecret.API_TOKEN);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void loadFile(String path, Collection<String> list) {
        try (InputStream inputStream = new FileInputStream(new File(path))) {
            if (inputStream == null) {
                throw new RuntimeException("File not found!");
            }
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            while (line != null) {
                if (line.isEmpty() || line.trim().isEmpty()) {
                    line = br.readLine();
                    continue;
                }
                list.add(line.trim());
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            logger.error("input sync_github_gitlab_repos.txt error! " + e.getMessage());
        }
    }

    public static void outputToFile(String path, String content, boolean append) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(file, append));
            out.write(content);
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.error("outputToFile exception, path: " + path);
        }
    }

    public static boolean isDirExists(String full_path) {
        File dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
        return dir.exists() && dir.isDirectory();
    }

    public static boolean deleteDir(String full_path) {
        File dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
        return dir.delete();
    }

    public static boolean isValidLinuxFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return GenericValidator.matchRegexp(path, "(/[^/ ]*)*([^/ ]+(/[^/ ]*)*)?");
    }

    public static boolean isValidUrl(String url) {
        if (!url.endsWith(".git")) {
            return false;
        }

        if (!url.startsWith("git@") && !url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }
        if (url.startsWith("https://") || url.startsWith("http://")) {
            UrlValidator urlValidator = new UrlValidator();
            return urlValidator.isValid(url);
        } else if (url.startsWith("git@") && url.contains("/")) {
            String GIT_SSH_URL_REGEX = "^git@[\\w.-]+:[\\w.-]+/[\\w.-]+\\.git$";
            Pattern pattern = Pattern.compile(GIT_SSH_URL_REGEX);
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
        } else if (url.startsWith("git@") && !url.contains("/")) {
            String GIT_SSH_URL_REGEX = "^git@[\\w.-]+:[\\w.-]+\\.git$";
            Pattern pattern = Pattern.compile(GIT_SSH_URL_REGEX);
            Matcher matcher = pattern.matcher(url);
            return matcher.matches();
        }
        return false;

    }

    public static String getFullPath(String url) throws IllegalFormatException {
        if (!isValidUrl(url)) {
            logger.error(url + " error format!");
            throw new IllegalFormatException("error format!");
        }
        String full_path = url;
        if (full_path.startsWith("git@")) {
            full_path = full_path.substring(full_path.indexOf("git@") + 4);
            full_path = full_path.replace(":", "/");
        } else if (full_path.startsWith("http://")) {
            full_path = full_path.substring(full_path.indexOf("http://") + 7);
        } else if (full_path.startsWith("https://")) {
            full_path = full_path.substring(full_path.indexOf("https://") + 8);
        }
        full_path = full_path.substring(full_path.indexOf("/") + 1);
        if (!full_path.contains("/")) {
            full_path = "root/" + full_path;
        }
        return full_path;
    }

    public static String getGroup(String url) throws IllegalFormatException {
        String full_path = getFullPath(url);
        full_path = full_path.substring(0, full_path.lastIndexOf(".git"));
        if (!isValidLinuxFilePath(full_path)) {
            throw new IllegalFormatException("error format: " + full_path);
        }

        if (full_path.lastIndexOf("/") == -1) {
            throw new IllegalFormatException("error format: " + full_path);
        }

        return full_path.substring(0, full_path.lastIndexOf("/"));
    }

    public static String getName(String url) throws IllegalFormatException {
        String full_path = getFullPath(url);
        full_path = full_path.substring(0, full_path.lastIndexOf(".git"));
        if (!isValidLinuxFilePath(full_path)) {
            throw new IllegalFormatException("error format: " + full_path);
        }
        if (full_path.lastIndexOf("/") == -1) {
            throw new IllegalFormatException("error format: " + full_path);
        }
        return full_path.substring(full_path.lastIndexOf("/") + 1);
    }

    public static boolean isGitRepository(String full_path) {
        File repo_dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
        if (!repo_dir.exists() || !repo_dir.isDirectory()) {
            return false;
        }
        try {
            Repository repository =
                    new FileRepositoryBuilder().setGitDir(repo_dir).readEnvironment().findGitDir().build();
            repository.close();
            return true;
        } catch (IOException e) {
        }
        return false;
    }

    public static String getRemoteUrl(String full_path) {
        String ret = null;
        try {
            File file = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            FileRepositoryBuilder fileRepositoryBuilder = new FileRepositoryBuilder();
            Repository repository =
                    fileRepositoryBuilder.setGitDir(file).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            List<RemoteConfig> remoteConfigs = git.remoteList().call();
            for (RemoteConfig remoteConfig : remoteConfigs) {
                List<URIish> urIs = remoteConfig.getURIs();
                for (URIish uri : urIs) {
                    ret = uri.toString();
                }
            }
            repository.close();
        } catch (IOException e) {
            logger.error("getRemoteUrl: " + full_path + " error!");
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public static String getGitlabUrl(String full_path) {
        return gitlabSecret.GITLAB_URL_BASE + full_path;
    }

    public static boolean changeRemoteUrl(String full_path, String url) {
        try {
            File file = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            String[] args_init = new String[]{"remote", "set-url", "origin", url};
            ProcessBuilder builder = FS.DETECTED.runInShell("git", args_init);
            builder.directory(file);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("set remote-url error: " + full_path + ", " + url);
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static Project getGitlabProject(Object projectIdOrPath) {
        try {
            return gitLabApi.getProjectApi().getProject(projectIdOrPath);
        } catch (GitLabApiException e) {
            // logger.error(projectIdOrPath + " not exists");
        }
        return null;
    }

    public static boolean getAllGitlabProjects(List<Project> projects) {
        try {
            projects.addAll(gitLabApi.getProjectApi().getProjects());
            return true;
        } catch (GitLabApiException e) {
            e.printStackTrace();
            logger.error("getAllProjects error!");
        }
        return false;
    }

    public static void getAllGitlabSubGroups(Group parent_group, List<Group> groups) {
        try {
            List<Group> sub_groups = gitLabApi.getGroupApi().getSubGroups(parent_group.getId());
            groups.addAll(sub_groups);
            for (Group group : sub_groups) {
                getAllGitlabSubGroups(group, groups);
            }
        } catch (GitLabApiException e) {
            logger.error(e.getMessage());
        }
    }

    public static boolean getAllGitlabGroups(List<Group> groups) {
        try {
            groups.addAll(gitLabApi.getGroupApi().getGroups());
            List<Group> tmp = Lists.newArrayList(groups);
            for (Group group : tmp) {
                getAllGitlabSubGroups(group, groups);
            }
            return true;
        } catch (GitLabApiException e) {
            logger.error("getAllGitlabGroups error!");
        }
        return false;
    }

    public static boolean cloneProject(String url) {
        try {
            String full_path = getFullPath(url);
            File repo_dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            if (!repo_dir.exists()) {
                if (!repo_dir.mkdirs()) {
                    logger.error("mkdir error: " + full_path);
                }
            }
            String[] CLONE_ARGS = new String[]{"clone", "--bare", url, repo_dir.getAbsolutePath()};
            ProcessBuilder builder = FS.DETECTED.runInShell("git", CLONE_ARGS);
            builder.directory(repo_dir);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            return ret == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean updateProject(String full_path) {
        try {
            File repo_dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            String[] FETCH_ARGS = new String[]{"fetch", "--all"};
            ProcessBuilder builder = FS.DETECTED.runInShell("git", FETCH_ARGS);
            builder.directory(repo_dir);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("fetch --all error: " + full_path);
                return false;
            }

            repo_dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            FETCH_ARGS = new String[]{"lfs", "fetch", "--all"};
            builder = FS.DETECTED.runInShell("git", FETCH_ARGS);
            builder.directory(repo_dir);
            os = new ByteArrayOutputStream();
            ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("lfs fetch --all error: " + full_path);
                return false;
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean pushProject(String full_path) {
        try {
            File repo_dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            full_path = full_path.substring(0, full_path.lastIndexOf(".git"));
            Project project = getGitlabProject(full_path);
            if (project == null) {
                String url = getRemoteUrl(full_path + ".git");
                project = createProject(url);
            }

            String[] PUSH_ARGS = new String[]{"fetch", "--all"};
            ProcessBuilder builder = FS.DETECTED.runInShell("git", PUSH_ARGS);
            builder.directory(repo_dir);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("fetch --all error: " + full_path + ", msg: " + os);
                return false;
            }

            PUSH_ARGS = new String[]{"push", , "-f", "-u", "origin" ,"--all"};
            builder = FS.DETECTED.runInShell("git", PUSH_ARGS);
            builder.directory(repo_dir);
            os = new ByteArrayOutputStream();
            ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("push --all error: " + full_path + ", msg: " + os);
                return false;
            }

            PUSH_ARGS = new String[]{"push", "--tags"};
            builder = FS.DETECTED.runInShell("git", PUSH_ARGS);
            builder.directory(repo_dir);
            os = new ByteArrayOutputStream();
            ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("push --tags error: " + full_path + ", msg: " + os);
                return false;
            }

            PUSH_ARGS = new String[]{"lfs", "push", "--all", "origin"};
            builder = FS.DETECTED.runInShell("git", PUSH_ARGS);
            builder.directory(repo_dir);
            os = new ByteArrayOutputStream();
            ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                logger.error("lfs push --all origin error: " + full_path + ", msg: " + os);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("push error: " + full_path);
        }
        return false;
    }

    public static Group createGroup(String url) throws IllegalFormatException {
        String group_full_path = Util.getGroup(url);
        if (group_full_path.isEmpty()) {
            return null;
        }
        String[] group_names = group_full_path.split("/");
        Group parentGroup = null;
        Group curGroup = null;
        String groupPath = "";
        for (int i = 0; i < group_names.length; i++) {
            groupPath += (i > 0 ? "/" : "") + group_names[i];
            try {
                curGroup = gitLabApi.getGroupApi().getGroup(groupPath);
            } catch (GitLabApiException e) {
                if (e.getHttpStatus() == 404) {
                    curGroup = new Group().withName(group_names[i]).withPath(group_names[i]);
                    if (parentGroup != null) {
                        curGroup.withParentId(parentGroup.getId());
                    }
                    try {
                        curGroup = gitLabApi.getGroupApi().addGroup(curGroup);
                    } catch (GitLabApiException ex) {
                        logger.error("create group error: " + curGroup.getFullPath());
                        return null;
                    }
                } else {
                    return null;
                }
            }
            parentGroup = curGroup;
        }
        return curGroup;
    }

    public static Project createProject(String url) {
        try {
            Group group = createGroup(url);
            if (group == null) {
                return null;
            }

            String project_name = getName(url);
            Project project =
                    new Project()
                            .withPath(project_name)
                            .withName(project_name)
                            .withIssuesEnabled(true)
                            .withMergeRequestsEnabled(true)
                            .withWikiEnabled(true)
                            .withPublic(false)
                            .withLfsEnabled(true);

            Optional<Project> projectOptional;
            if (group != null) {
                project.withNamespaceId(group.getId());
                projectOptional =
                        gitLabApi.getProjectApi().getOptionalProject(group.getFullPath() + "/" + project_name);
            } else {
                projectOptional = gitLabApi.getProjectApi().getOptionalProject(project_name);
            }

            if (projectOptional.isPresent()) {
                return projectOptional.get();
            }
            project = gitLabApi.getProjectApi().createProject(project);
            return project;
        } catch (GitLabApiException | IllegalFormatException e) {
            logger.error("create project error: " + url);
        }
        return null;
    }

    public static boolean deleteProject(String url) {
        try {
            String full_path = getFullPath(url);
            full_path = full_path.substring(0, full_path.lastIndexOf(".git"));
            Project project = getGitlabProject(full_path);

            gitLabApi.getProjectApi().deleteProject(project.getId());
        } catch (GitLabApiException | IllegalFormatException | NullPointerException e) {
            logger.error("delete project error: " + url);
            return false;
        }
        return true;
    }

    public static Project getProject(int project_id) {
        try {
            Optional<Project> projectOptional;
            projectOptional = gitLabApi.getProjectApi().getOptionalProject(project_id);
            if (projectOptional.isPresent()) {
                return projectOptional.get();
            }
            return null;
        } catch ( Exception e) {
            logger.error("create project error: " + project_id);
        }
        return null;
    }
}
