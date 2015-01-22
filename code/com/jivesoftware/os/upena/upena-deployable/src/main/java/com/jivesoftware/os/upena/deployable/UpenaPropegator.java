/*
 * Copyright 2014 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author jonathan.colt
 */
public class UpenaPropegator {

//    public static void main(String[] args) throws Exception {
//        UpenaPropegator upenaPropegator = new UpenaPropegator("./target/upena.jar",
//            "dev", "~/.ssh/id_rsa", "jive", "host", "/usr/local/jive/upena");
//        upenaPropegator.propegate();
//        System.exit(0);
//    }

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String pathToUpenaJar;
    private final String clusterName;
    private final String pathToSSHKey;
    private final String scpUser;
    private final String scpHost;
    private final String scpHome;

    public UpenaPropegator(String pathToUpenaJar,
        String clusterName,
        String pathToSSHKey,
        String scpUser,
        String scpHost,
        String scpHome) {

        this.pathToUpenaJar = pathToUpenaJar;
        this.clusterName = clusterName;

        if (pathToSSHKey.startsWith("~" + File.separator)) {
            this.pathToSSHKey = System.getProperty("user.home") + pathToSSHKey.substring(1);
        } else {
            this.pathToSSHKey = pathToSSHKey;
        }
        this.scpUser = scpUser;
        this.scpHost = scpHost;
        this.scpHome = scpHome;
    }

    public void propegate() throws Exception {
        JSch jsch = new JSch();

        //File local = new File(basedir, "target" + File.separator + mavenProject.getArtifactId() + "-" + mavenProject.getVersion() + ".tar.gz");
        File local = new File(pathToUpenaJar);
        if (!local.exists()) {
            LOG.error(local.getAbsolutePath() + " doesn't exists.");
            throw new RuntimeException("Couldn't deploy because " + local.getAbsolutePath() + " doesn't exists.");
        }

        String localFile = local.getAbsolutePath();
        File _lfile = new File(localFile);
        LOG.info("Local file:" + _lfile.getAbsolutePath());

        File remoteFile = new File(scpHome + "/" + local.getName());

        LOG.info("Remote file:" + remoteFile.getAbsolutePath());
        LOG.info("SSH user: " + scpUser + ", SSH host: " + scpHost);

        Session session = jsch.getSession(scpUser, scpHost, 22);

        File sshKeyFile = new File(pathToSSHKey);
        LOG.info("Using this sshKey:" + sshKeyFile.getAbsolutePath());
        jsch.addIdentity(sshKeyFile.getAbsolutePath());
        jsch.setKnownHosts(new File(sshKeyFile.getParentFile(), "known_hosts").getAbsolutePath());

        // username and password will be given via UserInfo interface.
        UserInfo ui = new SshUser(null, null);
        session.setUserInfo(ui);

        session.connect();

        throwException(sshCommand(session, "/bin/mkdir -p " + remoteFile.getParent(), false), " Couldn't create diretory for " + remoteFile.getParent());
        transfer(remoteFile, session, _lfile, localFile);

        throwException(sshCommand(session, "cd " + scpHome + "; "
            + "nohup java -jar upena.jar " + scpHost + " " + clusterName + " & ", true), " Failed to start upena");

        session.disconnect();
        LOG.info("deployed");

    }

    void throwException(int code, String message) throws Exception {
        if (code != 0) {
            throw new RuntimeException("failed to deploy because:" + message);
        }
    }

    int sshCommand(Session session, String command, boolean verbose) throws JSchException, IOException {
        int exitStatus = 0;
        Channel channel = session.openChannel("exec");
        channel.getOutputStream();

        LOG.info("exec:" + command);

        try {
            ((ChannelExec) channel).setCommand(command);
            // X Forwarding
            // channel.setXForwarding(true);
            //channel.setInputStream(System.in);
            channel.setInputStream(null);
            //channel.setOutputStream(System.out);
            //FileOutputStream fos=new FileOutputStream("/tmp/stderr");
            //((ChannelExec)channel).setErrStream(fos);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] tmp = new byte[1024 * 512];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024 * 512);
                    if (i < 0) {
                        break;
                    }
                    if (verbose) {
                        System.out.print(command + "$ " + new String(tmp, 0, i));
                    }
                }
                if (channel.isClosed() || channel.getExitStatus() != -1) {
                    exitStatus = channel.getExitStatus();
                    LOG.info("exit-status " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ee) {
                    Thread.interrupted();
                }
            }
            return exitStatus;
        } finally {
            channel.disconnect();
        }
    }

    void transfer(File remoteFile, Session session, File _lfile, String localFile) throws JSchException, FileNotFoundException, IOException {
        boolean ptimestamp = true;
        // exec 'scp -t rfile' remotely
        String command = "scp -r " + (ptimestamp ? "-p" : "") + " -t " + remoteFile;
        Channel channel = session.openChannel("exec");
        try {
            LOG.info("running:" + command);
            ((ChannelExec) channel).setCommand(command);
            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();
            LOG.info("connect");
            channel.connect();
            //getLog().info("checkAck");
            if (checkAck(in) != 0) {
                throw new RuntimeException("failed to deploy!");
            }
            if (ptimestamp) {
                command = "T " + (_lfile.lastModified() / 1000) + " 0";
                // The access time should be sent here,
                // but it is not accessible with JavaAPI ;-<
                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                //getLog().info("running:" + command);
                out.write(command.getBytes());
                out.flush();
                //getLog().info("checkAck");
                if (checkAck(in) != 0) {
                    throw new RuntimeException("failed to deploy!");
                }
            }
            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = _lfile.length();
            command = "C0644 " + filesize + " ";
            if (localFile.lastIndexOf('/') > 0) {
                command += localFile.substring(localFile.lastIndexOf('/') + 1);
            } else {
                command += localFile;
            }
            command += "\n";
            //getLog().info("running:" + command);
            out.write(command.getBytes());
            out.flush();
            //getLog().info("checkAck");
            if (checkAck(in) != 0) {
                throw new RuntimeException("failed to deploy!");
            }
            LOG.info("");
            LOG.info("transfering file:" + _lfile.getAbsolutePath());
            // send a content of lfile
            FileInputStream fis = new FileInputStream(localFile);
            byte[] buf = new byte[1024 * 1024 * 2];
            long bytes = 0;
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len);
                out.flush();
                bytes += len;
                LOG.info("transfered: " + (int) (((float) bytes / (float) filesize) * 100) + "% " + bytes + " bytes out of " + filesize + " bytes");
            }
            fis.close();
            fis = null;
            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                throw new RuntimeException("failed to deploy!");
            }
            LOG.info("transfered from file:" + _lfile.getAbsolutePath());
            LOG.info("transfered to file:" + remoteFile);
            out.close();
        } finally {
            channel.disconnect();

        }
    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) {
            return b;
        }
        if (b == -1) {
            return b;
        }

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    static class SshUser implements UserInfo {

        private final MetricLogger LOG = MetricLoggerFactory.getLogger();
        private final String passPhrase;
        private final String password;

        SshUser(String passPhrase, String password) {
            this.passPhrase = passPhrase;
            this.password = password;
        }

        @Override
        public String getPassphrase() {
            return passPhrase;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(String string) {
            LOG.info("UserInfo.promptPassword(" + string + ")");
            return false;
        }

        @Override
        public boolean promptPassphrase(String string) {
            LOG.info("UserInfo.promptPassphrase(" + string + ")");
            return false;
        }

        @Override
        public boolean promptYesNo(String string) {
            LOG.info("UserInfo.promptYesNo(" + string + ")");
            return true;
        }

        @Override
        public void showMessage(String string) {
            LOG.info("UserInfo.showMessage(" + string + ")");
        }

    }
}
