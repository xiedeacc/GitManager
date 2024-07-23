package Common;

public class GitlabSecret {
    public String API_TOKEN;
    public String API_URL_BASE;
    public String GITLAB_URL_BASE;
    public String CODE_PATH_BASE;

    public String getAPI_TOKEN() {
        return API_TOKEN;
    }

    public void setAPI_TOKEN(String API_TOKEN) {
        this.API_TOKEN = API_TOKEN;
    }

    public String getAPI_URL_BASE() {
        return API_URL_BASE;
    }

    public void setAPI_URL_BASE(String API_URL_BASE) {
        this.API_URL_BASE = API_URL_BASE;
    }

    public String getGITLAB_URL_BASE() {
        return GITLAB_URL_BASE;
    }

    public void setGITLAB_URL_BASE(String GITLAB_URL_BASE) {
        this.GITLAB_URL_BASE = GITLAB_URL_BASE;
    }

    public String getCODE_PATH_BASE() {
        return CODE_PATH_BASE;
    }

    public void setCODE_PATH_BASE(String CODE_PATH_BASE) {
        this.CODE_PATH_BASE = CODE_PATH_BASE;
    }
}
