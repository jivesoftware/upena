package com.jivesoftware.os.upena.deployable.region;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.region.ProjectsPluginRegion.ProjectsPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Artifact;
import com.jivesoftware.os.upena.shared.PathToRepo;
import com.jivesoftware.os.upena.shared.Project;
import com.jivesoftware.os.upena.shared.ProjectFilter;
import com.jivesoftware.os.upena.shared.ProjectKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;

/**
 *
 */
// soy.page.projectsPluginRegion
public class ProjectsPluginRegion implements PageRegion<ProjectsPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final String outputTemplate;
    private final String tailTemplate;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final PathToRepo localPathToRepo;

    private final Map<ProjectKey, AtomicLong> runningProjects = new ConcurrentHashMap<>();
    private final ExecutorService projectExecutors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public ProjectsPluginRegion(String template,
        String outputTemplate,
        String tailTemplate,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        PathToRepo localPathToRepo
    ) {
        this.template = template;
        this.outputTemplate = outputTemplate;
        this.tailTemplate = tailTemplate;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.localPathToRepo = localPathToRepo;
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

        final String profiles;
        final String properties;
        final String mavenOpts;

        final String mvnHome;

        final String oldCoordinate;
        final String newCoordinate;
        final String action;

        final boolean refresh;

        public ProjectsPluginRegionInput(String key,
            String name,
            String description,
            String localPath,
            String scmUrl,
            String branch,
            String pom,
            String goals,
            String profiles,
            String properties,
            String mavenOpts,
            String mvnHome,
            String oldCoordinate,
            String newCoordinate,
            String action,
            boolean refresh) {

            this.key = key;
            this.name = name;
            this.description = description;
            this.localPath = localPath;
            this.scmUrl = scmUrl;
            this.branch = branch;
            this.pom = pom;
            this.goals = goals;
            this.profiles = profiles;
            this.properties = properties;
            this.mavenOpts = mavenOpts;
            this.mvnHome = mvnHome;
            this.oldCoordinate = oldCoordinate;
            this.newCoordinate = newCoordinate;
            this.action = action;
            this.refresh = refresh;
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
                ProjectKey projectKey = new ProjectKey(input.key);
                if (input.action.equals("filter")) {
                    filter = new ProjectFilter(
                        input.name.isEmpty() ? null : input.name,
                        input.description.isEmpty() ? null : input.description,
                        0, 10000);
                    data.put("message", "Filtering: name.contains '" + input.name + "' description.contains '" + input.description + "'");
                } else if (input.action.equals("cancel")) {
                    AtomicLong removed = runningProjects.remove(projectKey);
                    if (removed != null) {
                        data.put("message", "Build for project " + input.key + " was cacneled. ");
                    } else {
                        data.put("message", "No progect was found for " + input.key + ". Cancel request ignored.");
                    }
                } else if (input.action.equals("build")) {
                    filters.clear();
                    File repoFile = localPathToRepo.get();

                    try {
                        Project project = upenaStore.projects.get(projectKey);
                        if (project == null) {
                            data.put("message", "Couldn't checkout no existent project. Someone else likely just removed it since your last refresh.");
                        } else {
                            File root = new File(project.localPath);
                            FileUtils.forceMkdir(root);
                            File localPath = new File(root, project.name);
                            File runningOutput = new File(root, project.name + "-running.txt");
                            File failedOutput = new File(root, project.name + "-failed.txt");
                            File successOutput = new File(root, project.name + "-success.txt");
                            File depsList = new File(root, project.name + "-deps.txt");

                            runningOutput.delete();
                            failedOutput.delete();
                            successOutput.delete();
                            depsList.delete();
                            AtomicLong running = runningProjects.computeIfAbsent(projectKey, (k) -> new AtomicLong());
                            if (running.compareAndSet(0, System.currentTimeMillis())) {

                                FileOutputStream fos = new FileOutputStream(runningOutput);
                                PrintStream ps = new PrintStream(fos);

                                projectExecutors.submit(() -> {

                                    try {
                                        File finalOutput = successOutput;

                                        try {

                                            if (localPath.exists()) {
                                                Git gitProject = null;
                                                try {
                                                    ps.println("GIT open repo " + localPath);
                                                    gitProject = Git.open(localPath);
                                                    Status status = gitProject.status().call();
                                                    if (!status.isClean()) {
                                                        ps.println("Build canceled because repo has uncommited changes.");
                                                        printGitStatus(status, ps);
                                                        finalOutput = failedOutput;
                                                        return;
                                                    }

                                                    if (!project.branch.equals(gitProject.getRepository().getBranch())) {

                                                        ps.println("COMMAND: Checking out branch " + project.branch);
                                                        gitProject.checkout().
                                                            setCreateBranch(true).
                                                            setName(project.branch).
                                                            setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                                                            setStartPoint("origin/" + project.branch).
                                                            call();
                                                        ps.println("PASSED changed branch to " + project.branch);
                                                    }

                                                    PullResult pullResult = gitProject.pull()
                                                        .setProgressMonitor(new TextProgressMonitor(new PrintWriter(ps)))
                                                        .call();

                                                    if (!pullResult.isSuccessful() || !status.hasUncommittedChanges()) {
                                                        if (!pullResult.isSuccessful()) {
                                                            ps.println("GIT pull was not successful.");
                                                        }
                                                        if (!status.hasUncommittedChanges()) {
                                                            printGitStatus(status, ps);
                                                        }
                                                        finalOutput = failedOutput;
                                                        return;
                                                    }

                                                    ps.println("PASSED git repo ready to build.");

                                                } catch (Exception x) {
                                                    String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                                    ps.println(trace);
                                                    ps.println("FAILED " + x.getMessage());
                                                    LOG.warn("Git failure", x);
                                                    finalOutput = failedOutput;
                                                    return;
                                                } finally {
                                                    if (gitProject != null) {
                                                        gitProject.close();
                                                    }
                                                }

                                            } else {
                                                ps.println("GIT: Cloning from " + project.scmUrl + " to " + localPath);

                                                try (Git git = Git.cloneRepository()
                                                    .setURI(project.scmUrl)
                                                    .setDirectory(localPath)
                                                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(ps)))
                                                    .call()) {

                                                    ps.println("PASSED git repo " + project.scmUrl + " cloned");
                                                    if (!runningProjects.containsKey(projectKey)) {
                                                        ps.println("FAILED Build canceled.");
                                                        finalOutput = failedOutput;
                                                        return;
                                                    }

                                                    if (!project.branch.equals(git.getRepository().getBranch())) {

                                                        ps.println("COMMAND: Checking out branch " + project.branch);
                                                        git.checkout().
                                                            setCreateBranch(true).
                                                            setName(project.branch).
                                                            setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                                                            setStartPoint("origin/" + project.branch).
                                                            call();
                                                        ps.println("PASSED changed branch to " + project.branch);
                                                    }
                                                } catch (Exception x) {
                                                    String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                                    ps.println(trace);
                                                    ps.println("FAILED " + x.getMessage());
                                                    finalOutput = failedOutput;
                                                    return;
                                                }
                                            }

                                            try {
                                                FileUtils.forceMkdir(repoFile);
                                                Invoker invoker = new DefaultInvoker();
                                                ps.println("CONFIG Using maven repo:" + repoFile.getAbsolutePath());
                                                invoker.setLocalRepositoryDirectory(repoFile);
                                                ps.println("CONFIG Using maven home:" + project.mvnHome);
                                                invoker.setMavenHome(new File(project.mvnHome));

                                                if (!checkForUpgradeables(projectKey, project, invoker, ps)) {
                                                    finalOutput = failedOutput;
                                                    return;
                                                }

                                                ps.println("COMMAND: mvn " + project.goals);

                                                List<String> goals = Lists.newArrayList(Splitter.on(' ').split(project.goals));
                                                InvocationRequest request = new DefaultInvocationRequest();
                                                request.setPomFile(new File(localPath, project.pom));
                                                request.setGoals(goals);
                                                request.setOutputHandler((line) -> {
                                                    ps.println(line);
                                                });
                                                request.setMavenOpts("-Xmx3000m");

                                                InvocationResult result = invoker.execute(request);

                                                if (!runningProjects.containsKey(projectKey)) {
                                                    ps.println("ERROR: Build canceled.");
                                                    finalOutput = failedOutput;
                                                    return;
                                                }

                                                if (result.getExitCode() == 0) {
                                                    ps.println("PASSED mvn " + project.goals);

                                                    ps.println("Invoking deploying... ");

                                                    request = new DefaultInvocationRequest();
                                                    request.setPomFile(new File(localPath, project.pom));
                                                    request.setGoals(Arrays.asList("javadoc:jar", "source:jar", "deploy"));
                                                    request.setOutputHandler((line) -> {
                                                        ps.println(line);
                                                    });
                                                    request.setShowErrors(true);
                                                    request.setShowVersion(true);

                                                    request.setMavenOpts("-Xmx3000m");
                                                    request.setDebug(false);

                                                    Properties properties = new Properties();

                                                    String repoUrl = "http://localhost:1175/repo";

                                                    // TODO deploy to backup/HA url
                                                    properties.setProperty("altDeploymentRepository", "mine::default::" + repoUrl);
                                                    properties.setProperty("altReleaseRepository", "mine::default::" + repoUrl);
                                                    properties.setProperty("altSnapshotRepository", "mine::default::" + repoUrl);

                                                    properties.setProperty("skipTests", "true");
                                                    properties.setProperty("deployAtEnd", "true");
                                                    request.setProperties(properties);
                                                    request.addShellEnvironment(user, user);

                                                    invoker = new DefaultInvoker();
                                                    invoker.setLocalRepositoryDirectory(repoFile);
                                                    invoker.setMavenHome(new File(project.mvnHome));
                                                    result = invoker.execute(request);

                                                    if (!runningProjects.containsKey(projectKey)) {
                                                        ps.println("ERROR: Build canceled.");
                                                        finalOutput = failedOutput;
                                                        return;
                                                    }

                                                    if (result.getExitCode() == 0) {
                                                        ps.println();
                                                        ps.println("PASSED mvn javadoc:jar source:jar deploy");
                                                        ps.println();
                                                    } else {
                                                        ps.println();
                                                        ps.println("FAILED: Darn it!");
                                                        ps.println();
                                                        finalOutput = failedOutput;
                                                    }
                                                } else {
                                                    ps.println();
                                                    ps.println("FAILED: Darn it!");
                                                    ps.println();
                                                    finalOutput = failedOutput;
                                                }
                                            } catch (Exception x) {
                                                String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                                ps.println(trace);
                                                ps.println("FAILED while calling mvn " + trace);
                                                finalOutput = failedOutput;
                                            }

                                        } catch (Exception x) {
                                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                                            ps.println(trace);
                                            finalOutput = failedOutput;
                                        } finally {
                                            fos.flush();
                                            fos.close();
                                            FileUtils.moveFile(runningOutput, finalOutput);
                                        }
                                    } catch (Exception x) {
                                        try {
                                            FileUtils.moveFile(runningOutput, failedOutput);
                                        } catch (Exception xx) {
                                            LOG.error("Failed to move " + runningOutput + " " + failedOutput, x);
                                        }
                                        LOG.error("Unexpected failure while building" + project, x);
                                    } finally {
                                        runningProjects.remove(projectKey);
                                    }
                                });

                                return output(input.key, input.refresh);
                            } else {
                                data.put("message", "Project is alreadying running.");
                            }
                        }
                    } catch (Exception x) {
                        runningProjects.remove(projectKey);
                        LOG.error("JGit eror", x);
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to build Project:" + input.name + "\n" + trace);
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
                            new HashSet<>(),
                            new HashSet<>(),
                            new HashMap<>(),
                            new HashMap<>());
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
                        Project project = upenaStore.projects.get(projectKey);
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
                                project.downloadFromArtifactRepositories,
                                project.uploadToArtifactRepositories,
                                project.conflictingCoordinateProjectKeyArtifacts,
                                project.dependantReleaseGroups);
                            upenaStore.projects.update(projectKey, updatedProject);
                            data.put("message", "Updated Project:" + input.name);
                            upenaStore.record(user, "updated", System.currentTimeMillis(), "", "projects-ui", project.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Project:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("clone")) {
                    filters.clear();
                    try {
                        Project project = upenaStore.projects.get(projectKey);
                        if (project == null) {
                            data.put("message", "Couldn't clone no existent project. Someone else likely just removed it since your last refresh.");
                        } else {
                            Project clone = new Project("CLONE-" + input.name,
                                input.description,
                                input.localPath,
                                input.scmUrl,
                                input.branch,
                                input.pom,
                                input.goals,
                                input.mvnHome,
                                new HashSet<>(),
                                new HashSet<>(),
                                new HashMap<>(),
                                new HashMap<>());
                            upenaStore.projects.update(null, clone);
                            data.put("message", "Cloned Project:" + input.name);
                            upenaStore.record(user, "cloned", System.currentTimeMillis(), "", "projects-ui", project.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to add Project:" + input.name + "\n" + trace);
                    }
                } else if (input.action.equals("remove")) {
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to remove Project:" + input.name);
                    } else if (runningProjects.containsKey(new ProjectKey(input.key))) {
                        data.put("message", "Failed to remove Project:" + input.name + " because it is currently running.");
                    } else {
                        try {
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
                } else if (input.action.equals("setVersion")) {
                    if (input.key.isEmpty()) {
                        data.put("message", "Failed to set version Project:" + input.name);
                    } else {
                        filters.clear();
                        try {
                            Project project = upenaStore.projects.get(projectKey);
                            if (project == null) {
                                data.put("message", "Couldn't checkout no existent project. Someone else likely just removed it since your last refresh.");
                            } else {
                                File root = new File(project.localPath);
                                FileUtils.forceMkdir(root);
                                File runningOutput = new File(root, project.name + "-running.txt");
                                File failedOutput = new File(root, project.name + "-failed.txt");
                                File successOutput = new File(root, project.name + "-success.txt");

                                runningOutput.delete();
                                failedOutput.delete();
                                successOutput.delete();

                                AtomicLong running = runningProjects.computeIfAbsent(projectKey, (k) -> new AtomicLong());
                                if (running.compareAndSet(0, System.currentTimeMillis())) {

                                    FileOutputStream fos = new FileOutputStream(runningOutput);
                                    PrintStream ps = new PrintStream(fos);

                                    projectExecutors.submit(() -> {
                                        try {
                                            setVersion(new ProjectKey(input.key),
                                                project,
                                                input.oldCoordinate.trim().split(":"),
                                                input.newCoordinate.trim().split(":"),
                                                ps);
                                        } catch (Exception x) {
                                            LOG.error("Unecpected failure.", x);
                                        }
                                    });
                                }
                            }
                        } catch (Exception x) {
                            String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                            data.put("message", "Error while trying to remove Project:" + input.name + "\n" + trace);
                        }
                    }
                } else if (input.action.equals("gitResetHardHead")) {
                    Git gitProject = null;
                    try {
                        Project project = upenaStore.projects.get(projectKey);
                        if (project == null) {
                            data.put("message", "Couldn't checkout no existent project. Someone else likely just removed it since your last refresh.");
                        } else {
                            File root = new File(project.localPath);
                            File localPath = new File(root, project.name);

                            gitProject = Git.open(localPath);
                            ResetCommand resetCmd = gitProject.reset().setMode(ResetType.HARD);
                            Ref ref = resetCmd.call();
                            data.put("message", "Reset to " + ref.toString());
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to remove Project:" + input.name + "\n" + trace);
                    } finally {
                        if (gitProject != null) {
                            gitProject.close();
                        }
                    }
                } else if (input.action.equals("wipe")) {
                    try {
                        Project project = upenaStore.projects.get(projectKey);
                        if (project == null) {
                            data.put("message", "Couldn't wipe no existent project. Someone else likely just removed it since your last refresh.");
                        } else {
                            File root = new File(project.localPath);
                            File runningOutput = new File(root, project.name + "-running.txt");
                            File failedOutput = new File(root, project.name + "-failed.txt");
                            File successOutput = new File(root, project.name + "-success.txt");
                            File depsList = new File(root, project.name + "-deps.txt");
                            File localPath = new File(root, project.name);

                            FileUtils.forceDelete(localPath);
                            depsList.delete();
                            runningOutput.delete();
                            failedOutput.delete();
                            successOutput.delete();

                            data.put("message", "Wiped project:" + project.name);
                        }
                    } catch (Exception x) {
                        String trace = x.getMessage() + "\n" + Joiner.on("\n").join(x.getStackTrace());
                        data.put("message", "Error while trying to wipe Project:" + input.name + "\n" + trace);
                    }
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            Map<ProjectKey, TimestampedValue<Project>> found = upenaStore.projects.find(filter);
            for (Map.Entry<ProjectKey, TimestampedValue<Project>> entrySet : found.entrySet()) {
                ProjectKey key = entrySet.getKey();
                TimestampedValue<Project> timestampedValue = entrySet.getValue();
                Project project = timestampedValue.getValue();

                File root = new File(project.localPath);

                File runningOutput = new File(root, project.name + "-running.txt");
                File failedOutput = new File(root, project.name + "-failed.txt");
                File successOutput = new File(root, project.name + "-success.txt");
                File localPath = new File(root, project.name);

                Map<String, Object> row = new HashMap<>();

                Git gitProject = null;
                try {
                    gitProject = Git.open(localPath);

                    Status status = gitProject.status().call();
                    Map<String, Object> gitStatus = new HashMap<>();
                    if (status.hasUncommittedChanges()) {
                        gitStatus.put("hasUncommittedChanges", String.valueOf(status.hasUncommittedChanges()));
                    }
                    gitStatus.put("added", status.getAdded().isEmpty() ? null : status.getAdded());
                    gitStatus.put("changed", status.getChanged().isEmpty() ? null : status.getChanged());
                    gitStatus.put("conflicting", status.getConflicting().isEmpty() ? null : status.getConflicting());
                    gitStatus.put("missing", status.getMissing().isEmpty() ? null : status.getMissing());
                    gitStatus.put("modified", status.getModified().isEmpty() ? null : status.getModified());
                    gitStatus.put("removed", status.getRemoved().isEmpty() ? null : status.getRemoved());
                    gitStatus.put("uncommited", status.getUncommittedChanges().isEmpty() ? null : status.getUncommittedChanges());
                    gitStatus.put("untracked", status.getUntracked().isEmpty() ? null : status.getUntracked());
                    gitStatus.put("untrackedFolders", status.getUntrackedFolders().isEmpty() ? null : status.getUntrackedFolders());
                    gitStatus.put("isClean", String.valueOf(status.isClean()));
                    row.put("gitStatus", gitStatus);

                } catch (Exception x) {
                    LOG.warn("Issues checking git status", x);
                } finally {
                    if (gitProject != null) {
                        gitProject.close();
                    }
                }

                AtomicLong got = runningProjects.get(key);

                row.put("running", got != null);
                if (got != null) {
                    row.put("elapse", UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - got.get()));
                }

                if (failedOutput.exists()) {
                    row.put("status", "danger");
                    row.put("elapse", UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - failedOutput.lastModified()));
                } else if (successOutput.exists()) {
                    row.put("status", "success");
                    row.put("elapse", UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - successOutput.lastModified()));
                } else if (runningOutput.exists()) {
                    row.put("status", "info");
                } else {
                    row.put("status", "default");
                    row.put("elapse", "never");
                }

                row.put("key", key.getKey());
                row.put("name", project.name);
                row.put("description", project.description);
                row.put("localPath", project.localPath);
                row.put("scmUrl", project.scmUrl);
                row.put("branch", project.branch);
                row.put("pom", project.pom);
                row.put("goals", project.goals);
                row.put("mvnHome", project.mvnHome);

                if (project.dependantReleaseGroups != null) {
                    List<Map<String, Object>> releases = new ArrayList<>();
                    for (Entry<ReleaseGroupKey, Artifact> e : project.dependantReleaseGroups.entrySet()) {
                        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(e.getKey());
                        if (releaseGroup != null) {
                            Map<String, Object> release = new HashMap<>();
                            release.put("name", releaseGroup.name);
                            release.put("version", releaseGroup.version);
                            Artifact expected = e.getValue();
                            String[] coordinates = releaseGroup.version.split(",");
                            for (String coordinate : coordinates) {
                                Artifact current = new Artifact(coordinate.trim().split(":"));
                                if (!current.equals(expected)) {
                                    release.put("alternateVersion", expected.toString());
                                }
                            }
                            releases.add(release);
                        }
                    }
                    if (releases.size() > 0) {
                        row.put("hasReleases", String.valueOf(releases.size()));
                        row.put("releases", releases);
                    }
                }

                if (project.conflictingCoordinateProjectKeyArtifacts != null) {
                    List<Map<String, Object>> versionConflicts = new ArrayList<>();
                    for (Entry<String, Map<ProjectKey, Artifact>> e : project.conflictingCoordinateProjectKeyArtifacts.entrySet()) {

                        List<Map<String, Object>> versionUsages = new ArrayList<>();
                        for (Entry<ProjectKey, Artifact> f : e.getValue().entrySet()) {
                            if (!f.getKey().equals(key)) {
                                Project p = upenaStore.projects.get(f.getKey());
                                if (p != null) {
                                    Map<String, Object> v = new HashMap<>();
                                    v.put("key", f.getKey());
                                    v.put("name", p.name);
                                    v.put("version", f.getValue().toString());
                                    versionUsages.add(v);
                                }
                            }
                        }
                        if (!versionUsages.isEmpty()) {
                            Map<String, Object> m = new HashMap<>();
                            m.put("version", e.getKey());
                            m.put("versionUsages", versionUsages);
                            versionConflicts.add(m);
                        }
                    }
                    if (versionConflicts.size() > 0) {
                        row.put("hasVersionConflicts", String.valueOf(versionConflicts.size()));
                        row.put("versionConflicts", versionConflicts);
                    }
                }

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

    public void setVersion(ProjectKey projectKey,
        Project project,
        String[] oldCoordinate,
        String[] newCoordinate,
        PrintStream ps) throws Exception {

        File root = new File(project.localPath);
        FileUtils.forceMkdir(root);
        File localPath = new File(root, project.name);
        File runningOutput = new File(root, project.name + "-running.txt");
        File failedOutput = new File(root, project.name + "-failed.txt");
        File successOutput = new File(root, project.name + "-success.txt");
        File finalOutput = null;
        try {

            Invoker invoker = new DefaultInvoker();
            File repoFile = localPathToRepo.get();
            ps.println("CONFIG Using maven repo:" + repoFile.getAbsolutePath());
            invoker.setLocalRepositoryDirectory(repoFile);
            ps.println("CONFIG Using maven home:" + project.mvnHome);
            invoker.setMavenHome(new File(project.mvnHome));

            File pomFile = new File(localPath, project.pom);

            ps.println("COMMAND: versions:set " + pomFile.getAbsolutePath());
            //mvn dependency:list -DappendOutput=true -DoutputFile=/jive/tmp/deps.txt
            List<String> goals = Arrays.asList("org.codehaus.mojo:versions-maven-plugin:2.1:set"); // TODO expose to config
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(pomFile);
            request.setGoals(goals);
            request.setOutputHandler((line) -> {
                ps.println(line);
            });
            request.setMavenOpts("-Xmx3000m");

            Properties prprts = new Properties();
            prprts.put("groupId", newCoordinate[0]);
            prprts.put("artifactId", newCoordinate[1]);
            prprts.put("oldVersion", oldCoordinate[2]);
            prprts.put("newVersion", newCoordinate[3]);

            prprts.put("processPlugins", "false");
            prprts.put("generateBackupPoms", "false");
            request.setProperties(prprts);
            request.setDebug(true);

            InvocationResult result = invoker.execute(request);
            if (!runningProjects.containsKey(projectKey)) {
                ps.println("FAILED: Build canceled.");
                finalOutput = failedOutput;
                return;
            }
            if (result.getExitCode() == 0) {
                ps.println();
                ps.println("PASSED set version to " + Arrays.toString(newCoordinate));
                ps.println();
            } else {
                ps.println();
                ps.println("FAILED to set version to " + Arrays.toString(newCoordinate));
                ps.println();
                finalOutput = failedOutput;
                return;
            }

            finalOutput = successOutput;
        } catch (Exception x) {
            LOG.error("Unexpected failure while building" + project, x);
            finalOutput = failedOutput;
        } finally {
            if (ps != null) {
                ps.flush();
                ps.close();
            }
            try {
                FileUtils.moveFile(runningOutput, finalOutput);
            } catch (Exception xx) {
                LOG.error("Failed to move " + runningOutput + " " + failedOutput, xx);
            }
            runningProjects.remove(projectKey);
        }

    }

    private void printGitStatus(Status status, PrintStream ps) {
        for (String string : status.getAdded()) {
            ps.println("Added:" + string);
        }
        for (String string : status.getChanged()) {
            ps.println("Changes:" + string);
        }
        for (String string : status.getConflicting()) {
            ps.println("Conflicting:" + string);
        }
        for (String string : status.getMissing()) {
            ps.println("Missing:" + string);
        }
        for (String string : status.getModified()) {
            ps.println("Modified:" + string);
        }
        for (String string : status.getRemoved()) {
            ps.println("Removed:" + string);
        }
        for (String string : status.getUncommittedChanges()) {
            ps.println("Uncommited:" + string);
        }
        for (String string : status.getUntrackedFolders()) {
            ps.println("Untracked:" + string);
        }
    }

    private boolean checkForUpgradeables(ProjectKey projectKey, Project project, Invoker invoker, PrintStream ps) throws Exception {
        File root = new File(project.localPath);
        File localPath = new File(root, project.name);
        File deps = new File(root, project.name + "-deps.txt");
        File pomFile = new File(localPath, project.pom);

        ps.println("COMMAND:  dependency:tree " + pomFile.getAbsolutePath());
        //mvn dependency:list -DappendOutput=true -DoutputFile=/jive/tmp/deps.txt
        List<String> goals = Arrays.asList("dependency:tree");
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(goals);
        request.setOutputHandler((line) -> {
            ps.println(line);
        });
        request.setMavenOpts("-Xmx3000m");

        Properties prprts = new Properties();
        prprts.put("outputFile", deps.getAbsolutePath());
        prprts.put("tokens", "whitespace");
        prprts.put("outputType", "text");
        prprts.put("appendOutput", "true");
        request.setProperties(prprts);

        InvocationResult result = invoker.execute(request);
        if (!runningProjects.containsKey(projectKey)) {
            ps.println("ERROR: Build canceled.");
            return false;
        }
        if (result.getExitCode() == 0) {
            ps.println("PASSED Recorded dependency list " + deps.getAbsolutePath());

            ps.println("COMMAND compute upgradables for " + project.name);
            buildUpgradables(projectKey, project, ps);

        } else {
            ps.println();
            ps.println("FAILED trying to build dependency list");
            ps.println();
            return false;
        }
        return true;
    }

    private class ProjectModule {

        String groupId;
        String artifactId;

        Map<ProjectKey, ProjectAndVersion> versions = new ConcurrentHashMap<>();

        public ProjectModule(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
    }

    private static class ProjectAndVersion {

        public final Project project;
        public final Artifact artifact;

        public ProjectAndVersion(Project project, String[] coordinate) {
            this.project = project;
            this.artifact = new Artifact(coordinate);
        }

        @Override
        public String toString() {
            return project.name + "->" + artifact;
        }

    }

    private void buildUpgradables(ProjectKey projectKey, Project project, PrintStream ps) throws Exception {
        File root = new File(project.localPath);
        File deps = new File(root, project.name + "-deps.txt");

        Set<String> moduleCoordinates = new HashSet<>();

        Map<String, ProjectModule> projectModules = new ConcurrentHashMap<>();

        List<String> lines = FileUtils.readLines(deps);
        for (String line : lines) {
            String[] coordinate = line.trim().split(":");
            if (line.startsWith("      ")) { // ignore secondary deps
            } else if (line.startsWith("   ")) { // primary
                ProjectModule dependsOnModule = projectModules.computeIfAbsent(coordinate[0] + ":" + coordinate[1], (t) -> new ProjectModule(coordinate[0],
                    coordinate[1]));
                dependsOnModule.versions.put(projectKey, new ProjectAndVersion(project, coordinate));
            } else if (!coordinate[2].equals("pom")) {
                ProjectModule projectModule = projectModules.computeIfAbsent(coordinate[0] + ":" + coordinate[1], (t)
                    -> new ProjectModule(coordinate[0], coordinate[1]));
                projectModule.versions.put(projectKey, new ProjectAndVersion(project, coordinate));
                moduleCoordinates.add(line);
            }
        }

        // Compute dependant modules:
        upenaStore.projects.scan((ProjectKey pk, Project p) -> {

            File otherRoot = new File(p.localPath);
            File otherDeps = new File(otherRoot, p.name + "-deps.txt");
            if (otherDeps.exists() && otherDeps.isFile()) {
                List<String> otherLines = FileUtils.readLines(otherDeps);
                for (String line : otherLines) {
                    String[] coordinate = line.trim().split(":");
                    if (line.startsWith("      ")) { // ignore secondary deps
                    } else if (line.startsWith("   ")) { // primary
                        ProjectModule dependsOnModule = projectModules.computeIfAbsent(coordinate[0] + ":" + coordinate[1], (t) -> new ProjectModule(
                            coordinate[0],
                            coordinate[1]));
                        dependsOnModule.versions.put(pk, new ProjectAndVersion(p, coordinate));
                    } else if (!coordinate[2].equals("pom")) {
                        ProjectModule otherProjectModule = projectModules.computeIfAbsent(coordinate[0] + ":" + coordinate[1], (t)
                            -> new ProjectModule(coordinate[0], coordinate[1]));
                        otherProjectModule.versions.put(pk, new ProjectAndVersion(p, coordinate));
                    }
                }
            }
            return true;
        });

        project.dependantReleaseGroups = new HashMap<>();
        for (String moduleCoordinate : moduleCoordinates) {

            System.out.println("INFO Modules:" + moduleCoordinate);
            String[] coordinate = moduleCoordinate.trim().split(":");
            String find = coordinate[0] + ":" + coordinate[1] + ":";
            ReleaseGroupFilter filter = new ReleaseGroupFilter(null, null, find, null, null, 0, 1000);
            ConcurrentNavigableMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(filter);
            for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entry : found.entrySet()) {
                project.dependantReleaseGroups.put(entry.getKey(), new Artifact(coordinate));
                ps.println("INFO: found a release:" + entry.getValue().getValue().name + " tied to " + find);
            }
        }
        ps.println("PASSED version conflicts computed");

//        
        Map<ProjectKey, Map<String, Map<ProjectKey, Artifact>>> conflictingCoordinateProjectKeyArtifacts = new HashMap<>();

        for (ProjectModule value : projectModules.values()) {
            if (value.versions.size() > 1) {
                Set<String> warning = new HashSet<>();
                for (ProjectAndVersion projectAndVersion : value.versions.values()) {
                    warning.add(projectAndVersion.artifact.groupId + ":" + projectAndVersion.artifact.artifactId + ":" + projectAndVersion.artifact.version);
                }
                if (warning.size() > 1) {
                    for (Entry<ProjectKey, ProjectAndVersion> e : value.versions.entrySet()) {

                        Map<String, Map<ProjectKey, Artifact>> conflicts = conflictingCoordinateProjectKeyArtifacts.computeIfAbsent(e.getKey(),
                            (k) -> new HashMap<>());

                        String currentArtifact = value.groupId + ":" + value.artifactId + ":" + value.versions.get(e.getKey()).artifact.version;
                        for (Entry<ProjectKey, ProjectAndVersion> f : value.versions.entrySet()) {

                            Map<ProjectKey, Artifact> projectVersionConflicts = conflicts.computeIfAbsent(currentArtifact, (k) -> new HashMap<>());
                            projectVersionConflicts.put(f.getKey(), f.getValue().artifact);
                            ps.println("WARNING:" + f.getValue());
                        }
                    }
                }
            }
        }

        for (Entry<ProjectKey, Map<String, Map<ProjectKey, Artifact>>> e : conflictingCoordinateProjectKeyArtifacts.entrySet()) {
            if (e.getKey().equals(projectKey)) {
                project.conflictingCoordinateProjectKeyArtifacts = e.getValue();
            } else {
                Project got = upenaStore.projects.get(e.getKey());
                if (got != null) {
                    got.conflictingCoordinateProjectKeyArtifacts = e.getValue();
                    upenaStore.projects.update(e.getKey(), got);
                }
            }
        }

        upenaStore.projects.update(projectKey, project);
        ps.println("PASSED version conflicts recorded");

    }

    public String output(String key, boolean refresh) throws Exception {
        Map<String, Object> data = Maps.newHashMap();

        List<String> log = new ArrayList<>();

        Project project = upenaStore.projects.get(new ProjectKey(key));
        if (project != null) {
            File projectDir = new File(project.localPath);

            boolean done = false;
            File running = new File(projectDir, project.name + "-running.txt");
            if (running.exists()) {
                List<String> lines = FileUtils.readLines(running);
                if (refresh) {
                    log.addAll(lines.subList(Math.max(0, lines.size() - 10000), lines.size()));
                } else {
                    log.addAll(lines);
                }
            }

            File failed = new File(projectDir, project.name + "-failed.txt");
            if (failed.exists()) {
                List<String> lines = FileUtils.readLines(failed);
                if (refresh) {
                    log.addAll(lines.subList(Math.max(0, lines.size() - 10000), lines.size()));
                } else {
                    log.addAll(lines);
                }
                refresh = false;
                done = true;
            }

            File success = new File(projectDir, project.name + "-success.txt");
            if (success.exists()) {
                List<String> lines = FileUtils.readLines(success);
                if (refresh) {
                    log.addAll(lines.subList(Math.max(0, lines.size() - 10000), lines.size()));
                } else {
                    log.addAll(lines);
                }
                refresh = false;
                done = true;
            }

            data.put("key", key);
            data.put("name", project.name);
            data.put("refresh", refresh);
            data.put("offset", 0);
            data.put("done", done);

        } else {
            data.put("key", key);
            data.put("name", "No project found for " + key);
            data.put("refresh", refresh);
            data.put("offset", 0);
            data.put("done", true);
        }
        data.put("log", log);

        return renderer.render(outputTemplate, data);
    }

    public String tail(String key, int offset) throws Exception {
        Map<String, Object> data = Maps.newHashMap();

        List<String> log = new ArrayList<>();

        Project project = upenaStore.projects.get(new ProjectKey(key));
        int newOffset = offset;
        if (project != null) {
            File projectDir = new File(project.localPath);

            boolean done = false;
            List<String> lines = Collections.emptyList();
            File running = new File(projectDir, project.name + "-running.txt");
            if (running.exists()) {
                lines = FileUtils.readLines(running);
            }

            File failed = new File(projectDir, project.name + "-failed.txt");
            if (failed.exists()) {
                lines = FileUtils.readLines(failed);
                done = true;
            }

            File success = new File(projectDir, project.name + "-success.txt");
            if (success.exists()) {
                lines = FileUtils.readLines(success);
                done = true;
            }

            log.addAll(lines.subList(Math.min(offset, lines.size()), lines.size()));
            newOffset = lines.size();

            data.put("key", key);
            data.put("name", project.name);
            data.put("offset", newOffset);
            data.put("done", done);

        } else {
            data.put("key", key);
            data.put("name", "No project found for " + key);
            data.put("offset", newOffset);
            data.put("done", true);
        }
        data.put("log", log);

        return renderer.render(tailTemplate, data);
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
