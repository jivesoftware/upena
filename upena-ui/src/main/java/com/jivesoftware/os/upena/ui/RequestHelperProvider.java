package com.jivesoftware.os.upena.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class RequestHelperProvider extends JPanel {

    final JTextField editHost;
    final JTextField editPort;
    final JTextField editUserName;
    final JPasswordField editPassword;

    public RequestHelperProvider(String lable, String host) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        final ImageIcon background = Util.resize(Util.icon("cluster"), 48, 48);
        JLabel banner = new JLabel(background);
        banner.setMinimumSize(new Dimension(72, 48));
        banner.setSize(new Dimension(72, 48));
        banner.setPreferredSize(new Dimension(72, 48));
        banner.setMaximumSize(new Dimension(72, 48));
        add(banner);
        add(Box.createRigidArea(new Dimension(10, 0)));
        add(new JLabel(lable + " host:"));
        editHost = new JTextField(host, 120);
        editHost.setMaximumSize(new Dimension(120, 24));
        add(editHost);
        add(Box.createRigidArea(new Dimension(10, 0)));

        add(new JLabel("port:"));
        editPort = new JTextField("1175", 120);
        editPort.setMaximumSize(new Dimension(120, 24));
        add(editPort);
        add(Box.createRigidArea(new Dimension(10, 0)));

        add(new JLabel("user:"));
        editUserName = new JTextField("anonymous", 120);
        editUserName.setMaximumSize(new Dimension(120, 24));
        add(editUserName);
        add(Box.createRigidArea(new Dimension(10, 0)));

        add(new JLabel("password:"));
        editPassword = new JPasswordField("", 120);
        editPassword.setMaximumSize(new Dimension(120, 24));
        add(editPassword);
        add(Box.createRigidArea(new Dimension(10, 0)));

        setOpaque(true);
        setBackground(Color.white);
    }

    HttpRequestHelper get() {

        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(editHost.getText(), Integer.parseInt(editPort.getText()));
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new HttpRequestHelper(httpClient, mapper);
    }
}
