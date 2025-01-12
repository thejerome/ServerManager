package com.spring.boot.server.service;

import com.spring.boot.server.model.ServerInfo;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentSkipListSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessService {

    @Autowired
    private final ServerService serverService;

    public Process starter(ServerInfo serverInfo)
            throws IOException {

        Process server = new ProcessBuilder(
                "java", "-cp", "\"",
                serverInfo.getLibDir(), ";", serverInfo.getRscDir(), "\"",
                "-jar",
                serverInfo.getJarDir()).start();

        serverInfo.setProcess(server);
        serverInfo.setPid(server.pid());
        new Thread(() -> {
            try {
                InputStream in = server.getErrorStream();
                byte[] buffer = new byte[1024];
                File file = new File(
                        "logs/" + serverInfo.getName() + "/" + serverInfo.getName() + "Error_"
                                + ZonedDateTime
                                .now().toEpochSecond() + ".txt");
                writeLogFile(serverInfo, in, buffer, file);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                InputStream in = server.getInputStream();
                byte[] buffer = new byte[1024];
                File file = new File(
                        "logs/" + serverInfo.getName() + "/" + serverInfo.getName() + "_"
                                + ZonedDateTime.now().toEpochSecond()
                                + ".txt");
                writeLogFile(serverInfo, in, buffer, file);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        return server;
    }

    private void writeLogFile(ServerInfo serverInfo, InputStream in, byte[] buffer, File file)
            throws IOException {
        int ch;
        serverInfo.getLogFiles().put(file,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        while ((ch = in.read(buffer)) >= 0) {
            out.write(buffer, 0, ch);
        }
        out.close();
        in.close();
    }

    public void kill(Long pid) {
        ConcurrentSkipListSet<ServerInfo> servers = serverService.getServers();
        for (ServerInfo serverInfo : servers) {
            if (serverInfo.getPid().equals(pid)) {
                serverInfo.getProcess().destroy();
                servers.remove(serverInfo);
                serverInfo.setPid(null);
                servers.add(serverInfo);
            }
        }
        System.out.println("Program completed");
    }


}
