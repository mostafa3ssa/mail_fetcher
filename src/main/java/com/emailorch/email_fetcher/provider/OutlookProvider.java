package com.emailorch.email_fetcher.provider;

import com.emailorch.email_fetcher.service.OutlookService;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.odataerrors.ODataError;

import java.io.*;
import java.util.Base64;

public class OutlookProvider implements  EmailProvider{
    private final OutlookService outlookService;
    public OutlookProvider(OutlookService outlookService){

        this.outlookService = outlookService;
    }

    @Override
    public InputStream stream(String tok, String msgId, String attId) throws UncheckedIOException {
      try {
          var outlookServiceClient = outlookService.createClient(tok);
          Attachment attachment = outlookServiceClient.me().messages().byMessageId(msgId).attachments().byAttachmentId(attId).get();
          if(attachment instanceof FileAttachment fa ){
               byte[] bytes =  fa.getContentBytes();
              if (bytes == null || bytes.length == 0) {
                  throw new RuntimeException("Outlook returned empty attachment");
              }
              return new ByteArrayInputStream(bytes);
          }




      }catch (ODataError ex){
        throw  new ODataError();
      }
        return null;
    }
}
