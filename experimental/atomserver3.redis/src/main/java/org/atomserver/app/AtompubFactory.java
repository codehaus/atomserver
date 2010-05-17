package org.atomserver.app;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

@Component
public class AtompubFactory {
    @Autowired
    private Abdera abdera;

    private Person atomserverAuthor;
    private Generator atomserverGenerator;

    @PostConstruct
    public void init() {
        atomserverAuthor = abdera.getFactory().newAuthor();
        atomserverAuthor.setName("AtomServer v3");
        atomserverAuthor.setEmail("contact@atomserver.org");
        atomserverAuthor.setUri("http://atomserver.org");

        atomserverGenerator = abdera.getFactory().newGenerator();
        atomserverGenerator.setText("AtomServer");
        atomserverGenerator.setVersion("v3");
        atomserverGenerator.setUri("http://atomserver.org");
    }

    public Feed newFeed(String id, String title, String self) {
        final Feed feed = abdera.newFeed();
        feed.addAuthor(atomserverAuthor);
        feed.setGenerator(atomserverGenerator);
        feed.setId(id);
        feed.setTitle(title);
        feed.addLink(self, "self");
        return feed;
    }

    public Entry newEntry() {
        return abdera.newEntry();
    }

    public Category newCategory() {
        return abdera.getFactory().newCategory();
    }
}
