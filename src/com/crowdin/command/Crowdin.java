package com.crowdin.command;

import com.crowdin.Credentials;
import com.crowdin.Crwdn;
import com.crowdin.client.CrowdinApiClient;
import com.crowdin.exceptions.EmptyParameterException;
import com.crowdin.parameters.CrowdinApiParametersBuilder;
import com.crowdin.utils.Utils;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.sun.jersey.api.client.ClientResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by ihor on 1/24/17.
 */
public class Crowdin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Crowdin.class);

    public static final String CROWDIN_BASE_URL = "base-url";

    public static final String CROWDIN_PROJECT_IDENTIFIER = "project-identifier";

    public static final String CROWDIN_PROJECT_KEY = "project-key";

    private String baseUrl;

    private String projectIdentifier;

    private String projectKey;

    public Crowdin() {
        this.baseUrl = "https://api.crowdin.com/api/";
        this.projectIdentifier = Utils.getPropertyValue(CROWDIN_PROJECT_IDENTIFIER);
        this.projectKey = Utils.getPropertyValue(CROWDIN_PROJECT_KEY);
    }

    public ClientResponse uploadFile(VirtualFile source, String branch) {
        if (source == null) {
            return null;
        }

        ClientResponse clientResponse = null;
        Credentials credentials = new Credentials(baseUrl, projectIdentifier, projectKey, null);
        CrowdinApiParametersBuilder crowdinApiParametersBuilder = new CrowdinApiParametersBuilder();
        CrowdinApiClient crowdinApiClient = new Crwdn();
        crowdinApiParametersBuilder.json()
                .files(source.getCanonicalPath())
                .exportPatterns("strings.xml", "/values-%two_letters_code%/%original_file_name%");
        String createdBranch = this.createBranch(branch);
        if (createdBranch != null) {
            crowdinApiParametersBuilder.branch(createdBranch);
        }
        try {
            clientResponse = crowdinApiClient.addFile(credentials, crowdinApiParametersBuilder);
            if (clientResponse != null && clientResponse.getStatus() == 200) {
                Utils.showInformationMessage("File '" + source.getName() + "' added to Crowdin");
            }
            //LOGGER.info("Crowdin: add file '" + source.getName() + "': " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            System.out.println("Crowdin: add file '" + source.getName() + "': " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            if (clientResponse.getStatus() == 400) {
                clientResponse = crowdinApiClient.updateFile(credentials, crowdinApiParametersBuilder);
                if (clientResponse != null && clientResponse.getStatus() == 200) {
                    Utils.showInformationMessage("File '" + source.getName() + "' updated in Crowdin");
                }
                //LOGGER.info("Crowdin: update file '" + source.getName() + "': " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
                System.out.println("Crowdin: update file '" + source.getName() + "': " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clientResponse;
    }

    public ClientResponse exportTranslations(String branch) {
        ClientResponse clientResponse = null;
        Credentials credentials = new Credentials(baseUrl, projectIdentifier, projectKey, null);
        CrowdinApiParametersBuilder crowdinApiParametersBuilder = new CrowdinApiParametersBuilder();
        CrowdinApiClient crowdinApiClient = new Crwdn();
        crowdinApiParametersBuilder.json();
        if (branch != null && !"master".equals(branch) && !branch.isEmpty()) {
            crowdinApiParametersBuilder.branch(branch);
        }
        try {
            clientResponse = crowdinApiClient.exportTranslations(credentials, crowdinApiParametersBuilder);
            //LOGGER.info("Crowdin: export translations " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            System.out.println("Crowdin: export translations " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            System.out.println(clientResponse.getEntity(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clientResponse;
    }

    public File downloadTranslations(VirtualFile sourceFile, String branch) {
        ClientResponse clientResponse;
        Credentials credentials = new Credentials(baseUrl, projectIdentifier, projectKey, null);
        CrowdinApiParametersBuilder crowdinApiParametersBuilder = new CrowdinApiParametersBuilder();
        CrowdinApiClient crowdinApiClient = new Crwdn();
        crowdinApiParametersBuilder.json()
                .downloadPackage("all")
                .destinationFolder(sourceFile.getParent().getParent().getCanonicalPath() + "/");
        if (branch != null && !branch.isEmpty()) {
            crowdinApiParametersBuilder.branch(branch);
        }
        try {
            clientResponse = crowdinApiClient.downloadTranslations(credentials, crowdinApiParametersBuilder);
            //LOGGER.info("Crowdin: export translations " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            System.out.println("Crowdin: download translations " + clientResponse.getStatus() + " " + clientResponse.getStatusInfo());
            System.out.println(clientResponse.getEntity(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File(sourceFile.getParent().getParent().getCanonicalPath() + "/all.zip");
    }

    private String createBranch(String branch) {
        if (branch == null || branch.isEmpty()) {
            return null;
        }
        String response = null;
        ClientResponse clientResponse;
        Credentials credentials = new Credentials(baseUrl, projectIdentifier, projectKey, null);
        CrowdinApiParametersBuilder crowdinApiParametersBuilder = new CrowdinApiParametersBuilder();
        CrowdinApiClient crowdinApiClient = new Crwdn();

        if (branch != null && !branch.isEmpty()) {
            crowdinApiParametersBuilder.json()
                    .name(branch)
                    .isBranch(true);
            try {
                clientResponse = crowdinApiClient.addDirectory(credentials, crowdinApiParametersBuilder);
                if (clientResponse != null) {
                    String clientResponseEntity = clientResponse.getEntity(String.class);
                    JSONObject jsonObject = new JSONObject(clientResponseEntity);
                    if (jsonObject != null && !jsonObject.getBoolean("success")) {
                        JSONObject error = jsonObject.getJSONObject("error");
                        if (error != null) {
                            if (error.getInt("code") == 50) {
                                System.out.println("Branch '" + branch + "' with such name already exists");
                                response = branch;
                            } else {
                                System.out.println("Branch '" + branch + "' not created");
                                System.out.println("code: " + error.getInt("code"));
                                System.out.println("message: " + error.getString("message"));
                            }
                        }
                    } else {
                        System.out.println("Branch '" + branch + "' created");
                        response = branch;
                    }
                }
            } catch (EmptyParameterException e) {
                e.printStackTrace();
            }
        }
        return response;
    }
}
