package com.emailorch.email_fetcher.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import org.springframework.stereotype.Service;
import com.google.api.client.json.jackson2.JacksonFactory;

@Service
public class GmailService {
    NetHttpTransport transport ;
    JsonFactory jsonFactory ;
    public GmailService(){
        this.transport =  new NetHttpTransport();
        this.jsonFactory   = JacksonFactory.getDefaultInstance();
}

    public Gmail createClient(String accessToken){
        Credential credentials =new GoogleCredential()
                .setAccessToken(accessToken);

        return new Gmail.Builder(transport,jsonFactory,credentials).build();

    }
}
