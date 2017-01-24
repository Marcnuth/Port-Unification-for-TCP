package com.github.marcnuth;

/**
 * Created by marc on 17/1/23.
 */
public class Configs {

    public final static boolean needBothAuth = true;
    public final static String SSL_PROTOCOL = "TLSV1";
    public final static String KEYSTORE_TYPE = "JKS";
    public final static String TRUST_MANAGER_ALGORITHM = "SunX509";

    public final static String FILE_SERVER_JKS = Configs.class.getClassLoader().getResource("server_test.jks").getPath();
    public final static String STOREPASS_SERVER_CERT = "testcase";
    public final static String KEYPASS_SERVER_CERT = "testcase";
    public final static String FILE_SERVER_TRUST_CERT = Configs.class.getClassLoader().getResource("server_trust_test.jks").getPath();
    public final static String PWD_SERVER_TRUST_CERT = "testcase";


    public final static String FILE_CLIENT_JKS = Configs.class.getClassLoader().getResource("client_test.jks").getPath();
    public final static String STOREPASS_CLIENT_CERT = "testcase";
    public final static String KEYPASS_CLIENT_CERT = "testcase";
    public final static String FILE_CLIENT_TRUST_CERT = Configs.class.getClassLoader().getResource("client_trust_test.jks").getPath();
    public final static String PWD_CLIENT_TRUST_CERT = "testcase";


}
