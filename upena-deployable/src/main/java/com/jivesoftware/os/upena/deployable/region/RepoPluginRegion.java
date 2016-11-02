package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.deployable.region.RepoPluginRegion.RepoPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.PathToRepo;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.SecurityUtils;

/**
 *
 */
// soy.page.repoPluginRegion
public class RepoPluginRegion implements PageRegion<RepoPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;
    private final PathToRepo localPathToRepo;

    public RepoPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore,
        PathToRepo localPathToRepo) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
        this.localPathToRepo = localPathToRepo;
    }

    @Override
    public String getRootPath() {
        return "/ui/repo";
    }

    public static class RepoPluginRegionInput implements PluginInput {

        final String groupIdFilter;
        final String artifactIdFilter;
        final String versionFilter;
        String fileNameFilter;
        final String action;

        public RepoPluginRegionInput(String groupIdFilter, String artifactIdFilter, String versionFilter, String fileNameFilter, String action) {
            this.groupIdFilter = groupIdFilter;
            this.artifactIdFilter = artifactIdFilter;
            this.versionFilter = versionFilter;
            this.fileNameFilter = fileNameFilter;
            this.action = action;
        }

        @Override
        public String name() {
            return "Repo";
        }
    }

    @Override
    public String render(String user, RepoPluginRegionInput input) {
        SecurityUtils.getSubject().checkPermission("write");
        Map<String, Object> data = Maps.newHashMap();

        try {

            File repoFile = localPathToRepo.get();
            List<Map<String, Object>> repo = new ArrayList<>();

            if (input.action != null) {
                if (input.action.equals("remove") && input.fileNameFilter != null && input.fileNameFilter.length() > 0) {
                    SecurityUtils.getSubject().checkPermission("write");

                    File f = new File(repoFile, input.fileNameFilter);
                    if (f.exists()) {
                        FileUtils.forceDelete(f);
                        data.put("message", "Deleted " + f.getAbsolutePath());
                    } else {
                        data.put("message", "Doesn''t exists " + f.getAbsolutePath());
                    }
                    input.fileNameFilter = !Strings.isNotBlank(input.groupIdFilter)
                        || !Strings.isNotBlank(input.artifactIdFilter)
                        || !Strings.isNotBlank(input.versionFilter)
                        || !Strings.isNotBlank(input.fileNameFilter) ? "filter" : "";

                }
                if (input.action.equals("filter")) {
                    MutableLong fileCount = new MutableLong();
                    if (repoFile.exists() && repoFile.isDirectory()) {
                        buildTree(input, repoFile, repoFile, repo, fileCount);
                    }
                }
            }

            Map<String, String> filter = new HashMap<>();
            filter.put("groupIdFilter", input.groupIdFilter);
            filter.put("artifactIdFilter", input.artifactIdFilter);
            filter.put("versionFilter", input.versionFilter);
            filter.put("fileNameFilter", input.fileNameFilter);
            data.put("filters", filter);

            if (!repo.isEmpty()) {
                data.put("repo", repo);
            }

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Repo";
    }

    public static void buildTree(RepoPluginRegionInput input, File root, File folder, List<Map<String, Object>> output, MutableLong fileCount) {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("folder is not a Directory");
        }
        int indent = 0;
        printDirectoryTree(input, root, folder, indent, output, fileCount);
    }

    private static void printDirectoryTree(RepoPluginRegionInput input,
        File root,
        File folder,
        int indent,
        List<Map<String, Object>> output,
        MutableLong fileCount) {

        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("folder is not a Directory");
        }
        output.add(fileToMap(root, folder, indent));
        long currentFileCount = fileCount.longValue();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                printDirectoryTree(input, root, file, indent + 1, output, fileCount);
            } else {
                printFile(input, root, file, indent + 1, output, fileCount);
            }
        }
        if (currentFileCount == fileCount.longValue()) {
            output.remove(output.size() - 1);
        }
    }

    private static void printFile(RepoPluginRegionInput input, File root, File file, int indent, List<Map<String, Object>> output, MutableLong fileCount) {
        String relativePath = getRelativePath(root, file);
        if (input.groupIdFilter != null && input.groupIdFilter.length() > 0) {
            if (!relativePath.contains(input.groupIdFilter.replace(".", "/"))) {
                return;
            }
        }
        if (input.artifactIdFilter != null && input.artifactIdFilter.length() > 0) {
            if (!relativePath.contains(input.artifactIdFilter.replace(".", "/"))) {
                return;
            }
        }
        if (input.versionFilter != null && input.versionFilter.length() > 0) {
            if (!relativePath.contains(input.versionFilter)) {
                return;
            }
        }
        if (input.fileNameFilter != null && input.fileNameFilter.length() > 0) {
            if (!file.getName().contains(input.fileNameFilter)) {
                return;
            }
        }
        fileCount.add(1);
        output.add(fileToMap(root, file, indent));
    }

    private static String getIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("|  ");
        }
        return sb.toString();
    }

    private static Map<String, Object> fileToMap(File root, File file, int indent) {
        boolean isDir = file.isDirectory();
        Map<String, Object> map = new HashMap<>();
        map.put("name", getIndentString(indent) + "+--" + file.getName() + ((isDir) ? "/" : ""));
        map.put("path", getRelativePath(root, file));
        map.put("lastModified", isDir ? "" : UpenaHealth.humanReadableUptime(System.currentTimeMillis() - file.lastModified()));
        return map;
    }

    public static String getRelativePath(File root, File file) {
        String home = root.getAbsolutePath();
        String path = file.getAbsolutePath();
        if (!path.startsWith(home)) {
            return null;
        }
        if (path.length() > home.length()) {
            return path.substring(home.length() + 1);
        } else {
            return null;
        }
    }

}
