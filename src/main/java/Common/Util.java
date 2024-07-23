package Common;

import Exceptions.IllegalFormatException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.stream.JsonReader;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupParams;
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

    static {
        loadGitlabSecret();
    }

    public static boolean loadGitlabSecret() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = Util.class.getClassLoader().getResourceAsStream("gitlab_secret.json")) {
            if (inputStream == null) {
                throw new RuntimeException("File not found!");
            }
            gitlabSecret = objectMapper.readValue(inputStream, GitlabSecret.class);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static final GitLabApi gitLabApi = new GitLabApi(GitLabApi.ApiVersion.V4, gitlabSecret.API_URL_BASE, gitlabSecret.API_TOKEN);
    public static boolean isDirExists(String path) {
        File dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + path);
        return dir.exists() && dir.isDirectory();
    }
    public static boolean deleteDir(String path) {
        File dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + path);
        return dir.delete();
    }
    public static void loadFile(String file_name, Collection<String> list) {
        try {
            String manual_need_download_project_url_path = Util.class.getClassLoader().getResource(file_name).getPath();
            File file = new File(manual_need_download_project_url_path);
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
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
            logger.error("input " + file_name + " error! " + e.getMessage());
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
    public static boolean isValidLinuxFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return GenericValidator.matchRegexp(path, "(/[^/ ]*)*([^/ ]+(/[^/ ]*)*)?");
    }
    public static boolean isValidURL(String url) {
        UrlValidator urlValidator = new UrlValidator();
        return urlValidator.isValid(url);
    }
    public static boolean isValidGitSSHUrl(String url) {
        Pattern pattern = Pattern.compile("git@([\\w.-]+):(\\w+)/(.+).git");
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }
    public static boolean isValidGitUrl(String url) {
        return isValidGitSSHUrl(url) || isValidURL(url);
    }
    public static boolean isSSHUrl(String url) {
        return isValidGitUrl(url ) && url.startsWith("git@");
    }
    public static boolean isHttpUrl(String url) {
        return isValidGitUrl(url ) && url.startsWith("http://");
    }
    public static boolean isHttpsUrl(String url) {
        return isValidGitUrl(url ) && url.startsWith("https://");
    }
    public static String getFullPath(String url) throws IllegalFormatException {
        if (!isValidGitUrl(url)) {
            logger.error(url + " error format!");
            throw new IllegalFormatException("error format!");
        }

        String full_path = url.substring(0, url.indexOf(".git"));
        if (isSSHUrl(url)) {
            full_path = full_path.substring(full_path.indexOf("git@") + 4);
            full_path = full_path.replace(":", "/");
        } else if (isHttpUrl(url)) {
            full_path = full_path.substring(full_path.indexOf("http://") + 7);
        } else if (isHttpsUrl(url)) {
            full_path = full_path.substring(full_path.indexOf("https://") + 8);
        }
        return full_path.substring(full_path.indexOf("/") + 1);
    }
    public static String getGroup(String url) throws IllegalFormatException {
        String full_path = url;
        if (isValidGitUrl(url)) {
            full_path = getFullPath(url);
        }

        if (!isValidLinuxFilePath(full_path)) {
            throw new IllegalFormatException("error format: " + full_path);
        }
        return full_path.substring(0, full_path.lastIndexOf("/"));
    }
    public static String getName(String url) throws IllegalFormatException {
        String full_path = url;
        if (isValidGitUrl(url)) {
            full_path = getFullPath(url);
        }

        if (!isValidLinuxFilePath(full_path)) {
            throw new IllegalFormatException("error format: " + full_path);
        }
        return full_path.substring(full_path.lastIndexOf("/") + 1);
    }
    public static boolean isGitRepository(String full_path) {
        File gitDir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            return false;
        }

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(gitDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();
            repository.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static String getRemoteUrl(String full_path) {
        try {
            File file = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path, ".git");
            FileRepositoryBuilder fileRepositoryBuilder = new FileRepositoryBuilder();
            Repository repository = fileRepositoryBuilder.setGitDir(file)
                    .readEnvironment()
                    .findGitDir()
                    .build();
            return repository.getConfig().getString("remote", "origin", "url");
        } catch (IOException e) {
            logger.error("getRemoteUrl: " + full_path + " error!");
        }
        return null;
    }
    public static String getGitlabUrl(String full_path) {
        return gitlabSecret.GITLAB_URL_BASE + full_path;
    }
    public static boolean changeRemoteUrl(String path, String url) {
        try {
            File file = new File(path);
            String[] args_init = new String[]{"remote", "set-url", "origin", url};
            ProcessBuilder builder = FS.DETECTED.runInShell("git", args_init);
            builder.directory(file);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static Project getGitlabProject(Object projectIdOrPath ) throws GitLabApiException {
        Project project = new Project();
        project.setId(50);
        return gitLabApi.getProjectApi().getProject(projectIdOrPath);
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
            File dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String[] FETCH_CLONE = new String[]{"clone", url, dir.getAbsolutePath()};
            ProcessBuilder builder = FS.DETECTED.runInShell("git", FETCH_CLONE);
            builder.directory(dir);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }
    public static boolean updateProject(String full_path) {
        try {
            File repo_dir = new File(gitlabSecret.CODE_PATH_BASE + File.separator + full_path);
            String[] PULL_ARGS = new String[]{"pull", "--all"};
            String url = getRemoteUrl(full_path);
            ProcessBuilder builder = FS.DETECTED.runInShell("git", PULL_ARGS);
            builder.directory(repo_dir);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
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
            String url = getRemoteUrl(full_path);
            String[] PUSH_ARGS = new String[]{"push", "--all"};
            logger.info("now git push " + url);
            ProcessBuilder builder = FS.DETECTED.runInShell("git", PUSH_ARGS);
            builder.directory(repo_dir);
            OutputStream os = new ByteArrayOutputStream();
            int ret = FS.DETECTED.runProcess(builder, os, os, (String) null);
            if (ret != 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("push error: " + full_path);
        }
        return false;
    }
    public static Group createGroup(String url) throws IllegalFormatException {
        GroupParams groupParams = new GroupParams();
        Group group = new Group();
        String group_full_path = Util.getGroup(url);
        String[] group_names = group_full_path.split("/");
        int i = 0;
        String current_group_full_path = "";
        while (i < group_names.length) {
            try {
                current_group_full_path = i == 0 ? group_names[0] : current_group_full_path + "/" + group_names[i];
                Optional<Group> optionalGroup = gitLabApi.getGroupApi().getOptionalGroup(current_group_full_path);
                if (optionalGroup.isPresent()) {
                    group = optionalGroup.get();
                    ++i;
                    continue;
                }
                groupParams.withName(group_names[i]);
                groupParams.withPath(group_names[i]);
                groupParams.withParentId(group.getId());
                group = gitLabApi.getGroupApi().createGroup(groupParams);
            } catch (GitLabApiException e) {
                logger.error(e.getMessage());
            }
            ++i;
        }
        return null;
    }

    public static Project createProject(String url) {
        try {
            Group group = createGroup(url);

            if (group == null) {
                logger.error("create group error: " + url);
                return null;
            }

            String project_name = getName(url);
            Project project = new Project()
                    .withPath(project_name)
                    .withName(project_name)
                    .withNamespaceId(group.getId())
                    .withIssuesEnabled(true)
                    .withMergeRequestsEnabled(true)
                    .withWikiEnabled(true)
                    .withPublic(false)
                    .withLfsEnabled(true);

            Optional<Project> projectOptional = gitLabApi.getProjectApi().getOptionalProject(group.getFullPath() + "/" + project_name);
            if (projectOptional.isPresent()) {
                logger.info(projectOptional.get().getPathWithNamespace() + " already exists!");
                return projectOptional.get();
            }
            project = gitLabApi.getProjectApi().createProject(project);
            return project;
        } catch (GitLabApiException | IllegalFormatException e) {
            logger.error("create project error: " + url);
        }
        return null;
    }
}
