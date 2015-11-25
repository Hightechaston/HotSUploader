// Copyright 2015 Eivind Vegsundvåg
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ninja.eivind.hotsreplayuploader.utils;

import ninja.eivind.hotsreplayuploader.services.platform.PlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class, which holds several application properties like paths
 * and provides several convencience methods like OS detection or
 * data mining game related information .
 */
@Singleton
public class StormHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StormHandler.class);
    private static final String SEPARATOR = System.getProperty("file.separator");
    private final String ACCOUNT_FOLDER_FILTER = "(\\d+[^A-Za-z,.\\-()\\s])";
    private final String hotsAccountFilter = "(\\d-Hero-\\d-\\d{1,20})";

    private File applicationHome;
    private File hotsHome;
    @Inject
    private PlatformService platformService;

    public StormHandler() {
    }

    public File getApplicationHome() {
        if (applicationHome == null) {
            applicationHome = buildApplicationHome();

            // Migration logic; should be removed somewhere around version 2.X(not 2.0) or 3.0
            File parentFile = applicationHome.getParentFile();
            File oldDirectory = new File(parentFile, "HotSLogs UploaderFX");
            if (oldDirectory.exists()) {
                if (!oldDirectory.renameTo(applicationHome)) {
                    if (!oldDirectory.delete()) {
                        LOG.warn("Could not delete old replay properties folder");
                    }
                }
            }
        }
        return applicationHome;
    }

    private File buildApplicationHome() {
        return platformService.getApplicationHome();
    }

    public File getPropertiesFile(final File replayFile) {
        final String propertiesFileName = replayFile.toString().replaceAll(".StormReplay", "") + ".json";
        final String replace = propertiesFileName.replace(getHotSHome().toString(), getApplicationHome() + SEPARATOR + "Accounts");
        return new File(replace);
    }

    public File getReplayFile(final File propertiesFile) {
        final String replayFileName = propertiesFile.toString().replaceAll(".json", "") + ".StormReplay";
        final String replace = replayFileName.replace(getApplicationHome() + SEPARATOR + "Accounts", getHotSHome().toString());
        return new File(replace);
    }

    public File getHotSHome() {
        if (hotsHome == null) {
            hotsHome = buildHotSHome();
        }
        return hotsHome;
    }

    private File buildHotSHome() {
        return platformService.getHotSHome();
    }

    public List<File> getHotSAccountDirectories() {
        return getAccountDirectories(getHotSHome());
    }

    public List<File> getApplicationAccountDirectories() {
        return getAccountDirectories(new File(getApplicationHome(), "Accounts"));
    }

    private List<File> getAccountDirectories(final File root) {
        List<File> hotsAccounts = new ArrayList<>();
        File[] files = root.listFiles((dir, name) -> name.matches(ACCOUNT_FOLDER_FILTER));
        if (files == null) {
            files = new File[0];
        }
        for (final File file : files) {
            File[] hotsFolders = file.listFiles((dir, name) -> name.matches(hotsAccountFilter));
            Arrays.stream(hotsFolders)
                    .map(folder -> new File(folder, "Replays"))
                    .map(folder -> new File(folder, "Multiplayer"))
                    .forEach(hotsAccounts::add);
        }
        return hotsAccounts;
    }

    public List<String> getAccountStringUris() {
        return getAccountDirectories(new File(getApplicationHome(), "Accounts")).stream()
                .map(File::getParentFile)
                .map(File::getParentFile)
                .map(folder -> {
                    String accountName = folder.getName();
                    String[] split = accountName.replace("-Hero", "").split("-");
                    StringBuilder accountNameBuilder = new StringBuilder();
                    for (final String s : split) {
                        accountNameBuilder.append("/").append(s);
                    }
                    return "https://www.hotslogs.com/API/Players" + accountNameBuilder.toString();
                }).collect(Collectors.toList());
    }

    public String getHotSAccountFilter() {
        return hotsAccountFilter;
    }
}
