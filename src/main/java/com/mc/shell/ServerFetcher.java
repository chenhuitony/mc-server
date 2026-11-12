package com.mc.shell;

import com.mc.util.IoUtil;
import com.mc.util.Json;
import com.mc.util.Log;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 官方服务端下载器。
 *
 * <p>从 Mojang 版本清单解析并下载对应版本的 server.jar，
 * 让本内核可以作为「原版服务端的外壳」运行真实服务端。
 */
public class ServerFetcher {

    private static final String VERSION_MANIFEST =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    /** 版本描述。 */
    public static class VersionInfo {
        public String id;
        public String type;
        public String url;
        public String serverUrl;
        public long serverSize;
        public String serverSha1;
    }

    /** 解析指定版本的元数据（null 表示最新正式版）。 */
    @SuppressWarnings("unchecked")
    public VersionInfo resolve(String wantedVersion) throws Exception {
        Log.info("shell.fetch_manifest");
        String manifestText = IoUtil.readUrl(VERSION_MANIFEST);
        Map<String, Object> manifest = Json.parseObject(manifestText);

        String versionId = wantedVersion;
        String versionUrl = null;
        String type = "release";

        Object latest = manifest.get("latest");
        if (versionId == null && latest instanceof Map) {
            versionId = (String) ((Map<String, Object>) latest).get("release");
        }

        Object versions = manifest.get("versions");
        if (versions instanceof List) {
            for (Object o : (List<Object>) versions) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> v = (Map<String, Object>) o;
                if (versionId != null && versionId.equals(v.get("id"))) {
                    versionUrl = (String) v.get("url");
                    type = String.valueOf(v.get("type"));
                    break;
                }
            }
        }
        if (versionUrl == null) {
            throw new IllegalStateException("找不到版本 " + versionId);
        }

        Log.info("shell.resolve_version", versionId);
        Map<String, Object> versionJson = Json.parseObject(IoUtil.readUrl(versionUrl));
        Object downloads = versionJson.get("downloads");
        if (!(downloads instanceof Map)) {
            throw new IllegalStateException("版本 " + versionId + " 没有 downloads 字段");
        }
        Object server = ((Map<String, Object>) downloads).get("server");
        if (!(server instanceof Map)) {
            throw new IllegalStateException("版本 " + versionId + " 没有服务端下载项");
        }
        Map<String, Object> s = (Map<String, Object>) server;

        VersionInfo info = new VersionInfo();
        info.id = versionId;
        info.type = type;
        info.url = versionUrl;
        info.serverUrl = (String) s.get("url");
        Object size = s.get("size");
        info.serverSize = size instanceof Number ? ((Number) size).longValue() : -1;
        info.serverSha1 = (String) s.get("sha1");
        return info;
    }

    /** 下载 server.jar 到目标文件。 */
    public void download(VersionInfo info, File dest) throws Exception {
        if (dest.exists() && info.serverSha1 != null) {
            String actual = IoUtil.sha1(dest);
            if (info.serverSha1.equalsIgnoreCase(actual)) {
                Log.info("shell.already_cached", dest.getName(), IoUtil.humanSize(dest.length()));
                return;
            }
        }
        Log.info("shell.downloading", info.id, IoUtil.humanSize(info.serverSize));
        IoUtil.download(new URL(info.serverUrl), dest, 0,
                (pct, got, total) -> Log.progress(pct, got, total));
        Log.progressDone();

        if (info.serverSha1 != null) {
            String actual = IoUtil.sha1(dest);
            if (!info.serverSha1.equalsIgnoreCase(actual)) {
                throw new IllegalStateException("下载校验失败（SHA-1 不匹配）");
            }
        }
        Log.ok("shell.downloaded", dest.getName(), IoUtil.humanSize(dest.length()));
    }
}