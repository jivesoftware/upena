package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion.ProjectsPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Project;
import com.jivesoftware.os.upena.shared.ProjectFilter;
import com.jivesoftware.os.upena.shared.ProjectKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;

/**
 *
 */
// soy.page.projectsPluginRegion
public class ProjectsPluginRegion implements PageRegion<ProjectsPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public ProjectsPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/projects";
    }

    public static class ProjectsPluginRegionInput implements PluginInput {

        final String key;
        final String name;
        final String description;
        final String localPath;
        final String scmUrl;
        final String branch;

        final String pom;
        final String goals;

        final String mvnHome;
        final String localPathToRepo;
        final String action;

        public ProjectsPluginRegionInput(String key,
            String name,
            String description,
            String localPath,
            String scmUrl,
            String branch,
            String pom,
            String goals,
            String mvnHome,
            String localPathToRepo,
            String action) {

            this.key = key;
            this.name = name;
            this.description = description;
            this.localPath = localPath;
            this.scmUrl = scmUrl;
            this.branch = branch;
            this.pom = pom;
            this.goals = goals;
            this.mvnHome = mvnHome;
            this.localPathToRepo = localPathToRepo;

            this.action = action;
        }

        @Override
        public String name() {
            return "Projects";
        }

    }

    @Override
    public String render(String user, ProjectsPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {

            Map<String, String> filters = new HashMap<>();
            filters.put("name", input.name);
            filters.put("description", input.description);

            data.put("filters", filters);

            ProjectFilter filter = new ProjectFilter(null, null, 0, 10000);
            if (input.action != null) {
                if (input.action.equals("filter")) {
                    filter = new ProjectFilter(
                        input.name.isEmpty() ? null : input.name,
                        input.description.isEmpty() ? null : input.description,
                        0, 10000);
                    data.put("message", "Filtering: name.contains '" + input.name + "' description.contains '" + input.description + "'");
                } else if (input.action.equals("build")) {
                    filters.clear();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);

                    try {
                        Project project = upenaStore.projects.get(new ProjectKey(input.key));
                        if (project == null) {
                            data.put("message", "Couldn't checkout no existent project. Someone else likely just removed it since your last refresh.");
                        } else {

                            File localPath = new File(new File(project.localPath), project.name);
                            ps.println("Wiping " + localPath);
                            FileUtils.deleteQuietly(localPath);
                            ps.println("Cloning from " + project.scmUrl + " to " + localPath);

                            // then clone
                            try (Git git = Git.cloneRepository()
                                .setURI(project.scmUrl)
                                .setDirectory(localPath)
                                .setProgressMonitor(new TextProgressMonitor(new PrintWriter(ps)))
                                .call()) {

                                Ref ref = git.checkout().
                                    setCreateBranch(true).
                                    setName(project.branch).
                                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                                    setStartPoint("origin/" + project.branch).
                                    call();

                                try {

                                    List<String> goals = Lists.newArrayList(Splitter.on(' ').split(input.goals));

                                    InvocationRequest request = new DefaultInvocationRequest();
                                    request.setPomFile(new File(localPath, input.pom));
                                    request.setGoals(goals);
                                    request.setOutputHandler((line) -> {
                                        ps.println(line);
                                    });
                                    request.setMavenOpts("-Xmx3000m");

                                    Invoker invoker = new DefaultInvoker();
                                    invoker.setLocalRepositoryDirectory(new File("/jive/tmp/"));
                                    invoker.setMavenHome(new File(input.mvnHome));
                                    InvocationResult result = invoker.execute(request);

                                    if (result.getExitCode() == 0) {

                                        request = new DefaultInvocationRequest();
                                        request.setPomFile(new File(localPath, input.pom));
                                        request.setGoals(Arrays.asList("deploy"));
                                        request.setOutputHandler((line) -> {
                                            ps.println(line);
                                        });
                                        request.setMavenOpts("-Xmx3000m");
                                        Properties properties = new Properties();

                                        properties.setProperty("altDeploymentRepository", "mine::default::file:///tmp/myrepo");
                                        properties.setProperty("altReleaseRepository", "mine::default::file:///tmp/myrepo");
                                        properties.setProperty("altSnapshotRepository", "mine::default::file:///tmp/myrepo");

                                        properties.setProperty("skipTests", "true");
                                        properties.setProperty("deployAtEnd", "true");
                                        request.setProperties(properties);
                                        request.addShellEnvironment(user, user);

                                        invoker = new DefaultInvoker();
                                        invoker.setLocalRepositoryDirectory(new File("/jive/tmp/"));
                                        invoker.setMavenHome(new File("/usr/local/Cellar/maven/3.3.1/libexec"));
                                        result = invoker.execute(request);

                                        ps.println(result.getExitCode());
                                    }
                                } catch (Exception x) {
                                    String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                    ps.println("error while calling mvn " + trace);
                                }

                            }

                            data.put("message", baos.toString("UTF-8"));

                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        ps.println("Error while trying to add Project:" + input.name + "\n" + trace);
                        data.put("message", baos.toString("UTF-8"));
                    }

                } else if (input.action.equals("add")) {
                    filters.clear();
                    try {
                        Project newProject = new Project(input.name,
                            input.description,
                            input.localPath,
                            input.scmUrl,
                            input.branch,
                            input.pom,
                            input.goals,
                            input.mvnHome,
                            input.localPathToRepo,
                            new HashSet<>(),
                            new HashSet<>(),
                            new ArrayList<>());
                        upenaStore.projects.update(null, newProject);
                        upenaStore.record(user, "added", System.currentTimeMillis(), "", "projects-ui", newProject.toString());

                        data.put("message", "Created Project:" + input.name);
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Project:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("update")) {
                    filters.clear();
                    try {
                        Project project = upenaStore.projects.get(new ProjectKey(input.key));
                        if (project == null) {
                            data.put("message", "Couldn't update no existent project. Someone else likely just removed it since your last refresh.");
                        } else {
                            Project updatedProject = new Project(input.name,
                                input.description,
                                input.localPath,
                                input.scmUrl,
                                input.branch,
                                input.pom,
                                input.goals,
                                input.mvnHome,
                                input.localPathToRepo,
                                new HashSet<>(),
                                new HashSet<>(),
                                new ArrayList<>());
                            upenaStore.projects.update(new ProjectKey(input.key), updatedProject);
                            data.put("message", "Updated Project:" + input.name);
                            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "projects-ui", project.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Project:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("remove")) {
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remove Project:" + input.name);
                    } else {
                        try {
                            ProjectKey projectKey = new ProjectKey(input.key);
                            Project removing = upenaStore.projects.get(projectKey);
                            if (removing != null) {
                                upenaStore.projects.remove(projectKey);
                                upenaStore.record(user, "removed", System.currentTimeMillis(), "", "projects-ui", removing.toString());
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remove Project:" + input.name + "\n" + trace);
                        }
                    }
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<ProjectKey, TimestampedValue<Project>> found = upenaStore.projects.find(filter);
            for (Map.Entry<ProjectKey, TimestampedValue<Project>> entrySet : found.entrySet()) {
                ProjectKey key = entrySet.getKey();
                TimestampedValue<Project> timestampedValue = entrySet.getValue();
                Project value = timestampedValue.getValue();

                Map<String, Object> row = new HashMap<>();
                row.put("key", key.getKey());
                row.put("name", value.name);
                row.put("description", value.description);
                row.put("localPath", value.localPath);
                row.put("scmUrl", value.scmUrl);
                row.put("branch", value.branch);
                row.put("pom", value.pom);
                row.put("goals", value.goals);
                row.put("mvnHome", value.mvnHome);
                row.put("localPathToRepo", value.localPathToRepo);
                rows.add(row);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                String projectName1 = (String) o1.get("name");
                String projectName2 = (String) o2.get("name");

                int c = projectName1.compareTo(projectName2);
                if (c != 0) {
                    return c;
                }
                return c;
            });

            data.put("projects", rows);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    public void add(String user, ProjectUpdate releaseGroupUpdate) throws Exception {
        ProjectKey projectKey = new ProjectKey(releaseGroupUpdate.projectId);
        Project project = upenaStore.projects.get(projectKey);
        if (project != null) {
            //project.defaultReleaseGroups.put(new ServiceKey(releaseGroupUpdate.serviceId), new ReleaseGroupKey(releaseGroupUpdate.releaseGroupId));
            //upenaStore.projects.update(projectKey, project);
            //upenaStore.record(user, "updated", System.currentTimeMillis(), "", "projects-ui", project.toString());
        }
    }

    public void remove(String user, ProjectUpdate releaseGroupUpdate) throws Exception {
        ProjectKey projectKey = new ProjectKey(releaseGroupUpdate.projectId);
        Project project = upenaStore.projects.get(projectKey);
        if (project != null) {
            //if (project.defaultReleaseGroups.remove(new ServiceKey(releaseGroupUpdate.serviceId)) != null) {
            //    upenaStore.projects.update(projectKey, project);
            //    upenaStore.record(user, "updated", System.currentTimeMillis(), "", "projects-ui", project.toString());
            //}
        }
    }

    public static class ProjectUpdate {

        public String projectId;
        public String serviceId;
        public String releaseGroupId;

        public ProjectUpdate() {
        }

        public ProjectUpdate(String projectId, String serviceId, String releaseGroupId) {
            this.projectId = projectId;
            this.serviceId = serviceId;
            this.releaseGroupId = releaseGroupId;
        }

    }

    @Override
    public String getTitle() {
        return "Projects";
    }

}
