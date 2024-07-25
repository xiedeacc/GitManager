import Common.Util;
import Exceptions.IllegalFormatException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UtilTest {
  private static final Logger logger = LogManager.getLogger(UtilTest.class);

  @BeforeEach
  public void setUp() {
    if (!Util.loadGitlabSecret()) {
      logger.error("load gitlab_secret.json error!");
      System.exit(-1);
    }
  }

  @Test
  public void testIsValidUrl() {
    String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidURL(ori_url) ? "right" : "wrong");

    ori_url = "git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidURL(ori_url) ? "right" : "wrong");

    ori_url = "git.github.com/code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidURL(ori_url) ? "right" : "wrong");

    ori_url = "https://git.github.com/code/aaa/bbb/ccc.git"; // right
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidURL(ori_url) ? "right" : "wrong");
  }

  @Test
  public void testIsValidGitSSHUrl() {
    String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitSSHUrl(ori_url) ? "right" : "wrong");

    ori_url = "git@git.github.com/code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitSSHUrl(ori_url) ? "right" : "wrong");

    ori_url = "git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitSSHUrl(ori_url) ? "right" : "wrong");

    ori_url = "git.github.com/code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitSSHUrl(ori_url) ? "right" : "wrong");

    ori_url = "git@github.com:user/repo.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitSSHUrl(ori_url) ? "right" : "wrong");
  }

  @Test
  public void testIsValidGitUrl() {
    String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");

    ori_url = "git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");

    ori_url = "git@git.github.com/code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");

    ori_url = "git@git.github.com/code/aaa/bbb/ccc";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");

    ori_url = "http://git.github.com:code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");

    ori_url = "http://git.github.com/code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");

    ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
    logger.info("ori_url: " + ori_url);
    logger.info(Util.isValidGitUrl(ori_url) ? "right" : "wrong");
  }

  @Test
  public void testGetFullPath() {
    String full_path = null;
    try {
      String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getFullPath(ori_url);
      logger.info("full_path: " + full_path);

      ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getFullPath(ori_url);
      logger.info("full_path: " + full_path);

    } catch (IllegalFormatException e) {
      // throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetGroup() {
    String full_path = null;
    try {
      String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getGroup(ori_url);
      logger.info("name: " + full_path);

      ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getGroup(ori_url);
      logger.info("name: " + full_path);

      ori_url = "code/aaa/bbb";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getGroup(ori_url);
      logger.info("name: " + full_path);

    } catch (IllegalFormatException e) {
      // throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetName() {
    String full_path = null;
    try {
      String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getName(ori_url);
      logger.info("name: " + full_path);

      ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getName(ori_url);
      logger.info("name: " + full_path);

    } catch (IllegalFormatException e) {
      // throw new RuntimeException(e);
    }
  }

  @Test
  public void testGetFullPath() {
    String full_path = null;
    try {
      String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getFullPath(ori_url);
      logger.info("full_path: " + full_path);

      ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getFullPath(ori_url);
      logger.info("full_path: " + full_path);

    } catch (IllegalFormatException e) {

    }
  }

  @Test
  public void testGetGroup() {
    String full_path = null;
    try {
      String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getGroup(ori_url);
      logger.info("name: " + full_path);

      ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getGroup(ori_url);
      logger.info("name: " + full_path);

      ori_url = "code/aaa/bbb";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getGroup(ori_url);
      logger.info("name: " + full_path);

    } catch (IllegalFormatException e) {

    }
  }

  @Test
  public void testGetName() {
    String full_path = null;
    try {
      String ori_url = "git@git.github.com:code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getName(ori_url);
      logger.info("name: " + full_path);

      ori_url = "https://git.github.com/code/aaa/bbb/ccc.git";
      logger.info("ori_url: " + ori_url);
      full_path = Util.getName(ori_url);
      logger.info("name: " + full_path);

    } catch (IllegalFormatException e) {

    }
  }

  @Test
  public void testGetRemoteUrl() {
    String ret = Util.getRemoteUrl("D:\\share");
    logger.info("result: " + ret);
  }

  @Test
  public void testChangeRemoteUrl() {
    Boolean ret = Util.changeRemoteUrl("D:\\share", "git@github.com:xiedeacc/share.git");
    // Boolean ret = Util.changeRemoteUrl("D:\\share", "git@code.xiedeacc.com:root/gittest.git");
    logger.info("result: " + ret);
  }

  @Test
  public void testChangeRemoteUrl() {
    Boolean ret = Util.changeRemoteUrl("D:\\share", "git@github.com:xiedeacc/share.git");
    // Boolean ret = Util.changeRemoteUrl("D:\\share", "git@code.xiedeacc.com:root/gittest.git");
    logger.info("result: " + ret);
  }
}
